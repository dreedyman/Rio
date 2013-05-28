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
package org.rioproject.jmx;

import net.jini.core.entry.Entry;
import net.jini.id.Uuid;
import net.jini.lookup.entry.jmx.JMXProperty;
import net.jini.lookup.entry.jmx.JMXProtocolType;
import org.rioproject.config.Constants;
import org.rioproject.core.jsb.ServiceBeanContext;
import org.rioproject.opstring.ClassBundle;
import org.rioproject.opstring.ServiceBeanConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.MBeanServerConnection;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.openmbean.*;
import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * Provides utilities for using JMX.
 *
 * @author Ming Fang
 * @author Dennis Reedy
 */
public class JMXUtil {
    private static Logger logger = LoggerFactory.getLogger(JMXUtil.class.getName());

    /**
     * Get a platform MXBean proxy
     *
     * @param mbsc The MBeanServerConnection
     * @param name The ObjectName to create
     * @param mxBeanInterface The platform MXBean interface type to create
     * @return A platform MXBean proxy
     */
    public static <T> T getPlatformMXBeanProxy(final MBeanServerConnection mbsc,
                                               final String name,
                                               final Class<T> mxBeanInterface) {
        T mxBean = null;
        try {
            ObjectName objName = new ObjectName(name);
            mxBean = ManagementFactory.newPlatformMXBeanProxy(mbsc, objName.toString(), mxBeanInterface);
        } catch (IOException e) {
            logger.warn("Could not create PlatformMXBeanProxy", e);
        } catch (MalformedObjectNameException e) {
            logger.warn("Could not create PlatformMXBeanProxy", e);
        }
        return mxBean;
    }

    /**
     * Create a Map of accessor methods for the data object that are supported
     * by {@link javax.management.openmbean.OpenType#ALLOWED_CLASSNAMES}
     *
     * @param data The data to map
     *
     * @return A Map of accessor methods for the data object that are supported
     * by {@link javax.management.openmbean.OpenType#ALLOWED_CLASSNAMES}
     * @throws IntrospectionException If an exception occurred during the
     * introspection of an MBean 
     * @throws IllegalAccessException If access permissions result reflecting
     * on the MBean
     * @throws InvocationTargetException If the MBean cannot be instantiated
     */
    public static Map toMap(final Object data) throws
                                               IntrospectionException,
                                               IllegalAccessException,
                                               InvocationTargetException {
        Map<String, Object> map = new HashMap<String, Object>();
        BeanInfo beanInfo = Introspector.getBeanInfo(data.getClass());
        PropertyDescriptor[] propertyDescriptors =
            beanInfo.getPropertyDescriptors();
        for (PropertyDescriptor propertyDescriptor : propertyDescriptors) {
            String key = propertyDescriptor.getName();
            Method readMethod = propertyDescriptor.getReadMethod();
            Object value = null;
            if (readMethod != null) {
                value = readMethod.invoke(data, (Object[]) null);
            }
            if (value == null) {
                continue;
            }
            if (!isOpenType(key)) {
                value = value.toString();
            }
            map.put(key, value);
        }
        return map;
    }

    /**
     * Create a {@link javax.management.openmbean.CompositeType}
     *
     * @param m A Map of accessor methods corresponding to open data types
     * @param compositeTypeName The name to use for the composite type, must
     * not be null or an empty string
     * @param compositeTypeDescription The description to use for the
     * composite type, must not be null or an empty string
     * @return The created CompositeType
     * @throws OpenDataException If the CompositeType cannot be created
     */
    public static CompositeType createCompositeType(final Map m,
                                                    final String compositeTypeName,
                                                    final String compositeTypeDescription) throws OpenDataException {
        String [] keys = new String[m.size()];
        OpenType [] types = new OpenType[m.size()];
        int index = 0;
        for (Object o : m.keySet()) {
            String key = (String) o;
            keys[index] = key;
            types[index] = getOpenType(m.get(key).getClass().getName(), null);
            index++;
        }
        return new CompositeType(compositeTypeName, compositeTypeDescription, keys, keys, types);
    }

    /**
     * Get the corresponding OpenType for a fully qualified class name
     *
     * @param classString A fully qualified class name, suitable for use
     * with Class.forName(), must not be null
     * @param defaultType The defaultOpenType to use
     * @return the corresponding OpenType
     *
     * @throws IllegalArgumentException if the classString argument is null
     * @throws InvalidOpenTypeException if the class described by the
     * classString argument is not a valid open type
     */
    public static OpenType getOpenType(final String classString, final OpenType defaultType) {
        if(classString==null)
            throw new IllegalArgumentException("classString is null");
        if(classString.equals("void")) {
            return SimpleType.VOID;
        }
        if(!isOpenType(classString)) {
            throw new InvalidOpenTypeException(classString);
        }
        if(classString.equals(String.class.getName())) {
            return SimpleType.STRING;
        } else if(classString.equals(Boolean.class.getName())) {
            return SimpleType.BOOLEAN;
        } else if(classString.equals(Long.class.getName())) {
            return SimpleType.LONG;
        } else if(classString.equals(Integer.class.getName())) {
            return SimpleType.INTEGER;
        } else if(classString.equals(Float.class.getName())) {
            return SimpleType.FLOAT;
        } else if(classString.equals(Double.class.getName())) {
            return SimpleType.DOUBLE;
        } else if(defaultType != null) {
            return defaultType;
        }
        throw new InvalidOpenTypeException("Unsupported type: "+classString);
    }

    /**
     * Determine if the class name is an OpenType
     *
     * @param className A fully qualified class name, suitable for use
     * with Class.forName(), must not be null
     * @return If the class name is supported as an OpenType return
     * <code>true</code>
     *
     * @see javax.management.openmbean.OpenType#ALLOWED_CLASSNAMES_LIST
     */
    public static boolean isOpenType(final String className) {
        return OpenType.ALLOWED_CLASSNAMES_LIST.contains(className);
    }

    /**
     * Get the JMX name to use as a base name for the ObjectName. Key property
     * list values should be appended to the value to create ObjectName
     * instances.
     *
     * This method will check to see if the
     * {@link org.rioproject.opstring.ServiceBeanConfig#JMX_NAME} property exists,
     * if it does not, the default domain will be used as a basis to create
     * the name, and the property will be added to the context with the
     * following format :
     * <tt>defaultDomain:type=<export-class-name>,name=servicename</tt>
     *
     * @param context The ServiceBeanContext, must not be null
     * @param defaultDomain The default domain to use if the
     * ServiceBeanConfig.JMX_NAME property is not found
     *
     * @return A String to use as a base name for the ObjectName. The returned
     * value will have the name with the format of:
     * <tt>jmxName,uuid=uuid-string</tt>
     *
     */
    public static String getJMXName(final ServiceBeanContext context,
                                    final String defaultDomain) {
        if(context==null)
            throw new IllegalArgumentException("context is null");
        Uuid uuid = context.getServiceBeanManager().getServiceID();
        Map<String, Object> configParms =
            context.getServiceBeanConfig().getConfigurationParameters();
        String jmxName = (String)configParms.get(ServiceBeanConfig.JMX_NAME);
        ClassBundle[] exports = context.getServiceElement().getExportBundles();        
        String type = "Service";
        String domain = null;
        if(exports.length>0) {
            String exportClass =  exports[0].getClassName();
            int ndx = exportClass.lastIndexOf(".");
            if(ndx>0) {
                type = exportClass.substring(ndx+1);
                domain = exportClass.substring(0, ndx);
            }
        }
        if(jmxName==null) {
            jmxName = (domain==null?defaultDomain:domain)+":"+"type="+type;
            configParms.put(ServiceBeanConfig.JMX_NAME, jmxName);
            context.getServiceBeanConfig().setConfigurationParameters(configParms);
        }
        return (jmxName+","+"uuid="+uuid);
    }

    /**
     * Get an ObjectName with the following format :
     *
     * <tt>defaultDomain:type=<export-class-name>,name=servicename,
     *     name=<name>, id=<id></tt>
     *
     * @param context The ServiceBeanContext, must not be null
     * @param defaultDomain The default domain to use if the
     * ServiceBeanConfig.JMX_NAME property is not found
     * @param name The name to use
     * @return The created ObjectName
     * @throws MalformedObjectNameException If the constructed name is malformed
     */
    public static ObjectName getObjectName(final ServiceBeanContext context,
                                           final String defaultDomain,
                                           final String name) throws MalformedObjectNameException {
        String jmxName = getJMXName(context, defaultDomain);
        String oName = jmxName+","+"name="+name;        
        return ObjectName.getInstance(oName);
    }

    /**
     * Get an ObjectName with the following format :
     * <p/>
     * <tt>defaultDomain:type=<export-class-name>,name=servicename, name=<name>,
     * id=<id></tt>
     *
     * @param context The ServiceBeanContext, must not be null
     * @param defaultDomain The default domain to use if the
     * ServiceBeanConfig.JMX_NAME property is not found
     * @param name The jmxName to use
     * @param id The instanceID of the bean
     *
     * @return An ObjectName based on the ServiceBeanContext, defaultDomain,
     * name and id
     *
     * @throws MalformedObjectNameException If the constructed name is malformed
     */
    public static ObjectName getObjectName(final ServiceBeanContext context,
                                           final String defaultDomain,
                                           final String name,
                                           final String id) throws MalformedObjectNameException {
        String jmxName = getJMXName(context, defaultDomain);
        String oName = jmxName+","+"name="+name+","+"id="+id;
        return (ObjectName.getInstance(oName));
    }

    /**
     * Get the attributes to add to a service's attribute collection
     *
     * @return An array of {@link net.jini.core.entry.Entry}s. If the
     * <tt>org.rioproject.jmxServiceURL</tt> system property exists (is not null)
     * create an array of 2 attributes, one being
     * {@link net.jini.lookup.entry.jmx.JMXProtocolType} with the protocol
     * type set to {@link net.jini.lookup.entry.jmx.JMXProtocolType#RMI}, the other
     * {@link net.jini.lookup.entry.jmx.JMXProperty}, set to the value of the property
     * <tt>org.rioproject.jmxServiceURL</tt>. If the
     * <tt>org.rioproject.jmxServiceURL</tt> property is not found, return a
     * zero-length array. A new array is created each time.
     */
    public static Entry[] getJMXConnectionEntries() {
        Entry[] entries = new Entry[0];
        /* Check for JMXConnection */
        try {
            JMXConnectionUtil.createJMXConnection();
            String jmxServiceURL = System.getProperty(Constants.JMX_SERVICE_URL);
            if(jmxServiceURL!=null) {
                entries = new Entry[2];
                entries[0] = new JMXProtocolType(JMXProtocolType.RMI);
                entries[1] = new JMXProperty(Constants.JMX_SERVICE_URL, jmxServiceURL);
            }
        } catch (Exception e) {
            logger.warn("Could not create JMX Connection, JMX monitoring not available", e);
        }
        return(entries);
    }

    /**
     * Get the String value found in the JMXProperty entry, or null if the attribute
     * set does not include a JMXProperty
     *
     * @param attributes An array of Entry attributes
     *
     * @return The JMX Connection String obtained from a
     */
    public static String getJMXConnection(final Entry[] attributes) {
        String jmxConn = null;
        for (Entry attribute : attributes) {
            if (attribute instanceof JMXProperty) {
                JMXProperty jmxProp = ((JMXProperty) attribute);
                if (jmxProp.name != null &&
                    jmxProp.name.equals(Constants.JMX_SERVICE_URL)) {
                    jmxConn = jmxProp.value;
                    break;
                }
            }
        }
        return(jmxConn);
    }
}
