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
package org.rioproject.system.measurable.memory;

import org.rioproject.watch.Calculable;

/**
 * A Calculable used to collect process Memory utilization
 *
 * @author Dennis Reedy
 */
public class CalculableMemory extends Calculable {
    static final long serialVersionUID = 1L;
    /**
     * Holds value of property containing details about memory utilization for a
     * process.
     */
    private ProcessMemoryUtilization memoryUtilization;

    /**
     * Creates new CalculableMemory
     *
     * @param id The identifier for this Calculable record
     * @param utilization The utilization for the Calculable Record
     * @param when The time when the recorded utilization was captured
     */
    public CalculableMemory(String id, double utilization, long when) {
        super(id, utilization, when);
    }

    /**
     * Creates new CalculableMemory
     *
     * @param id The identifier for this Calculable record
     * @param utilization The utilization for the Calculable Record
     * @param memoryUtilization Contains details for JVM memory utilization
     * @param when The time when the recorded utilization was captured
     */
    public CalculableMemory(String id, double utilization, ProcessMemoryUtilization memoryUtilization, long when) {
        super(id, utilization, when);
        this.memoryUtilization = memoryUtilization;
    }

    /**
     * Getter for property totalMemory.
     *
     * @return Value of property totalMemory.
     */
    public double getTotalMemory() {
        if(memoryUtilization==null)
            return Double.NaN;
        return memoryUtilization.getMaxHeap();
    }

    /**
     * Get the used memory
     *
     * @return The amount of used memory
     */
    public double getUsedMemory() {
        if(memoryUtilization==null)
            return Double.NaN;
        return(getValue()*memoryUtilization.getMaxHeap());
    }

    /**
     * Get the free memory
     *
     * @return The amount of free memory
     */
    public double getFreeMemory() {
        if(memoryUtilization==null)
            return Double.NaN;
        return (memoryUtilization.getMaxHeap()-getUsedMemory());
    }

    /**
     * Returns a string representation of the object.
     *
     * @return a string representation of the object.
     */
    public String toString() {
        return String.format("%s - id: [%s], total: [%s], used: [%s], free: [%s]",
                             getFormattedDate(), getId(), getTotalMemory(), getUsedMemory(), getFreeMemory());
    }
}
