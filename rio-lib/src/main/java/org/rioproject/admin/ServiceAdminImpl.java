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
package org.rioproject.admin;

import net.jini.core.discovery.LookupLocator;
import net.jini.core.entry.Entry;
import net.jini.core.lookup.ServiceRegistrar;
import net.jini.discovery.DiscoveryGroupManagement;
import net.jini.discovery.DiscoveryLocatorManagement;
import net.jini.discovery.DiscoveryManagement;
import net.jini.export.Exporter;
import net.jini.id.Uuid;
import net.jini.id.UuidFactory;
import net.jini.lookup.JoinManager;
import org.rioproject.core.jsb.ServiceBeanContext;
import org.rioproject.cybernode.ServiceAdvertiser;
import org.rioproject.jsb.ServiceBeanAdapter;
import org.rioproject.opstring.ServiceBeanConfig;
import org.rioproject.opstring.ServiceElement;
import org.rioproject.resources.persistence.SnapshotHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.rmi.RemoteException;

/**
 * The ServiceAdminImpl class implements the ServiceAdmin interface providing 
 * administrative support for ServiceProducer implementations.
 *
 * @author Dennis Reedy
 */
public class ServiceAdminImpl implements ServiceAdmin {
    /**  The ServiceBean instance */
    private ServiceBeanAdapter service;
    /** ServiceBeanContext */
    private ServiceBeanContext context;
    /** The Exporter for the ServiceAdmin */
    protected final Exporter exporter;
    /** The ServiceAdminProxy */
    protected ServiceAdminProxy adminProxy;
    /** A snapshot handler */
    private SnapshotHandler snapshotHandler;
    /** The time the service was started */
    private final long started;
    /** A Logger */
    private static Logger logger = LoggerFactory.getLogger("org.rioproject.admin");

    /**
     * Create a ServiceAdmin Impl
     * 
     * @param service Concrete implementation of a ServiceBeanAdapter
     * @param exporter The Exporter to export this object
     */
    public ServiceAdminImpl(ServiceBeanAdapter service, Exporter exporter)  {
        this(service, exporter, null);
    }

    /**
     * Create a ServiceAdmin Impl
     * 
     * @param service Concrete implementation of a ServiceBeanAdapter
     * @param exporter The Exporter to export this object
     * @param snapshotHandler The service's snapshot handler used for persistence
     */
    public ServiceAdminImpl(ServiceBeanAdapter service, 
                            Exporter exporter, 
                            SnapshotHandler snapshotHandler)  {
        if(service==null)
            throw new IllegalArgumentException("service is null");
        if(service.getServiceBeanContext()==null)
            throw new IllegalArgumentException("context is null");
        this.service = service;
        this.exporter = exporter;
        this.snapshotHandler = snapshotHandler;
        this.started = service.getStartTime();
        this.context = service.getServiceBeanContext();
    }

    /**
     * @see org.rioproject.admin.ServiceAdmin#getJoinSet()
     */
    public ServiceRegistrar[] getJoinSet() {
        JoinManager mgr = service.getJoinManager();
        if(mgr!=null)
            return(mgr.getJoinSet());
        else
            logger.warn("JoinManager is null");
        return(new ServiceRegistrar[0]);
    }

    /**
     * Get the object to communicate with the ServiceAdminImpl
     *
     * @return The ServiceAdmin
     *
     * @throws RemoteException if an communication errors occur
     */
    public ServiceAdmin getServiceAdmin() throws RemoteException {
        if(adminProxy==null) {
            ServiceAdmin serviceAdminRemoteRef = (ServiceAdmin)exporter.export(this);
            adminProxy = ServiceAdminProxy.getInstance(serviceAdminRemoteRef, UuidFactory.generate());
        }
        return(adminProxy);
    }

    /**
     * Unexport the ServiceAdmin
     *
     * @param force - If true forcibly unexport
     */
    @SuppressWarnings("unused")
    public void unexport(boolean force) {
        if(exporter!=null) {
            try {
                exporter.unexport(true);
            } catch(IllegalStateException e) {
                logger.warn("ServiceAdminImpl not exported");
            }
        }
    }


    /*-------------------
     * DestroyAdmin
     *------------------*/

    /**
     * This method terminates the service
     */
    public void destroy() {
        if(service!=null) {
            service.destroy();
            service = null;
        }
    }

    /*-------------------
     * ServiceBeanAdmin
     *------------------*/
    
    /** @see org.rioproject.admin.ServiceBeanAdmin#getServiceElement */
    public ServiceElement getServiceElement() {
        return(service.getServiceBeanContext().getServiceElement());
    }
    
    /** @see org.rioproject.admin.ServiceBeanAdmin#setServiceElement */
    public void setServiceElement(ServiceElement sElem) {
        try {
            ServiceBeanContext sbc = service.getServiceBeanContext();
            if(sbc instanceof org.rioproject.jsb.JSBContext)
                ((org.rioproject.jsb.JSBContext)sbc).setServiceElement(sElem);
            else
                logger.warn("ServiceBeanContext {} not an instance of JSBContext. Unable to set ServiceElement",
                            sbc.toString());
        } catch (Throwable t) {
            logger.warn("Setting ServiceElement", t);
        }
    }   
    
    /** @see org.rioproject.admin.ServiceBeanAdmin#getUpTime */
    public long getUpTime() {
        return(System.currentTimeMillis()-started);
    }

    /** @see org.rioproject.admin.ServiceBeanAdmin#getServiceBeanInstantiatorUuid */
    public Uuid getServiceBeanInstantiatorUuid() {
        return(service.getServiceBeanInstantiatorUuid());
    }
    

    /*-------------------
     * ServiceBeanControl
     *------------------*/

    /**
     * @see org.rioproject.admin.ServiceBeanControl#start
     */
    public Object start() throws ServiceBeanControlException {
        Object proxy;
        try {
            proxy = service.start(context);
        } catch(Throwable t) {
            throw new ServiceBeanControlException("start failed", t);
        }
        return(proxy);
    }

    /**
     * @see org.rioproject.admin.ServiceBeanControl#stop
     */
    public void stop(boolean force) throws ServiceBeanControlException {
        try {
            service.stop(force);
        } catch(Throwable t) {
            throw new ServiceBeanControlException("stop failed", t);
        }
    }

    /**
     * @see org.rioproject.admin.ServiceBeanControl#advertise
     */
    public void advertise() throws ServiceBeanControlException {
        try {
            if(context!=null) {
                Entry[] configuredAttrs = ServiceAdvertiser.getConfiguredAttributes(context);
                if(configuredAttrs.length>0)
                    service.addAttributes(configuredAttrs);
            } else {
                logger.warn("ServiceBeanContext is null");
            }
            service.advertise();
        } catch(Throwable t) {
            throw new ServiceBeanControlException("advertise failed", t);
        }
    }

    /**
     * @see org.rioproject.admin.ServiceBeanControl#unadvertise
     */
    public void unadvertise() throws ServiceBeanControlException {
        try {
            service.unadvertise();
        } catch(Throwable t) {
            throw new ServiceBeanControlException("unadvertise failed", t);
        }
    }

    /**
     * Set the ServiceBeanContext
     * 
     * @param context The ServiceBeanContext
     */
    public void setServiceBeanContext(ServiceBeanContext context) {
        if(context==null)
            throw new IllegalArgumentException("context is null");        
        this.context = context;
    }

    /*--------------
     * JoinAdmin 
     *-------------*/
    public Entry[] getLookupAttributes() {
        JoinManager mgr = service.getJoinManager();
        if(mgr!=null)
            return(mgr.getAttributes());
        else {
            logger.debug("JoinManager is null");
        }
        return(new Entry[0]);
    }

    public void addLookupAttributes(Entry[] attrs) {
        //service.addAttributes(attrs);
        JoinManager mgr = service.getJoinManager();
        if(mgr!=null) {
            if(attrs != null && attrs.length != 0)
                mgr.addAttributes(attrs, true);
            if(snapshotHandler!=null) {
                try {
                    snapshotHandler.takeSnapshot();
                } catch(IOException ioe) {
                    logger.warn("Persisting Added Lookup Attributes", ioe);
                }
            }
        } else {
            logger.warn("JoinManager is null");
        }
    }

    public void modifyLookupAttributes(Entry[] attrSetTemplates, Entry[] attrSets) {        
        JoinManager mgr = service.getJoinManager();
        if(mgr!=null) {
            mgr.modifyAttributes(attrSetTemplates, attrSets, true);
            if(snapshotHandler!=null) {
                try {
                    snapshotHandler.takeSnapshot();
                } catch(IOException ioe) {
                    logger.warn("Persisting Modified Lookup Attributes", ioe);
                }
            }
        } else {
            logger.warn("JoinManager is null");
        }
    }

    public String[] getLookupGroups() {
        DiscoveryManagement dm;
        JoinManager mgr = service.getJoinManager();
        if(mgr!=null) {
            dm = mgr.getDiscoveryManager();
        } else {
            try {
                dm = service.getServiceBeanContext().getDiscoveryManagement();
            } catch (IOException e) {
                logger.warn(
                           "Getting DiscoveryManagement ", 
                           e);
                return(new String[0]);
            }
        }                    
        DiscoveryGroupManagement dgm = (DiscoveryGroupManagement)dm;
        return(dgm.getGroups());        
    }

    public void addLookupGroups(String[] groups) {
        JoinManager mgr = service.getJoinManager();
        if(mgr!=null) {
            try {
                DiscoveryManagement dm = mgr.getDiscoveryManager();
                DiscoveryGroupManagement dgm = (DiscoveryGroupManagement)dm;
                dgm.addGroups(groups);                
                if(snapshotHandler!=null) {
                    try {
                        snapshotHandler.takeSnapshot();
                    } catch(IOException ioe) {
                        logger.warn("Persisting Added Lookup groups", ioe);
                    }
                }
                /* Update ServiceBeanConfig */
                setGroups(dgm.getGroups());
            } catch(IOException ioe) {
                logger.warn("Adding Lookup Groups", ioe);                
            }
        } else
            logger.warn("JoinManager is null");
    }

    public void removeLookupGroups(String[] groups) {
        JoinManager mgr = service.getJoinManager();
        if(mgr!=null) {
            try {
                DiscoveryManagement dm = mgr.getDiscoveryManager();
                DiscoveryGroupManagement dgm = (DiscoveryGroupManagement)dm;
                dgm.removeGroups(groups);
                if(snapshotHandler!=null) {
                    try {
                        snapshotHandler.takeSnapshot();
                    } catch(IOException ioe) {
                        logger.warn("Persisting removed Lookup groups", ioe);
                    }
                }
                /* Update ServiceBeanConfig */
                setGroups(dgm.getGroups());
            } catch(Exception e) {
                logger.warn("Removing Lookup groups", e);
            }
        } 
    }

    public void setLookupGroups(String[] groups) {
        JoinManager mgr = service.getJoinManager();
        if(mgr!=null) {
            try {
                DiscoveryManagement dm = mgr.getDiscoveryManager();
                DiscoveryGroupManagement dgm = (DiscoveryGroupManagement)dm;
                dgm.setGroups(groups);
                if(snapshotHandler!=null) {
                    try {
                        snapshotHandler.takeSnapshot();
                    } catch(IOException ioe) {
                        logger.warn("Persisting Lookup groups", ioe);
                    }
                }
                /* Update ServiceBeanConfig */
                setGroups(dgm.getGroups());
            } catch(IOException ioe) {
                logger.warn("Setting Lookup groups", ioe);                
            }
        } 
    }

    public LookupLocator[] getLookupLocators() {
        DiscoveryManagement dm;
        JoinManager mgr = service.getJoinManager();
        if(mgr!=null) {
            dm = mgr.getDiscoveryManager();
        } else {
            try {
                dm = service.getServiceBeanContext().getDiscoveryManagement();
            } catch(IOException e) {
                logger.warn( "Getting DiscoveryManagement", e);
                return(new LookupLocator[0]);
            }
        }
        DiscoveryLocatorManagement dlm = (DiscoveryLocatorManagement)dm;
        return(dlm.getLocators());        
    }

    public void addLookupLocators(LookupLocator[] locators) {
        JoinManager mgr = service.getJoinManager();
        if(mgr!=null) {
            DiscoveryManagement dm = mgr.getDiscoveryManager();
            DiscoveryLocatorManagement dlm = (DiscoveryLocatorManagement)dm;
            dlm.addLocators(locators);
            if(snapshotHandler!=null) {
                try {
                    snapshotHandler.takeSnapshot();
                } catch(IOException ioe) {
                    logger.warn( "Persisting Added LookupLocators", ioe);
                }
            }
            /* Update ServiceBeanConfig */
            setLocators(dlm.getLocators());
        } else
            logger.warn("JoinManager is null");
    }

    public void removeLookupLocators(LookupLocator[] locators) {
        JoinManager mgr = service.getJoinManager();
        if(mgr!=null) {
            DiscoveryManagement dm = mgr.getDiscoveryManager();
            DiscoveryLocatorManagement dlm = (DiscoveryLocatorManagement)dm;
            dlm.removeLocators(locators);
            if(snapshotHandler!=null) {
                try {
                    snapshotHandler.takeSnapshot();
                } catch(IOException ioe) {
                    logger.warn("Persisting removed LookupLocators", ioe);
                }
            }
            /* Update ServiceBeanConfig */
            setLocators(dlm.getLocators());
        } else
            logger.warn("JoinManager is null");
    }

    public void setLookupLocators(LookupLocator[] locators) {
        JoinManager mgr = service.getJoinManager();
        if(mgr!=null) {
            DiscoveryManagement dm = mgr.getDiscoveryManager();
            DiscoveryLocatorManagement dlm = (DiscoveryLocatorManagement)dm;
            dlm.setLocators(locators);
            if(snapshotHandler!=null) {
                try {
                    snapshotHandler.takeSnapshot();
                } catch(IOException ioe) {
                    logger.warn("Persisting LookupLocators", ioe);
                }
            }
            /* Update ServiceBeanConfig */
            setLocators(dlm.getLocators());
        } else
            logger.warn("JoinManager is null");
    }  
    
    /**
     * Set new groups into the ServiceBeanConfig and update the ServiceBeanConfig 
     * using the ServiceBeanManager
     * 
     * @param groups Array of groups names to set
     */
    private void setGroups(String[] groups) {
        ServiceBeanConfig sbConfig = service.getServiceBeanContext().getServiceBeanConfig();
        sbConfig.setGroups(groups);
        try {
            service.getServiceBeanContext().getServiceBeanManager().update(sbConfig);
        } catch(Exception e) {
            logger.warn("Setting groups", e);
        }
    }
    
    /**
     * Set new LookupLocators into the ServiceBeanConfig and update the 
     * ServiceBeanConfig using the ServiceBeanManager
     * 
     * @param locators Array of LookupLocator names to set
     */
    private void setLocators(LookupLocator[] locators) {
        ServiceBeanConfig sbConfig =  service.getServiceBeanContext().getServiceBeanConfig();
        sbConfig.setLocators(locators);
        try {
            service.getServiceBeanContext().getServiceBeanManager().update(sbConfig);
        } catch(Exception e) {
            logger.warn("Setting LookupLocators", e);
        }
    }
}
