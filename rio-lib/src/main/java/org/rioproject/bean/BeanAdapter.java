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
package org.rioproject.bean;

import net.jini.config.Configuration;
import net.jini.export.Exporter;
import net.jini.jeri.BasicJeriExporter;
import net.jini.jeri.tcp.TcpServerEndpoint;
import org.rioproject.bean.proxy.BeanDelegator;
import org.rioproject.config.Constants;
import org.rioproject.core.jsb.ServiceBean;
import org.rioproject.core.jsb.ServiceBeanContext;
import org.rioproject.jsb.ServiceBeanAdapter;
import org.rioproject.net.HostUtil;
import org.rioproject.resources.servicecore.Service;

import javax.management.*;
import java.io.IOException;
import java.lang.reflect.Method;
import java.rmi.Remote;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The BeanAdapter provides a basic concrete implementation of a ServiceBean,
 * and provides the support to delegate to a component, a <i>bean</i>
 * (a Plain Old Java Object : POJO), making the bean remotable as Jini
 * technology service using the dynamic service architecture provided by Rio.
 *
 * <p><b><u>Lifecycle Support</u></b><br>
 * The BeanAdapter will invoke lifecycle methods on the bean if
 * the bean has the following methods defined:
 *
 * <pre>
 * public void preAdvertise();
 * public void postAdvertise();
 * public void preUnAdvertise();
 * public void postUnAdvertise();
 * public void preDestroy();
 * </pre>
 *
 * <p>Alternatively, the bean can use the {@link Initialized}, {@link Started},
 * {@link PreAdvertise}, {@link PostAdvertise}, {@link PreUnAdvertise},
 * {@link PostUnAdvertise}, and {@link PreDestroy} annotations to be notified of
 * each respective lifecycle event.
 *
 * <p>Note: ServiceBean initialization is invoked by the start method to
 * initialize the ServiceBean. This method is called only once during the
 * lifecycle of a ServiceBean
 *
 * <p><b><u>Properties and Context</u></b><br>
 * Properties, the {@link net.jini.config.Configuration}, the
 * {@link org.rioproject.core.jsb.ServiceBeanContext} and the service bean itself
 * can be injected into the bean as well. The following methods must be
 * declared to have the respective properties injected into the bean:
 * <pre>
 * public void setParameters(Map<String, Object> parameters);
 * public void setConfiguration(Configuration config);
 * public void setServiceBeanContext(ServiceBeanContext context);
 * public void setServiceBean(ServiceBean serviceBean);
 * </pre>
 *
 * <p>Alternatively, the bean can use the {@link SetConfiguration}, {@link SetParameters},
 * and {@link SetServiceBeanContext} annotations as well.
 *
 * This property injection will be completed during the initialization of the
 * ServiceBean.
 *
 * <p><b><u>Proxy support</u></b><br>
 * The bean may also define a smart proxy. In order for the BeanAdapter to obtain
 * the smart proxy the following method signatures must be defined:
 *
 * <pre>
 * public Object createProxy(<proxy interface type>);
 * </pre>
 *
 * Using the <tt>createProxy</tt> method, the method passes a remote reference
 * to the exported back end implementation. This parameter can be declared to be
 * the interface type your proxy implements, or a Object, which can then be
 * narrowed to the interface type the bean implements. The method must return
 * the bean's smart proxy, using the reference provided.
 *
 * <pre>
 * public void setProxy(Object);
 * </pre>
 *
 * If the bean has the <tt>setProxy</tt> method declared, this method will be
 * invoked with the proxy that has been created for the bean.
 *
 * <p>Alternatively, the bean can use the {@link CreateProxy}
 * and {@link SetProxy} annotations
 *
 * @see org.rioproject.core.jsb.ServiceBean
 * @see org.rioproject.jsb.ServiceBeanAdapter
 *
 * @author Dennis Reedy
 */
public class BeanAdapter extends ServiceBeanAdapter {
    private static final String COMPONENT = "org.rioproject.bean";
    private static final Logger logger = Logger.getLogger(COMPONENT);
    private Object bean;
    private Remote delegatingProxy;

    /**
     * Create an instance of the BeanAdapter
     *
     * @param bean The bean, must not be null
     */
    public BeanAdapter(Object bean) {
        if(bean == null)
            throw new NullPointerException("bean is null");
        this.bean = bean;
    }
   
    @Override
    protected void registerMBean(ObjectName oName,
                                 MBeanServer mbeanServer)
    throws NotCompliantMBeanException,
           MBeanRegistrationException,
           InstanceAlreadyExistsException {
        String implClass = bean.getClass().getName();
        Class[] ifaces = bean.getClass().getInterfaces();
        Class mbean = null;
        for (Class iface : ifaces) {
            if (iface.getName().equals(implClass + "MBean")) {
                mbean = iface;
                break;
            }
        }
        if(mbean!=null) {
            String comment = context.getServiceBeanConfig().getComment();
            mbeanServer.registerMBean(new AggregatingMBean(this,
                                                           bean,
                                                           mbean,
                                                           comment),
                                      oName);
        } else {
            mbeanServer.registerMBean(this, oName);
        }
    }
    
    /**
     * Override the start method to create a delegating proxy required
     * to navigate between the ServiceBean and the bean.
     *
     * <p>This method will also check if the wrapped bean has a
     * <code>preStart()</code> or a <code>postStart()</code> method declared.
     * If the wrapped bean does have an accessible <code>preAdvertise()</code>
     * declared, it will be called prior to starting the bean.
     *
     * <p>Once the bean has been started, the wrapped bean will be checked
     * for <code>postStart()</code> method declaration. If the wrapped bean
     * does have an accessible <code>postStart()</code> declared, it will be
     * called following the parent's start method.
     *
     * @param context The ServiceBeanContext
     * @return A remoted proxy used to communicate to the bean
     * @throws Exception if starting the bean fails
     */
    @Override
    public Object start(final ServiceBeanContext context) throws Exception {
        delegatingProxy = createDelegatingProxy();
        /* If defined, invoke preStart lifecycle method */
        BeanHelper.invokeLifeCycle(null, "preStart", bean);
        Object o = super.start(context);
        /* If defined, invoke postStart lifecycle method, Check if we are
         * being started up first. If so, then the Cybernode will invoke
         * the lifecycle method (RIO-141) */
        Boolean rioStarting =
            (Boolean)context.getServiceBeanConfig().getConfigurationParameters()
                                                   .get(Constants.STARTING);
        if(logger.isLoggable(Level.FINEST))
            logger.finest("The bean ["+bean.getClass().getName()+"], " +
                          "is in the process of being instantiated: "+
                          (rioStarting==null?"false":rioStarting));
        if(!(rioStarting!=null && rioStarting))
            BeanHelper.invokeLifeCycle(null, "postStart", bean);
        /*
        new Thread(new Runnable() {
            public void run() {
                try {
                    BeanHelper.invokeLifeCycle(null, "postStart", bean);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
        */
        return(o);
    }

    /**
     * If the provided bean has the following method signatures defined,
     * the corresponding properties will be injected into the bean:
     * <pre>
     * public void setParameters(Map parameters);
     * public void setConfiguration(Configuration config);
     * public void setServiceBeanContext(ServiceBeanContext context);
     * </pre>
     * <br>
     * <p>Alternatively, the bean can use the {@link SetConfiguration},
     * {@link SetParameters}, and {@link SetServiceBeanContext} annotations as
     * well.
     *
     * @param bean The bean to check for property injection
     * @param context The {@link ServiceBeanContext}
     *
     * @throws IllegalArgumentException if either of the parameters are null
     */
    public static void invokeLifecycleInjectors(Object bean,
                                                ServiceBeanContext context) {
        if(bean==null)
            throw new IllegalArgumentException("bean cannot be null");
        if(context==null)
            throw new IllegalArgumentException("ServiceBeanContext cannot be null");
        /* Invoke the method with @SetParameters annotation or the
         * setParameters method */
        try {
            Map<String, ?> parameters =
                context.getServiceBeanConfig().getInitParameters();
            BeanHelper.invokeBeanMethod(bean,
                                        SetParameters.class,
                                        "setParameters",
                                        new Class[]{Map.class},
                                        new Object[]{parameters});
        } catch(Exception e) {
            logger.log(Level.WARNING, "Setting parameters", e);
        }
        /* Invoke the method with @SetConfiguration annotation or the
         * setConfiguration method */
        try {
            Configuration config = context.getConfiguration();
            BeanHelper.invokeBeanMethod(bean,
                                        SetConfiguration.class,
                                        "setConfiguration",
                                        new Class[]{Configuration.class},
                                        new Object[]{config});
        } catch(Exception e) {
            logger.log(Level.WARNING, "Setting configuration", e);
        }
        /* Invoke the method with @SetServiceBeanContext annotation or the
         * setServiceBeanContext method */
        try {
            BeanHelper.invokeBeanMethod(bean,
                                        SetServiceBeanContext.class,
                                        "setServiceBeanContext",
                                        new Class[]{ServiceBeanContext.class},
                                        new Object[]{context});
        } catch(Exception e) {
            logger.log(Level.WARNING, "Setting ServiceBeanContext", e);
        }
    }

    /**
     * Override the initialize method to check if the wrapped bean has a
     * <code>preInitialize()</code> method declared. If the wrapped bean
     * does have an accessible <code>preInitialize()</code> declared, it
     * will be called prior to initializing the bean.
     *
     * <p>Once the parent's initialize method has been invoked, if the wrapped
     * bean has the following method signatures defined, the corresponding
     * properties will be injected into the bean:
     * <pre>
     * public void setParameters(Map parameters);
     * public void setConfiguration(Configuration config);
     * public void setServiceBeanContext(ServiceBeanContext context);
     * public void setServiceBean(ServiceBean serviceBean);
     * </pre>
     * <br>
     *
     * <p>Once bean initialization has been processed,
     * the wrapped bean will be checked for <code>postInitialize()</code>
     * method declaration. If the wrapped bean does have an accessible
     * <code>postInitialize()</code> declared, it will be called following
     * the parent's initialize method.
     *
     * @throws Exception if the initialization process fails
     */
    @Override
    public void initialize(final ServiceBeanContext context) throws Exception {
        /* If defined, invoke preInitialize lifecycle method */
        BeanHelper.invokeLifeCycle(null, "preInitialize", bean);

        invokeLifecycleInjectors(bean, context);

        super.initialize(context);

        /* Invoke the setServiceBean method */
        BeanHelper.invokeBeanMethod(bean,
                                    SetServiceBean.class,
                                    "setServiceBean",
                                    new Class[]{ServiceBean.class},
                                    new Object[]{this});
        /* If defined, invoke postInitialize lifecycle method */
        //BeanHelper.invokeLifeCycle(null, "postInitialize", bean);
    }

    /**
     * Override the advertise method to check if the wrapped bean has a
     * <code>preAdvertise()</code> or a <code>postAdvertise()</code> method
     * declared. If the wrapped bean does have an accessible
     * <code>preAdvertise()</code> declared, it will be called prior to
     * advertising the bean.
     *
     * <p>Once bean advertisement has been processed,
     * the wrapped bean will be checked for <code>postAdvertise()</code>
     * method declaration. If the wrapped bean does have an accessible
     * <code>postAdvertise()</code> declared, it will be called following
     * the parent's advertise method.
     */
    @Override
    public void advertise() throws IOException {
        try {
            BeanHelper.invokeLifeCycle(PreAdvertise.class, "preAdvertise", bean);
        } catch(Exception e) {
            logger.log(Level.WARNING,
                       "Invoking Bean ["+bean.getClass().getName()+"] "+
                       "preAdvertise() ",
                       e);
        }
        super.advertise();
        /* If defined, invoke postAdvertise lifecycle method */
        try {
            BeanHelper.invokeLifeCycle(PostAdvertise.class, "postAdvertise", bean);
        } catch(Exception e) {
            logger.log(Level.WARNING,
                       "Invoking Bean ["+bean.getClass().getName()+"] "+
                       "postAdvertise() ",
                       e);
        }
    }

    /** 
     * Override the unadvertise method to check if the wrapped bean has a
     * <code>preUnAdvertise()</code> or a <code>postUnAdvertise()</code> method
     * declared. If the wrapped bean does have an accessible
     * <code>preUnadvertise()</code> declared, it will be called prior to
     * unadvertising the bean.
     *
     * <p>Once bean unadvertisement has been processed,
     * the wrapped bean will be checked for <code>postUnAdvertise()</code>
     * method declaration. If the wrapped bean does have an accessible
     * <code>postUnAdvertise()</code> declared, it will be called following
     * the parent's unadvertise method.
     */
    @Override
    public void unadvertise() {
        try {
            BeanHelper.invokeLifeCycle(PreUnAdvertise.class, "preUnAdvertise", bean);
        } catch(Exception e) {
            logger.log(Level.WARNING,
                       "Invoking Bean ["+bean.getClass().getName()+"] "+
                       "preUnAdvertise() ",
                       e);
        }
        super.unadvertise();
        /* If defined, invoke unadvertised lifecycle method */
        try {
            BeanHelper.invokeLifeCycle(PostUnAdvertise.class, "postUnAdvertise", bean);
        } catch(Exception e) {
            logger.log(Level.WARNING,
                       "Invoking Bean ["+bean.getClass().getName()+"] "+
                       "postUnAdvertise() ",
                       e);
        }
    }

    /**
     * Override the destroy method to check if the wrapped bean has a
     * <code>preDestroy()</code> method declared. If the wrapped bean does
     * have an accessible <code>preDestroy()</code> declared, it will be
     * called prior to destroying the bean.
     */
    @Override
    public void destroy() {
        try {
            BeanHelper.invokeLifeCycle(PreDestroy.class, "preDestroy", bean);
        } catch(Exception e) {
            String s = bean==null?"<unknown:null>":bean.getClass().getName();
            logger.log(Level.WARNING, "Invoking Bean ["+s+"] preDestroy()", e);
        }
        try {
            super.destroy();
        } finally {
            bean = null;
            if(delegatingProxy!=null) {
                /*if(ProxyCache.release(delegatingProxy)) {
                    if(logger.isLoggable(Level.FINE))
                        logger.fine("Released delegating proxy for "+
                                    getServiceBeanContext().getServiceElement().getName());
                }*/
                delegatingProxy = null;
            }
        }
    }

    /**
     * Create the delegating proxy
     *
     * @return The proxy that will handle the delegation between the ServiceBean
     * and the Bean (POJO)
     *
     * @throws RuntimeException
     */
    protected Remote createDelegatingProxy() {
        try {
            Class[] interfaces = getInterfaceClasses(bean.getClass());
            Remote proxy = (Remote) BeanDelegator.getInstance(this, bean, interfaces);
            return(proxy);
            //Remote proxy = (Remote)ProxyCache.getProxy(bean,
            //                                           this,
            //                                           getClass().getClassLoader());
            //return(proxy);

        } catch (Exception e) {
            logger.info("exporting a standard proxy");
            throw new RuntimeException("could not create proxy", e);
        }
    }

    /*
     * Get an array of classes the bean & service bean implement
     */
    private static Class[] getInterfaceClasses(Class c)
        throws ClassNotFoundException {
        Set<Class> remotes = new HashSet<Class>();
        Class[] interfaces = c.getInterfaces();
        remotes.addAll(Arrays.asList(interfaces));
        remotes.add(Service.class);
        return(remotes.toArray(new Class[remotes.size()]));
    }

    /**
     * Override exportDo, using our delegating proxy to handle
     * invocations
     */
    @Override
    protected Remote exportDo(Exporter exporter) throws Exception {
        if(exporter == null)
            throw new NullPointerException("exporter is null");
        return (exporter.export(delegatingProxy));
    }

    /**
     * Override getExporter, creating a BeanInvocationLayerFactory
     * for the Exporter instead of of the BasicILFactory
     */
    @Override
    protected Exporter getExporter(Configuration config) throws Exception {
        Exporter exporter = (Exporter)config.getEntry(COMPONENT,
                                                      "serverExporter",
                                                      Exporter.class,
                                                      null);
        if(exporter==null) {
            String host = HostUtil.getHostAddressFromProperty(Constants.RMI_HOST_ADDRESS);
            exporter = new BasicJeriExporter(TcpServerEndpoint.getInstance(host, 0),
                                             new BeanInvocationLayerFactory(),
                                             false,
                                             true);
        }
        if(logger.isLoggable(Level.FINER))
            logger.finer("["+bean.getClass().getName()+"] using exporter "+exporter.toString());
        return exporter;
    }

    /*
     * Override createProxy to check if the bean has it's own proxy defined
     */
    @Override
    protected Object createProxy() {
        Remote remoteRef = getExportedProxy();
        Class proxyType;

        Object proxy = getCustomProxy(bean, remoteRef);
        if(proxy==null)
            proxy = super.createProxy();

        String setProxyMethodName="setProxy";
        /* First check if the @SetProxy annotation is declared */
        Method setProxy = BeanHelper.getAnnotatedMethod(bean, SetProxy.class);
        if(setProxy!=null) {
            proxyType = BeanHelper.getMethodFirstParamType(setProxy);
            setProxyMethodName = setProxy.getName();
        } else {
            /* Else, if the setProxy method is declared, invoke it */
            proxyType = BeanHelper.getMethodFirstParamType("setProxy", bean);
        }

        try {
            if(proxyType!=null) {
                BeanHelper.invokeBeanMethod(bean,
                                            null,
                                            setProxyMethodName,
                                            new Class[]{proxyType},
                                            new Object[]{proxy});
            }
        } catch (Exception e) {
            logger.info("setting bean proxy");
            throw new RuntimeException("could not set bean proxy", e);
        }
        return (proxy);
    }

    public static Object getCustomProxy(Object bean, Remote remoteRef) {
        Object proxy = null;
        Class proxyType;
        String createProxyMethodName="createProxy";

        /* First check if the @CreateProxy annotation is declared */
        Method createProxy = BeanHelper.getAnnotatedMethod(bean,
                                                           CreateProxy.class);
        if(createProxy!=null) {
            proxyType = BeanHelper.getMethodFirstParamType(createProxy);
            createProxyMethodName = createProxy.getName();
        } else {
            /* Else, if the createProxy method is declared, invoke it */
            proxyType = BeanHelper.getMethodFirstParamType("createProxy", bean);
        }

        /* If declared, invoke the createProxy method */
        try {
            Object beanProxy = null;
            if(proxyType!=null)
                beanProxy = BeanHelper.invokeBeanMethod(bean,
                                                        null,
                                                        createProxyMethodName,
                                                        new Class[]{proxyType},
                                                        new Object[]{remoteRef});
            if(beanProxy!=null) {
                /* RIO-201 */
                if(beanProxy instanceof Service) {
                     proxy = beanProxy;
                } else {
                    Class[] interfaces = getInterfaceClasses(beanProxy.getClass());
                    proxy = BeanDelegator.getInstance(remoteRef,
                                                      beanProxy,
                                                      interfaces);
                }
            } /*else {
                proxy = super.createProxy();
            }*/
        } catch (Exception e) {
            logger.info("creating bean proxy");
            throw new RuntimeException("could not create bean proxy", e);
        }
        return proxy;
    }

}
