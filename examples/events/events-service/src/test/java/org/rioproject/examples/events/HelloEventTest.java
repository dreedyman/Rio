/*
 * Copyright to the original author or authors.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *         http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.rioproject.examples.events;

import org.junit.Before;
import org.junit.Test;
import org.rioproject.cybernode.StaticCybernode;
import org.rioproject.examples.events.service.HelloEventConsumer;

import java.io.File;
import java.net.URL;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Example testing the events example.
 */
public class HelloEventTest {
    String opstring = "opstring/events.groovy";
    Hello eventProducer;
    HelloEventConsumer eventConsumer;

    @Before
    public void setup() throws Exception {
        StaticCybernode cybernode = new StaticCybernode();
        URL url = HelloEventTest.class.getClassLoader().getResource(opstring);
        assertNotNull(url);
        Map<String, Object> map = cybernode.activate(new File(url.toURI()));
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String beanName = entry.getKey();
            Object beanImpl = entry.getValue();
            if (beanName.equals("Hello")) {
                eventProducer = (Hello) beanImpl;
            } else if(beanName.equals("Hello Event Consumer")) {
                eventConsumer = (HelloEventConsumer) beanImpl;
            }
        }
    }

    @Test
    public void testBean() throws Exception {
        assertNotNull(eventProducer);
        eventProducer.sayHello("Bonjour");
        assertEquals(eventConsumer.getNotificationCount(), eventConsumer.getNotificationCount());
    }

}
