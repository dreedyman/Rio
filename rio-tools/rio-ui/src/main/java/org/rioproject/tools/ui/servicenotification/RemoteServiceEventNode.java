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
package org.rioproject.tools.ui.servicenotification;

import org.jdesktop.swingx.treetable.AbstractMutableTreeTableNode;
import org.rioproject.event.RemoteServiceEvent;

import java.util.Date;

/**
 * Container that holds information about a remote event
 */
@SuppressWarnings("PMD.EmptyMethodInAbstractClassShouldBeAbstract")
public abstract class RemoteServiceEventNode <T extends RemoteServiceEvent> extends AbstractMutableTreeTableNode {
    private T event;

    public RemoteServiceEventNode(T event) {
        super();
        this.event = event;
    }

    public Date getDate() {
        return event.getDate();
    }

    public abstract Throwable getThrown();

    public abstract String getDescription();

    public abstract String getOperationalStringName();
    
    public abstract String getServiceName();

    public T getEvent() {
        return event;
    }

    @Override
    public boolean getAllowsChildren() {
        return false;
    }

    @Override
    public boolean isLeaf() {
        return true;
    }
}
