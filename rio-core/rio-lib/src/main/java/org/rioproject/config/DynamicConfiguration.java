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
package org.rioproject.config;

import net.jini.config.AbstractConfiguration;
import net.jini.config.ConfigurationException;
import net.jini.config.NoSuchEntryException;

import java.util.HashMap;
import java.util.Map;


/**
 * The class represents a configuration object that can be populated with
 * entries programmatically at runtime.
 */
public class DynamicConfiguration extends AbstractConfiguration {
    /**
     * Maps entry names to entry objects.
     */
    private final Map<String, Entry> entries = new HashMap<String, Entry>();


    /**
     * Sets an <code>int</code> entry.
     *
     * @param component the component name
     * @param name      the entry (short) name
     * @param value     the entry value
     */
    public void setEntry(String component, String name, int value) {
        setEntry(component, name, int.class, value);
    }

    /**
     * Sets an arbitrary entry.
     *
     * @param component the component name
     * @param name      the entry (short) name
     * @param type      the entry type
     * @param value     the entry value
     */
    public void setEntry(String component, String name, Class type, Object value) {
        if (component == null || name == null || type == null) {
            throw new IllegalArgumentException("Component, name, and type cannot be null");
        }
        Entry entry = new Entry();
        entry.type = type;
        entry.value = value;
        entries.put(component + '.' + name, entry);
    }


    @SuppressWarnings("unchecked")
    protected Object getEntryInternal(String component, String name, Class type, Object data)
        throws ConfigurationException {

        if (component == null || name == null || type == null) {
            throw new IllegalArgumentException("Component, name, and type cannot be null");
        }
        Entry entry = entries.get(component + '.' + name);
        if (entry == null) {
            throw new NoSuchEntryException("Entry not found for component [" + component + "], name [" + name + "]");
        }
        if (type == entry.type || type.isAssignableFrom(entry.type)) {
            if (entry.type.isPrimitive()) {
                return new Primitive(entry.value);
            } else {
                return entry.value;
            }
        }
        throw new ConfigurationException("Entry of wrong type for "
                                         + "component [" + component + "], name [" + name + "]:"
                                         + "expected [" + type + ", found [" + entry.type + "]");
    }


    /**
     * The class represents an entry.
     */
    private static class Entry {
        public Class type;
        public Object value;
    }
}
