/*
 *Copyright (c) 2020, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.am.integration.tests.benchmarktest;

import static org.junit.Assert.assertNotNull;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.Response;
import javax.xml.xpath.XPathExpressionException;

import org.apache.commons.lang.StringUtils;
import org.json.simple.parser.ParseException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;
import org.wso2.am.integration.clients.publisher.api.ApiException;
import org.wso2.am.integration.clients.store.api.v1.dto.APIDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyGenerateRequestDTO;
import org.wso2.am.integration.test.utils.APIManagerIntegrationTestException;
import org.wso2.am.integration.test.utils.base.APIMIntegrationBaseTest;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.bean.APILifeCycleAction;
import org.wso2.am.integration.test.utils.bean.APIRequest;
import org.wso2.carbon.automation.engine.annotations.ExecutionEnvironment;
import org.wso2.carbon.automation.engine.annotations.SetEnvironment;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.test.utils.http.client.HttpRequestUtil;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

@SetEnvironment(executionEnvironments = {ExecutionEnvironment.STANDALONE})
public class BenchmarkTestCase extends APIMIntegrationBaseTest {

    protected static final String TIER_UNLIMITED = "Unlimited";
    private static final String JDBC_METRIC = "jdbc";
    private static final String EXTERNAL_API_METRIC = "http";
    private final String API_END_POINT_POSTFIX_URL = "jaxrs_basic/services/customers/customerservice/";
    private final String API_END_POINT_METHOD = "/customers/123";
    private final String API_VERSION_1_0_0 = "1.0.0";
    BenchmarkUtils benchmarkUtils = new BenchmarkUtils();
    List<String> idList = new ArrayList<String>();
    List<String> apiIdList = new ArrayList<>();
    private String apiUUID;
    private String applicationID;
    private String testName;
    private String context;
    private LocalTime startTime;
    private String providerName;
    private String apiEndPointUrl;
    private String scenario;

    @Factory(dataProvider = "userModeDataProvider")
    public BenchmarkTestCase(TestUserMode userMode) {
        this.userMode = userMode;
    }

    @DataProvider
    public static Object[][] userModeDataProvider() {
        return new Object[][] {
            new Object[] {TestUserMode.SUPER_TENANT_ADMIN},
            new Object[] {TestUserMode.TENANT_ADMIN}
        };
    }

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {
        super.init(userMode);
        apiEndPointUrl = backEndServerUrl.getWebAppURLHttp() + API_END_POINT_POSTFIX_URL;
        providerName = user.getUserName();
    }

    @Test(dependsOnMethods = "createAnApplication")
    public void createRestApi(Method method)
        throws IOException, InterruptedException, ParseException, APIManagerIntegrationTestException, ApiException {
        scenario = "API_CREATE";
        benchmarkUtils.setTenancy(userMode);
        testName = method.getName();
        LocalTime startTime = benchmarkUtils.getCurrentTimeStamp();
        apiUUID = createAnApi("NewAPI", "sampleContext");
//        Validate the JDBC query counts executed from correlation log
        benchmarkUtils.validateBenchmarkResults(testName, JDBC_METRIC, startTime, scenario, providerName);
//        Validate external api request counts executed from correlation log
        benchmarkUtils.validateBenchmarkResults(testName, EXTERNAL_API_METRIC, startTime, scenario, providerName);
        apiIdList.add(apiUUID);
    }

    @Test
    public void publishRestApi(Method method)
        throws InterruptedException, IOException, ParseException, APIManagerIntegrationTestException, ApiException {
        scenario = "API_PUBLISH";
        benchmarkUtils.setTenancy(userMode);
        testName = method.getName();
        apiUUID = createAnApi("NAME_" + testName, "Context_" + testName);
        LocalTime startTime = benchmarkUtils.getCurrentTimeStamp();

        HttpResponse response = restAPIPublisher
            .changeAPILifeCycleStatus(apiUUID, APILifeCycleAction.PUBLISH.getAction(), null);
        assertEquals(response.getResponseCode(), Response.Status.OK.getStatusCode(),
                     "API publish Response code is invalid " + apiUUID);

//        Validate the JDBC query counts executed from correlation log
        benchmarkUtils.validateBenchmarkResults(testName, JDBC_METRIC, startTime, scenario, providerName);
//        Validate external api request counts executed from correlation log
        benchmarkUtils.validateBenchmarkResults(testName, EXTERNAL_API_METRIC, startTime, scenario, providerName);
//        benchmarkUtils.deleteRestAPI(apiUUID);
        apiIdList.add(apiUUID);
    }

    @Test
    public void retrieveAllApisFromPublisher(Method method)
        throws InterruptedException, IOException, ParseException, ApiException, APIManagerIntegrationTestException {

        scenario = "RETRIEVE_ALL_PUBLISHER";
        benchmarkUtils.setTenancy(userMode);
        testName = method.getName();
        int noOfAPISCreated = 20;
        int noOfAPISRetrieved = 10;

        for (int i = 0; i < noOfAPISCreated; i++) {
            apiUUID = createAnApi(testName + i, testName + "_context" + i);
            HttpResponse response = restAPIPublisher
                .changeAPILifeCycleStatus(apiUUID, APILifeCycleAction.PUBLISH.getAction(), null);
            assertEquals(response.getResponseCode(), Response.Status.OK.getStatusCode(),
                         "API publish Response code is invalid " + apiUUID);
            apiIdList.add(apiUUID);
        }
        Thread.sleep(5000);
        LocalTime startTime = benchmarkUtils.getCurrentTimeStamp();
        restAPIPublisher.getAPIs(0, noOfAPISRetrieved);
//        Validate the JDBC query counts executed from correlation log
        benchmarkUtils.validateBenchmarkResults(testName, JDBC_METRIC, startTime, scenario, providerName);
//        Validate external api request counts executed from correlation log
        benchmarkUtils.validateBenchmarkResults(testName, EXTERNAL_API_METRIC, startTime, scenario, providerName);
        apiIdList.add(apiUUID);
    }

    @Test(dependsOnMethods = "retrieveAllApisFromPublisher")
    public void retrieveAllApisFromDevPortal(Method method)
        throws InterruptedException, IOException, ParseException,
        org.wso2.am.integration.clients.store.api.ApiException {
        scenario = "RETRIEVE_ALL_STORE";
        benchmarkUtils.setTenancy(userMode);
        int noOfAPISRetrieved = 10;
        testName = method.getName();
        startTime = benchmarkUtils.getCurrentTimeStamp();
        restAPIStore.getAPIs(0, noOfAPISRetrieved);
//        Validate the JDBC query counts executed from correlation log
        benchmarkUtils.validateBenchmarkResults(testName, JDBC_METRIC, startTime, scenario, providerName);
//        Validate external api request counts executed from correlation log
        benchmarkUtils.validateBenchmarkResults(testName, EXTERNAL_API_METRIC, startTime, scenario, providerName);
    }

    @Test
    public void retrieveAnApiFromPublisher(Method method)
        throws InterruptedException, IOException, ParseException, APIManagerIntegrationTestException, ApiException {
        scenario = "RETRIEVE_API_PUBLISHER";
        benchmarkUtils.setTenancy(userMode);
        testName = method.getName();
        apiUUID = createAnApi("NAME_" + testName, "Context_" + testName);
        apiIdList.add(apiUUID);
        startTime = benchmarkUtils.getCurrentTimeStamp();
        HttpResponse response = restAPIPublisher.getAPI(apiUUID);
        assertEquals(response.getResponseCode(), Response.Status.OK.getStatusCode(),
                     "API get Response is not as expected");
//        Validate the JDBC query counts executed from correlation log
        benchmarkUtils.validateBenchmarkResults(testName, JDBC_METRIC, startTime, scenario, providerName);
//        Validate external api request counts executed from correlation log
        benchmarkUtils.validateBenchmarkResults(testName, EXTERNAL_API_METRIC, startTime, scenario, providerName);
    }

    @Test
    public void retrieveAnApiFromStore(Method method)
        throws InterruptedException, IOException, ParseException, APIManagerIntegrationTestException, ApiException,
        org.wso2.am.integration.clients.store.api.ApiException {

        benchmarkUtils.setTenancy(userMode);
        testName = method.getName();
        scenario = "RETRIEVE_API_STORE";

        apiUUID = createAnApi("NAME_" + testName, "Context_" + testName);
        restAPIPublisher
            .changeAPILifeCycleStatus(apiUUID, APILifeCycleAction.PUBLISH.getAction(), null);
        apiIdList.add(apiUUID);
        startTime = benchmarkUtils.getCurrentTimeStamp();
        APIDTO apidto = restAPIStore.getAPI(apiUUID);
        assertTrue(StringUtils.isNotEmpty(apidto.getId()));

//        Validate the JDBC query counts executed from correlation log
        benchmarkUtils.validateBenchmarkResults(testName, JDBC_METRIC, startTime, scenario, providerName);
//        Validate external api request counts executed from correlation log
        benchmarkUtils.validateBenchmarkResults(testName, EXTERNAL_API_METRIC, startTime, scenario, providerName);
    }

    @Test
    public void createAnApplication(Method method)
        throws InterruptedException, IOException, ParseException {
        scenario = "CREATE_APPLICATION";
        testName = method.getName();
        benchmarkUtils.setTenancy(userMode);
        startTime = benchmarkUtils.getCurrentTimeStamp();
        HttpResponse applicationResponse = restAPIStore.createApplication("Test_Application_" + testName,
                                                                          "Test Application For Benchmark",
                                                                          APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED,
                                                                          ApplicationDTO.TokenTypeEnum.JWT);
        applicationID = applicationResponse.getData();
//        Validate the JDBC query counts executed from correlation log
        benchmarkUtils.validateBenchmarkResults(testName, JDBC_METRIC, startTime, scenario, providerName);
//        Validate external api request counts executed from correlation log
        benchmarkUtils.validateBenchmarkResults(testName, EXTERNAL_API_METRIC, startTime, scenario, providerName);
        restAPIStore.deleteApplication(applicationID);
    }

    @Test
    public void subscribeToAnAPI(Method method)
        throws InterruptedException, IOException, ParseException, APIManagerIntegrationTestException, ApiException,
        org.wso2.am.integration.clients.store.api.ApiException {

        scenario = "SUBSCRIBE_TO_API";
        testName = method.getName();
        apiUUID = createAnApi(testName, testName + "_context");

        benchmarkUtils.setTenancy(userMode);
        restAPIPublisher.changeAPILifeCycleStatus(apiUUID, APILifeCycleAction.PUBLISH.getAction(), null);
        HttpResponse applicationResponse = restAPIStore.createApplication("Application_" + testName,
                                                                          "Test Application For Benchmark",
                                                                          APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED,
                                                                          ApplicationDTO.TokenTypeEnum.JWT);
        applicationID = applicationResponse.getData();

        startTime = benchmarkUtils.getCurrentTimeStamp();
        restAPIStore.subscribeToAPI(apiUUID, applicationID, TIER_UNLIMITED);
//        Validate the JDBC query counts executed from correlation log
        benchmarkUtils.validateBenchmarkResults(testName, JDBC_METRIC, startTime, scenario, providerName);
//        Validate external api request counts executed from correlation log
        benchmarkUtils.validateBenchmarkResults(testName, EXTERNAL_API_METRIC, startTime, scenario, providerName);
        apiIdList.add(apiUUID);
        restAPIStore.deleteApplication(applicationID);
    }

    @Test
    public void generateJwtAccessToken(Method method)
        throws InterruptedException, IOException, ParseException, APIManagerIntegrationTestException, ApiException,
        org.wso2.am.integration.clients.store.api.ApiException {

        scenario = "GENERATE_JWT_TOKEN";
        ArrayList grantTypes = new ArrayList();
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.CLIENT_CREDENTIAL);
        testName = method.getName();
        benchmarkUtils.setTenancy(userMode);

        apiUUID = createAnApi(testName, testName + "_context");
        restAPIPublisher
            .changeAPILifeCycleStatus(apiUUID, APILifeCycleAction.PUBLISH.getAction(), null);

        HttpResponse applicationResponse = restAPIStore.createApplication("Application_" + testName,
                                                                          "Test Application For Benchmark",
                                                                          APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED,
                                                                          ApplicationDTO.TokenTypeEnum.JWT);
        applicationID = applicationResponse.getData();
        restAPIStore.subscribeToAPI(apiUUID, applicationID, TIER_UNLIMITED);
        startTime = benchmarkUtils.getCurrentTimeStamp();
        ApplicationKeyDTO apiKeyDTO = restAPIStore
            .generateKeys(applicationID, "3600", null, ApplicationKeyGenerateRequestDTO.KeyTypeEnum.PRODUCTION, null,
                          grantTypes);

//        Validate the JDBC query counts executed from correlation log
        benchmarkUtils.validateBenchmarkResults(testName, JDBC_METRIC, startTime, scenario, providerName);
//        Validate external api request counts executed from correlation log
        benchmarkUtils.validateBenchmarkResults(testName, EXTERNAL_API_METRIC, startTime, scenario, providerName);
        apiIdList.add(apiUUID);
        restAPIStore.deleteApplication(applicationID);
    }

    @Test
    public void invokeCreatedApi(Method method)
        throws InterruptedException, IOException, ParseException, APIManagerIntegrationTestException, ApiException,
        org.wso2.am.integration.clients.store.api.ApiException, XPathExpressionException {

        scenario = "INVOKE_API";
        ArrayList grantTypes = new ArrayList();
        Map<String, String> requestHeaders;
        testName = method.getName();
        benchmarkUtils.setTenancy(userMode);
        context = "context_" + testName;
        apiUUID = createAnApi(testName, context);
        apiIdList.add(apiUUID);

        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.CLIENT_CREDENTIAL);
        restAPIPublisher.changeAPILifeCycleStatus(apiUUID, APILifeCycleAction.PUBLISH.getAction(), null);
        HttpResponse applicationResponse = restAPIStore.createApplication("Application_" + testName,
                                                                          "Test Application For Benchmark",
                                                                          APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED,
                                                                          ApplicationDTO.TokenTypeEnum.JWT);
        applicationID = applicationResponse.getData();
        restAPIStore.subscribeToAPI(apiUUID, applicationID, TIER_UNLIMITED);
        ApplicationKeyDTO apiKeyDTO = restAPIStore
            .generateKeys(applicationID, "3600", null, ApplicationKeyGenerateRequestDTO.KeyTypeEnum.PRODUCTION, null,
                          grantTypes);
        String accessToken = apiKeyDTO.getToken().getAccessToken();
        startTime = benchmarkUtils.getCurrentTimeStamp();

        requestHeaders = new HashMap<String, String>();
        requestHeaders.put("accept", "text/xml");
        requestHeaders.put("Authorization", "Bearer " + accessToken);
        HttpResponse invokeResponse =
            HttpRequestUtil.doGet(getAPIInvocationURLHttp(context, API_VERSION_1_0_0) + API_END_POINT_METHOD, requestHeaders);
        assertEquals(invokeResponse.getResponseCode(),
                     200, "Response code mismatched");

//        Validate the JDBC query counts executed from correlation log
        benchmarkUtils.validateBenchmarkResults(testName, JDBC_METRIC, startTime, scenario, providerName);
//        Validate external api request counts executed from correlation log
        benchmarkUtils.validateBenchmarkResults(testName, EXTERNAL_API_METRIC, startTime, scenario, providerName);
        restAPIStore.deleteApplication(applicationID);
    }

    @AfterClass(alwaysRun = true)
    public void cleanUpArtifacts() throws Exception {
            for (String apiId : apiIdList) {
                restAPIPublisher.deleteAPI(apiId); }
    }

    public String createAnApi(String apiName, String context)
        throws APIManagerIntegrationTestException, ApiException, MalformedURLException {
        //Create the api creation request object
        APIRequest apiRequest;
        apiRequest = new APIRequest(apiName, context, new URL(apiEndPointUrl));
        apiRequest.setVersion(API_VERSION_1_0_0);
        apiRequest.setTiersCollection(APIMIntegrationConstants.API_TIER.UNLIMITED);
        apiRequest.setTier(APIMIntegrationConstants.API_TIER.UNLIMITED);
        apiRequest.setProvider(providerName);
        //Add the API using the API publisher.
        HttpResponse apiResponse = restAPIPublisher.addAPI(apiRequest);
        apiUUID = apiResponse.getData();

        assertEquals(apiResponse.getResponseCode(), Response.Status.CREATED.getStatusCode(),
                     "Create API Response Code is invalid." + apiUUID);
        idList.add(apiUUID);
        return apiUUID;
    }
}

