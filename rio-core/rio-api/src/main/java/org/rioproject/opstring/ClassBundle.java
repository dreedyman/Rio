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
package org.rioproject.opstring;

import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * ClassBundle provides a mechanism to define the resources needed to load and 
 * instantiate a class.
 *
 * @author Dennis Reedy
 */
public class ClassBundle implements Serializable {
    @SuppressWarnings("unused")
    static final long serialVersionUID = 2L;
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
    private final List<String> jarNames = Collections.synchronizedList(new ArrayList<String>());
    /**
     * An artifact ID.
     */
    private String artifact;

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
    public ClassBundle(final String className) {
        if(className == null)
            throw new IllegalArgumentException("className is null");
        this.className = className;
    }

    /**
     * Create a new ClassBundle
     *
     * @param className The className
     * @param jarNames Array of Strings identifying resource names used to load
     * the className
     * @param codebase The URL path used to load the class. The path will be
     * applied to all JARs in this ClassBundle
     */
    public ClassBundle(final String className, final String[] jarNames, final String codebase) {
        if(className == null)
            throw new IllegalArgumentException("className cannot be null");
        if(jarNames==null)
            throw new IllegalArgumentException("jarNames cannot be null");
        this.className = className;
        for(String jar : jarNames) {
            if(jar!=null) {
                this.jarNames.add(jar);
            }
        }
        this.codebase = codebase;
    }

    /**
     * Set the codebase used to load the class. The path will be applied to all
     * JARs in this ClassBundle
     *
     * @param codebase The codebase to set
     */
    public void setCodebase(final String codebase) {
        this.codebase = codebase;
        if(this.codebase!=null) {
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
    public void setClassName(final String className) {
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
    public void setArtifact(final String artifact) {
        this.artifact = artifact;
    }

    /**
     * Set JARs to the ClassBundle.
     * 
     * @param jars Jar names to set
     */
    public void setJARs(final String... jars) {
        jarNames.clear();
        addJARs(jars);
    }

    /**
     * Add JARs to the ClassBundle.
     *
     * @param jars Jar names to add. 
     */
    public void addJARs(final String... jars) {
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
    public void addJAR(final String jar) {
        if(jar == null)
            throw new IllegalArgumentException("jar cannot be null");
        if(!jarNames.contains(jar))
            jarNames.add(jar);
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
     * Override hashCode
     */
    public int hashCode() {
        int hc = 17;
        hc = 37*hc+(className!=null?className.hashCode():0);
        if(!getJARList().isEmpty()) {
            hc = 37*hc+getJARList().hashCode();
        } else {
            hc = 37*hc+(artifact!=null?artifact.hashCode():0);
        }
        return(hc);
    }

    /**
     * Override equals
     */
    public boolean equals(final Object obj) {
        if(this == obj)
            return(true);
        if(!(obj instanceof ClassBundle))
            return(false);
        ClassBundle that = (ClassBundle)obj;
        if(this.className!=null && that.className!=null) {
            if(this.className.equals(that.className)) {
                if(this.artifact!=null && that.artifact!=null) {
                    return this.artifact.equals(that.artifact);
                } else {
                    return this.getJARList().equals(that.getJARList());
                }
            }
        }
        return(false);
    }

    /*
     * Get URLs for jarNames based on the codebase
     */
    private URL[] urlsFromJARs(final String[] jarNames) throws MalformedURLException {
        URL[] urls = new URL[jarNames.length];
        for(int i = 0; i < urls.length; i++) {
            urls[i] = new URL(translateCodebase()+jarNames[i]);
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
        buffer.append("ClassName=").append(className).append(", ");
        buffer.append("Artifact=").append(artifact).append(", ");
        buffer.append("Codebase=").append(codebase).append(", ");
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
        if(System.getProperty("os.name").startsWith("Win") && codebase.startsWith("file://")) {
            codebase = "file:/"+codebase.substring(7);
        }
        return(codebase);
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
    @SuppressWarnings("unused")
    public static ClassBundle merge(final ClassBundle... bundles) {
        String className = null;
        for(ClassBundle bundle : bundles) {
            if(bundle.getClassName()!=null) {
                if(className==null)
                    className = bundle.getClassName();
                else if(!className.equals(bundle.getClassName()))
                    throw new IllegalArgumentException("bundles must have same classname");
            }
        }

        ClassBundle cb = new ClassBundle();
        for(ClassBundle bundle : bundles) {
            cb.setArtifact(bundle.getArtifact());
            cb.addJARs(bundle.getJARNames());
            cb.setCodebase(bundle.getCodebase());
            cb.setClassName(bundle.getClassName());
        }
        return cb;
    }

}
