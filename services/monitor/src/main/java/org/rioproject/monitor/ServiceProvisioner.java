/*
 * Copyright 2008 the original author or authors.
 * Copyright 2005 Sun Microsystems, Inc.
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
import net.jini.core.event.UnknownEventException;
import net.jini.core.lease.Lease;
import net.jini.core.lease.LeaseDeniedException;
import net.jini.core.lease.UnknownLeaseException;
import net.jini.id.Uuid;
import net.jini.security.BasicProxyPreparer;
import net.jini.security.ProxyPreparer;
import org.rioproject.core.JSBInstantiationException;
import org.rioproject.core.ServiceBeanInstance;
import org.rioproject.core.ServiceElement;
import org.rioproject.core.provision.DeployedService;
import org.rioproject.core.provision.ServiceBeanInstantiator;
import org.rioproject.core.provision.ServiceProvisionEvent;
import org.rioproject.event.EventHandler;
import org.rioproject.jsb.ServiceElementUtil;
import org.rioproject.resources.servicecore.LandlordLessor;
import org.rioproject.resources.servicecore.LeaseListenerAdapter;
import org.rioproject.resources.servicecore.ServiceResource;
import org.rioproject.resources.util.ThrowableUtil;
import org.rioproject.system.ResourceCapability;
import org.rioproject.watch.GaugeWatch;

import java.rmi.MarshalledObject;
import java.rmi.NoSuchObjectException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
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
public class ServiceProvisioner {
    /** Indicates a Provision failure */
    static final int PROVISION_FAILURE = 1;
    /** Indicates a JSB cannot be provisioned */
    static final int UNINSTANTIABLE_JSB = 1 << 1;
    /** Indicates a JSB cannot be provisioned */
    static final int BAD_CYBERNODE = 1 << 2;
    /** The Landlord which will manage leases for ServiceInstantiation
     * resources */
    LandlordLessor landlord;
    /** ProvisionEvent sequence number */
    int sequenceNumber = 0;
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
    List<ServiceElement> inProcess =
        Collections.synchronizedList(new ArrayList<ServiceElement>());
    /** A Watch to measure provision time */
    GaugeWatch watch;
    /** Manages pending provision dispatch requests for provision types of
     * auto */
    PendingManager pendingMgr = new PendingManager();
    /** Manages provision dispatch requests for provision types of station */
    FixedServiceManager fixedServiceManager = new FixedServiceManager();
    /** Manages the selection of ServiceResource objects for provisioning
     * requests */
    final ServiceResourceSelector selector;
    /** ProxyPreparer for ServiceInstantiator proxies */
    ProxyPreparer instantiatorPreparer;
    /** Logger instance */
    static final Logger logger = ProvisionMonitorImpl.logger;
    /** Logger for feedback updates from Cybernode instances */
    static final Logger feedbackLogger =
        Logger.getLogger(ProvisionMonitorImpl.LOGGER+".feedback");
    /** Logger for provision requests */
    static final Logger provisionLogger =
        Logger.getLogger(ProvisionMonitorImpl.LOGGER+".provision");
    private boolean terminating = false;
    private boolean terminated = false;

    /**
     * Create a ServiceProvisioner
     * 
     * @param config Configuration object used to set operational parameters
     * 
     * @throws Exception if errors are encountered using the Configuration
     * object or creating LandlordLessor
     */
    ServiceProvisioner(Configuration config) throws Exception {
        if(config==null)
            throw new NullPointerException("config is null");
        long ONE_MINUTE = 1000 * 60;
        /* 5 minute default Lease time */
        long DEFAULT_LEASE_TIME = ONE_MINUTE*5;
        /* 1 day max lease time */
        long DEFAULT_MAX_LEASE_TIME = ONE_MINUTE*60*24;
        /* Get the maximum amount of Threads to create for the ThreadPools */
        int provisioningPoolMaxThreads =
            Config.getIntEntry(config,
                               ProvisionMonitorImpl.CONFIG_COMPONENT,
                               "provisioningPoolMaxThreads",
                               DEFAULT_MAX_THREADS,
                               1,
                               500);
        if(logger.isLoggable(Level.FINEST))
            logger.log(Level.FINEST,
                       "MaxThreads={"+provisioningPoolMaxThreads+"}");

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
        provisionFailurePool =
            (ThreadPoolExecutor) Executors.newFixedThreadPool(provisioningPoolMaxThreads);
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
            logger.log(Level.FINEST,
                       "ServiceResourceSelector : "+selector.getClass().getName());
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
     * Set the event source which will be used as the source of ProvisionFailureEvent
     * notifications
     *
     * @param eventSource The event source
     */
    void setEventSource(Object eventSource) {
        this.eventSource = eventSource;
    }

    /**
     * Set the ProvisionFailureHandler for sending ProvisionFailureEvent objects
     *
     * @param  failureHandler The failure handler
     */
    void setProvisionFailureHandler(EventHandler failureHandler) {
        this.failureHandler = failureHandler;
    }

    /**
     * Set the GaugeWatch to measure provision times
     *
     * @param watch The GaugeWatch ro use for recording provision times
     */
    void setWatch(GaugeWatch watch) {
        this.watch = watch;
    }

    /**
     * Registers a RemoteEventListener that will be notified in a round robin 
     * fashion to instantiate a ServiceBean as defined by the ServiceElement object 
     * that will be contained within the ServiceProvisionEvent<br>
     *
     * @param instantiator The listener
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
    EventRegistration register(ServiceBeanInstantiator instantiator,
                               MarshalledObject handback,
                               ResourceCapability resourceCapability,
                               List<DeployedService> deployedServices,
                               int serviceLimit,
                               long duration)
    throws LeaseDeniedException, RemoteException {

        if(instantiator instanceof RemoteMethodControl)
            instantiator = (ServiceBeanInstantiator)instantiatorPreparer.prepareProxy(instantiator);
        String name;
        try {
            name = instantiator.getName();
        } catch(Throwable t) {
            name = "Cybernode";
        }

        Uuid uuid=instantiator.getInstantiatorUuid();
        InstantiatorResource resource =
            new InstantiatorResource(instantiator,
                                     name,
                                     uuid,
                                     handback,
                                     resourceCapability,
                                     serviceLimit);
        try {
            resource.setDeployedServices(deployedServices);
        } catch (Throwable t) {
            feedbackLogger.log(Level.WARNING,
                               "Registering a ServiceBeanInstantiator",
                               t);
            throw new LeaseDeniedException("Getting ServiceRecords");
        }

        ServiceResource serviceResource = new ServiceResource(resource);

        /* Add ServiceResource to landlord */
        Lease lease = landlord.newLease(serviceResource, duration);        
        EventRegistration registration =
            new EventRegistration(ServiceProvisionEvent.ID,
                                  eventSource,
                                  lease,
                                  sequenceNumber);
        if(feedbackLogger.isLoggable(Level.FINE)) {
            int instantiatorCount = landlord.total();
            feedbackLogger.log(Level.FINE,
                               "Registered new "+name+" @ {0}, count [{1}]",
                               new Object[]{resourceCapability.getAddress(),
                                            instantiatorCount});
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
     * @param deployedServices List of depoyed services
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
            InstantiatorResource ir =
                (InstantiatorResource) svcResource.getResource();
            if(feedbackLogger.isLoggable(Level.FINEST))
                feedbackLogger.log(Level.FINEST,
                                   "Update from [{0}:{1}] "+
                                   "updatedCapabilities: {2}, "+
                                   "serviceLimit {3}",
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
                    feedbackLogger.log(Level.WARNING,
                                       "Getting ServiceRecords",
                                       t);
                }
                /* Process all provision types of Fixed first */
                fixedServiceManager.process(svcResource);
                /* See if any dynamic provision types are pending */
                pendingMgr.process();
                break;
            }
        }

        if(!updated) {
            throw new UnknownLeaseException("No matching registration found " +
                                            "for Cybernode");
        }
    }

    /**
     * Get a ServiceBeanInstantiator that meets the operational requirements of a 
     * ServiceElement
     * 
     * @param request The ProvisionRequest
     * 
     * @return A ServiceResource that contains an InstantiatorResource
     * which meets the operational criteria of the ServiceElement
     */
    ServiceResource acquireServiceResource(ProvisionRequest request) {
        ServiceResource resource = null;
        synchronized(selector) {
            try {
                if(request.requestedUuid!=null) {
                    resource =
                        selector.getServiceResource(request.sElem,
                                                    request.requestedUuid,
                                                    true);
                    /* If the returned resource is null, then try to get
                     * any resource */
                    if(resource==null) {
                        resource = selector.getServiceResource(request.sElem);
                    }

                } else if(request.excludeUuid!=null) {
                    resource =
                        selector.getServiceResource(request.sElem,
                                                    request.excludeUuid,
                                                    false);
                } else {
                    resource = selector.getServiceResource(request.sElem);
                }

                if(resource!=null) {
                    InstantiatorResource ir =
                        (InstantiatorResource)resource.getResource();
                    ir.incrementProvisionCounter(request.sElem);
                }
            } catch(ProvisionException e) {
                if(e.isUninstantiable()) {
                    request.type = ProvisionRequest.Type.UNINSTANTIABLE;
                    request.listener.uninstantiable(request);
                }
            } catch(Exception e) {
                logger.log(Level.WARNING, "Getting ServiceResource", e);
            }
        }
        return(resource);
    }

    /**
     * Dispatch a provision request. This method is used to provision ServiceElement 
     * object that has a provision type of DYNAMIC
     * 
     * @param request The ProvisionRequest
     */
    void dispatch(ProvisionRequest request) {
        ServiceResource resource = acquireServiceResource(request);
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
    private void dispatch(ProvisionRequest request,
                          ServiceResource resource,
                          long index) {
        if(terminating || terminated) {
            logger.info("Request to dispatch "+request.sElem.getName()+" ignored, utility has terminated");
            return;
        }
        try {
            if(resource != null) {
                inProcess.add(request.sElem);
                provisioningPool.execute(new ProvisionTask(request,
                                                           resource,
                                                           index,
                                                           pendingMgr));
            } else {
                int total = selector.getServiceResources().length;
                String action =
                    (request.type==ProvisionRequest.Type.PROVISION?
                     "provision":"relocate");
                String failureReason = "A compute resource could not be " +
                                       "obtained to "+action+" " +
                                       "["+request.sElem.getName()+"], " +
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
                                   "ServiceBeanInstantiatorListener failure "+
                                   "notification did not succeeed, "+
                                   "[java.rmi.NoSuchObjectException:"+
                                   e.getLocalizedMessage()+"], remove "+
                                   "ServiceBeanInstantiatorListener "+
                                   "["+request.svcProvisionListener+"]");
                        request.svcProvisionListener = null;
                    } catch(Exception e) {
                        logger.log(Level.WARNING,
                                   "ServiceBeanInstantiatorListener " +
                                   "notification",
                                   e);
                    }
                }
                /* If this is not the result of a relocation request, add to the 
                 * pending testManager */
                if(request.type==ProvisionRequest.Type.PROVISION) {
                    pendingMgr.addProvisionRequest(request, index);
                    if(logger.isLoggable(Level.FINE))
                        logger.log(Level.FINE,
                                   "Wrote ["+
                                   request.sElem.getName()+"] "+
                                   "to " + pendingMgr.getType());
                    if(logger.isLoggable(Level.FINEST))
                        pendingMgr.dumpCollection();
                }
                processProvisionFailure(new ProvisionFailureEvent(
                                                         eventSource,
                                                         request.sElem,
                                                         failureReason,
                                                         null));
            }
        } catch(Throwable t) {
            logger.log(Level.WARNING, "Dispatching ProvisionRequest", t);
            processProvisionFailure(new ProvisionFailureEvent(
                                                        eventSource,
                                                        request.sElem,
                                                        t.getClass().getName()+
                                                        ":"+
                                                        t.getLocalizedMessage(),
                                                        t));
        }
    }

    /**
     * The ProvisionTask is created to process a provision dispatch request
     */
    class ProvisionTask implements Runnable {
        ProvisionRequest request;
        ServiceResource svcResource;
        PendingServiceElementManager pendingMgr;
        long index;
        //StopWatch stopWatch;
        ServiceBeanInstance jsbInstance = null;
        Throwable thrown = null;
        String failureReason = null;

        /**
         * Create a ProvisionTask
         *
         * @param request The ProvisionRequest
         * @param svcResource The selected ServiceResource to provision to
         */
        ProvisionTask(ProvisionRequest request, ServiceResource svcResource) {
            this(request, svcResource, 0, null);
        }

        /**
         * Create a ProvisionTask
         *
         * @param request The ProvisionRequest
         * @param svcResource The selected ServiceResource to provision to
         * @param index The index of the ServiceElement in the
         * PendingServiceElementManager
         * @param pendingMgr The PendingServiceElementManager
         */
        ProvisionTask(ProvisionRequest request,
                      ServiceResource svcResource,
                      long index,
                      PendingServiceElementManager pendingMgr) {
            this.request = request;
            this.svcResource = svcResource;
            this.index = index;
            this.pendingMgr = pendingMgr;
        }

        public void run() {
            try {
                jsbInstance = null;
                int result = doProvision(request, svcResource);
                if((result & PROVISION_FAILURE) != 0) {
                    boolean resubmitted = true;
                    if(provisionLogger.isLoggable(Level.FINE))
                        provisionLogger.log(Level.FINE,
                                            "Provision attempt failed for "+
                                            "["+request.sElem.getName()+"]");
                    if((result & UNINSTANTIABLE_JSB) != 0) {
                        /* Notify ServiceProvisionListener of failure */
                        request.listener.uninstantiable(request);
                        resubmitted = false;
                        if(provisionLogger.isLoggable(Level.FINE))
                            provisionLogger.log(Level.FINE,
                                                "Service ["+
                                                request.sElem.getName()+"] "+
                                                "is un-instantiable, " +
                                                "do not resubmit");
                    } else if((result & BAD_CYBERNODE) != 0) {
                        /* Provision request encountered a bad Cybernode,
                         * dispatch another request immediately. Note the
                         * subsequent provision request occurs in a separate
                         * thread */
                        dispatch(request);
                        return;
                    } else {
                        if(pendingMgr != null) {
                            if(request.type==ProvisionRequest.Type.PROVISION) {
                                pendingMgr.addProvisionRequest(request, index);
                                if(provisionLogger.isLoggable(Level.FINE))
                                    provisionLogger.log(Level.FINE,
                                                        "Re-submitted ["+
                                                        request.sElem.getName()+
                                                        "] to " +
                                                        pendingMgr.getType());
                            }
                        }
                    }
                    /* Send a ProvisionFailureEvent */
                    if(thrown != null || result != 0) {
                        processProvisionFailure(
                                          new ProvisionFailureEvent(
                                                        eventSource,
                                                        request.sElem,
                                                        failureReason,
                                                        thrown));
                    }
                    /* If we have a ServiceProvisionListener,
                     * notify the listener */
                    if(request.svcProvisionListener!=null) {
                        try {
                            request.svcProvisionListener.failed(
                                                         request.sElem,
                                                         resubmitted);
                        } catch(Exception e) {
                            Throwable t = e;
                            if(e.getCause() != null)
                                t = e.getCause();
                            if(provisionLogger.isLoggable(Level.FINEST)) {
                                provisionLogger.log(
                                    Level.FINEST,
                                    "Error Notifying "+
                                    "ServiceProvisionListeners "+
                                    "on failure "+
                                    "["+t.getClass().getName()+":"+
                                    t.getLocalizedMessage()+"], this is " +
                                    "usually a benign error and indicates " +
                                    "that the ServiceProvisionListener gave " +
                                    "up listening");
                            }
                        }
                    }
                } else {
                    if(jsbInstance==null) {
                        if(provisionLogger.isLoggable(Level.FINER)) {
                            String addr =
                                ((InstantiatorResource)svcResource.
                                    getResource()).getHostAddress();
                            String name =
                                ((InstantiatorResource)svcResource.
                                    getResource()).getName();
                            provisionLogger.log(Level.FINER,
                                                name+" at ["+addr+"] " +
                                                "did not allocate ["+
                                                request.sElem.getName()+"], "+
                                                "service limit assumed to " +
                                                "have been met");
                        }
                        return;
                    }
                    /* Notify ServiceProvisionListener of success */
                    request.listener.serviceProvisioned(
                               jsbInstance,
                               (InstantiatorResource)svcResource.getResource());
                    /* If we have an ServiceBeanInstantiatorListener, 
                     * notify the listener */
                    if(request.svcProvisionListener!=null) {
                        try {
                            request.svcProvisionListener.succeeded(jsbInstance);
                        } catch(Exception e) {
                            Throwable cause = ThrowableUtil.getRootCause(e);
                            if(provisionLogger.isLoggable(Level.FINEST)) {
                                provisionLogger.log(Level.FINEST,
                                                    "Notifying " +
                                                    "ServiceProvisionListeners " +
                                                    "on success. "+
                                                    "["+cause.getClass().getName()+":" +
                                                    cause.getLocalizedMessage()+"]");
                            }
                        }
                    }
                }
            } finally {
                inProcess.remove(request.sElem);
            }
        }

        int doProvision(ProvisionRequest request,
                        ServiceResource serviceResource) {
            long start = System.currentTimeMillis();
            int result = 0;
            InstantiatorResource ir =
                (InstantiatorResource)serviceResource.getResource();
            try {
                try {
                    ServiceProvisionEvent event =
                        new ServiceProvisionEvent(eventSource,
                                                  request.opStringMgr,
                                                  request.sElem);
                    event.setSequenceNumber(sequenceNumber);
                    event.setHandback(ir.getHandback());
                    /*
                     * Put the instantiate invocation in a for loop, the
                     * Cybernode may return null if there is a race-condition
                     * where a service is terminated, the FDH notifies the
                     * ServiceElementManager of the failure, the
                     * ProvisionRequest is dispatched (we get to this point),
                     * and the Cybernode is in the process of doing
                     * housekeeping related to service termination. If we get
                     * a null returned, wait the specified time and retry
                     */
                    int numProvisionRetries = 3;
                    for(int i=0; i<numProvisionRetries; i++) {
                        if(provisionLogger.isLoggable(Level.FINER)) {
                            String retry = (i==0?"":", retry ("+i+") ");
                            provisionLogger.log(Level.FINER,
                                                "Allocating "+retry+"["+
                                                request.sElem.getOperationalStringName()+
                                                "/"+request.sElem.getName()+
                                                "] ...");
                        }
                        DeployedService deployedService =
                            ir.getInstantiator().instantiate(event);
                        if(deployedService !=null) {
                            jsbInstance = deployedService.getServiceBeanInstance();
                            ir.addDeployedService(deployedService);
                            sequenceNumber++;
                            if(provisionLogger.isLoggable(Level.INFO))
                                provisionLogger.log(Level.INFO,
                                                    "Allocated ["+
                                                    request.sElem.getOperationalStringName()+
                                                    "/"+request.sElem.getName()+
                                                    "]");
                            if(provisionLogger.isLoggable(Level.FINEST)) {
                                Object service = jsbInstance.getService();
                                Class serviceClass = service.getClass();
                                provisionLogger.log(
                                    Level.FINEST,
                                    "{0} ServiceBeanInstance {1}, "+
                                    "Annotation {2}",
                                    new Object[] {
                                        request.sElem.getName(),
                                        jsbInstance,
                                        java.rmi.server.RMIClassLoader.
                                            getClassAnnotation(serviceClass)});
                            }
                            break;
                        } else {
                            if(provisionLogger.isLoggable(Level.FINER))
                                provisionLogger.log(Level.FINER,
                                                    ir.getName()+" at ["+
                                                    ir.getHostAddress()+
                                                    "] did not allocate ["+
                                                    request.sElem.getOperationalStringName()+
                                                    "/"+request.sElem.getName()+
                                                    "], "+
                                                    "retry ...");
                            long retryWait = 1000;
                            try {
                                Thread.sleep(retryWait);
                            } catch (InterruptedException ie) {
                                if(logger.isLoggable(Level.FINEST))
                                    logger.log(Level.FINEST,
                                               "Interrupted while sleeping "+
                                               "["+retryWait+"] millis"+
                                               "for provision retry",
                                               ie);
                            }
                        }
                    }

                } catch(UnknownEventException e) {
                    result = PROVISION_FAILURE;
                    failureReason = e.getLocalizedMessage();
                    logger.severe(failureReason);
                    thrown = e;
                    selector.dropServiceResource(serviceResource);
                } catch(RemoteException e) {
                    result = PROVISION_FAILURE;
                    Throwable t = ThrowableUtil.getRootCause(e);
                    failureReason = t.getLocalizedMessage();
                    thrown = e;                    
                } catch(JSBInstantiationException e) {
                    if(e.isUninstantiable())
                        result = PROVISION_FAILURE | UNINSTANTIABLE_JSB;
                    else
                        result = PROVISION_FAILURE;
                    thrown = e;
                    Throwable t = ThrowableUtil.getRootCause(e);
                    failureReason = t.getLocalizedMessage();

                } catch(Throwable t) {
                    result = PROVISION_FAILURE | UNINSTANTIABLE_JSB;
                    thrown = t;
                    t = ThrowableUtil.getRootCause(t);
                    failureReason = t.getLocalizedMessage();
                }
            } finally {
                if (thrown != null) {
                    if(!ThrowableUtil.isRetryable(thrown)) {
                        if(provisionLogger.isLoggable(Level.INFO))
                            provisionLogger.log(
                                Level.INFO,
                                "Drop {0} {1} from collection",
                                new Object[] {
                                    ir.getName(),
                                    ir.getInstantiator()});
                        selector.dropServiceResource(serviceResource);
                        result = PROVISION_FAILURE | BAD_CYBERNODE;
                    } else {
                        if(logger.isLoggable(Level.FINEST))
                            provisionLogger.log(Level.WARNING,
                                                "Provisioning ["+
                                                request.sElem.getOperationalStringName()+
                                                "/"+request.sElem.getName()+"] "+
                                                "to ["+ir.getHostAddress()+"]",
                                                thrown);
                        else
                            provisionLogger.warning("Provisioning ["+
                                                    request.sElem.getOperationalStringName()+
                                                    "/"+request.sElem.getName()+"] to " +
                                                    "["+ir.getHostAddress()+"], "+
                                                    thrown.getClass().getName()+": "+
                                                    thrown.getLocalizedMessage());
                    }

                }
                ir.decrementProvisionCounter(request.sElem);
                long stop = System.currentTimeMillis();
                watch.addValue(stop-start);
            }
            return(result);
        }
    }

    /**
     * This class is used to manage the provisioning of pending ServiceElement 
     * objects that have a ServiceProvisionManagement type of DYNAMIC.
     */
    class PendingManager extends PendingServiceElementManager {
        /** Create a PendingManager */
        PendingManager() {
            super("Dynamic-Service TestManager");
        }

        /**
         * Override parent's getCount to include the number of in-process elements 
         * in addition to the number of pending ServiceElement instances*/
        int getCount(ServiceElement sElem) {
            int count = super.getCount(sElem);
            ServiceElement[] elems =
                inProcess.toArray(new ServiceElement[inProcess.size()]);
            for(Object elem : elems) {
                if(elem.equals(sElem))
                    count++;
            }
            return(count);
        }

        /** Process the pending collection */
        void process() {
            int pendingSize = getSize();
            if(logger.isLoggable(Level.FINE) && pendingSize > 0) {
                dumpCollection();
            }
            if(pendingSize == 0)
                return;
            try {
                Key [] keys;
                synchronized (collection) {
                    Set<Key> keySet = collection.keySet();
                    keys = keySet.toArray(new Key[keySet.size()]);
                }
                for(Key key : keys) {
                    ProvisionRequest request;
                    ServiceResource resource = null;
                    synchronized (collection) {
                        request = collection.get(key);
                        if(request!=null && request.sElem!=null) {
                            resource = acquireServiceResource(request);
                            if(resource!=null) {
                                synchronized (collection) {
                                    collection.remove(key);
                                }
                            }
                        }
                    }

                    if(resource==null)
                        continue;
                    try {
                        dispatch(request, resource, key.index);
                    } catch(Exception e) {
                        if(logger.isLoggable(Level.FINEST))
                            logger.log(Level.FINEST,
                                       "Dispatching Pending " +
                                       "Collection Element",
                                       e);
                    }
                    /* Slow the dispatching down, this will avoid pummeling
                     * a single InstantiatorResoource */
                    Thread.sleep(500);
                }
            } catch(Throwable t) {
                logger.log(Level.WARNING,
                           "Processing Pending Collection",
                           t);
            }
        }
    } // End PendingManager

    /**
     * This class is used to manage the provisioning of ServiceElement
     * objects that have a ServiceProvisionManagement type of FIXED.
     */
    class FixedServiceManager extends PendingServiceElementManager {
        //Map instanceMap = new HashMap();
        List<ServiceResource> inProcessResource =
            Collections.synchronizedList(new ArrayList<ServiceResource>());

        /** Create a FixedServiceManager */
        FixedServiceManager() {
            super("Fixed-Service TestManager");
        }

        /**
         * Process the entire Fixed collection over all known ServiceResource 
         * instances
         */
        void process() {
            ServiceResource[] resources = selector.getServiceResources();
            for(ServiceResource resource : resources)
                process(resource);
        }

        /**
         * Used to deploy a specific ProvisionRequest to all ServiceResource 
         * instances that support the requirements of the ServiceElement
         * 
         * @param request The ProvisionRequest to deploy
         */
        void deploy(ProvisionRequest request) {
            try {
                if(provisionLogger.isLoggable(Level.FINER))
                    provisionLogger.log(Level.FINER,
                                        "Deploy ["+
                                        request.sElem.getOperationalStringName()+
                                        "/"+request.sElem.getName()+"]");
                ServiceResource[] resources =
                    selector.getServiceResources(request.sElem);
                /* Filter out isolated associations and max per machine levels
                 * set at the physical level */
                resources = selector.filterMachineBoundaries(request.sElem, resources);
                if(resources.length>0)
                    resources = selector.filterIsolated(request.sElem, resources);

                if(resources.length > 0) {
                    for(ServiceResource resource : resources) {
                        try {
                            inProcessResource.add(resource);
                            doDeploy(resource, request);
                        } finally {
                            inProcessResource.remove(resource);
                        }
                    }
                    while(provisioningPool.getActiveCount()>0) {
                        Thread.sleep(100);
                    }
                    //if(list.size()>0)
                    //    taskJoiner(list);
                }
            } catch(Throwable t) {
                provisionLogger.log(Level.WARNING,
                                    "FixedServiceManager deployNew",
                                    t);
            }
        }

        /* Process the fixed services collection with an input
         * ServiceResource */
        void process(ServiceResource resource) {
            if(resource == null)
                throw new IllegalArgumentException("ServiceResource is null");
            if(inProcessResource.contains(resource))
                return;
            inProcessResource.add(resource);
            InstantiatorResource ir =
                (InstantiatorResource)resource.getResource();
            try {
                if(getSize() == 0)
                    return;
                if(logger.isLoggable(Level.FINEST)) {
                    dumpCollection();
                }
                /* Now traverse the collection for everything else, skipping
                 * the service elements that have been processed */
                synchronized(collection) {
                    Set<Key> requests = collection.keySet();
                    for (Key requestKey : requests) {
                        ProvisionRequest request = collection.get(requestKey);
                        try {
                            if (clearedMaxPerMachineAndIsolated(request,
                                                                ir.getHostAddress()) &&
                                ir.canProvision(request.sElem))
                                doDeploy(resource, request);
                        } catch (ProvisionException e) {
                            request.type = ProvisionRequest.Type.UNINSTANTIABLE;
                            if(provisionLogger.isLoggable(Level.FINE))
                                provisionLogger.log(Level.FINE,
                                                    "Service ["+
                                                    request.sElem.getOperationalStringName()+
                                                    "/"+request.sElem.getName()+"] "+
                                                    "is un-instantiable, " +
                                                    "do not resubmit");
                        }
                    }
                    while(provisioningPool.getActiveCount()>0) {
                        Thread.sleep(100);
                    }
                    //if(list.size()>0)
                    //    taskJoiner(list);
                }

            } catch(Throwable t) {
                logger.log(Level.WARNING,
                           "Processing FixedService Collection",
                           t);
            } finally {
                inProcessResource.remove(resource);
                ir.setDynamicEnabledOn();
            }
        }

        /**
         * Do the deployment for a fixed service
         * 
         * @param resource The ServiceResource
         * @param request The ProvisionRequest
         *
         * @return The number deployed
         * 
         * @throws Exception If there are errors
         */
        int doDeploy(ServiceResource resource,
                     ProvisionRequest request) throws Exception {
            return(doDeploy(resource, request, true));
        }

        /**
         * Do the deployment for a fixed service
         * 
         * @param resource The ServiceResource
         * @param req The ProvisionRequest
         * @param changeInstanceID If true, increment the instanceID
         * 
         * @return The number deployed
         *
         * @throws Exception If there are errors
         */
        int doDeploy(ServiceResource resource,
                     ProvisionRequest req,
                     boolean changeInstanceID) throws Exception {
            int numAllowed = getNumAllowed(resource, req);
            if(numAllowed>0) {
                long currentID = req.sElem.getServiceBeanConfig().getInstanceID();
                StringBuffer b = new StringBuffer();
                b.append("doDeploy ").
                    append(numAllowed).
                    append(" " + "[").
                    append(req.sElem.getOperationalStringName()).
                    append("/").
                    append(req.sElem.getName()).
                    append("] instances");
                
                for(int i=0; i<numAllowed; i++) {
                    ProvisionRequest request = ProvisionRequest.copy(req);
                    long nextID = (changeInstanceID?
                                   request.instanceIDMgr.getNextInstanceID() :
                                   currentID);
                    request.sElem = ServiceElementUtil.prepareInstanceID(
                                                        request.sElem,
                                                        true,
                                                        nextID);

                    if(provisionLogger.isLoggable(Level.FINEST))
                        provisionLogger.log(
                            Level.FINEST,
                            "["+request.sElem.getOperationalStringName()+
                            "/"+request.sElem.getName()+"] "+
                            "instanceID : "+
                            request.sElem.
                                getServiceBeanConfig().
                                getInstanceID());

                    /*b.append("instanceID=").
                        append(request.sElem.getServiceBeanConfig().getInstanceID()).
                        append(", " + "changeInstanceID=").
                        append(changeInstanceID).
                        append("\n");*/

                    inProcess.add(request.sElem);
                    InstantiatorResource ir =
                        (InstantiatorResource)resource.getResource();
                    ir.incrementProvisionCounter(request.sElem);
                    provisioningPool.execute(new ProvisionTask(request, resource));
                }
                if(logger.isLoggable(Level.FINER))
                    logger.finer(b.toString());
                //if(!changeInstanceID) {
                //    Throwable t = new Throwable();
                //    t.printStackTrace();
                //}
            }
            return(numAllowed);
        }

        /*
         * Determine how many services can be allocated based on how many
         * are already on the resource minus the number planned
         */
        int getNumAllowed(ServiceResource resource, ProvisionRequest request) {
            /* Filter out isolated associations and max per machine levels set
             * at the physical level */
            InstantiatorResource ir =
                (InstantiatorResource)resource.getResource();
            if(!clearedMaxPerMachineAndIsolated(request, ir.getHostAddress()))
                return 0;
            
            int planned = request.sElem.getPlanned();
            int actual = ir.getServiceElementCount(request.sElem);
            int numAllowed = planned-actual;
            if(request.sElem.getMaxPerMachine()!=-1 &&
               request.sElem.getMaxPerMachine()<numAllowed)
                numAllowed = request.sElem.getMaxPerMachine();
            return(numAllowed);                         
        }

        /*
         * Filter out isolated associations and max per machine levels set
         * at the physical level
         */
        boolean clearedMaxPerMachineAndIsolated(ProvisionRequest request,
                                                String hostAddress) {
            /* Filter out isolated associations and max per machine levels set
             * at the physical level */
            ServiceResource[] sr =
                selector.filterMachineBoundaries(request.sElem,
                                                 selector.getServiceResources(hostAddress,
                                                                              true));
            if(sr.length==0)
                return false;
            sr = selector.filterIsolated(request.sElem, sr);
            return sr.length != 0;
        }

        /*
         * Wait until all ProvisionTask threads are complete
         *
        void taskJoiner(ArrayList list) {
            if(provisionLogger.isLoggable(Level.INFO))
                provisionLogger.log(Level.INFO,
                                    "Wait until all ProvisionTask threads " +
                                    "are complete ...");
            for (Object aList : list) {
                try {
                    ((PoolableThread) aList).joinResource();
                } catch (Throwable e) {
                    provisionLogger.log(Level.WARNING,
                                        "PoolableThread join interruption",
                                        e);
                }
            }
            if(logger.isLoggable(Level.FINEST))
                provisionLogger.log(Level.FINEST,
                                    "ProvisionTask threads join complete");
        }
        */
    } // End FixedServiceManager

    /*
     * Helper method to obtain a PoolableThread and create a 
     * ProvisionFailureEventTask to send a ProvisionFailureEvent
     */
    void processProvisionFailure(ProvisionFailureEvent event) {
        provisionFailurePool.execute(new ProvisionFailureEventTask(event));
    }

    /**
     * This class is used as by a <code>PoolableThread</code> to notify registered
     * event consumers of a ProvisionFailureEvent
     */
    class ProvisionFailureEventTask implements Runnable {
        ProvisionFailureEvent event;

        ProvisionFailureEventTask(ProvisionFailureEvent event) {
            this.event = event;
        }

        public void run() {
            try {
                failureHandler.fire(event);
            } catch(Exception e) {
                logger.log(Level.WARNING,
                           "Exception notifying ProvisionFailureEvent consumers", e);
            }

        }
    }

    /**
     * Monitors ServiceBeanInstantiator leases being removed. 
     */
    class LeaseMonitor extends LeaseListenerAdapter {
        public void removed(LeasedResource resource) {
            InstantiatorResource ir =
                (InstantiatorResource)((ServiceResource)resource).getResource();
            int instantiatorCount = landlord.total();
            if(logger.isLoggable(Level.FINE)) {
                logger.log(Level.FINE,
                           "{0} @ {1} removed, count now [{2}]",
                           new Object[]{
                               ir.getName(),
                               ir.getResourceCapability().getAddress(),
                               instantiatorCount});
            }
        }
    }
}


