/*
 * Copyright to the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.rioproject.web;

import java.net.URI;

/**
 * Interface for a Webster service.
 */
public interface WebsterService {

    /**
     * Setup roots to serve.
     *
     * @param roots Roots to serve.
     */
    WebsterService setRoots(String... roots);

    /**
     * @return The URI of the service, or null
     */
    URI getURI();

    /**
     * Starts the service.
     *
     * @throws Exception If the service fails to start.
     */
    void start() throws Exception;

    /**
     * Starts the service with HTTPS.
     *
     * @throws Exception If the service fails to start.
     */
    void startSecure() throws Exception;

    /**
     * Stops the service.
     */
    void terminate();
}
