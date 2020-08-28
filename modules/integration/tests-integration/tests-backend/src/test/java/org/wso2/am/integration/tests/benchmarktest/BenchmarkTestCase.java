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
import static org.wso2.carbon.automation.test.utils.common.TestConfigurationProvider.getExecutionEnvironment;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import javax.xml.xpath.XPathExpressionException;

import io.restassured.RestAssured;
import org.apache.commons.io.FileUtils;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import org.wso2.am.admin.clients.registry.ResourceAdminServiceClient;
import org.wso2.am.integration.test.utils.base.APIMIntegrationBaseTest;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.carbon.automation.engine.annotations.ExecutionEnvironment;
import org.wso2.carbon.automation.engine.annotations.SetEnvironment;
import org.wso2.carbon.automation.engine.context.AutomationContext;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.integration.common.utils.mgt.ServerConfigurationManager;

    @SetEnvironment(executionEnvironments = {ExecutionEnvironment.STANDALONE})
    public class BenchmarkTestCase extends APIMIntegrationBaseTest {
        int statusCode;
        String publisherConsumerSecret;
        String publisherConsumerKey;
        String apiUUID;
        String applicationID;
        String corellationID;
        String testName;
        String context;
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
            System.setProperty("enableCorrelationLogs","true");
        }

        public void updateConfig() throws IOException {
            String APIM_HOME = System.getProperty("carbon.home");
            Path path = Paths.get(APIM_HOME+"/bin/wso2server.sh");
            Charset charset = StandardCharsets.UTF_8;
            String content = new String(Files.readAllBytes(path), charset);
            content = content.replaceAll("-DenableCorrelationLogs=false", "-DenableCorrelationLogs=true");
            Files.write(path, content.getBytes(charset));
        }

        @Test
        public void createRestApi() throws IOException {

            System.out.println("home are set to : "+ System.getProperty("carbon.home"));
            System.out.println("Corelation logs are set to : "+ System.getProperty("enableCorrelationLogs"));

            String corellationID = benchmarkUtils.setActivityID();
            benchmarkUtils.generateConsumerCredentials();
            apiUUID = benchmarkUtils.createRestAPI("TestAPI","samplecontext",corellationID );
            apiIdList.add(apiUUID);
//            benchmarkUtils.deleteRestAPI(apiUUID);
          int actualCount =  benchmarkUtils.extractSqlQueries("sqlLog",corellationID,"jdbc");
          benchmarkUtils.validateBenchmark(12,actualCount);
        }


        @Test
        public void publishRestApi() {
            String corellationID = benchmarkUtils.setActivityID();

            benchmarkUtils.generateConsumerCredentials();
            apiUUID = benchmarkUtils.createRestAPI("TestAPI","samplecontext","" );
            benchmarkUtils.publishAPI(apiUUID, corellationID);
            apiIdList.add(apiUUID);
//            benchmarkUtils.deleteRestAPI(apiUUID);
            int actualCount =  benchmarkUtils.extractSqlQueries("sqlLog",corellationID,"jdbc");
            benchmarkUtils.validateBenchmark(34,actualCount);
        }

        @Test
        public void retieveAllApisFromPublisher() throws InterruptedException {
            String corellationID = benchmarkUtils.setActivityID();

            for(int i= 0; i<20; i++){
                benchmarkUtils.generateConsumerCredentials();
                apiUUID = benchmarkUtils.createRestAPI("TestAPI_"+i, "samplecontext_"+i, "");
                benchmarkUtils.publishAPI(apiUUID, "");
                apiIdList.add(apiUUID);
            }
            Thread.sleep(1000);
            benchmarkUtils.retrieveAllApisFromPublisher(10, corellationID);
            int actualCount =  benchmarkUtils.extractSqlQueries("sqlLog",corellationID,"jdbc");
          benchmarkUtils.validateBenchmark(34,actualCount);
        }

                @Test
        public void retieveAllApisFromDevPortal() throws InterruptedException {
            String corellationID = benchmarkUtils.setActivityID();
            int noOfApisCreated = 20;
            for(int i= 0; i<noOfApisCreated; i++){
                benchmarkUtils.generateConsumerCredentials();
                apiUUID = benchmarkUtils.createRestAPI("TestAPI_"+i, "samplecontext_"+i, "");
                benchmarkUtils.publishAPI(apiUUID, "");
                apiIdList.add(apiUUID);
            }
            while (!(benchmarkUtils.getDevPortalApiCount() >= noOfApisCreated)) {
                        Thread.sleep(1000); }

            benchmarkUtils.retrieveAllApisFromStore(10, corellationID);
            int actualCount =  benchmarkUtils.extractSqlQueries("sqlLog",corellationID,"jdbc");
          benchmarkUtils.validateBenchmark(34,actualCount);
        }

                @Test
        public void retieveAnApiFromPublisher(Method method) throws InterruptedException {
            testName = method.getName();
            context = "testcontext_"+testName;
            corellationID = benchmarkUtils.setActivityID();

            benchmarkUtils.generateConsumerCredentials();
            apiUUID = benchmarkUtils.createRestAPI("TestAPI_"+testName,context,"" );
            benchmarkUtils.publishAPI(apiUUID, "");
            apiIdList.add(apiUUID);
            benchmarkUtils.retrieveAnApiFromPublisher(corellationID, apiUUID);
            int actualCount =  benchmarkUtils.extractSqlQueries("sqlLog",corellationID,"jdbc");
            benchmarkUtils.validateBenchmark(34,actualCount);
            apiIdList.add(apiUUID);
        }

        @Test
        public void retieveAnApiFromStore(Method method) throws InterruptedException {
            testName = method.getName();
            context = "testcontext_"+testName;
            corellationID = benchmarkUtils.setActivityID();

            benchmarkUtils.generateConsumerCredentials();
            apiUUID = benchmarkUtils.createRestAPI("TestAPI_"+testName,context,"" );
            benchmarkUtils.publishAPI(apiUUID, "");
            benchmarkUtils.retrieveAnApiFromStore(corellationID, apiUUID);
            int actualCount =  benchmarkUtils.extractSqlQueries("sqlLog",corellationID,"jdbc");
            benchmarkUtils.validateBenchmark(34,actualCount);
            apiIdList.add(apiUUID);
        }

        @Test
        public void createAnApplication(Method method) {
            testName = method.getName();
            corellationID = benchmarkUtils.setActivityID();

            benchmarkUtils.generateConsumerCredentials();
            applicationID = benchmarkUtils.createAnApplication("MyTestAPP_"+testName,corellationID);
            int actualCount =  benchmarkUtils.extractSqlQueries("sqlLog",corellationID,"jdbc");
            benchmarkUtils.validateBenchmark(34,actualCount);
            appIdList.add(applicationID);
        }

        @Test
        public void subscribeToAnAPI(Method method) throws InterruptedException {
            testName = method.getName();
            context = "testcontext_"+testName;
            corellationID = benchmarkUtils.setActivityID();

            benchmarkUtils.generateConsumerCredentials();
            apiUUID = benchmarkUtils.createRestAPI("TestAPI_"+testName,context,"" );
            benchmarkUtils.publishAPI(apiUUID,"");
            applicationID = benchmarkUtils.createAnApplication("MyTestAPP_"+testName,"");
            benchmarkUtils.addSubscription(apiUUID,applicationID,corellationID);
            int actualCount =  benchmarkUtils.extractSqlQueries("sqlLog",corellationID,"jdbc");
            apiIdList.add(apiUUID);
            appIdList.add(applicationID);
            benchmarkUtils.validateBenchmark(44,actualCount);
        }
//
        @Test(dataProvider = "testType")
        public void generateJwtAccessToken(String testType, Method method) {
            testName = method.getName();
            context = "samplecontext_"+testName;
            corellationID = benchmarkUtils.setActivityID();

            benchmarkUtils.generateConsumerCredentials();
            apiUUID = benchmarkUtils.createRestAPI("TestAPI_"+testName,context,"" );
            benchmarkUtils.publishAPI(apiUUID, "");
            applicationID = benchmarkUtils.createAnApplication("MyTestAPP_"+testName,"");
            benchmarkUtils.addSubscription(apiUUID,applicationID,"");
            benchmarkUtils.generateApplicationToken(applicationID,corellationID);
            int actualCount =  benchmarkUtils.extractSqlQueries("sqlLog",corellationID,testType);
            benchmarkUtils.validateBenchmark(34,actualCount);
            apiIdList.add(apiUUID);
            appIdList.add(applicationID);
        }

        @Test
        public void invokeCreatedApi(Method method) throws XPathExpressionException, InterruptedException {
            testName = method.getName();
            context = "samplecontext_"+testName;
            corellationID = benchmarkUtils.setActivityID();


            benchmarkUtils.generateConsumerCredentials();
            apiUUID = benchmarkUtils.createRestAPI("TestAPI_"+testName,context,"" );
            benchmarkUtils.publishAPI(apiUUID, "");
            applicationID = benchmarkUtils.createAnApplication("MyTestAPP_"+testName,"");
            benchmarkUtils.addSubscription(apiUUID,applicationID,"");
            benchmarkUtils.invokeAPI(context,benchmarkUtils.generateApplicationToken(applicationID, ""), corellationID);
            int actualCount =  benchmarkUtils.extractSqlQueries("sqlLog",corellationID,"http");
            apiIdList.add(apiUUID);
            appIdList.add(applicationID);
            benchmarkUtils.validateBenchmark(12,actualCount);
            System.out.println("WAITING HAS STARTED !!!!!!!!!!!!!!!!!!!!!!!!!");
//     Thread.sleep(500000);
        }

@AfterClass(alwaysRun = true)
public void cleanTestData() throws InterruptedException {
            Thread.sleep(5000);
    for (String appId : appIdList) {
        benchmarkUtils.deleteApplication(appId);
        System.out.println("Apps are DELETETD " +appId);
    }
    for (String apiId : apiIdList) {
        benchmarkUtils.deleteRestAPI(apiId);
        System.out.println("Apis are DELETETD " +apiId);
    }
}
        @DataProvider(name = "testType")
        public static Object[][] ApiDataProvide() {
            return new Object[][]{
                {"jdbc"}
            };
        }
    }

