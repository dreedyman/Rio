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
package org.rioproject.monitor.tasks;

import org.rioproject.monitor.ProvisionMonitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is used as by a Thread to notify all known ProvisionMonitor
 * instances of back notifications
 */
public class PeerNotificationTask implements Runnable {
    ProvisionMonitor[] peers;
    ProvisionMonitor.PeerInfo info;
    static Logger logger = LoggerFactory.getLogger(PeerNotificationTask.class.getName());

    public PeerNotificationTask(ProvisionMonitor[] peers, ProvisionMonitor.PeerInfo info) {
        this.peers = peers;
        this.info = info;
    }

    public void run() {
        for (ProvisionMonitor peer : peers) {
            try {
                peer.update(info);
            } catch (Exception e) {
                logger.warn("Exception notifying ProvisionMonitor", e);
            }
        }
    }
}
