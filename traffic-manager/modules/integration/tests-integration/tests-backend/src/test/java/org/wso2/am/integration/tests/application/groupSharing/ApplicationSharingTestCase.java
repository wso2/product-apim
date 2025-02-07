/*
 * Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *    WSO2 Inc. licenses this file to you under the Apache License,
 *    Version 2.0 (the "License"); you may not use this file except
 *    in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing,
 *   software distributed under the License is distributed on an
 *   "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *   KIND, either express or implied.  See the License for the
 *   specific language governing permissions and limitations
 *   under the License.
 */

package org.wso2.am.integration.tests.application.groupSharing;

import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;
import org.wso2.am.integration.clients.store.api.ApiException;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationInfoDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyGenerateRequestDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.APIKeyDTO;
import org.wso2.am.integration.test.impl.RestAPIStoreImpl;
import org.wso2.am.integration.test.utils.UserManagementUtils;
import org.wso2.am.integration.test.utils.base.APIMIntegrationBaseTest;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import java.util.ArrayList;
import java.util.List;

import static org.testng.Assert.assertFalse;
import static org.wso2.am.integration.test.utils.base.APIMIntegrationConstants.SUPER_TENANT_DOMAIN;

public class ApplicationSharingTestCase extends APIMIntegrationBaseTest {

    private static final String APPLICATION_NAME = "TestApplication";
    private static final String SHARED_APPLICATION_NAME = "SharedApplication";
    private static final String USER_ONE = "userOne";
    private static final String USER_TWO = "userTwo";
    private static final String PASSWORD = "test@123";
    private static final String FIRST_NAME = "John";
    private static final String LAST_NAME = "Doe";
    private static final String EMAIL = "john.doe@test.com";
    private static final String ORGANIZATION = "Test";
    private String userOneApplicationId;
    private String userTwoApplicationId;
    private String userOneSharedApplicationId;
    private RestAPIStoreImpl restAPIStoreClientUser1;
    private RestAPIStoreImpl restAPIStoreClientUser2;
    private static final Log log = LogFactory.getLog(ApplicationSharingTestCase.class);
    List<String> groups = new ArrayList<>();

    @Factory(dataProvider = "userModeDataProvider")
    public ApplicationSharingTestCase(TestUserMode userMode) {

        this.userMode = userMode;
    }

    @DataProvider
    public static Object[][] userModeDataProvider() {
        return new Object[][]{
                new Object[]{TestUserMode.SUPER_TENANT_ADMIN}
        };
    }

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {

        super.init(userMode);
        groups.add(ORGANIZATION);
        createUsersAndApplications();
    }

    @Test(groups = "wso2.am", description = "Remove user one's application and check if user two's application also " +
            "is getting deleted")
    public void testUserTwoApplicationRemoval() throws Exception {

        restAPIStoreClientUser1.removeApplicationById(userOneApplicationId);
        List<ApplicationInfoDTO> user2AllAppsList = restAPIStoreClientUser2.getAllApps().getList();
        boolean isUserTwoAppDeleted =
                user2AllAppsList.stream()
                        .noneMatch(applicationInfoDTO -> userTwoApplicationId.equals(applicationInfoDTO.getApplicationId()));
        assertFalse(isUserTwoAppDeleted, "Deletion of User One's application has deleted User Two's " +
                "application too");
    }

    @Test(groups = "wso2.am", description = "Edit application by application owner",
            dependsOnMethods = "testUserTwoApplicationRemoval")
    public void testEditApplicationByApplicationOwner() throws Exception {
        HttpResponse serviceResponse = restAPIStoreClientUser1.updateApplicationByID(userOneSharedApplicationId,
                SHARED_APPLICATION_NAME, "This app has been edited",
                APIMIntegrationConstants.APPLICATION_TIER.TEN_PER_MIN, ApplicationDTO.TokenTypeEnum.JWT, groups);
        Assert.assertEquals(serviceResponse.getResponseCode(), HttpStatus.SC_OK,
                "Response code is not as expected");
        ApplicationDTO applicationDTO = restAPIStoreClientUser1.getApplicationById(userOneSharedApplicationId);
        Assert.assertEquals(applicationDTO.getDescription(), "This app has been edited");
        Assert.assertEquals(applicationDTO.getThrottlingPolicy(), APIMIntegrationConstants.APPLICATION_TIER.TEN_PER_MIN);
    }

    @Test(groups = "wso2.am", description = "Edit application by application by user in application group",
            dependsOnMethods = "testEditApplicationByApplicationOwner")
    public void testEditApplicationByUserInApplicationGroup() throws ApiException {
        //View application by a user in the application group
        List<ApplicationInfoDTO> user2AllAppsList = restAPIStoreClientUser2.getAllApps().getList();
        ApplicationDTO applicationDTO = restAPIStoreClientUser2.getApplicationById(userOneSharedApplicationId);
        Assert.assertNotNull(applicationDTO);
        Assert.assertEquals(applicationDTO.getName(), SHARED_APPLICATION_NAME);

        //Edit application by a user in application group
        HttpResponse serviceResponse = restAPIStoreClientUser2.updateApplicationByID(userOneSharedApplicationId,
                APPLICATION_NAME, "This app has been edited by user1",
                APIMIntegrationConstants.APPLICATION_TIER.TEN_PER_MIN, ApplicationDTO.TokenTypeEnum.JWT, groups);
        Assert.assertEquals(serviceResponse.getResponseCode(), HttpStatus.SC_FORBIDDEN);
    }

    @Test(groups = "wso2.am", description = "Generate API key from user 1 and make sure that user 2 can revoke the key")
    public void testAPIKeyRevocationBySharedUser()
            throws ApiException {

        //Check for application availability
        List<ApplicationInfoDTO> user1AllAppsList = restAPIStoreClientUser1.getAllApps().getList();
        ApplicationDTO applicationDTO = restAPIStoreClientUser1.getApplicationById(userOneSharedApplicationId);
        Assert.assertNotNull(applicationDTO);
        Assert.assertEquals(applicationDTO.getName(), SHARED_APPLICATION_NAME);

        //Generate api key by user 1
        APIKeyDTO key = restAPIStoreClientUser1.generateAPIKeys(userOneSharedApplicationId,
                ApplicationKeyGenerateRequestDTO.KeyTypeEnum.PRODUCTION.toString(), -1, null, null);
        //Revoke api key by user 2
        restAPIStoreClientUser2.revokeAPIKey(userOneSharedApplicationId, key.getApikey());
    }

    private void createUsersAndApplications() throws Exception {
        //signup of user one
        UserManagementUtils.signupUser(USER_ONE, PASSWORD, FIRST_NAME, ORGANIZATION);
        //signup of user two
        UserManagementUtils.signupUser(USER_TWO, PASSWORD, FIRST_NAME, ORGANIZATION);

        restAPIStoreClientUser1 = new RestAPIStoreImpl(USER_ONE, PASSWORD, SUPER_TENANT_DOMAIN, storeURLHttps);
        restAPIStoreClientUser2 = new RestAPIStoreImpl(USER_TWO, PASSWORD, SUPER_TENANT_DOMAIN, storeURLHttps);

        // Create Application for user one
        HttpResponse appCreationResponse1 = restAPIStoreClientUser1.createApplication(APPLICATION_NAME,
                "App created by user1", APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED,
                ApplicationDTO.TokenTypeEnum.JWT);
        userOneApplicationId = appCreationResponse1.getData();

        // Create Application for user two
        HttpResponse appCreationResponse2 = restAPIStoreClientUser2.createApplication(APPLICATION_NAME,
                "App created by user2", APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED,
                ApplicationDTO.TokenTypeEnum.JWT);
        userTwoApplicationId = appCreationResponse2.getData();

        HttpResponse appCreationResponse3 = restAPIStoreClientUser1.createApplicationWithOrganization(SHARED_APPLICATION_NAME,
                "App created by user1", APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED,
                ApplicationDTO.TokenTypeEnum.JWT, groups);
        userOneSharedApplicationId = appCreationResponse3.getData();
    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
        restAPIStoreClientUser2.removeApplicationById(userTwoApplicationId);
    }
}
