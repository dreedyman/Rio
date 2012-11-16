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

import org.rioproject.monitor.ProvisionMonitorEvent;
import org.rioproject.opstring.ServiceElement;
import org.rioproject.tools.ui.Constants;

/**
 * @author Dennis Reedy
 */
public class ProvisionMonitorEventNode extends RemoteServiceEventNode<ProvisionMonitorEvent> {

    public ProvisionMonitorEventNode(ProvisionMonitorEvent event) {
        super(event);
    }

    @Override
    public Throwable getThrown() {
        return null;
    }

    @Override
    public String getDescription() {
        StringBuilder builder = new StringBuilder();
        if(getEvent().getAction().equals(ProvisionMonitorEvent.Action.SERVICE_PROVISIONED)) {
            builder.append(getServiceName()).append(" instantiated on ");
            builder.append(getEvent().getServiceBeanInstance().getHostAddress());
        } else if(getEvent().getAction().equals(ProvisionMonitorEvent.Action.OPSTRING_DEPLOYED) ||
                  getEvent().getAction().equals(ProvisionMonitorEvent.Action.OPSTRING_UNDEPLOYED)) {
            for(ServiceElement service : getEvent().getOperationalString().getServices()) {
                if(builder.length()>0) {
                    builder.append(", ");
                }
                builder.append(service.getName());
            }
        } else if (getEvent().getAction().equals(ProvisionMonitorEvent.Action.SERVICE_TERMINATED)) {
            builder.append(getServiceName()).append(" on ");
            if(getEvent().getServiceBeanInstance()!=null)
                builder.append(getEvent().getServiceBeanInstance().getHostAddress()).append(" terminated");
            else
                builder.append(" [unknown] terminated");
        } else if (getEvent().getAction().equals(ProvisionMonitorEvent.Action.SERVICE_FAILED)) {
            builder.append(getServiceName()).append(" on ");
            String hostAddress = getEvent().getServiceBeanInstance()==null?"?":
                                 getEvent().getServiceBeanInstance().getHostAddress();
            builder.append(hostAddress).append(" has failed");
        } else if (getEvent().getAction().equals(ProvisionMonitorEvent.Action.EXTERNAL_SERVICE_DISCOVERED)) {
            builder.append(getServiceName()).append(" has been discovered on ");
            builder.append(getEvent().getServiceBeanInstance().getHostAddress());
        } else if (getEvent().getAction().equals(ProvisionMonitorEvent.Action.SERVICE_ELEMENT_REMOVED)) {
            builder.append(getServiceName()).append(" has been removed ");
        } else if (getEvent().getAction().equals(ProvisionMonitorEvent.Action.SERVICE_ELEMENT_ADDED)) {
            builder.append(getServiceName()).append(" has been added ");
        } else if (getEvent().getAction().equals(ProvisionMonitorEvent.Action.SERVICE_BEAN_INCREMENTED)) {
            builder.append(getServiceName()).append(" has been incremented, now: ").append(getEvent().getServiceElement().getPlanned());
        } else if (getEvent().getAction().equals(ProvisionMonitorEvent.Action.SERVICE_BEAN_DECREMENTED)) {
            builder.append(getServiceName()).append(" has been decremented, now: ").append(getEvent().getServiceElement().getPlanned());
        }
        return builder.toString();
    }

    @Override
    public String getOperationalStringName() {
        String name;
        if(getEvent().getServiceElement()!=null)
            name = getEvent().getServiceElement().getOperationalStringName();
        else
            name = getEvent().getOperationalStringName();
        return name;
    }

    @Override
    public String getServiceName() {
        String name;
        if(getEvent().getServiceElement()!=null)
            name = getEvent().getServiceElement().getName();
        else
            name = null;
        return name;
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
            value = getEvent().getAction().toString();
        } else if (column == 1) {
            value = getDescription();
        } else {
            value = Constants.DATE_FORMAT.format(getDate());
        }
        return value;
    }
}
