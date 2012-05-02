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
import org.rioproject.opstring.ServiceElement
import org.rioproject.opstring.ClassBundle
import org.rioproject.opstring.ServiceBeanConfig
import org.rioproject.associations.AssociationDescriptor
import org.rioproject.associations.AssociationType
import org.rioproject.opstring.OpString
import org.rioproject.admin.ServiceBeanControl
import org.rioproject.admin.ServiceBeanControlException

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