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
package org.rioproject.start;

import com.sun.jini.start.NonActivatableServiceDescriptor;
import com.sun.jini.start.ServiceDescriptor;
import net.jini.config.Configuration;
import net.jini.config.ConfigurationException;
import net.jini.config.ConfigurationProvider;
import net.jini.config.EmptyConfiguration;
import org.rioproject.config.RioProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.security.auth.Subject;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;
import java.rmi.RMISecurityManager;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * This class provides the main routine for starting shared groups,
 * non-activatable services, and activatable services.
 * <p/>
 * The following implementation-specific items are discussed below: <ul> <li><a
 * href="#configEntries">Configuring ServiceStarter</a> </ul>
 * <p/>
 * <a name="configEntries"> <h3>Configuring ServiceStarter</h3> </a>
 * <p/>
 * This implementation of <code>ServiceStarter</code> supports the following
 * configuration entries, with component <code>org.rioproject.start</code>:
 * <p/>
 * <table summary="Describes the loginContext configuration entry" border="0"
 * cellpadding="2"> <tr valign="top"> <th scope="col" summary="layout"> <font
 * size="+1">&#X2022;</font> <th scope="col" align="left" colspan="2"> <font
 * size="+1"><code> loginContext</code></font> <tr valign="top"> <td> &nbsp <th
 * scope="row" align="right"> Type: <td> {@link javax.security.auth.login.LoginContext}
 * <tr valign="top"> <td> &nbsp <th scope="row" align="right"> Default: <td>
 * <code>null</code> <tr valign="top"> <td> &nbsp <th scope="row" align="right">
 * Description: <td> If not <code>null</code>, specifies the JAAS login context
 * to use for performing a JAAS login and supplying the {@link
 * javax.security.auth.Subject} to use when running the service starter. If
 * <code>null</code>, no JAAS login is performed. </table>
 * <p/>
 * <table summary="Describes the serviceDescriptors configuration entry"
 * border="0" cellpadding="2"> <tr valign="top"> <th scope="col"
 * summary="layout"> <font size="+1">&#X2022;</font> <th scope="col"
 * align="left" colspan="2"> <font size="+1"><code> serviceDescriptors</code></font>
 * <tr valign="top"> <td> &nbsp <th scope="row" align="right"> Type: <td> {@link
 * ServiceDescriptor}[] <tr valign="top"> <td> &nbsp <th scope="row"
 * align="right"> Default: no default <tr valign="top"> <td> &nbsp <th
 * scope="row" align="right"> Description: <td> Array of service descriptors to
 * start. </table>
 * <p/>
 * <p/>
 *
 * @author Sun Microsystems, Inc.
 * @author Dennis Reedy
 */
public class ServiceStarter {
    /**
     * Component name for service starter configuration entries
     */
    static final String COMPONENT = ServiceStarter.class.getPackage().getName();
    /**
     * Configure logger
     */
    static final Logger logger = LoggerFactory.getLogger(COMPONENT);
    /**
     * Array of strong references to transient services
     */
    private static final List<Object> transientServiceRefs = new ArrayList<Object>();
    /**
     * Object returned by {@link ServiceStarter#start}
     */
    public static class ServiceReference {
        /** The reference to the proxy of the created service */
        public final Object proxy;
        /** The reference to the implementation of the created service */
        public final Object impl;
        /** Constructs an instance of this class.
         * @param impl reference to the implementation of the created service
         * @param proxy reference to the proxy of the created service
         */
        public ServiceReference(Object impl, Object proxy) {
            this.proxy = proxy;
            this.impl = impl;
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder();
            sb.append("ServiceReference");
            sb.append("{proxy=").append(proxy);
            sb.append(", impl=").append(impl);
            sb.append('}');
            return sb.toString();
        }
    }
    
    /**
     * Prevent instantiation
     */
    private ServiceStarter() {
    }

    /**
     * Trivial class used as the return value by the <code>create</code>
     * methods. This class aggregates the results of a service creation attempt:
     * proxy (if any), exception (if any), associated descriptor object.
     */
    private static class Result {
        public final Object result;
        public final Exception exception;
        public final ServiceDescriptor descriptor;

        /**
         * Trivial constructor. Simply assigns each argument to the appropriate
         * field.
         * @param d The Associated <code>ServiceDescriptor</code> object used
         * to create the service instance
         * @param o The service proxy object, if any.
         * @param e The service creation exception, if any.
         */
        Result(final ServiceDescriptor d, final Object o, final Exception e) {
            descriptor = d;
            result = o;
            exception = e;
        }

        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append(this.getClass()).append(":[descriptor=").append(descriptor).append(", ");
            builder.append("result=").append(result).append(", exception=").append(exception).append("]");
            return builder.toString();
        }
    }

    /**
     * Generic service creation method that attempts to login via the provided
     * <code>LoginContext</code> and then call the <code>create</code> overload
     * without a login context argument.
     *
     * @param descs The <code>ServiceDescriptor[]</code> that contains the
     * descriptors for the services to start.
     * @param config The associated <code>Configuration</code> object used to
     * customize the service creation process.
     * @param loginContext The associated <code>LoginContext</code> object used
     * to login/logout.
     * @return Returns a <code>Result[]</code> that is the same length as
     *         <code>descs</code>, which contains the details for each service
     *         creation attempt.
     *
     * @throws Exception If there was a problem logging in/out or a problem
     * creating the service.
     * @see Result
     * @see ServiceDescriptor
     * @see net.jini.config.Configuration
     * @see javax.security.auth.login.LoginContext
     */

    private static Result[] createWithLogin(final ServiceDescriptor[] descs,
                                            final Configuration config,
                                            final LoginContext loginContext) throws Exception {
        loginContext.login();
        Result[] results = null;
        try {
            results = Subject.doAsPrivileged(loginContext.getSubject(),
                new PrivilegedExceptionAction<Result[]>() {
                    public Result[] run() throws Exception {
                        return create(descs, config);
                    }
                },
                null);
        } catch (PrivilegedActionException pae) {
            throw pae.getException();
        } finally {
            try {
                loginContext.logout();
            } catch (LoginException le) {
                logger.warn("service.logout.exception", le);
            }
        }
        return results;
    }

    /**
     * Generic service creation method that attempts to start the services
     * defined by the provided <code>ServiceDescriptor[]</code> argument.
     *
     * @param descs The <code>ServiceDescriptor[]</code> that contains the
     * descriptors for the services to start.
     * @param config The associated <code>Configuration</code> object used to
     * customize the service creation process.
     * @return Returns a <code>Result[]</code> that is the same length as
     *         <code>descs</code>, which contains the details for each service
     *         creation attempt.
     *
     * @throws Exception If there was a problem creating the service.
     * @see Result
     * @see ServiceDescriptor
     * @see net.jini.config.Configuration
     */
    private static Result[] create(final ServiceDescriptor[] descs, final Configuration config) throws Exception {
        List<Result> proxies = new ArrayList<Result>();
        logger.debug("Starting {} service(s)", descs.length);
        for (ServiceDescriptor desc : descs) {
            Object result = null;
            Exception problem = null;
            try {
                if (desc != null) {
                    result = desc.create(config);
                }
            } catch (Exception e) {
                problem = e;
            } finally {
                proxies.add(new Result(desc, result, problem));
            }
        }
        return proxies.toArray(new Result[proxies.size()]);
    }

    /**
     * Utility routine that sets a security manager if one isn't already
     * present.
     */
    synchronized static void ensureSecurityManager() {
        if (System.getSecurityManager() == null) {
            System.setSecurityManager(new RMISecurityManager());
        }
    }

    /**
     * Start services declared in a configuration. The <tt>args</t> argument is
     * passed directly to <tt>ConfigurationProvider.getInstance()</t> in order
     * to obtain a <tt>Configuration</tt> object.
     *
     * <p>This configuration object is then queried for the
     * <tt>org.rioproject.start.serviceDescriptors</tt> entry, which
     * is assumed to be a <tt>ServiceDescriptor[]</tt>.
     *
     * <p>The <tt>create()</tt> method is then called on each of the array
     * elements.
     *
     * @param descriptors {@code ServiceDescriptor}s to start.
     *
     * @return An immutable list of <tt>ServiceReference</tt> objects, one for
     * each transient service started. If there are no transient services
     * started, a zero-length list is returned. A new list is allocated each
     * time.
     *
     * @throws Exception If there are errors starting the services
     */
    public static List<ServiceReference> start(ServiceDescriptor... descriptors) throws Exception {
        Result[] results = create(descriptors, EmptyConfiguration.INSTANCE);
        checkResultFailures(results);
        List<ServiceReference> serviceRefs = getServiceReferences(results);
        return Collections.unmodifiableList(serviceRefs);
    }

    /**
     * Start services declared in a configuration. The <tt>args</t> argument is
     * passed directly to <tt>ConfigurationProvider.getInstance()</t> in order
     * to obtain a <tt>Configuration</tt> object.
     *
     * <p>This configuration object is then queried for the
     * <tt>org.rioproject.start.serviceDescriptors</tt> entry, which
     * is assumed to be a <tt>ServiceDescriptor[]</tt>.
     *
     * <p>The <tt>create()</tt> method is then called on each of the array
     * elements.
     *
     * @param args <tt>String[]</tt> passed to
     * <tt>ConfigurationProvider.getInstance()</tt> in order to obtain a
     * <tt>Configuration</tt> object.
     *
     * @return An immutable list of <tt>ServiceReference</tt> objects, one for
     * each transient service started. If there are no transient services
     * started, a zero-length list is returned. A new list is allocated each
     * time.
     *
     * @throws Exception If there are errors starting the services
     */
    public static List<ServiceReference> start(String... args) throws Exception {
        Result[] results = doStart(args);
        checkResultFailures(results);
        List<ServiceReference> serviceRefs = getServiceReferences(results);
        return Collections.unmodifiableList(serviceRefs);
    }

    /**
     * Start services declared in a configuration. The <tt>args</t> argument is
     * passed directly to <tt>ConfigurationProvider.getInstance()</t> in order
     * to obtain a <tt>Configuration</tt> object.
     *
     * <p>This configuration object is then queried for the
     * <tt>org.rioproject.start.serviceDescriptors</tt> entry, which
     * is assumed to be a <tt>ServiceDescriptor[]</tt>.
     *
     * <p>The <tt>create()</tt> method is then called on each of the array
     * elements.
     *
     * @param args <tt>String[]</tt> passed to
     * <tt>ConfigurationProvider.getInstance()</tt> in order to obtain a
     * <tt>Configuration</tt> object.
     *
     * @return An immutable list of <tt>ServiceReference</tt> objects, one for
     * each transient service started. If there are no transient services
     * started, a zero-length list is returned. A new list is allocated each
     * time.
     *
     * @throws Exception If there are errors starting the services
     */
    private static Result[] doStart(String... args) throws Exception {
        Configuration config = ConfigurationProvider.getInstance(args);
        logger.debug("Getting service descriptors to start");
        ServiceDescriptor[] descs =
            (ServiceDescriptor[])config.getEntry(COMPONENT, "serviceDescriptors", ServiceDescriptor[].class, null);
        if (descs == null || descs.length == 0) {
            logger.warn("service.config.empty");
            return new Result[0];
        }
        logger.debug("Obtained {} service descriptors to start", descs.length);
        LoginContext loginContext = (LoginContext)config.getEntry(COMPONENT, "loginContext", LoginContext.class, null);
        Result[] results;
        if (loginContext != null)
            results = createWithLogin(descs, config, loginContext);
        else
            results = create(descs, config);
        return results;
    }

    /*
    * Utility routine that maintains strong references to any transient
    * services in the provided <code>Result[]</code>. This prevents the
    * transient services from getting garbage collected.
    */
    private static List<ServiceReference> getServiceReferences(Result[] results) {
        List<ServiceReference> refs = new ArrayList<ServiceReference>();
        if (results.length == 0)
            return refs;
        for (Result result : results) {
            Class rDescClass = result.descriptor.getClass();
            if (result.result != null) {
                if(NonActivatableServiceDescriptor.class.equals(rDescClass)) {
                    NonActivatableServiceDescriptor.Created created =
                        ((NonActivatableServiceDescriptor.Created)result.result);
                    refs.add(new ServiceReference(created.impl, created.proxy));
                } else if(RioServiceDescriptor.class.equals(rDescClass)) {
                    RioServiceDescriptor.Created created =
                            ((RioServiceDescriptor.Created)result.result);
                        refs.add(new ServiceReference(created.impl, created.proxy));
                } else {
                    refs.add(new ServiceReference(result.result, null));
                }
            }
        }
        return refs;
    }

    /*
     * Utility routine that maintains strong references to any
     * transient services in the provided <code>Result[]</code>.
     * This prevents the transient services from getting garbage
     * collected.
     */
    private static void maintainNonActivatableReferences(Result[] results) {
        if (results.length == 0)
            return;
        for (Result result : results) {
            if (result != null && result.result != null &&
                NonActivatableServiceDescriptor.class.equals(result.descriptor.getClass())) {
                logger.trace("Storing ref to: {}", result.result);
                transientServiceRefs.add(result.result);
            }
        }
    }

    /*
     * Utility routine that prints out warning messages for each service
     * descriptor that produced an exception or that was null.
     */
    private static void checkResultFailures(Result[] results) {
        if (results.length == 0)
            return;
        for (int i = 0; i < results.length; i++) {
            if (results[i].exception != null) {
                logger.warn("service.creation.unknown", results[i].exception);
                logger.warn("service.creation.unknown.detail {} {}", i, results[i].descriptor);
            } else if (results[i].descriptor == null) {
                logger.warn("service.creation.null {}", i);
            }
        }
    }

    /**
     * The main method for the <code>ServiceStarter</code> application. The
     * <code>args</code> argument is passed directly to
     * <code>ConfigurationProvider.getInstance()</code> in order to obtain a
     * <code>Configuration</code> object.
     *
     * <p>This configuration object is then queried for the
     * <code>org.rioproject.start.serviceDescriptors</code> entry, which
     * is assumed to be a <code>ServiceDescriptor[]</code>.
     *
     * <p>The <code>create()</code> method is then called on each of the array
     * elements.
     *
     * @param args <code>String[]</code> passed to
     * <code>ConfigurationProvider.getInstance()</code> in order to obtain a
     * <code>Configuration</code> object.
     *
     * @see RioServiceDescriptor
     * @see net.jini.config.Configuration
     * @see net.jini.config.ConfigurationProvider
     */
    public static void main(String[] args) {
        LogManagementHelper.setup();
        logger.debug("Entering {}", ServiceStarter.class.getName());
        ensureSecurityManager();
        RioProperties.load();
        try {
            Result[] results = doStart(args);
            checkResultFailures(results);
            //TODO - kick off daemon thread to maintain refs via LifeCycle object
            maintainNonActivatableReferences(results);
        } catch (ConfigurationException cex) {
            logger.error("service.config.exception", cex);
        } catch (Exception e) {
            logger.error("service.creation.exception", e);
        }
    }

}
