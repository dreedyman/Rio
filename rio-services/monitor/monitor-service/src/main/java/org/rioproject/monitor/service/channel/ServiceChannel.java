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
package org.rioproject.monitor.service.channel;

import org.rioproject.associations.AssociationDescriptor;
import org.rioproject.opstring.ClassBundle;
import org.rioproject.opstring.ServiceElement;
import org.rioproject.impl.servicebean.ServiceElementUtil;

import java.util.*;

/**
 * The ServiceChannel provides a local notification channel for service instances that have
 * been provisioned, failed or gone idle.
 *
 * @author Dennis Reedy
 */
public final class ServiceChannel {
    private final List<Registration> registrations = new ArrayList<Registration>();
    private static final ServiceChannel instance = new ServiceChannel();

    public static ServiceChannel getInstance() {
        return instance;
    }

    public void subscribe(final ServiceChannelListener listener,
                          final AssociationDescriptor associationDescriptor,
                          final ServiceChannelEvent.Type type) {

        subscribe(listener,
                  associationDescriptor.getName(),
                  associationDescriptor.getInterfaceNames(),
                  associationDescriptor.getOperationalStringName(),
                  type);
    }

    public void subscribe(final ServiceChannelListener listener,
                          final ServiceElement serviceElement,
                          final ServiceChannelEvent.Type type) {
        ClassBundle[] exports = serviceElement.getExportBundles();
        String[] interfaceNames = new String[exports.length];
        for(int i=0; i< interfaceNames.length; i++) {
            interfaceNames[i] = exports[i].getClassName();
        }
        subscribe(listener,
                  serviceElement.getName(),
                  interfaceNames,
                  serviceElement.getOperationalStringName(),
                  type);
    }

    private void subscribe(final ServiceChannelListener listener,
                           final String name,
                           final String[] interfaces,
                           final String opStringName,
                           final ServiceChannelEvent.Type type) {
        Registration reg = new Registration(listener, name, interfaces, opStringName, type);
        if(!registrations.contains(reg)) {
            registrations.add(reg);
        }
    }

    public void unsubscribe(final ServiceChannelListener listener) {
        Registration[] regs = getRegistrations();
        for (Registration r : regs) {
            if (r.getServiceChannelListener().equals(listener)) {
                registrations.remove(r);
            }
        }
    }

    public void broadcast(final ServiceChannelEvent event) {
        Object[] arrLocal = getRegistrations();
        for (Object anArrLocal : arrLocal) {
            Registration r = (Registration) anArrLocal;
            if (r.matches(event.getServiceElement(), event.getType())) {
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

    private class Registration {
        final ServiceChannelListener listener;
        final String name;
        final String[] interfaces;
        final String opStringName;
        final ServiceChannelEvent.Type type;

        Registration(final ServiceChannelListener listener,
                     final String name,
                     final String[] interfaces,
                     final String opStringName,
                     final ServiceChannelEvent.Type type) {
            this.listener = listener;
            this.name = name;
            this.interfaces = interfaces;
            this.opStringName = opStringName;
            this.type = type;
        }

        boolean matches(final ServiceElement element, final ServiceChannelEvent.Type type) {
            return(ServiceElementUtil.matchesServiceElement(element, name, interfaces, opStringName) &&
                   this.type.equals(type));
        }

        ServiceChannelListener getServiceChannelListener() {
            return(listener);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if(!(o instanceof Registration)) {
                return (false);
            }

            Registration that = (Registration) o;

            return Arrays.equals(interfaces, that.interfaces) &&
                   listener.equals(that.listener) &&
                   name.equals(that.name) &&
                   opStringName.equals(that.opStringName) &&
                   type == that.type;

        }

        @Override
        public int hashCode() {
            int result = listener.hashCode();
            result = 31 * result + name.hashCode();
            result = 31 * result + Arrays.hashCode(interfaces);
            result = 31 * result + opStringName.hashCode();
            result = 31 * result + type.hashCode();
            return result;
        }
    }
}
