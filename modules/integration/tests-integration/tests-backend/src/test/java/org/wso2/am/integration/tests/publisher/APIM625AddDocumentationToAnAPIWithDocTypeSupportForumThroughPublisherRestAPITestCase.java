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
import org.wso2.am.integration.test.utils.base.APIMIntegrationBaseTest;
import org.wso2.am.integration.test.utils.bean.APICreationRequestBean;
import org.wso2.am.integration.test.utils.bean.APILifeCycleState;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;
import org.wso2.carbon.utils.multitenancy.MultitenantConstants;

import javax.ws.rs.core.Response;
import java.net.URL;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/**
 * Add Documentation To An API using  Doc Type support Forum with different source types
 * Through Publisher Rest API
 * APIM2 -625
 */
public class APIM625AddDocumentationToAnAPIWithDocTypeSupportForumThroughPublisherRestAPITestCase
        extends APIMIntegrationBaseTest {

    private final String apiName = "APIM625PublisherTest";
    private final String apiVersion = "1.0.0";
    private final String summary = "This is documentation summary";
    private String apiProvider;
    private String apiEndPointUrl;
    private String apiId;
    private String supportTypeDocumentId;
    private String apiContext = "apim625PublisherTestAPI";
    private String apiDescription = "This is Test API Created by API Manager Integration Test";
    private String apiTags = "tag625-1, tag625-2";
    private String docName = "APIM625PublisherTestHowTo-Inline-summary";
    private String docUrl = "https://docs.wso2.com/display/AM191/Published+APIs";

    @Factory(dataProvider = "userModeDataProvider")
    public APIM625AddDocumentationToAnAPIWithDocTypeSupportForumThroughPublisherRestAPITestCase
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
        apiProvider = publisherContext.getContextTenant().getContextUser().getUserName();
    }

    @Test(groups = {"wso2.am"}, description = "Add Documentation To An API With Type  support forum And" +
            " Source Url through the publisher rest API ")
    public void testAddDocumentToAnAPISupportInline() throws Exception {
        //Create an API
        APICreationRequestBean apiCreationRequestBean = new APICreationRequestBean(apiName, apiContext, apiVersion,
                apiProvider, new URL(apiEndPointUrl));
        apiCreationRequestBean.setTags(apiTags);
        apiCreationRequestBean.setDescription(apiDescription);
        apiCreationRequestBean.setTiersCollection("Gold,Bronze");
        apiCreationRequestBean.setDefaultVersion("default_version");
        apiCreationRequestBean.setDefaultVersionChecked("default_version");
        apiCreationRequestBean.setBizOwner("api625b");
        apiCreationRequestBean.setBizOwnerMail("api625b@ee.com");
        apiCreationRequestBean.setTechOwner("api625t");
        apiCreationRequestBean.setTechOwnerMail("api625t@ww.com");

        APIDTO apiCreationResponse = restAPIPublisher.addAPI(apiCreationRequestBean);
        String status = apiCreationResponse.getLifeCycleStatus();
        apiId = apiCreationResponse.getId();
        assertTrue(APILifeCycleState.CREATED.getState().equalsIgnoreCase(status), "Status of the " + apiName +
                "is not a valid status");
        //Add Documentation to an API "APIM625PublisherTest" -  support forum | Url
        DocumentDTO documentDTO = new DocumentDTO();
        documentDTO.setName(docName);
        documentDTO.setType(DocumentDTO.TypeEnum.SUPPORT_FORUM);
        documentDTO.setSourceType(DocumentDTO.SourceTypeEnum.URL);
        documentDTO.setSummary(summary);
        documentDTO.setVisibility(DocumentDTO.VisibilityEnum.API_LEVEL);
        documentDTO.setSourceUrl(docUrl);
        HttpResponse documentationResponse = restAPIPublisher.addDocument(apiId, documentDTO);
        assertEquals(documentationResponse.getResponseCode(), 200, "Error while add documentation to API");
        supportTypeDocumentId = documentationResponse.getData();
        HttpResponse docRemoveResponse = restAPIPublisher.removeDocumentation(apiId, supportTypeDocumentId);
        assertEquals(Response.Status.OK.getStatusCode(), docRemoveResponse.getResponseCode(), "Error when removing" +
                "documentation");
    }

    @AfterClass(alwaysRun = true)
    public void destroyAPIs() throws Exception {
        restAPIPublisher.deleteAPI(apiId);
    }
}
