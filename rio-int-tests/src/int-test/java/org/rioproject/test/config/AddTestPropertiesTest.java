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
package org.rioproject.test.config;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.rioproject.test.*;
import org.rioproject.test.simple.Simple;

import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import static org.junit.Assert.assertNotNull;

/**
 * Tests that you can add system properties to service creation when running tests
 */
@RunWith(RioTestRunner.class)
@RioTestConfig(
        groups = "AddTestPropertiesTest",
        numCybernodes = 1,
        numMonitors = 1,
        numLookups = 1,
        opstring = "src/int-test/resources/opstring/simple_opstring.groovy"
)
public class AddTestPropertiesTest {
    @SetTestManager
    private static TestManager testManager;

    @AddSystemProperties
    public static Map setProps() {
        Map<String,String> options = new HashMap<>();
        options.put("org.rioproject.flaps", "100");
        options.put("org.rioproject.gear", "up");
        return options;
    }

    @Test
    public void verifyPropertiesSet() throws RemoteException {
        Simple service = testManager.waitForService(Simple.class);
        Properties properties = service.getSystemProperties();
        assertNotNull(properties.getProperty("org.rioproject.flaps"));
        assertNotNull(properties.getProperty("org.rioproject.gear"));
    }
}
