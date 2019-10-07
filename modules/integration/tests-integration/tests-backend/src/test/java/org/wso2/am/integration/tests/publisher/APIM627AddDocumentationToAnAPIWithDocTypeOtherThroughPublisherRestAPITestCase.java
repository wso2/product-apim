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

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.DocumentDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.DocumentListDTO;
import org.wso2.am.integration.test.utils.base.APIMIntegrationBaseTest;
import org.wso2.am.integration.test.utils.bean.APICreationRequestBean;
import org.wso2.am.integration.test.utils.bean.APILifeCycleState;
import org.wso2.am.integration.test.utils.clients.APIPublisherRestClient;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;
import org.wso2.carbon.utils.multitenancy.MultitenantConstants;

import javax.ws.rs.core.Response;
import java.net.URL;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/**
 * Add Documentation To An API using  Doc Type Other with different source types  and Remove
 * API Documentation Through Publisher Rest API
 * APIM2 -627 / APIM2 -628 /APIM2 -633
 */
public class APIM627AddDocumentationToAnAPIWithDocTypeOtherThroughPublisherRestAPITestCase extends
        APIMIntegrationBaseTest {

    private final String apiName = "APIM627PublisherTest";
    private final String apiVersion = "1.0.0";
    private String apiProvider;
    private String apiEndPointUrl;
    private String apiId;
    private String inlineTypeDocumentId;
    private String urlTypeDocumentId;
    private String summary = "This is summary of the documentation";
    private String category = "Other";

    @Factory(dataProvider = "userModeDataProvider")
    public APIM627AddDocumentationToAnAPIWithDocTypeOtherThroughPublisherRestAPITestCase(TestUserMode userMode) {
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
        apiProvider = publisherContext.getContextTenant().getContextUser().getUserName();

    }

    @Test(groups = {"wso2.am"}, description = "Add Documentation To An API With Type Other And" +
            " Source Inline through the publisher rest API ")
    public void testAddDocumentToAnAPIOtherInline() throws Exception {
        String apiContext = "apim627PublisherTestAPI";
        String apiDescription = "This is Test API Created by API Manager Integration Test";
        String apiTags = "tag627-1, tag628-2";
        String docName = "APIM627PublisherTestHowTo-Inline-summary";
        //Create an API
        APICreationRequestBean apiCreationRequestBean = new APICreationRequestBean(apiName, apiContext, apiVersion,
                apiProvider, new URL(apiEndPointUrl));
        apiCreationRequestBean.setTags(apiTags);
        apiCreationRequestBean.setDescription(apiDescription);
        apiCreationRequestBean.setTiersCollection("Gold,Bronze");
        apiCreationRequestBean.setDefaultVersion("default_version");
        apiCreationRequestBean.setDefaultVersionChecked("default_version");
        apiCreationRequestBean.setBizOwner("api620b");
        apiCreationRequestBean.setBizOwnerMail("api620b@ee.com");
        apiCreationRequestBean.setTechOwner("api620t");
        apiCreationRequestBean.setTechOwnerMail("api620t@ww.com");

        APIDTO apiCreationResponse = restAPIPublisher.addAPI(apiCreationRequestBean);
        String status = apiCreationResponse.getLifeCycleStatus();
        apiId = apiCreationResponse.getId();
        assertTrue(APILifeCycleState.CREATED.getState().equalsIgnoreCase(status), "Status of the " + apiName +
                "is not a valid status");
        //Check availability of the API in publisher
        HttpResponse apiResponsePublisher = restAPIPublisher.getAPI(apiId);
        assertEquals(apiResponsePublisher.getResponseCode(), Response.Status.OK.getStatusCode(), apiName +
                " is not visible in publisher");
        //Add Documentation to an API "APIM627PublisherTest" - Other |Inline
        DocumentDTO documentDTO = new DocumentDTO();
        documentDTO.setName(docName);
        documentDTO.setType(DocumentDTO.TypeEnum.OTHER);
        documentDTO.setSourceType(DocumentDTO.SourceTypeEnum.INLINE);
        documentDTO.setSummary(summary);
        documentDTO.setVisibility(DocumentDTO.VisibilityEnum.API_LEVEL);
        documentDTO.setOtherTypeName(category);
        HttpResponse documentationResponse = restAPIPublisher.addDocument(apiId, documentDTO);
        assertEquals(documentationResponse.getResponseCode(), 200, "Error while add documentation to API");
        inlineTypeDocumentId = documentationResponse.getData();
    }

    @Test(groups = {"wso2.am"}, description = "Add Documentation To An API With Type HowTo And" +
            " Source Url through the publisher rest API ",
            dependsOnMethods = "testAddDocumentToAnAPIOtherInline")
    public void testAddDocumentToAnAPIOtherUrl() throws Exception {
        String docName = "APIM628PublisherTestHowTo-Url-summary";
        String docUrl = "https://docs.wso2.com/display/AM191/Published+APIs";
        //Add Documentation to an API "APIM627PublisherTest" - Other | Url
        DocumentDTO documentDTO = new DocumentDTO();
        documentDTO.setName(docName);
        documentDTO.setType(DocumentDTO.TypeEnum.OTHER);
        documentDTO.setSourceType(DocumentDTO.SourceTypeEnum.URL);
        documentDTO.setSummary(summary);
        documentDTO.setVisibility(DocumentDTO.VisibilityEnum.API_LEVEL);
        documentDTO.setOtherTypeName(category);
        documentDTO.setSourceUrl(docUrl);
        HttpResponse documentationResponse = restAPIPublisher.addDocument(apiId, documentDTO);
        assertEquals(documentationResponse.getResponseCode(), 200, "Error while add documentation to API");
        urlTypeDocumentId = documentationResponse.getData();
    }


    @Test(groups = {"wso2.am"}, description = "Remove API Documentation from the API through " +
            "the publisher rest API ",
            dependsOnMethods = "testAddDocumentToAnAPIOtherInline")
    public void testRemoveDocumentationOtherTheAPI() throws Exception {
        //Remove Documentation
        HttpResponse docRemoveResponse = restAPIPublisher.removeDocumentation(apiId, inlineTypeDocumentId);
        assertEquals(Response.Status.OK.getStatusCode(), docRemoveResponse.getResponseCode(), "Error when removing" +
                "documentation");
        restAPIPublisher.removeDocumentation(apiId, urlTypeDocumentId);
        //Check availability of the Documentation after Removing
        DocumentListDTO docResponse = restAPIPublisher.getDocuments(apiId);
        assertTrue(docResponse.getList().isEmpty(), "Documentations not removed properly");
    }

    @AfterClass(alwaysRun = true)
    public void destroyAPIs() throws Exception {
        restAPIPublisher.deleteAPI(apiId);
        super.cleanUp();
    }
}
