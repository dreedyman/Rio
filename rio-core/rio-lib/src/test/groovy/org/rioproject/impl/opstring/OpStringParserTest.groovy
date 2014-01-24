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
import org.rioproject.associations.AssociationDescriptor
import org.rioproject.associations.AssociationType
import org.rioproject.deploy.StagedSoftware
import org.rioproject.deploy.SystemComponent
import org.rioproject.deploy.SystemRequirements
import org.rioproject.impl.associations.strategy.Utilization
import org.rioproject.impl.exec.ServiceExecutor
import org.rioproject.impl.sla.SLAPolicyHandler
import org.rioproject.impl.sla.ScalingPolicyHandler
import org.rioproject.opstring.ServiceElement
import org.rioproject.opstring.ServiceElement.MachineBoundary
import org.rioproject.opstring.UndeployOption
import org.rioproject.sla.SLA
import org.rioproject.sla.ServiceLevelAgreements
import org.rioproject.system.SystemWatchID
import org.rioproject.watch.ThresholdValues
import org.rioproject.watch.WatchDescriptor

import java.lang.management.ManagementFactory
import java.util.concurrent.TimeUnit

/**
 * Test groovy parsing
 *
 * @author Jerome Bernard
 * @author Dennis Reedy
 */
class OpStringParserTest extends GroovyTestCase {
    def OpStringParser dslParser = new GroovyDSLOpStringParser()

    void testAssociationVersion() {
        File file = new File("src/test/resources/opstrings/association_version.groovy")
        def opstrings = dslParser.parse(file, null, null, null)
        assertEquals "There should be one and only one opstring", 1, opstrings.size()
        OpString opstring = opstrings[0]
        ServiceElement serviceElement = opstring.services[0]
        assertEquals 1, serviceElement.associationDescriptors.length
        assertEquals "2.1", serviceElement.associationDescriptors[0].version
    }

    void testAddedAttributes() {
        File file = new File("src/test/resources/opstrings/attributes.groovy")
        def opstrings = dslParser.parse(file, null, null, null)
        assertEquals "There should be one and only one opstring", 1, opstrings.size()
        OpString opstring = opstrings[0]
        ServiceElement serviceElement = opstring.services[0]
        assertEquals "There should be 2 additional entries", 2,
                     serviceElement.getServiceBeanConfig().getAdditionalEntries().size()
    }

    void testUndeployOption() {
        File file = new File("src/test/resources/opstrings/attributes.groovy")
        def opstrings = dslParser.parse(file, null, null, null)
        assertEquals "There should be one and only one opstring", 1, opstrings.size()
        OpString opstring = opstrings[0]
        assertEquals "There should be an UndeployOption for IDLE", UndeployOption.Type.WHEN_IDLE,
                     opstring.undeployOption.type
        assertEquals "The UndeployOption IDLE option should be 30", 30,
                     opstring.undeployOption.when
        assertEquals "The UndeployOption IDLE option should be in SECONDS", TimeUnit.SECONDS,
                     opstring.undeployOption.timeUnit
    }

    void testAssociationWithNoOpStringFiltering() {
        File file = new File("src/test/resources/opstrings/monitorAssociation.groovy")
        def opstrings = dslParser.parse(file, null, null, null)
        assertEquals "There should be one and only one opstring", 1, opstrings.size()
        OpString opstring = opstrings[0]
        ServiceElement serviceElement = opstring.services[0]
        assertEquals 1, serviceElement.associationDescriptors.length
        AssociationDescriptor associationDescriptor = serviceElement.getAssociationDescriptors()[0]
        assertTrue associationDescriptor.operationalStringName==null
    }

    void testRangeParsing() {
        File file = new File("src/test/resources/opstrings/servicebeanRange.groovy")
        def opstrings = dslParser.parse(file, null, null, null)
        assertEquals "There should be one and only one opstring", 1, opstrings.size()
        OpString opstring = opstrings[0]
        assertEquals 50, opstring.services.length
    }

    void testSlaExample() {
        File file = new File("src/test/resources/opstrings/slaexample.groovy")
        def opstrings = dslParser.parse(file, null, null, null)
        assertEquals "There should be one and only one opstring", 1, opstrings.size()
        OpString opstring = opstrings[0]

        def service = opstring.services[0]
        assertEquals 1, service.serviceLevelAgreements.serviceSLAs.size()
        def messageCount = service.serviceLevelAgreements.serviceSLAs[0]
        assertEquals 'messageCount', messageCount.identifier
        assertEquals 1, messageCount.lowThreshold
        assertEquals 3, messageCount.highThreshold
        assertEquals SLAPolicyHandler.class.getName(), messageCount.slaPolicyHandler
        assertEquals 10, messageCount.lowerThresholdDampeningTime
        assertEquals 10, messageCount.upperThresholdDampeningTime
    }

    void testAssociationEmpty() {
        File file = new File("src/test/resources/opstrings/association_empty.groovy")
        def opstrings = dslParser.parse(file, null, null, null)
        assertEquals "There should be one and only one opstring", 1, opstrings.size()
        OpString opstring = opstrings[0]

        def calculator = opstring.services[0]
        assertEquals "Calculator", calculator.name
        assertEquals "calculator.Calculator", calculator.exportBundles[0].className
        checkCalculatorClassBundles calculator.exportBundles, calculator.componentBundle
        assertEquals "calculator.service.CalculatorImpl", calculator.componentBundle.className
        assertEquals 1, calculator.associationDescriptors.length

        assertEquals "Add", calculator.associationDescriptors[0].name
        assertEquals AssociationType.REQUIRES, calculator.associationDescriptors[0].associationType
        assertEquals "add", calculator.associationDescriptors[0].propertyName
    }

    void testAssociationInjectProperty() {
        File file = new File("src/test/resources/opstrings/association_lazy.groovy")
        def opstrings = dslParser.parse(file, null, null, null)
        assertEquals "There should be one and only one opstring", 1, opstrings.size()
        OpString opstring = opstrings[0]


        def calculator = opstring.services[0]
        assertEquals "Calculator", calculator.name
        assertEquals "calculator.Calculator", calculator.exportBundles[0].className
        checkCalculatorClassBundles calculator.exportBundles, calculator.componentBundle
        assertEquals "calculator.service.CalculatorImpl", calculator.componentBundle.className
        assertEquals 1, calculator.associationDescriptors.length

        assertEquals "Add", calculator.associationDescriptors[0].name
        assertEquals AssociationType.REQUIRES, calculator.associationDescriptors[0].associationType
        assertEquals "add", calculator.associationDescriptors[0].propertyName
        assertEquals false, calculator.associationDescriptors[0].lazyInject
    }

    void testExternal() {
        File file = new File("src/test/resources/opstrings/external.groovy")
        def opstrings = dslParser.parse(file,     // opstring
                                        null,     // parent classloader
                                        null,     // defaultGroups
                                        null)     // loadPath
        assertEquals "There should be one and only one opstring", 1, opstrings.size()
        OpString opstring = (OpString)opstrings[0]
        ServiceElement[] elems = opstring.getServices()
        assertEquals "There should be 1 service", 1, elems.length
        assertEquals("Expected ${ServiceElement.ProvisionType.EXTERNAL}, got ${elems[0].provisionType}",
                     ServiceElement.ProvisionType.EXTERNAL, elems[0].provisionType)
    }

    void testLocators() {
        File file = new File("src/test/resources/opstrings/locators.groovy")
        def opstrings = dslParser.parse(file,     // opstring
                                        null,     // parent classloader
                                        null,     // defaultGroups
                                        null)     // loadPath
        assertEquals "There should be one and only one opstring", 1, opstrings.size()
        OpString opstring = (OpString)opstrings[0]
        ServiceElement[] elems = opstring.getServices()
        assertEquals "There should be 2 services", 2, elems.length
        LookupLocator[] locators = elems[0].getServiceBeanConfig().getLocators()
        assertEquals("There should be 2 locators", 2, locators.length)
        String[] groups = elems[0].getServiceBeanConfig().getGroups()
        assertEquals("There should be 1 group", 1, groups.length)

        locators = elems[1].getServiceBeanConfig().getLocators()
        assertEquals("There should be 1 locator", 1, locators.length)
        groups = elems[1].getServiceBeanConfig().getGroups()
        assertEquals("There should be 0 groups", 0, groups.length)
    }

    void testParameters() {
        File file = new File("src/test/resources/opstrings/parameters.groovy")
        def opstrings = dslParser.parse(file,     // opstring
                                        null,     // parent classloader
                                        null,     // defaultGroups
                                        null)     // loadPath
        assertEquals "There should be one and only one opstring", 1, opstrings.size()
        OpString opstring = (OpString)opstrings[0]
        ServiceElement[] elems = opstring.getServices()
        assertEquals "There should be 1 service", 1, elems.length
        Map<String, Object> parms = elems[0].getServiceBeanConfig().getInitParameters();
        assertEquals("There should be one parameter", 1, parms.size())
        Object o = parms.get("parm")
        assertNotNull "Should have a non-null value for parmater \"parm\"", o
    }

    void testSystemRequirements() {
        File file = new File("src/test/resources/opstrings/systemRequirements_declarations.groovy")
        def opstrings = dslParser.parse(file,     // opstring
                                        null,     // parent classloader
                                        null,     // defaultGroups
                                        null)     // loadPath
        assertEquals "There should be one and only one opstring", 1, opstrings.size()
        OpString opstring = (OpString)opstrings[0]
        ServiceElement[] elems = opstring.getServices()
        assertEquals "There should be 4 services", 4, elems.length

        for(int i=0; i<2; i++)
            checkSysRequirement(i, elems[i])

        ServiceLevelAgreements slas = elems[2].getServiceLevelAgreements()
        SystemRequirements sysReqs = slas.systemRequirements

        Map<String, ThresholdValues> map = sysReqs.getSystemThresholds()
        assertEquals "There should be 2 SystemThresholds", 2, map.size()
        ThresholdValues tVals = map.get("CPU")
        assertNotNull tVals
        assertTrue "CPU threshold high value should be .95", tVals.getHighThreshold()==0.95
        assertTrue "CPU threshold low value should be NaN, was ${tVals.getLowThreshold()}", Double.isNaN(tVals.getLowThreshold())
        tVals = map.get("Memory")
        assertNotNull tVals
        assertTrue "Memory threshold high value should be .99", tVals.getHighThreshold()==0.99
        assertTrue "Memory threshold low value should be 0.1", tVals.getLowThreshold()==0.1

        SystemComponent[] sysComps = sysReqs.getSystemComponents()
        assertEquals "There should be 7 SystemComponents", 7, sysComps.length
        boolean checkedMemory = false
        boolean checkedStorageCapability = false
        boolean checkedOperatingSystem = false
        boolean checkedProcessorArchitecture = false
        boolean checkedNativeLib = false
        for(SystemComponent sc : sysComps) {
            assertNotNull sc.attributes
            if(sc.name.equals("SystemMemory")) {
                Map attrs = new HashMap()
                attrs.put("Name", SystemWatchID.SYSTEM_MEMORY)
                attrs.put("Available", "4g")
                attrs.put("Capacity", "20g")
                checkSystemComponent sc, attrs
                checkedMemory = true
            }
            if(sc.name.equals("StorageCapability")) {
                Map attrs = new HashMap()
                attrs.put("Name", SystemWatchID.DISK_SPACE)
                attrs.put("Available", "100g")
                attrs.put("Capacity", "20t")
                checkSystemComponent sc, attrs
                checkedStorageCapability = true
            }
            if(sc.name.equals("OperatingSystem")) {
                Map attrs = new HashMap()
                attrs.put("Name", "Mac OSX")
                attrs.put("Version", "10.7*")
                checkSystemComponent sc, attrs
                checkedOperatingSystem = true
            }
            if(sc.name.equals("Processor")) {
                Map attrs = new HashMap()
                attrs.put("Available", 8)
                checkSystemComponent sc, attrs
                checkedProcessorArchitecture = true
            }
            if(sc.name.equals("NativeLibrarySupport")) {
                Map attrs = new HashMap()
                attrs.put("Name", "libbrlcad.19")
                checkSystemComponent sc, attrs
                checkedNativeLib = true
            }
        }
        assertTrue checkedMemory
        assertTrue checkedStorageCapability
        assertTrue checkedOperatingSystem
        assertTrue checkedProcessorArchitecture
        assertTrue checkedNativeLib
        
        /* Make sure we have no system requirements */
        slas = elems[3].getServiceLevelAgreements()
        assertTrue "Service \"${elems[3].getName()}\" should have no SystemComponent requirements",
                   slas.getSystemRequirements().getSystemComponents().length==0

    }

    private void checkSystemComponent(SystemComponent sysComp, Map expected) {
        Map attributes = sysComp.attributes
        assertNotNull attributes
        assertTrue attributes.equals(expected)
    }

    private void checkSysRequirement(int i, ServiceElement elem) {
        int n = i
        n++
        String s = "$n"

        assertTrue "Service should have a name of $s. not ${elem.getName()}", s.equals(elem.getName())
        ServiceLevelAgreements slas = elem.getServiceLevelAgreements();
        SystemRequirements sysReqs = slas.systemRequirements
        SystemComponent[] sysComps = sysReqs.getSystemComponents()
        assertEquals "There should be 1 SystemComponent", 1, sysComps.length
        assertTrue "SystemComponent name should be \"SoftwareSupport\", not \"${sysComps[0].getClassName()}\"",
                   sysComps[0].getClassName().equals("SoftwareSupport")
        Map attributes = sysComps[0].attributes
        String name = attributes.get("Name")
        assertTrue "Service \"$s\" \"SoftwareSupport\" Name should be \"name$s\" not \"$name\"", name.equals("name"+s)
        String version = attributes.get("Version")
        assertTrue "Service \"$s\" \"SoftwareSupport\" Version should be \"$s.0\" not \"$version\"", version.equals(s+".0")
        String comment = attributes.get("Comment")
        String test = i==0?"Wily E Coyote":"Road Runner"
        assertTrue "Service \"$s\" \"SoftwareSupport\" Comment should be \"$test\" not \"$comment\"", comment.equals(test)
    }

    void testEmptyResources() {
        File file = new File("src/test/resources/opstrings/bean_empty.groovy")
        def opstrings = dslParser.parse(file,     // opstring
                                        null,     // parent classloader
                                        null,     // defaultGroups
                                        null)     // loadPath
        assertEquals "There should be one and only one opstring", 1, opstrings.size()
        OpString opstring = (OpString)opstrings[0]
        ServiceElement[] elems = opstring.getServices()
        assertEquals "There should be one and only one service", 1, elems.length
        assertEquals "There should be no implementation resources",
                     0, elems[0].componentBundle.getJARNames().length
        assertEquals "There should be one and only one export bundle",
                     1, elems[0].getExportBundles().length        
        assertEquals "There should be no codebase resources",
                     0, elems[0].getExportBundles()[0].getJARNames().length
    }

    void testSpringDM() {
        File file = new File("src/test/resources/opstrings/springDM.groovy")
        def opstrings = dslParser.parse(file,     // opstring
                                        null,     // parent classloader
                                        null,     // defaultGroups
                                        null)     // loadPath
        assertEquals "There should be one and only one opstring", 1, opstrings.size()
        OpString opstring = (OpString)opstrings[0]
        ServiceElement[] elems = opstring.getServices()
        assertEquals "There should be one and only one service", 1, elems.length
        String classname = elems[0].componentBundle.className
        assertEquals "Service name should be"+ServiceExecutor.class.name,
                     ServiceExecutor.class.name, classname
        String commandLine = elems[0].execDescriptor.commandLine
        assertEquals "Commandline should be bin/startup.sh",
                     "bin/startup.sh", commandLine
        ServiceLevelAgreements slas = elems[0].getServiceLevelAgreements();
        SystemRequirements sysReqs = slas.systemRequirements
        SystemComponent[] sysComp = sysReqs.getSystemComponents()
        assertEquals "There should be one SystemComponent", 1, sysComp.length
        assertEquals "SystemComponent name should be [SoftwareSupport]",
                     "SoftwareSupport", sysComp[0].name
        def attrs = sysComp[0].attributes
        assertEquals "Value for Name should be [Spring DM]", "Spring DM", attrs.get("Name")
        assertEquals "Value for Version should be [1.0.0]", "1.0.0", attrs.get("Version")
        def software = sysComp[0].stagedSoftware
        assertNotNull "StagedSoftware element should not be null", software
        StagedSoftware sw = (StagedSoftware)software
        assertTrue "StagedSoftware should be removed on destroy", sw.removeOnDestroy()
        assertTrue "StagedSoftware should be unarchived", sw.unarchive()
        SLA[] serviceSLA = slas.getServiceSLAs();
        assertEquals "There should be one SLA ", 1, serviceSLA.length
        assertEquals "SLA policyHandler incorrect",
                     SLAPolicyHandler.class.name, serviceSLA[0].getSlaPolicyHandler()
        serviceSLA[0].slaPolicyHandler
        assertEquals "SLA id should be [thread-count] ", "thread-count", serviceSLA[0].identifier
        WatchDescriptor[] wDescs = serviceSLA[0].watchDescriptors
        assertEquals "There should be one WatchDescriptor ", 1, wDescs.length
        assertEquals "WatchDescriptor objectName should be ["+ManagementFactory.THREAD_MXBEAN_NAME+"] ",
                     ManagementFactory.THREAD_MXBEAN_NAME, wDescs[0].objectName
        assertEquals "WatchDescriptor attribute should be [ThreadCount] ",
                     "ThreadCount", wDescs[0].attribute

        URL url = opstring.loadedFrom()
        File f = new File(url.toURI())
        assertEquals f.name, file.name
        assertEquals f.canonicalPath, file.canonicalPath
    }

    void testGroovyIncludes() {
        File file = new File("src/test/resources/opstrings/outer.groovy")
        def opstrings = dslParser.parse(file,     // opstring
                                        null,     // parent classloader
                                        null,     // defaultGroups
                                        null)     // loadPath
        assertEquals "There should be one and only one opstring", 1, opstrings.size()
        OpString opstring = (OpString)opstrings[0]
        assertEquals "There should be one nested opstring", 1, opstring.nestedOperationalStrings.length
        OpString level1 = (OpString)opstring.nestedOperationalStrings[0]
        assertEquals "There should be one inner nested opstring", 1, level1.nestedOperationalStrings.length
        /*
        OpString level2 = (OpString)level1.nestedOperationalStrings[0]
        assertEquals "Level 1 nested opstring name should be ["+level1.name+"]");
        println("Level 2 nested opstring ["+level2.name+"]");
        */

    }

    void testDownloads() {
        File  file = new File("src/test/resources/opstrings/download.groovy")
        def opstrings = dslParser.parse(file,   // opstring
                                        null,     // parent classloader
                                        null,     // defaultGroups
                                        null)     // loadPath
        assertEquals "There should be one and only one opstring", 1, opstrings.size()
        OpString opstring = opstrings[0]
        def services = opstring.getServices()
        assertEquals "There should be one and only one service", 1, services.size()
        ServiceElement service = services[0]
        SystemComponent[] sysComps =
            service.getServiceLevelAgreements().getSystemRequirements().getSystemComponents();
        assertEquals "There should be 6 SystemComponents", 6, sysComps.length
        for(SystemComponent sysComp : sysComps) {
            StagedSoftware[] swArray = sysComp.getStagedSoftware()
            assertEquals "There should be 1 StagedSoftware", 1, swArray.length
            StagedSoftware sw = swArray[0]
            String name = (String)sysComp.attributes.get("Name")
            assertNotNull name
            if(name.startsWith("a"))
                assertTrue sw.getUseAsClasspathResource()
            else
                assertFalse sw.getUseAsClasspathResource()
        }
    }

    void testMultiGroovyIncludes() {
        File file = new File("src/test/resources/opstrings/outer_multi.groovy")
        def opstrings = dslParser.parse(file,     // opstring
                                        null,     // parent classloader
                                        null,     // defaultGroups
                                        null)     // loadPath
        assertEquals "There should be one and only one opstring", 1, opstrings.size()
        OpString opstring = (OpString)opstrings[0]
        assertEquals "There should be two nested opstrings", 2, opstring.nestedOperationalStrings.length
        OpString level1_A = (OpString)opstring.nestedOperationalStrings[0]
        OpString level1_B = (OpString)opstring.nestedOperationalStrings[1]
        assertEquals "There should be one inner nested opstring for ["+level1_A.name+"]", 1, level1_A.nestedOperationalStrings.length
        assertEquals "There should be one inner nested opstring for ["+level1_B.name+"]", 1, level1_B.nestedOperationalStrings.length
        /*
        OpString level2A = (OpString)level1_A.nestedOperationalStrings[0]
        OpString level2B = (OpString)level1_B.nestedOperationalStrings[0]
        println("Level 1-A nested opstring ["+level1_A.name+"]");
        println("Level 1-B nested opstring ["+level1_B.name+"]");
        println("Level 1-A nested opstring ["+level2A.name+"]");
        println("Level 1-B nested opstring ["+level2B.name+"]");
        */
    }

    void testParserOnSimpleBeanExample() {
        File file = new File("src/test/resources/opstrings/bean.groovy")
        def opstrings = dslParser.parse(file, null, null, null)
        assertEquals "There should be one and only one opstring", 1, opstrings.size()
        OpString opstring = opstrings[0]
        assertEquals "The OpString name is not valid", 'Hello World Example', opstring.name
        assertEquals "The number of services does not match", 1, opstring.services.size()
        def service = opstring.services[0]
        assertEquals "The name of the service is invalid", "Hello", service.name
        def exportBundle = service.exportBundles[0]
        assertEquals "bean.Hello", exportBundle.className
        assertEquals "bean/lib/bean-dl.jar", exportBundle.JARNames[0]
        def componentBundle = service.componentBundle
        assertEquals "bean.service.HelloImpl", componentBundle.className
        assertEquals "bean/lib/bean.jar", componentBundle.JARNames[0]
        assertEquals 1, service.planned
    }

    void testParserOnCalculatorExample() {
        doTestParserOnCalculatorExampleOnFile dslParser, new File("src/test/resources/opstrings/calculator.groovy")
        doTestParserOnCalculatorExampleOnFile dslParser, new File("src/test/resources/opstrings/calculator2.groovy")
        doTestParserOnCalculatorExampleOnFile dslParser, new File("src/test/resources/opstrings/calculator_artifacts.groovy")
    }

    def doTestParserOnCalculatorExampleOnFile(parser, file) {
        def opstrings = parser.parse(file, null, null, null)
        assertEquals "There should be one and only one opstring", 1, opstrings.size()
        OpString opstring = opstrings[0]
        assertEquals "The OpString name is not valid", 'Calculator', opstring.name
        assertEquals "The number of services does not match", 5, opstring.services.size()

        def calculator = opstring.services[0]
        assertEquals "Calculator", calculator.name
        assertEquals "calculator.Calculator", calculator.exportBundles[0].className
        checkCalculatorClassBundles calculator.exportBundles, calculator.componentBundle
        assertEquals "calculator.service.CalculatorImpl", calculator.componentBundle.className
        assertEquals 4, calculator.associationDescriptors.length

        assertEquals "Add", calculator.associationDescriptors[0].name
        assertEquals AssociationType.REQUIRES, calculator.associationDescriptors[0].associationType
        assertEquals "add", calculator.associationDescriptors[0].propertyName
        assertEquals "Subtract", calculator.associationDescriptors[1].name
        assertEquals AssociationType.REQUIRES, calculator.associationDescriptors[1].associationType
        assertEquals "subtract", calculator.associationDescriptors[1].propertyName
        assertEquals "Multiply", calculator.associationDescriptors[2].name
        assertEquals AssociationType.REQUIRES, calculator.associationDescriptors[2].associationType
        assertEquals "multiply", calculator.associationDescriptors[2].propertyName
        assertEquals "Divide", calculator.associationDescriptors[3].name
        assertEquals AssociationType.REQUIRES, calculator.associationDescriptors[3].associationType
        assertEquals "divide", calculator.associationDescriptors[3].propertyName
        assertEquals 1, calculator.planned

        def add = opstring.services[1]
        assertEquals "Add", add.name
        assertEquals "calculator.Add", add.exportBundles[0].className
        checkCalculatorClassBundles add.exportBundles, add.componentBundle
        assertEquals "calculator.service.AddImpl", add.componentBundle.className
        assertEquals 1, add.planned

        def substract = opstring.services[2]
        assertEquals "Subtract", substract.name
        assertEquals "calculator.Subtract", substract.exportBundles[0].className
        checkCalculatorClassBundles substract.exportBundles, substract.componentBundle
        assertEquals "calculator.service.SubtractImpl", substract.componentBundle.className
        assertEquals 1, substract.planned

        def multiply = opstring.services[3]
        assertEquals "Multiply", multiply.name
        assertEquals "calculator.Multiply", multiply.exportBundles[0].className
        checkCalculatorClassBundles multiply.exportBundles, multiply.componentBundle
        assertEquals "calculator.service.MultiplyImpl", multiply.componentBundle.className
        assertEquals 1, multiply.planned

        def divide = opstring.services[4]
        assertEquals "Divide", divide.name
        assertEquals "calculator.Divide", divide.exportBundles[0].className
        checkCalculatorClassBundles divide.exportBundles, divide.componentBundle
        assertEquals "calculator.service.DivideImpl", divide.componentBundle.className
        
        assertEquals 1, divide.planned
    }

    private void checkCalculatorClassBundles(exportBundles, componentBundle) {
        if(exportBundles[0].artifact==null) {
            assertEquals 1, exportBundles[0].JARNames.size()
            assertEquals "calculator/lib/calculator-dl.jar", exportBundles[0].JARNames[0]
        } else {
            assertEquals "org.rioproject.examples:calculator:dl:1.0", exportBundles[0].artifact
        }
        if(componentBundle.artifact==null) {
            assertEquals 1, componentBundle.JARNames.size()
            assertEquals "calculator/lib/calculator.jar", componentBundle.JARNames[0]
        } else {
            assertEquals "org.rioproject.examples:calculator:1.0", componentBundle.artifact
        }
    }

    void testParserOnEventsExample() {
        File file = new File("src/test/resources/opstrings/events.groovy")
        def opstrings = dslParser.parse(file, null, null, null)
        assertEquals "There should be one and only one opstring", 1, opstrings.size()
        OpString opstring = opstrings[0]
        assertEquals "The OpString name is not valid", 'Events Example', opstring.name
        assertEquals "The number of services does not match", 2, opstring.services.size()

        def producer = opstring.services[0]
        assertEquals "Hello World", producer.name
        assertEquals "events.Hello", producer.exportBundles[0].className
        assertEquals 1, producer.exportBundles[0].JARNames.size()
        assertEquals "events/lib/hello-event-dl.jar", producer.exportBundles[0].JARNames[0]
        assertEquals "events.service.HelloImpl", producer.componentBundle.className
        assertEquals 1, producer.componentBundle.JARNames.size()
        assertEquals "events/lib/hello-event.jar", producer.componentBundle.JARNames[0]

        // The following assert has been removed because I dont see why we need to verify the
        // number of configuration entries at this time
        /*StringBuilder sb = new StringBuilder()
        for (Map.Entry entry : producer.serviceBeanConfig.configurationParameters.entrySet()) {
            sb.append(entry.key).append(": ").append(entry.value).append("\n")
        }
        println "\n${sb.toString()}"
        assertEquals 10, producer.serviceBeanConfig.configurationParameters.size()*/
        assertEquals 1, producer.planned

        def consumer = opstring.services[1]
        assertEquals "Hello Event Consumer", consumer.name
        assertEquals "events.service.HelloEventConsumer", consumer.componentBundle.className
        assertEquals 1, consumer.componentBundle.JARNames.size()
        assertEquals "events/lib/hello-event.jar", consumer.componentBundle.JARNames[0]
        assertEquals 1, consumer.planned
    }

    void testParserOnServiceBeanExample() {
        testParserOnServiceBeanExampleFromFile dslParser, new File("src/test/resources/opstrings/servicebean.groovy")
    }
    void testParserOnServiceBeanExampleFromFile(parser, file) {
        def opstrings = parser.parse(file, null, null, null)
        assertEquals "There should be one and only one opstring", 1, opstrings.size()
        OpString opstring = opstrings[0]
        assertEquals "The OpString name is not valid", 'ServiceBean Example', opstring.name
        assertEquals "The number of services does not match", 1, opstring.services.size()
        def service = opstring.services[0]
        assertEquals "The name of the service is invalid", "Hello", service.name
        def exportBundle = service.exportBundles[0]
        assertEquals "servicebean.Hello", exportBundle.className
        assertEquals "servicebean/lib/servicebean-dl.jar", exportBundle.JARNames[0]
        def componentBundle = service.componentBundle
        assertEquals "servicebean.service.HelloImpl", componentBundle.className
        assertEquals "servicebean/lib/servicebean.jar", componentBundle.JARNames[0]
        assertEquals 1, service.planned
    }

    void testParserOnSpringBeanExample() {
        File file = new File("src/test/resources/opstrings/springbean.groovy")
        def opstrings = dslParser.parse(file, null, null, null)
        assertEquals "There should be one and only one opstring", 1, opstrings.size()
        OpString opstring = opstrings[0]
        assertEquals "The OpString name is not valid", 'Hello World Example', opstring.name
        assertEquals "The number of services does not match", 1, opstring.services.size()
        def service = opstring.services[0]
        assertEquals "The name of the service is invalid", "Hello", service.name
        def exportBundle = service.exportBundles[0]
        assertEquals "springbean.Hello", exportBundle.className
        assertEquals "springbean/lib/springbean-dl.jar", exportBundle.JARNames[0]
        def componentBundle = service.componentBundle
        assertEquals "springbean.service.HelloImpl", componentBundle.className
        assertEquals "springbean/lib/springbean.jar", componentBundle.JARNames[0]
        assertEquals 1, service.planned
    }

    void testParserOnTomcatDeployment() {
        File file = new File("src/test/resources/opstrings/tomcat.groovy")
        def opstrings = dslParser.parse(file, null, null, null)
        assertEquals "There should be one and only one opstring", 1, opstrings.size()
        OpString opstring = opstrings[0]
        assertEquals "The OpString name is not valid", 'Tomcat Deploy', opstring.name
        assertEquals "The number of services does not match", 1, opstring.services.size()
        def service = opstring.services[0]
        assertEquals "The name of the service is invalid", "Tomcat", service.name

        def systemComponents = service.serviceLevelAgreements.systemRequirements.systemComponents
        assertEquals 1, systemComponents.size()
        assertEquals 'Tomcat', systemComponents[0].attributes['Name']
        assertEquals '6.0.16', systemComponents[0].attributes['Version']
        assertEquals ServiceExecutor.class.name, service.componentBundle.className
        def softwareLoads = systemComponents[0].stagedSoftware

        assertNotNull 'Expected a non-null software load', softwareLoads
        def StagedSoftware tomcat = softwareLoads
        assertTrue "Data should be removed on service destroy", tomcat.removeOnDestroy()
        assertEquals 'https://elastic-grid.s3.amazonaws.com/tomcat/apache-tomcat-6.0.16.zip', tomcat.location.toString()
        assertEquals '${RIO_HOME}/system/external/tomcat', tomcat.installRoot
        assertTrue tomcat.unarchive()

        System.setProperty("RIO_HOME", '.')
        def postInstall = tomcat.postInstallAttributes
        
        assertEquals '/bin/chmod', postInstall.execDescriptor.commandLine
        assertEquals '+x ${RIO_HOME}/system/external/tomcat/apache-tomcat-6.0.16/bin/*sh', postInstall.execDescriptor.inputArgs

        def webapp = service.getStagedData()[0]
        assertEquals 'https://elastic-grid.s3.amazonaws.com/tomcat/sample.war', webapp.location.toString()
        assertEquals '${RIO_HOME}/system/external/tomcat/apache-tomcat-6.0.16/webapps', webapp.installRoot
        assertTrue webapp.unarchive()
        assertEquals 'ugo+rwx', webapp.perms

        assertEquals 'bin', service.execDescriptor.workingDirectory
        assertEquals 'catalina.sh', service.execDescriptor.commandLine
        assertEquals 'start', service.execDescriptor.inputArgs
        assertEquals '/tmp/tomcat.pid', service.execDescriptor.pidFile
        
        assertEquals 1, service.planned
    }

    void testParserOnOutriggerLite() {
        File file = new File("src/test/resources/opstrings/outrigger_lite.groovy")
        def opstrings = dslParser.parse(file, null, null, null)
        assertEquals "There should be one and only one opstring", 1, opstrings.size()
        OpString opstring = opstrings[0]
        assertEquals "The OpString name is not valid", 'Space', opstring.name
        assertEquals "The number of services does not match", 2, opstring.services.size()

        def resultsSpace = opstring.services[0]
        assertEquals "Result Space", resultsSpace.name
        assertEquals "net.jini.space.JavaSpace05", resultsSpace.exportBundles[0].className
        assertEquals 3, resultsSpace.exportBundles[0].JARNames.size()
        assertEquals "outrigger-dl.jar", resultsSpace.exportBundles[0].JARNames[0]
        assertEquals "com.sun.jini.outrigger.TransientOutriggerImpl", resultsSpace.componentBundle.className
        assertEquals 1, resultsSpace.componentBundle.JARNames.size()
        assertEquals "outrigger.jar", resultsSpace.componentBundle.JARNames[0]
        assertEquals 1, resultsSpace.planned
        assertEquals 1, resultsSpace.maxPerMachine
        assertEquals 1, resultsSpace.serviceBeanConfig.configArgs.size()

        def tasksSpace = opstring.services[1]
        assertEquals "Task Space", tasksSpace.name
        assertEquals "net.jini.space.JavaSpace05", tasksSpace.exportBundles[0].className
        assertEquals 3, tasksSpace.exportBundles[0].JARNames.size()
        assertEquals "outrigger-dl.jar", tasksSpace.exportBundles[0].JARNames[0]
        assertEquals "com.sun.jini.outrigger.TransientOutriggerImpl", tasksSpace.componentBundle.className
        assertEquals 1, tasksSpace.componentBundle.JARNames.size()
        assertEquals "outrigger.jar", tasksSpace.componentBundle.JARNames[0]
        assertEquals 1, tasksSpace.planned
        assertEquals 1, tasksSpace.maxPerMachine
        assertEquals 1, tasksSpace.serviceBeanConfig.configArgs.size()
        assertEquals 1, tasksSpace.associationDescriptors.size()
        assertEquals 'Result Space', tasksSpace.associationDescriptors[0].name
        assertEquals AssociationType.OPPOSED, tasksSpace.associationDescriptors[0].associationType
    }

    void testParserOnOutrigger() {
        File file = new File("src/test/resources/opstrings/outrigger.groovy")
        def opstrings = dslParser.parse(file, null, null, null)
        assertEquals "There should be one and only one opstring", 1, opstrings.size()
        OpString opstring = opstrings[0]
        assertEquals "The OpString name is not valid", 'Outrigger', opstring.name
        assertEquals "The number of services does not match", 2, opstring.services.size()

        def mahalo = opstring.services[0]
        assertEquals "Mahalo", mahalo.name
        assertEquals "net.jini.core.transaction.server.TransactionManager", mahalo.exportBundles[0].className
        assertEquals 2, mahalo.exportBundles[0].JARNames.size()
        assertEquals "mahalo-dl.jar", mahalo.exportBundles[0].JARNames[0]
        assertEquals "com.sun.jini.mahalo.TransientMahaloImpl", mahalo.componentBundle.className
        assertEquals 1, mahalo.componentBundle.JARNames.size()
        assertEquals "mahalo.jar", mahalo.componentBundle.JARNames[0]
        assertEquals 1, mahalo.planned
        assertEquals 1, mahalo.maxPerMachine

        def outrigger = opstring.services[1]
        assertEquals "Outrigger", outrigger.name
        assertEquals "net.jini.space.JavaSpace", outrigger.exportBundles[0].className
        assertEquals 2, outrigger.exportBundles[0].JARNames.size()
        assertEquals "outrigger-dl.jar", outrigger.exportBundles[0].JARNames[0]
        assertEquals "com.sun.jini.outrigger.TransientOutriggerImpl", outrigger.componentBundle.className
        assertEquals 1, outrigger.componentBundle.JARNames.size()
        assertEquals "outrigger.jar", outrigger.componentBundle.JARNames[0]
        assertEquals 1, outrigger.planned
        assertEquals 1, outrigger.maxPerMachine
    }

    void testParserOnRioExampleForGettingStartedGuide() {
        File file = new File("src/test/resources/opstrings/rioexample.groovy")
        System.setProperty('org.rioproject.codeserver', 'http://somefakedthing')

        def opstrings = dslParser.parse(file, null, null, null)
        assertEquals "There should be one and only one opstring", 1, opstrings.size()
        OpString opstring = opstrings[0]
        assertEquals "The OpString name is not valid", 'Echo', opstring.name
        assertEquals "The number of services does not match", 1, opstring.services.size()

        def service = opstring.services[0]
        assertEquals "Echo", service.name
        assertEquals "tutorial.Echo", service.exportBundles[0].className
        assertEquals 1, service.exportBundles[0].JARNames.size()
        assertEquals "rio-example/lib/rio-example-dl.jar", service.exportBundles[0].JARNames[0]
        assertEquals "tutorial.EchoJSB", service.componentBundle.className
        assertEquals 1, service.componentBundle.JARNames.size()
        assertEquals "rio-example/lib/rio-example.jar", service.componentBundle.JARNames[0]

        assertEquals 3, service.serviceLevelAgreements.serviceSLAs.size()

        def rateSLA = service.serviceLevelAgreements.serviceSLAs[0]
        assertEquals 'rate', rateSLA.identifier
        assertEquals 0, rateSLA.lowThreshold
        assertEquals 5, rateSLA.highThreshold
        assertEquals ScalingPolicyHandler.class.name, rateSLA.slaPolicyHandler
        assertEquals 5, rateSLA.maxServices
        assertEquals 10000, rateSLA.lowerThresholdDampeningTime
        assertEquals 200, rateSLA.upperThresholdDampeningTime

        def throughtputSLA = service.serviceLevelAgreements.serviceSLAs[1]
        assertEquals 'throughtput', throughtputSLA.identifier
        assertTrue Double.isNaN(throughtputSLA.lowThreshold)
        assertEquals 2, throughtputSLA.highThreshold
        assertEquals SLAPolicyHandler.class.name, throughtputSLA.slaPolicyHandler

        def backlogSLA = service.serviceLevelAgreements.serviceSLAs[2]
        assertEquals 'backlog', backlogSLA.identifier
        assertEquals 100, backlogSLA.lowThreshold
        assertEquals 500, backlogSLA.highThreshold
        assertEquals ScalingPolicyHandler.class.name, backlogSLA.slaPolicyHandler
        assertEquals 10, backlogSLA.maxServices
        assertEquals 3000, backlogSLA.lowerThresholdDampeningTime
        assertEquals 3000, backlogSLA.upperThresholdDampeningTime
        assertEquals "collector", backlogSLA.watchDescriptors[0].name
        assertEquals "count", backlogSLA.watchDescriptors[0].property
        assertEquals 5000, backlogSLA.watchDescriptors[0].period

        assertEquals 1, service.planned
        assertEquals 3, service.maxPerMachine

        assertEquals 1, service.serviceLevelAgreements.systemRequirements.getSystemComponents().size()
        def SystemComponent systemComponent = service.serviceLevelAgreements.systemRequirements.getSystemComponents()[0]
        assertEquals 'SoftwareSupport', systemComponent.className
        assertEquals 2, systemComponent.attributes.size()
        assertEquals 'Spring', systemComponent.attributes.Name
        assertEquals '2.5', systemComponent.attributes.Version
    }

    void testParserOnGridSampleForGettingStartedGuide() {
        File file = new File("src/test/resources/opstrings/grid.groovy")
        def opstrings = dslParser.parse(file, null, null, null)
        assertEquals "There should be one and only one opstring", 1, opstrings.size()
        OpString opstring = opstrings[0]
        assertEquals "The OpString name is not valid", 'Grid', opstring.name
        assertEquals "The number of services does not match", 1, opstring.services.size()

        def worker = opstring.services[0]
        assertEquals "Worker", worker.name
        assertEquals "tutorial.grid.Task", worker.exportBundles[0].className
        assertEquals 1, worker.exportBundles[0].JARNames.size()
        assertEquals "compute-grid/lib/grid-dl.jar", worker.exportBundles[0].JARNames[0]
        assertEquals "tutorial.grid.TaskServer", worker.componentBundle.className
        assertEquals 1, worker.componentBundle.JARNames.size()
        assertEquals "compute-grid/lib/grid.jar", worker.componentBundle.JARNames[0]
        
        assertEquals 5, worker.planned
        assertEquals 10, worker.maxPerMachine

        assertEquals 1, opstring.nestedOperationalStrings.size()
    }

    void testParserOnTerracotaServerDeployment() {
        File file = new File("src/test/resources/opstrings/terracottaServer.groovy")
        def opstrings = dslParser.parse(file, null, null, null)
        assertEquals "There should be one and only one opstring", 1, opstrings.size()
        OpString opstring = opstrings[0]
        assertEquals "The OpString name is not valid", 'Terracotta-Server Group', opstring.name
        assertEquals "The number of services does not match", 1, opstring.services.size()
        def service = opstring.services[0]
        assertEquals "The name of the service is invalid", 'Terracotta-Server', service.name

        def systemComponents = service.serviceLevelAgreements.systemRequirements.systemComponents
        assertEquals 1, systemComponents.size()
        assertEquals 'Terracotta', systemComponents[0].attributes['Name']
        assertEquals 'latest', systemComponents[0].attributes['Version']

        assertEquals 'bin/start-tc-server.sh', service.execDescriptor.commandLine
        assertEquals '-f /Users/tgautier/rio-test/tc-config.xml -n server2', service.execDescriptor.inputArgs

        assertEquals '10.0.4.222', service.cluster[0]
        assertEquals '10.0.4.224', service.cluster[1]

        assertEquals 2, service.planned
        assertEquals MachineBoundary.PHYSICAL, service.machineBoundary
        assertEquals 1, service.maxPerMachine
    }

    void testParserOnTerracotaClientDeployment() {
        File file = new File("src/test/resources/opstrings/terracottaClient.groovy")
        def opstrings = dslParser.parse(file, null, null, null)
        assertEquals "There should be one and only one opstring", 1, opstrings.size()
        OpString opstring = opstrings[0]
        assertEquals "The OpString name is not valid", 'Terracotta-SharedEditor', opstring.name
        assertEquals "The number of services does not match", 1, opstring.services.size()
        def service = opstring.services[0]
        assertEquals "The name of the service is invalid", 'SharedEditor', service.name

        def systemComponents = service.serviceLevelAgreements.systemRequirements.systemComponents
        assertEquals 1, systemComponents.size()
        assertEquals 'Terracotta', systemComponents[0].attributes['Name']
        assertEquals '2.4.8', systemComponents[0].attributes['Version']

        assertEquals 'samples/pojo/sharededitor', service.execDescriptor.workingDirectory
        assertEquals 'run.sh', service.execDescriptor.commandLine

        assertEquals 1, service.planned
        assertEquals MachineBoundary.PHYSICAL, service.machineBoundary
        assertEquals 1, service.maxPerMachine
    }

    void testParserOnMuves() {
        def opstrings = dslParser.parse(new File("src/test/resources/opstrings/muves.rio.groovy"),
                                        null, null, null)
        assertEquals "There should be one and only one opstring", 1, opstrings.size()
        OpString opstring = opstrings[0]
        assertEquals "The OpString name is not valid", 'Muves', opstring.name
        assertEquals "The number of services does not match", 5, opstring.services.size()

        def worker = opstring.services[0]
        assertEquals 'Worker', worker.name
        assertEquals 'net.gomez.worker.Worker', worker.exportBundles[0].className
        assertEquals 'net.gomez.worker.WorkerImpl', worker.componentBundle.className
        assertEquals 1, worker.serviceBeanConfig.initParameters.size()
        assertEquals 'false', worker.serviceBeanConfig.initParameters.doSpin
        assertEquals 3, worker.planned
        assertEquals 1, worker.maxPerMachine
        assertEquals MachineBoundary.PHYSICAL, worker.machineBoundary
        assertEquals 2, worker.associationDescriptors.size()
        assertEquals 'Task Space', worker.associationDescriptors[0].name
        assertEquals AssociationType.OPPOSED, worker.associationDescriptors[0].associationType
        assertTrue worker.associationDescriptors[0].interfaceNames.any { it == 'net.jini.space.JavaSpace05' } 
        assertEquals 'taskSpace', worker.associationDescriptors[0].propertyName
        assertEquals 'net.gomez.provider.space.SpaceProxy', worker.associationDescriptors[0].proxyClass
        assertEquals Utilization.class.name, worker.associationDescriptors[0].serviceSelectionStrategy
        assertEquals 'Result Space', worker.associationDescriptors[1].name
        assertEquals AssociationType.OPPOSED, worker.associationDescriptors[1].associationType
        assertTrue worker.associationDescriptors[1].interfaceNames.any { it == 'net.jini.space.JavaSpace05' }
        assertEquals 'net.gomez.provider.space.SpaceProxy', worker.associationDescriptors[1].proxyClass
        assertEquals Utilization.class.name, worker.associationDescriptors[1].serviceSelectionStrategy

        def jobMonitor = opstring.services[1]
        assertEquals 'Job Monitor', jobMonitor.name
        assertEquals 'net.gomez.jobmonitor.JobMonitor', jobMonitor.exportBundles[0].className
        assertEquals 'net.gomez.jobmonitor.JobMonitorImpl', jobMonitor.componentBundle.className
        assertEquals 1, jobMonitor.planned
        assertEquals 2, jobMonitor.associationDescriptors.size()
        assertEquals 'Task Space', jobMonitor.associationDescriptors[0].name
        assertEquals AssociationType.OPPOSED, jobMonitor.associationDescriptors[0].associationType
        assertTrue jobMonitor.associationDescriptors[0].interfaceNames.any { it == 'net.jini.space.JavaSpace05' }
        assertEquals 'taskSpace', jobMonitor.associationDescriptors[0].propertyName
        assertEquals 'net.gomez.provider.space.SpaceProxy', jobMonitor.associationDescriptors[0].proxyClass
        assertEquals Utilization.class.name, jobMonitor.associationDescriptors[0].serviceSelectionStrategy
        assertEquals 'Result Space', jobMonitor.associationDescriptors[1].name
        assertEquals AssociationType.OPPOSED, jobMonitor.associationDescriptors[1].associationType
        assertTrue jobMonitor.associationDescriptors[1].interfaceNames.any { it == 'net.jini.space.JavaSpace05' }
        assertEquals 'net.gomez.provider.space.SpaceProxy', jobMonitor.associationDescriptors[1].proxyClass
        assertEquals Utilization.class.name, jobMonitor.associationDescriptors[1].serviceSelectionStrategy

        def lurch = opstring.services[2]
        assertEquals 'Lurch', lurch.name
        assertEquals 'net.gomez.lurch.Lurch', lurch.exportBundles[0].className
        assertEquals 'net.gomez.lurch.LurchImpl', lurch.componentBundle.className
        assertEquals 1, lurch.planned
        assertEquals 4, lurch.associationDescriptors.size()
        assertEquals 'Task Space', lurch.associationDescriptors[0].name
        assertEquals AssociationType.OPPOSED, lurch.associationDescriptors[0].associationType
        assertTrue lurch.associationDescriptors[0].interfaceNames.any { it == 'net.jini.space.JavaSpace05' }
        assertEquals 'taskSpace', lurch.associationDescriptors[0].propertyName
        assertEquals 'net.gomez.provider.space.SpaceProxy', lurch.associationDescriptors[0].proxyClass
        assertEquals Utilization.class.name, lurch.associationDescriptors[0].serviceSelectionStrategy
        assertEquals 'Result Space', lurch.associationDescriptors[1].name
        assertEquals AssociationType.OPPOSED, lurch.associationDescriptors[1].associationType
        assertTrue lurch.associationDescriptors[1].interfaceNames.any { it == 'net.jini.space.JavaSpace05' }
        assertEquals 'net.gomez.provider.space.SpaceProxy', lurch.associationDescriptors[1].proxyClass
        assertEquals Utilization.class.name, lurch.associationDescriptors[1].serviceSelectionStrategy
        assertEquals 'Job Monitor', lurch.associationDescriptors[2].name
        assertEquals AssociationType.USES, lurch.associationDescriptors[2].associationType
        assertTrue lurch.associationDescriptors[2].interfaceNames.any { it == 'net.gomez.jobmonitor.JobMonitor' }
        assertEquals 'jobMonitor', lurch.associationDescriptors[2].propertyName
        assertEquals 'Job Monitor', lurch.associationDescriptors[3].name
        assertEquals AssociationType.USES, lurch.associationDescriptors[3].associationType
        assertTrue lurch.associationDescriptors[3].interfaceNames.any { it == 'org.rioproject.watch.Watchable' }
        assertEquals 'watchables', lurch.associationDescriptors[3].propertyName

        assertEquals(1, lurch.serviceLevelAgreements.serviceSLAs.length)
        SLA sla = lurch.serviceLevelAgreements.serviceSLAs[0]
        assertEquals("waitq", sla.identifier)
        assertEquals("net.kahona.dispatcher.SimScalingHandler", sla.slaPolicyHandler)
        assertEquals(2, sla.maxServices)

        def fester = opstring.services[3]
        assertEquals 'Fester', fester.name
        assertEquals 'net.gomez.fester.Fester', fester.exportBundles[0].className
        assertEquals 'net.gomez.fester.FesterImpl', fester.componentBundle.className
        assertEquals 1, fester.planned
        assertEquals 2, fester.associationDescriptors.size()
        assertEquals 'Task Space', fester.associationDescriptors[0].name
        assertEquals AssociationType.OPPOSED, fester.associationDescriptors[0].associationType
        assertTrue fester.associationDescriptors[0].interfaceNames.any { it == 'net.jini.space.JavaSpace05' }
        assertEquals 'taskSpace', fester.associationDescriptors[0].propertyName
        assertEquals 'net.gomez.provider.space.SpaceProxy', fester.associationDescriptors[0].proxyClass
        assertEquals Utilization.class.name, fester.associationDescriptors[0].serviceSelectionStrategy
        assertEquals 'Result Space', fester.associationDescriptors[1].name
        assertEquals AssociationType.OPPOSED, fester.associationDescriptors[1].associationType
        assertTrue fester.associationDescriptors[1].interfaceNames.any { it == 'net.jini.space.JavaSpace05' }
        assertEquals 'net.gomez.provider.space.SpaceProxy', fester.associationDescriptors[1].proxyClass
        assertEquals Utilization.class.name, fester.associationDescriptors[1].serviceSelectionStrategy

        def geometryService = opstring.services[4]
        assertEquals 'GeometryService', geometryService.name
        assertEquals 'mil.army.arl.geometryservice.GeometryService', geometryService.exportBundles[0].className
        assertEquals 'mil.army.arl.brlcadservice.impl.BrlcadServiceImpl', geometryService.componentBundle.className

        /* RIO-174 */
        assertEquals 1, geometryService.exportBundles.length

        assertEquals 3, geometryService.planned
        assertEquals 1, geometryService.maxPerMachine
        assertEquals MachineBoundary.PHYSICAL, geometryService.machineBoundary
        assertEquals 2, geometryService.associationDescriptors.size()
        assertEquals 'Task Space', geometryService.associationDescriptors[0].name
        assertEquals AssociationType.OPPOSED, geometryService.associationDescriptors[0].associationType
        assertTrue geometryService.associationDescriptors[0].interfaceNames.any { it == 'net.jini.space.JavaSpace05' }
        assertEquals 'taskSpace', geometryService.associationDescriptors[0].propertyName
        assertEquals 'net.gomez.provider.space.SpaceProxy', geometryService.associationDescriptors[0].proxyClass
        assertEquals Utilization.class.name, geometryService.associationDescriptors[0].serviceSelectionStrategy
        assertEquals 'Result Space', geometryService.associationDescriptors[1].name
        assertEquals AssociationType.OPPOSED, geometryService.associationDescriptors[1].associationType
        assertTrue geometryService.associationDescriptors[1].interfaceNames.any { it == 'net.jini.space.JavaSpace05' }
        assertEquals 'net.gomez.provider.space.SpaceProxy', geometryService.associationDescriptors[1].proxyClass
        assertEquals Utilization.class.name, geometryService.associationDescriptors[1].serviceSelectionStrategy

        assertEquals 1, opstring.nestedOperationalStrings.size()
    }
}
