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
package org.rioproject.cybernode;

import junit.framework.Assert;
import net.jini.config.EmptyConfiguration;
import net.jini.id.Uuid;
import net.jini.id.UuidFactory;
import org.junit.Before;
import org.junit.Test;
import org.rioproject.deploy.ServiceRecord;
import org.rioproject.deploy.ServiceStatement;
import org.rioproject.opstring.ClassBundle;
import org.rioproject.opstring.ServiceBeanConfig;
import org.rioproject.opstring.ServiceElement;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Test the {@code TransientServiceStatementManager}.
 *
 * @author Dennis Reedy
 */
public class TransientServiceStatementManagerTest {
    private TransientServiceStatementManager serviceStatementManager;
    private final List<ServiceStatement> statements = new ArrayList<ServiceStatement>();
    private final Uuid recordingUuid = UuidFactory.generate();
    private final String[] names = new String[]{"Spanky",
                                                "Alfalfa",
                                                "Darla",
                                                "Stymie",
                                                "Froggy",
                                                "Porky",
                                                "Buckwheat",
                                                "Butch",
                                                "Waldo"};

    @Before
    public void setupTSSM() throws UnknownHostException {
        statements.clear();
        serviceStatementManager = new TransientServiceStatementManager(EmptyConfiguration.INSTANCE);

        for (String name : names) {
            Uuid uuid = UuidFactory.generate();
            ServiceElement element = makeServiceElement(name);
            ServiceRecord serviceRecord = new ServiceRecord(uuid, element, "hostname");
            ServiceStatement statement = new ServiceStatement(element);
            statement.putServiceRecord(recordingUuid, serviceRecord);
            statements.add(statement);
        }
        for(ServiceStatement statement : statements) {
            serviceStatementManager.record(statement);
        }
    }

    @Test
    public void testTerminate() throws Exception {
        serviceStatementManager.terminate();
        Assert.assertTrue(serviceStatementManager.get().length==0);
    }

    @Test
    public void testGetActiveServiceStatements() throws Exception {
        ServiceStatement[] statements = serviceStatementManager.get();
        List<ServiceRecord> list = new ArrayList<ServiceRecord>();
        for (ServiceStatement statement : statements) {
            ServiceRecord[] records = statement.getServiceRecords(recordingUuid, ServiceRecord.ACTIVE_SERVICE_RECORD);
            list.addAll(Arrays.asList(records));
        }
        Assert.assertEquals(names.length, list.size());
    }

    @Test
    public void testGetServiceStatementsAfterSettingInactiveThenActive() throws Exception {
        ServiceElement element = makeServiceElement("Buckwheat");
        Assert.assertNotNull(element);
        ServiceStatement statement = serviceStatementManager.get(element);
        Assert.assertNotNull(statement);
        ServiceRecord[] records = statement.getServiceRecords();
        Assert.assertEquals(1, records.length);
        ServiceRecord inActiveServiceRecord = records[0];
        inActiveServiceRecord.setType(ServiceRecord.INACTIVE_SERVICE_RECORD);
        /* Putting the INACTIVE_SERVICE_RECORD ServiceRecord will replace the ACTIVE_SERVICE_RECORD since the
         * records have the same Uuid */
        statement.putServiceRecord(recordingUuid, inActiveServiceRecord);
        ServiceRecord activeServiceRecord = new ServiceRecord(UuidFactory.generate(),
                                                              statement.getServiceElement(),
                                                              "hostname");
        statement.putServiceRecord(recordingUuid, activeServiceRecord);
        Assert.assertEquals(2, statement.getServiceRecords().length);
        serviceStatementManager.record(statement);

        ServiceStatement fetchedStatement = serviceStatementManager.get(element);
        Assert.assertNotNull(fetchedStatement);
        Assert.assertEquals(2, fetchedStatement.getServiceRecords().length);

        ServiceRecord[] activeRecords = fetchedStatement.getServiceRecords(ServiceRecord.ACTIVE_SERVICE_RECORD);
        Assert.assertEquals(1, activeRecords.length);
    }


    @Test
    public void testRecordAndRetrieve() throws Exception {
        Assert.assertEquals(statements.size(), serviceStatementManager.get().length);
        for(ServiceStatement statement : statements) {
            ServiceStatement s = serviceStatementManager.get(statement.getServiceElement());
            Assert.assertEquals(statement, s);
            Assert.assertEquals(1, s.getServiceRecords(ServiceRecord.ACTIVE_SERVICE_RECORD).length);
            Assert.assertEquals(0, s.getServiceRecords(ServiceRecord.INACTIVE_SERVICE_RECORD).length);
        }

    }

    private ServiceElement makeServiceElement(String name) {
        ServiceElement elem = new ServiceElement();
        ClassBundle main = new ClassBundle("");
        elem.setComponentBundle(main);
        ServiceBeanConfig sbc = new ServiceBeanConfig();
        sbc.setName(name);
        elem.setServiceBeanConfig(sbc);
        return elem;
    }
}
