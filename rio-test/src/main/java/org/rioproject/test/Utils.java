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
package org.rioproject.test;

import junit.framework.Assert;
import org.rioproject.config.Constants;
import org.rioproject.watch.WatchDataSource;

import java.io.File;
import java.util.Collection;
import java.util.Iterator;

/**
 * The class provides utility methods for Rio tests
 */
@SuppressWarnings("PMD.AvoidThrowingRawExceptionTypes")
public class Utils {
    public static void setEnvironment() {
        if(System.getProperty("JAVA_HOME")==null) {
            String javaHome = System.getProperty("java.home");
            /* Make an attempt to find tools.jar */
            File jHome = new File(javaHome);
            File lib = new File(jHome, "lib");
            boolean foundTools = findFile(lib, "tools.jar");
            if(!foundTools) {
                File f = jHome.getParentFile();
                lib = new File(f, "lib");
                if(findFile(lib, "tools.jar")) {
                    jHome = f;
                }
            }
            javaHome = jHome.getAbsolutePath();
            System.setProperty("JAVA_HOME", javaHome);
        }

        if(System.getProperty("RIO_HOME")==null) {
            String rioHome = System.getenv().get("RIO_HOME");
            if(rioHome!=null)
                System.setProperty("RIO_HOME", rioHome);
            else
                System.err.println("\n============================\n"+
                                   "RIO_HOME IS NULL"+
                                   "\n============================");
            return;
        }

        checkSecurityPolicy();

        if(System.getProperty(Constants.GROUPS_PROPERTY_NAME)==null)
            System.setProperty(Constants.GROUPS_PROPERTY_NAME, "rio");
    }

    public static void checkSecurityPolicy() {
        if(System.getProperty("java.security.policy")==null) {
            StringBuilder b = new StringBuilder();
            b.append(getRioHome());
            b.append("policy");
            b.append(File.separator);
            b.append("policy.all");
            System.setProperty("java.security.policy", b.toString());
        }
    }

    public static String getRioHome() {
        String rioHome = System.getProperty("RIO_HOME");
        if(rioHome==null) {
            rioHome = System.getenv().get("RIO_HOME");
            if(rioHome!=null)
                System.setProperty("RIO_HOME", rioHome);
        }
        if(rioHome==null)
            throw new RuntimeException("RIO_HOME not set. You " +
                                       "must set it as a system property " +
                                       "or it must be set in your " +
                                       "environment");
        if(!rioHome.endsWith(File.separator))
            rioHome = rioHome +File.separator;
        return rioHome;
    }

    public static String getTestClassDirectory() {
        String rioHome = System.getProperty("RIO_HOME",
                                            System.getProperty("user.dir"));
        StringBuilder b = new StringBuilder();
        b.append(rioHome);
        b.append(File.separator);
        b.append("build");
        b.append(File.separator);
        b.append("test-classes");
        b.append(File.separator);
        return b.toString();
    }

    /**
     * Calls the <code>close()</code> method on a <code>WatchDataSource</code>
     * proxy, ignoring all exceptions.
     *
     * @param dataSource the object on which to call <code>close()</code>
     */
    public static void close(WatchDataSource dataSource) {
        try {
            dataSource.close();
        } catch (Throwable t) {
        }
    }

    /**
     * Causes the currently executing thread to sleep (temporarily cease
     * execution) for the specified number of milliseconds. The thread
     * does not lose ownership of any monitors. If another thread has
     * interrupted the current thread, the method simply exists.
     *
     * @param millis the length of time to sleep in milliseconds
     */
    public static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
        }
    }

    /**
     * Asserts that two collections have the same contents. Having the same
     * contents means being of the same size and containing the same objects
     * in the same order.
     *
     * @param c1 Collection
     * @param c2 Collection
     */
    public static void assertSameContents(Collection c1, Collection c2) {
        Assert.assertEquals("Collection size", c1.size(), c2.size());
        Iterator i1 = c1.iterator();
        Iterator i2 = c2.iterator();
        while(i1.hasNext() && i2.hasNext()) {
            Assert.assertSame(i1.next(), i2.next());
        }
    }

    /**
     * Asserts that two collections have equal contents. Having equal
     * contents means being of the same size and containing the same objects
     * (as in object.equals(otherObject) in the same order.
     *
     * @param c1 Collection
     * @param c2 Collection
     */
    public static void assertEqualContents(Collection c1, Collection c2) {
        Assert.assertEquals("Collection size", c1.size(), c2.size());
        Iterator i1 = c1.iterator();
        Iterator i2 = c2.iterator();
        while(i1.hasNext() && i2.hasNext()) {
            Assert.assertEquals(i1.next(), i2.next());
        }
    }

    private static boolean findFile(File dir, String fName) {
        boolean found = false;
        if(dir.exists()&& dir.isDirectory()) {
            for(String f : dir.list()) {
                if(f.equals(fName)) {
                    found = true;
                    break;
                }
            }
        }
        return found;
    }
}
