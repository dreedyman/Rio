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
import net.jini.config.Configuration;
import org.junit.Test;

/**
 * The class tests the <code>PeriodicWatch</code> class against its javadoc
 * specification. The class tests the public constructors, methods, and
 * fields declared in the <code>PeriodicWatch</code> class. However, testing
 * of the <code>PeriodicWatch</code>'s constructors is actually delegated
 * to <code>ThresholdWatchTest</code>. Testing of the methods and fields
 * inherited from <code>ThresholdWatch</code> is also delegated to
 * <code>ThresholdWatchTest</code>.
 */
public class PeriodicWatchTest extends ThresholdWatchTest {

    public PeriodicWatchTest() {
        super(MyPeriodicWatch.class,
              "org.rioproject.watch.ThresholdCalculableView");
    }

    /**
     * Tests the static final fields of the <code>PeriodicWatch</code>.
     */
    @Test
    public void testFields() {
        Assert.assertEquals(30000, PeriodicWatch.DEFAULT_PERIOD);
    }

    /**
     * Tests the <code>checkValue()</code> method.
     */
    @Test
    public void testCheckValue() {
        MyPeriodicWatch watch = new MyPeriodicWatch("watch");
        watch.setPeriod(500);
        watch.start();
        Utils.sleep(5000);
        watch.stop();
        Assert.assertTrue(watch.log().length() >= 8);
        Assert.assertTrue(watch.log().length() <= 12);
        Utils.close(watch.getWatchDataSource());
    }

    /**
     * Tests the <code>getPeriod()</code> method.
     */
    @Test
    public void testGetPeriod() {
        MyPeriodicWatch watch = new MyPeriodicWatch("watch");
        Assert.assertEquals(PeriodicWatch.DEFAULT_PERIOD, watch.getPeriod());
        for (int i = 0; i < 10; i++) {
            long value = Math.round(Math.random() * 100) + 1;
            watch.setPeriod(value);
            Assert.assertEquals(value, watch.getPeriod());
        }
        Utils.close(watch.getWatchDataSource());
    }

    /**
     * Tests the <code>setPeriod</code> method.
     */
    @Test
    public void testSetPeriod() {
        MyPeriodicWatch watch = new MyPeriodicWatch("watch");
        watch.start();
        Assert.assertEquals(PeriodicWatch.DEFAULT_PERIOD, watch.getPeriod());

        for (int i = 0; i < 10; i++) {
            long value = Math.round(Math.random() * 100) + 1;
            watch.setPeriod(value);
            Assert.assertEquals(value, watch.getPeriod());
        }

        try {
            watch.setPeriod(0);
            Assert.fail("IllegalArgumentException expected but not thrown");
        } catch (IllegalArgumentException e) {
        }
        try {
            watch.setPeriod(-1);
            Assert.fail("IllegalArgumentException expected but not thrown");
        } catch (IllegalArgumentException e) {
        }

        watch.setPeriod(500);
        Utils.sleep(5000);
        watch.stop();
        Assert.assertTrue(watch.log().length() >= 8);
        Assert.assertTrue(watch.log().length() <= 12);
        watch.log().delete(0, watch.log().length());

        watch.setPeriod(1000);
        Utils.sleep(5000);
        watch.stop();
        Assert.assertTrue(watch.log().length() >= 3);
        Assert.assertTrue(watch.log().length() <= 7);
        watch.log().delete(0, watch.log().length());

        // Same period after stop doesn't restart the watch
        watch.setPeriod(1000);
        Utils.sleep(5000);
        watch.stop();
        Assert.assertTrue(watch.log().length() == 0);
        watch.log().delete(0, watch.log().length());

        // Different period does
        watch.setPeriod(500);
        Utils.sleep(5000);
        watch.stop();
        Assert.assertTrue(watch.log().length() >= 8);
        Assert.assertTrue(watch.log().length() <= 12);
        watch.log().delete(0, watch.log().length());

        Utils.close(watch.getWatchDataSource());
    }

    /**
     * Tests the <code>start()</code> method.
     */
    @Test
    public void testStart() {
        MyPeriodicWatch watch = new MyPeriodicWatch("watch");
        
        watch.setPeriod(500);
        watch.stop();
        watch.start();
        Utils.sleep(5000);
        watch.stop();
        Assert.assertTrue(watch.log().length() >= 8);
        Assert.assertTrue(watch.log().length() <= 12);
        watch.log().delete(0, watch.log().length());

        // start after stop
        watch.start();
        watch.stop();
        Utils.sleep(5000);
        Assert.assertTrue(watch.log().length() == 0);
        watch.start();
        Utils.sleep(5000);
        watch.stop();
        Assert.assertTrue(watch.log().length() >= 8);
        Assert.assertTrue(watch.log().length() <= 12);
        watch.log().delete(0, watch.log().length());

        // Check restart behavior
        for (int i = 0; i < 1000; i++) {
            watch.start();
        }
        for (int i = 0; i < 50; i++) {
            watch.start();
            Utils.sleep(100);
        }
        Assert.assertTrue(watch.log().length() == 0);
        Utils.sleep(5000);
        watch.stop();
        Assert.assertTrue(watch.log().length() >= 8);
        Assert.assertTrue(watch.log().length() <= 12);
        watch.log().delete(0, watch.log().length());

        Utils.close(watch.getWatchDataSource());
    }

    /**
     * Tests the <code>stop()</code> method.
     */
    @Test
    public void testStop() {
        MyPeriodicWatch watch = new MyPeriodicWatch("watch");
        watch.start();
        watch.setPeriod(500);
        Utils.sleep(5000);
        watch.stop();
        Assert.assertTrue(watch.log().length() >= 8);
        Assert.assertTrue(watch.log().length() <= 12);
        watch.log().delete(0, watch.log().length());

        Utils.sleep(2000);
        Assert.assertTrue(watch.log().length() == 0);

        watch.stop();
        Utils.sleep(2000);
        Assert.assertTrue(watch.log().length() == 0);

        watch.start();
        Utils.sleep(5000);
        for (int i = 0; i < 100; i++) {
            watch.stop();
        }
        Assert.assertTrue(watch.log().length() >= 8);
        Assert.assertTrue(watch.log().length() <= 12);
        watch.log().delete(0, watch.log().length());

        for (int i = 0; i < 1000; i++) {
            watch.stop();
        }

        Utils.close(watch.getWatchDataSource());
    }


    /**
     * The class represents a periodic watch used throughout the test.
     */
    private static class MyPeriodicWatch extends PeriodicWatch {

        StringBuffer log = new StringBuffer();

        public MyPeriodicWatch(String id) {
            super(id);
        }

        public MyPeriodicWatch(String id, Configuration config) {
            super(id, config);
        }

        public MyPeriodicWatch(WatchDataSource watchDataSource, String id) {
            super(watchDataSource, id);
        }

        public StringBuffer log() {
            return log;
        }

        public void checkValue() {
            log.append("c");
        }
    }
}
