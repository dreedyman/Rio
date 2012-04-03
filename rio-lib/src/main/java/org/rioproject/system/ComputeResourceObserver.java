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
package org.rioproject.system;

import org.rioproject.core.provision.SystemRequirements;
import org.rioproject.core.jsb.ServiceBeanContext;
import org.rioproject.deploy.ServiceBeanInstance;
import org.rioproject.event.EventHandler;
import org.rioproject.watch.Calculable;
import org.rioproject.watch.ThresholdValues;
import org.rioproject.sla.SLAThresholdEvent;
import org.rioproject.sla.SLA;
import org.rioproject.sla.ServiceLevelAgreements;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The ComputeResourceObserver observes the ComputeResource object associated with a 
 * ServiceBean. It provides the following function:
 * <p>
 * As the ComputeResource object changes, it's overall utilization will be compared
 * to the maximum utilization that may have been specified by the ServiceBean. If 
 * that cost exceeds the maximum utilization specified, then a SLAThresholdEvent will 
 * be fired
 *
 * @author Dennis Reedy
 */
public class ComputeResourceObserver implements ResourceCapabilityChangeListener {
    /**
     * The ComputeResource the ComputeResourceObserver is observing
     */
    private ComputeResource computeResource;
    /**
     * If true, tells the ComputeResourceObserver to ignore any changes from 
     * the ComputeResource
     */
    private boolean ignore=false;
    /** 
     * Service source object 
     */
    private Object source;
    /**
     * The EventHandler used to fire SLAThresholdEvent objects
     */
    private EventHandler eventHandler;
    /** 
     * The ServiceBeanContext for the service bean
     */
    private ServiceBeanContext context;
    /** 
     * Maximum cost of ComputeResource 
     */
    private double utilizationLimit;
    /** 
     * Whether the ComputeResource cost has been breached 
     */
    private boolean computeCostBreached=false;
    /** 
     * A SLA constructed to represent the ComputeResource utilizationLimit 
     */
    private SLA utilizationSLA;
    /** A Logger for this component */
    private static Logger logger = Logger.getLogger("org.rioproject.system");

    /**
     * Construct a new ComputeResourceObserver
     * 
     * @param computeResource The ComputeResource to observe
     * @param context The ServiceBeanContext for the service bean
     * @param eventHandler The EventHandler for firing SLAThresholdEvents
     */
    public ComputeResourceObserver(ComputeResource computeResource,
                                   ServiceBeanContext context,
                                   EventHandler eventHandler) {
        if(computeResource==null)
            throw new IllegalArgumentException("computeResource is null");
        if(context==null)
            throw new IllegalArgumentException("context is null");
        if(eventHandler==null)
            throw new IllegalArgumentException("eventHandler is null");

        this.computeResource = computeResource;
        this.eventHandler = eventHandler;
        this.context = context;
        ServiceLevelAgreements slas =
            context.getServiceElement().getServiceLevelAgreements();
        utilizationLimit = 1.0;
        if(slas!=null) {
            String[] systemThresholdIDs =
                slas.getSystemRequirements().getSystemThresholdIDs();
            for (String systemThresholdID : systemThresholdIDs) {
                if (systemThresholdID.equals(SystemRequirements.SYSTEM)) {
                    ThresholdValues systemThreshold =
                        slas.getSystemRequirements()
                            .getSystemThresholdValue(systemThresholdID);
                    utilizationLimit = systemThreshold.getHighThreshold();
                    break;
                }
            }            
        }
        utilizationSLA = new SLA("ComputeResource", 0.0, utilizationLimit);
        computeResource.addListener(this);
    }    

    /**
     * Inform the ComputeResource object that this class is no longer interested 
     * in updates
     */
    public void disconnect() {
        computeResource.removeListener(this);
    }

    /**
     * Set the event source
     * 
     * @param source The event source
     */
    public void setSource(Object source) {
        this.source = source;
    }

    /**
     * Set the ignore property, telling the ComputeResourceObserver to ignore
     * any changes from the ComputeResource
     * 
     * @param ignore If true, ignore changes to the ComputeResource
     */
    public void setIgnore(boolean ignore) {
        this.ignore = ignore;
    }

    @Override
    public void update(ResourceCapability resourceCapability) {
        if(ignore)
            return;
        try {
            double currentUtilization = resourceCapability.getUtilization();
            /*
             Description for this utility
            */
            String DESCRIPTION = "ComputeResource Utilization Listener";
            if(currentUtilization > utilizationLimit && !computeCostBreached) {
                computeCostBreached = true;
                long now = System.currentTimeMillis();
                Calculable calc = new Calculable("ComputeResource.Utilization", currentUtilization, now);
                try {
                    String hostAddress = computeResource.getAddress().getHostAddress();
                    ServiceBeanInstance instance = context.getServiceBeanManager().getServiceBeanInstance();

                    SLAThresholdEvent event = new SLAThresholdEvent(source,
                                                                    context.getServiceElement(),
                                                                    instance,
                                                                    calc,
                                                                    utilizationSLA,
                                                                    DESCRIPTION,
                                                                    hostAddress,
                                                                    SLAThresholdEvent.BREACHED);
                    new Thread(new SLAThresholdEventTask(event)).start();
                } catch(Throwable t) {
                    if(t.getCause()!=null)
                        t = t.getCause();
                    logger.log(Level.WARNING,
                               "["+context.getServiceElement().getName()+"] " +
                               "Creating SLAThresholdEvent.BREACHED. " +
                               "Current =["+currentUtilization+"], " +
                               "Threshold=["+utilizationLimit+"]",
                               t);
                }
            }
            if(currentUtilization < utilizationLimit && computeCostBreached) {
                computeCostBreached = false;
                long now = System.currentTimeMillis();
                Calculable calc = new Calculable("ComputeResource.Utilization", currentUtilization, now);
                try {
                    String hostAddress = computeResource.getAddress().getHostAddress();
                    ServiceBeanInstance instance = context.getServiceBeanManager().getServiceBeanInstance();

                    SLAThresholdEvent event = new SLAThresholdEvent(source,
                                                                    context.getServiceElement(),
                                                                    instance,
                                                                    calc,
                                                                    utilizationSLA,
                                                                    DESCRIPTION,
                                                                    hostAddress,
                                                                    SLAThresholdEvent.CLEARED);
                    new Thread(new SLAThresholdEventTask(event)).start();
                } catch(Throwable t) {
                    logger.log(Level.WARNING,
                               "["+context.getServiceElement().getName()+"] " +
                               "Creating SLAThresholdEvent.CLEARED. " +
                               "Current =["+currentUtilization+"], " +
                               "Threshold=["+utilizationLimit+"]",
                               t);
                }
            }            
        } catch(Throwable t) {
            if(t.getCause()!=null)
                t = t.getCause();
            logger.log(Level.WARNING, "Processing notify from ComputeResource", t);
        }
    }

    /**
     * This class is used to notify registered event consumers of a SLAThresholdEvent
     */
    class SLAThresholdEventTask implements Runnable {
        SLAThresholdEvent event;

        SLAThresholdEventTask(SLAThresholdEvent event) {
            this.event = event;
        }

        public void run() {
            try {
                eventHandler.fire(event);
            } catch(Exception e) {
                logger.log(Level.WARNING, "Exception notifying SLAThresholdEvent consumers", e);
            }

        }
    }
}
