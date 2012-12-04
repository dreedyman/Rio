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
import net.jini.export.Exporter;
import net.jini.jeri.BasicILFactory;
import net.jini.jeri.BasicJeriExporter;
import net.jini.jeri.tcp.TcpServerEndpoint;
import org.junit.Test;
import org.rioproject.config.DynamicConfiguration;

import java.rmi.*;
import java.rmi.server.ExportException;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * The class tests the <code>WatchDataSourceImpl</code> class against its
 * javadoc specification. The class tests the public constructors, methods,
 * and fields declared in <code>WatchDataSourceImpl</code>.
 */
public class WatchDataSourceImplTest {
    static final Logger logger = LoggerFactory.getLogger("org.rioproject.watch");
    /**
     * The set of possible boolean values.
     */
    private static final Boolean[] booleanAxis =
            new Boolean[] {Boolean.FALSE, Boolean.TRUE};


    /**
     * Tests the <code>DEFAULT_COLLECTION_SIZE</code> field.
     */
    @Test public void testFields() {
        Assert.assertTrue(WatchDataSourceImpl.DEFAULT_COLLECTION_SIZE > 0);
        Assert.assertTrue(WatchDataSourceImpl.MAX_COLLECTION_SIZE > 0);
        Assert.assertTrue(WatchDataSourceImpl.MAX_COLLECTION_SIZE
                          >= WatchDataSourceImpl.DEFAULT_COLLECTION_SIZE);
    }

    /**
     * Tests the
     * <code>WatchDataSourceImpl(String, Configuration)</code>
     * constructor.
     *
     * @throws RemoteException if the test fails
     */
    @Test public void testConstructor() throws RemoteException {
        final int DCS = WatchDataSourceImpl.DEFAULT_COLLECTION_SIZE;
        final int MCS = WatchDataSourceImpl.MAX_COLLECTION_SIZE;
        final String[] ids = new String[] {"watch", ""};
        final WatchDataReplicator[] wdrs = new WatchDataReplicator[] {
                new LoggingWatchDataReplicator(), null};
        final Integer[] collectionSizes = ArrayUtils.asObjects(new int[] {
                -1, 0, 1, 10, 1000, 9999, 10000, 10001, 20000, 666});
        Object[][] combinations = ArrayUtils.combinations(new Object[][] {
                ids, wdrs, collectionSizes});
        for (Object[] combination : combinations) {
            String id = (String) combination[0];
            WatchDataReplicator wdr = (WatchDataReplicator) combination[1];
            int collectionSize = (Integer) combination[2];
            DynamicConfiguration config = new DynamicConfiguration();
            if (collectionSize != 666) {
                config.setEntry("org.rioproject.watch",
                                "collectionSize",
                                collectionSize);
            }
            WatchDataSourceImpl impl = new WatchDataSourceImpl(id, config);
            boolean added = impl.addWatchDataReplicator(wdr);
            if(wdr!=null)
                Assert.assertTrue("Expected to add "+wdr.getClass().getName(),
                                  added);
            else
                Assert.assertFalse("Expected to not add a null WatchDataReplicator",
                                   added);

            Assert.assertSame(id, impl.getID());
            if (wdr == null) {
                Assert.assertNotNull(impl.getWatchDataReplicators());
                Assert.assertTrue(impl.getWatchDataReplicators().length==0);
            } else {
                Assert.assertSame(wdr, impl.getWatchDataReplicators()[0]);
            }
            if (collectionSize == 666 || collectionSize < 1
                || collectionSize > MCS) {
                Assert.assertEquals(DCS, impl.getMaxSize());
            } else {
                Assert.assertEquals(collectionSize, impl.getMaxSize());
            }

            Assert.assertNull(impl.getProxy());
            assertAddCalculableWorks(impl,
                                     Math.min(Math.max(collectionSize, 0), 10),
                                     true);

            impl.close();
        }

        try {
            new WatchDataSourceImpl(null, new DynamicConfiguration());
            Assert.fail("IllegalArgumentException expected but not thrown");
        } catch (IllegalArgumentException e) {
        }
        try {
            new WatchDataSourceImpl("watch", null);
            Assert.fail("IllegalArgumentException expected but not thrown");
        } catch (IllegalArgumentException e) {
        }
        try {
            new WatchDataSourceImpl(null, null);
            Assert.fail("IllegalArgumentException expected but not thrown");
        } catch (IllegalArgumentException e) {
        }
    }

    /**
     * Tests the <code>export()</code> method.
     *
     ** @throws Exception if the test fails
     */
    @Test public void testExport() throws Exception {
        final int N = 3;
        Integer[] counts = new Integer[N];
        for (int i = 0; i < N; i++) {
            counts[i] = i;
        }
        Object[][] combinations = ArrayUtils.combinations(new Object[][] {
                counts, counts, booleanAxis});
        int i = 0;
        while (i < combinations.length) {
            int exportCount = (Integer) combinations[i][0];
            int unexportCount = (Integer) combinations[i][1];
            boolean force = (Boolean) combinations[i][2];

            LoggingWatchDataSourceImpl impl = new LoggingWatchDataSourceImpl(
                "watch", null, EmptyConfiguration.INSTANCE);

            List<WatchDataSource> proxies = new ArrayList<WatchDataSource>();
            for (int j = 0; j < exportCount; j++) {
                proxies.add(impl.export());
            }
            for (int j = 0; j < unexportCount; j++) {
                impl.unexport(force);
            }
            if (exportCount > 0 && unexportCount == 0 ) {
                Assert.assertNotNull(impl.getProxy());
            } else {
                Assert.assertNull(impl.getProxy());
            }

            WatchDataSource proxy = impl.export();

            Assert.assertNotNull(proxy);
            assertValidProxy(proxy, impl);
            Assert.assertSame(proxy, impl.getProxy());
            if (proxies.size() > 0) {
                assertAllSame(proxies);
                if (unexportCount == 0) {
                    Assert.assertSame(proxies.get(0), proxy);
                } else {
                    Assert.assertNotSame(proxies.get(0), proxy);
                }
            }
            assertAddCalculableWorks(impl, 10, true);

            impl.close();
            i++;
        }
    }

    /**
     * Tests the <code>unexport()</code> method.
     *
     * @throws Exception if the test fails
     */
    @Test public void testUnexport() throws Exception {
        final int N = 3;
        Integer[] counts = new Integer[N];
        for (int i = 0; i < N; i++) {
            counts[i] = i;
        }
        Object[][] combinations = ArrayUtils.combinations(new Object[][] {
                counts, counts, booleanAxis});
        for (Object[] combination : combinations) {
            int exportCount = (Integer) combination[0];
            int unexportCount = (Integer) combination[1];
            boolean force = (Boolean) combination[2];

            LoggingWatchDataSourceImpl impl = new LoggingWatchDataSourceImpl(
                "watch", null, EmptyConfiguration.INSTANCE);

            List<WatchDataSource> proxies = new ArrayList<WatchDataSource>();
            for (int j = 0; j < exportCount; j++) {
                proxies.add(impl.export());
            }
            for (int j = 0; j < unexportCount; j++) {
                impl.unexport(force);
            }
            if (exportCount > 0 && unexportCount == 0) {
                Assert.assertNotNull(impl.getProxy());
            } else {
                Assert.assertNull(impl.getProxy());
            }

            impl.unexport(force);

            Assert.assertNull(impl.getProxy());
            if (proxies.size() > 0) {
                assertAllSame(proxies);
                assertInvalidProxy(proxies.get(0), impl);
            }
            assertAddCalculableWorks(impl, 10, true);

            impl.close();
        }
    }

    /**
     * Tests the <code>getProxy()</code> method.
     *
     * @throws Exception if the test fails
     */
    @Test public void testGetProxy() throws Exception {
        final int N = 5;
        Integer[] counts = new Integer[N];
        for (int i = 0; i < N; i++) {
            counts[i] = i;
        }
        Object[][] combinations = ArrayUtils.combinations(new Object[][] {
                counts, counts, booleanAxis});
        for (Object[] combination : combinations) {
            int exportCount = (Integer) combination[0];
            int unexportCount = (Integer) combination[1];
            boolean force = (Boolean) combination[2];

            LoggingWatchDataSourceImpl impl = new LoggingWatchDataSourceImpl(
                "watch", null, EmptyConfiguration.INSTANCE);

            for (int j = 0; j < exportCount; j++) {
                impl.export();
            }
            for (int j = 0; j < unexportCount; j++) {
                impl.unexport(force);
            }

            if (exportCount > 0 && unexportCount == 0) {
                Assert.assertNotNull(impl.getProxy());
                assertValidProxy(impl.getProxy(), impl);
            } else {
                Assert.assertNull(impl.getProxy());
            }

            impl.close();
        }
    }

    /**
     * Tests the <code>getID()</code> method.
     *
     * @throws Exception if the test fails
     */
    @Test public void testGetID() throws Exception {
        WatchDataSourceImpl impl = new WatchDataSourceImpl();
        impl.setID("watch");
        Assert.assertEquals("watch", impl.getID());

        impl.setID("aaa");
        Assert.assertEquals("aaa", impl.getID());

        impl.close();
    }

    /**
     * Tests the <code>setID(String)</code> method.
     *
     * @throws Exception if the test fails
     */
    @Test public void testSetID() throws Exception {
        WatchDataSourceImpl impl = new WatchDataSourceImpl();
        impl.setID("aaa");
        Assert.assertEquals("aaa", impl.getID());

        impl.setID("");
        Assert.assertEquals("", impl.getID());

        try {
            impl.setID("bbb");
            impl.setID(null);
            Assert.fail("IllegalArgumentException expected but not thrown");
        } catch (IllegalArgumentException e) {
        }
        Assert.assertEquals("bbb", impl.getID());

        impl.close();
    }

    /**
     * Tests the <code>getOffset()</code> method.
     * @noinspection UNUSED_SYMBOL

    private void testGetOffset() throws Exception {
        WatchDataSourceImpl impl = new WatchDataSourceImpl(
                "watch", null, EmptyConfiguration.INSTANCE);

        final int DCS = WatchDataSourceImpl.DEFAULT_COLLECTION_SIZE;
        for (int i = 0; i < DCS; i++) {
            impl.addCalculable(new Calculable());
        }
        DataSourceMonitor mon = new DataSourceMonitor(impl);
        mon.waitFor(DCS);

        for (int i = 0; i < 10; i++) {
            impl.addCalculable(new Calculable());
            mon.waitFor(DCS + i + 1);
        }

        impl.close();
    }
    */
    /**
     * Tests the <code>getMaxSize()</code> method.
     *
     * @throws Exception if the test fails
     */
    @Test public void testGetSize() throws Exception {
        final int DCS = WatchDataSourceImpl.DEFAULT_COLLECTION_SIZE;

        {   // Default initial size
            WatchDataSourceImpl impl = new WatchDataSourceImpl();
            impl.setID("watch");
            impl.setConfiguration(EmptyConfiguration.INSTANCE);

            // Non-modified
            Assert.assertEquals(DCS, impl.getMaxSize());

            // Modified
            impl.setMaxSize(DCS + 10);
            Assert.assertEquals(DCS + 10, impl.getMaxSize());

            impl.close();
        }

        {   // Non-default initial size
            DynamicConfiguration config = new DynamicConfiguration();
            config.setEntry("org.rioproject.watch", "collectionSize",
                            DCS + 15);
            WatchDataSourceImpl impl = new WatchDataSourceImpl();
            impl.setID("watch");
            impl.setConfiguration(config);
            impl.initialize();

            // Non-modified
            Assert.assertEquals(DCS + 15, impl.getMaxSize());

            // Modified
            impl.setMaxSize(DCS + 20);
            Assert.assertEquals(DCS + 20, impl.getMaxSize());

            impl.close();
        }
    }

    /**
     * Tests the <code>setMaxSize(int)</code> method.
     *
     * @throws Exception if the test fails
     */
    @Test public void testSetSize() throws Exception {
        final int DCS = WatchDataSourceImpl.DEFAULT_COLLECTION_SIZE;
        final int MCS = WatchDataSourceImpl.MAX_COLLECTION_SIZE;
        final int[] sizes = new int[] {-100, -2, -1, 0, 1, 2, DCS,
                MCS - 1, MCS, MCS + 1, MCS + 5};
        for (int size : sizes) {
            WatchDataSourceImpl impl = new WatchDataSourceImpl();
            impl.setID("watch");
            impl.setConfiguration(EmptyConfiguration.INSTANCE);
            // TODO: Change when fixed
            if (size < -1) {
                try {
                    impl.setMaxSize(size);
                    Assert.fail("IndexOutOfBoundsException expected"
                                + " but not thrown");
                } catch (IndexOutOfBoundsException e) {
                }
            } else {
                impl.setMaxSize(size);
                Assert.assertEquals(size, impl.getMaxSize());
            }
            impl.close();
        }
    }

    /**
     * Tests the <code>clear()</code> method.
     *
     * @throws Exception if the test fails
     */
    @Test public void testClear() throws Exception {
        final int DCS = WatchDataSourceImpl.DEFAULT_COLLECTION_SIZE;
        WatchDataSourceImpl impl = new WatchDataSourceImpl();
        impl.setID("watch");
        impl.setConfiguration(EmptyConfiguration.INSTANCE);

        for (int j = 0; j < DCS; j++) {
            impl.addCalculable(new Calculable());
        }
        DataSourceMonitor mon = new DataSourceMonitor(impl);
        mon.waitFor(DCS);
        int expected = Math.min(DCS, DCS);
        Assert.assertEquals(expected, impl.getCalculable().length);

        impl.clear();
        Assert.assertEquals(0, impl.getCalculable().length);

        impl.close();
    }

    /**
     * Tests the <code>getCurrentSize()</code> method.
     *
     * @throws Exception if the test fails
     */
    @Test public void testGetCurrentSize() throws Exception {
        final int DCS = WatchDataSourceImpl.DEFAULT_COLLECTION_SIZE;
        WatchDataSourceImpl impl = new WatchDataSourceImpl();
        impl.setID("watch");
        impl.setConfiguration(EmptyConfiguration.INSTANCE);

        for (int j = 0; j < DCS; j++) {
            impl.addCalculable(new Calculable());
        }
        DataSourceMonitor mon = new DataSourceMonitor(impl);
        mon.waitFor(DCS);

        int expected = Math.min(DCS, DCS);
        Assert.assertEquals(expected, impl.getCurrentSize());

        impl.close();
    }

    /**
     * Tests the <code>addCalculable(Calculable)</code> method.
     *
     * @throws Exception if the test fails
     */
    @Test public void testAddCalculable() throws Exception {
        doTestAddCalculable(LoggingWatchDataReplicator.class.getName());
        doTestAddCalculable(RemoteWDR.class.getName());
    }

    private void doTestAddCalculable(String wdrClass) throws Exception {
        final int DCS = WatchDataSourceImpl.DEFAULT_COLLECTION_SIZE;
        final int MCS = WatchDataSourceImpl.MAX_COLLECTION_SIZE;
        final Integer[] collectionSizes = ArrayUtils.asObjects(new int[] {1, DCS, MCS});
        Object[][] combinations = ArrayUtils.combinations(new Object[][] {collectionSizes, booleanAxis});
        for (Object[] combination : combinations) {
            int collectionSize = (Integer) combination[0];
            boolean nullCalculable = (Boolean) combination[1];
            final int[] counts = new int[]{0,
                                           1,
                                           collectionSize - 1,
                                           collectionSize,
                                           collectionSize + 1,
                                           collectionSize + 10};
            for (int count : counts) {
                LoggingWatchDataReplicator wdr;
                if(wdrClass.equals(RemoteWDR.class.getName()))
                    wdr = new RemoteWDR();
                else
                    wdr = new LoggingWatchDataReplicator();
                DynamicConfiguration config = new DynamicConfiguration();
                config.setEntry("org.rioproject.watch", "collectionSize", collectionSize);

                WatchDataSourceImpl impl = new WatchDataSourceImpl();
                impl.setID("watch");
                impl.setConfiguration(config);
                if(wdrClass.equals(RemoteWDR.class.getName()))
                    impl.addWatchDataReplicator(((RemoteWDR)wdr).getWatchDataReplicator());
                else
                    impl.addWatchDataReplicator(wdr);
                impl.initialize();

                /*System.out.println(
                    "WDS max size=" + impl.getMaxSize() + ", " +
                    "configured to be=" + collectionSize + ", " +
                    "count is=" + count);*/

                List<Calculable> expected = new ArrayList<Calculable>();
                for (int k = 0; k < count; k++) {
                    Calculable c = new Calculable();
                    impl.addCalculable(c);
                    expected.add(c);
                }

                if (nullCalculable) {
                    try {
                        impl.addCalculable(null);
                        Assert.fail("IllegalArgumentException expected but not thrown");
                    } catch (IllegalArgumentException e) {
                    }
                } else {
                    Calculable c = new Calculable();
                    impl.addCalculable(c);
                    expected.add(c);
                }

                long waited = 0;
                if(wdrClass.equals(RemoteWDR.class.getName())) {
                    int maxIterations = 10000;
                    int iteration = 0;
                    long current = System.currentTimeMillis();
                    while(expected.size()!=wdr.calculables().size() && iteration<maxIterations) {
                        Utils.sleep(1);
                        iteration++;
                    }
                    waited = System.currentTimeMillis()-current;
                }
                // Replicator should have all the data
                System.out.println(
                    "WDS size=" + impl.getCurrentSize() + ", " +
                    "Replicator ("+wdrClass.substring(wdrClass.indexOf("$")+1,wdrClass.length())+") " +
                    "size=" + wdr.calculables().size() +", expected size=" + expected.size()+
                    (waited==0?"":", waited="+waited+" ms"));
                Assert.assertEquals(expected.size(), wdr.calculables().size());
                Utils.assertEqualContents(expected, wdr.calculables());                

                // Data source should contain the tail
                int off = Math.max(expected.size() - collectionSize, 0);
                expected = expected.subList(off, expected.size());
                Calculable[] res = impl.getCalculable();
                Utils.assertSameContents(expected, Arrays.asList(res));

                impl.close();

                if(wdrClass.equals(RemoteWDR.class.getName()))
                    wdr.close();
            }
        }
    }

    /**
     * Tests the <code>getWatchDataReplicator()</code> method.
     *
     * @throws Exception if the test fails
     */
    @Test public void testGetWatchDataReplicator() throws Exception {
        WatchDataReplicator wdr1 = new LoggingWatchDataReplicator();
        WatchDataSourceImpl impl = new WatchDataSourceImpl();
        impl.setID("watch");
        impl.setConfiguration(new DynamicConfiguration());
        impl.addWatchDataReplicator(wdr1);
        Assert.assertSame(wdr1, impl.getWatchDataReplicators()[0]);

        WatchDataReplicator wdr2 = new LoggingWatchDataReplicator();
        impl.addWatchDataReplicator(wdr2);
        Assert.assertEquals(2, impl.getWatchDataReplicators().length);
        Assert.assertSame(wdr2, impl.getWatchDataReplicators()[1]);

        impl.close();
    }

    /**
     * Tests the <code>addWatchDataReplicator(WatchDataReplicator)</code> method.
     *
     * @throws Exception if the test fails
     */
    @Test public void testAddWatchDataReplicator() throws Exception {
        WatchDataSourceImpl impl = new WatchDataSourceImpl();
        impl.setID("watch");
        impl.setConfiguration(new DynamicConfiguration());

        WatchDataReplicator wdr1 = new LoggingWatchDataReplicator();
        impl.addWatchDataReplicator(wdr1);
        Assert.assertSame(wdr1, impl.getWatchDataReplicators()[0]);

        WatchDataReplicator wdr2 = new LoggingWatchDataReplicator();
        impl.addWatchDataReplicator(wdr2);
        Assert.assertSame(wdr2, impl.getWatchDataReplicators()[1]);

        impl.addWatchDataReplicator(null);
        Assert.assertNotNull(impl.getWatchDataReplicators());
        Assert.assertTrue(impl.getWatchDataReplicators().length==2);

        impl.close();
    }

    /**
     * Tests the <code>getCalculable()</code> method.
     *
     * @throws Exception if the test fails
     */
    @Test public void testGetCalculable1() throws Exception {
        final int DCS = WatchDataSourceImpl.DEFAULT_COLLECTION_SIZE;
        WatchDataSourceImpl impl = new WatchDataSourceImpl();
        impl.setID("watch");
        impl.setConfiguration(EmptyConfiguration.INSTANCE);
        List<Calculable> expected = new ArrayList<Calculable>();
        for (int j = 0; j < DCS; j++) {
            Calculable c = new Calculable();
            impl.addCalculable(c);
            expected.add(c);
        }
        DataSourceMonitor mon = new DataSourceMonitor(impl);
        mon.waitFor(DCS);

        int off = Math.max(expected.size() - DCS, 0);
        expected = expected.subList(off, expected.size());
        Calculable[] res = impl.getCalculable();
        Utils.assertSameContents(expected, Arrays.asList(res));

        impl.close();
    }

    /**
     * Tests the <code>getLastCalculable()<code> method.
     *
     * @throws Exception if the test fails
     */
    @Test public void testGetLastCalculable1() throws Exception {
        final int DCS = WatchDataSourceImpl.DEFAULT_COLLECTION_SIZE;
        WatchDataSourceImpl impl = new WatchDataSourceImpl();
        impl.setID("watch");
        impl.setConfiguration(EmptyConfiguration.INSTANCE);
        Calculable expected = null;
        for (int j = 0; j < DCS; j++) {
            Calculable c = new Calculable();
            impl.addCalculable(c);
            expected = c;
        }
        DataSourceMonitor mon = new DataSourceMonitor(impl);
        mon.waitFor(DCS);

        Assert.assertSame(expected, impl.getLastCalculable());

        impl.close();
    }

    /**
     * Tests the <code>getThresholdValues()<code> method.
     *
     * @throws Exception if the test fails
     */
    @Test public void testGetThresholdValues() throws Exception {
        WatchDataSourceImpl impl = new WatchDataSourceImpl();
        impl.setID("watch");
        impl.setConfiguration(EmptyConfiguration.INSTANCE);
        ThresholdValues tv = impl.getThresholdValues();
        Assert.assertNotNull(tv);

        tv = new ThresholdValues();
        impl.setThresholdValues(tv);
        Assert.assertSame(tv, impl.getThresholdValues());

        impl.close();
    }

    /**
     * Tests the <code>getShresholdValues<code> method.
     *
     * @throws Exception if the test fails
     */
    @Test public void testSetThresholdValues() throws Exception {
        WatchDataSourceImpl impl = new WatchDataSourceImpl();
        impl.setID("watch");
        impl.setConfiguration(EmptyConfiguration.INSTANCE);

        ThresholdValues tv = new ThresholdValues();
        impl.setThresholdValues(tv);
        Assert.assertSame(tv, impl.getThresholdValues());

        // TODO: Change when fixed
        impl.setThresholdValues(null);
        Assert.assertSame(tv, impl.getThresholdValues());

        impl.close();
    }

    /**
     * Tests the <code>close()<code> method.
     *
     * @throws Exception if the test fails
     */
    @Test public void testClose() throws Exception {
        final int N = 3;
        Integer[] counts = new Integer[N];
        for (int i = 0; i < N; i++) {
            counts[i] = i;
        }
        Object[][] combinations = ArrayUtils.combinations(new Object[][] {
                counts, counts, counts});
        for (Object[] combination : combinations) {
            int exportCount = (Integer) combination[0];
            int unexportCount = (Integer) combination[1];
            int closeCount = (Integer) combination[2];

            LoggingWatchDataReplicator watchDataReplicator = new LoggingWatchDataReplicator();
            LoggingWatchDataSourceImpl impl = new LoggingWatchDataSourceImpl(
                "watch", watchDataReplicator, EmptyConfiguration.INSTANCE);
            // TODO: Without this sleep the writer thread often
            // TODO: starts after close() is called, therefore
            // TODO: close() does not really stop the writer
            // TODO: thread
            Utils.sleep(200);

            WatchDataSource proxy = null;
            for (int j = 0; j < exportCount; j++) {
                proxy = impl.export();
            }
            for (int j = 0; j < unexportCount; j++) {
                impl.unexport(true);
            }
            for (int j = 0; j < closeCount; j++) {
                impl.close();
            }

            impl.close();

            // The previous proxy (if any) should be unexported
            if (proxy != null) {
                assertInvalidProxy(proxy, impl);
            }
            // The data source must have no proxy
            Assert.assertNull(impl.getProxy());
            // Adding should not work
            assertAddCalculableWorks(impl, 10, false);
            // close() should close and clear the WatchDataReplicators
            Assert.assertEquals(0, impl.getWatchDataReplicators().length);
            checkLog(watchDataReplicator.log(), "close()");
        }
    }

    /**
     * Tests the <code>getView()</code> method.
     *
     * @throws Exception if the test fails
     */
    @Test public void testGetView() throws Exception {
        WatchDataSourceImpl impl = new WatchDataSourceImpl();
        impl.setID("watch");
        impl.setConfiguration(EmptyConfiguration.INSTANCE);
        Assert.assertEquals(null, impl.getView());

        impl.setView("abcd");
        Assert.assertEquals("abcd", impl.getView());

        impl.close();
    }

    /**
     * Tests the <code>setView(String)</code> method.
     *
     * @throws Exception if the test fails
     */
    @Test public void testSetView() throws Exception {
        WatchDataSourceImpl impl = new WatchDataSourceImpl();
        impl.setID("watch");
        impl.setConfiguration(EmptyConfiguration.INSTANCE);

        impl.setView("abcd");
        Assert.assertEquals("abcd", impl.getView());

        impl.setView("");
        Assert.assertEquals("", impl.getView());

        impl.setView(null);
        Assert.assertEquals(null, impl.getView());

        impl.close();
    }

    /**
     * Tests the <code>getProxyVerifier()</code> method.
     *
     * @throws Exception if the test fails
     */
    @Test
    public void testGetProxyVerifier() throws Exception {
        final int N = 3;
        Integer[] counts = new Integer[N];
        for (int i = 0; i < N; i++) {
            counts[i] = i;
        }
        Object[][] combinations = ArrayUtils.combinations(new Object[][] {
                counts, counts});
        for (Object[] combination : combinations) {
            int exportCount = (Integer) combination[0];
            int unexportCount = (Integer) combination[1];

            WatchDataSourceImpl impl = new WatchDataSourceImpl();
            impl.setID("watch");
            impl.setConfiguration(EmptyConfiguration.INSTANCE);

            for (int j = 0; j < exportCount; j++) {
                impl.export();
            }
            for (int j = 0; j < unexportCount; j++) {
                impl.unexport(true);
            }

            if (exportCount == 0 || unexportCount > 0) {
                // TODO: Is this correct? IllegalStateException is better 
                try {
                    impl.getProxyVerifier();
                    Assert.fail("IllegalArgumentException expected"
                                + " but not thrown");
                } catch (IllegalArgumentException e) {
                }
            } else {
                Assert.assertNotNull(impl.getProxyVerifier());
            }

            impl.close();
        }
    }

    /**
     * The class extends <code>WatchDataSourceImpl</code> and logs most
     * <code>WatchDataSource</code> method calls into a string buffer.
     */
    private class LoggingWatchDataSourceImpl extends WatchDataSourceImpl {

        private StringBuffer log = new StringBuffer();

        public LoggingWatchDataSourceImpl(String id,
                                          WatchDataReplicator wdr,
                                          Configuration config) {
            super();
            setID(id);
            setConfiguration(config);
            addWatchDataReplicator(wdr);
        }

        public StringBuffer log() {
            return log;
        }

        public String getID() {
            log.append("getID()");
            return super.getID();
        }

        public void setMaxSize(int size) {
            log.append("setMaxSize(").append(size).append(")");
            super.setMaxSize(size);
        }

        public int getMaxSize() {
            log.append("getMaxSize()");
            return super.getMaxSize();
        }

        public void clear() {
            log.append("clear()");
            super.clear();
        }

        public int getCurrentSize() {
            log.append("getCurrentSize()");
            return super.getCurrentSize();
        }

        public Calculable[] getCalculable() {
            log.append("getCalculable()");
            return super.getCalculable();
        }

        public Calculable getLastCalculable() {
            log.append("getLastCalculable()");
            return super.getLastCalculable();
        }

        public ThresholdValues getThresholdValues() {
            log.append("getThresholdValues()");
            return super.getThresholdValues();
        }

        public void close() {
            log.append("close()");
            super.close();
        }

        public void setView(String view) {
            log.append("setView(").append(view).append(")");
            super.setView(view);
        }

        public String getView() {
            log.append("getView()");
            return super.getView();
        }
    }

    /*
     * Asserts that a given object is a actually a proxy to a given
     * implementation object.
     */
    private void assertValidProxy(WatchDataSource proxy,
                                  LoggingWatchDataSourceImpl impl)
            throws RemoteException {
        impl.log().delete(0, impl.log().length());
        proxy.clear();
        proxy.getCalculable();
        proxy.getCurrentSize();
        proxy.getID();
        proxy.getLastCalculable();
        proxy.getMaxSize();
        proxy.getThresholdValues();
        proxy.getView();
        //proxy.setMaxSize(1);
        proxy.setView("aaa");
        checkLog(impl.log, "clear()"
                           + "getCalculable()"
                           //+ "getCalculable(0,1)"
                           //+ "getCalculable(watch,0,1)"
                           + "getCurrentSize()"
                           + "getID()"
                           + "getLastCalculable()"
                           + "getMaxSize()"
                           + "getThresholdValues()"
                           + "getView()"
                           //+ "setMaxSize(1)"
                           + "setView(aaa)");
    }

    /*
     * Asserts that a given object is not a working proxy
     * (e.g. closed proxy) to a given implementation object.
     */
    private void assertInvalidProxy(WatchDataSource proxy,
                                    LoggingWatchDataSourceImpl impl)
            throws RemoteException {
        impl.log().delete(0, impl.log().length());
        try {
            proxy.getID();
            Assert.fail("Exception expected but not thrown");
        } catch (NoSuchObjectException e) {
        } catch (ConnectException e) {
        } catch (ConnectIOException e) {            
        }
        checkLog(impl.log(), "");
    }


    /**
     * The class provides an implementation of the <code>WatchDataReplicator</code>
     * interface that logs <code>close()</code> method calls into a string
     * buffer and stores samples passed to the <code>addCalculable()</code> method
     * in a list.
     */
    private class LoggingWatchDataReplicator implements WatchDataReplicator {
        private StringBuffer log = new StringBuffer();
        protected List<Calculable> calculables = new ArrayList<Calculable>();

        public LoggingWatchDataReplicator() {
        }

        public StringBuffer log() {
            return log;
        }

        public List calculables() {
            return calculables;
        }

        public void close() {
            log.append("close()");
        }

        public void addCalculable(Calculable calculable) {            
            calculables.add(calculable);
        }
    }

    private class RemoteWDR extends LoggingWatchDataReplicator implements RemoteWatchDataReplicator {
        Exporter exporter;

        public RemoteWDR() {
            super();
        }

        WatchDataReplicator getWatchDataReplicator() throws ExportException {
            exporter = new BasicJeriExporter(TcpServerEndpoint.getInstance(0),
                                             new BasicILFactory(),
                                             false,
                                             true);
            RemoteWatchDataReplicator backend = (RemoteWatchDataReplicator)exporter.export(this);
            return WatchDataReplicatorProxy.getInstance(backend, UUID.randomUUID());
        }

        public void replicate(Calculable c) {
            addCalculable(c);
        }

        public void bulkReplicate(Collection<Calculable> c)  {
            calculables.addAll(c);
        }

        @Override
        public void close() {
            exporter.unexport(true);
            super.close();
        }
    }

    /*
     * Checks that a given string buffer (used as a log) holds a given
     * string, and clears the string buffer.
     */
    private void checkLog(StringBuffer log, String s) {
        Assert.assertEquals(s, log.toString());
        log.delete(0, log.length());
    }


    /*
     * Asserts that all objects in a given collection are same.
     */
    private void assertAllSame(Collection collection) {
        if (collection.isEmpty()) {
            return;
        }
        Iterator i = collection.iterator();
        Object obj = i.next();
        while (i.hasNext()) {
            Assert.assertSame(obj, i.next());
        }
    }

    /*
     * Asserts that the <code>addCalculable</code> method really works for
     * the current state of a given data source, that is, calculables get
     * into the history. The method verifies this by calling
     * <code>addCalculable</code> the specified number of times and
     * checking the history.
     *
     * @param ds    the data source to verify
     * @param count the required number of calls to <code>addCalculable</code>
     * @param works specifies whether <code>addCalculable</code> should
     *              work or not
     */
    private void assertAddCalculableWorks(WatchDataSource ds,
                                          int count,
                                          boolean works) throws RemoteException {
        ds.clear();
        List<Calculable> expected = new ArrayList<Calculable>();
        Calculable expectedLast = null;
        for (int j = 0; j < count; j++) {
            Calculable c = new Calculable();
            ds.addCalculable(c);
            if (works) {
                expected.add(c);
                expectedLast = c;
            }
        }

        DataSourceMonitor detector = new DataSourceMonitor(ds);
        detector.waitFor(works ? count : 0);

        List calculables = Arrays.asList(ds.getCalculable());
        Utils.assertSameContents(expected, calculables);
        Assert.assertEquals(expected.size(), ds.getCurrentSize());
        Assert.assertSame(expectedLast, ds.getLastCalculable());
        ds.clear();
    }
}
