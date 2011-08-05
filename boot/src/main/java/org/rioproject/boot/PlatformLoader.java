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
package org.rioproject.boot;

import groovy.lang.GroovyClassLoader;
import groovy.lang.GroovyObject;
import groovy.lang.MetaMethod;
import org.rioproject.RioVersion;
import org.rioproject.config.PlatformCapabilityConfig;
import org.rioproject.config.PropertyHelper;
import org.w3c.dom.*;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Parses platform configuration documents
 *
 * @author Dennis Reedy
 */
public class PlatformLoader {
    static final String COMPONENT = "org.rioproject.boot";
    static final Logger logger = Logger.getLogger(COMPONENT);

    /**
     * Parse the platform
     *
     * @param directory The directory to search for XML or groovy configuration documents
     *
     * @return An array of PlatformCapabilityConfig objects
     *
     * @throws Exception if there are errors parsing the configuration files
     */
    @SuppressWarnings("unchecked")
    public PlatformCapabilityConfig[] parsePlatform(String directory) throws Exception {
        if(directory == null)
            throw new IllegalArgumentException("directory is null");
        List<PlatformCapabilityConfig> platformList = new ArrayList<PlatformCapabilityConfig>();
        File dir = new File(directory);
        if(dir.exists()) {
            if(dir.isDirectory()) {
                if(dir.canRead()) {
                    File[] files = dir.listFiles();
                    for (File file : files) {
                        if (file.getName().endsWith("xml") ||
                            file.getName().endsWith("XML")) {
                            try {
                                platformList.addAll(
                                    parsePlatform(file.toURI().toURL()));
                            } catch (Exception e) {
                                logger.log(Level.WARNING,
                                           "Could not parse ["+file.getAbsolutePath()+"], " +
                                           "continue building platform",
                                           e);
                            }
                        } else if(file.getName().endsWith("groovy")) {
                            GroovyClassLoader gCL = new GroovyClassLoader(getClass().getClassLoader());
                            Class gClass = gCL.parseClass(file);
                            GroovyObject gO = (GroovyObject)gClass.newInstance();
                            String methodName = null;
                            for(Object o : gO.getMetaClass().getMethods()) {
                                MetaMethod m = (MetaMethod)o;
                                if(m.getName().startsWith("getPlatformCapabilityConfig")) {
                                    methodName = m.getName();
                                    break;
                                }
                            }
                            if(methodName==null) {
                                logger.warning("The "+file.getName()+" class " +
                                               "does not contain a " +
                                               "getPlatformCapabilityConfig() " +
                                               "or getPlatformCapabilityConfigs() " +
                                               "method ");
                                continue;
                            }
                            Object[] args = {};
                            try {
                                Object result = gO.invokeMethod(methodName, args);
                                if(result!=null) {
                                    if(result instanceof Collection) {
                                        Collection c = (Collection)result;
                                        for(Object o : c) {
                                            if(!(o instanceof PlatformCapabilityConfig)) {
                                                logger.warning("The "+file.getName()+
                                                               "."+methodName+"() " +
                                                               "method returned a collection of " +
                                                               "invalid type(s). " +
                                                               "The "+o.getClass().getName()+" " +
                                                               "type is not allowed");
                                                break;
                                            }
                                        }
                                        platformList.addAll(c);
                                    } else if(result instanceof PlatformCapabilityConfig) {
                                        platformList.add((PlatformCapabilityConfig)result);
                                    } else {
                                        logger.warning("The "+file.getName()+
                                                       "."+methodName+"() returned " +
                                                       "an unsupported type: "+
                                                       result.getClass().getName());
                                    }
                                }
                            } catch(Exception e) {
                                Throwable t = e.getCause()==null?e:e.getCause();
                                logger.warning("The "+file.getName()+" class " +
                                               "is in error. " +
                                               t.getClass()+": "+t.getMessage());
                            }
                        }
                    }
                } else {
                    logger.warning("No read permissions for platform " +
                                   "directory ["+directory+"]");
                }
            } else {
                logger.warning("Platform directory ["+dir+"] " +
                               "is not a directory");
            }
        } else {
            logger.warning("Platform directory ["+directory+"] not found");
        }
        return(platformList.toArray(new PlatformCapabilityConfig[platformList.size()]));
    }

    /*
     * Parse the platform
     */
    Collection<PlatformCapabilityConfig> parsePlatform(URL configURL) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        InputStream is = configURL.openStream();
        Collection<PlatformCapabilityConfig> caps = new ArrayList<PlatformCapabilityConfig>();
        try {
            Document document = builder.parse(is);
            Element element = document.getDocumentElement();
            if ((element != null) && element.getTagName().equals("platform")) {
                caps.addAll(visitElement_platform(element,
                                                  configURL.toExternalForm()));
            }
        } finally {
            is.close();
        }

        return(caps);
    }
    
    /*
     * Scan through Element named platform.
     */
    Collection<PlatformCapabilityConfig> visitElement_platform(Element element,
                                                 String configFile) {
        List<PlatformCapabilityConfig> capabilities = new ArrayList<PlatformCapabilityConfig>();
        NodeList nodes = element.getChildNodes();
        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);
            if(node.getNodeType()==Node.ELEMENT_NODE) {
                Element nodeElement = (Element)node;
                if (nodeElement.getTagName().equals("capability")) {
                    PlatformCapabilityConfig cap = visitElement_capability(nodeElement);
                    if(cap.getPath()!=null) {
                        File file = new File(cap.getPath());
                        if(file.exists())
                            capabilities.add(cap);
                        else
                            logger.warning("Platform configuration " +
                                           "for ["+cap+"] not loaded, " +
                                           "the path ["+cap.getPath()+"] " +
                                           "does not exist. Make sure the " +
                                           "configuration file " +
                                           "["+configFile+"] " +
                                           "is correct, or delete the file " +
                                           "if it no longer references a " +
                                           "valid capability");
                    } else if(cap.getClasspath()!=null) {
                        String[] classpath = cap.getClasspath();
                        boolean okay = true;
                        String failedClassPathEntry = null;
                        for(String s : classpath) {
                            File file = new File(s);
                            if(!file.exists()) {
                                failedClassPathEntry = file.getName();
                                okay = false;
                                break;
                            }
                        }
                        if(okay)
                            capabilities.add(cap);
                        else {
                            StringBuilder sb = new StringBuilder();
                            for(String s : cap.getClasspath()) {
                                if(sb.length()>0)
                                    sb.append(" ");
                                sb.append(s);
                            }
                            logger.warning("Platform configuration " +
                                           "for ["+cap+"] not loaded, " +
                                           "could not locate classpath " +
                                           "entry ["+failedClassPathEntry+"]. The "+
                                           "classpath ["+sb.toString()+"] " +
                                           "is invalid. Make sure the " +
                                           "configuration file " +
                                           "["+configFile+"] is " +
                                           "correct, or delete the file " +
                                           "if it no longer references a " +
                                           "valid capability");
                        }
                    } else {
                        capabilities.add(cap);
                    }
                }
            }
        }
        return(capabilities);
    }
    
    /*
     * Scan through Element named capability.
     */
    PlatformCapabilityConfig visitElement_capability(Element element) { // <capability>
        // element.getValue();
        PlatformCapabilityConfig cap = new PlatformCapabilityConfig();
        NamedNodeMap attrs = element.getAttributes();
        for (int i = 0; i < attrs.getLength(); i++) {
            Attr attr = (Attr)attrs.item(i);
            if (attr.getName().equals("common")) { // <capability common="???">
                cap.setCommon(attr.getValue());
            }
            if (attr.getName().equals("name")) { // <capability name="???">
                cap.setName(attr.getValue());
            }
            if (attr.getName().equals("class")) { // <capability class="???">
                cap.setPlatformClass(attr.getValue());
            }
        }
        NodeList nodes = element.getChildNodes();
        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);
            if(node.getNodeType()==Node.ELEMENT_NODE) {
                Element nodeElement = (Element)node;
                if(nodeElement.getTagName().equals("description")) {
                    cap.setDescription(getTextValue(nodeElement));
                }
                if(nodeElement.getTagName().equals("version")) {
                    cap.setVersion(getTextValue(nodeElement));
                }
                if(nodeElement.getTagName().equals("manufacturer")) {
                    cap.setManufacturer(getTextValue(nodeElement));
                }
                if(nodeElement.getTagName().equals("classpath")) {
                    cap.setClasspath(getTextValue(nodeElement));
                }
                if(nodeElement.getTagName().equals("path")) {
                    cap.setPath(getTextValue(nodeElement));
                }
                if(nodeElement.getTagName().equals("native")) {
                    cap.setNativeLib(getTextValue(nodeElement));
                }
                if(nodeElement.getTagName().equals("costmodel")) {
                    cap.setCostModelClass(getTextValue(nodeElement));
                }
            }
        }
        return(cap);
    }


    /**
     * Get the text value for a node
     *
     * @param node The Node to get the text value for
     * @return The text value for the Node, or a zero-length String if
     * the Node is not recognized
     */
    String getTextValue(Node node) {
        NodeList eList = node.getChildNodes();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < eList.getLength(); i++) {
            Node n = eList.item(i);
            if (n.getNodeType() == Node.ENTITY_REFERENCE_NODE) {
                sb.append(getTextValue(n));
            } else if (n.getNodeType() == Node.TEXT_NODE) {
                sb.append(n.getNodeValue());
            }
        }
        return(replaceProperties(sb.toString().trim()));
    }

    String replaceProperties(String arg) {
        return(PropertyHelper.expandProperties(arg, PropertyHelper.PARSETIME));
    }


    /**
     * Get the default platform configuration
     *
     * @param rioHome Home directory for Rio, must not be null and must exist
     *
     * @return An array of PlatformCapabilityConfig objects
     *
     * @throws IllegalArgumentException if rioHome is null and not running under
     * the StaticCybernode
     * @throws Exception if the rioHome does not exist
     */
    public PlatformCapabilityConfig[] getDefaultPlatform(String rioHome) throws Exception {
        if(rioHome==null) {
            if(System.getProperty("StaticCybernode")!=null)
                return new PlatformCapabilityConfig[0];
            else
                throw new IllegalArgumentException("RIO_HOME cannot be null. You " +
                                                   "must set it as a system property " +
                                                   "or it must be set in your " +
                                                   "environment");
        }
        File rioHomeDir = new File(rioHome);
        if(!rioHomeDir.exists())
            throw new Exception(rioHome+" does not exist");

        PlatformCapabilityConfig rioCap = new PlatformCapabilityConfig();
        File rioJar = new File(rioHomeDir, "lib"+File.separator+"rio.jar");
        rioCap.setCommon("yes");
        rioCap.setPlatformClass("org.rioproject.system.capability.software.RioSupport");
        rioCap.setName("Rio");
        rioCap.setVersion(RioVersion.VERSION);
        rioCap.setClasspath(rioJar.getAbsolutePath());

        PlatformCapabilityConfig jiniCap = new PlatformCapabilityConfig();
        File jskLibJar = new File(rioHomeDir, "lib"+File.separator+"jsk-lib.jar");
        jiniCap.setCommon("yes");
        jiniCap.setName("Apache River");
        jiniCap.setVersion("2.1.1");
        jiniCap.setClasspath(jskLibJar.getAbsolutePath());

        Collection<PlatformCapabilityConfig> c = new ArrayList<PlatformCapabilityConfig>();
        c.add(rioCap);
        c.add(jiniCap);
        return(c.toArray(new PlatformCapabilityConfig[c.size()]));
    }
    }
