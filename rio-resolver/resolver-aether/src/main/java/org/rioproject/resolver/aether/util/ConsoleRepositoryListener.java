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
package org.rioproject.resolver.aether.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonatype.aether.AbstractRepositoryListener;
import org.sonatype.aether.RepositoryEvent;

import java.io.PrintStream;

/**
 * A simplistic repository listener that logs events to the console.
 */
public class ConsoleRepositoryListener extends AbstractRepositoryListener {
    private PrintStream out;
    private final static Logger logger = LoggerFactory.getLogger(ConsoleRepositoryListener.class.getName());

    public ConsoleRepositoryListener() {
        this(null);
    }

    public ConsoleRepositoryListener(PrintStream out) {
        this.out = (out != null) ? out : System.err;
    }

    public void artifactDeployed(RepositoryEvent event) {
        logger.info("Deployed " + event.getArtifact() + " to " + event.getRepository());
    }

    public void artifactDeploying(RepositoryEvent event) {
        //out.println("Deploying " + event.getArtifact() + " to " + event.getRepository());
    }

    public void artifactDescriptorInvalid(RepositoryEvent event) {
        logger.info("Invalid artifact descriptor for " + event.getArtifact() + ": "
                    + event.getException().getMessage());
    }

    public void artifactDescriptorMissing(RepositoryEvent event) {
        logger.info("Missing artifact descriptor for " + event.getArtifact());
    }

    public void artifactInstalled(RepositoryEvent event) {
        out.println("Installed " + event.getArtifact() + " to " + event.getFile());
    }

    public void artifactInstalling(RepositoryEvent event) {
        //out.println("Installing " + event.getArtifact() + " to " + event.getFile());
    }

    public void artifactResolved(RepositoryEvent event) {
        if(logger.isDebugEnabled())
            logger.debug("Resolved artifact " + event.getArtifact() + " from " + event.getRepository());
    }

    public void artifactDownloading(RepositoryEvent event) {
        if(logger.isTraceEnabled())
            logger.trace("Downloading artifact " + event.getArtifact() + " from " + event.getRepository());
    }

    public void artifactDownloaded(RepositoryEvent event) {
        if(logger.isTraceEnabled())
            logger.trace("Downloaded artifact " + event.getArtifact() + " from " + event.getRepository());
    }

    public void artifactResolving(RepositoryEvent event) {
        if(logger.isTraceEnabled())
            logger.trace("Resolving artifact " + event.getArtifact());
    }

    public void metadataDeployed(RepositoryEvent event) {
        logger.info("Deployed " + event.getMetadata() + " to " + event.getRepository());
    }

    public void metadataDeploying(RepositoryEvent event) {
        //out.println("Deploying " + event.getMetadata() + " to " + event.getRepository());
    }

    public void metadataInstalled(RepositoryEvent event) {
        logger.info("Installed " + event.getMetadata() + " to " + event.getFile());
    }

    public void metadataInstalling(RepositoryEvent event) {
        //out.println("Installing " + event.getMetadata() + " to " + event.getFile());
    }

    public void metadataInvalid(RepositoryEvent event) {
        if(logger.isDebugEnabled())
            logger.debug("Invalid metadata " + event.getMetadata());
    }

    public void metadataResolved(RepositoryEvent event) {
        if(logger.isDebugEnabled())
            logger.debug("Resolved metadata " + event.getMetadata() + " from " + event.getRepository());
    }

    public void metadataResolving(RepositoryEvent event) {
        if(logger.isTraceEnabled())
            logger.trace("Resolving metadata " + event.getMetadata() + " from " + event.getRepository());
    }

}
