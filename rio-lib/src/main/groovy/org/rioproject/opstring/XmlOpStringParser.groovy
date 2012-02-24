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
package org.rioproject.opstring

import java.lang.reflect.Method
import java.util.logging.Logger
import org.rioproject.associations.AssociationDescriptor

import org.rioproject.core.provision.SystemRequirements
import org.rioproject.core.provision.SystemRequirements.SystemComponent
import org.rioproject.exec.ExecDescriptor
import org.rioproject.exec.ServiceExecutor
import org.rioproject.opstring.OpStringLoader.XMLErrorHandler
import org.rioproject.resources.util.JavaEntityResolver
import org.rioproject.sla.SLA
import org.rioproject.sla.ServiceLevelAgreements
import org.rioproject.system.capability.software.SoftwareSupport
import org.rioproject.watch.ThresholdValues
import org.rioproject.watch.WatchDescriptor
import org.rioproject.opstring.handlers.*

/**
 * Handles the parsing of an XML OperationalString
 *
 * @author Jerome Bernard
 * @author Dennis Reedy
 */
class XmlOpStringParser extends AbstractOpStringParser implements OpStringParser {
    def xmlSource
    def ClassLoader loader
    /** An array of default export jar names to always include */
    def String[] defaultExportJars
    /** Default groups to use */
    def String[] defaultGroups
    def loadPath
    /** A table to store service associations before adding them to the OperationalString */
    def final Map<String, Map<String, AssociationDescriptor[]>> associationTable =
    new Hashtable<String, Map<String, AssociationDescriptor[]>>()
    def final Map<String, ClassBundle> resourceMap = new HashMap<String, ClassBundle>();
    def final Map<String, SystemRequirements> sysRequirementsMap = new HashMap<String, SystemRequirements>()
    /** A suitable Logger */
    def logger = Logger.getLogger("org.rioproject.opstring")
    def handlers = [
            'associations':           new AssociationsHandler(parser: this),
            'association':            new AssociationHandler(),
            'download':               new DownloadHandler(),
            'exec':                   new ExecHandler(),
            'data':                   new DataHandler(),
            'parameters':             new ParametersHandler(),
            'softwareload':           new SoftwareLoadHandler(parser: this),
            'faultdetectionhandler':  new FaultDetectionHandlerHandler(parser: this),
            'maxpermachine':          new MaxPerMachineHandler(),
            'organization':           new OrganizationHandler(),
            'include':                new IncludeHandler(parser: this),
            'cluster':                new ClusterHandler(),
            'codebase':               new CodebaseHandler(),
            'locators':               new LocatorsHandler(),
            'groups':                 new GroupsHandler(),
            'logging':                new LoggingHandler(),
            'rules':                  new RulesHandler()
    ]

    def init() {
        /* Clear any cached references */
        handlers.get('include').visited.clear()
    }
    
    def List<OpString> parse(theXmlSource,
                             ClassLoader theLoader,
                             boolean verify = false,
                             String[] exportJars = ParsedService.DEFAULT_EXPORT_JARS,
                             String[] groups = null,
                             path) {
        xmlSource = theXmlSource
        loader = theLoader
        defaultExportJars = exportJars
        defaultGroups = groups
        loadPath = path

        def errorHandler = new XMLErrorHandler()
        def entityResolver
        if (loader)
            entityResolver = new JavaEntityResolver(loader)
        else
            entityResolver = new JavaEntityResolver()
        def parser = new XmlParser(true, false)
        parser.errorHandler = errorHandler;
        parser.entityResolver = entityResolver

        if (xmlSource instanceof URL) {
            def root = parser.parse(xmlSource.openStream())
            return parseOperationalString(root, xmlSource)
        } else if (xmlSource instanceof File) {
            def f = xmlSource as File
            def root = parser.parse(f)
            return parseOperationalString(root, f.toURI().toURL())
        } else {
            throw new UnsupportedOperationException("Unknown source ${xmlSource.getClass.name}")
        }
    }

    /**
     * Parse on OperationalString from a Document
     *
     * @param root The DOM Node object from an XML document root
     * @param loadedFrom The URL the OperationalString is loaded from
     * @return An array of OperationalString objects parsed from an XML document loaded from the location.
     * @throws Exception If errors occur parsing the OperationalString
     */
    def List<OpString> parseOperationalString(root, loadedFrom) {
        /* Clear the resource and system requirement maps before each parse */
        resourceMap.clear()
        sysRequirementsMap.clear()

        return root.OperationalString.collect {
            def opString = new OpString(it.'@Name', loadedFrom)
            associationTable.put(opString.name, new Hashtable<String, AssociationDescriptor[]>())
            def gAttrs = new GlobalAttrs()

            parseElement(it, gAttrs, null, opString)
            /* Post processor for opstring associations */
            def aTable = associationTable.get(opString.name)
            if (aTable.size() > 0) {
                for (ServiceElement sElem : opString.services) {
                    def aDescs = aTable.get(sElem.name)
                    if (aDescs == null)
                        continue
                    for (AssociationDescriptor aDesc : aDescs) {
                        def associatedName = aDesc.name
                        def svcName = sElem.name
                        if (associatedName.equals(svcName))
                            throw new IllegalArgumentException(
                                "Invalid AssociationDescriptor : " +
                                "A service cannot have an association " +
                                "to itself")

                        String assocOpStringName = aDesc.operationalStringName
                        if (assocOpStringName == null) {
                            if (aDesc.interfaceNames.length == 0)
                                throw new IllegalArgumentException(
                                    "Invalid AssociationDescriptor : Unknown service interface")
                        }

                        /*
                        * The Association has declared ClassName, no need
                        * to find matching ServiceElement in an opstring
                        */
                        if (aDesc.interfaceNames.length > 0) {
                            if (aDesc.faultDetectionHandlerBundle == null)
                                aDesc.faultDetectionHandlerBundle = OpStringLoader.getDefaultFDH()
                            aDesc.groups = sElem.serviceBeanConfig.groups
                            aDesc.locators = sElem.serviceBeanConfig.locators
                            continue
                        }

                        if (assocOpStringName && !(assocOpStringName == opString.name)) {
                            if (opString.containsOperationalString(assocOpStringName)) {
                                def op = opString.getNestedOperationalString(assocOpStringName)
                                def sElem1 = op.getNamedService(associatedName)
                                if (sElem1 == null)
                                    throw new IllegalArgumentException(
                                        "Associated service [$associatedName] not in [$assocOpStringName] OperationalString")
                                setAssociationDescriptorAttrs(sElem1, aDesc)
                            } else {
                                throw new IllegalArgumentException(
                                    "OperationalString [$assocOpStringName] not included in [${opString.name}] OperationalString")
                            }
                        } else {
                            def sElem1 = opString.getNamedService(associatedName)
                            if (sElem1 == null)
                                throw new IllegalArgumentException(
                                    "Associated service [$associatedName] not in [$assocOpStringName] OperationalString")
                            setAssociationDescriptorAttrs(sElem1, aDesc)
                        }


                    }
                    sElem.associationDescriptors = aDescs
                }
            }
            return opString
        }
    }

    /**
     * Parse the SystemRequirements element
     *
     * @param element The SystemRequirements element
     * @param codebase The service codebase
     *
     * @return A parsed SystemRequirement
     *
     * @throws Exception if the system requirements element cannot be parsed
     */
    private SystemRequirements parseSystemRequirements(element, codebase, global, sDescriptor, opString) throws Exception {
        def sysRequirements = new SystemRequirements()
        String ref = element.'@ref'
        if (ref) {
            SystemRequirements refSystemRequirements = sysRequirementsMap[ref]
            if (refSystemRequirements == null)
                throw new IllegalArgumentException("Unknown SystemRequirement element with id=[$ref]")
            sysRequirements = merge(refSystemRequirements, sysRequirements)
        }
        element.Utilization.each {
            def sysSLA = parseSLA(it, true)
            def tVal = new ThresholdValues(sysSLA.lowThreshold, sysSLA.highThreshold)
            sysRequirements.addSystemThreshold(sysSLA.identifier, tVal)
        }
        element.SystemComponent.each {
            def name = it.'@Name'
            def className = it.'@ClassName'
            def table = getAttributeTable(it)
            SystemComponent sysComp = new SystemComponent(name, className, table)
            if (className) {
                def bundle = parseElement(it, global, sDescriptor, opString)
                if (bundle)
                    sysComp.classpath = bundle.JARs
            }
            def exec = it.Exec
            def execDescriptor
            if (exec.size() == 1) {
                execDescriptor = parseElement(exec[0], global, sDescriptor, opString)
            }

            def softwareDownload =  it.SoftwareLoad.collect {
                parseElement(it, global, sDescriptor, opString)
            }
            if (softwareDownload)
                sysComp.stagedSoftware = softwareDownload
            if (execDescriptor)
                sysComp.execDescriptor = execDescriptor
            sysRequirements.addSystemComponent(sysComp)
        }
        return sysRequirements
    }

    /**
     * Parse the Resources element
     * @param el The Resources Element
     * @param className The classname that has resources
     * @param codebase The address of the codebase which will serve up the resources
     * @return A List of ClassBundle objects containing values corresponding to the JAR and Native elements.
     */
    private List<ClassBundle> parseResources(el, List<String> classNames, String codebase, GlobalAttrs global) {
        logger.fine "Parsing bundle $el"

        if (el.'@ref') {
            def bundles = global.bundles[el.'@ref'].collect {
                if (classNames.size() > 1) {
                    logger.fine "Uh oh... What are we supposed to do in that case? Picking the first interface"
                    logger.fine "Class names are: $classNames"
                }
                new ClassBundle(classNames[0], it.JARNames, it.codebase)

            }
            if (bundles == null)
                throw new IllegalArgumentException("Unknown Resources element with id=[${el.'@ref'}]")
            return bundles
        } else {
            def jars = el.JAR.collect { it.text() } as String[]

            /* Create the ClassBundle */
            def bundles
            if (classNames.size() > 1) {
                bundles = classNames.collect {
                    new ClassBundle(it, jars, codebase)
                }
            } else if (classNames.size() == 1) {
                bundles = [new ClassBundle(classNames[0], jars, codebase)]
            } else {
                def declaredBundle = new ClassBundle()
                declaredBundle.addJARs(jars)
                declaredBundle.codebase = codebase
                bundles = [declaredBundle]
            }
            logger.fine "Found some new bundles: $bundles"

            if (el.'@id') {
                logger.fine "Saving bundle with id '${el.'@id'}'"
                global.bundles[el.'@id'] = bundles
            }
            return bundles
        }
    }

    private ClassBundle parseArtifact(artifactElement, List<String> classNames, GlobalAttrs global) {
        ClassBundle cb = null
        String artifact = null
        if(artifactElement.'@ref') {
            artifact = global.artifacts.get(artifactElement.'@ref')
            if(artifact==null)
                throw new IllegalArgumentException("Unknown Artifact with "+
                                                   "ref: "+
                                                   "[${artifactElement.'@ref'}]")
        } else  if(artifactElement.text().length()>0) {
            artifact = artifactElement.text()
        }
        if(artifact!=null) {
            cb = new ClassBundle(classNames.get(0))
            cb.artifact = artifact
            logger.fine "Created ClassBundle from artifact: ${cb}"
        }
        return cb
    }

    /*
     * Parse the Environment element
     * @param elem The Environment element
     * @return An Map of name value pairs
     */
    def Map<String, String> parseEnvironment(elem) {
        def map
        return elem.Property.each {
            map[it.Name] = it.Value
        }
        return map
    }


    /**
     * Parse the SLA element
     *
     * @param el The SLA Element
     * @param enforceRelativeValues Whether the values should be relative,
     * i.e. between 0.0 and 1.0. This will be true for SystemRequirements
     * @return The SLA object for the SLA Element
     *
     * @throws Exception if the SLA element cannot be parsed
     */
    private SLA parseSLA(el, boolean enforceRelativeValues) throws Exception {
        def identifier = el.'@ID'
        def sLow = el.'@Low'
        def sHigh = el.'@High'

        double low = Double.NaN
        if (sLow)
            low = Double.parseDouble(sLow)

        if (enforceRelativeValues && sLow) {
            if (low < 0.0 || low > 1.0) {
                logger.severe("A value for the low range is inaccurate, must be between 0.0 and 1.0")
                throw new IllegalArgumentException("Bad low value, must be between 0.0 and 1.0")
            }
        }
        double high = Double.NaN
        if (sHigh)
            high = Double.parseDouble(sHigh)

        if (enforceRelativeValues && sHigh) {
            if (high < 0.0 || high > 1.0) {
                logger.severe("A value for the high range is inaccurate, must be between 0.0 and 1.0")
                throw new IllegalArgumentException("Bad high value, must be between 0.0 and 1.0")
            }
        }
        if (!Double.isNaN(low) && low > high)
            throw new IllegalArgumentException("Bad range, low value must be less then the high range value")

        // get the SLA policy handler details
        def type
        def max
        def className
        def lowerDampener
        def upperDampener

        el.PolicyHandler.each {
            type = it.'@type'
            className = it.'@handler'
            max = it.'@max'
            lowerDampener = it.'@lowerDampener'
            upperDampener = it.'@upperDampener'
        }

        // get declared <Monitor> elements
        def watchDescList = el.Monitor.collect { element ->
            def name = element.'@name'
            def property = element.'@property'
            def sPeriod = element.'@period'
            def objectName = element.'@objectName'
            def attribute = element.'@attribute'
            def period
            try {
                period = new Long(sPeriod)
            } catch (NumberFormatException e) {
                logger.warning("A value for the [$name] Monitor period is inaccurate [$sPeriod], must be a number")
                throw new IllegalArgumentException("A value for the [$name] Monitor period is inaccurate [$sPeriod], must be a number")
            }

            WatchDescriptor wDesc = new WatchDescriptor(name,
                                                        WatchDescriptor.Type.GAUGE,
                                                        property,
                                                        period)

            if (objectName)
                wDesc.objectName = objectName
            if (attribute)
                wDesc.attribute = attribute

            return wDesc
        }

        SLA sla = new SLA(identifier, low, high)
        sla.slaPolicyHandler = getSLAPolicyHandler(type, className)

        if (max)
            sla.maxServices = Integer.parseInt(max)
        if (lowerDampener)
            sla.lowerThresholdDampeningTime = Long.parseLong(lowerDampener)
        if (upperDampener)
            sla.upperThresholdDampeningTime = Long.parseLong(upperDampener)

        sla.watchDescriptors = watchDescList
        return sla
    }

    /**
     * Parse the FaultDetectionHandler element
     *
     * @param element The Element containing the fault detection handler
     * declaration
     * @param codebase The codebase to use
     *
     * @return A ClassBundle containing the fault detection handler attributes
     *
     * @throws Exception if the ClassBundle cannot be created
     */
    private ClassBundle parseFDH(element, String codebase, global, sDescriptor, opString) throws Exception {
        def className = element.'@ClassName'
        def classNameList = [className]
        def bundles = parseResources(element, classNameList, codebase, global)
        if (bundles==null) {
            bundles = [new ClassBundle(className)]
        }

        boolean setConfiguration = false
        ClassBundle bundle = bundles.get(0)
        element.Configuration.each {
            String[] configArgs = parseConfiguration(it, global, sDescriptor, opString)
            Object[] arg = [configArgs]
            bundle.addMethod("setConfiguration", arg)
            setConfiguration = true
        }
        if (!setConfiguration) {
            String[] empty = [ '-' ]
            Object[] arg = [empty]
            bundle.addMethod("setConfiguration", arg)
        }
        return bundle
    }

    /**
     * Parse a Configuration entry
     *
     * @param element The Element to parse
     *
     * @return A String[] suitable for use with Configuration object
     *
     * @throws Exception if the element cannot be parsed
     */
    private String[] parseConfiguration(element,
                                        global,
                                        sDescriptor, opString) throws Exception {
        logger.fine "Parsing configuration $element"
        boolean asOverride = true

        def configFile = element.'@file'
        if (configFile)
            return [ configFile ] as String[]

        def configList = element.Component.collect {
            def componentName = it.'@Name'
            def parms = ParametersHandler.parseParameters(it)
            parms.entrySet().collect {
                return createConfigEntry(componentName+'.'+it.getKey()+" = "+
                                         it.getValue())
            }
        }.flatten()

        if (element.children().size() == 1) {
            String content = element.text()
            if (content && !configList.contains(content)) {
                asOverride = false
                configList << content
            }
        }

        def configArgs = []
        if (asOverride) {
            configArgs << "-"
            for (int i = 0; i < configList.size(); i++) {
                configArgs << configList[i - 1]
            }
        } else {
            configArgs = configList
        }
        return configArgs as String[]
    }

    /**
     * Parse the Element
     *
     * @param element The Element
     * @param global Class to hold global elements
     * @param sDescriptor The parsedService object
     * @param opString The OpString object
     *
     * @throws Exception if the element cannot be parsed
     */
    def parseElement(element, GlobalAttrs global, ParsedService sDescriptor, OpString opString) throws Exception {
        logger.fine "Parsing element ${element.name()}"

        if (element.name() == "OperationalString") {
            [
                element.Include,
                element.Cluster,
                element.Codebase,
                element.Configuration,
                element.Groups,
                element.Locators,
                element.Logging,
                element.Organization,
                element.Parameters,
                element.Resources,
                element.Artifact,
                element.ServiceProvisionConfig,
                element.ServiceLevelAgreements,
                element.SystemRequirements,
                element.SLA,
                element.FaultDetectionHandler,
                element.Associations,
                element.Association,
                element.ServiceBean,
                element.ServiceExec,
                element.SpringBean
            ].each { tag ->
                if (tag.size() > 0) {
                    tag.each {
                        parseElement(it, global, sDescriptor, opString)
                    }
                }
            }


        } else if (element.name() == "ServiceBean" ||
                   element.name() == "ServiceExec" ||
                   element.name() == "SpringBean") {
            /* Verify minimal elements have been set */
            verify(element, global)
            ParsedService svcDescriptor = new ParsedService(element.'@Name', global)
            def jmxName = element.'@jmxname'
            if (jmxName)
                svcDescriptor.JMXName = jmxName
            svcDescriptor.operationalStringName = opString.name
            svcDescriptor.matchOnName = element.'@MatchOnName'
            svcDescriptor.provisionType = element.'@ProvisionType'
            if (global.autoAdvertise)
                svcDescriptor.autoAdvertise = element.'@AutoAdvertise'

            def fork = element.'@Fork'
            if(fork && fork.length() > 0)
                svcDescriptor.setFork(fork.equalsIgnoreCase("yes"));

            def jvmArgs = element.'@JVMArgs'
            if(jvmArgs && jvmArgs.length()>0) {
                if(!svcDescriptor.getFork()) {
                    logger.warning "Declaring JVM Options for a service that "+
                                   "is not declared to be forked will have "+
                                   "no effect"
                } else {
                    ExecDescriptor eDesc = new ExecDescriptor()
                    eDesc.setInputArgs(jvmArgs)
                    svcDescriptor.setExecDescriptor(eDesc)  
                }
            }

            def environment = element.'@Environment'
            if(environment && environment.length()>0) {
                if(!svcDescriptor.getFork()) {
                    logger.warning "Declaring Environment for a service that "+
                                   "is not declared to be forked will have "+
                                   "no effect"
                } else {
                    ExecDescriptor eDesc = svcDescriptor.getExecDescriptor()
                    if(eDesc==null)
                        eDesc = new ExecDescriptor()
                    Map<String, String> env = new HashMap<String, String>()
                    for(String s : environment.tokenize()) {
                        String[] parts = s.split("=")
                        env.put(parts[0], parts[1])
                    }
                    eDesc.setEnvironment(env) 
                    svcDescriptor.setExecDescriptor(eDesc)
                }
            }
            svcDescriptor.discoveryManagementPooling = element.'@DiscoveryManagementPooling'

            if (!element.name() == "ServiceExec") {
                String attr = element.'@Fork'
                if (attr)
                    svcDescriptor.fork = attr.equalsIgnoreCase("yes")
            }

            element.children().each { parseElement(it, global, svcDescriptor, opString) }

            if (element.name() == "ServiceExec") {
                ClassBundle cb = new ClassBundle(ServiceExecutor.class.name)
                svcDescriptor.componentBundle = cb
            }

            if(element.name() == "SpringBean") {
                def config = element.'@config'
                def st = new StringTokenizer(config, " \t\n\r\f,")
                def sb = new StringBuffer()
                sb.append("new String[]{")
                for (int i = 0; st.hasMoreTokens(); i++) {
                    if (i > 0)
                        sb.append(",")
                    sb.append("\"").append(st.nextToken()).append("\"")
                }
                sb.append("}")

                def springConfigParms = [
                                  'spring.config=' + sb.toString(),
                                  'spring.beanName="'+svcDescriptor.getName()+'"',
                                  'service.load.serviceBeanFactory=new org.rioproject.bean.spring.SpringBeanFactory()']
                def args = svcDescriptor.rawConfigParameters
                if (args.length == 0) {
                    svcDescriptor.setConfigParameters(['-'], false)
                }
                svcDescriptor.setConfigParameters(springConfigParms, true)

                if(svcDescriptor.componentBundle.artifact==null) {
                    def slas = svcDescriptor.serviceLevelAgreements
                    def sysComponents = slas.systemRequirements.systemComponents
                    def haveSpringRequirement = false
                    for (SystemComponent sysComp : sysComponents) {
                        def attrs = sysComp.attributes
                        def springKey = attrs['Name']
                        if (springKey && springKey.startsWith("Spring")) {
                            haveSpringRequirement = true
                            break
                        }
                    }
                    if (!haveSpringRequirement) {
                        def attrs = ['Name': 'Spring']
                        def springReq = new SystemComponent('SoftwareSupport', 
                                                            SoftwareSupport.getClass().name,
                                                            attrs)
                        slas.systemRequirements.addSystemComponent(springReq)
                        svcDescriptor.serviceLevelAgreements = slas
                    }
                }
            }
            
            logger.fine "svcDescriptor: $svcDescriptor"
            opString.addService(OpStringLoader.makeServiceElement(svcDescriptor, associationTable))

        } else if(element.name() == "ServiceProvisionConfig") {
            /* Ensure that we dont process the Element if the Element's parent
             * is a ServiceBean or a Service and the ParsedService is null. This
             * will happen if the Element is declared as a child of the
             * OperationalString (global configuration) and overridden in the
             * child Element */
            if(!((element.parent().name() == "ServiceBean" ||
                  element.parent().name() == "SpringBean") && sDescriptor == null)) {
                def config = element.Configuration.collect {
                    config = parseConfiguration(it, global, sDescriptor, opString)
                }.flatten()
                if (sDescriptor)
                    sDescriptor.serviceProvisionConfig = config
                else
                    global.serviceProvisionConfig = config
            }

        } else if (element.name() == "Configuration") {
            /* Since Configuration also appears as a child of other Elements,
             * make sure that the element parent is either an
             * OperationalString, Service or a ServiceBean
             *
             * Additionally, ensure that we do not process the Element if the
             * Element's parent is a ServiceBean and the ParsedService is null.
             * This will happen if the Element is declared as a child of the
             * OperationalString (global configuration) and overridden in the
             * child Element.
             */
            if((element.parent().name() == "OperationalString" ||
                element.parent().name() == "ServiceBean" ||
                element.parent().name() =="SpringBean") &&
                !((element.parent().name() == "ServiceBean" ||
                   element.parent().name() == "SpringBean") && sDescriptor == null)) {

                String includeGlobalDecl = element.'@IncludeGlobalDecl'
                boolean append = includeGlobalDecl == "yes"
                String[] config = parseConfiguration(element, global, sDescriptor, opString)
                if (sDescriptor)
                    sDescriptor.setConfigParameters(config, append)
                else
                    global.setConfigParameters(config, append)
            }
        } else if (element.name() == "Interfaces") {
            def interfaces = element.Interface.collect {
                it.text()
            }
            def bundles = []
            if(element.Resources.size()>0) {
                bundles = element.Resources.collect {
                    parseResources(it, interfaces, sDescriptor.getCodebase(), global)
                }.flatten()

                logger.fine "Found bundles: $bundles"

                /* if there is a set of default export jars, make sure they
                 * are included. If not included add them to the first bundle */
                boolean found = false
                if (defaultExportJars) {
                    List<String> toAdd = new ArrayList<String>()
                    for (ClassBundle bundle : bundles) {
                        for (String defaultExportJar : defaultExportJars) {
                            String[] jars = bundle.getJARNames()
                            for (String jar : jars) {
                                if (defaultExportJar.equals(jar)) {
                                    found = true
                                    break
                                }
                            }
                            if (!found)
                                toAdd << defaultExportJar
                        }
                    }
                    if(toAdd.size()>0) {
                        for (String jar : toAdd) {
                            bundles[0].addJAR(jar)
                        }
                    }
                }

            } else {
                if(element.Artifact.size()>0) {
                    ClassBundle cb = parseArtifact(element.Artifact[0], interfaces, global)
                    if(cb!=null)
                        bundles << cb
                }
            }

            /*
             * If there are no declared resources for an interface, create
             * empty classbundles. This will happen if the opstring
             * creator expects all classes to be loaded from the classpath
             * (RIO-145) */
            if(bundles.size()==0) {
                for(String iname : interfaces) {
                    bundles.add(new ClassBundle(iname))
                }
            }

            /* RIO-174: Temporarily merge the ClassBundles. This addresses the
             * issue where multiple resources may be declared for one interface.
             * This will not be the solution if there needs to be > 1 interface
             * classes supported */
            //TODO: Support multiple interface classes
            if(bundles.size()>1)
                bundles = ClassBundle.merge(bundles as ClassBundle[])
            
            sDescriptor.interfaceBundles = bundles as ClassBundle[]

        } else if (element.name() == "ImplementationClass") {
            String className = element.text()
            if (className.length() == 0)
                className = element.'@Name'

            if(element.Resources.size()>0) {
                def bundles = element.Resources.collect {
                    parseResources(it, [className], sDescriptor.getCodebase(), global)
                }.flatten()
                sDescriptor.componentBundle = ClassBundle.merge(bundles as ClassBundle[])
            } else {
                ClassBundle cb = null
                if(element.Artifact.size()>0)
                    cb = parseArtifact(element.Artifact[0], [className], global)
                if(cb!=null)
                    sDescriptor.componentBundle = cb
                else
                    sDescriptor.componentBundle = new ClassBundle()
            }


            /* Just in case there are no resources declared for the component bundle,
             * make sure we have a classname. This will happen if the opstring
             * creator expects all classes to be loaded from the classpath
             * (RIO-145) */
            if(sDescriptor.componentBundle.className==null)
                sDescriptor.componentBundle.className = className
        } else if (element.name() == "ServiceLevelAgreements" ||
                   element.name() == "SystemRequirements" ||
                   element.name() == "SLA") {
            /* Ensure that we dont process the Element if the Element's parent
             * is a ServiceBean and the ParsedService is null. This will happen
             * if the Element is declared as a child of the OperationalString
             * (global configuration) and overridden in the child Element */
            if(!((element.parent().name() == "ServiceBean" ||
                  element.parent().name() == "SpringBean" ||
                  element.parent().name() == "ServiceExec") && sDescriptor == null)) {
                def codebase = sDescriptor == null ? global.codebase : sDescriptor.codebase
                if (element.name() == "ServiceLevelAgreements") {
                    def sla = new ServiceLevelAgreements()

                    element.SystemRequirements.each {
                        logger.fine "Parsing SystemRequirement $it"
                        def sysReq = parseSystemRequirements(it, codebase, global, sDescriptor, opString)
                        sla.serviceRequirements = sysReq
                    }
                    element.SLA.each {
                        logger.fine "Parsing SLA $it"
                        def parsedSLA = parseSLA(it, false)
                        sla.addServiceSLA(parsedSLA)
                    }

                    if (sDescriptor)
                        sDescriptor.serviceLevelAgreements = sla
                    //else
                    //    global.serviceLevelAgreements = sla
                } else if (element.name() == "SystemRequirements") {
                    if (element.parent().name() == "OperationalString") {
                        def id = element.'@id'
                        if (!id)
                            throw new IllegalArgumentException("SystemRequirements element " +
                                                               "declared as outer " +
                                                               "element must have an id")
                        SystemRequirements sysReq = parseSystemRequirements(element, codebase, global, sDescriptor, opString)
                        sysRequirementsMap.put(id, sysReq)
                    } else {
                        SystemRequirements sysReq = parseSystemRequirements(element, codebase, global, sDescriptor, opString)
                        if (sDescriptor.serviceLevelAgreements) {
                            SystemRequirements base = sDescriptor.serviceLevelAgreements.systemRequirements
                            sysReq = merge(base, sysReq)
                            sDescriptor.serviceLevelAgreements.serviceRequirements = sysReq
                        } else {
                            ServiceLevelAgreements sla = new ServiceLevelAgreements()
                            sla.serviceRequirements = sysReq
                            sDescriptor.srviceLevelAgreements = sla
                        }
                    }

                } else if (element.name() == "SLA") {
                    SLA parsedSLA = parseSLA(element, false)
                    if (sDescriptor) {
                        if (sDescriptor.serviceLevelAgreements) {
                            sDescriptor.serviceLevelAgreements.addServiceSLA(parsedSLA)
                        } else {
                            ServiceLevelAgreements sla = new ServiceLevelAgreements()
                            sla.addServiceSLA(parsedSLA)
                            sDescriptor.serviceLevelAgreements = sla
                        }
                    }
                }
            }
        } else if (element.name() == "Resources") {
            parseResources(element, [], global.codebase, global)

        } else if (element.name() == "Artifact") {
            String id = element.'@id'
            if(id.length()==0)
                throw new IllegalArgumentException("The Artifact element must "+
                                                   "have an id attribute if "+
                                                   "declared globally")
            global.artifacts.put(id, element.text())

        } else if (handlers.containsKey(element.name().toLowerCase())) {
            def handler = handlers[element.name().toLowerCase()]
            def options = [
                    'opstring': opString,
                    'global': global,
                    'serviceDescriptor': sDescriptor,
                    'parent': element.parent(),
                    'source': xmlSource,
                    'loader': loader,
                    'defaultExportJars': defaultExportJars,
                    'defaultGroups': defaultGroups,
                    'loadPath': loadPath
            ]
            logger.fine "Going to parse ${element.name()} with handler ${handler.getClass().name}"
            return handler.parse(element, options)
        } else {
            String value = element.text()
            if (value) {
                Method method = sDescriptor.getClass().getMethod("set" + element.name(), String.class)
                try {
                    method.invoke(sDescriptor, value)
                } catch (Exception e) {
                    if (e.getCause() && (e.getCause() instanceof Exception)) {
                        throw ((Exception)e.getCause())
                    } else {
                        throw e
                    }
                }
            }
        }
    }
}
