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
import net.jini.config.EmptyConfiguration;
import org.junit.Test;
import org.junit.runner.JUnitCore;

import java.util.*;


/**
 * The class tests the <code>WatchDataSourceRegistry</code> class against
 * its javadoc specification. The class tests the public constructors and
 * methods declared in <code>WatchDataSourceRegistry</code>.
 * <p>
 * This test does not test:
 * <ul>
 * <li>Duplicated watch IDs
 * <li>Duplicated watches and listeners, that is, registering (or
 *     deregistering) same watches and listeners more than once
 * </ul>
 * These conditions are considered abnormal and related
 * <code>WatchDataSourceRegistry</code> behavior is considered undefined.
 */
public class WatchDataSourceRegistryTest {

    /**
     * Tests the <code>WatchDataSourceRegistry()</code> constructor.
     */
    @Test
    public void testConstructor() {
        new WatchDataSourceRegistry();
    }

    public static void main(String... args) {
        JUnitCore.main(WatchDataSourceRegistryTest.class.getName());
    }

    /**
     * Tests the <code>closeAll()</code> method.
     */
    @Test
    public void testCloseAll() {
        WatchDataSourceRegistry registry = new WatchDataSourceRegistry();

        int[] counts = new int[] {0, 1, 10, 100};
        for (int count : counts) {
            // Setup watches
            Collection<Watch> watches = new ArrayList<Watch>();
            for (int j = 0; j < count; j++) {
                String id = "watch" + j;
                LoggingWatchDataSource ds = new LoggingWatchDataSource(
                    id, EmptyConfiguration.INSTANCE);
                Watch watch = new GaugeWatch(ds, id);
                watches.add(watch);
            }

            // Populate registry
            for (Watch watch : watches) {
                registry.register(watch);
            }

            // Test closeAll()
            registry.closeAll();
            for (Watch watch : watches) {
                LoggingWatchDataSource ds =
                    (LoggingWatchDataSource) watch.getWatchDataSource();
                checkLog(ds.log(), "close()");
            }

            // Clear registry
            for (Watch watch : watches) {
                registry.deregister(watch);
                LoggingWatchDataSource ds =
                    (LoggingWatchDataSource) watch.getWatchDataSource();
                checkLog(ds.log(), "close()"); // deregister calls close
            }

            // Test closeAll()
            registry.closeAll();
            for (Watch watch : watches) {
                LoggingWatchDataSource ds =
                    (LoggingWatchDataSource) watch.getWatchDataSource();
                checkLog(ds.log(), "");
            }
        }
    }

    /**
     * Tests the <code>register(Watch)</code> and
     * <code>deregister(Watch)</code> methods.
     */
    @Test
    public void testRegisterDeregister() {
        WatchDataSourceRegistry registry = new WatchDataSourceRegistry();

        int[] counts = new int[] {0, 1, 10, 100};
        for (int count : counts) {
            Collection<Watch> watches = new ArrayList<Watch>();
            for (int k = 0; k < count; k++) {
                String id = "watch" + k;
                // Try empty name
                if (k == 1) {
                    id = "";
                }
                // We are specifying the data source explicitly because
                // otherwise every watch would use an exported proxy wich
                // gets unexported when the watch is deregistered,
                // therefore emitting annoying exceptions and slowing
                // the test badly
                WatchDataSource ds = new WatchDataSourceImpl(id,
                                                             EmptyConfiguration.INSTANCE);
                watches.add(new GaugeWatch(ds, id));
            }

            // Try registering/deregistering several times
            for (int j = 0; j < 3; j++) {
                Assert.assertEquals(0, registry.fetch().length);
                for (Watch watch : watches) {
                    String id = watch.getId();
                    Assert.assertNull(registry.findWatch(id));
                }

                for (Watch watch : watches) {
                    registry.register(watch);
                }

                Assert.assertEquals(watches.size(), registry.fetch().length);
                for (Watch watch : watches) {
                    String id = watch.getId();
                    Watch actual = registry.findWatch(id);
                    Assert.assertNotNull(actual);
                    Assert.assertSame(watch, actual);
                }

                for (Watch watch : watches) {
                    registry.deregister(watch);
                }

                Assert.assertEquals(0, registry.fetch().length);
                for (Watch watch : watches) {
                    String id = watch.getId();
                    Assert.assertNull(registry.findWatch(id));
                }
            }
        }

        try {
            registry.register((Watch[])null);
            Assert.fail("IllegalArgumentException expected but not thrown");
        } catch (IllegalArgumentException e) {
        }
        try {
            registry.deregister((Watch[])null);
            Assert.fail("IllegalArgumentException expected but not thrown");
        } catch (IllegalArgumentException e) {
        }
    }

    /**
     * Tests the <code>fetch()</code> method.
     */
    @Test
    public void testFetch1() {
        WatchDataSourceRegistry registry = new WatchDataSourceRegistry();

        int[] counts = new int[] {0, 1, 10, 100};
        for (int count : counts) {
            Set<Watch> watches = new HashSet<Watch>();
            Set<WatchDataSource> sources = new HashSet<WatchDataSource>();
            for (int k = 0; k < count; k++) {
                String id = "watch" + k;
                // Try empty name
                if (k == 1) {
                    id = "";
                }
                WatchDataSource ds = new WatchDataSourceImpl(
                    id, EmptyConfiguration.INSTANCE);
                Watch watch = new GaugeWatch(ds, id);
                watches.add(watch);
                sources.add(ds);
            }

            // Try registering/deregistering several times
            for (int j = 0; j < 5; j++) {
                Assert.assertEquals(0, registry.fetch().length);

                for (Watch watch : watches) {
                    registry.register(watch);
                }

                Set<WatchDataSource> res =
                    new HashSet<WatchDataSource>(Arrays.asList(registry.fetch()));
                Assert.assertEquals(sources, res);

                for (Watch watch : watches) {
                    registry.deregister(watch);
                }

                Assert.assertEquals(0, registry.fetch().length);
            }
        }
    }

    /**
     * Tests the <code>fetch(String)</code> method.
     */
    @Test
    public void testFetch2() {
        WatchDataSourceRegistry registry = new WatchDataSourceRegistry();

        int[] counts = new int[] {0, 1, 10, 100};
        for (int count : counts) {
            Collection<Watch> watches = new ArrayList<Watch>();
            for (int k = 0; k < count; k++) {
                String id = "watch" + k;
                // Try empty name
                if (k == 1) {
                    id = "";
                }
                // We are specifying the data source explicitly because
                // otherwise every watch would use an exported proxy wich
                // gets unexported when the watch is deregistered,
                // therefore emitting annoying exceptions and slowing
                // the test badly
                WatchDataSource ds = new WatchDataSourceImpl(
                    id, EmptyConfiguration.INSTANCE);
                watches.add(new GaugeWatch(ds, id));
            }

            // Try registering/deregistering several times
            for (int j = 0; j < 5; j++) {

                Assert.assertEquals(0, registry.fetch().length);
                for (Watch watch : watches) {
                    String id = watch.getId();
                    WatchDataSource res = registry.fetch(id);
                    Assert.assertNull(res);
                }

                for (Watch watch : watches) {
                    registry.register(watch);
                }

                Assert.assertEquals(watches.size(), registry.fetch().length);
                for (Watch watch : watches) {
                    String id = watch.getId();
                    WatchDataSource res = registry.fetch(id);
                    Assert.assertNotNull(res);
                    Assert.assertSame(watch.getWatchDataSource(), res);
                }
                Assert.assertNull(registry.fetch("aaa"));

                for (Watch watch : watches) {
                    registry.deregister(watch);
                }

                Assert.assertEquals(0, registry.fetch().length);
                for (Watch watch : watches) {
                    String id = watch.getId();
                    WatchDataSource res = registry.fetch(id);
                    Assert.assertNull(res);
                }
            }
        }

        try {
            registry.fetch(null);
            Assert.fail("IllegalArgumentException expected but not thrown");
        } catch (IllegalArgumentException e) {
        }
    }

    /**
     * Tests the <code>findWatch(String)</code> method.
     */
    @Test
    public void testFindWatch() {
        WatchDataSourceRegistry registry = new WatchDataSourceRegistry();

        int[] counts = new int[] {0, 1, 10, 100};
        for (int count : counts) {
            Collection<Watch> watches = new ArrayList<Watch>();
            for (int k = 0; k < count; k++) {
                String id = "watch" + k;
                // Try empty name
                if (k == 1) {
                    id = "";
                }
                // We are specifying the data source explicitly because
                // otherwise every watch would use an exported proxy wich
                // gets unexported when the watch is deregistered,
                // therefore emitting annoying exceptions and slowing
                // the test badly
                WatchDataSource ds = new WatchDataSourceImpl(
                    id, EmptyConfiguration.INSTANCE);
                watches.add(new GaugeWatch(ds, id));
            }

            // Try registering/deregistering several times
            for (int j = 0; j < 5; j++) {
                for (Watch watch : watches) {
                    String id = watch.getId();
                    Watch res = registry.findWatch(id);
                    Assert.assertNull(res);
                }

                for (Watch watch : watches) {
                    registry.register(watch);
                }

                for (Watch watch : watches) {
                    String id = watch.getId();
                    Watch res = registry.findWatch(id);
                    Assert.assertNotNull(res);
                    Assert.assertSame(watch, res);
                }
                Assert.assertNull(registry.findWatch("aaa"));

                for (Watch watch : watches) {
                    registry.deregister(watch);
                }

                for (Watch watch : watches) {
                    String id = watch.getId();
                    Watch res = registry.findWatch(id);
                    Assert.assertNull(res);
                }
            }
        }

        try {
            registry.findWatch(null);
            Assert.fail("IllegalArgumentException expected but not thrown");
        } catch (IllegalArgumentException e) {
        }
    }

    /**
     * Tests the <code>addThresholdListener(String,ThresholdListener)</code>
     * method.
     * <p>
     * This test does not test the non-documented dependency on the
     * listener type (<code>SLAPolicyHandler</code>-specific behavior).
     */
    @Test
    public void testAddThresholdListener() {
        WatchDataSourceRegistry registry = new WatchDataSourceRegistry();

        // Test different ordering and various numbers
        // of watches and listeners
        int[] watchCounts = new int[] {1, 5, 10};
        int[] listenerCounts = new int[] {1, 5, 10};
        for (int ordering = 0; ordering < 3; ordering++) {
            for (int watchCount : watchCounts) {
                for (int listenerCount : listenerCounts) {
                    Map<Watch, WatchEntry> map = new HashMap<Watch, WatchEntry>();
                    check(map, ordering);

                    setup(map, watchCount, listenerCount);
                    populate(registry, map, ordering);
                    check(map, ordering);

                    clear(registry, map);
                    check(new HashMap<Watch, WatchEntry>(), ordering);

                    // Check what happens if watches are re-registered
                    // and listeners are re-added
                    populate(registry, map, ordering);
                    check(map, ordering);

                    clear(registry, map);
                    check(new HashMap<Watch, WatchEntry>(), ordering);

                }
            }
        }

        try {
            registry.addThresholdListener(null, new LoggingThresholdListener());
            Assert.fail("IllegalArgumentException expected but not thrown");
        } catch (IllegalArgumentException e) {
        }
        try {
            registry.addThresholdListener("aaa", null);
            Assert.fail("IllegalArgumentException expected but not thrown");
        } catch (IllegalArgumentException e) {
        }
    }


    private void setup(Map<Watch, WatchEntry> map, int watchCount, int listenerCount) {
        for (int i = 0; i < watchCount; i++) {
            String id = "watch" + i;
            // Try empty id for one of the watches
            if (i == watchCount - 3 && i != 0) {
               id = "";
            }
            // We are specifying the data source explicitly because
            // otherwise every watch would use an exported proxy wich
            // gets unexported when the watch is deregistered,
            // therefore emitting annoying exceptions and slowing
            // the test badly
            WatchDataSource ds = new WatchDataSourceImpl(
                    id, EmptyConfiguration.INSTANCE);
            ThresholdWatch watch = new GaugeWatch(ds, id);
            watch.setThresholdValues(new ThresholdValues(0, 1));
            WatchEntry entry = new WatchEntry();
            for (int j = 0; j < listenerCount; j++) {
                entry.listeners.add(new LoggingThresholdListener());
            }
            // No listeners for one of the watches
            if (i == watchCount - 2 && i != 0) {
               entry.listeners = new ArrayList<ThresholdListener>();
            }
            // Not registering the last watch. This tests adding
            // listeners for id that never appears in the registry
            if (i == watchCount - 1 && i != 0) {
                entry.doNotRegister = true;
            }
            map.put(watch, entry);
        }
    }

    private void populate(WatchDataSourceRegistry registry,
                          Map<Watch, WatchEntry> map,
                          int ordering) {
        if (ordering == 0) {
            // All listeners first
            for (Map.Entry<Watch, WatchEntry> entry : map.entrySet()) {
                Watch watch = entry.getKey();
                WatchEntry wEntry = entry.getValue();
                for (ThresholdListener listener : wEntry.listeners) {
                    registry.addThresholdListener(watch.getId(), listener);
                }
            }
            for (Map.Entry<Watch, WatchEntry> entry : map.entrySet()) {
                Watch watch = entry.getKey();
                WatchEntry wEntry = entry.getValue();
                if (!wEntry.doNotRegister) {
                    registry.register(watch);
                }
            }
        } else if(ordering == 1) {
            // All watches first
            for (Map.Entry<Watch, WatchEntry> entry : map.entrySet()) {
                Watch watch = entry.getKey();
                WatchEntry wEntry = entry.getValue();
                if (!wEntry.doNotRegister) {
                    registry.register(watch);
                }
            }
            for (Map.Entry<Watch, WatchEntry> e : map.entrySet()) {
                Watch watch = e.getKey();
                WatchEntry entry =  e.getValue();
                for (ThresholdListener listener : entry.listeners) {
                    registry.addThresholdListener(watch.getId(), listener);
                }
            }
        } else {
            // Interleaving
            boolean watchFirst = true;
            for (Map.Entry<Watch, WatchEntry> e : map.entrySet()) {
                Watch watch = e.getKey();
                WatchEntry entry = e.getValue();
                if (watchFirst) {
                    if (!entry.doNotRegister) {
                        registry.register(watch);
                    }
                }
                for (ThresholdListener listener : entry.listeners) {
                    registry.addThresholdListener(watch.getId(), listener);
                }
                if (!watchFirst) {
                    if (!entry.doNotRegister) {
                        registry.register(watch);
                    }
                }
                watchFirst = !watchFirst;
            }
        }
    }

    private void clear(WatchDataSourceRegistry registry,
                       Map<Watch, WatchEntry> map) {
        for (Watch w : map.keySet()) {
            registry.deregister(w);
        }
    }

    private void check(Map<Watch, WatchEntry> map,
                       int ordering) {
        // This method needs to know the population index because
        // the effect of using the watch registry changes from
        // population to population. This is mainly because the
        // watch registry does not cleanup the id <-> listener
        // mapping when watches are deregistered).
        boolean watchFirst = true;
        for (Map.Entry<Watch, WatchEntry> e : map.entrySet()) {
            Watch watch = e.getKey();
            String id = watch.getId();

            watch.addWatchRecord(new Calculable(id, 1.1));
            watch.addWatchRecord(new Calculable(id, 0.5));
            WatchEntry entry = e.getValue();
            List listeners = entry.listeners;

            for (int j = 0; j < listeners.size(); j++) {
                LoggingThresholdListener lnr =
                        (LoggingThresholdListener) listeners.get(j);

                String stm = "setThresholdManager()";
                String prefix;
                if (ordering == 0) {
                    prefix = replicate(stm, 1);
                } else if (ordering == 1) {
                    int base = listeners.size();
                    prefix = replicate(stm, base - j);
                } else {
                    if (watchFirst) {
                        int base = listeners.size();
                        prefix = replicate(stm, base - j);
                    } else {
                        prefix = replicate(stm, 1);
                    }
                }

                if (!entry.doNotRegister) {
                    checkLog(lnr.log(), prefix + "breach(" + id + ":1.1)"
                                               + "clear(" + id + ":0.5)");
                } else {
                    checkLog(lnr.log(), "");
                }
            }
            watchFirst = !watchFirst;
        }
    }

    /*
     * Replicates a given string a given number of times.
     */
    private String replicate(String s, int count) {
        String res = "";
        for (int i = 0; i < count; i++) {
            res += s;
        }
        return res;
    }


    /**
     * The class holds data that describes one watch used in the test.
     */
    private class WatchEntry {
        public List<ThresholdListener> listeners = new ArrayList<ThresholdListener>();
        public boolean doNotRegister = false;
    }


    /**
     * The class extends <code>WatchDataSourceImpl</code> and logs
     * all calls to the <code>close()</code> method into a string buffer.
     */
    private class LoggingWatchDataSource extends WatchDataSourceImpl {

        private StringBuffer log = new StringBuffer();

        public LoggingWatchDataSource(String id,
                                      Configuration config) {
            super(id, config);
        }

        public StringBuffer log() {
            return log;
        }

        public void close() {
            log.append("close()");
            super.close();
        }
    }


    /**
     * The class provides a mock implementation of the
     * <code>ThresholdListener</code> interface. The main function
     * of the class is to receive threshold events and log them
     * into a string buffer.
     */
    private class LoggingThresholdListener implements SettableThresholdListener {

        StringBuffer log;

        public LoggingThresholdListener() {
            this.log = new StringBuffer();
        }

        public void notify(Calculable calculable, ThresholdValues thresholdValues, ThresholdType type) {
            log.append(type == ThresholdType.BREACHED ? "breach(" : "clear(");
            log.append(calculable.getId());
            log.append(":");
            log.append(calculable.getValue());
            log.append(")");
        }

        public void setThresholdManager(ThresholdManager thresholdManager) {
            log.append("setThresholdManager()");
            thresholdManager.addThresholdListener(this);
        }

        public StringBuffer log() {
            return log;
        }

        public String getID() {
            return null;
        }
    }

    /*
     * Checks that a given log holds a given string,
     * and clears the log.
     */
    private void checkLog(StringBuffer log, String s) {
        Assert.assertEquals(s, log.toString());
        log.delete(0, log.length());
    }
}
