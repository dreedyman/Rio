/*
 * Copyright to the original author or authors.
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

import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

/**
 * A PeriodicWatch provides a mechanism to obtain information at preset intervals of time.
 *
 * @author Dennis Reedy
 */
public abstract class PeriodicWatch extends ThresholdWatch implements PeriodicWatchMBean {
    /** Holds value of property period. */
    public static final long DEFAULT_PERIOD = 30 * 1000;
    private long period = DEFAULT_PERIOD;
    private Timer watchTimer;

    /**
     * Creates new {@code PeriodicWatch}
     * 
     * @param id the identifier for this watch
     */
    public PeriodicWatch(String id) {
        super(id);
    }

    /**
     * Creates new {@code PeriodicWatch}, creates and exports a {@code WatchDataSourceImpl} if
     * the {@code WatchDataSource} is {@code null} using the {@code Configuration} object provided
     * 
     * @param id The identifier for this watch
     * @param config Configuration object used for constructing a
     * WatchDataSource
     */
    public PeriodicWatch(String id, Configuration config) {
        super(id, config);
    }    

    /**
     * Creates new {@code PeriodicWatch}
     * 
     * @param watchDataSource the {@code WatchDataSource} associated with this watch
     * @param id the identifier for this watch
     */
    public PeriodicWatch(WatchDataSource watchDataSource, String id) {
        super(watchDataSource, id);
    }

    /**
     * @see org.rioproject.watch.PeriodicWatchMBean#start
     */
    public void start() {
        long now = System.currentTimeMillis();
        stop();
        watchTimer = new Timer(true);
        watchTimer.schedule(new PeriodicTask(), new Date(now+period), period);
    }

    /**
     * @see org.rioproject.watch.PeriodicWatchMBean#stop
     */
    public void stop() {
        if(watchTimer != null)
            watchTimer.cancel();
    }

    /**
     * @see org.rioproject.watch.PeriodicWatchMBean#getPeriod
     */
    public long getPeriod() {
        return (period);
    }

    /**
     * @see org.rioproject.watch.PeriodicWatchMBean#setPeriod
     */
    public void setPeriod(long newPeriod) {
        if(newPeriod == period)
            return;
        if(newPeriod <= 0)
            throw new IllegalArgumentException("period cannot be less then or equal to zero");
        this.period = newPeriod;
        if(watchTimer!=null) {
            stop();
            start();
        }
    }

    /**
     * The TimerTask which calls checkValue
     */
    class PeriodicTask extends TimerTask {
        /**
         * The action to be performed by this timer task.
         */
        public void run() {
            checkValue();
        }
    }
}
