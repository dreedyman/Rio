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
package org.rioproject.monitor;

import net.jini.security.AccessPermission;

/**
 * Permission that can be used to express the access control policy for an
 * instance of an ProvisionMonitor server exported with a 
 * BasicJeriExporter. This class can be specified to 
 * BasicInvocationDispatcher, which will then perform permission checks for
 * incoming remote calls using <code>ProvisionMonitorPermission</code>
 * instances.
 *
 * @author Dennis Reedy
 */
public class ProvisionMonitorPermission extends AccessPermission {
    private static final long serialVersionUID = 2L;

    /**
     * Create a new <code>ProvisionMonitorPermission</code> instance. See
     * {@link AccessPermission}for details on the name parameter.
     * 
     * @param name the target name
     * @throws IllegalArgumentException if the target name is <code>null</code>
     * @throws IllegalArgumentException if the target name does not match the
     * syntax specified in the comments at the beginning of
     * {@link AccessPermission}.
     */
    public ProvisionMonitorPermission(String name) {
        super(name);
    }
}
