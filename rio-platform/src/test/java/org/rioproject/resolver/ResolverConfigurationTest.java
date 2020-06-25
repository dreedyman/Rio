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

import org.junit.Assert;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.rioproject.RioVersion;

import java.io.File;
import java.util.List;

public class ResolverConfigurationTest {

    @Before
    public void setup() {
        File fakeRioHome = new File(System.getProperty("user.dir") + "/src/test/resources/fake-rio-home");
        System.setProperty("rio.home", fakeRioHome.getAbsolutePath());
    }

    @After
    public void clear() {
        System.clearProperty("rio.home");
        System.clearProperty(ResolverConfiguration.RESOLVER_CONFIG);
    }

    @Test
    public void testGetRemoteRepositories() throws Exception {
        ResolverConfiguration resolverConfiguration = new ResolverConfiguration();
        List<RemoteRepository> repositories = resolverConfiguration.getRemoteRepositories();
        Assert.assertNotNull(repositories);
        Assert.assertTrue(repositories.size() == 2);
        Assert.assertTrue(repositories.get(0).getId().equals("mine"));
        Assert.assertTrue(repositories.get(0).getUrl().equals("http://10.0.1.9:9010"));
    }

    @Test
    public void testGetJar() throws Exception {
        ResolverConfiguration resolverConfiguration = new ResolverConfiguration();
        String jarName = resolverConfiguration.getResolverJar();
        Assert.assertNotNull(jarName);
        String expected = String.format("%s/lib/resolver/resolver-aether-%s.jar",
                                        System.getProperty("rio.home"),
                                        RioVersion.VERSION);
        Assert.assertTrue("Expected: "+expected+", got: "+jarName, jarName.equals(expected));
    }

    @Test
    public void testLoadFromSystemProperty() {
        System.setProperty(ResolverConfiguration.RESOLVER_CONFIG,
                           new File(System.getProperty("user.dir") +
                                    "/src/test/resources/fake-rio-home/config/resolverConfig2.groovy").getAbsolutePath());
        ResolverConfiguration resolverConfiguration = new ResolverConfiguration();
        List<RemoteRepository> repositories = resolverConfiguration.getRemoteRepositories();
        Assert.assertNotNull(repositories);
        Assert.assertTrue(repositories.size() == 1);
        Assert.assertTrue(repositories.get(0).getId().equals("project"));

    }

    @Test
    public void testMissingConfig() {
        System.clearProperty("rio.home");
        System.clearProperty(ResolverConfiguration.RESOLVER_CONFIG);
        ResolverConfiguration resolverConfiguration = new ResolverConfiguration();
        String jarName = resolverConfiguration.getResolverJar();
        Assert.assertNull(jarName);
    }
}
