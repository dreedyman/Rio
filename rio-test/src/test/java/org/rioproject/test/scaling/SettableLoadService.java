/*
 * Copyright 2009 the original author or authors.
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
package org.rioproject.test.scaling;

import org.rioproject.resources.servicecore.Service;

import java.rmi.RemoteException;


/**
 * Represents a service for which a client can change the load
 * programmatically.
 */
public interface SettableLoadService extends Service {

    /**
     * Sets the service load.
     *
     * @param load the load.
     * @throws java.rmi.RemoteException if comms errors
     */
    void setLoad(double load) throws RemoteException;
}
