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

import java.io.IOException;
import java.lang.reflect.Method;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import io.restassured.RestAssured;
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.filter.log.ResponseLoggingFilter;
import org.json.simple.parser.ParseException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import org.wso2.am.integration.test.utils.base.APIMIntegrationBaseTest;
import org.wso2.carbon.automation.engine.annotations.ExecutionEnvironment;
import org.wso2.carbon.automation.engine.annotations.SetEnvironment;

@SetEnvironment(executionEnvironments = {ExecutionEnvironment.STANDALONE})
public class BenchmarkTestCase extends APIMIntegrationBaseTest {

    private static final String SUPERTENANT_USERNAME = "admin";
    private static final String SUPERTENANT_PASSWORD = "admin";
    private static final String TENANT_USERNAME = "nironw@wso2.com";
    private static final String TENANT_PASSWORD = "nironw";
    BenchmarkUtils benchmarkUtils = new BenchmarkUtils();
    private int benchmark;
    private String apiUUID;
    private String applicationID;
    private String corellationID;
    private String testName;
    private String context;
    private LocalTime startTime;

    @DataProvider(name = "testMetric")
    public static Object[][] DataProvider() {

        return new Object[][] {
            {"jdbc", SUPERTENANT_USERNAME, SUPERTENANT_PASSWORD},
            {"http", SUPERTENANT_USERNAME, SUPERTENANT_PASSWORD},
            {"jdbc", TENANT_USERNAME, TENANT_PASSWORD},
            {"http", TENANT_USERNAME, TENANT_PASSWORD}
        };
    }

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {
// Benchmark values are defined in "src/test/resources/benchmark-values"
        RestAssured.useRelaxedHTTPSValidation();
        benchmarkUtils.enableRestassuredHttpLogs(Boolean.valueOf(System.getProperty("okHttpLogs")));
        RestAssured.filters(new RequestLoggingFilter(), new ResponseLoggingFilter());
        System.setProperty("apim.url", benchmarkUtils.getApimURL());
    }

    @Test(dataProvider = "testMetric", priority = 1)
    public void createRestApi(String testMetric, String userName, String password, Method method)
        throws IOException, InterruptedException, ParseException {

        benchmarkUtils.generateConsumerCredentialsAndAccessToken(userName, password);
        benchmark = BenchmarkUtils.getBenchmark(testMetric, "API_CREATE");
        testName = method.getName();
        String corellationID = benchmarkUtils.setActivityID();
        LocalTime startTime = benchmarkUtils.getCurrentTimeStamp();
        apiUUID = benchmarkUtils.createRestAPI("TestAPI" + testMetric, "samplecontext" + testMetric, corellationID);

        benchmarkUtils.validateBenchmarkResults(testName, testMetric, startTime, benchmark);
        benchmarkUtils.deleteRestAPI(apiUUID);
    }

    @Test(dataProvider = "testMetric", priority = 2)
    public void publishRestApi(String testMetric, String userName, String password, Method method)
        throws InterruptedException, IOException, ParseException {

        benchmarkUtils.generateConsumerCredentialsAndAccessToken(userName, password);
        benchmark = BenchmarkUtils.getBenchmark(testMetric, "API_PUBLISH");
        String corellationID = benchmarkUtils.setActivityID();
        testName = method.getName();
        context = "testcontext_" + testName + testMetric;
        apiUUID = benchmarkUtils.createRestAPI(testName + testMetric, context, "");
        LocalTime startTime = benchmarkUtils.getCurrentTimeStamp();
        benchmarkUtils.publishAPI(apiUUID, corellationID);
        benchmarkUtils.validateBenchmarkResults(testName, testMetric, startTime, benchmark);

        benchmarkUtils.deleteRestAPI(apiUUID);
    }

    @Test(dataProvider = "testMetric", priority = 3)
    public void retrieveAllApisFromPublisher(String testMetric, String userName, String password, Method method)
        throws InterruptedException, IOException, ParseException {

        benchmarkUtils.generateConsumerCredentialsAndAccessToken(userName, password);
        List<String> apiIdList = new ArrayList<>();
        testName = method.getName();
        benchmark = BenchmarkUtils.getBenchmark(testMetric, "RETRIEVE_ALL_PUBLISHER");
        int noOfAPISCreated = 20;
        int noOfAPISRetrieved = 10;
        context = "testcontext_" + testName + testMetric;
        String corellationID = benchmarkUtils.setActivityID();

        for (int i = 0; i < noOfAPISCreated; i++) {
            apiUUID = benchmarkUtils.createRestAPI(testName + testMetric + i, context + i, "");
            benchmarkUtils.publishAPI(apiUUID, "");
            apiIdList.add(apiUUID);
        }
        LocalTime startTime = benchmarkUtils.getCurrentTimeStamp();
        benchmarkUtils.retrieveAllApisFromPublisher(noOfAPISRetrieved, corellationID);
        benchmarkUtils.validateBenchmarkResults(testName, testMetric, startTime, benchmark);

        for (String apiId : apiIdList) {
            benchmarkUtils.deleteRestAPI(apiId);
        }
    }

    @Test(dataProvider = "testMetric", priority = 4)
    public void retrieveAllApisFromDevPortal(String testMetric, String userName, String password, Method method)
        throws InterruptedException, IOException, ParseException {

        benchmarkUtils.generateConsumerCredentialsAndAccessToken(userName, password);
        List<String> apiIdList = new ArrayList<>();
        int noOfApisCreated = 20;
        int noOfAPISRetrieved = 10;
        benchmark = BenchmarkUtils.getBenchmark(testMetric, "RETRIEVE_ALL_STORE");
        testName = method.getName();
        context = "testcontext_" + testName;
        String corellationID = benchmarkUtils.setActivityID();
        for (int i = 0; i < noOfApisCreated; i++) {
            apiUUID = benchmarkUtils.createRestAPI(testName + testMetric + i, context + testMetric + i, "");
            benchmarkUtils.publishAPI(apiUUID, "");
            apiIdList.add(apiUUID);
        }
        while (!(benchmarkUtils.getDevPortalApiCount() >= noOfApisCreated)) {
            Thread.sleep(1000);
        }
        startTime = benchmarkUtils.getCurrentTimeStamp();
        benchmarkUtils.retrieveAllApisFromStore(noOfAPISRetrieved, corellationID);
        benchmarkUtils.validateBenchmarkResults(testName, testMetric, startTime, benchmark);

        for (String apiId : apiIdList) {
            benchmarkUtils.deleteRestAPI(apiId);
        }
    }

    @Test(dataProvider = "testMetric", priority = 5)
    public void retrieveAnApiFromPublisher(String testMetric, String userName, String password, Method method)
        throws InterruptedException, IOException, ParseException {

        benchmarkUtils.generateConsumerCredentialsAndAccessToken(userName, password);
        testName = method.getName();
        context = "testcontext_" + testName;
        corellationID = benchmarkUtils.setActivityID();
        benchmark = BenchmarkUtils.getBenchmark(testMetric, "RETRIEVE_API_PUBLISHER");
        apiUUID = benchmarkUtils.createRestAPI("TestAPI_" + testName + testMetric, context + testMetric, "");
        benchmarkUtils.publishAPI(apiUUID, "");
        startTime = benchmarkUtils.getCurrentTimeStamp();
        benchmarkUtils.retrieveApiFromPublisher(corellationID, apiUUID);
        benchmarkUtils.validateBenchmarkResults(testName, testMetric, startTime, benchmark);

        benchmarkUtils.deleteRestAPI(apiUUID);
    }

    @Test(dataProvider = "testMetric", priority = 6)
    public void retrieveAnApiFromStore(String testMetric, String userName, String password, Method method)
        throws InterruptedException, IOException, ParseException {

        benchmarkUtils.generateConsumerCredentialsAndAccessToken(userName, password);
        testName = method.getName();
        context = "testcontext_" + testName;
        corellationID = benchmarkUtils.setActivityID();
        benchmark = BenchmarkUtils.getBenchmark(testMetric, "RETRIEVE_API_STORE");

        apiUUID = benchmarkUtils.createRestAPI("TestAPI_" + testName + testMetric, context + testMetric, "");
        benchmarkUtils.publishAPI(apiUUID, "");
        startTime = benchmarkUtils.getCurrentTimeStamp();
        benchmarkUtils.retrieveApiFromStore(corellationID, apiUUID);
        benchmarkUtils.validateBenchmarkResults(testName, testMetric, startTime, benchmark);

        benchmarkUtils.deleteRestAPI(apiUUID);
    }

    //
    @Test(dataProvider = "testMetric", priority = 0)
    public void createAnApplication(String testMetric, String userName, String password, Method method)
        throws InterruptedException, IOException, ParseException {

        testName = method.getName();
        benchmarkUtils.generateConsumerCredentialsAndAccessToken(userName, password);
        benchmark = BenchmarkUtils.getBenchmark(testMetric, "CREATE_APPLICATION");
        corellationID = benchmarkUtils.setActivityID() + testName;
        startTime = benchmarkUtils.getCurrentTimeStamp();
        applicationID = benchmarkUtils.createAnApplication("MyTestAPP_" + testName + testMetric, corellationID);
        benchmarkUtils.validateBenchmarkResults(testName, testMetric, startTime, benchmark);

        benchmarkUtils.deleteApplication(applicationID);
    }

    @Test(dataProvider = "testMetric", priority = 7)
    public void subscribeToAnAPI(String testMetric, String userName, String password, Method method)
        throws InterruptedException, IOException, ParseException {

        testName = method.getName();
        benchmarkUtils.generateConsumerCredentialsAndAccessToken(userName, password);
        context = "testcontext_" + testName;
        benchmark = BenchmarkUtils.getBenchmark(testMetric, "SUBSCRIBE_TO_API");
        corellationID = benchmarkUtils.setActivityID();
        apiUUID = benchmarkUtils.createRestAPI("TestAPI_" + testName + testMetric, context + testMetric, "");
        benchmarkUtils.publishAPI(apiUUID, "");
        applicationID = benchmarkUtils.createAnApplication("MyTestAPP_" + testName + testMetric, "");
        startTime = benchmarkUtils.getCurrentTimeStamp();
        benchmarkUtils.addSubscription(apiUUID, applicationID, corellationID);
        benchmarkUtils.validateBenchmarkResults(testName, testMetric, startTime, benchmark);

        benchmarkUtils.deleteApplication(applicationID);
        benchmarkUtils.deleteRestAPI(apiUUID);
    }

    @Test(dataProvider = "testMetric", priority = 8)
    public void generateJwtAccessToken(String testMetric, String userName, String password, Method method)
        throws InterruptedException, IOException, ParseException {

        testName = method.getName();
        benchmarkUtils.generateConsumerCredentialsAndAccessToken(userName, password);
        context = "samplecontext_" + testName;
        benchmark = BenchmarkUtils.getBenchmark(testMetric, "GENERATE_JWT_TOKEN");
        corellationID = benchmarkUtils.setActivityID();

        apiUUID = benchmarkUtils.createRestAPI("TestAPI_" + testName + testMetric, context + testMetric, "");
        benchmarkUtils.publishAPI(apiUUID, "");
        applicationID = benchmarkUtils.createAnApplication("MyTestAPP_" + testName + testMetric, "");
        benchmarkUtils.addSubscription(apiUUID, applicationID, "");
        startTime = benchmarkUtils.getCurrentTimeStamp();
        benchmarkUtils.generateApplicationToken(applicationID, corellationID);
        benchmarkUtils.validateBenchmarkResults(testName, testMetric, startTime, benchmark);

        benchmarkUtils.deleteApplication(applicationID);
        benchmarkUtils.deleteRestAPI(apiUUID);
    }

    @Test(dataProvider = "testMetric", priority = 9)
    public void invokeCreatedApi(String testMetric, String userName, String password, Method method)
        throws InterruptedException, IOException, ParseException {
        benchmarkUtils.generateConsumerCredentialsAndAccessToken(userName, password);
        testName = method.getName();
        context = "samplecontext_" + testName;
        corellationID = benchmarkUtils.setActivityID();
        benchmark = BenchmarkUtils.getBenchmark(testMetric, "INVOKE_API");
        apiUUID = benchmarkUtils.createRestAPI("TestAPI_" + testName + testMetric, context + testMetric, "");
        benchmarkUtils.publishAPI(apiUUID, "");
        applicationID = benchmarkUtils.createAnApplication("MyTestAPP_" + testName + testMetric, "");
        benchmarkUtils.addSubscription(apiUUID, applicationID, "");
        String applicationToken = benchmarkUtils.generateApplicationToken(applicationID, "");
        startTime = benchmarkUtils.getCurrentTimeStamp();
        benchmarkUtils.invokeAPI(context + testMetric, applicationToken, corellationID);
        benchmarkUtils.validateBenchmarkResults(testName, testMetric, startTime, benchmark);

        benchmarkUtils.deleteApplication(applicationID);
        benchmarkUtils.deleteRestAPI(apiUUID);
    }
}

