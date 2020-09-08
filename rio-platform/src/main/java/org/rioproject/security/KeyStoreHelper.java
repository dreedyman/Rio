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

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.security.KeyStore;

/**
 * Helper class to create KeyStores.
 *
 * @author Dennis Reedy
 */
public class KeyStoreHelper {

    /**
     * Load a KeyStore from an InputStream.
     *
     * @param inputStream The InputStream.
     *
     * @return An initialized KeyStore
     *
     * @throws Exception if there are problems loading the KeyStore.
     */
    public static KeyStore load(InputStream inputStream) throws Exception {
        KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        keyStore.load(inputStream, null);
        return keyStore;
    }

    /**
     * Load a KeyStore from a path.
     *
     * @param keyStorePath The path of the KeyStore to load.
     *
     * @return An initialized KeyStore
     *
     * @throws Exception if there are problems loading the KeyStore.
     */
    public static KeyStore load(String keyStorePath) throws Exception {
        return load(new File(keyStorePath));

    }

    /**
     * Load a KeyStore from a path.
     *
     * @param keyStoreFile The KeyStore File.
     *
     * @return An initialized KeyStore
     *
     * @throws Exception if there are problems loading the KeyStore.
     */
    public static KeyStore load(File keyStoreFile) throws Exception {
        KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        try (InputStream in = new FileInputStream(keyStoreFile)) {
            keyStore.load(in, null);
        }
        return keyStore;
    }
}
