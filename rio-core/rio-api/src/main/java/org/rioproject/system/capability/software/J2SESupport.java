/*
 * Copyright 2008 the original author or authors.
 * Copyright 2005 Sun Microsystems, Inc.
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
 * The <code>J2SESupport</code> object base class for J2SE software support
 *
 * @author Dennis Reedy
 */
public class J2SESupport extends SoftwareSupport {
    static final long serialVersionUID = 1L;
    static final String DEFAULT_DESCRIPTION = "Java 2 Standard Edition";

    /**
     * Create a J2SESupport
     */
    public J2SESupport() {
        this(DEFAULT_DESCRIPTION);
    }

    /**
     * Create a J2SESupport
     * 
     * @param description Description to use
     */
    public J2SESupport(String description) {
        this.description = description;
        define(PlatformCapability.VERSION, System.getProperty("java.version"));
        String jvmName = System.getProperty("java.vm.name");
        if(jvmName!=null) {
            define(PlatformCapability.DESCRIPTION, jvmName);
        }
        define(PlatformCapability.NAME, "Java");
        String javaHome = System.getProperty("java.home");
        if(javaHome!=null)
            setPath(javaHome);
    }
}
