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
package org.rioproject.exec;

import org.rioproject.exec.posix.PosixShell;
import org.rioproject.exec.windows.WindowsShell;

/**
 * A factory that creates {@code Shell} instances.
 *
 * @author Dennis Reedy
 */
public class ShellFactory {
    private ShellFactory(){}

    /**
     * Create a {@code Shell}
     *
     * @return An implementation of a {@code Shell} based on the operating system.
     */
    public static Shell createShell() {
        if(System.getProperty("os.name").startsWith("Windows")) {
            return new WindowsShell();
        }
        return new PosixShell();
    }
}
