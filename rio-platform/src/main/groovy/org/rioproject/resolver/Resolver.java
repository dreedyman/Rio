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

/**
 * Interface for an entity that resolves an artifact. The artifact must be in
 * the form of:
 *
 * <br><br>
 * <tt>groupId:artifactId[:classifier]:version</tt>
 *
 */
public interface Resolver {

    /**
     * Get the classpath for an artifact.
     *
     * @param artifact The artifact string.
     * @param pom The pom to use (may be null)
     * @param download Whether to download the artifact
     *
     * @return A string array containing the classpath elements for the artifact
     *
     * @throws ResolverException if the artifact cannot be resolved, or if the artifact's
     * dependencies cannot be found
     */
    @Deprecated
    String[] getClassPathFor(String artifact, File pom, boolean download) throws ResolverException;

    /**
     * Get the classpath for an artifact, and resolve (download) the artifact
     * and it's dependencies if needed.
     *
     * @param artifact The artifact string.
     *
     * @return A string array containing the classpath elements for the artifact
     *
     * @throws ResolverException if the artifact cannot be resolved, or if the artifact's
     * dependencies cannot be found
     */
    String[] getClassPathFor(String artifact) throws ResolverException;

    /**
     * Get the classpath for an artifact, and resolve (download) the artifact
     * and it's dependencies if needed.
     *
     * @param artifact The artifact string.
     * @param repositories Array of repositories to use to resolve the artifact
     *
     * @return A string array containing the classpath elements for the artifact
     *
     * @throws ResolverException if the artifact cannot be resolved, or if the artifact's
     * dependencies cannot be found
     */
    String[] getClassPathFor(String artifact, RemoteRepository[] repositories) throws ResolverException;

    /**
     * Get the location of the artifact
     *
     * @param artifact The artifact string.
     * @param artifactType The type of artifact. Typically either "jar" or
     * "oar". If null (or empty string), "jar" is used. This is used to
     * determine the filename extension of the artifact to locate.
     *
     * @return The location of the artifact.
     *
     * @throws ResolverException if the artifact cannot be located
     */
    URL getLocation(String artifact, String artifactType) throws ResolverException;

    /**
     * Get the location of the artifact
     *
     * @param artifact The artifact string.
     * @param artifactType The type of artifact. Typically either "jar" or
     * "oar". If null (or empty string), "jar" is used. This is used to
     * determine the filename extension of the artifact to locate.
     * @param repositories Array of repositories to use to resolve the artifact
     *
     * @return The location of the artifact.
     *
     * @throws ResolverException if the artifact cannot be located
     */
    URL getLocation(String artifact, String artifactType, RemoteRepository[] repositories) throws ResolverException;

    /**
     * Get the @{link RemoteRepository} instances the Resolver is using
     *
     * @return The @{link RemoteRepository} instances the Resolver is using. If
     * there are no instances, return an empty collection.
     */
    Collection<RemoteRepository> getRemoteRepositories();
}
