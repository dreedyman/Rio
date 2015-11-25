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
package org.rioproject.start;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.SecureClassLoader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.StringTokenizer;


/**
 * This class provides useful utilities for creating and
 * manipulating class loaders.
 *
 * @author Sun Microsystems, Inc.
 * @author Dennis Reedy
 */
public final class ClassLoaderUtil {
    private static final Logger logger = LoggerFactory.getLogger(ClassLoaderUtil.class);

    // Private constructor to prevent instantiation
    private ClassLoaderUtil() {
    }

    public static URL[] getClasspathURLs(String classpath) throws IOException, MalformedURLException {
        StringTokenizer st = new StringTokenizer(classpath, File.pathSeparator);
        URL[] urls = new URL[st.countTokens()];
        if (logger.isTraceEnabled())
            logger.trace("Create URLs from {}", classpath);
        for(int i = 0; st.hasMoreTokens(); ++i) {
            String next = st.nextToken();
            File f = new File(next);
            if (logger.isTraceEnabled())
                logger.trace("Created {} from {}", f.getCanonicalFile().toURI().toURL(), next);
            urls[i] = f.getCanonicalFile().toURI().toURL();
        }

        return urls;
    }

    public static URL[] getCodebaseURLs(String codebase) throws MalformedURLException {
        StringTokenizer st = new StringTokenizer(codebase);
        URL[] urls = new URL[st.countTokens()];
        if (logger.isTraceEnabled())
            logger.trace("Create codebase from {}", codebase);
        for(int i = 0; st.hasMoreTokens(); ++i) {
            urls[i] = new URL(st.nextToken());
        }

        return urls;
    }

    /**
     * Utility method that retrieves the components making up the class loader
     * delegation tree for the current context class loader and returns each
     * in an <code>ArrayList</code>.
     *
     * @return an <code>List</code> instance in which each element of the
     * list is one of the components making up the current delegation
     * tree.
     */
    private static List<ClassLoader> getContextClassLoaderTreeList() {
        Thread curThread = Thread.currentThread();
        ClassLoader curClassLoader = curThread.getContextClassLoader();
        return getClassLoaderTreeList(curClassLoader);
    }//end getCurClassLoaderTree

    /**
     * Utility method that retrieves the components making up the class loader
     * delegation tree for the given <code>classloader</code> parameter and
     * returns them via an <code>ArrayList</code>.
     *
     * @param classloader <code>ClassLoader</code> instance whose delegation
     *                    tree is to be retrieved and returned
     * @return an <code>List</code> instance in which each element of the
     * list is one of the components making up the delegation tree
     * of the given class loader.
     */
    private static List<ClassLoader> getClassLoaderTreeList(ClassLoader classloader) {
        List<ClassLoader> loaderList = new ArrayList<ClassLoader>();
        while (classloader != null) {
            loaderList.add(classloader);
            classloader = classloader.getParent();
        }//end loop
        loaderList.add(null); //Append boot classloader
        Collections.reverse(loaderList);
        return loaderList;
    }//end getClassLoaderTree

    /**
     * Utility method that gets the class loader delegation tree for
     * the current context class loader. For each class loader in the tree,
     * this method displays the locations from which that class loader
     * will retrieve and load requested classes.
     * <p/>
     * This method can be useful when debugging problems related to the
     * receipt of exceptions such as <code>ClassNotFoundException</code>.
     */
    public static String getContextClassLoaderTree() {
        Thread curThread = Thread.currentThread();
        ClassLoader curClassLoader = curThread.getContextClassLoader();
        return getClassLoaderTree(curClassLoader);
    }

    /**
     * Utility method that gets the class loader delegation tree for
     * the given class loader. For each class loader in the tree, this
     * method formats the locations from which that class loader will
     * retrieve and load requested classes.
     * <p/>
     * This method can be useful when debugging problems related to the
     * receipt of exceptions such as <code>ClassNotFoundException</code>.
     *
     * @param classloader <code>ClassLoader</code> instance whose delegation tree is to be obtained
     */
    public static String getClassLoaderTree(ClassLoader classloader) {
        List<ClassLoader> loaderList = getClassLoaderTreeList(classloader);
        StringBuilder builder = new StringBuilder();
        builder.append("\n");

        builder.append("ClassLoader Tree has ").append(loaderList.size()).append(" levels\n");
        builder.append("  cl0 -- Boot ClassLoader\n");
        ClassLoader curClassLoader;
        for (int i = 1; i < loaderList.size(); i++) {
            builder.append("   |\n");
            curClassLoader = loaderList.get(i);
            builder.append("  cl" + i + " -- ClassLoader ").append(curClassLoader).append(": ");
            if (curClassLoader instanceof URLClassLoader) {
                URL[] urls = ((URLClassLoader) (curClassLoader)).getURLs();
                if (urls != null) {
                    builder.append(urls[0]);
                    for (int j = 1; j < urls.length; j++) {
                        builder.append(", ").append(urls[j]);
                    }
                } else {
                    builder.append("null search path");
                }
            } else {
                if (curClassLoader instanceof SecureClassLoader) {
                    builder.append("is instance of SecureClassLoader");
                } else {
                    builder.append("is unknown ClassLoader type");
                }
            }
            builder.append("\n");
        }
        builder.append("\n");
        return builder.toString();
    }

}
