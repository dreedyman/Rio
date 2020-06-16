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

import org.rioproject.opstring.OperationalStringManager;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * The {@code DeploymentResult} class provides context of deploying an {@code OperationalString}.
 *
 * @author Dennis Reedy
 */
public class DeploymentResult implements Serializable {
    static final long serialVersionUID = 1L;
    private final OperationalStringManager operationalStringManager;
    private final Map<String, Throwable> errorMap = new HashMap<String, Throwable>();

    public DeploymentResult(final OperationalStringManager operationalStringManager,
                            final Map<String, Throwable> errorMap) {
        this.operationalStringManager = operationalStringManager;
        if(errorMap!=null)
            this.errorMap.putAll(errorMap);
    }

    public OperationalStringManager getOperationalStringManager() {
        return operationalStringManager;
    }

    /**
     * If there are errors loading part of the {@code OperationalString} the
     * Map will be returned with name value pairs associating the service and
     * corresponding exceptions
     *
     * @return The {@code Map} of errors processing the {@code OperationalString}
     */
    public Map<String, Throwable> getErrorMap() {
        return errorMap;
    }
}
