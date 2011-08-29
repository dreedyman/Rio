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
package org.rioproject.cybernode;

import com.sun.jini.config.Config;
import com.sun.jini.constants.ThrowableConstants;
import net.jini.admin.Administrable;
import net.jini.config.Configuration;
import net.jini.config.ConfigurationException;
import net.jini.core.event.EventRegistration;
import net.jini.core.lease.Lease;
import net.jini.core.lookup.ServiceID;
import net.jini.core.lookup.ServiceItem;
import net.jini.core.lookup.ServiceTemplate;
import net.jini.discovery.DiscoveryManagement;
import net.jini.lookup.LookupCache;
import net.jini.lookup.ServiceDiscoveryEvent;
import net.jini.security.BasicProxyPreparer;
import net.jini.security.ProxyPreparer;
import org.rioproject.deploy.ProvisionManager;
import org.rioproject.deploy.DeployedService;
import org.rioproject.deploy.ServiceBeanInstantiator;
import org.rioproject.resources.client.LookupCachePool;
import org.rioproject.resources.client.ServiceDiscoveryAdapter;
import org.rioproject.resources.util.ThrowableUtil;
import org.rioproject.system.MeasuredResource;
import org.rioproject.util.TimeUtil;
import org.rioproject.system.ComputeResource;
import org.rioproject.system.ResourceCapability;

import java.io.IOException;
import java.rmi.MarshalledObject;
import java.rmi.RemoteException;
import java.security.AccessControlException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The ServiceConsumer manages the discovery, registration and update of the
 * {@link org.rioproject.system.ResourceCapability} component to discovered
 * {@link org.rioproject.deploy.ProvisionManager} instances
 *
 * @author Dennis Reedy
 */
public class ServiceConsumer extends ServiceDiscoveryAdapter {
    /** The maximum number of services the Cybernode has been configured to 
     * instantiate */
    int serviceLimit;
    CybernodeAdapter adapter;
    /** Table of Lease instances to manage */
    final Hashtable<Object, ProvisionLeaseManager> leaseTable;
    /** Collection of ProvisionMonitor instances */
    final List<ServiceID> provisioners =
        Collections.synchronizedList(new ArrayList<ServiceID>());
    /** LookupCache for ProvisionMonitor instances */
    LookupCache lCache;
    /**
     * The duration of the Lease requested by the ServiceInstantiator to
     * ProvisionManager instances
     */
    long provisionerLeaseDuration;
    /**
     * The number of times to attempt to reconnect to a ProvisionManager
     * instance if that instance could not be reached for Lease renewal.
     */
    int provisionerRetryCount;
    /**
     * The length of time (in milliseconds) to wait before attempting to
     * reconnect to a ProvisionManager instance if that instance could not be
     * reached for LeaseRenewal
     */
    long provisionerRetryDelay;
    /** ProxyPreparer for ProvisionManager proxies */
    ProxyPreparer provisionerPreparer;
    /** Configuration object */
    Configuration config;
    /** Observer for ComputeResource changes */
    ComputeResourceObserver computeResourceObserver;
    /* Flag to indicate we are destroyed */
    boolean destroyed = false;
    /** Logger */
    static Logger logger = CybernodeImpl.logger;

    /**
     * Construct a ServiceConsumer
     * 
     * @param adapter The CybernodeAdapter
     * @param serviceLimit The maximum number of services the Cybernode has been 
     * configured to instantiate 
     * @param config The Configuration object used to obtain operational
     * values
     *
     * @throws ConfigurationException if errors occur accessing the configuration
     */
    ServiceConsumer(CybernodeAdapter adapter,
                    int serviceLimit,
                    Configuration config) throws ConfigurationException {
        if(adapter == null)
            throw new NullPointerException("CybernodeAdapter is null");
        if(config == null)
            throw new NullPointerException("config is null");
        this.adapter = adapter;
        this.config = config;
        /* Establish the lease duration */
        long ONE_MINUTE = 1000*60;
        long DEFAULT_LEASE_TIME = ONE_MINUTE*30; /* 30 minutes */
        long MIN_LEASE_TIME = 10*1000; /* 10 seconds */
        provisionerLeaseDuration = Config.getLongEntry(config, 
                                                       CybernodeImpl.CONFIG_COMPONENT,
                                                       "provisionerLeaseDuration", 
                                                       DEFAULT_LEASE_TIME, 
                                                       MIN_LEASE_TIME, 
                                                       Long.MAX_VALUE);
        /* Get the retry count if we disconnect from a provisioner */
        int DEFAULT_RETRY_COUNT = 3;
        int MIN_RETRY_COUNT = 0;
        provisionerRetryCount = Config.getIntEntry(config, 
                                                   CybernodeImpl.CONFIG_COMPONENT,
                                                   "provisionerRetryCount", 
                                                   DEFAULT_RETRY_COUNT, 
                                                   MIN_RETRY_COUNT, 
                                                   Integer.MAX_VALUE);
        /* Get the amount of time to wait between retries */
        long DEFAULT_RETRY_DELAY = 1000; /* 1 second */
        long MIN_RETRY_DELAY = 0; 
        provisionerRetryDelay = Config.getLongEntry(config, 
                                                    CybernodeImpl.CONFIG_COMPONENT,
                                                    "provisionerRetryDelay", 
                                                    DEFAULT_RETRY_DELAY, 
                                                    MIN_RETRY_DELAY, 
                                                    Long.MAX_VALUE);
        if(logger.isLoggable(Level.FINEST))
            logger.log(Level.FINEST,
                       "LeaseDuration={0}, RetryCount={1}, RetryDelay={2}",
                       new Object[] {provisionerLeaseDuration,
                                     provisionerRetryCount,
                                     provisionerRetryDelay});
        
        /* Get the ProxyPreparer for discovered ProvisionMonitor instances */
        provisionerPreparer = 
            (ProxyPreparer)config.getEntry(CybernodeImpl.CONFIG_COMPONENT,
                                           "provisionerPreparer", 
                                           ProxyPreparer.class,
                                           new BasicProxyPreparer());
        if(logger.isLoggable(Level.FINEST))
            logger.log(Level.FINEST, "ProxyPreparer={0}", provisionerPreparer);
        leaseTable = new Hashtable<Object, ProvisionLeaseManager>();
        this.serviceLimit = serviceLimit;
        computeResourceObserver = new ComputeResourceObserver(adapter.getComputeResource());
    }

    /**
     * Start the discovery of Provisioners
     * 
     * @param dm The DiscoveryManagement to use
     *
     * @throws IOException if discovery cannot be initialized
     * @throws ConfigurationException if the configuration cannot be used
     */
    void initializeProvisionDiscovery(DiscoveryManagement dm) throws
        IOException, ConfigurationException {
        ServiceTemplate template =
            new ServiceTemplate(null,
                                new Class[] {ProvisionManager.class},
                                null);
        LookupCachePool lcPool = LookupCachePool.getInstance();
        lCache = lcPool.getLookupCache(dm, template);
        lCache.addListener(this);
    }

    /**
     * Destroy the ServiceConsumer
     */
    void destroy() {
        try {
            adapter.getComputeResource().deleteObserver(computeResourceObserver);
            lCache.removeListener(this);
            cancelRegistrations();
            synchronized(provisioners) {
                provisioners.clear();
            }
        } finally {
            destroyed = true;
        }
    }   
    
    /** 
     * Set the serviceLimit property
     * 
     * @param serviceLimit The maximum number of services the Cybernode has been 
     * configured to instantiate 
     */
    void setServiceLimit(int serviceLimit) {
        this.serviceLimit = serviceLimit;
    }

    /**
     * Notification that a Provisioner has been discovered
     * 
     * @param sdEvent The ServiceDiscoveryEvent
     */
    public void serviceAdded(ServiceDiscoveryEvent sdEvent) {
        // sdEvent.getPreEventServiceItem() == null                
        ServiceItem item = sdEvent.getPostEventServiceItem();
        if(logger.isLoggable(Level.FINEST)) {
            if(item==null) {
                logger.finest("ServiceItem is=NULL");
            } else {
                logger.finest(item.toString()+"item.service="+
                              (item.service==null?"NULL":item.service.toString()));
            }
        }
        if(item == null || item.service == null)
            return;
        synchronized(provisioners) {
            if(provisioners.contains(item.serviceID))
                return;
            logger.log(Level.INFO, 
                       "ProvisionManager discovered {0}",
                       new Object[] {item.service.toString()});
            provisioners.add(item.serviceID);
        }
        register(item);
    }

    /**
     * Notification that a Provisioner has been removed
     * 
     * @param sdEvent The ServiceDiscoveryEvent
     */
    public void serviceRemoved(ServiceDiscoveryEvent sdEvent) {
        ServiceItem item = sdEvent.getPreEventServiceItem();
        /* Check if provisioner is actually no longer available */
        try {
            ((Administrable)item.service).getAdmin();            
        } catch (RemoteException e) {
            removeProvisionManager((ProvisionManager)item.service,
                                   item.serviceID);
        }
    }

    /*
     * Remove a ProvisionManager from the collection
     */
    void removeProvisionManager(ProvisionManager pm, ServiceID sid) {
        lCache.discard(pm);
        synchronized(provisioners) {
            if(provisioners.remove(sid)) {
                if (logger.isLoggable(Level.INFO))
                    logger.log(Level.INFO,
                               "Dropping ProvisionManager, now connected " +
                               "to ["+provisioners.size()+"] " +
                               "ProvisionMonitor instances");
            }
        }
        cancelRegistration(pm);
    }

    /**
     * Register to a Provisioner
     * 
     * @param item The ServiceItem of a discovered Provisioner
     */
    void register(ServiceItem item) {
        try {
            if(haveRegistration(item)) {
                if(logger.isLoggable(Level.FINEST))
                    logger.log(Level.FINEST, 
                               "Already registered to {0}",
                               new Object[] {item.service});
                return;                
            }
            ProvisionManager provisioner = 
                (ProvisionManager)provisionerPreparer.prepareProxy(item.service);
            if(logger.isLoggable(Level.FINEST))
                logger.log(Level.FINEST,
                           "ServiceConsumer - prepared ProvisionManager proxy: {0}",
                           provisioner);
            ResourceCapability rCap = adapter.getResourceCapability();
            if(logger.isLoggable(Level.FINEST))
                logger.log(Level.FINEST,
                           "ResourceCapability {0}",
                           new Object[] {rCap});

            Lease lease = connect(provisioner);
            if(lease==null) {
                logger.log(Level.WARNING,
                           "Unable to register to ProvisionManager {0}",
                           new Object[] {provisioner.toString()});
                return;
            }            
            leaseTable.put(item.service, 
                           new ProvisionLeaseManager(lease,
                                                     provisioner,
                                                     item.serviceID));
            logger.log(Level.INFO, "Registered to a ProvisionManager");
                        
        } catch(Throwable t) {
            provisioners.remove(item.serviceID);
            logger.log(Level.SEVERE, "Registering ProvisionManager", t);
        }
    }

    /**
     * Cancel all event registrations to Provisioner instances
     */
    void cancelRegistrations() {
        for(Enumeration e = leaseTable.keys();
            e.hasMoreElements();) {
            Object service = e.nextElement();
            cancelRegistration(service);
        }
    }

    /**
     * Cancel the registration to a monitor
     *
     * @param monitor The monior to cancel
     */

    void cancelRegistration(Object monitor) {
        synchronized(leaseTable) {
            ProvisionLeaseManager leaseManager = leaseTable.remove(monitor);
            if(leaseManager != null) {
                leaseManager.drop(false);
            }
        }
    }

    /**
     * Update all known Provisioners
     */
    void updateMonitors() {
        updateMonitors(adapter.getResourceCapability(),
                       adapter.getDeployedServices());
    }

    /**
     * Update all known Provisioners with new serviceLimit value
     *
     * @param serviceLimit The maximum number of services the Cybernode has been 
     * configured to instantiate 
     */
    void updateMonitors(int serviceLimit) {
        setServiceLimit(serviceLimit);
        updateMonitors(adapter.getResourceCapability(),
                       adapter.getDeployedServices());
    }

    /**
     * Update all known Provisioners of the new ResourceCapability
     * 
     * @param resourceCapability The ResourceCapability object
     * @param deployedServices List of deployed services
     */
    void updateMonitors(ResourceCapability resourceCapability, List<DeployedService> deployedServices) {
        ProvisionLeaseManager[] mgrs;
        synchronized(leaseTable) {
            Collection<ProvisionLeaseManager> c = leaseTable.values();
            mgrs = c.toArray(new ProvisionLeaseManager[c.size()]);
        }
        if(mgrs == null)
            return;
        if(mgrs.length == 0)
            return;
        StringBuilder sb = new StringBuilder();
        sb.append("Deployed: ").append(deployedServices.size()).append(" Limit: ").append(serviceLimit);
        if(!resourceCapability.measuredResourcesWithinRange()) {
            for(MeasuredResource mr : resourceCapability.getMeasuredResources(ResourceCapability.MEASURED_RESOURCES_BREACHED)) {
                sb.append(mr.getIdentifier())
                    .append("\n\tBREACHED value: ").append(mr.getValue())
                    .append(", threshold: ").append(mr.getThresholdValues().getHighThreshold());
            }
        } else{
            sb.append(", All Measured Resources within range");
        }
        logger.info(sb.toString());

        for (ProvisionLeaseManager mgr : mgrs) {
            try {
                mgr.provisioner.update(adapter.getInstantiator(), resourceCapability, deployedServices, serviceLimit);
            } catch (Throwable t) {
                if (logger.isLoggable(Level.FINEST))
                    logger.log(Level.FINEST, "Updating ProvisionManager", t);
                boolean connected = false;

                /* Determine if we should even try to reconnect */
                final int category = ThrowableConstants.retryable(t);
                if (category == ThrowableConstants.INDEFINITE ||
                    category == ThrowableConstants.UNCATEGORIZED) {
                    connected = mgr.reconnect();
                }

                if (!connected) {
                    removeProvisionManager(mgr.provisioner, mgr.serviceID);
                }
            }
        }
    }
    
    /**
     * Attempt to connect to the ProvisionMonitor
     * 
     * @param provisioner The provision monitor to connect to
     * 
     * @return The Lease the ProvisionMonitor has returned, or null if a valid 
     * Lease could not be obtained
     */
    synchronized Lease connect(ProvisionManager provisioner) {
        boolean connected = false;
        Lease lease = null;
        for(int i = 1; i <= provisionerRetryCount; i++) {
            try {
                EventRegistration er = 
                    provisioner.register(new MarshalledObject<ServiceBeanInstantiator>(adapter.getInstantiator()),
                                         null,
                                         adapter.getResourceCapability(),
                                         getServiceDeployments(),
                                         serviceLimit,
                                         provisionerLeaseDuration);                
                lease = (Lease)provisionerPreparer.prepareProxy(er.getLease());
                long leaseTime = lease.getExpiration() - System.currentTimeMillis();
                if(leaseTime>0) {
                    if(logger.isLoggable(Level.FINE))
                        logger.log(Level.FINE, "Established ProvisionManager registration");
                    connected = true;
                    break;
                } else {
                    logger.log(Level.WARNING,
                               "Invalid Lease time ["+leaseTime+"] returned from "+
                               "ProvisionManager, retry count ["+i+"]");
                    try {
                        lease.cancel();
                    } catch(Exception e ) {
                        if(logger.isLoggable(Level.FINEST))
                            logger.log(Level.FINEST, 
                                       "Cancelling Lease with invalid lease time",
                                       e);
                    }
                    try {
                        Thread.sleep(provisionerRetryDelay);
                    } catch(InterruptedException ie) {
                        /* should not happen */
                    }
                }

            } catch(SecurityException e) {
                //cancelRegistration(provisioner);
                logger.log(Level.WARNING,
                           "ProvisionManager security exception",
                           e);
                break;
            } catch(Exception e) {
                Throwable cause = ThrowableUtil.getRootCause(e);
                logger.warning("Recovering ProvisionManager Lease attempt "+
                               "retry count ["+i+"] "+
                               cause.getClass().getName()+": "+cause.getMessage());
                /* Determine if we should even try to reconnect */
                final int category = ThrowableConstants.retryable(e);
                if(category==ThrowableConstants.INDEFINITE ||
                   category==ThrowableConstants.UNCATEGORIZED) {
                    try {
                        Thread.sleep(provisionerRetryDelay);
                    } catch(InterruptedException ie) {
                        /* should not happen */
                    }
                }
            }
        }
        /* If we're not connected, set lease to null and return */
        if(!connected)
            lease=null;                    
        return(lease);
    }        

    /**
     * Verify we arent already registered to a Provisioner
     *
     * @param item The ServiceItem for the provisioner
     *  
     * @return true if we already have a registration
     */
    boolean haveRegistration(ServiceItem item) {
        for(Enumeration e = leaseTable.keys(); e.hasMoreElements();) {
            Object service = e.nextElement();
            if(service.equals(item.service))
                return(true);
        }
        return(false);
    }

    /*
     * Get the DeployedService instances
     */
    List<DeployedService> getServiceDeployments() {
        return adapter.getDeployedServices();
    }

    /**
     * Use customized Lease renewal to manage leases to ProvisionManager
     * instances. This is needed because leases constructed to ProvisionManager
     * instamces may be shorter then 5 minutes. If we use LeaseRenewalManager
     * and the leases are shorter then 5 minutes, then after 5 minutes the lease
     * is allowed to expire.
     */
    class ProvisionLeaseManager extends Thread {
        long leaseTime;
        boolean keepAlive = true;
        Lease lease;
        ProvisionManager provisioner;
        ServiceID serviceID;

        ProvisionLeaseManager(Lease lease,
                              ProvisionManager provisioner,
                              ServiceID serviceID) {
            super("ProvisionLeaseManager");
            this.lease = lease; 
            leaseTime = lease.getExpiration() - System.currentTimeMillis();            
            if(logger.isLoggable(Level.FINEST))
                logger.log(Level.FINEST, 
                           "ProvisionMonitor Lease expiration : "+
                           "["+lease.getExpiration()+"]  millis, "+
                           "["+(lease.getExpiration()/1000)+"] seconds, "+
                           "duration : ["+leaseTime+"] millis, "+
                           "["+(leaseTime/1000)+"], secs");
            this.provisioner = provisioner;
            this.serviceID = serviceID;
            setDaemon(true);
            start();
        }

        void drop(boolean removed) {
            if(!removed) {
                try {
                    lease.cancel();
                } catch(AccessControlException e) {
                    logger.log(Level.WARNING,
                               "Permissions problem dropping lease",
                               ThrowableUtil.getRootCause(e));
                } catch(Exception e) {
                    if(logger.isLoggable(Level.FINER))
                        logger.finer("ProvisionLeaseManager: could not drop lease, already cancelled");
                }
            }
            lease = null;
            keepAlive = false;
            interrupt();           
        }

        public void run() {
            long leaseRenewalTime = TimeUtil.computeLeaseRenewalTime(leaseTime);
                       
            while(!interrupted()) {
                if(!keepAlive) {
                    return;
                }                
                try {
                    sleep(leaseRenewalTime);
                } catch(InterruptedException ie) {
                    /* should not happen */
                } catch(IllegalArgumentException iae) {
                    logger.warning("Lease renewal time incorrect : "+
                                   leaseRenewalTime);
                }
                if(lease != null) {
                    try {
                        lease.renew(leaseTime);
                    } catch(Exception e) {
                        /* Determine if we should even try to reconnect */
                        if(!ThrowableUtil.isRetryable(e)) {
                            keepAlive = false;
                            if(logger.isLoggable(Level.FINEST))
                                logger.log(Level.FINEST,
                                           "Unrecoverable Exception renewing"+
                                           "ProvisionManager Lease",
                                           e);
                            if(logger.isLoggable(Level.FINE))
                                logger.log(Level.FINE,
                                           "Unrecoverable Exception renewing"+
                                           "ProvisionManager Lease");
                        }
                        if(keepAlive) {
                            /*
                             * If we failed to renew the Lease we should try and
                             * re-establish comms to the ProvisionManager and get
                             * another Lease
                             */                            
                            if(logger.isLoggable(Level.FINE))
                                logger.log(Level.FINE,
                                           "Could not renew, attempt to "+
                                           "reconnect");
                            boolean connected = reconnect();
                            if(!connected) {
                                logger.log(Level.WARNING,
                                           "Unable to recover ProvisionManager "+
                                           "registration, exiting");
                                break;
                            } 
                            
                        } else {
                            if(logger.isLoggable(Level.FINE))
                                logger.log(Level.FINE,
                                           "No retry attempted, ProvisionMonitor "+
                                           "determined unreachable");
                            break;
                        }                        
                    }
                }
            }
                        
            /* Broken out of loop, make sure we are removed from the 
             * leaseManagerTable */
            Object removed = leaseTable.remove(provisioner);
            if(removed!=null) {
                if(logger.isLoggable(Level.FINE))
                    logger.log(Level.FINE,
                               "Remove ProvisionLeaseManager from leaseManagerTable");    
            }
        }   
        
        /**
         * Attempt to reconnect to the ProvisionMonitor
         * 
         * @return True if reconnected, false if not
         */
        boolean reconnect() {
            if(!keepAlive)
                return(false);                        
            this.lease = connect(provisioner);
            boolean connected = (lease != null);
            /* If we're not connected, set keepAlive flag to false */
            if(!connected)
                keepAlive=false;
            else
                this.leaseTime = lease.getExpiration() - System.currentTimeMillis();
            return(connected);
        }        
    }
    
    /**
     * The ComputeResourceObserver class listens for changes to the ComputeResource
     * component and updates known Provisioners of the change in state
     */
    class ComputeResourceObserver implements Observer {
        ComputeResource computeResource;

        ComputeResourceObserver(ComputeResource computeResource) {
            this.computeResource = computeResource;
            computeResource.addObserver(this);
        }

        /**
         * Notification from the ComputeResource of a change in the
         * ResourceCapability object. Get the updated ResourceCapability and notify
         * all provisioners
         */
        public void update(Observable o, Object arg) {            
            if(!(o instanceof ComputeResource)) {
                logger.log(Level.WARNING,
                           "Observable update is not a ComputeResource, detach");
                           computeResource.deleteObserver(this);
                return;
            }
            if(!destroyed) {
                updateMonitors((ResourceCapability)arg, getServiceDeployments());
            } else {
                logger.warning("Destroyed, but still getting updates from " +
                               "ComputeResource");
            }
        }        
    }
}
