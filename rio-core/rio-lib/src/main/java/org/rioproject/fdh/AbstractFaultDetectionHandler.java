/*
 * Copyright 2008 the original author or authors.
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
package org.rioproject.fdh;

import net.jini.config.Configuration;
import net.jini.core.entry.Entry;
import net.jini.core.lookup.ServiceID;
import net.jini.core.lookup.ServiceItem;
import net.jini.lookup.LookupCache;
import net.jini.lookup.ServiceDiscoveryEvent;
import net.jini.lookup.entry.Name;
import org.rioproject.resources.client.ServiceDiscoveryAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * The AbstractFaultDetectionHandler provides a base class which can be extended 
 * to provide concrete FaultDetectionHandler capabilities. The basic infrastructure 
 * included in the AbstractFaultDetectionHandler includes 
 * {@link FaultDetectionListener} registration and notification,
 * a {@link net.jini.lookup.ServiceDiscoveryListener} implementation
 * 
 * Properties that will be common across all classes which extend this class are also
 * provided:
 * <ul>
 * <li>retryCount
 * <li>retryTimeout
 * </ul>
 * 
 * @see FaultDetectionHandler
 * @see FaultDetectionListener
 *
 * @author Dennis Reedy
 */
@SuppressWarnings("PMD.AvoidThrowingRawExceptionTypes")
public abstract class AbstractFaultDetectionHandler implements FaultDetectionHandler<ServiceID> {
    public static final int DEFAULT_RETRY_COUNT = 3;
    public static final long DEFAULT_RETRY_TIMEOUT = 1000;
    public static final String RETRY_COUNT_KEY = "retryCount";
    public static final String RETRY_TIMEOUT_KEY = "retryTimeout";
    /** Object that can be used to communicate to the service */
    protected Object proxy;
    protected int retryCount = DEFAULT_RETRY_COUNT;
    protected long retryTimeout = DEFAULT_RETRY_TIMEOUT;
    /** Collection of FaultDetectionListeners */
    private final List<FaultDetectionListener<ServiceID>> listeners =
        new ArrayList<FaultDetectionListener<ServiceID>>();
    /** ServiceID used to discover service instance */
    private ServiceID serviceID;    
    /** The LookupCache */
    private LookupCache lCache;
    /** Flag to indicate the utility is terminating */
    protected boolean terminating = false;
    /** Configuration arguments */
    protected String[] configArgs;
    /** A Configuration object */
    protected Configuration config;    
    /** Listener for the LookupCache */
    private FDHListener fdhListener;
    /** Component name, used for config and logger */
    private static final String COMPONENT = 
        "org.rioproject.fdh.AbstractFaultDetectionHandler";
    /** Class which provides service monitoring */
    protected ServiceMonitor serviceMonitor;
    /** A Logger */
    static Logger logger = LoggerFactory.getLogger(COMPONENT);
    
    /**
     * @see FaultDetectionHandler#register
     */
    public void register(FaultDetectionListener<ServiceID> listener) {
        if(listener == null)
            throw new IllegalArgumentException("listener is null");
        synchronized(listeners) {
            if(!listeners.contains(listener))
                listeners.add(listener);
        }
    }

    /**
     * @see FaultDetectionHandler#unregister
     */
    public void unregister(FaultDetectionListener<ServiceID> listener) {
        if(listener == null)
            throw new IllegalArgumentException("listener is null");
        synchronized(listeners) {
            listeners.remove(listener);
        }
    }

    /**
     * @see FaultDetectionHandler#monitor
     */
    public void monitor(Object proxy, ServiceID id, LookupCache lCache) throws Exception {
        if(proxy == null)
            throw new IllegalArgumentException("proxy is null");
        if(id == null)
            throw new IllegalArgumentException("id is null");
        this.proxy = proxy;
        this.serviceID = id;
        if(lCache != null) {
            this.lCache = lCache;
            fdhListener = new FDHListener();
            try {
                this.lCache.addListener(fdhListener);
            } catch(IllegalStateException e) {
                throw new Exception("Unable to set the FaultDetectionListener", e);
            }
        }
        serviceMonitor = getServiceMonitor();        
    }

    /**
     * Get the class which implements the ServiceMonitor
     *
     * @return A ServiceMonitor
     *
     * @throws Exception if the ServiceMonitor cannot be created
     */
    protected abstract ServiceMonitor getServiceMonitor() throws Exception;
    
    /**
     * @see FaultDetectionHandler#terminate
     */
    public void terminate() {
        if(terminating)
            return;
        terminating = true;        
        if(lCache != null && fdhListener!=null) {
            try {
                lCache.removeListener(fdhListener);
            } catch (Throwable t) {
                logger.warn("Exception {} removing Listener from LookupCache", t.getClass().getName());
            }
        }
        if(serviceMonitor != null) {
            serviceMonitor.drop();
        }
        listeners.clear();
    }

    /**
     * Notify FaultDetectionListener instances the service has been removed
     */
    @SuppressWarnings("unchecked")
    protected void notifyListeners() {
        FaultDetectionListener<ServiceID>[] ls;
        synchronized(listeners) {
            ls = listeners.toArray(new FaultDetectionListener[listeners.size()]);
        }
        for(FaultDetectionListener<ServiceID> l : ls) {
            l.serviceFailure(proxy, serviceID);
        }
    }

    public void setRetryCount(int retryCount) {
        this.retryCount = retryCount;
    }

    public void setRetryTimeout(long retryTimeout) {
        this.retryTimeout = retryTimeout;
    }

    /**
     * Defines the semantics of an internal class which will be used in perform
     * service-specific monitoring
     */
    public interface ServiceMonitor {
        /**
         * Drop the Monitor, its no longer needed
         */
        void drop();
        
        /**
         * Verify that the service can be reached. If the service cannot be reached
         * return false
         *
         * @return true or false
         */
        boolean verify();
    }
    
    /**
     * Listen for service removal events for the service we're interested in
     */
    class FDHListener extends ServiceDiscoveryAdapter {
        boolean reachable = false;

        /**
         * Notification that the service has been removed
         * 
         * @param sdEvent The ServiceDiscoveryEvent
         */
        public void serviceRemoved(ServiceDiscoveryEvent sdEvent) {
            ServiceItem item = sdEvent.getPreEventServiceItem();
            if(!item.serviceID.equals(serviceID)) {
                return;
            }
            if(logger.isTraceEnabled())
                logger.trace("FDH: service [{}], removed notification", getName(item.attributeSets));

            if(serviceMonitor != null) {
                for(int i = 0; i < retryCount; i++) {                    
                    reachable = serviceMonitor.verify();
                    if(!reachable) {
                        break;
                    }
                    if(retryTimeout > 0) {
                        try {
                            Thread.sleep(retryTimeout);
                        } catch(InterruptedException ie) {
                            // Restore the interrupted status
                            Thread.currentThread().interrupt();
                            if(logger.isTraceEnabled())
                                logger.trace("FDH: service [{}], proxy [{}] interrupted during retry timeout",
                                             getName(item.attributeSets), proxy.getClass().getName());
                        }
                    }
                }
            }
            if(logger.isTraceEnabled())
                logger.trace("FDH: service [{}], proxy [{}] is reachable [{}]",
                             getName(item.attributeSets), proxy.getClass().getName(), reachable);
            if(!reachable && !terminating) {
                notifyListeners();
                terminate();
            }
        }
    }
        
    
    /**
     * Get the first Name.name from the attribute collection set
     *
     * @param attrs Array of Entry objects
     *
     * @return The the first Name.name from the attribute collection set or
     * null if not found
     */
    protected String getName(Entry[] attrs) {
        for (Entry attr : attrs) {
            if (attr instanceof Name) {
                return (((Name) attr).name);
            }
        }
        return("unknown");
    }
}
