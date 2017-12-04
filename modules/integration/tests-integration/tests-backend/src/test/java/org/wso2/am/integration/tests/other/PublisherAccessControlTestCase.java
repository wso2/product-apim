/*
 * Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.am.integration.test.utils.APIManagerIntegrationTestException;
import org.wso2.am.integration.test.utils.base.APIMIntegrationBaseTest;
import org.wso2.am.integration.test.utils.bean.APIRequest;
import org.wso2.am.integration.test.utils.clients.APIPublisherRestClient;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;
import org.wso2.carbon.integration.common.admin.client.UserManagementClient;
import org.wso2.carbon.user.mgt.stub.UserAdminUserAdminException;

import javax.xml.xpath.XPathExpressionException;
import java.net.MalformedURLException;
import java.net.URL;
import java.rmi.RemoteException;

/**
 * This test case verifies the functionality of publisher access control restriction.
 */
public class PublisherAccessControlTestCase extends APIMIntegrationBaseTest {
    private APIPublisherRestClient apiPublisher;
    private String contextUsername = "admin";
    private String contextUserPassword = "admin";
    private final String FIRST_USER = "publisher_user";
    private final String SECOND_USER = "publisher_user2";
    private final String FIRST_ROLE = "publisher_role1";
    private final String USER_PASSWORD = "123123";
    private final String EP_URL = "http://gdata.youtube.com/feeds/api/standardfeeds";

    @BeforeClass
    public void initTestCase() throws APIManagerIntegrationTestException, XPathExpressionException, RemoteException,
            UserAdminUserAdminException {
        super.init();
        publisherURLHttp = publisherUrls.getWebAppURLHttp();
        apiPublisher = new APIPublisherRestClient(publisherURLHttp);
        contextUsername = keyManagerContext.getContextTenant().getContextUser().getUserName();
        contextUserPassword = keyManagerContext.getContextTenant().getContextUser().getPassword();
        UserManagementClient userManagementClient1 = new UserManagementClient(
                keyManagerContext.getContextUrls().getBackEndUrl(), contextUsername, contextUserPassword);
        String PERMISSION_API_CREATE = "/permission/admin/manage/api/create";
        String PERMISSION_LOGIN = "/permission/admin/login";
        userManagementClient1
                .addRole(FIRST_ROLE, new String[] {}, new String[] { PERMISSION_LOGIN, PERMISSION_API_CREATE });
        String PERMISSION_API_PUBLISH = "/permission/admin/manage/api/publish";
        String SECOND_ROLE = "publisher_role2";
        userManagementClient1
                .addRole(SECOND_ROLE, new String[] {}, new String[] { PERMISSION_LOGIN, PERMISSION_API_PUBLISH });
        userManagementClient1.addUser(FIRST_USER, USER_PASSWORD, new String[] { FIRST_ROLE }, FIRST_USER);
        userManagementClient1.addUser(SECOND_USER, USER_PASSWORD, new String[] { SECOND_ROLE }, SECOND_USER);
    }

    @Test(groups = "wso2.am", description = "This test case tests the retrieval of API which was added with a access "
            + "control restriction.")
    public void testAPIAdditionWithAccessControlRestriction() throws Exception {
        apiPublisher.login(contextUsername, contextUserPassword);
        String SAMPLE_API = "PublisherAccessControl";
        APIRequest apiRequest = new APIRequest(SAMPLE_API, SAMPLE_API, new URL(EP_URL));
        apiRequest.setVersion("1.0.0");
        apiRequest.setProvider(contextUsername);
        apiRequest.setAccessControl("restricted");
        apiRequest.setAccessControlRoles(FIRST_ROLE);
        apiPublisher.addAPI(apiRequest);
        HttpResponse response = apiPublisher.getAPI(SAMPLE_API, contextUsername);
        Assert.assertTrue(response.getData().contains(FIRST_ROLE), "API was not visible to the APIM admin user");
        apiPublisher.logout();

        apiPublisher.login(FIRST_USER, USER_PASSWORD);
        response = apiPublisher.getAPI(SAMPLE_API, contextUsername);
        Assert.assertTrue(response.getData().contains(FIRST_ROLE),
                "API was not visible to the creators who have the relevant access control roles of the API");
        apiPublisher.logout();

        apiPublisher.login(SECOND_USER, USER_PASSWORD);
        response = apiPublisher.getAPI(SAMPLE_API, contextUsername);
        Assert.assertFalse(response.getData().contains(FIRST_ROLE),
                "API was visible to the creators who do not have the relevant access control roles of the API");

    }

    @Test(groups = "wso2.am", description = "This test case tests the retrieval of API which was added without "
            + "access control restriction.")
    public void testAPIAdditionWithoutAccessControlRestriction()
            throws APIManagerIntegrationTestException, MalformedURLException {
        apiPublisher.login(contextUsername, contextUserPassword);
        String SAMPLE_API2 = "PublisherAccessControl2";
        APIRequest apiRequest = new APIRequest(SAMPLE_API2, SAMPLE_API2, new URL(EP_URL));
        apiRequest.setVersion("1.0.0");
        apiRequest.setProvider(contextUsername);
        apiPublisher.addAPI(apiRequest);
        HttpResponse response = apiPublisher.getAPI(SAMPLE_API2, contextUsername);
        Assert.assertTrue(response.getData().contains("\"provider\" : \"admin\""),
                "API is not visible to APIM admin" + " without access control restriction");
        apiPublisher.logout();

        apiPublisher.login(FIRST_USER, USER_PASSWORD);
        response = apiPublisher.getAPI(SAMPLE_API2, contextUsername);
        Assert.assertTrue(response.getData().contains("\"provider\" : \"admin\""),
                "API is not visible to creator" + " without access control restriction");
        apiPublisher.logout();

        apiPublisher.login(SECOND_USER, USER_PASSWORD);
        response = apiPublisher.getAPI(SAMPLE_API2, contextUsername);
        Assert.assertTrue(response.getData().contains("\"provider\" : \"admin\""),
                "API is not visible to creator" + " without access control restriction");
    }
}
