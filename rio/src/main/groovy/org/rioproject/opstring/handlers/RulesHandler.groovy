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
package org.rioproject.opstring.handlers

import org.rioproject.sla.RuleMap.RuleDefinition
import org.rioproject.sla.RuleMap
import org.rioproject.sla.RuleMap.ServiceDefinition
import org.rioproject.opstring.ParsedService;

/**
 * Parses the Rules element.
 *
 * @author Dennis Reedy
 */
class RulesHandler implements Handler {

    def parse(Object element, Object options) {
        def ParsedService sDescriptor = options['serviceDescriptor']
        def ruleMaps = []
        element.children().each {
            if (it.name() == "Rule") {
                ruleMaps << parseRule(it)
            }
        }
        sDescriptor.setRuleMaps ruleMaps as Collection<RuleMap> 
    }

    RuleMap parseRule(element) {
        def services = []
        String resource = null
        String ruleClassPath = null
        element.children().each {
            if (it.name() == "RuleClassPath") {
                ruleClassPath = it.text()
            }
            if (it.name() == "Resource") {
                resource = it.text()
            }
            if (it.name() == "ServiceFeed") {
                String name = it.'@name'
                String opstring = it.'@opstring'
                String watches = parseWatches(it)
                if(watches==null)
                    throw new IllegalArgumentException("The serviceFeed requires "+
                                                       "watches to be declared")
                ServiceDefinition serviceDef = new ServiceDefinition(name, opstring)
                serviceDef.addWatches toArray(watches)
                services << serviceDef
            }
        }
        
        if(resource==null)
            throw new IllegalArgumentException("The rule declaration requires "+
                                               "a resource to be declared")
        RuleMap ruleMap = new RuleMap()
        RuleDefinition rule = new RuleDefinition(resource, ruleClassPath)
        ruleMap.addRuleMapping rule, services as List<ServiceDefinition>
        return ruleMap
    }

    String parseWatches(element) {
        String watches = null
        element.children().each {
            if (it.name() == "Watches") {
                watches = it.text()
            }
        }
        return watches
    }

    def toArray(String s) {
        StringTokenizer tok = new StringTokenizer(s, ",")
        String[] array = new String[tok.countTokens()]
        int i=0
        while(tok.hasMoreTokens()) {
            array[i] = tok.nextToken().trim()
            i++
        }
        return(array);
    }
}
