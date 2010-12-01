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
package org.rioproject.examples.springbean;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.rioproject.cybernode.StaticCybernode;

import java.io.File;
import java.rmi.RemoteException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

/**
 * Example testing the SpringBean service
 */
@RunWith(Parameterized.class)
public class ITSpringBeanTest {
    String opstring;
    Hello springBean;

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        String opstring = System.getProperty("opstring");
        Assert.assertNotNull("no opstring given", opstring);
        return Arrays.asList(new Object[][] {{ opstring }});
    }

    public ITSpringBeanTest(String opstring) {
        this.opstring = opstring;
    }

    @Before
    public void setupSpringBean() throws Exception {
        StaticCybernode cybernode = new StaticCybernode();
        Map<String, Object> map = cybernode.activate(new File(opstring));
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String beanName = entry.getKey();
            Object beanImpl = entry.getValue();
            if (beanName.equals("Hello"))
                springBean = (Hello) beanImpl;
        }
    }

    @Test
    public void testBean() throws RemoteException {
        Assert.assertNotNull(springBean);
        for(int i=1; i<10; i++) {
            String result = springBean.hello("Test Client");
            Assert.assertEquals("Hello visitor : "+i, result);
        }
    }    
}
