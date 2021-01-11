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
    }

    @Test(groups = "wso2.am", description = "Check functionality of the default version API")
    public void testDefaultVersionAPI() throws Exception {

        String apiName = "DefaultVersionAPI";
        String apiVersion = "1.0.0";
        String apiContext = "defaultversion";
        String endpointUrl = getGatewayURLNhttp() + "response";

        //Create the api creation request object
        APIRequest apiRequest = new APIRequest(apiName, apiContext, new URL(endpointUrl));
        apiRequest.setProvider(publisherContext.getContextTenant().getContextUser().getUserName());
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

        String apiInvocationUrl = getAPIInvocationURLHttp(apiContext);
        waitForAPIDeploymentSync(apiRequest.getProvider(), apiRequest.getName(), apiRequest.getVersion(),
                APIMIntegrationConstants.IS_API_EXISTS);

        //Going to access the API without the version in the request url.
        HttpResponse directResponse = HttpRequestUtil.doGet(endpointUrl, new HashMap<>());

        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Bearer " + accessToken);

        //Invoke the API
        waitForAPIDeploymentSync(apiRequest.getProvider(), apiRequest.getName(), apiRequest.getVersion(),
                APIMIntegrationConstants.IS_API_EXISTS);

        HttpResponse httpResponse = HttpRequestUtil.doGet(apiInvocationUrl, headers);

        //Check if accessing the back-end directly and accessing it via the API yield the same responses.
        assertEquals(httpResponse.getData(), directResponse.getData(),
                "Default version API test failed while " + "invoking the API.");
    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
        SubscriptionListDTO subsDTO = restAPIStore.getAllSubscriptionsOfApplication(applicationID);
        for (SubscriptionDTO subscriptionDTO : subsDTO.getList()) {
            restAPIStore.removeSubscription(subscriptionDTO.getSubscriptionId());
        }
        restAPIStore.deleteApplication(applicationID);
        undeployAndDeleteAPIRevisionsUsingRest(apiId, restAPIPublisher);
        restAPIPublisher.deleteAPI(apiId);
        super.cleanUp();
    }

}
