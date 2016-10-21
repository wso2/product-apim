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
    private final String FAULT_API_NAME = "FaultAPIInvocationStatPublisherAPIName";
    private final String THROTTLE_API_NAME = "ThrottleAPIInvocationStatPublisherAPIName";
    private final String FAULT_API_CONTEXT = "FaultAPIInvocationStatPublisherContext";
    private final String THROTTLE_API_CONTEXT = "ThrottleAPIInvocationStatPublisherContext";
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
            thriftTestServer.addStreamDefinition(StreamDefinitions.getStreamDefinitionFault(), -1234);
            thriftTestServer.addStreamDefinition(StreamDefinitions.getStreamDefinitionThrottle(), -1234);
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

        apiCreationRequestBean = new APICreationRequestBean(FAULT_API_NAME, FAULT_API_CONTEXT, API_VERSION,
                providerName, new URL(endpointUrl.replace("http", "https")));
        apiCreationRequestBean.setDescription(DESCRIPTION);
        apiCreationRequestBean.setTiersCollection(TIER_COLLECTION);
        apiCreationRequestBean.setResourceBeanList(resList);

        //add api for test fault stream
        serviceResponse = apiPublisher.addAPI(apiCreationRequestBean);
        verifyResponse(serviceResponse);

        //publish the api
        updateRequest = new APILifeCycleStateRequest(FAULT_API_NAME, user.getUserName(), APILifeCycleState.PUBLISHED);
        serviceResponse = apiPublisher.changeAPILifeCycleStatus(updateRequest);
        verifyResponse(serviceResponse);
        waitForAPIDeploymentSync(user.getUserName(), FAULT_API_NAME, API_VERSION,
                APIMIntegrationConstants.IS_API_EXISTS);

        apiCreationRequestBean = new APICreationRequestBean(THROTTLE_API_NAME, THROTTLE_API_CONTEXT, API_VERSION,
                providerName, new URL(endpointUrl));
        apiCreationRequestBean.setDescription(DESCRIPTION);
        apiCreationRequestBean.setTiersCollection(TIER_COLLECTION);
        apiCreationRequestBean.setResourceBeanList(resList);
        apiCreationRequestBean.setProductionTps("2");

        //add api for test throttle stream
        serviceResponse = apiPublisher.addAPI(apiCreationRequestBean);
        verifyResponse(serviceResponse);

        //publish the api
        updateRequest = new APILifeCycleStateRequest(THROTTLE_API_NAME, user.getUserName(),
                APILifeCycleState.PUBLISHED);
        serviceResponse = apiPublisher.changeAPILifeCycleStatus(updateRequest);
        verifyResponse(serviceResponse);
        waitForAPIDeploymentSync(user.getUserName(), THROTTLE_API_NAME, API_VERSION,
                APIMIntegrationConstants.IS_API_EXISTS);
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

    @Test(groups = { "wso2.am" }, description = "Test execution time Event stream",
            dependsOnMethods = "testAnonymousApiInvocationAndEventTest")
    public void testExecutionTimeEventTest() throws Exception {
        //clear the test thrift server received event to avoid event conflicting among tenants
        thriftTestServer.clearTables();

        //invoke api
        requestHeaders.put(APIMIntegrationConstants.AUTHORIZATION_HEADER, "Bearer " + accessToken);
        String invokeURL = getAPIInvocationURLHttp(API_CONTEXT, API_VERSION);
        HttpResponse serviceResponse = HTTPSClientUtils.doGet(invokeURL + "/add?x=1&y=1", requestHeaders);
        Assert.assertEquals(HttpStatus.SC_OK, serviceResponse.getResponseCode(), "Error in response code");

        String publisher, context;
        if (TestUserMode.SUPER_TENANT_ADMIN == userMode) {
            publisher = user.getUserName() + "@" + user.getUserDomain();
            context = "/" + API_CONTEXT + "/" + API_VERSION;
        } else {
            publisher = user.getUserName();
            context = "/" + "t/" + user.getUserDomain() + "/" + API_CONTEXT + "/" + API_VERSION;
        }

        Map<String, Object> map = new HashMap<String, Object>();
        map.put("api", API_NAME);
        map.put("api_version", API_VERSION);
        map.put("tenantDomain", user.getUserDomain());
        map.put("apiPublisher", publisher);
        map.put("context", context);
        testExecutionTimeEvent(map);

        //invoke api anonymously
        thriftTestServer.clearTables();
        invokeURL = getAPIInvocationURLHttp(API_CONTEXT, API_VERSION);
        serviceResponse = HTTPSClientUtils.doGet(invokeURL + "/multiply?x=1&y=1", null);
        Assert.assertEquals(HttpStatus.SC_OK, serviceResponse.getResponseCode(), "Error in response code");
        testExecutionTimeEvent(map);
    }

    @Test(groups = { "wso2.am" }, description = "Test fault Event stream",
            dependsOnMethods = "testExecutionTimeEventTest")
    public void testFaultEventTest() throws Exception {
        //clear the test thrift server received event to avoid event conflicting among tenants
        thriftTestServer.clearTables();

        //subscribe to the api
        SubscriptionRequest subscriptionRequest = new SubscriptionRequest(FAULT_API_NAME, user.getUserName());
        subscriptionRequest.setApplicationName(APP_NAME);
        subscriptionRequest.setTier(APIMIntegrationConstants.API_TIER.UNLIMITED);
        HttpResponse serviceResponse = apiStore.subscribe(subscriptionRequest);
        verifyResponse(serviceResponse);

        //invoke api
        requestHeaders.put(APIMIntegrationConstants.AUTHORIZATION_HEADER, "Bearer " + accessToken);
        String invokeURL = getAPIInvocationURLHttp(FAULT_API_CONTEXT, API_VERSION);
        serviceResponse = HTTPSClientUtils.doGet(invokeURL + "/add?x=1&y=1", requestHeaders);
        Assert.assertEquals(HttpStatus.SC_INTERNAL_SERVER_ERROR, serviceResponse.getResponseCode(),
                "Error in response code");

        String context, resourcePath, userId;
        if (TestUserMode.SUPER_TENANT_ADMIN == userMode) {
            context = "/" + FAULT_API_CONTEXT + "/" + API_VERSION;
            resourcePath = "/add?x=1&y=1";
            userId = user.getUserName() + "@" + user.getUserDomain();
        } else {
            context = "/" + "t/" + user.getUserDomain() + "/" + FAULT_API_CONTEXT + "/" + API_VERSION;
            resourcePath = "/" + FAULT_API_CONTEXT + "/" + API_VERSION + "/add?x=1&y=1";
            userId = user.getUserName();
        }

        Map<String, Object> map = new HashMap<String, Object>();
        map.put("consumerKey", consumerKey);
        map.put("context", context);
        map.put("api_version", FAULT_API_NAME + ":v" + API_VERSION);
        map.put("api", FAULT_API_NAME);
        map.put("resourcePath", resourcePath);
        map.put("method", "GET");
        map.put("version", API_VERSION);
        map.put("errorCode", "");
        map.put("errorMessage", "");
        map.put("requestTime", "");
        map.put("userId", userId);
        map.put("tenantDomain", user.getUserDomain());
        map.put("hostName", "");
        map.put("apiPublisher", user.getUserName());
        map.put("applicationName", APP_NAME);
        map.put("applicationId", "");
        map.put("protocol", "http");
        testFaultEvent(map);

        //invoke api anonymously
        thriftTestServer.clearTables();
        invokeURL = getAPIInvocationURLHttp(FAULT_API_CONTEXT, API_VERSION);
        serviceResponse = HTTPSClientUtils.doGet(invokeURL + "/multiply?x=1&y=1", null);
        Assert.assertEquals(HttpStatus.SC_INTERNAL_SERVER_ERROR, serviceResponse.getResponseCode(),
                "Error in response code");
        map.put("consumerKey", null);

        map.put("userId", "anonymous");
        map.put("applicationName", null);
        map.put("applicationId", null);
        if (TestUserMode.SUPER_TENANT_ADMIN == userMode) {
            resourcePath = "/multiply?x=1&y=1";
        } else {
            resourcePath = "/" + FAULT_API_CONTEXT + "/" + API_VERSION + "/multiply?x=1&y=1";
        }
        map.put("resourcePath", resourcePath);
        testFaultEvent(map);
    }

    @Test(groups = { "wso2.am" }, description = "Test fault Event stream", dependsOnMethods = "testFaultEventTest")
    public void testThrottleEventTest() throws Exception {
        //clear the test thrift server received event to avoid event conflicting among tenants
        thriftTestServer.clearTables();

        //subscribe to the api
        SubscriptionRequest subscriptionRequest = new SubscriptionRequest(THROTTLE_API_NAME, user.getUserName());
        subscriptionRequest.setApplicationName(APP_NAME);
        subscriptionRequest.setTier(APIMIntegrationConstants.API_TIER.UNLIMITED);
        HttpResponse serviceResponse = apiStore.subscribe(subscriptionRequest);
        verifyResponse(serviceResponse);

        //invoke api
        requestHeaders.put(APIMIntegrationConstants.AUTHORIZATION_HEADER, "Bearer " + accessToken);
        String invokeURL = getAPIInvocationURLHttp(THROTTLE_API_CONTEXT, API_VERSION);
        for (int i = 0; i < 5; i++) {
            serviceResponse = HTTPSClientUtils.doGet(invokeURL + "/add?x=1&y=1", requestHeaders);
        }

        Assert.assertNotEquals(HttpStatus.SC_OK, serviceResponse.getResponseCode(), "Error in response code");

        String context, apiVersion, userId;
        if (TestUserMode.SUPER_TENANT_ADMIN == userMode) {
            userId = user.getUserName() + "@" + user.getUserDomain();
            context = "/" + THROTTLE_API_CONTEXT + "/" + API_VERSION;
            apiVersion = user.getUserName() + "--" + THROTTLE_API_NAME + ":v" + API_VERSION;
        } else {
            userId = user.getUserName();
            context = "/" + "t/" + user.getUserDomain() + "/" + THROTTLE_API_CONTEXT + "/" + API_VERSION;
            apiVersion = user.getUserName().replace("@", "-AT-") + "--" + THROTTLE_API_NAME + ":v" + API_VERSION;
        }

        Map<String, Object> map = new HashMap<String, Object>();
        map.put("accessToken", accessToken);
        map.put("userId", userId);
        map.put("tenantDomain", user.getUserDomain());
        map.put("api", THROTTLE_API_NAME);
        map.put("api_version", apiVersion);
        map.put("context", context);
        map.put("apiPublisher", user.getUserName());
        map.put("applicationName", APP_NAME);
        map.put("subscriber", user.getUserName());
        map.put("throttledOutReason", "HARD_LIMIT_EXCEEDED");

        testThrottleEvent(map);

        //invoke api anonymously
        thriftTestServer.clearTables();
        invokeURL = getAPIInvocationURLHttp(THROTTLE_API_CONTEXT, API_VERSION);
        for (int i = 0; i < 5; i++) {
            serviceResponse = HTTPSClientUtils.doGet(invokeURL + "/multiply?x=1&y=1", null);
        }
        Assert.assertNotEquals(HttpStatus.SC_OK, serviceResponse.getResponseCode(), "Error in response code");
        map.put("accessToken", null);
        map.put("userId", "anonymous");
        map.put("applicationName", null);
        map.put("subscriber", null);
        testThrottleEvent(map);
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
        Assert.assertNull(map.get("consumerKey"), "Wrong consumer key is received");
        String context, apiVersion, resourcePath, apiPublisher;
        if (TestUserMode.SUPER_TENANT_ADMIN == userMode) {
            context = "/" + API_CONTEXT + "/" + API_VERSION;
            apiVersion = user.getUserName() + "--" + API_NAME + ":v" + API_VERSION;
            resourcePath = "/multiply?x=1&y=1";
        } else {
            context = "/" + "t/" + user.getUserDomain() + "/" + API_CONTEXT + "/" + API_VERSION;
            apiVersion = user.getUserName().replace("@", "-AT-") + "--" + API_NAME + ":v" + API_VERSION;
            resourcePath = "/" + API_CONTEXT + "/" + API_VERSION + "/multiply?x=1&y=1";
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
        Assert.assertEquals(user.getUserName(), map.get("apiPublisher").toString(), "Wrong apiPublisher received");
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

        String context, resourcePath, apiVersion;
        if (TestUserMode.SUPER_TENANT_ADMIN == userMode) {
            context = "/" + API_CONTEXT + "/" + API_VERSION;
            apiVersion = user.getUserName() + "--" + API_NAME + ":v" + API_VERSION;
            resourcePath = "/multiply?x=1&y=1";
        } else {
            context = "/" + "t/" + user.getUserDomain() + "/" + API_CONTEXT + "/" + API_VERSION;
            apiVersion = user.getUserName().replace("@", "-AT-") + "--" + API_NAME + ":v" + API_VERSION;
            resourcePath = "/" + API_CONTEXT + "/" + API_VERSION + "/multiply?x=1&y=1";
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
        Assert.assertEquals(user.getUserName(), map.get("apiPublisher").toString(), "Wrong apiPublisher received");
        Assert.assertNull(map.get("applicationName"), "Wrong applicationName received");
        Assert.assertEquals("200", map.get("responseCode").toString(), "Wrong throttledOut state received");
        Assert.assertEquals(endpointUrl, map.get("destination").toString(), "Wrong destination url received");
    }

    /**
     * used to test ExecutionTime event stream data
     *
     * @throws Exception if any exception throws
     */
    private void testExecutionTimeEvent(Map<String, Object> expected) throws Exception {
        List<Event> executionTimeTable = null;
        long currentTime = System.currentTimeMillis();
        long waitTime = currentTime + WAIT_TIME;
        while (waitTime > System.currentTimeMillis()) {
            executionTimeTable = thriftTestServer.getDataTables()
                    .get(StreamDefinitions.APIMGT_STATISTICS_EXECUTION_TIME_STREAM_ID);
            if (executionTimeTable == null || executionTimeTable.isEmpty()) {
                Thread.sleep(1000);
                continue;
            } else {
                break;
            }
        }
        Assert.assertEquals(1, executionTimeTable.size(), "Stat publisher published events not match");

        Map<String, Object> map = convertToMap(executionTimeTable.get(0).getPayloadData(),
                StreamDefinitions.getStreamDefinitionExecutionTime());

        Assert.assertEquals(expected.get("api"), map.get("api").toString(), "Wrong API name received");
        Assert.assertEquals(expected.get("api_version"), map.get("api_version").toString(),
                "Wrong api_version received");
        Assert.assertEquals(expected.get("tenantDomain"), map.get("tenantDomain").toString(),
                "Wrong tenantDomain received");
        Assert.assertEquals(expected.get("apiPublisher"), map.get("apiPublisher").toString(),
                "Wrong apiPublisher received");
        Assert.assertEquals(expected.get("context"), map.get("context").toString(), "Wrong context received");
    }

    /**
     * used to test Fault event stream data
     *
     * @throws Exception if any exception throws
     */
    private void testFaultEvent(Map<String, Object> expected) throws Exception {
        List<Event> faultTable = null;
        long currentTime = System.currentTimeMillis();
        long waitTime = currentTime + WAIT_TIME;
        while (waitTime > System.currentTimeMillis()) {
            faultTable = thriftTestServer.getDataTables().get(StreamDefinitions.APIMGT_STATISTICS_FAULT_STREAM_ID);
            if (faultTable == null || faultTable.isEmpty()) {
                Thread.sleep(1000);
                continue;
            } else {
                break;
            }
        }
        Assert.assertEquals(1, faultTable.size(), "Stat publisher published fault events not received");

        Map<String, Object> map = convertToMap(faultTable.get(0).getPayloadData(),
                StreamDefinitions.getStreamDefinitionFault());

        Assert.assertEquals(expected.get("consumerKey"), map.get("consumerKey"), "Wrong consumerKey received");
        Assert.assertEquals(expected.get("context"), map.get("context").toString(), "Wrong context received");
        Assert.assertEquals(expected.get("api_version"), map.get("api_version").toString(),
                "Wrong api_version received");
        Assert.assertEquals(expected.get("api"), map.get("api").toString(), "Wrong api name received");
        Assert.assertEquals(expected.get("resourcePath"), map.get("resourcePath").toString(),
                "Wrong resourcePath received");
        Assert.assertEquals(expected.get("method"), map.get("method").toString(), "Wrong method received");
        Assert.assertEquals(expected.get("version"), map.get("version").toString(), "Wrong version received");
        Assert.assertEquals(expected.get("userId"), map.get("userId"), "Wrong userId received");
        Assert.assertEquals(expected.get("tenantDomain"), map.get("tenantDomain").toString(),
                "Wrong tenantDomain received");
        Assert.assertEquals(expected.get("apiPublisher"), map.get("apiPublisher").toString(),
                "Wrong apiPublisher received");
        Assert.assertEquals(expected.get("applicationName"), map.get("applicationName"),
                "Wrong applicationName received");
        Assert.assertEquals(expected.get("protocol"), map.get("protocol").toString(), "Wrong protocol received");
    }

    /**
     * used to test Throttle event stream data
     *
     * @throws Exception if any exception throws
     */
    private void testThrottleEvent(Map<String, Object> expected) throws Exception {
        List<Event> faultTable = null;
        long currentTime = System.currentTimeMillis();
        long waitTime = currentTime + WAIT_TIME;
        while (waitTime > System.currentTimeMillis()) {
            faultTable = thriftTestServer.getDataTables().get(StreamDefinitions.APIMGT_STATISTICS_THROTTLE_STREAM_ID);
            if (faultTable == null || faultTable.isEmpty()) {
                Thread.sleep(1000);
                continue;
            } else {
                break;
            }
        }
        Assert.assertTrue(faultTable.size() > 0, "Stat publisher published throttle events not received");

        Map<String, Object> map = convertToMap(faultTable.get(0).getPayloadData(),
                StreamDefinitions.getStreamDefinitionThrottle());

        Assert.assertEquals(expected.get("userId"), map.get("userId"), "Wrong userId received");
        Assert.assertEquals(expected.get("tenantDomain"), map.get("tenantDomain").toString(),
                "Wrong tenantDomain received");
        Assert.assertEquals(expected.get("api"), map.get("api").toString(), "Wrong api name received");
        Assert.assertEquals(expected.get("api_version"), map.get("api_version").toString(),
                "Wrong api_version received");
        Assert.assertEquals(expected.get("context"), map.get("context").toString(), "Wrong context received");
        Assert.assertEquals(expected.get("apiPublisher"), map.get("apiPublisher").toString(),
                "Wrong apiPublisher received");
        Assert.assertEquals(expected.get("applicationName"), map.get("applicationName"),
                "Wrong applicationName received");
        Assert.assertEquals(expected.get("subscriber"), map.get("subscriber"), "Wrong subscriber received");
        Assert.assertEquals(expected.get("throttledOutReason"), map.get("throttledOutReason").toString(),
                "Wrong throttledOutReason received");
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
