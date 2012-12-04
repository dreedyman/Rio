/*
 * Copyright 2009 the original author or authors.
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
package org.rioproject.test.scaling;

import net.jini.lookup.ServiceDiscoveryManager;
import org.junit.*;
import org.junit.runner.RunWith;
import org.rioproject.cybernode.Cybernode;
import org.rioproject.monitor.ProvisionMonitor;
import org.rioproject.test.RioTestRunner;
import org.rioproject.test.ServiceMonitor;
import org.rioproject.test.SetTestManager;
import org.rioproject.test.TestManager;
import org.rioproject.test.utils.CybernodeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

/**
 * The Scaling Service test, verifies that, if an opstring defines a SLA that
 * states that a service must be scaled up/down when a watchable value breaches
 * the upper/lower threshold, then Rio actually scales the services up/down
 * as specified in the opstring.
 * <p>
 * The test makes use of the test service and the opstring defining
 * the test service attributes. The test service is a
 * <code>SettableLoadService</code>, interface: {@link SettableLoadService},
 * implementation: {@link SettableLoadServiceImpl}.
 * <code>SettableLoadService</code> provides a watchable value -
 * <code>load</code> - which has no specific meaning but can can be changed
 * programmatically through the service's interface. The opstring associates
 * a scaling policy handler with the <code>load</code> watchable value.
 * <p>
 * After usual initialization (starting Lookup Service, Provision Monitor,
 * and Cybernodes and deploying the opstring), the test starts increasing
 * the total load and verifying that Rio scales the service up as expected.
 * The test distributes the total load among available test service
 * instances using the {@link LoadDistributor} utility object. The test
 * increases the total load gradually, expecting the appearance of one
 * service instance on each step. The service's <code>MaxPerMachine</code>
 * parameter is 1, so each new service instance appears on a new Cybernode.
 * <p>
 * The test stops increasing the total load when only one Cybernode
 * remains unoccupied. After that the test stops one of the busy
 * Cybernodes and checks that Rio increments the service to compensate
 * the service disappearance.
 * <p>
 * Then the test goes in the opposite direction. It decreases the total
 * load gradually and verifies that Rio scales the service down as expected.
 * <p>
 * The test starts one Cybernode on each test host. However, if there are
 * less than 4 test hosts, the test starts 4 Cybernodes in a round-robin
 * fashion.
 */
@RunWith (RioTestRunner.class)
public class ScalingServiceTest {
    static Logger logger = LoggerFactory.getLogger("org.rioproject.test.scaling");
    @SetTestManager
    static TestManager testManager;
    ServiceMonitor<Cybernode> cyberMon;

    @BeforeClass
    public static void init() throws Exception {
        testManager.startReggie();
        testManager.startProvisionMonitor();

        ServiceDiscoveryManager sdm = testManager.getServiceDiscoveryManager();
        ServiceMonitor<ProvisionMonitor> pmMon = new ServiceMonitor<ProvisionMonitor>(sdm, ProvisionMonitor.class);
        pmMon.waitFor(1);
    }

    @Before
    public void setup() throws Exception {        
        // 1. START CYBERNODES IN A ROUND-ROBIN FASHION
        // If there are more than 4 hosts participating in the test, we want
        // to have one Cybernode running on each host. Otherwise we want to
        // have 4 Cybernodes in total (in order to have meaningful test).
        final int N = Math.max(testManager.getHostList().size(), 4);
        logger.info("");
        logger.info("Starting [" + N + "] Cybernodes"
                    + " in a round-robin fashion ...");
        for (int i = 0; i < N; i++) {
            testManager.startCybernode(i);
        }
        logger.info("[" + N + "] Cybernodes have been started");
        if(cyberMon==null) {
            ServiceDiscoveryManager sdm = testManager.getServiceDiscoveryManager();
            cyberMon = new ServiceMonitor<Cybernode>(sdm, Cybernode.class);
        }
        cyberMon.waitFor(N);
    }

    @After
    public void cleanup() {
        Assert.assertNotNull(cyberMon);
        Assert.assertNotNull(testManager);
        for(Object cybernode : cyberMon.getServices())
            testManager.stopCybernode(cybernode);
        Assert.assertEquals((long)0, (long)cyberMon.getCount());
        boolean undeployed = testManager.undeploy("Scaling Service Test");
        Assert.assertTrue(undeployed);
    }

    @Test
    public void runTest() throws Exception {
        logBanner("Running Test #1");
        Assert.assertNotNull(testManager);

        // 2. DEPLOY OPSTRING
        testManager.deploy(new File("src/test/resources/opstring/scaling_service_test.groovy"));

        // 3. ASSERTION: ONE SERVICE INSTANCE SHOULD APPEAR
        ServiceDiscoveryManager sdm = testManager.getServiceDiscoveryManager();
        ServiceMonitor<SettableLoadService> servMon = new ServiceMonitor<SettableLoadService>(sdm, SettableLoadService.class);
        servMon.waitFor(1);
        Assert.assertEquals((long)1, (long)servMon.getCount());

        // 4. GRADUALLY INCREASE LOAD. THE SERVICE SHOULD SCALE UP
        // The idea is to scale the service up until only one Cybernode
        // remains unoccupied                
        logger.info("");
        logger.info("===> Increasing total load ...");
        LoadDistributor loadDistributor = new LoadDistributor(sdm);
        double desiredServiceLoad = 0.5;
        int count = cyberMon.getCount();
        for (int servicesWanted = 2; servicesWanted < count; servicesWanted++) {
            // 5. INCREASE LOAD
            double totalLoad = desiredServiceLoad * servicesWanted;
            logger.info("");
            logger.info("===> New total load: " + totalLoad);
            loadDistributor.setTotalLoad(totalLoad);

            // 6. ASSERTION: ONE MORE SERVICE INSTANCE SHOULD APPEAR
            servMon.waitFor(servicesWanted);
        }

        // 7. STOP BUSY CYBERNODE
        logger.info("");
        count = servMon.getCount();
        logBanner("Stopping a busy Cybernode ...");
        testManager.stopCybernode(CybernodeUtils.findBusy(cyberMon.getServices()));
        logBanner("A busy Cybernode has been stopped");

        // 8. ASSERTION: A SERVICE INSTANCE SHOULD APPEAR ON
        // THE REMAINING CYBERNODE
        //servMon.waitFor(count - 1);
        logBanner("Wait for ["+count+"] services, have ["+servMon.getCount()+"] ...");
        servMon.waitFor(count);

        // 9. GRADUALLY DECREASE LOAD. THE SERVICE SHOULD SCALE DOWN
        logBanner("Stabilized after Cybernode sutdown, decreasing total load ...");
        for (int servicesWanted = count - 1; servicesWanted > 0; servicesWanted--) {
            // 10. DECREASE LOAD
            double totalLoad = desiredServiceLoad * servicesWanted;
            logger.info("");
            logger.info("===> New total load: " + totalLoad);
            loadDistributor.setTotalLoad(totalLoad);

            // 11. ASSERTION: ONE MORE SERVICE INSTANCE SHOULD DISAPPEAR
            servMon.waitFor(servicesWanted);
        }

        logBanner("Test #1 Complete");
    }

    @Test
    public void runTest2() throws Exception {
        logBanner("Running Test #2");
        Assert.assertNotNull(testManager);

        // 2. DEPLOY OPSTRING
        testManager.deploy(new File("src/test/resources/opstring/scaling_service_test.groovy"));

        // 3. ASSERTION: ONE SERVICE INSTANCE SHOULD APPEAR
        ServiceDiscoveryManager sdm = testManager.getServiceDiscoveryManager();
        ServiceMonitor<SettableLoadService> servMon = new ServiceMonitor<SettableLoadService>(sdm, SettableLoadService.class);
        servMon.waitFor(1);
        Assert.assertEquals((long)1, (long)servMon.getCount());

        // 4. GRADUALLY INCREASE LOAD. THE SERVICE SHOULD SCALE UP
        // The idea is to scale the service up until only one Cybernode
        // remains unoccupied
        logger.info("");
        logger.info("===> Increasing total load ...");
        LoadDistributor loadDistributor = new LoadDistributor(sdm);
        double desiredServiceLoad = 0.5;
        int count = cyberMon.getCount();
        for (int servicesWanted = 2; servicesWanted < count; servicesWanted++) {
            // 5. INCREASE LOAD
            double totalLoad = desiredServiceLoad * servicesWanted;
            logger.info("");
            logger.info("===> New total load: " + totalLoad);
            loadDistributor.setTotalLoad(totalLoad, 2);

            // 6. ASSERTION: ONE MORE SERVICE INSTANCE SHOULD APPEAR
            servMon.waitFor(servicesWanted);
        }

        // 7. STOP BUSY CYBERNODE
        logger.info("");
        count = servMon.getCount();
        logBanner("Stopping a busy Cybernode ...");
        testManager.stopCybernode(CybernodeUtils.findBusy(cyberMon.getServices()));
        logBanner("A busy Cybernode has been stopped");

        // 8. ASSERTION: A SERVICE INSTANCE SHOULD APPEAR ON
        // THE REMAINING CYBERNODE
        //servMon.waitFor(count - 1);
        logBanner("Wait for ["+count+"] services, have ["+servMon.getCount()+"] ...");
        servMon.waitFor(count);

        // 9. GRADUALLY DECREASE LOAD. THE SERVICE SHOULD SCALE DOWN
        logBanner("Stabilized after Cybernode sutdown, decreasing total load ...");
        for (int servicesWanted = count - 1; servicesWanted > 0; servicesWanted--) {
            // 10. DECREASE LOAD
            double totalLoad = desiredServiceLoad * servicesWanted;
            logger.info("");
            logger.info("===> New total load: " + totalLoad);
            loadDistributor.setTotalLoad(totalLoad, 4);

            // 11. ASSERTION: ONE MORE SERVICE INSTANCE SHOULD DISAPPEAR
            servMon.waitFor(servicesWanted);
        }

        logBanner("Test #2 Complete");
    }

    private void logBanner(String message) {
        logger.info("\n************************\n"+message+"\n************************");
    }
}
