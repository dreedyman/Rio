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
package org.rioproject.watch;

/**
 * An empty implementation of the {@link org.rioproject.watch.Archivable} interface. Produces the effect of
 * having no archival for the {@link org.rioproject.watch.WatchDataSource}.
 */
public class NullArchive implements Archivable {


    public void addCalculable(Calculable calculable) {

    }

    /**
     * @see org.rioproject.watch.Archivable#close
     */
    public void close() {
        // do nothing
    }

    /**
     * @see org.rioproject.watch.Archivable#archive
     */
    public void archive(Calculable calculable) {
        // do nothing
    }
}
