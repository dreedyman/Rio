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
package org.rioproject.cybernode;

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
import net.jini.lookup.entry.ServiceInfo;
import net.jini.lookup.entry.StatusType;
import net.jini.lookup.ui.AdminUI;
import net.jini.security.BasicProxyPreparer;
import net.jini.security.ProxyPreparer;
import net.jini.security.TrustVerifier;
import net.jini.security.proxytrust.ServerProxyTrust;
import org.rioproject.RioVersion;
import org.rioproject.config.Constants;
import org.rioproject.core.jsb.DiscardManager;
import org.rioproject.core.jsb.ServiceBeanContext;
import org.rioproject.core.jsb.ServiceBeanManager;
import org.rioproject.deploy.*;
import org.rioproject.entry.BasicStatus;
import org.rioproject.entry.UIDescriptorFactory;
import org.rioproject.event.EventDescriptor;
import org.rioproject.event.EventHandler;
import org.rioproject.jmx.JMXUtil;
import org.rioproject.jmx.MBeanServerFactory;
import org.rioproject.jsb.JSBContext;
import org.rioproject.jsb.JSBManager;
import org.rioproject.jsb.ServiceBeanActivation;
import org.rioproject.jsb.ServiceBeanActivation.LifeCycleManager;
import org.rioproject.jsb.ServiceBeanAdapter;
import org.rioproject.net.HostUtil;
import org.rioproject.opstring.*;
import org.rioproject.resources.client.DiscoveryManagementPool;
import org.rioproject.resources.persistence.PersistentStore;
import org.rioproject.resources.persistence.SnapshotHandler;
import org.rioproject.serviceui.UIComponentFactory;
import org.rioproject.resources.util.ThrowableUtil;
import org.rioproject.util.TimeUtil;
import org.rioproject.sla.SLA;
import org.rioproject.sla.SLAThresholdEvent;
import org.rioproject.system.ComputeResource;
import org.rioproject.system.ComputeResourceUtilization;
import org.rioproject.system.SystemCapabilities;
import org.rioproject.system.capability.PlatformCapability;
import org.rioproject.system.measurable.MeasurableCapability;
import org.rioproject.util.BannerProvider;
import org.rioproject.util.BannerProviderImpl;
import org.rioproject.util.RioManifest;
import org.rioproject.watch.*;

import javax.management.*;
import java.io.*;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.rmi.*;
import java.rmi.activation.ActivationID;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.text.NumberFormat;
import java.util.*;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Implementation of a Cybernode
 *
 * @link {org.rioproject.jmx.ThreadDeadlockMonitor#ID}
 *
 * @author Dennis Reedy
 */
public class CybernodeImpl extends ServiceBeanAdapter implements Cybernode,
                                                                 ServiceBeanContainerListener,
                                                                 CybernodeImplMBean,
                                                                 ServerProxyTrust {
    
    /** Component name we use to find items in the Configuration */
    static final String RIO_CONFIG_COMPONENT = "org.rioproject";
    /** Component name we use to find items in the Configuration */
    static final String CONFIG_COMPONENT = RIO_CONFIG_COMPONENT+".cybernode";
    /** Default Exporter attribute */
    static final String DEFAULT_EXPORTER = "defaultExporter";
    /** Logger name */
    static final String LOGGER = "org.rioproject.cybernode";
    /** Cybernode logger. */
    static final Logger logger = Logger.getLogger(LOGGER);
    /** Cybernode logger. */
    static final Logger loaderLogger = Logger.getLogger(LOGGER+".loader");
    /**The componant name for accessing the service's configuration */
    static String configComponent = CONFIG_COMPONENT;
    /** Instance of a ServiceBeanContainer */
    ServiceBeanContainer container;
    /** If the Cybernode has been configured wih an availability Schedule, and
     * the Cybernode removes itself as an asset, this property determines whether
     * instantiated services should be terminated upon unregistration */
    boolean serviceTerminationOnUnregister=true;
    /** default maximum service limit */
    int serviceLimit=500;
    /** service consumer responsible for Provision Monitor discovery and
     * registration */
    ServiceConsumer svcConsumer=null;
    /** flag indicating the Cybernode is in shutdown sequence */
    boolean shutdownSequence=false;
    /** Collection of JSBs that are in the process of instantiation */
    final List<ServiceProvisionEvent> inProcess =
        Collections.synchronizedList(new ArrayList<ServiceProvisionEvent>());
    /** Log format version */
    static final int LOG_VERSION = 1;
    /** PersistentStore to save state */
    //PersistentStore store;
    /** LogHandler required when dealing with the ReliableLog */
    CybernodeLogHandler logHandler;
    /** ThreadPool for SLAThresholdEvent processing */
    Executor thresholdTaskPool;
    /** This flag indicates whether the Cybernode has been configured to install
     * external software  defined by ServiceBean instances */
    boolean provisionEnabled=true;
    /**
     * The directory to provision external software to. This is the root
     * directory where software may be installed by ServiceBean instances which
     * require external software to be resident on compute resource the
     * Cybernode represents
     */
    String provisionRoot;
    /** The ServiceStatementManager defines the semantics of reading/writing
     * ServiceStatement instances */
    ServiceStatementManager serviceStatementManager;
    /** The Timer to use for scheduling a ServiceRecord update task */
    private Timer taskTimer;
    /** The Configuration for the Service */
    private Configuration config;
    /** ProxyPreparer for the OperationalStringMonitor */
    ProxyPreparer operationalStringManagerPreparer;
    private LifeCycle lifeCycle;
    /** Flag to indicate if the Cybernode is enlisted */
    private boolean enlisted=false;
    /** The availability schedule for the Cybernode */
    private Schedule availabilitySchedule;
    /** A List of scheduled TimerTasks */
    List<TimerTask> schedulingTaskList =
        Collections.synchronizedList(new ArrayList<TimerTask>());
    private ServiceRecordUpdateTask serviceRecordUpdateTask;
    private int registryPort;

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
    public CybernodeImpl(String[] configArgs, LifeCycle lifeCycle)
    throws Exception {
        super();
        this.lifeCycle = lifeCycle;
        bootstrap(configArgs);
    }

    /**
     * Create a Cybernode that uses RMI Activation
     *
     * @param activationID The ActivationID
     * @param data Startup data as a MarshalledObject
     *
     * @throws Exception if bootstrapping fails
     */
    public CybernodeImpl(ActivationID activationID, MarshalledObject data)
    throws Exception {
        super();
        this.activationID = activationID;
        bootstrap((String[])data.get());
    }

    @Override
    public void destroy() {
        super.destroy();
    }

    /**
     * Override destroy to ensure that any JSBs are shutdown as well
     */
    public void destroy(boolean force) {
        if(shutdownSequence)
            return;
        synchronized(CybernodeImpl.class) {
            if(serviceRecordUpdateTask!=null)
                serviceRecordUpdateTask.cancel();
            shutdownSequence=true;
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
                logger.log(Level.SEVERE, "While destroying persistent store", e);
            }
        }
        logger.log(Level.INFO,
                   context.getServiceElement().getName()+
                   ": destroy() notification");
        /* Stop the timer */
        if(taskTimer!=null)
            taskTimer.cancel();
        unadvertise();
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
                    Throwable cause = ThrowableUtil.getRootCause(e);
                    logger.warning("Unregistering PlatformCapability " +
                                   "[" + pCap.getName() + "], Exception: " +
                                   cause.getClass().getName());
                }
            }
        }
        /* Terminate the DiscoveryManagementPool */
        DiscoveryManagementPool discoPool =
            DiscoveryManagementPool.getInstance();
        if(discoPool!=null)
            discoPool.terminate();

        /* Tell the utility that started the Cybernode we are going away */
        ServiceBeanManager serviceBeanManager  = context.getServiceBeanManager();
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
        if (logger.isLoggable(Level.FINEST))
            logger.entering(this.getClass().getName(), "getProxyVerifier");        
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
        List<ServiceRecord> list = new ArrayList<ServiceRecord>();
        ServiceStatement[] statements = serviceStatementManager.get();
        for (ServiceStatement statement : statements) {
            ServiceRecord[] records = statement.getServiceRecords(getUuid(),
                                                                  filter);
            list.addAll(Arrays.asList(records));
        }
        return(list.toArray(new ServiceRecord[list.size()]));
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
        if(logger.isLoggable(Level.FINEST))
            logger.finest("ServiceBeanInstance count for "+"["+
                          element.getName()+"] = "+sbi.length);
        return (sbi);
    }

    /**
     * @see org.rioproject.deploy.ServiceBeanInstantiator#update
     */
    public void update(ServiceElement[] sElements,
                       OperationalStringManager opStringMgr) throws RemoteException {
        opStringMgr = (OperationalStringManager)
                         operationalStringManagerPreparer.prepareProxy(opStringMgr);
        if(logger.isLoggable(Level.FINEST))
            logger.log(Level.FINEST,
                       "Prepared OperationalStringManager proxy: {0}",
                       opStringMgr);
        container.update(sElements, opStringMgr);
    }

    /**
     * @see org.rioproject.deploy.ServiceBeanInstantiator#getName()
     */
    public String getName() {
        return(context.getServiceElement().getName());
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
        ServiceStatement statement =
            serviceStatementManager.get(serviceRecord.getServiceElement());
        if(statement==null) {
            /* ServiceStatement not found, create one */
            statement = new ServiceStatement(serviceRecord.getServiceElement());
        }
        statement.putServiceRecord(getUuid(), serviceRecord);
        serviceStatementManager.record(statement);
        //setChanged(StatusType.NORMAL);
        if(loaderLogger.isLoggable(Level.FINE)) {
            int instantiatedServiceCount = getInstantiatedServiceCount();
            logger.fine("Instantiated " +
                        "["+serviceRecord.getServiceElement().getName()+"], " +
                        "instance: "+serviceRecord.getServiceElement().getServiceBeanConfig().getInstanceID()+", " +
                        "numActive: "+instantiatedServiceCount);
        }
    }

    /**
     * Notification that a ServiceBean has been discarded
     *
     * @param serviceRecord The ServiceRecord
     */
    public void serviceDiscarded(ServiceRecord serviceRecord) {
        if(serviceRecord==null)
            logger.log(Level.WARNING,
                       "ServiceRecord is null when discarding ServiceBean");

        else {
            ServiceStatement statement =
                serviceStatementManager.get(serviceRecord.getServiceElement());
            if(statement!=null) {
                statement.putServiceRecord(getUuid(), serviceRecord);
                serviceStatementManager.record(statement);
                int instantiatedServiceCount = getInstantiatedServiceCount();
                if(loaderLogger.isLoggable(Level.FINE))
                    loaderLogger.fine("Discarded " +
                                      "["+serviceRecord.getServiceElement().getName()+"], " +
                                      "instance: "+serviceRecord.getServiceElement().getServiceBeanConfig().getInstanceID()+", " +
                                      "numActive: "+instantiatedServiceCount);
            } else {
                loaderLogger.log(Level.WARNING,
                                 "ServiceStatement is null when discarding ServiceBean"+
                                 "["+serviceRecord.getServiceElement().getName()+"]");
            }
            //setChanged(StatusType.NORMAL);
        }
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
    protected void bootstrap(String[] configArgs) throws Exception {
        try {
            context =
                ServiceBeanActivation.getServiceBeanContext(
                                                  getConfigComponent(),
                                                  "Cybernode",
                                                  configArgs,
                                                  getClass().getClassLoader());
        } catch(Exception e) {
            logger.log(Level.SEVERE, "Getting ServiceElement", e);
            throw e;
        }

        BannerProvider bannerProvider =
            (BannerProvider)context.getConfiguration().getEntry(getConfigComponent(),
                                                                "bannerProvider",
                                                                BannerProvider.class,
                                                                new BannerProviderImpl());
        logger.info(bannerProvider.getBanner(context.getServiceElement().getName()));
        try {
            try {
                start(context);
            } catch (Throwable t) {
                t.printStackTrace();
            }
            LifeCycleManager lMgr =
                (LifeCycleManager)context.
                    getServiceBeanManager().getDiscardManager();
            if(lMgr!=null) {
                Object proxy = getServiceProxy();
                if(proxy!=null) {
                    lMgr.register(proxy, context);
                } else {
                    logger.severe("Cybernode proxy is null, unable to " +
                                  "register to LifeCycleManager");
                }
            } else {
                logger.warning("LifeCycleManager is null, unable to register");
            }
        } catch(Exception e) {
            logger.log(Level.SEVERE, "Register to LifeCycleManager", e);
            throw e;
        }
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
            logger.severe("Could not get the exported proxy for the Cybernode, " +
                          "returning null. The Cybernode will not be able to " +
                          "accept remote inbound communications");
            return null;
        }
        Object proxy = CybernodeProxy.getInstance(cybernode, getUuid());
        /* Get the registry port */
        String sPort = System.getProperty(Constants.REGISTRY_PORT, "0");
        registryPort = Integer.parseInt(sPort);
        String name = context.getServiceBeanConfig().getName();

        if(registryPort!=0) {
            try {
                String address = HostUtil.getHostAddressFromProperty(Constants.RMI_HOST_ADDRESS);
                Registry registry = LocateRegistry.getRegistry(address,
                                                               registryPort);
                try {
                    registry.bind(name, (Remote)proxy);
                    if(logger.isLoggable(Level.FINER))
                        logger.finer("Bound to RMI Registry on port="+
                                     registryPort);
                } catch(AlreadyBoundException e) {
                    /*ignore */
                }
            } catch(AccessException e) {
                logger.log(Level.WARNING,
                           "Binding "+name+" to RMI Registry", e);
            } catch(RemoteException e) {
                logger.log(Level.WARNING,
                           "Binding "+name+" to RMI Registry", e);
            } catch (java.net.UnknownHostException e) {
                logger.log(Level.WARNING,
                           "Unknown host address locating RMI Registry", e);
            }
        } else {
            if(logger.isLoggable(Level.FINER))
                logger.finer("RMI Registry property not set, " +
                             "unable to bind "+name);
        }
        /*
         * Set the MarshalledInstance into the ServiceBeanManager
         */
        try {
            MarshalledInstance mi = new MarshalledInstance(proxy);
            ((JSBManager)context.getServiceBeanManager()).setMarshalledInstance(mi);
        } catch (IOException e) {
            logger.log(Level.WARNING,
                       "Unable to create MarshalledInstance for Cybernode " +
                       "proxy, non-fatal error, continuing ...",
                       e);
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
                    admin =
                        new CybernodeAdminImpl(this,
                                               adminExporter,
                                               contextMgr.
                                                  getContextAttributeLogHandler());
                else
                    admin = new CybernodeAdminImpl(this, adminExporter);
            }
            admin.setServiceBeanContext(getServiceBeanContext());
            ((CybernodeAdminImpl)admin).setRegistryPort(registryPort);
            adminProxy = admin.getServiceAdmin();
        } catch (Throwable t) {
            logger.log(Level.SEVERE, "Getting CybernodeAdminImpl", t);
        }
        return (adminProxy);
    }

    /**
     * @see org.rioproject.core.jsb.ServiceBean#initialize
     */
    public void initialize(ServiceBeanContext context) throws Exception {
        /*
         * Determine if a log directory has been provided. If so, create a
         * PersistentStore
         */
        String logDirName =
            (String)context.getConfiguration().getEntry(CONFIG_COMPONENT,
                                                        "logDirectory",
                                                        String.class,
                                                        null);
        if(logDirName!=null) {
            logHandler = new CybernodeLogHandler();
            store = new PersistentStore(logDirName, logHandler, logHandler);
            if(logger.isLoggable(Level.FINE))
                logger.log(Level.FINE,
                           "Cybernode: using absolute logdir path "+
                           "["+store.getStoreLocation()+"]");
            store.snapshot();
            super.initialize(context, store);
        } else {
            super.initialize(context);
        }
        
        /* Get the Configuration */
        config = context.getConfiguration();
        try {
            Exporter defaultExporter =
                (Exporter)config.getEntry(RIO_CONFIG_COMPONENT,
                                          DEFAULT_EXPORTER,
                                          Exporter.class);
            if(logger.isLoggable(Level.FINEST))
                logger.log(Level.FINEST,
                           "{0} has been set as the defaultExporter",
                           new Object[] {defaultExporter});
        } catch(Exception e) {
            logger.log(Level.SEVERE,
                       "The "+RIO_CONFIG_COMPONENT+"."+DEFAULT_EXPORTER+" "+
                       "attribute must be set");
            throw e;
        }

        /* Set up thread deadlock detection. This seems a little clumsy to use
         * the WatchInjector and could probaby be improved. This does make the
         * most use of the wiring for forked service monitoring. */
        long threadDeadlockCheck = (Long)config.getEntry(CONFIG_COMPONENT,
                                                         "threadDeadlockCheck",
                                                         long.class,
                                                         (long)5000);
        if(threadDeadlockCheck>=1000) {
            WatchDescriptor threadDeadlockDescriptor =
                ThreadDeadlockMonitor.getWatchDescriptor();
            threadDeadlockDescriptor.setPeriod(threadDeadlockCheck);
            Method getThreadDeadlockCalculable =
                ThreadDeadlockMonitor.class.getMethod("getThreadDeadlockCalculable");
            ThreadDeadlockMonitor threadDeadlockMonitor = new ThreadDeadlockMonitor();
            WatchInjector watchInjector = new WatchInjector(this, context);
            ThresholdWatch watch =
                (ThresholdWatch)watchInjector.inject(threadDeadlockDescriptor,
                                                     threadDeadlockMonitor,
                                                     getThreadDeadlockCalculable);
            watch.setThresholdValues(new ThresholdValues(0, 1));
            watch.addThresholdListener(new DeadlockedThreadPolicyHandler());
        } else {
            logger.info("Thread deadlock monitoring has been disabled. The " +
                        "configured thread deadlock check time was " +
                        "["+threadDeadlockCheck+"]. To enable " +
                        "thread deadlock monitoring, the thread deadlock check " +
                        "time must be >= 1000 milliseconds.");
        }
        try {
            serviceLimit = Config.getIntEntry(config,
                                              getConfigComponent(),
                                              "serviceLimit",
                                              500,
                                              0,
                                              Integer.MAX_VALUE);
        } catch(Exception e) {
            logger.log(Level.WARNING,
                       "Exception getting serviceLimit, default to 500");
        }

        /* Get the ProxyPreparer for passed in OperationalStringManager
         * instances */
        operationalStringManagerPreparer =
            (ProxyPreparer)config.getEntry(getConfigComponent(),
                                           "operationalStringManagerPreparer",
                                           ProxyPreparer.class,
                                           new BasicProxyPreparer());
        /* Check for JMXConnection */
        addAttributes(JMXUtil.getJMXConnectionEntries(config));

        /* Add service UIs programmatically */
        addAttributes(getServiceUIs());

        /* Get the security policy to apply to loading services */
        String serviceSecurityPolicy =
            (String)config.getEntry(getConfigComponent(),
                                    "serviceSecurityPolicy",
                                    String.class,
                                    null);
        if(serviceSecurityPolicy!=null)
            System.setProperty("rio.service.security.policy",
                               serviceSecurityPolicy);

        /* Establish default operating environment */
        Environment.setupDefaultEnvironment();

        /* Setup persistent provisioning attributes */
        provisionEnabled =
            (Boolean) config.getEntry(getConfigComponent(),
                                      "provisionEnabled",
                                      Boolean.class,
                                      true);
        provisionRoot = Environment.setupProvisionRoot(provisionEnabled, config);
        if(provisionEnabled) {
            if(logger.isLoggable(Level.FINE))
                logger.log(Level.FINE,
                           "Software provisioning has been enabled, "+
                           "default provision root location is ["+provisionRoot+"]");
        }
        computeResource.setPersistentProvisioningRoot(provisionRoot);

        /* Setup the native library directory */
        String nativeLibDirectories =
            Environment.setupNativeLibraryDirectories(config);

        /* Ensure org.rioproject.system.native property is set. This will be used
         * by the org.rioproject.system.SystemCapabilities class */
        if(nativeLibDirectories!=null)
            System.setProperty(SystemCapabilities.NATIVE_LIBS,
                               nativeLibDirectories);

        /* Initialize the ComputeResource */
        initializeComputeResource(computeResource);

        if(logger.isLoggable(Level.FINER)) {
            StringBuilder sb = new StringBuilder();
            sb.append("Service Limit : ").append(serviceLimit).append("\n");
            sb.append("System Capabilities\n");
            MeasurableCapability[] mCaps =
                computeResource.getMeasurableCapabilities();
            sb.append("MeasurableCapabilities : (").
               append(mCaps.length).
               append(")\n");
            for (MeasurableCapability mCap : mCaps) {
                sb.append("\t")
                    .append(mCap.getId())
                    //append(mCap.getClass().getName()).
                    .append("\n");
                ThresholdValues tValues = mCap.getThresholdValues();
                sb.append("\t\tlow threshold  : ")
                    .append(tValues.getLowThreshold())
                    .append("\n");
                sb.append("\t\thigh threshold : ")
                    .append(tValues.getHighThreshold())
                    .append("\n");
                sb.append("\t\treport rate    : ")
                    .append(mCap.getPeriod())
                    .append("\n");
                sb.append("\t\tsample size    : ")
                    .append(mCap.getSampleSize())
                    .append("\n");
            }
            PlatformCapability[] pCaps = computeResource.getPlatformCapabilities();
            sb.append("PlatformCapabilities : (")
               .append(pCaps.length)
               .append(")\n");

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
                sb.append("\t")
                    .append(stripPackageName(pCap.getClass().getName()))
                    .append("\n");
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
                    sb.append("\t\t")
                        .append(key)
                        .append(" : ")
                        .append(value)
                        .append("\n");
                }
                String path = pCap.getPath();
                if (path != null)
                    sb.append("\t\tPath : ")
                        .append(path).append("\n");
            }
            logger.finer(sb.toString());
        }

        /* Create the ServiceStatementManager */
        serviceStatementManager = Environment.getServiceStatementManager(config);

        /* Start the timerTask for updating ServiceRecords */
        long serviceRecordUpdateTaskTimer = 60;
        try {
            serviceRecordUpdateTaskTimer =
                Config.getLongEntry(config, getConfigComponent(),
                                    "serviceRecordUpdateTaskTimer",
                                    serviceRecordUpdateTaskTimer,
                                    1,
                                    Long.MAX_VALUE);
        } catch(Throwable t) {
            logger.log(Level.WARNING,
                       "Exception getting slaThresholdTaskPoolMinimum",
                       t);
        }
        taskTimer = new Timer(true);
        long period = 1000*serviceRecordUpdateTaskTimer;
        long now = System.currentTimeMillis();
        serviceRecordUpdateTask = new ServiceRecordUpdateTask();
        taskTimer.scheduleAtFixedRate(serviceRecordUpdateTask,
                                      new Date(now+period), period);
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
            new ServiceConsumer(new CybernodeAdapter((ServiceBeanInstantiator)getServiceProxy(),
                                                     this,
                                                     computeResource),
                                serviceLimit,
                                config);

        /* Get the property that determines whether instantiated services
         * should be terminated upon unregistration */
        serviceTerminationOnUnregister =
        (Boolean) config.getEntry(getConfigComponent(),
                                  "serviceTerminationOnUnregister",
                                  Boolean.class,
                                  Boolean.TRUE);
        if(logger.isLoggable(Level.FINE))
            logger.fine("serviceTerminationOnUnregister="+
                        serviceTerminationOnUnregister);

        /* Get the schedule which will control when the Cybernode will make
         * itself available as an asset */
        availabilitySchedule = (Schedule)config.getEntry(getConfigComponent(),
                                                         "availablitySchedule",
                                                         Schedule.class,
                                                         new Schedule());
        doEnlist(availabilitySchedule);

        /* Create a computeResourcePolicyHandler to watch for thresholds
         * being crossed */
        new ComputeResourcePolicyHandler(computeResource,
                                         getSLAEventHandler());

        /* Ensure we have a serviceID */
        if(serviceID==null) {
            if(logger.isLoggable(Level.FINE))
                logger.fine("Creating new ServiceID from UUID="+
                            getUuid().toString());
            serviceID = new ServiceID(getUuid().getMostSignificantBits(),
                                      getUuid().getLeastSignificantBits());
        }

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
            logger.log(Level.WARNING,
                       "Exception getting initialServiceLoadDelay",
                       t);
        }
        if(logger.isLoggable(Level.CONFIG))
            logger.config("initialServiceLoadDelay="+initialServiceLoadDelay);

        /*
        * Schedule the task to Load any configured services
        */
        if(initialServiceLoadDelay>0) {
            long now = System.currentTimeMillis();
            getTaskTimer().schedule(
                                    new InitialServicesLoadTask(config),
                                    new Date(now+initialServiceLoadDelay));
        } else {
            InitialServicesLoadTask serviceLoader =
                new InitialServicesLoadTask(config);
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
                initialServices =
                    (String[])config.getEntry(getConfigComponent(),
                                              "initialServices",
                                              String[].class,
                                              initialServices);
            } catch(ConfigurationException e) {
                logger.log(Level.WARNING, "Getting initialServices", e);
            }

            if(logger.isLoggable(Level.CONFIG))
                logger.config("Loading ["+initialServices.length+"] " +
                              "initialServices");
            for (String initialService : initialServices) {
                URL deploymentURL;
                try {
                    if (initialService.startsWith("http:"))
                        deploymentURL = new URL(initialService);
                    else
                        deploymentURL =
                            new File(initialService).toURI().toURL();
                    Map errorMap = load(deploymentURL);
                    dumpOpStringError(errorMap);
                } catch (Throwable t) {
                    logger.log(Level.SEVERE,
                               "Loading initial services : " +
                               initialService,
                               t);
                }
            }
        }

        /*
         * Load and activate services
         */
        Map load(URL deploymentURL) {
            if(deploymentURL == null)
                throw new IllegalArgumentException("Deployment URL cannot be null");
            HashMap map = new HashMap();
            try {
                /* Get the OperationalString loader */
                OpStringLoader opStringLoader =
                    new OpStringLoader(this.getClass().getClassLoader());
                OperationalString[] opStrings =
                    opStringLoader.parseOperationalString(deploymentURL);
                if(opStrings != null) {
                    for (OperationalString opString : opStrings) {
                        if (logger.isLoggable(Level.FINE))
                            logger.log(Level.FINE,
                                       "Activating Deployment " +
                                       "[" + opString.getName() + "]");
                        ServiceElement[] services = opString.getServices();
                        for (ServiceElement service : services) {
                            activate(service);
                        }
                    }
                }
            } catch(Throwable t) {
                logger.log(Level.WARNING,
                           "Activating OperationalString",
                           t);
            }
            return (map);
        }

        /**
         * Dump an error map associated with the loading and/or addition of an
         * OperationalString
         *
         * @param errorMap The map containing exceptions
         */
        void dumpOpStringError(Map errorMap) {
            if(!errorMap.isEmpty()) {
                Set keys = errorMap.keySet();
                StringBuilder sb = new StringBuilder();
                sb.append("+========================+\n");
                //int i = 0;
                for (Object comp : keys) {
                    sb.append("Component: ").append(comp);
                    Object o = errorMap.get(comp);
                    if (o instanceof Throwable) {
                        StackTraceElement[] stes =
                            ((Throwable) o).getStackTrace();
                        for (StackTraceElement ste : stes) {
                            sb.append("\tat ").append(ste).append("\n");
                        }
                    } else {
                        sb.append(" ").append(o.toString());
                    }
                }
                sb.append("\n+========================+");
                if(logger.isLoggable(Level.FINE))
                    logger.log(Level.FINE, sb.toString());
            }
        }
    }

    /*
     * Create a ServiceProvisionEvent and instantiate the ServiceElement
     */
    private void activate(ServiceElement sElem) {
        ServiceProvisionEvent spe =
            new ServiceProvisionEvent(getServiceProxy(), null, sElem);
        try {
            instantiate(spe);
        } catch (Throwable t) {
            logger.log(Level.WARNING,
                       "Activating service ["+sElem.getName()+"]",
                       t);
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
     * Get the availability schedule
     *
     * @return The Schedule for availability
     */
    protected Schedule getAvailabilitySchedule() {
        Schedule sched;
        synchronized(this) {
            sched = availabilitySchedule;

        }
        return(sched);
    }

    /**
     * Set the availability schedule
     *
     * @param sched The Schedule for availability
     */
    protected void setAvailabilitySchedule(Schedule sched) {
        synchronized(this) {
            availabilitySchedule = sched;
        }
    }

    /**
     * Get all in process TimerTask instances that are specific to availability
     * scheduling for registration to monitor instances
     *
     * @return Array of in process TimerTask instances. If there are no
     * TimerTask instances return a zero-length array
     */
    protected TimerTask[] getRegistrationTasks() {
        return(schedulingTaskList.toArray(
            new TimerTask[schedulingTaskList.size()]));
    }

    /**
     * Add a TimerTask to the Collection of TimerTasks
     *
     * @param task The TimerTask
     */
    protected void addRegistrationTask(TimerTask task) {
        if(task!=null) {
            if(task instanceof RegistrationTask ||
               task instanceof UnRegistrationTask)
            schedulingTaskList.add(task);
        }
    }

    /**
     * Remove a TimerTask from Collection of scheduled TimerTask
     * instances
     *
     * @param task The TimerTask to remove
     */
    protected void removeRegistrationTask(TimerTask task) {
        if(task!=null)
            schedulingTaskList.remove(task);
    }

    /**
     * Have the Cybernode add itself as a resource which can be
     * used to instantiate dynamic application services. Once
     * a Cybernode is enlisted, it will use the provided
     * availability schedule to control when it registers and unregisters to
     * discovered Provision Manager instances
     *
     * If the Cybernode is already enlisted, this method will have
     * no effect
     *
     * @param schedule The Schedule to use for enlistment
     */
    protected void doEnlist(Schedule schedule) {
        if(isEnlisted()) {
            if(logger.isLoggable(Level.FINE))
                logger.fine("Already enlisted");
            return;
        }
        if(schedule==null)
            throw new IllegalArgumentException("schedule is null");
        Date startDate = schedule.getStartDate();
        long now = System.currentTimeMillis();
        long delay = startDate.getTime()-now;
        boolean scheduled = false;
        if(logger.isLoggable(Level.FINE))
            logger.fine("Availability schedule "+
                        "Start Date=["+startDate.toString()+"], "+
                        "Available for="+
                        "["+
                        TimeUtil.format(schedule.getDuration())+
                        "], "+
                        "Time not available="+
                        "["+
                        TimeUtil.format(schedule.getRepeatInterval())+
                        "], "+
                        "Time till start ["+TimeUtil.format(delay)+"]");
        if(delay>0 || schedule.getDuration()>0) {
            RegistrationTask registrationTask = new RegistrationTask(schedule);
            addRegistrationTask(registrationTask);
            if(schedule.getDuration()>0)
                taskTimer.scheduleAtFixedRate(registrationTask,
                                              startDate,
                                              schedule.getDuration() +
                                              schedule.getRepeatInterval());
            else
                taskTimer.schedule(registrationTask, startDate);
            scheduled=true;
        }

        if(!scheduled) {
            if(logger.isLoggable(Level.INFO))
                logger.info("Configured for constant availability");
            try {
                svcConsumer.initializeProvisionDiscovery(
                                              context.getDiscoveryManagement());
            } catch(Exception e) {
                logger.log(Level.WARNING,
                           "Initializing Provision discovery",
                           e);
            }
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
        URL implUrl =
            getClass().getProtectionDomain().getCodeSource().getLocation();
        RioManifest rioManifest;
        String build = null;
        try {
            rioManifest = new RioManifest(implUrl);
            build = rioManifest.getRioBuild();
        } catch(IOException e) {
            logger.log(Level.WARNING, "Getting Rio Manifest", e);
        }
        if(build==null)
            build="0";
        return(new ServiceInfo(context.getServiceElement().getName(),
                               "",
                               "",
                               "v"+ RioVersion.VERSION+" Build "+build,
                               "",""));
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

        loaderLogger.log(Level.INFO,
                         "Instantiating "+
                         event.getServiceElement().getOperationalStringName()+"/"+
                         event.getServiceElement().getName());
        DeployedService deployedService;
        try {
            if(shutdownSequence) {
                throw new ServiceBeanInstantiationException("["+
                    context.getServiceElement().getName()+"] shutting down," +
                    "unavailable for service instantiation");
            }

            if(event.getID()!=ServiceProvisionEvent.ID)
                throw new UnknownEventException("Unknown event type "+
                                                "["+event.getID()+"]");

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
                if(loaderLogger.isLoggable(Level.FINEST))
                    loaderLogger.finest("["+event.getServiceElement().getName()+"] "+
                                        "activeServiceCounter=["+activeServiceCounter+"], "+
                                        "inProcessServiceCount=["+inProcessServiceCount+"], " +
                                        "instantiatedServiceCount=["+instantiatedServiceCount+"]");
                /* First check max per machine */
                int maxPerMachine = event.getServiceElement().getMaxPerMachine();
                if(maxPerMachine!=-1 && activeServiceCounter >= maxPerMachine) {
                    if(loaderLogger.isLoggable(Level.FINEST))
                        loaderLogger.finest("Abort allocation of ["+
                                            event.getServiceElement().getName()+"] "+
                                            "activeServiceCounter=["+activeServiceCounter+"] "+
                                            "inProcessServiceCount=["+inProcessServiceCount+"] "+
                                            "maxPerMachine=["+maxPerMachine+"]");
                    throw new ServiceBeanInstantiationException("MaxPerMachine "+
                                                        "["+maxPerMachine+"] " +
                                                        "has been "+
                                                        "reached");
                }

                /* The check planned service count */
                int numPlannedServices = event.getServiceElement().getPlanned();
                if(activeServiceCounter >= numPlannedServices) {
                    if(loaderLogger.isLoggable(Level.FINEST))
                        loaderLogger.finest("Cancel allocation of ["+
                                            event.getServiceElement().getName()+"] "+
                                            "activeServiceCounter=["+activeServiceCounter+"] "+
                                            "numPlannedServices=["+numPlannedServices+"]");
                    return(null);
                }

                inProcess.add(event);
            }

            int inProcessCount;
            synchronized(inProcess) {
                inProcessCount = inProcess.size() -
                                 container.getActivationInProcessCount();
            }
            int containerServiceCount = container.getServiceCounter();
            if((inProcessCount+containerServiceCount) <= serviceLimit) {
                OperationalStringManager
                    opMgr = event.getOperationalStringManager();
                if(!event.getServiceElement().forkService()) {
                    try {
                        opMgr = OpStringManagerProxy.getProxy(
                            event.getServiceElement().getOperationalStringName(),
                            event.getOperationalStringManager(),
                            context.getDiscoveryManagement());
                        
                    } catch (Exception e) {
                        loaderLogger.log(Level.WARNING,
                                         "Unable to create proxy for " +
                                         "OperationalStringManager, " +
                                         "using provided OperationalStringManager",
                                         ThrowableUtil.getRootCause(e));
                        opMgr = event.getOperationalStringManager();
                    }
                }
                try {
                    ServiceBeanInstance
                        jsbInstance = container.activate(event.getServiceElement(),
                                                     opMgr,
                                                     getSLAEventHandler());
                    ServiceBeanDelegate delegate =
                        container.getServiceBeanDelegate(jsbInstance.getServiceBeanID());
                    ComputeResourceUtilization cru = delegate.getComputeResourceUtilization();
                    deployedService = new DeployedService(event.getServiceElement(),
                                                          jsbInstance,
                                                          cru);
                } catch(ServiceBeanInstantiationException e) {
                    if(opMgr instanceof OpStringManagerProxy.OpStringManager)
                        ((OpStringManagerProxy.OpStringManager)opMgr).terminate();
                    throw e;
                }

                return(deployedService);
            } else {
                throw new ServiceBeanInstantiationException("Service Limit of "+
                                                    "["+serviceLimit+"] has been "+
                                                    "reached");
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
        ServiceBeanContainer defaultContainer = new JSBContainer(config, ((JSBContext)context).getConfigurationFiles());

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
        joiner.modifyAttributes(new Entry[]{new BasicStatus()},
                                new Entry[]{new BasicStatus(statusType)});
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
        PlatformCapability[] pCaps =
            computeResource.getPlatformCapabilities();
        MBeanServer mbeanServer = MBeanServerFactory.getMBeanServer();
        for (PlatformCapability pCap : pCaps) {
            try {
                ObjectName objectName = getObjectName(pCap);
                if (objectName != null)
                    mbeanServer.registerMBean(pCap, objectName);
            } catch (MalformedObjectNameException e) {
                logger.log(Level.WARNING,
                           "PlatformCapability " +
                           "[" + pCap.getClass().getName() + "." +
                           pCap.getName() + "]:" + e.toString(),
                           e);
            } catch (NotCompliantMBeanException e) {
                logger.log(Level.WARNING,
                           "PlatformCapability " +
                           "[" + pCap.getClass().getName() + "." +
                           pCap.getName() + "]:" + e.toString(),
                           e);
            } catch (MBeanRegistrationException e) {
                logger.log(Level.WARNING,
                           "PlatformCapability " +
                           "[" + pCap.getClass().getName() + "." +
                           pCap.getName() + "]:" + e.toString(),
                           e);
            } catch (InstanceAlreadyExistsException e) {
                logger.log(Level.WARNING,
                           "PlatformCapability " +
                           "[" + pCap.getClass().getName() + "." +
                           pCap.getName() + "]:" + e.toString(),
                           e);
            }
        }
    }

    /*
     * Get an ObjectName for a PlatformCapability
     */
    private ObjectName getObjectName(PlatformCapability pCap)
        throws MalformedObjectNameException {
        return(JMXUtil.getObjectName(context,
                                     "org.rioproject.cybernode",
                                     "PlatformCapability",
                                     pCap.getName()));
    }

    /*
     * Make a ServiceBeanInstance for the Cybernode
     */
    private ServiceBeanInstance makeServiceBeanInstance() throws IOException {
        return new ServiceBeanInstance(getUuid(),
                                       new MarshalledInstance(getServiceProxy()),
                                       context.getServiceElement().
                                           getServiceBeanConfig(),
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
                ServiceStatement statement =
                    serviceStatementManager.get(record.getServiceElement());
                if (statement != null) {
                    statement.putServiceRecord(getUuid(), record);
                    serviceStatementManager.record(statement);
                }
            }
        }
    }

    /**
     * Scheduled Task which will control Cybernode registration to ProvisionMonitor
     * instances
     */
    class RegistrationTask extends TimerTask {
        int repeats;
        Schedule schedule;

        RegistrationTask(Schedule schedule) {
            this.schedule = schedule;
        }

        /**
         * The action to be performed by this timer task.
         */
        public void run() {
            if(logger.isLoggable(Level.FINE))
                logger.fine("Register to Provision Monitor instances");

            try {
                svcConsumer.initializeProvisionDiscovery(
                                                 context.getDiscoveryManagement());
            } catch(Exception e) {
                logger.log(Level.WARNING,
                           "Initializing ServiceConsumer",
                           e);
            }

            repeats++;
            if(schedule.getRepeatCount()!= Schedule.INDEFINITE &&
               repeats > schedule.getRepeatCount()) {

                if(logger.isLoggable(Level.FINE))
                    logger.fine("Repeat count ["+schedule.getRepeatCount()+"] "+
                                "reached, cancel RegistrationTask");
                cancel();
                UnRegistrationTask unRegisterTask =
                    new UnRegistrationTask(true);
                addRegistrationTask(unRegisterTask);
                long sheduledUnRegistration =
                    schedule.getDuration()+System.currentTimeMillis();
                taskTimer.schedule(unRegisterTask,
                                   new Date(sheduledUnRegistration));
            } else {
                if(schedule.getDuration()!=Schedule.INDEFINITE) {
                    UnRegistrationTask unRegisterTask = new UnRegistrationTask();
                    addRegistrationTask(unRegisterTask);
                    long sheduledUnRegistration =
                        schedule.getDuration()+System.currentTimeMillis();
                    taskTimer.schedule(unRegisterTask,
                                       new Date(sheduledUnRegistration));
                }
            }
        }

        public boolean cancel() {
            removeRegistrationTask(this);
            return(super.cancel());
        }
    }

    /**
     * Scheduled Task which will disengage from all registered provisioners
     */
    class UnRegistrationTask extends TimerTask {
        boolean isLast = false;

        UnRegistrationTask() {
            this(false);
        }

        UnRegistrationTask(boolean isLast) {
            this.isLast = isLast;
            if(logger.isLoggable(Level.FINEST))
                logger.finest("Create new UnRegistrationTask");
        }

        /**
         * The action to be performed by this timer task.
         */
        public void run() {
            doRelease();
            if(isLast)
                setEnlisted(false);
            cancel();
        }

        public boolean cancel() {
            removeRegistrationTask(this);
            return(super.cancel());
        }
    }

    private

    /**
     * If deadlocked threads are detected fire SLAThresholdEvents
     */
    class DeadlockedThreadPolicyHandler implements ThresholdListener {

        public String getID() {
            return "Thread Deadlock Detector";
        }

        public void setThresholdManager(ThresholdManager thresholdManager) {
            // implemented for interface compatibility
        }

        public void notify(Calculable calculable,
                           ThresholdValues thresholdValues,
                           int type) {
            double[] range = new double[]{
                thresholdValues.getCurrentLowThreshold(),
                thresholdValues.getCurrentHighThreshold()
            };

            SLA sla = new SLA(calculable.getId(), range);
            try {
                ServiceBeanInstance instance = makeServiceBeanInstance();
                SLAThresholdEvent event =
                    new SLAThresholdEvent(proxy,
                                          context.getServiceElement(),
                                          instance,
                                          calculable,
                                          sla,
                                          "Cybernode Thread Deadlock " +
                                          "Policy Handler",
                                          instance.getHostAddress(),
                                          type);
                thresholdTaskPool.execute(new SLAThresholdEventTask(event,
                                                                    getSLAEventHandler()));
            } catch(Exception e) {
                logger.log(Level.WARNING,
                           "Could not send a SLA Threshold Notification as a " +
                           "result of compute resource threshold " +
                           "["+calculable.getId()+"] being crossed",
                           e);
            }
        }
    }

    /**
     * If thresholds get crossed for memory related MeasurableCapability components, this class
     * will either request immediate garbage collection or (in the case of perm gen)
     * release as a provisionable resource:
     */
    class ComputeResourcePolicyHandler implements ThresholdListener {
        ComputeResource computeResource;
        EventHandler thresholdEventHandler;

        ComputeResourcePolicyHandler(ComputeResource computeResource,
                                     EventHandler thresholdEventHandler) {

            this.computeResource = computeResource;
            this.thresholdEventHandler = thresholdEventHandler;
            computeResource.addThresholdListener(this);
        }

        /**
         * @see org.rioproject.watch.ThresholdListener#getID
         */
        public String getID() {
            return("org.rioproject.cybernode.ComputeResource");
        }

        /**
         * @see org.rioproject.watch.ThresholdListener#setThresholdManager
         */
        public void setThresholdManager(ThresholdManager manager) {
            // implemented for interface compatibility
        }

        public void notify(Calculable calculable,
                           ThresholdValues thresholdValues,
                           int type) {
            if(shutdownSequence)
                return;
            String status =
                (type == ThresholdEvent.BREACHED?"breached":"cleared");
            if(logger.isLoggable(Level.FINE))
                logger.log(Level.FINE,
                           "Threshold={0}, Status={1}, Value={2}, " +
                           "Low={3}, High={4}",
                           new Object[]{calculable.getId(),
                                        status,
                                        calculable.getValue(),
                                        thresholdValues.getLowThreshold(),
                                        thresholdValues.getHighThreshold()});

            switch(type) {
                case ThresholdEvent.BREACHED:
                    double tValue = calculable.getValue();
                    if(tValue>thresholdValues.getCurrentHighThreshold()) {
                        svcConsumer.updateMonitors();
                        if(calculable.getId().equals("Memory")) {
                            logger.info("Memory utilization is "+calculable.getValue()+", " +
                                        "threshold set at "+
                                        thresholdValues.getCurrentHighThreshold()+", " +
                                        "request immediate garbage " +
                                        "collection");
                            System.gc();
                        }
                        if(calculable.getId().indexOf("Perm Gen")!=-1) {
                            logger.info("Perm Gen has breached with utilization > "+
                                            thresholdValues.getCurrentHighThreshold());
                            //if(isEnlisted())
                            //release(false);
                            //svcConsumer.cancelRegistrations();
                        }
                        
                    }
                    break;

                case ThresholdEvent.CLEARED:
                    //setChanged(StatusType.WARNING);
                    svcConsumer.updateMonitors();
                    break;
            }

            try {
                double[] range = new double[]{
                    thresholdValues.getCurrentLowThreshold(),
                    thresholdValues.getCurrentHighThreshold()
                };

                SLA sla = new SLA(calculable.getId(), range);
                ServiceBeanInstance instance = makeServiceBeanInstance();
                SLAThresholdEvent event =
                         new SLAThresholdEvent(proxy,
                                               context.getServiceElement(),
                                               instance,
                                               calculable,
                                               sla,
                                               "Cybernode Resource " +
                                               "Policy Handler",
                                               instance.getHostAddress(),
                                               type);
                thresholdTaskPool.execute(
                    new SLAThresholdEventTask(event, thresholdEventHandler));
            } catch(Exception e) {
                logger.log(Level.WARNING,
                           "Could not send a SLA Threshold Notification as a " +
                           "result of compute resource threshold " +
                           "["+calculable.getId()+"] being crossed",
                           e);
            }
        }
    }

    /**
     * This class is used as by a PoolableThread to notify registered
     * event consumers of a SLAThresholdEvent
     */
    static class SLAThresholdEventTask implements Runnable {
        SLAThresholdEvent event;
        EventHandler thresholdEventHandler;

        SLAThresholdEventTask(SLAThresholdEvent event,
                              EventHandler thresholdEventHandler) {
            this.event = event;
            this.thresholdEventHandler = thresholdEventHandler;
        }

        public void run() {
            try {
                thresholdEventHandler.fire(event);
            } catch(Exception e) {
                logger.log(Level.SEVERE, "Fire SLAThresholdEvent", e);
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
     * @see org.rioproject.cybernode.Cybernode#getSchedule()
     */
    public Schedule getSchedule() {
        return(getAvailabilitySchedule());
    }

    /*
     * @see org.rioproject.cybernode.Cybernode#enlist(org.rioproject.opstring.Schedule)
     */
    public void enlist(Schedule schedule) {
        doEnlist(schedule);
    }

    /*
     * @see org.rioproject.cybernode.Cybernode#release(boolean)
     */
    public void release(boolean terminateServices) {
        TimerTask[] registrationTasks = getRegistrationTasks();
        for (TimerTask registrationTask : registrationTasks)
            registrationTask.cancel();
        doRelease();
        setEnlisted(false);
    }

    private void doRelease() {
        if(logger.isLoggable(Level.INFO))
                logger.info("Unregister from ProvisionMonitor instances ");
            svcConsumer.destroy();
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
    public void setPersistentProvisioning(boolean provisionEnabled)
    throws IOException {
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
