/*
 * Copyright 2008 the original author or authors.
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
package org.rioproject.examples.events;

import org.rioproject.event.EventDescriptor;
import org.rioproject.event.RemoteServiceEvent;

/**
 * The HelloEvent is a RemoteServiceEvent
 * 
 * @see org.rioproject.event.RemoteServiceEvent
 */
public class HelloEvent extends RemoteServiceEvent {
    /** A unique id number for the hello event **/
    public static final long ID = 9999999999L;
    /** Holds the property for the time the event was created */
    private long when;
    /** Holds the message property */
    private String message;

    /**
     * Creates a HelloEvent with no message
     *
     * @param source The event source
     */
    public HelloEvent(Object source) {
        this(source, null);
    }

    /**
     * Creates a HelloEvent with a message
     *
     * @param source The event source
     * @param message The message
     */
    public HelloEvent(Object source, String message) {
        super(source);
        this.message = message;
        when = System.currentTimeMillis();
    }

    /**
     * Getter for property when.
     * 
     * @return Value of property when.
     */
    public long getWhen() {
        return when;
    }

    /**
     * Getter for message property
     *
     * @return The valueof the message property
     */
    public String getMessage() {
        return(message);
    }

    /**
     * Helper method to return the EventDescriptor for this event
     *
     * @return The EventDescriptor for this event
     */
    public static EventDescriptor getEventDescriptor() {
        return (new EventDescriptor(HelloEvent.class, ID));
    }
}
