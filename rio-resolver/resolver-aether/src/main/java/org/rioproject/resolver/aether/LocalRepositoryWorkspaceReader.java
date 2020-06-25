/*
 * Copyright to the original author or authors.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *         http://www.apache.org/licenses/LICENSE-2.0
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
import org.eclipse.aether.repository.WorkspaceReader;
import org.eclipse.aether.repository.WorkspaceRepository;
import org.rioproject.resolver.aether.util.SettingsUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * A {@code WorkspaceReader} that checks if the artifact is in the local repository, and if configured,
 * available in any flat directories.
 *
 * @author Dennis Reedy
 */
public class LocalRepositoryWorkspaceReader implements WorkspaceReader {
    private final WorkspaceRepository workspaceRepository = new WorkspaceRepository();
    private final String localRepositoryDir;
    private static final Logger logger = LoggerFactory.getLogger(LocalRepositoryWorkspaceReader.class);

    public LocalRepositoryWorkspaceReader() throws SettingsBuildingException {
        localRepositoryDir = SettingsUtil.getLocalRepositoryLocation(SettingsUtil.getSettings());
    }

    public WorkspaceRepository getRepository() {
        return workspaceRepository;
    }

    public File findArtifact(Artifact artifact) {
        if(!artifact.isSnapshot()) {
            File artifactFile = new File(localRepositoryDir, getArtifactPath(artifact));
            if(artifactFile.exists())
                return artifactFile;
        } else {
            logger.info("Skip {}", artifact.toString());
        }
        return null;
    }

    protected String getLocalRepositoryDir() {
        return localRepositoryDir;
    }

    public List<String> findVersions(Artifact artifact) {
        List<String> versions = new ArrayList<String>();
        String artifactRoot = String.format("%s/%s",
                                            artifact.getGroupId().replace('.', File.separatorChar),
                                            artifact.getArtifactId());
        File artifactDir = new File(localRepositoryDir, artifactRoot);
        if(artifactDir.exists()) {
            for (File f : artifactDir.listFiles()) {
                if (f.isDirectory())
                    versions.add(f.getName());
            }
        }
        return versions;
    }

    protected String getArtifactPath(final Artifact a) {
        String sep = File.separator;
        StringBuilder path = new StringBuilder();
        path.append(a.getGroupId().replace('.', File.separatorChar));
        path.append(sep);
        path.append(a.getArtifactId());
        path.append(sep);
        path.append(a.getVersion());
        path.append(sep);
        path.append(getFileName(a));
        return path.toString();
    }

    String getFileName(Artifact a) {
        return new org.rioproject.resolver.Artifact(a.getGroupId(),
                                                    a.getArtifactId(),
                                                    a.getVersion(),
                                                    a.getExtension(),
                                                    a.getClassifier()).getFileName();
    }
}
