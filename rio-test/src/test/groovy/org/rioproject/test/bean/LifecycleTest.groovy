/*
 * Copyright 2008 the original author or authors.
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

import org.rioproject.cybernode.StaticCybernode

/**
 * Test bean lifecycle
 */
public class LifecycleTest extends GroovyTestCase {

    void testConfigurationSet1() {
        StaticCybernode cybernode = new StaticCybernode()
        Throwable thrown = null
        try {
            ServiceBeanService service =
            cybernode.activate(org.rioproject.test.bean.ServiceBeanService.class.name)
            assertTrue service.something != null
            assertTrue service.something2 != null
            assertTrue service.bogusProxy != null
        } catch(Throwable t) {
            t.printStackTrace()
            thrown = t
        }
        assertTrue thrown==null
        cybernode.destroy()
    }

    void testConfigurationSet2() {
        StaticCybernode cybernode = new StaticCybernode()
        Throwable thrown = null
        try {
            Service service =
            cybernode.activate(org.rioproject.test.bean.Service.class.name)
            assertTrue service.something != null
            assertTrue service.something2 != null
            assertTrue service.bogusProxy != null
        } catch(Throwable t) {
            t.printStackTrace()
            thrown = t
        }
        assertTrue thrown==null
        cybernode.destroy()
    }

    void testCreateProxy() {
        StaticCybernode cybernode = new StaticCybernode()
        Service service =
            cybernode.activate(org.rioproject.test.bean.Service.class.name)
        assertTrue service.initializedInvoked
        cybernode.destroy()
    }

    void testInitialized() {
        StaticCybernode cybernode = new StaticCybernode()
        Service service =
            cybernode.activate(org.rioproject.test.bean.Service.class.name)
        assertTrue service.initializedInvoked
        cybernode.destroy()
    }

    void testInitialized2() {
        StaticCybernode cybernode = new StaticCybernode()
        AnnotatedService service =
            cybernode.activate(org.rioproject.test.bean.AnnotatedService.class.name)
        assertTrue service.initializedInvoked
        cybernode.destroy()
    }

    void testStarted() {
        StaticCybernode cybernode = new StaticCybernode()
        Service service =
            cybernode.activate(org.rioproject.test.bean.Service.class.name)
        assertTrue service.startedInvoked
        cybernode.destroy()
    }

    void testStarted2() {
        StaticCybernode cybernode = new StaticCybernode()
        AnnotatedService service =
            cybernode.activate(org.rioproject.test.bean.AnnotatedService.class.name)
        assertTrue service.startedInvoked
        cybernode.destroy()
    }

    void testSetServiceBeanContext() {
        StaticCybernode cybernode = new StaticCybernode()
        AnnotatedService service =
            cybernode.activate(org.rioproject.test.bean.AnnotatedService.class.name)
        assertTrue service.contextInvoked
        cybernode.destroy()
    }

    void testSetConfiguration() {
        StaticCybernode cybernode = new StaticCybernode()
        AnnotatedService service =
            cybernode.activate(org.rioproject.test.bean.AnnotatedService.class.name)
        assertTrue service.configInvoked
        cybernode.destroy()
    }

    void testSetParameters() {
        StaticCybernode cybernode = new StaticCybernode()
        AnnotatedService service =
            cybernode.activate(org.rioproject.test.bean.AnnotatedService.class.name)
        assertTrue service.parametersInvoked
        cybernode.destroy()
    }

    void testSetServiceBean() {
        StaticCybernode cybernode = new StaticCybernode()
        AnnotatedService service =
            cybernode.activate(org.rioproject.test.bean.AnnotatedService.class.name)
        assertTrue service.parametersInvoked
        cybernode.destroy()
    }

    void testPreAdvertise() {
        StaticCybernode cybernode = new StaticCybernode()
        AnnotatedService service =
            cybernode.activate(org.rioproject.test.bean.AnnotatedService.class.name)
        assertTrue service.preAdvertisedInvoked
        cybernode.destroy()
    }

    void testPostAdvertise() {
        StaticCybernode cybernode = new StaticCybernode()
        AnnotatedService service =
            cybernode.activate(org.rioproject.test.bean.AnnotatedService.class.name)
        cybernode.destroy()
        assertTrue service.postAdvertisedInvoked
    }

    void testPreUnAdvertise() {
        StaticCybernode cybernode = new StaticCybernode()
        AnnotatedService service =
            cybernode.activate(org.rioproject.test.bean.AnnotatedService.class.name)
        cybernode.destroy()
        assertTrue service.preUnAdvertisedInvoked
    }

    void testPostUnAdvertise() {
        StaticCybernode cybernode = new StaticCybernode()
        AnnotatedService service =
            cybernode.activate(org.rioproject.test.bean.AnnotatedService.class.name)
        cybernode.destroy()
        assertTrue service.postUnAdvertisedInvoked
    }

    void testPreDestroy() {
        StaticCybernode cybernode = new StaticCybernode()
        Service service =
            cybernode.activate(org.rioproject.test.bean.Service.class.name)
        cybernode.destroy()
        assertTrue service.destroyedInvoked
    }
}