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
package org.rioproject.monitor.service;

import com.sun.jini.reggie.TransientRegistrarImpl;
import com.sun.jini.start.LifeCycle;
import junit.framework.Assert;
import net.jini.config.Configuration;
import net.jini.config.EmptyConfiguration;
import net.jini.discovery.DiscoveryEvent;
import net.jini.discovery.DiscoveryListener;
import net.jini.discovery.DiscoveryManagement;
import net.jini.discovery.LookupDiscoveryManager;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.rioproject.impl.config.DynamicConfiguration;
import org.rioproject.opstring.ServiceBeanConfig;
import org.rioproject.opstring.ServiceElement;
import org.rioproject.resolver.RemoteRepository;
import org.rioproject.resolver.ResolverHelper;

import java.io.File;
import java.io.IOException;
import java.rmi.NoSuchObjectException;
import java.rmi.RemoteException;
import java.security.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Test the DeploymentVerifier
 *
 * @author Dennis Reedy
 */
public class DeploymentVerifierTest {
    Reggie registrar;
    DL listener;
    DiscoveryManagement discoveryManagement;
    static {
        Policy.setPolicy(new Policy() {
            public PermissionCollection getPermissions(CodeSource codesource) {
                Permissions perms = new Permissions();
                perms.add(new AllPermission());
                return (perms);
            }

            public void refresh() {
            }

        });
        System.setSecurityManager(new SecurityManager());
        System.setProperty("StaticCybernode", "true");
    }
    private DeploymentVerifier deploymentVerifier;

    @BeforeClass
    public static void setResolverJar() {
        String classPath = System.getProperty("java.class.path");
        String[] parts = classPath.split(File.pathSeparator);
        String resolverJar = null;
        for(String part : parts) {
            if(part.contains("resolver-aether")) {
                resolverJar = part;
                break;
            }
        }
        Assert.assertNotNull(resolverJar);
        System.setProperty(ResolverHelper.RESOLVER_JAR, resolverJar);
    }

    @Before
    public void setup() throws Exception {
        listener = new DL();
        discoveryManagement = new LookupDiscoveryManager(new String[]{"foo"}, null, listener);
        deploymentVerifier = new DeploymentVerifier(EmptyConfiguration.INSTANCE, discoveryManagement);
    }

    @After
    public void teardown() throws RemoteException {
        if(registrar!=null) {
            registrar.destroy();
            registrar = null;
        }
        discoveryManagement.terminate();
    }

    @Test
    public void testMergeRepositories() throws Exception {
        RemoteRepository[] r1 = new RemoteRepository[]{createRR("http://1.1.1.1", "foo", "santa"),
                                                       createRR("http://1.1.1.2", "bar", "rudolf")};
        RemoteRepository[] r2 = new RemoteRepository[]{createRR("http://1.1.1.1", "foo", "santa"),
                                                       createRR("http://2.1.1.1", "baz", "easter-bunny")};

        RemoteRepository[] repositories = deploymentVerifier.mergeRepositories(r1, r2);
        Assert.assertTrue(repositories.length==3);
    }

    private RemoteRepository createRR(String url, String id, String name) {
        RemoteRepository rr = new RemoteRepository();
        rr.setId(id);
        rr.setName(name);
        rr.setUrl(url);
        return rr;
    }

    @Test
    public void testEnsureGroups() throws Exception {
        startLookup("foo");
        Assert.assertFalse(hasGroup(registrar, "gack"));
        ServiceElement service = createSE("gack");
        Assert.assertTrue(waitForDiscovery(listener));
        Assert.assertTrue(listener.discovered.get());
        DL newDiscoListener = new DL();
        new LookupDiscoveryManager(new String[]{"gack"}, null, newDiscoListener);
        deploymentVerifier.ensureGroups(service);
        Assert.assertTrue(waitForDiscovery(newDiscoListener));
        Assert.assertTrue(hasGroup(registrar, "gack"));
    }

    @Test
    public void testEnsureMultiGroups() throws Exception {
        startLookup("foo");
        Assert.assertTrue(waitForDiscovery(listener));
        Assert.assertFalse(hasGroup(registrar, "gack"));
        Assert.assertFalse(hasGroup(registrar, "blutarsky"));
        ServiceElement service = createSE("gack", "blutarsky");
        Assert.assertTrue(listener.discovered.get());
        DL newDiscoListener = new DL();
        new LookupDiscoveryManager(new String[]{"gack"}, null, newDiscoListener);
        deploymentVerifier.ensureGroups(service);
        Assert.assertTrue(waitForDiscovery(newDiscoListener));
        Assert.assertTrue(hasGroup(registrar, "gack"));
        Assert.assertTrue(hasGroup(registrar, "blutarsky"));
    }

    @Test(expected = IOException.class)
    public void testAllGroupsFail() throws Exception {
        ServiceElement service = new ServiceElement();
        ServiceBeanConfig serviceBeanConfig = new ServiceBeanConfig();
        serviceBeanConfig.setGroups("all");
        service.setServiceBeanConfig(serviceBeanConfig);
        deploymentVerifier.ensureGroups(service);
    }

    private ServiceElement createSE(String... groups) {
        ServiceElement service = new ServiceElement();
        ServiceBeanConfig serviceBeanConfig = new ServiceBeanConfig();
        serviceBeanConfig.setGroups(groups);
        service.setServiceBeanConfig(serviceBeanConfig);
        return service;
    }

    private boolean waitForDiscovery(DL dl) throws InterruptedException {
        int waited = 0;
        while(!dl.discovered.get() && waited<10) {
            Thread.sleep(500);
            waited++;
        }
        return dl.discovered.get();
    }

    private boolean hasGroup(Reggie registrar, String group) throws NoSuchObjectException {
        String[] groups = registrar.getMemberGroups();
        boolean found = false;
        for(String g : groups) {
            if(group.equals(g)) {
                found = true;
                break;
            }
        }
        return found;
    }

    private void startLookup(String... groups) throws Exception {
        DynamicConfiguration configuration = new DynamicConfiguration();
        configuration.setEntry("com.sun.jini.reggie", "initialMemberGroups", String[].class, groups);
        registrar = new Reggie(configuration, null);
        listener = new DL();
        discoveryManagement = new LookupDiscoveryManager(new String[]{"foo"}, null, listener);
        deploymentVerifier = new DeploymentVerifier(EmptyConfiguration.INSTANCE, discoveryManagement);
    }

    static class Reggie extends TransientRegistrarImpl {

        Reggie(Configuration config, LifeCycle lifeCycle) throws Exception {
            super(config, lifeCycle);
        }
    }

    class DL implements DiscoveryListener {
        AtomicBoolean discovered = new AtomicBoolean(false);

        public void discovered(DiscoveryEvent discoveryEvent) {
            discovered.set(true);
        }

        public void discarded(DiscoveryEvent discoveryEvent) {
        }
    }
}
