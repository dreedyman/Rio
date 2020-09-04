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
package org.rioproject.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.List;

/**
 * Sets up secure environment.
 */
public class SecureEnv {
    private static final Logger LOGGER = LoggerFactory.getLogger(SecureEnv.class);
    public static final String KEYSTORE = "org.rioproject.keystore";

    public static boolean setup() throws Exception {
        String keyStores = System.getProperty(KEYSTORE);
        if (keyStores != null) {
            setup(keyStores.split(","));
            return true;
        }
        return false;
    }

    public static void setup(String... keystorePaths) throws Exception {
        List<KeyStore> keyStores = new ArrayList<>();
        for (String keyStorePath : keystorePaths) {
            LOGGER.info("Loading {}", keyStorePath);
            File keyStoreFile = new File(keyStorePath);
            keyStores.add(KeyStoreHelper.load(keyStoreFile));
        }
        LOGGER.info("Initialize AggregateTrustManager");
        org.rioproject.security.AggregateTrustManager.initialize(keyStores.toArray(new KeyStore[0]));
        LOGGER.info("Allow all host names");
        javax.net.ssl.HttpsURLConnection.setDefaultHostnameVerifier((hostname, sslSession) -> true);
    }
}
