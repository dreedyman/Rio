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
import org.junit.Test;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;


/**
 * The class tests the <code>GaugeWatch</code> class against its javadoc
 * specification. The class tests the public constructors, methods, and
 * fields declared in the <code>GaugeWatch</code> class. However, testing
 * of the <code>GaugeWatch</code>'s constructors is actually delegated
 * to <code>ThresholdWatchTest</code>. Testing of the methods and fields
 * inherited from <code>ThresholdWatch</code> is also delegated to
 * <code>ThresholdWatchTest</code>.
 */
public class GaugeWatchTest extends ThresholdWatchTest {

    public GaugeWatchTest() {
        super(GaugeWatch.class, "org.rioproject.watch.ThresholdCalculableView");
    }

    /**
     * Tests the <code>addValue(double)</code> method.
     * The test creates a <code>GaugeWatch</code> and fills it with
     * a huge number of samples using the <code>addValue(double)</code>
     * method. At several checkpoints the method verifies that the data
     * is added correctly.
     * <p>
     * Like other watches, <code>GaugeWatch</code> created with the
     * <code>GaugeWatch(String)</code> constructor uses a data source
     * with a collection of size 1000, that's why the two important
     * checkpoints are 1000 and 1001.
     *
     * @throws RemoteException if watch data source causes problems
     */
    @Test
    public void testAddValue1() throws RemoteException {
        String id = "watch";
        GaugeWatch watch = new GaugeWatch(id);
        DataSourceMonitor mon = new DataSourceMonitor(watch);

        final int checkpoints[] = new int[] {0, 1, 50, 1000, 1001, 2000, 10000};
        final int collectionSize = 1000;
        List<Double> added = new ArrayList<Double>();

        // Go through the checkpoints
        for (int checkpoint : checkpoints) {
            if(watch.getWatchDataSource().getMaxSize()<checkpoint) {
                watch.getWatchDataSource().setMaxSize(1000);
            }
            // Generate samples to reach the checkpoint
            while (added.size() < checkpoint) {
                double d = Math.random();
                added.add(d);
                watch.addValue(d);
            }
            //mon.waitFor(added.size());
            mon.waitFor(Math.min(collectionSize, checkpoint));

            // Verify that the data has been added correctly
            Calculable[] calcs = watch.getWatchDataSource().getCalculable();
            int offset = Math.max(added.size() - collectionSize, 0);
            List<Double> expected = added.subList(offset, added.size());
            List<Double> actual = new ArrayList<Double>();
            for (Calculable calc : calcs) {
                Assert.assertEquals(id, calc.getId());
                actual.add(calc.getValue());
            }
            Assert.assertEquals(expected, actual);
        }

        Utils.close(watch.getWatchDataSource());
    }

    /**
     * Tests the <code>addValue(long)</code> method.
     * The test creates a <code>GaugeWatch</code> and fills it with
     * a huge number of samples using the <code>addValue(long)</code>
     * method. At several checkpoints the method verifies that the data
     * is added correctly.
     * <p>
     * Like other watches, <code>GaugeWatch</code> created with the
     * <code>GaugeWatch(String)</code> constructor uses a data source
     * with a buffer of size 1000, that's why the two important
     * checkpoints are 1000 and 1001.
     *
     * @throws RemoteException if watch data source causes problems
     */
    @Test
    public void testAddValue2() throws RemoteException {
        String id = "watch";
        GaugeWatch watch = new GaugeWatch(id);
        DataSourceMonitor mon = new DataSourceMonitor(watch);

        final int checkpoints[] = new int[] {0, 1, 50, 1000, 1001, 2000, 10000};
        final int collectionSize = 1000;
        List<Long> added = new ArrayList<Long>();

        // Go through the checkpoints
        for (int checkpoint : checkpoints) {
            if(watch.getWatchDataSource().getMaxSize()<checkpoint) {
                watch.getWatchDataSource().setMaxSize(1000);
            }

            // Generate samples to reach the checkpoint
            while (added.size() < checkpoint) {
                long l = (long) (Math.random() * 1000);
                added.add(l);
                watch.addValue(l);
            }
            mon.waitFor(Math.min(collectionSize, checkpoint));

            // Verify that the data has been added correctly
            Calculable[] calcs = watch.getWatchDataSource().getCalculable();
            int offset = Math.max(added.size() - collectionSize, 0);
            List<Long> expected = added.subList(offset, added.size());
            List<Long> actual = new ArrayList<Long>();
            for (Calculable calc : calcs) {
                Assert.assertEquals(id, calc.getId());
                actual.add((long) calc.getValue());
            }
            Assert.assertEquals(expected, actual);
        }

        Utils.close(watch.getWatchDataSource());
    }
}
