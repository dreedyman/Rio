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
package org.rioproject.start;

import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Dennis Reedy
 */
public class MemoryAppender extends AppenderBase<ILoggingEvent> {
    private PatternLayoutEncoder encoder;
    private final List<ILoggingEvent> loggingEvents = new ArrayList<>();

    public void reset() {
        synchronized (loggingEvents) {
            loggingEvents.clear();
        }
    }

    public List<ILoggingEvent> getLogMessages() {
        synchronized (loggingEvents) {
            return new ArrayList<>(loggingEvents);
        }
    }

    public PatternLayoutEncoder getEncoder() {
        return encoder;
    }

    public void setEncoder(PatternLayoutEncoder encoder) {
        this.encoder = encoder;
    }


    @Override public void start() {
        super.start();
        try {
            encoder.init(System.out);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void append(ILoggingEvent event) {
        try {
            encoder.doEncode(event);
            synchronized (loggingEvents) {
                loggingEvents.add(event);
                //System.out.println(event.getFormattedMessage());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
