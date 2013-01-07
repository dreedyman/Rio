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
package org.rioproject.core.jsb;

import org.rioproject.system.capability.PlatformCapability;
import org.rioproject.system.measurable.MeasurableCapability;
import org.rioproject.system.ComputeResource;

/**
 * The ComputeResourceManager provides the capabilities to access, set and obtain information on
 * the {@link ComputeResource}.
 *
 * @author Dennis Reedy
 */
public interface ComputeResourceManager {
    /**
     * Get the ComputeResource object
     * 
     * @return The {@code ComputeResource}, never {@code null}.
     */
    ComputeResource getComputeResource();

    /**
     * Get a PlatformCapability instance from a name.
     * 
     * @param description The name of the PlatformCapability
     *
     * @return The first PlatformCapability that matches the name. If no PlatformCapability
     * matches the name, return {@code null}.
     *
     * @throws IllegalArgumentException if the {@code description} is {@code null}.
     */
    PlatformCapability getPlatformCapability(String description);

    /**
     * Get the PlatformCapability instances that match declared system requirements.
     * 
     * @return  An array of PlatformCapability instances that match declared operational
     * requirements. If there are no declared PlatformCapability requirements, then return
     * a zero-length array.
     */
    PlatformCapability[] getMatchedPlatformCapabilities();

    /**
     * Get the MeasurableCapability instances that match declared SLAs.
     * 
     * @return An array of MeasurableCapability instances that match declared operational requirements.
     * If there are no declared SLAs which match MeasurableCapability identifiers, then return a
     * zero-length array
     */
    MeasurableCapability[] getMatchedMeasurableCapabilities();
}
