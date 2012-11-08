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
package org.rioproject.watch;

import javax.management.MBeanServerConnection;
import java.io.Serializable;

/**
 * The WatchDescriptor defines attributes of a declarable
 * {@code Watch}, allowing a Watch to be declared as part
 * of an OperationalString.
 *
 * @author Dennis Reedy
 */
public class WatchDescriptor implements Serializable {
    @SuppressWarnings("unused")
    static final long serialVersionUID = 1L;
    public enum Type { GAUGE, COUNTER, PERIODIC, STOP }
    public static final long DEFAULT_PERIOD=10*1000;
    private String name;
    private String objectName;
    private Type type;
    private String property;
    private String attribute;
    private long period;
    private transient MBeanServerConnection mbsc;


    /**
     * Create a WatchDescriptor
     *
     * @param name The name of the Watch
     * @param period The period the Watch should use to collect data (in
     * milliseconds). If the period is 0, the default period of 10 seconds will
     * be used
     *
     * @throws IllegalArgumentException if the name argument is null
     */
    public WatchDescriptor(String name, long period) {
        if(name == null)
            throw new IllegalArgumentException("name is null");
        this.name = name;
        this.period = (period==0?DEFAULT_PERIOD:period);
    }

    /**
     * Create a WatchDescriptor
     *
     * @param name The name of the Watch
     * @param property The bean property to invoke to obtain a value to record
     * in the Watch.
     * @param period The period the Watch should use to collect data (in
     * milliseconds). If the period is 0, the default period of 10 seconds will
     * be used
     *
     * @throws IllegalArgumentException if the name or property arguments
     * are null
     */
    public WatchDescriptor(String name, String property, long period) {
        this(name, Type.GAUGE, property, period);
    }

    /**
     * Create a WatchDescriptor
     *
     * @param name The name of the Watch
     * @param type An enum Type.
     * @param property The bean property to invoke to obtain a value to record
     * in the Watch.
     * @param period The period the Watch should use to collect data (in
     * milliseconds). If the period is 0, the default period of 10 seconds will
     * be used
     *
     * @throws IllegalArgumentException if the name, type, or property arguments
     * are null
     */
    public WatchDescriptor(String name,
                           Type type,
                           String property,
                           long period) {
        if(name == null)
            throw new IllegalArgumentException("name is null");
        if(property == null)
            throw new IllegalArgumentException("property is null");
        if(type == null)
            throw new IllegalArgumentException("type is null");
        this.type = type;
        this.name = name;
        this.type = type;
        this.property = property;
        this.period = (period==0?DEFAULT_PERIOD:period);
    }

    /**
     * Get the name of the watch
     *
     * @return The name of the Watch
     */
    public String getName() {
        return(name);
    }

    /**
     * Set the name of the watch
     *
     * @param name The name of the watch
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Set the JMX ObjectName to access
     *
     * @param objectName The JMX ObjectName
     */
    public void setObjectName(String objectName) {
        this.objectName = objectName;
    }

    /**
     * Get the JMX ObjectName to access
     *
     * @return The JMX ObjectName
     */
    public String getObjectName() {
        return objectName;
    }

    /**
     * Get the Watch type
     *
     * @return The type of Watch to create
     */
    public Type getType() {
        return(type);
    }

    /**
     * Set the Watch type
     *
     * @param type The watch type
     *
     * @throws IllegalArgumentException if the type is null
     */
    public void setType(Type type) {
        if(type == null)
            throw new IllegalArgumentException("type is null");
        this.type = type;
    }

    /**
     * Get the property name to invoke on the bean
     *
     * @return The bean property name to invoke. may be null
     */
    public String getProperty() {
        return(property);
    }

    /**
     * Set the property name to invoke on the bean
     *
     * @param property The property name to invoke on the bean
     */
    public void setProperty(String property) {
        this.property = property;
    }

    /**
     * Get the periodicity to collect data from the property
     *
     * @return The periodicity to collect data from the property, default is
     * 10 seconds
     */
    public long getPeriod() {
        return (period);
    }

    /**
     * Set the periodicity to collect data from the property
     *
     * @param period The periodicity to collect data from the property
     */
    public void setPeriod(long period) {
        this.period = period;
    }

    /**
     * Get the MBean attribute to access
     *
     * @return The MBean attribute to access. This property is used in
     * conjunction with the <tt>objectName</tt> property to create a Watch
     * that uses the value obtained by the attribute as input for the Watch.
     */
    public String getAttribute() {
        return attribute;
    }

    /**
     * Set the MBean attribute to access
     *
     * @param attribute The MBean attribute to access. This property is used in
     * conjunction with the <tt>objectName</tt> property to create a Watch
     * that uses the value obtained by the attribute as input for the Watch.
     */
    public void setAttribute(String attribute) {
        this.attribute = attribute;
    }

    public void setMBeanServerConnection(MBeanServerConnection mbsc) {
        this.mbsc = mbsc;
    }

    public MBeanServerConnection getMBeanServerConnection() {
        return mbsc;
    }

    @Override
    public boolean equals(Object o) {
        if(this == o) return true;
        if(o == null || getClass() != o.getClass())
            return false;
        WatchDescriptor that = (WatchDescriptor)o;
        return name.equals(that.name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public String toString() {
        return "WatchDescriptor{" +
               "name='" + name + '\'' +
               ", objectName='" + objectName + '\'' +
               ", type=" + type +
               ", property='" + property + '\'' +
               ", attribute='" + attribute + '\'' +
               ", period=" + period +
               '}';
    }


}
