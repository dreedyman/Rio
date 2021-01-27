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
package org.rioproject.tools.ui.servicenotification;

import org.rioproject.log.ServiceLogEvent;
import org.rioproject.tools.ui.Constants;

import java.util.logging.LogRecord;

/**
 * @author Dennis Reedy
 */
public class ServiceLogEventNode extends RemoteServiceEventNode<ServiceLogEvent> {

    public ServiceLogEventNode(ServiceLogEvent event) {
        super(event);
    }

    @Override
    public Throwable getThrown() {
        return getEvent().getLogRecord().getThrown();
    }

    @Override
    public String getDescription() {
        LogRecord logRecord = getEvent().getLogRecord();
        return logRecord.getMessage();
    }

    @Override
    public String getOperationalStringName() {
        return getEvent().getOpStringName();
    }

    @Override
    public String getServiceName() {
        return getEvent().getServiceName() == null ? "" : getEvent().getServiceName();
    }

    @Override
    public String toString() {
        return getServiceName();
    }

    @Override
    public Object getValueAt(int column) {
        String value;
        if (column == 0) {
            //value = getServiceName();
            value = getEvent().getLogRecord().getLevel().toString()+" "+getServiceName();
        } else if (column == 1) {
            StringBuilder sb = new StringBuilder();
            sb.append(getDescription());
            sb.append(". ");
            Throwable t = getThrown();
            if (t != null) {
                if (t.getCause() != null) {
                    t = t.getCause();
                }
                sb.append(t).append(" Exception raised.");
            }
            value = sb.toString();
        } else {
            value = Constants.DATE_FORMAT.format(getDate());
        }
        return value;
    }

    @Override
    public int getColumnCount() {
        return 3;
    }

    @Override
    public boolean getAllowsChildren() {
        return false;
    }

    @Override
    public boolean isLeaf() {
        return true;
    }
}
