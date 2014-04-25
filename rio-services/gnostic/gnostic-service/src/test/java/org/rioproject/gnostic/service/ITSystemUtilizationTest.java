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
import org.rioproject.gnostic.service.test.TestService;
import org.rioproject.test.RioTestRunner;
import org.rioproject.test.SetTestManager;
import org.rioproject.test.TestManager;

import java.io.IOException;

/**
 * Test SystemUtilization rule
 */
@RunWith (RioTestRunner.class)
public class ITSystemUtilizationTest {
    @SetTestManager
    static TestManager testManager;
    final long WAIT=1000*60;

    @BeforeClass
    public static void check() {
        Assume.assumeTrue(Util.getJVMVersion() < 1.8);
    }

    @Test
    public void verifyThatInvocationsOnTestServiceAreDisallowedIfUtilizationIsBreached() {
        Assert.assertNotNull(testManager);
        TestService t = testManager.waitForService(TestService.class);
        Assert.assertNotNull(t);
        Throwable thrown = null;
        try {
            long waited = 0;
            long duration = 1000;
            while(t.getStatus().equals(TestService.Status.ALLOWED) && waited<=WAIT) {
                try {
                    Thread.sleep(duration);
                } catch (InterruptedException e) {
                    Thread.interrupted();
                }
                waited += duration;
            }
            Assert.assertTrue("Got "+t.getStatus().name(), t.getStatus().equals(TestService.Status.DISALLOWED));
        } catch (IOException e) {
            thrown = e;
            e.printStackTrace();
        }
        Assert.assertNull(thrown);
    }
}
