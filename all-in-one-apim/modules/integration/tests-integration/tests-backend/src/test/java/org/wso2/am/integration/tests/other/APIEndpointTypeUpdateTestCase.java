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
*
*/
package org.wso2.am.integration.tests.other;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.simple.parser.JSONParser;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyGenerateRequestDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.SubscriptionDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.SubscriptionListDTO;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.bean.APIRequest;
import org.wso2.am.integration.test.utils.base.APIManagerLifecycleBaseTest;
import org.wso2.carbon.automation.engine.annotations.ExecutionEnvironment;
import org.wso2.carbon.automation.engine.annotations.SetEnvironment;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.test.utils.http.client.HttpRequestUtil;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import javax.ws.rs.core.Response;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static org.testng.Assert.*;

@SetEnvironment(executionEnvironments = {ExecutionEnvironment.STANDALONE})
public class APIEndpointTypeUpdateTestCase extends APIManagerLifecycleBaseTest {
    private final Log log = LogFactory.getLog(APIEndpointTypeUpdateTestCase.class);

    private String APIVersion = "1.0.0";
    private String apiContext = "APIEndpointTypeUpdateTestCaseAPIContext";
    private String apiId;
    private String applicationID;
    private APIRequest apiRequest;
    private Map<String, String> requestHeaders = new HashMap<>();
    private URL endpointUrl;

    @Factory(dataProvider = "userModeDataProvider")
    public APIEndpointTypeUpdateTestCase(TestUserMode userMode) {
        this.userMode = userMode;
    }

    @DataProvider
    public static Object[][] userModeDataProvider() {
        return new Object[][]{
                new Object[]{TestUserMode.SUPER_TENANT_ADMIN},
                new Object[]{TestUserMode.TENANT_ADMIN},
        };
    }

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {
        super.init(userMode);
    }

    @Test(groups = {"wso2.am"}, description = "Sample API creation and subscribe")
    public void testAPICreation() {
        try {
            endpointUrl = new URL(backEndServerUrl.getWebAppURLHttp() + "am/sample/calculator/v1/api/add");
        } catch (MalformedURLException e) {
            log.error("Invalid URL " + endpointUrl, e);
            fail(e.getMessage());
        }

        try {
            String apiName = "APIEndpointTypeUpdateTestCaseAPIName";
            String description = "This is test API create by API manager integration test";
            String tags = "test, EndpointType";
            apiRequest = new APIRequest(apiName, apiContext);
            String endPointString = "{\n" + "  \"endpoint_type\": \"sequence_backend\"\n" + "}";

            JSONParser parser = new JSONParser();
            apiRequest.setEndpoint((org.json.simple.JSONObject) parser.parse(endPointString));
            apiRequest.setTags(tags);
            apiRequest.setDescription(description);
            apiRequest.setVersion(APIVersion);
            apiRequest.setProvider(publisherContext.getContextTenant().getContextUser().getUserName());

            String appName = "APIEndpointTypeUpdateTestCaseAPIApp";
            HttpResponse applicationResponse = restAPIStore.createApplication(appName, "This-is-test",
                    APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED, ApplicationDTO.TokenTypeEnum.JWT);
            applicationID = applicationResponse.getData();

            apiId = createPublishAndSubscribeToAPIUsingRest(apiRequest, restAPIPublisher, restAPIStore, applicationID,
                    APIMIntegrationConstants.API_TIER.GOLD);

            ArrayList grantTypes = new ArrayList();
            grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.CLIENT_CREDENTIAL);
            ApplicationKeyDTO applicationKeyDTO = restAPIStore.generateKeys(applicationID, "3600", null,
                    ApplicationKeyGenerateRequestDTO.KeyTypeEnum.PRODUCTION, null, grantTypes);
            String accessToken = applicationKeyDTO.getToken().getAccessToken();

            requestHeaders.put(APIMIntegrationConstants.AUTHORIZATION_HEADER, "Bearer " + accessToken);
        } catch (Exception e) {
            log.error("Error while executing test case " + e.getMessage(), e);
            fail(e.getMessage());
        }
    }

    @Test(groups = {
            "wso2.am" }, description = "Update to only Sequence Backend endpoint and invoke", dependsOnMethods = "testAPICreation")
    public void testUpdatedSequenceBackendEndpoint() {
        try {
            File seqProd = new File(getAMResourceLocation() + File.separator + "sequenceBackend" + File.separator
                    + "sequence_prod.xml");
            File seqSand = new File(getAMResourceLocation() + File.separator + "sequenceBackend" + File.separator
                    + "sequence_sand.xml");
            restAPIPublisher.addSequenceBackend(seqProd, apiId, "PRODUCTION");
            restAPIPublisher.addSequenceBackend(seqSand, apiId, "SANDBOX");
            final String responseDataExpected = "{\"Response\" : \"Sample Response\"}";

            // Create Revision and Deploy to Gateway
            createAPIRevisionAndDeployUsingRest(apiId, restAPIPublisher);
            waitForAPIDeploymentSync(apiRequest.getProvider(), apiRequest.getName(), apiRequest.getVersion(),
                    APIMIntegrationConstants.IS_API_EXISTS);
            Thread.sleep(10000);

            HttpResponse response = HttpRequestUtil.doGet(getAPIInvocationURLHttps(apiContext, APIVersion),
                    requestHeaders);
            assertEquals(response.getResponseCode(), Response.Status.OK.getStatusCode(),
                    "Response code mismatched when api https invocation");
            assertNotNull(response.getData());
            assertEquals(response.getData(), responseDataExpected);
        } catch (Exception e) {
            log.error("Error while executing test case " + e.getMessage(), e);
            fail(e.getMessage());
        }
    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
        SubscriptionListDTO subsDTO = restAPIStore.getAllSubscriptionsOfApplication(applicationID);
        for (SubscriptionDTO subscriptionDTO : subsDTO.getList()) {
            restAPIStore.removeSubscription(subscriptionDTO);
        }
        restAPIStore.deleteApplication(applicationID);
        restAPIPublisher.deleteAPI(apiId);
        super.cleanUp();
    }
}
