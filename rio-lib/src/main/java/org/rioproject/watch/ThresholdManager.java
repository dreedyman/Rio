/*
 * Copyright 2008 the original author or authors.
 * Copyright 2005 Sun Microsystems, Inc.
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
package org.rioproject.watch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;
import java.util.List;

/**
 * The ThresholdManager is the keeper of ThresholdValues and determines when
 * Calculable items being recorded have crossed any thresholds. A
 * ThresholdManager provides threshold management processing, providing specific
 * behavior that may be used to remedy the situation where a threshold has been
 * crossed
 */
public abstract class ThresholdManager {
    protected final transient List<ThresholdListener> thresholdListeners = new LinkedList<ThresholdListener>();
    /** Holds value of property thresholdValues */
    protected ThresholdValues thresholdValues = new ThresholdValues();
    static Logger logger = LoggerFactory.getLogger(ThresholdManager.class.getName());

    /**
     * Check the threshold and determine if any action needs to occur
     *
     * @param calculable The Calculable to check
     */
    public abstract void checkThreshold(Calculable calculable);

    /**
     * Get the identifier for the  ThresholdManager
     */
    protected abstract String getID();

    /**
     * Get the type of threshold that has been crossed
     *
     * @return The type of threshold that has been crossed
     */
    public abstract boolean getThresholdCrossed();

    /**
     * Getter for property thresholdValues.
     * 
     * @return Value of property thresholdValues.
     */
    public ThresholdValues getThresholdValues() {
        return (thresholdValues);
    }

    /**
     * Setter for property thresholdValues.
     * 
     * @param thresholdValues New value of property thresholdValues.
     */
    public void setThresholdValues(ThresholdValues thresholdValues) {
        this.thresholdValues = thresholdValues;
        logger.trace("{} Set ThresholdValues, low={}, high={}",
                     getID(), thresholdValues.getLowThreshold(), thresholdValues.getHighThreshold());
    }

    /**
     * Notify all ThresholdListeners of a threshold being crossed
     * 
     * @param calculable The Calculable record
     * @param type The type of threshold, breached or cleared
     */
    protected void notifyListeners(Calculable calculable, ThresholdType type) {
        ThresholdListener[] tListeners = getThresholdListeners();
        logger.trace("{} Notify ThresholdListeners, number to notify: {}", getID(), tListeners.length);
        ThresholdValues thresholds = null;
        try {
            thresholds = (ThresholdValues)thresholdValues.clone();
        } catch (CloneNotSupportedException e) {
            //
        }

        for (ThresholdListener tListener : tListeners) {
            tListener.notify(calculable, thresholds, type);
        }
    }

    /**
     * Add a Threshold listener
     * 
     * @param listener the ThresholdListener to add
     */
    public void addThresholdListener(ThresholdListener listener) {
        synchronized(thresholdListeners) {
            if(!thresholdListeners.contains(listener))
                thresholdListeners.add(listener);
            logger.trace("{} Added a ThresholdListener, number now: {}", getID(), thresholdListeners.size());
        }
    }

    /**
     * Remove a ThresholdListener
     * 
     * @param listener the ThresholdListener to remove
     */
    public void removeThresholdListener(ThresholdListener listener) {
        synchronized(thresholdListeners) {
            thresholdListeners.remove(listener);
            logger.trace("{} Removed a ThresholdListener, number now: {}", getID(), thresholdListeners.size());
        }
    }

    /**
     * Get all registered ThresholdListener instances
     * 
     * @return Array of ThresholdListener[]. A new array will be allocated each
     * time this method is called. If there are no registered ThresholdListener
     * instances, a zero-length array will be returned
     */
    public ThresholdListener[] getThresholdListeners() {
        ThresholdListener[] tListeners;
        synchronized(thresholdListeners) {
            tListeners = thresholdListeners.toArray(new ThresholdListener[thresholdListeners.size()]);
        }
        return (tListeners);
    }

    /**
     * Remove all registered ThresholdListener instances
     */
    public void clear() {
        synchronized(thresholdListeners) {
            thresholdListeners.clear();
        }
    }
}
