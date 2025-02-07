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
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.am.integration.tests.admin;

import jdk.internal.joptsimple.internal.Strings;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.junit.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;
import org.wso2.am.integration.clients.admin.ApiResponse;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyGenerateRequestDTO;
import org.wso2.am.integration.test.impl.RestAPIAdminImpl;
import org.wso2.am.integration.test.impl.RestAPIStoreImpl;
import org.wso2.am.integration.test.utils.base.APIMIntegrationBaseTest;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.bean.APILifeCycleAction;
import org.wso2.am.integration.test.utils.bean.APIRequest;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.test.utils.http.client.HttpRequestUtil;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;
import org.wso2.carbon.integration.common.admin.client.UserManagementClient;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import static org.testng.Assert.assertEquals;
import static org.wso2.am.integration.test.utils.base.APIMIntegrationConstants.SUPER_TENANT_DOMAIN;

public class ChangeApiProviderTestCase extends APIMIntegrationBaseTest {

    private String publisherURLHttp;
    private RestAPIAdminImpl restAPIAdminClient;
    private String BEARER = "Bearer ";
    private String APIName = "NewApiForProviderChange";
    private String APIContext = "NewApiForProviderChange";
    private String tags = "youtube, token, media";
    private String apiEndPointUrl;
    private String description = "This is test API create by API manager integration test";
    private String APIVersion = "1.0.0";
    private String apiID;
    private String newUser = "peter123";
    private String firstUserName = "admin";
    private String newUserPass = "test123";
    private String[] subscriberRole = {APIMIntegrationConstants.APIM_INTERNAL_ROLE.CREATOR};
    private String APPLICATION_NAME = "testApplicationForProviderChange";
    private String applicationId;
    private String TIER_GOLD = "Gold";
    private String API_ENDPOINT_POSTFIX_URL = "jaxrs_basic/services/customers/customerservice/";
    private String API_ENDPOINT_METHOD = "customers/123";
    private int HTTP_RESPONSE_CODE_OK = Response.Status.OK.getStatusCode();
    private String RESPONSE_CODE_MISMATCH_ERROR_MESSAGE = "Response code mismatch";

    @Factory(dataProvider = "userModeDataProvider")
    public ChangeApiProviderTestCase(TestUserMode userMode) {

        this.userMode = userMode;
    }

    @DataProvider
    public static Object[][] userModeDataProvider() {

        return new Object[][]{new Object[]{TestUserMode.SUPER_TENANT_ADMIN},
                new Object[]{TestUserMode.TENANT_ADMIN},};
    }

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {

        super.init(userMode);
        publisherURLHttp = getPublisherURLHttp();
        userManagementClient = new UserManagementClient(keyManagerContext.getContextUrls().getBackEndUrl(),
                createSession(keyManagerContext));
        userManagementClient.addUser(newUser, newUserPass, subscriberRole, newUser);
        restAPIStore =
                new RestAPIStoreImpl(storeContext.getContextTenant().getContextUser().getUserNameWithoutDomain(),
                        storeContext.getContextTenant().getContextUser().getPassword(),
                        storeContext.getContextTenant().getDomain(), storeURLHttps);
        apiEndPointUrl = backEndServerUrl.getWebAppURLHttp() + API_ENDPOINT_POSTFIX_URL;
    }

    @Test(groups = {"wso2.am"}, description = "Calling API with invalid token")
    public void ChangeApiProvider() throws Exception {
        String providerName = user.getUserName();
        APIRequest apiRequest = new APIRequest(APIName, APIContext, new URL(apiEndPointUrl));
        apiRequest.setTags(tags);
        apiRequest.setProvider(providerName);
        apiRequest.setDescription(description);
        apiRequest.setVersion(APIVersion);
        apiRequest.setResourceMethod("GET");
        //add test api
        HttpResponse serviceResponse = restAPIPublisher.addAPI(apiRequest);
        assertEquals(serviceResponse.getResponseCode(), Response.Status.CREATED.getStatusCode(),
                "Response Code miss matched when creating the API");
        apiID = serviceResponse.getData();

        // Create Revision and Deploy to Gateway
        createAPIRevisionAndDeployUsingRest(apiID, restAPIPublisher);

        //publish the api
        restAPIPublisher.changeAPILifeCycleStatus(apiID, APILifeCycleAction.PUBLISH.getAction(), null);

        HttpResponse applicationResponse = restAPIStore.createApplication(APPLICATION_NAME, Strings.EMPTY,
                APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED,
                ApplicationDTO.TokenTypeEnum.JWT);

        applicationId = applicationResponse.getData();

        restAPIStore.subscribeToAPI(apiID, applicationId, TIER_GOLD);
        ArrayList<String> grantTypes = new ArrayList<>();
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.CLIENT_CREDENTIAL);
        ApplicationKeyDTO applicationKeyDTO = restAPIStore.generateKeys(applicationId,
                APIMIntegrationConstants.DEFAULT_TOKEN_VALIDITY_TIME,
                null,
                ApplicationKeyGenerateRequestDTO.KeyTypeEnum.PRODUCTION,
                null, grantTypes);
        Assert.assertNotNull(applicationKeyDTO.getToken());
        String accessToken = applicationKeyDTO.getToken().getAccessToken();

        HashMap<String, String> requestHeaders = new HashMap<>();
        requestHeaders.put(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON);
        requestHeaders.put(HttpHeaders.AUTHORIZATION, BEARER + accessToken);
        HttpResponse apiInvokeResponse = HttpRequestUtil.doGet(
                getAPIInvocationURLHttps(APIContext.replace(File.separator, Strings.EMPTY), APIVersion)
                        + File.separator + API_ENDPOINT_METHOD, requestHeaders);
        assertEquals(apiInvokeResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK, RESPONSE_CODE_MISMATCH_ERROR_MESSAGE);

        //Update provider of the api
        restAPIAdminClient = new RestAPIAdminImpl(firstUserName, firstUserName, "carbon.super",
                adminURLHttps);
        if(user.getUserName().equals(firstUserName)){
            ApiResponse<Void> changeProviderResponse = restAPIAdminClient.changeApiProvider(newUser, apiID);
            Assert.assertEquals(changeProviderResponse.getStatusCode(), HttpStatus.SC_OK);
        }
        apiInvokeResponse = HttpRequestUtil.doGet(
                getAPIInvocationURLHttps(APIContext.replace(File.separator, Strings.EMPTY), APIVersion)
                        + File.separator + API_ENDPOINT_METHOD, requestHeaders);
        assertEquals(apiInvokeResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK, RESPONSE_CODE_MISMATCH_ERROR_MESSAGE);
    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
        undeployAndDeleteAPIRevisionsUsingRest(apiID, restAPIPublisher);
        restAPIStore.deleteApplication(applicationId);
        restAPIPublisher.deleteAPI(apiID);
        userManagementClient.deleteUser(newUser);
    }
}
