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
package org.rioproject.bean;

import org.rioproject.jsb.ServiceBeanAdapterMBean;

import javax.management.*;
import java.lang.reflect.Method;
import java.util.*;

/**
 * Creates a {@link javax.management.DynamicMBean} that manages the delegation
 * between the service bean standard MBean and a POJO's standard MBean. This
 * is created when the bean implements a standard MBean interface.
 *
 * @author Dennis Reedy
 */
public class AggregatingMBean implements DynamicMBean,
                                          MBeanRegistration,
                                          NotificationEmitter {
    Object serviceBean;
    Object bean;
    private Set<String> serviceBeanMethodSet = new HashSet<String>();
    StandardMBean beanMBean;
    StandardMBean serviceBeanMBean;
    MBeanInfo mbeanIinfo;

    AggregatingMBean(ServiceBeanAdapterMBean serviceBean,
                     Object bean,
                     Class beanMBeanClass,
                     String desc)
    throws NotCompliantMBeanException {
        this.bean = bean;
        this.serviceBean = serviceBean;
        Method[] methods = ServiceBeanAdapterMBean.class.getMethods();

        for (Method method : methods)
            serviceBeanMethodSet.add(method.getName());

        beanMBean = new StandardMBean(bean, beanMBeanClass);
        serviceBeanMBean = new StandardMBean(serviceBean,
                                             ServiceBeanAdapterMBean.class);
        MBeanInfo mbi1 = beanMBean.getMBeanInfo();
        MBeanInfo mbi2 = serviceBeanMBean.getMBeanInfo();
        MBeanAttributeInfo[] attr1 = mbi1.getAttributes();
        MBeanAttributeInfo[] attr2 = mbi2.getAttributes();
        List<MBeanAttributeInfo> attrs = new ArrayList<MBeanAttributeInfo>();
        attrs.addAll(Arrays.asList(attr1));
        attrs.addAll(Arrays.asList(attr2));
        List<MBeanOperationInfo> ops = new ArrayList<MBeanOperationInfo>();
        MBeanOperationInfo[] op1 = mbi1.getOperations();
        MBeanOperationInfo[] op2 = mbi2.getOperations();
        ops.addAll(Arrays.asList(op1));
        ops.addAll(Arrays.asList(op2));
        List<MBeanNotificationInfo> notifies =
            new ArrayList<MBeanNotificationInfo>();
        MBeanNotificationInfo[] n1 = mbi1.getNotifications();
        MBeanNotificationInfo[] n2 = mbi2.getNotifications();
        notifies.addAll(Arrays.asList(n1));
        notifies.addAll(Arrays.asList(n2));
        mbeanIinfo =
            new MBeanInfo(AggregatingMBean.class.getName(),
                          desc,
                          attrs.toArray(new MBeanAttributeInfo[attrs.size()]),
                          null,
                          ops.toArray(new MBeanOperationInfo[ops.size()]),
                          notifies.toArray(
                              new MBeanNotificationInfo[notifies.size()]));
    }

    public Object getAttribute(String string) throws
                                              AttributeNotFoundException,
                                              MBeanException,
                                              ReflectionException {
        Object o;
        try {
            o = beanMBean.getAttribute(string);
        } catch (AttributeNotFoundException e) {
            o = serviceBeanMBean.getAttribute(string);
        }
        if(o == null)
            o = serviceBeanMBean.getAttribute(string);
        return(o);
    }

    public void setAttribute(Attribute attribute) throws
                                                  AttributeNotFoundException,
                                                  InvalidAttributeValueException,
                                                  MBeanException,
                                                  ReflectionException {
        try {
            beanMBean.setAttribute(attribute);
        } catch (AttributeNotFoundException e) {
            serviceBeanMBean.setAttribute(attribute);
        }
    }

    public AttributeList getAttributes(String[] strings) {
        AttributeList list = new AttributeList();
        list.addAll(beanMBean.getAttributes(strings));
        list.addAll(serviceBeanMBean.getAttributes(strings));
        return(list);
    }

    public AttributeList setAttributes(AttributeList attributeList) {
        Attribute[] attrs =
            (Attribute[])attributeList.toArray(
                new Attribute[attributeList.size()]);
        for (Attribute attr : attrs) {
            try {
                setAttribute(attr);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return attributeList;
    }

    public Object invoke(String actionName,
                         Object[] params,
                         String[] signature) throws
                                           MBeanException,
                                           ReflectionException {
        Object result;
        if(serviceBeanMethodSet.contains(actionName)) {            
            result = serviceBeanMBean.invoke(actionName, params, signature);
        } else {
            result = beanMBean.invoke(actionName, params, signature);
        }
        return(result);
    }

    public MBeanInfo getMBeanInfo() {
        return mbeanIinfo;
    }

    public ObjectName preRegister(MBeanServer mBeanServer,
                                  ObjectName objectName) throws Exception {
        if(bean instanceof MBeanRegistration)
            ((MBeanRegistration)bean).preRegister(mBeanServer, objectName);
        
        return(((MBeanRegistration)serviceBean).preRegister(mBeanServer,
                                                            objectName));
    }

    public void postRegister(Boolean aBoolean) {
        ((MBeanRegistration)serviceBean).postRegister(aBoolean);
        if(bean instanceof MBeanRegistration)
            ((MBeanRegistration)bean).postRegister(aBoolean);
    }

    public void preDeregister() throws Exception {
        ((MBeanRegistration)serviceBean).preDeregister();
        if(bean instanceof MBeanRegistration)
            ((MBeanRegistration)bean).preDeregister();
    }

    public void postDeregister() {
        ((MBeanRegistration)serviceBean).postDeregister();
        if(bean instanceof MBeanRegistration)
            ((MBeanRegistration)bean).postDeregister();
    }

    public void removeNotificationListener(NotificationListener listener,
                                           NotificationFilter filter,
                                           Object object) throws
                                                          ListenerNotFoundException {
        ((NotificationEmitter)serviceBean).removeNotificationListener(listener,
                                                                      filter,
                                                                      object);
        if(bean instanceof NotificationEmitter)
            ((NotificationEmitter)bean).removeNotificationListener(listener,
                                                                   filter,
                                                                   object);

    }


    public void addNotificationListener(NotificationListener listener,
                                        NotificationFilter filter,
                                        Object object) throws
                                                       IllegalArgumentException {
        ((NotificationEmitter)serviceBean).addNotificationListener(listener,
                                                                   filter,
                                                                   object);
        if(bean instanceof NotificationEmitter)
            ((NotificationEmitter)bean).addNotificationListener(listener,
                                                                filter,
                                                                object);
    }

    public void removeNotificationListener(NotificationListener listener)
    throws ListenerNotFoundException {
        ((NotificationEmitter)serviceBean).removeNotificationListener(listener);
        if(bean instanceof NotificationEmitter)
            ((NotificationEmitter)bean).removeNotificationListener(listener);

    }

    public MBeanNotificationInfo[] getNotificationInfo() {
        List<MBeanNotificationInfo> list = new ArrayList<MBeanNotificationInfo>();
        MBeanNotificationInfo[] m =
            ((NotificationEmitter)serviceBean).getNotificationInfo();
        list.addAll(Arrays.asList(m));
        if(bean instanceof NotificationEmitter) {
            m = ((NotificationEmitter)bean).getNotificationInfo();
            list.addAll(Arrays.asList(m));
        }
        return(list.toArray(new MBeanNotificationInfo[list.size()]));
    }
}
