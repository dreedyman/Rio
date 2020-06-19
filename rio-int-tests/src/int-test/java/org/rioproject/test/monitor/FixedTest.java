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
package org.rioproject.test.monitor;

import net.jini.lookup.ServiceDiscoveryManager;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.rioproject.cybernode.Cybernode;
import org.rioproject.deploy.DeployAdmin;
import org.rioproject.deploy.ServiceBeanInstance;
import org.rioproject.impl.opstring.OpStringLoader;
import org.rioproject.monitor.ProvisionMonitor;
import org.rioproject.opstring.OperationalString;
import org.rioproject.opstring.OperationalStringException;
import org.rioproject.opstring.OperationalStringManager;
import org.rioproject.opstring.ServiceElement;
import org.rioproject.test.*;

import java.io.File;
import java.rmi.RemoteException;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

/**
 * @author Dennis Reedy
 */
@RunWith(RioTestRunner.class)
@RioTestConfig (
        groups = "FixedTest",
        //locators = ""
        numCybernodes = 4,
        numMonitors = 1,
        numLookups = 1,
        opstring = "src/int-test/resources/opstring/fixed.groovy",
        autoDeploy = false
)
public class FixedTest {
    @SetTestManager
    private TestManager testManager;

    @Test
    public void testDeployMap1() throws Exception {
        Assert.assertNotNull(testManager);
        ServiceDiscoveryManager sdm = testManager.getServiceDiscoveryManager();
        ServiceMonitor<Cybernode> cyberMon = new ServiceMonitor<>(sdm, Cybernode.class);
        cyberMon.waitFor(4);
        ServiceMonitor<ProvisionMonitor> pmMon = new ServiceMonitor<ProvisionMonitor>(sdm, ProvisionMonitor.class);
        pmMon.waitFor(1);
        ProvisionMonitor monitor = pmMon.getServices().get(0);
        DeployAdmin admin = (DeployAdmin) monitor.getAdmin();

        File opstring = new File("src/int-test/resources/opstring/fixed.groovy");
        File opstring2 = new File("src/int-test/resources/opstring/fixed2.groovy");

        OperationalString[] operationalStrings = new OpStringLoader().parseOperationalString(opstring);
        OperationalString[] operationalStrings2 = new OpStringLoader().parseOperationalString(opstring2);
        CyclicBarrier gate = new CyclicBarrier(101);
        CyclicBarrier gate2 = new CyclicBarrier(101);

        for(int i=0; i<100; i++)
            new Thread(new Deployer(admin, operationalStrings[0], gate)).start();

        gate.await();
        OperationalStringManager mgr = null;
        while(mgr==null) {
            if(admin.getOperationalStringManagers().length>0)
                mgr = admin.getOperationalStringManagers()[0];
            Thread.sleep(1000);
        }

        for(int i=0; i<100; i++)
            new Thread(new Deployer(admin, operationalStrings2[0], gate2)).start();
        gate2.await();

        testManager.waitForDeployment(mgr);
        ServiceElement serviceElement = mgr.getOperationalString().getServices()[0];
        for(int i=0; i< 50; i++) {
            ServiceBeanInstance[] instances = mgr.getServiceBeanInstances(serviceElement);
            System.out.println("Have " + instances.length + " services");
            Thread.sleep(500);
        }

        testManager.undeployAll(monitor);
    }

    class Deployer implements Runnable {
        DeployAdmin admin;
        OperationalString opString;
        CyclicBarrier gate;

        Deployer(DeployAdmin admin, OperationalString opString, CyclicBarrier gate) {
            this.admin = admin;
            this.opString = opString;
            this.gate = gate;
        }

        @Override public void run() {
            try {
                gate.await();
                admin.deploy(opString);
            } catch (OperationalStringException | RemoteException | InterruptedException | BrokenBarrierException e) {
                e.printStackTrace();
            }
        }
    }
}
