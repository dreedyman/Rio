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
package org.rioproject.gnostic.service;

import junit.framework.Assert;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.rioproject.associations.AssociationDescriptor;
import org.rioproject.impl.associations.DefaultAssociationManagement;
import org.rioproject.gnostic.Gnostic;
import org.rioproject.gnostic.service.test.TestService;
import org.rioproject.sla.RuleMap;
import org.rioproject.sla.RuleMap.RuleDefinition;
import org.rioproject.sla.RuleMap.ServiceDefinition;
import org.rioproject.test.RioTestRunner;
import org.rioproject.test.SetTestManager;
import org.rioproject.test.TestManager;

import java.io.File;
import java.rmi.NoSuchObjectException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.*;

/**
 * Test ProvisionMonitor rules
 */
@RunWith (RioTestRunner.class)
public class ITMonitorRulesTest {
    Iterable<TestService> s1Instances;
    Iterable<TestService> s2Instances;
    private final int COUNT = 20;
    @SetTestManager
    static TestManager testManager;
    static Gnostic gnostic;

    @BeforeClass
    public static void setup() throws Exception {
        Assume.assumeTrue(Util.getJVMVersion() < 1.8);

        gnostic = testManager.waitForService(Gnostic.class);
        testManager.waitForService(TestService.class, "S1");
        Assert.assertNotNull(gnostic);
    }

    @Test
    public void runTests() throws ExecutionException, InterruptedException {
        verifyScalingUpWorks();
        verifyScalingDownWorks();
        verifyScalingMultipleServicesWorks();
    }

    @SuppressWarnings("unchecked")
    public void verifyScalingUpWorks() {
        Throwable thrown = null;
        try {
            s1Instances = getServices("S1", TestService.class, testManager.getGroups());
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            RuleMap ruleMap = getRuleMap();
            addRuleMap(ruleMap);
            List<RuleMap> ruleMappings = gnostic.get();
            Assert.assertEquals("Expected 1 RuleMap", 1, ruleMappings.size());
            Assert.assertEquals("RuleMaps should be the same", ruleMap, ruleMappings.get(0));
            List<TestService> services = null;
            for(int i=1; i<COUNT; i++) {
                services = (List<TestService>) testManager.getServices(TestService.class, "S1");
                increaseLoad(services);
                int z=0;
                for(TestService t : s1Instances)
                    z++;
                if(services.size()==5)
                System.out.println("[scale-up-test]          Num S1="+z);
            }
            Assert.assertNotNull(services);
            Assert.assertEquals("Should have 5 services", 5, services.size());
        } catch (Exception e) {
            e.printStackTrace();
            thrown = e;
        }
        Assert.assertNull(thrown);
    }

    @SuppressWarnings("unchecked")
    public void verifyScalingDownWorks() throws ExecutionException, InterruptedException {
        List<TestService> services = (List<TestService>)testManager.getServices(TestService.class, "S1");
        while(services.size()>1) {
            for(TestService s : services) {
                Throwable thrown = null;
                try {
                    double d = s.getLoad();
                    s.setLoad(d==0?d:d-10);
                    Thread.sleep(1000);
                } catch(NoSuchObjectException e) {
                    /* we may get this exception if we try and get the load
                     * of a service that has been decremented */
                } catch (Exception e) {
                    e.printStackTrace();
                    thrown = e;
                }
                Assert.assertNull(thrown);                
            }
            services = (List<TestService>)testManager.getServices(TestService.class, "S1");
            System.out.println("[scale-down-test]        Num S1="+services.size());
        }
        Throwable thrown = null;
        try {
            List<RuleMap> ruleMaps = gnostic.get();
            Assert.assertNotNull(ruleMaps);
            Assert.assertEquals("Expected 1 RuleMap", 1, ruleMaps.size());
            boolean removed = gnostic.remove(ruleMaps.get(0));
            Assert.assertTrue(removed);
            ruleMaps = gnostic.get();
            Assert.assertNotNull(ruleMaps);
            Assert.assertEquals("Expected zero RuleMaps", 0, ruleMaps.size());
        } catch (RemoteException e) {
            e.printStackTrace();
            thrown = e;
        }
        Assert.assertNull(thrown);
    }

    @SuppressWarnings("unchecked")
    public void verifyScalingMultipleServicesWorks() {
        Throwable thrown = null;
        try {
            testManager.deploy(new File("src"+File.separator+"test"+File.separator+"opstring", "testOpstring1.groovy"));
            try {
                s1Instances = getServices("S1", TestService.class, testManager.getGroups());
                s2Instances = getServices("S2", TestService.class, testManager.getGroups());
            } catch (Exception e) {
                e.printStackTrace();
            }

            RuleMap ruleMap = getRuleMap();
            RuleMap ruleMap2 = getRuleMap2();
            Assert.assertFalse("RuleMaps should not equal each other", ruleMap.equals(ruleMap2));
            addRuleMap(ruleMap);
            addRuleMap(ruleMap2);
            List<RuleMap> ruleMaps = gnostic.get();
            Assert.assertEquals("Expected 2 RuleMaps", 2, ruleMaps.size());
            Assert.assertEquals("RuleMap #1 should be the same", ruleMap, ruleMaps.get(0));
            Assert.assertEquals("RuleMap #2 should be the same", ruleMap2, ruleMaps.get(1));
            for(int i=1; i<COUNT; i++) {
                List<TestService> services = (List<TestService>)testManager.getServices(TestService.class);
                increaseLoad(services);
                int z=0;
                for(TestService t : s1Instances)
                    z++;
                int y=0;
                for(TestService t : s2Instances)
                    y++;
                System.out.println("[scale-up-multiple-test] Num S1="+z+", S2="+y);
            }
            List<TestService> services = (List<TestService>)testManager.getServices(TestService.class, "S1");

            Assert.assertEquals("Should have 5 S1 services", 5, services.size());
            services = (List<TestService>)testManager.getServices(TestService.class, "S2");
            Assert.assertEquals("Should have 3 S2 services", 3, services.size());
        } catch (Exception e) {
            e.printStackTrace();
            thrown = e;
        }
        Assert.assertNull(thrown);
        System.out.println("Completed verifyScalingMultipleServicesWorks");
    }

    private void increaseLoad(Collection<TestService> services ) {
        for(TestService s : services) {
            Throwable t = null;
            try {
                double d = s.getLoad();
                if(d<80)
                    s.setLoad(d+10);
                Thread.sleep(1000);
            } catch (Exception e) {
                t = e;
            }
            Assert.assertNull(t);
        }
    }

    private void addRuleMap(RuleMap ruleMap) throws Exception {
        boolean added = gnostic.add(ruleMap);
        Assert.assertTrue(added);
        while(!hasRule(gnostic.get(), ruleMap))
            Thread.sleep(1000);
        added = gnostic.add(ruleMap);
        Assert.assertFalse("Should have returned false for re-adding an already existing RuleMap", added);
    }

    private static RuleMap getRuleMap() {
        RuleMap ruleMap = new RuleMap();
        List<ServiceDefinition> services = new ArrayList<ServiceDefinition>();
        ServiceDefinition service = new ServiceDefinition("S1");
        //service.addWatches("load", "CPU");
        service.addWatches("load");
        services.add(service);
        RuleDefinition rule = new RuleDefinition("ScalingRuleHandler");
        ruleMap.addRuleMapping(rule, services.toArray(new ServiceDefinition[services.size()]));
        return ruleMap;
    }

    private static RuleMap getRuleMap2() {
        RuleMap ruleMap = new RuleMap();
        List<ServiceDefinition> services = new ArrayList<ServiceDefinition>();
        ServiceDefinition service = new ServiceDefinition("S2");
        service.addWatches("load");
        services.add(service);
        
        service = new ServiceDefinition("S3");
        service.addWatches("load");
        services.add(service);

        RuleDefinition rule = new RuleDefinition("ScalingRuleHandler");

        ruleMap.addRuleMapping(rule, services.toArray(new ServiceDefinition[services.size()]));
        return ruleMap;
    }

    private boolean hasRule(List<RuleMap> ruleMaps, RuleMap ruleMap) {
        boolean hasRule = false;
        for(RuleMap rm : ruleMaps) {
            if(rm.equals(ruleMap)) {
                hasRule = true;
                break;
            }
        }
        return hasRule;
    }

    private <T> Iterable<T> getServices(String name, Class<T> service, String... groups) throws ExecutionException,
                                                                                                InterruptedException {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<Iterable<T>> future = executor.submit(new AssociationHelper<T>(name, service, groups));
        return future.get();
    }

    public class AssociationHelper<T> implements Callable<Iterable<T>> {
        Iterable<T> services;
        DefaultAssociationManagement mgr;
        String serviceName;

        AssociationHelper(String serviceName, Class<T> serviceClass, String... groups) {
            this.serviceName = serviceName;
            mgr = new DefaultAssociationManagement();
            services = mgr.addAssociationDescriptor(AssociationDescriptor.create(serviceName,
                                                                                 null,
                                                                                 serviceClass,
                                                                                 groups));
        }

        public Iterable<T> call() throws Exception {
            return services;
        }

        @Override
        protected void finalize() throws Throwable {
            //mgr.terminate();
            super.finalize();
        }
    }
}
