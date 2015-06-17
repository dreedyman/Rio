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

import org.junit.Assert;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.rioproject.resolver.*;
import org.rioproject.resolver.maven2.Repository;
import org.rioproject.tools.webster.Webster;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;

/**
 * @author Dennis Reedy
 */
public class ResolverConfigTest {
    Webster webster;
    String repoPath = "something/something/darkside-provider/2.0";
    String artifactFileName = "darkside-provider-2.0-deploy.config";

    @Before
    public void setup() throws IOException {
        File repo = new File(System.getProperty("user.dir")+"/target/repo");
        repo.mkdirs();

        File resolvedArtifactDir = new File(Repository.getLocalRepository(), repoPath);
        if(resolvedArtifactDir.exists()) {
            FileUtils.remove(resolvedArtifactDir);
            System.out.println("Removed "+resolvedArtifactDir.getPath());
        }

        File artifactDir = new File(repo, repoPath);
        if(artifactDir.mkdirs()) {
            System.out.println("Created "+artifactDir.getPath());
        }
        File artifact = new File(artifactDir, artifactFileName);

        File source = new File(String.format("%s/src/test/resources/config/%s",
                                             System.getProperty("user.dir"),
                                             artifactFileName));
        FileUtils.copy(source, artifact);
        Assert.assertTrue(artifact.exists());

        webster = new Webster(0, repo.getAbsolutePath());
        System.setProperty("resolver.config.test.port", Integer.toString(webster.getPort()));
    }

    @After
    public void teardown() {
        webster.terminate();
    }

    @Test
    public void resolveConfig() throws ResolverException, URISyntaxException {
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
        URL url = resolver.getLocation("something.something:darkside-provider:config:deploy:2.0", "config");
        Assert.assertNotNull(url);
        File resolved = new File(String.format("%s/%s/%s",
                                               Repository.getLocalRepository(), repoPath, artifactFileName));
        org.junit.Assert.assertTrue(resolved.exists());
        System.out.println("===> "+new File(url.toURI()).getPath());
    }
}
