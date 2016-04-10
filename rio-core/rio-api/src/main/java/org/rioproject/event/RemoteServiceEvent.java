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

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.rmi.MarshalledObject;
import java.util.Date;
import net.jini.core.event.RemoteEvent;

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
    
    /* Support for versions prior to River 3.0.0:
     * Sets superclass fields in versions of River before 3.0.0, so
     * current state of those fields is serialized.  River 3.0.0 does
     * this automatically for us, by calling getter methods during writeObject.
     * 
     * Presently, due to the com.sun.jini to org.apache.river namespace change
     * it isn't possible for Rio to support both versions of River either
     * side of this change, so most of the code is commented out.
     * 
     * If Rio is restructured slightly, such that a new module is created,
     * that depends on com.sun.jini OR org.apache.river namspaces and 
     * content exported to Rio API, then Rio could support versions either side
     * of the namespace change.  In that case uncomment the code.
     */
//    static final boolean REMOTE_EVENT_CLASS_CHANGED;
//    static final Field EVENT_ID;
//    static final Field SEQ_NUM;
//    static final Field HANDBACK;
//    
//    static { //May throw a security exception
//	boolean classChange;
//	Field event_id = null;
//	Field seq_num = null;
//	Field hand_back = null;
//	try {
//	    event_id = RemoteEvent.class.getDeclaredField("eventID");
//	    int modifier = event_id.getModifiers();
//	    if (modifier == Modifier.PROTECTED){
//		classChange = false;
//		seq_num = RemoteEvent.class.getDeclaredField("seqNum");
//		hand_back = RemoteEvent.class.getDeclaredField("handback");
//	    } else {
//		classChange = true;
//	    }
//	} catch (NoSuchFieldException ex) {
//	    classChange = false;
//	} 
//	REMOTE_EVENT_CLASS_CHANGED = classChange;
//	if (!classChange){
//	    EVENT_ID = event_id;
//	    SEQ_NUM = seq_num;
//	    HANDBACK = hand_back;
//	} else {
//	    EVENT_ID = null;
//	    SEQ_NUM = null;
//	    HANDBACK = null;
//	}
//    }
//    
//    private static void setField (Field f, Object caller, long value){
//	// Support for River / Jini < 3.0.0, remove when no longer required
//	if (!REMOTE_EVENT_CLASS_CHANGED){
//	    try {
//		f.setLong(caller, value);
//	    } catch (IllegalArgumentException ex) {
//		throw new AssertionError("This shouldn't happen", ex);
//	    } catch (IllegalAccessException ex) {
//		throw new AssertionError(
//		    "This class should have access to protected method in superclass",
//		    ex
//		);
//	    }
//	}
//    }
//    
//    private static void setField (Field f, Object caller, Object value){
//	// Support for River / Jini < 3.0.0, remove when no longer required
//	if (!REMOTE_EVENT_CLASS_CHANGED){
//	    try {
//		f.set(caller, value);
//	    } catch (IllegalArgumentException ex) {
//		throw new AssertionError("This shouldn't happen", ex);
//	    } catch (IllegalAccessException ex) {
//		throw new AssertionError(
//		    "This class should have access to protected method in superclass",
//		    ex
//		);
//	    }
//	}
//    }
    
    /** The time this event was fired 
     * @serial
     */
    private final Date date;
    /** Private transient state for compatibility with River 3.0 RemoteEvent
     *  which is immutable.
     */
    private transient long eventID;
    private transient long seqNum;
    private transient MarshalledObject handback;

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
    
    //Inherit javadoc
    @Override
    public long getID(){
	return eventID;
    }

    /**
     * Set the eventID
     * 
     * @param eventID The eventID for the RemoteEvent
     */
    public void setEventID(long eventID) {
        this.eventID = eventID;
//	setField(EVENT_ID, this, eventID);
    }
    
    //Inherit javadoc
    @Override
    public long getSequenceNumber(){
	return seqNum;
    }

    /**
     * Set the sequence number
     * 
     * @param seqNum The sequence number of the RemoteEvent
     */
    public void setSequenceNumber(long seqNum) {
        this.seqNum = seqNum;
//	setField(SEQ_NUM, this, seqNum);
    }
    
    //Inherit javadoc
    @Override
    public MarshalledObject getRegistrationObject() {
	return handback;
    }

    /**
     * Set the Handback Object
     * 
     * @param handback A MarshalledObject providing a handback mechanism 
     * for the RemoteEvent
     */
    public void setHandback(MarshalledObject handback) {
        this.handback = handback;
//	setField(HANDBACK, this, handback);
    }

    /**
     * Get the Date object that represents the time of the event
     *
     * @return The Date object that represents the time
     */
    public Date getDate(){
        return(date);
    }
    
    private void readObject(ObjectInputStream in) 
	    throws ClassNotFoundException, IOException
    {
	/* From River 3.0.0, RemoteEvent's writeObject method calls the getter
	 * methods overridden by this instance.
	 */
	in.defaultReadObject();
	eventID = super.getID();
	seqNum = super.getSequenceNumber();
	handback = super.getRegistrationObject();
    }
}

