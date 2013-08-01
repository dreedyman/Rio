/*
 * Copyright 2008 the original author or authors.
 * Copyright 2005 Sun Microsystems, Inc.
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
package org.rioproject.resources.util;

import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Redirect an input stream to an output stream
 *
 * @author Dennis Reedy
 */
public class StreamRedirector extends Thread {
    InputStream in;
    OutputStream out;

    public StreamRedirector(InputStream in, OutputStream out) {
        super("StreamRedirector");
        this.in  = in;
        this.out = out;
    }

    public void run() {
        byte[] buf = new byte[2048];
        int count;
        try {
            while((count = in.read(buf)) != -1 && !isInterrupted()) {
                if(out!=null) {
                    out.write(buf, 0, count);
                    out.flush();
                }
            }
        } catch(IOException e) {
            //e.printStackTrace();
        }
    }
}
