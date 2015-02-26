/*
 * Copyright 2011 the original author or authors.
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

import net.jini.config.Configuration;
import net.jini.id.Uuid;
import org.rioproject.deploy.DeployAdmin;
import org.rioproject.deploy.ServiceBeanInstance;
import org.rioproject.deploy.ServiceProvisionListener;
import org.rioproject.monitor.ProvisionMonitor;
import org.rioproject.monitor.ProvisionMonitorEvent;
import org.rioproject.monitor.service.peer.ProvisionMonitorPeer;
import org.rioproject.monitor.service.persistence.StateManager;
import org.rioproject.opstring.OperationalString;
import org.rioproject.opstring.OperationalStringException;
import org.rioproject.opstring.OperationalStringManager;
import org.rioproject.opstring.ServiceElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.rmi.RemoteException;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This class manages interactions with {@link OpStringManager} classes.
 */
public class OpStringManagerController {
    /**
     * Collection for all OperationalString OpStringManager instances
     */
    private ProvisionMonitorPeer provisionMonitorPeer;
    private Configuration config;
    private ProvisionMonitor serviceProxy;
    private ProvisionMonitorEventProcessor eventProcessor;
    private StateManager stateManager;
    private ServiceProvisioner serviceProvisioner;
    private Uuid uuid;
    private static Logger logger = LoggerFactory.getLogger(OpStringManagerController.class.getName());
    private DeploymentVerifier deploymentVerifier;
    private final ConcurrentHashMap<String, OpStringManager> opStringManagerTable = new ConcurrentHashMap<String, OpStringManager>();

    void setServiceProvisioner(final ServiceProvisioner serviceProvisioner) {
        this.serviceProvisioner = serviceProvisioner;
    }

    void setUuid(Uuid uuid) {
        this.uuid = uuid;
    }

    void setEventProcessor(final ProvisionMonitorEventProcessor eventProcessor) {
        this.eventProcessor = eventProcessor;
    }

    void setServiceProxy(final ProvisionMonitor serviceProxy) {
        this.serviceProxy = serviceProxy;
    }

    void setProvisionMonitorPeer(final ProvisionMonitorPeer provisionMonitorPeer) {
        this.provisionMonitorPeer = provisionMonitorPeer;
    }

    void setConfig(final Configuration config) {
        this.config = config;
    }

    void setStateManager(final StateManager stateManager) {
        this.stateManager = stateManager;
    }

    public DeploymentVerifier getDeploymentVerifier() {
        return deploymentVerifier;
    }

    public void setDeploymentVerifier(DeploymentVerifier deploymentVerifier) {
        this.deploymentVerifier = deploymentVerifier;
    }

    public void undeploy(final OpStringManager opStringManager, final boolean terminate) {
        opStringManager.setDeploymentStatus(OperationalString.UNDEPLOYED);
        OperationalString opString = opStringManager.doGetOperationalString();
        logger.trace("Terminating Operational String [{}]", opString.getName());
        OperationalString[] terminated = opStringManager.terminate(terminate);
        logger.info("Undeployed Operational String [{}]", opString.getName());
        for (OperationalString os : terminated) {
            eventProcessor.processEvent(new ProvisionMonitorEvent(serviceProxy,
                                                                  ProvisionMonitorEvent.Action.OPSTRING_UNDEPLOYED,
                                                                  os));
        }
        if (stateManager != null)
            stateManager.stateChanged(opStringManager, true);
    }

    /**
     * Get all OperationalString objects from the Collection of OpStringManagers
     *
     * @return Array of OperationalString objects
     */
    public OperationalString[] getOperationalStrings() {
        List<OperationalString> list = new ArrayList<OperationalString>();
        for(Map.Entry<String, OpStringManager> entry : opStringManagerTable.entrySet()) {
            if (entry.getValue().isTopLevel())
                list.add(entry.getValue().doGetOperationalString());
        }
        return list.toArray(new OperationalString[list.size()]);
    }

    /**
     * Get the OpStringManager for an OperationalString
     *
     * @param name The name of an OperationalString
     * @return The corresponding OpStringManager, or null if
     * not found
     */
    public OpStringManager getOpStringManager(final String name) {
        if (name == null)
            return null;
        return opStringManagerTable.get(name);
    }

    /**
     * Get the primary OperationalStringManager for an opstring
     *
     * @param name The name of an OperationalString
     * @return The primary OperationalStringManager if found, or null if not
     */
    public OperationalStringManager getPrimary(final String name) {
        OperationalStringManager mgr = null;
        ProvisionMonitor[] monitors = provisionMonitorPeer.getProvisionMonitorPeers();
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
                logger.warn("Getting the primary OperationalStringManager for [" + name + "]", e);
            }
        }
        return mgr;
    }

    /**
     * Get the primary DeployAdmin for the opstring
     *
     * @param opStringName The opstring name
     * @return The primary DeployAdmin for the opstring, or null
     * if not found
     */
    public DeployAdmin getPrimaryDeployAdmin(final String opStringName) {
        DeployAdmin primary = null;
        ProvisionMonitor.PeerInfo[] peers = provisionMonitorPeer.getBackupInfo();
        for (ProvisionMonitor.PeerInfo peer : peers) {
            ProvisionMonitor peerMon = peer.getService();
            try {
                DeployAdmin dAdmin = (DeployAdmin) peerMon.getAdmin();
                if (dAdmin.hasDeployed(opStringName)) {
                    OperationalStringManager mgr = dAdmin.getOperationalStringManager(opStringName);
                    if (mgr.isManaging()) {
                        primary = dAdmin;
                        break;
                    }
                }
            } catch (RemoteException e) {
                if (logger.isDebugEnabled())
                    logger.debug("Communicating to peer while searching for " +
                                 "primary testManager of [" + opStringName + "]",
                                 e);
            } catch (OperationalStringException e) {
                /* ignore */
            }
        }
        return primary;
    }

    /**
     * Remove an OpStringManager from the collection of managed OpStringManagers
     *
     * @param opStringManager The OpStringManager to remove
     */
    public boolean remove(final OpStringManager opStringManager) {
        return  opStringManagerTable.remove(opStringManager.getName())!=null;
    }

    /**
     * Add an OpStringManager to this ProvisionMonitor.
     *
     * @param opString The OperationalString to add
     * @param map      A Map to store any exceptions produced while loading the
     *                 opString
     * @param parent   The parent of the opString. The addOperationalString
     *                 method recursively processes OperationalString instances and uses this
     *                 parameter to indicate a nested OperationalString
     * @param dAdmin   The managing DeployAdmin, if the OperationalString is
     *                 being added in an inactive mode, as a backup. If this value is null,
     *                 this ProvisionMonitor is set to be active
     * @param listener A ServiceProvisionListener that will be set to the
     *                 OperationalStringManager for notification of services as they are
     *                 deployed initially.
     * @return An OpStringManager
     * @throws IOException if an OpStringManger cannot be created
     */
    public  OpStringManager addOperationalString(final OperationalString opString,
                                                final Map<String, Throwable> map,
                                                final OpStringManager parent,
                                                final DeployAdmin dAdmin,
                                                final ServiceProvisionListener listener) throws IOException {
        /* If there is no DeployAdmin active is true */
        boolean active = dAdmin == null;
        OpStringManager opMgr = new DefaultOpStringManager(opString, parent, config, this);
        if (opStringManagerTable.putIfAbsent(opString.getName(), opMgr) == null) {
            if (logger.isInfoEnabled())
                logger.info("Adding OpString [" + opString.getName() + "] active [" + active + "]");
            ((DefaultOpStringManager) opMgr).setServiceProxy(serviceProxy);
            ((DefaultOpStringManager) opMgr).setEventProcessor(eventProcessor);
            ((DefaultOpStringManager) opMgr).setStateManager(stateManager);

            opMgr.initialize(active);
            Map<String, Throwable> errorMap =
                ((DefaultOpStringManager) opMgr).createServiceElementManagers(serviceProvisioner, uuid, listener);
            if (dAdmin != null) {
                OperationalStringManager activeMgr;
                Map<ServiceElement, ServiceBeanInstance[]> elemInstanceMap = new HashMap<ServiceElement, ServiceBeanInstance[]>();
                try {
                    activeMgr = dAdmin.getOperationalStringManager(opString.getName());
                    ServiceElement[] elems = opString.getServices();
                    for (ServiceElement elem : elems) {
                        try {
                            ServiceBeanInstance[] instances = activeMgr.getServiceBeanInstances(elem);
                            elemInstanceMap.put(elem, instances);
                        } catch (Exception e) {
                            logger.warn("Getting ServiceBeanInstances from active testManager", e);
                        }
                    }
                    ((DefaultOpStringManager) opMgr).startManager(listener, elemInstanceMap);
                } catch (Exception e) {
                    logger.warn("Getting active OperationalStringManager", e);
                }
            } else {
                ((DefaultOpStringManager) opMgr).startManager(listener);
            }

            if (map != null)
                map.putAll(errorMap);
            if (stateManager != null)
                stateManager.stateChanged(opMgr, false);
            OperationalString[] nestedStrings = opString.getNestedOperationalStrings();
            if (logger.isTraceEnabled()) {
                logger.trace("[{}] nested OperationalString count: [{}]", opString.getName(), nestedStrings.length);
            }
            for (OperationalString nestedString : nestedStrings) {
                if (logger.isTraceEnabled()) {
                    logger.trace("Processing nested OperationalString [{}]", nestedString.getName());
                }
                addOperationalString(nestedString, map, opMgr, dAdmin, listener);
                if (logger.isTraceEnabled()) {
                    logger.trace("Completed processing nested OperationalString [{}]", nestedString.getName());
                }
            }
        }

        if (active) {
            ProvisionMonitorEvent event = new ProvisionMonitorEvent(serviceProxy,
                                                                    ProvisionMonitorEvent.Action.OPSTRING_DEPLOYED,
                                                                    opMgr.doGetOperationalString());
            if (opMgr.getOAR() != null)
                event.setRemoteRepositories(opMgr.getRemoteRepositories());
            eventProcessor.processEvent(event);
        }
        if (logger.isTraceEnabled())
            logger.trace("Return from add OperationalString [{}], mgr: {}", opString.getName(), opMgr);
        return opMgr;
    }

    public void shutdownAllManagers() {
        for (OpStringManager opMgr : getOpStringManagers()) {
            opMgr.terminate(false);
        }
    }

    public OpStringManager[] getOpStringManagers() {
        Collection<OpStringManager> mgrs = opStringManagerTable.values();
        return mgrs.toArray(new OpStringManager[mgrs.size()]);
    }


    /**
     * Determine if the OperationalString with the provided name is deployed
     *
     * @param opStringName The name of the OperationalString
     * @return true if the  OperationalString with the provided name is loaded
     */
    public synchronized boolean opStringExists(final String opStringName) {
        return opStringManagerTable.get(opStringName)!=null;
    }

    /**
     * Dump an error map associated with the loading and/or addition of an
     * OperationalString
     *
     * @param errorMap The map containing exceptions
     */
    public void dumpOpStringError(Map errorMap) {
        if (!errorMap.isEmpty()) {
            Set keys = errorMap.keySet();
            StringBuilder sb = new StringBuilder();
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
            if (logger.isDebugEnabled())
                logger.debug(sb.toString());
        }
    }

    class Deployer implements Callable<OpStringManager> {

        @Override
        public OpStringManager call() throws Exception {
            return null;
        }
    }
}
