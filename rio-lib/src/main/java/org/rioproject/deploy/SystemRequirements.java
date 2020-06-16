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

import org.rioproject.watch.ThresholdValues;

import java.io.Serializable;
import java.util.*;

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
    private final List<SystemComponent> systemComponents = new ArrayList<SystemComponent>();
    /**
     * Array of system requirement SLAs
     */
    private final Map<String, ThresholdValues> systemThresholds = new HashMap<String, ThresholdValues>();

    enum Operation {EXCLUDE, INCLUDE}

    /** Array of machines that have been identified as part of a cluster of
     * machine used as targets for provisioning */
    private final List<String> machineCluster = new ArrayList<String>();

    /**
     * Add a system required ThresholdValue
     *
     * @param identifier String identifier for the system property that has a
     * ThresholdValue
     * @param tVals ThresholdValues specifying operational criteria which must
     * be met in order for the service to be provisioned to a compute resource
     */
    public void addSystemThreshold(final String identifier, final ThresholdValues tVals) {
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
        return keys;
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
    public ThresholdValues getSystemThresholdValue(final String identifier) {
        if (identifier == null)
            throw new IllegalArgumentException("identifier is null");
        ThresholdValues tVals;
        synchronized (systemThresholds) {
            tVals = systemThresholds.get(identifier);
        }
        return tVals;
    }

    /**
     * Get the map of system threshold ids and threshold values
     *
     * @return A Map of system threshold ids and ThresholdValues. If there are
     *         no values return an empty map
     */
    public Map<String, ThresholdValues> getSystemThresholds() {
        Map<String, ThresholdValues> map = new HashMap<String, ThresholdValues>();
        synchronized (systemThresholds) {
            map.putAll(systemThresholds);
        }
        return map;
    }

    /**
     * Add a SystemComponent as a system requirement.
     *
     * @param requirements SystemComponent representing a system component requirement
     */
    public void addSystemComponent(final SystemComponent... requirements) {
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
        return sysComponents;
    }

    /**
     * Determine if any of the required PlatformCapability objects can be
     * provisioned
     *
     * @return Return true if at least one PlatformCapability can be provisioned
     */
    @SuppressWarnings("unused")
    public boolean hasProvisionableCapability() {
        SystemComponent[] sysComponents = getSystemComponents();
        for (SystemComponent sysComp : sysComponents) {
            if (sysComp.getStagedSoftware()!=null)
                return true;
        }
        return false;
    }

    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("SystemRequirements: systemComponents=").append(systemComponents);
        builder.append(", systemThresholds=").append(systemThresholds);
        return builder.toString();
    }
}
