/*
 * Copyright 2008 the original author or authors.
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * LeastActiveSelector is used to select a target Cybernode for provisioning.
 * The strategy used here is to sort the list of all available Cybernodes and
 * use the one with the least number of active services.
 *
 * @author Dennis Reedy
 */
public class LeastActiveSelector extends ServiceResourceSelector {
    private final List<Bucket> resourceList = new LinkedList<Bucket>();
    static Logger logger = LoggerFactory.getLogger(LeastActiveSelector.class);
    private LeastActiveComparator comparator = new LeastActiveComparator();

    public LeastActiveSelector() {
        collection = new ArrayList<LeasedResource>();
        logger.info("Created LeastActiveSelector");
    }

    /**
     * Called when resource is selected.
     *
     * @param resource The selected ServiceResource
     */
    public void serviceResourceSelected(ServiceResource resource) {
        InstantiatorResource ir = (InstantiatorResource)resource.getResource();
        synchronized(resourceList) {
            for (Bucket b : resourceList) {
                if(b.getLabel().equals(ir.getHostAddress())) {
                    List<ServiceResource> list = b.getResources();
                    for(ServiceResource s : list) {
                        InstantiatorResource i = (InstantiatorResource)s.getResource();
                        if(i.getInstantiatorUuid().equals(ir.getInstantiatorUuid())) {
                            list.set(list.indexOf(s), s);
                            break;
                        }
                    }
                    break;
                }
            }
        }
    }

    @Override
    protected void add(LeasedResource resource) {
        ServiceResource sr = (ServiceResource)resource;
        InstantiatorResource ir = (InstantiatorResource)sr.getResource();
        synchronized(resourceList) {
            Bucket bucket = null;
            for(Bucket b : resourceList) {
                if(b.getLabel().equals(ir.getHostAddress())) {
                    bucket = b;
                    break;
                }
            }
            if(bucket==null) {
                bucket = new Bucket(ir.getHostAddress());
                resourceList.add(bucket);
            }

            bucket.add(sr);
            resourceList.set(resourceList.indexOf(bucket), bucket);
        }
        super.add(resource);
    }

    @Override
    protected void remove(LeasedResource resource) {
        ServiceResource sr = (ServiceResource)resource;
        InstantiatorResource ir = (InstantiatorResource)sr.getResource();
        synchronized(resourceList) {
            for(Bucket b : resourceList) {
                if(b.getLabel().equals(ir.getHostAddress())) {
                    b.remove(sr);
                    break;
                }
            }
        }
        super.remove(resource); 
    }

    @Override
    public ServiceResource[] getServiceResources() {
        Bucket[] buckets;
        synchronized(resourceList) {
            buckets = resourceList.toArray(new Bucket[resourceList.size()]);
        }
        List<ServiceResource> s = new ArrayList<ServiceResource>();
        int numIterations = getIterations();
        for(int i=0; i<numIterations; i++) {
            for(Bucket b :  buckets) {
                List<ServiceResource> list = b.getResources();
                if(list.size()>i) {
                   s.add(list.get(i));
                }
            }
        }
        Collections.sort(s, comparator);
        ServiceResource[] resources = s.toArray(new ServiceResource[s.size()]);
        if(logger.isTraceEnabled()) {
            StringBuilder b = new StringBuilder();
            if(resources.length>0) {
                int i=0;
                for(ServiceResource sr : resources) {
                    InstantiatorResource ir = (InstantiatorResource)sr.getResource();
                    if(i>0) {
                        b.append(", ");
                    }
                    int count = ((InstantiatorResource)sr.getResource()).getServiceCount();
                    b.append("(")
                        .append(ir.getName())
                        .append(" at ")
                        .append(ir.getHostAddress())
                        .append(", service count:")
                        .append(count)
                        .append(")");
                    i++;
                }
            } else {
                b.append("[No registered Cybernodes]");
            }
            logger.trace(b.toString());
        }
        return resources;
    }

    /*
     * Get iteration count
     */
    private int getIterations() {
        int count = 0;
        for (Bucket b : resourceList) {
            count += b.getResources().size();
        }

        int numBuckets;
        synchronized(resourceList) {
            numBuckets = resourceList.size();
        }

        if(numBuckets > 1 &&  count % 2 != 0)
            count++;

        return (numBuckets==0?0:count/numBuckets);
    }

    /**
     * Comparator that sorts by the least active first.
     * The least active is defined as the actual active plus the planned count
     */
    private class LeastActiveComparator implements Comparator<ServiceResource> {

        public int compare(ServiceResource sr1,
                           ServiceResource sr2) {
            if (sr1 == sr2) {
                return 0;
            }
            int count1 = 0;
            int count2 = 0;
            try {
                count1 = ((InstantiatorResource)sr1.getResource()).getServiceCount();
                count2 = ((InstantiatorResource)sr2.getResource()).getServiceCount();
            } catch (Throwable throwable) {
                logger.warn(throwable.toString(), throwable);
            }
            return(count1 - count2);
        }
    }

    /**
     * A bucket for holding ServiceResources from the same host
     */
    class Bucket {
        final List<ServiceResource> resources = new ArrayList<ServiceResource>();
        final String label;

        Bucket(String label) {
            this.label = label;
        }

        String getLabel() {
            return label;
        }

        void add(ServiceResource s) {
            resources.add(s);
        }

        void update(ServiceResource s) {
            int ndx = resources.indexOf(s);
            if(ndx>=0)
                resources.set(ndx, s);
        }

        void remove(ServiceResource s) {
            resources.remove(s);
        }

        List<ServiceResource> getResources() {
            return resources;
        }
    }
}
