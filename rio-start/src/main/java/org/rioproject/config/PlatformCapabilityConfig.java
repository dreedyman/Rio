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
package org.rioproject.config;

import org.rioproject.boot.BootUtil;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.StringTokenizer;

/**
 * Contains attributes for a platform capability.
 */
public class PlatformCapabilityConfig {
    String name;
    String description;
    String manufacturer;
    String version;
    String classpath;
    String path;
    String nativeLib;
    String common="yes";
    static String DEFAULT_PLATFORM_CLASS =
        "org.rioproject.system.capability.software.SoftwareSupport";
    String platformClass =DEFAULT_PLATFORM_CLASS;
    String costModelClass;

    public PlatformCapabilityConfig() {
    }

    public PlatformCapabilityConfig(String name,
                                    String version,
                                    String classpath) {
        this.name = name;
        this.version = version;
        this.classpath = classpath;
    }

    public PlatformCapabilityConfig(String name,
                                    String version,
                                    String description,
                                    String manufacturer,                                    
                                    String classpath) {
        this.name = name;
        this.description = description;
        this.manufacturer = manufacturer;
        this.version = version;
        this.classpath = classpath;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getManufacturer() {
        return manufacturer;
    }

    public void setManufacturer(String manufacturer) {
        this.manufacturer = manufacturer;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String[] getClasspath() {
        if(classpath==null)
            return(new String[0]);
        StringTokenizer st = new StringTokenizer(classpath, File.pathSeparator);
        String[] paths = new String[st.countTokens()];
        int n=0;
        while (st.hasMoreTokens ()) {
            paths[n++] = st.nextToken();
        }
        return paths;
    }

    public URL[] getClasspathURLs() throws MalformedURLException {
        String[] classpath = getClasspath();
        return(BootUtil.toURLs(classpath));
    }

    public void setClasspath(String classpath) {
        this.classpath = classpath;
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

    public String geCostModelClass() {
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
}
