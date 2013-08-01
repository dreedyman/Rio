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
package org.rioproject.config;

import net.jini.config.Configuration;
import net.jini.config.ConfigurationException;
import net.jini.config.ConfigurationProvider;
import net.jini.config.NoSuchEntryException;

/**
 * The AggregateConfig provides an approach to aggregate configuration files,
 * allowing entries to be resolved in a "common" {@link Configuration}.
 *
 * @author Dennis Reedy
 */
public class AggregateConfig implements Configuration {
    private Configuration common;
    private Configuration outer;
    
    /**
     * Create an AggregateConfig
     * 
     * @param commonConfig - The "common" Configuration
     * @param configArgs Configuration arguments for an 'outer' configuration.
     * This configuration will be looked at first for configuration entries.
     * If entries are not found, the common configuration is consulted.
     * @param loader - The class loader to use for loading entries from the
     * configuration file(s). If null, the classloader that loaded the
     * <tt>AggregateConfig</tt> class is used
     * 
     * @throws ConfigurationException If there are errors creating the
     * Configuration
     * @throws IllegalArgumentException if the config parameter is null
     */
    public AggregateConfig(final Configuration commonConfig,
                           final String[] configArgs,
                           final ClassLoader loader) throws ConfigurationException {
        if(commonConfig==null)
            throw new IllegalArgumentException("common config cannot be null");
        outer  = ConfigurationProvider.getInstance(configArgs, loader);
        common = commonConfig;
    }

    /**
     * Get an entry from the aggregate configuration files. If not found in the
     * 'outer' config, look in the common configuration
     *
     * @param component the component being configured
     * @param name the name of the entry for the component
     * @param type the type of the object to be returned
     *
     * @return an object created using the information in the entry matching
     * component and name, and using the value of data (unless it is NO_DATA),
     * or defaultValue if no matching entry is found and defaultValue is not
     * NO_DEFAULT
     *
     * @throws ConfigurationException
     */
    public Object getEntry(String component, String name, Class type)
        throws ConfigurationException {
        try {
            return outer.getEntry(component, name, type);
        } catch (NoSuchEntryException e) {
            return common.getEntry(component, name, type);
        }
    }

    /**
     * Get an entry from the aggregate configuration files. If not found in the
     * 'outer' config, look in the common configuration
     *
     * @param component the component being configured
     * @param name the name of the entry for the component
     * @param type the type of the object to be returned
     * @param defaultValue the object to return if no matching entry is
     * found, or NO_DEFAULT to specify no default
     *
     * @return an object created using the information in the entry matching
     * component and name, and using the value of data (unless it is NO_DATA),
     * or defaultValue if no matching entry is found and defaultValue is not
     * NO_DEFAULT
     *
     * @throws ConfigurationException
     */
    public Object getEntry(String component,
                           String name,
                           Class type,
                           Object defaultValue) throws ConfigurationException {
        try {
            return outer.getEntry(component, name, type);
        } catch (NoSuchEntryException e) {
            return common.getEntry(component, name, type, defaultValue);
        }
    }

    /**
     * Get an entry from the aggregate configuration files. If not found in the
     * 'outer' config, look in the common configuration
     *
     * @param component the component being configured
     * @param name the name of the entry for the component
     * @param type the type of the object to be returned
     * @param defaultValue the object to return if no matching entry is
     * found, or NO_DEFAULT to specify no default
     * @param data an object to use when computing the value of the entry,
     * or NO_DATA to specify no data
     *
     * @return an object created using the information in the entry matching
     * component and name, and using the value of data (unless it is NO_DATA),
     * or defaultValue if no matching entry is found and defaultValue is not
     * NO_DEFAULT
     *
     * @throws ConfigurationException
     */
    public Object getEntry(String component,
                           String name,
                           Class type,
                           Object defaultValue,
                           Object data) throws ConfigurationException {
        Object entry = outer.getEntry(component, name, type, null, data);
        if (entry != null) {
            return entry;
        }
        return common.getEntry(component, name, type, defaultValue, data);
    }
    
    public Configuration getOuterConfiguration() {
        return outer;
    }
}
