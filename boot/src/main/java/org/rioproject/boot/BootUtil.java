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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
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
     * Return the classpath for the provided JAR names. 
     * 
     * @param jars Array of JAR names
     * @return The classpath with system dependant path delimeters 
     */
    public static String getClasspath(String[] jars) {
        StringBuffer buffer = new StringBuffer();
        for(int i=0; i<jars.length; i++) {
            if(i>0)
                buffer.append(File.pathSeparator);
            buffer.append(jars[i]);
        }
        return(buffer.toString());        
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
        return(com.sun.jini.config.ConfigUtil.concat(new Object[] {
                "http://", 
                address,  
                ":", port,
                "/"+jar}));
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
        StringBuffer buffer = new StringBuffer();
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
     * Get an URL from a fully qualified file path
     * 
     * @param filePath A fully qualified file path
     * 
     * @return The URL from the file if the file exists
     * @throws MalformedURLException If the filePath cannot be converted
     */
    public static URL fileToURL(String filePath) throws MalformedURLException {
        File file = new File(filePath);
        return(file.toURI().toURL());
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

    /**
     * Read the properties file and return an array of String instances which
     * can be used as Jini configuration overrides
     *
     * @param propertiesFile A Properties file containing override declarations
     *
     * @return An array of String values suitable for use as Jini configuration
     * overrides. If the propeties file is empty return a zero-length array
     *
     * @throws NullPointerException if propertiesFile is null
     * @throws IOException if the Properties file cannot be read
     */
    public static String[] parseOverrides(String propertiesFile) throws
                                                               IOException {
        return(parseOverrides(propertiesFile, null));
    }

    /**
     * Read the properties file and return an array of String instances which
     * can be used as Jini configuration overrides
     *
     * @param propertiesFile A Properties file containing override declarations
     * @param components An array of components to match, maty be null. If not
     * null, then if a ket starts with an element in the provided components
     * array it will be p[art of the array of overrides returned. If this
     * parameter is null or zero-length, all property components are returned
     *
     * @return An array of String values suitable for use as Jini configuration
     * overrides. If there are no matching components or the propeties file is
     * empty return a zero-length array
     *
     * @throws NullPointerException if propertiesFile is null
     * @throws IOException if the Properties file cannot be read
     */
    public static String[] parseOverrides(String propertiesFile,
                                          String[] components) throws
                                                               IOException {
        if(propertiesFile==null)
            throw new NullPointerException("propertiesFile is null");
        ArrayList<String> list = new ArrayList<String>();
        Properties props = new Properties();
        props.load(new FileInputStream(propertiesFile));
        if(components!=null && components.length>0) {
            for(Enumeration e=props.propertyNames(); e.hasMoreElements();) {
                String key = (String)e.nextElement();
                for (String component : components) {
                    if (key.startsWith(component)) {
                        list.add(key + "=" + props.getProperty(key));
                    }
                }
            }
        } else {
            for(Enumeration e=props.propertyNames(); e.hasMoreElements();) {
                String key = (String)e.nextElement();
                list.add(key+"="+props.getProperty(key));
            }
        }
        return(list.toArray(new String[list.size()]));
    }

    /**
     * Assemble a String array suitable for use as Jini configuration options.
     * The String array whose first element is the configuration source and
     * remaining elements specify override values for entries
     *
     * @param configFile The location of the configuration file, must not be
     * null
     * @param overrides Array of elements specifying override values for
     * entries, mat be null
     *
     * @return A String array suitable for use as a Jini configuration
     *
     * @throws NullPointerException if configFile is null
     */
    public static String[] createConfigArgs(String configFile, String[] overrides) {
        String[] configArgs;
        if(overrides!=null && overrides.length>0) {
            configArgs = new String[overrides.length+1];
            configArgs[0] = configFile;
            System.arraycopy(overrides, 0, configArgs, 1, overrides.length);
        } else {
            configArgs = new String[]{configFile};
        }
        return(configArgs);
    }
}
