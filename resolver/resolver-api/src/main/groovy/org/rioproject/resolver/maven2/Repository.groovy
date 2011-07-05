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

import org.rioproject.resolver.RemoteRepository

/**
 * Basic utility for interfacing with Maven 2 repositories
 */
class Repository {
    /**
     * Get the local repository
     *
     * @return The File for the local maven repository, taking into account
     * settings.xml
     */
    def static File getLocalRepository() {
        File repoDir
        String localRepository = null
        File defaultM2Home =
                new File(System.getProperty("user.home")+File.separator+".m2")
        if(System.getProperty("M2_HOME")!=null) {
            File settingsFile = new File(System.getProperty("M2_HOME"), "conf/settings.xml")
            if(settingsFile.exists()) {
                def settings = new XmlSlurper().parse(settingsFile)
                localRepository = settings.localRepository
            }
        }
        File settingsFile = new File(defaultM2Home, "settings.xml")
        if(settingsFile.exists()) {
            def settings = new XmlSlurper().parse(settingsFile)
            localRepository = settings.localRepository
        }
        if(localRepository==null) {
            repoDir = new File(defaultM2Home, "repository")
        } else if(localRepository!=null && localRepository.length()==0) {
            repoDir = new File(defaultM2Home, "repository")
        } else {
            repoDir = new File(localRepository)
        }
        return repoDir
    }

    /**
     * Get the {@link org.rioproject.resolver.RemoteRepository} items from
     * the settings file
     *
     * @return A List of {@link org.rioproject.resolver.RemoteRepository} items,
     * as derived from the settings.xml file found in the local repository.
     *
     * @see #getLocalRepository
     */
    def static List<RemoteRepository> getRemoteRepositories() {
        File localRepo = getLocalRepository()
        File settingsFile = new File(localRepo, "settings.xml")
        SettingsParser parser = new SettingsParser()
        Settings settings = parser.parse(settingsFile)
        return settings.getRemoteRepositories()
    }

    /**
     * Get declared remote repositories from a pom
     *
     * @param pomFile The File object for the pom. If the parameter is null,
     * or the pomFile does not exist and empty List is returned
     *
     * @return A List of remote repository URLs from the pomFile. If there are
     * no declared repository elements, return an empty List.
     */
    def static List<RemoteRepository> getRemoteRepositories(File pomFile) {
        List<RemoteRepository> remoteRepositories = []
        if(pomFile==null)
            return remoteRepositories

        if(!pomFile.exists())
            return remoteRepositories
        //println("Parsing ${pomFile.path}")
        XmlSlurper parser = new XmlSlurper()
        def pom = parser.parseText(pomFile.text)

        if(pom.parent.size()>0) {
            StringBuilder sb = new StringBuilder()
            String groupId = pom.parent.groupId
            sb.append(groupId.replace(".", File.separator))
            sb.append(File.separator)
            sb.append(pom.parent.artifactId)
            sb.append(File.separator)
            sb.append(pom.parent.version)
            sb.append(File.separator)
            sb.append(pom.parent.artifactId).append("-").append(pom.parent.version).append(".pom")
            File parentPom = new File(getLocalRepository(), sb.toString())
            //println "\tparent: ${parentPom.path}"
            remoteRepositories.addAll(getRemoteRepositories(parentPom))
        }

        pom.repositories.repository.each { r ->
            RemoteRepository rr = SettingsParser.createRemoteRepository(r)
            if(!remoteRepositories.contains(rr))
                remoteRepositories.add(rr)
        }
        return remoteRepositories
    }

}
