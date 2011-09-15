/*
 * Written by Dawid Kurzyniec and released to the public domain, as explained
 * at http://creativecommons.org/licenses/publicdomain
 */

package edu.emory.mathcs.util.io;

import java.io.*;

/**
 * Input source capable of redirecting
 * the data to an output stream. It means that instead of reading data into an
 * array and then feeding it to the output stream, the data may be moved
 * directly from the input to the output.
 *
 * @author Dawid Kurzyniec
 * @version 1.0
 */
public interface RedirectibleInput extends Input {

    /**
     * Reads and redirects up to the <code>len</code> bytes of data to a
     * specified output stream. Returns the number of bytes actually redirected.
     *
     * @param dest the destination stream
     * @param len the maximum number of bytes to redirect
     * @return number of bytes actually redirected
     * @throws IOException if I/O error occurs
     */
    int redirect(OutputStream dest, int len) throws IOException;

    /**
     * Readirects all further data from this input into the specified output
     * stream, until EOF. Returns the number of bytes actually redirected.
     *
     * @param dest the destination stream
     * @return number of bytes actually redirected
     * @throws IOException if I/O error occurs
     */
    int redirectAll(OutputStream dest) throws IOException;
}
