/*
 * Copyright (c) 2016, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.wso2.am.integration.tests.header;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.Header;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.json.JSONException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;
import org.wso2.am.integration.clients.publisher.api.ApiException;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIOperationsDTO;
import org.wso2.am.integration.clients.store.api.ApiResponse;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyGenerateRequestDTO;
import org.wso2.am.integration.test.utils.APIManagerIntegrationTestException;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.bean.APILifeCycleAction;
import org.wso2.am.integration.test.utils.bean.APIRequest;
import org.wso2.am.integration.tests.api.lifecycle.APIManagerLifecycleBaseTest;
import org.wso2.carbon.automation.engine.annotations.ExecutionEnvironment;
import org.wso2.carbon.automation.engine.annotations.SetEnvironment;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.engine.context.beans.User;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.core.Response;
import javax.xml.xpath.XPathExpressionException;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

/**
 * Test CORS Access-Control-Allow-Credentials functionality
 */
@SetEnvironment(executionEnvironments = {ExecutionEnvironment.STANDALONE})
public class CORSAccessControlAllowCredentialsHeaderTestCase extends APIManagerLifecycleBaseTest {

    private static final String API_NAME_2 = "CorsACACHeadersTestAPI_2";
    private static final String APPLICATION_NAME_2 = "CorsACACApp_2";
    private static final String API_CONTEXT_2 = "corsACACHeadersTestAPI_2";
    private static final String API_VERSION = "1.0.0";
    private static final String TAGS = "ACAC, cors, test";
    private static final String DESCRIPTION = "This is test API create by API manager integration test";

    private static final String ACCESS_CONTROL_ALLOW_ORIGIN_HEADER = "Access-Control-Allow-Origin";
    private static final String ACCESS_CONTROL_ALLOW_ORIGIN_HEADER_VALUE_LOCALHOST = "http://localhost";
    private static final String ACCESS_CONTROL_ALLOW_CREDENTIALS_HEADER = "Access-Control-Allow-Credentials";
    private final String API_END_POINT_POSTFIX_URL = "jaxrs_basic/services/customers/customerservice/";

    private String accessToken;
    private String apiId;
    private String applicationId;

    Log log = LogFactory.getLog(CORSAccessControlAllowCredentialsHeaderTestCase.class);

    @BeforeClass(alwaysRun = true)
    public void initialize() throws Exception {
        super.init(userMode);
    }

    @Test(groups = {"wso2.am"}, description = "Checking Access-Control-Allow-Credentials header in response " +
            "when Access-Control-Allow-Origin is 'http://localhost'")
    public void CheckAccessControlAllowCredentialsHeadersWithSpecificOrigin() throws Exception {
        //Enable CORS Access Control Allow Credentials with Origin 'http://localhost'
        accessToken = createPublishAndSubscribeToApi(user, API_NAME_2, API_CONTEXT_2, API_VERSION, APPLICATION_NAME_2);
        waitForAPIDeploymentSync(user.getUserName(), API_NAME_2, API_VERSION, APIMIntegrationConstants.IS_API_EXISTS);
        HttpClient httpclient = HttpClientBuilder.create().build();
        HttpGet get = new HttpGet(getAPIInvocationURLHttps(API_CONTEXT_2, API_VERSION) + "/customers/123");
        get.addHeader("Origin", ACCESS_CONTROL_ALLOW_ORIGIN_HEADER_VALUE_LOCALHOST);
        get.addHeader("Authorization", "Bearer " + accessToken);

        org.apache.http.HttpResponse response = httpclient.execute(get);

        List<Integer> responseCodes = new ArrayList<Integer>();
        responseCodes.add(HTTP_RESPONSE_CODE_OK);
        responseCodes.add(HTTP_RESPONSE_CODE_CREATED);
        assertTrue(responseCodes.contains(response.getStatusLine().getStatusCode()), "Response code mismatch.");

        Header[] responseHeaders = response.getAllHeaders();

        log.info("Response Headers: CheckAccessControlAllowCredentialsHeadersWithAnyOrigin");
        for (Header header : responseHeaders) {
            log.info(header.getName() + " : " + header.getValue());
        }

        Header header = pickHeader(responseHeaders, ACCESS_CONTROL_ALLOW_ORIGIN_HEADER);
        assertNotNull(header, ACCESS_CONTROL_ALLOW_ORIGIN_HEADER + " header is not available in the response.");

        assertNotNull(pickHeader(responseHeaders, ACCESS_CONTROL_ALLOW_CREDENTIALS_HEADER),
                ACCESS_CONTROL_ALLOW_CREDENTIALS_HEADER + " header is not available in the response.");
    }

    private String createPublishAndSubscribeToApi(User user, String apiName, String apiContext, String apiVersion,
                                                  String appName)
            throws APIManagerIntegrationTestException, XPathExpressionException, MalformedURLException, ApiException, org.wso2.am.integration.clients.store.api.ApiException, JSONException {
        String providerName = user.getUserName();
        String apiEndPointUrl = backEndServerUrl.getWebAppURLHttps() + API_END_POINT_POSTFIX_URL;

        APIRequest apiRequest = new APIRequest(apiName, apiContext, new URL(apiEndPointUrl), true);
        apiRequest.setTags(TAGS);
        apiRequest.setDescription(DESCRIPTION);
        apiRequest.setTiersCollection(TIER_UNLIMITED);
        apiRequest.setProvider(providerName);
        //Add api resource
        APIOperationsDTO apiOperationsDTO1 = new APIOperationsDTO();
        apiOperationsDTO1.setVerb(APIMIntegrationConstants.HTTP_VERB_GET);
        apiOperationsDTO1.setTarget("/customers/{id}");
        apiOperationsDTO1.setAuthType(APIMIntegrationConstants.RESOURCE_AUTH_TYPE_APPLICATION_AND_APPLICATION_USER);
        apiOperationsDTO1.setThrottlingPolicy(APIMIntegrationConstants.RESOURCE_TIER.UNLIMITED);

        List<APIOperationsDTO> operationsDTOS = new ArrayList<>();
        operationsDTOS.add(apiOperationsDTO1);
        apiRequest.setOperationsDTOS(operationsDTOS);

        //Add the API using the API publisher.
        HttpResponse apiResponse = restAPIPublisher.addAPI(apiRequest);
        apiId = apiResponse.getData();

        // Create Revision and Deploy to Gateway
        createAPIRevisionAndDeployUsingRest(apiId, restAPIPublisher);
        waitForAPIDeployment();

        restAPIPublisher.changeAPILifeCycleStatus(apiId, APILifeCycleAction.PUBLISH.getAction(), null);

        //Create application
        org.wso2.carbon.automation.test.utils.http.client.HttpResponse applicationResponse =
                restAPIStore.createApplication(appName,
                        APIMIntegrationConstants.APPLICATION_TIER.DEFAULT_APP_POLICY_FIFTY_REQ_PER_MIN,
                        APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED,
                        ApplicationDTO.TokenTypeEnum.JWT);
        applicationId = applicationResponse.getData();

        //Subscribe to api
        restAPIStore.createSubscription(apiId, applicationId, APIMIntegrationConstants.API_TIER.UNLIMITED);

        //get access token
        ArrayList<String> grantTypes = new ArrayList<>();
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.PASSWORD);
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.CLIENT_CREDENTIAL);
        ApplicationKeyDTO applicationKeyDTO = restAPIStore.generateKeys(applicationId, "36000", "",
                ApplicationKeyGenerateRequestDTO.KeyTypeEnum.PRODUCTION, null, grantTypes);
        accessToken = applicationKeyDTO.getToken().getAccessToken();

        return accessToken;
    }

    @Test(groups = {"wso2.am"}, description = "Test generation of all supported SDKs", dependsOnMethods = {
            "CheckAccessControlAllowCredentialsHeadersWithSpecificOrigin"})
    public void testAllSupportedSDKGeneration() throws Exception {

        String languages[] = new String[]{"android", "java", "csharp", "dart", "groovy", "javascript", "jmeter", "perl",
                "php", "python", "ruby", "swift5", "clojure"};
        for (String language : languages) {
            ApiResponse<byte[]> sdkGenerationResponse = restAPIStore.generateSDKUpdated(apiId, language,
                    user.getUserDomain());
            assertEquals(sdkGenerationResponse.getStatusCode(), Response.Status.OK.getStatusCode(),
                    "Error when generating SDK for the " + language + " language");
        }
    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
        restAPIStore.deleteApplication(applicationId);
        undeployAndDeleteAPIRevisionsUsingRest(apiId, restAPIPublisher);
        restAPIPublisher.deleteAPI(apiId);
        super.cleanUp();
    }

    @DataProvider
    public static Object[][] userModeDataProvider() {
        return new Object[][]{new Object[]{TestUserMode.SUPER_TENANT_ADMIN},
                new Object[]{TestUserMode.TENANT_ADMIN},
        };
    }

    @Factory(dataProvider = "userModeDataProvider")
    public CORSAccessControlAllowCredentialsHeaderTestCase(TestUserMode userMode) {
        this.userMode = userMode;
    }
}
