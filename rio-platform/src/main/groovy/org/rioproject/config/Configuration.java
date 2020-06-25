/*
 * Copyright to the original author or authors.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *         http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.rioproject.config;

import net.jini.config.ConfigurationException;
import net.jini.config.ConfigurationProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A wrapper around {@link net.jini.config.Configuration}, using generics to allow for easier use.
 *
 * @author Dennis Reedy
 */
@SuppressWarnings("unchecked")
public class Configuration {
    private final net.jini.config.Configuration config;
    private final Logger logger = LoggerFactory.getLogger(Configuration.class);

    public Configuration(final net.jini.config.Configuration config) {
        this.config = config;
    }

    public static Configuration getInstance(final String... args) throws ConfigurationException {
        net.jini.config.Configuration config = ConfigurationProvider.getInstance(args);
        return new Configuration(config);
    }

    public static Configuration getInstance(final ClassLoader classLoader, final String... args) throws ConfigurationException {
        net.jini.config.Configuration config = ConfigurationProvider.getInstance(args, classLoader);
        return new Configuration(config);
    }

    public net.jini.config.Configuration getWrappedConfig() {
        return config;
    }

    public <T> T getEntry(final String component, final String name, final Class<T> type) throws ConfigurationException {
        return getEntry(component, name, type, net.jini.config.Configuration.NO_DEFAULT, net.jini.config.Configuration.NO_DATA);
    }

    public <T> T getEntry(final String component, final String name, final Class<T> type, final Object defaultValue) throws ConfigurationException {
        return getEntry(component, name, type, defaultValue, net.jini.config.Configuration.NO_DATA);
    }

    public <T> T getEntry(final String component, final String name, final Class<T> type, final Object defaultValue, final Object data) throws ConfigurationException {
        return (T) config.getEntry(component, name, type, defaultValue, data);
    }

    public <T> T getNonNullEntry(final String component, final String name, Class<T> type) throws ConfigurationException {
        return getNonNullEntry(component, name, type, net.jini.config.Configuration.NO_DEFAULT, net.jini.config.Configuration.NO_DATA);
    }

    public <T> T getNonNullEntry(final String component,
                                 final String name,
                                 final Class<T> type,
                                 final Object defaultValue) throws ConfigurationException {
        return getNonNullEntry(component, name, type, defaultValue, net.jini.config.Configuration.NO_DATA);
    }

    public <T> T getNonNullEntry(final String component,
                                 final String name,
                                 final Class<T> type,
                                 final Object defaultValue,
                                 final Object data) throws ConfigurationException {
        if (defaultValue == null)
            throw new NullPointerException("defaultValue cannot be null");

        final Object result = config.getEntry(component, name, type, defaultValue, data);

        if (result == null) {
            throw new ConfigurationException(String.format("entry for component %s, name %s cannot be null", component, name));
        }

        return (T) result;
    }

    public Integer getIntEntry(final String component,
                               final String name,
                               final int defaultValue,
                               final int min,
                               final int max) throws ConfigurationException {
        if (min > max) {
            throw new IllegalArgumentException("min must be less than or equal to max");
        }
        if (!inRange(defaultValue, min, max)) {
            throw new IllegalArgumentException("defaultValue (" + defaultValue + ") must be between " + min + " and " + max);
        }
        int rslt = getEntry(component, name, Integer.TYPE, defaultValue);
        if (!inRange(rslt, min, max)) {
            if (logger.isDebugEnabled()) {
                logger.debug("component {}, name {}: entry is out of range, value: {}, valid range: {}:{}",
                             component, name, rslt, min, max);
            }
            throw new ConfigurationException("entry for component " + component + ", name " + name + " must be between " + min + " and " + max + ", has a value of " + rslt);
        }
        return rslt;
    }

    private boolean inRange(float value, float min, float max) {
        return (min <= value) && (value <= max);
    }

}
