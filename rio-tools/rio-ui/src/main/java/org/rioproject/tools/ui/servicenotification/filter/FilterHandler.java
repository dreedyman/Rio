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
package org.rioproject.tools.ui.servicenotification.filter;

import org.jdesktop.swingx.treetable.AbstractMutableTreeTableNode;

/**
 * Provides the support to determine if {@code RemoteServiceEventNode}s should be filtered.
 *
 * @author Dennis Reedy
 */
public class FilterHandler {

    public boolean include(FilterCriteria filterCriteria, AbstractMutableTreeTableNode node) {
        boolean includeType = filterCriteria.getEventTypes().isEmpty();
        String eventType = (String) node.getValueAt(0);
        for (String eventFilter : filterCriteria.getEventTypes()) {
            if (eventFilter.endsWith("*")) {
                String filterValue = eventFilter.substring(0, eventFilter.length() - 1);
                includeType = eventType.contains(filterValue);
            } else {
                includeType = eventType.equals(eventFilter);
            }
            if(includeType) {
                break;
            }
        }
        boolean includeDescription = filterCriteria.getDescriptions().isEmpty();
        for(String descriptionFilter : filterCriteria.getDescriptions()) {
            String descriptionValue = (String) node.getValueAt(1);
            if (descriptionFilter.endsWith("*")) {
                String filterValue = descriptionFilter.substring(0, descriptionFilter.length() - 1);
                includeDescription = descriptionValue.contains(filterValue);
            } else {
                includeDescription = descriptionValue.equals(descriptionFilter);
            }
            if(includeDescription) {
                break;
            }
        }
        return includeType && includeDescription;
    }
}
