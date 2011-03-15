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
import org.rioproject.resolver.Dependency
import org.rioproject.resolver.maven2.filters.DependencyFilter
import org.rioproject.config.maven2.SettingsParser
import org.rioproject.resolver.RemoteRepository
import org.rioproject.config.maven2.Settings
import java.util.logging.Logger
import java.util.logging.Level

/**
 * Parses a Maven pom.
 */
public class PomParser {
    Settings settings
    def remoteRepositories
    def localRepository
    def depManagement = [:]
    private String pomGroupId
    private String pomVersion
    def processedDeps = []
    def processedPoms = [:]
    def resolvedParents = []
    def properties = [:]
    ArtifactUtils artifactUtils
    def excludes = []
    boolean buildFromProject = false
    Map<String, File> projectModuleMap = new HashMap<String, File>()
    Logger logger = Logger.getLogger(PomParser.class.getName())

    def parse(Dependency dep, DependencyFilter filter) {
        return parse(dep, dep.pomURL, filter)
    }

    ResolutionResult parse(Artifact artifact, URL u, DependencyFilter filter) {
        if(u==null)
            return null
        ResolutionResult result = new ResolutionResult(artifact)
        InputStream is = u.openStream()
        if(is==null)
            return result

        if(logger.isLoggable(Level.FINE))
            logger.fine "Processing pom: ${u.toString()}"
        
        XmlSlurper parser = PomUtils.getXmlSlurper()
        def pom = parser.parseText(u.text)
        String sPom = "${pom.groupId}:${pom.artifactId}:${pom.version}"
        if(processedPoms.containsKey(sPom))
            return result

        setPomVars(pom)

        String artifactId = pom.artifactId

        pom.distributionManagement.relocation.each { r ->
            String reloGroupId = r.groupId
            String reloArtifactId = r.artifactId
            String reloVersion = r.version

            /* Save old values for logging (if configured) */
            String oldGroupId = pomGroupId
            String oldArtifactId = artifactId
            String oldVersion = pomVersion

            /* Get new values */
            if(reloGroupId.length()>0)
                pomGroupId = reloGroupId
            if(reloArtifactId.length()>0)
                artifactId = reloArtifactId
            if(reloVersion.length()>0)
                pomVersion = reloVersion
            artifact.groupId = pomGroupId
            artifact.artifactId = artifactId
            artifact.version = pomVersion

            /* Reset pom url */
            artifact.pomURL = null
            if(logger.isLoggable(Level.FINE))
            logger.fine "Relocation detected. From "+
                        "[$oldGroupId:$oldArtifactId:$oldVersion] to "+
                        "[$pomGroupId:$artifactId:$pomVersion]"
        }

        if(u.toExternalForm().endsWith("pom.xml") && u.protocol.equals("file")) {
            buildFromProject = true
        }

        getProperties(pom)

        processParent(pom, artifact, filter, u, result)

        /* Reset pom variables, parent processing may have changed them */
        setPomVars(pom)

        /* If we are building from the project, lets check to make sure that
         * the artifact being processed is indeed a (sub) module of the project
         */
        String gav = "$artifact.groupId:$artifact.artifactId:$artifact.version"
        if(buildFromProject && !projectModuleMap.containsKey(gav)) {
            buildFromProject = false
            if(logger.is(Level.FINE))
                logger.fine "DO NOT build ${gav} from project pom: $u, "+
                            "not found in projectMap $projectModuleMap"
            artifact.loadFromProject = false
            artifact.pomURL = null
            artifact.pomURL = artifactUtils.getArtifactPomURLFromRepository(artifact,
                                                              localRepository,
                                                              remoteRepositories)
            projectModuleMap.clear()
            return parse(artifact, artifact.pomURL, filter)
        } else {
            artifact.loadFromProject = buildFromProject
            if(buildFromProject) {
                File target = projectModuleMap.get(gav)
                if(target!=null && target.exists()) {
                    File projectPom = new File(target.getParentFile(), "pom.xml")
                    if(projectPom.exists())
                        artifact.pomURL = projectPom.toURI().toURL()
                }
            }
        }

        pom.dependencyManagement.dependencies.dependency.each { dependency ->

            dependency.exclusions.exclusion.each { exclusion ->
                String excludedArtifact = "${exclusion.groupId}:${exclusion.artifactId}"
                //println "(1) EXCLUDE $excludedArtifact"
                if(!excludes.contains(excludedArtifact))
                    excludes << excludedArtifact
            }
            String type = dependency.type            
            if(type.length()==0 || type.equals("jar")) {
                String depVersion = dependency.version
                String v = getValue(depVersion, u.toString())
                if(v!=null)
                    depVersion = v
                if(depVersion.equals('${pom.version}') ||
                   depVersion.equals('${project.version}')) {
                    depVersion = pomVersion
                }
                String gid = dependency.groupId
                if(gid.equals('${pom.groupId}') || gid.equals('${project.groupId}'))
                    gid = pomGroupId
                depManagement.put("${gid}:${dependency.artifactId}",
                                  "${depVersion}:${dependency.scope}")
            }
        }

        pom.repositories.repository.each { repository ->
            RemoteRepository rr = SettingsParser.createRemoteRepository(repository)
            settings.checkMirrors(rr)
            if(!remoteRepositories.contains(rr)) {
                remoteRepositories.add(rr)
            }
        }

        pom.dependencies.dependency.each { dependency ->
            String type = dependency.type
            dependency.exclusions.exclusion.each { exclusion ->
                String excludedArtifact = "${exclusion.groupId}:${exclusion.artifactId}"
                //println "(2) EXCLUDE $excludedArtifact"
                if(!excludes.contains(excludedArtifact))
                    excludes << excludedArtifact
            }
            if(type.length()==0 || type.equals("jar")) {
                String classifier = dependency.classifier
                classifier = classifier.length()==0?null:classifier
                String scope = dependency.scope
                String gid = dependency.groupId
                if(gid.contains('${pom.groupId}') || gid.contains('${project.groupId}')) {
                    if(gid.contains('${pom.groupId}'))
                        gid = gid.replaceAll(/\$\{pom.groupId\}/, pomGroupId)
                    else
                        gid = gid.replaceAll(/\$\{project.groupId\}/, pomGroupId)
                }

                String depArtifactId = dependency.artifactId
                String depVersion = dependency.version
                String v = getValue(depVersion, u.toString())
                if(v!=null)
                    depVersion = v
                String depManagementInfo = depManagement.get("${gid}:${depArtifactId}")
                if(depVersion.length()==0) {
                    if(depManagementInfo!=null) {
                        int ndx = depManagementInfo.indexOf(":")
                        depVersion = depManagementInfo.substring(0, ndx)
                    }
                } else if(depVersion.equals('${pom.version}')) {
                    depVersion = pomVersion
                } else if(depVersion.equals('${project.version}')) {
                    depVersion = pomVersion
                }
                if(scope.length()==0 && depManagementInfo!=null) {
                    int ndx = depManagementInfo.indexOf(":")
                    scope = depManagementInfo.substring(ndx+1, depManagementInfo.length())
                    //if(scope.length()>0)
                    //    println "===> ${gid}:${depArtifactId} scope is: $scope"
                }
                Dependency dep = new Dependency(gid, depArtifactId, depVersion, classifier)
                dep.excluded = excludes.contains(gid+":"+depArtifactId)
                dep.optional = dependency.optional.equals("true")
                dep.scope = scope
                if(filter!=null && filter.include(dep)) {
                    if(!(gid.equals(pomGroupId) &&
                         depArtifactId.equals(artifactId) &&
                         depVersion.equals(pomVersion))) {
                        String depGAV = "${gid}:${depArtifactId}:${depVersion}"
                        if(!processedDeps.contains(depGAV)) {
                            if(projectModuleMap.get(depGAV)!=null) {
                                File target = projectModuleMap.get(depGAV)
                                if(target!=null) {
                                    dep.pom = new File(target.parentFile, "pom.xml")
                                    dep.pomURL = dep.pom.toURI().toURL()
                                    dep.loadFromProject = true
                                    dep.jar = new File(target, dep.getFileName("jar"))
                                }
                            } else {
                                dep.pomURL = artifactUtils.getArtifactPomURLFromRepository(dep,
                                                                                           localRepository,
                                                                                           remoteRepositories)

                            }
                            result.dependencies.add(dep)
                            processedDeps.add("${gid}:${depArtifactId}:${depVersion}")
                        }
                    }
                }
            }
        }
        processedPoms.put(sPom, properties)

        artifact.pom = ArtifactUtils.getLocalFile(artifact, localRepository, "pom")
        if(!artifact.pom.exists() && artifact.pomURL==null)
            artifact.pomURL =  artifactUtils.getArtifactPomURLFromRepository(artifact,
                                                                             localRepository,
                                                                             remoteRepositories)
        if(!pom.packaging.equals("pom")) {
            String ext = pom.packaging.equals("oar")?"oar":"jar"
            artifact.jar = ArtifactUtils.getLocalFile(artifact, localRepository, ext)
        }
        resolvedParents.each { parent ->
            result.resolvedParents.add parent
        }
        return result
    }

    private String getValue(String s, String pom) {
        if(s.length()==0)
            return null
        if(s.startsWith('${')) {
            s = s.substring(2, s.length())
            s = s.substring(0, s.length()-1)
        }
        return (String)properties.get(s)
    }

    private void getProperties(pom) {
        if(pom.properties.size()>0) {
            def propsNode = pom.properties
            for(Node child : propsNode.childNodes()) {
                properties.put(child.name(), child.text())
            }
        }
    }

    def processParent(def pom, Artifact a, DependencyFilter filter, URL u, ResolutionResult r) {
        if(pom.parent.size()>0) {
            String pPom = "${pom.parent.groupId}:${pom.parent.artifactId}:${pom.parent.version}"
            if(!processedPoms.containsKey(pPom)) {
                Artifact parentArtifact = new Artifact("${pom.parent.groupId}",
                                                       "${pom.parent.artifactId}",
                                                       "${pom.parent.version}")
                URL parentPom
                if(buildFromProject) {
                    File pomFile = new File(u.toURI())
                    parentPom = PomUtils.getParentPomFromProject(pomFile,
                                                                 parentArtifact.groupId,
                                                                 parentArtifact.artifactId)
                    projectModuleMap.putAll PomUtils.getProjectModuleMap(new File(parentPom.toURI()),
                                                                         "${pom.parent.version}")
                    String gav = "$a.groupId:$a.artifactId:$a.version"
                    if(!projectModuleMap.containsKey(gav)) {
                        return
                    }
                } else {
                    parentPom = artifactUtils.getArtifactPomURLFromRepository(parentArtifact,
                                                                              localRepository,
                                                                              remoteRepositories)
                }
                parentArtifact.pomURL = parentPom
                ResolutionResult parentResult = parse(parentArtifact, parentPom, filter)
                resolvedParents << parentResult

                for(Dependency d : parentResult.dependencies) {
                    if(!d.excluded)
                        r.dependencies.add(d)
                }

            }  else {
                properties.putAll(processedPoms.get(pPom))
            }
        } else {
            if(u.protocol=="file")
                projectModuleMap.putAll PomUtils.getProjectModuleMap(new File(u.toURI()),
                                                                     pomVersion)
        }
    }

    private void setPomVars(def pom) {
        if("${pom.groupId}".length()>0)
            pomGroupId = pom.groupId
        if("${pom.version}".length()>0)
            pomVersion = pom.version
    }
}
