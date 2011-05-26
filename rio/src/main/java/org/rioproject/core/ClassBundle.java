/*
 * Copyright 2008 the original author or authors.
 * Copyright 2005 Sun Microsystems, Inc.
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
package org.rioproject.core;

import com.sun.jini.start.ClassLoaderUtil;
import edu.emory.mathcs.util.classloader.URIClassLoader;
import org.rioproject.boot.ServiceClassLoader;
import org.rioproject.resources.util.PropertyHelper;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * ClassBundle provides a mechanism to define the resources needed to load and 
 * instantiate a class.
 *
 * @author Dennis Reedy
 */
public class ClassBundle implements Serializable {
    static final long serialVersionUID = 1L;
    /**
     * The classname
     */
    private String className;
    /**
     * The URL path used to load the class. The path will be applied to all
     * JARs in this ClassBundle
     */
    private String codebase;
    /**
     * Collection of jar names
     */
    private List<String> jarNames =
        Collections.synchronizedList(new ArrayList<String>());
    /**
     * An artifact ID
     */
    private String artifact;

    /**
     * Collection of shared components. 
     */
    private final Map<String, String[]> sharedComponents =
        Collections.synchronizedMap(new HashMap<String, String[]>());
    /**
     * A table of method names to Class[] objects
     */
    private Map<String, Object[]> methodObjectTable =
        Collections.synchronizedMap(new HashMap<String, Object[]>());
    private static Logger logger = Logger.getLogger(ClassBundle.class.getName());

    /**
     * Create a new ClassBundle
     */
    public ClassBundle() {
    }

    /**
     * Create a new ClassBundle
     *
     * @param className The className
     */
    public ClassBundle(String className) {
        if(className == null)
            throw new IllegalArgumentException("className is null");
        this.className = className;
    }


    /**
     * Create a new ClassBundle
     *
     * @param className The className
     * @param jarNames Array of Strings identifyng resource names used to load
     * the className
     * @param shComponents Map of class names and jar names to load the class
     * from. A shared component will be loaded by the common loader for all
     * services making it (and the resources it uses) available to all services
     * @param codebase The URL path used to load the class. The path will be
     * applied to all JARs in this ClassBundle
     */
    public ClassBundle(String className,
                       String[] jarNames,
                       Map<String, String[]> shComponents,
                       String codebase) {
        if(className == null)
            throw new IllegalArgumentException("className cannot be null");
        this.className = className;
        if(jarNames!=null)
            addJARs(jarNames);
        if(shComponents!=null)
            addSharedComponents(shComponents);
        this.codebase = codebase;
    }

    /**
     * Set the codebase used to load the class. The path will be applied to all
     * JARs in this ClassBundle
     *
     * @param codebase The codebase to set
     */
    public void setCodebase(String codebase) {
        if(codebase!=null) {
            this.codebase = codebase;
            if(!this.codebase.endsWith("/"))
                this.codebase = this.codebase+"/";
        }
    }

    /**
     * Get the codebase used to load the class.
     *
     * @return The codebase that has been set. If the codebase has properties
     * declared (in the form <tt>$[property]</tt>), return a formatted string
     * with the properties expanded. If there are no property elements
     * declared, return the original string.
     */
    public String getCodebase() {
        return(translateCodebase());
    }

    /**
     * Get the codebase without any translation
     *
     * @return The codebase that has been set
     */
    public String getRawCodebase() {
        return(codebase);
    }

    /**
     * Set the className
     * 
     * @param className The className, suitable for use with Class.forName()
     */
    public void setClassName(String className) {
        this.className = className;
    }

    /**
     * Get the className
     *
     * @return The className, suitable for use with Class.forName()
     */
    public String getClassName() {
        return (className);
    }

    /**
     * Get the artifact associated with the className
     *
     * @return The artifact associated with the className
     */
    public String getArtifact() {
        return artifact;
    }

    /**
     * Set the artifact
     *
     * @param artifact The artifact associated with the className
     */
    public void setArtifact(String artifact) {
        this.artifact = artifact;
    }

    /**
     * Set JARs to the ClassBundle.
     * 
     * @param jars Jar names to add
     */
    public void setJARs(String... jars) {
        addJARs(jars);
    }

    /**
     * Add JARs to the ClassBundle.
     *
     * @param jars Jar names to add. 
     */
    public void addJARs(String... jars) {
        if(jars == null)
            throw new IllegalArgumentException("jars cannot be null");
        for(String jar : jars)
            addJAR(jar);
    }

    /**
     * Add a JAR to the Collection of JAR resources.
     * 
     * @param jar Name of the JAR to add
     */
    public void addJAR(String jar) {
        if(jar == null)
            throw new IllegalArgumentException("jar cannot be null");
        if(!jarNames.contains(jar))
            jarNames.add(jar);
    }

    /**
     * Add a Map of shared components
     * 
     * @param m Map of shared components to add
     */
    public void addSharedComponents(Map<String, String[]> m) {
        if(m == null)
            return;
        sharedComponents.putAll(m);
    }

    /**
     * Get the JAR names.
     *
     * @return A String array of the JAR names. This method will return a new
     * array each time. If there are no JAR names, this method will return an
     * empty array
     */
    public String[] getJARNames() {
        return jarNames.toArray(new String[jarNames.size()]);
    }

    /**
     * Get the JAR resources
     * 
     * @return An URL array of the JARs that can be used as a classpath to
     * load the class. This method will return a new array each time. If
     * there are no JARs configured, this method will return an empty array.
     *
     * @throws MalformedURLException if the codebase has not been set, or if
     * the provided codebase contains an invalid protocol
     */
    public URL[] getJARs() throws MalformedURLException {
        return (urlsFromJARs(getJARNames()));
    }

    /**
     * Get shared component information
     *
     * @return A Map of the shared component information, transforming
     * configured string urls to URLs. This method will return a new Map
     * each time. If there are no shared components, this method will return
     * an empty Map
     *
     * @throws MalformedURLException If URLs cannot be created
     */
    public Map<String, URL[]> getSharedComponents() throws MalformedURLException {
        Map<String, URL[]> map = new HashMap<String, URL[]>();
        for(Map.Entry<String, String[]> entry : sharedComponents.entrySet()) {
            String className = entry.getKey();
            String[] jarNames = entry.getValue();
            URL[] urls = urlsFromJARs(jarNames);
            map.put(className, urls);
        }
        return map;
    }

    /**
     * Get raw shared component information
     * 
     * @return A Map of the shared component information, not performing the
     * transformation from string urls to URLs. This method will return a new Map
     * each time. If there are no shared components, this method will return
     * an empty Map
     */
    public Map<String, String[]> getRawSharedComponents()  {
        Map<String, String[]> map = new HashMap<String, String[]>();
        map.putAll(sharedComponents);
        return map;
    }

    /**
     * Add a method name and the parameters to use for when reflecting on
     * specified public member method of the class or interface represented by
     * this ClassBundle object. The array of parameter types will be determined
     * by the Class object for the Object types provided
     * 
     * @param methodName The public member method of the Class or interface
     * represented by this ClassBundle
     * @param parameters Array of Object parameters for use when reflecting on
     * the method
     */
    public void addMethod(String methodName, Object[] parameters) {
        if(methodName == null)
            throw new IllegalArgumentException("methodname is null");
        if(parameters == null) {
            methodObjectTable.put(methodName, null);
            return;
        }
        methodObjectTable.put(methodName, parameters);

    }

    /**
     * Get all method names to reflect on
     * 
     * @return Array of String method names to reflect on. If there
     * are no method names to reflect on this method will return an empty array
     */
    public String[] getMethodNames() {
        Set<String> keys = methodObjectTable.keySet();
        return (keys.toArray(new String[keys.size()]));
    }

    /**
     * Get the corresponding Class[] parameters to reflect on a method
     * 
     * @param methodName The name of the public method to reflect on
     * @return Array of Class objects to use when reflecting on the
     * public method
     */
    public Class[] getMethodClasses(String methodName) {
        Object[] args = getMethodObjects(methodName);
        Class[] classes = new Class[args.length];
        for(int i = 0; i < classes.length; i++) {
            classes[i] = args[i].getClass();
        }

        return classes;
    }

    /**
     * Get the corresponding Class[] parameters to reflect on a method
     * 
     * @param methodName The name of the public method to reflect on
     * @return Array of Object objects to use when reflecting on the
     * public method
     */
    public Object[] getMethodObjects(String methodName) {
        if(methodName == null)
            throw new IllegalArgumentException("methodName is null");
        return (methodObjectTable.get(methodName));
    }

    /**
     * Utility method to reflect on all added methods using an object
     * instantiated from the Class loaded by the ClassBundle
     * 
     * @param object An instantiated Object from the Class loaded by the
     * ClassBundle
     *
     * @throws Exception If there are errors running the known methods
     */
    public void runKnownMethods(Object object) throws Exception {
        String[] methods = getMethodNames();
        for (String method : methods) {
            Method m = object.getClass()
                .getMethod(method, getMethodClasses(method));
            m.invoke(object, getMethodObjects(method));
        }
    }

    /**
     * Override hashCode
     */
    public int hashCode() {
        int hc = 17;
        hc = 37*hc+className.hashCode();
        hc = 37*hc+getJARList().hashCode();
        return(hc);
    }

    /**
     * Override equals
     */
    public boolean equals(Object obj) {
        if(this == obj)
            return(true);
        if(!(obj instanceof ClassBundle))
            return(false);
        ClassBundle that = (ClassBundle)obj;
        if(this.className.equals(that.className)) {
            if(this.artifact!=null && that.artifact!=null) {
                return this.artifact.equals(that.artifact);
            } else {
                return this.getJARList().equals(that.getJARList());
            }
        }
        return(false);
    }

    /*
     * Get URLs for jarNames based on the codebase
     */
    private URL[] urlsFromJARs(String[] jarNames) throws MalformedURLException {
        URL[] urls = new URL[jarNames.length];
        StringBuilder sb = new StringBuilder();
        for(int i = 0; i < urls.length; i++) {
            if(i>0)
                sb.append(", ");
            sb.append(translateCodebase()).append(jarNames[i]);
            urls[i] = new URL(translateCodebase()+jarNames[i]);
        }
        if(logger.isLoggable(Level.FINE)) {
            logger.fine("Translated JARs=["+sb.toString()+"]");
        }
        return (urls);
    }

    /*
     * Return an ArrayList of JAR names
     */
    private ArrayList<String> getJARList() {
        ArrayList<String> list = new ArrayList<String>();
        String[] jars = getJARNames();
        list.addAll(Arrays.asList(jars));
        return(list);
    }

    @Override
    public String toString() {
        StringBuilder buffer = new StringBuilder();
        buffer.append("ClassName=").append(className).append("\n");
        buffer.append("Artifact=").append(artifact).append("\n");
        buffer.append("Codebase=").append(codebase).append("\n");
        String[] jars = getJARNames();
        if(jars.length>0) {
            buffer.append("Searchpath={");
            for(int i=0; i<jars.length; i++) {
                if(i>0)
                    buffer.append(", ");
                buffer.append(jars[i]);
            }
            buffer.append("}");
        }
        return(buffer.toString());
    }    

    /*
     * Custom serialization is needed, to provide backwards compatibility
     *
    private synchronized void writeObject(ObjectOutputStream oStream)
    throws IOException {
        oStream.defaultWriteObject();
        if(jarResources.size()==0 && codebase!=null) {
            URL[] urls = urlsFromJARs(getJARNames());
            jarResources.addAll(Arrays.asList(urls));
        }
    }
    */
    /*
     * Custom deserialization is needed.
     *
    private void readObject(ObjectInputStream oStream)
    throws IOException, ClassNotFoundException {
        oStream.defaultReadObject();
        if(jarNames==null)
            jarNames = new ArrayList<String>();
        if(jarNames.size()==0) {
            URL[] urls = jarResources.toArray(new URL[jarResources.size()]);
            if(urls.length>0) {
                String exportCodebase = urls[0].toExternalForm();
                if(exportCodebase.indexOf(".jar") != -1) {
                    int index = exportCodebase.lastIndexOf('/');
                    if(index != -1) {
                        exportCodebase = exportCodebase.substring(0, index+1);
                        setCodebase(exportCodebase);
                    }
                }
                for (URL url : urls) {
                    String jar = getJarPart(url);
                    jarNames.add(jar);
                }
            }
        }
    }
    */

    /*
     * Get the jar part from the url
     /
    private String getJarPart(URL url) {
        String s = url.toExternalForm();
        int ndx = s.lastIndexOf("/");
        if(ndx == -1)
            return ("");
        return (s.substring(ndx+1));
    }
    */

    /*
     * Expand any properties in the codebase String. Properties are declared
     * with the pattern of : <code>$[property]</code>
     *
     * @return If the codebase has properties declared (in the form
     * <code>$[property]</code>), return a formatted string with the
     * properties expanded. If there are no property elements declared, return
     * the original string.
     *
     * @throws IllegalArgumentException If a property value cannot be obtained
     * an IllegalArgument will be thrown
     */
    private String translateCodebase() {
        if(codebase==null)
            return(codebase);
        String translated =
                PropertyHelper.expandProperties(codebase,
                                               PropertyHelper.RUNTIME);
        if(System.getProperty("os.name").startsWith("Win") &&
           translated.startsWith("file://")) {
            translated = "file:/"+translated.substring(7);
        }
        return(translated);
    }    

    /**
     * Merge two ClassBundles
     *
     * @param bundles ClassBundle instances to merge
     *
     * @return A merged ClassBundle.
     *
     * @throws IllegalArgumentException For all ClassBundles that have a
     * non-null classname, that classname must be equal. If this is not the
     * case then an IllegalArgumentException is thrown.
     */
    public static ClassBundle merge(ClassBundle... bundles) {
        String className = null;
        for(ClassBundle bundle : bundles) {
            if(bundle.getClassName()!=null) {
                if(className==null)
                    className = bundle.getClassName();
                else if(!className.equals(bundle.getClassName()))
                    throw new IllegalArgumentException("bundles must have same " +
                                                       "classname");
            }
        }

        ClassBundle cb = new ClassBundle();
        for(ClassBundle bundle : bundles) {
            cb.setArtifact(bundle.getArtifact());
            cb.setJARs(bundle.getJARNames());
            cb.addSharedComponents(bundle.sharedComponents);
            cb.setCodebase(bundle.getCodebase());
            cb.setClassName(bundle.getClassName());
            cb.methodObjectTable.putAll(bundle.methodObjectTable);
        }
        return cb;
    }

}
