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
package org.wso2.am.integration.tests.rest;

import org.apache.http.*;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import org.wso2.am.integration.test.utils.base.APIMIntegrationBaseTest;
import org.wso2.carbon.automation.engine.context.TestUserMode;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

public class MalformedRequestTest extends APIMIntegrationBaseTest {

    @DataProvider
    public static Object[][] userModeDataProvider() {
        return new Object[][]{
                new Object[]{TestUserMode.SUPER_TENANT_ADMIN},
                new Object[]{TestUserMode.TENANT_ADMIN},
        };
    }

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {

        super.init();

        String gatewaySessionCookie = createSession(gatewayContextMgt);

        //Load the back-end dummy API
        if(TestUserMode.SUPER_TENANT_ADMIN == userMode) {
            loadSynapseConfigurationFromClasspath("artifacts" + File.separator + "AM"
                    + File.separator + "synapseconfigs" + File.separator + "rest"
                    + File.separator + "dummy_api.xml", gatewayContextMgt, gatewaySessionCookie);
        }
    }

    @Test(groups = "wso2.am", description = "Check if a malformed request breaks the system")
    public void testMalformedPostWithMessageBuilding() {

        HttpClient httpclient = HttpClientBuilder.create().build();
        HttpPost httppost = new HttpPost(getGatewayURLNhttp() + "response");

        httppost.addHeader(new Header() {
            @Override
            public String getName() {
                return "Content-Type";
            }

            @Override
            public String getValue() {
                return "application/xml";
            }

            @Override
            public HeaderElement[] getElements() throws ParseException {
                return new HeaderElement[0];
            }
        });

        String malformedBody = "<request>Request<request>";
        HttpResponse response = null;

        try {
            HttpEntity entity = new ByteArrayEntity(malformedBody.getBytes("UTF-8"));
            httppost.setEntity(entity);
            response = httpclient.execute(httppost);
        } catch (ClientProtocolException e) {
            //Fail the test case.
            Assert.assertTrue(false, e.getMessage());
        } catch (UnsupportedEncodingException e) {
            //Fail the test case.
            Assert.assertTrue(false, e.getMessage());
        } catch (IOException e) {
            //Fail the test case.
            Assert.assertTrue(false, e.getMessage());
        }

        Assert.assertNotNull(response, "Received null response for malformed post");

        Assert.assertEquals(response.getStatusLine().getStatusCode(), 500,
                                                        "Did not receive an http 500 for the malformed request");
    }

}
