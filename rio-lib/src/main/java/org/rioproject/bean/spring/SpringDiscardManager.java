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
package org.rioproject.bean.spring;

import org.rioproject.core.jsb.DiscardManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;

/**
 * Destroys the Spring application context once the bean is destroyed.
 *
 * @author Dennis Reedy
 */
public class SpringDiscardManager implements DiscardManager {
    //private AbstractApplicationContext springContext;
    private Object springContext;
    private DiscardManager discardManager;
    static final String COMPONENT = "org.rioproject.bean";
    static final Logger logger = LoggerFactory.getLogger(COMPONENT);

    /**
     * Create a SpringDiscardManager, destroying the Spring application
     * context, and delegating to the infrastructure created
     * {@link org.rioproject.core.jsb.DiscardManager} upon service discard
     * notification
     *
     * @param springContext The Spring application context
     * @param discardManager The infrastructure created
     * {@link org.rioproject.core.jsb.DiscardManager}
     *
     * @throws IllegalArgumentException if either parameter is <code>null</code>
     */
    public SpringDiscardManager(Object springContext,
                                DiscardManager discardManager) {
        if(springContext==null)
            throw new IllegalArgumentException("springContext is null");
        if(discardManager==null)
            throw new IllegalArgumentException("discardManager is null");
        this.springContext = springContext;
        this.discardManager = discardManager;
    }

    /**
     * @see org.rioproject.core.jsb.DiscardManager#discard()
     */
    public void discard() {
        /*
         * Reflection is used here because Spring technology classes may be
         * loaded by a child classloader of the the classloader which loaded
         * this class. If this is the case then we will be facing
         * NoClassDefFoundError exceptions.
         */
        try {
            //springContext.close();
            Method close = springContext.getClass().getMethod("close",
                                                              (Class[])null);
            close.invoke(springContext, (Object[])null);
        } catch (Exception e) {
            logger.warn("Closing Spring ApplicationContext", e);
        } finally {
            discardManager.discard();
        }
    }
}
