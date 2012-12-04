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

import net.jini.id.Uuid;
import net.jini.id.UuidFactory;
import org.rioproject.bean.CreateProxy;
import org.rioproject.core.jsb.ServiceBeanContext;
import org.rioproject.event.DispatchEventHandler;
import org.rioproject.event.EventHandler;
import org.rioproject.event.NoEventConsumerException;
import org.rioproject.examples.events.Hello;
import org.rioproject.examples.events.HelloEvent;
import org.rioproject.watch.GaugeWatch;
import org.rioproject.watch.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * An implementation of the Hello service basic event handling, an the use of
 * a <code>StopWatch</code> and a <code>GaugeWatch</code>.
 */
public class HelloImpl implements Hello {
    /** The event handler for producing HelloEvents */
    private EventHandler eventHandler;
    /** A local watch */
    private StopWatch watch = null;
    /** A copy of the proxy we created */
    private HelloProxy proxy;
    /** Method invocation counter */
    private AtomicInteger visitorNumber = new AtomicInteger(1);
    /** Notification count */
    private AtomicInteger notificationCount = new AtomicInteger();
    /** Watch used for notification counting */
    private GaugeWatch notification;
    /** The Logger for this example */
    static Logger logger = LoggerFactory.getLogger("org.rioproject.examples.events");

    /*
     * Create a custom proxy
     */
    @CreateProxy
    public HelloProxy createProxy(Hello exported) {
        Uuid uuid = UuidFactory.generate();
        proxy = HelloProxy.getInstance(exported, uuid);
        return (proxy);
    }

    /*
     * The ServiceBeanContext will be injected, allowing the bean to create
     * and add necessary event handling classes
     */
    public void setServiceBeanContext(ServiceBeanContext context) throws Exception {
        /* Create and register the event producer */
        eventHandler = new DispatchEventHandler(HelloEvent.getEventDescriptor());
        context.registerEventHandler(HelloEvent.getEventDescriptor(), eventHandler);

        notification = new GaugeWatch("notification");

        /*
        * Create the stop watch, and register the stop watch */
        watch = new StopWatch("HelloWatch");
        context.getWatchRegistry().register(watch, notification);

        logger.debug("Initialized HelloImpl");
    }

    public void sayHello(String message) {
        try {
            /* Measure the time it takes to fire the event */
            watch.startTiming();
            String reply = "Hello visitor #"+(visitorNumber.getAndIncrement())+", " +
                           "your message was \""+message+"\"";
            eventHandler.fire(new HelloEvent(proxy, reply));
            notificationCount.incrementAndGet();
        } catch (NoEventConsumerException e) {
            logger.warn("No Event Consumers currently registered");
        } finally {
            watch.stopTiming();
            notification.addValue(getNotificationCount());
        }
    }

    public int getNotificationCount()  {
        return notificationCount.get();
    }
}
