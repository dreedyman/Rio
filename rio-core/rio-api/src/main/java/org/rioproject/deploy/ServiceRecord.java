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
package org.rioproject.deploy;

import net.jini.id.Uuid;
import org.rioproject.costmodel.ResourceCost;
import org.rioproject.opstring.ServiceElement;

import java.io.Serializable;
import java.util.*;

/**
 * A <tt>ServiceRecord</tt> documents service instantiated time, discard time,
 * where the service ran and contains
 * {@link org.rioproject.costmodel.ResourceCost} information for
 * system resources accessed during the service's execution.
 *
 * @author Dennis Reedy
 */
public class ServiceRecord implements Comparable, Serializable {
    @SuppressWarnings("unused")
    static final long serialVersionUID = 1L;
    /**
     * Indicates a service which is currently active,
     */
    public static final int ACTIVE_SERVICE_RECORD = 1;
    /**
     * Specifies a service which has been discarded, is not longer active
     */
    public static final int INACTIVE_SERVICE_RECORD = 1 << 1;
    /**
     * The uuid for the service
     */
    private Uuid uuid;
    /**
     * The hostname of the compute resource the service is 
     * running on
     */
    private String hostName;
    /**
     * The Date the service was instantiated
     */
    private Date instantiated;
    /**
     * The most recent Date the ServiceRecord was updated
     */
    private Date updated;
    /**
     * The Date the service was discarded
     */
    private Date discarded;
    /**
     * The ServiceElement for the service
     */
    private ServiceElement sElem;
    /**
     * The type of this record, ACTIVE_SERVICE_RECORD or INACTIVE_SERVICE_RECORD
     */
    private int type;
    /**
     * If the service is forked, and active, the corresponding pid of the exec'd service
     */
    private int pid=-1;
    /**
     * ResourceCost instances for the service
     */
    private final List<ResourceCost> resourceCosts =
        new ArrayList<ResourceCost>();

    /**
     * Create a ServiceRecord. The <code>type</code> property is set to
     * <code>ACTIVE_SERVICE_RECORD</code>, and the <code>instantiated</code>
     * property set to the current Date
     * 
     * @param uuid Unique ID of the service
     * @param sElem The ServiceElement
     * @param hostName The hostname of the compute resource the service is
     * running on
     */
    public ServiceRecord(Uuid uuid, ServiceElement sElem, String hostName) {
        this(uuid, sElem, hostName, ACTIVE_SERVICE_RECORD, new Date());
    }

    /**
     * Create a ServiceRecord. The <code>type</code> property is set to
     * <code>ACTIVE_SERVICE_RECORD</code>, and the <code>instantiated</code>
     * property set to the current Date
     * 
     * @param uuid Unique ID of the service
     * @param sElem The ServiceElement
     * @param hostName The hostname of the compute resource the service is 
     * running on 
     * @param type The type of the record
     * @param instantiated The Date the service has been instantiated
     */
    public ServiceRecord(Uuid uuid, 
                         ServiceElement sElem,
                         String hostName,
                         int type,
                         Date instantiated) {        
        if(uuid == null)
            throw new IllegalArgumentException("uuid is null");
        if(sElem == null)
            throw new IllegalArgumentException("sElem is null");
        if(hostName == null)
            throw new IllegalArgumentException("hostName is null");
        doSetType(type);
        this.uuid = uuid;
        this.sElem = sElem;
        this.hostName = hostName;
        this.instantiated = new Date(instantiated.getTime());
    }

    /**
     * Get the uuid for the service
     * 
     * @return  The unique ID for the service
     */
    public Uuid getServiceID() {
        return (uuid);
    }

    /**
     * @return The service name
     */
    public String getName() {
        return (sElem.getName());
    }
    
    /**
     * @return The hostName
     */
    public String getHostName() {
        return(hostName);
    }

    /**
     * Set the type of this record to be either
     * <code>ACTIVE_SERVICE_RECORD</code> or
     * <code>INACTIVE_SERVICE_RECORD</code><br>
     * 
     * @param type The type of the ServiceRecord
     * @throws IllegalArgumentException if the supplied type is neither
     * <code>ACTIVE_SERVICE_RECORD</code> or
     * <code>INACTIVE_SERVICE_RECORD</code>
     */
    public void setType(int type) {
        doSetType(type);
    }

    private void doSetType(int type) {
        if(type == 0 || type != (type & (ACTIVE_SERVICE_RECORD | INACTIVE_SERVICE_RECORD)))
            throw new IllegalArgumentException("bad type");
        this.type = type;
    }

    /**
     * Get the type of ServiceRecord <br>
     * 
     * @return <code>ACTIVE_SERVICE_RECORD</code> or
     * <code>INACTIVE_SERVICE_RECORD</code>
     */
    public int getType() {
        return (type);
    }

    /**
     * @return The <code>Date</code> the service was instantiated
     */
    public Date getInstantiationDate() {
        Date d = null;
        if(instantiated != null)
            d = new Date(instantiated.getTime());
        return (d);
    }

    /**
     * Set that the ServiceRecord has been updated
     */
    public void setUpdated() {
        this.updated = new Date();
    }

    /**
     * @return The Date that the ServiceRecord was been updated
     */
    public Date getUpdated() {
        Date d = null;
        if(updated != null)
            d = new Date(updated.getTime());
        return (d);
    }

    /**
     * @param discarded The  <code>Date</code> the service was discarded
     */
    public void setDiscardedDate(Date discarded) {
        if(discarded!=null)
            this.discarded = new Date(discarded.getTime());
    }

    /**
     * @return The <code>Date</code> the service was discarded. If the
     * service has not been discarded then this method will return
     * <code>null</code>
     */
    public Date getDiscardedDate() {
        Date d = null;
        if(discarded!=null)
            d = new Date(discarded.getTime());
        return (d);
    }

    /**
     * @return The ServiceElement object
     */
    public ServiceElement getServiceElement() {
        return (sElem);
    }

    /**
     * Compute the elapsed time the service has been active. If the service
     * is still active the value returned will be based on the current time
     * 
     * @return The elapsed time (in milliseconds) the service has been instantiated
     */
    public long computeElapsedTime() {
        long t0 = instantiated.getTime();
        long t1 = System.currentTimeMillis();
        if(discarded != null
           && ((type & ServiceRecord.INACTIVE_SERVICE_RECORD) != 0)) {
            t1 = discarded.getTime();
        }
        return (t1 - t0);
    }

    /**
     * Add a ResourceCost
     * 
     * @param resourceCost The ResourceCost to add. If the ResourceCost for
     * a named resource already exists, it will be replaced
     */
    public void addResourceCost(ResourceCost resourceCost) {
        if(resourceCost == null)
            throw new IllegalArgumentException("resourceCost is null");
        synchronized(resourceCosts) {
            ResourceCost[] rCosts =
                resourceCosts.toArray(new ResourceCost[resourceCosts.size()]);
            boolean updated = false;
            for(ResourceCost rCost : rCosts) {
                if(rCost.getResourceName().equals(resourceCost.getResourceName())) {
                    int ndx = resourceCosts.indexOf(rCost);
                    resourceCosts.set(ndx, resourceCost);
                    updated = true;
                    break;
                }
            }
            if(!updated)
                resourceCosts.add(resourceCost);
        }
    }

    /**
     * Get the ResourceCost instances.
     * 
     * @return An array of ResourceCost instances. A new array
     * will be allocated each time. If there are no ResourceCost instances, an
     * empty array will be returned. 
     */
    public ResourceCost[] getResourceCosts() {
        ResourceCost[] rCosts;
        synchronized(resourceCosts) {
            rCosts = resourceCosts.toArray(new ResourceCost[resourceCosts.size()]);
        }
        return (rCosts);
    }

    /**
     * Compares this ServiceRecord to another ServiceRecord with respect to the
     * ServiceRecord instantiation Date.
     * 
     * @param o The Object to be compared.
     * @return A negative integer if this ServiceRecord has an instantiatedDate
     * before the ServiceRecord being compared to, zero if the instantiatedDate
     * property is equal, or a positive number if the instantiatedDate is later.
     */
    public int compareTo(Object o) {
        ServiceRecord serviceRecord = (ServiceRecord)o;
        return (instantiated.compareTo(serviceRecord.getInstantiationDate()));
    }

    public int getPid() {
        return pid;
    }

    public void setPid(int pid) {
        this.pid = pid;
    }

    /**
     * A ServiceRecord is equal to another ServiceRecord if their Uuid's
     * and instantiation Dates are equal
     */
    public boolean equals(Object o) {
        if(this == o)
            return (true);
        if(!(o instanceof ServiceRecord))
            return (false);
        ServiceRecord that = (ServiceRecord)o;
        return (this.uuid.equals(that.uuid) && 
                this.instantiated.equals(that.instantiated));
    }

    /**
     * Override hashCode to return the hashCode of the Uuid and the
     * instantiated Date
     */
    public int hashCode() {
        int hc = 17;
        hc = 37*hc+uuid.hashCode();
        hc = 37*hc+instantiated.hashCode();
        return (hc);
    }

    public String toString() {
        return "ServiceRecord {" +
               "hostName='" + hostName + '\'' +
               ", instantiated=" + instantiated +
               ", updated=" + updated +
               ", discarded=" + discarded +
               ", service=" + sElem.getName() +
               ", resourceCosts=" + resourceCosts +
               ", type=" + type +
               '}';
    }
}
