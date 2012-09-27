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
import org.rioproject.deploy.DeploymentMap;
import org.rioproject.deploy.ServiceBeanInstance;
import org.rioproject.opstring.ClassBundle;
import org.rioproject.opstring.OperationalStringManager;
import org.rioproject.opstring.ServiceElement;
import org.rioproject.deploy.DeployedService;
import org.rioproject.entry.OperationalStringEntry;
import org.rioproject.opstring.OpStringManagerProxy;
import org.rioproject.sla.SLA;
import org.rioproject.system.ComputeResourceUtilization;
import org.rioproject.system.MeasuredResource;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

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
    private final List<ServiceCapability<T>> services = new ArrayList<ServiceCapability<T>>();
    private OperationalStringManager opMgr;
    /** Scheduler for Cybernode utilization gathering */
    private ScheduledExecutorService scheduler;
    private static Logger logger = Logger.getLogger(Utilization.class.getName());

    @Override
    public void setAssociation(final Association<T> association) {
        this.association = association;
        initialize(association.getOperationalStringName());
    }

    public void setSLA(final SLA sla) {
        this.sla = sla;
    }

    public T getService() {
        T service = null;
        synchronized(services) {
            ServiceCapability<T> selected = null;
            /* Fail-Over
            for(ServiceCapability<T> sc : getServices()) {
                if(sc.isInvokable()) {
                    service = sc.getService();
                    break;
                } else {
                    services.remove(sc);
                    services.add(sc);
                }
            }
             */
            /*  Round-Robin */
            for(ServiceCapability<T> sc : getServices()) {
                if(sc.isInvokable()) {
                    services.remove(sc);
                    services.add(sc);
                    selected = sc;
                    service = sc.getService();
                    break;
                }
            }
            if(logger.isLoggable(Level.FINEST)) {
                String name = association==null?"<unknown>":association.getName();
                if(selected!=null) {
                    String address = selected.cru==null?"<unknown>":selected.cru.getAddress();
                    String util = selected.cru==null?"<unknown>":selected.cru.getUtilization().toString();
                    logger.finest("Using associated service " +
                                  "["+name+"] at "+
                                  "Host address="+address+", "+
                                  "Utilization="+
                                  util+", " +
                                  "values="+selected.getMeasuredResourcesAsList());
                } else {
                    logger.finest("All services are either breached, or " +
                                  "none are available for associated " +
                                  "service ["+name+"]");
                }
            }
        }
        return service;
    }

    @Override
    public void serviceAdded(final T service) {
        ServiceItem item = association.getServiceItem(service);
        if(item!=null) {
            addService(item);
        } else {
            logger.warning("Unable to obtain ServiceItem for " + service + ", force refresh all service instances");
            synchronized(services) {
                services.clear();
                for(ServiceItem serviceItem : association.getServiceItems())
                    addService(serviceItem);
            }
        }
    }

    @Override
    public void serviceRemoved(final T service) {
        if(removeService(service)){
            if(logger.isLoggable(Level.FINE))
                logger.fine("Service removed, " +
                            "service collection size="+services.size());
        }
    }

    @Override
    public void terminate() {        
        if(scheduler!=null) {
            scheduler.shutdownNow();
        }
        if(opMgr!=null)
            ((OpStringManagerProxy.OpStringManager)opMgr).terminate();
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
            } catch (RemoteException e) {
                logger.log(Level.WARNING,
                           "Getting ServiceElement for ["+association.getName()+"]",
                           e);
            } catch (Exception e) {
                logger.log(Level.WARNING,
                           "Unable to create OpStringManagerProxy " +
                           "for associated service ["+association.getName()+"]",
                           e);
            }
        }
    }

    private void setServiceList(final ServiceItem[] items) {
        synchronized(services) {
            services.clear();
            for(ServiceItem item : items) {
                addService(item);
            }
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

        synchronized(services) {
            boolean alreadyHaveIt = false;
            for(ServiceCapability<T> sc : services) {
                if(sc.uuid.equals(uuid)) {
                    alreadyHaveIt = true;
                    break;
                }
            }
            if(!alreadyHaveIt) {
                services.add(new ServiceCapability(item.service, uuid));
            }
        }
    }

    private boolean removeService(final T service) {
        boolean removed = false;
        for(ServiceCapability sc : getServices()) {
            if(sc.getService().equals(service)) {
                synchronized(services) {
                    removed = services.remove(sc);
                }
            }
        }
        return removed;
    }

    @SuppressWarnings("unchecked")
    private ServiceCapability<T>[] getServices() {
        ServiceCapability<T>[] scArray;
        synchronized(services) {
            scArray =  services.toArray(new ServiceCapability[services.size()]);
        }
        return scArray;
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
                if(logger.isLoggable(Level.FINEST))
                    logger.finest("ComputeResourceUtilizationFetcher, " +
                                  "obtaining DeploymentMap for " +
                                  "["+opStringName+"]");
                DeploymentMap dMap = opMgr.getDeploymentMap();
                if(serviceElements.isEmpty()) {
                    serviceElements.addAll(getMatchingServiceElements(dMap));
                    if(serviceElements.isEmpty())
                        logger.warning("Unable to obtain matching ServiceElement(s) " +
                                       "for associated service ["+association.getName()+"]");
                }
                if(dMap!=null) {
                    for(ServiceElement elem : serviceElements)
                        list.addAll(dMap.getDeployedServices(elem));
                }
            } catch (RemoteException e) {
                logger.log(Level.WARNING,
                           "Getting utilization for service " +
                           "["+association.getAssociationDescriptor()+"], terminating",
                           e);
                terminate();
            }
            synchronized(services) {
                for(DeployedService deployed : list) {
                    ServiceBeanInstance sbi = deployed.getServiceBeanInstance();
                    ComputeResourceUtilization cru =
                        deployed.getComputeResourceUtilization();
                    for(ServiceCapability sc : services) {
                        if(sc.uuid.equals(sbi.getServiceBeanID())) {
                            if(logger.isLoggable(Level.FINEST))
                                logger.finest("Obtained ComputeResourceUtilization for " +
                                              "["+association.getName()+"]");
                            sc.setComputeResourceUtilization(cru);
                            break;
                        }
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
                isInvokable =
                    (cru == null || cru.measuredResourcesWithinRange());
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
                        if(logger.isLoggable(Level.FINE)) {
                            List<MeasuredResource> breached =
                                new ArrayList<MeasuredResource>();
                            for(MeasuredResource mRes : cru.getMeasuredResources()) {
                                if(mRes.thresholdCrossed()) {
                                    breached.add(mRes);
                                }
                            }
                            logger.fine("Associated service at "+
                                        "Host address="+ cru.getAddress()+", "+
                                        "Utilization="+ cru.getUtilization()+" " +
                                        "has breached resources: "+breached);
                        }
                    }
                }
            }
            
            if(isInvokable && wasBreached) {
                if(logger.isLoggable(Level.FINE))
                    logger.fine("Associated service at "+
                                "Host address="+ cru.getAddress()+", "+
                                "Utilization="+ cru.getUtilization()+" " +
                                "was breached, now invokable");
            }
            wasBreached = !isInvokable;

            return isInvokable;
        }
    }
}
