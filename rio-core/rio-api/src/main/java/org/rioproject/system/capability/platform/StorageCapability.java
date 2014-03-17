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
package org.rioproject.system.capability.platform;

import org.rioproject.system.measurable.disk.CalculableDiskSpace;
import org.rioproject.watch.WatchDataReplicator;

/**
 * The <code>StorageCapability</code> object provides a definition of storage
 * support
 *
 * @author Dennis Reedy
 */
public class StorageCapability extends ByteOrientedDevice implements WatchDataReplicator<CalculableDiskSpace> {
    private static final long serialVersionUID = 1L;
    static final String DEFAULT_DESCRIPTION = "Storage Capability";
    /** Storage media type */
    public final static String TYPE = "StorageType";
    public final static String ID = "Disk";

    /**
     * Create a StorageCapability
     */
    public StorageCapability() {
        this(DEFAULT_DESCRIPTION);
    }

    /** 
     * Create a StorageCapability with a description
     *
     * @param description The description
     */
    public StorageCapability(String description) {
        this.description = description;
        define(NAME, ID);
    }

    /**
     * Determine if the requested available diskspace can be met by the
     * StorageCapability
     * 
     * @param requestedSize The request amount of disk space
     * @return Return true is the available amount of disk space is less then
     * the request amount
     */
    public boolean supports(int requestedSize) {
        double available = 0;
        Double dAvail = (Double)getValue(AVAILABLE);
        if(dAvail!=null)
            available = dAvail;
        return(requestedSize<available);
    }

    public void addCalculable(CalculableDiskSpace calculable) {
        Double dAvail = calculable.getAvailable();
        Double dCap = calculable.getCapacity();
        capabilities.put(CAPACITY, dCap);
        capabilities.put(AVAILABLE, dAvail);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        StorageCapability storageCapability = (StorageCapability) o;
        return description.equals(storageCapability.description);

    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + description.hashCode();
        return result;
    }

    public void close() {
    }
}
