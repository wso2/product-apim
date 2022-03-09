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
package org.wso2.am.integration.tests.version;

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
import org.wso2.am.integration.tests.api.lifecycle.APIManagerLifecycleBaseTest;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.test.utils.http.client.HttpRequestUtil;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.core.Response;

import static org.testng.Assert.assertEquals;

/**
 * This class is used to check the functionality of the Default Version API.
 * <p>
 * Note: By default an API will always have a version. Ex: 1.0.0. It is mandatory that the API request url contains this
 * version field. Ex: http://localhost:8280/twitter/1.0.0. By specifying a particular version of an API as the 'default'
 * versioned API, it makes it possible to invoke that particular api without having the version as part of the request
 * url. Ex: http://localhost:8280/twitter
 * </p>
 */
public class DefaultVersionAPITestCase extends APIManagerLifecycleBaseTest {
    private String applicationID;
    private String apiId;
    private String v2ApiId;
    private String newApiId;
    private String provider;
    private String apiName;
    private String apiVersion;
    private String apiContext;
    private String endpointUrl;
    Map<String, String> headers = new HashMap<>();

    @Factory(dataProvider = "userModeDataProvider")
    public DefaultVersionAPITestCase(TestUserMode userMode) {
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
        provider = publisherContext.getContextTenant().getContextUser().getUserName();
        apiName = "DefaultVersionAPI";
        apiVersion = "1.0.0";
        apiContext = "defaultversion";
        endpointUrl = getGatewayURLNhttp() + "response";
    }

    @Test(groups = "wso2.am", description = "Check functionality of the default version API")
    public void testDefaultVersionAPI() throws Exception {
        //Create the api creation request object
        APIRequest apiRequest = new APIRequest(apiName, apiContext, new URL(endpointUrl));
        apiRequest.setProvider(provider);
        apiRequest.setDefault_version("default_version");
        apiRequest.setDefault_version_checked("true");
        apiRequest.setVersion(apiVersion);
        apiRequest.setTiersCollection(APIMIntegrationConstants.API_TIER.UNLIMITED);
        apiRequest.setTier(APIMIntegrationConstants.API_TIER.UNLIMITED);

        //Add an Application in the Store.
        HttpResponse applicationResponse = restAPIStore
                .createApplication("DefaultVersionAPP", "Default version testing application",
                        APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED, ApplicationDTO.TokenTypeEnum.JWT);

        applicationID = applicationResponse.getData();

        //Create api and subscribe the API to the DefaultApplication
        apiId = createPublishAndSubscribeToAPIUsingRest(apiRequest, restAPIPublisher, restAPIStore, applicationID,
                APIMIntegrationConstants.API_TIER.UNLIMITED);

        //Generate production token and invoke with that
        ArrayList grantTypes = new ArrayList();
        grantTypes.add("client_credentials");
        ApplicationKeyDTO applicationKeyDTO = restAPIStore.generateKeys(applicationID, "3600", null,
                ApplicationKeyGenerateRequestDTO.KeyTypeEnum.PRODUCTION, null, grantTypes);
        String accessToken = applicationKeyDTO.getToken().getAccessToken();
        headers.put("Authorization", "Bearer " + accessToken);

        String apiInvocationUrl = getAPIInvocationURLHttp(apiContext);
        waitForAPIDeploymentSync(apiRequest.getProvider(), apiRequest.getName(), apiRequest.getVersion(),
                APIMIntegrationConstants.IS_API_EXISTS);

        //Going to access the API without the version in the request url.
        HttpResponse directResponse = HttpRequestUtil.doGet(endpointUrl, new HashMap<>());

        //Invoke the API
        waitForAPIDeploymentSync(apiRequest.getProvider(), apiRequest.getName(), apiRequest.getVersion(),
                APIMIntegrationConstants.IS_API_EXISTS);

        HttpResponse httpResponse = HttpRequestUtil.doGet(apiInvocationUrl, headers);

        //Check if accessing the back-end directly and accessing it via the API yield the same responses.
        assertEquals(httpResponse.getData(), directResponse.getData(),
                "Default version API test failed while invoking the API.");
    }

    @Test(groups = "wso2.am", dependsOnMethods = "testDefaultVersionAPI", description = "Check if the default" +
            " version API is available after creating a new version of the original API")
    public void testDefaultVersionAPIAfterNewVersion() throws Exception {
        String newVersion = "2.0.0";
        String defaultUrl = getAPIInvocationURLHttp(apiContext);

        // create new version
        HttpResponse v2Response = restAPIPublisher.copyAPI(newVersion, apiId, false);
        v2ApiId = v2Response.getData();
        assertEquals(v2Response.getResponseCode(), Response.Status.OK.getStatusCode(),
                "Response Code Mismatch. Didn't create new API version");

        // publish new version
        HttpResponse v2PublishResponse = restAPIPublisher.changeAPILifeCycleStatusToPublish(v2ApiId, false);
        assertEquals(v2PublishResponse.getResponseCode(), Response.Status.OK.getStatusCode(),
                "Response Code Mismatch, Didn't publish new API version");

        // invoke the API
        waitForAPIDeploymentSync(provider, apiName, newVersion, APIMIntegrationConstants.IS_API_EXISTS);
        HttpResponse httpResponse = HttpRequestUtil.doGet(defaultUrl, headers);
        assertEquals(httpResponse.getResponseCode(), Response.Status.OK.getStatusCode(),
                "Can't invoke default version after creating a new version");
    }

    @Test(groups = "wso2.am", dependsOnMethods = "testDefaultVersionAPIAfterNewVersion", description = "Check default" +
            " API invocation when context is similar to the version")
    public void testDefaultVersionAPIWithContextAndVersionSimilar() throws Exception {
        String newApiName = "DefaultNewVersionAPI";
        String newApiContext = "general/v1";
        String newVersion = "v1";

        APIRequest apiRequest = new APIRequest(newApiName, newApiContext, new URL(endpointUrl));
        apiRequest.setProvider(provider);
        apiRequest.setDefault_version("default_version");
        apiRequest.setDefault_version_checked("true");
        apiRequest.setVersion(newVersion);
        apiRequest.setTiersCollection(APIMIntegrationConstants.API_TIER.UNLIMITED);
        apiRequest.setTier(APIMIntegrationConstants.API_TIER.UNLIMITED);

        //Create api and subscribe the API to the DefaultApplication
        newApiId = createPublishAndSubscribeToAPIUsingRest(apiRequest, restAPIPublisher, restAPIStore, applicationID,
                APIMIntegrationConstants.API_TIER.UNLIMITED);
        waitForAPIDeploymentSync(provider, newApiName, newVersion, APIMIntegrationConstants.IS_API_EXISTS);

        String newApiInvocationUrl = getAPIInvocationURLHttp(newApiContext);
        HttpResponse httpResponse = HttpRequestUtil.doGet(newApiInvocationUrl, headers);
        assertEquals(httpResponse.getResponseCode(), Response.Status.OK.getStatusCode(),
                "Cannot invoke default version of APIs with similar context and versions");
    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
        SubscriptionListDTO subsDTO = restAPIStore.getAllSubscriptionsOfApplication(applicationID);
        for (SubscriptionDTO subscriptionDTO : subsDTO.getList()) {
            restAPIStore.removeSubscription(subscriptionDTO.getSubscriptionId());
        }
        restAPIStore.deleteApplication(applicationID);
        restAPIPublisher.deleteAPI(apiId);
        restAPIPublisher.deleteAPI(v2ApiId);
        restAPIPublisher.deleteAPI(newApiId);
        super.cleanUp();
    }

}
