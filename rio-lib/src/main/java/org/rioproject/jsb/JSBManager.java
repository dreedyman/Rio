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
package org.rioproject.jsb;

import net.jini.admin.Administrable;
import net.jini.id.Uuid;
import net.jini.io.MarshalledInstance;
import org.rioproject.admin.ServiceBeanControl;
import org.rioproject.core.jsb.DiscardManager;
import org.rioproject.core.jsb.ServiceBeanManager;
import org.rioproject.core.jsb.ServiceBeanManagerException;
import org.rioproject.core.jsb.ServiceElementChangeListener;
import org.rioproject.deploy.ServiceBeanInstance;
import org.rioproject.deploy.ServiceProvisionListener;
import org.rioproject.opstring.OperationalStringException;
import org.rioproject.opstring.OperationalStringManager;
import org.rioproject.opstring.ServiceBeanConfig;
import org.rioproject.opstring.ServiceElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.NotificationBroadcasterSupport;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;

/**
 * Implement ServiceBeanManager support
 * 
 * @see org.rioproject.core.jsb.ServiceBeanManager
 *
 * @author Dennis Reedy
 */
@SuppressWarnings("PMD.AvoidThrowingRawExceptionTypes")
public class JSBManager implements ServiceBeanManager {
    private static final String COMPONENT="org.rioproject.jsb";
    private static Logger logger = LoggerFactory.getLogger(COMPONENT);
    private DiscardManager discardManager;
    private OperationalStringManager opStringManager;
    private ServiceElement sElem;
    /** ServiceID */
    private Uuid serviceID;
    /** Cybernode Uuid */
    private Uuid cybernodeUuid;
    /** The  marshalledInstance */
    private MarshalledInstance marshalledInstance;
    /** The host name the service bean was instantiated on */
    private String hostName;
    /** The host address the service bean was instantiated on */
    private String hostAddress;
    /** List of listeners */
    private final List<ServiceElementChangeListener> listenerList = new ArrayList<ServiceElementChangeListener>();
    /** Flag to indicate we are in the process of updating the 
     * ServiceElement. This will avoid unnecessary ServiceBeanConfig updates
     * if DiscoveryManagement attributes have changed */
    boolean updating = false;
    private final NotificationBroadcasterSupport notificationBroadcasterSupport = new NotificationBroadcasterSupport();

    /**
     * Create a JSBManager
     *
     * @param sElem The ServiceElement
     * @param hostName The host name the service bean was instantiated on
     * @param hostAddress The host address the service bean was instantiated on
     * @param cybernodeUuid The Uuuid of the Cybernode
     *
     * @throws IllegalArgumentException if the sElem hostName, or hostAddress parameters are null
     */
    public JSBManager(final ServiceElement sElem, final String hostName, final String hostAddress, final Uuid cybernodeUuid) {
       this(sElem, null, hostName, hostAddress, cybernodeUuid);
    }

    /**
     * Create a JSBManager
     *
     * @param sElem The ServiceElement
     * @param opStringManager The OperationalStringManager
     * @param hostName The host name the service bean was instantiated on
     * @param hostAddress The host address the service bean was instantiated on
     * @param cybernodeUuid The Uuid of the Cybernode
     *
     * @throws IllegalArgumentException if the sElem hostName, or hostAddress parameters are null
     */
    public JSBManager(final ServiceElement sElem,
                      final OperationalStringManager opStringManager,
                      final String hostName,
                      final String hostAddress,
                      final Uuid cybernodeUuid) {
        super();
        if(sElem==null)
            throw new IllegalArgumentException("sElem is null");
         if(hostName==null)
            throw new IllegalArgumentException("hostName is null");
        if(hostAddress==null)
            throw new IllegalArgumentException("hostAddress is null");
        if(cybernodeUuid==null)
            throw new IllegalArgumentException("cybernodeUuid is null");
        this.sElem = sElem;
        this.opStringManager = opStringManager;
        this.hostName = hostName;
        this.hostAddress = hostAddress;
        this.cybernodeUuid = cybernodeUuid;
    }

    /**
     * Set the ServiceElement for the ServiceBean
     * 
     * @param newElem The ServiceElement for the ServiceBean
     */
    public void setServiceElement(final ServiceElement newElem) {
        if(newElem==null)
            throw new IllegalArgumentException("sElem is null");                
        ServiceElement preElem = sElem;
        this.sElem = newElem;                        
        try {
            updating = true;
            stateChange(preElem, sElem);
        } finally {
            updating = false;
        }
    }    

    /**
     * Set the ServiceID for the ServiceBean
     * 
     * @param serviceID The Service Identifier for the ServiceBean
     */
    public void setServiceID(final Uuid serviceID) {
        if(serviceID==null)
            throw new IllegalArgumentException("serviceID is null");
        this.serviceID = serviceID;
    }

    /**
     * @see org.rioproject.core.jsb.ServiceBeanManager#getServiceID
     */
    public Uuid getServiceID() {
        return(serviceID);
    }

    /**
     * Set the Object that can be used to communicate to the ServiceBean
     * 
     * @param mi The MarshalledInstance containing the proxy that can be used
     * to communicate to the ServiceBean
     */
    public void setMarshalledInstance(final MarshalledInstance mi) {
        this.marshalledInstance = mi;
    }

    /**
     * Set the DiscardManager for the ServiceBean
     *
     * @param discardManager The DiscardManager for the ServiceBean
     */
    public void setDiscardManager(final DiscardManager discardManager) {
        this.discardManager = discardManager;
    }

    /**
     * @see org.rioproject.core.jsb.ServiceBeanManager#getDiscardManager
     */
    public DiscardManager getDiscardManager() {
        return(discardManager);
    }

    /**
     * Set the OperationalStringManager
     * 
     * @param opStringManager The OperationalStringManager
     */
    public void setOperationalStringManager(final OperationalStringManager opStringManager) {
        this.opStringManager = opStringManager;
    }

    /**
     * @see org.rioproject.core.jsb.ServiceBeanManager#getOperationalStringManager
     */
    public OperationalStringManager getOperationalStringManager() {
        return(opStringManager);
    }    

    /**
     * @see org.rioproject.core.jsb.ServiceBeanManager#update
     */
    public void update(final ServiceBeanConfig sbConfig) throws ServiceBeanManagerException {
        if(sbConfig==null)
            throw new IllegalArgumentException("ServiceBeanConfig is null");
        if(sElem==null) {
            logger.warn("No ServiceElement to update ServiceBeanConfig");
            return;
        }
        if(updating) {            
            if(logger.isTraceEnabled())
                logger.trace("Updating ServiceElement, ServiceBeanConfig update ignored");
            return;
        }
        ServiceElement preElem = ServiceElementUtil.copyServiceElement(sElem);
        Long instanceID = sElem.getServiceBeanConfig().getInstanceID();
        sElem.setServiceBeanConfig(sbConfig);
        if(instanceID!=null)
            sElem = ServiceElementUtil.prepareInstanceID(sElem, false, instanceID);
        else
            logger.warn("No instanceID for [{}] to update", sElem.getName());
        
        if(opStringManager==null) {
            logger.warn("No OperationalStringManager to update ServiceBeanConfig");
            return;
        }

        try {
            opStringManager.update(getServiceBeanInstance());
        } catch (OperationalStringException e) {
            throw new ServiceBeanManagerException("Unable to update ServiceBeanConfig", e);
        } catch (RemoteException e) {
            throw new ServiceBeanManagerException("Problem communicating to OperationalStringManager, " +
                                                  "unable to update ServiceBeanConfig", e);
        }
        stateChange(preElem, sElem);
    }
    
    /**
     * @see org.rioproject.core.jsb.ServiceBeanManager#increment
     *
     * @throws ServiceBeanManagerException if the increment fails for any reason
     */
    public void increment() throws ServiceBeanManagerException {
        increment(null);
    }

    /**
     * @see org.rioproject.core.jsb.ServiceBeanManager#increment
     */
    public void increment(final ServiceProvisionListener listener) throws ServiceBeanManagerException {
        if(opStringManager==null) {
            throw new ServiceBeanManagerException("No OperationalStringManager to increment service");
        }
        try {
            opStringManager.increment(sElem, false, listener);
        } catch (OperationalStringException e) {
            throw new ServiceBeanManagerException("Unable to increment", e);
        } catch (RemoteException e) {
            throw new ServiceBeanManagerException("Problem communicating to OperationalStringManager, unable to increment", e);
        }
    }

    /**
     * @see org.rioproject.core.jsb.ServiceBeanManager#decrement
     */
    public void decrement(final boolean destroy) throws ServiceBeanManagerException {
        if(opStringManager==null) {
            throw new ServiceBeanManagerException("No OperationalStringManager to decrement service");
        }
        try {
            opStringManager.decrement(getServiceBeanInstance(), false, destroy);
        } catch (OperationalStringException e) {
            e.printStackTrace();
        } catch (RemoteException e) {
            throw new ServiceBeanManagerException("Problem communicating to OperationalStringManager, unable to decrement", e);
        }
    }    

    /**
     * @see org.rioproject.core.jsb.ServiceBeanManager#relocate
     */
    public void relocate(final ServiceProvisionListener listener, final Uuid uuid) throws ServiceBeanManagerException {
        if(opStringManager==null) {
            throw new ServiceBeanManagerException("No OperationalStringManager to relocate service");
        }
        if(sElem.getProvisionType()!= ServiceElement.ProvisionType.DYNAMIC)
            throw new ServiceBeanManagerException("Relocation only available for DYNAMIC services");
        try {
            opStringManager.relocate(getServiceBeanInstance(), listener, uuid);
        } catch (OperationalStringException e) {
            throw new ServiceBeanManagerException("Unable to relocate ServiceBeanConfig", e);
        } catch (RemoteException e) {
            throw new ServiceBeanManagerException("Problem communicating to OperationalStringManager, unable to relocate", e);
        }
    }

    /**
     * @see org.rioproject.core.jsb.ServiceBeanManager#getServiceBeanInstance
     */
    public ServiceBeanInstance getServiceBeanInstance() {
        return(new ServiceBeanInstance(serviceID,
                                       marshalledInstance,
                                       sElem.getServiceBeanConfig(),
                                       hostName,
                                       hostAddress,
                                       cybernodeUuid));
    }

    /**
     * @see org.rioproject.core.jsb.ServiceBeanManager#getServiceBeanControl()
     */
    public ServiceBeanControl getServiceBeanControl() throws ServiceBeanManagerException {
        if(marshalledInstance==null)
            throw new ServiceBeanManagerException("Unable to obtain ServiceBeanControl, there is no marshalled proxy instance");
        ServiceBeanControl serviceBeanControl;
        try {
            Object proxy = marshalledInstance.get(false);
            if(proxy instanceof Administrable) {
                Object adminObject = ((Administrable)proxy).getAdmin();
                if(adminObject instanceof ServiceBeanControl) {
                    serviceBeanControl = (ServiceBeanControl)proxy;
                } else {
                    throw new ServiceBeanManagerException(String.format("Service is not an instanceof %s",
                                                          ServiceBeanControl.class.getName()));
                }
            } else {
                throw new ServiceBeanManagerException(String.format("%s is derivable from %s, however, the service proxy does not implement %s",
                                                                    ServiceBeanControl.class.getName(),
                                                                    Administrable.class.getName(),
                                                                    Administrable.class.getName()));
            }
        } catch (Exception e) {
            throw new ServiceBeanManagerException("Unable to obtain ServiceBeanControl", e);
        }
        return serviceBeanControl;
    }

    /**
     * Notify all registered ServiceElementChangeListener instances. 
     * 
     * @param preElem The old ServiceElement
     * @param postElem An updated ServiceElement
     */
    void stateChange(final ServiceElement preElem, final ServiceElement postElem) {
        notifyListeners(preElem, postElem);
    }

    /**
     * @see org.rioproject.core.jsb.ServiceBeanManager#addListener
     */
    public void addListener(final ServiceElementChangeListener l) {
        if(l == null) {
            throw new IllegalArgumentException("can't add null listener");
        }
        synchronized(listenerList) {
            listenerList.add(l);
        }        
    }

    /**
     * @see org.rioproject.core.jsb.ServiceBeanManager#removeListener
     */
    public void removeListener(ServiceElementChangeListener l) {
        if(l==null)
            return;
        synchronized(listenerList) {
            if(listenerList.contains(l))
                listenerList.remove(l);
        }                        
    }

    /**
     * @see org.rioproject.core.jsb.ServiceBeanManager#getNotificationBroadcasterSupport()
     */
    public NotificationBroadcasterSupport getNotificationBroadcasterSupport() {
        return notificationBroadcasterSupport;
    }


    /*
     * Notify all registered listeners of the ServiceElement change
     */
    private synchronized void notifyListeners(final ServiceElement preElem, final ServiceElement postElem) {
        ServiceElementChangeListener[] listeners;
        synchronized(listenerList) {
            listeners = listenerList.toArray(new ServiceElementChangeListener[listenerList.size()]);
        }
        for (ServiceElementChangeListener listener : listeners)
            listener.changed(preElem, postElem);
    }
}
