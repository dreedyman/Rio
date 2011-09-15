/*
 * Written by Dawid Kurzyniec and released to the public domain, as explained
 * at http://creativecommons.org/licenses/publicdomain
 */

package edu.emory.mathcs.util.io;

import java.io.*;

/**
 * Input stream that supports redirecting data directly to an output stream.
 *
 * @see TeeInputStream
 * @see ForkOutputStream
 * @see RedirectingReader
 *
 * @author Dawid Kurzyniec
 * @version 1.0
 */
public class RedirectingInputStream extends FilterInputStream
                                     implements RedirectibleInput {
    final byte[] buf;
    final boolean autoFlush;
    final boolean autoClose;

    /**
     * Creates a new redirecting input stream that reads from the specified
     * source with default autoflush and autoclose policies and a default
     * buffer size of 2KB. The default autoflush policy is to flush the output
     * after redirecting every chunk of data. The default autoclose policy is
     * not to close the output upon EOF on the input during redirection.
     *
     * @param in the input to read from
     */
    public RedirectingInputStream(InputStream in) {
        this(in, 2048);
    }

    /**
     * Creates a new redirecting input stream that reads from the specified
     * source with default autoflush and autoclose policies and a specified
     * buffer size. The default autoflush policy is to flush the output
     * after redirecting every chunk of data. The default autoclose policy is
     * not to close the output upon EOF on the input during redirection.
     * Buffer length is the maximum chunk size.
     *
     * @param in the input to read from
     * @param buflen the maximum chunk size
     */
    public RedirectingInputStream(InputStream in, int buflen) {
        this(in, true, false, buflen);
    }

    /**
     * Creates a new redirecting input stream that reads from the specified
     * source, has specified autoflush and autoclose policy and a default
     * buffer size of 2KB. The autoFlush parameter decides whether the output
     * should be automatically flushed after every chunk of redirected data. The
     * autoClose parameter decides whether the output should be closed upon EOF
     * on the input during redirection.
     *
     * @param in the input to read from
     * @param autoFlush decides whether to flush the output after each redirect
     * @param autoClose decides whether to close the output upon EOF on input
     *                  during redirection
     */
    public RedirectingInputStream(InputStream in, boolean autoFlush,
                                   boolean autoClose) {
        this(in, autoFlush, autoClose, 2048);
    }

    /**
     * Creates a new redirecting input stream that reads from the specified
     * source, has specified autoflush and autoclose policy, and a given buffer
     * size. The autoFlush parameter decides whether the output should be
     * automatically flushed after every chunk of redirected data. The
     * autoClose parameter decides whether the output should be closed upon EOF
     * on the input during redirection. Buffer length is the maximum chunk size.

     * @param in the input to read from
     * @param autoFlush decides whether to flush the output after each redirect
     * @param autoClose decides whether to close the output upon EOF on input
     *                  during redirection
     * @param buflen the maximum chunk size
     */
    public RedirectingInputStream(InputStream in, boolean autoFlush,
                                  boolean autoClose, int buflen) {
        super(in);
        this.autoFlush = autoFlush;
        this.autoClose = autoClose;
        this.buf = new byte[buflen];
    }

    public int redirect(OutputStream out, int len) throws IOException {
        int read = read(buf);
        if (read < 0) {
            if (autoClose) out.close();
        }
        else {
            out.write(buf, 0, read);
            if (autoFlush) out.flush();
        }
        return read;
    }

    public int redirectAll(OutputStream out) throws IOException {
        int total = 0;
        int read;
        while (true) {
            read = read(buf);
            if (read < 0) {
                if (autoClose) out.close();
                return total;
            }
            out.write(buf, 0, read);
            if (autoFlush) out.flush();
            total += read;
        }
    }
}
