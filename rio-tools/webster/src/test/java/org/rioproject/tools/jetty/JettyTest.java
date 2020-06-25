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

import org.junit.Test;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.stream.Collectors;

import static org.junit.Assert.assertTrue;

/**
 * Test Jetty & Webster concurrency
 *
 * Created by Dennis Reedy on 6/19/17.
 */
public class JettyTest {

    @Test
    public void testSecure() throws Exception {
        String projectDir = System.getProperty("projectDir");
        String resources = new File(projectDir+"/src/test/resources").getPath();
        File fileDir = new File(projectDir+"/build/files-0");
        if(fileDir.mkdirs())
            System.out.println("Created "+fileDir.getPath());
        Jetty jetty = new Jetty().setRoots(fileDir.getPath()).setPutDir(fileDir.getPath());
        jetty.startSecure();
        String jettyURL = jetty.getURI().toURL().toExternalForm();
        System.out.println("===> jettyURL: "+jetty.getURI().toURL().getProtocol());
        System.out.println("===> jettyURL: "+jettyURL);
        assertTrue(jetty.getURI().toURL().getProtocol().equals("https"));
        //jetty.join();
    }

    @Test
    public void testUpload() throws Exception {
        String projectDir = System.getProperty("projectDir");
        String resources = new File(projectDir+"/src/test/resources").getPath();
        File fileDir = new File(projectDir+"/build/files-1");
        if(fileDir.mkdirs())
            System.out.println("Created "+fileDir.getPath());

        Jetty jetty = new Jetty().setRoots(fileDir.getPath()).setPutDir(fileDir.getPath());
        jetty.start();
        /*0, new String[]{fileDir.getPath()}, fileDir.getPath());*/
        String jettyURL = String.format("http://%s:%s", jetty.getAddress(), jetty.getPort());
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
    public void testStart() throws Exception {
        String projectDir = System.getProperty("projectDir");
        String resources = new File(projectDir+"/src/test/resources").getPath();
        File fileDir = new File(projectDir+"/build/files-2");
        if(fileDir.mkdirs())
            System.out.println("Created "+fileDir.getPath());
        Jetty jetty = new Jetty().setRoots(resources).setPutDir(fileDir.getPath());
        jetty.start();

        int count = 5;
        CyclicBarrier gate = new CyclicBarrier(count+1);
        CountDownLatch filesWritten = new CountDownLatch(count);
        List<Fetcher> fetchers = new ArrayList<>();
        for(int i=0; i<count; i++) {
            String file = String.format("file%s.txt", (i%2==0?1:2));
            String fileURL = String.format("http://%s:%s/%s",
                                           jetty.getAddress(), jetty.getPort(), file);
            Fetcher fetcher = new Fetcher(gate, new URL(fileURL), fileDir, filesWritten, i);
            fetchers.add(fetcher);
            new Thread(fetcher).start();
        }
        gate.await();

        filesWritten.await();
        int totalFailed = 0;
        for(Fetcher f : fetchers) {
            if(f.failed>0) {
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

}
