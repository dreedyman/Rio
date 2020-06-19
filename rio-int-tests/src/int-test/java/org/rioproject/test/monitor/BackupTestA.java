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
import org.rioproject.monitor.ProvisionMonitor;
import org.rioproject.test.*;
import org.rioproject.test.simple.Simple;
import org.rioproject.test.utils.CybernodeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.List;

/**
 * The class tests backup behavior of Provision Monitors.
 * <p>
 * The class tests backup behavior of Provision Monitors which is
 * specified in <em>Rio Specification Bits. Provision Monitors. Backup
 * Behavior</em>. The test basically tests the following specification
 * statement:
 * <p>
 * If an opstring is deployed to one or more Provision Monitors of
 * a federation, then the federation keeps enforcing the opstring
 * as long as there is at least one Provision Monitor remaining
 * in the federation.
 * <p>
 * Since federation is a dynamic set of Provision Monitors and there
 * is an infinite number of scenarios of how a federation can change
 * over time, it is impossible to test all of the scenarios. That is
 * why for this test we pick two scenarios - scenario A and scenario
 * B - which we believe are representative enough.
 *
 * <h1>Scenario A</h1>
 * In this scenario the test starts <code>N</code> (some number of)
 * Provision Monitors at once, deploys the opstring to the first
 * Provision Monitor, and then stops the Provision Monitors one by one,
 * starting from the first one, and verifying that the opstring remains
 * enforced until there are no running Provision Monitors.
 */
@RunWith (RioTestRunner.class)
@RioTestConfig (
        groups = "BackupTest",
        autoDeploy = false
)
public class BackupTestA {
    private static final Logger logger = LoggerFactory.getLogger(BackupTestA.class.getPackage().getName());
    @SetTestManager
    TestManager testManager;

    /**
     * Runs scenario A.
     *
     * @throws Exception if the test fails
     */
    @Test
    public void scenarioA() throws Exception {
        logger.info("\n*******************\nRunning scenario A ...\n*******************");
        Assert.assertNotNull(testManager);
        testManager.startReggie();
        
        // 0. N=max{4,number of test hosts}
        // If there are more than 4 hosts participating in the test, we
        // want to have one Cybernode and one Provision Monitor running
        // on each host. Otherwise we want to have 4 Cybernodes and 4
        // Provision Monitors (started in a round-robin fashion) in order
        // to have meaningful test.
        final int N = Math.max(4, testManager.getHostList().size());
        
        // 1. START N CYBERNODES AND N PROVISION MONITORS IN A ROUND-ROBIN
        // FASHION
        logger.info("Starting [" + N + "] Cybernodes and "
                           + "[" + N + "] Provision Monitors "
                           + "in a round-robin fashion ...");
        for (int i = 0; i < N; i++) {
            testManager.startProvisionMonitor(i);
            testManager.startCybernode(i);
        }
        logger.info("[" + N + "] Cybernodes and "
                  + "[" + N + "] Provision Monitors have been started");

        ServiceDiscoveryManager sdm = testManager.getServiceDiscoveryManager();
        ServiceMonitor<Cybernode> cyMon = new ServiceMonitor<>(sdm, Cybernode.class);
        ServiceMonitor<ProvisionMonitor> pmMon = new ServiceMonitor<>(sdm, ProvisionMonitor.class);
        cyMon.waitFor(N);
        pmMon.waitFor(N);

        List<Cybernode> cybernodes = cyMon.getServices();
        List<ProvisionMonitor> pms = pmMon.getServices();

        Assert.assertEquals("Provision Monitor collection should have 4 entries", 4, pms.size());
        Assert.assertEquals("Cybernode collection should have 4 entries", 4, cybernodes.size());
        for(ProvisionMonitor pm : pms)
            Assert.assertNotNull("Provision Monitor proxy should not be null", pm);

        for(Cybernode cybernode : cybernodes)
            Assert.assertNotNull("Cybernode proxy should not be null", cybernode);

        // 2. DEPLOY OPSTRING
        testManager.deploy(new File("src/int-test/resources/opstring/simple_opstring.groovy"),
                       pms.get(0));
        
        // 3. ASSERTION: ONE SERVICE INSTANCE SHOULD APPEAR
        ServiceMonitor<Simple> simpleMon = new ServiceMonitor<>(sdm, Simple.class);
        simpleMon.waitFor(1);
        
        // 4. DO THE MAIN CYCLE
        for (int i = 0; i < N - 1; i++) {
            try {

                // 5. STOP PROVISION MONTIOR
                logger.info("---> Stopping Provision Monitor [" + i + "] ...");
                ProvisionMonitor pm = pms.get(i);
                testManager.stopProvisionMonitor(pm);
                //pms.remove(pm);
                logger.info("Provision Monitor [" + i + "] has been stopped");

                // 6. STOP CYBERNODE RUNNING THE SERVICE
                logger.info("---> Stopping the busy Cybernode ...");
                Cybernode cybernode = CybernodeUtils.findBusy(cybernodes);
                testManager.stopCybernode(cybernode);
                cybernodes.remove(cybernode);
                logger.info("---> The busy Cybernode has been stopped");

                pmMon.waitFor(N - 1 - i);
                cyMon.waitFor(N - 1 - i);

                // 7. ASSERTION: THE SERVICE SHOULD BE PROVISIONED ONTO ANOTHER
                // CYBERNODE
                simpleMon.waitFor(1);
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }

        // 8. START ADDITIONAL CYBERNODE
        logger.info("Starting additional Cybernode ...");
        cybernodes.add(testManager.startCybernode(0));
        logger.info("Additional Cybernode has been started");
        
        // 9. STOP THE LAST PROVISION MONITOR
        logger.info("Stopping the last Provision Monitor ...");
        testManager.stopService(pms.get(N-1), "Monitor");
        logger.info("The last Provision Monitor has been stopped");
        
        cyMon.waitFor(2);
        pmMon.waitFor(0);
        
        // 10. STOP THE CYBERNODE RUNNING THE SERVICE
        logger.info("Stopping the busy Cybernode ...");
        testManager.stopCybernode(cybernodes.get(0));
        logger.info("The busy Cybernode has been stopped");
        cyMon.waitFor(1);
        
        // 11. ASSERTION: THE SERVICE SHOULD NOT APPEAR
        // (BECAUSE THERE ARE NO MORE PROVISION MONITORS)
        simpleMon.waitFor(0);
        
        // Cleanup
        testManager.stopCybernode(cybernodes.get(1));
        testManager.shutdown();

        logger.info("\n*******************\nScenario A completed\n*******************");
    }

}
