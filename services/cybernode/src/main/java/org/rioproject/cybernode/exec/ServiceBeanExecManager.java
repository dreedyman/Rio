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
package org.rioproject.cybernode.exec;

import com.sun.jini.config.Config;
import net.jini.config.Configuration;
import net.jini.config.ConfigurationException;
import net.jini.core.lookup.ServiceID;
import net.jini.export.Exporter;
import org.rioproject.config.Constants;
import org.rioproject.config.ExporterConfig;
import org.rioproject.core.JSBInstantiationException;
import org.rioproject.core.OperationalStringManager;
import org.rioproject.core.ServiceBeanInstance;
import org.rioproject.core.ServiceElement;
import org.rioproject.core.jsb.DiscardManager;
import org.rioproject.core.provision.ServiceRecord;
import org.rioproject.cybernode.ServiceBeanContainer;
import org.rioproject.exec.ExecDescriptor;
import org.rioproject.exec.JVMOptionChecker;
import org.rioproject.exec.ProcessManager;
import org.rioproject.exec.support.PosixShell;
import org.rioproject.fdh.FaultDetectionListener;
import org.rioproject.fdh.JMXFaultDetectionHandler;
import org.rioproject.jmx.JMXConnectionUtil;
import org.rioproject.resources.util.FileUtils;
import org.rioproject.resources.util.PropertyHelper;
import org.rioproject.resources.util.RMIServiceNameHelper;
import org.rioproject.system.capability.PlatformCapability;

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.rmi.NotBoundException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.ExportException;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Creates and manages a service bean in it's own process
 *
 * @author Dennis Reedy
 */
public class ServiceBeanExecManager {
    Object identifier;
    ServiceElement sElem;
    ServiceBeanContainer container;
    ServiceBeanExecutor execHandler;
    ServiceBeanInstance instance;
    ForkedServiceBeanListener forkedServiceListener;
    ServiceRecord serviceRecord;
    ProcessManager manager;
    /** The amount of time to wait for a forked service to be created */
    private int forkedServiceWaitTime = 60; // number of seconds
    static final String CONFIG_COMPONENT = "service.load";
    static final String COMPONENT = "org.rioproject.cybernode";
    static final Logger logger = Logger.getLogger(COMPONENT);

    public ServiceBeanExecManager(Object identifier,
                                  ServiceElement sElem,
                                  ServiceBeanContainer container) {
        this.identifier = identifier;
        this.sElem = sElem;
        this.container = container;
        try {
            forkedServiceWaitTime =
                    Config.getIntEntry(container.getSharedConfiguration(),
                                       CONFIG_COMPONENT,
                                       "forkedServiceWaitTime",
                                       60,    //default is 1 minute
                                       5,     //minimum of 5 second wait
                                       60*5); // max of 5 minute wait
        } catch(ConfigurationException e) {
            logger.log(Level.WARNING,
                       "Getting forkedServiceWaitTime, using default",
                       e);
        }
    }

    public ServiceBeanExecutor getServiceBeanExecutor() {
        return execHandler;
    }

    public ServiceBeanInstance exec(ServiceElement sElem,
                                    OperationalStringManager opStringMgr,
                                    DiscardManager discardManager,
                                    PlatformCapability[] installedpCaps) throws Exception {
        ExecDescriptor exDesc = new ExecDescriptor();

        String rioHome = System.getProperty("RIO_HOME");
        if(rioHome==null)
            throw new JSBInstantiationException("Cannot exec service " +
                                                "["+sElem.getName()+"], " +
                                                "unknown RIO_HOME system " +
                                                "property");
        File rioHomeBin = new File(rioHome+File.separator+"bin");
        if(!rioHomeBin.exists())
            throw new JSBInstantiationException("Cannot exec service " +
                                                "["+sElem.getName()+"], " +
                                                "the RIO_HOME/bin directory " +
                                                "does not exist " +
                                                "["+rioHome+File.separator+"bin]");
        if(!rioHomeBin.isDirectory())
            throw new JSBInstantiationException("Cannot exec service " +
                                                "["+sElem.getName()+"], " +
                                                "RIO_HOME/bin does not point to a " +
                                                "directory " +
                                                "["+rioHome+File.separator+"bin]");

        /* Create a normalized service name, translating " " to "_" and
         * appending the instance ID to the name. This will be used for the
         * log name and the registry bind name */
        String normalizedServiceName = RMIServiceNameHelper.createNormalizedServiceName(sElem);
        String serviceBindName = RMIServiceNameHelper.createBindName(sElem);

        /* Get the Cybernode's RMI Registry */
        String sPort = System.getProperty(Constants.REGISTRY_PORT);
        int regPort = Integer.parseInt(sPort);

        String logDir = getLogDirectory(container.getSharedConfiguration(),
                                        sElem.getOperationalStringName());
        if(!logDir.endsWith("/"))
            logDir = logDir+"/";

        /* Create command line */
        exDesc.setCommandLine(getJava());

        /* Create input args */
        StringBuilder inputArgsBuilder = new StringBuilder();
        inputArgsBuilder.append(getClassPath());
        String jvmOptions = (sElem.getExecDescriptor()==null? null:
                             sElem.getExecDescriptor().getInputArgs());
        inputArgsBuilder.append(getInputArgs(sElem.getName(),
                                             serviceBindName,
                                             sPort,
                                             jvmOptions,
                                             logDir));
        inputArgsBuilder.append(getMainClass());
        inputArgsBuilder.append(getStarterConfig(rioHome));

        exDesc.setInputArgs(inputArgsBuilder.toString());
        exDesc.setWorkingDirectory(FileUtils.getFilePath(rioHomeBin));

        /* If we have an exec descriptor, make add any environment settings the
         * service has declared */
        if(sElem.getExecDescriptor()!=null) {
            Map<String, String> env = sElem.getExecDescriptor().getEnvironment();
            for(Map.Entry<String, String> entry : env.entrySet()) {
                env.put(entry.getKey(),
                        PropertyHelper.expandProperties(entry.getValue()));
            }
            exDesc.setEnvironment(env);
        } 

        String serviceLog = logDir+normalizedServiceName+".log";
        exDesc.setStdErrFileName(serviceLog);
        exDesc.setStdOutFileName(serviceLog);

        try {
            Registry registry = LocateRegistry.getRegistry(regPort);
            forkedServiceListener =
                new ForkedServiceBeanListener(discardManager);
            ServiceBeanExecListener listener =
                forkedServiceListener.getServiceBeanExecListener();
            long start = System.currentTimeMillis();

            PosixShell shell = new PosixShell();
            try {
                String shellTemplate =
                    (String)container.getSharedConfiguration().getEntry(COMPONENT,
                                                                        "serviceBeanExecShellTemplate",
                                                                        String.class,
                                                                        null);
                if(shellTemplate!=null)
                    shell.setShellTemplate(shellTemplate);
            } catch (ConfigurationException e) {
                logger.warning("Cannot get shell template from configuration, " +
                               "continue with default");
            }
            manager = shell.exec(exDesc);

            forkedServiceListener.setName(serviceBindName);
            forkedServiceListener.setRegistryPort(regPort);

            long wait = 0;
            do {
                try {
                    execHandler =
                        (ServiceBeanExecutor)registry.lookup(serviceBindName);
                    int execRegPort = execHandler.getRegistryPort();
                    forkedServiceListener.createFDH(execRegPort);
                    execHandler.setUuid(container.getUuid());
                    execHandler.setServiceBeanExecListener(listener);

                    if(installedpCaps!=null && installedpCaps.length>0)
                        execHandler.applyPlatformCapabilities(installedpCaps);
                    instance = execHandler.instantiate(sElem, opStringMgr);
                    long activationTime = System.currentTimeMillis()-start;
                    if(wait>0)
                        System.out.print(" ");
                    //System.out.println();
                    logger.info("Forked instance created for ["+serviceBindName+"], " +
                                "pid=["+manager.getPid()+"], " +
                                "activation time="+activationTime+" ms");
                    break;
                } catch (NotBoundException e) {
                    System.out.print(".");
                    try {
                        Thread.sleep(1000);
                        wait++;
                    } catch (InterruptedException e1) {
                        logger.warning("Interrupted waiting for" +
                                       "ServiceBean ["+serviceBindName+"] to " +
                                       "register into Registry");
                    }
                }
            } while(wait<forkedServiceWaitTime);

            if(wait>=forkedServiceWaitTime) {
                logger.warning("Timed out waiting for ["+serviceBindName+"]. " +
                               "Waited ["+wait+"] seconds, configured wait " +
                               "time is ["+forkedServiceWaitTime+"] seconds. " +
                               "Killing spawned process and unregistering from local " +
                               "registry. Check the service's output log " +
                               "["+serviceLog+"] to determine root cause(s)");
                manager.destroy(true);
                throw new JSBInstantiationException("Failed to fork");
            }

        } catch (Exception e) {
            unregister(regPort, serviceBindName);
            logger.info("Terminate process for ServiceBean ["+serviceBindName+"]");
            manager.destroy(true);
            throw e;
        }
        return instance;
    }

    public ServiceRecord getServiceRecord() {
        return serviceRecord;
    }

    private String getJava() {
        StringBuilder jvmBuilder = new StringBuilder();
        jvmBuilder.append(System.getProperty("java.home"));
        jvmBuilder.append(File.separator);
        jvmBuilder.append("bin");
        jvmBuilder.append(File.separator);
        jvmBuilder.append("java");
        jvmBuilder.append(" ");
        return jvmBuilder.toString();
    }

    private String getOption(String option, String value) {
        StringBuilder optionBuilder = new StringBuilder();
        optionBuilder
            .append("-D")
            .append(option)
            .append("=")
            .append(value);
        return optionBuilder.toString();
    }

    private String getClassPath() {
        StringBuilder cpBuilder = new StringBuilder();
        RuntimeMXBean runtime = ManagementFactory.getRuntimeMXBean();
        cpBuilder.append("-cp");
        cpBuilder.append(" ");
        cpBuilder.append(runtime.getClassPath());
        cpBuilder.append(" ");
        return cpBuilder.toString();
    }

    private String getInputArgs(String serviceName,
                                String serviceBindName,
                                String sRegPort,
                                String declaredJVMOptions,
                                String logDir) {
        StringBuilder extendedJVMOptions = new StringBuilder();
        if(declaredJVMOptions!=null)
            extendedJVMOptions.append(declaredJVMOptions);
        extendedJVMOptions.append(" ");
        extendedJVMOptions.append(getOption("RIO_LOG_DIR", logDir));
        extendedJVMOptions.append(" ");
        extendedJVMOptions.append("-XX:HeapDumpPath=").append(logDir);
        StringBuilder argsBuilder = new StringBuilder();
        //String jvmInputArgs = JVMOptionChecker.getJVMInputArgs(declaredJVMOptions);
        String jvmInputArgs = JVMOptionChecker.getJVMInputArgs(extendedJVMOptions.toString());
        if(logger.isLoggable(Level.FINEST))
            logger.finest("Resulting JVM Options for service ["+serviceName+"]: "+jvmInputArgs);
        argsBuilder.append(jvmInputArgs);
        argsBuilder.append("-XX:OnOutOfMemoryError=\"kill -9 %p\"");
        argsBuilder.append(" ");
        /*argsBuilder.append("-verbose");
        argsBuilder.append(" ");*/
        argsBuilder.append(getOption(Constants.REGISTRY_PORT, sRegPort));
        if(System.getProperty(Constants.CODESERVER)!=null) {
            argsBuilder.append(" ");
            argsBuilder.append(getOption(Constants.CODESERVER,
                                         System.getProperty(Constants.CODESERVER)));
        }
        argsBuilder.append(" ");
        argsBuilder.append(getOption(Constants.SERVICE_BEAN_EXEC_NAME,
                                     serviceBindName));
        /*argsBuilder.append(" ");
        argsBuilder.append(getOption("RIO_LOG_DIR", logDir));*/
        argsBuilder.append(" ");
        return argsBuilder.toString();
    }

    private String getMainClass() {
        String mainClass = System.getProperty("rio.script.mainClass",
                                              "com.sun.jini.start.ServiceStarter");
        return mainClass+" ";
    }

    private String getStarterConfig(String rioHome) throws IOException {
        StringBuilder configBuilder = new StringBuilder();
        configBuilder
            .append(rioHome)
            .append(File.separator)
            .append("config")
            .append(File.separator)
            .append("start-service-bean-exec.groovy");
        File f = new File(configBuilder.toString());
        if(!f.exists())
            throw new IOException(configBuilder.toString()+" " +
                                  "does not exist, unable to fork service");
        return f.getCanonicalPath();
    }

    private void unregister(int regPort, String name) {
        try {
            Registry registry = LocateRegistry.getRegistry(regPort);
            registry.unbind(name);
            logger.info("Unbound failed ServiceBean fork for " +
                        "["+name+"]");
        } catch (Exception e1) {
            // ignore
        }
    }

    private String getLogDirectory(Configuration config,
                                   String opstringName) throws
                                                        ConfigurationException,
                                                        IOException {
        String serviceLogRootDirectory = (String)config.getEntry(COMPONENT,
                                                 "serviceLogRootDirectory",
                                                 String.class,
                                                 System.getProperty("java.io.tmpdir"));

        File rootDir = new File(serviceLogRootDirectory);
        FileUtils.checkDirectory(rootDir, "service log root");
        
        File serviceLogDir = new File(rootDir, "service_logs");
        FileUtils.checkDirectory(serviceLogDir, "service logs");

        String s = opstringName.replaceAll(" ", "_");
        File opstringDir = new File(serviceLogDir, s);
        FileUtils.checkDirectory(opstringDir, "opstring log root");
        return opstringDir.getAbsolutePath();
    }

    class ForkedServiceBeanListener implements ServiceBeanExecListener,
                                               FaultDetectionListener<ServiceID> {
        DiscardManager discardManager;
        Exporter exporter;
        ServiceBeanExecListener listener;
        int registryPort;
        int forkedRegistryPort;
        String name;

        ForkedServiceBeanListener(DiscardManager discardManager) {
            this.discardManager = discardManager;
        }

        void setRegistryPort(int registryPort) {
            this.registryPort = registryPort;
        }

        void createFDH(int forkedRegistryPort) {
            this.forkedRegistryPort = forkedRegistryPort;
            JMXFaultDetectionHandler fdh = new JMXFaultDetectionHandler();
            fdh.setInvocationDelay(3*1000);
            fdh.setRetryCount(0);
            fdh.setJMXConnection( JMXConnectionUtil.getJMXServiceURL(forkedRegistryPort,
                                                                     "localhost"));
            fdh.register(this);
            try {
                fdh.monitor();
            } catch (Exception e) {
                logger.log(Level.WARNING,
                           "Enable to monitor forked service ["+name+"]",
                           e);
            }
        }

        void setName(String name) {
            this.name = name;
        }

        ServiceBeanExecListener getServiceBeanExecListener() throws
                                                             ConfigurationException,
                                                             ExportException {
            if(listener==null) {
                exporter  = ExporterConfig.getExporter(container.getSharedConfiguration(),
                                                       "org.rioproject.cybernode",
                                                       "exporter");
                listener = (ServiceBeanExecListener)exporter.export(this);
            }

            return listener;
        }

        public void serviceInstantiated(ServiceRecord record) {
            serviceRecord = record;
            if(manager!=null)
                serviceRecord.setPid(manager.getPid());
        }

        public void serviceDiscarded(ServiceRecord record) {
            logger.info("Discard notification for "+
                        sElem.getOperationalStringName()+"/"+sElem.getName());
            discardManager.discard();
        }

        public void serviceFailure(Object service, ServiceID serviceID) {
            try {
                Registry registry = LocateRegistry.getRegistry(registryPort);
                registry.unbind(name);
                logger.info("Terminated ServiceBean fork for " +
                            "["+name+"] unbound from local registry");
            } catch (Exception e) {
                // ignore
            }
            serviceDiscarded(null);
        }
    }
}
