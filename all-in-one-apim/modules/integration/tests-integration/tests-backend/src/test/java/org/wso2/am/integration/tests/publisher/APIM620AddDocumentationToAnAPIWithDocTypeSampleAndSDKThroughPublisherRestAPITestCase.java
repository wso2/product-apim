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

import org.wso2.am.integration.clients.publisher.api.v1.dto.APIDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.DocumentDTO;
import org.wso2.am.integration.tests.api.lifecycle.APIManagerLifecycleBaseTest;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;
import org.wso2.am.integration.test.utils.bean.APICreationRequestBean;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.utils.multitenancy.MultitenantConstants;

import javax.ws.rs.core.Response;
import java.net.URL;

import static org.testng.Assert.assertEquals;

/**
 * Add Documentation To An API using  Doc Type Samples & SDK with different source types
 * Through Publisher Rest API
 * APIM2 -620 / APIM2 -621
 */
public class APIM620AddDocumentationToAnAPIWithDocTypeSampleAndSDKThroughPublisherRestAPITestCase
        extends APIManagerLifecycleBaseTest {

    private final String apiName = "APIM620PublisherTest";
    private final String apiVersion = "1.0.0";
    private final String summary = "This is documentation summary";
    private String apiEndPointUrl;
    private String apiId;
    private String sdkInlineDocumentId;
    private String sdkUrlDocumentId;
    private String provider;

    @Factory(dataProvider = "userModeDataProvider")
    public APIM620AddDocumentationToAnAPIWithDocTypeSampleAndSDKThroughPublisherRestAPITestCase
            (TestUserMode userMode) {
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

        String gatewayUrl;
        if (MultitenantConstants.SUPER_TENANT_DOMAIN_NAME.equals(gatewayContextWrk.getContextTenant().getDomain())) {
            gatewayUrl = gatewayUrlsWrk.getWebAppURLNhttp();
        } else {
            gatewayUrl = gatewayUrlsWrk.getWebAppURLNhttp() + "t/" +
                    gatewayContextWrk.getContextTenant().getDomain() + "/";
        }
        apiEndPointUrl = gatewayUrl + "jaxrs_basic/services/customers/customerservice";
        provider = user.getUserName();
    }

    @Test(groups = {"wso2.am"}, description = "Add Documentation To An API With Type HowTo And" +
            " Source Inline through the publisher rest API ")
    public void testAddDocumentToAnAPISDKInline() throws Exception {

        String apiContext = "apim620PublisherTestAPI";
        String apiDescription = "This is Test API Created by API Manager Integration Test";
        String apiTags = "tag620-1, tag621-2";
        String docName = "APIM611PublisherTestHowTo-Inline-summary";

        //Create an API
        APICreationRequestBean apiCreationRequestBean =
                new APICreationRequestBean(apiName, apiContext, apiVersion, provider,
                        new URL(apiEndPointUrl));
        apiCreationRequestBean.setTags(apiTags);
        apiCreationRequestBean.setDescription(apiDescription);
        apiCreationRequestBean.setTiersCollection("Gold,Bronze");
        apiCreationRequestBean.setDefaultVersion("default_version");
        apiCreationRequestBean.setDefaultVersionChecked("default_version");
        apiCreationRequestBean.setBizOwner("api620b");
        apiCreationRequestBean.setBizOwnerMail("api620b@ee.com");
        apiCreationRequestBean.setTechOwner("api620t");
        apiCreationRequestBean.setTechOwnerMail("api620t@ww.com");


        APIDTO apiDto = createAndPublishAPI(apiCreationRequestBean, restAPIPublisher, false);
        apiId = apiDto.getId();

        //Add Documentation to an API "apim620PublisherTestAPI" -  Samples | Inline
        DocumentDTO documentDTO = new DocumentDTO();
        documentDTO.setName(docName);
        documentDTO.setSummary(summary);
        documentDTO.setType(DocumentDTO.TypeEnum.SAMPLES);
        documentDTO.setSourceType(DocumentDTO.SourceTypeEnum.INLINE);
        documentDTO.setVisibility(DocumentDTO.VisibilityEnum.API_LEVEL);

        HttpResponse documentationResponse = restAPIPublisher.addDocument(apiId, documentDTO);
        assertEquals(documentationResponse.getResponseCode(), 200, "Error while add documentation to API");
        sdkInlineDocumentId = documentationResponse.getData();

        HttpResponse docRemoveResponse = restAPIPublisher.removeDocumentation(apiId, sdkInlineDocumentId);
        assertEquals(Response.Status.OK.getStatusCode(), docRemoveResponse.getResponseCode(), "Error when removing" +
                "documentation");
    }

    @Test(groups = {"wso2.am"}, description = "Add Documentation To An API With Type HowTo And" +
            " Source Url through the publisher rest API ",
            dependsOnMethods = "testAddDocumentToAnAPISDKInline")
    public void testAddDocumentToAnAPISDKUrl() throws Exception {

        String docName = "APIM621PublisherTestHowTo-Url-summary";
        String docUrl = "https://docs.wso2.com/display/AM191/Published+APIs";

        //Add Documentation to an API "APIM621PublisherTestHowTo-Url-summary" -  Samples | Url
        DocumentDTO documentDTO = new DocumentDTO();
        documentDTO.setName(docName);
        documentDTO.setSummary(summary);
        documentDTO.setType(DocumentDTO.TypeEnum.SAMPLES);
        documentDTO.setSourceUrl(docUrl);
        documentDTO.setSourceType(DocumentDTO.SourceTypeEnum.URL);
        documentDTO.setVisibility(DocumentDTO.VisibilityEnum.API_LEVEL);

        HttpResponse documentationResponse = restAPIPublisher.addDocument(apiId, documentDTO);
        assertEquals(documentationResponse.getResponseCode(), 200, "Error while add documentation to API");
        sdkUrlDocumentId = documentationResponse.getData();

        HttpResponse docRemoveResponse = restAPIPublisher.removeDocumentation(apiId, sdkUrlDocumentId);
        assertEquals(Response.Status.OK.getStatusCode(), docRemoveResponse.getResponseCode(), "Error when removing" +
                "documentation");
    }


    @AfterClass(alwaysRun = true)
    public void destroyAPIs() throws Exception {
        restAPIPublisher.deleteAPI(apiId);
        super.cleanUp();
    }


}
