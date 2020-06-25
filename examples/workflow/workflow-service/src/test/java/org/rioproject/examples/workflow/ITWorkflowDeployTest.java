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
package org.rioproject.examples.workflow;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.rioproject.test.RioTestConfig;
import org.rioproject.test.RioTestRunner;
import org.rioproject.test.SetTestManager;
import org.rioproject.test.TestManager;

import java.rmi.RemoteException;

/**
 * Testing the Workflow example using the Rio test framework
 */
@RunWith (RioTestRunner.class)
@RioTestConfig (
        groups = "Workflow",
        numCybernodes = 1,
        numMonitors = 1,
        opstring = "../src/main/opstring/workflow.groovy"
)
public class ITWorkflowDeployTest {
    @SetTestManager
    static TestManager testManager;
    static Master master;

    @BeforeClass
    public static void setup(){
	    Assert.assertNotNull(testManager);
        master = testManager.waitForService(Master.class);
    }

    @Test
    public void testBean() throws RemoteException, WorkflowException {
        Assert.assertNotNull(master);
        System.out.println("Submitting Order ...");
        WorkflowEntry result = master.process();
        System.out.println("Order result is : " + result.value);
    }

}
