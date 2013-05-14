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

import net.jini.id.Uuid;
import org.rioproject.deploy.ServiceBeanInstance;
import org.rioproject.deploy.ServiceProvisionListener;
import org.rioproject.opstring.OperationalStringManager;
import org.rioproject.opstring.ServiceElement;

import java.util.ArrayList;
import java.util.List;

/**
 * The ProvisionRequest class provides a container object holding information  
 * to provision a service.
 *
 * @author Dennis Reedy
 */
public class ProvisionRequest {
    public enum Type {PROVISION, RELOCATE, UNINSTANTIABLE}
    /** The ServiceElement */
    private ServiceElement sElem;
    /** The ServiceProvisionListener */
    private final ProvisionListener listener;
    /** The OperationalStringManager */
    private final OperationalStringManager opStringMgr;
    /** Optional remote ServiceProvisionListener */
    private ServiceProvisionListener svcProvisionListener;
    /** Optional ServiceBeanInstance */
    private ServiceBeanInstance instance;
    /** Optional Uuid of the ServiceBeanInstantiator to exclude */
    private Uuid excludeUuid;
    /** Optional Uuid of the ServiceBeanInstantiator to use */
    private Uuid requestedUuid;
    /** Type of provision request */
    private Type type;
    /** Needed for creating instance identifiers */
    private final InstanceIDManager instanceIDMgr;
    //long priority = 0;
    /** The time the ProvisionRequest was created */
    private final long timestamp;
    private final List<String> failureReasons = new ArrayList<String>();

    /**
     * Create a ProvisionRequest 
     * 
     * @param sElem The ServiceElement
     * @param listener The ProvisionListener
     * @param opStringMgr The OperationalStringManager     
     * @param instanceIDMgr Create instance identifiers if needed
     */
    public ProvisionRequest(ServiceElement sElem,
                            ProvisionListener listener,
                            OperationalStringManager opStringMgr,
                            InstanceIDManager instanceIDMgr) {
        this.sElem = sElem;
        this.listener = listener;
        this.opStringMgr = opStringMgr;
        this.instanceIDMgr = instanceIDMgr;
        this.type = Type.PROVISION;
        timestamp = System.currentTimeMillis();
    }

    /**
     * Create a ProvisionRequest
     *
     * @param sElem The ServiceElement
     * @param listener The ProvisionListener
     * @param opStringMgr The OperationalStringManager
     * @param instanceIDMgr Create instance identifiers if needed
     * @param svcProvisionListener Remote ServiceProvisionListener
     * @param instance The ServiceBeanInstance
     */
    public ProvisionRequest(ServiceElement sElem,
                            ProvisionListener listener,
                            OperationalStringManager opStringMgr,
                            InstanceIDManager instanceIDMgr,
                            ServiceProvisionListener svcProvisionListener,
                            ServiceBeanInstance instance) {
        this.sElem = sElem;
        this.listener = listener;
        this.opStringMgr = opStringMgr;
        this.instanceIDMgr = instanceIDMgr;
        this.svcProvisionListener = svcProvisionListener;
        this.instance = instance;
        this.type = Type.PROVISION;
        timestamp = System.currentTimeMillis();
    }

    /**
     * Create a ProvisionRequest 
     * 
     * @param sElem The ServiceElement
     * @param listener The ProvisionListener
     * @param opStringMgr The OperationalStringManager
     * @param instanceIDMgr Create instance identifiers if needed
     * @param svcProvisionListener Remote ServiceProvisionListener
     * @param instance The ServiceBeanInstance
     * @param excludeUuid The Uuid to exclude when selecting a compute resource
     * @param requestedUuid The requested uuid of the ServiceBeanInstantiator to provision the service to
     * @param type The ProvisionRequest.Type
     */
    public ProvisionRequest(ServiceElement sElem,
                            ProvisionListener listener,
                            OperationalStringManager opStringMgr,
                            InstanceIDManager instanceIDMgr,
                            /* Optional */
                            ServiceProvisionListener svcProvisionListener,
                            ServiceBeanInstance instance,
                            Uuid excludeUuid,
                            Uuid requestedUuid,
                            Type type) {
        this(sElem, listener, opStringMgr, instanceIDMgr);
        this.svcProvisionListener = svcProvisionListener;
        this.instance = instance;
        this.excludeUuid = excludeUuid;
        this.requestedUuid = requestedUuid;
        this.type = type;
    }

    public Uuid getRequestedUuid() {
        return requestedUuid;
    }

    public Uuid getExcludeUuid() {
        return excludeUuid;
    }

    public ServiceElement getServiceElement() {
        return sElem;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public Type getType() {
        return type;
    }

    public InstanceIDManager getInstanceIDMgr() {
        return instanceIDMgr;
    }

    public void setServiceElement(ServiceElement sElem) {
        this.sElem = sElem;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setServiceProvisionListener(ServiceProvisionListener svcProvisionListener) {
        this.svcProvisionListener = svcProvisionListener;
    }

    public ServiceProvisionListener getServiceProvisionListener() {
        return svcProvisionListener;
    }

    public ProvisionListener getListener() {
        return listener;
    }

    public OperationalStringManager getOpStringManager() {
        return opStringMgr;
    }

    public void addFailureReason(String failureReason) {
        if(failureReason!=null && !failureReasons.contains(failureReason))
            failureReasons.add(failureReason);
    }

    public void setRequestedUuid(Uuid requestedUuid) {
        this.requestedUuid = requestedUuid;
    }

    public ServiceBeanInstance getInstance() {
        return instance;
    }

    public List<String> getFailureReasons() {
        return failureReasons;
    }

    /**
     * Create a copy
     *
     * @param pr The ProvisionRequest to copy
     *
     * @return A copy of the ProvisionRequest
     */
    public static ProvisionRequest copy(ProvisionRequest pr) {
        return(new ProvisionRequest(pr.sElem,
                                    pr.listener,
                                    pr.opStringMgr,
                                    pr.instanceIDMgr,
                                    pr.svcProvisionListener,
                                    pr.instance,
                                    pr.excludeUuid,
                                    pr.requestedUuid,
                                    pr.type));
    }
}


