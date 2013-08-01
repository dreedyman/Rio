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
package org.rioproject.opstring;

import org.junit.Assert;
import org.junit.Test;

import java.io.File;

/**
 * @author Dennis Reedy
 */
public class OpStringLoaderTest {
    @Test
    public void testOpStringLoader() throws Exception {
        OpStringLoader opStringLoader = new OpStringLoader();
        opStringLoader.setDefaultGroups("banjo");
        String baseDir = System.getProperty("user.dir");
        File calculator = new File(baseDir, "src/test/resources/opstrings/opstringloadertest.groovy");
        OperationalString[] opStrings = opStringLoader.parseOperationalString(calculator);
        Assert.assertNotNull(opStrings);
        Assert.assertEquals("Should have only 1 opstring", 1, opStrings.length);
        Assert.assertEquals("Should have 1 service", 1, opStrings[0].getServices().length);
        Assert.assertEquals(1, opStrings[0].getServices()[0].getServiceBeanConfig().getGroups().length);
        Assert.assertEquals("banjo", opStrings[0].getServices()[0].getServiceBeanConfig().getGroups()[0]);
    }
}
