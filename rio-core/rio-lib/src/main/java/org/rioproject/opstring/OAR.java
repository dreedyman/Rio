/*
 * Copyright 2008 the original author or authors.
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
package org.rioproject.opstring;

import org.rioproject.resolver.RemoteRepository;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.net.JarURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

/**
 * Represents an OperationalString Archive (OAR).
 *
 * @author Dennis Reedy
 */
public class OAR implements Serializable {
    static final long serialVersionUID = 1L;
    private String name;
    private String version;
    private String opStringName;
    private URL url;
    private String deployDir;
    private String activationType;
    private String artifacts;
    private final Set<RemoteRepository> repositories = new HashSet<RemoteRepository>();

    public static final String OAR_NAME = "OAR-Name";
    public static final String OAR_VERSION = "OAR-Version";
    public static final String OAR_OPSTRING = "OAR-OperationalString";
    public static final String OAR_ACTIVATION = "OAR-Activation";
    public static final String OAR_ARTIFACTS = "OAR-Artifacts";
    public static final String AUTOMATIC="Automatic";
    public static final String MANUAL="Manual";
    
    /**
     * Create an OAR
     *
     * @param file The OperationalString Archive File
     *
     * @throws OARException If the manifest cannot be read
     * @throws IllegalArgumentException If the manifest is null
     */
    public OAR(File file) throws OARException {
        if(file==null)
            throw new IllegalArgumentException("file cannot be null");
        JarFile jar = null;
        try {
            jar = new JarFile(file);
            Manifest man = jar.getManifest();
            getManifestAttributes(man);
            loadRepositories(jar);
            url = file.toURI().toURL();
        } catch (IOException e) {
            throw new OARException("Problem processing "+file.getName(), e);
        } finally {
            try {
                if(jar!=null)
                    jar.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Create an OAR
     *
     * @param url The URL for the OperationalString Archive
     *
     * @throws OARException If the manifest cannot be read
     * @throws IllegalArgumentException If the url is null
     */
    public OAR(URL url) throws OARException {
        if(url==null)
            throw new IllegalArgumentException("url cannot be null");
        JarFile jar = null;
        try {
            URL oarURL;
            if(url.getProtocol().equals("jar")) {
                oarURL = url;
            } else {
                StringBuilder sb = new StringBuilder();
                sb.append("jar:").append(url.toExternalForm()).append("!/");
                oarURL = new URL(sb.toString());
            }
            JarURLConnection conn = (JarURLConnection)oarURL.openConnection();
            jar = conn.getJarFile();
            Manifest man = jar.getManifest();
            getManifestAttributes(man);
            loadRepositories(jar);
            jar.close();
            this.url = url;
        } catch (Exception e) {
            throw new OARException("Problem processing "+url.toExternalForm(), e);
        } finally {
            try {
                if(jar!=null)
                    jar.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Create an OAR
     *
     * @param manifest A Manifest to the oar
     *
     * @throws OARException If the manifest cannot be read
     * @throws IllegalArgumentException If the manifest is null
     */
    public OAR(Manifest manifest) throws OARException {
        getManifestAttributes(manifest);
    }

    /**
     * Get OAR attributes from the Manifest
     *
     * @param manifest A Manifest to the oar
     *
     * @throws OARException If the manifest cannot be read
     * @throws IllegalArgumentException If the manifest is null
     */
    private void getManifestAttributes(Manifest manifest) throws OARException {
        if(manifest==null)
            throw new IllegalArgumentException("manifest cannot be null");
        Attributes attrs = manifest.getMainAttributes();
        if(attrs==null)
            throw new OARException("Unable to process the OAR, " +
                                   "it has no manifest attributes");
        name = attrs.getValue(OAR_NAME);
        if(name==null)
            fillInAndThrow(OAR_NAME);
        version = attrs.getValue(OAR_VERSION);        
        opStringName = attrs.getValue(OAR_OPSTRING);
        if(opStringName==null)
            fillInAndThrow(OAR_OPSTRING);
        activationType = attrs.getValue(OAR_ACTIVATION);
        if(activationType==null)
            fillInAndThrow(OAR_ACTIVATION);
        artifacts = attrs.getValue(OAR_ARTIFACTS);
    }

    /**
     * Get the URL for the File that this OAR was created from.
     *
     * @return A URL for the File that this OAR was created from. if the OAR
     * was not created from a File, return null
     *
     * @throws MalformedURLException if the URL cannot be created
     */
    public URL getURL() throws MalformedURLException {
        return(url);
    }

    /**
     * Get the OAR-Name attribute
     *
     * @return The OAR-Name attribute
     */
    public String getName() {
        return name;
    }

    public String getVersion() {
        return version;
    }

    public String getArtifacts() {
        return artifacts;
    }

    /**
     * Get the OAR-OperationalString attribute
     *
     * @return The OAR-OperationalString attribute
     */
    public String getOpStringName() {
        return opStringName;
    }

    private void setRepositories(Collection<RemoteRepository> repositories) {
        if(repositories==null)
            throw new IllegalArgumentException("repositories must not be null");
        this.repositories.addAll(repositories);
    }

    public Collection<RemoteRepository> getRepositories() {
        Set<RemoteRepository> remoteRepositories = new HashSet<RemoteRepository>();
        remoteRepositories.addAll(repositories);
        return remoteRepositories;
    }

    /**
     * Get the name of the directory to store the OAR in
     *
     * @return The directory name to extract the OAR into. The OAR-Name
     * manifest attribute will be used to create a directory name the OAR
     * will be copied to and extracted. If the OAR-Name has spaces ' ' in it,
     * the spaces will be translated to '-' characters, ensuring there are no
     * issues with directory name creation and accessibility. If the
     * OAR-Version manifest entry has been provided, the version will be
     * appended to the translated name
     */
    public String getCompoundName() {
        String s = name.replace(' ', '-');
        if(version!=null)
           s = s+"-"+version;
        return(s);
    }

    /**
     * Set the deployment directory path
     *
     * @param deployDir The deployment directory path the OAR has been
     * extracted to
     */
    public void setDeployDir(String deployDir) {
        this.deployDir = deployDir;
    }

    /**
     * Get the deployment directory path
     *
     * @return The deployment directory path the OAR has been extracted to
     */
    public String getDeployDir() {
        return(deployDir);
    }

    /**
     * Get the activation type
     *
     * @return The activation type, either <tt>Automatic</tt> or <tt>Manual</tt>
     */
    public String getActivationType() {
        return(activationType);
    }

    public OperationalString[] loadOperationalStrings() throws OARException {
        return loadOperationalStrings(Thread.currentThread().getContextClassLoader());
    }

    public OperationalString[] loadOperationalStrings(ClassLoader loader) throws OARException {
        if(url==null)
            throw new OARException("Cannot load OperationalString(s), unknown URL in OAR");
        StringBuilder sb = new StringBuilder();
        String urlExternalForm = url.toExternalForm();
        if(!urlExternalForm.startsWith("jar:"))
            sb.append("jar:");
        sb.append(url.toExternalForm());
        if(!urlExternalForm.endsWith("!/"))
            sb.append("!/");
        sb.append(getOpStringName());
        try {
            URL opStringURL = new URL(sb.toString());
            OpStringLoader osl = new OpStringLoader(loader);
            return osl.parseOperationalString(opStringURL);
        } catch (Exception e) {
            throw new OARException("Unable to load OperationalStrings", e);
        }
    }

    public String toString() {
        return "OAR {" +
               "name='" + name + '\'' +
               ", version='" + version + '\'' +
               ", opStringName='" + opStringName + '\'' +
               ", url=" + (url==null?"uknown": url.toExternalForm()) +
               ", deployDir='" + deployDir + '\'' +
               ", activationType='" + activationType + '\'' +
               '}';
    }

    private void loadRepositories(JarFile jarFile) throws IOException {
        JarEntry repositoriesXML = jarFile.getJarEntry("repositories.xml");
        if(repositoriesXML!=null) {
            InputStream input = jarFile.getInputStream(repositoriesXML);
            RepositoryDecoder repositoryDecoder = new RepositoryDecoder();
            Collection<RemoteRepository> repositories = new ArrayList<RemoteRepository>();
            Collections.addAll(repositories, (RemoteRepository[]) repositoryDecoder.decode(input));
            setRepositories(repositories);
        }
    }

    private void fillInAndThrow(String name) throws OARException {
        String oarName = url==null?"": " ["+url.toExternalForm()+"]";
        throw new OARException("The "+name+" attribute was not found, invalid OAR"+oarName);
    }
}
