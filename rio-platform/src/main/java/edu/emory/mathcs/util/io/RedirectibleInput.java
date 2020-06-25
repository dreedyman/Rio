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
