/*
 * Copyright to the original author or authors.
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
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Dennis Reedy
 */
public class BoundedThresholdManagerTest {

    @Test
    public void testThresholdCrossedAscending() throws Exception {
        BoundedThresholdManager thresholdManager = new BoundedThresholdManager("foo");
        ThresholdValues thresholdValues = new ThresholdValues(0, 5);
        thresholdManager.setThresholdValues(thresholdValues);
        MyThresholdListener listener = new MyThresholdListener();
        thresholdManager.addThresholdListener(listener);
        for(Calculable calculable : makeCalculables(20)) {
            thresholdManager.checkThreshold(calculable);
        }

        /* Notifications start at 6 and last until 19 (inclusive) */
        Assert.assertEquals(14, listener.breached.size());
    }

    @Test
    public void testThresholdCrossedDescending() throws Exception {
        BoundedThresholdManager thresholdManager = new BoundedThresholdManager("foo");
        ThresholdValues thresholdValues = new ThresholdValues(30, 50);
        thresholdManager.setThresholdValues(thresholdValues);
        MyThresholdListener listener = new MyThresholdListener();
        thresholdManager.addThresholdListener(listener);
        List<Calculable> calculables = makeCalculables(50);
        Collections.reverse(calculables);
        for(Calculable calculable : calculables) {
            thresholdManager.checkThreshold(calculable);
        }
        /* Notifications start at 29 and last until 0 (inclusive) */
        Assert.assertEquals(30, listener.breached.size());
    }

    @Test
    public void testThresholdCrossedAscendingWithStep() throws Exception {
        BoundedThresholdManager thresholdManager = new BoundedThresholdManager("foo");
        ThresholdValues thresholdValues = new ThresholdValues(0, 5, 2);
        thresholdManager.setThresholdValues(thresholdValues);
        MyThresholdListener listener = new MyThresholdListener();
        thresholdManager.addThresholdListener(listener);
        /*
         * With a step of 2, the high threshold notifications will begin at 6. So:
         * 1st notify at 6, next at 8, 10, 12, 14, 16, and 18
         */
        for(Calculable calculable : makeCalculables(20)) {
            thresholdManager.checkThreshold(calculable);
        }
        for(Map.Entry<Integer, Calculable> entry : listener.breached.entrySet()) {
            System.out.println(entry.getValue().getValue());
        }
        Assert.assertEquals(7, listener.breached.size());
    }

    @Test
    public void testThresholdCrossedDescendingWithStep() throws Exception {
        BoundedThresholdManager thresholdManager = new BoundedThresholdManager("foo");
        ThresholdValues thresholdValues = new ThresholdValues(30, 50, 2);
        thresholdManager.setThresholdValues(thresholdValues);
        MyThresholdListener listener = new MyThresholdListener();
        thresholdManager.addThresholdListener(listener);
        /*
         * With a step of 2, the low threshold notifications will begin at 29. So:
         * 1st notify at 29, next at 27, 25, 23, 21, 19, 17, 15, 13, 11, 9, 7, 5, 3, 1
         */
        List<Calculable> calculables = makeCalculables(50);
        Collections.reverse(calculables);
        for(Calculable calculable : calculables) {
            thresholdManager.checkThreshold(calculable);
        }

        for(Map.Entry<Integer, Calculable> entry : listener.breached.entrySet()) {
            System.out.println(entry.getValue().getValue());
        }
        Assert.assertEquals(15, listener.breached.size());
    }

    @Test
    public void testThresholdCrossedMixedBagWithStep() throws Exception {
        BoundedThresholdManager thresholdManager = new BoundedThresholdManager("foo");
        ThresholdValues thresholdValues = new ThresholdValues(30, 50, 2.2);
        thresholdManager.setThresholdValues(thresholdValues);
        MyThresholdListener listener = new MyThresholdListener();
        thresholdManager.addThresholdListener(listener);

        List<Calculable> calculables = new LinkedList<Calculable>();
        calculables.add(new Calculable("foo", 30.1));
        calculables.add(new Calculable("foo", 32.1));
        calculables.add(new Calculable("foo", 19.6)); // breached
        calculables.add(new Calculable("foo", 1.3));  // breached
        calculables.add(new Calculable("foo", 18.3)); // breached
        calculables.add(new Calculable("foo", 28.6)); // breached
        calculables.add(new Calculable("foo", 58.7)); // cleared && breached
        calculables.add(new Calculable("foo", 20.7)); // cleared && breached
        /*
         * With a step of 2, the low threshold notifications will begin at 29. So:
         * 1st notify at 29, next at 27, 25, 23, 21, 19, 17, 15, 13, 11, 9, 7, 5, 3, 1
         */

        for(Calculable calculable : calculables) {
            thresholdManager.checkThreshold(calculable);
        }

        Map<Integer, String> notifications = new TreeMap<Integer, String>();
        for(Map.Entry<Integer, Calculable> entry : listener.breached.entrySet()) {
            notifications.put(entry.getKey(), "breached: " + entry.getValue().getValue());
        }
        for(Map.Entry<Integer, Calculable> entry : listener.cleared.entrySet()) {
            notifications.put(entry.getKey(), "cleared:  " + entry.getValue().getValue());
        }
        for(Map.Entry<Integer, String> entry : notifications.entrySet()) {
            System.out.println(entry.getKey() + " " + entry.getValue());
        }
        System.out.println();
        Assert.assertEquals(6, listener.breached.size());
        Assert.assertEquals(2, listener.cleared.size());
    }

    @Test
    public void testThresholdCrossedMixedBagOfPercentsWithStep() throws Exception {
        BoundedThresholdManager thresholdManager = new BoundedThresholdManager("foo");
        ThresholdValues thresholdValues = new ThresholdValues(.30, .50, .1);
        thresholdManager.setThresholdValues(thresholdValues);
        MyThresholdListener listener = new MyThresholdListener();
        thresholdManager.addThresholdListener(listener);

        List<Calculable> calculables = new LinkedList<Calculable>();
        calculables.add(new Calculable("foo", .301));
        calculables.add(new Calculable("foo", .321));
        calculables.add(new Calculable("foo", .196)); // breached
        calculables.add(new Calculable("foo", .03));  // breached
        calculables.add(new Calculable("foo", .183)); // breached
        calculables.add(new Calculable("foo", .286)); // breached
        calculables.add(new Calculable("foo", .587)); // cleared && breached
        calculables.add(new Calculable("foo", .207)); // cleared && breached
        /*
         * With a step of 2, the low threshold notifications will begin at 29. So:
         * 1st notify at 29, next at 27, 25, 23, 21, 19, 17, 15, 13, 11, 9, 7, 5, 3, 1
         */
        for(Calculable calculable : calculables) {
            thresholdManager.checkThreshold(calculable);
        }

        Map<Integer, String> notifications = new TreeMap<Integer, String>();
        for(Map.Entry<Integer, Calculable> entry : listener.breached.entrySet()) {
            notifications.put(entry.getKey(), "breached: " + entry.getValue().getValue());
        }
        for(Map.Entry<Integer, Calculable> entry : listener.cleared.entrySet()) {
            notifications.put(entry.getKey(), "cleared:  " + entry.getValue().getValue());
        }
        for(Map.Entry<Integer, String> entry : notifications.entrySet()) {
            System.out.println(entry.getKey() + " " + entry.getValue());
        }
        Assert.assertEquals(6, listener.breached.size());
        Assert.assertEquals(2, listener.cleared.size());
    }

    class MyThresholdListener implements ThresholdListener {
        final Map<Integer, Calculable> breached = new HashMap<Integer, Calculable>();
        final Map<Integer, Calculable> cleared = new HashMap<Integer, Calculable>();
        final AtomicInteger counter = new AtomicInteger(0);

        @Override
        public void notify(Calculable calculable, ThresholdValues thresholdValues, ThresholdType type) {
            if(type.equals(ThresholdType.BREACHED))
                breached.put(counter.incrementAndGet(), calculable);
            if(type.equals(ThresholdType.CLEARED))
                cleared.put(counter.incrementAndGet(), calculable);
        }
    }
    private List<Calculable> makeCalculables(int num) {
        List<Calculable> calcs = new ArrayList<Calculable>();
        for(int i=0; i<num; i++) {
            calcs.add(new Calculable("foo", i, System.currentTimeMillis()));
        }
        return calcs;
    }
}
