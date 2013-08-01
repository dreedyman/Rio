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
package org.rioproject.system;

import net.jini.config.Configuration;
import org.rioproject.system.capability.PlatformCapability;
import org.rioproject.system.measurable.MeasurableCapability;

import java.util.Map;

/**
 * The SystemCapabilitiesLoader defines the semantics to load 
 * {@link org.rioproject.system.capability.PlatformCapability} and
 * {@link org.rioproject.system.measurable.MeasurableCapability} instances
 *
 * @author Dennis Reedy
 */
public interface SystemCapabilitiesLoader {       
    
    /**
     * Get the MeasurableCapability objects based on a passed in Configuration
     * 
     * @param config A Configuration to use to assist in creating 
     * MeasurableCapability objects
     * 
     * @return Return an array of <code>MeasurableCapability</code> objects. This 
     * method will create a new array of <code>MeasurableCapability</code> objects 
     * each time it is invoked. 
     */
    MeasurableCapability[] getMeasurableCapabilities(Configuration config);        

    /**
     * Get the PlatformCapability objects
     *
     * @param config  A Configuration to use to assist in creating
     * MeasurableCapability objects
     * 
     * @return An array of <code>PlatformCapability</code> objects. This 
     * method will create a new array of <code>PlatformCapability</code> objects 
     * each time it is invoked. If there are no <code>PlatformCapability</code> 
     * objects contained within the <code>platforms</code> Collection, a 
     * zero-length array will be returned.
     */
    PlatformCapability[] getPlatformCapabilities(Configuration config);

    /**
     * Get the PlatformCapability name table
     *
     * @return A Map of PlatformCapability names to PlatformCapability classnames
     */
    Map<String, String> getPlatformCapabilityNameTable();

    /**
     * Get the directory to load platform configuration files from
     *
     * @param config A Configuration to use to retrieve the location. The
     * following property can be used to set the location:
     * <p>
     * <pre>
     * org.rioproject.system.platformDirs
     * </pre>
     * <p>If this property is not set, the default will be:
     * <pre>RIO_HOME/config/platform</pre>
     *
     * @return The directorty (path) to load plaform configuration files from
     */
    String getPlatformConfigurationDirectory(Configuration config);
}
