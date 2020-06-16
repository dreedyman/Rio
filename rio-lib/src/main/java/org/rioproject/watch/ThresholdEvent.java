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
package org.rioproject.watch;

import org.rioproject.event.RemoteServiceEvent;

import java.io.*;

/**
 * The ThresholdEvent extends RemoteServiceEvent allowing for remote
 * notification of a Threshold being crossed.
 *
 * @author Dennis Reedy
 */
public class ThresholdEvent extends RemoteServiceEvent implements Serializable {
    private static final long serialVersionUID = 1L;
    public static final long ID = 1000000000;
    /** Holds value of property Calculable */
    private Calculable calculable;
    /** Holds value of property ThresholdValue. */
    private ThresholdValues thresholdValues;
    /** The type of the ThresholdEvent, breached or cleared */
    private ThresholdType thresholdType;
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
                          ThresholdType type) {
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
                          ThresholdType type,
                          String detail) {
        super(source);
        this.calculable = calculable;
        this.thresholdValues = thresholdValues;
        this.thresholdType = type;
        this.detail = detail;
    }

    /**
     * Getter for property type
     * 
     * @return type Whether the threshold has been breached or cleared
     */
    public ThresholdType getThresholdType() {
        return thresholdType;
    }

    /**
     * Setter for property type
     *
     * @param type Whether the threshold has been breached or cleared
     */
    public void setThresholdType(ThresholdType type) {
        this.thresholdType = type;
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

    private void readObject(final ObjectInputStream in) throws IOException, ClassNotFoundException {
        ObjectInputStream.GetField fields = in.readFields();
        for(ObjectStreamField field : fields.getObjectStreamClass().getFields()) {
            if(field.getName().equals("calculable")) {
                this.calculable = (Calculable) fields.get(field.getName(), null);
            }
            if(field.getName().equals("detail")) {
                this.detail = (String) fields.get(field.getName(), null);
            }
            if(field.getName().equals("type")) {
                int type = fields.get("type", 0);
                /** Conversion from an older version of ThresholdEvent. A type of 0 is BREACHED, 1 is CLEARED */
                if(type==0) {
                    this.thresholdType = ThresholdType.BREACHED;
                } else {
                    this.thresholdType = ThresholdType.CLEARED;
                }
            }
            if(field.getName().equals("thresholdType")) {
                this.thresholdType = (ThresholdType) fields.get(field.getName(), null);
            }
            if(field.getName().equals("thresholdValues")) {
                this.thresholdValues = (ThresholdValues) fields.get(field.getName(), null);
            }
        }
    }
}
