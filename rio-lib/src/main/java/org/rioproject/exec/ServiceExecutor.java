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

import com.sun.jini.config.Config;
import net.jini.config.Configuration;
import net.jini.config.ConfigurationException;
import net.jini.config.ConfigurationProvider;
import org.rioproject.core.jsb.ServiceBean;
import org.rioproject.core.jsb.ServiceBeanContext;
import org.rioproject.deploy.ServiceBeanInstantiationException;
import org.rioproject.jmx.JMXConnectionUtil;
import org.rioproject.jmx.JMXUtil;
import org.rioproject.jsb.ServiceElementUtil;
import org.rioproject.opstring.ServiceElement;
import org.rioproject.sla.SLA;
import org.rioproject.system.ComputeResourceUtilization;
import org.rioproject.system.MeasuredResource;
import org.rioproject.system.SystemWatchID;
import org.rioproject.system.capability.PlatformCapability;
import org.rioproject.system.measurable.SigarHelper;
import org.rioproject.system.measurable.cpu.CPU;
import org.rioproject.system.measurable.cpu.ProcessCPUHandler;
import org.rioproject.system.measurable.memory.Memory;
import org.rioproject.system.measurable.memory.ProcessMemoryMonitor;
import org.rioproject.watch.WatchDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.MBeanServerConnection;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.RuntimeMXBean;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Provides support to execute an external service. If the external service is
 * a Java Virtual Machine, this utility will try to attach to the Java Virtual
 * Machine using the
 * <a href="http://java.sun.com/javase/6/docs/technotes/guides/attach/index.html">
 * JMX Attach API</a>. This is only possible using Java 6 or greater.
 * If the runtime is Java 5, external service utilization monitoring is not
 * provided.
 *
 * <p>In order to obtain the process identifier required to attach to
 * the exec'd JVM, the external process may create a pid file, containing the
 * process identifier of the process. If this is the case, then configuring the
 * deployment to declare the path of the produced pid file can be done.
 *
 * <p>If the exec'd process does not create a pid file,
 * <a href="http://www.hyperic.com/products/sigar.html">SIGAR</a>
 * is used to traverse the process tree to identify the parent process that
 * exec'd the child JVM. If the parent process can be identified
 * (see {@link org.rioproject.system.measurable.SigarHelper#matchChild(int, String[])} the
 * <tt>ServiceExecutor</tt> will attach to the JVM, and monitor CPU and Memory
 * utilization. <a href="http://www.hyperic.com/products/sigar.html">SIGAR</a>
 * is also used to monitor the real memory used by the exec'd JVM.
 *
 * <p>This class also provides configuration support for the following entries:
 *
 * <ul>
 * <li><span
 * style="font-weight: bold; font-family: courier new,courier,monospace;">shellTemplate</span><br
 *style="font-weight: bold; font-family: courier new,courier,monospace;">
 * <table style="text-align: left; width: 100%;" border="0"
 * cellpadding="2" cellspacing="2">
 * <tbody>
 * <tr>
 * <td
 * style="vertical-align: top; text-align: right; font-weight: bold;">
 * Type:<br>
 * </td>
 * <td style="vertical-align: top;">String<br>
 * </td>
 * </tr>
 * <tr>
 * <td
 * style="vertical-align: top; text-align: right; font-weight: bold;">
 * Default:<br>
 * </td>
 * <td style="vertical-align: top;"><span
 * style="font-family: monospace;">exec-template.sh</span><br>
 * </td>
 * </tr>
 * <tr>
 * <td
 * style="vertical-align: top; text-align: right; font-weight: bold;">
 * Description:<br>
 * </td>
 * <td style="vertical-align: top;">The template to use for
 * generating a script to exec a command. The script template must be
 * loadable as a resource, and must provide the following token that get
 * replaced by runtime values:<br>
 * <br>
 * <span style="font-family: monospace; font-weight: bold;">${command}</span>
 * The command to execute. This token eirs placed by the command to
 * execute.<br>
 * <span style="font-family: monospace;">$<span
 * style="font-weight: bold;">{pidFile}</span></span> Rio creates a file
 * that stores the pid of the executed command. This token is replaced by
 * the name of the pid file to create<br>
 * <span style="font-family: monospace; font-weight: bold;">${commandLine}</span>
 * This token is replaced by the actual command line that is created to
 * 'exec' the command above. Inoput arguments, standard error and output
 * are also part of the created command line<br>
 * </td>
 * </tr>
 * </tbody>
 * </table>
 * </li>
 * </ul>
 * <ul>
 * <li>
 * <span style="font-weight: bold; font-family: courier new,courier,monospace;">pidFileWaitTime</span>
 * <br style="font-weight: bold; font-family: courier new,courier,monospace;">
 * <table style="text-align: left; width: 100%;" border="0" cellpadding="2" cellspacing="2">
 * <tbody>
 * <tr>
 * <td style="vertical-align: top; text-align: right; font-weight: bold;">
 * Type:<br>
 * </td>
 * <td style="vertical-align: top;">int<br>
 * </td>
 * </tr>
 * <tr>
 * <td style="vertical-align: top; text-align: right; font-weight: bold;">
 * Default:<br>
 * </td>
 * <td style="vertical-align: top;"><span style="font-family: monospace;">60 </span><br>
 * </td>
 * </tr>
 * <tr>
 * <td style="vertical-align: top; text-align: right; font-weight: bold;">
 * Description:<br>
 * </td>
 * <td style="vertical-align: top;">
 * The amount of time to wait for an exec'd service to create a pid file.
 * The value is in seconds, where 60 seconds is the default. The minimum
 * value for this property is 5 seconds, the maximum allowed 5 minutes.<br>
 * <br>
 * This value represents the maximum amount of time to wait. If the pid file is
 * created prior to the timeout value, the utility proceeds immediately.<br>
 * </td>
 * </tr>
 * </tbody>
 * </table>
 * </li>
 * </ul>
 *
 * @author Dennis Reedy
 */
public class ServiceExecutor {
    private ServiceBean serviceBean;
    private ProcessManager processManager;
    private ExecDescriptor execDescriptor;
    private String shellTemplate;
    private SigarHelper sigar;
    private Memory memory;
    private CPU cpu;
    private long actualPID=-1;
    private MBeanServerConnection mbsc;
    private ServiceBeanContext context;
    private Configuration config;
    private static final String COMPONENT = ServiceExecutor.class.getPackage().getName();
    private int pidFileWaitTime = 60; // number of seconds
    static final Logger logger = LoggerFactory.getLogger(COMPONENT);

    public ServiceExecutor() {
        sigar = SigarHelper.getInstance();
    }

    public void setServiceBeanContext(final ServiceBeanContext context)
        throws ServiceBeanInstantiationException, IOException, ConfigurationException {
        this.context = context;
        this.config = context.getConfiguration();
        try {
            shellTemplate = (String)context.getConfiguration().getEntry(COMPONENT,
                                                                        "shellTemplate",
                                                                        String.class,
                                                                        null);
        } catch (ConfigurationException e) {
            logger.warn("Cannot get shell template from configuration, continue with default");
        }
        try {
            pidFileWaitTime = Config.getIntEntry(context.getConfiguration(),
                                                 COMPONENT,
                                                 "pidFileWaitTime",
                                                 60,    //default is 1 minute
                                                 5,     //minimum of 5 second wait
                                                 60 * 5); // max of 5 minute wait
        } catch(ConfigurationException e) {
            logger.warn("Getting pidFileWaitTime, using default", e);
        }
        execDescriptor = context.getServiceElement().getExecDescriptor();
        if(execDescriptor==null)
            throw new ServiceBeanInstantiationException("An ExecDescriptor is required " +
                                                "by the ServiceExecutor," +
                                                " unable to proceed.");

        String cmdLine = getCommandLine(execDescriptor);
        if(!cmdLine.startsWith(File.separator)) {
            PlatformCapability[] pCaps =
                context.getComputeResourceManager().getMatchedPlatformCapabilities();
            boolean matched = false;
            for(PlatformCapability pCap : pCaps) {
                if(pCap.getPath()!=null) {
                    File toExec = new File(pCap.getPath(), cmdLine);
                    if(toExec.exists() && toExec.canRead()) {
                        matched = true;
                        if(logger.isInfoEnabled()) {
                            logger.info("Adding PlatformCapability PATH [{}] to declared command line [{}]",
                                        pCap.getPath(), cmdLine);
                        }
                        
                        execDescriptor = Util.extendCommandLine(pCap.getPath(), execDescriptor);
                        break;
                    }
                }
            }
            if(!matched) {
                throw new ServiceBeanInstantiationException(
                    "ExecDescriptor with command line " +
                    "["+execDescriptor.getCommandLine()+"] " +
                    "cannot " +
                    "be executed, no associated PlatformCapability " +
                    "found");
            }
        } else {
            if(logger.isInfoEnabled()) {
                logger.info("Using command line [{}]",  execDescriptor.getCommandLine());
            }
        }

        File toExec = new File(execDescriptor.getCommandLine());
        if(!toExec.exists())
            throw new ServiceBeanInstantiationException("The command line ["+
                                                        execDescriptor.getCommandLine()+
                                                        "] can not be found, " +
                                                        "unable to continue. Check " +
                                                        "that the directory structure " +
                                                        "matches that as found on the " +
                                                        "executing platform. If the " +
                                                        "ServiceExec is a result of " +
                                                        "software downloading make sure " +
                                                        "that all installation is " +
                                                        "correct and that downloaded " +
                                                        "software has been extracted");
        exec();
        processManager.manage();
        String pidFileName = execDescriptor.getPidFile();
        if(pidFileName!=null) {
            logger.info("Try to obtain actual pid of exec'd process using pid file: "+pidFileName);
            long waited = 0;
            while(waited < pidFileWaitTime) {
                File pidFile = new File(pidFileName);
                if(pidFile.exists()) {
                    actualPID = readPidFromFile(pidFile);
                    break;
                }
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    logger.warn("Waiting for pid file to appear, abort wait", e);
                    break;
                }
                waited++;
            }
        }
        if(actualPID==-1) {
            long managedPID = processManager.getPid();
            long waited = 0;
            long t0 = System.currentTimeMillis();
            while(waited < 5) {
                String[] ids;
                try {
                    ids = VirtualMachineHelper.listIDs();
                } catch (Exception e) {
                    logger.error("Cannot load the Attach API", e);
                    break;
                }
                StringBuilder s = new StringBuilder();
                for(int i=0; i<ids.length; i++) {
                    if(i>0)
                        s.append(", ");
                    s.append(ids[i]);
                }
                System.out.println("JMX pids: ["+s.toString()+"]");
                long[] pids = new long[ids.length];
                for(int i=0; i<ids.length; i++)
                    pids[i] = new Long(ids[i]);

                /* First check to see if the actualPID is in the list of JMX managed pids */
                for(long pid : pids) {
                    if(pid==managedPID) {
                        actualPID = managedPID;
                        long t1 = System.currentTimeMillis();
                        System.out.println("Time waiting for process to be under JMX management: "+(t1/t0)+" milliseconds");
                        break;
                    }
                }
                if(actualPID!=-1)
                    break;
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    logger.warn("Waiting for pid to appear under JMX management, abort wait", e);
                    break;
                }
                waited++;
            }
        }
        if(sigar!=null) {
            if(actualPID==-1) {
                logger.info("Try to obtain actual pid of exec'd process using SIGAR");
                String[] ids = new String[0];
                try {
                    ids = VirtualMachineHelper.listIDs();
                } catch (Exception e) {
                    logger.error("Cannot load the Attach API", e);
                }
                actualPID = sigar.matchChild(processManager.getPid(), ids);
            }
        } else {
            if(actualPID==-1)
                logger.warn("No SIGAR support available, unable to obtain PID");
        }
        if(actualPID!=-1) {
            logger.info("PID of exec'd process obtained: "+actualPID);
            try {
                mbsc = JMXConnectionUtil.attach(Long.toString(actualPID));
                logger.info("JMX Attach succeeded to exec'd JVM with pid: "+actualPID);
                createSystemWatches();
                setThreadDeadlockDetector(context.getServiceElement());
                checkWatchDescriptors(context.getServiceElement());
            } catch(Exception e) {
                logger.warn("Could not attach to the exec'd JVM with PID: " +
                            actualPID +
                            ", continue service execution", e);
            }
        } else {
            logger.info("Could not obtain actual PID of exec'd process, " +
                        "process cpu and java memory utilization are not available");
        }

    }

    public ComputeResourceUtilization getComputeResourceUtilization() {
        List<MeasuredResource> mRes;
        if(memory!=null && cpu!=null) {
            mRes =  new ArrayList<MeasuredResource>();            
            mRes.add(memory.getMeasuredResource());
            mRes.add(cpu.getMeasuredResource());
        } else {
            mRes = Collections.unmodifiableList(new ArrayList<MeasuredResource>());
        }
        return new ComputeResourceUtilization("", "", "", mRes);
    }

    private void setThreadDeadlockDetector(final ServiceElement elem) {
        ServiceElementUtil.setThreadDeadlockDetector(elem, mbsc);
    }

    private void createSystemWatches() {
        if(sigar!=null) {
            int pidToUse = (int)(actualPID==-1?processManager.getPid():actualPID);
            memory = new Memory(config);
            ProcessMemoryMonitor memMonitor =
                (ProcessMemoryMonitor)memory.getMeasurableMonitor();
            memMonitor.setPID(pidToUse);
            if(mbsc!=null) {
                MemoryMXBean memBean =
                    getPlatformMXBeanProxy(mbsc,
                                           ManagementFactory.MEMORY_MXBEAN_NAME,
                                           MemoryMXBean.class);
                memMonitor.setMXBean(memBean);
            } else {
                memMonitor.setMXBean(null);
            }

            memory.start();

            cpu = new CPU(config, SystemWatchID.PROC_CPU, true);
            ProcessCPUHandler cpuMonitor = (ProcessCPUHandler)cpu.getMeasurableMonitor();
            cpuMonitor.setPID(pidToUse);
            OperatingSystemMXBean opSys =
                getPlatformMXBeanProxy(mbsc,
                                       ManagementFactory.OPERATING_SYSTEM_MXBEAN_NAME,
                                       OperatingSystemMXBean.class);
            cpuMonitor.setMXBean(opSys);
            RuntimeMXBean runtime =
                getPlatformMXBeanProxy(mbsc,
                                       ManagementFactory.RUNTIME_MXBEAN_NAME,
                                       RuntimeMXBean.class);
            cpuMonitor.setStartTime(runtime.getStartTime());
            cpu.start();

            context.getWatchRegistry().register(memory, cpu);
        }
    }

    private <T> T getPlatformMXBeanProxy(final MBeanServerConnection mbsc,
                                         final String name,
                                         final Class<T> mxBeanInterface) {
        return JMXUtil.getPlatformMXBeanProxy(mbsc, name, mxBeanInterface);
    }

    private void checkWatchDescriptors(ServiceElement elem) {
        SLA[] slas = elem.getServiceLevelAgreements().getServiceSLAs();
        for(SLA sla : slas) {
            WatchDescriptor[] wDescs = sla.getWatchDescriptors();
            for(WatchDescriptor wDesc : wDescs) {
                if(wDesc.getObjectName()!=null)
                    wDesc.setMBeanServerConnection(mbsc);
            }
        }
    }

    private String getCommandLine(final ExecDescriptor exec) {
        String cmd;
        if(exec.getWorkingDirectory()!=null) {
            String wd = exec.getWorkingDirectory();
            if(wd.endsWith(File.separator))
                cmd = wd + exec.getCommandLine();
            else
                cmd = wd + File.separator + exec.getCommandLine();
        } else {
            cmd = exec.getCommandLine();
        }

        return cmd;
    }

    @SuppressWarnings("unused")
    public void setExecDescriptor(final ExecDescriptor execDescriptor) {
        if (execDescriptor == null)
            throw new IllegalArgumentException("ExecDescriptor is null");
        this.execDescriptor = execDescriptor;
    }

    @SuppressWarnings("unused")
    public void setServiceBean(final ServiceBean serviceBean) {
        this.serviceBean = serviceBean;
    }

    @SuppressWarnings("unused")
    public void preDestroy() {
        terminate();
    }

    public ProcessManager exec() throws IOException {
        if(execDescriptor==null)
            throw new IllegalStateException("execDescriptor is not set");
        return exec(execDescriptor);
    }

    public ProcessManager exec(final ExecDescriptor exDesc) throws IOException {
        Shell shell = ShellFactory.createShell();
        if(shellTemplate!=null)
            shell.setShellTemplate(shellTemplate);

        processManager = shell.exec(exDesc);

        processManager.registerListener(new ProcessManager.Listener() {
            public void processTerminated(int pid) {
                if(logger.isDebugEnabled())
                    logger.debug("Process [{}] terminated", pid);
                if(serviceBean!=null)
                    serviceBean.destroy(true);
            }
        });
        return processManager;
    }

    /**
     * Close the shell and release all used resources
     */
    public synchronized void terminate() {
        if(memory!=null)
            memory.stop();
        if(cpu!=null)
            cpu.stop();
        if (processManager != null) {
            processManager.destroy(true);
        }
    }

    public long readPidFromFile(File f) throws IOException {
        long pid = -1;
        BufferedReader in = null;
        try {
            in = new BufferedReader(new FileReader(f));
            String line = in.readLine().trim();
            pid = Long.valueOf(line);
        } finally {
            if(in!=null)
                in.close();
        }
        return pid;
    }

    public static void main(String[] args) {
        ExecDescriptor exDesc;
        try {
            if(args.length>0 && args[0].endsWith("config")) {
                Configuration config = ConfigurationProvider.getInstance(args);
                exDesc = (ExecDescriptor) config.getEntry(COMPONENT,
                                                          "descriptor",
                                                          ExecDescriptor.class,
                                                          null);
            } else {
                exDesc = new ExecDescriptor();
                exDesc.setCommandLine("${RIO_HOME}/bin/cybernode");
                exDesc.setWorkingDirectory("${RIO_HOME}/bin");
                exDesc.setStdErrFileName("${RIO_HOME}/logs/cybernode.log");
                exDesc.setStdOutFileName("${RIO_HOME}/logs/cybernode.log");
            }
            final ServiceExecutor svcExecutor = new ServiceExecutor();
            try {
                ProcessManager manager = svcExecutor.exec(exDesc);
                manager.manage();
                new Thread(new Runnable() {
                    public void run() {
                        try {
                            Thread.sleep(10000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        //svcExecutor.terminate();
                    }
                }).start();
                manager.getProcess().waitFor();
                System.out.println("Manager returned from waitFor()");
            } catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        } catch (ConfigurationException e) {
            e.printStackTrace();
        }
    }
}
