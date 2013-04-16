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
package org.rioproject.logging.logback;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.ThrowableProxy;
import ch.qos.logback.core.AppenderBase;
import org.rioproject.logging.ServiceLogEventHandler;
import org.rioproject.logging.ServiceLogEventPublisher;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.LogRecord;

/**
 * An implementation of the {@link org.rioproject.logging.ServiceLogEventHandler} for Logback.
 *
 * @author Dennis Reedy
 */
@SuppressWarnings("unused")
public class LogbackServiceLogEventHandler extends AppenderBase<ILoggingEvent> implements ServiceLogEventHandler {
    private Level publishOnLevel = Level.ERROR;
    private final Collection<String> publishableLoggers = Collections.synchronizedCollection(new ArrayList<String>());
    private ServiceLogEventPublisher eventPublisher;
    private final BlockingQueue<ILoggingEvent> eventQ = new LinkedBlockingQueue<ILoggingEvent>();

    public LogbackServiceLogEventHandler() {
        Executor eventExecutor = Executors.newSingleThreadExecutor();
        eventExecutor.execute(new CheckThenPublish());
        start();
    }

    @Override
    public void setServiceLogEventPublisher(ServiceLogEventPublisher publisher) {
        this.eventPublisher = publisher;
    }

    @Override
    public ServiceLogEventPublisher getServiceLogEventPublisher() {
        return eventPublisher;
    }

    @Override
    public void setPublishOnLevel(String sLevel) {
        this.publishOnLevel = Level.toLevel(sLevel);
    }

    @Override
    public void addPublishableLogger(String publishableLogger) {
        publishableLoggers.add(publishableLogger);
    }

    @Override
    protected void append(ILoggingEvent loggingEvent) {
        if(eventPublisher==null)
            return;
        try {
            eventQ.put(loggingEvent);
        } catch (InterruptedException e) {
            System.err.println(String.format("[%d] %s interrupted",
                                             Thread.currentThread().getId(),
                                             LogbackServiceLogEventHandler.class.getName()));
        }
    }

    private String getJULLevel(Level level) {
        String julLevel;
        switch (level.levelInt) {
            case Level.TRACE_INT:
                julLevel = java.util.logging.Level.FINEST.getName();
                break;
            case Level.DEBUG_INT:
                julLevel = java.util.logging.Level.FINE.getName();
                break;
            case Level.WARN_INT:
                julLevel = java.util.logging.Level.WARNING.getName();
                break;
            case Level.ERROR_INT:
                julLevel = java.util.logging.Level.SEVERE.getName();
                break;
             default:
                 julLevel = java.util.logging.Level.INFO.getName();
        }
        return julLevel;
    }

    class CheckThenPublish implements Runnable {

        public void run() {
            while(!Thread.currentThread().isInterrupted()) {
                ILoggingEvent loggingEvent;
                try {
                    loggingEvent = eventQ.take();
                } catch (InterruptedException e) {
                    break;
                }
                if(loggingEvent!=null) {
                    boolean publish = false;
                    if(loggingEvent.getThrowableProxy()!=null) {
                        publish = true;
                    } else if(loggingEvent.getLevel().levelInt>=publishOnLevel.levelInt &&
                              loggingEvent.getLevel()!= Level.OFF) {
                        for(String logger : publishableLoggers) {
                            if(loggingEvent.getLoggerName().startsWith(logger)) {
                                publish = true;
                                break;
                            }
                        }
                    }
                    if(publish) {
                        LogRecord logRecord = new LogRecord(java.util.logging.Level.parse(getJULLevel(loggingEvent.getLevel())),
                                                            loggingEvent.getFormattedMessage());
                        logRecord.setMillis(loggingEvent.getTimeStamp());
                        if(loggingEvent.getThrowableProxy()!=null) {
                            if(loggingEvent.getThrowableProxy() instanceof ThrowableProxy) {
                                logRecord.setThrown((((ThrowableProxy) loggingEvent.getThrowableProxy()).getThrowable()));
                            }
                        }
                        eventPublisher.publish(logRecord);
                    }
                }
            }
        }
    }
}
