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
package org.wso2.am.scenario.tests.rest.api.creation;

import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.am.integration.test.utils.APIManagerIntegrationTestException;
import org.wso2.am.scenario.test.common.APIPublisherRestClient;
import org.wso2.am.scenario.test.common.APIRequest;
import org.wso2.am.scenario.test.common.ScenarioTestBase;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import java.util.Properties;

public class RESTApiCreationTestCase extends ScenarioTestBase {

    private APIPublisherRestClient apiPublisher;
    private String publisherURLHttp;
    private APIRequest apiRequest;
    private Properties infraProperties;

    private String apiName = "PhoneVerification";
    private String apiContext = "/phoneverify";
    private String apiVersion = "1.0.0";
    private String apiResource = "/find";
    private String apiVisibility = "public";

    @BeforeClass(alwaysRun = true)
    public void init() throws APIManagerIntegrationTestException {

        infraProperties = getDeploymentProperties();
        String authority = infraProperties.getProperty(CARBON_SERVER_URL);
        if (authority != null && authority.contains("/")) {
            authority = authority.split("/")[2];
        } else if (authority == null) {
            authority = "localhost";
        }
        publisherURLHttp = "http://" + authority + ":9763/";

        setKeyStoreProperties();
        apiPublisher = new APIPublisherRestClient(publisherURLHttp);
        apiPublisher.login("admin", "admin");
    }

    @Test(description = "1.1.1.1")
    public void testRESTAPICreationWithMandatoryValues() throws Exception {


        apiRequest = new APIRequest(apiName, apiContext, apiVisibility, apiVersion, apiResource);

        //Design API with name,context,version,visibility and apiResource
        HttpResponse serviceResponse = apiPublisher.designAPI(apiRequest);
        verifyResponse(serviceResponse);
    }

    @Test(description = "1.1.1.2")
    public void testRESTAPICreationWithNumericName() throws Exception{
        apiName = "123567890";

        apiRequest = new APIRequest(apiName, apiContext, apiVisibility, apiVersion, apiResource);

        //Try to design API with numeric characters in api name
        HttpResponse serviceResponse = apiPublisher.designAPI(apiRequest);
        verifyResponse(serviceResponse);

    }

    @Test(description = "1.1.1.3")
    public void testRESTAPICreationWithNonEnglishName() throws Exception{
        apiName = "电话验证";

        apiRequest = new APIRequest(apiName, apiContext, apiVisibility, apiVersion, apiResource);

        //Try to design API with chinese api name
        HttpResponse serviceResponse = apiPublisher.designAPI(apiRequest);
        verifyResponse(serviceResponse);

    }

    @Test(description = "1.1.1.4")
    public void testRESTAPICreationWithUnderscoreName() throws Exception{
        apiName = "Phone_verification_api";

        apiRequest = new APIRequest(apiName, apiContext, apiVisibility, apiVersion, apiResource);

        //Try to design API with including underscore characters in api name
        HttpResponse serviceResponse = apiPublisher.designAPI(apiRequest);
        verifyResponse(serviceResponse);

    }

    @AfterTest(alwaysRun = true)
    public void destroy() throws Exception {
        HttpResponse serviceResponse = apiPublisher.deleteAPI(apiName, apiVersion, "admin");
        verifyResponse(serviceResponse);
    }
}
