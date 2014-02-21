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
import net.jini.id.UuidFactory;
import org.junit.Test;
import org.rioproject.impl.config.DynamicConfiguration;
import org.rioproject.impl.opstring.OpString;
import org.rioproject.opstring.OperationalString;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

/**
 * @author Dennis Reedy
 */
public class OpStringManagerControllerTest {
    @Test
    public void testAddRemove() throws Exception {
        OperationalString opString = new OpString("test", null);
        opString.addService(TestUtil.makeServiceElement("foo", "test"));
        Configuration config = new DynamicConfiguration();
        OpStringManagerController opStringManagerController = new OpStringManagerController();
        opStringManagerController.setServiceProxy(TestUtil.createProvisionMonitor());
        opStringManagerController.setEventProcessor(new ProvisionMonitorEventProcessor(config));
        opStringManagerController.setConfig(config);
        opStringManagerController.setServiceProvisioner(new ServiceProvisioner(config,
                                                                               TestUtil.createProvisionMonitor(),
                                                                               TestUtil.createEventHandler(),
                                                                               null));
        opStringManagerController.setUuid(UuidFactory.generate());
        Map<String, Throwable> map = new HashMap<String, Throwable>();
        OpStringManager manager = opStringManagerController.addOperationalString(opString, map, null, null, null);
        Assert.assertNotNull(manager);
        Assert.assertTrue(opStringManagerController.opStringExists(manager.getName()));
        opStringManagerController.undeploy(manager, true);
        String name = manager.getName();
        manager = opStringManagerController.getOpStringManager(name);
        Assert.assertNull(manager);
        Assert.assertFalse(opStringManagerController.opStringExists(name));
    }

    @Test
    public void concurrentCreationTest() throws ExecutionException, InterruptedException {
        OpStringManagerController opStringManagerController = new OpStringManagerController();
        opStringManagerController.setConfig(new DynamicConfiguration());
        OperationalString opString = new OpString("concurrent", null);

        List<Future<OpStringManager>> futures = new ArrayList<Future<OpStringManager>>();
        for(int i=0; i<50; i++) {
            Callable<OpStringManager> deployer = new Deployer(opStringManagerController, opString);
            FutureTask<OpStringManager> task = new FutureTask<OpStringManager>(deployer);
            futures.add(task);
            new Thread(task).start();
        }
        System.out.println("Created "+futures.size()+" threads to bang on one OpStringManagerController");
        for(Future<OpStringManager> future : futures) {
            OpStringManager opStringManager = future.get();
            Assert.assertNotNull(opStringManager);
        }
        Assert.assertEquals(1, opStringManagerController.getOpStringManagers().length);
    }

    class Deployer implements Callable<OpStringManager> {
        final OpStringManagerController opStringManagerController;
        final OperationalString operationalString;

        Deployer(OpStringManagerController opStringManagerController, OperationalString operationalString) {
            this.opStringManagerController = opStringManagerController;
            this.operationalString = operationalString;
        }

        public void run() {
            Map<String, Throwable> errorMap = new HashMap<String, Throwable>();
            try {
                opStringManagerController.addOperationalString(operationalString,
                                                               errorMap,
                                                               null,
                                                               TestUtil.createDeployAdmin(),
                                                               null);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        public OpStringManager call() throws Exception {
            OpStringManager opStringManager = null;
            Map<String, Throwable> errorMap = new HashMap<String, Throwable>();
            try {
                opStringManager = opStringManagerController.addOperationalString(operationalString,
                                                                                 errorMap,
                                                                                 null,
                                                                                 TestUtil.createDeployAdmin(),
                                                                                 null);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return opStringManager;
        }
    }
}
