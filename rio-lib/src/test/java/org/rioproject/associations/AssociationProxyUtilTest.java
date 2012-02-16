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
import org.junit.Before;
import org.junit.Test;

/**
 * Test functionality of the {@link  AssociationProxyUtil}
 */
public class AssociationProxyUtilTest {
    Association<Dummy> a;
    Target t;

    @Before
    public void setup() {
        a = new Association<Dummy>(createAssociationDescriptor("foo"));
        t = new Target();
        AssociationInjector<Dummy> ai = new AssociationInjector<Dummy>(t);
        for(int i=0; i<10; i++) {
            Dummy dummy = new DummyImpl(i);
            a.addServiceItem(AssociationUtils.makeServiceItem(dummy));
            ai.discovered(a, dummy);
        }
    }

    @Test
    public void testGetAssociationFromProxy() {
        Association<Dummy> association = AssociationProxyUtil.getAssociation(t.getDummy());
        Assert.assertNotNull(association);
    }

    @Test
    public void testRegen() throws ClassNotFoundException, IllegalAccessException, InstantiationException {
        Dummy d = t.getDummy();
        int index = d.getIndex();
        Dummy d1 = AssociationProxyUtil.regenProxy(t.getDummy());
        Assert.assertEquals(index, d1.getIndex());
    }

    public class Target {
        int injectedCount;
        Dummy dummy;

        Dummy getDummy() {
            return dummy;
        }

        public void setDummy(Dummy dummy) {
            this.dummy = dummy;
            injectedCount++;
        }
    }

    private AssociationDescriptor createAssociationDescriptor(String name) {
        AssociationDescriptor ad = new AssociationDescriptor();
        ad.setName(name);
        ad.setInterfaceNames(Dummy.class.getName());
        ad.setPropertyName("dummy");
        return ad;
    }
}
