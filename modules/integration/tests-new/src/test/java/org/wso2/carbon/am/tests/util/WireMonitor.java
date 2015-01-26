/*
 * Copyright (c) 2005-2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 * 
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.am.tests.util;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

class WireMonitor extends Thread {
    private Log log = LogFactory.getLog(WireMonitor.class);
    private static final int TIMEOUT_VALUE = 60000;
    private int port;
    private ServerSocket providerSocket;
    private Socket connection = null;
    public String message = "";
    private WireMonitorServer trigger;

    public void run() {
        try {

            // creating a server socket
            providerSocket = new ServerSocket(port, 10);

            log.info("Waiting for connection");
            connection = providerSocket.accept();
            log.info("Connection received from " +
                     connection.getInetAddress().getHostName());
            InputStreamReader in = new InputStreamReader(connection.getInputStream());
            BufferedReader rd = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String line = "";
            Long time = System.currentTimeMillis();
            while ((line = rd.readLine()) != null && !line.equals("")) {
                message = message + line;
                // In this case no need of reading more than timeout value
                if (System.currentTimeMillis() > (time + TIMEOUT_VALUE)) {
                    break;
                }
            }

            // Signaling Main thread to continue
            trigger.response = message;
            trigger.isFinished = true;

            //Sending default response
            PrintWriter out = new PrintWriter(connection.getOutputStream());
            String responseText = "[Response] Request Received. This is the default Response.";
            String httpResponse = "HTTP/1.1 200 OK\n" +
                    "Content-Type: text/xml;charset=utf-8\n" +
                    "Content-Length: " + responseText.length() + "\n" +
                    "\n" +
                    responseText;
            out.write(httpResponse);
            out.flush();

            in.close();
            rd.close();
            out.close();

        } catch (IOException ioException) {
            ioException.printStackTrace();
        } finally {
            try {
                connection.close();
                providerSocket.close();
            } catch (Exception e) {

            }
        }

    }

    public WireMonitor(int listenPort, WireMonitorServer trigger) {
        port = listenPort;
        this.trigger = trigger;
    }

}
