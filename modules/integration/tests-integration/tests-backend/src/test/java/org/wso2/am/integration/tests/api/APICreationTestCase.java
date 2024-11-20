/*
 * Copyright (c) 2022, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.am.integration.tests.api;

import org.codehaus.plexus.util.StringUtils;
import org.json.JSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.bean.APIRequest;
import org.wso2.am.integration.tests.api.lifecycle.APIManagerLifecycleBaseTest;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import java.io.File;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

import static org.testng.Assert.assertTrue;

public class APICreationTestCase extends APIManagerLifecycleBaseTest {
    private final String API_END_POINT_POSTFIX_URL = "jaxrs_basic/services/customers/customerservice/";
    private String apiEndpointUrl;
    private String apiId;
    private String apiIdAPK;
    private String apiIdSynapse;

    @Factory(dataProvider = "userModeDataProvider")
    public APICreationTestCase(TestUserMode userMode) {

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
    public void initialize() throws Exception {

        super.init(userMode);
        apiEndpointUrl = backEndServerUrl.getWebAppURLHttp() + API_END_POINT_POSTFIX_URL;
    }

    @Test(groups = {"wso2.am"}, description = "Test deployment of API with Mutual SSL enabled")
    public void testCreateAndDeployApiWithMutualSSLEnabled() throws Exception {
        // Create API and enable Mutual SSL with client certificate
        APIRequest apiRequest;
        apiRequest = new APIRequest("MutuallSSLEnabledAPI", "mutualsslapi", new URL(apiEndpointUrl));
        apiRequest.setVersion(API_VERSION_1_0_0);
        apiRequest.setTier(APIMIntegrationConstants.API_TIER.UNLIMITED);
        apiRequest.setProvider(user.getUserName());
        List<String> securityScheme = Arrays.asList("mutualssl", "mutualssl_mandatory");
        apiRequest.setSecurityScheme(securityScheme);
        HttpResponse apiResponse = restAPIPublisher.addAPI(apiRequest);
        apiId = apiResponse.getData();
        String certificate = getAMResourceLocation() + File.separator + "lifecycletest" + File.separator + "mutualssl"
                + File.separator + "example.crt";
        restAPIPublisher.uploadCertificate(new File(certificate), "example", apiId,
                APIMIntegrationConstants.API_TIER.UNLIMITED, APIMIntegrationConstants.KEY_TYPE.SANDBOX);

        // Verify deployment of API with Mutual SSL enabled
        String revisionUUID = createAPIRevisionAndDeployUsingRest(apiId, restAPIPublisher);
        Assert.assertNotNull(revisionUUID);
    }

    @Test(groups = {"wso2.am"}, description = "Test deployment of API with Mutual SSL enabled")
    public void testCreateAndDeployApiWithGatewayType() throws Exception {
        // Create API with gateway type APK
        APIRequest apiRequest;
        apiRequest = new APIRequest("APKGatewayAPI1", "apkgateway", new URL(apiEndpointUrl));
        apiRequest.setVersion(API_VERSION_1_0_0);
        apiRequest.setTier(APIMIntegrationConstants.API_TIER.UNLIMITED);
        apiRequest.setProvider(user.getUserName());
        apiRequest.setGatewayType("wso2/apk");
        HttpResponse apiResponse = restAPIPublisher.addAPI(apiRequest);
        apiIdAPK = apiResponse.getData();
        String revisionUUID = createAPIRevisionAndDeployUsingRest(apiIdAPK, restAPIPublisher);
        Assert.assertNotNull(revisionUUID);

        // Verify the API in API Publisher
        HttpResponse apiDto = restAPIPublisher.getAPI(apiIdAPK);
        assertTrue(StringUtils.isNotEmpty(apiDto.getData()),
                "Added Api is not available in APi Publisher. API ID " + apiId);
        JSONObject apiResponseData = new JSONObject(apiDto.getData());
        String addedGatewayType = apiResponseData.getString("gatewayType");
        Assert.assertEquals(addedGatewayType, "wso2/apk", "Gateway type is not set as expected");

        // Create API with gateway type Synapse
        apiRequest = new APIRequest("SynapseGatewayAPI1", "synapsegateway", new URL(apiEndpointUrl));
        apiRequest.setVersion(API_VERSION_1_0_0);
        apiRequest.setTier(APIMIntegrationConstants.API_TIER.UNLIMITED);
        apiRequest.setProvider(user.getUserName());
        apiRequest.setGatewayType("wso2/synapse");
        apiResponse = restAPIPublisher.addAPI(apiRequest);
        apiIdSynapse = apiResponse.getData();
        revisionUUID = createAPIRevisionAndDeployUsingRest(apiIdSynapse, restAPIPublisher);
        Assert.assertNotNull(revisionUUID);

        // Verify the API in API Publisher
        apiDto = restAPIPublisher.getAPI(apiIdSynapse);
        assertTrue(StringUtils.isNotEmpty(apiDto.getData()),
                "Added Api is not available in APi Publisher. API ID " + apiId);
        apiResponseData = new JSONObject(apiDto.getData());
        addedGatewayType = apiResponseData.getString("gatewayType");
        Assert.assertEquals(addedGatewayType, "wso2/synapse", "Gateway type is not set as expected");
    }

    @AfterClass(alwaysRun = true)
    public void cleanUpArtifacts() throws Exception {

        restAPIPublisher.deleteAPI(apiId);
    }
}
