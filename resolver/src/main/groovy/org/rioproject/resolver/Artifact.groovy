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
package org.rioproject.resolver

/**
 * An artifact in the form of: groupId:artifactId[:classifier]:version
 */
class Artifact {
    String artifactId
    String groupId
    String version
    String classifier
    File jar
    File pom
    URL pomURL
    String checkSumPolicy
    boolean loadFromProject = false
    RemoteRepository remoteRepository
    
    def Artifact() {
    }

    def Artifact(String groupId, String artifactId, String version) {
        this.groupId = groupId
        this.artifactId = artifactId
        this.version = version
    }

    def Artifact(String groupId, String artifactId, String version, String classifier) {
        this.groupId = groupId
        this.artifactId = artifactId
        this.version = version
        this.classifier = classifier
    }

    def Artifact(String artifact) {
        if(artifact==null)
            throw new IllegalArgumentException("artifact is null")
        String[] parts = artifact.split(":")
        if(parts.length<3 )
            throw new IllegalArgumentException("artifact must be in the form of "+
                                               "groupId:artifactId[:classifier]:version")
        boolean haveClassifer = parts.length > 3
        groupId = parts[0]
        artifactId = parts[1]
        version = ""
        if (haveClassifer) {
            classifier = parts[2]
            version = parts[3]
        } else {
            if (parts.length > 2)
                version = parts[2]
        }
    }

    public URL getJarURL() {
        URL u = null
        if(jar!=null && jar.exists()) {
            u = jar.toURI().toURL()
        } else if(pomURL!=null) {
            if(loadFromProject) {
                File target = new File(new File(pomURL.toURI()).parentFile, "target")
                File jarFile = new File(target, getFileName("jar"))
                if(jarFile.exists()) {
                    u = jarFile.toURI().toURL()
                }
            } else {
                String s = getLocation()+(jar==null?getFileName("jar"):jar.name)
                u = new URL(s)
            }
        }
        return u
    }

    public String getPath(String sep, String ext) {
        StringBuilder path = new StringBuilder()
        path.append(groupId.replace('.', sep))
        path.append(sep)
        path.append(artifactId)
        path.append(sep)
        path.append(version)
        path.append(sep)
        path.append(getFileName(ext))
        return path.toString()
    }

    private String getLocation() {
        String location = null
        if(pomURL!=null) {
            String s = pomURL.toExternalForm()
            int ndx = s.lastIndexOf("/")
            location = s.substring(0, ndx+1)
        }
        return location
    }

    public String getFileName(String ext) {
        String name
        boolean useClassifier = !(ext.equals("pom") || ext.equals("oar"))
        if(pomURL==null || loadFromProject) {
            if(useClassifier && classifier!=null)
                name = artifactId+"-"+version+"-"+classifier
            else
                name = artifactId+"-"+version
        } else {
            String s = pomURL.toExternalForm()
            int ndx = s.lastIndexOf("/")
            s = s.substring(ndx+1)
            ndx = s.lastIndexOf(".")
            s = s.substring(0, ndx)
            if(useClassifier && classifier!=null)
                name = s+"-"+classifier
            else
                name = s
        }
        return name+"."+ext
    }

    public boolean isSnapshot() {
        return version==null?false:version.contains("SNAPSHOT")
    }

    public String getGAV() {
        def gav
        if(classifier==null)
            gav = "${groupId}:${artifactId}:${version}"
        else
            gav = "${groupId}:${artifactId}:${classifier}:${version}"
        return gav as String
    }

    public String toString() {
        final StringBuilder sb = new StringBuilder()
        sb.append("Artifact")
        sb.append("{ groupId='").append(groupId)
        sb.append(", artifactId='").append(artifactId)
        sb.append(", version='").append(version)
        sb.append(", classifier='").append(classifier)
        sb.append(' }') ;
        return sb.toString()
    }
}
