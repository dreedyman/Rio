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
package org.rioproject.sla;

import org.rioproject.watch.WatchDescriptor;
import org.rioproject.watch.ThresholdValues;

import java.io.Serializable;

/**
 * A SLA provides a mechanism to a specify a preferred range of acceptable lower
 * and upper boundaries that fit a specific criteria
 *
 * @author Dennis Reedy
 */
public class SLA extends ThresholdValues implements Serializable {
    @SuppressWarnings("unused")
    static final long serialVersionUID = 1L;
    /**
     * SLA identifier
     */
    private String identifier;
    /**
     * The WatchDescriptor property provides context to the watch instances
     * the SLA may be associated to, and optionally be dynamically created
     */
    private WatchDescriptor[] watchDescriptors;
    /** Dampening value for upper thresholds being crossed */
    private long upperThresholdDampeningTime;
    /** Dampening value for lower thresholds being crossed */
    private long lowerThresholdDampeningTime;
    /**
     * Undefined maximum number of services to increment to
     */
    public static final int UNDEFINED = -1;
    /**
     * The maximum number of services to increment. If the value is -1, then no
     * limit has been set
     */
    private int maxServices = UNDEFINED;
    /** The class name of the SLAPolicyHandler to create */
    private String slaPolicyHandler;

    /**
     * Construct a SLA
     */
    public SLA() {
        super();
    }

    /**
     * Construct a SLA
     *
     * @param identifier The identifier of the ThresholdWatch class the SLA
     * will be paired with
     * @param range Double values indicating the range of acceptable
     * lower and upper boundaries. There must be two values here, the first
     * the low boundary, the second te high
     */
    public SLA(final String identifier, final double... range) {
        super(range);
        if(identifier == null)
            throw new IllegalArgumentException("identifier is null");
        this.identifier = identifier;
    }

    /**
     * Get the identifier of the ThresholdWatch this SLA is for
     *
     * @return The identifier of the ThresholdWatch this SLA is for. If
     * there is no identifier, this method returns an empty String
     */
    public String getIdentifier() {
        return (identifier);
    }

    /**
     * Get the watchDescriptors property
     *
     * @return The WatchDescriptor instances configured for this utility. A new
     * array is allocated each time. If there are no WatchDescriptor instances,
     * a zero-length array is returned
     */
    public WatchDescriptor[] getWatchDescriptors() {
        if(watchDescriptors==null)
            return(new WatchDescriptor[0]);
        WatchDescriptor[] wds = new WatchDescriptor[watchDescriptors.length];
        System.arraycopy(watchDescriptors, 0, wds, 0,
                         watchDescriptors.length);
        return(wds);
    }

    /**
     * Set the watchDescriptors property
     *
     * @param watchDescs The WatchDescriptor instances to set.
     */
    public void setWatchDescriptors(final WatchDescriptor... watchDescs) {
        if(watchDescs==null)
            return;
        watchDescriptors = new WatchDescriptor[watchDescs.length];
        System.arraycopy(watchDescs, 0, watchDescriptors, 0, watchDescs.length);
    }

    /**
     * Get the dampening value for upper thresholds being crossed
     *
     * @return The dampening value for upper thresholds being crossed. The amount
     * of time to wait until an action is performed on the upper threshold
     * breach.
     */
    public long getUpperThresholdDampeningTime() {
        return upperThresholdDampeningTime;
    }

    /**
     * Set the dampening value for upper thresholds being crossed
     *
     * @param upperThresholdDampeningTime The dampening value for upper
     * thresholds being crossed
     */
    public void setUpperThresholdDampeningTime(final long upperThresholdDampeningTime) {
        if(upperThresholdDampeningTime<0)
            throw new IllegalArgumentException("upperThresholdDampeningTime must be >= 0");
        this.upperThresholdDampeningTime = upperThresholdDampeningTime;
    }

    /**
     * Get the dampening value for lower thresholds being crossed. The amount
     * of time to wait until an action is performed on the lower threshold
     * breach.
     *
     * @return The dampening value for lower thresholds being crossed
     */
    public long getLowerThresholdDampeningTime() {
        return lowerThresholdDampeningTime;
    }

    /**
     * Set the dampening value for lower thresholds being crossed
     *
     * @param lowerThresholdDampeningTime The dampening value for lower
     * thresholds being crossed
     */
    public void setLowerThresholdDampeningTime(final long lowerThresholdDampeningTime) {
        if(lowerThresholdDampeningTime<0)
            throw new IllegalArgumentException("lowerThresholdDampeningTime must be >= 0");
        this.lowerThresholdDampeningTime = lowerThresholdDampeningTime;
    }

    /**
     * Get the maximum number of services
     *
     * @return The maximum number of services for the SLA to be applied for.
     * If the value is -1, then no limit has been set
     */
    public int getMaxServices() {
        return maxServices;
    }

    /**
     * Set the maximum services property
     *
     * @param maxServices The maximum services
     */
    public void setMaxServices(final int maxServices) {
        if(maxServices < UNDEFINED)
            throw new IllegalArgumentException("maxServices must be > "+UNDEFINED);
        this.maxServices = maxServices;
    }

    /**
     * Get the SLAPolicyHandler class name to create
     *
     * @return The fully qualified class name of the SLAPolicyHandler to create
     */
    public String getSlaPolicyHandler() {
        return slaPolicyHandler;
    }

    /**
     * Set the SLAPolicyHandler class name to create
     *
     * @param slaPolicyHandler The fully qualified class name of the
     * SLAPolicyHandler to create
     */
    public void setSlaPolicyHandler(final String slaPolicyHandler) {
        this.slaPolicyHandler = slaPolicyHandler;
    }    

    public String toString() {
        StringBuilder toString = new StringBuilder();
        toString.append("SLA [");
        toString.append("ID: ").append(identifier).append(", ");
        toString.append("Low: ").append(getLowThreshold()).append(", ");
        toString.append("High: ").append(getHighThreshold()).append(", ");
        toString.append("upperThresholdDampeningTime: ").append(upperThresholdDampeningTime).append(", ");
        toString.append("lowerThresholdDampeningTime: ").append(lowerThresholdDampeningTime).append(", ");
        toString.append("maxServices: ").append(maxServices).append(", ");
        toString.append("slaPolicyHandler: ").append(slaPolicyHandler).append("]");
        return toString.toString();
    }
}
