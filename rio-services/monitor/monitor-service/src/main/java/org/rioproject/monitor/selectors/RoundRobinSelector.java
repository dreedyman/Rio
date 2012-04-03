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
package org.rioproject.monitor.selectors;

import com.sun.jini.landlord.LeasedResource;
import org.rioproject.resources.servicecore.ServiceResource;

import java.util.LinkedList;

/**
 * This class provides an implementation of a
 * <code>ServiceResourceSelector</code> which manages a
 * <code>LinkedList</code> of <code>ServiceResource</code> objects which
 * reflect the resources being leased. This class organizes list items in a
 * round robin fashion, putting a used elements last in the list one they are
 * selected. This class must be registered with the <code>LandlordLessor</code>,
 * and will be notified as resources are lease, updated or removed
 * 
 * @see org.rioproject.resources.servicecore.LandlordLessor
 * @see org.rioproject.resources.servicecore.ResourceLessor
 *
 * @author Dennis Reedy
 */
public class RoundRobinSelector extends ServiceResourceSelector {
    
    /**
     * Construct a RoundRobinSelector
     */
    public RoundRobinSelector() {
        collection = new LinkedList<LeasedResource>();
    }

    /**
     * @see ServiceResourceSelector#serviceResourceSelected
     */
    public void serviceResourceSelected(ServiceResource svcResource) {
        LinkedList<LeasedResource> list = (LinkedList<LeasedResource>)collection;
        synchronized(collectionLock) {
            list.remove(svcResource);
            list.addLast(svcResource);
        }
    }

    /**
     * Override parent's update method to provide proper LinkedList behavior
     */
    @Override
    protected void update(LeasedResource resource) {
        LinkedList<LeasedResource> list = (LinkedList<LeasedResource>)collection;
        synchronized(collectionLock) {
            int index = list.indexOf(resource);
            if(index != -1)
                list.set(index, resource);
        }
    }
}
