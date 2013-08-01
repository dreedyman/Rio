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
package org.rioproject.loader;

import org.junit.Assert;
import org.junit.Test;
import org.rioproject.opstring.ClassBundle;

import java.io.File;

/**
 * Test ClassBundleLoader
 */
public class ClassBundleLoaderTest {
    @Test
    public void testLoadClass() throws Exception {
        ClassBundle classBundle = new ClassBundle(JustForTesting.class.getName());
        classBundle.setCodebase(System.getProperty("user.dir")+File.separator+
                                "target"+File.separator+
                                "test-classes"+File.separator);
        Class testClass = ClassBundleLoader.loadClass(classBundle);
        Assert.assertNotNull(testClass);
    }
}
