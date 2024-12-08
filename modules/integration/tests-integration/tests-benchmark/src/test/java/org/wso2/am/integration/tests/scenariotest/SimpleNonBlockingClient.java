package org.wso2.am.integration.tests.scenariotest;

import javax.net.ssl.*;
import java.io.*;
import java.security.KeyStore;

public class SimpleNonBlockingClient extends AbstractSSLClient{
    private static String Bearer;
    private final String host;
    private final int port;
    //String context = "/bny/1.0";
    String context = "/test/1.0.0";

    public SimpleNonBlockingClient(String host, int port, String Bearer) {
        this.Bearer = Bearer;
        this.host = host;
        this.port = port;
    }

    public SimpleNonBlockingClient(String host, int port, String context, String Bearer, String location) {
        this.keyStoreLocation = location;
        this.Bearer = Bearer;
        this.host = host;
        this.port = port;
        this.context = context;
    }
    public void run(String payload, RequestMethod method) {
        try {
            // Create ssl socket
            SSLContext sslContext = this.createSSLContext();
            try {
                // Create socket factory
                SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();
                // Create socket
                SSLSocket sslSocket = (SSLSocket) sslSocketFactory.createSocket(this.host, this.port);
                //System.out.println("Client " + this.getClass().getName() + " started");
                sslSocket.setEnabledCipherSuites(sslSocket.getSupportedCipherSuites());
                try {
                    // Start handshake
                    sslSocket.startHandshake();
                    // Get session after the connection is established
                    SSLSession sslSession = sslSocket.getSession();
                    //System.out.println("SSLSession :");
                    //System.out.println("\tProtocol : " + sslSession.getProtocol());
                    //System.out.println("\tCipher suite : " + sslSession.getCipherSuite());
                    System.out.println("Connection established with the backend");
                    // Start handling application content
                    OutputStream outputStream = sslSocket.getOutputStream();
                    // Create a thread to read the response
                    ResponseReader responseReader = new ResponseReader(sslSocket);
                    Thread responseThread = new Thread(responseReader);
                    responseThread.start();

                    PrintStream printWriter = new PrintStream(outputStream);

                    // Write header data
                    //printWriter.print(method + " " + context + " HTTP/1.1\r\n");
                    printWriter.print(RequestMethod.GET + " " + context + " HTTP/1.1\r\n");
                    printWriter.print("Accept: application/json\r\n");
                    printWriter.print("Connection: keep-alive\r\n");
                    printWriter.print("Authorization: Bearer " + Bearer + "\r\n");
                    int contentLength = payload.getBytes().length;
                    if (contentLength == 0 | method == RequestMethod.GET) {
                        System.out.println("Actual Content-Length: is: " + contentLength + " but " + method + " method so not sending Content-Length header");
                        printWriter.print("\r\n");
                        printWriter.flush();
                        //added to avoid socket closed error
                        responseReader.waitForResponse();
                        sslSocket.close();
                        return;
                    }
                    System.out.println("Actual Content-Length is " + contentLength + " and sending Content-Length " + contentLength );
                    printWriter.print("Content-Length: " + contentLength + "\r\n");
                    printWriter.print("Content-Type: application/json\r\n");
                    printWriter.print("\r\n");
                    // Write payload data
                    printWriter.print(payload);
                    printWriter.print("\r\n");
                    printWriter.flush();
                    responseReader.waitForResponse();
                    sslSocket.close();
                    // closing the socket after reading the response
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
    private static class ResponseReader implements Runnable {
        private final SSLSocket sslSocket;
        private volatile boolean responseComplete = false;
        public ResponseReader(SSLSocket sslSocket) {
            this.sslSocket = sslSocket;
        }
        @Override
        public void run() {
            try {
                System.out.println("Reading the response ...");
                InputStream inputStream = sslSocket.getInputStream();
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
                String line;
                while((line = bufferedReader.readLine()) != null){
                    System.out.println("Response : "+line);
                }
                inputStream.close();
                responseComplete = true;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        public void waitForResponse() throws InterruptedException {
            while (!responseComplete) {
                Thread.sleep(10);
            }
        }
    }
}

