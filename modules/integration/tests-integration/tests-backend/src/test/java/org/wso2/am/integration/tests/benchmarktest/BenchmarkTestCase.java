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

package org.wso2.am.integration.tests.benchmarktest;

import static io.restassured.RestAssured.given;
import io.restassured.RestAssured;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.am.admin.clients.registry.ResourceAdminServiceClient;
import org.wso2.am.integration.test.utils.base.APIMIntegrationBaseTest;
import org.wso2.carbon.automation.engine.annotations.ExecutionEnvironment;
import org.wso2.carbon.automation.engine.annotations.SetEnvironment;
import org.wso2.carbon.automation.engine.context.AutomationContext;
import org.wso2.carbon.integration.common.utils.mgt.ServerConfigurationManager;

    @SetEnvironment(executionEnvironments = {ExecutionEnvironment.STANDALONE})
    public class BenchmarkTestCase extends APIMIntegrationBaseTest {
        int statusCode;
        String publisherConsumerSecret;
        String publisherConsumerKey;
        String apiUUID;
        String applicationID;
        String accessToken;
        private final String APIM_URL = "https://localhost:9443";
        private final String TOKEN_URL = "https://localhost:8243";
        private final String TARGET_URL = "https://localhost:8243";
        private final String SUPER_TENANT_USERNAME = "admin";
        private final String SUPER_TENANT_PASSWORD = "admin";

        //        private static final Log log = LogFactory.getLog(org.wso2.am.integration.tests.tests.ConfigDeploymentConfig.class);
        private final String TENANT_CONFIG_LOCATION = "/_system/config/apimgt/applicationdata/tenant-conf.json";
        private ServerConfigurationManager serverConfigurationManager;
        private AutomationContext superTenantKeyManagerContext;
        private ResourceAdminServiceClient resourceAdminServiceClient;

        @BeforeClass(alwaysRun = true)
        public void setEnvironment() throws Exception {
            RestAssured.useRelaxedHTTPSValidation();
//            serverConfigurationManager = new ServerConfigurationManager(superTenantKeyManagerContext);
//            serverConfigurationManager.restartGracefully();
        }

        @Test
        public void createRestApitest() throws InterruptedException {
//            Thread.sleep(2222222);
            generateConsumerCredentials();
            createRestAPI("TestAPI","samplecontext");
            publishAPI(apiUUID);
            createAnApplication("MyTestAPP");
            addSubscription(apiUUID,applicationID);
            invokeAPI("samplecontext",generateApplicationToken(applicationID));
            deleteApplication(applicationID);
            deleteRestAPI(apiUUID);
        }

        public void generateConsumerCredentials() {
            Response response =
                given()
                    .auth()
                    .preemptive()
                    .basic(SUPER_TENANT_USERNAME, SUPER_TENANT_PASSWORD)
                    .header("Content-Type", "application/json").
                        when().
                        body("{\n" +
                             "  \"callbackUrl\": \"www.google.lk\",\n" +
                             "  \"clientName\": \"rest_api_publisher\",\n" +
                             "  \"owner\": \"admin\",\n" +
                             "  \"grantType\": \"password refresh_token\",\n" +
                             "  \"saasApp\": true\n" +
                             "}").
                        post(APIM_URL+"/client-registration/v0.17/register");
            String responseBody = response.getBody().asString();
            JsonPath jsonResponse = new JsonPath(responseBody);
            statusCode = response.getStatusCode();
            Assert.assertEquals(statusCode /*actual value*/, 200 /*expected value*/, "Incorrect status code returned");
            publisherConsumerKey = jsonResponse.getString("clientId");
            publisherConsumerSecret = jsonResponse.getString("clientSecret");
        }

        public String generateAccessToken(String scope) {
            Response response =
                given()
                    .auth()
                    .preemptive()
                    .basic(publisherConsumerKey, publisherConsumerSecret)
                    .formParam("grant_type","password")
                    .formParam("username",SUPER_TENANT_USERNAME)
                    .formParam("password",SUPER_TENANT_PASSWORD)
                    .formParam("scope",scope).
                        when().
                        post(TOKEN_URL+"/token");
            String responseBody = response.getBody().asString();
            JsonPath jsonResponse = new JsonPath(responseBody);
            statusCode = response.getStatusCode();
//            Assert.assertEquals(statusCode /*actual value*/, 200 /*expected value*/, "Incorrect status code returned");
            String accessToken = jsonResponse.getString("access_token");
      return accessToken;
        }

        public String createRestAPI(String apiName, String apiContext) {
            Response response =
                given().log().all()
                    .header("Authorization","Bearer "+ generateAccessToken("apim:api_create"))
                    .header("Content-Type", "application/json").
                        when().
                        body("{\"name\":\""+apiName+"\",\"version\":\"v1.0\",\"context\":\""+apiContext+"\"," +
                             "\"policies\":[\"Gold\"],\"endpointConfig\":{\"endpoint_type\":\"http\"," +
                             "\"sandbox_endpoints\":{\"url\":\"https://jsonplaceholder.typicode.com/\"}," +
                             "\"production_endpoints\":{\"url\":\"https://jsonplaceholder.typicode.com/\"}}," +
                             "\"gatewayEnvironments\":[\"Production and Sandbox\"]}").
                        post(APIM_URL+"/api/am/publisher/v1/apis");
            String responseBody = response.getBody().asString();
            JsonPath jsonResponse = new JsonPath(responseBody);
            response.then().log().all();
            statusCode = response.getStatusCode();
            Assert.assertEquals(statusCode /*actual value*/, 201 /*expected value*/, "Incorrect status code returned");
            apiUUID = jsonResponse.getString("id");
            return apiUUID;
        }

        public void deleteRestAPI(String apiID) {
            Response response =
                given().log().all()
                       .header("Authorization","Bearer "+ generateAccessToken("apim:api_delete"))
                       .header("Content-Type", "application/json").
                           when().
                           delete(APIM_URL+"/api/am/publisher/v1/apis/"+apiID);
            String responseBody = response.getBody().asString();
            JsonPath jsonResponse = new JsonPath(responseBody);
            response.then().log().all();
            statusCode = response.getStatusCode();
            Assert.assertEquals(statusCode /*actual value*/, 200 /*expected value*/, "Incorrect status code returned");
        }

        public void publishAPI(String apiID) {
            Response response =
                given().log().all()
                       .header("Authorization","Bearer "+ generateAccessToken("apim:api_publish"))
                       .header("Content-Type", "application/json")
                .queryParam("apiId",apiID)
                .queryParam("action","Publish").
                           when().
                           post(APIM_URL+"/api/am/publisher/v1/apis/change-lifecycle");
            String responseBody = response.getBody().asString();
            JsonPath jsonResponse = new JsonPath(responseBody);
            response.then().log().all();
            statusCode = response.getStatusCode();
            Assert.assertEquals(statusCode /*actual value*/, 200 /*expected value*/, "Incorrect status code returned");
        }

        public String createAnApplication(String appName) {
            Response response =
                given().log().all()
                       .header("Authorization","Bearer "+ generateAccessToken("apim:subscribe"))
                       .header("Content-Type", "application/json").
                           when().
                    body("{\"name\":\""+appName+"\",\"throttlingPolicy\":\"Unlimited\",\"description\":\"test\"," +
                         "\"tokenType\":\"JWT\",\"groups\":null,\"attributes\":{}}").
                           post(APIM_URL+"/api/am/store/v1/applications");
            String responseBody = response.getBody().asString();
            JsonPath jsonResponse = new JsonPath(responseBody);
            response.then().log().all();
            statusCode = response.getStatusCode();
            Assert.assertEquals(statusCode /*actual value*/, 201 /*expected value*/, "Incorrect status code returned");
            applicationID = jsonResponse.getString("applicationId");
            return applicationID;
        }

        public void addSubscription(String apiID, String appID) {
            Response response =
                given().log().all()
                       .header("Authorization","Bearer "+ generateAccessToken("apim:subscribe"))
                       .header("Content-Type", "application/json").
                           when().
                           body("{\"throttlingPolicy\":\"Gold\",\n" +
                                "    \"apiId\": \""+apiID+"\",\n" +
                                "    \"applicationId\": \""+appID+"\"}").
                           post(APIM_URL+"/api/am/store/v1/subscriptions");
            String responseBody = response.getBody().asString();
            JsonPath jsonResponse = new JsonPath(responseBody);
            response.then().log().all();
            statusCode = response.getStatusCode();
            Assert.assertEquals(statusCode /*actual value*/, 201 /*expected value*/, "Incorrect status code returned");
        }

        public String generateApplicationToken(String appID) {
            Response response =
                given().log().all()
                       .header("Authorization","Bearer "+ generateAccessToken("apim:subscribe"))
                       .header("Content-Type", "application/json").
                           when().
                           body("{\n" +
                                "  \"keyType\": \"PRODUCTION\",\n" +
                                "  \"grantTypesToBeSupported\": [\n" +
                                "    \"refresh_token\",\"urn:ietf:params:oauth:grant-type:saml2-bearer\"," +
                                "\"password\",\"client_credentials\",\"iwa:ntlm\"," +
                                "\"urn:ietf:params:oauth:grant-type:jwt-bearer\"\n" +
                                "  ],\n" +
                                "  \"callbackUrl\": \"string\",  \"scopes\": [\"am_application_scope\",\"default\"]," +
                                "\"validityTime\": 3600,\"clientId\": \"\",\"clientSecret\": \"\"," +
                                "\"additionalProperties\": \"\"\n" +
                                "}").
                           post(APIM_URL+"/api/am/store/v1/applications/"+appID+"/generate-keys");
            String responseBody = response.getBody().asString();
            JsonPath jsonResponse = new JsonPath(responseBody);
            response.then().log().all();
            statusCode = response.getStatusCode();
            Assert.assertEquals(statusCode /*actual value*/, 200 /*expected value*/, "Incorrect status code returned");
            String accessToken = jsonResponse.getString("accessToken");
            return accessToken;
        }

        public void deleteApplication(String appID) {
            Response response =
                given().log().all()
                       .header("Authorization","Bearer "+ generateAccessToken("apim:subscribe"))
                       .header("Content-Type", "application/json").
                           when().
                           delete(APIM_URL+"/api/am/store/v1/applications/"+appID);
            String responseBody = response.getBody().asString();
            JsonPath jsonResponse = new JsonPath(responseBody);
            response.then().log().all();
            statusCode = response.getStatusCode();
            Assert.assertEquals(statusCode /*actual value*/, 200 /*expected value*/, "Incorrect status code returned");
        }

        public void invokeAPI(String context, String token) {
            Response response =
                given().log().all()
                       .header("Authorization","Bearer "+ token).
                           when().
                           post(TARGET_URL+"/"+context+"/v1.0/posts/1");
            String responseBody = response.getBody().asString();
            JsonPath jsonResponse = new JsonPath(responseBody);
            response.then().log().all();
            statusCode = response.getStatusCode();
            Assert.assertEquals(statusCode /*actual value*/, 200 /*expected value*/, "Incorrect status code returned");
        }

    }

