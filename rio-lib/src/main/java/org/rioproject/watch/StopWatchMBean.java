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
 * Provides a standard MBean to use when administering a StopWatch using JMX
 */
public interface StopWatchMBean extends ThresholdWatchMBean {
    /**
     * Start the timing for the watch
     */
    void startTiming();

    /**
     * Stop the timing for the watch
     */
    void stopTiming();

    /**
     * Sets the elapsed time of the measured interval
     *
     * @param elapsed milliseconds of elapsed time
     */
    void setElapsedTime(long elapsed);

    /**
     * Sets the elapsed time of the measured interval
     *
     * @param elapsed milliseconds of elapsed time
     * @param now the current time in milliseconds since the epoch.
     */
    void setElapsedTime(long elapsed, long now);

    /**
     * Getter for property startTime
     *
     * @return Value of property startTime.
     */
    long getStartTime();

    /**
     * Setter for property startTime
     *
     * @param startTime New value of property startTime.
     */
    void setStartTime(long startTime);
}
