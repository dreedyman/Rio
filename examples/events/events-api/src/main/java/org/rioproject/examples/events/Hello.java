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
package org.rioproject.examples.events;

import java.rmi.RemoteException;

/**
 *  Interface for the Hello example
 */
public interface Hello  {
    /**
     * Say hello to the service
     *
     * @param message A hello message
     *
     * @throws RemoteException If there are communication issues
     */
    void sayHello(String message) throws RemoteException;


    /**
     * Each time a hello message is sent to the service it will notify
     * registered consumers. This method will return the number of notifications
     * the service has sent.
     *
     * @return The number of notifications the service has sent.
     *  
     * @throws RemoteException If there are communication issues
     */
    int getNotificationCount() throws RemoteException;
}
