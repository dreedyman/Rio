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
package org.rioproject.test.associations;

import org.junit.Assert;
import net.jini.lookup.ServiceDiscoveryEvent;
import net.jini.space.JavaSpace05;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.rioproject.associations.*;
import org.rioproject.impl.associations.*;
import org.rioproject.test.RioTestConfig;
import org.rioproject.test.RioTestRunner;
import org.rioproject.test.SetTestManager;
import org.rioproject.test.TestManager;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Test Association Future handling.
 */
@RunWith (RioTestRunner.class)
@RioTestConfig(
        groups = "AssociationFutureTest",
        //locators = "jini://192.168.86.71"
        numCybernodes = 1,
        numMonitors = 1,
        numLookups = 1
)
public class AssociationFutureTest {
    @SetTestManager
    static TestManager testManager;
    
    @Test
    public void testSimpleFuture() {
        AssociationManagement aMgr = new DefaultAssociationManagement();
        Association<Dummy> association =
                aMgr.addAssociationDescriptor(AssociationDescriptor.create("Dummy", Dummy.class));
        Future<Dummy> future = association.getServiceFuture();
        association.addServiceItem(AssociationUtils.makeServiceItem(0));
        Dummy dummy = null;
        Throwable thrown = null;
        try {
            dummy = future.get();
            Assert.assertEquals(0, dummy.getIndex());
        } catch (Exception e) {
            e.printStackTrace();
            thrown = e;
        }
        Assert.assertNull(thrown);
        Assert.assertNotNull(dummy);
        aMgr.terminate();
    }

    @Test
    public void testContinuousGets() {
        org.rioproject.associations.AssociationManagement aMgr = new DefaultAssociationManagement();
        Association<Dummy> association =
            aMgr.addAssociationDescriptor(AssociationDescriptor.create("Dummy", Dummy.class));
        Future<Dummy> future = association.getServiceFuture();
        association.addServiceItem(AssociationUtils.makeServiceItem(0));
        for(int i=0; i<1000; i++) {
            Dummy dummy = null;
            Throwable thrown = null;
            try {
                dummy = future.get();
                Assert.assertEquals(0, dummy.getIndex());
            } catch (Exception e) {
                thrown = e;
            }
            Assert.assertNull(thrown);
            Assert.assertNotNull(dummy);
        }
        aMgr.terminate();
    }

    @Test
    public void testMultipleFutures() {
        org.rioproject.associations.AssociationManagement aMgr = new DefaultAssociationManagement();
        List<Future<Dummy>> futures = new ArrayList<>();
        for(int i=0; i<100; i++) {
            Association<Dummy> association =
                aMgr.addAssociationDescriptor(AssociationDescriptor.create("Dummy-"+i, Dummy.class));
            Future<Dummy> future = association.getServiceFuture();
            futures.add(future);
            association.addServiceItem(AssociationUtils.makeServiceItem(i));
        }
        int i=0;
        for(Future<Dummy> future: futures) {
            Dummy dummy = null;
            Throwable thrown = null;
            try {
                dummy = future.get();
                Assert.assertEquals(i++, dummy.getIndex());
            } catch (Exception e) {
                e.printStackTrace();
                thrown = e;
            }
            Assert.assertNull(thrown);
            Assert.assertNotNull(dummy);
        }
        aMgr.terminate();
    }

    @Test
    public void testWait() {
        final org.rioproject.associations.AssociationManagement aMgr = new DefaultAssociationManagement();
        final Association<Dummy> association =
            aMgr.addAssociationDescriptor(AssociationDescriptor.create("Dummy", Dummy.class));
        Future<Dummy> future = association.getServiceFuture();
        Dummy dummy = null;
        Throwable thrown = null;
        new Thread(() -> {
            try {
                Thread.sleep(4000);
            } catch (InterruptedException e) { }
            association.addServiceItem(AssociationUtils.makeServiceItem(0));
        }).start();
        try {
            long t0 = System.currentTimeMillis();
            dummy = future.get(5, TimeUnit.SECONDS);
            long t1 = System.currentTimeMillis();
            Assert.assertTrue("Timeout should be 3 < value < 5 seconds",
                              ((double)((t1-t0)/1000)>3 && ((double)(t1-t0)/1000)<5));
            Assert.assertEquals(0, dummy.getIndex());
        } catch (Exception e) {
            thrown = e;
        }
        Assert.assertNull(thrown);
        Assert.assertNotNull(dummy);
        aMgr.terminate();
    }

    @Test
    public void testForceTerminateWithException() {
        final org.rioproject.associations.AssociationManagement aMgr = new DefaultAssociationManagement();
        Association<Dummy> association =
            aMgr.addAssociationDescriptor(AssociationDescriptor.create("Dummy", Dummy.class));
        Future<Dummy> future = association.getServiceFuture();
        Dummy dummy = null;
        Throwable thrown = null;
        new Thread(() -> {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            aMgr.terminate();
        }).start();
        try {
            dummy = future.get();
        } catch (Exception e) {
            thrown = e;
        }
        Assert.assertNotNull(thrown);
        Assert.assertNull(dummy);
        aMgr.terminate();
    }

    @Test
    public void testFutureWithInjectionPreServiceDiscovery() {
        TargetDummy target = new TargetDummy();
        AssociationInjector<Dummy> ai = new AssociationInjector<>(target);
        AssociationDescriptor ad = AssociationDescriptor.create("Dummy", "dummy", Dummy.class);
        Association<Dummy> a = new DefaultAssociation<>(ad);
        a.addServiceItem(AssociationUtils.makeServiceItem(0));
        ai.discovered(a, new DummyImpl(0));
        Dummy dummy = null;
        Throwable thrown = null;
        try {
            Assert.assertNotNull(target.future);
            dummy = target.future.get();
            Assert.assertEquals(0, dummy.getIndex());
        } catch (Exception e) {
            e.printStackTrace();
            thrown = e;
        }
        Assert.assertNull(thrown);
        Assert.assertNotNull(dummy);
    }

    @Test
    public void testFutureWithLazyInjectionPostServiceDiscovery() {
        TargetDummy target = new TargetDummy();
        AssociationDescriptor ad = AssociationDescriptor.create("Dummy", "dummy", Dummy.class);
        ad.setLazyInject(false);
        final DefaultAssociationManagement aMgr = new DefaultAssociationManagement();
        aMgr.setBackend(target);
        final Association<Dummy> a = aMgr.addAssociationDescriptor(ad);
        Dummy dummy = null;
        Throwable thrown = null;
        try {
            Assert.assertNotNull(target.future);
            new Thread(new Runnable() {
                public void run() {
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    aMgr.getAssociationHandler(a).serviceAdded(
                            new ServiceDiscoveryEvent(this,
                                                      null,
                                                      AssociationUtils.makeServiceItem(0)));
                }
            }).start();
            dummy = target.future.get();

            Assert.assertEquals(0, dummy.getIndex());
        } catch (Exception e) {
            e.printStackTrace();
            thrown = e;
        }
        Assert.assertNull(thrown);
        Assert.assertNotNull(dummy);
    }

    @Test
    public void testWithDeploy() throws Exception {
        File opstring = new File("src/int-test/resources/opstring/space.groovy");
        Assert.assertNotNull(opstring);

        testManager.deploy(opstring);
        AssociationDescriptor ad = AssociationDescriptor.create("Spaced Out",
                                                                JavaSpace05.class,
                                                                "AssociationFutureTest");
        //ad.setOperationalStringName("AssociationFutureTest");
        ad.setMatchOnName(false);
        DefaultAssociationManagement mgr = new DefaultAssociationManagement();
        Association<JavaSpace05> a = mgr.addAssociationDescriptor(ad);
        Future<JavaSpace05> future = a.getServiceFuture();
        JavaSpace05 space = future.get(10, TimeUnit.SECONDS);
        Assert.assertNotNull(space);
    }

    public static class TargetDummy {
        Future<Dummy> future;

        public void setDummy(Association<Dummy> dummies) {
            future = dummies.getServiceFuture();
        }
    }
}
