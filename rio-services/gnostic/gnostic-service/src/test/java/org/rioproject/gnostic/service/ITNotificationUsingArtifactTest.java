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
package org.rioproject.gnostic.service;

import org.junit.*;
import org.junit.runner.RunWith;
import org.rioproject.gnostic.Gnostic;
import org.rioproject.opstring.OperationalStringManager;
import org.rioproject.gnostic.service.test.TestService;
import org.rioproject.test.RioTestRunner;
import org.rioproject.test.SetTestManager;
import org.rioproject.test.TestManager;

import java.io.*;

/**
 *
 */
@RunWith (RioTestRunner.class)
public class ITNotificationUsingArtifactTest {
    @SetTestManager
    static TestManager testManager;
    static String NAME = "test-1.0";

    @BeforeClass
    public static void createArtifact() {
        Assume.assumeTrue(Util.getJVMVersion() < 1.8);

        Throwable thrown = null;
        try {
            writeRule(5, 10, "target/test-classes/EmbeddedCounterNotification.drl");
        } catch (IOException e) {
            e.printStackTrace();
            thrown = e;
        }
        Assert.assertNull(thrown);
        Util.createJar(NAME);
        Util.writePom(NAME);
    }

    @AfterClass
    public static void clean() {
        //removeInstallation();
    }

    @Test
    public void runTests() {
        verifyChangingFileBasedRuleWorks();
        verifyChangingRuleLoadedFromClassPathWorks();
    }

    public void verifyChangingFileBasedRuleWorks() {
        Assert.assertNotNull(testManager);

        Throwable thrown = null;
        try {
            writeRule(5, 20, "src/test/resources/CounterNotification.drl");
        } catch (IOException e) {
            e.printStackTrace();
            thrown = e;
        }
        Assert.assertNull(thrown);

        File opstring = new File("src/test/opstring/artifactNotification.groovy");
        Assert.assertTrue(opstring.exists());
        testManager.deploy(opstring);
        Gnostic g = (Gnostic)testManager.waitForService(Gnostic.class);

        try {
            g.setScannerInterval(5);
            Assert.assertEquals("Scanner Interval expected to be 5 seconds", 5, g.getScannerInterval());
        } catch (Throwable e) {
            e.printStackTrace();
            thrown = e;
        }
        Assert.assertNull(thrown);
        TestService test = (TestService)testManager.waitForService("Test");
        Util.waitForRule(g, "CounterNotification");
        
        thrown = null;
        boolean written = false;
        for(int i=0; i<50; i++) {
            try {
                test.sendNotify();
                Util.sleep(1000);
                if(i>15 && !written) {
                    writeRule(25, 45, "src/test/resources/CounterNotification.drl");
                    written = true;
                }

            } catch (Exception e) {
                e.printStackTrace();
                thrown = e;
            }
            Assert.assertNull(thrown);
        }

        verifyCounts(test, 33, 50);
        testManager.undeploy("Notification Using Artifact Test");
        /* wait for resources to be cleaned up*/
        sleep(8000);
    }

    public void verifyChangingRuleLoadedFromClassPathWorks() {
        Throwable thrown;
        Assert.assertNotNull(testManager);
        File opstring = new File("src/test/opstring/artifactNotification2.groovy");
        Assert.assertTrue(opstring.exists());
        OperationalStringManager mgr = testManager.deploy(opstring);
        testManager.waitForDeployment(mgr);
        TestService test = (TestService)testManager.waitForService("Test");
        thrown = null;
        Gnostic g = (Gnostic)testManager.waitForService(Gnostic.class);
        Util.waitForRule(g, "CounterNotification");
        for(int i=0; i<15; i++) {
            try {
                test.sendNotify();
                Util.sleep(1000);
            } catch (Exception e) {
                e.printStackTrace();
                thrown = e;
            }
            Assert.assertNull(thrown);
        }
        verifyCounts(test, 4, 15);
    }

    private void verifyCounts(TestService test, int rhs, int notifies) {
        Throwable thrown = null;
        try {
            int rhsExecutedCount = test.getRHSExecutedCount();
            int notificationCount = test.getNotificationCount();
            Assert.assertTrue("Expected RHS to be executed <="+rhs+" times", rhsExecutedCount<=rhs);
            Assert.assertEquals("Expected to be notified "+notifies+" times", notifies, notificationCount);
        } catch (IOException e) {
            thrown = e;
        }
        Assert.assertNull(thrown);
    }

    private static void writeRule(int count1, int count2, String name) throws IOException {
        System.out.println("Write rule: "+name+" with lower bound: "+count1+" and upper bound: "+count2);
        File ruleFile = new File(System.getProperty("user.dir"), name);
        Writer output = new BufferedWriter(new FileWriter(ruleFile));
        output.write(getRulesContent(count1, count2));
        output.close();
    }

    private static String getRulesContent(int count1, int count2) {
        StringBuilder sb = new StringBuilder();
        sb.append("package org.rioproject.gnostic;").append("\n");
        sb.append("import org.rioproject.watch.Calculable;").append("\n");
        sb.append("import org.rioproject.gnostic.service.test.TestService;").append("\n");
        sb.append("global org.rioproject.gnostic.service.DeployedServiceContext context;").append("\n");
        sb.append("declare Calculable").append("\n");
        sb.append("    @role(event)").append("\n");
        sb.append("    @timestamp(date)").append("\n");
        sb.append("end").append("\n");
        sb.append("rule \"Counter Notification Rule\"").append("\n");
        sb.append("when").append("\n");
        sb.append("$count : Calculable(id == \"notification\", value > ").append(count1).append(" && < ").append(count2).append(") from entry-point \"calculables-stream\"").append("\n");
        sb.append("then").append("\n");
        sb.append("    System.out.println(\"===> Test has been notified : \"+$count);").append("\n");
        sb.append("    TestService t = context.getService(\"Test\", TestService.class);").append("\n");
        sb.append("    t.executedRHS();").append("\n");
        sb.append("end").append("\n");
        return sb.toString();
    }

    private void sleep(int time) {
        try {
            Thread.sleep(time);
        } catch (InterruptedException e) {
            //
        }
    }
}
