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
package org.rioproject.entry;

import net.jini.lookup.entry.Status;
import net.jini.lookup.entry.StatusType;

/**
 * The BasicStatus entry provides a mechanism for services to communicate their
 * status to interested entities. If important state conditions occur during a
 * service's operation, interested entities (such as administrative clients) can
 * register for notification (from lookup services) of changes to or the
 * existence of this attribute
 *
 * @author Dennis Reedy
 */
public class BasicStatus extends Status {
    static final long serialVersionUID = 1L;

    /**
     * Create a BasicStatus
     */
    public BasicStatus() {
        super();
    }

    /**
     * Create a BasicStatus
     * 
     * @param st The StatusType
     */
    public BasicStatus(StatusType st) {
        super(st);
    }
}
