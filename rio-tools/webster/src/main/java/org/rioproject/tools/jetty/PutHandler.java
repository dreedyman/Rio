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
package org.rioproject.tools.jetty;

import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.DefaultHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * @author Dennis Reedy
 */
public class PutHandler extends DefaultHandler {
    private final  File putDir;
    private static final int BUFFER_SIZE = 4096;
    private static final Logger logger = LoggerFactory.getLogger(PutHandler.class);

    public PutHandler(File putDir) {
        this.putDir = putDir;
        logger.info("Created PutHandler for: {}", putDir.getPath());
    }

    @Override
    public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        String method = request.getMethod();
        if (!HttpMethod.PUT.is(method)) {
            super.handle(target, baseRequest, request, response);
            return;
        }
        logger.info("Method: {}", method);
        if (logger.isDebugEnabled()) {
            String s = "================ PUT ================\n" +
                    "target: " + target + "\n" +
                    "baseRequest: " + baseRequest + "\n" +
                    "HttpServletRequest: " + request + "\n" +
                    "HttpServletResponse: " + response + "\n";
            logger.debug("\n" + s);
        }
        put(target, baseRequest, response);

        baseRequest.setHandled(true);
    }

    private void put(String fileName, Request baseRequest, HttpServletResponse response) throws IOException {
        int length = baseRequest.getContentLength();
        File putFile = new File(putDir, fileName);
        if (putFile.exists()) {
            response.setStatus(HttpServletResponse.SC_OK);
        } else {
            response.setStatus(HttpServletResponse.SC_CREATED);
            File parentDir = putFile.getParentFile();
            if (logger.isDebugEnabled()) {
                logger.debug("Parent: {}, exists? {}", parentDir.getPath(), parentDir.exists());
            }
            if (!parentDir.exists()) {
                parentDir.mkdirs();
            }
        }

        try(DataOutputStream requestedFileOutputStream = new DataOutputStream(new FileOutputStream(putFile))) {
            int read;
            long amountRead = 0;
            byte[] buffer = new byte[Math.min(length, BUFFER_SIZE)];
            while (amountRead < length) {
                read = baseRequest.getInputStream().read(buffer);
                requestedFileOutputStream.write(buffer, 0, read);
                amountRead += read;
            }
            requestedFileOutputStream.flush();
            if (logger.isDebugEnabled()) {
                StringBuilder s = new StringBuilder();
                s.append("Wrote: ").append(putFile.getPath()).append(" size: ").append(putFile.length()).append("\n");
                s.append("HEADERS\n");
                for (String n : response.getHeaderNames()) {
                    s.append("\t").append(response.getHeader(n)).append("\n");
                }
                logger.debug("\n{}", s.toString());
            }
        } catch (IOException e) {
            e.printStackTrace();
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }

    }

}
