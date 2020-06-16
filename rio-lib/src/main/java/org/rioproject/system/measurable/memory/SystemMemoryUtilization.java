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
 * Measured system memory utilization
 */
public class SystemMemoryUtilization extends MeasuredResource
    implements Serializable {
    static final long serialVersionUID = 1L;
    private double total = -1;
    private double free = -1;
    private double used = -1;
    private double freePerc = -1;
    private double usedPerc = -1;
    private double ram = -1;

    /**
     * Construct a SystemMemoryUtilization
     *
     * @param identifier Identifier for the SystemMemoryUtilization
     * @param tVals ThresholdValues for the SystemMemoryUtilization
     */
    public SystemMemoryUtilization(String identifier,
                                   ThresholdValues tVals) {
        super(identifier, (double)-1, tVals);
    }

    /**
     * Construct a SystemMemoryUtilization
     *
     * @param identifier Identifier for the SystemMemoryUtilization
     * @param value The utilized system memory
     * @param total The amount of total system memory in MB
     * @param free The amount of free memory in MB
     * @param used The amount of free memory in MB
     * @param freePerc The percentage of free memory
     * @param usedPerc The percentage of used memory
     * @param ram The amount of RAM (in MB) the system has
     * @param tVals ThresholdValues for the SystemMemoryUtilization
     */
    public SystemMemoryUtilization(String identifier,
                                   double value,
                                   double total,
                                   double free,
                                   double used,
                                   double freePerc,
                                   double usedPerc,
                                   double ram,
                                   ThresholdValues tVals) {
        super(identifier, value, tVals);
        this.total = total;
        this.free = free;
        this.used = used;
        this.freePerc = freePerc;
        this.usedPerc = usedPerc;
        this.ram = ram;
    }

    /**
     * Get the amount of total system memory
     *
     * @return The amount of total system memory in MB, or -1 if not available
     */
    public double getTotal() {
        return total;
    }

    /**
     * Get the amount of free memory
     *
     * @return The amount of free memory in MB, or -1 if not available
     */
    public double getFree() {
        return free;
    }

    /**
     * Get the amount of used memory
     *
     * @return The amount of used memory in MB, or -1 if not available
     */
    public double getUsed() {
        return used;
    }

    /**
     * Get the percentage of free memory
     *
     * @return The percentage of free memory
     */
    public double getFreePercentage() {
        return freePerc;
    }

    /**
     * Get the percentage of used memory
     *
     * @return The percentage of used memory
     */
    public double getUsedPercentage() {
        return usedPerc;
    }

    /**
     * Get the amount of Random Access Memory (RAM) the system has
     *
     * @return The amount of RAM (in MB) the system has
     */
    public double getRam() {
        return ram;
    }


    public String toString() {
        return "SystemMemoryUtilization {" +
               "total=" + total +
               ", free=" + free +
               ", used=" + used +
               ", freePerc=" + freePerc +
               ", usedPerc=" + usedPerc +
               ", ram=" + ram +
               '}';
    }
 }
