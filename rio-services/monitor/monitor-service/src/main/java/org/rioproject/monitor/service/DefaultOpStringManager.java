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

import com.sun.jini.proxy.BasicProxyTrustVerifier;
import net.jini.config.Configuration;
import net.jini.config.ConfigurationException;
import net.jini.config.EmptyConfiguration;
import net.jini.export.Exporter;
import net.jini.id.Uuid;
import net.jini.security.BasicProxyPreparer;
import net.jini.security.ProxyPreparer;
import net.jini.security.TrustVerifier;
import net.jini.security.proxytrust.ServerProxyTrust;
import org.rioproject.deploy.*;
import org.rioproject.impl.config.ExporterConfig;
import org.rioproject.impl.opstring.OAR;
import org.rioproject.impl.opstring.OpString;
import org.rioproject.impl.service.ServiceResource;
import org.rioproject.monitor.ProvisionMonitor;
import org.rioproject.monitor.ProvisionMonitorEvent;
import org.rioproject.monitor.service.channel.ServiceChannel;
import org.rioproject.monitor.service.channel.ServiceChannelEvent;
import org.rioproject.monitor.service.channel.ServiceChannelListener;
import org.rioproject.monitor.service.persistence.StateManager;
import org.rioproject.monitor.service.tasks.RedeploymentTask;
import org.rioproject.monitor.service.tasks.TaskTimer;
import org.rioproject.opstring.*;
import org.rioproject.resolver.RemoteRepository;
import org.rioproject.resolver.ResolverHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.rmi.RemoteException;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * The DefaultOpStringManager provides the management for an OperationalString that
 * has been deployed to the ProvisionMonitor
 */
@SuppressWarnings("PMD.ConstructorCallsOverridableMethod")
public class DefaultOpStringManager implements OperationalStringManager, OpStringManager, ServerProxyTrust {
    /** Component name we use to find items in the configuration */
    static final String CONFIG_COMPONENT = "org.rioproject.monitor";
    /** Logger */
    static final String LOGGER = "org.rioproject.monitor";
    /** ProvisionMonitor logger. */
    static Logger logger = LoggerFactory.getLogger(LOGGER);

    private OperationalString opString;
    /**
     * Collection of ServiceElementManager instances
     */
    private final List<ServiceElementManager> svcElemMgrs = new ArrayList<ServiceElementManager>();
    /**
     * Collection of nested DefaultOpStringManager instances
     */
    private final List<OpStringManager> nestedManagers = new ArrayList<OpStringManager>();
    /**
     * The DefaultOpStringManager parents for this DefaultOpStringManager
     */
    private final List<OpStringManager> parents = new ArrayList<OpStringManager>();
    /**
     * Property that indicates the mode of the DefaultOpStringManager. If active is
     * true, the DefaultOpStringManager will inform it's ServiceElementmanager
     * instances to actively provision services. If active is false, the
     * DefaultOpStringManager will inform its ServiceElementManager instances to
     * keep track of the service described by it's ServiceElement object but
     * not issue provision requests
     */
    private final AtomicBoolean active = new AtomicBoolean(true);
    /**
     * The Exporter for the OperationalStringManager
     */
    private Exporter exporter;
    /**
     * Object supporting remote semantics required for an
     * OperationalStringManager
     */
    private final OperationalStringManager proxy;
    /**
     * A List of scheduled TimerTasks
     */
    private final List<TimerTask> scheduledTaskList = Collections.synchronizedList(new ArrayList<TimerTask>());
    /**
     * A list a deployed Dates
     */
    private final List<Date> deployDateList = Collections.synchronizedList(new ArrayList<Date>());
    /**
     * Local copy of the deployment status of the OperationalString
     */
    private int deployStatus;
    /**
     * ProxyPreparer for ServiceProvisionListener proxies
     */
    private ProxyPreparer serviceProvisionListenerPreparer;
    /** The associated OperationalString archive (if any) */
    private OAR oar;
    /** The service proxy for the ProvisionMonitor */
    private ProvisionMonitor serviceProxy;
    private final Configuration config;
    private ProvisionMonitorEventProcessor eventProcessor;
    private final OpStringManagerController opStringMangerController;
    private StateManager stateManager;
    private ServiceProvisioner provisioner;
    private Uuid uuid;
    private final boolean standAlone;

    /**
     * Create an DefaultOpStringManager, making it available to receive incoming
     * calls supporting the OperationalStringManager interface
     *
     * @param opString The OperationalString to manage
     * @param parent   The DefaultOpStringManager parent. May be null
     * @param mode     Whether the OperationalStringManager is the active manager
     * @param config   Configuration object
     * @param opStringMangerController The managing entity for OpStringManagers
     * @throws java.rmi.RemoteException if the DefaultOpStringManager cannot export itself
     */
    public DefaultOpStringManager(final OperationalString opString,
                                  final OpStringManager parent,
                                  final boolean mode,
                                  final Configuration config,
                                  final OpStringManagerController opStringMangerController) throws IOException {

        this.config = config;
        this.opStringMangerController = opStringMangerController;
        Configuration myConfig = (config == null ? EmptyConfiguration.INSTANCE : config);
        try {
            exporter = ExporterConfig.getExporter(config, CONFIG_COMPONENT, "opStringManagerExporter");
            if (logger.isDebugEnabled())
                logger.debug("Deployment [{}] using exporter {}", opString.getName(), exporter);

            /* Get the ProxyPreparer for ServiceProvisionListener instances */
            serviceProvisionListenerPreparer = (ProxyPreparer) myConfig.getEntry(CONFIG_COMPONENT,
                                                                                 "serviceProvisionListenerPreparer",
                                                                                 ProxyPreparer.class,
                                                                                 new BasicProxyPreparer());
        } catch (ConfigurationException e) {
            logger.warn("Getting opStringManager Exporter", e);
        }

        proxy = (OperationalStringManager) exporter.export(this);
        this.opString = opString;
        this.active.set(mode);
        if (parent != null) {
            addParent(parent);
            parent.addNested(this);
            standAlone = false;
        } else {
            standAlone = opString.getNestedOperationalStrings().length==0;
        }
        logger.info("Manager for {} standAlone: {}", opString.getName(), standAlone);
        if (opString.loadedFrom() != null &&
            opString.loadedFrom().toExternalForm().startsWith("file") &&
            opString.loadedFrom().toExternalForm().endsWith(".oar")) {
            File f = new File(opString.loadedFrom().getFile());
            try {
                oar = new OAR(f);
                oar.setDeployDir(f.getParent());
            } catch (Exception e) {
                logger.warn("Could no create OAR", e);
            }
        }
    }

    public OperationalStringManager getOperationalStringManager() {
        return proxy;
    }

    public OAR getOAR() {
        return oar;
    }

    void setServiceProxy(final ProvisionMonitor serviceProxy) {
        this.serviceProxy = serviceProxy;
    }

    void setEventProcessor(final ProvisionMonitorEventProcessor eventProcessor) {
        this.eventProcessor = eventProcessor;
    }

    void setStateManager(final StateManager stateManager) {
        this.stateManager = stateManager;
    }

    /**
     * @see OpStringManager#getProxy()
     */
    public OperationalStringManager getProxy() {
        return (proxy);
    }

    /**
     * Set the active mode. If the new mode is not equal to the old mode,
     * iterate through the Collection of ServiceElementManager instances and
     * set their mode to be equal to the DefaultOpStringManager mode
     *
     * @param newActive the new mode
     */
    public synchronized void setActive(final boolean newActive) {
        synchronized (this) {
            if (active.get() != newActive) {
                active.set(newActive);
                List<ServiceElement> list = new ArrayList<ServiceElement>();
                ServiceElementManager[] mgrs = getServiceElementManagers();
                for (ServiceElementManager mgr : mgrs) {
                    mgr.setActive(active.get());
                    list.add(mgr.getServiceElement());
                }
                if (logger.isDebugEnabled())
                    logger.debug("OperationalStringManager for [{}] set active [{}] for OperationalString [{}]",
                                 getProxy().toString(), active, getName());
                if (active.get()) {
                    ServiceElement[] sElems = list.toArray(new ServiceElement[list.size()]);
                    updateServiceElements(sElems);
                }

                /* Trickle down effect : update all nested managers of the 
                 * new active state */
                OpStringManager[] nestedMgrs = nestedManagers.toArray(new OpStringManager[nestedManagers.size()]);
                for (OpStringManager nestedMgr : nestedMgrs) {
                    nestedMgr.setActive(newActive);
                }

            } else {
                if (logger.isTraceEnabled())
                    logger.trace("OperationalStringManager for [{}] already has active state of [{}]",
                                 opString.getName(), active);
            }
        }
    }

    /**
     * Get the active property
     *
     * @return The active property
     */
    public boolean isActive() {
        return active.get();
    }

    /**
     * @see org.rioproject.opstring.OperationalStringManager#setManaging(boolean)
     */
    public void setManaging(final boolean newActive) {
        setActive(newActive);
    }

    /**
     * @see org.rioproject.opstring.OperationalStringManager#isManaging
     */
    public boolean isManaging() {
        return isActive();
    }

    /**
     * @see org.rioproject.opstring.OperationalStringManager#getDeploymentDates
     */
    public Date[] getDeploymentDates() {
        return deployDateList.toArray(new Date[deployDateList.size()]);
    }

    /**
     * @see OpStringManager#setDeploymentStatus(int)
     */
    public void setDeploymentStatus(final int status) {
        opString.setDeployed(status);
        deployStatus = status;
        if (deployStatus == OperationalString.UNDEPLOYED) {
            if (!nestedManagers.isEmpty()) {
                OpStringManager[] nestedMgrs = nestedManagers.toArray(new OpStringManager[nestedManagers.size()]);
                for (OpStringManager nestedMgr : nestedMgrs) {
                    if (nestedMgr.getParentCount() == 1 && !nestedMgr.isStandAlone())
                        nestedMgr.setDeploymentStatus(OperationalString.UNDEPLOYED);
                }
            }
        }
    }

    /**
     * @see OpStringManager#addDeploymentDate(java.util.Date)
     */
    public void addDeploymentDate(final Date date) {
        if (date != null)
            deployDateList.add(date);
    }

    /**
     * Initialize all ServiceElementManager instances
     *
     * @param mode Whether the ServiceElementManager should actively manage (allocate) services. This will
     * also set the DefaultOpStringManager active Property
     * @param provisioner The ServiceProvisioner
     * @param uuid The Uuid of the ProvisionMonitorImpl. If the uuid of a
     * discovered service matches our uuid, don't spend the overhead of creating
     * a FaultDetectionHandler
     * @param listener A ServiceProvisionListener that will be notified
     *                 of services are they are provisioned. This notification approach is
     *                 only valid at DefaultOpStringManager creation (deployment), when services are
     *                 provisioned at OperationalString deployment time
     * @return A map of reasons and corresponding exceptions from creating
     *         service element manager instances. If the map has no entries there
     *         are no errors
     * @throws Exception if there are unrecoverable errors
     */
    Map<String, Throwable> init(final boolean mode,
                                final ServiceProvisioner provisioner,
                                final Uuid uuid,
                                final ServiceProvisionListener listener) throws Exception {
        this.active.set(mode);
        this.provisioner = provisioner;
        this.uuid = uuid;
        Map<String, Throwable> map = new HashMap<String, Throwable>();
        ServiceElement[] sElems = opString.getServices();
        for (ServiceElement sElem : sElems) {
            try {
                if (sElem.getExportBundles().length > 0) {
                    createServiceElementManager(sElem, false, listener);
                } else {
                    String message = String.format("Service [%s] has no declared interfaces, cannot deploy",
                                                   sElem.getName());
                    logger.warn(message);
                    map.put(sElem.getName(), new Exception(message));
                }
            } catch (Exception e) {
                Throwable cause = e.getCause();
                if (cause == null)
                    cause = e;
                logger.warn("Creating ServiceElementManager for [{}],deployment [{}]",
                            sElem.getName(), sElem.getOperationalStringName(),
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
     *                 of services are they are provisioned. This notification approach is
     *                 only valid at DefaultOpStringManager creation (deployment), when services are
     *                 provisioned at OperationalString deployment time
     *                 
     * @throws java.rmi.RemoteException If the DeployAdmin cannot be obtained
     */
    void startManager(final ServiceProvisionListener listener) throws RemoteException {
        startManager(listener, new HashMap());
    }

    /**
     * Start all ServiceElementManager instances
     *
     * @param listener         A ServiceProvisionListener that will be notified
     *                         of services are they are provisioned. This notification approach is
     *                         only valid at DefaultOpStringManager creation (deployment), when services are
     *                         provisioned at OperationalString deployment time
     * @param knownInstanceMap Known ServiceBeanInstance objects.
     * 
     * @throws java.rmi.RemoteException If the DeployAdmin cannot be obtained
     */
    void startManager(final ServiceProvisionListener listener, final Map knownInstanceMap) throws RemoteException {
        addDeploymentDate(new Date(System.currentTimeMillis()));
        setDeploymentStatus(OperationalString.DEPLOYED);
        ServiceElementManager[] mgrs = getServiceElementManagers();
        IdleServiceListener idleServiceListener = null;
        UndeployOption undeployOption = opString.getUndeployOption();
        if(undeployOption!=null && undeployOption.getType().equals(UndeployOption.Type.WHEN_IDLE)) {
            idleServiceListener = new IdleServiceListener(this);
        }
        for (ServiceElementManager mgr : mgrs) {
            ServiceElement elem = mgr.getServiceElement();
            ServiceBeanInstance[] instances = (ServiceBeanInstance[]) knownInstanceMap.get(elem);
            try {
                int alreadyRunning = mgr.startManager(listener, instances);
                if(logger.isTraceEnabled()) {
                    logger.trace("{} ServiceElementManager has {} instances already running {}",
                                opString.getName(), elem.getName(), alreadyRunning);
                }
                if (alreadyRunning > 0) {
                    updateServiceElements(new ServiceElement[]{mgr.getServiceElement()});
                }
                if(idleServiceListener !=null) {
                    logger.info("Deployment {} has an IDLE undeploy option; when: {}, timeUnit: {}",
                                opString.getName(), undeployOption.getWhen(), undeployOption.getTimeUnit());
                    mgr.setIdleTime(undeployOption.getTimeUnit().toMillis(undeployOption.getWhen()));
                    ServiceChannel.getInstance().subscribe(idleServiceListener, elem, ServiceChannelEvent.Type.IDLE);
                }
            } catch (Exception e) {
                logger.warn("Starting ServiceElementManager", e);
            }
        }
        if(logger.isTraceEnabled()) {
            logger.info("Started managers for {}", opString.getName());
        }
    }

    class IdleServiceListener implements ServiceChannelListener {
        final OpStringManager manager;
        final Map<ServiceElement, Boolean> tracking = new HashMap<ServiceElement, Boolean>();

        IdleServiceListener(OpStringManager manager) {
            this.manager = manager;
        }

        public void notify(ServiceChannelEvent event) {
            boolean undeploy = true;
            tracking.put(event.getServiceElement(), true);
            for(ServiceElementManager manager : getServiceElementManagers()) {
                if(manager.isTrackingIdleBehavior() && !tracking.containsKey(event.getServiceElement())) {
                    undeploy = false;
                    break;
                }
            }
            if(undeploy) {
                logger.info("Terminating [{}] due to idle services", getName());
                opStringMangerController.undeploy(manager, true);
            }
        }
    }

    /**
     * Create a specific ServiceElementManager, add it to the Collection of
     * ServiceElementManager instances, and start the manager
     *
     * @param sElem    The ServiceElement the ServiceElementManager will
     *                 manage
     * @param start    Whether to start the ServiceElementManager at creation
     * discovered service matches our uuid, don't spend the overhead of creating
     * a FaultDetectionHandler
     * @param listener A ServiceProvisionListener that will be notified
     *                 of services are they are provisioned. This notification approach is
     *                 only valid when the ServiceElementManager is created, and services are
     *                 provisioned at OperationalString deployment time
     * @throws Exception if the ServiceElementManager cannot be created
     */
    void createServiceElementManager(final ServiceElement sElem,
                                     final boolean start,
                                     final ServiceProvisionListener listener) throws Exception {
        ServiceElementManager svcElemMgr =
            new ServiceElementManager(sElem, proxy, provisioner, uuid, isActive(), config);
        /* Set event attributes */
        svcElemMgr.setEventProcessor(eventProcessor);
        svcElemMgr.setEventSource(serviceProxy);
        svcElemMgrs.add(svcElemMgr);

        if (start) {
            int alreadyRunning = svcElemMgr.startManager(listener);
            if (alreadyRunning > 0) {
                updateServiceElements(new ServiceElement[]{sElem});
            }
        }
    }

    /**
     * @see org.rioproject.opstring.OperationalStringManager#update(OperationalString)
     */
    public Map<String, Throwable> update(final OperationalString newOpString) throws OperationalStringException, RemoteException {

        if (!isActive()) {
            OperationalStringManager primary = opStringMangerController.getPrimary(getName());
            if (primary == null) {
                logger.warn("Primary testManager not located, force state to active for [{}]", getName());
                setActive(true);
            } else {
                logger.info("Forwarding update request to primary testManager for [{}]", getName());
                return (primary.update(newOpString));
            }
        }
        try {
            opStringMangerController.getDeploymentVerifier().verifyOperationalString(newOpString, getRemoteRepositories());
        } catch (Exception e) {
            throw new OperationalStringException("Verifying deployment for [" + newOpString.getName() + "]", e);
        }
        Map<String, Throwable> map = doUpdateOperationalString(newOpString);
        ProvisionMonitorEvent event = new ProvisionMonitorEvent(serviceProxy,
                                                                ProvisionMonitorEvent.Action.OPSTRING_UPDATED,
                                                                doGetOperationalString());
        eventProcessor.processEvent(event);
        return (map);
    }

    public RemoteRepository[] getRemoteRepositories() {
        Collection<RemoteRepository> remoteRepositories = new ArrayList<RemoteRepository>();
        if (oar != null)
            remoteRepositories.addAll(oar.getRepositories());
        return remoteRepositories.toArray(new RemoteRepository[remoteRepositories.size()]);
    }

    /**
     * @see OpStringManager#doUpdateOperationalString(org.rioproject.opstring.OperationalString)
     */
    public Map<String, Throwable> doUpdateOperationalString(final OperationalString newOpString) {
        if (newOpString == null)
            throw new IllegalArgumentException("OperationalString cannot be null");

        if (logger.isInfoEnabled()) {
            logger.info("Updating {} deployment", newOpString.getName());
        }
        Map<String, Throwable> map = new HashMap<String, Throwable>();
        ServiceElement[] sElems = newOpString.getServices();
        List<ServiceElementManager> notRefreshed = new ArrayList<ServiceElementManager>(svcElemMgrs);
        /* Refresh ServiceElementManagers */
        for (ServiceElement sElem : sElems) {
            try {
                ServiceElementManager svcElemMgr = getServiceElementManager(sElem);
                if (svcElemMgr == null) {
                    createServiceElementManager(sElem, true,  null);
                } else {
                    svcElemMgr.setServiceElement(sElem);
                    notRefreshed.remove(svcElemMgr);
                }
            } catch (Exception e) {
                map.put(sElem.getName(), e);
                logger.warn("Refreshing ServiceElementManagers", e);
            }
        }
        /*
        * Process nested Operational Strings. If a nested
        * OperationalString does not exist it will be created.
        * If it does exist check the number of parents, if its only referenced
        * by this testManager update it
        */
        OperationalString[] nested = newOpString.getNestedOperationalStrings();
        for (int i = 0; i < nested.length; i++) {
            if (!opStringMangerController.opStringExists(nested[i].getName())) {
                try {
                    opStringMangerController.addOperationalString(nested[i], map, this, null, null);
                } catch (Exception e) {
                    Throwable cause = e.getCause();
                    if (cause == null)
                        cause = e;
                    map.put(sElems[i].getName(), cause);
                    logger.warn("Adding nested OperationalString [{}]", nested[i].getName(), e);
                }
            } else {
                OpStringManager nestedMgr = opStringMangerController.getOpStringManager(nested[i].getName());
                if (nestedMgr.getParentCount() == 1 &&
                    nestedMgr.getParents().contains(this)) {
                    Map<String, Throwable> nestedMap = nestedMgr.doUpdateOperationalString(nested[i]);
                    map.putAll(nestedMap);
                }
            }
        }
        /*
        * If there are ServiceElementManagers that are not needed
        * remove them
        */
        for (ServiceElementManager mgr : notRefreshed) {
            mgr.stopManager(true);
            svcElemMgrs.remove(mgr);
        }
        opString = newOpString;
        stateChanged(false);
        if (isActive())
            updateServiceElements(sElems);
        return (map);
    }

    /**
     * @see OpStringManager#getParents()
     */
    public Collection<OpStringManager> getParents() {
        Collection<OpStringManager> rents = new ArrayList<OpStringManager>();
        rents.addAll(parents);
        return rents;
    }

    private void stateChanged(final boolean remove) {
        if(stateManager!=null)
            stateManager.stateChanged(this, remove);
    }

    /**
     * Verify all services are being monitored by iterating through the
     * Collection of ServiceElementManager instances and invoking each
     * instance's verify() method
     *
     * @param listener A ServiceProvisionListener that will be notified
     *                 of services if they are provisioned.
     */
    public void verify(final ServiceProvisionListener listener) {
        for (ServiceElementManager mgr : svcElemMgrs) {
            mgr.verify(listener);
        }
        for (OpStringManager nestedMgr : nestedManagers) {
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
    void updateServiceElements(final ServiceElement[] elements) {
        if (!isActive())
            return;
        ServiceResource[] resources = provisioner.getServiceResourceSelector().getServiceResources();
        Map<InstantiatorResource, List<ServiceElement>> map =new HashMap<InstantiatorResource, List<ServiceElement>>();
        for (ServiceResource resource : resources) {
            InstantiatorResource ir =
                (InstantiatorResource) resource.getResource();
            for (ServiceElement element : elements) {
                int count = ir.getServiceElementCount(element);
                if (logger.isTraceEnabled())
                    logger.trace("{} at [{}] has [{}] of [{}]",
                                 ir.getName(), ir.getHostAddress(), count, element.getName());
                if (count > 0) {
                    List<ServiceElement> list = map.get(ir);
                    if (list == null)
                        list = new ArrayList<ServiceElement>();
                    list.add(element);
                    map.put(ir, list);
                }
            }
        }

        for (Map.Entry<InstantiatorResource, List<ServiceElement>> entry : map.entrySet()) {
            InstantiatorResource ir = entry.getKey();
            List<ServiceElement> list = entry.getValue();

            ServiceElement[] elems = list.toArray(new ServiceElement[list.size()]);
            try {
                ServiceBeanInstantiator sbi = ir.getInstantiator();
                if (logger.isTraceEnabled())
                    logger.trace("Update {} at [{}] with [{}] elements",
                                 ir.getName(), ir.getHostAddress(), elems.length);
                sbi.update(elems, getProxy());
            } catch (RemoteException e) {
                if (logger.isTraceEnabled()) {
                    logger.trace("Updating ServiceElement for {} at [{}]", ir.getName(), ir.getHostAddress(), e);
                } else {
                    logger.info("{}: {} Updating ServiceElement for {} at [{}]",
                                e.getClass().getName(), e.getMessage(), ir.getName(), ir.getHostAddress());
                }
            }
        }
    }

    /**
     * @see OpStringManager#terminate(boolean)
     */
    public OperationalString[] terminate(final boolean killServices) {
        List<OperationalString> terminated = new ArrayList<OperationalString>();
        terminated.add(doGetOperationalString());
        /* Cancel all scheduled Tasks */
        TimerTask[] tasks = getTasks();
        for (TimerTask task : tasks)
            task.cancel();

        /* Unexport the testManager */
        try {
            exporter.unexport(true);
        } catch (IllegalStateException e) {
            if (logger.isDebugEnabled())
                logger.debug("OperationalStringManager not unexported");
        }
        /* Remove ourselves from the collection */
        if(opStringMangerController.remove(this)) {
            if(logger.isDebugEnabled())
                logger.debug("Removed [{}]", getName());
        } else {
            logger.warn("Did not remove [{}] when terminating", getName());
        }

        /* Stop all ServiceElementManager instances */
        for (ServiceElementManager mgr : svcElemMgrs) {
            mgr.stopManager(killServices);
        }
        /* Adjust parent/nested relationships */
        if (!parents.isEmpty()) {
            for (OpStringManager parent : parents) {
                parent.removeNested(this);
            }
            parents.clear();
        }

        OpStringManager[] nestedMgrs = nestedManagers.toArray(new OpStringManager[nestedManagers.size()]);
        for (OpStringManager nestedMgr : nestedMgrs) {
            /* If the nested DefaultOpStringManager has only 1 parent, then
             * terminate (undeploy) that DefaultOpStringManager as well */
            if (nestedMgr.getParentCount() == 1 && !nestedMgr.isStandAlone()) {
                terminated.add(nestedMgr.doGetOperationalString());
                nestedMgr.terminate(killServices);
            } else {
                nestedMgr.removeParent(this);
            }
        }
        if (logger.isInfoEnabled())
            logger.info("OpStringManager [{}] terminated", getName());
        return (terminated.toArray(new OperationalString[terminated.size()]));
    }

    /**
     * Get the name of this Operational String
     *
     * @return The name of this Operational String
     */
    public String getName() {
        return (opString.getName());
    }

    /*
    * @see org.rioproject.opstring.OperationalStringManager#addServiceElement
    */
    public void addServiceElement(final ServiceElement sElem,
                                  final ServiceProvisionListener listener)
        throws OperationalStringException {
        if (sElem == null)
            throw new IllegalArgumentException("ServiceElement is null");
        if (sElem.getOperationalStringName() == null)
            throw new IllegalArgumentException("ServiceElement must have an OperationalString name");
        if (!sElem.getOperationalStringName().equals(opString.getName()))
            throw new IllegalArgumentException("ServiceElement has wrong OperationalString name. " +
                                               "Provided " +"[" + sElem.getOperationalStringName() + "], " +
                                               "should be [" + opString.getName() + "]");
        if (!isActive())
            throw new OperationalStringException("not the primary OperationalStringManager");
        try {
            doAddServiceElement(sElem, listener);
            stateChanged(false);
            ProvisionMonitorEvent event = new ProvisionMonitorEvent(serviceProxy,
                                                                    ProvisionMonitorEvent.Action.SERVICE_ELEMENT_ADDED,
                                                                    sElem);
            eventProcessor.processEvent(event);

        } catch (Throwable t) {
            throw new OperationalStringException("Adding ServiceElement", t);
        }
        if (logger.isDebugEnabled())
            logger.debug("Added service [{}/{}]", sElem.getOperationalStringName(), sElem.getName());
    }


    /**
    * @see OpStringManager#doAddServiceElement(ServiceElement, ServiceProvisionListener)
    */
    public void doAddServiceElement(final ServiceElement sElem, final ServiceProvisionListener listener) throws Exception {
        if (sElem.getExportBundles().length > 0) {

            /*File pomFile = null;
            if(oar!=null) {
                File dir = new File(oar.getDeployDir());
                pomFile = OARUtil.find("pom.xml", dir);
            }*/
            opStringMangerController.getDeploymentVerifier().verifyOperationalStringService(sElem,
                                                                                            ResolverHelper.getResolver(),
                                                                                            getRemoteRepositories());
            createServiceElementManager(sElem, true, listener);
            opString.addService(sElem);
            stateChanged(true);
        } else {
            throw new OperationalStringException("Interfaces are null");
        }
    }

    /*
    * @see org.rioproject.opstring.OperationalStringManager#removeServiceElement
    */
    public void removeServiceElement(final ServiceElement sElem, final boolean destroy)
        throws OperationalStringException {
        if (sElem == null)
            throw new IllegalArgumentException("ServiceElement is null");
        if (!isActive())
            throw new OperationalStringException("not the primary OperationalStringManager");
        try {
            doRemoveServiceElement(sElem, destroy);
            ProvisionMonitorEvent event =
                new ProvisionMonitorEvent(serviceProxy,
                                          ProvisionMonitorEvent.Action.SERVICE_ELEMENT_REMOVED,
                                          sElem);
            eventProcessor.processEvent(event);
        } catch (Throwable t) {
            if (t instanceof OperationalStringException)
                throw (OperationalStringException) t;
            throw new OperationalStringException("Removing ServiceElement", t);
        }
        if (logger.isDebugEnabled())
            logger.debug("Removed service [{}/{}]", sElem.getOperationalStringName(), sElem.getName());
    }

    /**
     * @see OpStringManager#doRemoveServiceElement(ServiceElement, boolean)
     */
    public void doRemoveServiceElement(final ServiceElement sElem, final boolean destroy)throws OperationalStringException {
        ServiceElementManager svcElemMgr = getServiceElementManager(sElem);
        if (svcElemMgr == null)
            throw new OperationalStringException("OperationalStringManager for [" + opString.getName() + "], " +
                                                 "is not managing service " +
                                                 "[" +sElem.getOperationalStringName() +"/" +sElem.getName() +"]",
                                                 false);
        svcElemMgr.stopManager(destroy);
        if (!svcElemMgrs.remove(svcElemMgr))
            logger.warn("UNABLE to remove ServiceElementManager for [{}/{}]",
                           sElem.getOperationalStringName(), sElem.getName());
        else {
            logger.info(String.format("Removed ServiceElementManager for [%s/%s]",
                                      sElem.getOperationalStringName(), sElem.getName()));
            opString.removeService(sElem);
            stateChanged(true);
        }
    }

    /*
    * @see org.rioproject.opstring.OperationalStringManager#update
    */
    public void update(final ServiceElement sElem)
        throws OperationalStringException {
        if (sElem == null)
            throw new IllegalArgumentException("ServiceElement is null");
        if (!isActive())
            throw new OperationalStringException("not the primary OperationalStringManager");
        try {
            doUpdateServiceElement(sElem);
            ProvisionMonitorEvent.Action action = ProvisionMonitorEvent.Action.SERVICE_ELEMENT_UPDATED;
            ProvisionMonitorEvent event = new ProvisionMonitorEvent(serviceProxy, action, sElem);
            eventProcessor.processEvent(event);
        } catch (Throwable t) {
            logger.warn("Updating ServiceElement [{}]", sElem.getName(), t);
            throw new OperationalStringException("Updating ServiceElement [" + sElem.getName() + "]", t);
        }
    }

    /**
     * @see OpStringManager#doUpdateServiceElement(ServiceElement)
     */
    public void doUpdateServiceElement(final ServiceElement sElem) throws Exception {

        ServiceElementManager svcElemMgr = getServiceElementManager(sElem);
        if (svcElemMgr == null) {
            doAddServiceElement(sElem, null);
        } else {
            svcElemMgr.setServiceElement(sElem);
            svcElemMgr.verify(null);
            stateChanged(false);
            updateServiceElements(new ServiceElement[]{sElem});
        }
    }

    /**
     * @see org.rioproject.opstring.OperationalStringManager#relocate
     */
    public void relocate(final ServiceBeanInstance instance, final ServiceProvisionListener listener, final Uuid uuid)
        throws OperationalStringException, RemoteException {
        if (instance == null)
            throw new IllegalArgumentException("instance is null");
        if (!isActive())
            throw new OperationalStringException("not the primary OperationalStringManager");
        ServiceProvisionListener preparedListener = null;
        if (listener != null)
            preparedListener = (ServiceProvisionListener) serviceProvisionListenerPreparer.prepareProxy(listener);
        try {
            ServiceElementManager svcElemMgr = getServiceElementManager(instance);
            if (svcElemMgr == null)
                throw new OperationalStringException("Unmanaged ServiceBeanInstance " +
                                                     "[" + instance.getServiceBeanConfig().getName() + "], " +
                                                     "[" + instance.toString() + "]", false);
            if (svcElemMgr.getServiceElement().getProvisionType() != ServiceElement.ProvisionType.DYNAMIC)
                throw new OperationalStringException("Service must be dynamic to be relocated");
            svcElemMgr.relocate(instance, preparedListener, uuid);
        } catch (Throwable t) {
            logger.warn("Relocating ServiceBeanInstance {}: {}", t.getClass().getName(), t.getMessage());
            if (t instanceof OperationalStringException)
                throw (OperationalStringException) t;

            throw new OperationalStringException("Relocating ServiceBeanInstance", t);
        }
    }

    /**
     * @see org.rioproject.opstring.OperationalStringManager#update
     */
    public void update(final ServiceBeanInstance instance)
        throws OperationalStringException {
        if (instance == null)
            throw new IllegalArgumentException("instance is null");
        try {
            ServiceElement sElem = doUpdateServiceBeanInstance(instance);
            ProvisionMonitorEvent event = new ProvisionMonitorEvent(serviceProxy,
                                                                    sElem.getOperationalStringName(),
                                                                    instance);
            eventProcessor.processEvent(event);
        } catch (Throwable t) {
            logger.warn("Updating ServiceBeanInstance [{}] {}: {}",
                        instance.getServiceBeanConfig().getName(), t.getClass().getName(), t.getMessage());
            throw new OperationalStringException("Updating ServiceBeanInstance", t);
        }
    }

    /**
     * @see OpStringManager#doUpdateServiceBeanInstance(ServiceBeanInstance)
     */
    public ServiceElement doUpdateServiceBeanInstance(final ServiceBeanInstance instance)
        throws OperationalStringException {

        ServiceElementManager svcElemMgr = getServiceElementManager(instance);
        if (svcElemMgr == null)
            throw new OperationalStringException("Unmanaged ServiceBeanInstance [" + instance.toString() + "]", false);
        svcElemMgr.update(instance);
        return (svcElemMgr.getServiceElement());
    }

    /*
    * @see org.rioproject.opstring.OperationalStringManager#increment
    */
    public synchronized void increment(final ServiceElement sElem, final boolean permanent, final ServiceProvisionListener listener)
        throws OperationalStringException, RemoteException {
        if (sElem == null)
            throw new IllegalArgumentException("ServiceElement is null");
        if (!isActive())
            throw new OperationalStringException("not the primary OperationalStringManager");
        ServiceProvisionListener preparedListener = null;
        if (listener != null)
            preparedListener = (ServiceProvisionListener) serviceProvisionListenerPreparer.prepareProxy(listener);
        try {
            ServiceElementManager svcElemMgr = getServiceElementManager(sElem);
            if (svcElemMgr == null)
                throw new OperationalStringException("Unmanaged ServiceElement [" + sElem.getName() + "]", false);
            ServiceElement changed = svcElemMgr.increment(permanent, preparedListener);
            if (changed == null)
                return;
            stateChanged(false);
            updateServiceElements(new ServiceElement[]{changed});
            ProvisionMonitorEvent event = new ProvisionMonitorEvent(serviceProxy,
                                                                    ProvisionMonitorEvent.Action.SERVICE_BEAN_INCREMENTED,
                                                                    changed);
            eventProcessor.processEvent(event);
        } catch (Throwable t) {
            String message = String.format("Incrementing ServiceElement [%s] %s: %s" ,
                                           sElem.getName(), t.getClass().getName(), t.getMessage());
            logger.warn(message);
            throw new OperationalStringException(message, t);
        }
    }

    /*
    * @see org.rioproject.opstring.OperationalStringManager#getPendingCount
    */
    public int getPendingCount(final ServiceElement sElem) {
        if (sElem == null)
            throw new IllegalArgumentException("ServiceElement is null");
        int numPending = -1;
        ServiceElementManager svcElemMgr = getServiceElementManager(sElem);
        if (svcElemMgr != null)
            numPending = svcElemMgr.getPendingCount();
        return (numPending);
    }

    /*
    * @see org.rioproject.opstring.OperationalStringManager#trim
    */
    public int trim(final ServiceElement sElem, final int trimUp) throws OperationalStringException {
        if (sElem == null)
            throw new IllegalArgumentException("ServiceElement is null");
        if (!isActive())
            throw new OperationalStringException("not the primary OperationalStringManager");
        if (sElem.getProvisionType() != ServiceElement.ProvisionType.DYNAMIC)
            return (-1);
        int numTrimmed;
        try {
            ServiceElementManager svcElemMgr = getServiceElementManager(sElem);
            if (svcElemMgr == null)
                throw new OperationalStringException("Unmanaged ServiceElement [" + sElem.getName() + "]", false);
            numTrimmed = svcElemMgr.trim(trimUp);
            if (numTrimmed > 0) {
                stateChanged(false);
                ServiceElement updatedElement = svcElemMgr.getServiceElement();
                updateServiceElements(new ServiceElement[]{updatedElement});
                ProvisionMonitorEvent event = new ProvisionMonitorEvent(serviceProxy,
                                                                        ProvisionMonitorEvent.Action.SERVICE_BEAN_DECREMENTED,
                                                                        updatedElement);
                eventProcessor.processEvent(event);
            }
            return (numTrimmed);
        } catch (Throwable t) {
            logger.warn("Trimming ServiceElement [{}] {}: {}",sElem.getName(), t.getClass().getName(), t.getMessage());
            throw new OperationalStringException("Trimming ServiceElement ["+sElem.getName()+"]", t);
        }
    }

    /*
    * @see org.rioproject.opstring.OperationalStringManager#decrement
    */
    public void decrement(final ServiceBeanInstance instance, final boolean recommended, final boolean destroy)
        throws OperationalStringException {
        if (instance == null)
            throw new IllegalArgumentException("instance is null");
        if (!isActive())
            throw new OperationalStringException("not the primary OperationalStringManager");
        ServiceElementManager svcElemMgr =
            getServiceElementManager(instance);
        if (svcElemMgr == null)
            throw new OperationalStringException("Unmanaged ServiceBeanInstance [" + instance.toString() + "]", false);
        ServiceElement sElem = svcElemMgr.decrement(instance, recommended, destroy);
        if(sElem==null)
            return;
        stateChanged(false);
        updateServiceElements(new ServiceElement[]{sElem});
        ProvisionMonitorEvent event = new ProvisionMonitorEvent(serviceProxy,
                                                                ProvisionMonitorEvent.Action.SERVICE_BEAN_DECREMENTED,
                                                                sElem.getOperationalStringName(),
                                                                sElem,
                                                                instance);
        eventProcessor.processEvent(event);
    }

    /**
     * Determine of this DefaultOpStringManager has any parents
     *
     * @return If true, this DefaultOpStringManager is top-level (has no
     *         parents)
     */
    public boolean isTopLevel() {
        return parents.isEmpty();
    }

    /*
    * @see org.rioproject.opstring.OperationalStringManager#getOperationalString
    */
    public OperationalString getOperationalString() {
        return (doGetOperationalString());
    }

    /*
    * @see org.rioproject.opstring.OperationalStringManager#getServiceElement
    */
    public ServiceElement getServiceElement(final Object proxy) {
        if (proxy == null)
            throw new IllegalArgumentException("proxy is null");
        ServiceElementManager mgr = null;
        try {
            mgr = getServiceElementManager(proxy);
        } catch (IOException e) {
            if (logger.isTraceEnabled())
                logger.trace("Getting ServiceElementManager for proxy", e);
        }
        if (mgr == null)
            logger.warn("No ServiceElementManager found for proxy {}", proxy);
        ServiceElement element = null;
        if (mgr != null) {
            element = mgr.getServiceElement();
        }
        return (element);
    }

    /*
    * @see org.rioproject.opstring.OperationalStringManager#getServiceBeanInstances
    */
    public ServiceBeanInstance[] getServiceBeanInstances(final ServiceElement sElem) throws OperationalStringException {
        if (sElem == null)
            throw new IllegalArgumentException("ServiceElement is null");
        try {
            ServiceElementManager mgr = getServiceElementManager(sElem);
            if (mgr == null)
                throw new OperationalStringException("Unmanaged ServiceElement [" + sElem.getName() + "]", false);
            return (mgr.getServiceBeanInstances());
        } catch (Exception e) {
            if (e instanceof OperationalStringException) {
                throw (OperationalStringException) e;
            } else {
                logger.warn("Getting ServiceBeanInstances for ServiceElement [{}]", sElem.getName(), e);
                throw new OperationalStringException("Getting ServiceBeanInstances for ServiceElement " +
                                                     "["+sElem.getName() + "]", e);
            }
        }
    }

    /*
    * @see org.rioproject.opstring.OperationalStringManager#getServiceElement
    */
    public ServiceElement getServiceElement(final String[] interfaces,
                                            final String name) {
        if (interfaces == null)
            throw new IllegalArgumentException("interfaces cannot be null");
        for (ServiceElementManager mgr : svcElemMgrs) {
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

            if (found) {
                if (name == null)
                    return (sElem);
                if (name.equals(sElem.getName()))
                    return (sElem);
            }
        }
        return (null);
    }

    /**
     * Get the OperationalString the DefaultOpStringManager is managing
     *
     * @return The OperationalString the DefaultOpStringManager is managing
     */
    public OperationalString doGetOperationalString() {
        OpString opstr = new OpString(opString.getName(), opString.loadedFrom());
        opstr.setDeployed(deployStatus);
        for (ServiceElementManager mgr : svcElemMgrs) {
            opstr.addService(mgr.getServiceElement());
        }
        for (OpStringManager nestedMgr : nestedManagers) {
            opstr.addOperationalString(nestedMgr.doGetOperationalString());
        }
        opString = opstr;
        return (opString);
    }

    /**
     * @see org.rioproject.opstring.OperationalStringManager#redeploy(ServiceElement,
     *      ServiceBeanInstance, boolean, boolean, long, ServiceProvisionListener)
     */
    public void redeploy(final ServiceElement sElem,
                         final ServiceBeanInstance instance,
                         final boolean clean,
                         final long delay,
                         final ServiceProvisionListener listener)
        throws OperationalStringException {
        redeploy(sElem, instance, clean, true, delay, listener);
    }

    /**
     * @see org.rioproject.opstring.OperationalStringManager#redeploy(ServiceElement,
     *      ServiceBeanInstance, boolean, long, ServiceProvisionListener)
     */
    public void redeploy(final ServiceElement sElem,
                         final ServiceBeanInstance instance,
                         final boolean clean,
                         final boolean sticky,
                         final long delay,
                         final ServiceProvisionListener listener) throws OperationalStringException {

        if (!isActive())
            throw new OperationalStringException("not the primary OperationalStringManager");
        ServiceProvisionListener preparedListener = null;
        if (listener != null) {
            try {
                preparedListener = (ServiceProvisionListener) serviceProvisionListenerPreparer.prepareProxy(listener);
            } catch (RemoteException e) {
                logger.trace("Notifying ServiceProvisionListener of redeployment, continue with redeployment.", e);
            }
        }

        if (delay > 0) {
            doScheduleRedeploymentTask(delay, sElem, instance, clean, sticky, preparedListener);
        } else {
            if (sElem == null && instance == null)
                doRedeploy(clean, sticky, preparedListener);
            else
                doRedeploy(sElem, instance, clean, sticky, preparedListener);
        }
    }

    /**
     * @see org.rioproject.opstring.OperationalStringManager#getServiceStatements
     */
    public ServiceStatement[] getServiceStatements() {
        List<ServiceStatement> statements = new ArrayList<ServiceStatement>();
        for (ServiceElementManager mgr : getServiceElementManagers()) {
            statements.add(mgr.getServiceStatement());
        }
        return statements.toArray(new ServiceStatement[statements.size()]);
    }


    /**
     * @see org.rioproject.opstring.OperationalStringManager#getDeploymentMap
     */
    public DeploymentMap getDeploymentMap() {
        Map<ServiceElement, List<DeployedService>> map = new HashMap<ServiceElement, List<DeployedService>>();
        for (ServiceElementManager mgr : getServiceElementManagers()) {
            map.put(mgr.getServiceElement(), mgr.getServiceDeploymentList());
        }
        return new DeploymentMap(map);
    }

    /*
    * Redeploy the OperationalString  
    */
    public void doRedeploy(final boolean clean, final boolean sticky, final ServiceProvisionListener listener) throws OperationalStringException {
        if (!isActive())
            throw new OperationalStringException("not the primary OperationalStringManager");
        for (ServiceElementManager mgr : svcElemMgrs.toArray(new ServiceElementManager[svcElemMgrs.size()]))
            doRedeploy(mgr.getServiceElement(), null, clean, sticky, listener);
    }

    /*
    * Redeploy a ServiceElement or a ServiceBeanInstance  
    */
    public void doRedeploy(final ServiceElement sElem,
                           final ServiceBeanInstance instance,
                           final boolean clean,
                           final boolean sticky,
                           final ServiceProvisionListener listener)
        throws OperationalStringException {

        if (!isActive())
            throw new OperationalStringException("not the primary OperationalStringManager");
        ServiceElementManager svcElemMgr = null;
        RedeploymentTask scheduledTask = null;
        if (sElem != null) {
            svcElemMgr = getServiceElementManager(sElem);
            scheduledTask = getScheduledRedeploymentTask(sElem, null);
        } else if (instance != null) {
            svcElemMgr = getServiceElementManager(instance);
            scheduledTask = getScheduledRedeploymentTask(null, instance);
        }

        if (svcElemMgr == null) {
            String message =
                sElem==null? "Unmanaged ServiceElement" :"Unmanaged ServiceElement [" + sElem.getName() + "]";
            throw new OperationalStringException(message);
        }

        if (scheduledTask != null) {
            long exec = (scheduledTask.scheduledExecutionTime() - System.currentTimeMillis()) / 1000;
            if (exec > 0) {
                String item = (sElem == null ? "ServiceBeanInstance" : "ServiceElement");
                throw new OperationalStringException(item+" already scheduled for redeployment in "+exec+" seconds");
            }
        }

        /* Redeployment is for a ServiceElement */
        if (sElem != null) {
            ServiceBeanInstance[] instances = svcElemMgr.getServiceBeanInstances();
            if (instances.length > 0) {
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

    /**
     * @see OpStringManager#doScheduleRedeploymentTask(long, ServiceElement, ServiceBeanInstance, boolean, boolean, ServiceProvisionListener)
     */
    public void doScheduleRedeploymentTask(final long delay,
                                           final ServiceElement sElem,
                                           final ServiceBeanInstance instance,
                                           final boolean clean,
                                           final boolean sticky,
                                           final ServiceProvisionListener listener) throws OperationalStringException {
        RedeploymentTask scheduledTask = getScheduledRedeploymentTask(sElem, instance);
        if (scheduledTask != null) {
            long exec = (scheduledTask.scheduledExecutionTime() - System.currentTimeMillis()) / 1000;
            throw new OperationalStringException("Already " +
                                                 "scheduled " +
                                                 "for redeployment " +
                                                 "in " +
                                                 exec + " seconds");
        }
        RedeploymentTask task = new RedeploymentTask(this, sElem, instance, clean, sticky, listener);
        addTask(task);
        TaskTimer.getInstance().schedule(task, delay);
        Date redeployDate = new Date(System.currentTimeMillis() + delay);
        Object[] parms = new Object[]{redeployDate, clean, sticky, listener};
        ProvisionMonitorEvent event = new ProvisionMonitorEvent(serviceProxy,
                                                                opString.getName(),
                                                                sElem,
                                                                instance,
                                                                parms);
        eventProcessor.processEvent(event);

        if (logger.isTraceEnabled()) {
            String name = (instance == null ? sElem.getName() : instance.getServiceBeanConfig().getName());
            String item = (instance == null ? "ServiceElement" : "ServiceBeanInstance");
            logger.trace("Schedule [{}] {} redeploy in [{}] millis", name, item, delay);
        }
    }

    /**
     * Check for a scheduled RedeploymentTask for either the ServiceElement or
     * theServiceBeanInstance.
     *
     * @param sElem    the ServiceElement to check
     * @param instance The corresponding ServiceBeanInstance
     * @return The scheduled RedeploymentTask, or null if not found
     */
    RedeploymentTask getScheduledRedeploymentTask(final ServiceElement sElem, final ServiceBeanInstance instance) {
        TimerTask[] tasks = getTasks();
        RedeploymentTask scheduledTask = null;
        for (TimerTask task : tasks) {
            if (task instanceof RedeploymentTask) {
                RedeploymentTask rTask = (RedeploymentTask) task;
                if (sElem != null && rTask.getServiceElement() != null) {
                    if (rTask.getServiceElement().equals(sElem)) {
                        scheduledTask = rTask;
                        break;
                    }
                    if (instance != null && rTask.getInstance() != null) {
                        if (rTask.getInstance().equals(instance)) {
                            scheduledTask = rTask;
                            break;
                        }
                    }
                }
            }
        }
        return (scheduledTask);
    }

    /**
     * Get all ServiceElementManager instances
     *
     * @return All ServiceElementManager instances as an array
     */
    public ServiceElementManager[] getServiceElementManagers() {
        return (svcElemMgrs.toArray(new ServiceElementManager[svcElemMgrs.size()]));
    }

    /**
     * @see OpStringManager#getServiceElementManager(ServiceElement)
     */
    public ServiceElementManager getServiceElementManager(final ServiceElement sElem) {
        for (ServiceElementManager mgr : svcElemMgrs) {
            ServiceElement sElem1 = mgr.getServiceElement();
            if (sElem.equals(sElem1)) {
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
     *         managing the ServiceElement. If no ServiceElementManager is found,
     *         null is returned
     */
    ServiceElementManager getServiceElementManager(final ServiceBeanInstance instance) {
        for (ServiceElementManager mgr : svcElemMgrs) {
            if (mgr.hasServiceBeanInstance(instance))
                return (mgr);
        }
        return (null);
    }

    /**
     * Get the ServiceElementManager from a service proxy
     *
     * @param proxy The service proxy
     * @return The ServiceElementManager that is
     *         managing the ServiceElement. If no ServiceElementManager is found,
     *         null is returned
     * @throws IOException If the service proxy from a ServiceBeanInstance
     *                     returned from a ServiceElementManager cannot be unmarshalled
     */
    ServiceElementManager getServiceElementManager(final Object proxy) throws IOException {
        for (ServiceElementManager mgr : svcElemMgrs) {
            ServiceBeanInstance[] instances = mgr.getServiceBeanInstances();
            for (ServiceBeanInstance instance : instances) {
                try {
                    if (instance.getService().equals(proxy))
                        return (mgr);
                } catch (ClassNotFoundException e) {
                    logger.warn("Unable to obtain proxy", e);
                }
            }
        }
        return (null);
    }

    /**
     * @see OpStringManager#addNested(OpStringManager)
     */
    public void addNested(final OpStringManager nestedMgr) {
        nestedManagers.add(nestedMgr);
        nestedMgr.addParent(this);
    }

    /**
     * Remove a nested OpStringManager
     *
     * @param nestedMgr The nested OpStringManager to remove
     */
    public void removeNested(final OpStringManager nestedMgr) {
        nestedManagers.remove(nestedMgr);
    }

    /**
     * Add a parent for this OpStringManager. This OpStringManager will
     * now be a nested OpStringManager
     *
     * @param parent The parent for this OpStringManager.
     */
    public void addParent(final OpStringManager parent) {
        if (parents.contains(parent))
            return;
        parents.add(parent);
    }

    /**
     * Remove a parent from this OpStringManager.
     *
     * @param parent The parent to remove
     */
    public void removeParent(final OpStringManager parent) {
        parents.remove(parent);
    }

    /**
     * Get the number of parents the OpStringManager has
     *
     * @return The number of parents the OpStringManager has
     */
    public int getParentCount() {
        return (parents.size());
    }

    public boolean isStandAlone() {
        return standAlone;
    }

    /**
     * Returns a <code>TrustVerifier</code> which can be used to verify that a
     * given proxy to this policy handler can be trusted
     */
    public TrustVerifier getProxyVerifier() {
        return (new BasicProxyTrustVerifier(proxy));
    }

    /**
     * Get all in process TimerTask instances
     *
     * @return Array of in process TimerTask instances. If there are no
     *         TimerTask instances return a zero-length array
     */
    TimerTask[] getTasks() {
        return (scheduledTaskList.toArray(new TimerTask[scheduledTaskList.size()]));
    }

    /**
     * Add a TimerTask to the Collection of TimerTasks
     *
     * @param task The TimerTask to add
     */
    public void addTask(final TimerTask task) {
        if (task != null)
            scheduledTaskList.add(task);
    }

    /**
     * Remove a TimerTask from Collection of scheduled TimerTask
     * instances
     *
     * @param task The TimerTask to remove
     */
    public void removeTask(final TimerTask task) {
        if (task != null)
            scheduledTaskList.remove(task);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        DefaultOpStringManager manager = (DefaultOpStringManager) o;
        return opString.getName().equals(manager.opString.getName());
    }

    @Override
    public int hashCode() {
        return opString.getName().hashCode();
    }
}

