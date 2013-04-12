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
package org.rioproject.logging;

import org.rioproject.config.Constants;
import org.rioproject.event.EventHandler;
import org.rioproject.event.EventProducer;
import org.rioproject.loader.ServiceClassLoader;
import org.rioproject.log.*;
import org.rioproject.net.HostUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Properties;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.logging.LogRecord;

/**
 * Publishes {@code ServiceLogEvent}s
 *
 * @author Dennis Reedy
 */
public class ServiceLogEventPublisherImpl implements ServiceLogEventPublisher {
    private final EventHandler eventHandler;
    private final EventProducer eventProducer;
    private final InetAddress address;
    private final Executor eventExecutor = Executors.newSingleThreadExecutor();
    private final static Logger logger = LoggerFactory.getLogger(ServiceLogEventPublisherImpl.class);

    public ServiceLogEventPublisherImpl(final EventHandler eventHandler, final EventProducer eventProducer) {
        try {
            address = HostUtil.getInetAddressFromProperty(Constants.RMI_HOST_ADDRESS);
        } catch (UnknownHostException e) {
            throw new RuntimeException("Trying to initialize ServiceLogEventPublisher", e);
        }
        this.eventHandler = eventHandler;
        this.eventProducer = eventProducer;
    }

    public void publish(LogRecord logRecord) {
        if(eventProducer==null)
            return;
        String opStringName = null;
        String serviceName = null;
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        if(cl instanceof ServiceClassLoader) {
            Properties props = ((ServiceClassLoader)cl).getMetaData();
            opStringName = props.getProperty("opStringName");
            serviceName = props.getProperty("serviceName");
        }
        eventExecutor.execute(new ServiceLogEventTask(new ServiceLogEvent(eventProducer,
                                                                          logRecord,
                                                                          opStringName,
                                                                          serviceName,
                                                                          address)
        ));
    }

    class ServiceLogEventTask implements Runnable {
        ServiceLogEvent event;

        ServiceLogEventTask(ServiceLogEvent event) {
            this.event = event;
        }

        public void run() {
            try {
                eventHandler.fire(event);
                event = null;
            } catch(Exception e) {
                logger.error("Fire ServiceLogEvent", e);
            }
        }
    }
}

