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
package org.rioproject.test.deploy;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.rioproject.cybernode.Cybernode;
import org.rioproject.deploy.ServiceBeanInstance;
import org.rioproject.exec.ExecDescriptor;
import org.rioproject.exec.VirtualMachineHelper;
import org.rioproject.jmx.JMXConnectionUtil;
import org.rioproject.jmx.JMXUtil;
import org.rioproject.opstring.OperationalString;
import org.rioproject.opstring.OperationalStringManager;
import org.rioproject.opstring.ServiceElement;
import org.rioproject.test.RioTestRunner;
import org.rioproject.test.SetTestManager;
import org.rioproject.test.TestManager;
import org.rioproject.test.simple.Fork;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.MBeanServerConnection;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

/**
 * Tests simple deploy scenario that forks a service
 */
@RunWith (RioTestRunner.class)
public class SimpleForkTest {
    @SetTestManager
    static TestManager testManager;
    Cybernode cybernode;
    static Logger logger = LoggerFactory.getLogger(RioTestRunner.class.getName());

    @Before
    public void setup() {
        cybernode = (Cybernode)testManager.waitForService(Cybernode.class);
    }

    @Test
    public void testFork() {
        Assert.assertNotNull(testManager);
        Assert.assertNotNull(cybernode);
        Throwable thrown = null;
        try {
            OperationalStringManager mgr = testManager.getOperationalStringManager();
            Assert.assertNotNull("Expected non-null OperationalStringManager", mgr);
            OperationalString opstring = mgr.getOperationalString();
            Assert.assertNotNull(opstring);
            Assert.assertEquals(1, opstring.getServices().length);
            ServiceElement elem = opstring.getServices()[0];
            Assert.assertNotNull("Expected a non-null ExecDescriptor", elem.getExecDescriptor());
            testManager.waitForDeployment(mgr);
            String jvmVersion = System.getProperty("java.version");
            if(jvmVersion.contains("1.5")) {
                logger.info("The JMX Attach APIs require Java 6 or above. You are running Java "+jvmVersion);
            } else {
                RuntimeMXBean runtime = attach("T62___W_FL___SK_-1");
                Assert.assertNotNull("Expected a RuntimeMXBean", runtime);
                verifyJVMArgs(runtime, elem.getExecDescriptor());
            }
            ServiceBeanInstance[] instances = cybernode.getServiceBeanInstances(opstring.getServices()[0]);
            Assert.assertEquals(1, instances.length);
            Fork fork = (Fork)instances[0].getService();
            Assert.assertTrue("Expected verify() to return true, check service log for details", fork.verify());
            //logger.info("Wait for 5 minutes, make sure that only one service remains....");
            //Thread.sleep(TimeUnit.MINUTES.toMillis(5));
            //testManager.undeploy(opstring.getName());
            testManager.stopCybernode(cybernode);
            System.out.println("\nService Bean Exec JVMs\n============");
            String listing = null;
            long t0 = System.currentTimeMillis();
            for(int i=0; i<180; i++) {
                listing = list();
                if(listing.length()==0 || (System.currentTimeMillis()-t0)>15000)
                    break;
                if(i % 5 == 0)
                    System.out.println(listing);
                Thread.sleep(500);
            }
            System.out.println("Time to exit: "+(System.currentTimeMillis()-t0));
            Assert.assertTrue(listing!=null);
            Assert.assertTrue(listing.length()==0);
        } catch(Exception e) {
            thrown = e;
            e.printStackTrace();
        }
        Assert.assertNull("Should not have thrown an exception", thrown);
    }

    private void verifyJVMArgs(RuntimeMXBean runtime, ExecDescriptor exDesc) {
        String[] declaredArgs = toArray(exDesc.getInputArgs());
        List<String> jvmArgs = runtime.getInputArguments();
        logger.info("Runtime JVM Args ["+flatten(jvmArgs)+"]");
        for(String arg : declaredArgs) {
            boolean matched = false;
            for(String jvmArg : jvmArgs) {
                if(arg.equals(jvmArg)) {
                    matched = true;
                    break;
                }
            }
            Assert.assertTrue("Expected to match ["+arg+"]", matched);
        }
    }

    private RuntimeMXBean attach(final String matchName) throws Exception {
        String[] managedVMs = VirtualMachineHelper.listManagedVMs();
        for(String managedVM : managedVMs) {
            if(managedVM.contains("start-service-bean-exec")) {
                String pid = managedVM.substring(0, managedVM.indexOf(" "));
                long forkedPID = Long.valueOf(pid);
                if(forkedPID!=-1) {
                    logger.info("PID of exec'd process obtained: "+forkedPID);
                    try {
                        MBeanServerConnection mbsc = JMXConnectionUtil.attach(Long.toString(forkedPID));
                        logger.info("JMX Attach succeeded to exec'd JVM with pid: "+forkedPID);
                        RuntimeMXBean runtime = JMXUtil.getPlatformMXBeanProxy(mbsc,
                                                                               ManagementFactory.RUNTIME_MXBEAN_NAME,
                                                                               RuntimeMXBean.class);
                        for(Map.Entry<String,String> entry : runtime.getSystemProperties().entrySet()) {
                            if(entry.getKey().equals("org.rioproject.service") && entry.getValue().equals(matchName)) {
                                return runtime;
                            }
                        }
                    } catch(Exception e) {
                        logger.warn("Could not attach to the exec'd JVM with pid: "+forkedPID+", continue service execution",
                                   e);
                    }
                } else {
                    logger.warn("Could not obtain actual pid of exec'd process, " +
                                "process cpu and java memory utilization are not available [{}]", managedVM);
                }
            }
        }
        return null;
    }

    private String list() throws Exception {
        String[] managedVMs = VirtualMachineHelper.listManagedVMs();
        StringBuilder list = new StringBuilder();
        for(String managedVM : managedVMs) {
            if(managedVM.contains("start-service-bean-exec")) {
                if(list.length()>0)
                    list.append("\n");
                list.append("\t").append(managedVM);
            }
        }

        return list.toString();
    }

    private String[] toArray(String s) {
        StringTokenizer tok = new StringTokenizer(s);
        String[] array = new String[tok.countTokens()];
        int i=0;
        while(tok.hasMoreTokens()) {
            array[i] = tok.nextToken();
            i++;
        }
        return(array);
    }

    private String flatten(List<String> l) {
        StringBuilder sb = new StringBuilder();
        for (String s : l) {
            sb.append(s).append(" ");
        }
        return sb.toString();
    }
}
