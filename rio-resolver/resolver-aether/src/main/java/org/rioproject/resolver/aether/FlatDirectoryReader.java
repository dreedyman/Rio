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
package org.rioproject.resolver.aether;

import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.resolution.ArtifactResult;

import java.io.File;
import java.util.Collection;

/**
 * Manages flat directory based repositories
 *
 * @author Dennis Reedy
 */
public interface FlatDirectoryReader {
    /**
     * Add flat directories
     *
     * @param directories Directories to add
     */
    void addDirectories(Collection<File> directories);

    /**
     * Get flat directories
     *
     * @return Directories
     */
    Collection<File> getDirectories();

    /**
     * Find an artifact in flat directories.
     *
     * @param artifact The artifact to find.
     *
     * @return The resolved artifact or null.
     */
    ArtifactResult findArtifact(Artifact artifact);
}
