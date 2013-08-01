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
package org.rioproject.event;

import net.jini.entry.AbstractEntry;

/**
 * The EventDescriptor describes an event that an EventProducer will advertise
 * as part of it's attribute set in a Jini Lookup Service.
 *
 * @author Dennis Reedy
 */
public class EventDescriptor extends AbstractEntry {
    @SuppressWarnings("unused")
    static final long serialVersionUID = 1L;
    /**
     * The event classes Class that this EventDescriptor describes
     */
    public Class eventClass = null;
    /**
     * The event class event identifier. Care should be taken that this
     * identifier is unique
     */
    public Long eventID = null;

    /**
     * Construct an EventDescriptor with attributes set to null
     */
    public EventDescriptor() {
    }
    
    /**
     * Construct an EventDescriptor with a class and long event ID. The
     * eventID will be turned into a Long
     *
     * @param eventClass The event class the descriptor describes
     * @param eventID The identifier for the event
     */
    public EventDescriptor(Class eventClass, Long eventID) {
        this.eventClass = eventClass;
        this.eventID = eventID;
    }
    
    /**
     * Determine whether the provided EventDescriptor matches properties of this
     * EventDescriptor
     * 
     * @param template The EventDescriptor template to match
     * 
     * @return The following logic determines EventDescriptor matching semantics :
     * <ul>
     * <li>If the provided EventDescriptor properties <code>eventID</code> and 
     * <code>eventClass</code> are <code>null</code>, return <code>true</code>.
     * <li>If the provided EventDescriptor property <code>eventID</code> is 
     * <code>null</code>, and the <code>eventClass</code> property is not 
     * <code>null</code>, return whether the provided EventDescriptor property 
     * <code>eventClass</code> is equal to the <code>eventClass</code> property
     * <li>If the <code>eventID</code> property is <code>null</code>,
     * return <code>false</code>.
     * <li>If the <code>eventID</code> property is not <code>null</code>, and the
     * provided EventDescriptor <code>eventID</code> property is not 
     * <code>null</code>, return whether the provided EventDescriptor property 
     * <code>eventID</code> is equal to the <code>eventID</code> property
     * </ul>
     * 
     * @throws IllegalArgumentException if the the provided EventDescriptor is 
     * <code>null</code> 
     */
    public boolean matches(EventDescriptor template) {
        if(template==null)
            throw new IllegalArgumentException("template is null");
        
        boolean matches = false;        
        if(template.eventID==null) {            
            if(template.eventClass==null)
                matches = true;
            else if(this.eventClass!=null)
                matches = this.eventClass.getName().equals(template.eventClass.getName());
        } else {
            if(this.eventID!=null) 
                matches = this.eventID.equals(template.eventID);
        }                 
       return(matches);
     }
    
    /**
     * Produce a String representation of an EventDescriptor
     */
    public String toString() {
        String eventClassName = (eventClass==null? "<null>":eventClass.getName());
        String eventIDVal = (eventID==null? "<null>":eventID.toString());
        return ("Class: " + eventClassName + ", eventID: " + eventIDVal);
    }
}
