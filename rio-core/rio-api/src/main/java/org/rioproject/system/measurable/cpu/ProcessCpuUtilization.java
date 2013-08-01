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
package org.rioproject.system.measurable.cpu;

import org.rioproject.system.MeasuredResource;
import org.rioproject.watch.ThresholdValues;

import java.io.Serializable;

/**
 * Measured process CPU utilization
 */
public class ProcessCpuUtilization extends MeasuredResource implements Serializable {
    static final long serialVersionUID = 1L;
    private long system = -1;
    private long user = -1;
    private double total = -1;
    private double totalPercentage = -1;
    /**
     * Construct a ProcessCpuUtilization with parameters
     *
     * @param identifier Identifier for the ProcessCpuUtilization
     * @param totalPercentage The total system cpu utilization percentage
     * @param tVals ThresholdValues for the ProcessCpuUtilization
     */
    public ProcessCpuUtilization(String identifier,
                                 double totalPercentage,
                                 ThresholdValues tVals) {
        super(identifier, totalPercentage, tVals);
        this.totalPercentage = totalPercentage;
    }

    /**
     * Construct a ProcessCpuUtilization with parameters
     *
     * @param identifier Identifier for the ProcessCpuUtilization
     * @param totalPercentage The total system cpu utilization percentage
     * @param system The cpu kernel usage
     * @param user The cpu user usage
     * @param tVals ThresholdValues for the ProcessCpuUtilization
     */
    public ProcessCpuUtilization(String identifier,
                                 double totalPercentage,
                                 long system,
                                 long user,
                                 ThresholdValues tVals) {
        super(identifier, totalPercentage, tVals);
        this.system = system;
        this.user = user;
        this.total = system + user;
        this.totalPercentage = totalPercentage;
    }

    /**
     * Get the cpu kernel usage for the process
     *
     * @return The cpu kernel usage for the process; or -1 if not available.
     */
    public double getSystem() {
        return system;
    }

    /**
     * Get the cpu user usage
     *
     * @return The cpu user usage for the process; or -1 if not available.
     */
    public double getUser() {
        return user;
    }

    /**
     * Get the cpu utilization
     *
     * @return The cpu utilization for the process; or -1 if not available.
     */
    public double getTotal() {
        return total;
    }

    /**
     * Get the cpu utilization percentage
     *
     * @return The cpu utilization percentage for the process; or -1 if
     * not available.
     */
    public double getTotalPercentage() {
        return totalPercentage;
    }
}
