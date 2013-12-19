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

import net.jini.config.Configuration;
import net.jini.config.ConfigurationException;
import net.jini.core.lookup.ServiceItem;
import net.jini.id.Uuid;
import org.rioproject.deploy.ServiceBeanInstance;
import org.rioproject.opstring.OperationalString;
import org.rioproject.opstring.OperationalStringManager;
import org.rioproject.opstring.ServiceElement;
import org.rioproject.monitor.ProvisionMonitor;
import org.rioproject.ui.GlassPaneContainer;
import org.rioproject.ui.Util;
import org.rioproject.tools.ui.progresspanel.SingleComponentInfiniteProgress;
import prefuse.Display;
import prefuse.Visualization;
import prefuse.action.ActionList;
import prefuse.action.GroupAction;
import prefuse.action.ItemAction;
import prefuse.action.RepaintAction;
import prefuse.action.animate.ColorAnimator;
import prefuse.action.animate.PolarLocationAnimator;
import prefuse.action.animate.QualityControlAnimator;
import prefuse.action.animate.VisibilityAnimator;
import prefuse.action.assignment.ColorAction;
import prefuse.action.assignment.FontAction;
import prefuse.action.layout.CollapsedSubtreeLayout;
import prefuse.action.layout.Layout;
import prefuse.action.layout.graph.NodeLinkTreeLayout;
import prefuse.activity.Activity;
import prefuse.activity.SlowInSlowOutPacer;
import prefuse.controls.*;
import prefuse.data.Graph;
import prefuse.data.Node;
import prefuse.data.Schema;
import prefuse.data.Tuple;
import prefuse.data.event.TupleSetListener;
import prefuse.data.expression.Predicate;
import prefuse.data.expression.parser.ExpressionParser;
import prefuse.data.tuple.DefaultTupleSet;
import prefuse.data.tuple.TableNode;
import prefuse.data.tuple.TupleSet;
import prefuse.render.DefaultRendererFactory;
import prefuse.render.EdgeRenderer;
import prefuse.render.LabelRenderer;
import prefuse.render.ShapeRenderer;
import prefuse.util.ColorLib;
import prefuse.util.FontLib;
import prefuse.util.GraphicsLib;
import prefuse.util.PrefuseLib;
import prefuse.util.display.DisplayLib;
import prefuse.visual.DecoratorItem;
import prefuse.visual.VisualItem;
import prefuse.visual.expression.InGroupPredicate;
import prefuse.visual.sort.TreeDepthItemSorter;

import javax.swing.*;
import javax.swing.plaf.basic.BasicToolTipUI;
import java.awt.*;
import java.awt.geom.GeneralPath;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Vector;

/**
 * A tree viewer to display the status of deployed OperationalStrings
 *
 * @author Dennis Reedy
 */
public class GraphView extends Display {
    private final Graph g;
    private final Node root;
    //private SearchTupleSet search;
    private static final Schema DECORATOR_SCHEMA = PrefuseLib.getVisualItemSchema();
    static {
    	DECORATOR_SCHEMA.setDefault(VisualItem.INTERACTIVE, false);
    	DECORATOR_SCHEMA.setDefault(VisualItem.TEXTCOLOR, ColorLib.rgb(255,250,250));
    	DECORATOR_SCHEMA.setDefault(VisualItem.FONT, FontLib.getFont("Tahoma",12));
    }
    private final NodeLinkTreeLayout graphLayout;
    private final JFrame frame;
    private final Visualization vis = m_vis;
    private static final Predicate rootFilter = ExpressionParser.predicate("INDEGREE()==0");
    private int orientation;
    private SingleComponentInfiniteProgress progressPanel;
    private final Configuration config;
    private final ColorManager colorManager;
    private final ProvisionMonitor monitor;
    private static final String COMPONENT = GraphView.class.getPackage().getName();

    public GraphView(final JFrame frame,
                     final Configuration config,
                     final ColorManager colorManager,
                     final ProvisionMonitor monitor,
                     final int orientation) {
        super(new Visualization());
        this.frame = frame;
        this.config = config;
        this.colorManager = colorManager;
        this.monitor = monitor;
        this.orientation = orientation;
        g = new Graph(true);
        g.addColumn(VisualItem.LABEL, String.class);
        g.addColumn(Constants.USER_OBJECT, Object.class);
        g.addColumn(Constants.STATE, int.class);

        addControlListener(new GraphListener(g, frame));
        setCustomToolTip(new MultiLineToolTip());

        root = g.addNode();
        root.set(VisualItem.LABEL, Constants.ROOT);
        root.set(Constants.USER_OBJECT, Constants.ROOT);

        // -- set up visualization --
        vis.add(Constants.TREE, g);
        vis.setInteractive(Constants.TREE_EDGES, null, false);

        /* Make the root invisible until the system is discovered */
        vis.setVisible(Constants.TREE_NODES, null, false);

        // -- set up edge renderers --
        DefaultRendererFactory drf = new DefaultRendererFactory();
        drf.setDefaultRenderer(new CustomShapeRenderer());
        drf.add(new InGroupPredicate(Constants.NODE_DECORATORS),
                new MyLabelRenderer());
        drf.add(new InGroupPredicate(Constants.TREE_EDGES),
                getEdgeTypeRenderer(orientation));

        //drf.add(new InGroupPredicate(Constants.TREE_EDGES),
                //new EdgeRenderer(prefuse.Constants.EDGE_TYPE_LINE));
       //         new EdgeRenderer(prefuse.Constants.EDGE_TYPE_CURVE));


        vis.setRendererFactory(drf);
        vis.addDecorators(Constants.NODE_DECORATORS,
                          Constants.TREE_NODES,
                          DECORATOR_SCHEMA);

        // -- set up processing actions --
        ColorAction nodeStroke = new ColorAction(Constants.TREE_NODES,
                                                 VisualItem.STROKECOLOR);
        nodeStroke.setDefaultColor(ColorManager.BORDER_COLOR_RGB);
        nodeStroke.add("_hover", ColorManager.BORDER_COLOR_RGB);

        Predicate noServiceItemFilter =
            ExpressionParser.predicate("state=="+Constants.ACTIVE_NO_SERVICE_ITEM);
        //nodeStroke.add(noServiceItemFilter,
        //               ColorManager.NO_SERVICE_ITEM_BORDER_COLOR_RGB);

        ColorAction nodeFill = new ColorAction(Constants.TREE_NODES,
                                               VisualItem.FILLCOLOR);
        nodeFill.add("_hover", ColorManager.HOVER_COLOR_RGB);

        /* Color predicates */
        nodeFill.add(rootFilter, ColorManager.ROOT_COLOR_RGB);
        //Predicate okayFilter =
        //    ExpressionParser.predicate(Constants.STATE+"=="+Constants.ACTIVE+" || " +
        //                               Constants.STATE+"=="+Constants.ACTIVE_NO_SERVICE_ITEM);
        Predicate okayFilter =
            ExpressionParser.predicate(Constants.STATE+"=="+Constants.ACTIVE);
        nodeFill.add(okayFilter,
                     ColorLib.color(colorManager.getOkayColor()));

        Predicate emptyFilter =
            ExpressionParser.predicate(Constants.STATE+"=="+Constants.EMPTY);
        nodeFill.add(emptyFilter, ColorManager.EMPTY_COLOR_RGB);

        Predicate ambiguousFilter =
            ExpressionParser.predicate(Constants.STATE+"=="+Constants.WARNING);
        nodeFill.add(ambiguousFilter,
                     ColorLib.color(colorManager.getWarningColor()));
        nodeFill.add(noServiceItemFilter,
                     ColorLib.color(colorManager.getWarningColor()));        
        Predicate failureFilter =
            ExpressionParser.predicate(Constants.STATE+"=="+Constants.FAILED);
        nodeFill.add(failureFilter,
                     ColorLib.color(colorManager.getFailureColor()));

        colorManager.setColorAction(nodeFill);
        colorManager.setAmbiguousFilter(ambiguousFilter);
        colorManager.setFailureFilter(failureFilter);
        colorManager.setOkayFilter(okayFilter);
        colorManager.setNoServiceItemFilter(noServiceItemFilter);

        //ItemAction nodeColor = new NodeColorAction(TREE_NODES);
        ItemAction textColor = new TextColorAction(Constants.TREE_NODES);

        vis.putAction("textColor", textColor);

        ItemAction edgeColor = new ColorAction(Constants.TREE_EDGES,
                                               VisualItem.STROKECOLOR,
                                               ColorManager.EDGE_COLOR_RGB);

        FontAction fonts = new FontAction(Constants.TREE_NODES,
                                          FontLib.getFont("Tahoma", 12));
        fonts.add("ingroup('_focus_')", FontLib.getFont("Tahoma", 13));

        // recolor
        ActionList recolor = new ActionList();
        //recolor.add(new NodeColorAction());
        recolor.add(textColor);
        recolor.add(nodeStroke);
        recolor.add(nodeFill);
        vis.putAction("recolor", recolor);

        // repaint
        ActionList repaint = new ActionList();
        repaint.add(recolor);
        repaint.add(new RepaintAction());
        vis.putAction("repaint", repaint);

        // animate paint change
        ActionList animatePaint = new ActionList(400);
        animatePaint.add(new ColorAnimator(Constants.TREE_NODES));
        animatePaint.add(new RepaintAction());
        vis.putAction("animatePaint", animatePaint);

        //TreeLayout graphLayout = new RadialTreeLayout(Constants.TREE);
        graphLayout =
            new NodeLinkTreeLayout(Constants.TREE, orientation, 48, 12, 16);

        // create the tree layout action
        CollapsedSubtreeLayout subLayout = new CollapsedSubtreeLayout(Constants.TREE);
        vis.putAction("subLayout", subLayout);
        ActionList layout = new ActionList(Activity.INFINITY);        
        layout.add(graphLayout);
        layout.add(subLayout);
        layout.add(new LabelPositionLayout(Constants.NODE_DECORATORS));
        vis.putAction("graphLayout", layout);

        ActionList treeRoot = new ActionList();
        treeRoot.add(new TreeRootAction(Constants.TREE));
        vis.putAction("treeRoot", treeRoot);

        // create the filtering and layout
        ActionList filter = new ActionList();
        filter.add(treeRoot);
        filter.add(fonts);
        filter.add(layout);
        filter.add(edgeColor);
        filter.add(recolor);
        vis.putAction("filter", filter);

        // animated transition
        ActionList animate = new ActionList(1000);
        animate.setPacingFunction(new SlowInSlowOutPacer());
        animate.add(new QualityControlAnimator());
        animate.add(new VisibilityAnimator(Constants.TREE));
        animate.add(new PolarLocationAnimator(Constants.TREE_NODES,
                                              Constants.LINEAR));
        animate.add(new ColorAnimator(Constants.TREE_NODES));
        animate.add(new RepaintAction());
        vis.putAction("animate", animate);
        vis.alwaysRunAfter("filter", "animate");

        // initialize the display
        setSize(500, 500);
        setItemSorter(new TreeDepthItemSorter());
        addControlListener(new DragControl());
        addControlListener(new ZoomControl());
        addControlListener(new PanControl());
        addControlListener(new FocusControl(1, "filter"));
        addControlListener(new HoverActionControl("repaint"));
        //addControlListener(new NeighborHighlightControl("repaint"));

        // filter graph and perform layout
        vis.run("filter");

        // maintain a set of items that should be interpolated linearly
        // this isn't absolutely necessary, but makes the animations nicer
        // the PolarLocationAnimator should read this set and act accordingly
        vis.addFocusGroup(Constants.LINEAR, new DefaultTupleSet());
        vis.getGroup(Visualization.FOCUS_ITEMS).addTupleSetListener(
            new TupleSetListener() {
                public void tupleSetChanged(TupleSet t,
                                            Tuple[] add,
                                            Tuple[] rem) {
                    TupleSet linearInterp = vis.getGroup(Constants.LINEAR);
                    if (add.length < 1)
                        return;
                    linearInterp.clear();
                    for (Node n = (Node) add[0]; n != null; n = n.getParent())
                        linearInterp.addTuple(n);
                }
            }
        );        
    }

    public ColorManager getColorManager() {
        return colorManager;
    }

    /*public void showProgressPanel() {
        if(progressPanel==null) {
            progressPanel = new SingleComponentInfiniteProgress(false);
            GlassPaneContainer.findGlassPaneContainerFor(getParent())
                                                 .setGlassPane(progressPanel);
        }
        String waitMessage = "Waiting to discover the Rio system ...";
        try {
            waitMessage = (String)config.getEntry(COMPONENT,
                                                  "waitMessage",
                                                  String.class,
                                                  waitMessage);
        } catch (ConfigurationException e) {
            e.printStackTrace();
        }
        progressPanel.setText(waitMessage);
        progressPanel.start();
    }

    public void systemDown() {
        synchronized(vis) {
            vis.cancel("filter");
            vis.setVisible(Constants.TREE_NODES, rootFilter, false);
            vis.run("filter");
        }
        showProgressPanel();
    }

    public void systemUp() {
        if(progressPanel!=null) {
            progressPanel.stop();
        }
        synchronized(vis) {
            vis.cancel("filter");
            vis.setVisible(Constants.TREE_NODES, rootFilter, true);
            vis.run("filter");
        }
    }*/

    public void systemUp() {
        synchronized(vis) {
            vis.cancel("filter");
            vis.setVisible(Constants.TREE_NODES, rootFilter, true);
            vis.run("filter");
        }
    }

    public void zoomToFit() {
        if (!isTranformInProgress()) {
            int margin = 20;
            long duration = 2000;
            Rectangle2D bounds = vis.getBounds(Constants.TREE);
            GraphicsLib.expand(bounds, margin + (int)(1/getScale()));
            DisplayLib.fitViewToBounds(this, bounds, duration);
        }
    }

    /*public void zoom(final double degree) {
        zoom(new Point2D.Float(),degree);
        repaint();
    }*/

    public void setOrientation(final int orientation) {
        this.orientation = orientation;
        synchronized(vis) {
            DefaultRendererFactory drf = new DefaultRendererFactory();
            drf.setDefaultRenderer(new CustomShapeRenderer());
            drf.add(new InGroupPredicate(Constants.NODE_DECORATORS),
                    new MyLabelRenderer());

            drf.add(new InGroupPredicate(Constants.TREE_EDGES),
                    getEdgeTypeRenderer(orientation));
            
            vis.setRendererFactory(drf);
            vis.cancel("filter");
            graphLayout.setOrientation(orientation);
            vis.run("filter");
        }             
    }

    private prefuse.render.Renderer getEdgeTypeRenderer(final int orientation) {
        prefuse.render.Renderer edgeTypeRenderer;
        if(orientation==prefuse.Constants.ORIENT_TOP_BOTTOM) {
            edgeTypeRenderer = new EdgeRenderer(prefuse.Constants.EDGE_TYPE_LINE);
        } else {
            edgeTypeRenderer = new EdgeRenderer(prefuse.Constants.EDGE_TYPE_CURVE);
        }

        return edgeTypeRenderer;
    }

    public int getOrientation() {
        return orientation;
    }

    /*
     * Add an OperationalString
     */
    public void addOpString(final ProvisionMonitor monitor, final OperationalString opstring) {
        if(getOpStringNode(opstring.getName())==null) {
            synchronized(vis) {
                vis.cancel("filter");
                Node opstringNode = g.addNode();
                opstringNode.set(VisualItem.LABEL, opstring.getName());
                opstringNode.set(Constants.USER_OBJECT,
                                 new GraphNode(monitor,
                                               opstring,
                                               (TableNode)opstringNode));
                opstringNode.set(Constants.STATE, Constants.EMPTY);

                g.addEdge(root, opstringNode);
                //if(parent!=null)
                //    g.addEdge(parent, opstringNode);

                ServiceElement[] services = opstring.getServices();
                for(ServiceElement service : services) {
                    doAddServiceElement(service, monitor);
                }

                vis.run("filter");
            }
        }
    }

    void removeAllOpStrings() {
        GraphNode[] children = GraphUtil.getChildren(g, (TableNode) root);
        for(GraphNode child : children) {
            removeOpString(child.getOpStringName());
        }
        //vis.run("filter");
    }

    void removeOpString(final String name) {
        synchronized(vis) {
            GraphUtil.removeOpString(g, vis, name);
        }
    }

    void refresh() {
        GraphNode[] children = GraphUtil.getChildren(g, (TableNode) root);
        for(GraphNode child : children) {
            updateOpString(child.getProvisionMonitor(), child.getOpString());
        }
    }

    /*
     * Update an OperationalString
     */
    void updateOpString(final ProvisionMonitor monitor, final OperationalString opString) {
        synchronized(vis) {
            vis.cancel("filter");
            GraphNode opStringNode = getOpStringNode(opString.getName());
            if(opStringNode!=null) {
                OperationalStringManager mgr;
                try {
                    mgr = Util.getOperationalStringManager(monitor,
                                                           opString.getName());
                } catch (Exception e) {
                    Util.showError(e, frame,
                                   "Could not get an OperationalStringManager");
                    GraphUtil.removeOpString(g, vis, opString.getName());
                    vis.run("filter");
                    return;
                }
                opStringNode.setOpString(opString);
                opStringNode.setProvisionMonitor(monitor);
                ServiceElement[] services = opString.getServices();

                // TODO: If there are existing ServiceEements that are no longer
                // in the opstring they need to be removed

                for(ServiceElement service : services) {
                    GraphNode sElemNode = getServiceElementNode(opStringNode,
                                                                service);
                    if(sElemNode!=null) {
                        sElemNode.setServiceElement(service);
                        sElemNode.setProvisionMonitor(monitor);
                        Node serviceNode = sElemNode.getTableNode();

                        /* Fetch the known ServiceBeanInstances */
                        ServiceBeanInstance[] instances =
                            new ServiceBeanInstance[0];
                        try {
                            instances = mgr.getServiceBeanInstances(service);
                        } catch (Exception e) {
                            Util.showError(e, frame,
                                           "Fetching ServiceBean Instances");
                        }

                        /* If not collapsed, easier to delete the instance
                         * nodes and re-add them */
                        if(!(opStringNode.isCollapsed() || sElemNode.isCollapsed())) {
                            int instanceCount = removeServiceInstances(sElemNode);
                            for(int i=0; i<instanceCount; i++) {
                                Node instanceNode =
                                    GraphUtil.addServiceInstance(g,
                                                                 serviceNode,
                                                                 opString.getName(),
                                                                 Constants.AVAILABLE_ID);
                                GraphNode ign =
                                    (GraphNode)instanceNode.get(Constants.USER_OBJECT);
                                ign.setProvisionMonitor(monitor);
                            }
                        }
                        for(ServiceBeanInstance instance : instances) {
                            GraphNode n = getServiceBeanInstance(sElemNode,
                                                                 instance);
                            if(n!=null) {
                                n.setInstance(instance);
                                ServiceItemFetchQ.write(n, this);
                                if(n.getTableNode()!=null) {
                                    n.getTableNode().set(Constants.STATE,
                                                         Constants.ACTIVE_NO_SERVICE_ITEM);
                                }
                            } else {
                                System.err.println("### updateOpString(): " +
                                                   "ServiceBeanInstance node null for " +
                                                   "["+service.getName()+"], instanceID=" +
                                                   "["+instance.getServiceBeanConfig().getInstanceID()+"]");
                            }
                        }
                    } else {
                        Node node =
                            GraphUtil.addService(g,
                                                 opStringNode.getTableNode(),
                                                 service);
                        GraphNode sgn =
                            (GraphNode)node.get(Constants.USER_OBJECT);
                        sgn.setProvisionMonitor(monitor);
                    }
                }
            }
            vis.run("filter");
        }
    }

    /*
     * Get the OperationalString Names
     */
    String[] getOpStringNames() {
        ArrayList<String> list = new ArrayList<String>();
        for(Iterator it=g.neighbors(root); it.hasNext();) {
            Tuple t = (Tuple)it.next();
            Object o = t.get(Constants.USER_OBJECT);
            if(o!=null && o instanceof GraphNode) {
                if(((GraphNode)o).getOpString()!=null)
                    list.add(((GraphNode)o).getOpStringName());
            }
        }
        return(list.toArray(new String[list.size()]));
    }

    void addServiceElement(final ServiceElement sElem, final ProvisionMonitor monitor) {
        synchronized(vis) {
            vis.cancel("filter");
            doAddServiceElement(sElem, monitor);
            vis.run("filter");
        }
    }

    private void doAddServiceElement(final ServiceElement sElem, final ProvisionMonitor monitor) {
        GraphNode opStringNode =
            getOpStringNode(sElem.getOperationalStringName());
        if(opStringNode!=null) {
            GraphNode elemNode = getServiceElementNode(opStringNode, sElem);
            if(elemNode == null) {
                Node serviceNode = GraphUtil.addService(g, opStringNode.getTableNode(), sElem);
                GraphNode sgn =
                    (GraphNode)serviceNode.get(Constants.USER_OBJECT);
                sgn.setProvisionMonitor(monitor);
                for(int i=0; i<sElem.getPlanned(); i++) {
                    doAddServiceBeanInstance(opStringNode.getOpStringName(), serviceNode, monitor);
                }
                setOpStringState(sElem.getOperationalStringName());
            }
        }
    }

    private GraphNode doAddServiceBeanInstance(final String opStringName,
                                               final Node serviceNode,
                                               final ProvisionMonitor monitor) {
        Node instanceNode = GraphUtil.addServiceInstance(g, serviceNode, opStringName, Constants.AVAILABLE_ID);
        GraphNode serviceInstance = (GraphNode)instanceNode.get(Constants.USER_OBJECT);
        serviceInstance.setProvisionMonitor(monitor);
        return serviceInstance;
    }

    void removeServiceElement(final ServiceElement elem) {
        synchronized(vis) {
            vis.cancel("filter");
            GraphNode serviceElementNode = getServiceElementNode(elem);
            if(serviceElementNode!=null) {
                removeServiceInstances(serviceElementNode);
                g.removeTuple(serviceElementNode.getTableNode());
                setOpStringState(elem.getOperationalStringName());
            }
            vis.run("filter");
        }
    }

    /*
     * Indicate that a service has been terminated or failed
     */
    void serviceDown(final ServiceElement sElem, final ServiceBeanInstance instance) {
        synchronized(vis) {
            GraphNode node = getServiceBeanInstance(sElem, instance);
            if(node!=null) {
                ServiceElement.ProvisionType pType = sElem.getProvisionType();
                if(pType.equals(ServiceElement.ProvisionType.FIXED)) {
                    GraphNode sElemNode = getServiceElementNode(sElem);
                    if(sElemNode==null) {
                        System.err.println("!!! serviceDown(): FIXED service " +
                                           "["+sElem.getName()+"] " +
                                           "instance ["+
                                           instance.getServiceBeanConfig().getInstanceID()+"] " +
                                           "ServiceElement node is null");
                        return;
                    }
                    GraphNode[] children = GraphUtil.getChildren(g, sElemNode.getTableNode());
                    if(children.length>sElem.getPlanned()) {
                        vis.cancel("filter");
                        g.removeTuple(node.getTableNode());
                        vis.run("filter");
                    } else {
                        serviceInstanceDown(sElem, instance, node);
                        vis.run("repaint");
                    }
                } else {
                    serviceInstanceDown(sElem, instance, node);
                    vis.run("repaint");
                }
            } else {
                if (instance != null && instance.getServiceBeanConfig() != null) {
                    System.err.println("!!! serviceDown(): " +
                                       "["+sElem.getName()+"] " +
                                       "instance ["+
                                       instance.getServiceBeanConfig().getInstanceID()+"] " +
                                       "node is null");
                } else {
                    System.err.println("!!! serviceDown(): ["+sElem.getName()+"] instance [??] node is null");
                }
            }
        }
    }

    /*
     * Indicate that a service instance has been terminated or failed
     */
    private void serviceInstanceDown(final ServiceElement sElem,
                                     final ServiceBeanInstance instance,
                                     final GraphNode node) {
        node.setInstance(null);
        node.setServiceItem(null);
        if(node.getTableNode()!=null) {
            node.getTableNode().set(Constants.STATE, Constants.FAILED);
        } else {
            System.err.println("!!! ["+sElem.getName()+"] " +
                               "instance ["+
                               instance.getServiceBeanConfig().getInstanceID()+"] " +
                               "no TableNode");
        }
    }

    /*
     * Indicate that a service is active
     */
    GraphNode serviceUp(final ServiceElement sElem, final ServiceBeanInstance instance) {
        GraphNode node;
        synchronized(vis) {
            node = getServiceBeanInstance(sElem, instance);
            if(node!=null) {
                node.setInstance(instance);
                if(node.getTableNode()!=null) {
                    node.getTableNode().set(Constants.STATE,
                                            node.getServiceItem()!=null?
                                            Constants.ACTIVE:
                                            Constants.ACTIVE_NO_SERVICE_ITEM);
                }

                vis.run("repaint");
            } else {
                System.err.println("### serviceUp(): " +
                                   "ServiceBeanInstance node null for " +
                                   "["+sElem.getName()+"], instanceID=" +
                                   "["+instance.getServiceBeanConfig().getInstanceID()+"], " +
                                   "create one on the fly");
                vis.cancel("filter");
                GraphNode sElemNode = getServiceElementNode(sElem);
                node = doAddServiceBeanInstance(sElem.getOperationalStringName(),
                                                sElemNode.getTableNode(),
                                                sElemNode.getProvisionMonitor());
                node.setInstance(instance);
                node.setInstanceID(instance.getServiceBeanConfig().getInstanceID());
                if(node.getTableNode()!=null)
                    node.getTableNode().set(Constants.STATE, Constants.ACTIVE);
                vis.run("filter");
            }
        }

        return(node);
    }

    /*
     * Increment a service instance
     */
    void serviceIncrement(final ServiceElement sElem) {
        synchronized(vis) {
            vis.cancel("filter");
            GraphNode sElemNode = getServiceElementNode(sElem);
            GraphUtil.addServiceInstance(g,
                                         sElemNode.getTableNode(),
                                         sElemNode.getOpStringName(),
                                         Constants.AVAILABLE_ID);
            vis.run("filter");
        }
    }

    /*
     * Decrement a service instance
     */
    void serviceDecrement(final ServiceElement sElem, final ServiceBeanInstance instance) {
        GraphNode node;
        synchronized(vis) {
            GraphNode sElemNode = getServiceElementNode(sElem);
            sElemNode.setServiceElement(sElem);
            node = getServiceBeanInstance(sElem, instance);

            if(node!=null) {
                vis.cancel("filter");
                g.removeTuple(node.getTableNode());
                setOpStringState(sElem.getOperationalStringName());
                vis.run("filter");
            } else {
                System.err.println("### serviceDecrement(): " +
                                   "ServiceBeanInstance node null for " +
                                   "["+sElem.getName()+"], instanceID=" +
                                   "["+instance.getServiceBeanConfig().getInstanceID()+"]");
            }
        }
    }

    /*
     * Set the state of the OperationalString
     */
    void setOpStringState(final String name) {
        GraphNode opStringNode = getOpStringNode(name);
        if(opStringNode==null)
            throw new IllegalStateException("Expected to obtain an opstring node for ["+name+"]");
        GraphUtil.setOpStringState(g, opStringNode);
        vis.run("repaint");
    }

    void setGraphNodeServiceItem(final GraphNode node, final ServiceItem item) {
        if(item!=null && node !=null) {
            node.setServiceItem(item);
            if(node.getTableNode()!=null) {
                node.getTableNode().set(Constants.STATE, Constants.ACTIVE);
                setOpStringState(node.opStringName);
            }
        }
    }

    /*
     * Get the GraphNode for a ServiceBeanInstance
     */
    GraphNode getServiceBeanInstance(final ServiceElement sElem, final ServiceBeanInstance instance) {
        GraphNode instanceNode = null;
        GraphNode sElemNode = getServiceElementNode(sElem);
        if(sElemNode!=null) {
            instanceNode = getServiceBeanInstance(sElemNode, instance);
        }
        return(instanceNode);
    }

    /*
     * Get the GraphNode for a ServiceBeanInstance
     */
    GraphNode getServiceBeanInstance(final GraphNode sElemNode, final ServiceBeanInstance instance) {
        GraphNode instanceNode = null;
        GraphNode[] instances = new GraphNode[0];
        if(sElemNode.getCollapsedVertex()!=null) {
            GraphListener.CollapsedServiceElement elem =
                (GraphListener.CollapsedServiceElement)
                    sElemNode.getCollapsedVertex();
            instances = elem.getInstances();
        } else {
            if(sElemNode.getTableNode()!=null)
                instances = GraphUtil.getChildren(g, sElemNode.getTableNode());
        }

        for(GraphNode node : instances) {
            if(instance != null &&
               node.isServiceInstance() &&
               instance.getServiceBeanConfig() != null &&
               instance.getServiceBeanConfig().getInstanceID() == node.instanceID) {
                instanceNode = node;
                break;
            }
        }
        /* Look for available nodes */
        if(instanceNode==null) {
            for(GraphNode node : instances) {
                if(node.getInstanceID()==Constants.AVAILABLE_ID &&
                   node.isServiceInstance() &&
                   instance != null) {
                    node.setInstanceID(instance.getServiceBeanConfig().getInstanceID());
                    node.setInstance(instance);
                    instanceNode = node;
                    break;
                }
            }
        }
        return(instanceNode);
    }

    /*
     * Get the GraphNode for a ServiceBeanInstance
     */
    GraphNode getServiceBeanInstance(final GraphNode sElemNode, final Uuid uuid) {
        GraphNode instanceNode = null;
        GraphNode[] instances = new GraphNode[0];
        if(sElemNode.getCollapsedVertex()!=null) {
            GraphListener.CollapsedServiceElement elem =
                (GraphListener.CollapsedServiceElement)
                    sElemNode.getCollapsedVertex();
            instances = elem.getInstances();
        } else {
            if(sElemNode.getTableNode()!=null)
                instances = GraphUtil.getChildren(g, sElemNode.getTableNode());
        }

        for(GraphNode node : instances) {
            if(node.getInstance()==null)
                continue;
            if(uuid.equals(node.getInstance().getServiceBeanID())) {
                instanceNode = node;
                break;
            }
        }
        return(instanceNode);
    }

    /*
     * Get the TableNode for a ServiceElement
     */
    GraphNode getServiceElementNode(final ServiceElement sElem) {
        GraphNode sElemNode = null;
        GraphNode opStringNode = getOpStringNode(sElem.getOperationalStringName());
        if(opStringNode!=null) {
            sElemNode = getServiceElementNode(opStringNode, sElem);
        } else {
            System.err.println("### getServiceElementNode: OpString node null for ["+sElem.getOperationalStringName()+"]");
        }
        return(sElemNode);
    }

    /*
     * Get the TableNode for a ServiceElement
     */
    GraphNode getServiceElementNode(final GraphNode opStringNode, final ServiceElement sElem) {
        GraphNode sElemNode = null;
        GraphNode[] nodes;
        if(opStringNode.isCollapsed()) {
            ArrayList<GraphNode> list = new ArrayList<GraphNode>();
            GraphListener.CollapsedOpString collapsed =
                (GraphListener.CollapsedOpString)opStringNode.getCollapsedVertex();
            GraphListener.CollapsedServiceElement[] elems = collapsed.getInstances();
            for(GraphListener.CollapsedServiceElement elem : elems) {
                list.add(elem.getServiceElementNode());
            }
            nodes = list.toArray(new GraphNode[list.size()]);
        } else {
            nodes = GraphUtil.getChildren(g, opStringNode.getTableNode());
        }

        for(GraphNode node : nodes) {
            if(node.getServiceElement().getName().equals(sElem.getName())) {
                sElemNode = node;
                break;
            }
        }

        return(sElemNode);
    }

    /*
     * Get an OperationalString GraphNode
     */
    GraphNode getOpStringNode(final String name) {
        GraphNode opStringNode = null;
        for(Iterator it=g.neighbors(root); it.hasNext();) {
            Tuple t = (Tuple)it.next();
            Object o = t.get(Constants.USER_OBJECT);
            if(o!=null && o instanceof GraphNode) {
                GraphNode node = (GraphNode)o;
                if(node.getOpString()!=null &&
                   node.getOpStringName().equals(name)) {
                    opStringNode = node;
                    break;
                }
            }
        }
        return(opStringNode);
    }

    /*
     * Remove a ServiceElement's instances
     */
    int removeServiceInstances(final GraphNode node) {
        ArrayList<GraphNode> list = new ArrayList<GraphNode>();
        for (Iterator it = g.outNeighbors(node.getTableNode()); it.hasNext();) {
            Tuple t = (Tuple) it.next();
            Object o = t.get(Constants.USER_OBJECT);
            if (o != null && o instanceof GraphNode) {
                GraphNode g = (GraphNode) o;
                if (node.isServiceElement() && g.isOpString())
                    continue;
                list.add(g);
            }
        }

        GraphNode[] gNodes = list.toArray(new GraphNode[list.size()]);
        for (GraphNode gNode : gNodes) {
            g.removeTuple(gNode.getTableNode());
        }

        return gNodes.length;
    }

    /**
     * Switch the root of the tree by requesting a new spanning tree at the
     * desired root
     */
    class TreeRootAction extends GroupAction {
        public TreeRootAction(final String graphGroup) {
            super(graphGroup);
        }

        public void run(final double frac) {
            TupleSet focus = vis.getGroup(Visualization.FOCUS_ITEMS);
            if (focus == null || focus.getTupleCount() == 0)
                return;            
            Graph g = (Graph) vis.getGroup(m_group);
            Node f = null;
            Iterator tuples = focus.tuples();
            while (tuples.hasNext() &&
                   !g.containsTuple(f=(Node)tuples.next())) {
                f = null;
            }
            if (f == null) {
                return;
            }
            Object uo = f.get(Constants.USER_OBJECT);
            if(uo!=null && uo instanceof GraphNode) {
                GraphNode node = (GraphNode)uo;
                if(node.isServiceInstance())
                    return;
            }
            g.getSpanningTree(f);

        }
    }

    /**
     * Set node label colors
     */
    class TextColorAction extends ColorAction {
        public TextColorAction(final String group) {
            super(group, VisualItem.TEXTCOLOR, ColorLib.gray(0));
            add("_hover", ColorLib.rgb(255, 250, 250));
        }
    }

    /**
     * Set node label labels
     */
    class MyLabelRenderer extends LabelRenderer {
          public String getText(final VisualItem vi) {
              if(vi.getRow()==-1)
                  return("");
              Object o = vi.get(Constants.USER_OBJECT);
              String name="";
              if(o==null) {
                  name = vi.getString(VisualItem.LABEL);
              } else if(o instanceof GraphNode) {
                  GraphNode node = (GraphNode)o;
                  if(node.getOpString()!=null) {
                      name = node.getOpStringName();
                  } else if(node.getServiceElement()!=null) {
                      name = node.getServiceElement().getName();
                  }
              } else {
                  if(vi.getString(VisualItem.LABEL)==null) {
                      name = o.getClass().getName();
                  } else {
                      name = vi.getString(VisualItem.LABEL);
                      if(name.equals(Constants.ROOT))
                          name = "";
                  }
              }
              /*
              String[] result = name.split("\\s");
              StringBuffer sb = new StringBuffer();
              for(int i=0; i<result.length; i++) {
                  if(i>0)
                      sb.append("\n");
                  sb.append(result[i]);
              }

              return sb.toString();
              */
              return name;
          }
    }

    /**
     * Renders shapes for the graph
     */
    class CustomShapeRenderer extends ShapeRenderer {
         private final Rectangle2D frame = new Rectangle2D.Double();
         private final RoundRectangle2D theRoundRectangle = new RoundRectangle2D.Float();
         private final GeneralPath thePolygon = new GeneralPath();

        protected Shape getRawShape(final VisualItem item) {
            Shape shape;
            if(item.getRow()==-1 || item.getSourceTuple().getRow()==-1) {
                return(null);
            }            
            double x = item.getX();
            if ( Double.isNaN(x) || Double.isInfinite(x) )
                x = 0;
            double y = item.getY();
            if ( Double.isNaN(y) || Double.isInfinite(y) )
                y = 0;
            double width = getBaseSize()*item.getSize();

            // Center the shape around the specified x and y
            if (width > 1 ) {
                x = x-width/2;
                y = y-width/2;
            }
            Object uo = item.get(Constants.USER_OBJECT);
            if(uo!=null) {
                if(uo.equals(Constants.ROOT)) {
                    width = 20*item.getSize();
                    shape = diamond((float)x, (float)y, (float)width);
                } else if(uo instanceof GraphNode) {
                    GraphNode node = (GraphNode)uo;
                    if(node.isServiceInstance()) {
                        width = 20*item.getSize();
                        shape = ellipse(x, y, width, width);
                    } else {
                        String name;
                        if(node.getOpString()!=null) {
                            name = node.getOpStringName();
                        } else {
                            name = node.getServiceElement().getName();
                        }
                        width = DEFAULT_GRAPHICS.getFontMetrics().stringWidth(name);
                        x = item.getX()-width/2;
                        int height = DEFAULT_GRAPHICS.getFontMetrics().getHeight();
                        if(node.isCollapsed()) {
                            shape = getCollapsedShape((float)x,
                                                      (float)y,
                                                      (float)width,
                                                      (float)height);
                        } else {
                            frame.setFrame(x, y, width, height);
                            float arc_size =
                                (float)Math.min(frame.getHeight(), frame.getWidth()) / 2;
                            theRoundRectangle.setRoundRect(frame.getX(),
                                                           frame.getY(),
                                                           frame.getWidth(),
                                                           frame.getHeight(),
                                                           arc_size,
                                                           arc_size);
                            shape = theRoundRectangle;
                        }

                    }
                } else {
                    shape = super.getRawShape(item);
                }
            } else {
                shape = super.getRawShape(item);
            }
            return(shape);
        }

        /*
         * Returns a hexagon shape of the given dimenisions.
         */
        private Shape getCollapsedShape(final float x, final float y, final float width, final float height) {
            thePolygon.reset();
            thePolygon.moveTo(x, y);
            thePolygon.lineTo(x+width, y);
            thePolygon.lineTo(x+1.15f*width, y+0.5f*height);
            thePolygon.lineTo(x+width, y+height);

            thePolygon.lineTo(x, y+height);
            
            thePolygon.closePath();
            return thePolygon;
        }
    }

    /**
     * Set label positions. Labels are assumed to be DecoratorItem instances,
     * decorating their respective nodes. The layout simply gets the bounds
     * of the decorated node and assigns the label coordinates to the center
     * of those bounds.
     */
    class LabelPositionLayout extends Layout {
        public LabelPositionLayout(final String group) {
            super(group);
        }
        public void run(final double frac) {
            Iterator iter = vis.items(m_group);
            while (iter.hasNext() ) {
                DecoratorItem decorator = (DecoratorItem)iter.next();
                if(decorator.getRow()==-1)
                    continue;
                try {
                    if(decorator.isValid()) {
                        VisualItem decoratedItem = decorator.getDecoratedItem();
                        Rectangle2D bounds = decoratedItem.getBounds();
                        double x = bounds.getCenterX();
                        double y = bounds.getCenterY();
                        setX(decorator, null, x);
                        setY(decorator, null, y);
                    }
                } catch(IllegalArgumentException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static class MultiLineToolTip extends JToolTip {
        public MultiLineToolTip() {
            setUI(new MultiLineToolTipUI());
        }
    }

    public static class MultiLineToolTipUI extends BasicToolTipUI {
        private String[] strs;

        public void paint(final Graphics g, final JComponent c) {
            FontMetrics metrics = g.getFontMetrics();
            Dimension size = c.getSize();
            g.setColor(c.getBackground());
            g.fillRect(0, 0, size.width, size.height);
            g.setColor(c.getForeground());
            if (strs != null) {
                for (int i = 0; i < strs.length; i++) {
                    g.drawString(strs[i], 3, (metrics.getHeight()) * (i + 1));
                }
            }
        }

        public Dimension getPreferredSize(final JComponent c) {
            FontMetrics metrics = c.getFontMetrics(c.getFont());
            String tipText = ((JToolTip) c).getTipText();
            if (tipText == null) {
                tipText = "";
            }
            BufferedReader br = new BufferedReader(new StringReader(tipText));
            String line;
            int maxWidth = 0;
            Vector<String> v = new Vector<String>();
            try {
                while ((line = br.readLine()) != null) {
                    int width = SwingUtilities.computeStringWidth(metrics,
                                                                  line);
                    maxWidth = (maxWidth < width) ? width : maxWidth;
                    v.addElement(line);
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            int lines = v.size();
            if (lines < 1) {
                strs = null;
                lines = 1;
            } else {
                strs = new String[lines];
                int i = 0;
                for (Enumeration e = v.elements(); e.hasMoreElements(); i++) {
                    strs[i] = (String) e.nextElement();
                }
            }
            int height = metrics.getHeight() * lines;
            return new Dimension(maxWidth + 6, height + 4);
        }
    }
}
