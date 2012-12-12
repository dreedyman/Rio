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
package org.rioproject.cybernode;

import org.rioproject.deploy.ServiceBeanInstance;
import org.rioproject.event.EventHandler;
import org.rioproject.opstring.ServiceElement;
import org.rioproject.sla.SLA;
import org.rioproject.sla.SLAThresholdEvent;
import org.rioproject.system.SystemWatchID;
import org.rioproject.watch.Calculable;
import org.rioproject.watch.ThresholdListener;
import org.rioproject.watch.ThresholdType;
import org.rioproject.watch.ThresholdValues;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * If thresholds get crossed for memory related MeasurableCapability components, this class
 * will either request immediate garbage collection or (in the case of perm gen)
 * release as a provisionable resource.
 *
 * @author Dennis Reedy
 */
public class ComputeResourcePolicyHandler implements ThresholdListener {
    private final EventHandler thresholdEventHandler;
    private final ServiceElement serviceElement;
    private final Executor thresholdTaskPool = Executors.newCachedThreadPool();
    private final ServiceConsumer serviceConsumer;
    private final ServiceBeanInstance instance;
    private final AtomicBoolean terminate = new AtomicBoolean(false);
    private static final Logger logger = LoggerFactory.getLogger(ComputeResourcePolicyHandler.class.getName());

    public ComputeResourcePolicyHandler(final ServiceElement serviceElement,
                                        final EventHandler thresholdEventHandler,
                                        final ServiceConsumer serviceConsumer,
                                        final ServiceBeanInstance instance) {
        this.serviceElement = serviceElement;
        this.thresholdEventHandler = thresholdEventHandler;
        this.instance = instance;
        this.serviceConsumer = serviceConsumer;
    }


    public void terminate() {
        terminate.set(true);
    }

    public void notify(Calculable calculable, ThresholdValues thresholdValues, ThresholdType type) {
        if(terminate.get())
            return;
        String status = type.name().toLowerCase();
        logger.debug("Threshold={}, Status={}, Value={}, Low={}, High={}",
                     calculable.getId(),
                     status,
                     calculable.getValue(),
                     thresholdValues.getLowThreshold(),
                     thresholdValues.getHighThreshold());

        if(type==ThresholdType.BREACHED)  {
            double tValue = calculable.getValue();
            if(tValue>thresholdValues.getCurrentHighThreshold()) {
                if(serviceConsumer!=null) {
                    serviceConsumer.updateMonitors();
                }
                if(calculable.getId().equals(SystemWatchID.JVM_MEMORY)) {
                    logger.warn("Memory utilization is {}, threshold set at {}, request immediate garbage collection",
                                calculable.getValue(), thresholdValues.getCurrentHighThreshold());
                    System.gc();
                }
                if(calculable.getId().contains(SystemWatchID.JVM_PERM_GEN)) {
                    logger.warn("Perm Gen has breached with utilization > {}", thresholdValues.getCurrentHighThreshold());
                    //if(isEnlisted())
                    //release(false);
                    //svcConsumer.cancelRegistrations();
                }

            }
        } else if(type== ThresholdType.CLEARED) {
            if(serviceConsumer!=null) {
                serviceConsumer.updateMonitors();
            }
                }

        try {
            double[] range = new double[]{thresholdValues.getCurrentLowThreshold(),
                                          thresholdValues.getCurrentHighThreshold()};

            SLA sla = new SLA(calculable.getId(), range);
            SLAThresholdEvent event = new SLAThresholdEvent(instance.getService(),
                                                            serviceElement,
                                                            instance,
                                                            calculable,
                                                            sla,
                                                            serviceElement.getName()+" Resource Policy Handler",
                                                            instance.getHostAddress(),
                                                            type);
            thresholdTaskPool.execute(new SLAThresholdEventTask(event, thresholdEventHandler));
        } catch(Exception e) {
            logger.warn("Could not send a SLAThresholdEvent as a result of compute resource threshold [{}] being crossed",
                        calculable.getId(),
                        e);
        }
    }
}
