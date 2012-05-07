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
package org.rioproject.resources.servicecore;

import net.jini.admin.Administrable;
import net.jini.config.ConfigurationException;
import net.jini.core.event.EventRegistration;
import net.jini.core.event.RemoteEventListener;
import net.jini.core.event.UnknownEventException;
import net.jini.core.lease.Lease;
import net.jini.core.lease.LeaseDeniedException;
import net.jini.id.ReferentUuid;
import net.jini.id.ReferentUuids;
import net.jini.id.Uuid;
import org.rioproject.admin.MonitorableService;
import org.rioproject.event.EventDescriptor;
import org.rioproject.event.EventProducer;
import org.rioproject.watch.WatchDataSource;
import org.rioproject.watch.Watchable;

import java.io.*;
import java.rmi.MarshalledObject;
import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * Defines an abstract class that supplies basic referent UUID and serialization
 * behavior for Service proxies.
 *
 * @author Dennis Reedy
 */
public abstract class AbstractProxy implements ReferentUuid, Service, Serializable {
    private static final long serialVersionUID = 2L;
    /** The server */
    final protected Remote server;
    /** The unique identifier for this proxy */
    final protected Uuid uuid;

    public AbstractProxy(Remote server, Uuid uuid) {
        if(server == null) {
            throw new IllegalArgumentException("server cannot be null");
        } else if(uuid == null) {
            throw new IllegalArgumentException("uuid cannot be null");
        }
        this.server = server;
        this.uuid = uuid;
    }

    /**
     * Proxies for servers with the same uuid have the same hash code.
     */
    public int hashCode() {
        return (uuid.hashCode());
    }

    /*
     * Proxies for servers with the same <code>uuid</code> are considered
     * equal.
     */
    public boolean equals(Object o) {        
        return (ReferentUuids.compare(this, o));
    }

    /*
     * When an instance of this class is deserialized, this method is
     * automatically invoked. This implementation of this method validates the
     * state of the deserialized instance.
     * 
     * @throws InvalidObjectException if the state of the
     * deserialized instance of this class is found to be invalid.
     */
    private void readObject(ObjectInputStream s) throws IOException, ClassNotFoundException {
        s.defaultReadObject();
        /* Verify server */
        if(server == null) {
            throw new InvalidObjectException("AbstractProxy.readObject failure - server field is null");
        }
        /* Verify uuid */
        if(uuid == null) {
            throw new InvalidObjectException("AbstractProxy.uuid failure - uuid field is null");
        }
    }

    /**
     * During deserialization of an instance of this class, if it is found that
     * the stream contains no data, this method is automatically invoked.
     * Because it is expected that the stream should always contain data, this
     * implementation of this method simply declares that something must be
     * wrong.
     * 
     * @throws InvalidObjectException to indicate that there was
     * no data in the stream during de-serialization of an instance of this
     * class; declaring that something is wrong.
     */
    private void readObjectNoData() throws ObjectStreamException {
        throw new InvalidObjectException("No data found when attempting to de-serialize AbstractProxy instance");
    }

    /* -------- Implement org.rioproject.admin.MonitorableService methods -------- */
    /** @see org.rioproject.admin.MonitorableService#ping */
    public void ping() throws RemoteException {
        ((MonitorableService)server).ping();
    }

    /** @see org.rioproject.admin.MonitorableService#monitor */
    public Lease monitor(long duration) throws LeaseDeniedException, RemoteException {
        return (((MonitorableService)server).monitor(duration));
    }

    /** @see org.rioproject.admin.MonitorableService#startHeartbeat */
    public void startHeartbeat(String[] configArgs) throws ConfigurationException, RemoteException {
        ((MonitorableService)server).startHeartbeat(configArgs);
    }

    /* -------- Implement org.rioproject.event.EventProducer methods -------- */
    /** @see org.rioproject.event.EventProducer#register */
    public EventRegistration register(EventDescriptor descriptor,
                                      RemoteEventListener listener,
                                      MarshalledObject handback, long duration)
            throws LeaseDeniedException, UnknownEventException, RemoteException {
        return (((EventProducer)server).register(descriptor, listener, handback, duration));
    }

    /* -------- Implement org.rioproject.watch.Watchable methods -------- */
    /** @see org.rioproject.watch.Watchable#fetch */
    public WatchDataSource[] fetch() throws RemoteException {
        return (((Watchable)server).fetch());
    }

    /** @see org.rioproject.watch.Watchable#fetch */
    public WatchDataSource fetch(String id) throws RemoteException {
        return (((Watchable)server).fetch(id));
    }

    /* -------- Implement net.jini.admin.Administrable methods -------- */
    /** @see net.jini.admin.Administrable#getAdmin */
    public Object getAdmin() throws RemoteException {
        return (((Administrable)server).getAdmin());
    }

    /* -------- Implement net.jini.id.ReferentUuid methods -------- */
    /** @see net.jini.id.ReferentUuid#getReferentUuid */
    public Uuid getReferentUuid() {
        return (uuid);
    }
}
