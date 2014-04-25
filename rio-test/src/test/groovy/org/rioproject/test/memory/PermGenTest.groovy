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
package org.rioproject.test.memory

import org.junit.Assume
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith
import org.rioproject.util.TimeUtil;
import org.rioproject.test.RioTestRunner;
import org.rioproject.test.SetTestManager;
import org.rioproject.test.TestManager;
import static org.junit.Assert.*

import org.rioproject.opstring.OperationalStringManager
import org.rioproject.servicecore.Service
import org.rioproject.cybernode.Cybernode
import org.rioproject.watch.WatchDataSource
import net.jini.core.lookup.ServiceItem

import net.jini.lookup.entry.jmx.JMXProperty
import net.jini.core.entry.Entry
import javax.management.remote.JMXServiceURL
import javax.management.remote.JMXConnectorFactory
import java.lang.management.ManagementFactory

/**
 * Test Perm-Gen behavior
 */
@RunWith(RioTestRunner.class)
class PermGenTest {
    @SetTestManager
    static TestManager testManager;

    @Before
    public void checkBeforeJava1dot8() {
        String sVersion = System.getProperty("java.version");
        String[] parts = sVersion.split("\\.");
        Double version = Double.parseDouble(String.format("%s.%s", parts[0], parts[1]));
        Assume.assumeTrue(version<1.8);
    }

    @Test
    public void deployThenUndeployAndCheckPermGen() {
        assertNotNull(testManager)
        ServiceItem[] items = testManager.getServiceItems(Cybernode.class)
        assertTrue(items.length==1)
        String jmxConnection = null
        for(Entry e : items[0].attributeSets) {
            if(e instanceof JMXProperty) {
                jmxConnection = ((JMXProperty)e).value
                break
            }
        }
        def server = JMXConnectorFactory.connect(new JMXServiceURL(jmxConnection)).MBeanServerConnection
        def memoryMXBean = new GroovyMBean(server, ManagementFactory.MEMORY_MXBEAN_NAME)

        Cybernode cybernode = items[0].service as Cybernode
        WatchDataSource permGen = null
        for(WatchDataSource w : cybernode.fetch()) {
            if(w.ID.contains("Perm Gen")) {
                permGen = w
                break
            }
        }
        assertNotNull(permGen)

        for(int i=0; i<5; i++) {
            Thread.sleep(1000)
            println "${getPermGen(permGen)}"
        }

        log("DEPLOY")
        OperationalStringManager mgr = testManager.deploy()
        assertNotNull(mgr)
        def service = testManager.waitForService(Service.class, "Simple Simon")
        assertNotNull(service)

        OperationalStringManager mgr2 = testManager.deploy("org.rioproject.test:instrumentation-accessor:2.0")

        def instrumentor = testManager.waitForService(Service.class, "Instrumentation Accessor")
        assertNotNull(instrumentor)

        Thread.sleep(5000)

        log("UNDEPLOY ${mgr.operationalString.name}")
        testManager.undeploy(mgr.operationalString.name)

        boolean stillLoaded = checkStillLoaded("org.rioproject.test.simple.Simple",
                                               "org.rioproject.test.simple.SimpleImpl",
                                               memoryMXBean,
                                               permGen,
                                               instrumentor)

        log("UNDEPLOY ${mgr2.operationalString.name}")
        testManager.undeploy(mgr2.operationalString.name)

        assertFalse(stillLoaded)

        /*for(int i=0; i<420; i++) {
            if(i%30==0)
                println "${String.format('%-5s', "[${i}]")} ${getPermGen(permGen)}"
            Thread.sleep(1000)
        }*/
    }

    private boolean checkStillLoaded(String api, String impl, def memoryMXBean, WatchDataSource permGen, def instrumentor) {
        long start = System.currentTimeMillis()
        int iteration = 0
        boolean apiStillLoaded = true
        boolean implStillLoaded = true
        while((apiStillLoaded || implStillLoaded) && iteration < 600) {
            if(iteration%30==0) {
                memoryMXBean.gc()
                if(apiStillLoaded) {
                    print "${String.format('%-5s', "[${iteration}]")} Check if ${api} is loaded ..."
                    if(!instrumentor.isClassNameLoaded(api)) {
                        apiStillLoaded = false
                        println " unloaded, ${getPermGen(permGen)}"
                    } else
                        println " still loaded, ${getPermGen(permGen)}"
                }
                if(!apiStillLoaded) {
                    if(implStillLoaded && !instrumentor.isClassNameLoaded(impl)) {
                        print "${String.format('%-5s', "[${iteration}]")} Check if ${impl} is loaded ..."
                        implStillLoaded = false
                        println " unloaded, ${getPermGen(permGen)}"
                    } else {
                        println " still loaded, ${getPermGen(permGen)}"
                    }
                }
            }
            Thread.sleep(1000)
            iteration++
        }
        println "Time to unload classes: ${TimeUtil.format(System.currentTimeMillis()-start)}"
        println "apiStillLoaded: ${apiStillLoaded}, implStillLoaded: ${implStillLoaded} "+(apiStillLoaded && implStillLoaded)
        return apiStillLoaded && implStillLoaded
    }

    private static void log(String s) {
        println "------------------------------------------------------------------------"
        println "$s "
        println "------------------------------------------------------------------------"
    }

    private String getPermGen(WatchDataSource w) {
        return "perm gen utilization: ${String.format('%5.4f', w.lastCalculable.value)}"
    }
}
