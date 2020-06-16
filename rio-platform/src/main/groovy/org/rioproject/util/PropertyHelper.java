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
package org.rioproject.util;

import java.util.Properties;

/**
 * Property helper.
 *
 * @author Dennis Reedy
 */
public final class PropertyHelper {
    public static final String[] PARSETIME = {"${", "}"};
    public static final String[] RUNTIME = {"$[", "]"};

    private PropertyHelper() {}

    /**
     * Expand any properties in the String. Properties are declared with
     * the pattern of : <code>${property}</code>
     *
     * @param arg The string with properties to expand, must not be null
     *
     * @return If the string has properties declared (in the form
     * <tt>${property}</tt>), return a formatted string with the
     * properties expanded. If there are no property elements declared,
     * return the original string.
     *
     * @throws IllegalArgumentException if any of the arguments are
     * <code>null</code>
     */
    public static String expandProperties(final String arg) {
        return expandProperties(arg, PARSETIME);
    }

    /**
     * Expand any properties in the String. Properties are declared with
     * the pattern of : <code><start-delim>property<end-delim></code>
     *
     * @param arg The string with properties to expand, must not be null
     * @param delimeters The string patterns indicating the starting and
     * ending property delimiter
     *
     * @return If the string has properties declared (in the form
     * <start-delim>property<end-delim>), return a formatted string with the
     * properties expanded. If there are no property elements declared,
     * return the original string.
     *
     * @throws IllegalArgumentException if any of the arguments are
     * <code>null</code> or if the delimeters argument does not
     * contain at least 2 entries, or if a property value cannot be obtained
     */
    public static String expandProperties(final String arg, final String[] delimeters) {
        if(arg ==null)
            throw new IllegalArgumentException("arg is null");
        if(delimeters == null)
            throw new IllegalArgumentException("delimeters is null");
        if(delimeters.length <2)
            throw new IllegalArgumentException("delimeters bad size");
        String start=delimeters[0];
        String end = delimeters[1];
        int s = 0;
        int e  ;
        StringBuilder result = new StringBuilder();
        while((e = arg.indexOf(start, s)) >= 0) {
            String str = arg.substring(e+start.length());
            int n = str.indexOf(end);
            if(n != -1) {
                result.append(arg.substring(s, e));
                String prop = str.substring(0, n);
                if(prop.equals("/")) {
                    result.append(java.io.File.separator);
                } else if(prop.equals(":")) {
                    result.append(java.io.File.pathSeparator);
                } else {
                    String value = System.getProperty(prop);
                    if(value == null)
                        throw new IllegalArgumentException("property "+
                                                           "["+prop+"] "+
                                                           "not declared");
                    result.append(value);
                }
                s = e+start.length()+prop.length()+end.length();
            } else {
                result.append(start);
                s = e+start.length();
            }
        }
        result.append(arg.substring(s));
        return (result.toString());
    }

    /**
     * Expand any properties in the String. Properties are declared with
     * the pattern of : <code><start-delim>property<end-delim></code>
     *
     * @param arg The string with properties to expand, must not be null
     * @param properties A {@code Properties} object to use for property replacements, must not be null.
     *
     * @return If the string has properties declared (in the form
     * <start-delim>property<end-delim>), return a formatted string with the
     * properties expanded. If there are no property elements declared,
     * return the original string.
     *
     * @throws IllegalArgumentException if any of the arguments are
     * <code>null</code>, or if a property value cannot be obtained
     */
    public static String expandProperties(final String arg, final Properties properties) {
        if(arg ==null)
            throw new IllegalArgumentException("arg is null");
        if(properties ==null)
            throw new IllegalArgumentException("arg is null");

        String start=PARSETIME[0];
        String end = PARSETIME[1];
        int s = 0;
        int e  ;
        StringBuilder result = new StringBuilder();
        while((e = arg.indexOf(start, s)) >= 0) {
            String str = arg.substring(e+start.length());
            int n = str.indexOf(end);
            if(n != -1) {
                result.append(arg.substring(s, e));
                String prop = str.substring(0, n);
                if(prop.equals("/")) {
                    result.append(java.io.File.separator);
                } else if(prop.equals(":")) {
                    result.append(java.io.File.pathSeparator);
                } else {
                    String value = properties.getProperty(prop);
                    if(value == null)
                        throw new IllegalArgumentException("property "+
                                                           "["+prop+"] "+
                                                           "not declared");
                    result.append(value);
                }
                s = e+start.length()+prop.length()+end.length();
            } else {
                result.append(start);
                s = e+start.length();
            }
        }
        result.append(arg.substring(s));
        return (result.toString());
    }
}
