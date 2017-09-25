/*
 * Copyright 2008 the original author or authors.
 * Copyright 2005 Sun Microsystems, Inc.
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
package org.rioproject.tools.jetty;

import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.DefaultHandler;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.rioproject.net.HostUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

/**
 * Embedded Jetty server
 *
 * Created by Dennis Reedy on 6/19/17.
 */
public class Jetty {
    private final Server server;
    private static final Logger logger = LoggerFactory.getLogger(Jetty.class);

    public Jetty(int port, String... roots) throws Exception {
        this(port, roots, System.getProperty("webster.put.dir"));
    }

    public Jetty(int port, String[] roots, String putDir) throws Exception {
        this(port, roots, putDir, 0, 0);
    }

    public Jetty(int port, String[] roots, String putDir, int maxThreads, int minThreads) throws Exception {
        if(maxThreads>0) {
            QueuedThreadPool threadPool = new QueuedThreadPool(maxThreads, minThreads);
            server = new Server(threadPool);
        } else {
            server = new Server();
        }
        ServerConnector connector = new ServerConnector(server);
        connector.setPort(port);
        String address = HostUtil.getHostAddressFromProperty("java.rmi.server.hostname");
        connector.setHost(address);
        server.setConnectors(new Connector[] { connector });
        HandlerList handlers = new HandlerList();
        for(String root : roots) {
            logger.info("Adding {}", new File(root).getName());

            /*ResourceHandler resourceHandler = new ResourceHandler();
            resourceHandler.setDirectoriesListed(true);
            resourceHandler.setResourceBase(new File(root).getName());
            ContextHandler context = new ContextHandler();
            context.setContextPath("/");
            File dir = new File(root);
            context.setBaseResource(Resource.newResource(dir));
            context.setHandler(resourceHandler);
            contexts.addHandler(context);*/


            ResourceHandler resourceHandler = new ResourceHandler();
            resourceHandler.setDirectoriesListed(true);
            resourceHandler.setResourceBase(".");
            handlers.addHandler(resourceHandler);
        }
        if(putDir!=null)
            handlers.addHandler(new PutHandler(new File(putDir)));
        handlers.addHandler(new DefaultHandler());

        server.setHandler(handlers);
        server.start();
    }

    public int getPort() {
        return ((ServerConnector)server.getConnectors()[0]).getLocalPort();
    }

    public String getAddress() {
        return server.getURI().getHost();
    }

    public void join()  {
        try {
            server.join();
        } catch (InterruptedException e) {
            logger.warn("Jetty join interrupted", e);
        }
    }

}
