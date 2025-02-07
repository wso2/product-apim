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

package org.wso2.am.integration.tests.header;

import org.apache.axiom.om.OMElement;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLContextBuilder;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.wso2.am.integration.test.utils.base.APIMIntegrationBaseTest;
import org.wso2.am.integration.test.utils.generic.APIMTestCaseUtils;
import org.wso2.carbon.automation.engine.annotations.ExecutionEnvironment;
import org.wso2.carbon.automation.engine.annotations.SetEnvironment;
import org.wso2.carbon.integration.common.admin.client.AuthenticatorClient;

import java.io.*;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Scanner;

/**
 * This test class will test the TLS version when gateway call happen to the BE.
 * In Order to run this test case you should start your product in SSL debug enabled.
 * ./wso2server.sh -Djavax.net.debug=ssl:handshake > /tmp/ssl_debug.log and file will be saved to given location.
 */
public class APIMANAGER4568TLSTestCase extends APIMIntegrationBaseTest {

    static int port = 443;
    static String addressString = "http://127.0.0.1:8280/ContentTypeAPI";
    static String searchText = "*** ClientHello, TLSv1.2";

    PrintWriter out;
    BufferedReader in;

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {
        super.init();

        AuthenticatorClient login = new AuthenticatorClient(gatewayContextMgt.getContextUrls().getBackEndUrl());
        String session = login.login("admin", "admin", "localhost");
        // Upload the synapse
        String file = "artifacts" + File.separator + "AM" + File.separator + "synapseconfigs" +
                File.separator + "property" + File.separator +
                "TLS_TEST.xml";
        OMElement synapseConfig = APIMTestCaseUtils.loadResource(file);
        APIMTestCaseUtils.updateSynapseConfiguration(synapseConfig, gatewayContextMgt.getContextUrls().getBackEndUrl(),
                session);
        Thread.sleep(5000);
    }

    @SetEnvironment(executionEnvironments = {ExecutionEnvironment.ALL})
    @org.testng.annotations.Test(groups = "wso2.am",
            description = "Test TSL version")
    public void testTLS_VERSIONTest() throws Exception {

        SSLContextBuilder builder = new SSLContextBuilder();
        builder.loadTrustMaterial(null, new TrustStrategy() {
            public boolean isTrusted(X509Certificate[] chain, String authType)
                    throws CertificateException {
                return true;
            }
        });
        SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(
                builder.build());
        CloseableHttpClient httpclient = HttpClients.custom().setSSLSocketFactory(
                sslsf).build();

        HttpGet httpget = new HttpGet(addressString);
        httpclient.execute(httpget);

        File f = new File("/tmp/ssl_debug.log");

        boolean isTextThere = find(f, searchText);

        Assert.assertTrue(isTextThere, "TSL version  1.2 is not found!!");

    }

    public static boolean find(File f, String searchString) {
        boolean result = false;
        Scanner in = null;
        try {
            in = new Scanner(new FileReader(f));
            while (in.hasNextLine() && !result) {
                result = in.nextLine().indexOf(searchString) >= 0;
            }
        } catch (IOException e) {
            Assert.fail("An exception thrown while reading log file");
        } finally {
            try {
                in.close();
            } catch (Exception e) { /* ignore */ }
        }
        return result;
    }

    @AfterClass(alwaysRun = true)
    public void stop() throws Exception {
        cleanUp();
    }
}