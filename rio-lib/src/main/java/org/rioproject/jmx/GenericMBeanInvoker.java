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
package org.rioproject.jmx;

import org.rioproject.resources.util.ThrowableUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;

/**
 * A generic handler that creates a {@link javax.management.MBeanServerConnection}
 * and connects to a {@link javax.management.MBeanServer} and gets the value of
 * an attribute
 *
 * @author Dennis Reedy
 */
public class GenericMBeanInvoker {
    private MBeanServerConnection mbsc;
    private ObjectName objectName;
    private String attribute;
    /* flag that indicates an unretryable exception has occured */
    private boolean failed = false;
    public static final String GETTER="value";
    private static final Logger logger = LoggerFactory.getLogger("org.rioproject.jmx");

    public void setObjectName(ObjectName objectName) {
        this.objectName = objectName;
    }

    public void setAttribute(String attribute) {
        this.attribute = attribute;
    }

    public void setMBeanServerConnection(MBeanServerConnection mbsc) {
        this.mbsc = mbsc;
    }

    public Object getValue()  {
        if(failed)
            return null;
        Object result = null;
        for(int i=0; i<3; i++) {
            try {
                if(mbsc==null) {
                    logger.warn("Cannot obtain value of MBean ["+objectName+"] " +
                                   "without an MBeanServerConection.");
                    failed = true;
                    break;
                }
                if(objectName==null) {
                    logger.warn("An ObjectName must be set, it is " +
                                   "currently null.");
                    failed = true;
                    break;
                }
                if(attribute==null) {
                    logger.warn("An attribute must be set in order to get " +
                                   "a value to observe from " +
                                   "MBean ["+objectName+"].");
                    failed = true;
                    break;
                }
                result = mbsc.getAttribute(objectName, attribute);
                break;
            } catch (Throwable t) {
                if(ThrowableUtil.isRetryable(t)) {
                    logger.warn("Retry ["+i+"] connection to MBeanServer " +
                                   "for MBean ["+objectName+"], " +
                                   "attribute ["+attribute+"]. " +
                                   "Exception: "+t.getClass().getName()+": "+
                                   t.getMessage());
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        //
                    }
                } else {
                    logger.warn("Unretryable exception to MBeanServer for " +
                                "MBean ["+objectName+"], attribute ["+objectName+"]",
                                t);
                    failed = true;
                    break;
                }
            }
        }
        mbsc = (result==null?null:mbsc);
        return result;
    }
}
