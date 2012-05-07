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
package org.rioproject.sla;

import java.util.EventObject;

/**
 * SLAPolicyHandler implementations create SLAPolicyEvent objects to notify
 * local SLAPolicyEventListener instances of actions it has taken
 *
 * @author Dennis Reedy
 */
public class SLAPolicyEvent extends EventObject {
    /** The SLA the policy event is for */
    private SLA sla;
    /** A message detailing what action occured */
    private String message;
    /** An Object that was created/changed/removed as a result of an action
     * the SLAPolicyHandler took */
    private Object resultant;
    /** When the event was fired */
    private long when;
    /** SLAThresholdEvent */
    private SLAThresholdEvent slaThresholdEvent;

    /**
     * Construct a SLAPolicyEvent   
     *
     * @param source The originator of the event
     * @param sla The SLA the policy event is for
     * @param message A message detailing what action occured
     */
    public SLAPolicyEvent(Object source, SLA sla, String message) {
        super(source);
        if(sla == null)
            throw new IllegalArgumentException("sla is null");
        if(message == null)
            throw new IllegalArgumentException("message is null");
        this.sla = sla;
        this.message = message;
        when = System.currentTimeMillis();
    }

    /**
     * Construct a SLAPolicyEvent   
     *
     * @param source The originator of the action
     * @param sla The SLA the policy event is for
     * @param message A message detailing what action occured
     * @param resultant An Object that was created/changed/removed as a result
     * of an action the SLAPolicyHandler took
     */
    public SLAPolicyEvent(Object source,
                          SLA sla,
                          String message,
                          Object resultant) {
        this(source, sla, message);
        if(resultant==null)
            throw new IllegalArgumentException("resultant is null");
        this.resultant = resultant;
    }

    /**
     * @return Get the SLA
     */
    public SLA getSLA() {
        return(sla);
    }

    /**
     * @return Get the message
     */
     public String getMessage(){
         return(message);
     }

    /**
     * @return Get when the event was fired
     */
    public long getWhen() {
        return(when);
    }

    /**
     * @return Get the resultant Object
     */
    public Object getResultant() {
        return(resultant);
    }

    /**
     * Set the SLAThresholdEvent
     *
     * @param slaThresholdEvent The SLAThresholdEvent
     */
    public void setSLAThresholdEvent(SLAThresholdEvent slaThresholdEvent) {
        this.slaThresholdEvent = slaThresholdEvent;
    }

    /**
     * @return Get the SLAThresholdEvent
     */
    public SLAThresholdEvent getSLAThresholdEvent() {
        return(slaThresholdEvent);
    }
}
