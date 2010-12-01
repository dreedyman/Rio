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
package org.rioproject.system.measurable.memory.pool;

import org.rioproject.system.MeasuredResource;
import org.rioproject.watch.ThresholdValues;

/**
 * Measured JMX memory pool utilization
 */
public class MemoryPoolUtilization extends MeasuredResource {
    private long committed;
    private long init;
    private long max;
    private long used;
    
    /**
     * Construct a MeasuredResource with parameters
     *
     * @param identifier Identifier for the MeasurableResource
     * @param value The measured value
     * @param thresholdValues ThresholdValues for the MeasurableResource
     */
    public MemoryPoolUtilization(String identifier,
                                 double value,
                                 ThresholdValues thresholdValues) {
        super(identifier, value, thresholdValues);
    }

    public MemoryPoolUtilization(String identifier,
                                 double value,                                 
                                 long committed,
                                 long init,
                                 long max,
                                 long used,
                                 ThresholdValues thresholdValues) {
        super(identifier, value, thresholdValues);
        this.committed = committed;
        this.init = init;
        this.max = max;
        this.used = used;
    }

    public long getCommitted() {
        return committed;
    }

    public long getInit() {
        return init;
    }

    public long getMax() {
        return max;
    }

    public long getUsed() {
        return used;
    }       

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("MemoryPoolUtilization");
        sb.append(" {committed=").append(committed);
        sb.append(", init=").append(init);
        sb.append(", max=").append(max);
        sb.append(", used=").append(used);
        sb.append('}');
        return sb.toString();
    }
}
