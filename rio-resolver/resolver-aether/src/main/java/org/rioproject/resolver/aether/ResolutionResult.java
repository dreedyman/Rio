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

import org.sonatype.aether.artifact.Artifact;
import org.sonatype.aether.resolution.ArtifactResult;

import java.util.List;

/**
 * The result of resolving dependencies for an artifact
 */
public class ResolutionResult {
    private Artifact artifact;
    private List<ArtifactResult> artifactResults;

    public ResolutionResult(Artifact artifact, List<ArtifactResult> artifactResults) {
        this.artifact = artifact;
        this.artifactResults = artifactResults;
    }

    public void setArtifact(Artifact artifact) {
        this.artifact = artifact;
    }

    public Artifact getArtifact() {
        return artifact;
    }

    public List<ArtifactResult> getArtifactResults() {
        return artifactResults;
    }
}
