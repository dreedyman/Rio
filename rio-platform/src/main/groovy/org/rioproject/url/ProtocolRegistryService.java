/*
 * Copyright to the original author or authors
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
package org.rioproject.url;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A registry for protocols and their {@link java.net.URLStreamHandler}s
 *
 * @author Dennis Reedy
 */
public class ProtocolRegistryService implements URLStreamHandlerFactory {
    private static ProtocolRegistryService registry;
    private final Map<String, URLStreamHandler> handlers = new ConcurrentHashMap<>();
    private static Logger logger = LoggerFactory.getLogger(ProtocolRegistryService.class);

    private ProtocolRegistryService() {}

    @Override
    public URLStreamHandler createURLStreamHandler(final String protocol) {
        return handlers.get(protocol);
    }

    /**
     * Register an URLStreamHandler for a protocol
     *
     * @param protocol The protocol to register
     * @param handler The URLStreamHandler for the protocol
     *
     * @throws java.lang.IllegalArgumentException if the protocol or handler is null.
     */
    public void register(final String protocol, final URLStreamHandler handler) {
        if(protocol==null)
            throw new IllegalArgumentException("protocol must not be null");
        if(handler==null)
            throw new IllegalArgumentException("handler must not be null");
        URLStreamHandler previous = handlers.put(protocol, handler);
        if(previous!=null) {
            logger.warn("Replaced {} with {} for protocol {}",
                        previous.getClass().getName(), handler.getClass().getName(), protocol);
        }
    }

    /**
     * Get the ProtocolRegistryService.
     *
     * @return The instance of the ProtocolRegistryService.
     */
    public static synchronized ProtocolRegistryService create() {
        if (registry==null) {
            registry = new ProtocolRegistryService();
            URL.setURLStreamHandlerFactory(registry);
        }
        return registry;
    }
}
