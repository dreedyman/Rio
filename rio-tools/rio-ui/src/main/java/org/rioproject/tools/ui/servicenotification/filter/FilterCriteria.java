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
    private final java.util.List<String> contains = new ArrayList<String>();

    public void addEventType(String eventType) {
        eventTypes.add(eventType);
    }

    public void addContains(String contains) {
        this.contains.add(contains);
    }

    public Collection<String> getEventTypes() {
        return eventTypes;
    }

    public List<String> getContains() {
        return contains;
    }

    @Override
    public String toString() {
        return "FilterCriteria " +
               "eventTypes=" + eventTypes +
               ", contains=" + contains;
    }
}
