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
package org.rioproject.core.provision;

import org.rioproject.watch.ThresholdValues;
import org.rioproject.exec.ExecDescriptor;

import java.io.Serializable;
import java.util.*;
import java.net.URL;

/**
 * The SystemRequirements class provides context on the attributes required to
 * meet system requirements for a service.
 *
 * @author Dennis Reedy
 */
public class SystemRequirements implements Serializable {
    static final long serialVersionUID = 1L;
    /**
     * Key for the maximum compute resource system utilization
     */
    public static final String SYSTEM = "System";
    /**
     * Array of system  components
     */
    private final List<SystemComponent> systemComponents =
        new ArrayList<SystemComponent>();
    /**
     * Array of system requirement SLAs
     */
    private final Map<String, ThresholdValues> systemThresholds =
        new HashMap<String, ThresholdValues>();

    /**
     * Add a system required ThresholdValue
     *
     * @param identifier String identifier for the system property that has a
     * ThresholdValue
     * @param tVals ThresholdValues specifying operational criteria which must
     * be met in order for the service to be provisioned to a compute resource
     */
    public void addSystemThreshold(String identifier, ThresholdValues tVals) {
        if (identifier == null)
            throw new IllegalArgumentException("identifier is null");
        if (tVals == null)
            throw new IllegalArgumentException("ThresholdValues is null");
        synchronized (systemThresholds) {
            systemThresholds.put(identifier, tVals);
        }
    }

    /**
     * Get the system identifier keys that have ThresholdValues associated to
     * them
     *
     * @return Array of ThresholdValues specifying operational criteria that
     *         must be met in order for the service to be provisioned to a
     *         compute resource. If there are no system SLAs, this method will
     *         return a zero length array
     */
    public String[] getSystemThresholdIDs() {
        String[] keys;
        synchronized (systemThresholds) {
            Set<String> keySet = systemThresholds.keySet();
            keys = keySet.toArray(new String[keySet.size()]);
        }
        return (keys);
    }

    /**
     * Get the ThresholdValue associated to a system identifier
     *
     * @param identifier The system identifier
     * @return A ThresholdValues corresponding to a system identifier which
     *         specifies operational criteria which sysComponents be met in
     *         order for the service to be provisioned to a compute resource. If
     *         there is no matching ThresholdValues object for the system
     *         identifier a null will be returned
     */
    public ThresholdValues getSystemThresholdValue(String identifier) {
        if (identifier == null)
            throw new IllegalArgumentException("identifier is null");
        ThresholdValues tVals;
        synchronized (systemThresholds) {
            tVals = systemThresholds.get(identifier);
        }
        return (tVals);
    }

    /**
     * Get the map of system threshold ids and threshold values
     *
     * @return A Map of system threshold ids and ThresholdValues. If there are
     *         no values return an empty map
     */
    public Map<String, ThresholdValues> getSystemThresholds() {
        Map<String, ThresholdValues> map =
            new HashMap<String, ThresholdValues>();
        synchronized (systemThresholds) {
            map.putAll(systemThresholds);
        }
        return map;
    }

    /**
     * Add a SystemComponent as a system requirement.
     *
     * @param requirements SystemComponent representing a system component
     * requirement
     */
    public void addSystemComponent(SystemComponent... requirements) {
        if (requirements == null)
            return;
        synchronized (systemComponents) {
            systemComponents.addAll(Arrays.asList(requirements));
        }
    }

    /**
     * Get system requirements as an array of SystemComponent objects
     *
     * @return An array of SystemComponent objects representing system component
     *         requirements. A new array will be allocated each time. If there
     *         are no  system requirements, a zero length array will be
     *         returned
     */
    public SystemComponent[] getSystemComponents() {
        SystemComponent[] sysComponents;
        synchronized (systemComponents) {
            sysComponents = systemComponents.toArray(
                new SystemComponent[systemComponents.size()]);
        }
        return (sysComponents);
    }

    /**
     * Determine if any of the required PlatformCapability objects can be
     * provisioned
     *
     * @return Return true if at least one PlatformCapability can be
     *         provisioned
     */
    public boolean hasProvisionableCapability() {
        SystemComponent[] sysComponents = getSystemComponents();
        for (SystemComponent sysComp : sysComponents) {
            if (sysComp.getStagedSoftware().length > 0)
                return (true);
        }
        return (false);
    }

    /**
     * Simple data structure that holds details for a system component
     */
    public static class SystemComponent implements Serializable {
        static final long serialVersionUID = 1L;
        private String name;
        private String className;
        private Map<String, Object> attributes = new HashMap<String, Object>();
        private final List<StagedSoftware> stagedSoftware =
            new ArrayList<StagedSoftware>();
        private ExecDescriptor execDescriptor;
        private URL[] location;

        /**
         * Create a SystemComponent
         *
         * @param name A short name, typically the name of the class sans the
         * package name. Must not be null
         * @param attrs Collection of name value pairs to evaluate, optional
         */
        public SystemComponent(String name, Map<String, Object> attrs) {
            this(name, null, attrs);
        }

        /**
         * Create a SystemComponent
         *
         * @param name A short name, typically the name of the class sans the
         * package name, if null, is derived from the className parameter. Must
         * not be null if the classname parameter is null
         * @param className The fully qualified classname, must not be null if
         * the name parameter is null
         * @param attrs Collection of name value pairs to evaluate, optional
         */
        public SystemComponent(String name,
                               String className,
                               Map<String, Object> attrs) {
            if (name == null && className == null)
                throw new IllegalArgumentException("name and className are null");
            this.className = className;
            if (name == null) {
                int ndx = className.lastIndexOf(".");
                if (ndx > 0)
                    this.name = className.substring(ndx + 1);
                else
                    this.name = className;
            } else {
                this.name = name;
            }
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
        public void setStagedSoftware(StagedSoftware... staged) {
            stagedSoftware.addAll(Arrays.asList(staged));
        }

        /**
         * Get the StagedSoftware for this system component
         *
         * @return The associated StagedSoftware. If there is no
         * StagedSoftware, return a zero-length array
         */
        public StagedSoftware[] getStagedSoftware() {
            return (stagedSoftware.toArray(
                new StagedSoftware[stagedSoftware.size()]));
        }

        /**
         * Set the classpath URLs to load this capability
         *
         * @param urls An array of URLs
         */
        public void setClasspath(URL[] urls) {
            if (urls != null) {
                location = new URL[urls.length];
                System.arraycopy(urls, 0, location, 0, location.length);
            }
        }

        /**
         * Get the classpath URLs to load this capability
         *
         * @return The classpath URLs to load the capability
         */
        public URL[] getClasspath() {
            if (location == null)
                location = new URL[0];
            return (location);
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
        public void setExecDescriptor(ExecDescriptor execDescriptor) {
            this.execDescriptor = execDescriptor;
        }

        /**
         * Provide a String representation
         */
        public String toString() {
            StringBuffer buff = new StringBuffer();
            buff.append("Name=").append(name == null ? "<null>" : name).append(
                ", ");
            buff.append("ClassName=").append(
                className == null ? "<null>" : className).append(", ");
            buff.append("Attributes=").append(attributes.toString());
            return (buff.toString());
        }
    }


    public String toString() {
        return "SystemRequirements{" +
               "systemComponents=" + systemComponents +
               ", systemThresholds=" + systemThresholds +
               '}';
    }
}
