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
import net.jini.lookup.entry.Name;
import net.jini.space.JavaSpace05;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.rioproject.entry.OperationalStringEntry;
import org.rioproject.test.RioTestRunner;
import org.rioproject.test.SetTestManager;
import org.rioproject.test.TestManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Test that non-Rio services have additional attributes
 *
 * @author Dennis Reedy
 */
@RunWith(RioTestRunner.class)
public class HasExtraAttributesTest {
    @SetTestManager
    static TestManager testManager;
    private static final Logger logger = LoggerFactory.getLogger(HasExtraAttributesTest.class.getName());

    @Test
    public void testHasOperationalStringEntryAndName() {
        Assert.assertNotNull(testManager);
        testManager.waitForService(JavaSpace05.class);
        ServiceItem[] items = testManager.getServiceItems(JavaSpace05.class);
        Assert.assertEquals(1, items.length);
        Name name = null;
        OperationalStringEntry operationalStringEntry = null;
        for(Entry entry : items[0].attributeSets) {
            logger.info(String.format("Found %s", entry.getClass().getName()));
            if(entry instanceof OperationalStringEntry) {
                operationalStringEntry = (OperationalStringEntry) entry;
            }
            if(entry instanceof Name) {
                name = (Name) entry;
            }
        }
        Assert.assertNotNull("The OperationalStringEntry is null", operationalStringEntry);
        Assert.assertEquals("Spaced", operationalStringEntry.name);
        logger.info(String.format("OperationalStringEntry: %s", operationalStringEntry.name));

        Assert.assertNotNull("The Name is null", name);
        Assert.assertEquals("Spaced Out", name.name);
        logger.info(String.format("Name: %s", name.name));
    }
}
