package org.rioproject.resolver;

import junit.framework.Assert;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

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
        String jarName = resolverConfiguration.getResolverJarName();
        Assert.assertNotNull(jarName);
        Assert.assertTrue(jarName.equals("resolver-aether"));
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
        String jarName = resolverConfiguration.getResolverJarName();
        Assert.assertNull(jarName);
    }
}