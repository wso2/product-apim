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
import org.wso2.am.integration.clients.publisher.api.v1.dto.DocumentDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.DocumentListDTO;
import org.wso2.am.integration.test.utils.base.APIMIntegrationBaseTest;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.bean.APIRequest;
import org.wso2.am.integration.test.utils.clients.APIPublisherRestClient;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import java.net.URL;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/**
 * Add Documentation To An API with doc Type How To and different Source types
 * Through Publisher Rest API
 * APIM2 -611 / APIM2 -612
 */
public class APIM611AddDocumentationToAnAPIWithDocTypeHowToThroughPublisherRestAPITestCase extends
        APIMIntegrationBaseTest {

    private final String apiName = "APIM620PublisherTest";
    private final String apiVersion = "1.0.0";
    private APIPublisherRestClient apiPublisher;
    private String apiProvider;
    private String apiEndPointUrl;
    private String apiId;

    @Factory(dataProvider = "userModeDataProvider")
    public APIM611AddDocumentationToAnAPIWithDocTypeHowToThroughPublisherRestAPITestCase
            (TestUserMode userMode) {
        this.userMode = userMode;
    }

    @DataProvider
    public static Object[][] userModeDataProvider() {
        return new Object[][]{
                new Object[]{TestUserMode.SUPER_TENANT_ADMIN},
//                new Object[]{TestUserMode.TENANT_ADMIN},
        };
    }

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {
        super.init(userMode);

        String apiProductionEndpointPostfixUrl = "jaxrs_basic/services/customers/" +
                "customerservice/customers/123";

        String publisherURLHttp = publisherUrls.getWebAppURLHttp();

        apiPublisher = new APIPublisherRestClient(publisherURLHttp);
        apiPublisher.login(publisherContext.getContextTenant().getContextUser().getUserName(),
                publisherContext.getContextTenant().getContextUser().getPassword());

        apiEndPointUrl = gatewayUrlsWrk.getWebAppURLHttp() + apiProductionEndpointPostfixUrl;
        apiProvider = publisherContext.getContextTenant().getContextUser().getUserName();

    }

    @Test(groups = {"wso2.am"}, description = "Add Documentation To An API With Type HowTo And" +
            " Source Inline through the publisher rest API ")
    public void testAddDocumentToAnAPIHowToInline() throws Exception {

        String apiContext = "apim611PublisherTestAPI";
        String apiDescription = "This is Test API Created by API Manager Integration Test";
        String apiTags = "tag611-1, tag612-2";
        String docName = "APIM611PublisherTestHowTo-Inline-summary";
        String docType = "How To";
        String sourceType = "Inline";

        APIRequest apiRequest = new APIRequest(apiName, apiContext, new URL(apiEndPointUrl));

        apiRequest.setVersion(apiVersion);
        apiRequest.setProvider(apiProvider);
        apiRequest.setTiersCollection(APIMIntegrationConstants.API_TIER.UNLIMITED);
        apiRequest.setTier(APIMIntegrationConstants.API_TIER.UNLIMITED);
        apiRequest.setTags(apiTags);
        apiRequest.setDescription(apiDescription);
        apiRequest.setBusinessOwner("api611b");
        apiRequest.setBusinessOwnerEmail("api611b@ee.com");
        apiRequest.setTechnicalOwner("api611t");
        apiRequest.setTechnicalOwnerEmail("api611t@ww.com");

        HttpResponse apiResponse = restAPIPublisher.addAPI(apiRequest);
        apiId = apiResponse.getData();

        DocumentDTO documentDTO = new DocumentDTO();
        documentDTO.setSourceType(DocumentDTO.SourceTypeEnum.INLINE);
        documentDTO.setName(docName);
        documentDTO.setSummary("Testing");
        documentDTO.setType(DocumentDTO.TypeEnum.HOWTO);
        documentDTO.setVisibility(DocumentDTO.VisibilityEnum.API_LEVEL);

        HttpResponse response1 = restAPIPublisher.addDocument(apiId, documentDTO);
        String documentId = response1.getData();

        DocumentListDTO documentListDTO = restAPIPublisher.getDocuments(apiId);
        DocumentDTO documentOne = documentListDTO.getList().get(0);

        assertEquals(documentId, documentOne.getDocumentId(), "Document addition failed.");
    }

    @Test(groups = {"wso2.am"}, description = "Add Documentation To An API With Type HowTo And" +
            " Source Url through the publisher rest API ",
            dependsOnMethods = "testAddDocumentToAnAPIHowToInline")
    public void testAddDocumentToAnAPIHowToUrl() throws Exception {

        String docName = "APIM612PublisherTestHowTo-Url-summary";
        String docUrl = "https://docs.wso2.com/display/AM191/Published+APIs";


        DocumentDTO documentDTO = new DocumentDTO();
        documentDTO.setSourceType(DocumentDTO.SourceTypeEnum.URL);
        documentDTO.setName(docName);
        documentDTO.setSummary("Testing");
        documentDTO.setType(DocumentDTO.TypeEnum.HOWTO);
        documentDTO.setSourceUrl(docUrl);
        documentDTO.setVisibility(DocumentDTO.VisibilityEnum.API_LEVEL);

        HttpResponse response1 = restAPIPublisher.addDocument(apiId, documentDTO);
        String documentId = response1.getData();

        DocumentListDTO documentListDTO = restAPIPublisher.getDocuments(apiId);
        boolean available = false;
        for (DocumentDTO documentDTO1 : documentListDTO.getList()){
            if (documentId.equals(documentDTO1.getDocumentId())){
                available = true;
                break;
            }
        }
        assertTrue(available, "Document addition failed.");
    }


    @AfterClass(alwaysRun = true)
    public void destroyAPIs() throws Exception {
        restAPIPublisher.deleteAPI(apiId);
    }
}
