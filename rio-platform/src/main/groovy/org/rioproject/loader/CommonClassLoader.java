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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.net.URLClassLoader;

/**
 * The CommonClassLoader is created by the <code>RioServiceDescriptor</code> when starting a Rio
 * service and contains common declared platform JARs to be made available to its 
 * children.
 * 
 * <p>The CommonClassLoader enables a platform oriented framework conducive towards 
 * creating a layered product. The resulting capabilities allow the declaration of 
 * JARs that are added to the CommonClassLoader, making the classes accessible by 
 * all ClassLoader instances which delegate to the CommonClassLoader. In this 
 * fashion a platform can be declared, initialized and made available.
 *
 @author Dennis Reedy
 */
public final class CommonClassLoader extends URLClassLoader {
    private static final String COMPONENT = "org.rioproject.loader";
    private static Logger logger = LoggerFactory.getLogger(COMPONENT);
    private static CommonClassLoader instance;

	/**
	 * Create a CommonClassLoader
     *
     * @param parent The parent ClassLoader
	 */
    private CommonClassLoader(final ClassLoader parent) {
        super(new URL[0], parent);
    }
    
    /**
     * Get an instance of the CommonCLassLoader
     * 
     * @return The CommonClassLoader
     */
    public static synchronized CommonClassLoader getInstance() {
        if(instance==null) {
            instance = new CommonClassLoader(ClassLoader.getSystemClassLoader());
        }
        return(instance);
    }    
    
    /**
     * Override getURLs to ensure when an Object is marshalled its
     * annotation is correct
     * 
     * @return Array of URLs
     */
    public URL[] getURLs() {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        URL[] urls = doGetURLs(cl);
        if(logger.isTraceEnabled()) {
            StringBuilder buffer = new StringBuilder();
            for(int i=0; i<urls.length; i++) {
                if(i>0)
                    buffer.append(", ");
                buffer.append(urls[i].toExternalForm());
            }
            logger.trace("Context ClassLoader={} URLs={}", cl.toString(), buffer.toString());
        } 
        return(urls);
    }

    /**
     * Get the URLs, ensuring when an Object is marshalled the annotation is
     * correct
     *
     * @param cl The current context ClassLoader
     *
     * @return Array of URLs
     */
    private URL[] doGetURLs(final ClassLoader cl) {
        URL[] urls;
        if(cl.equals(this)) {
            urls = super.getURLs();
        } else {
            if(cl instanceof ServiceClassLoader) {
                ServiceClassLoader scl = (ServiceClassLoader)cl;
                urls = scl.getURLs();
            } else {
                urls = super.getURLs();
            }
        }
        return(urls);
    }

    /**
     * Add common JARs
     * 
     * @param jars Array of URLs
     */
    public void addCommonJARs(final URL[] jars) {
        if(jars==null)
            return;
        for (URL jar : jars) {
            if (!hasURL(jar))
                addURL(jar);
        }
    }

    /*
     * Check if the URL already is registered
     */
    private boolean hasURL(final URL url) {
        URL[] urls = getURLs();
        for (URL url1 : urls) {
            if (url1.equals(url))
                return (true);
        }
        return(false);
    }

    /**
     * Returns a string representation of this class loader.
     **/
    public String toString() {
        URL[] urls = doGetURLs(this);
        StringBuilder buffer = new StringBuilder();
        for(int i=0; i<urls.length; i++) {
            if(i>0)
                buffer.append(" ");
            buffer.append(urls[i].toExternalForm());
        }
        return(super.toString()+" ["+buffer.toString()+"]");
    }    
} 
