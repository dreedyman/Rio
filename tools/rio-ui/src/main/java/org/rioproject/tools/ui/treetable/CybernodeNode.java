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

import net.jini.core.entry.Entry;
import net.jini.core.lookup.ServiceItem;
import org.rioproject.cybernode.Cybernode;
import org.rioproject.cybernode.CybernodeAdmin;
import org.rioproject.entry.ApplianceInfo;
import org.rioproject.system.ComputeResourceUtilization;

import javax.swing.tree.DefaultMutableTreeNode;

/**
 * Simple data structure for holding cybernode proxy and compute resource
 * utilization
 *
 * @author Dennis Reedy
 */
public class CybernodeNode extends DefaultMutableTreeNode
    implements CRUNode {
    ServiceItem item;
    Cybernode cybernode;
    ComputeResourceUtilization cru;
    String hostName;
    CybernodeAdmin admin;

    public CybernodeNode(ServiceItem item,
                        CybernodeAdmin admin,
                        ComputeResourceUtilization cru) {
        super(item);
        this.item = item;
        this.cybernode = (Cybernode) item.service;
        this.admin = admin;
        this.cru = cru;
    }

    public CybernodeAdmin getAdmin() {
        return admin;
    }

    public String getHostName() {
        if (hostName == null) {
            if (cru == null) {
                for (Entry e : item.attributeSets) {
                    if (e instanceof ApplianceInfo) {
                        hostName = ((ApplianceInfo) e).hostName;
                        break;
                    }
                }
            } else {
                hostName = cru.getHostName();
            }
        }
        return (hostName);
    }

    public String toString() {
        return getHostName();
    }

    public boolean isLeaf() {
        return false;
    }

    @Override
    public boolean getAllowsChildren() {
        return true;
    }

    public void setComputeResourceUtilization(ComputeResourceUtilization cru) {
        this.cru = cru;
    }

    public ComputeResourceUtilization getComputeResourceUtilization() {
        return cru;
    }

    public ServiceItem getServiceItem() {
        return item;
    }

    public Cybernode getCybernode() {
        return cybernode;
    }
}
