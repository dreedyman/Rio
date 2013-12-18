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
package org.rioproject.cybernode.service;

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
import org.rioproject.deploy.DeployedService;
import org.rioproject.deploy.ProvisionManager;
import org.rioproject.deploy.ServiceBeanInstantiator;
import org.rioproject.impl.client.LookupCachePool;
import org.rioproject.impl.client.ServiceDiscoveryAdapter;
import org.rioproject.impl.system.ComputeResource;
import org.rioproject.impl.system.ResourceCapabilityChangeListener;
import org.rioproject.impl.util.ThrowableUtil;
import org.rioproject.impl.util.TimeConstants;
import org.rioproject.system.MeasuredResource;
import org.rioproject.system.ResourceCapability;
import org.rioproject.util.TimeUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.rmi.MarshalledObject;
import java.rmi.RemoteException;
import java.security.AccessControlException;
import java.text.NumberFormat;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * The ServiceConsumer manages the discovery, registration and update of the
 * {@link org.rioproject.system.ResourceCapability} component to discovered
 * {@link org.rioproject.deploy.ProvisionManager} instances
 *
 * @author Dennis Reedy
 */
public class ServiceConsumer extends ServiceDiscoveryAdapter {
    /** The maximum number of services the Cybernode has been configured to instantiate */
    private int serviceLimit;
    private final CybernodeAdapter adapter;
    /** Table of Lease instances to manage */
    private final ConcurrentMap<ProvisionManager, ProvisionLeaseManager> leaseTable = new ConcurrentHashMap<ProvisionManager, ProvisionLeaseManager>();
    /** Collection of ProvisionMonitor instances */
    private final ConcurrentMap<ServiceID, ProvisionManager> provisionerMap = new ConcurrentHashMap<ServiceID, ProvisionManager>();
    /** LookupCache for ProvisionMonitor instances */
    private LookupCache lCache;
    /**
     * The duration of the Lease requested by the ServiceInstantiator to
     * ProvisionManager instances
     */
    private final long provisionerLeaseDuration;
    /**
     * The number of times to attempt to reconnect to a ProvisionManager
     * instance if that instance could not be reached for Lease renewal.
     */
    private final int provisionerRetryCount;
    /**
     * The length of time (in milliseconds) to wait before attempting to
     * reconnect to a ProvisionManager instance if that instance could not be
     * reached for LeaseRenewal
     */
    private final long provisionerRetryDelay;
    /** ProxyPreparer for ProvisionManager proxies */
    private final ProxyPreparer provisionerPreparer;
    /** Observer for ComputeResource changes */
    private final ComputeResourceObserver computeResourceObserver;
    /* Flag to indicate we are destroyed */
    private boolean destroyed = false;
    private static final String CONFIG_COMPONENT = "org.rioproject.cybernode";
    /** Logger */
    private static final Logger logger = LoggerFactory.getLogger(ServiceConsumer.class.getName());

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
    ServiceConsumer(final CybernodeAdapter adapter, final int serviceLimit, final Configuration config) throws ConfigurationException {
        if(adapter == null)
            throw new IllegalArgumentException("CybernodeAdapter is null");
        if(config == null)
            throw new IllegalArgumentException("config is null");
        this.adapter = adapter;
        /* Establish the lease duration */
        long DEFAULT_LEASE_TIME = TimeConstants.ONE_MINUTE*30; /* 30 minutes */
        long MIN_LEASE_TIME = 10*1000; /* 10 seconds */
        provisionerLeaseDuration = Config.getLongEntry(config,
                                                       CONFIG_COMPONENT,
                                                       "provisionerLeaseDuration", 
                                                       DEFAULT_LEASE_TIME, 
                                                       MIN_LEASE_TIME, 
                                                       Long.MAX_VALUE);
        /* Get the retry count if we disconnect from a provisioner */
        int DEFAULT_RETRY_COUNT = 3;
        int MIN_RETRY_COUNT = 0;
        provisionerRetryCount = Config.getIntEntry(config, 
                                                   CONFIG_COMPONENT,
                                                   "provisionerRetryCount", 
                                                   DEFAULT_RETRY_COUNT, 
                                                   MIN_RETRY_COUNT, 
                                                   Integer.MAX_VALUE);
        /* Get the amount of time to wait between retries */
        long DEFAULT_RETRY_DELAY = 1000; /* 1 second */
        long MIN_RETRY_DELAY = 0; 
        provisionerRetryDelay = Config.getLongEntry(config, 
                                                    CONFIG_COMPONENT,
                                                    "provisionerRetryDelay", 
                                                    DEFAULT_RETRY_DELAY, 
                                                    MIN_RETRY_DELAY, 
                                                    Long.MAX_VALUE);
        logger.trace("LeaseDuration={}, RetryCount={}, RetryDelay={}",
                      provisionerLeaseDuration, provisionerRetryCount, provisionerRetryDelay);
        
        /* Get the ProxyPreparer for discovered ProvisionMonitor instances */
        provisionerPreparer = (ProxyPreparer)config.getEntry(CONFIG_COMPONENT,
                                                             "provisionerPreparer",
                                                             ProxyPreparer.class,
                                                             new BasicProxyPreparer());
        logger.trace("ProxyPreparer={}", provisionerPreparer);
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
    void initializeProvisionDiscovery(final DiscoveryManagement dm) throws IOException, ConfigurationException {
        if(lCache==null) {
            ServiceTemplate template = new ServiceTemplate(null, new Class[] {ProvisionManager.class}, null);
            LookupCachePool lcPool = LookupCachePool.getInstance();
            lCache = lcPool.getLookupCache(dm, template);
            lCache.addListener(this);
        } else {
            for(Map.Entry<ServiceID, ProvisionManager> entry : provisionerMap.entrySet()) {
                register(entry.getKey(), entry.getValue());
            }
        }
    }

    /**
     * Destroy the ServiceConsumer
     */
    void destroy() {
        try {
            adapter.getComputeResource().removeListener(computeResourceObserver);
            if(lCache!=null)
                lCache.removeListener(this);
            cancelRegistrations();
            synchronized(provisionerMap) {
                provisionerMap.clear();
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
    private void setServiceLimit(final int serviceLimit) {
        this.serviceLimit = serviceLimit;
    }

    /**
     * Notification that a Provisioner has been discovered
     * 
     * @param sdEvent The ServiceDiscoveryEvent
     */
    public void serviceAdded(final ServiceDiscoveryEvent sdEvent) {
        // sdEvent.getPreEventServiceItem() == null                
        ServiceItem item = sdEvent.getPostEventServiceItem();
            if(item==null) {
                logger.trace("ServiceItem is=NULL");
            } else {
                logger.trace("{} item.service={}", item.toString(),
                             (item.service==null?"NULL":item.service.toString()));
            }

        if(item == null || item.service == null)
            return;
        synchronized(provisionerMap) {
            if(provisionerMap.get(item.serviceID)!=null)
                return;
            logger.debug("ProvisionManager discovered {}", item.service.toString());
            provisionerMap.put(item.serviceID, (ProvisionManager) item.service);
        }
        register(item.serviceID, (ProvisionManager)item.service);
    }

    /**
     * Notification that a Provisioner has been removed
     * 
     * @param sdEvent The ServiceDiscoveryEvent
     */
    public void serviceRemoved(final ServiceDiscoveryEvent sdEvent) {
        ServiceItem item = sdEvent.getPreEventServiceItem();
        /* Check if provisioner is actually no longer available */
        try {
            ((Administrable)item.service).getAdmin();            
        } catch (RemoteException e) {
            removeProvisionManager((ProvisionManager)item.service, item.serviceID);
        }
    }

    /*
     * Remove a ProvisionManager from the collection
     */
    private void removeProvisionManager(final ProvisionManager pm, final ServiceID sid) {
        provisionerMap.remove(sid);
        cancelRegistration(pm);
        try {
            lCache.discard(pm);
        } catch(IllegalStateException e) {
            logger.warn("For some reason LookupDiscovery has been terminated, non-fatal, must be shutting down", e);
        }
    }

    /**
     * Register to a Provisioner
     * 
     * @param serviceID The ServiceID of a discovered Provisioner
     * @param provisionManager The ProvisionManager to register
     */
    private void register(final ServiceID serviceID, ProvisionManager provisionManager) {
        try {
            if(haveRegistration(provisionManager)) {
                logger.trace("Already registered to {}", provisionManager);
                return;                
            }
            ProvisionManager provisioner = (ProvisionManager)provisionerPreparer.prepareProxy(provisionManager);
            logger.trace("ServiceConsumer - prepared ProvisionManager proxy: {}", provisioner.toString());
            ResourceCapability rCap = adapter.getResourceCapability();
            logger.trace("ResourceCapability {}", rCap);

            Lease lease = connect(provisioner);
            if(lease==null) {
                logger.warn("Unable to register to ProvisionManager {}", provisioner.toString());
                return;
            }            
            leaseTable.put(provisioner, new ProvisionLeaseManager(lease, provisioner, serviceID));
            logger.info("Registered to a ProvisionManager, now connected to [{}] ProvisionMonitor instances",
                        provisionerMap.size());
                        
        } catch(Throwable t) {
            provisionerMap.remove(serviceID);
            logger.error("Registering ProvisionManager", t);
        }
    }

    /**
     * Cancel all event registrations to Provisioner instances
     */
    void cancelRegistrations() {
        logger.debug("Canceling all event registrations to Provisioner instances");
        Map<ProvisionManager, ProvisionLeaseManager> map = new HashMap<ProvisionManager, ProvisionLeaseManager>();
        map.putAll(leaseTable);
        for(Map.Entry<ProvisionManager, ProvisionLeaseManager> entry : map.entrySet()) {
            cancelRegistration(entry.getKey());
        }
    }

    /**
     * Cancel the registration to a monitor
     *
     * @param monitor The monitor to cancel
     */
    private void cancelRegistration(final ProvisionManager monitor) {
        ProvisionLeaseManager leaseManager = leaseTable.get(monitor);
        if(leaseManager != null) {
            leaseManager.drop(false);
            leaseTable.remove(monitor);
            logger.info("Dropping ProvisionManager, now connected to [{}] ProvisionMonitor instances",
                        leaseTable.size());
        }
    }

    /**
     * Update all known Provisioners
     */
    void updateMonitors() {
        updateMonitors(adapter.getResourceCapability(), adapter.getDeployedServices());
    }

    /**
     * Update all known Provisioners with new serviceLimit value
     *
     * @param serviceLimit The maximum number of services the Cybernode has been 
     * configured to instantiate 
     */
    void updateMonitors(final int serviceLimit) {
        setServiceLimit(serviceLimit);
        updateMonitors(adapter.getResourceCapability(), adapter.getDeployedServices());
    }

    /**
     * Update all known Provisioners of the new ResourceCapability
     * 
     * @param resourceCapability The ResourceCapability object
     * @param deployedServices List of deployed services
     */
    private void updateMonitors(final ResourceCapability resourceCapability, final List<DeployedService> deployedServices) {
        ProvisionLeaseManager[] mgrs;
        synchronized(leaseTable) {
            Collection<ProvisionLeaseManager> c = leaseTable.values();
            mgrs = c.toArray(new ProvisionLeaseManager[c.size()]);
        }
        if(mgrs.length == 0)
            return;
        StringBuilder sb = new StringBuilder();
        sb.append("Deployed: ").append(deployedServices.size()).append(" Limit: ").append(serviceLimit);
        if(!resourceCapability.measuredResourcesWithinRange()) {
            NumberFormat numberFormatter = NumberFormat.getNumberInstance();
            numberFormatter.setGroupingUsed(false);
            numberFormatter.setMaximumFractionDigits(3);
            for(MeasuredResource mr : resourceCapability.getMeasuredResources(ResourceCapability.MEASURED_RESOURCES_BREACHED)) {
                sb.append(" ");
                sb.append(mr.getIdentifier());
                sb.append(" BREACHED value: ").append(numberFormatter.format(mr.getValue()));
                sb.append(", threshold: ").append(mr.getThresholdValues().getHighThreshold());
            }
            logger.warn(sb.toString());
        } else{
            sb.append(", All Measured Resources within range");
            logger.trace(sb.toString());
        }

        for (ProvisionLeaseManager mgr : mgrs) {
            try {
                logger.trace("Updating ProvisionMonitor with ResourceCapability. Number of deployed services: {}",
                             deployedServices.size());
                mgr.provisioner.update(adapter.getInstantiator(), resourceCapability, deployedServices, serviceLimit);
            } catch (Exception e) {
                logger.warn("Failed updating ProvisionManager", e);
                boolean connected = false;

                StringBuilder logMessage = new StringBuilder();
                /* Determine if we should even try to reconnect */
                final int category = ThrowableConstants.retryable(e);
                if (category == ThrowableConstants.INDEFINITE ||
                    category == ThrowableConstants.UNCATEGORIZED) {
                    connected = mgr.reconnect();
                    String reconnection = connected?"succeeded. ":"failed. ";
                    logMessage.append("Attempted reconnect after failure: ").append(reconnection);
                }

                if (!connected) {
                    removeProvisionManager(mgr.provisioner, mgr.serviceID);
                }
                logger.warn("{}Now connected to [{}] ProvisionMonitor instances. ", logMessage.toString(), leaseTable.size());
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
    private synchronized Lease connect(final ProvisionManager provisioner) {
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
                    logger.debug("Established ProvisionManager registration");
                    connected = true;
                    break;
                } else {
                    logger.warn("Invalid Lease time [{}] returned from ProvisionManager, retry count [{}]",
                                leaseTime, i);
                    try {
                        lease.cancel();
                    } catch(Exception e ) {
                        logger.trace("Cancelling Lease with invalid lease time", e);
                    }
                    try {
                        Thread.sleep(provisionerRetryDelay);
                    } catch(InterruptedException ie) {
                        /* should not happen */
                    }
                }

            } catch(SecurityException e) {
                //cancelRegistration(provisioner);
                logger.warn("ProvisionManager security exception", e);
                break;
            } catch(Exception e) {
                logger.warn("Recovering ProvisionManager Lease attempt retry count [{}] {}:{}",
                            i, e.getClass().getName(), e.getMessage());
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
        return lease;
    }        

    /**
     * Verify we arent already registered to a Provisioner
     *
     * @param provisionManager The ProvisionManager to check
     *  
     * @return true if we already have a registration
     */
    private boolean haveRegistration(final ProvisionManager provisionManager) {
        for(Map.Entry<ProvisionManager, ProvisionLeaseManager> entry : leaseTable.entrySet()) {
            ProvisionManager service = entry.getKey();
            if(service.equals(provisionManager))
                return(true);
        }
        return(false);
    }

    /*
     * Get the DeployedService instances
     */
    private List<DeployedService> getServiceDeployments() {
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
        final ProvisionManager provisioner;
        final ServiceID serviceID;

        ProvisionLeaseManager(final Lease lease, final ProvisionManager provisioner, final ServiceID serviceID) {
            super("ProvisionLeaseManager");
            this.lease = lease; 
            leaseTime = lease.getExpiration() - System.currentTimeMillis();
            logger.trace("ProvisionMonitor Lease expiration : [{}]", TimeUtil.format(leaseTime));
            this.provisioner = provisioner;
            this.serviceID = serviceID;
            setDaemon(true);
            start();
        }

        void drop(boolean removed) {
            if(!removed && lease!=null) {
                try {
                    lease.cancel();
                    logger.trace("Canceled Lease to ProvisionManager");
                } catch(AccessControlException e) {
                    logger.warn("Permissions problem dropping lease", e);
                } catch(Exception e) {
                    logger.warn("ProvisionLeaseManager: could not drop lease {}: {}",
                                e.getClass().getName(), e.getMessage());
                }
            }
            lease = null;
            keepAlive = false;
        }

        public void run() {
            long leaseRenewalTime = TimeUtil.computeLeaseRenewalTime(leaseTime);
                       
            while(keepAlive) {
                try {
                    Thread.sleep(leaseRenewalTime);
                } catch(InterruptedException ie) {
                    /* should not happen */
                } catch(IllegalArgumentException iae) {
                    logger.warn("Lease renewal time incorrect : {}", leaseRenewalTime);
                }
                if(lease != null) {
                    try {
                        lease.renew(leaseTime);
                    } catch(Exception e) {
                        /* Determine if we should even try to reconnect */
                        if(!ThrowableUtil.isRetryable(e)) {
                            keepAlive = false;
                            logger.warn("Unrecoverable Exception renewing ProvisionManager Lease", e);
                        }
                        if(keepAlive) {
                            /*
                             * If we failed to renew the Lease we should try and
                             * re-establish communications to the ProvisionManager and get
                             * another Lease
                             */
                            logger.warn("Could not renew, attempt to reconnect", e);
                            boolean connected = reconnect();
                            if(!connected) {
                                logger.warn("Unable to recover ProvisionManager registration, exiting");
                                break;
                            } else {
                                logger.info("Recovered ProvisionManager registration");
                            }
                            
                        } else {
                            logger.debug("No retry attempted, ProvisionMonitor determined unreachable");
                            break;
                        }                        
                    }
                }
            }
                        
            /* Broken out of loop, make sure we are removed from the
             * leaseManagerTable */
            Object removed = leaseTable.remove(provisioner);
            if(removed!=null) {
                logger.debug("Remove ProvisionLeaseManager from leaseManagerTable");
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
            return connected;
        }        
    }
    
    /**
     * The ComputeResourceObserver class listens for changes to the ComputeResource
     * component and updates known Provisioners of the change in state
     */
    class ComputeResourceObserver implements ResourceCapabilityChangeListener {
        final ComputeResource computeResource;

        ComputeResourceObserver(final ComputeResource computeResource) {
            this.computeResource = computeResource;
            computeResource.addListener(this);
        }

        /**
         * Notification from the ComputeResource of a change in the
         * ResourceCapability object. Get the updated ResourceCapability and notify
         * all provisioners
         */
        public void update(final ResourceCapability resourceCapability) {
            if(!destroyed) {
                updateMonitors(resourceCapability, getServiceDeployments());
            } else {
                logger.warn("Destroyed, but still getting updates from ComputeResource");
            }
        }        
    }
}
