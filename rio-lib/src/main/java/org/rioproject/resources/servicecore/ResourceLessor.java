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
package org.rioproject.resources.servicecore;

import com.sun.jini.landlord.LeasedResource;
import net.jini.core.lease.Lease;
import net.jini.core.lease.LeaseDeniedException;
import net.jini.id.Uuid;
import org.rioproject.util.TimeConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Abstract class to manage the service's leased resources.
 *
 * @author Dennis Reedy
 */
@SuppressWarnings("unused")
public abstract class ResourceLessor {
    /** A hash of resources to cookies */
    private final Map<Uuid, LeasedResource> resources = new ConcurrentHashMap<Uuid, LeasedResource>();
    /** A Thread which will clean up stale leases */
    private Thread reaper = null;
    private long reapingInterval = TimeConstants.ONE_SECOND*10;
    /** A LinkedList of LeaseListener objects */
    private final List<LeaseListener> listeners = new LinkedList<LeaseListener>();
    /** Component for getting the Logger */
    private static final String COMPONENT_NAME = ResourceLessor.class.getPackage().getName();
    /** The Logger */
    private static final Logger logger = LoggerFactory.getLogger(COMPONENT_NAME);

    /**
     * Check to make sure that the LeasedResource lease has not expired yet <br>
     *
     * @param resource The LeasedResource
     *
     * @return Returns true if the lease on the passed resource has not expired
     * yet
     */
    public boolean ensure(final LeasedResource resource) {
        return(resource.getExpiration() > currentTime());
    }

    public void setReapingInterval(long reapingInterval) {
        this.reapingInterval = reapingInterval;
    }

    /**
     * Create a new lease <br>
     * 
     * @param resource to be leased
     * @param duration Time requested for <code>Lease</code>
     *
     * @throws LeaseDeniedException If the lease has been denied
     * @return A new Lease
     */
    public abstract Lease newLease(LeasedResource resource, long duration) throws LeaseDeniedException;

    /**
     * Remove a leased resource from the list of managed leases. <br>
     * 
     * @param resource ServiceResource to remove
     *
     * @return true if the lease was removed
     */
    public boolean remove(final LeasedResource resource) {
        return (remove(resource.getCookie()));
    }

    /**
     * Remove all leased resources .<br>
     */
    public void removeAll() {
        LeasedResource[] resources = getLeasedResources();
        for (LeasedResource resource : resources) {
            remove(resource.getCookie());
        }
    }

    /**
     * Remove a leased resource from the list of managed leases. <br>
     * 
     * @param cookie Object to remove
     *
     * @return boolean True if removed false if not removed
     */
    public boolean remove(final Uuid cookie) {
        LeasedResource resource;
        boolean removed = false;
        synchronized(resources) {
            resource = resources.remove(cookie);
        }
        if(resource != null) {
            notifyLeaseRemoval(resource);
            removed = true;
        }
        return (removed);
    }

    /**
     * Add a LeaseListener <br>
     * 
     * @param listener the LeaseListener to add
     */
    public void addLeaseListener(final LeaseListener listener) {
        listeners.add(listener);
    }

    /**
     * Remove a LeaseListener <br>
     * 
     * @param listener the LeaseListener to remove
     */
    public void removeLeaseListener(final LeaseListener listener) {
        listeners.remove(listener);
    }

    /**
     * @return the total number of service resources that have been leased
     */
    public int total() {
        int size;
        synchronized(resources) {
            size = resources.size();
        }
        return (size);
    }

    /**
     * Add a LeasedResource for a new Lease or renewing a Lease
     *
     * @param resource The resource to add or update
     *
     * @throws IllegalArgumentException if the resource is null
     */
    public void addLeasedResource(final LeasedResource resource) {
        if(resource == null)
            throw new IllegalArgumentException("resource is null");
        synchronized(this) {
            if(reaper==null) {
                reaper = new LeaseReaper(reapingInterval);
                reaper.setDaemon(true);
                reaper.start();
            }
        }
        synchronized(resources) {
            resources.put(resource.getCookie(), resource);
        }
    }

    /**
     * Get a LeasedResource
     *
     * @param cookie The Uuid of the LeasedResource to get
     *
     * @return The LeasedResource for the Uuid
     *
     * @throws IllegalArgumentException if the cookie is null
     */
    public LeasedResource getLeasedResource(final Uuid cookie) {
        if(cookie == null)
            throw new IllegalArgumentException("cookie is null");
        LeasedResource resource;
        synchronized(resources) {
            resource = resources.get(cookie);
        }
        return(resource);
    }
    /**
     * This method returns a snapshot of the LeasedResource objects that
     * this ResourceLessor is managing
     *
     * @return An array of LeasedResource elements. If no LeasedResource
     * objects are found return a zero-length array. A new array is returned
     * each time
     */
    public LeasedResource[] getLeasedResources() {
        LeasedResource[] leasedResources;
        synchronized(resources) {
            Collection<LeasedResource> c = resources.values();
            leasedResources = c.toArray(new LeasedResource[c.size()]);
        }
        return (leasedResources);
    }

    /**
     * Notify LeaseListener instances of a new registration
     * 
     * @param resource The LeasedResource
     */
    public void notifyLeaseRegistration(final LeasedResource resource) {
        for (LeaseListener listener : listeners)
            listener.register(resource);
    }

    /**
     * Notify LeaseListener instances of a lease renewal
     * 
     * @param resource The LeasedResource
     */
    public void notifyLeaseRenewal(final LeasedResource resource) {
        for (LeaseListener listener : listeners)
            listener.renewed(resource);
    }

    /**
     * Notify LeaseListener instances of a lease expiration
     * 
     * @param resource The LeasedResource
     */
    public void notifyLeaseExpiration(final LeasedResource resource) {
        for (LeaseListener listener : listeners)
            listener.expired(resource);
    }

    /**
     * Notify LeaseListener instances of a lease removal
     * 
     * @param resource The LeasedResource
     */
    public void notifyLeaseRemoval(final LeasedResource resource) {
        for (LeaseListener listener : listeners)
            listener.removed(resource);
    }

    /**
     * Stop and clean up all resources
     */
    public void stop() {
        if(reaper!=null)
            reaper.interrupt();
        reaper = null;
        removeAll();
    }

    /**
     * Method that provides some notion of the current time in milliseconds
     * since the beginning of the epoch. Default implementation calls
     * System.currentTimeMillis()
     *
     * @return The current time
     */
    public long currentTime() {
        return (System.currentTimeMillis());
    }
    
    /**
     * Clean up leases that have not been renewed. Check every 30 seconds for
     * stale leases
     */
    protected class LeaseReaper extends Thread {
        private final long reapingInterval;

        public LeaseReaper(long reapingInterval) {
            super("LeaseReaper");
            this.reapingInterval = reapingInterval;
        }
        
        public void run() {
            while (reaper != null || !isInterrupted()) {
                try {
                    Thread.sleep(reapingInterval);
                } catch(InterruptedException e) {
                    break;
                }
                Set<Entry<Uuid,LeasedResource>> mapEntries;
                synchronized(resources) {
                    mapEntries = resources.entrySet();
                }
                for (Entry<Uuid, LeasedResource> entry : mapEntries) {
                    LeasedResource lr = entry.getValue();
                    if (!ensure(lr)) {
                        if (logger.isDebugEnabled())
                            logger.debug("Lease expired for resource {}, cookie {}",
                                         ((ServiceResource) lr).getResource().toString(), lr.getCookie());
                        remove(lr.getCookie());
                        notifyLeaseExpiration(lr);
                    }
                }
            }
        }
    }
}
