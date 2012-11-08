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
import org.rioproject.deploy.ServiceBeanInstance;
import org.rioproject.event.BasicEventConsumer;
import org.rioproject.event.RemoteServiceEvent;
import org.rioproject.event.RemoteServiceEventListener;
import org.rioproject.monitor.ProvisionFailureEvent;
import org.rioproject.monitor.ProvisionMonitor;
import org.rioproject.monitor.ProvisionMonitorEvent;
import org.rioproject.opstring.*;
import org.rioproject.test.RioTestRunner;
import org.rioproject.test.SetTestManager;
import org.rioproject.test.TestManager;

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
    public void testThatServiceThatThrowsDuringPreAdvertiseDoesNotGetDeployed() throws Exception {
        String opStringName = "Foo";
        ServiceElement element = makeServiceElement(ServiceThatThrowsDuringAdvertiseCallbacks.class.getName(),
                                                    "Test",
                                                    opStringName,
                                                    Boolean.TRUE.toString(),
                                                    Boolean.FALSE.toString(),
                                                    1);
        PFEListener listener = new PFEListener();
        BasicEventConsumer eventConsumer = new BasicEventConsumer(ProvisionFailureEvent.getEventDescriptor(), listener);
        eventConsumer.register(monitorItems[0]);

        OpString opString = new OpString(opStringName, null);
        opString.addService(element);
        Assert.assertNotNull(monitor);
        testManager.deploy(opString, monitor);
        for(int i=0; i<10; i++) {
            if(listener.failed!=null) {
                break;
            }
            Thread.sleep(500);
        }
        Assert.assertNotNull(listener.failed);
        ServiceBeanInstance[] instances = cybernode.getServiceBeanInstances(element);
        Assert.assertEquals(0, instances.length);
        eventConsumer.terminate();
    }

    @Test
    public void testThatServiceThatThrowsDuringPostAdvertiseWithRequiresAssociation() throws Exception {
        String opStringName = "Bar";
        ServiceElement element1 = makeServiceElement(ServiceThatThrowsDuringAdvertiseCallbacks.class.getName(),
                                                    "Test",
                                                    opStringName,
                                                    Boolean.FALSE.toString(),
                                                    Boolean.TRUE.toString(),
                                                    1);
        ServiceElement element2 = makeServiceElement(ServiceThatThrowsDuringAdvertiseCallbacks.class.getName(),
                                                     "DependsOn",
                                                     opStringName,
                                                     Boolean.FALSE.toString(),
                                                     Boolean.FALSE.toString(),
                                                     1);
        AssociationDescriptor descriptor = new AssociationDescriptor(AssociationType.REQUIRES, "DependsOn");
        descriptor.setMatchOnName(true);
        descriptor.setOperationalStringName(element2.getOperationalStringName());
        descriptor.setGroups(testManager.getGroups());
        element1.addAssociationDescriptors(descriptor);
        OpString opString = new OpString(opStringName, null);
        opString.addService(element1);
        opString.addService(element2);
        OperationalStringManager manager = testManager.deploy(opString, monitor);
        Assert.assertNotNull(manager);
        testManager.waitForDeployment(manager);

        PMEListener listener = new PMEListener(ProvisionMonitorEvent.Action.SERVICE_BEAN_DECREMENTED);
        BasicEventConsumer eventConsumer = new BasicEventConsumer(ProvisionMonitorEvent.getEventDescriptor(), listener);
        eventConsumer.register(monitorItems[0]);

        manager.removeServiceElement(element2, true);

        for(int i=0; i<10; i++) {
            if(listener.event!=null &&
               listener.event.getAction().equals(ProvisionMonitorEvent.Action.SERVICE_BEAN_DECREMENTED)) {
                break;
            }
            Thread.sleep(500);
        }
        eventConsumer.terminate();
        Assert.assertNotNull(listener.event);
        Assert.assertEquals(ProvisionMonitorEvent.Action.SERVICE_BEAN_DECREMENTED, listener.event.getAction());
        Assert.assertEquals(element1, listener.event.getServiceElement());

        OperationalString operationalString = manager.getOperationalString();
        Assert.assertEquals(1, operationalString.getServices().length);
        Assert.assertEquals("Test", operationalString.getServices()[0].getName());
        Assert.assertEquals(0, operationalString.getServices()[0].getPlanned());
    }

    @Test
    public void testThatServiceThrowsDuringPreAdvertiseWithRequiresAssociation() throws Exception {
        ServiceElement element1 = makeServiceElement(ServiceThatThrowsDuringAdvertiseCallbacks.class.getName(),
                                                    "Test",
                                                    "FooBar",
                                                    Boolean.TRUE.toString(),
                                                    Boolean.FALSE.toString(),
                                                    1);
        ServiceElement element2 = makeServiceElement(ServiceThatThrowsDuringAdvertiseCallbacks.class.getName(),
                                                     "DependsOn",
                                                     "FooBar",
                                                     Boolean.FALSE.toString(),
                                                     Boolean.FALSE.toString(),
                                                     0);

        AssociationDescriptor descriptor = new AssociationDescriptor(AssociationType.REQUIRES, "DependsOn");
        descriptor.setMatchOnName(true);
        descriptor.setOperationalStringName(element2.getOperationalStringName());
        descriptor.setGroups(testManager.getGroups());
        element1.addAssociationDescriptors(descriptor);
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
        PMEListener listener = new PMEListener(ProvisionMonitorEvent.Action.SERVICE_BEAN_DECREMENTED);
        BasicEventConsumer eventConsumer = new BasicEventConsumer(ProvisionMonitorEvent.getEventDescriptor(), listener);
        eventConsumer.register(monitorItems[0]);

        testManager.waitForService("DependsOn");
        for(int i=0; i<10; i++) {
            if(listener.event!=null &&
               listener.event.getAction().equals(ProvisionMonitorEvent.Action.SERVICE_BEAN_DECREMENTED)) {
                break;
            }
            Thread.sleep(500);
        }
        eventConsumer.terminate();
        Assert.assertNotNull(listener.event);
        Assert.assertEquals(ProvisionMonitorEvent.Action.SERVICE_BEAN_DECREMENTED, listener.event.getAction());
        instances = cybernode.getServiceBeanInstances(element1);
        Assert.assertEquals(0, instances.length);
        instances = manager.getServiceBeanInstances(element1);
        Assert.assertEquals(0, instances.length);
        OperationalString operationalString = manager.getOperationalString();
        ServiceElement element1AfterDecrement = null;
        for(ServiceElement service : operationalString.getServices()) {
            if(service.getName().equals("Test")) {
                element1AfterDecrement = service;
                break;
            }
        }

        Assert.assertNotNull(element1AfterDecrement);
        Assert.assertEquals(0, element1AfterDecrement.getPlanned());
    }

    private ServiceElement makeServiceElement(String implClass,
                                              String name,
                                              String opstringName,
                                              String throwOnPreAdvertise,
                                              String throwOnPostUnAdvertise,
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
        sbc.addInitParameter("throwOnPostUnAdvertise", Boolean.valueOf(throwOnPostUnAdvertise));
        elem.setServiceBeanConfig(sbc);
        elem.setOperationalStringName(opstringName);
        elem.setPlanned(planned);
        elem.setFaultDetectionHandlerBundle(null);
        return elem;
    }

    class PFEListener implements RemoteServiceEventListener {
        ProvisionFailureEvent failed;

        public void notify(RemoteServiceEvent event) {
            failed = (ProvisionFailureEvent)event;
        }
    }

    class PMEListener implements RemoteServiceEventListener {
        ProvisionMonitorEvent event;
        ProvisionMonitorEvent.Action actionToMatch;

        PMEListener(ProvisionMonitorEvent.Action actionToMatch) {
            this.actionToMatch = actionToMatch;
        }

        public void notify(RemoteServiceEvent rEvent) {
            if(((ProvisionMonitorEvent)rEvent).getAction().equals(actionToMatch))
                event = (ProvisionMonitorEvent)rEvent;
        }
    }
}
