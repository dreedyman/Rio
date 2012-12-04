/*
 * Copyright 2009 the original author or authors.
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
package org.rioproject.test.scaling;

import net.jini.core.lookup.ServiceItem;
import net.jini.core.lookup.ServiceTemplate;
import net.jini.lookup.LookupCache;
import net.jini.lookup.ServiceDiscoveryEvent;
import net.jini.lookup.ServiceDiscoveryListener;
import net.jini.lookup.ServiceDiscoveryManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.rmi.RemoteException;
import java.util.Random;

/**
 * The class represents an object that distributes total load among
 * {@link SettableLoadService}s.
 */
public class LoadDistributor {
    /**
     * The logger used by this class.
     */
    static Logger logger = LoggerFactory.getLogger("org.rioproject.test.scaling");
    /**
     * The lookup cache used to monitor services.
     */
    private LookupCache lookupCache;
    /**
     * The flag indicates if service load update is needed.
     */
    private boolean serviceLoadUpdateNeeded = true;
    /**
     * The current total load.
     */
    private double totalLoad = 0;
    /**
     * The number of times to invoke the setServiceLoad
     * method on the service
     */
    private int iterations = 1;
            
    
    /**
     * Constructs a <code>LoadDistributor</code>.
     * 
     * @param sdm the service discovery manager to use to discover
     *            {@link SettableLoadService}s.
     * @throws java.rmi.RemoteException If connectivity fails
     */
    public LoadDistributor(ServiceDiscoveryManager sdm) throws RemoteException {
        
        // Create LookupCache
        Class[] types = new Class[] {SettableLoadService.class};
        ServiceTemplate template = new ServiceTemplate(null, types, null);
        ServiceDiscoveryListener listener = new ServiceDiscoveryListener() {
            public void serviceAdded(ServiceDiscoveryEvent event) {
                logger.info("serviceAdded() event received: " + event);
                triggerServiceLoadUpdate();
            }
            public void serviceChanged(ServiceDiscoveryEvent event) {
                logger.info("serviceChanged() event received: " + event);
                triggerServiceLoadUpdate();
            }
            public void serviceRemoved(ServiceDiscoveryEvent event) {
                logger.info("serviceRemoved() event received: " + event);
                triggerServiceLoadUpdate();
            }
        };
        lookupCache = sdm.createLookupCache(template, null, listener);
        
        // Run the monitoring method in a separate thread
        Runnable starter = new Runnable() {
            public void run() {
                LoadDistributor.this.run();
            }
        };
        new Thread(starter).start();
    }

    
    /**
     * Sets the total load to distribute among the services.
     *
     * @param totalLoad the new total load.
     */
    void setTotalLoad(double totalLoad) {
        setTotalLoad(totalLoad, 1);
    }

    /**
     * Sets the total load to distribute among the services.
     *
     * @param totalLoad the new total load.
     * @param iterations The number of times to invoke the setServiceLoad
     * method on the service
     */
    void setTotalLoad(double totalLoad, int iterations) {
        this.totalLoad = totalLoad;
        this.iterations = iterations;
        triggerServiceLoadUpdate();
    }
    
    /**
     * Triggers the service load update.
     */
    private synchronized void triggerServiceLoadUpdate() {
        serviceLoadUpdateNeeded = true;
        notifyAll();
    }
    
    /**
     * Monitors the {@link #serviceLoadUpdateNeeded} flag and, when
     * necessary, distributes the total load among available services.
     */
    private void run() {
        while(true) {
            synchronized (this) {
                while(!serviceLoadUpdateNeeded) {
                    try {
                        wait(10000);
                    } catch (InterruptedException e) {
                    }
                }
                serviceLoadUpdateNeeded = false;
            }
            updateServiceLoad();
        }
    }
    
    /**
     * Distributes the total load among available services.
     */
    private void updateServiceLoad() {
        double totalLoad = this.totalLoad;
        ServiceItem[] items = lookupCache.lookup(null, Integer.MAX_VALUE);
        
        String msg = "===> Changing service load.";
        msg += " Total load: " + totalLoad + ",";
        msg += " number of services: " + items.length + ", iterations: "+iterations+".";
        logger.info("");
        logger.info(msg);

        if (items.length > 0) {
            double load = totalLoad / items.length;
            boolean retry = true;
            while(retry) {
                Random random = new Random();
                int index = items.length-1;
                if(index>1)
                    index = random.nextInt(index);
            //for (int i = 0; i < items.length; i++) {
                SettableLoadService service =
                        (SettableLoadService) items[index].service;
                String str = "service [" + index + "].setLoad(" + load + ") - ";
                try {
                    for(int i=0; i<iterations; i++)
                        service.setLoad(load);
                    logger.info(str + "OK");
                    retry = false;
                } catch (RemoteException e) {
                    logger.info(str + "FAILURE");
                    logger.info(e.toString());
                }
            }
        }
    }
}
