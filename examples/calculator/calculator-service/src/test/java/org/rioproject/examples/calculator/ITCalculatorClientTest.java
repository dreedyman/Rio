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
package org.rioproject.examples.calculator;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.rioproject.associations.Association;
import org.rioproject.associations.AssociationDescriptor;
import org.rioproject.associations.AssociationManagement;
import org.rioproject.associations.AssociationMgmt;
import org.rioproject.test.RioTestRunner;
import org.rioproject.test.SetTestManager;
import org.rioproject.test.TestManager;

import java.rmi.RemoteException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * Tests the Calculator service using Rio association management as a client.
 */
@RunWith (RioTestRunner.class)
public class ITCalculatorClientTest extends ITAbstractCalculatorTest {
	@SetTestManager
    static TestManager testManager;
    Future<Calculator> future;

    @Before
    public void setup() throws Exception {
	    Assert.assertNotNull(testManager);
        /*
         * Call the static create method providing the service name, service
         * interface class and discovery group name(s). Note the the returned
         * AssociationDescriptor can be modified for additional options as well.
         */
        AssociationDescriptor descriptor = AssociationDescriptor.create("Calculator",
                                                                        Calculator.class,
                                                                        testManager.getGroups());
        /* Create association management and get the Future. */
        AssociationManagement aMgr = new AssociationMgmt();
        Association<Calculator> association = aMgr.addAssociationDescriptor(descriptor);
        future = association.getServiceFuture();
    }

    @Test
    public void testInjectedService() throws RemoteException, ExecutionException, InterruptedException {
        Calculator service = future.get();
        Assert.assertNotNull(service);
        testService(service);
    }
}
