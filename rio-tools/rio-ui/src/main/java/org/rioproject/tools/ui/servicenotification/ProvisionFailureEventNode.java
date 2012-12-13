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

import org.rioproject.monitor.ProvisionFailureEvent;
import org.rioproject.tools.ui.Constants;

/**
 * @author Dennis Reedy
 */
public class ProvisionFailureEventNode extends RemoteServiceEventNode<ProvisionFailureEvent> {
    private String status;

    public ProvisionFailureEventNode(ProvisionFailureEvent event) {
        super(event);
        status = "Pending";
    }

    @Override
    public Throwable getThrown() {
        return getEvent().getThrowable();
    }

    @Override
    public String getDescription() {
        StringBuilder builder = new StringBuilder();
        for(String reason : getEvent().getFailureReasons()) {
            if(builder.length()>0)
                builder.append("\n    ");
            builder.append(reason);
        }
        return builder.toString();
    }

    @Override
    public String getOperationalStringName() {
        return getEvent().getServiceElement().getOperationalStringName();
    }

    @Override
    public String getServiceName() {
        return getEvent().getServiceElement().getName();
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return getServiceName();
    }

    @Override
    public int getColumnCount() {
        return 3;
    }

    @Override
    public Object getValueAt(int column) {
        String value;
        if (column == 0) {
            value = "PROVISION_FAILURE";
        } else if (column == 1) {
            value = getDescription();
        } else {
            value = Constants.DATE_FORMAT.format(getDate());
        }
        return value;
    }
}
