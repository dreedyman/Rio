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
import org.junit.Before;
import org.junit.Test;
import org.rioproject.resolver.aether.AetherResolver;
import org.rioproject.resolver.maven2.Repository;

import java.io.*;

/**
 * Test resolving artifacts with and without inclusion of Rio
 */
public class ITResolverUsingRioDepsTest {
    
    private static String RESOLVER_PRUNE_PLATFORM="org.rioproject.resolver.prune.platform";

    @Before
    public void setup() throws IOException {
        File testRepo = Repository.getLocalRepository();
        File parentDir = new File(testRepo, "org/rioproject/examples/events/2.0");
        if(parentDir.exists())
            FileUtils.remove(parentDir);
        File artifactDir = new File(testRepo, "org/rioproject/examples/events/events-api/2.0");
        if(artifactDir.exists())
            FileUtils.remove(artifactDir);
        prepareParentPom();
    }

    @Test
    public void testExcludeRioDeps() throws ResolverException, IOException {
        System.clearProperty(RESOLVER_PRUNE_PLATFORM);
        String[] classPath = getClassPath();
        Assert.assertTrue(classPath.length>0);
        System.out.println("EXCLUDE:\n"+Utils.formatClassPath(classPath)+"\n");
        Assert.assertTrue("Expected classPath length of 1, got "+classPath.length, classPath.length==1);
    }

    @Test
    public void testExcludingRioDepsBySettingPropertyToTrue() throws ResolverException, IOException {
        System.setProperty(RESOLVER_PRUNE_PLATFORM, "true");
        String[] classPath = getClassPath();
        Assert.assertTrue(classPath.length>0);
        System.out.println("EXCLUDE:\n"+Utils.formatClassPath(classPath)+"\n");
        Assert.assertTrue(classPath.length==1);
    }

    @Test
    public void testIncludingRioDeps() throws ResolverException, IOException {
        System.setProperty(RESOLVER_PRUNE_PLATFORM, "false");
        String[] classPath = getClassPath();
        Assert.assertTrue(classPath.length>0);
        boolean foundRio = false;
        for(String jar : classPath) {
            File f = new File(jar);
            if(f.getName().startsWith("rio")) {
                foundRio = true;
                break;
            }
        }
        Assert.assertTrue(foundRio);
        System.out.println("INCLUDE:\n"+Utils.formatClassPath(classPath)+"\n");
    }

    private String[] getClassPath() throws ResolverException, IOException {
        File testRepo;
        Resolver r = new AetherResolver();
        testRepo = Repository.getLocalRepository();

        File srcParentPom = new File("src/test/resources/events-2.0.pom");
        Assert.assertTrue(srcParentPom.exists());

        File parentDir = new File(testRepo, "org/rioproject/examples/events/2.0");
        if(!parentDir.exists()) {
            if(parentDir.mkdirs())
                System.out.println("Created "+parentDir.getAbsolutePath());
        }
        File parentPom = new File(parentDir, "events-2.0.pom");
        if(!parentPom.exists()) {
            FileUtils.copy(srcParentPom, parentPom);
        }
        Assert.assertTrue("The events-2.0.pom should be in "+parentPom.getAbsolutePath(), parentPom.exists());

        File srcPom = new File("src/test/resources/events-api-2.0.pom");
        Assert.assertTrue(srcPom.exists());
        File srcJar = new File("src/test/resources/events-api-2.0.jar");
        Assert.assertTrue(srcJar.exists());

        File artifactDir = new File(testRepo, "org/rioproject/examples/events/events-api/2.0");
        if(!artifactDir.exists()) {
            if(artifactDir.mkdirs())
                System.out.println("Created "+artifactDir.getAbsolutePath());
        }
        File pomArtifact = new File(artifactDir, "events-api-2.0.pom");
        if(!pomArtifact.exists()) {
            FileUtils.copy(srcPom, pomArtifact);
        }
        Assert.assertTrue("The events-api-2.0.pom should be in "+pomArtifact.getAbsolutePath(), pomArtifact.exists());

        File jarArtifact = new File(artifactDir, "events-api-2.0.jar");
        if(!jarArtifact.exists()) {
            FileUtils.copy(srcJar, jarArtifact);
        }
        Assert.assertTrue("The events-api-2.0.jar should be in "+jarArtifact.getAbsolutePath(), jarArtifact.exists());

        return r.getClassPathFor("org.rioproject.examples.events:events-api:2.0");
    }

    private void prepareParentPom() throws IOException {
        File srcParentPomTemplate = new File("src/test/resources/events-2.0.pom.template");
        Assert.assertTrue(srcParentPomTemplate.exists());
        StringBuilder sb = new StringBuilder();
        BufferedReader in = new BufferedReader(new FileReader(srcParentPomTemplate));
        String str;
        String rioVersion = System.getProperty("rio.version");
        System.out.println("=======================");
        System.out.println("Rio Version: "+rioVersion);
        System.out.println("=======================");
        while ((str = in.readLine()) != null) {
            str = Utils.replace(str, "${current-rio-version}", rioVersion);
            sb.append(str).append("\n");
        }
        in.close();
        File srcParentPom = new File("src/test/resources/events-2.0.pom");
        FileUtils.writeFile(sb.toString(), srcParentPom);

    }
}
