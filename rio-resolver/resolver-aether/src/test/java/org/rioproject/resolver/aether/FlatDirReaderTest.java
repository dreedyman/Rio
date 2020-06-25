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
package org.rioproject.resolver.aether;

import org.apache.maven.settings.building.SettingsBuildingException;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.rioproject.resolver.*;
import org.rioproject.resolver.aether.util.DefaultPomGenerator;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

public class FlatDirReaderTest {
    File flatRepo;
    String artifactFileName = "darkside-deathstar-2.0-deploy.config";
    Resolver resolver;
    File resolverJar;

    @Before
    public void setup() throws IOException, ResolverException {
        flatRepo = new File(System.getProperty("user.dir") + "/build/flat");
        flatRepo.mkdirs();
        File artifact = new File(flatRepo, artifactFileName);
        File source = new File(String.format("%s/src/test/resources/config/%s",
                                             System.getProperty("user.dir"),
                                             artifactFileName));
        FileUtils.copy(source, artifact);
        Assert.assertTrue(artifact.exists());

        File config = new File(System.getProperty("user.dir") +
                               "/src/test/resources/config/testResolverConfig.groovy");
        File target = new File(System.getProperty("user.dir"), "build/libs");
        resolverJar = null;
        for(File f : target.listFiles()) {
            if(f.getName().startsWith("resolver-aether")) {
                resolverJar = f;
                break;
            }
        }

        Assert.assertNotNull(resolverJar);
        Assert.assertTrue(config.exists());
        System.setProperty(ResolverConfiguration.RESOLVER_CONFIG, config.getAbsolutePath());
        System.setProperty(ResolverConfiguration.RESOLVER_JAR, resolverJar.getAbsolutePath());
        resolver = ResolverHelper.getResolver();
    }

    @After
    public void clean() {
        File poms =  new File(String.format("%s/.rio/generated/poms",
                                            System.getProperty("user.home").replace('\\', '/')));
        org.eclipse.aether.artifact.Artifact a = new DefaultArtifact("something.something:darkside-deathstar:pom:2.1");
        File pomDir = new File(poms, DefaultPomGenerator.getGenerationPath(a));
        FileUtils.remove(pomDir.getParentFile().getParentFile(), true);
    }

    @Test
    public void testMultiDirectoryResolver() throws SettingsBuildingException, MalformedURLException, ArtifactResolutionException, ResolverException {
        URL url = resolver.getLocation("something.something:darkside-deathstar:config:deploy:2.0", "config");
        Assert.assertNotNull(url);
    }

    @Test
    public void testFlatDirReader() throws ResolverException, IOException {
        File copy = new File(flatRepo, "darkside-deathstar-2.1.jar");
        FileUtils.copy(resolverJar, copy);
        String[] cp = resolver.getClassPathFor("something.something:darkside-deathstar:2.1");
        Assert.assertTrue(cp.length>0);
    }

    @Test
    public void testFlatDirReader2() throws ResolverException, IOException {
        File copy = new File(flatRepo, "darkside-deathstar-2.1.jar");
        File copy2 = new File(flatRepo, "darkside-deathstar-2.2.jar");
        FileUtils.copy(resolverJar, copy);
        FileUtils.copy(resolverJar, copy2);
        String[] cp = resolver.getClassPathFor("something.something:darkside-deathstar:2.1");
        Assert.assertTrue(cp.length>0);
        String[] cp2 = resolver.getClassPathFor("something.something:darkside-deathstar:2.2");
        Assert.assertTrue(cp2.length>0);
    }
}
