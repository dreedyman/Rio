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
import org.rioproject.system.capability.PlatformCapability;

import java.io.Serializable;
import java.util.*;

/**
 * The {@code SystemComponent} holds details for a required system component.
 *
 * @author Dennis Reedy
 */
public class SystemComponent implements Serializable {
    @SuppressWarnings("unused")
    static final long serialVersionUID = 1L;
    private final String name;
    private final String className;
    private final Map<String, Object> attributes = new HashMap<String, Object>();
    private final List<StagedSoftware> stagedSoftware = new ArrayList<StagedSoftware>();
    private ExecDescriptor execDescriptor;

    /**
     * Create a SystemComponent
     */
    public SystemComponent(final PlatformCapability pCap) {
        name = pCap.getName();
        className = pCap.getClass().getName();
        attributes.putAll(pCap.getCapabilities());
    }

    /**
     * Create a SystemComponent
     *
     * @param name A short name, typically the name of the class sans the
     * package name. Must not be null
     * @param attrs Collection of name value pairs to evaluate, optional
     */
    public SystemComponent(final String name, final Map<String, Object> attrs) {
        this(name, null, attrs);
    }

    /**
     * Create a SystemComponent
     *
     * @param name A short name, typically the name of the class sans the
     * package name, if null, is derived from the className parameter. Must
     * not be null if the classname parameter is null
     * @param className Either the class simple name or the fully qualified classname,
     * must not be null if the name parameter is null
     * @param attrs Collection of name value pairs to evaluate, optional
     */
    public SystemComponent(final String name,
                           final String className,
                           final Map<String, Object> attrs) {
        if (name == null && className == null)
            throw new IllegalArgumentException("name and className are null");
        this.className = className;
        this.name = name;
        if (attrs != null)
            attributes.putAll(attrs);
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
     * Set the StagedSoftware for this system component
     *
     * @param staged Associated StagedSoftware
     */
    public void setStagedSoftware(final StagedSoftware... staged) {
        stagedSoftware.addAll(Arrays.asList(staged));
    }

    /**
     * Get the StagedSoftware for this system component
     *
     * @return The associated StagedSoftware. If there is no
     * StagedSoftware, return a zero-length array
     */
    public StagedSoftware[] getStagedSoftware() {
        return (stagedSoftware.toArray(new StagedSoftware[stagedSoftware.size()]));
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
        buff.append("Name=").append(name == null ? "<null>" : name).append(", ");
        buff.append("ClassName=").append(className == null ? "<null>" : className).append(", ");
        buff.append("Attributes=").append(attributes.toString());
        return (buff.toString());
    }
}
