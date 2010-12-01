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
package org.rioproject.system.capability.platform;

import java.lang.reflect.Method;
import java.util.Observable;
import java.util.Observer;

/**
 * The <code>StorageCapability</code> object provides a definition of storage
 * support
 *
 * @author Dennis Reedy
 */
public class StorageCapability extends ByteOrientedDevice implements Observer {
    static final long serialVersionUID = 1L;
    static final String DEFAULT_DESCRIPTION = "Storage Capability";
    /** Storage media type */
    public final static String TYPE = "StorageType";

    /** 
     * Create a StorageCapability 
     */
    public StorageCapability() {
        this(DEFAULT_DESCRIPTION);
    }

    /** 
     * Create a StorageCapability with a description
     *
     * @param description The description
     */
    public StorageCapability(String description) {
        this.description = description;
        define(NAME, "Disk");
    }

    /**
     * Notification from the DiskSpace MeasurableCapability
     * 
     * @param o The Observable object
     * @param arg The argument, a
     * {@link org.rioproject.system.measurable.disk.CalculableDiskSpace} instance
     */
    public void update(Observable o, Object arg) {
        try {
            Method getAvailable = arg.getClass().getMethod("getAvailable",
                                                           (Class[])null);
            Double dAvail = (Double)getAvailable.invoke(arg, (Object[])null);
            Method getCapacity = arg.getClass().getMethod("getCapacity",
                                                          (Class[])null);
            Double dCap = (Double)getCapacity.invoke(arg, (Object[])null);
            capabilities.put(CAPACITY, dCap);
            capabilities.put(AVAILABLE, dAvail);
        } catch(Throwable t) {
            t.printStackTrace();
        }
    }

    /**
     * Determine if the requested available diskspace can be met by the
     * StorageCapability
     * 
     * @param requestedSize The request amount of disk space
     * @return Return true is the available amount of disk space is less then
     * the request amount
     */
    public boolean supports(int requestedSize) {
        double available = 0;
        Double dAvail = (Double)getValue(AVAILABLE);
        if(dAvail!=null)
            available = dAvail;
        return(requestedSize<available);
    }
}
