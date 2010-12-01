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
import java.net.URL;
import java.util.Collection;
import java.util.List;

/**
 * Interface for an entity that resolves an artifact. The artifact must be in
 * the form of:
 *
 * <br><br>
 * <tt>groupId:artifactId[:classifier]:version</tt>
 *
 * <p>The <tt>Resolver</tt> will also download and install the resolved
 * artifact(s) and their dependencies (including transitive dependencies)
 * if instructed to.
 */
public interface Resolver {

    /**
     * Get the classpath for an artifact, and resolve (download) the artifact
     * and it's dependencies if needed.
     *
     * @param artifact The artifact string.
     * @param pom The artifact pom
     * @param download If true, download and install the resolved elements
     *
     * @return A string array containing the classpath elements for the artifact
     */
    String[] getClassPathFor(String artifact, File pom, boolean download);

    /**
     * Get the classpath for an artifact, and resolve (download) the artifact
     * and it's dependencies if needed.
     *
     * @param artifact The artifact string.
     * @param repositories Array of repositories to use to resolve the artifact
     * @param download If true, download and install the resolved elements
     *
     * @return A string array containing the classpath elements for the artifact
     */
    String[] getClassPathFor(String artifact, RemoteRepository[] repositories, boolean download);

    /**
     * Get the location of the artifact
     *
     * @param artifact The artifact string.
     * @param artifactType The type of artifact. Typically either "jar" or
     * "oar". If null (or empty string), "jar" is used. This is used to
     * determine the filename extension of the artifact to locate. 
     * @param pom The artifact pom
     *
     * @return The location of the artifact, or null if not found.
     */
    URL getLocation(String artifact, String artifactType,  File pom);

    /**
     * Get the @{link RemoteRepository} instances the Resolver is using
     *
     * @return The @{link RemoteRepository} instances the Resolver is using. If
     * there are no instances, return an empty collection.
     */
    Collection<RemoteRepository> getRemoteRepositories();
}
