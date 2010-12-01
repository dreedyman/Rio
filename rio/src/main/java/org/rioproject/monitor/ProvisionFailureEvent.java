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
package org.rioproject.monitor;

import org.rioproject.core.ServiceElement;
import org.rioproject.event.EventDescriptor;
import org.rioproject.event.RemoteServiceEvent;

import java.io.Serializable;
import java.util.Date;

/**
 * This class is used to communicate to interested registrants that a provision
 * attempt for a particular service was unsuccessful
 *
 * @author Dennis Reedy
 */
public class ProvisionFailureEvent extends RemoteServiceEvent
    implements Serializable {
    static final long serialVersionUID = 1L;
    /** Unique Event ID */
    public static final long ID = -7832310585750966248L;
    /** The Exception associated with this failure. This may be null */
    private Throwable exception;
    /** The ServiceElement that could not be provisioned */
    private ServiceElement sElem;
    /** A plausible reason why the provision failed */
    private String reason;

    /**
     * Create a ProvisionFailureEvent with attributes
     *
     * @param source The originator of the event
     * @param sElem The ServiceElement
     * @param reason Reason for the failure
     * @param exception An associated Exception (if any)
     */
    public ProvisionFailureEvent(Object source,
                                 ServiceElement sElem,
                                 String reason,
                                 Throwable exception) {
        super(source);
        this.sElem = sElem;
        this.reason = reason;
        this.exception = exception;
        this.date = new Date();
    }

    /**
     * Get the ServiceElement attribute
     *
     * @return The ServiceElement
     */
    public ServiceElement getServiceElement() {
        return (sElem);
    }

    /**
     * Get the reason why provisioning failed
     *
     * @return Reason for the failure
     */
    public String getReason() {
        return (reason);
    }

    /**
     * Get the Throwable attribute
     *
     * @return An associated Exception
     */
    public Throwable getThrowable() {
        return (exception);
    }

    public static EventDescriptor getEventDescriptor() {
        return new EventDescriptor(ProvisionFailureEvent.class, ID);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("ProvisionFailureEvent: ");
        sb.append("opStringName=").append(sElem.getOperationalStringName());
        sb.append(", service=").append(sElem.getName());
        sb.append(", reason='").append(reason);
        return sb.toString();
    }
}
