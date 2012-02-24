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

import net.jini.id.Uuid;
import net.jini.io.MarshalledInstance;
import org.rioproject.core.jsb.DiscardManager;
import org.rioproject.core.jsb.ServiceBeanManager;
import org.rioproject.core.jsb.ServiceElementChangeListener;
import org.rioproject.deploy.ServiceBeanInstance;
import org.rioproject.deploy.ServiceProvisionListener;
import org.rioproject.opstring.OperationalStringManager;
import org.rioproject.opstring.ServiceBeanConfig;
import org.rioproject.opstring.ServiceElement;

import javax.management.NotificationBroadcasterSupport;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Implement ServiceBeanManager support
 * 
 * @see org.rioproject.core.jsb.ServiceBeanManager
 *
 * @author Dennis Reedy
 */
public class JSBManager implements ServiceBeanManager {
    private static final String COMPONENT="org.rioproject.jsb";
    private static Logger logger = Logger.getLogger(COMPONENT);
    private DiscardManager discardManager;
    private OperationalStringManager opStringManager;
    private ServiceElement sElem;
    /** ServiceID */
    private Uuid serviceID;
    /** Cybernode Uuid */
    private Uuid cybernodeUuid;
    /** The  marshalledInstance */
    private MarshalledInstance marshalledInstance;
    /** The host address the service bean was instantiated on */
    private String hostAddress;
    /** List of listeners */
    private final List<ServiceElementChangeListener> listenerList =
        new ArrayList<ServiceElementChangeListener>();
    /** Flag to indicate we are in the process of updating the 
     * ServiceElement. This will avoid unecessary ServiceBeanConfig updates
     * if DiscoveryManagement attributes have changed */
    boolean updating = false;
    protected final NotificationBroadcasterSupport
        notificationBroadcasterSupport = new NotificationBroadcasterSupport();

    /**
     * Create a JSBManager
     *
     * @param sElem The ServiceElement
     * @param hostAddress The host address the service bean was instantiated on
     * @param cybernodeUuid The Uuuid of the Cybernode
     *
     * @throws IllegalArgumentException if the sElem or hostAddress parameters
     * are null
     */
    public JSBManager(ServiceElement sElem,
                      String hostAddress,
                      Uuid cybernodeUuid) {
       this(sElem, null, hostAddress, cybernodeUuid);
    }

    /**
     * Create a JSBManager
     *
     * @param sElem The ServiceElement
     * @param opStringManager The OperationalStringManager
     * @param hostAddress The host address the service bean was instantiated on
     * @param cybernodeUuid The Uuuid of the Cybernode
     *
     * @throws IllegalArgumentException if the sElem or hostAddress parameters
     * are null
     */
    public JSBManager(ServiceElement sElem,
                      OperationalStringManager opStringManager,
                      String hostAddress,
                      Uuid cybernodeUuid) {
        super();
        if(sElem==null)
            throw new IllegalArgumentException("sElem is null");
         if(hostAddress==null)
            throw new IllegalArgumentException("hostAddress is null");
        if(cybernodeUuid==null)
            throw new IllegalArgumentException("cybernodeUuid is null");
        this.sElem = sElem;
        this.opStringManager = opStringManager;
        this.hostAddress = hostAddress;
        this.cybernodeUuid = cybernodeUuid;
    }

    /**
     * Set the ServiceElement for the ServiceBean
     * 
     * @param newElem The ServiceElement for the ServiceBean
     */
    public void setServiceElement(ServiceElement newElem) {
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
    public void setServiceID(Uuid serviceID) {
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
    public void setMarshalledInstance(MarshalledInstance mi) {        
        this.marshalledInstance = mi;
    }

    /**
     * Set the DiscardManager for the ServiceBean
     *
     * @param discardManager The DiscardManager for the ServiceBean
     */
    public void setDiscardManager(DiscardManager discardManager) {
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
    public void setOperationalStringManager(
        OperationalStringManager opStringManager) {
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
    public void update(ServiceBeanConfig sbConfig) throws Exception {
        if(sbConfig==null)
            throw new IllegalArgumentException("ServiceBeanConfig is null");
        if(sElem==null) {
            logger.log(Level.WARNING,
                       "No ServiceElement to update ServiceBeanConfig");
            return;
        }
        if(updating) {            
            if(logger.isLoggable(Level.FINER))
                logger.log(Level.FINER,
                           "Updating ServiceElement, ServiceBeanConfig "+
                           "update ignored");
            return;
        }
        ServiceElement preElem = ServiceElementUtil.copyServiceElement(sElem);
        Long instanceID = sElem.getServiceBeanConfig().getInstanceID();
        sElem.setServiceBeanConfig(sbConfig);
        if(instanceID!=null)
            sElem = ServiceElementUtil.prepareInstanceID(sElem, 
                                                         false,
                                                         instanceID);
        else
            logger.log(Level.WARNING, "No instanceID for ["+sElem.getName()+"] "+
                                      "to update");        
        
        if(opStringManager==null) {
            if(logger.isLoggable(Level.WARNING))
                logger.log(Level.WARNING,
                           "No OperationalStringManager to update ServiceBeanConfig");
            return;
        }
        
        opStringManager.update(getServiceBeanInstance());
        stateChange(preElem, sElem);
    }
    
    /**
     * @see org.rioproject.core.jsb.ServiceBeanManager#increment
     *
     * @throws Exception if the increment fails for any reason
     */
    public void increment() throws Exception {
        increment(null);
    }

    /**
     * @see org.rioproject.core.jsb.ServiceBeanManager#increment
     */
    public void increment(ServiceProvisionListener listener) throws Exception {
        if(opStringManager==null) {
            if(logger.isLoggable(Level.WARNING))
                logger.log(Level.WARNING,
                           "No OperationalStringManager to increment service");
            return;
        }
        opStringManager.increment(sElem, false, listener);
    }

    /**
     * @see org.rioproject.core.jsb.ServiceBeanManager#decrement
     */
    public void decrement(boolean destroy) throws Exception {
        if(opStringManager==null) {
            if(logger.isLoggable(Level.WARNING))
                logger.log(Level.WARNING,
                           "No OperationalStringManager to decrement service");
            return;
        }
        opStringManager.decrement(getServiceBeanInstance(), false, destroy);
    }    

    /**
     * @see org.rioproject.core.jsb.ServiceBeanManager#relocate
     */
    public void relocate(ServiceProvisionListener listener, 
                         Uuid uuid) throws Exception {
        if(opStringManager==null) {
            if(logger.isLoggable(Level.WARNING))
                logger.log(Level.WARNING,
                           "No OperationalStringManager to relocate service");
            return;
        }
        if(sElem.getProvisionType()!= ServiceElement.ProvisionType.DYNAMIC)
            throw new Exception("Relocation only available for DYNAMIC services");
        opStringManager.relocate(getServiceBeanInstance(), listener, uuid);
    }

    /**
     * @see org.rioproject.core.jsb.ServiceBeanManager#getServiceBeanInstance
     */
    public ServiceBeanInstance getServiceBeanInstance() {
        return(new ServiceBeanInstance(serviceID,
                                       marshalledInstance,
                                       sElem.getServiceBeanConfig(),
                                       hostAddress,
                                       cybernodeUuid));
    }

    /**
     * Notify all registered ServiceElementChangeListener instances. 
     * 
     * @param preElem The old ServiceElement
     * @param postElem An updated ServiceElement
     */
    void stateChange(ServiceElement preElem, ServiceElement postElem) {
        notifyListeners(preElem, postElem);
    }

    /**
     * @see org.rioproject.core.jsb.ServiceBeanManager#addListener
     */
    public void addListener(ServiceElementChangeListener l) {
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
    private synchronized void notifyListeners(ServiceElement preElem, 
                                              ServiceElement postElem) {
        ServiceElementChangeListener[] listeners;
        synchronized(listenerList) {
            listeners = listenerList.toArray(
                          new ServiceElementChangeListener[listenerList.size()]);
        }
        for (ServiceElementChangeListener listener : listeners)
            listener.changed(preElem, postElem);
    }
}
