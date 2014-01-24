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
package org.rioproject.impl.opstring

import org.rioproject.opstring.ServiceElement
import org.rioproject.sla.RuleMap
import org.rioproject.sla.RuleMap.RuleDefinition
import org.rioproject.sla.RuleMap.ServiceDefinition
/**
 * Test rule parsing
 */
class RuleParseTest extends GroovyTestCase {
    def OpStringParser dslParser = new GroovyDSLOpStringParser()

    void testRulesAsService() {
        File file = new File("src/test/resources/opstrings/rules.groovy")
        def opstrings = dslParser.parse(file,     // opstring
                                        null,     // parent classloader
                                        null,     // defaultGroups
                                        null)     // loadPath
        assertEquals "There should be one and only one opstring", 1, opstrings.size()
        OpString opstring = (OpString)opstrings[0]
        ServiceElement[] elems = opstring.getServices()
        assertEquals "There should be 1 service", 1, elems.length
        Collection<RuleMap> ruleMaps = elems[0].ruleMaps
        assertEquals "There should be 2 RuleMaps", 2, ruleMaps.size()
        RuleMap ruleMap1 = ruleMaps.get(0)
        List<ServiceDefinition> serviceDef1 = ruleMap1.getServiceDefinitions()
        assertEquals "There should be 2 ServiceDefinitions", serviceDef1.size(), 2
        RuleDefinition ruleDef1 = ruleMap1.getRuleDefinition()
        assertNotNull ruleDef1
        assertEquals "ScalingRuleHandler", ruleDef1.resource
        assertEquals "org.sample:model:1.0", ruleDef1.ruleClassPath
        assertEquals "There should be 1 service watch", serviceDef1.get(0).watches.size(), 1
        assertEquals "There should be 1 service watch", serviceDef1.get(1).watches.size(), 1
        assertEquals "Service watch should be \'load\'", serviceDef1.get(0).watches.get(0), "load"
        assertEquals "Service watch should be \'load\'", serviceDef1.get(1).watches.get(0), "load"

        RuleMap ruleMap2 = ruleMaps.get(1)
        List<ServiceDefinition> serviceDef2 = ruleMap2.getServiceDefinitions()
        assertEquals "There should be 1 ServiceDefinition", serviceDef2.size(), 1
        def watches = serviceDef2.get(0).watches
        assertEquals "There should be 2 service watches", watches.size(), 2
        assertTrue "There should be a \'CPU\' watch", "CPU" in watches
        assertTrue "There should be a \'Memory\' watch", "Memory" in watches        
        RuleDefinition ruleDef2 = ruleMap2.getRuleDefinition()
        assertNotNull ruleDef2
        def resources = toArray(ruleDef2.resource, " ,")
        assertEquals "There should be 2 resources", resources.length, 2
        assertNull ruleDef2.ruleClassPath
    }

    void testRulesAsOuter() {
        File file = new File("src/test/resources/opstrings/rules2.groovy")
        def opstrings = dslParser.parse(file,     // opstring
                                        null,     // parent classloader
                                        null,     // defaultGroups
                                        null)     // loadPath
        assertEquals "There should be one and only one opstring", 1, opstrings.size()
        OpString opstring = (OpString)opstrings[0]
        ServiceElement[] elems = opstring.getServices()
        assertEquals "There should be 1 service", 1, elems.length
        Collection<RuleMap> ruleMaps = elems[0].ruleMaps
        assertEquals "There should be 2 RuleMaps", 2, ruleMaps.size()
        RuleMap ruleMap1 = ruleMaps.get(0)
        List<ServiceDefinition> serviceDef1 = ruleMap1.getServiceDefinitions()
        assertEquals "There should be 2 ServiceDefinitions", serviceDef1.size(), 2
        RuleDefinition ruleDef1 = ruleMap1.getRuleDefinition()
        assertNotNull ruleDef1
        assertEquals "ScalingRuleHandler", ruleDef1.resource
        assertEquals "org.sample:model:1.0", ruleDef1.ruleClassPath
        assertEquals "There should be 1 service watch", serviceDef1.get(0).watches.size(), 1
        assertEquals "There should be 1 service watch", serviceDef1.get(1).watches.size(), 1
        assertEquals "Service watch should be \'load\'", serviceDef1.get(0).watches.get(0), "load"
        assertEquals "Service watch should be \'load\'", serviceDef1.get(1).watches.get(0), "load"

        RuleMap ruleMap2 = ruleMaps.get(1)
        List<ServiceDefinition> serviceDef2 = ruleMap2.getServiceDefinitions()
        assertEquals "There should be 1 ServiceDefinition", serviceDef2.size(), 1
        def watches = serviceDef2.get(0).watches
        assertEquals "There should be 2 service watches", watches.size(), 2
        assertTrue "There should be a \'CPU\' watch", "CPU" in watches
        assertTrue "There should be a \'Memory\' watch", "Memory" in watches
        RuleDefinition ruleDef2 = ruleMap2.getRuleDefinition()
        assertNotNull ruleDef2
        def resources = toArray(ruleDef2.resource, " ,")
        assertEquals "There should be 2 resources", resources.length, 2
        assertNull ruleDef2.ruleClassPath
    }

    void testRulesAsOuterWithService() {
        File file = new File("src/test/resources/opstrings/rules3.groovy")
        def opstrings = dslParser.parse(file,     // opstring
                                        null,     // parent classloader
                                        null,     // defaultGroups
                                        null)     // loadPath
        assertEquals "There should be one and only one opstring", 1, opstrings.size()
        OpString opstring = (OpString)opstrings[0]
        ServiceElement[] elems = opstring.getServices()
        assertEquals "There should be 2 services", 2, elems.length
        Collection<RuleMap> ruleMaps = elems[1].ruleMaps
        assertEquals "There should be 1 RuleMap", 1, ruleMaps.size()
        RuleMap ruleMap1 = ruleMaps.get(0)
        List<ServiceDefinition> serviceDef1 = ruleMap1.getServiceDefinitions()
        assertEquals "There should be 1 ServiceDefinition", serviceDef1.size(), 1
        RuleDefinition ruleDef1 = ruleMap1.getRuleDefinition()
        assertNotNull ruleDef1
        assertEquals "file:src/test/resources/CounterNotification", ruleDef1.resource
        assertEquals "org.rioproject.examples:events:dl:1.0", ruleDef1.ruleClassPath
        assertEquals "There should be 1 service watch", serviceDef1.get(0).watches.size(), 1
        assertEquals "Service watch should be \'notification\'", serviceDef1.get(0).watches.get(0), "notification"
    }

    String[] toArray(String arg, String delim) {
        StringTokenizer tok = new StringTokenizer(arg, delim);
        String[] array = new String[tok.countTokens()];
        int i=0;
        while(tok.hasMoreTokens()) {
            array[i] = tok.nextToken();
            i++;
        }
        return(array);
    }
}
