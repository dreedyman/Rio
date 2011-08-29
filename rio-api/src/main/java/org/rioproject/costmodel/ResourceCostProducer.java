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

/**
 * Defines the signature for an entity to provude a {@link ResourceCost}
 *
 * @author Dennis Reedy
 */
public interface ResourceCostProducer {
    /**
     * Calculate the cost based on the units provided. The cost will be computed
     * using the ResourceCostModel
     * 
     * @param units The units to be costed
     * @param duration The amount of time in milliseconds the resource has
     * been used for
     * @return A ResourceCost object
     */
    ResourceCost calculateResourceCost(double units, long duration);
}
