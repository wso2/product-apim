/*
 *Copyright (c) 2019, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.am.scenario.test.common;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;

import static org.apache.axis2.transport.http.HTTPConstants.USER_AGENT;

public class ScenarioTestUtils {

    private static final Log log = LogFactory.getLog(ScenarioTestUtils.class);

    public static String readFromFile(String file_name) throws IOException {

        BufferedReader br = new BufferedReader(new FileReader(file_name));
        StringBuilder sb = new StringBuilder();
        int x;
        while ((x = br.read()) != -1) {
            sb.append((char) x);
        }
        String payloadText = sb.toString();
        return payloadText;
    }

    public static String readFromURL(String url) throws Exception {

        StringBuilder sb = new StringBuilder();

        URL obj = new URL(url);

        BufferedReader in = null;
        HttpURLConnection con = null;
        try {
            con = (HttpURLConnection) obj.openConnection();

            con.setRequestMethod("GET");

            con.setRequestProperty("User-Agent", USER_AGENT);
            int responseCode = 0;
            responseCode = con.getResponseCode();
            System.out.println("GET Response Code :: " + responseCode);
            if (responseCode == HttpURLConnection.HTTP_OK) { // success
                in = new BufferedReader(new InputStreamReader(
                        con.getInputStream(), Charset.forName("UTF-8")));
                int x = -2;
                while ((x = in.read()) != -1) {
                    sb.append((char) x);
                }
                in.close();
            }
        } catch (IOException e) {
            log.error("Error in reading url : " + url, e);
        } finally {
            if (con != null) {
                con.disconnect();
            }
        }
        return sb.toString();
    }
}