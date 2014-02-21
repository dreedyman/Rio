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
package org.rioproject.monitor.service.tasks;

import org.rioproject.deploy.DeployAdmin;
import org.rioproject.deploy.DeploymentResult;
import org.rioproject.monitor.ProvisionMonitor;
import org.rioproject.monitor.service.OpStringManagerController;
import org.rioproject.monitor.service.peer.ProvisionMonitorPeer;
import org.rioproject.monitor.service.persistence.StateManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URL;
import java.rmi.RemoteException;
import java.util.TimerTask;

/**
 * Scheduled Task which will load configured OperationalString files
 */
public class InitialOpStringLoadTask extends TimerTask {
    private final ProvisionMonitorPeer provisionMonitorPeer;
    private final StateManager stateManager;
    private final DeployAdmin deployAdmin;
    private final OpStringManagerController opStringMangerController;
    private final String[] initialOpStrings;
    static final Logger logger = LoggerFactory.getLogger(InitialOpStringLoadTask.class.getName());

    public InitialOpStringLoadTask(final String[] initialOpStrings,
                                   final DeployAdmin deployAdmin,
                                   final ProvisionMonitorPeer provisionMonitorPeer,
                                   final OpStringManagerController opStringMangerController,
                                   final StateManager stateManager) {
        this.initialOpStrings = initialOpStrings;
        this.provisionMonitorPeer = provisionMonitorPeer;
        this.deployAdmin = deployAdmin;
        this.opStringMangerController = opStringMangerController;
        this.stateManager = stateManager;
    }

    /**
     * The action to be performed by this timer task.
     */
    public void run() {
        if (stateManager!=null && stateManager.inRecovery())
            return;
        try {
            /*
             * Wait for all peers to be in a "ready" state. Once this
             * method returns begin our own initialOpStrings and OARs
             * deployment, setting the PeerInfo state to
             * LOADING_INITIAL_DEPLOYMENTS.
             */
            waitForPeers();
            ProvisionMonitor.PeerInfo peerInfo = provisionMonitorPeer.doGetPeerInfo();
            peerInfo.setInitialDeploymentLoadState(ProvisionMonitor.PeerInfo.LOADING_INITIAL_DEPLOYMENTS);

            /* Load initialOpStrings */
            for (String initialOpString : initialOpStrings) {
                URL opstringURL;
                try {
                    if (initialOpString.startsWith("http:"))
                        opstringURL = new URL(initialOpString);
                    else
                        opstringURL = new File(initialOpString).toURI().toURL();
                    DeploymentResult result = deployAdmin.deploy(opstringURL, null);
                    opStringMangerController.dumpOpStringError(result.getErrorMap());
                } catch (Throwable t) {
                    logger.warn("Loading OperationalString : " +initialOpString, t);
                }
            }

        } finally {
            ProvisionMonitor.PeerInfo peerInfo = provisionMonitorPeer.doGetPeerInfo();
            peerInfo.setInitialDeploymentLoadState(ProvisionMonitor.PeerInfo.LOADED_INITIAL_DEPLOYMENTS);
        }
    }

    /**
     * Wait until peers are ready.
     * <p/>
     * if a peer's initialDeploymentState is LOADING_INITIAL_DEPLOYMENTS,
     * wait until the LOADED_INITIAL_DEPLOYMENTS state is set. If a peer
     * has not loaded (INITIAL_DEPLOYMENTS_PENDING) this is fine as well.
     * Once we find that all peers are ready, return
     */
    void waitForPeers() {
        ProvisionMonitor.PeerInfo[] peers = provisionMonitorPeer.getBackupInfo();
        long t0 = System.currentTimeMillis();
        if (logger.isDebugEnabled())
            logger.debug("Number of peers to wait on [" + peers.length + "]");
        if (peers.length == 0)
            return;
        boolean peersReady = false;
        while (!peersReady) {
            int numPeersReady = 0;
            StringBuilder b = new StringBuilder();
            b.append("ProvisionMonitor Peer verification\n");
            for (int i = 0; i < peers.length; i++) {
                if (i > 0)
                    b.append("\n");
                try {
                    ProvisionMonitor peer = peers[i].getService();
                    ProvisionMonitor.PeerInfo peerInfo = peer.getPeerInfo();
                    int state = peerInfo.getInitialDeploymentLoadState();
                    b.append("Peer at ");
                    b.append(peerInfo.getAddress());
                    b.append(", " + "state=");
                    b.append(getStateName(state));
                    switch (state) {
                        case ProvisionMonitor.PeerInfo.INITIAL_DEPLOYMENTS_PENDING:
                            numPeersReady++;
                            break;
                        case ProvisionMonitor.PeerInfo.LOADED_INITIAL_DEPLOYMENTS:
                            numPeersReady++;
                            break;
                        case ProvisionMonitor.PeerInfo.LOADING_INITIAL_DEPLOYMENTS:
                            break;
                    }
                } catch (RemoteException e) {
                    b.append("Peer [" + 0 + "] exception " + "[");
                    b.append(e.getMessage());
                    b.append("], continue");
                    if (logger.isTraceEnabled())
                        logger.trace("Getting PeerInfo", e);
                    numPeersReady++;
                }

            }
            if (logger.isDebugEnabled())
                logger.debug(b.toString());
            b.delete(0, b.length());
            if (numPeersReady == peers.length) {
                peersReady = true;
            } else {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        long t1 = System.currentTimeMillis();
        if (logger.isDebugEnabled())
            logger.debug("Peer state resolution took " + (t1 - t0) + " millis");
    }

    private String getStateName(int state) {
        String name;
        switch (state) {
            case ProvisionMonitor.PeerInfo.INITIAL_DEPLOYMENTS_PENDING:
                name = "INITIAL_DEPLOYMENTS_PENDING";
                break;
            case ProvisionMonitor.PeerInfo.LOADED_INITIAL_DEPLOYMENTS:
                name = "LOADED_INITIAL_DEPLOYMENTS";
                break;
            default:
                name = "LOADING_INITIAL_DEPLOYMENTS";
        }
        return (name);

    }
}
