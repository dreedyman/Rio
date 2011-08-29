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
package org.rioproject.system.capability.software;

import org.rioproject.system.capability.PlatformCapability;

/**
 * The <code>SoftwareSupport</code> object provides a common base for all
 * software support
 *
 * @author Dennis Reedy
 */
public class SoftwareSupport extends PlatformCapability {
    static final long serialVersionUID = 1L;
    static final String DEFAULT_DESCRIPTION = "Software Element";

    /**
     * Create a SoftwareSupport object     
     */
    public SoftwareSupport() {
        this(DEFAULT_DESCRIPTION);
    }

    /**
     * Create a SoftwareSupport object
     *
     * @param description A description of the software
     */
    public SoftwareSupport(String description) {
        this.description = description;
    }

}
