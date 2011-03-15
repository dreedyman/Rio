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

import org.rioproject.exec.ProcessManager;
import org.rioproject.exec.Util;
import org.rioproject.resources.util.StreamRedirector;
import org.rioproject.resources.util.FileUtils;

import java.io.*;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A ProcessManager implementation for posix compliant systems
 *
 * @author Dennis Reedy
 */
public class PosixProcessManager extends ProcessManager {
    private StreamRedirector outputStream;
    private StreamRedirector errorStream;
    private ProcessListener processListener;
    private boolean terminated = false;
    private File commandFile;
    private String commandLine;
    private final Object processLock = new Object();
    private static final String COMPONENT = PosixProcessManager.class.getPackage().getName();
    private static final String KILL_SCRIPT="ps-kill-template.sh";
    private static final String PROC_STATUS_SCRIPT="proc-status-template.sh";
    static final Logger logger = Logger.getLogger(COMPONENT);

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
            logger.log(Level.WARNING,
                       "Could not completely terminate " +
                       "process and process children, will attempt to close " +
                       "stdout and stderr",
                       e);
        }
        try {
            getProcess().waitFor();
            if(processListener!=null)
                processListener.cleanFiles();
        } catch (InterruptedException e) {
            logger.warning("process.waitFor() was interrupted, continuing");
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
                if(logger.isLoggable(Level.FINEST)) {
                    logger.finest("Command file ["+commandFile.getName()+"] " +
                                  "for ["+getPid()+"], " +
                                  "command ["+commandLine+"]");
                }
            }
        }
        terminated = true;
    }

    /**
     * Waits for the process to exit
     */
    public void waitFor() {
        registerListener(new Listener() {
            public void processTerminated(int pid) {
                synchronized(processLock) {
                    processLock.notifyAll();
                }
            }
        });
        synchronized(processLock) {
            try {
                processLock.wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private File genProcStatusScript(File procStatusFile) throws IOException {
        int pid = getPid();
        File procStatusScript = new File(System.getProperty("java.io.tmpdir"),
                                         "proc-status-"+pid+".sh");
        procStatusScript.deleteOnExit();
        URL url = Util.getResource(PROC_STATUS_SCRIPT);

        String sPid = Integer.toString(pid);
        String processStatusFile = FileUtils.getFilePath(procStatusFile);
        StringBuffer sb = new StringBuffer();
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
        StringBuffer sb = new StringBuffer();
        String str;
        BufferedReader in =
            new BufferedReader(new InputStreamReader(url.openStream()));
        while ((str = in.readLine()) != null) {
            sb.append(Util.replace(str, "${pid}", sPid)).append("\n");
        }
        in.close();

        Util.writeFile(sb.toString(), killFile);
        Util.chmodX(killFile);
        return killFile;
    }

    private void handleRedirects(String stdOutFileName,
                                 String stdErrFileName) {
        if (stdOutFileName == null) {
            /*System.out*/
            outputStream = new StreamRedirector(getProcess().getInputStream(),
                                                System.out);
            outputStream.start();
        }
        if (stdErrFileName == null) {
            /*System.err*/
            errorStream = new StreamRedirector(getProcess().getErrorStream(),
                                               System.err);
            errorStream.start();
        }        
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
                    if(logger.isLoggable(Level.FINE))
                        logger.fine("Process status script for pid " +
                                    "["+getPid()+"], command " +
                                    "["+commandLine+"] deleted");
                }

            }
            if(procStatusFile!=null)
                if(procStatusFile.delete()) {
                    if(logger.isLoggable(Level.FINE))
                        logger.fine("Process stats file for pid " +
                                    "["+getPid()+"], command " +
                                    "["+commandLine+"] deleted");
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
                            if(logger.isLoggable(Level.FINE)) {
                                logger.fine("Process status for pid " +
                                            "["+getPid()+"], command " +
                                            "["+commandLine+"] is: "+
                                            Integer.parseInt(s));
                            }
                            break;
                        }
                    }
                } catch (IOException e) {
                    logger.warning("Non fatal exception trying to read " +
                                   "process status file "+
                                   e.getClass().getName()+": "+e.getMessage());
                } finally {
                    if(in!=null)
                        try {
                            in.close();
                        } catch (IOException e) {
                        }
                }
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {}
            }
            if(logger.isLoggable(Level.FINE))
                logger.info("Process ["+getPid()+"] terminated for command ["+commandLine+"]");
            notifyOnTermination();
            if(procStatusFile.delete() && logger.isLoggable(Level.FINE))
                logger.fine("Process stats file ["+getPid()+"] removed for command ["+commandLine+"]");
        }
    }
}
