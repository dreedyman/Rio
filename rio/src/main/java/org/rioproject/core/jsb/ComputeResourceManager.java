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

import java.net.URL;
import java.util.Map;

/**
 * The ComputeResourceManager provides the capabilities to access, set and
 * obtain information on ComputeResource mechanisms
 *
 * @author Dennis Reedy
 */
public interface ComputeResourceManager {
    /**
     * Get the ComputeResource object
     * 
     * @return The Object representing the platform
     * and measurable capabilities of the compute resource upon which the
     * service has been instantiated
     */
    ComputeResource getComputeResource();

    /**
     * Get a PlatformCapability instance from a name
     * 
     * @param description The name of the PlatformCapability
     * @return The first PlatformCapability that matches
     * the name. If no PlatformCapability matches the name, return
     * null
     */
    PlatformCapability getPlatformCapability(String description);

    /**
     * Get the PlatformCapability instances that match declared system
     * requirements
     * 
     * @return  An array of PlatformCapability instances
     * that match declared operational requirements. If there are no declared
     * PlatformCapability requirements, then return a zero-length array
     */
    PlatformCapability[] getMatchedPlatformCapabilities();

    /**
     * Add a PlatformCapability. A PlatformCapability will be added to the
     * ComputeResource object for the duration of the ServiceBean's lifetime
     * 
     * @param className The class name of the PlatformCapability. This name
     * must be suitable for Class.forName use
     * @param location An URL indicating where to load the PlatformCapability
     * from. If this parmater is null, the PlatformCapability will be loaded
     * from platform-capabilities.jar
     * @param mapping The Map of name,value pairs the PlatformCapability will
     * set
     *
     * @return The added PlatformCapability, or null if the PlatformCapability
     * could not be created or added
     */
    PlatformCapability addPlatformCapability(String className,
                                             URL location,
                                             Map<String, Object> mapping);

    /**
     * Remove a PlatformCapability that was added by the
     * ServiceBean. Only PlatformCapability instances that were added by the
     * ServiceBean may be removed using this method
     * 
     * @param pCap The PlatformCapability to remove
     * @return True if removed
     */
    boolean removePlatformCapability(PlatformCapability pCap);

    /**
     * Get the MeasurableCapability instances that match declared SLAs. 
     * 
     * @return An array of MeasurableCapability
     * instances that match declared operational requirements. If there are no
     * declared SLAs which match MeasurableCapability identifiers, then return a 
     * zero-length array
     */
    MeasurableCapability[] getMatchedMeasurableCapabilities();
}
