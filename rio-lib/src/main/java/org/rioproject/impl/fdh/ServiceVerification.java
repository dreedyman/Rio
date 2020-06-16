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
package org.rioproject.impl.fdh;

import net.jini.core.lookup.ServiceItem;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * @author Dennis Reedy
 */
public class ServiceVerification {
    private static final BlockingQueue<ExternalServiceRequest> verificationQ = new LinkedBlockingQueue<>();

    static class ExternalServiceRequest {
        final ServiceItem item;
        final boolean removed;

        public ExternalServiceRequest(ServiceItem item) {
            this(item, false);
        }

        public ExternalServiceRequest(ServiceItem item, boolean removed) {
            this.item = item;
            this.removed = removed;
        }
    }
}
