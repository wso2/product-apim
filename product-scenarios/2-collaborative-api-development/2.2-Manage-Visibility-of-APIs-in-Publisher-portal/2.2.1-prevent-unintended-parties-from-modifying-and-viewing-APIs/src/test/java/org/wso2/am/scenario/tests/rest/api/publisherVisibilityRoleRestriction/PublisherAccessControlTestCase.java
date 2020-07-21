/*
 * Copyright (c) 2019, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wso2.am.scenario.tests.rest.api.publisherVisibilityRoleRestriction;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIDTO;
import org.wso2.am.integration.test.impl.RestAPIPublisherImpl;
import org.wso2.am.integration.test.utils.bean.APICreationRequestBean;
import org.wso2.am.scenario.test.common.ScenarioTestBase;
import org.wso2.am.scenario.test.common.ScenarioTestConstants;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import java.net.URL;

import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

public class PublisherAccessControlTestCase extends ScenarioTestBase {

    private String secondUser;
    private String apiName;
    private String apiContext;
    private String apiVersion = "1.0.0";
    private String tierCollection = "Gold,Bronze";
    private String backendEndPoint = "http://ws.cdyne.com/phoneverify/phoneverify.asmx";
    private int count = 0;
    private static String apiID;

    private static final Log log = LogFactory.getLog(PublisherAccessControlTestCase.class);
    private static final String ADMIN_USERNAME = "admin";
    private static final String ADMIN_PW = "admin";
    private static final String TENANT_ADMIN_USERNAME = "admin@wso2.com";
    private static final String TENANT_ADMIN_PW = "admin";
    private static final String API_CREATOR_PUBLISHER_USERNAME = "micheal";
    private static final String API_CREATOR_PUBLISHER_PW = "Micheal#123";
    private static final String API_SUBSCRIBER_USERNAME = "andrew";
    private static final String API_SUBSCRIBER_PW = "Andrew#123";



    @Factory(dataProvider = "userModeDataProvider")
    public PublisherAccessControlTestCase(TestUserMode userMode) {
        this.userMode = userMode;
    }

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {
        if (this.userMode.equals(TestUserMode.SUPER_TENANT_USER)) {
            createUserWithPublisherAndCreatorRole(API_CREATOR_PUBLISHER_USERNAME, API_CREATOR_PUBLISHER_PW,
                                                  ADMIN_USERNAME, ADMIN_PW);
            createUserWithSubscriberRole(API_SUBSCRIBER_USERNAME, API_SUBSCRIBER_PW, ADMIN_USERNAME, ADMIN_PW);
            secondUser = "adminUser1";
        }

        if (this.userMode.equals(TestUserMode.TENANT_USER)) {
            // create user in wso2.com tenant
            addTenantAndActivate(ScenarioTestConstants.TENANT_WSO2, ADMIN_USERNAME, ADMIN_PW);
            if (isActivated(ScenarioTestConstants.TENANT_WSO2)) {
//                Add and activate wso2.com tenant
                createUserWithPublisherAndCreatorRole(API_CREATOR_PUBLISHER_USERNAME, API_CREATOR_PUBLISHER_PW,
                                                      TENANT_ADMIN_USERNAME, TENANT_ADMIN_PW);
                createUserWithSubscriberRole(API_SUBSCRIBER_USERNAME, API_SUBSCRIBER_PW, TENANT_ADMIN_USERNAME,
                                             TENANT_ADMIN_PW);
            }
            secondUser = "adminUser1"+ "@" + ScenarioTestConstants.TENANT_WSO2;
        }
        super.init(userMode);
    }

    @Test(description = "2.2.1.1")
    public void testVisibilityOfAPIsInPublisherRestrictedByRoles() throws Exception {

        apiName = "API_Restricted_" + count;
        apiContext = "/Restricted_"+ count;
        count++;

        APICreationRequestBean apiCreationRequestBean = new APICreationRequestBean(apiName, apiContext, apiVersion,"admin", new URL(backendEndPoint));
        apiCreationRequestBean.setTiersCollection(tierCollection);
        apiCreationRequestBean.setRoles(ScenarioTestConstants.PUBLISHER_ROLE);
        apiCreationRequestBean.setVisibility(APIDTO.VisibilityEnum.RESTRICTED.getValue());
        APIDTO apiDto = restAPIPublisher.addAPI(apiCreationRequestBean);

        apiID = apiDto.getId();
        assertNotNull(apiDto.getId(), "API creation fails");

        if (this.userMode.equals(TestUserMode.SUPER_TENANT_USER)) {
            createUser(secondUser, "password123$", new String[] {ScenarioTestConstants.PUBLISHER_ROLE},
                       ADMIN_USERNAME, ADMIN_PW);
        }
        if (this.userMode.equals(TestUserMode.TENANT_USER)) {
            createUser(secondUser, "password123$", new String[]{ScenarioTestConstants.CREATOR_ROLE},
                       TENANT_ADMIN_USERNAME, TENANT_ADMIN_PW);
        }

        RestAPIPublisherImpl restAPIPublisherNew;
        restAPIPublisherNew = new RestAPIPublisherImpl(secondUser, "password123$", publisherContext.getContextTenant().getDomain(), publisherURLHttps);

        HttpResponse apiResponseGetAPI = restAPIPublisherNew.getAPI(apiDto.getId());
        verifyResponse(apiResponseGetAPI);
        assertTrue(apiResponseGetAPI.getData().contains(apiName), apiName + " is not visible in publisher");

        restAPIPublisher.deleteAPI(apiDto.getId());

        if (this.userMode.equals(TestUserMode.SUPER_TENANT_USER)) {
            deleteUser(secondUser, ADMIN_USERNAME, ADMIN_PW);
        }
        if (this.userMode.equals(TestUserMode.TENANT_USER)) {
            deleteUser(secondUser, TENANT_ADMIN_USERNAME, TENANT_ADMIN_PW);
        }
    }

    @Test(description = "2.2.1.2")
    public void testVisibilityOfAPIsPublisherRestrictedByMultipleRoles() throws Exception {

        apiName = "API_multiple_" + count;
        apiContext = "/multiple_"+ count;
        count++;

        String multipleRoles = ScenarioTestConstants.PUBLISHER_ROLE+","+ScenarioTestConstants.CREATOR_ROLE;

        APICreationRequestBean apiCreationRequestBean = new APICreationRequestBean(apiName, apiContext, apiVersion,"admin", new URL(backendEndPoint));
        apiCreationRequestBean.setTiersCollection(tierCollection);
        apiCreationRequestBean.setVisibility(APIDTO.VisibilityEnum.RESTRICTED.getValue());
        apiCreationRequestBean.setRoles(multipleRoles);

        APIDTO apiDto = restAPIPublisher.addAPI(apiCreationRequestBean);
//      apiDto.setVisibility(APIDTO.VisibilityEnum.RESTRICTED);

        apiID = apiDto.getId();
        assertNotNull(apiDto.getId(), "API creation fails");

        if (this.userMode.equals(TestUserMode.SUPER_TENANT_USER)) {
            createUser(secondUser, "password123$", new String[] {ScenarioTestConstants.PUBLISHER_ROLE},
                       ADMIN_USERNAME, ADMIN_PW);
        }
        if (this.userMode.equals(TestUserMode.TENANT_USER)) {
            createUser(secondUser, "password123$", new String[]{ScenarioTestConstants.CREATOR_ROLE},
                       TENANT_ADMIN_USERNAME, TENANT_ADMIN_PW);
        }

        RestAPIPublisherImpl restAPIPublisherNew;
        restAPIPublisherNew = new RestAPIPublisherImpl(secondUser, "password123$", publisherContext.getContextTenant().getDomain(), publisherURLHttps);

        HttpResponse apiResponseGetAPI = restAPIPublisherNew.getAPI(apiDto.getId());
        verifyResponse(apiResponseGetAPI);
        assertTrue(apiResponseGetAPI.getData().contains(apiName), apiName + " is not visible in publisher");

        restAPIPublisher.deleteAPIByID(apiDto.getId());

        if (this.userMode.equals(TestUserMode.SUPER_TENANT_USER)) {
            deleteUser(secondUser, ADMIN_USERNAME, ADMIN_PW);
        }
        if (this.userMode.equals(TestUserMode.TENANT_USER)) {
            deleteUser(secondUser, TENANT_ADMIN_USERNAME, TENANT_ADMIN_PW);
        }
    }

    @Test(description = "2.2.1.3")
    public void testVisibilityInPublisherRestrictedByRolesWithSpace() throws Exception {

        apiName = "API"+ count;
        apiContext = "/verify"+ count;
        String multipleRoles = ScenarioTestConstants.PUBLISHER_ROLE+", "+ScenarioTestConstants.CREATOR_ROLE;

        APICreationRequestBean apiCreationRequestBean = new APICreationRequestBean(apiName, apiContext, apiVersion,"admin", new URL(backendEndPoint));
        apiCreationRequestBean.setTiersCollection(tierCollection);
        apiCreationRequestBean.setVisibility(APIDTO.VisibilityEnum.RESTRICTED.getValue());
        apiCreationRequestBean.setRoles(multipleRoles);

        APIDTO apiDto = restAPIPublisher.addAPI(apiCreationRequestBean);

        apiID = apiDto.getId();
        assertNotNull(apiDto.getId(), "API creation fails");

        if (this.userMode.equals(TestUserMode.SUPER_TENANT_USER)) {
            createUser(secondUser, "password123$", new String[] {ScenarioTestConstants.PUBLISHER_ROLE},
                       ADMIN_USERNAME, ADMIN_PW);
        }
        if (this.userMode.equals(TestUserMode.TENANT_USER)) {
            createUser(secondUser, "password123$", new String[]{ScenarioTestConstants.CREATOR_ROLE},
                       TENANT_ADMIN_USERNAME, TENANT_ADMIN_PW);
        }

        RestAPIPublisherImpl restAPIPublisherNew;
        restAPIPublisherNew = new RestAPIPublisherImpl(secondUser, "password123$", publisherContext.getContextTenant().getDomain(), publisherURLHttps);

        HttpResponse apiResponseGetAPI = restAPIPublisherNew.getAPI(apiDto.getId());
        verifyResponse(apiResponseGetAPI);
        assertTrue(apiResponseGetAPI.getData().contains(apiName), apiName + " is not visible in publisher");

        restAPIPublisher.deleteAPIByID(apiDto.getId());

        if (this.userMode.equals(TestUserMode.SUPER_TENANT_USER)) {
            deleteUser(secondUser, ADMIN_USERNAME, ADMIN_PW);
        }
        if (this.userMode.equals(TestUserMode.TENANT_USER)) {
            deleteUser(secondUser, TENANT_ADMIN_USERNAME, TENANT_ADMIN_PW);
        }
    }

    @Test(description = "2.2.1.4")
    public void testCreateAPIsInPublisherRestrictedByRoles() throws Exception {

        apiName = "API-X_" +count;
        apiContext = "/check_"+count;

        APICreationRequestBean apiCreationRequestBean = new APICreationRequestBean(apiName, apiContext, apiVersion,"admin", new URL(backendEndPoint));
        apiCreationRequestBean.setTiersCollection(tierCollection);
        apiCreationRequestBean.setRoles(ScenarioTestConstants.PUBLISHER_ROLE);
        apiCreationRequestBean.setVisibility(APIDTO.VisibilityEnum.RESTRICTED.getValue());
        APIDTO apiDto = restAPIPublisher.addAPI(apiCreationRequestBean);

        apiID = apiDto.getId();
        assertNotNull(apiDto.getId(), "API creation fails");

        HttpResponse apiResponseGetAPI = restAPIPublisher.getAPI(apiDto.getId());
        verifyResponse(apiResponseGetAPI);
        assertTrue(apiResponseGetAPI.getData().contains(apiName), apiName + " is not visible in publisher");

        restAPIPublisher.deleteAPIByID(apiID);
    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
        if (this.userMode.equals(TestUserMode.SUPER_TENANT_USER)) {
            deleteUser(API_CREATOR_PUBLISHER_USERNAME, ADMIN_USERNAME, ADMIN_PW);
            deleteUser(API_SUBSCRIBER_USERNAME, ADMIN_USERNAME, ADMIN_PW);
        }
        if (this.userMode.equals(TestUserMode.TENANT_USER)) {
            deleteUser(API_CREATOR_PUBLISHER_USERNAME, TENANT_ADMIN_USERNAME, TENANT_ADMIN_PW);
            deleteUser(API_SUBSCRIBER_USERNAME, TENANT_ADMIN_USERNAME, TENANT_ADMIN_PW);
            deactivateAndDeleteTenant(ScenarioTestConstants.TENANT_WSO2);
        }
    }

    @DataProvider
    public static Object[][] userModeDataProvider() throws Exception {
        setup();
        // return the relevant parameters for each test run
        // 1) Super tenant API creator
        // 2) Tenant API creator
        return new Object[][]{
            new Object[]{TestUserMode.SUPER_TENANT_USER},
            // new Object[]{TestUserMode.TENANT_USER},
        };
    }
}