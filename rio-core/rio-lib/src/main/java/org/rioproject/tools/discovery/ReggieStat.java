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
package org.rioproject.tools.discovery;

import net.jini.core.discovery.LookupLocator;
import net.jini.core.lookup.ServiceID;
import net.jini.core.lookup.ServiceRegistrar;

import java.rmi.RemoteException;

/**
 * Class to hold basic information for a discovered/discarded reggie
 *
 * @author Dennis Reedy
 */
public class ReggieStat {
    long eventTime;
    long baseTime;
    String[] groups;
    String machine;
    int port;
    int type;
    ServiceID serviceID;
    public final static int DISCOVERED = 0;
    public final static int DISCARDED = 1;

    /**
     * Create a RegieStat object
     *
     * @param type Either <code>ReggieStat.DISCOVERED</code> or
     * <code>ReggieStat.DISCARDED</code>
     * @param t The time the event occurred
     * @param reggie The ServiceRegistrar instance being recorded
     * @throws RemoteException If there are comunication exceptions obtaining
     * information from the ServiceRegistrar instance
     */
    ReggieStat(int type, long t, ServiceRegistrar reggie)
        throws RemoteException {
        if(type < DISCOVERED || type > DISCARDED)
            throw new IllegalArgumentException("bad type");
        if(reggie == null)
            throw new IllegalArgumentException("reggie is null");
        this.type = type;
        eventTime = t;
        groups = reggie.getGroups();
        LookupLocator locator = reggie.getLocator();
        machine = locator.getHost();
        port = locator.getPort();
        serviceID = reggie.getServiceID();
    }

    /**
     * Convenience method to obtain the time that the ServiceRegistar was
     * discarded, provided a new "base time" to establish (re-)discovery time
     *
     * @return the base time
     */
    public long getBaseTime() {
        return (baseTime);
    }

    /**
     * Convenience method to obtain the host the ServiceRegistrar is on
     *
     * @return the host the ServiceRegistrar is on
     */
    public String getMachine() {
        return (machine);
    }

    /**
     * Convenience method to obtain the port the ServiceRegistrar is
     * listening on
     *
     * @return The port the ServiceRegistrar is listening on
     */
    public int getPort() {
        return (port);
    }

    /**
     * Convenience method to obtain pre-fetced group names
     *
     * @return group names
     */
    public String[] getGroups() {
        return (groups);
    }

    /**
     * @return Get the time the event occurred
     */
    public long getEventTime() {
        return (eventTime);
    }

    boolean groupsMatch(ReggieStat rStat) {
        if(rStat.groups == null && groups == null)
            return (true);
        if(rStat.groups != null && groups == null)
            return (false);
        if(rStat.groups == null && groups != null)
            return (false);
        if(groups!=null && (rStat.groups.length != groups.length))
            return (false);
        if(groups!=null) {
            for (String group : rStat.groups) {
                boolean found = false;
                for (String group1 : groups) {
                    if (group1.equals(group)) {
                        found = true;
                        break;
                    }
                }
                if (!found)
                    return (false);
            }
        }
        return (true);
    }
}
