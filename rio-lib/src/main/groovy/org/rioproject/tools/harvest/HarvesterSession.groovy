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
package org.rioproject.tools.harvest

/**
 * Contains information needed to communicate with Harvester
 */
class HarvesterSession implements Serializable {
    static final long serialVersionUID = 1L
    int port
    String host

    HarvesterSession(int port, String host) {
        this.port = port
        this.host = host
    }


    @Override
    String toString() {
        return String.format("HarvesterSession port: %d, host: %s", port, host)
    }
}