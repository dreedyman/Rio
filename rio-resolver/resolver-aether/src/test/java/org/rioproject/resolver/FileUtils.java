/*
 * Copyright 2011 to the original author or authors.
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
package org.rioproject.resolver;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;

/**
 * Utilities for working with files for this test suite
 */
public class FileUtils {
    private static final Logger logger = LoggerFactory.getLogger(FileUtils.class.getName());

    /**
     * Get the path from a File object
     *
     * @param f The file
     * @return The canonical path. If there is an IOException getting the
     *         canonical path, return the absolute path
     */
    public static String getFilePath(File f) {
        String path;
        try {
            path = f.getCanonicalPath();
        } catch (IOException e) {
            path = f.getAbsolutePath();
            logger.warn("Unable to obtain canonical path for file [{}], returning absolute path: {}", f.getName(), path);
        }
        return path;
    }

    /**
     * Copies one file to another
     *
     * @param src The source file
     * @param dst The destination File. If the dst file does not exist, it is created
     * @throws java.io.IOException If the copy fails
     */
    public static void copy(File src, File dst) throws IOException {
        InputStream in = new FileInputStream(src);
        OutputStream out = new FileOutputStream(dst);

        // Transfer bytes from in to out
        byte[] buf = new byte[1024];
        int len;
        while ((len = in.read(buf)) > 0) {
            out.write(buf, 0, len);
        }
        in.close();
        out.close();
    }

    /**
     * Write contents to a file
     *
     * @param contents The contents to write
     * @param file     The file to write to
     * @throws IOException If the file cannot be written to
     */
    public static void writeFile(String contents, File file) throws IOException {
        BufferedWriter out = null;
        try {
            out = new BufferedWriter(new FileWriter(file));
            out.write(contents);
        } finally {
            if (out != null)
                out.close();
        }
    }

    /**
     * Remove a File
     *
     * @param file A File object to remove
     */
    public static void remove(File file) {
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            if(files!=null) {
                for (File f : files) {
                    if (f.isDirectory())
                        remove(f);
                    else {
                        if (f.delete()) {
                            if (logger.isDebugEnabled())
                                logger.debug("Removed {}", getFilePath(f));
                        } else {
                            if (f.exists())
                                logger.warn("Unable to remove {}", getFilePath(f));
                        }
                    }
                }
                if (file.delete()) {
                    if (logger.isDebugEnabled())
                        logger.debug("Removed {}", getFilePath(file));
                } else {
                    if (file.exists())
                        logger.warn("Unable to remove " + getFilePath(file));
                }
            }
        } else {
            if (file.delete()) {
                if (logger.isDebugEnabled())
                    logger.debug("Removed {}", getFilePath(file));
            } else {
                if (file.exists())
                    logger.warn("Unable to remove {}", getFilePath(file));
            }
        }
    }
}
