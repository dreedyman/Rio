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

import net.jini.core.lookup.ServiceID;
import net.jini.core.lookup.ServiceRegistrar;
import net.jini.discovery.DiscoveryEvent;
import net.jini.discovery.DiscoveryListener;
import net.jini.discovery.DiscoveryManagement;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;

/**
 * A DiscoveryListener that keeps a record of discovered and discarded
 * ServiceRegistrar instances
 *
 * @author Dennis Reedy
 */
public class RecordingDiscoveryListener implements DiscoveryListener {
    DiscoveryManagement disco;
    final List<ReggieStat> discoveryTimes = new ArrayList<ReggieStat>();

    public RecordingDiscoveryListener(DiscoveryManagement disco) {
        if(disco==null)
            throw new IllegalArgumentException(
                "DiscoveryManagement cannot be null");
        this.disco = disco;
    }

    public DiscoveryManagement getDiscoveryManagement() {
        return(disco);
    }

    public void discovered(DiscoveryEvent dEvent) {
        long t = System.currentTimeMillis();
        for(int i = 0; i < dEvent.getRegistrars().length; i++) {
            try {
                ReggieStat rt = new ReggieStat(ReggieStat.DISCOVERED,
                                               t,
                                               dEvent.getRegistrars()[i]);
                ReggieStat existingStat = getReggieStat(rt);
                if(existingStat != null &&
                   existingStat.type == ReggieStat.DISCARDED) {
                    rt.baseTime = existingStat.eventTime;
                }
                synchronized(discoveryTimes) {
                    discoveryTimes.add(rt);
                }
            } catch(RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    public void discarded(DiscoveryEvent dEvent) {
        long t = System.currentTimeMillis();
        ServiceRegistrar[] reggies = dEvent.getRegistrars();
        for (ServiceRegistrar reggy : reggies) {
            ReggieStat rStat = removeReggieStat(reggy.getServiceID());
            if (rStat != null) {
                rStat.eventTime = t;
                rStat.type = ReggieStat.DISCARDED;
                synchronized (discoveryTimes) {
                    discoveryTimes.add(rStat);
                }
            }
        }
    }

    /**
     * Get the collection of known discovered/discarded ServiceRegistrar
     * discovery stats
     *
     * @param type The type of stat, either <code>ReggieStat.DISCOVERED</code>
     * or <code>ReggieStat.DISCARDED</code>
     *
     * @return Array of <code>ReggieStat</code>s. If there are no stats, a
     * zero-length array is returned. A new array is allocated each time.
     */
    public ReggieStat[] getReggieStats(int type) {
        if(type < ReggieStat.DISCOVERED || type > ReggieStat.DISCARDED)
            throw new IllegalArgumentException("bad type");
        List<ReggieStat> list = new ArrayList<ReggieStat>();
        synchronized(discoveryTimes) {
            for (ReggieStat rt : discoveryTimes) {
                if (rt.type == type)
                    list.add(rt);
            }
        }
        return (list.toArray(new ReggieStat[list.size()]));
    }

    /**
     * Find and a ReggieStat based on the provided machine and port
     *
     * @param reggieStat A ReggieStat object, must not be null
     *
     * @return A ReggieStat instance from the collection which matches the
     *         machine name and port the provided ReggieStat has as properties
     */
    private ReggieStat getReggieStat(ReggieStat reggieStat) {
        if(reggieStat == null)
            throw new IllegalArgumentException("reggieStat is null");
        ReggieStat rStat = null;
        synchronized(discoveryTimes) {
            for (ReggieStat rt : discoveryTimes) {
                if (rt.machine.equals(reggieStat.machine) &&
                    rt.port == reggieStat.port &&
                    rt.groupsMatch(reggieStat)) {
                    rStat = rt;
                    break;
                }
            }
        }
        return (rStat);
    }

    /**
     * Find and remove a ReggieStat based on the provided ServiceID
     *
     * @param id The ServiceID for the ServiceRegistrar, must not be null
     *
     * @return A ReggieStat instance that has been removed from the collection
     *         or null if not found
     */
    private ReggieStat removeReggieStat(ServiceID id) {
        if(id == null)
            throw new IllegalArgumentException("id is null");
        ReggieStat rStat = null;
        synchronized(discoveryTimes) {
            for (ReggieStat rt : discoveryTimes) {
                if (rt.serviceID.equals(id)) {
                    rStat = rt;
                    discoveryTimes.remove(rt);
                    break;
                }
            }
        }
        return (rStat);
    }
}
