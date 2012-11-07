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
package org.rioproject.cybernode;

import net.jini.config.ConfigurationException;
import org.rioproject.config.AggregateConfig;
import org.rioproject.config.GroovyConfig;
import org.rioproject.core.jsb.ServiceBeanContext;
import org.rioproject.jsb.JSBContext;
import org.rioproject.log.LoggerConfig;

import java.lang.reflect.Field;
import java.security.AccessController;
import java.security.PrivilegedAction;

/**
 * Service termination helper.
 */
public class ServiceTerminationHelper {

    /**
     * Cleanup resources that may cause memory leaks
     *
     * @param context The <code>ServiceBeanContext</code> to use
     */
    public static void cleanup(ServiceBeanContext context) {
        if (context != null) {
            /* Release any created Handlers */
            LoggerConfig[] loggerConfigs = context.getServiceBeanConfig().getLoggerConfigs();
            if (loggerConfigs != null) {
                for (LoggerConfig loggerConfig : loggerConfigs) {
                    loggerConfig.close();
                }
            }

            /* If we have a groovy configuration, clean the configuration, clearing
             * the class cache and setting the embedded groovy classloader to null */
            try {
                if (context.getConfiguration() instanceof AggregateConfig) {
                    AggregateConfig ac = (AggregateConfig) context.getConfiguration();
                    if (ac.getOuterConfiguration() instanceof GroovyConfig) {
                        ((GroovyConfig) ac.getOuterConfiguration()).clear();
                    }
                    if (context instanceof JSBContext)
                        ((JSBContext) context).setConfiguration(null);
                }
            } catch (ConfigurationException e) {
                /* */
            }
        }

        /* Services using groovy leak memory on undeployment. The object retaining a reference to the
         * GroovyClassLoader (that has as it's parent the ServiceClassLoader) is a static
         * org.codehaus.groovy.control.CompilationUnit field in the
         * org.codehaus.groovy.transform.ASTTransformationVisitor class */
        try {
            final Class clazz = loadClass("org.codehaus.groovy.transform.ASTTransformationVisitor");
            final Field compUnit = clazz.getDeclaredField("compUnit");
            compUnit.setAccessible(true);
            // static field
            compUnit.set(null, null);
        } catch (Throwable t) {
            // ignore
        }
    }

    static Class loadClass(final String className) {
        return AccessController.doPrivileged(new PrivilegedAction<Class>() {
            public Class run() {
                try {
                    return Thread.currentThread().getContextClassLoader().loadClass(className);
                } catch (ClassNotFoundException e) {
                    return null;
                }
            }
        });
    }

}
