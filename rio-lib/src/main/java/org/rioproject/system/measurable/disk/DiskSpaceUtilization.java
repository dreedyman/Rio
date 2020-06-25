/*
 * Copyright to the original author or authors.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *         http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.rioproject.system.measurable.disk;

import org.rioproject.system.MeasuredResource;
import org.rioproject.watch.ThresholdValues;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Measured Disk statistics
 */
public class DiskSpaceUtilization extends MeasuredResource
    implements Serializable {
    private static final long serialVersionUID = 1L;
    private double used = 0;
    private double available = 0;
    private double capacity = 0;

    /**
     * Construct a DiskSpaceUtilization with parameters
     *
     * @param identifier Identifier for the DiskSpaceUtilization
     * @param total The total system disk utilization
     * @param tVals ThresholdValues for the DiskSpaceUtilization
     */
    public DiskSpaceUtilization(String identifier, double total, ThresholdValues tVals) {
        super(identifier, total, tVals);
    }

    public DiskSpaceUtilization(String identifier,
                                double used,
                                double available,
                                double capacity,
                                double utilization,
                                ThresholdValues tVals) {
        super(identifier, utilization, tVals);
        this.used = used;
        this.available = available;
        this.capacity = capacity;
    }

    public double getUsed() {
        return used;
    }

    public double getAvailable() {
        return available;
    }

    public double getCapacity() {
        return capacity;
    }

    @Override public String toString() {
        /** Gigabytes */
        double GB = Math.pow(1024, 3);

        BigDecimal bd = new BigDecimal(used/GB).setScale(2, RoundingMode.HALF_EVEN);
        double u = bd.doubleValue();
        bd = new BigDecimal(available/GB).setScale(2, RoundingMode.HALF_EVEN);
        double a = bd.doubleValue();
        bd = new BigDecimal(capacity/GB).setScale(2, RoundingMode.HALF_EVEN);
        double c = bd.doubleValue();
        return String.format("utilization=%s, used= %s GB, available=%s GB, capacity=%s GB", getValue(), u, a, c);
    }
}
