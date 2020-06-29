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
package org.rioproject.tools.jetty;

import org.eclipse.jetty.server.*;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.rioproject.net.HostUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URI;

/**
 * Embedded Jetty server
 *
 * Created by Dennis Reedy on 6/19/17.
 */
public class Jetty {
    private int port;
    private int minThreads;
    private int maxThreads;
    private Server server;
    private final ContextHandlerCollection contexts = new ContextHandlerCollection();
    private static final Logger logger = LoggerFactory.getLogger(Jetty.class);

    public Jetty setRoots(String... roots) {
        for(String root : roots) {
            logger.info("Adding {}", new File(root).getName());
            File dir = new File(root);
            contexts.addHandler(createContextHandler(dir, new ResourceHandler()));
        }
        return this;
    }

    public Jetty setPort(int port) {
        this.port = port;
        return this;
    }

    public Jetty setMinThreads(int minThreads) {
        this.minThreads = minThreads;
        return this;
    }

    public Jetty setMaxThreads(int maxThreads) {
        this.maxThreads = maxThreads;
        return this;
    }

    public Jetty setPutDir(String putDir) {
        File dir = new File(putDir);
        contexts.addHandler(createContextHandler(dir, new PutHandler(dir)));
        return this;
    }

    public void start() throws Exception {
        createServer();
        ServerConnector connector = new ServerConnector(server);
        connector.setPort(port);
        String address = HostUtil.getHostAddressFromProperty("java.rmi.server.hostname");
        connector.setHost(address);
        server.setConnectors(new Connector[] { connector });
        server.setHandler(contexts);
        server.start();
    }

    public void startSecure() throws Exception {
        createServer();
        HttpConfiguration https = new HttpConfiguration();
        https.addCustomizer(new SecureRequestCustomizer());

        SslContextFactory sslContextFactory = new SslContextFactory();
        sslContextFactory.setKeyStorePath(Jetty.class.getResource("/org/rioproject/riokey.jks").toExternalForm());
        sslContextFactory.setKeyStorePassword("riorules");
        sslContextFactory.setKeyManagerPassword("riorules");
        ServerConnector sslConnector = new ServerConnector(server,
                                                           new SslConnectionFactory(sslContextFactory, "http/1.1"),
                                                           new HttpConnectionFactory(https));
        sslConnector.setPort(0);
        String address = HostUtil.getHostAddressFromProperty("java.rmi.server.hostname");
        sslConnector.setHost(address);
        server.setConnectors(new Connector[] { sslConnector });
        server.setHandler(contexts);
        server.start();
    }

    public URI getURI() {
        return server==null?null:server.getURI();
    }

    private void createServer() {
        if(server!=null)
            return;
        if(maxThreads>0) {
            QueuedThreadPool threadPool = new QueuedThreadPool(maxThreads, minThreads);
            server = new Server(threadPool);
        } else {
            server = new Server();
        }
    }

    public int getPort() {
        if(server==null)
            return port;
        return ((ServerConnector)server.getConnectors()[0]).getLocalPort();
    }

    public String getAddress() {
        if(server==null)
            return null;
        return server.getURI().getHost();
    }

    public void join()  {
        try {
            server.join();
        } catch (InterruptedException e) {
            logger.warn("Jetty join interrupted", e);
        }
    }

    private ContextHandler createContextHandler(File dir, AbstractHandler handler) {
        ContextHandler context = new ContextHandler();
        context.setContextPath("/");
        context.setBaseResource(Resource.newResource(dir));
        context.setHandler(handler);
        return context;
    }

}
