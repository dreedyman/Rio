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
package org.rioproject.test

import org.rioproject.resolver.aether.AetherResolver
import org.rioproject.resolver.Artifact
import org.rioproject.resolver.RemoteRepository
import org.sonatype.aether.util.artifact.DefaultArtifact
import org.apache.maven.model.Model
import org.apache.maven.model.io.xpp3.MavenXpp3Reader
import org.apache.maven.model.Parent
import org.rioproject.resources.util.PropertyHelper
import org.rioproject.test.maven2.PomUtils
import org.rioproject.resolver.aether.ResolutionResult
import org.sonatype.aether.resolution.ArtifactResult
import org.sonatype.aether.resolution.ArtifactRequest

/**
 * Resolves artifacts from within (or among) a project (module)
 */
class ProjectModuleResolver extends AetherResolver {

    def ProjectModuleResolver() {
        super()
    }
    
    def URL getLocation(String a, String type) {
        File pom = new File(System.getProperty("user.dir"), "pom.xml")
        Artifact artifact = new Artifact(a)
        Map<String, File> map = getModuleMap(pom, artifact)
        File target = map.get(artifact.getGAV())
        URL u = null
        if(target!=null) {
            File f =  new File(target, artifact.getFileName(type))
            if(f!=null && f.exists())
                u = f.toURI().toURL()
        } else {
            u = super.getLocation(a, type)
        }
        return u
    }

   def String[] getClassPathFor(String s, RemoteRepository[] remote) {
        List<String> classPath = getProjectArtifacts(s, transformRemoteRepository(remote))
        return classPath.toArray(new String[classPath.size()]);
    }

    def String[] getClassPathFor(String artifactCoordinates, File pom) {
        List<String> classPath = getProjectArtifacts(artifactCoordinates, null)
        return classPath.toArray(new String[classPath.size()]);
    }

    private List<String> getProjectArtifacts(String artifactCoordinates,
                                             List<org.sonatype.aether.repository.RemoteRepository> repositories) {
        Artifact art = new Artifact(artifactCoordinates);
        DefaultArtifact artifact = new DefaultArtifact(art.groupId, art.artifactId, art.classifier, "jar", art.version);

        File target = new File(System.getProperty("user.dir"), "target")
        File localRepositoryJar = getLocalRepositoryFile(art)
        File projectArtifactJar = getProjectFile(target, art)

        ResolutionResult result = new ResolutionResult(artifact, null, new ArrayList<ArtifactResult>())
        /* Check project artifact first */
        if(projectArtifactJar.exists()) {
            artifact = (DefaultArtifact)artifact.setFile(projectArtifactJar)
            ArtifactResult artifactResult = new ArtifactResult(new ArtifactRequest(artifact, null, null))
            result.artifactResults << artifactResult.setArtifact(artifact)
            result.artifact =  artifact
            File pomFile = new File(System.getProperty("user.dir"), "pom.xml");
            if(pomFile.exists()) {
                FileReader pomReader = null;
                try {
                    pomReader = new FileReader(pomFile);
                    Model model = new MavenXpp3Reader().read(pomReader);

                    for(org.apache.maven.model.Dependency d : model.getDependencies()) {
                        if(d.groupId.equals("org.rioproject") &&
                           (d.artifactId.equals("rio") || d.artifactId.equals("rio-test")))
                            continue
                        String version = d.version
                        if(version==null)
                            version = resolveVersionUsingLocalMetaData(d)
                        version = interpolateVersionIfNeeded(artifact.version, model, version)
                        appendArtifactResults(result, doResolve(d.groupId,
                                                                d.artifactId,
                                                                d.type,
                                                                d.classifier,
                                                                version,
                                                                repositories).artifactResults)
                    }

                } catch (Exception e) {
                    e.printStackTrace()
                    System.err.println("[WARNING] Unable to resolve $artifactCoordinates "+
                                       "while processing pom. "+e.getLocalizedMessage())
                } finally {
                    if(pomReader!=null) {
                        try {
                            pomReader.close();
                        } catch (IOException e) {
                            System.err.println("[WARNING] Exception closing pom for $artifactCoordinates: "+
                                               e.getLocalizedMessage())
                        }
                    }
                }
            } else {
                System.err.println("[WARNING] Coulnd not locate pom.xml for $artifactCoordinates "+
                                   "Unable to resolve dependencies. Current working directory is "+
                                   System.getProperty("user.dir"))
            }

        /* If not found in the project, check and resolve using the local repository */
        } else  if(localRepositoryJar.exists()) {
            appendArtifactResults(result, doResolve(artifact.groupId,
                                                    artifact.artifactId,
                                                    artifact.extension,
                                                    artifact.classifier,
                                                    artifact.version,
                                                    repositories).artifactResults)

        } else {
            System.err.println("[WARNING] Unable to resolve $artifactCoordinates")
        }
        return produceClassPathFromResolutionResult(result)
    }

    private ResolutionResult doResolve(String groupId,
                                       String artifactId,
                                       String type,
                                       String classifier,
                                       String version,
                                       List<org.sonatype.aether.repository.RemoteRepository> repositories) {
        ResolutionResult result
        if(repositories==null)
            result = aetherService.resolve(groupId, artifactId, type, classifier, version)
        else
            result = aetherService.resolve(groupId, artifactId, type, classifier, version, repositories)
        return result
    }

    private String resolveVersionUsingLocalMetaData(org.apache.maven.model.Dependency d) {
        String version = d.version
        if(version==null) {
            String groupId = d.groupId.replace('.', File.separator)
            String name = "${groupId}${File.separator}${d.artifactId}"
            File dir = new File(aetherService.localRepositoryLocation, name)
            File metaData = new File(dir, "maven-metadata-local.xml")
            if(metaData.exists()) {
                XmlSlurper parser = new XmlSlurper()
                def metadata = parser.parseText(metaData.toURI().toURL().text)
                metadata.versioning.versions.each { v ->
                    version = v
                }
            }
        }
        return version
    }

    private String interpolateVersionIfNeeded(String pomVersion, Model model, String version) {
        if(version.charAt(0) == '$') {
            if (pomVersion == null) {
                Parent parent = model.getParent();
                if (pomVersion == null) {
                    pomVersion = parent.getVersion();
                }
                if (pomVersion == null) {
                    pomVersion = "unknown";
                }
            }
            Properties properties = model.getProperties();
            if(properties.getProperty("project.version")==null)
                properties.setProperty("project.version", pomVersion);
            if(properties.getProperty("project.parent.version")==null)
                properties.setProperty("project.parent.version", pomVersion);
            if(properties.getProperty("pom.version")==null)
                properties.setProperty("pom.version", pomVersion);

            version =  PropertyHelper.expandProperties(version, properties)
        }
        return version
    }

    private File getLocalRepositoryFile(Artifact a) {
        String groupId = a.groupId.replace('.', File.separator)
        String jarName
        if(a.classifier)
            jarName = String.format("%s-%s-%s.jar", a.artifactId, a.version, a.classifier)
        else
            jarName = String.format("%s-%s.jar", a.artifactId, a.version)
        String name = String.format("%s"+File.separator+"%s"+File.separator+"%s"+File.separator+"%s",
                                    groupId,
                                    a.artifactId,
                                    a.version,
                                    jarName)
        return new File(aetherService.localRepositoryLocation, name)
    }

    private File getProjectFile(File target, Artifact a) {
        String jarName = String.format("%s-%s.jar", a.getArtifactId(), a.getVersion());
        return new File(target, jarName);
    }

    private void appendArtifactResults(ResolutionResult result, List<ArtifactResult> artifactResults) {
        for(ArtifactResult ar : artifactResults) {
            boolean add = true
            for(ArtifactResult arr : result.artifactResults) {
                if(arr.artifact.file.equals(ar.artifact.file)) {
                    add = false
                    break
                }
            }
            if(add)
                result.artifactResults << ar
        }
    }


//    protected DependencyFilter getDependencyFilter(Artifact a) {
//        DependencyFilter filter
//        if(a.classifier && a.classifier=="dl") {
//            filter = new ClassifierFilter(a.classifier)
//        } else {
//            filter = new ExclusionFilter(System.getProperty("RIO_TEST_ATTACH")!=null)
//        }
//        return filter
//    }
//
//
//    private File getJar(Artifact a, Map<String, File> map) {
//        File jar = null
//        if(map==null || map.size()==0) {
//            File target = new File(System.getProperty("user.dir"), "target")
//            jar =  new File(target, a.getFileName("jar"))
//        } else {
//            File target = map.get(a.getGAV())
//            if(target!=null)
//                jar =  new File(target, a.getFileName("jar"))
//        }
//        return jar
//    }
//
    private Map<String, File> getModuleMap(File pomFile, Artifact a) {
        Map<String, File> map
        URL u = PomUtils.getParentPomFromProject(pomFile, a.groupId, a.artifactId)
        if(u!=null) {
            map = PomUtils.getProjectModuleMap(new File(u.toURI()), null)
        } else {
            map = PomUtils.getProjectModuleMap(pomFile, null)
        }

        return map
    }
}
