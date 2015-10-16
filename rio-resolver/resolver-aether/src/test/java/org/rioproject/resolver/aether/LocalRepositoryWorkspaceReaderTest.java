/*
 * Copyright to the original author or authors.
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
package org.rioproject.resolver.aether;

import org.apache.maven.settings.building.SettingsBuildingException;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.rioproject.resolver.*;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

public class LocalRepositoryWorkspaceReaderTest {
    File flatRepo;
    String artifactFileName = "darkside-deathstar-2.0-deploy.config";

    @Before
    public void setup() throws IOException {
        flatRepo = new File(System.getProperty("user.dir") + "/target/flat");
        flatRepo.mkdirs();
        File artifact = new File(flatRepo, artifactFileName);
        File source = new File(String.format("%s/src/test/resources/config/%s",
                                             System.getProperty("user.dir"),
                                             artifactFileName));
        FileUtils.copy(source, artifact);
        Assert.assertTrue(artifact.exists());
    }

    @Test
    public void testMultiDirectoryResolver() throws SettingsBuildingException, MalformedURLException, ArtifactResolutionException, ResolverException {
        File config = new File(System.getProperty("user.dir") +
                               "/src/test/resources/config/testResolverConfig.groovy");
        File target = new File(System.getProperty("user.dir"), "target");
        File resolverJar = null;
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
        Resolver resolver = ResolverHelper.getResolver();
        //resolver.setFlatDirectories(Arrays.asList(flatRepo));
        /*MultiLocalDirectoryReader reader = new MultiLocalDirectoryReader();
        reader.addDirectories(flatRepo);
        AetherService service = AetherService.getInstance(reader);
        URL url = service.getLocation("something.something:darkside-deathstar:config:deploy:2.0", "config");*/
        URL url = resolver.getLocation("something.something:darkside-deathstar:config:deploy:2.0", "config");
        Assert.assertNotNull(url);
        System.out.println("===> " + url.toExternalForm());
    }

}