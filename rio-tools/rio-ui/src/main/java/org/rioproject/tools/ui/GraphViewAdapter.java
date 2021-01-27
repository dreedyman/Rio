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

import net.jini.core.lookup.ServiceItem;
import net.jini.id.Uuid;
import org.rioproject.opstring.ServiceElement;

import javax.swing.*;

/**
 * Use as an interchange between the Cybernode tree table and the graph to
 * set service selection
 *
 * @author Dennis Reedy
 */
public class GraphViewAdapter {
    private Main main;

    public GraphViewAdapter(Main main) {
        if(main == null)
            throw new IllegalArgumentException("main cannot be null");
        this.main = main;
    }

    public ServiceItem getServiceItem(ServiceElement sElem, Uuid uuid) {
        GraphNode sElemNode = main.getGraphView().getServiceElementNode(sElem);
        if(sElemNode==null) {
            return null;
        }
        GraphNode serviceNode = main.getGraphView().getServiceBeanInstance(sElemNode, uuid);
        if(serviceNode==null) {
            return null;
        }
        return serviceNode.getServiceItem();
    }

    public void refreshCybernodeTable() {
        main.refreshCybernodeTable();
    }

    public JFrame getMain() {
        return main;
    }
}
