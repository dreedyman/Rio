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
package org.rioproject.monitor.selectors;

import com.sun.jini.landlord.LeasedResource;
import net.jini.id.Uuid;
import org.rioproject.monitor.AssociationMatcher;
import org.rioproject.monitor.InstantiatorResource;
import org.rioproject.monitor.ProvisionException;
import org.rioproject.monitor.ProvisionRequest;
import org.rioproject.opstring.ServiceElement;
import org.rioproject.resources.servicecore.LandlordLessor;
import org.rioproject.resources.servicecore.LeaseListener;
import org.rioproject.resources.servicecore.ServiceResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * This abstract class defines the semantics for selecting a
 * <code>ServiceResource</code> based on requirements contained within a
 * <code>ServiceElement</code>. Additionally, this interface defines
 * semantics for the removal and snapshot retrieval of
 * <code>ServiceResource</code> objects under its control.
 * <p>
 * Objects which implement this interface will need to create a
 * <code>Collection</code> to manage the <code>ServiceResource</code>
 * elements it is notified of. The managed <code>Collection</code> will back
 * the <code>Collection</code> of <code>ServiceResource</code> elements
 * which are leased and managed by the <code>LandlordLessor</code>
 * <p>
 * The <code>LandlordLessor</code> will send notifications of the following:
 * <ul>
 * <li>Notifies the manager of a lease expiration
 * <li>Notifies the manager of a new lease being created
 * <li>Notifies the manager of a lease being renewed
 * </ul>
 * <p>
 * It is the responsibility of the entity to register with the
 * <code>LandlordLessor</code>. The object that implements this interface
 * should define the actions to take upon receipt of such notifications
 * <p>
 * Concrete implementations of this class must provide a zero-argument
 * constructor in order to be instantiated by the ProvisionMonitor
 *
 * @author Dennis Reedy
 */
public abstract class ServiceResourceSelector implements LeaseListener {
    /** The collection of ServiceResource objects to manage */
    protected Collection<LeasedResource> collection;
    /* Semaphore for access to modifying the collection */
    protected final Object collectionLock = new Object();
    static final Logger logger = LoggerFactory.getLogger(ServiceResourceSelector.class);
    /**
     * The LandlordLessor which will be registered to, and will provide Lease
     * notification events
     */
    protected LandlordLessor landlord;

    /**
     * Set the <code>LandlordLessor</code> the
     * <code>ServiceResourceSelector</code> will register to.
     * 
     * @param landlord The LandlordLessor
     */
    public void setLandlordLessor(final LandlordLessor landlord) {
        this.landlord = landlord;
        landlord.addLeaseListener(this);
    }

    /**
     * This method will attempt to identify an available
     * <code>ServiceResource</code> based on the operational criteria
     * specified by a ServiceBean
     * 
     * @param provisionRequest The ProvisionRequest
     * @return If a <code>ServiceResource</code> object can
     * be identified, return the <code>ServiceResource</code>, otherwise 
     * return <code>null</code>
     *
     * @throws Exception If there are errors getting a ServiceResource
     */
    public ServiceResource getServiceResource(final ProvisionRequest provisionRequest) throws Exception {
        ServiceResource[] svcResources = getServiceResources();
        if(svcResources.length==0) {
            provisionRequest.addFailureReason(String.format("There are no registered Cybernodes, unable to provision %s",
                                                            provisionRequest.getServiceElement().getName()));
            return null;
        }
        return (selectServiceResource(provisionRequest, svcResources));
    }

    /**
     * This method will attempt to identify an available
     * <code>ServiceResource</code> based on the operational criteria
     * specified by a ServiceBean
     * 
     * @param provisionRequest The ProvisionRequest
     * @param uuid The Uuid of the InstantiatorResource to either include or
     * not include
     * @param inclusive Either include or exclude the uuid from the selection.
     * If true, include the uuid otherwise exclude the uuid
     * @return If a <code>ServiceResource</code> object can
     * be identified, otherwise return <code>null</code>
     *
     * @throws ProvisionException if any errors occur selecting a resource
     */
    public ServiceResource getServiceResource(final ProvisionRequest provisionRequest,
                                              final Uuid uuid,
                                              final boolean inclusive) throws ProvisionException {
        return (selectServiceResource(provisionRequest, getServiceResources(uuid, inclusive)));
    }

    /**
     * Select a ServiceResource for dynamic ServiceBean provisioning based on
     * the operational criteria of a ServiceBean
     *
     * @param provisionRequest The ProvisionRequest
     * @param svcResources Array ServiceResource candidates
     * @return If a <code>ServiceResource</code> object can
     * be identified, otherwise return <code>null</code>
     *
     * @throws org.rioproject.monitor.ProvisionException If there are unrecoverable errors
     * provisioning the service
     */
    protected ServiceResource selectServiceResource(final ProvisionRequest provisionRequest,
                                                    final ServiceResource[] svcResources) throws ProvisionException {
        if(svcResources.length==0) {
            provisionRequest.addFailureReason(String.format("There are no available Cybernodes, unable to provision %s",
                                                            provisionRequest.getServiceElement().getName()));
            return null;
        }

        /* Filter out isolated associations and max per machine levels set
         * at the physical level */
        ServiceResource[] filteredResources = filterMachineBoundaries(provisionRequest, svcResources);
        if(filteredResources.length>0) {
            filteredResources = filterIsolated(provisionRequest, filteredResources);
        } else {
            provisionRequest.addFailureReason(String.format("There are no available Cybernodes, unable to provision %s",
                                                            provisionRequest.getServiceElement().getName()));
            return null;
        }

        for (ServiceResource svcResource : filteredResources) {
            InstantiatorResource ir = (InstantiatorResource) svcResource.getResource();
            /*
             * Make sure the InstantiatorResource has not reached it's
             * serviceLimit
             */
            ServiceElement sElem = provisionRequest.getServiceElement();
            int serviceLimit = ir.getServiceLimit();
            int total = ir.getServiceElementCount() + ir.getInProcessCounter();
            if (total >= serviceLimit) {
                String reason =
                    String.format("%s reached service limit of [%d], cannot be used to instantiate [%s/%s]",
                              ir.getName(), serviceLimit, sElem.getOperationalStringName(),
                              sElem.getName());
                provisionRequest.addFailureReason(reason);
                logger.debug(reason);
                continue;
            }
            /*
             * Check if the InstantiatorResource doesn't already have the
             * maximum amount of services allocated. this is different then
             * MaxPerNode
             */
            int planned = sElem.getPlanned();
            int actual = ir.getServiceElementCount(sElem);
            logger.trace("{} has [{}] instance(s), planned [{}] of [{}/{}]",
                         ir.getName(), actual, planned, sElem.getOperationalStringName(), sElem.getName());
            if (actual >= planned) {
                String message = String.format("%s has reached service limit of [%s], cannot be used to instantiate [%s/%s]",
                                               ir.getName(), serviceLimit, sElem.getOperationalStringName(), sElem.getName());
                provisionRequest.addFailureReason(message);
                continue;
            }
            if (ir.getDynamicEnabled()) {
                try {
                    if (ir.canProvision(provisionRequest)) {
                        serviceResourceSelected(svcResource);
                        logger.trace("[{}, service count: {}] has been selected for service [{}/{}]",
                                     ir.getName(), ir.getServiceCount(), sElem.getOperationalStringName(),
                                     sElem.getName());
                        return (svcResource);
                    }
                } catch (Exception e) {
                    logger.warn("[{}] during canProvision check for [{}]",
                                ir.getName(), sElem.getOperationalStringName(), sElem.getName(),
                                e);
                    if(e instanceof ProvisionException)
                        throw (ProvisionException)e;
                }
            } else {
                logger.trace("{}, dynamic enabled: {}", ir.getName(), ir.getDynamicEnabled());
            }
        }
        return (null);
    }

    /**
     * Filter ServiceResource instances for isolated requirements
     *
     * @param provisionRequest The ProvisionRequest to verify
     * @param candidates The candidate ServiceResource instances
     *
     * @return An array of suitable ServiceResource instances
     */
    public ServiceResource[] filterIsolated(final ProvisionRequest provisionRequest, final ServiceResource... candidates) {
        /* For the set of candidate instantiator resources, remove the
         * candidate instantiator resources that have the same host name */
        InstantiatorResource[] known = getInstantiatorResources(provisionRequest.getServiceElement(), true);
        
        List<ServiceResource> candidateList = new ArrayList<ServiceResource>();
        candidateList.addAll(Arrays.asList(candidates));
        for (ServiceResource candidate1 : candidates) {
            InstantiatorResource candidate = (InstantiatorResource) candidate1.getResource();
            if (!AssociationMatcher.meetsIsolatedRequirements(provisionRequest.getServiceElement(), candidate, known)) {
                candidateList.remove(candidate1);
            }
        }
        if(logger.isDebugEnabled() && candidateList.isEmpty()) {
            logger.debug("Service [{}/{}] has a virtual machine boundary constraint and an instance of the service has " +
                         "been found on all Cybernodes executing on each machine. There are no available Cybernodes to " +
                         "allocate service instances",
                         provisionRequest.getServiceElement().getOperationalStringName(),
                         provisionRequest.getServiceElement().getName());
        }
        return(candidateList.toArray(new ServiceResource[candidateList.size()]));
    }

    /*
     * Filter on machine boundaries
     *
     * @param provisionRequest
     * @param candidates
     * @return
     */
    public ServiceResource[] filterMachineBoundaries(final ProvisionRequest provisionRequest, final ServiceResource... candidates) {
        ServiceElement elem = provisionRequest.getServiceElement();
        int maxPerMachine = elem.getMaxPerMachine();
        if(!(maxPerMachine!=-1 &&
             elem.getMachineBoundary()==ServiceElement.MachineBoundary.PHYSICAL)) {
            return(candidates);
        }

        /*
         * 1. Create a table composed of keys that are the host address and
         *    values a list of Cybernodes
         * 2. Count the number services each cybernode has on each host
         * 3. Remove table entries that exceed the count per host
         */
        Map<String, List<ServiceResource>> table = new HashMap<String, List<ServiceResource>>();

        for (ServiceResource candidate : candidates) {
            InstantiatorResource ir = (InstantiatorResource)candidate.getResource();
            List<ServiceResource> list = table.get(ir.getHostAddress());
            if(list==null)
                list = new ArrayList<ServiceResource>();
            list.add(candidate);
            table.put(ir.getHostAddress(), list);
        }

        List<String> remove = new ArrayList<String>();
        for(Map.Entry<String, List<ServiceResource>> entry : table.entrySet()) {
            List<ServiceResource> list = entry.getValue();
            int serviceCount = 0;
            for(ServiceResource sr : list) {
                InstantiatorResource ir = (InstantiatorResource)sr.getResource();
                serviceCount += ir.getInProcessCounter(elem) + ir.getServiceElementCount(elem);
                if(serviceCount>=maxPerMachine) {
                    remove.add(ir.getHostAddress());
                    break;
                }
            }
        }
        for(String s : remove) {
            table.remove(s);
        }

        List<ServiceResource> candidateList = new ArrayList<ServiceResource>();
        for(Map.Entry<String, List<ServiceResource>> entry : table.entrySet()) {
            List<ServiceResource> list = entry.getValue();
            for(int i=0; i<list.size(); i++) {
                if(i<maxPerMachine) {
                    candidateList.add(list.get(i));
                } else {
                    break;
                }
            }
        }

        if(logger.isDebugEnabled() && candidateList.isEmpty() && elem.getProvisionType().equals(ServiceElement.ProvisionType.DYNAMIC)) {
            logger.debug("Service [{}/{}] has a physical machine boundary constraint and an " +
                         "instance of the service has been found on all known machines.",
                         elem.getOperationalStringName(), elem.getName());
        }
        return(candidateList.toArray(new ServiceResource[candidateList.size()]));
    }

    /**
     * This method allows concrete implementations of this class to order the
     * Collection of ServiceResource instances based on a ServiceResource being
     * selected
     * 
     * @param resource A ServiceResource instance selected for provisioning a
     * ServiceBean
     */
    public abstract void serviceResourceSelected(ServiceResource resource);

    /**
     * Get all available <code>ServiceResource</code> instances that support
     * the <code>ServiceElement</code> object's contained requirement
     * specifications
     * 
     * @param provisionRequest The ProvisionRequest
     * @return Array of ServiceResource instances that
     * support the requirements
     *
     * @throws ProvisionException If there are unrecoverable errors
     * provisioning the service
     */
    public ServiceResource[] getServiceResources(final ProvisionRequest provisionRequest) throws ProvisionException {
        ServiceResource[] svcResources = getServiceResources();
        ArrayList<ServiceResource> list = new ArrayList<ServiceResource>();
        for (ServiceResource svcResource : svcResources) {
            InstantiatorResource ir = (InstantiatorResource) svcResource.getResource();
            try {
                if (ir.canProvision(provisionRequest)) {
                    list.add(svcResource);
                }
            } catch (Exception e) {
                logger.warn("[{}] during canProvision check for [{}/{}]",
                            ir.getName(),
                            provisionRequest.getServiceElement().getOperationalStringName(),
                            provisionRequest.getServiceElement().getName(),
                            e);
                if(e instanceof ProvisionException)
                    throw (ProvisionException)e;
            }
        } 
        return list.toArray(new ServiceResource[list.size()]);
    }

    /**
     * Get all available <code>ServiceResource</code> instances that match the
     * Uuid provided
     * 
     * @param uuid A uuid address to match
     * @param inclusive Either include or exclude the resource. If true,
     * include the resource if it matches the uuid, otherwise exclude the
     * resource
     * @return Array of ServiceResource instances that match
     * the host address
     */
    ServiceResource[] getServiceResources(final Uuid uuid, final boolean inclusive) {
        ServiceResource[] svcResources = getServiceResources();
        ArrayList<ServiceResource> list = new ArrayList<ServiceResource>();
        for (ServiceResource svcResource : svcResources) {
            InstantiatorResource ir = (InstantiatorResource) svcResource.getResource();
            if (ir.getInstantiatorUuid().equals(uuid)) {
                if (inclusive)
                    list.add(svcResource);
            } else {
                if (!inclusive)
                    list.add(svcResource);
            }
        } 
        return list.toArray(new ServiceResource[list.size()]);
    }

    /**
     * Get all available <code>ServiceResource</code> instances that match the
     * host address provided
     *
     * @param hostAddress A hostAddress address to match
     * @param inclusive Either include or exclude the resource. If true,
     * include the resource if host addresses match, otherwise exclude the
     * resource
     *
     * @return Array of ServiceResource instances that match the host address
     */
    public ServiceResource[] getServiceResources(final String hostAddress, final boolean inclusive) {
        return(getServiceResources(getServiceResources(), hostAddress, inclusive));
    }

    /**
     * Get all available <code>ServiceResource</code> instances that match the
     * host address provided
     *
     * @param svcResources Array of ServiceResource instances
     * @param hostAddress A hostAddress address to match
     * @param inclusive Either include or exclude the resource. If true,
     * include the resource if host addresses match, otherwise exclude the
     * resource
     *
     * @return Array of ServiceResource instances that match the host address
     */
    ServiceResource[] getServiceResources(final ServiceResource[] svcResources, final String hostAddress, final boolean inclusive) {
        ArrayList<ServiceResource> list = new ArrayList<ServiceResource>();
        for (ServiceResource svcResource : svcResources) {
            InstantiatorResource ir = (InstantiatorResource) svcResource.getResource();
            if (ir.getHostAddress().equals(hostAddress)) {
                if (inclusive)
                    list.add(svcResource);
            } else {
                if (!inclusive)
                    list.add(svcResource);
            }
        }
        return list.toArray(new ServiceResource[list.size()]);
    }

    /**
     * Drop a <code>ServiceResource</code> from the managed
     * <code>Collection</code>. This method will also remove the
     * <code>ServiceResource</code> from the <code>LandlordLessor</code> as
     * well
     * 
     * @param resource The ServiceResource
     */
    public void dropServiceResource(final ServiceResource resource) {
        remove(resource);
        try {
            landlord.cancel(resource.getCookie());
        } catch(Exception ignore) {
            logger.warn("Landlord Lease cancellation failure, Force removal from collection of InstantiatorResource");
            remove(resource);
        }
    }

    /**
     * This method will return a snapshot of all <code>ServiceResource</code>
     * elements contained in its managed <code>Collection</code>
     *
     * @return An array of ServiceResource instances
     */
    public ServiceResource[] getServiceResources() {
        LeasedResource[] resources;
        synchronized(collectionLock) {
            resources = collection.toArray(new LeasedResource[collection.size()]);
        }
        ServiceResource[] svcResources = new ServiceResource[resources.length];
        for(int i=0; i<svcResources.length; i++)
            svcResources[i] = (ServiceResource)resources[i];
        return (svcResources);
    }

    /**
     * Notifies the manager of a lease expiration <br>
     * 
     * @param resource The resource associated with the expiration
     */
    public void expired(final LeasedResource resource) {
        if(resource != null)
            remove(resource);
    }

    /**
     * Notifies the manager of a lease removal <br>
     * 
     * @param resource The resource associated with the removal
     */
    public void removed(final LeasedResource resource) {
        if(resource != null)
            remove(resource);
    }

    /**
     * Notifies the manager of a new lease being created.
     * 
     * @param resource The resource associated with the new Lease.
     */
    public void register(final LeasedResource resource) {
        add(resource);
    }

    /**
     * Notifies the manager of a lease being renewed.
     * 
     * @param resource The resource associated with the new Lease.
     */
    public void renewed(final LeasedResource resource) {
        update(resource);
    }

    /**
     * If the <code>Collection</code> backed by the concrete class requires
     * processing other then that defined by <code>Collection.add</code>
     * override this method to provide the appropriate semantics
     *
     * @param resource The LeasedResource to add
     */
    protected void add(final LeasedResource resource) {
        try {
            synchronized(collectionLock) {
                collection.add(resource);
            }
        } catch(UnsupportedOperationException e) {
            logger.warn("Adding LeasedResource Failed", e);
        }
    }

    /**
     * If the <code>Collection</code> backed by the concrete class requires
     * processing other then that defined by <code>Collection.remove</code>
     * override this method to provide the appropriate semantics
     *
     * @param resource The LeasedResource to remove
     */
    protected void remove(final LeasedResource resource) {
        try {
            synchronized(collectionLock) {
                if(resource != null) {
                    collection.remove(resource);
                }
            }
        } catch(UnsupportedOperationException e) {
            logger.warn("Removing LeasedResource Failed", e);
        }
    }

    /**
     * If the <code>Collection</code> backed by the concrete class requires
     * processing other then that defined by <code>Collection.add</code>
     * override this method to provide the appropriate semantics
     *
     * @param resource The LeasedResource
     */
    protected void update(final LeasedResource resource) {
        try {
            synchronized(collectionLock) {
                collection.add(resource);
            }
        } catch(UnsupportedOperationException e) {
            logger.warn("Updating LeasedResource Failed", e);
        }
    }

    /**
     * Get all <code>InstantiatorResource</code> instances that have
     * instantiated instances of the ServiceElement
     * 
     * @param sElem The ServiceElement
     * @return Array of InstantiatorResource instances that have instantiated
     * the ServiceElement
     */
    public InstantiatorResource[] getInstantiatorResources(final ServiceElement sElem) {
        return(getInstantiatorResources(sElem, false));
    }

    /**
     * Get all <code>InstantiatorResource</code> instances that have
     * instantiated instances of the ServiceElement
     *
     * @param sElem The ServiceElement
     * @param includeInProcess Whether to include in process elements
     * @return Array of InstantiatorResource instances that have instantiated
     * the ServiceElement
     */
    InstantiatorResource[] getInstantiatorResources(final ServiceElement sElem, final boolean includeInProcess) {
        ServiceResource[] svcResources = getServiceResources();
        ArrayList<InstantiatorResource> list = new ArrayList<InstantiatorResource>();
        for (ServiceResource svcResource : svcResources) {
            InstantiatorResource ir = (InstantiatorResource) svcResource.getResource();
            if (includeInProcess) {
                if (ir.getServiceElementCount(sElem) > 0 ||
                    ir.getInProcessCounter(sElem) > 0)
                    list.add(ir);
            } else {
                if (ir.getServiceElementCount(sElem) > 0)
                    list.add(ir);
            }
        }            
        return list.toArray(new InstantiatorResource[list.size()]);
    }
}
