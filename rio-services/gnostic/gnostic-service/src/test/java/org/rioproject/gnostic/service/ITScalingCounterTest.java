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

import org.junit.Assert;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.rioproject.gnostic.Gnostic;
import org.rioproject.gnostic.service.test.ExecutionNodeService;
import org.rioproject.test.RioTestRunner;
import org.rioproject.test.SetTestManager;
import org.rioproject.test.TestManager;

import java.io.File;
import java.util.List;

/**
 * 
 */
@RunWith(RioTestRunner.class)
public class ITScalingCounterTest {
    @SetTestManager
    static TestManager testManager;
    static String NAME = "test-1.0";

    @BeforeClass
    public static void createArtifact() {
        Assume.assumeTrue(Util.getJVMVersion() < 1.8);

        File f = Util.createJar(NAME);
        Util.writePom(NAME);
    }

    @Test
    public void verifyScalingOnWatchCounterWorks() {
        Throwable thrown = null;
        Assert.assertNotNull(testManager);
        Assert.assertNotNull(testManager);
        File opstring = new File("src/test/opstring/executionNodeService.groovy");
        Assert.assertTrue(opstring.exists());
        testManager.deploy(opstring);
        Gnostic g = (Gnostic)testManager.waitForService(Gnostic.class);
        Util.waitForRule(g, "SLAKsessions");
        ExecutionNodeService test = (ExecutionNodeService)testManager.waitForService("ExecutionNodeService");

        for(int i=0; i<15; i++) {
            try {
                test.incrementSessionCounter();
                sleep(1000);
            } catch (Exception e) {
                e.printStackTrace();
                thrown = e;
            }
            Assert.assertNull(thrown);
        }
        List l = (List)testManager.getServices(ExecutionNodeService.class);
        while(l.size()<5) {
            System.out.println("Waiting for 5 services, have "+l.size());
            sleep(2000);
            l = (List)testManager.getServices(ExecutionNodeService.class);
        }
        Assert.assertTrue(l.size()==5);
    }

    private void sleep(long l) {
        try {
            Thread.sleep(l);
        } catch (InterruptedException e) {

        }
    }

}
