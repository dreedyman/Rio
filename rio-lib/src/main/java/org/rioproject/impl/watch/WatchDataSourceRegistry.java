/*
 * Copyright to the original author or authors.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *         http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.rioproject.impl.watch;

import org.rioproject.impl.sla.SLAPolicyHandler;
import org.rioproject.servicebean.ServiceBeanContext;
import org.rioproject.system.SystemWatchID;
import org.rioproject.watch.WatchDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.rmi.NoSuchObjectException;
import java.util.*;

/**
 * An implementation of a {@link WatchRegistry}
 */
public class WatchDataSourceRegistry implements WatchRegistry {
    /** Collection of Watch instances that have been registered */
    private final List<Watch> watchRegistry = new ArrayList<>();
    /** Table of ThresholdWatch class names and ThresholdListener objects */
    private final Map<String, Collection<ThresholdListener>> thresholdListenerTable = new Hashtable<>();
    /** The ServiceBeanContext */
    private ServiceBeanContext context;
    /** A Logger */
    private static Logger logger = LoggerFactory.getLogger(WatchDataSourceRegistry.class);

    /**
     * @see WatchRegistry#deregister
     */
    public void deregister(Watch... watches) {
        if (watches == null)
            throw new IllegalArgumentException("Watches cannot be null");
        watchRegistry.removeAll(Arrays.asList(watches));
        for (Watch watch : watches) {
            try {
                if(watch instanceof PeriodicWatch)
                    ((PeriodicWatch)watch).stop();
                watch.getWatchDataSource().close();

            } catch (NoSuchObjectException e) {
                if (logger.isTraceEnabled())
                    logger.trace("Deregistering Watch", e);
            } catch (Throwable t) {
                logger.warn("Deregistering Watch", t);
            }
            /// unregister ThresholdListeners
            thresholdListenerTable.remove(watch.getId());
        }
    }

    /**
     * @see WatchRegistry#closeAll()
     */
    public void closeAll() {
        Watch[] watches = watchRegistry.toArray(new Watch[0]);
        for(Watch w : watches) {
            //unregisterJMX(w);
            if(w instanceof PeriodicWatch)
                ((PeriodicWatch)w).stop();
            try {
                WatchDataSource wd = w.getWatchDataSource();
                if (wd != null)
                    wd.close();
                watchRegistry.remove(w);
            } catch (Exception e) {
                logger.warn("Closing WatchDataSource", e);
            }
        }        
    }

    /**
     * @see WatchRegistry#register
     */
    public void register(Watch... watches) {
        if(watches == null)
            throw new IllegalArgumentException("Watches cannot be null");
        watchRegistry.addAll(Arrays.asList(watches));
        for (Watch watch : watches) {
            associateThresholdListener(watch);
        }
    }

    /**
     * @see WatchRegistry#addThresholdListener
     */
    public void addThresholdListener(String id, ThresholdListener thresholdListener) {
        if(id==null)
            throw new IllegalArgumentException("id is null");
        if(thresholdListener==null)
            throw new IllegalArgumentException("thresholdListener is null");
        Collection<ThresholdListener> collection;
        if(thresholdListenerTable.containsKey(id)) {
            collection = thresholdListenerTable.get(id);
        } else {
            collection = new ArrayList<>();
        }
        if(!collection.contains(thresholdListener)) {
            if(logger.isTraceEnabled())
                logger.trace("Add [{}] for watch [{}]", thresholdListener.getClass().getName(), id);
            collection.add(thresholdListener);
            thresholdListenerTable.put(id, collection);

            Watch watch = findWatch(id);
            if(logger.isTraceEnabled())
                logger.trace("Found [{}] previously registered watch [{}]", (watch==null?0:1), id);
            if(watch!=null) {
                associateThresholdListener(watch);
            }
        }
    }

    /**
     * @see WatchRegistry#removeThresholdListener
     */
    public void removeThresholdListener(String id, ThresholdListener thresholdListener) {
        if(thresholdListenerTable.containsKey(id)) {
            Collection<ThresholdListener> collection = thresholdListenerTable.get(id);
            collection.remove(thresholdListener);
        }
    }

    /*
     * This method will associate a Watch to a ThresholdListener, iff the Watch
     * is a ThresholdWatch and the Watch objects classname is found in the table
     * 
     * @param watch The Watch to associate
     */
    private void associateThresholdListener(Watch watch) {
        if(!(watch instanceof ThresholdWatch))
            return;
        ThresholdWatch tWatch = (ThresholdWatch)watch;
        if(thresholdListenerTable.containsKey(tWatch.getId())) {
            Collection<ThresholdListener> collection = thresholdListenerTable.get(tWatch.getId());
            for (ThresholdListener tListener : collection) {
                if (logger.isTraceEnabled())
                    logger.trace("Associate Watch [{}] to [{}]", tWatch.getId(), tListener.getClass().getName());
                if (tListener instanceof SLAPolicyHandler) {
                    SLAPolicyHandler slaPolicyHandler = (SLAPolicyHandler)tListener;
                    tWatch.setThresholdValues(slaPolicyHandler.getSLA());
                }
                if(tListener instanceof SettableThresholdListener) {
                    ((SettableThresholdListener)tListener).setThresholdManager(tWatch.getThresholdManager());
                }
            }
        }
    }

    /**
     * @see WatchRegistry#findWatch(String)
     */
    public Watch findWatch(String id) {
        if(id == null)
            throw new IllegalArgumentException("id is null");

        Watch watch = null;
        for(Watch w : watchRegistry) {
            if(id.equals(w.getId())) {
                watch = w;
                break;
            }
        }
        return watch;
    }

    /**
     * @see WatchRegistry#fetch()
     */
    public WatchDataSource[] fetch() {
        WatchDataSource[] wds = new WatchDataSource[watchRegistry.size()];
        int i = 0;
        for(Watch watch : watchRegistry) {
            wds[i++] = watch.getWatchDataSource();
        }
        return wds;
    }

    /**
     * @see WatchRegistry#fetch(String)
     */
    public WatchDataSource fetch(String id) {
        WatchDataSource wds = null;
        Watch watch = findWatch(id);
        if(watch != null) {
            wds = watch.getWatchDataSource();
        } else {
            boolean isSystemWatch = false;
            for(String s : SystemWatchID.IDs) {
                if(s.equals(id)) {
                    isSystemWatch = true;
                    break;
                }
            }
            if(isSystemWatch && context!=null) {
                Watch w = context.getComputeResourceManager().getComputeResource().getMeasurableCapability(id);
                if(w!=null)
                    wds = w.getWatchDataSource();
            }
        }
        return wds;
    }

    /**
     * @see WatchRegistry#setServiceBeanContext
     */
    public void setServiceBeanContext(ServiceBeanContext context) {
        this.context = context;
    }
}
