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
package org.rioproject.monitor;

import net.jini.id.Uuid;
import org.rioproject.associations.AssociationDescriptor;
import org.rioproject.associations.AssociationType;
import org.rioproject.core.ServiceBeanInstance;
import org.rioproject.core.ServiceElement;
import org.rioproject.core.provision.*;
import org.rioproject.core.provision.SystemRequirements.SystemComponent;
import org.rioproject.jsb.ServiceElementUtil;
import org.rioproject.sla.ServiceLevelAgreements;
import org.rioproject.system.MeasuredResource;
import org.rioproject.system.ResourceCapability;
import org.rioproject.system.capability.PlatformCapability;
import org.rioproject.system.capability.platform.StorageCapability;
import org.rioproject.watch.ThresholdValues;

import java.io.IOException;
import java.rmi.MarshalledObject;
import java.rmi.RemoteException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * An InstantiatorResource is the object being leased and controlled by the
 * ServiceResource, and represents an available ServiceInstantiation
 * that can be used to provision a service. The InstantiatorResource
 * provides a concurrency lock through a <code>boolean</code> member variable
 * that indicates that the ServiceBeanInstantiator is available for the
 * provisioning of ServiceBean objects which have a provisioning type of
 * <i>dynamic</i>. Until this flag is set to <code>true</code>, the
 * ServiceBeanInstantiator will only be used for the provisioning of ServiceBean
 * objects which have a provisioning type of <i>fixed</i>. The
 * InstantiatorResource also maintains a table of ServiceElement objects and how
 * many of each ServiceElement has been provisioned
 * 
 * @see org.rioproject.core.jsb.ServiceBean
 * @see org.rioproject.core.provision.ServiceBeanInstantiator
 *
 * @author Dennis Reedy
 */
public class InstantiatorResource {
    /**
     * The ServiceBeanInstantiator
     */
    private ServiceBeanInstantiator instantiator;
    /**
     * The maximum number of services the ServiceBeanInstantiator can instantiate 
     */
    private int serviceLimit;
    /**
     * An in-process counter indicating the InstantiatorResource is being used to
     * provision a service 
     */
    private AtomicInteger inProcessCounter = new AtomicInteger();
    /**
     * The handback option provided by the ServiceBeanInstantiator and sent back
     * to the ServiceBeanInstantiator as part of the ProvisionEvent
     */
    private MarshalledObject handback;
    /**
     * A Copy of the ResourceCapability object from the ServiceBeanInstantiator
     */
    private ResourceCapability resourceCapability;
    private final Object resourceCapabilityLock = new Object(); 
    /**
     * Whether the instantiator is ready to accept requests for the
     * instantiation of dynamic services
     */
    private boolean dynamicEnabled = false;
    /**
     * Table of ServiceElement instances and how many the InstantiatorResource
     * has instantiated
     */
    private final Map<ServiceElement, List<DeployedService>> serviceElementMap =
        new HashMap<ServiceElement, List<DeployedService>>();
    /** Table of in process ServiceElement instances */
    private final Map<ServiceElement, Integer> inProcessMap =
        new HashMap<ServiceElement, Integer>();
    /**
     * Name of the ServiceBeanInstantiator
     */
    private String instantiatorName;
    /**
     * The Uuid that has been assigned to the ServiceBeanInstantiator
     */
    private Uuid instantiatorUuid;
    /** The Logger */
    static final Logger logger =
        Logger.getLogger(ProvisionMonitorImpl.LOGGER+".provision");

    /**
     * Create an InstantiatorResource
     * 
     * @param instantiator A ServiceBeanInstantiator
     * @param instantiatorName Name for the ServiceBeanInstantiator
     * @param instantiatorUuid The Uuid that has been assigned to the
     * ServiceBeanInstantiator, may be null
     * @param handback The handback object the ServiceBeanInstantiator has 
     * provided, may be null
     * @param resourceCapability The ResourceCapability object for the
     * ServiceBeanInstantiator 
     * @param serviceLimit The total number of services the ServiceBeanInstantiator
     * will allocate
     */
    public InstantiatorResource(ServiceBeanInstantiator instantiator,
                                String instantiatorName,
                                Uuid instantiatorUuid,
                                MarshalledObject handback,
                                ResourceCapability resourceCapability,
                                int serviceLimit) {
        this.instantiator = instantiator;
        this.instantiatorName = instantiatorName;
        this.instantiatorUuid = instantiatorUuid;
        this.handback = handback;
        this.resourceCapability = resourceCapability;
        this.serviceLimit = serviceLimit;
    }

    /**
     * Add a DeployedService instance to the serviceElementMap.
     * 
     * @param newDeployedService The service to add
     */
    void addDeployedService(DeployedService newDeployedService) {
        ServiceElement sElem = newDeployedService.getServiceElement();
        synchronized(serviceElementMap) {
            if(serviceElementMap.containsKey(sElem)) {
                List<DeployedService> list = serviceElementMap.get(sElem);
                list.add(newDeployedService);
                serviceElementMap.put(sElem, list);
            } else {
                List<DeployedService> list = new ArrayList<DeployedService>();
                list.add(newDeployedService);
                serviceElementMap.put(sElem, list);
            }
        }
    }

    /**
     * Set the DeployedService instances
     *
     * @param deployedServices List of active & deployed services
     */
    void setDeployedServices(List<DeployedService> deployedServices) {
        synchronized(serviceElementMap) {
            serviceElementMap.clear();
            for(DeployedService deployedService : deployedServices) {

                ServiceElement sElem = deployedService.getServiceElement();
                if (serviceElementMap.containsKey(sElem)) {
                    List<DeployedService> list = serviceElementMap.get(sElem);
                    list.add(deployedService);
                    serviceElementMap.put(sElem, list);
                } else {
                    List<DeployedService> list = new ArrayList<DeployedService>();
                    list.add(deployedService);
                    serviceElementMap.put(sElem, list);
                }
            }
        }
    }

    /**
     * Get the name of the ServiceBeanInstantiator
     *
     * @return The name of the ServiceBeanInstantiator
     */
    String getName() {
        return(instantiatorName);
    }

    /**
     * Get the Uuid that has been assigned to the ServiceBeanInstantiator
     *
     * @return The Uuid for the ServiceBeanInstantiator
     */
    Uuid getInstantiatorUuid() {
        return(instantiatorUuid);
    }

    /**
     * Get the ServiceBeanInstantiator
     *
     * @return The ServiceBeanInstantiator
     */
    ServiceBeanInstantiator getServiceBeanInstantiator() {
        return instantiator;
    }

    /**
     * Get the active ServiceRecord instances for this InstantiatorResource
     *
     * @return Array of active ServiceRecord instances for this
     * InstantiatorResource
     *
     * @throws RemoteException If the active ServiceReecords cannot be obtained
     */
    ServiceRecord[] getActiveServiceRecords() throws RemoteException {
        ServiceRecord[] records = null;
        /*
         * Addresses an observed anomaly where for some reason we could not 
         * communicate back to the Cybernode, the connection was reset. The
         * strategy here is to retry 3 times, waiting 1 second between retries
         * to attempt to get the active ServiceRecord instances
         */
        int RETRY = 3;
        RemoteException toThrow = null;
        for(int i=0; i< RETRY; i++) {
            try {
                records =
                    getInstantiator().getServiceRecords(
                                          ServiceRecord.ACTIVE_SERVICE_RECORD);
                break;
            } catch(RemoteException e) {
                logger.warning("Exception ["+e.getClass().getName()+"] "+
                               "occurred, retry ["+i+"] ....");
                toThrow = e;
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ignore) {
                    if(logger.isLoggable(Level.FINEST)) {
                        logger.finest("Timeout Interrupted, handled");
                    }
                }
            }
        }
        if(toThrow!=null)
            throw toThrow;

        return(records);
    }

    /**
     * Get the ServiceRecord instances for a ServiceElement on this
     * InstantiatorResource
     *
     * @param elem The ServiceElement
     *
     * @return Array of ServiceRecord instances for a ServiceElement on this
     * InstantiatorResource. If there are no ServiceRecords, return a
     * zero-length array
     *
     * @throws RemoteException If the ServiceReecords cannot be obtained
     */
    ServiceRecord[] getServiceRecords(ServiceElement elem) throws RemoteException {
        ServiceStatement statement = null;
        /*
         * Addresses an observed anomaly where for some reason we could not
         * communicate back to the Cybernode, the connection was reset. The
         * strategy here is to retry 3 times, waiting 1 second between retries
         * to attemp to get the active ServiceRecord instances
         */
        int RETRY = 3;
        RemoteException toThrow = null;
        for(int i=0; i< RETRY; i++) {
            try {
                statement = getInstantiator().getServiceStatement(elem);
                break;
            } catch(RemoteException e) {
                logger.warning("Exception ["+e.getClass().getName()+"] "+
                               "occurred, retry ["+i+"] ....");
                toThrow = e;
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ignore) {
                    if(logger.isLoggable(Level.FINEST)) {
                        logger.finest("Timeout Interrupted, handled");
                    }
                }
            }
        }
        if(toThrow!=null)
            throw toThrow;

        return(statement==null?
               new ServiceRecord[0] : statement.getServiceRecords());
    }

    /**
     * Determine if the instance is found on this InstantiatorResource
     *
     * @param sElem The ServiceElement instance
     * @param uuid The id of the instance
     *
     * @return true if the instance is found on this InstantiatorResource
     */
    boolean hasServiceElementInstance(ServiceElement sElem, Uuid uuid) {
        boolean found = false;
        synchronized(serviceElementMap) {
            if(serviceElementMap.containsKey(sElem)) {
                List<DeployedService> list = serviceElementMap.get(sElem);
                DeployedService[] ids =
                    list.toArray(new DeployedService[list.size()]);
                for (DeployedService deployedService : ids) {
                    if (deployedService.getServiceBeanInstance().getServiceBeanID().equals(uuid)) {
                        found = true;
                        break;
                    }
                }
            }
        }
        return(found);
    }

    /**
     * Remove (decrement) a ServiceElement instance in the serviceElementMap.
     * If the ServiceElement exists in the table decrease it's instance counter
     * by one. If the ServiceElement instances counter is decremented to zero,
     * remove the ServiceElement from the serviceElementMap
     * 
     * @param sElem The ServiceElement instance to decrease
     * @param uuid The id of the instance to remove
     *
     * @return The removed ServiceBeanInstance, or null if the instance was removed
     */
    ServiceBeanInstance removeServiceElementInstance(ServiceElement sElem, Uuid uuid) {
        ServiceBeanInstance removedInstance = null;
        synchronized(serviceElementMap) {
            if(serviceElementMap.containsKey(sElem)) {
                List<DeployedService> list = serviceElementMap.get(sElem);
                DeployedService[] ids =
                    list.toArray(new DeployedService[list.size()]);
                for (DeployedService deployedService : ids) {
                    if (deployedService.getServiceBeanInstance().getServiceBeanID().equals(uuid)) {
                        list.remove(deployedService);
                        removedInstance = deployedService.getServiceBeanInstance();
                        break;
                    }
                }
                if(list.size()>0) {
                    serviceElementMap.put(sElem, list);
                } else {
                    serviceElementMap.remove(sElem);
                }
            }
        }

        return removedInstance;
    }

    ServiceElement[] getServiceElements() {
        ServiceElement[] elems;
        synchronized(serviceElementMap) {
            elems = new ServiceElement[serviceElementMap.size()];
            int i=0;
            for (Map.Entry<ServiceElement, List<DeployedService>> entry :
                serviceElementMap.entrySet()) {
                elems[i++] = entry.getKey();
            }
        }
        return(elems);
    }

    /**
     * Clear the Table of ServiceElement instances
     */
    void clearServiceElementInstances() {
        synchronized(serviceElementMap) {
            serviceElementMap.clear();
        }
    }

    /**
     * Get the number of ServiceElement instances
     *
     * @param sElem The ServiceElement to count
     * 
     * @return The number of instances of the ServiceElement the
     * ServiceBeanInstantiator has instantiated. If not found return 0
     */
    int getServiceElementCount(ServiceElement sElem) {
        int numInstances = 0;
        synchronized(serviceElementMap) {
            if(serviceElementMap.containsKey(sElem)) {
                List list = serviceElementMap.get(sElem);
                numInstances = list.size();
            }
            if(logger.isLoggable(Level.FINEST))
                logger.finest("Get service element count for " +
                              "["+sElem.getOperationalStringName()+"/"+sElem.getName()+"], " +
                              getName()+" at ["+getHostAddress()+"], "+
                              "ServiceElementMap: "+serviceElementMap);
        }
        return (numInstances);
    }

    /**
     * Get the total number of all ServiceElement instances
     * 
     * @return The total number of ServiceElement instances the 
     * ServiceBeanInstantiator has instantiated
     */
    int getServiceElementCount() {
        int totalInstances = 0;
        synchronized(serviceElementMap) {
            Set<ServiceElement> keys = serviceElementMap.keySet();
            for (ServiceElement key : keys) {
                List<DeployedService> list = serviceElementMap.get(key);
                totalInstances += list.size();
            }
        }
        return (totalInstances);
    }

    /**
     * Get the ServiceBeanInstantiator
     * 
     * @return The Instantiator
     */
    ServiceBeanInstantiator getInstantiator() {
        return (instantiator);
    }

    /**
     * Get the number of active and in-process services
     *
     * @return The number of active and in-process services.
     */
    int getServiceCount() {
        int count = getServiceElementCount();
        int planned = getInProcessCounter();
        return count + planned;
    }


    /**
     * Get the handback object
     * 
     * @return The handback object
     */
    public MarshalledObject getHandback() {
        return (handback);
    }

    /**
     * Get the ServiceDeployment for a ServiceBeanInstance
     *
     * @param sElem The ServiceElement, used as the key to the serviceElementMap
     * @param instance The ServicBeanInstance to locate
     * @return The ServiceDeployment for a ServiceBeanInstance, or if not
     * found return a null
     */
    DeployedService getServiceDeployment(ServiceElement sElem,
                                           ServiceBeanInstance instance) {
        DeployedService deployedService = null;
        synchronized(serviceElementMap) {
            if(serviceElementMap.containsKey(sElem)) {
                List<DeployedService> list = serviceElementMap.get(sElem);
                DeployedService[] services =
                    list.toArray(new DeployedService[list.size()]);
                for (DeployedService service : services) {
                    if (service.getServiceBeanInstance().equals(instance)) {
                        deployedService = service;
                        break;
                    }
                }
            }
        }
        return deployedService;
    }
    /**
     * Get the ResourceCapability object
     * 
     * @return The ResourceCapability of the ServiceBeanInstantiator
     */
    ResourceCapability getResourceCapability() {
        ResourceCapability rCap;
        synchronized(resourceCapabilityLock) {
            rCap = resourceCapability;
        }
        return (rCap);
    }

    /**
     * Set the ResourceCapability
     * 
     * @param resourceCapability The ResourceCapability object of the
     * ServiceBeanInstantiator
     */
    void setResourceCapability(ResourceCapability resourceCapability) {
        synchronized(resourceCapabilityLock) {
            this.resourceCapability = resourceCapability;
        }
    }

    /**
     * Set the serviceLimit property
     * 
     * @param serviceLimit The maximum number of services the 
     * ServiceBeanInstantiator can instantiate 
     */
    void setServiceLimit(int serviceLimit) {
        synchronized(this) {
            this.serviceLimit = serviceLimit;
        }
    }

    /**
     * Get the serviceLimit property
     * 
     * @return The maximum number of services the ServiceBeanInstantiator can 
     * instantiate 
     */
    int getServiceLimit() {
        int limit;
        synchronized(this) {
            limit = serviceLimit;
        }
        return(limit);
    }

    /**
     * Increment the inprocess counter
     *
     * @param sElem The ServiceElement to add
     */
    void incrementProvisionCounter(ServiceElement sElem) {
        inProcessCounter.incrementAndGet();
        synchronized(inProcessMap) {
            if(inProcessMap.containsKey(sElem)) {
                int i = inProcessMap.get(sElem);
                i++;
                inProcessMap.put(sElem, i);
            } else {
                inProcessMap.put(sElem, 1);
            }
        }
    }
    /**
     * Decrement the inprocess counter
     *
      * @param sElem The ServiceElement to remove
     */
    void decrementProvisionCounter(ServiceElement sElem) {
        inProcessCounter.decrementAndGet();
        synchronized(inProcessMap) {
            if(inProcessMap.containsKey(sElem)) {
                int i = inProcessMap.get(sElem);
                i--;
                inProcessMap.put(sElem, i);
            }
        }
    }

    /**
     * Get the inprocess counter value
     *
     * @return The inprocess counter value
     */
    int getInProcessCounter() {
        return(inProcessCounter.intValue());
    }

    /**
     * Get the inprocess counter value for a ServiceElement
     *
     * @param sElem The ServiceElement to use
     *
     * @return The inprocess counter value for a ServiceElement
     */
    int getInProcessCounter(ServiceElement sElem) {
        int count = 0;
        synchronized(inProcessMap) {
            if(inProcessMap.containsKey(sElem)) {
                count = inProcessMap.get(sElem);
            }
        }
        return(count);
    }

    /**
     * Get all in process elements, excluding the element passed in
     *
     * @param exclude The ServiceElement to exclude
     *
     * @return An array of ServiceElements
     */
    ServiceElement[] getServiceElementsInprocess(ServiceElement exclude) {
        ArrayList<ServiceElement> list = new ArrayList<ServiceElement>();
        synchronized(inProcessMap) {
            Set<ServiceElement> keys = inProcessMap.keySet();
            for (ServiceElement element : keys) {
                if (!element.equals(exclude))
                    list.add(element);
            }
        }
        return (list.toArray(new ServiceElement[list.size()]));
    }

    /**
     * Get the host address of the ServiceBeanInstantiator
     * 
     * @return The host address of the ServiceBeanInstantiator as a
     * String "%d.%d.%d.%d"
     */
    String getHostAddress() {
        return (resourceCapability.getAddress());
    }

    /**
     * Get the host name of the ServiceBeanInstantiator
     *
     * @return The host name of the ServiceBeanInstantiator
     */
    String getHostName() {
        return (resourceCapability.getHostName());
    }

    /**
     * Set the dynamicEnabled attribute to <code>true</code> indicating that
     * the ServiceBeanInstantiator is available for the provisioning of
     * ServiceBean objects which have a provisioning type of <i>dynamic </i>
     */
    void setDynamicEnabledOn() {
        dynamicEnabled = true;
    }

    /**
     * Get the dynamicEnabled property
     * 
     * @return <code>true</code> if the ServiceBeanInstantiator is available
     * for the provisioning of ServiceBean objects which have a provisioning
     * type of <i>dynamic </i>, otherwise return <code>false</code>
     */
    boolean getDynamicEnabled() {
        return (dynamicEnabled);
    }

    /**
     * Determine if the provided ServiceElement can be instantiated on the
     * compute resource represented by this InstantiatorResource. If it is
     * determined that there are downloadable PlatformCapability components
     * which can meet the platform requirements the ServiceBean has declared,
     * the ServiceProvisionManagement object will be updated with the
     * ResourceCapability instance's identifier and the Collection of
     * PlatformCapability components to install
     * 
     * @param sElem The ServiceElement
     * @return Return true if the InstantiatorResource supports the
     * operational requirements of the ServiceBean.
     * @throws ProvisionException If there are errors obtaining available disk space
     */
    boolean canProvision(ServiceElement sElem) throws ProvisionException {
        if(sElem.getPlanned()==0)
            return(false);

        String provType = sElem.getProvisionType().toString();
        /*
        * Check if the serviceLimit has been reached
        */
        if(getServiceElementCount() == serviceLimit) {
            if(logger.isLoggable(Level.FINER) &&
               !provType.equals(ServiceElement.ProvisionType.FIXED.toString()))
                logger.log(Level.FINER,
                           "Do not allocate "+provType+" service "+
                           "["+sElem.getOperationalStringName()+"/"+sElem.getName()+"] to "+
                           getName()+" at ["+getHostAddress()+"], "+
                           "service limit of ["+serviceLimit+"] has been met");
            return(false);
        }

        /*
        * Check if the maximum amount per machine has been reached
        */
        if(sElem.getMaxPerMachine()!=-1) {
            int numInstances = getServiceElementCount(sElem)+
                               getInProcessCounter(sElem);
            if(numInstances >= sElem.getMaxPerMachine()) {
                if(logger.isLoggable(Level.FINER))
                    logger.log(Level.FINER,
                               "Do not allocate "+provType+" service "+
                               "["+sElem.getOperationalStringName()+"/"+sElem.getName()+"] to "+
                               getName()+" at ["+getHostAddress()+"], "+
                               "maximum number of services "+
                               "["+sElem.getMaxPerMachine()+"] "+
                               "per machine has been met");
                return(false);
            }
        }

        /*
        * Fixed service allocation is similar to maxPerMachine, ensure that
        * there are not too many service allocated
        */
        if(sElem.getProvisionType() == ServiceElement.ProvisionType.FIXED) {
            int planned = sElem.getPlanned();
            int actual = getServiceElementCount(sElem);
            int numAllowed = planned-actual;
            if(numAllowed <=0) {
                if(logger.isLoggable(Level.FINER))
                    logger.log(Level.FINER,
                               "Do not allocate "+provType+" service "+
                               "["+sElem.getOperationalStringName()+"/"+sElem.getName()+"] to "+
                               getName()+" at ["+getHostAddress()+"] has "+
                               "["+actual+"] "+
                               "instance(s), planned ["+planned+"]");
                return(false);
            } else {
                if(logger.isLoggable(Level.FINER))
                    logger.log(Level.FINER,
                               getName()+" at ["+getHostAddress()+"] has "+
                               "["+actual+"]"+
                               " instance(s), planned ["+planned+"] of "+
                               provType+" "+
                               "service ["+sElem.getOperationalStringName()+"/"+sElem.getName()+"]");
            }
        }

        if(!AssociationMatcher.meetsColocationRequirements(sElem, this)) {
            if(logger.isLoggable(Level.FINER)) {
                StringBuffer b = new StringBuffer();
                b.append("Do not allocate ")
                    .append(provType)
                    .append(" " + "service [")
                    .append(sElem.getOperationalStringName())
                    .append("/")
                    .append(sElem.getName())
                    .append("] to ")
                    .append(getName())
                    .append(" " + "at [")
                    .append(getHostAddress())
                    .append("], " + "required colocated services not present: ");
                AssociationDescriptor[] aDesc =
                    ServiceElementUtil.getAssociationDescriptors(
                        sElem, AssociationType.COLOCATED);
                int found = 0;
                for (AssociationDescriptor anADesc : aDesc) {
                    if (found > 0)
                        b.append(", ");
                    found++;
                    b.append(anADesc.getName());
                }
                logger.finer("{"+b.toString()+"}");
            }
            return (false);
        }

        if(!AssociationMatcher.meetsOpposedRequirements(sElem, this)) {
            return (false);
        }

        if(!resourceCapability.measuredResourcesWithinRange()) {
            if(logger.isLoggable(Level.FINER)) {
                StringBuffer buffer = new StringBuffer();
                MeasuredResource[] m =
                    resourceCapability.getMeasuredResources(
                        ResourceCapability.MEASURED_RESOURCES_BREACHED);
                for (MeasuredResource aM : m)
                    buffer.append("\n[")
                        .append(aM.getIdentifier())
                        .append("] Low=[")
                        .append(aM.getThresholdValues().getLowThreshold())
                        .append("], High=[")
                        .append(aM.getThresholdValues().getHighThreshold())
                        .append("], Actual=[")
                        .append(aM.getValue())
                        .append("]");

                logger.finer(getName()+" at ["+ getHostAddress()+"] " +
                             "not eligible for "+
                             provType+" service ["+sElem.getOperationalStringName()+"/"+sElem.getName()+"], "+
                             "MeasuredResources have exceeded threshold " +
                             "constraints : "+buffer.toString());
            }
            return(false);
        }
        if(meetsGeneralRequirements(sElem) &&
           meetsQuantitativeRequirements(sElem)) {
            Collection<SystemComponent> unsupportedReqs =
                meetsQualitativeRequirements(sElem.getServiceLevelAgreements());
            if(unsupportedReqs.size() == 0) {
                if(logger.isLoggable(Level.FINER))
                    logger.finer(getName()+" at ["+getHostAddress()+"] meets "+
                                 "qualitative requirements for "+
                                 "["+sElem.getOperationalStringName()+"/"+sElem.getName()+"]");
                return (true);
            } else {
                /* Create a String representation of the unsupportedReqs
                 * object for logging */
                int x = 0;
                StringBuffer buffer = new StringBuffer();
                for (SystemComponent unsupportedReq : unsupportedReqs) {
                    if (x > 0)
                        buffer.append(", ");
                    buffer.append("[").append(unsupportedReq.toString()).append(
                        "]");
                    x++;
                }
                String unsupportedReqsString = buffer.toString();
                if(logger.isLoggable(Level.FINER))
                    logger.finer(getName()+" at ["+getHostAddress()+"] " +
                                 "does not meet qualitative requirements for "+
                                 provType+" service ["+sElem.getOperationalStringName()+"/"+sElem.getName()+"], "+
                                 "determine if SystemRequirement objects can " +
                                 "be downloaded : "+ unsupportedReqsString);
                /* Determine if the resource supports persistent provisioning */
                if(!resourceCapability.supportsPersistentProvisioning()) {
                    if(logger.isLoggable(Level.FINER))
                        logger.finer("Cannot allocate "+provType+" service "+
                                     "["+sElem.getOperationalStringName()+"/"+sElem.getName()+"] to "+
                                     getName()+" at ["+getHostAddress()+"], "
                                     + "required SystemComponents "
                                     + "cannot be provisioned. This is " +
                                     "because the "+getName()+" is not " +
                                     "configured for persistentProvisioning. " +
                                     "If you want to enable this feature, " +
                                     "verify the "+getName()+"'s configuration " +
                                     "for the org.rioproject.cybernode." +
                                     "persistentProvisioning property " +
                                     "is set to true");
                    return (false);
                }
                /*
                 * Check if the unsupported PlatformCapability objects can be
                 * provisioned. If there are any that cannot be provisioned move
                 * onto the next resource
                 */
                boolean provisionableCaps = true;
                for (SystemComponent sysReq : unsupportedReqs) {
                    if (sysReq.getStagedSoftware().length == 0) {
                        provisionableCaps = false;
                        break;
                    }
                }
                if(!provisionableCaps) {
                    if(logger.isLoggable(Level.FINER))
                        logger.finer(getName()+" at ["+getHostAddress()+"] "
                                     + "does not meet qualitative " +
                                     "requirements "+
                                     "for "+provType+" service " +
                                     "["+sElem.getOperationalStringName()+"/"+sElem.getName()+"]. "
                                     + "PlatformCapability "
                                     + "objects are not configured to be " +
                                     "downloadable : "
                                     + unsupportedReqsString);
                    return (false);
                }
                /* Get the size of the download(s) */
                int requiredSize = 0;
                IOException failed = null;
                try {
                    for (SystemComponent sysReq : unsupportedReqs) {
                        StagedSoftware[] downloads = sysReq.getStagedSoftware();
                        for(StagedSoftware download : downloads) {
                            int size = download.getDownloadSize();
                            if(size < 0) {
                                logger.warning("Unable to obtain " +
                                               "download size for "+
                                               download.getLocation()+
                                               ", abort provision request");
                                requiredSize = size;
                                break;
                            }
                            requiredSize += size;
                            if(download.getPostInstallAttributes() != null &&
                               download.getPostInstallAttributes()
                                   .getStagedData() != null) {
                                StagedData postInstall =
                                    download.getPostInstallAttributes().getStagedData();
                                size = postInstall.getDownloadSize();
                                if(size < 0) {
                                    logger.warning("Unable to obtain " +
                                                   "download size for " +
                                                   "PostInstall "+
                                                   postInstall.getLocation()+
                                                   ", abort provision request");
                                    requiredSize = size;
                                    break;
                                }
                                requiredSize += size;
                            }
                        }
                    }
                } catch(IOException e) {
                    failed = e;
                }

                if (requiredSize < 0 || failed!=null)
                    throw new ProvisionException("Service ["+sElem.getOperationalStringName()+"/"+sElem.getName()+"] "+
                                                 "instantiation failed",
                                                 failed==null?
                                                 new IOException("Unable to obtain download size"):failed,
                                                 true);
                /* Find out if the resource has the necessary disk-space */
                if(supportsStorageRequirement(
                                  requiredSize,
                                  resourceCapability.getPlatformCapabilities())) {
                    if(logger.isLoggable(Level.FINER))
                        logger.finer(getName()+" at ["+getHostAddress()+"] "+
                                     "supports provisioning requirements for "+
                                     provType+" service ["+sElem.getOperationalStringName()+"/"+sElem.getName()+"]");

                    sElem.setProvisionablePlatformCapabilities(unsupportedReqs);
                    return (true);
                }
                if(logger.isLoggable(Level.FINER)) {
                    double avail =
                        getAvailableStorage(
                            resourceCapability.getPlatformCapabilities());
                    StringBuffer sb = new StringBuffer();
                    sb.append(getName())
                        .append(" at [").append(getHostAddress()).append("] ");
                    if(avail>0) {
                        /* For logging purposes compute the size in GB */
                        double GB = Math.pow(1024, 3);
                        avail = avail/GB;
                        sb.append("does not have adequate disk-space for ")
                            .append("[")
                            .append(sElem.getOperationalStringName())
                            .append("/")
                            .append(sElem.getName()).append("] ")
                            .append("Required=")
                            .append(+requiredSize).append(", ")
                            .append("Available=").append(avail).append(" GB");
                    } else {
                        sb.append("does not report a StorageCapability. ")
                            .append("Rio cannot allocate the ")
                            .append("[")
                            .append(sElem.getOperationalStringName())
                            .append("/")
                            .append(sElem.getName())
                            .append("] ")
                            .append("service with a software download size of ")
                            .append(+requiredSize).append(". ")
                            .append("This may be due to a known limitation ")
                            .append("found when running the ").append(getName())
                            .append(" on a Windows machine, or if the ")
                            .append("DiskSpace monitor has been disabled. ")
                            .append("Check the Cybernode environment and ")
                            .append("configuration.");
                    }

                    logger.finer(sb.toString());
                }
                return (false);
            }
        } else {
            if(logger.isLoggable(Level.FINER))
                logger.finer(getName()+" at ["+getHostAddress()+"] does "
                             + "not meet general or quantitative requirements "
                             + "for "+provType+" service ["+sElem.getOperationalStringName()+"/"+sElem.getName()+"]");
            return (false);
        }
    }

    /**
     * Determine if an Array of PlatformCapability components contains a
     * StorageCapability and if that StorageCapability has the requested disk
     * space size available
     * 
     * @param requestedSize The size to verify
     * @param pCaps Array of PlatformCapability instances to use
     * @return Return true if the Array of PlatformCapability
     * components contains a StorageCapability and if that StorageCapability has
     * the requested disk space size available
     */
     boolean supportsStorageRequirement(int requestedSize,
                                        PlatformCapability[] pCaps) {
        boolean supports = false;
        for (PlatformCapability pCap : pCaps) {
            if (pCap instanceof
                StorageCapability) {
                try {
                    StorageCapability storage = (StorageCapability) pCap;
                    supports = storage.supports(requestedSize);
                    break;
                } catch (Throwable t) {
                    return (false);
                }
            }
        }
        return (supports);
    }

    /**
     * Determine if an Array of PlatformCapability components contains a
     * StorageCapability and if that StorageCapability has the requested disk
     * space size available
     *
     * @param requestedSize The size to verify
     * @param requirement The class to ensure compatibility
     * @param pCaps Array of PlatformCapability instances to use
     *
     * @return Return true if the Array of PlatformCapability components
     *         contains a StorageCapability and if that StorageCapability has
     *         the requested disk space size available
     */
    boolean supportsSystemRequirement(int requestedSize,
                                      Class requirement,
                                      PlatformCapability[] pCaps) {
        boolean supports = false;
        for (PlatformCapability pCap : pCaps) {
            if (pCap.getClass().isAssignableFrom(requirement)) {
                try {
                    StorageCapability storage = (StorageCapability) pCap;
                    supports = storage.supports(requestedSize);
                    break;
                } catch (Throwable t) {
                    return (false);
                }
            }
        }
        return (supports);
    }

    /**
     * Get the available storage from the StorageCapability
     *
     * @param pCaps Array of PlatformCapability instances to use
     *
     * @return The available storage from the StorageCapability. If a
     * StorageCapability cannot be found return -1
     */
    double getAvailableStorage(PlatformCapability[] pCaps) {
        double available = -1;
        for (PlatformCapability pCap : pCaps) {
            if (pCap instanceof StorageCapability) {
                StorageCapability storage = (StorageCapability) pCap;
                Double dCap =
                    (Double) storage.getValue(StorageCapability.CAPACITY);
                if (dCap != null) {
                    available = dCap;
                }
                break;
            }
        }
        return(available);
    }

    /**
     * This method determines whether or not the defined criteria meets general 
     * requirements:
     * <ul>
     * <li>If there is a cluster of machines defined, the compute resource is
     * defined in the cluster of machines that have been defined
     * </ul>
     * <br>
     * 
     * @param sElem The ServiceElement object
     * @return Return true if the provided ResourceCapability meets
     * general requirements
     */
    boolean meetsGeneralRequirements(ServiceElement sElem) {
        /*
         * If we have a cluster defined, then see if the provided resource has
         * either an IP address or hostname thats in the list of IP addresses
         * and hostnames in our machine cluster list. If it isnt in the list,
         * then there is no sense in proceeding
         */
        String[] machineCluster = sElem.getCluster();
        if(machineCluster != null) {
            if(machineCluster.length > 0) {
                if(logger.isLoggable(Level.FINER))
                    logger.finer("ServiceBean ["+sElem.getOperationalStringName()+"/"+sElem.getName()+"] has a "+
                                 "cluster requirement");
                boolean found = false;
                for (String aMachineCluster : machineCluster) {
                    if (aMachineCluster.equals(resourceCapability.getAddress())
                        ||
                        aMachineCluster.equals(resourceCapability.getHostName()))
                        found = true;
                }
                if(!found) {
                    if(logger.isLoggable(Level.FINER))
                        logger.finer(getName()+" at ["+getHostAddress()+"] " +
                                     "not found in cluster requirement for "+
                                     "["+sElem.getOperationalStringName()+"/"+sElem.getName()+"]");
                    return (false);
                }
            }
        }
        return (true);
    }

    /**
     * This method verifies whether the ResourceCapability can support the
     * Qualitative Requirements specified by the ServiceBean
     * 
     * @param sla The ServiceLevelAgreements object
     * @return A Collection of SystemRequirement objects which the
     * ResourceCapability does not support. If the Collection has zero entries,
     * then the provided ResourceCapability supports the Qualitative
     * Requirements specified by the ServiceBean
     */
    Collection<SystemComponent> meetsQualitativeRequirements(ServiceLevelAgreements sla) {
        PlatformCapability[] platformCapabilities =
            resourceCapability.getPlatformCapabilities();
        SystemComponent[] jsbRequirements =
            sla.getSystemRequirements().getSystemComponents();
        ArrayList<SystemComponent> unsupportedReqs =
            new ArrayList<SystemComponent>();
        /*
         * If there are no PlatformCapability requirements we can return
         * successfully
         */
        if(jsbRequirements.length == 0)
            return (unsupportedReqs);
        /*
         * Check each of our PlatformCapability objects for supportability
         */
        for (SystemComponent jsbRequirement : jsbRequirements) {
            boolean supported = false;
            /*
             * Iterate through all resource PlatformCapability objects and see
             * if any of them supports the current PlatformCapability. If none
             * are found, then we dont have a match
             */
            for (PlatformCapability platformCapability : platformCapabilities) {
                if (platformCapability.supports(jsbRequirement)) {
                    supported = true;
                    break;
                }
            }
            if (!supported)
                unsupportedReqs.add(jsbRequirement);
        }
        return (unsupportedReqs);
    }

    /**
     * This method verifies whether the ResourceCapability can support the
     * Quantitative Requirements specified by the ServiceBean
     * 
     * @param sElem The ServiceElement object
     * @return Return true if the provided ResourceCapability meets
     * Quantitative requirements
     */
    boolean meetsQuantitativeRequirements(ServiceElement sElem) {
        ServiceLevelAgreements sla = sElem.getServiceLevelAgreements();
        boolean provisionable = true;
        String[] systemThresholdIDs = sla.getSystemRequirements().getSystemThresholdIDs();
        if(systemThresholdIDs.length == 0)
            return (true);
        MeasuredResource[] measured = resourceCapability.getMeasuredResources();
        /*
         * If the number of MeasuredCapabilities is less then what we are asking
         * for there is no reason to continue
         */
        if(measured == null || measured.length < systemThresholdIDs.length) {
            if(logger.isLoggable(Level.FINER)) {
                if(measured==null)
                    logger.finer(getName()+" at ["+getHostAddress()+"] "+
                                 "has a [null] "+
                                 "MeasuredCapability instance, "+
                                 "ServiceBean ["+sElem.getOperationalStringName()+"/"+sElem.getName()+"] "+
                                 "has a requirement to test "+
                                 "["+systemThresholdIDs.length+"]");
                else
                    logger.finer(getName()+" at ["+getHostAddress()+"] "+
                                 "only has ["+measured.length+"] "+
                                 "MeasuredCapability instances, "+
                                 "ServiceBean ["+sElem.getOperationalStringName()+"/"+sElem.getName()+"] "+
                                 "has a requirement to test "+
                                 "["+systemThresholdIDs.length+"]");
            }
            return (false);
        }
        /*
         * Check each of the MeasuredResource objects
         */
        StringBuffer buffer = new StringBuffer();
        if(logger.isLoggable(Level.FINER))
            buffer.append("Evaluate [")
                .append(systemThresholdIDs.length)
                .append("] " + "System Threshold Requirements for " + "[")
                .append(sElem.getOperationalStringName())
                .append("/")
                .append(sElem.getName())
                .append("] using ")
                .append(getName())
                .append(" at [")
                .append(getHostAddress())
                .append("]");
        for (String systemThresholdID : systemThresholdIDs) {
            boolean supported = false;
            ThresholdValues systemThreshold =
                sla.getSystemRequirements().getSystemThresholdValue(systemThresholdID);
            if (systemThresholdID.equals(SystemRequirements.SYSTEM)) {
                double systemUtilization = systemThreshold.getHighThreshold();
                if (systemUtilization < resourceCapability.getUtilization()) {
                    if (logger.isLoggable(Level.FINER)) {
                        buffer.append("\n")
                            .append("Cannot meet system utilization requirement. Desired [")
                            .append(systemUtilization)
                            .append("], Actual [")
                            .append(resourceCapability.getUtilization())
                            .append("]");
                        logger.finest(buffer.toString());
                    }
                    return (false);
                } else {
                    supported = true;
                    if (logger.isLoggable(Level.FINER))
                        buffer.append("\n")
                            .append("[System] utilization requirement met. Desired [")
                            .append(systemUtilization)
                            .append("], Actual [")
                            .append(resourceCapability.getUtilization())
                            .append("]");
                }
            }
            /*
             * Iterate through all resource MeasuredResource objects and see if
             * any of them supports the current MeasuredResource. If none are
             * found, then we dont have a match
             */
            for (MeasuredResource mRes : measured) {
                if (mRes.getIdentifier().equals(systemThresholdID)) {
                    if (mRes.evaluate(systemThreshold)) {
                        supported = true;
                        if (logger.isLoggable(Level.FINER))
                            buffer.append("\n[")
                                .append(systemThresholdID)
                                .append("] utilization requirement met. Desired " + "Low=[")
                                .append(systemThreshold.getLowThreshold())
                                .append("], High=[")
                                .append(systemThreshold.getHighThreshold())
                                .append("], Actual=[")
                                .append(mRes.getValue())
                                .append("]");
                        break;
                    } else {
                        if (logger.isLoggable(Level.FINER))
                            buffer.append("\n")
                                .append("Cannot meet [")
                                .append(systemThresholdID)
                                .append("] utilization requirement. Desired Low=[")
                                .append(systemThreshold.getLowThreshold())
                                .append("], High=[")
                                .append(systemThreshold.getHighThreshold())
                                .append("], Actual=[")
                                .append(mRes.getValue())
                                .append("]");
                    }
                }
            }

            if (!supported) {
                provisionable = false;
                break;
            }
        }

        if(logger.isLoggable(Level.FINER))
            logger.finer(buffer.toString());

        return (provisionable);
    }
}
