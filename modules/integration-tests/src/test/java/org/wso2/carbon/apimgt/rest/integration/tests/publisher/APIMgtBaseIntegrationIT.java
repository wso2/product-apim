/**
 * Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 * <p>
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.apimgt.rest.integration.tests.publisher;

import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.carbon.apimgt.rest.integration.tests.exceptions.AMIntegrationTestException;
import org.wso2.carbon.apimgt.rest.integration.tests.publisher.api.ApiCollectionApi;
import org.wso2.carbon.apimgt.rest.integration.tests.publisher.model.APIList;
import org.wso2.carbon.apimgt.rest.integration.tests.util.TestUtil;
import org.wso2.carbon.apimgt.rest.integration.tests.util.TokenInfo;

import java.util.Collections;

public class APIMgtBaseIntegrationIT {

    protected ApiClient apiPublisherClient;
    protected org.wso2.carbon.apimgt.rest.integration.tests.store.ApiClient apiStoreClient;
    protected org.wso2.carbon.apimgt.rest.integration.tests.admin.ApiClient apiAdminClient;

    @BeforeClass
    public void init() throws AMIntegrationTestException {

        TokenInfo tokenInfo = TestUtil.getToken("admin", "admin");
        apiPublisherClient = new ApiClient(TestUtil.OAUTH2_SECURITY).setBasePath
                ("https://" + TestUtil.getIpAddressOfContainer() + ":9443/api/am/publisher/v1.0");
        apiPublisherClient.setAccessToken(tokenInfo.getToken(), tokenInfo.getExpiryTime());
        apiStoreClient = new org.wso2.carbon.apimgt.rest.integration.tests.store.ApiClient(TestUtil.OAUTH2_SECURITY)
                .setBasePath("https://" + TestUtil.getIpAddressOfContainer() + ":9443/api/am/store/v1.0");
        apiStoreClient.setAccessToken(tokenInfo.getToken(), tokenInfo.getExpiryTime());
        apiAdminClient = new org.wso2.carbon.apimgt.rest.integration.tests.admin.ApiClient(TestUtil.OAUTH2_SECURITY)
                .setBasePath("https://" + TestUtil.getIpAddressOfContainer() + ":9443/api/am/admin/v1.0");
        apiAdminClient.setAccessToken(tokenInfo.getToken(), tokenInfo.getExpiryTime());
    }

    @Test
    public void testApiSearch() {
        ApiCollectionApi apiCollectionApi = apiPublisherClient.buildClient(ApiCollectionApi.class);

        APIList apiList = apiCollectionApi.apisGet("", Collections.emptyMap());
        Assert.assertEquals(apiList.getCount().intValue(), 0);
    }
}
