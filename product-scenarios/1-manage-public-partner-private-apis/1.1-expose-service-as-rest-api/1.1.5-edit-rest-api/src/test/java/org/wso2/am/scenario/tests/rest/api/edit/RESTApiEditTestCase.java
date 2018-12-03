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

import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.am.integration.test.utils.APIManagerIntegrationTestException;
import org.wso2.am.integration.test.utils.bean.APICreationRequestBean;
import org.wso2.am.scenario.test.common.APIPublisherRestClient;
import org.wso2.am.scenario.test.common.ScenarioTestBase;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import java.net.URL;
import java.util.Properties;
import javax.ws.rs.core.Response;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class RESTApiEditTestCase extends ScenarioTestBase {

    private APIPublisherRestClient apiPublisher;
    private String publisherURL;
    private Properties infraProperties;

    private String apiName = "PhoneVerification";
    private String apiContext = "/phoneverify";
    private String apiVersion = "1.0.0";
    private String admin = "admin";
    private String description = "This is a API creation description";
    private String tag = "APICreationTag";
    private String tierCollection = "Gold,Bronze";
    private String bizOwner = "wso2Test";
    private String bizOwnerMail = "wso2test@gmail.com";
    private String techOwner = "wso2";
    private String techOwnerMail = "wso2@gmail.com";
    private String default_version_checked = "default_version";
    private String backendEndPoint = "http://ws.cdyne.com/phoneverify/phoneverify.asmx";

    @BeforeClass(alwaysRun = true)
    public void init() throws APIManagerIntegrationTestException {

        infraProperties = getDeploymentProperties();
        publisherURL = infraProperties.getProperty(PUBLISHER_URL);

        if (publisherURL == null) {
            publisherURL = "https://localhost:9443/publisher";
        }

        setKeyStoreProperties();
        apiPublisher = new APIPublisherRestClient(publisherURL);
        apiPublisher.login("admin", "admin");
    }

    @Test(description = "1.1.5.1")
    public void testRESTAPIEditAlreadyCreatedApi() throws Exception {
        //Create an API
        APICreationRequestBean apiCreationRequestBean =
                new APICreationRequestBean(apiName, apiContext, apiVersion, admin,
                        new URL(backendEndPoint));
        apiCreationRequestBean.setTags(tag);
        apiCreationRequestBean.setDescription(description);
        apiCreationRequestBean.setTiersCollection(tierCollection);
        apiCreationRequestBean.setDefaultVersion(default_version_checked);
        apiCreationRequestBean.setDefaultVersionChecked(default_version_checked);
        apiCreationRequestBean.setBizOwner(bizOwner);
        apiCreationRequestBean.setBizOwnerMail(bizOwnerMail);
        apiCreationRequestBean.setTechOwner(techOwner);
        apiCreationRequestBean.setTechOwnerMail(techOwnerMail);

        HttpResponse apiCreationResponse = apiPublisher.addAPI(apiCreationRequestBean);
        assertEquals(apiCreationResponse.getResponseCode(), Response.Status.OK.getStatusCode(),
                "Response Code miss matched when creating the API");
        verifyResponse(apiCreationResponse);

        //Check availability of the API in publisher
        HttpResponse apiResponsePublisher = apiPublisher.getAPI
                (apiName, admin, apiVersion);
        verifyResponse(apiResponsePublisher);
        assertTrue(apiResponsePublisher.getData().contains(apiName), apiName + " is not visible in publisher");

        //Update API with the description and tiersCollection & validate the result
        apiCreationRequestBean.setDescription("Description Changed");
        apiCreationRequestBean.setTiersCollection("Unlimited,Gold,Bronze");

        HttpResponse apiUpdateResponse = apiPublisher.updateAPI(apiCreationRequestBean);
        verifyResponse(apiUpdateResponse);

        //Check whether API is updated from the above request
        HttpResponse apiUpdateResponsePublisher = apiPublisher.getAPI
                (apiName, admin, apiVersion);
        assertTrue(apiUpdateResponsePublisher.getData().contains(apiName),
                apiName + " is not updated");
        assertTrue(apiUpdateResponsePublisher.getData().contains("Description Changed"),
                "Description of the " + apiName + " is not updated");
        assertTrue(apiUpdateResponsePublisher.getData().contains("Unlimited"),
                "Tier Collection of the " + apiName + " is not updated");
        assertTrue(apiUpdateResponsePublisher.getData().contains("Bronze"),
                "Tier Collection of the " + apiName + " is not updated");
        assertTrue(apiUpdateResponsePublisher.getData().contains("Gold"),
                "Tier Collection of the " + apiName + " is not updated");

    }
    @AfterTest(alwaysRun = true)
    public void destroy() throws Exception {
        HttpResponse serviceResponse = apiPublisher.deleteAPI(apiName, apiVersion, admin);
        verifyResponse(serviceResponse);
    }
}
