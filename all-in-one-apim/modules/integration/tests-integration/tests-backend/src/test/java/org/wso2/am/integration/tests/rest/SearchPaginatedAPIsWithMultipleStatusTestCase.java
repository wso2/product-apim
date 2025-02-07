/*
*Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
*WSO2 Inc. licenses this file to you under the Apache License,
*Version 2.0 (the "License"); you may not use this file except
*in compliance with the License.
*You may obtain a copy of the License at
*
*http://www.apache.org/licenses/LICENSE-2.0
*
*Unless required by applicable law or agreed to in writing,
*software distributed under the License is distributed on an
*"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
*KIND, either express or implied.  See the License for the
*specific language governing permissions and limitations
*under the License.
*/

package org.wso2.am.integration.tests.rest;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;
import org.wso2.am.integration.clients.store.api.v1.dto.APIListDTO;
import org.wso2.am.integration.test.utils.bean.APILifeCycleAction;
import org.wso2.am.integration.test.utils.bean.APIRequest;
import org.wso2.am.integration.tests.api.lifecycle.APIManagerLifecycleBaseTest;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;
import org.wso2.carbon.utils.multitenancy.MultitenantConstants;

import java.net.URL;

import static org.testng.Assert.assertNotNull;

public class SearchPaginatedAPIsWithMultipleStatusTestCase extends APIManagerLifecycleBaseTest {

    private final int apiCount = 24;
    private static final String PROVIDER = "admin";
    private static final String API_NAME_PREFIX = "YoutubeFeeds";
    private static final String API_CONTEXT_PREFIX = "youtube";
    private static final String API_VERSION = "1.0.0";
    private static final String API_URL = "http://gdata.youtube.com/feeds/api/standardfeeds";
    private static final int PAGINATED_COUNT = 10;

    @Factory(dataProvider = "userModeDataProvider")
    public SearchPaginatedAPIsWithMultipleStatusTestCase(TestUserMode userMode) {
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
    }

    @Test(groups = { "wso2.am" }, description = "check paginated API count")
    public void testPaginationWithMultipleStatus() throws Exception {
        for (int i = 0; i < apiCount; i++) {
            APIRequest apiRequest = new APIRequest(API_NAME_PREFIX + i, API_CONTEXT_PREFIX + i,
                                                   new URL(API_URL));
            apiRequest.setVersion(API_VERSION);
            apiRequest.setProvider(PROVIDER);
            HttpResponse addApiResponse = restAPIPublisher.addAPI(apiRequest);
            String apiId = addApiResponse.getData();
            if (i % 2 == 0) {
                restAPIPublisher.changeAPILifeCycleStatus(apiId, APILifeCycleAction.PUBLISH.getAction());
            }
            Thread.sleep(500);
        }

        //Wait till APIs get indexed
        int returnApiCount = 0;
        for (int i = 0; i < 25; i++) {
            APIListDTO apiListDTO = restAPIStore
                    .searchPaginatedAPIs(PAGINATED_COUNT, 0, MultitenantConstants.SUPER_TENANT_DOMAIN_NAME,
                            API_NAME_PREFIX);
            assertNotNull(apiListDTO, "Unable to retrieve the requested APIs");
            returnApiCount = apiListDTO.getCount();
            if (returnApiCount == PAGINATED_COUNT) {
                break;
            }
            Thread.sleep(5000);
        }
        Assert.assertEquals(returnApiCount, PAGINATED_COUNT);
    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
        super.cleanUp();
    }
}