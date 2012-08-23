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
package org.rioproject.event;

import org.junit.Assert;
import org.junit.Test;

import java.net.MalformedURLException;
import java.util.List;

/**
 * @author Dennis Reedy
 */
public class EventDescriptorFactoryTest {
    @Test
    public void testCreateEventDescriptors() throws Exception {
        List<EventDescriptor> list =
            EventDescriptorFactory.createEventDescriptors (null, "org.rioproject.sla.SLAThresholdEvent");
        Assert.assertNotNull(list);
        Assert.assertEquals(1, list.size());
    }

    @Test
    public void testCreateEventDescriptorsMulti() throws Exception {
        List<EventDescriptor> list =
            EventDescriptorFactory.createEventDescriptors (null,
                                                           "org.rioproject.sla.SLAThresholdEvent",
                                                           "org.rioproject.watch.ThresholdEvent");
        Assert.assertNotNull(list);
        Assert.assertEquals(2, list.size());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateEventDescriptorsNullClassNames() throws Exception {
        EventDescriptorFactory.createEventDescriptors (null);
    }

    @Test(expected = MalformedURLException.class)
    public void testCreateEventDescriptorsBadClassPath() throws Exception {
        EventDescriptorFactory.createEventDescriptors ("foo", "");
    }
}
