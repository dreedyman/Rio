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
package org.rioproject.event;

import java.net.MalformedURLException;
import java.rmi.server.RMIClassLoader;
import java.rmi.server.RMIClassLoaderSpi;

/**
 *
 */
public class EventResolver extends RMIClassLoaderSpi {
    private static final RMIClassLoaderSpi loader = RMIClassLoader.getDefaultProviderInstance();

    public Class<?> loadClass(String s, String s1, ClassLoader classLoader) throws MalformedURLException, ClassNotFoundException {
        return loader.loadClass(s, s1, classLoader);
    }

    public Class<?> loadProxyClass(String s, String[] strings, ClassLoader classLoader) throws MalformedURLException, ClassNotFoundException {
        return loader.loadProxyClass(s, strings, classLoader);
    }

    public ClassLoader getClassLoader(String s) throws MalformedURLException {
        return loader.getClassLoader(s);
    }

    public String getClassAnnotation(Class<?> aClass) {
        return loader.getClassAnnotation(aClass);
    }
}
