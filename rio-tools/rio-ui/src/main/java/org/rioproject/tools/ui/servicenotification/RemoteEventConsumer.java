package org.rioproject.tools.ui.servicenotification;

import net.jini.core.event.RemoteEvent;
import net.jini.core.event.RemoteEventListener;
import org.rioproject.event.RemoteServiceEvent;
import org.rioproject.event.RemoteServiceEventListener;
import org.rioproject.log.ServiceLogEvent;
import org.rioproject.monitor.ProvisionFailureEvent;
import org.rioproject.monitor.ProvisionMonitorEvent;
import org.rioproject.ui.Util;

import java.util.ArrayList;

/**
 * @author Dennis Reedy
 */
public class RemoteEventConsumer implements RemoteEventListener, RemoteServiceEventListener {
    private final RemoteEventTable remoteEventTable;

    public RemoteEventConsumer(RemoteEventTable remoteEventTable) {
        this.remoteEventTable = remoteEventTable;
    }

    public void notify(RemoteEvent event) {
        try {
            if(event instanceof ProvisionFailureEvent || event instanceof ServiceLogEvent) {
                remoteEventTable.getDataModel().addItem((RemoteServiceEvent) event);
                remoteEventTable.notifyListeners();
            } else if(event instanceof ProvisionMonitorEvent) {
                ProvisionMonitorEvent pme = (ProvisionMonitorEvent)event;
                synchronized(this) {
                    if(pme.getAction().equals(ProvisionMonitorEvent.Action.SERVICE_PROVISIONED)) {
                        if(remoteEventTable.getAutoRemove()) {
                            for(RemoteServiceEventNode rsn : remoteEventTable.getDataModel().getRemoteServiceEventNodes(pme.getServiceElement())) {
                                if(rsn instanceof ProvisionFailureEventNode) {
                                    ProvisionFailureEventNode en = (ProvisionFailureEventNode)rsn;
                                    if(en.getStatus().equals("Resolved"))
                                        continue;
                                    if(rsn.getServiceName().equals(pme.getServiceElement().getName())) {
                                        remoteEventTable.getDataModel().removeItem(rsn);
                                        remoteEventTable.notifyListeners();
                                        break;
                                    }
                                }
                            }
                        } else {
                            remoteEventTable.getDataModel().addItem(pme);
                            remoteEventTable.notifyListeners();
                        }
                    } else if(pme.getAction().equals(ProvisionMonitorEvent.Action.OPSTRING_UNDEPLOYED)) {
                        DeploymentNode dn = remoteEventTable.getDataModel().getDeploymentNode(pme.getOperationalStringName());
                        remoteEventTable.getDataModel().addItem(pme);
                        if(remoteEventTable.getAutoRemove()) {
                            if(dn==null)
                                return;
                            java.util.List<RemoteServiceEventNode> removals = new ArrayList<RemoteServiceEventNode>();
                            for (int i = 0; i < dn.getChildCount(); i++) {
                                RemoteServiceEventNode child = (RemoteServiceEventNode)dn.getChildAt(i);
                                if(child instanceof ProvisionFailureEventNode) {
                                    removals.add(child);
                                }
                            }
                            for(RemoteServiceEventNode rsn : removals)
                                remoteEventTable.getDataModel().removeItem(rsn);
                        }
                        remoteEventTable.notifyListeners();
                    } else {
                        remoteEventTable.getDataModel().addItem(pme);
                        remoteEventTable.notifyListeners();
                    }
                }
            }
            remoteEventTable.expandAll();
            //setStatusErrorText("ProvisionFailureEvent received for "
            //        + pfe.getServiceElement().getName());
        } catch (Throwable t) {
            Util.showError(t, remoteEventTable, "Notification of a ProvisionFailureEvent");
        }
    }

    @Override
    public void notify(RemoteServiceEvent event) {
        notify((RemoteEvent)event);
    }
}
