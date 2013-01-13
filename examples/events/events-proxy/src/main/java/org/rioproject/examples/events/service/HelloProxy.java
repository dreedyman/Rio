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
package org.rioproject.examples.events.service;

import net.jini.id.ReferentUuid;
import net.jini.id.Uuid;
import org.rioproject.examples.events.Hello;

import java.io.Serializable;
import java.rmi.RemoteException;

/**
 * A proxy example
 */
public final class HelloProxy implements Hello, ReferentUuid, Serializable {
    static final long serialVersionUID = 1L;
    private final Hello hello;
    private final Uuid uuid;

    /**
     * Creates a Hello proxy, returning an instance that implements
     * RemoteMethodControl if the server does too.
     *
     * @param hello - The Hello server
     * @param id - The Uuid of the Hello
     * @return An instance of the HelloProxy
     */
    static HelloProxy getInstance(Hello hello, Uuid id) {
        return (new HelloProxy(hello, id));
    }

    /*
     * Private constructor
     */
    private HelloProxy(Hello hello, Uuid uuid) {
        this.hello = hello;
        this.uuid = uuid;
    }

    /* -------- Implement Hello methods -------- */
    /**
     * Gets the hello world string from the Hello JSB
     */
    public void sayHello(String message) throws RemoteException {
        long startTime = System.currentTimeMillis();
        hello.sayHello(message);
        long elapsed = (System.currentTimeMillis() - startTime);
        System.out.println("Elapsed time="+elapsed+" ms");
    }

    public int getNotificationCount() throws RemoteException {
        return hello.getNotificationCount(); 
    }

    public boolean equals(Object o) {
        if (this == o)
            return true;

        if (!(o instanceof ReferentUuid))
            return false;

        ReferentUuid that = (ReferentUuid) o;

        if (uuid != null ?
            !uuid.equals(that.getReferentUuid()) :
            that.getReferentUuid() != null)
            return false;

        return true;
    }

    public int hashCode() {
        return (uuid != null ? uuid.hashCode() : 0);
    }

    public Uuid getReferentUuid() {
        return uuid;
    }
}
