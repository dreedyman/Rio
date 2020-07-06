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
package org.rioproject.examples.workflow;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.rioproject.cybernode.StaticCybernode;

import java.io.File;
import java.net.URL;
import java.rmi.RemoteException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

import static org.junit.Assert.assertNotNull;

public class WorkflowTest {
    private Master master;

    @Before
    public void setup() throws Exception {
        StaticCybernode cybernode = new StaticCybernode();
        String opstring = "opstring/workflow.groovy";
        URL url = WorkflowTest.class.getClassLoader().getResource(opstring);
        assertNotNull(url);
        Map<String, Object> map = cybernode.activate(new File(url.toURI()));
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String beanName = entry.getKey();
            Object beanImpl = entry.getValue();
            if (beanName.equals("Master")) {
                master = (Master) beanImpl;
                System.out.println("Check if Master has been advertised...");
                while(!((MasterImpl)beanImpl).hasBeenAdvertised()) {
                    System.out.println("Wait for Master to be advertised...");
                    Thread.sleep(1000L);
                }
            }
        }
    }

    @Test
    public void testBean() throws RemoteException, WorkflowException {
        Assert.assertNotNull(master);
        System.out.println("Submitting Order ...");
        WorkflowEntry result = master.process();
        System.out.println("Order result is : "+result.value);
    }
}
