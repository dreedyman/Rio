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

import net.jini.core.event.RemoteEvent;
import net.jini.core.event.UnknownEventException;
import org.rioproject.event.EventNotificationAdapter;

import javax.management.MBeanNotificationInfo;
import javax.management.Notification;
import javax.management.NotificationBroadcasterSupport;
import javax.management.ObjectName;
import java.rmi.RemoteException;

/**
 * Provides the support to transform ProvisionFailureEvent notifications to JMX notifications
 *
 * @author Dennis Reedy
 */
public class ProvisionFailureEventAdapter extends EventNotificationAdapter {
    private static final String EVENT_TYPE = "ProvisionFailureEvent";
    private static final MBeanNotificationInfo NOTIFICATION_INFO =
        new MBeanNotificationInfo(new String[]{EVENT_TYPE}, Notification.class.getName(), "ProvisionFailureEvent");

    /**
     * Create a ProvisionFailureEventAdapter
     *
     * @param objectName The JMX ObjectName
     * @param notificationBroadcasterSupport The MBean that is sending the
     * notification
     */
    public ProvisionFailureEventAdapter(final ObjectName objectName,
                                        final NotificationBroadcasterSupport notificationBroadcasterSupport) {
        super(objectName, notificationBroadcasterSupport);
    }

    /**
     * Transforms the notification of a ProvisionFailureEvent to a JMX
     * notification
     *
     * @see org.rioproject.event.EventNotificationAdapter#notify(net.jini.core.event.RemoteEvent)
     */
    public void notify(final RemoteEvent theEvent) throws UnknownEventException, RemoteException {
        if(theEvent==null)
            throw new IllegalArgumentException("event is null");
        if(!(theEvent instanceof ProvisionFailureEvent)) {
            throw new UnknownEventException("Not a ProvisionFailureEvent ["+theEvent.getClass().getName()+"]");
        }
        ProvisionFailureEvent event = (ProvisionFailureEvent)theEvent;
        StringBuilder builder = new StringBuilder();
        for(String reason : event.getFailureReasons()) {
            if(builder.length()>0)
                builder.append("\n    ");
            builder.append(reason);
        }
        Notification notification = new Notification(EVENT_TYPE,
                                                     objectName,
                                                     event.getSequenceNumber(),
                                                     event.getDate().getTime(),
                                                     builder.toString());
        notificationBroadcasterSupport.sendNotification(notification);
    }


    /**
     * Get the MBeanNotificationInfo for the transformed ProvisionFailureEvent
     *
     * @see org.rioproject.event.EventNotificationAdapter#getNotificationInfo()
     */
    public MBeanNotificationInfo getNotificationInfo() {
        return NOTIFICATION_INFO;
    }
}
