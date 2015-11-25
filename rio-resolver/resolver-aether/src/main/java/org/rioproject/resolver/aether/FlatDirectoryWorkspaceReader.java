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
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.repository.WorkspaceRepository;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResult;
import org.rioproject.resolver.aether.util.DefaultPomGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Dennis Reedy
 */
public class FlatDirectoryWorkspaceReader implements FlatDirectoryReader {
    private final File localPomGenerationDir;
    private final Set<File> directories = new HashSet<File>();
    private static final Logger logger = LoggerFactory.getLogger(FlatDirectoryWorkspaceReader.class);

    public FlatDirectoryWorkspaceReader() {
        localPomGenerationDir = new File(String.format("%s/.rio/generated/poms",
                                                       System.getProperty("user.home").replace('\\', '/')));
    }

    @Override public void addDirectories(Collection<File> directories) {
        if(directories!=null) {
            this.directories.addAll(directories);
        }
    }

    @Override public ArtifactResult findArtifact(Artifact artifact) {
        File f = findArtifactInFlatDirs(artifact);
        if(f!=null) {
            Artifact resolved = artifact.setFile(f);
            ArtifactResult result = new ArtifactResult(new ArtifactRequest(resolved, null, null));
            result.setArtifact(resolved);
            result.setRepository(new WorkspaceRepository("flat-dir"));
            return result;
        }
        return null;
    }

    File findArtifactInFlatDirs(Artifact artifact) {
        if (logger.isDebugEnabled())
            logger.debug("Look in flat dirs for {}", artifact.toString());
        File artifactFile = null;
        if(!directories.isEmpty()) {
            String fileName = getFileName(artifact);
            if (logger.isDebugEnabled())
                logger.debug("Resolve {}, file {}", artifact.toString(), fileName);
            for (File directory : directories) {
                File file = new File(directory, fileName);
                if (logger.isDebugEnabled())
                    logger.debug("Check {}, exists? {}", file.getPath(), file.exists());
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
                    File jarFile = findArtifactInFlatDirs(jar);
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
        } else {
            logger.debug("No directories found");
        }
        return artifactFile;
    }

    String getPomPathAndName(Artifact a) {
        return String.format("%s/%s", DefaultPomGenerator.getGenerationPath(a), getFileName(a));
    }

    String getFileName(Artifact a) {
        return new org.rioproject.resolver.Artifact(a.getGroupId(),
                                                    a.getArtifactId(),
                                                    a.getVersion(),
                                                    a.getExtension(),
                                                    a.getClassifier()).getFileName();
    }
}
