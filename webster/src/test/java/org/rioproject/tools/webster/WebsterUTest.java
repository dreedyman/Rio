/*
 * Copyright 2011 the original author or authors
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
package org.rioproject.tools.webster;

import org.junit.Assert;
import org.junit.Test;
import org.rioproject.net.PortRangeServerSocketFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.BindException;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Unit tests for Webster
 */
public class WebsterUTest {
    @Test
    public void createWebsterZeroArg() {
        Throwable t = null;
        Webster w = null;
        try {
            w = new Webster();
        } catch (BindException e) {
            t = e;
        }
        Assert.assertNull(t);
        Assert.assertNotNull(w);
        w.terminate();
    }

    @Test
    public void createWebsterWithPort() {
        Throwable t = null;
        Webster w = null;
        try {
            w = new Webster(9000);
        } catch (BindException e) {
            t = e;
        }
        Assert.assertNull(t);
        Assert.assertNotNull(w);
        Assert.assertEquals(9000, w.getPort());
        w.terminate();
    }

    @Test
    public void createWebsterWithPortAndRoots() {
        Throwable t = null;
        Webster w = null;
        try {
            w = new Webster(9000, System.getProperty("user.dir"));
        } catch (BindException e) {
            t = e;
        }
        Assert.assertNull(t);
        Assert.assertNotNull(w);
        Assert.assertEquals(9000, w.getPort());
        Assert.assertEquals(System.getProperty("user.dir"), w.getRoots());
        w.terminate();
    }

    @Test
    public void createWebsterWithPortAndRootsAndInetAddress() {
        Throwable t = null;
        Webster w = null;
        try {
            w = new Webster(9000, System.getProperty("user.dir"), InetAddress.getLocalHost().getHostAddress());
        } catch (Exception e) {
            t = e;
        }
        Assert.assertNull(t);
        Assert.assertNotNull(w);
        Assert.assertEquals(9000, w.getPort());
        Assert.assertEquals(System.getProperty("user.dir"), w.getRoots());
        w.terminate();
    }

    @Test
    public void createWebsterWithOptionsAsArray() {
        int port = 40000;
        String root = System.getProperty("user.dir");
        Throwable t = null;
        Webster w = null;
        try {
            String[] options = new String[]{"-port", Integer.toString(port),
                                            "-roots", root,
                                            "-bindAddress", InetAddress.getLocalHost().getHostAddress()};
            w = new Webster(options, null);
        } catch (Exception e) {
            t = e;
        }
        Assert.assertNull(t);
        Assert.assertNotNull(w);
        Assert.assertEquals(port, w.getPort());
        Assert.assertEquals(System.getProperty("user.dir"), w.getRoots());
    }

    @Test
    public void createWebsterWithPortRangeUsingOptions() {
        Throwable t = null;
        Webster w = null;
        String root = System.getProperty("user.dir");
        try {
            String[] options = new String[]{"-portRange", "10000-10005",
                                            "-roots", root,
                                            "-bindAddress", InetAddress.getLocalHost().getHostAddress()};
            w = new Webster(options, null);
        } catch (Exception e) {
            t = e;
        }
        Assert.assertNull(t);
        Assert.assertNotNull(w);
        int port = w.getPort();
        Assert.assertTrue("Port " + port + " should be >= 10000", port >= 10000);
        Assert.assertTrue("Port " + port + " should be <= 10005", port <= 10005);
    }

    @Test
    public void createWebsterWithPortRangeServerSocketFactory() {
        Throwable t = null;
        Webster w = null;
        String root = System.getProperty("user.dir");
        try {
            w = new Webster(new PortRangeServerSocketFactory(10000, 10005), root, null);
        } catch (Exception e) {
            t = e;
        }
        Assert.assertNull(t);
        Assert.assertNotNull(w);
        int port = w.getPort();
        Assert.assertTrue("Port " + port + " should be >= 10000", port >= 10000);
        Assert.assertTrue("Port " + port + " should be <= 10005", port <= 10005);
    }

    @Test
    public void verifyGetFromWebster() {
        Throwable t = null;
        Webster w = null;
        try {
            w = new Webster();
        } catch (BindException e) {
            t = e;
        }
        Assert.assertNull(t);
        Assert.assertNotNull(w);
        t = null;
        List<String> items = null;
        try {
            items = get(w.getPort());
        } catch (Exception e) {
            t = e;
            e.printStackTrace();
        }
        Assert.assertNull(t);
        Assert.assertNotNull(items);
        File cwd = new File(System.getProperty("user.dir"));
        Assert.assertTrue(items.size() == cwd.list().length);
    }

    private List<String> get(int port) throws IOException {
        URL url = new URL("http://" + InetAddress.getLocalHost().getHostAddress() + ":" + port);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        connection.setRequestMethod("GET");

        List<String> items = new ArrayList<String>();

        connection.connect();
        BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));

        String line;
        while ((line = in.readLine()) != null) {
            items.add(line);
        }
        return items;
    }


}
