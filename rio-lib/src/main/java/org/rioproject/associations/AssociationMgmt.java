/*
 * Copyright 2008 the original author or authors.
 * Copyright 2005 Sun Microsystems, Inc.
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
package org.rioproject.associations;

import com.sun.jini.admin.DestroyAdmin;
import net.jini.admin.Administrable;
import net.jini.admin.JoinAdmin;
import net.jini.config.Configuration;
import net.jini.config.ConfigurationException;
import net.jini.config.EmptyConfiguration;
import net.jini.core.entry.Entry;
import net.jini.core.lookup.*;
import net.jini.discovery.DiscoveryEvent;
import net.jini.discovery.DiscoveryListener;
import net.jini.discovery.DiscoveryManagement;
import net.jini.id.Uuid;
import net.jini.id.UuidFactory;
import net.jini.lookup.LookupCache;
import net.jini.lookup.ServiceDiscoveryEvent;
import net.jini.lookup.entry.Name;
import net.jini.security.BasicProxyPreparer;
import net.jini.security.ProxyPreparer;
import org.rioproject.admin.ServiceBeanControl;
import org.rioproject.config.Constants;
import org.rioproject.core.jsb.ServiceBeanContext;
import org.rioproject.cybernode.ServiceBeanContainer;
import org.rioproject.cybernode.ServiceBeanContainerListener;
import org.rioproject.cybernode.ServiceBeanDelegate;
import org.rioproject.deploy.ServiceBeanInstance;
import org.rioproject.deploy.ServiceRecord;
import org.rioproject.fdh.FaultDetectionHandler;
import org.rioproject.fdh.FaultDetectionHandlerFactory;
import org.rioproject.fdh.FaultDetectionListener;
import org.rioproject.opstring.OperationalStringManager;
import org.rioproject.resources.client.DiscoveryManagementPool;
import org.rioproject.resources.client.JiniClient;
import org.rioproject.resources.client.LookupCachePool;
import org.rioproject.resources.client.ServiceDiscoveryAdapter;
import org.rioproject.resources.util.ThrowableUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.beans.IntrospectionException;
import java.io.IOException;
import java.lang.reflect.Method;
import java.rmi.MarshalledObject;
import java.rmi.RemoteException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The AssociationMgmt class implements the AssociationManagement interface.
 *
 * @author Dennis Reedy
 */
public class AssociationMgmt implements AssociationManagement {
    /**
     * Registered AssociationListener instances.
     */
    private final List<AssociationListener> listeners = new ArrayList<AssociationListener>();
    /**
     * Collection of AssociationDescriptor.
     */
    private final List<AssociationHandler> associationHandlers = new ArrayList<AssociationHandler>();
    /**
     * The ServiceBeanControl object for the ServiceBean.
     */
    private ServiceBeanControl control;
    /**
     * If true, and the service has an Association with a type of
     * AssociationType.REQUIRES and the Association is broken, the
     * AssociationMgmt object will unadvertise the ServiceBean using the
     * ServiceBean instance's ServiceBeanControl object. If false, the
     * AssociationMgmt object will not unadvertise the ServiceBean.
     */
    private final AtomicBoolean unadvertiseOnBroken = new AtomicBoolean(true);
    /**
     * Counter to indicate that we have at least one Association that is
     * AssociationType.REQUIRES.
     */
    private final AtomicInteger numRequires = new AtomicInteger(0);
    /**
     * Indicates whether the JSB is advertised or not.
     */
    private final AtomicBoolean advertised = new AtomicBoolean(false);
    /**
     * Indicates that all required associations have been met, and for some
     * reason the ServiceBeanControl object has not been set.
     */
    private final AtomicBoolean advertisePending = new AtomicBoolean(false);
    /**
     * The ServiceBeanContext.
     */
    private ServiceBeanContext context;
    /**
     * Internal AssociationListener
     */
    private Listener listener;
    /**
     * The ClassLoader which will be used to provide the caller/client 
     * with a properly annotated proxy for associated services
     */
    private ClassLoader callerCL;
    /**
     * The ServiceBeanContainer the service is running in
     */
    private ServiceBeanContainer container;
    /**
     * Name of the service (client) that has the associations.
     */
    private String clientName="<unknown>";
    /**
     * The configuration to use for obtaining a proxy preparer
     */
    private Configuration config;
    /**
     * The Logger.
     */
    private static final Logger logger = LoggerFactory.getLogger(AssociationMgmt.class);
    /**
     * Configuration component attribute
     */
    private static final String CONFIG_COMPONENT = "service.association";

    /**
     * Create an AssociationMgmt instance. Uses the current thread's context
     * class loader to provide the caller/client with properly annotated
     * proxies for associated services
     *
     */
    public AssociationMgmt() {
        this(null);
    }

    /**
     * Create an AssociationMgmt instance. Uses the specified class loader
     * to provide the caller/client with properly annotated proxies for
     * associated services, or the current thread's context class
     * loader if cl is null.
     *
     * @param cl  The class loader to provide the caller/client with properly
     */
    public AssociationMgmt(final ClassLoader cl) {
        if(cl==null) {
            final Thread currentThread = Thread.currentThread();
            callerCL = AccessController.doPrivileged(
                new PrivilegedAction<ClassLoader>() {
                    public ClassLoader run() {
                        return (currentThread.getContextClassLoader());
                    }
                });
        } else {
            callerCL = cl;
        }
        listener = new Listener();
    }

    /**
     * {@inheritDoc}
     */
    public void setUnadvertiseOnBroken(final boolean unadvertiseOnBroken) {
        this.unadvertiseOnBroken.set(unadvertiseOnBroken);
    }

    /**
     * @see AssociationManagement#setServiceBeanControl
     */
    public void setServiceBeanControl(final ServiceBeanControl control) {
        if(control == null)
            throw new IllegalArgumentException("control is null");
        this.control = control;
        if(advertisePending.get())
            advertise();
    }

    /**
     * {@inheritDoc}
     */
    public void setServiceBeanContainer(final ServiceBeanContainer container) {
        if(container == null)
            throw new IllegalArgumentException("control is null");
        this.container = container;
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public void register(final AssociationListener... listeners) {
        if (listeners == null)
            throw new IllegalArgumentException("listeners are null");
        synchronized (this.listeners) {
            this.listeners.addAll(Arrays.asList(listeners));
        }
        for (Association association : getAssociations()) {
            if (association.getState() == Association.State.PENDING ||
                association.getState() == Association.State.BROKEN)
                continue;
            for (AssociationListener listener : listeners) {
                if (checkAssociationListener(listener, association)) {
                    listener.discovered(association, association.getService());
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public void remove(final AssociationListener... listeners) {
        if (listeners == null)
            throw new IllegalArgumentException("listeners is null");
        synchronized (this.listeners) {
            this.listeners.removeAll(Arrays.asList(listeners));
        }
    }

    /**
     * {@inheritDoc}
     */
    public void terminate() {
        synchronized(associationHandlers) {
            for (AssociationHandler ah : associationHandlers) {
                ah.terminate();
            }
            if(listener!=null && listener.associationInjector!=null)
                listener.associationInjector.terminate();
            listeners.clear();
            associationHandlers.clear();
        }
        callerCL = null;
        control = null;
        logger.debug("Terminated AssociationManagement for {}", clientName);
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public <T> Association<T> getAssociation(final Class<T> serviceType,
                                             final String serviceName,
                                             final String opStringName) {
        if(serviceType == null)
            throw new IllegalArgumentException("A serviceType must be provided");

        Association<T> association = null;
        synchronized(associationHandlers) {
            for (AssociationHandler ah : associationHandlers) {
                Association a = ah.getAssociation();
                Object service = a.getService();
                boolean serviceTypeMatches = false;
                if(service!=null) {
                    serviceTypeMatches = isAssignableFrom(serviceType, service.getClass());
                } else {
                    ServiceTemplate template = ah.getServiceTemplate();
                    if(template!=null) {
                        for(Class c : template.serviceTypes) {
                            if(isAssignableFrom(serviceType, c)) {
                                serviceTypeMatches = true;
                                break;
                            }
                        }
                    }
                }
                boolean found = false;
                if (serviceTypeMatches) {
                    if(serviceName!=null) {
                        if (opStringName != null) {
                            if (a.getName().equals(serviceName) &&
                                a.getOperationalStringName().equals( opStringName)) {
                                found = true;
                            }
                        } else  if (a.getName().equals(serviceName)) {
                            found = true;
                        }
                    } else {
                        found = true;
                    }
                }
                if(found) {
                    association = a;
                    break;
                }
            }
        }
        return association;
    }

    /**
     * {@inheritDoc}
     */
    public List<Association<?>> getAssociations() {
        List<Association<?>> associations = new ArrayList<Association<?>>();
        synchronized(associationHandlers) {
            for (AssociationHandler ah : associationHandlers) {
                associations.add(ah.getAssociation());
            }
        }
        return Collections.unmodifiableList(associations);
    }

    /**
     * Set the PropertyDescriptor elements. This will be used to inject
     * associated services
     *
     * @param backend The backend (implementation) to use for property injection
     */
    public void setBackend(final Object backend) {
        try {
            listener.createAssociationInjector(backend);
        } catch(IntrospectionException e) {
            logger.warn("Creating AssociationInjector", e);
        }
    }

    /**
     * Set the name of the client that has the associations.
     *
     * @param clientName The name of the client that has the associations.
     * This is used for logging and diagnostics
     */
    private void setClientName(final String clientName) {
        this.clientName = clientName;
    }

    /**
     * @see AssociationManagement#setServiceBeanContext
     */
    public void setServiceBeanContext(final ServiceBeanContext context) {
        if(context==null)
            throw new IllegalArgumentException("context is null");
        boolean update = false;
        if(this.context!=null)
            update = true;
        this.context = context;
        StringBuilder nameBuilder = new StringBuilder();
        if(context.getServiceElement().getOperationalStringName()!=null &&
           context.getServiceElement().getOperationalStringName().length()>0) {
            nameBuilder.append(context.getServiceElement().getOperationalStringName()).append("/");
        }
        nameBuilder.append(context.getServiceElement().getName());
        if(context.getServiceElement().getServiceBeanConfig().getInstanceID()!=null) {
            nameBuilder.append(":").append(context.getServiceElement().getServiceBeanConfig().getInstanceID());
        }
        setClientName(nameBuilder.toString());
        ClassLoader currentCL = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(callerCL);
            setConfiguration(context.getConfiguration());
        } catch (ConfigurationException e) {
            logger.warn("Unable to get Configuration from ServiceBeanContext. Will proceed without configuration", e);
        } finally {
            Thread.currentThread().setContextClassLoader(currentCL);
        }
        AssociationDescriptor[] newDesc = context.getServiceElement().getAssociationDescriptors();
        if(newDesc != null) {
            if(!update) {
                if(logger.isTraceEnabled()) {
                    StringBuilder sb = new StringBuilder();
                    sb.append("[").append(clientName).append("]");
                    sb.append(" AssociationManagement descriptors=").append(newDesc.length);
                    if(newDesc.length>0)
                        sb.append("\n");
                    for(int i=0; i<newDesc.length; i++) {
                        if(i>0)
                            sb.append("\n");
                        sb.append(i+1).append(". ").append(newDesc[i].toString());
                    }
                    logger.trace(sb.toString());
                }

                addAssociationDescriptors(newDesc);
                if(numRequires.get()==0) {
                    advertised.set(true);
                }
            } else {
                /* Determine if any of the AssociationDescriptors are new or
                 * are no longer needed */
                AssociationDescriptor[] current = getAssociationDescriptors();
                /* Add new AssociationDescriptor instances */
                AssociationDescriptor[] addDesc = getDifference(newDesc, current);
                addAssociationDescriptors(addDesc);
                /* Remove AssociationHandlers which are no longer configured */
                AssociationDescriptor[] remDesc = getDifference(current, newDesc);
                AssociationHandler[] handlers = getAssociationHandlers();
                for (AssociationDescriptor aRemDesc : remDesc) {
                    for (AssociationHandler handler : handlers) {
                        if (handler.aDesc.equals(aRemDesc)) {
                            logger.trace("[{}] During update: terminate AssociationHandler for {}",
                                         clientName, handler.aDesc.toString());
                            handler.terminate();
                            if (handler.aDesc.getAssociationType()==AssociationType.REQUIRES) {
                                numRequires.decrementAndGet();
                                listener.requiredAssociations.remove(handler.association);
                            }
                            synchronized (associationHandlers) {
                                associationHandlers.remove(handler);
                            }
                            break;
                        }
                    }
                }
                listener.checkAdvertise();
            }
        }
    }

    protected Configuration getConfiguration() {
        return (config==null? EmptyConfiguration.INSTANCE:config);
    }

    public void setConfiguration(final Configuration config) {
        this.config = config;
    }

    /*
     * Check if the AssociationListener method has a type that is assignable
     */
    private boolean checkAssociationListener(final AssociationListener<?> al, final Association a) {
        return checkAssociationListener(al, a.getService());
    }

    /*
     * Check if the AssociationListener method has a type that is assignable
     */
    private boolean checkAssociationListener(final AssociationListener<?> al, final Object service) {
        if(service==null)
            return false;
        boolean ok = true;
        Class<?> svcClass = service.getClass();
        Method[] methods = al.getClass().getMethods();
        for(Method m : methods) {
            if(m.getName().equals("discovered")) {
                Class<?>[] types = m.getParameterTypes();
                if(!(types[1].isAssignableFrom(svcClass))) {
                    ok = false;
                    break;
                }
            }
        }
        return ok;
    }

    /*
     * Return an array of AssociationDescriptor instances which are
     * in the desc1 array but not in the desc2 array of AssociationDescriptor
     * instances
     */
    private AssociationDescriptor[] getDifference(final AssociationDescriptor[] desc1,
                                                  final AssociationDescriptor[] desc2) {
        ArrayList<AssociationDescriptor> diffList =
            new ArrayList<AssociationDescriptor>();
        for (AssociationDescriptor aDesc1 : desc1) {
            boolean matched = false;
            for (AssociationDescriptor aDesc2 : desc2) {
                if (aDesc1.equals(aDesc2)) {
                    matched = true;
                    break;
                }
            }
            if (!matched) {
                diffList.add(aDesc1);
            }
        }
        return(diffList.toArray(new AssociationDescriptor[diffList.size()]));
    }


    /*
     * Get an AssociationHandler for an Association
     *
     * @return Array of AssociationHandler instances
     */
    public AssociationHandler getAssociationHandler(final Association association) {
        AssociationHandler handler = null;
        AssociationHandler[] handlers = getAssociationHandlers();
        for (AssociationHandler handler1 : handlers) {
            if (handler1.getAssociation().equals(association)) {
                handler = handler1;
                break;
            }
        }
        return(handler);
    }

    /**
     * Get all AssociationHandler instances
     *
     * @return Array of AssociationHandler instances
     */
    private AssociationHandler[] getAssociationHandlers() {
        ArrayList<AssociationHandler> list = new ArrayList<AssociationHandler>();
        synchronized(associationHandlers) {
            for (AssociationHandler ah : associationHandlers) {
                list.add(ah);
            }
        }
        return (list.toArray(new AssociationHandler[list.size()]));
    }

    /**
     * Get all AssociationDescriptor instances
     *
     * @return Array of AssociationDescriptor instances
     */
    private AssociationDescriptor[] getAssociationDescriptors() {
        ArrayList<AssociationDescriptor> list = new ArrayList<AssociationDescriptor>();
        synchronized(associationHandlers) {
            for (AssociationHandler ah : associationHandlers) {
                list.add(ah.aDesc);
            }
        }
        return(list.toArray(new AssociationDescriptor[list.size()]));
    }

    /**
     * Get all AssociationListener instances
     *
     * @return An Array of AssociationListener
     * instances. A new array is allocated each time. If there are no
     * AssociationListener instances a zero-length array is returned
     */
    protected AssociationListener<?>[] getAssociationListeners() {
        AssociationListener[] aListeners;
        synchronized(listeners) {
            aListeners = listeners.toArray(new AssociationListener[listeners.size()]);
        }
        return (aListeners);
    }

    /**
     * Create an AssociationHandler
     *
     * @param aDesc The AssociationDescriptor to create an AssociationHandler
     * for
     *
     * @return An AssociationHandler for the AssociationDescriptor. A new
     * AssociationHandler is created each time this method is called
     *
     * @throws IllegalArgumentException if the AssociationDescriptor is null
     */
    protected AssociationHandler createAssociationHandler(final AssociationDescriptor aDesc) {
        if(aDesc == null)
            throw new IllegalArgumentException("aDesc is null");
        return(new AssociationHandler(aDesc));
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public <T> Association<T> addAssociationDescriptor(final AssociationDescriptor aDesc) {
        if(aDesc == null)
            throw new IllegalArgumentException("The AssociationDescriptor cannot be null");
        Association<T> association;
        AssociationHandler handler = getAssociationHandler(aDesc);
        if (handler!=null) {
            logger.trace("Already managing [{}] for [{}]", aDesc.toString(), clientName);
            association = handler.getAssociation();
        } else {
            if (aDesc.getAssociationType()==AssociationType.REQUIRES)
                numRequires.incrementAndGet();
            synchronized (associationHandlers) {
                handler = createAssociationHandler(aDesc);
                associationHandlers.add(handler);
            }
            handler.exec();
            association = handler.getAssociation();
        }
        return association;
    }

    /**
     * Checks to see if the service should be advertised.
     */
    public void checkAdvertise() {
        listener.checkAdvertise();
    }

    /**
     * {@inheritDoc}
     */
    public List<Association<?>> addAssociationDescriptors(final AssociationDescriptor... aDescs) {
        if(aDescs == null)
            throw new IllegalArgumentException("The AssociationDescriptor cannot be null");
        List<AssociationHandler> newHandlers = new ArrayList<AssociationHandler>();
        List<Association<?>> associations = new ArrayList<Association<?>>();
        for (AssociationDescriptor aDesc : aDescs) {
            AssociationHandler handler = getAssociationHandler(aDesc);
            if (handler!=null) {
                logger.trace("Already managing [{}] for [{}]", aDesc.toString(), clientName);
                associations.add(handler.getAssociation());
                continue;
            }
            if (aDesc.getAssociationType()==AssociationType.REQUIRES)
                numRequires.incrementAndGet();
            synchronized (associationHandlers) {
                handler = createAssociationHandler(aDesc);
                associationHandlers.add(handler);
                newHandlers.add(handler);
            }
        }
        for (AssociationHandler handler : newHandlers) {
            handler.exec();
            associations.add(handler.getAssociation());
        }

        return Collections.unmodifiableList(associations);
    }

    /*
     * Determine if we already have an association
     */
    private AssociationHandler getAssociationHandler(final AssociationDescriptor aDesc) {
        AssociationHandler aHandler = null;
        synchronized(associationHandlers) {
            for (AssociationHandler ah : associationHandlers) {
                if (ah.aDesc.equals(aDesc)) {
                    aHandler = ah;
                    break;
                }
            }
        }
        return aHandler;
    }

    /**
     * Internal AssociationListener to control advertise and unadvertise
     * support
     */
    @SuppressWarnings("unchecked")
    class Listener implements AssociationListener {
        private final List<Association> requiredAssociations = Collections.synchronizedList(new ArrayList<Association>());
        private AssociationInjector associationInjector;

        AssociationInjector getAssociationInjector() {
            return associationInjector;
        }

        void addRequiredAssociation(final Association association) {
            requiredAssociations.add(association);
        }
        /*
         * Create injector
         */
        private void createAssociationInjector(final Object backend) throws IntrospectionException {
            if(associationInjector==null)
                associationInjector = new AssociationInjector(backend);
            else
                associationInjector.setBackend(backend);
            associationInjector.setCallerClassLoader(callerCL);
        }

        /*
         * @see org.rioproject.associations.AssociationListener#discovered
         */
        public void discovered(final Association assoc, final Object service) {
            if(numRequires.get() > 0) {
                AssociationType aType = assoc.getAssociationType();

                if(aType.equals(AssociationType.REQUIRES)) {
                    if(!requiredAssociations.contains(assoc)) {
                        requiredAssociations.add(assoc);
                    }
                    if(associationInjector!=null) {
                        associationInjector.discovered(assoc, service);
                    }
                    checkAdvertise();
                }
            } else {
                if(associationInjector!=null)
                    associationInjector.discovered(assoc, service);
                checkAdvertise();
            }
        }

        /*
         * Check to see if the service can be advertised. If all required 
         * associations have been satisfied, and we're not advertised, then 
         * advertise the service
         */
        void checkAdvertise() {
            logger.trace("Check advertise for [{}] advertised={}, numRequires={}, requiredAssociations.size()={}",
                         clientName, advertised.get(), numRequires, requiredAssociations.size());
            if((requiredAssociations.size() >= numRequires.get()) && !advertised.get()) {
                if(context!=null) {
                    Map<String, Object> configParms = context.getServiceBeanConfig().getConfigurationParameters();
                    if(!configParms.containsKey(Constants.STARTING)) {
                        advertise();
                    } else {
                        logger.debug("[{}] is still starting, do not advertise", clientName);
                        context.getServiceElement().setAutoAdvertise(true);
                        advertisePending.set(true);
                    }
                } else {
                    logger.debug("AssociationManagement created without ServiceBeanContext, unable to verify service {} " +
                                 "lifecycle. Proceeding with service advertisement", clientName);
                    advertise();
                }
            } else if((requiredAssociations.size() < numRequires.get()) && advertised.get())
                unadvertise();
        }

        /*
         * Check to see if the service should be unadvertised. This check is
         * done if the AssociationDescriptors have changed. In this case we
         * may have added a requires association, and we need to make sure all
         * required associations are satisfied. If they are not, unadvertise,
         * otherwise check to make sure the proper state is set
         */
        /*void checkUnadvertise() {
            if((requiredAssociations.size() < numRequires.get()) && advertised.get())
                unadvertise();
        }*/

        /*
         * @see org.rioproject.associations.AssociationListener#changed
         */
        public void changed(final Association association, final Object service) {
            if(associationInjector!=null)
                associationInjector.changed(association, service);
        }

        /*
         * @see org.rioproject.associations.AssociationListener#broken
         */
        public void broken(final Association association, final Object service) {
            if(associationInjector!=null)
                associationInjector.broken(association, service);
            if(association.getAssociationType()==AssociationType.REQUIRES) {
                requiredAssociations.remove(association);
                unadvertise();
            }
        }
    }

    /**
     * Advertise the ServiceBean
     */
    private synchronized void advertise() {
        try {
            if(control != null && !advertised.get()) {
                logger.info("Advertise Service: {}", clientName);
                control.advertise();
                advertised.set(true);
                advertisePending.set(false);
            } else {
                advertisePending.set(true);
                logger.debug("Service [{}] advertisement pending, ServiceBeanControl is null", clientName);
            }
        } catch(Exception e) {
            logger.warn("Failed advertising service: {}", clientName, e);
            decrementOnFailedAdvertiseLifecycle();
        }
    }

    /**
     * Unadvertise the Service
     */
    private void unadvertise() {
        logger.info("[{}] unadvertiseOnBroken : {}", clientName, unadvertiseOnBroken.get());
        if(unadvertiseOnBroken.get()) {
            try {
                if(control != null) {
                    control.unadvertise();
                    advertised.set(false);
                    logger.debug("Unadvertise Service : {}", clientName);
                } else {
                    logger.warn("Cannot unadvertise Service [{}], ServiceBeanControl is null", clientName);
                }
            } catch(Exception e) {
                logger.warn("Failed unadvertising Service [{}] while processing Broken Association", clientName, e);
                decrementOnFailedAdvertiseLifecycle();
            }
        }
    }

    private void decrementOnFailedAdvertiseLifecycle() {
        if(context!=null) {
            OperationalStringManager manager = context.getServiceBeanManager().getOperationalStringManager();
            try {
                manager.decrement(context.getServiceBeanManager().getServiceBeanInstance(),
                                  true,
                                  true);
                logger.debug("Decremented {}, the instance will not be re-allocated", clientName);
            } catch (Exception e1) {
                logger.warn("Unable to remove service {}/{}:{} instance from OperationalStringManager",
                            context.getServiceElement().getOperationalStringName(),
                            clientName,
                            context.getServiceElement().getServiceBeanConfig().getInstanceID(), e1);
            }
        } else {
            logger.warn("The ServiceBeanContext is null, unable to remove the service {}", clientName);
        }
    }

    /**
     * Notify listeners on discovery
     *
     * @param association The Association
     * @param service The discovered service
     */
    @SuppressWarnings("unchecked")
    protected void notifyOnDiscovery(final Association association, final Object service) {
        AssociationListener[] listeners = getAssociationListeners();
        logger.trace("[{}] DISCOVERED [{}], Notify [{}] listeners", clientName, association.getName(), listeners.length+1);
        try {
            for (AssociationListener al : listeners) {
                if(checkAssociationListener(al, association)) {
                    al.discovered(association, service);
                }
            }
        } finally {
            listener.discovered(association, service);
        }
    }

    /**
     * Notify listeners on Association change
     *
     * @param association The Association
     * @param service The discovered service
     */
    @SuppressWarnings("unchecked")
    protected void notifyOnChange(final Association association, final Object service) {
        AssociationListener[] listeners = getAssociationListeners();
        logger.trace("[{}] CHANGED [{}], Notify [{}] listeners", clientName, association.getName(), listeners.length);
        try {
            for (AssociationListener al : listeners) {
                if(checkAssociationListener(al, association))
                    al.changed(association, service);
            }
        } finally {
            listener.changed(association, service);
        }
    }

    /**
     * Notify listeners that the Association is broken
     *
     * @param association The Association
     * @param service The discovered service
     */
    @SuppressWarnings("unchecked")
    protected void notifyOnBroken(final Association association, final Object service) {
        AssociationListener[] listeners = getAssociationListeners();
        logger.trace("[{}] BROKEN [{}], Type={}, Notify [{}] listeners",
                     clientName, association.getName(), association.getAssociationType().toString(), listeners.length);
        try {
            for (AssociationListener al : listeners) {
                if(checkAssociationListener(al, service))
                    al.broken(association, service);
            }
        } finally {
            listener.broken(association, service);
        }

        /* If we have a colocated association that is broken destroy the
         * service */
        AssociationHandler handler = getAssociationHandler(association);
        boolean colocatedDestroy = true;
        if(handler!=null) {
            try {
                colocatedDestroy = (Boolean) getConfiguration().getEntry(CONFIG_COMPONENT,
                                                                         "colocatedDestroy",
                                                                         boolean.class,
                                                                         true);
            } catch(Exception e) {
                logger.warn("Getting {}.colocatedDestroy property from association configuration", CONFIG_COMPONENT, e);
            }
        }
        if(association.getAssociationType()==AssociationType.COLOCATED &&
           colocatedDestroy) {
            try {
                logger.trace("[{}] Colocated Association to [{}] BROKEN, terminate service",
                             clientName, association.getName());
                if(context==null) {
                    logger.debug("ServiceBeanContext is null, unable to destroy {}", clientName);
                    return;
                }
                Object proxy = context.getServiceBeanManager().getServiceBeanInstance().getService();
                if(proxy instanceof Administrable) {
                    Object admin = ((Administrable)proxy).getAdmin();
                    if(admin instanceof DestroyAdmin) {
                        ((DestroyAdmin)admin).destroy();
                    } else {
                        logger.warn("{} admin object does not implement {} access, unable to terminate",
                                    clientName, DestroyAdmin.class.getName());
                    }
                } else {
                    logger.warn("{} does not provide an {} access, unable to terminate",
                                clientName, Administrable.class.getName());
                }
            } catch(Exception e) {
                logger.warn("{} Destroying from broken colocated asssociation", clientName, e);
            }
        }
    }

    /*
     * Tests if class <code>c1</code> is equal to, or a superclass of, class
     * <code>c2</code>, using the class equivalence semantics of the lookup
     * service: same name.
     */
    private boolean isAssignableFrom(final Class<?> c1, final Class<?> c2) {
        if(c1.isAssignableFrom(c2))
            return true;
        String n1 = c1.getName();
        for(Class sup = c2; sup != null; sup = sup.getSuperclass()) {
            if(n1.equals(sup.getName()))
                return true;
        }
        return false;
    }

    /**
     * The AssociationHandler handle an Association created from
     * AssociationDescriptor.
     */
    public class AssociationHandler extends ServiceDiscoveryAdapter implements FaultDetectionListener <ServiceID> {
        /** The AssociationDescriptor */
        private final AssociationDescriptor aDesc;
        /** Number of instances of the associated service */
        private int instances;
        /**
         * Table of service IDs to FaultDetectionHandler instances, one for
         * each service
         */
        private final Map<ServiceID, FaultDetectionHandler> fdhTable = new Hashtable<ServiceID, FaultDetectionHandler>();
        /** The Association for the AssociationHandler */
        private final Association association;
        /** The LookupCache for the ServiceDiscoveryManager */
        private LookupCache lCache;
        /** The LookupServiceHandler */
        private LookupServiceHandler lookupServiceHandler;
        /** A ProxyPreparer for discovered services */
        private ProxyPreparer proxyPreparer;
        private ServiceTemplate template;

        /**
         * Create an AssociationHandler
         *
         * @param assocDesc The AssociationDescriptor describing the
         * attributes of the Association
         */
        public AssociationHandler(final AssociationDescriptor assocDesc) {
            aDesc = assocDesc;
            association = new Association(aDesc);
            logger.trace("AssociationManagement created for [{}], [{}]", clientName, assocDesc);
        }

        AssociationDescriptor getAssociationDescriptor() {
            return aDesc;
        }

        /**
         * Begin handling the association
         */
        @SuppressWarnings("unchecked")
        protected void exec() {
            if(!aDesc.isLazyInject()) {
                listener.getAssociationInjector().injectEmpty(association);
                listener.addRequiredAssociation(association);
                listener.checkAdvertise();
            }
            try {
                boolean lookupService = false;
                /* See if the association is a lookup service */
                String[] iFaces = aDesc.getInterfaceNames();
                for (String iFace : iFaces) {
                    if (iFace.equals("net.jini.core.lookup.ServiceRegistrar")) {
                        lookupService = true;
                        break;
                    }
                }
                
                /* Process colocated associations without discovery management */
                if(association.getAssociationType()==AssociationType.COLOCATED && container!=null) {
                    try {
                        handleColocatedServiceInjection(this);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    //new ColocatedListener();
                } else if(!lookupService) {
                    try {
                        proxyPreparer = (ProxyPreparer)getConfiguration().getEntry(CONFIG_COMPONENT,
                                                                                   "proxyPreparer",
                                                                                   ProxyPreparer.class,
                                                                                   new BasicProxyPreparer());
                    } catch (ConfigurationException e) {
                        logger.warn("Could not create ProxyPreparer from configuration, using default", e);
                        proxyPreparer = new BasicProxyPreparer();
                    }
                    logger.trace("Association [{}]  ProxyPreparer : {}",
                                 aDesc.getName(), proxyPreparer.getClass().getName());
                    template = JiniClient.getServiceTemplate(aDesc, callerCL);
                    logger.trace("Created ServiceTemplate {}", template.toString());
                    LookupCachePool lcPool = LookupCachePool.getInstance();
                    String sharedName = aDesc.getOperationalStringName();

                    lCache = lcPool.getLookupCache(sharedName, aDesc.getGroups(), aDesc.getLocators(), template);
                    lCache.addListener(this);
                    logger.debug("AssociationManagement for [{}], obtained LookupCache for [{}]",
                                 clientName, aDesc.getName());
                } else {
                    lookupServiceHandler = new LookupServiceHandler(aDesc, this, getConfiguration());
                }
            } catch(IllegalStateException e) {
                logger.warn("Creating an AssociationHandler, {}", e.getMessage());
            } catch(IOException e) {
                logger.warn("Creating an AssociationHandler", e);
            } catch (ClassNotFoundException e) {
                logger.error("Creating an AssociationHandler", e);
            } catch(Throwable t) {
                logger.error("Creating an AssociationHandler", t);
            }
        }

        ServiceTemplate getServiceTemplate() {
            return template;
        }

        /**
         * Stop the AssociationHandler, terminating the FaultDetectionListener
         * and ServiceDiscoveryManager
         */
        void terminate() {
            association.terminate();
            if(lCache!=null) {
                try {
                    lCache.removeListener(this);
                } catch (Throwable t) {
                    logger.warn("Exception {} removing Listener from LookupCache", t.getClass().getName());
                }
            }
            Set<ServiceID> keySet = fdhTable.keySet();
            for (ServiceID aKeySet : keySet) {
                FaultDetectionHandler fdh = fdhTable.get(aKeySet);
                if (fdh != null) {
                    fdh.terminate();
                }
            }
            if(lookupServiceHandler != null)
                lookupServiceHandler.terminate();
        }

        /*
         * Create a ServiceItem from a ServiceBeanInstance
         */
        private ServiceItem makeServiceItem(final ServiceBeanInstance instance)
            throws IOException, ClassNotFoundException {
            Uuid uuid = instance.getServiceBeanID();
            ServiceID serviceID = new ServiceID(uuid.getMostSignificantBits(), uuid.getLeastSignificantBits());
            Entry[] attrs = null;
            Object service = instance.getService();
            if(service instanceof Administrable) {
                try {
                    Object admin = ((Administrable)service).getAdmin();
                    if(admin instanceof JoinAdmin) {
                        attrs = ((JoinAdmin)admin).getLookupAttributes();
                    }
                } catch(RemoteException e) {
                    logger.warn("Getting attributes from [{}]", getAssociation().getName(), e);
                }
            }
            return(new ServiceItem(serviceID, service, attrs));
        }

        /**
         * Notification that a service has been discovered. This method will
         * first marshall then unmarshal the discovered ServiceItem (and
         * hence all contained classes) using the caller's classloader.
         * 
         * Since we are using the LookupCachePool, the "original" proxy
         * reference may have been obtained using a sibling class loader, and
         * the proxy may not be "usable" by the caller.
         *
         * This method will additionally use the configured ProxyPreparer to
         * prepare the discovered proxy. If no ProxyPreparer has been
         * configured, the default proxy preparer (BasicProxyPreparer that
         * does nothing but return the original proxy) is used
         *
         * @param sdEvent The ServiceDiscoveryEvent
         */
        public void serviceAdded(final ServiceDiscoveryEvent sdEvent) {
            ServiceItem item = sdEvent.getPostEventServiceItem();
            final Thread currentThread = Thread.currentThread();
            final ClassLoader cCL = AccessController.doPrivileged(
                    new PrivilegedAction<ClassLoader>() {
                        public ClassLoader run() {
                            return (currentThread.getContextClassLoader());
                        }
                    });
            if(item.service==null) {
                logger.debug("[{}] serviceAdded [{}], service is null, abort notification",
                             clientName, association.getName());
                return;
            }
            try {
                AccessController.doPrivileged(new PrivilegedAction<Void>() {
                    public Void run() {
                        currentThread.setContextClassLoader(callerCL);
                        return null;
                    }
                });
                item = new MarshalledObject<ServiceItem>(item).get();
                item.service = proxyPreparer.prepareProxy(item.service);
                try {
                    serviceDiscovered(item);
                } catch(Exception e) {
                    if(!ThrowableUtil.isRetryable(e)) {
                        lCache.discard(item.service);
                    }
                    logger.trace("Handling service discovery, unable to attach FDH", e);
                }
            } catch(Exception e) {
                logger.warn("(Un)Marshalling ServiceItem", e);
            } finally {
                AccessController.doPrivileged(new PrivilegedAction<Void>() {
                    public Void run() {
                        currentThread.setContextClassLoader(cCL);
                        return (null);
                    }
                });
            }
        }

        /**
         * Add a service
         *
         * @param item The service being added
         *
         * @throws Exception if the fault detection handler cannot be attached
         */
        protected void serviceDiscovered(final ServiceItem item) throws Exception {
            logger.trace("[{}] Service Discovered [{}] CL=[{}]",
                         clientName, item.service.getClass().getName(),
                         item.service.getClass().getClassLoader().toString());
            if(association.addServiceItem(item)) {
                Association.State state = association.getState();
                if(state == Association.State.PENDING || Association.State.BROKEN == state)
                    association.setState(Association.State.DISCOVERED);
                increment();
                setFaultDetectionHandler(item.service, item.serviceID);
                notifyOnDiscovery(association, item.service);
            }
        }

        /**
         * @see org.rioproject.fdh.FaultDetectionListener#serviceFailure
         */
        @SuppressWarnings("unchecked")
        public void serviceFailure(final Object proxy, final ServiceID sid) {
            decrement();
            lCache.discard(proxy);
            ServiceItem item = association.removeService(proxy);
            if(item!=null) {
                notifyOnFailure(item);
                if(logger.isDebugEnabled())
                    logger.debug("[{}] Service FAILURE : {}", clientName, item.service.getClass().getName());
            } else {
                StringBuilder builder = new StringBuilder();
                builder.append("[").append(clientName).append("] ");
                builder.append("Unable to notify Listeners on failure, returned ServiceItem is null for association ");
                builder.append(association.getName());
                builder.append(". The Association reports ").append(association.getServiceCount()).append(" services");
                logger.warn(builder.toString());
            }
            fdhTable.remove(sid);
        }

        /**
         * Notify on service failure
         *
         * @param item The service that has failed
         */
        protected void notifyOnFailure(final ServiceItem item) {
            if(instances >0) {
                association.setState(Association.State.CHANGED);
                notifyOnChange(association, item.service);
            } else {
                association.setState(Association.State.BROKEN);
                notifyOnBroken(association, item.service);
            }
        }

        /**
         * Get the number of instances
         *
         * @return instance count
         */
        protected int instances() {
            return(instances);
        }

        /**
         * Increment instances
         */
        protected void increment() {
            instances++;
        }

        /**
         * Decrement instances
         */
        protected void decrement() {
            instances--;
        }

        /**
         * Get the Association AssociationHandler
         *
         * @return The Association instance for this AssociationHandler
         */
        public Association getAssociation() {
            return (association);
        }

        /**
         * Set the FaultDetectionHandler for this AssociationHandler
         *
         * @param service The Object used to communicate to the service
         * @param serviceID Unique service identifier
         *
         * @throws Exception if the fault detection handler cannot be attached
         */
        protected void setFaultDetectionHandler(final Object service, final ServiceID serviceID) throws Exception {
            FaultDetectionHandler<ServiceID> fdh = null;
            if(container!=null) {
                ServiceBeanDelegate delegate =
                    container.getServiceBeanDelegate(UuidFactory.create(serviceID.getMostSignificantBits(),
                                                                        serviceID.getLeastSignificantBits()));
                if(delegate!=null) {
                    logger.debug("Create FDH to monitor colocated service [{}]", association.getName());
                    fdh = new ColocatedListener();
                }
            }
            if(fdh==null) {
                logger.debug("Create FDH to monitor external service [{}]", association.getName());
                ClassLoader cl = service.getClass().getClassLoader();
                fdh = FaultDetectionHandlerFactory.getFaultDetectionHandler(aDesc, cl);
            }
            registerFaultDetectionHandler(service, serviceID, fdh);
        }

        /**
         * Register a FaultDetectionHandler and have it start monitoring
         *
         * @param service The service proxy
         * @param serviceID The service id
         * @param fdh The FaultDetectionHandler
         *
         * @throws Exception If error(s) occur
         */
        protected void registerFaultDetectionHandler(final Object service,
                                                     final ServiceID serviceID,
                                                     final FaultDetectionHandler<ServiceID> fdh) throws Exception {
            fdh.register(this);
            fdh.monitor(service, serviceID, lCache);
            fdhTable.put(serviceID, fdh);
        }
    }

    /*
     * Load interfaces from an AssociationDescriptor
     */
    private Class[] loadAssociatedInterfaces(final AssociationDescriptor a, final ClassLoader cl)
        throws ClassNotFoundException {
        String[] iNames = a.getInterfaceNames();
        Class[] interfaces = new Class[iNames.length];
        for(int i = 0; i < interfaces.length; i++) {
            interfaces[i] = Class.forName(iNames[i], false, cl);
        }
        return(interfaces);
    }

    /*
     * Handle colocated service injection
     */
    private void handleColocatedServiceInjection(final AssociationHandler aHandler) throws Exception {
        AssociationDescriptor aDesc = aHandler.getAssociationDescriptor();
        ServiceBeanInstance[] instances = container.getServiceBeanInstances(null);

        final Thread currentThread = Thread.currentThread();
        final ClassLoader cCL =
            AccessController.doPrivileged(
                new PrivilegedAction<ClassLoader>() {
                    public ClassLoader run() {
                        return (currentThread.getContextClassLoader());
                    }
                });
        try {
            AccessController.doPrivileged(new PrivilegedAction<Void>() {
                public Void run() {
                    currentThread.setContextClassLoader(callerCL);
                    return null;
                }
            });
            for (ServiceBeanInstance instance : instances) {
                Object service = instance.getService();
                //check name
                if (aHandler.aDesc.matchOnName()) {
                    boolean hailMaryCheck = false;
                    if (service instanceof Administrable) {
                        Administrable admin = (Administrable) service;
                        Object adminObject = admin.getAdmin();
                        if (adminObject instanceof JoinAdmin) {
                            JoinAdmin joinAdmin = (JoinAdmin) adminObject;
                            Entry[] attrs = joinAdmin.getLookupAttributes();
                            boolean matched = false;
                            for (Entry attr : attrs) {
                                if (attr instanceof Name) {
                                    if (aDesc.getName().equals(((Name) attr).name)) {
                                        matched = true;
                                        break;
                                    }
                                }
                            }
                            if (!matched) {
                                // name does not match, move on to
                                // next instance
                                continue;
                            }
                        } else {
                            hailMaryCheck = true;
                            logger.debug("AssociationManagement for [{}], associated service proxy " +
                                         "for colocated service does not implement {}, using declared service name",
                                         clientName, JoinAdmin.class.getName());
                        }
                    } else {
                        hailMaryCheck = true;
                        logger.debug("AssociationManagement for [{}], associated service proxy for" +
                                     "colocated service does not implement {}, using declared service name",
                                     clientName, Administrable.class.getName());
                    }

                    if (hailMaryCheck) {
                        if (!aDesc.getName().equals(
                            instance.getServiceBeanConfig().getName())) {
                            //name does not match, move on to next instance
                            continue;
                        }
                    }
                }
                Class[] interfaces = loadAssociatedInterfaces(aDesc, callerCL);
                if (isAssignableFrom(interfaces[0],service.getClass())) {
                    ServiceItem item = aHandler.makeServiceItem(instance);
                    aHandler.serviceDiscovered(item);
                }
            }
        } finally {
            AccessController.doPrivileged(new PrivilegedAction<Void>() {
                public Void run() {
                    currentThread.setContextClassLoader(cCL);
                    return (null);
                }
            });
        }
    }

    /**
     * This class handles the condition when the association is a Jini Lookup
     * Service
     */
    static class LookupServiceHandler implements DiscoveryListener {
        /** An instance of DiscoveryManagement */
        DiscoveryManagement dm;
        /**
         * An instance of the AssociationHandler to delegate association
         * actions to
         */
        private final AssociationHandler handler;
        /** Table of ServiceRegistrar instances to ServiceIDs */
        private final Map<ServiceRegistrar, ServiceID> registrarTable = new Hashtable<ServiceRegistrar, ServiceID>();
        /** A ProxyPreparer for discovered services */
        ProxyPreparer proxyPreparer;

        /**
         * Create a new LookupServiceHandler
         *
         * @param aDesc The associationDesc
         * @param handler The AssociationHandler to delegate association
         * actions to
         * @param config Configuration to use when discovering lookup services
         */
        LookupServiceHandler(final AssociationDescriptor aDesc,
                             final AssociationHandler handler,
                             final Configuration config) {
            this.handler = handler;
            try {
                DiscoveryManagementPool discoPool = DiscoveryManagementPool.getInstance();
                dm = discoPool.getDiscoveryManager(aDesc.getOperationalStringName(),
                                                   aDesc.getGroups(),
                                                   aDesc.getLocators());
                dm.addDiscoveryListener(this);
                proxyPreparer = (ProxyPreparer)config.getEntry(CONFIG_COMPONENT,
                                                               "proxyPreparer",
                                                               ProxyPreparer.class,
                                                               new BasicProxyPreparer());
                logger.debug("Association [{}] ProxyPreparer: {}",
                             aDesc.getName(), proxyPreparer.getClass().getName());
            } catch(Exception e) {
                logger.error("Creating LookupServiceHandler", e);
            }
        }

        /**
         * Stop listening for DiscoveryEvents
         */
        void terminate() {
            dm.removeDiscoveryListener(this);
        }

        /**
         * This method is invoked as ServiceRegistrar instances are discovered.
         * This method uses the configured ProxyPreparer to prepare the
         * discovered proxy. If no ProxyPreparer has been configured, the
         * default proxy preparer (BasicProxyPreparer that does nothing but
         * return the original proxy) is used
         *
         * @see net.jini.discovery.DiscoveryListener#discovered
         */
        public void discovered(DiscoveryEvent dEvent) {
            ServiceRegistrar[] registrars = dEvent.getRegistrars();
            for (ServiceRegistrar registrar : registrars) {
                try {
                    ServiceRegistrar sr = (ServiceRegistrar) proxyPreparer.prepareProxy(registrar);
                    registrarDiscovered(sr);
                } catch (RemoteException e) {
                    logger.error("Preparing ServiceRegistrar Proxy", e);
                }
            }
        }

        /**
         * Handle the discovery of a ServiceRegistrar
         *
         * @param registrar The ServiceRegistrar that has been discovered
         */
        private void registrarDiscovered(ServiceRegistrar registrar) {
            try {
                ServiceID serviceID = registrar.getServiceID();
                ServiceTemplate template = new ServiceTemplate(serviceID, null, null);
                ServiceMatches matches = registrar.lookup(template, 1);
                if(matches.items.length > 0) {
                    registrarTable.put(registrar, serviceID);
                    handler.serviceDiscovered(matches.items[0]);
                }
            } catch(Exception e) {
                logger.error("Adding Registrar", e);
            }
        }

        /**
         * @see net.jini.discovery.DiscoveryListener#discarded
         */
        public void discarded(DiscoveryEvent dEvent) {
            ServiceRegistrar[] registrars = dEvent.getRegistrars();
            for (ServiceRegistrar registrar : registrars) {
                ServiceID serviceID = registrarTable.remove(registrar);
                handler.serviceFailure(registrar, serviceID);
            }
        }
    }

    /**
     * Listen for colocated service removals
     */
    class ColocatedListener implements FaultDetectionHandler <ServiceID>, ServiceBeanContainerListener {
        private Uuid uuid;
        private ServiceID serviceID;
        private Object service;
        private FaultDetectionListener<ServiceID> listener;

        ColocatedListener() {
            container.addListener(this);
        }

        public void serviceInstantiated(ServiceRecord record) {
        }

        public void serviceDiscarded(ServiceRecord record) {
            if(uuid.equals(record.getServiceID()))
                listener.serviceFailure(service, serviceID);
        }

        public void setConfiguration(String[] configArgs) {
        }

        public void register(FaultDetectionListener<ServiceID> listener) {
            this.listener = listener;
        }

        public void unregister(FaultDetectionListener listener) {
        }

        public void monitor(Object service, ServiceID id, LookupCache lCache) throws Exception {
            serviceID = id;
            uuid = UuidFactory.create(serviceID.getMostSignificantBits(), serviceID.getLeastSignificantBits());
            this.service = service;
        }

        public void terminate() {
            container.removeListener(this);
        }
    }
}
