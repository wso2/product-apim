/*
 *Copyright (c) 2022, WSO2 LLC. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.am.integration.tests.server.restart;

import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.json.simple.JSONObject;
import org.testng.Assert;
import org.testng.ITestContext;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIRevisionDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.APIDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.APIDefaultVersionURLsDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.APIEndpointURLsDTO;
import org.wso2.am.integration.test.Constants;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.bean.APIRevisionDeployUndeployRequest;
import org.wso2.am.integration.tests.api.lifecycle.APIManagerLifecycleBaseTest;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DefaultVersionAPIServerRestartTestCase extends APIManagerLifecycleBaseTest {

    private String defaultVersionApiId;
    String defaultVersionAccessToken;
    String newAPIVersion;

    @BeforeClass
    public void initialize(ITestContext ctx) throws Exception {
        super.init();
        defaultVersionApiId = (String) ctx.getAttribute("defaultVersionApiId");
        defaultVersionAccessToken = (String) ctx.getAttribute("defaultVersionAccessToken");

    }
    @Test(groups = "wso2.am", description = "Create new version of API with default Version enable in created stage " +
            "and verify")
    public void createNewVersionWithDefaultVersionOptionAndVerifyDefaultAPIBreaking() throws Exception {

        waitForAPIDeploymentSync(publisherContext.getContextTenant().getContextUser().getUserName(),
                "DefaultVersionAPI", "1.0.0", APIMIntegrationConstants.IS_API_EXISTS);

        String apiVersion = "2.0.0";
        String apiContext = "defaultversion";
        String endpointUrl = getGatewayURLNhttp() + "version2";
        newAPIVersion = restAPIPublisher.createNewAPIVersion(apiVersion, defaultVersionApiId, true);
        // verify Default Version still not changed to 2.0.0 API
        APIDTO storeAPI = restAPIStore.getAPI(defaultVersionApiId);
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
        headers.put("Authorization", "Bearer " + defaultVersionAccessToken);
        // Check Still Default API Invocation works successfully.
        String defaultVersionAPIInvocationUrl = getAPIInvocationURLHttp(apiContext);
        HttpResponse defaultHttpResponse = invokeWithGet(defaultVersionAPIInvocationUrl, headers);
        Assert.assertEquals(defaultHttpResponse.getData(), directResponse.getData(),
                "Default version API test failed while " + "invoking the API.");
        Assert.assertEquals(defaultHttpResponse.getHeaders().get("Version"), "v1");
        restAPIPublisher.changeAPILifeCycleStatusToPublish(newAPIVersion, false);
        // Default API not changing after publish
        defaultHttpResponse = invokeWithGet(defaultVersionAPIInvocationUrl, headers);
        Assert.assertEquals(defaultHttpResponse.getData(), directResponse.getData(),
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
        APIDTO storeAPIAfterUpdate = restAPIStore.getAPI(defaultVersionApiId);
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
        Assert.assertEquals(defaultHttpResponse1.getData(), directResponse.getData(),
                "Default version API test failed while " + "invoking the API.");
        Assert.assertEquals(defaultHttpResponse1.getHeaders().get("Version"), "v2");
    }

    @Test(groups = "wso2.am", description = "change default Version back to v1", dependsOnMethods = {
            "createNewVersionWithDefaultVersionOptionAndVerifyDefaultAPIBreaking"})
    public void changeNewVersionBacktoV1AndVerify() throws Exception {
        String apiContext = "defaultversion";
        org.wso2.am.integration.clients.publisher.api.v1.dto.APIDTO oldAPI =
                restAPIPublisher.getAPIByID(defaultVersionApiId);
        oldAPI.setIsDefaultVersion(true);
        restAPIPublisher.updateAPI(oldAPI);
        APIDTO storeAPI = restAPIStore.getAPI(defaultVersionApiId);
        List<APIEndpointURLsDTO> endpointURLs = storeAPI.getEndpointURLs();
        Assert.assertNotNull(endpointURLs);
        Assert.assertEquals(endpointURLs.size(), 1);
        APIDefaultVersionURLsDTO defaultVersionURLs = endpointURLs.get(0).getDefaultVersionURLs();
        Assert.assertNotNull(defaultVersionURLs);
        Assert.assertNotNull(defaultVersionURLs.getHttp());
        Assert.assertNotNull(defaultVersionURLs.getHttps());
        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Bearer " + defaultVersionAccessToken);
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
        oldAPI = restAPIPublisher.getAPIByID(defaultVersionApiId);
        oldAPI.setIsDefaultVersion(false);
        restAPIPublisher.updateAPI(oldAPI);
        defaultHttpResponse = invokeDefaultAPIWithWait(defaultVersionAPIInvocationUrl, headers, 404);
        Assert.assertEquals(defaultHttpResponse.getResponseCode(), 404);
        storeAPIAfterUpdate = restAPIStore.getAPI(defaultVersionApiId);
        endpointURLs = storeAPIAfterUpdate.getEndpointURLs();
        Assert.assertNotNull(endpointURLs);
        Assert.assertEquals(endpointURLs.size(), 1);
        defaultVersionURLs = endpointURLs.get(0).getDefaultVersionURLs();
        Assert.assertNotNull(defaultVersionURLs);
        Assert.assertNull(defaultVersionURLs.getHttp());
        Assert.assertNull(defaultVersionURLs.getHttps());
    }

    private HttpResponse invokeDefaultAPIWithWait(String invocationUrl, Map<String, String> headers,
                                                  int statusCode) throws IOException, InterruptedException {
        HttpResponse response = invokeWithGet(invocationUrl, headers);
        int count = 0;
        if (response.getResponseCode() != statusCode) {
            do {
                Thread.sleep(10000);
                response = invokeWithGet(invocationUrl, headers);
                if (response.getResponseCode() == 200) {
                    return response;
                }
                count++;
            } while (count > 6);
        }
        return response;
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
