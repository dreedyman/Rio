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
package org.rioproject.costmodel;

import java.util.Collections;
import java.util.Comparator;
import java.util.Set;
import java.util.TreeSet;

/**
 * The GenericCostModel provides a straightforward implementation of a 
 * {@link ResourceCostModel}
 *
 * @author Dennis Reedy
 */
public class GenericCostModel implements ResourceCostModel {
    @SuppressWarnings("unused")
    static final long serialVersionUID = 1L;
    /** Default description */
    private static final String DEFAULT_DESCRIPTION = "Generic Cost Model";     
    /** The cost per unit */
    private double costPerUnit;
    /** A description */
    private String description = DEFAULT_DESCRIPTION;
    /** List of TimeBoundary objects */
    private final Set<TimeBoundary> timeBoundaries =
        new TreeSet<TimeBoundary>(new Comparator<TimeBoundary>() {
            public int compare(TimeBoundary timeBoundary,
                               TimeBoundary timeBoundary1) {
                return (timeBoundary.compareTo(timeBoundary1));
            }
        });

    /**
     * Create a GenericCostModel
     * 
     * @param value The cost per unit, must be non-negative and not NaN
     */
    public GenericCostModel(double value) {
        this(value, null, DEFAULT_DESCRIPTION);
    }
    
    /**
     * Create a GenericCostModel
     * 
     * @param value The cost per unit, must be non-negative and not NaN
     * @param timeBoundaries An Array of TimeBoundary classes
     */
    public GenericCostModel(double value, TimeBoundary[] timeBoundaries) {
        this(value, timeBoundaries, DEFAULT_DESCRIPTION);
    }
    
    /**
     * Create a GenericCostModel
     * 
     * @param value The cost per unit, must be non-negative and not NaN
     * @param timeBoundaries An Array of TimeBoundary classes
     * @param description A Description for the GenericCostModel
     */
    public GenericCostModel(double value, 
                            TimeBoundary[] timeBoundaries, 
                            String description) {
        if(value == Double.NaN)
            throw new IllegalArgumentException("value cannot be NaN");
        if(value < 0)
            throw new IllegalArgumentException("value must be non-negative");
        costPerUnit = value;
        if(timeBoundaries!=null) {
            Collections.addAll(this.timeBoundaries, timeBoundaries);
        }
        if(description!=null)
            this.description = description;
    }

    /**
     * @see ResourceCostModel#getCostPerUnit
     */
    public double getCostPerUnit(long duration) {
        TimeBoundary[] tBoundaries = getTimeBoundaries();
        for(int i = (tBoundaries.length - 1); i >= 0; i--) {
            if(duration >= tBoundaries[i].getBoundary()) {
                return (costPerUnit * tBoundaries[i].getMultiplier());
            }
        }
        return (costPerUnit);
    }

    /**
     * @see ResourceCostModel#addTimeBoundary
     */
    public void addTimeBoundary(TimeBoundary timeBoundary) {
        synchronized(timeBoundaries) {
            timeBoundaries.add(timeBoundary);
        }
    }

    /**
     * @see ResourceCostModel#getDescription
     */
    public String getDescription() {
        return (description);
    }

    /*
     * Get all TimeBoundary instances from the Collection, sorted by boundary
     */
    private TimeBoundary[] getTimeBoundaries() {
        TimeBoundary[] boundaries;
        synchronized(timeBoundaries) {
            boundaries = timeBoundaries.toArray(
                                           new TimeBoundary[timeBoundaries.size()]);
        }
        return (boundaries);
    }
}
