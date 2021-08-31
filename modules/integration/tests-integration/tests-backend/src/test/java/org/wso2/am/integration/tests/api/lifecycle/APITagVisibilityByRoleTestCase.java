/*
* Copyright (c) 2016, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
* WSO2 Inc. licenses this file to you under the Apache License,
* Version 2.0 (the "License"); you may not use this file except
* in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing,
* software distributed under the License is distributed on an
* "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
* KIND, either express or implied. See the License for the
* specific language governing permissions and limitations
* under the License.
*
*/
package org.wso2.am.integration.tests.api.lifecycle;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.TagDTO;
import org.wso2.am.integration.test.impl.RestAPIStoreImpl;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.bean.APICreationRequestBean;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.integration.common.admin.client.UserManagementClient;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Test class is used to test the API tags visibility for anonymous and authorised users when API role based visibility
 * is used.
 */
public class APITagVisibilityByRoleTestCase extends APIManagerLifecycleBaseTest {

    private final Log log = LogFactory.getLog(APITagVisibilityByRoleTestCase.class);
    private String NAME_PUBLIC_API = "APITagVisibilityPublicAPI";
    private String CONTEXT_PUBLIC_API = "contextPublicAPI";
    private String NAME_RESTRICTED_API = "APITagVisibilityRestrictedAPI";
    private String CONTEXT_RESTRICTED_API = "contextRestrictedAPI";
    private String TAGS_PUBLIC_API = "APITagVisibilityPublicTag";
    private String TAGS_RESTRICTED_API = "APITagVisibilityRestrictedTag";
    private String DESCRIPTION = "This is test API create by APIM APITagVisibilityByRoleTestCase";
    private String API_VERSION_1_0_0 = "1.0.0";
    private String ALLOWED_USER = "AllowedUser";
    private String ALLOWED_USER_PASS = "password@123";
    private String ROLE = "APITagVisibleRole";
    private final String API_END_POINT_POSTFIX_URL = "jaxrs_basic/services/customers/customerservice/";
    private String[] PERMISSIONS = { "/permission/admin/login", "/permission/admin/manage/api/subscribe" };
    private UserManagementClient userManagementClient;
    private String endpointUrl;
    private String providerName;
    private String publicApiId;
    private String restrictedApiId;
    private RestAPIStoreImpl apiStoreClientAllowedUser;
    private RestAPIStoreImpl anonymousRestAPIImpl;

    @Factory(dataProvider = "userModeDataProvider")
    public APITagVisibilityByRoleTestCase(TestUserMode userMode) {
        this.userMode = userMode;
    }

    @DataProvider
    public static Object[][] userModeDataProvider() {
        return new Object[][]{
                new Object[]{TestUserMode.SUPER_TENANT_ADMIN},
                new Object[]{TestUserMode.TENANT_ADMIN},
                new Object[] { TestUserMode.SUPER_TENANT_USER_STORE_USER },
                new Object[] { TestUserMode.SUPER_TENANT_EMAIL_USER },
                new Object[] { TestUserMode.TENANT_EMAIL_USER },
        };
    }

    @BeforeClass(alwaysRun = true)
    public void initialize() throws Exception {
        super.init(userMode);
        endpointUrl = backEndServerUrl.getWebAppURLHttp() + API_END_POINT_POSTFIX_URL;
        providerName = user.getUserName();
        userManagementClient = new UserManagementClient(keyManagerContext.getContextUrls().getBackEndUrl(),
                keyManagerContext.getContextTenant().getTenantAdmin().getUserName(),
                keyManagerContext.getContextTenant().getTenantAdmin().getPassword());

        if (TestUserMode.SUPER_TENANT_USER_STORE_USER.equals(userMode)) {
            ROLE = APIMIntegrationConstants.SECONDARY_USER_STORE + "/" + ROLE;
            ALLOWED_USER =
                    APIMIntegrationConstants.SECONDARY_USER_STORE + "/" + ALLOWED_USER;
        }

        //add a role for which API tags should be visible
        userManagementClient.addRole(ROLE, null, PERMISSIONS);
        // add new user with the above role
        userManagementClient.addUser(ALLOWED_USER, ALLOWED_USER_PASS, new String[]{ROLE}, null);
        apiStoreClientAllowedUser = new RestAPIStoreImpl(ALLOWED_USER, ALLOWED_USER_PASS,
                keyManagerContext.getContextTenant().getDomain(), storeURLHttps);
        //get a rest api client for anonymous user
        anonymousRestAPIImpl = getRestAPIStoreForAnonymousUser(keyManagerContext.getContextTenant().getDomain());
    }

    @Test(groups = { "wso2.am" }, description = "Create and publish two apis with public and role based visibility")
    public void testAPICreationWithVisibility() throws Exception {
        //Create request for API with public visibility.
        APICreationRequestBean apiCreationReqBeanPublicAPI = new APICreationRequestBean(
                NAME_PUBLIC_API, CONTEXT_PUBLIC_API, API_VERSION_1_0_0, providerName, new URL(endpointUrl));
        apiCreationReqBeanPublicAPI.setTags(TAGS_PUBLIC_API);
        apiCreationReqBeanPublicAPI.setDescription(DESCRIPTION);
        apiCreationReqBeanPublicAPI.setVersion(API_VERSION_1_0_0);
        apiCreationReqBeanPublicAPI.setProvider(providerName);
        //add API
        APIDTO apiDtoPublicAPI = restAPIPublisher.addAPI(apiCreationReqBeanPublicAPI);
        publicApiId = apiDtoPublicAPI.getId();
        //create Revision and Deploy to Gateway
        createAPIRevisionAndDeployUsingRest(publicApiId, restAPIPublisher);
        //publish API
        publishAPI(publicApiId, restAPIPublisher, false);
        waitForAPIDeployment();

        //create request for Restricted API
        APICreationRequestBean apiCreationRequestBeanRestrictedAPI = new APICreationRequestBean(
                NAME_RESTRICTED_API, CONTEXT_RESTRICTED_API, API_VERSION_1_0_0, providerName, new URL(endpointUrl));
        apiCreationRequestBeanRestrictedAPI.setTags(TAGS_RESTRICTED_API);
        apiCreationRequestBeanRestrictedAPI.setDescription(DESCRIPTION);
        apiCreationRequestBeanRestrictedAPI.setVisibility(APIDTO.VisibilityEnum.RESTRICTED.getValue());
        apiCreationRequestBeanRestrictedAPI.setRoles(ROLE);
        //add and publish Restricted API
        APIDTO apiDtoRestrictedAPI = restAPIPublisher.addAPI(apiCreationRequestBeanRestrictedAPI);
        restrictedApiId = apiDtoRestrictedAPI.getId();
        //create Revision and Deploy to Gateway
        createAPIRevisionAndDeployUsingRest(restrictedApiId, restAPIPublisher);
        //publish API
        publishAPI(restrictedApiId, restAPIPublisher, false);
        waitForAPIDeployment();
    }

    @Test(groups = { "wso2.am" }, description = "Test the API tag visibility as a anonymous user",
            dependsOnMethods = "testAPICreationWithVisibility")
    public void testAPITagVisibilityAnonymousUser() throws Exception {
        List<TagDTO> tagDto = anonymousRestAPIImpl.getAllTags().getList();
        List<String> tagList = new ArrayList<>();
        tagDto.forEach((tempDto) -> {
            tagList.add(tempDto.getValue());
        });
        Assert.assertTrue(tagList.contains(TAGS_PUBLIC_API),
                "Public visibility tag is not available for anonymous user");
        Assert.assertFalse(tagList.contains(TAGS_RESTRICTED_API),
                "Restricted visibility tag is available for anonymous user");
    }

    @Test(groups = {"wso2.am"}, description = "Test the API tag visibility as a authorised user",
            dependsOnMethods = "testAPITagVisibilityAnonymousUser")
    public void testAPITagVisibilityAuthorisedUser() throws Exception {
        List<TagDTO> tagDtoList = apiStoreClientAllowedUser.getAllTags().getList();
        List<String> tagList = new ArrayList<>();
        for (TagDTO tempTagDTO : tagDtoList) {
            tagList.add(tempTagDTO.getValue());
        }
        Assert.assertTrue(tagList.contains(TAGS_PUBLIC_API),
                "Public visibility tag is not available for authorised user");
        int retry = 10;
        if (!tagList.contains(TAGS_RESTRICTED_API)) {
            Thread.sleep(3000);
            for (int i = 0; i < retry; i++) {
                tagDtoList = apiStoreClientAllowedUser.getAllTags().getList();
                tagList = new ArrayList<>();
                for (TagDTO tempTagDTO : tagDtoList) {
                    tagList.add(tempTagDTO.getValue());
                }

                if (!tagList.contains(TAGS_RESTRICTED_API)) {
                    Thread.sleep(3000);
                    log.info("Waiting for the API Tags");
                } else {
                    break;
                }
            }
        }

        Assert.assertTrue(tagList.contains(TAGS_RESTRICTED_API),
                "Restricted visibility tag is not available for authorised user");
    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
        undeployAndDeleteAPIRevisionsUsingRest(publicApiId, restAPIPublisher);
        undeployAndDeleteAPIRevisionsUsingRest(restrictedApiId, restAPIPublisher);
        restAPIPublisher.deleteAPI(publicApiId);
        restAPIPublisher.deleteAPI(restrictedApiId);
        userManagementClient.deleteRole(ROLE);
        userManagementClient.deleteUser(ALLOWED_USER);
    }

}
