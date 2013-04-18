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
import org.rioproject.sla.SLAThresholdEvent;
import org.rioproject.tools.ui.servicenotification.filter.FilterCriteria;
import org.rioproject.tools.ui.servicenotification.filter.FilterHandler;

import javax.swing.tree.TreePath;
import java.util.*;

/**
 * The RemoteEventTreeModel extends DefaultTreeTableModel providing the model to
 * display {@link ProvisionFailureEvent}s, {@link ServiceLogEvent}s, {@code SLAThresholdEvent}s
 * and {@code ProvisionMonitorEvent}s in a tree table.
 */
public class RemoteEventTreeModel extends DefaultTreeTableModel {
    private JXTreeTable treeTable;
    private FilterCriteria filterCriteria;
    private final RemoteServiceEventNodeComparator comparator = new RemoteServiceEventNodeComparator();
    private final FilterHandler filterControl = new FilterHandler();
    private final List<DeploymentNode> completeModel = new ArrayList<DeploymentNode>();
    private final List<DeploymentNode> filteredModel = new ArrayList<DeploymentNode>();
    private final String COMPLETE = "complete";
    private final String FILTER = "filtered";

    public RemoteEventTreeModel(RootNode root, java.util.List<String> columns) {
        super(root, columns);
    }

    public void setTreeTable(JXTreeTable treeTable) {
        this.treeTable = treeTable;
    }

    public synchronized void addItem(RemoteServiceEvent event) {
        RemoteServiceEventNode remoteServiceEventNode = createRemoteServiceEventNode(event);
        Map<String, DeploymentNode> nodes = getDeploymentNodes(remoteServiceEventNode.getOperationalStringName());
        addItem(remoteServiceEventNode, nodes.get(COMPLETE), nodes.get(FILTER));
    }

    private RemoteServiceEventNode createRemoteServiceEventNode(RemoteServiceEvent event) {
        RemoteServiceEventNode remoteServiceEventNode = null;
        if(event instanceof ProvisionFailureEvent) {
            ProvisionFailureEvent pfe = (ProvisionFailureEvent)event;
            remoteServiceEventNode = new ProvisionFailureEventNode(pfe);
        }
        if(event instanceof ProvisionMonitorEvent) {
            ProvisionMonitorEvent pme = (ProvisionMonitorEvent)event;
            remoteServiceEventNode = new ProvisionMonitorEventNode(pme);
        }
        if(event instanceof ServiceLogEvent) {
            ServiceLogEvent sle = (ServiceLogEvent)event;
            remoteServiceEventNode = new ServiceLogEventNode(sle);
        }
        if(event instanceof SLAThresholdEvent) {
            SLAThresholdEvent sla = (SLAThresholdEvent)event;
            remoteServiceEventNode = new SLAThresholdEventNode(sla);
        }
        return remoteServiceEventNode;
    }

    private boolean hasRemoteServiceEventNode(RemoteServiceEventNode node, AbstractMutableTreeTableNode parent) {
        boolean has = false;
        for(int i=0; i< parent.getChildCount(); i++) {
            RemoteServiceEventNode childNode =(RemoteServiceEventNode)parent.getChildAt(i);
            if(comparator.compare(childNode, node)==0&&
               (node.getEvent().getID()==childNode.getEvent().getID() &&
                   node.getEvent().getSequenceNumber()==childNode.getEvent().getSequenceNumber())) {
                has = true;
                break;
            }
        }
        return has;
    }

    private void addItem(AbstractMutableTreeTableNode node,
                         AbstractMutableTreeTableNode parent,
                         AbstractMutableTreeTableNode filterParent) {
        if(parent==null)
            return;
        synchronized (completeModel) {
            int index = parent.getChildCount();
            if(node instanceof RemoteServiceEventNode) {
                RemoteServiceEventNode rNode = (RemoteServiceEventNode)node;
                if(hasRemoteServiceEventNode(rNode, parent))
                    return;
                if(filterParent!=null && hasRemoteServiceEventNode(rNode, filterParent))
                    return;
                /* If we have a filter and the node passes filter step, insert into filterModel */
                if(filterCriteria!=null && filterParent!=null && filterControl.include(filterCriteria, rNode)) {
                    index = getItemIndex(rNode, filterParent);
                    if(index!=-1) {
                        filterParent.insert(rNode, index);
                        //this.modelSupport.fireChildAdded(new TreePath(getPathToRoot(filterParent)), index, rNode);
                        this.modelSupport.fireTreeStructureChanged(new TreePath(getPathToRoot(filterParent)));
                    }
                }
                /* Always insert into the complete model */
                index = getItemIndex(rNode, parent);
                if(index!=-1) {
                    parent.insert(node, index);
                    if(filterCriteria==null) {
                        this.modelSupport.fireChildAdded(new TreePath(getPathToRoot(parent)), index, rNode);
                    }
                }
            }  else {
                parent.insert(node, index);
                this.modelSupport.fireChildAdded(new TreePath(getPathToRoot(parent)), index, node);
            }
        }
    }

    private int getItemIndex(RemoteServiceEventNode rNode, AbstractMutableTreeTableNode parent) {
        int index = parent.getChildCount();
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
                    return -1;
                }
            }
        }
        return index;
    }

    public void reset() {
        this.modelSupport.fireNewRoot();
    }

    public void removeItem(int row) {
        AbstractMutableTreeTableNode node = getNode(row);
        if(node!=null) {
            TreePath treePath = null;
            if(node instanceof DeploymentNode) {

            } else {
                DeploymentNode dNode = (DeploymentNode) node.getParent();
                if(dNode!=null)
                    treePath = treeTable.getPathForRow(getDeploymentNodeRow(dNode));
            }
            removeItem(node);
            if(treePath!=null) {
                this.modelSupport.fireTreeStructureChanged(treePath);
                //this.modelSupport.fireChildrenRemoved(treePath, new int[]{row}, new Object[]{node});
            }
        }
    }

    public void removeItem(AbstractMutableTreeTableNode node) {
        synchronized (completeModel) {
            if(node!=null) {
                AbstractMutableTreeTableNode parent = (AbstractMutableTreeTableNode)node.getParent();
                removeNodeFromParent(node);
                this.modelSupport.fireTreeStructureChanged(new TreePath(getPathToRoot(parent)));
                DeploymentNode deploymentNode = null;
                if(node instanceof DeploymentNode) {
                    deploymentNode = (DeploymentNode)node;
                }
                if(parent.getChildCount()==0 && parent.getParent()!=null) {
                    removeNodeFromParent(parent);
                    this.modelSupport.fireNewRoot();
                    deploymentNode = (DeploymentNode)parent;
                }
                if(deploymentNode!=null) {
                    if(filterCriteria!=null) {
                        filteredModel.remove(deploymentNode);
                        DeploymentNode dNode = getDeploymentNode(deploymentNode.getName(), completeModel);
                        if(dNode!=null) {
                            completeModel.remove(dNode);
                        }
                    } else {
                        completeModel.remove(deploymentNode);
                    }
                }
            }
        }
    }

    @Override
    public TreeTableNode[] getPathToRoot(TreeTableNode aNode) {
        List<TreeTableNode> path = new ArrayList<TreeTableNode>();
        TreeTableNode node = aNode;

        while (node != root && node!=null) {
            path.add(0, node);
            node = node.getParent();
        }

        if (node == root) {
            path.add(0, node);
        }

        return path.toArray(new TreeTableNode[path.size()]);
    }

    public RemoteServiceEventNode getRemoteServiceEventNode(int row) {
        AbstractMutableTreeTableNode node = getNode(row);
        RemoteServiceEventNode eventNode = null;
        if(node instanceof RemoteServiceEventNode) {
            eventNode = (RemoteServiceEventNode)node;
        }
        return eventNode;
    }

    private Map<String, DeploymentNode> getDeploymentNodes(String name) {
        Map<String, DeploymentNode> nodeMap = new HashMap<String, DeploymentNode>();
        if(name==null) {
            return nodeMap;
        }
        DeploymentNode node = getDeploymentNode(name, completeModel);
        if(node==null) {
            node = new DeploymentNode(name);
            completeModel.add(node);
            if(filterCriteria==null) {
                addItem(node, (AbstractMutableTreeTableNode)getRoot(), null);
            }
        }
        nodeMap.put(COMPLETE, node);
        if(filterCriteria!=null) {
            DeploymentNode filteredNode = getDeploymentNode(name, filteredModel);
            if(filteredNode==null) {
                filteredNode = new DeploymentNode(name);
                filteredModel.add(filteredNode);
                addItem(filteredNode, (AbstractMutableTreeTableNode)getRoot(), null);
            }
            nodeMap.put(FILTER, filteredNode);
        } else {
            nodeMap.put(FILTER, null);
        }

        return nodeMap;
    }

    public DeploymentNode getDeploymentNode(String name, List<DeploymentNode> nodes) {
        if(name==null)
            return null;
        DeploymentNode dNode = null;
        for(DeploymentNode node : nodes) {
            if(node.getName().equals(name)) {
                dNode = node;
                break;
            }
        }
        return dNode;
    }

    Collection<DeploymentNode> getDeploymentNodes() {
        Collection<DeploymentNode> dNodes = new ArrayList<DeploymentNode>();
        if(filterCriteria!=null) {
            dNodes.addAll(filteredModel);
        } else {
            dNodes.addAll(completeModel);
        }
        return dNodes;
    }

    public void setFilterCriteria(FilterCriteria filterCriteria) {
        if(this.filterCriteria!=null && this.filterCriteria.equals(filterCriteria)) {
            return;
        }
        this.filterCriteria = filterCriteria;
        if(this.filterCriteria!=null) {
            clearTree(filteredModel.isEmpty()?completeModel:filteredModel);
            filteredModel.clear();
            for(DeploymentNode node : completeModel) {
                DeploymentNode newNode = new DeploymentNode(node.getName());
                filteredModel.add(newNode);
            }
            for(DeploymentNode node : filteredModel) {
                ((AbstractMutableTreeTableNode)getRoot()).insert(node, filteredModel.indexOf(node));
            }
            for(DeploymentNode node : completeModel) {
                int index = 0;
                for(int i=0; i < node.getChildCount(); i++) {
                    RemoteServiceEventNode rNode = (RemoteServiceEventNode)node.getChildAt(i);
                    RemoteServiceEventNode newNode = createRemoteServiceEventNode(rNode.getEvent());
                    if(filterControl.include(this.filterCriteria, newNode)) {
                        getDeploymentNode(node.getName(), filteredModel).insert(newNode, index++);
                    }
                }
            }
        } else {
            clearTree(filteredModel);
            filteredModel.clear();
            for(DeploymentNode node : completeModel) {
                ((AbstractMutableTreeTableNode)getRoot()).insert(node, completeModel.indexOf(node));
            }
        }
        this.modelSupport.fireNewRoot();
    }

    private void clearTree(Collection<DeploymentNode> nodes) {
        for(DeploymentNode node : nodes) {
            node.removeFromParent();
        }
    }

    public FilterCriteria getFilterCriteria() {
        return filterCriteria;
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
            if(treeTable.isExpanded(rowCounter)) {
                for (int j = 0; j < tn.getChildCount(); j++) {
                    rowCounter++;
                    if (rowCounter == row) {
                        node = (AbstractMutableTreeTableNode) tn.getChildAt(j);
                        break;
                    }
                }
            }
            if (node != null)
                break;
            rowCounter++;
        }
        return node;
    }

    int getDeploymentNodeRow(DeploymentNode deploymentNode) {
        int row = -1;
        int rowCounter = 0;
        for (int i = 0; i < getRoot().getChildCount(); i++) {
            AbstractMutableTreeTableNode tn = (AbstractMutableTreeTableNode) getRoot().getChildAt(i);
            if(tn instanceof DeploymentNode && deploymentNode.equals(tn)) {
                row = rowCounter;
                break;
            }
            if (treeTable.isExpanded(rowCounter)) {
                for (int j = 0; j < tn.getChildCount(); j++) {
                    rowCounter++;
                }
            }
            rowCounter++;
        }
        return row;
    }

    void updated(TreePath treePath) {
        this.modelSupport.firePathChanged(treePath);
    }

    @Override
    public Object getValueAt(Object node, int column) {
        /*if (!isValidTreeTableNode(node)) {
            throw new IllegalArgumentException("node must be a valid node managed by this model");
        }*/

        if (column < 0 || column >= getColumnCount()) {
            throw new IllegalArgumentException("column must be a valid index");
        }

        TreeTableNode ttn = (TreeTableNode) node;

        if (column >= ttn.getColumnCount()) {
            return null;
        }

        return ttn.getValueAt(column);
        //return super.getValueAt(node, column);
    }

    @Override
    public boolean isLeaf(Object node) {
        return ((TreeTableNode) node).isLeaf();
    }

    public int getRowCount() {
        int rowCounter = 0;
        for (int i = 0; i < getRoot().getChildCount(); i++) {
            DeploymentNode dn = (DeploymentNode)getRoot().getChildAt(i);
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
