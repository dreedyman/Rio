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
package org.rioproject.sla;

import org.rioproject.deploy.SystemRequirements;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * The {@code ServiceLevelAgreements} class provides context on the attributes required to
 * meet and monitor service level agreements for a service. Included in this
 * class are the {@link org.rioproject.deploy.SystemRequirements} that
 * must be met in order for the service to be provisioned, and
 * {@link org.rioproject.sla.SLA} declarations that will monitor service behavior
 * and be managed by provided service level agreement manager.
 *
 * @author Dennis Reedy
 */
public class ServiceLevelAgreements implements Serializable {
    @SuppressWarnings("unused")
    static final long serialVersionUID = 1L;
    /** System  requirements */
    private SystemRequirements systemRequirements;
    /** Array of service SLAs */
    private final List<SLA> serviceSLAs = new ArrayList<SLA>();

    public void setServiceRequirements(SystemRequirements systemRequirements) {
        this.systemRequirements = systemRequirements;
    }

    public synchronized SystemRequirements getSystemRequirements() {
        if(systemRequirements==null)
            systemRequirements = new SystemRequirements();
        return systemRequirements;
    }

    /**
     * Add a service specified SLAs
     * 
     * @param sla An SLA specifying service specific operational criteria
     */
    public void addServiceSLA(SLA sla) {
        if(sla == null)
            throw new IllegalArgumentException("sla is null");
        synchronized(serviceSLAs) {
            serviceSLAs.add(sla);
        }
    }

    /**
     * Get the service specified SLAs
     *
     * @return Array of service SLAs. A new array is allocated each time. If
     * there are no service SLAs, a zero-length array is returned
     */
    public SLA[] getServiceSLAs() {
        SLA[] slas;
        synchronized(serviceSLAs) {
            slas = serviceSLAs.toArray(new SLA[serviceSLAs.size()]);
        }
        return (slas);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("ServiceLevelAgreements: ").append(systemRequirements);
        builder.append(", serviceSLAs=").append(serviceSLAs);
        return builder.toString();
    }
}
