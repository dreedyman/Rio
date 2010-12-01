/*
 * Copyright 2008 the original author or authors.
 * Copyright 2005 Sun Microsystems, Inc.
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

import org.rioproject.system.MeasuredResource;
import org.rioproject.watch.ThresholdValues;

/**
 * The <tt>MeasurableMonitor</tt> defines the semantics needed to provide
 * a feedback mechanism to measure a {@link MeasurableCapability}
 *
 * @author Dennis Reedy
 */
public interface MeasurableMonitor <M extends MeasuredResource> {
    
    /**
     * Terminate any collection mechanisms 
     */
    void terminate();

    /**
     * Set the identifier for the resource being measured
     *
     * @param id The identifier
     */
    void setID(String id);

    /**
     * Set the ThresholdValues for the resource being measured
     *
     * @param tVals The ThresholdValues
     */
    void setThresholdValues(ThresholdValues tVals);

    /**
     * Get the <tt>MeasuredResource</tt>
     *
     * @return The {@link org.rioproject.system.MeasuredResource}
     *  for the <tt>MeasurableMonitor</tt>
     */
    M getMeasuredResource();
    
}
