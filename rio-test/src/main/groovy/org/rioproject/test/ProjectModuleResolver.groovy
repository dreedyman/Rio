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

import org.rioproject.resolver.maven2.SimpleResolver
import org.rioproject.resolver.Artifact
import org.rioproject.resolver.maven2.PomUtils
import org.rioproject.resolver.RemoteRepository
import org.rioproject.config.Constants
import java.util.logging.Logger
import java.util.logging.Level
import org.rioproject.resolver.maven2.filters.ExclusionFilter
import org.rioproject.resolver.maven2.filters.ClassifierFilter
import org.rioproject.resolver.maven2.filters.DependencyFilter

/**
 * Resolves artifacts from within (or among) a project (module)
 */
class ProjectModuleResolver extends SimpleResolver {
    private static Logger logger= Logger.getLogger(ProjectModuleResolver.getClass().name)

    def ProjectModuleResolver() {
        super()
    }
    
    def URL getLocation(String a, String type, File pom) {
        if(pom==null)
            pom = new File(System.getProperty("user.dir"), "pom.xml")
        Artifact artifact = new Artifact(a)
        Map<String, File> map = getModuleMap(pom, artifact)
        File target = map.get(artifact.getGAV())
        URL u = null
        if(target!=null) {
            File f =  new File(target, artifact.getFileName(type))
            if(f!=null && f.exists())
                u = f.toURI().toURL()
        } else {
            u = super.getLocation(a, type, pom)
        }
        return u
    }

    def String[] getClassPathFor(String s, RemoteRepository[] remote, boolean download) {
        for(RemoteRepository rr: remote) {
            if(!repositories.contains(rr))
                repositories << rr
        }
        return getClassPathFor(s, (File)null, download)
    }

    def String[] getClassPathFor(String a, File pom, boolean download) {
        if(pom==null) {
            pom = new File(System.getProperty(Constants.RIO_TEST_EXEC_DIR,
                                              System.getProperty("user.dir")),
                           "pom.xml")
            Artifact artifact = new Artifact(a)
            Map<String, File> map = getModuleMap(pom, artifact)
            if(map.get(a)!=null) {
                File target = map.get(a)
                pom = new File(target.parentFile, "pom.xml")
            }
        }
        if(logger.isLoggable(Level.FINE))
            logger.fine "using POM: ${pom.absolutePath}, exists? ${pom.exists()}"

        if(!pom.exists())
            pom = null
        List<String> classPath = new ArrayList<String>()
        String[] deps = super.getClassPathFor(a, (File)pom, download);
        for(String dep : deps) {
            if(!classPath.contains(dep))
                classPath << dep
        }
        return classPath.toArray(new String[classPath.size()])
    }

    protected DependencyFilter getDependencyFilter(Artifact a) {
        DependencyFilter filter
        if(a.classifier && a.classifier=="dl") {
            filter = new ClassifierFilter(a.classifier)
        } else {
            filter = new ExclusionFilter(System.getProperty("RIO_TEST_ATTACH")!=null)
        }
        return filter
    }


    private File getJar(Artifact a, Map<String, File> map) {
        File jar = null
        if(map==null || map.size()==0) {
            File target = new File(System.getProperty("user.dir"), "target")
            jar =  new File(target, a.getFileName("jar"))
        } else {
            File target = map.get(a.getGAV())
            if(target!=null)
                jar =  new File(target, a.getFileName("jar"))
        }
        return jar
    }

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
