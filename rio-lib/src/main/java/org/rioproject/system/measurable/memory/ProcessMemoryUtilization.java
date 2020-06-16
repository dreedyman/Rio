/*
 * Copyright 2008 to the original author or authors.
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

import org.rioproject.system.MeasuredResource;
import org.rioproject.watch.ThresholdValues;

import java.io.Serializable;

/**
 * Measured process memory utilization
 */
public class ProcessMemoryUtilization extends MeasuredResource
    implements Serializable {
    static final long serialVersionUID = 1L;
    private double vSize = -1;
    private double resident = -1;
    private double shared = -1;
    private double usedHeap = -1;
    private double maxHeap = -1;
    private double committedHeap = -1;
    private double initHeap = -1;
    private double usedNonHeap = -1;
    private double maxNonHeap = -1;
    private double committedNonHeap = -1;
    private double initNonHeap = -1;

    /**
     * Construct ProcessMemoryUtilization with parameters
     *
     * @param identifier Identifier for the ProcessMemoryUtilization
     * @param utilization The measured utilization
     * @param tVals ThresholdValues for the ProcessMemoryUtilization
     */
    public ProcessMemoryUtilization(String identifier,
                                    double utilization,
                                    ThresholdValues tVals) {
        super(identifier, utilization, tVals);
    }

    /**
     * Construct ProcessMemoryUtilization with parameters
     *
     * @param identifier Identifier for the ProcessMemoryUtilization
     * @param utilization The measured utilization, calculated as the
     * used-heap-use/total-heap-use
     * @param initHeap The amount of heap memory (in MB) that the JVM
     * initially requests
     * @param usedHeap The amount of heap memory (in MB) used for the process
     * @param maxHeap The maximum amount of memory (in MB) for the JVM
     * @param committedHeap The amount of committed memory (in MB) for the JVM
     * @param initNonHeap The amount of non-heap memory (in MB) that the JVM
     * initially requests
     * @param usedNonHeap The amount of non-heap memory (in MB) used by the JVM
     * @param maxNonHeap The current non-heap memory size (in MB) that is used
     * by the JVM
     * @param committedNonHeap The amount of non-heap committed memory (in MB)
     * for the JVM
     * @param tVals ThresholdValues for the ProcessMemoryUtilization
     */
    public ProcessMemoryUtilization(String identifier,
                                    double utilization,
                                    double initHeap,
                                    double usedHeap,
                                    double maxHeap,
                                    double committedHeap,
                                    double initNonHeap,
                                    double usedNonHeap,
                                    double maxNonHeap,
                                    double committedNonHeap,
                                    ThresholdValues tVals) {
        super(identifier, utilization, tVals);
        this.initHeap = initHeap;
        this.usedHeap = usedHeap;
        this.maxHeap = maxHeap;
        this.committedHeap = committedHeap;
        this.initNonHeap = initNonHeap;
        this.usedNonHeap = usedNonHeap;
        this.maxNonHeap = maxNonHeap;
        this.committedNonHeap = committedNonHeap;
    }



    /**
     * Construct ProcessMemoryUtilization with parameters
     *
     * @param identifier Identifier for the ProcessMemoryUtilization
     * @param utilization The measured utilization, calculated as the
     * used-heap-use/total-heap-use
     * @param vSize Total process virtual memory
     * @param resident Total process resident memory
     * @param shared Total process shared memory
     * @param tVals ThresholdValues for the ProcessMemoryUtilization
     */
    public ProcessMemoryUtilization(String identifier,
                                    double utilization,
                                    double vSize,
                                    double resident,
                                    double shared,
                                    ThresholdValues tVals) {
        super(identifier, utilization, tVals);
        this.vSize = vSize;
        this.resident = resident;
        this.shared = shared;
    }

    /**
     * Construct ProcessMemoryUtilization with parameters
     *
     * @param identifier Identifier for the ProcessMemoryUtilization
     * @param utilization The measured utilization
     * @param vSize Total process virtual memory
     * @param resident Total process resident memory
     * @param shared Total process shared memory
     * @param initHeap The amount of heap memory (in MB) that the JVM
     * initially requests
     * @param usedHeap The amount of heap memory (in MB) used for the process
     * @param maxHeap The maximum amount of memory (in MB) for the JVM
     * @param committedHeap The amount of committed memory (in MB) for the JVM
     * @param initNonHeap The amount of non-heap memory (in MB) that the JVM
     * initially requests
     * @param usedNonHeap The amount of non-heap memory (in MB) used by the JVM
     * @param maxNonHeap The current non-heap memory size (in MB) that is used
     * by the JVM
     * @param committedNonHeap The amount of non-heap committed memory (in MB)
     * for the JVM
     * @param tVals ThresholdValues for the ProcessMemoryUtilization
     */
    public ProcessMemoryUtilization(String identifier,
                                    double utilization,
                                    double vSize,
                                    double resident,
                                    double shared,
                                    double initHeap,
                                    double usedHeap,
                                    double maxHeap,
                                    double committedHeap,
                                    double initNonHeap,
                                    double usedNonHeap,
                                    double maxNonHeap,
                                    double committedNonHeap,
                                    ThresholdValues tVals) {
        super(identifier, utilization, tVals);
        this.vSize = vSize;
        this.resident = resident;
        this.shared = shared;
        this.initHeap = initHeap;
        this.usedHeap = usedHeap;
        this.maxHeap = maxHeap;
        this.committedHeap = committedHeap;
        this.initNonHeap = initNonHeap;
        this.usedNonHeap = usedNonHeap;
        this.maxNonHeap = maxNonHeap;
        this.committedNonHeap = committedNonHeap;
    }

    /**
     * Get the Total process virtual memory
     *
     * @return Total process virtual memory (in MB), if undefined return <tt>-1</tt>
     */
    public double getVirtualMemorySize() {
        return vSize;
    }


    /**
     * Get the Total process resident (real) memory
     *
     * @return Total process resident (real) memory (in MB), if undefined
     * return <tt>-1</tt>
     */
    public double getResident() {
        return resident;
    }

    /**
     * Get the Total process shared memory
     *
     * @return Total process shared memory (in MB), if undefined return <tt>-1</tt>
     */
    public double getShared() {
        return shared;
    }

    /**
     * Returns the current memory usage of the heap that is used for object
     * allocation. The heap consists of one or more memory pools.
     *
     * <p>The amount of used memory in the returned memory usage is the amount
     * of memory occupied by both live objects and garbage objects that have
     * not been collected, if any.
     *
     * @return The sum of all <tt>used</tt> values across all heap memory
     * pools (in MB) .
     *
     * @see java.lang.management.MemoryMXBean#getHeapMemoryUsage()
     */
    public double getUsedHeap() {
        return usedHeap;
    }

    /**
     * Get the maximum amount of memory (in bytes) that can be used for heap
     * memory management. Its value may be undefined. The maximum amount of
     * memory may change over time if defined.
     *  
     * @return The maximum amount of memory (in MB) that can be allocated. If
     * undefined return <tt>-1</tt>
     *
     * @see java.lang.management.MemoryMXBean#getHeapMemoryUsage()
     */
    public double getMaxHeap() {
        return maxHeap;
    }

    /**
     * Get the amount of memory (in bytes) that is guaranteed to be available
     * for use by the Java virtual machine.
     *
     * @return The amount of committed memory (in MB).
     *
     * @see java.lang.management.MemoryMXBean#getHeapMemoryUsage()
     */
    public double getCommittedHeap() {
        return committedHeap;
    }

    /**
     * Get the amount of heap memory in bytes that the Java virtual machine
     * initially requests from the operating system for memory management.
     *  
     * @return The initial size of memory (in MB). If undefined return <tt>-1</tt>
     *
     * @see java.lang.management.MemoryMXBean#getHeapMemoryUsage()
     */
    public double getInitHeap() {
        return initHeap;
    }

    /**
     * Returns the current memory usage of the non-heap that is used for object
     * allocation. The heap consists of one or more memory pools.
     *
     * @return The sum of all <tt>used</tt> values (in MB) across all non-heap
     * memory pools.
     *
     * @see java.lang.management.MemoryMXBean#getNonHeapMemoryUsage()
     */
    public double getUsedNonHeap() {
        return usedNonHeap;
    }

    /**
     * Get the maximum amount of memory (in MB) that can be used for non-heap
     * memory management.
     *
     * @return The maximum amount of memory (in MB) that can be allocated for
     * non-heap memory management. If undefined return <tt>-1</tt>
     *
     * @see java.lang.management.MemoryMXBean#getNonHeapMemoryUsage()
     */
    public double getMaxNonHeap() {
        return maxNonHeap;
    }

    /**
     * Get the amount of non-heap memory (in MB) that is guaranteed to be
     * available for use by the Java virtual machine.
     *
     * @return The amount of committed non-heap memory (in MB).
     *
     * @see java.lang.management.MemoryMXBean#getNonHeapMemoryUsage()
     */
    public double getCommittedNonHeap() {
        return committedNonHeap;
    }

    /**
     * Get the amount of non-heap memory (in MB) that the Java virtual machine
     * initially requests from the operating system for memory management.
     *
     * @return The initial size of non-heap memory (in MB). If
     * undefined return <tt>-1</tt>
     *
     * @see java.lang.management.MemoryMXBean#getHeapMemoryUsage()
     */
    public double getInitNonHeap() {
        return initNonHeap;
    }

    @Override
    public String toString() {
        return "ProcessMemoryUtilization{" +
                "utilization="+getValue()+
                ", vSize=" + vSize +
                ", resident=" + resident +
                ", shared=" + shared +
                ", usedHeap=" + usedHeap +
                ", maxHeap=" + maxHeap +
                ", committedHeap=" + committedHeap +
                ", initHeap=" + initHeap +
                ", usedNonHeap=" + usedNonHeap +
                ", maxNonHeap=" + maxNonHeap +
                ", committedNonHeap=" + committedNonHeap +
                ", initNonHeap=" + initNonHeap +
                '}';
    }
}
