/*
 *   Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *   WSO2 Inc. licenses this file to you under the Apache License,
 *   Version 2.0 (the "License"); you may not use this file except
 *   in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 *
 */

package org.wso2.am.integration.tests.header;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.wso2.am.integration.test.utils.base.APIMIntegrationBaseTest;
import org.wso2.am.integration.test.utils.monitor.utils.WireMonitorServer;
import org.wso2.carbon.automation.engine.annotations.ExecutionEnvironment;
import org.wso2.carbon.automation.engine.annotations.SetEnvironment;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * This class is used to check the functionality of gateway header splitting.
 * <p>
 * Note: By default an API will always have a version. Ex: 1.0.0. It is mandatory that the API request url contains this
 * version field. Ex: http://localhost:8280/twitter/1.0.0. By specifying a particular version of an API as the 'default'
 * versioned API, it makes it possible to invoke that particular api without having the version as part of the request
 * url. Ex: http://localhost:8280/twitter
 * </p>
 */
public class HeaderSplitingTestCase extends APIMIntegrationBaseTest {


    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {
        super.init();
    }

    @SetEnvironment(executionEnvironments = { ExecutionEnvironment.ALL })
    @org.testng.annotations.Test(groups = "wso2.am",
            description = "Test for gateway header splitting")
    public void testHeaderSplitting() throws Exception {

        // We only need to access the gateway to see if header splitting is happening no need to invoke
        // and API
        String apiInvocationUrl = getAPIInvocationURLHttp("SampleAPI");

        Map<String, String> headers = new HashMap<String, String>();
        headers.put("SplitHeader", "Sample");
        //Invoke the API
        HttpResponse httpResponse = doGet(apiInvocationUrl, headers);

        //Check if accessing the back-end directly and accessing it via the API yield the same responses.
        if(httpResponse.getHeaders() != null) {
            Assert.assertNull(httpResponse.getHeaders().get("SplitHeader"), "The Header spliting patch failed.");
        }
        else{
            Assert.assertFalse(true, "The Header spliting patch failed.");
        }
    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
        super.cleanUp();
    }

    private static HttpResponse doGet(String endpoint, Map<String, String> headers) throws
                                                                                   IOException {
        if(!endpoint.startsWith("http://")) {
            return null;
        } else {
            URL url = new URL(endpoint);
            HttpURLConnection conn = (HttpURLConnection)url.openConnection();
            conn.setRequestMethod("GET");
            conn.setDoOutput(true);
            conn.setReadTimeout(30000);
            if(headers != null && headers.size() > 0) {
                Iterator sb = headers.keySet().iterator();
                String rd = (String)sb.next();
                conn.setRequestProperty(rd, (String)headers.get(rd));
            }

            conn.connect();
            StringBuilder sb1 = new StringBuilder();
            BufferedReader rd1 = null;

            HttpResponse httpResponse;
            try {
                rd1 = new BufferedReader(new InputStreamReader(conn.getInputStream()));

                String ignored;
                while((ignored = rd1.readLine()) != null) {
                    sb1.append(ignored);
                }

                httpResponse = new HttpResponse(sb1.toString(), conn.getResponseCode());
                httpResponse.setResponseMessage(conn.getResponseMessage());
            } catch (IOException var12) {
                rd1 = new BufferedReader(new InputStreamReader(conn.getErrorStream()));

                String line;
                while((line = rd1.readLine()) != null) {
                    sb1.append(line);
                }

                httpResponse = new HttpResponse(sb1.toString(), conn.getResponseCode());
                httpResponse.setResponseMessage(conn.getResponseMessage());
            } finally {
                if(rd1 != null) {
                    rd1.close();
                }

            }

            Iterator itr1 = conn.getHeaderFields().keySet().iterator();
            headers = new HashMap();

            while(itr1.hasNext()) {
                String key = (String)itr1.next();
                if(key != null) {
                    headers.put(key, conn.getHeaderField(key));
                }
            }

            httpResponse.setHeaders(headers);

            return httpResponse;
        }
    }
}
