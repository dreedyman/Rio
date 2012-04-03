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
package org.rioproject.exec.support;

import org.rioproject.exec.ExecDescriptor;
import org.rioproject.exec.ProcessManager;
import org.rioproject.exec.Shell;
import org.rioproject.exec.Util;
import org.rioproject.resources.util.FileUtils;

import java.io.*;
import java.net.URL;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A Shell implementation that forks commands for posix compliant systems
 *
 * @author Dennis Reedy
 */
public class PosixShell implements Shell {
    private static final String COMPONENT = PosixShell.class.getPackage().getName();
    private static final String EXEC_SCRIPT="exec-template.sh";
    //private static final String EXEC_NOHUP_SCRIPT="exec-nohup-template.sh";
    private String template = EXEC_SCRIPT;
    static final Logger logger = Logger.getLogger(COMPONENT);

    public void setShellTemplate(String template) {
        if(template==null)
            throw new IllegalArgumentException("template cannot be null");
        this.template = template;
        logger.info("Set PosixShell template to: "+template);
    }

    /**
     * @see org.rioproject.exec.Shell#exec(org.rioproject.exec.ExecDescriptor)
     */
    public ProcessManager exec(ExecDescriptor execDescriptor) throws IOException {
        String commandLine = execDescriptor.getCommandLine();
        String workingDirectory = execDescriptor.getWorkingDirectory();

        if (commandLine == null) {
            throw new IllegalArgumentException("commandLine cannot be null");
        }
        String command = commandLine;

        commandLine = "exec "+commandLine;

        if (execDescriptor.getInputArgs() != null) {
            commandLine = commandLine+" "+execDescriptor.getInputArgs();
        }
        if (execDescriptor.getStdOutFileName() != null) {
            commandLine = commandLine + " > "+(new File(execDescriptor.getStdOutFileName())).getPath();
        }
        if (execDescriptor.getStdErrFileName() != null) {
            commandLine = commandLine + " 2> "+(new File(execDescriptor.getStdErrFileName())).getPath();
        }

        File pidFile = File.createTempFile("exec-", ".pid");
        File generatedShellScript = File.createTempFile("exec-", ".sh");

        /* Delete the generated file on exit */
        generatedShellScript.deleteOnExit();

        URL url = Util.getResource(template);
        StringBuilder sb = new StringBuilder();
        BufferedReader in =
            new BufferedReader(new InputStreamReader(url.openStream()));
        String str;
        while ((str = in.readLine()) != null) {
            str = Util.replace(str, "${command}", command);
            str = Util.replace(str, "${pidFile}", FileUtils.getFilePath(pidFile));
            str = Util.replace(str, "${commandLine}", commandLine);
            sb.append(str).append("\n");
        }
        in.close();

        Util.writeFile(sb.toString(), generatedShellScript);
        Util.chmodX(generatedShellScript);        

        String toExec = FileUtils.getFilePath(generatedShellScript);
        logger.info("Executing command [" + commandLine + "]");
        ProcessBuilder pb = new ProcessBuilder(toExec);

        Map<String, String> declaredEnv = execDescriptor.getEnvironment();
        Map<String, String> environment = pb.environment();
        for(Map.Entry<String, String> entry : environment.entrySet()) {
            String value = declaredEnv.get(entry.getKey());
            if(value!=null) {
                String oldValue = entry.getValue();
                String setValue = oldValue+File.pathSeparator+value;
                environment.put(entry.getKey(), setValue);
                logger.info(entry.getKey()+" was ["+oldValue+"], " +
                            "now ["+environment.get(entry.getKey())+"]");
            }
        }
        environment.putAll(execDescriptor.getEnvironment());
        if(logger.isLoggable(Level.FINE))
            logger.fine("Process Builder's environment="+environment);

        if(workingDirectory!=null) {
            pb = pb.directory(new File(workingDirectory));
            if(logger.isLoggable(Level.FINE))
                logger.fine("Process Builder's working directory set to ["+
                            pb.directory().getCanonicalFile()+"]");

        }
        pb.redirectErrorStream(true);
        Process process = pb.start();        
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
            logger.warning("Non fatal, " +
                           "could not delete "+FileUtils.getFilePath(pidFile));

        PosixProcessManager procMgr =
                new PosixProcessManager(process,
                                        pid,
                                        execDescriptor.getStdOutFileName(),
                                        execDescriptor.getStdErrFileName());
        procMgr.setCommandFile(generatedShellScript);
        procMgr.setCommandLine(commandLine);
        return procMgr;
    }
}
