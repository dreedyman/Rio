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

import groovy.xml.MarkupBuilder
import java.util.jar.JarEntry
import java.util.jar.JarFile
import java.util.jar.Manifest
import java.util.logging.Level
import java.util.logging.Logger
import org.rioproject.RioVersion
import org.rioproject.resolver.Resolver
import org.rioproject.resolver.ResolverHelper

/**
 * A parser that handles the Groovy Domain Specific Language support for Rio
 *
 * @author Jerome Bernard
 * @author Dennis Reedy
 */
class GroovyDSLOpStringParser implements OpStringParser {
    Map<String, List<OpString>> nestedTable = new HashMap<String, List<OpString>>()
    def OpStringParser xmlParser = new XmlOpStringParser()
    def logger = Logger.getLogger(getClass().name);

    public List<OpString> parse(Object source,
                                ClassLoader loader,
                                boolean verify,
                                String[] defaultExportJars,
                                String[] defaultGroups,
                                Object loadPath) {
        logger.fine "Parsing source $source"
        ExpandoMetaClass.enableGlobally()

        def parent
        def debug = false
        def tempFile = File.createTempFile('rio-dsl', '.xml')
        def writer = new FileWriter(tempFile)
        def builder = new MarkupBuilder(writer)
        builder.setOmitNullAttributes(true)
        URL sourceLocation = null
        if(source instanceof URL) {
            sourceLocation = (URL)source
            source = ((URL)source).openStream()
        } else if(source instanceof File) {
            sourceLocation = ((File)source).toURL()
        }

        //println "==> Parsing source $source"
        Script dslScript
        if(loader==null)
            dslScript = new GroovyShell().parse(source)
        else
            dslScript = new GroovyShell(loader).parse(source)

        writer.write('<!DOCTYPE opstring PUBLIC "-/RIO//DTD" "http://www.rio-project.org/dtd/rio_opstring.dtd">\n')
        writer.write('<opstring>\n')

        dslScript.metaClass = createEMC(dslScript.class,
                                        builder, {
                                        ExpandoMetaClass emc ->
            def opStringName

            emc.deployment = { Map attributes, Closure cl ->
                opStringName = attributes.name
                nestedTable.put(opStringName, new ArrayList<OpString>())
                debug = attributes.debug == null? false : attributes.debug

                builder.OperationalString(Name: opStringName) {
                    cl()
                }

            }

            emc.groups = { String... groups ->
                builder.Groups {
                    groups.each { Group(it) }
                }
            }

            emc.locators = { String... locators ->
                builder.Locators {
                    locators.each { Locator(it) }
                }
            }

            emc.codebase = { String codebase ->
                builder.Codebase(codebase)
            }

            emc.cluster = { String... machines ->
                builder.Cluster {
                    machines.each { Machine(it) }
                }
            }

            emc.service = { Map attributes, Closure cl ->
                def oldParent = parent
                parent = "service"
                builder.ServiceBean(Name: attributes.name,
                                    ProvisionType: attributes.type,
                                    Fork: attributes.fork,
                                    JVMArgs: attributes.jvmArgs,
                                    Environment: attributes.environment) {
                    cl()
                }
                parent = oldParent
            }

            emc.serviceExec = { Map attributes, Closure cl ->
                builder.ServiceExec(Name: attributes.name,
                                    ProvisionType: attributes.type) {
                    cl()
                }
            }

            emc.spring = { Map attributes, Closure cl ->
                builder.SpringBean(Name: attributes.name,
                                   config: attributes.config,
                                   ProvisionType: attributes.type,
                                   Fork: attributes.fork,
                                   JVMArgs: attributes.jvmArgs,
                                   Environment: attributes.environment) {
                    cl()
                }
            }
            emc.interfaces = { Closure cl ->
                builder.Interfaces { cl() }
            }

            /* The DSL must support the declaration of an implementation with
             * no resources (RIO-145) */
            emc.implementation = { Map attributes ->
                builder.ImplementationClass(Name: attributes.class)
            }

            emc.implementation = { Map attributes, Closure cl ->
                builder.ImplementationClass(Name: attributes.class) { cl() }
            }

            emc.execute = { Map attributes ->
                builder.Exec() {
                    if (attributes.inDirectory)
                        WorkingDirectory(attributes.inDirectory)
                    if (attributes.pidFile)
                        PidFile(attributes.pidFile)
                    def String[] cmd = attributes.command.tokenize()
                    CommandLine(cmd[0])
                    if (cmd.size() - 1 > 0)
                        cmd[1..cmd.size() - 1].each { InputArg(' ' + it) }
                }
            }

            emc.execute = { Map attributes, Closure cl ->
                builder.Exec() {
                    if (attributes.inDirectory)
                        WorkingDirectory(attributes.inDirectory)
                    if (attributes.pidFile)
                        PidFile(attributes.pidFile)
                    def String[] cmd = attributes.command.tokenize()
                    CommandLine(cmd[0])
                    if (cmd.size() - 1 > 0)
                        cmd[1..cmd.size() - 1].each { InputArg(' ' + it) }
                    cl()
                }
            }

            emc.environment { Closure cl ->
                builder.Environment { cl() }
            }

            emc.property { Map attributes ->
                builder.Property (Name: attributes.name, Value: attributes.value)
            }


            emc.classes = { String... interfaceClasses ->
                interfaceClasses.each { builder.Interface(it) }
            }

            /*emc.artifact = { Map attributes, String artifact ->
                builder.Artifact(attributes, artifact)
            }*/
                    
            emc.artifact = { Map attributes, String... artifacts ->
                StringBuilder artifactBuilder = new StringBuilder()
                for(String artifact : artifacts) {
                    if(artifactBuilder.length()>0)
                        artifactBuilder.append(" ")
                    artifactBuilder.append(artifact)
                }
                builder.Artifact(attributes, artifactBuilder.toString())
            }

            emc.artifact = { String... artifacts ->
                StringBuilder artifactBuilder = new StringBuilder()
                for(String artifact : artifacts) {
                    if(artifactBuilder.length()>0)
                        artifactBuilder.append(" ")
                    artifactBuilder.append(artifact)
                }
                builder.Artifact(artifactBuilder.toString())
            }

            emc.artifact = { Map attributes ->
                builder.Artifact(attributes)
            }

            emc.resources = { String... resources ->
                builder.Resources {
                    resources.each {
                        //if(it && it.endsWith(".jar"))
                        if(it)
                            JAR(it)
                    }
                }
            }

            emc.resources = { Map attributes ->
                builder.Resources(attributes)
            }

            emc.resources = { Map attributes, String... resources ->
                builder.Resources(id: attributes.id) {
                    resources.each { JAR(it) }
                }
            }

            emc.className = { String classname ->
                builder.ClassName(classname)
            }

            emc.configuration = { String configuration ->
                builder.Configuration(configuration)
            }

            emc.comment = { String comment ->
                builder.Comment(comment)
            }

            emc.configuration = { Map<String, Map<String, String>> components  ->
                if (!components.empty) {
                    builder.Configuration {
                        components.each { String componentName, Map<String, String> values ->
                            builder.Component(Name: componentName) {
                                values.each { String paramKey, String paramValue ->
                                    builder.Parameter(Name: paramKey, Value: paramValue)
                                }
                            }
                        }
                    }
                }
            }

            emc.configuration = { Map<String, String> attributes  ->
                String config = attributes.get("file")
                if(config==null)
                    throw new DSLException(
                            "There is no corresponding file entry for this "+
                            "configuration element");
                builder.Configuration(file: config)
            }

            emc.associations = { Closure cl ->
                builder.Associations { cl() }
            }

            emc.association = { Map attributes ->
                def matchOnName = attributes.matchOnName == null? 'yes': (
                                  attributes.matchOnName? 'yes' : 'no')
                builder.Association(Name: attributes.name,
                                    Type: attributes.type,
                                    Interface: attributes.serviceType,
                                    Property: attributes.property,
                                    MatchOnName: matchOnName)

            }

            emc.association = { Map attributes, Closure cl ->
                def matchOnName = attributes.matchOnName == null? 'yes': (
                                  attributes.matchOnName? 'yes' : 'no')
                builder.Association(Name: attributes.name,
                                    Type: attributes.type,
                                    Interface: attributes.serviceType,
                                    Property: attributes.property,
                                    MatchOnName: matchOnName) {
                                        cl()
                                    }
            }

            emc.management = { Map attributes->
                builder.Management(Proxy: attributes.proxy,
                                   ProxyType: attributes.proxyType,
                                   Strategy: attributes.strategy,
                                   Filter: attributes.filter,
                                   Inject: attributes.inject,
                                   ServiceDiscoveryTimeout: attributes.serviceDiscoveryTimeout,
                                   ServiceDiscoveryTimeoutUnits: attributes.serviceDiscoveryTimeoutUnits)
            }

            emc.management = { Map attributes, Closure cl ->
                def unitsAttr = attributes.serviceDiscoveryTimeoutUnits == null? "minutes": attributes.units
                builder.Management(Proxy: attributes.proxy,
                                   ProxyType: attributes.proxyType,
                                   Strategy: attributes.strategy,
                                   Filter: attributes.filter,
                                   Inject: attributes.inject,
                                   ServiceDiscoveryTimeout: attributes.serviceDiscoveryTimeout,
                                   ServiceDiscoveryTimeoutUnits: unitsAttr) {
                                        cl()
                                    }
            }

            /*emc.serviceDiscoveryTimeout = { Map attributes ->
                def unitsAttr = attributes.units == null? "minutes": attributes.units
                builder.ServiceDiscovery(timeout: attributes.timeout, units: unitsAttr)
            }*/

            emc.maintain = { Integer maintain ->
                builder.Maintain(maintain)
            }

            emc.maxPerMachine = { Integer max ->
                builder.MaxPerMachine(max)
            }

            emc.maxPerMachine = { Map attributes, Integer max ->
                if (attributes.type)
                    builder.MaxPerMachine(max, type: attributes.type)
                else
                    builder.MaxPerMachine(max)
            }

            emc.systemRequirements = { Map attributes ->
                builder.SystemRequirements(ref: attributes.ref)
            }

            emc.systemRequirements = { Map attributes, Closure cl ->
                def oldParent = parent
                parent = 'systemRequirements'
                builder.SystemRequirements(id: attributes.id) {
                    cl();
                }
                parent = oldParent
            }

            emc.systemRequirements = { Closure cl ->
                def oldParent = parent
                parent = 'systemRequirements'
                builder.SystemRequirements {
                    cl();
                }
                parent = oldParent
            }

            emc.memory = { Map attributes ->
                builder.SystemComponent(Name: "Memory") {
                    Attribute(Name: 'Name', Value: 'Memory')
                    if(attributes.available)
                        Attribute(Name: 'Available', Value: attributes.available)
                    if(attributes.capacity)
                        Attribute(Name: 'Capacity', Value: attributes.capacity)
                }
            }

            emc.diskspace = { Map attributes ->
                builder.SystemComponent(Name: "StorageCapability") {
                    Attribute(Name: 'Name', Value: 'Disk')
                    if(attributes.available)
                        Attribute(Name: 'Available', Value: attributes.available)
                    if(attributes.capacity)
                        Attribute(Name: 'Capacity', Value: attributes.capacity)
                }
            }

            emc.utilization = { Map attributes ->
                builder.Utilization(ID: attributes.id, High: attributes.high, Low: attributes.low)
            }

            emc.platformRequirement = { Map attributes ->
                generateSystemRequirements(builder, attributes, parent)
            }

            emc.software = { Map attributes ->
                generateSystemRequirements(builder, attributes, parent)
            }

            emc.software = { Map attributes, Closure cl ->
                def componentName = attributes.type == null ? "SoftwareSupport" : attributes.type
                builder.SystemRequirements {
                    SystemComponent(Name: componentName) {
                        for(Map.Entry<String, String> entry : attributes.entrySet()) {
                            def skip = ['removeOnDestroy', 'type', 'classpathresource', 'overwrite']
                            if(entry.key in skip) {
                                continue;
                            }

                            def key = entry.key.substring(0,1).toUpperCase() + entry.key.substring(1);
                            def value = entry.value
                            Attribute(Name: key, Value: value)
                        }
                        def classpathresource = attributes.classpathresource == null? 'yes': (
                                                    attributes.classpathresource? 'yes' : 'no')
                        builder.SoftwareLoad(RemoveOnDestroy: attributes.removeOnDestroy ? 'yes' : 'no',
                                             ClasspathResource: classpathresource) {
                            cl()
                        }
                    }
                }
            }

            emc.install = { Map attributes ->
                builder.Download(InstallRoot: attributes.target,
                                 Unarchive: attributes.unarchive ? 'yes' : 'no',
                                 Overwrite: attributes.overwrite ? 'yes' : 'no',
                                 Source: '') {
                    Location(attributes.source)
                }
            }

            emc.postInstall = { Map attributes, Closure cl ->
                builder.PostInstall(RemoveOnCompletion: attributes.removeOnCompletion ? 'yes': 'no') { cl() }
            }

            emc.data = { Map attributes ->
                builder.Data(Unarchive: attributes.unarchive ? 'yes' : 'no',
                             Perms: attributes.perms) {
                    FileName('')
                    Source(attributes.source)
                    Target(attributes.target)
                }
            }

            emc.serviceLevelAgreements = { Closure cl ->
                builder.ServiceLevelAgreements { cl() }
            }

            emc.sla = { Map attributes, Closure cl ->
                builder.SLA(ID: attributes.id,
                            Low: attributes.low,
                            High: attributes.high) {
                    cl()
                }
            }

            emc.policy = { Map attributes ->
                builder.PolicyHandler(type: attributes.type,
                                      handler: attributes.handler,
                                      max: attributes.max,
                                      lowerDampener: attributes.lowerDampener,
                                      upperDampener: attributes.upperDampener)
            }
            emc.monitor = { Map attributes ->
                builder.Monitor(name: attributes.name,
                                objectName: attributes.objectName,
                                property: attributes.property,
                                attribute: attributes.attribute,
                                period: attributes.period)
            }

            emc.insert = {String fileName ->
                File file = null
                if(fileName.startsWith(File.separator)) {
                    file = new File(fileName)
                } else {
                    if (source instanceof File) {
                        file = new File(((File)source).parent, fileName);
                    } else if(sourceLocation!=null) {
                        file = new File(new File(sourceLocation.toURI()).parent,
                                        fileName)
                    }
                }
                if(file && file.exists()) {
                    if(file.name.endsWith(".groovy")) {
                        throw new DSLException("groovy inserts are not supported");
                    } else {
                        writer.write(file.text)
                    }
                } else {
                    throw new DSLException("Unable to resolve and include "+
                                           fileName+", parent source "+
                                           "["+source+"], location "+
                                           "["+sourceLocation+"]")
                }
            }

            emc.include = { String opStringRef ->
                def resolved = false
                def asXML = opStringRef.endsWith(".xml")

                if (source instanceof File)
                    if (new File(((File)source).parent, opStringRef).exists())
                        resolved = true

                def location = null
                File tempResolvedOpString = null
                if(opStringRef.indexOf(":")!=-1) {
                    Resolver r = ResolverHelper.getResolver()
                    URL u = r.getLocation(opStringRef, "oar")
                    if(u==null)
                        throw new DSLException("Unable to resolve artifact "+
                                               "${opStringRef}, using "+
                                               "Resolver ${r.getClass().name}, "+
                                               "user.dir=${System.getProperty("user.dir")}")
                    u = new URL("jar:"+u.toExternalForm()+"!/")
                    if(logger.isLoggable(Level.FINE))
                        logger.fine "OpStringRef resolved as ${u.toExternalForm()}"
                    JarURLConnection jarConn = (JarURLConnection)u.openConnection();
                    Manifest manifest = jarConn.getManifest();
                    OAR oar = new OAR(manifest)
                    String includeOpString = oar.opStringName
                    asXML = includeOpString.endsWith(".xml")
                    JarFile oarJar = jarConn.getJarFile()
                    JarEntry entry = oarJar.getJarEntry(includeOpString)
                    int ndx = includeOpString.lastIndexOf(".")
                    if(ndx!=-1)
                        includeOpString = includeOpString.substring(0, ndx)
                    tempResolvedOpString = File.createTempFile(includeOpString+"_", ".groovy")
                    tempResolvedOpString.deleteOnExit()
                    def file = new FileOutputStream(tempResolvedOpString)
                    def out = new BufferedOutputStream(file)
                    out << oarJar.getInputStream(entry)
                    out.close()
                    location = tempResolvedOpString
                    resolved = true
                }

                if (resolved) {
                    if (source instanceof File && tempResolvedOpString==null) {
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
                    throw new DSLException("Unable to resolve and include "+
                                           opStringRef)
                def includes
                try {
                    if(asXML)
                        includes = xmlParser.parse(location,
                                                   loader,
                                                   false,
                                                   defaultExportJars,
                                                   defaultGroups,
                                                   loadPath)
                    else {
                        includes = parse(location,
                                         loader,
                                         false,
                                         defaultExportJars,
                                         defaultGroups,
                                         loadPath)
                        if(tempResolvedOpString!=null)
                            tempResolvedOpString.delete()
                    }
                    includes.each {
                        List<OpString> nested = nestedTable.get(opStringName)
                        nested.add(it)
                        //println ("\tAdd nested ["+it.name+"] to ["+opStringName+"] nested table, nested length="+nested.size())
                        nestedTable.put(opStringName, nested)
                    }

                } catch (Throwable t) {
                    logger.log(Level.SEVERE,
                               "Failed to include OperationalString : "+opStringRef,
                               t)
                }
            }
            emc.logging = { Closure cl ->
                builder.Logging { cl() }
            }

            emc.logger = { String name ->
                builder.Logger(Name: name, Level:Level.INFO) {
                    builder.Handler(ClassName: handler, Level:Level.INFO)
                }
            }

            emc.logger = { String name, Level level ->
                builder.Logger(Name: name, Level:level.toString()) {
                    builder.Handler(ClassName: 'java.util.logging.ConsoleHandler', Level:level.toString())
                }
            }

            emc.logger = { String name, String handler, Level level ->
                builder.Logger(Name: name, Level:level.toString()) {
                    builder.Handler(ClassName: handler, Level:level.toString())
                }
            }

            emc.parameters = { Closure cl ->
                builder.Parameters { cl() }
            }

            emc.parameter = { Map attributes ->
                builder.Parameter (Name: attributes.name, Value: attributes.value)
            }

            /* Used for rule declaration */
            emc.rules = { Closure cl->
                /* If there is no enclosing parent, then generate a Gnostic service */
                if(parent==null) {
                    service(name: 'Gnostic') {
                        interfaces {
                            classes 'org.rioproject.gnostic.Gnostic'
                            artifact "org.rioproject.gnostic:gnostic-api:${RioVersion.VERSION}"
                        }
                        implementation(class: 'org.rioproject.gnostic.GnosticImpl') {
                            artifact "org.rioproject.gnostic:gnostic-service:${RioVersion.VERSION}"
                        }
                        rules {
                            cl()
                        }
                        maintain 1
                    }
                } else {
                    builder.Rules { cl() }
                }
            }

            emc.rule = { Closure cl->
                builder.Rule { cl() }
            }

            emc.ruleClassPath = { String ruleClassPath ->
                builder.RuleClassPath(ruleClassPath)
            }

            emc.resource = { String resource ->
                builder.Resource(resource)
            }

            emc.serviceFeed = { Map attributes, Closure cl->
                builder.ServiceFeed(name: attributes.name, opstring: attributes.opstring) {
                    cl()
                }                
            }

            emc.watches = { String watches ->
                builder.Watches(watches)
            }

            emc.faultDetectionHandler = { String fdh ->
                builder.FaultDetectionHandler(ClassName: fdh)    
            }
        })

        dslScript.run()

        writer.write('</opstring>\n')
        writer.flush()
        writer.close()

        List<OpString> opstrings = xmlParser.parse(tempFile,
                                                   loader,
                                                   verify,
                                                   defaultExportJars,
                                                   defaultGroups,
                                                   loadPath)
        for(OpString os : opstrings) {
            for(Map.Entry<String, List<OpString>> entry : nestedTable.entrySet()) {
                if(sourceLocation!=null)
                    os.loadedFrom = sourceLocation
                String name = entry.key;
                List<OpString> nested = entry.value;
                //println("["+name+"], : "+nested);
                if(os.name.equals(name)) {
                    //println "!!!! Found nested List for ["+os.name+"]"
                    if(nested.size()>0) {
                        //for(OpString n : nested) {
                        //    println ("++++ Add nested ["+n.name+"] to ["+os.name+"]")
                        //    os.addOperationalString(n)
                        //}
                        os.addOperationalString(nested.toArray(new OpString[nested.size]))
                    }
                    break;
                }
            }
        }

        if(!debug)
            tempFile.delete()

        //println ("Leaving parse $source")

        if(source instanceof InputStream)
            ((InputStream)source).close()
        
        return opstrings
    }

    public parseElement(Object element, GlobalAttrs global, ParsedService sDescriptor, OpString opString) {
        throw new UnsupportedOperationException()
    }

    protected void processAdditionalTags(MarkupBuilder builder, ExpandoMetaClass emc) {
        // do nothing by default -- this is here so that subclasses can add additional behaviour!
    }

    Map<String, List<OpString>> getNestedTable() {
        return nestedTable
    }

    private generateSystemRequirements(MarkupBuilder builder, Map attributes, def parent) {
        def componentName = attributes.type == null ? "SoftwareSupport" : attributes.type
        if (!parent) {
            builder.SystemRequirements {
                generateSystemComponent(builder, componentName, attributes)
            }
        } else {
            if(parent.equals("service")) {
                builder.SystemRequirements {
                    generateSystemComponent(builder, componentName, attributes)
                }
            } else {
                generateSystemComponent(builder, componentName, attributes)
            }
        }
    }

    private generateSystemComponent(MarkupBuilder builder,
                                    def componentName,
                                    Map attributes) {
        builder.SystemComponent(Name: componentName) {
            for(Map.Entry<String, String> entry : attributes.entrySet()) {
                def key = entry.key.substring(0,1).toUpperCase() + entry.key.substring(1);
                def value = entry.value
                Attribute(Name: key, Value: value)
            }
        }
    }
    
    def ExpandoMetaClass createEMC(Class clazz, MarkupBuilder builder, Closure cl) {
        ExpandoMetaClass emc = new ExpandoMetaClass(clazz, false)
        cl(emc)
        processAdditionalTags(builder, emc)
        emc.initialize()
        return emc
    }

}
