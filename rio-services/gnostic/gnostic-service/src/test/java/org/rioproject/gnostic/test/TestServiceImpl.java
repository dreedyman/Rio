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
package org.rioproject.gnostic.test;

import org.rioproject.core.jsb.ServiceBeanContext;
import org.rioproject.watch.GaugeWatch;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 */
public class TestServiceImpl implements TestService {
    GaugeWatch loadWatch;
    Long instanceID;
    String name;
    Status status = Status.ALLOWED;
    private AtomicInteger notificationCount = new AtomicInteger();
    private AtomicInteger rhsExecutedCount = new AtomicInteger();
    private GaugeWatch notification;
    private Logger logger = LoggerFactory.getLogger(TestServiceImpl.class.getName());

    public void setServiceBeanContext(ServiceBeanContext context) {
        loadWatch = new GaugeWatch("load");
        context.getWatchRegistry().register(loadWatch);
        instanceID = context.getServiceBeanConfig().getInstanceID();
        name = context.getServiceElement().getName();

        notification = new GaugeWatch("notification");
        context.getWatchRegistry().register(notification);
    }

    public double getLoad() {
        return loadWatch.getLastCalculableValue();  
    }

    public void setLoad(double load) {
        double last = loadWatch.getLastCalculableValue();
        loadWatch.addValue(load);
        boolean verified = loadWatch.getLastCalculableValue() == load;
        if (!verified)
            logger.warn("---> ["+instanceID+"] was [" + loadWatch.getLastCalculableValue() +
                           "], SET FAILED [" + load + "] " +
                           "breached=" +
                           loadWatch.getThresholdManager().getThresholdCrossed());
        else
            logger.info("---> ["+name+"-"+instanceID+"] Load now [" + load + "] " +
                        "breached=" +
                        loadWatch.getThresholdManager().getThresholdCrossed());
    }

    public void setStatus(Status status) {
        this.status = status;
    }
    
    public Status getStatus() {
        return status; 
    }

    public void sendNotify() {
        notification.addValue(notificationCount.incrementAndGet());
    }

    public void executedRHS() throws IOException {
        rhsExecutedCount.incrementAndGet();
    }

    public int getRHSExecutedCount() {
        return rhsExecutedCount.get();
    }

    public int getNotificationCount() {
        return notificationCount.get();
    }
}
