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
package org.rioproject.gnostic;

import org.rioproject.sla.RuleMap;

import java.rmi.RemoteException;
import java.util.List;

/**
 * Provides the ability to add, remove and return managed rules.
 *
 * @author Dennis Reedy
 */
public interface Gnostic {

    /**
     * Add a rule mapping to Gnostic.
     *
     * @param ruleMap The rule mapping to add.
     *
     * @return True if the rule mapping was added, false otherwise. If the rule
     * mapping already exists, this method will return false. Note that adding
     * a rule mapping is asynchronous. Returning true means that the rule was
     * added for processing. The rule(s) still require compilation and validation,
     * verifying that the rule has been added can be achieved by getting the
     * managed rule mappings.
     *
     * @throws IllegalArgumentException if the ruleMap argument is null or
     * contains null values.
     *
     * @throws RemoteException If communication errors occur
     */
    boolean add(RuleMap ruleMap) throws RemoteException;

    /**
     * Get all managed rule mappings
     *
     * @return A non-modifiable list of managed rule mappings. If there are no
     * managed rule mappings a zer-length array is returned. A new array is
     * created each time.
     *
     * @throws RemoteException If communication errors occur
     */
    List<RuleMap> get() throws RemoteException;    

    /**
     * Remove an existing managed rule mapping
     *
     * @param ruleMap The rule map to remove
     *
     * @return True if the rule mapping was removed, false otherwise. If the rule
     * mapping does not exist, this method will return false.
     *
     * @throws IllegalArgumentException if the ruleMap argument is null or
     * contains null values.
     *
     * @throws RemoteException If communication errors occur
     */
    boolean remove(RuleMap ruleMap) throws RemoteException;

    /**
     * Get the amount of time (in seconds) the Gnostic polls for changes to
     * managed rules.
     *
     * @return The amount of time (in seconds) the Gnostic polls for changes made
     * to managed rules
     * 
     * @throws RemoteException If communication errors occur
     */
    int getScannerInterval() throws RemoteException;

    /**
     * Set the amount of time (in seconds) the Gnostic polls for changes to
     * managed rules.
     * 
     * @param interval The amount of time (in seconds) to poll
     *
     * @throws RemoteException If communication errors occur
     * @throws IllegalArgumentException if the scannerInterval is <= 0
     */
    void setScannerInterval(int interval) throws RemoteException;

}
