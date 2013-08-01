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
package org.rioproject.resources.persistence;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import java.util.Collection;
import java.util.List;

/**
 * Miscellaneous file system manipulation methods.
 *
 * @author Sun Microsystems, Inc.
 */
public class FileSystem {
    /**
     * Remove this completely.  If the parameter is a directory, it is
     * removed after recursively destroying all its contents, including
     * subdirectories.  If the named file does not exist,
     * <code>destroy</code> simply returns.
     *
     * @param proceed Proceed in the face of errors; otherwise the first error stops
     *                the execution of the method.
     * @throws IOException The list of files that couldn't be removed (in the detail string).
     */
    public static void destroy(File file, boolean proceed) throws IOException {
        if (!file.exists() || file.delete())    // that was easy
            return;

        List<File> errors = (proceed ? new ArrayList<File>() : null);
        destroyDir(file, errors);

        if (errors != null && !errors.isEmpty()) {
            StringBuilder buf = new StringBuilder("couldn't delete:");
            for (File error : errors)
                buf.append('\n').append(error);
            throw new IOException(buf.toString());
        }
    }

    /**
     * Perform the recursion for <code>destroy</code>.
     */
    private static void destroyDir(File dir, Collection<File> errors) throws IOException {
        if (!dir.isDirectory()) {    // catch assumption that this is a dir
            handleError(errors, dir);
            return;
        }

        String[] names = dir.list();
        for (String name : names) {
            File file = new File(dir, name);
            if (!file.delete())        // assume it's a dir
                destroyDir(file, errors);
        }
        if (!dir.delete())
            handleError(errors, dir);
    }

    /**
     * Handle an error, either by adding to the list, or if there is no
     * list, throwing an <code>IOException</code>.
     */
    private static void handleError(Collection<File> errors, File path) throws IOException {
        if (errors == null)
            throw new IOException("couldn't delete " + path);
        errors.add(path);
    }
}
