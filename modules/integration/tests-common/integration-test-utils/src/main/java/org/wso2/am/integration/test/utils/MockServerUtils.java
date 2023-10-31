/*
 * Copyright (c) 2023, WSO2 LLC. (http://www.wso2.org) All Rights Reserved.
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
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.am.integration.test.utils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.net.Socket;
import java.util.Random;

public class MockServerUtils {
    private static final Log log = LogFactory.getLog(MockServerUtils.class);
    public static String LOCALHOST = "localhost";
    public static final int httpPortLowerRange = 8080;
    public static final int httpPortUpperRange = 8099;
    public static final int httpsPortLowerRange = 9950;
    public static final int httpsPortUpperRange = 9999;
    private static final int[] reservedPorts = new int[]{ 9960 };
    private static int httpOffset = 0;
    private static int httpsOffset = 0;
    static {
        Random random = new Random();
        httpOffset = random.nextInt(httpPortUpperRange - httpPortLowerRange + 1) + httpPortLowerRange;
        httpsOffset = random.nextInt(httpsPortUpperRange - httpsPortLowerRange + 1) + httpsPortLowerRange;
    }
    private static final Object lock = new Object();

    /**
     * Check whether give port is available
     *
     * @param port Port Number
     * @return status
     */
    private static boolean isPortFree(int port, String host) {
        if (isPortReserved(port)) {
            return false;
        }
        Socket s = null;
        try {
            s = new Socket(host, port);
            // something is using the port and has responded.
            return false;
        } catch (IOException e) {
            //port available
            return true;
        } finally {
            if (s != null) {
                try {
                    s.close();
                } catch (IOException e) {
                    throw new RuntimeException("Unable to close connection ", e);
                }
            }
        }
    }

    /**
     * Find a free port to start backend WebSocket server in given port range
     *
     * @param isHttps
     * @return Available Port Number
     */
    public static int getAvailablePort(String host, boolean isHttps) {
        synchronized (lock) {
            int offset;
            int upperPortLimit;
            int lowerPortLimit;
            if (isHttps) {
                offset = httpsOffset;
                upperPortLimit = httpsPortUpperRange;
                lowerPortLimit = httpsPortLowerRange;
            } else {
                offset = httpOffset;
                upperPortLimit = httpPortUpperRange;
                lowerPortLimit = httpPortLowerRange;
            }
            int portRangeLen = upperPortLimit - lowerPortLimit;
            int targetPort = lowerPortLimit + (offset % (portRangeLen + 1));
            for (int i = 0; i < portRangeLen; i++) {
                if (MockServerUtils.isPortFree(targetPort, host)) {
                    if (isHttps) {
                        httpsOffset = (httpsOffset + i + 1) % (portRangeLen + 1);
                    } else {
                        httpOffset = (httpOffset + i + 1) % (portRangeLen + 1);
                    }
                    log.info("Port " + targetPort + " selected for mock server.");
                    return targetPort;
                }
                targetPort ++;
                if (targetPort > upperPortLimit) {
                    targetPort = lowerPortLimit;
                }
            }
            return -1;
        }
    }

    private static boolean isPortReserved(int port) {
        for (int prohibitedPort : reservedPorts) {
            if (port == prohibitedPort) {
                return true;
            }
        }
        return false;
    }
}