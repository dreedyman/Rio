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
package org.rioproject.opstring;

import net.jini.core.discovery.LookupLocator;
import org.rioproject.associations.AssociationDescriptor;
import org.rioproject.config.Constants;
import org.rioproject.resolver.RemoteRepository;
import org.rioproject.url.artifact.ArtifactURLConfiguration;
import org.rioproject.util.PropertyHelper;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.logging.Logger;

/**
 * The OpStringLoader utility is a helper class used to parse, validate and
 * return an {@link OperationalString}.<br/>
 * This class searches in the given classloader for a resource named
 * <tt>/org/rioproject/opstring/OpStringParserSelectionStrategy</tt> in the classpath.
 * If found the content of that resource is supposed to indicate the name of the class
 * to use as an {@link OpStringParserSelectionStrategy}.
 *
 * @author Dennis Reedy
 * @author Jerome Bernard
 */
public class OpStringLoader {
    private boolean verify;
    private ClassLoader loader;
    private String[] exportJars;
    //private String codebaseOverride;
    private String[] groups;
    //private boolean processingOverrides;
    /** Path location of an OperationalString loaded from the file system */
    private String loadPath;
    /** A suitable Logger */
    private static Logger logger = Logger.getLogger("org.rioproject.opstring");
    /** Default FaultDetectionHandler */
    public static final String DEFAULT_FDH =
        "org.rioproject.fdh.AdminFaultDetectionHandler";
    /** Default groups to use */
    private static String[] defaultGroups = null;
    private static final String OPSTRING_PARSER_SELECTION_STRATEGY_LOCATION =
        "META-INF/org/rioproject/opstring/OpStringParserSelectionStrategy";

    /**
     * Simple constructor which instantiates an OpStringLoader object that
     * validates XML documents as they are parsed
     *
     * @throws Exception if there are errors creating the utility
     */
    public OpStringLoader() throws Exception {
        this(true, null);
    }

    /**
     * Create a new OpStringLoader, validating documents as they are parsed.
     * 
     * @param loader The parent ClassLoader to use for delegation
     *
     * @throws Exception if there are errors creating the utility
     */
    public OpStringLoader(ClassLoader loader) throws Exception {
        this(true, loader);
    }

    /**
     * Create a new OpStringLoader
     * 
     * @param verify If true specifies that the parser produced by this code
     * will validate documents as they are parsed.
     * @param loader The parent ClassLoader to use for delegation
     *
     * @throws Exception if there are errors creating the utility
     */
    public OpStringLoader(boolean verify, ClassLoader loader) throws Exception {
        this.verify = verify;
        this.loader = loader;
        this.exportJars = ParsedService.DEFAULT_EXPORT_JARS;
        String group = System.getProperty(Constants.GROUPS_PROPERTY_NAME);
        if (group != null)
            setDefaultGroups(new String[]{group});
    }

    /**
     * Set an array of jar names to be always included in a service's
     * declaration
     *
     * @param jars An array of jar names which will be verified as part of the
     * services interface resources
     */
    public void setDefaultExportJars(String[] jars) {
        if(jars==null)
            return;
        exportJars = new String[jars.length];
    }

    /**
     * Set the default groups to add into the parsed OperationalString.
     *
     * @param groups The groups to set, must not be null
     *
     * @throws IllegalArgumentException if the groups parameter is null or
     * if the groups parameter is a zero-length array
     */
    public void setDefaultGroups(String[] groups) {
        if(groups==null)
            throw new IllegalArgumentException("groups is null");
        if(groups.length == 0)
            throw new IllegalArgumentException("groups is empty");
        this.groups = new String[groups.length];
        System.arraycopy(this.groups, 0, groups, 0, groups.length);
    }

    /**
     * Parse on OperationalString from a File
     * 
     * @param file A File object for an XML or groovy file
     * @return An array of OperationalString objects
     * parsed from the file.
     *
     * @throws Exception if any errors occur parsing the document
     */
    public OperationalString[] parseOperationalString(File file) throws Exception {
        if (loadPath == null) {
            String path = file.getCanonicalPath();
            int index = path.lastIndexOf(System.getProperty("file.separator"));
            loadPath = path.substring(0, index + 1);
        }
        return parse(file);
    }

    /**
     * Parse on OperationalString from a URL location.
     * 
     * @param url URL location of the OperationalString
     * @return An array of OperationalString objects
     * parsed from the document loaded from the URL.
     *
     * @throws Exception if any errors occur parsing the document
     */
    public OperationalString[] parseOperationalString(URL url) throws Exception {
        if(url==null)
            throw new IllegalArgumentException("url is null");
        if (loadPath == null) {
            String path = url.toExternalForm();
            int index = path.lastIndexOf('/');
            loadPath = path.substring(0, index + 1);
        }
        return parse(url);
    }

    @SuppressWarnings("unchecked")
    private OperationalString[] parse(Object source) throws Exception {
        logger.fine("Parsing file " + source + "...");

        /* Search for a resource named
         * "/org/rioproject/opstring/OpStringParserSelectionStrategy" in the
         * classpath.
         *
         * If found read that resource and use the line of the resource as the
         * name of the class to use for selecting the {@link OpStringParser}
         * based on the source object.
         */
        OpStringParserSelectionStrategy selectionStrategy =
            new DefaultOpStringParserSelectionStrategy();
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        if (loader != null)
            cl = loader;
        URL propFile = cl.getResource(OPSTRING_PARSER_SELECTION_STRATEGY_LOCATION);
        if (propFile != null) {
            String strategyClassName;
            BufferedReader reader = null;
            try {
                reader = new BufferedReader(new InputStreamReader(propFile.openStream()));
                strategyClassName = reader.readLine();
            } finally {
                if (reader != null)
                    reader.close();
            }
            Class<OpStringParserSelectionStrategy> strategyClass =
                    (Class<OpStringParserSelectionStrategy>) cl.loadClass(strategyClassName);
            selectionStrategy = strategyClass.newInstance();
        }

        OpStringParser parser = selectionStrategy.findParser(source);
        /* If the parser has an init method, invoke it. This will
         * allow any previously cached results to be cleared */
        try {
            Method init = parser.getClass().getMethod("init");
            init.invoke(parser);
        } catch (Exception e) {
            //ignore
        }
        // parse the source
        List<OpString> opstrings = parser.parse(source,
                                                loader,
                                                verify,
                                                exportJars,
                                                groups,
                                                loadPath);
        return opstrings.toArray(new OperationalString[opstrings.size()]);
    }

    /**
     * Parse on OperationalString from a String location.
     * 
     * @param location String location of the file. The parameter
     * passed in can either point to an URL (prefixed by http) or a file found
     * in the classpath.
     * @return An array of OperationalString objects
     * parsed from an XML document loaded from the location.
     *
     * @throws Exception if any errors occur parsing the document 
     */
    private OperationalString[] parseOperationalString(String location) throws Exception {
        if(location==null)
            throw new IllegalArgumentException("location is null");
        URL url = getURL(location);
        if (url == null)
            throw new FileNotFoundException("OperationalString Location ["+location+"] not found");
        return parseOperationalString(url);
    }

    /**
     * Get the URL from a file or http connection
     *
     * @param location The location string
     *
     * @return A URL
     *
     * @throws MalformedURLException If the location string is bogus
     */
    private URL getURL(String location) throws MalformedURLException {
        URL url;
        if(location.startsWith("http") || location.startsWith("file:")) {
            url = new URL(location);
        } else {
            url = new File(location).toURI().toURL();
        }
        return(url);
    }

    /**
     * Create a ServiceElement object from a ParsedService
     *
     * @param parsedSvc A ParsedService object, typically from loading and
     * parsing an OperationalString document
     * @param associationTable A table of named associations and descriptors
     *
     * @return The ServiceElement object
     *
     * @throws Exception if errors occur parsing the service element
     */
    public static ServiceElement makeServiceElement(ParsedService parsedSvc,
                                                    Map<String, Map<String, AssociationDescriptor[]>> associationTable)
            throws Exception {

        ServiceBeanConfig sbConfig = makeServiceBeanConfig(parsedSvc);
        AssociationDescriptor[] aDescs = parsedSvc.getAssociationDescriptors();
        if (aDescs != null && aDescs.length > 0) {
            Map<String, AssociationDescriptor[]> aTable = associationTable.get(
                parsedSvc.getOperationalStringName());
                                      aTable.put(parsedSvc.getName(), aDescs);
            associationTable.put(parsedSvc.getOperationalStringName(),
                                 aTable);
        }

        ClassBundle fdhBundle = parsedSvc.getFaultDetectionHandlerBundle();
        if(fdhBundle==null)
            fdhBundle = getDefaultFDH();

        ClassBundle[] interfaceBundles = parsedSvc.getInterfaceBundles();
        List<RemoteRepository> remoteRepositories = new ArrayList<RemoteRepository>();
        for(ClassBundle interfaceBundle : interfaceBundles) {
            if(interfaceBundle.getArtifact()!=null) {
                ArtifactURLConfiguration artifactURLConfiguration = 
                    new ArtifactURLConfiguration(interfaceBundle.getArtifact());
                interfaceBundle.setArtifact(artifactURLConfiguration.getArtifact());
                if(artifactURLConfiguration.getRepositories().length>0) {
                    Collections.addAll(remoteRepositories, artifactURLConfiguration.getRepositories());
                }
            }
        }
        if(parsedSvc.getComponentBundle().getArtifact()!=null) {
            ArtifactURLConfiguration artifactURLConfiguration =
                new ArtifactURLConfiguration(parsedSvc.getComponentBundle().getArtifact());
            parsedSvc.getComponentBundle().setArtifact(artifactURLConfiguration.getArtifact());
            if(artifactURLConfiguration.getRepositories().length>0) {
                for(RemoteRepository r : remoteRepositories) {
                    boolean add = true;
                    for(RemoteRepository r2 : artifactURLConfiguration.getRepositories()) {
                        if(r2.getUrl().equals(r.getUrl())) {
                            add = false;
                            break;
                        }
                    }
                    if(add) {
                        remoteRepositories.add(r);
                    }
                }
            }
        }

        ServiceElement elem = new ServiceElement(parsedSvc.getProvisionType(),
                                                 sbConfig,
                                                 parsedSvc.getServiceLevelAgreements(),
                                                 interfaceBundles,
                                                 fdhBundle,
                                                 parsedSvc.getComponentBundle());
        elem.setRemoteRepositories(remoteRepositories);
        String maintain = parsedSvc.getMaintain();
        if(maintain==null)
            maintain = "0";
        elem.setPlanned(Integer.parseInt(maintain));
        elem.setCluster(parsedSvc.getCluster());
        String sMaxPerMachine = parsedSvc.getMaxPerMachine();
        if(sMaxPerMachine==null)
            sMaxPerMachine = "-1";
        elem.setMaxPerMachine(Integer.parseInt(sMaxPerMachine));
        elem.setMachineBoundary(parsedSvc.getMachineBoundary());
        elem.setMatchOnName(parsedSvc.getMatchOnName());
        elem.setAutoAdvertise(parsedSvc.getAutoAdvertise());
        elem.setDiscoveryManagementPooling(
            parsedSvc.getDiscoveryManagementPooling());
        elem.setExecDescriptor(parsedSvc.getExecDescriptor());
        elem.setStagedData(parsedSvc.getStagedData());
        elem.setFork(parsedSvc.getFork());
        elem.setRuleMaps(parsedSvc.getRuleMaps());
        return (elem);
    }

    /**
     * Create a ServiceBeanConfig from a ParsedService
     *
     * @param parsedService A ParsedService object, typically from loading and
     * parsing an XML OperationalString
     * @return The ServiceBeanConfig object
     *
     * @throws Exception if the service bean config cannot be created
     */
    private static ServiceBeanConfig makeServiceBeanConfig(ParsedService parsedService) throws Exception {
        if(parsedService==null)
            throw new IllegalArgumentException("parsedService is null");

        String component = getComponentName(parsedService);
        /* Create the configuration parameters */
        Map<String, Object> configParms = new HashMap<String, Object>();
        /* Set the config component */
        configParms.put(ServiceBeanConfig.COMPONENT, component);
        /* Get the service name */
        configParms.put(ServiceBeanConfig.NAME, parsedService.getName());
        /* Get the jmx name */
        if(parsedService.getJMXName() != null)
            configParms.put(ServiceBeanConfig.JMX_NAME,
                            parsedService.getJMXName());
        /* Get the service comment */
        if(parsedService.getComment()!= null)
            configParms.put(ServiceBeanConfig.COMMENT, parsedService.getComment());

        /* Get the opstring name */
        configParms.put(ServiceBeanConfig.OPSTRING,
                        parsedService.getOperationalStringName());

        /* Set the configured codebase */
        configParms.put(ServiceBeanConfig.CONFIGURED_CODEBASE,
                        parsedService.getCodebase());

        /* Set the provisioning configuration */
        configParms.put(ServiceBeanConfig.SERVICE_PROVISION_CONFIG,
                        parsedService.getServiceProvisionConfig());

        /* Get the organization name */
        if(parsedService.getOrganization() != null)
            configParms.put(ServiceBeanConfig.ORGANIZATION,
                            parsedService.getOrganization());

        /* Get the discovery groups*/
        String[] parsedGroups = parsedService.getGroups();
        if(parsedGroups.length==0)
            parsedGroups = defaultGroups;
        if(parsedGroups!=null) {
            for(int i=0; i<parsedGroups.length; i++) {
                parsedGroups[i] = replaceProperties(parsedGroups[i]);
            }
            configParms.put(ServiceBeanConfig.GROUPS, parsedGroups);
        }        

        /* Get Locator instances */
        net.jini.core.discovery.LookupLocator[] locators = null;
        if(parsedService.getLocators() != null) {
            locators = getLocators(parsedService);
        }
        if(locators!=null) {
            configParms.put(ServiceBeanConfig.LOCATORS, locators);
        }

        /* Get LoggerConfig objects */
        if (parsedService.getLogConfigs() != null)
            configParms.put(ServiceBeanConfig.LOGGER,
                            parsedService.getLogConfigs());

        /* Get the Configuration parameters */
        String[] configArgs = parsedService.getConfigParameters();
        /*
        * String[] configArgs = new String[configList.length+1]; configArgs[0] =
        * "-"; for(int i=1; i <configArgs.length; i++) configArgs[i] =
        * configList[i-1];
        */
        /* Create the ServiceBeanConfig object */
        ServiceBeanConfig sbConfig = new ServiceBeanConfig(configParms,
                                                           configArgs);
        /* Get the initialization parameters and add them */
        Properties initParms = parsedService.getParameters();
        for (Map.Entry<Object, Object> entry : initParms.entrySet()) {
            sbConfig.addInitParameter((String) entry.getKey(),
                                      entry.getValue());
        }
        return (sbConfig);
    }

    /*
     * Get locators
     */
    private static LookupLocator[] getLocators(ParsedService svc) throws MalformedURLException {
        String[] sLocators = svc.getLocators();
        net.jini.core.discovery.LookupLocator[] locators =
            new net.jini.core.discovery.LookupLocator[sLocators.length];
        for(int i = 0; i < locators.length; i++) {
            String l = sLocators[i];
            if(!l.startsWith("jini://"))
                l = "jini://"+l;
            locators[i] = new net.jini.core.discovery.LookupLocator(l);
        }
        return(locators);
    }

    /*
     * Get the component name from the parsed service.
     */
    private static String getComponentName(ParsedService parsedService) {
        String implClass;
        if (parsedService.getComponentBundle() != null) {
            implClass = parsedService.getComponentBundle().getClassName();
            if (implClass == null)
                throw new IllegalArgumentException("The component bundle must have a classname set: "
                        + parsedService.getComponentBundle());
        } else {
            ClassBundle[] bundles = parsedService.getInterfaceBundles();
            implClass = bundles[0].getClassName();
        }
        String componentName = implClass;
        int ndx = implClass.lastIndexOf(".");
        if (ndx != -1)
            componentName = implClass.substring(0, ndx);
        return componentName;
    }

    /*
     * Create the default FaultDetectionHandler
     */
    static ClassBundle getDefaultFDH() {
        ClassBundle fdhBundle = new ClassBundle(DEFAULT_FDH);
        String[] empty = new String[]{"-"};
        fdhBundle.addMethod("setConfiguration", new Object[]{empty});
        return(fdhBundle);
    }

    /**
     * An ErrorHandler for parsing
     */
    public static class XMLErrorHandler implements ErrorHandler {

        public void warning(final SAXParseException err) throws SAXException {
            System.out.println ("+++ Warning"
                                + ", line " + err.getLineNumber ()
                                + ", uri " + err.getSystemId ());
            System.out.println("   " + err.getMessage ());
        }

        public void error(final SAXParseException err) throws SAXException {
            System.out.println ("+++ Error"
                                + ", line " + err.getLineNumber ()
                                + ", uri " + err.getSystemId ());
            System.out.println("   " + err.getMessage ());
            throw err;
        }

        public void fatalError(final SAXParseException err) throws SAXException {
            System.out.println ("+++ Fatal"
                                + ", line " + err.getLineNumber ()
                                + ", uri " + err.getSystemId ());
            System.out.println("   " + err.getMessage ());
            throw err;
        }

    }

    public static void main(String[] args) {
        try {
            OpStringLoader loader = new OpStringLoader();
            if(args.length==0) {
                BufferedReader br =
                new BufferedReader(new InputStreamReader(System.in));
                try {
                    String pwd = System.getProperty("user.dir");
                    System.out.println("["+pwd+"]");

                    System.out.print("Enter OperationalString " +
                                     "to parse> ");
                    String opstring = br.readLine();
                    loader.parseOperationalString(opstring);
                } catch(Throwable t) {
                    t.printStackTrace();
                }

            } else {
                loader.parseOperationalString(args[0]);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Expand any properties in the String. Properties are declared with
     * the pattern of : <code>${property}</code>
     *
     * @param arg The string with properties to expand, must not be null
     * @return If the string has properties declare (in the form
     * <code>${property}</code>), return a formatted string with the
     * properties expanded. If there are no property elements declared, return
     * the original string.
     *
     * @throws IllegalArgumentException If a property value cannot be obtained
     * an IllegalArgument will be thrown
     */
    private static String replaceProperties(String arg) {
        return PropertyHelper.expandProperties(arg, PropertyHelper.PARSETIME);
    }

}
