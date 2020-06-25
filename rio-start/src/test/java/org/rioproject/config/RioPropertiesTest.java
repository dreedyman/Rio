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
package org.rioproject.config;

import org.junit.Assert;
import org.junit.Test;

/**
 * Test loading properties from rio.env
 */
public class RioPropertiesTest {

    @Test
    public void testLoadFromEnv() {
        try {
            System.setProperty(Constants.ENV_PROPERTY_NAME,
                               System.getProperty("user.dir") + "/src/test/resources/config/rio.env");
            RioProperties.load();
            String locators = System.getProperty(Constants.LOCATOR_PROPERTY_NAME);
            Assert.assertNotNull(locators);
        } finally {
            System.clearProperty(Constants.ENV_PROPERTY_NAME);
        }
    }

    @Test
    public void testLoadFromRioHome()  {
        String oldRioHome = System.getProperty("rio.home");
        try {
            System.setProperty("rio.home", System.getProperty("user.dir") + "/src/test/resources");
            RioProperties.load();
            String locators = System.getProperty(Constants.LOCATOR_PROPERTY_NAME);
            Assert.assertNotNull(locators);
        } finally {
            if (oldRioHome == null)
                System.clearProperty("RIO_HOME");
            else
                System.setProperty("rio.home", oldRioHome);
        }
    }
}
