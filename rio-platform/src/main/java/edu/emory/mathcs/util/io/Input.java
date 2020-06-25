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
