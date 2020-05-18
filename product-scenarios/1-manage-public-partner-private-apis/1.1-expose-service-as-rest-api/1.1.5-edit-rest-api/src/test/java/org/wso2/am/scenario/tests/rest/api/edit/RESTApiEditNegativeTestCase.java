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
package org.wso2.am.scenario.tests.rest.api.edit;

import com.google.gson.Gson;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.testng.Assert;
import org.testng.annotations.*;
import org.wso2.am.integration.clients.publisher.api.ApiException;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIDTO;
import org.wso2.am.integration.test.utils.bean.APICreationRequestBean;
import org.wso2.am.scenario.test.common.ScenarioDataProvider;
import org.wso2.am.scenario.test.common.ScenarioTestBase;
import org.wso2.am.scenario.test.common.ScenarioTestConstants;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.testng.Assert.assertTrue;

public class RESTApiEditNegativeTestCase extends ScenarioTestBase {
    private static final Log log = LogFactory.getLog(ScenarioTestBase.class);

    private String apiName = UUID.randomUUID().toString();
    private String apiContext = "/" + UUID.randomUUID();
    private String apiVersion = "1.0.0";
    private String APICreator = "APICreatorEdit";
    private String description = "This is a API creation description";
    private String tag = "APICreationTag";
    private String invalidTag = "^invalid^";
    private String tierCollection = "Gold,Bronze";
    private String bizOwner = "wso2Test";
    private String bizOwnerMail = "wso2test@gmail.com";
    private String techOwner = "wso2";
    private String techOwnerMail = "wso2@gmail.com";
    private String default_version_checked = "default_version";
    private String backendEndPoint = "http://ws.cdyne.com/phoneverify/phoneverify.asmx";
    private APICreationRequestBean apiCreationRequestBean;
    private static final Log log = LogFactory.getLog(ScenarioTestBase.class);
    private String apiProductionEndPointUrl;
    private String apiId;
    private String apiProductionEndpointPostfixUrl = "jaxrs_basic/services/customers/" +
            "customerservice/customers/123";
    private String apiProviderName;
    private APIDTO apidto;
    private List apiDTOList = new ArrayList<APIDTO>();

    // All tests in this class will run with a super tenant API creator and a tenant API creator.
    @Factory(dataProvider = "userModeDataProvider")
    public RESTApiEditNegativeTestCase(TestUserMode userMode) {
        this.userMode = userMode;
    }

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {
        setup();
        super.init(userMode);

        apiProductionEndPointUrl = gatewayUrlsWrk.getWebAppURLHttp() +
                apiProductionEndpointPostfixUrl;
        apiProviderName = publisherContext.getContextTenant().getContextUser().getUserName();
        apiId = createAnAPI();
        apiDTOList.add(apiId);
    }

    @Test(description = "1.1.5.2")
    public void testRESTAPIEditWithInvalidValue() throws Exception {

        //Check availability of the API in publisher
        HttpResponse apiResponsePublisher = restAPIPublisher.getAPI(apiId);
        assertTrue(apiResponsePublisher.getData().contains(apiId), apiName + " is not visible in publisher");

        HttpResponse response = restAPIPublisher.getAPI(apiId);
        Gson g = new Gson();
        apidto = g.fromJson(response.getData(), APIDTO.class);
        List<String> tags = apidto.getTags();
        tags.add(invalidTag);
        apidto.setTags(tags);
        APIDTO apidto1 = null;
        try {
            apidto1 = restAPIPublisher.updateAPI(apidto);
        } catch (Exception e) {
            Assert.assertEquals(apidto1, null);
        }

        //Check whether the previously created api is not altered
        HttpResponse apiUpdateResponsePublisher = restAPIPublisher.getAPI(apiId);
        assertTrue(apiUpdateResponsePublisher.getData().contains(tag));
    }

    @Test(description = "1.1.5.16", dataProvider = "InvalidAPITags", dataProviderClass = ScenarioDataProvider.class)
    public void testRESTAPIEditTags(String tags) throws Exception {
        //Check availability of the API and the tag in publisher
        HttpResponse apiResponsePublisher = restAPIPublisher.getAPI(apiId);
        verifyResponse(apiResponsePublisher);
        assertTrue(apiResponsePublisher.getData().contains(tag), apiName + " does not have the tag " + tag);

        Gson g = new Gson();
        apidto = g.fromJson(apiResponsePublisher.getData(), APIDTO.class);

        //remove tags from the API and update with new tags
        List<String> tagList = apidto.getTags();
        tagList.add(String.valueOf((tags)));
        apidto.setTags(tagList);

        HttpResponse apiUpdateResponse = null;
        try {
            apidto = restAPIPublisher.updateAPI(apidto);
        } catch (Exception e) {
            Assert.assertTrue(((ApiException) e).getResponseBody().contains("Error while updating API : " + apiId));
        }
    }

    @AfterTest(alwaysRun = true)
    public void destroy() throws Exception {
        for (Object apidtoTemp : apiDTOList) {
            restAPIPublisher.deleteAPI(((String) apidtoTemp));
        }
    }

    private String createAnAPI() throws Exception {

        apiCreationRequestBean = new APICreationRequestBean(apiName, apiContext, apiVersion,
                APICreator, new URL(backendEndPoint));
        apiCreationRequestBean.setTags(tag);
        apiCreationRequestBean.setDescription(description);
        apiCreationRequestBean.setTiersCollection(tierCollection);
        apiCreationRequestBean.setDefaultVersion(default_version_checked);
        apiCreationRequestBean.setDefaultVersionChecked(default_version_checked);
        apiCreationRequestBean.setBizOwner(bizOwner);
        apiCreationRequestBean.setBizOwnerMail(bizOwnerMail);
        apiCreationRequestBean.setTechOwner(techOwner);
        apiCreationRequestBean.setTechOwnerMail(techOwnerMail);

        APIDTO apiDto = restAPIPublisher.addAPI(apiCreationRequestBean);
        assertTrue(StringUtils.isNotEmpty(apiDto.getId()), "Error occured when creating api");
        return apiDto.getId();
    }


    // This method runs prior to the @BeforeClass method.
    // Create users and tenants needed or the tests in here. Try to reuse the TENANT_WSO2 as much as possible to avoid the number of tenants growing.
    @DataProvider
    public static Object[][] userModeDataProvider() throws Exception {
        setup();
        //Add and activate wso2.com tenant
        addTenantAndActivate(ScenarioTestConstants.TENANT_WSO2, "admin", "admin");

        // create user in super tenant
        createUserWithPublisherAndCreatorRole("micheal", "Micheal#123", "admin", "admin");

        // create user in wso2.com tenant
        createUserWithPublisherAndCreatorRole("andrew", "Andrew#123", "admin@wso2.com", "admin");

        // return the relevant parameters for each test run
        // 1) Super tenant API creator
        // 2) Tenant API creator
        return new Object[][]{
                new Object[]{TestUserMode.SUPER_TENANT_USER},
                new Object[]{TestUserMode.TENANT_USER},
        };
    }
}
