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

import org.drools.agent.KnowledgeAgent;
import org.drools.builder.ResourceType;
import org.drools.event.rule.DebugWorkingMemoryEventListener;
import org.drools.io.Resource;
import org.drools.io.ResourceFactory;
import org.drools.logger.KnowledgeRuntimeLoggerFactory;
import org.drools.runtime.StatefulKnowledgeSession;
import org.drools.runtime.rule.WorkingMemoryEntryPoint;
import org.drools.template.ObjectDataCompiler;
import org.rioproject.sla.RuleMap;
import org.rioproject.sla.SLA;
import org.rioproject.util.StringUtil;
import org.rioproject.watch.Calculable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.URL;
import java.util.*;

/**
 * Creates and manages the Drools knowledge session.
 */
public class DroolsCEPManager implements CEPSession {
    private StatefulKnowledgeSession session;
    private WorkingMemoryEntryPoint stream;
    private final KnowledgeAgent kAgent;
    //private final KnowledgeBase kBase;
    private final DeployedServiceContext context;
    private Logger logger = LoggerFactory.getLogger(DroolsCEPManager.class.getName());
    private Logger droolsLogger = LoggerFactory.getLogger("org.rioproject.gnostic.drools");
    private boolean initialized = false;

    public DroolsCEPManager(DeployedServiceContext context, KnowledgeAgent kAgent) {
        this.context = context;
        this.kAgent = kAgent;
    }

    /*public DroolsCEPManager(DeployedServiceContext context,
                            KnowledgeBase kBase) {
        this.context = context;
        this.kBase = kBase;
    }*/

    public void initialize(List<ServiceHandle> serviceHandles,
                           RuleMap ruleMap,
                           ClassLoader loader) {
        try {
            Map<Resource, ResourceType> resources = new HashMap<Resource, ResourceType>();
            RuleMap.RuleDefinition ruleDef = ruleMap.getRuleDefinition();

            List<String> rules = new ArrayList<String>();
            boolean classPathResource = false;
            for (String rule : StringUtil.toArray(ruleDef.getResource(), " ,")) {
                logger.info("PROCESSING: {}", rule);
                if (rule.startsWith("http")) {
                    resources.put(ResourceFactory.newUrlResource(rule), ResourceType.DRL);
                } else if (rule.startsWith("file:")) {
                    rule = rule.substring(5, rule.length());
                    if (!rule.endsWith(".drl"))
                        rule = rule + ".drl";
                    rules.add("file:"+rule);
                    if (isBuiltInRule(rule)) {
                        for (Resource r : generateRule(rule, true, serviceHandles))
                            resources.put(r, ResourceType.DRL);
                    } else {
                        resources.put(ResourceFactory.newFileResource(rule), ResourceType.DRL);
                    }
                } else {
                    if (!rule.endsWith(".drl"))
                        rule = rule + ".drl";
                    if (isBuiltInRule(rule)) {
                        for (Resource r : generateRule(rule, false, serviceHandles))
                            resources.put(r, ResourceType.DRL);
                    } else {
                        classPathResource = true;
                        rules.add(rule);
                        resources.put(ResourceFactory.newClassPathResource(rule), ResourceType.DRL);
                    }
                }
            }

            if(!classPathResource) {
                try {
                    generateAndApplyChangeSet(rules);
                } catch (IOException e) {
                    StringBuilder sb = new StringBuilder();
                    for(String rule : rules) {
                        if(sb.length()>0)
                            sb.append(", ");
                        sb.append(rule);
                    }
                    logger.warn("Unable to provide change-set support for the rules {}", sb.toString(), e);
                }
            }

            /* Create the Drools StatefulKnowledgeSession */
            try {
                session = DroolsFactory.createStatefulSession(kAgent.getKnowledgeBase(),
                                                              //kBase,
                                                              resources,
                                                              loader);
            } catch (Throwable t) {
                logger.warn("While creating StatefulKnowledgeSession for {}", ruleMap, t);
            }
            if (session == null) {
                throw new IllegalStateException(String.format("Could not create StatefulKnowledgeSession for %s", ruleMap));
            }
            if(session.getGlobal("context")==null)
                session.setGlobal("context", context);

            // log all Drools activity
            if(droolsLogger.isTraceEnabled()) {
                KnowledgeRuntimeLoggerFactory.newConsoleLogger(session);
                session.addEventListener(new DebugWorkingMemoryEventListener());
            }

            stream = session.getWorkingMemoryEntryPoint(Constants.CALCULABLES_STREAM);
        } finally {
            initialized = true;
        }
    }

    public void insert(Calculable calculable) {
        if(calculable==null)
            return;
        if(!initialized) {
            while(!initialized) {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    logger.warn("Waiting for {} initialization interrupted", getClass().getName(), e);
                }
            }
        }
        if(stream==null)
            throw new IllegalStateException("Could not insert calculable into CEP engine, " +
                                            "the working memory entryPoint (stream) is null");
        logger.trace("Inserting [{}] into CEP engine", calculable);
        try {
            stream.insert(calculable);
        } catch(Exception e) {
            logger.warn("Could not insert calculable into CEP engine", e);
            return;
        }
        // fire CEPSession engine for rules
        try {
            session.fireAllRules();
        } catch (Throwable t) {
            logger.warn("Could not fire rules", t);
        }
        logger.debug("Working Memory Size: {}", session.getFactCount());
    }

    public void close() {
        if(session!=null)
            session.dispose();
    }

    private void generateAndApplyChangeSet(List<String> rules) throws IOException {

        //http://anonsvn.jboss.org/repos/labs/labs/jbossrules/trunk/drools-api/src/main/resources/change-set-1.0.0.xsd
        StringBuilder sb = new StringBuilder();
		sb.append("<change-set xmlns='http://drools.org/drools-5.0/change-set'").append("\n");
		sb.append("    xmlns:xs='http://www.w3.org/2001/XMLSchema-instance'").append("\n");
		sb.append("    xs:schemaLocation='http://drools.org/drools-5.0/change-set ");
        sb.append("http://anonsvn.jboss.org/repos/labs/labs/jbossrules/trunk/drools-api/src/main/resources/change-set-1.0.0.xsd' >").append("\n");
		sb.append("    <add> ").append("\n");
        for(String rule : rules)
            sb.append("        <resource source=\'").append(rule).append("\' type='DRL' />").append("\n");
		sb.append("    </add> ").append("\n");
		sb.append("</change-set>").append("\n");
		File changeSetFile = File.createTempFile("changeSet", ".xml");
        Writer output;
        try {
            output = new BufferedWriter(new FileWriter(changeSetFile));
            output.write(sb.toString());
            close(output);
            URL url = changeSetFile.toURI().toURL();
            logger.info("kAgent: {} url: {}", kAgent, url);
            kAgent.applyChangeSet(ResourceFactory.newUrlResource(url));
        } finally {
            if(changeSetFile.delete()) {
                logger.trace("Removed temporary change set file {}", changeSetFile.getPath());
            }
        }
    }

    private boolean isBuiltInRule(String rule) {
        boolean isBuiltIn = false;
        for (String s : Constants.BUILT_IN_RULES) {
            if (rule.endsWith(s)) {
                isBuiltIn = true;
                break;
            }
        }
        return isBuiltIn;
    }

    private List<Resource> generateRule(String rule,
                                        boolean asFile,
                                        List<ServiceHandle> serviceHandles) {
        List<Resource> resources = new ArrayList<Resource>();
        InputStream is = null;
        for (ServiceHandle sh : serviceHandles) {
            for (Map.Entry<String, SLA> entry : sh.getSLAMap().entrySet()) {
                String watchID = entry.getKey();
                SLA sla = entry.getValue();
                try {
                    if (asFile) {
                        is = new FileInputStream(rule);
                    } else {
                        is = this.getClass().getClassLoader().getResourceAsStream(rule);
                    }
                    if(is!=null)
                        resources.add(expand(is, new RuleParameters(sla,
                                                                    watchID,
                                                                    sh.elem)));
                    else
                        logger.warn("Unable to load template {}", rule);
                } catch (FileNotFoundException e) {
                    logger.warn("While expanding template {}", rule, e);
                } finally {
                    close(is);
                }
            }
        }
        return resources;
    }

    private Resource expand(InputStream is, RuleParameters ruleParameters) {
        ObjectDataCompiler converter = new ObjectDataCompiler();
        Collection<RuleParameters> parameters = new ArrayList<RuleParameters>();
        parameters.add(ruleParameters);
        String drl = converter.compile(parameters, is);
        logger.trace("\n------------\n{}\n------------", drl);
        Reader rdr = new StringReader(drl);
        return ResourceFactory.newReaderResource(rdr);
    }

    private void close(Closeable c) {
        try {
            if(c!=null)
                c.close();
        } catch (IOException e) {
            /**/
        }
    }
}
