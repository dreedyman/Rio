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

import org.rioproject.system.SystemWatchID;
import org.rioproject.system.measurable.memory.CalculableSystemMemory;
import org.rioproject.system.measurable.memory.SystemMemoryUtilization;
import org.rioproject.watch.WatchDataReplicator;

/**
 * Describes the amount of memory available for the system as a qualitative resource.
 *
 * @author Dennis Reedy
 */
@SuppressWarnings("unused")
public class SystemMemory extends ByteOrientedDevice implements WatchDataReplicator<CalculableSystemMemory> {
    static final long serialVersionUID = 1L;
    static final String DEFAULT_DESCRIPTION = "System Memory";
    public static final String ID = SystemWatchID.SYSTEM_MEMORY;

    /**
     * Create a Memory capability
     */
    public SystemMemory() {
        this(DEFAULT_DESCRIPTION);
    }

    /**
     * Create a Memory capability with a description
     *
     * @param description The description
     */
    public SystemMemory(final String description) {
        this.description = description;
        define(NAME, ID);
    }

    public void addCalculable(CalculableSystemMemory calculable) {
        SystemMemoryUtilization memoryUtilization = calculable.getMemoryUtilization();
        Double dFree = memoryUtilization.getFree();
        Double dTotal = memoryUtilization.getTotal();

        /* The values will come to us in MB, need to convert to bytes */
        dTotal = dTotal*MB;
        dFree = dFree*MB;
        capabilities.put(CAPACITY, dTotal);
        capabilities.put(AVAILABLE, dFree);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        SystemMemory memory = (SystemMemory) o;
        return description.equals(memory.description);

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
