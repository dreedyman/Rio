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

import java.util.ArrayList;
import java.util.List;

/**
 * The class tests the <code>ThresholdWatch</code> part of a class derived from
 * <code>ThresholdWatch</code>. The class tests the public methods and fields
 * inherited by the class under test from <code>ThresholdWatch</code>, and also
 * the public constructors of the class under test. However, testing of the
 * constructors is actually delegated to <code>WatchTest</code>. Testing of the
 * methods and fields inherited from <code>Watch</code> is also delegated to
 * <code>WatchTest</code>.
 */
public class ThresholdWatchTest extends WatchTest {
    /**
     * The logger used by this class.
     *
     * @noinspection UNUSED_SYMBOL
     */
    //private static Logger logger = LoggerFactory.getLogger("org.rioproject.watch");

    /**
     * Constructs a <code>ThresholdWatchTest</code>.
     *
     * @param clazz the <code>ThresholdWatch</code>-derived class to be tested
     * @param expectedView the expected default return value of the
     * <code>getView()</code> method
     */
    public ThresholdWatchTest(Class clazz, String expectedView) {
        super(clazz, expectedView);
    }


    /**
     * Tests the <code>getThresholdManager()</code> method.
     *
     * @throws Exception if the test fails
     */
    @Test
    public void testGetThresholdManager() throws Exception {
        ThresholdWatch watch = (ThresholdWatch) construct1("watch");
        ThresholdManager mgr = watch.getThresholdManager();
        Assert.assertNotNull(mgr);
        Utils.close(watch.getWatchDataSource());
    }

    /*
     * Tests the <code>addThresholdListener(ThresholdListener)</code> and
     * <code>removeThresholdListener(ThresholdListener)</code> methods.
     *
     * @throws Exception if the test fails
     */
    @Test
    public void testAddRemoveThresholdListener() throws Exception {
        String id = "watch1";
        ThresholdWatch watch = (ThresholdWatch) construct1(id);
        watch.setThresholdValues(new ThresholdValues(0, 1));

        // Test adding multiple listeners, with several checkpoints
        final int[] checkpoints = new int[]{0, 1, 10, 100};
        List<LoggingThresholdListener> lnrs = new ArrayList<LoggingThresholdListener>();
        List<StringBuffer> logs = new ArrayList<StringBuffer>();
        int count = 0;
        for (int checkpoint : checkpoints) {
            while (count < checkpoint) {
                StringBuffer log = new StringBuffer();
                LoggingThresholdListener lnr =
                    new LoggingThresholdListener(id, log);
                lnrs.add(lnr);
                logs.add(log);
                watch.addThresholdListener(lnr);
                count++;
            }
            watch.addWatchRecord(new Calculable(id, 0.5));
            checkLogs(logs, "");
            watch.addWatchRecord(new Calculable(id, -0.1));
            checkLogs(logs, "breach(" + id + ",-0.1,0.0,1.0)");
            watch.addWatchRecord(new Calculable(id, 1.1));
            checkLogs(logs, "clear(" + id + ",1.1,0.0,1.0)"
                            + "breach(" + id + ",1.1,0.0,1.0)");
            watch.addWatchRecord(new Calculable(id, 0.5));
            checkLogs(logs, "clear(" + id + ",0.5,0.0,1.0)");
        }

        // Test adding the same listeners (should not change anything)
        for (LoggingThresholdListener lnr : lnrs) {
            watch.addThresholdListener(lnr);
        }
        watch.addWatchRecord(new Calculable(id, -0.1));
        checkLogs(logs, "breach(" + id + ",-0.1,0.0,1.0)");
        watch.addWatchRecord(new Calculable(id, 1.1));
        checkLogs(logs, "clear(" + id + ",1.1,0.0,1.0)"
                        + "breach(" + id + ",1.1,0.0,1.0)");
        watch.addWatchRecord(new Calculable(id, 0.5));
        checkLogs(logs, "clear(" + id + ",0.5,0.0,1.0)");

        // Test removing listeners
        count = 0;
        for (int checkpoint : checkpoints) {
            while (count < checkpoint) {
                LoggingThresholdListener lnr = lnrs.get(count);
                watch.removeThresholdListener(lnr);
                count++;
            }
            List<StringBuffer> removedLogs = logs.subList(0, count);
            List<StringBuffer> remainingLogs = logs.subList(count, logs.size());
            watch.addWatchRecord(new Calculable(id, 0.5));
            checkLogs(removedLogs, "");
            checkLogs(remainingLogs, "");
            watch.addWatchRecord(new Calculable(id, -0.1));
            checkLogs(removedLogs, "");
            checkLogs(remainingLogs, "breach(" + id + ",-0.1,0.0,1.0)");
            watch.addWatchRecord(new Calculable(id, 1.1));
            checkLogs(removedLogs, "");
            checkLogs(remainingLogs, "clear(" + id + ",1.1,0.0,1.0)"
                                     + "breach(" + id + ",1.1,0.0,1.0)");
            watch.addWatchRecord(new Calculable(id, 0.5));
            checkLogs(removedLogs, "");
            checkLogs(remainingLogs, "clear(" + id + ",0.5,0.0,1.0)");
        }

        // Test removing the same listeners (should not change anything)
        for (LoggingThresholdListener lnr : lnrs) {
            watch.removeThresholdListener(lnr);
        }
        watch.addWatchRecord(new Calculable(id, -0.1));
        checkLogs(logs, "");
        watch.addWatchRecord(new Calculable(id, 1.1));
        checkLogs(logs, "");
        watch.addWatchRecord(new Calculable(id, 0.5));
        checkLogs(logs, "");

        // Test adding 1, 10, and 100 listeners again
        count = 0;
        for (int checkpoint : checkpoints) {
            while (count < checkpoint) {
                LoggingThresholdListener lnr = lnrs.get(count);
                watch.addThresholdListener(lnr);
                count++;
            }
            List<StringBuffer> addedLogs = logs.subList(0, count);
            List<StringBuffer> remainingLogs = logs.subList(count, logs.size());
            watch.addWatchRecord(new Calculable(id, 0.5));
            checkLogs(addedLogs, "");
            checkLogs(remainingLogs, "");
            watch.addWatchRecord(new Calculable(id, -0.1));
            checkLogs(addedLogs, "breach(" + id + ",-0.1,0.0,1.0)");
            checkLogs(remainingLogs, "");
            watch.addWatchRecord(new Calculable(id, 1.1));
            checkLogs(addedLogs, "clear(" + id + ",1.1,0.0,1.0)"
                                 + "breach(" + id + ",1.1,0.0,1.0)");
            checkLogs(remainingLogs, "");
            watch.addWatchRecord(new Calculable(id, 0.5));
            checkLogs(addedLogs, "clear(" + id + ",0.5,0.0,1.0)");
            checkLogs(remainingLogs, "");
        }

        // Test adding illegal listener
        try {
            watch.addThresholdListener(null);
            Assert.fail("IllegalArgumentException expected but not thrown");
        } catch (IllegalArgumentException e) {
        }

        // Test removing illegal listener
        try {
            watch.removeThresholdListener(null);
            Assert.fail("IllegalArgumentException expected but not thrown");
        } catch (IllegalArgumentException e) {
        }

        Utils.close(watch.getWatchDataSource());
    }

    /**
     * Tests adding a threshold listener after one or more threshold events have
     * occurred.
     *
     * @throws Exception if the test fails
     */
    @Test
    public void testAddThresholdListenerAfterEvent() throws Exception {
        String id = "watch";
        ThresholdWatch watch = (ThresholdWatch) construct1(id);
        watch.setThresholdValues(new ThresholdValues(0, 1));
        StringBuffer log = new StringBuffer();
        LoggingThresholdListener lnr = new LoggingThresholdListener(id, log);
        watch.addWatchRecord(new Calculable(id, 0.5));
        watch.addWatchRecord(new Calculable(id, -0.1));
        watch.addWatchRecord(new Calculable(id, 1.1));
        watch.addThresholdListener(lnr);
        Assert.assertEquals("", log.toString());
        watch.addWatchRecord(new Calculable(id, 0.5));
        Assert.assertEquals("clear(" + id + ",0.5,0.0,1.0)", log.toString());
        Utils.close(watch.getWatchDataSource());
    }

    /**
     * Tests the <code>getThresholdValues()</code> and <code>setThresholdValues(ThresholdValues)</code>
     * methods.
     *
     * @throws Exception if the test fails
     */
    @Test
    public void testGetSetThresholdValues() throws Exception {

        // Getting default value
        String id = "watch";
        ThresholdWatch watch = (ThresholdWatch) construct1(id);
        ThresholdValues values = watch.getThresholdValues();
        Assert.assertTrue(Double.isNaN(values.getLowThreshold()));
        Assert.assertTrue(Double.isNaN(values.getHighThreshold()));

        // "set" followed by "get"
        ThresholdValues newValues = new ThresholdValues(7.13, 13.7);
        watch.setThresholdValues(newValues);
        values = watch.getThresholdValues();
        Assert.assertEquals(newValues.getLowThreshold(),
                            values.getLowThreshold(), 0);
        Assert.assertEquals(newValues.getHighThreshold(),
                            values.getHighThreshold(), 0);
        // Check that the watch holds the same object (at the time
        // of writing this test it does)
        Assert.assertSame(newValues, values);

        // Setting illegal value
        try {
            watch.setThresholdValues(null);
            Assert.fail("IllegalArgumentException expected but not thrown");
        } catch (IllegalArgumentException e) {
        }

        Utils.close(watch.getWatchDataSource());
    }

    /**
     * Verifies that <code>ThresholdWatch</code> generates threshold events
     * correctly. The following combinations are tested: <ul> <li>None of the
     * thresholds is set <li>The lower threshold is set <li>The upper threshold
     * is set <li>Both thresholds are set </ul>
     *
     * @throws Exception if the test fails
     */
    @Test
    public void testThresholdEvents() throws Exception {
        testThresholdEvents(NO_THRESHOLDS);
        testThresholdEvents(LOWER_THRESHOLD);
        testThresholdEvents(UPPER_THRESHOLD);
        testThresholdEvents(BOTH_THRESHOLDS);
    }


    /**
     * None of the thresholds is set.
     */
    private static final int NO_THRESHOLDS = 0x0;

    /**
     * The lower threshold is set.
     */
    private static final int LOWER_THRESHOLD = 0x1;

    /**
     * The upper threshold is set
     */
    private static final int UPPER_THRESHOLD = 0x2;

    /**
     * Both thresholds are set.
     */
    private static final int BOTH_THRESHOLDS = 0x3;

    /*
     * Verifies that <code>ThresholdWatch</code> generates threshold events
     * correctly for a given combination of thresholds.
     */
    private void testThresholdEvents(int thresholds) throws Exception {
        String id = "w";
        ThresholdWatch watch = (ThresholdWatch) construct1(id);
        StringBuffer log = new StringBuffer();
        LoggingThresholdListener lnr = new LoggingThresholdListener(id, log);
        watch.addThresholdListener(lnr);

        boolean lower = (thresholds & LOWER_THRESHOLD) != 0;
        boolean upper = (thresholds & UPPER_THRESHOLD) != 0;
//        logger.info("Lower threshold [" + lower + "]"
//                + ", upper threshold [" + upper + "]");
        double lowerTh = Double.NaN;
        double upperTh = Double.NaN;
        if (lower) {
            lowerTh = -1;
        }
        if (upper) {
            upperTh = 1;
        }
        ThresholdValues thValues = new ThresholdValues(lowerTh, upperTh);
        watch.setThresholdValues(thValues);
        String suffix = "," + thValues.getLowThreshold()
                        + "," + thValues.getHighThreshold() + ")";

        // The idea is to test the most important situations
        watch.addWatchRecord(new Calculable(id, 0));                // 0
        checkLog(log, "");
        watch.addWatchRecord(new Calculable(id, -1));               // -1
        checkLog(log, "");
        watch.addWatchRecord(new Calculable(id, -1.1));             // -1.1
        checkLog(log, lower ? "breach(w,-1.1" + suffix : "");
        watch.addWatchRecord(new Calculable(id, -1));               // -1
        checkLog(log, "");
        watch.addWatchRecord(new Calculable(id, 0));                // 0
        checkLog(log, lower ? "clear(w,0.0" + suffix : "");
        watch.addWatchRecord(new Calculable(id, -1.1));             // -1.1
        checkLog(log, lower ? "breach(w,-1.1" + suffix : "");
        watch.addWatchRecord(new Calculable(id, 1.1));              // 1.1
        if (lower & upper) {
            checkLog(log, "clear(w,1.1" + suffix
                          + "breach(w,1.1" + suffix);
        } else if (lower) {
            checkLog(log, "clear(w,1.1" + suffix);
        } else if (upper) {
            checkLog(log, "breach(w,1.1" + suffix);
        } else {
            checkLog(log, "");
        }
        watch.addWatchRecord(new Calculable(id, 0));                // 0
        checkLog(log, upper ? "clear(w,0.0" + suffix : "");

        // The other way around
        watch.addWatchRecord(new Calculable(id, 1));                // 1
        checkLog(log, "");
        watch.addWatchRecord(new Calculable(id, 1.1));              // 1.1
        checkLog(log, upper ? "breach(w,1.1" + suffix : "");
        watch.addWatchRecord(new Calculable(id, 1));                // 1
        checkLog(log, "");
        watch.addWatchRecord(new Calculable(id, 0));                // 0
        checkLog(log, upper ? "clear(w,0.0" + suffix : "");
        watch.addWatchRecord(new Calculable(id, 1.1));              // 1.1
        checkLog(log, upper ? "breach(w,1.1" + suffix : "");
        watch.addWatchRecord(new Calculable(id, -1.1));             // -1.1
        if (lower & upper) {
            checkLog(log, "clear(w,-1.1" + suffix
                          + "breach(w,-1.1" + suffix);
        } else if (upper) {
            checkLog(log, "clear(w,-1.1" + suffix);
        } else if (lower) {
            checkLog(log, "breach(w,-1.1" + suffix);
        } else {
            checkLog(log, "");
        }
        watch.addWatchRecord(new Calculable(id, 0));                // 0
        checkLog(log, lower ? "clear(w,0.0" + suffix : "");

        Utils.close(watch.getWatchDataSource());
    }


    /**
     * The class provides a mock implementation of the <code>ThresholdListener</code>
     * interface. The main function of the class is to receive threshold events
     * and log them into a string buffer.
     */
    private class LoggingThresholdListener implements ThresholdListener {

        private String id;
        StringBuffer log;

        public LoggingThresholdListener(String id, StringBuffer log) {
            this.id = id;
            this.log = log;
        }

        public void notify(Calculable calculable, ThresholdValues thresholdValues, ThresholdType type) {

            /*String status =
                (type == ThresholdEvent.BREACHED ? "breached" : "cleared");
            System.out.println(
                "Threshold [" + calculable.getId() + "] " + status + " " +
                "value [" + calculable.getValue() + "] " +
                "low [" + thresholdValues.getCurrentLowThreshold() + "] " +
                "high [" + thresholdValues.getCurrentHighThreshold() + "]");*/
            StringBuilder buf = new StringBuilder();
            buf.append(type == ThresholdType.BREACHED ? "breach(" : "clear(");
            buf.append(calculable.getId()).append(",");
            buf.append(calculable.getValue()).append(",");
            buf.append(thresholdValues.getLowThreshold()).append(",");
            buf.append(thresholdValues.getHighThreshold());
            buf.append(")");
            log.append(buf);
        }

        public String getID() {
            return id;
        }
    }

    /*
     * Checks that a given log consists of a given string, and clears the log.
     */
    private void checkLog(StringBuffer log, String s) {
        Assert.assertEquals(s, log.toString());
        log.delete(0, log.length());
    }

    /*
     * Checks that each log in a given list consists of a given string, and
     * clears each log.
     */
    private void checkLogs(List<StringBuffer> logs, String s) {
        for (StringBuffer log : logs) {
            checkLog(log, s);
        }
    }
}
