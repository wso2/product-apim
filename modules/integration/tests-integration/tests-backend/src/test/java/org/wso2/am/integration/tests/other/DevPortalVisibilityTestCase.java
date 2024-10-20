/*
 * Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
package org.wso2.am.integration.tests.other;

import java.net.URL;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.xpath.XPathExpressionException;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIDTO.VisibilityEnum;
import org.wso2.am.integration.clients.publisher.api.v1.dto.DocumentDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.DocumentDTO.SourceTypeEnum;
import org.wso2.am.integration.clients.publisher.api.v1.dto.DocumentDTO.TypeEnum;
import org.wso2.am.integration.test.impl.RestAPIPublisherImpl;
import org.wso2.am.integration.test.impl.RestAPIStoreImpl;
import org.wso2.am.integration.test.utils.APIManagerIntegrationTestException;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.bean.APIRequest;
import org.wso2.am.integration.test.utils.http.HTTPSClientUtils;
import org.wso2.am.integration.tests.api.lifecycle.APIManagerLifecycleBaseTest;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;
import org.wso2.carbon.integration.common.admin.client.UserManagementClient;
import org.wso2.carbon.user.mgt.stub.UserAdminUserAdminException;
import org.wso2.carbon.utils.multitenancy.MultitenantUtils;

import com.google.gson.Gson;

import  org.testng.*;
/**
 * This test case verifies the functionality of dev portal visibility restrictions.
 */
public class DevPortalVisibilityTestCase extends APIManagerLifecycleBaseTest {

    private static final Log log = LogFactory.getLog(DevPortalVisibilityTestCase.class);
    
    private String contextUsername = "admin";
    private final String EMAIL_DOMAIN = "@gm.co";
    private final String AT = "@";
    private final String INTERNAL_CREATOR = "Internal/creator";
    private final String INTERNAL_PUBLISHER = "Internal/publisher";
    private final String INTERNAL_SUBSCRIBER = "Internal/subscriber";
    private final String USER_PASSWORD = "123123";
    
    private final String[] OLD_ROLE_LIST = { "Internal/publisher", "Internal/creator",
            "Internal/subscriber", "Internal/everyone", "role1" };
    
    
    private String DEV_USER_A = "dusera";
    private String DEV_USER_B = "duserb";
    private String DEV_USER_C = "duserc";
    private String PUB_SUB_USER = "pbsbusr";
    
    private String DEV_USER_A_ROLE = "rolex";
    private String DEV_USER_B_ROLE = "roley";
    
    private final String[] PUB_SUB_USER_ROLES = { "Internal/publisher", "Internal/creator",
            "Internal/subscriber", "Internal/everyone" };
    
    private final String EP_URL = "http://gdata.youtube.com/feeds/api/standardfeeds";
    
    UserManagementClient userManagementClient1;
    private RestAPIPublisherImpl pubSubUserPublisher;
    private RestAPIStoreImpl pubSubUserStore;
    private RestAPIStoreImpl devUser1;
    private RestAPIStoreImpl devUser2;
    private RestAPIStoreImpl devUser3;
    private String APINAME = "VisA";
    private String CONTEXT = "ctx9";
    private String VERSION = "1";
    private String apiId;

    private final String STORE_BASE_PATH = "api/am/devportal/v3/apis/";
    @DataProvider
    public static Object[][] userModeDataProvider() {
        return new Object[][]{
                new Object[]{TestUserMode.SUPER_TENANT_ADMIN},
                new Object[]{TestUserMode.TENANT_ADMIN},
                new Object[]{TestUserMode.SUPER_TENANT_USER_STORE_USER},
                new Object[]{TestUserMode.SUPER_TENANT_EMAIL_USER},
                new Object[]{TestUserMode.TENANT_EMAIL_USER},
        };
    }

    @Factory(dataProvider = "userModeDataProvider")
    public DevPortalVisibilityTestCase(TestUserMode userMode) {
        this.userMode = userMode;
    }

    @BeforeClass
    public void initTestCase() throws APIManagerIntegrationTestException, XPathExpressionException, RemoteException,
            UserAdminUserAdminException {
        super.init(userMode);
        
        publisherURLHttp = publisherUrls.getWebAppURLHttp();
        storeURLHttp = storeUrls.getWebAppURLHttp();
        contextUsername = keyManagerContext.getContextTenant().getContextUser().getUserName();
        userManagementClient1 = new UserManagementClient(keyManagerContext.getContextUrls().getBackEndUrl(),
                keyManagerContext.getContextTenant().getTenantAdmin().getUserName(),
                keyManagerContext.getContextTenant().getTenantAdmin().getPassword());
        
        if (TestUserMode.TENANT_EMAIL_USER.equals(userMode) || TestUserMode.SUPER_TENANT_EMAIL_USER.equals(userMode)) {
            DEV_USER_A = DEV_USER_A + EMAIL_DOMAIN + AT + keyManagerContext.getContextTenant().getContextUser()
                    .getUserDomain();
            DEV_USER_B = DEV_USER_B + EMAIL_DOMAIN + AT + keyManagerContext.getContextTenant().getContextUser()
                    .getUserDomain();
            DEV_USER_C =
                    DEV_USER_C + EMAIL_DOMAIN + AT + keyManagerContext.getContextTenant().getContextUser()
                            .getUserDomain();
            PUB_SUB_USER = PUB_SUB_USER + EMAIL_DOMAIN + AT + keyManagerContext.getContextTenant().getContextUser()
                    .getUserDomain();
        } else if (TestUserMode.SUPER_TENANT_USER_STORE_USER.equals(userMode)) {
            DEV_USER_A = APIMIntegrationConstants.SECONDARY_USER_STORE + "/" + DEV_USER_A;
            DEV_USER_B = APIMIntegrationConstants.SECONDARY_USER_STORE + "/" + DEV_USER_B;
            DEV_USER_C = APIMIntegrationConstants.SECONDARY_USER_STORE + "/" + DEV_USER_C;
            PUB_SUB_USER = APIMIntegrationConstants.SECONDARY_USER_STORE + "/" + PUB_SUB_USER;

            DEV_USER_A_ROLE = APIMIntegrationConstants.SECONDARY_USER_STORE + "/" + DEV_USER_A_ROLE;
            DEV_USER_B_ROLE = APIMIntegrationConstants.SECONDARY_USER_STORE + "/" + DEV_USER_B_ROLE;

        }
        
        userManagementClient1.addRole(DEV_USER_A_ROLE, new String[] {}, new String[] {});
        userManagementClient1.addRole(DEV_USER_B_ROLE, new String[] {}, new String[] {});
        
        //Dev portal user with rolex role
        userManagementClient1.addUser(DEV_USER_A, USER_PASSWORD, new String[] { INTERNAL_SUBSCRIBER, DEV_USER_A_ROLE },
                DEV_USER_A);
        //Dev portal user with roley role
        userManagementClient1.addUser(DEV_USER_B, USER_PASSWORD, new String[] { INTERNAL_SUBSCRIBER, DEV_USER_B_ROLE },
                DEV_USER_B);
        //Dev portal user with minimul dev portal login roles
        userManagementClient1.addUser(DEV_USER_C, USER_PASSWORD, new String[] { INTERNAL_SUBSCRIBER }, DEV_USER_C);
        
        //publisher user who has dev portal access
        userManagementClient1.addUser(PUB_SUB_USER, USER_PASSWORD,
                new String[] { INTERNAL_PUBLISHER, INTERNAL_SUBSCRIBER, INTERNAL_CREATOR }, PUB_SUB_USER);
        
        pubSubUserPublisher = new RestAPIPublisherImpl(PUB_SUB_USER, USER_PASSWORD,
                keyManagerContext.getContextTenant().getDomain(), publisherURLHttps);
        pubSubUserStore = new RestAPIStoreImpl(PUB_SUB_USER, USER_PASSWORD,
                keyManagerContext.getContextTenant().getDomain(), storeURLHttps);

        devUser1 = new RestAPIStoreImpl(DEV_USER_A, USER_PASSWORD, keyManagerContext.getContextTenant().getDomain(),
                storeURLHttps);

        devUser2 = new RestAPIStoreImpl(DEV_USER_B, USER_PASSWORD, keyManagerContext.getContextTenant().getDomain(),
                storeURLHttps);

        devUser3 = new RestAPIStoreImpl(DEV_USER_C, USER_PASSWORD, keyManagerContext.getContextTenant().getDomain(),
                storeURLHttps);

    }

    @Test(groups = "wso2.am", description = "This test case tests the retrieval of API which was added without dev"
            + " portal visibility")
    public void testAnonymousUserAccessDevPortalAPI() throws Exception {
        APIRequest apiRequest = new APIRequest(APINAME, CONTEXT, new URL(EP_URL));
        apiRequest.setVersion(VERSION);

        apiId = createAndPublishAPIUsingRest(apiRequest, pubSubUserPublisher, false);
        waitForAPIDeploymentSync(contextUsername, APINAME, VERSION, APIMIntegrationConstants.IS_API_EXISTS);
        HttpResponse response = pubSubUserPublisher.getAPI(apiId);
        Gson g = new Gson();
        APIDTO apidto = g.fromJson(response.getData(), APIDTO.class);
        Assert.assertEquals(apidto.getId(), apiId, "API Id is not equal");
        
        // check anonymous access
        Map<String, String> headers = new HashMap<String, String>();
        headers.put("X-WSO2-Tenant", MultitenantUtils.getTenantDomain(contextUsername));
        HttpResponse resp = HTTPSClientUtils.doGet(storeURLHttps + STORE_BASE_PATH + apiId, headers);
        log.info("Response " + resp.getData());
        Assert.assertEquals(resp.getResponseCode(), 200, "Public API cannot be accessed by anonymous user");
        
    }
    
    @Test(groups = "wso2.am", description = "This test case tests the retrieval of API which was added with "
            + "dev portal visibility.", dependsOnMethods = "testAnonymousUserAccessDevPortalAPI")
    public void testRestrictedDevPortalAPIAccess()
            throws Exception {
        HttpResponse response = pubSubUserPublisher.getAPI(apiId);
        Gson g = new Gson();
        APIDTO apidto = g.fromJson(response.getData(), APIDTO.class);
        apidto.setVisibility(VisibilityEnum.RESTRICTED);
        List<String> visibleRoles = new ArrayList<String>();
        visibleRoles.add(DEV_USER_A_ROLE);
        apidto.setVisibleRoles(visibleRoles);
        pubSubUserPublisher.updateAPI(apidto);
        waitForAPIDeploymentSync(contextUsername, APINAME, VERSION, APIMIntegrationConstants.IS_API_EXISTS);
        response = pubSubUserPublisher.getAPI(apiId);
        g = new Gson();
        apidto = g.fromJson(response.getData(), APIDTO.class);
        Assert.assertEquals(apidto.getVisibleRoles().get(0), DEV_USER_A_ROLE, "API visibility not updated.");

        // check anonymous access
        Map<String, String> headers = new HashMap<String, String>();
        headers.put("X-WSO2-Tenant", MultitenantUtils.getTenantDomain(contextUsername));
        HttpResponse resp = HTTPSClientUtils.doGet(storeURLHttps + STORE_BASE_PATH + apiId, headers);
        log.info("Response " + resp.getData());
        Assert.assertEquals(resp.getResponseCode(), 404, "Restricted API can be accessed by anonymous user");
        
        // dev portal user with role DEV_USER_A_ROLE
        org.wso2.am.integration.clients.store.api.v1.dto.APIDTO result = devUser1.getAPI(apiId);
        Assert.assertTrue(StringUtils.isNotEmpty(result.getId()),
                "Restricted API not visible for the user with role " + DEV_USER_A_ROLE);
        
        // dev portal user with role DEV_USER_B_ROLE. user should not be able to view api
        headers = new HashMap<String, String>();
        //headers.put("X-WSO2-Tenant", MultitenantUtils.getTenantDomain(contextUsername));
        headers.put("Authorization", "Bearer " + devUser2.getAccessToken());
        resp = HTTPSClientUtils.doGet(storeURLHttps + STORE_BASE_PATH + apiId, headers);
        log.info("Response " + resp.getData());
        Assert.assertEquals(resp.getResponseCode(), 404,
                "Restricted API can be accessed by user with " + DEV_USER_B_ROLE + " role");
        
        // publisher portal user without role DEV_USER_A_ROLE should be able to view the api in dev portal
        headers = new HashMap<String, String>();
        headers.put("Authorization", "Bearer " + pubSubUserStore.getAccessToken());
        resp = HTTPSClientUtils.doGet(storeURLHttps + STORE_BASE_PATH + apiId, headers);
        log.info("Response " + resp.getData());
        Assert.assertEquals(resp.getResponseCode(), 200, "Restricted API not visible for publisher user");
    }
    
    @Test(groups = "wso2.am", description = "This test case tests the accessibility of documents on dev portal for "
            + "restricted APIs", dependsOnMethods = "testRestrictedDevPortalAPIAccess")
    public void testRestrictedDevPortalDocumentAccess()
            throws Exception {
        
        DocumentDTO body = new DocumentDTO();
        body.setName("DocName");
        body.setSourceType(SourceTypeEnum.INLINE);
        body.setType(TypeEnum.HOWTO);
        body.setSummary("this is doc summary");
        body.setVisibility(org.wso2.am.integration.clients.publisher.api.v1.dto.DocumentDTO.VisibilityEnum.API_LEVEL);
        HttpResponse docResp = pubSubUserPublisher.addDocument(apiId, body);

        String docId = docResp.getData();
        Assert.assertEquals(docResp.getResponseCode(), 200, "Document creation faild for publisher");

        String docContent = "Sample content";
        pubSubUserPublisher.addContentDocument(apiId, docId, docContent);

        // check anonymous access
        Map<String, String> headers = new HashMap<String, String>();
        headers.put("X-WSO2-Tenant", MultitenantUtils.getTenantDomain(contextUsername));
        HttpResponse resp = HTTPSClientUtils
                .doGet(storeURLHttps + STORE_BASE_PATH + apiId + "/documents/" + docId, headers);
        log.info("Response " + resp.getData());
        Assert.assertEquals(resp.getResponseCode(), 404, "Restricted API doc can be accessed by anonymous user");
        resp = HTTPSClientUtils
                .doGet(storeURLHttps + STORE_BASE_PATH + apiId + "/documents/" + docId + "/content", headers);
        log.info("Response " + resp.getData());
        Assert.assertEquals(resp.getResponseCode(), 404, "Restricted API doc content can be accessed by anonymous user");
        
        // dev portal user with role DEV_USER_A_ROLE
        headers = new HashMap<String, String>();
        headers.put("Authorization", "Bearer " + devUser1.getAccessToken());
        resp = HTTPSClientUtils
                .doGet(storeURLHttps + STORE_BASE_PATH + apiId + "/documents/" + docId, headers);
        log.info("Response " + resp.getData());
        Assert.assertEquals(resp.getResponseCode(), 200,
                "Restricted API doc cannot be accessed by user with role " + DEV_USER_A_ROLE);
        resp = HTTPSClientUtils
                .doGet(storeURLHttps + STORE_BASE_PATH + apiId + "/documents/" + docId + "/content", headers);
        log.info("Response " + resp.getData());
        Assert.assertEquals(resp.getResponseCode(), 200,
                "Restricted API doc content cannot be accessed by user with role " + DEV_USER_A_ROLE);

        // dev portal user with role DEV_USER_B_ROLE. user should not be able to view api
        headers = new HashMap<String, String>();
        headers.put("Authorization", "Bearer " + devUser2.getAccessToken());
        resp = HTTPSClientUtils.doGet(storeURLHttps + STORE_BASE_PATH + apiId + "/documents/" + docId, headers);
        log.info("Response " + resp.getData());
        Assert.assertEquals(resp.getResponseCode(), 404,
                "Restricted API doc can be accessed by user with role " + DEV_USER_B_ROLE);
        resp = HTTPSClientUtils
                .doGet(storeURLHttps + STORE_BASE_PATH + apiId + "/documents/" + docId + "/content", headers);
        log.info("Response " + resp.getData());
        Assert.assertEquals(resp.getResponseCode(), 404,
                "Restricted API doc content can be accessed by user with role " + DEV_USER_B_ROLE);
        
        // publisher portal user without role DEV_USER_A_ROLE should be able to view the api in dev portal
        headers = new HashMap<String, String>();
        headers.put("Authorization", "Bearer " + pubSubUserStore.getAccessToken());
        resp = HTTPSClientUtils.doGet(storeURLHttps + STORE_BASE_PATH + apiId + "/documents/" + docId, headers);
        log.info("Response " + resp.getData());
        Assert.assertEquals(resp.getResponseCode(), 200,
                "Restricted API doc not visible for publisher user");
        resp = HTTPSClientUtils
                .doGet(storeURLHttps + STORE_BASE_PATH + apiId + "/documents/" + docId + "/content", headers);
        log.info("Response " + resp.getData());
        Assert.assertEquals(resp.getResponseCode(), 200,
                "Restricted API doc content not visible for publisher user");

    }
    
    @Test(groups = "wso2.am", description = "This test case tests the accessibility of openapi spec on dev portal for "
            + "restricted APIs", dependsOnMethods = "testRestrictedDevPortalAPIAccess")
    public void testRestrictedDevPortalOpenAPISpecAccess()
            throws Exception {
        // check anonymous access
        Map<String, String> headers = new HashMap<String, String>();
        headers.put("X-WSO2-Tenant", MultitenantUtils.getTenantDomain(contextUsername));
        HttpResponse resp = HTTPSClientUtils.doGet(storeURLHttps + STORE_BASE_PATH + apiId + "/swagger", headers);
        log.info("Response " + resp.getData());
        Assert.assertEquals(resp.getResponseCode(), 404, "Restricted API definition can be accessed by anonymous user");
        
        // dev portal user with role DEV_USER_A_ROLE
        headers = new HashMap<String, String>();
        headers.put("Authorization", "Bearer " + devUser1.getAccessToken());
        resp = HTTPSClientUtils
                .doGet(storeURLHttps + STORE_BASE_PATH + apiId + "/swagger", headers);
        log.info("Response " + resp.getData());
        Assert.assertEquals(resp.getResponseCode(), 200,
                "Restricted API definition cannot be accessed by user with role " + DEV_USER_A_ROLE);
        
        // dev portal user with role DEV_USER_B_ROLE. user should not be able to view api
        headers = new HashMap<String, String>();
        //headers.put("X-WSO2-Tenant", MultitenantUtils.getTenantDomain(contextUsername));
        headers.put("Authorization", "Bearer " + devUser2.getAccessToken());
        resp = HTTPSClientUtils.doGet(storeURLHttps + STORE_BASE_PATH + apiId + "/swagger", headers);
        log.info("Response " + resp.getData());
        Assert.assertEquals(resp.getResponseCode(), 404,
                "Restricted API definition can be accessed by user with " + DEV_USER_B_ROLE + " role");
        
        // publisher portal user without role DEV_USER_A_ROLE should be able to view the api in dev portal
        headers = new HashMap<String, String>();
        headers.put("Authorization", "Bearer " + pubSubUserStore.getAccessToken());
        resp = HTTPSClientUtils.doGet(storeURLHttps + STORE_BASE_PATH + apiId + "/swagger", headers);
        log.info("Response " + resp.getData());
        Assert.assertEquals(resp.getResponseCode(), 200, "Restricted API definition not visible for publisher user");

    }
    @AfterClass (alwaysRun = true)
    public void destroy() throws Exception {
        restAPIPublisher.deleteAPI(apiId);
        //Reverting back the roles of email users
        if (TestUserMode.SUPER_TENANT_EMAIL_USER.equals(userMode) || TestUserMode.TENANT_EMAIL_USER.equals(userMode)) {
            userManagementClient1.updateRolesOfUser(user.getUserNameWithoutDomain(), OLD_ROLE_LIST);
        }

        userManagementClient1.deleteUser(DEV_USER_A);
        userManagementClient1.deleteUser(DEV_USER_B);
        userManagementClient1.deleteUser(DEV_USER_C);
        userManagementClient1.deleteUser(PUB_SUB_USER);

        userManagementClient1.deleteRole(DEV_USER_A_ROLE);
        userManagementClient1.deleteRole(DEV_USER_B_ROLE);
    }
}
