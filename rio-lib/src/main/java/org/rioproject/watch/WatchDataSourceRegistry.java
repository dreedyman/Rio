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
package org.rioproject.watch;

import org.rioproject.core.jsb.ServiceBeanContext;
import org.rioproject.jmx.JMXUtil;
import org.rioproject.jmx.MBeanServerFactory;
import org.rioproject.sla.SLAPolicyHandler;
import org.rioproject.system.SystemWatchID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.*;
import java.rmi.NoSuchObjectException;
import java.util.*;

/**
 * An implementation of a {@link org.rioproject.watch.WatchRegistry}
 */
public class WatchDataSourceRegistry implements WatchRegistry {
    /** Collection of Watch instances that have been registered */
    protected final List<Watch> watchRegistry = new ArrayList<Watch>();
    /** Table of ThresholdWatch classnames and ThresholdListener objects */
    protected final Map<String, Collection<ThresholdListener>> thresholdListenerTable = new Hashtable<String, Collection<ThresholdListener>>();
    /** The ServiceBeanContext */
    private ServiceBeanContext context;
    /** A Logger */
    private static Logger logger = LoggerFactory.getLogger(WatchDataSourceRegistry.class);

    /**
     * @see org.rioproject.watch.WatchRegistry#deregister
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
            if(thresholdListenerTable.containsKey(watch.getId())) {
                thresholdListenerTable.remove(watch.getId());
            }
            //unregister from jmx
            unregisterJMX(watch);
        }
    }

    private void unregisterJMX(Watch watch) {
        try {
            ObjectName objectName = getObjectName(watch);
            if(MBeanServerFactory.getMBeanServer().isRegistered(objectName))
               MBeanServerFactory.getMBeanServer().unregisterMBean(objectName);
        } catch (MalformedObjectNameException e) {
            logger.warn(e.toString(), e);
        } catch (InstanceNotFoundException e) {
            logger.warn(e.toString(), e);
        } catch (MBeanRegistrationException e) {
            logger.warn(e.toString(), e);
        }
    }

    /**
     * @see org.rioproject.watch.WatchRegistry#closeAll()
     */
    public void closeAll() {
        Watch[] watches = watchRegistry.toArray(new Watch[watchRegistry.size()]);
        for(Watch w : watches) {
            unregisterJMX(w);
            if(w instanceof PeriodicWatch)
                ((PeriodicWatch)w).stop();
            try {
                WatchDataSource wd = w.getWatchDataSource();
                if (wd != null)
                    wd.close();
            } catch (Throwable t) {
                logger.warn("Closing WatchDataSource", t);
            }
        }        
    }

    /**
     * @see org.rioproject.watch.WatchRegistry#register
     */
    public void register(Watch... watches) {
        if(watches == null)
            throw new IllegalArgumentException("Watches cannot be null");
        watchRegistry.addAll(Arrays.asList(watches));
        for (Watch watch : watches) {
            associateThresholdListener(watch);
            //register jmx
            registerJMX(watch);
        }
    }

    private void registerJMX(Watch watch) {
        try {
            ObjectName objectName = getObjectName(watch);
            MBeanServer mbeanServer = MBeanServerFactory.getMBeanServer();
            mbeanServer.registerMBean(watch, objectName);
        } catch (MalformedObjectNameException e) {
            logger.warn(e.toString(), e);
        } catch (MBeanRegistrationException e) {
            logger.warn(e.toString(), e);
        } catch (InstanceAlreadyExistsException e) {
            logger.warn(e.toString(), e);
        } catch (NotCompliantMBeanException e) {
            logger.warn(e.toString(), e);
        }
    }

    private ObjectName getObjectName(Watch watch) throws
                                                  MalformedObjectNameException {
        String domain = getClass().getPackage().getName();
        String objectName = null;
        if(context != null) {
            String jmxName = JMXUtil.getJMXName(context, domain);
            objectName = jmxName+","+
                         "name=Watch,"+
                         "id="+watch.getId();
        }
        if(objectName == null) {
            String id = watch.getId();
            if(id.equals(""))
                id = "<empty-string>";
            objectName = domain+":type=Watch,ID="+id;
        }
        return (ObjectName.getInstance(objectName));
    }

    /**
     * @see org.rioproject.watch.WatchRegistry#addThresholdListener
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
            collection = new ArrayList<ThresholdListener>();
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
     * @see org.rioproject.watch.WatchRegistry#removeThresholdListener
     */
    public void removeThresholdListener(String id, ThresholdListener thresholdListener) {
        if(thresholdListenerTable.containsKey(id)) {
            Collection<ThresholdListener> collection = thresholdListenerTable.get(id);
            collection.remove(thresholdListener);
        }
    }

    /**
     * This method will associate a Watch to a ThresholdListener, iff the Watch
     * is a ThresholdWatch and the Watch objects classname is found in the table
     * 
     * @param watch The Watch to associate
     */
    protected void associateThresholdListener(Watch watch) {
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
     * @see org.rioproject.watch.WatchRegistry#findWatch(String)
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
     * @see org.rioproject.watch.WatchRegistry#fetch()
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
     * @see org.rioproject.watch.WatchRegistry#fetch(String)
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
            if(isSystemWatch) {
                Watch w = context.getComputeResourceManager().getComputeResource().getMeasurableCapability(id);
                if(w!=null)
                    wds = w.getWatchDataSource();
            }
        }
        return wds;
    }

    /**
     * @see org.rioproject.watch.WatchRegistry#setServiceBeanContext
     */
    public void setServiceBeanContext(ServiceBeanContext context) {
        this.context = context;
    }
}
