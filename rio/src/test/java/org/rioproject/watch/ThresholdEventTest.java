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

import java.io.Serializable;
import java.util.logging.Logger;


/**
 * The class tests the <code>ThresholdEvent</code> class against its javadoc
 * specification. The class tests the public constructors, methods, and
 * fields declared in <code>ThresholdEvent</code>.
 */
public class ThresholdEventTest  {

    /**
     * The logger used by this class.
     */
    private static Logger logger =
            Logger.getLogger("org.rioproject.test.watch");

    /**
     * Tests the <code>ID</code> field.
     */
    @Test
    public void testId() {
        logger.info("ThresholdEvent.ID: " + ThresholdEvent.ID);
    }

    /**
     * Tests the <code>BREACHED</code> field.
     */
    @Test
    public void testBreached() {
        Assert.assertTrue(ThresholdEvent.BREACHED != ThresholdEvent.CLEARED);
    }

    /**
     * Tests the <code>CLEARED</code> field.
     */
    @Test
    public void testCleared() {
        Assert.assertTrue(ThresholdEvent.CLEARED != ThresholdEvent.BREACHED);
    }

    /**
     * Tests the <code>ThresholdEvent(Object)</code> constructor.
     */
    @Test
    public void testConstructor1() {

        /* States:
         *   N/A
         * Argument combinations:
         *   source: valid object, null
         */

        Object obj = new Object();
        ThresholdEvent event = new ThresholdEvent(obj);
        Assert.assertSame(obj, event.getSource());
        Assert.assertSame(null, event.getCalculable());
        Assert.assertSame(null, event.getThresholdValues());
        Assert.assertEquals(0, event.getType());

        try {
            new ThresholdEvent(null);
            Assert.fail("IllegalArgumentException expected but not thrown");
        } catch (IllegalArgumentException e) {
        }
    }

    /**
     * Tests the
     * <code>ThresholdEvent(Object, Calculable, ThresholdValues, int)</code>
     * constructor.
     */
    @Test
    public void testConstructor2() {

        /* States:
         *   N/A
         * Argument combinations:
         *   source:            valid object, null
         *   calculable:        valid calculable, null
         *   thresholdValues:   valid object, null
         *   type:              BREACHED, CLEARED, 0, 12345
         */

        final Object[] sources = new Object[] {
                new Object(), null};
        final Calculable[] calculables = new Calculable[] {
                new Calculable(), null};
        final ThresholdValues[] tvs = new ThresholdValues[] {
                new ThresholdValues(), null};
        final Integer[] types = ArrayUtils.asObjects(new int[] {
                ThresholdEvent.BREACHED, ThresholdEvent.CLEARED, 0, 12345});

        Object[][] combinations = ArrayUtils.combinations(new Object[][] {
                sources, calculables, tvs, types});
        for (Object[] combination : combinations) {
            Object source = combination[0];
            Calculable calculable = (Calculable) combination[1];
            ThresholdValues tv = (ThresholdValues) combination[2];
            int type = (Integer) combination[3];

            if (source == null) {
                try {
                    new ThresholdEvent(source, calculable, tv, type);
                    Assert.fail("IllegalArgumentException expected"
                                + " but not thrown");
                } catch (IllegalArgumentException e) {
                }
            } else {
                ThresholdEvent event =
                    new ThresholdEvent(source, calculable, tv, type);
                Assert.assertSame(source, event.getSource());
                Assert.assertSame(calculable, event.getCalculable());
                Assert.assertSame(tv, event.getThresholdValues());
                Assert.assertEquals(type, event.getType());
            }
        }
    }

    /**
     * Tests that <code>ThresholdEvent</code> implements
     * <code>Serializable</code>.
     */
    @Test
    public void testSerializable() {
        ThresholdEvent event = new ThresholdEvent(new Object());
        //noinspection ConstantConditions
        Assert.assertTrue(event instanceof Serializable);
    }

    /**
     * Tests the <code>getType()</code> method.
     */
    @Test
    public void testGetType() {

        /* States:
         *   Type: original, modified
         * Argument combinations:
         *   N/A
         */

        ThresholdEvent event = new ThresholdEvent(new Object());
        Assert.assertEquals(0, event.getType());

        event.setType(1);
        Assert.assertEquals(1, event.getType());
    }

    /**
     * Tests the <code>setType(int)</code> method.
     */
    @Test
    public void testSetType() {

        /* States:
         *   Any
         * Argument combinations:
         *   type: BREACHED, CLEARED, 0, 12345
         */

        ThresholdEvent event = new ThresholdEvent(new Object());

        event.setType(ThresholdEvent.BREACHED);
        Assert.assertEquals(ThresholdEvent.BREACHED, event.getType());

        event.setType(ThresholdEvent.CLEARED);
        Assert.assertEquals(ThresholdEvent.CLEARED, event.getType());

        event.setType(0);
        Assert.assertEquals(0, event.getType());

        event.setType(12345);
        Assert.assertEquals(12345, event.getType());
    }

    /**
     * Tests the <code>getCalculable()</code> method.
     */
    @Test
    public void testGetCalculable() {

        /* States:
         *   Calculable: original, modified
         * Argument combinations:
         *   N/A
         */

        ThresholdEvent event = new ThresholdEvent(new Object());
        Assert.assertSame(null, event.getCalculable());

        Calculable c = new Calculable();
        event.setCalculable(c);
        Assert.assertSame(c, event.getCalculable());
    }

    /**
     * Tests the <code>setCalculable(Calculable)</code> method.
     */
    @Test
    public void testSetCalculable() {

        /* States:
         *   Any
         * Argument combinations:
         *   calculable: valid alculable, null
         */

        ThresholdEvent event = new ThresholdEvent(new Object());

        Calculable c = new Calculable();
        event.setCalculable(c);
        Assert.assertSame(c, event.getCalculable());

        event.setCalculable(null);
        Assert.assertSame(null, event.getCalculable());
    }

    /**
     * Tests the <code>getThresholdValues()</code> method.
     */
    @Test
    public void testGetThresholdValues() {

        /* States:
         *   Threshold values: original, modified
         * Argument combinations:
         *   N/A
         */

        ThresholdEvent event = new ThresholdEvent(new Object());
        Assert.assertSame(null, event.getThresholdValues());

        ThresholdValues thv = new ThresholdValues();
        event.setThresholdValues(thv);
        Assert.assertSame(thv, event.getThresholdValues());
    }

    /**
     * Tests the <code>setThresholdValues(ThresholdValues)</code> method.
     */
    @Test
    public void testSetThresholdValues() {

        /* States:
         *   Any
         * Argument combinations:
         *   thresholdValues: valid object, null
         */

        ThresholdEvent event = new ThresholdEvent(new Object());

        ThresholdValues thv = new ThresholdValues();
        event.setThresholdValues(thv);
        Assert.assertSame(thv, event.getThresholdValues());

        event.setThresholdValues(null);
        Assert.assertSame(null, event.getThresholdValues());
    }
}
