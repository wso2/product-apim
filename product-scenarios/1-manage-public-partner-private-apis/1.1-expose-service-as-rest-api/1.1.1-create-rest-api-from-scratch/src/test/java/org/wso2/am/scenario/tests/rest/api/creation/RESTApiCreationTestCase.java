/*
 * Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.wso2.am.scenario.tests.rest.api.creation;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.am.integration.test.utils.APIManagerIntegrationTestException;
import org.wso2.am.scenario.test.common.APIPublisherRestClient;
import org.wso2.am.scenario.test.common.APIRequest;
import org.wso2.am.scenario.test.common.ScenarioDataProvider;
import org.wso2.am.scenario.test.common.ScenarioTestBase;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;
import org.json.JSONObject;
import org.testng.Assert;

import java.util.Properties;

public class RESTApiCreationTestCase extends ScenarioTestBase {
    private static final Log log = LogFactory.getLog(APIRequest.class);

    private APIPublisherRestClient apiPublisher;
    private String publisherURL;
    private APIRequest apiRequest;
    private Properties infraProperties;

    private String apiName;
    private String apiContext;
    private String apiVersion = "1.0.0";
    private String apiResource = "/find";
    private String apiVisibility = "public";

    private String description = "This is a API creation description";
    private String tag = "APICreationTag";
    private String tierCollection = "Silver";
    private String bizOwner = "wso2Test";
    private String bizOwnerMail = "wso2test@gmail.com";
    private String techOwner = "wso2";
    private String techOwnerMail = "wso2@gmail.com";
    private String endpointType = "secured";
    private String endpointAuthType = "basicAuth";
    private String epUsername = "wso2";
    private String epPassword = "wso2123";
    private String default_version_checked = "default_version";
    private String responseCache = "enabled";
    private String cacheTimeout = "300";
    private String subscriptions = "all_tenants";
    private String http_checked = "http";
    private String https_checked = "";
    private String inSequence = "debug_in_flow";
    private String outSequence = "debug_out_flow";

    private String backendEndPoint = "http://ws.cdyne.com/phoneverify/phoneverify.asmx";

    @BeforeClass(alwaysRun = true)
    public void init() throws APIManagerIntegrationTestException {

        infraProperties = getDeploymentProperties();
        publisherURL = infraProperties.getProperty(PUBLISHER_URL);

        if (publisherURL == null) {
            publisherURL = "https://localhost:9443/publisher";
        }

        setKeyStoreProperties();
        apiPublisher = new APIPublisherRestClient(publisherURL);
        apiPublisher.login("admin", "admin");
    }

    @Test(description = "1.1.1.1", dataProvider = "apiNames", dataProviderClass = ScenarioDataProvider.class)
    public void testRESTAPICreationWithMandatoryValues(String apiName) throws Exception {

        apiRequest = new APIRequest(apiName, "/" + apiName, apiVisibility,
                apiVersion, apiResource);

        //Design API with name,context,version,visibility and apiResource
        HttpResponse serviceResponse = apiPublisher.designAPI(apiRequest);
        verifyResponse(serviceResponse);
        verifyAPIName(apiName);
    }

    @Test(description = "1.1.1.2")
    public void testRESTAPICreationWithOptionalValues() throws Exception {
        apiName = "PhoneVerificationOptionalAdd";
        apiContext = "/phoneverifyOptionaladd";

        apiRequest = new APIRequest(apiName, apiContext, apiVisibility, apiVersion, apiResource, description, tag,
                tierCollection, backendEndPoint, bizOwner, bizOwnerMail, techOwner, techOwnerMail, endpointType,
                endpointAuthType, epUsername, epPassword, default_version_checked, responseCache, cacheTimeout,
                subscriptions, http_checked, https_checked, inSequence, outSequence);

        //Design API with name,context,version,visibility,apiResource and with all optional values
        HttpResponse serviceResponse = apiPublisher.addAPI(apiRequest);
        verifyResponse(serviceResponse);
        validateOptionalFiled();
    }

    @Test(description = "1.1.1.3")
    public void testRESTAPICreationWithwildCardResource() throws Exception {
        apiName = "APIWildCard";
        apiContext = "apiwildcard";
        apiResource = "/*";

        apiRequest = new APIRequest(apiName, apiContext, apiVisibility, apiVersion, apiResource);

        //Design API with name,context,version,visibility and wildcard apiResource
        HttpResponse serviceResponse = apiPublisher.designAPI(apiRequest);
        verifyResponse(serviceResponse);
        verifyAPIName(apiName);
    }


    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
        apiPublisher.deleteAPI("PhoneVerification", apiVersion, "admin");
        apiPublisher.deleteAPI("123567890", apiVersion, "admin");
        apiPublisher.deleteAPI("eñe", apiVersion, "admin");
        apiPublisher.deleteAPI("Pho_ne-verify?api.", apiVersion, "admin");
        apiPublisher.deleteAPI("PhoneVerificationOptionalAdd", apiVersion, "admin");
        apiPublisher.deleteAPI("APIWildCard", apiVersion, "admin");
    }

    private void validateOptionalFiled() throws APIManagerIntegrationTestException {
        HttpResponse getApi = apiPublisher.getAPI(apiName, "admin");
        JSONObject response = new JSONObject(getApi.getData());
        Assert.assertEquals(response.getJSONObject("api").get("bizOwner").toString(), bizOwner, "Expected bizOwner value not match");
        Assert.assertEquals(response.getJSONObject("api").get("bizOwnerMail").toString(), bizOwnerMail, "Expected bizOwnerMail value not match");
        Assert.assertEquals(response.getJSONObject("api").get("techOwner").toString(), techOwner, "Expected techOwner value not match");
        Assert.assertEquals(response.getJSONObject("api").get("techOwnerMail").toString(), techOwnerMail, "Expected techOwnerMail value not match with the actual value");
        Assert.assertEquals(response.getJSONObject("api").get("endpointTypeSecured").toString(), "true", "Expected endpointType value not match with the actual value");
        Assert.assertEquals(response.getJSONObject("api").get("endpointAuthTypeDigest").toString(), "false", "Expected endpointAuthType value not match with the actual value");
        Assert.assertEquals(response.getJSONObject("api").get("epUsername").toString(), epUsername, "Expected epUsername value not match");
        Assert.assertEquals(response.getJSONObject("api").get("epPassword").toString(), epPassword, "Expected epPassword value not match");
        Assert.assertEquals(response.getJSONObject("api").get("isDefaultVersion").toString(), "true", "Expected default_version_checked value not match");
        Assert.assertEquals(response.getJSONObject("api").get("responseCache").toString(), "Enabled", "Expected responseCache: value not match");
        Assert.assertEquals(response.getJSONObject("api").get("cacheTimeout").toString(), cacheTimeout, "Expected cacheTimeout value not match");
        Assert.assertEquals(response.getJSONObject("api").get("subscriptionAvailability").toString(), subscriptions, "Expected subscriptions value not match");
        Assert.assertEquals(response.getJSONObject("api").get("transport_http").toString(), "checked", "Expected http_checked value not match");
        Assert.assertEquals(response.getJSONObject("api").get("transport_https").toString(), "", "Expected https_checked value not match");
        Assert.assertEquals(response.getJSONObject("api").get("inSequence").toString(), inSequence, "Expected inSequence: value not match");
        Assert.assertEquals(response.getJSONObject("api").get("outSequence").toString(), outSequence, "Expected outSequence value not match");

    }

    private void verifyAPIName(String apiName) throws APIManagerIntegrationTestException {
        HttpResponse getApi = apiPublisher.getAPI(apiName, "admin");
        JSONObject response = new JSONObject(getApi.getData());
        Assert.assertEquals(response.getJSONObject("api").get("name").toString(), apiName, "Expected API name value not match");

    }
}
