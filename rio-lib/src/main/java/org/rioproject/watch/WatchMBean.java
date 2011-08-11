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

import javax.management.openmbean.TabularData;

/**
 * Provides a standard MBean to use when administering a Watch using JMX
 */
public interface WatchMBean {
    /**
     * Getter for property id.
     *
     * @return Value of property id.
     */
    String getId();

    /**
     * Get the last Calculable as a CompositeData object
     *
     * @return The last Calculable as a CompositeData object
     */
    double getLastCalculableValue();

    /**
     * Clear the collected values
     */
    void clear();

    /**
     * Get all collected Calculable values as TabularData
     *
     * @return All Calculable values as TabularData
     */
    TabularData getCalculables();
}
