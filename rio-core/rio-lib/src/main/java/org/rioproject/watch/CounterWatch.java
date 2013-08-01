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

/**
 * A CounterWatch provides a mechanism to count a monotonically increasing
 * non-negative value of an arbitrary occurrence of something over time.
 *
 * @author Dennis Reedy
 */
@SuppressWarnings("unused")
public class CounterWatch extends ThresholdWatch implements CounterWatchMBean {
    public static final String VIEW = "org.rioproject.watch.CounterCalculableView";

    /**
     * Create a new Counter Watch
     * 
     * @param id the identifier for this watch
     */
    public CounterWatch(String id) {
        super(id);
        setView(VIEW);
    }

    /**
     * Creates new CounterWatch, creates and exports a WatchDataSourceImpl if
     * the WatchDataSource is null using the Configuration object provided
     * 
     * @param id The identifier for this watch
     * @param config Configuration object used for constructing a
     * WatchDataSource
     */
    public CounterWatch(String id, Configuration config) {
        super(id, config);
        setView(VIEW);
    }    

    /**
     * Create a new Counter Watch
     * 
     * @param watchDataSource the watch data source associated with this watch
     * @param id the identifier for this watch
     */
    public CounterWatch(WatchDataSource watchDataSource, String id) {
        super(watchDataSource, id);
        setView(VIEW);
    }    

    /**
     * @see org.rioproject.watch.CounterWatchMBean#getCounter
     */
    public long getCounter() {
        return (long) getLastCalculableValue();
    }

    /**
     * @see org.rioproject.watch.CounterWatchMBean#setCounter(long)
     */
    public void setCounter(long counter) {
        setCounter(counter, null);
    }

    /**
     * @see org.rioproject.watch.CounterWatchMBean#setCounter(long)
     */
    public void setCounter(long counter, String detail) {
        Calculable calculable = new Calculable(id, (double)counter, System.currentTimeMillis());
        calculable.setDetail(detail);
        addWatchRecord(calculable);
    }

    /**
     * @see org.rioproject.watch.CounterWatchMBean#increment()
     */
    public void increment() {
        setCounter(getCounter() + 1);
    }

    /**
     * Increment with detail
     */
    public void increment(String detail) {
        setCounter(getCounter() + 1, detail);
    }

    /**
     * @see org.rioproject.watch.CounterWatchMBean#increment(long)
     */
    public void increment(long value) {
        setCounter(getCounter() + value);
    }

    /**
     * Increment with detail
     */
    public void increment(long value, String detail) {
        setCounter(getCounter() + value, detail);
    }

    /**
     * @see org.rioproject.watch.CounterWatchMBean#decrement()
     */
    public void decrement() {
        setCounter(getCounter() - 1);
    }

    /**
     * Decrement with detail
     */
    public void decrement(String detail) {
        setCounter(getCounter() - 1, detail);
    }

    /**
     * @see org.rioproject.watch.CounterWatchMBean#decrement(long)
     */
    public void decrement(long value) {
        setCounter(getCounter() - value);
    }

    /**
     * Decrement with detail
     */
    public void decrement(long value, String detail) {
        setCounter(getCounter() - value, detail);
    }
}
