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
import org.rioproject.deploy.StagedData;
import org.rioproject.deploy.SystemComponent;
import org.rioproject.exec.ExecDescriptor;
import org.rioproject.resolver.RemoteRepository;
import org.rioproject.sla.RuleMap;
import org.rioproject.sla.ServiceLevelAgreements;

import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

/**
 * The ServiceElement object provides context on how to provision and instantiate 
 * services in the Rio architecture.
 *
 * @author Dennis Reedy
 */
public class ServiceElement implements Serializable {
    @SuppressWarnings("unused")
    static final long serialVersionUID = 1L;
    public enum ProvisionType {
        /**
         * The EXTERNAL type indicates that the ServiceElement is not a
         * provisionable component and must be instantiated using external
         * mechanisms */
        EXTERNAL,
        /**
         * The DYNAMIC type indicates that the ServiceElement will be
         * provisioned to available ServiceBeanInstantiator instances that
         * support the service's operational criteria up to the amount
         * specified by the number of planned instances. */
        DYNAMIC,
        /**
         * The FIXED type indicates that the ServiceElement will be provisioned
         * to <i>every</i> ServiceBeanInstantiator instance that supports the
         * ServiceBean's operational criteria */
        FIXED}

    /** Provision type, default to DYNAMIC */
    private ProvisionType provisionType = ProvisionType.DYNAMIC;
    /** The ServiceBeanConfig, providing configuration information for the 
     * ServiceBean */
    private ServiceBeanConfig sbConfig;    
    /** Array of AssociationDescriptor instances, describing associations the 
     * ServiceElement has to other services */
    private final List<AssociationDescriptor> associations = new ArrayList<AssociationDescriptor>();
    /** The ClassBundle for the ServiceBean */
    private ClassBundle componentBundle;
    /** Array of ClassBundles containing the export codebase */
    private ClassBundle[] exportBundles = new ClassBundle[0];
    /** Collection of provisionable PlatformCapability objects  */
    private final Collection<SystemComponent> provisionableCapabilities = new ArrayList<SystemComponent>();
    /** The ServiceLevelAgreements object defines system and service level 
     * objectives that are to be monitored, metered and acted on by policy 
     * handlers */
    private ServiceLevelAgreements slAgreements;
    /** Use the name in addition to public interfaces to track the service */
    private boolean matchOnName = true;
    /** Whether to automatically advertise the service as part of service
     * instantiation */
    private boolean autoAdvertise = true;
    /**
     * Whether to acquire the DiscoveryManagement for the service from a shared
     * pool of DiscoveryManagement instances
     */
    private boolean discoPool = true;
    /** Number of planned instances */
    private int planned;
    /** Maximum number of services per machine. Setting to -1 means no maximum */
    private int maxPerMachine = -1;
    public enum MachineBoundary {
        /**
         * Declares that the machine boundary between service instances is at
         * the JVM */
        VIRTUAL,
        /**
         * Declares that the machine boundary between service instances is at
         * the physical (machine) level */
        PHYSICAL
    }
    /**
     * The machine boundary property, only used if the maxPerMachine property
     * is set
     */
    private MachineBoundary machineBoundary=MachineBoundary.VIRTUAL;
    /** Number of actual instances */
    private int actual;    
    /** Array of machines that have been identified as part of a cluster of
     * machine used as targets for provisioning */
    private String[] machineCluster;
    /** FaultDetectionHandler ClassBundle */
    private ClassBundle fdhBundle;
    /** ExecDescriptor, providing attributes for an external service to execute */
    private ExecDescriptor execDescriptor;
    /** Collection of artifacts to download for the service */
    private final List<StagedData> stagedData = new ArrayList<StagedData>();
    /** Whether the service executes in it's own JVM */
    private boolean fork = false;
    private final List<RemoteRepository> remoteRepositories = new ArrayList<RemoteRepository>();
    private final List<RuleMap> ruleMaps = new ArrayList<RuleMap>();

    /**
     * Construct a ServiceElement
     */
    public ServiceElement() {
        this(ProvisionType.DYNAMIC);
    }

    /**
     * Construct a ServiceElement
     *
     * @param provisionType The {@link ProvisionType} for the ServiceElement
     */
    public ServiceElement(ProvisionType provisionType) {
       this.provisionType = provisionType;
    }


    /**
     * Construct a ServiceElement
     *
     * @param provisionType The {@link ProvisionType} for the ServiceElement
     * @param sbConfig The ServiceBeanConfig to set
     * @param slAgreements Attributes relating to SLAs
     * @param exports ClassBundle[] of JARs to use as the export codebase
     */
    public ServiceElement(ProvisionType provisionType,
                          ServiceBeanConfig sbConfig,
                          ServiceLevelAgreements slAgreements,
                          ClassBundle[] exports) {

        this(provisionType, sbConfig, slAgreements, exports, null, null);
    }

    /**
     * Construct a ServiceElement
     * 
     * @param provisionType The {@link ProvisionType} for the ServiceElement
     * @param sbConfig The ServiceBeanConfig to set
     * @param slAgreements Attributes relating to SLAs
     * @param exports ClassBundle[] of JARs to use as the export codebase
     * @param fdhBundle ClassBundle for the FaultDetectionHandler
     * @param componentBundle The ClassBundle for the ServiceBean component
     */
    public ServiceElement(ProvisionType provisionType,
                          ServiceBeanConfig sbConfig,
                          ServiceLevelAgreements slAgreements,
                          ClassBundle[] exports,
                          /* Optional args */
                          ClassBundle fdhBundle,
                          ClassBundle componentBundle) {

        if(sbConfig==null)
            throw new IllegalArgumentException("sbConfig is null");
        if(exports == null)
            throw new IllegalArgumentException("exports is null");
        this.fdhBundle = fdhBundle;
        this.provisionType = provisionType;
        exportBundles = new ClassBundle[exports.length];
        System.arraycopy(exports, 0, exportBundles, 0, exports.length);        
        this.componentBundle = componentBundle;
        this.sbConfig = sbConfig;
        this.slAgreements = slAgreements;
    }

    /**
     * Set the provision type set for this service.
     *
     * @param provisionType The provision type
     */
    public void setProvisionType(ProvisionType provisionType) {
        this.provisionType = provisionType;
    }

    /**
     * Get the provision type set for this service.
     *
     * @return The provision type
     */
    public ProvisionType getProvisionType() {
        return provisionType;
    }

    /**
     * Get the name of the ServiceElement
     * 
     * @return The name of the ServiceElement
     */
    public String getName() {
        if(sbConfig==null)
            return null;
        return(sbConfig.getName());
    }        

    /**
     * Set the number of instances of this service that should exist on the
     * network.
     * 
     * @param planned The number of planned instances
     *
     * @throws IllegalArgumentException if planned is less then 0
     */
    public void setPlanned(int planned) {
        if(planned<0)
            throw new IllegalArgumentException("planned cannot be < 0");
        this.planned = planned;
    }

    /**
     * Increment the number of instances of this service that should exist on
     * the network by one
     */
    public void incrementPlanned() {
        planned++;
    }

    /**
     * Decrement the number of instances of this service that should exist on
     * the network by one. If the planned attribute is zero, this operational
     * does nothing
     */
    public void decrementPlanned() {
        if(planned > 0)
            planned--;
    }

    /**
     * Get the number of instances of this service that should exist on the
     * network.
     * 
     * @return int The number of instances of this service that should exist on
     * the network
     */
    public int getPlanned() {
        return planned;
    }

    /**
     * Set the actual
     * 
     * @param actual The amount of services discovered
     */
    public void setActual(int actual) {
        this.actual = actual;
    }

    /**
     * Get the actual amount of services discovered
     *
     * @return The actualt number of discovered instances
     */
    public int getActual() {
        return actual;
    }

    /**
     * Set the maximum number of instances of this service that should exist on
     * any given machine
     * 
     * @param maxPerMachine The maximum number of service that should exist on
     * any given machine
     *
     * @throws IllegalArgumentException if maxPerMachine is less then -1
     */
    public void setMaxPerMachine(int maxPerMachine) {
        if(maxPerMachine < -1)
            throw new IllegalArgumentException("maxPerMachine cannot be less then -1");
        this.maxPerMachine = maxPerMachine;
    }

    /**
     * Get the maximum number of instances of this service that should exist on
     * any given machine
     * 
     * @return The maximum number of service that should exist on any
     * given machine. If the maxPerMachine value has not been set, the value
     * will be -1
     */
    public int getMaxPerMachine() {
        return maxPerMachine;
    }

    /**
     * Set the machine boundary. This property, when used in conjunction with
     * the {@link ServiceElement#maxPerMachine} property, sets the boundary
     * affinity between servce instances to be either at the virtual machine or
     * the physical machine
     *
     * @param boundary The machine boundary.
     */
    public void setMachineBoundary(MachineBoundary boundary) {
        machineBoundary = boundary;
    }

    /**
     * Get the machine boundary
     *
     * @return The machine boundary. Only used in conjunction with
     * the {@link ServiceElement#maxPerMachine} property
     */
    public MachineBoundary getMachineBoundary() {
        return machineBoundary;
    }
    
    /**
     * Set the machineCluster property
     * 
     * @param machineCluster The cluster of targeted machine(s) to provision
     * the ServiceBean to. Array elements should be either the hostname or an IP
     * Address of a machine.
     */
    public void setCluster(String... machineCluster) {
        this.machineCluster = machineCluster;
    }

    /**
     * Get the cluster of targeted machine(s) to provision the ServiceBean to.
     * Array elements will be either the hostname or an IP Address of a machine.
     * 
     * @return An array of machines to provision the ServiceBean to
     */
    public String[] getCluster() {
        if(machineCluster == null)
            return new String[0];
        String[] cluster = new String[machineCluster.length];
        System.arraycopy(machineCluster, 0, cluster, 0, machineCluster.length);
        return cluster;
    }

    /**
     * Set the FaultDetectionHandler ClassBundle
     * 
     * @param fdhBundle  The ClassBundle providing attributes on the
     * FaultDetectionHandler to use
     */
    public void setFaultDetectionHandlerBundle(ClassBundle fdhBundle) {
        this.fdhBundle = fdhBundle;
    }

    /**
     * Get the FaultDetectionHandler ClassBundle
     *
     * @return The ClassBundle providing attributes on the
     * FaultDetectionHandler to use
     */
    public ClassBundle getFaultDetectionHandlerBundle() {
        return fdhBundle;
    }
       
    /**
     * This method sets the instance of <code>ServiceBeanConfig</code> as 
     * an attribute of the <code>ServiceElement</code> object.
     *
     * @param sbConfig The ServiceBeanConfig to set
     */
    public void setServiceBeanConfig(ServiceBeanConfig sbConfig) {
        if(sbConfig==null)
            throw new IllegalArgumentException("sbConfig is null");
        this.sbConfig = sbConfig;
    }

    /**
     * This method returns an instance of the <code>ServiceBeanConfig</code> 
     * object created for this service as part of this Operational String<br>
     *
     * @return The ServiceBeanConfig object for this ServiceElement
     */
    public ServiceBeanConfig getServiceBeanConfig() {
        return sbConfig;
    }    
    
    /**
     * Get the name of the OperationalString
     * 
     * @return Name of the OperationalString
     */
    public String getOperationalStringName() {
        if(sbConfig==null)
            return null;
        return sbConfig.getOperationalStringName();
    }

    /**
     * Set the name of the OperationalString. This will also set the
     * OperationalString name into
     * {@link org.rioproject.associations.AssociationDescriptor}s that are set
     * to discover intra-OperationalString services
     *
     * @param name Name of the OperationalString
     *
     * @throws IllegalArgumentException if the name is null
     */
    public void setOperationalStringName(String name) {
        if(name==null)
            throw new IllegalArgumentException("name cannot be null");
        String oldName = getOperationalStringName();
        sbConfig.setOperationalStringName(name);
        for(AssociationDescriptor aDesc : getAssociationDescriptors()) {
            if(aDesc.getOperationalStringName()!=null &&
                aDesc.getOperationalStringName().equals(oldName))
                aDesc.setOperationalStringName(name);
        }
    }

    /**
     * Set whether to automatically advertise the service as part of service
     * instantiation
     * 
     * @param autoAdvertise If true, automatically advertise the ServiceBean when
     * instantiated
     */
    public void setAutoAdvertise(boolean autoAdvertise) {
        this.autoAdvertise = autoAdvertise;
    }
    
    /**
     * Get whether to automatically advertise the service as part of service
     * instantiation
     * 
     * @return If true, automatically advertise the ServiceBean when
     * instantiated
     */
    public boolean getAutoAdvertise() {
        return autoAdvertise;
    }    
    
    /**
     * Set whether to acquire the DiscoveryManagement for the service from a shared
     * pool of DiscoveryManagement instances. Sharing DiscoveryManagement instances
     * results in optimizing the creation of DiscoveryManagement instances
     *
     * <p>Note: Use of shared (pooled) DiscoveryManagement instances must be used with
     * care as changing the settings of the returned DiscoveryManagement instance may 
     * present problems for other users of DiscoveryManagement instance and is not 
     * advised.
     * 
     * @param discoPool If true, get the DiscoveryManagement instance for the 
     * service from a pool of available DiscoveryManagement instances
     */
    public void setDiscoveryManagementPooling(boolean discoPool) {
        this.discoPool = discoPool;
    }
    
    /**
     * Get whether to acquire the DiscoveryManagement for the service from a shared
     * pool of DiscoveryManagement instances. Sharing DiscoveryManagement instances
     * results in optimizing the creation of DiscoveryManagement instances
     * 
     * <p>Note: Use of shared (pooled) DiscoveryManagement instances must be used with
     * care as changing the settings of the returned DiscoveryManagement instance may 
     * present problems for other users of DiscoveryManagement instance and is not 
     * advised.
     * 
     * @return If true, get the DiscoveryManagement instance for the service from a 
     * pool of available DiscoveryManagement instances
     */
    public boolean getDiscoveryManagementPooling() {
        return discoPool;
    }

    /**
     * Set whether to use the name of the service is used in addition to the 
     * interfaces implemented by the service or service proxy to track service 
     * instances. 
     * 
     * @param matchOnName If true to use the name returned by the 
     * <code>getName</code> method in addition to the interfaces implemented by 
     * the service or service proxy to track service instances.
     */
    public void setMatchOnName(boolean matchOnName) {
        this.matchOnName = matchOnName;
    }
    
    /**
     * If this method returns true then the name of the service is used in
     * addition to the interfaces implemented by the service or service proxy to
     * track service instances. If this method returns false, then only the
     * interfaces will be used.
     * 
     * @return boolean true to use the name returned by the <code>getName</code>
     * method
     */
    public boolean getMatchOnName() {
        return matchOnName;
    }
    
    /**
     * Set the component ClassBundle
     * 
     * @param componentBundle The ClassBundle identifying the class and required 
     * resources to load the ServiceBean
     */
    public void setComponentBundle(ClassBundle componentBundle) {
        this.componentBundle = componentBundle;
    }
    
    /**
     * Get the component ClassBundle
     * 
     * @return The ClassBundle identifying the class and required resources to
     * load the ServiceBean
     */
    public ClassBundle getComponentBundle() {
        return componentBundle;
    }

    /**
     * Set the the ClassBundle objects for the public interfaces the
     * service implements
     *
     * @param exports ClassBundle objects for the public interfaces the
     * service implements
     */
    public void setExportBundles(ClassBundle... exports) {
        this.exportBundles = new ClassBundle[exports.length];
        System.arraycopy(exports, 0, exportBundles, 0, exports.length);
    }

    /**
     * Get the the Array of ClassBundle objects for the public interfaces the
     * service implements
     *
     * @return Array of ClassBundle objects for the public interfaces the
     * service implements
     */
    public ClassBundle[] getExportBundles() {
        ClassBundle[] bundle;
        if(exportBundles!=null) {
            bundle = new ClassBundle[exportBundles.length];
            System.arraycopy(exportBundles, 0, bundle, 0, exportBundles.length);
        } else {
            bundle = new ClassBundle[0];
        }
        return bundle;
    }

    /**
     * Get the export URLs. Helper method to get the array of URLs from the
     * array of ClassBundle objects representing the export class(es) and search paths
     * for those classes
     * 
     * @return Array of URLs of the export classes. A new array is allocated each
     * time this method is called
     *
     * @throws MalformedURLException If the URLs are incorrect
     */
    public URL[] getExportURLs() throws MalformedURLException {
        ArrayList<URL> list = new ArrayList<URL>();
        if(exportBundles!=null) {
            for (ClassBundle exportBundle : exportBundles) {
                if(exportBundle.getCodebase()!=null) {
                    URL[] urls = exportBundle.getJARs();
                    list.addAll(Arrays.asList(urls));
                }
            }
        }
        return list.toArray(new URL[list.size()]);
    }
  
    /**
     * Set the ServiceLevelAgreements object.
     * 
     * @param slaAgreements The ServiceLevelAgreements object defines system and
     * service level objectives that are to be monitored, metered and acted on
     * by policy handlers
     */
    public void setServiceLevelAgreements(ServiceLevelAgreements slaAgreements) {
        this.slAgreements = slaAgreements;
    }

    /**
     * Returns the ServiceLevelAgreements object.
     *
     * @return The ServiceLevelAgreements object defines system and service level
     * objectives that are to be monitored, metered and acted on by policy handlers
     */
    public ServiceLevelAgreements getServiceLevelAgreements() {
        if(slAgreements==null)
            slAgreements=new ServiceLevelAgreements();
        return slAgreements;
    }

    /**
     * Set {@code AssociationDescriptor} for the service
     *
     * @param associationDescriptors {@code AssociationDescriptor} to set
     */
    public void setAssociationDescriptors(AssociationDescriptor... associationDescriptors) {
        associations.clear();
        if(associationDescriptors!=null) {
            Collections.addAll(associations, associationDescriptors);
        }
    }

    /**
     * Add {@code AssociationDescriptor} for the service
     *
     * @param associationDescriptors {@code AssociationDescriptor} to add
     */
    public void addAssociationDescriptors(AssociationDescriptor... associationDescriptors) {
        if(associationDescriptors!=null) {
            Collections.addAll(associations, associationDescriptors);
        }
    }

    /**
     * Get the associations for the service
     * 
     * @return Array of AssociationDescriptor objects. If there are no 
     * AssociationDescriptor objects, this method returns a zero-length array
     */
    public AssociationDescriptor[] getAssociationDescriptors() {
        return associations.toArray(new AssociationDescriptor[associations.size()]);
    }
    
    /**
     * Set the SystemRequirement downloads for a resource.
     * 
     * @param downloadableCapabilities The downloadableCapabilities
     */
    public void setProvisionablePlatformCapabilities(
        Collection<SystemComponent> downloadableCapabilities) {
        synchronized(provisionableCapabilities) {
            provisionableCapabilities.clear();
            provisionableCapabilities.addAll(downloadableCapabilities);
        }
    }

    /**
     * Get the provisionable SystemRequirement instances
     * 
     * @return A Collection of SystemRequirement instances which
     * need to be provisioned in order for the ServiceBean to be instantiated. A
     * new Collection is created each time this method is called. If there are
     * no provisionable SystemRequirement instances, this method returns an
     * empty Collection.
     */
    public Collection<SystemComponent> getProvisionablePlatformCapabilities() {
        Collection<SystemComponent> collection = new ArrayList<SystemComponent>();
        synchronized(provisionableCapabilities) {
            collection.addAll(provisionableCapabilities);
        }
        return collection;
    }

    /**
     * Get the execution descriptor
     *
     * @return The ExecDescriptor, providing attributes for an external
     * service to execute. May be null.
     */
    public ExecDescriptor getExecDescriptor() {
        return execDescriptor;
    }

    /**
     * Set the execution descriptor
     *
     * @param execDescriptor The ExecDescriptor, providing attributes for an
     * external service to execute.
     */
    public void setExecDescriptor(ExecDescriptor execDescriptor) {
        this.execDescriptor = execDescriptor;
    }

    /**
     * Set StagedData for the service
     *
     * @param stagedData StagedData to set. If null, the method does
     * nothing
     */
    public void setStagedData(StagedData... stagedData) {
        if(stagedData !=null)
            this.stagedData.addAll(Arrays.asList(stagedData));
    }

    /**
     * Get the StagedData for the service
     *
     * @return An array of StagedData for the service. If there is no
     * StagedData for the service, a zero-length array is returned.
     */
    public StagedData[] getStagedData() {
        return stagedData.toArray(new StagedData[stagedData.size()]);
    }

    /**
     * Get whether the service executes in it's own JVM
     *
     * @return True if the service is to be forked in it's own JVM, false
     * otherwise.
     */
    public boolean forkService() {
        return fork;
    }

    /**
     * Set whether the service executes in it's own JVM
     *
     * @param fork If true, the service will be created in it's own JVM
     */
    public void setFork(boolean fork) {
        this.fork = fork;
    }

    public RemoteRepository[] getRemoteRepositories() {
        return remoteRepositories.toArray(new RemoteRepository[remoteRepositories.size()]);
    }

    public void setRemoteRepositories(Collection<RemoteRepository> remoteRepositories) {
        if(remoteRepositories!=null)
            this.remoteRepositories.addAll(remoteRepositories);
    }

    public List<RuleMap> getRuleMaps() {
        return ruleMaps;
    }

    public void setRuleMaps(Collection<RuleMap> ruleMaps) {
        this.ruleMaps.addAll(ruleMaps);
    }

    /**
     * Override hashCode
     */
    public int hashCode() {
        int hc = 17;
        hc = 37*hc+getName().hashCode();
        hc = 37*hc+getOperationalStringName().hashCode();
        for (ClassBundle exportBundle : exportBundles)
            hc = 37 * hc + exportBundle.hashCode();
        hc = 37*hc+(componentBundle==null?0:componentBundle.hashCode());
        return(hc);
    }

    /**
     * Override equals
     */
    public boolean equals(Object obj) {
        if(this == obj)
            return true;
        if(!(obj instanceof ServiceElement)) {
            return false;
        }
        ServiceElement that = (ServiceElement)obj;        
        if(this.getName().equals(that.getName()) && 
            this.getOperationalStringName().equals(that.getOperationalStringName())) {
            /* If they exportBundles are different lengths, then the ServiceElements 
             * are not equal */            
            if(this.exportBundles.length!=that.exportBundles.length) {
                return false;
            }
            /* The ServiceElements should have the same export jar names */
            for (ClassBundle exportBundle1 : this.exportBundles) {
                boolean matched = false;
                for (ClassBundle exportBundle : that.exportBundles) {
                    if (exportBundle1.equals(exportBundle))
                        matched = true;
                }
                if (!matched) {
                    return false;
                }
            }
            /* If we've matched the exports and both ServiceElements dont have 
             * components bundles they are the same */
            if(this.componentBundle==null && that.componentBundle==null)
                return true;
            /* If the ServiceElements do have components, make sure the component
             * jar names are equal */
            if(this.componentBundle!=null && that.componentBundle!=null) {
                return this.componentBundle.equals(that.componentBundle);
            }
        }        
        return false;
    }  

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("ServiceElement");
        sb.append("{provisionType=").append(provisionType);
        sb.append(", sbConfig=").append(sbConfig);
        sb.append(", associations=").append(associations);
        sb.append(", componentBundle=").append(componentBundle);
        sb.append(", exportBundles=").append(exportBundles == null? "null": Arrays.asList(exportBundles).toString());
        sb.append(", provisionableCapabilities=").append(provisionableCapabilities);
        sb.append(", slAgreements=").append(slAgreements);
        sb.append(", matchOnName=").append(matchOnName);
        sb.append(", autoAdvertise=").append(autoAdvertise);
        sb.append(", discoPool=").append(discoPool);
        sb.append(", planned=").append(planned);
        sb.append(", maxPerMachine=").append(maxPerMachine);
        sb.append(", machineBoundary=").append(machineBoundary);
        sb.append(", actual=").append(actual);
        sb.append(", machineCluster=").append(machineCluster == null? "null": Arrays.asList(machineCluster).toString());
        sb.append(", fdhBundle=").append(fdhBundle);
        sb.append(", execDescriptor=").append(execDescriptor);
        sb.append(", stagedData=").append(stagedData);
        sb.append(", fork=").append(fork);
        sb.append('}');
        return sb.toString();
    }

}

