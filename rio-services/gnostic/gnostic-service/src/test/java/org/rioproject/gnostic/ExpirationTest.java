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

import junit.framework.Assert;
import org.drools.KnowledgeBase;
import org.drools.KnowledgeBaseConfiguration;
import org.drools.KnowledgeBaseFactory;
import org.drools.builder.*;
import org.drools.conf.EventProcessingOption;
import org.drools.io.ResourceFactory;
import org.drools.runtime.StatefulKnowledgeSession;
import org.drools.runtime.rule.WorkingMemoryEntryPoint;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.rioproject.system.measurable.memory.CalculableMemory;

import java.util.Properties;

/**
 * Tests event expiration
 *
 * @author Dennis Reedy
 */
public class ExpirationTest {
    private StatefulKnowledgeSession session;
    private WorkingMemoryEntryPoint stream;

    @Test
    public void testExpiration() throws InterruptedException {
        for(int i=0; i<1000; i++) {
            stream.insert(new CalculableMemory("Foo", 0.9, System.currentTimeMillis()));
            Thread.sleep(1);
            session.fireAllRules();
        }
        int wait = 1;
        while(wait <= 60) {
            System.out.println("\r["+String.format("%2d", wait)+"] Fact Count : "+stream.getFactCount()+", Fact Handles : "+stream.getFactHandles().size());
            if(stream.getFactCount()==0 && stream.getFactHandles().size()==0)
                break;
            Thread.sleep(1000);
            wait++;
            session.fireAllRules();
        }
        Assert.assertTrue("Expected fact count to be 0", stream.getFactCount()==0);
        Assert.assertTrue("Expected fact handles to be 0", stream.getFactHandles().size()==0);
    }

    @Before
    public void setupDrools() {
        Properties props = new Properties();
        props.setProperty("drools.dialect.java.compiler", "JANINO");
        props.setProperty("drools.dialect.java.compiler.lnglevel","1.7" );
        KnowledgeBuilderConfiguration config =
            KnowledgeBuilderFactory.newKnowledgeBuilderConfiguration(props);

        KnowledgeBuilder kbuilder = KnowledgeBuilderFactory.newKnowledgeBuilder(config);
        kbuilder.add(ResourceFactory.newClassPathResource("Expiration.drl"), ResourceType.DRL);
        if (kbuilder.hasErrors()) {
            System.out.println("Could not create the KnowledgeBuilder using Expiration.drl properly");
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

        //KnowledgeRuntimeLoggerFactory.newConsoleLogger(session);

        stream = session.getWorkingMemoryEntryPoint(Constants.CALCULABLES_STREAM);
        assert stream != null;
    }

    @After
    public void releaseDroolsSession() {
        if (session != null)
            session.dispose();
    }
}
