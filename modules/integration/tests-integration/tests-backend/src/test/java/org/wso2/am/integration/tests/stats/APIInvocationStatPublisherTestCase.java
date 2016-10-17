/*
* Copyright (c) 2016, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
* KIND, either express or implied. See the License for the
* specific language governing permissions and limitations
* under the License.
*
*/
package org.wso2.am.integration.tests.stats;

import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;
import org.wso2.am.integration.test.utils.base.APIMIntegrationBaseTest;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.bean.APICreationRequestBean;
import org.wso2.am.integration.test.utils.bean.APILifeCycleState;
import org.wso2.am.integration.test.utils.bean.APILifeCycleStateRequest;
import org.wso2.am.integration.test.utils.bean.APIResourceBean;
import org.wso2.am.integration.test.utils.bean.APIThrottlingTier;
import org.wso2.am.integration.test.utils.bean.APPKeyRequestGenerator;
import org.wso2.am.integration.test.utils.bean.SubscriptionRequest;
import org.wso2.am.integration.test.utils.clients.APIPublisherRestClient;
import org.wso2.am.integration.test.utils.clients.APIStoreRestClient;
import org.wso2.am.integration.test.utils.http.HTTPSClientUtils;
import org.wso2.am.integration.test.utils.thrift.DASThriftTestServer;
import org.wso2.am.integration.test.utils.thrift.StreamDefinitions;
import org.wso2.carbon.automation.engine.annotations.ExecutionEnvironment;
import org.wso2.carbon.automation.engine.annotations.SetEnvironment;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;
import org.wso2.carbon.databridge.commons.Event;
import org.wso2.carbon.integration.common.utils.mgt.ServerConfigurationManager;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class is used to test APIM statistics event publish to the DAS
 */
@SetEnvironment(executionEnvironments = { ExecutionEnvironment.STANDALONE })
public class APIInvocationStatPublisherTestCase extends APIMIntegrationBaseTest {
    private final Log log = LogFactory.getLog(APIInvocationStatPublisherTestCase.class);
    private final String API_NAME = "APIInvocationStatPublisherAPIName";
    private final String API_CONTEXT = "APIInvocationStatPublisherContext";
    private final String DESCRIPTION = "This is test API create by API manager integration test";
    private final String API_VERSION = "1.0.0";
    private final String APP_NAME = "APIInvocationStatPublisherApp";
    private final String TIER_COLLECTION = APIMIntegrationConstants.API_TIER.UNLIMITED;
    private final static DASThriftTestServer thriftTestServer = new DASThriftTestServer();
    private final int thriftServerListenPort = 7614;
    private final long WAIT_TIME = 300 * 1000;
    private String publisherURLHttps;
    private String storeURLHttp;
    private APICreationRequestBean apiCreationRequestBean;
    private List<APIResourceBean> resList;
    private String endpointUrl;
    private Map<String, String> requestHeaders = new HashMap<String, String>();
    private APIPublisherRestClient apiPublisher;
    private APIStoreRestClient apiStore;
    private String consumerKey;
    private String accessToken;
    private ServerConfigurationManager serverConfigurationManager;

    @Factory(dataProvider = "userModeDataProvider")
    public APIInvocationStatPublisherTestCase(TestUserMode userMode) {
        this.userMode = userMode;
    }

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {
        super.init(userMode);
        publisherURLHttps = publisherUrls.getWebAppURLHttp();
        storeURLHttp = getStoreURLHttp();
        endpointUrl = backEndServerUrl.getWebAppURLHttp() + "am/sample/calculator/v1/api";
        apiPublisher = new APIPublisherRestClient(publisherURLHttps);
        apiStore = new APIStoreRestClient(storeURLHttp);

        if (TestUserMode.SUPER_TENANT_ADMIN == userMode) {
            thriftTestServer.addStreamDefinition(StreamDefinitions.getStreamDefinitionRequest(), -1234);
            thriftTestServer.addStreamDefinition(StreamDefinitions.getStreamDefinitionResponse(), -1234);
            thriftTestServer.addStreamDefinition(StreamDefinitions.getStreamDefinitionExecutionTime(), -1234);
            thriftTestServer.addStreamDefinition(StreamDefinitions.getStreamDefinitionWorkflow(), -1234);
            thriftTestServer.start(thriftServerListenPort);
            serverConfigurationManager = new ServerConfigurationManager(gatewayContextWrk);
            serverConfigurationManager.applyConfiguration(new File(
                    getAMResourceLocation() + File.separator + "configFiles" + File.separator + "stats" + File.separator
                            + "api-manager.xml"));
        }
        apiPublisher.login(user.getUserName(), user.getPassword());
        apiStore.login(user.getUserName(), user.getPassword());
    }

    @Test(groups = { "wso2.am" }, description = "Sample API creation")
    public void testAPICreation() throws Exception {
        String providerName = user.getUserName();

        apiCreationRequestBean = new APICreationRequestBean(API_NAME, API_CONTEXT, API_VERSION, providerName,
                new URL(endpointUrl));
        apiCreationRequestBean.setDescription(DESCRIPTION);
        apiCreationRequestBean.setTiersCollection(TIER_COLLECTION);

        //define resources
        resList = new ArrayList<APIResourceBean>();
        APIResourceBean res1 = new APIResourceBean(APIMIntegrationConstants.HTTP_VERB_GET,
                APIMIntegrationConstants.ResourceAuthTypes.APPLICATION_AND_APPLICATION_USER.getAuthType(),
                APIMIntegrationConstants.RESOURCE_TIER.PLUS, "/add");
        APIResourceBean res2 = new APIResourceBean(APIMIntegrationConstants.HTTP_VERB_GET,
                APIMIntegrationConstants.ResourceAuthTypes.NONE.getAuthType(),
                APIMIntegrationConstants.RESOURCE_TIER.PLUS, "/multiply");
        resList.add(res1);
        resList.add(res2);

        apiCreationRequestBean.setResourceBeanList(resList);

        //add test api
        HttpResponse serviceResponse = apiPublisher.addAPI(apiCreationRequestBean);
        verifyResponse(serviceResponse);

        //publish the api
        APILifeCycleStateRequest updateRequest = new APILifeCycleStateRequest(API_NAME, user.getUserName(),
                APILifeCycleState.PUBLISHED);
        serviceResponse = apiPublisher.changeAPILifeCycleStatus(updateRequest);
        verifyResponse(serviceResponse);
        waitForAPIDeploymentSync(user.getUserName(), API_NAME, API_VERSION, APIMIntegrationConstants.IS_API_EXISTS);
    }

    @Test(groups = { "wso2.am" }, description = "Test API invocation", dependsOnMethods = "testAPICreation")
    public void testApiInvocationAndEventTest() throws Exception {
        //clear the test thrift server received event to avoid event conflicting among tenants
        thriftTestServer.clearTables();
        //add a application
        HttpResponse serviceResponse = apiStore
                .addApplication(APP_NAME, APIThrottlingTier.UNLIMITED.getState(), "", "this-is-test");
        verifyResponse(serviceResponse);

        //subscribe to the api
        SubscriptionRequest subscriptionRequest = new SubscriptionRequest(API_NAME, user.getUserName());
        subscriptionRequest.setApplicationName(APP_NAME);
        subscriptionRequest.setTier(APIMIntegrationConstants.API_TIER.UNLIMITED);
        serviceResponse = apiStore.subscribe(subscriptionRequest);
        verifyResponse(serviceResponse);

        //generate the key for the subscription
        APPKeyRequestGenerator generateAppKeyRequest = new APPKeyRequestGenerator(APP_NAME);
        String responseString = apiStore.generateApplicationKey(generateAppKeyRequest).getData();
        JSONObject response = new JSONObject(responseString);
        accessToken = response.getJSONObject("data").getJSONObject("key").get("accessToken").toString();
        consumerKey = response.getJSONObject("data").getJSONObject("key").get("consumerKey").toString();
        Assert.assertNotNull("Access Token not found " + responseString, accessToken);

        //invoke api
        requestHeaders.put(APIMIntegrationConstants.AUTHORIZATION_HEADER, "Bearer " + accessToken);
        String invokeURL = getAPIInvocationURLHttp(API_CONTEXT, API_VERSION);
        serviceResponse = HTTPSClientUtils.doGet(invokeURL + "/add?x=1&y=1", requestHeaders);
        Assert.assertEquals(HttpStatus.SC_OK, serviceResponse.getResponseCode(), "Error in response code");
        log.info("Waiting till all the events are published to the event listner..");
        //adding waiting time to prevent intermittent test failure
        Thread.sleep(10000);
        //testing request event stream
        testRequestEvent();
        //testing response event stream
        testResponseEvent();
    }

    @Test(groups = { "wso2.am" }, description = "Test Anonymous API invocation",
            dependsOnMethods = "testApiInvocationAndEventTest")
    public void testAnonymousApiInvocationAndEventTest() throws Exception {
        //clear the test thrift server received event to avoid event conflicting among tenants
        thriftTestServer.clearTables();
        String invokeURL = getAPIInvocationURLHttp(API_CONTEXT, API_VERSION);
        HttpResponse serviceResponse = HTTPSClientUtils.doGet(invokeURL + "/multiply?x=1&y=1", requestHeaders);
        Assert.assertEquals(HttpStatus.SC_OK, serviceResponse.getResponseCode(), "Error in response code");

        //testing request event stream
        testAnonymousRequestEvent();
        //testing response event stream
        testAnonymousResponseEvent();
    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
        super.cleanUp();
        thriftTestServer.stop();
        if (TestUserMode.SUPER_TENANT_ADMIN == userMode) {
            serverConfigurationManager.restoreToLastConfiguration();
        }
    }

    @DataProvider
    public static Object[][] userModeDataProvider() {
        return new Object[][] { new Object[] { TestUserMode.SUPER_TENANT_ADMIN },
                new Object[] { TestUserMode.TENANT_ADMIN }, };
    }

    /**
     * used to test request event stream data
     *
     * @throws Exception if any exception throws
     */
    private void testRequestEvent() throws Exception {
        List<Event> requestTable = null;
        long currentTime = System.currentTimeMillis();
        long waitTime = currentTime + WAIT_TIME;
        while (waitTime > System.currentTimeMillis()) {
            requestTable = thriftTestServer.getDataTables().get(StreamDefinitions.APIMGT_STATISTICS_REQUEST_STREAM_ID);
            if (requestTable == null || requestTable.isEmpty()) {
            	log.info("Request data table is empty or null. waiting 1s and retry..");
                Thread.sleep(1000);
                continue;
            } else {
                break;
            }
        }
        if (requestTable == null ) {
        	log.error("Response data table is null!!");
        }

        Assert.assertEquals(1, requestTable.size(), "Stat publisher published events not match");
        Map<String, Object> map = convertToMap(requestTable.get(0).getPayloadData(),
                StreamDefinitions.getStreamDefinitionRequest());

        Assert.assertEquals(consumerKey, map.get("consumerKey").toString(), "Wrong consumer key is received");
        String context, apiVersion, resourcePath, userId;
        if (TestUserMode.SUPER_TENANT_ADMIN == userMode) {
            context = "/" + API_CONTEXT + "/" + API_VERSION;
            apiVersion = user.getUserName() + "--" + API_NAME + ":v" + API_VERSION;
            resourcePath = "/add?x=1&y=1";
            userId = user.getUserName() + "@" + user.getUserDomain();
        } else {
            context = "/" + "t/" + user.getUserDomain() + "/" + API_CONTEXT + "/" + API_VERSION;
            apiVersion = user.getUserName().replace("@", "-AT-") + "--" + API_NAME + ":v" + API_VERSION;
            resourcePath = "/" + API_CONTEXT + "/" + API_VERSION + "/add?x=1&y=1";
            userId = user.getUserName();
        }
        Assert.assertEquals(context, map.get("context").toString(), "Wrong context received");
        Assert.assertEquals(apiVersion, map.get("api_version").toString(), "Wrong api_version received");
        Assert.assertEquals(API_NAME, map.get("api").toString(), "Wrong api name received");
        Assert.assertEquals(resourcePath, map.get("resourcePath").toString(), "Wrong resourcePath received");
        Assert.assertEquals("/add", map.get("resourceTemplate").toString(), "Wrong resourceTemplate received");
        Assert.assertEquals("GET", map.get("method").toString(), "Wrong http method method received");
        Assert.assertEquals(API_VERSION, map.get("version").toString(), "Wrong version received");
        Assert.assertEquals(1, Integer.parseInt(map.get("request").toString()), "Wrong request count received");
        Assert.assertEquals(userId, map.get("userId").toString(), "Wrong userId received");
        Assert.assertEquals(user.getUserDomain(), map.get("tenantDomain").toString(), "Wrong tenant domain received");
        Assert.assertEquals(user.getUserName(), map.get("apiPublisher").toString(), "Wrong apiPublisher received");
        Assert.assertEquals(APP_NAME, map.get("applicationName").toString(), "Wrong applicationName received");
        Assert.assertEquals(APIMIntegrationConstants.API_TIER.UNLIMITED, map.get("tier").toString(),
                "Wrong subscribe tier received");
        Assert.assertEquals("false", map.get("throttledOut").toString(), "Wrong throttledOut state received");
    }

    /**
     * used to test Anonymous request event stream data
     *
     * @throws Exception if any exception throws
     */
    private void testAnonymousRequestEvent() throws Exception {
        List<Event> requestTable = null;
        long currentTime = System.currentTimeMillis();
        long waitTime = currentTime + WAIT_TIME;
        while (waitTime > System.currentTimeMillis()) {
            requestTable = thriftTestServer.getDataTables().get(StreamDefinitions.APIMGT_STATISTICS_REQUEST_STREAM_ID);
            if (requestTable == null || requestTable.isEmpty()) {
                Thread.sleep(1000);
                continue;
            } else {
                break;
            }
        }

        Assert.assertEquals(1, requestTable.size(), "Stat publisher published events not match");
        Map<String, Object> map = convertToMap(requestTable.get(0).getPayloadData(),
                StreamDefinitions.getStreamDefinitionRequest());
        System.out.println(map);
        Assert.assertNull(map.get("consumerKey"), "Wrong consumer key is received");
        String context, apiVersion, resourcePath, apiPublisher;
        if (TestUserMode.SUPER_TENANT_ADMIN == userMode) {
            context = "/" + API_CONTEXT + "/" + API_VERSION;
            apiVersion = user.getUserName() + "--" + API_NAME + ":v" + API_VERSION;
            resourcePath = "/multiply?x=1&y=1";
            apiPublisher = user.getUserName() + "@" + user.getUserDomain();
        } else {
            context = "/" + "t/" + user.getUserDomain() + "/" + API_CONTEXT + "/" + API_VERSION;
            apiVersion = user.getUserName().replace("@", "-AT-") + "--" + API_NAME + ":v" + API_VERSION;
            resourcePath = "/" + API_CONTEXT + "/" + API_VERSION + "/multiply?x=1&y=1";
            apiPublisher = user.getUserName();
        }
        Assert.assertEquals(context, map.get("context").toString(), "Wrong context received");
        Assert.assertEquals(apiVersion, map.get("api_version").toString(), "Wrong api_version received");
        Assert.assertEquals(API_NAME, map.get("api").toString(), "Wrong api name received");
        Assert.assertEquals(resourcePath, map.get("resourcePath").toString(), "Wrong resourcePath received");
        Assert.assertEquals("/multiply", map.get("resourceTemplate").toString(), "Wrong resourceTemplate received");
        Assert.assertEquals("GET", map.get("method").toString(), "Wrong http method method received");
        Assert.assertEquals(API_VERSION, map.get("version").toString(), "Wrong version received");
        Assert.assertEquals(1, Integer.parseInt(map.get("request").toString()), "Wrong request count received");
        Assert.assertEquals("anonymous", map.get("userId").toString(), "Wrong userId received");
        Assert.assertEquals(user.getUserDomain(), map.get("tenantDomain").toString(), "Wrong tenant domain received");
        Assert.assertEquals(apiPublisher, map.get("apiPublisher").toString(), "Wrong apiPublisher received");
        Assert.assertNull(map.get("applicationName"), "Wrong applicationName received");
        Assert.assertEquals("Unauthenticated", map.get("tier").toString(), "Wrong subscribe tier received");
        Assert.assertEquals("false", map.get("throttledOut").toString(), "Wrong throttledOut state received");
    }

    /**
     * used to test response event stream data
     *
     * @throws Exception if any exception throws
     */
    private void testResponseEvent() throws Exception {
        List<Event> responseTable = null;
        long currentTime = System.currentTimeMillis();
        long waitTime = currentTime + WAIT_TIME;
        while (waitTime > System.currentTimeMillis()) {
            responseTable = thriftTestServer.getDataTables()
                    .get(StreamDefinitions.APIMGT_STATISTICS_RESPONSE_STREAM_ID);
            if (responseTable == null || responseTable.isEmpty()) {
            	log.info("Response data table is empty or null. waiting 1s and retry..");
                Thread.sleep(1000);
                continue;
            } else {
                break;
            }
        }
        if (responseTable == null ) {
        	log.error("Response data table is null!!");
        }
        Assert.assertEquals(1, responseTable.size(), "Stat publisher published events not match");

        Map<String, Object> map = convertToMap(responseTable.get(0).getPayloadData(),
                StreamDefinitions.getStreamDefinitionResponse());

        String context, resourcePath, username, apiVersion;
        if (TestUserMode.SUPER_TENANT_ADMIN == userMode) {
            context = "/" + API_CONTEXT + "/" + API_VERSION;
            apiVersion = user.getUserName() + "--" + API_NAME + ":v" + API_VERSION;
            resourcePath = "/add?x=1&y=1";
            username = user.getUserName() + "@" + user.getUserDomain();
        } else {
            context = "/" + "t/" + user.getUserDomain() + "/" + API_CONTEXT + "/" + API_VERSION;
            apiVersion = user.getUserName().replace("@", "-AT-") + "--" + API_NAME + ":v" + API_VERSION;
            resourcePath = "/" + API_CONTEXT + "/" + API_VERSION + "/add?x=1&y=1";
            username = user.getUserName();
        }

        Assert.assertEquals(consumerKey, map.get("consumerKey").toString(), "Wrong consumer key is received");
        Assert.assertEquals(context, map.get("context").toString(), "Wrong context received");
        Assert.assertEquals(apiVersion, map.get("api_version").toString(),
                "Wrong api_version received");
        Assert.assertEquals(API_NAME, map.get("api").toString(), "Wrong api name received");
        Assert.assertEquals(resourcePath, map.get("resourcePath").toString(), "Wrong resourcePath received");
        Assert.assertEquals("/add", map.get("resourceTemplate").toString(), "Wrong resourceTemplate received");
        Assert.assertEquals("GET", map.get("method").toString(), "Wrong http method method received");
        Assert.assertEquals(API_VERSION, map.get("version").toString(), "Wrong version received");
        Assert.assertEquals(1, Integer.parseInt(map.get("response").toString()), "Wrong request count received");
        Assert.assertEquals(username, map.get("username").toString(), "Wrong userId received");
        Assert.assertEquals(user.getUserDomain(), map.get("tenantDomain").toString(), "Wrong tenant domain received");
        Assert.assertEquals(user.getUserName(), map.get("apiPublisher").toString(), "Wrong apiPublisher received");
        Assert.assertEquals(APP_NAME, map.get("applicationName").toString(), "Wrong applicationName received");
        Assert.assertEquals("200", map.get("responseCode").toString(), "Wrong throttledOut state received");
        Assert.assertEquals(endpointUrl, map.get("destination").toString(), "Wrong destination url received");
    }

    /**
     * used to test Anonymous response event stream data
     *
     * @throws Exception if any exception throws
     */
    private void testAnonymousResponseEvent() throws Exception {
        List<Event> responseTable = null;
        long currentTime = System.currentTimeMillis();
        long waitTime = currentTime + WAIT_TIME;
        while (waitTime > System.currentTimeMillis()) {
            responseTable = thriftTestServer.getDataTables()
                    .get(StreamDefinitions.APIMGT_STATISTICS_RESPONSE_STREAM_ID);
            if (responseTable == null || responseTable.isEmpty()) {
                Thread.sleep(1000);
                continue;
            } else {
                break;
            }
        }
        Assert.assertEquals(1, responseTable.size(), "Stat publisher published events not match");

        Map<String, Object> map = convertToMap(responseTable.get(0).getPayloadData(),
                StreamDefinitions.getStreamDefinitionResponse());

        String context, resourcePath, username, apiVersion, apiPublisher;
        if (TestUserMode.SUPER_TENANT_ADMIN == userMode) {
            context = "/" + API_CONTEXT + "/" + API_VERSION;
            apiVersion = user.getUserName() + "--" + API_NAME + ":v" + API_VERSION;
            resourcePath = "/multiply?x=1&y=1";
            username = user.getUserName() + "@" + user.getUserDomain();
            apiPublisher = user.getUserName() + "@" + user.getUserDomain();
        } else {
            context = "/" + "t/" + user.getUserDomain() + "/" + API_CONTEXT + "/" + API_VERSION;
            apiVersion = user.getUserName().replace("@", "-AT-") + "--" + API_NAME + ":v" + API_VERSION;
            resourcePath = "/" + API_CONTEXT + "/" + API_VERSION + "/multiply?x=1&y=1";
            username = user.getUserName();
            apiPublisher = user.getUserName();
        }

        Assert.assertNull(map.get("consumerKey"), "Wrong consumer key is received");
        Assert.assertEquals(context, map.get("context").toString(), "Wrong context received");
        Assert.assertEquals(apiVersion, map.get("api_version").toString(), "Wrong api_version received");
        Assert.assertEquals(API_NAME, map.get("api").toString(), "Wrong api name received");
        Assert.assertEquals(resourcePath, map.get("resourcePath").toString(), "Wrong resourcePath received");
        Assert.assertEquals("/multiply", map.get("resourceTemplate").toString(), "Wrong resourceTemplate received");
        Assert.assertEquals("GET", map.get("method").toString(), "Wrong http method method received");
        Assert.assertEquals(API_VERSION, map.get("version").toString(), "Wrong version received");
        Assert.assertEquals(1, Integer.parseInt(map.get("response").toString()), "Wrong request count received");
        Assert.assertEquals("anonymous", map.get("username").toString(), "Wrong userId received");
        Assert.assertEquals(user.getUserDomain(), map.get("tenantDomain").toString(), "Wrong tenant domain received");
        Assert.assertEquals(apiPublisher, map.get("apiPublisher").toString(), "Wrong apiPublisher received");
        Assert.assertNull(map.get("applicationName"), "Wrong applicationName received");
        Assert.assertEquals("200", map.get("responseCode").toString(), "Wrong throttledOut state received");
        Assert.assertEquals(endpointUrl, map.get("destination").toString(), "Wrong destination url received");
    }

    /**
     * used to convert received event stream json payload into key-value pair
     *
     * @param result json payload of the event
     * @param stream Stream definition of the result
     * @return list of map having payload attribute and it's value
     * @throws JSONException throws if json payload is malformed
     */
    private Map<String, Object> convertToMap(Object[] result, String stream) throws JSONException {
        Map<String, Object> map = new HashMap<String, Object>();
        JSONArray payloadData = new JSONObject(stream).getJSONArray("payloadData");
        Assert.assertEquals(result.length, payloadData.length(), "attributes counts are not equal");
        for (int i = 0; i < result.length; i++) {
            String key = payloadData.getJSONObject(i).getString("name");
            map.put(key, result[i]);
        }
        return map;
    }
}
