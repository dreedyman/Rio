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
package org.rioproject.watch;

import net.jini.config.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A GaugeWatch provides a mechanism to record values that can go up and down,
 * and can be positive or negative.
 */
public class GaugeWatch extends ThresholdWatch {
    private final Logger logger = LoggerFactory.getLogger(GaugeWatch.class);
    /**
     * Create a new GaugeWatch
     * 
     * @param id The identifier for this watch
     */
    public GaugeWatch(String id) {
        super(id);
    }

    /**
     * Creates new GaugeWatch, creates and exports a WatchDataSourceImpl if the
     * WatchDataSource is null using the Configuration object provided
     * 
     * @param id The identifier for this watch
     * @param config Configuration object used for constructing a
     * WatchDataSource
     */
    public GaugeWatch(String id, Configuration config) {
        super(id, config);
    }    

    /**
     * Create a new GaugeWatch
     * 
     * @param watchDataSource The watch data source associated with this watch
     * @param id The identifier for this watch
     */
    public GaugeWatch(WatchDataSource watchDataSource, String id) {
        super(watchDataSource, id);
    }

    /**
     * Add a value
     * 
     * @param value New value
     */
    public void addValue(long value) {
        logger.debug("id: [{}], value: [{}]", getId(), value);
        addWatchRecord(new Calculable(id, (double)value, System.currentTimeMillis()));
    }

    /**
     * Add a value
     *
     * @param value New value.
     * @param detail Detail about the metric.
     */
    public void addValue(long value, String detail) {
        logger.debug("id: [{}], value: [{}], detail: [{}]", getId(), value, detail);
        Calculable calculable = new Calculable(id, (double)value, System.currentTimeMillis());
        calculable.setDetail(detail);
        addWatchRecord(calculable);
    }
    
    /**
     * Add a value
     * 
     * @param value New value
     */
    public void addValue(double value) {
        logger.debug("id: [{}], value: [{}]", getId(), value);
        addWatchRecord(new Calculable(id, value, System.currentTimeMillis()));
    }

    /**
     * Add a value
     *
     * @param value New value.
     * @param detail Detail about the metric.
     */
    public void addValue(double value, String detail) {
        logger.debug("id: [{}], value: [{}], detail: [{}]", getId(), value, detail);
        Calculable calculable = new Calculable(id, value, System.currentTimeMillis());
        calculable.setDetail(detail);
        addWatchRecord(calculable);
    }
}
