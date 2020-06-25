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
package org.rioproject.impl.system;

import net.jini.config.ConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.UnknownHostException;

/**
 * Provides access to a created {@link ComputeResource}
 *
 * @author Dennis Reedy
 */
public class ComputeResourceAccessor {
    private static ComputeResource computeResource;
    private static Logger logger = LoggerFactory.getLogger(ComputeResourceAccessor.class);

    public static ComputeResource getComputeResource() {
        if(computeResource==null) {
            try {
                computeResource = new ComputeResource();
                computeResource.boot();
            } catch (ConfigurationException | UnknownHostException e) {
                logger.error("Unable to create default ComputeResource", e);
            }
        }
        return computeResource;
    }

    static void setComputeResource(ComputeResource cr) {
        computeResource = cr;
    }
}
