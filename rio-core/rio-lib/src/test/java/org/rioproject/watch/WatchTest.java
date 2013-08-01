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
import org.rioproject.config.DynamicConfiguration;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.rmi.RMISecurityManager;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * The class tests the <code>Watch</code> part of a class derived from
 * <code>Watch</code>. The class tests the public methods and fields inherited
 * by the class under test from <code>Watch</code>, and also the public
 * constructors of the class under test (we managed to factor this into this
 * shared class and reuse in concrete tests).
 * <p/>
 * The class under test is instantiated using reflection. The
 * <code>construct*()</code> methods are responsible for that.
 */
public class WatchTest {

    /**
     * The class under test.
     */
    private Class clazz;

    /**
     * The expected default return value of the <code>getView()</code> method.
     */
    private String expectedView;

   /**
     * Constructs a <code>WatchTest</code>.
     *
     * @param clazz the <code>Watch</code>-derived class to be tested
     * @param expectedView the expected default return value of the
     * <code>getView()</code> method
     */
    public WatchTest(Class clazz, String expectedView) {
        this.clazz = clazz;
        this.expectedView = expectedView;
        if (System.getSecurityManager() == null)
            System.setSecurityManager(new RMISecurityManager());
    }

    /**
     * Tests the <code>&lt;init&gt;(String id)</code> constructor of the class
     * under test. The method calls the constructor with different arguments and
     * checks that the constructor works correctly, that is, the arguments are
     * processed as specified and generated instances behave correctly. The test
     * verifies that illegal arguments are handled correctly.
     *
     * @throws Exception if the test fails
     */
    @Test
    public void testConstructor1() throws Exception {

        // id
        final String ids[] = new String[]{"1-2", "watch", "aBcde"};
        for (String id : ids) {
            Watch watch = construct1(id);
            Assert.assertEquals(id, watch.getId());
            testInstance(watch);
            Utils.close(watch.getWatchDataSource());
        }

        // Illegal id
        try {
            String id = null;
            construct1(id);
            Assert.fail("IllegalArgumentException expected but not thrown");
        } catch (IllegalArgumentException e) {
        }
        // Illegal id
        try {
            String id = "";
            construct1(id);
            Assert.fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException e) {
        }
    }

    /**
     * Tests the <code>&lt;init&gt;(String id, Configuration config)</code>
     * constructor of the class under test. The method calls the constructor
     * with different arguments and checks that the constructor works correctly,
     * that is, the arguments are processed as specified and generated instances
     * behave correctly. The test verifies that illegal arguments are handled
     * correctly.
     *
     * @throws Exception if the test fails
     */
    @Test
    public void testConstructor2() throws Exception {
        // id
        final String ids[] = new String[]{"1-2", "watch", "aBcde"};
        for (String id : ids) {
            Watch watch = construct2(id, EmptyConfiguration.INSTANCE);
            Assert.assertEquals(id, watch.getId());
            testInstance(watch);
            Utils.close(watch.getWatchDataSource());
        }

        // Illegal id
        try {
            String id = null;
            construct2(id, EmptyConfiguration.INSTANCE);
            Assert.fail("IllegalArgumentException expected but not thrown");
        } catch (IllegalArgumentException e) {
        }
        // Illegal id
        try {
            String id = "";
            construct1(id);
            Assert.fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException e) {
        }

        // config
        final int collectionSizes[] = new int[]{1, 1000, 5000};
        for (int collectionSize : collectionSizes) {
            DynamicConfiguration config = new DynamicConfiguration();
            config.setEntry("org.rioproject.watch", "collectionSize",
                            collectionSize);
            Watch watch = construct2("watch", config);
            checkInstance(watch, collectionSize, false);
            Utils.close(watch.getWatchDataSource());
        }

        // Illegal config
        try {
            Configuration config = null;
            construct2("watch", config);
            Assert.fail("IllegalArgumentException expected but not thrown");
        } catch (IllegalArgumentException e) {
        }
    }

    /**
     * Tests the <code>&lt;init&gt;(WatchDataSource watchDataSource, String
     * id)</code> constructor of the class under test. The method calls the
     * constructor with different arguments and checks that the constructor
     * works correctly, that is, the arguments are processed as specified and
     * generated instances behave correctly. The test verifies that illegal
     * arguments are handled correctly.
     *
     * @throws Exception if the test fails
     */
    @Test
    public void testConstructor4() throws Exception {
        // id
        final String ids[] = new String[]{"1-2", "watch", "aBcde"};
        for (String id : ids) {
            //WatchDataSource wds = new WatchDataSourceImpl(
            //        id, null, EmptyConfiguration.INSTANCE).export();
            WatchDataSource wds = new WatchDataSourceImpl();
            wds.setConfiguration(EmptyConfiguration.INSTANCE);
            wds.initialize();
            //wds.setID(id);
            //wds.initialize();

            Watch watch = construct4(wds, id);
            Assert.assertEquals(id, watch.getId());
            testInstance(watch);
            Utils.close(watch.getWatchDataSource());
        }

        // Illegal id
        try {
            String id = null;
            WatchDataSourceImpl wdsi = new WatchDataSourceImpl();
            wdsi.setID("");
            wdsi.setConfiguration(EmptyConfiguration.INSTANCE);
            WatchDataSource wds = wdsi.export();

            construct4(wds, id);
            Assert.fail("IllegalArgumentException expected but not thrown");
        } catch (IllegalArgumentException e) {
        }
        try {
            String id = "";
            construct1(id);
            Assert.fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException e) {
        }

        // watchDataSource
        final int collectionSizes[] = new int[]{1, 1000, 5000};
        for (int collectionSize : collectionSizes) {
            DynamicConfiguration config = new DynamicConfiguration();
            config.setEntry("org.rioproject.watch", "collectionSize",
                            collectionSize);
            //WatchDataSource wds = new WatchDataSourceImpl("", null,
            //                                              config).export();
            WatchDataSource wds = new WatchDataSourceImpl();
            wds.setConfiguration(config);

            Watch watch = construct4(wds, "watch");
            Assert.assertSame(wds, watch.getWatchDataSource());
            checkInstance(watch, collectionSize, false);
            Utils.close(watch.getWatchDataSource());
        }

        // Illegal watchDataSource
        try {
            WatchDataSource wds = null;
            construct4(wds, "watch");
            Assert.fail("IllegalArgumentException expected but not thrown");
        } catch (IllegalArgumentException e) {
        }
    }

    /**
     * Tests the <code>addWatchRecord</code> method of the class under test. The
     * test calls the method under test with different arguments and checks that
     * it works correctly. The test verifies that illegal arguments are handled
     * correctly.
     *
     * @throws Exception if the test fails
     */
    @Test
    public void testAddWatchRecord() throws Exception {

        Watch watch1 = construct1("watch1");
        checkInstance(watch1, 1000, false);
        Utils.close(watch1.getWatchDataSource());

        Watch watch2 = construct1("watch2");
        try {
            checkInstance(watch2, 1000, true);
            Assert.fail("IllegalArgumentException expected but not thrown");
        } catch (IllegalArgumentException e) {
        }
        Utils.close(watch2.getWatchDataSource());
    }

    /**
     * Tests the <code>equals()</code> method of the class under test.
     *
     * @throws Exception if the test fails
     */
    @Test
    public void testEquals() throws Exception {
        Watch watch1 = construct1("abc");
        Watch watch2 = construct1("abc");
        Watch watch3 = construct1("1-2");
        Watch watch4 = construct1("1.2");
        Assert.assertTrue(watch1.equals(watch2));
        Assert.assertFalse(watch1.equals(watch3));
        Assert.assertFalse(watch1.equals(watch4));
        Assert.assertFalse(watch1.equals(new Object()));
        Assert.assertFalse(watch1.equals("abc"));
        Utils.close(watch1.getWatchDataSource());
        Utils.close(watch2.getWatchDataSource());
        Utils.close(watch3.getWatchDataSource());
        Utils.close(watch4.getWatchDataSource());
    }

    /**
     * Tests the <code>getId()</code> method of the class under test.
     *
     * @throws Exception if the test fails
     */
    @Test
    public void testGetId() throws Exception {
        String ids[] = new String[]{"1-2", "abc"};
        for (String id : ids) {
            Watch watch = construct1(id);
            Assert.assertEquals(id, watch.getId());
            Utils.close(watch.getWatchDataSource());
        }
    }

    /**
     * Tests the <code>getView()</code> and <code>setView(String)</code> methods
     * of the class under test.
     *
     * @throws Exception if the test fails
     */
    @Test
    public void testGetSetView() throws Exception {
        Watch watch = construct1("watch");
        Assert.assertEquals(expectedView, watch.getView());

        watch.setView("MyView");
        Assert.assertEquals("MyView", watch.getView());

        try {
            watch.setView(null);
            Assert.fail("IllegalArgumentException expected but not thrown");
        } catch (IllegalArgumentException e) {
        }

        Utils.close(watch.getWatchDataSource());
    }

    /**
     * Tests the <code>getWatchDataSource()</code> and <code>setWatchDataSource(WatchDataSource)</code>
     * methods of the class under test.
     *
     * @throws Exception if the test fails
     */
    @Test
    public void testGetSetWatchDataSource() throws Exception {
        WatchDataSourceImpl wdsi = new WatchDataSourceImpl();
        wdsi.setID("wds");
        wdsi.setConfiguration(EmptyConfiguration.INSTANCE);
        WatchDataSource wds = wdsi.export();
        Watch watch = construct4(wds, "watch");
        Assert.assertSame(wds, watch.getWatchDataSource());

        WatchDataSourceImpl wdsi2 = new WatchDataSourceImpl();
        wdsi2.setID("otherWds");
        wdsi2.setConfiguration(EmptyConfiguration.INSTANCE);
        WatchDataSource otherWds = wdsi2.export();
                
        watch.setWatchDataSource(otherWds);
        Assert.assertSame(otherWds, watch.getWatchDataSource());

        watch.setWatchDataSource(null);
        Assert.assertSame(otherWds, watch.getWatchDataSource());

        Utils.close(watch.getWatchDataSource());
    }

    /**
     * Tests the <code>hashCode()</code> method of the class under test.
     *
     * @throws Exception if the test fails
     */
    @Test
    public void testHashCode() throws Exception {
        String ids[] = new String[]{"1-2", "abc"};
        for (String id : ids) {
            Watch watch = construct1(id);
            watch.hashCode();
            Utils.close(watch.getWatchDataSource());
        }
    }

    /**
     * Tests the <code>toString()</code> method of the class under test.
     *
     * @throws Exception if the test fails
     */
    @Test
    public void testToString() throws Exception {
        String ids[] = new String[]{"1-2", "watch", "abc"};
        for (String id : ids) {
            Watch watch = construct1(id);
            Assert.assertEquals(id, watch.toString());
            Utils.close(watch.getWatchDataSource());
        }
    }


    /**
     * Tests if a Watch instance behaves correctly. This method is used by many
     * other test methods. The method does some typical actions on the watch
     * object to check if it works well. Besides all, the method tests that the
     * watch's data source internal collection is of size <code>1000</code>.
     *
     * @param watch the object to be tested
     *
     * @throws RemoteException if the test fails
     */
    private void testInstance(Watch watch) throws RemoteException {
        checkInstance(watch, 1000, false);
    }


    /*
     * Tests if a Watch instance behaves correctly. This method is used by many
     * other test methods. The method does some typical actions on the watch
     * object to check if it works well. Besides all, the method tests that the
     * watch's data source internal collection is of a specified size.
     *
     * @param watch the object to be tested
     * @param collectionSize the expected collection size
     * @param tryNulls if <code>true</code>, the method will try to add
     * <code>null</code>s among non-<code>null</code> calculable records
     *
     * @throws RemoteException if the test fails
     */
    private synchronized void checkInstance(Watch watch,
                                           int collectionSize,
                                           boolean tryNulls)
        throws RemoteException {

        watch.getWatchDataSource().setMaxSize(collectionSize);
        final int checkpoints[] = new int[]{0, 1, collectionSize / 2,
                                            collectionSize,
                                            collectionSize + 1,
                                            collectionSize * 2};
        List<Calculable> added = new ArrayList<Calculable>();

        // Go through the checkpoints
        for (int checkpoint : checkpoints) {

            // Generate samples to reach the checkpoint
            while (added.size() < checkpoint) {
                Calculable c = new Calculable(watch.getId(), Math.random(),
                                              added.size());
                if (tryNulls && added.size() % 10 == 3) {
                    c = null;
                }
                added.add(c);
                watch.addWatchRecord(c);
            }
            //DataSourceMonitor mon = new DataSourceMonitor(watch);
            //mon.waitFor(added.size());

            // Verify that the data has been added correctly
            Calculable[] calcs = watch.getWatchDataSource().getCalculable();
            int offset = Math.max(added.size() - collectionSize, 0);
            List expected = added.subList(offset, added.size());
            //System.out.println("---> offset="+offset+", " +
            //                   "expected="+expected.size()+", " +
            //                   "actual="+calcs.length+", " +
            //                   "collectionSize="+collectionSize);
            Assert.assertEquals(expected, Arrays.asList(calcs));
        }

    }


    /**
     * Creates a <code>Watch</code> instance using the <code>&lt;init&gt;(String
     * id)</code> constructor of the class under test.
     *
     * @param id the watch identifier passed to the constructor
     * @return the constructed object
     *
     * @throws Exception if the test fails
     */
    public Watch construct1(String id) throws Exception {

        Constructor ctor = clazz.getConstructor(String.class);
        try {
            return (Watch) ctor.newInstance(id);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause != null && cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            } else {
                throw e;
            }
        }
    }

    /**
     * Creates a <code>Watch</code> instance using the <code>&lt;init&gt;(String
     * id, Configuration config)</code> constructor of the class under test.
     *
     * @param id the watch identifier passed to the constructor
     * @param config the configuration object passed to the constructor
     * @return the constructed object
     *
     * @throws NoSuchMethodException for failure to construct
     * @throws InstantiationException for failure to construct
     * @throws IllegalAccessException for failure to construct
     * @throws InvocationTargetException for failure to construct
     */
    public Watch construct2(String id, Configuration config)
        throws NoSuchMethodException, InstantiationException,
               IllegalAccessException, InvocationTargetException {

        Constructor ctor = clazz.getConstructor(String.class, Configuration.class);
        try {
            return (Watch) ctor.newInstance(id, config);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause != null && cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            } else {
                throw e;
            }
        }
    }


    /**
     * Creates a <code>Watch</code> instance using the <code>&lt;init&gt;(WatchDataSource
     * watchDataSource, String id)</code> constructor of the class under test.
     *
     * @param watchDataSource the data source object passed to the constructor
     * @param id the watch identifier passed to the constructor
     * @return the constructed object
     *
     * @throws NoSuchMethodException for failure to construct
     * @throws InstantiationException for failure to construct
     * @throws IllegalAccessException for failure to construct
     * @throws InvocationTargetException for failure to construct
     */
    public Watch construct4(WatchDataSource watchDataSource, String id)
        throws NoSuchMethodException, InstantiationException,
               IllegalAccessException, InvocationTargetException {

        Constructor ctor = clazz.getConstructor(WatchDataSource.class, String.class);
        try {
            return (Watch) ctor.newInstance(watchDataSource, id);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause != null && cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            } else {
                throw e;
            }
        }
    }
}
