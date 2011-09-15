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
package org.rioproject.tools.maven;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.artifact.metadata.ArtifactMetadata;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.OverConstrainedVersionException;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.plugin.testing.stubs.MavenProjectStub;
import org.codehaus.plexus.util.ReaderFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * The mock project needed for mojo unit tests.
 */
public class ProjectStub extends MavenProjectStub {
    private final List<Artifact> dependencies = new ArrayList<Artifact>();
    private final List repositories = new ArrayList();

    public ProjectStub() {
        MavenXpp3Reader pomReader = new MavenXpp3Reader();
        Model model;
        File pom = new File(getBasedir(), "src/test/resources/test-project/pom.xml");
        try {
            model = pomReader.read(ReaderFactory.newXmlReader(pom));
            setModel(model);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        setGroupId(model.getGroupId());
        setArtifactId(model.getArtifactId());
        setVersion(model.getVersion());
        setName(model.getName());
        setUrl(model.getUrl());
        setPackaging(model.getPackaging());
        setRemoteArtifactRepositories(model.getRepositories());
        for(Object o : model.getDependencies()) {
            Dependency dep = (Dependency)o;
            Artifact a = new ArtifactMock(dep.getGroupId(), dep.getArtifactId(), dep.getVersion(), dep.getType());
            a.setFile(pom);
            dependencies.add(a);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void setRemoteArtifactRepositories(List list) {
        repositories.addAll(list);
    }

    @Override
    public List getRemoteArtifactRepositories() {
        return repositories;
    }

    @Override
    public List getDependencies() {
        return dependencies;
    }

    static class ArtifactMock implements Artifact {
        private String groupId;
        private String artifactId;
        private String version;
        private String type;
        private File file;
        private String baseVersion;

        ArtifactMock(String groupId, String artifactId, String version, String type) {
            this.groupId = groupId;
            this.artifactId = artifactId;
            this.version = version;
            this.type = type;
        }

        public String getGroupId() {
            return groupId;
        }

        public String getArtifactId() {
            return artifactId;
        }

        public String getVersion() {
            return version;
        }

        public void setVersion(String string) {
            this.version = string;
        }

        public String getScope() {
            return null;
        }

        public String getType() {
            return type;
        }

        public String getClassifier() {
            return null;
        }

        public boolean hasClassifier() {
            return false;  
        }

        public File getFile() {
            return file;
        }

        public void setFile(File file) {
            this.file = file;
        }

        public String getBaseVersion() {
            return baseVersion;
        }

        public void setBaseVersion(String s) {
            this.baseVersion = s;
        }

        public String getId() {
            return null;
        }

        public String getDependencyConflictId() {
            return null;
        }

        public void addMetadata(ArtifactMetadata artifactMetadata) {
        }

        public Collection getMetadataList() {
            return null;
        }

        public void setRepository(ArtifactRepository artifactRepository) {
        }

        public ArtifactRepository getRepository() {
            return null;
        }

        public void updateVersion(String string, ArtifactRepository artifactRepository) {            
        }

        public String getDownloadUrl() {
            return null; 
        }

        public void setDownloadUrl(String string) {
        }

        public ArtifactFilter getDependencyFilter() {
            return null;
        }

        public void setDependencyFilter(ArtifactFilter artifactFilter) {
        }

        public ArtifactHandler getArtifactHandler() {
            return null;
        }

        public List getDependencyTrail() {
            return null; 
        }

        public void setDependencyTrail(List list) {
        }

        public void setScope(String string) {
        }

        public VersionRange getVersionRange() {
            return null;  
        }

        public void setVersionRange(VersionRange versionRange) {
        }

        public void selectVersion(String string) {
        }

        public boolean isSnapshot() {
            return false;  
        }

        public void setResolved(boolean b) {
        }

        public boolean isResolved() {
            return false;  
        }

        public void setResolvedVersion(String string) {
        }

        public void setArtifactHandler(ArtifactHandler artifactHandler) {
        }

        public boolean isRelease() {
            return false;  
        }

        public void setRelease(boolean b) {
        }

        public List getAvailableVersions() {
            return null;  
        }

        public void setAvailableVersions(List list) {
        }

        public boolean isOptional() {
            return false;
        }

        public void setOptional(boolean b) {
        }

        public ArtifactVersion getSelectedVersion() throws OverConstrainedVersionException {
            return null;  
        }

        public boolean isSelectedVersionKnown() throws OverConstrainedVersionException {
            return false;
        }

        public void setGroupId(String groupId) {
            this.groupId = groupId;
        }

        public void setArtifactId(String artifactId) {
            this.artifactId = artifactId;
        }

        public int compareTo(Object o) {
            return 0;  
        }
    }
}
