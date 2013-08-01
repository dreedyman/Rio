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

import junit.framework.Assert;

import java.rmi.RemoteException;

/**
 * A utility object used to detect the moment when all samples passed to a data
 * source get into the data source history.
 */
public class DataSourceMonitor {
    /**
     * The data source this object is attached to
     */
    private WatchDataSource dataSource;

    /**
     * Constructs a DataSourceMonitor for a WatchDataSource.
     *
     * @param dataSource the data source to attach this object to
     */
    public DataSourceMonitor(WatchDataSource dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * Constructs a DataSourceMonitor for a Watch.
     *
     * @param watch the watch to attach this object to
     */
    public DataSourceMonitor(Watch watch) {
        this.dataSource = watch.getWatchDataSource();
    }


    /**
     * Waits until the specified number of samples appear in the data source
     * history.
     *
     * @param count the number of samples in the history to wait for
     * @throws RemoteException of the data source fails
     */
    public void waitFor(int count) throws RemoteException {
        final long timeout = 60000;
        final long period = 50;
        final long startTime = System.currentTimeMillis();
        while (true) {
            int current = dataSource.getCurrentSize();
            if (current == count) {
                return;
            }
            if (current > count) {
                Assert.fail("data source history exceeds expected size"
                            + ", expected: " + count
                            + ", current: " + current);
            }
            if (System.currentTimeMillis() - startTime > timeout) {
                Assert.fail("wait timed out"
                            + ", expected: " + count
                            + ", current: " + current);
            }
            Utils.sleep(period);
        }
    }
}
