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
package org.rioproject.gnostic;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.rioproject.deploy.ServiceBeanInstantiationException;
import org.rioproject.cybernode.StaticCybernode;


/**
 * Test getting and setting scanner interval programmatically.
 */
public class ScannerIntervalTest {
    Gnostic g;

    @Before
    public void setup() throws ServiceBeanInstantiationException {
        StaticCybernode cybernode = new StaticCybernode();
        g = (Gnostic)cybernode.activate(GnosticImpl.class.getName());
    }
     @Test
     public void verifySettingAndGettingScannerIntervalBehavesAsDesigned() {
         Assert.assertNotNull(g);
         Throwable thrown = null;
         try {
             int scannerInterval = g.getScannerInterval();
             Assert.assertEquals("Expected scannerInterval to be 30", 30, scannerInterval);
         } catch (Throwable t) {
             t.printStackTrace();
             thrown = t;
         }
         Assert.assertNull(thrown);

         try {
             g.setScannerInterval(60);
             int scannerInterval = g.getScannerInterval();
             Assert.assertEquals("Expected scannerInterval to be 60", 60, scannerInterval);
         } catch (Throwable t) {
             t.printStackTrace();
             thrown = t;
         }
         Assert.assertNull(thrown);
         try {
             g.setScannerInterval(0);
         } catch (Throwable t) {
             thrown = t;
         }
         Assert.assertNotNull(thrown);
         Assert.assertTrue(thrown instanceof IllegalArgumentException);
         thrown = null;
         try {
             int scannerInterval = g.getScannerInterval();
             Assert.assertEquals("Expected scannerInterval to be 60", 60, scannerInterval);
         } catch (Throwable t) {
             t.printStackTrace();
             thrown = t;
         }
         Assert.assertNull(thrown);
     }
}
