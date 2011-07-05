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

import org.rioproject.resolver.RemoteRepository;

/**
 * Encapsulates parsed settings.
 */
class Settings {
    List<Profile> profiles = []
    File localRepository
    List<Mirror> mirrors = []

    List<RemoteRepository> getRemoteRepositories() {
        List<RemoteRepository> remoteRepositories = []
        for (Profile p: profiles) {
            for (RemoteRepository rr: p.repositories) {
                checkMirrors(rr)
                if (p.isActive())
                    remoteRepositories << rr
            }
        }
        /* Add central last of all */
        RemoteRepository central = new RemoteRepository()
        central.url = "http://repo1.maven.org/maven2/"
        central.id = "central"
        central.snapshots = false
        checkMirrors(central)
        if(!remoteRepositories.contains(central))
            remoteRepositories << central
        return  remoteRepositories
    }

    def checkMirrors(RemoteRepository rr) {
        for (Mirror m: mirrors) {
            if (exclude(m.mirrorOf, rr.id))
                continue
            String[] mirrorOfs = m.mirrorOf.split(",")
            for (String mirrorOf: mirrorOfs) {
                //println "repo: [${rr.id}], mirrorOf: [${mirrorOf}]"
                if (mirrorOf.equals(rr.id)) {
                    rr.url = m.url
                    rr.mirrored = true
                    //println ("\tnames match (${rr.id})")
                    break
                }
                if (mirrorOf == "*") {
                    rr.url = m.url
                    rr.mirrored = true
                    //println ("\twildcard match (${rr.id})")
                    break
                }
                if (mirrorOf == "external:*" &&
                    !(rr.url.startsWith("file:") ||
                      rr.url.contains("localhost"))) {
                    rr.url = m.url
                    rr.mirrored = true
                    //println ("\texternal wildcard match (${rr.id})")
                    break
                }
            }
            if (rr.isMirrored())
                break
        }
    }

    private boolean exclude(String arg, String id) {
        boolean exclude = false
        String[] args = arg.split(",")
        for (String s: args) {
            if (s.startsWith("!")) {
                String r = s.substring(1, s.length())
                if (r.equals(id)) {
                    exclude = true
                    break
                }
            }
        }
        return exclude
    }

}
