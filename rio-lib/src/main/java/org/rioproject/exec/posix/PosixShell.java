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
package org.rioproject.exec.posix;

import org.rioproject.exec.AbstractShell;
import org.rioproject.exec.ExecDescriptor;
import org.rioproject.exec.ProcessManager;
import org.rioproject.exec.Util;
import org.rioproject.resources.util.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.URL;

/**
 * A Shell implementation that forks commands for posix compliant systems
 *
 * @author Dennis Reedy
 */
public class PosixShell extends AbstractShell {
    private static final String DEFAULT_EXEC_SCRIPT="classpath:exec-template.sh";
    static final Logger logger = LoggerFactory.getLogger(PosixShell.class);

    public PosixShell() {
        super(DEFAULT_EXEC_SCRIPT);
    }

    /**
     * @see org.rioproject.exec.Shell#exec(org.rioproject.exec.ExecDescriptor)
     */
    public ProcessManager exec(ExecDescriptor execDescriptor) throws IOException {
        String workingDirectory = execDescriptor.getWorkingDirectory();
        String commandLine = buildCommandLine(execDescriptor);
        String originalCommand = execDescriptor.getCommandLine();

        File pidFile = File.createTempFile("exec-", ".pid");
        File generatedShellScript = File.createTempFile("exec-", ".sh");

        /* Delete the generated file on exit */
        generatedShellScript.deleteOnExit();
        logger.debug("Generated exec script here: {}", generatedShellScript.getPath());

        URL url = getTemplateURL();
        StringBuilder sb = new StringBuilder();
        BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
        String str;
        while ((str = in.readLine()) != null) {
            str = Util.replace(str, "${command}", originalCommand);
            str = Util.replace(str, "${pidFile}", FileUtils.getFilePath(pidFile));
            str = Util.replace(str, "${commandLine}", commandLine);
            sb.append(str).append("\n");
        }
        in.close();

        Util.writeFile(sb.toString(), generatedShellScript);
        Util.chmodX(generatedShellScript);        

        String toExec = FileUtils.getFilePath(generatedShellScript);
        logger.debug("Generated command line: [{}]", commandLine);
        /*ProcessBuilder pb = new ProcessBuilder(toExec);

        Map<String, String> declaredEnv = execDescriptor.getEnvironment();
        Map<String, String> environment = pb.environment();
        for(Map.Entry<String, String> entry : environment.entrySet()) {
            String value = declaredEnv.get(entry.getKey());
            if(value!=null) {
                String oldValue = entry.getValue();
                String setValue = oldValue+File.pathSeparator+value;
                environment.put(entry.getKey(), setValue);
                logger.info("{} was [{}], now [{}]", entry.getKey(), oldValue, environment.get(entry.getKey()));
            }
        }
        environment.putAll(execDescriptor.getEnvironment());
        logger.trace("Process Builder's environment={}", environment);

        if(workingDirectory!=null) {
            pb = pb.directory(new File(workingDirectory));
            logger.debug("Process Builder's working directory set to [{}]", pb.directory().getPath());
        }

        logger.debug("ProcessBuilder command: {}", pb.command());
        logger.debug("ProcessBuilder environment: {}", pb.environment());

        pb.redirectErrorStream(true);*/
        ProcessBuilder processBuilder = createProcessBuilder(toExec, execDescriptor, workingDirectory);
        Process process = processBuilder.start();

        /* Started process, wait for pid file ... */
        while(pidFile.length()==0) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        in = new BufferedReader(new FileReader(pidFile));
        String s = in.readLine();
        int pid = Integer.parseInt(s);
        in.close();
        if(!pidFile.delete())
            logger.warn("Non fatal, could not delete {}", FileUtils.getFilePath(pidFile));

        PosixProcessManager processManager = new PosixProcessManager(process,
                                                                     pid,
                                                                     execDescriptor.getStdOutFileName(),
                                                                     execDescriptor.getStdErrFileName());
        processManager.setCommandFile(generatedShellScript);
        processManager.setCommandLine(commandLine);
        return processManager;
    }
}
