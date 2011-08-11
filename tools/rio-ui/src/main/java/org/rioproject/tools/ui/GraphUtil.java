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

import org.rioproject.deploy.ServiceBeanInstance;
import org.rioproject.opstring.ServiceElement;
import prefuse.Visualization;
import prefuse.data.Graph;
import prefuse.data.Node;
import prefuse.data.Tuple;
import prefuse.data.tuple.TableNode;
import prefuse.data.tuple.TupleSet;
import prefuse.visual.VisualItem;
import prefuse.visual.tuple.TableEdgeItem;
import prefuse.visual.tuple.TableNodeItem;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Utilities for working with the Graph
 *
 * @author Dennis Reedy
 */
public class GraphUtil {

    public static Node addService(Graph g,
                                  Node opstringNode,
                                  ServiceElement service) {
        Node node = g.addNode();
        node.set(VisualItem.LABEL, service.getName());
        node.set(Constants.USER_OBJECT,
                 new GraphNode(service, (TableNode) node));
        node.set(Constants.STATE, Constants.EMPTY);
        g.addEdge(opstringNode, node);
        return (node);
    }

    public static Node addService(Graph g,
                                  Node opstringNode,
                                  String name,
                                  GraphNode graphNode) {
        Node node = g.addNode();
        node.set(VisualItem.LABEL, name);
        node.set(Constants.USER_OBJECT, graphNode);
        node.set(Constants.STATE, Constants.EMPTY);
        g.addEdge(opstringNode, node);
        graphNode.setTableNode((TableNode) node);
        return (node);
    }

    public static Node addServiceInstance(Graph g,
                                          Node serviceNode,
                                          String opStringName,
                                          long instanceID) {
        Node instance = g.addNode();
        g.addEdge(serviceNode, instance);
        instance.set(VisualItem.LABEL, "");
        GraphNode gNode = new GraphNode(instanceID,
                                        opStringName,
                                        (TableNode) instance);
        instance.set(Constants.USER_OBJECT, gNode);
        instance.set(Constants.STATE, Constants.EMPTY);
        gNode.setTableNode((TableNode)instance);
        return (instance);
    }

    public static Node addServiceInstance(Graph g,
                                          Node serviceNode,
                                          GraphNode graphNode) {
        Node instance = g.addNode();
        g.addEdge(serviceNode, instance);
        instance.set(VisualItem.LABEL, "");
        instance.set(Constants.USER_OBJECT, graphNode);
        instance.set(Constants.STATE, Constants.EMPTY);
        if (graphNode.getServiceItem() == null) {
            instance.set(Constants.STATE, Constants.ACTIVE_NO_SERVICE_ITEM);
        } else {
            instance.set(Constants.STATE, Constants.ACTIVE);
        }
        graphNode.setTableNode((TableNode) instance);
        return (instance);
    }

    public static GraphNode addServiceInstance(Graph g,
                                               GraphNode serviceNode,
                                               ServiceBeanInstance sbInstance) {
        Node instance = g.addNode();
        g.addEdge(serviceNode.getTableNode(), instance);
        instance.set(VisualItem.LABEL, "");
        GraphNode graphNode =
            new GraphNode(sbInstance.getServiceBeanConfig().getInstanceID(),
                          serviceNode.getServiceElement().getOperationalStringName(),
                          (TableNode) instance);
        instance.set(Constants.USER_OBJECT, graphNode);
        if (graphNode.getInstance() != null) {
            if (graphNode.getServiceItem() == null) {
                instance.set(Constants.STATE, Constants.ACTIVE_NO_SERVICE_ITEM);
            } else {
                instance.set(Constants.STATE, Constants.ACTIVE);
            }
        } else {
            instance.set(Constants.STATE, Constants.FAILED);
        }
        /*
        if(graphNode.getServiceItem()==null) {
            instance.set(Constants.STATE, Constants.FAILED);
        } else {
            instance.set(Constants.STATE, Constants.ACTIVE);
        }
        */
        graphNode.setTableNode((TableNode) instance);
        return (graphNode);
    }

    /*
     * Set the state of the OperationalString
     */
    public static void setOpStringState(Graph g, GraphNode opStringNode) {
        GraphNode[] services;
        if (opStringNode.isCollapsed()) {
            services = getCollapsedChildren(opStringNode);
        } else {
            services = getChildren(g, opStringNode.getTableNode());
        }
        int opStringState = Constants.EMPTY;
        for (GraphNode service : services) {
            int numActive = 0;
            int planned = service.getServiceElement().getPlanned();
            int numWithServiceItems = 0;
            GraphNode[] instances;
            if (service.getCollapsedVertex() != null)
                instances = getCollapsedChildren(service);
            else
                instances = getChildren(g, service.getTableNode());

            for (GraphNode instance : instances) {
                if (instance.getInstance() != null)
                    numActive++;
                if(instance.getServiceItem()!=null)
                    numWithServiceItems++;
            }
            if(planned==0) {
                if (service.getTableNode() != null)
                    service.getTableNode().set(Constants.STATE,
                                               Constants.EMPTY);
            } else if (numActive == 0) {
                if (service.getTableNode() != null)
                    service.getTableNode().set(Constants.STATE,
                                               Constants.FAILED);
                opStringState = Constants.FAILED;
            } else if (numActive < planned) {
                if (service.getTableNode() != null)
                    service.getTableNode().set(Constants.STATE,
                                               Constants.WARNING);
                if (opStringState != Constants.FAILED)
                    opStringState = Constants.WARNING;
            } else {
                if (service.getTableNode() != null) {
                    int state = numWithServiceItems==numActive?
                                Constants.ACTIVE:
                                Constants.ACTIVE_NO_SERVICE_ITEM; 
                    service.getTableNode().set(Constants.STATE, state);
                    if(state==Constants.ACTIVE_NO_SERVICE_ITEM)
                        opStringState = Constants.WARNING;
                }
            }
        }
        opStringNode.getTableNode().set(Constants.STATE,
                                        (opStringState == Constants.EMPTY ?
                                         Constants.ACTIVE : opStringState));
    }

    /*
     * Get the children of a TableNode as an array of GraphNode instances
     */
    public static GraphNode[] getChildren(Graph g, TableNode node) {
        ArrayList<GraphNode> list = new ArrayList<GraphNode>();
        for (Iterator it = g.outNeighbors(node); it.hasNext();) {
            Tuple t = (Tuple) it.next();
            Object o = t.get(Constants.USER_OBJECT);
            if (o != null && o instanceof GraphNode) {
                list.add((GraphNode) o);
            }
        }
        return (list.toArray(new GraphNode[list.size()]));
    }

    /*
     * Remove an OperationalString from the graph
     */
    public static void removeOpString(Graph g, Visualization vis, String name) {
        vis.cancel("filter");
        TupleSet set = vis.getGroup(Constants.TREE);
        for (Iterator it = set.tuples(); it.hasNext();) {
            Object o = it.next();
            boolean delete = false;
            Tuple sourceTuple = null;
            Object uo = null;

            if (o instanceof TableEdgeItem) {
                TableEdgeItem tei = (TableEdgeItem) o;
                uo = tei.getTargetNode().get(Constants.USER_OBJECT);
            }

            if (o instanceof TableNodeItem) {
                TableNodeItem tni = (TableNodeItem) o;
                uo = tni.get(Constants.USER_OBJECT);
                if (uo != null && uo.equals(Constants.ROOT))
                    continue;
                if (uo instanceof GraphNode) {
                    GraphNode node = (GraphNode) uo;
                    if (node.isServiceInstance()) {
                        if (!node.getOpStringName().equals(name))
                            continue;
                    }
                }
                sourceTuple = tni.getSourceTuple();
            }

            if (uo != null) {
                if (uo instanceof GraphNode) {
                    GraphNode node = (GraphNode) uo;
                    if (node.getOpString() != null) {
                        if (node.getOpStringName().equals(name))
                            delete = true;
                    }
                    if (node.isServiceElement()) {
                        ServiceElement se = node.getServiceElement();
                        if (se.getOperationalStringName().equals(name))
                            delete = true;
                    }
                    if (node.isServiceInstance())
                        delete = true;
                }
            }

            if (delete && sourceTuple != null) {
                try {
                    g.removeTuple(sourceTuple);
                } catch (IllegalArgumentException e) {
                    e.printStackTrace();
                    //break;
                }
            }
        }
        vis.run("filter");

    }

    private static GraphNode[] getCollapsedChildren(GraphNode node) {
        GraphNode[] nodes;
        if (node.isOpString()) {
            List<GraphNode> list = new ArrayList<GraphNode>();
            GraphListener.CollapsedServiceElement[] elements =
                ((GraphListener.CollapsedOpString)
                    node.getCollapsedVertex()).getInstances();
            for (GraphListener.CollapsedServiceElement element : elements) {
                list.add(element.getServiceElementNode());
            }
            nodes = list.toArray(new GraphNode[list.size()]);
        } else {
            nodes =
                ((GraphListener.CollapsedServiceElement)
                    node.getCollapsedVertex()).getInstances();
        }
        return (nodes);
    }
}
