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
package org.rioproject.util;

import java.io.File;
import java.util.StringTokenizer;

/**
 * Utility for converting strings to arrays
 */
public final class StringUtil {

    private StringUtil() {}

    /**
     * Convert a comma, space and/or {@link java.io.File#pathSeparator}
     * delimited String to array of Strings
     *
     * @param arg The String to convert
     *
     * @return An array of Strings
     */
    public static String[] toArray(final String arg) {
        return toArray(arg, " ,"+ File.pathSeparator);
    }

    /**
     * Convert a delimited String to array of Strings
     *
     * @param arg The String to convert
     * @param delim the delimiters to use
     *
     * @return An array of Strings
     */
    public static String[] toArray(final String arg, final String delim) {
        StringTokenizer tok = new StringTokenizer(arg, delim);
        String[] array = new String[tok.countTokens()];
        int i=0;
        while(tok.hasMoreTokens()) {
            array[i] = tok.nextToken();
            i++;
        }
        return(array);
    }
}
