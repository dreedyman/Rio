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
package org.rioproject.monitor;

import com.sun.jini.config.Config;
import com.sun.jini.proxy.BasicProxyTrustVerifier;
import com.sun.jini.reliableLog.LogHandler;
import com.sun.jini.start.LifeCycle;
import net.jini.admin.Administrable;
import net.jini.config.Configuration;
import net.jini.config.ConfigurationException;
import net.jini.config.EmptyConfiguration;
import net.jini.core.event.EventRegistration;
import net.jini.core.event.UnknownEventException;
import net.jini.core.lease.LeaseDeniedException;
import net.jini.core.lease.UnknownLeaseException;
import net.jini.core.lookup.ServiceID;
import net.jini.core.lookup.ServiceItem;
import net.jini.core.lookup.ServiceTemplate;
import net.jini.discovery.DiscoveryManagement;
import net.jini.discovery.LookupDiscovery;
import net.jini.export.Exporter;
import net.jini.id.Uuid;
import net.jini.jeri.BasicILFactory;
import net.jini.jeri.BasicJeriExporter;
import net.jini.jeri.tcp.TcpServerEndpoint;
import net.jini.lease.LeaseRenewalManager;
import net.jini.lookup.LookupCache;
import net.jini.lookup.ServiceDiscoveryEvent;
import net.jini.lookup.ServiceDiscoveryManager;
import net.jini.lookup.entry.ServiceInfo;
import net.jini.security.BasicProxyPreparer;
import net.jini.security.ProxyPreparer;
import net.jini.security.TrustVerifier;
import net.jini.security.proxytrust.ServerProxyTrust;
import org.rioproject.RioVersion;
import org.rioproject.boot.ServiceClassLoader;
import org.rioproject.config.Constants;
import org.rioproject.config.ExporterConfig;
import org.rioproject.core.*;
import org.rioproject.core.jsb.ServiceBeanAdmin;
import org.rioproject.core.jsb.ServiceBeanContext;
import org.rioproject.core.provision.DeployedService;
import org.rioproject.core.provision.DeploymentMap;
import org.rioproject.core.provision.ServiceBeanInstantiator;
import org.rioproject.core.provision.ServiceStatement;
import org.rioproject.event.*;
import org.rioproject.fdh.FaultDetectionHandler;
import org.rioproject.fdh.FaultDetectionHandlerFactory;
import org.rioproject.fdh.FaultDetectionListener;
import org.rioproject.jmx.JMXUtil;
import org.rioproject.jmx.MBeanServerFactory;
import org.rioproject.jsb.ServiceBeanActivation;
import org.rioproject.jsb.ServiceBeanActivation.LifeCycleManager;
import org.rioproject.jsb.ServiceBeanAdapter;
import org.rioproject.monitor.ServiceChannel.ServiceChannelEvent;
import org.rioproject.opstring.*;
import org.rioproject.resolver.Artifact;
import org.rioproject.resolver.Resolver;
import org.rioproject.resolver.ResolverException;
import org.rioproject.resolver.ResolverHelper;
import org.rioproject.resources.client.ServiceDiscoveryAdapter;
import org.rioproject.resources.persistence.PersistentStore;
import org.rioproject.resources.persistence.SnapshotHandler;
import org.rioproject.resources.servicecore.ServiceResource;
import org.rioproject.resources.util.ThrowableUtil;
import org.rioproject.system.ResourceCapability;
import org.rioproject.util.BannerProvider;
import org.rioproject.util.BannerProviderImpl;
import org.rioproject.util.RioManifest;
import org.rioproject.watch.GaugeWatch;
import org.rioproject.watch.PeriodicWatch;
import org.rioproject.watch.ThreadDeadlockMonitor;

import javax.management.MBeanServer;
import javax.management.openmbean.*;
import java.io.*;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.net.MalformedURLException;
import java.net.URL;
import java.rmi.*;
import java.rmi.activation.ActivationID;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.*;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The ProvisionMonitor service provides the capability to deploy and monitor
 * OperationalStrings.
 *
 * @author Dennis Reedy
 */
public class ProvisionMonitorImpl extends ServiceBeanAdapter
        implements
        ProvisionMonitor, ProvisionMonitorImplMBean, ServerProxyTrust {
    /** Component name we use to find items in the configuration */
    static final String CONFIG_COMPONENT = "org.rioproject.monitor";
    /** Logger */
    static final String LOGGER = "org.rioproject.monitor";
    /** ProvisionMonitor logger. */
    static Logger logger = Logger.getLogger(LOGGER);
    /** ProvisionMonitorPeer logger. */
    static Logger peerLogger = Logger.getLogger(LOGGER+".peer");
    /** Collection for all OperationalString OpStringManager instances */
    final List<OpStringManager> opStringManagers = new ArrayList<OpStringManager>();
    /** Collection for all pending (in process) OperationalString 
     * OpStringManager instances */
    final List<OpStringManager> pendingManagers = new ArrayList<OpStringManager>();
    /** EventHandler for ProvisionMonitorEvent consumers */
    EventHandler monitorEventHandler;
    /** ThreadPool for sending ProvisionMonitorEvent notifications */
    Executor monitorEventPool;
    /** EventHandler for ProvisionFailureEvent consumers */
    EventHandler failureHandler;
    /** The provisioner to use for provisioning */
    ServiceProvisioner provisioner;
    /** OpStringLoader for loading XML OperationalStrings */
    OpStringLoader opStringLoader;
    /** A watch to track how long it takes to provision services */
    GaugeWatch provisionWatch;
    /** Log format version */
    static final int LOG_VERSION = 1;
    /** Log File must contain this many records before a snapshot is allowed */
    // TODO - allow this to be a user configurable parameter
    int logToSnapshotThresh = 10;
    /** Manages the persistence of OperationalStrings */
    OpStringLogHandler opStringLogHandler;
    /** Flag to indicate that we are in recover mode */
    boolean inRecovery;
    /** Handles discovery and synchronization with other ProvisionMonitors */
    ProvisionMonitorPeer provisionMonitorPeer;
    /** The Configuration for the Service */
    private Configuration config;
    /** A Timer used to schedule load tasks */
    Timer taskTimer;
    private LifeCycle lifeCycle;
    DeployMonitor deployMonitor;

    /**
     * Create a ProvisionMonitor
     *
     * @throws Exception If the ProvisionMonitorImpl cannot be created
     */
    public ProvisionMonitorImpl() throws Exception {
        super();
    }

    /**
     * Create a ProvisionMonitor launched from the ServiceStarter framework
     *
     * @param configArgs Configuration arguments
     * @param lifeCycle Service lifecycle manager
     *
     * @throws Exception If the ProvisionMonitorImpl cannot be created
     */
    public ProvisionMonitorImpl(String[] configArgs, LifeCycle lifeCycle)
            throws Exception {
        super();
        this.lifeCycle = lifeCycle;
        bootstrap(configArgs);
    }

    /**
     * Create a ProvisionMonitor using RMI Activation
     *
     * @param activationID The ActivationID
     * @param data Serialized data for initialization
     *
     * @throws Exception If the ProvisionMonitorImpl cannot be created
     */
    public ProvisionMonitorImpl(ActivationID activationID, MarshalledObject data)
    throws Exception {
        super();
        this.activationID = activationID;
        bootstrap((String[]) data.get());
    }

    /**
     * Get the ServiceBeanContext and bootstrap the ProvisionMonitor
     *
     * @param configArgs configuration arguments
     *
     * @throws Exception If bootstrapping fails
     */
    protected void bootstrap(String[] configArgs) throws Exception {
        try {
            context = ServiceBeanActivation.getServiceBeanContext(
                                           CONFIG_COMPONENT,
                                           "ProvisionMonitor",
                                           configArgs,
                                           getClass().getClassLoader());
        } catch(Exception e) {
            logger.log(Level.SEVERE, "Getting ServiceElement", e);
            throw e;
        }
        BannerProvider bannerProvider =
            (BannerProvider)context.getConfiguration().getEntry(CONFIG_COMPONENT,
                                                                "bannerProvider",
                                                                BannerProvider.class,
                                                                new BannerProviderImpl());
        logger.info(bannerProvider.getBanner(context.getServiceElement().getName()));
        try {
            start(context);
            LifeCycleManager lMgr = (LifeCycleManager)context
                    .getServiceBeanManager().getDiscardManager();
            lMgr.register(getServiceProxy(), context);
        } catch(Exception e) {
            logger.log(Level.SEVERE, "Register to LifeCycleManager", e);
            throw e;
        }
    }

    /**
     * Override destroy to ensure that all OpStringManagers are shutdown as well
     */
    @Override
    public void destroy() {
        if(logger.isLoggable(Level.FINE))
            logger.log(Level.FINE, "ProvisionMonitor: destroy() notification");
        /* stop the provisioner */
        if(provisioner!=null)
            provisioner.terminate();
        /* Cleanup opStringManagers */
        if(opStringManagers != null) {
            for(OpStringManager opMgr : getOpStringManagers()) {
                opMgr.terminate(false);
            }
        }
        if(deployMonitor!=null)
            deployMonitor.terminate();
        /* Remove watches */
        if(provisionWatch != null)
            getWatchRegistry().deregister(provisionWatch);
        if(taskTimer!=null)
            taskTimer.cancel();
        /* stop the provisionMonitorPeer */
        if(provisionMonitorPeer!=null)
            provisionMonitorPeer.terminate();
        /* destroy the PersistentStore */
        if(snapshotter != null)
            snapshotter.interrupt();
        if(store != null) {
            try {
                store.destroy();
            } catch(Exception t) {
                logger.log(Level.WARNING, "While destroying persistent store",
                           t);
            }
        }
        super.destroy();
        logger.info("Destroyed Monitor");
        if(lifeCycle!=null)
            lifeCycle.unregister(this);
    }

    /**
     * Override parent's getAdmin to return custom service admin
     * 
     * @return A ProvisionMonitorAdminProxy instance
     */
    @Override
    public Object getAdmin() {
        Object adminProxy = null;
        try {
            if (admin == null) {
                Exporter adminExporter = getAdminExporter();
                if (contextMgr != null)
                    admin =
                        new ProvisionMonitorAdminImpl(
                                              this,
                                              adminExporter,
                                              contextMgr.
                                                  getContextAttributeLogHandler());
                else
                    admin = new ProvisionMonitorAdminImpl(this,
                                                          adminExporter);
            }
            admin.setServiceBeanContext(getServiceBeanContext());
            adminProxy = admin.getServiceAdmin();
        } catch (Throwable t) {
            logger.log(Level.WARNING, "Getting ProvisionMonitorAdminImpl", t);
        }
        return (adminProxy);
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
        return (new ProvisionMonitorProxy.Verifier(getExportedProxy()));
    }

    /**
     * Override ServiceBeanAdapter createProxy to return a ProvisionMonitor
     * Proxy
     */
    @Override
    protected Object createProxy() {
        Object proxy = ProvisionMonitorProxy.getInstance(
                                             (ProvisionMonitor)getExportedProxy(),
                                             getUuid());
        /* Get the registry port */
        String sPort = System.getProperty(Constants.REGISTRY_PORT, "0");
        int registryPort = Integer.parseInt(sPort);
        String name = context.getServiceBeanConfig().getName();
        if(registryPort!=0) {
            try {
                Registry registry = LocateRegistry.getRegistry(registryPort);
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
            }
        } else {
            if(logger.isLoggable(Level.FINER))
                        logger.finer("RMI Registry property not set, " +
                                     "unable to bind "+name);
        }
        return(proxy);
    }

    private OpStringManager[] getOpStringManagers() {
        OpStringManager[] mgrs;
        synchronized(opStringManagers) {
            mgrs = opStringManagers.toArray(new OpStringManager[opStringManagers.size()]);
        }
        return mgrs;
    }

    private OpStringManager[] getPendingOpStringManagers() {
        OpStringManager[] pendingMgrs;
        synchronized(pendingManagers) {
            pendingMgrs = pendingManagers.toArray(
                new OpStringManager[pendingManagers.size()]);
        }
        return pendingMgrs;
    }

    /*
     * Get the {@link net.jini.lookup.entry.ServiceInfo} for the Monitor
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

    /*
     * @see org.rioproject.monitor.DeployAdmin#getOperationalStringManagers
     */
    public OperationalStringManager[] getOperationalStringManagers() {
        if(opStringManagers.isEmpty())
            return (new OperationalStringManager[0]);
        ArrayList<OpStringManager> list = new ArrayList<OpStringManager>();
        list.addAll(Arrays.asList(getOpStringManagers()));
        
        /* Get the OperationalStringManager instances that may be initializing
         * as well */
        OpStringManager[] pendingMgrs = getPendingOpStringManagers();
        
        for(OpStringManager pendingMgr : pendingMgrs) {
            boolean add = true;
            for(OpStringManager mgr : list) {
                if(mgr.getName().equals(pendingMgr.getName())) {
                    add = false;
                    break;
                }
            }
            if(add)
                list.add(pendingMgr);
        }
        //list.addAll(Arrays.asList(pendingMgrs));

        OperationalStringManager[] os = new OperationalStringManager[list.size()];
        int i = 0;
        for (OpStringManager opMgr : list) {
            os[i++] = opMgr.getProxy();
        }
        return (os);
    }

    /*
     * @see org.rioproject.monitor.DeployAdmin#getOperationalStringManager
     */
    public OperationalStringManager getOperationalStringManager(String name)
    throws OperationalStringException {
        if(name==null)
            throw new IllegalArgumentException("name is null");
        OperationalStringManager opStringManager = null;
        OpStringManager opMgr = getOpStringManager(name);
        if(opMgr!=null && opMgr.isActive()) {
            opStringManager = opMgr.getProxy();
        } else  {
            try {
                DeployAdmin dAdmin = getPrimaryManager(name);
                if(dAdmin!=null) {
                    OperationalStringManager mgr =
                        dAdmin.getOperationalStringManager(name);
                    if(mgr.isManaging()) {
                        opStringManager = mgr;
                    }
                }
            } catch(RemoteException e) {
                if(logger.isLoggable(Level.FINE))
                    logger.log(Level.FINE,
                               "Communicating to peer during " +
                               "getOperationalStringManager " +
                               "for ["+name+"]",
                               e);
            } catch(OperationalStringException e) {
                /* ignore */
            }
        }

        if(opStringManager==null)
            throw new OperationalStringException("Unmanaged " +
                                                 "OperationalString ["+name+"]",
                                                 false);
        return(opStringManager);
    }

    /*
     * @see org.rioproject.monitor.ProvisionMonitorImplMBean#deploy
     */
    public Map deploy(String opStringLocation) throws MalformedURLException {
        if(opStringLocation == null)
            throw new IllegalArgumentException("argument cannot be null");
        URL opStringURL = null;
        try {
            opStringURL = getArtifactURL(opStringLocation);
        } catch (OperationalStringException e) {
            e.printStackTrace();
        }
        if(opStringURL==null) {
            File f = new File(opStringLocation);
            if(f.exists())
                opStringURL = f.toURI().toURL();
            else
                opStringURL = new URL(opStringLocation);
        }

        Map<String, Throwable> m = null;
        /*
         * The deploy call may be invoked via the MBeanServer. If it is
         * context classloader will not be the classloader which loaded this
         * bean. If the context classloader is not a ServiceClassLoader, then
         * set the current context classloader to be the classloader which
         * loaded this class.
         */
        final Thread currentThread = Thread.currentThread();
        final ClassLoader cCL = AccessController.doPrivileged(
            new PrivilegedAction<ClassLoader>() {
                public ClassLoader run() {
                    return (currentThread.getContextClassLoader());
                }
            });
        boolean swapCLs = !(cCL instanceof ServiceClassLoader);
        try {
            final ClassLoader myCL = AccessController.doPrivileged(
                new PrivilegedAction<ClassLoader>() {
                    public ClassLoader run() {
                        return (getClass().getClassLoader());
                    }
                });
            if(swapCLs) {
                AccessController.doPrivileged(new PrivilegedAction<Void>() {
                    public Void run() {
                        currentThread.setContextClassLoader(myCL);
                        return (null);
                    }
                });
            }
            m = deploy(opStringURL, null);
        } catch(OperationalStringException e) {
            Throwable cause =  e.getCause();
            if(cause==null)
                cause = e;
            m = new HashMap<String, Throwable>();
            m.put(cause.getClass().getName(), cause);
            if(logger.isLoggable(Level.FINE))
                logger.log(Level.FINE,
                           "Deploying ["+opStringURL+"]",
                           e);
        } finally {
            if(swapCLs) {
                AccessController.doPrivileged(new PrivilegedAction<Void>() {
                    public Void run() {
                        currentThread.setContextClassLoader(cCL);
                        return (null);
                    }
                });
            }
        }
        return(m);
    }

    private URL getArtifactURL(String a) throws OperationalStringException {
        boolean isArtifact = false;
        try {
            new Artifact(a);
            isArtifact = true;
        } catch(Exception e) {
            /* no-op */
        }
        URL opStringURL = null;
        if(isArtifact) {
            try {
                Resolver r = ResolverHelper.getInstance();
                opStringURL = r.getLocation(a, "oar");
                if(opStringURL==null)
                    throw new OperationalStringException("Artifact "+a+" not resolvable");
            } catch (ResolverException e) {
                throw new OperationalStringException(e.getLocalizedMessage(), e);
            }
        }
        return opStringURL;
    }

    /*
     * @see org.rioproject.monitor.ProvisionMonitorImplMBean#deploy
     */
    public Map<String, Throwable> deploy(String opStringLocation, ServiceProvisionListener listener) throws OperationalStringException {
        if(opStringLocation == null)
            throw new IllegalArgumentException("OperationalString location " +
                                               "cannot be null");

        URL opStringURL = getArtifactURL(opStringLocation);
        if(opStringURL==null) {
            try {
                opStringURL = new URL(opStringLocation);
            } catch (MalformedURLException e) {
                throw new OperationalStringException("Failed to create URL from "+opStringLocation, e);
            }
        }

        return deploy(opStringURL, listener);
    }


    /*
     * @see org.rioproject.monitor.DeployAdmin#deploy
     */
    public Map<String, Throwable> deploy(URL opStringUrl, ServiceProvisionListener listener)
    throws OperationalStringException {
        if(opStringUrl == null)
            throw new IllegalArgumentException("OperationalString URL " +
                                               "cannot be null");
        Map<String, Throwable> map = new HashMap<String, Throwable>();

        try {
            if(opStringUrl.toExternalForm().endsWith("oar")) {
                OAR oar = new OAR(opStringUrl);
                StringBuilder sb = new StringBuilder();
                sb.append("jar:").append(oar.getURL().toExternalForm()).append("!/").append(oar.getOpStringName());
                opStringUrl = new URL(sb.toString());
            }
            OperationalString[] opStrings =
                opStringLoader.parseOperationalString(opStringUrl);
            if(opStrings != null) {
                for (OperationalString opString : opStrings) {
                    if (!opStringExists(opString.getName())) {
                        if (logger.isLoggable(Level.INFO))
                            logger.log(Level.INFO,
                                       "Deploying Operational String " +
                                       "[" + opString.getName() + "]");
                        addOperationalString(opString,
                                             map,
                                             null,
                                             null,
                                             listener);
                    } else {
                        if (logger.isLoggable(Level.INFO))
                            logger.log(Level.INFO,
                                       "Operational String " +
                                       "[" + opString.getName() + "] already " +
                                       "deployed");
                    }
                }
            }
        } catch(Throwable t) {
            throw new OperationalStringException("Deploying " +
                                                 "OperationalString",
                                                 t);
        }
        return (map);
    }

    /*
     * @see org.rioproject.monitor.DeployAdmin#deploy
     */
    public Map<String, Throwable> deploy(OperationalString opString,
                                         ServiceProvisionListener listener)
    throws OperationalStringException {
        if(opString == null)
            throw new IllegalArgumentException("OperationalString cannot be null");
        Map<String, Throwable> map = new HashMap<String, Throwable>();
        try {
            if(!opStringExists(opString.getName())) {
                if(logger.isLoggable(Level.INFO))
                    logger.log(Level.INFO,
                               "Deploying Operational String "+"["+
                               opString.getName()+"]");
                addOperationalString(opString, map, null, null, listener);
            } else {
                if(logger.isLoggable(Level.INFO))
                    logger.log(Level.INFO,
                               "Operational String "+"["+opString.getName()+"] " +
                               "already "+"deployed");
            }
        } catch(Throwable t) {
            logger.log(Level.WARNING,
                       "Deploying OperationalString ["+opString.getName()+"]",
                       t);
            if(!(t instanceof OperationalStringException))
                throw new OperationalStringException("Deploying " +
                                                     "OperationalString " +
                                                     "["+opString.getName()+"]",
                                                     t);
            throw (OperationalStringException)t;
        }
        return (map);
    }


    /*
     * @see org.rioproject.monitor.ProvisionMonitorImplMBean#undeploy(String)
     */
    public boolean undeploy(String opStringName) {
        boolean undeployed = false;
        try {
            undeployed = undeploy(opStringName, true);
        } catch(OperationalStringException e) {
            logger.log(Level.WARNING,
                       "Undeploying ["+opStringName+"]",
                       e);
        }
        return(undeployed);
    }

    /*
     * @see org.rioproject.monitor.DeployAdmin#undeploy
     */
    public boolean undeploy(String name, boolean terminate)
    throws OperationalStringException  {
        if(name == null)
            throw new IllegalArgumentException("name cannot be null");
        URL artifactURL = getArtifactURL(name);
        if(artifactURL!=null) {
            try {
                OAR oar = new OAR(artifactURL);
                OperationalString[] opstring = oar.loadOperationalStrings();
                name = opstring[0].getName();
            } catch(Exception e) {
                throw new OperationalStringException("Unable to undeploy, cannot parse/load ["+name+"]");
            }
        }
        boolean undeployed = false;
        OpStringManager opMgr = getOpStringManager(name);
        if(opMgr == null || (!opMgr.isActive())) {
            try {
                DeployAdmin dAdmin = getPrimaryManager(name);
                if(dAdmin!=null) {
                    OperationalStringManager mgr =
                        dAdmin.getOperationalStringManager(name);
                    if(mgr.isManaging()) {
                        dAdmin.undeploy(name);
                        undeployed = true;
                    }
                }
            } catch(RemoteException e) {
                if(logger.isLoggable(Level.FINE))
                    logger.log(Level.FINE,
                               "Communicating to peer during undeployment " +
                               "of ["+name+"]",
                               e);
            } catch(OperationalStringException e) {
                /* ignore */
            }

        } else {
            opMgr.setDeploymentStatus(OperationalString.UNDEPLOYED);
            OperationalString opString = opMgr.doGetOperationalString();
            OperationalString[] terminated = opMgr.terminate(terminate);
            if(logger.isLoggable(Level.INFO))
                logger.log(Level.INFO,
                           "Undeployed Operational String "+"["+
                           opString.getName()+"]");
            stateChanged(opMgr, true);
            undeployed = true;
            for(OperationalString os : terminated) {
                processEvent(
                    new ProvisionMonitorEvent(getEventProxy(),
                                              ProvisionMonitorEvent.Action.OPSTRING_UNDEPLOYED,
                                              os));
            }

        }
        if(!undeployed) {
            throw new OperationalStringException("No deployment for " +
                                                 "["+name+"] found");
        }

        return (true);
    }

    /*
     * @see org.rioproject.monitor.DeployAdmin#hasDeployed
     */
    public boolean hasDeployed(String opStringName) {
        if(opStringName == null)
            throw new IllegalArgumentException("Parameters cannot be null");
        for(OpStringManager opMgr : getOpStringManagers()) {
            if(opStringName.equals(opMgr.getName())) {
                return (true);
            }
        }
        return (false);
    }

    /*
     * @see org.rioproject.monitor.ProvisionMonitorImplMBean#getDeployments
     */
    public TabularData getDeployments() {
        String[] itemNames = new String[] {"Name",
                                           "Status",
                                           "Role",
                                           "Deployed"};

        OpenType[] itemTypes = new OpenType[]{SimpleType.STRING,
                                              SimpleType.STRING,
                                              SimpleType.STRING,
                                              SimpleType.DATE};

        TabularDataSupport tabularDataSupport = null;
        try {
            CompositeType row = new CompositeType("Deployments", // typeName
                                                  "Deployments", // description
                                                  itemNames,
                                                  itemNames,
                                                  itemTypes);

            TabularType tabularType = new TabularType("Deployments",
                                                      "Deployments",
                                                      row,
                                                      new String[]{"Name"});
            tabularDataSupport = new TabularDataSupport(tabularType);
            OpStringManager[] mgrs = opStringManagers.toArray(
                    new OpStringManager[opStringManagers.size()]);

            for (OpStringManager mgr : mgrs) {
                Date deployed = null;
                if (mgr.getDeploymentDates().length > 0) {
                    Date[] dDates = mgr.getDeploymentDates();
                    deployed = dDates[dDates.length - 1];
                }
                int status = mgr.doGetOperationalString().getStatus();
                String sStatus = null;
                switch (status) {
                    case OperationalString.BROKEN:
                        sStatus = "Broken";
                        break;
                    case OperationalString.COMPROMISED:
                        sStatus = "Compromised";
                        break;
                    case OperationalString.INTACT:
                        sStatus = "Intact";
                        break;
                    case OperationalString.SCHEDULED:
                        sStatus = "Scheduled";
                        break;
                }
                String role = (mgr.isActive() ? "Primary" : "Backup");
                Object[] data = new Object[]{mgr.getName(),
                                             sStatus,
                                             role,
                                             deployed};
                CompositeData compositeData =
                    new CompositeDataSupport(row, itemNames, data);
                tabularDataSupport.put(compositeData);
            }

        } catch(OpenDataException e) {
            logger.log(Level.WARNING, e.toString(), e);
        }

        return(tabularDataSupport);
    }

    /*
     * @see org.rioproject.monitor.ProvisionMonitor#getPeerInfo
     */
    public PeerInfo[] getBackupInfo()  {
        return (provisionMonitorPeer.getBackupInfo());
    }

    /*
     * @see org.rioproject.monitor.ProvisionMonitor#getPeerInfo
     */
    public PeerInfo getPeerInfo() throws RemoteException {
        return (provisionMonitorPeer.doGetPeerInfo());
    }

    /*
     * @see org.rioproject.monitor.ProvisionMonitor#assignBackupFor
     */
    public boolean assignBackupFor(ProvisionMonitor primary)
    throws RemoteException {
        return (provisionMonitorPeer.addAsBackupFor(primary));
    }

    /*
     * @see org.rioproject.monitor.ProvisionMonitor#removeBackupFor
     */
    public boolean removeBackupFor(ProvisionMonitor primary)
    throws RemoteException {
        return (provisionMonitorPeer.removeAsBackupFor(primary));
    }

    /*
     * @see org.rioproject.monitor.ProvisionMonitor#update
     */
    public void update(PeerInfo peerInfo)
    throws RemoteException {
        provisionMonitorPeer.peerUpdated(peerInfo);
    }

    /*
     * @see org.rioproject.core.provision.ProvisionManager#register
     */
    public EventRegistration register(ServiceBeanInstantiator instantiator,
                                      MarshalledObject handback,
                                      ResourceCapability resourceCapability,
                                      List<DeployedService> deployedServices,
                                      int serviceLimit,
                                      long duration)
    throws LeaseDeniedException, RemoteException {
        return (provisioner.register(instantiator,
                                     handback,
                                     resourceCapability,
                                     deployedServices,
                                     serviceLimit,
                                     duration));
    }

    /*
     * @see org.rioproject.core.provision.ProvisionManager#update
     */
    public void update(ServiceBeanInstantiator instantiator,
                       ResourceCapability resourceCapability,
                       List<DeployedService> deployedServices,
                       int serviceLimit)
    throws UnknownLeaseException, RemoteException {
        /* delegate to provisioner */
        provisioner.handleFeedback(instantiator,
                                   resourceCapability,
                                   deployedServices,
                                   serviceLimit);
    }

    /*
     * @see org.rioproject.core.provision.ProvisionManager#getServiceBeanInstantiators
     */
    public ServiceBeanInstantiator[] getServiceBeanInstantiators() {
        ServiceResource[] resources =
            provisioner.getServiceResourceSelector().getServiceResources();
        List<ServiceBeanInstantiator> list = new ArrayList<ServiceBeanInstantiator>();
        for(ServiceResource s : resources) {
            list.add(((InstantiatorResource)s.getResource()).getServiceBeanInstantiator());
        }        
        return list.toArray(new ServiceBeanInstantiator[list.size()]);
    }

    /**
     * Add an OpStringManager to this ProvisionMonitor.
     * 
     * @param opString The OperationalString to add
     * @param map A Map to store any exceptions produced while loading the
     * opString
     * @param parent The parent of the opString. The addOperationalString
     * method recursively processes OperationalString instances and uses this
     * parameter to indicate a nested OperationalString
     * @param dAdmin The managing DeployAdmin, if the OperationalString is
     * being added in an inactive mode, as a backup. If this value is null,
     * this ProvisionMonitor is set to be active
     * @param listener A ServiceProvisionListener that will be set to the
     * OperationalStringManager for notification of services as they are
     * deployed initially.
     *
     * @return An OpStringManager
     *
     * @throws Exception if an OpStringManger cannot be created
     */
    public OpStringManager addOperationalString(OperationalString opString,
                                                Map<String, Throwable> map,
                                                OpStringManager parent,
                                                DeployAdmin dAdmin,
                                                ServiceProvisionListener listener)
        throws Exception {
        OpStringManager opMgr = null;
        boolean active = dAdmin==null;
        try {
            if(!opStringExists(opString.getName())) {
                if(logger.isLoggable(Level.FINEST))
                    logger.finest("Adding OpString ["+opString.getName()+"] "+
                                  "active ["+active+"]");

                try {
                    Resolver resolver = ResolverHelper.getInstance();
                    OpStringUtil.checkCodebase(opString,
                                               System.getProperty(Constants.CODESERVER),
                                               resolver);
                } catch (IOException e) {
                    Throwable cause = e.getCause()==null?e:e.getCause();
                    if(logger.isLoggable(Level.FINEST))
                        logger.log(Level.FINEST,
                                   "Unable to resolve resources for services " +
                                   "in ["+opString.getName()+"], "+
                                   cause.getClass().getName()+": "+cause.getMessage(),
                                   e);
                    throw new OperationalStringException(
                        "Unable to resolve resources for services " +
                        "in ["+opString.getName()+"], "+
                        cause.getClass().getName()+": "+cause.getMessage(),
                        e);
                }
                try {
                    opMgr = new OpStringManager(opString,
                                                parent,
                                                active,
                                                config);
                } catch (RemoteException e) {
                    logger.log(Level.WARNING,
                               "Creating OpStringManager",
                               e);
                    return(null);
                }
                synchronized(pendingManagers) {
                    pendingManagers.add(opMgr);
                }

                Map<String, Throwable> errorMap = opMgr.init(active, listener);
                //Map errorMap = opMgr.init(active, listener);
                synchronized(opStringManagers) {
                    opStringManagers.add(opMgr);
                }

                if(dAdmin!=null) {
                    OperationalStringManager activeMgr;
                    Map<ServiceElement, ServiceBeanInstance[]> elemInstanceMap =
                        new HashMap<ServiceElement, ServiceBeanInstance[]>();
                    try {
                        activeMgr =
                            dAdmin.getOperationalStringManager(
                                opString.getName());
                        ServiceElement[] elems = opString.getServices();
                        for (ServiceElement elem : elems) {
                            try {
                                ServiceBeanInstance[] instances =
                                    activeMgr.getServiceBeanInstances(elem);
                                elemInstanceMap.put(elem, instances);
                            } catch (Exception e) {
                                logger.log(Level.WARNING,
                                           "Getting ServiceBeanInstances from " +
                                           "active testManager",
                                           e);
                            }
                        }
                        opMgr.startManager(listener, elemInstanceMap);
                    } catch(Exception e) {
                        logger.log(Level.WARNING,
                                   "Getting active OperationalStringManager",
                                   e);
                    }
                } else {
                    opMgr.startManager(listener);
                }

                if(map != null)
                    map.putAll(errorMap);
                stateChanged(opMgr, false);
                OperationalString[] nestedStrings =
                    opString.getNestedOperationalStrings();
                for (OperationalString nestedString : nestedStrings) {
                    addOperationalString(nestedString,
                                         map,
                                         opMgr,
                                         dAdmin,
                                         listener);
                }
            } else {
                if(parent != null) {
                    OpStringManager mgr = getOpStringManager(opString.getName());
                    if(mgr != null) {
                        parent.addNested(mgr);
                    }
                }
            }
        } finally {
            synchronized(pendingManagers) {
                pendingManagers.remove(opMgr);
            }
        }
        if(opMgr!=null && active) {
            ProvisionMonitorEvent event =
                new ProvisionMonitorEvent(
                                  getEventProxy(),
                                  ProvisionMonitorEvent.Action.OPSTRING_DEPLOYED,
                                  opMgr.doGetOperationalString());
            processEvent(event);
        }
        return (opMgr);
    }

    /**
     * Determine if the OperationalString with the provided name is deployed
     * 
     * @param opStringName The name of the OperationalString
     *
     * @return true if the  OperationalString with the provided name is loaded
     */
    public boolean opStringExists(String opStringName) {
        boolean exists = false;
        synchronized(pendingManagers) {
            for (OpStringManager mgr : pendingManagers) {
                if (mgr.getName().equals(opStringName)) {
                    exists = true;
                    break;
                }
            }
        }
        if(!exists) {
            for(OpStringManager opMgr : getOpStringManagers()) {
                if(opStringName.equals(opMgr.getName())) {
                    exists = true;
                    break;
                }
            }
        }

        return (exists);
    }

    /**
     * Get the ProvisionMonitor event proxy source
     *
     * @return The ProvisionMonitor event proxy source
     */
    protected ProvisionMonitor getEventProxy() {
        return((ProvisionMonitor)getServiceProxy());
    }

    /**
     * Get the primary OperationalStringManager for the opstring
     *
     * @param opStringName The opstring name
     * @return The primary OperationalStringManager for the opsting, or null
     * if not found
     */
    protected DeployAdmin getPrimaryManager(String opStringName) {
        DeployAdmin primary = null;
        PeerInfo[] peers = provisionMonitorPeer.getBackupInfo();
        for (PeerInfo peer : peers) {
            ProvisionMonitor peerMon = peer.getService();
            try {
                DeployAdmin dAdmin = (DeployAdmin) peerMon.getAdmin();
                OperationalStringManager mgr =
                    dAdmin.getOperationalStringManager(opStringName);
                if (mgr.isManaging()) {
                    primary = dAdmin;
                    break;
                }
            } catch (RemoteException e) {
                if (logger.isLoggable(Level.FINE))
                    logger.log(Level.FINE,
                               "Communicating to peer while searching for " +
                               "primary testManager of [" + opStringName + "]",
                               e);
            } catch (OperationalStringException e) {
                /* ignore */
            }
        }
        return(primary);
    }

    /**
     * Get the OpStringManager for an OperationalString
     * 
     * @param name The name of an OperationalString
     * @return The corresponding OpStringManager, or null if
     * not found
     */
    protected OpStringManager getOpStringManager(String name) {
        if(name == null)
            return (null);

        OpStringManager mgr = null;
        OpStringManager[] pendingMgrs = getPendingOpStringManagers();
        for (OpStringManager pendingMgr : pendingMgrs) {
            if (name.equals(pendingMgr.getName())) {
                mgr = pendingMgr;
                break;
            }
        }
        if(mgr==null) {
            for(OpStringManager opMgr : getOpStringManagers()) {
                if(name.equals(opMgr.getName())) {
                    mgr = opMgr;
                    break;
                }
            }
        }
        return (mgr);
    }

    /**
     * Get all OperationalString objects from the Collection of OpStringManagers
     * 
     * @return Array of OperationalString objects
     */
    protected OperationalString[] getOperationalStrings() {
        if(opStringManagers.isEmpty())
            return (new OperationalString[0]);
        List<OpStringManager> list = new ArrayList<OpStringManager>();
        for(OpStringManager opMgr : getOpStringManagers()) {
            if(opMgr.isTopLevel())
                list.add(opMgr);
        }
        OperationalString[] os = new OperationalString[list.size()];
        int i = 0;
        for (OpStringManager opMgr : list) {
            try {
                os[i++] = opMgr.getOperationalString();
            } catch (Exception e) {
                logger.log(Level.WARNING,
                           "Getting all OperationalString instances", e);
            }
        }
        return (os);
    }

    /**
     * Override parent initialize() method to provide specific initialization
     * for the ProvisionMonitor
     * 
     * @param context The ServiceBeanContext
     */
    public void initialize(ServiceBeanContext context) throws Exception {
        try {
            /*
             * Determine if a log directory has been provided. If so, create a
             * PersistentStore
             */
            String logDirName =
                (String)context.getConfiguration().getEntry(CONFIG_COMPONENT,
                                                            "logDirectory",
                                                            String.class,
                                                            null);
            if(logDirName != null) {
                opStringLogHandler = new OpStringLogHandler();
                store = new PersistentStore(logDirName, opStringLogHandler,
                                            opStringLogHandler);
                logger.log(Level.INFO,
                           "ProvisionMonitor: using absolute logdir path ["
                           + store.getStoreLocation() + "]");
                store.snapshot();
                super.initialize(context, store);
                inRecovery = false;
            } else {
                super.initialize(context);
            }
            config = context.getConfiguration();
            provisioner = new ServiceProvisioner(config);
            provisionWatch = new GaugeWatch("Provision Clock", config);
            getWatchRegistry().register(provisionWatch);

            if(System.getProperty(Constants.CODESERVER)==null) {
                System.setProperty(Constants.CODESERVER, context.getExportCodebase());
                logger.warning("The system property ["+Constants.CODESERVER+"] " +
                               "has not been set, it has been resolved to: "+
                               System.getProperty(Constants.CODESERVER));

            }
            /*
             * Set up the Threadpool for ProvisionMonitorEvent notifications
             */
            int provisionMonitorEventTaskPoolMaximum = 10;
            try {
                provisionMonitorEventTaskPoolMaximum =
                    Config.getIntEntry(config,
                                       CONFIG_COMPONENT,
                                       "provisionMonitorEventTaskPoolMaximum",
                                       provisionMonitorEventTaskPoolMaximum,
                                       1,
                                       100);
            } catch(Throwable t) {
                logger.log(Level.WARNING,
                           "Exception getting " +
                           "provisionMonitorEventTaskPoolMaximum",
                           t);
            }
            monitorEventPool =
                Executors.newFixedThreadPool(provisionMonitorEventTaskPoolMaximum);
            EventDescriptor clientEventDesc =
                new EventDescriptor(ProvisionMonitorEvent.class,
                                    ProvisionMonitorEvent.ID);
            monitorEventHandler =
                new DispatchEventHandler(clientEventDesc, config);

            getEventTable().put(clientEventDesc.eventID, monitorEventHandler);
            EventDescriptor failureEventDesc =
                new EventDescriptor(ProvisionFailureEvent.class,
                                    ProvisionFailureEvent.ID);
            failureHandler = new DispatchEventHandler(failureEventDesc, config);
            getEventTable().put(ProvisionFailureEvent.ID, failureHandler);

            registerEventAdapters();

            /*
             * Initialize various provisioner attributes: - provision failure
             * handler - ambiguous failure processor - watch for
             * provision notifications
             */
            provisioner.setProvisionFailureHandler(failureHandler);
            provisioner.setEventSource(getEventProxy());
            provisioner.setWatch(provisionWatch);
            /*
             * Add attributes
             */

            /* Check for JMXConnection */
            addAttributes(JMXUtil.getJMXConnectionEntries(config));
            
            addAttribute(clientEventDesc);
            addAttribute(failureEventDesc);

            /* Utility for loading OperationalStrings */
            opStringLoader = getOpStringLoader();

            /*
            * If we have a persistent store, process recovered or updated
            * OperationalString elements
            */
            if(store != null && opStringLogHandler != null) {
                opStringLogHandler.processRecoveredOpStrings();
                opStringLogHandler.processUpdatedOpStrings();
            }

            /*
            * Start the ProvisionMonitorPeer
            */
            provisionMonitorPeer =
                new ProvisionMonitorPeer(context.getConfiguration(),
                                         context.getDiscoveryManagement());

            /* Create the task Timer */
            taskTimer = new Timer(true);

            /*
             * Setup the DeployMonitor with DeployHandlers
             */
            long deployMonitorPeriod = 1000*30;
            try {
                deployMonitorPeriod = Config.getLongEntry(config,
                                                          CONFIG_COMPONENT,
                                                          "deployMonitorPeriod",
                                                          deployMonitorPeriod,
                                                          -1,
                                                          Long.MAX_VALUE);
            } catch(Throwable t) {
                logger.log(Level.WARNING,
                           "Non-fatal exception getting deployMonitorPeriod, " +
                           "using default value of ["+deployMonitorPeriod+"] " +
                           "milliseconds. Continuing on with initialization.",
                           t);
            }
            if(logger.isLoggable(Level.CONFIG))
                logger.config("deployMonitorPeriod="+deployMonitorPeriod);

            if(deployMonitorPeriod>0) {
                String rioHome = System.getProperty("RIO_HOME");
                if(!rioHome.endsWith("/"))
                    rioHome = rioHome+"/";
                File deployDir = new File(rioHome+"deploy");
                DeployHandler fsDH = new FileSystemOARDeployHandler(deployDir);
                DeployHandler[] deployHandlers =
                    (DeployHandler[])config.getEntry(CONFIG_COMPONENT,
                                                     "deployHandlers",
                                                     DeployHandler[].class,
                                                     new DeployHandler[]{fsDH});
                deployMonitor = new DeployMonitor(deployHandlers,
                                                  deployMonitorPeriod);

            } else {
                logger.info("OAR hot deploy capabilities have been disabled");
            }
            /* Get the timeout value for loading OperationalStrings */
            long initialOpStringLoadDelay = 1000*5;
            try {
                initialOpStringLoadDelay =
                    Config.getLongEntry(config,
                                        CONFIG_COMPONENT,
                                        "initialOpStringLoadDelay",
                                        initialOpStringLoadDelay,
                                        1,
                                        Long.MAX_VALUE);
            } catch(Throwable t) {
                logger.log(Level.WARNING,
                           "Exception getting initialOpStringLoadDelay",
                           t);
            }
            if(logger.isLoggable(Level.CONFIG))
                logger.config("initialOpStringLoadDelay="+
                              initialOpStringLoadDelay);

            /*
             * Schedule the task to Load any configured OperationalStrings
             */
            long now = System.currentTimeMillis();
            taskTimer.schedule(new InitialOpStringLoadTask(config),
                               new Date(now+initialOpStringLoadDelay));



            /*
            * If we were booted without a serviceID (perhaps using RMI
            * Activation), then create one
            */
            if(serviceID == null) {
                if(logger.isLoggable(Level.FINE))
                    logger.fine("Creating new ServiceID from UUID="
                                + getUuid().toString());
                serviceID = new ServiceID(getUuid().getMostSignificantBits(),
                                          getUuid().getLeastSignificantBits());
            }
            /*
             * Force a snapshot so the persistent store reflects the current
             * state of the Provisioner
             */
            if(store != null)
                store.snapshot();

            MBeanServer mbs = MBeanServerFactory.getMBeanServer();
            final ThreadDeadlockMonitor threadDeadlockMonitor = new ThreadDeadlockMonitor();
            ThreadMXBean threadMXBean = JMXUtil.getPlatformMXBeanProxy(mbs,
                                                                       ManagementFactory.THREAD_MXBEAN_NAME,
                                                                       ThreadMXBean.class);
            threadDeadlockMonitor.setThreadMXBean(threadMXBean);
            PeriodicWatch p = new PeriodicWatch("Thread Deadlock", config) {
                public void checkValue() {
                    threadDeadlockMonitor.getThreadDeadlockCalculable();
                }
            };
            p.setPeriod(10*1000); // 10 seconds
            context.getWatchRegistry().register(p);
            p.start();

            if(logger.isLoggable(Level.INFO)) {
                String[] g = context.getServiceBeanConfig().getGroups();
                StringBuffer buff = new StringBuffer();
                if(g!= LookupDiscovery.ALL_GROUPS) {
                    for(int i=0; i<g.length; i++) {
                        if(i>0)
                            buff.append(", ");
                        buff.append(g[i]);
                    }
                }
                logger.info("Started Provision Monitor ["+buff.toString()+"]");
            }
        } catch(Exception e) {
            logger.log(Level.SEVERE, "Unrecoverable initialization exception",
                       e);
            destroy();
        }
    }


    private void registerEventAdapters()
        throws LeaseDeniedException, UnknownEventException, RemoteException {
        // translate ProvisionFailureEvents to notifications
        EventDescriptor provisionFailureEventDescriptor =
            new EventDescriptor(ProvisionFailureEvent.class,
                                ProvisionFailureEvent.ID);
        ProvisionFailureEventAdapter provisionFailureEventAdapter =
            new ProvisionFailureEventAdapter(
                objectName,
                getNotificationBroadcasterSupport());
        register(provisionFailureEventDescriptor,
                 provisionFailureEventAdapter,
                 null,
                 Long.MAX_VALUE);

        // register notification info
        mbeanNoticationInfoList.add(
            provisionFailureEventAdapter.getNotificationInfo());

        // translate ProvisionMonitorEvents to notifications
        EventDescriptor provisionMonitorEventDescriptor = new EventDescriptor(
            ProvisionMonitorEvent.class, ProvisionMonitorEvent.ID
        );
        ProvisionMonitorEventAdapter provisionMonitorEventAdapter =
            new ProvisionMonitorEventAdapter(
                objectName,
                getNotificationBroadcasterSupport());
        register(provisionMonitorEventDescriptor,
                 provisionMonitorEventAdapter,
                 null,
                 Long.MAX_VALUE);
        //register notification info
        mbeanNoticationInfoList.add(
            provisionMonitorEventAdapter.getNotificationInfo());
    }

    /**
     * Get the OpStringloader, the utility to load OperationalStrings
     *
     * @return The OpStringLoader
     *
     * @throws Exception if the OpStringLoader cannot be created
     */
    protected OpStringLoader getOpStringLoader() throws Exception {
        return(new OpStringLoader(this.getClass().getClassLoader()));
    }

    /**
     * Notification of an OperationalString state change. This method is
     * invoked whenever an OperationalString has been added or removed, and
     * whenever elements of an OperationalString have been modified, added or
     * removed.
     * 
     * @param opMgr The OpStringManager that has changed
     * @param remove Whether or not the OpStringManager has been removed
     */
    protected void stateChanged(OpStringManager opMgr, boolean remove) {
        if(inRecovery || store == null)
            return;
        if(!opMgr.isActive())
            return;
        try {
            store.acquireMutatorLock();
            int action = (remove? RecordHolder.REMOVED:RecordHolder.MODIFIED);
            store.update(new MarshalledObject<RecordHolder>(
                                   new RecordHolder(opMgr.getOperationalString(),
                                                    action)));
        } catch(IllegalStateException ise) {
            logger.log(Level.WARNING,
                       "OperationalString state change notification", ise);
        } catch(Throwable t) {
            logger.log(Level.WARNING,
                       "OperationalString state change notification", t);
        } finally {
            store.releaseMutatorLock();
        }
    }

    /**
     * Helper method to obtain a PoolableThread and create a 
     * ProvisionMonitorTask to send a ProvisionMonitorEvent
     *
     * @param event The ProvisionMonitorEvent to send
     */
    protected void processEvent(ProvisionMonitorEvent event) {
        monitorEventPool.execute(new ProvisionMonitorEventTask(monitorEventHandler,
                                                               event));
    }

    /**
     * Get the primary OperationalStringManager for an opstring
     *
     * @param name The name of an OperationalString
     *
     * @return The primary OperationalStringManager if found, or null if not
     */
    OperationalStringManager getPrimary(String name) {
        OperationalStringManager mgr = null;
        ProvisionMonitor[] monitors =
            provisionMonitorPeer.getProvisionMonitorPeers();
        for (ProvisionMonitor monitor : monitors) {
            try {
                DeployAdmin dAdmin = (DeployAdmin) monitor.getAdmin();
                OperationalStringManager[] mgrs =
                    dAdmin.getOperationalStringManagers();
                for (OperationalStringManager mgr1 : mgrs) {
                    if (mgr1.getOperationalString().getName().equals(name) &&
                        mgr1.isManaging()) {
                        mgr = mgr1;
                        break;
                    }
                }
            } catch (Exception e) {
                logger.log(Level.WARNING,
                           "Getting the primary OperationalStringManager " +
                           "for [" + name + "]",
                           e);
            }
        }
        return(mgr);
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
            StringBuffer sb = new StringBuffer();
            sb.append("+========================+\n");
            //int i = 0;
            for (Object comp : keys) {
                sb.append("Component: ");
                sb.append(comp);
                Object o = errorMap.get(comp);
                if (o instanceof Throwable) {
                    StackTraceElement[] els = ((Throwable) o).getStackTrace();
                    for (StackTraceElement el : els) {
                        sb.append("\tat ");
                        sb.append(el);
                        sb.append("\n");
                    }
                } else {
                    sb.append(" ");
                    sb.append(o.toString());
                }
            }
            sb.append("\n+========================+");
            if(logger.isLoggable(Level.FINE))
                logger.log(Level.FINE, sb.toString());
        }
    }

    /**
     * Scheduled Task which will load configured OperationalString files
     */
    class InitialOpStringLoadTask extends TimerTask {
        Configuration config;

        InitialOpStringLoadTask(Configuration config) {
            this.config = config;
        }
        /**
         * The action to be performed by this timer task.
         */
        public void run() {
            loadInitialOpStrings(config);
        }

        void loadInitialOpStrings(Configuration config) {
            if(!inRecovery) {
                String[] initialOpStrings = new String[] {};
                try {
                    initialOpStrings =
                        (String[])config.getEntry(CONFIG_COMPONENT,
                                                  "initialOpStrings",
                                                  String[].class,
                                                  initialOpStrings);
                    if(logger.isLoggable(Level.FINE)) {
                        if(initialOpStrings.length>0) {
                            StringBuffer sb = new StringBuffer();
                            for(int i=0; i< initialOpStrings.length; i++) {
                                if(i<0)
                                    sb.append(", ");
                                sb.append(initialOpStrings[i]);
                            }
                            logger.fine("Loading initialOpStrings " +
                                        "["+sb.toString()+"]");
                        } else {
                            logger.fine("No initialOpStrings to load");
                        }
                    }
                } catch(ConfigurationException e) {
                    logger.log(Level.WARNING, "Getting initialOpStrings", e);
                }
                try {
                    /*
                     * Wait for all peers to be in a "ready" state. Once this
                     * method returns begin our own initialOpStrings and OARs
                     * deployment, setting the PeerInfo state to
                     * LOADING_INITIAL_DEPLOYMENTS.
                     */
                    waitForPeers();
                    provisionMonitorPeer.myPeerInfo.
                        setInitialDeploymentLoadState(
                            PeerInfo.LOADING_INITIAL_DEPLOYMENTS);

                    /* Load initialOpStrings */
                    for (String initialOpString : initialOpStrings) {
                        URL opstringURL;
                        try {
                            if (initialOpString.startsWith("http:"))
                                opstringURL = new URL(initialOpString);
                            else
                                opstringURL =
                                    new File(initialOpString).toURI().toURL();
                            Map errorMap = deploy(opstringURL, null);
                            dumpOpStringError(errorMap);
                        } catch (Throwable t) {
                            logger.log(Level.WARNING,
                                       "Loading OperationalString : " +
                                       initialOpString,
                                       t);
                        }
                    }

                } finally {
                    provisionMonitorPeer.myPeerInfo.
                        setInitialDeploymentLoadState(
                            PeerInfo.LOADED_INITIAL_DEPLOYMENTS);
                }
            }
        }

        /**
         * Wait until peers are ready.
         *
         * if a peer's initialDeploymentState is LOADING_INITIAL_DEPLOYMENTS,
         * wait until the LOADED_INITIAL_DEPLOYMENTS state is set. If a peer
         * has not loaded (INITIAL_DEPLOYMENTS_PENDING) this is fine as well.
         * Once we find that all peers are ready, return
         */
        void waitForPeers() {
            PeerInfo[] peers = provisionMonitorPeer.getBackupInfo();
            long t0 = System.currentTimeMillis();
            if(peerLogger.isLoggable(Level.FINE))
                peerLogger.fine("Number of peers to wait on ["+peers.length+"]");
            if(peers.length==0)
                return;
            boolean peersReady = false;
            while(!peersReady) {
                int numPeersReady=0;
                StringBuffer b = new StringBuffer();
                b.append("ProvisionMonitor Peer verification\n");
                for(int i = 0; i < peers.length; i++) {
                    if(i>0)
                        b.append("\n");
                    try {
                        ProvisionMonitor peer = peers[i].getService();
                        PeerInfo peerInfo = peer.getPeerInfo();
                        int state = peerInfo.getInitialDeploymentLoadState();
                        b.append("Peer at ");
                        b.append(peerInfo.getAddress());
                        b.append(", " + "state=");
                        b.append(getStateName(state));
                        switch(state) {
                            case PeerInfo.INITIAL_DEPLOYMENTS_PENDING:
                                numPeersReady++;
                                break;
                            case PeerInfo.LOADED_INITIAL_DEPLOYMENTS:
                                numPeersReady++;
                                break;
                            case PeerInfo.LOADING_INITIAL_DEPLOYMENTS:
                                break;
                        }
                    } catch(RemoteException e) {
                        b.append("Peer [" + 0 + "] exception " + "[");
                        b.append(e.getMessage());
                        b.append("], continue");
                        if(peerLogger.isLoggable(Level.FINEST))
                            peerLogger.log(Level.FINEST, "Getting PeerInfo", e);
                        numPeersReady++;
                    }

                }
                if(peerLogger.isLoggable(Level.FINE))
                    peerLogger.fine(b.toString());
                b.delete(0, b.length());
                if(numPeersReady==peers.length) {
                    peersReady = true;
                } else {
                    try {
                        Thread.sleep(1000);
                    } catch(InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
            long t1 = System.currentTimeMillis();
            if(peerLogger.isLoggable(Level.FINER))
                peerLogger.finer("Peer state resolution took "+(t1-t0)+" millis");
        }

        private String getStateName(int state) {
            String name;
            switch(state) {
                case PeerInfo.INITIAL_DEPLOYMENTS_PENDING:
                    name = "INITIAL_DEPLOYMENTS_PENDING";
                    break;
                case PeerInfo.LOADED_INITIAL_DEPLOYMENTS:
                    name = "LOADED_INITIAL_DEPLOYMENTS";
                    break;
                default:
                    name = "LOADING_INITIAL_DEPLOYMENTS";
            }
            return(name);

        }
    } /* end InitialOpStringLoadTask */

    /**
     * Scheduled Task which will control deployment scheduling
     */
    class DeploymentTask extends TimerTask {
        int repeats;
        OperationalString loadedOpString;
        OpStringManager opMgr;

        DeploymentTask(OpStringManager opMgr) {
            this.opMgr = opMgr;
        }

        /**
         * The action to be performed by this timer task.
         */
        public void run() {
            if(logger.isLoggable(Level.FINEST))
                logger.finest("Deploy ["+opMgr.getName()+"]");
            opMgr.addDeploymentDate(new Date(System.currentTimeMillis()));
            ServiceElementManager[] mgrs = opMgr.getServiceElementManagers();
            for (ServiceElementManager mgr : mgrs) {
                try {
                    int alreadyRunning = mgr.startManager(null);
                    if (alreadyRunning > 0) {
                        opMgr.updateServiceElements(new ServiceElement[]{
                            mgr.getServiceElement()});
                    }
                } catch (Exception e) {
                    logger.log(Level.WARNING,
                               "Starting ServiceElementManager",
                               e);
                }
            }
            opMgr.setDeploymentStatus(OperationalString.DEPLOYED);
            loadedOpString = opMgr.doGetOperationalString();
            Schedule schedule = loadedOpString.getSchedule();
            repeats++;
            if(schedule.getRepeatCount()!= Schedule.INDEFINITE &&
               repeats > schedule.getRepeatCount()) {
                if(logger.isLoggable(Level.FINE))
                    logger.fine("Repeat count ["+schedule.getRepeatCount()+"] "+
                                "reached for OperationalString "+
                                "["+loadedOpString.getName()+"], "+
                                "cancel DeploymentTask");
                cancel();
                UnDeploymentTask unDeploymentTask =
                    new UnDeploymentTask(opMgr, true);
                opMgr.addTask(unDeploymentTask);
                long sheduledUndeploy =
                    schedule.getDuration()+System.currentTimeMillis();
                taskTimer.schedule(unDeploymentTask, new Date(sheduledUndeploy));
            } else {
                if(schedule.getDuration()!=Schedule.INDEFINITE) {
                    UnDeploymentTask unDeploymentTask =
                        new UnDeploymentTask(opMgr, false);
                    long sheduledUndeploy =
                        schedule.getDuration()+System.currentTimeMillis();
                    taskTimer.schedule(unDeploymentTask, new Date(sheduledUndeploy));
                }
            }
        }

        public boolean cancel() {
            if(opMgr!=null)
                opMgr.removeTask(this);
            return(super.cancel());
        }
    }

    /**
     * Scheduled Task which will undeploy an OperationalString
     */
    class UnDeploymentTask extends TimerTask {
        OpStringManager opMgr;
        boolean undeploy;


        UnDeploymentTask(OpStringManager opMgr, boolean undeploy) {
            this.opMgr = opMgr;
            this.undeploy = undeploy;
            if(logger.isLoggable(Level.FINEST))
                logger.finest("Create new UnDeploymentTask for "+
                              "["+opMgr.getName()+"]");
        }
        /**
         * The action to be performed by this timer task.
         */
        public void run() {
            if(logger.isLoggable(Level.FINEST))
                logger.finest("Undeploy ["+opMgr.getName()+"]");
            if(undeploy) {
                opMgr.setDeploymentStatus(OperationalString.UNDEPLOYED);
                try {
                    if(opMgr.isActive())
                        undeploy(opMgr.getName(), true);
                } catch (OperationalStringException e) {
                    logger.log(Level.WARNING, "Undeploying OperationalString", e);
                }
            } else {
                ServiceElementManager[] mgrs = opMgr.getServiceElementManagers();
                for (ServiceElementManager mgr : mgrs)
                    mgr.stopManager(true);
                opMgr.setDeploymentStatus(OperationalString.SCHEDULED);
            }
        }

        public boolean cancel() {
            if(opMgr!=null)
                opMgr.removeTask(this);
            return(super.cancel());
        }
    }

    /**
     * The OpStringManager provides the management for an OperationalString that
     * has been deployed to the ProvisionMonitor
     */
    public class OpStringManager implements OperationalStringManager,
                                            ServerProxyTrust {
        OperationalString opString;
        /** Collection of ServiceElementmanager instances */
        List<ServiceElementManager> svcElemMgrs = new ArrayList<ServiceElementManager>();
        /** Collection of nested OpStringManager instances */
        List<OpStringManager> nestedManagers = new ArrayList<OpStringManager>();
        /** The OpStringManager parents for this OpStringManager */
        List<OpStringManager> parents = new ArrayList<OpStringManager>();
        /** Property that indicates the mode of the OpStringManager. If active is
         * true, the OpStringManager will inform it's ServiceElementmanager
         * instances to actively provision services. If active is false, the
         * OpStringManager will inform its ServiceElementManager instances to
         * keep track of the service described by it's ServiceElement object but
         * not issue provision requests */
        private Boolean active = true;
        /** The Exporter for the OperationalStringManager */
        Exporter exporter;
        /** Object supporting remote semantics required for an
         * OperationalStringManager */
        OperationalStringManager proxy;
        /** A List of scheduled TimerTasks */
        List<TimerTask> scheduledTaskList =
            Collections.synchronizedList(new ArrayList<TimerTask>());
        /** A list a deployed Dates */
        List<Date> deployDateList = 
            Collections.synchronizedList(new ArrayList<Date>());
        /** Local copy of the deployment status of the OperationalString */
        int deployStatus;
        /** ProxyPreparer for ServiceProvisionListener proxies */
        ProxyPreparer serviceProvisionListenerPreparer;
        OAR oar;

        /**
         * Create an OpStringManager, making it available to receive incoming
         * calls supporting the OperationalStringManager interface
         * 
         * @param opString The OperationalString to manage
         * @param parent The OpStringManager parent. May be null
         * @param mode Whether the OperationalStringManager is the active manager
         * @param config Configuration object
         *
         * @throws RemoteException if the OpStringManager cannot export itself
         */
        public OpStringManager(OperationalString opString,
                               OpStringManager parent,
                               boolean mode,
                               Configuration config)
        throws RemoteException {

            Exporter defaultExporter =
                new BasicJeriExporter(TcpServerEndpoint.getInstance(0),
                                      new BasicILFactory());
            config = (config==null? EmptyConfiguration.INSTANCE:config);
            try {
                exporter = ExporterConfig.getExporter(config,
                                                      CONFIG_COMPONENT,
                                                      "opStringManagerExporter",
                                                      defaultExporter);
                if(logger.isLoggable(Level.FINER))
                    logger.finer("Deployment ["+opString.getName()+"] using exporter "+exporter);
                /* Get the ProxyPreparer for ServiceProvisionListener instances */
                serviceProvisionListenerPreparer =
                    (ProxyPreparer)config.getEntry(CONFIG_COMPONENT,
                                                   "serviceProvisionListenerPreparer",
                                                   ProxyPreparer.class,
                                                   new BasicProxyPreparer());

            } catch(ConfigurationException e) {
                logger.log(Level.WARNING, "Getting opStringManager Exporter", e);
            }

            proxy = (OperationalStringManager)exporter.export(this);
            this.opString = opString;
            this.active = mode;
            if(parent != null) {
                addParent(parent);
                parent.addNested(this);
            }
            if(opString.loadedFrom()!=null &&
               opString.loadedFrom().toExternalForm().startsWith("file") &&
               opString.loadedFrom().toExternalForm().endsWith(".oar")) {
                File f = new File(opString.loadedFrom().getFile());
                try {
                    oar = new OAR(f);
                    oar.setDeployDir(f.getParent());
                } catch (Exception e) {
                    logger.log(Level.WARNING, "Could no create OAR", e);
                }
            }
        }

        /**
         * Get the OperationalStringManager remote object
         * 
         * @return The OperationalStringManager
         * remote object created during a call to the Exporter
         */
        OperationalStringManager getProxy() {
            return (proxy);
        }

        /**
         * Set the active mode. If the new mode is not equal to the old mode,
         * iterate through the Collection of ServiceElementManager instances and
         * set their mode to be equal to the OpStringManager mode
         * 
         * @param newActive the new mode
         */
        void setActive(boolean newActive) {
            synchronized(this) {
                if(active != newActive) {
                    active = newActive;
                    List<ServiceElement> list = new ArrayList<ServiceElement>();
                    ServiceElementManager[] mgrs = getServiceElementManagers();
                    for (ServiceElementManager mgr : mgrs) {
                        mgr.setActive(active);
                        list.add(mgr.getServiceElement());
                    }
                    if(logger.isLoggable(Level.FINER))
                        logger.finer("OperationalStringManager for "+
                                     "["+getProxy().toString()+"] "+
                                     "set active ["+active+"] "+
                                     "for OperationalString ["+getName()+"]");
                    if(active) {
                        ServiceElement[] sElems =
                            list.toArray(new ServiceElement[list.size()]);
                        updateServiceElements(sElems);
                    }

                    /* Trickle down effect : update all nested managers of the 
                     * new active state */
                    OpStringManager[] nestedMgrs =
                        nestedManagers.toArray(
                                        new OpStringManager[nestedManagers.size()]);
                    for (OpStringManager nestedMgr : nestedMgrs) {
                        nestedMgr.setActive(newActive);
                    }

                } else {
                    if(logger.isLoggable(Level.FINEST))
                        logger.finest("OperationalStringManager for "+
                                      "["+opString.getName()+"] already "+
                                      "has active state of ["+active+"]");
                }
            }
        }

        /**
         * Get the active property
         *
         * @return The active property
         */
        boolean isActive() {
            boolean mode ;
            //synchronized(this) {
                mode = active;
            //}
            return (mode);
        }

        /**
         * @see org.rioproject.core.OperationalStringManager#setManaging(boolean)
         */
        public void setManaging(boolean newActive)  {
            setActive(newActive);
        }
        
        /**
         * @see org.rioproject.core.OperationalStringManager#isManaging
         */
        public boolean isManaging() {
            return(isActive());
        }

        /**
         * @see org.rioproject.core.OperationalStringManager#getDeploymentDates
         */
        public Date[] getDeploymentDates() {
            return(deployDateList.toArray(new Date[deployDateList.size()]));
        }

        /**
         * Set the deployment status
         * 
         * @param status Either OperationalString.UNDEPLOYED,
         * OperationalString.SCHEDULED or OperationalString.DEPLOYED
         */
        void setDeploymentStatus(int status) {
            opString.setDeployed(status);
            deployStatus = status;
            if(deployStatus==OperationalString.UNDEPLOYED) {
                if(nestedManagers.size() > 0) {
                    OpStringManager[] nestedMgrs =
                        nestedManagers.toArray(
                                        new OpStringManager[nestedManagers.size()]);
                    for (OpStringManager nestedMgr : nestedMgrs) {
                        if (nestedMgr.getParentCount() == 1)
                            nestedMgr.setDeploymentStatus(
                                OperationalString.UNDEPLOYED);
                    }
                }

            }
        }

        /**
         * Add a deployment Date
         * 
         * @param date A Date indicating when an OperationalString gets deployed
         */
        void addDeploymentDate(Date date) {
            if(date!=null)
                deployDateList.add(date);
        }

        /**
         * Initialize all ServiceElementManager instances
         * 
         * @param mode Whether the ServiceElementManager should actively
         * manage (allocate) services. This will also set the OpStringManager active
         * Property
         * @param listener A ServiceProvisionListener that will be notified
         * of services are they are provisioned. This notification approach is
         * only valid at OpStringManager creation (deployment), when services are 
         * provisioned at OperationalString deployment time
         *
         * @return A map of reasons and corresponding exceptions from creating
         * service element manager instances. If the map has no entries there
         * are no errors
         *
         * @throws Exception if there are unrecoverable errors
         */
        Map<String, Throwable> init(boolean mode, ServiceProvisionListener listener)
        throws Exception {
            this.active = mode;
            Map<String, Throwable> map = new HashMap<String, Throwable>();
            ServiceElement[] sElems = opString.getServices();
            for (ServiceElement sElem : sElems) {
                try {
                    if (sElem.getExportBundles().length > 0) {
                        createServiceElementManager(sElem, false, listener);
                    } else {
                        String message = "Service ["+sElem.getName()+"] has no " +
                                         "declared interfaces, cannot deploy"; 
                        logger.warning(message);
                        map.put(sElem.getName(), new Exception(message));
                    }
                } catch (Exception e) {
                    Throwable cause = e.getCause();
                    if (cause == null)
                        cause = e;
                    logger.log(Level.WARNING,
                               "Creating ServiceElementManager for " +
                               "[" + sElem.getName() + "], " +
                               "deployment ["+sElem.getOperationalStringName()+"]",
                               e);
                    map.put(sElem.getName(), cause);
                    throw e;
                }
            }
            return (map);
        }

        /**
         * Start all ServiceElementManager instances
         * 
         * @param listener A ServiceProvisionListener that will be notified
         * of services are they are provisioned. This notification approach is
         * only valid at OpStringManager creation (deployment), when services are 
         * provisioned at OperationalString deployment time
         */
        void startManager(ServiceProvisionListener listener) {
            startManager(listener, new HashMap());
        }

        /**
         * Start all ServiceElementManager instances
         *
         * @param listener A ServiceProvisionListener that will be notified
         * of services are they are provisioned. This notification approach is
         * only valid at OpStringManager creation (deployment), when services are
         * provisioned at OperationalString deployment time
         * @param knownInstanceMap Known ServiceBeanInstance objects.
         */
        void startManager(ServiceProvisionListener listener,
                          Map knownInstanceMap) {
            boolean scheduled = false;
            Schedule schedule = opString.getSchedule();
            Date startDate = schedule.getStartDate();
            long now = System.currentTimeMillis();
            long delay = startDate.getTime()-now;
            if(logger.isLoggable(Level.FINEST))
                logger.finest("OperationalString ["+getName()+"] "+
                              "Start Date=["+startDate.toString()+"], "+
                              "Delay ["+delay+"]");
            if(delay>0 || schedule.getDuration()>0) {
                DeploymentTask deploymentTask = new DeploymentTask(this);
                addTask(deploymentTask);
                if(schedule.getDuration()>0)
                    taskTimer.scheduleAtFixedRate(deploymentTask,
                                                  startDate,
                                                  (schedule.getDuration()+
                                                   schedule.getRepeatInterval()));
                else
                    taskTimer.schedule(deploymentTask, startDate);
                scheduled = true;
                setDeploymentStatus(OperationalString.SCHEDULED);
            }

            if(!scheduled) {
                addDeploymentDate(new Date(System.currentTimeMillis()));
                setDeploymentStatus(OperationalString.DEPLOYED);
                ServiceElementManager[] mgrs = getServiceElementManagers();
                for (ServiceElementManager mgr : mgrs) {
                    ServiceElement elem = mgr.getServiceElement();
                    ServiceBeanInstance[] instances =
                        (ServiceBeanInstance[]) knownInstanceMap.get(elem);
                    try {
                        int alreadyRunning = mgr.startManager(listener,
                                                              instances);
                        if (alreadyRunning > 0) {
                            updateServiceElements(new ServiceElement[]{
                                mgr.getServiceElement()});
                        }
                    } catch (Exception e) {
                        logger.log(Level.WARNING,
                                   "Starting ServiceElementManager",
                                   e);
                    }
                }
            }
        }

        /**
         * Create a specific ServiceElementManager, add it to the Collection of
         * ServiceElementManager instances, and start the manager
         * 
         * @param sElem The ServiceElement the ServiceElementManager will
         * manage
         * @param start Whether to start the ServiceElementManager at creation
         * @param listener A ServiceProvisionListener that will be notified
         * of services are they are provisioned. This notification approach is
         * only valid when the ServiceElementManager is created, and services are
         * provisioned at OperationalString deployment time
         *
         * @throws Exception if the ServiceElementManager cannot be created
         */
        void createServiceElementManager(ServiceElement sElem,
                                         boolean start,
                                         ServiceProvisionListener listener)
        throws Exception {
            ServiceElementManager svcElemMgr =
                new ServiceElementManager(sElem,
                                          proxy,
                                          provisioner,
                                          getUuid(),
                                          isActive(),
                                          config);
            /* Set event attributes */
            svcElemMgr.setEventPool(monitorEventPool);
            svcElemMgr.setEventSource(getEventProxy());
            svcElemMgr.setEventHandler(monitorEventHandler);
            svcElemMgrs.add(svcElemMgr);

            if(start) {
                int alreadyRunning = svcElemMgr.startManager(listener);
                if(alreadyRunning>0) {
                    updateServiceElements(new ServiceElement[] {sElem});
                }
            }
        }

        /**
         * @see org.rioproject.core.OperationalStringManager#update(OperationalString)
         */
        public Map<String, Throwable> update(OperationalString newOpString)
            throws OperationalStringException, RemoteException {

            if(!isActive()) {
                OperationalStringManager primary = getPrimary(getName());
                if(primary==null) {
                    logger.warning("Primary testManager not located, " +
                                   "force state to active for ["+getName()+"]");
                    setActive(true);
                } else {
                    logger.info("Forwarding update request to primary testManager " +
                                "for ["+getName()+"]");
                    return(primary.update(newOpString));
                }
            }
            try {
                Resolver resolver = ResolverHelper.getInstance();
                OpStringUtil.checkCodebase(newOpString,
                                           System.getProperty(Constants.CODESERVER),
                                           resolver);
            } catch (Exception e) {
                throw new OperationalStringException("Checking codebase for " +
                                                     "["+newOpString.getName()+"]",
                                                     e);
            }
            Map<String, Throwable> map = doUpdateOperationalString(newOpString);
            ProvisionMonitorEvent event =
                new ProvisionMonitorEvent(
                                  getEventProxy(),
                                  ProvisionMonitorEvent.Action.OPSTRING_UPDATED,
                                  doGetOperationalString());
            processEvent(event);
            return(map);
        }

        /*
         * Do the OperationalString update
         * 
         * @param newOpString
         * 
         * @return Map of any errors
         */
        Map<String, Throwable> doUpdateOperationalString(OperationalString newOpString) {
            if(newOpString == null)
                throw new IllegalArgumentException("OperationalString cannot be null");

            if(logger.isLoggable(Level.INFO)) {
                logger.info("Updating "+newOpString.getName()+" deployment");
            }
            Map<String, Throwable> map = new HashMap<String, Throwable>();
            ServiceElement[] sElems = newOpString.getServices();
            List<ServiceElementManager> notRefreshed =
                new ArrayList<ServiceElementManager>(svcElemMgrs);
            /* Refresh ServiceElementManagers */
            for (ServiceElement sElem : sElems) {
                try {
                    ServiceElementManager svcElemMgr =
                        getServiceElementManager(sElem);
                    if (svcElemMgr == null) {
                        createServiceElementManager(sElem, true, null);
                    } else {
                        svcElemMgr.setServiceElement(sElem);
                        notRefreshed.remove(svcElemMgr);
                    }
                } catch (Exception e) {
                    map.put(sElem.getName(), e);
                    logger.log(Level.WARNING,
                               "Refreshing ServiceElementManagers", e);
                }
            }
            /*
             * Process nested Operational Strings. If a nested
             * OperationalString does not exist it will be created.
             * If it does exist check the number of parents, if its only refernced 
             * by this testManager update it
             */
            OperationalString[] nested =
                newOpString.getNestedOperationalStrings();
            for(int i = 0; i < nested.length; i++) {
                if(!opStringExists(nested[i].getName())) {
                    try {
                        addOperationalString(nested[i], map, this, null, null);
                    } catch(Exception e) {
                        Throwable cause = e.getCause();
                        if(cause == null)
                            cause = e;
                        map.put(sElems[i].getName(), cause);
                        logger.log(Level.WARNING,
                                   "Adding nested OperationalString " +
                                   "["+nested[i].getName()+"]",
                                   e);
                    }
                } else {
                    OpStringManager nestedMgr =
                        getOpStringManager(nested[i].getName());
                    if(nestedMgr.getParentCount()==1 &&
                       nestedMgr.parents.contains(this)) {
                        Map <String, Throwable>nestedMap =
                            nestedMgr.doUpdateOperationalString(nested[i]);
                        map.putAll(nestedMap);
                    }
                }
            }
            /*
             * If there are ServiceElementManagers that are not needed
             * remove them
             */
            for(ServiceElementManager mgr : notRefreshed) {
                mgr.stopManager(true);
                svcElemMgrs.remove(mgr);
            }
            opString = newOpString;
            stateChanged(this, false);
            if(isActive())
                updateServiceElements(sElems);
            return (map);
        }

        /**
         * Verify all services are being monitored by iterating through the
         * Collection of ServiceElementManager instances and invoking each
         * instance's verify() method
         * 
         * @param listener A ServiceProvisionListener that will be notified
         * of services if they are provisioned. 
         */
        void verify(ServiceProvisionListener listener) {
            for(ServiceElementManager mgr : svcElemMgrs) {
                mgr.verify(listener);
            }
            for(OpStringManager nestedMgr : nestedManagers) {
                nestedMgr.verify(listener);
            }
        }


        /**
         * Update ServiceElement instances to ServiceBeanInstantiators which are
         * hosting the ServiceElement instance(s). If the OperationalStringManager 
         * is not active. do not perform this task 
         * 
         * @param elements Array of ServiceElement instances to update
         */
        void updateServiceElements(ServiceElement[] elements) {
            if(!isActive())
                return;
            ServiceResource[] resources =
                provisioner.getServiceResourceSelector().getServiceResources();
            Map<InstantiatorResource, List<ServiceElement>> map =
                new HashMap<InstantiatorResource, List<ServiceElement>>();
            for (ServiceResource resource : resources) {
                InstantiatorResource ir =
                    (InstantiatorResource) resource.getResource();
                for (ServiceElement element : elements) {
                    int count = ir.getServiceElementCount(element);
                    if (logger.isLoggable(Level.FINEST))
                        logger.log(Level.FINEST,
                                   ir.getName() + " at " +
                                   "[" + ir.getHostAddress() + "] has " +
                                   "[" + count + "] of " +
                                   "[" + element.getName() + "]");
                    if (count > 0) {
                        List<ServiceElement> list = map.get(ir);
                        if (list == null)
                            list = new ArrayList<ServiceElement>();
                        list.add(element);
                        map.put(ir, list);
                    }
                }
            }

            for (Map.Entry<InstantiatorResource, List<ServiceElement>> entry :
                 map.entrySet()) {
                InstantiatorResource ir = entry.getKey();
                List<ServiceElement> list = entry.getValue();

                ServiceElement[] elems =
                    list.toArray(new ServiceElement[list.size()]);
                try {
                    ServiceBeanInstantiator sbi = ir.getInstantiator();
                    if (logger.isLoggable(Level.FINEST))
                        logger.log(Level.FINEST,
                                   "Update " + ir.getName() + " " +
                                   "at [" + ir.getHostAddress() + "] " +
                                   "with [" + elems.length + "] elements");
                    sbi.update(elems, getProxy());
                } catch (RemoteException e) {
                    if (logger.isLoggable(Level.FINEST)) {
                        logger.log(Level.FINEST,
                                   "Updating ServiceElement for " +
                                   ir.getName() + " at [" +
                                   ir.getHostAddress() + "]",
                                   e);
                    } else {
                        logger.log(Level.INFO,
                                   e.getClass().getName()+": "+e.getLocalizedMessage()+" "+
                                   "Updating ServiceElement for " +
                                   ir.getName() + " at [" +
                                   ir.getHostAddress() + "]");
                    }
                }
            }
        }

        /**
         * Shutdown the monitoring for this OperationalString
         *
         * @param killServices If true destroy the services being managed
         *
         * @return Array of terminated OperationalStrings. If the
         * managed OperationalString has no nested OperationalStrings, return
         * just the OperationalString that was being managed. If nested
         * OperationalStrings were also terminated, return those as well. A
         * new array is allocated each time.
         */
        OperationalString[] terminate(boolean killServices) {
            List<OperationalString> terminated =
                new ArrayList<OperationalString>();
            terminated.add(doGetOperationalString());
            /* Cancel all scheduled Tasks */
            TimerTask[] tasks = getTasks();
            for (TimerTask task : tasks)
                task.cancel();

            /* Unexport the testManager */
            try {
                exporter.unexport(true);
            } catch(IllegalStateException e) {
                if(logger.isLoggable(Level.FINE))
                    logger.log(Level.FINE,
                               "OperationalStringManager not unexported");
            }
            /* Remove ourselves from the collection */
            synchronized(opStringManagers) {
                opStringManagers.remove(this);
            }

            /* Stop all ServiceElementManager instances */
            for(ServiceElementManager mgr :  svcElemMgrs) {
                mgr.stopManager(killServices);
            }
            /* Adjust parent/nested relationships */
            if(parents.size()>0) {
                for(OpStringManager parent : parents) {
                    parent.removeNested(this);
                }
                parents.clear();
            }

            OpStringManager[] nestedMgrs =
                nestedManagers.toArray(
                    new OpStringManager[nestedManagers.size()]);

            for (OpStringManager nestedMgr : nestedMgrs) {
                /* If the nested OpStringManager has only 1 parent, then
                 * terminate (undeploy) that OpStringManager as well */
                if (nestedMgr.getParentCount() == 1) {
                    terminated.add(nestedMgr.doGetOperationalString());
                    nestedMgr.terminate(killServices);
                } else {
                    nestedMgr.removeParent(this);
                }
            }
            if(logger.isLoggable(Level.FINE))
                logger.fine("OpStringManager ["+getName()+"] terminated");
            return(terminated.toArray(new OperationalString[terminated.size()]));
        }

        /**
         * Get the name of this Operational String
         *
         * @return The name of this Operational String
         */
        String getName() {
            return(opString.getName());
        }

        /*
         * @see org.rioproject.core.OperationalStringManager#addServiceElement
         */
        public void addServiceElement(ServiceElement sElem)
        throws OperationalStringException {
            addServiceElement(sElem, null);
        }

        /*
         * @see org.rioproject.core.OperationalStringManager#addServiceElement
         */
        public void addServiceElement(ServiceElement sElem,
                                      ServiceProvisionListener listener)
        throws OperationalStringException {
            if(sElem == null)
                throw new IllegalArgumentException("ServiceElement is null");
            if(sElem.getOperationalStringName()==null)
                throw new IllegalArgumentException("ServiceElement must have an " +
                                                   "OperationalString name");
            if(!sElem.getOperationalStringName().equals(opString.getName()))
                throw new IllegalArgumentException("ServiceElement has wrong " +
                                                   "OperationalString name. " +
                                                   "Provided " +
                                                   "["+sElem.getOperationalStringName()+"], " +
                                                   "should be " +
                                                   "["+opString.getName()+"]");
            if(!isActive())
                throw new OperationalStringException(
                        "not the primary OperationalStringManager");
            try {
                doAddServiceElement(sElem, listener);
                stateChanged(this, false);
                ProvisionMonitorEvent event =
                    new ProvisionMonitorEvent(
                                        getEventProxy(),
                                        ProvisionMonitorEvent.Action.SERVICE_ELEMENT_ADDED,
                                        sElem);
                processEvent(event);

            } catch(Throwable t) {
                throw new OperationalStringException("Adding ServiceElement", t);
            }
            if(logger.isLoggable(Level.FINE))
                logger.log(Level.FINE,
                           "Added service ["+
                           sElem.getOperationalStringName()+"/"+sElem.getName()+"]");
        }

        /*
         * Do the ServiceElement add
         */
        void doAddServiceElement(ServiceElement sElem,
                                 ServiceProvisionListener listener) throws Exception {
            if(sElem.getExportBundles().length>0) {
                
                /*File pomFile = null;
                if(oar!=null) {
                    File dir = new File(oar.getDeployDir());
                    pomFile = OARUtil.find("pom.xml", dir);
                }*/
                Resolver resolver = ResolverHelper.getInstance();
                OpStringUtil.checkCodebase(sElem,
                                           System.getProperty(Constants.CODESERVER),
                                           resolver);
                createServiceElementManager(sElem, true, listener);
            } else {
                throw new OperationalStringException("Interfaces are null");
            }
            stateChanged(this, false);
        }

        /*
         * @see org.rioproject.core.OperationalStringManager#removeServiceElement
         */
        public void removeServiceElement(ServiceElement sElem, boolean destroy)
        throws OperationalStringException {
            if(sElem == null)
                throw new IllegalArgumentException("ServiceElement is null");
            if(!isActive())
                throw new OperationalStringException(
                        "not the primary OperationalStringManager");
            try {
                doRemoveServiceElement(sElem, destroy);
                ProvisionMonitorEvent event =
                    new ProvisionMonitorEvent(
                                      getEventProxy(),
                                      ProvisionMonitorEvent.Action.SERVICE_ELEMENT_REMOVED,
                                      sElem);
                processEvent(event);
            } catch(Throwable t) {
                if(t instanceof OperationalStringException)
                    throw (OperationalStringException)t;
                throw new OperationalStringException("Removing ServiceElement", t);
            }
            if(logger.isLoggable(Level.FINE))
                logger.log(Level.FINE,
                           "Removed service ["+
                           sElem.getOperationalStringName()+"/"+sElem.getName()+"]");
        }

        /*
         * Do the ServiceElement removal
         */
        void doRemoveServiceElement(ServiceElement sElem, boolean destroy)
        throws OperationalStringException {

            ServiceElementManager svcElemMgr = getServiceElementManager(sElem);
            if(svcElemMgr == null)
                throw new OperationalStringException(
                    "OperationalStringManager for ["+opString.getName()+"], " +
                    "is not managing service "+
                    "["+sElem.getOperationalStringName()+"/"+sElem.getName()+"]",
                    false);
            svcElemMgr.stopManager(destroy);
            if(!svcElemMgrs.remove(svcElemMgr))
                logger.warning("UNABLE to remove ServiceElementManager for " +
                               "["+sElem.getOperationalStringName()+
                               "/"+sElem.getName()+"]");
            stateChanged(this, false);
        }

        /*
         * @see org.rioproject.core.OperationalStringManager#update
         */
        public void update(ServiceElement sElem)
        throws OperationalStringException {
            if(sElem == null)
                throw new IllegalArgumentException("ServiceElement is null");
            if(!isActive())
                throw new OperationalStringException(
                        "not the primary OperationalStringManager");
            try {
                doUpdateServiceElement(sElem);
                ProvisionMonitorEvent.Action action = ProvisionMonitorEvent.Action.SERVICE_ELEMENT_UPDATED;
                ProvisionMonitorEvent event =
                    new ProvisionMonitorEvent(getEventProxy(),
                                              action,
                                              sElem);
                processEvent(event);
            } catch(Throwable t) {
                logger.log(Level.WARNING, "Updating ServiceElement "+
                                          "["+sElem.getName()+"]", t);
                throw new OperationalStringException("Updating ServiceElement "+
                                                     "["+sElem.getName()+"]", t);
            }
        }

        /*
         * Do the ServiceElement update
         */
        void doUpdateServiceElement(ServiceElement sElem) throws Exception {

            ServiceElementManager svcElemMgr = getServiceElementManager(sElem);
            if(svcElemMgr == null) {
                doAddServiceElement(sElem, null);
            } else {
                svcElemMgr.setServiceElement(sElem);
                svcElemMgr.verify(null);
                stateChanged(this, false);
                updateServiceElements(new ServiceElement[] {sElem});
            }
        }

        /*
         * @see org.rioproject.core.OperationalStringManager#relocate
         */
        public void relocate(ServiceBeanInstance instance,
                             ServiceProvisionListener listener,
                             Uuid uuid)
        throws OperationalStringException, RemoteException {
            if(instance == null)
                throw new IllegalArgumentException("instance is null");
            if(!isActive())
                throw new OperationalStringException(
                    "not the primary OperationalStringManager");
            if(listener != null)
                listener =
                    (ServiceProvisionListener)serviceProvisionListenerPreparer.
                        prepareProxy(listener);
            try {
                ServiceElementManager svcElemMgr =
                    getServiceElementManager(instance);
                if(svcElemMgr == null)
                    throw new OperationalStringException(
                        "Unmanaged "+
                        "ServiceBeanInstance "+
                        "["+instance.getServiceBeanConfig().getName()+"], "+
                        "["+instance.toString()+"]", false);
                if(svcElemMgr.getServiceElement().getProvisionType() !=
                   ServiceElement.ProvisionType.DYNAMIC)
                    throw new OperationalStringException(
                        "Service must be dynamic "+
                        "to be relocated");
                svcElemMgr.relocate(instance,
                                    listener,
                                    uuid);
            } catch(Throwable t) {                
                logger.warning("Relocating ServiceBeanInstance ["+
                               t.getClass().getName()+":"+t.getMessage()+"]");
                if(t instanceof OperationalStringException)
                    throw (OperationalStringException)t;

                throw new OperationalStringException("Relocating "+
                                                     "ServiceBeanInstance",
                                                     t);
            }
        }

        /*
         * @see org.rioproject.core.OperationalStringManager#update
         */
        public void update(ServiceBeanInstance instance)
        throws OperationalStringException {
            if(instance == null)
                throw new IllegalArgumentException("instance is null");
            try {
                ServiceElement sElem = doUpdateServiceBeanInstance(instance);
                ProvisionMonitorEvent event =
                    new ProvisionMonitorEvent(getEventProxy(),
                                              sElem.getOperationalStringName(),
                                              instance);
                processEvent(event);
            } catch(Throwable t) {
                logger.warning("Updating ServiceBeanInstance ["+
                               "["+instance.getServiceBeanConfig().getName()+"] ["+
                               t.getClass().getName()+":"+t.getMessage()+"]");
                throw new OperationalStringException("Updating ServiceBeanInstance",
                                                     t);
            }
        }

        /*
         * Do the ServiceBeanInstance update
         */
        ServiceElement doUpdateServiceBeanInstance(ServiceBeanInstance instance)
        throws OperationalStringException {

            ServiceElementManager svcElemMgr = getServiceElementManager(instance);
            if(svcElemMgr == null)
                throw new OperationalStringException("Unmanaged ServiceBeanInstance "+
                                                     "["+instance.toString()+"]",
                                                     false);
            svcElemMgr.update(instance);
            return(svcElemMgr.getServiceElement());
        }

        /*
         * @see org.rioproject.core.OperationalStringManager#increment
         */
        public synchronized void increment(ServiceElement sElem,
                                           boolean permanent,
                                           ServiceProvisionListener listener)
        throws OperationalStringException, RemoteException {
            if(sElem == null)
                throw new IllegalArgumentException("ServiceElement is null");
            if(!isActive())
                throw new OperationalStringException(
                        "not the primary OperationalStringManager");
            if(listener!=null)
                listener =
                    (ServiceProvisionListener)serviceProvisionListenerPreparer.
                                                              prepareProxy(listener);
            try {
                ServiceElementManager svcElemMgr = getServiceElementManager(sElem);
                if(svcElemMgr == null)
                    throw new OperationalStringException(
                            "Unmanaged ServiceElement ["+sElem.getName()+"]",
                            false);
                ServiceElement changed = svcElemMgr.increment(permanent, listener);
                if(changed==null)
                    return;
                stateChanged(this, false);
                updateServiceElements(new ServiceElement[] {changed});
                ProvisionMonitorEvent event =
                    new ProvisionMonitorEvent(
                                     getEventProxy(),
                                     ProvisionMonitorEvent.Action.SERVICE_BEAN_INCREMENTED,
                                     changed);
                processEvent(event);
            } catch(Throwable t) {
                logger.warning("Incrementing ServiceElement " +
                               "["+sElem.getName()+"] ["+
                               t.getClass().getName()+":"+t.getMessage()+"]");
                throw new OperationalStringException("Incrementing " +
                                                     "ServiceElement "+
                                                     "["+sElem.getName()+"]",
                                                     t);
            }
        }

        /*
         * @see org.rioproject.core.OperationalStringManager#getPendingCount
         */
        public int getPendingCount(ServiceElement sElem) {
            if(sElem == null)
                throw new IllegalArgumentException("ServiceElement is null");
            int numPending = -1;
            ServiceElementManager svcElemMgr = getServiceElementManager(sElem);
            if(svcElemMgr != null)
                numPending = svcElemMgr.getPendingCount();
            return(numPending);
        }

        /*
         * @see org.rioproject.core.OperationalStringManager#trim
         */
        public int trim(ServiceElement sElem, int trimUp)
        throws OperationalStringException {
            if(sElem == null)
                throw new IllegalArgumentException("ServiceElement is null");
            if(!isActive())
                throw new OperationalStringException(
                        "not the primary OperationalStringManager");
            if(sElem.getProvisionType() != ServiceElement.ProvisionType.DYNAMIC)
                return(-1);
            int numTrimmed;
            try {
                ServiceElementManager svcElemMgr =
                    getServiceElementManager(sElem);
                if(svcElemMgr == null)
                    throw new OperationalStringException("Unmanaged " +
                                                         "ServiceElement "+
                                                         "["+sElem.getName()+"]",
                                                         false);
                numTrimmed = svcElemMgr.trim(trimUp);
                if(numTrimmed>0) {
                    stateChanged(this, false);
                    ServiceElement updatedElement = svcElemMgr.getServiceElement();
                    updateServiceElements(new ServiceElement[] {updatedElement});
                    ProvisionMonitorEvent event =
                        new ProvisionMonitorEvent(
                                     getEventProxy(),
                                     ProvisionMonitorEvent.Action.SERVICE_BEAN_DECREMENTED,
                                     updatedElement);
                    processEvent(event);
                }
                return (numTrimmed);
            } catch(Throwable t) {
                logger.warning(
                           "Trimming ServiceElement ["+sElem.getName()+"] "+
                               t.getClass().getName()+":"+t.getMessage()+"]");
                throw new OperationalStringException("Trimming " +
                                                     "ServiceElement "+
                                                     "["+sElem.getName()+"]",
                                                     t);
            }
        }

        /*
         * @see org.rioproject.core.OperationalStringManager#decrement
         */
        public void decrement(ServiceBeanInstance instance,
                              boolean recommended,
                              boolean destroy) throws OperationalStringException {
            if(instance == null)
                throw new IllegalArgumentException("instance is null");
            if(!isActive())
                throw new OperationalStringException(
                        "not the primary OperationalStringManager");
            ServiceElementManager svcElemMgr =
                getServiceElementManager(instance);
            if(svcElemMgr == null)
                throw new OperationalStringException("Unmanaged " +
                                                     "ServiceBeanInstance "+
                                                     "["+instance.toString()+"]",
                                                     false);
            ServiceElement sElem = svcElemMgr.decrement(instance,
                                                        recommended,
                                                        destroy);
            stateChanged(this, false);
            updateServiceElements(new ServiceElement[] {sElem});
            ProvisionMonitorEvent event =
                new ProvisionMonitorEvent(getEventProxy(),
                                          ProvisionMonitorEvent.Action.SERVICE_BEAN_DECREMENTED,
                                          sElem.getOperationalStringName(),
                                          sElem,
                                          instance);
            processEvent(event);
        }

        /**
         * Determine of this OpStringManager has any parents
         * 
         * @return If true, this OpStringManager is top-level (has no
         * parents)
         */
        boolean isTopLevel() {
            return parents.size() == 0;
        }

        /*
         * @see org.rioproject.core.OperationalStringManager#getOperationalString
         */
        public OperationalString getOperationalString() {
            return(doGetOperationalString());
        }

        /*
         * @see org.rioproject.core.OperationalStringManager#getServiceElement
         */
        public ServiceElement getServiceElement(Object proxy) {
            if(proxy == null)
                throw new IllegalArgumentException("proxy is null");
            ServiceElementManager mgr = null;
            try {
                mgr = getServiceElementManager(proxy);
            } catch(IOException e) {
                if(logger.isLoggable(Level.FINEST))
                logger.log(Level.FINEST,
                           "Getting ServiceElementManager for proxy",
                           e);
            }
            if(mgr==null)
                logger.warning("No ServiceElementManager found for proxy "+proxy);
            ServiceElement element = null;
            if(mgr != null) {
                element = mgr.getServiceElement();                
            }
            return(element);
        }

        /*
         * @see org.rioproject.core.OperationalStringManager#getServiceBeanInstances
         */
        public ServiceBeanInstance[]
            getServiceBeanInstances(ServiceElement sElem)
            throws OperationalStringException {

            if(sElem == null)
                throw new IllegalArgumentException("ServiceElement is null");
            try {
                ServiceElementManager mgr = getServiceElementManager(sElem);
                if(mgr == null)
                    throw new OperationalStringException("Unmanaged " +
                                                         "ServiceElement "+
                                                         "["+sElem.getName()+"]",
                                                         false);
                return(mgr.getServiceBeanInstances());
            } catch(Throwable t) {
                logger.log(Level.WARNING,
                           "Getting ServiceBeanInstances for ServiceElement "+
                           "["+sElem.getName()+"]",
                           t);
                if(t instanceof OperationalStringException)
                    throw (OperationalStringException)t;
                else
                    throw new OperationalStringException("Getting " +
                                                         "ServiceBeanInstances "+
                                                         "for ServiceElement "+
                                                         "["+sElem.getName()+"]",
                                                         t);
            }
        }

        /*
         * @see org.rioproject.core.OperationalStringManager#getServiceElement
         */
        public ServiceElement getServiceElement(String[] interfaces,
                                                String name) {
            if(interfaces == null)
                throw new IllegalArgumentException("interfaces cannot be null");
            for(ServiceElementManager mgr : svcElemMgrs) {
                ServiceElement sElem = mgr.getServiceElement();
                boolean found = false;
                ClassBundle[] exports = sElem.getExportBundles();
                for (ClassBundle export : exports) {
                    boolean matched = false;
                    for (String anInterface : interfaces) {
                        if (export.getClassName().equals(anInterface))
                            matched = true;
                    }
                    if (matched) {
                        found = true;
                        break;
                    }
                }

                if(found) {
                    if(name == null)
                        return (sElem);
                    if(name.equals(sElem.getName()))
                        return (sElem);
                }
            }
            return (null);
        }

        /**
         * Get the OperationalString the OpStringManager is managing
         * 
         * @return The OperationalString the OpStringManager is managing
         */
        OperationalString doGetOperationalString() {
            OpString opstr = new OpString(opString.getName(),
                                          opString.loadedFrom());
            opstr.setDeployed(deployStatus);
            opstr.setSchedule(opString.getSchedule());
            for(ServiceElementManager mgr : svcElemMgrs) {
                opstr.addService(mgr.getServiceElement());
            }
            for(OpStringManager nestedMgr : nestedManagers) {
                opstr.addOperationalString(nestedMgr.doGetOperationalString());
            }
            opString = opstr;
            return (opString);
        }

        /**
         * @see org.rioproject.core.OperationalStringManager#redeploy(ServiceElement,
         * ServiceBeanInstance, boolean, boolean, long, ServiceProvisionListener)
         */
        public void redeploy(ServiceElement sElem,
                             ServiceBeanInstance instance,
                             boolean clean,
                             long delay,
                             ServiceProvisionListener listener)
        throws OperationalStringException {
            redeploy(sElem, instance, clean, true, delay, listener);
        }

        /**
         * @see org.rioproject.core.OperationalStringManager#redeploy(ServiceElement,
         * ServiceBeanInstance, boolean, long, ServiceProvisionListener)
         */
        public void redeploy(ServiceElement sElem,
                             ServiceBeanInstance instance,
                             boolean clean,
                             boolean sticky,
                             long delay,
                             ServiceProvisionListener listener)
        throws OperationalStringException {

            if(!isActive())
                throw new OperationalStringException(
                        "not the primary OperationalStringManager");
            if(listener!=null) {
                try {
                    listener =
                        (ServiceProvisionListener)serviceProvisionListenerPreparer.
                                                                  prepareProxy(listener);
                } catch (RemoteException e) {
                    Throwable cause = ThrowableUtil.getRootCause(e);
                    if(logger.isLoggable(Level.FINER))
                        logger.log(Level.FINER,
                                   "Notifying ServiceProvsionListener of " +
                                   "redeployment, continue with redeployment. "+
                                   cause.getClass().getName()+": "+
                                   cause.getLocalizedMessage());
                }
            }

            if(delay>0) {
                doScheduleReploymentTask(delay,
                                         sElem,     /* ServiceElement */
                                         instance,  /* ServiceBeanInstance */
                                         clean,
                                         sticky,
                                         listener);
            } else {
                if(sElem==null && instance==null)
                    doRedeploy(clean, sticky, listener);
                else
                    doRedeploy(sElem, instance, clean, sticky, listener);
            }
        }

        /**
         * @see org.rioproject.core.OperationalStringManager#getServiceStatements
         */
        public ServiceStatement[] getServiceStatements()  {
            List<ServiceStatement> statements = new ArrayList<ServiceStatement>();
            ServiceElementManager[] mgrs = getServiceElementManagers();
            for(ServiceElementManager mgr : mgrs) {
                statements.add(mgr.getServiceStatement());
            }
            return statements.toArray(new ServiceStatement[statements.size()]);
        }


        /**
         * @see org.rioproject.core.OperationalStringManager#getDeploymentMap
         */
        public DeploymentMap getDeploymentMap() {
            Map<ServiceElement, List<DeployedService>> map =
                new HashMap<ServiceElement, List<DeployedService>>();
            ServiceElementManager[] mgrs = getServiceElementManagers();
            for(ServiceElementManager mgr : mgrs) {
                map.put(mgr.getServiceElement(),
                         mgr.getServiceDeploymentList());
            }
            return new DeploymentMap(map);
        }

        /*
         * Redeploy the OperationalString  
         */
        void doRedeploy(boolean clean,
                        boolean sticky,
                        ServiceProvisionListener listener)
            throws OperationalStringException {

            if(!isActive())
                throw new OperationalStringException(
                        "not the primary OperationalStringManager");
            ServiceElementManager[] mgrs =
                svcElemMgrs.toArray(
                    new ServiceElementManager[svcElemMgrs.size()]);
            for (ServiceElementManager mgr : mgrs)
                doRedeploy(mgr.getServiceElement(), null, clean, sticky, listener);
        }

        /*
         * Redeploy a ServiceElement or a ServiceBeanInstance  
         */
        void doRedeploy(ServiceElement sElem,
                        ServiceBeanInstance instance,
                        boolean clean,
                        boolean sticky,
                        ServiceProvisionListener listener)
        throws OperationalStringException  {

            if(!isActive())
                throw new OperationalStringException(
                        "not the primary OperationalStringManager");
            ServiceElementManager svcElemMgr = null;
            RedeploymentTask scheduledTask = null;
            if(sElem!=null) {
                svcElemMgr = getServiceElementManager(sElem);
                scheduledTask = getScheduledRedeploymentTask(sElem,
                                                             null);
            } else if(instance != null) {
                svcElemMgr = getServiceElementManager(instance);
                scheduledTask = getScheduledRedeploymentTask(null,
                                                             instance);
            }

            if(svcElemMgr == null) {
                String message;
                if(sElem==null)
                    message = "Unmanaged ServiceElement";
                else
                    message = "Unmanaged ServiceElement ["+sElem.getName()+"]";
                throw new OperationalStringException(message);
            }

            if(scheduledTask!=null) {
                long exec = (scheduledTask.scheduledExecutionTime()-
                             System.currentTimeMillis())/1000;
                if(exec>0) {
                    String item =
                        (sElem==null?"ServiceBeanInstance":"ServiceElement");
                    throw new OperationalStringException(item+" "+
                                                         "already scheduled "+
                                                         "for redeployment "+
                                                         "in "+
                                                         exec+" seconds");
                }
            }

            /* Redeployment is for a ServiceElement */
            if(sElem!=null) {
                ServiceBeanInstance[] instances =
                    svcElemMgr.getServiceBeanInstances();
                if(instances.length>0) {
                    for (ServiceBeanInstance inst : instances)
                        svcElemMgr.redeploy(inst, clean, sticky, listener);
                } else {
                    svcElemMgr.redeploy(listener);
                }
            /* Redeployment is for a ServiceBeanInstance */
            } else {
                svcElemMgr.redeploy(instance, clean, sticky, listener);
            }
        }

        /*
         * Schedule a redeploy task
         */
        void doScheduleReploymentTask(long delay,
                                      ServiceElement sElem,
                                      ServiceBeanInstance instance,
                                      boolean clean,
                                      boolean sticky,
                                      ServiceProvisionListener listener)
        throws OperationalStringException {
            int lastIndex = deployDateList.size()-1;
            if(lastIndex<0 || opString.getStatus()==OperationalString.SCHEDULED)
                throw new OperationalStringException("Cannot redeploy an " +
                                                     "OperationalString with a "+
                                                     "status of Scheduled");
            Date lastDeployDate = deployDateList.get(lastIndex);
            if(opString.getSchedule().getDuration()!=Schedule.INDEFINITE &&
               (delay >
                (lastDeployDate.getTime()+opString.getSchedule().getDuration())))
                throw new OperationalStringException("delay is too long");

            RedeploymentTask scheduledTask = getScheduledRedeploymentTask(sElem,
                                                                          instance);
            if(scheduledTask!=null) {
                long exec = (scheduledTask.scheduledExecutionTime()-
                             System.currentTimeMillis())/1000;
                throw new OperationalStringException("Already "+
                                                     "scheduled "+
                                                     "for redeployment "+
                                                     "in "+
                                                     exec+" seconds");
            }
            RedeploymentTask task = new RedeploymentTask(this,
                                                         sElem,
                                                         instance,
                                                         clean,
                                                         sticky,
                                                         listener);
            addTask(task);
            taskTimer.schedule(task, delay);
            Date redeployDate = new Date(System.currentTimeMillis()+delay);
            Object[] parms = new Object[] {redeployDate,
                                           clean,
                                           sticky,
                                           listener};
            ProvisionMonitorEvent event =
                new ProvisionMonitorEvent(getEventProxy(),
                                          opString.getName(),
                                          sElem,
                                          instance,
                                          parms);

            processEvent(event);

            if(logger.isLoggable(Level.FINEST)) {
                String name = (instance==null?
                               sElem.getName():
                               instance.getServiceBeanConfig().getName());
                String item = (instance==null?
                               "ServiceElement" : "ServiceBeanInstance");
                logger.finest("Schedule ["+name+"] "+item+" "+
                              "redeploy in ["+delay+"] millis");
            }
        }

        /**
         * Check for a scheduled RedeploymentTask for either the ServiceElement or 
         * theServiceBeanInstance. 
         *
         * @param sElem the ServiceElement to check
         * @param instance The coresponding ServiceBeanInstance
         *
         * @return The scheduled RedeploymentTask, or null if not found
         */
        RedeploymentTask getScheduledRedeploymentTask(ServiceElement sElem,
                                                      ServiceBeanInstance instance) {
            TimerTask[]  tasks = getTasks();
            RedeploymentTask scheduledTask = null;
            for (TimerTask task : tasks) {
                if (task instanceof RedeploymentTask) {
                    RedeploymentTask rTask = (RedeploymentTask) task;
                    if (sElem != null && rTask.sElem != null) {
                        if (rTask.sElem.equals(sElem)) {
                            scheduledTask = rTask;
                            break;
                        }
                        if (instance != null && rTask.instance != null) {
                            if (rTask.instance.equals(instance)) {
                                scheduledTask = rTask;
                                break;
                            }
                        }
                    }
                }
            }
            return(scheduledTask);
        }

        /**
         * Get all ServiceElementManager instances
         *
         * @return All ServiceElementManager instances as an array 
         */
        ServiceElementManager[] getServiceElementManagers() {
            return(svcElemMgrs.toArray(
                new ServiceElementManager[svcElemMgrs.size()]));
        }

        /**
         * Get the ServiceElementManager for a ServiceElement instance
         * 
         * @param sElem The ServiceElement instance
         * @return The ServiceElementManager that is managing the ServiceElement. 
         * If no ServiceElementManager is found, null is returned
         */
        ServiceElementManager getServiceElementManager(ServiceElement sElem) {
            for(ServiceElementManager mgr : svcElemMgrs) {
                ServiceElement sElem1 = mgr.getServiceElement();                
                if(sElem.equals(sElem1)) {
                    return (mgr);
                }
            }
            return (null);
        }

        /**
         * Get the ServiceElementManager for a ServiceBeanInstance instance
         * 
         * @param instance The ServiceBeanInstance instance
         * @return The ServiceElementManager that is
         * managing the ServiceElement. If no ServiceElementManager is found,
         * null is returned
         */
        ServiceElementManager getServiceElementManager(ServiceBeanInstance instance) {
            for(ServiceElementManager mgr : svcElemMgrs) {
                if(mgr.hasServiceBeanInstance(instance))
                    return (mgr);
            }
            return (null);
        }

        /**
         * Get the ServiceElementManager from a service proxy
         * 
         * @param proxy The service proxy
         *
         * @return The ServiceElementManager that is
         * managing the ServiceElement. If no ServiceElementManager is found,
         * null is returned
         *
         * @throws IOException If the service proxy from a ServiceBeanInstance
         * returned from a ServiceElementManager cannot be unmarshalled
         */
        ServiceElementManager getServiceElementManager(Object proxy) throws IOException {
            for(ServiceElementManager mgr : svcElemMgrs) {
                ServiceBeanInstance[] instances = mgr.getServiceBeanInstances();
                for (ServiceBeanInstance instance : instances) {
                    try {
                        if(instance.getService().equals(proxy))
                            return (mgr);
                    } catch (ClassNotFoundException e) {
                        logger.log(Level.WARNING, "Unable to obtain proxy", e);
                    }
                }
            }
            return (null);
        }

        /**
         * Add a nested OpStringManager
         * 
         * @param nestedMgr The nested OpStringManager to add
         */
        void addNested(OpStringManager nestedMgr) {
            nestedManagers.add(nestedMgr);
            nestedMgr.addParent(this);
        }

        /**
         * Remove a nested OpStringManager
         * 
         * @param nestedMgr The nested OpStringManager to remove
         */
        void removeNested(OpStringManager nestedMgr) {
            nestedManagers.remove(nestedMgr);
        }

        /**
         * Add a parent for this OpStringManager. This OpStringManager will
         * now be a nested OpStringManager
         * 
         * @param parent The parent for this OpStringManager.
         */
        private void addParent(OpStringManager parent) {
            if(parents.contains(parent))
                return;
            parents.add(parent);
        }

        /**
         * Remove a parent from this OpStringManager. 
         * 
         * @param parent The parent to remove
         */
        private void removeParent(OpStringManager parent) {
            parents.remove(parent);
        }

        /**
         * Get the number of parents the OpStringManager has
         *
         * @return The number of parents the OpStringManager has
         */
        private int getParentCount() {
            return(parents.size());
        }

        /**
         * Returns a <code>TrustVerifier</code> which can be used to verify that a
         * given proxy to this policy handler can be trusted
         */
        public TrustVerifier getProxyVerifier() {
            if(logger.isLoggable(Level.FINEST))
                logger.entering(this.getClass().getName(), "getProxyVerifier");
            return (new BasicProxyTrustVerifier(proxy));
        }

        /**
         * Get all in process TmerTask instances
         * 
         * @return Array of in process TimerTask instances. If there are no
         * TimerTask instances return a zero-length array
         */
        TimerTask[] getTasks() {
            return(scheduledTaskList.toArray(
                                        new TimerTask[scheduledTaskList.size()]));
        }

        /**
         * Add a TimerTask to the Collection of TimerTasks 
         * 
         * @param task The TimerTask to add
         */
        void addTask(TimerTask task) {
            if(task!=null)
                scheduledTaskList.add(task);
        }

        /**
         * Remove a TimerTask from Collection of scheduled TimerTask 
         * instances
         *
         * @param task The TimerTask to remove
         */
        void removeTask(TimerTask task) {
            if(task!=null)
                scheduledTaskList.remove(task);
        }

    } // End OpStringManager

    /**
     * This class represents a scheduled redeployment request
     */
    static class RedeploymentTask extends TimerTask {
        OpStringManager opMgr;
        ServiceBeanInstance instance;
        ServiceElement sElem;
        boolean clean = false;
        boolean sticky = false;
        ServiceProvisionListener listener;

        /**
         * Create a RedeploymentTask
         * 
         * @param opMgr The OpStringManager which scheduled the task 
         * @param sElem The ServiceElement
         * @param instance The ServiceBeanInstance
         * @param clean Use the original configuration or the current instance's
         * config
         * @param sticky Use the same cybernode
         * @param listener A ServiceProvisionListener
         */
        RedeploymentTask(OpStringManager opMgr,
                         ServiceElement sElem,
                         ServiceBeanInstance instance,
                         boolean clean,
                         boolean sticky,
                         ServiceProvisionListener listener) {
            this.opMgr = opMgr;
            this.instance = instance;
            this.sElem = sElem;
            this.clean = clean;
            this.sticky = sticky;
            this.listener = listener;
        }

        public void run() {
            if(!opMgr.isActive()) {
                if(logger.isLoggable(Level.FINEST)) {
                    String name = "unknown";
                    if(instance==null && sElem==null) {
                        name = opMgr.getName();
                    } else {
                        if(sElem!=null)
                            name = sElem.getName();
                        if(instance!=null)
                            name = instance.getServiceBeanConfig().getName();
                    }
                    logger.finest("Redeployment request for "+"["+name+"] "+
                                  "cancelled, OpStringManager is not primary");
                }
                cancel();
                return;
            }

            try {
                if(instance==null && sElem==null) {
                    opMgr.doRedeploy(clean, sticky, listener);
                } else {
                    opMgr.doRedeploy(sElem, instance, clean, sticky, listener);
                }
            } catch (Exception e) {
                logger.log(Level.WARNING,
                           "Executing Scheduled Redeployment", e);
            } finally {
                cancel();
            }
        }

        public boolean cancel() {
            if(opMgr!=null)
                opMgr.removeTask(this);
            return(super.cancel());
        }
    }

    /**
     * This class is used as by a Thread to notify all known ProvisionMonitor
     * instances of back notifications
     */
    static class PeerNotificationTask implements Runnable {
        ProvisionMonitor[] peers;
        PeerInfo info;

        PeerNotificationTask(ProvisionMonitor[] peers, PeerInfo info) {
            this.peers = peers;
            this.info = info;
        }

        public void run() {
            for (ProvisionMonitor peer : peers) {
                try {
                    peer.update(info);
                } catch (Exception e) {
                    logger.log(Level.WARNING,
                               "Exception notifying ProvisionMonitor", e);
                }
            }
        }
    }
    /**
     * Class which manages the discovery of other ProvisionMonitor instances,
     * handles the registration and notification of ProvisionMonitorEvent
     * occurences, and determines backup ProvisionMonitor capabilities
     */
    class ProvisionMonitorPeer extends ServiceDiscoveryAdapter
            implements
            RemoteServiceEventListener,
            FaultDetectionListener<ServiceID>,
            Runnable {
        /** The PeerInfo object for this ProvisionMonitor */
        PeerInfo myPeerInfo;
        /**
         * The BasicEventConsumer providing notification of
         * ProvisionMonitorEvents
         */
        BasicEventConsumer eventConsumer;
        /** Table of Peer ProvisionMonitor instances to OpStringManagers which
         * are backups for the peer */
        final Map<ProvisionMonitor, List<OpStringManager>> opStringTable =
            new HashMap<ProvisionMonitor, List<OpStringManager>>();
        /** The ProvisionMonitor instances this ProvisionMonitor is a backup for */
        final List<ProvisionMonitor> backupList = new ArrayList<ProvisionMonitor>();
        /** The ProvisionMonitor that is our backup */
        ProvisionMonitor backup;
        /** Ordered set of ProvisionMonitor instances */
        final Set<PeerInfo> peerSet = new TreeSet<PeerInfo>();
        /** Flag which indicates my backup is local, on the same machine */
        boolean localBackup = false;
        /**
         * Table of service IDs to FaultDetectionHandler instances, one for each
         * service
         */
        final Hashtable<ServiceID, FaultDetectionHandler> fdhTable =
            new Hashtable<ServiceID, FaultDetectionHandler>();
        /** ServiceTemplate for ProvisionMonitor discovery */
        ServiceTemplate template;
        /** DiscoveryManagement instance */
        DiscoveryManagement dm;
        /** ServiceDiscoveryManager for ProvisionMonitor instances */
        ServiceDiscoveryManager sdm;
        /** The LookupCache for the ServiceDiscoveryManager */
        LookupCache lCache;
        ProxyPreparer peerProxyPreparer;

        ProvisionMonitorPeer(Configuration config,
                             DiscoveryManagement dm) {
            try {
                this.dm = dm;
                peerProxyPreparer =
                    (ProxyPreparer)config.getEntry(CONFIG_COMPONENT,
                                                   "peerProxyPreparer",
                                                   ProxyPreparer.class,
                                                   new BasicProxyPreparer());
                /* Create the PeerInfo object */
                java.util.Random rand =
                    new java.util.Random(System.currentTimeMillis());
                long randomNumber = rand.nextLong();
                myPeerInfo = new PeerInfo(
                                      (ProvisionMonitor)getServiceProxy(),
                                      randomNumber,
                                      computeResource.getAddress().getHostAddress());
                eventConsumer =
                    new BasicEventConsumer(new EventDescriptor(
                                                ProvisionMonitorEvent.class,
                                                ProvisionMonitorEvent.ID),
                                           this,
                                           config);
                template =
                    new ServiceTemplate(null,
                                        new Class[] {ProvisionMonitor.class},
                                        null);
                new Thread(this).start();
            } catch(Exception e) {
                peerLogger.log(Level.WARNING,
                               "Constructing a ProvisionMonitorPeer",
                               e);
            }
        }

        /**
         * Terminate the ProvisionMonitorPeer, cleaning up listeners, etc...
         */
        void terminate() {
            for(Enumeration<FaultDetectionHandler> en = fdhTable.elements();
                en.hasMoreElements();) {
                FaultDetectionHandler fdh = en.nextElement();
                fdh.terminate();
            }
            eventConsumer.deregister(this);
            if(eventConsumer != null)
                eventConsumer.terminate();
            if(sdm != null)
                sdm.terminate();
        }

        /**
         * Be a backup for another ProvisionMonitor
         * 
         * @param primary The ProvisionMonitor to be the backup for
         * @return True if added to the collection, false if not
         */
        boolean addAsBackupFor(ProvisionMonitor primary) {
            boolean assigned;
            int listSize;
            synchronized(backupList) {
                assigned = backupList.add(primary);
                listSize = backupList.size();
            }
            if(assigned) {
                PeerInfo peer = getPeerInfo(primary);
                ProvisionMonitor[] provisioners = getProvisionMonitorPeers();
                /* if we dont know about the new primary, add it into the mix */
                if(peer == null) {
                    ProvisionMonitor[] adjustedProvisioners =
                        new ProvisionMonitor[provisioners.length + 1];
                    if(provisioners.length > 0)
                        System.arraycopy(provisioners,
                                         0,
                                         adjustedProvisioners,
                                         0,
                                         provisioners.length);
                    adjustedProvisioners[provisioners.length] = primary;
                    provisioners = adjustedProvisioners;
                }
                myPeerInfo.setBackupCount(listSize);
                new Thread(new PeerNotificationTask(provisioners,
                                                    myPeerInfo)).start();
            }
            return (assigned);
        }

        /**
         * Remove the ProvisionMonitor from the backup list
         * 
         * @param primary The ProvisionMonitor to remove
         * @return True if removed from the collection, false if not
         */
        boolean removeAsBackupFor(ProvisionMonitor primary) {
            boolean removed;
            int listSize;
            synchronized(backupList) {
                removed = backupList.remove(primary);
                listSize = backupList.size();
            }
            if(removed) {
                myPeerInfo.setBackupCount(listSize);
                notifyPeers(myPeerInfo);
            }
            return (removed);
        }

        /**
         * Get the latest & greatest PeerInfo
         *
         * @return The PeerInfo
         */
        PeerInfo doGetPeerInfo() {
            synchronized(backupList) {
                int bCount = backupList.size();
                myPeerInfo.setBackupCount(bCount);
            }
            return (myPeerInfo);
        }

        /**
         * Get all backups
         *
         * @return An array of PeerInfo objects, one for each backup
         */
        PeerInfo[] getBackupInfo() {
            List<PeerInfo> list = new ArrayList<PeerInfo>();
            synchronized(peerSet) {
                for (PeerInfo aPeerSet : peerSet) {
                    list.add(aPeerSet);
                }
            }
            return(list.toArray(new PeerInfo[list.size()]));
        }

        /**
         * Update the backup count for a ProvisionMonitor peer
         * 
         * @param peer The PeerInfo
         */
        void peerUpdated(PeerInfo peer) {
            if(peer == null) {
                if(peerLogger.isLoggable(Level.FINE))
                    peerLogger.log(Level.FINE,
                                   "updatePeer(): Unknown ProvisionMonitor");
                return;
            }
            synchronized(peerSet) {
                if(peerSet.remove(peer)) {
                    peerSet.add(peer);
                }
            }
            if(peerLogger.isLoggable(Level.FINE))
                peerLogger.log(Level.FINE,
                               "ProvisionMonitorPeer: ProvisionMonitor updated "+
                               "info :\n"
                               + "\tcount={0}, address={1}, id={2}, ",
                               new Object[] {peer.getBackupCount(),
                                             peer.getAddress(),
                                             peer.getID().toString()});
        }

        /**
         * Add a ProvisionMonitor
         * 
         * @param item The ServiceItem for the ProvisionMonitor
         * @return The PeerInfo object
         */
        PeerInfo addProvisioner(ServiceItem item) {
            PeerInfo peer = null;
            try {
                if(getServiceProxy().equals(item.service))
                    return (null);
                peer = ((ProvisionMonitor)item.service).getPeerInfo();
                synchronized(peerSet) {
                    peerSet.add(peer);
                }
                if(peerLogger.isLoggable(Level.FINE))
                    peerLogger.log(Level.FINE,
                                   "Import OperationalStrings from: " +
                                   "address={"+peer.getAddress()+"}, " +
                                   "id={"+peer.getID().toString()+"}");
                importOperationalStrings(((ProvisionMonitor)item.service));
                eventConsumer.register(item);
            } catch(Exception e) {
                peerLogger.log(Level.WARNING,
                               "Adding a ProvisionMonitor peer instance", e);
            }
            return (peer);
        }

        /**
         * Notification that a Provisioner has been discovered
         * 
         * @param sdEvent The ServiceDiscoveryEvent
         */
        public void serviceAdded(ServiceDiscoveryEvent sdEvent) {
            ServiceItem item = sdEvent.getPostEventServiceItem();
            /* prepare the newly discovered proxy */
            try {
                item.service = peerProxyPreparer.prepareProxy(item.service);
            } catch (RemoteException e) {
                peerLogger.log(Level.WARNING,
                               "Could not prepare peer proxy, " +
                               "not adding as peer",
                               e);
                return;
            }

            /* Check if we already know about this guy. If so, just return */
            PeerInfo peer = getPeerInfo((ProvisionMonitor)item.service);
            if(peer != null)
                return;
            /* Add the new provisioner to the set */
            peer = addProvisioner(item);
            /* If peer is null, then we just discovered ourselves */
            if(peer == null)
                return;
            /*
             * If we dont have a backup yet, use the newly discovered
             * provisioner
             */
            if(backup == null) {
                if(peerLogger.isLoggable(Level.FINE))
                    peerLogger.log(Level.FINE, "No backup yet, try assignment");
                doAssignment(peer);
            } else {
                /*
                 * If our backup is a local backup (same machine), and the newly
                 * added peer is not on the same machine, assign the new
                 * provisioner as the backup
                 */
                //if(localBackup &&
                //   (!computeResource.getAddress().equals(peer.getAddress()))) {

                String resourceAddress =
                    computeResource.getAddress().getHostAddress();
                String resourceHostName =
                    computeResource.getAddress().getHostName();

                if(localBackup && !(
                    resourceAddress.equals(peer.getAddress()) ||
                    resourceHostName.equals(peer.getAddress())) ) {

                    ProvisionMonitor currentBackup = backup;
                    if(doAssignment(peer)) {
                        try {
                            currentBackup.removeBackupFor(
                                (ProvisionMonitor)getServiceProxy());
                        } catch(Exception e) {
                            peerLogger.log(Level.WARNING,
                                           "Adjusting backup instance away from "+
                                           "local instance",
                                           e);
                        }
                    }
                }
            }
            setFaultDetectionHandler(item.service, item.serviceID);
        }

        /**
         * A ProvisionMonitor has failed
         */
        public void serviceFailure(Object service, ServiceID serviceID) {
            /* Clean up the event consumer, dont explictly cancel the lease since
             * the remote peer may be unreachable, this could block on the
             * remote method invocation on the lease itself */
            eventConsumer.deregister(serviceID, false);
            ProvisionMonitor primary = null;
            ProvisionMonitor provMon = (ProvisionMonitor)service;
            /*
             * Remove the ProvisionMonitor that left the network from the
             * peerSet
             */
            PeerInfo info = getPeerInfo(provMon);
            if(info == null) {
                if(peerLogger.isLoggable(Level.FINE))
                    peerLogger.log(Level.FINE,
                                   "serviceFailure: Unknown ProvisionMonitor");
                return;
            }
            synchronized(peerSet) {
                if(peerSet.remove(info))
                    if(peerLogger.isLoggable(Level.FINE))
                        peerLogger.log(Level.FINE,
                                       "ProvisionMonitorPeer: Removed "+
                                       "ProvisionMonitor "+
                                       "at ["+info.getAddress()+"]");
            }
            /*
             * Check to see if the ProvisionMonitor that just left the network
             * is our back-up. If so, locate another ProvisionMonitor and set
             * that as the backup
             */
            if(backup != null && backup.equals(provMon)) {
                peerLogger.log(Level.FINE,
                               "ProvisionMonitorPeer: Backup has left the network, "+
                               "locate another backup");
                if(!assignBackup()) {
                    backup = null;
                    if(peerLogger.isLoggable(Level.FINE))
                        peerLogger.log(Level.FINE,
                                       "ProvisionMonitorPeer: No backup located");
                }
            }
            /*
             * Check to see if we're the back-up for the ProvisionMonitor that
             * just left the network. If we are the backup then remove the entry
             * for the backup list
             */
            boolean removedBackup = false;
            int listSize = 0;
            synchronized(backupList) {
                int index = backupList.indexOf(provMon);
                if(index != -1) {
                    removedBackup = true;
                    primary = backupList.remove(index);
                    listSize = backupList.size();
                }
            }
            /* If we were a backup, notify all peers of our current backup count */
            if(removedBackup) {
                myPeerInfo.setBackupCount(listSize);
                notifyPeers(myPeerInfo);
            }
            /* Get OpStringManagers which were backing up opstrings in the failed
             * ProvisionMonitor */
            List<OpStringManager> opMgrList;
            synchronized(opStringTable) {
                opMgrList = opStringTable.remove(primary);
            }
            /*
             * If we have a list, then iterate through all OpStringManager
             * instances and set them active
             */
            if(opMgrList != null) {
                if(peerLogger.isLoggable(Level.FINEST))
                    peerLogger.log(Level.FINEST,
                                   "Number of OpStringManager instances that " +
                                   "are back ups : "+opMgrList.size());
                for (OpStringManager opMgr : opMgrList) {
                    opMgr.setActive(true);
                    ProvisionMonitorEvent event =
                        new ProvisionMonitorEvent(
                            getEventProxy(),
                            ProvisionMonitorEvent.Action.OPSTRING_MGR_CHANGED,
                            opMgr.doGetOperationalString());
                    processEvent(event);
                    if (peerLogger.isLoggable(Level.FINE))
                        peerLogger.log(Level.FINE,
                                       "Set active for opstring : " +
                                       opMgr.getName());
                }
            } else {
                if(peerLogger.isLoggable(Level.FINE))
                    peerLogger.log(Level.FINE,
                                   "No backup OpStringManagers for removed peer");
            }
            /* Remove the FaultDetectionHandler from the table */
            fdhTable.remove(serviceID);
        }

        /**
         * Notify ProvisionMonitor peers of a change in PeerInfo
         * 
         * @param info PeerInfo
         */
        void notifyPeers(PeerInfo info) {
            new Thread(new PeerNotificationTask(getProvisionMonitorPeers(),
                                                info)).start();
        }

        /**
         * Assign a backup
         *
         * @return true is successfull
         */
        boolean assignBackup() {
            boolean assignedBackup = false;
            PeerInfo[] peers = getPeers();
            if(peers.length > 0) {
                for (PeerInfo peer : peers) {
                    String resourceAddress =
                        computeResource.getAddress().getHostAddress();
                    String resourceHostName =
                        computeResource.getAddress().getHostName();
                    if (resourceAddress.equals(peer.getAddress()) ||
                        resourceHostName.equals(peer.getAddress()))
                        continue;
                    if (doAssignment(peer)) {
                        assignedBackup = true;
                        localBackup = false;
                        break;
                    }
                }
                /*
                 * If we dont have a backup yet, its because the peerList has
                 * entries that are on the same machine as we are. If its the
                 * latter case, use the local backup for now
                 */
                if(!assignedBackup) {
                    if(doAssignment(peers[0])) {
                        localBackup = true;
                        assignedBackup = true;
                    }
                }
            }
            return (assignedBackup);
        }

        /**
         * Assign the ProvisionMonitor described by the PeerInfo object as a
         * backup
         * 
         * @param peer The PeerInfo for a ProvisionMonitor
         *
         * @return true if assigment succeeded
         */
        boolean doAssignment(PeerInfo peer) {
            boolean assigned = false;
            try {
                backup = peer.getService();
                backup.assignBackupFor((ProvisionMonitor)getServiceProxy());
                assigned = true;
                if(peerLogger.isLoggable(Level.FINE))
                    peerLogger.log(Level.FINE,
                                   "ProvisionMonitorPeer: ProvisionMonitor backup "+
                                   "info :"+
                                   "\n\tcount={0}, address={1}, id={2}",
                                   new Object[] {peer.getBackupCount(),
                                                 peer.getAddress(),
                                                 peer.getID().toString()});
            } catch(Exception e) {
                peerLogger.log(Level.WARNING,
                               "Assigning ProvisionMonitor backup",
                               e);
            }
            return (assigned);
        }

        /**
         * Get a snapshot of the PeerInfo collection
         * 
         * @return An array of PeerInfo instances in sorted order.
         * A new array is allocated each time. If there are no PeerInfo objects,
         * a zero length array will be returned.
         */
        PeerInfo[] getPeers() {
            PeerInfo[] peers;
            synchronized(peerSet) {
                peers = peerSet.toArray(new PeerInfo[peerSet.size()]);
            }
            return (peers);
        }

        /**
         * Get an array of ProvisionMonitor instances from the PeerInfo
         * collection
         * 
         * @return An array of ProvisionMonitor instances
         * in sorted order. A new array is allocated each time. If there are no
         * PeerInfo objects, a zero length array will be returned.
         */
        ProvisionMonitor[] getProvisionMonitorPeers() {
            PeerInfo[] peers = getPeers();
            ProvisionMonitor[] provisioners = new ProvisionMonitor[peers.length];
            for(int i = 0; i < provisioners.length; i++)
                provisioners[i] = peers[i].getService();
            return (provisioners);
        }

        /**
         * Get the PeerInfo object for a ProvisionMonitor
         * 
         * @param monitor The ProvisionMonitor instance
         * @return The PeerInfo instance or null if not found
         */
        PeerInfo getPeerInfo(ProvisionMonitor monitor) {
            PeerInfo[] peers = getPeers();
            PeerInfo peer = null;
            for (PeerInfo peer1 : peers) {
                if (peer1.getService().equals(monitor)) {
                    peer = peer1;
                    break;
                }
            }
            return (peer);
        }

        /**
         * @see org.rioproject.event.RemoteServiceEventListener#notify
         */
        public void notify(RemoteServiceEvent event) {
            try {
                Object eventSource = event.getSource();
                if(!(eventSource instanceof ProvisionMonitor)) {
                    if(peerLogger.isLoggable(Level.FINE))
                        peerLogger.log(Level.FINE,
                                       "ProvisionMonitorPeer: unknown event source : "
                                       + eventSource.getClass().getName());
                    return;
                }
                ProvisionMonitor remoteMonitor = (ProvisionMonitor)eventSource;
                String opStringName = null;
                OpStringManager opMgr;
                ProvisionMonitorEvent pme = (ProvisionMonitorEvent)event;
                OperationalString opString = pme.getOperationalString();
                ServiceElement sElem = pme.getServiceElement();
                ProvisionMonitorEvent.Action action = pme.getAction();
                switch(action) {
                    case SERVICE_ELEMENT_UPDATED:
                    case SERVICE_BEAN_INCREMENTED:
                    case SERVICE_BEAN_DECREMENTED:
                        String sAction = action.toString();
                        if(sElem == null) {
                            if(peerLogger.isLoggable(Level.FINE))
                                peerLogger.log(Level.FINE,
                                               "ProvisionMonitorPeer: "
                                               + sAction + " sElem is null");
                            return;
                        }
                        opStringName = sElem.getOperationalStringName();
                        if(peerLogger.isLoggable(Level.FINEST))
                            peerLogger.finest("ProvisionMonitorPeer: " + sAction
                                              + ", opstring: " + opStringName);
                        opMgr = getOpStringManager(opStringName);
                        if(opMgr == null) {
                            if(peerLogger.isLoggable(Level.FINE))
                                peerLogger.log(Level.FINE,
                                               "ProvisionMonitorPeer: "
                                               + sAction + " opstring ["
                                               + opStringName + "] not found");
                            return;
                        }
                        try {
                            opMgr.doUpdateServiceElement(sElem);
                        } catch(Exception e) {
                            peerLogger.log(Level.WARNING,
                                           "Updating OperationalStringManager's "+
                                           "ServiceElement",
                                           e);
                        }
                        break;
                    case SERVICE_ELEMENT_ADDED:
                        if(sElem == null) {
                            if(peerLogger.isLoggable(Level.FINE))
                                peerLogger.log(Level.FINE,
                                               "ProvisionMonitorPeer: "+
                                               "SERVICE_ELEMENT_ADDED sElem is null");
                            return;
                        }
                        opStringName = sElem.getOperationalStringName();
                        if(peerLogger.isLoggable(Level.FINEST))
                            peerLogger.finest("ProvisionMonitorPeer: "+
                                              "SERVICE_ELEMENT_ADDED, opstring: "
                                              + opStringName);
                        opMgr = getOpStringManager(opStringName);
                        if(opMgr == null) {
                            if(peerLogger.isLoggable(Level.FINE))
                                peerLogger.log(Level.FINE,
                                               "ProvisionMonitorPeer: "+
                                               "SERVICE_ELEMENT_ADDED opstring "+
                                               "["+opStringName+"] not found");
                            return;
                        }
                        try {
                            opMgr.doAddServiceElement(sElem, null);
                        } catch(Exception e) {
                            peerLogger.log(Level.WARNING,
                                           "Adding ServiceElement to "+
                                           "OperationalStringManager",
                                           e);
                        }
                        break;
                    case SERVICE_ELEMENT_REMOVED:
                        if(sElem == null) {
                            if(peerLogger.isLoggable(Level.FINE))
                                peerLogger.log(Level.FINE,
                                               "ProvisionMonitorPeer: "+
                                               "SERVICE_ELEMENT_REMOVED sElem "+
                                               "is null");
                            return;
                        }
                        opStringName = sElem.getOperationalStringName();
                        if(peerLogger.isLoggable(Level.FINEST))
                            peerLogger.finest("ProvisionMonitorPeer: "+
                                              "SERVICE_ELEMENT_REMOVED, opstring: "
                                              + opStringName);
                        opMgr = getOpStringManager(opStringName);
                        if(opMgr == null) {
                            if(peerLogger.isLoggable(Level.FINE))
                                peerLogger.log(Level.FINE,
                                               "ProvisionMonitorPeer: "+
                                               "SERVICE_ELEMENT_REMOVED opstring ["
                                               + opStringName
                                               + "] not found");
                            return;
                        }
                        try {
                            opMgr.doRemoveServiceElement(sElem, false);
                        } catch(Exception e) {
                            peerLogger.log(Level.WARNING,
                                           "Removing ServiceElement from "+
                                           "OperationalStringManager",
                                           e);
                        }
                        break;
                    case OPSTRING_DEPLOYED:
                        if(opString == null) {
                            if(peerLogger.isLoggable(Level.FINE))
                                peerLogger.log(Level.FINE,
                                               "ProvisionMonitorPeer: "+
                                               "OPSTRING_DEPLOYED opstring is null");
                            return;
                        }
                        DeployAdmin deployAdmin =
                            (DeployAdmin)remoteMonitor.getAdmin();
                        opStringProcessor(opString, remoteMonitor, deployAdmin);
                        if(peerLogger.isLoggable(Level.FINEST))
                            peerLogger.finest("ProvisionMonitorPeer: "+
                                              "OPSTRING_DEPLOYED, opstring: "
                                              + opString.getName());
                        break;
                    case OPSTRING_UNDEPLOYED:
                        if(opString == null) {
                            if(peerLogger.isLoggable(Level.FINE))
                                peerLogger.log(Level.FINE,
                                               "ProvisionMonitorPeer: "+
                                               "OPSTRING_UNDEPLOYED opstring "+
                                               "is null");
                            return;
                        }
                        if(peerLogger.isLoggable(Level.FINEST))
                            peerLogger.finest("ProvisionMonitorPeer: "+
                                              "OPSTRING_UNDEPLOYED, opstring: "
                                              + opString.getName());
                        opMgr = getOpStringManager(opString.getName());
                        if(opMgr == null) {
                            if(peerLogger.isLoggable(Level.FINE))
                                peerLogger.log(Level.FINE,
                                               "ProvisionMonitorPeer: "+
                                               "OPSTRING_UNDEPLOYED for opstring ["
                                               + opString.getName()+"] not found");
                            return;
                        }

                        opMgr.setDeploymentStatus(OperationalString.UNDEPLOYED);
                        opMgr.terminate(false);
                        synchronized(opStringTable) {
                            if(opStringTable.containsKey(remoteMonitor)) {
                                List<OpStringManager> list =
                                    opStringTable.get(remoteMonitor);
                                list.remove(opMgr);
                                opStringTable.put(remoteMonitor, list);
                            }
                        }
                        break;

                    case OPSTRING_MGR_CHANGED:
                        if(opString == null) {
                            if(peerLogger.isLoggable(Level.FINE))
                                peerLogger.log(Level.FINE,
                                               "ProvisionMonitorPeer: "+
                                               "OPSTRING_MGR_CHANGED opstring "+
                                               "is null");
                            return;
                        }
                        if(peerLogger.isLoggable(Level.FINE))
                            peerLogger.fine("ProvisionMonitorPeer: "+
                                            "OPSTRING_MGR_CHANGED, opstring: "
                                            + opString.getName());
                        opMgr = getOpStringManager(opString.getName());
                        if(opMgr == null) {
                            if(peerLogger.isLoggable(Level.FINE))
                                peerLogger.log(Level.FINE,
                                               "ProvisionMonitorPeer: "+
                                               "OPSTRING_MGR_CHANGED opstring ["
                                               + opString.getName()+"] "+
                                               "not found");
                            return;
                        }
                        synchronized(opStringTable) {
                            List<OpStringManager> list;
                            if(opStringTable.containsKey(remoteMonitor))
                                list = opStringTable.get(remoteMonitor);
                            else
                                list = new ArrayList<OpStringManager>();
                            if(!list.contains(opMgr)) {
                                list.add(opMgr);
                                opStringTable.put(remoteMonitor, list);
                                if(peerLogger.isLoggable(Level.FINE))
                                    peerLogger.fine("ProvisionMonitorPeer: "+
                                                    "Reset backup peer for ["+
                                                    opString.getName()+"] to "+
                                                    remoteMonitor.toString());
                            } else {
                                if(peerLogger.isLoggable(Level.FINE))
                                    peerLogger.fine("ProvisionMonitorPeer: "+
                                                    "Already a backup for ["+
                                                    opString.getName()+"] to "+
                                                    remoteMonitor.toString());
                            }
                        }
                        break;
                    case OPSTRING_UPDATED:
                        if(opString == null) {
                            if(peerLogger.isLoggable(Level.FINE))
                                peerLogger.log(Level.FINE,
                                               "ProvisionMonitorPeer: "+
                                               "OPSTRING_UNDEPLOYED opstring is null");
                            return;
                        }
                        if(peerLogger.isLoggable(Level.FINEST))
                            peerLogger.finest("ProvisionMonitorPeer: "+
                                              "OPSTRING_UPDATED, opstring: "
                                              + opString.getName());
                        opMgr = getOpStringManager(opString.getName());
                        if(opMgr == null) {
                            if(peerLogger.isLoggable(Level.FINE))
                                peerLogger.log(Level.FINE,
                                               "ProvisionMonitorPeer: "+
                                               "OPSTRING_UPDATED for opstring ["
                                               + opString.getName()+"] not found");
                            return;
                        }
                        opMgr.doUpdateOperationalString(opString);
                        break;

                    case SERVICE_BEAN_INSTANCE_UPDATED:
                        ServiceBeanInstance instance = pme.getServiceBeanInstance();
                        if(instance == null) {
                            if(peerLogger.isLoggable(Level.FINE))
                                peerLogger.log(Level.FINE,
                                               "ProvisionMonitorPeer: "+
                                               "SERVICE_BEAN_INSTANCE_UPDATED "+
                                               "instance is null");
                            return;
                        }
                        opStringName = pme.getOperationalStringName();
                        if(peerLogger.isLoggable(Level.FINEST))
                            peerLogger.finest("ProvisionMonitorPeer: "+
                                              "SERVICE_BEAN_INSTANCE_UPDATED, "+
                                              "opstring: "+ opStringName);
                        opMgr = getOpStringManager(opStringName);
                        if(opMgr == null) {
                            if(peerLogger.isLoggable(Level.FINE))
                                peerLogger.log(Level.FINE,
                                               "ProvisionMonitorPeer: "+
                                               "SERVICE_BEAN_INSTANCE_UPDATED for "+
                                               "opstring ["+opStringName+"] "+
                                               "not found");
                            return;
                        }
                        try {
                            opMgr.doUpdateServiceBeanInstance(instance);
                        } catch(Exception e) {
                            peerLogger.log(Level.WARNING,
                                           "Updating OperationalStringManager's "+
                                           "ServiceBeanInstance",
                                           e);
                        }
                        break;

                    case SERVICE_PROVISIONED:
                        instance = pme.getServiceBeanInstance();
                        if(instance == null) {
                            if(peerLogger.isLoggable(Level.FINE))
                                peerLogger.log(Level.FINE,
                                               "ProvisionMonitorPeer: "+
                                               "SERVICE_PROVISIONED "+
                                               "instance is null");
                            return;
                        }
                        opStringName = pme.getOperationalStringName();
                        if(peerLogger.isLoggable(Level.FINEST))
                            peerLogger.finest("ProvisionMonitorPeer: "+
                                              "SERVICE_PROVISIONED, "+
                                              "opstring: "+ opStringName);
                        opMgr = getOpStringManager(opStringName);
                        if(opMgr == null) {
                            if(peerLogger.isLoggable(Level.FINE))
                                peerLogger.log(Level.FINE,
                                               "ProvisionMonitorPeer: "+
                                               "SERVICE_PROVISIONED for "+
                                               "opstring ["+opStringName+"] "+
                                               "OpStringManager not found");
                            return;
                        }
                        ServiceElementManager mgr =
                            opMgr.getServiceElementManager(sElem);
                        if(mgr==null) {
                            if(peerLogger.isLoggable(Level.FINE))
                                peerLogger.log(Level.FINE,
                                               "ProvisionMonitorPeer: "+
                                               "SERVICE_PROVISIONED for "+
                                               "opstring ["+opStringName+"] "+
                                               "ServiceElementManager "+
                                               "not found");
                            return;
                        }
                        boolean isStarted = mgr.isStarted();
                        while(!isStarted) {
                            try {
                                Thread.sleep(1000);
                                isStarted = mgr.isStarted();
                            } catch(InterruptedException ignore) {
                                /* Ignore */
                            }
                        }
                        mgr.importServiceBeanInstance(instance);

                        ServiceChannel channel = ServiceChannel.getInstance();
                        channel.broadcast(
                            new ServiceChannelEvent(this,
                                                    sElem,
                                                    instance,
                                                    ServiceChannelEvent.PROVISIONED));
                        break;

                case REDEPLOY_REQUEST:
                    opMgr = getOpStringManager(pme.getOperationalStringName());
                    if(opMgr == null) {
                        if(peerLogger.isLoggable(Level.FINE))
                            peerLogger.log(Level.FINE,
                                           "ProvisionMonitorPeer: "+
                                           "REDEPLOY_REQUEST opstring "+
                                           "["+opStringName+"] not found");
                        return;
                    }
                    Object[] parms = pme.getRedeploymentParms();
                    Date redeployDate = (Date)parms[0];
                    boolean clean = (Boolean) parms[1];
                    boolean sticky = (Boolean) parms[2];
                    ServiceProvisionListener listener =
                        (ServiceProvisionListener)parms[3];

                    long delay = redeployDate.getTime() - System.currentTimeMillis();
                    if(delay <= 0) {
                        if(peerLogger.isLoggable(Level.FINE))
                            peerLogger.log(Level.FINE,
                                           "ProvisionMonitorPeer: "+
                                           "REDEPLOY_REQUEST for opstring "+
                                           "["+opStringName+"] startTime has "+
                                           "already passed, scheduled "+
                                           "redeployment cancelled");
                    } else  {
                        opMgr.doScheduleReploymentTask(delay,
                                                       pme.getServiceElement(),
                                                       pme.getServiceBeanInstance(),
                                                       clean,
                                                       sticky,
                                                       listener);
                    }
                    break;
                }
            } catch(Throwable t) {
                peerLogger.log(Level.WARNING,
                               "ProvisionMonitorEvent notification",
                               t);
            }
        }

        /**
         * Perform initial discovery in a thread
         */
        public void run() {
            try {
                if(config == null)
                    sdm = new ServiceDiscoveryManager(dm,
                                                      new LeaseRenewalManager());
                else
                    sdm =
                        new ServiceDiscoveryManager(dm,
                                                    new LeaseRenewalManager(config),
                                                    config);
                lCache = sdm.createLookupCache(template, null, this);
                if(!assignBackup())
                    if(peerLogger.isLoggable(Level.FINE))
                        peerLogger.log(Level.FINE,
                                       "ProvisionMonitorPeer: No backup");
            } catch(Exception e) {
                peerLogger.log(Level.WARNING, "ProvisionMonitor discovery", e);
            }
        }

        /**
         * Set the FaultDetectionHandler for a newly discovered ProvisionMonitor
         * 
         * @param service The ProvisionMonitor's proxy
         * @param serviceID The serviceID of the ProvisionMonitor
         */
        void setFaultDetectionHandler(Object service, ServiceID serviceID) {
            try {
                if(serviceID == null) {
                    peerLogger.info("No ServiceID for newly discovered "+
                                    "ProvisionMonitor, cant setup FDH");
                    return;
                }
                ServiceBeanAdmin sbAdmin =
                    (ServiceBeanAdmin)((Administrable)service).getAdmin();
                ClassBundle fdhBundle =
                    sbAdmin.getServiceElement().getFaultDetectionHandlerBundle();
                FaultDetectionHandler<ServiceID> fdh =
                FaultDetectionHandlerFactory.getFaultDetectionHandler(
                                                fdhBundle,
                                                service.getClass().getClassLoader());
                fdh.register(this);
                fdh.monitor(service, serviceID, lCache);
                fdhTable.put(serviceID, fdh);
            } catch(Exception e) {
                peerLogger.log(Level.WARNING,
                               "Setting FaultDetectionHandler for a newly "+
                               "discovered ProvisionMonitor",
                               e);
            }
        }

        /**
         * Import all OperationalString instances from a newly discovered
         * ProvisionMonitor that the ProvisionMonitor is managing
         * 
         * @param peer The peer ProvisionMonitor service
         */
        void importOperationalStrings(ProvisionMonitor peer) {
            try {
                DeployAdmin peerDeployAdmin = (DeployAdmin)peer.getAdmin();
                OperationalStringManager[] mgrs =
                    peerDeployAdmin.getOperationalStringManagers();
                if(mgrs == null || mgrs.length == 0) {
                    return;
                }
                for (OperationalStringManager mgr : mgrs) {
                    if (mgr.isManaging()) {
                        opStringProcessor(mgr.getOperationalString(),
                                          peer,
                                          peerDeployAdmin);
                    }
                }
            } catch(Exception e) {
                peerLogger.log(Level.WARNING, "Importing OperationalStrings", e);
            }
        }

        /**
         * Recursive method to add an OperationalString
         * 
         * @param opString The OperationalString to add
         * @param peer A peer ProvisionMonitor service
         * @param peerDeployAdmin The peer ProvisionMonitor DeployAdmin
         */
        void opStringProcessor(OperationalString opString,
                               ProvisionMonitor peer,
                               DeployAdmin peerDeployAdmin) {
            try {
                addPeerOpString(opString, peer, peerDeployAdmin);
                OperationalString[] nested =
                    opString.getNestedOperationalStrings();
                for (OperationalString aNested : nested)
                    opStringProcessor(aNested, peer, peerDeployAdmin);
            } catch(Exception e) {
                peerLogger.log(Level.WARNING,
                               "Adding OperationalString ["+opString.getName()+"]",
                               e);
            }
        }

        /**
         * Adds an OperationalString
         * 
         * @param opString OperationalString to add
         * @param peer A peer ProvisionMonitor service
         * @param peerDeployAdmin The peer ProvisionMonitor DeployAdmin
         */
        synchronized void addPeerOpString(OperationalString opString,
                                          ProvisionMonitor peer,
                                          DeployAdmin peerDeployAdmin) {
            Map<String, Throwable> map = new HashMap<String, Throwable>();
            try {
                boolean resolveConflict = false;
                if(!opStringExists(opString.getName())) {
                    if(peerLogger.isLoggable(Level.FINE))
                        peerLogger.log(Level.FINE,
                                       "Adding OpString [{0}] from Peer {1}",
                                       new Object[] {opString.getName(),
                                                     peer.toString()});
                    OpStringManager opMgr =
                        addOperationalString(opString,
                                             map,
                                             null,
                                             peerDeployAdmin,
                                             null);
                    if(opMgr==null) {
                        resolveConflict = true;
                    } else {
                        List<OpStringManager> list;
                        synchronized(opStringTable) {
                            if(opStringTable.containsKey(peer))
                                list = opStringTable.get(peer);
                            else
                                list = new ArrayList<OpStringManager>();
                            list.add(opMgr);
                            opStringTable.put(peer, list);
                        }
                    }
                } else {
                    resolveConflict = true;
                }
                if(resolveConflict) {
                    if(peerLogger.isLoggable(Level.FINE))
                        peerLogger.log(Level.FINE,
                                       "Resolve conflict for OperationalString " +
                                       "["+opString.getName()+"]");
                    OpStringManager localMgr = getOpStringManager(opString.getName());
                    if(localMgr.isActive()) {
                        resolveConflict(opString, peer, peerDeployAdmin);
                    }
                }
            } catch(Throwable t) {
                peerLogger.log(Level.WARNING, "Adding Peer OperationalStrings", t);
            }
        }

        /**
         * Resolve a conflicting Primary relationship
         *
         * @param opString The OperationalString caught in the middle
         * @param peer The ProvisionMonitor that is also managing
         * @param peerDeployAdmin The peer's DeployAdmin
         */
        void resolveConflict(OperationalString opString,
                             ProvisionMonitor peer,
                             DeployAdmin peerDeployAdmin) {
            boolean addAsBackup = false;
            OpStringManager localMgr = null;
            OperationalStringManager peerMgr;
            try {
                PeerInfo peerInfo = getPeerInfo(peer);
                if(peerLogger.isLoggable(Level.FINE))
                    peerLogger.fine("ProvisionMonitor at "+
                                    "["+peerInfo.getAddress()+"] "+
                                    "also has "+
                                    "["+opString.getName()+"] deployed");

                localMgr = getOpStringManager(opString.getName());
                peerMgr = peerDeployAdmin.getOperationalStringManager(
                                                        opString.getName());
                if(!peerMgr.isManaging()) {
                    if(peerLogger.isLoggable(Level.FINE))
                        peerLogger.fine("ProvisionMonitor at "+
                                        "["+peerInfo.getAddress()+"] "+
                                        "is not managing "+
                                        "["+opString.getName()+"], "+
                                        "no conflict observed");
                } else {
                    /* See who deployed the OperationalString first */
                    Date[] peerDates = peerMgr.getDeploymentDates();
                    Date[] localDates = localMgr.getDeploymentDates();
                    /* If its scheduled, the use PeerInfo */
                    if(peerDates.length==0 && localDates.length==0) {
                        /* Use PeerInfo comparable, lower is prefered */
                        if(logger.isLoggable(Level.FINE))
                            logger.fine("No Peer or Local dates");
                        int result = myPeerInfo.compareTo(peerInfo);
                        if(result<0) {
                            if(peerLogger.isLoggable(Level.FINE))
                                peerLogger.fine("Set ProvisionMonitor at "+
                                                "["+peerInfo.getAddress()+"] "+
                                                "as backup for "+
                                                "["+opString.getName()+"]");
                            peerMgr.setManaging(false);
                        } else {
                            if(peerLogger.isLoggable(Level.FINE))
                                peerLogger.fine("Set Local "+
                                                "as backup for "+
                                                "["+opString.getName()+"]");
                            localMgr.setManaging(false);
                            addAsBackup = true;
                        }

                    } else {
                        /* If our instance has deployed and the peer has not,
                         * set the peer to be a backup */
                        if(peerDates.length==0 && localDates.length>0) {
                            if(peerLogger.isLoggable(Level.FINE))
                                peerLogger.fine("No Peer deployment dates, set "+
                                                "ProvisionMonitor at "+
                                                "["+peerInfo.getAddress()+"] "+
                                                "as backup for "+
                                                "["+opString.getName()+"]");
                            peerMgr.setManaging(false);
                        }

                        /* Conversely, if the peer has deployed and we have not,
                         * set the local instance to be backup */
                        else if(peerDates.length>0 && localDates.length==0) {
                            if(peerLogger.isLoggable(Level.FINE))
                                peerLogger.fine("No Local deployment dates, set "+
                                                "Local as backup for "+
                                                "["+opString.getName()+"]");
                            localMgr.setManaging(false);
                            addAsBackup = true;
                        } else {
                            /* Both have deployed, get the latest date from both
                             * and compare */
                            Date lastPeerDate = peerDates[peerDates.length-1];
                            Date lastLocalDate = localDates[localDates.length-1];
                            /* Highly unlikely that the 2 Deployment times will 
                             * be exact, if they are, use the random number in
                             * PeerInfo */
                            if(lastPeerDate.equals(lastLocalDate)) {
                                /* Use PeerInfo comparable, lower is prefered */
                                if(peerLogger.isLoggable(Level.FINE))
                                    peerLogger.fine("Last deployment dates are equal");
                                int result = myPeerInfo.compareTo(peerInfo);
                                if(result<0) {
                                    if(peerLogger.isLoggable(Level.FINE))
                                        peerLogger.fine("Local is preferred, set "+
                                                        "ProvisionMonitor at "+
                                                        "["+peerInfo.getAddress()+"] "+
                                                        "as backup for "+
                                                        "["+opString.getName()+"]");
                                    peerMgr.setManaging(false);
                                } else {
                                    if(peerLogger.isLoggable(Level.FINE))
                                        peerLogger.fine("Peer is preferred, set "+
                                                        "Local as backup for "+
                                                        "["+opString.getName()+"]");
                                    localMgr.setManaging(false);
                                }
                            } else {
                                if(lastPeerDate.before(lastLocalDate)) {
                                    if(peerLogger.isLoggable(Level.FINE))
                                        peerLogger.fine("Peer deployed before Local, "+
                                                        "set Local "+
                                                        "as backup for "+
                                                        "["+opString.getName()+"]");
                                    localMgr.setManaging(false);
                                    addAsBackup = true;
                                } else {
                                    if(peerLogger.isLoggable(Level.FINE))
                                        peerLogger.fine("Local deployed before Peer, "+
                                                        "set ProvisionMonitor at "+
                                                        "["+peerInfo.getAddress()+"] "+
                                                        "as backup for "+
                                                        "["+opString.getName()+"]");
                                    peerMgr.setManaging(false);
                                }
                            }
                        }
                    }
                }

            } catch(OperationalStringException e) {
                /* This should not happen, log the occurance just the same */
                 peerLogger.log(Level.WARNING,
                               "OperationalStringException trying to resolve "+
                               "OperationalStringManager supremacy",
                               e);
            } catch(RemoteException e) {
                peerLogger.log(Level.WARNING,
                               "RemoteException trying to resolve "+
                               "OperationalStringManager supremacy", e);
            } finally {
                if(addAsBackup) {
                    /* Set the OperationalString to the OpStringManager */
                    localMgr.doUpdateOperationalString(opString);
                    /* Add OpStringManager to list of managers providing backup */
                    List<OpStringManager> list;
                    synchronized(opStringTable) {
                        if(opStringTable.containsKey(peer))
                            list = opStringTable.get(peer);
                        else
                            list = new ArrayList<OpStringManager>();
                        list.add(localMgr);
                        opStringTable.put(peer, list);
                    }
                }
            }
        }

    } // End class ProvisionMonitorPeer

    /**
     * Class that manages the persistence details behind saving and restoring
     * OperationalStrings
     */
    class OpStringLogHandler extends LogHandler implements SnapshotHandler {
        /** Collection of of recovered Operational Strings to add */
        Vector<OperationalString> recoveredOpstrings = new Vector<OperationalString>();
        /** Collection of of recovered Operational Strings to add */
        Vector<RecordHolder> updatedOpstrings = new Vector<RecordHolder>();
        /** flag to indicate whether OperationalStrings have been recovered */
        boolean opStringsRecovered = false;

        public void snapshot(OutputStream out) throws IOException {
            ObjectOutputStream oostream = new ObjectOutputStream(out);
            oostream.writeUTF(ProvisionMonitorImpl.class.getName());
            oostream.writeInt(LOG_VERSION);
            List<OperationalString> list = new ArrayList<OperationalString>();
            OperationalString[] opStrings = getOperationalStrings();
            list.addAll(Arrays.asList(opStrings));
            oostream.writeObject(new MarshalledObject<List<OperationalString>>(list));
            oostream.flush();
        }

        /**
         * Required method implementing the abstract recover() defined in
         * ReliableLog's associated LogHandler class. This callback is invoked
         * from the recover method of ReliableLog.
         */
        @SuppressWarnings("unchecked")
        public void recover(InputStream in) throws Exception {
            inRecovery = true;
            ObjectInputStream oistream = new ObjectInputStream(in);
            if(!ProvisionMonitorImpl.class.getName().equals(oistream.readUTF()))
                throw new IOException("Log from wrong implementation");
            if(oistream.readInt() != LOG_VERSION)
                throw new IOException("Wrong log format version");
            MarshalledObject mo = (MarshalledObject)oistream.readObject();
            List<OperationalString> list = (List<OperationalString>)mo.get();
            for (OperationalString opString : list) {
                if (logger.isLoggable(Level.FINER))
                    logger.finer("Recovered : " + opString.getName());
                //dumpOpString(opString);
                recoveredOpstrings.add(opString);
                opStringsRecovered = true;
            }
        }

        /**
         * Required method implementing the abstract applyUpdate() defined in
         * ReliableLog's associated LogHandler class.
         * <p>
         * During state recovery, the recover() method defined in the
         * ReliableLog class is invoked. That method invokes the method
         * recoverUpdates() which invokes the method readUpdates(). Both of
         * those methods are defined in ReliableLog. The method readUpdates()
         * retrieves a record from the log file and then invokes this method.
         */
        public void applyUpdate(Object update) throws Exception {
            if(update instanceof MarshalledObject) {
                RecordHolder holder =
                    (RecordHolder)((MarshalledObject)update).get();
                updatedOpstrings.add(holder);
                opStringsRecovered = true;
            }
        }

        /**
         * Called by <code>PersistentStore</code> after every update to give
         * server a chance to trigger a snapshot <br>
         * 
         * @param updateCount Number of updates since last snapshot
         */
        public void updatePerformed(int updateCount) {
            if(updateCount >= logToSnapshotThresh) {
                snapshotter.takeSnapshot();
            }
        }

        /**
         * Delegate snapshot request to PersistentStore
         */
        public void takeSnapshot() {
            snapshotter.takeSnapshot();
        }

        /**
         * Determine if OperationalString objects have been recovered or updated
         * 
         * @return boolean <code/true</code> if OperationalString objects have
         * been recovered or updated, otherwise <code>false</code>
         */
        boolean opStringsRecovered() {
            return (opStringsRecovered);
        }

        /**
         * Process recovered OperationalString objects
         */
        void processRecoveredOpStrings() {
            for(Enumeration e = recoveredOpstrings.elements();
                e.hasMoreElements();) {
                OperationalString opString = (OperationalString)e.nextElement();
                try {
                    if(!opStringExists(opString.getName())) {
                        Map<String, Throwable> map =
                            new HashMap<String, Throwable>();
                        addOperationalString(opString, map, null, null, null);
                        dumpOpStringError(map);
                    } else {
                        OpStringManager opMgr =
                            getOpStringManager(opString.getName());
                        Map map = opMgr.update(opString);
                        dumpOpStringError(map);
                    }
                } catch(Exception ex) {
                    logger.log(Level.WARNING,
                               "Processing recovered OperationalStrings", ex);
                }
            }
            recoveredOpstrings.clear();
        }

        /**
         * Process updated OperationalString objects
         *
         * @throws OperationalStringException if there are errors processing
         * the OperationalStrings
         */
        void processUpdatedOpStrings() throws OperationalStringException {
            for(Enumeration<RecordHolder> e = updatedOpstrings.elements();
                e.hasMoreElements();) {
                RecordHolder holder = e.nextElement();
                OperationalString opString = holder.getOperationalString();
                try {
                    if(holder.getAction() == RecordHolder.MODIFIED) {
                        if(!opStringExists(opString.getName())) {
                            Map<String, Throwable> map =
                                new HashMap<String, Throwable>();
                            addOperationalString(opString, map, null, null, null);
                            dumpOpStringError(map);
                        } else {
                            OpStringManager opMgr =
                                getOpStringManager(opString.getName());
                            Map map = opMgr.update(opString);
                            dumpOpStringError(map);
                        }
                    } else {
                        undeploy(opString.getName(), false);
                    }
                } catch(Exception ex) {
                    logger.log(Level.WARNING,
                               "Processing updated OperationalStrings", ex);
                }
            }
            updatedOpstrings.clear();
        }
    }


    /**
     * Use DeployHandlers to provide hot deployment capability
     */
    public class DeployMonitor {
        DeployHandler[] deployHandlers;
        ScheduledExecutorService deployExecutor;
        long lastRecordedTime;

        public DeployMonitor(DeployHandler[] deployHandlers,
                             long deployScan) {
            this.deployHandlers = deployHandlers;
            processDeployHandlers(null);
            lastRecordedTime = System.currentTimeMillis();
            deployExecutor = Executors.newSingleThreadScheduledExecutor();
            deployExecutor.scheduleAtFixedRate(
                new Runnable() {
                    public void run() {
                        processDeployHandlers(new Date(lastRecordedTime));
                        lastRecordedTime = System.currentTimeMillis();
                    }
                },
                0,
                deployScan,
                TimeUnit.MILLISECONDS);
        }

        void terminate() {
            if(deployExecutor!=null)
                deployExecutor.shutdownNow();
        }

        private void processDeployHandlers(Date from) {
            for(DeployHandler dHandler : deployHandlers) {
                List<OperationalString> opstrings = from==null?
                                                    dHandler.listofOperationalStrings():
                                                    dHandler.listofOperationalStrings(from);
                for(OperationalString opstring : opstrings) {
                    String action = null;
                    try {
                        Map<String, Throwable> result;
                        if(hasDeployed(opstring.getName())) {
                            action = "update";
                            OpStringManager mgr = getOpStringManager(opstring.getName());
                            result = mgr.doUpdateOperationalString(opstring);
                        } else {
                            action = "deploy";
                            result = deploy(opstring, null);
                        }
                        if(result.size()>0) {
                            for(Map.Entry<String, Throwable> entry :  result.entrySet()) {
                                logger.log(Level.WARNING,
                                           "Deploying service " +
                                           "["+entry.getKey()+"] resulted in " +
                                           "the following exception",
                                           entry.getValue());
                            }
                        }
                    } catch (OperationalStringException e) {
                        logger.log(Level.WARNING,
                                   "Unable to "+action+" ["+opstring.getName()+"]");
                    }
                }
            }
        }
    }
}
