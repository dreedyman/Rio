/*
 * Copyright to the original author or authors.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *         http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.rioproject.start.descriptor;

import com.sun.jini.start.ServiceDescriptor;
import net.jini.config.Configuration;
import org.rioproject.loader.ServiceClassLoader;
import org.rioproject.start.util.ClassLoaderUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.net.URLClassLoader;

/**
 * @author Dennis Reedy
 */
public class ConfigurationServiceDescriptor implements ServiceDescriptor {
    private final String classPath;
    private final String implClassName;
    private final Configuration configuration;
    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigurationServiceDescriptor.class);

    public ConfigurationServiceDescriptor(String classPath,
                                          String implClassName,
                                          Configuration configuration) {
        this.classPath = classPath;
        this.implClassName = implClassName;
        this.configuration = configuration;
    }

    @Override public Object create(Configuration config) throws Exception {
        Thread currentThread = Thread.currentThread();
        ClassLoader currentClassLoader = currentThread.getContextClassLoader();
        if (currentClassLoader instanceof URLClassLoader) {
            LOGGER.info("====> " + implClassName + ", cL: " + currentClassLoader);
        }
        URLClassLoader serviceCL = new URLClassLoader(ClassLoaderUtil.getClasspathURLs(classPath),
                                                      currentClassLoader);
        currentThread.setContextClassLoader(serviceCL);
        try {
            Class<?> implClass = Class.forName(implClassName, false, serviceCL);
            //Class<?> implClass = serviceCL.loadClass(implClassName);
            Constructor<?> constructor = implClass.getDeclaredConstructor(Configuration.class);
            constructor.setAccessible(true);
            Object impl = constructor.newInstance(configuration);
            return new RioServiceDescriptor.Created(impl, null);
        } finally {
            currentThread.setContextClassLoader(currentClassLoader);
        }
    }
}
