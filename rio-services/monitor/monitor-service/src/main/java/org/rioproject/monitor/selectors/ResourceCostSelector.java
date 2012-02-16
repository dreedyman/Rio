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
import org.rioproject.monitor.InstantiatorResource;
import org.rioproject.resources.servicecore.ServiceResource;
import org.rioproject.system.ResourceCapability;

import java.util.Comparator;
import java.util.TreeSet;

/**
 * This class provides an implementation of a ServiceResourceSelector which
 * manages a <code>TreeSet</code> of <code>ServiceResource</code> objects
 * which reflect the resources being leased. This class organizes list items in
 * a sorted collection. The natural order of the collection will be used to
 * obtain a <code>ServiceResource</code> element if it can accept the
 * resources capabilities.
 * <p>
 * This class must be registered with the <code>LandlordLessor</code>, and
 * will be notified as resources are lease, updated or removed <br>
 * 
 * @see org.rioproject.resources.servicecore.LandlordLessor
 * @see org.rioproject.resources.servicecore.ResourceLessor
 * @see org.rioproject.system.ComputeResource
 * @see org.rioproject.system.ResourceCapability
 *
 * @author Dennis Reedy
 */
public class ResourceCostSelector extends ServiceResourceSelector {
    /**
     * Construct a ResourceCostSelector
     */
    @SuppressWarnings("unchecked")
    public ResourceCostSelector() {
        collection = new TreeSet(new CostComparator());
    }

    /**
     * @see ServiceResourceSelector#serviceResourceSelected
     */
    public void serviceResourceSelected(ServiceResource svcResource) {
        /* Empty implementation, nothing required */
    }

    /**
     * Override parent's update method to provide proper <code>TreeSet</code>
     * behavior
     */
    @SuppressWarnings("unchecked")
    protected void update(LeasedResource resource) {
        TreeSet set = (TreeSet)collection;
        synchronized(collectionLock) {
            set.remove(resource);
            set.add(resource);
        }
    }
    /**
     * The <code>CostComparator</code> is a <code>Comparator</code> which
     * sorts the <code>Collection</code> based on the <code>Comparable</code>
     * implementation of the <code>ResourceCapability</code>
     */
    public static class CostComparator implements Comparator {
        public int compare(Object o1, Object o2) {
            if(!(o1 instanceof ServiceResource))
                throw new ClassCastException();
            if(!(o2 instanceof ServiceResource))
                throw new ClassCastException();
            ResourceCapability rc1 =
                ((InstantiatorResource)((ServiceResource)o1).
                                            getResource()).getResourceCapability();
            ResourceCapability rc2 = ((InstantiatorResource)((ServiceResource)o2).
                                            getResource()).getResourceCapability();
            return (rc1.compareTo(rc2));
        }
    }
}
