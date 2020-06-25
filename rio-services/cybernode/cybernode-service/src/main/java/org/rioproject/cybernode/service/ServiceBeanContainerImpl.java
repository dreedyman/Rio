/*
 * Copyright to the original author or authors.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *         http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.rioproject.cybernode.service;

import net.jini.config.Configuration;
import net.jini.id.Uuid;
import net.jini.id.UuidFactory;
import org.rioproject.impl.container.ServiceBeanContainer;
import org.rioproject.impl.container.ServiceBeanContainerListener;
import org.rioproject.impl.container.ServiceBeanDelegate;
import org.rioproject.impl.container.ServiceLogUtil;
import org.rioproject.deploy.ServiceBeanInstance;
import org.rioproject.deploy.ServiceBeanInstantiationException;
import org.rioproject.deploy.ServiceRecord;
import org.rioproject.event.EventHandler;
import org.rioproject.opstring.OperationalStringManager;
import org.rioproject.opstring.ServiceElement;
import org.rioproject.impl.system.ComputeResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The ServiceBeanContainerImpl implements support for a ServiceBeanContainer
 *
 * @author Dennis Reedy
 */
public class ServiceBeanContainerImpl implements ServiceBeanContainer {
    /** identifier token */
    private final AtomicInteger token = new AtomicInteger(0);
    /** Whether or not we are in a shutdown mode */
    private final AtomicBoolean shutdownSequence = new AtomicBoolean(false);
    /** The ComputeResource attribute associated to this ServiceBeanContainer */
    private ComputeResource computeResource;
    /** Collection of ServiceBeanDelegates */
    private final Map<Object, ServiceBeanDelegate> controllerMap = new ConcurrentHashMap<>();
    /**
     * Count of activations that have registered with controllerMap but activate
     * routine has not finished.
     */
    private final AtomicInteger activationInProcessCount = new AtomicInteger(0);
    /** Uuid for the container */
    private Uuid uuid;
    /** Collection of ServiceBeanContainerListeners */
    private final List<ServiceBeanContainerListener> listeners = new CopyOnWriteArrayList<>();
    /** Configuration object, which is also used as the shared configuration */
    private final Configuration config;
    /** Logger */
    private static final Logger logger = LoggerFactory.getLogger("org.rioproject.cybernode");

    /**
     * Create a new ServiceBeanContainer
     *
     * @param config The Configuration to use
     */
    public ServiceBeanContainerImpl(Configuration config) {
        this.config = config;
    }

    /**
     * @see org.rioproject.impl.container.ServiceBeanContainer#getSharedConfiguration()
     */
    public Configuration getSharedConfiguration() {
        return config;
    }

    /**
     * Set the computeResource property.
     * 
     * @param computeResource The ComputeResource to use
     */
    public void setComputeResource(ComputeResource computeResource) {
        this.computeResource = computeResource;
    }

    /**
     * @see org.rioproject.impl.container.ServiceBeanContainer#getComputeResource()
     */
    public ComputeResource getComputeResource() {
        return (computeResource);
    }

    /**
     * Terminate the ServiceBeanContainer
     */
    public void terminate() {
        shutdownSequence.set(true);
        terminateServices();
    }
    
    /**
     * Terminate the ServiceBeanContainer
     */
    public void terminateServices() {
        for (ServiceBeanDelegate delegate : controllerMap.values()) {
            delegate.terminate();
        }
        controllerMap.clear();
    }

    /**
     * @see org.rioproject.impl.container.ServiceBeanContainer#getServiceRecords
     */
    public ServiceRecord[] getServiceRecords() {
        List<ServiceRecord> list = new ArrayList<>();
        for (ServiceBeanDelegate delegate : controllerMap.values()) {
            ServiceRecord record = delegate.getServiceRecord();
            if (record != null) {
                list.add(record);
            }
        }
        return list.toArray(new ServiceRecord[0]);
    }

    /**
     * @see org.rioproject.impl.container.ServiceBeanContainer#getServiceCounter()
     */
    public synchronized int getServiceCounter() {
        return controllerMap.size();
    }

    /**
     * @see org.rioproject.impl.container.ServiceBeanContainer#getActivationInProcessCount()
     */
    public int getActivationInProcessCount() {
        return activationInProcessCount.get();
    }
    
    /**
     * @see org.rioproject.impl.container.ServiceBeanContainer#activate
     */
    public ServiceBeanInstance activate(ServiceElement sElem,
                                        OperationalStringManager opStringMgr,
                                        EventHandler slaEventHandler)
    throws ServiceBeanInstantiationException {
        Uuid serviceID = UuidFactory.generate();
        Integer identifier = token.incrementAndGet();

        ServiceBeanDelegateImpl delegate = new ServiceBeanDelegateImpl(identifier, serviceID, this);
        delegate.setOperationalStringManager(opStringMgr);
        delegate.setServiceElement(sElem);
        delegate.setEventHandler(slaEventHandler);
        controllerMap.put(identifier, delegate);
        activationInProcessCount.incrementAndGet();
        boolean started = false;
        ServiceBeanInstance loadedInstance;
        try {
            loadedInstance = delegate.load();
            started = true;
            /* notification to shutdown may have come in the middle of
             * service creation, if it did, terminate */
            if(shutdownSequence.get()) {
                delegate.terminate();
                throw new ServiceBeanInstantiationException("Resource unavailable, shutting down");
            }
            if(sElem.getAutoAdvertise()) {
                delegate.advertise();
            }

        } catch(ServiceBeanInstantiationException e) {
            if(started) {
                discarded(identifier);
                delegate.terminate();
            }
            /* rethrow ServiceBeanInstantiationException */
            throw e;
        } catch(Throwable t) {
            if(started) {
                discarded(identifier);
                delegate.terminate();
            }
            logger.error("Could not activate service {}", ServiceLogUtil.logName(sElem), t);
            throw new ServiceBeanInstantiationException(String.format("Service %s load failed", ServiceLogUtil.logName(sElem)),
                                                        t, true);
        } finally {
            activationInProcessCount.decrementAndGet();
        }
        return (loadedInstance);
    }
    
    /**
     * @see org.rioproject.impl.container.ServiceBeanContainer#update
     */
    public void update(ServiceElement[] elements,  OperationalStringManager opStringMgr) {
        if(elements==null) {
            throw new IllegalArgumentException("elements is null");
        }
        if(opStringMgr==null) {
            throw new IllegalArgumentException("opStringMgr is null");
        }

        for (ServiceElement element : elements) {
            ServiceBeanDelegate[] delegates = getDelegates(element);
            for (ServiceBeanDelegate delegate : delegates) {
                if(delegate.isActive()) {
                    delegate.update(element, opStringMgr);
                } else {
                    logger.warn(String.format("Service %s has been discarded, do not update",
                                              ServiceLogUtil.logName(element)));
                }
            }
        }
    }

    /**
     * @see org.rioproject.impl.container.ServiceBeanContainer#getServiceBeanInstances
     */
    public ServiceBeanInstance[] getServiceBeanInstances(ServiceElement element) {
        List<ServiceBeanInstance> list = new ArrayList<>();
        ServiceBeanDelegate[] delegates = getDelegates(element);
        for (ServiceBeanDelegate delegate : delegates) {
            ServiceBeanInstance instance = delegate.getServiceBeanInstance();
            if (instance != null) {
                list.add(instance);
            }
        }
        return list.toArray(new ServiceBeanInstance[0]);
    }

    public void setUuid(Uuid uuid) {
        this.uuid = uuid;
    }

    public Uuid getUuid() {
        return uuid;
    }

    /*
    * Get all ServiceBeanDelegate instances for a ServiceElement
    */
    public ServiceBeanDelegate[] getDelegates(ServiceElement element) {
        ArrayList<ServiceBeanDelegate> list = new ArrayList<>();
        if(element!=null) {
            for (ServiceBeanDelegate delegate : controllerMap.values()) {
                if (delegate.getServiceElement().equals(element)) {
                    list.add(delegate);
                }
            }
        }
        return list.toArray(new ServiceBeanDelegate[0]);
    }

    /**
     * @see org.rioproject.impl.container.ServiceBeanContainer#started(Object)
     */
    public void started(Object identifier) {
        ServiceBeanDelegate delegate = controllerMap.get(identifier);
        if(delegate == null) {
            return;
        }
        ServiceRecord record = delegate.getServiceRecord();
        if(record==null) {
            logger.warn("ServiceRecord for [{}] is null, no way to notify container of instantiation",
                           delegate.getServiceElement().getName());
            return;
        }
        notifyOnInstantiation(delegate.getServiceRecord());
    }

    /**
     * @see org.rioproject.impl.container.ServiceBeanContainer#discarded(Object)
     */
    public void discarded(Object identifier) {
        ServiceBeanDelegate delegate = controllerMap.get(identifier);
        if(delegate == null) {
            return;
        }
        notifyOnDiscard(delegate.getServiceRecord());
    }

    /**
     * @see org.rioproject.impl.container.ServiceBeanContainer#remove(Object)
     */
    public void remove(Object identifier) {
        if(shutdownSequence.get())
            return;
        controllerMap.remove(identifier);
    }

    /**
     * @see org.rioproject.impl.container.ServiceBeanContainer#addListener
     */
    public synchronized void addListener(ServiceBeanContainerListener l) {
        if(!listeners.contains(l))
            listeners.add(l);
    }

    /**
     * @see org.rioproject.impl.container.ServiceBeanContainer#addListener
     */
    public synchronized void removeListener(ServiceBeanContainerListener l) {
        listeners.remove(l);
    }

    public ServiceBeanDelegate getServiceBeanDelegate(Uuid serviceUuid) {
        ServiceBeanDelegate delegate = null;
        ServiceBeanDelegate[] delegates = getDelegates();
        for(ServiceBeanDelegate d : delegates) {
            if(d.getServiceBeanInstance()!=null &&
               d.getServiceBeanInstance().getServiceBeanID().equals(serviceUuid)) {
                delegate = d;
                break;
            }
        }
        return delegate;
    }

    /*
     * Notify all <code>ServiceBeanContainerListener</code> objects that a
     * ServiceBean has just been instantiated
     */
    void notifyOnInstantiation(ServiceRecord serviceRecord) {
        for(ServiceBeanContainerListener l : listeners) {
            l.serviceInstantiated(serviceRecord);
        }
    }

    /*
     * Notify all <code>ServiceBeanContainerListener</code> objects that a
     * ServiceBean has just been discarded
     */
    void notifyOnDiscard(ServiceRecord serviceRecord) {
        for(ServiceBeanContainerListener l : listeners) {
            l.serviceDiscarded(serviceRecord);
        }
    }

    ServiceBeanDelegate[] getDelegates() {
        ServiceBeanDelegate[] delegates;
        Collection<ServiceBeanDelegate> controllers = controllerMap.values();
        delegates = controllers.toArray(new ServiceBeanDelegate[0]);
        return delegates;
    }

}
