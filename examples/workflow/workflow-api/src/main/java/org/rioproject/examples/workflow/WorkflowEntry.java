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
package org.rioproject.examples.workflow;

import net.jini.core.entry.Entry;

import java.util.Date;
import java.util.UUID;

public class WorkflowEntry implements Entry {
    private static final long serialVersionUID = 1L;
    public State state;
    public UUID id;
    public String value;

    public WorkflowEntry() {
    }

    public WorkflowEntry(State state) {
        this.state = state;
    }

    public WorkflowEntry(UUID id, State state) {
        this(state);
        this.id = id;
    }

    public Entry execute() {
        state = state.next();
        if(state.equals(State.CLOSED)) {
            value = "Closed at ["+new Date(System.currentTimeMillis())+"]";
        }
        return(this);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("WorkflowEntry");
        sb.append(" [state=").append(state);
        sb.append(']');
        return sb.toString();
    }
}
