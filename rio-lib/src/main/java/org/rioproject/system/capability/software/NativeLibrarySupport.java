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

/**
 * The <code>NativeLibrarySupport</code> object describes a native library
 * available to services
 *
 * @author Dennis Reedy
 */
public class NativeLibrarySupport extends SoftwareSupport {
    @SuppressWarnings("unused")
	static final long serialVersionUID = 1L;
    static final String DEFAULT_DESCRIPTION = "Native Library Support";
    /** 
     * System Library file name
     */
    public final static String FILENAME = "FileName";

    /** 
     * Create a NativeLibrarySupport
     */
    public NativeLibrarySupport() {
        this(DEFAULT_DESCRIPTION);
    }

    /** 
     * Create a NativeLibrarySupport
     * 
     * @param description Description to use
     */
    public NativeLibrarySupport(String description) {
        this.description = description;
    }

}
