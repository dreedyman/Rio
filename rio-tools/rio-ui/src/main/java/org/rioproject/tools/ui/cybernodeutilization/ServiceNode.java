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
package org.rioproject.tools.ui.cybernodeutilization;

import net.jini.id.Uuid;
import org.jdesktop.swingx.treetable.AbstractMutableTreeTableNode;
import org.rioproject.opstring.ServiceElement;
import org.rioproject.deploy.ServiceRecord;
import org.rioproject.system.ComputeResourceUtilization;

/**
 * Simple data structure for a contained service node
 *
 * @author Dennis Reedy
 */
public class ServiceNode extends AbstractMutableTreeTableNode implements CRUNode {
    private final String name;
    private final Uuid uuid;
    private final ServiceElement sElem;
    private ComputeResourceUtilization cru;
    private final ColumnHelper columnHelper;

    public ServiceNode(final ServiceRecord record, final ColumnHelper columnHelper) {
        this.name = record.getServiceElement().getName();
        uuid = record.getServiceID();
        sElem = record.getServiceElement();
        this.columnHelper = columnHelper;
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

    public void setComputeResourceUtilization(final ComputeResourceUtilization cru) {
        this.cru = cru;
    }

    @Override
    public Object getValueAt(final int column) {
        String value = null;
        if(column==0) {
            value = getName();
        } else {
            if(getComputeResourceUtilization()!=null) {
                ComputeResourceUtilization cru = getComputeResourceUtilization();
                value = columnHelper.getColumnValue(column, cru, false);
            }
        }
        return value;
    }

    @Override
    public int getColumnCount() {
        return columnHelper.getColumnCount();
    }
}
