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
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.am.integration.tests.publisher;

import static org.testng.Assert.assertEquals;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;
import org.wso2.am.integration.test.utils.bean.APICreationRequestBean;
import org.wso2.am.integration.test.utils.bean.APIResourceBean;
import org.wso2.am.integration.test.utils.clients.APIPublisherRestClient;
import org.wso2.am.integration.tests.api.lifecycle.APIManagerLifecycleBaseTest;
import org.wso2.carbon.apimgt.api.model.APIIdentifier;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

public class APIResourceWithTemplateTestCase extends APIManagerLifecycleBaseTest {

    private final String API_NAME_DEFAULT = "APIResourceWithTemplateDef";
    private final String API_CONTEXT_DEFAULT = "APIResourceWithTemplateDef";
    private final String API_NAME = "APIResourceWithTemplate";
    private final String API_CONTEXT = "APIResourceWithTemplate";
    private static final String API_VERSION_1_0_0 = "1.0.0";
    private final String API_TAGS = "testTag1, testTag2, testTag3";
    private String providerName;
    private APICreationRequestBean apiCreationRequestBean;
    private APIPublisherRestClient apiPublisherClientUser1;
    private final String API_END_POINT_POSTFIX_URL = "jaxrs_basic/services/customers/customerservice/";
    private String apiEndPointUrl;
    private APIIdentifier apiIdentifier;
 
    @Factory(dataProvider = "userModeDataProvider")
    public APIResourceWithTemplateTestCase(TestUserMode userMode) {
        this.userMode = userMode;
    }

    @DataProvider
    public static Object[][] userModeDataProvider() {
        return new Object[][]{
                new Object[]{TestUserMode.SUPER_TENANT_ADMIN},
                new Object[]{TestUserMode.TENANT_ADMIN},
        };
    }

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {
        super.init(userMode);
        apiEndPointUrl = getGatewayURLHttp() + API_END_POINT_POSTFIX_URL;
        providerName = user.getUserName();

        String publisherURLHttp = getPublisherURLHttps();
        apiPublisherClientUser1 = new APIPublisherRestClient(publisherURLHttp);

        // Login to API Publisher with admin
        apiPublisherClientUser1.login(user.getUserName(), user.getPassword());

    }

    @Test(groups = { "wso2.am" }, description = "Test API with resouce containing url template for default api")
    public void testAPIwithResourceTemplateForDefaultAPI() throws Exception {
        apiCreationRequestBean = new APICreationRequestBean(API_NAME_DEFAULT, API_CONTEXT_DEFAULT, API_VERSION_1_0_0,
                providerName, new URL(apiEndPointUrl));
        apiCreationRequestBean.setTags(API_TAGS);
        apiCreationRequestBean.setDefaultVersionChecked("default_version");
        List<APIResourceBean> resourceBeanList = new ArrayList<APIResourceBean>();

        APIResourceBean res = new APIResourceBean("GET", "Application & Application User", "Unlimited",
                "/resource/{param}");
        resourceBeanList.add(res);
        apiCreationRequestBean.setResourceBeanList(resourceBeanList);

        HttpResponse createAPIResponse = apiPublisherClientUser1.addAPI(apiCreationRequestBean);

        assertEquals(createAPIResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK,
                "API APIResourceWithTemplateDef creation failed");

        // publish the api
        apiIdentifier = new APIIdentifier(providerName, API_NAME_DEFAULT, API_VERSION_1_0_0);
        HttpResponse publishAPIResponse = publishAPI(apiIdentifier, apiPublisherClientUser1, false);
        assertEquals(publishAPIResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK,
                "API APIResourceWithTemplateDef publish failed");
    }

    @Test(groups = { "wso2.am" }, description = "Test API with resouce containing url template")
    public void testAPIwithResourceTemplateForAPI() throws Exception {
        apiCreationRequestBean = new APICreationRequestBean(API_NAME, API_CONTEXT, API_VERSION_1_0_0, providerName,
                new URL(apiEndPointUrl));
        apiCreationRequestBean.setTags(API_TAGS);
        List<APIResourceBean> resourceBeanList = new ArrayList<APIResourceBean>();

        APIResourceBean res = new APIResourceBean("GET", "Application & Application User", "Unlimited",
                "/resource/{param}");
        resourceBeanList.add(res);
        apiCreationRequestBean.setResourceBeanList(resourceBeanList);

        HttpResponse createAPIResponse = apiPublisherClientUser1.addAPI(apiCreationRequestBean);

        assertEquals(createAPIResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK,
                "API APIResourceWithTemplateDef creation failed");

        // publish the api
        apiIdentifier = new APIIdentifier(providerName, API_NAME, API_VERSION_1_0_0);
        HttpResponse publishAPIResponse = publishAPI(apiIdentifier, apiPublisherClientUser1, false);
        assertEquals(publishAPIResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK,
                "API APIResourceWithTemplateDef publish failed");
    }

    @AfterClass(alwaysRun = true)
    public void cleanUpArtifacts() throws Exception {
        super.cleanUp();
    }

}
