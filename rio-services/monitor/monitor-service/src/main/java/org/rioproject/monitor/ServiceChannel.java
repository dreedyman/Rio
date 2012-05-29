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
package org.rioproject.monitor;

import org.rioproject.opstring.ServiceElement;
import org.rioproject.jsb.ServiceElementUtil;

import java.util.*;

/**
 * The ServiceChannel provides a local channel for service instances that have
 * been provisioned or failed
 *
 * @author Dennis Reedy
 */
public class ServiceChannel {
    private final List<Registration> registrations = new ArrayList<Registration>();
    private static final ServiceChannel instance = new ServiceChannel();

    public static ServiceChannel getInstance() {
        return(instance);
    }

    public void subscribe(ServiceChannelListener listener, String name, String[] interfaces, String opStringName) {
        Registration reg = new Registration(listener, name, interfaces, opStringName);
        if(!registrations.contains(reg)) {
            registrations.add(reg);
        }
    }

    public void unsubscribe(ServiceChannelListener listener) {
        Registration[] regs = getRegistrations();
        for (Registration r : regs) {
            if (r.getServiceChannelListener().equals(listener)) {
                registrations.remove(r);
            }
        }
    }

    public void broadcast(ServiceChannelEvent event) {
        Object[] arrLocal = getRegistrations();
        for (Object anArrLocal : arrLocal) {
            Registration r = (Registration) anArrLocal;
            if (r.matches(event.getServiceElement())) {
                r.getServiceChannelListener().notify(event);
            }
        }
    }

    private Registration[] getRegistrations() {
        Registration[] regs;
        synchronized(this) {
            regs = registrations.toArray(new Registration[registrations.size()]);
        }
        return(regs);
    }

    /**
     * Interface for clients
     */
    public interface ServiceChannelListener {
        void notify(ServiceChannelEvent event);
    }

    /**
     * Event sent to listeners
     */
    public static class ServiceChannelEvent extends EventObject {
        public static final int PROVISIONED = 1;
        public static final int FAILED = 2;
        private ServiceElement element;
        private int type;

        public ServiceChannelEvent(Object source, ServiceElement element, int type) {
            super(source);
            this.element = element;
            this.type = type;
        }

        public ServiceElement getServiceElement() {
            return(element);
        }

        public int getType() {
            return(type);
        }
    }

    private class Registration {
        final ServiceChannelListener listener;
        final String name;
        final String[] interfaces;
        final String opStringName;

        Registration(final ServiceChannelListener listener,
                     final String name,
                     final String[] interfaces,
                     final String opStringName) {
            this.listener = listener;
            this.name = name;
            this.interfaces = interfaces;
            this.opStringName = opStringName;
        }

        boolean matches(final ServiceElement element) {
            return(ServiceElementUtil.matchesServiceElement(element, name, interfaces, opStringName));
        }

        ServiceChannelListener getServiceChannelListener() {
            return(listener);
        }

        public boolean equals(final Object obj) {
            if(this == obj)
                return (true);
            if(!(obj instanceof Registration)) {
                return (false);
            }
            Registration that = (Registration)obj;
            return(this.name.equals(that.name) &&
                   Arrays.equals(this.interfaces, that.interfaces) &&
                   this.opStringName.equals(that.opStringName) &&
                   this.listener==that.listener);
        }

        public int hashCode() {
            return(name.hashCode()+Arrays.hashCode(interfaces)+opStringName.hashCode()+listener.hashCode());
        }
    }
}
