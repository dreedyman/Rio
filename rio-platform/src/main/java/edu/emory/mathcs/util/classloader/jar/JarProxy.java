/*
 * Written by Dawid Kurzyniec and released to the public domain, as explained
 * at http://creativecommons.org/licenses/publicdomain
 */

package edu.emory.mathcs.util.classloader.jar;

import edu.emory.mathcs.util.classloader.ResourceUtils;
import edu.emory.mathcs.util.io.RedirectibleInput;
import edu.emory.mathcs.util.io.RedirectingInputStream;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.security.AccessController;
import java.security.Permission;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.security.cert.Certificate;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;

/**
 * Implementation of {@link edu.emory.mathcs.util.classloader.jar.JarURLConnection.JarOpener} that caches downloaded
 * JAR files in a local file system.
 *
 * @see edu.emory.mathcs.util.classloader.jar.JarURLConnection
 * @see edu.emory.mathcs.util.classloader.jar.JarURLStreamHandler
 *
 * @author Dawid Kurzyniec
 * @version 1.0
 */
@SuppressWarnings("unchecked")
public class JarProxy implements JarURLConnection.JarOpener {

    private final Map cache = new HashMap();

    public JarProxy() {}

    public JarFile openJarFile(java.net.JarURLConnection conn) throws IOException {
        URL url = conn.getJarFileURL();
        CachedJarFile result;
        synchronized (cache) {
            result = (CachedJarFile)cache.get(url);
        }
        if (result != null) {
            SecurityManager security = System.getSecurityManager();
            if (security != null) {
                security.checkPermission(result.perm);
            }
            return result;
        }

        // we have to download and open the JAR; yet it may be a local file
        try {
            URI uri = new URI(url.toString());
            if (ResourceUtils.isLocalFile(uri)) {
                File file = new File(uri);
                Permission perm = new FilePermission(file.getAbsolutePath(), "read");
                result = new CachedJarFile(file, perm, false);
            }
        }
        catch (URISyntaxException e) {
            // apparently not a local file
        }

        if (result == null) {
            final URLConnection jarconn = url.openConnection();

            // set up the properties based on the JarURLConnection
            jarconn.setAllowUserInteraction(conn.getAllowUserInteraction());
            jarconn.setDoInput(conn.getDoInput());
            jarconn.setDoOutput(conn.getDoOutput());
            jarconn.setIfModifiedSince(conn.getIfModifiedSince());

            Map map = conn.getRequestProperties();
            for (Iterator itr = map.entrySet().iterator(); itr.hasNext();) {
                Map.Entry entry = (Map.Entry)itr.next();
                jarconn.setRequestProperty((String)entry.getKey(), (String)entry.getValue());
            }

            jarconn.setUseCaches(conn.getUseCaches());

            final InputStream in = getJarInputStream(jarconn);

            try {
                result = (CachedJarFile)
                    AccessController.doPrivileged(new PrivilegedExceptionAction() {
                        public Object run() throws IOException {
                            File file = File.createTempFile("jar_cache", "");
                            FileOutputStream out = new FileOutputStream(file);
                            try {
                                RedirectibleInput r =
                                    new RedirectingInputStream(in, false, false);
                                int len = r.redirectAll(out);
                                out.flush();
                                if (len == 0) {
                                    // e.g. HttpURLConnection: "NOT_MODIFIED"
                                    return null;
                                }
                            }
                            finally {
                                out.close();
                            }
                            return new CachedJarFile(file, jarconn.getPermission(), true);

                        }
                    });
            }
            catch (PrivilegedActionException pae) {
                throw (IOException)pae.getException();
            }
            finally {
                in.close();
            }
        }

        // if no input came (e.g. due to NOT_MODIFIED), do not cache
        if (result == null) return null;

        // optimistic locking
        synchronized (cache) {
            CachedJarFile asyncResult = (CachedJarFile) cache.get(url);
            if (asyncResult != null) {
                // some other thread already retrieved the file; return w/o
                // security check since we already succeeded in getting past it
                result.closeCachedFile();
                return asyncResult;
            }
            cache.put(url, result);
            return result;
        }
    }

    protected InputStream getJarInputStream(URLConnection conn) throws IOException {
        return conn.getInputStream();
    }

    protected void clear() {
        Map cache;
        synchronized (this.cache) {
            cache = new HashMap(this.cache);
            this.cache.clear();
        }
        for (Iterator itr = cache.values().iterator(); itr.hasNext();) {
            CachedJarFile jfile = (CachedJarFile)itr.next();
            try {
                jfile.closeCachedFile();
            }
            catch (IOException e) {
                // best-effort
            }
        }
    }

    protected void finalize() {
        clear();
    }

    private static class CachedJarFile extends JarFile {
        final Permission perm;
        CachedJarFile(File file, Permission perm, boolean tmp) throws IOException {
            super(file, true, JarFile.OPEN_READ | (tmp ? JarFile.OPEN_DELETE : 0));
            this.perm = perm;
        }

        public Manifest getManifest() throws IOException {
            Manifest orig = super.getManifest();
            if (orig == null) return null;
            // make sure the original manifest is not modified
            Manifest man = new Manifest();
            man.getMainAttributes().putAll(orig.getMainAttributes());
            for (Iterator itr = orig.getEntries().entrySet().iterator(); itr.hasNext();) {
                Map.Entry entry = (Map.Entry)itr.next();
                man.getEntries().put((String)entry.getKey(),
                                     new Attributes((Attributes)entry.getValue()));
            }
            return man;
        }

        public ZipEntry getEntry(String name) {
            // super.getJarEntry() would result in stack overflow
            return super.getEntry(name);
        }

        protected void finalize() throws IOException {
            closeCachedFile();
        }

        protected void closeCachedFile() throws IOException {
            super.close();
        }

        public void close() throws IOException {
            // no op; do NOT close file while still in cache
        }

        private static class Entry extends JarEntry {
            JarEntry jentry;
            Entry(JarEntry jentry) {
                super(jentry);
                this.jentry = jentry;
            }
            public Certificate[] getCertificates() {
                Certificate[] certs = jentry.getCertificates();
                return (certs == null ? null : (Certificate[])certs.clone());
            }
            public Attributes getAttributes() throws IOException {
                Attributes attr = jentry.getAttributes();
                return (attr == null ? null : new Attributes(attr));
            }
        }
    }
}
