/*
 * Written by Dawid Kurzyniec and released to the public domain, as explained
 * at http://creativecommons.org/licenses/publicdomain
 */

package edu.emory.mathcs.util.classloader.jar;

import java.io.*;
import java.net.*;
import java.util.regex.*;

import edu.emory.mathcs.util.classloader.*;

/**
 * Alternative implementation of URLStreamHandler for JAR files that supports
 * customizable JAR caching policies. It addresses bugs
 * 4405789, 4388666, 4639900 in Java Bug Parade. SUN recommends to disable
 * caches completely as a workaround for those bugs; however, this may
 * significantly affect performance in case of resources downloaded from the
 * network. This class is a part of the solution that allows to tailor the
 * caching policy according to the program needs, with cache-per-classloader
 * default policy.
 *
 * @see edu.emory.mathcs.util.classloader.jar.JarURLConnection
 *
 * @author Dawid Kurzyniec
 * @version 1.0
 */
public class JarURLStreamHandler extends URLStreamHandler {

    // "jar:" + url + "!" + /<path>/ + <file>? + "#"<anchor>?
    private static final Pattern ABSOLUTE_JAR_URL_PATTERN =
        Pattern.compile("jar:(.*)!(/(?:.*/)?)((?:[^/#]+)?)((?:#.*)?)");

    final JarURLConnection.JarOpener opener;

    /**
     * Create new JarURLStreamHandler that will use its separate URL cache
     * managed by a newly created {@link edu.emory.mathcs.util.classloader.jar.JarProxy} instance.
     */
    public JarURLStreamHandler() {
        this(new JarProxy());
    }

    /**
     * Create new JarURLStreamHandler that will use specified
     * JAR opener.
     *
     * @param opener JAR opener that handles file download and caching
     */
    public JarURLStreamHandler(JarURLConnection.JarOpener opener) {
        this.opener = opener;
    }

    public URLConnection openConnection(URL url) throws IOException {
        return new JarURLConnection(url, opener);
    }

    protected void parseURL(URL u, String spec, int start, int limit) {
        Matcher matcher;
        if ((matcher = ABSOLUTE_JAR_URL_PATTERN.matcher(spec)).matches()) {
            // spec is an absolute URL
            String base = matcher.group(1);
            try {
                // verify
                URL baseURL = new URL(base);
            }
            catch (MalformedURLException e) {
                throw new IllegalArgumentException(e.toString());
            }
            String path = matcher.group(2) + matcher.group(3);
            path = ResourceUtils.canonizePath(path);
            String ref = matcher.group(4);
            if (ref.length() == 0) {
                ref = null;
            }
            else {
                ref = ref.substring(1);
            }
            setURL(u, "jar", "", -1, "", "", base + "!" + path, null, ref);
        }
        else if ((matcher = ABSOLUTE_JAR_URL_PATTERN.matcher(u.toString())).matches()) {
            String ref = spec.substring(limit);
            if (ref.length() == 0) {
                ref = null;
            }
            else {
                ref = ref.substring(1);
            }
            spec = spec.substring(start, limit);
            String base = matcher.group(1);
            String path;
            if (spec.length() > 0 && spec.charAt(0) == '/') {
                path = spec;
            }
            else {
                String cxtDir = matcher.group(2);
                path = cxtDir + spec;
            }
            path = ResourceUtils.canonizePath(path);
            setURL(u, "jar", "", -1, "", "", base + "!" + path, null, ref);
        }
        else {
            throw new IllegalArgumentException("Neither URL nor the spec are " +
                                               "valid JAR urls");
        }
    }
}
