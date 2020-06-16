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
import net.jini.lookup.ui.factory.JDialogFactory;

import javax.swing.*;
import java.awt.*;
import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.AccessController;
import java.security.PrivilegedAction;

/**
 * The UIDialogFactory class is a helper for use with the ServiceUI
 *
 * @author Dennis Reedy
 */
public class UIDialogFactory implements JDialogFactory, Serializable {
    private static final long serialVersionUID = 1L;
    private String className;
    private URL[] exportURL;

    public UIDialogFactory(URL exportUrl, String className) {
        this.className = className;
        this.exportURL = new URL[]{exportUrl};
    }

    public UIDialogFactory(URL[] exportURL, String className) {
        this.className = className;
        this.exportURL = exportURL;
    }

    public JDialog getJDialog(Object roleObject, Dialog dialog, boolean modal) {
        if(!(roleObject instanceof ServiceItem)) {
            throw new IllegalArgumentException("ServiceItem required");
        }
        JDialog component;
        try {
            component = (JDialog)loadComponent(roleObject, dialog);
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
        return(component);
    }

    public JDialog getJDialog(Object roleObject, Dialog dialog) {
        if(!(roleObject instanceof ServiceItem)) {
            throw new IllegalArgumentException("ServiceItem required");
        }
        JDialog component;
        try {
            component = (JDialog)loadComponent(roleObject, dialog);
        } catch(Exception e) {
            e.printStackTrace();
            throw new IllegalArgumentException("Unable to instantiate ServiceUI :"+e.getClass().getName()+": "+e.getLocalizedMessage());
        }
        return(component);
    }

    public JDialog getJDialog(Object roleObject, Frame frame, boolean modal) {
        if(!(roleObject instanceof ServiceItem)) {
            throw new IllegalArgumentException("ServiceItem required");
        }
        JDialog component;
        try {
            component = (JDialog)loadComponent(roleObject, frame);
        } catch(Exception e) {
            e.printStackTrace();
            throw new IllegalArgumentException("Unable to instantiate ServiceUI :"+e.getClass().getName()+": "+e.getLocalizedMessage());
        }
        return(component);
    }

    public JDialog getJDialog(Object roleObject, Frame frame) {
        if(!(roleObject instanceof ServiceItem)) {
            throw new IllegalArgumentException("ServiceItem required");
        }
        JDialog component;
        try {
            component = (JDialog)loadComponent(roleObject, frame);
        } catch(Exception e) {
            e.printStackTrace();
            throw new IllegalArgumentException("Unable to instantiate ServiceUI :"+e.getClass().getName()+": "+e.getLocalizedMessage());
        }
        return(component);
    }

    public JDialog getJDialog(Object roleObject) {
        if(!(roleObject instanceof ServiceItem)) {
            throw new IllegalArgumentException("ServiceItem required");
        }
        JDialog component;
        try {
            component = (JDialog)loadComponent(roleObject, null);
        } catch(Exception e) {
            e.printStackTrace();
            throw new IllegalArgumentException("Unable to instantiate ServiceUI :"+e.getClass().getName()+": "+e.getLocalizedMessage());
        }
        return(component);
    }

    private Object loadComponent(Object roleObject, Object param) throws Exception {
        ClassLoader cl = ((ServiceItem)roleObject).service.getClass().getClassLoader();
        Object component;
        Object instanceObj = null;
        final URLClassLoader uiLoader = URLClassLoader.newInstance(exportURL, cl);
        final Thread currentThread = Thread.currentThread();
        final ClassLoader parentLoader =
            AccessController.doPrivileged(
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

            Class clazz = uiLoader.loadClass(className);
            Constructor constructor  ;
            
            if(param==null) {
                constructor = clazz.getConstructor(Object.class);
                instanceObj = constructor.newInstance(roleObject);
            } else if(param instanceof Dialog) {
                constructor = clazz.getConstructor(Object.class, Dialog.class);
                instanceObj = constructor.newInstance(roleObject, param);
            } else {
                constructor = clazz.getConstructor(Object.class, Frame.class);
                instanceObj = constructor.newInstance(roleObject, param);
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
        component = instanceObj;
        return(component);
    }
}






