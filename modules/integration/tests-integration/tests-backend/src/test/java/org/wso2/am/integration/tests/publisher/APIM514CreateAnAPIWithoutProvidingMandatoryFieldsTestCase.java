/*
 *
 *   Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *   WSO2 Inc. licenses this file to you under the Apache License,
 *   Version 2.0 (the "License"); you may not use this file except
 *   in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing,
 *   software distributed under the License is distributed on an
 *   "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *   KIND, either express or implied.  See the License for the
 *   specific language governing permissions and limitations
 *   under the License.
 *
 */

package org.wso2.am.integration.tests.publisher;

import org.apache.http.HttpStatus;
import org.apache.commons.lang.StringUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;
import org.wso2.am.integration.clients.publisher.api.ApiException;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIDTO;
import org.wso2.am.integration.test.utils.APIManagerIntegrationTestException;
import org.wso2.am.integration.test.utils.bean.APICreationRequestBean;
import org.wso2.am.integration.test.utils.bean.APIDesignBean;
import org.wso2.am.integration.tests.api.lifecycle.APIManagerLifecycleBaseTest;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import java.net.URL;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertEquals;

/**
 * Create an API through the Publisher Rest API without providing mandatory fields
 * JIRA Issue Id - APIMANAGER-4039
 */


public class APIM514CreateAnAPIWithoutProvidingMandatoryFieldsTestCase extends
        APIManagerLifecycleBaseTest {
    private final String apiNameTest1 = "APIM514PublisherTest1";
    private final String apiVersion = "1.0.0";
    private final String apiDescription = "This is Test API Created by API Manager Integration Test";
    private String apiTag = "tag514-1, tag514-2, tag514-3";
    private String apiProviderName;
    private String apiProductionEndPointUrl;

    @Factory(dataProvider = "userModeDataProvider")
    public APIM514CreateAnAPIWithoutProvidingMandatoryFieldsTestCase(TestUserMode userMode) {
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

        String apiProductionEndpointPostfixUrl = "jaxrs_basic/services/customers/" +
                "customerservice/customers/123";

        apiProductionEndPointUrl = gatewayUrlsWrk.getWebAppURLHttp() + apiProductionEndpointPostfixUrl;
        apiProviderName = publisherContext.getContextTenant().getContextUser().getUserName();

    }

    @Test(groups = {"wso2.am"}, description = "Create an API Through the Publisher Rest API" +
            " without proving API Name")
    public void testCreateAnAPIThroughThePublisherRestWithoutName() throws Exception {

        String apiContextTest = "apim514PublisherTestAPI1";

        APICreationRequestBean apiCreationRequestBean =
                new APICreationRequestBean("", apiContextTest, apiVersion, apiProviderName,
                        new URL(apiProductionEndPointUrl));
        apiCreationRequestBean.setTags(apiTag);
        apiCreationRequestBean.setDescription(apiDescription);

        try {
            APIDTO apiDto = createAndPublishAPI(apiCreationRequestBean, restAPIPublisher, false);
            assertFalse(StringUtils.isNotEmpty(apiDto.getId()), "Api is not created without proving API Name");
        } catch (ApiException e) {
            assertEquals(e.getCode(), HttpStatus.SC_BAD_REQUEST, "Required Parameter name is not provided " );
        }
//        Need to uncomment this after resolving https://github.com/wso2/product-apim/issues/6172
//        JSONObject apiResponse = new JSONObject(apiId);
//        assertTrue(apiResponse.getBoolean("error"), "can be create API without name");
//        assertTrue(apiResponse.getString("message").contains
//                ("API name is not specified"), "can be create API without name");
    }

    @Test(groups = {"wso2.am"}, description = "Create an API Through the Publisher Rest API " +
            "without proving Context")
    public void testCreateAnAPIThroughThePublisherRestWithoutContext() throws Exception {

        APICreationRequestBean apiCreationRequestBean =
                new APICreationRequestBean(apiNameTest1, "", apiVersion, apiProviderName,
                        new URL(apiProductionEndPointUrl));
        apiCreationRequestBean.setTags(apiTag);
        apiCreationRequestBean.setDescription(apiDescription);

        try {
            APIDTO apiDto = createAndPublishAPI(apiCreationRequestBean, restAPIPublisher, false);
            assertFalse(StringUtils.isNotEmpty(apiDto.getId()), "Api is not created without proving Context");
        } catch (ApiException e) {
            assertEquals(e.getCode(), HttpStatus.SC_BAD_REQUEST, "Required Parameter context is not provided " );
        }
//        Need to uncomment this after resolving https://github.com/wso2/product-apim/issues/6172
//        JSONObject apiResponse = new JSONObject(apiId);
//        assertTrue(apiResponse.getBoolean("error"), apiNameTest1 + "can be create without Context");
//        assertTrue(apiResponse.getString("message").contains
//                        ("Context not defined for API"),
//                apiNameTest1 + "can be create without Context");
    }

    @Test(groups = {"wso2.am"}, description = "Create an API Through the Publisher Rest API " +
            "without proving version")
    public void testCreateAnAPIThroughThePublisherRestWithoutVersion() throws Exception {

        String apiContextTest = "apim514PublisherTestAPI2";
        String apiNameTest2 = "APIM514PublisherTest2";

        APICreationRequestBean apiCreationRequestBean =
                new APICreationRequestBean(apiNameTest2, apiContextTest, "", apiProviderName,
                        new URL(apiProductionEndPointUrl));
        apiCreationRequestBean.setTags(apiTag);
        apiCreationRequestBean.setDescription(apiDescription);

        try {
            APIDTO apiDto = createAndPublishAPI(apiCreationRequestBean, restAPIPublisher, false);
            assertFalse(StringUtils.isNotEmpty(apiDto.getId()), "Api is not created without proving Version");
        } catch (ApiException e) {
            assertEquals(e.getCode(), HttpStatus.SC_BAD_REQUEST, "Required Parameter version is not provided " );
        }
//        Need to uncomment this after resolving https://github.com/wso2/product-apim/issues/6172
//        JSONObject apiResponse = new JSONObject(apiId);
//        assertTrue(apiResponse.getBoolean("error"), apiNameTest2 + "can be create without Version");
//        assertTrue(apiResponse.getString("message").contains
//                ("Version not specified for API " + apiNameTest2), apiNameTest2 + "can be create without Version");
    }

    @Test(groups = {"wso2.am"}, description = "Create an API Through the Publisher Rest API " +
            "without proving tier availability")
    public void testCreateAnAPIThroughThePublisherRestWithoutTierAvailability() throws Exception {

        String apiContextTest = "apim514PublisherTestAPI3";
        String apiNameTest3 = "APIM514PublisherTest3";

        APICreationRequestBean apiCreationRequestBean =
                new APICreationRequestBean(apiNameTest3, apiContextTest, apiVersion, apiProviderName,
                        new URL(apiProductionEndPointUrl));
        apiCreationRequestBean.setTags(apiTag);
        apiCreationRequestBean.setDescription(apiDescription);
        apiCreationRequestBean.setTiersCollection("");
        apiCreationRequestBean.setTier("");

        try {
            APIDTO apiDto = createAndPublishAPI(apiCreationRequestBean, restAPIPublisher, false);
            assertFalse(StringUtils.isEmpty(apiDto.getId()), "Api is created without proving Version");
        } catch (ApiException e) {
            assertEquals(e.getCode(), HttpStatus.SC_INTERNAL_SERVER_ERROR, "Internal Server Error " );
        }
//        Need to uncomment this after resolving https://github.com/wso2/product-apim/issues/6172
//        JSONObject apiResponse = new JSONObject(apiDto.getId());
//        assertTrue(apiResponse.getBoolean("error"), apiNameTest3 +
//                "can be create without Tier availability");
//        assertTrue(apiResponse.getString("message").contains
//                ("No tier defined for the API"), apiNameTest3 + "can be create without Tier availability");
    }
    
//TODO Disabling test case due to the error occured while creating empty/null URL
//    @Test(groups = {"wso2.am"}, description = "Create an API Through the Publisher Rest API " +
//            "without proving Production endpoint")
    public void testCreateAnAPIThroughThePublisherRestWithoutEndpoint() throws Exception {

        String apiContextTest = "apim514PublisherTestAPI4";
        String apiNameTest4 = "APIM514PublisherTest4";
        URL url = null;
        APICreationRequestBean apiCreationRequestBean =
                new APICreationRequestBean(apiNameTest4, apiContextTest, apiVersion, apiProviderName,
                        url);
        apiCreationRequestBean.setTags(apiTag);
        apiCreationRequestBean.setDescription(apiDescription);

        APIDTO apiDto = createAndPublishAPI(apiCreationRequestBean, restAPIPublisher, false);
//        JSONObject apiResponse = new JSONObject(apiDto.getId());
//        assertTrue(apiResponse.getBoolean("error"), apiNameTest4 + "can be create without Endpoint");
//        assertTrue(apiResponse.getString("message").contains
//                ("Endpoint Configuration is missing"), apiNameTest4 + "can be create without Endpoint");
    }

//TODO Disabling test case: Reference https://wso2.org/jira/browse/APIMANAGER-4240
//    @Test(groups = {"wso2.am"}, description = "Create an API Through the Publisher Rest API " +
//            "without proving Resources")
    public void testCreateAnAPIThroughThePublisherRestWithoutResources() throws Exception {

        String apiContextTest = "apim514PublisherTestAPI5";
        String apiNameTest5 = "APIM514PublisherTest5";

        APIDesignBean designBean =
                new APIDesignBean(apiNameTest5, apiContextTest, apiVersion, apiDescription, apiTag);
        designBean.setSwagger("");

        HttpResponse apiCreationResponse = apiPublisher.designAPI(designBean);
//        JSONObject apiResponse = new JSONObject(apiCreationResponse.getData());
//        assertTrue(apiResponse.getBoolean("error"), apiNameTest5 + "can be create without Resources");
//        assertTrue(apiResponse.getString("message").contains
//                ("Invalid resource configuration "), apiNameTest5 + "can be create without Resources");
    }

    @Test(groups = {"wso2.am"}, description = "Create an API Through the Publisher Rest API " +
            "without proving Action")
    public void testCreateAnAPIThroughThePublisherRestWithoutAction() throws Exception {

        String apiContextTest = "apim514PublisherTestAPI6";
        String apiNameTest6 = "APIM514PublisherTest6";

        APICreationRequestBeanWithoutAction apiCreationRequestBean =
                new APICreationRequestBeanWithoutAction
                        (apiNameTest6, apiContextTest, apiVersion, apiProviderName,
                                new URL(apiProductionEndPointUrl));
        apiCreationRequestBean.setTags(apiTag);
        apiCreationRequestBean.setDescription(apiDescription);
        apiCreationRequestBean.setAction("");

        try {
            APIDTO apiDto = createAndPublishAPI(apiCreationRequestBean, restAPIPublisher, false);
            assertFalse(StringUtils.isEmpty(apiDto.getId()), "Api is created without proving Action");
        } catch (ApiException e) {
            assertEquals(e.getCode(), HttpStatus.SC_INTERNAL_SERVER_ERROR, "Internal Server Error " );
        }
//        Need to uncomment this after resolving https://github.com/wso2/product-apim/issues/6172
//        JSONObject apiResponse = new JSONObject(apiDto.getId());
//        assertTrue(apiResponse.getBoolean("error"), apiNameTest6 + "can be create without Action");
//        assertTrue(apiResponse.getString("message").contains
//                (" is not supported"), apiNameTest6 + "can be create without Action");
    }

    @AfterClass(alwaysRun = true)
    public void destroyAPIs() throws Exception {
        super.cleanUp();
    }

    private class APICreationRequestBeanWithoutAction extends APICreationRequestBean {

        public APICreationRequestBeanWithoutAction(String apiName, String context, String version,
                                                   String provider, URL endpointUrl)
                throws APIManagerIntegrationTestException {
            super(apiName, context, version, provider, endpointUrl);
        }

        @Override
        public void setAction() {
            //setting action empty
            setAction("");
        }
    }
}
