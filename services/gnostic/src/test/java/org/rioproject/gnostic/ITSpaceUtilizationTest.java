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
package org.rioproject.gnostic;

import net.jini.space.JavaSpace;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.rioproject.test.RioTestRunner;
import org.rioproject.test.SetTestManager;
import org.rioproject.test.TestManager;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

/**
 * Test forked space access
 */
@RunWith (RioTestRunner.class)
public class ITSpaceUtilizationTest {
    @SetTestManager
    static TestManager testManager;
    final long WAIT=1000*30;

    @Test
    public void verifyGettingMetricsFromDeployedSpaceWorks() {
        doTest(testManager);
    }

    void doTest(TestManager testManager) {
        Assert.assertNotNull(testManager);
        Gnostic g = (Gnostic)testManager.waitForService(Gnostic.class);
        Util.waitForRule(g, "SpaceUtilization");
        testManager.waitForService(JavaSpace.class);
        File file = new File(System.getProperty("java.io.tmpdir"), getOutputFileName());
        if(file.exists())
            if(file.delete())
                System.out.println("Removed "+file.getName()+", start with clean slate");
        file.deleteOnExit();
        long waited = 0;
        long duration = 1000;
        while(waited<=WAIT) {
            try {
                Thread.sleep(duration);
            } catch (InterruptedException e) {
                Thread.interrupted();
            }
            waited += duration;
        }
        Assert.assertTrue(file.exists());
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
