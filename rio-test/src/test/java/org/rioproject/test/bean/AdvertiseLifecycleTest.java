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
package org.rioproject.test.bean;

import net.jini.core.lookup.ServiceItem;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.rioproject.associations.AssociationDescriptor;
import org.rioproject.associations.AssociationType;
import org.rioproject.cybernode.Cybernode;
import org.rioproject.cybernode.StaticCybernode;
import org.rioproject.deploy.ServiceBeanInstance;
import org.rioproject.deploy.ServiceBeanInstantiationException;
import org.rioproject.event.BasicEventConsumer;
import org.rioproject.event.RemoteServiceEvent;
import org.rioproject.event.RemoteServiceEventListener;
import org.rioproject.monitor.ProvisionFailureEvent;
import org.rioproject.monitor.ProvisionMonitor;
import org.rioproject.opstring.*;
import org.rioproject.test.RioTestRunner;
import org.rioproject.test.SetTestManager;
import org.rioproject.test.TestManager;

import java.io.IOException;


/**
 * Test pre and post advertisement invocations
 *
 * @author Dennis Reedy
 */
@RunWith(RioTestRunner.class)
public class AdvertiseLifecycleTest {
    @SetTestManager
    static TestManager testManager;
    ServiceItem[] monitorItems;
    ProvisionMonitor monitor;
    Cybernode cybernode;

    @Before
    public void getServices() {
        monitorItems = testManager.getServiceItems(ProvisionMonitor.class);
        Assert.assertEquals(1, monitorItems.length);
        monitor = (ProvisionMonitor)monitorItems[0].service;
        cybernode = (Cybernode)testManager.waitForService(Cybernode.class);
    }

    @Test
    public void testThatServiceThatThrowsDuringPreAdvertiseDoesNotGetCreated() {
        StaticCybernode cybernode = new StaticCybernode();
        ServiceBeanInstantiationException thrown = null;
        try {
            cybernode.activate(ServiceThatThrowsDuringPreAdvertise.class.getName());
        } catch(ServiceBeanInstantiationException e) {
            thrown = e;
        }
        Assert.assertNotNull("Expected a ServiceBeanInstantiationException got " +
                             (thrown == null ? "null" : thrown.getClass().getName()),
                             thrown);
    }

    @Test
    public void testThatServiceThatThrowsDuringPreAdvertiseDoesNotGetDeployed() throws Exception {
        ServiceElement element = makeServiceElement(ServiceThatThrowsDuringPreAdvertise.class.getName(),
                                                    "Test",
                                                    "Foo",
                                                    Boolean.TRUE.toString(),
                                                    1);
        PFEListener listener = new PFEListener();
        BasicEventConsumer eventConsumer = new BasicEventConsumer(ProvisionFailureEvent.getEventDescriptor(), listener);
        eventConsumer.register(monitorItems[0]);

        OpString opString = new OpString("Foo", null);
        opString.addService(element);
        Assert.assertNotNull(monitor);
        testManager.deploy(opString, monitor);
        try {
            for(int i=0; i<10; i++) {
                if(listener.failed!=null) {
                    break;
                }
                Thread.sleep(500);
            }
            Assert.assertNotNull(listener.failed);
            ServiceBeanInstance[] instances = cybernode.getServiceBeanInstances(element);
            Assert.assertEquals(0, instances.length);
        } finally {
            testManager.undeploy("Foo");
        }
    }

    @Test
    public void testThatServiceThrowsDuringPreAdvertiseWithRequiresAssociation() throws IOException,
                                                                                        ClassNotFoundException,
                                                                                        InterruptedException,
                                                                                        OperationalStringException {
        ServiceElement element1 = makeServiceElement(ServiceThatThrowsDuringPreAdvertise.class.getName(),
                                                    "Test",
                                                    "FooBar",
                                                    Boolean.TRUE.toString(),
                                                    1);
        ServiceElement element2 = makeServiceElement(ServiceThatThrowsDuringPreAdvertise.class.getName(),
                                                    "DependsOn",
                                                    "FooBar",
                                                    Boolean.FALSE.toString(),
                                                    0);

        AssociationDescriptor descriptor = new AssociationDescriptor(AssociationType.REQUIRES, "DependsOn");
        descriptor.setMatchOnName(true);
        //descriptor.setInterfaceNames(element2.getExportBundles()[0].getClassName());
        descriptor.setOperationalStringName(element2.getOperationalStringName());
        descriptor.setGroups(testManager.getGroups());
        element1.setAssociationDescriptors(descriptor);
        OpString opString = new OpString("FooBar", null);
        opString.addService(element1);
        opString.addService(element2);
        OperationalStringManager manager = testManager.deploy(opString, monitor);
        Assert.assertNotNull(manager);
        ServiceBeanInstance[] instances = new ServiceBeanInstance[0];
        for(int i=0; i<10; i++) {
            instances = cybernode.getServiceBeanInstances(element1);
            if(instances.length>0){
                break;
            }
            Thread.sleep(500);
        }
        Assert.assertEquals(1, instances.length);
        manager.increment(element2, true, null);

        testManager.waitForService("DependsOn");

       /* org.rioproject.resources.servicecore.Service proxy =
            (org.rioproject.resources.servicecore.Service)instances[0].getService();

        try {
            Object admin = proxy.getAdmin();
            ((ServiceBeanControl)admin).advertise();
        } catch(ServiceBeanControlException e) {
            e.printStackTrace();
        } catch (RemoteException e) {
            e.printStackTrace();
        }*/
    }

    ServiceElement makeServiceElement(String implClass,
                                      String name,
                                      String opstringName,
                                      String throwOnPreAdvertise,
                                      int planned)  {
        ServiceElement elem = new ServiceElement();
        ClassBundle main = new ClassBundle(implClass,
                                           new String[]{System.getProperty("user.dir")+"/target/test-classes/"},
                                           "file://");
        elem.setComponentBundle(main);
        ClassBundle export = new ClassBundle(org.rioproject.resources.servicecore.Service.class.getName(),
                                             new String[]{System.getProperty("user.dir")+"/target/test-classes/"},
                                             "file://");

        elem.setExportBundles(export);
        ServiceBeanConfig sbc = new ServiceBeanConfig();
        sbc.setName(name);
        sbc.setGroups(System.getProperty("org.rioproject.groups"));
        sbc.addInitParameter("throwOnPreAdvertise", Boolean.valueOf(throwOnPreAdvertise));
        elem.setServiceBeanConfig(sbc);
        elem.setOperationalStringName(opstringName);
        elem.setPlanned(planned);
        return elem;
    }

    class PFEListener implements RemoteServiceEventListener {
        ProvisionFailureEvent failed;

        public void notify(RemoteServiceEvent event) {
            failed = (ProvisionFailureEvent)event;
        }
    }
}
