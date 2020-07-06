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

import org.junit.Before;
import org.junit.Test;
import org.rioproject.cybernode.StaticCybernode;

import java.io.File;
import java.net.URL;
import java.rmi.RemoteException;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Example testing the Calculator service and it's required services from the
 * <tt>OperationalString</tt>
 */
public class CalculatorTest {
    String opstring = "opstring/calculator.groovy";
    Calculator calculator;

    @Before
    public void setupCalculator() throws Exception {
        StaticCybernode cybernode = new StaticCybernode();
        URL url = CalculatorTest.class.getClassLoader().getResource(opstring);
        assertNotNull(url);
        File opStringFile = new File(url.toURI());
        Map<String, Object> map = cybernode.activate(opStringFile);
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String beanName = entry.getKey();
            Object beanImpl = entry.getValue();
            if (beanName.equals("Calculator"))
                calculator = (Calculator) beanImpl;
        }
    }

    @Test
    public void testBean() throws RemoteException {
        assertNotNull(calculator);
        double sum = calculator.add(3, 2);
        assertEquals(sum, 5, 0);

        double difference = calculator.subtract(3, 2);
        assertEquals(difference, 1, 0);

        double dividend = calculator.divide(10, 10);
        assertEquals(dividend, 1, 0);

        double product = calculator.multiply(10, 10);
        assertEquals(product, 100, 0);
    }

}
