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
package org.rioproject.examples.calculator;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.rioproject.test.RioTestConfig;
import org.rioproject.test.RioTestRunner;
import org.rioproject.test.SetTestManager;
import org.rioproject.test.TestManager;

import java.rmi.RemoteException;

/**
 * Testing the Calculator service using the Rio test framework
 */
@RunWith (RioTestRunner.class)
@RioTestConfig (
        groups = "Calculator",
        numCybernodes = 1,
        numMonitors = 1,
        opstring = "opstring/calculator.groovy"
)
public class ITCalculatorDeployTest {
	@SetTestManager
    static TestManager testManager;
    Calculator service;
    CalculatorTester calculatorTester;

    @Before
    public void setup() {
        calculatorTester = new CalculatorTester();
	    Assert.assertNotNull(testManager);
        service = testManager.waitForService(Calculator.class);
    }

    @Test
    public void test1() throws RemoteException {
        calculatorTester.verify(service);
    }
}
