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

import java.io.IOException;
import java.util.Collection;
import java.util.UUID;

/**
 * Proxy for a WatchDataReplicator to be used in conjunction with a
 * {@link org.rioproject.watch.RemoteWatchDataReplicator}.
 */
public class WatchDataReplicatorProxy extends QueuedReplicator {
    private final RemoteWatchDataReplicator backend;
    private final UUID uuid;

    public static WatchDataReplicatorProxy getInstance(RemoteWatchDataReplicator backend,
                                                       UUID uuid) {
        return new WatchDataReplicatorProxy(backend, uuid);
    }

    private WatchDataReplicatorProxy(RemoteWatchDataReplicator backend,
                                     UUID uuid) {
        this.backend = backend;
        this.uuid = uuid;
    }

    protected void replicate(Calculable calculable) throws IOException {
        backend.replicate(calculable);
    }

    protected void bulkReplicate(Collection<Calculable> calculables) throws IOException {
        backend.bulkReplicate(calculables);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        WatchDataReplicatorProxy that = (WatchDataReplicatorProxy) o;
        return uuid.equals(that.uuid);
    }

    @Override
    public int hashCode() {
        return uuid.hashCode();
    }
}
