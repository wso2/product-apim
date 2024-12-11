package org.wso2.am.integration.tests.scenariotest;

import javax.net.ssl.*;
import java.io.FileInputStream;
import java.net.SocketException;
import java.security.KeyStore;

public class AbstractSSLClient {

    public static int port = 8743;
    public  String keyStoreLocation = "../src/test/resources/keystores/products/wso2carbon.jks";
    public static String keyStorePassword = "wso2carbon";
    public void run(String payload, RequestMethod method) {}
    protected SSLContext createSSLContext() {

        try {
            KeyStore keyStore = KeyStore.getInstance("JKS");
            keyStore.load(new FileInputStream(keyStoreLocation), keyStorePassword.toCharArray());

            // Create key manager
            KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance("SunX509");
            keyManagerFactory.init(keyStore, "wso2carbon".toCharArray());
            KeyManager[] km = keyManagerFactory.getKeyManagers();

            // Create trust manager
            TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance("SunX509");
            trustManagerFactory.init(keyStore);
            TrustManager[] tm = trustManagerFactory.getTrustManagers();

            // Initialize SSLContext
            SSLContext sslContext = SSLContext.getInstance("TLSv1.2");
            sslContext.init(km, tm, null);

            return sslContext;
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return null;
    }
}

