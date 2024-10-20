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
import org.wso2.am.integration.test.utils.bean.APICreationRequestBean;
import org.wso2.am.integration.test.utils.generic.TestConfigurationProvider;
import org.wso2.am.integration.tests.api.lifecycle.APIManagerLifecycleBaseTest;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.utils.multitenancy.MultitenantConstants;

import java.io.File;
import java.net.URL;

import static org.testng.Assert.assertEquals;

/**
 * Add Documentation To An API using source type File Through Publisher Rest API
 * APIM2 -614 / APIM2 -622 /APIM2 -624 /APIM2 -626 /APIM2 -629
 */
public class APIM614AddDocumentationToAnAPIWithDocTypeSampleAndSDKThroughPublisherRestAPITestCase
        extends APIManagerLifecycleBaseTest {

    private final String apiName = "APIM614PublisherTest";
    private final String apiVersion = "1.0.0";
    private final String docName = "APIM629PublisherTestHowTo-File-summary";
    private final String summary = "Testing";
    private String apiProvider;
    private String apiEndPointUrl;
    private String apiId;
    private String fileTypeDocumentId;
    private DocumentDTO documentDTO = new DocumentDTO();

    @Factory(dataProvider = "userModeDataProvider")
    public APIM614AddDocumentationToAnAPIWithDocTypeSampleAndSDKThroughPublisherRestAPITestCase
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
        apiProvider = user.getUserName();
    }

    @Test(groups = {"wso2.am"}, description = "Create an API to update the documents with " +
            "source type file  through the publisher rest API ")
    public void testApiCreation() throws Exception {

        String apiContext = "apim614PublisherTestAPI";
        String apiDescription = "This is Test API Created by API Manager Integration Test";
        String apiTags = "tag614-1, tag622-2, tag624-3";

        //Create an API and validate it
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

        APIDTO apiDto = createAndPublishAPI(apiCreationRequestBean, restAPIPublisher, false);
        apiId = apiDto.getId();

        documentDTO.setName(docName);
        documentDTO.setSummary(summary);
        documentDTO.setType(DocumentDTO.TypeEnum.HOWTO);
        documentDTO.setSourceType(DocumentDTO.SourceTypeEnum.FILE);
        documentDTO.setVisibility(DocumentDTO.VisibilityEnum.API_LEVEL);

        org.wso2.carbon.automation.test.utils.http.client.HttpResponse documentationResponse =
                restAPIPublisher.addDocument(apiId, documentDTO);
        assertEquals(documentationResponse.getResponseCode(), 200,
                "Error while adding file-based documentation to API");
        fileTypeDocumentId = documentationResponse.getData();
    }

    @Test(groups = {"wso2.am"}, description = "Add Documentation To An API With Type HowTo And" +
            " Source File through the publisher rest API ",
            dependsOnMethods = "testApiCreation")
    public void testAddDocumentToAnAPIHowToFile() throws Exception {

        String fileNameAPIM614 = "APIM614.txt";
        String docName = "APIM614PublisherTestHowTo-File-summary";
        String filePathAPIM614 = TestConfigurationProvider.getResourceLocation() + File.separator + "artifacts" +
                File.separator + "AM" + File.separator + "lifecycletest" + File.separator + fileNameAPIM614;
        File file = new File(filePathAPIM614);

        //Update Type and update document
        documentDTO.setType(DocumentDTO.TypeEnum.HOWTO);
        documentDTO.setName(docName);
        org.wso2.carbon.automation.test.utils.http.client.HttpResponse updateDocumentResponse =
                restAPIPublisher.updateDocument(apiId, fileTypeDocumentId, documentDTO);
        assertEquals(updateDocumentResponse.getResponseCode(), 200,
                "Error while updating the documents");

        //Update the document content
        org.wso2.carbon.automation.test.utils.http.client.HttpResponse documentationResponse =
                restAPIPublisher.updateContentDocument(apiId, fileTypeDocumentId, file);
        assertEquals(documentationResponse.getResponseCode(), 200,
                "Error while updating documentation to API");
}

    @Test(groups = {"wso2.am"}, description = "Add Documentation To An API With Type Sample SDK And" +
            " Source File through the publisher rest API ",
            dependsOnMethods = "testAddDocumentToAnAPIHowToFile")
    public void testAddDocumentToAnAPISDKToFile() throws Exception {

        String fileNameAPIM622 = "APIM622.txt";
        String docName = "APIM622PublisherTestHowTo-File-summary";
        String filePathAPIM622 = TestConfigurationProvider.getResourceLocation() + File.separator + "artifacts" +
                File.separator + "AM" + File.separator + "lifecycletest" + File.separator + fileNameAPIM622;
        File file = new File(filePathAPIM622);

        //Update Type and update document
        documentDTO.setType(DocumentDTO.TypeEnum.SAMPLES);
        documentDTO.setName(docName);
        org.wso2.carbon.automation.test.utils.http.client.HttpResponse updateDocumentResponse =
                restAPIPublisher.updateDocument(apiId, fileTypeDocumentId, documentDTO);
        assertEquals(updateDocumentResponse.getResponseCode(), 200,
                "Error while updating the documents");

        //Update the document content
        org.wso2.carbon.automation.test.utils.http.client.HttpResponse documentationResponse =
                restAPIPublisher.updateContentDocument(apiId, fileTypeDocumentId, file);
        assertEquals(documentationResponse.getResponseCode(), 200,
                "Error while updating documentation to API");
    }

    @Test(groups = {"wso2.am"}, description = "Add Documentation To An API With Type Other And" +
            " Source File through the publisher rest API ",
            dependsOnMethods = "testAddDocumentToAnAPISDKToFile")
    public void testAddDocumentToAnAPIOtherFile() throws Exception {

        String fileNameAPIM629 = "APIM629.txt";
        String docName = "APIM629PublisherTestHowTo-File-summary";

        String filePathAPIM629 = TestConfigurationProvider.getResourceLocation() + File.separator + "artifacts" +
                File.separator + "AM" + File.separator + "lifecycletest" + File.separator + fileNameAPIM629;
        File file = new File(filePathAPIM629);

        //Update Type and update document
        documentDTO.setType(DocumentDTO.TypeEnum.HOWTO);
        documentDTO.setName(docName);
        org.wso2.carbon.automation.test.utils.http.client.HttpResponse updateDocumentResponse =
                restAPIPublisher.updateDocument(apiId, fileTypeDocumentId, documentDTO);
        assertEquals(updateDocumentResponse.getResponseCode(), 200,
                "Error while updating the documents");

        //Update the document content
        org.wso2.carbon.automation.test.utils.http.client.HttpResponse documentationResponse =
                restAPIPublisher.updateContentDocument(apiId, fileTypeDocumentId, file);
        assertEquals(documentationResponse.getResponseCode(), 200,
                "Error while updating documentation to API");
    }

    @AfterClass(alwaysRun = true)
    public void destroyAPIs() throws Exception {
        restAPIPublisher.deleteAPI(apiId);
    }
}
