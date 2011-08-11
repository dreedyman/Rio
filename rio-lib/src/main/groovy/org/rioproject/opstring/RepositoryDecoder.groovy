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
package org.rioproject.opstring

import org.rioproject.resolver.RemoteRepository

/**
 * Creates {@link org.rioproject.resolver.RemoteRepository} instances from an XML file
 */
class RepositoryDecoder {

    def decode(repositoryConf) {
        def remoteRepositories = []
        def repositories = new XmlSlurper().parse(repositoryConf)
        repositories.repository.each { r ->
            RemoteRepository repository = new RemoteRepository()
            repository.id = r.'@id'
            repository.url = r.'@url'

            String supported = r.snapshots.'@supported'
            repository.snapshots = Boolean.valueOf(supported)
            repository.snapshotChecksumPolicy = r.snapshots.'@checksumPolicy'
            repository.snapshotUpdatePolicy = r.snapshots.'@policy'

            supported = r.releases.'@supported'
            repository.releases =  Boolean.valueOf(supported)
            repository.releaseChecksumPolicy = r.releases.'@checksumPolicy'
            repository.releaseUpdatePolicy = r.releases.'@policy'

            remoteRepositories << repository
        }
        return remoteRepositories as RemoteRepository[]
    }

}
