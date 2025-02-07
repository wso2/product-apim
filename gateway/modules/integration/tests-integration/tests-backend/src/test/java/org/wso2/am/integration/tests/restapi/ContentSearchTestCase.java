/*
*  Copyright (c) 2019, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
*  WSO2 Inc. licenses this file to you under the Apache License,
*  Version 2.0 (the "License"); you may not use this file except
*  in compliance with the License.
*  You may obtain a copy of the License at
*
*    http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing,
* software distributed under the License is distributed on an
* "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
* KIND, either express or implied.  See the License for the
* specific language governing permissions and limitations
* under the License.
*/

package org.wso2.am.integration.tests.restapi;

import com.google.gson.Gson;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpStatus;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.DocumentDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.SearchResultListDTO;
import org.wso2.am.integration.test.impl.RestAPIPublisherImpl;
import org.wso2.am.integration.test.impl.RestAPIStoreImpl;
import org.wso2.am.integration.test.utils.APIManagerIntegrationTestException;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.bean.APILifeCycleAction;
import org.wso2.am.integration.test.utils.bean.APIRequest;
import org.wso2.am.integration.tests.api.lifecycle.APIManagerLifecycleBaseTest;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;
import org.wso2.carbon.integration.common.admin.client.UserManagementClient;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import static org.testng.Assert.assertEquals;

public class ContentSearchTestCase extends APIManagerLifecycleBaseTest {
    private Log log  = LogFactory.getLog(ContentSearchTestCase.class);
    private String endpointURL = "http://gdata.youtube.com/feeds/api/standardfeeds";
    private String version = "1.0.0";
    private int retries = 10; //because indexing needs time, we are retrying api calls at an interval of 3s
    private String contentSearchTestAPI = "contentSearchTestAPI";
    private String description = "Unified Search Feature";
    private String apiId;
    private String password = "wso2apim";
    private String user1 = "user1";
    private String user2 = "user2";
    private String role1 = "role1";
    private String role2 = "role2";
    private RestAPIPublisherImpl restAPIPublisherFirstUser;
    private RestAPIStoreImpl restAPIStoreFirstUser;
    private RestAPIPublisherImpl restAPIPublisherSecondUser;
    private RestAPIStoreImpl restAPIStoreSecondUser;
    private UserManagementClient userManagementClient1;
    private final String[] SEC_ROLE1_PERMISSIONS = { "/permission/admin/login", "/permission/admin/manage",
            "/permission/admin/configure", "/permission/admin/monitor" };
    private final String[] SEC_ROLE2_PERMISSIONS = { "/permission/admin/login", "/permission/admin/configure"};
    private final String[] SEC_OLD_ROLE_LIST = { "Internal/publisher", "Internal/creator",
            "Internal/subscriber", "Internal/everyone"};

    @Factory(dataProvider = "userModeDataProvider")
    public ContentSearchTestCase(TestUserMode userMode) {
        this.userMode = userMode;
    }

    @DataProvider
    public static Object[][] userModeDataProvider() {
        return new Object[][] {
                new Object[] { TestUserMode.SUPER_TENANT_ADMIN },
                new Object[] { TestUserMode.TENANT_ADMIN },
                new Object[] { TestUserMode.SUPER_TENANT_USER_STORE_USER },
                new Object[] { TestUserMode.SUPER_TENANT_EMAIL_USER },
                new Object[] { TestUserMode.TENANT_EMAIL_USER },
        };
    }

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {
        super.init(userMode);
        userManagementClient1 = new UserManagementClient(keyManagerContext.getContextUrls().getBackEndUrl(),
                keyManagerContext.getContextTenant().getTenantAdmin().getUserName(),
                keyManagerContext.getContextTenant().getTenantAdmin().getPassword());

        if (TestUserMode.SUPER_TENANT_USER_STORE_USER.equals(userMode)) {
            user1 = APIMIntegrationConstants.SECONDARY_USER_STORE + "/" + user1;
            user2 = APIMIntegrationConstants.SECONDARY_USER_STORE + "/" + user2;

            role1 = APIMIntegrationConstants.SECONDARY_USER_STORE + "/" + role1;
            role2 = APIMIntegrationConstants.SECONDARY_USER_STORE + "/" + role2;

            userManagementClient1.addRole(role1, new String[]{}, SEC_ROLE1_PERMISSIONS);
            userManagementClient1.addRole(role2, new String[]{}, SEC_ROLE2_PERMISSIONS);

            String[] newRoleList = { "Internal/publisher", "Internal/creator", "Internal/subscriber",
                    "Internal/everyone", role1};
            userManagementClient1.updateRolesOfUser(user.getUserNameWithoutDomain(), newRoleList);
        }

        APIRequest apiRequest = createAPIRequest(contentSearchTestAPI, contentSearchTestAPI, endpointURL, version,
                user.getUserName(), description);

        apiId = createAndPublishAPIUsingRest(apiRequest, restAPIPublisher, false);

    }

    @Test(groups = { "wso2.am" }, description = "Test basic content Search")
    public void testBasicContentSearch() throws Exception {
        log.info("Basic Content Search");

        //check in publisher
        for (int i = 0; i <= retries; i++) {
            SearchResultListDTO searchResultListDTO = restAPIPublisher.searchAPIs(description);
            if (searchResultListDTO.getCount() >= 1) {
                Assert.assertTrue(true);
                break;
            } else {
                if (i == retries) {
                    Assert.fail("Basic content search in publisher failed. Received response : " + searchResultListDTO
                            .getCount());
                } else {
                    log.warn("Basic content search in publisher failed. Received response : " + searchResultListDTO
                            .getCount() + " Retrying...");
                    Thread.sleep(3000);
                }
            }
        }

        for (int i = 0; i <= retries; i++) {
            //search term : UnifiedSearchFeature, created api has this in description filed
            org.wso2.am.integration.clients.store.api.v1.dto.SearchResultListDTO searchResultListDTO = restAPIStore
                    .searchAPIs(description);
            if (searchResultListDTO.getCount() >= 1) { // API and API Definitions
                Assert.assertTrue(true);
                break;
            } else {
                if (i == retries) {
                    Assert.fail("Basic content search in store failed. Received response : " + searchResultListDTO
                            .getCount());
                } else {
                    log.warn("Basic content search in store failed. Received response : " + searchResultListDTO
                            .getCount() + " Retrying...");
                    Thread.sleep(3000);
                }
            }
        }

        //change status to create and check whether it is accessible from store
        restAPIPublisher.changeAPILifeCycleStatus(apiId, APILifeCycleAction.DEMOTE_TO_CREATE.getAction());
        for (int i = 0; i <= retries; i++) {
            org.wso2.am.integration.clients.store.api.v1.dto.SearchResultListDTO searchResultListDTO = restAPIStore
                    .searchAPIs(description);
            if (searchResultListDTO.getCount() == 0) {
                Assert.assertTrue(true);
                break;
            } else {
                if (i == retries) {
                    Assert.fail("Basic content search in store failed. 0 results expected. Received response : "
                            + searchResultListDTO.getCount());
                } else {
                    log.warn("Basic content search in store failed. 0 results expected. Received response : "
                            + searchResultListDTO.getCount() + " Retrying...");
                    Thread.sleep(3000);
                }
            }
        }
    }

    @Test(groups = {
            "wso2.am" }, description = "Test document content Search", dependsOnMethods = "testContentSearchWithStoreVisibility")
    public void testDocumentContentSearch() throws Exception {
        log.info("Document Content Search");
        String documentName = "Test-Document";
        String documentContent = "This is a sample API to test unified search feature - github4156";
        DocumentDTO documentDTO = new DocumentDTO();
        documentDTO.setName(documentName);
        documentDTO.setSourceType(DocumentDTO.SourceTypeEnum.INLINE);
        documentDTO.setType(DocumentDTO.TypeEnum.HOWTO);
        documentDTO.setSummary("document summary");
        documentDTO.setVisibility(DocumentDTO.VisibilityEnum.API_LEVEL);
        HttpResponse documentHttpResponse = restAPIPublisher.addDocument(apiId, documentDTO);
        assertEquals(documentHttpResponse.getResponseCode(), HttpStatus.SC_OK,
                "Error while add documentation to API");
        String documentId = documentHttpResponse.getData();
        restAPIPublisher.addContentDocument(apiId, documentId, documentContent);

        //check in publisher
        for (int i = 0; i <= retries; i++) {
            SearchResultListDTO searchResultListDTO = restAPIPublisher.searchAPIs("github4156");
            if (searchResultListDTO.getCount() == 1) {
                Assert.assertTrue(true);
                break;
            } else {
                if (i == retries) {
                    Assert.fail("Document content search in publisher failed. Received response : " + searchResultListDTO
                            .getCount());
                } else {
                    log.warn("Document content search in publisher failed. Received response : " + searchResultListDTO
                            .getCount() + " Retrying...");
                    Thread.sleep(Math.min(3000L * (i + 1), 30000L));
                }
            }
        }

        //check in store
        for (int i = 0; i <= retries; i++) {
            //search term : UnifiedSearchFeature, created api has this in description filed
            org.wso2.am.integration.clients.store.api.v1.dto.SearchResultListDTO searchResultListDTO = restAPIStore
                    .searchAPIs("github4156");
            if (searchResultListDTO.getCount() == 1) {
                Assert.assertTrue(true);
                break;
            } else {
                if (i == retries) {
                    Assert.fail("Document content search in store failed. Received response : " + searchResultListDTO
                            .getCount());
                } else {
                    log.warn("Document content search in store failed. Received response : " + searchResultListDTO
                            .getCount() + " Retrying...");
                    Thread.sleep(Math.min(3000L * (i + 1), 30000L));
                }
            }
        }
    }

    @Test(groups = {
            "wso2.am" }, description = "Test content Search with access control", dependsOnMethods = "testBasicContentSearch")
    public void testContentSearchWithAccessControl() throws Exception {
        // Set API publisher access control to role1
        HttpResponse httpResponse = restAPIPublisher.getAPI(apiId);
        APIDTO apiDto = new Gson().fromJson(httpResponse.getData(), APIDTO.class);

        apiDto.setAccessControl(APIDTO.AccessControlEnum.RESTRICTED);
        List<String> roles = new ArrayList<>();
        roles.add(role1);
        apiDto.setAccessControlRoles(roles);

        restAPIPublisher.updateAPI(apiDto);
        restAPIPublisher.changeAPILifeCycleStatus(apiId, APILifeCycleAction.PUBLISH.getAction());

        // Add users for test
        userManagementClient1
                .addUser(user1, password, new String[] { role1, "Internal/publisher"}, user1);
        userManagementClient1
                .addUser(user2, password, new String[] { role2, "Internal/publisher"}, user2);

        //Login to API Publisher adn Store with CarbonSuper normal user1
        restAPIPublisherFirstUser = new RestAPIPublisherImpl(user1, password, user.getUserDomain(), publisherURLHttps);
        restAPIPublisherSecondUser = new RestAPIPublisherImpl(user2, password, user.getUserDomain(), publisherURLHttps);

        //check with user1
        for (int i = 0; i <= retries; i++) {
            SearchResultListDTO searchResultListDTO = restAPIPublisherFirstUser.searchAPIs(description);
            if (searchResultListDTO.getCount() == 2) { // API and API Definition
                Assert.assertTrue(true);
                break;
            } else {
                if (i == retries) {
                    Assert.fail("Content search with access control failed. 2 result expected. Received response : "
                            + searchResultListDTO.getCount());
                } else {
                    log.warn("Content search with access control failed. 2 results expected. Received response : "
                            + searchResultListDTO.getCount() + " Retrying...");
                    Thread.sleep(3000);
                }
            }
        }

        //check with user2 who doesn't have permissions for api
        for (int i = 0; i <= retries; i++) {
            SearchResultListDTO searchResultListDTO = restAPIPublisherSecondUser.searchAPIs(description);
            if (searchResultListDTO.getCount() == 0) {
                Assert.assertTrue(true);
                break;
            } else {
                if (i == retries) {
                    Assert.fail("Content search with access control failed. 0 result expected. Received response : "
                            + searchResultListDTO.getCount());
                } else {
                    log.warn("Content search with access control failed. 0 results expected. Received response : "
                            + searchResultListDTO.getCount() + " Retrying...");
                    Thread.sleep(3000);
                }
            }
        }

        //clear apis, roles and users


    }

    @Test(groups = {
            "wso2.am" }, description = "Test content Search with store visibility", dependsOnMethods = "testContentSearchWithAccessControl")
    public void testContentSearchWithStoreVisibility() throws Exception {

        // Set store visibility to role1
        HttpResponse httpResponse = restAPIPublisher.getAPI(apiId);
        APIDTO apiDto = new Gson().fromJson(httpResponse.getData(), APIDTO.class);
        apiDto.setAccessControl(APIDTO.AccessControlEnum.NONE);

        List<String> roles = new ArrayList<>();
        roles.add(role1);
        apiDto.setVisibility(APIDTO.VisibilityEnum.RESTRICTED);
        apiDto.setVisibleRoles(roles);

        restAPIPublisher.updateAPI(apiDto);

        // Update user for tests
        userManagementClient1.updateRolesOfUser(user1, new String[] { role1, "Internal/subscriber" });
        userManagementClient1.updateRolesOfUser(user2, new String[] { role2, "Internal/subscriber" });

        restAPIStoreFirstUser = new RestAPIStoreImpl(user1, password, user.getUserDomain(), storeURLHttps);
        restAPIStoreSecondUser = new RestAPIStoreImpl(user2, password, user.getUserDomain(), storeURLHttps);

        //check with user1
        for (int i = 0; i <= retries; i++) {
            org.wso2.am.integration.clients.store.api.v1.dto.SearchResultListDTO searchResultListDTO = restAPIStoreFirstUser
                    .searchAPIs(description);
            if (searchResultListDTO.getCount() == 2) { // API and API Definition
                Assert.assertTrue(true);
                break;
            } else {
                if (i == retries) {
                    Assert.fail("Content search with visibility failed. 2 result expected. Received response : "
                            + searchResultListDTO.getCount());
                } else {
                    log.warn("Content search with visibility failed. 2 results expected. Received response : "
                            + searchResultListDTO.getCount() + " Retrying...");
                    Thread.sleep(5000);
                }
            }
        }

        //check with user2 who doesn't have permissions for api
        for (int i = 0; i <= retries; i++) {
            org.wso2.am.integration.clients.store.api.v1.dto.SearchResultListDTO searchResultListDTO = restAPIStoreSecondUser
                    .searchAPIs(description);
            if (searchResultListDTO.getCount() == 0) {
                Assert.assertTrue(true);
                break;
            } else {
                if (i == retries) {
                    Assert.fail("Content search with visibility failed. 0 result expected. Received response : "
                            + searchResultListDTO.getCount());
                } else {
                    log.warn("Content search with visibility failed. 0 results expected. Received response : "
                            + searchResultListDTO.getCount() + " Retrying...");
                    Thread.sleep(5000);
                }
            }
        }
    }

    @AfterClass(alwaysRun = true)
    public void destroyAPIs() throws Exception {
        undeployAndDeleteAPIRevisionsUsingRest(apiId, restAPIPublisher);
        restAPIPublisher.deleteAPI(apiId);
        if (TestUserMode.SUPER_TENANT_USER_STORE_USER.equals(userMode)) {
            userManagementClient1.deleteRole(role1);
            userManagementClient1.deleteRole(role2);
            userManagementClient1.updateRolesOfUser(user.getUserNameWithoutDomain(), SEC_OLD_ROLE_LIST);
        }
        userManagementClient1.deleteUser(user1);
        userManagementClient1.deleteUser(user2);
        super.cleanUp();
    }

    private APIRequest createAPIRequest(String name, String context, String url, String version, String provider,
            String description) throws MalformedURLException, APIManagerIntegrationTestException {
        APIRequest apiRequest = new APIRequest(name, context, new URL(url));
        apiRequest.setVersion(version);
        apiRequest.setDescription(description);
        apiRequest.setProvider(provider);

        return apiRequest;
    }


}

