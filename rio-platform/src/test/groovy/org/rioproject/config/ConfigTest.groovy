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
package org.rioproject.config

import net.jini.config.Configuration
import net.jini.config.ConfigurationException
import net.jini.config.ConfigurationProvider
import net.jini.config.NoSuchEntryException
import org.junit.Test
import static org.junit.Assert.*

/**
 * Test groovy configs
 *
 * @author Dennis Reedy
 */
class ConfigTest  {

    @Test
    void testSimpleCompiled() {
        def files = []
        for(File f : new File('src/test/resources/compiled-configs').listFiles()) {
            files << f.path
        }
        Configuration config = ConfigurationProvider.getInstance(files as String[])
        String s = (String)config.getEntry('bean.config',
                                           'food',
                                           String.class);
        assertEquals 'yummy', s
    }

    @Test
    void testSimple() {
        def conf = ['src/test/resources/bean_config.groovy']        
        Configuration config = ConfigurationProvider.getInstance((String[])conf)
        String s = (String)config.getEntry('bean.config',
                                           'food',
                                           String.class);
        assertEquals 'yummy', s
    }

    @Test
    void testExtendsCompiled() {
        def files = []
        for(File f : new File('src/test/resources/compiled-configs').listFiles()) {
            files << f.path
        }
        Configuration config = ConfigurationProvider.getInstance(files as String[])
        doTestExtends(config)
    }

    @Test
    void testExtends() {
        def conf = ['src/test/resources/bean_config.groovy']
        Configuration config = ConfigurationProvider.getInstance((String[])conf)
        doTestExtends(config)
    }

    void doTestExtends(Configuration config) {
        Map<String, List<String>> map1 = (Map)config.getEntry('Bean',
                                                              'testMap',
                                                              Map.class);
        assertEquals 1, map1.size()
        List<String> ls1 = map1.get('foo')
        assertEquals 1, ls1.size()
        assertEquals 'bar', ls1.get(0)

        String s = (String)config.getEntry('bean.config',
                                           'food',
                                           String.class);
        assertEquals s, 'yummy'
        s = (String)config.getEntry('bean.config',
                                    'food',
                                    String.class,
                                    Configuration.NO_DEFAULT,
                                    'good');
        assertEquals s, 'yummy good'

        Map<String, List<String>> map2 = (Map)config.getEntry('bean.config',
                                                              'testMap',
                                                              Map.class);
        assertEquals 1, map2.size()
        List<String> ls2 = map2.get('foo')
        assertEquals 2, ls2.size()
        assertEquals 'bar', ls2.get(0)
        assertEquals 'baz', ls2.get(1)
    }

    @Test
    void testCheckDefaults() {
        def conf = ['src/test/resources/bean_config.groovy']
        Configuration config = ConfigurationProvider.getInstance((String[])conf)
        String usedDefault = "usedDefault"
        String s1  = (String)config.getEntry('BeanExtendedX',
                                             'notDeclared',
                                             String.class,
                                             "usedDefault")
        assertEquals usedDefault, s1
        String s2  = (String)config.getEntry('Bean',
                                             'notDeclared',
                                             String.class,
                                             "usedDefault")
        assertEquals usedDefault, s2
    }

    @Test
    void testCheckforFailure() {
        def conf = ['src/test/resources/bean_config.groovy']
        Configuration config = ConfigurationProvider.getInstance((String[])conf)
        Exception thrown = null
        try {
            config.getEntry('BeanExtendedX', 'testMap', String.class)
        } catch(Exception e) {
            thrown = e
        }
        assertNotNull thrown
        assertEquals NoSuchEntryException.class.name, thrown.class.name

        thrown = null
        try {
            config.getEntry('Bean', 'notDeclared', String.class)
        } catch(Exception e) {
            thrown = e
        }

        assertNotNull thrown
        assertEquals NoSuchEntryException.class.name, thrown.class.name
    }

    @Test
    void testForNulls() {
        def conf = ['src/test/resources/bean_config.groovy']
        Configuration config = ConfigurationProvider.getInstance((String[])conf)
        String shouldbeNull = (String)config.getEntry("Bean", "nullValue", String.class, null)
        assertNull shouldbeNull
    }

    @Test
    void testGetLong() {
        def conf = ['src/test/resources/bean_config.groovy']
        Configuration config = ConfigurationProvider.getInstance((String[])conf)
        long l = config.getEntry('Config', 'longValue', long.class)
        assertEquals 99, l
        l = config.getEntry('Config', 'longValue', Long.class)
        assertEquals 99, l
    }

    @Test
    void testGetWithClosure() {
        def conf = ['src/test/resources/bean_config.groovy']
        Configuration config = ConfigurationProvider.getInstance((String[])conf)
        Map map = config.getEntry("Config", "withClosure", Map.class)
        assertNotNull map
        assertEquals 3, map.size()
        assertEquals 'oof', map.get('foo')
        assertEquals 'rab', map.get('bar')
        assertEquals 'zab', map.get('baz')

    }

    @Test
    void testBadComponentName() {
        Exception thrown = null
        def conf = ['src/test/resources/bad_config.groovy']
        try {
            Configuration config = ConfigurationProvider.getInstance((String[])conf)
        } catch(Exception e) {
            thrown = e
        }
        assertNotNull 'Should have non-null exception', thrown        
    }

    @Test
    void testBadScript() {
        Exception thrown = null
        def conf = ['src/test/resources/bad_config2.groovy']
        try {
            Configuration config = ConfigurationProvider.getInstance((String[])conf)
            config.getEntry("foo.bar", "baz", String.class, null)
        } catch(Exception e) {
            thrown = e
        }
        assertNotNull thrown
        assertTrue 'Should have a ConfigurationException', (thrown instanceof ConfigurationException)
        assertTrue "Should have a cause of MissingPropertyException", (thrown.getCause() instanceof MissingPropertyException)

        thrown = null
        String result = ""
        try {
            Configuration config = ConfigurationProvider.getInstance((String[])conf)
            result = (String)config.getEntry("foo.bar", "booya", String.class, null)
        } catch(Exception e) {
            thrown = e
        }

        assertNull thrown
        assertNull 'Should get a null result', result

        thrown = null
        try {
            Configuration config = ConfigurationProvider.getInstance((String[])conf)
            result = (String)config.getEntry("foo.bar", "booya", String.class)
        } catch(Exception e) {
            thrown = e
        }

        assertNotNull thrown
        assertTrue 'Should have a ConfigurationException', (thrown instanceof NoSuchEntryException)
    }
}