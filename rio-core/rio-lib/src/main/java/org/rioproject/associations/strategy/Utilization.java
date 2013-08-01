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
package org.rioproject.associations.strategy;

import net.jini.core.entry.Entry;
import net.jini.core.lookup.ServiceItem;
import net.jini.id.ReferentUuid;
import net.jini.id.Uuid;
import net.jini.id.UuidFactory;
import org.rioproject.associations.Association;
import org.rioproject.associations.AssociationDescriptor;
import org.rioproject.deploy.DeployedService;
import org.rioproject.deploy.DeploymentMap;
import org.rioproject.deploy.ServiceBeanInstance;
import org.rioproject.entry.OperationalStringEntry;
import org.rioproject.opstring.ClassBundle;
import org.rioproject.opstring.OpStringManagerProxy;
import org.rioproject.opstring.OperationalStringManager;
import org.rioproject.opstring.ServiceElement;
import org.rioproject.sla.SLA;
import org.rioproject.system.ComputeResourceUtilization;
import org.rioproject.system.MeasuredResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * A round-robin selector that selects services running on compute resources
 * whose system resources are not depleted. System resource depletion is
 * determined by {@link org.rioproject.system.MeasuredResource} provided as part of
 * the {@link org.rioproject.system.ResourceCapability} object returned as part
 * of the deployment map.
 *
 * @author Dennis Reedy
 */
public class Utilization<T> extends AbstractServiceSelectionStrategy<T> {
    private SLA sla;
    private final List<ServiceCapability<T>> services = Collections.synchronizedList(new ArrayList<ServiceCapability<T>>());
    private OperationalStringManager opMgr;
    /** Scheduler for Cybernode utilization gathering */
    private ScheduledExecutorService scheduler;
    private static Logger logger = LoggerFactory.getLogger(Utilization.class.getName());

    @Override
    public void setAssociation(final Association<T> association) {
        this.association = association;
        initialize(association.getOperationalStringName());
    }

    @SuppressWarnings("unused")
    public void setSLA(final SLA sla) {
        this.sla = sla;
    }

    public T getService() {
        T service = null;
        ServiceCapability<T> selected = null;
        /*  Round-Robin */
        for(ServiceCapability<T> sc : getServices()) {
            if(sc==null)
                continue;
            if(sc.isInvokable()) {
                services.remove(sc);
                services.add(sc);
                selected = sc;
                service = sc.getService();
                break;
            }
        }
        if(logger.isTraceEnabled()) {
            String name = association==null?"<unknown>":association.getName();
            if(selected!=null) {
                String address = selected.cru==null?"<unknown>":selected.cru.getAddress();
                String util = selected.cru==null?"<unknown>":selected.cru.getUtilization().toString();
                logger.trace("Using associated service [{}] at Host address={}, Utilization={}, values={}",
                             name, address, util, selected.getMeasuredResourcesAsList().toString());
            } else {
                logger.trace("All services are either breached, or none are available for associated service [{}]", name);
            }
        }
        return service;
    }

    @Override
    public void serviceAdded(final T service) {
        logger.trace("Adding service for {}, {}", association.getName(), service);
        ServiceItem item = association.getServiceItem(service);
        logger.trace("Found ServiceItem?, {}", item);
        if(item!=null) {
            addService(item);
        } else {
            logger.warn("Unable to obtain ServiceItem for {}, force refresh all service instances", service.toString());
            services.clear();
            for(ServiceItem serviceItem : association.getServiceItems())
                addService(serviceItem);
        }
    }

    @Override
    public void serviceRemoved(final T service) {
        if(removeService(service)){
            logger.debug("Service removed, service collection size={}", services.size());
        }
    }

    @Override
    public void terminate() {        
        if(scheduler!=null) {
            scheduler.shutdownNow();
        }
        if(opMgr!=null) {
            try {
                ((OpStringManagerProxy.OpStringManager)opMgr).terminate();
            } catch(IllegalStateException e) {
                logger.warn("Terminating the Utilization strategy for associated service {}", association.getName(), e);
            }
        }
    }

    private void initialize(final String opStringName) {
        String opStringNameToUse = opStringName;
        if(opStringNameToUse==null) {
            ServiceItem item = association.getServiceItem();
            if(item==null)
                return;
            for(Entry e : item.attributeSets) {
                if(e instanceof OperationalStringEntry) {
                    opStringNameToUse = ((OperationalStringEntry)e).name;
                    break;
                }
            }
        }
        if(opStringNameToUse!=null && opMgr==null && association.getServiceItem()!=null) {
            try {
                opMgr = OpStringManagerProxy.getProxy(opStringNameToUse, null);
                ComputeResourceUtilizationFetcher cruf = new ComputeResourceUtilizationFetcher(opMgr,
                                                                                               opStringNameToUse);
                setServiceList(association.getServiceItems());
                long initialDelay = 0;
                long period = 1000*5;                
                scheduler = Executors.newSingleThreadScheduledExecutor();
                scheduler.scheduleAtFixedRate(cruf, initialDelay, period, TimeUnit.MILLISECONDS);
                logger.trace("Initialized");
            } catch (RemoteException e) {
                logger.warn("Getting ServiceElement for [{}]", association.getName(), e);
            } catch (Exception e) {
                logger.warn("Unable to create OpStringManagerProxy for associated service [{}]", association.getName(), e);
            }
        } else {
            logger.warn("Unable to initialize successfully. opStringNameToUse: {}, opMgr: {}, association.getServiceItem(): {}",
                        opStringNameToUse, opMgr, association.getServiceItem());
        }
    }

    private void setServiceList(final ServiceItem[] items) {
        services.clear();
        for(ServiceItem item : items) {
            addService(item);
        }
    }

    @SuppressWarnings("unchecked")
    private synchronized void addService(final ServiceItem item) {
        if(opMgr==null) {
            String opStringName = null;
            for(Entry e : item.attributeSets) {
                if(e instanceof OperationalStringEntry) {
                    opStringName = ((OperationalStringEntry)e).name;
                    break;
                }
            }
            initialize(opStringName);
        }
        Uuid uuid;
        if(item.service instanceof ReferentUuid)
            uuid = ((ReferentUuid)item.service).getReferentUuid();
        else {
            uuid = UuidFactory.create(item.serviceID.getMostSignificantBits(),
                                      item.serviceID.getLeastSignificantBits());
        }

        boolean alreadyHaveIt = false;
        for(ServiceCapability<T> sc : services) {
            if(sc.uuid.equals(uuid)) {
                alreadyHaveIt = true;
                break;
            }
        }

        if(!alreadyHaveIt) {
            services.add(new ServiceCapability(item.service, uuid));
            logger.trace("Adding new ServiceCapability, service count now {}", services.size());
        } else {
            logger.trace("Already have {}, service count now {}", item, services.size());
        }
    }

    private boolean removeService(final T service) {
        boolean removed = false;
        for(ServiceCapability sc : getServices()) {
            if(sc.getService().equals(service)) {
                removed = services.remove(sc);
            }
        }
        return removed;
    }

    @SuppressWarnings("unchecked")
    private ServiceCapability<T>[] getServices() {
        return services.toArray(new ServiceCapability[services.size()]);
    }

    class ComputeResourceUtilizationFetcher implements Runnable {
        final OperationalStringManager opMgr;
        final String opStringName;
        final List<DeployedService> list = new ArrayList<DeployedService>();
        final List<ServiceElement> serviceElements = new ArrayList<ServiceElement>();

        ComputeResourceUtilizationFetcher(final OperationalStringManager opMgr,
                                          final String opStringName) {
            this.opMgr = opMgr;
            this.opStringName = opStringName;
        }

        public void run() {
            list.clear();
            try {
                logger.trace("ComputeResourceUtilizationFetcher, obtaining DeploymentMap for [{}]", opStringName);
                DeploymentMap dMap = opMgr.getDeploymentMap();
                if(serviceElements.isEmpty()) {
                    serviceElements.addAll(getMatchingServiceElements(dMap));
                    if(serviceElements.isEmpty())
                        logger.warn("Unable to obtain matching ServiceElement(s) for associated service [{}]",
                                    association.getName());
                }
                if(dMap!=null) {
                    for(ServiceElement elem : serviceElements)
                        list.addAll(dMap.getDeployedServices(elem));
                }
            } catch (RemoteException e) {
                logger.warn("Getting utilization for service [{}], terminating", association.getAssociationDescriptor(), e);
                terminate();
            }
            for(DeployedService deployed : list) {
                ServiceBeanInstance sbi = deployed.getServiceBeanInstance();
                ComputeResourceUtilization cru =
                    deployed.getComputeResourceUtilization();
                for(ServiceCapability sc : services) {
                    if(sc.uuid.equals(sbi.getServiceBeanID())) {
                        logger.trace("Obtained ComputeResourceUtilization for [{}]", association.getName());
                        sc.setComputeResourceUtilization(cru);
                        break;
                    }
                }
            }
        }

        private List<ServiceElement> getMatchingServiceElements(final DeploymentMap dMap) {
            List<ServiceElement> matching = new ArrayList<ServiceElement>();
            AssociationDescriptor ad = association.getAssociationDescriptor();
            String[] adInterfaces = ad.getInterfaceNames();
            Arrays.sort(adInterfaces);
            for(ServiceElement elem : dMap.getServiceElements()) {
                List<String> list = new ArrayList<String>();
                for(ClassBundle cb : elem.getExportBundles()) {
                    list.add(cb.getClassName());
                }
                String[] cbInterfaces = list.toArray(new String[list.size()]);
                if(Arrays.equals(adInterfaces, cbInterfaces)) {
                    if(ad.matchOnName()) {
                        if(ad.getName().equals(elem.getName())) {
                            matching.add(elem);
                        }
                    } else {
                        matching.add(elem);
                    }
                }
            }
            return matching;
        }
    }

    class ServiceCapability<T> {
        final T service;
        final Uuid uuid;
        ComputeResourceUtilization cru;
        boolean wasBreached = false;
        final Object updateLock = new Object();

        ServiceCapability(final T service, final Uuid uuid) {
            this.service = service;
            this.uuid = uuid;
        }

        T getService() {
            return service;
        }

        void setComputeResourceUtilization(final ComputeResourceUtilization cru) {
            synchronized(updateLock) {
                this.cru = cru;
            }
        }

        List<MeasuredResource> getMeasuredResourcesAsList() {
            List<MeasuredResource> list = new ArrayList<MeasuredResource>();
            if(cru!=null) {
                synchronized(updateLock) {
                    list.addAll(cru.getMeasuredResources());
                }
            }
            return list;
        }

        boolean isInvokable() {
            boolean isInvokable;
            synchronized(updateLock) {
                isInvokable = (cru == null || cru.measuredResourcesWithinRange());
                if(cru !=null) {
                    if(isInvokable) {
                        for(MeasuredResource mRes : cru.getMeasuredResources()) {
                            if(sla!=null &&
                               sla.getIdentifier().equals(mRes.getIdentifier())) {
                                isInvokable = mRes.evaluate(sla);
                                break;
                            }
                        }

                    } else {
                        if(logger.isDebugEnabled()) {
                            List<MeasuredResource> breached = new ArrayList<MeasuredResource>();
                            for(MeasuredResource mRes : cru.getMeasuredResources()) {
                                if(mRes.thresholdCrossed()) {
                                    breached.add(mRes);
                                }
                            }
                            logger.debug("Associated service at Host address={}, Utilization={} has breached resources: {}",
                                         cru.getAddress(), cru.getUtilization(), breached.toString());
                        }
                    }
                    if(isInvokable && wasBreached) {
                        logger.debug("Associated service at Host address={}, Utilization={} was breached, now invokable",
                                     cru.getAddress(), cru.getUtilization());
                    }
                }
            }
            wasBreached = !isInvokable;

            return isInvokable;
        }
    }
}
