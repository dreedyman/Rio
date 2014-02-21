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
package org.rioproject.monitor.service;

import com.sun.jini.config.Config;
import com.sun.jini.start.LifeCycle;
import net.jini.config.Configuration;
import net.jini.config.ConfigurationException;
import net.jini.core.event.EventRegistration;
import net.jini.core.event.UnknownEventException;
import net.jini.core.lease.LeaseDeniedException;
import net.jini.core.lease.UnknownLeaseException;
import net.jini.core.lookup.ServiceID;
import net.jini.export.Exporter;
import net.jini.lookup.entry.ServiceInfo;
import net.jini.security.TrustVerifier;
import net.jini.security.proxytrust.ServerProxyTrust;
import org.rioproject.RioVersion;
import org.rioproject.config.Constants;
import org.rioproject.deploy.*;
import org.rioproject.event.EventDescriptor;
import org.rioproject.event.EventHandler;
import org.rioproject.impl.client.JiniClient;
import org.rioproject.impl.event.DispatchEventHandler;
import org.rioproject.impl.jmx.JMXUtil;
import org.rioproject.impl.jmx.MBeanServerFactory;
import org.rioproject.impl.opstring.OAR;
import org.rioproject.impl.opstring.OpStringLoader;
import org.rioproject.impl.service.ServiceResource;
import org.rioproject.impl.servicebean.ServiceBeanActivation;
import org.rioproject.impl.servicebean.ServiceBeanActivation.LifeCycleManager;
import org.rioproject.impl.servicebean.ServiceBeanAdapter;
import org.rioproject.impl.system.ComputeResource;
import org.rioproject.impl.util.BannerProvider;
import org.rioproject.impl.util.BannerProviderImpl;
import org.rioproject.impl.watch.GaugeWatch;
import org.rioproject.impl.watch.PeriodicWatch;
import org.rioproject.impl.watch.ThreadDeadlockMonitor;
import org.rioproject.loader.ServiceClassLoader;
import org.rioproject.monitor.ProvisionFailureEvent;
import org.rioproject.monitor.ProvisionMonitor;
import org.rioproject.monitor.ProvisionMonitorEvent;
import org.rioproject.monitor.proxy.ProvisionMonitorProxy;
import org.rioproject.monitor.service.handlers.DeployHandler;
import org.rioproject.monitor.service.handlers.DeployHandlerMonitor;
import org.rioproject.monitor.service.handlers.FileSystemOARDeployHandler;
import org.rioproject.monitor.service.peer.ProvisionMonitorPeer;
import org.rioproject.monitor.service.persistence.StateManager;
import org.rioproject.monitor.service.tasks.InitialOpStringLoadTask;
import org.rioproject.monitor.service.tasks.TaskTimer;
import org.rioproject.opstring.OperationalString;
import org.rioproject.opstring.OperationalStringException;
import org.rioproject.opstring.OperationalStringManager;
import org.rioproject.resolver.*;
import org.rioproject.servicebean.ServiceBeanContext;
import org.rioproject.system.ResourceCapability;
import org.rioproject.util.RioManifest;
import org.rioproject.util.TimeUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.MBeanServer;
import javax.management.openmbean.*;
import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.net.MalformedURLException;
import java.net.URL;
import java.rmi.*;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * The ProvisionMonitor service provides the capability to deploy and monitor
 * OperationalStrings.
 *
 * @author Dennis Reedy
 */
public class ProvisionMonitorImpl extends ServiceBeanAdapter implements ProvisionMonitor,
                                                                        ProvisionMonitorImplMBean,
                                                                        ServerProxyTrust {
    /** Component name we use to find items in the configuration */
    private static final String CONFIG_COMPONENT = "org.rioproject.monitor";
    /** ProvisionMonitor logger. */
    private static Logger logger = LoggerFactory.getLogger(ProvisionMonitorImpl.class);
    /** The provisioner to use for provisioning */
    private ServiceProvisioner provisioner;
    /** OpStringLoader for loading XML OperationalStrings */
    private OpStringLoader opStringLoader;
    /** A watch to track how long it takes to provision services */
    private GaugeWatch provisionWatch;
    /** Handles discovery and synchronization with other ProvisionMonitors */
    private ProvisionMonitorPeer provisionMonitorPeer;
    private final OpStringManagerController opStringMangerController = new OpStringManagerController();
    private DeploymentVerifier deploymentVerifier;
    private StateManager stateManager;
    /** A Timer used to schedule load tasks */
    private TaskTimer taskTimer;
    private LifeCycle lifeCycle;
    private DeployHandlerMonitor deployMonitor;

    /**
     * Create a ProvisionMonitor
     *
     * @throws Exception If the ProvisionMonitorImpl cannot be created
     */
    @SuppressWarnings("unused")
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
    @SuppressWarnings("unused")
    public ProvisionMonitorImpl(String[] configArgs, LifeCycle lifeCycle) throws Exception {
        super();
        this.lifeCycle = lifeCycle;
        bootstrap(configArgs);
    }

    /**
     * Get the ServiceBeanContext and bootstrap the ProvisionMonitor
     *
     * @param configArgs configuration arguments
     *
     * @throws Exception If bootstrapping fails
     */
    private void bootstrap(String[] configArgs) throws Exception {
        context = ServiceBeanActivation.getServiceBeanContext(CONFIG_COMPONENT,
                                                              "ProvisionMonitor",
                                                              configArgs,
                                                              getClass().getClassLoader());
        BannerProvider bannerProvider =
            (BannerProvider)context.getConfiguration().getEntry(CONFIG_COMPONENT,
                                                                "bannerProvider",
                                                                BannerProvider.class,
                                                                new BannerProviderImpl());
        logger.info(bannerProvider.getBanner(context.getServiceElement().getName()));
        start(context);
        LifeCycleManager lMgr = (LifeCycleManager)context.getServiceBeanManager().getDiscardManager();
        lMgr.register(getServiceProxy(), context);
    }

    /**
     * Override destroy to ensure that all OpStringManagers are shutdown as well
     */
    @Override
    public void destroy() {
        logger.debug("ProvisionMonitor: destroy() notification");
        /* stop the provisioner */
        if(provisioner!=null)
            provisioner.terminate();
        /* Cleanup opStringManagers */
        opStringMangerController.shutdownAllManagers();
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
                logger.warn("While destroying persistent store", t);
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
        if(inShutdown.get())
            return null;
        Object adminProxy = null;
        try {
            if (admin == null) {
                Exporter adminExporter = getAdminExporter();
                if (contextMgr != null)
                    admin = new ProvisionMonitorAdminImpl(this,
                                                          adminExporter,
                                                          contextMgr.getContextAttributeLogHandler());
                else
                    admin = new ProvisionMonitorAdminImpl(this, adminExporter);
            }
            adminProxy = admin.getServiceAdmin();
        } catch (Exception e) {
            logger.warn("Getting ProvisionMonitorAdminImpl", e);
        }
        return (adminProxy);
    }

    /**
     * Get the ComputeResource associated with this ProvisionMonitor
     *
     * @return The ComputeResource associated with this ProvisionMonitor
     */
    public ComputeResource getComputeResource() {
        return(computeResource);
    }


    /**
     * Override parent's method to return <code>TrustVerifier</code> which can
     * be used to verify that the given proxy to this service can be trusted
     *
     * @return TrustVerifier The TrustVerifier used to verify the proxy
     *
     */
    public TrustVerifier getProxyVerifier() {
        return (new ProvisionMonitorProxy.Verifier(getExportedProxy()));
    }

    /**
     * Override ServiceBeanAdapter createProxy to return a ProvisionMonitor
     * Proxy
     */
    @Override
    protected Object createProxy() {
        Object proxy = ProvisionMonitorProxy.getInstance((ProvisionMonitor)getExportedProxy(), getUuid());
        /* Get the registry port */
        String sPort = System.getProperty(Constants.REGISTRY_PORT, "0");
        int registryPort = Integer.parseInt(sPort);
        String name = context.getServiceBeanConfig().getName();
        if(registryPort!=0) {
            try {
                Registry registry = LocateRegistry.getRegistry(registryPort);
                try {
                    registry.bind(name, (Remote)proxy);
                    logger.debug("Bound to RMI Registry on port={}", registryPort);
                } catch(AlreadyBoundException e) {
                    /*ignore */
                }
            } catch(AccessException e) {
                logger.warn("Binding {} to RMI Registry", name, e);
            } catch(RemoteException e) {
                logger.warn("Binding {} to RMI Registry", name, e);
            }
        } else {
            logger.debug("RMI Registry property not set, unable to bind {}", name);
        }
        return(proxy);
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

    /*
     * @see org.rioproject.monitor.DeployAdmin#getOperationalStringManagers
     */
    public OperationalStringManager[] getOperationalStringManagers() {
        if(opStringMangerController.getOpStringManagers().length==0)
            return (new OperationalStringManager[0]);
        ArrayList<OpStringManager> list = new ArrayList<OpStringManager>();
        list.addAll(Arrays.asList(opStringMangerController.getOpStringManagers()));
        
        /* Get the OperationalStringManager instances that may be initializing
         * as well */
        OpStringManager[] pendingMgrs = opStringMangerController.getPendingOpStringManagers();
        
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
    public OperationalStringManager getOperationalStringManager(String name) throws OperationalStringException {
        if(name==null)
            throw new IllegalArgumentException("name is null");
        OperationalStringManager opStringManager = null;
        OpStringManager opMgr = opStringMangerController.getOpStringManager(name);
        if(opMgr!=null && opMgr.isActive()) {
            opStringManager = opMgr.getProxy();
        } else  {
            try {
                DeployAdmin dAdmin = opStringMangerController.getPrimaryDeployAdmin(name);
                if(dAdmin!=null) {
                    OperationalStringManager mgr = dAdmin.getOperationalStringManager(name);
                    if(mgr.isManaging()) {
                        opStringManager = mgr;
                    }
                }
            } catch(RemoteException e) {
                    logger.debug("Communicating to peer during getOperationalStringManager for {}", name, e);
            } catch(OperationalStringException e) {
                /* ignore */
            }
        }

        if(opStringManager==null)
            throw new OperationalStringException(String.format("Unmanaged OperationalString [%s]",name ), false);
        return(opStringManager);
    }

    /*
     * @see org.rioproject.monitor.ProvisionMonitorImplMBean#deploy
     */
    public DeploymentResult deploy(String opStringLocation) throws MalformedURLException {
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

        DeploymentResult deploymentResult;
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
            deploymentResult = deploy(opStringURL, null);
        } catch(OperationalStringException e) {
            Throwable cause =  e.getCause();
            if(cause==null)
                cause = e;
            Map<String, Throwable> m = new HashMap<String, Throwable>();
            m.put(cause.getClass().getName(), cause);
            deploymentResult = new DeploymentResult(null, m);
            logger.warn("Deploying {}", opStringURL, e);
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
        return deploymentResult;
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
                Resolver r = ResolverHelper.getResolver();
                opStringURL = r.getLocation(a, "oar");
                if(opStringURL==null)
                    throw new OperationalStringException(String.format("Artifact %s not resolvable", a));
            } catch (ResolverException e) {
                throw new OperationalStringException(e.getLocalizedMessage(), e);
            }
        }
        return opStringURL;
    }

    /*
     * @see org.rioproject.monitor.ProvisionMonitorImplMBean#deploy
     */
    public DeploymentResult deploy(String opStringLocation, ServiceProvisionListener listener)
        throws OperationalStringException {
        if(opStringLocation == null)
            throw new IllegalArgumentException("OperationalString location cannot be null");

        URL opStringURL = getArtifactURL(opStringLocation);
        if(opStringURL==null) {
            try {
                opStringURL = new URL(opStringLocation);
            } catch (MalformedURLException e) {
                throw new OperationalStringException(String.format("Failed to create URL from %s", opStringLocation), e);
            }
        }
        return deploy(opStringURL, listener);
    }


    /*
     * @see org.rioproject.monitor.DeployAdmin#deploy
     */
    public DeploymentResult deploy(final URL opStringUrl, ServiceProvisionListener listener)
    throws OperationalStringException {
        if(opStringUrl == null)
            throw new IllegalArgumentException("OperationalString URL cannot be null");
        DeploymentResult deploymentResult;
        try {
            OAR oar;
            URL opStringUrlToUse = opStringUrl;
            if(opStringUrl.toExternalForm().endsWith("oar")) {
                oar = new OAR(opStringUrl);
                StringBuilder sb = new StringBuilder();
                sb.append("jar:").append(oar.getURL().toExternalForm()).append("!/").append(oar.getOpStringName());
                opStringUrlToUse = new URL(sb.toString());
            }
            OperationalString[] opStrings = opStringLoader.parseOperationalString(opStringUrlToUse);
            if(opStrings != null && opStrings.length>0) {
                deploymentResult = deploy(opStrings[0], listener);
            } else {
                throw new OperationalStringException("After parsing "+opStringUrl.toExternalForm()+", " +
                                                     "there were no OperationalString returned");
            }
        } catch(Exception e) {
            logger.warn("Problem opening or deploying {}", opStringUrl.toExternalForm(), e);
            throw new OperationalStringException("Deploying OperationalString", e);
        }
        return deploymentResult;
    }

    /*
     * @see org.rioproject.monitor.DeployAdmin#deploy
     */
    public DeploymentResult deploy(OperationalString opString, ServiceProvisionListener listener)
    throws OperationalStringException {
        if(opString == null)
            throw new IllegalArgumentException("OperationalString cannot be null");
        DeploymentResult deploymentResult;
        try {
            if(!opStringMangerController.opStringExists(opString.getName())) {
                logger.info("Deploying Operational String [{}]", opString.getName());

                DeployRequest request = new DeployRequest(opString, (RemoteRepository[])null);
                deploymentVerifier.verifyDeploymentRequest(request);

                Map<String, Throwable> map = new HashMap<String, Throwable>();
                OpStringManager manager = opStringMangerController.addOperationalString(opString, map, null, null, listener);
                deploymentResult = new DeploymentResult(manager.getOperationalStringManager(), map);
            } else {
                if(logger.isInfoEnabled())
                    logger.info("Operational String [{}] already deployed", opString.getName());
                OperationalStringManager manager = opStringMangerController.getOpStringManager(opString.getName()).getOperationalStringManager();
                deploymentResult = new DeploymentResult(manager, null);
            }
        } catch(Exception e) {
            logger.warn("Deploying OperationalString [{}]", opString.getName(), e);
            if(!(e instanceof OperationalStringException))
                throw new OperationalStringException(String.format("Deploying OperationalString [%s]", opString.getName()), e);
            throw (OperationalStringException)e;
        }
        return deploymentResult;
    }


    /*
     * @see org.rioproject.monitor.ProvisionMonitorImplMBean#undeploy(String)
     */
    public boolean undeploy(String opStringName) {
        boolean undeployed = false;
        try {
            undeployed = undeploy(opStringName, true);
        } catch(OperationalStringException e) {
            logger.warn("Undeploying [{}]", opStringName, e);
        }
        return(undeployed);
    }

    /*
     * @see org.rioproject.monitor.DeployAdmin#undeploy
     */
    public boolean undeploy(final String name, boolean terminate) throws OperationalStringException  {
        if(name == null)
            throw new IllegalArgumentException("name cannot be null");
        String opStringName = name;
        logger.info("Undeploying {}", opStringName);
        URL artifactURL = getArtifactURL(name);
        if(artifactURL!=null) {
            try {
                OAR oar = new OAR(artifactURL);
                OperationalString[] opstring = oar.loadOperationalStrings();
                opStringName = opstring[0].getName();
            } catch(Exception e) {
                throw new OperationalStringException(String.format("Unable to undeploy, cannot parse/load [%s]",
                                                                   opStringName));
            }
        }
        boolean undeployed = false;
        OpStringManager opMgr = opStringMangerController.getOpStringManager(opStringName);
        logger.trace("OpStringManager: {}", opMgr);
        if(opMgr == null || (!opMgr.isActive())) {
            try {
                DeployAdmin dAdmin = opStringMangerController.getPrimaryDeployAdmin(opStringName);
                if(dAdmin!=null) {
                    OperationalStringManager mgr = dAdmin.getOperationalStringManager(opStringName);
                    if(mgr.isManaging()) {
                        dAdmin.undeploy(name);
                        undeployed = true;
                    }
                }
            } catch(RemoteException e) {
                logger.debug("Communicating to peer during undeployment of [{}]", opStringName, e);
            } catch(OperationalStringException e) {
                /* ignore */
            }

        } else {
            opStringMangerController.undeploy(opMgr, terminate);
            undeployed = true;
        }
        if(!undeployed) {
            throw new OperationalStringException(String.format("No deployment for [%s] found", opStringName));
        }
        return true;
    }

    /*
     * @see org.rioproject.monitor.DeployAdmin#hasDeployed
     */
    public boolean hasDeployed(String opStringName) {
        if(opStringName == null)
            throw new IllegalArgumentException("Parameters cannot be null");
        for(OpStringManager opMgr : opStringMangerController.getOpStringManagers()) {
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
        String[] itemNames = new String[] {"Name", "Status", "Role", "Deployed"};
        OpenType[] itemTypes = new OpenType[]{SimpleType.STRING, SimpleType.STRING, SimpleType.STRING, SimpleType.DATE};
        TabularDataSupport tabularDataSupport = null;
        try {
            CompositeType row = new CompositeType("Deployments", "Deployments", itemNames, itemNames, itemTypes);

            TabularType tabularType = new TabularType("Deployments", "Deployments", row, new String[]{"Name"});
            tabularDataSupport = new TabularDataSupport(tabularType);
            for (OpStringManager mgr : opStringMangerController.getPendingOpStringManagers()) {
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
                }
                String role = (mgr.isActive() ? "Primary" : "Backup");
                Object[] data = new Object[]{mgr.getName(), sStatus, role, deployed};
                CompositeData compositeData = new CompositeDataSupport(row, itemNames, data);
                tabularDataSupport.put(compositeData);
            }

        } catch(OpenDataException e) {
            logger.warn(e.toString(), e);
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
    public boolean assignBackupFor(ProvisionMonitor primary){
        return (provisionMonitorPeer.addAsBackupFor(primary));
    }

    /*
     * @see org.rioproject.monitor.ProvisionMonitor#removeBackupFor
     */
    public boolean removeBackupFor(ProvisionMonitor primary) {
        return (provisionMonitorPeer.removeAsBackupFor(primary));
    }

    /*
     * @see org.rioproject.monitor.ProvisionMonitor#update
     */
    public void update(PeerInfo peerInfo) {
        provisionMonitorPeer.peerUpdated(peerInfo);
    }

    /*
     * @see org.rioproject.deploy.ProvisionManager#register
     */
    public EventRegistration register(MarshalledObject<ServiceBeanInstantiator> instantiator,
                                      MarshalledObject handback,
                                      ResourceCapability resourceCapability,
                                      List<DeployedService> deployedServices,
                                      int serviceLimit,
                                      long duration) throws LeaseDeniedException, RemoteException {
        return (provisioner.register(instantiator,
                                     handback,
                                     resourceCapability,
                                     deployedServices,
                                     serviceLimit,
                                     duration));
    }

    /*
     * @see org.rioproject.deploy.ProvisionManager#update
     */
    public void update(ServiceBeanInstantiator instantiator,
                       ResourceCapability resourceCapability,
                       List<DeployedService> deployedServices,
                       int serviceLimit) throws UnknownLeaseException, RemoteException {
        /* delegate to provisioner */
        provisioner.handleFeedback(instantiator, resourceCapability, deployedServices, serviceLimit);
    }

    public Collection<MarshalledObject<ServiceBeanInstantiator>> getWrappedServiceBeanInstantiators() {
        Collection<MarshalledObject<ServiceBeanInstantiator>> marshalledWrappers =
            new ArrayList<MarshalledObject<ServiceBeanInstantiator>>();
        ServiceResource[] resources = provisioner.getServiceResourceSelector().getServiceResources();
        for(ServiceResource s : resources) {
            marshalledWrappers.add(((InstantiatorResource)s.getResource()).getWrappedServiceBeanInstantiator());
        }
        return marshalledWrappers;
    }

    /*
    * @see org.rioproject.deploy.ProvisionManager#getServiceBeanInstantiators
    */
    public ServiceBeanInstantiator[] getServiceBeanInstantiators() {
        ServiceResource[] resources = provisioner.getServiceResourceSelector().getServiceResources();
        List<ServiceBeanInstantiator> list = new ArrayList<ServiceBeanInstantiator>();
        for(ServiceResource s : resources) {
            list.add(((InstantiatorResource)s.getResource()).getServiceBeanInstantiator());
        }        
        return list.toArray(new ServiceBeanInstantiator[list.size()]);
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
            String logDirName = (String)context.getConfiguration().getEntry(CONFIG_COMPONENT,
                                                                            "logDirectory",
                                                                            String.class,
                                                                            null);
            if(logDirName != null) {
                stateManager = new StateManager(logDirName, opStringMangerController);
                logger.info("ProvisionMonitor: using absolute logdir path [{}]", store.getStoreLocation());
                store.snapshot();
                super.initialize(context, store);
            } else {
                super.initialize(context);
            }
            Configuration config = context.getConfiguration();
            deploymentVerifier = new DeploymentVerifier(config, context.getDiscoveryManagement());
            ProvisionMonitorEventProcessor eventProcessor = new ProvisionMonitorEventProcessor(config);
            provisionWatch = new GaugeWatch("Provision Clock", config);
            getWatchRegistry().register(provisionWatch);

            /* Wire up event handlers for the ProvisionMonitorEvent and the ProvisionFailureEvent */
            EventDescriptor clientEventDesc = ProvisionMonitorEvent.getEventDescriptor();
            getEventTable().put(clientEventDesc.eventID, eventProcessor.getMonitorEventHandler());

            EventDescriptor failureEventDesc = ProvisionFailureEvent.getEventDescriptor();
            /* EventHandler for ProvisionFailureEvent consumers */
            EventHandler failureHandler = new DispatchEventHandler(failureEventDesc, config);
            getEventTable().put(failureEventDesc.eventID, failureHandler);

            registerEventAdapters();

            provisioner = new ServiceProvisioner(config, getEventProxy(), failureHandler, provisionWatch);

            opStringMangerController.setConfig(config);
            opStringMangerController.setEventProcessor(eventProcessor);
            opStringMangerController.setServiceProvisioner(provisioner);
            opStringMangerController.setUuid(getUuid());
            opStringMangerController.setStateManager(stateManager);
            opStringMangerController.setServiceProxy(getEventProxy());
            opStringMangerController.setDeploymentVerifier(deploymentVerifier);

            if(System.getProperty(Constants.CODESERVER)==null) {
                System.setProperty(Constants.CODESERVER, context.getExportCodebase());
                logger.warn("The system property [{}] has not been set, it has been resolved to: {}",
                               Constants.CODESERVER, System.getProperty(Constants.CODESERVER));

            }

            /*
             * Add attributes
             */

            /* Check for JMXConnection */
            addAttributes(JMXUtil.getJMXConnectionEntries());
            
            addAttribute(ProvisionMonitorEvent.getEventDescriptor());
            addAttribute(failureEventDesc);

            /* Utility for loading OperationalStrings */
            opStringLoader = getOpStringLoader();

            /*
            * If we have a persistent store, process recovered or updated
            * OperationalString elements
            */
            if(stateManager!=null) {
                stateManager.processRecoveredOpStrings();
                stateManager.processUpdatedOpStrings();
            }

            /*
            * Start the ProvisionMonitorPeer
            */
            provisionMonitorPeer = new ProvisionMonitorPeer();
            provisionMonitorPeer.setComputeResource(computeResource);
            provisionMonitorPeer.setOpStringMangerController(opStringMangerController);
            provisionMonitorPeer.setEventProcessor(eventProcessor);

            provisionMonitorPeer.setDiscoveryManagement(context.getDiscoveryManagement());
            provisionMonitorPeer.setServiceProxy(getEventProxy());
            provisionMonitorPeer.setConfig(config);
            provisionMonitorPeer.initialize();

            opStringMangerController.setProvisionMonitorPeer(provisionMonitorPeer);

            /* Create the task Timer */
            taskTimer = TaskTimer.getInstance();

            /*
             * Setup the DeployHandlerMonitor with DeployHandlers
             */
            long deployMonitorPeriod = TimeUnit.SECONDS.toMillis(30);
            try {
                deployMonitorPeriod = Config.getLongEntry(config,
                                                          CONFIG_COMPONENT,
                                                          "deployMonitorPeriod",
                                                          deployMonitorPeriod,
                                                          -1,
                                                          Long.MAX_VALUE);
            } catch(ConfigurationException e) {
                logger.warn("Non-fatal exception getting deployMonitorPeriod, using default value of [{}] " +
                            "milliseconds. Continuing on with initialization.",
                            deployMonitorPeriod, e);
            }
            if(logger.isDebugEnabled())
                logger.debug("Configured to scan for OAR deployments every {}", TimeUtil.format(deployMonitorPeriod));

            if(deployMonitorPeriod>0) {
                String rioHome = System.getProperty("RIO_HOME");
                if(!rioHome.endsWith("/"))
                    rioHome = rioHome+"/";
                File deployDir = new File(rioHome+"deploy");
                DeployHandler fsDH = new FileSystemOARDeployHandler(deployDir, deploymentVerifier);
                DeployHandler[] deployHandlers = (DeployHandler[]) config.getEntry(CONFIG_COMPONENT,
                                                                                   "deployHandlers",
                                                                                   DeployHandler[].class,
                                                                                   new DeployHandler[]{fsDH});
                deployMonitor = new DeployHandlerMonitor(deployHandlers,
                                                         deployMonitorPeriod,
                                                         opStringMangerController,
                                                         getLocalDeployAdmin());

            } else {
                logger.info("OAR hot deploy capabilities have been disabled");
            }
            /* Get the timeout value for loading OperationalStrings */
            long initialOpStringLoadDelay = TimeUnit.SECONDS.toMillis(5);
            try {
                initialOpStringLoadDelay = Config.getLongEntry(config,
                                                               CONFIG_COMPONENT,
                                                               "initialOpStringLoadDelay",
                                                               initialOpStringLoadDelay,
                                                               1,
                                                               Long.MAX_VALUE);
            } catch(ConfigurationException e) {
                logger.warn("Exception getting initialOpStringLoadDelay", e);
            }

            String[] initialOpStrings = new String[]{};
            try {
                initialOpStrings = (String[]) config.getEntry(CONFIG_COMPONENT,
                                                              "initialOpStrings",
                                                              String[].class,
                                                              initialOpStrings);
            } catch(ConfigurationException e) {
                logger.warn("Exception getting initialOpStrings", e);
            }
            if(logger.isDebugEnabled()) {
                StringBuilder builder = new StringBuilder();
                for(String s : initialOpStrings) {
                    if(builder.length()>0)
                        builder.append(", ");
                    builder.append(s);
                }
                logger.debug("initialOpStrings=[{}], initialOpStringLoadDelay={}",
                              builder.toString(), initialOpStringLoadDelay);
            }

            /*
             * Schedule the task to Load any configured OperationalStrings
             */
            long now = System.currentTimeMillis();
            DeployAdmin dAdmin = getLocalDeployAdmin();
            if(initialOpStrings.length>0)
                taskTimer.schedule(new InitialOpStringLoadTask(initialOpStrings,
                                                               dAdmin,
                                                               provisionMonitorPeer,
                                                               opStringMangerController,
                                                               stateManager),
                                   new Date(now+initialOpStringLoadDelay));
            /*
            * If we were booted without a serviceID (perhaps using RMI
            * Activation), then create one
            */
            if(serviceID == null) {
                serviceID = new ServiceID(getUuid().getMostSignificantBits(), getUuid().getLeastSignificantBits());
                logger.debug("Created new ServiceID: {}", serviceID.toString());
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

            logger.info("Started Provision Monitor [{}]", JiniClient.getDiscoveryAttributes(context));
        } catch(Exception e) {
            logger.error("Unrecoverable initialization exception", e);
            destroy();
        }
    }

    private DeployAdmin getLocalDeployAdmin() {
        DeployAdmin deployAdmin = null;
        try {
            deployAdmin = (DeployAdmin) ((ProvisionMonitor)getServiceProxy()).getAdmin();
        } catch (RemoteException e) {
            logger.warn("While trying to get the DeployAdmin", e);
        }
        return deployAdmin;
    }

    private void registerEventAdapters() throws LeaseDeniedException, UnknownEventException, RemoteException {
        // translate ProvisionFailureEvents to notifications
        EventDescriptor provisionFailureEventDescriptor = new EventDescriptor(ProvisionFailureEvent.class,
                                                                              ProvisionFailureEvent.ID);
        ProvisionFailureEventAdapter provisionFailureEventAdapter =
            new ProvisionFailureEventAdapter(objectName, getNotificationBroadcasterSupport());
        register(provisionFailureEventDescriptor, provisionFailureEventAdapter, null, Long.MAX_VALUE);

        // register notification info
        mbeanNoticationInfoList.add(provisionFailureEventAdapter.getNotificationInfo());

        // translate ProvisionMonitorEvents to notifications
        EventDescriptor provisionMonitorEventDescriptor = new EventDescriptor(ProvisionMonitorEvent.class,
                                                                              ProvisionMonitorEvent.ID
        );
        ProvisionMonitorEventAdapter provisionMonitorEventAdapter =
            new ProvisionMonitorEventAdapter(objectName, getNotificationBroadcasterSupport());

        register(provisionMonitorEventDescriptor, provisionMonitorEventAdapter, null, Long.MAX_VALUE);
        //register notification info
        mbeanNoticationInfoList.add(provisionMonitorEventAdapter.getNotificationInfo());
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
}
