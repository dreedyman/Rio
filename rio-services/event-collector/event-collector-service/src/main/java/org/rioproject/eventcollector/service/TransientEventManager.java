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

import net.jini.core.event.RemoteEvent;
import org.rioproject.event.RemoteServiceEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentSkipListMap;

/**
 * An {@code EventManager} that keeps an in-memory collection of events.
 *
 * @author Dennis Reedy
 */
public class TransientEventManager extends AbstractEventManager {
    private final ConcurrentSkipListMap<EventKey, RemoteServiceEvent> eventLog = new ConcurrentSkipListMap<EventKey, RemoteServiceEvent>();
    private static final Logger logger = LoggerFactory.getLogger(TransientEventManager.class.getName());

    @Override
    public void postNotify(final RemoteServiceEvent event) {
        EventKey key = new EventKey(event);
        if(eventLog.containsKey(key)) {
            logger.warn("Already have {}", key);
            return;
        }
        eventLog.put(key, event);
        if(logger.isDebugEnabled())
            logger.debug(String.format("Added key: %s, we have %d events", key, eventLog.size()));
    }

    @Override
    public Collection<RemoteServiceEvent> getEvents() {
        List<RemoteServiceEvent> events = new LinkedList<RemoteServiceEvent>();
        events.addAll(eventLog.values());
        return events;
    }

    @Override
    public Date getLastRecordedDate() {
        if(eventLog.isEmpty())
            return null;
        return eventLog.lastKey().date;
    }

    @Override
    public Collection<RemoteServiceEvent> getEvents(final Date from) {
        if(from==null)
            return getEvents();
        if(logger.isDebugEnabled()) {
            DateFormat formatter = new SimpleDateFormat("HH:mm:ss,SSS");
            logger.debug(String.format("Getting sublist from %s, to %s",
                                      formatter.format(from), formatter.format(eventLog.lastKey())));
        }
        List<RemoteServiceEvent> events = new LinkedList<RemoteServiceEvent>();
        events.addAll(eventLog.subMap(new EventKey(from), eventLog.lastKey()).values());
        return events;
    }

    @Override
    public int delete(Collection<RemoteServiceEvent> events) {
        List<EventKey> removals = new ArrayList<EventKey>();
        for(RemoteEvent event : events) {
            for(Map.Entry<EventKey, RemoteServiceEvent> entry : eventLog.entrySet()) {
                RemoteEvent remoteEvent = entry.getValue();
                if(remoteEvent.getClass().getName().equals(event.getClass().getName()) &&
                    remoteEvent.getSequenceNumber()==event.getSequenceNumber()) {
                    removals.add(entry.getKey());
                }
            }
        }
        for(EventKey eventKey : removals) {
            eventLog.remove(eventKey);
        }
        return removals.size();
    }

    protected void addRemoteEvents(Collection<RemoteServiceEvent> events) {
        for(RemoteServiceEvent event : events) {
            postNotify(event);
        }
    }

    protected int getNumberOfCollectedEvents() {
        return eventLog.size();
    }

    private class EventKey implements Comparable<EventKey> {
        private Long sequenceNumber;
        private final Date date;
        private String name;

        private EventKey(final RemoteServiceEvent event) {
            sequenceNumber = event.getSequenceNumber();
            date = event.getDate();
            name = event.getClass().getName();
        }

        private EventKey(final Date date) {
            this.date = date;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;

            EventKey eventKey = (EventKey) o;

            return date.equals(eventKey.date) &&
                   !(name != null ? !name.equals(eventKey.name) : eventKey.name != null) &&
                   !(sequenceNumber != null ?
                     !sequenceNumber.equals(eventKey.sequenceNumber) :
                     eventKey.sequenceNumber != null);

        }

        @Override
        public int hashCode() {
            int result = sequenceNumber != null ? sequenceNumber.hashCode() : 0;
            result = 31 * result + date.hashCode();
            result = 31 * result + (name != null ? name.hashCode() : 0);
            return result;
        }

        @Override
        public int compareTo(EventKey o) {
            int result = date.compareTo(o.date);
            if(result==0) {
                if(name!=null && o.name!=null) {
                    result = name.compareTo(o.name);
                }
            }
            if(result==0) {
                if(sequenceNumber!=null && o.sequenceNumber!=null) {
                    result = sequenceNumber.compareTo(o.sequenceNumber);
                }
            }
            return result;
        }

        @Override
        public String toString() {
            DateFormat dateFormatter = new SimpleDateFormat("yyyy.MM.dd-HH.mm.ss.SSS");
            StringBuilder nameBuilder = new StringBuilder();
            nameBuilder.append(sequenceNumber).append("-").append(name).append("-");
            nameBuilder.append(dateFormatter.format(date));
            return nameBuilder.toString();
        }
    }
}
