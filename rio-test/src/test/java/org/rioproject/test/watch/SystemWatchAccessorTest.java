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
package org.rioproject.test.watch;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.rioproject.system.SystemWatchID;
import org.rioproject.test.RioTestRunner;
import org.rioproject.test.SetTestManager;
import org.rioproject.test.TestManager;
import org.rioproject.test.scaling.SettableLoadService;
import org.rioproject.watch.WatchDataSource;

import java.rmi.RemoteException;

/**
 * Test getting service and system watches.
 */
@RunWith (RioTestRunner.class)
public class SystemWatchAccessorTest {
    @SetTestManager
    static TestManager testManager;

    @Test
    public void testGetWatches() {
        Assert.assertNotNull(testManager);
        SettableLoadService service = (SettableLoadService)testManager.waitForService(SettableLoadService.class);
        Assert.assertNotNull(service);
        Throwable thrown = null;
        try {            
            WatchDataSource systemCPU = service.fetch(SystemWatchID.SYSTEM_CPU);
            Assert.assertNotNull(systemCPU);
            WatchDataSource load = service.fetch("load");
            Assert.assertNotNull(load);
        } catch (RemoteException e) {
            thrown = e;
            e.printStackTrace();
        }
        Assert.assertNull(thrown);
    }
}
