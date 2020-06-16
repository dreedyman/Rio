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

import edu.emory.mathcs.util.classloader.URIClassLoader;
import net.jini.loader.ClassAnnotation;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.MalformedURLException;
import java.util.Properties;

/**
 * The ServiceClassLoader overrides getURLs(), ensuring all classes that need to
 * be annotated with specific location(s) are returned appropriately
 *
 * @author Dennis Reedy
 */
public class ServiceClassLoader extends URIClassLoader implements ClassAnnotation {
    private URI[] searchPath;
    /** The ClassAnnotator to use */
    private ClassAnnotator annotator;
    /** Meta data associated with the classloader */
    private Properties metaData = new Properties();

    /**
     * Constructs a new ServiceClassLoader for the specified URLs having the
     * given parent. The constructor takes two sets of URLs. The first set is
     * where the class loader loads classes from, the second set is what it
     * returns when getURLs() is called.
     * 
     * @param searchPath Array of URIs to search for classes
     * @param annotator Array of URLs to use for the codebase
     * @param parent Parent ClassLoader to delegate to
     */
    public ServiceClassLoader(URI[] searchPath,
                              ClassAnnotator annotator,
                              ClassLoader parent) {
        this(searchPath, annotator, parent, null);
    }

    /**
     * Constructs a new ServiceClassLoader for the specified URLs having the
     * given parent. The constructor takes two sets of URLs. The first set is
     * where the class loader loads classes from, the second set is what it
     * returns when getURLs() is called.
     *
     * @param searchPath Array of URIs to search for classes
     * @param annotator Array of URLs to use for the codebase
     * @param parent Parent ClassLoader to delegate to
     * @param metaData Optional meta data associated with the classloader
     */
    public ServiceClassLoader(URI[] searchPath,
                              ClassAnnotator annotator,
                              ClassLoader parent,
                              Properties metaData) {
        super(searchPath, parent);
        if(annotator==null)
            throw new IllegalArgumentException("annotator is null");
        this.annotator = annotator;
        this.searchPath = searchPath;
        if(metaData!=null)
            this.metaData.putAll(metaData);
    }

    /**
     * Get the {@link org.rioproject.loader.ClassAnnotator} created at construction
     * time
     *
     * @return The ClassAnnotator
     */
    public ClassAnnotator getClassAnnotator() {
        return(annotator);
    }
    
    /**
     * Get the meta data associated with this classloader
     *
     * @return A Properties object representing any meta data associated with
     * this classloader. A new Properties object is created each time
     */
    public Properties getMetaData() {
        return(new Properties(metaData));
    }

    /**
     * Add meta data associated with the classloader
     *
     * @param metaData Properties to associate to this classloader. If the
     * property already exists in the managed metaData, it will be replaced.
     * New properties will be added. A null parameter will be ignored.
     */
    public void addMetaData(Properties metaData) {
        if(metaData==null)
            return;
        this.metaData.putAll(metaData);
    }

    /**
     * Get the URLs to be used for class annotations as determined by the
     * {@link org.rioproject.loader.ClassAnnotator}
     */
    public URL[] getURLs() {
        return(annotator.getURLs());
    }
    
    /**
     * Get the search path of URLs for loading classes and resources
     *
     * @return The array of <code>URL[]</code> which corresponds to the search
     * path for the class loader; that is, the array elements are the locations
     * from which the class loader will load requested classes.
     *
     * @throws MalformedURLException If any of the URis cannot be transformed
     * to URLs
     */
    public URL[] getSearchPath() throws MalformedURLException {
        URL[] urls;
        if(searchPath != null) {
            urls = new URL[searchPath.length];
            for(int i=0; i<urls.length; i++)
                urls[i] = searchPath[i].toURL();
        } else {
            urls = new URL[0];
        }
        return (urls);
    }

    /**
     * Appends the specified URLs to the list of URLs to search for classes and
     * resources.
     *
     * @param urls The URLs to add
     */
    public void addURLs(URL[] urls) {
        URI[] uris = new URI[0];
        try {
            uris = getURIs(urls);
        } catch (URISyntaxException e) {
            e.printStackTrace();  
        }
        for (URI uri : uris)
            super.addURI(uri);
    }

    /**
     * Get the class annotations as determined by the
     * {@link org.rioproject.loader.ClassAnnotator}
     *
     * @see net.jini.loader.ClassAnnotation#getClassAnnotation
     */
    public String getClassAnnotation() {
        return (annotator.getClassAnnotation());
    }
    
    /**
     * Returns a String representation of this class loader.
     **/
    public String toString() {
        return(ServiceClassLoader.class.getName()+" "+
               "ClassPath : ["+ClassAnnotator.urisToPath(searchPath)+"] "+
               "Codebase : ["+getClassAnnotation()+"]");
    }

    /**
     * Convert a <code>URL[]</code> into a <code>URI[]</code>
     *
     * @param urls Array of URLs to convert
     *
     * @return Converted array of URIs
     *
     * @throws URISyntaxException If there are errors converting the URLs to
     * URIs
     */
    public static URI[] getURIs(URL[] urls) throws URISyntaxException {
        if(urls==null)
            throw new IllegalArgumentException("urls array must not be null");
        URI[] uris = new URI[urls.length];
        for(int i=0; i<urls.length; i++) {
            if(urls[i].getProtocol().equals("file")) {
                File f = new File(urls[i].getFile());
                if(f.getAbsolutePath().contains("%20") ) {
                    String path = f.getAbsolutePath().replaceAll("%20", " ");
                    f = new File(path);
                }
                uris[i] = f.toURI();
            } else {
                uris[i] = urls[i].toURI();
            }
        }
        return(uris);
    }

}
