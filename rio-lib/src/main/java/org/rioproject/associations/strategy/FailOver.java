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
import org.rioproject.resources.util.ThrowableUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.net.UnknownHostException;

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
    private final List<ServiceItem> serviceList = new ArrayList<ServiceItem>();
    static final Logger logger = Logger.getLogger(FailOver.class.getName());

    @SuppressWarnings("unchecked")
    public T getService() {
        T service = null;
        synchronized(serviceList) {
            if(!serviceList.isEmpty()) {
                ServiceItem item = serviceList.get(0);
                service = (T)item.service;
            }
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
            logger.log(Level.WARNING, "Unable to obtain host address", ThrowableUtil.getRootCause(e));
        }
    }

    @Override
    public void serviceAdded(T service) {
        ServiceItem item = association.getServiceItem(service);
        if(item!=null) {
            add(item);
        } else {
            logger.warning("Unable to obtain ServiceItem for " + service + ", force refresh all service instances");
            synchronized(serviceList) {
                serviceList.clear();
                for(ServiceItem serviceItem : association.getServiceItems())
                    add(serviceItem);
            }
        }
    }

    @Override
    public void serviceRemoved(T service) {
        if(service!=null) {
            remove(service);
        } else {
            logger.warning("The service is null, cannot remove from "+FailOver.class.getName());
        }
    }

    /*
     * Add a service to the list, sorting by returned host address
     */
    private synchronized void add(ServiceItem item) {
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
        if (ndx == -1)
            serviceList.add(item);
        else
            serviceList.add(ndx, item);
    }


    /*
     * remove a service
     */
    private void remove(T service) {
        ServiceItem[] items;
        synchronized (serviceList) {
            items = serviceList.toArray(new ServiceItem[serviceList.size()]);
        }
        ServiceItem item = null;
        for (ServiceItem si : items) {
            if (si.service.equals(service)) {
                item = si;
                break;
            }
        }
        if (item != null) {
            synchronized (serviceList) {
                serviceList.remove(item);
            }
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
