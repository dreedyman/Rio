/*
 * Copyright to the original author or authors.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *         http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.rioproject.tools.webster;

import org.junit.Test;
import org.rioproject.net.PortRangeServerSocketFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.*;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Unit tests for Webster
 */
public class WebsterUTest {
    @Test
    public void createWebsterWithConfig() throws Exception {
        Webster w = new Webster(new File(System.getProperty("user.dir")+"/src/test/resources/webster.groovy"));
        assertNotNull(w);
        w.terminate();
    }

    @Test
    public void createWebsterWithPortAndRoots() throws Exception {
        Webster w = new Webster(9000, System.getProperty("user.dir"));
        assertNotNull(w);
        assertEquals(9000, w.getPort());
        assertEquals(System.getProperty("user.dir"), w.getRoots());
        w.terminate();
    }

    @Test
    public void createWebsterWithPortAndRootsAndInetAddress() throws Exception {
        Webster w = new Webster(9000, System.getProperty("user.dir"), InetAddress.getLocalHost().getHostAddress());
        assertNotNull(w);
        assertEquals(9000, w.getPort());
        assertEquals(System.getProperty("user.dir"), w.getRoots());
        w.terminate();
    }

    @Test
    public void createWebsterWithOptionsAsArray() throws Exception {
        int port = 40000;
        String root = System.getProperty("user.dir");
        String[] options = new String[]{"-port", Integer.toString(port),
                "-roots", root,
                "-bindAddress", InetAddress.getLocalHost().getHostAddress()};
        Webster w = new Webster(options, null);
        assertNotNull(w);
        assertEquals(port, w.getPort());
        assertEquals(System.getProperty("user.dir"), w.getRoots());
    }

    @Test
    public void createWebsterWithPortRangeUsingOptions() throws Exception {
        String root = System.getProperty("user.dir");
        String[] options = new String[]{"-portRange", "10000-10005",
                "-roots", root,
                "-bindAddress", InetAddress.getLocalHost().getHostAddress()};
        Webster w = new Webster(options, null);
        assertNotNull(w);
        int port = w.getPort();
        assertTrue("Port " + port + " should be >= 10000", port >= 10000);
        assertTrue("Port " + port + " should be <= 10005", port <= 10005);
    }

    @Test
    public void createWebsterWithPortRangeServerSocketFactory() throws Exception {
        Throwable t = null;
        String root = System.getProperty("user.dir");
        Webster w = new Webster(new PortRangeServerSocketFactory(10000, 10005), root, null);
        assertNotNull(w);
        int port = w.getPort();
        assertTrue("Port " + port + " should be >= 10000", port >= 10000);
        assertTrue("Port " + port + " should be <= 10005", port <= 10005);
    }

    @Test
    public void verifyGetURI() throws Exception {
        Webster webster = new Webster(9010, System.getProperty("user.dir"));
        URI uri = webster.getURI();
        assertNotNull(uri);
    }

    @Test
    public void verifyGetFromWebster() throws Exception {
        Webster w = new Webster(new File(System.getProperty("user.dir")+"/src/test/resources/webster.groovy"));
        assertNotNull(w);
        List<String> items = get(w.getPort());
        assertNotNull(items);
        File cwd = new File(System.getProperty("user.dir"));
        assertTrue(items.size() == cwd.list().length);
    }

    private List<String> get(int port) throws IOException {
        URL url = new URL("http://" + InetAddress.getLocalHost().getHostName()+ ":" + port);
        System.out.println("===> "+url.toExternalForm());
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        connection.setRequestMethod("GET");

        List<String> items = new ArrayList<>();

        connection.connect();
        BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));

        String line;
        while ((line = in.readLine()) != null) {
            items.add(line);
        }
        return items;
    }


}
