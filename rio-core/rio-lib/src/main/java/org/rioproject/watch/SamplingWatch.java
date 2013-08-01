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
package org.rioproject.watch;

import net.jini.config.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * An implementation of a {@link PeriodicWatch} that samples a bean's method
 */
public class SamplingWatch extends PeriodicWatch {
    private String property;
    private Object bean;
    private Method accessor;
    private final AtomicInteger nullReturnCount = new AtomicInteger();
    private static final Logger logger = LoggerFactory.getLogger(SamplingWatch.class);

    public SamplingWatch(String id) {
        super(id);
    }

    public SamplingWatch(String id, Configuration config) {
        super(id, config);
    }

    public SamplingWatch(WatchDataSource watchDataSource, String id) {
        super(watchDataSource, id);
    }

    public void setProperty(String property) {
        if(this.property!=null && !this.property.equals(property))
            accessor = null;
        this.property = property;
    }

    public String getProperty() {
        return property;
    }

    public void setBean(Object bean) {
        this.bean = bean;
    }

    public void setAccessor(Method accessor) {
        this.accessor = accessor;
    }

    public void checkValue() {
        if(accessor==null) {
            accessor = getAccessor();
        }
        if(accessor==null) {
            return;
        }
        try {
            Object value = accessor.invoke(bean, (Object[])null);
            if(value==null) {
                int count = nullReturnCount.incrementAndGet();
                if(count>3) {
                    logger.warn("SamplingWatch [{}], Invoking [{}.{}] returned null " +
                               "[{}] times. Halting periodic sampling due to null returns",
                                getId(), bean.getClass().getName(), accessor.getName(), count);
                    stop();
                } else {
                    logger.warn("SamplingWatch [{}], Invoking [{}.{}] returned null. Verify the underlying object",
                               getId(), bean.getClass().getName(), accessor.getName());
                }
                return;
            } else {
                if(nullReturnCount.get()>0)
                    nullReturnCount.decrementAndGet();
            }
            Calculable metric;
            if(value instanceof Calculable) {
                metric = (Calculable)value;
            } else {
                Double d = new Double(value.toString());
                metric = new Calculable(getId(), d, System.currentTimeMillis());
            }
            addWatchRecord(metric);
            
        } catch(Throwable t) {
            logger.warn("SamplingWatch [{}], Invoking [{}={}()]",
                        getId(), accessor.getReturnType().getName(), accessor.getName(), t);
        }
    }

    private Method getAccessor() {
        BeanInfo bi = null;
        try {
            bi = Introspector.getBeanInfo(bean.getClass());
        } catch (IntrospectionException e) {
            e.printStackTrace();
        }
        if(bi==null)
            return null;
        Method accessor = null;
        PropertyDescriptor[] pds = bi.getPropertyDescriptors();
        boolean propertyMatch = false;
        for (PropertyDescriptor pd : pds) {
            String propName = pd.getName();
            if (propName.equals(property)) {
                propertyMatch = true;
                Method m = pd.getReadMethod();                
                if (m != null) {
                    accessor = m;
                } else {
                    logger.warn("SamplingWatch [{}], with declared propertyName " +
                                "[{}], matched, no readMethod found on target object [{}]",
                                getId(), property, bean.getClass().getName());
                }
            }
        }

        if(!propertyMatch) {
            logger.warn("SamplingWatch [{}] with property [{}] not found on target object {}",
                        getId(), property, bean.getClass().getName());
        }

        return accessor;
    }
}
