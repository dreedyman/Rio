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
package org.rioproject.examples.events.service;

import org.rioproject.associations.Association;
import org.rioproject.bean.PreDestroy;
import org.rioproject.core.jsb.ServiceBeanContext;
import org.rioproject.event.BasicEventConsumer;
import org.rioproject.event.RemoteServiceEvent;
import org.rioproject.event.RemoteServiceEventListener;
import org.rioproject.examples.events.Hello;
import org.rioproject.examples.events.HelloEvent;
import org.rioproject.watch.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The HelloEventConsumer example
 */
public class HelloEventConsumer {
    /**
     * An EventConsumer that receives notifications for EventProducer
     * instances that produce HelloEvents
     */
    private BasicEventConsumer consumer;
    /**
     * A local watch for measuring how long it took to send the event
     */
    private StopWatch watch = null;
    /**
     * The local event consumer
     */
    private RemoteServiceEventListener localConsumer;

    /**
     * Event notification count
     */
    private int notificationCount;

    /** The Logger for this example */
    static Logger logger = LoggerFactory.getLogger("org.rioproject.examples.events");

    /*
    * The ServiceBeanContext will be injected, allowing the bean to create
    * and add necessary event handling classes
    */
    public void setServiceBeanContext(ServiceBeanContext context) throws Exception {
        /**
         * Create the stop watch, and register the stop watch with the
         * WatchDataRegistry
         */
        watch = new StopWatch("Hello Consumer");
        context.getWatchRegistry().register(watch);
        localConsumer = new LocalEventConsumer();
        consumer = new BasicEventConsumer(HelloEvent.getEventDescriptor(),
                                          localConsumer);
        logger.info("Initialized HelloEvent Consumer");
    }

    public void setEventProducer(Association<Hello> hello) {
        consumer.register(hello.getServiceItem());
    }

    @PreDestroy
    public void cleanup() {
        if (consumer != null) {
            consumer.deregister(localConsumer);
            consumer.terminate();
        }
    }

    public int getNotificationCount() {
        return notificationCount;
    }

    /**
     * The LocalEventConsumer handles notification of remote events. Event
     * notifications of <code>RemoteServiceEvent</code> objects is done
     * within a JVM, i.e. remote invocation semantics are not implied by the use
     * of this interface.
     */
    class LocalEventConsumer implements RemoteServiceEventListener {
        public void notify(RemoteServiceEvent event) {
            if (event instanceof HelloEvent) {
                notificationCount++;
                HelloEvent helloEvent = (HelloEvent) event;
                /*
                * Instead of calling startTiming(), we use the time from the
                * event to set the start time. This will allow us to measure a
                * distributed response. Note: it is assumed the clocks are
                * roughly the same for this example.
                */
                watch.setStartTime(((HelloEvent) event).getWhen());
                watch.stopTiming();
                logger.info("Received HelloEvent seqno={}, message=[{}]",
                            event.getSequenceNumber(), helloEvent.getMessage());
            } else {
                logger.warn("Unwanted event received: {}", event);
            }
        }
    }
}
