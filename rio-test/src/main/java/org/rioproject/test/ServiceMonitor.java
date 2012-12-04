/*
 * Copyright 2009 the original author or authors.
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
package org.rioproject.test;

import net.jini.core.lookup.ServiceItem;
import net.jini.core.lookup.ServiceTemplate;
import net.jini.lookup.ServiceDiscoveryManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeoutException;

/**
 * The class represents a service monitor. Using this class, you
 * can specify the type of services you are interested in, and then:
 * <ul>
 * <li>Retrieve the currently available services.
 * <li>Determine the number of currently available services of the
 *     specified type.
 * <li>Wait until the number of services of the specified type is
 *     equal to a given number and stable.
 * </ul>
 */
public class ServiceMonitor<T> {
    public static long MAX_TIMEOUT = 180000;
    public static long STABILITY_TIMEOUT = 3000;
    private static Logger logger = LoggerFactory.getLogger(ServiceMonitor.class.getPackage().getName());
    private ServiceDiscoveryManager sdm;
    private Class<T> type;
    private long maxTimeout = MAX_TIMEOUT;
    private long stabilityTimeout = STABILITY_TIMEOUT;

    /**
     * Constructs a <code>ServiceMonitor</code>.
     *
     * @param sdm the service discovery manager to use to discover services.
     * @param type The parameterized type
     */
    public ServiceMonitor(ServiceDiscoveryManager sdm, Class<T> type) {
        this.sdm = sdm;
        this.type = type;
    }

    /**
     * Waits until the number of services is equal to a given number and
     * stable.
     * <p>
     * The method waits until the number of services is equal to a given
     * number. If the number of services has reached the required level,
     * the method, to ensure that the number of services is stable, waits
     * for the so-called "stability timeout" and checks that the number
     * of services does not change. If the number of services does not
     * change during the stability timeout, the method exits successfully.
     * Otherwise the method starts waiting from the beginning.
     * <p>
     * If the method waits for too long so that the so-called "maximum
     * timeout" elapses, a  {@link TimeoutException}  is thrown.
     *
     * @param serviceCount the number of service to wait for.
     *
     * @throws TimeoutException if the wait times out (i.e. the maximum
     *          timeout has elapsed).
     */
    public void waitFor(long serviceCount) throws TimeoutException {
        logger.info("Waiting for ["+serviceCount+"] services of type ["+type.getName()+"] ...");

        // This method is implemented as a state machine invoked once
        // a second. An alternative is a fully event-driven state machine
        // controlled by simultaneous timers and lookup cache events
        // (which seems to be unreasonably complex for now)
        final long TIME_STEP = 1000;
        final int WAIT = 0;
        final int STABILITY_WAIT = 1;
        int state = WAIT;

        long maxTime = System.currentTimeMillis() + maxTimeout;
        long stabilityTime = 0;

        // State machine
        while (true) {
            long time = System.currentTimeMillis();
            if (state == WAIT) {
                if (getAndLogCount(serviceCount) == serviceCount) {   // -> STABILITY_WAIT
                    state = STABILITY_WAIT;
                    stabilityTime = time + stabilityTimeout;
                    logger.info("Ensuring stable state ...");
                } else if (time > maxTime) {        // ERROR
                    throw new TimeoutException();
                }
            }
            if (state == STABILITY_WAIT) {
                if (getAndLogCount(serviceCount) != serviceCount) {   // -> WAIT
                    state = WAIT;
                    logger.info("Waiting again ...");
                } else if (time > stabilityTime) {  // SUCCESS
                    break;
                } else if (time > maxTime) {        // ERROR
                    throw new TimeoutException();
                }
            }
            try {
                Thread.sleep(TIME_STEP);
            } catch (InterruptedException e) {
                //
            }
        }

       logger.info("Waiting for ["+serviceCount+"] services of type ["+type.getName()+"] - OK");
    }

    /**
     * Determines the number of currently available services.
     *
     * @return the number of currently available services
     */
    public int getCount() {
        return getServices().size();
    }

    /**
     * Determines and logs the number of currently available services.
     *
     * @param expected The expected amount
     *
     * @return the number of currently available services
     */
    private int getAndLogCount(long expected) {
        logger.info(type.getName()+" found: ["+getCount()+"], Expecting ["+expected+"]");
        return getCount();
    }

    /**
     * Retrieves the currently available services.
     *
     * @return the List of proxies
     */
    @SuppressWarnings("unchecked")
    public List<T> getServices() {
        ServiceTemplate template = new ServiceTemplate(null, new Class[]{type}, null);
        ServiceItem[] items = sdm.lookup(template, Integer.MAX_VALUE, null);
        List<T> services = new ArrayList<T>();
        for (ServiceItem item : items) {
            services.add((T)item.service); // TODO: prepare
        }
        return services;
    }    


}
