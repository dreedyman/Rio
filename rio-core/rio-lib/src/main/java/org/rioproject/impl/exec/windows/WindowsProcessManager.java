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
package org.rioproject.impl.exec.windows;

import org.rioproject.impl.exec.ProcessManager;
import org.rioproject.impl.exec.Util;

import java.io.IOException;

/**
 * A {@code ProcessManager} for Windows based systems.
 *
 * @author Dennis Reedy
 */
public class WindowsProcessManager extends ProcessManager {

    /**
     * Create a WindowsProcessManager
     *
     * @param process The {@link Process} the ProcessManager will manage
     * @param pid     The process ID of the started Process
     */
    public WindowsProcessManager(Process process, int pid) {
        super(process, pid);
        handleRedirects(null, null);
    }

    @Override
    public void manage() throws IOException {
        //todo This would be for tracking external (non service-exec) processes
    }

    @Override
    public void destroy(boolean includeChildren) {
        if (outputStream != null)
            outputStream.interrupt();
        if (errorStream != null)
            errorStream.interrupt();

        Util.close(getProcess().getOutputStream());
        Util.close(getProcess().getInputStream());
        Util.close(getProcess().getErrorStream());
        getProcess().destroy();
    }
}
