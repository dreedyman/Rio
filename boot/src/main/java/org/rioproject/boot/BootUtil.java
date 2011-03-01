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
package org.rioproject.boot;

import org.rioproject.net.PortRangeServerSocketFactory;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.ServerSocket;
import java.net.URL;
import java.util.*;


/**
 * Provides static convenience methods for use in configuration files. This class cannot
 * be instantiated.
 *
 * @author Dennis Reedy
 */
public class BootUtil {
    
    /** This class cannot be instantiated. */
    private BootUtil() {
        throw new AssertionError(
                "org.rioproject.boot.BootUtil cannot be instantiated");
    }
    
    /**
     * Return the codebase for the provided JAR name and port. This method will 
     * first get the IP Address of the machine using 
     * <code>java.net.InetAddress.getLocalHost().getHostAddress()</code>, then
     * construct the codebase using the host address and the port for the provided 
     * jar.
     * 
     * @param jar The JAR to use
     * @param port The port to use when constructing the codebase
     * @return the codebase for the JAR
     * @throws java.net.UnknownHostException if no IP address for the local
     * host could be found.
     */
    public static String getCodebase(String jar, String port) 
    throws java.net.UnknownHostException {
        return(getCodebase(jar, getHostAddress(), port));        
    }
    
    /**
     * Return the codebase for the provided JAR names and port. This method will 
     * first get the IP Address of the machine using 
     * <code>java.net.InetAddress.getLocalHost().getHostAddress()</code>, then 
     * construct the codebase using the host address and the port for each jar in 
     * the array of jars.
     * 
     * @param jars The JAR names to use
     * @param port The port to use when constructing the codebase
     * @return The codebase as a space-delimited String for the provided JARs
     * @throws java.net.UnknownHostException if no IP address for the local
     * host could be found.
     */
    public static String getCodebase(String[] jars, String port) 
    throws java.net.UnknownHostException {
        return(getCodebase(jars, getHostAddress(), port));        
    }
    
    /**
     * Return the codebase for the provided JAR name, port and address
     * 
     * @param jar The JAR to use
     * @param address The address to use when constructing the codebase
     * @param port The port to use when constructing the codebase
     * @return the codebase for the JAR
     */
    public static String getCodebase(String jar, String address, String port) {
        StringBuilder sb = new StringBuilder();
        sb.append("http://").append(address).append(":").append(port).append("/").append(jar);
        return(sb.toString());
    }
    
    /**
     * Return the codebase for the provided JAR names, port and address
     * 
     * @param jars Array of JAR names
     * @param address The address to use when constructing the codebase
     * @param port The port to use when constructing the codebase
     * @return the codebase for the JAR
     */
    public static String getCodebase(String[] jars, String address, String port) {
        StringBuilder buffer = new StringBuilder();
        for(int i=0; i<jars.length; i++) {
            if(i>0)
                buffer.append(" ");
            buffer.append("http://")
                .append(address)
                .append(":")
                .append(port)
                .append("/")
                .append(jars[i]);
        }
        return(buffer.toString());
    }
    
    /**
     * Return the local host address using 
     * <code>java.net.InetAddress.getLocalHost().getHostAddress()</code>
     * 
     * @return The local host address
     * @throws java.net.UnknownHostException if no IP address for the local
     * host could be found.
     */
    public static String getHostAddress() throws java.net.UnknownHostException {
        return java.net.InetAddress.getLocalHost().getHostAddress();
    }

    /**
     * Return the local host address for a passed in host using
     * {@link java.net.InetAddress#getByName(String)}
     *
     * @param name The name of the host to return
     *
     * @return The local host address
     *
     * @throws java.net.UnknownHostException if no IP address for the host name
     * could be found.
     */
    public static String getHostAddress(String name)
    throws java.net.UnknownHostException {
        return java.net.InetAddress.getByName(name).getHostAddress();
    }

    /**
     * Return the local host address based on the value of a system property.
     * using {@link java.net.InetAddress#getByName(String)}. If the system
     * property is not resolvable, return the default host address obtained from
     * {@link java.net.InetAddress#getLocalHost()}
     *
     * @param property The property name to use
     *
     * @return The local host address
     *
     * @throws java.net.UnknownHostException if no IP address for the host name
     * could be found.
     */
    public static String getHostAddressFromProperty(String property)
        throws java.net.UnknownHostException {
        String host = getHostAddress();
        String value = System.getProperty(property);
        if(value != null) {
            host = java.net.InetAddress.getByName(value).getHostAddress();
        }
        return(host);
    }
    
    /**
     * Return the local host name 
     * <code>java.net.InetAddress.getLocalHost().getHostName()</code>
     * 
     * @return The local host name
     * @throws java.net.UnknownHostException if no hostname for the local
     * host could be found.
     */
    public static String getHostName() throws java.net.UnknownHostException {
        return java.net.InetAddress.getLocalHost().getHostName();
    }
    
    /**
     * Get an anonymous port
     * 
     * @return An anonymous port created by instantiating a 
     * <code>java.net.ServerSocket</code> with a port of 0
     *
     * @throws IOException If an anonymous port cannot be obtained
     */
    public static int getAnonymousPort() throws java.io.IOException {
        java.net.ServerSocket socket = new java.net.ServerSocket(0);
        int port = socket.getLocalPort();
        socket.close();
        return(port);
    }

    /**
     * Get a port from a range of ports
     *
     * @param portRange A range of ports. The port range is specified as &quot;-&quot; delimited
     * string, <tt>startRange-endRange</tt>, where <tt>startRange</tt> and <tt>endRange</tt>
     * are inclusive.
     *
     * @return An port created by instantiating a
     * <code>org.rioproject.net.PortRangeServerSocketFactory</code> with provided range
     *
     * @throws IOException If a port cannot be obtained
     * @throws IllegalArgumentException is either bound is not between
     * 0 and 65535, or if <code>end</code> is &lt; than <code>low</code>.
     */
    public static int getPortFromRange(String portRange) throws IOException {
        String[] range = portRange.split("-");
        int start = Integer.parseInt(range[0]);
        int end = Integer.parseInt(range[1]);
        return getPortFromRange(start, end);
    }

    /**
     * Get a port from a range of ports
     *
     * @param start The range to start from (inclusive)
     * @param end The end of the range (inclusive)
     *
     * @return A port created by instantiating a
     * <code>org.rioproject.net.PortRangeServerSocketFactory</code> with provided range
     *
     * @throws IOException If a port cannot be obtained
     * @throws IllegalArgumentException is either bound is not between
     * 0 and 65535, or if <code>end</code> is &lt; than <code>low</code>.
     */
    public static int getPortFromRange(int start, int end) throws IOException {
        PortRangeServerSocketFactory factory = new PortRangeServerSocketFactory(start, end);
        ServerSocket ss = factory.createServerSocket(0);
        int p = factory.getLastPort();
        ss.close();
        return(p);
    }
    
    /**
     * Will return an array of URLs based on the input String array. If the array 
     * element is a file, the fully qualified file path must be provided. If the
     * array element is a directory, a fully qualified directory path must be 
     * provided, and the directory will be searched for all .jar and .zip files
     * 
     * @param elements A String array of fully qualified directory path
     * 
     * @return An URL[] elements
     * 
     * @throws MalformedURLException if any of the elements cannot be converted
     */
    public static URL[] toURLs(String[] elements)throws MalformedURLException {
        ArrayList<URL> list = new ArrayList<URL>();
        if(elements!=null) {
            for (String el : elements) {
                File element = new File(el);
                if (element.isDirectory()) {
                    URL[] urls = scanDirectory(el);
                    list.addAll(Arrays.asList(urls));
                } else {
                    list.add(element.toURI().toURL());
                }
            }
        }
        return(list.toArray(new URL[list.size()]));
    }
    
    /**
     * Will return an array of URLs for all .jar and .zip files in the directory,
     * including the directory itself
     * 
     * @param dirPath A fully qualified directory path
     * 
     * @return A URL[] for all .jar and .zip files in the directory,
     *  including the directory itself
     * 
     * @throws MalformedURLException If elements in the directory cannot be
     * created into a URL
     */
    public static URL[] scanDirectory(String dirPath) throws MalformedURLException {
        File dir = new File(dirPath);
        if(!dir.isDirectory())
            throw new IllegalArgumentException(dirPath+" is not a directory");
        if(!dir.canRead())
            throw new IllegalArgumentException("No read permissions for "+dirPath);
        File[] files = dir.listFiles();
        ArrayList<URL> list = new ArrayList<URL>();
        
        list.add(dir.toURI().toURL());

        for (File file : files) {
            if (file.getName().endsWith(".jar") ||
                file.getName().endsWith(".JAR") ||
                file.getName().endsWith(".zip") ||
                file.getName().endsWith(".ZIP")) {

                if (file.isFile())
                    list.add(file.toURI().toURL());
            }
        }
        return(list.toArray(new URL[list.size()]));
    }
    
    /**
     * Convert a comma, space and/or {@link java.io.File#pathSeparator} 
     * delimited String to array of Strings
     *
     * @param arg The String to convert
     *
     * @return An array of Strings
     */    
    public static String[] toArray(String arg) {
        return toArray(arg, " ,"+File.pathSeparator);        
    }

    /**
     * Convert a delimited String to array of Strings
     *
     * @param arg The String to convert
     * @param delim the delimiters to use
     *
     * @return An array of Strings
     */
    public static String[] toArray(String arg, String delim) {
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
