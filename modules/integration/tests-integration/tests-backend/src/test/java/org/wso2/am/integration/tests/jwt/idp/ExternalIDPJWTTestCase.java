/*
 *Copyright (c) 2019, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.am.integration.tests.jwt.idp;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.json.JSONException;
import org.json.JSONObject;
import org.testng.Assert;
import org.testng.annotations.*;
import org.wso2.am.integration.clients.store.api.ApiResponse;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyGenerateRequestDTO;
import org.wso2.am.integration.test.utils.APIManagerIntegrationTestException;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.bean.APIRequest;
import org.wso2.am.integration.test.utils.generic.APIMTestCaseUtils;
import org.wso2.am.integration.tests.api.lifecycle.APIManagerLifecycleBaseTest;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.engine.context.beans.User;
import org.wso2.carbon.um.ws.api.stub.ClaimValue;
import org.wso2.carbon.um.ws.api.stub.RemoteUserStoreManagerServiceUserStoreExceptionException;
import org.wso2.carbon.user.core.UserStoreException;

import javax.ws.rs.core.Response;

import java.io.File;
import java.net.URL;
import java.nio.file.Paths;
import java.rmi.RemoteException;
import java.rmi.server.UID;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.testng.AssertJUnit.assertTrue;

public class ExternalIDPJWTTestCase extends APIManagerLifecycleBaseTest {

    private static final Log log = LogFactory.getLog(ExternalIDPJWTTestCase.class);

    private String apiName = "ExternalJWTTest";
    private String apiContext = "externaljwtTest";
    private String providerName;
    private String apiVersion = "1.0.0";
    private String jwtApplicationName = "JWTAppFOrJWTTest";

    private String endpointURL;
    private String jwtApplicationId;
    private String apiId;
    URL tokenEndpointURL;

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {

        super.init(userMode);
        tokenEndpointURL = new URL(gatewayUrlsWrk.getWebAppURLNhttp() + "token");
        providerName = user.getUserName();
        endpointURL = getSuperTenantAPIInvocationURLHttp("jwt_backend", "1.0");
        //create JWT Base App
        org.wso2.carbon.automation.test.utils.http.client.HttpResponse applicationDTO =
                restAPIStore.createApplication(jwtApplicationName, "JWT Application",
                        APIMIntegrationConstants.APPLICATION_TIER.TEN_PER_MIN, ApplicationDTO.TokenTypeEnum.JWT);
        jwtApplicationId = applicationDTO.getData();

        APIRequest apiRequest = new APIRequest(apiName, apiContext, new URL(endpointURL));
        apiRequest.setVersion(apiVersion);
        apiRequest.setVisibility("public");
        apiRequest.setProvider(providerName);
        apiId = createAndPublishAPIUsingRest(apiRequest, restAPIPublisher, false);
        restAPIStore.subscribeToAPI(apiId, jwtApplicationId, TIER_GOLD);
        ArrayList<String> grantTypes = new ArrayList<>();
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.CLIENT_CREDENTIAL);
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.PASSWORD);
        //generate keys
        restAPIStore.generateKeys(jwtApplicationId, "36000", "",
                ApplicationKeyGenerateRequestDTO.KeyTypeEnum.PRODUCTION, null, grantTypes);
    }

    @Test(groups = {"wso2.am"}, description = "invoking From ExternalIDP Generated JWT")
    public void testInvokeExternalIDPGeneratedJWT() throws Exception {

        ApiResponse<ApplicationKeyDTO> applicationKeysByKeyType =
                restAPIStore.getApplicationKeysByKeyType(jwtApplicationId,
                        ApplicationKeyGenerateRequestDTO.KeyTypeEnum.PRODUCTION.getValue());
        ApplicationKeyDTO applicationKeyDTO = applicationKeysByKeyType.getData();
        File keyStoreFile = Paths.get(getAMResourceLocation(), "configFiles", "idpjwt", "keystore.jks").toFile();
        Map attributes = new HashMap();
        attributes.put("azp", applicationKeyDTO.getConsumerKey());
        String generatedJWT =
                JWTGeneratorUtil.generatedJWT(keyStoreFile, "idptest", "wso2carbon", "wso2carbon", "userexternal",
                        attributes);
        HttpClient httpclient = HttpClientBuilder.create().build();
        HttpGet get = new HttpGet(getAPIInvocationURLHttp(apiContext, apiVersion));
        get.addHeader("Authorization", "Bearer " + generatedJWT);
        HttpResponse response = httpclient.execute(get);
        Assert.assertEquals(response.getStatusLine().getStatusCode(), Response.Status.OK.getStatusCode(),
                "Response code mismatched when api invocation");
    }
    @Test(groups = {"wso2.am"}, description = "invoking From ExternalIDP Generated JWT Consumer key is invalid")
    public void testInvokeExternalIDPGeneratedJWTNegative1() throws Exception {

        File keyStoreFile = Paths.get(getAMResourceLocation(), "configFiles", "idpjwt", "keystore.jks").toFile();
        Map attributes = new HashMap();
        attributes.put("azp", UUID.randomUUID().toString());
        String generatedJWT =
                JWTGeneratorUtil.generatedJWT(keyStoreFile, "idptest", "wso2carbon", "wso2carbon", "userexternal",
                        attributes);
        HttpClient httpclient = HttpClientBuilder.create().build();
        HttpGet get = new HttpGet(getAPIInvocationURLHttp(apiContext, apiVersion));
        get.addHeader("Authorization", "Bearer " + generatedJWT);
        HttpResponse response = httpclient.execute(get);
        Assert.assertEquals(response.getStatusLine().getStatusCode(), Response.Status.FORBIDDEN.getStatusCode(),
                "Response code mismatched when api invocation");
        String payload = IOUtils.toString(response.getEntity().getContent());
        Assert.assertTrue(payload.contains("900908"));
        Assert.assertTrue(payload.contains("User is NOT authorized to access the Resource. API Subscription validation failed."));
    }

    @Test(groups = {"wso2.am"}, description = "invoking From ExternalIDP Generated JWT Certificate is unknown")
    public void testInvokeExternalIDPGeneratedJWTNegative2() throws Exception {

        File keyStoreFile = Paths.get(getAMResourceLocation(), "configFiles", "idpjwt", "keystore2.jks").toFile();
        Map attributes = new HashMap();
        attributes.put("azp", UUID.randomUUID().toString());
        String generatedJWT =
                JWTGeneratorUtil.generatedJWT(keyStoreFile, "idptest", "wso2carbon", "wso2carbon", "userexternal",
                        attributes);
        HttpClient httpclient = HttpClientBuilder.create().build();
        HttpGet get = new HttpGet(getAPIInvocationURLHttp(apiContext, apiVersion));
        get.addHeader("Authorization", "Bearer " + generatedJWT);
        HttpResponse response = httpclient.execute(get);
        Assert.assertEquals(response.getStatusLine().getStatusCode(), Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(),
                "Response code mismatched when api invocation");
        String payload = IOUtils.toString(response.getEntity().getContent());
        Assert.assertTrue(payload.contains("900900"));
        Assert.assertTrue(payload.contains("Unclassified Authentication Failure"));
    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {

        super.cleanUp();
    }

    @DataProvider
    public static Object[][] userModeDataProvider() {

        return new Object[][]{new Object[]{TestUserMode.SUPER_TENANT_ADMIN},
                new Object[]{TestUserMode.TENANT_ADMIN}};
    }

    @Factory(dataProvider = "userModeDataProvider")
    public ExternalIDPJWTTestCase(TestUserMode userMode) {

        this.userMode = userMode;
    }

}
