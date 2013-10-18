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
package org.rioproject.test.cybernode;

import net.jini.core.entry.Entry;
import net.jini.core.lookup.ServiceItem;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.rioproject.RioVersion;
import org.rioproject.test.RioTestRunner;
import org.rioproject.test.SetTestManager;
import org.rioproject.test.TestManager;
import org.rioproject.test.simple.Simple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tests the parsing, loading and registration of custom attributes
 *
 * @author Dennis Reedy
 */
@RunWith(RioTestRunner.class)
public class CustomAttributesTest {
    @SetTestManager
    static TestManager testManager;
    private static final Logger logger = LoggerFactory.getLogger(HasExtraAttributesTest.class.getName());

    @Test
    public void testHasOperationalStringEntryAndName() {
        Assert.assertNotNull(testManager);
        testManager.waitForService(Simple.class);
        ServiceItem[] items = testManager.getServiceItems(Simple.class);
        Assert.assertEquals(1, items.length);
        Entry simpleEntry = null;
        for(Entry entry : items[0].attributeSets) {
            logger.info(String.format("Found %s", entry.getClass().getName()));
            if(entry.getClass().getName().equals("org.rioproject.test.simple.SimpleEntry")) {
                simpleEntry = entry;
                break;
            }
        }
        Assert.assertNotNull("The SimpleEntry is null", simpleEntry);
        Assert.assertEquals(RioVersion.VERSION, simpleEntry.toString());
        logger.info(String.format("SimpleEntry: %s", simpleEntry));
    }
}
