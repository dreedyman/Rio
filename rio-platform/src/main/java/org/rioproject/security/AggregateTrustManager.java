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

import javax.net.ssl.*;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.*;

/**
 * The AggregateTrustManager uses at least 1 underlying keystore to check if clients or servers are trusted by
 * checking with each known keystore (starting with the JRE's default $JAVA_HOME/jre/lib/security/cacerts).
 *
 * @author Dennis Reedy
 */
public class AggregateTrustManager implements X509TrustManager {
    private final List<X509TrustManager> trustManagers = new LinkedList<>();
    private static final Logger LOGGER = LoggerFactory.getLogger(AggregateTrustManager.class);

    public static void initialize(KeyStore... keyStores) throws Exception {
        TrustManager[] trustManagers = new TrustManager[] { new AggregateTrustManager(keyStores) };
        SSLContext sc = SSLContext.getInstance("SSL");
        sc.init(null, trustManagers, new java.security.SecureRandom());
        HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
    }

    private AggregateTrustManager(KeyStore... keyStores) throws Exception {
        trustManagers.add(getTrustManager(null));
        for (KeyStore keyStore : keyStores) {
            trustManagers.add(getTrustManager(keyStore));
        }
    }

    @Override
    public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        CertificateException last = null;
        for (X509TrustManager trustManager : trustManagers) {
            try {
                doCheckTrusted(trustManager, chain, authType, true);
                last = null;
                break;
            } catch (CertificateException e) {
                last = e;
            }
        }
        if (last != null) {
            throw last;
        }
    }

    private void doCheckTrusted(X509TrustManager trustManager,
                                X509Certificate[] chain,
                                String authType,
                                boolean isClient) throws CertificateException {
        if (isClient) {
            trustManager.checkClientTrusted(chain, authType);
        } else {
            trustManager.checkServerTrusted(chain, authType);
        }
    }

    @Override
    public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        CertificateException last = null;
        for (X509TrustManager trustManager : trustManagers) {
            try {
                doCheckTrusted(trustManager, chain, authType, false);
                last = null;
                break;
            } catch (CertificateException e) {
                last = e;
            }
        }
        if (last != null) {
            throw last;
        }
    }

    @Override
    public X509Certificate[] getAcceptedIssuers() {
        return trustManagers.stream()
                .map(X509TrustManager::getAcceptedIssuers)
                .flatMap(Arrays::stream).toArray(X509Certificate[]::new);
    }

    static X509TrustManager getTrustManager(KeyStore keyStore) throws NoSuchAlgorithmException, KeyStoreException {
        X509TrustManager trustManager = null;
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(keyStore);
        for (TrustManager tm : tmf.getTrustManagers()) {
            if (tm instanceof X509TrustManager) {
                trustManager = (X509TrustManager) tm;
                break;
            }
        }
        LOGGER.debug("Got X509TrustManager for {}", keyStore);
        return trustManager;
    }
}

