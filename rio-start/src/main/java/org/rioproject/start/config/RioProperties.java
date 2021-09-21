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
package org.rioproject.start.config;

import org.rioproject.config.Constants;
import org.rioproject.util.PropertyHelper;

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
                String rioHome = System.getProperty("rio.home", System.getenv("RIO_HOME"));
                if(rioHome!=null) {
                    rioEnv = new File(rioHome, "config/rio.env");
                    if(rioEnv.exists()) {
                        loadAndSetProperties(rioEnv);
                    } else {
                        System.err.println(rioEnv.getPath()+" not found, skipping");
                    }
                } else {
                    System.err.println("RIO_HOME environment not set");
                }
            }
        } else {
            rioEnv = new File(rioEnvFileName);
            if(rioEnv.exists()) {
                loadAndSetProperties(rioEnv);
            }
        }
    }

    private static void loadAndSetProperties(final File rioEnv) {
        System.err.println("Loading properties from "+ rioEnv.getPath());
        Properties properties = new Properties();
        try {
            properties.load(new FileInputStream(rioEnv));
            for(String propertyName : properties.stringPropertyNames()) {
                String properyValue = PropertyHelper.expandProperties(properties.getProperty(propertyName));
                System.setProperty(propertyName, properyValue);
                System.out.println("Setting "+propertyName+" = "+System.getProperty(propertyName));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
