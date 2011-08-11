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

import net.jini.entry.AbstractEntry;

/**
 * The OperationalStringEntry contains the name of the OperationalString the
 * service is a member of
 *
 * @author Dennis Reedy
 */
public class OperationalStringEntry extends AbstractEntry {
    static final long serialVersionUID = 1L;
    public String name;

    /**
     * Construct a OperationalStringEntry
     */
    public OperationalStringEntry() {
        super();
    }

    /**
     * Construct a OperationalStringEntry
     *
     * @param name The name of an OperationalString
     */
    public OperationalStringEntry(String name) {
        this.name = name;
    }
}
