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
import org.rioproject.associations.Association;
import org.rioproject.associations.AssociationDescriptor;

import java.util.*;

/**
 * Tests adding, removing and iterating over collection of associated services
 */
public class AssociationCollectionTest {

    @Test
    public void testAdd() {
        Association<Dummy> a = new Association<Dummy>(new AssociationDescriptor());
        for(int i=0; i<10; i++)
            a.addServiceItem(AssociationUtils.makeServiceItem(i));

        Assert.assertEquals("Should have 10 associated services", 10, a.getServiceCount());

        int i=0;
        for(Dummy d : a.getServices()) {
            Assert.assertEquals(i, d.getIndex());
            i++;
        }
    }

    @Test
    public void testAddRemove() {
        Association<Dummy> a = new Association<Dummy>(new AssociationDescriptor());
        for(int i=0; i<10; i++)
            a.addServiceItem(AssociationUtils.makeServiceItem(i));

        Assert.assertEquals("Should have 10 associated services", 10, a.getServiceCount());
        ServiceItem[] items = a.getServiceItems();
        List<ServiceItem> list = new ArrayList<ServiceItem>();
        list.addAll(Arrays.asList(items));
        Random r = new Random();
        while(list.size()>0) {
            int toRemove = r.nextInt(list.size());
            ServiceItem item = list.remove(toRemove);
            ServiceItem removed = a.removeService((Dummy)item.service);
            Assert.assertEquals(item.service, removed.service);
        }
        Assert.assertEquals("Should have 0 associated services", 0, a.getServiceCount());
    }

    @Test
    public void testNext() {
        Association<Dummy> a = new Association<Dummy>(new AssociationDescriptor());
        for(int i=0; i<10; i++)
            a.addServiceItem(AssociationUtils.makeServiceItem(i));

        Assert.assertEquals("Should have 10 associated services", 10, a.getServiceCount());
        for(int i=0; i<a.getServiceCount(); i++) {
            ServiceItem item = a.getNextServiceItem();
            Assert.assertEquals(i, ((Dummy)item.service).getIndex());
        }
        Assert.assertEquals(0, ((Dummy)a.getNextServiceItem().service).getIndex());        
    }

    @Test
    public void testNextAndAdd() {
        Association<Dummy> a = new Association<Dummy>(new AssociationDescriptor());
        for(int i=0; i<10; i++)
            a.addServiceItem(AssociationUtils.makeServiceItem(i));

        Assert.assertEquals("Should have 10 associated services", 10, a.getServiceCount());
        for(int i=0; i<a.getServiceCount(); i++) {
            ServiceItem item = a.getNextServiceItem();
            Assert.assertEquals(i, ((Dummy)item.service).getIndex());
        }
        a.addServiceItem(AssociationUtils.makeServiceItem(a.getServiceCount()));
        Assert.assertEquals(10, ((Dummy)a.getNextServiceItem().service).getIndex());
    }

    @Test
    public void testNextAndRemove() {
        Association<Dummy> a = new Association<Dummy>(new AssociationDescriptor());
        for(int i=0; i<10; i++)
            a.addServiceItem(AssociationUtils.makeServiceItem(i));

        Assert.assertEquals("Should have 10 associated services", 10, a.getServiceCount());
        for(int i=0; i<a.getServiceCount(); i++) {
            ServiceItem item = a.getNextServiceItem();
            Assert.assertEquals(i, ((Dummy)item.service).getIndex());
        }       

        ServiceItem removed = a.removeService(new DummyImpl(0));
        Assert.assertNotNull(removed);
        Assert.assertEquals(9, a.getServiceCount());

        Assert.assertEquals(1, ((Dummy)a.getNextServiceItem().service).getIndex());
        Assert.assertEquals(2, ((Dummy)a.getNextServiceItem().service).getIndex());

        removed = a.removeService(new DummyImpl(3));
        Assert.assertNotNull(removed);
        Assert.assertEquals(8, a.getServiceCount());

        Assert.assertEquals(4, ((Dummy)a.getNextServiceItem().service).getIndex());
        Assert.assertEquals(5, ((Dummy)a.getNextServiceItem().service).getIndex());
        Assert.assertEquals(6, ((Dummy)a.getNextServiceItem().service).getIndex());

        removed = a.removeService(new DummyImpl(7));
        Assert.assertNotNull(removed);
        removed = a.removeService(new DummyImpl(8));
        Assert.assertNotNull(removed);
        removed = a.removeService(new DummyImpl(9));
        Assert.assertNotNull(removed);
        Assert.assertEquals(5, a.getServiceCount());
        removed = a.removeService(new DummyImpl(1));
        Assert.assertNotNull(removed);
        Assert.assertEquals(2, ((Dummy)a.getNextServiceItem().service).getIndex());
    }

    @Test
    public void testIterable() {
        Association<Dummy> a = new Association<Dummy>(new AssociationDescriptor());
        for(int i=0; i<10; i++)
            a.addServiceItem(AssociationUtils.makeServiceItem(i));

        Assert.assertEquals("Should have 10 associated services", 10, a.getServiceCount());
        int i=0;
        for(Dummy d : a) {
            Assert.assertEquals(i, d.getIndex());
            i++;
        }
    }

    @Test
    public void testEmpty() {
        Association<Dummy> a = new Association<Dummy>(new AssociationDescriptor());
        Assert.assertNull(a.getNextServiceItem());
        Assert.assertNull(a.getService());
        Assert.assertNull(a.getServiceItem());
        Assert.assertNull(a.removeService(new DummyImpl(0)));
        Assert.assertEquals(0, a.getServices().size());
        Assert.assertFalse(a.iterator().hasNext());
        Assert.assertEquals(0, a.getServiceCount());
    }
    
}
