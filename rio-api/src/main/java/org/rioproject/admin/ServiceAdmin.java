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
package org.rioproject.admin;

import com.sun.jini.admin.DestroyAdmin;
import net.jini.admin.JoinAdmin;
import net.jini.core.lookup.ServiceRegistrar;
import org.rioproject.admin.ServiceBeanAdmin;
import org.rioproject.admin.ServiceBeanControl;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * The ServiceAdmin provides a common interface, aggregating all admin type 
 * interfaces into a single interface
 *
 * @author Dennis Reedy
 */
public interface ServiceAdmin extends Remote, ServiceBeanAdmin, ServiceBeanControl, JoinAdmin, DestroyAdmin {
    /**
     * Returns an array of instances of ServiceRegistrar, each corresponding to a
     * lookup service with which the service is currently registered (joined)
     *
     * @return An array of ServiceRegistrar instances
     * @throws RemoteException If communication errors occur
     */
    ServiceRegistrar[] getJoinSet() throws RemoteException;
}
