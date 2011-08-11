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
import org.rioproject.deploy.ServiceBeanInstantiationException

/**
 * Test loading service config
 */
class ServiceConfigTest extends GroovyTestCase {

    void testLoadBadClasspath() {
        System.setProperty('rio.test.config', 'useclasspath')
        StaticCybernode cybernode = new StaticCybernode()
        Throwable thrown = null
        try {
            cybernode.activate(
                new File('src/test/resources/bean_empty_use_config.groovy'))
        } catch (Throwable t) {
            thrown = t
        }
        assertNotNull thrown
        assertEquals ServiceBeanInstantiationException.class.name, thrown.getClass().name
        cybernode.destroy()
    }

    void testMultiFileConfig() {
        System.setProperty('rio.test.config', 'use-multi-file')
        StaticCybernode cybernode = new StaticCybernode()
        Throwable thrown = null
        try {
            Map<String, Object> map =
                cybernode.activate(
                    new File('src/test/resources/bean_empty_use_config.groovy'))
            assertEquals 1, map.size()
            Service service = map.get('Test')
            assertNotNull service
            assertEquals 'something', service.something
            assertEquals 'something-else', service.something2
        } catch (Throwable t) {
            t.printStackTrace()
            thrown = t
        }
        assertNull 'Exception ['+thrown.getClass().name+'] was thrown, should be null', thrown
        cybernode.destroy()
    }

    void testMultiClasspathConfig() {
        System.setProperty('rio.test.config', 'use-multi-classpath')
        StaticCybernode cybernode = new StaticCybernode()
        Throwable thrown = null
        try {
            Map<String, Object> map =
                cybernode.activate(
                    new File('src/test/resources/bean_empty_use_config.groovy'))
            assertEquals 1, map.size()
            Service service = map.get('Test')
            assertNotNull service
            assertEquals 'something', service.something
            assertEquals 'something-else', service.something2
        } catch (Throwable t) {
            t.printStackTrace()
            thrown = t
        }
        assertNull 'Exception ['+thrown.getClass().name+'] was thrown, should be null', thrown
        cybernode.destroy()
    }


    void testLoadBadFile() {
        System.setProperty('rio.test.config', 'usebadfile')
        StaticCybernode cybernode = new StaticCybernode()
        Throwable thrown = null
        try {
            cybernode.activate(
                    new File('src/test/resources/bean_empty_use_config.groovy'))
        } catch (Throwable t) {
            thrown = t
        }
        assertNotNull thrown
        assertEquals ServiceBeanInstantiationException.class.name, thrown.getClass().name
        cybernode.destroy()
    }

    void testLoadFile() {
        System.setProperty('rio.test.config', 'usefile')
        StaticCybernode cybernode = new StaticCybernode()
        Throwable thrown = null
        try {
            Map<String, Object> map =
                cybernode.activate(
                    new File('src/test/resources/bean_empty_use_config.groovy'))
            assertEquals 1, map.size()
            Service service = map.get('Test')
            assertNotNull service
            assertEquals 'something', service.something
        } catch (Throwable t) {
            t.printStackTrace()
            thrown = t
        }
        assertNull 'Exception ['+thrown.getClass().name+'] was thrown, should be null', thrown
        cybernode.destroy()
    }

    void testAnonymousGroovyConfig() {
        System.setProperty('rio.test.config', 'anon-groovy')
        StaticCybernode cybernode = new StaticCybernode()
        Throwable thrown = null
        try {
            Map<String, Object> map =
                cybernode.activate(
                    new File('src/test/resources/bean_empty_anon_config.groovy'))
            assertEquals 1, map.size()
            Service service = map.get('Test')
            assertNotNull service
            assertEquals 'something', service.something
        } catch (Throwable t) {
            t.printStackTrace()
            thrown = t
        }
        assertNull 'Exception ['+thrown.getClass().name+'] was thrown, should be null', thrown
        cybernode.destroy()
    }

    void testAnonymousGroovyConfig2() {
        System.setProperty('rio.test.config', 'anon-groovy-2')
        StaticCybernode cybernode = new StaticCybernode()
        Throwable thrown = null
        try {
            Map<String, Object> map =
            cybernode.activate(
                new File('src/test/resources/bean_empty_anon_config.groovy'))
            assertEquals 1, map.size()
            Service service = map.get('Test')
            assertNotNull service
            assertEquals 'something', service.something
        } catch (Throwable t) {
            t.printStackTrace()
            thrown = t
        }
        assertNull 'Exception ['+thrown.getClass().name+'] was thrown, should be null', thrown
        cybernode.destroy()
    }

    void testAnonymousJiniConfig() {
        System.setProperty('rio.test.config', 'anon-jiniconfig')
        StaticCybernode cybernode = new StaticCybernode()
        Throwable thrown = null
        try {
            Map<String, Object> map =
                cybernode.activate(
                    new File('src/test/resources/bean_empty_anon_config.groovy'))
            assertEquals 1, map.size()
            Service service = map.get('Test')
            assertNotNull service
            assertEquals 'something', service.something
        } catch (Throwable t) {
            t.printStackTrace()
            thrown = t
        }
        assertNull 'Exception ['+thrown.getClass().name+'] was thrown, should be null', thrown
        cybernode.destroy()
    }
}