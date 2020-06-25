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
package edu.emory.mathcs.util.classloader;

import java.io.File;
import java.io.IOException;
import java.net.*;
import java.security.*;
import java.util.Enumeration;
import java.util.jar.Attributes;
import java.util.jar.Attributes.Name;
import java.util.jar.Manifest;

/**
 * Equivalent of java.net.URLClassloader but without bugs related to ill-formed
 * URLs and with customizable JAR caching policy. The standard URLClassLoader
 * accepts URLs containing spaces and other characters which are forbidden in
 * the URI syntax, according to the RFC 2396.
 * As a workaround to this problem, Java escapes and un-escapes URLs in various
 * arbitrary places; however, this is inconsistent and leads to numerous
 * problems with URLs referring to local files with spaces in the path. SUN
 * acknowledges the problem, but refuses to modify the behavior for
 * compatibility reasons; see Java Bug Parade 4273532, 4466485.
 * <p>
 * Additionally, the JAR caching policy used by
 * URLClassLoader is system-wide and inflexible: once downloaded JAR files are
 * never re-downloaded, even if one creates a fresh instance of the class
 * loader that happens to have the same URL in its search path. In fact, that
 * policy is a security vulnerability: it is possible to crash any URL class
 * loader, thus affecting potentially separate part of the system, by creating
 * URL connection to one of the URLs of that class loader search path and
 * closing the associated JAR file.
 * See Java Bug Parade 4405789, 4388666, 4639900.
 * <p>
 * This class avoids these problems by 1) using URIs instead of URLs for the
 * search path (thus enforcing strict syntax conformance and defining precise
 * escaping semantics), and 2) using custom URLStreamHandler which ensures
 * per-classloader JAR caching policy.
 *
 * @author Dawid Kurzyniec
 * @version 1.0
 *
 * @see java.io.File#toURL
 * @see java.io.File#toURI
 */
@SuppressWarnings("unchecked")
public class URIClassLoader extends URLClassLoader {

    final URIResourceFinder finder;
    final AccessControlContext acc;

    /**
     * Creates URIClassLoader with the specified search path.
     * @param uris the search path
     */
    public URIClassLoader(URI[] uris) {
        this(uris, (URLStreamHandler)null);
    }

    /**
     * Creates URIClassLoader with the specified search path.
     * @param uris the search path
     * @param jarHandler stream handler for JAR files; implements caching policy
     */
    public URIClassLoader(URI[] uris, URLStreamHandler jarHandler) {
        super(new URL[0]);
        this.finder = new URIResourceFinder(uris, jarHandler);
        this.acc = AccessController.getContext();
    }

    /**
     * Creates URIClassLoader with the specified search path and parent class
     * loader.
     * @param uris the search path
     * @param parent the parent class loader.
     */
    public URIClassLoader(URI[] uris, ClassLoader parent) {
        this(uris, parent, null);
    }

    /**
     * Creates URIClassLoader with the specified search path and parent class
     * loader.
     * @param uris the search path
     * @param parent the parent class loader.
     * @param jarHandler stream handler for JAR files; implements caching policy
     */
    public URIClassLoader(URI[] uris, ClassLoader parent, URLStreamHandler jarHandler) {
        super(new URL[0], parent);
        this.finder = new URIResourceFinder(uris, jarHandler);
        this.acc = AccessController.getContext();
    }

    /**
     * Add specified URI at the end of the search path.
     * @param uri the URI to add
     */
    protected void addURI(URI uri) {
        finder.addURI(uri);
    }

    /**
     * Add specified URL at the end of the search path.
     * @param url the URL to add
     * @deprecated use addURI
     */
    protected void addURL(URL url) {
        finder.addURI(URI.create(url.toExternalForm()));
    }

    public URL[] getURLs() {
        return finder.getUrls().clone();
    }

//    public URL[] getAllResolvedURLs() {
//        return finder.getResolvedSearchPath();
//    }

    /**
     * Finds and loads the class with the specified name.
     *
     * @param name the name of the class
     * @return the resulting class
     * @exception ClassNotFoundException if the class could not be found
     */
    protected Class<?> findClass(final String name)
        throws ClassNotFoundException
    {
        try {
            return (Class<?>)
                AccessController.doPrivileged((PrivilegedExceptionAction<?>) () -> {
                    String path = name.replace('.', '/').concat(".class");
                    ResourceHandle h = finder.getResource(path);
                    if (h != null) {
                        try {
                            return defineClass(name, h);
                        } catch (IOException e) {
                            throw new ClassNotFoundException(name, e);
                        }
                    } else {
                        throw new ClassNotFoundException(name);
                    }
                }, acc);
        } catch (PrivilegedActionException pae) {
            throw (ClassNotFoundException)pae.getException();
        }
    }

    protected Class<?> defineClass(String name, ResourceHandle h) throws IOException {
        int i = name.lastIndexOf('.');
        URL url = h.getCodeSourceURL();
        if (i != -1) { // check package
            String pkgname = name.substring(0, i);
            // check if package already loaded
            Package pkg = getPackage(pkgname);
            Manifest man = h.getManifest();
            if (pkg != null) {
                // package found, so check package sealing
                boolean ok;
                if (pkg.isSealed()) {
                    // verify that code source URLs are the same
                    ok = pkg.isSealed(url);
                } else {
                    // make sure we are not attempting to seal the package
                    // at this code source URL
                    ok = (man == null) || !isSealed(pkgname, man);
                }
                if (!ok) {
                    throw new SecurityException("sealing violation: " + name);
                }
            } else { // package not yet defined
                if (man != null) {
                    definePackage(pkgname, man, url);
                } else {
                    definePackage(pkgname, null, null, null, null, null, null, null);
                }
            }
        }

        // now read the class bytes and define the class
        byte[] b = h.getBytes();
        java.security.cert.Certificate[] certs = h.getCertificates();
        CodeSource cs = new CodeSource(url, certs);
        return defineClass(name, b, 0, b.length, cs);
    }

    /**
     * returns true if the specified package name is sealed according to the
     * given manifest.
     */
    private boolean isSealed(String name, Manifest man) {
        String path = name.replace('.', '/').concat("/");
        Attributes attr = man.getAttributes(path);
        String sealed = null;
        if (attr != null) {
            sealed = attr.getValue(Name.SEALED);
        }
        if (sealed == null) {
            if ((attr = man.getMainAttributes()) != null) {
                sealed = attr.getValue(Name.SEALED);
            }
        }
        return "true".equalsIgnoreCase(sealed);
    }

    /**
     * Finds the resource with the specified name.
     *
     * @param name the name of the resource
     * @return a <code>URL</code> for the resource, or <code>null</code>
     *         if the resource could not be found.
     */
    public URL findResource(final String name) {
        return
            (URL) AccessController.doPrivileged((PrivilegedAction<?>) () -> finder.findResource(name), acc);
    }

    /**
     * Returns an Enumeration of URLs representing all of the resources
     * having the specified name.
     *
     * @param name the resource name
     * @return an <code>Enumeration</code> of <code>URL</code>s
     */
    public Enumeration findResources(final String name) {
        return
            (Enumeration) AccessController.doPrivileged((PrivilegedAction<?>) () -> finder.findResources(name), acc);
    }

    /**
     * Returns the absolute path name of a native library. The VM
     * invokes this method to locate the native libraries that belong
     * to classes loaded with this class loader. If this method returns
     * <code>null</code>, the VM searches the library along the path
     * specified as the <code>java.library.path</code> property.
     * This method invoke {@link #getLibraryHandle} method to find handle
     * of this library. If the handle is found and its URL protocol is "file",
     * the system-dependent absolute library file path is returned.
     * Otherwise this method returns null. <p>
     *
     * Subclasses can override this method to provide specific approaches
     * in library searching.
     *
     * @param      libname   the library name
     * @return     the absolute path of the native library
     * @see        System#loadLibrary(String)
     * @see        System#mapLibraryName(String)
     */
    protected String findLibrary(String libname) {
        ResourceHandle md = getLibraryHandle(libname);
        if (md == null) return null;
        URL url = md.getURL();
        if (!"file".equals(url.getProtocol())) return null;
        return new File(URI.create(url.toString())).getPath();
    }
//
//    /**
//     * Gets the ResourceHandle object for the specified loaded class.
//     *
//     * @param clazz the Class
//     * @return the ResourceHandle of the Class
//     */
//    protected ResourceHandle getClassHandle(Class clazz)
//    {
//        return null;
//    }

    /**
     * Finds the ResourceHandle object for the class with the specified name.
     * Unlike <code>findClass()</code>, this method does not load the class.
     *
     * @param name the name of the class
     * @return the ResourceHandle of the class
     */
    protected ResourceHandle getClassHandle(final String name) {
        String path = name.replace('.', '/').concat(".class");
        return getResourceHandle(path);
    }

    /**
     * Finds the ResourceHandle object for the resource with the specified name.
     *
     * @param name the name of the resource
     * @return the ResourceHandle of the resource
     */
    protected ResourceHandle getResourceHandle(final String name)
    {
        return
            (ResourceHandle) AccessController.doPrivileged((PrivilegedAction<?>) () -> finder.getResource(name), acc);
    }

    /**
     * Finds the ResourceHandle object for the native library with the specified
     * name.
     * The library name must be '/'-separated path. The last part of this
     * path is substituted by its system-dependent mapping (using
     * {@link System#mapLibraryName(String)} method). Next, the
     * <code>ResourceFinder</code> is used to look for the library as it
     * was ordinary resource. <p>
     *
     * Subclasses can override this method to provide specific approaches
     * in library searching.
     *
     * @param name the name of the library
     * @return the ResourceHandle of the library
     */
    protected ResourceHandle getLibraryHandle(final String name) {
        int idx = name.lastIndexOf('/');
        String path;
        String simplename;
        if (idx == -1) {
            path = "";
            simplename = name;
        } else if (idx == name.length()-1) { // name.endsWith('/')
            throw new IllegalArgumentException(name);
        } else {
            path = name.substring(0, idx+1); // including '/'
            simplename = name.substring(idx+1);
        }
        return getResourceHandle(path + System.mapLibraryName(simplename));
    }

    /**
     * Returns an Enumeration of ResourceHandle objects representing all of the
     * resources having the specified name.
     *
     * @param name the name of the resource
     * @return the ResourceHandle of the resource
     */
    protected Enumeration getResourceHandles(final String name)
    {
        return
            (Enumeration) AccessController.doPrivileged((PrivilegedAction<?>) () -> finder.getResources(name), acc);
    }

    protected URLStreamHandler getJarHandler() {
        return finder.loader.jarHandler;
    }

    private static class URIResourceFinder implements ResourceFinder {
        URL[] urls;
        final ResourceLoader loader;
        public URIResourceFinder(URI[] uris, URLStreamHandler jarHandler) {
            try {
                this.loader = (jarHandler != null)
                    ? new ResourceLoader(jarHandler)
                    : new ResourceLoader();
                URL[] urls = new URL[uris.length];
                for (int i = 0; i < uris.length; i++) {
                    urls[i] = uris[i].toURL();
                }
                this.urls = urls;
            }
            catch (MalformedURLException e) {
                throw new IllegalArgumentException(e.getMessage());
            }
        }
        public synchronized void addURI(URI uri) {
            try {
                URL url = uri.toURL();
                int len = this.urls.length;
                    URL[] urls = new URL[len + 1];
                System.arraycopy(this.urls, 0, urls, 0, len);
                urls[len] = url;
                this.urls = urls;
            }
            catch (MalformedURLException e) {
                throw new IllegalArgumentException(e.getMessage());
            }
        }
        private synchronized final URL[] getUrls() {
            return urls;
        }
        public ResourceHandle getResource(String name) {
            return loader.getResource(getUrls(), name);
        }
        public Enumeration getResources(String name) {
            return loader.getResources(getUrls(), name);
        }
        public URL findResource(String name) {
            return loader.findResource(getUrls(), name);
        }
        public Enumeration findResources(String name) {
            return loader.findResources(getUrls(), name);
        }
//        public URL[] getResolvedSearchPath() {
//            return loader.getResolvedSearchPath(getUrls());
//        }
    }
}
