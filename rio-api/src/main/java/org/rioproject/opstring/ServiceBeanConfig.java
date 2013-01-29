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
package org.rioproject.opstring;

import net.jini.core.discovery.LookupLocator;
import org.rioproject.log.LoggerConfig;

import java.io.Serializable;
import java.util.*;

/**
 * This class defines configuration attributes for a ServiceBean instance. Some
 * attributes may also be updated (groups, locators and initialization
 * properties) and used for subsequent ServiceBean initializations
 *
 * @author Dennis Reedy
 */
public class ServiceBeanConfig implements Serializable {
    @SuppressWarnings("unused")
    static final long serialVersionUID = 1L;
    /** Initialization Properties for the ServiceBean */
    private final Map<String, Object> initParameters = new HashMap<String, Object>();
    /** Configuration parameters in the form of name,value pairs */
    private final Map<String, Object> configParms = new HashMap<String, Object>();
    /** A collection on {@code LoggerConfigs}*/
    private final Collection<LoggerConfig> loggerConfigs = new ArrayList<LoggerConfig>();
    /** The configArgs property used to create the Configuration object for the
     * ServiceBean */
    private String[] configArgs;
    /** ServiceBean component field, used to access Configuration entries */
    public static final String COMPONENT = "configComponent";
    /** ServiceBean name field */
    public static final String NAME = "serviceName";
    /** Name to use when constructing the JMX ObjectName */
    public static final String JMX_NAME = "jmxName";
    /** ServiceBean organization field */
    public static final String ORGANIZATION = "organization";
    /** ServiceBean Comment field */
    public static final String COMMENT = "serviceComment";
    /** ServiceBean OperationalString name field */
    public static final String OPSTRING = "opStringName";
    /** Lookup Discovery groups */
    public static final String GROUPS = "lookupGroups";
    /** Lookup Locators */
    public static final String LOCATORS = "lookupLocators";
    /** Key for accessing list of hosts the ServiceBean has previously visited 
     * (been allocated to). If this is the first instantiation of the service,
     * an empty List has will be returned */
    public static final String HOST_HISTORY = "hostList";
    /** Key for accessing instanceID.  The instance ID represents an ordinal
     * value relative to the number of services provisioned. */
    public static final String INSTANCE_ID = "instanceID";
    /** Key for accessing service provisioning config */
    public static final String SERVICE_PROVISION_CONFIG = "provisionConfig";
    /** Key for accessing the initial value for the number of planned services.
     * This property is stored as an configuration parameter. The value for
     * this property is an Integer */
    public static final String INITIAL_PLANNED_SERVICES = "initial.planned";
    /**
     * Convenience constant used to request that attempts be made to
     * discover all lookup services that are within range, and which
     * belong to any group.
     */
    public static final String[] ALL_GROUPS = null;
    /**
     * Convenience constant used to request that discovery by group
     * membership be halted (or not started, if the group discovery
     * mechanism is simply being instantiated).
     */
    public static final String[] NO_GROUPS = new String[0];

    /**
     * Create a new ServiceBeanConfig
     */
    public ServiceBeanConfig() {
    }

    /**
     * Create a new ServiceBeanConfig
     * 
     * @param configMap Configuration parameters, composed of name,value pairs
     * @param configArgs String[] of attributes and values suitable for use
     * with {@link net.jini.config.Configuration}
     * 
     * @throws IllegalArgumentException if the configMap or configArgs parameters
     * are null
     */
    public ServiceBeanConfig(final Map<String, Object> configMap, final String[] configArgs) {
        if(configMap == null)
            throw new IllegalArgumentException("configMap is null");
        if(configArgs == null)
            throw new IllegalArgumentException("configArgs is null");
        configParms.putAll(configMap);
        if(configParms.get(HOST_HISTORY)==null)
            configParms.put(HOST_HISTORY, new ArrayList<String>());
        if(configParms.get(INSTANCE_ID)==null)
            configParms.put(INSTANCE_ID, (long) 0);
        this.configArgs = new String[configArgs.length];
        System.arraycopy(configArgs, 0, this.configArgs, 0, this.configArgs.length);
    }

    /**
     * Set the name for the ServiceBean.
     * 
     * @param name for the ServiceBean
     */
    public void setName(final String name) {
        if(name!=null)
            configParms.put(NAME, name);
    }
    
    /**
     * Get the name for the ServiceBean
     * 
     * @return Name for the ServiceBean
     */
    public String getName() {
        String name = (String)configParms.get(NAME);
        return ((name==null?"":name));
    }
    
    /**
     * Get the name of the OperationalString this object has been constructed with
     * 
     * @return Name of the OperationalString. If the name is null, an empty String is
     * returned
     */
    public String getOperationalStringName() {
        String name = (String)configParms.get(OPSTRING);
        return ((name==null?"":name));
    }
    
    /**
     * Set the name of the OperationalString 
     * 
     * @param name of the OperationalString. 
     */
    public void setOperationalStringName(final String name) {
        if(name!=null)
            configParms.put(OPSTRING, name);
    }

    /**
     * Informative comment for the ServiceBean
     * 
     * @return Comment for the ServiceBean, may be null 
     */
    public String getComment() {
        return ((String)configParms.get(COMMENT));
    }
    
    /**
     * Get the organization
     * 
     * @return The organization (representative owner) of the ServiceBean, may 
     * be null
     */    
    public String getOrganization() {
        return ((String)configParms.get(ORGANIZATION));
    }

    /**
     * Add a {@code LoggerConfig}
     *
     * @param loggerConfigs The {@code LoggerConfig}s to add. If {@code null} no-op.
     */
    public void addLoggerConfig(LoggerConfig... loggerConfigs) {
        if(loggerConfigs!=null) {
            Collections.addAll(this.loggerConfigs, loggerConfigs);
        }
    }

    /**
     * Get all configured {@code LoggerConfig}s.
     *
     * @return An array of configured {@code LoggerConfig}s. If there are no configured {@code LoggerConfig}s return an
     * empty array. A new array is allocated each time.
     */
    public LoggerConfig[] getLoggerConfigs() {
        return loggerConfigs.toArray(new LoggerConfig[loggerConfigs.size()]);
    }
    
    /**
     * Set the Lookup groups the service will use for discovery. If this 
     * parameter is not set (remains null), then no attempts will be made via group
     * discovery to discover lookup services
     * 
     * @param groups Array of String group names whose members are
     * the lookup services to discover. Elements contained within the array may 
     * be modified as follows:
     * 
     * <ul>
     * <li>If the groups property is equivalent to the value of 
     * {@link #ALL_GROUPS}, the value
     * will be transformed into "all"
     * <li>If groups property is equivalent  to an empty string "", then the 
     * value will be transformed to "public"
     * </ul>
     */
    public void setGroups(final String... groups) {
        String[] g;
        if(groups == ALL_GROUPS)
            g = new String[]{"all"};
        else {
            g = new String[groups.length];
            System.arraycopy(groups, 0, g, 0, groups.length);
            for(int i=0; i<g.length; i++) {
                if(g[i].equals(""))
                    g[i] = "public";
            }
        }
        configParms.put(GROUPS, g);
    }

    /**
     * Returns an array consisting of the names of the groups whose members are
     * the lookup services to discover.
     * <ul>
     * <li>If the groups property is null or has a zero length, then the
     * returned value is  
     * {@link #NO_GROUPS}
     * <li>If an element has the value of "all", that value will be transformed
     * to  {@link #ALL_GROUPS}
     * <li>If an element has the value of "public", that value is transformed
     * to an empty string ""
     * </ul>
     * 
     * @return String array of groups
     */
    public String[] getGroups() {
        String[] groups = (String[])configParms.get(GROUPS);
        if(groups == null || groups.length == 0)
            groups = NO_GROUPS;
        if(groups.length > 0) {
            if(groups.length == 1 && groups[0].equals("all")) {
                groups = ALL_GROUPS;
            } else {
                for(int i = 0; i < groups.length; i++) {
                    if(groups[i].equals("public"))
                        groups[i] = "";
                }
            }
        }
        return (groups);
    }

    /**
     * Set Locator information the service optionally uses for discovery.
     * 
     * @param lookupLocators Array of LookupLocator instances
     */
    public void setLocators(final LookupLocator[] lookupLocators) {
        configParms.put(LOCATORS, lookupLocators);
    }

    /**
     * Return Locator information the service optionally uses for discovery.
     * 
     * @return Array of LookupLocator objects, or null
     */
    public LookupLocator[] getLocators() {
        return ((LookupLocator[])configParms.get(LOCATORS));
    }

    /**
     * Add a name/value pair to the Collection of ServiceBean controlled
     * initialization parameters.
     * 
     * @param name Object key name
     * @param value Object value
     * 
     * @throws IllegalArgumentException if the name parameter is null
     */
    public void addInitParameter(final String name, final Object value) {
        if(name == null)
            throw new IllegalArgumentException("name is null");
        initParameters.put(name, value);
    }
    
    /**
     * Service controlled initialization parameters.
     * 
     * @return A Map of Service controlled initialization parameters.
     */
    public Map<String, Object> getInitParameters() {
        return (initParameters);
    }

    /**
     * Get the system initialization parameters
     * 
     * @return A Map of system initialization parameters. A new Map is
     * allocated each time this method is invoked
     */
    public Map<String, Object> getConfigurationParameters() {
        Map<String, Object> map = new HashMap<String, Object>();
        map.putAll(configParms);
        return (map);
    }

    /**
     * Set the system initialization parameters
     *
     * @param configParms Map of system initialization parameters.
     */
    public void setConfigurationParameters(final Map<String, Object> configParms) {
        this.configParms.clear();
        this.configParms.putAll(configParms);
    }

    /**
     * Get the instance ID. The instance ID represents an ordinal value relative to
     * the number of services provisioned.
     * 
     * @return The instance ID
     */
    public Long getInstanceID() {
        return((Long)configParms.get(INSTANCE_ID));
    }

    public void setConfigArgs(String[] configArgs) {
        if(configArgs == null)
            throw new IllegalArgumentException("configArgs is null");
        this.configArgs = new String[configArgs.length];
        System.arraycopy(configArgs, 0, this.configArgs, 0, this.configArgs.length);
    }
    
    /**
     * Get the service's configuration arguments
     * 
     * @return The service's configuration arguments. The returned String[]
     * value is suitable for the creation of a {@link net.jini.config.Configuration}
     * object.
     */
    public String[] getConfigArgs() {
        if(configArgs == null)
            return (new String[]{"-"});
        String[] args = new String[configArgs.length];
        System.arraycopy(configArgs, 0, args, 0, configArgs.length);
        return (args);
    }


    public String toString() {
        StringBuilder buffer = new StringBuilder();
        buffer.append("configComponent=").append(configParms.get(COMPONENT)).append("\n");
        buffer.append("serviceName=").append(configParms.get(NAME)).append("\n");
        buffer.append("organization=" + "").append(configParms.get(ORGANIZATION)).append("\n");
        buffer.append("serviceComment=").append(configParms.get(COMMENT)).append("\n");
        buffer.append("opStringName=").append(configParms.get(OPSTRING)).append("\n");
        String[] groups = (String[])configParms.get(GROUPS);
        buffer.append("lookupGroups={");
        if(groups==null) {
            buffer.append("null");
        } else {
            for(int i=0; i<groups.length; i++) {
                if(i>0)
                    buffer.append(", ");
                buffer.append(groups[i]);
            }
        }
        buffer.append("}\n");
        LookupLocator[] locators = (LookupLocator[])configParms.get(LOCATORS);
        buffer.append("lookupLocators={");
        if(locators!=null) {
            for(int i=0; i<locators.length; i++) {
                if(i>0)
                    buffer.append(", ");
                buffer.append("\t").append(locators[i].toString()).append("\n");
            }
        } else {
            buffer.append("null");
        }
        buffer.append("}\n");

        if(!loggerConfigs.isEmpty()) {
            buffer.append("loggerConfig=\n");
            for (Object lConfig : loggerConfigs) {
                buffer.append(lConfig.toString());
            }
        } else {
            buffer.append("loggerConfig={empty}\n");
        }
        buffer.append("instanceID=").append(getInstanceID()).append("\n");
        buffer.append("Initialization Parameters=\n");
        buffer.append(initParameters.toString()).append("\n");
        buffer.append("Configuration Properties=\n");
        if(configArgs!=null) {
            for(int i=0; i<configArgs.length; i++) {
                if(i>0)
                    buffer.append("\n");
                buffer.append(configArgs[i]);
            }
        } else {
            buffer.append("-");
        }
        buffer.append("\n");
        return(buffer.toString());
    }
}
