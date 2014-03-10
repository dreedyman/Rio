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
package org.rioproject.cybernode.service;

import com.sun.jini.config.Config;
import com.sun.jini.reliableLog.LogHandler;
import com.sun.jini.start.LifeCycle;
import net.jini.config.Configuration;
import net.jini.config.ConfigurationException;
import net.jini.core.entry.Entry;
import net.jini.core.event.UnknownEventException;
import net.jini.core.lookup.ServiceID;
import net.jini.export.Exporter;
import net.jini.id.Uuid;
import net.jini.io.MarshalledInstance;
import net.jini.jeri.BasicILFactory;
import net.jini.jeri.BasicJeriExporter;
import net.jini.lookup.entry.ServiceInfo;
import net.jini.lookup.entry.StatusType;
import net.jini.lookup.ui.AdminUI;
import net.jini.security.BasicProxyPreparer;
import net.jini.security.ProxyPreparer;
import net.jini.security.TrustVerifier;
import net.jini.security.proxytrust.ServerProxyTrust;
import org.rioproject.RioVersion;
import org.rioproject.config.Constants;
import org.rioproject.cybernode.Cybernode;
import org.rioproject.cybernode.proxy.CybernodeProxy;
import org.rioproject.deploy.*;
import org.rioproject.entry.BasicStatus;
import org.rioproject.entry.UIDescriptorFactory;
import org.rioproject.event.EventDescriptor;
import org.rioproject.impl.client.DiscoveryManagementPool;
import org.rioproject.impl.client.JiniClient;
import org.rioproject.impl.config.ExporterConfig;
import org.rioproject.impl.container.*;
import org.rioproject.impl.jmx.JMXUtil;
import org.rioproject.impl.jmx.MBeanServerFactory;
import org.rioproject.impl.opstring.OpStringLoader;
import org.rioproject.impl.opstring.OpStringManagerProxy;
import org.rioproject.impl.persistence.PersistentStore;
import org.rioproject.impl.persistence.SnapshotHandler;
import org.rioproject.impl.servicebean.DefaultServiceBeanManager;
import org.rioproject.impl.servicebean.ServiceBeanActivation;
import org.rioproject.impl.servicebean.ServiceBeanActivation.LifeCycleManager;
import org.rioproject.impl.servicebean.ServiceBeanAdapter;
import org.rioproject.impl.system.ComputeResource;
import org.rioproject.impl.system.SystemCapabilities;
import org.rioproject.impl.system.measurable.MeasurableCapability;
import org.rioproject.impl.util.BannerProvider;
import org.rioproject.impl.util.BannerProviderImpl;
import org.rioproject.impl.watch.ThreadDeadlockMonitor;
import org.rioproject.impl.watch.ThresholdListener;
import org.rioproject.impl.watch.ThresholdWatch;
import org.rioproject.impl.watch.WatchInjector;
import org.rioproject.net.HostUtil;
import org.rioproject.opstring.OperationalString;
import org.rioproject.opstring.OperationalStringManager;
import org.rioproject.opstring.ServiceElement;
import org.rioproject.servicebean.ServiceBeanContext;
import org.rioproject.servicebean.ServiceBeanManager;
import org.rioproject.serviceui.UIComponentFactory;
import org.rioproject.sla.SLA;
import org.rioproject.sla.SLAThresholdEvent;
import org.rioproject.system.ComputeResourceUtilization;
import org.rioproject.system.capability.PlatformCapability;
import org.rioproject.util.RioManifest;
import org.rioproject.util.TimeUtil;
import org.rioproject.watch.Calculable;
import org.rioproject.watch.ThresholdType;
import org.rioproject.watch.ThresholdValues;
import org.rioproject.watch.WatchDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.*;
import java.io.*;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.rmi.AccessException;
import java.rmi.AlreadyBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.text.NumberFormat;
import java.util.*;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Implementation of a Cybernode
 *
 * @link {org.rioproject.impl.jmx.ThreadDeadlockMonitor#ID}
 *
 * @author Dennis Reedy
 */
@SuppressWarnings("unused")
public class CybernodeImpl extends ServiceBeanAdapter implements Cybernode,
                                                                 ServiceBeanContainerListener,
                                                                 CybernodeImplMBean,
                                                                 ServerProxyTrust {
    
    /** Component name we use to find items in the Configuration */
    private static final String RIO_CONFIG_COMPONENT = "org.rioproject";
    /** Component name we use to find items in the Configuration */
    private static final String CONFIG_COMPONENT = RIO_CONFIG_COMPONENT+".cybernode";
    /** Default Exporter attribute */
    private static final String DEFAULT_EXPORTER = "defaultExporter";
    /** Logger name */
    private static final String LOGGER = CybernodeImpl.class.getName();
    /** Cybernode logger. */
    private static final Logger logger = LoggerFactory.getLogger(LOGGER);
    /** Cybernode logger. */
    private static final Logger loaderLogger = LoggerFactory.getLogger(CybernodeImpl.class.getPackage().getName()+".loader");
    /**The component name for accessing the service's configuration */
    private static String configComponent = CONFIG_COMPONENT;
    /** Instance of a ServiceBeanContainer */
    private ServiceBeanContainer container;
    /** If the Cybernode has been configured an an instantiation resource, and
     * the Cybernode removes itself as an asset, this property determines whether
     * instantiated services should be terminated upon unregistration */
    private boolean serviceTerminationOnUnregister=true;
    /** default maximum service limit */
    private int serviceLimit=500;
    /** Service consumer responsible for Provision Monitor discovery and registration */
    private ServiceConsumer svcConsumer=null;
    /** Flag indicating the Cybernode is in shutdown sequence */
    private final AtomicBoolean shutdownSequence= new AtomicBoolean(false);
    /** Collection of services that are in the process of instantiation */
    private final List<ServiceProvisionEvent> inProcess = new ArrayList<ServiceProvisionEvent>();
    /** Log format version */
    private static final int LOG_VERSION = 1;
    /** PersistentStore to save state */
    //PersistentStore store;
    /** ThreadPool for SLAThresholdEvent processing */
    private Executor thresholdTaskPool;
    private ComputeResourcePolicyHandler computeResourcePolicyHandler;
    /** This flag indicates whether the Cybernode has been configured to install
     * external software  defined by ServiceBean instances */
    private boolean provisionEnabled=true;
    /** The ServiceStatementManager defines the semantics of reading/writing
     * ServiceStatement instances */
    private ServiceStatementManager serviceStatementManager;
    /** The Timer to use for scheduling a ServiceRecord update task */
    private Timer taskTimer;
    /** The Configuration for the Service */
    private Configuration config;
    /** ProxyPreparer for the OperationalStringMonitor */
    private ProxyPreparer operationalStringManagerPreparer;
    private LifeCycle lifeCycle;
    /** Flag to indicate if the Cybernode is enlisted */
    private boolean enlisted=false;
    private ServiceRecordUpdateTask serviceRecordUpdateTask;
    private int registryPort;
    private String instantiatorID;

    /**
     * Create a Cybernode
     */
    public CybernodeImpl() {
        super();
    }

    /**
     * Create a Cybernode launched from the ServiceStarter framework
     *
     * @param configArgs Configuration arguments
     * @param lifeCycle The LifeCycle object that started the Cybernode
     *
     * @throws Exception if bootstrapping fails
     */
    public CybernodeImpl(String[] configArgs, LifeCycle lifeCycle) throws Exception {
        super();
        this.lifeCycle = lifeCycle;
        bootstrap(configArgs);
    }

    @Override
    public void destroy() {
        super.destroy();
    }

    /**
     * Override destroy to ensure that any JSBs are shutdown as well
     */
    public void destroy(boolean force) {
        if(shutdownSequence.get())
            return;
        synchronized(CybernodeImpl.class) {
            if(serviceRecordUpdateTask!=null)
                serviceRecordUpdateTask.cancel();
            shutdownSequence.set(true);
            if(computeResourcePolicyHandler!=null) {
                computeResourcePolicyHandler.terminate();
            }
            /* Shutdown the ComputeResource */
            if(computeResource!=null)
                computeResource.shutdown();
            /* Stop the consumer */
            if(svcConsumer != null)
                svcConsumer.destroy();
        }
        svcConsumer = null;
        /* Destroy the container */
        if(container != null)
            container.terminate();

        /* If we have a store, destroy it */
        if(snapshotter!=null)
            snapshotter.interrupt();
        if(store!=null) {
            try {
                store.destroy();
            } catch(Exception e) {
                logger.error("While destroying persistent store", e);
            }
        }
        logger.info("{}: destroy() notification", context.getServiceElement().getName());
        /* Stop the timer */
        if(taskTimer!=null)
            taskTimer.cancel();
        try {
            unadvertise();
        } catch (IOException e) {
            logger.warn("While unadvertising", e);
        }
        stop(force);
        /* Terminate the ServiceStatementManager */
        if(serviceStatementManager!=null)
            serviceStatementManager.terminate();

        /* Close down all WatchDataSource instances, unexporting them from
         * the runtime */
        destroyWatches();
        /* Unregister all PlatformCapability instances */
        if(computeResource!=null) {
            PlatformCapability[] pCaps = computeResource.getPlatformCapabilities();
            for (PlatformCapability pCap : pCaps) {
                try {
                    ObjectName objectName = getObjectName(pCap);
                    MBeanServerFactory.getMBeanServer().unregisterMBean(objectName);
                } catch (Exception e) {
                    logger.warn("Unregistering PlatformCapability [{}]", pCap.getName(), e);
                }
            }
        }
        /* Terminate the DiscoveryManagementPool */
        DiscoveryManagementPool discoPool = DiscoveryManagementPool.getInstance();
        if(discoPool!=null)
            discoPool.terminate();

        /* Tell the utility that started the Cybernode we are going away */
        ServiceBeanManager serviceBeanManager = context.getServiceBeanManager();
        if(serviceBeanManager!=null) {
            DiscardManager discardMgr = serviceBeanManager.getDiscardManager();
            if(discardMgr!=null) {
                discardMgr.discard();
            }
        } else {
            if(lifeCycle!=null) {
                lifeCycle.unregister(this);
            }
        }
        container = null;
    }

    /**
     * Override parent's method to return <code>TrustVerifier</code> which can
     * be used to verify that the given proxy to this service can be trusted
     *
     * @return TrustVerifier The TrustVerifier used to verify the proxy
     *
     */
    public TrustVerifier getProxyVerifier() {
        return (new CybernodeProxy.Verifier(getExportedProxy()));
    }

    /**
     * @see org.rioproject.deploy.ServiceBeanInstantiator#getServiceStatements
     */
    public ServiceStatement[] getServiceStatements() {
        return(serviceStatementManager.get());
    }

    /**
     * @see org.rioproject.deploy.ServiceBeanInstantiator#getServiceRecords
     */
    public ServiceRecord[] getServiceRecords(int filter) {
        Set<ServiceRecord> recordSet = new HashSet<ServiceRecord>();
        ServiceStatement[] statements = serviceStatementManager.get();
        for (ServiceStatement statement : statements) {
            ServiceRecord[] records = statement.getServiceRecords(getUuid(), filter);
            recordSet.addAll(Arrays.asList(records));
        }
        if(logger.isTraceEnabled()) {
            StringBuilder sb = new StringBuilder();
            int i=1;
            for(ServiceRecord record : recordSet) {
                if(sb.length()>0) {
                    sb.append("\n");
                }
                sb.append(String.format("%2d. %-40s instance:%-3d  %s",
                                        i++,
                                        ServiceLogUtil.simpleLogName(record.getServiceElement()),
                                        record.getServiceElement().getServiceBeanConfig().getInstanceID(),
                                        record.getServiceID().toString()));
            }
            String newLine = recordSet.isEmpty()?"":"\n";

            String type = filter==ServiceRecord.ACTIVE_SERVICE_RECORD?"ACTIVE_SERVICE_RECORD":"INACTIVE_SERVICE_RECORD";
            logger.trace("Returning ({}) {} ServiceRecords{}{}", recordSet.size(), type, newLine, sb.toString());
        }
        return(recordSet.toArray(new ServiceRecord[recordSet.size()]));
    }

    /**
     * @see org.rioproject.deploy.ServiceBeanInstantiator#getServiceStatement
     */
    public ServiceStatement getServiceStatement(ServiceElement elem) {
        if(elem==null)
            throw new IllegalArgumentException("ServiceElement is null");
        return serviceStatementManager.get(elem);
    }

    /**
     * @see org.rioproject.deploy.ServiceBeanInstantiator#getServiceBeanInstances
     */
    public ServiceBeanInstance[] getServiceBeanInstances(ServiceElement element) {
        ServiceBeanInstance[] sbi = container.getServiceBeanInstances(element);
        logger.trace("ServiceBeanInstance count for [{}] = {}", element.getName(), sbi.length);
        return (sbi);
    }

    /**
     * @see org.rioproject.deploy.ServiceBeanInstantiator#update
     */
    public void update(ServiceElement[] sElements, OperationalStringManager opStringMgr) throws RemoteException {
        OperationalStringManager preparedOpStringMgr =
            (OperationalStringManager)operationalStringManagerPreparer.prepareProxy(opStringMgr);
        logger.trace("Prepared OperationalStringManager proxy: {}", preparedOpStringMgr.toString());
        container.update(sElements, preparedOpStringMgr);
    }

    /**
     * @see org.rioproject.deploy.ServiceBeanInstantiator#getName()
     */
    public String getName() {
        return instantiatorID;
    }

    /**
     * Implemented to support interface contract. The CybernodeProxy returns
     * the result of this method without a backend invocation. For completeness
     * this method will return the Uuid as well
     *
     * @see org.rioproject.deploy.ServiceBeanInstantiator#getInstantiatorUuid()
     */
    public Uuid getInstantiatorUuid() {
        return(getUuid());
    }

    public InetAddress getInetAddress() {
        return getComputeResource().getAddress(); 
    }

    /**
     * Notification that a ServiceBean has been instantiated
     *
     * @param serviceRecord The ServiceRecord
     */
    public void serviceInstantiated(ServiceRecord serviceRecord) {
        ServiceStatement statement = serviceStatementManager.get(serviceRecord.getServiceElement());
        if(statement==null) {
            /* ServiceStatement not found, create one */
            statement = new ServiceStatement(serviceRecord.getServiceElement());
        }
        statement.putServiceRecord(getUuid(), serviceRecord);
        serviceStatementManager.record(statement);
        //setChanged(StatusType.NORMAL);
        if(loaderLogger.isInfoEnabled()) {
            int instantiatedServiceCount = getInstantiatedServiceCount();
            ServiceElement service = serviceRecord.getServiceElement();
            logger.info("Instantiated {}, {}, total services active: {}",
                        ServiceLogUtil.logName(service),
                        ServiceLogUtil.discoveryInfo(service),
                        instantiatedServiceCount);
        }
    }

    /**
     * Notification that a ServiceBean has been discarded
     *
     * @param serviceRecord The ServiceRecord
     */
    public void serviceDiscarded(ServiceRecord serviceRecord) {
        if(serviceRecord==null) {
            logger.warn("ServiceRecord is null when discarding ServiceBean");
        } else {
            ServiceStatement statement = serviceStatementManager.get(serviceRecord.getServiceElement());
            if(statement!=null) {
                if(serviceRecord.getType()!=ServiceRecord.INACTIVE_SERVICE_RECORD) {
                    serviceRecord.setType(ServiceRecord.INACTIVE_SERVICE_RECORD);
                    logger.warn("Fixing ServiceRecord for {}, notified as being discarded, but has ServiceRecord.ACTIVE_SERVICE_RECORD",
                                   ServiceLogUtil.logName(serviceRecord.getServiceElement()));
                }
                statement.putServiceRecord(getUuid(), serviceRecord);
                serviceStatementManager.record(statement);
                if(loaderLogger.isInfoEnabled()) {
                    int instantiatedServiceCount = getInstantiatedServiceCount();
                    loaderLogger.info("Discarded {}, total services active: {}",
                                      ServiceLogUtil.logName(serviceRecord.getServiceElement()), instantiatedServiceCount);
                }
            } else {
                loaderLogger.warn("ServiceStatement is null when discarding ServiceBean {}",
                                  ServiceLogUtil.logName(serviceRecord.getServiceElement()));
            }
            //setChanged(StatusType.NORMAL);
        }
        if (svcConsumer != null)
            svcConsumer.updateMonitors();
    }

    private int getInstantiatedServiceCount() {
        int instantiatedServiceCount = 0;
        ServiceRecord[] records = container.getServiceRecords();
        for (ServiceRecord record : records) {
            if (record.getType() == ServiceRecord.ACTIVE_SERVICE_RECORD)
                instantiatedServiceCount++;
        }
        return instantiatedServiceCount;
    }

    /*
     * Get the ServiceBeanContext and bootstrap the Cybernode
     */
    private void bootstrap(String[] configArgs) throws Exception {
        logger.trace("Entering bootstrap");
        context = ServiceBeanActivation.getServiceBeanContext(getConfigComponent(),
                                                              "Cybernode",
                                                              configArgs,
                                                              getClass().getClassLoader());
        logger.trace("Obtained ServiceBeanContext {}", context);
        BannerProvider bannerProvider = (BannerProvider)context.getConfiguration().getEntry(getConfigComponent(),
                                                                                            "bannerProvider",
                                                                                            BannerProvider.class,
                                                                                            new BannerProviderImpl());
        logger.info(bannerProvider.getBanner(context.getServiceElement().getName()));
        start(context);
        LifeCycleManager lMgr = (LifeCycleManager)context. getServiceBeanManager().getDiscardManager();
        lMgr.register(getServiceProxy(), context);
    }

    /**
     * Get the component name to use for accessing the services configuration
     * properties
     *
     * @return The component name
     */
    public static String getConfigComponent() {
        return(configComponent);
    }

    /**
     * Set the component name to use for accessing the services configuration
     * properties
     *
     * @param comp The component name
     */
    protected void setConfigComponent(String comp) {
        if(comp!=null)
            configComponent = comp;
    }

    /**
     * Override ServiceBeanAdapter createProxy to return a Cybernode Proxy
     */
    @Override
    protected Object createProxy() {
        Cybernode cybernode = (Cybernode)getExportedProxy();
        if(cybernode==null) {
            logger.error("Could not get the exported proxy for the Cybernode, " +
                          "returning null. The Cybernode will not be able to " +
                          "accept remote inbound communications");
            return null;
        }
        Object proxy = CybernodeProxy.getInstance(cybernode, getUuid());
        logger.trace("Proxy created {}", proxy);
        /* Get the registry port */
        String sPort = System.getProperty(Constants.REGISTRY_PORT, "0");
        registryPort = Integer.parseInt(sPort);
        String name = context.getServiceBeanConfig().getName();

        if(registryPort!=0) {
            try {
                String address = HostUtil.getHostAddressFromProperty(Constants.RMI_HOST_ADDRESS);
                Registry registry = LocateRegistry.getRegistry(address, registryPort);
                try {
                    registry.bind(name, (Remote)proxy);
                    logger.debug("Bound to RMI Registry on port={}", registryPort);
                } catch(AlreadyBoundException e) {
                    /*ignore */
                }
            } catch(AccessException e) {
                logger.warn("Binding "+name+" to RMI Registry", e);
            } catch(RemoteException e) {
                logger.warn("Binding "+name+" to RMI Registry", e);
            } catch (java.net.UnknownHostException e) {
                logger.warn("Unknown host address locating RMI Registry", e);
            }
        } else {
            logger.debug("RMI Registry property not set, unable to bind {}", name);
        }
        /*
         * Set the MarshalledInstance into the ServiceBeanManager
         */
        try {
            MarshalledInstance mi = new MarshalledInstance(proxy);
            ((DefaultServiceBeanManager)context.getServiceBeanManager()).setMarshalledInstance(mi);
        } catch (IOException e) {
            logger.warn("Unable to create MarshalledInstance for Cybernode proxy, non-fatal error, continuing ...", e);
        }        
        return(proxy);
    }

    /**
     * Override parent's getAdmin to return custom service admin
     *
     * @return A CybernodeAdminProxy instance
     */
    public Object getAdmin() {
        Object adminProxy = null;
        try {
            if(admin == null) {
                Exporter adminExporter = getAdminExporter();
                if (contextMgr != null)
                    admin = new CybernodeAdminImpl(this, adminExporter, contextMgr.getContextAttributeLogHandler());
                else
                    admin = new CybernodeAdminImpl(this, adminExporter);
            }
            admin.setServiceBeanContext(getServiceBeanContext());
            ((CybernodeAdminImpl)admin).setRegistryPort(registryPort);
            adminProxy = admin.getServiceAdmin();
        } catch (Throwable t) {
            logger.error("Getting CybernodeAdminImpl", t);
        }
        return (adminProxy);
    }

    /**
     * @see org.rioproject.servicebean.ServiceBean#initialize
     */
    public void initialize(ServiceBeanContext context) throws Exception {
        /*
         * Create the instantiatorID. This will be used as the name set in the proxy, and used to track
         * registrations and updates in the ProvisionMonitor
         */
        StringBuilder instantiatorIDBuilder = new StringBuilder();
        instantiatorIDBuilder.append(context.getServiceElement().getName()).append("-");
        String name = ManagementFactory.getRuntimeMXBean().getName();
        String pid = name;
        int ndx = name.indexOf("@");
        if(ndx>=1) {
            pid = name.substring(0, ndx);
        }
        instantiatorIDBuilder.append(pid).append("@");
        instantiatorIDBuilder.append(InetAddress.getLocalHost().getHostName());
        instantiatorID = instantiatorIDBuilder.toString();

        /*
         * Determine if a log directory has been provided. If so, create a
         * PersistentStore
         */
        String logDirName = 
            (String)context.getConfiguration().getEntry(CONFIG_COMPONENT, "logDirectory", String.class, null);
        if(logDirName!=null) {
            /* LogHandler required when dealing with the ReliableLog */
            CybernodeLogHandler logHandler = new CybernodeLogHandler();
            store = new PersistentStore(logDirName, logHandler, logHandler);
            logger.debug("Cybernode: using absolute logdir path [{}]", store.getStoreLocation());
            store.snapshot();
            super.initialize(context, store);
        } else {
            super.initialize(context);
        }
        
        /* Get the Configuration */
        config = context.getConfiguration();
        try {
            Exporter defaultExporter = (Exporter)config.getEntry(RIO_CONFIG_COMPONENT,
                                                                 DEFAULT_EXPORTER,
                                                                 Exporter.class,
                                                                 null);
            if(defaultExporter==null)
                defaultExporter = new BasicJeriExporter(ExporterConfig.getServerEndpoint(), new BasicILFactory());
            logger.trace("{} has been set as the defaultExporter", defaultExporter);
        } catch(Exception e) {
            logger.error("The {}.{} attribute must be set", RIO_CONFIG_COMPONENT, DEFAULT_EXPORTER);
            throw e;
        }

        /* Set up thread deadlock detection. This seems a little clumsy to use
         * the WatchInjector and could probably be improved. This does make the
         * most use of the wiring for forked service monitoring. */
        long threadDeadlockCheck = (Long)config.getEntry(CONFIG_COMPONENT,
                                                         "threadDeadlockCheck",
                                                         long.class, (long)5000);
        if(threadDeadlockCheck>=1000) {

            WatchDescriptor threadDeadlockDescriptor = ThreadDeadlockMonitor.getWatchDescriptor();
            threadDeadlockDescriptor.setPeriod(threadDeadlockCheck);
            Method getThreadDeadlockCalculable = ThreadDeadlockMonitor.class.getMethod("getThreadDeadlockCalculable");
            ThreadDeadlockMonitor threadDeadlockMonitor = new ThreadDeadlockMonitor();
            threadDeadlockMonitor.setThreadMXBean(ManagementFactory.getThreadMXBean());

            WatchInjector watchInjector = new WatchInjector(this, context);
            ThresholdWatch watch = (ThresholdWatch)watchInjector.inject(threadDeadlockDescriptor,
                                                                        threadDeadlockMonitor,
                                                                        getThreadDeadlockCalculable);
            watch.setThresholdValues(new ThresholdValues(0, 1));
            watch.addThresholdListener(new DeadlockedThreadPolicyHandler());
        } else {
            logger.info("Thread deadlock monitoring has been disabled. The " +
                        "configured thread deadlock check time was " +
                        "[{}]. To enable thread deadlock monitoring, the thread deadlock check " +
                        "time must be >= 1000 milliseconds.", threadDeadlockCheck);
        }
        try {
            serviceLimit = Config.getIntEntry(config, getConfigComponent(), "serviceLimit", 500, 0, Integer.MAX_VALUE);
        } catch(Exception e) {
            logger.warn("Exception getting serviceLimit, default to 500");
        }

        /* Get the ProxyPreparer for passed in OperationalStringManager
         * instances */
        operationalStringManagerPreparer = (ProxyPreparer)config.getEntry(getConfigComponent(),
                                                                          "operationalStringManagerPreparer",
                                                                          ProxyPreparer.class,
                                                                          new BasicProxyPreparer());
        /* Check for JMXConnection */
        addAttributes(JMXUtil.getJMXConnectionEntries());

        /* Add service UIs programmatically */
        addAttributes(getServiceUIs());

        /* Get the security policy to apply to loading services */
        String serviceSecurityPolicy = (String)config.getEntry(getConfigComponent(),
                                                               "serviceSecurityPolicy",
                                                               String.class,
                                                               null);
        if(serviceSecurityPolicy!=null)
            System.setProperty("rio.service.security.policy", serviceSecurityPolicy);

        /* Establish default operating environment */
        Environment.setupDefaultEnvironment();

        /* Setup persistent provisioning attributes */
        provisionEnabled = (Boolean) config.getEntry(getConfigComponent(), "provisionEnabled", Boolean.class, true);
        /*
         * The directory to provision external software to. This is the root
         * directory where software may be installed by ServiceBean instances which
         * require external software to be resident on compute resource the
         * Cybernode represents
         */
        String provisionRoot = Environment.setupProvisionRoot(provisionEnabled, config);
        if(provisionEnabled) {
            if(logger.isTraceEnabled())
                logger.trace("Software provisioning has been enabled, default provision root location is [{}]",
                             provisionRoot);
        }
        computeResource.setPersistentProvisioningRoot(provisionRoot);

        /* Setup the native library directory */
        String nativeLibDirectories = Environment.setupNativeLibraryDirectories(config);

        /* Ensure org.rioproject.system.native property is set. This will be used
         * by the org.rioproject.system.SystemCapabilities class */
        if(nativeLibDirectories!=null)
            System.setProperty(SystemCapabilities.NATIVE_LIBS, nativeLibDirectories);

        /* Initialize the ComputeResource */
        initializeComputeResource(computeResource);

        if(logger.isTraceEnabled()) {
            StringBuilder sb = new StringBuilder();
            sb.append("Service Limit : ").append(serviceLimit).append("\n");
            sb.append("System Capabilities\n");
            MeasurableCapability[] mCaps = computeResource.getMeasurableCapabilities();
            sb.append("MeasurableCapabilities : (").
               append(mCaps.length).
               append(")\n");
            for (MeasurableCapability mCap : mCaps) {
                sb.append("\t").append(mCap.getId()).append("\n");
                ThresholdValues tValues = mCap.getThresholdValues();
                sb.append("\t\tlow threshold  : ").append(tValues.getLowThreshold()).append("\n");
                sb.append("\t\thigh threshold : ").append(tValues.getHighThreshold()).append("\n");
                sb.append("\t\treport rate    : ").append(mCap.getPeriod()).append("\n");
                sb.append("\t\tsample size    : ");
                sb.append(mCap.getSampleSize()).append("\n");
            }
            PlatformCapability[] pCaps = computeResource.getPlatformCapabilities();
            sb.append("PlatformCapabilities : (").append(pCaps.length).append(")\n");

            double KB = 1024;
            double MB = Math.pow(KB, 2);
            double GB = Math.pow(KB, 3);
            NumberFormat nf = NumberFormat.getInstance();
            nf.setMaximumFractionDigits(2);
            
            for (PlatformCapability pCap : pCaps) {
                boolean convert = false;
                if(pCap.getClass().getName().contains("StorageCapability") ||
                   pCap.getClass().getName().contains("Memory")) {
                    convert = true;
                }
                sb.append("\t").append(stripPackageName(pCap.getClass().getName())).append("\n");
                String[] keys = pCap.getPlatformKeys();
                for (String key : keys) {
                    Object value = pCap.getValue(key);
                    if(convert && value instanceof Double) {
                        if(pCap.getClass().getName().contains("StorageCapability")) {
                            double d = ((Double)value)/GB;
                            value = nf.format(d)+" GB";
                        } else {
                            double d = ((Double)value)/MB;
                            value = nf.format(d)+" MB";
                        }
                    }
                    sb.append("\t\t").append(key).append(" : ").append(value).append("\n");
                }
                String path = pCap.getPath();
                if (path != null)
                    sb.append("\t\tPath : ").append(path).append("\n");
            }
            logger.trace(sb.toString());
        }

        /* Create the ServiceStatementManager */
        serviceStatementManager = Environment.getServiceStatementManager(config);

        /* Start the timerTask for updating ServiceRecords */
        long serviceRecordUpdateTaskTimer = 60;
        try {
            serviceRecordUpdateTaskTimer = Config.getLongEntry(config,
                                                               getConfigComponent(),
                                                               "serviceRecordUpdateTaskTimer",
                                                               serviceRecordUpdateTaskTimer,
                                                               1,
                                                               Long.MAX_VALUE);
        } catch(Throwable t) {
            logger.warn("Exception getting slaThresholdTaskPoolMinimum", t);
        }
        taskTimer = new Timer(true);
        long period = 1000*serviceRecordUpdateTaskTimer;
        long now = System.currentTimeMillis();
        serviceRecordUpdateTask = new ServiceRecordUpdateTask();
        taskTimer.scheduleAtFixedRate(serviceRecordUpdateTask, new Date(now+period), period);
        /*
         * Create a thread pool for processing SLAThresholdEvent
         */
        thresholdTaskPool = Executors.newCachedThreadPool();
        /*
         * Create event descriptor for the SLAThresholdEvent and add it as
         * an attribute
         */
        EventDescriptor thresholdEventDesc = SLAThresholdEvent.getEventDescriptor();
        getEventTable().put(thresholdEventDesc.eventID, getSLAEventHandler());
        addAttribute(thresholdEventDesc);        

        addAttribute(new BasicStatus(StatusType.NORMAL));

        /* Create the container */
        createContainer();

        /* Create the consumer which will discover, register and maintain
         * connection(s) to ProvisionMonitor instances */
        svcConsumer =
            new ServiceConsumer(new CybernodeAdapter((ServiceBeanInstantiator)getServiceProxy(),this, computeResource),
                                serviceLimit,
                                config);

        /* Get the property that determines whether instantiated services
         * should be terminated upon unregistration */
        serviceTerminationOnUnregister = (Boolean) config.getEntry(getConfigComponent(),
                                                                   "serviceTerminationOnUnregister",
                                                                   Boolean.class,
                                                                   Boolean.TRUE);
        logger.debug("serviceTerminationOnUnregister={}", serviceTerminationOnUnregister);

        /* Get whether the Cybernode will make itself available as an asset */
        boolean doEnlist = (Boolean)config.getEntry(getConfigComponent(), "enlist", Boolean.class, true);
        if(doEnlist) {
            doEnlist();
        } else {
            logger.info("Do not enlist with ProvisionManagers as an instantiation resource");
        }

        /* Create a computeResourcePolicyHandler to watch for thresholds
         * being crossed */
        computeResourcePolicyHandler = new ComputeResourcePolicyHandler(context.getServiceElement(),
                                                                        getSLAEventHandler(),
                                                                        svcConsumer,
                                                                        makeServiceBeanInstance());
        computeResource.addThresholdListener(computeResourcePolicyHandler);

        /* Ensure we have a serviceID */
        if(serviceID==null) {
            serviceID = new ServiceID(getUuid().getMostSignificantBits(), getUuid().getLeastSignificantBits());
            logger.debug("Created new ServiceID: {}", serviceID.toString());
        }

        /* Log discovery setup */
        logger.info("Started Cybernode [{}]", JiniClient.getDiscoveryAttributes(context));

        loadInitialServices(context.getConfiguration());
        /*
         * Force a snapshot so the persistent store reflects the current state
         * of the Cybernode
         */
        if(store!=null)
            store.snapshot();
    }

    /*
     * Get the name of the class sans the package name
     */
    private String stripPackageName(String className) {
        int ndx = className.lastIndexOf(".");
        if(ndx==-1)
            return className;
        return className.substring(ndx+1);
    }

    /**
     * Load any initial services
     *
     * @param config The Jini configuration to use
     */
    protected void loadInitialServices(Configuration config) {
        /* Get the timeout value for loading initial services */
        long initialServiceLoadDelay = 1000*5;
        try {
            initialServiceLoadDelay =
                Config.getLongEntry(config,
                                    getConfigComponent(),
                                    "initialServiceLoadDelay",
                                    initialServiceLoadDelay,
                                    0,
                                    Long.MAX_VALUE);
        } catch(Throwable t) {
            logger.warn("Exception getting initialServiceLoadDelay", t);
        }
        logger.debug("initialServiceLoadDelay={}", initialServiceLoadDelay);

        /*
        * Schedule the task to Load any configured services
        */
        if(initialServiceLoadDelay>0) {
            long now = System.currentTimeMillis();
            getTaskTimer().schedule(new InitialServicesLoadTask(config), new Date(now+initialServiceLoadDelay));
        } else {
            InitialServicesLoadTask serviceLoader = new InitialServicesLoadTask(config);
            serviceLoader.loadInitialServices();
        }
    }

    /**
     * Scheduled Task which will load configured services
     */
    class InitialServicesLoadTask extends TimerTask {
        Configuration config;

        /**
         * Create a InitialServicesLoadTask
         *
         * @param config Configuration, must not be null
         */
        InitialServicesLoadTask(Configuration config) {
            if(config==null)
                throw new IllegalArgumentException("config is null");
            this.config = config;
        }

        /**
         * The action to be performed by this timer task.
         */
        public void run() {
            loadInitialServices();
        }

        void loadInitialServices() {
            /* Load initial services */
            String[] initialServices = new String[] {};
            try {
                initialServices = (String[])config.getEntry(getConfigComponent(),
                                                            "initialServices",
                                                            String[].class,
                                                            initialServices);
            } catch(ConfigurationException e) {
                logger.warn("Getting initialServices", e);
            }

            logger.debug("Loading [{}] initialServices", initialServices.length);
            for (String initialService : initialServices) {
                URL deploymentURL;
                try {
                    if (initialService.startsWith("http:"))
                        deploymentURL = new URL(initialService);
                    else
                        deploymentURL = new File(initialService).toURI().toURL();
                    load(deploymentURL);
                } catch (Throwable t) {
                    logger.error("Loading initial services : {}", initialService, t);
                }
            }
        }

        /*
         * Load and activate services
         */
        void load(URL deploymentURL) {
            if(deploymentURL == null)
                throw new IllegalArgumentException("Deployment URL cannot be null");
            try {
                /* Get the OperationalString loader */
                OpStringLoader opStringLoader = new OpStringLoader(this.getClass().getClassLoader());
                OperationalString[] opStrings = opStringLoader.parseOperationalString(deploymentURL);
                if(opStrings != null) {
                    for (OperationalString opString : opStrings) {
                        logger.debug("Activating Deployment [{}]", opString.getName());
                        ServiceElement[] services = opString.getServices();
                        for (ServiceElement service : services) {
                            activate(service);
                        }
                    }
                }
            } catch(Throwable t) {
                logger.warn("Activating OperationalString", t);
            }
        }
    }

    /*
     * Create a ServiceProvisionEvent and instantiate the ServiceElement
     */
    private void activate(ServiceElement sElem) {
        ServiceProvisionEvent spe = new ServiceProvisionEvent(getServiceProxy(), null, sElem);
        try {
            instantiate(spe);
        } catch (Throwable t) {
            logger.warn("Activating service [{}]", sElem.getName(), t);
        }
    }

    /**
     * Get the enlisted state
     */
    public boolean isEnlisted() {
        return(enlisted);
    }

    /**
     * Set the enlisted flag
     *
     * @param value True if enlisted, false if not
     */
    protected void setEnlisted(boolean value) {
        enlisted = value;
    }

    /**
     * Have the Cybernode add itself as a resource which can be
     * used to instantiate dynamic application services.
     *
     * If the Cybernode is already enlisted, this method will have
     * no effect
     */
    protected void doEnlist() {
        if(isEnlisted()) {
            logger.debug("Already enlisted");
            return;
        }
        logger.info("Enlist with ProvisionManagers as an instantiation resource using: {}", instantiatorID);
        try {
            svcConsumer.initializeProvisionDiscovery(context.getDiscoveryManagement());
        } catch(Exception e) {
            logger.warn("Initializing Provision discovery", e);
        }

        setEnlisted(true);
    }

    /**
     * Get the Timer created for the Cybernode
     *
     * @return The task Timer used to schedule tasks
     */
    protected Timer getTaskTimer() {
        return(taskTimer);
    }

    /**
     * Get the {@link net.jini.lookup.entry.ServiceInfo} for the Cybernode
     *
     * @return The ServiceInfo
     */
    @Override
    protected ServiceInfo getServiceInfo() {
        URL implUrl = getClass().getProtectionDomain().getCodeSource().getLocation();
        RioManifest rioManifest;
        String build = null;
        try {
            rioManifest = new RioManifest(implUrl);
            build = rioManifest.getRioBuild();
        } catch(IOException e) {
            logger.warn("Getting Rio Manifest", e);
        }
        if(build==null)
            build="0";
        return new ServiceInfo(context.getServiceElement().getName(),
                               "Asarian Technologies LLC",
                               "Rio Project",
                               RioVersion.VERSION,
                               "Build "+build,
                               "");
    }

    private URL[] getUIJars() throws MalformedURLException {
        return new URL[]{new URL("artifact:org.rioproject.cybernode:cybernode-ui:"+RioVersion.VERSION),
                         new URL("artifact:org.rioproject:watch-ui:"+RioVersion.VERSION)};
    }

    /**
     * Override parents getWatchUI, using cybernode-ui.jar as the JAR containing
     * org.rioproject.watch.AccumulatorViewer
     * @throws MalformedURLException
     * @throws IOException
     */
    protected Entry getWatchUI() throws IOException {
        return(UIDescriptorFactory.getUIDescriptor(
                AdminUI.ROLE,
                new UIComponentFactory(getUIJars(), "org.rioproject.watch.AccumulatorViewer")));
    }

    /**
     * Get the service UIs to add
     *
     * @return An array of UIDescriptors used for the service-UIs for the
     * Cybernode.
     *
     * @throws IOException If the UIDescriptors cannot be created
     */
    protected Entry[] getServiceUIs() throws IOException {
        UIComponentFactory cybernodeUI = new UIComponentFactory(getUIJars(),
                                                                "org.rioproject.cybernode.ui.PlatformCapabilityUI");

        UIComponentFactory platformCapabilityUI = new UIComponentFactory(getUIJars(),
                                                                         "org.rioproject.cybernode.ui.CybernodeUI");

        Entry[] uis = new Entry[] {UIDescriptorFactory.getUIDescriptor(AdminUI.ROLE, platformCapabilityUI),
                                   UIDescriptorFactory.getUIDescriptor(AdminUI.ROLE, cybernodeUI)};
        return(uis);
    }

    protected ServiceBeanContainer getServiceBeanContainer() {
        return container;
    }

    /**
     * @see org.rioproject.deploy.ServiceBeanInstantiator#instantiate
     */
    public DeployedService instantiate(ServiceProvisionEvent event)
        throws ServiceBeanInstantiationException, UnknownEventException {

        DeployedService deployedService;
        try {
            if(shutdownSequence.get()) {
                StringBuilder builder = new StringBuilder();
                builder.append(ServiceLogUtil.logName(event)).append(" shutting down, unavailable for service instantiation");
                logger.warn(builder.toString());
                throw new ServiceBeanInstantiationException(builder.toString());
            }
            loaderLogger.info("Instantiating {}", ServiceLogUtil.logName(event));

            if(event.getID()!=ServiceProvisionEvent.ID) {
                logger.warn("Unknown event type [{}], ID={}", event.getClass().getName(), event.getID());
                throw new UnknownEventException("Unknown event type ["+event.getID()+"]");
            }

            /* Verify that the Cybernode does not instantiate a service past
             * it's specified max per machine or planned value
             *
             * This may occur if a provisioner is in the process of deploying
             * multiple instances of a service, and service instantiation time takes
             * longer then expected, or the off chance that multiple provisioners
             * may be managing the same opstring and may not be out of synch with
             * each other */
            synchronized(inProcess) {
                /* Get the number of active instances for the ServiceElement */
                int instantiatedServiceCount = 0;
                ServiceRecord[] records = container.getServiceRecords();
                for (ServiceRecord record : records) {
                    if (record.getType() == ServiceRecord.ACTIVE_SERVICE_RECORD &&
                        record.getServiceElement().equals(event.getServiceElement()))
                        instantiatedServiceCount++;
                }
                /* Get the number of instances that are in the process of being
                 * activated for the ServiceElement */
                int inProcessServiceCount = 0;
                for(ServiceProvisionEvent spe : inProcess) {
                    if(spe.getServiceElement().equals(event.getServiceElement()))
                        inProcessServiceCount++;
                }

                /* Set the temporal service count to be equal to the in process
                 * count and active instances */
                int activeServiceCounter = inProcessServiceCount+instantiatedServiceCount;
                if(loaderLogger.isTraceEnabled())
                    loaderLogger.trace("{} activeServiceCounter=[{}], inProcessServiceCount=[{}], instantiatedServiceCount=[{}]",
                                        ServiceLogUtil.logName(event), activeServiceCounter, inProcessServiceCount, instantiatedServiceCount);
                /* First check max per machine */
                int maxPerMachine = event.getServiceElement().getMaxPerMachine();
                if(maxPerMachine!=-1 && activeServiceCounter >= maxPerMachine) {
                    if(loaderLogger.isTraceEnabled())
                        loaderLogger.trace("Abort allocation of {} "+
                                            "activeServiceCounter=[{}] "+
                                            "inProcessServiceCount=[{}] "+
                                            "maxPerMachine=[{}]",
                                            ServiceLogUtil.logName(event), activeServiceCounter, inProcessServiceCount, maxPerMachine);
                    throw new ServiceBeanInstantiationException("MaxPerMachine "+"["+maxPerMachine+"] has been reached");
                }

                /* The check planned service count */
                int numPlannedServices = event.getServiceElement().getPlanned();
                if(activeServiceCounter >= numPlannedServices) {
                    if(loaderLogger.isTraceEnabled())
                        loaderLogger.trace("Cancel allocation of {} activeServiceCounter=[{}] numPlannedServices=[{}]",
                                           ServiceLogUtil.logName(event), activeServiceCounter, numPlannedServices+"]");
                    return(null);
                }

                inProcess.add(event);
            }

            int inProcessCount;
            synchronized(inProcess) {
                inProcessCount = inProcess.size() - container.getActivationInProcessCount();
            }
            int containerServiceCount = container.getServiceCounter();
            if((inProcessCount+containerServiceCount) <= serviceLimit) {
                OperationalStringManager opMgr = event.getOperationalStringManager();
                if(!event.getServiceElement().forkService()) {
                    if(loaderLogger.isTraceEnabled())
                        loaderLogger.trace("Get OpStringManagerProxy for {}", ServiceLogUtil.logName(event));
                    try {
                        opMgr = OpStringManagerProxy.getProxy(
                                                                 event.getServiceElement().getOperationalStringName(),
                                                                 event.getOperationalStringManager(),
                                                                 context.getDiscoveryManagement());
                        if(loaderLogger.isTraceEnabled())
                            loaderLogger.trace("Got OpStringManagerProxy for {}", ServiceLogUtil.logName(event));
                    } catch (Exception e) {
                        loaderLogger.warn("Unable to create proxy for OperationalStringManager, " +
                                          "using provided OperationalStringManager",
                                          e);
                        if(shutdownSequence.get()) {
                            throw new ServiceBeanInstantiationException(
                                String.format("Cancel allocation of %s, Cybernode is shutting down",
                                              ServiceLogUtil.logName(event)));
                        }
                        opMgr = event.getOperationalStringManager();
                    }
                }
                try {
                    loaderLogger.trace("Activating {}", ServiceLogUtil.logName(event));
                    ServiceBeanInstance jsbInstance = container.activate(event.getServiceElement(),
                                                                         opMgr,
                                                                         getSLAEventHandler());
                    loaderLogger.trace("Activated {}", ServiceLogUtil.logName(event));
                    ServiceBeanDelegate delegate = container.getServiceBeanDelegate(jsbInstance.getServiceBeanID());
                    ComputeResourceUtilization cru = null;
                    if(delegate!=null) {
                        cru = delegate.getComputeResourceUtilization();
                    }
                    deployedService = new DeployedService(event.getServiceElement(), jsbInstance, cru);
                    loaderLogger.trace("Created DeployedService for {}", ServiceLogUtil.logName(event));
                } catch(ServiceBeanInstantiationException e) {
                    if(opMgr instanceof OpStringManagerProxy.OpStringManager) {
                        try {
                            ((OpStringManagerProxy.OpStringManager)opMgr).terminate();
                        } catch(IllegalStateException ex) {
                            logger.warn("Shutting down OpStringManagerProxy more then once for service {}",
                                        ServiceLogUtil.logName(event));
                        }
                    }
                    throw e;
                }
                return(deployedService);
            } else {
                throw new ServiceBeanInstantiationException("Service Limit of ["+serviceLimit+"] has been reached");
            }

        } finally {
            synchronized(inProcess) {
                inProcess.remove(event);
            }
        }
    }
    
    /**
     * Create the container the Cybernode will use
     *
     * @throws ConfigurationException if the ServiceBeanContainer cannot be
     * created using the configuration
     */
    void createContainer() throws ConfigurationException {
        Configuration config = context.getConfiguration();
        ServiceBeanContainer defaultContainer = new ServiceBeanContainerImpl(config);

        this.container = (ServiceBeanContainer)config.getEntry(getConfigComponent(),
                                                               "serviceBeanContainer",
                                                               ServiceBeanContainer.class,
                                                               defaultContainer,
                                                               config);
        this.container.setUuid(getUuid());
        this.container.setComputeResource(computeResource);
        this.container.addListener(this);
    }

    /**
     * Change the BasicStatus Entry to reflect a change in state
     *
     * @param statusType The StatusType
     */
    void setChanged(StatusType statusType) {
        joiner.modifyAttributes(new Entry[]{new BasicStatus()}, new Entry[]{new BasicStatus(statusType)});
    }

    /**
     * Get the ComputeResource associated with this Cybernode
     *
     * @return The ComputeResource associated with this Cybernode
     */
    public ComputeResource getComputeResource() {
        return(computeResource);
    }

    /**
     * The initializeComputeResource initializes the instantiated ComputeResource
     * object, and adds MeasurableCapability watches to the Watch Registry.
     *
     * @param computeResource The ComputeResource to initialize
     */
    void initializeComputeResource(ComputeResource computeResource) {
        /*
         * Set whether the Cybernode supports persistent provisioning
         */
        computeResource.setPersistentProvisioning(provisionEnabled);
        /*
         * Have the ComputeResource object start all it's MeasurableCapabilities
         */
        computeResource.boot();
        /*
         * Add MeasurableCapability watches to the Watch Registry
         */
        MeasurableCapability[] mCaps = computeResource.getMeasurableCapabilities();
        for (MeasurableCapability mCap : mCaps) {
            getWatchRegistry().register(mCap);
        }
        PlatformCapability[] pCaps = computeResource.getPlatformCapabilities();
        MBeanServer mbeanServer = MBeanServerFactory.getMBeanServer();
        for (PlatformCapability pCap : pCaps) {
            try {
                ObjectName objectName = getObjectName(pCap);
                if (objectName != null)
                    mbeanServer.registerMBean(pCap, objectName);
            } catch (MalformedObjectNameException e) {
                logger.warn("PlatformCapability [{}.{}]", pCap.getClass().getName(), pCap.getName(), e);
            } catch (NotCompliantMBeanException e) {
                logger.warn("PlatformCapability [{}.{}]", pCap.getClass().getName(), pCap.getName(), e);
            } catch (MBeanRegistrationException e) {
                logger.warn("PlatformCapability [{}.{}]", pCap.getClass().getName(), pCap.getName(), e);
            } catch (InstanceAlreadyExistsException e) {
                logger.warn("PlatformCapability [{}.{}]", pCap.getClass().getName(), pCap.getName(), e);
            }
        }
        logger.debug("Will update discovered Provision Monitors with resource utilization details at " +
                     "intervals of {}", TimeUtil.format(computeResource.getReportInterval()));
    }

    /*
     * Get an ObjectName for a PlatformCapability
     */
    private ObjectName getObjectName(PlatformCapability pCap)
        throws MalformedObjectNameException {
        return(JMXUtil.getObjectName(context, "org.rioproject.cybernode", "PlatformCapability", pCap.getName()));
    }

    /*
     * Make a ServiceBeanInstance for the Cybernode
     */
    private ServiceBeanInstance makeServiceBeanInstance() throws IOException {
        return new ServiceBeanInstance(getUuid(),
                                       new MarshalledInstance(getServiceProxy()),
                                       context.getServiceElement().getServiceBeanConfig(),
                                       computeResource.getHostName(),
                                       computeResource.getAddress().getHostAddress(),
                                       getUuid());
    }

    /**
     * Scheduled Task which will update ServiceStatement instances with the latest and
     * greatest ServiceRecords
     */
    class ServiceRecordUpdateTask extends TimerTask {
        /**
         * The action to be performed by this timer task.
         */
        public void run() {
            ServiceRecord[] records = container.getServiceRecords();
            for (ServiceRecord record : records) {
                ServiceStatement statement = serviceStatementManager.get(record.getServiceElement());
                if (statement != null) {
                    statement.putServiceRecord(getUuid(), record);
                    serviceStatementManager.record(statement);
                }
            }
        }
    }


    /**
     * If deadlocked threads are detected fire SLAThresholdEvents
     */
    private class DeadlockedThreadPolicyHandler implements ThresholdListener {

        public void notify(Calculable calculable, ThresholdValues thresholdValues, ThresholdType type) {
            double[] range = new double[]{
                thresholdValues.getCurrentLowThreshold(),
                thresholdValues.getCurrentHighThreshold()
            };

            SLA sla = new SLA(calculable.getId(), range);
            try {
                ServiceBeanInstance instance = makeServiceBeanInstance();
                SLAThresholdEvent event = new SLAThresholdEvent(proxy,
                                                                context.getServiceElement(),
                                                                instance,
                                                                calculable,
                                                                sla,
                                                                "Cybernode Thread Deadlock " +
                                                                "Policy Handler",
                                                                instance.getHostAddress(),
                                                                type);
                thresholdTaskPool.execute(new SLAThresholdEventTask(event, getSLAEventHandler()));
            } catch(Exception e) {
                logger.warn("Could not send a SLA Threshold Notification as a result of compute resource threshold " +
                            "[{}] being crossed", calculable.getId(), e);
            }
        }
    }

    /**
     * Class that manages the persistence details for the Cybernode
     */
    class CybernodeLogHandler extends LogHandler implements SnapshotHandler {

        public void snapshot(OutputStream out) throws IOException {
            ObjectOutputStream oostream = new ObjectOutputStream(out);
            oostream.writeUTF(CybernodeImpl.class.getName());
            oostream.writeInt(LOG_VERSION);
            oostream.flush();
        }

        public void recover(InputStream in) throws Exception {
            ObjectInputStream oistream = new ObjectInputStream(in);
            if(!CybernodeImpl.class.getName().equals(oistream.readUTF()))
                throw new IOException("Log from wrong implementation");
            if(oistream.readInt() != LOG_VERSION)
                throw new IOException("Wrong log format version");
        }

        /**
         * This method always throws <code>UnsupportedOperationException</code>
         * since <code>CybernodeLogHandler</code> should never update a log.
         */
        public void applyUpdate(Object update) throws Exception {
            throw new UnsupportedOperationException("CybernodeLogHandler : "+
                                                    "Recovering log update this "+
                                                    "should not happen");
        }

        public void updatePerformed(int updateCount) {
            // empty implementation
        }

        public void takeSnapshot() throws IOException {
            store.snapshot();
        }
    }

    /*---------------------
     * Cybernode
     * --------------------*/

    /*
     * @see org.rioproject.cybernode.Cybernode#enlist
     */
    public void enlist() {
        doEnlist();
    }

    /*
     * @see org.rioproject.cybernode.Cybernode#release(boolean)
     */
    public void release(boolean terminateServices) {
        doRelease();
        setEnlisted(false);
    }

    private void doRelease() {
        logger.debug("Unregister from ProvisionMonitor instances ");
        svcConsumer.cancelRegistrations();
        if(serviceTerminationOnUnregister)
            container.terminateServices();
    }

    /*---------------------
     * CybernodeAdmin
     *---------------------*/

    /*
     * @see org.rioproject.cybernode.CybernodeAdmin#getServiceLimit
     */
    public Integer getServiceLimit() {
        return(serviceLimit);
    }

    /*
     * @see org.rioproject.cybernode.CybernodeAdmin#setServiceLimit
     */
    public void setServiceLimit(Integer limit) {
        this.serviceLimit = limit;
        svcConsumer.updateMonitors(serviceLimit);
    }

    /*
     * @see org.rioproject.cybernode.CybernodeAdmin#getServiceCount
     */
    public Integer getServiceCount() {
        return(container.getServiceCounter());
    }

    /*
     * @see org.rioproject.cybernode.CybernodeAdmin#getPersistentProvisioning
     */
    public boolean getPersistentProvisioning() {
        return(provisionEnabled);
    }

    /*
     * @see org.rioproject.cybernode.CybernodeAdmin#setPersistentProvisioning
     */
    public void setPersistentProvisioning(boolean provisionEnabled) throws IOException {
        if(this.provisionEnabled==provisionEnabled)
            return;
        this.provisionEnabled = provisionEnabled;
        if(this.provisionEnabled)
            Environment.setupProvisionRoot(this.provisionEnabled, config);
        computeResource.setPersistentProvisioning(this.provisionEnabled);
    }
             
    public double getUtilization() {
        return(computeResource.getUtilization());
    }

}
