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

import org.rioproject.logging.WrappedLogger;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

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
 * <p>The ClassLoader hierarchy when starting a Rio service is as follows :
 * <br>
<table cellpadding="2" cellspacing="2" border="0"
 style="text-align: left; width: 50%;">
  <tbody>
    <tr>
      <td style="vertical-align: top;">
        <pre>
                  AppCL
                    |
            CommonClassLoader (http:// URLs of common JARs)
                   |
            +-------+-------+----...---+
            |               |          |
        Service-1CL   Service-2CL  Service-nCL
        </pre>
      </td>
    </tr>
  </tbody>
</table>
 <span style="font-weight: bold;">AppCL</span> - Contains the main()
class of the container. Main-Class in
manifest points to <span style="font-family: monospace;">com.sun.jini.start.ServiceStarter</span><br>
Classpath:&nbsp; rio-start.jar, start.jar, jsk-platform.jar<br>
Codebase: none<br>
<br>
<span style="font-weight: bold;">CommonClassLoader</span> - Contains
the common Rio and
Jini technology classes (and other declared common platform JARs) to be
made available to its children.<br>
Classpath: Common JARs such as rio-lib.jar<br>
Codebase: Context dependent. The codebase returned is the codebase of
the specific child CL that is the current context of the request.<br>
<br>
<span style="font-weight: bold;">Service-nCL</span> - Contains the
service specific implementation classes.<br>
Classpath: serviceImpl.jar<br>
Codebase: "serviceX-dl.jar rio-api.jar jsk-lib-dl.jar"<br>

 @author Dennis Reedy
 */
public class CommonClassLoader extends URLClassLoader {
    private static final String COMPONENT = "org.rioproject.loader";
    private static WrappedLogger logger = WrappedLogger.getLogger(COMPONENT);
    private static final Map<String, URL[]> components = new HashMap<String, URL[]>();
    private static CommonClassLoader instance;

	/**
	 * Create a CommonClassLoader
     *
     * @param parent The parent ClassLoader
	 */
    private CommonClassLoader(ClassLoader parent) {
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
        if(logger.isLoggable(Level.FINEST)) {
            StringBuilder buffer = new StringBuilder();
            for(int i=0; i<urls.length; i++) {
                if(i>0)
                    buffer.append(", ");
                buffer.append(urls[i].toExternalForm());
            }
            logger.finest("Context ClassLoader=%s URLs=%s", cl.toString(), buffer.toString());
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
    private URL[] doGetURLs(ClassLoader cl) {
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
     * Test whether a named component (Class) exists.
     *
     * @param name The component name
     * @return true If the requested component can be located, false
     * otherwise.
     */
	public boolean testComponentExistence(String name) {
		boolean exists = false;
		/* First check if the class is registered in the component Map */
		synchronized(components) {
			if(components.containsKey(name))
				exists = true;
		}		
		if(!exists) {					
			/* Although not registered, it may be in our search path, 
			 * so try and load the class */
			try {
				loadClass(name);
				exists = true;
            } catch(Throwable t) {
                logger.finest("Failed to find class %s", name);
            }
		}        
		return(exists);
	}
    
    /**
     * Add common JARs
     * 
     * @param jars Array of URLs
     */
    public void addCommonJARs(URL[] jars) {
        if(jars==null)
            return;
        for (URL jar : jars) {
            if (!hasURL(jar))
                addURL(jar);
        }
    }

	/**
     * Registers a class name, and the code source which is used as the search
     * path to load the class.
     *
     * @param name The name of the class
     * @param urls Codebase for the class identified by the name parameter
     */
	public void addComponent(String name, URL[] urls) {
		boolean added = false;
		boolean toAdd = false;
        boolean toReplace = false;
		synchronized (components) {
			if(components.containsKey(name)) {
			    /* Check if codebase matches */
                URL[] fetched = components.get(name);
                if(fetched.length==urls.length) {                    
                    for(int i=0; i<fetched.length; i++) {
                        /* There is a difference, replace */
                        if(!fetched[i].equals(urls[i])) {
                            toReplace = true;
                            break;
                        }
                    }
                } else {
                    /* Since the codebase is different, replace the entry */
                    toReplace = true;
                }
				
			} else {
                /* Not found, add the entry */
                toAdd = true;
			}
            
			if(toAdd || toReplace) {
                added = true;
                if(logger.isLoggable(Level.FINEST)) {
                    String action = (toAdd?"Adding":"Replacing");
                    logger.finest(action+" %s Component %s ", action, name);
                }
                components.put(name, urls);
            } else {
                if(logger.isLoggable(Level.FINEST)) {
                    StringBuilder buffer = new StringBuilder();
                    URL[] codebase = components.get(name);
                    for(int i=0; i<codebase.length; i++) {
                        if(i>0)
                            buffer.append(":");
                        buffer.append(codebase[i].toExternalForm());
                    }
                    logger.finest("Component %s has already been registered with a codebase of %s",
                                  name, buffer.toString());
                }
            }
		}
		if(added) {
            for (URL url : urls) {
                if (!hasURL(url)) {
                    addURL(url);
                }
            }
		}        
	}
    
    /*
     * Check if the URL already is registered
     */
    private boolean hasURL(URL url) {
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
