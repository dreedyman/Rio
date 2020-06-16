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

import java.util.jar.*;
import java.io.*;
import java.net.*;

/**
 * Utility for getting the Rio Attributes from a Jar File.
 * <br>
 * The Rio attributes are:<br>
 * <br>
 * "Rio-Build:", for example:<br>
 * <br>
 *  Manifest-Version: 1.0<br>
 *  Rio-Build: 20070602<br>
 *  <br>
 *
 * @author Jim Clarke
 * @author Dennis Reedy
 */
public class RioManifest {
    public static final Attributes.Name RIO_BUILD = new Attributes.Name("Rio-Build");
    private JarInputStream jarIn = null;
    /** Holds value of property manifest. */
    private Manifest manifest;
    
    public RioManifest(URL url) throws IOException {
        URL u = url;
        if(!u.getProtocol().equals("jar")) {
            u = new URL("jar:" + u.toExternalForm() + "!/");
        }
        JarURLConnection uc = (JarURLConnection) u.openConnection();
        this.manifest = uc.getManifest();
    }
    
    public void close() throws IOException {
        if(jarIn != null)
            jarIn.close();        
    }    

    /**
     * Get the Rio Build from a Jar file
     *
     * @return the Rio Build from a Jar file, or null if not defined.
     */
    public String getRioBuild() throws IOException {
        return getMainAttribute(RIO_BUILD);
    }
    
    /**
     * Get an Attribute from a Jar file
     *
     * @param name the name of the main attribute entry
     * @return the value of the main attribute from a Jar file, or null if not defined.
     */
    public String getMainAttribute(Attributes.Name name) throws IOException {
        if(manifest==null)
            throw new IllegalArgumentException("there is no manifest");
        Attributes attributes = manifest.getMainAttributes();
        if(attributes == null)
            return null;
        return (String)attributes.get(name);
    }
        
    /**
     * Get an Entry from a Jar file
     *
     * @param name the name of the entry
     * @return the attributes for the entry or null if not defined
     */
    public Attributes getEntry(String name) throws IOException {
        if(manifest==null)
            throw new IllegalArgumentException("there is no manifest");
        return manifest.getAttributes(name);
    }
}
