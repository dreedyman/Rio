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
package org.rioproject.system.measurable.cpu;

import org.rioproject.system.MeasuredResource;
import org.rioproject.watch.ThresholdValues;

import java.io.Serializable;
import java.util.Arrays;

/**
 * Measured system CPU utilization
 */
public class CpuUtilization extends MeasuredResource implements Serializable {
    static final long serialVersionUID = 1L;
    private double system = Double.NaN;
    private double user = Double.NaN;
    private double total = Double.NaN;
    private double load[] = new double[]{Double.NaN};
    /** The number of available processors on the machine */
    private int availableProcessors;

    /**
     * Construct a CpuUtilization with parameters
     *
     * @param identifier Identifier for the CpuUtilization
     * @param total The total system cpu utilization
     * @param tVals ThresholdValues for the CpuUtilization
     */
    public CpuUtilization(String identifier,
                          double total,
                          ThresholdValues tVals) {
        super(identifier, total, tVals);
        this.total = total;
    }

    /**
     * Construct a CpuUtilization with parameters
     *
     * @param identifier Identifier for the CpuUtilization
     * @param system The cpu kernel usage
     * @param user The cpu user usage
     * @param load The system load average
     * @param availableProcessors The number of available processors on the machine
     * @param tVals ThresholdValues for the CpuUtilization
     */
    public CpuUtilization(String identifier,
                          double system,
                          double user,
                          double[] load,
                          int availableProcessors,
                          ThresholdValues tVals) {
        super(identifier, system + user, tVals);
        this.system = system;
        this.user = user;
        this.total = system + user;
        if(load!=null) {
            this.load = new double[load.length];
            System.arraycopy(load, 0, this.load, 0, load.length);
        }
        this.availableProcessors = availableProcessors;
    }

    /**
     * Get the cpu kernel usage
     *
     * @return The cpu kernel use as a percentage; or a Double.NaN if not
     *         available.
     */
    public double getSystem() {
        return system;
    }

    /**
     * Get the cpu user usage
     *
     * @return The cpu user use as a percentage; or a Double.NaN if not
     *         available.
     */
    public double getUser() {
        return user;
    }

    /**
     * Get the cpu utilization
     *
     * @return The cpu utilization; or a Double.NaN if not available.
     */
    public double getTotal() {
        return total;
    }

    /**
     * Get the system load
     *
     * @return The system load average; or a Double.NaN if not available.
     */
    public double[] getLoad() {
        return load;
    }

    /**
     * Get the number of processors on the machine
     *
     * @return The number of available processors on the machine
     */
    public int getAvailableProcessors() {
        return availableProcessors;
    }

    public String toString() {
        return "CpuUtilization{" +
               "system=" + system +
               ", user=" + user +
               ", total=" + total +
               ", load=" + Arrays.toString(load) +
               ", availableProcessors=" + availableProcessors +
               '}';
    }

}
