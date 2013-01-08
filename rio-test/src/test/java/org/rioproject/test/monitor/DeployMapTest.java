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

import junit.framework.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.rioproject.cybernode.Cybernode;
import org.rioproject.deploy.DeployedService;
import org.rioproject.deploy.DeploymentMap;
import org.rioproject.opstring.OperationalStringManager;
import org.rioproject.opstring.ServiceElement;
import org.rioproject.test.RioTestRunner;
import org.rioproject.test.SetTestManager;
import org.rioproject.test.TestManager;

import java.rmi.RemoteException;
import java.util.List;

/**
 * Test deploy Map reflects attributes from Cybernode correctly
 */
@RunWith (RioTestRunner.class)
public class DeployMapTest {
    @SetTestManager
    private TestManager testManager;

    @Test
    public void testDeployMap1() throws RemoteException {
        Assert.assertNotNull(testManager);
        Cybernode cybernode = (Cybernode)testManager.waitForService(Cybernode.class);
        Assert.assertNotNull(cybernode);
        OperationalStringManager mgr = testManager.getOperationalStringManager();
        testManager.waitForDeployment(mgr);

        DeploymentMap dMap = mgr.getDeploymentMap();
        Assert.assertEquals(1, dMap.getServiceElements().length);
        ServiceElement elem = dMap.getServiceElements()[0];
        List<DeployedService> list = dMap.getDeployedServices(elem);
        Assert.assertEquals(1, list.size());
        DeployedService deployed =  list.get(0);
        Assert.assertNotNull(deployed);

        testManager.undeploy(mgr.getOperationalString().getName());

    }

}
