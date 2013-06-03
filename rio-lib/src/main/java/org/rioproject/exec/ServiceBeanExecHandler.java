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
import net.jini.export.Exporter;
import net.jini.id.Uuid;
import net.jini.jeri.BasicILFactory;
import net.jini.jeri.BasicJeriExporter;
import org.rioproject.config.ExporterConfig;
import org.rioproject.core.jsb.DiscardManager;
import org.rioproject.cybernode.CybernodeLogUtil;
import org.rioproject.deploy.ServiceBeanInstance;
import org.rioproject.deploy.ServiceBeanInstantiationException;
import org.rioproject.deploy.ServiceRecord;
import org.rioproject.fdh.FaultDetectionListener;
import org.rioproject.opstring.OperationalStringManager;
import org.rioproject.opstring.ServiceElement;
import org.rioproject.resources.util.FileUtils;
import org.rioproject.resources.util.RMIServiceNameHelper;
import org.rioproject.rmi.RegistryUtil;
import org.rioproject.system.capability.PlatformCapability;
import org.rioproject.util.PropertyHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.net.UnknownHostException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.ExportException;
import java.util.Map;

/**
 * Creates and manages a service bean in it's own process
 *
 * @author Dennis Reedy
 */
public class ServiceBeanExecHandler {
    private final ServiceElement sElem;
    private final Configuration config;
    private final Uuid uuid;
    private ServiceBeanExecutor execHandler;
    private ServiceBeanInstance instance;
    private ServiceRecord serviceRecord;
    private ProcessManager manager;
    /** The amount of time to wait for a forked service to be created */
    private int forkedServiceWaitTime = 60; // number of seconds
    private static final String CONFIG_COMPONENT = "service.load";
    private static final String COMPONENT = "org.rioproject.cybernode";
    private static final Logger logger = LoggerFactory.getLogger(ServiceBeanExecHandler.class);

    public ServiceBeanExecHandler(final ServiceElement sElem, final Configuration config, final Uuid uuid) {
        this.sElem = sElem;
        this.config = config;
        this.uuid = uuid;
        try {
            forkedServiceWaitTime = Config.getIntEntry(config,
                                                       CONFIG_COMPONENT,
                                                       "forkedServiceWaitTime",
                                                       60,    //default is 1 minute
                                                       5,     //minimum of 5 second wait
                                                       60*5); // max of 5 minute wait
        } catch(ConfigurationException e) {
            logger.warn("Getting forkedServiceWaitTime, using default", e);
        }
    }

    public ServiceBeanExecutor getServiceBeanExecutor() {
        return execHandler;
    }
    public ServiceBeanInstance exec(final OperationalStringManager opStringMgr,
                                    final DiscardManager discardManager,
                                    final PlatformCapability[] installedPlatformCapabilities) throws Exception {
        RuntimeMXBean runtime = ManagementFactory.getRuntimeMXBean();
        return exec(opStringMgr, discardManager, installedPlatformCapabilities, runtime.getClassPath());

    }

    public ServiceBeanInstance exec(final OperationalStringManager opStringMgr,
                                    final DiscardManager discardManager,
                                    final PlatformCapability[] installedPlatformCapabilities,
                                    String classPath) throws Exception {
        ExecDescriptor exDesc = new ExecDescriptor();

        String rioHome = System.getProperty("RIO_HOME");
        if(rioHome==null)
            throw new ServiceBeanInstantiationException(String.format("Cannot exec service [%s], unknown RIO_HOME system property",
                                                                      sElem.getName()));
        if(classPath==null)
            classPath = CommandLineHelper.generateRioStarterClassPath();

        /* Create a normalized service name, translating " " to "_" and
         * appending the instance ID to the name. This will be used for the
         * log name and the registry bind name */
        String normalizedServiceName = RMIServiceNameHelper.createNormalizedServiceName(sElem);
        String serviceBindName = RMIServiceNameHelper.createBindName(sElem);

        /* Get the Cybernode's RMI Registry */
        int regPort = RegistryUtil.checkRegistry();
        String sPort = Integer.toString(regPort);

        String logDir = getLogDirectory(config, sElem.getOperationalStringName());
        if(!logDir.endsWith(File.separator))
            logDir = logDir+File.separator;
        logger.info("Logging for {} will be sent to {}", sElem.getName(), logDir);

        /* Create command line */
        exDesc.setCommandLine(CommandLineHelper.getJava());

        /* Create input args */
        StringBuilder inputArgsBuilder = new StringBuilder();
        inputArgsBuilder.append(CommandLineHelper.getClassPath(classPath));
        String jvmOptions = (sElem.getExecDescriptor()==null? null: sElem.getExecDescriptor().getInputArgs());
        inputArgsBuilder.append(CommandLineHelper.createInputArgs(normalizedServiceName,
                                                                  serviceBindName,
                                                                  sPort,
                                                                  jvmOptions,
                                                                  logDir));
        inputArgsBuilder.append(CommandLineHelper.getStarterClass()).append(" ");
        String serviceBeanExecStarter = CommandLineHelper.getStarterConfig(rioHome);
        logger.trace("Using service bean exec starter: {}", serviceBeanExecStarter);
        inputArgsBuilder.append(serviceBeanExecStarter);

        exDesc.setInputArgs(inputArgsBuilder.toString());
        exDesc.setWorkingDirectory(System.getProperty("user.dir"));

        /* If we have an exec descriptor, make add any environment settings the
         * service has declared */
        if(sElem.getExecDescriptor()!=null) {
            Map<String, String> env = sElem.getExecDescriptor().getEnvironment();
            for(Map.Entry<String, String> entry : env.entrySet()) {
                env.put(entry.getKey(), PropertyHelper.expandProperties(entry.getValue()));
            }
            exDesc.setEnvironment(env);
        } 

        String serviceOut = logDir+normalizedServiceName+".out";
        exDesc.setStdErrFileName(serviceOut);
        exDesc.setStdOutFileName(serviceOut);

        try {
            Registry registry = LocateRegistry.getRegistry(regPort);
            ForkedServiceBeanListener forkedServiceListener = new ForkedServiceBeanListener(discardManager);
            ServiceBeanExecListener listener = forkedServiceListener.getServiceBeanExecListener();
            long start = System.currentTimeMillis();

            Shell shell = ShellFactory.createShell();
            try {
                String shellTemplate = (String)config.getEntry(COMPONENT,
                                                               "serviceBeanExecShellTemplate",
                                                               String.class,
                                                               null);
                if(shellTemplate!=null)
                    shell.setShellTemplate(shellTemplate);
            } catch (ConfigurationException e) {
                logger.warn("Cannot get shell template from configuration, continue with default");
            }
            logger.info("Invoke PosixShell.exec for {}, working directory {}", CybernodeLogUtil.logName(sElem), exDesc.getWorkingDirectory());
            manager = shell.exec(exDesc);
            forkedServiceListener.setName(serviceBindName);
            forkedServiceListener.setRegistryPort(regPort);

            long wait = 0;
            do {
                try {
                    execHandler = (ServiceBeanExecutor)registry.lookup(serviceBindName);
                    forkedServiceListener.createFDH(execHandler);
                    execHandler.setUuid(uuid);
                    execHandler.setServiceBeanExecListener(listener);

                    if(installedPlatformCapabilities!=null && installedPlatformCapabilities.length>0)
                        execHandler.applyPlatformCapabilities(installedPlatformCapabilities);
                    instance = execHandler.instantiate(sElem, opStringMgr);
                    long activationTime = System.currentTimeMillis()-start;
                    logger.info("Forked instance created for [{}], pid=[{}], activation time={} ms",
                                serviceBindName, manager.getPid(), activationTime);
                    break;
                } catch (NotBoundException e) {
                    try {
                        Thread.sleep(1000);
                        wait++;
                    } catch (InterruptedException e1) {
                        logger.warn("Interrupted waiting for ServiceBean [{}] to register into Registry",
                                    serviceBindName);
                    }
                }
            } while (wait < forkedServiceWaitTime);

            if (wait >= forkedServiceWaitTime) {
                logger.warn("Timed out waiting for [{}]. Waited [{}] seconds, configured wait " +
                            "time is [{}] seconds. Killing spawned process and unregistering from local " +
                            "registry. Check the service's output log to determine root cause(s)",
                            serviceBindName, wait, forkedServiceWaitTime);
                manager.destroy(true);
                throw new ServiceBeanInstantiationException("Failed to fork");
            }

        } catch (Exception e) {
            unregister(regPort, serviceBindName);
            logger.info("Terminate process for ServiceBean [{}]", serviceBindName);
            if(manager!=null)
                manager.destroy(true);
            throw e;
        }
        return instance;
    }

    public ServiceRecord getServiceRecord() {
        return serviceRecord;
    }

    private void unregister(final int regPort, final String name) {
        try {
            Registry registry = LocateRegistry.getRegistry(regPort);
            registry.unbind(name);
            logger.info("Unbound failed ServiceBean fork for [{}]", name);
        } catch (Exception e1) {
            // ignore
        }
    }

    private String getLogDirectory(final Configuration config,
                                   final String opstringName) throws ConfigurationException, IOException {
        String logDirDefault = System.getProperty("RIO_LOG_DIR");
        if(logDirDefault==null)
            logDirDefault = System.getProperty("RIO_HOME")+File.separator+"logs";
        String serviceLogRootDirectory = (String)config.getEntry(COMPONENT,
                                                                 "serviceLogRootDirectory",
                                                                 String.class,
                                                                 logDirDefault);
        File rootDir = new File(serviceLogRootDirectory);
        FileUtils.checkDirectory(rootDir, "service log root");

        File serviceLogDir = new File(rootDir, "service_logs");
        FileUtils.checkDirectory(serviceLogDir, "service logs");

        String s = opstringName.replaceAll(" ", "_");
        File opstringDir = new File(serviceLogDir, s);
        FileUtils.checkDirectory(opstringDir, "opstring log root");
        return opstringDir.getAbsolutePath();
    }

    class ForkedServiceBeanListener implements ServiceBeanExecListener, FaultDetectionListener<String> {
        final DiscardManager discardManager;
        Exporter exporter;
        ServiceBeanExecListener listener;
        int registryPort;
        String name;

        ForkedServiceBeanListener(final DiscardManager discardManager) {
            this.discardManager = discardManager;
        }

        void setRegistryPort(final int registryPort) {
            this.registryPort = registryPort;
        }

        void createFDH(final ServiceBeanExecutor execHandler) {
            try {
                JVMProcessMonitor.getInstance().monitor(execHandler.getID(), this);
            } catch (RemoteException e) {
                serviceFailure(execHandler, null);
            }
        }

        void setName(String name) {
            this.name = name;
        }

        ServiceBeanExecListener getServiceBeanExecListener() throws ConfigurationException, ExportException, UnknownHostException {
            if(listener==null) {
                exporter  = ExporterConfig.getExporter(config,
                                                       "org.rioproject.cybernode",
                                                       "exporter");
                if(exporter==null)
                    exporter = new BasicJeriExporter(ExporterConfig.getServerEndpoint(), new BasicILFactory());
                listener = (ServiceBeanExecListener)exporter.export(this);
            }
            return listener;
        }

        public void serviceInstantiated(final ServiceRecord record) {
            serviceRecord = record;
            if(manager!=null) {
                serviceRecord.setPid(manager.getPid());
            }
            logger.debug("Instantiation notification for {}", CybernodeLogUtil.logName(sElem));
        }

        public void serviceDiscarded(final ServiceRecord record) {
            logger.info("Discard notification for {}/{}", sElem.getOperationalStringName(), sElem.getName());
            discardManager.discard();
        }

        public void serviceFailure(final Object service, final String serviceID) {
            try {
                Registry registry = LocateRegistry.getRegistry(registryPort);
                registry.unbind(name);
                logger.info("Terminated ServiceBean fork for [{}] unbound from local registry", name);
            } catch (Exception e) {
                // ignore
            }
            serviceDiscarded(null);
        }
    }

}
