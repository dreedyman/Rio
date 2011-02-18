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

import org.rioproject.event.RemoteServiceEvent;

import java.io.Serializable;

/**
 * The ThresholdEvent extends RemoteServiceEvent allowing for remote
 * notification of a Threshold being crossed
 */
public class ThresholdEvent extends RemoteServiceEvent implements Serializable {
    static final long serialVersionUID = 1L;
    public static final long ID = 1000000000;
    /** Holds value of property Calculable */
    private Calculable calculable;
    /** Holds value of property ThresholdValue. */
    private ThresholdValues thresholdValues;
    /** Indicates a threshold that has been breached */
    public static final int BREACHED = 0;
    /** Indicates a threshold that has been cleared */
    public static final int CLEARED = 1;
    /** The type of the ThresholdEvent, breached or cleared */
    private int type;
    /** Optional detail describing the ThresholdEvent */
    private String detail;

    /**
     * Creates new ThresholdEvent
     * 
     * @param source The event source
     */
    public ThresholdEvent(Object source) {
        super(source);
    }

    /**
     * Creates new ThresholdEvent
     * 
     * @param source The event source
     * @param calculable Value of property calculable
     * @param thresholdValues Value of property thresholdValues
     * @param type Whether the threshold has been breached or cleared
     */
    public ThresholdEvent(Object source, 
                          Calculable calculable,
                          ThresholdValues thresholdValues,
                          int type) {
        this(source, calculable, thresholdValues, type, null);
    }

    /**
     * Creates new ThresholdEvent
     *
     * @param source The event source
     * @param calculable Value of property calculable
     * @param thresholdValues Value of property thresholdValues
     * @param type Whether the threshold has been breached or cleared
     * @param detail Optional details of the notification (may be null)
     */
    public ThresholdEvent(Object source,
                          Calculable calculable,
                          ThresholdValues thresholdValues,
                          int type,
                          String detail) {
        super(source);
        setCalculable(calculable);
        setThresholdValues(thresholdValues);
        setType(type);
        setDetail(detail);
    }

    /**
     * Getter for property type
     * 
     * @return type Whether the threshold has been breached or cleared
     */
    public int getType() {
        return (type);
    }

    /**
     * Setter for property type
     * 
     * @param type Whether the threshold has been breached or cleared
     */
    public void setType(int type) {
        this.type = type;
    }

    /**
     * Getter for property calculable
     * 
     * @return Value of property calculable
     */
    public Calculable getCalculable() {
        return (calculable);
    }

    /**
     * Setter for property calculable
     * 
     * @param calculable New value of property calculable
     */
    public void setCalculable(Calculable calculable) {
        this.calculable = calculable;
    }

    /**
     * Getter for property thresholdValues
     * 
     * @return Value of property thresholdValues
     */
    public ThresholdValues getThresholdValues() {
        return (thresholdValues);
    }

    /**
     * Setter for property thresholdValues
     * 
     * @param thresholdValues New value of property thresholdValues
     */
    public void setThresholdValues(ThresholdValues thresholdValues) {
        this.thresholdValues = thresholdValues;
    }

    public String getDetail() {
        return detail;
    }

    public void setDetail(String detail) {
        this.detail = detail;
    }
}
