/*
*Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
package org.wso2.am.integration.test.utils.generic;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class ServiceDeploymentUtil {

    private static Log log = LogFactory.getLog(ServiceDeploymentUtil.class);

    /**
     * Check whether the wsdl is exists or not
     *
     * @param wsdlURL            - URL of the wsdl
     * @param synchronizingDelay - time interval to wait until service get deployed on worker nodes.
     * @return - status of the wsdl availability
     * @throws IOException - Throws if given wsdl cannot be found
     */
    public static boolean isServiceWSDlExist(String wsdlURL, long synchronizingDelay)
            throws IOException {

        log.info("waiting " + synchronizingDelay + " millis for Proxy deployment in worker");

        boolean isServiceDeployed = false;
        long startTimeMs = System.currentTimeMillis();

        while ((System.currentTimeMillis() - startTimeMs) < synchronizingDelay) {
            if (isWSDLAvailable(wsdlURL)) {
                isServiceDeployed = true;
                log.info("Proxy Deployed in " + (System.currentTimeMillis() - startTimeMs) + " millis in worker");
                break;
            }
            try {
                Thread.sleep(500);//wait for 500ms for next iteration
            } catch (InterruptedException ignored) {
                log.warn("Thread  interrupted ", ignored);
            }
        }
        return isServiceDeployed;
    }

    /**
     * Check whether the wsdl is available
     *
     * @param serviceEndpoint - service endpoint (without ?wsdl)
     * @return - Status of wsdl availability
     * @throws IOException - Throws if wsdl cannot be found
     */
    public static boolean isWSDLAvailable(String serviceEndpoint) throws IOException {
        URL url = new URL(serviceEndpoint + "?wsdl");
        boolean isWsdlExist = false;
        BufferedReader rd = null;
        HttpURLConnection conn = null;

        try {
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setDoOutput(true);
            conn.setReadTimeout(6000);
            conn.connect();

            // Get the response
            StringBuilder sb = new StringBuilder();
            rd = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
            String line;
            while ((line = rd.readLine()) != null) {
                sb.append(line);
                if (sb.toString().contains("wsdl:definitions")) {
                    isWsdlExist = true;
                    break;
                }
            }

        } finally {
            if (rd != null) {
                try {
                    rd.close();
                } catch (IOException e) {
                    //ignored
                }
            }
            if (conn != null) {
                conn.disconnect();
            }
        }
        return isWsdlExist;

    }
}
