/*
 * Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.am.integration.tests.streamingapis;

import java.io.IOException;
import java.net.Socket;

/**
 * Contains utility methods for Streaming API test cases.
 */
public class StreamingApiTestUtils {

    /**
     * Returns the available port, within the range of given lower and upper ports, in the given host.
     * @param lowerPortLimit    Lower port limit
     * @param upperPortLimit    Upper port limit
     * @param host              Host
     * @return                  Available port
     */
    public static int getAvailablePort(int lowerPortLimit, int upperPortLimit, String host) {
        while (lowerPortLimit < upperPortLimit) {
            if (isPortFree(lowerPortLimit, host)) {
                return lowerPortLimit;
            }
            lowerPortLimit += 1;
        }
        return -1;
    }

    private static boolean isPortFree(int port, String host) {
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
}
