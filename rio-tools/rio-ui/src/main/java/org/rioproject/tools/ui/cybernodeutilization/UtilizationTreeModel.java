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
package org.rioproject.tools.ui.cybernodeutilization;

import org.jdesktop.swingx.JXTreeTable;
import org.jdesktop.swingx.treetable.AbstractMutableTreeTableNode;
import org.jdesktop.swingx.treetable.DefaultTreeTableModel;
import org.jdesktop.swingx.treetable.MutableTreeTableNode;
import org.jdesktop.swingx.treetable.TreeTableNode;
import org.rioproject.cybernode.Cybernode;
import org.rioproject.cybernode.CybernodeAdmin;
import org.rioproject.deploy.ServiceRecord;
import org.rioproject.system.ComputeResourceUtilization;

import javax.swing.tree.TreePath;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * @author Dennis Reedy
 */
public class UtilizationTreeModel extends DefaultTreeTableModel {
    private boolean expandAll;
    private final JXTreeTable treeTable;
    private final Executor updateHandler = Executors.newSingleThreadExecutor();

    public UtilizationTreeModel(final TreeTableNode root,
                                final java.util.List<String> columns,
                                final JXTreeTable treeTable) {
        super(root, columns);
        this.treeTable = treeTable;
    }

    public void setExpandAll(final boolean expandAll) {
        this.expandAll = expandAll;
    }

    public int getCybernodesInUse() {
        int inUse = 0;
        for (int i = 0; i < getRoot().getChildCount(); i++) {
            CybernodeNode node = (CybernodeNode) getRoot().getChildAt(i);
            try {
                Integer num = ((CybernodeAdmin) node.getCybernode().getAdmin()).getServiceCount();
                if (num > 0)
                    inUse++;
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }
        return (inUse);
    }

    public synchronized void addCybernodeNode(final CybernodeNode item) {
        insertNodeInto(item, (MutableTreeTableNode) getRoot(), getRoot().getChildCount());
        modelSupport.firePathChanged(nodeToTreePath(getRoot()));
        setServices(item);
    }

    public void removeCybernode(final Cybernode item) {
        for (int i = 0; i < getRoot().getChildCount(); i++) {
            CybernodeNode node = (CybernodeNode) getRoot().getChildAt(i);
            if (item.equals(node.getCybernode())) {
                removeNodeFromParent(node);
                break;
            }
        }
        modelSupport.firePathChanged(nodeToTreePath(getRoot()));
    }

    public ServiceNode getServiceNode(final int row) {
        AbstractMutableTreeTableNode t = getNode(row);
        return (t instanceof ServiceNode ? (ServiceNode) t : null);
    }

    public CybernodeNode getCybernodeNode(final int row) {
        AbstractMutableTreeTableNode t = getNode(row);
        return (t instanceof CybernodeNode ? (CybernodeNode) t : null);
    }

    public AbstractMutableTreeTableNode getNode(final int row) {
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

    public int getCybernodeCount() {
        return (getRoot().getChildCount());
    }

    public void updateCybernode(final CybernodeNode node) {
        int row = getCybernodeRow(node.getCybernode());
        if (row != -1) {
            setValueAt(node, row);
        } else {
            System.err.println("Could not update Cybernode at ["+node+"], unable to find table row");
        }
    }

    public void updateCybernodesAt(final String hostAddress) {
        updateHandler.execute(new Runnable() {
            List<CybernodeNode> nodes = new ArrayList<CybernodeNode>();
            public void run() {
                for(int i=0; i<getRoot().getChildCount(); i++) {
                    nodes.add((CybernodeNode)getChild(getRoot(), i));
                }
                for(CybernodeNode node : nodes) {
                    if(node.getHostName().equals(hostAddress)) {
                        setServices(node);
                    }
                }
            }
        });
    }

    public void setValueAt(final Object item, final int row) {
        CybernodeNode node = getCybernodeNode(row);
        if (node != null) {
            node.setComputeResourceUtilization(((CybernodeNode) item).getComputeResourceUtilization());
            setServices(node);
            modelSupport.firePathChanged(nodeToTreePath(node));
        }
    }

    private void setServices(final CybernodeNode node) {
        java.util.List<ServiceRecord> serviceList = new ArrayList<ServiceRecord>();
        try {
            ServiceRecord[] records = node.getCybernode().getServiceRecords(ServiceRecord.ACTIVE_SERVICE_RECORD);
            serviceList.addAll(Arrays.asList(records));
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        List<ServiceNode> removals = new ArrayList<ServiceNode>();
        ServiceRecord[] cybernodeServices =
            serviceList.toArray(new ServiceRecord[serviceList.size()]);

        for (int i = 0; i < node.getChildCount(); i++) {
            boolean found = false;
            ServiceNode sNode = (ServiceNode) node.getChildAt(i);
            for (ServiceRecord record : cybernodeServices) {
                if (sNode.getName().equals(record.getServiceElement().getName())) {
                    if (serviceList.remove(record)) {
                        found = true;
                        setServiceNodeUtilization(sNode, node.getAdmin());
                        break;
                    }
                }
            }
            if (!found)
                removals.add(sNode);
        }

        /* Remove services */
        for (ServiceNode sNode : removals) {
            removeNodeFromParent(sNode);
        }

        /* Add new services */
        for (ServiceRecord record : serviceList) {
            ServiceNode sNode = new ServiceNode(record, node.getColumnHelper());
            setServiceNodeUtilization(sNode, node.getAdmin());
            insertNodeInto(sNode, node, node.getChildCount());
        }
        try {
            modelSupport.fireTreeStructureChanged(nodeToTreePath(node));
        } catch(Exception e) {
            e.printStackTrace();
        }
        if (expandAll) {
            treeTable.expandRow(getCybernodeRow(node.getCybernode()));
        }
    }

    private int getCybernodeRow(final Cybernode item) {
        int rowCounter = 0;
        for (int i = 0; i < getRoot().getChildCount(); i++) {
            CybernodeNode node = (CybernodeNode) getRoot().getChildAt(i);
            if (item.equals(node.getCybernode())) {
                return (rowCounter);
            }
            rowCounter++;
            for (int j = 0; j < node.getChildCount(); j++) {
                TreeTableNode t = node.getChildAt(j);
                if (treeTable.isVisible(nodeToTreePath(t))) {
                    rowCounter++;
                }
            }
        }
        return (-1);
    }

    /*
     * set the compute resource utilization for a ServiceNode
     */
    private void setServiceNodeUtilization(final ServiceNode sNode, final CybernodeAdmin cAdmin) {
        if (sNode.isForked()) {
            try {
                ComputeResourceUtilization util = cAdmin.getComputeResourceUtilization(sNode.getUuid());
                sNode.setComputeResourceUtilization(util);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    private TreePath nodeToTreePath(final TreeTableNode n) {
        TreePath ret = null;
        ArrayList<TreeTableNode> nodes = new ArrayList<TreeTableNode>();
        if (n != null) {
            nodes.add(n);
            TreeTableNode p = n.getParent();
            while (p != null) {
                nodes.add(p);
                p = p.getParent();
            }
            Collections.reverse(nodes);
            ret = new TreePath(nodes.toArray());
        }
        return ret;
    }
}
