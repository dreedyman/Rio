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
package org.rioproject.associations;

import junit.framework.Assert;
import net.jini.core.lookup.ServiceItem;
import org.junit.Test;

import java.util.Collection;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * Tests injecting associated services
 */
public class AssociationInjectorTest {

    @Test
    public void testInjectProxy() {
        Association<Dummy> a = new Association<Dummy>(createAssociationDescriptor());
        Target1 t = new Target1();
        AssociationInjector<Dummy> ai = new AssociationInjector<Dummy>(t);
        for(int i=0; i<10; i++) {
            Dummy dummy = new DummyImpl(i);
            a.addServiceItem(AssociationUtils.makeServiceItem(dummy));
            ai.discovered(a, dummy);
        }
        Assert.assertEquals(1, t.injectedCount);
    }

    @Test
    public void testInjectProxyWithListener() {
        Association<Dummy> a = new Association<Dummy>(createAssociationDescriptor());
        Target1 t = new Target1();
        AssociationInjector<Dummy> ai = new AssociationInjector<Dummy>(t);
        AL aL = new AL();
        a.registerAssociationServiceListener(aL);
        for(int i=0; i<10; i++) {
            Dummy dummy = new DummyImpl(i);
            a.addServiceItem(AssociationUtils.makeServiceItem(dummy));
            ai.discovered(a, dummy);
        }
        Assert.assertEquals(1, t.injectedCount);
        Assert.assertEquals(10, aL.addedServiceCount.get());
        Assert.assertEquals(0, aL.removedServiceCount.get());
        Collection<Dummy> dummies = a.getServices();
        for(Dummy d: dummies)
            a.removeService(d);
        Assert.assertEquals(10, aL.removedServiceCount.get());
        Assert.assertEquals(0, a.getServiceCount());
    }

    @Test
    public void testInjectIterable() {
        Association<Dummy> a = new Association<Dummy>(createAssociationDescriptor());
        Target2 t = new Target2();
        AssociationInjector<Dummy> ai = new AssociationInjector<Dummy>(t);
        for(int i=0; i<10; i++) {
            Dummy dummy = new DummyImpl(i);
            a.addServiceItem(AssociationUtils.makeServiceItem(dummy));
            ai.discovered(a, dummy);
        }
        Assert.assertEquals(1, t.injectedCount);
        int j = 0;
        for(Dummy dummy : t.dummies)
            j++;
        Assert.assertEquals(10, j);
    }

    @Test
    public void testInjectAssociation() {
        Association<Dummy> a = new Association<Dummy>(createAssociationDescriptor());
        Target3 t = new Target3();
        AssociationInjector<Dummy> ai = new AssociationInjector<Dummy>(t);
        for(int i=0; i<10; i++) {
            Dummy dummy = new DummyImpl(i);
            ServiceItem item = AssociationUtils.makeServiceItem(dummy);
            a.addServiceItem(item);
            ai.discovered(a, dummy);
        }
        Assert.assertEquals(1, t.injectedCount);
    }

    @Test
    public void testInjectMultipleAssociation() {
        Target4 t = new Target4();
        AssociationInjector<Dummy> ai = new AssociationInjector<Dummy>(t);
        setup(10, "uno", ai);
        setup(9, "dos", ai);
        setup(8, "tres", ai);

        Assert.assertEquals(3, t.injectedCount);
        Assert.assertEquals(10, t.unoDummies.getServiceCount());
        Assert.assertEquals(9, t.dosDummies.getServiceCount());
        Assert.assertEquals(8, t.tresDummies.getServiceCount());
    }
    
    @Test
    public void testEagerInjection() {
        AssociationMgmt aMgr = new AssociationMgmt();
        Target5 target = new Target5();
        aMgr.setBackend(target);
        AssociationDescriptor descriptor = createAssociationDescriptor();
        descriptor.setLazyInject(false);
        Association<Dummy> a = aMgr.addAssociationDescriptor(descriptor);
        Assert.assertTrue(target.injectedCount==1);
        Assert.assertNotNull(target.dummy);
        Assert.assertEquals(Association.State.PENDING, target.dummy.getState());
    }

    public class Target1 {
        int injectedCount;

        public void setDummy(Dummy dummy) {
            injectedCount++;
        }
    }

    public class Target2 {
        int injectedCount;
        Iterable<Dummy> dummies;

        public void setDummy(Iterable<Dummy> dummies) {
            this.dummies = dummies;
            injectedCount++;
        }
    }

    public class Target3 {
        int injectedCount;

        public void setDummy(Association<Dummy> dummies) {
            injectedCount++;
        }
    }

    public class Target4 {
        int injectedCount;
        Association<Dummy> unoDummies;
        Association<Dummy> dosDummies;
        Association<Dummy> tresDummies;

        public void setDummy(Association<Dummy> dummies) {
            injectedCount++;
            if(dummies.getName().equals("uno"))
                unoDummies = dummies;
            else if(dummies.getName().equals("dos"))
                dosDummies = dummies;
            else if(dummies.getName().equals("tres"))
                tresDummies = dummies;
            else
                Assert.fail("Unknown association "+dummies);
        }
    }

    public class Target5 {
        int injectedCount;
        Association<Dummy> dummy;

        public void setDummy(Association<Dummy> dummy) {
            this.dummy = dummy;
            injectedCount++;
        }
    }

    private class AL implements AssociationServiceListener<Dummy> {
        AtomicInteger addedServiceCount = new AtomicInteger();
        AtomicInteger removedServiceCount = new AtomicInteger();

        public void serviceAdded(Dummy service) {
            addedServiceCount.incrementAndGet();
        }

        public void serviceRemoved(Dummy service) {
            removedServiceCount.incrementAndGet();
        }
    }
    
    private AssociationDescriptor createAssociationDescriptor() {
        return createAssociationDescriptor(null);
    }

    private AssociationDescriptor createAssociationDescriptor(String name) {
        AssociationDescriptor ad = new AssociationDescriptor();
        ad.setName(name);
        ad.setInterfaceNames(Dummy.class.getName());
        ad.setPropertyName("dummy");
        return ad;
    }

    private void setup(int num, String name, AssociationInjector<Dummy> ai) {
        Association<Dummy> a = new Association<Dummy>(createAssociationDescriptor(name));
        for(int i=0; i<num; i++) {
            DummyImpl d1 = new DummyImpl(i, name);
            a.addServiceItem(AssociationUtils.makeServiceItem(d1));
            ai.discovered(a, d1);
        }
    }
}
