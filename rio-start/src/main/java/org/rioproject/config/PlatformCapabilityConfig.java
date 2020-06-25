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
package org.rioproject.config;

import org.rioproject.resolver.Artifact;
import org.rioproject.resolver.ResolverException;
import org.rioproject.resolver.ResolverHelper;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.StringTokenizer;

/**
 * Contains attributes for a platform capability.
 */
public class PlatformCapabilityConfig {
    private final String name;
    private final String description;
    private final String manufacturer;
    private final String version;
    private final String[] classpath;
    private String nativeLib;
    private String path;
    private String common="yes";
    static String DEFAULT_PLATFORM_CLASS = "org.rioproject.system.capability.software.SoftwareSupport";
    private String platformClass = DEFAULT_PLATFORM_CLASS;
    private String costModelClass;

    public PlatformCapabilityConfig(String name,
                                    String version,
                                    String classpath) throws ResolverException {
        this(name, version, null, null, classpath);
    }

    public PlatformCapabilityConfig(String name,
                                    String version,
                                    String description,
                                    String manufacturer,
                                    String classpath) throws ResolverException {
        this.name = name;
        this.description = description;
        this.manufacturer = manufacturer;
        this.version = version;
        if(classpath==null) {
            this.classpath = new String[0];
        } else if(Artifact.isArtifact(classpath)) {
            this.classpath = ResolverHelper.getResolver().getClassPathFor(classpath);
        } else {
            StringTokenizer st = new StringTokenizer(classpath, File.pathSeparator);
            String[] paths = new String[st.countTokens()];
            int n=0;
            while (st.hasMoreTokens ()) {
                paths[n++] = st.nextToken();
            }
            this.classpath = paths;
        }
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getManufacturer() {
        return manufacturer;
    }

    public String getVersion() {
        return version;
    }

    public String[] getClasspath() {
        return classpath;
    }

    public URL[] getClasspathURLs() throws MalformedURLException {
        String[] classpath = getClasspath();
        return(toURLs(classpath));
    }

    public String getNativeLib() {
        return nativeLib;
    }

    public void setNativeLib(String nativeLib) {
        this.nativeLib = nativeLib;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public boolean getCommon() {
        return Boolean.parseBoolean(common.equals("yes")?"true":"false");
    }

    public void setCommon(String common) {
        this.common = common;
    }

    public String getPlatformClass() {
        return platformClass;
    }

    public void setPlatformClass(String platformClass) {
        this.platformClass = platformClass;
    }

    public String getCostModelClass() {
        return costModelClass;
    }

    public void setCostModelClass(String costModelClass) {
        this.costModelClass = costModelClass;
    }    

    public String toString() {
        return "PlatformCapabilityConfig {" +
               "name='" + name + '\'' +
               ", description='" + description + '\'' +
               ", manufacturer='" + manufacturer + '\'' +
               ", version='" + version + '\'' +
               ", classpath='" + classpath + '\'' +
               ", path='" + path + '\'' +
               ", native='" + nativeLib + '\'' +
               ", common='" + common + '\'' +
               ", platformClass='" + platformClass + '\'' +
               ", costModelClass='" + costModelClass + '\'' +
               '}';
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
}
