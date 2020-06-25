/*
 * Copyright to the original author or authors.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *         http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.rioproject.monitor.service.managers;

import org.rioproject.deploy.ServiceProvisionListener;
import org.rioproject.impl.servicebean.ServiceElementUtil;
import org.rioproject.monitor.service.ProvisionRequest;
import org.rioproject.monitor.service.ServiceProvisionContext;
import org.rioproject.monitor.service.util.LoggingUtil;
import org.rioproject.opstring.ServiceElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentSkipListMap;

/**
 * Abstract class which will manage ProvisionRequest instances that are waiting
 * to be provisioned. There are two types of ServiceElements which require
 * management:
 * <ol>
 * <li>ServiceElement objects which could not be provisioned and need to be put
 * on a pending list. These elements have the provision type of DYNAMIC, and
 * will be provisioned when instantiation resources become available that meet
 * their operational requirements
 * <li>ServiceElement objects that will be provisioned to all instantiation
 * resources that match their operational requirements. These elements have the
 * provision type of FIXED
 * </ol>
 *
 * @author Dennis Reedy
 */
public abstract class PendingServiceElementManager {
    /** Sorted collection of pending provision requests */
    protected final Map<Key, ProvisionRequest> collection = new ConcurrentSkipListMap<>();
    /** Index into the collection to insert elements */
    private long collectionIndex = 1;
    /** A descriptive String type */
    private final String type;
    protected final ServiceProvisionContext context;
    /** A Logger */
    private final Logger logger = LoggerFactory.getLogger(PendingServiceElementManager.class.getName());

    /**
     * A key for the sortable set
     */
    static class Key implements Comparable<Key> {
        int priority;
        ServiceElement sElem;
        long index;
        long timestamp;
        
        Key(ServiceElement sElem, long index, long timestamp) {
            this.sElem = sElem;
            this.index = index;
            this.timestamp = timestamp;
        }
        
        /* 
         * @see java.lang.Comparable#compareTo(java.lang.Object)
         */
        @Override
        public int compareTo(Key that) {
            if(this==that)
                return 0;
            int comparison;
            long now = System.currentTimeMillis();
            /* The priority is the high order bit, if not set then check if
             * the ServiceElement objects are equal, if they are, make sure
             * instanceIDs are sorted. If the ServiceElement instances dont match,
             * compare on the timestamp, then the index */
            if(this.priority==0 && that.priority==0) {
                if(this.sElem.equals(that.sElem)) {
                    Long thisInstanceID = this.sElem.getServiceBeanConfig().getInstanceID();
                    Long thatInstanceID = that.sElem.getServiceBeanConfig().getInstanceID();
                    comparison = thisInstanceID.compareTo(thatInstanceID);
                } else {
                    /* Dont have different timestamps, use index */
                    if(this.timestamp == that.timestamp) {
                        comparison = (this.index>that.index ? 1 : -1);
                    } else {
                        /* age before beauty :) */
                        comparison = (( (now-this.timestamp) > (now-that.timestamp)) ?-1 : 1);
                    }
                }
            } else {
                /* Priority based comparison, a higher the priority comes before
                 * a lower one */
                comparison = (this.priority>that.priority ? -1 : 1);
            }
            return comparison;
        }        
    }
    
    /**
     * Create a PendingServiceElementManager
     * 
     * @param type A descriptive name for the Manager
     */

    PendingServiceElementManager(String type, ServiceProvisionContext context) {
        this.type = type;
        this.context = context;
    }

    /**
     * @return The descriptive type of this Manager
     */
    public String getType() {
        return type;
    }

    /**
     * Add an element to the pending collection with an index
     * 
     * @param request The ProvisionRequest
     * @param index The index of the ServiceElement
     * @return The index that was used to insert the ProvisionRequest into the
     * collection
     */
    public long addProvisionRequest(ProvisionRequest request, long index) {
        long keyIndex = (index == 0 ? collectionIndex++ : index);
        Key key = new Key(request.getServiceElement(), keyIndex, request.getTimestamp());
        collection.put(key, request);
        return keyIndex;
    }

    /**
     * Get the size of the collection
     *
     * @return The size of the collection
     */
    int getSize() {
        return collection.size();
    }

    /**
     * Get the number of ServiceElement instances in the collection that match
     * provided ServiceElement
     * 
     * @param sElem The ServiceElement to match
     *
     * @return The number of ServiceElement matches
     */
    int getCount(ServiceElement sElem) {
        int count = 0;
        Collection<ProvisionRequest> provisionRequests = collection.values();
        for (ProvisionRequest pr : provisionRequests) {
            if (pr.getServiceElement().equals(sElem)) {
                count++;
            }
        }
        return count;
    }

    /**
     * Remove all ServiceElement instances from the collection
     * 
     * @param sElem The ServiceElement
     * @param numToRemove The number to remove
     * 
     * @return An array of ProvisionRequest instances that have been removed. If 
     * there are no instances that have been removed, return an empty array
     */
    public ProvisionRequest[] removeServiceElement(ServiceElement sElem, int numToRemove) {
        List<Key> removals = removeFromCollection(sElem);
        List<ProvisionRequest> removed = new ArrayList<ProvisionRequest>();
        
        if(!removals.isEmpty()) {
            if(removals.size()>numToRemove) {
                removals = removals.subList((removals.size()-numToRemove), removals.size());
            }
            logger.info("{}: removing [{}] [{}] instances", type, removals.size(), LoggingUtil.getLoggingName(sElem));
            for (Key removal : removals) {
                ProvisionRequest pr = collection.remove(removal);
                removed.add(pr);
            }
        }
        return removed.toArray(new ProvisionRequest[0]);
    }

    private List<Key> removeFromCollection(ServiceElement serviceElement) {
        List<Key> removals = new ArrayList<>();
        Set<Key> keys = collection.keySet();
        for (Key key : keys) {
            ProvisionRequest pr = collection.get(key);
            if (pr != null && serviceElement.equals(pr.getServiceElement()))
                removals.add(key);
        }
        context.getSelector().removeUninstantiable(serviceElement);
        return removals;
    }
    
    /**
     * Remove all ServiceElement instances from the collection
     *
     * @param sElem The ServiceElement
     * 
     * @return An array of ProvisionRequest instances that have been removed. If 
     * there are no instances that have been removed, return an empty array
     */
    public ProvisionRequest[] removeServiceElement(ServiceElement sElem) {
        List<Key> removals = removeFromCollection(sElem);
        List<ProvisionRequest> removed = new ArrayList<>();
        
        if(!removals.isEmpty()) {
            logger.debug("{}: removing [{}] [{}] instances", type, removals.size(), LoggingUtil.getLoggingName(sElem));
            for (Key removal : removals) {
                ProvisionRequest pr = collection.remove(removal);
                removed.add(pr);
            }
        } else {
            logger.debug("{}: There are no pending instances of [{}] to remove ", type, LoggingUtil.getLoggingName(sElem));
        }
        return removed.toArray(new ProvisionRequest[0]);
    }

    /**
     * Determine if the ServiceElement is in the collection
     * 
     * @param sElem The ServiceElement
     *
     * @return If found return true
     */
    public boolean hasServiceElement(ServiceElement sElem) {
        boolean contains = false;
        Set<Key> keys = collection.keySet();
        for (Key key : keys) {
            ProvisionRequest pr = collection.get(key);
            if (sElem.equals(pr.getServiceElement())) {
                contains = true;
                break;
            }
        }
        return (contains);
    }

    /**
     * For each ProvisionRequest in the Collection of ProvisionRequest instances,
     * which contain the ServiceElement, update the ProvisionRequest with the
     * provided ServiceElement
     *
     * @param sElem The ServiceElement
     * @param listener A ServiceProvisionListener to be notified
     */
    public void updateProvisionRequests(ServiceElement sElem,
                                        ServiceProvisionListener listener) {
        Set<Key> keys = collection.keySet();
        for (Key key : keys) {
            ProvisionRequest pr = collection.get(key);
            if (sElem.equals(pr.getServiceElement())) {
                /* Preserve instance IDs */
                Long id = pr.getServiceElement().getServiceBeanConfig().getInstanceID();
                ServiceElement newElem = sElem;
                if (id != null)
                    newElem = ServiceElementUtil.prepareInstanceID(sElem, id.intValue());
                pr.setServiceElement(newElem);
                if(listener!=null) {
                    pr.setServiceProvisionListener(listener);
                }
            }
        }
    }

    /**
     * A concrete implementation on how to process the collection is required as
     * follows: The collection will be processed, attempting to have each
     * ServiceElement provisioned to available ServiceInstantiation resources.
     * Each element will be processed with it's ordinal index ensuring that if
     * there are no available resources it will be put back into the collection
     * in order
     */
    public abstract void process();

    /**
     * Get all ProvisionRequest instances
     *
     * @param sElem The ServiceElement to use
     *
     * @return An array of ProvisionRequest instances
     */
    private ProvisionRequest[] getProvisionRequests(ServiceElement sElem) {
        ProvisionRequest[] prs;
        /* Get all ProvisionRequest instances */
        if(sElem==null) {
            Collection<ProvisionRequest> c = collection.values();
            prs = c.toArray(new ProvisionRequest[0]);
        } else {
            /* Get the ProvisionRequest instances for a ServiceElement */
            ArrayList<ProvisionRequest> items = new ArrayList<ProvisionRequest>();
            Set<Key> keys = collection.keySet();
            for (Key key : keys) {
                ProvisionRequest pr = collection.get(key);
                if (sElem.equals(pr.getServiceElement())) {
                    items.add(pr);
                }
            }
            prs = items.toArray(new ProvisionRequest[0]);
        }
        return prs;
    }
    
    /**
     * Dumps the contents of the collection
     */
    public void dumpCollection() {
        if(logger.isTraceEnabled()) {
            ProvisionRequest[] elements = getProvisionRequests(null);
            StringBuilder buffer = new StringBuilder();
            int x=1;
            buffer.append("\n");
            buffer.append(type);
            buffer.append(" collection size : ");
            buffer.append(elements.length).append("\n");
            buffer.append("--\n");
            for (ProvisionRequest element : elements) {
                ServiceElement sElem = element.getServiceElement();
                Long id = sElem.getServiceBeanConfig().getInstanceID();
                buffer.append(x++);
                buffer.append(" ");
                buffer.append(sElem.getOperationalStringName());
                buffer.append("/");
                buffer.append(sElem.getName());
                if(!type.startsWith("Fixed")) {
                    buffer.append(" instanceID=");
                    buffer.append(id);
                }
                buffer.append("\n");
            }
            buffer.append("--");
            logger.trace(buffer.toString());
        }
    }
}
