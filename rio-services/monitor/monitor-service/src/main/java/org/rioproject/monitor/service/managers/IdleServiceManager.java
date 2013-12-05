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
package org.rioproject.monitor.service.managers;

import org.rioproject.admin.ServiceActivityProvider;
import org.rioproject.impl.util.ThrowableUtil;
import org.rioproject.monitor.service.channel.ServiceChannel;
import org.rioproject.monitor.service.channel.ServiceChannelEvent;
import org.rioproject.monitor.service.util.LoggingUtil;
import org.rioproject.opstring.ServiceElement;
import org.rioproject.util.TimeUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Monitors idle services.
 *
 * @author Dennis Reedy
 */
public class IdleServiceManager {
    private final Map<ServiceActivityProvider, Long> activityMap = new ConcurrentHashMap<ServiceActivityProvider, Long>();
    private final Long maxIdleTime;
    private final ServiceElement serviceElement;
    private final ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(1);
    private static final Logger logger = LoggerFactory.getLogger(IdleServiceManager.class);

    public IdleServiceManager(final Long maxIdleTime, final ServiceElement serviceElement) {
        this.maxIdleTime = maxIdleTime;
        this.serviceElement = serviceElement;
        long delay = TimeUtil.computeLeaseRenewalTime(maxIdleTime);
        if(logger.isDebugEnabled()) {
            logger.debug("Service [{}] idle time: {}, computed delay time for checking idle status: {}.",
                         LoggingUtil.getLoggingName(serviceElement), TimeUtil.format(maxIdleTime), TimeUtil.format(delay));
        }
        scheduledExecutorService.scheduleWithFixedDelay(new IdleChecker(delay), delay, delay, TimeUnit.MILLISECONDS);
    }

    public void addService(final ServiceActivityProvider service) {
        activityMap.put(service, 0l);
    }

    public void removeService(final ServiceActivityProvider service) {
        activityMap.remove(service);
    }

    public void terminate() {
        scheduledExecutorService.shutdownNow();
    }

    class IdleChecker implements Runnable {
        final long delay;

        IdleChecker(long delay) {
            this.delay = delay;
        }

        public void run() {
            try {
                long now = System.currentTimeMillis();
                List<ServiceActivityProvider> idleServices = new ArrayList<ServiceActivityProvider>();
                for (Map.Entry<ServiceActivityProvider, Long> entry : activityMap.entrySet()) {
                    try {
                        if (!entry.getKey().isActive()) {
                            Long value = entry.getValue();
                            if (value == 0) {
                                value = System.currentTimeMillis();
                            }
                            if ((now - value) > maxIdleTime) {
                                idleServices.add(entry.getKey());
                            } else {
                                activityMap.put(entry.getKey(), value);
                            }
                        } else {
                            activityMap.put(entry.getKey(), 0l);
                        }
                    } catch (IOException e) {
                        logger.warn("Unable to get activity from service", e);
                        if (!ThrowableUtil.isRetryable(e)) {
                            idleServices.add(entry.getKey());
                        }
                    }
                }

                if (idleServices.size() == serviceElement.getPlanned()) {
                    logger.info("Service [{}] has {} / {} idle instances. Notify for undeploy.",
                                LoggingUtil.getLoggingName(serviceElement),
                                idleServices.size(),
                                serviceElement.getPlanned());
                    ServiceChannel.getInstance().broadcast(new ServiceChannelEvent(this,
                                                                                   serviceElement,
                                                                                   ServiceChannelEvent.Type.IDLE));
                    for (ServiceActivityProvider remove : idleServices) {
                        removeService(remove);
                    }
                } else {
                    logger.info("Service [{}] has {} / {} idle instances. Next check in {}.",
                                LoggingUtil.getLoggingName(serviceElement),
                                idleServices.size(),
                                serviceElement.getPlanned(),
                                TimeUtil.format(delay));
                }

            } catch (Exception e) {
                logger.warn("Caught while determining service [{}] idle", LoggingUtil.getLoggingName(serviceElement), e);
            }
        }
    }
}
