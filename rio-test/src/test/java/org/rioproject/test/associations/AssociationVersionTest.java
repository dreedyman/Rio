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

import junit.framework.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.rioproject.associations.Association;
import org.rioproject.associations.AssociationDescriptor;
import org.rioproject.associations.AssociationManagement;
import org.rioproject.impl.associations.DefaultAssociationManagement;
import org.rioproject.impl.associations.filter.VersionMatchFilter;
import org.rioproject.test.RioTestRunner;
import org.rioproject.test.SetTestManager;
import org.rioproject.test.TestManager;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

/**
 * @author Dennis Reedy
 */
@RunWith(RioTestRunner.class)
public class AssociationVersionTest {
    @SetTestManager
    static TestManager testManager;

    @Test
    public void testVersionedAssociations() throws ExecutionException, InterruptedException, IOException {
        AssociationManagement aMgr = new DefaultAssociationManagement();
        AssociationDescriptor descriptor = AssociationDescriptor.create("Dummy", Dummy.class, System.getProperty("org.rioproject.groups"));
        descriptor.setMatchOnName(false);
        descriptor.setVersion("2.1");
        descriptor.setAssociationMatchFilter(new VersionMatchFilter());
        Association<Dummy> association = aMgr.addAssociationDescriptor(descriptor);
        Dummy dummy = association.getServiceFuture().get();
        Assert.assertNotNull(dummy);
        Assert.assertEquals("Expected 1 got "+association.getServiceCount(), 1, association.getServiceCount());
        Assert.assertTrue("Expected \'Other Darrel\', got "+dummy.getName(), "Other Darrel".equals(dummy.getName()));
    }
}
