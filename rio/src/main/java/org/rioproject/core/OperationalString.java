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
package org.rioproject.core;

import java.net.URL;

/**
 * An OperationalString represents a collection of application
 * and/or infrastructure software services that when put together provide a
 * coarse-grained service, typically distributed through the network.
 *
 * <p>The OperationalString is the unit of deployment in Rio, and
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
     * Indicates the OperationalString is scheduled for deployment
     */
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
     * Set the OperationalString as being scheduled, deployed or undeployed
     * 
     * @param deployed Either {@link OperationalString#SCHEDULED},
     * {@link OperationalString#DEPLOYED} or {@link OperationalString#UNDEPLOYED}
     *
     * @throws IllegalStateException if the deployed parameter is not
     * {@link OperationalString#SCHEDULED}, {@link OperationalString#DEPLOYED} or
     * {@link OperationalString#UNDEPLOYED}
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
     * {@link org.rioproject.core.ServiceElement} objects
     * 
     * @return An array of ServiceElement objects. If this OperationalString
     * contains no services, this method will return a zero-length array
     */
    ServiceElement[] getServices();

    /**
     * Add a {@link org.rioproject.core.ServiceElement} to the OperationalString.
     * 
     * @param sElem The ServiceElement to add
     */
    void addService(ServiceElement sElem);

    /**
     * Remove a {@link org.rioproject.core.ServiceElement} from the OperationalString.
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
     * Get the {@link org.rioproject.core.Schedule} for the OperationalString 
     * <p>
     * Scheduling of an OperationalString is part of the OperationalString itself,
     * the schedule is an optional declarative attribute, and if not declared the
     * OperationalString will be deployed immediately. The following properties
     * describe the Schedule :
     * <ul>
     * <li>The time to deploy the OperationalString
     * <li>How long it should be deployed for
     * <li>How many times deployment should be repeated
     * <li>The repeatInterval between deployment executions
     * </ul>
     * <b>Declaring a Schedule </b> <br>
     * A Schedule can be declared in an OperationalString XML document as follows :
     * <br>
     * <br>
     * <div style="margin-left: 40px;"> <span style="font-family:
     * monospace;">&lt;DeploymentSchedule&gt; </span> <br style="font-family:
     * monospace;"> <span style="font-family: monospace;">&nbsp;&nbsp;&nbsp;&nbsp;
     * &lt;DeployDate DayOfWeek="Thursday" Hour="8" Minute="30" Format="PM"/&gt;
     * </span> <br style="font-family: monospace;">
     * <span style="font-family: monospace;">&nbsp;&nbsp;&nbsp;&nbsp; &lt;Duration
     * Minutes="30"/&gt; </span> <br style="font-family: monospace;">
     * <span style="font-family: monospace;">&nbsp;&nbsp;&nbsp;&nbsp; &lt;Repeats
     * Count="10"&gt; </span> <br style="font-family: monospace;">
     * <span style="font-family:
     * monospace;">&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; &lt;Interval
     * Days="1" Hours="1" Minutes="1"/&gt; </span> <br style="font-family:
     * monospace;"> <span style="font-family: monospace;">&nbsp;&nbsp;&nbsp;&nbsp;
     * &lt;/Repeats&gt; </span> <br style="font-family: monospace;">
     * <span style="font-family: monospace;">&nbsp;&lt;/DeploymentSchedule&gt;
     * </span> <br>
     * </div>
     * <p>
     * This DeploymentSchedule produces the following:
     * <ul>
     * <li>Deploy the OperationalString every Thursday at 8:30 PM
     * <li>The OperationalString shall remain deployed for 30 minutes
     * <li>The OperationalString deployment repeats 10 times, waiting 1 day, 1 hour
     * and 1 minute between deployments
     * </ul>
     * <p>
     * Once the DeploymentSchedule element is parsed, a Schedule object is created 
     * and set to the enclosing {@link org.rioproject.core.OperationalString} object.
     * 
     * @return The Schedule for the OperationalString, providing context on
     * declared start date, duration, repeat count and repeat interval
     */
    Schedule getSchedule();
}
