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

import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.json.simple.JSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIRevisionDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.*;
import org.wso2.am.integration.test.Constants;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.bean.APIRequest;
import org.wso2.am.integration.test.utils.bean.APIRevisionDeployUndeployRequest;
import org.wso2.am.integration.tests.api.lifecycle.APIManagerLifecycleBaseTest;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
    private String newApiId;
    String accessToken;
    String newAPIVersion;

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
        //Add an Application in the Store.
        HttpResponse applicationResponse = restAPIStore
                .createApplication("DefaultVersionAPP", "Default version testing application",
                        APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED, ApplicationDTO.TokenTypeEnum.JWT);

        applicationID = applicationResponse.getData();
        //Generate production token and invoke with that
        ArrayList grantTypes = new ArrayList();
        grantTypes.add("client_credentials");
        ApplicationKeyDTO applicationKeyDTO = restAPIStore.generateKeys(applicationID, "3600", null,
                ApplicationKeyGenerateRequestDTO.KeyTypeEnum.PRODUCTION, null, grantTypes);
        accessToken = applicationKeyDTO.getToken().getAccessToken();

    }

    @Test(groups = "wso2.am", description = "Create New API without Selecting Default Version")
    public void testDefaultVersionAPI() throws Exception {

        String apiName = "DefaultVersionAPI";
        String apiVersion = "1.0.0";
        String apiContext = "defaultversion";
        String endpointUrl = getGatewayURLNhttp() + "version1";

        //Create the api creation request object
        APIRequest apiRequest = new APIRequest(apiName, apiContext, new URL(endpointUrl));
        apiRequest.setProvider(publisherContext.getContextTenant().getContextUser().getUserName());
        apiRequest.setVersion(apiVersion);
        apiRequest.setTiersCollection(APIMIntegrationConstants.API_TIER.UNLIMITED);
        apiRequest.setTier(APIMIntegrationConstants.API_TIER.UNLIMITED);


        //Create api and subscribe the API to the DefaultApplication
        apiId = createPublishAndSubscribeToAPIUsingRest(apiRequest, restAPIPublisher, restAPIStore, applicationID,
                APIMIntegrationConstants.API_TIER.UNLIMITED);
        waitForAPIDeploymentSync(apiRequest.getProvider(), apiRequest.getName(), apiRequest.getVersion(),
                APIMIntegrationConstants.IS_API_EXISTS);
        APIDTO storeAPI = restAPIStore.getAPI(apiId);
        List<APIEndpointURLsDTO> endpointURLs = storeAPI.getEndpointURLs();
        Assert.assertNotNull(endpointURLs);
        Assert.assertEquals(endpointURLs.size(), 1);
        APIDefaultVersionURLsDTO defaultVersionURLs = endpointURLs.get(0).getDefaultVersionURLs();
        Assert.assertNotNull(defaultVersionURLs);
        Assert.assertNull(defaultVersionURLs.getHttp());
        Assert.assertNull(defaultVersionURLs.getHttps());
        Assert.assertNull(defaultVersionURLs.getWs());
        Assert.assertNull(defaultVersionURLs.getWss());
        String versionAPIInvocationUrl = getAPIInvocationURLHttp(apiContext, apiVersion);
        //Going to access the API without the version in the request url.
        HttpResponse directResponse = invokeWithGet(endpointUrl, new HashMap<>());

        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Bearer " + accessToken);

        HttpResponse httpResponse = invokeWithGet(versionAPIInvocationUrl, headers);

        //Check if accessing the back-end directly and accessing it via the API yield the same responses.
        assertEquals(httpResponse.getData(), directResponse.getData(),
                "Default version API test failed while " + "invoking the API.");
        // Invoke Default API and check if theres any default API deployed.
        String defaultVersionAPIInvocationUrl = getAPIInvocationURLHttp(apiContext);
        HttpResponse defaultHttpResponse = invokeWithGet(defaultVersionAPIInvocationUrl, headers);
        // No default API deployed. hence 404
        Assert.assertEquals(defaultHttpResponse.getResponseCode(), 404);
        // Updating API with DefaultVersion Check
        org.wso2.am.integration.clients.publisher.api.v1.dto.APIDTO apiv1 = restAPIPublisher.getAPIByID(apiId);
        apiv1.setIsDefaultVersion(true);
        org.wso2.am.integration.clients.publisher.api.v1.dto.APIDTO apidto = restAPIPublisher.updateAPI(apiv1);
        Assert.assertNotNull(apidto);
        Assert.assertTrue(apidto.isIsDefaultVersion());
        APIDTO storeAPIAfterUpdate = restAPIStore.getAPI(apiId);
        endpointURLs = storeAPIAfterUpdate.getEndpointURLs();
        Assert.assertNotNull(endpointURLs);
        Assert.assertEquals(endpointURLs.size(), 1);
        defaultVersionURLs = endpointURLs.get(0).getDefaultVersionURLs();
        Assert.assertNotNull(defaultVersionURLs);
        Assert.assertNotNull(defaultVersionURLs.getHttp());
        Assert.assertNotNull(defaultVersionURLs.getHttps());
        waitForAPIDeploymentSync(apiRequest.getProvider(), apiRequest.getName(), apiRequest.getVersion(),
                APIMIntegrationConstants.IS_API_EXISTS);
        HttpResponse defaultHttpResponse1 = invokeDefaultAPIWithWait(defaultVersionAPIInvocationUrl, headers, 200);

        assertEquals(defaultHttpResponse1.getData(), directResponse.getData(),
                "Default version API test failed while " + "invoking the API.");
        Assert.assertEquals(defaultHttpResponse1.getHeaders().get("Version"), "v1");
    }

    @Test(groups = "wso2.am", description = "Create new version of API with default Version enable in created stage " +
            "and verify", dependsOnMethods = {"testDefaultVersionAPI"})
    public void createNewVersionWithDefaultVersionOptionAndVerifyDefaultAPIBreaking() throws Exception {

        String apiVersion = "2.0.0";
        String apiContext = "defaultversion";
        String endpointUrl = getGatewayURLNhttp() + "version2";
        newAPIVersion = restAPIPublisher.createNewAPIVersion(apiVersion, apiId, true);
        // verify Default Version still not changed to 2.0.0 API
        APIDTO storeAPI = restAPIStore.getAPI(apiId);
        List<APIEndpointURLsDTO> endpointURLs = storeAPI.getEndpointURLs();
        Assert.assertNotNull(endpointURLs);
        Assert.assertEquals(endpointURLs.size(), 1);
        APIDefaultVersionURLsDTO defaultVersionURLs = endpointURLs.get(0).getDefaultVersionURLs();
        Assert.assertNotNull(defaultVersionURLs);
        Assert.assertNotNull(defaultVersionURLs.getHttp());
        Assert.assertNotNull(defaultVersionURLs.getHttps());
        //Going to access the API without the version in the request url.
        HttpResponse directResponse = invokeWithGet(endpointUrl, new HashMap<>());

        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Bearer " + accessToken);
        // Check Still Default API Invocation works successfully.
        String defaultVersionAPIInvocationUrl = getAPIInvocationURLHttp(apiContext);
        HttpResponse defaultHttpResponse = invokeWithGet(defaultVersionAPIInvocationUrl, headers);
        assertEquals(defaultHttpResponse.getData(), directResponse.getData(),
                "Default version API test failed while " + "invoking the API.");
        Assert.assertEquals(defaultHttpResponse.getHeaders().get("Version"), "v1");
        restAPIPublisher.changeAPILifeCycleStatusToPublish(newAPIVersion, false);
        // Default API not changing after publish
        defaultHttpResponse = invokeWithGet(defaultVersionAPIInvocationUrl, headers);
        assertEquals(defaultHttpResponse.getData(), directResponse.getData(),
                "Default version API test failed while " + "invoking the API.");
        Assert.assertEquals(defaultHttpResponse.getHeaders().get("Version"), "v1");
        org.wso2.am.integration.clients.publisher.api.v1.dto.APIDTO newVersionAPI =
                restAPIPublisher.getAPIByID(newAPIVersion);
        JSONObject endpointConfigJson = new JSONObject();
        endpointConfigJson.putAll((Map) newVersionAPI.getEndpointConfig());
        Map productionEndpoint = (Map) endpointConfigJson.get("production_endpoints");
        productionEndpoint.replace("url", endpointUrl);
        newVersionAPI.setEndpointConfig(endpointConfigJson);
        restAPIPublisher.updateAPI(newVersionAPI);
        APIRevisionDTO apiRevisionDTO = restAPIPublisher.addAPIRevision(newAPIVersion);
        APIRevisionDeployUndeployRequest apiRevisionDeployUndeployRequest = new APIRevisionDeployUndeployRequest();
        apiRevisionDeployUndeployRequest.setName(Constants.GATEWAY_ENVIRONMENT);
        apiRevisionDeployUndeployRequest.setVhost("localhost");
        apiRevisionDeployUndeployRequest.setDisplayOnDevportal(true);
        restAPIPublisher.deployAPIRevision(newAPIVersion, apiRevisionDTO.getId(), apiRevisionDeployUndeployRequest,
                "API");
        APIDTO storeAPIAfterUpdate = restAPIStore.getAPI(apiId);
        endpointURLs = storeAPIAfterUpdate.getEndpointURLs();
        Assert.assertNotNull(endpointURLs);
        Assert.assertEquals(endpointURLs.size(), 1);
        defaultVersionURLs = endpointURLs.get(0).getDefaultVersionURLs();
        Assert.assertNotNull(defaultVersionURLs);
        Assert.assertNull(defaultVersionURLs.getHttp());
        Assert.assertNull(defaultVersionURLs.getHttps());
        APIDTO newVersionFromStore = restAPIStore.getAPI(newAPIVersion);
        endpointURLs = newVersionFromStore.getEndpointURLs();
        Assert.assertNotNull(endpointURLs);
        Assert.assertEquals(endpointURLs.size(), 1);
        defaultVersionURLs = endpointURLs.get(0).getDefaultVersionURLs();
        Assert.assertNotNull(defaultVersionURLs);
        Assert.assertNotNull(defaultVersionURLs.getHttp());
        Assert.assertNotNull(defaultVersionURLs.getHttps());
        waitForAPIDeploymentSync(newVersionFromStore.getProvider(), newVersionFromStore.getName(),
                newVersionFromStore.getVersion(), APIMIntegrationConstants.IS_API_EXISTS);
        HttpResponse defaultHttpResponse1 = invokeWithGet(defaultVersionAPIInvocationUrl, headers);
        assertEquals(defaultHttpResponse1.getData(), directResponse.getData(),
                "Default version API test failed while " + "invoking the API.");
        Assert.assertEquals(defaultHttpResponse1.getHeaders().get("Version"), "v2");
    }

    @Test(groups = "wso2.am", description = "change default Version back to v1", dependsOnMethods = {
            "testDefaultVersionAPI", "createNewVersionWithDefaultVersionOptionAndVerifyDefaultAPIBreaking"})
    public void changeNewVersionBacktoV1AndVerify() throws Exception {
        String apiContext = "defaultversion";
        org.wso2.am.integration.clients.publisher.api.v1.dto.APIDTO oldAPI =
                restAPIPublisher.getAPIByID(apiId);
        oldAPI.setIsDefaultVersion(true);
        restAPIPublisher.updateAPI(oldAPI);
        APIDTO storeAPI = restAPIStore.getAPI(apiId);
        List<APIEndpointURLsDTO> endpointURLs = storeAPI.getEndpointURLs();
        Assert.assertNotNull(endpointURLs);
        Assert.assertEquals(endpointURLs.size(), 1);
        APIDefaultVersionURLsDTO defaultVersionURLs = endpointURLs.get(0).getDefaultVersionURLs();
        Assert.assertNotNull(defaultVersionURLs);
        Assert.assertNotNull(defaultVersionURLs.getHttp());
        Assert.assertNotNull(defaultVersionURLs.getHttps());
        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Bearer " + accessToken);
        // Check Still Default API Invocation works successfully.
        waitForAPIDeployment();
        String defaultVersionAPIInvocationUrl = getAPIInvocationURLHttp(apiContext);
        HttpResponse defaultHttpResponse = invokeWithGet(defaultVersionAPIInvocationUrl, headers);
        Assert.assertEquals(defaultHttpResponse.getHeaders().get("Version"), "v1");

        APIDTO storeAPIAfterUpdate = restAPIStore.getAPI(newAPIVersion);
        endpointURLs = storeAPIAfterUpdate.getEndpointURLs();
        Assert.assertNotNull(endpointURLs);
        Assert.assertEquals(endpointURLs.size(), 1);
        defaultVersionURLs = endpointURLs.get(0).getDefaultVersionURLs();
        Assert.assertNotNull(defaultVersionURLs);
        Assert.assertNull(defaultVersionURLs.getHttp());
        Assert.assertNull(defaultVersionURLs.getHttps());
        oldAPI = restAPIPublisher.getAPIByID(apiId);
        oldAPI.setIsDefaultVersion(false);
        restAPIPublisher.updateAPI(oldAPI);
        defaultHttpResponse = invokeDefaultAPIWithWait(defaultVersionAPIInvocationUrl, headers, 404);
        Assert.assertEquals(defaultHttpResponse.getResponseCode(), 404);
        storeAPIAfterUpdate = restAPIStore.getAPI(apiId);
        endpointURLs = storeAPIAfterUpdate.getEndpointURLs();
        Assert.assertNotNull(endpointURLs);
        Assert.assertEquals(endpointURLs.size(), 1);
        defaultVersionURLs = endpointURLs.get(0).getDefaultVersionURLs();
        Assert.assertNotNull(defaultVersionURLs);
        Assert.assertNull(defaultVersionURLs.getHttp());
        Assert.assertNull(defaultVersionURLs.getHttps());
    }

    @Test(groups = "wso2.am", dependsOnMethods = "testDefaultVersionAPI", description = "Check default" +
            " API invocation when context is similar to the version")
    public void testDefaultVersionAPIWithContextAndVersionSimilar() throws Exception {
        String newApiName = "DefaultNewVersionAPI";
        String newApiContext = "general/v1";
        String newVersion = "v1";
        String endpointUrl = getGatewayURLNhttp() + "response";
        String provider = publisherContext.getContextTenant().getContextUser().getUserName();

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
        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Bearer " + accessToken);

        String newApiInvocationUrl = getAPIInvocationURLHttp(newApiContext);
        HttpResponse httpResponse = invokeDefaultAPIWithWait(newApiInvocationUrl, headers, 200);
        assertEquals(httpResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK,
                "Cannot invoke default version of APIs with similar context and versions");
    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
        restAPIStore.deleteApplication(applicationID);
        restAPIPublisher.deleteAPI(apiId);
        restAPIPublisher.deleteAPI(newAPIVersion);
        restAPIPublisher.deleteAPI(newApiId);
    }

    private HttpResponse invokeDefaultAPIWithWait(String invocationUrl, Map<String, String> headers,
                                                  int statusCode) throws IOException, InterruptedException {
        HttpResponse response = invokeWithGet(invocationUrl, headers);
        int count = 0;
        if (response.getResponseCode() == statusCode) {
            return response;

        } else {
            do {
                Thread.sleep(10000);
                response = invokeWithGet(invocationUrl, headers);
                if (response.getResponseCode() == 200) {
                    return response;
                }
                count++;
            } while (count > 6);
            return response;
        }
    }

    private HttpResponse invokeWithGet(String url, Map<String, String> headers) throws IOException {
        HttpClient httpclient = HttpClientBuilder.create().build();
        HttpGet get = new HttpGet(url);
        headers.forEach(get::addHeader);
        org.apache.http.HttpResponse response = httpclient.execute(get);
        InputStream stream = response.getEntity().getContent();
        String content = IOUtils.toString(stream);
        Map<String, String> outputHeaders = new HashMap();
        for (Header header : response.getAllHeaders()) {
            outputHeaders.put(header.getName(), header.getValue());
        }
        return new HttpResponse(content, response.getStatusLine().getStatusCode(), outputHeaders);
    }
}
