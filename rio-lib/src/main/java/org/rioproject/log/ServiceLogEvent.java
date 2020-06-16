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

import org.rioproject.event.EventDescriptor;
import org.rioproject.event.RemoteServiceEvent;

import java.io.Serializable;
import java.net.InetAddress;
import java.util.logging.LogRecord;

/**
 * A remote event of a service's log record, typically indicating an exception
 * of interest has been thrown.
 */
public class ServiceLogEvent extends RemoteServiceEvent implements Serializable {
    static final long serialVersionUID = 1L;
    private LogRecord logRecord;
    private String opStringName;
    private String serviceName;
    private InetAddress address;
    public static final long ID=90210L;

    /**
     * Create a new <code>ServiceLogEvent</code>
     *
     * @param source event source
     * @param logRecord the
     */
    public ServiceLogEvent(Object source, LogRecord logRecord) {
        super(source);
        this.logRecord = logRecord;
    }

    public ServiceLogEvent(Object source,
                           LogRecord logRecord,
                           String opStringName,
                           String serviceName,
                           InetAddress address) {
        super(source);
        this.logRecord = logRecord;
        this.opStringName = opStringName;
        this.serviceName = serviceName;
        this.address = address;
    }

    public String getOpStringName() {
        return opStringName;
    }

    public String getServiceName() {
        return serviceName;
    }

    public InetAddress getAddress() {
        return address;
    }

    public LogRecord getLogRecord() {
        return logRecord;
    }

    /**
     * Helper method to return the EventDescriptor for this event
     *
     * @return The EventDescriptor for this event
     */
    public static EventDescriptor getEventDescriptor(){
        return(new EventDescriptor(ServiceLogEvent.class, ID));
    }
}
