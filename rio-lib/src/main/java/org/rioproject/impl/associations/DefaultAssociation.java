/*
 * Copyright 2008 the original author or authors.
 * Copyright 2005 Sun Microsystems, Inc.
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
package org.rioproject.impl.associations;

import net.jini.core.lookup.ServiceItem;
import org.rioproject.associations.Association;
import org.rioproject.associations.AssociationDescriptor;
import org.rioproject.associations.AssociationServiceListener;
import org.rioproject.associations.AssociationType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * An implementation of an {@link org.rioproject.associations.Association}.
 *
 * @author Dennis Reedy
 */
public class DefaultAssociation<T> implements Association<T> {

    /**
     * The Association state
     */
    private State state = State.PENDING;
    /**
     * The descriptor for this Association
     */
    private final AssociationDescriptor descriptor;
    /**
     * LinkedList of associated service instances
     */
    private final List<ServiceItem> serviceList = new LinkedList<>();
    /**
     * Index to associated service list
     */
    private final AtomicInteger index = new AtomicInteger(0);
    /**
     * Collection of AssociationServiceListeners
     */
    private final List<AssociationServiceListener<T>> aListeners = new ArrayList<>();
    /**
     * Executor for futures
     */
    private final ExecutorService futuresExecutor = Executors.newCachedThreadPool();

    private static final Logger logger = LoggerFactory.getLogger(DefaultAssociation.class);

    /**
     * Create an Association
     * 
     * @param descriptor The {@link AssociationDescriptor} for
     * this association
     */
    public DefaultAssociation(AssociationDescriptor descriptor) {
        if(descriptor == null)
            throw new IllegalArgumentException("descriptor is null");
        this.descriptor = descriptor;
    }

    /**
     * Get the AssociationType
     * 
     * @return The AssociationType
     */
    public AssociationType getAssociationType() {
        return (descriptor.getAssociationType());
    }

    /**
     * Set the Association state
     * 
     * @param state The Association state
     */
    public void setState(State state) {
        synchronized(this) {
            this.state = state;
        }
    }

    /**
     * Get the Association state
     * 
     * @return The Association state
     */
    public State getState() {
        State currentState;
        synchronized(this) {
            currentState = state;
        }
        return (currentState);
    }

    /**
     * Get the associated service's name
     * 
     * @return The associated service's name
     */
    public String getName() {
        return (descriptor.getName());
    }

    /**
     * Get the associated service's OperationalString name
     * 
     * @return The associated service's OperationalString name
     */
    public String getOperationalStringName() {
        return (descriptor.getOperationalStringName());
    }

    /**
     * Get the AssociationDescriptor
     * 
     * @return The {@link AssociationDescriptor} used to
     * create this Association
     */
    public AssociationDescriptor getAssociationDescriptor() {
        return(descriptor);
    }

    /**
     * Get the number of associated services
     *
     * @return The number of associated services
     */
    public int getServiceCount() {
        int count;
        synchronized(serviceList) {
            count = serviceList.size();
        }
        if(logger.isTraceEnabled())
            logger.trace("returning service count of: {}", count);
        return count;
    }
    
    /**
     * Get the first service Object that can be used to communicate to the
     * associated service.
     * 
     * @return The first service  (proxy) Object in the collection of associated 
     * services. If there are no services a null will be returned.
     */
    @SuppressWarnings("unchecked")
    public T getService() {
        T service = null;
        ServiceItem item = getServiceItem();
        if(item!=null)
            service = (T) item.service;
        return (service);
    }


    /**
     * Get a future representing pending service association.
     *
     * @return a Future representing pending service association. A new Future
     * is created each time.
     *
     * @see org.rioproject.associations.AssociationProxy
     * @see org.rioproject.associations.ServiceSelectionStrategy
     */
    public Future<T> getServiceFuture() {
        AssociationFutureTask<T> task = new AssociationFutureTask<>();
        AssociationInjector<T> ai = new AssociationInjector<>(task);
        ai.setTargetPropertyName("proxy");
        Collection<T> services = getServices();
        if(services.isEmpty()) {
            registerAssociationServiceListener(new AssociationFutureListener<>(ai,this));
        } else {
            for(T service : services)
                ai.discovered(this, service);
        }
        return futuresExecutor.submit(task);
    }

    /**
     * Get all Objects that can be used to communicate to all known associated
     * services.
     * 
     * @return Array of service Object instances that can be used to
     * communicate to all known associated service instances. A new collection
     * is allocated each time. If there are no services, an empty collection
     * will be returned
     */
    @SuppressWarnings("unchecked")
    public Collection<T> getServices() {
        Collection<T> c = new ArrayList<>();
        ServiceItem[] items = getServiceItems();
        for(ServiceItem item : items) {
            c.add((T)item.service);
        }
        return c;
    }

    /**
     * Satisfies the contract of an {@link java.lang.Iterable}, allowing the
     * associated services to be the target of a "foreach" statement
     *
     * @return An {@link java.lang.Iterable} that can be used to iterate over
     * the collection of associated services. A new {@code Iterable} is created
     * each time. 
     */
    @Override
    public Iterator<T> iterator() {
        return getServices().iterator();
    }

    /**
     * Get the ServiceItem for the service thats first in the List of services.
     * 
     * @return The ServiceItem for an associated service. If there are no
     * services, a null will be returned
     */
    public ServiceItem getServiceItem() {
        ServiceItem item = null;
        synchronized(serviceList) {
            if(!serviceList.isEmpty())
                item = serviceList.get(0);
        }
        return (item);
    }
    
    /**
     * Get the ServiceItem for the associated service. The collection of
     * associated services will be searched and if the service proxy is equal
     * to a known associated service proxy, the
     * {@link net.jini.core.lookup.ServiceID} for that service will be returned
     *
     * @param service The proxy of an associated service
     *
     * @return The ServiceItem for an associated service. If the service is
     * unknown, a null will be returned
     */
    public ServiceItem getServiceItem(T service) {
        ServiceItem item = null;
        ServiceItem[] items = getServiceItems();
        for (ServiceItem item1 : items) {
            if (item1.service.equals(service)) {
                item = item1;
                break;
            }
        }
        return (item);
    }

    /**
     * Get ServiceItem instances for all known associated service instances.
     * 
     * @return Array of ServiceItem instances for all known associated
     * service instances. A new array is allocated each time. If there are no
     * services, an empty array will be returned
     */
    public ServiceItem[] getServiceItems() {
        ServiceItem[] items;
        synchronized(serviceList) {
            items = serviceList.toArray(new ServiceItem[0]);
        }
        return (items);
    }

    /**
     * Get the next ServiceItem in the collection of associated services.   
     *
     * @return The next ServiceItem in the collection of associated services.
     * If there are services, a null will be returned. If the current
     * ServiceItem is the last in the collection, the first ServiceItem in the
     * collection will be returned
     */
    public ServiceItem getNextServiceItem() {
        ServiceItem item;
        synchronized(serviceList) {
            if(serviceList.isEmpty()) {
                item = null;
            } else {
                if(index.get()>=serviceList.size()) {
                    index.set(0);
                }
                item = serviceList.get(index.getAndIncrement());
            }
        }
        return item;
    }

    /**
     * Add a service to the Association
     * 
     * @param item The ServiceItem for the service to add
     * @return True if added, false if the ServiceItem already exists
     */
    @SuppressWarnings("unchecked")
    public boolean addServiceItem(ServiceItem item) {
        if(item == null)
            throw new IllegalArgumentException("item is null");
        ServiceItem[] items = getServiceItems();
        for (ServiceItem item1 : items) {
            if (item1.service.equals(item.service)) {
                return false;
            }
        }
        synchronized(serviceList) {
            serviceList.add(item);
        }
        if(logger.isDebugEnabled())
            logger.debug("Service count now: {}", serviceList.size());
        notifyServiceAdd((T)item.service);
        return true;
    }

    /**
     * Remove a service from the Association
     * 
     * @param service The proxy for the Service to remove
     * @return The ServiceItem of the removed service
     */
    public ServiceItem removeService(T service) {
        if(service == null)
            throw new IllegalArgumentException("service is null");
        ServiceItem item = null;
        ServiceItem[] items = getServiceItems();
        for (ServiceItem item1 : items) {
            if (item1.service.equals(service)) {
                synchronized (serviceList) {
                    if(serviceList.remove(item1))
                        item = item1;
                }
                break;
            }
        }
        if(item!=null)
            notifyServiceRemoved(service);
        return item;
    }

    /**
     * Register an {@link AssociationServiceListener} for notification.
     *
     * @param al The AssociationServiceListener. If the AssociationServiceListener
     * is not null and not already registered it will be added to the collection of
     * AssociationServiceListeners
     */
    public void registerAssociationServiceListener(AssociationServiceListener<T> al) {
        if(al!=null) {
            synchronized(aListeners) {
                if(!aListeners.contains(al)) {
                    aListeners.add(al);
                }
            }
        }
    }

    /**
     * Remove a {@link AssociationServiceListener} for notification.
     *
     * @param al The AssociationServiceListener. If the AssociationServiceListener
     * is not null and registered, it will be removed from the collection of
     * AssociationServiceListeners
     */
    public void removeAssociationServiceListener(AssociationServiceListener<T> al) {
        if(al!=null) {
            synchronized(aListeners) {
                aListeners.remove(al);
            }
        }
    }
    public void terminate() {
        futuresExecutor.shutdownNow();
    }

    private void notifyServiceAdd(T service) {
        List<AssociationServiceListener<T>> listeners;
        synchronized(aListeners) {
            listeners = new ArrayList<>(aListeners);
        }
        for(AssociationServiceListener<T> a : listeners) {
            a.serviceAdded(service);
        }
    }

    private void notifyServiceRemoved(T service) {
        List<AssociationServiceListener<T>> listeners;
        synchronized(aListeners) {
            listeners = new ArrayList<>(aListeners);
        }
        for(AssociationServiceListener<T> a : listeners) {
            a.serviceRemoved(service);
        }
    }

    static class AssociationFutureListener<T> implements AssociationServiceListener<T> {
        AssociationInjector<T> ai;
        Association<T> a;

        AssociationFutureListener(AssociationInjector<T> ai, Association<T> a) {
            this.ai = ai;
            this.a = a;
        }

        public void serviceAdded(T service) {
            ai.discovered(a, service);
        }

        /* Left empty, no-op*/
        public void serviceRemoved(T service) {
        }
    }

    static class AssociationFutureTask<T> implements Callable<T> {
        T proxy;
        private final CountDownLatch counter = new CountDownLatch(1);

        public void setProxy(T proxy) {
            this.proxy = proxy;
            counter.countDown();
        }

        public T call() throws Exception {
            counter.await();
            return proxy;
        }
    }

}
