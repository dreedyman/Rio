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
import org.rioproject.test.RioTestRunner;
import org.rioproject.test.SetTestManager;
import org.rioproject.test.TestManager;

/**
 * Testing the Calculator service using the Rio test framework
 */
@RunWith (RioTestRunner.class)
public class ITCalculatorDeployTest extends ITAbstractCalculatorTest {
	@SetTestManager
    static TestManager testManager;
    Calculator service;

    @Before
    public void setup() throws Exception {
	    Assert.assertNotNull(testManager);
        service = (Calculator)testManager.waitForService(Calculator.class);
    }

    @Test
    public void test1() {
        Throwable thrown = null;
        try {
            testService(service);
        } catch (Exception e) {
            thrown = e;
            e.printStackTrace();
        }
        Assert.assertNull("Should not have thrown an exception", thrown);
    }
}
