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
package org.rioproject.deploy;

import junit.framework.Assert;
import net.jini.id.Uuid;
import net.jini.id.UuidFactory;
import org.junit.Test;
import org.rioproject.opstring.ClassBundle;
import org.rioproject.opstring.ServiceBeanConfig;
import org.rioproject.opstring.ServiceElement;

import java.util.ArrayList;
import java.util.List;

/**
 * Test {@code ServiceStatement}s
 *
 * @author Dennis Reedy
 */
public class ServiceStatementTest {
    private final Uuid recordingUuid = UuidFactory.generate();

    @Test
    public void testAddingServiceRecords() {
        List<ServiceRecord> list = new ArrayList<ServiceRecord>();
        ServiceElement element = makeServiceElement("Foo");
        int recordCount = 200;
        for(int i=0; i<recordCount; i++) {
            Uuid uuid = UuidFactory.generate();
            list.add(new ServiceRecord(uuid, element, "hostname"));

        }
        ServiceStatement statement = new ServiceStatement(element);
        for(ServiceRecord serviceRecord : list) {
            statement.putServiceRecord(recordingUuid, serviceRecord);
        }
        Assert.assertEquals(recordCount, statement.getServiceRecords().length);
        Assert.assertEquals(recordCount, statement.getServiceRecords(recordingUuid, ServiceRecord.ACTIVE_SERVICE_RECORD).length);
        Assert.assertEquals(1, statement.getServiceRecords(list.get(0).getServiceID()).length);
    }

    @Test
    public void testUpdatingServiceRecords() {
        List<ServiceRecord> list = new ArrayList<ServiceRecord>();
        ServiceElement element = makeServiceElement("Foo");
        int recordCount = 200;
        for(int i=0; i<recordCount; i++) {
            Uuid uuid = UuidFactory.generate();
            list.add(new ServiceRecord(uuid, element, "hostname"));

        }
        ServiceStatement statement = new ServiceStatement(element);
        for(ServiceRecord serviceRecord : list) {
            statement.putServiceRecord(recordingUuid, serviceRecord);
        }

        Assert.assertEquals(recordCount, statement.getServiceRecords().length);
        System.out.println("ServiceRecord count: "+statement.getServiceRecords().length);
        for(ServiceRecord record : statement.getServiceRecords()) {
            record.setType(ServiceRecord.INACTIVE_SERVICE_RECORD);
            statement.putServiceRecord(recordingUuid, record);
        }
        Assert.assertEquals(0, statement.getServiceRecords(ServiceRecord.ACTIVE_SERVICE_RECORD).length);
        Assert.assertFalse(statement.hasActiveServiceRecords());
    }

    @Test
    public void testGetOrganization() {
        ServiceElement element = makeServiceElement("Foo");
        Uuid uuid = UuidFactory.generate();
        ServiceRecord record = new ServiceRecord(uuid, element, "hostname");
        ServiceStatement statement = new ServiceStatement(element);
        statement.putServiceRecord(recordingUuid, record);
        String organization = statement.getOrganization();
        Assert.assertNotNull(organization);
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
