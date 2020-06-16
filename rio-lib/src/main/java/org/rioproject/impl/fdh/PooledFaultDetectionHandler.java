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
package org.rioproject.impl.fdh;

import net.jini.admin.Administrable;
import net.jini.core.lookup.ServiceID;
import net.jini.core.lookup.ServiceItem;
import net.jini.lookup.ServiceDiscoveryEvent;
import org.rioproject.impl.util.ThrowableUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.rmi.RemoteException;
import java.util.*;
import java.util.concurrent.*;

/**
 * @author Dennis Reedy
 */
public class PooledFaultDetectionHandler /*implements FaultDetectionHandler<ServiceID>*/ extends AbstractFaultDetectionHandler {
    private final Set<ServiceEntry> serviceSet;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private static final Logger logger = LoggerFactory.getLogger(PooledFaultDetectionHandler.class);

    public PooledFaultDetectionHandler() {
        Map<ServiceEntry, Boolean> concurrentMap = new ConcurrentHashMap<>();
        serviceSet = Collections.newSetFromMap(concurrentMap);
    }

    @Override public void configure(Properties properties) {
        super.configure(properties);
        long invocationDelay = getInvocationDelay();
        scheduler.scheduleAtFixedRate(new Reaper(), 0, invocationDelay, TimeUnit.MILLISECONDS);
    }

    int getServiceCount() {
        return serviceSet.size();
    }

    @Override public void monitor(Object service, ServiceID serviceID) {
        if(service==null || serviceID==null)
            throw new IllegalArgumentException("The proxy or the serviceID cannot be null");
        serviceSet.add(new ServiceEntry(service, serviceID));
    }

    @Override protected ServiceMonitor getServiceMonitor() {
        return null;
    }

    @Override protected ServiceCacheListener getServiceCacheListener() {
        return new FDHListener();
    }

    @Override public void terminate() {
        System.out.println("Terminate: "+serviceSet.size());
        scheduler.shutdownNow();
        super.terminate();
    }

    private class Reaper implements Runnable {

        @Override public void run() {
            List<ServiceEntry> removals = new ArrayList<>();
            System.out.println("Reaper: "+serviceSet.size());
            for(ServiceEntry entry : serviceSet) {
                if(entry.administrable!=null) {
                    boolean verified = false;
                    try {
                        entry.administrable.getAdmin();
                        verified = true;
                    } catch (RemoteException e) {
                        //e.printStackTrace();
                    }
                    if(!verified) {
                        removals.add(entry);
                    }
                } else {
                    logger.debug("Skipped {}, it is not Administrable", entry.serviceID) ;
                }
            }
            for(final ServiceEntry entry : removals) {
                serviceSet.remove(entry);
                getListeners().stream().forEach(l -> l.serviceFailure(entry.proxy, entry.serviceID));
            }
        }
    }

    /**
     * Listen for service removal events for the service we're interested in
     */
    private class FDHListener extends ServiceCacheListener {
        final ExecutorService verifyPool = Executors.newCachedThreadPool();

        /**
         * Notification that the service has been removed
         *
         * @param sdEvent The ServiceDiscoveryEvent
         */
        public void serviceRemoved(ServiceDiscoveryEvent sdEvent) {
            ServiceItem item = sdEvent.getPreEventServiceItem();
            ServiceEntry entry = new ServiceEntry(item);
            if(!serviceSet.contains(entry)) {
                return;
            }
            if(logger.isTraceEnabled())
                logger.trace("FDH: service [{}], removed notification", NameHelper.getName(item.attributeSets));

            verifyPool.submit(new ServiceVerifier(entry));
        }

        public void terminate() {
            verifyPool.shutdownNow();
        }
    }

    private class ServiceVerifier implements Runnable {
        ServiceEntry serviceEntry;

        ServiceVerifier(ServiceEntry serviceEntry) {
            this.serviceEntry = serviceEntry;
        }

        @Override public void run() {
            boolean reachable = false;
            for(int i = 0; i < retryCount; i++) {
                reachable = verify();
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
                                         NameHelper.getName(serviceEntry.item.attributeSets),
                                         serviceEntry.proxy.getClass().getName());
                    }
                }
            }

            if(logger.isTraceEnabled())
                logger.trace("FDH: service [{}], proxy [{}] is reachable [{}]",
                             NameHelper.getName(serviceEntry.item.attributeSets),
                             serviceEntry.proxy.getClass().getName(), reachable);
            if(!reachable) {
                getListeners().stream().forEach(l -> l.serviceFailure(serviceEntry.proxy, serviceEntry.serviceID));
            }
        }

        boolean verify() {
            boolean verified = false;
            try {
                if(logger.isTraceEnabled())
                    logger.trace("Invoke getAdmin() on : {}", serviceEntry.proxy.getClass().getName());
                serviceEntry.administrable.getAdmin();
                if(logger.isTraceEnabled())
                    logger.trace("Invocation to getAdmin() on : {} returned", serviceEntry.proxy.getClass().getName());
                verified = true;
            } catch(RemoteException e) {
                if(logger.isDebugEnabled())
                    logger.debug("RemoteException reaching service, service cannot be reached");
            } catch(Throwable t) {
                if(!ThrowableUtil.isRetryable(t)) {
                    if(logger.isDebugEnabled())
                        logger.debug("Unrecoverable Exception invoking getAdmin()", t);
                }
            }
            return verified;
        }
    }

    /*
     * Holds a reference to a service proxy and service id
     */
    private class ServiceEntry {
        Object proxy;
        ServiceID serviceID;
        Administrable administrable;
        ServiceItem item;

        ServiceEntry(ServiceItem item) {
            this(item.service, item.serviceID);
            this.item = item;
        }

        ServiceEntry(Object proxy, ServiceID serviceID) {
            this.proxy = proxy;
            this.serviceID = serviceID;
            if(proxy instanceof Administrable)
                administrable = (Administrable)proxy;
        }

        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;
            ServiceEntry that = (ServiceEntry) o;
            return !(serviceID != null ? !serviceID.equals(that.serviceID) : that.serviceID != null);
        }

        public int hashCode() {
            return serviceID != null ? serviceID.hashCode() : 0;
        }
    }
}
