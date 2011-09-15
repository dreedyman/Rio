/*
 * Written by Dawid Kurzyniec and released to the public domain, as explained
 * at http://creativecommons.org/licenses/publicdomain
 */

package edu.emory.mathcs.util.classloader;

import java.util.regex.*;
import java.net.*;

/**
 * Utility methods related to remote resource access.
 *
 * @author Dawid Kurzyniec
 * @version 1.0
 */
public class ResourceUtils {

    private static final Pattern dotInMiddlePattern      = Pattern.compile("/\\./");
    private static final Pattern dotAtBegPattern         = Pattern.compile("^\\./");
    private static final Pattern dotAtEndPattern         = Pattern.compile("/\\.$");
    private static final Pattern multipleSlashPattern    = Pattern.compile("//+");
    private static final Pattern initialParentPattern    = Pattern.compile("/?(\\.\\./)*");
    private static final Pattern embeddedParentPattern   = Pattern.compile("[^/]+/\\.\\./");
    private static final Pattern trailingParentPattern   = Pattern.compile("[^/]+/\\.\\.$");
    private static final Pattern initialAbsParentPattern = Pattern.compile("^/(\\.\\./)+");
    private static final Pattern initialAbsSingleParentPattern =
                                                           Pattern.compile("^/\\.\\.$");

    private ResourceUtils() {}

    /**
     * Checks if the URI points to the local file.
     * @param uri the uri to check
     * @return true if the URI points to a local file
     */
    public static boolean isLocalFile(URI uri) {
        if (!uri.isAbsolute()) return false;
        if (uri.isOpaque()) return false;
        if (!"file".equalsIgnoreCase(uri.getScheme())) return false;
        if (uri.getAuthority() != null) return false;
        if (uri.getFragment() != null) return false;
        if (uri.getQuery() != null) return false;
        if ("".equals(uri.getPath())) return false;
        return true;
    }

    /**
     * Returns the path converted to the canonic form. Examples:
     * <pre>
     *  "/aaa/b/"                                      ->  "/aaa/b/"
     *  "/aaa/b/c/../.."                               ->  "/aaa/"
     *  "/aaa/../bbb/cc/./.././../dd/eee/fff/."        ->  "/dd/eee/fff/"
     *  "../aaa/../././bbb/./../ccc/"                  ->  "../ccc/"
     *  "aa/ddfdd/./sadfd/.././sdafa/../../.././././"  ->  ""
     *  "./aaa/."                                      ->  "aaa/"
     *  ".///aa//bb/"                                  ->  "aa/bb/"
     *  "../../aaa"                                    ->  "../../aaa"
     *  "/../../aaa"                                   ->  "/aaa"
     * </pre>
     */
    public static String canonizePath(String path) {
        StringBuffer buf = new StringBuffer(path);
        StringBuffer aux = new StringBuffer();
        while (replaceAll(buf, aux, dotInMiddlePattern, "/", 0));
        replaceAll(buf, aux, multipleSlashPattern, "/", 0);
        replaceFirst(buf, aux, dotAtBegPattern, "", 0);
        replaceFirst(buf, aux, dotAtEndPattern, "/", 0);

        int pos = 0;
        while (pos < buf.length()) {
            pos += skipPrefix(buf, aux, initialParentPattern, pos);
            if (!replaceFirst(buf, aux, embeddedParentPattern, "", pos)) {
                break;
            }
        }

        replaceFirst(buf, aux, trailingParentPattern, "", pos);

        replaceFirst(buf, aux, initialAbsParentPattern, "/", 0);
        replaceFirst(buf, aux, initialAbsSingleParentPattern, "/", 0);
        return buf.toString();
    }

    public static boolean isAbsolute(String path) {
        return (path.length() > 0 && path.charAt(0) == '/');
    }

    private static boolean replaceFirst(StringBuffer buf, StringBuffer aux,
                                        Pattern pattern, String replacement, int pos) {
        boolean chg = false;
        aux.setLength(0);
        Matcher matcher = pattern.matcher(buf);
        if (matcher.find(pos)) {
            matcher.appendReplacement(aux, replacement);
            chg = true;
        }
        matcher.appendTail(aux);
        buf.setLength(0);
        buf.append(aux);
        return chg;
    }

    private static boolean replaceAll(StringBuffer buf, StringBuffer aux,
                                      Pattern pattern, String replacement, int pos) {
        aux.setLength(0);
        Matcher matcher = pattern.matcher(buf);
        boolean found = matcher.find(pos);
        boolean chg = found;
        while (found) {
            matcher.appendReplacement(aux, replacement);
            found = matcher.find();
        }
        matcher.appendTail(aux);
        buf.setLength(0);
        buf.append(aux);
        return chg;
    }

    private static int skipPrefix(StringBuffer buf, StringBuffer aux,
                                  Pattern pattern, int pos) {
        Matcher matcher = initialParentPattern.matcher(buf);
        if (matcher.find(pos)) {
            return matcher.end()-pos;
        }
        return 0;
    }
}
