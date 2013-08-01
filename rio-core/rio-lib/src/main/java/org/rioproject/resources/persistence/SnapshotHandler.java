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
package org.rioproject.resources.persistence;

import java.io.IOException;

/**
 * Interface a server needs to implement in order to work with a PersistentStore to 
 * determine whether a snapshot should be taken
 */
public interface SnapshotHandler {
    /**
     * Invoked to trigger persistent state be written and committed to a recoverable 
     * resource<br>
     *
     * @throws IOException if an error occurs
     */
    public void takeSnapshot() throws IOException;

    /**
     * Called by <code>PersistentStore</code> after every update to give
     * server a chance to trigger a snapshot<br>
     *
     * @param updateCount Number of updates since last snapshot
     */
    public void updatePerformed(int updateCount);
}
