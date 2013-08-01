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
package org.rioproject.resources.client;

import net.jini.lookup.ServiceDiscoveryEvent;
import net.jini.lookup.ServiceDiscoveryListener;

/**
 * An adapter that receives {@link net.jini.lookup.ServiceDiscoveryEvent}s.
 * The methods in this class are empty; this class is provided as a convenience
 * for easily creating listeners by extending this class and overriding only
 * the methods of interest.
 *
 * @author Dennis Reedy
 */
public class ServiceDiscoveryAdapter implements ServiceDiscoveryListener {
    
    /**
     * @see net.jini.lookup.ServiceDiscoveryListener#serviceAdded
     */
    public void serviceAdded(ServiceDiscoveryEvent sdEvent) {
    }
    
    /**
     * @see net.jini.lookup.ServiceDiscoveryListener#serviceChanged
     */
    public void serviceChanged(ServiceDiscoveryEvent sdEvent) {
    }
    
    /**
     * @see net.jini.lookup.ServiceDiscoveryListener#serviceRemoved
     */
    public void serviceRemoved(ServiceDiscoveryEvent sdEvent) {
    } 
}
