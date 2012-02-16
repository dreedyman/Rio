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

import static org.junit.Assert.*;

import org.junit.BeforeClass;
import org.junit.Test;
import org.rioproject.RioVersion;
import org.rioproject.rmi.ResolvingLoader;
import org.rioproject.url.artifact.ArtifactURLStreamHandlerFactory;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.rmi.RMISecurityManager;

/**
 * Tests the {@link org.rioproject.rmi.ResolvingLoader}
 */
public class ResolvingLoaderTest {
    @BeforeClass
    public static void setSecurityPolicy() {
        URL.setURLStreamHandlerFactory(new ArtifactURLStreamHandlerFactory());
        String rioHome = System.getProperty("RIO_HOME");
        assertNotNull(rioHome);
        StringBuilder sb = new StringBuilder();
        sb.append(rioHome).append(File.separator).append("policy").append(File.separator).append("policy.all");
        System.setProperty("java.security.policy", sb.toString());
        if (System.getSecurityManager() == null)
            System.setSecurityManager(new RMISecurityManager());
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
        ClassLoader classLoader = loader.getClassLoader("artifact:com.sun.jini/reggie/jar/dl/2.1;http://www.rio-project.org/maven2");
        assertNotNull(classLoader);
        assertTrue("Returned ClassLoader should not be the same as the system ClassLoader", !(classLoader.equals(sysCL)));
        Class c = classLoader.loadClass("com.sun.jini.reggie.ConstrainableAdminProxy");
        assertNotNull(c);
    }

    @Test
    public void testGetClassLoaderAndCheckURLsAreFileBased() throws MalformedURLException, ClassNotFoundException {
        ResolvingLoader loader = new ResolvingLoader();
        assertNotNull(loader);
        ClassLoader classLoader = loader.getClassLoader("artifact:com.sun.jini/reggie/jar/dl/2.1;http://www.rio-project.org/maven2");
        assertNotNull(classLoader);
        assertTrue("Returned ClassLoader should be an instanceof URLClassLoader", classLoader instanceof URLClassLoader);
        for(URL u : ((URLClassLoader)classLoader).getURLs())
            assertTrue(u.getProtocol().equals("file"));
    }

    @Test
    public void testGetClassLoaderWith2Artifacts() throws MalformedURLException, ClassNotFoundException {
        ResolvingLoader loader = new ResolvingLoader();
        assertNotNull(loader);
        ClassLoader sysCL = ClassLoader.getSystemClassLoader();
        StringBuilder sb = new StringBuilder();
        sb.append("artifact:com.sun.jini/reggie/jar/dl/2.1;http://www.rio-project.org/maven2");
        sb.append(" ");
        sb.append("artifact:org.rioproject/rio-api/"+RioVersion.VERSION+";http://www.rio-project.org/maven2");
        ClassLoader classLoader = loader.getClassLoader(sb.toString());
        assertNotNull(classLoader);
        assertTrue("Returned ClassLoader should not be the same as the system ClassLoader", !(classLoader.equals(sysCL)));
        assertTrue("Returned ClassLoader should be an instanceof URLClassLoader", classLoader instanceof URLClassLoader);
        Class c = classLoader.loadClass("com.sun.jini.reggie.ConstrainableAdminProxy");
        assertNotNull(c);
    }

    @Test
    public void testGetClassAnnotation() throws MalformedURLException, ClassNotFoundException {
        URL[] u = new URL[]{new URL("artifact:com.sun.jini/reggie/jar/dl/2.1;http://www.rio-project.org/maven2")};
        URLClassLoader cl = new URLClassLoader(u);
        ResolvingLoader loader = new ResolvingLoader();
        Class<?> c = cl.loadClass("com.sun.jini.reggie.ConstrainableAdminProxy");
        String annotation = loader.getClassAnnotation(c);
        assertNotNull(annotation);
    }

}