package org.wso2.am.integration.backend.service;

import com.sun.net.httpserver.HttpsServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.security.KeyStore;
import java.util.concurrent.Executors;

public class SimpleHTTPSServer {

    private final String keyStoreLocation;
    private final String keyStorePassword;

    public SimpleHTTPSServer(String keyStoreLocation, String keyStorePassword){
        this.keyStoreLocation = keyStoreLocation;
        this.keyStorePassword =keyStorePassword;
    }

    public void run() throws InterruptedException {
        // Create a new thread to run the server
        try {
            // Initialize the SSL context with the keystore
            SSLContext sslContext = SSLContext.getInstance("TLS");

            KeyStore keyStore = KeyStore.getInstance("JKS");
            FileInputStream fis = new FileInputStream(this.keyStoreLocation);
            keyStore.load(fis, this.keyStorePassword.toCharArray());

            KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
            kmf.init(keyStore, this.keyStorePassword.toCharArray());

            TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
            tmf.init(keyStore);

            sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);

            // Create an HttpsServer instance
            HttpsServer server = HttpsServer.create(new InetSocketAddress(8100), 0);
            server.setHttpsConfigurator(new com.sun.net.httpserver.HttpsConfigurator(sslContext));

            server.createContext("/", new SimpleHTTPSServer.MyHandler());
            server.setExecutor(Executors.newSingleThreadExecutor());
            server.start();

            System.out.println("SSL Server is listening on port 8100...");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Define the handler that processes incoming HTTP requests
    public static class MyHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            // Respond with status code 200 for any request method
            t.sendResponseHeaders(200, 0);
            OutputStream os = t.getResponseBody();
            os.close();
        }
    }
}

