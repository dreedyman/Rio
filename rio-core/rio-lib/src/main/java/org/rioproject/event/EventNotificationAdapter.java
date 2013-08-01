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
package org.rioproject.event;

import net.jini.core.event.RemoteEvent;
import net.jini.core.event.RemoteEventListener;
import net.jini.core.event.UnknownEventException;

import javax.management.MBeanNotificationInfo;
import javax.management.NotificationBroadcasterSupport;
import javax.management.ObjectName;
import java.rmi.RemoteException;

/**
 * The EventNotificationAdapter defines the essentials for an event client to
 * transform a remote event notification to a JMX notification.
 *
 * @author Dennis Reedy
 */
public abstract class EventNotificationAdapter implements RemoteEventListener {
    protected final ObjectName objectName;
    protected final NotificationBroadcasterSupport notificationBroadcasterSupport;

    /**
     * Creates an EventNotificationAdapter, initializing required properties
     * @param objectName The JMX {@link javax.management.ObjectName}
     * @param notificationBroadcasterSupport The
     * {@link javax.management.NotificationBroadcasterSupport} for sending
     * the notification
     *
     * @throws IllegalArgumentException if any of the constructor arguments are
     * <code>null</code>
     */
    public EventNotificationAdapter(final ObjectName objectName,
                                    final NotificationBroadcasterSupport notificationBroadcasterSupport) {
        if(objectName==null)
            throw new IllegalArgumentException("objectName is null");
        if(notificationBroadcasterSupport == null)
            throw new IllegalArgumentException("notificationBroadcasterSupport is null");
        this.objectName = objectName;
        this.notificationBroadcasterSupport = notificationBroadcasterSupport;
    }

    /**
     * Implements the contract of a
     * {@link net.jini.core.event.RemoteEventListener}, and provides the
     * adapter that concrete classes must implement to transform the remote
     * event to a JMX notification
     *
     * @param theEvent The remote evet notification
     * @throws UnknownEventException If theEvent argument is an unknown or
     * unexpected type
     * @throws IllegalArgumentException If theEvent argument is <code>null</code>
     * @throws RemoteException
     *
     * @see net.jini.core.event.RemoteEventListener#notify(net.jini.core.event.RemoteEvent)
     */
    public abstract void notify(RemoteEvent theEvent) throws UnknownEventException, RemoteException;

    /**
     * Get the MBeanNotificationInfo for the transformed event
     *
     * @return The {@link javax.management.MBeanNotificationInfo} for the JMX
     * notification
     */
    public abstract MBeanNotificationInfo getNotificationInfo();
}
