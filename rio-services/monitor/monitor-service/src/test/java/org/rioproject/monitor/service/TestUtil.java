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

import org.rioproject.deploy.DeployAdmin;
import org.rioproject.event.EventHandler;
import org.rioproject.monitor.ProvisionMonitor;
import org.rioproject.opstring.ClassBundle;
import org.rioproject.opstring.ServiceBeanConfig;
import org.rioproject.opstring.ServiceElement;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

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

    public static DeployAdmin createDeployAdmin() {
        return (DeployAdmin) Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(),
                                                    new Class[]{DeployAdmin.class},
                                                    new IH());
    }

    public static ProvisionMonitor createProvisionMonitor() {
        return (ProvisionMonitor) Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(),
                                                         new Class[]{ProvisionMonitor.class},
                                                         new IH());
    }

    public static EventHandler createEventHandler() {
        return (EventHandler) Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(),
                                                     new Class[]{EventHandler.class},
                                                     new IH());
    }

    static class IH implements InvocationHandler {
        @Override public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            System.out.println("===> "+method.getName());
            return null;
        }
    }
}
