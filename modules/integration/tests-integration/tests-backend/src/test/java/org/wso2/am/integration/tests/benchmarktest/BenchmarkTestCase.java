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

import static io.restassured.RestAssured.given;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import io.restassured.RestAssured;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.am.admin.clients.registry.ResourceAdminServiceClient;
import org.wso2.am.integration.test.utils.base.APIMIntegrationBaseTest;
import org.wso2.carbon.automation.engine.annotations.ExecutionEnvironment;
import org.wso2.carbon.automation.engine.annotations.SetEnvironment;
import org.wso2.carbon.automation.engine.context.AutomationContext;
import org.wso2.carbon.integration.common.utils.mgt.ServerConfigurationManager;

    @SetEnvironment(executionEnvironments = {ExecutionEnvironment.STANDALONE})
    public class BenchmarkTestCase extends APIMIntegrationBaseTest {
        int statusCode;
        String publisherConsumerSecret;
        String publisherConsumerKey;
        String apiUUID;
        String applicationID;
        String accessToken;
        private final String APIM_URL = "https://localhost:9443";
        private final String TOKEN_URL = "https://localhost:8243";
        private final String TARGET_URL = "https://localhost:8243";
        private final String SUPER_TENANT_USERNAME = "admin";
        private final String SUPER_TENANT_PASSWORD = "admin";
        private static List<String> apiIdList = new ArrayList<>();
        private static List<String> appIdList = new ArrayList<>();

        BenchmarkUtils benchmarkUtils = new BenchmarkUtils();

        //        private static final Log log = LogFactory.getLog(org.wso2.am.integration.tests.tests.ConfigDeploymentConfig.class);
        private final String TENANT_CONFIG_LOCATION = "/_system/config/apimgt/applicationdata/tenant-conf.json";
        private ServerConfigurationManager serverConfigurationManager;
        private AutomationContext superTenantKeyManagerContext;
        private ResourceAdminServiceClient resourceAdminServiceClient;

        @BeforeClass(alwaysRun = true)
        public void setEnvironment() throws Exception {
            RestAssured.useRelaxedHTTPSValidation();
//            serverConfigurationManager = new ServerConfigurationManager(superTenantKeyManagerContext);
//            serverConfigurationManager.restartGracefully();
        }

//        @Test
//        public void createRestApi() {
//            String corellationID = benchmarkUtils.setActivityID();
//            benchmarkUtils.generateConsumerCredentials();
//            apiUUID = benchmarkUtils.createRestAPI("TestAPI","samplecontext",corellationID );
//            apiIdList.add(apiUUID);
////            benchmarkUtils.deleteRestAPI(apiUUID);
//          int actualCount =  benchmarkUtils.extractSqlQueries("sqlLog",corellationID,"jdbc");
//          benchmarkUtils.validateBenchmark(12,actualCount);
//        }


//        @Test
//        public void publishRestApi() {
//            String corellationID = benchmarkUtils.setActivityID();
//
//            benchmarkUtils.generateConsumerCredentials();
//            apiUUID = benchmarkUtils.createRestAPI("TestAPI","samplecontext","" );
//            benchmarkUtils.publishAPI(apiUUID, corellationID);
//            apiIdList.add(apiUUID);
////            benchmarkUtils.deleteRestAPI(apiUUID);
//            int actualCount =  benchmarkUtils.extractSqlQueries("sqlLog",corellationID,"jdbc");
//            benchmarkUtils.validateBenchmark(34,actualCount);
//        }
//
//        @Test
//        public void retieveAllApis() throws InterruptedException {
//            String corellationID = benchmarkUtils.setActivityID();
//
//            for(int i= 0; i<10; i++){
//                benchmarkUtils.generateConsumerCredentials();
//                apiUUID = benchmarkUtils.createRestAPI("TestAPI_"+i, "samplecontext_"+i, "");
//                benchmarkUtils.publishAPI(apiUUID, "");
//                apiIdList.add(apiUUID);
//            }
//            Thread.sleep(1000);
//            benchmarkUtils.retrieveAllApis(10, corellationID);
//            int actualCount =  benchmarkUtils.extractSqlQueries("sqlLog",corellationID,"jdbc");
//          benchmarkUtils.validateBenchmark(34,actualCount);
//        }
//
//                @Test
//        public void retieveAllApisFromDevPortal() throws InterruptedException {
//            String corellationID = benchmarkUtils.setActivityID();
//            int noOfApisCreated = 10;
//            for(int i= 0; i<noOfApisCreated; i++){
//                benchmarkUtils.generateConsumerCredentials();
//                apiUUID = benchmarkUtils.createRestAPI("TestAPI_"+i, "samplecontext_"+i, "");
//                benchmarkUtils.publishAPI(apiUUID, "");
//                apiIdList.add(apiUUID);
//            }
//            while (!(benchmarkUtils.getDevPortalApiCount() >= noOfApisCreated)) {
//                        Thread.sleep(1000); }
//
//            benchmarkUtils.retrieveAllApisFromStore(10, corellationID);
//            int actualCount =  benchmarkUtils.extractSqlQueries("sqlLog",corellationID,"jdbc");
//          benchmarkUtils.validateBenchmark(34,actualCount);
//        }
//
//        @Test
//        public void createAnApplication() {
//            String corellationID = benchmarkUtils.setActivityID();
//
//            benchmarkUtils.generateConsumerCredentials();
////            apiUUID = benchmarkUtils.createRestAPI("TestAPI","samplecontext","" );
////            benchmarkUtils.publishAPI(apiUUID, "");
////            apiIdList.add(apiUUID);
//            applicationID = benchmarkUtils.createAnApplication("MyTestAPP",corellationID);
//            appIdList.add(applicationID);
//            int actualCount =  benchmarkUtils.extractSqlQueries("sqlLog",corellationID,"jdbc");
//            benchmarkUtils.validateBenchmark(34,actualCount);
//        }
//
//        @Test
//        public void subscribeToAnAPI() {
//            String corellationID = benchmarkUtils.setActivityID();
//
//            benchmarkUtils.generateConsumerCredentials();
//            apiUUID = benchmarkUtils.createRestAPI("TestAPI","samplecontext","" );
//            benchmarkUtils.publishAPI(apiUUID,"");
//            apiIdList.add(apiUUID);
//            applicationID = benchmarkUtils.createAnApplication("MyTestAPP","");
//            benchmarkUtils.addSubscription(apiUUID,applicationID,corellationID);
//            int actualCount =  benchmarkUtils.extractSqlQueries("sqlLog",corellationID,"jdbc");
//            benchmarkUtils.validateBenchmark(12,actualCount);
//        }
//
//        @Test
//        public void generateJwtAccessToken() {
//            String corellationID = benchmarkUtils.setActivityID();
//
//            benchmarkUtils.generateConsumerCredentials();
//            apiUUID = benchmarkUtils.createRestAPI("TestAPI","samplecontext","" );
//            benchmarkUtils.publishAPI(apiUUID, "");
//            apiIdList.add(apiUUID);
//            applicationID = benchmarkUtils.createAnApplication("MyTestAPP","");
//            benchmarkUtils.addSubscription(apiUUID,applicationID,"");
//            benchmarkUtils.generateApplicationToken(applicationID,corellationID);
//            int actualCount =  benchmarkUtils.extractSqlQueries("sqlLog",corellationID,"jdbc");
//            benchmarkUtils.validateBenchmark(34,actualCount);
//        }

        @Test
        public void invokeCreatedApi() {
            String corellationID = benchmarkUtils.setActivityID();

            benchmarkUtils.generateConsumerCredentials();
            apiUUID = benchmarkUtils.createRestAPI("TestAPI","samplecontext","" );
            benchmarkUtils.publishAPI(apiUUID, "");
            apiIdList.add(apiUUID);
            applicationID = benchmarkUtils.createAnApplication("MyTestAPP","");
            benchmarkUtils.addSubscription(apiUUID,applicationID,"");
            benchmarkUtils.invokeAPI("samplecontext",benchmarkUtils.generateApplicationToken(applicationID, ""), corellationID);
            int actualCount =  benchmarkUtils.extractSqlQueries("sqlLog",corellationID,"http");
            benchmarkUtils.validateBenchmark(12,actualCount);
        }

@AfterMethod(alwaysRun = true)
public void clear() {
    if(applicationID!=null){
        benchmarkUtils.deleteApplication(applicationID);
        System.out.println("APP IS DELETED");
    }
//    for (String appId : appIdList) {
//        benchmarkUtils.deleteRestAPI(appId);
//        System.out.println("APIS IS DELETETD " +appId);
//    }
    for (String apiId : apiIdList) {
        benchmarkUtils.deleteRestAPI(apiId);
        System.out.println("APIS IS DELETETD " +apiId);
    }
}

    }

