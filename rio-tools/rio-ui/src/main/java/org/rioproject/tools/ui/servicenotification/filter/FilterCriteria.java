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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author Dennis Reedy
 */
public class FilterCriteria {
    private final java.util.List<String> eventTypes = new ArrayList<String>();
    private final java.util.List<String> descriptions = new ArrayList<String>();

    public void addEventType(String eventType) {
        eventTypes.add(eventType);
    }

    public void addDescription(String contains) {
        this.descriptions.add(contains);
    }

    public Collection<String> getEventTypes() {
        return eventTypes;
    }

    public List<String> getDescriptions() {
        return descriptions;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        FilterCriteria that = (FilterCriteria) o;

        return !(descriptions != null ? !descriptions.equals(that.descriptions) : that.descriptions != null) &&
               !(eventTypes != null ? !eventTypes.equals(that.eventTypes) : that.eventTypes != null);

    }

    @Override
    public int hashCode() {
        int result = eventTypes != null ? eventTypes.hashCode() : 0;
        result = 31 * result + (descriptions != null ? descriptions.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "FilterCriteria " +
               "eventTypes=" + eventTypes +
               ", descriptions=" + descriptions;
    }
}
