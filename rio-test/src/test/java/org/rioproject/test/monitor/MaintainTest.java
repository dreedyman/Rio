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
import net.jini.lookup.ServiceDiscoveryManager;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.rioproject.core.OperationalString;
import org.rioproject.core.ServiceElement;
import org.rioproject.cybernode.Cybernode;
import org.rioproject.monitor.ProvisionMonitor;
import org.rioproject.opstring.OpString;
import org.rioproject.opstring.OpStringLoader;
import org.rioproject.test.RioTestRunner;
import org.rioproject.test.ServiceMonitor;
import org.rioproject.test.SetTestManager;
import org.rioproject.test.TestManager;
import org.rioproject.test.simple.Simple;
import org.rioproject.test.utils.CybernodeUtils;
import org.rioproject.test.watch.ArrayUtils;

import java.io.File;
import java.rmi.RemoteException;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * The class tests the <code>Maintain</code> element.
 */
@RunWith (RioTestRunner.class)
public class MaintainTest {
    static Logger logger = Logger.getLogger("org.rioproject.test.monitor");
    @SetTestManager
    static TestManager testManager;
    static ServiceMonitor cyberMon;

    /**
     * The previous <code>maintain</code> value successfully tested.
     */
    private int prevMaintainInt = 0;
    /**
     * The previous <code>fixed</code> value successfully tested.
     */
    private boolean prevFixed = false;


    @Before
    public void setup() throws Exception {
        if(cyberMon==null) {
            //testManager.startReggie();
            //testManager.startProvisionMonitor();
            int cybernodeCount = Math.max(testManager.getHostList().size(), 2);
            for (int i = 0; i < cybernodeCount; i++) {
                testManager.startCybernode(i);
            }
            ServiceDiscoveryManager sdm = testManager.getServiceDiscoveryManager();
            cyberMon = new ServiceMonitor(sdm, Cybernode.class);
            cyberMon.waitFor(cybernodeCount);
        }
    }

    @Test
    public void runTest() throws Exception {
        ServiceDiscoveryManager sdm = testManager.getServiceDiscoveryManager();
        ServiceMonitor pmMon = new ServiceMonitor(sdm, ProvisionMonitor.class);
        pmMon.waitFor(1);
        ProvisionMonitor monitor = (ProvisionMonitor) pmMon.getServices().get(0);

        final boolean[] fixedValues = new boolean[] {false, true};
        final int[] maintainValues = new int[]
            {-5, -2, -1, 0, 1, 2, 5, 2, 1, 0, -1, -2, -5, 10};

        for (boolean fixedValue : fixedValues) {
            for (int maintainValue : maintainValues) {
                testCombination(fixedValue, maintainValue, monitor, cyberMon);
            }
        }

        testManager.undeployAll(monitor);
    }

    /*
     * Tests a combination.
     */
    private void testCombination(boolean fixed,
                                 int maintain,
                                 ProvisionMonitor monitor,
                                 ServiceMonitor cyberMon)
            throws Exception {

        logger.log(Level.INFO, "Test combination:"
                + " [fixed=" + fixed + ", maintain=" + maintain + "]");

        testManager.undeployAll(monitor);

        prevMaintainInt = 0;

        // Prepare OpString
        OpStringLoader loader = new OpStringLoader();
        OperationalString[] opstrings =
            loader.parseOperationalString(
                new File("src/test/resources/simple_opstring.groovy"));
        org.junit.Assert.assertEquals(1, opstrings.length);
        OpString opstring = (OpString)opstrings[0];
        org.junit.Assert.assertEquals(1, opstring.getServices().length);
        ServiceElement service = opstring.getServices()[0];
        service.setProvisionType(fixed?
                                 ServiceElement.ProvisionType.FIXED:
                                 ServiceElement.ProvisionType.DYNAMIC);
        if (maintain < 0) {
            try {
                service.setPlanned(maintain);
                Assert.fail("IllegalArgumentException expected"
                            + " but not thrown");
            } catch (IllegalArgumentException e) {
            }

            checkState(prevFixed, prevMaintainInt, cyberMon.getServices());
        } else {
            service.setPlanned(maintain);
            testManager.deploy(opstring, monitor);
            checkState(fixed, maintain, cyberMon.getServices());
            prevFixed = fixed;
            prevMaintainInt = maintain;
        }
    }

    /*
     * Checks that the current state of the system is as expected.
     */
    private void checkState(boolean fixed,
                            int maintain,
                            final List<Cybernode> cybernodes) throws Exception {

        final boolean dynamic = !fixed;
        final int maintainF = Math.max(maintain, 0);

        // Setup the condition to wait for
        Condition condition = new Condition() {
            public boolean test() {
                int[] counts;
                try {
                    counts = CybernodeUtils.calcServices(cybernodes,
                                                         Simple.class);
                } catch (RemoteException e) {
                    logger.log(Level.SEVERE, "Error calculating services", e);
                    return false;
                }
                logger.log(Level.INFO, "Services found: {0}",
                           Arrays.asList(ArrayUtils.asObjects(counts)));

                if (dynamic) {
                    int sum = ArrayUtils.sum(counts);
                    return sum == maintainF;
                } else {
                    for (int count : counts) {
                        if (count != maintainF) {
                            return false;
                        }
                    }
                    return true;
                }
            }

            public String toString() {
                String pattern = dynamic
                        ? "# of services {0} {1}"
                        : "# of services on each Cybernode {0} {1}";
                Object[] params = new Object[]{"==" , maintainF};
                return MessageFormat.format(pattern, params);
            }
        };

        Waiter waiter = new Waiter();
        waiter.waitFor(condition);
    }
}
