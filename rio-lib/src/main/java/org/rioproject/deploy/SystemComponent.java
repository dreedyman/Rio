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
package org.rioproject.deploy;

import org.rioproject.exec.ExecDescriptor;

import java.io.Serializable;
import java.util.*;

/**
 * The {@code SystemComponent} holds details for a required system component.
 *
 * @author Dennis Reedy
 */
public class SystemComponent implements Serializable {
    private static final long serialVersionUID = 2L;
    private final String name;
    private final String className;
    private final Map<String, Object> attributes = new HashMap<String, Object>();
    private StagedSoftware stagedSoftware;
    private ExecDescriptor execDescriptor;
    private boolean exclude = false;

    /**
     * Create a SystemComponent
     *
     * @param name A short name, typically the name of the class sans the
     * package name. Must not be null.
     */
    public SystemComponent(final String name) {
        this(name, null, null);
    }

    /**
     * Create a SystemComponent
     *
     * @param name A short name, typically the name of the class sans the
     * package name, if null, is derived from the className parameter. Must
     * not be null if the classname parameter is null
     * @param className Either the class simple name or the fully qualified classname,
     * must not be null if the name parameter is null
     */
    public SystemComponent(final String name, final String className) {
        if (name == null && className == null)
            throw new IllegalArgumentException("name and className are null");
        this.className = className;
        this.name = name;
    }

    /**
     * Create a SystemComponent
     *
     * @param name A short name, typically the name of the class sans the
     * package name, if null, is derived from the className parameter. Must
     * not be null if the classname parameter is null
     * @param className Either the class simple name or the fully qualified classname,
     * must not be null if the name parameter is null
     * @param attributes Collection of name value pairs to evaluate, optional
     */
    public SystemComponent(final String name,
                           final String className,
                           final Map<String, Object> attributes) {
        this(name, className);
        if (attributes != null)
            this.attributes.putAll(attributes);
    }

    /**
     * Get the className property
     *
     * @return the classname
     */
    public String getClassName() {
        return (className);
    }

    /**
     * Get the name property
     *
     * @return The name
     */
    public String getName() {
        return (name);
    }

    /**
     * Get the attribute Map. If there are no attributes an empty Map will
     * be returned
     *
     * @return The attribute Map
     */
    public Map<String, Object> getAttributes() {
        return (attributes);
    }

    /**
     * Associates the specified value with the specified key as a required attribute.
     *
     * @param key key with which the specified value is to be associated, ignored if {@code null}
     * @param value value to be associated with the specified key, ignored if {@code null}
     */
    public void put(final String key, final Object value) {
        if(key!=null && value!=null) {
            attributes.put(key, value);
        }
    }

    /**
     * Copies all of the mappings from the specified map
     *
     * @param attributes map to be stored.
     */
    public void putAll(final Map<String, Object> attributes) {
        if(attributes!=null) {
            this.attributes.putAll(attributes);
        }
    }

    public boolean exclude() {
        return exclude;
    }

    public void setExclude(boolean exclude) {
        this.exclude = exclude;
    }

    /**
     * Set the StagedSoftware for this system component
     *
     * @param staged Associated StagedSoftware
     */
    public void setStagedSoftware(final StagedSoftware staged) {
        stagedSoftware = staged;
    }

    /**
     * Get the StagedSoftware for this system component
     *
     * @return The associated StagedSoftware. If there is no
     * StagedSoftware, return null
     */
    public StagedSoftware getStagedSoftware() {
        return stagedSoftware;
    }

    /**
     * Get the ExecDescriptor
     *
     * @return The ExecDescriptor
     */
    public ExecDescriptor getExecDescriptor() {
        return execDescriptor;
    }

    /**
     * Set the ExecDescriptor
     *
     * @param execDescriptor The ExecDescriptor
     */
    public void setExecDescriptor(final ExecDescriptor execDescriptor) {
        this.execDescriptor = execDescriptor;
    }

    /**
     * Provide a String representation
     */
    public String toString() {
        StringBuilder buff = new StringBuilder();
        if(name!=null)
            buff.append("Name: ").append(name).append(", ");
        if(className!=null)
            buff.append("ClassName: ").append(className).append(", ");
        buff.append(attributes.toString()).append(", ");
        buff.append("Exclude: ").append(exclude);
        return (buff.toString());
    }
}
