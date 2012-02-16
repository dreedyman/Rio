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
package org.rioproject.monitor;

import org.rioproject.opstring.OperationalString;
import org.rioproject.resolver.RemoteRepository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

/**
 * Contains details required to deploy an OperationalString or OAR
 */
public class DeployRequest {
    private final Collection<OperationalString> operationalStrings = new ArrayList<OperationalString>();
    private final Collection<RemoteRepository> repositories = new ArrayList<RemoteRepository>();

    public DeployRequest(OperationalString opString, Collection<RemoteRepository> repositories) {
        operationalStrings.add(opString);
        if(repositories!=null)
            this.repositories.addAll(repositories);
    }

    public DeployRequest(OperationalString opString, RemoteRepository[] repositories) {
        operationalStrings.add(opString);
        if(repositories!=null) {
            Collections.addAll(this.repositories, repositories);
        }
    }

    public DeployRequest(OperationalString[] opStrings, Collection<RemoteRepository> repositories) {
        Collections.addAll(operationalStrings, opStrings);
        if(repositories!=null)
            this.repositories.addAll(repositories);
    }

    public Collection<OperationalString> getOperationalStrings() {
        return operationalStrings;
    }

    public RemoteRepository[] getRepositories() {
        return repositories.toArray(new RemoteRepository[repositories.size()]);
    }
}
