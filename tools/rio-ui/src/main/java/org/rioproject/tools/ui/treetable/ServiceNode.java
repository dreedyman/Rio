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
package org.rioproject.tools.ui.treetable;

import net.jini.id.Uuid;
import org.rioproject.core.ServiceElement;
import org.rioproject.core.provision.ServiceRecord;
import org.rioproject.system.ComputeResourceUtilization;

import javax.swing.tree.DefaultMutableTreeNode;

/**
 * Simple data structure for a contained service node
 *
 * @author Dennis Reedy
 */
public class ServiceNode extends DefaultMutableTreeNode
    implements CRUNode {
    String name;
    Uuid uuid;
    ServiceElement sElem;
    ComputeResourceUtilization cru;

    public ServiceNode(ServiceRecord record) {
        this.name = record.getServiceElement().getName();
        uuid = record.getServiceID();
        sElem = record.getServiceElement();
    }

    public Uuid getUuid() {
        return uuid;
    }

    public boolean isLeaf() {
        return true;
    }

    public String toString() {
        return name;
    }

    @Override
    public boolean getAllowsChildren() {
        return false;
    }

    public ServiceElement getServiceElement() {
        return sElem;
    }

    public String getName() {
        return name;
    }

    public boolean isForked() {
        return (sElem.forkService() || sElem.getExecDescriptor()!=null);
    }

    public ComputeResourceUtilization getComputeResourceUtilization() {
        return cru;
    }

    public void setComputeResourceUtilization(ComputeResourceUtilization cru) {
        this.cru = cru;
    }
}
