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
package org.rioproject.costmodel;

import java.io.Serializable;
import java.util.Date;

/**
 * The ResourceCost provides context on the cost of a system resource. Items 
 * related to a resource's cost include an actual (monetary) cost
 * value, the number of units, the name of the resource which provided the cost,
 * and a description of the {@link ResourceCostModel} which
 * calculated the cost
 *
 * @author Dennis Reedy
 */
public class ResourceCost implements Serializable {
    static final long serialVersionUID = 1L;
    private String resourceName;
    private Date date;
    private String costModelDescription;
    private double cost;
    private double units;

    /**
     * Create a ResourceCost
     * 
     * @param resourceName The name of the resource
     * @param cost The computed cost
     * @param units The amount of units being charged
     * @param costModelDescription A description of the 
     * {@link ResourceCostModel} used to compute the cost
     * @param date The date the cost had been computed
     */
    public ResourceCost(String resourceName, 
                        double cost, 
                        double units,
                        String costModelDescription, 
                        Date date) {
        if(resourceName == null)
            throw new IllegalArgumentException("resourceName is null");
        if(costModelDescription == null)
            throw new IllegalArgumentException("costModelDescription is null");
        if(date == null)
            throw new IllegalArgumentException("date is null");
        this.resourceName = resourceName;
        this.cost = cost;
        this.units = units;
        this.costModelDescription = costModelDescription;
        this.date = date;
    }

    /**
     * @return The computed cost
     */
    public double getCost() {
        return (cost);
    }

    /**
     * Get the number of units used. If the resource is a system resource
     * (a depletion oriented resource), the number of units used corresponds to
     * the utilization of the resource at the time the <tt>ResourceCost</tt>
     * was created.
     *
     * @return The number of units used
     */
    public double getUnits() {
        return (units);
    }

    /**
     * @return The name of the resource which had the cost computed for it
     */
    public String getResourceName() {
        return (resourceName);
    }

    /**
     * @return The Date the cost was computed
     */
    public Date getDate() {
        return (date);
    }

    /**
     * @return The description of the {@link ResourceCostModel}
     * that computed the cost
     */
    public String getCostModelDescription() {
        return (costModelDescription);
    }

    public String toString() {
        return "ResourceCost{" +
               "resourceName='" + resourceName +
               ", units=" + units +
               ", cost=" + cost +
               ", date=" + date +
               ", costModelDescription='" + costModelDescription +
               '}';
    }    
}
