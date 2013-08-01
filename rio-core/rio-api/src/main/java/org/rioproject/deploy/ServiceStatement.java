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
import org.rioproject.opstring.ServiceElement;

import java.io.Serializable;
import java.util.*;

/**
 * A ServiceStatement contains {@link ServiceRecord}
 * instances documenting service instantiation(s) and usage information
 *
 * @author Dennis Reedy
 */
public class ServiceStatement implements Serializable {
    static final long serialVersionUID = 1L;
    /**
     * The ServiceElement
     */
    private final ServiceElement sElem;
    /**
     * A Map of ServiceRecord instances
     */
    private final Map<Uuid, List<ServiceRecord>> serviceRecords = new HashMap<Uuid, List<ServiceRecord>>();

    /**
     * Create a ServiceStatement
     * 
     * @param sElem The sElem for the ServiceBean
     */
    public ServiceStatement(final ServiceElement sElem) {
        if(sElem == null)
            throw new IllegalArgumentException("sElem is null");
        this.sElem = sElem;
    }

    /**
     * Get the ServiceElement 
     * 
     * @return The ServiceElement for the ServiceBean
     */
    public ServiceElement getServiceElement() {
        return (sElem);
    }

    /**
     * Get the organization property
     * 
     * @return The organization (representative owner) of the service. If the 
     * organization property is null, an empty String will be returned
     */
    public String getOrganization() {
        String organization = sElem.getServiceBeanConfig().getOrganization();
        organization = (organization==null ? "" : organization);
        return (organization);
    }

    /**
     * @return If there are any ServiceRecord instances with a type of
     * ServiceRecord.ACTIVE return true
     */
    public boolean hasActiveServiceRecords() {
        ServiceRecord[] records =  getServiceRecords(ServiceRecord.ACTIVE_SERVICE_RECORD);
        return records.length != 0;
    }

    /**
     * Add a ServiceRecord
     * 
     * @param instantiatorID The ServiceBeanInstantiator id to store the record
     * for
     * @param record The ServiceRecord to add
     * @throws IllegalArgumentException If either of the parameters are null
     */
    public void putServiceRecord(final Uuid instantiatorID, final ServiceRecord record) {
        if(instantiatorID == null)
            throw new IllegalArgumentException("instantiatorID is null");
        if(record == null)
            throw new IllegalArgumentException("record is null");
        synchronized(serviceRecords) {
            List<ServiceRecord> recordList;
            if(!serviceRecords.containsKey(instantiatorID))
                recordList = new ArrayList<ServiceRecord>();
            else
                recordList = serviceRecords.get(instantiatorID);
            int index = recordList.indexOf(record);
            if(index != -1)
                recordList.set(index, record);
            else
                recordList.add(record);
            serviceRecords.put(instantiatorID, recordList);
        }
    }

    /**
     * Get all ServiceRecord instances
     * 
     * @return Array of ServiceRecord objects. If there are no
     * records found, a zero length array will be returned
     */
    public ServiceRecord[] getServiceRecords() {
        Collection<ServiceRecord> collection = new ArrayList<ServiceRecord>();
        synchronized(serviceRecords) {
            Collection<List<ServiceRecord>> values = serviceRecords.values();
            for (List<ServiceRecord> list : values) {
                collection.addAll(list);
            }
        }
        return (collection.toArray(new ServiceRecord[collection.size()]));
    }

    /**
     * Get ServiceRecord instances for ServiceRecord type(s)
     * 
     * @param type The type of ServiceRecord to get, either
     * ServiceRecord.ACTIVE_SERVICE_RECORD or
     * ServiceRecord.INACTIVE_SERVICE_RECORD.
     * @return Array of ServiceRecord objects based on the
     * provided type(s). If there are no records found, a zero length array will
     * be returned
     */
    public ServiceRecord[] getServiceRecords(final int type) {
        return (filterServiceRecords(type, getServiceRecords()));
    }

    /**
     * Get ServiceRecord instances for a key and ServiceRecord type(s)
     * 
     * @param instantiatorID The ServiceBeanInstantiator id
     * @param type The type of ServiceRecord to get, either
     * ServiceRecord.ACTIVE_SERVICE_RECORD or
     * ServiceRecord.INACTIVE_SERVICE_RECORD.
     * @return An array of ServiceRecord instances catalogued
     * with the provided key. A new array will be allocated each time. If there
     * are no ServiceRecords found, an empty array will be returned
     */
    public ServiceRecord[] getServiceRecords(final Uuid instantiatorID, final int type) {
        if(instantiatorID == null)
            throw new IllegalArgumentException("key is null");
        ServiceRecord[] records;
        synchronized(serviceRecords) {
            List<ServiceRecord> recordList;
            if(!serviceRecords.containsKey(instantiatorID))
                recordList = new ArrayList<ServiceRecord>();
            else
                recordList = serviceRecords.get(instantiatorID);
            records = recordList.toArray(new ServiceRecord[recordList.size()]);
        }
        return (filterServiceRecords(type, records));
    }

    /**
     * Get all ServiceRecord instances for a ServiceBean identifier
     * 
     * @param sbid The Uuid for a ServiceBean
     * @return Array of ServiceRecord objects based on the
     * provided identifier. If there are no records found, a zero length array
     * will be returned
     */
    public ServiceRecord[] getServiceRecords(final Uuid sbid) {
        return (filterServiceRecords(sbid, getServiceRecords()));
    }

    /**
     * Get all ServiceRecord instances for a ServiceBean identifier and key
     * 
     * @param sbid The identifier for a ServiceBean
     * @param instantiatorID The ServiceBeanInstantiator id to retrieve
     * ServiceRecord instances
     * @return Array of ServiceRecord objects based on the
     * provided identifier. If there are no records found, a zero length array
     * will be returned
     */
    public ServiceRecord[] getServiceRecords(final Uuid sbid, final Uuid instantiatorID) {
        return (filterServiceRecords(sbid, getServiceRecords(instantiatorID,
                                                             ServiceRecord.ACTIVE_SERVICE_RECORD |
                                                             ServiceRecord.INACTIVE_SERVICE_RECORD)));
    }

    /*
     * Filter ServiceRecord instances based on type
     */
    private ServiceRecord[] filterServiceRecords(final int type, final ServiceRecord[] records) {
        if(type == 0 || type != (type & (ServiceRecord.ACTIVE_SERVICE_RECORD | ServiceRecord.INACTIVE_SERVICE_RECORD)))
            throw new IllegalArgumentException("invalid recordType");
        if(((type & ServiceRecord.ACTIVE_SERVICE_RECORD) != 0) && ((type & ServiceRecord.INACTIVE_SERVICE_RECORD) != 0)) {
            return (records);
        }
        ArrayList<ServiceRecord> list = new ArrayList<ServiceRecord>();

        for (ServiceRecord record : records) {
            if ((type & ServiceRecord.ACTIVE_SERVICE_RECORD) != 0) {
                if ((record.getType() & ServiceRecord.ACTIVE_SERVICE_RECORD) != 0) {
                    list.add(record);
                }
            }
            if ((type & ServiceRecord.INACTIVE_SERVICE_RECORD) != 0) {
                if ((record.getType() & ServiceRecord.INACTIVE_SERVICE_RECORD) != 0) {
                    list.add(record);
                }
            }
        }
        return (list.toArray(new ServiceRecord[list.size()]));
    }

    /*
     * Filter ServiceRecord instances based on an identifier
     */
    private ServiceRecord[] filterServiceRecords(final Uuid identifier, final ServiceRecord[] records) {
        ArrayList<ServiceRecord> list = new ArrayList<ServiceRecord>();
        for (ServiceRecord record : records) {
            if (record.getServiceID().equals(identifier))
                list.add(record);
        }
        return (list.toArray(new ServiceRecord[list.size()]));
    }
}
