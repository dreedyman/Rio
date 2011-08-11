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
package org.rioproject.serviceui;

import net.jini.core.lookup.ServiceItem;
import net.jini.lookup.ui.factory.JFrameFactory;

import javax.swing.*;
import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.AccessController;
import java.security.PrivilegedAction;

/**
 * The UIFrameFactory class is a helper for use with the ServiceUI
 *
 * @author Dennis Reedy
 */
public class UIFrameFactory implements JFrameFactory, Serializable {
    private String className;
    private URL[] exportURL;

    public UIFrameFactory(URL exportUrl, String className) {
        this.className = className;
        this.exportURL = new URL[]{exportUrl};
    }

    public UIFrameFactory(URL[] exportURL, String className) {
        this.className = className;
        this.exportURL = exportURL;
    }

    public JFrame getJFrame(Object roleObject) {
        if(!(roleObject instanceof ServiceItem)) {
            throw new IllegalArgumentException("ServiceItem required");
        }
        ClassLoader cl = ((ServiceItem)roleObject).service.getClass().getClassLoader();
        JFrame component=null;
        final URLClassLoader uiLoader = URLClassLoader.newInstance(exportURL, cl);
        final Thread currentThread = Thread.currentThread();

        final ClassLoader parentLoader = AccessController.doPrivileged(
            new PrivilegedAction<ClassLoader>() {
                public ClassLoader run() {
                    return(currentThread.getContextClassLoader());
                }
        }
        );

        try {
            AccessController.doPrivileged(
                new PrivilegedAction<Void>() {
                    public Void run() {
                        currentThread.setContextClassLoader(uiLoader);
                        return(null);
                    }
                }
            );

            try {
                Class clazz = uiLoader.loadClass(className);
                Constructor constructor = clazz.getConstructor(Object.class);
                Object instanceObj = constructor.newInstance(roleObject);
                component = (JFrame)instanceObj;
            } catch(Throwable t) {
                if(t.getCause() != null)
                    t = t.getCause();
                IllegalArgumentException e = 
                    new IllegalArgumentException("Unable to instantiate ServiceUI :"
                                                   + t.getClass().getName()
                                                   + ": "
                                                   + t.getLocalizedMessage());
                e.initCause(t);
                throw e;
            }
        } finally {
            AccessController.doPrivileged(
                new PrivilegedAction<Void>() {
                    public Void run() {
                        currentThread.setContextClassLoader(parentLoader);
                        return(null);
                    }
                }
            );
        }
        return(component);
    }
}






