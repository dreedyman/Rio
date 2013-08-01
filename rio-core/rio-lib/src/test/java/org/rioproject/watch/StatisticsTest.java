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

import java.util.*;


/**
 * The class tests the <code>Statistics</code> class against its javadoc
 * specification. The class tests the public constructors, methods, and fields
 * declared in <code>Statistics</code>.
 */
public class StatisticsTest {
    /**
     * Tests the <code>Statistics()</code> constructor.
     */
    @Test
    public void testConstructor1() {
        Statistics stat = new Statistics();
        assertClean(stat);
    }

    /**
     * Tests the <code>Statistics(Vector)</code> constructor.
     */
    @Test
    public void testConstructor2() {

        final int N = 100;
        for (int size = 0; size < N; size++) {
            List<Double> list = new ArrayList<Double>();
            for (int j = 0; j < size; j++) {
                list.add((double) j);
            }
            Statistics stat = new Statistics(list);
            Assert.assertEquals(list, stat.getValues());
            Assert.assertNotSame(list, stat.getValues());
        }

        List<Double> list = new ArrayList<Double>();
        Statistics stat = new Statistics(list);
        Assert.assertEquals(0, stat.count());
        list.add((double) 0);
        Assert.assertEquals(0, stat.count());

        try {
            new Statistics(null);
            Assert.fail("IllegalArgumentException expected but not thrown");
        } catch (IllegalArgumentException e) {
        }
    }

    /**
     * Tests the <code>clearAll()</code> method.
     *
     */
    @Test
    public void testClearAll() {

        Statistics stat = new Statistics();

        for (int i = 0; i < 5; i++) {
            stat.clearAll();
            assertClean(stat);
        }

        List<Double> list = new ArrayList<Double>();
        list.add((double) 0);
        list.add((double) 1);
        list.add((double) 2);
        stat.setValues(list);
        assertCorrect(list, stat);

        for (int i = 0; i < 5; i++) {
            stat.clearAll();
            assertClean(stat);
        }
    }

    /**
     * Tests statistical calculations
     */
    @Test
    public void testStats() {

        Statistics stat = new Statistics();

        for (int i = 0; i < 10; i++) {
            Vector<Double> v = new Vector<Double>();
            int count = (int) (Math.random() * 1000);
            for (int j = 0; j < count; j++) {
                double d = Math.random();
                stat.addValue(d);
                v.add(d);
            }
            assertCorrect(v, stat);
            stat.clearAll();
            assertCorrect(new Vector<Double>(), stat);
        }
    }

    /**
     * Tests the <code>getValues()</code> method.
     */
    @Test
    public void testGetValues() {

        Statistics stat = new Statistics();
        Assert.assertEquals(new Vector(), stat.getValues());

        for (int i = 0; i < 10; i++) {
            Vector<Double> v = new Vector<Double>();
            int count = (int) (Math.random() * 1000);
            for (int j = 0; j < count; j++) {
                double d = Math.random();
                stat.addValue(d);
                v.add(d);
            }
            Assert.assertEquals(v, stat.getValues());
            stat.clearAll();
            Assert.assertEquals(new Vector(), stat.getValues());
        }

        Vector<Double> v = new Vector<Double>();
        stat.setValues(v);
        Assert.assertEquals(v, stat.getValues());
        Assert.assertNotSame(v, stat.getValues());
    }

    /**
     * Tests the <code>addValue(Double)</code> method.
     */
    @Test
    public void testAddValue1() {
        Statistics stat = new Statistics();
        for (int i = 0; i < 10; i++) {
            Vector<Double> v = new Vector<Double>();
            int count = (int) (Math.random() * 100);
            for (int j = 0; j < count; j++) {
                Double d = Math.random();
                stat.addValue(d);
                v.add(d);
                assertCorrect(v, stat);
            }
            stat.clearAll();
        }

        try {
            stat.addValue(null);
            Assert.fail("IllegalArgumentException expected but not thrown");
        } catch (IllegalArgumentException e) {
        }
    }

    /**
     * Tests the <code>addValue(double)</code> method.
     */
    @Test
    public void testAddValue2() {

        Statistics stat = new Statistics();

        for (int i = 0; i < 10; i++) {
            Vector<Double> v = new Vector<Double>();
            int count = (int) (Math.random() * 100);
            for (int j = 0; j < count; j++) {
                double d = Math.random();
                stat.addValue(d);
                v.add(d);
                assertCorrect(v, stat);
            }
            stat.clearAll();
        }
    }

    /**
     * Tests the <code>setValues</code> method.
     */
    @Test
    public void testSetValues() {

        Statistics stat = new Statistics();

        for (int i = 0; i < 10; i++) {
            Vector<Double> v = new Vector<Double>();
            int count = (int) (Math.random() * 100);
            for (int j = 0; j < count; j++) {
                v.add(Math.random());
                stat.setValues(v);
                Assert.assertEquals(v, stat.getValues());
                Assert.assertNotSame(v, stat.getValues());
            }
        }

        List<Double> v = new ArrayList<Double>();
        stat.setValues(v);
        Assert.assertEquals(0, stat.count());
        v.add((double) 0);
        Assert.assertEquals(0, stat.count());

        try {
            stat.setValues((List<Double>)null);
            Assert.fail("IllegalArgumentException expected but not thrown");
        } catch (IllegalArgumentException e) {
        }
    }

    /**
     * Tests the <code>removeValues(double, boolean)</code> method.
     */
    @Test
    public void testRemoveValues1() {

        List<Double> vTest = asList(new double[]{
            1, 2, 3, 4, 5,
            1, 2, 3, 4, 5, 6,
            1, 2, 3, 4, 5, 6, 7,
            1, 2, 3, 4, 5, 6, 7, 8,
            1, 2, 3, 4, 5, 6, 7, 8, 9,
            1, 2, 3, 4, 5, 6, 7, 8, 9, 10
        });

        {
            Statistics stat = new Statistics(vTest);
            for (int i = 0; i <= 10; i++) {
                stat.removeValues(i, false);
                vTest.remove(new Double(i));
                Assert.assertEquals(vTest, stat.getValues());
            }
        }

        {
            Statistics stat = new Statistics(vTest);
            for (int i = 0; i <= 10; i++) {
                stat.removeValues(i, true);
                vTest.removeAll(asList(new double[]{i}));
                Assert.assertEquals(vTest, stat.getValues());
            }
            assertClean(stat);
        }
    }

    /**
     * Tests the <code>removeValues(Double, boolean)</code> method.
     */
    @Test
    public void testRemoveValues2() {

        List<Double> vTest = asList(new double[]{
            1, 2, 3, 4, 5,
            1, 2, 3, 4, 5, 6,
            1, 2, 3, 4, 5, 6, 7,
            1, 2, 3, 4, 5, 6, 7, 8,
            1, 2, 3, 4, 5, 6, 7, 8, 9,
            1, 2, 3, 4, 5, 6, 7, 8, 9, 10
        });

        {
            Statistics stat = new Statistics(vTest);
            for (int i = 0; i <= 10; i++) {
                stat.removeValues(new Double(i), false);
                vTest.remove(new Double(i));
                Assert.assertEquals(vTest, stat.getValues());
            }
        }

        {
            Statistics stat = new Statistics(vTest);
            for (int i = 0; i <= 10; i++) {
                stat.removeValues(new Double(i), true);
                vTest.removeAll(asList(new double[]{i}));
                Assert.assertEquals(vTest, stat.getValues());
            }
            assertClean(stat);
        }

        try {
            new Statistics().removeValues(null, false);
            Assert.fail("IllegalArgumentException expected but not thrown");
        } catch (IllegalArgumentException e) {
        }

        try {
            new Statistics().removeValues(null, true);
            Assert.fail("IllegalArgumentException expected but not thrown");
        } catch (IllegalArgumentException e) {
        }
    }

    /**
     * Tests the <code>removeValue(int)</code> method.
     */
    @Test
    public void testRemoveValue() {

        List<Double> v = asList(new double[]{
            1, 2, 3, 4, 5,
            1, 2, 3, 4, 5, 6,
            1, 2, 3, 4, 5, 6, 7,
            1, 2, 3, 4, 5, 6, 7, 8,
            1, 2, 3, 4, 5, 6, 7, 8, 9,
            1, 2, 3, 4, 5, 6, 7, 8, 9, 10
        });

        Statistics stat = new Statistics(v);

        while (stat.count() > 0) {
            int i = (int) (Math.random() * stat.count());
            stat.removeValue(i);
            v.remove(i);
            Assert.assertEquals(v, stat.getValues());
        }
        assertClean(stat);

        v = asList(new double[]{1, 2, 3, 4, 5});
        stat.setValues(v);
        stat.removeValue(-1);
        stat.removeValue(5);
        Assert.assertEquals(v, stat.getValues());
    }

    /**
     * Tests the <code>removeValues(Double, Double)</code> method.
     */
    @Test
    public void testRemoveValues3() {

        List<Double> vOrg = asList(new double[]{
            1, 2, 3, 4, 5,
            1, 2, 3, 4, 5, 6,
            1, 2, 3, 4, 5, 6, 7,
            1, 2, 3, 4, 5, 6, 7, 8,
            1, 2, 3, 4, 5, 6, 7, 8, 9,
            1, 2, 3, 4, 5, 6, 7, 8, 9, 10
        });
        Statistics stat = new Statistics(vOrg);

        stat.removeValues(new Double(3.5), new Double(3.6));
        Assert.assertEquals(vOrg, stat.getValues());

        stat.removeValues(new Double(3.6), new Double(3.5));
        Assert.assertEquals(vOrg, stat.getValues());

        stat.removeValues(new Double(100), new Double(-100));
        Assert.assertEquals(vOrg, stat.getValues());

        stat.removeValues(new Double(-100), new Double(100));
        Assert.assertEquals(new Vector(), stat.getValues());

        stat.setValues(vOrg);
        stat.removeValues(new Double(3), new Double(3));
        Assert.assertEquals(asList(new double[]{
            1, 2, 4, 5,
            1, 2, 4, 5, 6,
            1, 2, 4, 5, 6, 7,
            1, 2, 4, 5, 6, 7, 8,
            1, 2, 4, 5, 6, 7, 8, 9,
            1, 2, 4, 5, 6, 7, 8, 9, 10
        }), stat.getValues());

        stat.setValues(vOrg);
        stat.removeValues(new Double(3.5), new Double(4.5));
        Assert.assertEquals(asList(new double[]{
            1, 2, 3, 5,
            1, 2, 3, 5, 6,
            1, 2, 3, 5, 6, 7,
            1, 2, 3, 5, 6, 7, 8,
            1, 2, 3, 5, 6, 7, 8, 9,
            1, 2, 3, 5, 6, 7, 8, 9, 10
        }), stat.getValues());

        stat.setValues(vOrg);
        stat.removeValues(new Double(1.1), new Double(9));
        Assert.assertEquals(asList(new double[]{
            1,
            1,
            1,
            1,
            1,
            1, 10
        }), stat.getValues());

        try {
            stat.removeValues(null, (double) 0);
            Assert.fail("IllegalArgumentException expected but not thrown");
        } catch (IllegalArgumentException e) {
        }

        try {
            stat.removeValues((double) 0, null);
            Assert.fail("IllegalArgumentException expected but not thrown");
        } catch (IllegalArgumentException e) {
        }

        try {
            stat.removeValues(null, null);
            Assert.fail("IllegalArgumentException expected but not thrown");
        } catch (IllegalArgumentException e) {
        }
    }

    /**
     * Tests the <code>removeValues(double, double)</code> method.
     */
    @Test
    public void testRemoveValues4() {

        List<Double> vOrg = asList(new double[]{
            1, 2, 3, 4, 5,
            1, 2, 3, 4, 5, 6,
            1, 2, 3, 4, 5, 6, 7,
            1, 2, 3, 4, 5, 6, 7, 8,
            1, 2, 3, 4, 5, 6, 7, 8, 9,
            1, 2, 3, 4, 5, 6, 7, 8, 9, 10
        });
        Statistics stat = new Statistics(vOrg);

        stat.removeValues(3.5, 3.6);
        Assert.assertEquals(vOrg, stat.getValues());

        stat.removeValues(3.6, 3.5);
        Assert.assertEquals(vOrg, stat.getValues());

        stat.removeValues(100, -100);
        Assert.assertEquals(vOrg, stat.getValues());

        stat.removeValues(-100, 100);
        Assert.assertEquals(new Vector(), stat.getValues());

        stat.setValues(vOrg);
        stat.removeValues(3, 3);
        Assert.assertEquals(asList(new double[]{
            1, 2, 4, 5,
            1, 2, 4, 5, 6,
            1, 2, 4, 5, 6, 7,
            1, 2, 4, 5, 6, 7, 8,
            1, 2, 4, 5, 6, 7, 8, 9,
            1, 2, 4, 5, 6, 7, 8, 9, 10
        }), stat.getValues());

        stat.setValues(vOrg);
        stat.removeValues(3.5, 4.5);
        Assert.assertEquals(asList(new double[]{
            1, 2, 3, 5,
            1, 2, 3, 5, 6,
            1, 2, 3, 5, 6, 7,
            1, 2, 3, 5, 6, 7, 8,
            1, 2, 3, 5, 6, 7, 8, 9,
            1, 2, 3, 5, 6, 7, 8, 9, 10
        }), stat.getValues());

        stat.setValues(vOrg);
        stat.removeValues(1.1, 9);
        Assert.assertEquals(asList(new double[]{
            1,
            1,
            1,
            1,
            1,
            1, 10
        }), stat.getValues());
    }


    /*
     * Asserts that a given <code>Statistics</code> object is clean, that is,
     * contains no values.
     */
    private void assertClean(Statistics stat) {
        Assert.assertEquals(0, stat.count());
        Assert.assertTrue(Double.isNaN(stat.max()));
        Assert.assertTrue(Double.isNaN(stat.min()));
        Assert.assertTrue(Double.isNaN(stat.mean()));
        Assert.assertTrue(Double.isNaN(stat.median()));
        Assert.assertTrue(Double.isNaN(stat.mode()));
        Assert.assertEquals(0, stat.modeOccurrenceCount());
        Assert.assertTrue(Double.isNaN(stat.range()));
        Assert.assertTrue(Double.isNaN(stat.standardDeviation()));
        Assert.assertEquals(0, stat.getValues().size());
        Assert.assertTrue(Double.isNaN(stat.sum()));
    }

    /*
     * Asserts that a given <code>Statistics</code> object produces correct
     * statistical results.
     */
    private void assertCorrect(List<Double> v, Statistics stat) {
        if (v.size() > 0) {
            Assert.assertEquals(count(v), stat.count());
            Assert.assertEquals(max(v), stat.max(), 0);
            Assert.assertEquals(min(v), stat.min(), 0);
            Assert.assertEquals(mean(v), stat.mean(), 0);
            Assert.assertEquals(median(v), stat.median(), 0);
            Assert.assertEquals(mode(v), stat.mode(), 0);
            Assert.assertEquals(modeOccurrenceCount(v),
                                stat.modeOccurrenceCount());
            Assert.assertEquals(range(v), stat.range(), 0);
            Assert.assertEquals(standardDeviation(v),
                                stat.standardDeviation(), 0);
            Assert.assertEquals(v, stat.getValues());
            Assert.assertEquals(sum(v), stat.sum(), 0);
        } else {
            assertClean(stat);
        }
    }

    private int count(List<Double> v) {
        return v.size();
    }

    private double max(List<Double> v) {
        if (v.isEmpty()) return Double.NaN;
        return Collections.max(v);
    }

    private double min(List<Double> v) {
        if (v.isEmpty()) return Double.NaN;
        return Collections.min(v);
    }

    private double mean(List<Double> v) {
        if (v.isEmpty()) return Double.NaN;
        double sum = sum(v);
        return sum / (double) v.size();
    }

    private double median(List<Double> v) {
        if (v.isEmpty())
            return Double.NaN;
        double result;
        List<Double> sortedV = sorted(v);
        Double[] dArray = new Double[v.size()];
        dArray = sortedV.toArray(dArray);

        if (dArray.length % 2 != 0 && dArray.length > 0) {
            // length is odd
            int k = (dArray.length - 1) / 2; // middle index
            result = dArray[k];
        } else { // length is even, take avg of two mid vals
            int k = (dArray.length - 1) / 2; // low middle index
            /* average of low mid val and low mid idx+1 val */
            result =
                (dArray[k] + dArray[k + 1]) / 2D;
        }
        return result;
    }

    private double mode(List<Double> v) {
        if (v.isEmpty()) return Double.NaN;
        return (Double) mode0(v, true);
    }

    private int modeOccurrenceCount(List<Double> v) {
        if (v.isEmpty())
            return (0);
        return (Integer) mode0(v, false);
    }

    private Object mode0(List<Double> v, boolean returnMode) {
        Map<Object, Integer> hashtable = new HashMap<Object, Integer>(v.size());
        Object obj;
        Integer occurrences = 0;
        Integer cnt;
        int modeCnt = 0;
        Double mode = 0.0D;

        Iterator it = v.iterator();
        while (it.hasNext()) {
            obj = it.next();
            if (hashtable.containsKey(obj)) {
                modeCnt = (hashtable.get(obj)) + 1;
            } else {
                modeCnt = 1;
            }
            hashtable.put(obj, modeCnt);
        }

        it = v.iterator();
        while (it.hasNext()) {
            obj = it.next();
            cnt = hashtable.get(obj);
            if ((cnt) > modeCnt) {
                mode = (Double) obj;
                occurrences = cnt;
            }
        }

        if (returnMode) {
            return mode;
        } else {
            return occurrences;
        }
    }

    private double range(List<Double> v) {
        if (v.isEmpty())
            return Double.NaN;
        else
            return max(v) - min(v);
    }

    private double standardDeviation(List<Double> v) {
        if (v.isEmpty()) {
            return Double.NaN;
        }
        if (v.size() == 1) {
            // Need to return 0 explicitly because otherwise there would
            // be division by zero
            return 0;
        }
        double sum = sum(v);
        double sumOfSquares = 0.0D;

        for (Double aV : v) {
            sumOfSquares += Math.pow(aV, 2D);
        }
        return Math.sqrt((sumOfSquares - Math.pow(sum, 2d) /
                                         (double) v.size()) /
                         (double) (v.size() - 1));

    }

    private double sum(List<Double> v) {
        if (v.isEmpty())
            return Double.NaN;

        double tot = 0.0D;
        for (Double aV : v) {
            tot += aV;
        }
        return tot;
    }

    private List<Double> sorted(List<Double> v) {
        List<Double> cpy = new ArrayList<Double>(v);
        Collections.sort(cpy);
        return cpy;
    }

    private List<Double> asList(double[] doubles) {
        List<Double> v = new ArrayList<Double>();
        for (double aDouble : doubles) {
            v.add(aDouble);
        }
        return v;
    }
}
