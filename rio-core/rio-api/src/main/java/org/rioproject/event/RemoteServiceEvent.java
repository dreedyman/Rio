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

import net.jini.core.event.RemoteEvent;

import java.io.Serializable;
import java.rmi.MarshalledObject;
import java.util.Date;

/**
 * Wrapper around <code>RemoteEvent</code> to allow user defined event objects. 
 * Since <code>RemoteEvent</code> requires certain parameters to be set in its 
 * constructor this class provides set methods for those parameters. The
 * user can extend this class, creating their own event.
 *
 * <p>Developers of service events will extend this class, creating their own event.
 * When the event handler for an event is invoked to fire the event it will fill in 
 * the required parameters.
 *
 * @author Dennis Reedy
 */
public class RemoteServiceEvent extends RemoteEvent implements Serializable {
    static final long serialVersionUID = 1L;
    /** The time this event was fired */
    private final Date date;

    /**
     * Create a new <code>RemoteServiceEvent</code>. This creates
     * a remote event with the <code>eventID</code>, <code>seqNum</code>
     * and <code>handback</code> parameters uninitialized. They are
     * filled in latter by an <code>EventHandler</code> when the event
     * is actually sent.<br>
     *
     * @param source event source
     */
    public RemoteServiceEvent(Object source) {
        /**
         * <code>source</code> is required by RemoteEvents' superclass, 
         * EventObject, which rejects a null for <code>source</code>.
         */
        super(source, 0, 0, null);
        date = new Date(System.currentTimeMillis());
    }

    /**
     * Set the eventID
     * 
     * @param eventID The eventID for the RemoteEvent
     */
    public void setEventID(long eventID) {
        super.eventID = eventID;
    }

    /**
     * Set the sequence number
     * 
     * @param seqNum The sequence number of the RemoteEvent
     */
    public void setSequenceNumber(long seqNum) {
        super.seqNum = seqNum;
    }

    /**
     * Set the Handback Object
     * 
     * @param handback A MarshalledObject providing a handback mechanism 
     * for the RemoteEvent
     */
    public void setHandback(MarshalledObject handback) {
        super.handback = handback;
    }

    /**
     * Get the Date object that represents the time of the event
     *
     * @return The Date object that represents the time
     */
    public Date getDate(){
        return(date);
    }
}

