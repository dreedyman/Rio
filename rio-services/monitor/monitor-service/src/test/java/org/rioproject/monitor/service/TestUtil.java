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
package org.rioproject.monitor.service;

import org.rioproject.opstring.ClassBundle;
import org.rioproject.opstring.ServiceBeanConfig;
import org.rioproject.opstring.ServiceElement;

/**
 * @author Dennis Reedy
 */
public class TestUtil {

    public static ServiceElement makeServiceElement(String name, String opStringName) {
        return makeServiceElement(name, opStringName, 1);
    }

    public static ServiceElement makeServiceElement(String name, String opStringName, int planned) {
        ServiceElement elem = new ServiceElement();
        ClassBundle exports = new ClassBundle(Object.class.getName());
        elem.setExportBundles(exports);
        ClassBundle main = new ClassBundle("");
        elem.setComponentBundle(main);
        ServiceBeanConfig sbc = new ServiceBeanConfig();
        sbc.setName(name);
        sbc.setOperationalStringName(opStringName);
        elem.setServiceBeanConfig(sbc);
        elem.setPlanned(planned);
        return elem;
    }
}
