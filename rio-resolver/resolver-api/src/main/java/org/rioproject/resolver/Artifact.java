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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * An artifact in the form of: groupId:artifactId[:type[:classifier]]:version.
 *
 * @author Dennis Reedy
 */
public class Artifact {
    private String artifactId;
    private String groupId;
    private String version;
    private String classifier;
    private String type;

    public Artifact() {
    }

    public Artifact(String groupId, String artifactId, String version) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
        this.classifier = "";
    }

    public Artifact(String groupId, String artifactId, String version, String type, String classifier) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
        this.type = type;
        this.classifier = classifier==null?"":classifier;
    }

    public Artifact(String artifact) {
        if(artifact==null)
            throw new IllegalArgumentException("artifact is null");
        Pattern p = Pattern.compile("([^: /\\\\]+):([^: /\\\\]+)(:([^: /\\\\]*)(:([^: /\\\\]+))?)?:([^: /\\\\]+)" );
        Matcher m = p.matcher( artifact );
        if (!m.matches() ) {
            throw new IllegalArgumentException( "Bad artifact coordinates " + artifact
                + ", expected format is <groupId>:<artifactId>[:<type>[:<classifier>]]:<version>" );
        }
        groupId = m.group(1);
        artifactId = m.group(2);
        type = get(m.group(4), "jar");
        classifier = get( m.group(6), "" );
        version = m.group(7);
    }

    private static String get( String value, String defaultValue ) {
        return ( value == null || value.length() <= 0 ) ? defaultValue : value;
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

    public String getType() {
        return type;
    }

    public String getFileName() {
        return getFileName(type);
    }

    public String getFileName(String ext) {
        String name;
        if(classifier.length()>0) {
            System.out.println("classifier length: "+classifier.length());
            name = String.format("%s-%s-%s.%s", artifactId, version, classifier, ext);
        } else {
            name = String.format("%s-%s.%s", artifactId, version, ext);
        }
        return name;
    }

    public String getGAV() {
        String gav;
        if(classifier==null || classifier.length()==0) {
            gav = String.format("%s:%s:%s:%s", groupId, artifactId, type, version);
        } else {
            gav = String.format("%s:%s:%s:%s:%s", groupId, artifactId, type, classifier, version);
        }
        return gav;
    }

    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("Artifact");
        sb.append("{ groupId='").append(groupId);
        sb.append(", artifactId='").append(artifactId);
        sb.append(", version='").append(version);
        sb.append(", type='").append(type);
        sb.append(", classifier='").append(classifier);
        sb.append(" }");
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        Artifact artifact = (Artifact) o;

        if (artifactId != null ? !artifactId.equals(artifact.artifactId) : artifact.artifactId != null) return false;
        if (classifier != null ? !classifier.equals(artifact.classifier) : artifact.classifier != null) return false;
        if (groupId != null ? !groupId.equals(artifact.groupId) : artifact.groupId != null) return false;
        if (type != null ? !type.equals(artifact.type) : artifact.type != null) return false;
        if (version != null ? !version.equals(artifact.version) : artifact.version != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = artifactId != null ? artifactId.hashCode() : 0;
        result = 31 * result + (groupId != null ? groupId.hashCode() : 0);
        result = 31 * result + (version != null ? version.hashCode() : 0);
        result = 31 * result + (classifier != null ? classifier.hashCode() : 0);
        result = 31 * result + (type != null ? type.hashCode() : 0);
        return result;
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
