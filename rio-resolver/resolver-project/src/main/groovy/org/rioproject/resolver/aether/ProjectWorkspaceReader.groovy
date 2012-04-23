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

import org.apache.maven.settings.building.SettingsBuildingException
import org.rioproject.resolver.aether.util.SettingsUtil
import org.sonatype.aether.artifact.Artifact
import org.sonatype.aether.repository.WorkspaceReader
import org.sonatype.aether.repository.WorkspaceRepository
import org.apache.maven.model.Model
import org.apache.maven.model.io.xpp3.MavenXpp3Reader

/**
 * A {@code WorkspaceReader} that reads the current project's pom and artifact.
 *
 * @author Dennis Reedy
 */
public class ProjectWorkspaceReader implements WorkspaceReader {
    private final WorkspaceRepository workspaceRepository = new WorkspaceRepository();
    private final String localRepositoryLocation;
    private final String projectArtifactName
    private final File pomFile
    private final File artifactFile

    public ProjectWorkspaceReader() throws SettingsBuildingException {
        localRepositoryLocation = SettingsUtil.getLocalRepositoryLocation(SettingsUtil.getSettings())
        File pomFile = new File(System.getProperty("user.dir"), "pom.xml")
        if(pomFile.exists()) {
            this.pomFile = pomFile
            FileReader pomReader = new FileReader(pomFile)
            Model model = new MavenXpp3Reader().read(pomReader)
            String version = model.version
            if(version==null) {
                version = model.parent.version
            }
            projectArtifactName = "${model.artifactId}-${version}"
            pomReader.close()
            File target = new File(System.getProperty("user.dir"), "target")
            if(target.exists()) {
                File f = new File(target, "${projectArtifactName}.jar")
                if(f.exists()) {
                    artifactFile = f
                }
            }
            pomReader.close()
        }
    }

    public WorkspaceRepository getRepository() {
        return workspaceRepository;
    }

    public File findArtifact(Artifact artifact) {
        String fileName = String.format("%s-%s.%s", artifact.getArtifactId(), artifact.getVersion(), artifact.getExtension());
        String artifactName = String.format("%s-%s", artifact.getArtifactId(), artifact.getVersion());
        if(projectArtifactName.equals(artifactName)) {
            if("pom".equals(artifact.extension) && pomFile!=null) {
                return pomFile;
            }
            if(artifactFile.getName().equals(fileName) && artifactFile!=null) {
                return artifactFile;
            }
        }
        File artifactFile = new File(localRepositoryLocation, getArtifactPath(artifact));
        if(artifactFile.exists())
            return artifactFile;
        return null;
    }

    public List<String> findVersions(Artifact artifact) {
        return new ArrayList<String>();
    }

    private String getFileName(Artifact a) {
        org.rioproject.resolver.Artifact artifact = new org.rioproject.resolver.Artifact(a.getGroupId(),
                                                                                         a.getArtifactId(),
                                                                                         a.getVersion(),
                                                                                         a.getExtension(),
                                                                                         a.getClassifier());
        return artifact.getFileName();
    }

    private String getArtifactPath(final Artifact a) {
        String sep = File.separator;
        StringBuilder path = new StringBuilder();
        path.append(a.getGroupId().replace('.', sep));
        path.append(sep);
        path.append(a.getArtifactId());
        path.append(sep);
        path.append(a.getVersion());
        path.append(sep);
        path.append(getFileName(a));
        return path.toString();
    }
}
