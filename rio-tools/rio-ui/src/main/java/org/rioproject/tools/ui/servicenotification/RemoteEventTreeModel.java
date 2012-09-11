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
package org.rioproject.tools.ui.servicenotification;

import org.jdesktop.swingx.JXTreeTable;
import org.jdesktop.swingx.treetable.AbstractMutableTreeTableNode;
import org.jdesktop.swingx.treetable.DefaultTreeTableModel;
import org.jdesktop.swingx.treetable.TreeTableNode;
import org.rioproject.event.RemoteServiceEvent;
import org.rioproject.log.ServiceLogEvent;
import org.rioproject.monitor.ProvisionFailureEvent;
import org.rioproject.monitor.ProvisionMonitorEvent;
import org.rioproject.opstring.ServiceElement;
import org.rioproject.sla.SLAThresholdEvent;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * The RemoteEventTreeModel extends DefaultTreeTableModel providing the model to
 * display {@link ProvisionFailureEvent}s, {@link ServiceLogEvent}s, {@code SLAThresholdEvent}s
 * and {@code ProvisionMonitorEvent}s in a tree table.
 */
public class RemoteEventTreeModel extends DefaultTreeTableModel {
    private JXTreeTable treeTable;
    private final RemoteServiceEventNodeComparator comparator = new RemoteServiceEventNodeComparator();

    public RemoteEventTreeModel(TreeTableNode root, java.util.List<String> columns) {
        super(root, columns);
    }

    public void setTreeTable(JXTreeTable treeTable) {
        this.treeTable = treeTable;
    }

    public synchronized void addItem(RemoteServiceEvent event) {
        if(event instanceof ProvisionFailureEvent) {
            ProvisionFailureEvent pfe = (ProvisionFailureEvent)event;
            String name = pfe.getServiceElement().getOperationalStringName();
            AbstractMutableTreeTableNode node = getDeploymentNode(name);
            if(node==null) {
                node = new DeploymentNode(name);
                addItem(node, (AbstractMutableTreeTableNode) getRoot());
            } 
            addItem(new ProvisionFailureEventNode(pfe), node);
        }
        if(event instanceof ProvisionMonitorEvent) {
            ProvisionMonitorEvent pme = (ProvisionMonitorEvent)event;
            String name;
            if(pme.getServiceElement()!=null)
                name = pme.getServiceElement().getOperationalStringName();
            else
                name = pme.getOperationalStringName();
            AbstractMutableTreeTableNode node = getDeploymentNode(name);
            if(node==null) {
                node = new DeploymentNode(name);
                addItem(node, (AbstractMutableTreeTableNode) getRoot());
            }
            addItem(new ProvisionMonitorEventNode(pme), node);
        }
        if(event instanceof ServiceLogEvent) {
            ServiceLogEvent sle = (ServiceLogEvent)event;
            String name = sle.getOpStringName();
            if(name==null)
                name = "Unknown";
            DeploymentNode node = getDeploymentNode(name);
            if(node==null) {
                node = new DeploymentNode(name);
                addItem(node, (AbstractMutableTreeTableNode) getRoot());
            }
            addItem(new ServiceLogEventNode(sle), node);
        }
        if(event instanceof SLAThresholdEvent) {
            SLAThresholdEvent sla = (SLAThresholdEvent)event;
            String name = sla.getServiceElement().getOperationalStringName();
            if(name==null)
                name = "Unknown";
            DeploymentNode node = getDeploymentNode(name);
            if(node==null) {
                node = new DeploymentNode(name);
                addItem(node, (AbstractMutableTreeTableNode) getRoot());
            }
            addItem(new SLAThresholdEventNode(sla), node);
        }
    }

    private void addItem(AbstractMutableTreeTableNode node, AbstractMutableTreeTableNode parent) {
        int index = parent.getChildCount();
        if(node instanceof RemoteServiceEventNode) {
            RemoteServiceEventNode rNode = (RemoteServiceEventNode)node;
            for (int i = 0; i < parent.getChildCount(); i++) {
                RemoteServiceEventNode childNode =(RemoteServiceEventNode)parent.getChildAt(i);
                int result = comparator.compare(childNode, rNode);
                if(result>0) {
                    index = i;
                    break;

                /* Check for duplicate events */
                } else if(result==0) {
                    if(rNode.getEvent().getID()==childNode.getEvent().getID() &&
                       rNode.getEvent().getSequenceNumber()==childNode.getEvent().getSequenceNumber()) {
                        return;
                    }
                }
            }
        }
        insertNodeInto(node, parent, index);
    }

    public void removeItem(int row) {
        removeItem(getNode(row));
    }

    public void removeItem(AbstractMutableTreeTableNode node) {
        if(node!=null) {
            AbstractMutableTreeTableNode parent = (AbstractMutableTreeTableNode)node.getParent();
            removeNodeFromParent(node);
            if(parent.getChildCount()==0 && parent.getParent()!=null)
                removeNodeFromParent(parent);
        }
    }

    public RemoteServiceEventNode getRemoteServiceEventNode(int row) {
        AbstractMutableTreeTableNode node = getNode(row);
        RemoteServiceEventNode eventNode = null;
        if(node instanceof RemoteServiceEventNode) {
            eventNode = (RemoteServiceEventNode)node;
        }
        return eventNode;
    }

    public List<RemoteServiceEventNode> getRemoteServiceEventNodes(ServiceElement elem) {
        List<RemoteServiceEventNode> eNodes = new ArrayList<RemoteServiceEventNode>();
        for (int i = 0; i < getRoot().getChildCount(); i++) {
            AbstractMutableTreeTableNode dn =(AbstractMutableTreeTableNode) getRoot().getChildAt(i);
            if(dn==null)
                continue;
            if(dn instanceof DeploymentNode) {
                DeploymentNode dNode = (DeploymentNode)dn;
                if(dNode.getName().equals(elem.getOperationalStringName())) {
                    for (int j = 0; j < dn.getChildCount(); j++) {
                        RemoteServiceEventNode en = (RemoteServiceEventNode)dn.getChildAt(j);
                        if(en.getServiceName()!=null && en.getServiceName().equals(elem.getName())) {
                            eNodes.add(en);
                        }
                    }
                }
            }
        }
        return eNodes;
    }

    public DeploymentNode getDeploymentNode(String name) {
        DeploymentNode dNode = null;
        for (int i = 0; i < getRoot().getChildCount(); i++) {
            AbstractMutableTreeTableNode node = (AbstractMutableTreeTableNode) getRoot().getChildAt(i);
            if(node instanceof DeploymentNode) {
                DeploymentNode dn = (DeploymentNode)node;
                if(dn.getName().equals(name)) {
                    dNode = dn;
                    break;
                }
            }
        }
        return dNode;
    }

    public AbstractMutableTreeTableNode getNode(int row) {
        int rowCounter = 0;
        AbstractMutableTreeTableNode node = null;

        for (int i = 0; i < getRoot().getChildCount(); i++) {
            AbstractMutableTreeTableNode tn = (AbstractMutableTreeTableNode) getRoot().getChildAt(i);
            if (rowCounter == row) {
                node = tn;
                break;
            }
            rowCounter++;
            for (int j = 0; j < tn.getChildCount(); j++) {
                AbstractMutableTreeTableNode t = (AbstractMutableTreeTableNode) tn.getChildAt(j);
                if (treeTable.isVisible(treeTable.getPathForRow(rowCounter))) {
                    if (rowCounter == row) {
                        node = t;
                        break;
                    }
                    rowCounter++;
                }
            }

            if (node != null)
                break;
        }
        return node;
    }

    public int getRowCount() {
        int rowCounter = 0;
        for (int i = 0; i < getRoot().getChildCount(); i++) {
            DeploymentNode dn =(DeploymentNode) getRoot().getChildAt(i);
            rowCounter += dn.getChildCount();
        }
        return rowCounter;
    }

    class RemoteServiceEventNodeComparator implements Comparator<RemoteServiceEventNode> {
        public int compare(RemoteServiceEventNode node1, RemoteServiceEventNode node2) {
            return node1.getDate().compareTo(node2.getDate());
        }
    }

}
