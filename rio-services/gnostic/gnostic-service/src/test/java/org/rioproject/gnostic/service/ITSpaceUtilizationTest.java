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
package org.rioproject.gnostic.service;

import net.jini.core.lookup.ServiceItem;
import net.jini.space.JavaSpace;
import org.junit.*;
import org.junit.runner.RunWith;
import org.rioproject.gnostic.Gnostic;
import org.rioproject.monitor.ProvisionMonitor;
import org.rioproject.test.RioTestRunner;
import org.rioproject.test.SetTestManager;
import org.rioproject.test.TestManager;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

/**
 * Test forked space access and embedded spaces access
 */
@RunWith (RioTestRunner.class)
public class ITSpaceUtilizationTest {
    @SetTestManager
    static TestManager testManager;
    final long WAIT=1000*30;

    @BeforeClass
    public static void check() {
        Assume.assumeTrue(Util.getJVMVersion() < 1.8);
    }

    @Test
    public void verifyGettingMetricsFromDeployedSpaceWorks() {
        Assert.assertNotNull(testManager);
        File opstring = new File("src/test/opstring/testspace.groovy");
        Assert.assertTrue(opstring.exists());
        doTest(testManager, opstring);
    }

    @Test
    public void verifyGettingMetricsFromDeployedForkedSpaceWorks() {
        Assert.assertNotNull(testManager);
        File opstring = new File("src/test/opstring/testforkedspace.groovy");
        Assert.assertTrue(opstring.exists());
        doTest(testManager, opstring);
    }

    @After
    public void undeploy() {
        ServiceItem[] items = testManager.getServiceItems(ProvisionMonitor.class);
        Assert.assertTrue(items.length>0);
        testManager.undeployAll((ProvisionMonitor)items[0].service);
    }

    void doTest(TestManager testManager, File opstring) {
        Assert.assertNotNull(testManager);
        testManager.deploy(opstring);
        Gnostic g = (Gnostic)testManager.waitForService(Gnostic.class);
        File file = new File(System.getProperty("java.io.tmpdir"), getOutputFileName());
        if(file.exists())
            if(file.delete())
                System.out.println("Removed "+file.getName()+", start with clean slate");
        Util.waitForRule(g, "SpaceUtilization");
        testManager.waitForService(JavaSpace.class);        
        file.deleteOnExit();
        long waited = 0;
        long duration = 1000;
        System.out.println("wait 30 seconds to accumulate results...");
        while(waited<=WAIT) {
            try {
                Thread.sleep(duration);
            } catch (InterruptedException e) {
                Thread.interrupted();
            }
            waited += duration;
        }
        Assert.assertTrue("Expected "+file.getPath()+" to exist, it does not", file.exists());
        int lineCount = 0;
        Throwable thrown = null;
        try {
            BufferedReader in = new BufferedReader(new FileReader(file));
            String str;
            while ((str = in.readLine()) != null) {
                //process(str);
                System.out.println(str);
                lineCount++;
            }
            in.close();
        } catch (IOException e) {
            thrown = e;
            e.printStackTrace();
        }
        Assert.assertTrue(thrown==null);
        Assert.assertTrue(lineCount>0);
    }

    protected String getOutputFileName() {
        return "SpaceUtilizationTest.out";
    }
}
