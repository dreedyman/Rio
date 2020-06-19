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
package org.rioproject.test.monitor;

import org.rioproject.test.TimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The class represents an object that can be used to wait for a condition to
 * become true and stable.
 */
public class Waiter {
    /**
     * The logger used by this class.
     */
    private static Logger logger = LoggerFactory.getLogger("org.rioproject.test");

    /**
     * The maximum timeout.
     */
    private long maxTimeout = 180000;

    /**
     * The stability timeout.
     */
    private long stabilityTimeout = 2000;


    /**
     * Constructs a <code>Waiter</code>.
     */
    public Waiter() {
    }


    /**
     * Waits for a given condition to become true and stable.
     *
     * @param condition the condition to wait for.
     * @throws org.rioproject.test.TimeoutException if the wait times out (i.e. the maximum timeout
     * has elapsed).
     */
    public void waitFor(Condition condition) throws TimeoutException {

        logger.info("Waiting for [" + condition.toString() + "] ...");

        // This method is implemented as a state machine invoked once
        // a second. An alternative is a fully event-driven state machine
        // controlled by simultaneous timers and events
        // (which seems unreasonably complex for now)
        final long TIME_STEP = 1000;
        final int WAIT = 0;
        final int STABILITY_WAIT = 1;
        int state = WAIT;

        long maxTime = System.currentTimeMillis() + getMaxTimeout();
        long stabilityTime = 0;

        // State machine
        while (true) {
            long time = System.currentTimeMillis();
            if (state == WAIT) {
                if (condition.test()) {   // -> STABILITY_WAIT
                    state = STABILITY_WAIT;
                    stabilityTime = time + getStabilityTimeout();
                    logger.info("Ensuring stable state ...");
                } else if (time > maxTime) {        // ERROR
                    throw new TimeoutException();
                }
            } else {
                if (!condition.test()) {   // -> WAIT
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
            }
        }

        logger.info("Waiting for [" + condition.toString() + "] - OK");
    }


    /**
     * Retrieves the maximum timeout. The default maximum timeout is 180000 (3
     * minutes).
     *
     * @return the maximum timeout, in milliseconds.
     */
    public long getMaxTimeout() {
        return maxTimeout;
    }

    /**
     * Sets the maximum timeout. The default maximum timeout is 180000 (3
     * minutes).
     *
     * @param timeout the new maximum timeout, in milliseconds.
     */
    public void setMaxTimeout(long timeout) {
        maxTimeout = timeout;
    }

    /**
     * Retrieves the stability timeout. The default stability timeout is 2000 (2
     * seconds).
     *
     * @return the stability timeout, in milliseconds.
     */
    public long getStabilityTimeout() {
        return stabilityTimeout;
    }

    /**
     * Sets the stability timeout. The default stability timeout is 2000 (2
     * seconds).
     *
     * @param timeout the new stability timeout, in milliseconds.
     */
    public void setStabilityTimeout(long timeout) {
        stabilityTimeout = timeout;
    }
}
