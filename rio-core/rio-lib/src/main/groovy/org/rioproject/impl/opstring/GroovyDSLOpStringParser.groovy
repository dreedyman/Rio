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
package org.rioproject.impl.opstring

import net.jini.core.discovery.LookupLocator
import net.jini.core.entry.Entry
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.stmt.BlockStatement
import org.codehaus.groovy.ast.stmt.ExpressionStatement
import org.codehaus.groovy.ast.stmt.Statement
import org.codehaus.groovy.classgen.GeneratorContext
import org.codehaus.groovy.control.CompilationFailedException
import org.codehaus.groovy.control.CompilePhase
import org.codehaus.groovy.control.CompilerConfiguration
import org.codehaus.groovy.control.SourceUnit
import org.codehaus.groovy.control.customizers.CompilationCustomizer
import org.rioproject.RioVersion
import org.rioproject.associations.AssociationDescriptor
import org.rioproject.deploy.StagedData
import org.rioproject.deploy.StagedSoftware
import org.rioproject.deploy.SystemComponent
import org.rioproject.deploy.SystemRequirements
import org.rioproject.exec.ExecDescriptor
import org.rioproject.impl.exec.ServiceExecutor
import org.rioproject.log.LoggerConfig
import org.rioproject.opstring.ClassBundle
import org.rioproject.opstring.ServiceBeanConfig
import org.rioproject.opstring.ServiceElement
import org.rioproject.opstring.UndeployOption
import org.rioproject.resolver.Resolver
import org.rioproject.resolver.ResolverHelper
import org.rioproject.sla.RuleMap
import org.rioproject.sla.RuleMap.RuleDefinition
import org.rioproject.sla.RuleMap.ServiceDefinition
import org.rioproject.sla.SLA
import org.rioproject.system.SystemWatchID
import org.rioproject.system.capability.platform.OperatingSystem
import org.rioproject.system.capability.platform.ProcessorArchitecture
import org.rioproject.system.capability.platform.StorageCapability
import org.rioproject.system.capability.platform.SystemMemory
import org.rioproject.watch.ThresholdValues
import org.rioproject.watch.WatchDescriptor
import org.slf4j.LoggerFactory

import java.util.concurrent.TimeUnit
import java.util.jar.JarEntry
import java.util.jar.JarFile
import java.util.jar.Manifest
import java.util.logging.Level
/**
 * A parser that handles the Groovy Domain Specific Language support for Rio
 *
 * @author Jerome Bernard
 * @author Dennis Reedy
 */
class GroovyDSLOpStringParser implements OpStringParser {
    Map<String, List<OpString>> nestedTable = new HashMap<String, List<OpString>>()
    def logger = LoggerFactory.getLogger(getClass().name);

    @Deprecated
    public List<OpString> parse(final Object source,
                                final ClassLoader loader,
                                String[] defaultExportJars,
                                final String[] defaultGroups,
                                final Object loadPath) {
        return parse(source, loader, defaultGroups, loadPath)
    }

    public List<OpString> parse(final Object source,
                                final ClassLoader loader,
                                final String[] defaultGroups,
                                final Object loadPath) {
        logger.debug "Parsing source $source"
        ExpandoMetaClass.enableGlobally()

        def parent = null

        def compilerConfig = new CompilerConfiguration()
        compilerConfig.addCompilationCustomizers(
                new CompilationCustomizer(CompilePhase.CONVERSION) {
                    @Override
                    void call(SourceUnit sourceUnit,
                              GeneratorContext generatorContext,
                              ClassNode classNode) throws CompilationFailedException {
                        BlockStatement blockStatement = sourceUnit.getAST().getStatementBlock();
                        List<Statement> statements = blockStatement.getStatements();
                        UsesVisitor visitor = new UsesVisitor(sourceUnit.classLoader, statements)
                        for(Statement statement : statements) {
                            if(!(statement instanceof ExpressionStatement))
                                continue;
                            statement.visit(visitor);
                        }
                    }
                })
        URL sourceLocation = null
        GroovyCodeSource groovyCodeSource
        if(source instanceof URL) {
            sourceLocation = (URL)source
            groovyCodeSource = new GroovyCodeSource(sourceLocation)
        } else if(source instanceof File) {
            sourceLocation = ((File)source).toURI().toURL()
            groovyCodeSource = new GroovyCodeSource((File)source)
        } else if (source instanceof String) {
            groovyCodeSource = new GroovyCodeSource((String)source, "DynamicOpstring", "groovy/script")
        } else {
            throw new DSLException("Unrecognized source "+source)
        }

        Script dslScript
        if(loader==null)
            dslScript = new GroovyShell(compilerConfig).parse(groovyCodeSource)
        else
            dslScript = new GroovyShell(loader, new Binding(), compilerConfig).parse(groovyCodeSource)

        def opStrings = []
        OpStringParserHelper helper = new OpStringParserHelper()

        /* Global settings referenced for all services being parsed and built */
        Map<String, String> opStringArtifacts = new HashMap<String, String>()
        def opStringResources = [:]
        def globalSettings = [:]
        Map<String, SystemRequirements> systemRequirementsTable = new HashMap<String, SystemRequirements>()
        globalSettings[OpStringParserGlobals.ASSOCIATIONS] = []
        globalSettings[OpStringParserGlobals.LOGGERS] = []
        globalSettings[OpStringParserGlobals.GROUPS] = defaultGroups

        /* The 5 properties below represent in-flight objects that are referenced as the dsl gets parsed */
        ServiceElement currentService = null
        AssociationDescriptor currentAssociationDescriptor = null
        SystemComponent currentSoftwareSystemComponent = null
        SLA currentSLA = null

        dslScript.metaClass = createEMC(dslScript.class, { ExpandoMetaClass emc ->
            String opStringName = null
            OpString opString = null

            emc.uses = { String... s ->
            }

            emc.using = { String s ->
            }

            emc.using = { String... s ->
            }

            emc.deployment = { Map attributes, Closure cl ->
                opStringName = attributes.name
                globalSettings[OpStringParserGlobals.OPSTRING] = opStringName
                opString = new OpString(opStringName, sourceLocation)
                cl()
                opStrings << opString
            }

            emc.undeploy = { Map attributes ->
                Long when = attributes.get("idle")
                opString.undeployOption = new UndeployOption(when, UndeployOption.Type.WHEN_IDLE)
            }

            emc.undeploy = { Map attributes, TimeUnit timeUnit ->
                Long when = attributes.get("idle")
                opString.undeployOption = new UndeployOption(when, UndeployOption.Type.WHEN_IDLE, timeUnit)
            }

            emc.groups = { String... groups ->
                def groupNames = []
                groups.each { group ->
                    groupNames << group
                }
                if(currentService!=null) {
                    currentService.serviceBeanConfig.groups = groupNames as String[]
                } else {
                    globalSettings[OpStringParserGlobals.GROUPS] = groupNames
                }
            }

            emc.locators = { String... locators ->
                def lookupLocators = []
                locators.each { locator ->
                    if(!locator.startsWith("jini:"))
                        lookupLocators << new LookupLocator("jini://${locator}")
                    else
                        lookupLocators << new LookupLocator(locator)
                }
                if(currentService!=null) {
                    currentService.serviceBeanConfig.locators = lookupLocators
                } else {
                    globalSettings[OpStringParserGlobals.LOCATORS] = lookupLocators
                }
            }

            emc.codebase = { String codebase ->
                globalSettings[OpStringParserGlobals.CODEBASE] = codebase
            }

            emc.cluster = { String... machines ->
                if(currentService==null) {
                    globalSettings[OpStringParserGlobals.CLUSTER] = machines
                } else {
                    currentService.cluster = machines
                }
            }

            emc.service = { Map attributes, Closure cl ->
                def oldParent = parent
                parent = "service"
                currentService = new ServiceElement()
                helper.applyServiceElementAttributes(currentService, attributes, globalSettings)
                cl()
                opString.addService(currentService)
                parent = oldParent
            }

            emc.serviceExec = { Map attributes, Closure cl ->
                if(currentService!=null)
                    opString.addService(currentService)
                def oldParent = parent
                parent = "service"
                currentService = new ServiceElement()

                helper.applyServiceElementAttributes(currentService, attributes, globalSettings)
                ClassBundle componentBundle = new ClassBundle(ServiceExecutor.class.name)
                currentService.componentBundle = componentBundle
                cl()
                opString.addService(currentService)
                parent = oldParent
            }

            emc.spring = { Map attributes, Closure cl ->
                if(currentService!=null)
                    opString.addService(currentService)
                def oldParent = parent
                parent = "service"
                currentService = new ServiceElement()

                helper.applyServiceElementAttributes(currentService, attributes, globalSettings)

                def config = attributes.config
                def st = new StringTokenizer(config, " \t\n\r\f,")
                def sb = new StringBuffer()
                sb.append("new String[]{")
                for (int i = 0; st.hasMoreTokens(); i++) {
                    if (i > 0)
                        sb.append(",")
                    sb.append("\"").append(st.nextToken()).append("\"")
                }
                sb.append("}")

                def springConfigParms = ['-',
                        'spring.config=' + sb.toString(),
                        'spring.beanName="'+attributes.name+'"',
                        'service.load.serviceBeanFactory=new org.rioproject.impl.bean.spring.SpringBeanFactory()']

                currentService.serviceBeanConfig.setConfigArgs(springConfigParms as String[])
                cl()
                opString.addService(currentService)
                parent = oldParent
            }

            emc.interfaces = { Closure cl ->
                def oldParent = parent
                parent = "interfaces"
                cl()
                parent = oldParent
            }

            /* The DSL must support the declaration of an implementation with no resources (RIO-145) */
            emc.implementation = { Map attributes ->
                currentService.componentBundle = new ClassBundle(attributes.class)
            }

            emc.implementation = { Map attributes, Closure cl ->
                currentService.componentBundle = new ClassBundle(attributes.class)
                cl()
            }

            emc.execute = { Map attributes ->
                ExecDescriptor execDescriptor = new ExecDescriptor()
                if(attributes.inDirectory)
                    execDescriptor.workingDirectory = attributes.inDirectory
                if(attributes.pidFile)
                    execDescriptor.pidFile = attributes.pidFile
                if(attributes.command) {
                    helper.handleCommandLine(execDescriptor, attributes.command)
                }

                if(parent=="service") {
                    currentService.execDescriptor = execDescriptor
                } else {
                    StagedSoftware.PostInstallAttributes postInstall =
                        new StagedSoftware.PostInstallAttributes(execDescriptor, null)
                    currentSoftwareSystemComponent.stagedSoftware.postInstallAttributes = postInstall
                }
            }

            emc.execute = { Map attributes, Closure cl ->
                ExecDescriptor execDescriptor = new ExecDescriptor()
                if(attributes.inDirectory)
                    execDescriptor.workingDirectory = attributes.inDirectory
                if(attributes.pidFile)
                    execDescriptor.pidFile = attributes.pidFile
                if(attributes.command)
                    helper.handleCommandLine(execDescriptor, attributes.command)

                currentService.execDescriptor = execDescriptor

                cl()
            }

            emc.environment { Closure cl ->
                cl()
            }

            emc.property { Map attributes ->
                currentService.execDescriptor.getEnvironment().put(attributes.name, attributes.value)
            }

            emc.classes = { String... interfaceClasses ->
                def classBundles = []
                interfaceClasses.each{ interfaceClass ->
                    ClassBundle classBundle = new ClassBundle(interfaceClass)
                    classBundles << classBundle
                }
                currentService.setExportBundles(classBundles as ClassBundle[])
            }

            /* declared at the opstring level */
            emc.artifact = { Map attributes, String... artifacts ->
                StringBuilder artifactBuilder = new StringBuilder()
                for(String artifact : artifacts) {
                    if(artifactBuilder.length()>0)
                        artifactBuilder.append(" ")
                    artifactBuilder.append(artifact)
                }
                opStringArtifacts.put((String)attributes.get("id"), artifactBuilder.toString())
            }

            emc.artifact = { String... artifacts ->
                StringBuilder artifactBuilder = new StringBuilder()
                for(String artifact : artifacts) {
                    if(artifactBuilder.length()>0)
                        artifactBuilder.append(" ")
                    artifactBuilder.append(artifact)
                }
                if(parent=="interfaces") {
                    for(ClassBundle classBundle : currentService.getExportBundles()) {
                        classBundle.setArtifact(artifactBuilder.toString())
                    }
                } else {
                    currentService.componentBundle.artifact = artifactBuilder.toString()
                }
            }

            emc.artifact = { Map attributes ->
                String refId = attributes.get("ref")
                if(parent=="interfaces") {
                    for(ClassBundle classBundle : currentService.getExportBundles()) {
                        classBundle.setArtifact(opStringArtifacts.get(refId))
                    }
                } else {
                    currentService.getComponentBundle().setArtifact(opStringArtifacts.get(refId))
                }
            }

            emc.attributes = { Entry... entries ->
                currentService.getServiceBeanConfig().addAdditionalEntries(entries)
            }

            emc.attributes = { Entry[] entries, Closure cl ->
                currentService.getServiceBeanConfig().addAdditionalEntries(entries)
            }

            emc.resources = { String... resources ->
                helper.processResources(parent, globalSettings[OpStringParserGlobals.CODEBASE] as String, currentService, resources)
            }

            emc.resources = { Map attributes ->
                def resources = opStringResources[attributes.get("ref")] as String[]
                helper.processResources(parent, globalSettings[OpStringParserGlobals.CODEBASE] as String, currentService, resources)
            }

            emc.resources = { Map attributes, String... resources ->
                opStringResources.put(attributes.get("id"), resources)
            }

            emc.comment = { String comment ->
                currentService.serviceBeanConfig.configurationParameters.put(ServiceBeanConfig.COMMENT, comment)
            }

            //TODO: IS THIS STILL NEEDED?
            emc.className = { String classname ->
                println "===> parent: $parent, classname: $classname"
            }

            //TODO: IS THIS STILL NEEDED?
            emc.configuration = { Map<String, Map<String, String>> components  ->
                println "===> configuration: components:$components"
            }

            /* The configuration is being passed in as 'inline' */
            emc.configuration = { String configuration ->
                def configArg = [configuration]
                if(currentService!=null) {
                    currentService.serviceBeanConfig.configArgs = configArg as String[]
                } else {
                    globalSettings[OpStringParserGlobals.CONFIGURATION] = configArg as String[]
                }
            }

            emc.configuration = { Map<String, String> attributes  ->
                String config = attributes.get("file")
                def configArg = [config]
                if(currentService!=null) {
                    currentService.serviceBeanConfig.configArgs = configArg as String[]
                } else {
                    globalSettings[OpStringParserGlobals.CONFIGURATION] = configArg as String[]
                }
                if(config==null)
                    throw new DSLException("There is no corresponding file entry for this configuration element");
            }

            emc.associations = { Closure cl ->
                cl()
            }

            emc.association = { Map attributes ->
                currentAssociationDescriptor = helper.createAssociationDescriptor(attributes, opStringName)
                helper.addAssociationDescriptor(currentAssociationDescriptor,
                                                currentService,
                                                globalSettings[OpStringParserGlobals.ASSOCIATIONS])
            }

            emc.association = { Map attributes, Closure cl ->
                currentAssociationDescriptor = helper.createAssociationDescriptor(attributes, opStringName)
                cl()
                helper.addAssociationDescriptor(currentAssociationDescriptor,
                                                currentService,
                                                globalSettings[OpStringParserGlobals.ASSOCIATIONS])

            }

            emc.management = { Map attributes ->
                currentAssociationDescriptor = helper.createAssociationManagement(attributes, currentAssociationDescriptor)
            }

            emc.management = { Map attributes, Closure cl ->
                currentAssociationDescriptor = helper.createAssociationManagement(attributes, currentAssociationDescriptor)
                cl()
            }

            emc.maintain = { Integer maintain ->
                currentService.planned = maintain
            }

            emc.maxPerMachine = { Integer max ->
                currentService.maxPerMachine = max
            }

            emc.maxPerMachine = { Map attributes, Integer max ->
                if (attributes.type) {
                    currentService.machineBoundary = ServiceElement.MachineBoundary.valueOf(attributes.type.toUpperCase())
                }
                currentService.maxPerMachine = max
            }

            /* These are declared at the opstring level */
            emc.systemRequirements = { Map attributes, Closure cl ->
                def oldParent = parent
                parent = "systemRequirements:id=${attributes.id}"
                systemRequirementsTable[attributes.id] = new SystemRequirements()
                cl()
                parent = oldParent
            }

            /* These are declared at the service level */
            emc.systemRequirements = { Map attributes ->
                SystemRequirements systemRequirements = systemRequirementsTable[attributes.ref]
                SystemRequirements merged =
                    helper.merge(currentService.serviceLevelAgreements.systemRequirements, systemRequirements)
                currentService.serviceLevelAgreements.serviceRequirements = merged
            }

            /* These are declared at the service level */
            emc.systemRequirements = { Closure cl ->
                def oldParent = parent
                parent = 'systemRequirements'
                cl()
                parent = oldParent
            }

            emc.memory = { Map attributes ->
                Map attributeMap = [:]
                attributeMap["Name"] = SystemWatchID.SYSTEM_MEMORY
                attributeMap.putAll(helper.capitalizeFirstLetterOfEachKey(attributes))
                SystemComponent memory = new SystemComponent(SystemMemory.ID, SystemMemory.class.name, attributeMap)
                helper.addSystemComponent(parent, memory, systemRequirementsTable, currentService)
            }

            emc.diskspace = { Map attributes ->
                Map attributeMap = [:]
                attributeMap["Name"] = SystemWatchID.DISK_SPACE
                attributeMap.putAll(helper.capitalizeFirstLetterOfEachKey(attributes))
                SystemComponent diskspace = new SystemComponent(StorageCapability.ID, StorageCapability.class.name, attributeMap)
                helper.addSystemComponent(parent, diskspace, systemRequirementsTable, currentService)
            }

            emc.operatingSystem = { Map attributes ->
                Map attributeMap = [:]
                attributeMap["Name"] = attributes.name
                attributeMap["Version"] = attributes.version
                SystemComponent operatingSystem = new SystemComponent(OperatingSystem.ID, OperatingSystem.class.name, attributeMap)
                helper.addSystemComponent(parent, operatingSystem, systemRequirementsTable, currentService)
            }

            emc.processor = { Map attributes ->
                Map attributeMap = [:]
                attributeMap["Available"] = attributes.available
                SystemComponent processor = new SystemComponent(ProcessorArchitecture.ID, ProcessorArchitecture.class.name, attributeMap)
                helper.addSystemComponent(parent, processor, systemRequirementsTable, currentService)
            }

            emc.platformRequirement = { Map attributes ->
                helper.createAndAddSystemComponent(parent, attributes, systemRequirementsTable, currentService)
            }

            emc.software = { Map attributes ->
                helper.createAndAddSystemComponent(parent, attributes, systemRequirementsTable, currentService)
            }

            emc.utilization = { Map attributes ->
                SLA sysSLA = helper.createSLA(attributes, true)
                def tVal = new ThresholdValues(sysSLA.lowThreshold, sysSLA.highThreshold)

                if(helper.isSystemRequirementGlobal(parent)) {
                    String id =  helper.getSystemRequirementID(parent)
                    SystemRequirements systemRequirements = systemRequirementsTable[id]
                    systemRequirements.addSystemThreshold(sysSLA.identifier, tVal)
                } else {
                    currentService.serviceLevelAgreements.systemRequirements.addSystemThreshold(sysSLA.identifier, tVal)
                }
            }

            emc.software = { Map attributes, Closure cl ->
                currentSoftwareSystemComponent = helper.createSystemComponent(attributes,
                                                                              /* the following args are attributes to skip */
                                                                              "removeOnDestroy",
                                                                              "type",
                                                                              "classpathresource",
                                                                              "overwrite")
                StagedSoftware stagedSoftware = new StagedSoftware()
                stagedSoftware.useAsClasspathResource =
                    attributes.classpathresource == null? true: attributes.classpathresource
                stagedSoftware.removeOnDestroy = attributes.removeOnDestroy
                currentSoftwareSystemComponent.stagedSoftware = stagedSoftware
                cl()
                currentService.serviceLevelAgreements.systemRequirements.addSystemComponent(currentSoftwareSystemComponent)
            }

            emc.install = { Map attributes ->
                currentSoftwareSystemComponent.getStagedSoftware().location = attributes.source
                currentSoftwareSystemComponent.getStagedSoftware().installRoot = attributes.target
                currentSoftwareSystemComponent.getStagedSoftware().unarchive = attributes.unarchive
            }

            emc.postInstall = { Map attributes, Closure cl ->
                def oldParent = parent
                parent = "postInstall"
                cl()
                parent = oldParent
            }

            emc.data = { Map attributes ->
                StagedData stagedData = new StagedData()
                stagedData.location = attributes.source
                stagedData.installRoot = attributes.target
                stagedData.unarchive = attributes.unarchive
                stagedData.perms = attributes.perms
                if(attributes.removeOnDestroy)
                    stagedData.removeOnDestroy = attributes.removeOnDestroy
                if(attributes.overwrite)
                    stagedData.overwrite = attributes.overwrite
                currentService.stagedData = [stagedData]
            }

            emc.serviceLevelAgreements = { Closure cl ->
                cl()
            }

            emc.sla = { Map attributes, Closure cl ->
                currentSLA = helper.createSLA(attributes, false)
                cl()
                currentService.serviceLevelAgreements.addServiceSLA(currentSLA)
                currentSLA = null
            }

            emc.policy = { Map attributes ->
                currentSLA.slaPolicyHandler =
                    helper.getSLAPolicyHandler(attributes.type==null?attributes.handler:attributes.type)
                if(attributes.lowerDampener)
                    currentSLA.lowerThresholdDampeningTime = attributes.lowerDampener
                if(attributes.upperDampener)
                    currentSLA.upperThresholdDampeningTime = attributes.upperDampener
                if(attributes.max)
                    currentSLA.maxServices = attributes.max
            }

            emc.monitor = { Map attributes ->
                WatchDescriptor descriptor = new WatchDescriptor(attributes.name, attributes.period)
                if(attributes.property)
                    descriptor.property = attributes.property
                if(attributes.objectName)
                    descriptor.objectName = attributes.objectName
                if(attributes.attribute)
                    descriptor.attribute = attributes.attribute

                currentSLA.watchDescriptors = [descriptor]
            }

            emc.insert = {String fileName ->
                throw new DSLException("inserts are no longer supported")
            }

            emc.include = { String opStringRef ->
                def resolved = false
                if(opStringRef.endsWith(".xml"))
                    throw new DSLException("Unable to process XML documents.")

                if (source instanceof File) {
                    if (new File(((File)source).parent, opStringRef).exists())
                        resolved = true
                }

                def location = null
                def resolvedAsOAR = false
                if(opStringRef.indexOf(":")!=-1) {
                    Resolver r = ResolverHelper.getResolver()
                    URL u = r.getLocation(opStringRef, "oar")
                    if(u==null)
                        throw new DSLException("Unable to resolve artifact "+
                                               "${opStringRef}, using "+
                                               "Resolver ${r.getClass().name}, "+
                                               "user.dir=${System.getProperty("user.dir")}")
                    u = new URL("jar:"+u.toExternalForm()+"!/")
                    logger.debug "OpStringRef resolved as ${u.toExternalForm()}"
                    JarURLConnection jarConn = (JarURLConnection)u.openConnection();
                    Manifest manifest = jarConn.getManifest();
                    OAR oar = new OAR(manifest)
                    String includeOpString = oar.opStringName

                    JarFile oarJar = jarConn.getJarFile()
                    JarEntry entry = oarJar.getJarEntry(includeOpString)
                    String contents = oarJar.getInputStream(entry).text
                    location = contents
                    resolved = true
                    resolvedAsOAR = true
                }

                if (resolved) {
                    if (!resolvedAsOAR && source instanceof File) {
                        location = new File(((File)source).parentFile, opStringRef)
                    }
                } else {
                    if (!(opStringRef.startsWith("http:") || opStringRef.startsWith("file:")) && loadPath) {
                        if (loadPath.startsWith("file:")) {
                            location = loadPath.substring(5) + opStringRef
                        } else {
                            location = loadPath + opStringRef
                        }
                        location = new File(location)
                    }
                }

                if(location==null)
                    throw new DSLException("Unable to resolve and include "+opStringRef)
                def includes
                try {
                    includes = parse(location, loader, defaultGroups, loadPath)

                    includes.each {
                        List<OpString> nested = nestedTable.get(opStringName)
                        if(nested ==null)
                            nested = new ArrayList<OpString>();
                        nested.add(it)
                        //println ("\tAdd nested ["+it.name+"] to ["+opStringName+"] nested table, nested length="+nested.size())
                        nestedTable.put(opStringName, nested)
                    }

                } catch (Throwable t) {
                    logger.error("Failed to include OperationalString : "+opStringRef, t)
                }
            }
            emc.logging = { Closure cl ->
                cl()
            }

            emc.logger = { String name ->
                LoggerConfig loggerConfig = new LoggerConfig(name, Level.INFO)
                if(currentService!=null) {
                    currentService.serviceBeanConfig.addLoggerConfig(loggerConfig)
                } else {
                    globalSettings[OpStringParserGlobals.LOGGERS] << loggerConfig
                }
            }

            emc.logger = { String name, Level level ->
                LoggerConfig loggerConfig = new LoggerConfig(name, level)
                if(currentService!=null) {
                    currentService.serviceBeanConfig.addLoggerConfig(loggerConfig)
                } else {
                    globalSettings[OpStringParserGlobals.LOGGERS] << loggerConfig
                }
            }

            emc.logger = { String name, String handler, Level level ->
                LoggerConfig loggerConfig = new LoggerConfig(name,
                                                             level,
                                                             new LoggerConfig.LogHandlerConfig(handler, level))
                if(currentService!=null) {
                    currentService.serviceBeanConfig.addLoggerConfig(loggerConfig)
                } else {
                    globalSettings[OpStringParserGlobals.LOGGERS] << loggerConfig
                }
            }

            emc.parameters = { Closure cl ->
                cl()
            }

            emc.parameter = { Map attributes ->
                currentService.serviceBeanConfig.addInitParameter(attributes.name, attributes.value)
            }

            /* Used for rule declaration */
            emc.rules = { Closure cl->
                /* If there is no enclosing parent, then generate a Gnostic service */
                if(parent==null) {
                    currentService = new ServiceElement()
                    def attributes = [:]
                    attributes["name"] = "Gnostic"
                    helper.applyServiceElementAttributes(currentService, attributes, globalSettings)

                    ClassBundle exportBundle = new ClassBundle()
                    exportBundle.className = 'org.rioproject.gnostic.Gnostic'
                    exportBundle.artifact = "org.rioproject.gnostic:gnostic-api:${RioVersion.VERSION}"

                    ClassBundle componentBundle = new ClassBundle()
                    componentBundle.className = 'org.rioproject.gnostic.service.GnosticImpl'
                    componentBundle.artifact = "org.rioproject.gnostic:gnostic-service:${RioVersion.VERSION}"

                    currentService.componentBundle = componentBundle
                    currentService.exportBundles = [exportBundle]
                    currentService.planned = 1

                    cl()

                    currentService.setRuleMaps(helper.inFlightRuleMaps)
                    helper.inFlightRuleMaps.clear()
                    opString.addService(currentService)
                } else {
                    cl()
                    currentService.setRuleMaps(helper.inFlightRuleMaps)
                    helper.inFlightRuleMaps.clear()
                }
            }

            emc.rule = { Closure cl->
                helper.setInFlightRuleDefinition(new RuleDefinition())
                cl()
                RuleMap ruleMap = new RuleMap()
                ruleMap.addRuleMapping(helper.inFlightRuleDefinition,
                                       helper.inFlightRuleServiceDefinitions as ServiceDefinition[])
                helper.inFlightRuleMaps << ruleMap
                helper.inFlightRuleDefinition = null
                helper.inFlightRuleServiceDefinition = null
                helper.inFlightRuleServiceDefinitions.clear()
            }

            emc.ruleClassPath = { String ruleClassPath ->
                helper.getInFlightRuleDefinition().ruleClassPath = ruleClassPath
            }

            emc.resource = { String resource ->
                helper.getInFlightRuleDefinition().resource = resource
            }

            emc.serviceFeed = { Map attributes, Closure cl->
                helper.inFlightRuleServiceDefinition = new ServiceDefinition(attributes.name, attributes.opstring)

                cl()

                helper.inFlightRuleServiceDefinitions << helper.inFlightRuleServiceDefinition
            }

            emc.watches = { String watches ->
                helper.inFlightRuleServiceDefinition.addWatches(helper.toArray(watches))
            }

            emc.faultDetectionHandler = { String fdh ->
                println "FIX: faultDetectionHandler"
                //builder.FaultDetectionHandler(ClassName: fdh)
            }
        })

        dslScript.run()

       OpStringPostProcessor.process(opStrings)

        /* Process nested opstrings */
        for(OpString opString : opStrings) {
            for(Map.Entry<String, List<OpString>> entry : nestedTable.entrySet()) {
                String name = entry.key;
                List<OpString> nested = entry.value;
                if(opString.name.equals(name)) {
                    if(nested.size()>0) {
                        opString.addOperationalString(nested.toArray(new OpString[nested.size()]))
                    }
                    break;
                }
            }
        }

        //println ("Leaving parse $source")

        if(source instanceof InputStream)
            ((InputStream)source).close()
        
        return opStrings
    }

    protected void processAdditionalTags(ExpandoMetaClass emc) {
        // do nothing by default -- this is here so that subclasses can add additional behaviour!
    }
    
    def ExpandoMetaClass createEMC(Class clazz, Closure cl) {
        ExpandoMetaClass emc = new ExpandoMetaClass(clazz, false)
        cl(emc)
        processAdditionalTags(emc)
        emc.initialize()
        return emc
    }

}
