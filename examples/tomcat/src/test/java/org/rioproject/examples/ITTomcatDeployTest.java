/*
 * Copyright 2010 the original author or authors.
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
package org.rioproject.examples;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.rioproject.resources.servicecore.Service;
import org.rioproject.test.RioTestRunner;
import org.rioproject.test.SetTestManager;
import org.rioproject.test.TestManager;
import org.rioproject.watch.WatchDataSource;

/**
 * Testing the Tomcat service using the Rio test framework
 */
@RunWith(RioTestRunner.class)
public class ITTomcatDeployTest {
    @SetTestManager
    static TestManager testManager;
    Service service;

    @Test
    public void verifyTomcatServiceDeployed() {
        Assert.assertNotNull(testManager);
        service = (Service)testManager.waitForService("Tomcat");
        Assert.assertNotNull(service);
        Throwable thrown = null;
        int maxWait = 20;
        int wait = 0;
        long t0 = System.currentTimeMillis();
        boolean watchAttached = false;
        while(wait<maxWait && !watchAttached) {
            try {
                for(WatchDataSource wds : service.fetch()) {
                    if(wds.getID().equals("Tomcat Thread Pool")) {
                        watchAttached = true;
                        break;
                    }
                }
                wait++;
                Thread.sleep(500);
            } catch(Exception e) {
                e.printStackTrace();
                thrown = e;
            }
        }
        long t1 = System.currentTimeMillis();
        System.out.println("Waited ("+(t1-t0)/1000+") seconds for Watch to be available");
        Assert.assertTrue("The \"Tomcat Thread Pool\" watch attach was unsuccessful", watchAttached);
        Assert.assertNull(thrown);
    }

}
