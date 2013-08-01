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
package org.rioproject.associations;

import net.jini.core.discovery.LookupLocator;
import org.rioproject.opstring.ClassBundle;
import org.rioproject.opstring.ServiceBeanConfig;

import java.io.Serializable;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

/**
 * The AssociationDescriptor defines the attributes needed to create, manage and
 * monitor <code>Association</code> instances.
 *
 * @author Dennis Reedy
 */
public class AssociationDescriptor implements Serializable {
    @SuppressWarnings("unused")
    static final long serialVersionUID = 1L;
    /**
     * If an AssociationDescriptor is created with a null for the name
     * property, the AssociationDescriptor name property will be set to this
     * value
     */
    public static final String NO_NAME="<no-name>";
    /**
     * The AssociationDescriptor type
     */
    private AssociationType type;
    /**
     * Array of public interface names the associated service implements
     */
    private String[] interfaceNames;
    /**
     * Name of the associated service
     */
    private String name;
    /**
     * Name of the associated service's OperationalString
     */
    private String opStringName;
    /**
     * The FaultDetectionHandler ClassBundle
     */
    private ClassBundle fdhBundle;
    /**
     * Associated service discovery groups
     */
    private String[] groups;
    /**
     * Associated service discovery locators
     */
    private LookupLocator[] lookupLocators;
    /**
     * Use the name in addition to public interfaces to track the service
     */
    private boolean matchOnName = false;
    /**
     * The property to set when the associated service is discovered, changed 
     * or broken. This provides support for association based dependency
     * injection
     */
    private String propertyName;
    /**
     * The proxy to create
     */
    private String proxyClass;
    public static final String JDK_PROXY="jdk";
    /**
     * The proxy type, default is JDK proxy
     */
    private String proxyType = JDK_PROXY;
    /**
     * The association match filter
     */
    private String associationMatchFilter;

    /**
     * The ServiceSelectionStrategy class
     */
    private String serviceStrategyClass;
    /**
     * Whether to lazily inject (inject when a service is discovered),
     * or to eagerly inject (inject immediately)
     */
    private boolean lazyInject = true;
    /**
     * The timeout value for service discovery
     */
    private long serviceDiscoveryTimeout;
    /**
     * The number of units service discovery timeout is for
     */
    private TimeUnit serviceDiscoveryTimeUnits = TimeUnit.MINUTES;

    /**
     * Create an AssociationDescriptor     
     */
    public AssociationDescriptor() {
        this(AssociationType.USES);
    }

    /**
     * Create an AssociationDescriptor
     *
     * @param type The AssociationType
     */
    public AssociationDescriptor(final AssociationType type) {
        this(type, null, null, null);
    }

    /**
     * Create an AssociationDescriptor
     *
     * @param type The AssociationType
     * @param name The name of the associated service, may be <code>null</code>
     */
    public AssociationDescriptor(final AssociationType type, final String name) {
        this(type, name, null, null);
    }

    /**
     * Create an AssociationDescriptor
     *
     * @param type The AssociationType
     * @param name The name of the associated service
     * @param opStringName The name of the OperationalString the
     * associated service is part of, may be <code>null</code>
     * @param propertyName The property to set when the associated service is
     * discovered, changed or broken. May be <code>null</code>
    */
    public AssociationDescriptor(final AssociationType type,
                                 final String name,
                                 final String opStringName,
                                 final String propertyName) {
        if(type==null)
            throw new IllegalArgumentException("type is null");
        this.type = type;
        this.name = name;
        this.opStringName = opStringName;
        this.propertyName = propertyName;
    }

    /**
     * Get the AssociationDescriptor type
     *
     * @return The AssociationType
     */
    public AssociationType getAssociationType() {
        return (type);
    }

    /**
     * Set the propertyName
     *
     * @param propertyName The propertyName to use to inject the Association
     */
    public void setPropertyName(final String propertyName) {
        this.propertyName = propertyName;
    }

    /**
     * Get the propertyName
     *
     * @return The propertyName to set when the associated service is
     * discovered, changed or broken. This provides support for association
     * based dependency injection. If the propertyName is null, dependency
     * injection will not be used with this association
     */
    public String getPropertyName() {
        return(propertyName);
    }

    /**
     * Set the associated service's name
     *
     * @param name The associated service's name. 
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Get the associated service's name
     *
     * @return The associated service's name. If the name is <tt>null</tt>, the
     * {@link AssociationDescriptor#NO_NAME} value is returned
     */
    public String getName() {
        return (name==null?NO_NAME:name);
    }

    /**
     * Set the matchOnName property
     *
     * @param matchOnName Whether the association should match on the service
     * name.
     *
     * @throws IllegalArgumentException If the AssociationDescriptor has it's
     * name property set to null and the matchOnName value is true
     */
    public void setMatchOnName(final boolean matchOnName) {
        if(matchOnName && name==null)
            throw new IllegalArgumentException("cannot match on a null name");
        this.matchOnName = matchOnName;
    }

    /**
     * If this method returns true then the name of the service is used in
     * addition to the interfaces implemented by the service or service proxy
     * to track service instances. If this method returns false, then only the
     * interfaces will be used.
     *
     * @return True to use the name returned by the <code>getName</code>
     * method
     */
    public boolean matchOnName() {
        return (matchOnName);
    }

    /**
     * Set the Array of public interface names the associated service
     * implements
     *
     * @param interfaces Array of public interface names the associated
     * service implements
     */
    public void setInterfaceNames(final String... interfaces) {
        if(interfaces == null)
            return;
        this.interfaceNames = new String[interfaces.length];
        System.arraycopy(interfaces, 0, interfaceNames, 0, interfaceNames.length);
    }

    /**
     * Get the the Array of public interfaces the associated service implements
     *
     * @return Array of public interfaces names the
     * associated service implements. If there are no public interfaces this
     * method will return a zero-length array
     */
    public String[] getInterfaceNames() {
        if(interfaceNames==null)
            return(new String[0]);
        String[] iNames = new String[interfaceNames.length];
        System.arraycopy(interfaceNames, 0, iNames, 0, interfaceNames.length);
        return (iNames);
    }

    /**
     * Get the associated service's OperationalString name
     *
     * @return The associated service's OperationalString name
     */
    public String getOperationalStringName() {
        return (opStringName);
    }

    /**
     * Set the associated service's OperationalString name
     *
     * @param opStringName The associated service's
     * OperationalString name
     */
    public void setOperationalStringName(final String opStringName) {
        this.opStringName = opStringName;
    }

    /**
     * Set the <code>FaultDetectionHandler</code> ClassBundle
     *
     * @param fdhBundle The {@link org.rioproject.opstring.ClassBundle} used to
     * create the fault detection handler
     */
    public void setFaultDetectionHandlerBundle(final ClassBundle fdhBundle) {
        this.fdhBundle = fdhBundle;
    }

    /**
     * Get a <code>FaultDetectionHandler</code> from the Configuration
     *
     * @return The ClassBundle providing attributes on the
     * FaultDetectionHandler to use
     */
    public ClassBundle getFaultDetectionHandlerBundle() {
        return (fdhBundle);
    }

    /**
     * Set the Lookup groups the used to discover the associated service. If
     * this parameter is not set (remains null), then no attempts will be
     * made via group discovery to discover lookup services the associated
     * service has registered
     *
     * @param groups Array of String group names whose members are
     * the lookup services to discover. Elements contained within the array may
     * be modified as follows:
     *
     * <ul>
     * <li>If the groups property is equivalent to the value of
     * LookupDiscovery.ALL_GROUPS, the value will be transformed into "all"
     * <li>If groups property is equivalent  to an empty string "", then the
     * value will be transformed to "public"
     * </ul>
     */
    public void setGroups(final String... groups) {
        if(groups == ServiceBeanConfig.ALL_GROUPS)
            this.groups = new String[]{"all"};
        else {
            this.groups = new String[groups.length];
            System.arraycopy(groups, 0, this.groups, 0, groups.length);
            for(int i=0; i<this.groups.length; i++) {
                if(this.groups[i].equals(""))
                    this.groups[i] = "public";
            }
        }
    }

    /**
     * This method provides the ability to set the array of locators that this
     * ServiceBean would like to join. If this parameter is not set, then no
     * attempts will be made via unicast discovery
     *
     * @param lookupLocators Array of LookupLocator instances
     */
    public void setLocators(final LookupLocator... lookupLocators) {
        this.lookupLocators = lookupLocators;
   }

    /**
     * Returns an array consisting of the names of the groups whose members are
     * the lookup services to discover.
     * <ul>
     * <li>If the groups property is null or has a zero length, then the
     * returned value is LookupDiscovery.NO_GROUPS
     * <li>If an element has the value of "all", that value will be transformed
     * to LookupDiscovery.ALL_GROUPS
     * <li>If an element has the value of "public", that value is transformed
     * to an empty string ""
     * </ul>
     *
     * @return String array of groups
     */
    public String[] getGroups() {
        String[] adjustedGroups = groups;
        if(groups == null || groups.length == 0)
            adjustedGroups = ServiceBeanConfig.NO_GROUPS;
        if(adjustedGroups.length > 0) {
            if(adjustedGroups.length == 1 && adjustedGroups[0].equals("all")) {
                adjustedGroups = ServiceBeanConfig.ALL_GROUPS;
            } else {
                for(int i = 0; i < adjustedGroups.length; i++) {
                    if(adjustedGroups[i].equals("public"))
                        adjustedGroups[i] = "";
                }
            }
        }
        return (adjustedGroups);
    }

    /**
     * Returns an array consisting of instances of
     * {@link net.jini.core.discovery.LookupLocator} in which each
     * instance corresponds to a specific lookup service to discover
     *
     * @return An array of LookupLocator instances. If there are no
     * LookupLocator instances, return a zero length array
     */
    public LookupLocator[] getLocators() {
        if(lookupLocators == null)
            return (new LookupLocator[0]);
        LookupLocator[] locators = new LookupLocator[lookupLocators.length];
        System.arraycopy(lookupLocators, 0, locators, 0, lookupLocators.length);
        return (locators);
    }

    /**
     * Set the proxy factory
     *
     * @param proxyClass The class name of the <code>AssociationProxy</code> to use
     */
    public void setProxyClass(final String proxyClass) {
        this.proxyClass = proxyClass;
    }

    /**
     * Get the proxy factory
     *
     * @return The classname of the <code>AssociationProxy</code> to use
     */
    public String getProxyClass() {
        return proxyClass;
    }

    /**
     * Set the proxy type to create for the Association
     *
     * @param proxyType The type of proxy to generate. May be null. If not
     * null, must be {@link AssociationDescriptor#JDK_PROXY} .
     *
     * @throws IllegalArgumentException if the proxyType is not {@link AssociationDescriptor#JDK_PROXY}
     */
    @Deprecated
    @SuppressWarnings("unused")
    public void setProxyType(final String proxyType) {
        if(proxyType==null) {
            this.proxyType = null;
        } else {
            if(proxyType.equals(JDK_PROXY)) {
                this.proxyType = proxyType;
            } else {
                throw new IllegalArgumentException("unknown proxy type: "+proxyType);
            }
        }
    }

    /**
     * Get the proxy type to create for the Association
     *
     * @return The type of proxy to generate
     */
    public String getProxyType() {
        return proxyType;
    }

    /**
     * Set the association match filter class name
     *
     * @param associationMatchFilter The classname of the <code>AssociationMatchFilter</code>
     */
    public void setAssociationMatchFilter(final String associationMatchFilter) {
        this.associationMatchFilter = associationMatchFilter;
    }

    /**
     * Get the association match filter class name
     *
     * @return The classname of the <code>AssociationMatchFilter</code>
     */
    public String getAssociationMatchFilter() {
        return associationMatchFilter;
    }

    /**
     * Set the <code>ServiceSelectionStrategy</code> to use when selecting associated services.
     *
     * @param serviceStrategyClass The classname of the ServiceSelectionStrategy.
     * The ServiceSelectionStrategy must have a zero-arg constructor and
     * implement <code>ServiceSelectionStrategy</code>
     */
    public void setServiceSelectionStrategy(final String serviceStrategyClass) {
        this.serviceStrategyClass = serviceStrategyClass;
    }

    /**
     * Get the {@code org.rioproject.associations.ServiceSelectionStrategy}
     * classname to use for selecting associated services.
     *
     * @return The classname of the ServiceSelectionStrategy to create. 
     * The ServiceSelectionStrategy must have a zero-arg constructor and
     * implement {@code org.rioproject.associations.ServiceSelectionStrategy}
     */
    public String getServiceSelectionStrategy() {
        return serviceStrategyClass;
    }

    /**
     * Get the association injection style
     *
     * @return If the association is to be injected, whether to inject the
     * association lazily (when the service is discovered), or eagerly (inject
     * immediately).
     */
    public boolean isLazyInject() {
        return lazyInject;
    }

    /**
     * Set the association injection style
     *
     * @param lazyInject If true, the association will be injected when the
     * service is discovered. If false the association will be injected
     * immediately.
     */
    public void setLazyInject(final boolean lazyInject) {
        this.lazyInject = lazyInject;
    }

    /**
     * Set the timeout for service discovery
     *
     * @param serviceDiscoveryTimeout The service discovery timeout.
     */
    public void setServiceDiscoveryTimeout(final long serviceDiscoveryTimeout) {
        this.serviceDiscoveryTimeout = serviceDiscoveryTimeout;
    }

    /**
     * Get the service discovery timeout
     *
     * @return The value to use for the service discovery timeout
     */
    public long getServiceDiscoveryTimeout() {
        return serviceDiscoveryTimeout;
    }

    /**
     * Set the unit of time for the service discovery timeout
     *
     * @param serviceDiscoveryTimeUnits The unit of time for the service
     * discovery timeout
     */
    public void setServiceDiscoveryTimeUnits(final TimeUnit serviceDiscoveryTimeUnits) {
        if(serviceDiscoveryTimeUnits==null)
            throw new IllegalArgumentException("serviceDiscoveryTimeUnits is null");
        if(serviceDiscoveryTimeUnits.ordinal()<TimeUnit.MILLISECONDS.ordinal())
            throw new IllegalArgumentException("serviceDiscoveryTimeUnits cannot be smaller then MILLISECONDS");
        this.serviceDiscoveryTimeUnits = serviceDiscoveryTimeUnits;
    }

    /**
     * Get the unit of time for the service discovery timeout
     *
     * @return The unit of time for the service discovery timeout
     */
    public TimeUnit getServiceDiscoveryTimeUnits() {
        return serviceDiscoveryTimeUnits;
    }

    /**
     * Creates a "uses" AssociationDescriptor for a service, matching on the
     * service name
     *
     * @param name The service name
     * @param serviceClass The service's exported proxy class
     * @param groups Discovery groups to use
     *
     * @return An AssociationDescriptor
     */
    public static AssociationDescriptor create(final String name, final Class serviceClass, final String... groups) {
        return create(name, null, serviceClass, AssociationType.USES, groups);
    }

    /**
     * Creates a "uses" AssociationDescriptor for a service, matching on the
     * service name
     *
     * @param name The service name
     * @param setter The setter property to use when injecting
     * @param serviceClass The service's exported proxy class
     * @param groups Discovery groups to use
     *
     * @return An AssociationDescriptor
     */
    public static AssociationDescriptor create(final String name, final String setter, final Class serviceClass, final String... groups) {
        return create(name, setter, serviceClass, AssociationType.USES, groups);
    }

    /**
     * Creates an AssociationDescriptor for a service, matching on the service name
     *
     * @param name The service name
     * @param setter The setter property to use when injecting
     * @param serviceClass The service's exported proxy class
     * @param type The type of Association to create
     * @param groups Discovery groups to use
     *
     * @return An AssociationDescriptor
     */
    public static AssociationDescriptor create(final String name,
                                               final String setter,
                                               final Class serviceClass,
                                               final AssociationType type,
                                               final String... groups) {
        AssociationDescriptor ad = new AssociationDescriptor(type, name);
        ad.setInterfaceNames(serviceClass.getName());
        if(setter!=null)
            ad.setPropertyName(setter);
        ad.setGroups(groups);
        ad.setMatchOnName(true);
        return ad;
    }

    /**
     * Override hashCode to return the hashCode of the name, opStringName
     * and {@link AssociationType} hashCodes
     */
    public int hashCode() {
        int hc = 17;
        hc = 37*hc+type.hashCode();
        hc = 37*hc+getName().hashCode();
        if(opStringName!=null)
            hc = 37*hc+opStringName.hashCode();
        if(propertyName!=null)
            hc = 37*hc+propertyName.hashCode();
        if(interfaceNames !=null)
            hc = Arrays.hashCode(interfaceNames);
        return (hc);
    }

    /**
     * An AssociationDescriptor is equal to another AssociationDescriptor if
     * their name, opStringName and
     * {@link AssociationType} attributes are equal
     */
    public boolean equals(final Object obj) {
        if(this == obj)
            return (true);
        if(!(obj instanceof AssociationDescriptor))
            return (false);
        AssociationDescriptor that = (AssociationDescriptor)obj;
        if(this.type.equals(that.type) && this.getName().equals(that.getName())) {
            boolean matched = false;
            /* Check propertyName attributes */
            if(this.opStringName!=null &&
               that.opStringName!=null) {
                if(this.opStringName.equals(
                    that.opStringName))
                    matched = true;
            }
            if(this.opStringName==null &&
               that.opStringName==null)
                matched = true;

            if(!matched)
                return(matched);

            if (!Arrays.equals(this.interfaceNames, that.interfaceNames))
                return false;

            /* Check propertyName attributes */
            if(this.propertyName!=null && that.propertyName!=null) {
                if(this.propertyName.equals(that.propertyName))
                    return(true);
            }
            if(this.propertyName==null && that.propertyName==null)
                return(true);

        }
        return (false);
    }

    /**
     * Override toString
     */
    public String toString() {
        StringBuilder buffer = new StringBuilder();
        if(interfaceNames!=null) {
            for(int i=0; i<interfaceNames.length; i++) {
                if(i>0)
                    buffer.append(", ");
                buffer.append(interfaceNames[i]);
            }
        } else {
            buffer.append("<null>");
        }
        String iFaces = buffer.toString();
        buffer.delete(0, buffer.length());
        if(groups==null) {
            buffer.append("<null>");
        } else {
            for(int i=0; i<groups.length; i++) {
                if(i>0)
                    buffer.append(", ");
                buffer.append(groups[i]);
            }
        }
        String gps = buffer.toString();
        String fdh = "<null>";
        if(fdhBundle!=null)
            fdh = fdhBundle.getClassName();
        String ops="<null>";
        if(opStringName!=null)
            ops = opStringName;
        return("Type="+type.toString()+", "+
               "Name="+getName()+", "+
               "Interfaces="+iFaces+", "+
               "Groups="+gps+", "+
               "MatchOnName="+matchOnName+", "+
               "OperationalString="+ops+", "+
               "Property="+propertyName+", "+
               "associationMatchFilter="+associationMatchFilter+", "+
               "proxyClass="+proxyClass+", "+
               "proxyType="+proxyType+", "+
               "serviceStrategyClass="+serviceStrategyClass+", "+
               "lazyInject="+lazyInject+", "+
               "serviceDiscoveryTimeout="+serviceDiscoveryTimeout+", "+
               "serviceDiscoveryTimeUnits="+serviceDiscoveryTimeUnits+", "+
               "FDH="+fdh);
    }


}
