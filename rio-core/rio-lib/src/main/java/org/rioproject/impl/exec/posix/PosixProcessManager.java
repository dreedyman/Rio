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
package org.rioproject.impl.exec.posix;

import org.rioproject.impl.exec.ProcessManager;
import org.rioproject.impl.exec.Util;
import org.rioproject.impl.util.FileUtils;
import org.rioproject.impl.util.StreamRedirector;
import org.rioproject.impl.system.OperatingSystemType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.URL;

/**
 * A ProcessManager implementation for posix compliant systems
 *
 * @author Dennis Reedy
 */
public class PosixProcessManager extends ProcessManager {
    private ProcessListener processListener;
    private boolean terminated = false;
    private File commandFile;
    private String commandLine;
    private static final String COMPONENT = PosixProcessManager.class.getPackage().getName();
    private static final String KILL_SCRIPT="ps-kill-template.sh";
    private static final String PROC_STATUS_SCRIPT="proc-status-template.sh";
    static final Logger logger = LoggerFactory.getLogger(COMPONENT);

    /**
     * Create a PosixProcessManager
     *
     * @param process The {@link Process} the ProcessManager will manage
     * @param pid The process ID of the started Process
     * @param stdOutFileName The file name to redirect standard output to
     * @param stdErrFileName The file name to redirect standard error to
     */
    public PosixProcessManager(Process process,
                               int pid,
                               String stdOutFileName,
                               String stdErrFileName) {
        super(process, pid);
        handleRedirects(stdOutFileName, stdErrFileName);
    }

    /**
     * Set the generated command file to delete
     *
     * @param commandFile The script that started the process this
     * process manager is managing. Upon exit, delete this file
     */
    public void setCommandFile(File commandFile) {
        this.commandFile = commandFile;
    }

    /**
     * Set the command line that was executed
     *
     * @param commandLine The command line that was executed, and as a result
     * of it's execution this manager created. If set, this will be useful for
     * logging purposes.
     */
    public void setCommandLine(String commandLine) {
        this.commandLine = commandLine;
    }

    /**
     * Manage the Process
     *
     * @throws java.io.IOException if the process management utility cannot be created
     */
    public void manage() throws IOException {
        processListener = new ProcessListener();
        processListener.start();
        logger.info("Managing process ["+getPid()+"], command ["+commandLine+"]");
    }

    /**
     * Destroy the managed process
     *
     * @param includeChildren If true, destroy all child processes as well.
     * This method will look for all child processes that have a parent process
     * ID of the managed process and forcibly terminate them.
     */
    public void destroy(boolean includeChildren) {
        if(terminated)
            return;

        try {
            if(includeChildren) {
                File killFile = genKillFile();
                Util.runShellScript(killFile);
            } else {
                getProcess().destroy();
            }
            if(processListener!=null)
                processListener.interrupt();
        } catch (IOException e) {
            logger.warn("Could not completely terminate process and process children, will attempt to close stdout and stderr",
                       e);
        }
        try {
            getProcess().waitFor();
            if(processListener!=null)
                processListener.cleanFiles();
        } catch (InterruptedException e) {
            logger.warn("process.waitFor() was interrupted, continuing");
        }
        if (outputStream != null)
            outputStream.interrupt();
        if (errorStream != null)
            errorStream.interrupt();

        Util.close(getProcess().getOutputStream());
        Util.close(getProcess().getInputStream());
        Util.close(getProcess().getErrorStream());
        getProcess().destroy();

        if(commandFile!=null) {
            if(commandFile.delete()) {
                if(logger.isTraceEnabled()) {
                    logger.trace("Command file [{}] for [{}], command [{}]", commandFile.getName(), getPid(), commandLine);
                }
            }
        }
        terminated = true;
    }

    private File genProcStatusScript(File procStatusFile) throws IOException {
        int pid = getPid();
        File procStatusScript = new File(System.getProperty("java.io.tmpdir"),
                                         "proc-status-"+pid+".sh");
        procStatusScript.deleteOnExit();
        URL url = Util.getResource(PROC_STATUS_SCRIPT);

        String sPid = Integer.toString(pid);
        String processStatusFile = FileUtils.getFilePath(procStatusFile);
        StringBuilder sb = new StringBuilder();
        String str;
        BufferedReader in =
            new BufferedReader(new InputStreamReader(url.openStream()));
        while ((str = in.readLine()) != null) {
            str = Util.replace(str, "${pid}", sPid);
            str = Util.replace(str, "${process_status_file}", processStatusFile);
            sb.append(str).append("\n");
        }
        in.close();

        Util.writeFile(sb.toString(), procStatusScript);
        Util.chmodX(procStatusScript);
        return procStatusScript;
    }

    private File genKillFile() throws IOException {
        int pid = getPid();
        File killFile = new File(System.getProperty("java.io.tmpdir"),
                                 "ps-kill-"+pid+".sh");
        killFile.deleteOnExit();
        URL url = Util.getResource(KILL_SCRIPT);

        String sPid = Integer.toString(pid);
        StringBuilder sb = new StringBuilder();
        String str;
        BufferedReader in =
            new BufferedReader(new InputStreamReader(url.openStream()));
        String psOptions = "xo";
        if(OperatingSystemType.isSolaris())
            psOptions = "-e -o";
        while ((str = in.readLine()) != null) {
            str = Util.replace(str, "${psOptions}", psOptions);
            str = Util.replace(str, "${pid}", sPid);
            sb.append(str).append("\n");
        }
        in.close();

        Util.writeFile(sb.toString(), killFile);
        Util.chmodX(killFile);
        return killFile;
    }

    /**
     * Waits for a process to exit
     */
    class ProcessListener extends Thread {
        File procStatusFile;
        File procStatusScript;

        ProcessListener() throws IOException {
            procStatusFile = new File(System.getProperty("java.io.tmpdir"),
                                      "proc-stat-"+getPid());            
            procStatusScript = genProcStatusScript(procStatusFile);
            procStatusScript.deleteOnExit();
            Util.runShellScript(procStatusScript, false);
        }

        void cleanFiles() {
            if(procStatusScript!=null) {
                if(procStatusScript.delete()) {
                    if(logger.isDebugEnabled())
                        logger.debug("Process status script for pid [{}], command [{}] deleted", getPid(), commandLine);
                }

            }
            if(procStatusFile!=null)
                if(procStatusFile.delete()) {
                    if(logger.isDebugEnabled())
                        logger.debug("Process stats file for pid [{}], command [{}] deleted", getPid(), commandLine);
                }
        }
        
        public void run() {
            while(!isInterrupted()) {
                BufferedReader in = null;
                try {
                    if(!procStatusFile.exists()) {
                        continue;
                    }
                    in = new BufferedReader(new FileReader(procStatusFile));
                    String s = in.readLine();
                    if(s==null)
                        continue;
                    //System.out.println("process ["+pid+"] status="+s);
                    if(s!=null) {
                        if(Integer.parseInt(s)!=0) {
                            if(logger.isDebugEnabled()) {
                                logger.debug("Process status for pid [{}], command [{}] is: {}",
                                             getPid(), commandLine, Integer.parseInt(s));
                            }
                            break;
                        }
                    }
                } catch (IOException e) {
                    logger.warn("Non fatal exception trying to read " +
                                   "process status file "+
                                   e.getClass().getName()+": "+e.getMessage());
                } finally {
                    if(in!=null)
                        try {
                            in.close();
                        } catch (IOException e) {
                            if(logger.isTraceEnabled()) {
                                logger.trace("Problem closing "+procStatusFile.getName(), e);
                            }
                        }
                }
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    if(logger.isTraceEnabled()) {
                        logger.trace("Interrupted", e);
                    }
                }
            }
            if(logger.isDebugEnabled())
                logger.info("Process ["+getPid()+"] terminated for command ["+commandLine+"]");
            notifyOnTermination();
            if(procStatusFile.delete() && logger.isDebugEnabled())
                logger.debug("Process stats file [{}] removed for command [{}]", getPid(), commandLine);
        }
    }
}
