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
package org.rioproject.opstring;

import org.rioproject.associations.AssociationDescriptor;
import org.rioproject.log.LoggerConfig;

import java.util.ArrayList;
import java.util.Map;
import java.util.List;
import java.util.Vector;
import java.util.HashMap;
import java.util.Arrays;
import java.util.Properties;

/**
 * A GlobalAttrs represents the descriptive attributes of a service that has
 * been parsed.
 *
 * @author Dennis Reedy
 */
@SuppressWarnings("PMD.AvoidThrowingRawExceptionTypes")
public class GlobalAttrs {
    /** List of groups to join */
    protected List<String> groupList = new ArrayList<String>();
    /** List of ClassBundles */
    protected Map<String, ClassBundle> bundles = new HashMap<String, ClassBundle>();
    /** List of ClassBundles */
    protected Map<String, String> artifacts = new HashMap<String, String>();
    /** List of locators to create */
    protected List<String> locatorList = new ArrayList<String>();
    /** Initialization parameters */
    protected Properties initParms = new Properties();
    /** Configuration parameters */
    protected List<String> configParms = new ArrayList<String>();
    /** Collection of machines to provision to */
    protected List<String> cluster = new ArrayList<String>();
    /** Codebase for the service */
    protected String codebase;
    /** The organization (representative owner) of the ServiceBean */
    protected String organization;
    /** Array of LoggerConfig objects */
    protected List<LoggerConfig> logConfigs = new ArrayList<LoggerConfig>();
    /** Service Provision config */
    protected String[] provisionConfig;
    /** FaultDetectionHandler ClassBundle */
    private ClassBundle fdhBundle;
    /** Collection of service associations */
    protected Vector<AssociationDescriptor> associations =
        new Vector<AssociationDescriptor>();
    /** Flag to determine if the service should be automatically advertised
     * when provisioned
     */
    protected boolean autoAdvertise = true;

    /**
     * Set a target machine to provision the service to
     * 
     * @param machines Array of machines to provision the ServiceBean to. Will 
     * be either the hostname or IP Address
     * @param append If true, add to the current machine list, otherwise clear
     * the current machine list and set the machine list to the input param
     */
    public void setCluster(String[] machines, boolean append) {
        if(!append)
            cluster.clear();
        cluster.addAll(Arrays.asList(machines));
    }
    
    /**
     * Set the target machine to provision the service to
     * 
     * @return An array of target machine hostnames or IP Addresses.
     * This method will return a new array each time. If there are no Target
     * Machines then a zero-length array will be returned
     */
    public String[] getCluster() {
        return (cluster.toArray(new String[cluster.size()]));
    }

    /**
     * Set the codebase property
     * 
     * @param codebase The codebase
     */
    public void setCodebase(String codebase) {
        this.codebase = codebase;
    }

    /**
     * Get the codebase property
     * 
     * @return The codebase property
     */
    public String getCodebase() {
        return (codebase);
    }

    /**
     * Add a Configuration parameter
     * 
     * @param parms A String array of configuration parameters
     * @param append If true, add to the current configuration, otherwise the
     * configuration set to the input param
     */
    public void setConfigParameters(String[] parms, boolean append) {
        if(parms==null)
            return;
        if(!append || configParms.size()==0) {
            configParms.clear();
            configParms.addAll(Arrays.asList(parms));
        } else {
            String[] currentConfigs = getConfigParameters();
            for (String parm : parms) {
                for (String currentConfig : currentConfigs) {
                    if (currentConfig.equals("-"))
                        continue;
                    if (getName(parm).equals(getName(currentConfig))) {
                        configParms.remove(currentConfig);
                        break;
                    }
                }
                if (!parm.equals("-"))
                    configParms.add(parm);
            }
        }
    }

    /**
     * Add a Configuration parameter
     *
     * @param parms A String array of configuration parameters
     * @param append If true, add to the current configuration, otherwise the
     * configuration set to the input param
     */
    public void setConfigParameters(List<String> parms, boolean append) {
        setConfigParameters(parms.toArray(new String[parms.size()]), append);
    }

    /*
     * Get the name of the config parameter
     * 
     * @return The name of the config parameter
     */
    private String getName(String parm) {
        String[] values = parm.split("=");
        return(values[0].trim());
    }

    public String[] getRawConfigParameters() {
        return(configParms.toArray(new String[configParms.size()]));
    }
    
    /**
     * Get Configuration parameters
     * 
     * @return String array suitable for use for a Configuration
     */
    public String[] getConfigParameters() {
        if(configParms.size()==0)
            return (new String[]{"-"});
        return(configParms.toArray(new String[configParms.size()]));
    }

    /**
     * Add to the groups collection
     * 
     * @param groups Array group names
     * @param append If true, add to the current groups, otherwise clear
     * the current groups and set the group list to the input param
     */
    public void setGroups(String[] groups, boolean append) {
        if(!append)
            groupList.clear();
        for (String group : groups) {
            if (group.equals("public"))
                group = "";
            groupList.add(group);
        }
    }

    /**
     * Add to the groups collection
     *
     * @param groups Array group names
     * @param append If true, add to the current groups, otherwise clear
     * the current groups and set the group list to the input param
     */
    public void setGroups(List<String> groups, boolean append) {
        setGroups(groups.toArray(new String[groups.size()]), append);
    }

    /**
     * Get the groups
     * 
     * @return Array of group names
     */
    public String[] getGroups() {
        String[] groups = new String[groupList.size()];
        for(int i = 0; i < groupList.size(); i++)
            groups[i] = groupList.get(i);
        return (groups);
    }

    /**
     * Add to the locators collection
     * 
     * @param locators Array of string representations of a known LookupLocator
     * instance
     * @param append If true, add to the current locators, otherwise clear
     * the current locators and set the locators list to the input param
     */
   public void setLocators(String[] locators, boolean append) {
       if(!append)
           locatorList.clear();
        locatorList.addAll(Arrays.asList(locators));
   }

    /**
     * Get the locators as an array of String objects
     * 
     * @return Array of locator Strings
     */
    public String[] getLocators() {
        String[] locators = new String[locatorList.size()];
        for(int i = 0; i < locators.length; i++)
            locators[i] = locatorList.get(i);
        return (locators);
    }

    /**
     * Set LoggerConfig objects
     * 
     * @param configs Array of LoggerConfig objects
     * @param append Add to the Collection of LoggerConfigs or replace it
     */
    public void setLogConfigs(LoggerConfig[] configs, boolean append) {
        if(configs==null)
            return;
        if(!append || logConfigs.size()==0) {
            logConfigs.clear();
            logConfigs.addAll(Arrays.asList(configs));
        } else {
            LoggerConfig[] currentConfigs = getLogConfigs();
            for (LoggerConfig config : configs) {
                for (LoggerConfig currentConfig : currentConfigs) {
                    if (config.getLoggerName().equals(
                        currentConfig.getLoggerName())) {
                        logConfigs.remove(currentConfig);
                        break;
                    }
                }
                logConfigs.add(config);
            }
        }

    }

    /**
     * Get the LoggerConfig objects
     * 
     * @return Array of LoggerConfig objects
     */
    public LoggerConfig[] getLogConfigs() {
        return(logConfigs.toArray(new LoggerConfig[logConfigs.size()]));
    }

    /**
     * Set the organization value
     * 
     * @param organization The organization (representative owner) of the
     * ServiceBean
     */
    public void setOrganization(String organization) {
        this.organization = organization;
    }

    /**
     * Get the organization
     * 
     * @return The organization (representative owner) of the
     * ServiceBean
     */
    public String getOrganization() {
        return (organization);
    }

    /**
     * Set initialization parameters
     * 
     * @param params the input parameters to set
     * @param append Whether to add the imput parametsr to the current Colection
     * or to clear and set them to the inpt params
     */
    public void setParameters(Properties params, boolean append) {
        if(append)
            initParms.putAll(params);
        else {
            initParms.clear();
            initParms.putAll(params);
        }
    }

    /**
     * Get initialization parameters
     * 
     * @return A Properties object containing initialization name,value pairs
     */
    public Properties getParameters() {
        return (initParms);
    }

    /**
     * Set service provision config
     *
     * @param args Array of service provision configuration arguments
     */
    public void setServiceProvisionConfig(String[] args) {
        if(args!=null) {
            provisionConfig = new String[args.length];
            System.arraycopy(args, 0, provisionConfig, 0, args.length);
        }
    }

    /**
     * Get the service provision config
     *
     * @return Array of service provision configuration arguments
     */
    public String[] getServiceProvisionConfig() {
        if(provisionConfig==null)
            provisionConfig = new String[]{"-"};
        return(provisionConfig);
    }

    /**
     * Set the ClassBundle for the FaultDetectionHandler
     *
     * @param bundle The ClassBundle for the FaultDetectionHandler
     */
    public void setFaultDetectionHandlerBundle(ClassBundle bundle) {
        if(bundle != null)
            fdhBundle = bundle;
    }

    /**
     * Get the ClassBundle for the FaultDetectionHandler
     *
     * @return The ClassBundle for the FaultDetectionHandler
     */
    public ClassBundle getFaultDetectionHandlerBundle() {
        return (fdhBundle);
    }


    /**
     * Add an AssociationDescriptor
     *
     * @param a The AssociationDescriptor to add.
     *
     * @throws Exception If the AssociationDescriptor already exists (as defined
     * by the equals method of an AssociationDescriptor), then an Exception is
     * thrown
     */
    public void addAssociationDescriptor(AssociationDescriptor a) throws Exception {
        if(associations.contains(a))
            throw new Exception("Duplicate AssociationDescriptor ["+a.getName()+"]");
        associations.add(a);
    }

    /**
     * Set auto advertise
     *
     * @param value String value for autoAdvertise. If "yes" then true,
     * otherwise false
     */
    public void setAutoAdvertise(String value) {
        autoAdvertise = "yes".equals(value);
    }

    /**
     * Get auto advertise
     *
     * @return autoAdvertise
     */
    public boolean getAutoAdvertise() {
        return (autoAdvertise);
    }
    
    /**
     * Get all AssociationDescriptors
     *
     * @return An array of AssociationDescriptor components. This method will
     * return a new array each time. If there are no AssociationDescriptor
     * components then a zero-length array will be returned
     */
    public AssociationDescriptor[] getAssociationDescriptors() {
        return (associations.toArray(
            new AssociationDescriptor[associations.size()]));
    }
}
