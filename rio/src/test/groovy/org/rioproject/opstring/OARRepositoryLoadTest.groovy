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
 * Tests loading a repositories.xml file
 */
class OARRepositoryLoadTest extends GroovyTestCase {

      void testReadingFile() {
        RepositoryDecoder decoder = new RepositoryDecoder()
        RemoteRepository[] remoteRepositories = decoder.decode(new File("src/test/resources/repositories.xml"))
        assertTrue(remoteRepositories.length==2)
    }

    void testRioRepository() {
        RepositoryDecoder decoder = new RepositoryDecoder()
        RemoteRepository[] remoteRepositories = decoder.decode(new File("src/test/resources/repositories.xml"))
        assertTrue(remoteRepositories.length==2)
        RemoteRepository rio = remoteRepositories[0]
        assertTrue rio.id.equals('rio')
        assertTrue rio.url.equals('http://www.rio-project.org/maven2')
        assertTrue rio.supportsSnapshots()
        assertTrue rio.supportsReleases()
        assertTrue rio.releaseChecksumPolicy=='warn'
        assertTrue rio.snapshotChecksumPolicy=='warn'
        assertTrue rio.releaseUpdatePolicy=='daily'
        assertTrue rio.snapshotUpdatePolicy=='daily'
    }

    void testCentralRepository() {
        RepositoryDecoder decoder = new RepositoryDecoder()
        RemoteRepository[] remoteRepositories = decoder.decode(new File("src/test/resources/repositories.xml"))
        assertTrue(remoteRepositories.length==2)
        RemoteRepository central = remoteRepositories[1]
        assertTrue central.id.equals('central')
        assertTrue central.url.equals('http://repo1.maven.org/maven2')
        assertTrue !central.supportsSnapshots()
        assertTrue central.supportsReleases()
        assertTrue central.releaseChecksumPolicy=='warn'
        assertTrue central.snapshotChecksumPolicy=='warn'
        assertTrue central.releaseUpdatePolicy=='daily'
        assertTrue central.snapshotUpdatePolicy=='daily'
    }

}
