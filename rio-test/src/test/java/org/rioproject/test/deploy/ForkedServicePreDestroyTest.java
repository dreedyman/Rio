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
package org.rioproject.test.deploy;

import com.sun.jini.admin.DestroyAdmin;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.rioproject.opstring.OperationalStringManager;
import org.rioproject.cybernode.Cybernode;
import org.rioproject.test.IfPropertySet;
import org.rioproject.test.RioTestRunner;
import org.rioproject.test.SetTestManager;
import org.rioproject.test.TestManager;
import org.rioproject.test.simple.ForkImpl;

import java.io.File;

/**
 * Test that a forked service has it's preDestroy method invoked if a Cybernode goes away
 */
@RunWith(RioTestRunner.class)
@IfPropertySet(name = "os.name", notvalue = "Windows*")
public class ForkedServicePreDestroyTest {
    @SetTestManager
    static TestManager testManager;
    Cybernode cybernode;

    @Before
    public void setup() {
        cybernode = (Cybernode)testManager.waitForService(Cybernode.class);
    }

    @Test
    public void testForkServiceHasPreDestroyInvoked() {
        Assert.assertNotNull(testManager);
        Assert.assertNotNull(cybernode);
        Throwable thrown = null;
        try {
            OperationalStringManager mgr = testManager.getOperationalStringManager();
            Assert.assertNotNull("Expected non-null OperationalStringManager", mgr);
            testManager.waitForDeployment(mgr);
            File marker = ForkImpl.getMarkerFile();
            Assert.assertTrue("The marker file should exist", marker.exists());
            DestroyAdmin dAdmin = (DestroyAdmin)cybernode.getAdmin();
            dAdmin.destroy();
            long t0 = System.currentTimeMillis();
            long maxWait=60*1000;
            long interval = 1000;
            long waited = 0;
            while(marker.exists() && waited <maxWait) {
                System.err.println("Waiting for "+marker.getAbsolutePath()+" to be removed");
                Thread.sleep(interval);
                waited += interval;
            }
            long t1 = System.currentTimeMillis();
            System.err.println("Elapsed time to marker being removed: "+((double)(t1-t0)/1000));
        } catch(Exception e) {
            thrown = e;
            e.printStackTrace();
        }
        Assert.assertNull("Should not have thrown an exception", thrown);
    }

}
