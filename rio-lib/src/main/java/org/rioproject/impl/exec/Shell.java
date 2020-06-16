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
package org.rioproject.impl.exec;

import org.rioproject.exec.ExecDescriptor;

import java.io.IOException;

/**
 * Interface defining a shell within which commands can be run
 *
 * @author Dennis Reedy
 */
public interface Shell {
    /**
     * Execute a command, returning a {@link ProcessManager}
     *
     * @param execDescriptor The descriptor for the service control adapter
     *
     * @return A {@link ProcessManager} for managing the started {@link Process}
     *
     * @throws java.io.IOException if the command cannot be executed
     */
    ProcessManager exec(ExecDescriptor execDescriptor) throws IOException;

    /**
     * Set the template to generate a script that will be executed
     * in the background.
     *
     * @param template The template, to be loaded as a classpath resource, used
     * to generate a platform specific script to execute a command in the
     * background.
     */
    void setShellTemplate(String template);
}
