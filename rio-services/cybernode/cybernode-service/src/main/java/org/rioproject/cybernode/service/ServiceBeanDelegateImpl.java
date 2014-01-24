/*
 * Copyright to the original author or authors.
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

import com.sun.jini.admin.DestroyAdmin;
import com.sun.jini.start.LifeCycle;
import groovy.lang.MissingMethodException;
import net.jini.admin.Administrable;
import net.jini.config.Configuration;
import net.jini.config.ConfigurationException;
import net.jini.id.Uuid;
import org.rioproject.admin.ServiceBeanControlException;
import org.rioproject.associations.Association;
import org.rioproject.impl.associations.DefaultAssociationManagement;
import org.rioproject.associations.AssociationType;
import org.rioproject.impl.bean.BeanHelper;
import org.rioproject.annotation.Initialized;
import org.rioproject.annotation.Started;
import org.rioproject.config.Constants;
import org.rioproject.impl.container.*;
import org.rioproject.impl.servicebean.DefaultServiceBeanContext;
import org.rioproject.impl.servicebean.DefaultServiceBeanManager;
import org.rioproject.servicebean.ServiceBeanContext;
import org.rioproject.costmodel.ResourceCost;
import org.rioproject.deploy.ServiceBeanInstance;
import org.rioproject.deploy.ServiceBeanInstantiationException;
import org.rioproject.deploy.ServiceRecord;
import org.rioproject.event.EventHandler;
import org.rioproject.impl.exec.ServiceBeanExecHandler;
import org.rioproject.impl.exec.ServiceExecutor;
import org.rioproject.impl.jmx.JMXUtil;
import org.rioproject.impl.jmx.MBeanServerFactory;
import org.rioproject.impl.servicebean.ServiceBeanSLAManager;
import org.rioproject.impl.servicebean.ServiceElementUtil;
import org.rioproject.impl.opstring.OpStringManagerProxy;
import org.rioproject.opstring.OperationalStringManager;
import org.rioproject.opstring.ServiceElement;
import org.rioproject.sla.SLAThresholdEvent;
import org.rioproject.impl.system.ComputeResource;
import org.rioproject.system.ComputeResourceUtilization;
import org.rioproject.system.capability.PlatformCapability;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.management.ObjectName;
import java.lang.annotation.Annotation;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * The ServiceBeanDelegateImpl provides loading and management services for a service
 * which has been instantiated by a {@link org.rioproject.impl.container.ServiceBeanContainer}
 *
 * @author Dennis Reedy
 */
public class ServiceBeanDelegateImpl implements ServiceBeanDelegate {
    /** ServiceBeanInstance of a loaded and started ServiceBean */
    private ServiceBeanInstance instance;
    /** Unique identifier for the delegate */
    private final Object identifier;
    /** ServiceBean ID */
    private Uuid serviceID;
    /** ServiceBean proxy */
    private Object serviceProxy;
    /** Reference to the ServiceBeanContainer */
    private final ServiceBeanContainer container;
    /** The ServiceElement */
    private ServiceElement sElem;
    /** The OperationalStringManager for the JSB */
    private OperationalStringManager opStringMgr;
    /** Throwable for ThreadGroup processing */
    private Throwable abortThrowable;
    /** The ServiceRecord */
    private ServiceRecord serviceRecord;
    private final Object serviceRecordLock = new Object();
    private long lastServiceRecordUpdate;
    /** Flag to indicate the service is in the process of starting */
    private final AtomicBoolean starting = new AtomicBoolean(false);
    /** Flag to indicate the service is in the process of terminating */
    private final AtomicBoolean terminating =new AtomicBoolean(false);
    /** Flag to indicate the service is terminated */
    private final AtomicBoolean terminated = new AtomicBoolean(false);
    /** A utility used to install staged data */
    private final StagedDataManager stagedDataManager;
    /** The ServiceBeanContext */
    private ServiceBeanContext context;
    /** EventHandler for SLAThresholdEvent processing */
    private EventHandler slaEventHandler;
    /* Manage declared SLAs */
    private ServiceBeanSLAManager serviceBeanSLAManager;
    /** A ServiceElementChangeManager for handling updates to the
     * ServiceElement */
    private ServiceElementChangeManager sElemChangeMgr;
    private ServiceCostCalculator serviceCostCalculator;
    //private ServiceProvisionEvent provisionEvent;
    private ServiceBeanExecHandler execManager;
    private final Collection<PlatformCapability> installedPlatformCapabilities = new ArrayList<PlatformCapability>();
    private static final String CONFIG_COMPONENT = "org.rioproject.cybernode";
    /** Logger */
    private final static Logger logger = LoggerFactory.getLogger(ServiceBeanDelegateImpl.class.getName());
    /** Result from loading the service */
    protected ServiceBeanLoaderResult loadResult;
    private AtomicBoolean isDiscarded = new AtomicBoolean(false);

    /**
     * Create a ServiceBeanDelegateImpl
     *
     * @param identifier A cookie for internal accounting
     * @param serviceID Unique identifier for the ServiceBean
     * @param container The ServiceBeanContainer
     */
    public ServiceBeanDelegateImpl(Integer identifier, Uuid serviceID, ServiceBeanContainer container) {
        this.identifier = identifier;
        this.serviceID = serviceID;
        this.container = container;
        stagedDataManager = new StagedDataManager(container.getComputeResource());
        ServiceCostCalculator defaultCostCalculator = new ServiceCostCalculator();
        Configuration config = container.getSharedConfiguration();
        try {
            serviceCostCalculator = (ServiceCostCalculator)config.getEntry(CONFIG_COMPONENT,
                                                                           "serviceCostCalculator",
                                                                           ServiceCostCalculator.class,
                                                                           defaultCostCalculator);
        } catch(ConfigurationException e) {
            logger.warn("Getting ServiceCostCalculator, using default", e);
            serviceCostCalculator = defaultCostCalculator;
        }
        serviceCostCalculator.setComputeResource(container.getComputeResource());
    }

    public void setOperationalStringManager(OperationalStringManager opStringMgr) {
        this.opStringMgr= opStringMgr;
    }

    public void setServiceElement(ServiceElement sElem) {
        this.sElem = sElem;
        stagedDataManager.setServiceElement(sElem);
    }

    public void setEventHandler(EventHandler slaEventHandler) {
        this.slaEventHandler = slaEventHandler;
    }

    /**
     * Get the ServiceRecord
     * 
     * @return ServiceRecord
     */
    public ServiceRecord getServiceRecord() {
        if(serviceRecord!=null) {
            synchronized(serviceRecordLock) {
                try {
                    long now = System.currentTimeMillis();
                    if(lastServiceRecordUpdate!=0)
                        serviceRecord.setUpdated();
                    long diff = (lastServiceRecordUpdate==0 ? 0 : now-lastServiceRecordUpdate);
                    ResourceCost[] resourceCosts = serviceCostCalculator.calculateCosts(diff);
                    for (ResourceCost resourceCost : resourceCosts) {
                        serviceRecord.addResourceCost(resourceCost);
                    }
                    lastServiceRecordUpdate = now;
                } catch (Throwable t) {
                    logger.warn("Calculating resource costs for [{}]", sElem.getName(), t);
                }
            }
        }
        return(serviceRecord);
    }

    public boolean isActive() {
        return !isDiscarded.get();
    }

    /**
     * Get the ServiceElement
     * 
     * @return The ServiceElement used by this ServiceBeanDelegateImpl
     */
    public ServiceElement getServiceElement() {
        return(sElem);
    }

    /*
     * Update the ServiceElement and potentially the OperationalStringManager
     * if the ServiceElement is the same as the ServiceElement the delegate
     * has created a ServiceBean instance of
     */
    public boolean update(ServiceElement newElem, OperationalStringManager opMgr) {
        if(terminated.get() || terminating.get())
            return(false);

        if(execManager!=null) {
            try {
                long waited = 0;
                long totalWait = 10*1000;
                while(execManager.getServiceBeanExecutor()==null && waited<totalWait) {
                    try {
                        Thread.sleep(500);
                        waited += 500;
                    } catch (InterruptedException e) {
                        return false;
                    }
                }
                if(execManager.getServiceBeanExecutor()==null)
                    return false;
                execManager.getServiceBeanExecutor().update(newElem, opMgr);
                return true;
            } catch (RemoteException e) {
                logger.warn("Updating forked service [{}]", ServiceLogUtil.logName(sElem), e);
                return false;
            }
        }
        if(!this.sElem.equals(newElem))
            return(false);

        synchronized(this) {
            if(serviceProxy==null) {
                logger.trace("Cannot update [{}], Proxy is null", sElem.getName());
                return(false);
            }
            /* Preserve instanceID */
            Long instanceID = sElem.getServiceBeanConfig().getInstanceID();
            if(instanceID!=null) {
                sElem = ServiceElementUtil.prepareInstanceID(newElem, true, instanceID);
            } else {
                sElem = ServiceElementUtil.copyServiceElement(newElem);
                logger.warn("No instanceID for [{}] to update", sElem.getName());
            }

            if(context instanceof DefaultServiceBeanContext) {
                ((DefaultServiceBeanContext)context).setServiceElement(sElem);
            } else {
                logger.warn("Cannot update [{}], Unknown ServiceBeanContext type [{}]",
                            ServiceLogUtil.logName(sElem), context.getClass().getName());
                return(false);
            }

            if(context.getServiceBeanManager() instanceof DefaultServiceBeanManager) {
                DefaultServiceBeanManager jsbMgr = (DefaultServiceBeanManager)context.getServiceBeanManager();
                OperationalStringManager mgr = jsbMgr.getOperationalStringManager();
                if(mgr!=null &&
                   !(mgr instanceof OpStringManagerProxy.OpStringManager)) {
                    jsbMgr.setOperationalStringManager(opMgr);
                }                
            } else {
                logger.warn("Cannot update [{}], Unknown ServiceBeanManager type [{}]",
                            sElem.getName(), context.getServiceBeanManager().getClass().getName());
                return(false);
            }
        }
        return(true);
    }

    /**
     * Loads and starts a service, blocking until the service has been started
     * 
     * @return The ServiceBeanInstance
     *
     * @throws org.rioproject.deploy.ServiceBeanInstantiationException if there are errors loading the service
     * bean
     */
    public ServiceBeanInstance load() throws ServiceBeanInstantiationException {
        if(instance!=null)
            return(instance);
        synchronized(this) {
            startServiceBean(sElem, opStringMgr);
        }
        return(instance);
    }

    /**
     * Get the ServiceBeanInstance
     *
     * @return The ServiceBeanInstance
     */
    public ServiceBeanInstance getServiceBeanInstance() {
        return (instance);
    }

    /**
     * Advertise the ServiceBean, making it available to all clients
     *
     * @throws ServiceBeanControlException if the service bean cannot be advertised
     */
    public void advertise() throws ServiceBeanControlException {
        if(execManager!=null)
            return;
        if(serviceProxy==null) {
            throw new ServiceBeanControlException("Cannot advertise ["+sElem.getName()+"], Proxy is null");
        }
        if(terminated.get() || terminating.get())
            throw new ServiceBeanControlException("advertising service while in the process of terminating");
        /* If any of the associations are of type requires, and they are lazily (not eagerly) injected, service
         * advertisement is managed by AssociationManagement */
        for (Association association : context.getAssociationManagement().getAssociations()) {
            if (association.getAssociationType()== AssociationType.REQUIRES) {
                logger.debug("{} has at least one requires Association, advertisement managed by AssociationManagement",
                             sElem.getName());
                if(context.getAssociationManagement() instanceof DefaultAssociationManagement) {
                    ((DefaultAssociationManagement)context.getAssociationManagement()).checkAdvertise();
                }
                return;
            }
        }
        try {

            ServiceAdvertiser.advertise(serviceProxy, context, runningForked());
            logger.debug("{}: advertised", ServiceLogUtil.logName(sElem));
        } catch(ServiceBeanControlException e) {
            logger.warn("Could not advertise {}", sElem.getName());
            throw e;
        }
    }

    public ComputeResourceUtilization getComputeResourceUtilization() {
        ComputeResourceUtilization cru = null;
        if(sElem.forkService()) {
            try {
                cru  = execManager.getServiceBeanExecutor().getComputeResourceUtilization();
            } catch (RemoteException e) {
                logger.warn("Getting compute resource utilization failed for " +
                            "service [{}], the service may be " +
                            "in the process of failing or may have already " +
                            "failed. {}:{}",
                            ServiceElementUtil.getLoggingName(sElem), e.getClass().getName(), e.getMessage());
            }
        } else if(loadResult.getImpl() instanceof ServiceExecutor) {
            cru = ((ServiceExecutor)loadResult.getImpl()).getComputeResourceUtilization();
        } else {
            cru = container.getComputeResource().getComputeResourceUtilization();
        }
        return cru;
    }

    /**
     * Terminate the ServiceBeanDelegate. This calls the destroy method of the 
     * ServiceBean
     */
    public void terminate() {
        if(terminated.get() || terminating.get())
            return;
        try {
            terminating.set(true);
            if(serviceBeanSLAManager != null) {
                serviceBeanSLAManager.terminate();
                serviceBeanSLAManager = null;
            }

            if(context!=null) {
                /* Remove ServiceElementChangeManager */
                context.getServiceBeanManager().removeListener(sElemChangeMgr);
            }

            ServiceTerminationHelper.cleanup(context);

            if(opStringMgr instanceof OpStringManagerProxy.OpStringManager) {
                ((OpStringManagerProxy.OpStringManager)opStringMgr).terminate();
            }

            /* If we have an instance, go through service termination */
            if(instance!=null) {
                if(serviceProxy!=null) {
                    try {
                        if(serviceProxy instanceof Administrable) {
                            Administrable admin = (Administrable)serviceProxy;
                            Object adminObject = admin.getAdmin();

                            if(adminObject instanceof DestroyAdmin) {
                                DestroyAdmin destroyAdmin = (DestroyAdmin)adminObject;
                                destroyAdmin.destroy();
                                setDiscarded();
                                //container.discarded(identifier);
                                terminated.set(true);
                            } else {
                                logger.debug("No DestroyAdmin capabilities for {}", serviceProxy.getClass().getName());
                            }
                        } else {
                            logger.debug("No Administrable capabilities for {}", serviceProxy.getClass().getName());
                        }
                    } catch(Throwable t) {
                        logger.error("Terminating ServiceBean", t);
                        terminating.set(false);
                    } finally {
                        serviceProxy = null;
                    }
                }
            }

            /* Decrement platform capability counter */
            for(PlatformCapability pCap : installedPlatformCapabilities) {
                int count = pCap.decrementUsage();
                if(count==-1) {
                    logger.info("PlatformCapability ["+pCap.getName()+"], unknown count");
                } else {
                    logger.info("PlatformCapability ["+pCap.getName()+"], count="+count);
                }
            }

            /* Unprovision any installed platform capability components */
            PlatformCapability[] pCaps = stagedDataManager.removeInstalledPlatformCapabilities(false);
            for(PlatformCapability pCap : pCaps) {
                unregisterPlatformCapability(pCap);
            }
            /* Unprovision staged data*/
            stagedDataManager.removeStagedData();
        } finally {
            container.remove(identifier);
            if(loadResult!=null) {
                ServiceBeanLoader.unload(loadResult, sElem);
                loadResult = null;
            }
            if(context!=null && context.getServiceBeanManager() instanceof DefaultServiceBeanManager)
                ((DefaultServiceBeanManager)context.getServiceBeanManager()).setMarshalledInstance(null);
            context = null;
        }
    }

    /**
     * Load, instantiate and start the ServiceBean
     *
     * @param sElem The ServiceElement
     * @param opStringMgr The OperationalStringManager
     *
     * @throws org.rioproject.deploy.ServiceBeanInstantiationException if there are errors creating the
     * service bean
     */
    protected void startServiceBean(final ServiceElement sElem, final OperationalStringManager opStringMgr)
    throws ServiceBeanInstantiationException {
        /*
        threadGroup = new ThreadGroup("ServiceBeanThreadGroup:"+sElem.getName()) {
            public void uncaughtException(Thread t, Throwable o) {
                logger.log(Level.SEVERE,
                           "UncaughtException thrown in ServiceBean "+
                           "["+sElem.getName()+"]",
                           o);
            }
        };

        Thread jsbThread = new Thread(threadGroup, "ServiceBeanDelegateImpl") {
        */
        StringBuilder threadName = new StringBuilder();
        threadName.append(sElem.getName()).append("-");
        Long instanceID = sElem.getServiceBeanConfig().getInstanceID();
        threadName.append(instanceID==null?"<?>":instanceID).append("-delegate");
        Thread jsbThread = new Thread(threadName.toString()) {
            public void run() {
                starting.set(true);
                ComputeResource computeResource = container.getComputeResource();
                try {

                    if(!runningForked()) {
                       /* If there are provisionable capabilities, or
                         * data staging, perform the stagedData/installation */
                        stagedDataManager.download();

                        installedPlatformCapabilities.addAll(stagedDataManager.getInstalledPlatformCapabilities());
                        for(PlatformCapability pCap : installedPlatformCapabilities) {
                            int count = pCap.incrementUsage();
                            logger.info("PlatformCapability ["+getName()+"], count="+count);
                        }
                    }

                    /* Check if we are forking a service bean */
                    if(sElem.forkService() && !runningForked()) {
                        logger.debug("Fork required for {}", ServiceLogUtil.logName(sElem));
                        logger.trace("Created a ServiceBeanExecHandler for {}", ServiceLogUtil.logName(sElem));
                        execManager = new ServiceBeanExecHandler(sElem, container.getSharedConfiguration(), container.getUuid());
                        try {
                            /* Get matched PlatformCapability instances to apply */
                            PlatformCapability[] pCaps = computeResource.getPlatformCapabilities();
                            PlatformCapability[] matched = ServiceElementUtil.getMatchedPlatformCapabilities(sElem, pCaps);
                            logger.trace("Invoke ServiceBeanExecHandler.exec for {}", ServiceLogUtil.logName(sElem));
                            instance = execManager.exec(opStringMgr, new JSBDiscardManager(), matched);
                            logger.trace("ServiceBeanInstance obtained from ServiceBeanExecHandler for {}", ServiceLogUtil
                                                                                                                .logName(sElem));
                            serviceRecord = execManager.getServiceRecord();
                            logger.trace("ServiceRecord obtained from ServiceBeanExecHandler for {}", ServiceLogUtil.logName(sElem));
                        } catch (Exception e) {
                            abortThrowable = e;
                        }

                    } else {
                        /* Create the DiscardManager */
                        JSBDiscardManager discardManager = new JSBDiscardManager();

                        /* Create the ServiceBeanManager */
                        DefaultServiceBeanManager serviceBeanManager = new DefaultServiceBeanManager(sElem,
                                                               opStringMgr,
                                                               computeResource.getHostName(),
                                                               computeResource.getAddress().getHostAddress(),
                                                               container.getUuid());
                        serviceBeanManager.setDiscardManager(discardManager);
                        serviceBeanManager.setServiceID(serviceID);
                        /*
                        * Load and start the ServiceBean
                        */
                        loadResult = ServiceBeanLoader.load(sElem, serviceID, serviceBeanManager, container);

                        org.rioproject.associations.AssociationManagement associationManagement;
                        ClassLoader currentCL = Thread.currentThread().getContextClassLoader();
                        try {
                            ClassLoader jsbCL = loadResult.getImpl().getClass().getClassLoader();
                            Thread.currentThread().setContextClassLoader(jsbCL);
                            context = loadResult.getServiceBeanContext();
                            associationManagement = context.getAssociationManagement();
                            serviceProxy = loadResult.getMarshalledInstance().get(false);
                        } finally {
                            Thread.currentThread().setContextClassLoader(currentCL);
                        }
                        serviceID = loadResult.getServiceID();
                        
                        /* Set properties to the ServiceCostCalculator */
                        serviceCostCalculator.setDownloadRecords(stagedDataManager.getDownloadRecords());

                        //serviceCostCalculator.setImpl(loadResult.impl);
                        //serviceCostCalculator.setProxy(serviceProxy);
                        serviceCostCalculator.setServiceBeanContext(context);

                        /* Register any PlatformCapability mbeans we created for
                         * the ServiceBean */
                        registerPlatformCapabilities();

                        if(context.getAssociationManagement() instanceof DefaultAssociationManagement) {
                            ((DefaultAssociationManagement)context.getAssociationManagement()).setBackend(loadResult.getImpl());
                            ((DefaultAssociationManagement)associationManagement).setServiceBeanContainer(container);
                            ((DefaultAssociationManagement)associationManagement).setServiceBeanContext(context);
                        } else {
                            logger.warn("The service's AssociationManagement is not an instance of {}, " +
                                        "failed to invoke setBackend method on [{}] impl",
                                        context.getAssociationManagement(), sElem.getName());
                        }

                        if(context instanceof DefaultServiceBeanContext) {
                            EventHandler eH = ((DefaultServiceBeanContext)context).getEventTable().get(SLAThresholdEvent.ID);
                            if(eH!=null) {
                                slaEventHandler = eH;
                                logger.debug("Set EventHandler [{}] for SLAManagement for service {}",
                                            slaEventHandler.getClass().getName(), sElem.getName());
                            }
                        }

                        /* Create the ServiceBeanSLAManager */
                        serviceBeanSLAManager = new ServiceBeanSLAManager(loadResult.getImpl(),
                                                                          serviceProxy,
                                                                          context,
                                                                          slaEventHandler);
                        serviceBeanSLAManager.addSLAs(sElem.getServiceLevelAgreements().getServiceSLAs());
                        serviceBeanSLAManager.createSLAThresholdEventAdapter();

                        sElemChangeMgr = new ServiceElementChangeManager(context, serviceBeanSLAManager, serviceProxy);
                        context.getServiceBeanManager().addListener(sElemChangeMgr);


                        /* Invoke postInitialize lifecycle method if defined
                         * (RIO-141) */
                        BeanHelper.invokeLifeCycle(Initialized.class, "postInitialize", loadResult.getImpl());

                        /* Create the ServiceBeanInstance */
                        instance = new ServiceBeanInstance(serviceID,
                                                           loadResult.getMarshalledInstance(),
                                                           context.getServiceBeanConfig(),
                                                           container.getComputeResource().getHostName(),
                                                           container.getComputeResource().getAddress().getHostAddress(),
                                                           container.getUuid());

                        /* Create the ServiceRecord */
                        serviceRecord = new ServiceRecord(serviceID,
                                                          sElem,
                                                          container.getComputeResource().getAddress().getHostName());
                    }

                    /* If we have not aborted, continue ... */
                    if(abortThrowable==null) {
                        container.started(identifier);
                    } else {
                        throw abortThrowable;
                    }

                } catch(Throwable t) {
                    StringBuilder buff = new StringBuilder();
                    String label = "classpath";
                    if(sElem.getComponentBundle()!=null) {
                        if(sElem.getComponentBundle().getArtifact()==null) {
                            String[] jars = sElem.getComponentBundle().getJARNames();
                            for(int i=0; i<jars.length; i++) {
                                if(i>0)
                                    buff.append(", ");
                                buff.append(sElem.getComponentBundle().getCodebase()).append(jars[i]);
                            }
                        } else {
                            label = "artifact";
                            buff.append(sElem.getComponentBundle().getArtifact());
                        }
                    } else {
                        buff.append("<unknown>");
                    }
                    abortThrowable = t;
                    if(t instanceof MissingMethodException) {
                        MissingMethodException e = (MissingMethodException)t;
                         System.out.println("===> "+sElem.getName()+", MISSING:"+e.getMethod());
                    }
                    logger.error("Failed to load the ServiceBean [{}] {} [{}]", sElem.getName(), label, buff.toString(),
                                 abortThrowable);
                    container.discarded(identifier);
                    terminate();
                }
            }
        };
        jsbThread.start();
        try {
            jsbThread.join();
            logger.trace("ServiceBean [{}] start thread completed", sElem.getName());
        } catch(InterruptedException e) {
            logger.warn("ServiceBean [{}] start Thread interrupted, abort start", sElem.getName());
        } finally {
            starting.set(false);
        }

        try {
            /* Invoke postStart lifecycle method if defined (RIO-141). Note that
             * we will not have a loadResult.impl of the service if it has been
             * forked/exec'd */
            if(loadResult!=null && loadResult.getImpl()!=null) {
                Class<? extends Annotation> annotation =
                    BeanHelper.hasAnnotation(loadResult.getImpl(), PostConstruct.class)?
                    PostConstruct.class :Started.class;
                BeanHelper.invokeLifeCycle(annotation, "postStart", loadResult.getImpl());
            }
        } catch (Exception e) {
            logger.warn("Failed to invoke the postStart() lifecycle method on the target bean. " +
                        "At this point this is considered a non-fatal exception",
                        e);
        } finally {
            /* Remove the initialization cookie, Make sure to check for the
             * context, similar to the above, we will not have a context for
             * the service if it has been forked/exec'd (RIO-141) */
            if(context!=null) {
                Map<String, Object> configParms = context.getServiceBeanConfig().getConfigurationParameters();
                configParms.remove(Constants.STARTING);
                context.getServiceBeanConfig().setConfigurationParameters(configParms);
            }
        }
        if(abortThrowable != null) {
            for(PlatformCapability pCap : installedPlatformCapabilities) {
                pCap.decrementUsage();
            }
            ServiceBeanInstantiationException toThrow;
            if(abortThrowable instanceof ServiceBeanInstantiationException) {
                toThrow = (ServiceBeanInstantiationException) abortThrowable;
            } else {
                toThrow = new ServiceBeanInstantiationException(String.format("ServiceBean [%s] instantiation failed",
                                                                              ServiceLogUtil.logName(sElem)),
                                                                abortThrowable,
                                                                true);
            }
            stagedDataManager.removeInstalledPlatformCapabilities();
            throw toThrow;
        }
    }

    /*
     * Get the created implementation
     */
    public Object getImpl() {
        return loadResult.getImpl();
    }

    /*
     * Get the service proxy
     */
    public Object getProxy() {
        return serviceProxy;
    }

    public ServiceBeanLoaderResult getLoadedServiceResult() {
        return loadResult;
    }

    /*
     * Register any PlatformCapability mbeans we created for
     * the ServiceBean
     */
    private void registerPlatformCapabilities() {
        for (PlatformCapability pCap :
            stagedDataManager.getInstalledPlatformCapabilities()) {
            try {
                ObjectName objectName = JMXUtil.getObjectName(context, "", "PlatformCapability", pCap.getName());
                MBeanServerFactory.getMBeanServer().registerMBean(pCap, objectName);
            } catch (Exception e) {
                Throwable cause = e;
                if (e.getCause() != null)
                    cause = e.getCause();
                logger.warn("Registering PlatformCapability [{}] to JMX", pCap.getName(), cause);
            }
        }
    }

    /*
     * Unregister a PlatformCapability from JMX
     */
    private void unregisterPlatformCapability(PlatformCapability pCap) {
        try {
            if(context==null)
                return;
            ObjectName objectName = JMXUtil.getObjectName(context, "", "PlatformCapability", pCap.getName());
            MBeanServerFactory.getMBeanServer().unregisterMBean(objectName);
        } catch(Exception e) {
            logger.warn("Unregistering PlatformCapability [{}]:{}", pCap.getName(), e.toString(), e);
        }
    }

    /*
     * Set discarded attributes in the ServiceRecord
     */
    private void setDiscarded() {
        /* check if the service is "starting" this may happen if service
         * creation is aborted before the start thread completes */
        if(starting.get()) {
            int iterations = 0;
            while(serviceRecord==null && iterations<4) {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    //
                }
                iterations++;
            }
        }
        isDiscarded.set(true);
        if(serviceRecord==null) {
            logger.warn("Discarding [{}] service, has no ServiceRecord", sElem.getName());
            return;
        }
        synchronized(serviceRecordLock) {
            serviceRecord.setDiscardedDate(new Date());
            serviceRecord.setType(ServiceRecord.INACTIVE_SERVICE_RECORD);
        }
    }

    private boolean runningForked() {
        return (System.getProperty(Constants.SERVICE_BEAN_EXEC_NAME)!=null);
    }


    /**
     * The JSBDiscardManager provides a mechanism to manage the discarding of a 
     * ServiceBean
     */
    class JSBDiscardManager implements DiscardManager, LifeCycle {

        /**
         * @see org.rioproject.impl.container.DiscardManager#discard
         */
        public void discard() {
            if(terminated.get())
                return;
            setDiscarded();
            container.discarded(identifier);
            instance=null;
            terminate();
        }

        /**
         * @see com.sun.jini.start.LifeCycle#unregister
         */
        public boolean unregister(Object impl) {
            discard();
            return(true);
        }
    }

}
