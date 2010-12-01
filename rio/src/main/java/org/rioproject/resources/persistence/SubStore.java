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

import java.io.File;
import java.io.IOException;

/**
 * Interface components must meet if they implement their own persistent store
 */
public interface SubStore {
    /**
     * If this components what's its own sub-directory it should return
     * a non-<code>null</code> string that will be its sub-directory's name.
     * If it does not need its own sub-directory this method should return
     * <code>null</code>
     */
    public String subDirectory();

    /**
     * Gives the <code>SubStore</code> a piece of the file system to 
     * use for its store.
     * @param dir the directory to use
     * @throws IOException if there is a problem initializing it's store
     *         or recovering its state.
     */
    public void setDirectory(File dir) throws IOException;

    /**
     * Informs the <code>SubStore</code> that the service is being destroyed
     * and it should perform any necessary cleanup (closing files for example).
     * The store does not need to delete it's data.
     */
    public void prepareDestroy();
}
