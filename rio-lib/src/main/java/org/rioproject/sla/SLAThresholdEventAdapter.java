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
package org.rioproject.sla;

import net.jini.core.event.RemoteEvent;
import net.jini.core.event.UnknownEventException;
import org.rioproject.event.EventNotificationAdapter;
import org.rioproject.watch.ThresholdType;

import javax.management.MBeanNotificationInfo;
import javax.management.Notification;
import javax.management.NotificationBroadcasterSupport;
import javax.management.ObjectName;
import java.rmi.RemoteException;

/**
 * Provides the support to transform SLAThresholdEvent notifications
 * to JMX notifications
 *
 * @author Dennis Reedy
 */
public class SLAThresholdEventAdapter extends EventNotificationAdapter implements SLAPolicyEventListener {
    public static final String EVENT_TYPE_BREACHED = "sla.SLAThresholdEvent.breached";
    public static final String EVENT_TYPE_CLEARED = "sla.SLAThresholdEvent.cleared";

    protected static final MBeanNotificationInfo NOTIFICATION_INFO =
        new MBeanNotificationInfo(
            new String[]{EVENT_TYPE_BREACHED, EVENT_TYPE_CLEARED},
            Notification.class.getName(),
            "SLAThresholdEvent");

    /**
     * Create a SLAThresholdEventAdapter
     *
     * @param objectName The JMX ObjectName
     * @param notificationBroadcasterSupport The
     * JMX NotificationBroadcasterSupport
     */
    public SLAThresholdEventAdapter(
        ObjectName objectName,
        NotificationBroadcasterSupport notificationBroadcasterSupport) {
        super(objectName, notificationBroadcasterSupport);
    }

    /**
     * Transforms the notification of a SLAThresholdEvent to a JMX
     * notification
     *
     * @see org.rioproject.event.EventNotificationAdapter#notify(net.jini.core.event.RemoteEvent)
     */
    public void notify(RemoteEvent theEvent)
        throws UnknownEventException, RemoteException {
        if(theEvent == null)
            throw new IllegalArgumentException("event is null");
        if(!(theEvent instanceof SLAThresholdEvent)) {
            throw new UnknownEventException("Not a SLAThresholdEvent "+
                                            "["+
                                            theEvent.getClass().getName()+
                                            "]");
        }
        buildAndSend((SLAThresholdEvent)theEvent);
    }

    /**
     * Notification of a SLAPolicyEvent
     *
     * @param event The SLAPolicyEvent
     */
    public void policyAction(SLAPolicyEvent event) {
        if(event == null)
            throw new IllegalArgumentException("event is null");
        SLAThresholdEvent slaThresholdEvent = event.getSLAThresholdEvent();
        if(slaThresholdEvent != null)
            buildAndSend(slaThresholdEvent);
    }

    /**
     * Get the MBeanNotificationInfo for the transformed SLAThresholdEvent
     *
     * @see org.rioproject.event.EventNotificationAdapter#getNotificationInfo()
     */
    public MBeanNotificationInfo getNotificationInfo() {
        return NOTIFICATION_INFO;
    }

    private void buildAndSend(SLAThresholdEvent event) {
        String type=null;
        if(event.getThresholdType() == ThresholdType.BREACHED) {
            type = EVENT_TYPE_BREACHED;
        } else if(event.getThresholdType() == ThresholdType.CLEARED) {
            type = EVENT_TYPE_CLEARED;
        }

        Notification notification = new Notification(type,
                                                     objectName,
                                                     event.getSequenceNumber(),
                                                     event.getDate().getTime(),
                                                     getMessage(event));
        notificationBroadcasterSupport.sendNotification(notification);
    }

    private String getMessage(SLAThresholdEvent event) {
        return(event.getServiceElement().getName()+"."+
                   event.getServiceElement().getOperationalStringName()+" "+
                   "SLA ["+event.getSLA().getIdentifier()+"] "+
                   (event.getThresholdType() == ThresholdType.BREACHED?"BREACHED":
                    "CLEARED")+" " +
                    "low="+event.getSLA().getCurrentLowThreshold()+", " +
                    "high="+event.getSLA().getCurrentHighThreshold());
    }
}
