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
package org.rioproject.admin;

import java.io.IOException;

/**
 * A {@code ServiceActivityProvider} is used to determine whether a service is active
 * (processing something). Service activity is generally defined and is implementation
 * specific. Essentially, a service is active when it is being used. If a service is not
 * being used (no activity), the service is considered inactive.
 *
 * <p>Service inactivity can be used to determine when a service (and constituent services in an
 * {@link org.rioproject.opstring.OperationalString}) can (or should be) undeployed.
 *
 * @author Dennis Reedy
 */
public interface ServiceActivityProvider {

    /**
     * Provide feedback on whether the service is active.
     *
     * @return {@code true} if the service is active, {@code false} otherwise.
     * @throws IOException
     */
    boolean isActive() throws IOException;
}
