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
package org.rioproject.entry;

import net.jini.entry.AbstractEntry;
import net.jini.lookup.entry.ServiceControlled;

/**
 * Provides information for a service version.
 *
 * @author Dennis Reedy
 */
public class VersionEntry extends AbstractEntry implements ServiceControlled {
    private static final long serialVersionUID = 1l;
    public String version;

    public VersionEntry() {
        super();
    }

    public VersionEntry(final String version) {
        super();
        this.version = version;
    }

    @Override
    public String toString() {
        return String.format("VersionEntry version=%s", version);
    }
}
