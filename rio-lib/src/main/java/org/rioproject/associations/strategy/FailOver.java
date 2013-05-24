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
package org.rioproject.associations.strategy;

import net.jini.core.entry.Entry;
import net.jini.core.lookup.ServiceItem;
import net.jini.lookup.entry.Host;
import org.rioproject.associations.Association;
import org.rioproject.config.Constants;
import org.rioproject.net.HostUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Returns the first service in the {@link org.rioproject.associations.Association}. If
 * the underlying service is removed (as determined by the {@link
 * org.rioproject.fdh.FaultDetectionHandler}), the next available service is
 * returned.
 *
 * This service selection strategy will always prefer local services over
 * remote services and sorts the service list collection returned from the
 * {@link org.rioproject.associations.Association} accordingly, using the
 * <tt>net.jini.lookup.entry.Host</tt> entry attribute
 *
 * @author Dennis Reedy
 */
public class FailOver<T> extends AbstractServiceSelectionStrategy<T> {
    private String hostAddress;
    private final List<ServiceItem> serviceList = Collections.synchronizedList(new ArrayList<ServiceItem>());
    static final Logger logger = LoggerFactory.getLogger(FailOver.class.getName());

    @SuppressWarnings("unchecked")
    public T getService() {
        T service = null;
        if(!serviceList.isEmpty()) {
            ServiceItem item = serviceList.get(0);
            service = (T)item.service;
        }
        return service;
    }

    @Override
    public void setAssociation(Association<T> association) {
        this.association = association;
        try {
            hostAddress = HostUtil.getHostAddressFromProperty(Constants.RMI_HOST_ADDRESS);
            for(ServiceItem item : association.getServiceItems()) {
                add(item);
            }
        } catch (UnknownHostException e) {
            logger.warn("Unable to obtain host address", e);
        }
    }

    @Override
    public void serviceAdded(T service) {
        ServiceItem item = association.getServiceItem(service);
        if(logger.isTraceEnabled())
            logger.trace("Adding service {}, item: {}", association.getName(),  service);
        if(item!=null) {
            add(item);
        } else {
            logger.warn("Unable to obtain ServiceItem for {}, force refresh all service instances ", service);
            serviceList.clear();
            for(ServiceItem serviceItem : association.getServiceItems())
                add(serviceItem);
        }
    }

    @Override
    public void serviceRemoved(T service) {
        if(service!=null) {
            remove(service);
        } else {
            logger.warn("The service is null, cannot remove from {}", FailOver.class.getName());
        }
    }

    /*
     * Add a service to the list, sorting by returned host name
     */
    private void add(ServiceItem item) {
        if(serviceList.contains(item)) {
            if(logger.isTraceEnabled()) {
                logger.trace("Already have {}, service count now {}", item, serviceList.size());
            }
        }
        int ndx = -1;
        if (hostAddress != null) {
            Host host = getHostEntry(item);
            if (host != null) {
                if (hostAddress.equals(host.hostName)) {
                    for (ServiceItem si : serviceList) {
                        Host h = getHostEntry(si);
                        if (h == null) {
                            ndx = serviceList.indexOf(si);
                            break;
                        } else if (h.hostName == null) {
                            ndx = serviceList.indexOf(si);
                            break;
                        } else if (!hostAddress.equals(h.hostName)) {
                            ndx = serviceList.indexOf(si);
                            break;
                        }
                    }
                }
            }
        }
        if (ndx == -1) {
            serviceList.add(item);
        } else {
            serviceList.add(ndx, item);
        }

        if(logger.isTraceEnabled()) {
            logger.trace("Added item: {}, now have {}", item, serviceList.size());
        }
    }


    /*
     * remove a service
     */
    private void remove(T service) {
        ServiceItem[] items = serviceList.toArray(new ServiceItem[serviceList.size()]);
        ServiceItem item = null;
        for (ServiceItem si : items) {
            if (si.service.equals(service)) {
                item = si;
                break;
            }
        }
        if (item != null) {
            serviceList.remove(item);
        }
    }

    /*
     * Get the net.jini.lookup.entry.Host from the attribute set. Return null
     * if not found.
     */
    private Host getHostEntry(ServiceItem item) {
        Host host = null;
        if(item!=null && item.attributeSets!=null) {
            for (Entry e : item.attributeSets) {
                if (e instanceof Host) {
                    host = (Host) e;
                    break;
                }
            }
        }
        return host;
    }
}
