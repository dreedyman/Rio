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

/**
 * An artifact in the form of: groupId:artifactId[:classifier]:version
 */
public class Artifact {
    String artifactId;
    String groupId;
    String version;
    String classifier;
    File jar;
    File pom;
    URL pomURL;
    String checkSumPolicy;
    boolean loadFromProject = false;
    RemoteRepository remoteRepository;
    
    public Artifact() {
    }

    public Artifact(String groupId, String artifactId, String version) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
    }

    public Artifact(String groupId, String artifactId, String version, String classifier) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
        this.classifier = classifier;
    }

    public Artifact(String artifact) {
        if(artifact==null)
            throw new IllegalArgumentException("artifact is null");
        String[] parts = artifact.split(":");
        if(parts.length<3 )
            throw new IllegalArgumentException("artifact must be in the form of "+
                                               "groupId:artifactId[:classifier]:version");
        boolean haveClassifier = parts.length > 3;
        groupId = parts[0];
        artifactId = parts[1];
        version = "";
        if (haveClassifier) {
            classifier = parts[2];
            version = parts[3];
        } else {
            if (parts.length > 2)
                version = parts[2];
        }
    }

    public String getArtifactId() {
        return artifactId;
    }

    public String getGroupId() {
        return groupId;
    }

    public String getVersion() {
        return version;
    }

    public String getClassifier() {
        return classifier;
    }

    public String getFileName(String ext) {
        String name;
        boolean useClassifier = !(ext.equals("pom") || ext.equals("oar"));
        if(pomURL==null || loadFromProject) {
            if(useClassifier && classifier!=null)
                name = artifactId+"-"+version+"-"+classifier;
            else
                name = artifactId+"-"+version;
        } else {
            String s = pomURL.toExternalForm();
            int ndx = s.lastIndexOf("/");
            s = s.substring(ndx+1);
            ndx = s.lastIndexOf(".");
            s = s.substring(0, ndx);
            if(useClassifier && classifier!=null)
                name = s+"-"+classifier;
            else
                name = s;
        }
        return name+"."+ext;
    }

    public boolean isSnapshot() {
        return version==null?false:version.contains("SNAPSHOT");
    }

    public String getGAV() {
        String gav;
        if(classifier==null)
            gav = groupId+":"+artifactId+":"+version;
        else
            gav = groupId+":"+artifactId+":"+classifier+":"+version;
        return gav;
    }

    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("Artifact");
        sb.append("{ groupId='").append(groupId);
        sb.append(", artifactId='").append(artifactId);
        sb.append(", version='").append(version);
        sb.append(", classifier='").append(classifier);
        sb.append(" }");
        return sb.toString();
    }

        /**
     * Check to see if the provided string represents an Artifact
     *
     * @param s The string to check
     *
     * @return true if the string is an artifact, false otherwise
     * @throws IllegalArgumentException is the provided string is null
     */
    public static boolean isArtifact(String s) {
        if(s==null)
            throw new IllegalArgumentException("supplied argument cannot be null");
        boolean isArtifact = true;
        try {
            new Artifact(s);
        } catch (IllegalArgumentException e) {
            isArtifact = false;
        }
        return isArtifact;
    }
}
