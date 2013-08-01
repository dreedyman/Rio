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


/**
 * The class tests the <code>CounterWatch</code> class against its javadoc
 * specification. The class tests the public constructors, methods, and
 * fields declared in the <code>CounterWatch</code> class. However, testing
 * of the <code>CounterWatch</code>'s constructors is actually delegated
 * to <code>ThresholdWatchTest</code>. Testing of the methods and fields
 * inherited from <code>ThresholdWatch</code> is also delegated to
 * <code>ThresholdWatchTest</code>.
 */
public class CounterWatchTest extends ThresholdWatchTest {

    public CounterWatchTest() {
        super(CounterWatch.class, "org.rioproject.watch.CounterCalculableView");
    }

    /**
     * Tests the static final fields of the <code>CounterWatch</code>.
     */
    @Test
    public void testFields() {
        Assert.assertEquals("org.rioproject.watch.CounterCalculableView",
                            CounterWatch.VIEW);
    }

    /**
     * Tests the <code>getCounter</code> and <code>setCounter</code>
     * methods.
     */
    @Test
    public void testGetSetCounter() {
        CounterWatch watch = new CounterWatch("watch");
        Assert.assertEquals(0, watch.getCounter());
        for (int i = 0; i < 10; i++) {
            long value = Math.round(Math.random() * 100);
            watch.setCounter(value);
            Assert.assertEquals(value, watch.getCounter());
        }
        Utils.close(watch.getWatchDataSource());
    }

    /**
     * Tests that the <code>decrement</code>, <code>increment</code>,
     * and <code>setCounter</code> methods work correctly.
     *
     * @throws RemoteException if the test fails
     */
    @Test
    public void testModificatorMethods() throws RemoteException {
        CounterWatch watch = new CounterWatch("watch");
        checkData(new double[] {}, watch);

        watch.decrement();
        checkData(new double[] {-1}, watch);

        watch.decrement(10);
        checkData(new double[] {-1, -11}, watch);

        watch.setCounter(-100);
        checkData(new double[] {-1, -11, -100}, watch);

        watch.setCounter(1);
        checkData(new double[] {-1, -11, -100, 1}, watch);

        watch.increment();
        checkData(new double[] {-1, -11, -100, 1, 2}, watch);

        watch.increment(10);
        checkData(new double[] {-1, -11, -100, 1, 2, 12}, watch);

        watch.decrement(13);
        checkData(new double[] {-1, -11, -100, 1, 2, 12, -1}, watch);

        Utils.close(watch.getWatchDataSource());
    }

    /*
     * Checks that a watch holds a given sequence of samples.
     */
    private void checkData(double[] expected, CounterWatch watch)
            throws RemoteException {
        DataSourceMonitor mon = new DataSourceMonitor(watch);
        mon.waitFor(expected.length);
        Calculable[] calcs = watch.getWatchDataSource().getCalculable();
        Assert.assertEquals(expected.length, calcs.length);
        for (int i = 0; i < expected.length; i++) {
            Assert.assertEquals(expected[i], calcs[i].getValue(), 0);
        }
    }
}
