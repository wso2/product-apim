package org.wso2.am.integration.tests.benchmarktest;

import io.restassured.RestAssured;
import org.json.simple.parser.ParseException;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;
import java.lang.reflect.Method;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public class BenchmarkUITestCase {
    private static final String TEST_TYPE = "ui-jdbc";
    private static final String TEST_METRIC = "jdbc";
    private static final String SUPERTENANT_USER_NAME = "admin";
    private static final String SUPER_TENANT_PASSWORD = "admin";
    private static final String API_NAME = "API_NAME_";
    private static final String API_CONTEXT = "API_CONTEXT_";
    private static final String API_VERSION = "v1.0";
    String testName;
    UiComponents uiComponents = new UiComponents();
    BenchmarkUtils benchmarkUtils = new BenchmarkUtils();

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {
        RestAssured.useRelaxedHTTPSValidation();
    }

    @BeforeMethod()
    public void launchBrowser() throws Exception {
        uiComponents.openBrowser();
    }

    @Test(priority = 2)
    public void uiCreateAnAPI(Method method) throws IOException, InterruptedException, ParseException {

        int benchmark = BenchmarkUtils.getBenchmark(TEST_TYPE, "API_CREATE");
        testName = method.getName();
        uiComponents.navigateToPublisher();
        uiComponents.loginToPublisher(SUPERTENANT_USER_NAME, SUPER_TENANT_PASSWORD);
        uiComponents.navigateToAPIsPage();
        uiComponents.navigateToCreateAnApiPage();

        LocalTime startTime = benchmarkUtils.getCurrentTimeStamp();
        uiComponents.createAnAPI(API_NAME + testName, API_CONTEXT + testName, API_VERSION);
        benchmarkUtils.validateBenchmarkResults(testName, TEST_METRIC, startTime, benchmark);
    }

    @Test(priority = 3)
    public void uiPublishAnAPI(Method method) throws IOException, InterruptedException, ParseException {
        testName = method.getName();
        uiComponents.navigateToPublisher();
        uiComponents.loginToPublisher(SUPERTENANT_USER_NAME, SUPER_TENANT_PASSWORD);
        int benchmark = BenchmarkUtils.getBenchmark(TEST_TYPE, "API_PUBLISH");
        uiComponents.navigateToAPIsPage();
        uiComponents.navigateToCreateAnApiPage();
        uiComponents.createAnAPI(API_NAME + testName, API_CONTEXT + testName, API_VERSION);
        LocalTime startTime = benchmarkUtils.getCurrentTimeStamp();
        uiComponents.publishApi();
        benchmarkUtils.validateBenchmarkResults(testName, TEST_METRIC, startTime, benchmark);
        uiComponents.closeBrowser();
    }

    @Test(priority = 1)
    public void uiLogintoPublisherAndLoadAllAPIs(Method method)
        throws IOException, InterruptedException, ParseException {
        List<String> apiIdList = new ArrayList<>();
        testName = method.getName();
        int noOfAPISCreated = 20;
        int benchmark = BenchmarkUtils.getBenchmark(TEST_TYPE, "RETRIEVE_ALL_PUBLISHER");
        String context = API_CONTEXT + testName;
        benchmarkUtils.generateConsumerCredentialsAndAccessToken(SUPERTENANT_USER_NAME, SUPER_TENANT_PASSWORD);
        for (int i = 0; i < noOfAPISCreated; i++) {
            String apiUUID = benchmarkUtils.createRestAPI(testName + i, context + i, "");
            benchmarkUtils.publishAPI(apiUUID, "");
            apiIdList.add(apiUUID);
        }
        LocalTime startTime = benchmarkUtils.getCurrentTimeStamp();
        uiComponents.navigateToPublisher();
        uiComponents.loginToPublisher(SUPERTENANT_USER_NAME, SUPER_TENANT_PASSWORD);
        uiComponents.validateNoOfAPIS("1-10");
        benchmarkUtils.validateBenchmarkResults(testName, TEST_METRIC, startTime, benchmark);
        for (String apiId : apiIdList) {
            benchmarkUtils.deleteRestAPI(apiId);
        }
    }

    @Test(priority = 0)
    public void uiLoginToDevPortalAndLoadAllAPIs(Method method)
        throws IOException, InterruptedException, ParseException {
        benchmarkUtils.generateConsumerCredentialsAndAccessToken(SUPERTENANT_USER_NAME, SUPER_TENANT_PASSWORD);
        List<String> apiIdList = new ArrayList<>();
        testName = method.getName();
        int noOfAPISCreated = 20;
        String context = API_CONTEXT + testName;
        for (int i = 0; i < noOfAPISCreated; i++) {
            String apiUUID = benchmarkUtils.createRestAPI(testName + i, context + i, "");
            benchmarkUtils.publishAPI(apiUUID, "");
            apiIdList.add(apiUUID);
        }
        int benchmark = BenchmarkUtils.getBenchmark(TEST_TYPE, "RETRIEVE_ALL_STORE");
        LocalTime startTime = benchmarkUtils.getCurrentTimeStamp();
        uiComponents.navigateToDevPortal();
        benchmarkUtils.validateBenchmarkResults(testName, TEST_METRIC, startTime, benchmark);
        for (String apiId : apiIdList) {
            benchmarkUtils.deleteRestAPI(apiId);
        }
    }

    @Test(priority = 4)
    public void uiCreateAnApplication(Method method) throws IOException, InterruptedException, ParseException {
        testName = method.getName();
        int benchmark = BenchmarkUtils.getBenchmark(TEST_TYPE, "CREATE_APPLICATION");
        uiComponents.loginToDevPortal(SUPERTENANT_USER_NAME, SUPER_TENANT_PASSWORD);
        uiComponents.navigateToCreateAnApplicationPage();
        LocalTime startTime = benchmarkUtils.getCurrentTimeStamp();
        uiComponents.createAnApplication("APP-"+testName);
        benchmarkUtils.validateBenchmarkResults(testName, TEST_METRIC, startTime, benchmark);
        uiComponents.closeBrowser();
    }

    @AfterMethod
    public void closeBrowser() {
        uiComponents.closeBrowser();
    }
}
