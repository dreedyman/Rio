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
package org.rioproject.url.artifact;

import org.rioproject.resolver.Artifact;
import org.rioproject.resolver.Resolver;
import org.rioproject.resolver.ResolverException;
import org.rioproject.resolver.ResolverHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

/**
 * <p>A stream handler for URLs with the <code>artifact</code> protocol. The <code>artifact</code> URL
 * provides a way to resolve an artifact's dependencies.</p>
 *
 * <p>The URL scheme for this handler is:<br/>
 * <pre>artifact:groupId/artifactId/version[/type[/classifier]][;repository[@repositoryId]]</pre></p>
 *
 * <p>This handler has to be installed before any connection is made by using the following code:
 * <pre>URL.setURLStreamHandlerFactory(new ArtifactURLStreamHandlerFactory());</pre>
 * or must be set via a JVM property such as:
 * <pre>-Djava.protocol.handler.pkgs=org.rioproject.url</pre>
 * </p>
 *
 * <p>Here are some examples:</p>
 * <ul>
 *     <li>An artifact URL for groupId, artifactId and version<br/>
 *     <code>artifact:org.rioproject.examples.calculator/calculator-service/2.0.1</code></li>
 *     <li>An artifact URL for groupId, artifactId, version and repository with an id<br/>
 *     <code>artifact:org.rioproject.examples.calculator/calculator-proxy/2.0.1;http://www.rio-project.org@rio</code></li>
 * </ul>
 *
 * <p>Once an artifact has been resolved to an {@code URL}, the {@code artifact:URL} pair is
 * cached, avoiding future interactions with the underlying {@link Resolver}</p>
 *
 * @author Dennis Reedy
 */
/* We want to throw a RuntimeException if we cannot get a Resolver */
@SuppressWarnings("PMD.AvoidThrowingRawExceptionTypes")
public class Handler extends URLStreamHandler {
    private static final Logger logger = LoggerFactory.getLogger(Handler.class.getName());
    private static Resolver resolver;
    static {
        try {
            resolver = ResolverHelper.getResolver();
        } catch (ResolverException e) {
            logger.error("Could not get a ResolverInstance", e);
            throw new RuntimeException("Could not get a ResolverInstance", e);
        }
    }
    private final ConcurrentMap<Artifact, URL> cache = new ConcurrentHashMap<Artifact, URL>();

    public Handler() {
        ScheduledExecutorService snapshotReaper = Executors.newScheduledThreadPool(1, new ThreadFactory() {
            @Override public Thread newThread(Runnable runnable) {
                Thread thread = Executors.defaultThreadFactory().newThread(runnable);
                thread.setDaemon(true);
                return thread;
            }
        });
        snapshotReaper.scheduleAtFixedRate(new SnapShotReaper(), 3, 3, TimeUnit.HOURS);
    }

    protected URLConnection openConnection(URL url) throws IOException {
        if(url==null)
            throw new MalformedURLException("url cannot be null");
        String path = url.getPath();
        if(path==null)
            throw new MalformedURLException("url has null path");

        ArtifactURLConfiguration configuration = new ArtifactURLConfiguration(path);
        String artifact = configuration.getArtifact();
        Artifact a;
        try {
            a = new Artifact(artifact);
        } catch(IllegalArgumentException e) {
            throw new MalformedURLException(e.getLocalizedMessage());
        }

        URL u;
        try {
            u = cache.get(a);
            if(u==null) {
                u = resolver.getLocation(artifact, a.getType(), configuration.getRepositories());
                cache.put(a, u);
                if(logger.isDebugEnabled())
                    logger.debug("Location of {} is {}", a, u==null?"<NULL>":u.toExternalForm());
            }
        } catch (ResolverException e) {
            logger.warn(String.format("Could not resolve %s", a), e);
            throw new IOException(e.getLocalizedMessage());
        }

        if(u!=null)
            return u.openConnection();

        return null;
    }

    /**
     * The {@code SnapShotReaper} is used to check if any cached artifacts are SNAPSHOTs. This class
     * is run by a ScheduledExecutor every 3 hours to check the cache, if there are SNAPSHOTs in the cache
     * they will be purged, meaning the next time the artifact gets resolved, it may be downloaded if necessary.
     *
     * TODO: The timing of this needs to be configurable.
     */
    private class SnapShotReaper implements Runnable {
        public void run() {
            List<Artifact> removals = new ArrayList<Artifact>();
            for(Map.Entry<Artifact, URL> entry : cache.entrySet()) {
                if(entry.getKey().getVersion().endsWith("SNAPSHOT")) {
                    removals.add(entry.getKey());
                }
            }
            for(Artifact a : removals) {
                if(logger.isDebugEnabled())
                    logger.debug("Removing "+a.toString()+" from cache");
                cache.remove(a);
            }
        }
    }
}
