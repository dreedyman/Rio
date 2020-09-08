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
package org.rioproject.tools.jetty;

import com.sun.jini.start.ServiceDescriptor;
import net.jini.config.EmptyConfiguration;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.rioproject.config.DynamicConfiguration;
import org.rioproject.security.SecureEnv;
import org.rioproject.util.RioHome;
import org.rioproject.util.ServiceDescriptorUtil;

import java.io.*;
import java.lang.reflect.Field;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

/**
 * Test Jetty & Webster concurrency
 *
 * Created by Dennis Reedy on 6/19/17.
 */
public class JettyTest {

    @BeforeClass
    public static void setup() throws Exception {
        String keyStorePath = String.format("%s/config/security/rio-cert.ks", RioHome.get());
        SecureEnv.setup(keyStorePath);
    }

    @Test
    public void testSecure() throws Exception {
        String projectDir = System.getProperty("projectDir");
        File fileDir = new File(projectDir+"/build/files-0");
        if(fileDir.mkdirs())
            System.out.println("Created "+fileDir.getPath());
        Jetty jetty = new Jetty().setRoots(fileDir.getPath()).setPutDir(fileDir.getPath());
        jetty.startSecure();
        String jettyURL = jetty.getURI().toURL().toExternalForm();
        System.out.println("===> jettyURL: "+jetty.getURI().toURL().getProtocol());
        System.out.println("===> jettyURL: "+jettyURL);
        assertEquals("https", jetty.getURI().toURL().getProtocol());
    }

    @Test
    public void testUpload() throws Exception {
        String projectDir = System.getProperty("projectDir");
        String resources = new File(projectDir+"/src/test/resources").getPath();
        File fileDir = new File(projectDir+"/build/files-1");
        if(fileDir.mkdirs())
            System.out.println("Created "+fileDir.getPath());

        Jetty jetty = new Jetty().setRoots(fileDir.getPath()).setPutDir(fileDir.getPath());
        jetty.startSecure();
        String jettyURL = jetty.getURI().toString();
        /*0, new String[]{fileDir.getPath()}, fileDir.getPath());*/
        //String jettyURL = String.format("https://%s:%s", jetty.getAddress(), jetty.getPort());
        System.out.println("===> jettyURL: "+jettyURL);
        //jetty.join();
        File file1 = new File(resources+"/file1.txt");
        assertTrue(file1.exists());
        //GenericUtil.upload(file1, new URL(jettyURL+"/"+file1.getName()));
        upload(new URL(jettyURL+"/"+file1.getName()), file1);

        File uploaded = new File(fileDir, "file1.txt");
        assertTrue(uploaded.exists());
    }

    @Test
    public void testStartWithConfig() throws Exception {
        DynamicConfiguration config = new DynamicConfiguration();
        config.setEntry(Jetty.COMPONENT,"port", int.class,9020);
        config.setEntry(Jetty.COMPONENT,"roots", String[].class,
                new String[]{
                        System.getProperty("user.dir") + "/build/classes",
                        System.getProperty("user.dir") + "/build/libs"
        });
        Jetty jetty = new Jetty(config);
        assertEquals(9020, jetty.getPort());
    }

    @Test
    public void testStartWithGroovyConfig() throws Exception {
        String projectDir = System.getProperty("projectDir");
        String resources = new File(projectDir+"/src/test/resources").getPath();
        File config = new File(resources +"/jetty.groovy");
        Jetty jetty = new Jetty(config);
        assertEquals(9029, jetty.getPort());
    }

    @Test
    public void testStartWithDescriptor() {
        Assert.assertNotNull(System.getProperty("java.security.policy"));
        String webster = System.getProperty("WEBSTER_JAR");
        Assert.assertNotNull(webster);
        ServiceDescriptor desc = ServiceDescriptorUtil.getJetty("0",
                new String[]{System.getProperty("user.dir")},
                System.getProperty("user.dir"));

        Assert.assertNotNull(desc);
        Jetty j = getJetty(desc);
        Assert.assertNotNull(j);
    }

    @Test
    public void testStart() throws Exception {
        String projectDir = System.getProperty("projectDir");
        String resources = new File(projectDir+"/src/test/resources").getPath();
        File fileDir = new File(projectDir+"/build/files-2");
        if(fileDir.mkdirs())
            System.out.println("Created "+fileDir.getPath());
        Jetty jetty = new Jetty().setRoots(resources).setPutDir(fileDir.getPath());
        jetty.startSecure();

        int count = 5;
        CyclicBarrier gate = new CyclicBarrier(count+1);
        CountDownLatch filesWritten = new CountDownLatch(count);
        List<Fetcher> fetchers = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            String file = String.format("file%s.txt", (i % 2 == 0 ? 1 : 2));
            String fileURL = String.format("%s%s", jetty.getURI().toASCIIString(), file);
            Fetcher fetcher = new Fetcher(gate, new URL(fileURL), fileDir, filesWritten, i);
            fetchers.add(fetcher);
            new Thread(fetcher).start();
        }
        gate.await();

        filesWritten.await();
        int totalFailed = 0;
        for (Fetcher f : fetchers) {
            if (f.failed > 0) {
                totalFailed++;
                System.out.println("Fetcher ["+f.index+"] failed "+f.failed+" time(s)");
            }
        }
        System.out.println("Total failed Fetchers: "+totalFailed);
    }

    private void upload(URL url, File file) throws IOException {
        HttpURLConnection httpCon = (HttpURLConnection) url.openConnection();
        httpCon.setDoOutput(true);
        httpCon.setRequestMethod("PUT");
        try (DataOutputStream out = new DataOutputStream(httpCon.getOutputStream())) {
            out.write(Files.readAllBytes(file.toPath()));
            out.flush();
            httpCon.getInputStream();
        }
    }

    class Fetcher implements Runnable {
        CyclicBarrier gate;
        URL url;
        File fileDir;
        int index;
        CountDownLatch filesWritten;
        int failed;

        Fetcher(CyclicBarrier gate, URL url, File fileDir, CountDownLatch filesWritten, int index) {
            this.gate = gate;
            this.url = url;
            this.fileDir = fileDir;
            this.filesWritten = filesWritten;
            this.index = index;
        }

        @Override
        public void run() {
            File f = new File(fileDir, url.getFile()+"."+index);
            try {
                gate.await();
                boolean downloaded = false;
                while(!downloaded) {
                    try {
                        Files.write(f.toPath(), fetch(url).getBytes());
                        System.out.println("Wrote " + f.getName());
                        downloaded = true;
                    } catch (IOException e) {
                        failed++;
                        System.out.println("Failed writing "+f.getName()+", "+e.getClass().getName()+": "+e.getMessage());
                        if(e instanceof SocketException) {
                            Thread.sleep(50);
                        } else {
                            e.printStackTrace();
                            break;
                        }
                    }
                }
                filesWritten.countDown();
            } catch (InterruptedException | BrokenBarrierException  e) {
                e.printStackTrace();
            }
        }
    }

    private String fetch(URL url) throws IOException {
        String text;
        URLConnection conn = url.openConnection();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(),
                                                                              StandardCharsets.UTF_8))) {
            text = reader.lines().collect(Collectors.joining("\n"));
        }
        return text;
    }

    private Jetty getJetty(ServiceDescriptor desc) {
        Jetty j = null;
        try {
            Object created = desc.create(EmptyConfiguration.INSTANCE);
            Field impl = created.getClass().getDeclaredField("impl");
            j = (Jetty) impl.get(created);
        } catch(Exception e) {
            e.printStackTrace();
        }
        return j;
    }

}
