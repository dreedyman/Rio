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
package org.rioproject.associations;

import org.rioproject.associations.strategy.FailOver;

import java.lang.annotation.*;
import java.util.concurrent.TimeUnit;


/**
 * <p>
 * Annotation to indicate an association to another service.
 * </p>
 * <p>
 * Example: You can declare an association to another service as follows:
 * </p>
 *
 * <pre class="code">
 * {@link ServiceAssociation @ServiceAssociation}(name=&quot;Foo&quot;, serviceType=&quot;Foo.class&quot;)
 * setFoo(Foo foo) {
 *     // ...
 * }
 * </pre>
 * <p>
 *  Note: {@link ServiceAssociation @ServiceAssociation} can be applied at either the
 * method or field level.
 * </p>
 *
 * @see Association
 * @see AssociationManagement
 * @see AssociationDescriptor
 * 
 * @author Dennis Reedy
 */
@Target({ElementType.METHOD, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ServiceAssociation {
    /**
     * The name of the service to match.
     *
     * @return The name of the service to match. If the default <tt>""</tt> is
     * used, then the service will be matched using only the {@link #serviceType}.
     */
    String name() default "";

    /**
     * The exported service interface type to match.
     *
     * @return The service's exported service interface type to match.
     */
    Class serviceType() ;   

    /**
     * The type of association to declare.
     * The {@link ServiceAssociation @ServiceAssociation} may have one of the
     * following types:
     *
     * <ul>
     * <li>{@link AssociationType#USES}
     * <li>{@link AssociationType#COLOCATED}
     * <li>{@link AssociationType#REQUIRES}
     * <li>{@link AssociationType#ISOLATED}
     * <li>{@link AssociationType#OPPOSED}
     * </ul>
     * @return The type of the association. 
     */
    AssociationType type() default AssociationType.USES;

    /**
     * The dynamic proxy to create. When association injection occurs the
     * injected service is actually a generated dynamic proxy. The dynamic
     * proxy is used as a way to manage a collection of discovered services. 
     *
     * @return The class to create for association, defaults to
     * {@link AssociationProxySupport}
     *
     * @see AssociationProxyType
     */
    Class proxy() default AssociationProxySupport.class;

    /**
     * A strategy that determines how services in the collection of
     * discovered services are invoked. The current service selection
     * strategies are:
     * <ul>
     * <li>{@link org.rioproject.associations.strategy.FailOver}
     * <li>{@link org.rioproject.associations.strategy.RoundRobin}
     * <li>{@link org.rioproject.associations.strategy.Utilization}
     * </ul>
     *
     * <p>Note: all service selection strategies will also
     * prefer local services over remote services.
     *
     * <p>The Fail-Over strategy will invoke the first service in it's collection
     * for each method invocation until that service is no longer reachable.
     * If the associated service is unavailable, the fail-over strategy invokes
     * the next service in it's list.
     *
     * <p>The Round Robin strategy alternates selection of services from the
     * association, alternating the selection of associated services using a
     * round-robin approach.
     *
     * <p>The Utilization strategy is a round-robin selector that selects
     * services running on compute resources whose system resources are not
     * depleted. System resource depletion is determined by 
     * {@link org.rioproject.system.MeasuredResource} provided as part of
     * the {@link org.rioproject.system.ComputeResourceUtilization}
     * object returned as part of the deployment map. If any of a Cybernode's
     * resources are depleted, the service hosted in that Cybernode will not
     * be invoked. This is of particular use in cases where out of memory
     * conditions may occur. Using the utilization strategy a service running
     * in a memory constrained Cybernode will not be invoked until the JVM
     * performs garbage collection and memory is reclaimed.
     *
     * @return The class to use for service selection. 
     */
    Class strategy() default FailOver.class;

    /**
     * Declare how the association proxy is injected. When the association proxy
     * is injected <i>eagerly</i>, it is injected immediately upon service
     * creation.
     *
     * <p>If the association proxy is injected <i>lazily</i>, it is injected
     * when associated services are discovered.
     *
     * <p>Care must be taken when using the <i>eager</i> approach, the proxy
     * may not have discovered services <i>backing</i> the generated
     * proxy.
     *
     * <p>If you configure that the association proxy is injected eagerly,
     * you really need to consider declaring a service discovery timeout.
     *
     * @return Whether to inject eagerly or lazily. 
     */
    boolean eager() default false;

    /**
     * How long to wait for a service before throwing a
     * {@link java.rmi.RemoteException}. In a typical distributed environment,
     * if there are no discovered (available) services and a remote method
     * invocation is attempted on a service, a <tt>RemoteException</tt> is thrown.
     *
     * <p>By declaring the <tt>timeout</tt> value below the dynamic proxy will
     * wait the declared amount of time and wait for a service to become
     * available to make the invocation.
     *
     * @return The amount of time to wait for service discovery if no services
     * are available.
     */
    long timeout() default 0;

    /**
     * The {@link #timeout} unit of time for service discovery timeout.
     * 
     * @return The unit of time that the {@link #timeout} is configured for.
     */
    TimeUnit timeoutUnits() default TimeUnit.SECONDS;
}

