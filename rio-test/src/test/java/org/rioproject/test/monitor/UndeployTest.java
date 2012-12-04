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
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.rioproject.cybernode.Cybernode;
import org.rioproject.monitor.ProvisionMonitor;
import org.rioproject.opstring.OpString;
import org.rioproject.opstring.OpStringLoader;
import org.rioproject.opstring.OperationalString;
import org.rioproject.opstring.ServiceElement;
import org.rioproject.test.RioTestRunner;
import org.rioproject.test.ServiceMonitor;
import org.rioproject.test.SetTestManager;
import org.rioproject.test.TestManager;
import org.rioproject.test.simple.Simple;
import org.rioproject.test.utils.ArrayUtils;
import org.rioproject.test.utils.CybernodeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.rmi.RemoteException;
import java.text.MessageFormat;
import java.util.Arrays;

/**
 * The class tests OpString undeployment. The test is incomplete;
 * it just tests one specific sequence of OpStrings that brings
 * the system into a state in which undeployment doesn't work.
 */
@RunWith (RioTestRunner.class)
public class UndeployTest  {
    @SetTestManager
    static TestManager testManager;
    static ServiceMonitor<Cybernode> cyberMon;
    static ServiceMonitor<ProvisionMonitor> pmMon;
    private static Logger logger = LoggerFactory.getLogger("org.rioproject.test.monitor");

    @Before
    public  void setup() throws java.util.concurrent.TimeoutException {
        if(cyberMon==null) {
            testManager.startReggie();
            testManager.startProvisionMonitor();
            final int N = Math.max(testManager.getHostList().size(), 2);
            for (int i = 0; i < N; i++) {
                testManager.startCybernode(i);
            }
            ServiceDiscoveryManager sdm = testManager.getServiceDiscoveryManager();
            cyberMon = new ServiceMonitor<Cybernode>(sdm, Cybernode.class);
            cyberMon.waitFor(N);

            pmMon = new ServiceMonitor<ProvisionMonitor>(sdm, ProvisionMonitor.class);
            pmMon.waitFor(1);
        }
    }

    @Test
    public void testUndeploy() throws Exception {
        ProvisionMonitor monitor = pmMon.getServices().get(0);

        deployOpString(ServiceElement.ProvisionType.FIXED, 10, monitor);
        checkState("fixed", 10, true);

        deployOpString(ServiceElement.ProvisionType.DYNAMIC, 10, monitor);
        checkState("dynamic", 10, false);

        testManager.undeployAll(monitor);
        checkState("dynamic", 0, true);
    }

    /*
     * Deploys the test OpString with the specified parameters.
     */
    private void deployOpString(ServiceElement.ProvisionType provisionType,
                                int maintain,
                                ProvisionMonitor monitor) throws Exception {
        logger.info("Deployment parameters:\n"
                    + "provisionType: {}\n"
                    + "maintain:      {}",
                    provisionType,maintain);

        OpStringLoader loader = new OpStringLoader();
        OperationalString[] opstrings =
            loader.parseOperationalString(
                new File("src/test/resources/opstring/simple_opstring.groovy"));
        Assert.assertEquals(1, opstrings.length);
        OpString opstring = (OpString)opstrings[0];
        Assert.assertEquals(1, opstring.getServices().length);
        ServiceElement service = opstring.getServices()[0];
        service.setPlanned(maintain);
        service.setProvisionType(provisionType);                

        int[] counts = CybernodeUtils.calcServices(cyberMon.getServices(),
                                                   Simple.class);
        logger.info("Services found before deploying the OpString: {}",
                   Arrays.asList(ArrayUtils.asObjects(counts)).toString());

        testManager.deploy(opstring, monitor);
    }

    /*
     * Checks that the current state of the system is as expected.
     */
    private void checkState(String provisionType,
                            final int maintain,
                            final boolean exactMatch) throws Exception {

        final boolean dynamic = !provisionType.equals("fixed");
        
        // Setup the condition to wait for
        Condition condition = new Condition() {
            public boolean test() {
                int[] counts;
                try {
                    counts = CybernodeUtils.calcServices(cyberMon.getServices(),
                                                         Simple.class);
                } catch (RemoteException e) {
                    logger.error("Error calculating services", e);
                    return false;
                }
                logger.info("Services found: {}",Arrays.asList(ArrayUtils.asObjects(counts)).toString());

                if (dynamic) {
                    int sum = ArrayUtils.sum(counts);
                    return exactMatch ? sum == maintain : sum >= maintain;
                } else {
                    for (int count : counts) {
                        if (exactMatch) {
                            if (count != maintain) {
                                return false;
                            }
                        } else {
                            if (count < maintain) {
                                return false;
                            }
                        }
                    }
                    return true;
                }
            }

            public String toString() {
                String pattern = dynamic
                        ? "# of services {0} {1}"
                        : "# of services on each Cybernode {0} {1}";
                Object[] params = new Object[]
                        {exactMatch ? "==" : ">=", maintain};
                return MessageFormat.format(pattern, params);
            }
        };

        Waiter waiter = new Waiter();
        waiter.waitFor(condition);
    }
}
