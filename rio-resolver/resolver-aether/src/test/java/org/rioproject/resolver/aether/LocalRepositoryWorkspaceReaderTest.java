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

import org.apache.maven.settings.building.SettingsBuildingException;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.junit.Test;

import java.io.File;

import static org.junit.Assert.*;

/**
 * Some testing of LocalRepositoryWorkspaceReader
 */
public class LocalRepositoryWorkspaceReaderTest {
    @Test
    public void testLocalRepositoryWorkspaceReader() throws SettingsBuildingException {
        LocalRepositoryWorkspaceReader workspaceReader = new LocalRepositoryWorkspaceReader();
        Artifact a = new DefaultArtifact("something.something:darkside-deathstar:pom:2.1");
        String path = workspaceReader.getArtifactPath(a);
        System.out.println(path);
        assertEquals(getArtifactPath(a, workspaceReader), path);
    }

    private String getArtifactPath(final Artifact a, LocalRepositoryWorkspaceReader workspaceReader) {
        String sep = File.separator;
        StringBuilder path = new StringBuilder();
        path.append(a.getGroupId().replace('.', File.separatorChar));
        path.append(sep);
        path.append(a.getArtifactId());
        path.append(sep);
        path.append(a.getVersion());
        path.append(sep);
        path.append(workspaceReader.getFileName(a));
        return path.toString();
    }

}