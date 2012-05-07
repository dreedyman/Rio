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
 * Provides the support to transform ProvisionMonitorEvent notifications to JMX
 * notifications
 *
 * @author Ming Fang
 * @author Dennis Reedy
 */
public class ProvisionMonitorEventAdapter extends EventNotificationAdapter {
    private static final String EVENT_TYPE_SERVICE_ELEMENT_UPDATED =
        "ProvisionMonitorEvent.serviceElementUpdated";
    private static final String EVENT_TYPE_SERVICE_ELEMENT_ADDED =
        "ProvisionMonitorEvent.serviceElementAdded";
    private static final String EVENT_TYPE_SERVICE_ELEMENT_REMOVED =
        "ProvisionMonitorEvent.serviceElementRemoved";
    private static final String EVENT_TYPE_SERVICE_BEAN_INCREMENTED =
        "ProvisionMonitorEvent.serviceBeanIncremented";
    private static final String EVENT_TYPE_SERVICE_BEAN_DECREMENTED =
        "ProvisionMonitorEvent.serviceBeanDecremented";
    private static final String EVENT_TYPE_SERVICE_BEAN_INSTANCE_UPDATED =
        "ProvisionMonitorEvent.serviceBeanInstanceUpdated";
    private static final String EVENT_TYPE_OPSTRING_DEPLOYED =
        "ProvisionMonitorEvent.opstringDeployed";
    private static final String EVENT_TYPE_OPSTRING_UNDEPLOYED =
        "ProvisionMonitorEvent.opstringUndeployed";
    private static final String EVENT_TYPE_OPSTRING_UPDATED =
        "ProvisionMonitorEvent.opstringUpdated";
    private static final String EVENT_TYPE_OPSTRING_MGR_CHANGED =
        "ProvisionMonitorEvent.opstringMgrChanged";
    private static final String EVENT_TYPE_SERVICE_PROVISIONED =
        "ProvisionMonitorEvent.serviceProvisioned";
    private static final String EVENT_TYPE_SERVICE_FAILED =
        "ProvisionMonitorEvent.serviceFailed";
    private static final String EVENT_TYPE_SERVICE_TERMINATED =
        "ProvisionMonitorEvent.serviceTerminated";
    private static final String EVENT_TYPE_REDEPLOY_REQUEST =
        "ProvisionMonitorEvent.redeployRequest";
    private static final MBeanNotificationInfo NOTIFICATION_INFO =
        new MBeanNotificationInfo(
            new String[]{
                EVENT_TYPE_SERVICE_ELEMENT_UPDATED,
                EVENT_TYPE_SERVICE_ELEMENT_ADDED,
                EVENT_TYPE_SERVICE_ELEMENT_REMOVED,
                EVENT_TYPE_SERVICE_BEAN_INCREMENTED,
                EVENT_TYPE_SERVICE_BEAN_DECREMENTED,
                EVENT_TYPE_SERVICE_BEAN_INSTANCE_UPDATED,
                EVENT_TYPE_OPSTRING_DEPLOYED,
                EVENT_TYPE_OPSTRING_UNDEPLOYED,
                EVENT_TYPE_OPSTRING_UPDATED,
                EVENT_TYPE_OPSTRING_MGR_CHANGED,
                EVENT_TYPE_SERVICE_PROVISIONED,
                EVENT_TYPE_SERVICE_FAILED,
                EVENT_TYPE_SERVICE_TERMINATED,
                EVENT_TYPE_REDEPLOY_REQUEST,
            },
            Notification.class.getName(),
            "ProvisionMonitorEvent"
        );

    /**
     * Create a ProvisionFailureEventAdapter
     *
     * @param objectName The JMX ObjectName
     * @param notificationBroadcasterSupport The mbean sending the notification
     */
    public ProvisionMonitorEventAdapter(
        ObjectName objectName,
        NotificationBroadcasterSupport notificationBroadcasterSupport) {
        super(objectName, notificationBroadcasterSupport);
    }


    /**
     * Transforms the notification of a ProvisionMonitorEvent to a JMX
     * notification
     *
     * @see org.rioproject.event.EventNotificationAdapter#notify(net.jini.core.event.RemoteEvent)
     */
    public void notify(RemoteEvent theEvent)
        throws UnknownEventException, RemoteException {
        if(theEvent == null)
            throw new IllegalArgumentException("event is null");
        if(!(theEvent instanceof ProvisionMonitorEvent)) {
            throw new UnknownEventException("Not a ProvisionMonitorEvent "+
                                            "["+
                                            theEvent.getClass().getName()+
                                            "]");
        }
        ProvisionMonitorEvent event = (ProvisionMonitorEvent)theEvent;
        String type = getNotificationType(event);
        Notification notification = new Notification(
            type,
            objectName,
            event.getSequenceNumber(),
            getMessage(event)
            //event.toString()
        );
        notificationBroadcasterSupport.sendNotification(notification);
    }

    /**
     * Get the MBeanNotificationInfo for the transformed ProvisionMonitorEvent
     *
     * @see org.rioproject.event.EventNotificationAdapter#getNotificationInfo()
     */
    public MBeanNotificationInfo getNotificationInfo() {
        return NOTIFICATION_INFO;
    }

    private String getNotificationType(ProvisionMonitorEvent event)
        throws UnknownEventException {
        String type;
        switch(event.getAction()) {
            case OPSTRING_DEPLOYED: {
                type = EVENT_TYPE_OPSTRING_DEPLOYED;
                break;
            }
            case OPSTRING_MGR_CHANGED: {
                type = EVENT_TYPE_OPSTRING_MGR_CHANGED;
                break;
            }
            case OPSTRING_UNDEPLOYED: {
                type = EVENT_TYPE_OPSTRING_UNDEPLOYED;
                break;
            }
            case OPSTRING_UPDATED: {
                type = EVENT_TYPE_OPSTRING_UPDATED;
                break;
            }
            case REDEPLOY_REQUEST: {
                type = EVENT_TYPE_REDEPLOY_REQUEST;
                break;
            }
            case SERVICE_BEAN_DECREMENTED: {
                type = EVENT_TYPE_SERVICE_BEAN_DECREMENTED;
                break;
            }
            case SERVICE_BEAN_INCREMENTED: {
                type = EVENT_TYPE_SERVICE_BEAN_INCREMENTED;
                break;
            }
            case SERVICE_BEAN_INSTANCE_UPDATED: {
                type = EVENT_TYPE_SERVICE_BEAN_INSTANCE_UPDATED;
                break;
            }
            case SERVICE_ELEMENT_ADDED: {
                type = EVENT_TYPE_SERVICE_ELEMENT_ADDED;
                break;
            }
            case SERVICE_ELEMENT_REMOVED: {
                type = EVENT_TYPE_SERVICE_ELEMENT_REMOVED;
                break;
            }
            case SERVICE_ELEMENT_UPDATED: {
                type = EVENT_TYPE_SERVICE_ELEMENT_UPDATED;
                break;
            }
            case SERVICE_FAILED: {
                type = EVENT_TYPE_SERVICE_FAILED;
                break;
            }
            case SERVICE_PROVISIONED: {
                type = EVENT_TYPE_SERVICE_PROVISIONED;
                break;
            }
            case SERVICE_TERMINATED: {
                type = EVENT_TYPE_SERVICE_TERMINATED;
                break;
            }
            default:
                throw new UnknownEventException();
        }
        return type;
    }

    private String getMessage(ProvisionMonitorEvent event) {
        String data = null;
        switch(event.getAction()) {
            case OPSTRING_DEPLOYED: {
                data = "["+event.getOperationalStringName()+"] deployed";
                break;
            }
            case OPSTRING_MGR_CHANGED: {
                data = "Manager for ["+event.getOperationalStringName()+"] "+
                        "changed";
                break;
            }
            case OPSTRING_UNDEPLOYED: {
                data = "["+event.getOperationalStringName()+"] Undeployed";
                break;
            }
            case OPSTRING_UPDATED: {
                data = "["+event.getOperationalStringName()+"] Updated";
                break;
            }
            case REDEPLOY_REQUEST: {
                data = "["+event.getOperationalStringName()+"] Redeployed";
                break;
            }
            case SERVICE_BEAN_DECREMENTED: {
                data = "["+event.getServiceElement().getName()+"] Decremented";
                break;
            }
            case SERVICE_BEAN_INCREMENTED: {
                data = "["+event.getServiceElement().getName()+"] Incremented";
                break;
            }
            case SERVICE_BEAN_INSTANCE_UPDATED: {
                data =
                    "["+event.getServiceElement().getName()+"] instance updated";
                break;
            }
            case SERVICE_ELEMENT_ADDED: {
                data = "["+event.getServiceElement().getName()+"] added to " +
                       "deployment";
                break;
            }
            case SERVICE_ELEMENT_REMOVED: {
                data = "["+event.getServiceElement().getName()+"] Removed " +
                       "from deployment";
                break;
            }
            case SERVICE_ELEMENT_UPDATED: {
                data = "["+event.getServiceElement().getName()+"] Updated";
                break;
            }
            case SERVICE_FAILED: {
                data = "["+event.getServiceElement().getName()+"] Failed";
                break;
            }
            case SERVICE_TERMINATED: {
                data = "["+event.getServiceElement().getName()+"] Terminated";
                break;
            }
            case SERVICE_PROVISIONED: {
                data = "["+event.getServiceElement().getName()+"] Provisioned";
                break;
            }
        }
        return(data);
    }


}
