package org.wso2.am.integration.tests.scenariotest;

import org.testng.Assert;

import javax.net.ssl.*;
import java.io.*;
import java.net.SocketException;
import java.security.KeyStore;

public class NonBlockingClientSendLessContent extends AbstractSSLClient{
    private static String Bearer;
    private final String host;
    private final int port;
    //String context = "/bny/1.0";
    String context = "/test/1.0.0";

    public NonBlockingClientSendLessContent(String host, int port, String Bearer) {
        this.Bearer = Bearer;
        this.host = host;
        this.port = port;
    }

    public NonBlockingClientSendLessContent(String host, int port, String context, String Bearer, String location) {
        this.keyStoreLocation = location;
        this.Bearer = Bearer;
        this.host = host;
        this.port = port;
        this.context = context;
    }
    public void run(String payload, RequestMethod method){
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
                    System.out.println("Connection established with the backend");
                    // Start handling application content
                    OutputStream outputStream = sslSocket.getOutputStream();
                    // Create a thread to read the response
                    ResponseReader responseReader = new ResponseReader(sslSocket);
                    Thread responseThread = new Thread(responseReader);
                    responseThread.start();

                    PrintStream printWriter = new PrintStream(outputStream);

                    // Write data
                    printWriter.print(method + " " + context + " HTTP/1.1\r\n");
                    printWriter.print("Accept: application/json\r\n");
                    printWriter.print("Connection: keep-alive\r\n");
                    printWriter.print("Authorization: Bearer "+ Bearer +"\r\n");
                    int contentLength = payload.getBytes().length;
                    // There is no possible partial client scenario with GET method or zero payload size.
                    if(contentLength == 0 | method == RequestMethod.GET){
                        System.out.println("Actual Content-Length: is: "+ contentLength +" but "+ method +" method so not sending Content-Length header");
                        printWriter.print("\r\n");
                        printWriter.flush();
                        sslSocket.close();
                        return;
                    }
                    System.out.println("Actual Content-Length is "+ contentLength +" but sending Content-Length " + contentLength + 100);
                    // Sending large Content-Length to make the client sending partial content
                    printWriter.print("Content-Length: "+contentLength + 100+"\r\n");
                    printWriter.print("Content-Type: application/json\r\n");
                    printWriter.print("\r\n");
                    printWriter.print(payload);
                    // Remove the eol to make the client sending partial content
                    //printWriter.print("\r\n");
                    printWriter.flush();
                    // Sleep the thread until socket timeout of the end server
                    Thread.sleep(10000);
                    printWriter.print("\r\n");
                    sslSocket.close();
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
        public void run(){
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
                //Assert that the caught exception is an instance of SocketException
                if (e instanceof SocketException) {
                    Assert.assertEquals("Socket closed", e.getMessage(), "Expected a Socket closed exception");
                    System.out.println("Socket closed");
                }else{
                    throw new RuntimeException(e);
                }
            }
        }
        public void waitForResponse() throws InterruptedException {
            while (!responseComplete) {
                Thread.sleep(10);
            }
        }
    }
}