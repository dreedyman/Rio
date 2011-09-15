/*
 * Copyright to the original author or authors
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
package org.rioproject.url.artifact;

import org.rioproject.resolver.RemoteRepository;

import java.util.ArrayList;
import java.util.List;

/**
 * Holds the attributes of a URL of type <code>artifact</code>
 *
 * @author Dennis Reedy
 */
public class ArtifactURLConfiguration {
    private String artifact;
    private final List<RemoteRepository> repositories = new ArrayList<RemoteRepository>();

    public ArtifactURLConfiguration(String path) {
        int index = path.indexOf(";");
        if(index!=-1) {
            String repoString = path.substring(index+1, path.length());
            path = path.substring(0, index);
            String[] parts = repoString.split(";");
            for(String s : parts) {
                RemoteRepository r = new RemoteRepository();
                r.setUrl(s);
                repositories.add(r);
            }
        }
        artifact = path.replaceAll("/", ":");
    }

    public String getArtifact() {
        return artifact;
    }

    public RemoteRepository[] getRepositories() {
        return repositories.toArray(new RemoteRepository[repositories.size()]);
    }
}
