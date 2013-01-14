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
import org.rioproject.cybernode.StaticCybernode
import org.rioproject.deploy.ServiceBeanInstantiationException
/**
 * Test bean lifecycle
 *
 * @author Dennis Reedy
 *
 */
public class LifecycleTest extends GroovyTestCase {

    void testConfigurationSet1() {
        StaticCybernode cybernode = new StaticCybernode()
        Throwable thrown = null
        try {
            ServiceBeanService service = cybernode.activate(ServiceBeanService.class.name) as ServiceBeanService
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
            Service service = cybernode.activate(Service.class.name) as Service
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
        Service service = cybernode.activate(Service.class.name) as Service
        assertTrue service.initializedInvoked
        cybernode.destroy()
    }

    void testInitialized() {
        StaticCybernode cybernode = new StaticCybernode()
        Service service = cybernode.activate(Service.class.name) as Service
        assertTrue service.initializedInvoked
        cybernode.destroy()
    }

    void testInitialized2() {
        StaticCybernode cybernode = new StaticCybernode()
        AnnotatedService service = cybernode.activate(AnnotatedService.class.name) as AnnotatedService
        assertTrue service.initializedInvoked
        cybernode.destroy()
    }

    void testStarted() {
        StaticCybernode cybernode = new StaticCybernode()
        Service service = cybernode.activate(Service.class.name) as Service
        assertTrue service.startedInvoked
        cybernode.destroy()
    }

    void testStarted2() {
        StaticCybernode cybernode = new StaticCybernode()
        AnnotatedService service = cybernode.activate(AnnotatedService.class.name) as AnnotatedService
        assertTrue service.startedInvoked
        cybernode.destroy()
    }

    void testSetServiceBeanContext() {
        StaticCybernode cybernode = new StaticCybernode()
        AnnotatedService service = cybernode.activate(AnnotatedService.class.name) as AnnotatedService
        assertTrue service.contextInvoked
        cybernode.destroy()
    }

    void testSetConfiguration() {
        StaticCybernode cybernode = new StaticCybernode()
        AnnotatedService service = cybernode.activate(AnnotatedService.class.name) as AnnotatedService
        assertTrue service.configInvoked
        cybernode.destroy()
    }

    void testSetParameters() {
        StaticCybernode cybernode = new StaticCybernode()
        AnnotatedService service = cybernode.activate(AnnotatedService.class.name) as AnnotatedService
        assertTrue service.parametersInvoked
        cybernode.destroy()
    }

    void testSetServiceBean() {
        StaticCybernode cybernode = new StaticCybernode()
        AnnotatedService service = cybernode.activate(AnnotatedService.class.name) as AnnotatedService
        assertTrue service.parametersInvoked
        cybernode.destroy()
    }

    void testPreAdvertise() {
        StaticCybernode cybernode = new StaticCybernode()
        AnnotatedService service = cybernode.activate(AnnotatedService.class.name) as AnnotatedService
        assertTrue service.preAdvertisedInvoked
        cybernode.destroy()
    }

    void testPostUnAdvertise() {
        StaticCybernode cybernode = new StaticCybernode()
        AnnotatedService service = cybernode.activate(AnnotatedService.class.name) as AnnotatedService
        cybernode.destroy()
        assertTrue service.postUnAdvertisedInvoked
    }

    void testPreDestroy() {
        StaticCybernode cybernode = new StaticCybernode()
        Service service = cybernode.activate(Service.class.name) as Service
        cybernode.destroy()
        assertTrue service.destroyedInvoked
    }

    void testOrder() {
        StaticCybernode cybernode = new StaticCybernode()
        AnnotatedService service = cybernode.activate(AnnotatedService.class.name) as AnnotatedService
        assertEquals 7, service.order.size()
        assertEquals "set-parameters", service.order.get(0)
        assertEquals "set-config", service.order.get(1)
        assertEquals "set-context", service.order.get(2)
        assertEquals "set-service-bean", service.order.get(3)
        assertEquals "initialized", service.order.get(4)
        assertEquals "started", service.order.get(5)
        assertEquals "pre-advertise", service.order.get(6)
        cybernode.destroy()
    }

    void testOrderWithAssociations() {
        StaticCybernode cybernode = new StaticCybernode()
        Map<String, Object> map = cybernode.activate(new File("src/test/resources/opstring/annotatedService.groovy"))
        Service service1 = map.get("Annotated-1")  as Service
        assertEquals 4, service1.order.size()
        assertEquals "inject-service", service1.order.get(0)
        assertEquals "initialized", service1.order.get(1)
        assertEquals "started", service1.order.get(2)
        assertEquals "pre-advertise", service1.order.get(3)

        Service service2 = map.get("Annotated-2")  as Service
        assertEquals 3, service2.order.size()
        assertEquals "initialized", service2.order.get(0)
        assertEquals "started", service2.order.get(1)
        assertEquals "pre-advertise", service2.order.get(2)
        cybernode.destroy()
    }

    void testThatServiceThatThrowsDoesNotGetCreated() {
        StaticCybernode cybernode = new StaticCybernode()
        ServiceBeanInstantiationException thrown = null
        try {
            cybernode.activate(org.rioproject.test.bean.ServiceThatThrowsDuringLifecycle.class.name)
        } catch(ServiceBeanInstantiationException e) {
            thrown = e
        }
        assertNotNull("Expected a ServiceBeanInstantiationException", thrown)
        assertTrue(thrown.uninstantiable)
        assertNotNull("Expected a ServiceBeanInstantiationException", thrown.getCauseExceptionDescriptor())
        StackTraceElement element = getElement(thrown.getCauseExceptionDescriptor().stacktrace)
        assertNotNull("Expected a matching StackTraceElement", element)
        assertEquals("Expected the parms method to throw", "parms", element.methodName)
    }

    StackTraceElement getElement(def stack) {
        StackTraceElement element = null
        for (StackTraceElement elem : stack) {
            if(elem.className.equals(ServiceThatThrowsDuringLifecycle.class.name)) {
                element = elem
                break
            }
        }
        if(element==null && t.getCause()!=null)
            element = getElement(t.getCause())
        return element
    }


}