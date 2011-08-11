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
package org.rioproject.system.measurable;

import org.rioproject.watch.PeriodicWatchMBean;

/**
 * Provides a standard MBean to use when administering a MeasurableCapability
 * using JMX
 *
 * @author Ming Fang
 * @author Dennis Reedy
 */
public interface MeasurableCapabilityMBean extends PeriodicWatchMBean {
    /**
     * Set the sampleSize for the MeasurableCapability
     *
     * @param sampleSize The number of samples the MeasurableCapability will
     * accumulate in the period defined by the period in order to produce a
     * result.
     */
    void setSampleSize(int sampleSize);

    /**
     * Get the sampleSize property
     *
     * @return The number of samples the MeasurableCapability will
     * accumulate in the period defined by the period in order to produce a
     * result.
     */
    int getSampleSize();

    /**
     * Provide a method to return the utilization associated with this
     * MeasurableCapability
     */
    double getUtilization();
}
