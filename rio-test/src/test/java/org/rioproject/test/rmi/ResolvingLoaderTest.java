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
package org.rioproject.test.rmi;

import org.junit.BeforeClass;
import org.junit.Test;
import org.rioproject.rmi.ResolvingLoader;
import org.rioproject.url.artifact.ArtifactURLStreamHandlerFactory;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Tests the {@link org.rioproject.rmi.ResolvingLoader}
 */
public class ResolvingLoaderTest {
    @BeforeClass
    public static void setSecurityPolicy() {
        URL.setURLStreamHandlerFactory(new ArtifactURLStreamHandlerFactory());
        if (System.getSecurityManager() == null) {
            System.setSecurityManager(new SecurityManager());
        }
    }

    @Test
    public void testCreation() {
        ResolvingLoader loader = new ResolvingLoader();
        assertNotNull(loader);
    }

    @Test
    public void testGetClassLoader() throws MalformedURLException, ClassNotFoundException {
        ResolvingLoader loader = new ResolvingLoader();
        assertNotNull(loader);
        ClassLoader sysCL = ClassLoader.getSystemClassLoader();
        ClassLoader classLoader = loader.getClassLoader("artifact:com.sun.jini/reggie-dl/2.1.1;http://www.rio-project.org/maven2;http://repo1.maven.org/maven2/@central;http://10.0.1.9:52117@Provision Monitor ");
        assertNotNull(classLoader);
        assertTrue("Returned ClassLoader should not be the same as the system ClassLoader", !(classLoader.equals(sysCL)));
        Class c = classLoader.loadClass("com.sun.jini.reggie.ConstrainableAdminProxy");
        assertNotNull(c);
    }

    @Test
    public void testGetClassLoaderAndCheckURLsAreFileBased() throws MalformedURLException, ClassNotFoundException {
        ResolvingLoader loader = new ResolvingLoader();
        assertNotNull(loader);
        /*ClassLoader classLoader = loader.getClassLoader("artifact:com.sun.jini/reggie/jar/dl/2.1;http://www.rio-project.org/maven2");*/
        ClassLoader classLoader = loader.getClassLoader("artifact:com.sun.jini/reggie-dl/2.1.1;http://www.rio-project.org/maven2");
        assertNotNull(classLoader);
        assertTrue("Returned ClassLoader should be an instanceof URLClassLoader", classLoader instanceof URLClassLoader);
        for(URL u : ((URLClassLoader)classLoader).getURLs())
            assertTrue(u.getProtocol().equals("file"));
    }

    @Test
    public void testGetClassAnnotation() throws MalformedURLException, ClassNotFoundException {
        URL[] u = new URL[]{new URL("artifact:com.sun.jini/reggie-dl/2.1.1;http://www.rio-project.org/maven2")};
        URLClassLoader cl = new URLClassLoader(u);
        ResolvingLoader loader = new ResolvingLoader();
        Class<?> c = cl.loadClass("com.sun.jini.reggie.ConstrainableAdminProxy");
        String annotation = loader.getClassAnnotation(c);
        assertNotNull(annotation);
    }

}
