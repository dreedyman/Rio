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
package org.rioproject.monitor;

import com.sun.jini.admin.DestroyAdmin;
import com.sun.jini.start.ClassLoaderUtil;
import net.jini.admin.Administrable;
import net.jini.config.Configuration;
import net.jini.config.ConfigurationProvider;
import net.jini.config.EmptyConfiguration;
import net.jini.constraint.BasicMethodConstraints;
import net.jini.core.constraint.ConnectionRelativeTime;
import net.jini.core.constraint.InvocationConstraints;
import net.jini.core.constraint.MethodConstraints;
import net.jini.core.constraint.RemoteMethodControl;
import net.jini.core.discovery.LookupLocator;
import net.jini.core.entry.Entry;
import net.jini.core.lookup.ServiceID;
import net.jini.core.lookup.ServiceItem;
import net.jini.core.lookup.ServiceTemplate;
import net.jini.discovery.DiscoveryGroupManagement;
import net.jini.discovery.DiscoveryLocatorManagement;
import net.jini.discovery.DiscoveryManagement;
import net.jini.id.ReferentUuid;
import net.jini.id.Uuid;
import net.jini.id.UuidFactory;
import net.jini.io.MarshalledInstance;
import net.jini.lease.LeaseRenewalManager;
import net.jini.lookup.LookupCache;
import net.jini.lookup.ServiceDiscoveryEvent;
import net.jini.lookup.ServiceDiscoveryManager;
import net.jini.lookup.entry.Name;
import net.jini.security.BasicProxyPreparer;
import net.jini.security.ProxyPreparer;
import org.rioproject.associations.AssociationDescriptor;
import org.rioproject.associations.AssociationType;
import org.rioproject.deploy.*;
import org.rioproject.entry.ComputeResourceInfo;
import org.rioproject.opstring.ServiceElement.ProvisionType;
import org.rioproject.admin.ServiceBeanAdmin;
import org.rioproject.deploy.DeployedService;
import org.rioproject.deploy.ServiceRecord;
import org.rioproject.deploy.ServiceStatement;
import org.rioproject.fdh.FaultDetectionHandler;
import org.rioproject.fdh.FaultDetectionHandlerFactory;
import org.rioproject.fdh.FaultDetectionListener;
import org.rioproject.admin.MonitorableService;
import org.rioproject.jsb.ServiceElementUtil;
import org.rioproject.loader.ClassBundleLoader;
import org.rioproject.monitor.ServiceChannel.ServiceChannelEvent;
import org.rioproject.monitor.util.LoggingUtil;
import org.rioproject.opstring.*;
import org.rioproject.resources.client.DiscoveryManagementPool;
import org.rioproject.resources.client.ServiceDiscoveryAdapter;
import org.rioproject.resources.servicecore.ServiceResource;
import org.rioproject.resources.util.ThrowableUtil;
import org.rioproject.sla.SLA;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.rmi.MarshalledObject;
import java.rmi.NoSuchObjectException;
import java.rmi.RemoteException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * ServiceElementManager is used to manage the discovery and dispatch
 * (provision) request for a Service within an OperationalString
 *
 * @author Dennis Reedy
 */
public class ServiceElementManager implements InstanceIDManager {
    /** The ServiceElement */
    private ServiceElement svcElement;
    private final Object svcElementRWLock = new Object();
    /** The number of services to maintain */
    private int maintain=0;
    /** The number of services to maintain, as initially set when this manager
     * had it's ServiceElement set */
    private int initialMaintain=0;
    /** A client listening for Service discovery */
    private ServiceDiscoveryManager sdm;
    /** The LookupCache for the ServiceDiscoveryManager */
    LookupCache lCache;
    /** The interfaces used to discover the service */
    private Class[] interfaces;
    /** Utility used to send provision requests */
    private ServiceProvisioner provisioner;
    /** Shutdown mode */
    private boolean shutdown=false;
    /** A collection of known services */
    private Vector services = new Vector();
    /** A table of services that have been provisioned, but whose proxies
     * (or stubs) do not support the MonitorableService interface */
    private final Map<Object, String> ambiguousServices = new Hashtable<Object, String>();
    /** Whether this ServiceElementManager has been started */
    private boolean svcManagerStarted = false;
    /** The OperationalStringManager for the ServiceElementManager */
    private OperationalStringManager opStringMgr;
    /** Is informed that a service has been provisioned successfully */
    private ProvisionListener listener = new ServiceBeanProvisionListener();
    /** Is informed if a service is detected to have failed */
    private ServiceFaultListener serviceFaultListener =
                                                   new ServiceFaultListener();
    /** Table of service IDs to FaultDetectionHandler instances, one for each
     * service */
    private final Map<ServiceID, FaultDetectionHandler> fdhTable = new Hashtable<ServiceID, FaultDetectionHandler>();
    /** A List of ServiceBeanInstances */
    private final List<ServiceBeanInstance> serviceBeanList = new ArrayList<ServiceBeanInstance>();
    /** A List of ServiceBeanInstances which have been decremented and are not
     * part of the list of ServiceBeanInstances. If a service is decremented,
     * the ServiceBeanInstance decremented is taken from the seviceBeanList
     * and placed onto this list until that service has terminated, upon which
     * the instance will be 'cleaned' from the system */
    private final List<ServiceBeanInstance> decrementedServiceBeanList = new ArrayList<ServiceBeanInstance>();
    /** A List of ProvisionRequest instances correlating to redeploy requests */
    private final List<ProvisionRequest> redeployRequestList =
        Collections.synchronizedList(new ArrayList<ProvisionRequest>());
    /** Property that indicates the mode of the ServiceElementManager. If
     * active is true, the ServiceElementManager will actively provision
     * services based on attributes set in the ServiceElementManager. If active
     * is false, the ServiceElementManager will keep track of the service
     * described by the ServiceElement but not issue provision requests to the
     * ServiceProvisioner unless the active flag is set to true. This property
     * defaults to true */
    private boolean active=true;
    /** Configuration object passed from ProvisionMonitor */
    private Configuration config;
    /** Service specific provisioning config */
    Configuration serviceProvisionConfig;
    /** The Uuid of the ProvisionMonitorImpl. If the uuid of a discovered
     * service matches our uuid, dont spend the overhead of creating a
     * FaultDetectionHandler */
    private Uuid myUuid;
    /** LookupCache listener */
    private ServiceElementManagerServiceListener sElemListener;
    /** Event source */
    ProvisionMonitor eventSource;
    /** Event processor */
    ProvisionMonitorEventProcessor eventProcessor;
    InstanceIDManager instanceIDMgr;
    /** Collection of known/allocated instance IDs.  */
    final List<Long> instanceIDs = Collections.synchronizedList(new ArrayList<Long>());
    /** A ProxyPreparer for discovered services */
    ProxyPreparer proxyPreparer;
    ServiceChannelClient serviceChannelClient = new ServiceChannelClient();
    /** Logger instance */
    static Logger logger = Logger.getLogger("org.rioproject.monitor");
    /** Logger instance for ServiceElementManager details */
    static Logger mgrLogger = Logger.getLogger("org.rioproject.monitor.services");
    /** Logger instance for ServiceBeanInstance tracking */
    static Logger sbiLogger = Logger.getLogger("org.rioproject.monitor.sbi");
    /** Used to access service provisioning configuration */
    static final String SERVICE_PROVISION_CONFIG_COMPONENT="service.provision";

    /**
     * Construct a ServiceElementManager
     *
     * @param sElem The ServiceElement
     * @param opStringMgr The OperationalStringManager for the ServiceElement
     * @param provisioner The ServiceProvisioner
     * @param uuid The Uuid of the ProvisionMonitorImpl. If the uuid of a 
     * discovered service matches our uuid, dont spend the overhead of creating
     * a FaultDetectionHandler
     * @param active Specifies the mode of the ServiceElementManager. If active
     * is true, the ServiceElementManager will actively provision services based
     * on attributes set in the ServiceElementManager. If active is false, the
     * ServiceElementManager will keep track of the service described by the 
     * ServiceElement but not issue provision requests to the ServiceProvisioner 
     * unless the active flag is set to true
     * @param config Configuration object
     *
     * @throws Exception if errors occur
     */
    ServiceElementManager(ServiceElement sElem,
                          OperationalStringManager opStringMgr,
                          ServiceProvisioner provisioner,
                          Uuid uuid,
                          boolean active,
                          Configuration config)  throws Exception {
        if(sElem==null)
            throw new NullPointerException("sElem is null");
        if(opStringMgr==null)
            throw new NullPointerException("opStringMgr is null");
        if(provisioner==null)
            throw new NullPointerException("provisioner is null");
        if(uuid==null)
            throw new NullPointerException("uuid is null");
        this.opStringMgr = opStringMgr;
        this.provisioner = provisioner;
        this.myUuid = uuid;
        this.active = active;
        this.config = config;
        instanceIDMgr = this;
        setServiceElement(sElem);
    }

    /**
     * Set the ServiceElement for the ServiceElementManager
     *
     * @param newElem The ServiceEle
     *
     * @throws Exception if there are errors setting the element
     */
    void setServiceElement(ServiceElement newElem) throws Exception {
        if(newElem==null)
            throw new NullPointerException("sElem is null");
        boolean update = (this.svcElement != null);
        ServiceElement preElem = svcElement;
        this.svcElement = newElem;

        ServiceChannel channel = ServiceChannel.getInstance();
        channel.unsubscribe(serviceChannelClient);
        AssociationDescriptor[] aDesc = svcElement.getAssociationDescriptors();
        for (AssociationDescriptor anADesc : aDesc) {
            if (anADesc.getAssociationType() == AssociationType.COLOCATED) {
                channel.subscribe(serviceChannelClient,
                                  anADesc.getName(),
                                  anADesc.getInterfaceNames(),
                                  anADesc.getOperationalStringName());
            }
        }

        ServiceBeanConfig sc = svcElement.getServiceBeanConfig();
        Map<String, Object> configParameters = sc.getConfigurationParameters();

        String[] args = (String[])configParameters.get(ServiceBeanConfig.SERVICE_PROVISION_CONFIG);
        MethodConstraints serviceListenerConstraints=
                new BasicMethodConstraints(new InvocationConstraints(new ConnectionRelativeTime(30000), null));
        ProxyPreparer defaultProxyPreparer =  new BasicProxyPreparer(false, serviceListenerConstraints, null);
        if(args==null) {
            proxyPreparer = defaultProxyPreparer;
        } else {
            serviceProvisionConfig = ConfigurationProvider.getInstance(args);
            proxyPreparer = (ProxyPreparer)serviceProvisionConfig.getEntry(SERVICE_PROVISION_CONFIG_COMPONENT,
                                                                           "proxyPreparer",
                                                                           ProxyPreparer.class,
                                                                           defaultProxyPreparer);
        }

        /* Get the Class[] of interfaces to discover, simple for loop,
         * execute it twice if this is an update and the codebase has issues */
        Exception toThrow = null;
        for(int i=0; i<2; i++) {
            try {
                this.interfaces = loadInterfaceClasses(svcElement);
                toThrow = null;
                break;
            } catch(ClassNotFoundException e) {
                toThrow = e;
                /* If this is an update action, try to fix this by checking if
                 * the codebases are different between the old and the new
                 * ServiceElement. If so then try and repair it */
                if(update) {
                    ClassBundle[] oldBundles = preElem.getExportBundles();
                    ClassBundle[] newBundles = svcElement.getExportBundles();
                    String cb = oldBundles[0].getCodebase();
                    if(!cb.equals(newBundles[0].getCodebase())) {
                        for (ClassBundle newBundle : newBundles) {
                            newBundle.setCodebase(cb);
                        }
                    }
                } else {
                    StringBuilder sb = new StringBuilder();
                    for(URL u : svcElement.getExportURLs()) {
                        if(sb.length()>0)
                            sb.append(", ");
                        sb.append(u.toExternalForm());
                    }
                    logger.warning("Failed ClassBundle: "+sb.toString());
                    break;
                }
            }
        }
        if(toThrow!=null)
            throw toThrow;

        this.maintain = svcElement.getPlanned();
        /* Set the initial planned services */
        Integer ips =
            (Integer)svcElement.getServiceBeanConfig()
                         .getConfigurationParameters()
                         .get(ServiceBeanConfig.INITIAL_PLANNED_SERVICES);
        if(ips==null)
            setInitialPlanned(svcElement.getPlanned());


        if(update) {
            /* If there is a change in provision types, adjust
             * pending managers */
            if(svcElement.getProvisionType() != preElem.getProvisionType()) {
                if(preElem.getProvisionType() == ProvisionType.FIXED) {
                    removeFixedServiceRequests(preElem);
                } else {
                    provisioner.getPendingManager().removeServiceElement(preElem);
                }
            }
            synchronized(serviceBeanList) {
                for (ServiceBeanInstance sbi : serviceBeanList) {
                    Long instanceID =
                        sbi.getServiceBeanConfig().getInstanceID();
                    ServiceBeanConfig updated = newElem.getServiceBeanConfig();
                    Map<String, Object> configParms = updated.getConfigurationParameters();
                    configParms.put(ServiceBeanConfig.INSTANCE_ID, instanceID);
                    ServiceBeanConfig newConfig = new ServiceBeanConfig(configParms, updated.getConfigArgs());
                    ServiceBeanConfig sbc = sbi.getServiceBeanConfig();
                    Map<String, Object> initParms = sbc.getInitParameters();

                    for (Map.Entry<String, Object> e : initParms.entrySet()) {
                        newConfig.addInitParameter(e.getKey(), e.getValue());
                    }
                    sbi.setServiceBeanConfig(newConfig);
                }
            }
            /* Check if the DiscoveryManagement groups or locators have 
             * been changed */
            if(sdm!=null) {                
                /* Update groups if they have changed */
                if(ServiceElementUtil.hasDifferentGroups(preElem, newElem)) {
                    String[] groups = newElem.getServiceBeanConfig().getGroups();
                    if(mgrLogger.isLoggable(Level.FINEST)) {
                        StringBuilder buffer = new StringBuilder();
                        if(groups == DiscoveryGroupManagement.ALL_GROUPS)
                            buffer.append("ALL_GROUPS");
                        else {
                            for(int i=0; i<groups.length; i++) {
                                if(i>0)
                                    buffer.append(", ");
                                if(groups[i].equals(""))
                                    buffer.append("<public>");
                                else
                                    buffer.append(groups[i]);
                            }
                        }
                        mgrLogger.finest("["+LoggingUtil.getLoggingName(svcElement)+"] "+
                                         "Discovery has changed, setting " +
                                         "groups " +
                                         "to : "+buffer.toString());
                    }
                    DiscoveryManagement dMgr = sdm.getDiscoveryManager();
                    ((DiscoveryGroupManagement)dMgr).setGroups(groups);
                }
                /* Update locators if they have changed */
                if(ServiceElementUtil.hasDifferentLocators(preElem, newElem)) {
                    LookupLocator[] locators = newElem.getServiceBeanConfig().getLocators();
                    if(mgrLogger.isLoggable(Level.FINEST)) {
                        StringBuilder buffer = new StringBuilder();
                        if(locators==null)
                            buffer.append("null");
                        else {
                            for(int i=0; i<locators.length; i++) {
                                if(i>0)
                                    buffer.append(", ");
                                buffer.append(locators[i].toString());
                            }
                        }
                        mgrLogger.finest("["+LoggingUtil.getLoggingName(svcElement)+"] "+
                                         "Discovery has changed, setting " +
                                         "locators "+
                                         "to : "+buffer.toString());
                    }
                    DiscoveryManagement dMgr = sdm.getDiscoveryManager();
                    ((DiscoveryLocatorManagement)dMgr).setLocators(locators);
                }
            }
        }

        /* TODO: Check if the groups the service requires are available */
        if(svcManagerStarted) {
            notifyPendingManager(null);
        }
    }

    /*
     * Get the ServiceStatement, including all ServiceRecords, for the
     * ServiceElement being managed
     */
    ServiceStatement getServiceStatement() {
        ServiceStatement statement = new ServiceStatement(svcElement);
        InstantiatorResource[] resources = provisioner.getServiceResourceSelector().getInstantiatorResources(svcElement);
        for(InstantiatorResource ir : resources) {
            try {
                ServiceRecord[] records = ir.getServiceRecords(svcElement);
                for(ServiceRecord record : records)
                    statement.putServiceRecord(ir.getInstantiatorUuid(), record);
            } catch (RemoteException e) {
                Throwable cause = ThrowableUtil.getRootCause(e);
                mgrLogger.log(Level.WARNING,  "Could not obtain ServiceRecords from "+ir.getHostAddress(), cause);
            }
        }
        return statement;
    }

    /*
     * For each ServiceBeanInstance with a non-null ServiceBeanInstantiator
     * Uuid, get the ServiceBeanInstantiator's ResourceCapability
     */
    List<DeployedService> getServiceDeploymentList() {
        List<DeployedService> list = new ArrayList<DeployedService>();
        InstantiatorResource[] resources = provisioner.getServiceResourceSelector().getInstantiatorResources(svcElement);
        for(ServiceBeanInstance sbi : getServiceBeanInstances()) {
            for(InstantiatorResource ir : resources) {
                if(sbi.getServiceBeanInstantiatorID()!=null &&
                   sbi.getServiceBeanInstantiatorID().equals(
                    ir.getInstantiatorUuid())) {
                    list.add(ir.getServiceDeployment(svcElement, sbi));
                    break;
                }
            }
        }
        return list;
    }

    private void setInitialPlanned(int value) {
        initialMaintain = value;
        svcElement.setServiceBeanConfig(ServiceElementUtil.addConfigParameter(svcElement.getServiceBeanConfig(),
                                                                              ServiceBeanConfig.INITIAL_PLANNED_SERVICES,
                                                                              value));
    }

    /*
     * Load interfaces for the service
     */
    private Class[] loadInterfaceClasses(ServiceElement elem) throws MalformedURLException, ClassNotFoundException {
        ClassBundle[] exportBundles = elem.getExportBundles();
        Class[] classes = new Class[exportBundles.length];
        for(int i = 0; i < classes.length; i++) {
            classes[i] = ClassBundleLoader.loadClass(exportBundles[i]);
        }
        return(classes);
    }

    /**
     * Notify the appropriate PendingServiceElementManager
     *
     * @param provListener the ServiceProvisionListener to notify
     */
    void notifyPendingManager(ServiceProvisionListener provListener) {
        if(!active) {
            /* If the ServiceElement is dynamic and it is in the pending queue, 
             * remove ProvisionRequest instances from the PendingManager */
            if(svcElement.getProvisionType()==ProvisionType.DYNAMIC) {
                if(provisioner.getPendingManager().hasServiceElement(svcElement)) {
                    if(mgrLogger.isLoggable(Level.FINER))
                        mgrLogger.finer("Remove ["+LoggingUtil.getLoggingName(svcElement)+"] from PendingServiceManager");
                    provisioner.getPendingManager().removeServiceElement(svcElement);
                }
            }

            /* If the ServiceElement is fixed, then use the FixedServiceManager
             * to remove ProvisionRequest instances */
            if(svcElement.getProvisionType()==ProvisionType.FIXED) {
                removeFixedServiceRequests(svcElement);
            }

        } else {
            /* If the ServiceElement is dynamic and it is in the pending queue, 
             * update the ProvisionRequest instances in the PendingManager */
            if(svcElement.getProvisionType()==ProvisionType.DYNAMIC) {
                if(provisioner.getPendingManager().hasServiceElement(svcElement)) {
                    if(mgrLogger.isLoggable(Level.FINER))
                        mgrLogger.finer("Update ["+LoggingUtil.getLoggingName(svcElement)+"] in PendingServiceManager");
                    int count = provisioner.getPendingManager().getCount(svcElement);
                    if(count > svcElement.getPlanned()) {
                        int toRemove = count - svcElement.getPlanned();
                        ProvisionRequest[] removed = provisioner.getPendingManager().removeServiceElement(svcElement,
                                                                                                          toRemove);
                        for (ProvisionRequest aRemoved : removed)
                            removeInstanceID(aRemoved.sElem.getServiceBeanConfig().getInstanceID(),
                                             "removal from pending testManager");
                    } else {
                        provisioner.getPendingManager().updateProvisionRequests(svcElement, provListener);
                    }
                }
                verify(provListener);
            }

            /* If the ServiceElement is fixed, then use the FixedServiceManager
             * to update/add ProvisionRequest instances */
            if(svcElement.getProvisionType()==ProvisionType.FIXED) {
                /* If the planned value is zero, then make sure there are no
                 * pending requests (if there ar they will be removed),
                 * then return */
                if(svcElement.getPlanned()==0) {
                    removeFixedServiceRequests(svcElement);
                    return;
                }
                ProvisionRequest request = new ProvisionRequest(ServiceElementUtil.copyServiceElement(svcElement),
                                                                listener,
                                                                opStringMgr,
                                                                instanceIDMgr);
                if(provisioner.getFixedServiceManager().hasServiceElement(svcElement)) {
                    if(mgrLogger.isLoggable(Level.FINER))
                        mgrLogger.finer("Update ["+LoggingUtil.getLoggingName(svcElement)+"] instance in  FixedServiceManager");
                    provisioner.getFixedServiceManager().updateProvisionRequests(svcElement, provListener);
                }  else {
                    /* Add the ProvisionRequest so new Cybernodes that match the 
                     * requirements will have the Service provisioned */
                    provisioner.getFixedServiceManager().addProvisionRequest(request, 0);
                    if(mgrLogger.isLoggable(Level.FINER))
                        mgrLogger.finer("Add ["+LoggingUtil.getLoggingName(svcElement)+"] to FixedServiceManager");

                }
                /* Deploy to existing Cybernodes that match the requirements  */
                provisioner.getFixedServiceManager().deploy(request);
            }
        }
    }

    /**
     * Remove ServiceElement instances from the FixedServiceManager
     *
     * @param sElem The ServiceElement to remove
     */
    void removeFixedServiceRequests(ServiceElement sElem) {
        if(sElem.getProvisionType() != ProvisionType.FIXED)
            return;
        if(provisioner.getFixedServiceManager().hasServiceElement(sElem)){
            if(mgrLogger.isLoggable(Level.FINER))
                mgrLogger.finer("Remove ["+LoggingUtil.getLoggingName(sElem)+"] instances from FixedServiceManager");
            provisioner.getFixedServiceManager().removeServiceElement(sElem);
        }
    }

    /**
     * Set the property that indicates the mode of the ServiceElementManager. 
     * 
     * @param active If active is true, the ServiceElementManager will actively 
     * provision services based on attributes set in the ServiceElementManager.
     * If active is false, the ServiceElementManager will keep track of the
     * service described by the ServiceElement but not issue provision requests
     * to the ServiceProvisioner unless the active flag is set to true.
     */
    void setActive(boolean active) {
        synchronized(this) {
            if(!this.active && active) {
                ServiceBeanInstance[] instances = getServiceBeanInstances();
                /* If we have a collection of ServiceBeanInstance objects
                 * but have no known instanceIDs, we need to check known
                 * Cybernodes to fill in missing instanceIDs.
                 * 
                 * This happens if the service described by the ServiceElement
                 * does not implement the ServiceBeanAdmin interface, allowing
                 * us to obtain the ServiceBeanConfig to access the
                 * embedded instanceID 
                 */

                // TODO: IS THIS CODE STILL NEEDED? 
                if(instanceIDs.size()==0 && instances.length>0) {
                    InstantiatorResource[] resources =
                        provisioner.getServiceResourceSelector().getInstantiatorResources(svcElement);
                    for(InstantiatorResource resource : resources) {
                        try {
                            ServiceRecord[] records = resource.getActiveServiceRecords();
                            for(ServiceRecord record : records) {
                                Uuid uuid = record.getServiceID();
                                ServiceBeanConfig sbc = record.getServiceElement().getServiceBeanConfig();
                                for (ServiceBeanInstance instance : instances) {
                                    if (instance.getServiceBeanID().equals(uuid)) {
                                        instance.setServiceBeanConfig(sbc);
                                        addServiceBeanInstance(instance);
                                        Long instanceID = sbc.getInstanceID();
                                        if (instanceID != null && !instanceIDs.contains(instanceID))
                                            instanceIDs.add(instanceID);
                                        break;
                                    }
                                }
                            }
                        } catch (Throwable e) {
                            if (mgrLogger.isLoggable(Level.FINEST))
                                mgrLogger.log(Level.FINEST, "Getting active ServiceRecords", e);
                        }
                    }
                }
            }
            this.active = active;
        }
        if(mgrLogger.isLoggable(Level.INFO))
            mgrLogger.info("Set Active ["+active+"] for ["+svcElement.getName()+"]");

        notifyPendingManager(null);
    }

    /**
     * Get the ServiceElement property
     *
     * @return The ServiceElement property
     */
    public ServiceElement getServiceElement() {
        svcElement.setActual(getActual());
        return(svcElement);
    }

    /**
     * Start the ServiceElementManager, initiate service discovery
     *
     * @param provListener If not <code>null</code>, the
     * ServiceProvisionListener to be notified of initial provisioning actions
     *
     * @return The number of services that had been discovered, had already
     * been running
     *
     * @throws Exception If there are any problems starting the manager
     */
    public int startManager(ServiceProvisionListener provListener) throws Exception {
        return(startManager(provListener, new ServiceBeanInstance[0]));
    }

    /**
     * Start the ServiceElementManager, initiate service discovery
     *
     * @param provListener If not <code>null</code>, the
     * ServiceProvisionListener to be notified of initial provisioning actions
     * @param instances Known ServiceBeanInstance objects.
     * @return The number of services that had been discovered, had already
     * been running
     *
     * @throws Exception If there are any problems starting the manager
     */
    int startManager(ServiceProvisionListener provListener, ServiceBeanInstance[] instances) throws Exception {
        if(svcManagerStarted)
            return(0);
        synchronized(this) {
            if(instances!=null) {
                for (ServiceBeanInstance instance : instances) {
                    addServiceBeanInstance(instance);
                }
            }
            DiscoveryManagementPool discoPool = DiscoveryManagementPool.getInstance();
            DiscoveryManagement dm = discoPool.getDiscoveryManager(svcElement.getOperationalStringName(),
                                                                   svcElement.getServiceBeanConfig().getGroups(),
                                                                   svcElement.getServiceBeanConfig().getLocators());
            ServiceTemplate template;
            if(svcElement.getMatchOnName())
                template = new ServiceTemplate(null, interfaces, new Entry[]{new Name(svcElement.getName())});
            else
                template = new ServiceTemplate(null, interfaces, null);

            if(config==null)
                config = EmptyConfiguration.INSTANCE;
            sdm = new ServiceDiscoveryManager(dm, new LeaseRenewalManager(config), config);
            InstantiatorResource[] irArray = provisioner.getServiceResourceSelector().getInstantiatorResources(svcElement);
            List<ServiceBeanInstance> instanceList = new ArrayList<ServiceBeanInstance>();
            for (InstantiatorResource ir : irArray) {
                try {
                    ServiceBeanInstance[] sbi = ir.getInstantiator().getServiceBeanInstances(svcElement);
                    instanceList.addAll(Arrays.asList(sbi));
                } catch (RemoteException e) {
                    if(mgrLogger.isLoggable(Level.FINEST))
                        mgrLogger.log(Level.FINEST,
                                      "Unable to get ServiceBeanInstance(s) " +
                                      "from "+ir.getName()+", "+e.getClass().getName()+": "+e.getMessage());
                }
            }
            lCache = sdm.createLookupCache(template,
                                           new OpStringFilter(svcElement.getOperationalStringName()),
                                           null);

            sElemListener = new ServiceElementManagerServiceListener();
            lCache.addListener(sElemListener);
            
            ServiceBeanInstance[] sbInstances = instanceList.toArray(new ServiceBeanInstance[instanceList.size()]);

            if(sbInstances.length>0) {
                if(mgrLogger.isLoggable(Level.FINEST))
                    mgrLogger.finest("ServiceElement " +
                                     "["+LoggingUtil.getLoggingName(svcElement)+"] " +
                                     "Instantiator count="+irArray.length+", "+
                                     "ServiceBeanInstance count="
                                     +sbInstances.length+", "+
                                     "synch testManager with discovered "+
                                     "instances");
                int lastID = 0;
                for (ServiceBeanInstance sbInstance : sbInstances) {
                    Uuid uuid = sbInstance.getServiceBeanID();
                    Object proxy = sbInstance.getService();
                    if(proxy instanceof RemoteMethodControl)
                        proxy = proxyPreparer.prepareProxy(sbInstance.getService());
                    ServiceID serviceID = new ServiceID(uuid.getMostSignificantBits(), uuid.getLeastSignificantBits());
                    try {
                        setFaultDetectionHandler(proxy, serviceID);
                        addServiceProxy(proxy);
                        addServiceBeanInstance(sbInstance);
                        /* Check the instanceID, make sure we maintain the
                         * monotonically increasing requirement */
                        if (sbInstance.getServiceBeanConfig() != null) {
                            Long id = sbInstance.getServiceBeanConfig().getInstanceID();
                            if (id != null && id.intValue() > lastID)
                                lastID = id.intValue();
                        }
                    } catch(Throwable e) {
                        Throwable t = ThrowableUtil.getRootCause(e);
                        mgrLogger.warning("Unable to set FaultDetectionHandler " +
                                          "to existing instance of " +
                                          "["+LoggingUtil.getLoggingName(svcElement)+"], " +
                                          "assume service is unavailable. "+
                                          t.getClass()+": "+t.getMessage());
                    }
                }
            } else {
                if(mgrLogger.isLoggable(Level.FINEST))
                    mgrLogger.finest("ServiceElement "+
                                     "["+LoggingUtil.getLoggingName(svcElement)+"] "+
                                     "Instantiator count="+irArray.length+", "+
                                     "ServiceBeanInstance count="+
                                     sbInstances.length+", "+
                                     "provision instances");
            }

            svcManagerStarted = true;
        }
        /* If there are any pending ServiceElement requests, reset them */
        notifyPendingManager(provListener);
        return(services.size());
    }

    /*
     * Return the started state
     */
    public boolean isStarted() {
        return(svcManagerStarted);
    }

    /*
     * If the amount of discovered service instances is less then the number to 
     * maintain, initiate dispatch requests to the ServiceProvisioner
     */
    void verify(ServiceProvisionListener listener) {
        if(!active) {
            return;
        }
        int pending =
            provisioner.getPendingManager().getCount(svcElement);
        if(mgrLogger.isLoggable(Level.FINE)) {
            mgrLogger.log(Level.FINE,
                          "ServiceElementManager.verify(): " +
                          "["+LoggingUtil.getLoggingName(svcElement)+"] "+
                          "actual ["+getActual()+"], "+
                          "pending ["+pending+"], "+
                          "maintain ["+maintain+"]");
        }
        int actual = getActual()+pending;
        if(actual<maintain)
            dispatchProvisionRequests(listener);
    }

    /**
     * Destroy all discovered services this ServiceElementManager knows of
     */
    void destroyServices() {
        if(sdm==null)
            return;
        ServiceBeanInstance[] instances = getServiceBeanInstances();
        if(mgrLogger.isLoggable(Level.FINE))
            mgrLogger.log(Level.FINE,
                          "Terminating ["+instances.length+"] instances of " +
                          "["+LoggingUtil.getLoggingName(svcElement)+"] ...");
        for(int i=0; i<instances.length; i++) {
            if(mgrLogger.isLoggable(Level.FINE))
                mgrLogger.log(Level.FINE, "Destroying ["+(i+1)+"] of "+
                                          "["+instances.length+"] " +
                                          "["+LoggingUtil.getLoggingName(svcElement)+"] "+
                                          "instances ...");
            Object proxy = null;
            try {
                proxy = instances[i].getService();
            } catch(Exception e) {
                mgrLogger.log(Level.WARNING, "Getting service proxy", e);
            }
            if(proxy!=null)
                destroyService(proxy,
                               instances[i].getServiceBeanID(),
                               true);
        }
        if(mgrLogger.isLoggable(Level.FINE))
            mgrLogger.log(Level.FINE,
                          "Completed ["+svcElement.getOperationalStringName()+
                          "/"+svcElement.getName()+"] "+
                          "service termination");
    }

    /**
     * Destroy a service
     * 
     * @param service The ServiceItem of the service to destroy
     * @param serviceUuid The service Uuid
     * @param clean remove service references
     *
     * @return True if the service is destroyed
     */
    boolean destroyService(Object service, Uuid serviceUuid, boolean clean) {
        boolean terminated = false;
        try {
            Administrable admin = (Administrable)service;
            DestroyAdmin destroyAdmin = (DestroyAdmin)admin.getAdmin();
            destroyAdmin.destroy();
            terminated = true;
        } catch(Exception e) {
            //if(mgrLogger.isLoggable(Level.FINEST)) {
                mgrLogger.log(Level.WARNING, "Destroying Service ["+LoggingUtil.getLoggingName(svcElement)+"]", e);
            /*} else {
                Throwable t = ThrowableUtil.getRootCause(e);
                mgrLogger.warning("Destroying Service " +
                                  "["+LoggingUtil.getLoggingName(svcElement)+"]\n"+
                                  t.getClass().getName()+": "+
                                  t.getLocalizedMessage());
            }*/
            
            if(!ThrowableUtil.isRetryable(e)) {
                if(mgrLogger.isLoggable(Level.FINE))
                    mgrLogger.log(Level.FINE,
                                  "Force clean for " +
                                  "["+LoggingUtil.getLoggingName(svcElement)+"] "+
                                  "ServiceBeanInstance ["+
                                  serviceUuid.toString()+"]");
                clean = true;
            }
        }
        ServiceBeanInstance instance;
        if(clean)
            instance = cleanService(service, serviceUuid, true);
        else
            instance = getServiceBeanInstance(serviceUuid);
        /* Notify that a service has been terminated */
        ProvisionMonitorEvent event = new ProvisionMonitorEvent(eventSource,
                                                                ProvisionMonitorEvent.Action.SERVICE_TERMINATED,
                                                                svcElement.getOperationalStringName(),
                                                                svcElement,
                                                                instance);
        processEvent(event);
        return(terminated);
    }

    /**
     * Relocate (move) a ServiceBean to a different ServiceBeanInstantiator
     *
     * @param instance The ServiceBeanInstance
     * @param svcProvisionListener the ServiceProvisionListener. May be null
     * @param requestedUuid The requested ServiceBeanInstatiator to relocate
     * to, may be null.
     *
     * @throws OperationalStringException If both the Uuid of the
     * ServiceBeanInstantiator cannot be obtained
     */
    void relocate(ServiceBeanInstance instance,
                  ServiceProvisionListener svcProvisionListener,
                  Uuid requestedUuid) throws OperationalStringException {
        Uuid excludeUuid = null;
        ServiceResource[] resources =
            provisioner.getServiceResourceSelector().getServiceResources(instance.getHostAddress(), true);
        for (ServiceResource resource : resources) {
            InstantiatorResource ir = (InstantiatorResource) resource.getResource();
            if (ir.hasServiceElementInstance(svcElement, instance.getServiceBeanID())) {
                excludeUuid = ir.getInstantiatorUuid();
                break;
            }
        }

        if(excludeUuid==null) {
            throw new OperationalStringException("Unable to obtain the Uuid of the instantiator");
        }
        ServiceBeanInstance sbi = getServiceBeanInstance(instance.getServiceBeanID());
        if(sbi==null) {
            throw new OperationalStringException("Unable to relocate service "+
                                                 "["+LoggingUtil.getLoggingName(svcElement)+"], unknown " +
                                                 "ServiceBeanInstance");
        }
        ServiceElement newElem = ServiceElementUtil.copyServiceElement(svcElement);
        newElem.setServiceBeanConfig(instance.getServiceBeanConfig());
        ProvisionRequest request = new ProvisionRequest(newElem,
                                                        listener,
                                                        opStringMgr,
                                                        instanceIDMgr,
                                                        new RelocationListener(svcProvisionListener, instance),
                                                        sbi,
                                                        excludeUuid,
                                                        requestedUuid,
                                                        ProvisionRequest.Type.RELOCATE);
        doDispatchProvisionRequests(new ProvisionRequest[]{request});
    }

    /**
     * Update a ServiceBeanInstance
     * 
     * @param instance The ServiceBeanInstance
     *
     * @throws OperationalStringException if there are errors updating
     */
    void update(ServiceBeanInstance instance) throws OperationalStringException {
        synchronized(serviceBeanList) {
            int index = serviceBeanList.indexOf(instance);
            if(index==-1) {
                if(sbiLogger.isLoggable(Level.FINE))
                    sbiLogger.log(Level.FINE,
                                  "["+LoggingUtil.getLoggingName(svcElement)+"] "+
                                  "Adding {0} ServiceBeanInstance ID {1}",
                                  new Object[] {svcElement.getName(), instance.getServiceBeanConfig().getInstanceID()});
                serviceBeanList.add(instance);
            } else {
                if(sbiLogger.isLoggable(Level.FINE))
                    sbiLogger.log(Level.FINE,
                                  "["+LoggingUtil.getLoggingName(svcElement)+"] "+
                                  "Updating {0} ServiceBeanInstance ID {1}",
                                  new Object[]{svcElement.getName(), instance.getServiceBeanConfig().getInstanceID()});
                serviceBeanList.set(index, instance);
            }
        }
    }

    /**
     * Redeploy the ServiceElement
     * 
     * @param svcProvisionListener A ServiceProvisionListener, to be notified 
     * when the service either gets allocated or not
     */
    void redeploy(ServiceProvisionListener svcProvisionListener) {
        if(svcElement.getProvisionType()== ProvisionType.DYNAMIC) {
            provisioner.getPendingManager().updateProvisionRequests(svcElement, svcProvisionListener);
        }
        if(svcElement.getProvisionType()==ProvisionType.FIXED) {
            if(provisioner.getFixedServiceManager().hasServiceElement(svcElement)) {
                provisioner.getFixedServiceManager().updateProvisionRequests(svcElement, svcProvisionListener);
            }
        }
    }

    /**
     * Redeploy a service instance
     * 
     * @param instance The ServiceBeanInstance
     * @param clean If set to true, use the original ServiceElement
     * configuration, not any service-specific saved state
     * @param sticky If true, use the Uuid of the ServiceBeanInstatiator
     * @param svcProvisionListener A ServiceProvisionListener, to be notified 
     * when the service either gets allocated or not
     *
      * @throws OperationalStringException if there are errors redeploying
     */
    void redeploy(ServiceBeanInstance instance,
                  boolean clean,
                  boolean sticky,
                  ServiceProvisionListener svcProvisionListener)
    throws OperationalStringException {

        ServiceBeanInstance sbi =
            getServiceBeanInstance(instance.getServiceBeanID());
        if(sbi==null)
            throw new OperationalStringException("Not tracking ServiceBeanInstance");
        ServiceElement newElem =
            ServiceElementUtil.copyServiceElement(svcElement);
        if(clean) {
            Long instanceID = sbi.getServiceBeanConfig().getInstanceID();
            newElem = ServiceElementUtil.prepareInstanceID(newElem, false, instanceID.intValue());
        } else {
            newElem.setServiceBeanConfig(sbi.getServiceBeanConfig());
        }


        /* If there are pending requests, the pending requests must be updated
        * with a new ServiceElement reflecting potentially newly created
        * message digests.
        */
        if(svcElement.getProvisionType()== ProvisionType.DYNAMIC) {
            provisioner.getPendingManager().updateProvisionRequests(newElem, svcProvisionListener);
        }
        if(svcElement.getProvisionType()==ProvisionType.FIXED) {
            provisioner.getFixedServiceManager().updateProvisionRequests(newElem, svcProvisionListener);
        }

        ProvisionRequest req = new ProvisionRequest(newElem,
                                                    listener,
                                                    opStringMgr,
                                                    instanceIDMgr,
                                                    svcProvisionListener,
                                                    sbi);
        if(sticky)
            req.requestedUuid = instance.getServiceBeanInstantiatorID();

        redeployRequestList.add(req);
        Object proxy = null;
        try {
            proxy = instance.getService();
        } catch(Exception e) {
            mgrLogger.log(Level.WARNING, "Getting service for destroy invocation", e);
        }
        if(proxy!=null) {
            boolean destroyed = destroyService(proxy, instance.getServiceBeanID(), false);
            /* If we couldnt destroy the instance, a false return means the
             * service destroy invocation resulted in an unrecoverable failure,
             * we can assume we will never get a serviceFailure notification.
             * Post the redeployment request here
             */
            if(!destroyed) {
                ProvisionRequest provRequest = serviceFaultListener.getRedeploymentProvisionRequest(proxy);
                doDispatchProvisionRequests(new ProvisionRequest[]{provRequest});
            }
        }
    }

    /**
     * Determine if a ServiceBeanInstance is tracked by this
     * ServiceElementManager
     * 
     * @param instance The ServiceBeanInstance
     * 
     * @return True if the ServiceElementManager knows about the
     * ServiceBeanInstance
     */
    boolean hasServiceBeanInstance(ServiceBeanInstance instance) {
        ServiceBeanInstance[] sbs = getServiceBeanInstances();
        for (ServiceBeanInstance sb : sbs) {
            if (instance.equals(sb))
                return (true);
        }        
        return(false);
    }

    /**
     * Get the ServiceBeanInstance for a Uuid
     *
     * @param uuid The uuid
     *
     * @return A ServiceBeanInstance for the uuid, or <code>null</code> if not
     * found
     */
    ServiceBeanInstance getServiceBeanInstance(Uuid uuid) {
        ServiceBeanInstance instance = null;
        if(uuid!=null) {
            ServiceBeanInstance[] sbs = getServiceBeanInstances();
            for (ServiceBeanInstance sb : sbs) {
                if (sb.getServiceBeanID().equals(uuid)) {
                    instance = sb;
                    break;
                }
            }
        }
        return (instance);
    }

    /*
     * Replace the ServiceBeanInstance
     */
    boolean replaceServiceBeanInstance(ServiceBeanInstance instance) {
        boolean replaced = false;
        int ndx = serviceBeanList.indexOf(instance);
        if(ndx!=-1) {
            serviceBeanList.set(ndx, instance);
            replaced = true;
        }
        return(replaced);
    }

    /*
     * Get the number of in-process provisioning requests
     */
    int getPendingCount() {
        int count = -1;
        if(svcElement.getProvisionType()==ProvisionType.DYNAMIC)
            count = provisioner.getPendingManager().getCount(svcElement);
        return(count);
    }

    /**
     * Increment the number to maintain
     *
     * @param permanent Whethr the change should be set into the ServiceElement
     * @param svcProvisionListener the ServiceProvisionListener. May not be null
     * 
     * @return An updated ServiceElement. If not incremented return null
     */
    synchronized ServiceElement increment(boolean permanent, ServiceProvisionListener svcProvisionListener) {
        boolean okayToIncrement = false;
        synchronized(svcElementRWLock) {
            int planned = svcElement.getPlanned();
            StringBuilder sb = new StringBuilder();
            sb.append("INCREMENT [")
                .append(LoggingUtil.getLoggingName(svcElement))
                .append("] ")
                .append("Permanently=")
                .append(permanent)
                .append(", NUM PLANNED=")
                .append(planned)
                .append(", ");
            if(!permanent) {
                int max = getMaxServiceCount();
                sb.append("MAX COUNT=").append(max).append(", ");
                if(planned < max)
                    okayToIncrement = true;
            } else {
                okayToIncrement = true;
            }

            sb.append("okayToIncrement=").append(okayToIncrement);

            if(okayToIncrement) {
                svcElement.incrementPlanned();
                if(permanent) {
                    setInitialPlanned(svcElement.getPlanned());
                }
                maintain = svcElement.getPlanned();
                notifyPendingManager(svcProvisionListener);
                if(mgrLogger.isLoggable(Level.FINE))
                    mgrLogger.fine(sb.toString()+", was ["+planned+"], "+
                                   "initialMaintain="+initialMaintain+", "+
                                   "new maintain="+maintain);
            } else {
                if(mgrLogger.isLoggable(Level.FINE))
                    mgrLogger.fine(sb.toString()+", cancelled, already at maximum allowed ["+planned+"]");
            }
        }
        return((okayToIncrement?svcElement:null));
    }

    /*
     * Trim all pending requests
     */
    int trim(int trimUp) {
        ProvisionRequest[] removed = new ProvisionRequest[0];
        if(svcElement.getProvisionType()==ProvisionType.DYNAMIC) {
            if(trimUp==-1) {
                if(provisioner.getPendingManager().hasServiceElement(svcElement)) {
                    removed = provisioner.getPendingManager().removeServiceElement(svcElement);
                }
            } else {
                int numToTrim = Math.min((maintain-getActual()), trimUp);
                if(provisioner.getPendingManager().hasServiceElement(svcElement)) {
                    removed = provisioner.getPendingManager().removeServiceElement(svcElement, numToTrim);
                }
            }
            for (ProvisionRequest aRemoved : removed)
                removeInstanceID(aRemoved.sElem.getServiceBeanConfig().getInstanceID(), "trim");
            if(mgrLogger.isLoggable(Level.FINER))
                mgrLogger.finer("Removed "+removed.length+" ["+LoggingUtil.getLoggingName(svcElement)+"] "+
                                "pending requests from PendingServiceManager");
        }

        synchronized(svcElementRWLock) {
            svcElement.setPlanned(svcElement.getPlanned()-removed.length);
            maintain = svcElement.getPlanned();
        }
        return(removed.length);
    }

    /*
     * Remove and decrement the number to maintain
     */
    synchronized ServiceElement decrement(ServiceBeanInstance instance, boolean mandate, boolean destroy) {

        boolean okayToDecrement = true;
        synchronized(svcElementRWLock) {
            int current = getServiceBeanInstances().length;
            if(current>initialMaintain)
                okayToDecrement = true;
            else if(current==initialMaintain) {
                if(!mandate)
                    okayToDecrement = false;
            } else {
                if(maintain==initialMaintain && !mandate)
                    okayToDecrement = false;
            }

            if(okayToDecrement) {
                svcElement.decrementPlanned();
                int temp = svcElement.getPlanned();
                setInitialPlanned((initialMaintain < temp?initialMaintain:temp));
            }
            maintain = svcElement.getPlanned();

            if(mgrLogger.isLoggable(Level.FINE))
                mgrLogger.fine("DECREMENT ["+LoggingUtil.getLoggingName(svcElement)+"] "+
                               "current="+current+", "+
                               "maintain="+maintain+", "+
                               "initialMaintain="+initialMaintain+", "+
                               "mandate="+mandate+", "+
                               "okayToDecrement="+okayToDecrement+", " +
                               "destroyOnDecrement="+destroy);
        }

        if(okayToDecrement) {
            removeServiceBeanInstance(instance);
            removeInstanceID(instance.getServiceBeanConfig().getInstanceID(), "decrement");
            synchronized(decrementedServiceBeanList) {
                decrementedServiceBeanList.add(instance);
            }
            notifyPendingManager(null);
        }

        if(okayToDecrement && destroy) {
            try {
                destroyService(instance.getService(), instance.getServiceBeanID(), false);
            } catch(Exception e) {
                mgrLogger.log(Level.WARNING,
                              "Getting ["+LoggingUtil.getLoggingName(svcElement)+"] service for destroy invocation",
                              e);
            }
        }
        return(svcElement);
    }

    /**
     * Remove a ServiceBeanInstance
     * 
     * @param instance The ServiceBeanInstance
     */
    void removeServiceBeanInstance(ServiceBeanInstance instance) {
        synchronized(serviceBeanList) {
            int index = serviceBeanList.indexOf(instance);
            if(index!=-1) {
                serviceBeanList.remove(index);
            }
        }
    }

    /**
     * Remove a ServiceBeanInstance
     * 
     * @param id The instanceID to remove
     * @param action The action taken
     */
    void removeInstanceID(Long id, String action) {
        if(id!=null) {
            if(instanceIDs.remove(id)) {
                if(sbiLogger.isLoggable(Level.FINE)) {
                    StringBuffer buff = new StringBuffer();
                    buff.append("[").append(svcElement.getName()).append("] ")
                        .append("Removed [").append(LoggingUtil.getLoggingName(svcElement))
                        .append("] Instance ID=").append(id)
                        .append(", ").append("Action=[").append(action).append("]\n");
                    instanceIDLog(buff);
                }
            } else {
                if(sbiLogger.isLoggable(Level.FINE)) {
                    StringBuffer buff = new StringBuffer();
                    buff.append("[").append(LoggingUtil.getLoggingName(svcElement)).append("] Instance ID=")
                        .append(id).append(" ").append("not removed for [")
                        .append(LoggingUtil.getLoggingName(svcElement))
                        .append("], Action=[").append(action).append("]\n");
                    instanceIDLog(buff);
                }
            }
        }
    }

    /**
     * Stop the ServiceElementManager
     *
     * @param destroyServices Whether to stop the services, true to stop. This 
     * value will be ignored if the provision type is
     * ServiceProvisionManagement.EXTERNAL
     */
    public void stopManager(boolean destroyServices) {
        shutdown=true;
        /* Unsubscribe from the service channel */
        ServiceChannel.getInstance().unsubscribe(serviceChannelClient);
        /* Remove service from TestManager instances */
        if(svcElement.getProvisionType()==ProvisionType.DYNAMIC)
            provisioner.getPendingManager().removeServiceElement(svcElement);
        else
            provisioner.getFixedServiceManager().removeServiceElement(svcElement);
        /* Terminate the LookupCache and ServiceDiscoveryManagement
         * instances */
        if(lCache!=null && sElemListener!=null) {
            try {
                lCache.removeListener(sElemListener);
            } catch (Throwable t) {
                mgrLogger.log(Level.WARNING, "Terminating LookupCache", t);
            }
        }

        /* Stop all FaultDetectionHandler instances */
        for (Map.Entry<ServiceID, FaultDetectionHandler> entry :
            fdhTable.entrySet()) {
            FaultDetectionHandler fdh = entry.getValue();
            fdh.terminate();
        }

        if(sdm!=null) {
            try {
                sdm.terminate();
            } catch (Throwable t) {
                mgrLogger.log(Level.WARNING, "Terminating SDM", t);
            }
        }

        /* If requested, destroy service instances */
        if(destroyServices &&
           svcElement.getProvisionType()!=ProvisionType.EXTERNAL)
            destroyServices();
        svcManagerStarted = false;
    }

    /*
     * Create and dispatch the number of ProvisionRequest instances based on
     * the difference between the current count of services, the number pending
     * and the number to maintain
     */
    private void dispatchProvisionRequests(
        ServiceProvisionListener provListener) {
        int count = maintain-getActual();
        int pending = provisioner.getPendingManager().getCount(svcElement);
        int numRequests = count-pending;
        if(numRequests<=0)
            return;
        ProvisionRequest[] requests = new ProvisionRequest[numRequests];
        synchronized(svcElementRWLock) {
            if(mgrLogger.isLoggable(Level.FINEST))
                mgrLogger.finest("Dispatch ["+numRequests+"] " +
                                 "ProvisionRequests " +
                                 "for ["+LoggingUtil.getLoggingName(svcElement)+"]");
            for(int i=0; i<numRequests; i++) {
                long instanceID = getNextInstanceID();
                ServiceElement newElem = ServiceElementUtil.prepareInstanceID(svcElement, instanceID);
                requests[i] = new ProvisionRequest(newElem,
                                                   listener,
                                                   opStringMgr,
                                                   instanceIDMgr,
                                                   provListener,
                                                   null);
            }
        }
        doDispatchProvisionRequests(requests);
    }

    /**
     * Dispatch an array of ProvisionRequest instances
     * 
     * @param requests Array of ProvisionRequests
     */
    private void doDispatchProvisionRequests(ProvisionRequest[] requests) {
        /* If we are not in active mode, bail */
        if(!getActive())
            return;
        /* If we are shutting down, bail */
        if(shutdown)
            return;
        /* If this thing is not provisionable, get out of Dodge */
        if(svcElement.getProvisionType()==ProvisionType.EXTERNAL)
            return;
        /* Or if this is not a DYNAMIC provisioning type, return */
        if(svcElement.getProvisionType()!=ProvisionType.DYNAMIC)
            return;
        /*
         * Dispatch each ProvisionRequest 
         */
        for (ProvisionRequest request : requests) {
            provisioner.dispatch(request);
        }
    }

    /**
     * Get the active property
     * 
     * @return The active property
     */
    private boolean getActive() {
        boolean mode;
        synchronized(this) {
            mode = active;
        }
        return(mode);
    }

    private int getActual() {
        return(getServiceBeanInstances().length);
    }

    /**
     * Determine if we have already discovered a service
     * 
     * @param proxy The discovered service proxy
     * 
     * @return Return true if the services collection conatins the proxy, 
     * otherwise return false
     */
    private boolean alreadyDiscovered(Object proxy) {
        return(services.contains(proxy));
    }

    /*
     * Set the FaultDetectionHandler for a service
     */
    private void setFaultDetectionHandler(Object proxy, ServiceID serviceID)
        throws Exception {
        if(serviceID==null)
            return;
        if(fdhTable.containsKey(serviceID))
            return;
        if(proxy instanceof ReferentUuid) {
            Uuid uuid = ((ReferentUuid)proxy).getReferentUuid();
            if(myUuid.equals(uuid)) {
                /* Discovered instance of ourself, no FaultDetectionHandler needed */
                return;
            }
        }
        ClassLoader cl = proxy.getClass().getClassLoader();
        FaultDetectionHandler<ServiceID> fdh = FaultDetectionHandlerFactory.getFaultDetectionHandler(svcElement, cl);
        fdh.register(serviceFaultListener);
        fdhTable.put(serviceID, fdh);
        fdh.monitor(proxy, serviceID, lCache);

        if(mgrLogger.isLoggable(Level.FINEST))
            mgrLogger.finest("Obtained FaultDetectionHandler " +
                             "["+fdh.getClass().getName()+"] for "+
                             "["+LoggingUtil.getLoggingName(svcElement)+"]");
    }

    /**
     * Get ServiceBeanInstance elements 
     *    
     * @return Array of ServiceBeanInstance objects. If there are no 
     * ServiceBeanInstance objects, a zero-length array is returned. A new
     * array is returned each time
     */
    ServiceBeanInstance[] getServiceBeanInstances() {
        ServiceBeanInstance[] instances;
        synchronized(serviceBeanList) {
            instances = serviceBeanList.toArray(new ServiceBeanInstance[serviceBeanList.size()]);
        }
        return(instances);
    }

    /**
     * Get allocated instance IDs
     * 
     * @return Array of longs. If there are no allocated instance IDs, a 
     * zero-length array is returned. A new array is returned each time
     */
    private long[] getAllocatedIDs() {
        long[] ids = new long[instanceIDs.size()];
        int i=0;
        for (Long instanceID : instanceIDs) {
            ids[i++] = instanceID;
        }
        return(ids);
    }

    /**
     * @see org.rioproject.monitor.InstanceIDManager#getNextInstanceID
     */
    public long getNextInstanceID() {
        long instanceID = ServiceElementUtil.getNextID(getAllocatedIDs());
        synchronized(instanceIDs) {
            instanceIDs.add(instanceID);
        }
        return(instanceID);
    }

    /*
     * Clean up instances of a service
     * 
     * @return The ServiceBeanInstance of the 'cleaned' service
     */
    private synchronized ServiceBeanInstance cleanService(Object proxy, Uuid serviceUuid, boolean removeInstanceID) {
        ServiceBeanInstance instance = null;
        ServiceBeanInstance[] instances = getServiceBeanInstances();
        try {
            instance = findServiceBeanInstance(proxy, serviceUuid, instances, true);
            if(instance==null)
                instance = findServiceBeanInstance(proxy, serviceUuid, instances, false);
            if(instance!=null) {
                removeServiceBeanInstance(instance);
                if (removeInstanceID)
                    removeInstanceID(instance.getServiceBeanConfig().getInstanceID(), "clean");
            }
        } catch (Exception e) {
            mgrLogger.log(Level.WARNING, "Getting ServiceBeanInstance", e);
        }
        if(instance==null) {
            if(mgrLogger.isLoggable(Level.FINE)) {
                StringBuffer buff = new StringBuffer();
                dumpInstanceIDs(buff);
                mgrLogger.fine("Could not find ServiceBeanInstance " +
                                "for ["+LoggingUtil.getLoggingName(svcElement)+"] "+
                                "UUID=["+serviceUuid+"], in known collection of ServiceBeanInstances, look in " +
                                "decremented list: "+decrementedServiceBeanList);
            }
            /* See if the proxy has been placed on the decrementedServiceBeanList */
            List<ServiceBeanInstance> decremented = new ArrayList<ServiceBeanInstance>();
            synchronized(decrementedServiceBeanList) {
                decremented.addAll(decrementedServiceBeanList);
            }
            try {
                for (ServiceBeanInstance sbi : decremented) {
                    if (serviceUuid.equals(sbi.getServiceBeanID())) {
                        instance = sbi;
                        break;
                    }
                }
            } catch (Exception e) {
                mgrLogger.log(Level.WARNING, "Getting ServiceBeanInstance", e);
            }
            if(instance!=null) {
                synchronized(decrementedServiceBeanList) {
                    decrementedServiceBeanList.remove(instance);
                }
            } else {
                logger.fine("["+LoggingUtil.getLoggingName(svcElement)+"] "+
                            "Could not locate ServiceBeanInstance "+serviceUuid+" in decremented list: "+
                            decrementedServiceBeanList);
            }
        }

        InstantiatorResource[] instantiators;
        if(instance!=null) {
            if(mgrLogger.isLoggable(Level.FINEST))
                mgrLogger.finest("CLEAN SBI = ["+LoggingUtil.getLoggingName(svcElement)+"] "+
                                 instance.toString());
            if(instance.getHostAddress()!=null) {
                ServiceResource[] resources =
                    provisioner.getServiceResourceSelector().getServiceResources(instance.getHostAddress(), true);
                instantiators = new InstantiatorResource[resources.length];
                for (int i=0; i<instantiators.length; i++) {
                    instantiators[i] = (InstantiatorResource) resources[i].getResource();
                }
            } else {
                mgrLogger.warning("ServiceBeanInstance for "+
                                  "["+LoggingUtil.getLoggingName(svcElement)+"], " +
                                  "instance=["+instance.getServiceBeanConfig().getInstanceID()+"], " +
                                  "UUID=["+serviceUuid+"], " +
                                  "unknown host address, look across all registered Cybernodes for removal");
                instantiators = provisioner.getServiceResourceSelector().getInstantiatorResources(svcElement);
            }
        } else {
            mgrLogger.warning("No ServiceBeanInstance for service "+
                              "["+LoggingUtil.getLoggingName(svcElement)+"], " +
                              "UUID=["+serviceUuid+"], look across all registered Cybernodes for removal");
            instantiators = provisioner.getServiceResourceSelector().getInstantiatorResources(svcElement);
        }

        if(mgrLogger.isLoggable(Level.FINER) && instantiators.length>0)
            mgrLogger.finer("Attempt to remove instance of " +
                            "["+LoggingUtil.getLoggingName(svcElement)+"] from provided " +
                            "["+instantiators.length+"] Cybernodes");

        /* Remove all instances of the ServiceElement from InstantiatorResource objects */
        for(InstantiatorResource ir : instantiators) {
            instance = ir.removeServiceElementInstance(svcElement, instance==null?serviceUuid:instance.getServiceBeanID());
            if(instance!=null) {
                if(mgrLogger.isLoggable(Level.FINER))
                    mgrLogger.finer("Removed ["+
                                    LoggingUtil.getLoggingName(svcElement)+"] instance from "+ir.getHostAddress());
                break;
            }
        }
        /*
        if(provisioner.getPendingManager().getCount(svcElement)>0) {
            mgrLogger.info("PENDING MANAGER PROCESS!!!!!!");
            provisioner.getPendingManager().process();
        }
        */
        services.remove(proxy);
        fdhTable.remove(new ServiceID(serviceUuid.getMostSignificantBits(), serviceUuid.getLeastSignificantBits()));
        StringBuffer buff = new StringBuffer();
        buff.append("[").append(LoggingUtil.getLoggingName(svcElement)).append("] ");
        buff.append("cleanService():\n");
        instanceIDLog(buff);
        return(instance);
    }

    private ServiceBeanInstance findServiceBeanInstance(Object proxy,
                                                        Uuid serviceUuid,
                                                        ServiceBeanInstance[] instances,
                                                        boolean matchOnProxy) throws ClassNotFoundException, IOException {
        ServiceBeanInstance instance = null;
        for (ServiceBeanInstance sbi : instances) {
            if(matchOnProxy) {
                if (sbi.getService().equals(proxy)) {
                    instance = sbi;
                    break;
                }
            } else {
                if (sbi.getServiceBeanID().equals(serviceUuid)) {
                    instance = sbi;
                    break;
                }
            }
        }
        return instance;
    }

    /*
     * Create a ServiceBeanInstance from a ServiceItem
     *
     * @throws IOException
     */
    private ServiceBeanInstance createServiceBeanInstance(ServiceItem item) throws IOException {
        ServiceBeanInstance instance = null;
        /* Create a uuid from the ServiceID */
        Uuid uuid = UuidFactory.create(item.serviceID.getMostSignificantBits(),
                                       item.serviceID.getLeastSignificantBits());

        /* Make sure we dont already have a ServiceBeanInstance for the item */
        ServiceBeanInstance[] instances = getServiceBeanInstances();
        for (ServiceBeanInstance sbi : instances) {
            if (sbi.getServiceBeanID().equals(uuid)) {
                instance = sbi;
                break;
            }
        }
        if(instance==null) {
            String hostAddress = null;
            ServiceBeanConfig jsbConfig = null;
            Uuid instantiatorUuid = null;
            if(item.service instanceof Administrable) {
                try {
                    Object admin = ((Administrable)item.service).getAdmin();
                    if(admin instanceof ServiceBeanAdmin) {
                        ServiceBeanAdmin jsbAdmin = (ServiceBeanAdmin)admin;
                        jsbConfig = jsbAdmin.getServiceElement().getServiceBeanConfig();
                        instantiatorUuid = jsbAdmin.getServiceBeanInstantiatorUuid();
                    }
                } catch(RemoteException e) {
                    mgrLogger.log(Level.WARNING,
                                  "Getting ServiceBeanConfig for service ["+LoggingUtil.getLoggingName(svcElement)+"]",
                                  e);
                }
            }

            ComputeResourceInfo ai = getApplianceInfo(item.attributeSets);
            if(ai!=null) {
                hostAddress = ai.hostAddress;
            }

            if(svcElement.getProvisionType() == ProvisionType.EXTERNAL) {
                jsbConfig = svcElement.getServiceBeanConfig();
                if(sbiLogger.isLoggable(Level.FINEST))
                    sbiLogger.finest("ServiceElement ["+LoggingUtil.getLoggingName(svcElement)+"] "+
                                     "is an external service, create " +
                                     "generic ServiceBeanInstance");
            }

            /* If we couldnt get the ServiceBeanConfig or instantiatorUuid,
             * try to obtain it from the collection of Cybernodes */
            if(jsbConfig == null || instantiatorUuid==null) {
                InstantiatorResource[] resources =
                    provisioner.getServiceResourceSelector().getInstantiatorResources(svcElement);
                for (InstantiatorResource resource : resources) {
                    ServiceRecord[] records = new ServiceRecord[0];
                    try {
                        records = resource.getActiveServiceRecords();
                    } catch (Throwable t) {
                        if (mgrLogger.isLoggable(Level.FINEST))
                            mgrLogger.log(Level.FINEST, "Getting active ServiceRecords", t);
                    }
                    for (ServiceRecord record : records) {
                        if (uuid.equals(record.getServiceID())) {
                            if(jsbConfig==null)
                                jsbConfig = record.getServiceElement().getServiceBeanConfig();
                            hostAddress = resource.getHostAddress();
                            instantiatorUuid = resource.getInstantiatorUuid();
                            break;
                        }
                    }
                }

                if(jsbConfig == null) {
                    if(sbiLogger.isLoggable(Level.FINEST))
                        sbiLogger.finest("ServiceBeanConfiguration cannot be obtained from " +
                                         "["+LoggingUtil.getLoggingName(svcElement)+"], " +
                                         "Proxy ["+item.service.getClass().getName()+"], "+
                                         "Uuid "+uuid.toString());
                } else {
                    if(sbiLogger.isLoggable(Level.FINEST))
                        sbiLogger.finest("MATCHED\n"+
                                         "\t["+LoggingUtil.getLoggingName(jsbConfig)+"]" +
                                         "\n"+
                                         "THIS\n" +
                                         "\t["+LoggingUtil.getLoggingName(svcElement)+"]");
                }
            }

            if(jsbConfig!=null) {
                if(!instanceIDs.contains(jsbConfig.getInstanceID()))
                    instanceIDs.add(jsbConfig.getInstanceID());
                /* Create the ServiceBeanInstance */
                instance =
                    new ServiceBeanInstance(uuid,
                                            new MarshalledInstance(item.service),
                                            jsbConfig,
                                            hostAddress,
                                            instantiatorUuid);
            }
        }
        return(instance);
    }

    /*
     * Import a ServiceBeanInstance, notification from a peer
     */
    public void importServiceBeanInstance(ServiceBeanInstance instance) throws Exception {
        Object proxy = instance.getService();
        if(proxy instanceof RemoteMethodControl)
            proxy = proxyPreparer.prepareProxy(instance.getService());
        addServiceBeanInstance(instance);
        Uuid uuid = instance.getServiceBeanID();
        ServiceID serviceID = new ServiceID(uuid.getMostSignificantBits(), uuid.getLeastSignificantBits());
        setFaultDetectionHandler(proxy, serviceID);
    }

    /*
     * Add a ServiceBeanInstance
     */
    void addServiceBeanInstance(ServiceBeanInstance instance) {
        if(instance==null)
            return;
        boolean changed = false;
        StringBuffer buff = new StringBuffer();
        buff.append("[").append(svcElement.getName()).append("] ");
        if(!serviceBeanList.contains(instance)) {
            serviceBeanList.add(instance);
            Long instanceID =
            instance.getServiceBeanConfig().getInstanceID();
            if(!instanceIDs.contains(instanceID))
                instanceIDs.add(instanceID);
            changed = true;
            if(sbiLogger.isLoggable(Level.FINE))
                buff.append("Added SBI = ")
                    .append(instance.getServiceBeanConfig().getInstanceID())
                    .append(", ")
                    .append(instance.getServiceBeanID().toString())
                    .append("\n");
        } else {
            int ndx = serviceBeanList.indexOf(instance);
            ServiceBeanInstance current = serviceBeanList.get(ndx);
            /* Adjust host address */
            if(current.getHostAddress()==null &&
               instance.getHostAddress()!=null) {
                serviceBeanList.set(ndx, instance);
                changed = true;
                if(sbiLogger.isLoggable(Level.FINE))
                    buff.append("Adjusted SBI host address, was [null], now = ")
                        .append(instance.getHostAddress())
                        .append(", instanceID : ")
                        .append(instance.getServiceBeanConfig())
                        .append("\n");
            }
            /* Adjust instance ID */
            Long iid = current.getServiceBeanConfig().getInstanceID();
            if((iid==null || (iid ==0)) &&
               instance.getServiceBeanConfig().getInstanceID()!=null) {
                serviceBeanList.set(ndx, instance);
                changed = true;
                if(sbiLogger.isLoggable(Level.FINE))
                    buff.append("Adjusted SBI instanceID, was [null], now = ")
                        .append(instance.getServiceBeanConfig().getInstanceID())
                        .append("\n");
            }
        }

        if(changed)
            instanceIDLog(buff);
    }

    @SuppressWarnings("unchecked")
    private void addServiceProxy(Object proxy) {
        services.add(proxy);
    }

    /**
     * Manage service provision notifications 
     */
    class ServiceBeanProvisionListener implements ProvisionListener {
        /**
         * @see ProvisionListener#uninstantiable(ProvisionRequest)
         */
        public void uninstantiable(ProvisionRequest request) {
            ServiceBeanConfig sbc;
            if(request.instance!=null)
                sbc = request.instance.getServiceBeanConfig();
            else
                sbc = request.sElem.getServiceBeanConfig();
            if(sbc!=null) {
                removeInstanceID(sbc.getInstanceID(), "uninstantiable");
            } else {
                mgrLogger.warning("Received uninstantiable service notification, " +
                                  "getServiceBeanConfig property null");
            }
        }

        /**
         * @see org.rioproject.monitor.ProvisionListener#serviceProvisioned(ServiceBeanInstance, InstantiatorResource)
         */
        @SuppressWarnings("unchecked")
        public void serviceProvisioned(ServiceBeanInstance instance, InstantiatorResource resource) {
            try {
                Object proxy = instance.getService();
                String hostAddress = instance.getHostAddress();
                synchronized(serviceBeanList) {
                    /* Prepare the proxy */
                    if(proxy instanceof RemoteMethodControl)
                        proxy = proxyPreparer.prepareProxy(proxy);

                    addServiceProxy(proxy);

                    /* If for some reason the hostAddress or instantiatorUuid
                     * is null, then construct a new ServiceBeanInstance with
                     * the hostAddress and Uuid of the InstantiatorResource */
                    if(hostAddress==null ||
                        instance.getServiceBeanInstantiatorID()==null) {
                        hostAddress = resource.getHostAddress();
                        instance =
                        new ServiceBeanInstance(instance.getServiceBeanID(),
                                                instance.getMarshalledInstance(),
                                                instance.getServiceBeanConfig(),
                                                hostAddress,
                                                instance.getServiceBeanInstantiatorID());
                    }
                    if(hasServiceBeanInstance(instance)) {
                        replaceServiceBeanInstance(instance);
                    }
                    else {
                        addServiceBeanInstance(instance);
                    }
                    mgrLogger.info("["+LoggingUtil.getLoggingName(svcElement)+"] service " +
                                   "provisioned, "+
                                   "instanceId=["+instance.getServiceBeanConfig().getInstanceID()+"], "+
                                   "type=["+svcElement.getProvisionType()+"], " +
                                   "have ["+serviceBeanList.size()+"] service instances");
                }
                /* Re-get the proxy using the proxy's classloader */
                ClassLoader currentCL = Thread.currentThread().getContextClassLoader();
                try {
                    Thread.currentThread().setContextClassLoader(proxy.getClass().getClassLoader());
                    proxy = new MarshalledObject(proxy).get();
                    if(proxy instanceof RemoteMethodControl)
                        proxy = proxyPreparer.prepareProxy(proxy);

                    if(proxy instanceof MonitorableService) {
                        Uuid uuid = instance.getServiceBeanID();
                        ServiceID serviceID = new ServiceID(uuid.getMostSignificantBits(),
                                                            uuid.getLeastSignificantBits());
                        setFaultDetectionHandler(proxy, serviceID);
                    } else if(proxy instanceof ReferentUuid) {
                        Uuid uuid = ((ReferentUuid)proxy).getReferentUuid();
                        ServiceID serviceID = new ServiceID(uuid.getMostSignificantBits(),
                                                            uuid.getLeastSignificantBits());
                        setFaultDetectionHandler(proxy, serviceID);
                    } else {
                        StringBuilder sb = new StringBuilder();
                        sb.append("Proxy interfaces: ");
                        for(Class c : proxy.getClass().getInterfaces()) {
                            if(sb.length()>0)
                                sb.append(", ");
                            sb.append(c.getName());
                        }
                        /* An ambiguous service is a service that we cannot
                         * get the serviceID of */
                        if(mgrLogger.isLoggable(Level.FINER))
                            mgrLogger.log(Level.FINER,
                                          "Could not get the serviceID of "+
                                          "["+LoggingUtil.getLoggingName(svcElement)+"], " +
                                          "[proxy="+proxy.getClass().getName()+"] " +
                                          "provisioned to "+
                                          "["+hostAddress+"]. Attempts will be " +
                                          "made to resolve the serviceID of this " +
                                          "service. "+sb.toString());
                        ClassLoaderUtil.displayContextClassLoaderTree();
                        ambiguousServices.put(proxy, hostAddress);
                    }
                } catch(Exception e) {
                    mgrLogger.log(Level.WARNING,
                              "Unable to set or create FaultDetectionHandler for " +
                              "["+LoggingUtil.getLoggingName(svcElement)+"]",
                              e);
                } finally {
                    Thread.currentThread().setContextClassLoader(currentCL);
                }

            } catch(Throwable t) {
                mgrLogger.log(Level.WARNING,
                              "Service provision notification for ["+LoggingUtil.getLoggingName(svcElement)+"]",
                              t);
            }
            /* Notify that a service has been provisioned */
            ProvisionMonitorEvent event = new ProvisionMonitorEvent(eventSource,
                                                                    ProvisionMonitorEvent.Action.SERVICE_PROVISIONED,
                                                                    svcElement.getOperationalStringName(),
                                                                    svcElement,
                                                                    instance);
            processEvent(event);
            ServiceChannel channel = ServiceChannel.getInstance();
            channel.broadcast(new ServiceChannelEvent(this, svcElement, instance, ServiceChannelEvent.PROVISIONED));
        }
    }

    /**
     * Handle internal service notifications for associated service transitions
     */
    class ServiceChannelClient implements ServiceChannel.ServiceChannelListener {

        public void notify(ServiceChannelEvent event) {
            if(getActive() &&
               event.getType()==ServiceChannelEvent.PROVISIONED) {
                if(provisioner.getPendingManager().getCount(svcElement)>0) {
                    provisioner.getPendingManager().process();
                }
            }
        }
    }

    /**
     * Manage service discovery notifications
     */
    class ServiceElementManagerServiceListener extends ServiceDiscoveryAdapter {

        /**
         * Notification that a service has been discovered
         *
         * @param sdEvent The ServiceDiscoveryEvent
         */
        public void serviceAdded(ServiceDiscoveryEvent sdEvent) {
            try {
                ServiceItem item = sdEvent.getPostEventServiceItem();
                if(item.service==null) {
                    mgrLogger.log(Level.WARNING,
                                  "ServiceElementManager.serviceAdded(): "+
                                  "item.service is NULL for ["+LoggingUtil.getLoggingName(svcElement)+"]");
                    return;
                }
                /* Prepare the proxy */
                if(item.service instanceof RemoteMethodControl)
                    item.service = proxyPreparer.prepareProxy(item.service);

                /* Construct the ServiceBeanInstance and add it to the
                 * serviceBeanList */
                ServiceBeanInstance sbi = createServiceBeanInstance(item);
                if(sbi!=null)
                    addServiceBeanInstance(sbi);

                /* See if this was an ambiguous service (one that we
                 * could not obtain the serviceId for), if so clean it up */
                if(ambiguousServices.containsKey(item.service)) {
                    ambiguousServices.remove(item.service);
                    if(mgrLogger.isLoggable(Level.FINER))
                        mgrLogger.log(Level.FINER,
                                      "Resolved ambiguous service " +
                                      "["+LoggingUtil.getLoggingName(svcElement)+"], proxy="+
                                      item.service.getClass().getName());
                    setFaultDetectionHandler(item.service, item.serviceID);
                }

                /**
                 * If this ServiceElementManager is active, then services will
                 * be added as they are provisioned (through the
                 * ServiceBeanProvisionListener). Therefore only process a serviceAdded
                 * transition in active-mode if the service is EXTERNAL, or
                 * when in inactive (backup) mode
                 */
                if(getActive()) {
                    if(svcElement.getProvisionType()==ProvisionType.EXTERNAL) {
                        if(alreadyDiscovered(item.service))
                            return;
                        addServiceProxy(item.service);
                        setFaultDetectionHandler(item.service, item.serviceID);
                    }
                } else {
                    if(alreadyDiscovered(item.service))
                        return;
                    addServiceProxy(item.service);
                    setFaultDetectionHandler(item.service, item.serviceID);
                }
            } catch(Throwable t) {
                mgrLogger.log(Level.WARNING,
                              "Service discovery notification for ["+LoggingUtil.getLoggingName(svcElement)+"]",
                              t);
            }
        }
    }

    /**
     * Manage service failure notifications
     */
    class ServiceFaultListener implements FaultDetectionListener<ServiceID> {
        /**
         * @see org.rioproject.fdh.FaultDetectionListener#serviceFailure(Object, Object)
         */
        public synchronized void serviceFailure(Object proxy, ServiceID sID) {
            if(shutdown)
                return;
            ServiceBeanInstance instance = null;
            Uuid uuid = UuidFactory.create(sID.getMostSignificantBits(),
                                           sID.getLeastSignificantBits());
            boolean asResultOfRedeployment = false;
            try {
                /* Clean up instances of the service and decrease the number
                 * of services if the service's proxy was removed from the
                 * collection of known service proxies. If the service is a
                 * FIXED service, clean up instanceIDs.
                 */
                instance = cleanService(proxy, uuid, (svcElement.getProvisionType() == ProvisionType.FIXED));
                if(instance!=null) {
                    if(mgrLogger.isLoggable(Level.FINE))
                        mgrLogger.fine("["+LoggingUtil.getLoggingName(svcElement)+"] service failure, "+
                                       "instance=["+instance.getServiceBeanConfig().getInstanceID()+"], "+
                                       "type=["+svcElement.getProvisionType()+"], "+
                                       "proxy=["+proxy.getClass().getName()+"]");
                } else {
                    if(mgrLogger.isLoggable(Level.FINE))
                        mgrLogger.fine("["+LoggingUtil.getLoggingName(svcElement)+"] service failure, "+
                                       "type=["+svcElement.getProvisionType()+"], "+
                                       "proxy=["+proxy.getClass().getName()+"], " +
                                       "COULD NOT OBTAIN INSTANCE, active monitor? "+getActive());
                }
                String hostAddress = (instance==null?null:instance.getHostAddress());

                /* If there is a ProvisionRequest in the redeployRequestList,
                 * use that ProvisionRequest. This allows a ServiceProvisionListener to be added */
                ProvisionRequest provRequest = getRedeploymentProvisionRequest(proxy);
                asResultOfRedeployment = (provRequest!=null);
                if(mgrLogger.isLoggable(Level.FINEST))
                    mgrLogger.finest("Redeployment ProvisionRequest for " +
                                     "["+LoggingUtil.getLoggingName(svcElement)+"] obtained: "+
                                     (provRequest==null?"no":"yes"));
                if(provRequest == null) {
                    ServiceElement newElem = ServiceElementUtil.copyServiceElement(svcElement);
                    if(instance!=null) {
                        newElem.setServiceBeanConfig(instance.getServiceBeanConfig());
                        if(mgrLogger.isLoggable(Level.FINEST))
                            mgrLogger.finest("["+LoggingUtil.getLoggingName(svcElement)+"] found instance, instanceID="+
                                             newElem.getServiceBeanConfig().getInstanceID());
                    } else {
                        if(mgrLogger.isLoggable(Level.FINEST))
                            mgrLogger.finest("["+LoggingUtil.getLoggingName(svcElement)+"] " +
                                             "instance not found, use default " +
                                             "ServiceElement settings");
                    }
                    provRequest = new ProvisionRequest(newElem, listener, opStringMgr, instanceIDMgr, null, instance);
                }
                /* Add the host address to the list of hosts the service has visited */
                ServiceBeanConfig sbConfig = addHost(instance, hostAddress);
                if(sbConfig!=null) {
                    provRequest.sElem.setServiceBeanConfig(sbConfig);
                }

                provRequest.sElem.setPlanned(maintain);
                if(svcElement.getProvisionType()==ProvisionType.DYNAMIC) {
                    int pending = provisioner.getPendingManager().getCount(svcElement);
                    //int actual = getActual()+pending;
                    /* Dont count pending */
                    int actual = getActual();
                    if(mgrLogger.isLoggable(Level.FINE))
                        mgrLogger.fine("["+LoggingUtil.getLoggingName(svcElement)+"] "+
                                      "Removed: actual ["+actual+"], pending ["+pending+"], maintain ["+maintain+"]");

                    if(actual<maintain) {
                        doDispatchProvisionRequests(new ProvisionRequest[]{provRequest});
                    }
                }
                
            } catch(Throwable t) {
                mgrLogger.log(Level.SEVERE,
                              "Service Fault Detection for ["+LoggingUtil.getLoggingName(svcElement)+"]",
                              t);
            }
            /* Notify a service has failed */
            if(!asResultOfRedeployment) {
                ProvisionMonitorEvent event = new ProvisionMonitorEvent(eventSource,
                                                                        ProvisionMonitorEvent.Action.SERVICE_FAILED,
                                                                        svcElement.getOperationalStringName(),
                                                                        svcElement,
                                                                        instance);
                processEvent(event);
                ServiceChannel channel = ServiceChannel.getInstance();
                channel.broadcast(new ServiceChannelEvent(this, svcElement, instance, ServiceChannelEvent.FAILED));
            }
        }

        /*
         * Add the host address to the host list in the ServiceBeanConfig
         * 
         * @return A new ServiceBeanConfig
         */
        @SuppressWarnings("unchecked")
        ServiceBeanConfig addHost(ServiceBeanInstance instance, String host) {
            ServiceBeanConfig config = null;
            if(instance!=null) {
                if(host==null) {
                    config = instance.getServiceBeanConfig();
                } else {
                    ServiceBeanConfig current = instance.getServiceBeanConfig();
                    Map<String, Object> configMap = current.getConfigurationParameters();
                    Map<String, Object> initMap = current.getInitParameters();
                    List<String> seenHosts =
                        (List)configMap.get(ServiceBeanConfig.HOST_HISTORY);
                    List<String> hosts = new ArrayList<String>();
                    hosts.addAll(seenHosts);
                    boolean addHost = true;
                    if(hosts.size()>0) {
                        if(hosts.get(hosts.size()-1).equals(host))
                            addHost = false;
                    }
                    if(addHost)
                        hosts.add(host);
                    configMap.put(ServiceBeanConfig.HOST_HISTORY, hosts);
                    config = new ServiceBeanConfig(configMap, current.getConfigArgs());

                    for (Map.Entry<String, Object> e : initMap.entrySet()) {
                        config.addInitParameter(e.getKey(), e.getValue());
                    }
                }
            }
            return(config);
        }


        /*
         * Get a ProvisionRequest created from a redeploy invocation. If not 
         * found return null
         */
        ProvisionRequest getRedeploymentProvisionRequest(Object service) {
            ProvisionRequest pr = null;
            ProvisionRequest[] prs = redeployRequestList.toArray(new ProvisionRequest[redeployRequestList.size()]);
            for (ProvisionRequest pr1 : prs) {
                try {
                    if (pr1.instance != null && pr1.instance.getService().equals(service)) {
                        pr = pr1;
                        redeployRequestList.remove(pr);
                        break;
                    }
                } catch (Exception e) {
                    mgrLogger.log(Level.WARNING, "Getting service for redeployment invocation", e);
                }
            }
            return(pr);
        }

    }

    /**
     * A local ServiceProvisionListener for relocation requests
     */
    class RelocationListener implements ServiceProvisionListener {
        ServiceProvisionListener remoteListener;
        ServiceBeanInstance original;

        RelocationListener(ServiceProvisionListener remoteListener, ServiceBeanInstance original) {
            this.remoteListener = remoteListener;
            this.original = original;
        }

        /**
         * Notify listener that the Service described by the ServiceBeanInstance has
         * been provisioned succesfully
         *
         * @param jsbInstance The ServiceBeanInstance
         */
        public void succeeded(ServiceBeanInstance jsbInstance) throws RemoteException {
            try {
                Administrable admin = (Administrable)original.getService();
                DestroyAdmin destroyAdmin = (DestroyAdmin)admin.getAdmin();
                destroyAdmin.destroy();
            } catch(Exception e) {
                if(mgrLogger.isLoggable(Level.FINEST)) {
                    mgrLogger.log(Level.FINEST,
                                  "["+LoggingUtil.getLoggingName(svcElement)+"] "+
                                  "Destroying original service",
                                  e);
                } else {
                    mgrLogger.log(Level.INFO,
                                  "["+LoggingUtil.getLoggingName(svcElement)+"] Destroying "+
                                  "original service");
                }
            } 
            if(remoteListener != null) {
                try {
                    remoteListener.succeeded(jsbInstance);
                } catch(Exception e) {
                    if(mgrLogger.isLoggable(Level.FINEST)) {
                        mgrLogger.log(Level.FINEST,
                                      "["+LoggingUtil.getLoggingName(svcElement)+"] Error " +
                                      "notifying ServiceProvisionListeners on " +
                                      "success", e);
                    }
                }
            }
        }

        /**
         * Notify listener that the Service described by the ServiceElement has not
         * been provision successfully
         *
         * @param sElem The ServiceElement
         * @param resubmitted Whether the  Service described by the ServiceElement
         * has been resubmitted for provisioning
         */
        public void failed(ServiceElement sElem, boolean resubmitted) throws RemoteException {
            if(remoteListener != null) {
                try {
                    remoteListener.failed(svcElement, true);
                } catch(NoSuchObjectException e) {
                    mgrLogger.log(Level.WARNING,
                                  "ServiceBeanInstantiatorListener failure "+
                                  "notification did not succeed, "+
                                  "[java.rmi.NoSuchObjectException:"+
                                  e.getLocalizedMessage()+"], remove "+
                                  "ServiceBeanInstantiatorListener "+
                                  "["+remoteListener+"]");
                } catch(Exception e) {
                    mgrLogger.log(Level.WARNING,
                                  "ServiceBeanInstantiatorListener "+
                                  "notification",
                                  e);
                }
            }
        }
    }

    /*
     * Helper method to obtain execute a
     * ProvisionMonitorTask to send a ProvisionMonitorEvent
     */
    void processEvent(ProvisionMonitorEvent event) {
        eventProcessor.processEvent(event);
    }

    /*
     * Set the event source which will be used as the source of
     * ProvisionFailureEvent notifications
     */
    void setEventSource(ProvisionMonitor eventSource) {
        this.eventSource = eventSource;
    }

    /*
     * Set the ProvisionMonitorEventProcessor
     */
    void setEventProcessor(ProvisionMonitorEventProcessor eventProcessor) {
        this.eventProcessor = eventProcessor;
    }

    /**
     * Helper to get the ComputeResourceInfo Entry
     * 
     * @param attrs Array of Entry objects
     * 
     * @return ApplianceInfo
     */
    ComputeResourceInfo getApplianceInfo(Entry[] attrs) {
        for (Entry attr : attrs) {
            if (attr instanceof ComputeResourceInfo) {
                return (ComputeResourceInfo) attr;
            }
        }
        return(null);
    }

    /*
     * See if the newly discovered service can be found in an existing
     * Cybernode. This happens when a service is discovered that does not
     * implement the ServiceBeanAdmin interface. If the service had been
     * provisioned to a Cybernode, we should be able to get it's
     * ServiceBeanConfig
     */
    void instanceIDLog(StringBuffer buff) {
        if(sbiLogger.isLoggable(Level.FINEST)) {
            dumpInstanceIDs(buff);
            sbiLogger.finest(buff.toString());
        }
    }

    void dumpInstanceIDs(StringBuffer buff) {
        long[] ids = getAllocatedIDs();
        if(ids.length > 0) {
            buff.append("Instance ID list [");
            for(int i = 0; i < ids.length; i++) {
                if(i > 0)
                    buff.append(", ");
                buff.append(ids[i]);
            }
            buff.append("]\n");
        }
        ServiceBeanInstance[] instances = getServiceBeanInstances();
        if(instances.length == 0) {
            buff.append("     ServiceBeanInstance List: {empty}");
        } else {
            buff.append("     ServiceBeanInstance List:\n");
            for(int i = 0; i < instances.length; i++) {
                if(i > 0)
                    buff.append("\n");
                buff.append("     ").append(instances[i].toString());
            }
        }
    }

    /*
     * Locate the parameter referencing the ScalingPolicyHandler and return
     * the number of maxServices. If either the ScalingPolicyHandler
     * attribute cannot be found, or the maxServices attribute in the
     * ScalingPolicyHandler cannot be found return -1
     */
    private int getMaxServiceCount() {
        SLA[] slas = svcElement.getServiceLevelAgreements().getServiceSLAs();
        int count = -1;
        for (SLA sla : slas) {
            //String handler = sla.getSlaPolicyHandler();
            //if (handler.indexOf("ScalingPolicyHandler") != -1) {
                int x = (sla.getMaxServices()==SLA.UNDEFINED?
                         Integer.MAX_VALUE:sla.getMaxServices());
                if (x > count)
                    count = x;
            //}
        }
        return (count);
    }
}
