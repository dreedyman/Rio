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
import org.drools.builder.*;
import org.drools.conf.EventProcessingOption;
import org.drools.event.rule.BeforeActivationFiredEvent;
import org.drools.event.rule.DefaultAgendaEventListener;
import org.drools.io.ResourceFactory;
import org.drools.logger.KnowledgeRuntimeLoggerFactory;
import org.drools.runtime.StatefulKnowledgeSession;
import org.drools.runtime.rule.WorkingMemoryEntryPoint;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.rioproject.system.measurable.cpu.CalculableCPU;

import java.util.Properties;

/**
 * Test sla-watches.drl rules.
 */
public class SLAWatchesRulesTest {
    private StatefulKnowledgeSession session;
    private WorkingMemoryEntryPoint stream;

    @Test
    public void testFiringWithNoEventInserted() {
        session.fireAllRules();
    }

    @Test
    public void testFiringWithOneCalculableCPUInserted() {
        stream.insert(new CalculableCPU("CPU", 0.4, System.currentTimeMillis()));
        session.fireAllRules();
    }

    @Test
    public void testFiringWithHighCPU() {
        stream.insert(new CalculableCPU("CPU", 0.8, System.currentTimeMillis()));
        DebugAgendaEventListener debugListener = new DebugAgendaEventListener();
        session.addEventListener(debugListener);
        session.fireAllRules();
        assert debugListener.hasFired(); 
    }

    @Before
    public void setupDrools() {
        Properties props = new Properties();
        props.setProperty("drools.dialect.java.compiler", "JANINO");
        props.setProperty("drools.dialect.java.compiler.lnglevel","1.6" );
        KnowledgeBuilderConfiguration config =
            KnowledgeBuilderFactory.newKnowledgeBuilderConfiguration(props);

        KnowledgeBuilder kbuilder = KnowledgeBuilderFactory.newKnowledgeBuilder(config);
        kbuilder.add(ResourceFactory.newClassPathResource("sla-watches.drl"), ResourceType.DRL);
        if (kbuilder.hasErrors()) {
            System.out.println("Could not create the KnowledgeBuilder using sla-watches.drl properly");
            for (KnowledgeBuilderError error : kbuilder.getErrors()) {
                StringBuilder sb = new StringBuilder();
                for(int i : error.getLines()) {
                    if(sb.length()>0)
                        sb.append(", ");
                    sb.append(i);
                }
                System.out.println("At lines "+sb.toString()+", got error "+error.getMessage());
            }
        }

        KnowledgeBaseConfiguration conf = KnowledgeBaseFactory.newKnowledgeBaseConfiguration();
        conf.setOption(EventProcessingOption.STREAM);

        KnowledgeBase kbase = KnowledgeBaseFactory.newKnowledgeBase(conf);
        kbase.addKnowledgePackages(kbuilder.getKnowledgePackages());
        session = kbase.newStatefulKnowledgeSession();
        assert session != null;

        KnowledgeRuntimeLoggerFactory.newConsoleLogger(session);

        stream = session.getWorkingMemoryEntryPoint(Constants.CALCULABLES_STREAM);
        assert stream != null;
    }

    @After
    public void releaseDroolsSession() {
        if (session != null)
            session.dispose();
    }

    class DebugAgendaEventListener extends DefaultAgendaEventListener {
        boolean fired = false;

        public void beforeActivationFired(BeforeActivationFiredEvent event) {
            fired = true;
        }

        public boolean hasFired() {
            return fired;
        }
    }
}
