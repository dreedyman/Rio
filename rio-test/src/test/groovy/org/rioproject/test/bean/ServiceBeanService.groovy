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
package org.rioproject.test.bean

import org.rioproject.jsb.ServiceBeanAdapter
import org.rioproject.bean.Initialized
import org.rioproject.bean.Started
import org.rioproject.bean.PreDestroy
import org.rioproject.bean.SetConfiguration
import net.jini.config.Configuration
import org.rioproject.bean.SetParameters
import org.rioproject.bean.CreateProxy
import net.jini.id.UuidFactory
import net.jini.id.Uuid
import org.rioproject.resources.servicecore.AbstractProxy
import java.util.concurrent.atomic.AtomicInteger
import org.rioproject.bean.SetServiceBeanContext
import org.rioproject.core.jsb.ServiceBeanContext


class ServiceBeanService extends ServiceBeanAdapter implements ServiceBeanServiceInterface {
    AtomicInteger count = new AtomicInteger(0)
    boolean initializedInvoked = false
    boolean startedInvoked = false
    boolean destroyedInvoked = false
    String something
    String something2
    Map<String, ?> parms = null
    ServiceBeanServiceInterfaceProxy bogusProxy

    @Initialized
    public void initialized() {
        int c = count.getAndIncrement()
        if(c!=3)
            throw new IllegalStateException("@Initialized should be called fourth")
        initializedInvoked = true
    }

    @Started
    public void started() {
        startedInvoked = true
    }

    @PreDestroy
    public void destroyed() {
        destroyedInvoked = true
    }

    @SetConfiguration
    public void setDaConfiguration(Configuration config) {
        int c = count.getAndIncrement()
        if(c!=1)
            throw new IllegalStateException("@SetParameters should be called second")
        if(bogusProxy!=null)
            throw new IllegalStateException("configuration set after createProxy")
        something = config.getEntry('simple', 'something', String.class, "1")
        something2 = config.getEntry('simple2', 'something', String.class, "2")
    }

    public String getSomething() {
        return something
    }

    @SetServiceBeanContext
    public void setsbc(ServiceBeanContext sbc) {
        int c = count.getAndIncrement()
        if(c!=2)
            throw new IllegalStateException("@SetServiceBeanContext should be called third")
    }

    @SetParameters
    public void parms(Map<String, ?> p) {
        int c = count.getAndIncrement()
        if(c!=0)
            throw new IllegalStateException("@SetParameters should be called first")
        parms = p
    }

    @CreateProxy
    public ServiceBeanServiceInterface makeOne(ServiceBeanServiceInterface o) {
        if(something==null) {
            throw new IllegalStateException("createProxy called before configuration, something is null")
        }
        if(initializedInvoked) {
            throw new IllegalStateException("createProxy called before initializedInvoked")
        }
        if(parms==null) {
            throw new IllegalStateException("createProxy called before @SetParameters")
        }
        bogusProxy = new ServiceBeanServiceInterfaceProxy(o, UuidFactory.generate())
        return bogusProxy
    }
}

class ServiceBeanServiceInterfaceProxy extends AbstractProxy implements ServiceBeanServiceInterface, Serializable {

    def ServiceBeanServiceInterfaceProxy(ServiceBeanServiceInterface server, Uuid uuid) {
        super(server, uuid);
    }
}
