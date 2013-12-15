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
package org.rioproject.monitor.service.peer;

import net.jini.admin.Administrable;
import net.jini.config.Configuration;
import net.jini.core.entry.Entry;
import net.jini.core.lookup.ServiceID;
import net.jini.core.lookup.ServiceItem;
import net.jini.core.lookup.ServiceTemplate;
import net.jini.discovery.DiscoveryManagement;
import net.jini.lease.LeaseRenewalManager;
import net.jini.lookup.LookupCache;
import net.jini.lookup.ServiceDiscoveryEvent;
import net.jini.lookup.ServiceDiscoveryManager;
import net.jini.lookup.entry.ServiceInfo;
import net.jini.security.BasicProxyPreparer;
import net.jini.security.ProxyPreparer;
import org.rioproject.RioVersion;
import org.rioproject.admin.ServiceBeanAdmin;
import org.rioproject.deploy.DeployAdmin;
import org.rioproject.deploy.ServiceBeanInstance;
import org.rioproject.deploy.ServiceProvisionListener;
import org.rioproject.event.EventDescriptor;
import org.rioproject.event.RemoteServiceEvent;
import org.rioproject.event.RemoteServiceEventListener;
import org.rioproject.impl.client.ServiceDiscoveryAdapter;
import org.rioproject.impl.event.BasicEventConsumer;
import org.rioproject.impl.fdh.FaultDetectionHandler;
import org.rioproject.impl.fdh.FaultDetectionHandlerFactory;
import org.rioproject.impl.fdh.FaultDetectionListener;
import org.rioproject.impl.system.ComputeResource;
import org.rioproject.monitor.ProvisionMonitor;
import org.rioproject.monitor.ProvisionMonitorEvent;
import org.rioproject.monitor.service.*;
import org.rioproject.monitor.service.channel.ServiceChannel;
import org.rioproject.monitor.service.channel.ServiceChannelEvent;
import org.rioproject.monitor.service.tasks.PeerNotificationTask;
import org.rioproject.opstring.*;
import org.rioproject.resolver.RemoteRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.rmi.RemoteException;
import java.util.*;

/**
 * Class that manages the discovery of other ProvisionMonitor instances, handles the registration and 
 * notification of ProvisionMonitorEvent occurrences, and determines backup ProvisionMonitor capabilities
 */
public class ProvisionMonitorPeer extends ServiceDiscoveryAdapter implements RemoteServiceEventListener,
                                                                             FaultDetectionListener<ServiceID>,
                                                                             Runnable {
    static Logger peerLogger = LoggerFactory.getLogger(ProvisionMonitorPeer.class.getName());
    static final String CONFIG_COMPONENT = "org.rioproject.monitor";
    /** The PeerInfo object for this ProvisionMonitor */
    private ProvisionMonitor.PeerInfo myPeerInfo;
    /**
     * The BasicEventConsumer providing notification of
     * ProvisionMonitorEvents
     */
    private BasicEventConsumer eventConsumer;
    /** Table of Peer ProvisionMonitor instances to OpStringManagers which
     * are backups for the peer */
    private final Map<ProvisionMonitor, List<OpStringManager>> opStringTable =
        new HashMap<ProvisionMonitor, List<OpStringManager>>();
    /** The ProvisionMonitor instances this ProvisionMonitor is a backup for */
    private final List<ProvisionMonitor> backupList = new ArrayList<ProvisionMonitor>();
    /** The ProvisionMonitor that is our backup */
    private ProvisionMonitor backup;
    /** Ordered set of ProvisionMonitor instances */
    private final Set<ProvisionMonitor.PeerInfo> peerSet = new TreeSet<ProvisionMonitor.PeerInfo>();
    /** Flag which indicates my backup is local, on the same machine */
    private boolean localBackup = false;
    /**
     * Table of service IDs to FaultDetectionHandler instances, one for each service
     */
    private final Hashtable<ServiceID, FaultDetectionHandler> fdhTable = new Hashtable<ServiceID, FaultDetectionHandler>();
    /** ServiceTemplate for ProvisionMonitor discovery */
    private ServiceTemplate template;
    /** DiscoveryManagement instance */
    private DiscoveryManagement dm;
    /** ServiceDiscoveryManager for ProvisionMonitor instances */
    private ServiceDiscoveryManager sdm;
    /** The LookupCache for the ServiceDiscoveryManager */
    private LookupCache lCache;
    private ProxyPreparer peerProxyPreparer;
    private Configuration config;
    private ComputeResource computeResource;
    private ProvisionMonitor serviceProxy;
    private ProvisionMonitorEventProcessor eventProcessor;
    private OpStringManagerController opStringMangerController;

    public void initialize() throws Exception {
        if(config==null)
            throw new IllegalStateException("no configuration");
        if(serviceProxy==null)
            throw new IllegalStateException("no serviceProxy");
        if(dm==null)
            throw new IllegalStateException("no DiscoveryManagement");
        if(eventProcessor==null)
            throw new IllegalStateException("no eventProcessor");
        if(opStringMangerController==null)
            throw new IllegalStateException("no opStringMangerController");

        peerProxyPreparer = (ProxyPreparer)config.getEntry(CONFIG_COMPONENT,
                                                           "peerProxyPreparer",
                                                           ProxyPreparer.class,
                                                           new BasicProxyPreparer());
        /* Create the PeerInfo object */
        java.util.Random rand = new java.util.Random(System.currentTimeMillis());
        long randomNumber = rand.nextLong();
        myPeerInfo = new ProvisionMonitor.PeerInfo(getServiceProxy(),
                                                   randomNumber,
                                                   computeResource.getAddress().getHostAddress());
        eventConsumer = new BasicEventConsumer(new EventDescriptor(ProvisionMonitorEvent.class,
                                                                   ProvisionMonitorEvent.ID),
                                               this,
                                               config);
        template = new ServiceTemplate(null, new Class[] {ProvisionMonitor.class}, null);
        new Thread(this).start();
    }


    public void setOpStringMangerController(final OpStringManagerController opStringMangerController) {
        this.opStringMangerController = opStringMangerController;
    }

    public void setEventProcessor(ProvisionMonitorEventProcessor eventProcessor) {
        this.eventProcessor = eventProcessor;
    }

    public void setDiscoveryManagement(final DiscoveryManagement dm) {
        this.dm = dm;
    }

    ProvisionMonitor getServiceProxy() {
        return serviceProxy;
    }

    public void setServiceProxy(final ProvisionMonitor serviceProxy) {
        this.serviceProxy = serviceProxy;
    }

    public void setConfig(final Configuration config) {
        this.config = config;
    }

    public void setComputeResource(final ComputeResource computeResource) {
        this.computeResource = computeResource;
    }

    /**
     * Terminate the ProvisionMonitorPeer, cleaning up listeners, etc...
     */
    public void terminate() {
        for(Enumeration<FaultDetectionHandler> en = fdhTable.elements();
            en.hasMoreElements();) {
            FaultDetectionHandler fdh = en.nextElement();
            fdh.terminate();
        }
        eventConsumer.deregister(this);
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
    public boolean addAsBackupFor(final ProvisionMonitor primary) {
        boolean assigned;
        int listSize;
        synchronized(backupList) {
            assigned = backupList.add(primary);
            listSize = backupList.size();
        }
        if(assigned) {
            ProvisionMonitor.PeerInfo peer = getPeerInfo(primary);
            ProvisionMonitor[] provisioners = getProvisionMonitorPeers();
            /* if we dont know about the new primary, add it into the mix */
            if(peer == null) {
                ProvisionMonitor[] adjustedProvisioners = new ProvisionMonitor[provisioners.length + 1];
                if(provisioners.length > 0)
                    System.arraycopy(provisioners, 0, adjustedProvisioners, 0, provisioners.length);
                adjustedProvisioners[provisioners.length] = primary;
                provisioners = adjustedProvisioners;
            }
            myPeerInfo.setBackupCount(listSize);
            new Thread(new PeerNotificationTask(provisioners, myPeerInfo)).start();
        }
        return (assigned);
    }

    /**
     * Remove the ProvisionMonitor from the backup list
     *
     * @param primary The ProvisionMonitor to remove
     * @return True if removed from the collection, false if not
     */
    public boolean removeAsBackupFor(final ProvisionMonitor primary) {
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
    public ProvisionMonitor.PeerInfo doGetPeerInfo() {
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
    public ProvisionMonitor.PeerInfo[] getBackupInfo() {
        List<ProvisionMonitor.PeerInfo> list = new ArrayList<ProvisionMonitor.PeerInfo>();
        synchronized(peerSet) {
            for (ProvisionMonitor.PeerInfo aPeerSet : peerSet) {
                list.add(aPeerSet);
            }
        }
        return(list.toArray(new ProvisionMonitor.PeerInfo[list.size()]));
    }

    /**
     * Update the backup count for a ProvisionMonitor peer
     *
     * @param peer The PeerInfo
     */
    public void peerUpdated(final ProvisionMonitor.PeerInfo peer) {
        if(peer == null) {
            peerLogger.debug("updatePeer(): Unknown ProvisionMonitor");
            return;
        }
        synchronized(peerSet) {
            if(peerSet.remove(peer)) {
                peerSet.add(peer);
            }
        }
        peerLogger.debug("ProvisionMonitorPeer: ProvisionMonitor updated info :\n\tcount={}, address={}, id={}, ",
                         peer.getBackupCount(), peer.getAddress(), peer.getID().toString());
    }

    /**
     * Add a ProvisionMonitor
     *
     * @param item The ServiceItem for the ProvisionMonitor
     * @return The PeerInfo object
     */
    ProvisionMonitor.PeerInfo addProvisioner(final ServiceItem item) {
        ProvisionMonitor.PeerInfo peer = null;
        try {
            if(getServiceProxy().equals(item.service))
                return (null);
            peer = ((ProvisionMonitor)item.service).getPeerInfo();
            synchronized(peerSet) {
                peerSet.add(peer);
            }
            importOperationalStrings(((ProvisionMonitor)item.service));
            eventConsumer.register(item);
        } catch(Exception e) {
            peerLogger.warn("Adding a ProvisionMonitor peer instance", e);
        }
        return (peer);
    }

    /**
     * Notification that a Provisioner has been discovered
     *
     * @param sdEvent The ServiceDiscoveryEvent
     */
    public void serviceAdded(final ServiceDiscoveryEvent sdEvent) {
        ServiceItem item = sdEvent.getPostEventServiceItem();
        /* prepare the newly discovered proxy */
        try {
            item.service = peerProxyPreparer.prepareProxy(item.service);
        } catch (RemoteException e) {
            peerLogger.warn("Could not prepare peer proxy, not adding as peer", e);
            return;
        }

        /* Get ServiceInfo, determine if we have a version conflict. */
        ServiceInfo serviceInfo = null;
        for(Entry entry : item.attributeSets) {
            if(entry instanceof ServiceInfo) {
                serviceInfo = (ServiceInfo)entry;
                break;
            }
        }
        if(serviceInfo==null) {
            peerLogger.warn("Unknown ProvisionMonitor, no {}, will not register with peer", ServiceInfo.class.getName());
            return;
        }
        if(serviceInfo.version==null || serviceInfo.version.length()==0) {
            peerLogger.warn("Unable to determine ProvisionMonitor {} version information, will not register with peer",
                            ServiceInfo.class.getName());
            return;
        }
        String version;
        if(serviceInfo.version.startsWith("v")) {
            version = serviceInfo.version.substring(1, serviceInfo.version.length());
        } else {
            version = serviceInfo.version;
        }
        String[] versionParts = version.split("-");
        if(versionParts.length==0) {
            peerLogger.warn("Unable to determine ProvisionMonitor version information, " +
                            "found [{}], unknown format, will not register with peer", version);
            return;
        }
        String versionNumber;
        if(versionParts[0].endsWith("-SNAPSHOT")) {
            int ndx = versionParts[0].indexOf("-SNAPSHOT");
            versionNumber = versionParts[0].substring(0, ndx);
        } else {
            versionNumber = versionParts[0];
        }
        double actualVersionNumber;
        try {
            actualVersionNumber = Double.valueOf(versionNumber);
        } catch(NumberFormatException e) {
            peerLogger.warn("Unable to determine ProvisionMonitor version information, " +
                            "found [{}], unknown format, will not register with peer", versionNumber,
                            e);
            return;
        }
        if(actualVersionNumber<5) {
            peerLogger.warn("Discovered ProvisionMonitor with version {}, unsupported for {}, will not register with peer",
                            versionNumber, RioVersion.VERSION);
            return;
        }
        /* Check if we already know about this guy. If so, just return */
        ProvisionMonitor.PeerInfo peer = getPeerInfo((ProvisionMonitor)item.service);
        if(peer != null)
            return;
        /* Add the new provisioner to the set */
        peer = addProvisioner(item);
        /* If peer is null, then we just discovered ourselves */
        if(peer == null)
            return;
        /*
         * If we dont have a backup yet, use the newly discovered provisioner
         */
        if(backup == null) {
            peerLogger.debug("No backup yet, try assignment");
            doAssignment(peer);
        } else {
            /*
             * If our backup is a local backup (same machine), and the newly
             * added peer is not on the same machine, assign the new
             * provisioner as the backup
             */
            //if(localBackup &&
            //   (!computeResource.getAddress().equals(peer.getAddress()))) {

            String resourceAddress = computeResource.getAddress().getHostAddress();
            String resourceHostName =  computeResource.getAddress().getHostName();

            if(localBackup && !(resourceAddress.equals(peer.getAddress()) ||
                                resourceHostName.equals(peer.getAddress())) ) {

                ProvisionMonitor currentBackup = backup;
                if(doAssignment(peer)) {
                    try {
                        currentBackup.removeBackupFor(getServiceProxy());
                    } catch(Exception e) {
                        peerLogger.warn("Adjusting backup instance away from local instance", e);
                    }
                }
            }
        }
        setFaultDetectionHandler(item.service, item.serviceID);
    }

    /**
     * A ProvisionMonitor has failed
     */
    public void serviceFailure(final Object service, final ServiceID serviceID) {
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
        ProvisionMonitor.PeerInfo info = getPeerInfo(provMon);
        if(info == null) {
            peerLogger.debug("serviceFailure: Unknown ProvisionMonitor");
            return;
        }
        synchronized(peerSet) {
            if(peerSet.remove(info))
                peerLogger.debug("ProvisionMonitorPeer: Removed ProvisionMonitor at [{}]", info.getAddress());
        }
        /*
         * Check to see if the ProvisionMonitor that just left the network
         * is our back-up. If so, locate another ProvisionMonitor and set
         * that as the backup
         */
        if(backup != null && backup.equals(provMon)) {
            peerLogger.debug("ProvisionMonitorPeer: Backup has left the network, locate another backup");
            if(!assignBackup()) {
                backup = null;
                peerLogger.debug("ProvisionMonitorPeer: No backup located");
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
            peerLogger.trace("Number of OpStringManager instances that are back ups : {}", opMgrList.size());
            for (OpStringManager opMgr : opMgrList) {
                opMgr.setActive(true);
                ProvisionMonitorEvent event = new ProvisionMonitorEvent(getServiceProxy(),
                                                                        ProvisionMonitorEvent.Action.OPSTRING_MGR_CHANGED,
                                                                        opMgr.doGetOperationalString());
                eventProcessor.processEvent(event);
                peerLogger.debug("Set active for opstring : {}", opMgr.getName());
            }
        } else {
            peerLogger.debug("No backup OpStringManagers for removed peer");
        }
        /* Remove the FaultDetectionHandler from the table */
        fdhTable.remove(serviceID);
    }

    /**
     * Notify ProvisionMonitor peers of a change in PeerInfo
     *
     * @param info PeerInfo
     */
    void notifyPeers(final ProvisionMonitor.PeerInfo info) {
        new Thread(new PeerNotificationTask(getProvisionMonitorPeers(),
                                            info)).start();
    }

    /**
     * Assign a backup
     *
     * @return true is successful
     */
    boolean assignBackup() {
        boolean assignedBackup = false;
        ProvisionMonitor.PeerInfo[] peers = getPeers();
        if(peers.length > 0) {
            for (ProvisionMonitor.PeerInfo peer : peers) {
                String resourceAddress = computeResource.getAddress().getHostAddress();
                String resourceHostName =  computeResource.getAddress().getHostName();
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
     * @return true if assignment succeeded
     */
    boolean doAssignment(final ProvisionMonitor.PeerInfo peer) {
        boolean assigned = false;
        try {
            backup = peer.getService();
            backup.assignBackupFor(getServiceProxy());
            assigned = true;
            peerLogger.debug("ProvisionMonitorPeer: ProvisionMonitor backup info :\n\tcount={}, address={}, id={}",
                             peer.getBackupCount(), peer.getAddress(), peer.getID().toString());
        } catch(Exception e) {
            peerLogger.warn("Assigning ProvisionMonitor backup", e);
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
    ProvisionMonitor.PeerInfo[] getPeers() {
        ProvisionMonitor.PeerInfo[] peers;
        synchronized(peerSet) {
            peers = peerSet.toArray(new ProvisionMonitor.PeerInfo[peerSet.size()]);
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
    public ProvisionMonitor[] getProvisionMonitorPeers() {
        ProvisionMonitor.PeerInfo[] peers = getPeers();
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
    ProvisionMonitor.PeerInfo getPeerInfo(final ProvisionMonitor monitor) {
        ProvisionMonitor.PeerInfo[] peers = getPeers();
        ProvisionMonitor.PeerInfo peer = null;
        for (ProvisionMonitor.PeerInfo peer1 : peers) {
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
    public void notify(final RemoteServiceEvent event) {
        try {
            Object eventSource = event.getSource();
            if(!(eventSource instanceof ProvisionMonitor)) {
                peerLogger.debug("ProvisionMonitorPeer: unknown event source : {}", eventSource.getClass().getName());
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
                        peerLogger.debug("ProvisionMonitorPeer: {} sElem is null", sAction);
                        return;
                    }
                    opStringName = sElem.getOperationalStringName();
                    peerLogger.trace("ProvisionMonitorPeer: {}, opstring: {}", sAction, opStringName);
                    opMgr = opStringMangerController.getOpStringManager(opStringName);
                    if(opMgr == null) {
                        peerLogger.debug("ProvisionMonitorPeer: {} opstring [{}] not found", sAction, opStringName);
                        return;
                    }
                    try {
                        opMgr.doUpdateServiceElement(sElem);
                    } catch(Exception e) {
                        peerLogger.warn("Updating OperationalStringManager's ServiceElement", e);
                    }
                    break;
                case SERVICE_ELEMENT_ADDED:
                    if(sElem == null) {
                        peerLogger.warn("ProvisionMonitorPeer: SERVICE_ELEMENT_ADDED sElem is null");
                        return;
                    }
                    opStringName = sElem.getOperationalStringName();
                    peerLogger.trace("ProvisionMonitorPeer: SERVICE_ELEMENT_ADDED, opstring: {}", opStringName);
                    opMgr = opStringMangerController.getOpStringManager(opStringName);
                    if(opMgr == null) {
                        peerLogger.debug("ProvisionMonitorPeer: SERVICE_ELEMENT_ADDED opstring [{}] not found" ,opStringName);
                        return;
                    }
                    try {
                        opMgr.doAddServiceElement(sElem, null);
                    } catch(Exception e) {
                        peerLogger.warn("Adding ServiceElement to OperationalStringManager", e);
                    }
                    break;
                case SERVICE_ELEMENT_REMOVED:
                    if(sElem == null) {
                        peerLogger.debug("ProvisionMonitorPeer: SERVICE_ELEMENT_REMOVED sElem is null");
                        return;
                    }
                    opStringName = sElem.getOperationalStringName();
                    peerLogger.trace("ProvisionMonitorPeer: SERVICE_ELEMENT_REMOVED, opstring: {}", opStringName);
                    opMgr = opStringMangerController.getOpStringManager(opStringName);
                    if(opMgr == null) {
                        peerLogger.debug("ProvisionMonitorPeer: SERVICE_ELEMENT_REMOVED opstring [{}] not found",
                                         opStringName);
                        return;
                    }
                    try {
                        opMgr.doRemoveServiceElement(sElem, false);
                    } catch(Exception e) {
                        peerLogger.warn("Removing ServiceElement from OperationalStringManager", e);
                    }
                    break;
                case OPSTRING_DEPLOYED:
                    if(opString == null) {
                        peerLogger.warn("ProvisionMonitorPeer: OPSTRING_DEPLOYED opstring is null");
                        return;
                    }
                    DeployAdmin deployAdmin = (DeployAdmin)remoteMonitor.getAdmin();
                    opStringProcessor(opString, remoteMonitor, deployAdmin, pme.getRemoteRepositories());
                    peerLogger.trace("ProvisionMonitorPeer: OPSTRING_DEPLOYED, opstring: {}", opString.getName());
                    break;
                case OPSTRING_UNDEPLOYED:
                    if(opString == null) {
                        peerLogger.debug("ProvisionMonitorPeer: OPSTRING_UNDEPLOYED opstring is null");
                        return;
                    }
                    peerLogger.trace("ProvisionMonitorPeer: OPSTRING_UNDEPLOYED, opstring: {}", opString.getName());
                    opMgr = opStringMangerController.getOpStringManager(opString.getName());
                    if(opMgr == null) {
                        peerLogger.debug("ProvisionMonitorPeer: OPSTRING_UNDEPLOYED for opstring [{}] not found",
                                         opString.getName());
                        return;
                    }

                    opMgr.setDeploymentStatus(OperationalString.UNDEPLOYED);
                    opMgr.terminate(false);
                    synchronized(opStringTable) {
                        if(opStringTable.containsKey(remoteMonitor)) {
                            List<OpStringManager> list = opStringTable.get(remoteMonitor);
                            list.remove(opMgr);
                            opStringTable.put(remoteMonitor, list);
                        }
                    }
                    break;

                case OPSTRING_MGR_CHANGED:
                    if(opString == null) {
                        peerLogger.debug("ProvisionMonitorPeer: OPSTRING_MGR_CHANGED opstring is null");
                        return;
                    }
                    peerLogger.debug("ProvisionMonitorPeer: OPSTRING_MGR_CHANGED, opstring: {}", opString.getName());
                    opMgr = opStringMangerController.getOpStringManager(opString.getName());
                    if(opMgr == null) {
                        peerLogger.debug("ProvisionMonitorPeer: OPSTRING_MGR_CHANGED opstring [{}] not found",
                                         opString.getName());
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
                            peerLogger.debug("ProvisionMonitorPeer: Reset backup peer for [{}] to {}",
                                             opString.getName(), remoteMonitor.toString());
                        } else {
                            peerLogger.debug("ProvisionMonitorPeer: Already a backup for [{}] to {}",
                                             opString.getName(), remoteMonitor.toString());
                        }
                    }
                    break;
                case OPSTRING_UPDATED:
                    if(opString == null) {
                        peerLogger.debug("ProvisionMonitorPeer: OPSTRING_UNDEPLOYED opstring is null");
                        return;
                    }
                    peerLogger.trace("ProvisionMonitorPeer: OPSTRING_UPDATED, opstring: {}", opString.getName());
                    opMgr = opStringMangerController.getOpStringManager(opString.getName());
                    if(opMgr == null) {
                        peerLogger.debug("ProvisionMonitorPeer: OPSTRING_UPDATED for opstring [{}] not found",
                                         opString.getName());
                        return;
                    }
                    opMgr.doUpdateOperationalString(opString);
                    break;

                case SERVICE_BEAN_INSTANCE_UPDATED:
                    ServiceBeanInstance instance = pme.getServiceBeanInstance();
                    if(instance == null) {
                        peerLogger.debug("ProvisionMonitorPeer: SERVICE_BEAN_INSTANCE_UPDATED instance is null");
                        return;
                    }
                    opStringName = pme.getOperationalStringName();
                    peerLogger.trace("ProvisionMonitorPeer: SERVICE_BEAN_INSTANCE_UPDATED, opstring: {}", opStringName);
                    opMgr = opStringMangerController.getOpStringManager(opStringName);
                    if(opMgr == null) {
                        peerLogger.debug("ProvisionMonitorPeer: SERVICE_BEAN_INSTANCE_UPDATED for opstring [{}] not found",
                                         opStringName);
                        return;
                    }
                    try {
                        opMgr.doUpdateServiceBeanInstance(instance);
                    } catch(Exception e) {
                        peerLogger.warn("Updating OperationalStringManager's ServiceBeanInstance", e);
                    }
                    break;

                case SERVICE_PROVISIONED:
                    instance = pme.getServiceBeanInstance();
                    if(instance == null) {
                        peerLogger.debug("ProvisionMonitorPeer: SERVICE_PROVISIONED instance is null");
                        return;
                    }
                    opStringName = pme.getOperationalStringName();
                    peerLogger.trace("ProvisionMonitorPeer: SERVICE_PROVISIONED, opstring: {}", opStringName);
                    opMgr = opStringMangerController.getOpStringManager(opStringName);
                    if(opMgr == null) {
                        peerLogger.debug("ProvisionMonitorPeer: SERVICE_PROVISIONED for opstring [{}] " +
                                         "OpStringManager not found", opStringName);
                        return;
                    }
                    ServiceElementManager mgr = opMgr.getServiceElementManager(sElem);
                    if(mgr==null) {
                        peerLogger.debug("ProvisionMonitorPeer: SERVICE_PROVISIONED for " +
                                         "opstring [{}] ServiceElementManager not found", opStringName);
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
                    channel.broadcast(new ServiceChannelEvent(this, sElem, ServiceChannelEvent.Type.PROVISIONED));
                    break;

                case REDEPLOY_REQUEST:
                    opMgr = opStringMangerController.getOpStringManager(pme.getOperationalStringName());
                    if(opMgr == null) {
                        peerLogger.debug("ProvisionMonitorPeer: REDEPLOY_REQUEST opstring [{}] not found", opStringName);
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
                        peerLogger.debug("ProvisionMonitorPeer: REDEPLOY_REQUEST for opstring " +
                                         "[{}] startTime has already passed, scheduled " +
                                         "redeployment cancelled", opStringName);
                    } else  {
                        opMgr.doScheduleRedeploymentTask(delay,
                                                         pme.getServiceElement(),
                                                         pme.getServiceBeanInstance(),
                                                         clean,
                                                         sticky,
                                                         listener);
                    }
                    break;
            }
        } catch(Throwable t) {
            peerLogger.warn("ProvisionMonitorEvent notification", t);
        }
    }

    /**
     * Perform initial discovery in a thread
     */
    public void run() {
        try {
            if(config == null)
                sdm = new ServiceDiscoveryManager(dm, new LeaseRenewalManager());
            else
                sdm = new ServiceDiscoveryManager(dm, new LeaseRenewalManager(config), config);
            lCache = sdm.createLookupCache(template, null, this);
            if(!assignBackup())
                peerLogger.debug("ProvisionMonitorPeer: No backup");
        } catch(Exception e) {
            peerLogger.warn("ProvisionMonitor discovery", e);
        }
    }

    /**
     * Set the FaultDetectionHandler for a newly discovered ProvisionMonitor
     *
     * @param service The ProvisionMonitor's proxy
     * @param serviceID The serviceID of the ProvisionMonitor
     */
    void setFaultDetectionHandler(final Object service, final ServiceID serviceID) {
        try {
            if(serviceID == null) {
                peerLogger.info("No ServiceID for newly discovered ProvisionMonitor, cant setup FDH");
                return;
            }
            ServiceBeanAdmin sbAdmin = (ServiceBeanAdmin)((Administrable)service).getAdmin();
            ClassBundle fdhBundle = sbAdmin.getServiceElement().getFaultDetectionHandlerBundle();
            FaultDetectionHandler<ServiceID> fdh =
                FaultDetectionHandlerFactory.getFaultDetectionHandler(fdhBundle, service.getClass().getClassLoader());
            fdh.register(this);
            fdh.monitor(service, serviceID, lCache);
            fdhTable.put(serviceID, fdh);
        } catch(Exception e) {
            peerLogger.warn("Setting FaultDetectionHandler for a newly discovered ProvisionMonitor", e);
        }
    }

    /**
     * Import all OperationalString instances from a newly discovered
     * ProvisionMonitor that the ProvisionMonitor is managing
     *
     * @param peer The peer ProvisionMonitor service
     */
    void importOperationalStrings(final ProvisionMonitor peer) {
        try {
            DeployAdmin peerDeployAdmin = (DeployAdmin)peer.getAdmin();
            OperationalStringManager[] mgrs = peerDeployAdmin.getOperationalStringManagers();
            if(mgrs == null || mgrs.length == 0) {
                return;
            }
            for (OperationalStringManager mgr : mgrs) {
                if (mgr.isManaging()) {
                    opStringProcessor(mgr.getOperationalString(), peer, peerDeployAdmin, mgr.getRemoteRepositories());
                }
            }
        } catch(Exception e) {
            peerLogger.warn("Importing OperationalStrings", e);
        }
    }

    /**
     * Recursive method to add an OperationalString
     *
     * @param opString The OperationalString to add
     * @param peer A peer ProvisionMonitor service
     * @param peerDeployAdmin The peer ProvisionMonitor DeployAdmin
     * @param repositories A collection of {@code RemoteRepository} instances to use for the resolution of artifacts
     */
    void opStringProcessor(final OperationalString opString,
                           final ProvisionMonitor peer,
                           final DeployAdmin peerDeployAdmin,
                           final RemoteRepository[] repositories) {
        try {
            addPeerOpString(opString, peer, peerDeployAdmin, repositories);
            OperationalString[] nested = opString.getNestedOperationalStrings();
            for (OperationalString aNested : nested)
                opStringProcessor(aNested, peer, peerDeployAdmin, repositories);
        } catch(Exception e) {
            peerLogger.warn("Adding OperationalString [{}]", opString.getName(), e);
        }
    }

    /**
     * Adds an OperationalString
     *
     * @param opString OperationalString to add
     * @param peer A peer ProvisionMonitor service
     * @param peerDeployAdmin The peer ProvisionMonitor DeployAdmin
     * @param repositories A collection of {@code RemoteRepository} instances to use for the resolution of artifacts                        
     */
    synchronized void addPeerOpString(final OperationalString opString,
                                      final ProvisionMonitor peer,
                                      final DeployAdmin peerDeployAdmin,
                                      final RemoteRepository[] repositories) {
        Map<String, Throwable> map = new HashMap<String, Throwable>();
        try {
            boolean resolveConflict = false;
            if(!opStringMangerController.opStringExists(opString.getName())) {
                peerLogger.debug("Adding OpString [{}] from Peer {}", opString.getName(), peer.toString());

                opStringMangerController.getDeploymentVerifier().verifyDeploymentRequest(new DeployRequest(opString,
                                                                                                           repositories));

                OpStringManager opMgr =
                    opStringMangerController.addOperationalString(opString, map, null, peerDeployAdmin, null);
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
                peerLogger.debug("Resolve conflict for OperationalString [{}]", opString.getName());
                OpStringManager localMgr = opStringMangerController.getOpStringManager(opString.getName());
                if(localMgr.isActive()) {
                    resolveConflict(opString, peer, peerDeployAdmin);
                }
            }
        } catch(Throwable t) {
            peerLogger.warn("Adding Peer OperationalStrings", t);
        }
    }

    /**
     * Resolve a conflicting Primary relationship
     *
     * @param opString The OperationalString caught in the middle
     * @param peer The ProvisionMonitor that is also managing
     * @param peerDeployAdmin The peer's DeployAdmin
     */
    void resolveConflict(final OperationalString opString,
                         final ProvisionMonitor peer,
                         final DeployAdmin peerDeployAdmin) {
        boolean addAsBackup = false;
        OpStringManager localMgr = null;
        OperationalStringManager peerMgr;
        try {
            ProvisionMonitor.PeerInfo peerInfo = getPeerInfo(peer);
            peerLogger.debug("ProvisionMonitor at [{}] also has [{}] deployed", peerInfo.getAddress(), opString.getName());

            localMgr = opStringMangerController.getOpStringManager(opString.getName());
            peerMgr = peerDeployAdmin.getOperationalStringManager(opString.getName());
            if(!peerMgr.isManaging()) {
                peerLogger.debug("ProvisionMonitor at [{}] is not managing [{}], no conflict observed",
                                 peerInfo.getAddress(), opString.getName());
            } else {
                /* See who deployed the OperationalString first */
                Date[] peerDates = peerMgr.getDeploymentDates();
                Date[] localDates = localMgr.getDeploymentDates();
                /* If its scheduled, the use PeerInfo */
                if(peerDates.length==0 && localDates.length==0) {
                    /* Use PeerInfo comparable, lower is preferred */
                    peerLogger.debug("No Peer or Local dates");
                    int result = myPeerInfo.compareTo(peerInfo);
                    if(result<0) {
                        peerLogger.debug("Set ProvisionMonitor at [{}] as backup for [{}]",
                                         peerInfo.getAddress(), opString.getName());
                        peerMgr.setManaging(false);
                    } else {
                        peerLogger.debug("Set Local as backup for [{}]", opString.getName());
                        localMgr.setManaging(false);
                        addAsBackup = true;
                    }

                } else {
                    /* If our instance has deployed and the peer has not,
                     * set the peer to be a backup */
                    if(peerDates.length==0 && localDates.length>0) {
                        peerLogger.debug("No Peer deployment dates, set ProvisionMonitor at [{}] as backup for [{}]",
                                         peerInfo.getAddress(), opString.getName());
                        peerMgr.setManaging(false);
                    }

                    /* Conversely, if the peer has deployed and we have not,
                     * set the local instance to be backup */
                    else if(peerDates.length>0 && localDates.length==0) {
                        peerLogger.debug("No Local deployment dates, set Local as backup for [{}]", opString.getName());
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
                            peerLogger.debug("Last deployment dates are equal");
                            int result = myPeerInfo.compareTo(peerInfo);
                            if(result<0) {
                                peerLogger.debug("Local is preferred, set ProvisionMonitor at [{}] as backup for [{}]",
                                                 peerInfo.getAddress(), opString.getName());
                                peerMgr.setManaging(false);
                            } else {
                                peerLogger.debug("Peer is preferred, set Local as backup for [{}]", opString.getName());
                                localMgr.setManaging(false);
                            }
                        } else {
                            if(lastPeerDate.before(lastLocalDate)) {
                                peerLogger.debug("Peer deployed before Local, set Local as backup for [{}]", opString.getName());
                                localMgr.setManaging(false);
                                addAsBackup = true;
                            } else {
                                peerLogger.debug("Local deployed before Peer, set ProvisionMonitor at [{}] as backup for [{}]",
                                                 peerInfo.getAddress(), opString.getName());
                                peerMgr.setManaging(false);
                            }
                        }
                    }
                }
            }

        } catch(OperationalStringException e) {
            /* This should not happen, log the occurrence just the same */
            peerLogger.warn("OperationalStringException trying to resolve OperationalStringManager supremacy", e);
        } catch(RemoteException e) {
            peerLogger.warn("RemoteException trying to resolve OperationalStringManager supremacy", e);
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

