/*
 * Copyright (c) 2024, WSO2 LLC. (http://www.wso2.org) All Rights Reserved.
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
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.am.integration.tests.header;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpOptions;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.HttpClientBuilder;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIOperationsDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyGenerateRequestDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.SubscriptionDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.SubscriptionListDTO;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.bean.APIRequest;
import org.wso2.am.integration.tests.api.lifecycle.APIManagerLifecycleBaseTest;
import org.wso2.carbon.automation.engine.FrameworkConstants;
import org.wso2.carbon.automation.engine.annotations.ExecutionEnvironment;
import org.wso2.carbon.automation.engine.annotations.SetEnvironment;
import org.wso2.carbon.automation.engine.context.TestUserMode;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;

/**
 * Test CORS backend routing functionality
 */
@SetEnvironment(executionEnvironments = {ExecutionEnvironment.ALL })
public class CORSBackendTrafficRouteTestCase extends APIManagerLifecycleBaseTest {

    private static final String API_VERSION = "1.0.0";
    private static final String DESCRIPTION = "This is test API create by API manager integration test";

    private static final String API_NAME = "CorsBackendTestAPI";
    private static final String APPLICATION_NAME2 = "CorsBackendTestApp";
    private static final String API_CONTEXT2 = "corsBackendTestAPI";
    private static final String TAGS2 = "cors, backend, test";

    private static final String ACCESS_CONTROL_ALLOW_ORIGIN_HEADER = "Access-Control-Allow-Origin";
    private static final String ACCESS_CONTROL_ALLOW_ORIGIN_HEADER_VALUE_BACKEND = "http://localhost";
    private static final String ACCESS_CONTROL_ALLOW_METHODS_HEADER = "Access-Control-Allow-Methods";
    private static final String ACCESS_CONTROL_ALLOW_METHODS_HEADER_VALUE_BACKEND = "GET, POST, OPTIONS";
    private static final String ACCESS_CONTROL_ALLOW_HEADERS_HEADER = "Access-Control-Allow-Headers";
    private static final String ACCESS_CONTROL_ALLOW_HEADERS_HEADER_VALUE_BACKEND = "authorization,Access-Control-Allow-Origin";
    private static final String ACCESS_CONTROL_ALLOW_CREDENTIALS_HEADER = "Access-Control-Allow-Credentials";

    private String applicationId;
    private String apiId;
    private ApplicationKeyDTO applicationKeyDTO;

    Log log = LogFactory.getLog(CORSBackendTrafficRouteTestCase.class);

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {
        super.init(userMode);

        //Create application
        org.wso2.carbon.automation.test.utils.http.client.HttpResponse applicationResponse =
                restAPIStore.createApplication(APPLICATION_NAME2,
                        "This is a test application for CORS backend routing test",
                        APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED,
                        ApplicationDTO.TokenTypeEnum.JWT);
        applicationId = applicationResponse.getData();

        //get access token
        ArrayList<String> grantTypes = new ArrayList<>();
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.PASSWORD);
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.CLIENT_CREDENTIAL);
        applicationKeyDTO = restAPIStore.generateKeys(applicationId, "36000", "",
                ApplicationKeyGenerateRequestDTO.KeyTypeEnum.PRODUCTION, null, grantTypes);
    }

    @Test(groups = {"wso2.am"}, description = "Check CORS headers in OPTIONS response from Backend for a OPTIONS resource defined API")
    public void CheckCORSHeadersInOptionsResponseFromBackend() throws Exception {
        APIRequest apiRequest = new APIRequest(API_NAME, API_CONTEXT2,
                new URL(gatewayUrlsWrk.getWebAppURLNhttp() + "backend-cors"));
        apiRequest.setTags(TAGS2);
        apiRequest.setVersion(API_VERSION);
        apiRequest.setVisibility("public");
        apiRequest.setDescription(DESCRIPTION);
        apiRequest.setTiersCollection(TIER_UNLIMITED);
        apiRequest.setProvider(publisherContext.getContextTenant().getContextUser().getUserName());

        //Add api resource
        APIOperationsDTO apiOperationsDTO1 = new APIOperationsDTO();
        apiOperationsDTO1.setVerb(APIMIntegrationConstants.HTTP_VERB_OPTIONS);
        apiOperationsDTO1.setTarget("/cors");
        apiOperationsDTO1.setAuthType(APIMIntegrationConstants.RESOURCE_AUTH_TYPE_APPLICATION_AND_APPLICATION_USER);
        apiOperationsDTO1.setThrottlingPolicy(APIMIntegrationConstants.RESOURCE_TIER.UNLIMITED);

        List<APIOperationsDTO> operationsDTOS = new ArrayList<>();
        operationsDTOS.add(apiOperationsDTO1);
        apiRequest.setOperationsDTOS(operationsDTOS);

        apiId = createPublishAndSubscribeToAPIUsingRest(apiRequest, restAPIPublisher, restAPIStore, applicationId,
                APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED);

        HttpClient httpclient = HttpClientBuilder.create().build();
        String uri;
        if (gatewayContextWrk.getContextTenant().getDomain().equals(FrameworkConstants.SUPER_TENANT_DOMAIN_NAME)) {
            uri = gatewayUrlsWrk.getWebAppURLNhttp() + API_CONTEXT2 + "/" + API_VERSION + "/cors";
        } else {
            uri = gatewayUrlsWrk.getWebAppURLNhttp() + "t/" + gatewayContextWrk.getContextTenant()
                    .getDomain() + "/" + API_CONTEXT2 + "/" + API_VERSION + "/cors";
        }

        HttpUriRequest option = new HttpOptions(uri);
        option.addHeader("Authorization", "Bearer " + applicationKeyDTO.getToken().getAccessToken());
        HttpResponse response = httpclient.execute(option);

        assertEquals(response.getStatusLine().getStatusCode(), HTTP_RESPONSE_CODE_OK, "Response code mismatch.");

        Header[] responseHeaders = response.getAllHeaders();

        log.info("Response Headers: CheckCORSHeadersInOptionsResponseFromBackend");
        for (Header header : responseHeaders) {
            log.info(header.getName() + " : " + header.getValue());
        }

        Header header = pickHeader(responseHeaders, ACCESS_CONTROL_ALLOW_ORIGIN_HEADER);
        assertNotNull(header, ACCESS_CONTROL_ALLOW_ORIGIN_HEADER + " header is not available in the response.");
        assertEquals(header.getValue(), ACCESS_CONTROL_ALLOW_ORIGIN_HEADER_VALUE_BACKEND,
                ACCESS_CONTROL_ALLOW_ORIGIN_HEADER + " header value mismatch.");

        header = pickHeader(responseHeaders, ACCESS_CONTROL_ALLOW_METHODS_HEADER);
        assertNotNull(header, ACCESS_CONTROL_ALLOW_METHODS_HEADER + " header is not available in the response.");
        assertEquals(header.getValue(), ACCESS_CONTROL_ALLOW_METHODS_HEADER_VALUE_BACKEND, ACCESS_CONTROL_ALLOW_METHODS_HEADER + " header value mismatch.");

        header = pickHeader(responseHeaders, ACCESS_CONTROL_ALLOW_HEADERS_HEADER);
        assertNotNull(header, ACCESS_CONTROL_ALLOW_HEADERS_HEADER + " header is not available in the response.");
        assertEquals(header.getValue(), ACCESS_CONTROL_ALLOW_HEADERS_HEADER_VALUE_BACKEND,
                ACCESS_CONTROL_ALLOW_HEADERS_HEADER + " header value mismatch.");

        assertNull(pickHeader(responseHeaders, ACCESS_CONTROL_ALLOW_CREDENTIALS_HEADER),
                ACCESS_CONTROL_ALLOW_CREDENTIALS_HEADER + " header is available in the response, " +
                        "but it should not be.");
    }


    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
        SubscriptionListDTO subsDTO = restAPIStore.getAllSubscriptionsOfApplication(applicationId);
        for (SubscriptionDTO subscriptionDTO: subsDTO.getList()){
            restAPIStore.removeSubscription(subscriptionDTO);
        }
        restAPIStore.deleteApplication(applicationId);
        undeployAndDeleteAPIRevisionsUsingRest(apiId, restAPIPublisher);
        restAPIPublisher.deleteAPI(apiId);

        super.cleanUp();
    }

    @DataProvider
    public static Object[][] userModeDataProvider() {
        return new Object[][] { new Object[] { TestUserMode.SUPER_TENANT_ADMIN },
                new Object[] { TestUserMode.TENANT_ADMIN },
        };
    }

    @Factory(dataProvider = "userModeDataProvider")
    public CORSBackendTrafficRouteTestCase(TestUserMode userMode) {
        this.userMode = userMode;
    }
}
