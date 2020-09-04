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

import net.jini.config.Configuration;
import org.eclipse.jetty.http.HttpVersion;
import org.eclipse.jetty.server.*;
import org.eclipse.jetty.server.handler.*;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.rioproject.config.Constants;
import org.rioproject.net.HostUtil;
import org.rioproject.util.RioHome;
import org.rioproject.web.WebsterService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URI;
import java.util.Arrays;

/**
 * Embedded Jetty server
 *
 * Created by Dennis Reedy on 6/19/17.
 */
public class Jetty implements WebsterService {
    private int port;
    private int minThreads;
    private int maxThreads;
    private boolean isSecure;
    private Server server;
    private final HandlerList handlers = new HandlerList();
    private static final Logger logger = LoggerFactory.getLogger(Jetty.class);
    static final String COMPONENT = "org.rioproject.tools.jetty";

    public Jetty() {
    }

    public Jetty(Configuration config) throws Exception {
        setPort((Integer) config.getEntry(COMPONENT,"port", int.class, 0))
                .setRoots((String[]) config.getEntry(COMPONENT, "roots", String[].class,null))
                .setPutDir((String) config.getEntry(COMPONENT, "putDir", String.class, null))
                .setMinThreads((Integer) config.getEntry(COMPONENT, "minThreads", int.class, 0))
                .setMaxThreads((Integer) config.getEntry(COMPONENT, "maxThreads", int.class, 0))
                .setSecure((Boolean) config.getEntry(COMPONENT, "secure", Boolean.class, true));
        if (isSecure) {
            startSecure();
        } else {
            start();
        }
    }

    public Jetty setRoots(String... roots) {
        if (logger.isDebugEnabled()) {
            logger.debug("Setting roots {}", Arrays.toString(roots));
        }
        for(String root : roots) {
            if (";".equals(root)) {
                continue;
            }
            if (logger.isDebugEnabled()) {
                logger.debug("Adding {}", new File(root).getPath());
            }
            File dir = new File(root);
            //contexts.addHandler(createContextHandler(dir, new ResourceHandler()));
            //contexts.addHandler(createHandler(dir));
            handlers.addHandler(createHandler(dir));
        }
        return this;
    }

    public Jetty setPort(int port) {
        this.port = port;
        return this;
    }

    public Jetty setMinThreads(int minThreads) {
        if (minThreads > 0) {
            this.minThreads = minThreads;
        }
        return this;
    }

    public Jetty setMaxThreads(int maxThreads) {
        if (maxThreads > 0) {
            this.maxThreads = maxThreads;
        }
        return this;
    }

    public Jetty setPutDir(String putDir) {
        if (putDir != null) {
            File dir = new File(putDir);
            //contexts.addHandler(createContextHandler(dir, new PutHandler(dir)));
            handlers.addHandler(createContextHandler(dir, new PutHandler(dir)));
        }
        return this;
    }

    @Override
    public void start() throws Exception {
        createServer();
        ServerConnector connector = new ServerConnector(server);
        connector.setPort(port);
        String address = HostUtil.getHostAddressFromProperty("java.rmi.server.hostname");
        connector.setHost(address);
        server.setConnectors(new Connector[] { connector });
        //server.setHandler(contexts);
        server.setHandler(handlers);
        server.start();
        setProperty();
    }

    @Override
    public void startSecure() throws Exception {
        logger.info("Starting in secure mode");
        String keyStorePath = String.format("%s/config/security/rio-cert.ks", RioHome.get());
        if (logger.isDebugEnabled()) {
            logger.debug("KeyStore file: {}", keyStorePath);
        }
        createServer();

        HttpConfiguration https = new HttpConfiguration();
        https.addCustomizer(new SecureRequestCustomizer());

        SslContextFactory sslContextFactory = new SslContextFactory.Server();
        sslContextFactory.setKeyStorePath(keyStorePath);

        sslContextFactory.setKeyStorePassword("OBF:1vn21ugu1saj1v9i1v941sar1ugw1vo0");
        sslContextFactory.setKeyManagerPassword("OBF:1vn21ugu1saj1v9i1v941sar1ugw1vo0");

        ServerConnector sslConnector = new ServerConnector(server,
                                                           new SslConnectionFactory(sslContextFactory,
                                                                   HttpVersion.HTTP_1_1.asString()),
                                                           new HttpConnectionFactory(https));
        sslConnector.setPort(port);
        String address = HostUtil.getHostAddressFromProperty("java.rmi.server.hostname");
        sslConnector.setHost(address);
        server.setConnectors(new Connector[] { sslConnector });
        //server.setHandler(contexts);
        server.setHandler(handlers);
        server.start();
        setProperty();
    }

    @Override
    public void terminate() {
        if (server != null) {
            try {
                server.stop();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public URI getURI() {
        return server == null ? null : server.getURI();
    }

    private void createServer() {
        if (server != null) {
            return;
        }
        if (maxThreads > 0) {
            QueuedThreadPool threadPool = new QueuedThreadPool(maxThreads, minThreads);
            server = new Server(threadPool);
        } else {
            server = new Server();
        }
    }

    public int getPort() {
        if (server == null) {
            return port;
        }
        return ((ServerConnector)server.getConnectors()[0]).getLocalPort();
    }

    public String getAddress() {
        if (server == null) {
            return null;
        }
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

    private Handler createHandler(File dir) {
        //PathResource pathResource = new PathResource(dir);
        ResourceHandler resourceHandler = new ResourceHandler();
        resourceHandler.setResourceBase(dir.getAbsolutePath());
        //resourceHandler.setBaseResource(pathResource);
        resourceHandler.setDirectoriesListed(true);
        return resourceHandler;
        /*FileHandler fileHandler = new FileHandler(dir);
        return fileHandler;*/
    }

    private void setProperty() {
        if (server != null) {
            URI uri = server.getURI();
            String serverAddress = uri.toASCIIString().endsWith("/")
                    ? uri.toASCIIString().substring(0, uri.toASCIIString().length() - 1)
                    : uri.toASCIIString();

            System.setProperty(Constants.WEBSTER, serverAddress);
        }
    }

    private void setSecure(boolean isSecure) {
        this.isSecure = isSecure;
    }

}
