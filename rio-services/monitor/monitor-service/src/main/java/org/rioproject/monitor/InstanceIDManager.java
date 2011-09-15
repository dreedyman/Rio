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
package org.rioproject.monitor;

/**
 * The interface defines the semantics to manage instance identifiers. An instance 
 * identifier correlates to an ordinal value of a service instance relative to the 
 * number of service instances that have been provisioned.
 *
 * @author Dennis Reedy
 */ 
public interface InstanceIDManager {
    /**
     * Get the next instance identifier
     *    
     * @return The next instance identifier
     */ 
    long getNextInstanceID();
}
