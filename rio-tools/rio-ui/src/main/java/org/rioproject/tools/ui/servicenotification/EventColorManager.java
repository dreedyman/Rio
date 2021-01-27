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

import java.awt.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Utility to get colors for event types.
 *
 * @author Dennis Reedy
 */
public class EventColorManager {
    private final Color normalColor = new Color(215,225, 205);
    private final Color warningColor = new Color(255, 245, 205);
    private final Color minorColor = new Color(255, 235, 205);
    private final Color criticalColor = new Color(245, 205, 205);
    private final Color indeterminateColor = new Color(235, 235, 205);
    private final Map<String, Color> eventColorMap = new HashMap<String, Color>();

    public EventColorManager() {

        eventColorMap.put("PROVISION_FAILURE", criticalColor);
        eventColorMap.put("BREACHED", criticalColor);
        eventColorMap.put("CLEARED", indeterminateColor);

        for(ProvisionMonitorEvent.Action action : ProvisionMonitorEvent.Action.values()) {
            String value = action.name();
            if(value.equals("SERVICE_BEAN_DECREMENTED") || value.equals("SERVICE_BEAN_INCREMENTED")) {
                eventColorMap.put(value, normalColor);
            } else if((value.equals("OPSTRING_UNDEPLOYED") || value.equals("SERVICE_TERMINATED"))) {
                eventColorMap.put(value, minorColor);
            } else if(value.equals("SERVICE_TERMINATED")) {
                eventColorMap.put(value, warningColor);
            } else if(value.equals("SERVICE_FAILED")) {
                eventColorMap.put(value, criticalColor);
            } else {
                eventColorMap.put(value, indeterminateColor);
            }
        }
    }

    /**
     * Get the event type name and it's color
     *
     * @return A {@code Map} of event type names and the event type's color
     */
    public Map<String, Color> getEventColorMap() {
        return eventColorMap;
    }

    /**
     * Get the color name and it's color
     *
     * @return A {@code Map} od color names to color
     */
    public Map<String, Color> getColorMap() {
        Map<String, Color> colorMap = new HashMap<String, Color>();
        colorMap.put("Normal", normalColor);
        colorMap.put("Warning", warningColor);
        colorMap.put("Minor", minorColor);
        colorMap.put("Critical", criticalColor);
        colorMap.put("Indeterminate", indeterminateColor);
        return colorMap;
    }

    public String getColorName(Color color) {
        String name;
        if(color.equals(normalColor)) {
            name = "Normal";
        } else if(color.equals(warningColor)) {
            name = "Warning";
        } else if(color.equals(minorColor)) {
            name = "Minor";
        } else if(color.equals(criticalColor)) {
            name = "Critical";
        } else {
            name = "Indeterminate";
        }
        return name;
    }

    public Color getEventColor(String eventName) {
        return eventColorMap.get(eventName);
    }

    public boolean isNormal(String eventName) {
        return isMatch(eventName, normalColor);
    }

    public boolean isWarning(String eventName) {
        return isMatch(eventName, warningColor);
    }

    public boolean isMinor(String eventName) {
        return isMatch(eventName, minorColor);
    }

    public boolean isCritical(String eventName) {
        return isMatch(eventName, criticalColor);
    }

    public boolean isIndeterminate(String eventName) {
        return isMatch(eventName, indeterminateColor);
    }

    private boolean isMatch(String eventName, Color color) {
        boolean matched = false;
        for(Map.Entry<String, Color> entry : eventColorMap.entrySet()) {
            if(entry.getKey().equals(eventName)) {
                matched = entry.getValue().equals(color);
                break;
            }
        }
        return matched;
    }
}
