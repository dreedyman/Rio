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
package org.rioproject.monitor.service;

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
import org.rioproject.deploy.DeployedService;
import org.rioproject.deploy.ServiceBeanInstantiator;
import org.rioproject.deploy.ServiceProvisionEvent;
import org.rioproject.event.EventHandler;
import org.rioproject.monitor.ProvisionFailureEvent;
import org.rioproject.monitor.service.managers.FixedServiceManager;
import org.rioproject.monitor.service.managers.PendingManager;
import org.rioproject.monitor.service.selectors.RoundRobinSelector;
import org.rioproject.monitor.service.selectors.Selector;
import org.rioproject.monitor.service.selectors.ServiceResourceSelector;
import org.rioproject.monitor.service.tasks.ProvisionFailureEventTask;
import org.rioproject.monitor.service.tasks.ProvisionTask;
import org.rioproject.monitor.service.util.FailureReasonFormatter;
import org.rioproject.monitor.service.util.LoggingUtil;
import org.rioproject.opstring.ServiceElement;
import org.rioproject.impl.service.LandlordLessor;
import org.rioproject.impl.service.LeaseListenerAdapter;
import org.rioproject.impl.service.ServiceResource;
import org.rioproject.system.ResourceCapability;
import org.rioproject.impl.util.TimeConstants;
import org.rioproject.impl.watch.GaugeWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    private LandlordLessor landlord;
    /** ProvisionEvent sequence number */
    private AtomicInteger serviceProvisionEventSequenceNumber = new AtomicInteger(0);
    /** EventRegistration sequence number */
    private AtomicInteger eventRegistrationSequenceNumber = new AtomicInteger(0);
    /** Event source */
    private Object eventSource;
    /** EventHandler to fire ProvisionFailureEvent notifications */
    private EventHandler failureHandler;
    /** Executor for provision processing */
    private ThreadPoolExecutor provisioningPool;
    /** Executor for provision failure event processing */
    private ThreadPoolExecutor provisionFailurePool;
    /** Collection of in-process provision attempts */
    private final List<ServiceElement> inProcess = Collections.synchronizedList(new ArrayList<ServiceElement>());
    /** A Watch to measure provision time */
    private GaugeWatch watch;
    /** Manages pending provision dispatch requests for provision types of auto */
    private final PendingManager pendingMgr;
    /** Manages provision dispatch requests for provision types of station */
    private final FixedServiceManager fixedServiceManager;
    /** Manages the selection of ServiceResource objects for provisioning requests */
    private final ServiceResourceSelector selector;
    /** ProxyPreparer for ServiceInstantiator proxies */
    private ProxyPreparer instantiatorPreparer;
    private static final String CONFIG_COMPONENT = "org.rioproject.monitor";
    /** Logger instance */
    private static final Logger logger = LoggerFactory.getLogger(ServiceProvisioner.class);
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
    ServiceProvisioner(final Configuration config,
                       final Object eventSource,
                       final EventHandler failureHandler,
                       final GaugeWatch watch) throws Exception {
        if(config==null)
            throw new IllegalArgumentException("config is null");
        if(failureHandler==null)
            throw new IllegalArgumentException("failureHandler is null");
        /* 5 minute default Lease time */
        long DEFAULT_LEASE_TIME = TimeConstants.FIVE_MINUTES;
        /* 1 day max lease time */
        long DEFAULT_MAX_LEASE_TIME = TimeConstants.ONE_DAY;

        /* Get the Lease policy */
        LeasePeriodPolicy provisionerLeasePolicy =
            (LeasePeriodPolicy)Config.getNonNullEntry(config,
                                                      CONFIG_COMPONENT,
                                                      "provisionerLeasePeriodPolicy",
                                                      LeasePeriodPolicy.class,
                                                      new FixedLeasePeriodPolicy(DEFAULT_MAX_LEASE_TIME,
                                                                                 DEFAULT_LEASE_TIME));
        /* Get the ProxyPreparer for ServiceInstantiator instances */
        instantiatorPreparer = (ProxyPreparer)config.getEntry(CONFIG_COMPONENT,
                                                              "instantiatorPreparer",
                                                              ProxyPreparer.class,
                                                              new BasicProxyPreparer());

        /* Create a ThreadPool for provisioning notification */
        //provisioningPool = (ThreadPoolExecutor) Executors.newFixedThreadPool(provisioningPoolMaxThreads);
        provisioningPool = (ThreadPoolExecutor) Executors.newCachedThreadPool();

        /* Create a ThreadPool for provision failure notification */
        //provisionFailurePool = (ThreadPoolExecutor) Executors.newFixedThreadPool(provisioningPoolMaxThreads);
        provisionFailurePool = (ThreadPoolExecutor) Executors.newCachedThreadPool();

        /* Create the LandlordLessor */
        landlord = new LandlordLessor(config, provisionerLeasePolicy);
        landlord.addLeaseListener(new LeaseMonitor());

        /* Get the ServiceResourceSelector */
        selector = (ServiceResourceSelector)config.getEntry(CONFIG_COMPONENT,
                                                            "serviceResourceSelector",
                                                            ServiceResourceSelector.class,
                                                            new RoundRobinSelector());
        selector.setLandlordLessor(landlord);
        logger.trace("ServiceResourceSelector : {}", selector.getClass().getName());

        this.eventSource = eventSource;
        this.watch = watch;
        this.failureHandler = failureHandler;

        pendingMgr = new PendingManager(getServiceProvisionContext(null, null));
        fixedServiceManager = new FixedServiceManager(getServiceProvisionContext(null, null));
    }

    private ServiceProvisionContext getServiceProvisionContext(final ProvisionRequest request,
                                                               final ServiceResource serviceResource) {
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
    EventRegistration register(final MarshalledObject<ServiceBeanInstantiator> sbi,
                               final MarshalledObject handback,
                               final ResourceCapability resourceCapability,
                               final List<DeployedService> deployedServices,
                               final int serviceLimit,
                               final long duration) throws LeaseDeniedException, RemoteException {
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
            logger.warn("Registering a ServiceBeanInstantiator", t);
            throw new LeaseDeniedException("Getting ServiceRecords");
        }

        ServiceResource serviceResource = new ServiceResource(resource);

        /* Add ServiceResource to landlord */
        Lease lease = landlord.newLease(serviceResource, duration);        
        EventRegistration registration = new EventRegistration(ServiceProvisionEvent.ID,
                                                               eventSource,
                                                               lease,
                                                               eventRegistrationSequenceNumber.incrementAndGet());
        logger.debug("Registered new {}, count [{}]", name, landlord.total());
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
    void handleFeedback(final ServiceBeanInstantiator resource,
                        final ResourceCapability updatedCapabilities,
                        final List<DeployedService> deployedServices,
                        final int serviceLimit) throws UnknownLeaseException, RemoteException {
        ServiceBeanInstantiator preparedResource = resource;
        if(resource instanceof RemoteMethodControl)
            preparedResource = (ServiceBeanInstantiator)instantiatorPreparer.prepareProxy(resource);
        ServiceResource[] svcResources = selector.getServiceResources();
        if(svcResources.length == 0) {
            logger.warn("{} is updating resource information, but we don't have any registered Cybernodes. " +
                        "Force removal of all Leases", resource.getName());
            landlord.removeAll();
            throw new UnknownLeaseException("Update failed, there are no known leases.");
        }
        boolean updated = false;
        ServiceResource couldNotEnsureLease = null;
        for(ServiceResource svcResource : svcResources) {
            InstantiatorResource ir = (InstantiatorResource) svcResource.getResource();
            logger.trace("Checking for InstantiatorResource match");
            if(ir.getInstantiator().equals(preparedResource)) {
                logger.trace("Update from {}, current serviceCount {}, serviceLimit {}",
                             ir.getName(),
                             deployedServices.size(),
                             serviceLimit);
                logger.trace("Matched InstantiatorResource");
                if(!landlord.ensure(svcResource)) {
                    couldNotEnsureLease = svcResource;
                    break;
                }
                updated = true;
                logger.trace("Set updated resource capabilities");
                ir.setResourceCapability(updatedCapabilities);
                logger.trace("Set serviceLimit to {}", serviceLimit);
                ir.setServiceLimit(serviceLimit);
                try {
                    logger.trace("Set deployedServices, was: {}, updated count is now: {}",
                                  ir.getServiceCount(), deployedServices.size());
                    ir.setDeployedServices(deployedServices);
                } catch (Throwable t) {
                    logger.warn("Getting ServiceRecords from {}", ir.getName(), t);
                }
                /* Process all provision types of Fixed first */
                fixedServiceManager.process(svcResource);
                /* See if any dynamic provision types are pending */
                pendingMgr.process();
                break;
            } else {
                logger.trace("Did not match InstantiatorResource");
            }
        }

        if(couldNotEnsureLease!=null) {
            selector.dropServiceResource(couldNotEnsureLease);
            throw new UnknownLeaseException("Could not ensure lease. Lease expiration: "+couldNotEnsureLease.getExpiration()+", " +
                                            "current time: "+System.currentTimeMillis());
        }
        if(!updated) {
            logger.warn("Update failed, no matching registration found for {}", resource.getName());
            throw new UnknownLeaseException("Update failed, no matching registration found");
        }
    }

    /**
     * Dispatch a provision request. This method is used to provision ServiceElement 
     * object that has a provision type of DYNAMIC
     * 
     * @param request The ProvisionRequest
     */
    public void dispatch(final ProvisionRequest request) {
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
    public void dispatch(final ProvisionRequest request, final ServiceResource resource, final long index) {
        if(terminating || terminated) {
            logger.info("Request to dispatch {} ignored, utility has terminated", LoggingUtil.getLoggingName(request));
            return;
        }
        try {
            if(resource != null) {
                inProcess.add(request.getServiceElement());
                provisioningPool.execute(new ProvisionTask(getServiceProvisionContext(request, resource),
                                                           pendingMgr,
                                                           index));
            } else {
                logger.warn(FailureReasonFormatter.format(request, selector));

                /* If we have a ServiceProvisionListener, notify the
                 * listener */
                if(request.getServiceProvisionListener()!=null) {
                    try {
                        request.getServiceProvisionListener().failed(request.getServiceElement(), true);
                    } catch(NoSuchObjectException e) {
                        logger.warn("ServiceBeanInstantiatorListener failure notification did not succeed, "+
                                       "[java.rmi.NoSuchObjectException: {}], remove ServiceBeanInstantiatorListener [{}]",
                                       e.getLocalizedMessage(), request.getServiceProvisionListener());
                        request.setServiceProvisionListener(null);
                    } catch(Exception e) {
                        logger.warn("ServiceBeanInstantiatorListener notification", e);
                    }
                }
                /* If this is not the result of a relocation request, add to the pending testManager */
                if(!request.getType().equals(ProvisionRequest.Type.RELOCATE)) {
                    pendingMgr.addProvisionRequest(request, index);
                    logger.debug("Wrote [{}] to {}", LoggingUtil.getLoggingName(request), pendingMgr.getType());
                    pendingMgr.dumpCollection();
                }
                processProvisionFailure(new ProvisionFailureEvent(eventSource,
                                                                  request.getServiceElement(),
                                                                  request.getFailureReasons(),
                                                                  null));
            }
        } catch(Throwable t) {
            logger.warn("Dispatching ProvisionRequest", t);
            processProvisionFailure(new ProvisionFailureEvent(eventSource,
                                                              request.getServiceElement(),
                                                              t.getClass().getName()+":"+t.getLocalizedMessage(),
                                                              t));
        }
    }

    /*
     * Helper method to dispatch a ProvisionFailureEventTask to send a ProvisionFailureEvent
     */
    private void processProvisionFailure(final ProvisionFailureEvent event) {
        provisionFailurePool.execute(new ProvisionFailureEventTask(event, failureHandler));
    }

    /**
     * Monitors ServiceBeanInstantiator leases being removed. 
     */
    class LeaseMonitor extends LeaseListenerAdapter {
        @Override
        public void expired(final LeasedResource resource) {
            remove(resource);
        }

        @Override
        public void removed(final LeasedResource resource) {
            remove(resource);
        }

        private void remove(final LeasedResource resource) {
            InstantiatorResource ir = (InstantiatorResource)((ServiceResource)resource).getResource();
            int instantiatorCount = landlord.total();
            logger.info("{} @ {} removed, count now [{}]",
                        ir.getName(), ir.getResourceCapability().getAddress(), instantiatorCount);
        }
    }
}


