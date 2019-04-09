/*
 * Copyright (c) 2019, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.wso2.am.scenario.test.common.httpserver;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Date;
import java.util.StringTokenizer;

public class SimpleHTTPServer implements Runnable {

    static final File WEB_ROOT = new File(System.getProperty("test.resource.location") + File.separator);
    static final String DEFAULT_FILE = "index.html";
    static final String FILE_NOT_FOUND = "404.html";
    static final String METHOD_NOT_SUPPORTED = "not_supported.html";
    // port to listen connection
    static final int PORT = 8083;

    // Client Connection via Socket Class
    private Socket connect;
    private static final Log log = LogFactory.getLog(SimpleHTTPServer.class);
    private static boolean isRunning = true;
    private static ServerSocket serverConnect;

    public SimpleHTTPServer() {

        try {
            serverConnect = new ServerSocket(PORT);
            log.info("Server started.\nListening for connections on port : " + PORT + " ...\n");
            isRunning = true;
        } catch (IOException e) {
            System.err.println("Server Connection error : " + e.getMessage());
        }
    }

    public SimpleHTTPServer(int serverPort) {

        try {
            serverConnect = new ServerSocket(serverPort);
            log.info("Server started.\nListening for connections on port : " + serverPort + " ...\n");

        } catch (IOException e) {
            System.err.println("Server Connection error : " + e.getMessage());
        }
    }

    @Override
    public void run() {

        try {
            connect = serverConnect.accept();
        } catch (IOException e) {
            e.printStackTrace();
        }

        // we manage our particular client connection
        BufferedReader in = null;
        PrintWriter out = null;
        BufferedOutputStream dataOut = null;
        String fileRequested = null;

        if (isRunning) {
            try {
                // we read characters from the client via input stream on the socket
                in = new BufferedReader(new InputStreamReader(connect.getInputStream()));
                // we get character output stream to client (for headers)
                out = new PrintWriter(connect.getOutputStream());
                // get binary output stream to client (for requested data)
                dataOut = new BufferedOutputStream(connect.getOutputStream());

                // get first line of the request from the client
                String input = in.readLine();
                // we parse the request with a string tokenizer
                StringTokenizer parse = new StringTokenizer(input);
                String method = parse.nextToken().toUpperCase(); // we get the HTTP method of the client
                // we get file requested
                fileRequested = parse.nextToken();

                // we support only GET and HEAD methods, we check
                if (!method.equals("GET") && !method.equals("HEAD")) {
                    log.warn("501 Not Implemented : " + method + " method.");

                    // we return the not supported file to the client
                    File file = new File(WEB_ROOT, METHOD_NOT_SUPPORTED);
                    int fileLength = (int) file.length();
                    String contentMimeType = "text/html";
                    //read content to return to client
                    byte[] fileData = readFileData(file, fileLength);

                    // we send HTTP Headers with data to client
                    out.println("HTTP/1.1 501 Not Implemented");
                    out.println("Server: Java HTTP Server from SSaurel : 1.0");
                    out.println("Date: " + new Date());
                    out.println("Content-type: " + contentMimeType);
                    out.println("Content-length: " + fileLength);
                    out.println(); // blank line between headers and content, very important !
                    out.flush(); // flush character output stream buffer
                    // file
                    dataOut.write(fileData, 0, fileLength);
                    dataOut.flush();

                } else {
                    // GET or HEAD method
                    if (fileRequested.endsWith("/")) {
                        fileRequested += DEFAULT_FILE;
                    }
                    File file = new File(WEB_ROOT, fileRequested);
                    int fileLength = (int) file.length();
                    String content = getContentType(fileRequested);

                    if (method.equals("GET")) { // GET method so we return content
                        byte[] fileData = readFileData(file, fileLength);

                        // send HTTP Headers
                        out.println("HTTP/1.1 200 OK");
                        out.println("Server: Java HTTP Server from SSaurel : 1.0");
                        out.println("Date: " + new Date());
                        out.println("Content-type: " + content);
                        out.println("Content-length: " + fileLength);
                        out.println(); // blank line between headers and content, very important !
                        out.flush(); // flush character output stream buffer

                        dataOut.write(fileData, 0, fileLength);
                        dataOut.flush();
                    }

                    if (log.isDebugEnabled()) {
                        log.debug("File " + fileRequested + " of type " + content + " returned");
                    }
                }
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    // Ignore
                }

            } catch (FileNotFoundException fnfe) {
                log.error("Error while creating the connection : " + fnfe.getMessage(), fnfe);

            } catch (NullPointerException npe) {
                log.error("Error with file not found exception : " + npe.getMessage());

            } catch (IOException ioe) {
                log.error("Server error : " + ioe);
            } finally {
                try {
                    if (in != null) {
                        in.close();
                    }
                    out.close();
                    dataOut.close();
                    connect.close(); // close socket connection
                    closeServerConnection(serverConnect);
                } catch (Exception e) {
                    log.error("Error closing stream : " + e.getMessage());
                }

                log.debug("Connection closed.\n");
            }
        }

    }

    private void closeServerConnection(ServerSocket server) {

        if (server != null && !server.isClosed()) {
            try {
                server.close();
            } catch (IOException e) {
                log.error(System.err);
            }
        }
    }

    public void stop() {

        isRunning = false;
        closeServerConnection(serverConnect);
        try {
            if (connect != null && !connect.isClosed()) {
                connect.close();
            }
        } catch (IOException e) {
            //Ignore
        }
    }

    private byte[] readFileData(File file, int fileLength) throws IOException {

        FileInputStream fileIn = null;
        byte[] fileData = new byte[fileLength];

        try {
            fileIn = new FileInputStream(file);
            fileIn.read(fileData);
        } finally {
            if (fileIn != null)
                fileIn.close();
        }

        return fileData;
    }

    // return supported MIME Types
    private String getContentType(String fileRequested) {

        if (fileRequested.endsWith(".htm") || fileRequested.endsWith(".html"))
            return "text/html";
        else
            return "text/plain";
    }

}