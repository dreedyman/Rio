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

import net.jini.config.Configuration;
import net.jini.config.ConfigurationException;
import net.jini.config.ConfigurationProvider;
import org.rioproject.core.JSBInstantiationException;
import org.rioproject.core.ServiceElement;
import org.rioproject.core.jsb.ServiceBean;
import org.rioproject.core.jsb.ServiceBeanContext;
import org.rioproject.exec.support.PosixShell;
import org.rioproject.jmx.JMXConnectionUtil;
import org.rioproject.jmx.JMXUtil;
import org.rioproject.jsb.ServiceElementUtil;
import org.rioproject.sla.SLA;
import org.rioproject.system.ComputeResourceUtilization;
import org.rioproject.system.MeasuredResource;
import org.rioproject.system.OperatingSystemType;
import org.rioproject.system.SystemWatchID;
import org.rioproject.system.capability.PlatformCapability;
import org.rioproject.system.measurable.SigarHelper;
import org.rioproject.system.measurable.cpu.CPU;
import org.rioproject.system.measurable.cpu.ProcessCPUHandler;
import org.rioproject.system.measurable.memory.Memory;
import org.rioproject.system.measurable.memory.ProcessMemoryMonitor;
import org.rioproject.watch.WatchDescriptor;

import javax.management.MBeanServerConnection;
import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.RuntimeMXBean;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Provides support to execute an external service. If the external service is
 * a Java Virtual Machine, this utility will try to attach to the Java Virtual
 * Machine using the
 * <a href="http://java.sun.com/javase/6/docs/technotes/guides/attach/index.html">
 * JMX Attach API</a>. This is only possible using Java 6 or greater.
 * If the runtime is Java 5, external service utilization monitoring is not
 * provided.
 *
 * <p>In order to obtain the identifier required to attach to
 * the exec'd JVM, <a href="http://www.hyperic.com/products/sigar.html">SIGAR</a>
 * is used to traverse the process tree to identify the parent process that
 * exec'd the child JVM. If the parent process can be identified
 * (see {@link org.rioproject.system.measurable.SigarHelper#matchChild} the
 * <tt>ServiceExecutor</tt> will attach to the JVM, and monitor CPU and Memory
 * utilization. <a href="http://www.hyperic.com/products/sigar.html">SIGAR</a>
 * is also used to monitor the real memory used by the exec'd JVM.
 *
 * <p><b>Notes:</b>
 * <ul>
 * <li>
 * <a href="http://www.hyperic.com/products/sigar.html">Hyperic SIGAR</a>
 * is licensed under the GPL with a FLOSS license exception, allowing it to be
 * included with the Rio Apache License v2 distribution. If for some reason the
 * GPL cannot be used with your distribution of Rio,
 * remove the <tt>RIO_HOME/lib/hyperic</tt> directory.
 * <li>If <a href="http://www.hyperic.com/products/sigar.html">SIGAR</a>
 * is removed, external service utilization monitoring is not provided.
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
    static final Logger logger = Logger.getLogger(COMPONENT);

    public ServiceExecutor() {
        sigar = SigarHelper.getInstance();
    }

    public void setServiceBeanContext(ServiceBeanContext context)
        throws JSBInstantiationException, IOException, ConfigurationException {
        this.context = context;
        this.config = context.getConfiguration();
        try {
            shellTemplate = (String)context.getConfiguration().getEntry(COMPONENT,
                                                                        "shellTemplate",
                                                                        String.class,
                                                                        null);
        } catch (ConfigurationException e) {
            logger.warning("Cannot get shell template from configuration, " +
                           "continue with default");
        }
        execDescriptor = context.getServiceElement().getExecDescriptor();
        if(execDescriptor==null)
            throw new JSBInstantiationException("An ExecDescriptor is required " +
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
                        if(logger.isLoggable(Level.INFO)) {
                            logger.info("Adding PlatformCapability PATH " +
                                        "["+pCap.getPath()+"] to declared " +
                                        "command line " +
                                        "["+cmdLine+"]");
                        }
                        
                        execDescriptor = Util.extendCommandLine(pCap.getPath(),
                                                                execDescriptor);
                        break;
                    }
                }
            }
            if(!matched) {
                throw new JSBInstantiationException(
                    "ExecDescriptor with command line " +
                    "["+execDescriptor.getCommandLine()+"] " +
                    "cannot " +
                    "be executed, no associated PlatformCapability " +
                    "found");
            }
        } else {
            if(logger.isLoggable(Level.INFO)) {
                logger.info("Using command line " +
                            "["+execDescriptor.getCommandLine()+"]");
            }
        }

        File toExec = new File(execDescriptor.getCommandLine());
        if(!toExec.exists())
            throw new JSBInstantiationException("The command line ["+
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
        if(sigar!=null) {
            String[] ids = JMXConnectionUtil.listIDs();
            actualPID = sigar.matchChild(processManager.getPid(), ids);
            if(actualPID!=-1) {
                logger.info("PID of exec'd process obtained: "+actualPID);
                try {
                    mbsc = JMXConnectionUtil.attach(Long.toString(actualPID));
                    logger.info("JMX Attach succeeded to exec'd JVM with pid: "+actualPID);
                    createSystemWatches();
                    setThreadDeadlockDetector(context.getServiceElement());
                    checkWatchDescriptors(context.getServiceElement());
                } catch(Exception e) {
                    logger.log(Level.WARNING,
                               "Could not attach to the exec'd " +
                               "JVM with pid: "+actualPID+", " +
                               "continue service execution",
                               e);
                }
            } else {
                logger.info("Could not obtain actual pid of " +
                            "exec'd process, process cpu and " +
                            "java memory utilization are not available");
            }
        } else {
            logger.warning("No SIGAR support available");
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

    private void setThreadDeadlockDetector(ServiceElement elem) {
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

    private <T> T getPlatformMXBeanProxy(MBeanServerConnection mbsc,
                                         String name,
                                         Class<T> mxBeanInterface) {
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

    private String getCommandLine(ExecDescriptor exec) {
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

    public void setExecDescriptor(ExecDescriptor execDescriptor) {
        if (execDescriptor == null)
            throw new IllegalArgumentException("ExecDescriptor is null");
        this.execDescriptor = execDescriptor;
    }

    public void setServiceBean(ServiceBean serviceBean) {
        this.serviceBean = serviceBean;
    }

    public void preDestroy() {
        terminate();
    }

    public ProcessManager exec() throws IOException {
        if(execDescriptor==null)
            throw new IllegalStateException("execDescriptor is not set");
        return exec(execDescriptor);
    }

    public ProcessManager exec(ExecDescriptor exDesc) throws IOException {
        if (!OperatingSystemType.isWindows()) {
            Shell shell = new PosixShell();
            if(shellTemplate!=null)
                shell.setShellTemplate(shellTemplate);
            processManager = shell.exec(exDesc);

        } else {
            throw new UnsupportedOperationException("ServiceExecutor " +
                                                    "support not provided for " +
                                                    "Windows");
        }
        processManager.registerListener(new ProcessManager.Listener() {
            public void processTerminated(int pid) {
                if(logger.isLoggable(Level.FINE))
                    logger.fine("Process ["+pid+"] terminated");
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
                manager.waitFor();
                System.out.println("Manager returned from waitFor()");
            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (ConfigurationException e) {
            e.printStackTrace();
        }
    }
}
