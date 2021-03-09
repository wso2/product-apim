
/*
 * Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
package org.wso2.am.scenario.tests.rest.api.restrictedVisibility;

import org.json.JSONObject;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.am.integration.clients.store.api.v1.dto.TagListDTO;
import org.wso2.am.integration.test.impl.RestAPIPublisherImpl;
import org.wso2.am.integration.test.impl.RestAPIStoreImpl;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.bean.APICreationRequestBean;
import org.wso2.am.integration.test.utils.bean.APILifeCycleState;
import org.wso2.am.integration.test.utils.bean.APILifeCycleStateRequest;
import org.wso2.am.integration.test.utils.bean.APIResourceBean;
import org.wso2.am.scenario.test.common.APIConstants;
import org.wso2.am.scenario.test.common.APIPublisherRestClient;
import org.wso2.am.scenario.test.common.APIRequest;
import org.wso2.am.scenario.test.common.APIStoreRestClient;
import org.wso2.am.scenario.test.common.ScenarioTestBase;
import org.wso2.am.scenario.test.common.ScenarioTestConstants;
import org.wso2.carbon.apimgt.api.model.APIIdentifier;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import static org.testng.Assert.assertTrue;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertEquals;

public class RESTApiVisibilityRestrictedByRolesNegativeTestCase extends ScenarioTestBase {

    private APIPublisherRestClient apiPublisher;
    private String apiName;
    private String apiContext;
    private String apiVersion = "1.0.0";

    private String apiName2 = "RestrictedAPITagTest";
    private String apiContext2 = "/tag-test";
    private String apiVersion2 = "1.0.0";

    private String apiVisibility = "restricted";
    private String apiResource = "/find";
    private String tierCollection = "Gold,Bronze";
    private String backendEndPoint = "http://ws.cdyne.com/phoneverify/phoneverify.asmx";

    private final String ADMIN_LOGIN_USERNAME = "admin";
    private final String ADMIN_PASSWORD = "admin";
    private final String SUBSCRIBER_USERNAME = "subscriberUser2";
    private final String SUBSCRIBER_PASSWORD = "password@123";
    private static final String ADMIN_USERNAME = "admin";
    private static final String ADMIN_PW = "admin";
    private static final String API_CREATOR_PUBLISHER_USERNAME = "micheal";
    private static final String API_CREATOR_PUBLISHER_PW = "Micheal#123";
    private static final String API_SUBSCRIBER_USERNAME = "andrew";
    private static final String API_SUBSCRIBER_PW = "Andrew#123";
    private final String SUPER_TENANT_USER_USERNAME = "APIVisiSuperTenantUserNeg";
    private final String SUPER_TENANT_USER_PASSWORD = "APIVisiSuperTenantUserNeg";
    private final String TENANT_SUBSCRIBER_USERNAME = "APIVisiTenantSubscriberNeg";
    private final String TENANT_SUBSCRIBER_PASSWORD = "APIVisiTenantSubscriberNeg";
    private final String VISIBILITY_TYPE = "store";
    private final String CREATOR_ROLE = "Creator";
    private final String SUBSCRIBER_ROLE = "Subscriber";

    private final String API_DEVELOPER_USER = "api-dev-user1";
    private final String API_DEVELOPER_USER_PWD = "api-dev-user1";

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {

        String[] permission = new String[]{"/permission/admin/login",
                "/permission/admin/manage/api/subscribe"};

        createUserWithPublisherAndCreatorRole(API_CREATOR_PUBLISHER_USERNAME, API_CREATOR_PUBLISHER_PW,
                ADMIN_USERNAME, ADMIN_PW);
        createUserWithSubscriberRole(API_SUBSCRIBER_USERNAME, API_SUBSCRIBER_PW, ADMIN_USERNAME, ADMIN_PW);
        createRole(ADMIN_LOGIN_USERNAME, ADMIN_PASSWORD, SUBSCRIBER_ROLE, permission);
        createRole(ADMIN_LOGIN_USERNAME, ADMIN_PASSWORD, CREATOR_ROLE, permission);
        createUserWithSubscriberRole(SUBSCRIBER_USERNAME, SUBSCRIBER_PASSWORD, ADMIN_LOGIN_USERNAME, ADMIN_PASSWORD);
        createUserWithPublisherAndCreatorRole(API_DEVELOPER_USER, API_DEVELOPER_USER_PWD, ADMIN_LOGIN_USERNAME, ADMIN_PASSWORD);

        setup();
        super.init();
    }

    @Test(description = "1.5.2.3")
    public void testVisibilityOfAPISLoginUserWithIncompatibleRole() throws Exception {
        apiName = "PhoneVerificationOptionalAdd";
        apiContext = "/phoneverify";

        APICreationRequestBean apiCreationRequestBean = new APICreationRequestBean(apiName, apiContext, apiVersion,
                API_DEVELOPER_USER, new URL(backendEndPoint));
        apiCreationRequestBean.setRoles(SUBSCRIBER_ROLE);
        apiCreationRequestBean.setVisibility(apiVisibility);

        String apiId = createAPI(apiCreationRequestBean);
        getAPI(apiId, apiName);
        publishAPI(apiId);

        RestAPIStoreImpl restAPIStoreNew = new RestAPIStoreImpl(
                SUBSCRIBER_USERNAME, SUBSCRIBER_PASSWORD, storeContext.getContextTenant().getDomain(), storeURLHttps);

        restAPIStoreNew.isAvailableInDevPortal(apiId);

        restAPIPublisher.deleteAPI(apiId);
    }

    @Test(description = "1.5.2.6")
    public void testTagCloudContainsRestrictedTag() throws Exception {
        final String restrictedTag = "restricted-tag";

        APICreationRequestBean apiCreationRequestBean = new APICreationRequestBean(apiName2, apiContext2, apiVersion,
                API_DEVELOPER_USER, new URL(backendEndPoint));
        apiCreationRequestBean.setRoles(ADMIN_LOGIN_USERNAME);
        apiCreationRequestBean.setVisibility(apiVisibility);
        apiCreationRequestBean.setTiersCollection(tierCollection);
        apiCreationRequestBean.setTags(restrictedTag);

        //define resources
        List<APIResourceBean> resList = new ArrayList<APIResourceBean>();
        APIResourceBean res = new APIResourceBean("GET",
                APIMIntegrationConstants.ResourceAuthTypes.NONE.getAuthType(),
                APIMIntegrationConstants.RESOURCE_TIER.TWENTYK_PER_MIN, "/find");
        resList.add(res);
        apiCreationRequestBean.setResourceBeanList(resList);

        String apiId = createAPI(apiCreationRequestBean);
        getAPI(apiId, apiName2);
        publishAPI(apiId);

        RestAPIStoreImpl restAPIStoreNew = new RestAPIStoreImpl(
                SUBSCRIBER_USERNAME, SUBSCRIBER_PASSWORD, storeContext.getContextTenant().getDomain(), storeURLHttps);
        TagListDTO allTags = restAPIStoreNew.getAllTags();
        assertFalse(allTags.getList().contains(restrictedTag),
                "Restricted tag was"
                        + " returned in the tag cloud but it is not expected to be returned.");
        restAPIPublisher.deleteAPI(apiId);
    }

    @Test(description = "1.5.2.10")
    public void testCreateAPIWithInvalidRoleInStoreVisibility() throws Exception {
        RestAPIPublisherImpl restAPIPublisherImpl = new RestAPIPublisherImpl(API_DEVELOPER_USER, API_DEVELOPER_USER_PWD,
                publisherContext.getContextTenant().getDomain(), baseUrl);
        restAPIPublisherImpl.validateRoles(CREATOR_ROLE);
    }

    @Test(description = "1.5.2.11")
    public void testAPIVisibilityRestrictedByRoleAndTenantType() throws Exception {
        apiName = "APIVisibility_ByRoleAndTenant";
        apiContext = "APIVisibility_ByRoleAndTenant";
        createUser(SUPER_TENANT_USER_USERNAME, SUPER_TENANT_USER_PASSWORD, new String[]{
                ScenarioTestConstants.CREATOR_ROLE, ScenarioTestConstants.PUBLISHER_ROLE,
                ScenarioTestConstants.SUBSCRIBER_ROLE}, ADMIN_LOGIN_USERNAME, ADMIN_PASSWORD);
        //Add and activate wso2.com tenant
        addTenantAndActivate(ScenarioTestConstants.TENANT_WSO2, ADMIN_LOGIN_USERNAME, ADMIN_PASSWORD);
        // create user in wso2.com tenant
        createUserWithSubscriberRole(TENANT_SUBSCRIBER_USERNAME, TENANT_SUBSCRIBER_PASSWORD,
                ADMIN_LOGIN_USERNAME + "@" + ScenarioTestConstants.TENANT_WSO2, ADMIN_PASSWORD);

        APICreationRequestBean apiCreationRequestBean = new APICreationRequestBean(apiName,
                apiContext, apiVersion, API_DEVELOPER_USER, new URL(backendEndPoint));
        apiCreationRequestBean.setRoles(SUBSCRIBER_ROLE);
        apiCreationRequestBean.setVisibility(apiVisibility);
        apiCreationRequestBean.setTiersCollection(tierCollection);

        String apiId = createAPI(apiCreationRequestBean);
        getAPI(apiId, apiName);
        publishAPI(apiId);

        RestAPIStoreImpl restAPIStoreSuperTenant = new RestAPIStoreImpl(SUPER_TENANT_USER_USERNAME,
                SUPER_TENANT_USER_PASSWORD, storeContext.getContextTenant().getDomain(), storeURLHttps);

        restAPIStoreSuperTenant.isAvailableInDevPortal(apiId);

        RestAPIStoreImpl restAPIStoreTenant = new RestAPIStoreImpl(TENANT_SUBSCRIBER_USERNAME,
                TENANT_SUBSCRIBER_PASSWORD, ScenarioTestConstants.TENANT_WSO2, storeURLHttps);

        restAPIStoreTenant.isAvailableInDevPortal(apiId);

        restAPIPublisher.deleteAPI(apiId);
    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
        deleteUser(SUBSCRIBER_USERNAME, ADMIN_LOGIN_USERNAME, ADMIN_PASSWORD);
        deleteUser(API_DEVELOPER_USER, ADMIN_LOGIN_USERNAME, ADMIN_PASSWORD);
        deleteUser(API_CREATOR_PUBLISHER_USERNAME, ADMIN_USERNAME, ADMIN_PW);
        deleteUser(API_SUBSCRIBER_USERNAME, ADMIN_USERNAME, ADMIN_PW);
        deleteUser(SUPER_TENANT_USER_USERNAME, ADMIN_LOGIN_USERNAME, ADMIN_PASSWORD);
        deleteRole(SUBSCRIBER_ROLE, ADMIN_LOGIN_USERNAME, ADMIN_PASSWORD);
    }

    private void getAPI(String id, String apiName) throws Exception {
        HttpResponse apiResponseGetAPI = restAPIPublisher.getAPI(id);
        verifyResponse(apiResponseGetAPI);
        JSONObject responseJson = new JSONObject(apiResponseGetAPI.getData());
        assertEquals(responseJson.get("name").toString(), apiName, apiName + " is not updated");
    }
}
