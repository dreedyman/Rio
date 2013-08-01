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
package org.rioproject.watch;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Collection;

/**
 * A remote WatchDataReplicator.
 */
public interface RemoteWatchDataReplicator extends Remote {
    /**
     * Replicate a {@link org.rioproject.watch.Calculable}
     *
     * @param c The Calculable to replicate
     *
     * @throws RemoteException If communication errors occur
     */
    void replicate(Calculable c) throws RemoteException;

    /**
     * Replicate a collection of {@link org.rioproject.watch.Calculable}s
     *
     * @param c The collection of {@link org.rioproject.watch.Calculable}s to
     * replicate
     *
     * @throws RemoteException If communication errors occur
     */
    void bulkReplicate(Collection<Calculable> c) throws RemoteException;
}
