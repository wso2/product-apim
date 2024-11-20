/*
 * Copyright (c) 2024, WSO2 LLC. (http://www.wso2.org) All Rights Reserved.
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

import java.io.IOException;
import java.net.Socket;

public class ServerPortsUtils {

    public static String LOCALHOST = "localhost";
    public static final int httpPortLowerRange = 8080;
    public static final int httpPortUpperRange = 8099;
    public static final int httpsPortLowerRange = 9950;
    public static final int httpsPortUpperRange = 9999;

    /**
     * Check whether give port is available
     *
     * @param port Port Number
     * @return status
     */
    private static boolean isPortFree(int port, String host) {

        Socket s = null;
        try {
            s = new Socket(host, port);
            // Something is using the port and has responded.
            return false;
        } catch (IOException e) {
            // Port available
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

    public static int getAvailableHttpPort(String host) {
        return getAvailablePort(httpPortLowerRange, httpPortUpperRange, host);
    }

    public static int getAvailableHttpsPort(String host) {
        return getAvailablePort(httpsPortLowerRange, httpsPortUpperRange, host);
    }

    /**
     * Find a free port to start backend WebSocket server in given port range
     *
     * @param lowerPortLimit from port number
     * @param upperPortLimit to port number
     * @return Available Port Number
     */
    private static int getAvailablePort(int lowerPortLimit, int upperPortLimit, String host) {
        while (lowerPortLimit < upperPortLimit) {
            if (ServerPortsUtils.isPortFree(lowerPortLimit, host)) {
                return lowerPortLimit;
            }
            lowerPortLimit += 1;
        }
        return -1;
    }

}
