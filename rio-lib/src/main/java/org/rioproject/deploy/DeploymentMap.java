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
package org.rioproject.deploy;

import org.rioproject.opstring.ServiceElement;

import java.io.Serializable;
import java.util.*;

/**
 * Provides details on where services have been deployed.
 *
 * @author Dennis Reedy
 */
public class DeploymentMap implements Serializable {
    private static final long serialVersionUID = 1L;
    private final Map<ServiceElement, List<DeployedService>> deployed =
        new HashMap<ServiceElement, List<DeployedService>>();

    public DeploymentMap(Map<ServiceElement, List<DeployedService>> map ) {
        deployed.putAll(map);
    }

    /**
     * Get the <tt>List</tt> of {@link DeployedService}s
     * for a {@link org.rioproject.opstring.ServiceElement} .
     *
     * @param elem The ServiceElement to inquire about
     *
     * @return An immutable <tt>List</tt> of
     * {@link DeployedService}s. If there is
     * no mapping for the <tt>ServiceElement</tt>, return an empty <tt>List</tt>.
     * A new <tt>List</tt> is allocated each time.
     *
     * @throws IllegalArgumentException if the <tt>ServiceElement</tt> is null.
     */
    public List<DeployedService> getDeployedServices(ServiceElement elem) {
        if(elem==null)
            throw new IllegalArgumentException("A ServiceElement must be provided");

        List<DeployedService> list = deployed.get(elem);
        List<DeployedService> result;
        if(list==null)
            result =
                Collections.unmodifiableList(new ArrayList<DeployedService>());
        else
            result = Collections.unmodifiableList(list);
        return result;
    }

    /**
     * Get the deployed {@link org.rioproject.opstring.ServiceElement}s
     *
     * @return An array of deployed <tt>ServiceElement</tt>s. If there are no
     * deployed <tt>ServiceElement</tt>s, return an empty array. A new array
     * is allocated each time.
     */
    public ServiceElement[] getServiceElements() {
        List<ServiceElement> list = new ArrayList<ServiceElement>();
        for (Map.Entry<ServiceElement, List<DeployedService>> entry :
            deployed.entrySet()) {
            list.add(entry.getKey());
        }
        return list.toArray(new ServiceElement[list.size()]);
    }
}
