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
import org.rioproject.config.Constants;
import org.rioproject.config.maven2.Repository;
import org.rioproject.resources.util.FileUtils;

import java.io.File;
import java.io.IOException;

/**
 * Test resolving artifacts with and without inclusion of Rio
 */
public class ITResolverUsingRioDepsTest {
    @Test
    public void testExcludeRioDeps() throws ResolverException, IOException {
        String[] classPath = getClassPath();
        Assert.assertTrue(classPath.length>0);
        Assert.assertTrue(classPath.length==1);
        System.out.println("EXCLUDE:\n"+Utils.formatClassPath(classPath)+"\n");
    }

    @Test
    public void testExcludingRioDepsBySettingPropertyToTrue() throws ResolverException, IOException {
        System.setProperty(Constants.RESOLVER_PRUNE_PLATFORM, "true");
        String[] classPath = getClassPath();
        Assert.assertTrue(classPath.length>0);
        Assert.assertTrue(classPath.length==1);
        System.out.println("EXCLUDE:\n"+Utils.formatClassPath(classPath)+"\n");
    }

    @Test
    public void testIncludingRioDeps() throws ResolverException, IOException {
        System.setProperty(Constants.RESOLVER_PRUNE_PLATFORM, "false");
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
        Resolver r = ResolverHelper.getInstance();
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

        return r.getClassPathFor("org.rioproject.examples.events:events-api:2.0", (File) null, false);
    }
}
