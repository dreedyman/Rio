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
package org.rioproject.impl.fdh;

import net.jini.core.entry.Entry;
import net.jini.lookup.entry.Name;

/**
 * @author Dennis Reedy
 */
public final class NameHelper {
    private NameHelper() {}

    /**
     * Get the first Name.name from the attribute collection set
     *
     * @param attrs Array of Entry objects
     *
     * @return The the first Name.name from the attribute collection set or
     * null if not found
     */
    static String getName(Entry[] attrs) {
        for (Entry attr : attrs) {
            if (attr instanceof Name) {
                return (((Name) attr).name);
            }
        }
        return "unknown";
    }
}
