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
 * <h1>Scenario B</h1>
 * In this scenario the federation of Provision Monitors changes more
 * dramatically. The test starts one Provision Monitor, deploys the
 * opstring to it, then starts another Provision Monitor, stops
 * the first one, starts another one, stops the previous one, and
 * so on <code>N</code> times, having no more than two and no less
 * than one Provision Monitor at any moment of time, and verifying
 * on each step that the opstring is still enforced.
 */
@RunWith (RioTestRunner.class)
@RioTestConfig (
        groups = "BackupTest",
        autoDeploy = false
)
public class BackupTestB {
    private static final Logger logger = LoggerFactory.getLogger(BackupTestB.class.getPackage().getName());
    @SetTestManager
    TestManager testManager;

    /**
     * Runs scenario B.
     *
     * @throws Exception if the test fails
     */
    @Test
    public void scenarioB() throws Exception {
        testManager.cleanLogs();
        logger.info("\n*******************\nRunning scenario B\n*******************");
        Assert.assertNotNull(testManager);
        testManager.startReggie();
        
        // 0. N=max{4,number of test hosts}
        // If there are more than 4 hosts participating in the test, we
        // want to have as many iterations as hosts. Otherwise we want
        // to have 4 iterations in order to have meaningful test.
        final int N = Math.max(4, testManager.getHostList().size());

        ServiceDiscoveryManager sdm = testManager.getServiceDiscoveryManager();

        // 1. START PROVISION MONITOR
        logger.info("Starting Provision Monitor ...");
        //ProvisionMonitor pm = testManager.startProvisionMonitor(-1);
        testManager.startProvisionMonitor(-1);
        logger.info("Provision Monitor has been started");
        ServiceMonitor<ProvisionMonitor> pmMon = new ServiceMonitor<>(sdm, ProvisionMonitor.class);
        
        // 2. START CYBERNODE
        logger.info("Starting Cybernode ...");
        //Cybernode cybernode = testManager.startCybernode(-1);
        testManager.startCybernode(-1);
        logger.info("Cybernode has been started");        
        ServiceMonitor<Cybernode> cyMon = new ServiceMonitor<>(sdm, Cybernode.class);

        pmMon.waitFor(1);
        cyMon.waitFor(1);

        List<Cybernode> cybernodes = cyMon.getServices();
        List<ProvisionMonitor> pms = pmMon.getServices();

        Cybernode cybernode = cybernodes.get(0);
        ProvisionMonitor pm = pms.get(0);
        
        // 3. DEPLOY OPSTRING
        testManager.deploy(new File("src/int-test/resources/opstring/simple_opstring.groovy"));
        
        // 4. ASSERTION: ONE SERVICE INSTANCE SHOULD APPEAR
        ServiceMonitor<Simple> simpleMon = new ServiceMonitor<>(sdm, Simple.class);
        simpleMon.waitFor(1);
        
        // 5. DO THE MAIN CYCLE
        for (int i = 0; i < N; i++) {

            // 6. START THE NEXT PROVISION MONITOR
            logger.info("---> Starting the next Provision Monitor ...");
            //ProvisionMonitor nextPM = testManager.startProvisionMonitor(-1);
            testManager.startProvisionMonitor(-1);
            logger.info("The next Provision Monitor has been started");
            
            // 7. START THE NEXT CYBERNODE
            logger.info("---> Starting the next Cybernode ...");
            //Cybernode nextCybernode = testManager.startCybernode(-1);
            testManager.startCybernode(-1);
            logger.info("---> The next Cybernode has been started");
            
            cyMon.waitFor(2);            
            pmMon.waitFor(2);

            // 8. GIVE PROVISION MONITORS SOME TIME TO DISCOVER EACH OTHER
            Thread.sleep(2000);
            
            // 9. STOP THE PREVIOUS PROVISION MONITOR
            logger.info("Stopping the previous Provision Monitor ...");
            TestManager.stopProvisionMonitor(pm);
            logger.info("The previous Provision Monitor has been stopped");
            
            // 10. STOP THE PREVIOUS CYBERNODE
            logger.info("Stopping the previous Cybernode ...");
            TestManager.stopCybernode(cybernode);
            logger.info("The previous Cybernode has been stopped");
            
            pmMon.waitFor(1);
            cyMon.waitFor(1);
            
            // 11. ASSERTION: THE SERVICE SHOULD BE PROVISIONED ONTO ANOTHER
            // CYBERNODE
            simpleMon.waitFor(1);

            //cybernode = nextCybernode;
            //pm = nextPM;
            cybernodes = cyMon.getServices();
            pms = pmMon.getServices();

            cybernode = cybernodes.get(0);
            pm = pms.get(0);
        }
        
        // 12. START ADDITIONAL CYBERNODE
        logger.info("Starting additional Cybernode ...");
        testManager.startCybernode(-1);
        logger.info("Additional Cybernode has been started");
        
        // 13. STOP THE LAST PROVISION MONITOR
        logger.info("Stopping the last Provision Monitor ...");
        TestManager.stopProvisionMonitor(pm);
        logger.info("The last Provision Monitor has been stopped");
        
        cyMon.waitFor(2);
        pmMon.waitFor(0);
        
        // 14. STOP THE CYBERNODE RUNNING THE SERVICE
        logger.info("Stopping the busy Cybernode ...");
        TestManager.stopCybernode(cybernode);
        logger.info("The busy Cybernode has been stopped");
        
        // 15. ASSERTION: THE SERVICE SHOULD NOT APPEAR
        // (BECAUSE THERE ARE NO MORE PROVISION MONITORS)
        simpleMon.waitFor(0);

        for(Cybernode c : cyMon.getServices()) {
            TestManager.stopCybernode(c);
        }
        cyMon.waitFor(0);

        logger.info("\n*******************\nScenario B completed\n*******************");
    }

}
