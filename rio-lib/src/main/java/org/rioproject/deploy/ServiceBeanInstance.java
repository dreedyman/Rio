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
package org.rioproject.deploy;

import com.sun.jini.proxy.MarshalledWrapper;
import net.jini.id.Uuid;
import net.jini.io.MarshalledInstance;
import org.rioproject.opstring.ServiceBeanConfig;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;

/**
 * This class indicates an instance of an instantiated ServiceBean
 *
 * @author Dennis Reedy
 */
public class ServiceBeanInstance implements Serializable {
    @SuppressWarnings("unused")
    static final long serialVersionUID = 1L;
    /**
     * Unique identifier for the ServiceBean
     */
    private Uuid sbID;
    /**
     * The Unique identifier of the ServiceBeanInstantiator
     */
    private Uuid instantiatorID;
    /**
     * MarshalledInstance representation of the Object used to communicate 
     * to the ServiceBean
     */
    private MarshalledInstance mi;
    /**
     * Object used to communicate to the ServiceBean
     */
    private transient Object service;
    /**
     * ServiceBeanConfig object for the ServiceBean
     */
    private ServiceBeanConfig sbConfig;
    /**
     * The name of the compute resource the service is executing on
     */
    private String hostName;
    /**
     * The IP address of the compute resource the service is executing on
     */
    private String hostAddress;
    /**
     * Whether to verify codebase integrity. 
     */
    private transient boolean verifyCodebaseIntegrity;

    /**
     * Create a ServiceBeanInstance
     * 
     * @param identifier Unique identifier for the service
     * @param mi MarshalledInstance of the service's proxy
     * @param sbConfig ServiceBeanConfig object for the service
     * @param hostName The hostName the service is executing on
     * @param instantiatorID The Unique identifier of the
     * <tt>ServiceBeanInstantiator</tt> that instantiated the service
     */
    public ServiceBeanInstance(Uuid identifier,
                               MarshalledInstance mi,
                               ServiceBeanConfig sbConfig,
                               /* Optional */
                               String hostName,
                               String hostAddress,
                               Uuid instantiatorID) {
        if(identifier==null)
            throw new IllegalArgumentException("identifier is null");
        if(mi==null)
            throw new IllegalArgumentException("mi is null");
        if(sbConfig==null)
            throw new IllegalArgumentException("sbConfig is null");
        sbID = identifier;
        this.mi = mi;
        this.sbConfig = sbConfig;
        this.hostName = hostName;
        this.hostAddress = hostAddress;
        this.instantiatorID = instantiatorID;
    }

    /**
     * Get the unique identifier for the ServiceBean
     *
     * @return Unique identifier for the ServiceBean
     */
    public Uuid getServiceBeanID() {
        return(sbID);
    }

    /**
     * Get the unique identifier for the ServiceBeanInstantiator
     *
     * @return Unique identifier for the ServiceBeanInstantiator
     */
    public Uuid getServiceBeanInstantiatorID() {
        return(instantiatorID);
    }

    /**
     * Get the MarshalledInstance
     *
     * @return The MarshalledInstance for the service proxy
     */
    public MarshalledInstance getMarshalledInstance() {
        return(mi);
    }

    /**
     * Get the object used to communicate to the ServiceBean
     * 
     * @return Object used to communicate to the ServiceBean
     *
     * @throws ClassNotFoundException If the class cannot be found when loading
     * the service proxy
     * @throws IOException As a result of de-marshalling the service proxy
     */
    public Object getService() throws IOException, ClassNotFoundException {
        if(service==null)
            service = mi.get(verifyCodebaseIntegrity);
        return(service);
    }

    /**
     * Set the ServiceBeanConfig for the ServiceBean
     * 
     * @param sbConfig The ServiceBeanConfig for the ServiceBean
     */
    public void setServiceBeanConfig(ServiceBeanConfig sbConfig) {
        if(sbConfig==null)
            throw new IllegalArgumentException("sbConfig is null");
        this.sbConfig = sbConfig;
    }

    /**
     * Get the ServiceBeanConfig for the ServiceBean
     * 
     * @return The ServiceBeanConfig for the ServiceBean
     */
    public ServiceBeanConfig getServiceBeanConfig() {
        return(sbConfig);
    }

    /**
     * Get the host name of the compute resource the service is executing on
     *
     * @return The host name of of the compute resource the service is
     * executing on. This value may be null
     */
    public String getHostName() {
        return hostName;
    }

    /**
     * Get the IP address of the compute resource the service is executing on
     * 
     * @return The IP address of of the compute resource the service is
     * executing on. This value may be null
     */
    public String getHostAddress() {
        return(hostAddress);
    }

    /**
     * Override hashCode to be the hash of the service identifier
     */
    public int hashCode() {
        return(sbID.hashCode());
    }

    /**
     * A ServiceBeanInstance is equal to another ServiceBeanInstance if the
     * service identifier properties are equal
     */
    public boolean equals(Object obj) {
        if(this == obj)
            return(true);
        if(!(obj instanceof ServiceBeanInstance))
            return(false);
        ServiceBeanInstance other = (ServiceBeanInstance)obj;
        return(sbID.equals(other.sbID));
    }

    public String toString() {
        String serviceClassName;
        try {
            getService();
            serviceClassName = service.getClass().getName();
        } catch (Exception e) {
            serviceClassName = "unknown : system error";
        }
        String instanceID = "<null>";
        if(sbConfig!=null) {
            Long id = sbConfig.getInstanceID();
            if(id!=null)
                instanceID = id.toString();
        }

        return("Instance=["+instanceID+"] "+
               "Proxy=["+serviceClassName+"] "+
               "ID=["+sbID.toString()+"] "+
               "HostAddress=["+(hostAddress==null?"null":hostAddress)+"]");
    }

    /* Set transient fields. */
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        verifyCodebaseIntegrity = MarshalledWrapper.integrityEnforced(in);
    }

}
