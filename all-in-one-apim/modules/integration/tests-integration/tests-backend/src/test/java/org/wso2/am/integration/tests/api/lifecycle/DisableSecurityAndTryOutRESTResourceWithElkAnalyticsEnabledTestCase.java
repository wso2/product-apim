/*
 * Copyright (c) 2023, WSO2 LLC. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 LLC. licenses this file to you under the Apache License,
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

package org.wso2.am.integration.tests.api.lifecycle;

import org.json.JSONException;
import org.testng.ITestContext;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.am.integration.clients.publisher.api.ApiException;
import org.wso2.am.integration.test.utils.APIManagerIntegrationTestException;
import org.wso2.am.integration.test.utils.base.APIManagerLifecycleBaseTest;
import org.wso2.carbon.automation.test.utils.http.client.HttpRequestUtil;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;
import org.wso2.carbon.integration.common.utils.exceptions.AutomationUtilException;
import org.wso2.carbon.integration.common.utils.mgt.ServerConfigurationManager;

import javax.xml.xpath.XPathExpressionException;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import static org.junit.Assert.assertNotEquals;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;
import static org.testng.AssertJUnit.assertNotNull;

/**
 * Add , edit and remove rest resource and test the invocation of API
 */
public class DisableSecurityAndTryOutRESTResourceWithElkAnalyticsEnabledTestCase extends APIManagerLifecycleBaseTest {
    private final String API_VERSION_1_0_0 = "1.0.0";
    private final String INVOKABLE_API_CONTEXT = API_VERSION_1_0_0 + "/api";

    private final String RESPONSE_GET = "<id>123</id><name>John</name></Customer>";
    private final String API_GET_ENDPOINT_METHOD = "/customers/123";

    private HashMap<String, String> requestHeadersGet;
    private ITestContext ctx;

    private ServerConfigurationManager serverConfigurationManager;

    @BeforeClass(alwaysRun = true)
    public void initialize(ITestContext ctx) throws APIManagerIntegrationTestException, XPathExpressionException,
            JSONException, ApiException, IOException, AutomationUtilException {

        super.init();
        serverConfigurationManager = new ServerConfigurationManager(superTenantKeyManagerContext);

        serverConfigurationManager.applyConfiguration(new File(getAMResourceLocation()
                + File.separator + "configFiles" + File.separator + "ElkAnalytics" +
                File.separator + "deployment.toml"));

        requestHeadersGet = new HashMap<String, String>();
        requestHeadersGet.put("accept", "text/xml");

        this.ctx = ctx;
        String apiId = (String) ctx.getAttribute("apiId");
        // Create Revision and Deploy to Gateway
        createAPIRevisionAndDeployUsingRest(apiId, restAPIPublisher);
        waitForAPIDeployment();
    }

    @Test(groups = {"webapp"}, description = "Test the invocation of GET resource")
    public void testTurnOffSecurityAndInvokeGETResource(ITestContext ctx) throws Exception {
        String apiId = (String) ctx.getAttribute("apiId");

        String oldSwagger = restAPIPublisher.getSwaggerByID(apiId);
        // resource are modified by using swagger doc. create the swagger doc with modified information.

        String modifiedResource = "{\n" +
                "  \"openapi\" : \"3.0.1\",\n" +
                "  \"info\" : {\n" +
                "    \"title\" : \"APITest\",\n" +
                "    \"description\" : \"description\",\n" +
                "    \"version\" : \"1.0.0\"\n" +
                "  },\n" +
                "  \"servers\" : [ {\n" +
                "    \"url\" : \"/\"\n" +
                "  } ],\n" +
                "  \"security\" : [ {\n" +
                "    \"default\" : [ ]\n" +
                "  } ],\n" +
                "  \"paths\" : {\n" +
                "    \"/\" : {\n" +
                "      \"get\" : {\n" +
                "        \"responses\" : {\n" +
                "          \"200\" : {\n" +
                "            \"description\" : \"OK\"\n" +
                "          }\n" +
                "        },\n" +
                "        \"security\" : [ ],\n" +
                "        \"x-auth-type\" : \"Application & Application User\",\n" +
                "        \"x-throttling-tier\" : \"Unlimited\",\n" +
                "        \"x-wso2-application-security\" : {\n" +
                "          \"security-types\" : [ \"oauth2\" ],\n" +
                "          \"optional\" : false\n" +
                "        }\n" +
                "      }\n" +
                "    },\n" +
                "    \"/customers/{id}\" : {\n" +
                "      \"get\" : {\n" +
                "        \"parameters\" : [ {\n" +
                "          \"name\" : \"id\",\n" +
                "          \"in\" : \"path\",\n" +
                "          \"required\" : true,\n" +
                "          \"style\" : \"simple\",\n" +
                "          \"explode\" : false,\n" +
                "          \"schema\" : {\n" +
                "            \"type\" : \"string\"\n" +
                "          }\n" +
                "        } ],\n" +
                "        \"responses\" : {\n" +
                "          \"200\" : {\n" +
                "            \"description\" : \"OK\"\n" +
                "          }\n" +
                "        },\n" +
                "        \"security\" : [ ],\n" +
                "        \"x-auth-type\" : \"None\",\n" +
                "        \"x-throttling-tier\" : \"Unlimited\",\n" +
                "        \"x-wso2-application-security\" : {\n" +
                "          \"security-types\" : [ \"oauth2\" ],\n" +
                "          \"optional\" : false\n" +
                "        }\n" +
                "      }\n" +
                "    }\n" +
                "  },\n" +
                "  \"components\" : {\n" +
                "    \"securitySchemes\" : {\n" +
                "      \"default\" : {\n" +
                "        \"type\" : \"oauth2\",\n" +
                "        \"flows\" : {\n" +
                "          \"implicit\" : {\n" +
                "            \"authorizationUrl\" : \"https://test.com\",\n" +
                "            \"scopes\" : { }\n" +
                "          }\n" +
                "        }\n" +
                "      }\n" +
                "    }\n" +
                "  },\n" +
                "  \"x-wso2-auth-header\" : \"Authorization\",\n" +
                "  \"x-wso2-api-key-header\" : \"ApiKey\",\n" +
                "  \"x-wso2-cors\" : {\n" +
                "    \"corsConfigurationEnabled\" : false,\n" +
                "    \"accessControlAllowOrigins\" : [ ],\n" +
                "    \"accessControlAllowCredentials\" : false,\n" +
                "    \"accessControlAllowHeaders\" : [ ],\n" +
                "    \"accessControlAllowMethods\" : [ ]\n" +
                "  },\n" +
                "  \"x-wso2-production-endpoints\" : {\n" +
                "    \"urls\" : [ \"http://localhost:10263/jaxrs_basic/services/customers/customerservice/\" ],\n" +
                "    \"type\" : \"http\"\n" +
                "  },\n" +
                "  \"x-wso2-sandbox-endpoints\" : {\n" +
                "    \"urls\" : [ \"http://localhost:10263/jaxrs_basic/services/customers/customerservice/\" ],\n" +
                "    \"type\" : \"http\"\n" +
                "  },\n" +
                "  \"x-wso2-basePath\" : \"/1.0.0/api\",\n" +
                "  \"x-wso2-transports\" : [ \"http\", \"https\" ],\n" +
                "  \"x-wso2-application-security\" : {\n" +
                "    \"security-types\" : [ \"oauth2\" ],\n" +
                "    \"optional\" : false\n" +
                "  },\n" +
                "  \"x-wso2-response-cache\" : {\n" +
                "    \"enabled\" : false,\n" +
                "    \"cacheTimeoutInSeconds\" : 100\n" +
                "  }\n" +
                "}";
        String swaggerResponse = restAPIPublisher.updateSwagger(apiId, modifiedResource);
        assertNotNull(swaggerResponse);

        String updatedSwagger = restAPIPublisher.getSwaggerByID(apiId);
        assertNotEquals(updatedSwagger, oldSwagger, "Modifying resources failed for API");

        createAPIRevisionAndDeployUsingRest(apiId, restAPIPublisher);
        waitForAPIDeployment();

        requestHeadersGet.remove("Authorization");

        //Send GET Request

        HttpResponse httpResponse =
                HttpRequestUtil.doGet(getAPIInvocationURLHttp(INVOKABLE_API_CONTEXT) + API_GET_ENDPOINT_METHOD,
                        requestHeadersGet);
        assertEquals(httpResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK, "Invocation fails for GET request");
        assertTrue(httpResponse.getData().contains(RESPONSE_GET), "Response Data not match for GET request." +
                " Expected value :\"" + RESPONSE_GET + "\" not contains in response data:\"" + httpResponse.getData() + "\"");

    }

    @AfterClass(alwaysRun = true)
    public void removeApplicationSharingConfig() throws Exception {
        serverConfigurationManager.restoreToLastConfiguration(false);
    }

}
