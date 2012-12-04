/*
 * Copyright 2008 the original author or authors.
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
package org.rioproject.test.harvest

import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.rioproject.cybernode.Cybernode
import org.rioproject.deploy.ServiceBeanInstantiator
import org.rioproject.monitor.ProvisionMonitor
import org.rioproject.test.RioTestRunner
import org.rioproject.test.ServiceMonitor
import org.rioproject.test.SetTestManager
import org.rioproject.test.TestManager
import org.rioproject.tools.harvest.Harvester
import org.rioproject.tools.harvest.HarvesterBean

/**
 * Test Harvester components
 */
@RunWith(RioTestRunner.class )
class HarvesterTest {
    @SetTestManager
    static TestManager testManager

    @Test
    void testHarvester() {
        Assert.assertNotNull(testManager)
        ProvisionMonitor[] monitors = testManager.getServices(ProvisionMonitor.class)
        Assert.assertTrue "Expected at least 1 ProvisionMonitor", monitors.length>0
        Cybernode[] cybernodes = testManager.getServices(Cybernode.class)
        int waited = 0
        while(cybernodes.length < 2 && waited < 10) {
            Thread.sleep(1000)
            waited++
            cybernodes = testManager.getServices(ServiceBeanInstantiator.class)
            println "Waiting for 2 Cybernodes, have [${cybernodes.length} of 2] ..."
        }
        Assert.assertTrue "Expected 2 Cybernodes", cybernodes.length>1
        ProvisionMonitor monitor = monitors[0]
        List<String> hosts = new ArrayList<String>()
        for(ServiceBeanInstantiator sbi : monitor.getServiceBeanInstantiators()) {
            String s = sbi.getInetAddress().toString()
            if(!hosts.contains(s))
                hosts.add(s)
        }
        HarvesterBean h = new HarvesterBean(testManager.serviceDiscoveryManager.discoveryManager)        
        long timeout = 1000*60
        long duration = 0
        while(h.agentsHandledCount<hosts.size()) {
            Thread.sleep(1000)
            duration += 1000
            if(duration >= timeout)
                break;
        }
        Assert.assertTrue("Agents handled should be greater than 0, actual=${h.agentsHandledCount}", h.agentsHandledCount>0)
        ServiceMonitor sMon = new ServiceMonitor(testManager.serviceDiscoveryManager,
                                                 Harvester.class)
        sMon.waitFor(1)
        Assert.assertEquals(1, sMon.count)

        h.unadvertise()

        sMon.waitFor(0)
        Assert.assertEquals(0, sMon.count)
    }


}