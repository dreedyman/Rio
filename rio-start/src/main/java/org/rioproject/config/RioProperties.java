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
package org.rioproject.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.util.Properties;

/**
 * Load a properties file and set system properties. Locating the properties file is doe as follows:
 *
 * <ol>
 *     <li>First check the {@code org.rioproject.env} environment variable, if not found...</li>
 *     <li>Check for {@code ~/.rio/rio.env} file, if not found...</li>
 *     <li>Check for {@code $RIO_HOME/config/rio.env} file</li>
 * </ol>
 *
 * @author Dennis Reedy
 */
public final class RioProperties {
    private final static Logger logger = LoggerFactory.getLogger(RioProperties.class);
    private RioProperties(){}

    public static void load() {
        File rioEnv;
        String rioEnvFileName = System.getProperty(Constants.ENV_PROPERTY_NAME,
                                                   System.getenv(Constants.ENV_PROPERTY_NAME));
        if(rioEnvFileName==null) {
            File rioRoot = new File(System.getProperty("user.home"), ".rio");
            rioEnv = new File(rioRoot, "rio.env");
            if(rioEnv.exists()) {
                loadAndSetProperties(rioEnv);
            } else {
                String rioHome = System.getProperty("RIO_HOME", System.getenv("RIO_HOME"));
                if(rioHome!=null) {
                    rioEnv = new File(rioHome, "config/rio.env");
                    if(rioEnv.exists()) {
                        loadAndSetProperties(rioEnv);
                    } else {
                        logger.info("{} not found, skipping", rioEnv.getPath());
                    }
                } else {
                    logger.info("RIO_HOME environment not set");
                }
            }
        } else {
            logger.debug("Loading from {}", Constants.ENV_PROPERTY_NAME);
            rioEnv = new File(rioEnvFileName);
            if(rioEnv.exists()) {
                loadAndSetProperties(rioEnv);
            }
        }
    }

    private static void loadAndSetProperties(final File rioEnv) {
        logger.info("Loading properties from {}", rioEnv.getPath());
        Properties properties = new Properties();
        try {
            properties.load(new FileInputStream(rioEnv));
            for(String propertyName : properties.stringPropertyNames()) {
                if(logger.isDebugEnabled()) {
                    logger.debug("Setting {}: {}", propertyName, properties.getProperty(propertyName));
                }
                System.setProperty(propertyName, properties.getProperty(propertyName));
            }
        } catch (Exception e) {
            logger.warn("Problem reading {}", rioEnv.getPath(), e);
        }
    }
}
