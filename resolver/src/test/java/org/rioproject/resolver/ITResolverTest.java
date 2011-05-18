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
            /*if(testRepo.exists())
                FileUtils.remove(testRepo);*/
            //File jskPlatformDir = new File(testRepo, "net/jini/jsk-platform/2.1");
            //Assert.assertFalse(jskPlatformDir.exists());
            String[] classPath = r.getClassPathFor("net.jini:jsk-platform:2.1");
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

    @Test
    public void testGroovyResolution() throws ResolverException {
        File testRepo;
        File saveOrigSettings = null;
        Throwable thrown = null;
        try {
            saveOrigSettings = Utils.saveM2Settings();
            Utils.writeLocalM2RepoSettings();
            /*testRepo = Repository.getLocalRepository();
            if(testRepo.exists())
                FileUtils.remove(testRepo);*/
            Resolver r = ResolverHelper.getInstance();

            URL loc = r.getLocation("org.codehaus.groovy:groovy-all:1.6.2", null);
            Assert.assertNotNull(loc);
            List<String> cp = getClassPathFor("org.codehaus.groovy:groovy-all:1.6.2", r);
            /*Assert.assertTrue("Expected groovy artifact to have " +
                              "12 classpath elements, had "+cp.size()+": "+Utils.flatten(cp),
                              cp.size()==12);*/
            Assert.assertTrue("Expected element [0]: to be groovy-all-1.6.2.jar, was: "+cp.get(0),
                              cp.get(0).endsWith("groovy-all-1.6.2.jar"));
            Assert.assertTrue("Expected element [1]: to be junit-3.8.2.jar, was: "+cp.get(1),
                              cp.get(1).endsWith("junit-3.8.2.jar"));
            Assert.assertTrue("Expected element [2]: to be ant-1.7.1.jar, was: "+cp.get(2),
                              cp.get(2).endsWith("ant-1.7.1.jar"));
            Assert.assertTrue("Expected element [3]: to be ant-junit-1.7.1.jar, was: "+cp.get(3),
                              cp.get(3).endsWith("ant-junit-1.7.1.jar"));
            Assert.assertTrue("Expected element [4]: to be ant-launcher-1.7.1.jar, was: "+cp.get(4),
                              cp.get(4).endsWith("ant-launcher-1.7.1.jar"));
            Assert.assertTrue("Expected element [5]: to be bsf-2.4.0.jar, was: "+cp.get(5),
                              cp.get(5).endsWith("bsf-2.4.0.jar"));
            Assert.assertTrue("Expected element [6]: to be commons-logging-1.1.jar, was: "+cp.get(6),
                              cp.get(6).endsWith("commons-logging-1.1.jar"));
            Assert.assertTrue("Expected element [7]: to be servlet-api-2.4.jar, was: "+cp.get(7),
                              cp.get(7).endsWith("servlet-api-2.4.jar"));
            Assert.assertTrue("Expected element [8]: to be jsp-api-2.0.jar, was: "+cp.get(8),
                              cp.get(8).endsWith("jsp-api-2.0.jar"));
            Assert.assertTrue("Expected element [9]: to be xstream-1.3.jar, was: "+cp.get(9),
                              cp.get(9).endsWith("xstream-1.3.jar"));
            Assert.assertTrue("Expected element [10]: to be jline-0.9.94.jar, was: "+cp.get(10),
                              cp.get(10).endsWith("jline-0.9.94.jar"));
            Assert.assertTrue("Expected element [11]: to be ivy-2.0.0.jar, was: "+cp.get(11),
                              cp.get(11).endsWith("ivy-2.0.0.jar"));
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
            /*testRepo = Repository.getLocalRepository();
            if(testRepo.exists())
                FileUtils.remove(testRepo);*/
            Resolver r = ResolverHelper.getInstance();
            List<String> cp = getClassPathFor("com.sun.jini:outrigger:dl:2.1", r);
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

    private List<String> getClassPathFor(String artifact, Resolver r) throws ResolverException {
        String[] cp = r.getClassPathFor(artifact);
        List<String> jars = new ArrayList<String>();
        String codebase = System.getProperty("user.home")+
                          File.separator+".m2"+
                          File.separator+"repository"+
                          File.separator;
        for(String jar : cp) {
            jars.add(jar.substring(codebase.length()));
        }
        System.out.println("classpath ("+artifact+"): "+jars);
        return jars;
    }


}
