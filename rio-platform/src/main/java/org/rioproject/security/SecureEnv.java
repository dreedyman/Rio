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

import org.rioproject.config.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Sets up secure environment.
 */
public class SecureEnv {
    private static final Logger LOGGER = LoggerFactory.getLogger(SecureEnv.class);

    public static boolean setup() throws Exception {
        String keyStores = System.getProperty(Constants.KEYSTORE);
        if (keyStores != null) {
            return setup(keyStores.split(","));
        }
        return false;
    }

    public static boolean setup(String... keystorePaths) throws Exception {
        List<KeyStore> keyStores = new ArrayList<>();
        for (String keyStorePath : keystorePaths) {
            LOGGER.debug("Loading {}", keyStorePath);
            File keyStoreFile = new File(keyStorePath);
            KeyStore keyStore = KeyStoreHelper.load(keyStoreFile);
            for (String a : Collections.list(keyStore.aliases())) {
                if (KeyStoreHelper.notExpired(keyStore, a)) {
                    keyStores.add(KeyStoreHelper.load(keyStoreFile));
                }
            }
        }
        if (!keyStores.isEmpty()) {
            LOGGER.debug("Initialize AggregateTrustManager");
            AggregateTrustManager.initialize(keyStores.toArray(new KeyStore[0]));
            LOGGER.debug("Allow all host names");
            javax.net.ssl.HttpsURLConnection.setDefaultHostnameVerifier((hostname, sslSession) -> true);
            return true;
        }
        return false;
    }
}
