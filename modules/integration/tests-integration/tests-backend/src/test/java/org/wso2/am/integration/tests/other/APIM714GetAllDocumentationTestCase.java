/*
 *
 * Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.am.integration.tests.other;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.testng.annotations.*;
import org.wso2.am.integration.clients.publisher.api.v1.dto.DocumentDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.DocumentListDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationDTO;
import org.wso2.am.integration.test.utils.base.APIMIntegrationBaseTest;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.bean.*;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import java.net.URL;
import java.util.ArrayList;

import static org.testng.Assert.assertEquals;


public class APIM714GetAllDocumentationTestCase extends APIMIntegrationBaseTest {
    private static final Log log = LogFactory.getLog(APIM714GetAllDocumentationTestCase.class);
    private APIRequest apiRequest;
    private final String API_END_POINT_POSTFIX_URL = "jaxrs_basic/services/customers/customerservice/";
    private String apiEndPointUrl;
    private String providerName;
    private final String API_DESCRIPTION = "This is test API create by API manager integration test";
    private static final String apiName = "DocumentTestAPI";
    private static final String apiVersion = "1.0.0";
    private final String applicationName = "NewApplication";
    private static final String apiContext = "documenttestapi";
    ArrayList<String> apis = new ArrayList();
    ArrayList<String> applications = new ArrayList();
    String documentId = null;
    DocumentDTO documentDTO = new DocumentDTO();

    @Factory(dataProvider = "userModeDataProvider")
    public APIM714GetAllDocumentationTestCase(TestUserMode userMode) {
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
        log.info("Test Starting user mode:" + userMode);

        providerName = publisherContext.getContextTenant().getContextUser().getUserName();
        apiEndPointUrl = backEndServerUrl.getWebAppURLHttp() + API_END_POINT_POSTFIX_URL;
        apiRequest = new APIRequest(apiName, apiContext, new URL(apiEndPointUrl));
        apiRequest.setVersion(apiVersion);
        apiRequest.setTiersCollection(APIMIntegrationConstants.API_TIER.UNLIMITED);
        apiRequest.setTier(APIMIntegrationConstants.API_TIER.UNLIMITED);
        apiRequest.setProvider(providerName);
        apiRequest.setDescription(API_DESCRIPTION);
        HttpResponse createResponse = restAPIPublisher.addAPI(apiRequest);

        assertEquals(createResponse.getResponseCode(), 201, "Error in API Creation");
        apis.add(0, createResponse.getData());
        //publish API
        restAPIPublisher
                .changeAPILifeCycleStatus(createResponse.getData(), APILifeCycleAction.PUBLISH.getAction(),
                        null);

        HttpResponse applicationResponse = restAPIStore.createApplication(applicationName,
                "app1", APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED,
                ApplicationDTO.TokenTypeEnum.OAUTH);
        applications.add(0, applicationResponse.getData());

        restAPIStore.createSubscription(createResponse.getData(),
                applicationResponse.getData(), "Unlimited");

        // add inline documentation to the API

        DocumentDTO.VisibilityEnum docVisibility = DocumentDTO.VisibilityEnum.API_LEVEL;
        DocumentDTO.SourceTypeEnum sourceType = DocumentDTO.SourceTypeEnum.INLINE;
        String summary = "This is a sample documentation";
        String name = "Introduction to PhoneVerification API";
        DocumentDTO.TypeEnum type = DocumentDTO.TypeEnum.HOWTO;

        documentDTO.setName(name);
        documentDTO.setType(type);
        documentDTO.setSourceType(sourceType);
        documentDTO.setSummary(summary);
        documentDTO.setVisibility(docVisibility);

        HttpResponse documentationResponse = restAPIPublisher.addDocument(createResponse.getData(), documentDTO);
        assertEquals(documentationResponse.getResponseCode(), 200,
                "Error while add documentation to API");
        documentId = documentationResponse.getData();

    }


    @Test(groups = "webapp", description = "Update Document content")
    public void updateDocumentationContent() throws Exception {
        HttpResponse updateContentResponse = restAPIPublisher.addContentDocument(apis.get(0),documentId,
                "updated documentation content");
        assertEquals(updateContentResponse.getResponseCode(), 200 ,
                "Error while update documentation content");
    }

    @Test(groups = "webapp", description = "Update Document")
    public void updateDocument() throws Exception {
        documentDTO.setType(DocumentDTO.TypeEnum.SAMPLES);
        HttpResponse updateDocumentResponse = restAPIPublisher.updateDocument(apis.get(0), documentId, documentDTO);
        assertEquals(updateDocumentResponse.getResponseCode(), 200, "Error while update the documents");
    }

    @Test(groups = "webapp", description = "Get All Documents")
    public void testAllDocuments() throws Exception {
        DocumentListDTO documentListDTO = restAPIPublisher.getDocuments(apis.get(0));
        assertEquals(1, documentListDTO.getList().size(),
                "Error while getting documentations of API");
    }


    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
        restAPIStore.deleteApplication(applications.get(0));
        //Remove documentation
        HttpResponse deleteResponse = restAPIPublisher.deleteDocument(apis.get(0), documentId);
        assertEquals(deleteResponse.getResponseCode(), 200, "Error while delete the document");
        restAPIPublisher.deleteAPI(apis.get(0));
        super.cleanUp();
    }
}
