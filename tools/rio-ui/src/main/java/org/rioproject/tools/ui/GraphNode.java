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
import org.rioproject.monitor.ProvisionMonitor;
import org.rioproject.opstring.OperationalString;
import org.rioproject.opstring.ServiceElement;
import net.jini.core.lookup.ServiceItem;
import prefuse.data.tuple.TableNode;

/**
 * Container class representing vertices on the graph
 *
 * @author Dennis Reedy
 */
class GraphNode {
    ProvisionMonitor monitor;
    OperationalString opString;
    ServiceElement serviceElement;
    ServiceBeanInstance instance;
    ServiceItem item;
    String opStringName;
    long instanceID;
    boolean isRoot;
    TableNode tableNode;
    boolean collapsed;
    Object collapsedVertex;

    GraphNode() {
        isRoot = true;
    }

    GraphNode(ProvisionMonitor monitor,
              OperationalString opString,
              TableNode tableNode) {
        this.opString = opString;
        this.monitor = monitor;
        this.tableNode = tableNode;
    }

    GraphNode(ServiceElement serviceElement, TableNode tableNode) {
        this.serviceElement = serviceElement;
        this.tableNode = tableNode;
    }

    GraphNode(long instanceID, String opStringName, TableNode tableNode) {
        this.instanceID = instanceID;
        this.opStringName = opStringName;
        this.tableNode = tableNode;
    }

    public TableNode getTableNode() {
        return tableNode;
    }

    public void setTableNode(TableNode tableNode) {
        this.tableNode = tableNode;
    }

    public void setOpString(OperationalString opString) {
        this.opString = opString;
    }

    public OperationalString getOpString() {
        return opString;
    }

    public String getOpStringName() {
        if(opStringName == null) {
            if(opString!=null)
                opStringName = opString.getName();
            else if(serviceElement!=null)
                opStringName = serviceElement.getOperationalStringName();
        }
        return opStringName;
    }

    public void setServiceElement(ServiceElement serviceElement) {
        this.serviceElement = serviceElement;
    }

    public ServiceElement getServiceElement() {
        return serviceElement;
    }

    public boolean isRoot() {
        return(isRoot);
    }

    public boolean isOpString() {
        return(!(opString==null));
    }

    public boolean isServiceElement() {
        return(!(serviceElement==null));
    }

    public boolean isServiceInstance() {
        return(opString==null && serviceElement==null && !isRoot);
    }

    public long getInstanceID() {
        return instanceID;
    }

     public void setInstanceID(long instanceID) {
        this.instanceID = instanceID;
    }

    public ProvisionMonitor getProvisionMonitor() {
        return monitor;
    }

    public void setProvisionMonitor(ProvisionMonitor monitor) {
        this.monitor = monitor;
    }

    public ServiceBeanInstance getInstance() {
        return instance;
    }

    public void setInstance(ServiceBeanInstance instance) {
        this.instance = instance;
    }

    public ServiceItem getServiceItem() {
        return item;
    }

    public void setServiceItem(ServiceItem item) {
        this.item = item;
    }

    public void setCollapsed(boolean collapsed) {
        this.collapsed = collapsed;
    }

    public boolean isCollapsed() {
        return(collapsed);
    }

    public void setCollapsedVertex(Object collapsedVertex) {
        this.collapsedVertex = collapsedVertex;
    }

    public Object getCollapsedVertex() {
        return collapsedVertex;
    }

    public String toString() {
        String s;
        if(opString!=null)
            s = opString.getName();
        else if(serviceElement!=null)
            s = serviceElement.getName();
        else
            s = "instanceID="+instanceID;
        return s;
    }
}
