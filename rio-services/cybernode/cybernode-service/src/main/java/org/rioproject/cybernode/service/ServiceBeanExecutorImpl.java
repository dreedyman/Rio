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
package org.rioproject.cybernode.service;

import com.sun.jini.start.LifeCycle;
import net.jini.config.Configuration;
import net.jini.config.ConfigurationProvider;
import net.jini.core.lookup.ServiceID;
import net.jini.export.Exporter;
import net.jini.id.Uuid;
import org.rioproject.config.Constants;
import org.rioproject.deploy.ServiceBeanInstance;
import org.rioproject.deploy.ServiceBeanInstantiationException;
import org.rioproject.deploy.ServiceRecord;
import org.rioproject.event.EventHandler;
import org.rioproject.exec.ServiceBeanExecListener;
import org.rioproject.exec.ServiceBeanExecutor;
import org.rioproject.impl.config.ExporterConfig;
import org.rioproject.impl.container.ServiceBeanContainerListener;
import org.rioproject.impl.container.ServiceBeanLoaderResult;
import org.rioproject.impl.container.ServiceLogUtil;
import org.rioproject.impl.exec.JVMProcessMonitor;
import org.rioproject.impl.exec.VirtualMachineHelper;
import org.rioproject.impl.fdh.FaultDetectionListener;
import org.rioproject.impl.fdh.JMXFaultDetectionHandler;
import org.rioproject.impl.opstring.OpStringManagerProxy;
import org.rioproject.impl.servicebean.ServiceBeanActivation;
import org.rioproject.impl.servicebean.ServiceBeanAdapter;
import org.rioproject.impl.servicebean.ServiceElementUtil;
import org.rioproject.impl.system.ComputeResource;
import org.rioproject.impl.system.measurable.MeasurableCapability;
import org.rioproject.impl.system.measurable.cpu.CPU;
import org.rioproject.impl.system.measurable.memory.Memory;
import org.rioproject.opstring.OperationalStringManager;
import org.rioproject.opstring.ServiceElement;
import org.rioproject.rmi.RegistryUtil;
import org.rioproject.servicebean.ServiceBeanContext;
import org.rioproject.system.ComputeResourceUtilization;
import org.rioproject.system.SystemWatchID;
import org.rioproject.system.capability.PlatformCapability;
import org.rioproject.watch.WatchDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Provides support to create a ServiceBean in it's own JVM.
 *
 * @author Dennis Reedy
 */
@SuppressWarnings({"unused", "PMD.AvoidThrowingRawExceptionTypes"})
public class ServiceBeanExecutorImpl implements ServiceBeanExecutor,
                                                ServiceBeanContainerListener,
                                                FaultDetectionListener<String> {
    private ServiceBeanContainerImpl container;
    private Exporter exporter;
    private String execBindName;
    private ServiceBeanContext context;
    private int cybernodeRegistryPort;
    private Registry registry;
    private ServiceBeanExecListener listener;
    private JMXFaultDetectionHandler fdh;
    private ComputeResource computeResource;
    private ComputeResourcePolicyHandler computeResourcePolicyHandler;
    private ServiceBeanInstance instance;
    private final String myID;
    private final AtomicBoolean initializing = new AtomicBoolean(true);
    static final String CONFIG_COMPONENT = "org.rioproject.cybernode";
    private Logger logger = LoggerFactory.getLogger(ServiceBeanExecutorImpl.class);

    /**
     * Create a ServiceBeanExecutor launched from the ServiceStarter
     * framework
     *
     * @param configArgs Configuration arguments
     * @param lifeCycle The LifeCycle object that started the
     * ServiceBeanExecutor
     * 
     * @throws Exception if bootstrapping fails
     */
    public ServiceBeanExecutorImpl(final String[] configArgs, final LifeCycle lifeCycle)
        throws Exception {
        myID = VirtualMachineHelper.getID();
        String sPort = System.getProperty(Constants.REGISTRY_PORT, "0");
        if (Integer.parseInt(sPort) == 0)
            logger.warn("The RMI Registry port provided (or obtained by default) is 0. " +
                        "Because it is 0, the port will default to {}. This port may be " +
                        "taken by another RMI Registry instance. If there is an MBeanServer " +
                        "bound to that RMI Registry instance, the ServiceBeanExecutorImpl will monitor the " +
                        "ability to connect to that MBeanServer. If that MBeanServer is terminated, " +
                        "the ServiceBeanExecutorImpl will also terminate. Care should be taken to not " +
                        "use the default RMI Registry port.", Registry.REGISTRY_PORT);
        cybernodeRegistryPort = Integer.parseInt(sPort);
        execBindName = System.getProperty(Constants.SERVICE_BEAN_EXEC_NAME, "ServiceBeanExecutorImpl");
        bootstrap(configArgs);
        logger.info("Started ServiceBeanExecutor for {}", execBindName);
    }

    private void bootstrap(final String[] configArgs) throws Exception {
        ClassLoader cCL = Thread.currentThread().getContextClassLoader();
        Configuration config = ConfigurationProvider.getInstance(configArgs, cCL);
        container = new ServiceBeanContainerImpl(config);
        computeResource = new ComputeResource(config);

        /* Setup persistent provisioning attributes */
        boolean provisionEnabled = (Boolean) config.getEntry("org.rioproject.cybernode",
                                                             "provisionEnabled",
                                                             Boolean.class,
                                                             true);
        computeResource.setPersistentProvisioning(provisionEnabled);        
        String provisionRoot = Environment.setupProvisionRoot(provisionEnabled, config);
        computeResource.setPersistentProvisioningRoot(provisionRoot);

        MeasurableCapability[] mCaps = loadMeasurables(config);
        for(MeasurableCapability mCap : mCaps) {
            computeResource.addMeasurableCapability(mCap);
            mCap.start();
        }

        container.setComputeResource(computeResource);
        container.addListener(this);

        context = ServiceBeanActivation.getServiceBeanContext(CONFIG_COMPONENT,
                                                              "Cybernode",
                                                              configArgs,
                                                              config,
                                                              getClass().getClassLoader());
        registry = LocateRegistry.getRegistry(cybernodeRegistryPort);

        exporter = ExporterConfig.getExporter(config, "org.rioproject.cybernode", "exporter");

        int createdRegistryPort = RegistryUtil.getRegistry();
        if(createdRegistryPort>0) {
            System.setProperty(Constants.REGISTRY_PORT, Integer.toString(createdRegistryPort));
        } else {
            throw new RuntimeException("Unable to create RMI Registry");
        }

        Remote proxy = exporter.export(this);
        registry.bind(execBindName, proxy);
        initializing.set(false);

        new Thread(new CreateFDH(config, this)).start();
    }

    public String getID() {
        return myID;
    }

    public void setUuid(final Uuid uuid) {
        if(uuid==null)
            throw new IllegalArgumentException("uuid cannot be null");
        container.setUuid(uuid);
    }

    public void setServiceBeanExecListener(final ServiceBeanExecListener listener) {
        this.listener = listener;
    }

    public void applyPlatformCapabilities(final PlatformCapability[] pCaps) {
        if(pCaps==null)
            return;
        for(PlatformCapability pCap : pCaps) {
            logger.info("Adding [{}] capability", pCap.getName());
            computeResource.addPlatformCapability(pCap);
        }
    }

    @Override
    public ServiceBeanInstance getServiceBeanInstance()  {
        return instance;
    }

    public ServiceBeanInstance instantiate(final ServiceElement sElem,
                                           final OperationalStringManager opStringMgr)
        throws ServiceBeanInstantiationException {
        logger.info("Instantiating {}, service counter={}", sElem.getName(), container.getServiceCounter());
        if (container.getServiceCounter() > 0)
            throw new ServiceBeanInstantiationException("ServiceBeanExecutor has already instantiated a service");

        OperationalStringManager opMgr = opStringMgr;
        try {
            opMgr = OpStringManagerProxy.getProxy(sElem.getOperationalStringName(),
                                                  opStringMgr,
                                                  context.getDiscoveryManagement());
        } catch (Exception e) {
            logger.warn("Unable to create proxy for OperationalStringManager, using provided OperationalStringManager", e);

        }

        /* Set up thread deadlock detection */
        ServiceElementUtil.setThreadDeadlockDetector(sElem, null);

        /* Get the SLA ThresholdEvent wired up */
        EventHandler slaThresholdEventHandler = null;
        instance = container.activate(sElem, opMgr, null);
        ServiceBeanDelegateImpl delegate = (ServiceBeanDelegateImpl) container.getServiceBeanDelegate(instance.getServiceBeanID());
        ServiceBeanLoaderResult result = delegate.getLoadedServiceResult();
        if(result.getImpl() instanceof ServiceBeanAdapter) {
            slaThresholdEventHandler = ((ServiceBeanAdapter)result.getImpl()).getSLAEventHandler();
        } else {
            if(result.getBeanAdapter()!=null) {
                slaThresholdEventHandler = result.getBeanAdapter().getSLAEventHandler();
            } else {
                String className = result.getImpl()==null?"<NO IMPLEMENTATION>":result.getImpl().getClass().getName();
                logger.warn("SLAThresholdEvent notifications from this forked service are not enabled, service class does not provide support for Rio event registration {}.",
                            className);
            }
        }
        if(slaThresholdEventHandler!=null) {
            computeResourcePolicyHandler = new ComputeResourcePolicyHandler(sElem,
                                                                            slaThresholdEventHandler,
                                                                            null,
                                                                            instance);
            computeResource.addThresholdListener(computeResourcePolicyHandler);
        }
        return instance;
    }

    public void update(final ServiceElement element, final OperationalStringManager opStringMgr)  {
        container.update(new ServiceElement[]{element}, opStringMgr);
    }

    public ComputeResourceUtilization getComputeResourceUtilization() {
        return computeResource.getComputeResourceUtilization();
    }

    public void serviceInstantiated(final ServiceRecord record) {
        ServiceElement service = record.getServiceElement();
        logger.info("Instantiated {}, {}", ServiceLogUtil.logName(service), ServiceLogUtil.discoveryInfo(service));
        try {
            listener.serviceInstantiated(record);
        } catch (RemoteException e) {
            logger.warn("Notifying Cybernode that the service is active", e);
        }
    }

    public void serviceDiscarded(final ServiceRecord record) {
        logger.info("Destroying ServiceBeanExecutor for {}", execBindName);
        if(fdh!=null)
            fdh.terminate();

        if(computeResourcePolicyHandler!=null) {
            computeResourcePolicyHandler.terminate();
        }

        exporter.unexport(true);
        try {
            registry.unbind(execBindName);
        } catch (Exception e) {
            logger.warn("{}: {}, Unbinding from RMI Registry", e.getClass().getName(), e.getMessage());
        }
        if(record!=null) {
            try {
                listener.serviceDiscarded(record);
            } catch (RemoteException e) {
                logger.warn("{}: {}, Notifying Cybernode that we are exiting", e.getClass().getName(), e.getMessage());
            }
        }
        
        /* Perform the system exit in a thread, allowing the method to return */
        new Thread(new Runnable() {
            public void run() {
                System.exit(0);
            }
        }).start();
    }

    public void serviceFailure(final Object service, final String pid) {
        logger.warn("Parent Cybernode has orphaned us, exiting");
        container.terminate();
        serviceDiscarded(null);
    }

    public WatchDataSource[] fetch() {
        List<WatchDataSource> watchDataSources = new ArrayList<WatchDataSource>();
        for(MeasurableCapability mCap : computeResource.getMeasurableCapabilities()) {
            watchDataSources.add(mCap.getWatchDataSource());
        }
        return watchDataSources.toArray(new WatchDataSource[watchDataSources.size()]);
    }

    public WatchDataSource fetch(final String id) {
        WatchDataSource watchDataSource = null;
        for(MeasurableCapability mCap : computeResource.getMeasurableCapabilities()) {
            if(mCap.getId().equals(id)) {
                watchDataSource = mCap.getWatchDataSource();
                break;
            }
        }
        return watchDataSource;
    }

    private MeasurableCapability[] loadMeasurables(final Configuration config) {
        List<MeasurableCapability> measurables = new ArrayList<MeasurableCapability>();
        /* Create the Memory MeasurableCapability */
        MeasurableCapability memory = new Memory(config);
        if(memory.isEnabled())
            measurables.add(memory);

        try{
            MeasurableCapability cpu = new CPU(config, SystemWatchID.PROC_CPU, true);
            if(cpu.isEnabled())
                measurables.add(cpu);
        } catch (RuntimeException e) {
            logger.warn("JVM CPU monitoring not supported");
        }
        return measurables.toArray(new MeasurableCapability[measurables.size()]);
    }

    private class CreateFDH implements Runnable {
        private final Configuration config;
        private final FaultDetectionListener<String> faultDetectionListener;

        private CreateFDH(final Configuration config, final FaultDetectionListener<String> faultDetectionListener) {
            this.config = config;
            this.faultDetectionListener = faultDetectionListener;
        }

        public void run() {
            final String parentPID = System.getProperty(Constants.PROCESS_ID);
            if(parentPID!=null) {
                JVMProcessMonitor.getInstance().monitor(parentPID, faultDetectionListener);
            } else {
                logger.error("Cybernode PID is null, unable to setup FDH to make sure Cybernode doesn't orphan us");
            }
        }
    }

    class WrappedFaultDetectionListener implements FaultDetectionListener<ServiceID> {
        private final FaultDetectionListener<String> wrapped;

        WrappedFaultDetectionListener(FaultDetectionListener<String> wrapped) {
            this.wrapped = wrapped;
        }

        @Override
        public void serviceFailure(Object service, ServiceID serviceID) {
            wrapped.serviceFailure(service, serviceID==null?null:serviceID.toString());
        }
    }
}
