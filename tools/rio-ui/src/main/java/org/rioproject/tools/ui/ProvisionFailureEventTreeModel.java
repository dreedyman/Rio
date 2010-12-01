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
package org.rioproject.tools.ui;

import org.rioproject.core.ServiceElement;
import org.rioproject.event.RemoteServiceEvent;
import org.rioproject.log.ServiceLogEvent;
import org.rioproject.monitor.ProvisionFailureEvent;
import org.rioproject.tools.ui.treetable.*;

import javax.swing.table.TableColumn;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.LogRecord;

/**
 * The ProvisionFailureEventTreeModel extends AbstractTableModel providing the model to
 * display ProvisionFailureEvents in a tree table.
 */
public class ProvisionFailureEventTreeModel extends AbstractTreeTableModel {
    final static String FIXED_COLUMN = "Deployment";
    private JTreeTable treeTable;
    DateFormat dateFormat = Constants.DATE_FORMAT;

    public ProvisionFailureEventTreeModel(DefaultMutableTreeNode root) {
        super(root);
    }

    public void setTreeTable(JTreeTable treeTable) {
        this.treeTable = treeTable;
    }

    public boolean isCellEditable(Object node, int column) {
        return column == 0;
    }

    public Class getColumnClass(int column) {
        return (column == 0 ? TreeTableModel.class : String.class);
    }

    public DateFormat getDateFormat() {
        return dateFormat;
    }

    public Object getValueAt(Object node, int columnIndex) {
        String value = "";
        if (node instanceof RemoteServiceEventNode) {
            RemoteServiceEventNode rsn = (RemoteServiceEventNode) node;
            value = getColumnValue(columnIndex, rsn);
        }

        if (node instanceof DeploymentNode) {
            DeploymentNode dn = (DeploymentNode) node;
            if (columnIndex == 0) {
                value = dn.getName();
            } else {
                value = getColumnValue(columnIndex, null);
            }
        }
        return value;
    }

    private String getColumnValue(int columnIndex, RemoteServiceEventNode en) {
        String cName = getColumnName(columnIndex);
        if (cName == null) {
            System.out.println(
                ">> FAILED!!!, getColumnValue for index (" + columnIndex + ")");
            return null;
        }

        if(en==null)
            return "";

        String value = null;
        //if(cName.equals("Status"))
        //    value = en.getStatus();
        //else
        if (cName.equals("Description")) {
            StringBuffer sb = new StringBuffer();
            sb.append(en.getDescription());
            sb.append(". ");
            Throwable t = en.getThrown();
            if(t!=null) {
                if(t.getCause()!=null) {
                    t = t.getCause();
                }
                sb.append(t+" Exception raised.");
            }
            value = sb.toString();
        } else if (cName.equals("When")) {
            value = dateFormat.format(en.getDate());
        }
        return (value);
    }

    public synchronized void addItem(RemoteServiceEvent event) {
        if(event instanceof ProvisionFailureEvent) {
            ProvisionFailureEvent pfe = (ProvisionFailureEvent)event;
            String name = pfe.getServiceElement().getOperationalStringName();
            DeploymentNode node = getDeploymentNode(name);
            if(node==null) {
                node = new DeploymentNode(name);
                addItem(node, root);
            } 
            addItem(new EventNode(pfe), node);
        }
        if(event instanceof ServiceLogEvent) {
            ServiceLogEvent sle = (ServiceLogEvent)event;
            String name = sle.getOpStringName();
            if(name==null)
                name = "Unknown";
            DeploymentNode node = getDeploymentNode(name);
            if(node==null) {
                node = new DeploymentNode(name);
                addItem(node, root);
            }
            addItem(new ServiceLogEventNode(sle), node);
        }
    }

    private void addItem(DefaultMutableTreeNode node,
                         DefaultMutableTreeNode parent) {
        insertNodeInto(node, parent, parent.getChildCount());
        treeTable.getTree().makeVisible(new TreePath(node.getPath()));
    }

    public void removeItem(int row) {
        removeItem(getNode(row));
    }

    public void removeItem(DefaultMutableTreeNode node) {
        if(node!=null) {
            DefaultMutableTreeNode parent = (DefaultMutableTreeNode)node.getParent();
            Map<DefaultMutableTreeNode, Boolean> expandMap = new HashMap<DefaultMutableTreeNode, Boolean>();
            for (int i = 0; i < root.getChildCount(); i++) {
                DefaultMutableTreeNode tn = (DefaultMutableTreeNode)root.getChildAt(i);
                expandMap.put(tn,
                              treeTable.getTree().isExpanded(new TreePath(tn.getPath())));
            }
            removeNodeFromParent(node);
            nodeStructureChanged(parent);
            if(parent.getChildCount()==0 && parent.getParent()!=null)
                removeNodeFromParent(parent);
            expandMap.remove(node);
            for(Map.Entry<DefaultMutableTreeNode, Boolean> entry : expandMap.entrySet()) {
                if(entry.getValue()) 
                    treeTable.getTree().expandPath(new TreePath(entry.getKey().getPath()));
            }
        }
    }

    public RemoteServiceEvent getItem(int row) {
        DefaultMutableTreeNode node = getNode(row);
        RemoteServiceEvent event = null;
        if(node instanceof RemoteServiceEventNode) {
            event = ((RemoteServiceEventNode)node).getEvent();
        }
        return event;
    }

    public List<RemoteServiceEventNode> getRemoteServiceEventNode(ServiceElement elem) {
        List<RemoteServiceEventNode> eNodes = new ArrayList<RemoteServiceEventNode>();
        for (int i = 0; i < root.getChildCount(); i++) {
            DeploymentNode dn =(DeploymentNode) root.getChildAt(i);
            if(dn==null)
                continue;
            if(dn.getName().equals(elem.getOperationalStringName())) {
                for (int j = 0; j < dn.getChildCount(); j++) {
                    RemoteServiceEventNode en = (RemoteServiceEventNode)dn.getChildAt(j);
                    if(en.getServiceName().equals(elem.getName())) {
                        eNodes.add(en);
                    }
                }
            }
        }
        return eNodes;
    }

    public int getRowCount() {
        int rowCounter = 0;
        for (int i = 0; i < root.getChildCount(); i++) {
            DeploymentNode dn =(DeploymentNode) root.getChildAt(i);
            rowCounter += dn.getChildCount();
        }
        return rowCounter;
    }

    public DeploymentNode getDeploymentNode(String name) {
        DeploymentNode dNode = null;
        for (int i = 0; i < root.getChildCount(); i++) {
            DeploymentNode node = (DeploymentNode) root.getChildAt(i);
            if(node.getName().equals(name)) {
                dNode = node;
                break;
            }
        }
        return dNode;
    }

    public DeploymentNode getDeploymentNode(int row) {
        DefaultMutableTreeNode t = getNode(row);
        return (t instanceof DeploymentNode ? (DeploymentNode) t : null);
    }

    public DefaultMutableTreeNode getNode(int row) {
        int rowCounter = 0;
        DefaultMutableTreeNode node = null;

        for (int i = 0; i < root.getChildCount(); i++) {
            DefaultMutableTreeNode tn =
                (DefaultMutableTreeNode) root.getChildAt(i);
            if (rowCounter == row) {
                node = tn;
                break;
            }
            rowCounter++;
            for (int j = 0; j < tn.getChildCount(); j++) {
                DefaultMutableTreeNode t =
                    (DefaultMutableTreeNode) tn.getChildAt(j);
                if (treeTable.getTree().isVisible(new TreePath(t.getPath()))) {
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

    public int getColumnCount() {
        return (treeTable == null ? 1 :
                treeTable.getColumnModel().getColumnCount());
    }

    public String getColumnName(int index) {
        if (index == 0)
            return FIXED_COLUMN;
        String cName = null;
        if (index < treeTable.getColumnModel().getColumnCount()) {
            TableColumn column = treeTable.getColumnModel().getColumn(index);
            cName = (String) column.getHeaderValue();
        }
        return cName;
    }

    public void setValueAt(Object item, int row) {
        System.out.println("===> setValueAt("+item+", "+row+")");
        DeploymentNode node = getDeploymentNode(row);
        TreePath selectionPath = treeTable.getTree().getSelectionPath();
        if (selectionPath != null) {
            treeTable.getTree().setSelectionPath(selectionPath);
        }
    }


    static class EventNode extends RemoteServiceEventNode<ProvisionFailureEvent> {
        String status;

        public EventNode(ProvisionFailureEvent event) {
            super(event);
            status = "Pending";
        }

        @Override
        public Throwable getThrown() {
            return getEvent().getThrowable();
        }

        @Override
        public String getDescription() {
            return getEvent().getReason();
        }

        @Override
        public String getOperationalStringName() {
            return getEvent().getServiceElement().getOperationalStringName();
        }

        @Override
        public String getServiceName() {
            return getEvent().getServiceElement().getName();
        }

        String getStatus() {
            return status;
        }

        void setStatus(String status) {
            this.status = status;
        }

        @Override
        public String toString() {
            return getServiceName();
        }
    }

    static class ServiceLogEventNode extends RemoteServiceEventNode<ServiceLogEvent> {

        public ServiceLogEventNode(ServiceLogEvent event) {
            super(event);
        }

        @Override
        public Throwable getThrown() {
            return getEvent().getLogRecord().getThrown();
        }

        @Override
        public String getDescription() {
            LogRecord logRecord = getEvent().getLogRecord();
            return logRecord.getLevel().toString()+": "+logRecord.getMessage();
        }

        @Override
        public String getOperationalStringName() {
            return getEvent().getOpStringName();
        }

        @Override
        public String getServiceName() {
            return getEvent().getServiceName()==null?"":getEvent().getServiceName();
        }

        @Override
        public String toString() {
            return getServiceName();
        }
    }
    
}
