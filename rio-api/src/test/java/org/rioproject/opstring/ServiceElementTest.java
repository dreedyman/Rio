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
package org.rioproject.opstring;

import junit.framework.Assert;
import org.junit.Test;
import org.rioproject.sla.ServiceLevelAgreements;

import java.util.HashMap;

/**
 * Test ServiceElement
 */
public class ServiceElementTest {
    @Test
    public void testEquality() {
        ServiceBeanConfig sbc = new ServiceBeanConfig(new HashMap<String, Object>(), new String[]{"-"});
        ClassBundle export1 = new ClassBundle("org.rioproject.resources.servicecore.Service");
        export1.addJAR("rio-api.jar");
        export1.addJAR("service-dl.jar");
        export1.setCodebase("http://10.1.1.3:9000");
        ClassBundle impl = new ClassBundle("com.foo.ExampleImpl");
        impl.addJAR("rio.jar");
        impl.addJAR("service.jar");
        impl.setCodebase("http://10.1.1.3:9000");
        String fdh = "org.rioproject.fdh.LeaseFaultDetectionHandler";
        ServiceElement s1 = new ServiceElement(ServiceElement.ProvisionType.DYNAMIC,
                                               sbc,
                                               new ServiceLevelAgreements(),
                                               new ClassBundle[]{export1},
                                               new ClassBundle(fdh),
                                               impl);

        ClassBundle export2 = new ClassBundle("org.rioproject.resources.servicecore.Service");
        export2.addJAR("rio-api.jar");
        export2.addJAR("service-dl.jar");
        export2.setCodebase("http://10.1.1.3:9000");
        ServiceElement s2 = new ServiceElement(ServiceElement.ProvisionType.DYNAMIC,
                                               sbc,
                                               new ServiceLevelAgreements(),
                                               new ClassBundle[]{export2},
                                               new ClassBundle(fdh),
                                               impl);
        Assert.assertTrue("s1 equal s2 ? ", s1.equals(s2));
        Assert.assertTrue("s2 equal s1 ? ", s2.equals(s1));
        Assert.assertTrue("s1 equal s1 ? ", s1.equals(s1));
        Assert.assertTrue("s2 equal s2 ? ", s2.equals(s2));
    }
}
