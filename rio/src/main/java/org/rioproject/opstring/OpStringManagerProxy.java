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
package org.rioproject.opstring;

import net.jini.admin.Administrable;
import net.jini.config.ConfigurationException;
import net.jini.core.discovery.LookupLocator;
import net.jini.core.lookup.ServiceItem;
import net.jini.core.lookup.ServiceTemplate;
import net.jini.discovery.DiscoveryManagement;
import net.jini.lookup.LookupCache;
import net.jini.lookup.ServiceDiscoveryEvent;
import net.sf.cglib.proxy.*;
import org.rioproject.core.OperationalStringManager;
import org.rioproject.core.provision.ProvisionManager;
import org.rioproject.monitor.DeployAdmin;
import org.rioproject.monitor.ProvisionMonitor;
import org.rioproject.resources.client.DiscoveryManagementPool;
import org.rioproject.resources.client.LookupCachePool;
import org.rioproject.resources.client.ServiceDiscoveryAdapter;
import org.rioproject.resources.util.ThrowableUtil;

import java.io.IOException;
import java.lang.reflect.Method;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;

/**
 * A generated proxy to assist with the management of
 * {@link org.rioproject.core.OperationalStringManager} instances.
 *
 * @author Dennis Reedy
 */
public class OpStringManagerProxy {
    private static DiscoveryManagement discoMgmt;

    /**
     * Create a dynamic CGLIB generated proxy for OperationalStringManager
     * management. Invoking this method relies on this utility having it's
     * DiscoveryManagement property set.
     *
     * @param name The name of the OperationalString. This must not be null.
     * @param manager The primary (managing) OperationalStringManager
     *
     * @return A generated proxy
     *
     * @throws ConfigurationException If the LookupCachePool cannot be accessed
     * or created
     * @throws IOException If DiscoveryManagement has problems
     */
    public static
    OperationalStringManager getProxy(String name,
                                      OperationalStringManager manager)
        throws ConfigurationException, IOException {

        if(discoMgmt==null) {
            throw new IllegalStateException("DiscoveryManagement has not been " +
                                            "set into proxy");
        }
        Enhancer enhancer = new Enhancer();
        enhancer.setInterfaces(new Class[]{OpStringManager.class});
        enhancer.setUseFactory(false);
        enhancer.setCallback(new OpStringManagerDispatcher(name,
                                                           manager,
                                                           discoMgmt));
        return (OperationalStringManager)enhancer.create();
    }

    /**
     * Create a dynamic CGLIB generated proxy for OperationalStringManager
     * management
     *
     * @param name The name of the OperationalString. This must not be null.
     * @param manager The primary (managing) OperationalStringManager
     * @param dMgr DiscoveryManagement instance to use. This must not be null.
     *
     * @return A generated proxy
     *
     * @throws ConfigurationException If the LookupCachePool cannot be accessed
     * or created
     * @throws IOException If DiscoveryManagement has problems
     */
    public static
    OperationalStringManager getProxy(String name,
                                      OperationalStringManager manager,
                                      DiscoveryManagement dMgr)
        throws ConfigurationException, IOException {

        assert name!=null;
        assert dMgr!=null;
        if(discoMgmt==null)
            discoMgmt = dMgr;
        Enhancer enhancer = new Enhancer();
        enhancer.setInterfaces(new Class[]{OpStringManager.class});
        enhancer.setUseFactory(false);
        enhancer.setCallbackFilter(FILTER);
        enhancer.setCallbacks(new Callback[] {
            new OpStringManagerDispatcher(name, manager, dMgr),
            NoOp.INSTANCE});        
        return (OperationalStringManager)enhancer.create();
    }

    /**
     * A CGLIB dispatcher for managing the invocation of methods to the
     * primary {@link org.rioproject.core.OperationalStringManager} instance.
     */
    public static class OpStringManagerDispatcher
        extends ServiceDiscoveryAdapter implements MethodInterceptor {
        String name;
        OperationalStringManager manager;
        DiscoveryManagement dMgr;
        LookupCache lCache;
        boolean terminated = false;
        final List<ProvisionManager> monitors = new ArrayList<ProvisionManager>();

        public OpStringManagerDispatcher(String name,
                                         OperationalStringManager manager,
                                         DiscoveryManagement dMgr) throws
                                                                   ConfigurationException,
                                                                   IOException {
            this.name = name;
            this.manager = manager;
            this.dMgr = dMgr;
            ServiceTemplate template =
                new ServiceTemplate(null,
                                    new Class[] {ProvisionManager.class},
                                    null);
            lCache = LookupCachePool.getInstance().getLookupCache(dMgr,
                                                                  template);
            if(lCache instanceof LookupCachePool.SharedLookupCache) {
                LookupCachePool.SharedLookupCache sCache =
                    (LookupCachePool.SharedLookupCache)lCache;
                for(ServiceItem item : sCache.lookupRemote(null, Integer.MAX_VALUE)) {
                    synchronized(monitors) {
                        monitors.add((ProvisionMonitor) item.service);
                    }
                }
            } else {
                for(ServiceItem item : lCache.lookup(null, Integer.MAX_VALUE)) {
                    synchronized(monitors) {
                        monitors.add((ProvisionMonitor) item.service);
                    }
                }
            }
            lCache.addListener(this);
        }

        public Object intercept(Object proxy,
                                Method method,
                                Object[] args,
                                MethodProxy methodProxy) throws Throwable {
            if(terminated) {
                throw new IllegalStateException("The OpStringManagerDispatcher "+
                                                "has been terminated, " +
                                                "invocations through this " +
                                                "utility are not possible " +
                                                "in it's current state. " +
                                                "Make sure all invoking " +
                                                "threads are terminated to " +
                                                "resolve this issue");
            }
            if(method.getName().equals("terminate")) {
                lCache.removeListener(this);
                monitors.clear();
                terminated = true;
                return null;
            }
            while(!terminated) {
                try {
                    if(manager.isManaging())
                        break;
                    manager = getManager();
                } catch (Throwable t) {
                    if(terminated)
                        break;
                    if(!ThrowableUtil.isRetryable(t)) {
                        manager = getManager();
                    } else {
                        throw t;
                    }
                }
            }
            if(terminated)
                return null;
            return method.invoke(manager, args);
        }

        public void serviceAdded(ServiceDiscoveryEvent event) {
            ServiceItem item = event.getPostEventServiceItem();
            synchronized(monitors) {
                ProvisionMonitor monitor = (ProvisionMonitor)item.service;
                if(!monitors.contains(monitor))
                    monitors.add(monitor);
            }
        }

        @SuppressWarnings("unchecked")
        public void serviceRemoved(ServiceDiscoveryEvent event) {
            ServiceItem item = event.getPreEventServiceItem();
            ProvisionMonitor m = (ProvisionMonitor)item.service;
            synchronized(monitors) {
                monitors.remove(m);
            }
            lCache.discard(m);
        }

        OperationalStringManager getManager() throws Throwable {
            OperationalStringManager opMgr = null;
            ProvisionManager monitor;
            synchronized(monitors) {
                if(monitors.size()>0)
                    monitor = monitors.get(0);
                else {
                    StringBuffer sb = new StringBuffer();
                    if(dMgr instanceof DiscoveryManagementPool.SharedDiscoveryManager) {
                        DiscoveryManagementPool.SharedDiscoveryManager sdm =
                            (DiscoveryManagementPool.SharedDiscoveryManager)dMgr;
                        sb.append("Using discovery attributes: ");
                        if(sdm.getGroups()!=null) {
                            sb.append("groups=[");
                            int i=0;
                            for(String s : sdm.getGroups()) {
                                if(i>0)
                                    sb.append(", ");
                                sb.append(s);
                                i++;
                            }
                            sb.append("]");
                        }
                        if(sdm.getLocators()!=null) {
                            sb.append(" ");
                            sb.append("locators=[");
                            int i=0;
                            for(LookupLocator l : sdm.getLocators()) {
                                if(i>0)
                                    sb.append(", ");
                                sb.append(l.toString());
                                i++;
                            }
                            sb.append("] ");
                        }
                    }
                    throw new RemoteException("No ProvisionMonitor " +
                                              "instances available. "+
                                              sb.toString());
                }
            }
            /* If we get an OperationalStringException getting the primary
             * manager, retry before re-throwing the exception. This is to
             * allow for a backup ProvisionMonitor to take over */
            Throwable toThrow = null;
            for(int i=0; i<3; i++) {
                if(terminated)
                    break;
                try {
                    DeployAdmin dAdmin =
                        (DeployAdmin)((Administrable)monitor).getAdmin();
                    opMgr = dAdmin.getOperationalStringManager(name);
                    toThrow = null;
                    break;
                } catch(Throwable t) {
                    //t.printStackTrace();
                    toThrow = t;
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e1) {
                        /* ignore */
                    }
                    if(!ThrowableUtil.isRetryable(t)) {
                        synchronized(monitors) {
                            if(monitors.size()>0)
                                monitors.remove(monitor);
                            else
                                toThrow = t;
                        }
                    }
                }
            }
            if(toThrow!=null)
                throw toThrow;
            return opMgr;
        }
    }

    public static void setDiscoveryManagement(DiscoveryManagement dMgr) {
        discoMgmt = dMgr;
    }

    public static DiscoveryManagement getDiscoveryManagement() {
        return discoMgmt;
    }

    public static interface OpStringManager extends OperationalStringManager {
        void terminate();
    }

    private static final CallbackFilter FILTER = new CallbackFilter() {
        public int accept(Method method) {
            if (method.getParameterTypes().length == 0 &&
                method.getName().equals("finalize") ){
                return 1;
            }
            else {
                return 0;
            }
        }
    };
}
