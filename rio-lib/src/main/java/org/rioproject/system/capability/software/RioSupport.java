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

/**
 * The <code>RioSupport</code> object provides a typed name for Rio support.
 *
 * @author Dennis Reedy
 */
public class RioSupport extends SoftwareSupport {
    static final long serialVersionUID = 1L;
    static final String DEFAULT_DESCRIPTION = "Rio";

    /**
     * Create a RioSupport object
     */
    public RioSupport() {
        this.description = DEFAULT_DESCRIPTION;
        define(NAME, "Rio");
    }
   
}
