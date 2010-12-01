/*
 * Written by Dawid Kurzyniec and released to the public domain, as explained
 * at http://creativecommons.org/licenses/publicdomain
 */

package edu.emory.mathcs.util.classloader.jar;

import java.net.*;
import java.io.*;
import java.util.jar.*;
import java.security.*;

/**
 * Alternative implementation of {@link java.net.JarURLConnection} which
 * supports customizable JAR caching policies. It addresses bugs
 * 4405789, 4388666, 4639900 in Java Bug Parade. SUN recommends to disable
 * caches completely as a workaround for those bugs; however, this may
 * significantly affect performance in case of resources downloaded from the
 * network. This class is a part of the solution that allows to tailor the
 * caching policy according to the program needs, with cache-per-classloader
 * default policy.
 *
 * @see edu.emory.mathcs.util.classloader.jar.JarURLStreamHandler
 *
 * @author Dawid Kurzyniec
 * @version 1.0
 */
public class JarURLConnection extends java.net.JarURLConnection {
    final JarOpener opener;
    boolean connected;
    JarFile jfile;
    JarEntry jentry;

    /**
     * Creates JarURLConnection for a given URL, using specified JAR opener.
     * @param url the URL to open connection to
     * @param opener the JAR opener to use
     * @throws java.io.IOException
     */
    public JarURLConnection(URL url, JarOpener opener) throws IOException {
        super(url);
        this.opener = opener;
    }

    public synchronized void connect() throws IOException {
        if (connected) return;
        jfile = opener.openJarFile(this);
        if (jfile != null && getEntryName() != null) {
            jentry = jfile.getJarEntry(getEntryName());
            if (jentry == null) {
                throw new FileNotFoundException("Entry " + getEntryName() +
                    " not found in " + getJarFileURL());
            }
        }
        connected = true;
    }

    public synchronized JarFile getJarFile() throws IOException {
        connect();
        return jfile;
    }

    public synchronized JarEntry getJarEntry() throws IOException {
        connect();
        return jentry;
    }

    public synchronized InputStream getInputStream() throws IOException {
        connect();
        return jfile.getInputStream(jentry);
    }

    public Permission getPermission() throws IOException {
        return getJarFileURL().openConnection().getPermission();
    }

    /**
     * Abstraction of JAR opener which allows to implement various caching
     * policies. The opener receives URL pointing to the JAR file, along
     * with other meta-information, as a JarURLConnection instance. Then it has
     * to download the file (if it is remote) and open it.
     *
     * @author Dawid Kurzyniec
     * @version 1.0
     */
    public static interface JarOpener {
        /**
         * Given the URL connection (not yet connected), return JarFile
         * representing the resource. This method is invoked as a part of
         * the {@link #connect} method in JarURLConnection.
         *
         * @param conn the connection for which the JAR file is required
         * @return opened JAR file
         * @throws java.io.IOException if I/O error occurs
         */
        public JarFile openJarFile(java.net.JarURLConnection conn) throws IOException;
    }
}
