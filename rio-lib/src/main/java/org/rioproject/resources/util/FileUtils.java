/*
 * Copyright to the original author or authors.
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

import org.rioproject.util.PropertyHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;

/**
 * Utilities for working with Files.
 */
public final class FileUtils {
    private static final Logger logger = LoggerFactory.getLogger(FileUtils.class.getName());

    private FileUtils() {}

    /**
     * Get the path from a File object
     *
     * @param f The file
     *
     * @return The canonical path. If there is an IOException getting the
     * canonical path, return the absolute path
     */
    public static String getFilePath(final File f) {
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
     * Make a directory or file name
     *
     * @param root The root directory name
     * @param extension The name of the extension
     * @return A directory name
     */
    public static String makeFileName(final String root, final String extension) {
        String name;
        if(extension==null)
            throw new IllegalArgumentException("extension cannot be null");
        String ext = PropertyHelper.expandProperties(extension);
        if(ext.startsWith("/")) {
            name = ext;
        } else if(root!=null) {
            if(root.endsWith(File.separator))
                name = root + ext;
            else
                name = root + File.separator + ext;
        } else {
            name = ext;
        }
        return (name);
    }

    /**
     * Remove a File
     *
     * @param file A File object to remove
     *
     * @return {@code} true if the file has been removed {@code} false if not.
     */
    public static boolean remove(final File file) {
        boolean removed;
        if(file.isDirectory()) {
            File[] files = file.listFiles();
            if(files==null)
                return false;
            for (File f : files) {
                if (f.isDirectory())
                    remove(f);
                else {
                    if(f.delete()) {
                        logger.debug("Removed {}", getFilePath(f));
                    } else {
                        if(f.exists())
                            logger.warn("Unable to remove {}", getFilePath(f));
                    }
                }
            }
            removed = file.delete();
            if(removed) {
                logger.debug("Removed {}", getFilePath(file));
            } else {
                if(file.exists())
                    logger.warn("Unable to remove {}", getFilePath(file));
            }
        } else {
            removed = file.delete();
            if(removed) {
                logger.debug("Removed {}", getFilePath(file));
            } else {
                if(file.exists())
                    logger.warn("Unable to remove {}", getFilePath(file));
            }
        }
        return removed;
    }

    /**
     * Check that a directory exists, is a directory and can be written to
     *
     * @param dir The directory to check. If the directory does not exist,
     * create it
     * @param description A description of the directory, used for filling in
     * details for IOException that may be thrown
     *
     * @throws IOException If the provided dir is not a directory, or if the
     * directory cannot be written to
     * @throws IllegalArgumentException if the dir parameter is null
     */
    public static void checkDirectory(final File dir, final String description) throws IOException {
        if(dir==null)
            throw new IllegalArgumentException("dir is null");
        String descriptionToUse = description;
        if(descriptionToUse==null)
            descriptionToUse = dir.getName();
        if(dir.exists()) {
            if(!dir.isDirectory())
                throw new IOException("The "+descriptionToUse+" "+
                                      "["+dir.getAbsolutePath()+"] exists, "+
                                      "but is not a directory. "+
                                      "Aborting service creation");
        } else {
            if(dir.mkdirs())
                logger.info("Created {} directory [{}]",descriptionToUse, dir.getAbsolutePath());
        }
        if(!dir.canWrite())
            throw new IOException("We do not have write access to the " +
                                  descriptionToUse+" directory "+
                                  "["+dir.getAbsolutePath()+"], " +
                                  "therefore a log cannot be created. " +
                                  "Aborting service creation");
    }

    /**
     * Copies one file to another
     *
     * @param src The source file
     * @param dst The destination File. If the dst file does not exist, it is created
     * @throws IOException If the copy fails
     */
    public static void copy(final File src, final File dst) throws IOException {
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
     * Determine if the provided {@code File} is a symbolic link.
     *
     * @param file The file to test.
     *
     * @return If the file is a symbolic link return {@code true}, otherwise return {@code false}.
     *
     * @throws IOException If there are problems obtaining the canonical file.
     * @throws IllegalArgumentException if the {@code file} is {@code null}
     */
    public static boolean isSymbolicLink(final File file) throws IOException {
        if (file == null)
            throw new IllegalArgumentException("File must not be null");
        File canon;
        if (file.getParent() == null) {
            canon = file;
        } else {
            File canonDir = file.getParentFile().getCanonicalFile();
            canon = new File(canonDir, file.getName());
        }
        return !canon.getCanonicalFile().equals(canon.getAbsoluteFile());
    }
}
