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
package org.rioproject.exec;

import org.rioproject.util.PropertyHelper;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Execution Attributes for an external service
 *
 * @author Dennis Reedy
 */
public class ExecDescriptor implements Serializable {
    static final long serialVersionUID = 1L;
    private String commandLine;
    private String workingDirectory;
    private final Map<String, String> environment = new HashMap<String, String>();
    private String stdOutFileName;
    private String stdErrFileName;
    private String inputArgs;
    private String pidFile;

    /**
     * Create an ExecDescriptor
     */
    public ExecDescriptor() {
    }

    /**
     * Create an ExecDescriptor
     *
     * @param commandLine The commandLine
     */
    public ExecDescriptor(String commandLine) {
        this(commandLine, null, null, null, null);
    }

    /**
     * Create an ExecDescriptor
     *
     * @param commandLine The commandLine
     * @param workingDirectory The working directory
     */
    public ExecDescriptor(String commandLine,
                          String workingDirectory) {
        this(commandLine, workingDirectory, null, null, null);
    }

    /**
     * Create an ExecDescriptor
     *
     * @param commandLine The commandLine
     * @param workingDirectory The working directory
     * @param stdOutFileName The file name to redirect standard output to
     * @param stdErrFileName The file name to redirect standard error to
     * @param inputArgs Input arguments
     */
    public ExecDescriptor(String commandLine,
                          String workingDirectory,
                          String stdOutFileName,
                          String stdErrFileName,
                          String inputArgs) {
        this.commandLine = commandLine;
        this.workingDirectory = workingDirectory;
        this.stdOutFileName = stdOutFileName;
        this.stdErrFileName = stdErrFileName;
        this.inputArgs = inputArgs;
    }

    /**
     * Get the command line
     *
     * @return The command line. All system properties of the form
     * <tt>${property}</tt> will be expanded
     */
    public String getCommandLine() {
        return PropertyHelper.expandProperties(commandLine);
    }

    public void setCommandLine(String commandLine) {
        this.commandLine = commandLine;
    }

    /**
     * Get the working directory
     *
     * @return The working directory. All system properties of the form
     * <tt>${property}</tt> will be expanded
     */
    public String getWorkingDirectory() {
        return (workingDirectory!=null?
                PropertyHelper.expandProperties(workingDirectory) : null);
    }

    public void setWorkingDirectory(String workingDirectory) {
        this.workingDirectory = workingDirectory;
    }

    public void setEnvironment(String[] env) {
        if(env == null)
            throw new IllegalArgumentException("env is null");
        if(env.length%2 != 0) {
            throw new IllegalArgumentException("environment elements has odd " +
                                               "length : "+env.length);
        } else {
            for(int i = 0; i < env.length; i += 2) {
                String name = env[i];
                String value = env[i+1];
                environment.put(name, value);
            }
        }
    }

    public void setEnvironment(Map<String, String>  env) {
        if(env == null)
            throw new IllegalArgumentException("env is null");
        environment.putAll(env);
    }

    /**
     * Get the environment variable settings for the subprocess.
     *
     * @return The environment variable settings for the subprocess.
     */
    public Map<String, String> getEnvironment() {
        return environment;
    }

    /**
     * Get the standard output file name
     *
     * @return The standard output file name. All system properties of the form
     * <tt>${property}</tt> will be expanded
     */
    public String getStdOutFileName() {
        return (stdOutFileName==null?null:
                PropertyHelper.expandProperties(stdOutFileName));
    }

    /**
     * A file name for the standard output stream. The file name is relative to
     * the Cybernode's log directory.
     * 
     * @param stdOutFileName The file name to redirect standard output stream to.
     */

    public void setStdOutFileName(String stdOutFileName) {
        this.stdOutFileName = stdOutFileName;
    }

    /**
     * Get the standard error file name
     *
     * @return The file name for standard error output. All system properties
     * of the form <tt>${property}</tt> will be expanded
     */
    public String getStdErrFileName() {
        return (stdErrFileName==null?null:
                PropertyHelper.expandProperties(stdErrFileName));
    }

    /**
     * A file name for the standard error stream. The file name is relative to
     * the Cybernode's log directory
     *
     * @param stdErrFileName The file name to redirect standard error stream to.
     */
    public void setStdErrFileName(String stdErrFileName) {
        this.stdErrFileName = stdErrFileName;
    }

    public String getInputArgs() {
        return inputArgs;
//        return inputArgs==null?null:PropertyHelper.expandProperties(inputArgs);
    }

    public void setInputArgs(String inputArgs) {
        this.inputArgs = inputArgs;
    }

    /**
     * Optional path of the file that contains the pid of of the process
     *
     * @return The path of the file that contains the pid of of the process. Maybe null if no
     */
    public String getPidFile() {
        return pidFile;
    }

    /**
     * Set the pid file
     *
     * @param pidFile The path of the file that contains the pid of of the process
     */
    public void setPidFile(String pidFile) {
        this.pidFile = pidFile;
    }
}
