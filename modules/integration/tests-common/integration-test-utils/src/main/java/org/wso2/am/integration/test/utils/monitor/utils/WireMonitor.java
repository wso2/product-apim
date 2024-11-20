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

package org.wso2.am.integration.test.utils.monitor.utils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.Charset;

class WireMonitor extends Thread {
    private Log log = LogFactory.getLog(WireMonitor.class);
    private int port;
    private ServerSocket providerSocket;
    private Socket connection = null;
    private WireMonitorServer trigger;

    public void run() {

        try (ServerSocket providerSocket = new ServerSocket(port, 10);
            Socket connection = providerSocket.accept();
            InputStream in = connection.getInputStream();
            OutputStream out = connection.getOutputStream()) {
            // creating a server socket
            log.info("Waiting for connection");
            log.info("Connection received from " + connection.getInetAddress().getHostName());
            int ch;
            StringBuilder buffer = new StringBuilder();
            StringBuffer headerBuffer = new StringBuffer();
            Long time = System.currentTimeMillis();
            int contentLength = -1;
            while ((ch = in.read()) != 1 && in.available() > 0) {
                buffer.append((char) ch);
                //message headers end with
                if (contentLength == -1 && buffer.toString().endsWith("\r\n\r\n")) {
                    headerBuffer = new StringBuffer(buffer.toString());
                    if (buffer.toString().contains("Content-Length")) {
                        String headers = buffer.toString();
                        //getting content-length header
                        String contentLengthHeader = headers.substring(headers.indexOf("Content-Length:"));
                        contentLengthHeader = contentLengthHeader.substring(0, contentLengthHeader.indexOf("\r\n"));
                        contentLength = Integer.parseInt(contentLengthHeader.split(":")[1].trim());
                        //clear the buffer
                        buffer.setLength(0);
                    }
                }

                //braking loop since whole message is read
                if (buffer.toString().length() == contentLength) {
                    break;
                }
                // In this case no need of reading more than timeout value
                if ((System.currentTimeMillis() > (time + trigger.READ_TIME_OUT)) || buffer.toString()
                        .contains("</soapenv:Envelope>")) {
                    break;
                }
            }

            // Signaling Main thread to continue
            trigger.response = headerBuffer.toString() + buffer.toString();
            trigger.setFinished(true);

            
            out.write(("HTTP/1.1 202 Accepted" + "\r\n\r\n").getBytes(Charset.defaultCharset()));
            out.flush();
        } catch (IOException ioException) {
            throw new IllegalStateException("Wire monitor error occurred", ioException);
        } catch (Exception e) {
            log.warn("Error occurred", e);
        }
    }

    public WireMonitor(int listenPort, WireMonitorServer trigger) {
        port = listenPort;
        this.trigger = trigger;
    }
}
