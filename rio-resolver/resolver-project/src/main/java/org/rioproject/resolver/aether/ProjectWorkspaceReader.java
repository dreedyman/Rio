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
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.sonatype.aether.artifact.Artifact;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.logging.*;

/**
 * A {@code WorkspaceReader} that reads the current project's pom and artifact.
 *
 * @author Dennis Reedy
 */
public class ProjectWorkspaceReader extends LocalRepositoryWorkspaceReader {
    private String projectArtifactName;
    private File pomFile;
    private File artifactFile;
    private final Logger logger = Logger.getLogger(ProjectWorkspaceReader.class.getName());

    public ProjectWorkspaceReader() throws SettingsBuildingException, IOException, XmlPullParserException {
        super();
        File pomFile = new File(System.getProperty("user.dir"), "pom.xml");
        if(pomFile.exists()) {
            this.pomFile = pomFile;
            FileReader pomReader = new FileReader(pomFile);
            Model model = new MavenXpp3Reader().read(pomReader);
            String version = model.getVersion();
            if(version==null) {
                version = model.getParent().getVersion();
            }
            projectArtifactName = String.format("%s-%s",model.getArtifactId(), version);
            if(logger.isLoggable(Level.FINE)) {
                logger.fine("Project artifact ${projectArtifactName}");
            }
            pomReader.close();
            File target = new File(System.getProperty("user.dir"), "target");
            if(target.exists()) {
                File f = new File(target, projectArtifactName+".jar");
                if(f.exists()) {
                    artifactFile = f;
                    if(logger.isLoggable(Level.FINE)) {
                        logger.fine(String.format("Project artifact file %s", artifactFile.getName()));
                    }
                }
            }
            pomReader.close();
        } else {
            logger.info(String.format("Pom file not found, working in %s", System.getProperty("user.dir")));
        }
    }

    public File findArtifact(Artifact artifact) {
        String fileName = String.format("%s-%s.%s", artifact.getArtifactId(), artifact.getVersion(), artifact.getExtension());
        String artifactName = String.format("%s-%s", artifact.getArtifactId(), artifact.getVersion());
        if(logger.isLoggable(Level.FINE))
            logger.fine(String.format("find artifact %s", artifactName));
        if(projectArtifactName!=null && projectArtifactName.equals(artifactName)) {
            if("pom".equals(artifact.getExtension()) && pomFile!=null) {
                if(logger.isLoggable(Level.FINE)) {
                    logger.fine(String.format("Return project pom %s", pomFile.getName()));
                }
                return pomFile;
            }
            if( artifactFile!=null && artifactFile.getName().equals(fileName)) {
                if(logger.isLoggable(Level.FINE)) {
                    logger.fine(String.format("Return project artifact %s", artifactFile.getName()));
                }
                return artifactFile;
            }
        }
        File artifactFile = new File(getLocalRepositoryLocation(), getArtifactPath(artifact));
        if(artifactFile.exists())
            return artifactFile;
        return null;
    }

}
