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
 * The class tests the <code>StopWatch</code> class against its javadoc
 * specification. The class tests the public constructors, methods, and fields
 * declared in the <code>StopWatch</code> class. However, testing of the
 * <code>StopWatch</code>'s constructors is actually delegated to
 * <code>ThresholdWatchTest</code>. Testing of the methods and fields inherited
 * from <code>ThresholdWatch</code> is also delegated to
 * <code>ThresholdWatchTest</code>.
 */
public class StopWatchTest extends ThresholdWatchTest {

    public StopWatchTest() {
        super(StopWatch.class,
              "org.rioproject.watch.ResponseTimeCalculableView");
    }

    /**
     * Tests the static final fields of the <code>StopWatch</code>.
     */
    @Test
    public void testFields() {
        Assert.assertEquals("org.rioproject.watch.ResponseTimeCalculableView",
                            StopWatch.VIEW);
    }

    /**
     * Tests the <code>getStartTime()</code> and <code>setStartTime(long)</code>
     * methods.
     *
     * @throws RemoteException if the test fails
     */
    @Test
    public void testGetSetStartTime() throws RemoteException {
        StopWatch watch = new StopWatch("watch");
        Assert.assertEquals(0, watch.getStartTime());

        for (int i = 0; i < 10; i++) {
            long value = Math.round(Math.random() * 100);
            watch.setStartTime(value);
            Assert.assertEquals(value, watch.getStartTime());
        }

        checkData(0, watch);

        for (int i = 0; i < 10; i++) {
            watch.setStartTime(System.currentTimeMillis());
            watch.stopTiming();
            checkData(i + 1, watch);
        }

        Utils.close(watch.getWatchDataSource());
    }

    /**
     * Tests the <code>setElapsedTime(long)</code> method.
     *
     * @throws RemoteException if the test fails
     */
    @Test
    public void testSetElapsedTime1() throws RemoteException {
        StopWatch watch = new StopWatch("watch");

        List<Long> expected = new ArrayList<Long>();
        checkData(expected, watch);

        for (int i = 0; i < 10; i++) {
            long value = Math.round(Math.random() * 100);
            watch.setElapsedTime(value);
            expected.add(value);
            checkData(expected, watch);
        }

        Utils.close(watch.getWatchDataSource());
    }

    /**
     * Tests the <code>setElapsedTime(long, long)</code> method.
     *
     * @throws RemoteException if the test fails
     */
    @Test
    public void testSetElapsedTime2() throws RemoteException {
        StopWatch watch = new StopWatch("watch");

        List<Long> expectedValues = new ArrayList<Long>();
        List<Long> expectedStamps = new ArrayList<Long>();
        checkData(expectedValues, expectedStamps, watch);

        for (int i = 0; i < 10; i++) {
            long value = Math.round(Math.random() * 100);
            long stamp = Math.round(Math.random() * 100);
            watch.setElapsedTime(value, stamp);
            expectedValues.add(value);
            expectedStamps.add(stamp);
            checkData(expectedValues, expectedStamps, watch);
        }

        Utils.close(watch.getWatchDataSource());
    }

    /**
     * Tests the <code>startTiming()</code> method.
     *
     * @throws RemoteException if the test fails
     */
    @Test
    public void testStartTiming() throws RemoteException {
        StopWatch watch = new StopWatch("watch");

        Assert.assertEquals(0, watch.getStartTime());

        long prevStartTime = -1;
        for (int i = 0; i < 10; i++) {
            Utils.sleep(100);
            watch.startTiming();
            long startTime = watch.getStartTime();
            Assert.assertTrue(startTime >= 0);
            if (prevStartTime != -1) {
                Assert.assertTrue(startTime > prevStartTime);
            }
            prevStartTime = startTime;
        }

        checkData(0, watch);

        for (int i = 0; i < 10; i++) {
            watch.startTiming();
            Utils.sleep(100);
            watch.stopTiming();
            checkData(i + 1, watch);
        }

        Utils.close(watch.getWatchDataSource());
    }

    /**
     * Tests the <code>stopTiming()</code> method.
     *
     * @throws RemoteException if the test fails
     */
    @Test
    public void testStopTiming() throws RemoteException {
        StopWatch watch = new StopWatch("watch");
        DataSourceMonitor mon = new DataSourceMonitor(watch);

        checkData(0, watch);

        for (int i = 0; i < 10; i++) {
            Utils.sleep(100);
            watch.stopTiming();
            mon.waitFor(i + 1);
            Calculable[] calcs = watch.getWatchDataSource().getCalculable();
            Assert.assertEquals(i + 1, calcs.length);
            Assert.assertTrue(calcs[0].getValue() > 0);
            for (int j = 1; j < calcs.length; j++) {
                Assert.assertTrue(calcs[j].getValue()
                                  >= calcs[j - 1].getValue());
            }
        }

        for (int i = 0; i < 10; i++) {
            watch.startTiming();
            Utils.sleep(100);
            watch.stopTiming();
            checkData(10 + i + 1, watch);
        }

        Utils.close(watch.getWatchDataSource());
    }


    /*
     * Checks that a watch holds a given number of samples and the samples
     * are non-negative.
     */
    private void checkData(int count, StopWatch watch) throws RemoteException {
        DataSourceMonitor mon = new DataSourceMonitor(watch);
        mon.waitFor(count);
        Calculable[] calcs = watch.getWatchDataSource().getCalculable();
        Assert.assertEquals(count, calcs.length);
        for (Calculable calc : calcs) {
            Assert.assertTrue(calc.getValue() >= 0);
        }
    }

    /*
     * Checks that a watch holds a given sequence of samples.
     */
    private void checkData(List<Long> expected, StopWatch watch)
        throws RemoteException {
        DataSourceMonitor mon = new DataSourceMonitor(watch);
        mon.waitFor(expected.size());
        Calculable[] calcs = watch.getWatchDataSource().getCalculable();
        Assert.assertEquals(expected.size(), calcs.length);
        for (int i = 0; i < calcs.length; i++) {
            long expectedValue = expected.get(i);
            Assert.assertEquals(expectedValue, calcs[i].getValue(), 0);
        }
    }

    /*
     * Checks that a watch holds a given sequence of samples with
     * given timestamps.
     */
    private void checkData(List<Long> expectedValues,
                           List<Long> expectedStamps,
                           StopWatch watch) throws RemoteException {
        Assert.assertTrue(expectedValues.size() == expectedStamps.size());
        DataSourceMonitor mon = new DataSourceMonitor(watch);
        mon.waitFor(expectedValues.size());
        Calculable[] calcs = watch.getWatchDataSource().getCalculable();
        Assert.assertEquals(expectedValues.size(), calcs.length);
        for (int i = 0; i < calcs.length; i++) {
            long value = expectedValues.get(i);
            long stamp = expectedStamps.get(i);
            Assert.assertEquals(value, calcs[i].getValue(), 0);
            Assert.assertEquals(stamp, calcs[i].getWhen());
        }
    }
}
