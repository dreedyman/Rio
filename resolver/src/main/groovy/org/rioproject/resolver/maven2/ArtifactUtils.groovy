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
package org.rioproject.resolver.maven2

import org.rioproject.resolver.Artifact
import org.rioproject.resolver.RemoteRepository
import java.util.logging.Logger
import java.util.logging.Level

/**
 * Some utilities for working with the artifact and URLs.
 */
class ArtifactUtils {
    static Logger logger = Logger.getLogger(ArtifactUtils.class.getName());
    Map<String, List<String>> failedLookups = new HashMap<String, List<String>>()

    public URL getArtifactPomURLFromRepository(Artifact artifact,
                                               File localRepo,
                                               List<RemoteRepository> remoteRepositories)
    throws MalformedURLException {
        return getArtifactURLFromRepository(artifact, "pom", localRepo, remoteRepositories)
    }

    public URL getArtifactURLFromRepository(Artifact artifact,
                                            String ext,
                                            File localRepo,
                                            List<RemoteRepository> remoteRepositories)
    throws MalformedURLException {
        URL artifactURL
        File localArtifact
        if(artifact.loadFromProject) {
            localArtifact = getLocalFile(artifact, (File)localRepo, ext)
        } else {
            localArtifact = new File((File)localRepo, artifact.getPath(File.separator,
                                                                       ext))
        }
        if(logger.isLoggable(Level.FINE))
            logger.fine "artifact: ${artifact.getGAV()}, "+
                        "localArtifact=${localArtifact.absolutePath}, "+
                        "exists? ${localArtifact.exists()}, "+
                        "loadFromProject? ${artifact.loadFromProject}"
        if (!localArtifact.exists()) {
            if(artifact.isSnapshot()) {
                String snapShotPath = buildArtifactSnapshotPath(artifact, ext, remoteRepositories)
                if(logger.isLoggable(Level.FINE))
                    logger.fine "snapShotPath: $snapShotPath"
                boolean notfound = false
                if(snapShotPath==null) {
                    notfound = true
                } else {
                    URL u = new URL(snapShotPath)
                    if(test(u)) {
                        artifactURL = u
                    } else {
                        notfound = true
                    }
                }
                if(notfound) {
                    System.err.println "[WARNING] snapshot ${artifact.getGAV()} (${ext}) not found"
                }
            } else {
                String path = artifact.getPath('/', ext)
                for (RemoteRepository repo: remoteRepositories) {
                    if(!repo.supportsReleases())
                        continue
                    if(!repo.url.endsWith("/"))
                        repo.url = repo.url+"/"
                    //println "===> path: ${path}, repo=${repo.url}, failedLookups=${failedLookups}"
                    if(failedLookups.get(path)!=null) {
                        List<String> repos = failedLookups.get(path)
                        if(repos.contains(repo.url))
                            continue
                    }
                    URL u = new URL(repo.url+path)
                    if(test(u)) {
                        artifactURL = u
                        artifact.checkSumPolicy = repo.releaseChecksumPolicy
                        break
                    } else {
                        System.err.println "[WARNING] ${artifact.getGAV()} (${ext}) not found on repository ${repo.url}"
                        List<String> repos = failedLookups.get(path)
                        if(repos==null)
                            repos = new ArrayList<String>()
                        if(!repos.contains(repo.url))
                            repos.add(repo.url)
                        failedLookups.put(path, repos)
                    }
                }
            }
        } else {
            artifactURL = localArtifact.toURI().toURL();
        }
        if(logger.isLoggable(Level.FINE))
            logger.fine "artifact: ${artifact.getGAV()} artifactURL: $artifactURL"
        return artifactURL
    }
    
    private String buildArtifactSnapshotPath(Artifact artifact,
                                             String ext,
                                             List<RemoteRepository> remoteRepositories) {
        String snapshotPath = null
        StringBuilder path = new StringBuilder()
        path.append(artifact.groupId.replace('.', "/"))
        path.append("/")
        path.append(artifact.artifactId)
        path.append("/")
        path.append(artifact.version)
        path.append("/")
        int ndx = artifact.version.indexOf("SNAPSHOT")
        String versionNoSnapshot = artifact.version.substring(0, ndx)
        String prefix = artifact.artifactId+"-"+versionNoSnapshot
        URL u = getSnapshotMetaData(artifact, path.toString()+"maven-metadata.xml", remoteRepositories)
        if(u!=null) {
            InputStream is = u.openStream()
            if(is==null)
                return null
            XmlSlurper parser = new XmlSlurper()
            def metadata = parser.parseText(u.text)
            String timestamp = ""
            String buildNumber = ""
            metadata.versioning.snapshot.each { snapshot ->
                timestamp = snapshot.timestamp
                buildNumber = snapshot.buildNumber
            }
            ndx = u.toExternalForm().indexOf("maven-metadata.xml")
            boolean useClassifier = !(ext.equals("pom") || ext.equals("oar"))
            if(useClassifier && artifact.classifier!=null)
                snapshotPath =  u.toExternalForm().substring(0, ndx)+
                                prefix+timestamp+"-"+buildNumber+"-${artifact.classifier}."+ext
            else
                snapshotPath =  u.toExternalForm().substring(0, ndx)+
                                prefix+timestamp+"-"+buildNumber+"."+ext
        }
        return snapshotPath
    }

    private URL getSnapshotMetaData(Artifact a, String path, List<RemoteRepository> remoteRepositories) {
        URL metaDataURL = null
        for (RemoteRepository repo: remoteRepositories) {
            if(!repo.supportsSnapshots())
                continue
            if(!repo.url.endsWith("/"))
                repo.url = repo.url+"/"
            URL u = new URL(repo.url+path)
            if(test(u)) {
                metaDataURL = u
                a.checkSumPolicy = repo.snapshotChecksumPolicy
                break
            } else {
                System.err.println "[WARNING] snapshot $path not found on repository ${repo.url}"
            }
        }
        return metaDataURL
    }

    private boolean test(URL u) {
        boolean okay = false
        try {
            u.openStream()
            okay = true
        } catch(Exception e) {
        }
        return okay
    }

    public static File getLocalFile(Artifact a, File repoRoot, String ext) {
        String path = null
        if(repoRoot!=null)
            path = repoRoot.absolutePath
        return getLocalFile(a, (String)path, ext)
    }

    public static File getLocalFile(Artifact a, String repoRoot, String ext) {
        File localFile
        if(a.loadFromProject) {
            File projectPom = new File(a.pomURL.toURI())
            if(ext=="pom") {
                localFile = projectPom
            } else {
                String s
                if(ext.equals("oar")) {
                    s = "${a.artifactId}-${a.version}.$ext"
                } else {
                    if(a.classifier!=null)
                        s = "${a.artifactId}-${a.version}-${a.classifier}.$ext"
                    else
                        s = "${a.artifactId}-${a.version}.$ext"
                }
                File targetDir = new File(projectPom.parentFile, "target")
                localFile = new File(targetDir, s)
            }
        } else {
            String gid = a.groupId.replace('.', File.separator)
            String jar = repoRoot+"/"+gid+"/"+a.artifactId+"/"+a.version+"/"+a.getFileName(ext)
            localFile = new File(jar)
        }
        if(logger.isLoggable(Level.FINE))
            logger.fine "artifact=${a.getGAV()}, "+
                        "fromProject? ${a.loadFromProject}, "+
                        "extension: $ext, "+
                        "local file=${localFile.absolutePath}, exists? ${localFile.exists()}"
        return localFile
    }

}
