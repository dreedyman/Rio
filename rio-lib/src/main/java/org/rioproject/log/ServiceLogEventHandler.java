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
package org.rioproject.log;

import org.rioproject.loader.ServiceClassLoader;
import org.rioproject.config.Constants;
import org.rioproject.event.EventHandler;
import org.rioproject.event.EventProducer;

import javax.management.*;
import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Properties;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

/**
 * A {@link java.util.logging.Handler} that publishes {@link ServiceLogEvent}s
 * to interested consumers when a {@link java.util.logging.LogRecord} of
 * interest is logged.
 *
 * <p>A {@link java.util.logging.LogRecord} of interest is one that contains an
 * exception.
 *
 * <p>The <code>ServiceLogEventHandler</code> will also publish
 * {@link ServiceLogEvent}s to interested consumers if the <code>LogRecord</code>
 * has a level that is greater than or equal to the <code>ServiceLogEventHandler</code>'s
 * configured {@link java.util.logging.Level} property (the default for this is
 * {@link java.util.logging.Level#SEVERE}), and whose name has been configured as a
 * <i>publishable</i> logger. Matching semantics for a <i>publishable</i> logger
 * name (as obtained from {@link java.util.logging.LogRecord#getLoggerName()}
 * starts with a configured <i>publishable</i> logger name.  
 */
@SuppressWarnings("PMD.AvoidThrowingRawExceptionTypes")
public class ServiceLogEventHandler extends Handler implements ServiceLogEventHandlerMBean {
    private Level publishOnLevel = Level.SEVERE;
    private final Collection<String> publishableLoggers =
        Collections.synchronizedCollection(new ArrayList<String>());
    private EventHandler eventHandler;
    private EventProducer source;
    private InetAddress address;
    private final Executor eventExecutor = Executors.newSingleThreadExecutor();
    private final static Logger logger = Logger.getLogger(ServiceLogEventHandler.class.getName());

    public ServiceLogEventHandler() {
        try {
            String a = System.getProperty(Constants.RMI_HOST_ADDRESS);
            if(a==null)
                address = InetAddress.getLocalHost();
            else
                address = InetAddress.getByName(a);
        } catch (UnknownHostException e) {
            throw new RuntimeException("Trying to initialize ServiceLogEventHandler", e);
        }
        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
        try {
            ObjectName oName = new ObjectName(ServiceLogEventHandler.class.getName()+":type=ServiceLogEventHandler");
            mbs.registerMBean(this, oName);
        } catch (Exception e) {
            logger.log(Level.WARNING,
                       "Trying to create MBean for ServiceLogEventHandler",
                       e);
        }
    }

    public void setEventHandler(EventHandler eventHandler) {
        this.eventHandler = eventHandler;
    }
    
    public void setSource(EventProducer source) {
        this.source = source;
    }

    public EventProducer getSource() {
        return source;
    }

    public void setPublishOnLevel(String level) {
        publishOnLevel = Level.parse(level);
    }

    public void setPublishOnLevel(Level level) {
        publishOnLevel = level;
    }

    public String getPublishOnLevel() {
        return publishOnLevel.toString();
    }

    public boolean addPublishableLogger(String publishableLogger) {
        return publishableLoggers.add(publishableLogger);
    }

    public boolean removePublishableLogger(String publishableLogger) {
        return publishableLogger != null && publishableLoggers.remove(publishableLogger);
    }

    public void setPublishableLoggers(Collection<String> publishableLoggers) {
        this.publishableLoggers.clear();
        this.publishableLoggers.addAll(publishableLoggers);
    }

    public Collection<String> getPublishableLoggers() {
        Collection<String> c = new ArrayList<String>();
        c.addAll(publishableLoggers);
        return c;
    }

    public void publish(LogRecord logRecord) {
        boolean publish = false;
        if(logRecord.getThrown()!=null) {
            publish = true;
        } else if(logRecord.getLevel().intValue()>=publishOnLevel.intValue() &&
                  logRecord.getLevel()!=Level.OFF) {
            for(String logger : publishableLoggers) {
                if(logRecord.getLoggerName().startsWith(logger)) {
                    publish = true;
                    break;
                }
            }
        }
        if(publish && source!=null) {
            String opStringName = null;
            String serviceName = null;
            ClassLoader cl = Thread.currentThread().getContextClassLoader();
            if(cl instanceof ServiceClassLoader) {
                Properties props = ((ServiceClassLoader)cl).getMetaData();
                opStringName = props.getProperty("opStringName");
                serviceName = props.getProperty("serviceName");
            }
            eventExecutor.execute(new ServiceLogEventTask(
                new ServiceLogEvent(source, logRecord, opStringName, serviceName, address)
            ));

        }
    }

    public void flush() {
    }

    public void close() {
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
                logger.log(Level.SEVERE, "Fire SLAThresholdEvent", e);
            }
        }
    }
}
