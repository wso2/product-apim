/*
*Copyright (c) 2005-2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
*WSO2 Inc. licenses this file to you under the Apache License,
*Version 2.0 (the "License"); you may not use this file except
*in compliance with the License.
*You may obtain a copy of the License at
*
*http://www.apache.org/licenses/LICENSE-2.0
*
*Unless required by applicable law or agreed to in writing,
*software distributed under the License is distributed on an
*"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
*KIND, either express or implied.  See the License for the
*specific language governing permissions and limitations
*under the License.
*/
package org.wso2.am.integration.tests.header.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Simple java socket server.
 */
public class SimpleSocketServer extends Thread {

    private int port;
    private String expectedOutput;
    private ServerSocket serverSocket;

    public SimpleSocketServer(int port, String expectedOutput) {
        this.port = port;
        this.expectedOutput = expectedOutput;
    }

    public void run() {

        try {
            serverSocket = new ServerSocket(port);
            System.err.println("Server starting on port : " + port);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.err.println("Client connected");
                BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                BufferedWriter out = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream()));

                while (true) {
                    String s;
                    if ((s = in.readLine()) != null) {
                        System.out.println(s);
                        if (!s.isEmpty()) {
                            continue;
                        }
                    }

                    out.write(expectedOutput);
                    System.err.println("connection terminated");
                    out.close();
                    in.close();
                    clientSocket.close();
                    break;
                }

            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }


    public void shutdown() {
        if (serverSocket != null && !serverSocket.isClosed()) {
            try {
                System.err.println("Server shutting down");
                serverSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


}
