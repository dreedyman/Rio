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
 * Parse maven settings
 */
class SettingsParser {

    Settings parse(File settingsFile) {
        Settings s = new Settings()
        if(!settingsFile.exists())
            return s

        XmlSlurper parser = new XmlSlurper()
        def settings = parser.parse(settingsFile)

        def activeProfiles = []

        settings.activeProfiles.activeProfile.each { a ->
            String ap = a
            activeProfiles << ap
        }

        settings.mirrors.mirror.each { m ->
            Mirror mirror = new Mirror()
            mirror.mirrorOf = m.mirrorOf
            mirror.url = m.url
            mirror.id = m.id
            s.mirrors << mirror
        }
        
        settings.profiles.profile.each { p ->
            Profile profile = new Profile()
            profile.id = p.id
            if(p.activation.activeByDefault && p.activation.activeByDefault=="true") {
                profile.activeByDefault = true
            } else if(profile.id in activeProfiles) {
                profile.activeByDefault = true
            }

            if(p.activation.property.name) {
                String name = p.activation.property.name
                if(p.activation.property.value) {
                    String value = p.activation.property.value
                    profile.setActivateOnProperty(name, value)
                } else {
                    profile.setActivateOnProperty(name)
                }
            }
            p.repositories.repository.each { r ->                
                RemoteRepository rr = createRemoteRepository(r)
                profile.repositories << rr

            }
            s.profiles << profile
        }


        return s
    }

    static RemoteRepository createRemoteRepository(def r) {
        String url = r.url
        url = url.trim().replaceAll("\n", '');
        if(!url.endsWith("/"))
            url = url+"/"
        RemoteRepository rr = new RemoteRepository()
        rr.id = r.id
        rr.name = r.name
        rr.url = url
        /* Set snapshot properties */
        if(r.snapshots.enabled && r.snapshots.enabled=="false")
            rr.snapshots = false
        if(r.snapshots.checksumPolicy && r.snapshots.checksumPolicy=="warn")
            rr.snapshotChecksumPolicy = "warn"

        /* Set release properties */
        if(r.releases.enabled && r.releases.enabled=="false")
            rr.releases = false
        if(r.releases.checksumPolicy && r.releases.checksumPolicy=="warn")
            rr.releaseChecksumPolicy = "warn"
        return rr
    }
}