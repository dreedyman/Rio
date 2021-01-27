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

import org.rioproject.sla.SLA;
import org.rioproject.sla.SLAThresholdEvent;
import org.rioproject.tools.ui.Constants;

import java.text.NumberFormat;

/**
 * @author Dennis Reedy
 */
public class SLAThresholdEventNode extends RemoteServiceEventNode<SLAThresholdEvent> {
    private final NumberFormat percentFormatter;

    public SLAThresholdEventNode(SLAThresholdEvent event) {
        super(event);
        percentFormatter = NumberFormat.getPercentInstance();
        /* Display 3 digits of precision */
        percentFormatter.setMaximumFractionDigits(3);
    }

    @Override
    public Throwable getThrown() {
        return null;
    }

    @Override
    public String getDescription() {
        StringBuilder builder = new StringBuilder();
        SLA sla = getEvent().getSLA();
        builder.append(getEvent().getServiceElement().getName()).append(" on ");
        builder.append(getEvent().getHostAddress()).append(" SLA ");
        builder.append("\"").append(sla.getIdentifier()).append("\"").append(" ");
        builder.append(getStatus().toLowerCase()).append(", ");
        builder.append("value: ");
        double value = getEvent().getCalculable().getValue();
        if(value<1)
            builder.append(formatPercent(value));
        else
            builder.append(value);
        return builder.toString();
    }

    private String formatPercent(final Double value) {
        if (value != null && !Double.isNaN(value))
            return (percentFormatter.format(value.doubleValue()));
        return ("?");
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
        return getEvent().getThresholdType().name();
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
            value = getStatus();
        } else if (column == 1) {
            value = getDescription();
        } else {
            value = Constants.DATE_FORMAT.format(getDate());
        }
        return value;
    }
}

