/*
 * Copyright 2008 the original author or authors.
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

import net.jini.core.entry.Entry;
import net.jini.core.lookup.ServiceItem;
import net.jini.lookup.entry.Host;
import org.rioproject.deploy.ServiceBeanInstance;
import org.rioproject.entry.ComputeResourceInfo;
import org.rioproject.opstring.OperationalStringException;
import org.rioproject.opstring.OperationalStringManager;
import org.rioproject.impl.servicebean.ServiceElementUtil;
import org.rioproject.deploy.DeployAdmin;
import org.rioproject.ui.Util;
import org.rioproject.tools.ui.serviceui.ServiceAdminManager;
import org.rioproject.tools.ui.serviceui.ServiceElementPanel;
import prefuse.Display;
import prefuse.Visualization;
import prefuse.controls.ControlAdapter;
import prefuse.data.Graph;
import prefuse.data.Tuple;
import prefuse.visual.VisualItem;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * A listener for graph actions
 *
 * @author Dennis Reedy
 */
public class GraphListener extends ControlAdapter {
    JFrame frame;
    ServiceAdminManager adminManager = ServiceAdminManager.getInstance();
    Graph g;

    GraphListener(Graph g, JFrame frame) {
        this.g = g;
        this.frame = frame;
    }

    public void itemEntered(VisualItem item, MouseEvent e) {
        Display d = (Display) e.getSource();
        d.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        Object uo = item.getSourceTuple().get(Constants.USER_OBJECT);
        if (!(uo instanceof GraphNode))
            return;
        final GraphNode node = (GraphNode) uo;
        StringBuilder buff = new StringBuilder();
        if (node.isOpString()) {
            buff.append("OperationalString: ")
                .append(item.getString(VisualItem.LABEL));
        } else if(node.isServiceElement()) {
            buff.append("ServiceElement: ")
                .append(item.getString(VisualItem.LABEL));
        } else if (node.isServiceInstance()) {
            if(node.getInstance()!=null) {
                buff.append("Instance ID=");
                buff.append(node.getInstance().getServiceBeanConfig().getInstanceID());
            }
            if (node.getServiceItem() != null) {
                ComputeResourceInfo aInfo = getComputeResourceInfo(node.getServiceItem().attributeSets);
                if(aInfo!=null) {
                    buff.append("\n");
                    if (aInfo.osName != null && aInfo.osName.length() > 0) {
                        buff.append("Operating System=");
                        buff.append(aInfo.osName);
                        if (aInfo.osVersion != null &&
                            aInfo.osVersion.length() > 0)
                            buff.append(aInfo.osVersion);
                        buff.append("\n");
                    }
                    if (aInfo.arch != null && aInfo.arch.length() > 0) {
                        buff.append("Architecture=");
                        buff.append(aInfo.arch);
                        buff.append("\n");
                    }
                    if (aInfo.hostAddress != null &&
                        aInfo.hostAddress.length() > 0) {
                        buff.append("Host name=");
                        buff.append(aInfo.hostName);
                        buff.append("\n");
                    }
                    if (aInfo.hostAddress != null &&
                        aInfo.hostAddress.length() > 0) {
                        buff.append("Host address=");
                        buff.append(aInfo.hostAddress);
                        buff.append("\n");
                    }
                    buff.append("JVM Version=");
                    buff.append(aInfo.jvmVersion);
                } else {
                    Host host = getHost(node.getServiceItem().attributeSets);
                    if(host!=null) {
                        buff.append("\n");
                        buff.append(host.hostName);
                    }
                }
            } else {
                buff.append("Active, no ServiceItem");
            }
            if(node.getInstance()!=null) {
                buff.append("\n");
                buff.append(ServiceElementUtil.formatDiscoverySettings(
                    node.getInstance().getServiceBeanConfig()));
            }
        }
        d.setToolTipText(buff.toString());
    }

    private ComputeResourceInfo getComputeResourceInfo(final Entry[] attrs) {
        for (Entry attr : attrs) {
            if (attr instanceof ComputeResourceInfo) {
                return (ComputeResourceInfo) attr;
            }
        }
        return (null);
    }

    private Host getHost(final Entry[] attrs) {
        for (Entry attr : attrs) {
            if (attr instanceof Host) {
                return (Host) attr;
            }
        }
        return null;
    }

    public void itemExited(VisualItem item, MouseEvent e) {
        Display d = (Display) e.getSource();
        d.setToolTipText(null);
        d.setCursor(Cursor.getDefaultCursor());
        // clear the focus
        //Visualization vis = item.getVisualization();
        //vis.getFocusGroup(Visualization.FOCUS_ITEMS).clear();
    }

    public void itemClicked(final VisualItem item, MouseEvent e) {
        final Visualization vis = item.getVisualization();
        if (SwingUtilities.isRightMouseButton(e)) {
            if (e.getClickCount() == 1) {
                Object uo = item.getSourceTuple().get(Constants.USER_OBJECT);
                if (!(uo instanceof GraphNode))
                    return;
                JPopupMenu popup = null;
                final GraphNode node = (GraphNode) uo;
                if (node.isOpString()) {
                    popup = new JPopupMenu();
                    JMenuItem redeploy = new JMenuItem("Redeploy");
                    redeploy.addActionListener(new ActionListener() {
                        public void actionPerformed(ActionEvent ae) {
                            try {
                                ReDeployer.redeploy(node.getProvisionMonitor(),
                                                    node.getOpStringName());
                            } catch (Exception e) {
                                Util.showError(e, frame, "Could not Redeploy");
                            }
                        }
                    });

                    JMenuItem undeploy = new JMenuItem("Undeploy");
                    undeploy.addActionListener(new ActionListener() {

                        public void actionPerformed(ActionEvent actionEvent) {
                            try {
                                DeployAdmin dAdmin =
                                    (DeployAdmin) node.getProvisionMonitor()
                                        .getAdmin();
                                dAdmin.undeploy(node.getOpStringName());
                            } catch (OperationalStringException e) {
                                GraphUtil.removeOpString(g,
                                                         item.getVisualization(),
                                                         node.getOpStringName());
                            } catch (Exception e) {
                                Util.showError(e, frame, "Could not Undeploy");
                            }
                        }
                    });
                    JMenuItem collapse =
                        new JMenuItem(node.isCollapsed() ? "Expand" : "Collapse");
                    collapse.addActionListener(new ActionListener() {
                        public void actionPerformed(ActionEvent actionEvent) {
                            doCollapseSupport(node, vis);
                        }
                    });
                    popup.add(redeploy);
                    popup.add(undeploy);
                    popup.addSeparator();
                    popup.add(collapse);

                }
                if (node.isServiceElement()) {
                    popup = new JPopupMenu();
                    JMenuItem redeployAll = new JMenuItem(
                        "Redeploy the ServiceElement");
                    redeployAll.addActionListener(new ActionListener() {
                        public void actionPerformed(ActionEvent ae) {
                            try {
                                ReDeployer.redeploy(node.getProvisionMonitor(),
                                                    node.getOpStringName(),
                                                    node.getServiceElement());
                            } catch (Exception e) {
                                Util.showError(e, frame,
                                               "Could not Redeploy all Services");
                            }
                        }
                    });

                    JMenuItem serviceElementAdmin = new JMenuItem(
                        "Show ServiceElement UI");
                    serviceElementAdmin.addActionListener(new ActionListener() {
                        public void actionPerformed(ActionEvent ae) {
                            JDialog dialog =
                                new JDialog((JFrame)null,
                                            "ServiceElement Admin",
                                            true);
                            Container contentPane = dialog.getContentPane();
                            ServiceElementPanel sElemPanel =
                                new ServiceElementPanel(dialog);
                            GraphNode[] nodes =
                                GraphUtil.getChildren(g,
                                                      node.getTableNode());
                            ArrayList<ServiceBeanInstance> list =
                                new ArrayList<ServiceBeanInstance>();
                            for(GraphNode node : nodes) {
                                if(node.getInstance()!=null)
                                    list.add(node.getInstance());
                            }
                            try {
                                OperationalStringManager mgr =
                                    Util.getOperationalStringManager(
                                        node.getProvisionMonitor(),
                                        node.getOpStringName());
                                sElemPanel.showServiceElement(
                                    node.getServiceElement(),
                                    list.toArray(new ServiceBeanInstance[list.size()]),
                                    mgr);

                                contentPane.add(sElemPanel, BorderLayout.CENTER);
                                int width = 565;
                                int height = 355;
                                dialog.setSize(new Dimension(width, height));
                                dialog.setLocationRelativeTo(frame);
                                dialog.setVisible(true);
                            } catch (Exception e) {
                                Util.showError(e, dialog,
                                               "Could not Show all Service UIs");
                            }
                        }
                    });

                    JMenuItem serviceUiAdmin = new JMenuItem(
                        "Show all Service UIs");
                    serviceUiAdmin.addActionListener(new ActionListener() {
                        public void actionPerformed(ActionEvent event) {
                            GraphNode[] nodes =
                                GraphUtil.getChildren(g,
                                                      node.getTableNode());
                            Map<String, ServiceItem> items =
                                new HashMap<String, ServiceItem>();
                            String sName = node.getServiceElement().getName();
                            for(GraphNode gn : nodes) {
                                if(gn.getServiceItem()!=null) {
                                String name = sName+" ("+gn.getInstanceID()+")";
                                items.put(name, gn.getServiceItem());
                                }
                            }

                            adminManager.doShowAdminUIs(node.getServiceElement(),
                                                        items);
                        }
                    });
                    JMenuItem collapse =
                        new JMenuItem(node.isCollapsed() ? "Expand" : "Collapse");
                    collapse.addActionListener(new ActionListener() {
                        public void actionPerformed(ActionEvent actionEvent) {
                            doCollapseSupport(node, vis);
                        }
                    });
                    popup.add(serviceElementAdmin);
                    popup.add(serviceUiAdmin);
                    popup.addSeparator();
                    popup.add(redeployAll);
                    popup.addSeparator();
                    popup.add(collapse);
                }
                if (node.isServiceInstance()) {
                    popup = new JPopupMenu();
                    JMenuItem redeploy = new JMenuItem("Redeploy the Service");
                    redeploy.addActionListener(new ActionListener() {
                        public void actionPerformed(ActionEvent ae) {
                            try {
                                ReDeployer.redeploy(node.getProvisionMonitor(),
                                                    node.getOpStringName(),
                                                    node.getInstance());
                            } catch (Exception e) {
                                Util.showError(e, frame,
                                               "Could not Redeploy service");
                            }
                        }
                    });

                    JMenuItem redeployAll = new JMenuItem(
                        "Redeploy the ServiceElement");
                    redeployAll.addActionListener(new ActionListener() {
                        public void actionPerformed(ActionEvent ae) {
                            try {
                                ReDeployer.redeploy(node.getProvisionMonitor(),
                                                    node.getOpStringName(),
                                                    node.getServiceElement());
                            } catch (Exception e) {
                                Util.showError(e, frame,
                                               "Could not Redeploy the ServiceElement");
                            }
                        }
                    });
                    JMenuItem admin = new JMenuItem("Show Service UI");
                    admin.addActionListener(new ActionListener() {
                        public void actionPerformed(ActionEvent ae) {
                            adminManager.doShowAdminUI(node.getServiceItem(), frame);
                        }
                    });
                    popup.add(redeploy);
                    popup.add(redeployAll);
                    popup.addSeparator();
                    popup.add(admin);
                }
                if (popup != null) {
                    popup.pack();
                    popup.show(e.getComponent(), e.getX(), e.getY());
                }

            }
            return;
        }
        /*
        if (e.getClickCount() == 1) {
            Object uo = item.getSourceTuple().get(Constants.USER_OBJECT);
            if (uo instanceof GraphNode) {
                if(((GraphNode)uo).isServiceInstance())
                    return;
            }
            g.getSpanningTree((Node)item.getSourceTuple());
            vis.run("filter");
        }
        */
        if (e.getClickCount() == 2) {
            Object uo = item.getSourceTuple().get(Constants.USER_OBJECT);
            if (!(uo instanceof GraphNode))
                return;
            GraphNode node = (GraphNode) uo;
            if (node.getServiceItem() != null) {
                adminManager.doShowAdminUI(node.getServiceItem(), frame);
            } //else {
              //  vis.cancel("treeRoot");
              //  doCollapseSupport(node, vis);
            //}
        }
    }

    void doCollapseSupport(GraphNode node, Visualization vis) {
        if (!node.isCollapsed()) {
            vis.cancel("animate");
            node.setCollapsedVertex(collapse(node));
            node.setCollapsed(true);
        } else {
            Object o = node.getCollapsedVertex();
            if (o instanceof CollapsedOpString) {
                CollapsedOpString collapsedVertex = (CollapsedOpString) o;
                expand(node, collapsedVertex);
                GraphUtil.setOpStringState(g, node);
            }
            if (o instanceof CollapsedServiceElement) {
                CollapsedServiceElement collapsedVertex =
                    (CollapsedServiceElement) o;

                /* Set the collapsed proeprty to false,
                 * allowing the node to be expanded */
                node.setCollapsed(false);
                expand(node, collapsedVertex);
            }
        }
        vis.run("filter");        
    }

    /*
     * Delete children of the node
     */
    Object collapse(GraphNode node) {
        CollapsedOpString collapsedOpString = null;
        CollapsedServiceElement collapsedServiceElem = null;
        if (node.isOpString())
            collapsedOpString = new CollapsedOpString();
        if (node.isServiceElement()) {
            collapsedServiceElem =
                (CollapsedServiceElement)node.getCollapsedVertex();
            if(collapsedServiceElem==null) {
                collapsedServiceElem = new CollapsedServiceElement();
                collapsedServiceElem.setServiceElementNode(node);
            }
        }

        ArrayList<GraphNode> list = new ArrayList<GraphNode>();
        for (Iterator it = g.neighbors(node.getTableNode()); it.hasNext();) {
            Tuple t = (Tuple) it.next();
            Object o = t.get(Constants.USER_OBJECT);
            if (o != null && o instanceof GraphNode) {
                GraphNode g = (GraphNode) o;
                if (node.isServiceElement() && g.isOpString())
                    continue;
                if (node.isOpString() && g.isServiceElement()) {
                    if (collapsedOpString != null)
                        collapsedOpString.addServiceElement(
                            (CollapsedServiceElement) collapse(g));
                }
                list.add(g);
            }
        }

        GraphNode[] gNodes = list.toArray(new GraphNode[list.size()]);
        for (GraphNode gNode : gNodes) {
            g.removeTuple(gNode.getTableNode());
            gNode.setTableNode(null);
            if (collapsedServiceElem != null) {
                collapsedServiceElem.addInstance(gNode);
            }
        }
        return (collapsedOpString == null ? collapsedServiceElem :
                collapsedOpString);
    }

    void expand(GraphNode opStringNode, CollapsedOpString collapsed) {
        CollapsedServiceElement[] elements = collapsed.getInstances();
        for (CollapsedServiceElement element : elements) {
            GraphNode serviceNode = element.getServiceElementNode();            
            GraphUtil.addService(g,
                                 opStringNode.getTableNode(),
                                 serviceNode.getServiceElement().getName(),
                                 serviceNode);
            expand(serviceNode, element);
        }
        opStringNode.setCollapsed(false);
    }

    void expand(GraphNode serviceNode, CollapsedServiceElement collapsed) {
        if (serviceNode.isCollapsed())
            return;
        GraphNode[] instances = collapsed.getInstances();
        for (GraphNode instance : instances) {
            GraphUtil.addServiceInstance(g,
                                         serviceNode.getTableNode(),
                                         instance);
        }
        serviceNode.setCollapsed(false);
        serviceNode.setCollapsedVertex(null);
    }

    static class CollapsedServiceElement {
        GraphNode serviceElementNode;
        ArrayList<GraphNode> instances = new ArrayList<GraphNode>();

        void addInstance(GraphNode node) {
            instances.add(node);
        }

        void removeInstance(GraphNode node) {
            instances.remove(node);
        }

        GraphNode[] getInstances() {
            return instances.toArray(new GraphNode[instances.size()]);
        }

        void setServiceElementNode(GraphNode serviceElementNode) {
            this.serviceElementNode = serviceElementNode;
        }

        GraphNode getServiceElementNode() {
            return serviceElementNode;
        }
    }

    static class CollapsedOpString {
        ArrayList<CollapsedServiceElement> elements =
            new ArrayList<CollapsedServiceElement>();

        void addServiceElement(CollapsedServiceElement element) {
            elements.add(element);
        }

        void removeServiceElement(CollapsedServiceElement element) {
            elements.remove(element);
        }

        CollapsedServiceElement[] getInstances() {
            return elements.toArray(new CollapsedServiceElement[elements.size()]);
        }

    }
}
