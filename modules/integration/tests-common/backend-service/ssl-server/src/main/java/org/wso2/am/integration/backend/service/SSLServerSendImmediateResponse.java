package org.wso2.am.integration.backend.service;

import com.sun.net.httpserver.HttpsServer;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.*;
import java.net.InetSocketAddress;
import java.security.KeyStore;
import java.util.concurrent.Executors;

public class SSLServerSendImmediateResponse extends AbstractSSLServer {

    public void run(int port, String content, int statusCode) throws InterruptedException {
        // Create a new thread to run the server
        try {
            // Initialize the SSL context with the keystore
            SSLContext sslContext = SSLContext.getInstance("TLS");

            KeyStore keyStore = KeyStore.getInstance("JKS");
            FileInputStream fis = new FileInputStream("../src/test/resources/keystores/products/wso2carbon.jks");
            keyStore.load(fis, "wso2carbon".toCharArray());

            KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
            kmf.init(keyStore, "wso2carbon".toCharArray());

            TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
            tmf.init(keyStore);

            sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);

            // Create an HttpsServer instance
            HttpsServer server = HttpsServer.create(new InetSocketAddress(8100), 0);
            this.setServer(server);
            server.setHttpsConfigurator(new com.sun.net.httpserver.HttpsConfigurator(sslContext));

            server.createContext("/", new Handler(statusCode,content));
            server.setExecutor(Executors.newSingleThreadExecutor());
            server.start();

            System.out.println("SSL Server is listening on port 8100...");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
