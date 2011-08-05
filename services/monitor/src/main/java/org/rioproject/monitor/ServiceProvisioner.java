/*
 * Copyright to the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.rioproject.monitor;

import com.sun.jini.config.Config;
import com.sun.jini.landlord.FixedLeasePeriodPolicy;
import com.sun.jini.landlord.LeasePeriodPolicy;
import com.sun.jini.landlord.LeasedResource;
import net.jini.config.Configuration;
import net.jini.core.constraint.RemoteMethodControl;
import net.jini.core.event.EventRegistration;
import net.jini.core.lease.Lease;
import net.jini.core.lease.LeaseDeniedException;
import net.jini.core.lease.UnknownLeaseException;
import net.jini.id.Uuid;
import net.jini.security.BasicProxyPreparer;
import net.jini.security.ProxyPreparer;
import org.rioproject.core.ServiceElement;
import org.rioproject.core.provision.DeployedService;
import org.rioproject.core.provision.ServiceBeanInstantiator;
import org.rioproject.core.provision.ServiceProvisionEvent;
import org.rioproject.event.EventHandler;
import org.rioproject.monitor.managers.FixedServiceManager;
import org.rioproject.monitor.managers.PendingManager;
import org.rioproject.monitor.selectors.RoundRobinSelector;
import org.rioproject.monitor.selectors.Selector;
import org.rioproject.monitor.selectors.ServiceResourceSelector;
import org.rioproject.monitor.tasks.ProvisionFailureEventTask;
import org.rioproject.monitor.tasks.ProvisionTask;
import org.rioproject.monitor.util.LoggingUtil;
import org.rioproject.resources.servicecore.LandlordLessor;
import org.rioproject.resources.servicecore.LeaseListenerAdapter;
import org.rioproject.resources.servicecore.ServiceResource;
import org.rioproject.system.ResourceCapability;
import org.rioproject.watch.GaugeWatch;

import java.io.IOException;
import java.rmi.MarshalledObject;
import java.rmi.NoSuchObjectException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The ServiceProvisioner is responsible for the managing the leases of
 * <code>ServiceBeanInstantiator</code> objects and performing the actual
 * provisioning of <code>ServiceElement</code> objects to available
 * <code>ServiceBeanInstantiator</code> instances based on the capability to
 * fulfill the Quality of Service specification of the
 * <code>ServiceElement</code>.
 *
 * @author Dennis Reedy
 */
public class ServiceProvisioner implements ServiceProvisionDispatcher {
    /** Indicates a Provision failure */
    public static final int PROVISION_FAILURE = 1;
    /** Indicates a JSB cannot be provisioned */
    public static final int UNINSTANTIABLE_JSB = 1 << 1;
    /** Indicates a JSB cannot be provisioned */
    public static final int BAD_CYBERNODE = 1 << 2;
    /** The Landlord that will manage leases for ServiceInstantiation resources */
    LandlordLessor landlord;
    /** ProvisionEvent sequence number */
    AtomicInteger serviceProvisionEventSequenceNumber = new AtomicInteger(0);
    /** EventRegistration sequence number */
    AtomicInteger eventRegistrationSequenceNumber = new AtomicInteger(0);
    /** Event source */
    Object eventSource;
    /** EventHandler to fire ProvisionFailureEvent notifications */
    EventHandler failureHandler;
    /** Executor for provision processing */
    ThreadPoolExecutor provisioningPool;
    /** Default number of maximum Threads to have in the ThreadPool */
    static final int DEFAULT_MAX_THREADS = 10;
    /** Executor for provision failure event processing */
    ThreadPoolExecutor provisionFailurePool;
    /** Collection of in-process provision attempts */
    final List<ServiceElement> inProcess = Collections.synchronizedList(new ArrayList<ServiceElement>());
    /** A Watch to measure provision time */
    GaugeWatch watch;
    /** Manages pending provision dispatch requests for provision types of auto */
    final PendingManager pendingMgr;
    /** Manages provision dispatch requests for provision types of station */
    final FixedServiceManager fixedServiceManager;
    /** Manages the selection of ServiceResource objects for provisioning requests */
    final ServiceResourceSelector selector;
    /** ProxyPreparer for ServiceInstantiator proxies */
    ProxyPreparer instantiatorPreparer;
    /** Logger instance */
    static final Logger logger = Logger.getLogger("org.rioproject.monitor");
    private boolean terminating = false;
    private boolean terminated = false;

    /**
     * Create a ServiceProvisioner
     * 
     * @param config Configuration object used to set operational parameters
     * @param eventSource The originator of events
     * @param failureHandler Event handler for processing failure event notifications
     * @param watch For timing how long it takes to provision a service
     * @throws Exception if errors are encountered using the Configuration
     * object or creating LandlordLessor
     */
    ServiceProvisioner(Configuration config,
                       Object eventSource,
                       EventHandler failureHandler,
                       GaugeWatch watch) throws Exception {
        if(config==null)
            throw new NullPointerException("config is null");
        if(failureHandler==null)
            throw new NullPointerException("failureHandler is null");
        long ONE_MINUTE = 1000 * 60;
        /* 5 minute default Lease time */
        long DEFAULT_LEASE_TIME = ONE_MINUTE*5;
        /* 1 day max lease time */
        long DEFAULT_MAX_LEASE_TIME = ONE_MINUTE*60*24;
        /* Get the maximum amount of Threads to create for the ThreadPools */
        int provisioningPoolMaxThreads = Config.getIntEntry(config,
                                                            ProvisionMonitorImpl.CONFIG_COMPONENT,
                                                            "provisioningPoolMaxThreads",
                                                            DEFAULT_MAX_THREADS,
                                                            1,
                                                            500);
        if(logger.isLoggable(Level.FINEST))
            logger.finest("MaxThreads={" + provisioningPoolMaxThreads + "}");

        /* Get the Lease policy */
        LeasePeriodPolicy provisionerLeasePolicy =
            (LeasePeriodPolicy)Config.getNonNullEntry(config,
                                                      ProvisionMonitorImpl.CONFIG_COMPONENT,
                                                      "provisionerLeasePeriodPolicy",
                                                      LeasePeriodPolicy.class,
                                                      new FixedLeasePeriodPolicy(DEFAULT_MAX_LEASE_TIME,
                                                                                 DEFAULT_LEASE_TIME));
        /* Get the ProxyPreparer for ServiceInstantiator instances */
        instantiatorPreparer =
            (ProxyPreparer)config.getEntry(ProvisionMonitorImpl.CONFIG_COMPONENT,
                                           "instantiatorPreparer",
                                           ProxyPreparer.class,
                                           new BasicProxyPreparer());

        /* Create a ThreadPool for provisioning notification */
        provisioningPool = (ThreadPoolExecutor) Executors.newFixedThreadPool(provisioningPoolMaxThreads);

        /* Create a ThreadPool for provision failure notification */
        provisionFailurePool = (ThreadPoolExecutor) Executors.newFixedThreadPool(provisioningPoolMaxThreads);

        /* Create the LandlordLessor */
        landlord = new LandlordLessor(config, provisionerLeasePolicy);
        landlord.addLeaseListener(new LeaseMonitor());

        /* Get the ServiceResourceSelector */
        selector = (ServiceResourceSelector)config.getEntry(
                                               ProvisionMonitorImpl.CONFIG_COMPONENT,
                                               "serviceResourceSelector",
                                               ServiceResourceSelector.class,
                                               new RoundRobinSelector());
        selector.setLandlordLessor(landlord);
        if(logger.isLoggable(Level.FINEST))
            logger.finest("ServiceResourceSelector : " + selector.getClass().getName());

        this.eventSource = eventSource;
        this.watch = watch;
        this.failureHandler = failureHandler;

        pendingMgr = new PendingManager(getServiceProvisionContext(null, null));
        fixedServiceManager = new FixedServiceManager(getServiceProvisionContext(null, null), pendingMgr);
    }

    ServiceProvisionContext getServiceProvisionContext(ProvisionRequest request, ServiceResource serviceResource) {
        ServiceProvisionContext context = new ServiceProvisionContext(selector,
                                                                      provisioningPool,
                                                                      inProcess,
                                                                      eventSource,
                                                                      watch,
                                                                      this,
                                                                      provisionFailurePool,
                                                                      failureHandler,
                                                                      serviceProvisionEventSequenceNumber);
        context.setProvisionRequest(request);
        context.setServiceResource(serviceResource);
        return context;
    }

    /**
     * Clean up all resources
     */
    void terminate() {
        terminating = true;
        landlord.stop(true);
        provisioningPool.shutdownNow();
        provisionFailurePool.shutdownNow();
        terminated = true;
    }

    /**
     * @return the ServiceResourceSelector
     */
    ServiceResourceSelector getServiceResourceSelector() {
        return(selector);
    }

    /**
     * @return The PendingManager
     */
    PendingManager getPendingManager() {
        return(pendingMgr);
    }

    /**
     * @return The FixedServiceManager
     */
    FixedServiceManager getFixedServiceManager() {
        return(fixedServiceManager);
    }

    /**
     * Registers a RemoteEventListener that will be notified in a round robin 
     * fashion to instantiate a ServiceBean as defined by the ServiceElement object 
     * that will be contained within the ServiceProvisionEvent<br>
     *
     * @param sbi The ServiceBeanInstantiator wrapped in a MarshalledObject
     * @param handback A handback object
     * @param resourceCapability The capabilities of the ServiceInstantiator
     * @param deployedServices List of deployed services
     * @param serviceLimit The maximum number of services the 
     * ServiceBeanInstantiator has been configured to instantiate 
     * @param duration Requested lease duration
     *
     * @return EventRegistration
     *
     * @throws LeaseDeniedException If the Lease is denied for any reason
     * @throws RemoteException for comm errors
     */
    EventRegistration register(MarshalledObject<ServiceBeanInstantiator> sbi,
                               MarshalledObject handback,
                               ResourceCapability resourceCapability,
                               List<DeployedService> deployedServices,
                               int serviceLimit,
                               long duration) throws LeaseDeniedException, RemoteException {
        ServiceBeanInstantiator instantiator;

        try {
            instantiator = sbi.get();
        } catch (IOException e) {
            throw new LeaseDeniedException("Error calling MarshalledObject.get(), "+e.getLocalizedMessage());
        } catch (ClassNotFoundException e) {
            throw new LeaseDeniedException("Could not load ServiceBeanInstantiator, "+e.getLocalizedMessage());
        }

        if(instantiator instanceof RemoteMethodControl)
            instantiator = (ServiceBeanInstantiator)instantiatorPreparer.prepareProxy(instantiator);
        String name;
        try {
            name = instantiator.getName();
        } catch(Throwable t) {
            name = "Cybernode";
        }

        Uuid uuid=instantiator.getInstantiatorUuid();
        InstantiatorResource resource = new InstantiatorResource(sbi,
                                                                 instantiator,
                                                                 name,
                                                                 uuid,
                                                                 handback,
                                                                 resourceCapability,
                                                                 serviceLimit);
        try {
            resource.setDeployedServices(deployedServices);
        } catch (Throwable t) {
            logger.log(Level.WARNING, "Registering a ServiceBeanInstantiator", t);
            throw new LeaseDeniedException("Getting ServiceRecords");
        }

        ServiceResource serviceResource = new ServiceResource(resource);

        /* Add ServiceResource to landlord */
        Lease lease = landlord.newLease(serviceResource, duration);        
        EventRegistration registration = new EventRegistration(ServiceProvisionEvent.ID,
                                                               eventSource,
                                                               lease,
                                                               eventRegistrationSequenceNumber.incrementAndGet());

        if(logger.isLoggable(Level.FINE)) {
            int instantiatorCount = landlord.total();
            logger.log(Level.FINE,
                       "Registered new "+name+" @ {0}, count [{1}]",
                       new Object[]{resourceCapability.getAddress(), instantiatorCount});
        }
        /* Process all provision types of Fixed first */
        fixedServiceManager.process(serviceResource);
        /* See if any dynamic provision types are pending */
        pendingMgr.process();
        return(registration);
    }


    /**
     * Get the corresponding InstantiatorResource by iterating through all
     * known ServiceResource instances. If not found or the lease is not valid 
     * throw an UnknownLeaseException
     * 
     * @param resource The ServiceBeanInstantiator
     * @param updatedCapabilities Updated ResourceCapability
     * @param deployedServices List of deployed services
     * @param serviceLimit The maximum number of services the 
     * ServiceBeanInstantiator has been configured to instantiate
     *
     * @throws UnknownLeaseException If the Lease is unknown
     * @throws RemoteException if the ServiceBeanInstantiator proxy fails
     * preparation
     */

    void handleFeedback(ServiceBeanInstantiator resource,
                        ResourceCapability updatedCapabilities,
                        List<DeployedService> deployedServices,
                        int serviceLimit)
    throws UnknownLeaseException, RemoteException {

        if(resource instanceof RemoteMethodControl)
            resource = (ServiceBeanInstantiator)instantiatorPreparer.prepareProxy(resource);
        ServiceResource[] svcResources = selector.getServiceResources();
        if(svcResources.length == 0)
            throw new UnknownLeaseException("Empty Collection, no leases");
        boolean updated = false;
        for(ServiceResource svcResource : svcResources) {
            InstantiatorResource ir = (InstantiatorResource) svcResource.getResource();
            if(logger.isLoggable(Level.FINEST))
                logger.log(Level.FINEST,
                           "Update from [{0}:{1}] updatedCapabilities: {2}, serviceLimit {3}",
                           new Object[] {ir.getHostAddress(),
                                         resource.toString(),
                                         updatedCapabilities,
                                         serviceLimit
                           });
            if(ir.getInstantiator().equals(resource)) {
                if(!landlord.ensure(svcResource))
                    throw new UnknownLeaseException("No matching Lease found");
                updated = true;
                ir.setResourceCapability(updatedCapabilities);
                ir.setServiceLimit(serviceLimit);
                try {
                    ir.setDeployedServices(deployedServices);
                } catch (Throwable t) {
                    logger.log(Level.WARNING, "Getting ServiceRecords", t);
                }
                /* Process all provision types of Fixed first */
                fixedServiceManager.process(svcResource);
                /* See if any dynamic provision types are pending */
                pendingMgr.process();
                break;
            }
        }

        if(!updated) {
            throw new UnknownLeaseException("No matching registration found for Cybernode");
        }
    }

    /**
     * Dispatch a provision request. This method is used to provision ServiceElement 
     * object that has a provision type of DYNAMIC
     * 
     * @param request The ProvisionRequest
     */
    public void dispatch(ProvisionRequest request) {
        ServiceResource resource = Selector.acquireServiceResource(request, selector);
        dispatch(request, resource, 0);
    }

    /**
     * Provision a pending ServiceElement with a provision type of DYNAMIC with an 
     * index into the Collection of pending ServiceElement instances managed by the 
     * PendingManager. If a ServiceResource cannot be found, put the ServiceElement 
     * under the management of the PendingManager and fire a ProvisionFailureEvent
     * 
     * @param request The ProvisionRequest
     * @param resource A ServiceResource that contains an InstantiatorResource 
     * which meets the operational requirements of the ServiceElement
     * @param index Index of the ServiceElement in the pending collection
     */
    public void dispatch(ProvisionRequest request, ServiceResource resource, long index) {
        if(terminating || terminated) {
            logger.info("Request to dispatch "+ LoggingUtil.getLoggingName(request)+" ignored, utility has terminated");
            return;
        }
        try {
            if(resource != null) {
                inProcess.add(request.sElem);
                provisioningPool.execute(new ProvisionTask(getServiceProvisionContext(request, resource),
                                                           pendingMgr,
                                                           index));
            } else {
                int total = selector.getServiceResources().length;
                String action = (request.type==ProvisionRequest.Type.PROVISION? "provision":"relocate");
                String failureReason = "A compute resource could not be " +
                                       "obtained to "+action+" ["+ LoggingUtil.getLoggingName(request)+"], " +
                                       "total registered="+total;
                if(logger.isLoggable(Level.FINE)) {
                    logger.log(Level.FINE,  failureReason);
                }
                /* If we have a ServiceProvisionListener, notify the
                 * listener */
                if(request.svcProvisionListener!=null) {
                    try {
                        request.svcProvisionListener.failed(
                                                      request.sElem,
                                                      true);
                    } catch(NoSuchObjectException e) {
                        logger.log(Level.WARNING,
                                   "ServiceBeanInstantiatorListener failure notification did not succeed, "+
                                   "[java.rmi.NoSuchObjectException: "+e.getLocalizedMessage()+"], remove "+
                                   "ServiceBeanInstantiatorListener ["+request.svcProvisionListener+"]");
                        request.svcProvisionListener = null;
                    } catch(Exception e) {
                        logger.log(Level.WARNING, "ServiceBeanInstantiatorListener notification", e);
                    }
                }
                /* If this is not the result of a relocation request, add to the 
                 * pending testManager */
                if(request.type==ProvisionRequest.Type.PROVISION) {
                    pendingMgr.addProvisionRequest(request, index);
                    if(logger.isLoggable(Level.FINE))
                        logger.fine("Wrote [" + LoggingUtil.getLoggingName(request) + "] to " + pendingMgr.getType());
                    if(logger.isLoggable(Level.FINEST))
                        pendingMgr.dumpCollection();
                }
                processProvisionFailure(new ProvisionFailureEvent(eventSource, request.sElem, failureReason, null));
            }
        } catch(Throwable t) {
            logger.log(Level.WARNING, "Dispatching ProvisionRequest", t);
            processProvisionFailure(new ProvisionFailureEvent(eventSource,
                                                              request.sElem,
                                                              t.getClass().getName()+":"+t.getLocalizedMessage(),
                                                              t));
        }
    }

    /*
     * Helper method to dispatch a ProvisionFailureEventTask to send a ProvisionFailureEvent
     */
    private void processProvisionFailure(ProvisionFailureEvent event) {
        provisionFailurePool.execute(new ProvisionFailureEventTask(event, failureHandler));
    }

    /**
     * Monitors ServiceBeanInstantiator leases being removed. 
     */
    class LeaseMonitor extends LeaseListenerAdapter {
        public void removed(LeasedResource resource) {
            InstantiatorResource ir = (InstantiatorResource)((ServiceResource)resource).getResource();
            int instantiatorCount = landlord.total();
            if(logger.isLoggable(Level.FINE)) {
                logger.log(Level.FINE,
                           "{0} @ {1} removed, count now [{2}]",
                           new Object[]{ir.getName(), ir.getResourceCapability().getAddress(), instantiatorCount});
            }
        }
    }
}


