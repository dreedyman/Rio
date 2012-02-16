/*
 * Written by Dawid Kurzyniec and released to the public domain, as explained
 * at http://creativecommons.org/licenses/publicdomain
 */

package edu.emory.mathcs.util.io;

import java.io.*;

/**
 * An abstraction of a plain input stream. Subinterfaces supply additional
 * methods, indicating additional properties of the stream such as read
 * with timeout or redirectability.
 *
 * @see InputStream
 *
 * @author Dawid Kurzyniec
 * @version 1.0
 */
public interface Input {
    int read() throws IOException;
    int read(byte[] buf) throws IOException;
    int read(byte[] buf, int off, int len) throws IOException;
    int available() throws IOException;
    long skip(long n) throws IOException;
    void close() throws IOException;
    boolean markSupported();
    void reset() throws IOException;
    void mark(int readLimit);
}
