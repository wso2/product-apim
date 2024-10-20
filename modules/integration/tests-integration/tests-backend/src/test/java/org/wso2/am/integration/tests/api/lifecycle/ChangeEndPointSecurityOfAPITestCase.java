/*
 * Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.am.integration.tests.api.lifecycle;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyGenerateRequestDTO;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.bean.APICreationRequestBean;
import org.wso2.am.integration.test.utils.http.HTTPSClientUtils;
import org.wso2.carbon.apimgt.api.model.APIIdentifier;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import javax.xml.bind.DatatypeConverter;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/**
 * Change the endpoint security of APi and invoke. Endpoint application was developed to return thr security token in
 * the response body.
 */
public class ChangeEndPointSecurityOfAPITestCase extends APIManagerLifecycleBaseTest {

    private static final Log log = LogFactory.getLog(ChangeEndPointSecurityOfAPITestCase.class);
    private final String API_NAME = "ChangeEndPointSecurityOfAPITest";
    private final String API_CONTEXT = "ChangeEndPointSecurityOfAPI";
    private final String API_TAGS = "security, username, password";
    private final String API_END_POINT_POSTFIX_URL = "jaxrs_basic/services/customers/customerservice/";
    private final String API_DESCRIPTION = "This is test API create by API manager integration test";
    private final String API_VERSION_1_0_0 = "1.0.0";
    private final String APPLICATION_NAME = "ChangeEndPointSecurityOfAPI";
    private HashMap<String, String> requestHeadersGet;
    private String providerName;
    private String apiEndPointUrl;
    private String applicationID;
    private String apiID;

    @Factory(dataProvider = "userModeDataProvider")
    public ChangeEndPointSecurityOfAPITestCase(TestUserMode userMode) {

        this.userMode = userMode;
    }

    @DataProvider
    public static Object[][] userModeDataProvider() {

        return new Object[][]{
                new Object[]{TestUserMode.SUPER_TENANT_ADMIN},
        };
    }

    @BeforeClass(alwaysRun = true)
    public void initialize() throws Exception {

        super.init();
        apiEndPointUrl = backEndServerUrl.getWebAppURLHttp() + API_END_POINT_POSTFIX_URL;
        providerName = user.getUserName();
        requestHeadersGet = new HashMap<String, String>();
        requestHeadersGet.put("accept", "text/plain");
        requestHeadersGet.put("Content-Type", "text/plain");
        //Create application
        ApplicationDTO dto = restAPIStore.addApplication(APPLICATION_NAME,
                APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED, "", "");
        applicationID = dto.getApplicationId();
    }

    @Test(groups = {"wso2.am"}, description = "Test the API with endpoint security enabled with simple password" +
            " that only has characters and numbers")
    public void testInvokeGETResourceWithSecuredEndPointPasswordOnlyNumbersAndLetters() throws Exception {

        String endpointUsername = "admin1";
        String endpointPassword = "admin123";
        byte[] userNamePasswordByteArray = (endpointUsername + ":" + endpointPassword).getBytes();
        String encodedUserNamePassword = DatatypeConverter.printBase64Binary(userNamePasswordByteArray);

        APICreationRequestBean apiCreationRequestBean =
                new APICreationRequestBean(API_NAME, API_CONTEXT, API_VERSION_1_0_0, providerName,
                        new URL(apiEndPointUrl));
        apiCreationRequestBean.setTags(API_TAGS);
        apiCreationRequestBean.setDescription(API_DESCRIPTION);
        apiCreationRequestBean.setEndpointType("basic");
        apiCreationRequestBean.setEpUsername(endpointUsername);
        apiCreationRequestBean.setEpPassword(endpointPassword);
        apiCreationRequestBean.setTier(TIER_UNLIMITED);
        apiCreationRequestBean.setTiersCollection(TIER_UNLIMITED);
        APIIdentifier apiIdentifier = new APIIdentifier(providerName, API_NAME, API_VERSION_1_0_0);
        apiIdentifier.setTier(TIER_UNLIMITED);
        APIDTO apidto =
                createPublishAndSubscribeToAPI(apiIdentifier, apiCreationRequestBean, restAPIPublisher, restAPIStore,
                        applicationID, TIER_UNLIMITED);
        apiID = apidto.getId();
        // Create Revision and Deploy to Gateway
        createAPIRevisionAndDeployUsingRest(apiID, restAPIPublisher);
        waitForAPIDeploymentSync(user.getUserName(), API_NAME, API_VERSION_1_0_0,
                APIMIntegrationConstants.IS_API_EXISTS);

        ArrayList<String> grantTypes = new ArrayList<>();
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.CLIENT_CREDENTIAL);
        ApplicationKeyDTO applicationKeyDTO = restAPIStore
                .generateKeys(applicationID, "3600", null, ApplicationKeyGenerateRequestDTO.KeyTypeEnum.PRODUCTION,
                        null, grantTypes);

        requestHeadersGet.put("Authorization", "Bearer " + applicationKeyDTO.getToken().getAccessToken());
        HttpResponse httpResponseGet =
                HTTPSClientUtils.doGet(getAPIInvocationURLHttp(API_CONTEXT, API_VERSION_1_0_0) + "/sec",
                        requestHeadersGet);
        assertEquals(httpResponseGet.getResponseCode(), HTTP_RESPONSE_CODE_OK, "Invocation fails for GET request for " +
                "endpoint type secured. username:" + endpointUsername + " password:" + String.valueOf(endpointPassword));
        assertTrue(httpResponseGet.getData().contains(encodedUserNamePassword), "Response Data not match for GET" +
                " request for endpoint type secured. Expected value :" + encodedUserNamePassword + " not contains in " +
                "response data:" + httpResponseGet.getData() + "username:" + endpointUsername + " password:" +
                endpointPassword);

    }

    @Test(groups = {"wso2.am"}, description = "Test the API with endpoint security" +
            " enabled with complex password",
            dependsOnMethods = "testInvokeGETResourceWithSecuredEndPointPasswordOnlyNumbersAndLetters")
    public void testInvokeGETResourceWithSecuredEndPointComplexPassword()
            throws Exception {

        String[] symbolicCharacter = {"!", "@", "#", "$", "%", "^", "&", "*", "(", ")", "_", "-", "+", "=", "{", "[",
                "}", "]", "|", ":", ";", "'", "<", ",", ">", ".", "?", "/"};
        APIDTO apidto = restAPIPublisher.getAPIByID(apiID, user.getUserDomain());

        for (int i = 0; i < symbolicCharacter.length; i++) {
            String endpointUsername = "user";
            String endpointPassword = "abcd" + symbolicCharacter[i] + "efghijk";
            byte[] userNamePasswordByteArray = (endpointUsername + ":" + endpointPassword).getBytes();
            String encodedUserNamePassword = DatatypeConverter.printBase64Binary(userNamePasswordByteArray);
            String endpointSecurity = "{\n" +
                    "  \"production\":{\n" +
                    "    \"enabled\":true,\n" +
                    "    \"type\":\"BASIC\",\n" +
                    "    \"username\":\"" + endpointUsername + "\",\n" +
                    "    \"password\":\"" + endpointPassword + "\"\n" +
                    "  },\n" +
                    "  \"sandbox\":{\n" +
                    "    \"enabled\":true,\n" +
                    "    \"type\":\"BASIC\",\n" +
                    "    \"username\":\"" + endpointUsername + "\",\n" +
                    "    \"password\":\"" + endpointPassword + "\"\n" +
                    "  }\n" +
                    "  }";
            Object endpointConfig = apidto.getEndpointConfig();
            JSONObject endpointConfigJson = new JSONObject();
            endpointConfigJson.putAll((Map) endpointConfig);
            endpointConfigJson.put("endpoint_security", new JSONParser().parse(endpointSecurity));
            apidto.setEndpointConfig(endpointConfigJson);            //Update API with Edited information
            restAPIPublisher.updateAPI(apidto);

            // Undeploy and Delete existing API Revisions Since it has reached 5 max revision limit
            undeployAndDeleteAPIRevisionsUsingRest(apiID, restAPIPublisher);
            waitForAPIDeployment();

            // Create Revision and Deploy to Gateway
            createAPIRevisionAndDeployUsingRest(apiID, restAPIPublisher);

            //Send GET request
            waitForAPIDeployment();

            int retries = 3;
            for (int j = 0; j <= retries; j++) {
                HttpResponse httpResponseGet = HTTPSClientUtils.doGet(
                        getAPIInvocationURLHttp(API_CONTEXT, API_VERSION_1_0_0) + "/sec", requestHeadersGet);
                assertEquals(httpResponseGet.getResponseCode(), HTTP_RESPONSE_CODE_OK,
                        "Invocation fails for GET request for endpoint type secured. username:"
                                + endpointUsername + " password:" + String.valueOf(endpointPassword));
                if (httpResponseGet.getData().contains(encodedUserNamePassword)) {
                    Assert.assertTrue(true);
                    break;
                } else {
                    if (j == retries) {
                        log.error("Max retry count reached!!!");
                        Assert.fail("Response Data not match for GET request for endpoint type secured. " +
                                "Expected value : " + encodedUserNamePassword + " not contains in " +
                                "response data: " + httpResponseGet.getData() + " username:" + endpointUsername
                                + " password:" + String.valueOf(endpointPassword));
                    } else {
                        log.warn("[Warning] Response Data not match for GET request for endpoint type secured. " +
                                "Expected value : " + encodedUserNamePassword + " not contains in " +
                                "response data: " + httpResponseGet.getData() + " username:" + endpointUsername
                                + " password:" + String.valueOf(endpointPassword) + " Retrying...");
                        waitForAPIDeployment();
                    }
                }
            }
        }
    }

    @AfterClass(alwaysRun = true)
    public void cleanUpArtifacts() throws Exception {

        restAPIStore.removeApplicationById(applicationID);
        undeployAndDeleteAPIRevisionsUsingRest(apiID, restAPIPublisher);
        restAPIPublisher.deleteAPI(apiID);
    }
}
