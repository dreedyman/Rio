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
package org.rioproject.config;

import groovy.lang.GroovyClassLoader;
import groovy.lang.GroovyObject;
import groovy.lang.MetaMethod;
import org.rioproject.RioVersion;
import org.rioproject.resolver.Resolver;
import org.rioproject.resolver.ResolverException;
import org.rioproject.resolver.ResolverHelper;
import org.rioproject.util.PropertyHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.*;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * Parses platform configuration documents
 *
 * @author Dennis Reedy
 */
@SuppressWarnings("PMD.AvoidThrowingRawExceptionTypes")
public class PlatformLoader {
    static final String COMPONENT = "org.rioproject.boot";
    protected static final Logger logger = LoggerFactory.getLogger(COMPONENT);

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
                        if (file.getName().endsWith("xml") || file.getName().endsWith("XML")) {
                            try {
                                platformList.addAll(parsePlatform(file.toURI().toURL()));
                            } catch (Exception e) {
                                logger.warn("Could not parse ["+file.getAbsolutePath()+"], continue building platform", e);
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
                                logger.warn("The {} class does not contain a getPlatformCapabilityConfig() " +
                                            "or getPlatformCapabilityConfigs() method", file.getName());
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
                                                logger.warn("The {}.{}() method returned a collection of invalid type(s). " +
                                                            "The {} type is not allowed", file.getName(), methodName, o.getClass().getName());
                                                break;
                                            }
                                        }
                                        platformList.addAll(c);
                                    } else if(result instanceof PlatformCapabilityConfig) {
                                        platformList.add((PlatformCapabilityConfig)result);
                                    } else {
                                        logger.warn("The {}.{}() returned an unsupported type: {}",
                                                    file.getName(), methodName, result.getClass().getName());
                                    }
                                }
                            } catch(Exception e) {
                                Throwable t = e.getCause()==null?e:e.getCause();
                                logger.warn("The {} class is in error. {}:{}", file.getName(), t.getClass(), t.getMessage());
                            }
                        }
                    }
                } else {
                    logger.warn("No read permissions for platform directory [{}]", directory);
                }
            } else {
                logger.warn("Platform directory [{}] is not a directory", dir);
            }
        } else {
            logger.warn("Platform directory [{}] not found", directory);
        }
        return(platformList.toArray(new PlatformCapabilityConfig[platformList.size()]));
    }

    /*
     * Parse the platform
     */
    protected Collection<PlatformCapabilityConfig> parsePlatform(URL configURL) throws Exception {
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
    Collection<PlatformCapabilityConfig> visitElement_platform(Element element, String configFile) {
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
                            logger.warn("Platform configuration for [{}] not loaded, the path [{}] " +
                                        "does not exist. Make sure the configuration file [{}] " +
                                        "is correct, or delete the file if it no longer references a " +
                                        "valid capability", cap.toString(), cap.getPath(), configFile);
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
                            logger.warn("Platform configuration for [{}] not loaded, could not locate classpath " +
                                        "entry [{}]. The classpath [{}] is invalid. Make sure the configuration file " +
                                        "[{}] is correct, or delete the file if it no longer references a valid capability",
                                        cap.toString(), failedClassPathEntry, sb.toString(), configFile);
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
		List<String> classPath = new LinkedList<String>();
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
					visitElement_classpath(nodeElement, cap, classPath);
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
		postProcessClassPath(cap, classPath);
        return(cap);
    }

	/**
	 * Text node of classpath element is set as {@link PlatformCapabilityConfig#classpath} and all text nodes under classpath/entry are put to classPath parameter for further processing in {@link #postProcessClassPath}
	 *
	 * @param parentElement capability/classpath element
	 * @param cap           target capability object
	 * @param classPath     class path entry list built from classpath/entry
	 */
	void visitElement_classpath(Element parentElement, PlatformCapabilityConfig cap, List<String> classPath) {
		NodeList childNodes = parentElement.getChildNodes();
		for (int i = 0; i < childNodes.getLength(); i++) {
			Node node = childNodes.item(i);
			if (node.getNodeType() == Node.ELEMENT_NODE) {
				Element nodeElement = (Element) node;
				if ("entry".equals(nodeElement.getTagName())) {
					classPath.add(getTextValue(node));
				}
			} else if (node.getNodeType() == Node.TEXT_NODE) {
				cap.setClasspath(getTextValue(parentElement));
			}
		}
	}

	/**
	 * Resolve maven artifacts from classpath/entry nodes and set {@link PlatformCapabilityConfig#classpath}
	 * <p/>
	 * If artfact coordinates miss version, the {@link PlatformCapabilityConfig#version} is used
	 *
	 * @param cap       Capability config
	 * @param classPath list of classpath/entry values
	 */
	void postProcessClassPath(PlatformCapabilityConfig cap, List<String> classPath) {
		if (classPath.isEmpty()) {
			return;
		}
		Resolver resolver;
		try {
			resolver = ResolverHelper.getResolver();
		} catch (ResolverException e) {
			throw new IllegalStateException("Resolver not properly configured", e);
		}
		List<String> classPathList = new LinkedList<String>();
		for (String cpEntry : classPath) {
			classPathList.addAll(resolveArtifact(cpEntry, resolver, cap.getVersion()));
		}
		cap.setClasspath(join(classPathList));
	}

	/**
	 * Try to resolve {@code userClassPathEntry} as artifact coordinates, if successful, resolve its classpath. Otherwise just return userClassPathEntry so it's trated as a file path.
	 * <p/>
	 * If userClassPathEntry looks like artifactCoordinates without version (groupId:artifactId) the defaultVersion is used.
	 *
	 * @param userClassPathEntry text value of the classpath/entry element
	 * @param resolver           Artifact resolver
	 * @param defaultVersion     capability version
	 * @return classpath as returned by {@link Resolver#getClassPathFor(String)}
	 */
	List<String> resolveArtifact(String userClassPathEntry, Resolver resolver, String defaultVersion) {
		String[] strings = userClassPathEntry.split(":");
		if (strings.length > 1) {
			try {
				String coords = userClassPathEntry;
				if (strings.length == 2) {
					coords += ":" + defaultVersion;
				}
				String[] classPathEntries = resolver.getClassPathFor(coords);
				return Arrays.asList(classPathEntries);
			} catch (ResolverException e) {
				//entry isn't a path to existing file nor is a proper artifact - allow to fail on missing file.
				return Collections.singletonList(userClassPathEntry);
			}
		} else {
			return Collections.singletonList(userClassPathEntry);
		}
	}

	static String join(List<String> strings) {
		if (strings == null || strings.isEmpty()) {
			return "";
		}
		Iterator<String> iter = strings.iterator();
		StringBuilder result = new StringBuilder(iter.next());
		while (iter.hasNext()) {
			result.append(File.pathSeparator).append(iter.next());
		}
		return result.toString();
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
        if(System.getProperty("StaticCybernode")!=null)
            return new PlatformCapabilityConfig[0];
        if(rioHome==null) {
            throw new IllegalArgumentException("RIO_HOME cannot be null. You must set it as a system property " +
                                               "or it must be set in your environment");
        }
        File rioHomeDir = new File(rioHome);
        if(!rioHomeDir.exists())
            throw new Exception(rioHome+" does not exist");

        PlatformCapabilityConfig rioCap = new PlatformCapabilityConfig();
        File rioJar = new File(rioHomeDir, "lib"+File.separator+"rio-lib.jar");
        //File rioApiJar = new File(rioHomeDir, "lib-dl"+File.separator+"rio-api.jar");
        //Resolver resolver = ResolverHelper.getResolver();
        //String[] rioAPIClassPath = resolver.getClassPathFor("org.rioproject:rio-api:"+RioVersion.VERSION);

        rioCap.setCommon("yes");
        rioCap.setPlatformClass("org.rioproject.system.capability.software.RioSupport");
        rioCap.setName("Rio");
        rioCap.setVersion(RioVersion.VERSION);
        StringBuilder rioClassPath = new StringBuilder();
        rioClassPath.append(rioJar.getAbsolutePath());
        /*for(String s : rioAPIClassPath) {
            rioClassPath.append(File.pathSeparator);
            rioClassPath.append(s);
        }*/
        rioCap.setClasspath(rioClassPath.toString());

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
