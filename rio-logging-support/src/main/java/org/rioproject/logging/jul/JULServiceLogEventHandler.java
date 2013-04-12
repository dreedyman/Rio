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
package org.rioproject.logging.jul;

import org.rioproject.logging.ServiceLogEventHandler;
import org.rioproject.logging.ServiceLogEventPublisher;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;

/**
 * A {@link java.util.logging.Handler} that publishes {@code ServiceLogEvent}s
 * to interested consumers when a {@link java.util.logging.LogRecord} of
 * interest is logged.
 *
 * <p>A {@link java.util.logging.LogRecord} of interest is one that contains an
 * exception.
 *
 * <p>The <code>JULServiceLogEventHandler</code> will also publish
 * {@code ServiceLogEvent}s to interested consumers if the <code>LogRecord</code>
 * has a level that is greater than or equal to the <code>JULServiceLogEventHandler</code>'s
 * configured {@link java.util.logging.Level} property (the default for this is
 * {@link java.util.logging.Level#SEVERE}), and whose name has been configured as a
 * <i>publishable</i> logger. Matching semantics for a <i>publishable</i> logger
 * name (as obtained from {@link java.util.logging.LogRecord#getLoggerName()}
 * starts with a configured <i>publishable</i> logger name.  
 */
@SuppressWarnings("PMD.AvoidThrowingRawExceptionTypes")
public class JULServiceLogEventHandler extends Handler implements ServiceLogEventHandler {
    private Level publishOnLevel = Level.SEVERE;
    private final Collection<String> publishableLoggers = Collections.synchronizedCollection(new ArrayList<String>());
    private ServiceLogEventPublisher eventPublisher;

    @Override
    public void setServiceLogEventPublisher(ServiceLogEventPublisher publisher) {
        this.eventPublisher = publisher;
    }

    @Override
    public ServiceLogEventPublisher getServiceLogEventPublisher() {
        return eventPublisher;
    }

    @Override
    public synchronized void setLevel(Level newLevel) throws SecurityException {
        super.setLevel(newLevel);
        setPublishOnLevel(newLevel.toString());
    }

    @Override
    public void setPublishOnLevel(String level) {
        publishOnLevel = Level.parse(level);
    }

    @Override
    public void addPublishableLogger(String publishableLogger) {
        publishableLoggers.add(publishableLogger);
    }

    public void publish(LogRecord logRecord) {
        if(eventPublisher==null)
            return;
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
        if(publish) {
            eventPublisher.publish(logRecord);
        }
    }

    public void flush() {
    }

    public void close() {
    }
}
