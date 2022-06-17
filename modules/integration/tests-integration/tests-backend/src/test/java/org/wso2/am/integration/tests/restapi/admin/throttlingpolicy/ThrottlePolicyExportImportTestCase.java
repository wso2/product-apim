/*
 *
 *   Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *   WSO2 Inc. licenses this file to you under the Apache License,
 *   Version 2.0 (the "License"); you may not use this file except
 *   in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing,
 *   software distributed under the License is distributed on an
 *   "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *   KIND, either express or implied.  See the License for the
 *   specific language governing permissions and limitations
 *   under the License.
 *
 */

package org.wso2.am.integration.tests.restapi.admin.throttlingpolicy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.Files;
import com.google.gson.Gson;
import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;
import org.wso2.am.integration.clients.admin.ApiException;
import org.wso2.am.integration.clients.admin.ApiResponse;
import org.wso2.am.integration.clients.admin.api.dto.*;
import org.wso2.am.integration.test.helpers.AdminApiTestHelper;
import org.wso2.am.integration.test.impl.DtoFactory;
import org.wso2.am.integration.test.utils.APIManagerIntegrationTestException;
import org.wso2.am.integration.test.utils.base.APIMIntegrationBaseTest;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.http.HTTPSClientUtils;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.integration.common.admin.client.UserManagementClient;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.testng.Assert.assertEquals;
import static org.wso2.am.integration.test.utils.generic.APIMTestCaseUtils.encodeCredentials;

/**
 * This test case is used to test the Throttle Policy Import Export
 */
public class ThrottlePolicyExportImportTestCase extends APIMIntegrationBaseTest {
    private final String displayName = "Test Policy";
    private final String timeUnit = "min";
    private final Integer unitTime = 1;
    private final String ADMIN_ROLE = "admin";
    private final String ADMIN1_USERNAME = "admin1";
    private final String PASSWORD = "admin1";
    private final String APIM_VERSION = "v4.1.0";
    private final String description = "This is a test throttle policy";
    private final String advancedPolicyName = "TestPolicyAdvanced";
    private final String applicationPolicyName = "TestPolicyApplication";
    private final String customPolicyName = "TestPolicyCustom";
    private final String subscriptionPolicyName = "TestPolicySubscription";
    private final String THROTTLE_POLICY_TYPE = "throttling policy";
    private final String ADVANCED_POLICY_SUBTYPE = "advanced policy";
    private final String APPLICATION_POLICY_SUBTYPE = "application policy";
    private final String SUBSCRIPTION_POLICY_SUBTYPE = "subscription policy";
    private final String CUSTOM_POLICY_SUBTYPE = "custom rule";
    private final String advancedPolicyType = "api";
    private final String applicationPolicyType = "app";
    private final String subscriptionPolicyType = "sub";
    private final String customPolicyType = "global";
    private final String exportRestAPIResource = "/throttling/policies/export";
    private final String importRestAPIResource = "/throttling/policies/import";
    private AdvancedThrottlePolicyDTO AdvancedPolicyDTO;
    private ApplicationThrottlePolicyDTO ApplicationPolicyDTO;
    private CustomRuleDTO CustomPolicyDTO;
    private SubscriptionThrottlePolicyDTO SubscriptionPolicyDTO;
    private AdminApiTestHelper adminApiTestHelper;
    private String exportUrl;
    private String importUrl;
    private String advancedPolicyId;
    private String applicationPolicyId;
    private String subscriptionPolicyId;
    private String customPolicyId;
    private File exportedFileAdvancedPolicy;
    private File exportedFileApplicationPolicy;
    private File exportedFileCustomPolicy;
    private File exportedFileSubscriptionPolicy;

    @Factory(dataProvider = "userModeDataProvider")
    public ThrottlePolicyExportImportTestCase(TestUserMode userMode) {
        this.userMode = userMode;
    }

    @DataProvider public static Object[][] userModeDataProvider() {
        return new Object[][] { new Object[] { TestUserMode.SUPER_TENANT_ADMIN },
                new Object[] { TestUserMode.TENANT_ADMIN } };
    }

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {
        super.init(userMode);
        adminApiTestHelper = new AdminApiTestHelper();

        exportUrl = adminURLHttps + APIMIntegrationConstants.REST_API_ADMIN_CONTEXT_FULL_0 + exportRestAPIResource;
        importUrl = adminURLHttps + APIMIntegrationConstants.REST_API_ADMIN_CONTEXT_FULL_0 + importRestAPIResource;

        addAdvancedPolicy();
        addApplicationPolicy();
        addSubscriptionPolicy();
        addCustomPolicy();
    }

    /**
     * Adds a new subscription policy
     */
    public void addSubscriptionPolicy() throws Exception {

        String internal_creator = "Internal/creator";
        String timeUnit = "min";
        Integer unitTime = 1;
        int graphQLMaxComplexity = 400;
        int graphQLMaxDepth = 10;
        int rateLimitCount = -1;
        String rateLimitTimeUnit = "NA";
        boolean stopQuotaOnReach = false;
        String billingPlan = "COMMERCIAL";
        int subscriberCount = 0;
        MonetizationInfoDTO monetization = new MonetizationInfoDTO();
        ArrayList<CustomAttributeDTO> customAttributes = new ArrayList<>();
        CustomAttributeDTO attribute = new CustomAttributeDTO();
        attribute.setName("testAttribute");
        attribute.setValue("testValue");
        customAttributes.add(attribute);
        Long requestCount = 50L;
        ArrayList<String> roleList = new ArrayList<>();
        roleList.add(internal_creator);
        monetization.setMonetizationPlan(MonetizationInfoDTO.MonetizationPlanEnum.DYNAMICRATE);
        RequestCountLimitDTO requestCountLimit = DtoFactory.createRequestCountLimitDTO(timeUnit, unitTime,
                requestCount);
        ThrottleLimitDTO defaultLimit = DtoFactory.createThrottleLimitDTO(ThrottleLimitDTO.TypeEnum.REQUESTCOUNTLIMIT,
                requestCountLimit, null);
        SubscriptionThrottlePolicyPermissionDTO permissions = DtoFactory.createSubscriptionThrottlePolicyPermissionDTO(
                SubscriptionThrottlePolicyPermissionDTO.PermissionTypeEnum.ALLOW, roleList);
        SubscriptionPolicyDTO = DtoFactory.createSubscriptionThrottlePolicyDTO(subscriptionPolicyName, displayName,
                description, false, defaultLimit, graphQLMaxComplexity, graphQLMaxDepth, rateLimitCount,
                rateLimitTimeUnit, customAttributes, stopQuotaOnReach, billingPlan, subscriberCount, permissions);
        SubscriptionPolicyDTO.setMonetization(monetization);
        //Add the subscription throttling policy
        ApiResponse<SubscriptionThrottlePolicyDTO> addedPolicy = restAPIAdmin.addSubscriptionThrottlingPolicy(
                SubscriptionPolicyDTO);
        //Assert the status code and policy ID
        Assert.assertEquals(addedPolicy.getStatusCode(), HttpStatus.SC_CREATED);
        SubscriptionThrottlePolicyDTO addedPolicyDTO = addedPolicy.getData();
        subscriptionPolicyId = addedPolicyDTO.getPolicyId();
        Assert.assertNotNull(subscriptionPolicyId, "The policy ID cannot be null or empty");
        SubscriptionPolicyDTO.setPolicyId(subscriptionPolicyId);
        SubscriptionPolicyDTO.setIsDeployed(false);
        SubscriptionPolicyDTO.setType("SubscriptionThrottlePolicy");
        //Verify the created subscription throttling policy DTO
        adminApiTestHelper.verifySubscriptionThrottlePolicyDTO(SubscriptionPolicyDTO, addedPolicyDTO);
    }

    /**
     * Adds a new advanced policy
     */
    public void addAdvancedPolicy() throws Exception {
        Long requestCount = 50L;
        List<ConditionalGroupDTO> conditionalGroups = new ArrayList<>();
        RequestCountLimitDTO requestCountLimit = DtoFactory.createRequestCountLimitDTO(timeUnit, unitTime,
                requestCount);
        ThrottleLimitDTO defaultLimit = DtoFactory.createThrottleLimitDTO(ThrottleLimitDTO.TypeEnum.REQUESTCOUNTLIMIT,
                requestCountLimit, null);
        AdvancedThrottlingPolicyTestCase advancedThrottlingPolicyTestCase = new AdvancedThrottlingPolicyTestCase(
                userMode);
        conditionalGroups.add(advancedThrottlingPolicyTestCase.createConditionalGroup(defaultLimit));
        AdvancedPolicyDTO = DtoFactory.createAdvancedThrottlePolicyDTO(advancedPolicyName, displayName, description,
                false, defaultLimit, conditionalGroups);
        //Add the advanced throttling policy
        ApiResponse<AdvancedThrottlePolicyDTO> addedPolicy = restAPIAdmin.addAdvancedThrottlingPolicy(
                AdvancedPolicyDTO);
        //Assert the status code and policy ID
        Assert.assertEquals(addedPolicy.getStatusCode(), HttpStatus.SC_CREATED);
        AdvancedThrottlePolicyDTO addedPolicyDTO = addedPolicy.getData();
        advancedPolicyId = addedPolicyDTO.getPolicyId();
        Assert.assertNotNull(advancedPolicyId, "The policy ID cannot be null or empty");
        AdvancedPolicyDTO.setPolicyId(advancedPolicyId);
        AdvancedPolicyDTO.setIsDeployed(true);
        //Verify the created advanced throttling policy DTO
        adminApiTestHelper.verifyAdvancedThrottlePolicyDTO(AdvancedPolicyDTO, addedPolicyDTO);
    }

    /**
     * Adds a new application policy
     */
    public void addApplicationPolicy() throws Exception {
        Long requestCount = 50L;
        RequestCountLimitDTO requestCountLimit = DtoFactory.createRequestCountLimitDTO(timeUnit, unitTime,
                requestCount);
        ThrottleLimitDTO defaultLimit = DtoFactory.createThrottleLimitDTO(ThrottleLimitDTO.TypeEnum.REQUESTCOUNTLIMIT,
                requestCountLimit, null);
        ApplicationPolicyDTO = DtoFactory.createApplicationThrottlePolicyDTO(applicationPolicyName, displayName,
                description, false, defaultLimit);
        //Add the application throttling policy
        ApiResponse<ApplicationThrottlePolicyDTO> addedPolicy = restAPIAdmin.addApplicationThrottlingPolicy(
                ApplicationPolicyDTO);
        //Assert the status code and policy ID
        Assert.assertEquals(addedPolicy.getStatusCode(), HttpStatus.SC_CREATED);
        ApplicationThrottlePolicyDTO addedPolicyDTO = addedPolicy.getData();
        applicationPolicyId = addedPolicyDTO.getPolicyId();
        Assert.assertNotNull(applicationPolicyId, "The policy ID cannot be null or empty");
        ApplicationPolicyDTO.setPolicyId(applicationPolicyId);
        ApplicationPolicyDTO.setIsDeployed(true);
        //Verify the created application throttling policy DTO
        adminApiTestHelper.verifyApplicationThrottlePolicyDTO(ApplicationPolicyDTO, addedPolicyDTO);
    }

    /**
     * Adds a new custom policy
     */
    public void addCustomPolicy() {
        //Create the custom throttling policy DTO
        String description = "This is a test custom throttle policy";
        String siddhiQuery = "FROM RequestStream\nSELECT userId, ( userId == 'admin@carbon.super' ) AS isEligible, "
                + "str:concat('admin@carbon.super','') as throttleKey\nINSERT INTO EligibilityStream; \n\nFROM "
                + "EligibilityStream[isEligible==true]#throttler:timeBatch(1 min) \nSELECT throttleKey, (count(userId) >= 10) "
                + "as isThrottled, expiryTimeStamp group by throttleKey \nINSERT ALL EVENTS into ResultStream;";
        String keyTemplate = "$userId";
        CustomPolicyDTO = DtoFactory.createCustomThrottlePolicyDTO(customPolicyName, description, false, siddhiQuery,
                keyTemplate);

        ApiResponse<CustomRuleDTO> addedPolicy;
        try {
            //Add the custom throttling policy
            addedPolicy = restAPIAdmin.addCustomThrottlingPolicy(CustomPolicyDTO);

            //Assert the status code and policy ID
            Assert.assertEquals(addedPolicy.getStatusCode(), HttpStatus.SC_CREATED);
            CustomRuleDTO addedPolicyDTO = addedPolicy.getData();
            customPolicyId = addedPolicyDTO.getPolicyId();
            Assert.assertNotNull(customPolicyId, "The policy ID cannot be null or empty");
            CustomPolicyDTO.setPolicyId(customPolicyId);
            CustomPolicyDTO.setIsDeployed(true);
            //Verify the created custom throttling policy DTO
            adminApiTestHelper.verifyCustomThrottlePolicyDTO(CustomPolicyDTO, addedPolicyDTO);
        } catch (ApiException e) {
            if (userMode == TestUserMode.TENANT_ADMIN) {
                Assert.assertEquals(e.getCode(), HttpStatus.SC_FORBIDDEN);
            }
        }
    }

    @Test(groups = {"wso2.am"}, description = "Exported Sample Custom ThrottlePolicy" )
    public void testCustomThrottlePolicyExport() throws Exception {
        if (userMode == TestUserMode.TENANT_ADMIN) {
            throw new SkipException("Skipping the export custom policy");
        }
        exportedFileCustomPolicy = exportArtifact(user.getUserName(), user.getPassword(), customPolicyName,
                customPolicyType);
        CustomPolicyDTO.setIsDeployed(false);
        CustomPolicyDTO.setType("CustomRule");
        ExportThrottlePolicyDTO customPolicyExportedDTO = DtoFactory.createExportThrottlePolicyDTO(THROTTLE_POLICY_TYPE,
                CUSTOM_POLICY_SUBTYPE, APIM_VERSION, CustomPolicyDTO);

        StringBuilder contentBuilder = new StringBuilder();
        try (Stream<String> stream = java.nio.file.Files.lines(Paths.get(String.valueOf(exportedFileCustomPolicy)),
                StandardCharsets.UTF_8)) {
            stream.forEach(s -> contentBuilder.append(s).append("\n"));
        } catch (IOException e) {
            throw new APIManagerIntegrationTestException(
                    "Error in reading from extracted Throttle Policy file " + exportedFileCustomPolicy, e);
        }

        String exportedThrottlePolicyContent = contentBuilder.toString();
        JSONParser parser = new JSONParser();
        JSONObject exportedThrottlePolicyJson = (JSONObject) parser.parse(exportedThrottlePolicyContent);
        CustomPolicyDTO.setIsDeployed(false);
        CustomPolicyDTO.setType("CustomRule");
        customPolicyExportedDTO = DtoFactory.createExportThrottlePolicyDTO(THROTTLE_POLICY_TYPE, CUSTOM_POLICY_SUBTYPE,
                APIM_VERSION, CustomPolicyDTO);

        Gson gson = new Gson();
        ObjectMapper mapper = new ObjectMapper();
        ExportThrottlePolicyDTO expectedExportedPolicy = gson.fromJson(exportedThrottlePolicyJson.toJSONString(),
                ExportThrottlePolicyDTO.class);
        CustomRuleDTO customPolicy = mapper.convertValue(expectedExportedPolicy.getData(), CustomRuleDTO.class);
        expectedExportedPolicy.setData(customPolicy);
        Assert.assertEquals(expectedExportedPolicy, customPolicyExportedDTO);
    }

    @Test(groups = {"wso2.am"}, description = "Exported Sample Subscription ThrottlePolicy")
    public void testSubscriptionThrottlePolicyExport() throws Exception {

        exportedFileSubscriptionPolicy = exportArtifact(user.getUserName(), user.getPassword(), subscriptionPolicyName,
                subscriptionPolicyType);

        StringBuilder contentBuilder = new StringBuilder();
        try (Stream<String> stream = java.nio.file.Files.lines(
                Paths.get(String.valueOf(exportedFileSubscriptionPolicy)), StandardCharsets.UTF_8)) {
            stream.forEach(s -> contentBuilder.append(s).append("\n"));
        } catch (IOException e) {
            throw new APIManagerIntegrationTestException(
                    "Error in reading from extracted Throttle Policy file " + exportedFileSubscriptionPolicy, e);
        }

        String exportedThrottlePolicyContent = contentBuilder.toString();
        JSONParser parser = new JSONParser();
        JSONObject exportedThrottlePolicyJson = (JSONObject) parser.parse(exportedThrottlePolicyContent);
        SubscriptionPolicyDTO.setIsDeployed(false);
        SubscriptionPolicyDTO.setType("SubscriptionThrottlePolicy");
        ExportThrottlePolicyDTO subscriptionPolicyExportedDTO = DtoFactory.createExportThrottlePolicyDTO(
                THROTTLE_POLICY_TYPE, SUBSCRIPTION_POLICY_SUBTYPE, APIM_VERSION, SubscriptionPolicyDTO);

        Gson gson = new Gson();
        ObjectMapper mapper = new ObjectMapper();
        ExportThrottlePolicyDTO expectedExportedPolicy = gson.fromJson(exportedThrottlePolicyJson.toJSONString(),
                ExportThrottlePolicyDTO.class);
        SubscriptionThrottlePolicyDTO subPolicy = mapper.convertValue(expectedExportedPolicy.getData(),
                SubscriptionThrottlePolicyDTO.class);
        expectedExportedPolicy.setData(subPolicy);
        Assert.assertEquals(subscriptionPolicyExportedDTO, expectedExportedPolicy);
    }

    @Test(groups = {"wso2.am"}, description = "Exported Sample Application ThrottlePolicy")
    public void testApplicationThrottlePolicyExport() throws Exception {

        exportedFileApplicationPolicy = exportArtifact(user.getUserName(), user.getPassword(), applicationPolicyName,
                applicationPolicyType);

        StringBuilder contentBuilder = new StringBuilder();
        try (Stream<String> stream = java.nio.file.Files.lines(Paths.get(String.valueOf(exportedFileApplicationPolicy)),
                StandardCharsets.UTF_8)) {
            stream.forEach(s -> contentBuilder.append(s).append("\n"));
        } catch (IOException e) {
            throw new APIManagerIntegrationTestException(
                    "Error in reading from extracted Throttle Policy file " + exportedFileApplicationPolicy, e);
        }

        String exportedThrottlePolicyContent = contentBuilder.toString();
        JSONParser parser = new JSONParser();
        JSONObject exportedThrottlePolicyJson = (JSONObject) parser.parse(exportedThrottlePolicyContent);
        ApplicationPolicyDTO.setIsDeployed(false);
        ApplicationPolicyDTO.setType("ApplicationThrottlePolicy");
        ExportThrottlePolicyDTO applicationPolicyExportedDTO = DtoFactory.createExportThrottlePolicyDTO(
                THROTTLE_POLICY_TYPE, APPLICATION_POLICY_SUBTYPE, APIM_VERSION, ApplicationPolicyDTO);

        Gson gson = new Gson();
        ObjectMapper mapper = new ObjectMapper();
        ExportThrottlePolicyDTO expectedExportedPolicy = gson.fromJson(exportedThrottlePolicyJson.toJSONString(),
                ExportThrottlePolicyDTO.class);
        ApplicationThrottlePolicyDTO appPolicy = mapper.convertValue(expectedExportedPolicy.getData(),
                ApplicationThrottlePolicyDTO.class);
        expectedExportedPolicy.setData(appPolicy);
        Assert.assertEquals(applicationPolicyExportedDTO, expectedExportedPolicy);
    }

    @Test(groups = {"wso2.am"}, description = "Exported Sample Advanced ThrottlePolicy")
    public void testAdvancedThrottlePolicyExport() throws Exception {

        exportedFileAdvancedPolicy = exportArtifact(user.getUserName(), user.getPassword(), advancedPolicyName,
                advancedPolicyType);

        StringBuilder contentBuilder = new StringBuilder();
        try (Stream<String> stream = java.nio.file.Files.lines(Paths.get(String.valueOf(exportedFileAdvancedPolicy)),
                StandardCharsets.UTF_8)) {
            stream.forEach(s -> contentBuilder.append(s).append("\n"));
        } catch (IOException e) {
            throw new APIManagerIntegrationTestException(
                    "Error in reading from extracted Throttle Policy file " + exportedFileAdvancedPolicy, e);
        }

        String exportedThrottlePolicyContent = contentBuilder.toString();
        JSONParser parser = new JSONParser();
        JSONObject exportedThrottlePolicyJson = (JSONObject) parser.parse(exportedThrottlePolicyContent);
        AdvancedPolicyDTO.setIsDeployed(false);
        AdvancedPolicyDTO.setType("AdvancedThrottlePolicy");
        ExportThrottlePolicyDTO advancedPolicyExportedDTO = DtoFactory.createExportThrottlePolicyDTO(
                THROTTLE_POLICY_TYPE, ADVANCED_POLICY_SUBTYPE, APIM_VERSION, AdvancedPolicyDTO);

        Gson gson = new Gson();
        ObjectMapper mapper = new ObjectMapper();
        ExportThrottlePolicyDTO expectedExportedPolicy = gson.fromJson(exportedThrottlePolicyJson.toJSONString(),
                ExportThrottlePolicyDTO.class);
        AdvancedThrottlePolicyDTO advancedPolicy = mapper.convertValue(expectedExportedPolicy.getData(),
                AdvancedThrottlePolicyDTO.class);
        expectedExportedPolicy.setData(advancedPolicy);
        Assert.assertEquals(advancedPolicyExportedDTO, expectedExportedPolicy);

    }

    @Test(groups = {"wso2.am"}, description = "Importing an existing Custom Throttle Policy without update",
            dependsOnMethods = "testCustomThrottlePolicyExport")
    public void testCustomThrottlePolicyUpdateConflict() throws Exception {
        URL importRequest = new URL(importUrl + "?overwrite=" + "false");
        CloseableHttpResponse response = importArtifact(importRequest, exportedFileCustomPolicy, user.getUserName(),
                user.getPassword().toCharArray());
        Assert.assertEquals(response.getStatusLine().getStatusCode(), HttpStatus.SC_CONFLICT);
    }

    @Test(groups = {"wso2.am"}, description = "Importing an existing Subscription Throttle Policy without update",
            dependsOnMethods = "testSubscriptionThrottlePolicyExport")
    public void testSubscriptionThrottlePolicyUpdateConflict() throws Exception {
        URL importRequest = new URL(importUrl + "?overwrite=" + "false");
        CloseableHttpResponse response = importArtifact(importRequest, exportedFileSubscriptionPolicy,
                user.getUserName(), user.getPassword().toCharArray());
        Assert.assertEquals(response.getStatusLine().getStatusCode(), HttpStatus.SC_CONFLICT);
    }

    @Test(groups = {"wso2.am"}, description = "Importing an existing Application Throttle Policy without update",
            dependsOnMethods = "testApplicationThrottlePolicyExport")
    public void testApplicationThrottlePolicyUpdateConflict() throws Exception {
        URL importRequest = new URL(importUrl + "?overwrite=" + "false");
        CloseableHttpResponse response = importArtifact(importRequest, exportedFileApplicationPolicy,
                user.getUserName(), user.getPassword().toCharArray());
        Assert.assertEquals(response.getStatusLine().getStatusCode(), HttpStatus.SC_CONFLICT);
    }

    @Test(groups = {"wso2.am"}, description = "Importing an existing Advanced Throttle Policy without update",
            dependsOnMethods = "testAdvancedThrottlePolicyExport")
    public void testAdvancedThrottlePolicyUpdateConflict() throws Exception {
        URL importRequest = new URL(importUrl + "?overwrite=" + "false");
        CloseableHttpResponse response = importArtifact(importRequest, exportedFileAdvancedPolicy, user.getUserName(),
                user.getPassword().toCharArray());
        Assert.assertEquals(response.getStatusLine().getStatusCode(), HttpStatus.SC_CONFLICT);
    }

    @Test(groups = {"wso2.am" }, description = "Importing an existing Custom Throttle Policy with update",
            dependsOnMethods = "testCustomThrottlePolicyExport")
    public void testCustomThrottlePolicyUpdate() throws Exception {
        URL importRequest = new URL(importUrl + "?overwrite=" + "true");
        CloseableHttpResponse response = importArtifact(importRequest, exportedFileCustomPolicy, user.getUserName(),
                user.getPassword().toCharArray());
        Assert.assertEquals(response.getStatusLine().getStatusCode(), HttpStatus.SC_OK);
    }

    @Test(groups = {"wso2.am"}, description = "Importing an existing Subscription Throttle Policy with update",
            dependsOnMethods = "testSubscriptionThrottlePolicyExport")
    public void testSubscriptionThrottlePolicyUpdate() throws Exception {
        URL importRequest = new URL(importUrl + "?overwrite=" + "true");
        CloseableHttpResponse response = importArtifact(importRequest, exportedFileSubscriptionPolicy,
                user.getUserName(), user.getPassword().toCharArray());
        Assert.assertEquals(response.getStatusLine().getStatusCode(), HttpStatus.SC_OK);
    }

    @Test(groups = {"wso2.am"}, description = "Importing an existing Application Throttle Policy with update",
            dependsOnMethods = "testApplicationThrottlePolicyExport")
    public void testApplicationThrottlePolicyUpdate() throws Exception {
        URL importRequest = new URL(importUrl + "?overwrite=" + "true");
        CloseableHttpResponse response = importArtifact(importRequest, exportedFileApplicationPolicy,
                user.getUserName(), user.getPassword().toCharArray());
        Assert.assertEquals(response.getStatusLine().getStatusCode(), HttpStatus.SC_OK);
    }

    @Test(groups = {"wso2.am"}, description = "Importing an existing Advanced Throttle Policy with update",
            dependsOnMethods = "testAdvancedThrottlePolicyExport")
    public void testAdvancedThrottlePolicyUpdate() throws Exception {
        URL importRequest = new URL(importUrl + "?overwrite=" + "true");
        CloseableHttpResponse response = importArtifact(importRequest, exportedFileAdvancedPolicy, user.getUserName(),
                user.getPassword().toCharArray());
        Assert.assertEquals(response.getStatusLine().getStatusCode(), HttpStatus.SC_OK);
    }

    @Test(groups = {"wso2.am" }, description = "Importing a new Custom ThrottlePolicy",
            dependsOnMethods = "testCustomThrottlePolicyUpdate")
    public void testCustomThrottlePolicyNewImport() throws Exception {

        ApiResponse<Void> ApiResponse = restAPIAdmin.deleteCustomThrottlingPolicy(customPolicyId);
        Assert.assertEquals(ApiResponse.getStatusCode(), HttpStatus.SC_OK);
        URL importRequest = new URL(importUrl + "?overwrite=" + "false");
        CloseableHttpResponse response = importArtifact(importRequest, exportedFileCustomPolicy, user.getUserName(),
                user.getPassword().toCharArray());
        Assert.assertEquals(response.getStatusLine().getStatusCode(), HttpStatus.SC_CREATED);
    }

    @Test(groups = {"wso2.am"}, description = "Importing a new Subscription ThrottlePolicy",
            dependsOnMethods = "testSubscriptionThrottlePolicyUpdate")
    public void testSubscriptionThrottlePolicyNewImport() throws Exception {
        ApiResponse<Void> ApiResponse = restAPIAdmin.deleteSubscriptionThrottlingPolicy(subscriptionPolicyId);
        Assert.assertEquals(ApiResponse.getStatusCode(), HttpStatus.SC_OK);
        URL importRequest = new URL(importUrl + "?overwrite=" + "false");
        CloseableHttpResponse response = importArtifact(importRequest, exportedFileSubscriptionPolicy,
                user.getUserName(), user.getPassword().toCharArray());
        Assert.assertEquals(response.getStatusLine().getStatusCode(), HttpStatus.SC_CREATED);
    }

    @Test(groups = {"wso2.am"}, description = "Importing a new Application ThrottlePolicy",
            dependsOnMethods = "testApplicationThrottlePolicyUpdate")
    public void testApplicationThrottlePolicyNewImport() throws Exception {
        ApiResponse<Void> ApiResponse = restAPIAdmin.deleteApplicationThrottlingPolicy(applicationPolicyId);
        Assert.assertEquals(ApiResponse.getStatusCode(), HttpStatus.SC_OK);
        URL importRequest = new URL(importUrl + "?overwrite=" + "false");
        CloseableHttpResponse response = importArtifact(importRequest, exportedFileApplicationPolicy,
                user.getUserName(), user.getPassword().toCharArray());
        Assert.assertEquals(response.getStatusLine().getStatusCode(), HttpStatus.SC_CREATED);
    }

    @Test(groups = {"wso2.am"}, description = "Importing a new Advanced ThrottlePolicy",
            dependsOnMethods = "testAdvancedThrottlePolicyUpdate")
    public void testAdvancedThrottlePolicyNewImport() throws Exception {
        ApiResponse<Void> ApiResponse = restAPIAdmin.deleteAdvancedThrottlingPolicy(advancedPolicyId);
        Assert.assertEquals(ApiResponse.getStatusCode(), HttpStatus.SC_OK);
        URL importRequest = new URL(importUrl + "?overwrite=" + "false");
        CloseableHttpResponse response = importArtifact(importRequest, exportedFileAdvancedPolicy, user.getUserName(),
                user.getPassword().toCharArray());
        Assert.assertEquals(response.getStatusLine().getStatusCode(), HttpStatus.SC_CREATED);
    }

    /**
     * Setting Request Headers and executing export throttle policy request
     * @param exportRequest url with query parameters
     * @return Exported Throttle policy details response
     * @throws IOException throws if connection issues occurred
     * @throws URISyntaxException throws if URL is malformed
     */
    private CloseableHttpResponse exportThrottlePolicyRequest(URL exportRequest, String username, String password)
            throws IOException, URISyntaxException {
        CloseableHttpClient client = HTTPSClientUtils.getHttpsClient();
        HttpGet get = new HttpGet(exportRequest.toURI());
        get.addHeader(APIMIntegrationConstants.AUTHORIZATION_HEADER,
                "Basic " + encodeCredentials(username, password.toCharArray()));
        return client.execute(get);
    }

    /**
     * Exports the required throttling policy and writes to a temporary location
     * @param policyName Throttle Policy name
     * @param policyType Throttling Policy type
     * @return Exported Throttle policy file
     * @throws URISyntaxException throws if URI is malformed
     * @throws IOException throws if connection issues occurred
     */
    private File exportArtifact(String username, String password, String policyName, String policyType)
            throws URISyntaxException, IOException {
        //construct export Throttle Policy url
        URL exportRequest = new URL(exportUrl + "?name=" + policyName + "&type=" + policyType);
        File TempDir = Files.createTempDir();
        String fileName = policyType + "_" + policyName;
        File newExportedFile = new File(TempDir.getAbsolutePath() + File.separator + fileName + ".json");
        CloseableHttpResponse response = exportThrottlePolicyRequest(exportRequest, username, password);
        HttpEntity entity = response.getEntity();

        if (entity != null) {
            try (FileOutputStream outStream = new FileOutputStream(newExportedFile)) {
                entity.writeTo(outStream);
            }
        }
        assertEquals(response.getStatusLine().getStatusCode(), HttpStatus.SC_OK, "Response code is not as expected");
        Assert.assertTrue(newExportedFile.exists(), "File save was not successful");
        return newExportedFile;
    }

    /**
     * Execute POST request to send the exported throttle policy file
     * @param importUrl URL for the importing throttle policy with query params
     * @param fileName Exported Throttle Policy File
     * @return response of the importation
     * @throws IOException throws if connection issues occurred
     */
    private static CloseableHttpResponse importArtifact(URL importUrl, File fileName, String username, char[] password)
            throws IOException {
        try (CloseableHttpClient client = HttpClientBuilder.create().build()) {
            HttpPost request = new HttpPost(importUrl.toURI());
            FileBody fileBody = new FileBody(fileName);
            MultipartEntity multipartEntity = new MultipartEntity(HttpMultipartMode.STRICT);
            multipartEntity.addPart("file", fileBody);
            request.setEntity(multipartEntity);
            request.setHeader("Content-Type", multipartEntity.getContentType().getValue());
            request.setHeader("Accept", "application/json");
            request.setHeader(APIMIntegrationConstants.AUTHORIZATION_HEADER, "Basic " + encodeCredentials(username, password));
            return client.execute(request);
        } catch (URISyntaxException e) {
            String message="Invalid URI";
            throw new RuntimeException(message,e);
        }
    }

}
