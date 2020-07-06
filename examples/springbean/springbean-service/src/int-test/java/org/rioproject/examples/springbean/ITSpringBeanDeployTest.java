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
package org.rioproject.examples.springbean;

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
 * Testing the SpringBean service using the Rio test framework
 */
@RunWith(RioTestRunner.class)
@RioTestConfig (
        groups = "SpringBean",
        numCybernodes = 1,
        numMonitors = 1,
        opstring = "opstring/springbean.groovy"
)
public class ITSpringBeanDeployTest {
	@SetTestManager
    static TestManager testManager;
    static Hello service;

    @BeforeClass
    public static void setup() {
	    Assert.assertNotNull(testManager);       
        service = testManager.waitForService(Hello.class);
    }

    @Test
    public void testBean() throws RemoteException {
        Assert.assertNotNull(service);
        for (int i = 1; i < 10; i++) {
            String result = service.hello("Test Client");
            Assert.assertEquals("Hello visitor : "+i, result);
        }
    }
}
