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

import org.rioproject.impl.exec.AbstractShell;
import org.rioproject.exec.ExecDescriptor;
import org.rioproject.impl.exec.ProcessManager;
import org.rioproject.impl.exec.Util;
import org.rioproject.impl.util.FileUtils;
import org.rioproject.util.PropertyHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;

/**
 * A Shell implementation that starts commands for Windows systems.
 *
 * @author Dennis Reedy
 */
public class WindowsShell extends AbstractShell {
    private static final String DEFAULT_EXEC_CMD="classpath:start-template.cmd";
    static final Logger logger = LoggerFactory.getLogger(WindowsShell.class);

    public WindowsShell() {
        super(DEFAULT_EXEC_CMD);
    }

    @Override
    protected String getRedirection(final ExecDescriptor execDescriptor) {
        StringBuilder redirection = new StringBuilder();
        if (execDescriptor.getStdOutFileName() != null) {
            String stdOutFileName = PropertyHelper.expandProperties(execDescriptor.getStdOutFileName());
            redirection.append(" > ").append(stdOutFileName);
        }
        /*if (execDescriptor.getStdErrFileName() != null) {
            redirection.append(" 2>&1");
        }*/
        return redirection.toString();
    }

    @Override
    public ProcessManager exec(final ExecDescriptor execDescriptor) throws IOException {
        String workingDirectory = execDescriptor.getWorkingDirectory();
        String commandLine = buildCommandLine(execDescriptor);

        File generatedCommandScript = File.createTempFile("start-", ".cmd");
        generatedCommandScript.deleteOnExit();

        /* Delete the generated file on exit */
        //generatedCommandScript.deleteOnExit();
        logger.info("Generated start script here: {}", generatedCommandScript.getPath());

        URL url = getTemplateURL();
        StringBuilder sb = new StringBuilder();
        BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
        String str;
        String commandLineToExecute = null;
        while ((str = in.readLine()) != null) {
            String filtered = Util.replace(str, "${commandLine}", commandLine);
            if(!str.equals(filtered)) {
                commandLineToExecute = filtered;
            }
            sb.append(filtered).append("\n");
        }
        in.close();

        Util.writeFile(sb.toString(), generatedCommandScript);

        String toExec = FileUtils.getFilePath(generatedCommandScript);
        logger.info("Generated command line: [{}]", commandLineToExecute);

        //ProcessBuilder processBuilder = createProcessBuilder(workingDirectory, execDescriptor, "cmd.exe", "/C", toExec);
        ProcessBuilder processBuilder = createProcessBuilder(workingDirectory,
                                                             execDescriptor,
                                                             "cmd.exe", "/C", commandLineToExecute);
        Process process = processBuilder.start();

        return new WindowsProcessManager(process, 1);
    }
}
