/*
 * Copyright 2010 to the original author or authors.
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
package org.rioproject.resolver;

import org.junit.Assert;
import org.junit.Test;
import org.rioproject.config.maven2.Repository;
import org.rioproject.resources.util.FileUtils;

import java.io.*;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Test maven resolver
 */
public class ITResolverTest {
    @Test
    public void testJskPlatformPom() throws ResolverException {
        File testRepo;
        File saveOrigSettings = null;
        Throwable thrown = null;
        try {
            saveOrigSettings = Utils.saveM2Settings();
            Utils.writeLocalM2RepoSettings();
            Resolver r = ResolverHelper.getInstance();
            File pom = new File("src/test/resources/jsk-platform-2.1.pom");
            Assert.assertTrue(pom.exists());
            testRepo = Repository.getLocalRepository();
            if(testRepo.exists())
                FileUtils.remove(testRepo);
            File jskPlatformDir = new File(testRepo, "net/jini/jsk-platform/2.1");
            Assert.assertFalse(jskPlatformDir.exists());
            String[] classPath = r.getClassPathFor("net.jini:jsk-platform:2.1", pom, true);
            Assert.assertTrue(classPath.length>0);
            File jskPlatformJar = new File(testRepo, "net/jini/jsk-platform/2.1/jsk-platform-2.1.jar");
            Assert.assertTrue(jskPlatformJar.exists());
            StringBuilder sb = new StringBuilder();
            for(String s : classPath) {
                if(sb.length()>0)
                    sb.append(",");
                sb.append(s);
            }
            Assert.assertEquals(jskPlatformJar.getAbsolutePath(), sb.toString());
        } catch (IOException e) {
            e.printStackTrace();
            thrown = e;
        } finally {
            try {
                Assert.assertNotNull(saveOrigSettings);
                FileUtils.copy(saveOrigSettings, Utils.getM2Settings());
            } catch (IOException e) {
                e.printStackTrace();
            }
            Assert.assertNull(thrown);
        }
    }

    //@Test
    public void testDroolsResolution() throws ResolverException {
        File testRepo;
        File saveOrigSettings = null;
        Throwable thrown = null;
        try {
            saveOrigSettings = Utils.saveM2Settings();
            Utils.writeLocalM2RepoSettings();
            Resolver r = ResolverHelper.getInstance();
            File pom = new File("src/test/resources/drools-grid-core-5.1.0.SNAPSHOT.pom");
            Assert.assertTrue(pom.exists());
            testRepo = Repository.getLocalRepository();
            if(testRepo.exists())
                FileUtils.remove(testRepo);
            //File droolsGridCoreDir = new File(testRepo, "org/drools/drools-grid-core/5.1.0.SNAPSHOT");            
            String[] classPath = r.getClassPathFor("org.drools:drools-grid-core:5.1.0.SNAPSHOT", pom, true);
            //File droolsVSMJar = new File(testRepo, "org/drools/drools-grid-core/5.1.0.SNAPSHOT/drools-grid-core-5.1.0.SNAPSHOT.jar");
            //Assert.assertTrue(droolsVSMJar.exists());
            System.out.println("===> "+Utils.formatClassPath(classPath));
        } catch (IOException e) {
            e.printStackTrace();
            thrown = e;
        } finally {
            try {
                Assert.assertNotNull(saveOrigSettings);
                FileUtils.copy(saveOrigSettings, Utils.getM2Settings());
            } catch (IOException e) {
                e.printStackTrace();
            }
            Assert.assertNull(thrown);
        }
    }

    @Test
    public void testGroovyResolution() throws ResolverException {
        File testRepo;
        File saveOrigSettings = null;
        Throwable thrown = null;
        try {
            saveOrigSettings = Utils.saveM2Settings();
            Utils.writeLocalM2RepoSettings();
            testRepo = Repository.getLocalRepository();
            if(testRepo.exists())
                FileUtils.remove(testRepo);
            Resolver r = ResolverHelper.getInstance();

            URL loc = r.getLocation("org.codehaus.groovy:groovy-all:1.6.2",
                                    null,
                                    null);
            Assert.assertNotNull(loc);
            Assert.assertTrue("Expected groovy jar to be a http:// URL",
                              loc.toExternalForm().startsWith("http"));

            List<String> cp = getClassPathFor("org.codehaus.groovy:groovy-all:1.6.2",
                                              null,
                                              r,
                                              true);
            Assert.assertTrue("Expected groovy artifact to have " +
                              "6 classpath elements, had "+cp.size()+": "+Utils.flatten(cp),
                              cp.size()==6);
            Assert.assertTrue("Expected groovy jar to be groovy-all-1.6.2.jar",
                              cp.get(0).endsWith("groovy-all-1.6.2.jar"));
            Assert.assertTrue("Expected junit jar to be junit-3.8.2.jar",
                              cp.get(1).endsWith("junit-3.8.2.jar"));
            Assert.assertTrue("Expected ant jar to be ant-1.7.1.jar",
                              cp.get(2).endsWith("ant-1.7.1.jar"));
            Assert.assertTrue("Expected ant launcher jar to be ant-launcher-1.7.1.jar",
                              cp.get(3).endsWith("ant-launcher-1.7.1.jar"));
            Assert.assertTrue("Expected jline jar to be jline-0.9.94.jar",
                              cp.get(4).endsWith("jline-0.9.94.jar"));
            Assert.assertTrue("Expected last junit jar to be junit-3.8.1.jar",
                              cp.get(5).endsWith("junit-3.8.1.jar"));
        } catch (IOException e) {
            e.printStackTrace();
            thrown = e;
        } finally {
            try {
                Assert.assertNotNull(saveOrigSettings);
                FileUtils.copy(saveOrigSettings, Utils.getM2Settings());
            } catch (IOException e) {
                e.printStackTrace();
            }
            Assert.assertNull(thrown);
        }
    }

    @Test
    public void testHttpResolution() throws ResolverException {
        File testRepo;
        File saveOrigSettings = null;
        Throwable thrown = null;
        try {
            saveOrigSettings = Utils.saveM2Settings();
            Utils.writeLocalM2RepoSettings();
            testRepo = Repository.getLocalRepository();
            if(testRepo.exists())
                FileUtils.remove(testRepo);
            Resolver r = ResolverHelper.getInstance();

            List<String> cp = getClassPathFor("org.springframework:spring:2.5.5", null, r, false);
            Assert.assertTrue("Expected spring artifact to have 2 classpath elements", cp.size()==2);
            Assert.assertTrue("Expected spring jar to be a http:// URL",
                              cp.get(0).startsWith("http"));
            Assert.assertTrue("Expected spring jar to be spring-2.5.5.jar",
                              cp.get(0).endsWith("spring-2.5.5.jar"));

            cp = getClassPathFor("com.sun.jini:outrigger:dl:2.1", null, r, false);
            Assert.assertTrue("Expected outrigger dl artifact to have " +
                              "1 classpath element, had "+cp.size()+": "+Utils.flatten(cp),
                              cp.size()==1);
            Assert.assertTrue("Expected outrigger jar to be a http:// URL",
                              cp.get(0).startsWith("http"));
            Assert.assertTrue("Expected outrigger jar to be outrigger-2.1-dl.jar",
                              cp.get(0).endsWith("outrigger-2.1-dl.jar"));
        } catch (IOException e) {
            e.printStackTrace();
            thrown = e;
        } finally {
            try {
                Assert.assertNotNull(saveOrigSettings);
                FileUtils.copy(saveOrigSettings, Utils.getM2Settings());
            } catch (IOException e) {
                e.printStackTrace();
            }
            Assert.assertNull(thrown);
        }
    }

    @Test
    public void testWithSettings() throws ResolverException {
         File testRepo;
        File saveOrigSettings = null;
        try {
            saveOrigSettings = Utils.saveM2Settings();
            Utils.writeLocalM2RepoSettings();
            testRepo = Repository.getLocalRepository();
            if(testRepo.exists())
                FileUtils.remove(testRepo);
            Resolver r = ResolverHelper.getInstance();
            List<String> cp = getClassPathFor("com.sun.jini:outrigger:dl:2.1", null, r, true);
            Assert.assertTrue(cp.size()==1);
           
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                Assert.assertNotNull(saveOrigSettings);
                FileUtils.copy(saveOrigSettings, Utils.getM2Settings());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private List<String> getClassPathFor(String artifact, File pom, Resolver r, boolean install) {
        String[] cp = r.getClassPathFor(artifact, pom, install);
        List<String> jars = new ArrayList<String>();
        String codebase = System.getProperty("user.home")+
                          File.separator+".m2"+
                          File.separator+"repository"+
                          File.separator;
        for(String jar : cp) {
            if(install)
                jars.add(jar.substring(codebase.length()));
            else
                jars.add(jar);
        }
        System.out.println("classpath ("+artifact+"): "+jars);
        return jars;
    }


}
