/*
 * Written by Dawid Kurzyniec and released to the public domain, as explained
 * at http://creativecommons.org/licenses/publicdomain
 */

package edu.emory.mathcs.util.classloader;

import java.net.*;
import java.io.*;
import java.util.*;

/**
 * Abstraction of resource searching policy. Given resource name, the resource
 * finder performs implementation-specific lookup, and, if it is able to locate
 * the resource, returns the {@link edu.emory.mathcs.util.classloader.ResourceHandle handle(s)} or URL(s) of it.
 *
 * @author Dawid Kurzyniec
 * @version 1.0
 */
public interface ResourceFinder {
    /**
     * Find the resource by name and return URL of it if found.
     * @param name the resource name
     * @return resource URL or null if resource was not found
     */
    public URL findResource(String name);

    /**
     * Find all resources with given name and return enumeration of their URLs.
     * @param name the resource name
     * @return enumeration of resource URLs (possibly empty).
     */
    public Enumeration findResources(String name);

    /**
     * Get the resource by name and, if found, open connection to it and return
     * the {@link edu.emory.mathcs.util.classloader.ResourceHandle handle} of it.
     * @param name the resource name
     * @return resource handle or null if resource was not found
     */
    public ResourceHandle getResource(String name);

    /**
     * Get all resources with given name and return enumeration of their
     * {@link edu.emory.mathcs.util.classloader.ResourceHandle resource handles}.
     * @param name the resource name
     * @return enumeration of resource handles (possibly empty).
     */
    public Enumeration getResources(String name);
}
