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
package org.rioproject.monitor.service;

import junit.framework.Assert;
import net.jini.config.Configuration;
import org.junit.Test;
import org.rioproject.impl.config.DynamicConfiguration;
import org.rioproject.impl.opstring.OpString;
import org.rioproject.opstring.OperationalString;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * Test basic interactions with the {@code DefaultOpStringManager}
 *
 * @author Dennis Reedy
 */
public class DefaultOpStringManagerTest {

    @Test
    public void testSimpleStandAlone() throws IOException {
        OperationalString opString = new OpString("test", null);
        OpStringManager parent = null;
        boolean active = true;
        Configuration config = new DynamicConfiguration();
        OpStringManagerController opStringManagerController = new OpStringManagerController();
        DefaultOpStringManager manager = new DefaultOpStringManager(opString,
                                                                    parent,
                                                                    active,
                                                                    config,
                                                                    opStringManagerController);
        Assert.assertTrue(manager.isStandAlone());
        Assert.assertTrue(manager.getParentCount()==0);
    }

    @Test
    public void testNestedStandAlone() throws Exception {
        OpStringManagerController opStringManagerController = new OpStringManagerController();
        opStringManagerController.setConfig(new DynamicConfiguration());
        OperationalString opString1 = new OpString("test-1", null);

        Map<String, Throwable> errorMap = new HashMap<String, Throwable>();
        OpStringManager manager1 = opStringManagerController.addOperationalString(opString1,
                                                                                 errorMap,
                                                                                 null,
                                                                                 TestUtil.createDeployAdmin(),
                                                                                 null);
        Assert.assertTrue(manager1.isStandAlone());
        Assert.assertTrue(manager1.getParentCount()==0);

        OpString opString2 = new OpString("test-2", null);
        opString2.addOperationalString(opString1);
        OpStringManager manager2 = opStringManagerController.addOperationalString(opString2,
                                                                                 errorMap,
                                                                                 null,
                                                                                 TestUtil.createDeployAdmin(),
                                                                                 null);

        Assert.assertTrue(opStringManagerController.getOpStringManagers().length==2);
        Assert.assertFalse(manager2.isStandAlone());
        Assert.assertTrue(manager2.getParentCount()==0);
        OperationalString[] terminated = manager2.terminate(true);
        Assert.assertTrue(terminated.length==1);
        Assert.assertTrue(opStringManagerController.getOpStringManagers().length==1);
    }

    @Test
    public void testNested() throws Exception {
        OpStringManagerController opStringManagerController = new OpStringManagerController();
        opStringManagerController.setConfig(new DynamicConfiguration());
        OperationalString opString1 = new OpString("test-1", null);

        Map<String, Throwable> errorMap = new HashMap<String, Throwable>();        

        OpString opString2 = new OpString("test-2", null);
        opString2.addOperationalString(opString1);
        OpStringManager manager2 = opStringManagerController.addOperationalString(opString2,
                                                                                 errorMap,
                                                                                 null,
                                                                                 TestUtil.createDeployAdmin(),
                                                                                 null);
        OpStringManager manager1 = opStringManagerController.getOpStringManager("test-1");
        Assert.assertNotNull(manager1);
        Assert.assertTrue(opStringManagerController.getOpStringManagers().length==2);
        Assert.assertFalse(manager1.isStandAlone());
        Assert.assertFalse(manager2.isStandAlone());
        
        Assert.assertTrue(manager1.getParentCount()==1);
        Assert.assertTrue(manager2.getParentCount()==0);
        OperationalString[] terminated = manager2.terminate(true);
        Assert.assertTrue(terminated.length==2);
        Assert.assertTrue(opStringManagerController.getOpStringManagers().length==0);
    }

}
