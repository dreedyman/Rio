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

import com.sun.jini.start.ClassLoaderUtil;
import edu.emory.mathcs.util.classloader.URIClassLoader;
import org.rioproject.opstring.ClassBundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedAction;

/**
 * Utility for loading classes defined by a {@link org.rioproject.opstring.ClassBundle}
 */
public class ClassBundleLoader {
    private static Logger logger = LoggerFactory.getLogger(ClassBundleLoader.class.getName());

    /**
     * Load the class using resources in the {@code ClassBundle}. If there are no
     * resources for the class the parent ClassLoader will be used to load the
     * class. If there are resources (jars), a new ClassLoader will be created,
     * setting the delegating ClassLoader to the contextClassLoader
     *
     * @param bundle The ClassBundle to load
     * @return A new Class instance each time this method is invoked
     *
     * @throws ClassNotFoundException If the class cannot be loaded
     * @throws MalformedURLException If URLs cannot be created
     */
    public static Class loadClass(ClassBundle bundle) throws ClassNotFoundException, MalformedURLException {
        Class theClass = null;
        final Thread currentThread = Thread.currentThread();
        final ClassLoader cCL = AccessController.doPrivileged(
                new PrivilegedAction<ClassLoader>() {
                    public ClassLoader run() {
                        return (currentThread.getContextClassLoader());
                    }
                });
        try {
            theClass = loadClass(cCL, bundle);
        } finally {
            AccessController.doPrivileged(new PrivilegedAction<ClassLoader>() {
                public ClassLoader run() {
                    currentThread.setContextClassLoader(cCL);
                    return (null);
                }
            });
        }
        return theClass;
    }

    /**
     * Load the class using resources in the {@code ClassBundle}. If there are no
     * resources for the class the parent ClassLoader will be used to load the
     * class. If there are resources (jars), a new ClassLoader will be created.
     *
     * @param parent Parent ClassLoader to use for delegation.
     * @param bundle The ClassBundle to load
     *
     * @return A new Class instance each time this method is invoked
     *
     * @throws ClassNotFoundException If the class cannot be loaded
     * @throws MalformedURLException If URLs cannot be created
     */
    public static Class loadClass(final ClassLoader parent, ClassBundle bundle)
        throws ClassNotFoundException, MalformedURLException {
        if(parent == null)
            throw new IllegalArgumentException("parent is null");
        if(bundle == null)
            throw new IllegalArgumentException("bundle is null");
        ClassLoader loader = parent;
        String className =  bundle.getClassName();
        URL[] urls = bundle.getJARs();
        if(urls.length > 0) {
            try {
                loader = new URIClassLoader(ServiceClassLoader.getURIs(urls), parent);
            } catch (URISyntaxException e) {
                throw new MalformedURLException("Creating URIs");
            }
        }
        if(logger.isTraceEnabled()) {
            logger.trace("Using ClassLoader [{}] to load class {}", loader.getClass().getName(), className);
            ClassLoaderUtil.displayClassLoaderTree(loader);
        }
        return (loader.loadClass(className));
    }
}
