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
 * Provides a standard MBean to use when administering a Watch using JMX
 */
public interface ThresholdWatchMBean extends WatchMBean {
    /**
     * Get the high threshold value
     *
     * @return The high threshold value
     */
    double getHighThreshold();

    /**
     * Get the low threshold value
     *
     * @return The low threshold value
     */
    double getLowThreshold();

    /**
     * Set the low threshold value
     *
     * @param threshold The threshold value
     */
    void setCurrentLowThreshold(double threshold);

    /**
     * Get the current high threshold value
     *
     * @return The current high threshold value
     */
    double getCurrentHighThreshold();

    /**
     * Get the current high threshold value
     *
     * @param threshold The current high threshold value
     */
    void setCurrentHighThreshold(double threshold);

    /**
     * Get the current low threshold value
     *
     * @return The current low threshold value
     */
    double getCurrentLowThreshold();

    /**
     * Get the number of times the threshold has been breached
     *
     * @return The number of times the threshold has been breached
     */
    long getBreachedCount();

    /**
     * Get the number of times the threshold has been cleared
     *
     * @return The number of times the threshold has been cleared 
     */
    long getClearedCount();
}
