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
import static org.junit.Assert.assertEquals;
import java.rmi.RemoteException;

/**
 * Base testing class for the Calculator service
 */
public abstract class ITAbstractCalculatorTest {
    
    protected void testService(Calculator calculator) throws RemoteException {
        Assert.assertNotNull(calculator);
        add(calculator);
        subtract(calculator);
        divide(calculator);
        multiply(calculator);
    }

    private void add(Calculator calculator) throws RemoteException {
        double val = calculator.add(3, 2);
        assertEquals(val, 5, 0);
        System.out.println("    3 + 2 = " + val);
    }

    private void subtract(Calculator calculator) throws RemoteException {
        double val = calculator.subtract(3, 2);
        assertEquals(val, 1, 0);
        System.out.println("    3 - 2 = " + val);
    }

    private void divide(Calculator calculator) throws RemoteException {
        double val = calculator.divide(10, 10);
        assertEquals(val, 1, 0);
        System.out.println("    10 % 10 = " + val);
    }

    private void multiply(Calculator calculator) throws RemoteException {
        double val = calculator.multiply(10, 10);
        assertEquals(val, 100, 0);
        System.out.println("    10 * 10 = " + val);
    }

    void sleep(long duration) {
        try {
            Thread.sleep(duration);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

}
