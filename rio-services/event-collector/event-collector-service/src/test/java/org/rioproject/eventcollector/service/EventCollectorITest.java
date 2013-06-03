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
package org.rioproject.eventcollector.service;

import net.jini.core.event.RemoteEventListener;
import net.jini.core.lease.Lease;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.rioproject.eventcollector.api.EventCollector;
import org.rioproject.eventcollector.api.EventCollectorRegistration;
import org.rioproject.resources.util.FileUtils;
import org.rioproject.test.RioTestRunner;
import org.rioproject.test.SetTestManager;
import org.rioproject.test.TestManager;
import org.rioproject.url.artifact.ArtifactURLStreamHandlerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URL;

/**
 * @author Dennis Reedy
 */
@RunWith(RioTestRunner.class)
public class EventCollectorITest {
    @SetTestManager
    static TestManager testManager;
    private static Logger logger = LoggerFactory.getLogger(EventCollectorITest.class.getName());

    @BeforeClass
    public static void clean() {
        String userDir = System.getProperty("user.dir");
        File eventDir = new File(userDir, "target/events");
        if(eventDir.exists()) {
            FileUtils.remove(eventDir);
        }
    }

    @Test
    public void testEventCollector() throws Exception {
        try {
            URL.setURLStreamHandlerFactory(new ArtifactURLStreamHandlerFactory());
        } catch(Error e) {
            System.out.println("Factory already defined, move along");
        }
        EventCollector eventCollector = (EventCollector)testManager.waitForService(EventCollector.class);
        Assert.assertNotNull(eventCollector);
        EventCollectorRegistration registration1 = eventCollector.register(Lease.ANY);
        EventCollectorRegistration registration2 = eventCollector.register(Lease.ANY);

        BasicEventListener listener1 = new BasicEventListener();
        RemoteEventListener eventListener = listener1.export();
        registration1.enableDelivery(eventListener);

        try {
            logger.info("Deploy ....");
            testManager.deploy(new File("src/test/opstring/outrigger.groovy"));
            logger.info("Wait 3 seconds to get events.... ");
            Thread.sleep(1000*3);

            registration1.disableDelivery();
            Thread.sleep(1000*3);
            testManager.undeploy("Outrigger");

            logger.info("Wait 3 seconds to get events for next client.... ");
            Thread.sleep(1000*3);
            registration1.enableDelivery(eventListener);
            BasicEventListener listener2 = new BasicEventListener();
            registration2.enableDelivery(listener2.export());
            logger.info("Wait 3 seconds to clean up");
            Thread.sleep(1000 * 3);
            log(1, listener1);
            log(2, listener2);
            Assert.assertEquals(listener1.eventCollectionCount(), listener2.eventCollectionCount());
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    private void log(int i, BasicEventListener listener) {
        logger.info("\nListener "+i+" reports: "+listener.eventCollectionCount()+"\n"+
                       "==================\n"+listener.printEventCollection());

    }
}
