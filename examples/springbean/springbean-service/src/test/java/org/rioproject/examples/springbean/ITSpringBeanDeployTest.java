package org.rioproject.examples.springbean;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.rioproject.test.RioTestRunner;
import org.rioproject.test.SetTestManager;
import org.rioproject.test.TestManager;

import java.rmi.RemoteException;

/**
 * Testing the SpringBean service using the Rio test framework
 */
@RunWith(RioTestRunner.class)
public class ITSpringBeanDeployTest {
	@SetTestManager
    static TestManager testManager;
    static Hello service;

    @BeforeClass
    public static void setup() throws Exception {
	    Assert.assertNotNull(testManager);       
        service = (Hello)testManager.waitForService(Hello.class);
    }

    @Test
    public void testBean() throws RemoteException {
        Assert.assertNotNull(service);
        for(int i=1; i<10; i++) {
            String result = service.hello("Test Client");
            Assert.assertEquals("Hello visitor : "+i, result);
        }
    }
}
