package org.rioproject.security;

import org.junit.Before;
import org.junit.Test;
import org.rioproject.config.Constants;

import javax.net.ssl.SSLHandshakeException;
import java.io.File;
import java.net.URL;
import java.net.URLConnection;
import java.security.KeyStore;

import static org.junit.Assume.assumeTrue;

public class AggregateTrustManagerTest {
    private File keyStoreFile;
    private KeyStore keyStore;

    @Before
    public void setup() throws Exception {
        keyStoreFile = new File(System.getProperty(Constants.KEYSTORE));
        keyStore = KeyStoreHelper.load(keyStoreFile);
    }

    @Test
    public void testWithDefault_usingBothCacertsAndRioKeyStore() throws Exception {
        assumeTrue(KeyStoreHelper.notExpired(keyStore, "rio-project"));
        AggregateTrustManager.initialize(keyStore);
        URL locationURL = new URL("https://www.google.com");
        URLConnection urlConnection = locationURL.openConnection();
        urlConnection.connect();
    }

    @Test(expected= SSLHandshakeException.class)
    public void testWithJustRioKeystore_shouldFail() throws Exception {
        assumeTrue(KeyStoreHelper.notExpired(keyStore, "rio-project"));
        System.setProperty("javax.net.ssl.trustStore", keyStoreFile.getPath());
        AggregateTrustManager.initialize(keyStore);
        URL locationURL = new URL("https://www.google.com");
        URLConnection urlConnection = locationURL.openConnection();
        urlConnection.connect();
    }
}