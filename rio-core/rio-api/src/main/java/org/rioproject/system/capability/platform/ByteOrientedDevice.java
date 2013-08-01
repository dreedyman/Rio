/*
 * Copyright 2008 the original author or authors.
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
package org.rioproject.system.capability.platform;

import org.rioproject.deploy.SystemComponent;
import org.rioproject.system.capability.PlatformCapability;

import java.util.Map;

/**
 * Represents a capability that has a capacity thats measured in bytes
 *
 * @author Dennis Reedy
 */
public class ByteOrientedDevice extends PlatformCapability {
    @SuppressWarnings("unused")
    static final long serialVersionUID = 1L;
    /** Available resource units (in bytes) */
    public final static String AVAILABLE = "Available";
    /** Resource capacity (in bytes) */
    public final static String CAPACITY = "Capacity";
    /** Kilobytes */
    public static final double KB = 1024;
    /** Megabytes */
    public static final double MB = Math.pow(KB, 2);
    /** Gigabytes */
    public static final double GB = Math.pow(KB, 3);
    /** Terabytes */
    public static final double TB = Math.pow(KB, 4);

    /**
     * Override parents define method to create values for capacity and
     * available
     */
    @Override
    public void define(String key, Object value) {
        if(key == null || value == null)
            return;

        if(key.equals(CAPACITY)) {
            try {
                Double dCap = makeDouble(value);
                capabilities.put(CAPACITY, dCap);
            } catch(NumberFormatException e) {
                System.out.println("Bad value for Capacity");
                e.printStackTrace();
            }
        } else if(key.equals(AVAILABLE)) {
            try {
                Double dAvail = makeDouble(value);
                capabilities.put(AVAILABLE, dAvail);
            } catch(NumberFormatException e) {
                System.out.println("Bad value for Available");
                e.printStackTrace();
            }
        } else {
            super.define(key, value);
        }
    }

    /**
     * Override supports to ensure that requirements are supported
     *
     * @param requirement The requirement to test
     * @return True if they are, false if not
     *
     * @see org.rioproject.system.capability.PlatformCapability#supports
     */
    @Override
    public boolean supports(SystemComponent requirement) {
        boolean supports = hasBasicSupport(requirement.getName(),
                                           requirement.getClassName());
        if(supports) {
            Map<String, Object> attributes = requirement.getAttributes();
            for (Map.Entry<String, Object> entry : attributes.entrySet()) {
                String key = entry.getKey();
                if (entry.getValue() == null)
                    continue;
                if (key.equals(CAPACITY)) {
                    Double dCap = (Double) getValue(CAPACITY);
                    if (dCap != null) {
                        supports = evaluate(entry.getValue(), dCap);
                    }
                    break;
                }
                if (key.equals(AVAILABLE)) {
                    Double dAvail = (Double) getValue(AVAILABLE);
                    if (dAvail != null) {
                        supports = evaluate(entry.getValue(), dAvail);
                    }
                    break;
                }
            }
        }        

        return (supports?supports:super.supports(requirement));
    }

    /**
     * Evaluate the input against a measures amount. Based on the
     *
     * @param input The value of either the CAPACITY or the AVAILABLE key.
     * @param val The measured value, either the total or the available property
     *
     * @return True if the amount requested can be supported, or false if not
     */
    protected boolean evaluate(Object input, double val) {
        if(input instanceof String) {
            return (evaluate((String)input, val));
        }
        if(input instanceof Double) {
            Double requestedSize = (Double)input;
            return (requestedSize < val);
        }
        return false;
    }

    /**
     * Evaluate the input against a measures amount.
     *
     * @param input The value of either the CAPACITY or the AVAILABLE key. Must
     * end in 'm' or 'M' for megabytes, 'k' or 'K' for kilobytes, 'g' or 'G'
     * for gigabytes, and 't' or 'T' for terabytes
     * @param val The measured value, either the total or the available
     * property
     *
     * @return True if the amount requested can be supported, or false if not
     */
    protected boolean evaluate(String input, double val) {
        boolean supports = true;
        double d = val;
        if(input.endsWith("k") || input.endsWith("K")) {
            d = val/KB;
        } else if(input.endsWith("m") || input.endsWith("M")) {
            d = val/MB;
        } else if(input.endsWith("g") || input.endsWith("G")) {
            d = val/GB;
        } else if(input.endsWith("t") || input.endsWith("T")) {
            d = val/TB;
        } else {
            supports = false;
        }
        if(supports) {
            try {
                String s = input.substring(0, input.length()-1);
                double requestedSize = Double.parseDouble(s);
                supports = (requestedSize < d);
            } catch(NumberFormatException e) {
                supports = false;
            }
        }
        return (supports);
    }

    Double makeDouble(Object input) {
        if(input instanceof String) {
            return makeDouble((String)input);
        }
        return (Double)input;
    }

    Double makeDouble(String input) {
        Double d;
        if(input.endsWith("k") || input.endsWith("K")) {
            String s = input.substring(0, input.length()-1);
            Double base = Double.parseDouble(s);
            d = base * KB;
        } else if(input.endsWith("m") || input.endsWith("M")) {
            String s = input.substring(0, input.length()-1);
            Double base = Double.parseDouble(s);
            d = base * MB;
        } else if(input.endsWith("g") || input.endsWith("G")) {
            String s = input.substring(0, input.length()-1);
            Double base = Double.parseDouble(s);
            d = base * GB;
        } else if(input.endsWith("t") || input.endsWith("T")) {
            String s = input.substring(0, input.length()-1);
            Double base = Double.parseDouble(s);
            d = base * TB;
        } else {
            d = Double.parseDouble(input);
        }
        return d;
    }
}
