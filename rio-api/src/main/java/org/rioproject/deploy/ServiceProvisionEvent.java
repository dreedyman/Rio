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

import net.jini.core.event.RemoteEvent;
import org.rioproject.opstring.OperationalStringManager;
import org.rioproject.opstring.ServiceElement;

import java.io.Serializable;
import java.rmi.MarshalledObject;

/**
 * This event class is sent by a {@link ProvisionManager}
 * after detecting that a specific service requires provisioning. The recipient of
 * this event must attempt to instantiate the service described by the
 * {@link org.rioproject.opstring.ServiceElement} object.
 *
 * @author Dennis Reedy
 */
public class ServiceProvisionEvent extends RemoteEvent implements Serializable {
    static final long serialVersionUID = 1L;
    /** Unique Event ID */
    public static final long ID = -5934673946890420068L;
    /** The ServiceElement */
    private ServiceElement svcElement;
    /** The OperationalStringManager */
    private OperationalStringManager opStringManager;    

    /**
     * Create a ServiceProvisionEvent
     * 
     * @param source The event source
     */
    public ServiceProvisionEvent(Object source) {
        super(source, ID, 0, null);
    }

    /**
     * Instantiate a ServiceProvisionEvent
     * 
     * @param source The event source
     * @param opStringManager The OperationalStringMonitor
     * @param svcElement The ServiceElement
     */
    public ServiceProvisionEvent(Object source,
                                 OperationalStringManager opStringManager, 
                                 ServiceElement svcElement) {
        super(source, ID, 0, null);
        this.opStringManager = opStringManager;
        this.svcElement = svcElement;
    }

    /**
     * Set the ServiceElement object
     * 
     * @param svcElement The ServiceElement
     */
    public void setServiceElement(ServiceElement svcElement) {
        this.svcElement = svcElement;
    }

    /**
     * Get the ServiceElement object
     * 
     * @return ServiceElement The ServiceElement
     */
    public ServiceElement getServiceElement() {
        return (svcElement);
    }

    /**
     * Set the OperationalStringManager
     * 
     * @param opStringManager The OperationalStringManager
     */
    public void setOperationalStringManager(
                                      OperationalStringManager opStringManager) {
        this.opStringManager = opStringManager;
    }

    /**
     * Get the OperationalStringManager
     * 
     * @return OperationalStringManager The OperationalStringManager
     */
    public OperationalStringManager getOperationalStringManager() {
        return (opStringManager);
    }

    /**
     * Set the sequence number
     * 
     * @param seqNum The sequence number
     */
    public void setSequenceNumber(long seqNum) {
        super.seqNum = seqNum;
    }

    /**
     * Set the handback object
     * 
     * @param handback The handback object
     */
    public void setHandback(MarshalledObject handback) {
        super.handback = handback;
    }
}
