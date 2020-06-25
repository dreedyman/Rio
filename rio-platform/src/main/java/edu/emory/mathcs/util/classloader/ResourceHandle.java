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

import java.io.*;
import java.net.*;
import java.security.cert.*;
import java.util.jar.*;

/**
 * This class represents a handle (a connection) to some resource, which may
 * be a class, native library, text file, image, etc. Handles are returned
 * by {@link edu.emory.mathcs.util.classloader.ResourceLoader}'s <i>get</i> methods.
 * Having the resource handle, in addition to accessing the resource data
 * (using methods {@link #getInputStream} or {@link #getBytes}) as well as
 * access resource metadata, such as attributes, certificates, etc.
 * <p>
 * As soon as the handle is no longer in use, it should be explicitly
 * {@link #close}d, similarly to I/O streams.
 *
 * @author Dawid Kurzyniec
 * @version 1.0
 */
@SuppressWarnings("PMD")
public abstract class ResourceHandle {

    /**
     * Return the name of the resource. The name is a "/"-separated path
     * name that identifies the resource.
     */
    public abstract String getName();

    /**
     * Returns the URL of the resource.
     */
    public abstract URL getURL();

    /**
     * Returns the CodeSource URL for the class or resource.
     */
    public abstract URL getCodeSourceURL();

    /**
     * Returns and InputStream for reading this resource data.
     */
    public abstract InputStream getInputStream() throws IOException;

    /**
     * Returns the length of this resource data, or -1 if unknown.
     */
    public abstract int getContentLength();

    /**
     * Returns this resource data as an array of bytes.
     */
    public byte[] getBytes() throws IOException {
        byte[] buf;
        InputStream in = getInputStream();
        int len = getContentLength();
        try {
            if (len != -1) {
                // read exactly len bytes
                buf = new byte[len];
                while (len > 0) {
                    int read = in.read(buf, buf.length - len, len);
                    if (read < 0) {
                        throw new IOException("unexpected EOF");
                    }
                    len -= read;
                }
            } else {
                // read until end of stream is reached
                buf = new byte[2048];
                int total = 0;
                while ((len = in.read(buf, total, buf.length - total)) >= 0) {
                    total += len;
                    if (total >= buf.length) {
                        byte[] aux = new byte[total * 2];
                        System.arraycopy(buf, 0, aux, 0, total);
                        buf = aux;
                    }
                }
                // trim if necessary
                if (total != buf.length) {
                    byte[] aux = new byte[total];
                    System.arraycopy(buf, 0, aux, 0, total);
                    buf = aux;
                }
            }
        } finally {
            in.close();
        }
        return buf;
    }

    /**
     * Returns the Manifest of the JAR file from which this resource
     * was loaded, or null if none.
     */
    public Manifest getManifest() throws IOException {
        return null;
    }

    /**
     * Return the Certificates of the resource, or null if none.
     */
    public Certificate[] getCertificates() {
        return null;
    }

    /**
     * Return the Attributes of the resource, or null if none.
     */
    public Attributes getAttributes() throws IOException {
        Manifest m = getManifest();
        if (m == null) return null;
        String entry = getURL().getFile();
        return m.getAttributes(entry);
    }

   /**
    * Closes a connection to the resource indentified by this handle. Releases
    * any I/O objects associated with the handle.
    */
    public void close() {
    }
//
//    /**
//     * Ensures that {@link #release()} method is eventually called when this
//     * object is finalized.
//     */
//    protected void finalize() throws Throwable {
//        super.finalize();
//        release();
//    }

    public String toString() {
        return "[" + getName() + ": " + getURL() + "; code source: " + getCodeSourceURL() + "]";
    }
}
