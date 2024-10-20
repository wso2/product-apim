/*
 * Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.am.integration.tests.api.lifecycle;

import com.google.gson.internal.LinkedTreeMap;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.SearchResultListDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyGenerateRequestDTO;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.bean.APIRequest;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.test.utils.http.client.HttpRequestUtil;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;
import org.wso2.carbon.integration.common.admin.client.UserManagementClient;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static org.testng.Assert.assertEquals;

/**
 * When we have a templated context (i.e. a dynamic context), check whether the created API behaves as expected.
 * This test case covers the API creation to invocation flow and the ability to search for APIs using the
 * dynamic context.
 */
public class DynamicAPIContextTestCase extends APIManagerLifecycleBaseTest {

    private final Log log = LogFactory.getLog(DynamicAPIContextTestCase.class);

    private final String API_NAME = "ContextSearchAPI";
    private final String API_CONTEXT_TEMPLATE = "api/developer/{version}";
    private final String API_CONTEXT = "api/developer";
    private final String API_DESCRIPTION = "This is an API with a dynamic context";
    private final String API_VERSION = "1.0.0";
    private final String API_ENDPOINT_POSTFIX_URL = "jaxrs_basic/services/customers/customerservice/";
    private final String API_ENDPOINT_RESOURCE = "/customers/123";
    private final String APPLICATION_NAME = "DynamicContextTestApp";
    private String endpointUrl;
    private String apiId;
    private String applicationId;
    private String providerName;

    @Factory(dataProvider = "userModeDataProvider")
    public DynamicAPIContextTestCase(TestUserMode userMode) {
        this.userMode = userMode;
    }

    @DataProvider
    public static Object[][] userModeDataProvider() {

        return new Object[][]{
                new Object[]{TestUserMode.SUPER_TENANT_ADMIN},
        };
    }

    @BeforeClass(alwaysRun = true)
    public void initialize() throws Exception {
        super.init(userMode);

        userManagementClient = new UserManagementClient(
                keyManagerContext.getContextUrls().getBackEndUrl(),
                keyManagerContext.getContextTenant().getTenantAdmin().getUserName(),
                keyManagerContext.getContextTenant().getTenantAdmin().getPassword());

        endpointUrl = backEndServerUrl.getWebAppURLHttp() + API_ENDPOINT_POSTFIX_URL;
        providerName = user.getUserName();

        // Create application
        HttpResponse applicationResponse = restAPIStore.createApplication(APPLICATION_NAME, "Test Application",
                APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED, ApplicationDTO.TokenTypeEnum.JWT);
        applicationId = applicationResponse.getData();
    }

    @Test(groups = {"wso2.am"}, description = "Test API behavior when the API context is a templated context")
    public void testAPIWithTemplatedContext() throws Exception {

        // Create, publish and subscribe to an API with a templated context
        APIRequest apiRequest = new APIRequest(API_NAME, API_CONTEXT_TEMPLATE, new URL(endpointUrl));

        apiRequest.setVersion(API_VERSION);
        apiRequest.setDescription(API_DESCRIPTION);
        apiRequest.setTiersCollection(APIMIntegrationConstants.API_TIER.UNLIMITED);
        apiRequest.setTier(APIMIntegrationConstants.API_TIER.UNLIMITED);
        apiRequest.setProvider(providerName);
        apiRequest.setVisibility(APIDTO.VisibilityEnum.PUBLIC.getValue());

        apiId = createPublishAndSubscribeToAPIUsingRest(apiRequest, restAPIPublisher, restAPIStore, applicationId,
                APIMIntegrationConstants.API_TIER.UNLIMITED);

        // Generate access token
        ArrayList<String> grantTypes = new ArrayList<>();
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.PASSWORD);
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.CLIENT_CREDENTIAL);
        ApplicationKeyDTO applicationKeyDTO = restAPIStore.generateKeys(applicationId, "3600", "",
                ApplicationKeyGenerateRequestDTO.KeyTypeEnum.PRODUCTION, null, grantTypes);

        String accessToken = applicationKeyDTO.getToken().getAccessToken();
        Map<String, String> requestHeaders = new HashMap<>();
        requestHeaders.put(APIMIntegrationConstants.AUTHORIZATION_HEADER, "Bearer " + accessToken);
        requestHeaders.put("accept", "text/xml");

        // Try invoking the API
        String apiInvocationUrl = getAPIInvocationURLHttps(API_CONTEXT, API_VERSION) + API_ENDPOINT_RESOURCE;
        HttpResponse response = HttpRequestUtil.doGet(apiInvocationUrl, requestHeaders);
        assertEquals(response.getResponseCode(), HTTP_RESPONSE_CODE_OK, "Invocation fails for GET Method");

    }

    @Test(groups = {"wso2.am"}, description = "Test API search using the templated context",
            dependsOnMethods = "testAPIWithTemplatedContext")
    public void testAPISearchUsingTemplatedContext() throws Exception {

        // Generating the Solr Query to search for the API by context template
        String query = "context:/" + API_CONTEXT_TEMPLATE;

        // Since indexing needs time, we are retrying at an interval of 3s
        int retries = 20;

        // Search for API in Publisher
        for (int i = 0; i <= retries; i++) {
            SearchResultListDTO searchResultListDTO = restAPIPublisher.searchAPIs(query);
            // Check if the returned list has the API we originally created
            if (searchResultListDTO.getCount() == 1) {
                Object apiObj = searchResultListDTO.getList() != null ? searchResultListDTO.getList().get(0) : null;
                String searchResultAPIName = ((LinkedTreeMap) apiObj).get("name").toString();
                assertEquals(searchResultAPIName, API_NAME,
                        "Returned API list after executing the search query is not as expected");
                break;
            } else {
                if (i == retries) {
                    Assert.fail("API search using context template in publisher failed. Received API count: " +
                            searchResultListDTO.getCount());
                } else {
                    log.warn("API search using context template in publisher failed. Received API count: "
                            + searchResultListDTO.getCount() + ". Retrying...");
                    Thread.sleep(3000);
                }
            }
        }

    }

    @AfterClass(alwaysRun = true)
    public void cleanUpArtifacts() throws Exception {
        restAPIStore.deleteApplication(applicationId);
        undeployAndDeleteAPIRevisionsUsingRest(apiId, restAPIPublisher);
        restAPIPublisher.deleteAPI(apiId);
    }

}
