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
package org.rioproject.system;

import org.rioproject.watch.ThresholdValues;

import java.io.Serializable;

/**
 * The MeasuredResource represents a quantitative value of a resource that has
 * been measured
 *
 * @author Dennis Reedy
 */
public class MeasuredResource implements Serializable {
    static final long serialVersionUID = 1L;
    /** Identifier for the measurable resource */
    private String identifier;
    /** The measured value */
    private double value;
    /** ThresholdValues */
    private ThresholdValues tValues;

    /**
     * Construct a MeasuredResource with parameters
     * 
     * @param identifier Identifier for the MeasurableResource
     * @param value The measured value
     * @param tVals ThresholdValues for the MeasurableResource
     */
    public MeasuredResource(String identifier, double value, ThresholdValues tVals) {
        if(identifier==null)
            throw new IllegalArgumentException("identifier is null");
        if(tVals==null)
            throw new IllegalArgumentException("tVals is null");
        this.identifier = identifier;
        this.value = value;
        tValues = tVals;
    }

    /**
     * Get the measured value
     * 
     * @return The measured value
     */
    public double getValue() {
        return (value);
    }

    /**
     * Get the identifier of the measurable resource
     * 
     * @return String identifier of the measurable resource
     */
    public String getIdentifier() {
        return (identifier);
    }
    
    /**
     * Get the ThresholdValues property
     * 
     * @return The ThresholdValues for the MeasuredResource at the time of the 
     * measurement
     */
    public ThresholdValues getThresholdValues() {
        return(tValues);
    }
    
    /**
     * Determine if the value has crossed a threshold declared in the
     * ThresholdValues property
     * 
     * @return True if the value falls within the range declared by the 
     * ThresholdValues, false if the value exceeds either the lower or upper 
     * range values
     */
    public boolean thresholdCrossed() {
        return value < tValues.getLowThreshold() || value > tValues.getHighThreshold();
    }

    /**
     * Evaluates whether the MeasuredResource can meet the criteria specified by
     * the ThresholdValues object.
     * 
     * @param thresholdValue The ThresholdValue to evaluate
     * @return boolean true if the MeasuredResource can meet the criteria
     * specified by the ThresholdValues object, otherwise false
     */
    public boolean evaluate(ThresholdValues thresholdValue) {
        double low = thresholdValue.getLowThreshold();
        double high = thresholdValue.getHighThreshold();
        return value >= low && value <= high;
    }

    /**
     * Indicates whether some other object is "equal to" this one.
     * 
     * @param obj the object to compare to this one
     * @return true if the objects are equal
     */
    @Override
    public boolean equals(Object obj) {
        if(obj instanceof MeasuredResource) {
            if(getIdentifier() == null && ((MeasuredResource)obj).getIdentifier() == null)
                return (true);
            else if(getIdentifier() == null || ((MeasuredResource)obj).getIdentifier() == null)
                return (false);
            else
                return (getIdentifier().equals(((MeasuredResource)obj).getIdentifier()));
        }
        return (false);
    }

    /**
     * Returns a hash code value for the object.
     * 
     * @return a hash code value for the object.
     */
    @Override
    public int hashCode() {
        int hc = 17;
        hc = 37*hc+(identifier != null? identifier.hashCode() : 0);
        return(hc);
    }


    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("identifier=").append(identifier);
        builder.append(", value=").append(value);
        builder.append(", tValues=").append(tValues);
        return builder.toString();
    }
}
