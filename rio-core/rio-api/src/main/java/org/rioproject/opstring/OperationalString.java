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
package org.rioproject.opstring;

import java.net.URL;

/**
 * An OperationalString represents a collection of application
 * and/or infrastructure software services that when put together provide a
 * coarse-grained service, typically distributed through the network.
 *
 * <p>The {@code OperationalString} is the unit of deployment in Rio, and
 * provides the capability to declare, monitor and manage the availability
 * of enclosed services.
 *
 * @author Dennis Reedy
 */
public interface OperationalString {
    /**
     * Indicates the OperationalString is not deployed
     */
    public static final int UNDEPLOYED = 0;
    /**
     * @deprecated No longer supported
     */
    @Deprecated
    public static final int SCHEDULED = 1;
    /**
     * Indicates the OperationalString is deployed
     */
    public static final int DEPLOYED = 2;
    /**
     * Indicates the OperationalString is deployed and is broken, where all
     * required services are not available
     */
    public static final int BROKEN = 3;
    /**
     * Indicates the OperationalString is deployed and is compromised, where
     * some specified services are not available
     */
    public static final int COMPROMISED = 4;
    /**
     * Indicates the OperationalString is deployed and is intact, where all
     * specified services are available
     */
    public static final int INTACT = 5;

    /**
     * Get the status of the OperationalString
     * 
     * @return The status of the OperationalString. If the OperationalString has
     * not been scheduled or deployed the OperationalString status must always
     * return {@link OperationalString#UNDEPLOYED}. If the OperationalString is
     * {@link OperationalString#DEPLOYED}, then the status
     * will represent the 'weakest link in the chain', that is if this
     * OperationalString has nested OperationalString instances whose state is
     * of lesser fidelity (lesser fidelity reflecting a 
     * {@link OperationalString#BROKEN} status, highest
     * fidelity representing an {@link OperationalString#INTACT} state) then the
     * status of this OperationalString must reflect the weakest status. If no nested
     * OperationalString instances are found, or the OperationalString itself
     * has a lesser fidelity then it's nested OperationalString instances, the
     * status is determined by the inspecting ServiceElement instance
     * availability
     */
    int getStatus();

    /**
     * Set the OperationalString as being deployed or undeployed
     * 
     * @param deployed Either {@link OperationalString#DEPLOYED} or {@link OperationalString#UNDEPLOYED}
     *
     * @throws IllegalStateException if the deployed parameter is not
     * {@link OperationalString#DEPLOYED} or {@link OperationalString#UNDEPLOYED}
     */
    void setDeployed(int deployed);

    /**
     * All OperationalString instances have a descriptive name. This method is
     * used to get the name of this OperationalString
     * 
     * @return The name of the OperationalString
     */
    String getName();    

    /**
     * An OperationalString may contain other OperationalString instances. In
     * this fashion OperationalString instances may be nested. This method
     * returns an array of OperationalString objects that this OperationalString
     * contains
     * 
     * @return An array of OperationalString objects. If this OperationalString
     * does not contain any other OperationalStrings, this method will return a
     * zero-length array
     */
    OperationalString[] getNestedOperationalStrings();

    /**
     * Get all services contained by this OperationalString as an array of
     * {@link ServiceElement} objects
     * 
     * @return An array of ServiceElement objects. If this OperationalString
     * contains no services, this method will return a zero-length array
     */
    ServiceElement[] getServices();

    /**
     * Add a {@link ServiceElement} to the OperationalString.
     * 
     * @param sElem The ServiceElement to add
     */
    void addService(ServiceElement sElem);

    /**
     * Remove a {@link ServiceElement} from the OperationalString.
     * 
     * @param sElem The ServiceElement to remove
     */
    void removeService(ServiceElement sElem);

    /**
     * Get the location the OperationalString was loaded from.
     * 
     * @return The URL OperationalString was loaded from. The value may be null
     * if the OperationalString was not loaded from a file or repository based
     * mechanism
     */
    URL loadedFrom();

    /**
     * Get undeployment option
     *
     * @return The {@code UndeployOption} if any. May return {@code null}.
     */
    UndeployOption getUndeployOption();
}
