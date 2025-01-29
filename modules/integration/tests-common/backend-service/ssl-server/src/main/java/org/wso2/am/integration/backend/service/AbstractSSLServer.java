package org.wso2.am.integration.backend.service;

import com.sun.net.httpserver.HttpsServer;

import java.io.IOException;
import java.net.ServerSocket;

public class AbstractSSLServer {
    private HttpsServer server;

    public AbstractSSLServer() {
        System.out.println("initiating "+ this.getClass().getSimpleName() +" server ");
    }

    public void setServer(HttpsServer server) {
        this.server = server;
    }

    public void run(int port, String content, int statusCode) throws Exception {}

//    public void shutdownServer() throws InterruptedException {
//        try {
//            while (!isserverdone){
//                Thread.sleep(10);
//            }
//            System.out.println("Shutting down the "+ this.getClass().getSimpleName() +" server");
//            ss.close();
//        } catch (IOException e) {
//            System.out.println("Error while shutting down the server ");
//        }
//    }
    public void stop() {
        if (server != null) {
            server.stop(0);  // Stops the server immediately (0-second delay)
            System.out.println("SSL Server has been stopped.");
        }
    }
}
