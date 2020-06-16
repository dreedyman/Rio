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

import org.rioproject.opstring.OperationalString;
import org.rioproject.deploy.ServiceBeanInstance;
import org.rioproject.opstring.ServiceElement;
import org.rioproject.event.EventDescriptor;
import org.rioproject.event.RemoteServiceEvent;
import org.rioproject.resolver.RemoteRepository;

import java.io.Serializable;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

/**
 * This class is used to communicate state changes on OperationalString and 
 * ServiceElements contained within OperationalString objects that are being 
 * monitored by the ProvisionMonitor. Interested event consumers register for event 
 * notification(s) and are notified as state changes occur relative to 
 * OperationalString and ServiceElements within OperationalString objects
 *
 * @author Dennis Reedy
 */
public class ProvisionMonitorEvent extends RemoteServiceEvent implements Serializable {
    @SuppressWarnings("unused")
    static final long serialVersionUID = 1L;
    /** Unique Event ID */
    public static final long ID=2764185076071141340L;
    public enum Action {
        /** Indicates that this event has been created and sent as a result of updating
         * a ServiceElement in an OperationalString */
        SERVICE_ELEMENT_UPDATED,
        /** Indicates that this event has been created and sent as a result of
         * incrementing the number of ServiceBean instances in an OperationalString */
        SERVICE_BEAN_INCREMENTED,
        /** Indicates that this event has been created and sent as a result of
         * incrementing the number of ServiceBean instances in an OperationalString */
        SERVICE_BEAN_DECREMENTED,
        /** Indicates that this event has been created and sent as a result of adding
         * a service to an OperationalString */
        SERVICE_ELEMENT_ADDED,
        /** Indicates that this event has been created and sent as a result of removing
         * a service from an OperationalString */
        SERVICE_ELEMENT_REMOVED,
        /** Indicates that this event has been created and sent as a result of deploying
         * an OperationalString */
        OPSTRING_DEPLOYED,
        /** Indicates that this event has been created and sent as a result of
         * undeploying an OperationalString */
        OPSTRING_UNDEPLOYED,
        /** Indicates that this event has been created and sent as a result of
         * updating an OperationalString */
        OPSTRING_UPDATED,
        /** Indicates that this event has been created and sent as a result of updating
         * a ServiceBeanInstance */
        SERVICE_BEAN_INSTANCE_UPDATED,
        /** Indicates that this event has been created and sent as a result of submitting
         * a redeployment request */
        REDEPLOY_REQUEST,
        /** Indicates that this event has been created and sent as a result of a
         * successful service provisioning */
        SERVICE_PROVISIONED,
        /** Indicates that this event has been created and sent as a result of a
         * service failure */
        SERVICE_FAILED,
        /** Indicates that this event has been created and sent as a result of
         * changing the primary OperationalStringManager */
        OPSTRING_MGR_CHANGED,
        /** Indicates that this event has been created and sent as a result of a
         * service termination */
        SERVICE_TERMINATED,
        /** Indicates that an external service has been discovered */
        EXTERNAL_SERVICE_DISCOVERED
    }
    /** The action for the event */
    private Action action;
    /** The OperationalString name */
    private String opStringName;
    /** The ServiceElement. May be null */
    private ServiceElement sElem;
    /** The OperationalString. May be null */
    private OperationalString opString;    
    /** The Redeployment arguments. May be null */
    private Object[] redeploymentParms;
    /** The ServiceBeanInstance. May be null */
    private ServiceBeanInstance instance;
    private final Collection<RemoteRepository> remoteRepositories = new ArrayList<RemoteRepository>();

    /**
     * Create a ProvisionMonitorEvent for a ServiceElement add, remove or change
     * notification
     *
     * @param source The source (originator) of the event
     * @param action The type of ProvisionMonitorEvent
     * @param sElem The ServiceElement that changed
     */
    public ProvisionMonitorEvent(Object source, Action action, ServiceElement sElem) {
        super(source);
        if(sElem==null)
            throw new IllegalArgumentException("sElem is null");
        opStringName = sElem.getOperationalStringName();
        this.action = action;
        this.sElem = sElem;
    }

    /**
     * Create a ProvisionMonitorEvent for an OperationalString deployment, undeployment, update,
     * or OperationaStringManager change
     *
     * @param source The source (originator) of the event
     * @param action The action the event represents
     * @param opString The OperationalString undeployed
     */
    public ProvisionMonitorEvent(Object source, Action action, OperationalString opString) {
        super(source);
        if(opString==null)
            throw new IllegalArgumentException("opString is null");
        this.action = action;
        this.opString = opString;
        opStringName = opString.getName();
    }

    /**
     * Create a ProvisionMonitorEvent indicating a ServiceBeanInstance has been 
     * updated
     *
     * @param source The source (originator) of the event
     * @param opStringName The name of the OperationalString 
     * @param instance The ServiceBeanInstance that changed
     */
    public ProvisionMonitorEvent(Object source, String opStringName, ServiceBeanInstance instance) {
        super(source);
        if(opStringName==null)
            throw new IllegalArgumentException("opStringName cannot be null");
        if(instance==null)
            throw new IllegalArgumentException("instance is null");
        this.action = Action.SERVICE_BEAN_INSTANCE_UPDATED;
        this.instance = instance;
        this.opStringName = opStringName;
    }

    /**
     * Create a ProvisionMonitorEvent indicating a service provision or failure
     * notification
     *
     * @param source The source (originator) of the event
     * @param action The action the evet represents
     * @param opStringName The name of the OperationalString
     * @param sElem The ServiceElement
     * @param instance The ServiceBeanInstance
     */
    public ProvisionMonitorEvent(Object source,
                                 Action action,
                                 String opStringName,
                                 ServiceElement sElem,
                                 ServiceBeanInstance instance) {
        super(source);
        if(opStringName==null)
            throw new IllegalArgumentException("opStringName cannot be null");
        if(sElem==null)
            throw new IllegalArgumentException("sElem is null");
        this.action = action;
        this.sElem = sElem;
        this.instance = instance;
        this.opStringName = opStringName;
    }
    
    /**
     * Create a ProvisionMonitorEvent indicating a redeployment request 
     * has been submitted
     *
     * @param source The source (originator) of the event
     * @param opStringName The name of the OperationalString
     * @param sElem The ServiceElement
     * @param instance The ServiceBeanInstance
     * @param args Parameters for a redeployment  
     */
    public ProvisionMonitorEvent(Object source,
                                 String opStringName,
                                 ServiceElement sElem,
                                 ServiceBeanInstance instance,
                                 Object[] args) {
        super(source);        
        if(opStringName==null)
            throw new IllegalArgumentException("opStringName cannot be null");
        if(args==null)
            throw new IllegalArgumentException("redeployment args cannot be null");
        this.action = Action.REDEPLOY_REQUEST;
        this.opStringName = opStringName;
        this.sElem = sElem;
        this.instance = instance;
        redeploymentParms = new Object[args.length];
        System.arraycopy(args, 0, redeploymentParms, 0, redeploymentParms.length);        
    }

    /**
     * Get the action attribute
     *
     * @return The action
     */
    public Action getAction() {
        return(action);
    }    

    /**
     * Get the OperationalString name. 
     *
     * @return The name of the OperationalString associated with this event
     */
    public String getOperationalStringName() {
        return(opStringName);
    }
    
    /**
     * Get the ServiceElement attribute
     *
     * @return The ServiceElement associated with this event. This property 
     * will be null if the action type is not SERVICE_ELEMENT_ADDED, 
     * SERVICE_ELEMENT_UPDATED, SERVICE_ELEMENT_REMOVED, SERVICE_PROVISIONED or
     * SERVICE_FAILED. This property may be null if the action type is 
     * REDEPLOY_REQUEST
     */
    public ServiceElement getServiceElement() {
        return(sElem);
    }    

    /**
     * Get the OperationalString attribute. 
     *
     * @return The OperationalString associated with this event. This property 
     * will be null if the action type is not OPSTRING_DEPLOYED or 
     * OPSTRING_UNDEPLOYED
     */
    public OperationalString getOperationalString() {
        return(opString);
    }    
    
    /**
     * Get the ServiceBeanInstance
     * 
     * @return The ServiceBeanInstance associated with this event. This property 
     * will be null if the action type is not SERVICE_BEAN_INSTANCE_UPDATED. This
     * property may be null if the action type is REDEPLOY_REQUEST, 
     * SERVICE_PROVISIONED or SERVICE_FAILED
     */
    public ServiceBeanInstance getServiceBeanInstance() {
        return(instance);
    }
        
    /**
     * Get the Redeployment parameters
     * 
     * @return The Redeployment parameters associated with this event. The
     * Object array will have as it's content 
     * {Date.class, Boolean.class, ServiceProvisionListener.class}. This 
     * property will be null if the action type is not REDEPLOY_REQUEST
     */
    public Object[] getRedeploymentParms() {
        return(redeploymentParms);
    }

    /**
     * Helper method to return the EventDescriptor for this event
     *
     * @return The EventDescriptor for this event
     */
    public static EventDescriptor getEventDescriptor(){
        return(new EventDescriptor(ProvisionMonitorEvent.class, ID));
    }

    public RemoteRepository[] getRemoteRepositories() {
        return remoteRepositories.toArray(new RemoteRepository[remoteRepositories.size()]);
    }
    
    public void setRemoteRepositories(RemoteRepository[] repositories) {
        Collections.addAll(this.remoteRepositories, repositories);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        String format = "yyyy.MM.dd HH:mm:ss,SSS";
        DateFormat formatter = new SimpleDateFormat(format);
        sb.append("ProvisionMonitorEvent: ");
        sb.append("action=").append(action);
        sb.append(", opStringName='").append(opStringName);
        if(sElem!=null)
            sb.append(", service=").append(sElem.getName());
        if(redeploymentParms!=null)
            sb.append(", redeploymentParms=").append(Arrays.asList(redeploymentParms).toString());
        sb.append(", when: ").append(formatter.format(getDate()));
        return sb.toString();
    }
}
