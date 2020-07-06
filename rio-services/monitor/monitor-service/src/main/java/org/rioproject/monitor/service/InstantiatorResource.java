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
package org.rioproject.monitor.service;

import net.jini.id.Uuid;
import org.rioproject.associations.AssociationDescriptor;
import org.rioproject.associations.AssociationType;
import org.rioproject.deploy.*;
import org.rioproject.impl.servicebean.ServiceElementUtil;
import org.rioproject.monitor.service.util.LoggingUtil;
import org.rioproject.opstring.ServiceElement;
import org.rioproject.sla.ServiceLevelAgreements;
import org.rioproject.system.MeasuredResource;
import org.rioproject.system.ResourceCapability;
import org.rioproject.system.capability.PlatformCapability;
import org.rioproject.system.capability.connectivity.TCPConnectivity;
import org.rioproject.system.capability.platform.OperatingSystem;
import org.rioproject.system.capability.platform.ProcessorArchitecture;
import org.rioproject.system.capability.platform.StorageCapability;
import org.rioproject.watch.ThresholdValues;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.rmi.MarshalledObject;
import java.rmi.RemoteException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * An {@code InstantiatorResource} is the object being leased and controlled by the
 * {@code ServiceResource}, and represents an available {@link ServiceBeanInstantiator} service
 * that can be used to instantiate a service.
 * 
 * @see org.rioproject.servicebean.ServiceBean
 * @see org.rioproject.deploy.ServiceBeanInstantiator
 *
 * @author Dennis Reedy
 */
public class InstantiatorResource {
    /**
     * The ServiceBeanInstantiator
     */
    private final ServiceBeanInstantiator instantiator;
    /**
     * The ServiceBeanInstantiator wrapped in a MarshalledObject
     */
    private final MarshalledObject<ServiceBeanInstantiator> wrappedServiceBeanInstantiator;
    /**
     * The maximum number of services the ServiceBeanInstantiator can instantiate 
     */
    private final AtomicInteger serviceLimit = new AtomicInteger(0);
    /**
     * An in-process counter indicating the InstantiatorResource is being used to
     * provision a service 
     */
    private final AtomicInteger inProcessCounter = new AtomicInteger();
    /**
     * The handback option provided by the ServiceBeanInstantiator and sent back
     * to the ServiceBeanInstantiator as part of the ProvisionEvent
     */
    private final MarshalledObject<?> handback;
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
    private final Map<ServiceElement, List<DeployedService>> serviceElementMap = new ConcurrentHashMap<>();
    /** Table of in process ServiceElement instances */
    private final Map<ServiceElement, Integer> inProcessMap = new ConcurrentHashMap<>();
    /**
     * Name of the ServiceBeanInstantiator
     */
    private final String instantiatorName;
    /**
     * The Uuid that has been assigned to the ServiceBeanInstantiator
     */
    private final Uuid instantiatorUuid;
    private final List<ServiceElement> uninstantiables = new ArrayList<>();
    /** The Logger */
    private static final Logger logger = LoggerFactory.getLogger(InstantiatorResource.class);

    /**
     * Create an InstantiatorResource
     *
     * @param wrappedServiceBeanInstantiator The ServiceBeanInstantiator wrapped in a MarshalledObject
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
    public InstantiatorResource(MarshalledObject<ServiceBeanInstantiator> wrappedServiceBeanInstantiator,
                                ServiceBeanInstantiator instantiator,
                                String instantiatorName,
                                Uuid instantiatorUuid,
                                MarshalledObject<?> handback,
                                ResourceCapability resourceCapability,
                                int serviceLimit) {
        this.wrappedServiceBeanInstantiator = wrappedServiceBeanInstantiator;
        this.instantiator = instantiator;
        this.instantiatorName = instantiatorName;
        this.instantiatorUuid = instantiatorUuid;
        this.handback = handback;
        this.resourceCapability = resourceCapability;
        this.serviceLimit.set(serviceLimit);
    }

    MarshalledObject<ServiceBeanInstantiator> getWrappedServiceBeanInstantiator() {
        return wrappedServiceBeanInstantiator;
    }

    /**
     * Add a DeployedService instance to the serviceElementMap.
     * 
     * @param newDeployedService The service to add
     */
    public void addDeployedService(DeployedService newDeployedService) {
        ServiceElement sElem = newDeployedService.getServiceElement();
        if(serviceElementMap.containsKey(sElem)) {
            List<DeployedService> list = serviceElementMap.get(sElem);
            if(!list.contains(newDeployedService)) {
                list.add(newDeployedService);
                serviceElementMap.put(sElem, list);
            }
        } else {
            List<DeployedService> list = new ArrayList<>();
            list.add(newDeployedService);
            serviceElementMap.put(sElem, list);
        }
    }

    /**
     * Set the DeployedService instances
     *
     * @param deployedServices List of active & deployed services
     */
    void setDeployedServices(List<DeployedService> deployedServices) {
        serviceElementMap.clear();
        for(DeployedService deployedService : deployedServices) {
            addDeployedService(deployedService);
        }
    }

    /**
     * Get the name of the ServiceBeanInstantiator
     *
     * @return The name of the ServiceBeanInstantiator
     */
    public String getName() {
        return(instantiatorName);
    }

    /**
     * Get the Uuid that has been assigned to the ServiceBeanInstantiator
     *
     * @return The Uuid for the ServiceBeanInstantiator
     */
    public Uuid getInstantiatorUuid() {
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
                records = getInstantiator().getServiceRecords(ServiceRecord.ACTIVE_SERVICE_RECORD);
                break;
            } catch(RemoteException e) {
                logger.warn("Exception [{}] occurred, retry [{}] ....", e.getClass().getName(), i);
                toThrow = e;
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ignore) {
                    logger.trace("Timeout Interrupted, handled");
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
         * to attempt to get the active ServiceRecord instances
         */
        int RETRY = 3;
        RemoteException toThrow = null;
        for(int i=0; i< RETRY; i++) {
            try {
                statement = getInstantiator().getServiceStatement(elem);
                break;
            } catch(RemoteException e) {
                logger.warn("Exception [{}] occurred, retry [{}] ....", e.getClass().getName(), i);
                toThrow = e;
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ignore) {
                    logger.trace("Timeout Interrupted, handled");
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
        if(serviceElementMap.containsKey(sElem)) {
            List<DeployedService> list = serviceElementMap.get(sElem);
            DeployedService[] ids = list.toArray(new DeployedService[0]);
            for (DeployedService deployedService : ids) {
                if (deployedService.getServiceBeanInstance().getServiceBeanID().equals(uuid)) {
                    found = true;
                    break;
                }
            }
        }
        return found;
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
        if(serviceElementMap.containsKey(sElem)) {
            List<DeployedService> list = serviceElementMap.get(sElem);
            list.removeIf(Objects::isNull);
            for (DeployedService deployedService : list) {
                if (deployedService.getServiceBeanInstance() != null
                        && deployedService.getServiceBeanInstance().getServiceBeanID().equals(uuid)) {
                    list.remove(deployedService);
                    removedInstance = deployedService.getServiceBeanInstance();
                    break;
                }
            }
            if(list.isEmpty()) {
                serviceElementMap.remove(sElem);
            } else {
                serviceElementMap.put(sElem, list);
            }
        }
        return removedInstance;
    }

    ServiceElement[] getServiceElements() {
        ServiceElement[] elems = new ServiceElement[serviceElementMap.size()];
        int i=0;
        for (Map.Entry<ServiceElement, List<DeployedService>> entry : serviceElementMap.entrySet()) {
            elems[i++] = entry.getKey();
        }
        return elems;
    }

    /**
     * Get the number of ServiceElement instances
     *
     * @param sElem The ServiceElement to count
     * 
     * @return The number of instances of the ServiceElement the
     * ServiceBeanInstantiator has instantiated. If not found return 0
     */
    public int getServiceElementCount(ServiceElement sElem) {
        int numInstances = 0;
        if(serviceElementMap.containsKey(sElem)) {
            List<DeployedService> list = serviceElementMap.get(sElem);
            numInstances = list.size();
        }
        logger.trace("Get service element count for [{}], {} has {} instances",
                     LoggingUtil.getLoggingName(sElem), getName(), numInstances);
        return numInstances;
    }

    /**
     * Get the total number of all ServiceElement instances
     * 
     * @return The total number of ServiceElement instances the 
     * ServiceBeanInstantiator has instantiated
     */
    public int getServiceElementCount() {
        int totalInstances = 0;
        Set<ServiceElement> keys = serviceElementMap.keySet();
        for (ServiceElement key : keys) {
            List<DeployedService> list = serviceElementMap.get(key);
            totalInstances += list.size();
        }
        return totalInstances;
    }

    /**
     * Get the ServiceBeanInstantiator
     * 
     * @return The Instantiator
     */
    public ServiceBeanInstantiator getInstantiator() {
        return (instantiator);
    }

    /**
     * Get the number of active and in-process services
     *
     * @return The number of active and in-process services.
     */
    public int getServiceCount() {
        int count = getServiceElementCount();
        int inProcess = getInProcessCounter();
        return count + inProcess;
    }

    /**
     * Get the handback object
     * 
     * @return The handback object
     */
    public MarshalledObject<?> getHandback() {
        return (handback);
    }

    /**
     * Get the ServiceDeployment for a ServiceBeanInstance
     *
     * @param sElem The ServiceElement, used as the key to the serviceElementMap
     * @param instance The ServiceBeanInstance to locate
     * @return The ServiceDeployment for a ServiceBeanInstance, or if not
     * found return a null
     */
    DeployedService getServiceDeployment(ServiceElement sElem, ServiceBeanInstance instance) {
        DeployedService deployedService = null;
        if(serviceElementMap.containsKey(sElem)) {
            List<DeployedService> list = serviceElementMap.get(sElem);
            DeployedService[] services = list.toArray(new DeployedService[0]);
            for (DeployedService service : services) {
                if (service.getServiceBeanInstance().equals(instance)) {
                    deployedService = service;
                    break;
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
    public ResourceCapability getResourceCapability() {
        ResourceCapability rCap;
        synchronized(resourceCapabilityLock) {
            rCap = resourceCapability;
        }
        return rCap;
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
     * @param serviceLimit The maximum number of services the ServiceBeanInstantiator can instantiate
     */
    void setServiceLimit(int serviceLimit) {
        this.serviceLimit.set(serviceLimit);
    }

    /**
     * Get the serviceLimit property
     * 
     * @return The maximum number of services the ServiceBeanInstantiator can instantiate
     */
    public int getServiceLimit() {
        return serviceLimit.get();
    }

    /**
     * Increment the inprocess counter
     *
     * @param sElem The ServiceElement to add
     */
    public void incrementProvisionCounter(ServiceElement sElem) {
        inProcessCounter.incrementAndGet();
        if(inProcessMap.containsKey(sElem)) {
            int i = inProcessMap.get(sElem);
            i++;
            inProcessMap.put(sElem, i);
        } else {
            inProcessMap.put(sElem, 1);
        }
    }
    /**
     * Decrement the inprocess counter
     *
      * @param sElem The ServiceElement to remove
     */
    public synchronized void decrementProvisionCounter(ServiceElement sElem) {
        if(inProcessCounter.get()>0) {
            inProcessCounter.decrementAndGet();
        }
        if(inProcessMap.containsKey(sElem)) {
            int i = inProcessMap.get(sElem);
            i--;
            if(i==0)
                inProcessMap.remove(sElem);
            else
                inProcessMap.put(sElem, i);
        }
    }

    /**
     * Get the in-process counter value
     *
     * @return The in-process counter value
     */
    public int getInProcessCounter() {
        return inProcessCounter.get();
    }

    /**
     * Get the inprocess counter value for a ServiceElement
     *
     * @param sElem The ServiceElement to use
     *
     * @return The inprocess counter value for a ServiceElement
     */
    public int getInProcessCounter(ServiceElement sElem) {
        int count = 0;
        if(inProcessMap.containsKey(sElem)) {
            count = inProcessMap.get(sElem);
        }
        return count;
    }

    /**
     * Get all in process elements, excluding the element passed in
     *
     * @param exclude The ServiceElement to exclude
     *
     * @return An array of ServiceElements
     */
    ServiceElement[] getServiceElementsInprocess(ServiceElement exclude) {
        ArrayList<ServiceElement> list = new ArrayList<>();
        Set<ServiceElement> keys = inProcessMap.keySet();
        for (ServiceElement element : keys) {
            if (!element.equals(exclude))
                list.add(element);
        }
        return list.toArray(new ServiceElement[0]);
    }

    /**
     * Get the host address of the ServiceBeanInstantiator
     * 
     * @return The host address of the ServiceBeanInstantiator
     */
    public String getHostAddress() {
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

    public void addUninstantiable(ServiceElement serviceElement) {
        uninstantiables.add(serviceElement);
    }

    public boolean isUninstantiable(ServiceElement serviceElement) {
        return uninstantiables.contains(serviceElement);
    }

    public void removeUninstantiable(ServiceElement serviceElement) {
        uninstantiables.remove(serviceElement);
    }

    /**
     * Set the dynamicEnabled attribute to <code>true</code> indicating that
     * the ServiceBeanInstantiator is available for the provisioning of
     * ServiceBean objects which have a provisioning type of <i>dynamic </i>
     */
    public void setDynamicEnabledOn() {
        dynamicEnabled = true;
    }

    /**
     * Get the dynamicEnabled property
     * 
     * @return <code>true</code> if the ServiceBeanInstantiator is available
     * for the provisioning of ServiceBean objects which have a provisioning
     * type of <i>dynamic </i>, otherwise return <code>false</code>
     */
    public boolean getDynamicEnabled() {
        return (dynamicEnabled);
    }

    /**
     * Determine if the provided {@code ProvisionRequest} can be instantiated on the
     * compute resource represented by this {@code InstantiatorResource}. If it is
     * determined that there are downloadable {@code PlatformCapability} components
     * which can meet the platform requirements the service has declared, these components will be
     * verified, and the targeted {@code InstantiatorResource} checked to ensure adequate disk space is available.
     *
     * @param provisionRequest The {@code ProvisionRequest}
     * @return Return true if the {@code InstantiatorResource} supports the
     * operational requirements of the {@code ProvisionRequest}
     * @throws ProvisionException If there are errors obtaining available disk space. Note this will only
     * happen if the {@code ProvisionRequest} contains downloadable {@code PlatformCapability} components and there
     * is a problem obtaining the size of the download.
     */
    public boolean canProvision(final ProvisionRequest provisionRequest) throws ProvisionException {
        ServiceElement sElem = provisionRequest.getServiceElement();
        if(sElem.getPlanned()==0)
            return(false);

        String provType = sElem.getProvisionType().toString();
        /*
         * Check if the serviceLimit has been reached
         */
        if(getServiceElementCount() == serviceLimit.get() &&
           !provType.equals(ServiceElement.ProvisionType.FIXED.toString())) {
            String failureReason =
                String.format("%s not selected to allocate service [%s], it has reached it's service limit of [%d]",
                              getName(), LoggingUtil.getLoggingName(sElem), serviceLimit.get());

            provisionRequest.addFailureReason(failureReason);
            logger.debug(failureReason);
            return(false);
        }

        /*
         * Check if the maximum amount per machine has been reached
         */
        if(sElem.getMaxPerMachine()!=-1) {
            int serviceCount = getServiceElementCount(sElem);
            int inProcessCount = getInProcessCounter(sElem);
            int numInstances = serviceCount+inProcessCount;
            if(numInstances >= sElem.getMaxPerMachine()) {
                String failureReason =
                    String.format("%s not selected to allocate service [%s], declaration specifies no more than %d services per machine, found %d",
                                  getName(), LoggingUtil.getLoggingName(sElem), sElem.getMaxPerMachine(), numInstances);
                provisionRequest.addFailureReason(failureReason);
                logger.debug(failureReason);
                return(false);
            }
        }

        /*
         * Fixed service allocation is similar to maxPerMachine, ensure that
         * there are not too many service allocated
         */
        if(sElem.getProvisionType() == ServiceElement.ProvisionType.FIXED) {
            int planned = sElem.getPlanned();
            int actual = getServiceElementCount(sElem)+getInProcessCounter(sElem);
            int numAllowed = planned-actual;
            if(numAllowed <= 0) {
                String failureReason =
                    String.format("Do not allocate %s service [%s] to %s has [%d] instance(s), planned [%d]",
                                  provType, LoggingUtil.getLoggingName(sElem), getName(), actual, planned);
                provisionRequest.addFailureReason(failureReason);
                logger.debug(failureReason);
                return false;
            } else {
                String failureReason =
                    String.format("%s has [%d] instance(s), planned [%d] of %s service [%s]",
                                  getName(), actual, planned, provType, LoggingUtil.getLoggingName(sElem));
                provisionRequest.addFailureReason(failureReason);
                logger.debug(failureReason);
            }
        }

        if(!AssociationMatcher.meetsColocationRequirements(sElem, this)) {
            StringBuilder b = new StringBuilder();
            b.append(getName()).append(" not selected to allocate ").append(LoggingUtil.getLoggingName(sElem));
            b.append(", required colocated services not present: ");
            AssociationDescriptor[] aDesc = ServiceElementUtil.getAssociationDescriptors(sElem, AssociationType.COLOCATED);
            int found = 0;
            for (AssociationDescriptor anADesc : aDesc) {
                if (found > 0)
                    b.append(", ");
                found++;
                b.append(anADesc.getName());
            }
            String failureReason = b.toString();
            provisionRequest.addFailureReason(failureReason);
            logger.debug(failureReason);
            return false;
        }

        if(!AssociationMatcher.meetsOpposedRequirements(sElem, this)) {
            String failureReason = AssociationMatcher.getLastErrorMessage();
            provisionRequest.addFailureReason(failureReason);
            logger.debug(failureReason);
            return  false;
        }

        if(!resourceCapability.measuredResourcesWithinRange()) {
            StringBuilder buffer = new StringBuilder();
            MeasuredResource[] m = resourceCapability.getMeasuredResources(ResourceCapability.MEASURED_RESOURCES_BREACHED);
            for (MeasuredResource aM : m) {
                buffer.append("\n");
                buffer.append("[").append(aM.getIdentifier()).append("] ");
                buffer.append("Low: ").append(aM.getThresholdValues().getLowThreshold()).append(", ");
                buffer.append("High: ").append(aM.getThresholdValues().getHighThreshold()).append(", ");
                buffer.append("Actual: ").append(aM.getValue());
            }

            String failureReason =
                String.format("%s not selected to allocate service [%s], MeasuredResources have exceeded threshold constraints: %s",
                              getName(), LoggingUtil.getLoggingName(sElem), buffer.toString());
            provisionRequest.addFailureReason(failureReason);
            logger.debug(failureReason);
            return false;
        }
        if(meetsGeneralRequirements(provisionRequest) && meetsQuantitativeRequirements(provisionRequest)) {
            Collection<SystemComponent> unsupportedReqs = meetsQualitativeRequirements(provisionRequest);
            if(unsupportedReqs.isEmpty()) {
                logger.debug("{} meets qualitative requirements for [{}]", getName(), LoggingUtil.getLoggingName(sElem));
                return true;
            } else {
                /* Create a String representation of the unsupportedReqs
                 * object for logging */
                int x = 0;
                StringBuilder buffer = new StringBuilder();
                for (SystemComponent unsupportedReq : unsupportedReqs) {
                    if (x > 0)
                        buffer.append(", ");
                    buffer.append("[").append(unsupportedReq.toString()).append("]");
                    x++;
                }
                String unsupportedReqsString = buffer.toString();
                logger.debug("{} does not meet requirements for {} service [{}]",
                             getName(), provType, LoggingUtil.getLoggingName(sElem));
                /* Determine if the resource supports persistent provisioning */
                if(!resourceCapability.supportsPersistentProvisioning()) {
                    String failureReason =
                        String.format("Cannot allocate %s service [%s] to %s, required SystemComponents cannot be " +
                                      "provisioned. This is because the %s is not configured for persistentProvisioning. " +
                                      "If you want to enable this feature, verify the %s's configuration for the " +
                                      "org.rioproject.cybernode.persistentProvisioning property is set to true",
                                      provType, LoggingUtil.getLoggingName(sElem), getName(), getName(), getName());
                    provisionRequest.addFailureReason(failureReason);
                    logger.debug(failureReason);
                    return false;
                }
                /*
                 * Check if the unsupported PlatformCapability objects can be
                 * provisioned. If there are any that cannot be provisioned move
                 * onto the next resource
                 */
                boolean provisionableCaps = true;
                for (SystemComponent sysReq : unsupportedReqs) {
                    if (sysReq.getStagedSoftware()==null) {
                        provisionableCaps = false;
                        break;
                    }
                }
                if(!provisionableCaps) {
                    String failureReason = getName() + " does not meet requirements for " +
                            provType + " service " +
                            "[" + LoggingUtil.getLoggingName(sElem) + "] " +
                            unsupportedReqsString;
                    provisionRequest.addFailureReason(failureReason);
                    logger.warn(failureReason);
                    return false;
                }
                /* Get the size of the download(s) */
                int requiredSize = 0;
                IOException failed = null;
                try {
                    for (SystemComponent sysReq : unsupportedReqs) {
                        StagedSoftware download = sysReq.getStagedSoftware();
                        if(download!=null) {
                            int size = download.getDownloadSize();
                            if(size < 0) {
                                logger.warn("Unable to obtain download size for {}, abort provision request",
                                            download.getLocation());
                                requiredSize = size;
                                break;
                            }
                            requiredSize += size;
                            if(download.getPostInstallAttributes() != null &&
                               download.getPostInstallAttributes().getStagedData() != null) {
                                StagedData postInstall = download.getPostInstallAttributes().getStagedData();
                                size = postInstall.getDownloadSize();
                                if(size < 0) {
                                    logger.warn("Unable to obtain download size for PostInstall {}, abort provision request",
                                                postInstall.getLocation());
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
                    throw new ProvisionException("Service ["+LoggingUtil.getLoggingName(sElem)+"] "+
                                                 "instantiation failed",
                                                 failed==null?
                                                 new IOException("Unable to obtain download size"):failed,
                                                 true);
                /* Find out if the resource has the necessary disk-space */
                if(supportsStorageRequirement(requiredSize, resourceCapability.getPlatformCapabilities())) {
                    logger.debug("{} supports provisioning requirements for {} service [{}]",
                                 getName(), provType, LoggingUtil.getLoggingName(sElem));

                    sElem.setProvisionablePlatformCapabilities(unsupportedReqs);
                    return true;
                }
                double avail = getAvailableStorage(resourceCapability.getPlatformCapabilities());
                StringBuilder sb = new StringBuilder();
                sb.append(getName()).append(" ");
                if(avail>0) {
                    /* For logging purposes compute the size in GB */
                    double GB = Math.pow(1024, 3);
                    avail = avail/GB;
                    sb.append("does not have adequate disk-space for ")
                        .append("[")
                        .append(LoggingUtil.getLoggingName(sElem)).append("] ")
                        .append("Required=")
                        .append(+requiredSize).append(", ")
                        .append("Available=").append(avail).append(" GB");
                } else {
                    sb.append("does not report a StorageCapability. ")
                        .append("Rio cannot allocate the ")
                        .append("[")
                        .append(LoggingUtil.getLoggingName(sElem))
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
                String failureReason = sb.toString();
                provisionRequest.addFailureReason(failureReason);
                logger.warn(failureReason);
                return false;
            }
        } else {
            String failureReason =
                String.format("%s does not meet general or quantitative requirements for %s service [%s]",
                              getName(), provType, LoggingUtil.getLoggingName(sElem));
            logger.debug(failureReason);
            return false;
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
    private boolean supportsStorageRequirement(int requestedSize, PlatformCapability[] pCaps) {
        boolean supports = false;
        for (PlatformCapability pCap : pCaps) {
            if (pCap instanceof StorageCapability) {
                StorageCapability storage = (StorageCapability) pCap;
                supports = storage.supports(requestedSize);
                break;
            }
        }
        return supports;
    }

    /**
     * Get the available storage from the StorageCapability
     *
     * @param pCaps Array of PlatformCapability instances to use
     *
     * @return The available storage from the StorageCapability. If a
     * StorageCapability cannot be found return -1
     */
    private double getAvailableStorage(PlatformCapability[] pCaps) {
        double available = -1;
        for (PlatformCapability pCap : pCaps) {
            if (pCap instanceof StorageCapability) {
                StorageCapability storage = (StorageCapability) pCap;
                Double dCap = (Double) storage.getValue(StorageCapability.CAPACITY);
                if (dCap != null) {
                    available = dCap;
                }
                break;
            }
        }
        return available;
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
     * @param provisionRequest The ProvisionRequest
     * @return Return true if the provided ResourceCapability meets
     * general requirements
     */
    boolean meetsGeneralRequirements(final ProvisionRequest provisionRequest) {
        /*
         * If we have a cluster defined, then see if the provided resource has
         * either an IP address or hostname thats in the list of IP addresses
         * and hostnames in our machine cluster list. If it isnt in the list,
         * then there is no sense in proceeding
         */
        ServiceElement sElem = provisionRequest.getServiceElement();
        String[] machineCluster = sElem.getCluster();
        if(machineCluster != null && machineCluster.length > 0) {
            logger.debug("ServiceBean [{}] has a cluster requirement", LoggingUtil.getLoggingName(sElem));
            boolean found = false;
            for (String aMachineCluster : machineCluster) {
                if (aMachineCluster.equals(resourceCapability.getAddress()) ||
                    aMachineCluster.equalsIgnoreCase(resourceCapability.getHostName()))
                    found = true;
            }
            if(!found) {
                StringBuilder builder = new StringBuilder();
                for(String m : machineCluster) {
                    if(builder.length()>0)
                        builder.append(", ");
                    builder.append(m);
                }
                String failureReason = String.format("%s not found in cluster requirement [%s] for [%s]",
                                                     getName(),
                                                     builder.toString(),
                                                     LoggingUtil.getLoggingName(sElem));
                provisionRequest.addFailureReason(failureReason);
                logger.debug(failureReason);
                return false;
            }
        }
        return true;
    }

    /**
     * This method verifies whether the ResourceCapability can support the
     * Qualitative Requirements specified by the ServiceBean
     * 
     * @param request The ProvisionRequest object
     * @return A Collection of SystemRequirement objects which the
     * ResourceCapability does not support. If the Collection has zero entries,
     * then the provided ResourceCapability supports the Qualitative
     * Requirements specified by the ServiceBean
     */
    Collection<SystemComponent> meetsQualitativeRequirements(final ProvisionRequest request) {
        ServiceElement sElem = request.getServiceElement();
        ServiceLevelAgreements sla = sElem.getServiceLevelAgreements();
        SystemComponent[] serviceRequirements = sla.getSystemRequirements().getSystemComponents();
        List<SystemComponent> unsupportedRequirements = new ArrayList<>();
        /*
         * If there are no PlatformCapability requirements we can return
         * successfully
         */
        if(serviceRequirements.length == 0)
            return unsupportedRequirements;

        PlatformCapability[] platformCapabilities = resourceCapability.getPlatformCapabilities();

        List<SystemComponent> operatingSystems = new ArrayList<>();
        List<SystemComponent> architectures = new ArrayList<>();
        List<SystemComponent> machineAddresses = new ArrayList<>();
        List<SystemComponent> remaining = new ArrayList<>();

        for (SystemComponent serviceRequirement : serviceRequirements) {
            if(isOperatingSystem(serviceRequirement)) {
                operatingSystems.add(serviceRequirement);
            } else if(isArchitecture(serviceRequirement)) {
                architectures.add(serviceRequirement);
            } else if(isMachineAddress(serviceRequirement)) {
                machineAddresses.add(serviceRequirement);
            } else {
                remaining.add(serviceRequirement);
            }
        }

        /*
         * Check if we have a match in one of the sought after architectures
         */
        if(!architectures.isEmpty()) {
            ProcessorArchitecture architecture = getArchitecture();
            Result result = check(architecture, architectures);
            if (!result.supported) {
                String failureReason = formatFailureReason(architectures,
                                                           (String)architecture.getCapabilities().get(ProcessorArchitecture.ARCHITECTURE),
                                                           "architecture",
                                                           sElem,
                                                           result.excluded.isEmpty(),
                                                           ProcessorArchitecture.ARCHITECTURE);
                if(logger.isWarnEnabled()) {
                    logger.warn(failureReason);
                }
                request.addFailureReason(failureReason);
                unsupportedRequirements.addAll(architectures);
                return unsupportedRequirements;
            }
        }

        /*
         * Check if we have a match in one of the sought after operating systems
         */
        if(!operatingSystems.isEmpty()) {
            OperatingSystem operatingSystem = getOperatingSystem();
            Result result = check(operatingSystem, operatingSystems);
            if (!result.supported) {
                String failureReason = formatFailureReason(operatingSystems,
                                                           operatingSystem.getCapabilities().get(OperatingSystem.NAME).toString(),
                                                           "operating system",
                                                           sElem,
                                                           result.excluded.isEmpty(),
                                                           OperatingSystem.NAME);
                if(logger.isWarnEnabled()) {
                    logger.warn(failureReason);
                }
                request.addFailureReason(failureReason);
                unsupportedRequirements.addAll(operatingSystems);
                return unsupportedRequirements;
            }
        }
        /*
         * Check if we have a match in one of the sought after machine addresses
         */
        if(!machineAddresses.isEmpty()) {
            TCPConnectivity tcpConnectivity = getTCPConnectivity();
            Result result = check(tcpConnectivity, machineAddresses);
            if (!result.supported) {
                String formattedComponents = formatSystemComponents(machineAddresses,
                                                                    TCPConnectivity.HOST_NAME, TCPConnectivity.HOST_ADDRESS);
                String failureReason;
                if(result.excluded.isEmpty()) {
                    failureReason = String.format("The machine addresses being requested [%s] do not match the " +
                                                  "target resource's machine name/ip [%s/%s] for [%s]",
                                                  formattedComponents,
                                                  tcpConnectivity.getCapabilities().get(TCPConnectivity.HOST_NAME),
                                                  tcpConnectivity.getCapabilities().get(TCPConnectivity.HOST_ADDRESS),
                                                  LoggingUtil.getLoggingName(sElem));
                } else {
                    failureReason = String.format("The target resource's machine name/ip [%s/%s] is on the exclusion list of [%s] for [%s]",
                                                  tcpConnectivity.getCapabilities().get(TCPConnectivity.HOST_NAME),
                                                  tcpConnectivity.getCapabilities().get(TCPConnectivity.HOST_ADDRESS),
                                                  formattedComponents,
                                                  LoggingUtil.getLoggingName(sElem));
                }
                if(logger.isWarnEnabled()) {
                    logger.warn(failureReason);
                }
                request.addFailureReason(failureReason);
                unsupportedRequirements.addAll(machineAddresses);
                return unsupportedRequirements;
            }
        }

        /*
         * Check remaining PlatformCapability objects for supportability
         */
        for (SystemComponent serviceRequirement : remaining) {
            boolean supported = false;
            /*
             * Iterate through all resource PlatformCapability objects and see
             * if any of them supports the current PlatformCapability. If none
             * are found, then we don't have a match
             */
            for (PlatformCapability platformCapability : platformCapabilities) {
                if (platformCapability.supports(serviceRequirement)) {
                    if(serviceRequirement.exclude()) {
                        continue;
                    }
                    supported = true;
                    break;
                }
            }
            if (!supported) {
                unsupportedRequirements.add(serviceRequirement);
            }
        }
        return unsupportedRequirements;
    }

    private Result check(final PlatformCapability platformCapability,
                         final List<SystemComponent> systemComponents) {
        Result result = new Result();
        boolean supported = false;
        for (SystemComponent serviceRequirement : systemComponents) {
            if(serviceRequirement.exclude()) {
                if(platformCapability.supports(serviceRequirement)) {
                    result.excluded.add(serviceRequirement);
                } else {
                    supported = true;
                }
                break;
            } else {
                if(platformCapability.supports(serviceRequirement)) {
                    supported = true;
                    break;
                }
            }

        }
        result.supported = supported;
        return result;
    }

    private static class Result {
        boolean supported;
        List<SystemComponent> excluded = new ArrayList<>();
    }

    private String formatSystemComponents(final List<SystemComponent> systemComponents, final String... keys) {
        StringBuilder builder = new StringBuilder();
        for(String key : keys) {
            for (SystemComponent serviceRequirement : systemComponents) {
                if(builder.length()>0)
                    builder.append(", ");
                String value = (String) serviceRequirement.getAttributes().get(key);
                if(value!=null)
                    builder.append(value);
            }
        }
        return builder.toString();
    }

    private String formatFailureReason(final List<SystemComponent> systemComponents,
                                       final String capability,
                                       final String name,
                                       final ServiceElement sElem,
                                       final boolean notExcluded,
                                       final String... keys) {

        String formattedComponents = formatSystemComponents(systemComponents, keys);
        String failureReason;
        if(notExcluded) {
            failureReason = String.format("The %ss being requested [%s] are not supported by the " +
                                          "target resource's %s [%s] for [%s]",
                                          name,
                                          formattedComponents,
                                          name,
                                          capability,
                                          LoggingUtil.getLoggingName(sElem));
        } else {
            failureReason = String.format("The target resource's %s [%s] is on the exclusion list of [%s] for [%s]",
                                          name,
                                          capability,
                                          formattedComponents,
                                          LoggingUtil.getLoggingName(sElem));
        }
        return failureReason;
    }

    private boolean isOperatingSystem(SystemComponent systemComponent) {
        String name = systemComponent.getName();
        String className = systemComponent.getClassName();
        if(className==null) {
            return name.equals(OperatingSystem.ID);
        }
        return systemComponent.getClassName().equals(OperatingSystem.class.getName());
    }

    private boolean isArchitecture(SystemComponent systemComponent) {
        String name = systemComponent.getName();
        String className = systemComponent.getClassName();
        if(className==null) {
            return name.equals(ProcessorArchitecture.ID);
        }
        return systemComponent.getClassName().equals(ProcessorArchitecture.class.getName());
    }

    private boolean isMachineAddress(SystemComponent systemComponent) {
        String name = systemComponent.getName();
        String className = systemComponent.getClassName();
        if(className==null) {
            return name.equals(TCPConnectivity.ID);
        }
        return systemComponent.getClassName().equals(TCPConnectivity.class.getName());
    }

    /*boolean isHardwareRelated(SystemComponent systemComponent) {
        return isArchitecture(systemComponent) || isOperatingSystem(systemComponent) || isMachineAddress(systemComponent);
    }*/

    private ProcessorArchitecture getArchitecture () {
        ProcessorArchitecture architecture = null;
        for (PlatformCapability platformCapability : resourceCapability.getPlatformCapabilities()) {
            if(platformCapability instanceof ProcessorArchitecture) {
                architecture = (ProcessorArchitecture) platformCapability;
                break;
            }
        }
        return architecture;
    }

    private OperatingSystem getOperatingSystem () {
        OperatingSystem operatingSystem = null;
        for (PlatformCapability platformCapability : resourceCapability.getPlatformCapabilities()) {
            if(platformCapability instanceof OperatingSystem) {
                operatingSystem = (OperatingSystem) platformCapability;
                break;
            }
        }
        return operatingSystem;
    }

    private TCPConnectivity getTCPConnectivity () {
        TCPConnectivity tcpConnectivity = null;
        for (PlatformCapability platformCapability : resourceCapability.getPlatformCapabilities()) {
            if(platformCapability instanceof TCPConnectivity) {
                tcpConnectivity = (TCPConnectivity) platformCapability;
                break;
            }
        }
        return tcpConnectivity;
    }

    /**
     * This method verifies whether the ResourceCapability can support the
     * Quantitative Requirements specified by the ServiceBean
     * 
     * @param provisionRequest The ProvisionRequest
     * @return Return true if the provided ResourceCapability meets
     * Quantitative requirements
     */
    boolean meetsQuantitativeRequirements(final ProvisionRequest provisionRequest) {
        ServiceElement sElem = provisionRequest.getServiceElement();
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
            StringBuilder message = new StringBuilder();
            message.append(getName()).append(" ");
            if(measured==null) {
                message.append("has a [null] MeasuredCapability instance, ServiceBean [");
                message.append(LoggingUtil.getLoggingName(sElem)).append("] ");
                message.append("has a requirement to test ").append(systemThresholdIDs.length);
            } else {
                message.append("only has [").append(measured.length).append("] MeasuredCapability instances, ");
                message.append("ServiceBean [").append(LoggingUtil.getLoggingName(sElem)).append("] ");
                message.append("has a requirement to test [").append(systemThresholdIDs.length).append("]");
            }
            provisionRequest.addFailureReason(message.toString());
            logger.debug(message.toString());
            return false;
        }
        /*
         * Check each of the MeasuredResource objects
         */

        for (String systemThresholdID : systemThresholdIDs) {
            boolean supported = false;
            ThresholdValues systemThreshold = sla.getSystemRequirements().getSystemThresholdValue(systemThresholdID);
            if (systemThresholdID.equals(SystemRequirements.SYSTEM)) {
                double systemUtilization = systemThreshold.getHighThreshold();
                if (systemUtilization < resourceCapability.getUtilization()) {
                    String failureReason =
                        String.format("%s cannot meet system utilization requirement. Desired: %f, Actual: %f",
                                      getName(),
                                      systemUtilization,
                                      resourceCapability.getUtilization());
                    provisionRequest.addFailureReason(failureReason);
                    logger.debug(failureReason);
                    return (false);
                } else {
                    supported = true;
                    logger.debug("[System] utilization requirement met. Desired {}, Actual {}",
                                 systemUtilization, resourceCapability.getUtilization());
                }
            }
            /*
             * Iterate through all resource MeasuredResource objects and see if
             * any of them supports the current MeasuredResource. If none are
             * found, then we don't have a match
             */
            for (MeasuredResource mRes : measured) {
                if (mRes.getIdentifier().equals(systemThresholdID)) {
                    if (mRes.evaluate(systemThreshold)) {
                        supported = true;
                        logger.debug("{} meets [{}] utilization requirement. Desired Low: {}, High: {}, Actual: {}",
                                     getName(),
                                     systemThresholdID,
                                     systemThreshold.getLowThreshold(),
                                     systemThreshold.getHighThreshold(),
                                     mRes.getValue());
                        break;
                    } else {
                        String failureReason =
                            String.format("%s cannot meet [%s], utilization requirement. Desired Low: %f, High: %f, Actual: %f",
                                          getName(),
                                          systemThresholdID,
                                          systemThreshold.getLowThreshold(),
                                          systemThreshold.getHighThreshold(),
                                          mRes.getValue());
                        provisionRequest.addFailureReason(failureReason);
                        logger.debug(failureReason);
                    }
                }
            }

            if (!supported) {
                provisionable = false;
                break;
            }
        }
        return provisionable;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        InstantiatorResource that = (InstantiatorResource) o;
        return instantiatorUuid.equals(that.instantiatorUuid);

    }

    @Override
    public int hashCode() {
        return instantiatorUuid.hashCode();
    }

}
