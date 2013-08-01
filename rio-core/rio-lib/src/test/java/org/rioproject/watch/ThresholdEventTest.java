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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


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
            LoggerFactory.getLogger("org.rioproject.test.watch");

    /**
     * Tests the <code>ID</code> field.
     */
    @Test
    public void testId() {
        logger.info("ThresholdEvent.ID: " + ThresholdEvent.ID);
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
        Assert.assertEquals(null, event.getThresholdType());

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

        final Object[] sources = new Object[] { new Object(), null};
        final Calculable[] calculables = new Calculable[] {new Calculable(), null};
        final ThresholdValues[] tvs = new ThresholdValues[] { new ThresholdValues(), null};
        final ThresholdType[] types = new ThresholdType[] {
                ThresholdType.BREACHED, ThresholdType.CLEARED};

        Object[][] combinations = ArrayUtils.combinations(new Object[][] {
                sources, calculables, tvs, types});
        for (Object[] combination : combinations) {
            Object source = combination[0];
            Calculable calculable = (Calculable) combination[1];
            ThresholdValues tv = (ThresholdValues) combination[2];
            ThresholdType type = (ThresholdType) combination[3];

            if (source == null) {
                try {
                    new ThresholdEvent(source, calculable, tv, type);
                    Assert.fail("IllegalArgumentException expected but not thrown");
                } catch (IllegalArgumentException e) {
                }
            } else {
                ThresholdEvent event = new ThresholdEvent(source, calculable, tv, type);
                Assert.assertSame(source, event.getSource());
                Assert.assertSame(calculable, event.getCalculable());
                Assert.assertSame(tv, event.getThresholdValues());
                Assert.assertEquals(type, event.getThresholdType());
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
     * Tests the <code>setType(Type)</code> method.
     */
    @Test
    public void testSetType() {

        /* States:
         *   Any
         * Argument combinations:
         *   type: BREACHED, CLEARED, 0, 12345
         */

        ThresholdEvent event = new ThresholdEvent(new Object());

        event.setThresholdType(ThresholdType.BREACHED);
        Assert.assertEquals(ThresholdType.BREACHED, event.getThresholdType());

        event.setThresholdType(ThresholdType.CLEARED);
        Assert.assertEquals(ThresholdType.CLEARED, event.getThresholdType());
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
