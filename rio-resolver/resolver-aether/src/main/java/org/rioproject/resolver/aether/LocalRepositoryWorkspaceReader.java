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
import org.eclipse.aether.repository.WorkspaceReader;
import org.eclipse.aether.repository.WorkspaceRepository;
import org.rioproject.resolver.aether.util.DefaultPomGenerator;
import org.rioproject.resolver.aether.util.SettingsUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.*;

/**
 * A {@code WorkspaceReader} that checks if the artifact is in the local repository, and if configured,
 * available in any flat directories.
 *
 * @author Dennis Reedy
 */
public class LocalRepositoryWorkspaceReader implements WorkspaceReader, FlatDirectoryReader {
    private final WorkspaceRepository workspaceRepository = new WorkspaceRepository();
    private final String localRepositoryDir;
    private final File localPomGenerationDir;
    private final Set<File> directories = new HashSet<File>();
    private static final Logger logger = LoggerFactory.getLogger(LocalRepositoryWorkspaceReader.class);

    public LocalRepositoryWorkspaceReader() throws SettingsBuildingException {
        localRepositoryDir = SettingsUtil.getLocalRepositoryLocation(SettingsUtil.getSettings());
        localPomGenerationDir = new File(String.format("%s/.rio/generated/poms",
                                                       System.getProperty("user.home").replace('\\', '/')));
    }

    public WorkspaceRepository getRepository() {
        return workspaceRepository;
    }

    public void addDirectories(Collection<File> directories) {
        if(directories!=null) {
            this.directories.addAll(directories);
        }
    }

    public File findArtifact(Artifact artifact) {
        if(!artifact.isSnapshot()) {
            File artifactFile = new File(localRepositoryDir, getArtifactPath(artifact));
            if(artifactFile.exists())
                return artifactFile;
            return findArtifactInFlatDirectories(artifact);
        }
        return null;
    }

    protected String getLocalRepositoryDir() {
        return localRepositoryDir;
    }

    public List<String> findVersions(Artifact artifact) {
        return new ArrayList<String>();
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

    File findArtifactInFlatDirectories(Artifact artifact) {
        File artifactFile = null;
        if(!directories.isEmpty()) {
            String fileName = getFileName(artifact);
            if (logger.isDebugEnabled())
                logger.debug("Resolve {}, file {}", artifact.toString(), fileName);
            for (File directory : directories) {
                File file = new File(directory, fileName);
                if (file.exists()) {
                    if (logger.isDebugEnabled())
                        logger.debug("Resolved {}, file {}", artifact.toString(), file.getPath());
                    artifactFile = file;
                    break;
                }
            }

            if (artifactFile == null) {
                if(artifact.getExtension().equals("pom")) {
                    File pomFile = new File(localPomGenerationDir, getPomPathAndName(artifact));
                    if(pomFile.exists())
                        return pomFile;
                    Artifact jar = new DefaultArtifact(artifact.getGroupId(), artifact.getArtifactId(), "jar", artifact.getVersion());
                    File jarFile = findArtifactInFlatDirectories(jar);
                    if(jarFile!=null) {
                        if (logger.isDebugEnabled())
                            logger.debug("Generating pom for {}, location: {}", artifact, pomFile.getPath());
                        if(!pomFile.getParentFile().exists()) {
                            if(pomFile.getParentFile().mkdirs())  {
                                logger.debug("Created {}", pomFile.getParentFile().getPath());
                            }
                        }
                        try {
                            DefaultPomGenerator.writeTo(pomFile, artifact);
                            return pomFile;
                        } catch (IOException ex) {
                            logger.error("Failed to generate {}", pomFile.getPath(), ex);
                        }
                    }
                }
                if (logger.isWarnEnabled()) {
                    StringBuilder b = new StringBuilder();
                    for (File d : directories) {
                        if (b.length() > 0)
                            b.append("\n");
                        b.append("\t").append(d.getPath());
                    }
                    logger.warn("Could not resolve {} using the following flat directories\n{}",
                                artifact.toString(), b.toString());
                }
            }
        }
        return artifactFile;
    }

    String getFileName(Artifact a) {
        return new org.rioproject.resolver.Artifact(a.getGroupId(),
                                                    a.getArtifactId(),
                                                    a.getVersion(),
                                                    a.getExtension(),
                                                    a.getClassifier()).getFileName();
    }

    String getPomPathAndName(Artifact a) {
        return String.format("%s/%s", DefaultPomGenerator.getGenerationPath(a), getFileName(a));
    }

}
