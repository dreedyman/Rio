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
import org.junit.Test;
import org.junit.runner.RunWith;
import org.rioproject.associations.Association;
import org.rioproject.associations.AssociationDescriptor;
import org.rioproject.associations.AssociationListener;
import org.rioproject.associations.AssociationManagement;
import org.rioproject.impl.associations.DefaultAssociationManagement;
import org.rioproject.test.RioTestConfig;
import org.rioproject.test.RioTestRunner;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author Dennis Reedy
 */
@RunWith(RioTestRunner.class)
@RioTestConfig (
        groups = "AssociationVersionTest",
        numCybernodes = 1,
        numMonitors = 1,
        numLookups = 1,
        opstring = "src/test/resources/opstring/associationVersioning.groovy"
)
public class AssociationVersionTest {

    @Test
    public void testVersionedAssociations() throws ExecutionException, InterruptedException, IOException {
        AssociationManagement aMgr = new DefaultAssociationManagement();
        AssociationDescriptor descriptor = AssociationDescriptor.create("Dummy",
                                                                        Dummy.class,
                                                                        "AssociationVersionTest");
        descriptor.setMatchOnName(false);
        descriptor.setVersion("2.1");
        Association<Dummy> association = aMgr.addAssociationDescriptor(descriptor);
        Dummy dummy = association.getServiceFuture().get();
        Assert.assertNotNull(dummy);
        Assert.assertEquals("Expected 1 got " + association.getServiceCount(),
                            1,
                            association.getServiceCount());
        Assert.assertEquals("Expected 'His Brother Darrel', got " + dummy.getName(),
                            "His Brother Darrel",
                            dummy.getName());
    }

    @Test
    public void testVersionedAssociationRange() throws ExecutionException, InterruptedException, IOException {
        AssociationManagement aMgr = new DefaultAssociationManagement();
        AssociationDescriptor descriptor = AssociationDescriptor.create("Dummy",
                                                                        Dummy.class,
                                                                        "AssociationVersionTest");
        descriptor.setMatchOnName(false);
        descriptor.setVersion("2.8");
        Association<Dummy> association = aMgr.addAssociationDescriptor(descriptor);
        Dummy dummy = association.getServiceFuture().get();
        Assert.assertNotNull(dummy);
        Assert.assertEquals("Expected 1 got "+association.getServiceCount(),
                            1,
                            association.getServiceCount());
        Assert.assertEquals("Expected 'His Other Brother Darrel', got " + dummy.getName(),
                            "His Other Brother Darrel",
                            dummy.getName());
    }

    @Test
    public void testVersionedAssociationNoMatches() throws ExecutionException, InterruptedException {
        AssociationManagement aMgr = new DefaultAssociationManagement();
        AssociationDescriptor descriptor = AssociationDescriptor.create("Dummy",
                                                                        Dummy.class,
                                                                        "AssociationVersionTest");
        descriptor.setMatchOnName(false);
        descriptor.setVersion("1.0");
        Association<Dummy> association = aMgr.addAssociationDescriptor(descriptor);
        Dummy dummy = null;
        try {
            dummy = association.getServiceFuture().get(5, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            System.out.println("Received expected TimeoutException");
        }
        Assert.assertNull(dummy);
        Assert.assertEquals("Expected 0 got "+association.getServiceCount(),
                            0,
                            association.getServiceCount());
    }

    @Test
    public void testVersionedAssociationMatchesAll() throws ExecutionException, InterruptedException {
        AssociationManagement aMgr = new DefaultAssociationManagement();
        L listener = new L();
        aMgr.register(listener);
        AssociationDescriptor descriptor = AssociationDescriptor.create("Dummy",
                                                                        Dummy.class,
                                                                        "AssociationVersionTest");
        descriptor.setMatchOnName(false);
        Association<Dummy> association = aMgr.addAssociationDescriptor(descriptor);
        int waited = 0;
        while(listener.association.get()==null && waited < 100) {
            Thread.sleep(100);
            waited++;
        }
        Assert.assertNotNull(listener.association.get());
        waited = 0;
        while(listener.association.get().getServiceCount()< 4 && waited < 100) {
            Thread.sleep(100);
            waited++;
        }
        Dummy dummy = association.getServiceFuture().get();
        Assert.assertNotNull(dummy);
        Assert.assertEquals("Expected 4 got "+association.getServiceCount(),
                            4,
                            association.getServiceCount());
    }

    class L implements AssociationListener {
        AtomicReference<Association> association = new AtomicReference<>();

        @Override public void discovered(Association association, Object service) {
            this.association.set(association);
        }

        @Override public void changed(Association association, Object service) {

        }

        @Override public void broken(Association association, Object service) {

        }
    }
}
