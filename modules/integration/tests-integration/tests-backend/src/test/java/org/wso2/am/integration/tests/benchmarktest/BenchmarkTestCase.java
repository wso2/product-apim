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

package org.wso2.am.integration.tests.benchmarktest;

import java.io.IOException;
import java.lang.reflect.Method;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import io.restassured.RestAssured;
import org.json.simple.parser.ParseException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import org.wso2.am.integration.test.utils.base.APIMIntegrationBaseTest;
import org.wso2.carbon.automation.engine.annotations.ExecutionEnvironment;
import org.wso2.carbon.automation.engine.annotations.SetEnvironment;

@SetEnvironment(executionEnvironments = {ExecutionEnvironment.STANDALONE})
    public class BenchmarkTestCase extends APIMIntegrationBaseTest {

    private int benchmark;
    private static String apimHost;
    private String apiUUID;
    private String applicationID;
    private String corellationID;
    private String testName;
    private String context;
    private LocalTime startTime;
    private static List<String> apiIdList = new ArrayList<>();
    private static List<String> appIdList = new ArrayList<>();

    BenchmarkUtils benchmarkUtils = new BenchmarkUtils();

    @BeforeClass(alwaysRun = true)
        public void setEnvironment() throws Exception {
            RestAssured.useRelaxedHTTPSValidation();
            apimHost = benchmarkUtils.getApimURL();
            System.setProperty("apim.url", apimHost);
        }

        @BeforeMethod
        public void generateConsumerCredentialsAndToken() throws IOException {
            benchmarkUtils.generateConsumerCredentialsAndAccessToken();
        }

        @Test(dataProvider = "testType")
        public void createRestApi(String testType,Method method)
            throws IOException, InterruptedException, ParseException {
            benchmark = BenchmarkUtils.getBenchmark(testType, "API_CREATE");
            testName = method.getName();
            String corellationID = benchmarkUtils.setActivityID();
            LocalTime startTime = benchmarkUtils.getCurrentTimeStamp();
            apiUUID = benchmarkUtils.createRestAPI("TestAPI"+testType,"samplecontext"+testType,corellationID );
            apiIdList.add(apiUUID);
            int actualCount =  benchmarkUtils.extractCountsFromLog(testName, testType, startTime);
            benchmarkUtils.writeResultsToFile(testType, testName, actualCount, benchmark);
            benchmarkUtils.validateBenchmark(benchmark,actualCount);
        }

        @Test(dataProvider = "testType")
        public void publishRestApi(String testType, Method method)
            throws InterruptedException, IOException, ParseException {
            benchmark = BenchmarkUtils.getBenchmark(testType, "API_PUBLISH");
            String corellationID = benchmarkUtils.setActivityID();
            testName = method.getName();
            context = "testcontext_"+testName+testType;
            apiUUID = benchmarkUtils.createRestAPI(testName+testType,context,"" );
            LocalTime startTime = benchmarkUtils.getCurrentTimeStamp();
            benchmarkUtils.publishAPI(apiUUID, corellationID);
            apiIdList.add(apiUUID);
            int actualCount =  benchmarkUtils.extractCountsFromLog(testName, testType, startTime);
            benchmarkUtils.writeResultsToFile(testType, testName, actualCount, benchmark);
            benchmarkUtils.validateBenchmark(benchmark,actualCount);
        }

        @Test(dataProvider = "testType")
        public void retrieveAllApisFromPublisher(String testType, Method method)
            throws InterruptedException, IOException, ParseException {
            testName = method.getName();
            benchmark = BenchmarkUtils.getBenchmark(testType, "RETRIEVE_ALL_PUBLISHER");
            int noOfAPISCreated = 20;
            int noOfAPISRetrieved = 10;
            context = "testcontext_"+testName+testType;
            String corellationID = benchmarkUtils.setActivityID();

            for(int i= 0; i<noOfAPISCreated; i++){
                apiUUID = benchmarkUtils.createRestAPI(testName+testType+i, context+i, "");
                benchmarkUtils.publishAPI(apiUUID, "");
                apiIdList.add(apiUUID);
            }
            LocalTime startTime = benchmarkUtils.getCurrentTimeStamp();
            benchmarkUtils.retrieveAllApisFromPublisher(noOfAPISRetrieved, corellationID);
            int actualCount =  benchmarkUtils.extractCountsFromLog(testName, testType, startTime);
            benchmarkUtils.writeResultsToFile(testType, testName, actualCount, benchmark);
            benchmarkUtils.validateBenchmark(benchmark,actualCount);        }

        @Test(dataProvider = "testType")
        public void retrieveAllApisFromDevPortal(String testType, Method method)
            throws InterruptedException, IOException, ParseException {
            int noOfApisCreated = 20;
            int noOfAPISRetrieved = 10;
            benchmark = BenchmarkUtils.getBenchmark(testType, "RETRIEVE_ALL_STORE");
            testName = method.getName();
            context = "testcontext_"+testName;
            String corellationID = benchmarkUtils.setActivityID();
            for(int i= 0; i<noOfApisCreated; i++){
                apiUUID = benchmarkUtils.createRestAPI(testName+testType+i, context+testType+i, "");
                benchmarkUtils.publishAPI(apiUUID, "");
                apiIdList.add(apiUUID);
            }
            while (!(benchmarkUtils.getDevPortalApiCount() >= noOfApisCreated)) {
                        Thread.sleep(1000); }
            startTime = benchmarkUtils.getCurrentTimeStamp();
            benchmarkUtils.retrieveAllApisFromStore(noOfAPISRetrieved, corellationID);
            int actualCount =  benchmarkUtils.extractCountsFromLog(testName, testType, startTime);
            benchmarkUtils.writeResultsToFile(testType, testName, actualCount, benchmark);
            benchmarkUtils.validateBenchmark(benchmark,actualCount);        }

        @Test(dataProvider = "testType")
        public void retrieveAnApiFromPublisher(String testType, Method method)
            throws InterruptedException, IOException, ParseException {
            testName = method.getName();
            context = "testcontext_"+testName;
            corellationID = benchmarkUtils.setActivityID();
            benchmark = BenchmarkUtils.getBenchmark(testType, "RETRIEVE_API_PUBLISHER");
            apiUUID = benchmarkUtils.createRestAPI("TestAPI_"+testName+testType,context+testType,"" );
            benchmarkUtils.publishAPI(apiUUID, "");
            apiIdList.add(apiUUID);
            startTime = benchmarkUtils.getCurrentTimeStamp();
            benchmarkUtils.retrieveAnApiFromPublisher(corellationID, apiUUID);
            int actualCount =  benchmarkUtils.extractCountsFromLog(testName, testType, startTime);
            apiIdList.add(apiUUID);
            benchmarkUtils.writeResultsToFile(testType, testName, actualCount, benchmark);
            benchmarkUtils.validateBenchmark(benchmark,actualCount);
        }

        @Test(dataProvider = "testType")
        public void retrieveAnApiFromStore(String testType, Method method)
            throws InterruptedException, IOException, ParseException {
            testName = method.getName();
            context = "testcontext_"+testName;
            corellationID = benchmarkUtils.setActivityID();
            benchmark = BenchmarkUtils.getBenchmark(testType, "RETRIEVE_API_STORE");

            apiUUID = benchmarkUtils.createRestAPI("TestAPI_"+testName+testType,context+testType,"" );
            benchmarkUtils.publishAPI(apiUUID, "");
            startTime = benchmarkUtils.getCurrentTimeStamp();
            benchmarkUtils.retrieveAnApiFromStore(corellationID, apiUUID);
            int actualCount =  benchmarkUtils.extractCountsFromLog(testName, testType, startTime);
            apiIdList.add(apiUUID);
            benchmarkUtils.writeResultsToFile(testType, testName, actualCount, benchmark);
            benchmarkUtils.validateBenchmark(benchmark,actualCount);
        }

        @Test(dataProvider = "testType")
        public void createAnApplication(String testType, Method method)
            throws InterruptedException, IOException, ParseException {
            testName = method.getName();
            benchmark = BenchmarkUtils.getBenchmark(testType, "CREATE_APPLICATION");
            corellationID = benchmarkUtils.setActivityID()+testName;
            startTime = benchmarkUtils.getCurrentTimeStamp();
            applicationID = benchmarkUtils.createAnApplication("MyTestAPP_"+testName+testType,corellationID);
            int actualCount =  benchmarkUtils.extractCountsFromLog(testName, testType, startTime);
            appIdList.add(applicationID);
            benchmarkUtils.writeResultsToFile(testType, testName, actualCount, benchmark);
            benchmarkUtils.validateBenchmark(benchmark,actualCount);
        }

        @Test(dataProvider = "testType")
        public void subscribeToAnAPI(String testType, Method method)
            throws InterruptedException, IOException, ParseException {
            testName = method.getName();
            context = "testcontext_"+testName;
            benchmark = BenchmarkUtils.getBenchmark(testType, "SUBSCRIBE_TO_API");
            corellationID = benchmarkUtils.setActivityID();
            apiUUID = benchmarkUtils.createRestAPI("TestAPI_"+testName+testType,context+testType,"" );
            benchmarkUtils.publishAPI(apiUUID,"");
            applicationID = benchmarkUtils.createAnApplication("MyTestAPP_"+testName+testType,"");
            startTime = benchmarkUtils.getCurrentTimeStamp();
            benchmarkUtils.addSubscription(apiUUID,applicationID,corellationID);
            int actualCount =  benchmarkUtils.extractCountsFromLog(testName, testType, startTime);
            apiIdList.add(apiUUID);
            appIdList.add(applicationID);
            benchmarkUtils.writeResultsToFile(testType, testName, actualCount, benchmark);
            benchmarkUtils.validateBenchmark(benchmark,actualCount);
    }

        @Test(dataProvider = "testType")
        public void generateJwtAccessToken(String testType, Method method)
            throws InterruptedException, IOException, ParseException {
            testName = method.getName();
            context = "samplecontext_"+testName;
            benchmark = BenchmarkUtils.getBenchmark(testType, "GENERATE_JWT_TOKEN");
            corellationID = benchmarkUtils.setActivityID();

            apiUUID = benchmarkUtils.createRestAPI("TestAPI_"+testName+testType,context+testType,"" );
            benchmarkUtils.publishAPI(apiUUID, "");
            applicationID = benchmarkUtils.createAnApplication("MyTestAPP_"+testName+testType,"");
            benchmarkUtils.addSubscription(apiUUID,applicationID,"");
            startTime = benchmarkUtils.getCurrentTimeStamp();
            benchmarkUtils.generateApplicationToken(applicationID,corellationID);
            int actualCount =  benchmarkUtils.extractCountsFromLog(testName, testType, startTime);
            apiIdList.add(apiUUID);
            appIdList.add(applicationID);
            benchmarkUtils.writeResultsToFile(testType, testName, actualCount, benchmark);
            benchmarkUtils.validateBenchmark(benchmark,actualCount);
        }

        @Test(dataProvider = "testType")
        public void invokeCreatedApi(String testType, Method method)
            throws InterruptedException, IOException, ParseException {
            testName = method.getName();
            context = "samplecontext_"+testName;
            corellationID = benchmarkUtils.setActivityID();
            benchmark = BenchmarkUtils.getBenchmark(testType, "INVOKE_API");

            apiUUID = benchmarkUtils.createRestAPI("TestAPI_"+testName+testType,context+testType,"" );
            benchmarkUtils.publishAPI(apiUUID, "");
            applicationID = benchmarkUtils.createAnApplication("MyTestAPP_"+testName+testType,"");
            benchmarkUtils.addSubscription(apiUUID,applicationID,"");
            startTime = benchmarkUtils.getCurrentTimeStamp();
            benchmarkUtils.invokeAPI(context+testType,benchmarkUtils.generateApplicationToken(applicationID, ""), corellationID);
            int actualCount =  benchmarkUtils.extractCountsFromLog(testName, testType, startTime);
            apiIdList.add(apiUUID);
            appIdList.add(applicationID);
            benchmarkUtils.writeResultsToFile(testType, testName, actualCount, benchmark);
            benchmarkUtils.validateBenchmark(benchmark,actualCount);
    }

@AfterClass(alwaysRun = true)
public void cleanTestData() throws InterruptedException {
            Thread.sleep(5000);

    if(appIdList!=null){
    for (String appId : appIdList) {
        benchmarkUtils.deleteApplication(appId);
    }}
    for (String apiId : apiIdList) {
        benchmarkUtils.deleteRestAPI(apiId);
    }
}
        @DataProvider(name = "testType")
        public static Object[][] DataProvide() {
            return new Object[][]{
                {"jdbc"},{"http"}
            };
        }
    }

