/*
 * Copyright to the original author or authors
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
package org.rioproject.eventcollector.service;

import com.sun.jini.landlord.LeasedResource;
import net.jini.core.event.RemoteEvent;
import net.jini.core.event.RemoteEventListener;
import net.jini.id.Uuid;
import net.jini.security.ProxyPreparer;

import java.io.IOException;
import java.io.Serializable;
import java.rmi.MarshalledObject;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author Dennis Reedy
 */
public class RegisteredNotification implements LeasedResource, Serializable {
    private static final long serialVersionUID = 1L;
    private final Uuid registrationID;
    private transient RemoteEventListener eventListener;
    /**
     * The marshalled form of the client-provided notification target.
     */
    private MarshalledObject<RemoteEventListener> marshalledEventListener = null;
    private long expiration;
    private final AtomicBoolean historyUpdating = new AtomicBoolean(false);
    private final List<RemoteEvent> missedEvents = new ArrayList<RemoteEvent>();
    private final AtomicReference<Date> eventIndex = new AtomicReference<Date>();
    private final List<RemoteEvent> unknownEvents = new ArrayList<RemoteEvent>();

    public RegisteredNotification(Uuid registrationID) {
        this.registrationID = registrationID;
    }

    public void restore(ProxyPreparer preparer) throws ClassNotFoundException, IOException {
        if(marshalledEventListener!=null) {
            RemoteEventListener unprepared = marshalledEventListener.get();
            eventListener = (RemoteEventListener)preparer.prepareProxy(unprepared);
        }
    }

    public void setRemoteEventListener(RemoteEventListener eventListener) throws IOException {
        this.eventListener = eventListener;
        if(this.eventListener==null) {
            marshalledEventListener = null;
            unknownEvents.clear();
        } else {
            marshalledEventListener = new MarshalledObject<RemoteEventListener>(eventListener);
        }
    }

    public RemoteEventListener getEventListener() {
        return eventListener;
    }

    public void setHistoryUpdating(boolean updating) {
        historyUpdating.set(updating);
    }

    public boolean getHistoryUpdating() {
        return historyUpdating.get();
    }

    public void setExpiration(long expiration) {
        this.expiration = expiration;
    }

    public long getExpiration() {
        return expiration;
    }

    public Uuid getCookie() {
        return registrationID;
    }

    public void addMissedEvent(RemoteEvent event) {
        missedEvents.add(event);
    }

    public List<RemoteEvent> getAndClearMissedEvents() {
        List<RemoteEvent> list = new ArrayList<RemoteEvent>();
        list.addAll(missedEvents);
        missedEvents.clear();
        return list;
    }

    public void setEventIndex(Date index) {
        eventIndex.set(index);
    }

    public Date getEventIndex() {
        return eventIndex.get();
    }

    public void addUnknown(RemoteEvent event) {
        unknownEvents.add(event);
    }

    public boolean isUnknown(RemoteEvent event) {
        boolean unknown = false;
        for(RemoteEvent unknownEvent : unknownEvents) {
            if(event.getID()==unknownEvent.getID()) {
                unknown = true;
                break;
            }
        }
        return unknown;

    }
}
