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
package org.rioproject.test.associations;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.rioproject.config.Constants;
import org.rioproject.net.HostUtil;
import org.rioproject.opstring.ClassBundle;
import org.rioproject.opstring.OperationalStringManager;
import org.rioproject.opstring.ServiceBeanConfig;
import org.rioproject.opstring.ServiceElement;
import org.rioproject.test.RioTestRunner;
import org.rioproject.test.SetTestManager;
import org.rioproject.test.TestManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Test that serviceDiscoveryTimeout works
 *
 * @author Dennis Reedy
 */
@RunWith(RioTestRunner.class)
public class AssociationManagementTest {
    @SetTestManager
    static TestManager testManager;
    OperationalStringManager manager;
    Logger logger = LoggerFactory.getLogger(AssociationManagementTest.class);

    @Before
    public void setup() {
        manager = testManager.getOperationalStringManager();
        testManager.waitForDeployment(manager);
    }

    @Test
    public void testServiceDiscoveryTimeout() throws Exception {
        Dummy darrel = (Dummy) testManager.waitForService(Dummy.class);
        new Thread(new Runnable() {
            @Override public void run() {
                ServiceElement elem = new ServiceElement();
                elem.setPlanned(1);
                try {
                    String codebase = "http://"+HostUtil.getHostAddressFromProperty(Constants.RMI_HOST_ADDRESS)+":9010";
                    elem.setExportBundles(makeClassBundle(Dummy.class.getName(), codebase));
                    elem.setComponentBundle(makeClassBundle(DummyImpl.class.getName(), codebase));

                    ServiceBeanConfig sbc = new ServiceBeanConfig();
                    sbc.setName("Add");
                    sbc.setGroups(System.getProperty("org.rioproject.groups"));
                    sbc.setOperationalStringName("association stuff");
                    elem.setServiceBeanConfig(sbc);

                    logger.info("Wait 5 seconds to deploy another dummy...");
                    Thread.sleep(5*1000);
                    logger.info("Deploying another dummy...");
                    manager.addServiceElement(elem, null);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();

        int index = darrel.getIndexFromAnotherDummy();
        logger.info("The other dummy returned: {}", index);
    }

    ClassBundle makeClassBundle(String className, String codebase) {
        ClassBundle classBundle = new ClassBundle();
        classBundle.addJAR("test-classes/");
        classBundle.setClassName(className);
        classBundle.setCodebase(codebase);
        return classBundle;
    }
}
