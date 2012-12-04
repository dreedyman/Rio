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
package org.rioproject.gnostic;

import org.drools.KnowledgeBase;
import org.drools.KnowledgeBaseConfiguration;
import org.drools.KnowledgeBaseFactory;
import org.drools.SystemEventListener;
import org.drools.agent.KnowledgeAgent;
import org.drools.agent.KnowledgeAgentConfiguration;
import org.drools.agent.KnowledgeAgentFactory;
import org.drools.builder.*;
import org.drools.conf.EventProcessingOption;
import org.drools.definition.KnowledgePackage;
import org.drools.definition.rule.Rule;
import org.drools.io.Resource;
import org.drools.io.ResourceChangeScannerConfiguration;
import org.drools.io.ResourceFactory;
import org.drools.runtime.StatefulKnowledgeSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Utility for creating Drools {@link org.drools.agent.KnowledgeAgent}s,
 * {@link org.drools.KnowledgeBase}s, and {@link org.drools.runtime.StatefulKnowledgeSession}s
 */
public class DroolsFactory {
    private static final Logger logger = LoggerFactory.getLogger(DroolsFactory.class.getName());

    /**
     * Create a new {@link org.drools.agent.KnowledgeAgent}
     *
     * @param scannerInterval the interval the KnowledgeAgent should scan resources for change
     * @return A KnowledgeAgent
     */
    public static KnowledgeAgent createKnowledgeAgent(int scannerInterval) {
        KnowledgeBaseConfiguration kBaseConfiguration = KnowledgeBaseFactory.newKnowledgeBaseConfiguration();
        kBaseConfiguration.setOption(EventProcessingOption.STREAM);

        KnowledgeAgentConfiguration kaConf = KnowledgeAgentFactory.newKnowledgeAgentConfiguration();

        /* Do not scan directories, just scan files */
        kaConf.setProperty("drools.agent.scanDirectories", "false");
        kaConf.setProperty("drools.agent.scanResources", "true");

        /* Incremental KnowledgeBase build */
        kaConf.setProperty("drools.agent.newInstance", "false");
        kaConf.setProperty("drools.agent.useKBaseClassLoaderForCompiling", "true");
        KnowledgeBase kBase = KnowledgeBaseFactory.newKnowledgeBase(kBaseConfiguration);
        KnowledgeAgent kAgent = KnowledgeAgentFactory.newKnowledgeAgent("Gnostic", kBase, kaConf);

        ResourceFactory.getResourceChangeNotifierService().start();
        ResourceChangeScannerConfiguration sConf =
        ResourceFactory.getResourceChangeScannerService().
                                                         newResourceChangeScannerConfiguration();
        sConf.setProperty( "drools.resource.scanner.interval", Integer.toString(scannerInterval) );
        ResourceFactory.getResourceChangeScannerService().configure( sConf );
        ResourceFactory.getResourceChangeScannerService().setSystemEventListener(new SystemEventListener() {
            public void info(String message) {
                logger.info(message);
            }
            public void info(String message, Object object) {
                logger.info(message, object.toString());
            }
            public void warning(String message) {
                logger.warn(message);
            }
            public void warning(String message, Object object) {
                logger.warn(message, object.toString());
            }
            public void exception(String message, Throwable e) {
                logger.warn(message, e);
            }
            public void exception(Throwable e) {
                logger.warn("", e);
            }
            public void debug(String message) {
                if(logger.isDebugEnabled())
                    logger.debug(message);
            }
            public void debug(String message, Object object) {
                if(logger.isDebugEnabled())
                    logger.debug(message, object.toString());
            }
        });
        ResourceFactory.getResourceChangeScannerService().start();

        logger.info("KnowledgeAgent initialized");
        return kAgent;
    }

    public static StatefulKnowledgeSession createStatefulSession(KnowledgeBase kBase,
                                                                 Map<Resource, ResourceType> resources,
                                                                 ClassLoader loader) {
        /*Properties props = new Properties();
        props.setProperty("drools.dialect.java.compiler", "JANINO");
        KnowledgeBuilderConfiguration config =
            KnowledgeBuilderFactory.newKnowledgeBuilderConfiguration(props, null);
        KnowledgeBuilder builder = KnowledgeBuilderFactory.newKnowledgeBuilder(config);*/
        //KnowledgeBuilder builder = KnowledgeBuilderFactory.newKnowledgeBuilder(config);

        Properties props = new Properties();
        props.setProperty("drools.dialect.java.compiler.lnglevel","1.6" );
        KnowledgeBuilderConfiguration config = KnowledgeBuilderFactory.newKnowledgeBuilderConfiguration(props);
        KnowledgeBuilder builder = KnowledgeBuilderFactory.newKnowledgeBuilder(config);

        /*KnowledgeBuilderConfiguration kbConfig = null;
        if(loader!=null)
            kbConfig = KnowledgeBuilderFactory.newKnowledgeBuilderConfiguration(null, loader);

        KnowledgeBuilder builder;
        if(kbConfig!=null)
            builder = KnowledgeBuilderFactory.newKnowledgeBuilder(kbConfig);
        else
            builder = KnowledgeBuilderFactory.newKnowledgeBuilder();*/

        //KnowledgeBuilder builder = KnowledgeBuilderFactory.newKnowledgeBuilder();
        for(Map.Entry<Resource, ResourceType> entry : resources.entrySet()) {
            Resource resource = entry.getKey();
            ResourceType resourceType = entry.getValue();
            builder.add(resource, resourceType);

            if (builder.hasErrors()) {
                logger.error(String.format("Could not create the KnowledgeBuilder using %s properly", resource));
                for (KnowledgeBuilderError error : builder.getErrors()) {
                    logger.error(String.format("At lines %s, got error %s",
                                                Arrays.toString(error.getLines()),
                                                error.getMessage()));
                }
                return null;
            }
        }


        Collection<KnowledgePackage> toAdd = new ArrayList<KnowledgePackage>();

        toAdd.addAll(builder.getKnowledgePackages());

        //TODO: Add all for now because when using KnowledgeAgent things seem a little different
        /*if(kBase.getKnowledgePackages().size()==0) {
            toAdd.addAll(builder.getKnowledgePackages());
        } else {
            for(KnowledgePackage known : kBase.getKnowledgePackages()) {
                KnowledgePackage candidate = getKnowledgePackage(known.getName(),
                                                                 builder.getKnowledgePackages());
                boolean matched = false;
                for(Rule rule : known.getRules()) {
                    String ruleName = rule.getPackageName()+"."+rule.getName();                    
                    for(Rule r : candidate.getRules()) {
                        String rName = r.getPackageName()+"."+r.getName();
                        if(ruleName.equals(rName)) {
                            matched = true;
                            break;
                        }
                    }
                }
                if(!matched) {
                    toAdd.add(candidate);
                    break;
                }
            }
        }*/
        logger.info(String.format("ADDING: %s", getToAddNames(toAdd)));
        if(!toAdd.isEmpty())
            kBase.addKnowledgePackages(toAdd);
        return kBase.newStatefulKnowledgeSession();
    }

    private static KnowledgePackage getKnowledgePackage(String name,
                                                        Collection<KnowledgePackage> packages) {
        KnowledgePackage kp = null;
        for(KnowledgePackage k : packages) {
            if(k.getName().equals(name)) {
                kp = k;
                break;
            }
        }
        return kp;
    }

    private static String getToAddNames(Collection<KnowledgePackage> toAdd) {
        StringBuilder sb = new StringBuilder();
        for(KnowledgePackage k : toAdd) {
            if(sb.length()>0)
                sb.append("\n");
            sb.append(k.getName()).append(", Rules: ");
            StringBuilder sbr = new StringBuilder();
            for(Rule rule : k.getRules()) {
                if(sbr.length()>0)
                    sbr.append(", ");
                sbr.append(rule.getName());
            }
            sb.append(sbr);
        }
        return sb.toString();
    }
}
