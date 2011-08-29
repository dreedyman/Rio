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
package org.rioproject.monitor.persistence;

import org.rioproject.opstring.OperationalString;

import java.io.Serializable;

/**
 * The RecordHolder class is a simple container that holds an OperationalString
 * and an action indicating whether the OperationalString was modified or
 * removed. The RecordHolder object will be used to restore the state of the
 * ProvisionMonitor
 *
 * @author Dennis Reedy
 */
public class RecordHolder implements Serializable {
    static final long serialVersionUID = 1L;
    public static final int MODIFIED = 0;
    public static final int REMOVED = 1;
    private int action;
    private OperationalString opstring;

    public RecordHolder(OperationalString opstring, int action) {
        this.opstring = opstring;
        this.action = action;
    }

    public OperationalString getOperationalString() {
        return (opstring);
    }

    public int getAction() {
        return (action);
    }
}
