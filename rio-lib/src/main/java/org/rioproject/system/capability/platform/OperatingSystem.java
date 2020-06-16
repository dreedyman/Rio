/*
 * Copyright 2008 the original author or authors.
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
package org.rioproject.system.capability.platform;

import org.rioproject.system.capability.PlatformCapability;

/**
 * The OperatingSystem class defines attributes describing the operating system.
 *
 * @author Dennis Reedy
 */
public class OperatingSystem extends PlatformCapability {
    private static final long serialVersionUID = 1L;
    static final String DEFAULT_DESCRIPTION = "Operating System";
    public static final String ID = "OperatingSystem";

    /** 
     * Create a OperatingSystem object
     */
    public OperatingSystem() {
        this(DEFAULT_DESCRIPTION);
    }

    /** 
     * Create a OperatingSystem object with a description
     *
     * @param description The description
     */
    public OperatingSystem(final String description) {
        this.description = description;
        define(NAME, System.getProperty("os.name"));
        define(VERSION, System.getProperty("os.version"));
    }
}
