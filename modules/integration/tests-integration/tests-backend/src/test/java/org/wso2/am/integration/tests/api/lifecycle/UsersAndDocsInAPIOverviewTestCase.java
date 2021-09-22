/*
 * Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.am.integration.tests.api.lifecycle;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.ApplicationInfoDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.DocumentDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.DocumentListDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.SubscriptionDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.SubscriptionListDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationDTO;
import org.wso2.am.integration.test.impl.RestAPIStoreImpl;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.bean.APICreationRequestBean;
import org.wso2.carbon.apimgt.api.model.APIIdentifier;
import org.wso2.carbon.automation.engine.context.TestUserMode;

import java.net.URL;

/**
 * Subscribe the API by more users and add documentations to API. In API overview it should show the correct
 * user subscription count. Document tab should show the correct  information about documents and the Users
 * tab should show the correct information about subscribed uses.
 */
public class UsersAndDocsInAPIOverviewTestCase extends APIManagerLifecycleBaseTest {
    private final String API_NAME = "UsersAndDocsInAPIOverviewTest";
    private final String API_CONTEXT = "UsersAndDocsInAPIOverview";
    private final String API_TAGS = "testTag1, testTag2, testTag3";
    private final String API_DESCRIPTION = "This is test API create by API manager integration test";
    private final String API_VERSION_1_0_0 = "1.0.0";
    private final String USER_KEY_USER2 = "userKey1";
    private final String APPLICATION_NAME = "UsersAndDocsInAPIOverviewTestCase";
    private final String API_END_POINT_POSTFIX_URL = "jaxrs_basic/services/customers/customerservice/";
    private String apiEndPointUrl;
    private String providerName;
    private String apiID;
    private String app1ID;
    private String app2ID;
    private APIIdentifier apiIdentifier;
    private RestAPIStoreImpl apiStoreClientUser1;
    private RestAPIStoreImpl apiStoreClientUser2;
    private APICreationRequestBean apiCreationRequestBean;

    @Factory(dataProvider = "userModeDataProvider")
    public UsersAndDocsInAPIOverviewTestCase(TestUserMode userMode) {
        this.userMode = userMode;
    }

    @BeforeClass(alwaysRun = true)
    public void initialize() throws Exception {
        super.init();
        apiEndPointUrl = backEndServerUrl.getWebAppURLHttp() + API_END_POINT_POSTFIX_URL;
        providerName = user.getUserName();
        apiCreationRequestBean = new APICreationRequestBean(API_NAME, API_CONTEXT, API_VERSION_1_0_0, providerName,
                new URL(apiEndPointUrl));
        apiCreationRequestBean.setTags(API_TAGS);
        apiCreationRequestBean.setDescription(API_DESCRIPTION);
        apiStoreClientUser1 = getRestAPIStoreForUser(user.getUserName(), user.getPassword(), user.getUserDomain()
        );
        apiStoreClientUser2 =
                getRestAPIStoreForUser(publisherContext.getContextTenant().getTenantUser(USER_KEY_USER2).getUserName(),
                        publisherContext.getContextTenant().getTenantUser(USER_KEY_USER2).getPassword(),
                        publisherContext.getContextTenant().getTenantUser(USER_KEY_USER2).getUserDomain());
        apiIdentifier = new APIIdentifier(providerName, API_NAME, API_VERSION_1_0_0);
    }

    @Test(groups = {"wso2.am"}, description = "test the user count in API overview is correct")
    public void testNumberOfUsersInAPIOverview() throws Exception {
        String applicationDescription = "";
        String applicationCallBackUrl = "";
        ApplicationDTO app1 = apiStoreClientUser1.addApplication(APPLICATION_NAME,
                APIMIntegrationConstants.APPLICATION_TIER.DEFAULT_APP_POLICY_FIFTY_REQ_PER_MIN, applicationCallBackUrl,
                applicationDescription);
        ApplicationDTO app2 = apiStoreClientUser2.addApplication(APPLICATION_NAME,
                APIMIntegrationConstants.APPLICATION_TIER.DEFAULT_APP_POLICY_FIFTY_REQ_PER_MIN, applicationCallBackUrl,
                applicationDescription);
        app1ID = app1.getApplicationId();
        app2ID = app2.getApplicationId();
        //Create publish and subscribe a API by user 1
        APIIdentifier apiIdentifier = new APIIdentifier(providerName, API_NAME, API_VERSION_1_0_0);
        apiIdentifier.setTier(TIER_GOLD);
        APIDTO apidto = createPublishAndSubscribeToAPI(apiIdentifier, apiCreationRequestBean, restAPIPublisher,
                apiStoreClientUser1, app1.getApplicationId(), TIER_GOLD);
        apiID = apidto.getId();
        SubscriptionListDTO subscriptionListDTO = restAPIPublisher.getSubscriptionByAPIID(apidto.getId());
        Assert.assertEquals(subscriptionListDTO.getCount().intValue(), 1);
        // subscribe 2nd user
        apiStoreClientUser2.subscribeToAPI(apiID, app2.getApplicationId(), TIER_GOLD);
        SubscriptionListDTO subscriptionListDTO1 = restAPIPublisher.getSubscriptionByAPIID(apidto.getId());
        Assert.assertEquals(subscriptionListDTO1.getCount().intValue(), 2);

        for (SubscriptionDTO dto : subscriptionListDTO1.getList()) {
            ApplicationInfoDTO infoDTO = dto.getApplicationInfo();
            if (infoDTO.getApplicationId().equalsIgnoreCase(app1.getApplicationId())) {
                Assert.assertEquals(infoDTO.getSubscriber(), user.getUserName());
            }
            if (infoDTO.getApplicationId().equalsIgnoreCase(app2.getApplicationId())) {
                Assert.assertEquals(infoDTO.getSubscriber(),
                        publisherContext.getContextTenant().getTenantUser(USER_KEY_USER2).getUserName());
            }
        }
    }

    @Test(groups = {"wso2.am"}, description = "test user information in API overview Docs tab is correct",
            dependsOnMethods = "testNumberOfUsersInAPIOverview")
    public void testDocInformationInDocsTabInAPIOverview() throws Exception {
        // Add 2 documents
        DocumentDTO documentDTO1 = new DocumentDTO();
        documentDTO1.setName("Doc1");
        documentDTO1.setType(DocumentDTO.TypeEnum.HOWTO);
        documentDTO1.setSourceType(DocumentDTO.SourceTypeEnum.INLINE);
        documentDTO1.setSummary("test doc 1");
        documentDTO1.setVisibility(DocumentDTO.VisibilityEnum.API_LEVEL);

        DocumentDTO documentDTO2 = new DocumentDTO();
        documentDTO2.setName("Doc2");
        documentDTO2.setType(DocumentDTO.TypeEnum.HOWTO);
        documentDTO2.setSourceType(DocumentDTO.SourceTypeEnum.INLINE);
        documentDTO2.setSummary("test doc 2");
        documentDTO2.setVisibility(DocumentDTO.VisibilityEnum.API_LEVEL);

        restAPIPublisher.addDocument(apiID, documentDTO1);
        restAPIPublisher.addDocument(apiID, documentDTO2);

        DocumentListDTO documentListDTO = restAPIPublisher.getDocuments(apiID);
        Assert.assertEquals(documentListDTO.getCount().intValue(), 2);
    }

    @AfterClass(alwaysRun = true)
    public void cleanUpArtifacts() throws Exception {
        if (apiStoreClientUser1 != null) {
            apiStoreClientUser1.removeApplicationById(app1ID);
        }
        if (apiStoreClientUser2 != null) {
            apiStoreClientUser2.removeApplicationById(app2ID);
        }
        restAPIPublisher.deleteAPI(apiID);
    }

    @DataProvider
    public static Object[][] userModeDataProvider() {
        return new Object[][] { new Object[] { TestUserMode.SUPER_TENANT_ADMIN },
                new Object[] { TestUserMode.TENANT_ADMIN }, };
    }
}
