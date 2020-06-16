/*
 * Copyright 2008 the original author or authors.
 * Copyright 2005 Sun Microsystems, Inc.
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
package org.rioproject.servicebean;

import org.rioproject.opstring.ServiceElement;

/**
 * The ServiceElementChangeListener gets notified that the ServiceElement has been 
 * modified
 *
 * @author Dennis Reedy
 */
public interface ServiceElementChangeListener {
    /**
     * A ServiceElementChangeListener is notified if the ServiceElement has changed.
     * Details on what has changed in the ServiceElement are undefined
     * 
     * @param preElem The ServiceElement before the change
     * @param postElem The ServiceElement after the change
     */
    void changed(ServiceElement preElem, ServiceElement postElem);
}
