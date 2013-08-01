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
package org.rioproject.sla;

import org.rioproject.deploy.ServiceBeanInstance;
import org.rioproject.event.EventDescriptor;
import org.rioproject.opstring.ServiceElement;
import org.rioproject.watch.Calculable;
import org.rioproject.watch.ThresholdEvent;
import org.rioproject.watch.ThresholdType;

import java.io.Serializable;

/**
 * The SLAThresholdEvent extends ThresholdEvent and represents a threshold
 * being crossed for a Service.
 *
 * @author Dennis Reedy
 */
public class SLAThresholdEvent extends ThresholdEvent implements Serializable {
    @SuppressWarnings("unused")
    static final long serialVersionUID = 1L;
    /** Unique ID */
    public static final long ID = 1000000001;
    /** The ServiceElement for the service */
    private final ServiceElement sElem;
    /** Description of th SLAPolicyHandler */
    private String slaPolicyHandlerDescription;
    /** Holds value of property hostAddress */
    private final String hostAddress;
    /** The ServiceBeanInstance */
    private ServiceBeanInstance instance;
    
    /**
     * Creates new SLAThresholdEvent 
     *
     * @param source The event source
     * @param sElem The ServiceElement for the service
     * @param instance The ServiceBeanInstance
     * @param calculable New value of property calculable
     * @param sla The sla
     * @param slaPolicyHandlerDescription = Description of the SLAPolicyHandler
     * @param hostAddress The TCP/IP address of the ComputeResource
     * @param type The type of event, breached or cleared
     */
    public SLAThresholdEvent(Object source, 
                             ServiceElement sElem,
                             ServiceBeanInstance instance,
                             Calculable calculable, 
                             SLA sla, 
                             String slaPolicyHandlerDescription,
                             String hostAddress,
                             ThresholdType type) {
        super(source, calculable, sla, type);
        this.sElem = sElem;
        this.slaPolicyHandlerDescription = slaPolicyHandlerDescription;
        this.hostAddress = hostAddress;
        this.instance = instance;
    }

    /** 
     * Getter for property sElem
     *
     * @return Value of property sElem
     */
    public ServiceElement getServiceElement() {
        return(sElem);
    }

    /** 
     * Getter for property sla
     *
     * @return Value of property sla
     */
    public SLA getSLA() {
        return((SLA)getThresholdValues());
    }

    /** 
     * Setter for property sla
     *
     * @param sla New value of property sla
     */
    /*public void setSLA(final SLA sla) {
        setThresholdValues(sla);
    }*/

    /** 
     * Getter for property slaPolicyHandlerDescription
     *
     * @return Value of property slaPolicyHandlerDescription
     */
    public String getSLAPolicyHandlerDescription() {
        return(slaPolicyHandlerDescription);
    }

    /** 
     * Getter for property hostAddress
     *
     * @return Value of property hostAddress
     */
    public String getHostAddress() {
        return(hostAddress);
    }

    /**
     * Get the ServiceBeanInstance
     *
     * @return The ServiceBeanInstance
     */
    public ServiceBeanInstance getServiceBeanInstance() {
        return(instance);
    }
    
    /**
     * Helper method to return the EventDescriptor for this event
     *
     * @return The EventDescriptor for this event
     */
    public static EventDescriptor getEventDescriptor(){
        return(new EventDescriptor(SLAThresholdEvent.class, ID));
    }

}
