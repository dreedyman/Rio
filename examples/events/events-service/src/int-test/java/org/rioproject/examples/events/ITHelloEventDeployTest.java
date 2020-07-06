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
package org.rioproject.examples.events;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.rioproject.test.RioTestConfig;
import org.rioproject.test.RioTestRunner;
import org.rioproject.test.SetTestManager;
import org.rioproject.test.TestManager;

import java.rmi.RemoteException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Testing the events example using the Rio test framework
 */
@RunWith (RioTestRunner.class)
@RioTestConfig (
        groups = "HelloEvent",
        numCybernodes = 1,
        numMonitors = 1,
        opstring = "opstring/events.groovy"
)
public class ITHelloEventDeployTest {
    @SetTestManager
    static TestManager testManager;
    private Hello eventProducer;

    @Before
    public void setup() {
	    assertNotNull(testManager);
        eventProducer = testManager.waitForService(Hello.class);
    }

    @Test
    public void testService() throws RemoteException {
        assertNotNull(eventProducer);
        eventProducer.sayHello("Hola");
        assertEquals(1, eventProducer.getNotificationCount());
    }
}
