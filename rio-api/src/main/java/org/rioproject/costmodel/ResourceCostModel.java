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

import java.io.Serializable;

/**
 * The ResourceCostModel provides a mechanism to define the cost per unit for a
 * resource
 *
 * @author Dennis Reedy
 */
public interface ResourceCostModel extends Serializable {
    /**
     * Get the cost per unit
     * 
     * @param duration The amount of time in milliseconds that is to be used
     * to compute the cost per unit
     * @return The cost per unit
     */
    double getCostPerUnit(long duration);

    /**
     * Get the description of the ResourceCostModel
     * 
     * @return String The description of the ResourceCostModel
     */
    String getDescription();

    /**
     * Add a TimeBoundary to the ResourceCostModel
     * 
     * @param timeBoundary A TimeBoundary indicating the attributes to be
     * applied to the computation of cost per unit for resource use over a
     * duration
     */
    void addTimeBoundary(TimeBoundary timeBoundary);
    
    /**
     * Indicates a time boundary and the multiplier to apply to the cost per
     * unit
     */
    public static class TimeBoundary implements Comparable, Serializable {
        static final long serialVersionUID = 1L;
        /** Indicates the boundary has been provided in milliseconds */
        public static final int MILLIS = 1;
        /** Indicates the boundary has been provided in seconds */
        public static final int SECONDS = 1000;
        /** Indicates the boundary has been provided in minutes */
        public static final int MINUTES = SECONDS * 60;
        /** Indicates the boundary has been provided in hours */
        public static final int HOURS = MINUTES * 60;
        /** Indicates the boundary has been provided in days */
        public static final int DAYS = HOURS * 24;
        /** The boundary */
        private long boundary;
        /** The boundary type */
        private int type;
        /** The multiplier */
        private double multiplier;

        /**
         * Create a TimeBoundary. A TimeBoundary defines a boundary and a
         * multiplier to be used with the cost per unit. The boundary defines a
         * start point for any duration which is greater then or equal to the
         * specified boundary. For example: if the boundary is 300000 (5
         * minutes) any duration that is 5 minutes or greater will have it's
         * cost per unit muliplied by the multiplier.
         * 
         * @param boundary The amount of time defining a time boundary in
         * milliseconds
         * @param multiplier The value which will be used as a multiplier for
         * the costPerUnit if the duration is greater then or equal to the
         * boundary value
         */
        public TimeBoundary(long boundary, double multiplier) {
            this(boundary, multiplier, MILLIS);
        }

        /**
         * Create a TimeBoundary. A TimeBoundary defines a boundary and a
         * multiplier to be used with the cost per unit. The boundary defines a
         * start point for any duration which is greater then or equal to the
         * specified boundary. For example: if the boundary is 300000 (5
         * minutes) any duration that is 5 minutes or greater will have it's
         * cost per unit muliplied by the multiplier.
         * 
         * @param boundary The amount of time defining a time boundary
         * @param multiplier The value which will be used as a multiplier for
         * the costPerUnit if the duration is greater then or equal to the
         * boundary value
         * @param type Determines the type (MILLIS, SECONDS, ...) of the
         * boundary value
         */
        public TimeBoundary(long boundary, double multiplier, int type) {
            if(type != MILLIS
               && type != SECONDS
               && type != MINUTES
               && type != HOURS
               && type != DAYS)
                throw new IllegalArgumentException("bad type [" + type + "]");
            this.boundary = boundary * type;
            this.multiplier = multiplier;
            this.type = type;
        }

        /**
         * @return The time boundary.
         */
        public long getBoundary() {
            return (boundary);
        }

        /**
         * @return The multiplier
         */
        public double getMultiplier() {
            return (multiplier);
        }
        
        /**
         * @return The type
         */
        public int getType() {
            return(type);
        }

        /**
         * Compares this TimeBoundary object with another TimeBoundary object
         * for order using the boundary attribute
         * 
         * @param o Object to compare to
         */
        public int compareTo(Object o) {
            /* Will throw a ClassCastException if the obj is not the right type */
            TimeBoundary that = (TimeBoundary)o;
            if(this.getBoundary() == that.getBoundary())
                return (0);
            if(this.getBoundary() < that.getBoundary())
                return (-1);
            return (1);
        }

        /**
         * A TimeBoundary is equal to another TimeBoundary if their boundary
         * attributes are equal
         */
        public boolean equals(Object o) {
            if(this == o)
                return (true);
            if(!(o instanceof TimeBoundary))
                return (false);
            TimeBoundary that = (TimeBoundary)o;
            return ((this.getBoundary() == that.getBoundary()) && 
                (this.getMultiplier() == that.getMultiplier()));
        }

        /**
         * The hashCode is the int conversion of the boundary attribute
         */
        public int hashCode() {
            return (new Long(getBoundary()).intValue());
        }
    }
}
