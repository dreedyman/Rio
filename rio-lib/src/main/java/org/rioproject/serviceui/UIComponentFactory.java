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
package org.rioproject.serviceui;

import net.jini.core.lookup.ServiceItem;
import net.jini.lookup.ui.factory.JComponentFactory;

import javax.swing.*;
import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The UIComponentFactory class is a helper for use with the ServiceUI
 *
 * @author Dennis Reedy
 */
public class UIComponentFactory implements JComponentFactory, Serializable {
    static final long serialVersionUID = 1L;
    private final String className;
    private URL[] exportURL;
    private String urlString;

    public UIComponentFactory(URL exportUrl, String className) {
        this.className = className;
        this.exportURL = new URL[]{exportUrl};
    }

    public UIComponentFactory(String urlString, String className) {
        this.className = className;
        this.urlString = urlString;
    }

    public UIComponentFactory(URL[] exportURL, String className) {
        this.className = className;
        this.exportURL = exportURL;
    }

    public JComponent getJComponent(Object roleObject) {
        if (!(roleObject instanceof ServiceItem)) {
            throw new IllegalArgumentException("ServiceItem required");
        }
        ClassLoader cl = ((ServiceItem)roleObject).service.getClass().getClassLoader();
        JComponent component;
        try {
            if (urlString != null) {
                exportURL = expandUrlString();
            }
            StringBuilder b = new StringBuilder();
            for (URL url : exportURL) {
                if (b.length() > 0) {
                    b.append(":");
                }
                b.append(url.toExternalForm());
            }
            System.out.println("Load: " + className + ", from: " + b.toString());
            final URLClassLoader uiLoader = new URLClassLoader(exportURL, cl);
            final Thread currentThread = Thread.currentThread();
            final ClassLoader parentLoader = 
                AccessController.doPrivileged((PrivilegedAction<ClassLoader>) currentThread::getContextClassLoader);
            try {
                AccessController.doPrivileged((PrivilegedAction<Void>) () -> {
                    currentThread.setContextClassLoader(uiLoader);
                    return null;
                });
                Class<?> clazz  ;
                try {
                    clazz = uiLoader.loadClass(className);
                } catch (ClassNotFoundException ex) {
                    ex.printStackTrace();
                    throw ex;
                }
                Constructor<?> constructor = clazz.getConstructor(Object.class);
                Object instanceObj = constructor.newInstance(roleObject);
                component = (JComponent)instanceObj;
            } finally {
                AccessController.doPrivileged((PrivilegedAction<Void>) () -> {
                    currentThread.setContextClassLoader(parentLoader);
                    return null;
                });
            }
        } catch (Throwable t) {
            if (t.getCause() != null) {
                t = t.getCause();
            }
            throw new IllegalArgumentException("Unable to instantiate ServiceUI :" + className, t);
        }
        return component;
    }

    URL[] expandUrlString() throws MalformedURLException {
        URL[] urls = new URL[1];
        String regex = "\\$\\{(.+?)\\}";
        Pattern pattern = Pattern.compile(regex, Pattern.DOTALL);
        Matcher matcher = pattern.matcher(urlString);
        StringBuilder s = new StringBuilder();
        int last = 0;
        while (matcher.find()) {
            s.append(urlString,
                     last,
                     matcher.start());
            String match = urlString.substring(matcher.start(), matcher.end());
            last = matcher.end();
            String property = match.substring(2, match.length() - 1);
            s.append(System.getProperty(property));
        }
        urls[0] = new URL(s.toString());
        return urls;
    }
}
