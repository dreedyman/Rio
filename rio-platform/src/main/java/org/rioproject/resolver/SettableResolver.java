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
package org.rioproject.resolver;

import java.io.File;
import java.util.Collection;

/**
 * A {@link org.rioproject.resolver.Resolver} that enables it's {@link org.rioproject.resolver.RemoteRepository}
 * instances to be set.
 *
 * @author Dennis Reedy
 */
public interface SettableResolver {

    /**
     * Set the {@link RemoteRepository} instances the Resolver should use
     *
     * @param repositories The {@link RemoteRepository} instances the Resolver should use.
     *                     May be {@code null}.
     *
     * @return An updated instance of the {@code SettableResolver}
     */
    SettableResolver setRemoteRepositories(Collection<RemoteRepository> repositories);

    /**
     * This adds repositories which look into one or more directories for finding dependencies. These
     * directories will be searched last. Note that this does not support any meta-data formats found
     * in Maven POM files.
     *
     * @param directories Directory instances the Resolver should use for finding dependencies.
     *                     May be {@code null}.
     *
     * @return An updated instance of the {@code SettableResolver}
     */
    SettableResolver setFlatDirectories(Collection<File> directories);
}
