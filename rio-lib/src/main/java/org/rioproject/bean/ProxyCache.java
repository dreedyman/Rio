/*
 * Copyright to the original author or authors.
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

import org.rioproject.bean.proxy.PackagedMethod;
import org.rioproject.servicecore.Service;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Provides a cache of bean implementation proxies. This is required to avoid
 * unnecessary creation of proxy classes that can quickly clutter permanent
 * generation.
 */
public class ProxyCache {
    private static final ReferenceQueue<Object> refQueue =
      new ReferenceQueue<Object>();
    private static final List<Object> proxies = new ArrayList<Object>();
    private static final Logger logger = Logger.getLogger(ProxyCache.class.getName());

    private ProxyCache() {}

    /**
     * Get a proxy from the cache.
     *
     * @param bean The bean to delegate invocations to.
     * @param service The <code>Service</code> to delegate invocations to
     * @param loader The <code>ClassLoader</code> to use
     *
     * @return An object (dynamic proxy) used to delegate method invocations
     * between the service an the bean. If there is not an available proxy, one
     * will be created. Otherwise an available proxy from the cache of proxies
     * will be used.
     *
     * @throws IllegalArgumentException if the bean, service or loader parameters
     * are null
     */
    public static Object getProxy(Object bean, Service service, ClassLoader loader) {
        if(bean==null)
            throw new IllegalArgumentException("bean cannot be null");
        if(service==null)
            throw new IllegalArgumentException("service cannot be null");
        if(loader==null)
            throw new IllegalArgumentException("loader cannot be null");
        Object proxy = null;
        synchronized(proxies) {
            /*
             * Take this opportunity to remove from the table entries
             * whose weak references have been cleared.
             */
            checkRefQueue();
            for(Object o : proxies) {
                ProxyHandler p = (ProxyHandler)Proxy.getInvocationHandler(o);
                if(!p.isInUse()) {
                    p.setInUse(true);
                    p.setBean(bean);
                    p.setService(service);
                    proxy = o;
                    if(logger.isLoggable(Level.FINE))
                        logger.fine("Re-use proxy for "+bean.getClass().getName());
                    break;
                }
            }
        }
        if(proxy==null) {
            ProxyHandler p = new ProxyHandler();
            p.setInUse(true);
            p.setBean(bean);
            p.setService(service);
            proxy = Proxy.newProxyInstance(loader,
                                   new Class[]{Service.class},
                                   p);
            synchronized (proxies) {
                proxies.add(proxy);
                new ProxyHandlerEntry(proxy);
                if(logger.isLoggable(Level.FINE))
                    logger.fine("Create new proxy for "+bean.getClass().getName()+", count now: "+proxies.size());
            }
        }
        return proxy;
    }

    /**
     * Release a proxy back into the cache. The provided proxy must be a proxy
     * produced by the class.
     *
     * @param proxy The proxy to release. If null
     *
     * @return If the proxy is released (made available) return true, otherwise
     * return false
     *
     * @throws IllegalArgumentException If the proxy was not created by this class.
     */
    public static boolean release(Object proxy) {
        if(proxy==null)
            return false;
        boolean released = false;
        Object ih = Proxy.getInvocationHandler(proxy);
        if(!(ih instanceof ProxyHandler))
            throw new IllegalArgumentException("The provided proxy does not " +
                                               "have the correct InvocationHandler");
        ProxyHandler handler = (ProxyHandler)ih;
        synchronized(proxies) {
            for(Object o : proxies) {
                ProxyHandler p = (ProxyHandler)Proxy.getInvocationHandler(o);
                if(handler.getService().equals(p.getService())) {
                    p.setBean(null);
                    p.setService(null);
                    p.setInUse(false);
                    released = true;
                    break;
                }
            }
        }
        /*
         * Take this opportunity to remove from the table entries
         * whose weak references have been cleared.
         */
        checkRefQueue();
        return released;
    }

    private static void checkRefQueue() {
        ProxyHandlerEntry entry ;
        while ((entry = (ProxyHandlerEntry)refQueue.poll()) != null) {
            if (!entry.removed) {
                proxies.remove(entry.proxy);
                if(logger.isLoggable(Level.FINE))
                    logger.fine("Removed de-referenced ProxyHandler, count now: "+proxies.size());
            }
        }
    }

    private static class ProxyHandler implements InvocationHandler {
        private boolean inUse = false;
        private Object bean;
        private Object service;
        private Set<PackagedMethod> methodSet = new HashSet<PackagedMethod>();

        void setBean(Object bean) {
            this.bean = bean;
        }

        void setService(Object service) {
            this.service = service;
            methodSet.clear();
            if(this.service!=null) {
                for(Method method : service.getClass().getMethods()) {
                    methodSet.add(new PackagedMethod(method));
                }
            }
        }

        Object getService() {
            return service;
        }

        public boolean isInUse() {
            return inUse;
        }

        void setInUse(boolean inUse) {
            this.inUse = inUse;
        }

        public Object invoke(Object proxy, Method method, Object[] args) throws
                                                                         Throwable {
            PackagedMethod template = new PackagedMethod(method);
            try {
                if(methodSet.contains(template)) {
                    if(logger.isLoggable(Level.FINEST)) {
                        logger.finest("Method "+method.getName()+", " +
                                      "invocation found in ServiceBean using");
                    }
                    return method.invoke(service, args);
                } else {
                    if(logger.isLoggable(Level.FINEST)) {
                        logger.finest("Method "+method.getName()+", " +
                                      "invocation being performed on " +
                                      bean.getClass().getName()+", "+
                                      "no matching method found on service bean");
                    }
                    Class beanClass = bean.getClass();
                    Method beanMethod =
                        beanClass.getMethod(method.getName(),
                                            method.getParameterTypes());
                    return(beanMethod.invoke(bean, args));
                }
            } catch (InvocationTargetException e) {
                throw e.getTargetException();
            } finally {
                template.clear();
            }
        }
    }

    /**
     * ProxyHandlerEntry contains a weak reference to a proxy.  The weak
     * reference is registered with the private static "refQueue" queue.
     */
    private static class ProxyHandlerEntry extends WeakReference<Object> {
        public Object proxy;
        /**
         * set to true if the entry has been removed from the table because it
         * has been replaced, so it should not be attempted to be removed again
         */
        public boolean removed = false;

        public ProxyHandlerEntry(Object proxy) {
            super(proxy, refQueue);
            this.proxy = proxy;
        }
    }
}
