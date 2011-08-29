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

import org.rioproject.associations.AssociationDescriptor;
import org.rioproject.sla.RuleMap;
import org.rioproject.sla.SLA;
import org.rioproject.opstring.ServiceElement.MachineBoundary;
import org.rioproject.opstring.ServiceElement.ProvisionType;
import org.rioproject.sla.ServiceLevelAgreements;
import org.rioproject.core.provision.StagedData;
import org.rioproject.core.provision.SystemRequirements.SystemComponent;
import org.rioproject.exec.ExecDescriptor;
import org.rioproject.log.LoggerConfig;
import org.rioproject.servicecore.Service;

import java.util.*;

/**
 * A ParsedService represents the descriptive attributes of a service that has
 * been parsed.
 *
 * @author Dennis Reedy
 */
public class ParsedService extends GlobalAttrs{
    public static final String DYNAMIC="dynamic";
    public static final String FIXED="fixed";
    /** Default array of export jar names */
    public static final String[] DEFAULT_EXPORT_JARS = new String[]{"rio-api.jar",
                                                                    "jsk-dl.jar",
                                                                    "jmx-lookup.jar",
                                                                    "serviceui.jar"};
    /** Default name of the service interface class */
    public static final String DEFAULT_SERVICE_INTERFACE  = Service.class.getName();
    /** Name of the service */
    private String name;
    /** Comment for the service */
    private String comment;
    /** OperationalString name for the service */
    private String opStringName;
    /** Array of ClassBundle objects for the Interface classes */
    private ClassBundle[] interfaceBundle;
    /** Number of services to maintain */
    //private int maintain = 0;
    private String maintain;
    /** Max number of services per machine */
    //private int maxPerMachine = 0;
    private String maxPerMachine;
    /** machine boundary*/
    private MachineBoundary machineBoundary;
    /** Flag to determine if the service requires to match-on-name instead of
     * just by interfaces */
    private Boolean useName = true;
    /** Flag to determine the provision type, dynamic or external. True is
     * dynamic */
    private ProvisionType provisionType;
    /** Flag to determine if the service should use DiscoveryManagement pooling */
    private boolean discoPool = true;
    /** Component ClassBundle */
    private ClassBundle componentBundle;
    /** The name to use when registering with JMX */
    private String jmxName;
    private ExecDescriptor execDescriptor;
    private final List<StagedData> stagedData = new ArrayList<StagedData>();
    /** ServiceLevelAgreements object */
    private ServiceLevelAgreements slaAgreements;
    private boolean fork = false;
    private Collection<RuleMap> ruleMaps = new ArrayList<RuleMap>();

    /**
     * Create a ParsedService
     */
    public ParsedService() {
        super();
    }

    /**
     * Create a ParsedService with a descriptive name
     *
     * @param name The name of the service
     */
    public ParsedService(String name) {
        super();
        this.name = name;
    }

    /**
     * Create a ParsedService with a descriptive name
     * 
     * @param name The name of the service
     * @param global GlobalAttrs containing attributes that should be applied to 
     * the ParsedService. 
     */
    public ParsedService(String name, GlobalAttrs global) {
        super();
        this.name = name;
        /* Set Cluster from Global attributes */
        if(global.cluster.size()>0)
            cluster.addAll(global.cluster);
        /* Set Codebase from Global attributes */
        if(global.getCodebase()!=null)
            setCodebase(global.getCodebase());
        /* Set Configuration from Global attributes */
        this.configParms.addAll(global.configParms);
        /* Set Groups from Global attributes */
        this.groupList.addAll(global.groupList);
        /* Set Locators from Global attributes */
        this.locatorList.addAll(global.locatorList);
        /* Set LoggerConfigs from Global attributes */
        this.logConfigs.addAll(global.logConfigs);
        /* Set Organization from Global attributes */
        if(global.organization!=null)
            this.organization = global.organization;
        /* Set Parameters from Global attributes */
        this.initParms.putAll(global.initParms);
        /* Set Associations from Global attributes */
        this.associations.addAll(global.associations);
        /* Set fault detection handler */
        if(global.getFaultDetectionHandlerBundle()!=null)
            this.setFaultDetectionHandlerBundle(
                global.getFaultDetectionHandlerBundle());
        /* Set ServiceLevelAgreements */
        //this.setServiceLevelAgreements(global.getServiceLevelAgreements());
        /* Set service provision config */
        this.setServiceProvisionConfig(global.getServiceProvisionConfig());
    }    

    /**
     * Set discovery management pooling
     * 
     * @param value String value for discoPool. If "yes" then true,
     * otherwise false
     */
    public void setDiscoveryManagementPooling(boolean value) {
        discoPool = value;
    }

    /**
     * Set discovery management pooling
     *
     * @param value String value for discoPool. If "yes" then true,
     * otherwise false
     */
    public void setDiscoveryManagementPooling(String value) {
        discoPool = "yes".equals(value);
    }

    /**
     * Get discoPool
     * 
     * @return discoPool
     */
    public boolean getDiscoveryManagementPooling() {
        return (discoPool);
    }

    /**
     * Set the name for the ParsedService
     * 
     * @param name The name of the service
     */
    public void setName(String name) {
        this.name = name.trim();
    }

    /**
     * Get the name that the ParsedService has been constructed with
     * 
     * @return The name of the service
     */
    public String getName() {
        return (name);
    }

    /**
     * Set the name to use for JMX ObjectName
     *
     * @param name The name to use for JMX ObjectName
     */
    public void setJMXName(String name) {
        String test = name.trim();
        /* must conform to the pattern domain:key=value */
        int ndx = test.indexOf(":");
        if(ndx == -1)
            throw new IllegalArgumentException("malformed jmxname, missing ':'");
        test = test.substring(ndx);
        ndx = test.indexOf("=");
        if(ndx == -1)
            throw new IllegalArgumentException("malformed jmxame, " +
                                               "missing key=value pairing");
        test = test.substring(ndx);
        if(test.length() == 1)
            throw new IllegalArgumentException("malformed jmxame, "+
                                               "missing key=value pairing");
        this.jmxName = name.trim();
    }

    /**
     * Get the name to use for JMX ObjectName
     *
     * @return The name to use for JMX ObjectName
     */
    public String getJMXName() {
        return (jmxName);
    }

    /**
     * Set the comment
     * 
     * @param comment The comment for the service
     */
    public void setComment(String comment) {
        this.comment = comment.trim();
    }

    /**
     * Get the comment
     * 
     * @return The comment for the service
     */
    public String getComment() {
        return (comment);
    }

    /**
     * Set the OperationalString name
     * 
     * @param opStringName The OperationalString name the service belongs to
     */
    public void setOperationalStringName(String opStringName) {
        this.opStringName = opStringName;
    }

    /**
     * Get the OperationalString name
     * 
     * @return The OperationalString name the service belongs to
     */
    public String getOperationalStringName() {
        return (opStringName);
    }

    /**
     * Set the Interfaces as ClassBundle object containing the className and
     * resources required to load each class
     * 
     * @param bundles Array of ClassBundle objects
     */
    public void setInterfaceBundles(ClassBundle... bundles) {
        if(bundles == null) {
            interfaceBundle = new ClassBundle[0];
        } else {
            interfaceBundle = new ClassBundle[bundles.length];
            System.arraycopy(bundles, 0, interfaceBundle, 0, bundles.length);
        }
    }

    /**
     * Set the Interfaces as ClassBundle object containing the className and
     * resources required to load each class
     *
     * @param bundles Array of ClassBundle objects
     */
    public void setInterfaceBundles(List<ClassBundle> bundles) {
        setInterfaceBundles(bundles.toArray(new ClassBundle[bundles.size()]));
    }

    /**
     * Get the Interfaces as ClassBundle object containing the className and
     * resources required to load each class
     * 
     * @return Array of ClassBundle objects
     */
    public ClassBundle[] getInterfaceBundles() {
        if(interfaceBundle==null) {
            ClassBundle cb = new ClassBundle(DEFAULT_SERVICE_INTERFACE);
            if(componentBundle!=null && componentBundle.getArtifact()==null)
                cb.setJARs(DEFAULT_EXPORT_JARS);
            interfaceBundle = new ClassBundle[]{cb};
        }
        
        ClassBundle[] bundle = new ClassBundle[interfaceBundle.length];
        System.arraycopy(interfaceBundle, 0, bundle, 0, interfaceBundle.length);
        return (bundle);
    }

    /**
     * Set the Component as ClassBundle object containing the className and
     * resources required to load the component
     * 
     * @param bundle ClassBundle for the component
     */
    public void setComponentBundle(ClassBundle bundle) {
        componentBundle = bundle;
    }

    /**
     * Get the Component as ClassBundle object containing the className and
     * resources required to load the component
     * 
     * @return ClassBundle for the component
     */
    public ClassBundle getComponentBundle() {
        /*
        if(componentBundle!=null) {
            if(componentBundle.getCodebase()==null)
                componentBundle.setCodebase(getCodebase());
        }
        */
        return (componentBundle);
    }

    /**
     * Set the maintain value
     * 
     * @param maintain The number of service instances to maintain
     */
    public void setMaintain(String maintain) {
        /* A test to see if the value is of the correct format */
        int value = Integer.parseInt(maintain);
        if(value<0)
            throw new IllegalArgumentException("Illegal Maintain value " +
                                               "["+maintain+"], " +
                                               "must be > 0");
        this.maintain = maintain;
    }

    /**
     * Get the maintain value
     * 
     * @return The number of services to maintain. If not set this value will
     * be null
     */
    public String getMaintain() {
        return (maintain);
    }

    /**
     * Set the maxPerMachine value
     * 
     * @param maxPerMachine The maximum number of service instances per
     * machine
     */
    public void setMaxPerMachine(String maxPerMachine) {
        if(maxPerMachine==null)
            return;
        if(maxPerMachine.length()==0)
            return;        
        /* A test to make sure it is an Integer */
        int value = Integer.parseInt(maxPerMachine);
        if(value < -1)
            throw new IllegalArgumentException("Illegal MaxPerMachine value "+
                                               "["+maxPerMachine+"], "+
                                               "must be > -1");
        this.maxPerMachine = maxPerMachine;
    }

    /**
     * Get the maxPerMachine value
     * 
     * @return The maximum number of service instances per machine. If the
     * maxPerMachine value is 0, then return -1
     */
    public String getMaxPerMachine() {
        return((maxPerMachine==null?"-1": maxPerMachine));
    }

    /**
     * Set the machine boundary value
     *
     * @param boundary The boundary, "physical" or "virtual"
     */
    public void setMachineBoundary(String boundary) {
        if(!(boundary.equals("physical") || boundary.equals("virtual")))
            throw new IllegalArgumentException("bad value for machine boundary " +
                                               "["+boundary+"], must be either " +
                                               "physical or virtual");
        machineBoundary =
            (boundary.equals("physical")?MachineBoundary.PHYSICAL:
             MachineBoundary.VIRTUAL);
    }

    /**
     * Get the machine boundary value
     *
     * @return The machine boundary
     */
    public MachineBoundary getMachineBoundary() {
        return(machineBoundary==null?MachineBoundary.VIRTUAL:machineBoundary);
    }

    /**
     * Set whether the provisioning system should match on the service's name as
     * well as it's interfaces
     *
     * @param value If "yes", then true, otherwise false
     */
    public void setMatchOnName(String value) {
        useName = "yes".equals(value);
    }

    /**
     * Set whether the provisioning system should match on the service's name as
     * well as it's interfaces
     * 
     * @param value Boolean value, true or false
     */
    public void setMatchOnName(boolean value) {
        useName = value;
    }

    /**
     * Get whether the provisioning system whould match on the service's name as
     * well as it's interfaces
     * 
     * @return If true then match on name as well as interfaces,
     * othwerwise just match on interfaces
     */
    public Boolean getMatchOnName() {
        return useName;
    }

    /**
     * Set the provisioning type of the service
     * 
     * @param value If the value is "dynamic", then the type maps to
     * ServiceElement.ProvisionType.DYNAMIC, if "fixed", then
     * ServiceElement.ProvisionType.FIXED, if "external"
     * then ServiceElement.ProvisionType.EXTERNAL, otherwise throw an
     * IllegalArgumentException
     */
    public void setProvisionType(String value) {
        if (DYNAMIC.equals(value))
            provisionType = ProvisionType.DYNAMIC;
        else if (FIXED.equals(value))
            provisionType = ProvisionType.FIXED;
        else
            throw new IllegalArgumentException(value+" : unknown provision type");
    }

    /**
     * Get the provisioning type of the service
     * 
     * @return Either ServiceElement.DYNAMIC,
     * ServiceElement.FIXED or ServiceElement.EXTERNAL
     */
    public ProvisionType getProvisionType() {
        return(provisionType);
    }

    public void setStagedData(StagedData... stagedData) {
        if(stagedData !=null)
            this.stagedData.addAll(Arrays.asList(stagedData));
    }

    public StagedData[] getStagedData() {
        return stagedData.toArray(new StagedData[stagedData.size()]);
    }

    public ExecDescriptor getExecDescriptor() {
        return execDescriptor;
    }

    public void setExecDescriptor(ExecDescriptor execDescriptor) {
        this.execDescriptor = execDescriptor;
    }

    /**
     * Set the ServiceLevelAgreements
     *
     * @param sla The ServiceLevelAgreements for the service
     */
    public void setServiceLevelAgreements(ServiceLevelAgreements sla) {
        this.slaAgreements = sla;
    }

    /**
     * Get the ServiceLevelAgreements
     *
     * @return The ServiceLevelAgreements for the service
     */
    public ServiceLevelAgreements getServiceLevelAgreements() {
        if(slaAgreements == null)
            slaAgreements = new ServiceLevelAgreements();
        return (slaAgreements);
    }

    public Collection<RuleMap> getRuleMaps() {
        return ruleMaps;
    }

    public void setRuleMaps(Collection<RuleMap> ruleMaps) {
        this.ruleMaps = ruleMaps;
    }

    public boolean getFork() {
        return fork;
    }

    public void setFork(boolean fork) {
        this.fork = fork;
    }

    /**
     * Provide a String output 
     */
    public String toString() {
        /*                           
         Vector associations =    
         ClassBundle fdhBundle;
         String[] fdhConfig;    
         ServiceLevelAgreements slaAgreements; 
         */
        StringBuffer buffer = new StringBuffer();
        buffer.append("ParsedService : ").append(name).append("\n");
        buffer.append("Use Name      : ").append(useName).append("\n");
        buffer.append("JMX Name      : ").append(jmxName == null ? "<null>" : jmxName).append("\n");
        buffer.append("OpString      : ").append(opStringName).append("\n");
        buffer.append("Maintain      : ").append(maintain).append("\n");
        buffer.append("MaxPerMachine : ").append(maxPerMachine).append("\n");
        buffer.append("ProvisionType : ").append(provisionType).append("\n");
        buffer.append("AutoAdvertise : ").append(autoAdvertise).append("\n");
        buffer.append("Comment       : ").append(comment).append("\n");
        buffer.append("Codebase      : ").append(codebase).append("\n");
        buffer.append("Organization  : ").append(organization).append("\n");
        buffer.append("Fork  : ").append(fork).append("\n");
        buffer.append("Groups\n");
        String[] g = getGroups();
        for (String aG : g)
            buffer.append("\t").append(aG).append("\n");
        buffer.append("Locators\n");
        String[] l = getLocators();
        for (String aL : l)
            buffer.append("\t").append(aL).append("\n");

        buffer.append("Implementation\n");
        if (componentBundle != null) {
            buffer.append("\t").append(componentBundle.getClassName()).append("\n");
            String[] jars = componentBundle.getJARNames();
            for (String jar : jars)
                buffer.append("\t\t").append(jar).append("\n");
        } else {
            buffer.append("\tnull\n");
        }
        buffer.append("Interfaces\n");
        if (interfaceBundle != null) {
            for (ClassBundle anInterfaceBundle : interfaceBundle) {
                buffer.append("\t")
                    .append(anInterfaceBundle == null ? "???" : anInterfaceBundle.getClassName())
                    .append("\n");
                String[] jars = anInterfaceBundle == null ? new String[] {} : anInterfaceBundle.getJARNames();
                for (String jar : jars)
                    buffer.append("\t\t").append(jar).append("\n");
            }
        } else {
            buffer.append("\tnull\n");
        }
        buffer.append("Parameters\n");
        Set keys = initParms.keySet();
        for (Object key1 : keys) {
            String key = (String) key1;
            String value = (String) initParms.get(key);
            buffer.append("\tName=")
                .append(key)
                .append(" Value=")
                .append(value)
                .append("\n");
        }
        buffer.append("Configuration\n");
        String[] config = getConfigParameters();
        for (String aConfig : config)
            buffer.append("\t").append(aConfig).append("\n");
        buffer.append("Cluster\n");
        for (String aCluster : cluster)
            buffer.append("\t").append(aCluster).append("\n");
        buffer.append("Logging\n");
        for (LoggerConfig logConfig : logConfigs) {
            buffer.append(logConfig.toString()).append("\n");
        }

        buffer.append("ServiceLevelAgreements\n");
        ServiceLevelAgreements slaAgreements = getServiceLevelAgreements();
        if(slaAgreements!=null) {
            SystemComponent[] sysReqs =
                slaAgreements.getSystemRequirements().getSystemComponents();
            if(sysReqs.length>0) {
                buffer.append("SystemRequirements:\n");
                for (SystemComponent sysReq : sysReqs) {
                    buffer.append(sysReq.toString());
                }
                buffer.append("\n");
            } else {
                buffer.append("\tNo SystemRequirements\n");
            }
            SLA[] slas = slaAgreements.getServiceSLAs();
            if(slas.length > 0) {
                buffer.append("SLAs:\n");
                for (SLA sla : slas) {
                    buffer.append('\t').append(sla.toString());
                }
                buffer.append("\n");
            } else {
                buffer.append("\tNo SLAs\n");
            }
        } else {
            buffer.append("\tnull\n");
        }
        buffer.append("\nServiceProvisionConfig\n");
        String[] provisionConfig = getServiceProvisionConfig();
        for (String aProvisionConfig : provisionConfig)
            buffer.append("\t").append(aProvisionConfig).append("\n");

        buffer.append("FaultDetectionHandler\n");
        ClassBundle fdh = getFaultDetectionHandlerBundle();
        if(fdh==null) {
            buffer.append("\tNo FaultDetectionHandler\n");
        } else {
            buffer.append("\t").append(fdh.getClassName()).append("\n");
        }
        buffer.append("Associations\n");
        AssociationDescriptor[] ads = getAssociationDescriptors();
        if(ads.length==0) {
            buffer.append("\tnone declared\n");
        } else {
            for (AssociationDescriptor ad : ads)
                buffer.append("\t").append(ad.toString()).append("\n");
        }

        buffer.append("Staged Data\n");
        StagedData[] datas = getStagedData();
        if(datas.length==0) {
            buffer.append("\tnone declared\n");
        } else {
            for (StagedData data : datas)
                buffer.append("\t").append(data.toString()).append("\n");
        }
        buffer.append("Exec\n");
        ExecDescriptor exec = getExecDescriptor();
        if (exec == null) {
            buffer.append("\tnone declared\n");
        } else {
            buffer.append("\t").append(exec.toString()).append("\n");
        }

        return buffer.toString();
    }
}
