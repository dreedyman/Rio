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
package org.rioproject.resolver;

import org.junit.*;
import org.rioproject.resolver.aether.AetherResolver;
import org.rioproject.resolver.maven2.Repository;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assume.assumeTrue;

/**
 * Test maven resolver
 */
public class ITResolverTest {
    private File saveOriginalSettings;
    private static boolean online = true;

    @BeforeClass
    public static void check()  {
        online = ConnectionCheck.connected();
    }

    @Before
    public void saveOriginalSettings() throws IOException {
        saveOriginalSettings = Utils.saveM2Settings();
    }

    @After
    public void restoreOriginalSettings() throws IOException {
        if(saveOriginalSettings!=null) {
            FileUtils.copy(saveOriginalSettings, Utils.getM2Settings());
        } else {
            FileUtils.remove(Utils.getM2Settings());
        }
    }

    @Test
    public void testJskLibResolution() throws ResolverException {
        assumeTrue(online);
        File testRepo;
        Utils.writeLocalM2RepoSettings();
        Resolver r = new AetherResolver();
        testRepo = Repository.getLocalRepository();
        if(testRepo.exists())
            FileUtils.remove(testRepo);
        String[] classPath = r.getClassPathFor("net.jini:jsk-lib:2.1");
        Assert.assertTrue("classPath for net.jini:jsk-lib:2.1 expected to be > 1, actual="+classPath.length,
                          classPath.length>0);
        File jskPlatformJar = new File(testRepo, "net/jini/jsk-lib/2.1/jsk-lib-2.1.jar");
        Assert.assertTrue(jskPlatformJar.exists());
    }

    @Test
    public void testGroovyResolution() throws ResolverException {
        assumeTrue(online);
        File testRepo;
        Utils.writeLocalM2RepoSettings();
        testRepo = Repository.getLocalRepository();
        if(testRepo.exists())
            FileUtils.remove(testRepo);
        Resolver r = new AetherResolver();
        URL loc = r.getLocation("org.codehaus.groovy:groovy-all:1.6.2", null);
        Assert.assertNotNull(loc);
        File groovyJar = new File(testRepo, "org/codehaus/groovy/groovy-all/1.6.2/groovy-all-1.6.2.jar");
        Assert.assertTrue(groovyJar.exists());
    }

    @Test
    public void testWithSettings() throws ResolverException {
        assumeTrue(online);
        File testRepo;
        Utils.writeLocalM2RepoSettings();
        testRepo = Repository.getLocalRepository();
        if(testRepo.exists())
            FileUtils.remove(testRepo);
        Resolver r = new AetherResolver();
        List<String> cp = getClassPathFor("com.sun.jini:outrigger-dl:jar:2.1", r);
        Assert.assertTrue(cp.size()==2);
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
