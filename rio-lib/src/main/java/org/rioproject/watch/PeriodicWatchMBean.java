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
package org.rioproject.watch;

/**
 * Provides a standard MBean to use when administering a PeriodicWatch using JMX
 */
public interface PeriodicWatchMBean extends ThresholdWatchMBean {
    /**
     * Start the PeriodicWatch. The PeriodicWatch will be started using repeated
     * fixed-delay executions, with the first invocation of
     * <code>checkValue</code> occurring at the time this method is invoked
     * plus the time specified for the <code>property</code> value In
     * fixed-delay execution, each execution is scheduled relative to the actual
     * execution time of the previous execution. If an execution is delayed for
     * any reason (such as garbage collection or other background activity),
     * subsequent executions will be delayed as well. A new Timer will be
     * created each time this method is called. If a Timer already exists, it
     * will be cancelled.
     *
     * @see java.util.Timer#schedule(java.util.TimerTask, java.util.Date, long)
     */
    void start();

    /**
     * Stop the PeriodicWatch
     */
    void stop();

    /**
     * Getter for property period.
     *
     * @return Value of property period.
     */
    long getPeriod();

    /**
     * Setter for property period.
     *
     * @param newPeriod New value of property period. The result of invoking this
     * method will be to stop the PeriodicWatch and reschedule the subsequent
     * fixed-delay execution based on the value provided for period
     */
    void setPeriod(long newPeriod);

    /**
     * Abstract method to check the watch's value at the periodic interval
     */
    void checkValue();
}
