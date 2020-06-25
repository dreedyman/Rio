/*
 * Copyright to the original author or authors.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *         http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.rioproject.impl.associations;

import org.junit.Assert;
import net.jini.core.entry.Entry;
import net.jini.core.lookup.ServiceID;
import net.jini.core.lookup.ServiceItem;
import net.jini.lookup.entry.Name;
import org.junit.Test;
import org.rioproject.associations.AssociationDescriptor;
import org.rioproject.entry.OperationalStringEntry;
import org.rioproject.entry.VersionEntry;
import org.rioproject.impl.associations.filter.VersionMatchFilter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * @author Dennis Reedy
 */
public class VersionMatchFilterTest {

    @Test
    public void testVersionMatchFilter1() {
        VersionMatchFilter versionMatchFilter = new VersionMatchFilter();
        Assert.assertTrue(versionMatchFilter.check(makeAssociationDescriptor("1.*"), makeServiceItem("1.1")));
    }

    @Test
    public void testVersionMatchFilter2() {
        VersionMatchFilter versionMatchFilter = new VersionMatchFilter();
        Assert.assertFalse(versionMatchFilter.check(makeAssociationDescriptor("1.*"), makeServiceItem()));
    }

    @Test
    public void testVersionMatchFilter3() {
        VersionMatchFilter versionMatchFilter = new VersionMatchFilter();
        Assert.assertFalse(versionMatchFilter.check(makeAssociationDescriptor("1.*"), makeServiceItem("2.1")));
    }

    @Test
    public void testVersionMatchFilter4() {
        VersionMatchFilter versionMatchFilter = new VersionMatchFilter();
        Assert.assertTrue(versionMatchFilter.check(makeAssociationDescriptor(null), makeServiceItem()));
    }

    @Test
    public void testVersionMatchFilter5() {
        VersionMatchFilter versionMatchFilter = new VersionMatchFilter();
        Assert.assertTrue(versionMatchFilter.check(makeAssociationDescriptor(null), makeServiceItem()));
    }

    @Test
    public void testVersionMatchFilter6() {
        VersionMatchFilter versionMatchFilter = new VersionMatchFilter();
        Assert.assertFalse(versionMatchFilter.check(makeAssociationDescriptor("2.0-SNAPSHOT"), makeServiceItem("1.0", "1.1", "1.2")));
    }

    @Test
    public void testVersionMatchFilter7() {
        VersionMatchFilter versionMatchFilter = new VersionMatchFilter();
        Assert.assertTrue(versionMatchFilter.check(makeAssociationDescriptor("dev-3.24.0"), makeServiceItem("dev-3.23.10, dev-3.24.0")));
    }

    ServiceItem makeServiceItem(String... versions) {
        UUID uuid = UUID.randomUUID();
        ServiceID sid = new ServiceID(uuid.getMostSignificantBits(),
                                      uuid.getLeastSignificantBits());
        List<Entry> attributes = new ArrayList<Entry>();
        attributes.add(new Name("foo"));
        attributes.add(new OperationalStringEntry("bar"));
        if(versions!=null) {
            for(String version : versions)
                attributes.add(new VersionEntry(version));
        }
        return new ServiceItem(sid, new Object(), attributes.toArray(new Entry[attributes.size()]));
    }

    AssociationDescriptor makeAssociationDescriptor(String version) {
        AssociationDescriptor descriptor = new AssociationDescriptor();
        descriptor.setVersion(version);
        return descriptor;
    }
}
