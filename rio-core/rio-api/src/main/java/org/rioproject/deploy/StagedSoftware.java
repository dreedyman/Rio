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
package org.rioproject.deploy;

import org.rioproject.exec.ExecDescriptor;

import java.io.Serializable;

/**
 * The StagedSoftware defines an installable software element with an optional
 * post-install helper to configure the downloaded software.
 *
 * @author Dennis Reedy
 */
public class StagedSoftware extends StagedData {
    static final long serialVersionUID = 1L;
    private boolean useAsClasspathResource;
    /**
     * PostInstallAttributes object
     */
    private PostInstallAttributes postInstall;

    /**
     * Set the post install attributes
     *
     * @param postInstall A PostInstallAttributes object defining a post
     * installation
     */
    public void setPostInstallAttributes(PostInstallAttributes postInstall) {
        if (postInstall == null)
            throw new IllegalArgumentException("postInstall is null");
        this.postInstall = postInstall;
    }

    /**
     * Get the post install attributes
     *
     * @return The PostInstallAttributes
     */
    public PostInstallAttributes getPostInstallAttributes() {
        return (postInstall);
    }

    public boolean getUseAsClasspathResource() {
        return useAsClasspathResource;
    }

    public void setUseAsClasspathResource(boolean useAsClasspathResource) {
        this.useAsClasspathResource = useAsClasspathResource;
    }

    /**
     * The PostInstallAttributes defines attributes needed to run and optionally
     * download a utility to configure the StagedSoftware
     */
    public static class PostInstallAttributes implements Serializable {
        static final long serialVersionUID = 1L;
        /**
         * PostInstall attributes
         */
        private StagedData stagedData;
        /**
         * Specifies a utility that will be run to configure the StagedSoftware
         */
        private ExecDescriptor execDescriptor;

        /**
         * Create a PostInstallAttributes instance
         *
         * @param execDescriptor Specifies a utility that will be run to
         * configure a downloaded StagedSoftware.
         * @param stagedData StagedData instance defining
         * attributes needed to download the post-install utility
         */
        public PostInstallAttributes(ExecDescriptor execDescriptor,
                                     StagedData stagedData) {
            if (execDescriptor == null)
                throw new IllegalArgumentException("executionTarget is null");
            this.execDescriptor = execDescriptor;
            this.stagedData = stagedData;
        }

        /**
         * Get the ExecDescriptor
         *
         * @return An ExecDescriptor declaring the utility that will be run to
         *         configure downloaded StagedSoftware.
         */
        public ExecDescriptor getExecDescriptor() {
            return (execDescriptor);
        }

        /**
         * Get the StagedData for the post installer
         *
         * @return The StagedData for the post install utility. If there
         *         is no StagedData for the post-installer, this value
         *         will be null
         */
        public StagedData getStagedData() {
            return (stagedData);
        }
    }
}
