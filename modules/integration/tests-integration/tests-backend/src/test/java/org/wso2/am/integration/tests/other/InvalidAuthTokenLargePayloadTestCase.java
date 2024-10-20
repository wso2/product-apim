/*
* Copyright (c) 2016, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
* KIND, either express or implied. See the License for the
* specific language governing permissions and limitations
* under the License.
*
*/
package org.wso2.am.integration.tests.other;

import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.testng.Assert;
import org.testng.annotations.*;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIOperationsDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationDTO;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.bean.*;
import org.wso2.am.integration.tests.api.lifecycle.APIManagerLifecycleBaseTest;
import org.wso2.am.integration.tests.restapi.RESTAPITestConstants;
import org.wso2.carbon.automation.engine.annotations.ExecutionEnvironment;
import org.wso2.carbon.automation.engine.annotations.SetEnvironment;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.test.utils.common.TestConfigurationProvider;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;

import static org.testng.Assert.assertEquals;

/**
 * This test case is used test the unauthorised response when large payload is sent
 */
@SetEnvironment(executionEnvironments = { ExecutionEnvironment.ALL })
public class InvalidAuthTokenLargePayloadTestCase extends APIManagerLifecycleBaseTest {
    private final Log log = LogFactory.getLog(InvalidAuthTokenLargePayloadTestCase.class);
    private final String API_NAME = "InvalidAuthTokenLargePayloadAPIName";
    private final String API_CONTEXT = "InvalidAuthTokenLargePayloadContext";
    private final String DESCRIPTION = "This is test API create by API manager integration test";
    private final String API_VERSION = "1.0.0";
    private final String APP_NAME = "InvalidAuthTokenLargePayloadApp";
    private APICreationRequestBean apiCreationRequestBean;
    private List<APIResourceBean> resList;
    private String endpointUrl;
    private Map<String, String> requestHeaders = new HashMap<String, String>();
    private String tierCollection;
    private String testFile1KBFilePath;
    private String testFile100KBFilePath;
    private String testFile1MBFilePath;
    private String apiId;
    private String applicationId;

    @Factory(dataProvider = "userModeDataProvider")
    public InvalidAuthTokenLargePayloadTestCase(TestUserMode userMode) {
        this.userMode = userMode;
    }

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {
        super.init(userMode);
        String providerName = user.getUserName();

        publisherURLHttp = getPublisherURLHttp();
        storeURLHttp = getStoreURLHttp();
        endpointUrl = backEndServerUrl.getWebAppURLHttp() + "am/sample/calculator/v1/api";
        tierCollection = APIMIntegrationConstants.API_TIER.BRONZE + "," + APIMIntegrationConstants.API_TIER.GOLD + ","
                + APIMIntegrationConstants.API_TIER.SILVER + "," + APIMIntegrationConstants.API_TIER.UNLIMITED;
        String testArtifactPath = TestConfigurationProvider.getResourceLocation() + File.separator + "artifacts" +
                File.separator + "AM" + File.separator + "testFiles";
        testFile1KBFilePath = testArtifactPath + File.separator + "test1kb.db";
        testFile100KBFilePath = testArtifactPath + File.separator + "test100kb.db";
        testFile1MBFilePath = testArtifactPath + File.separator + "test1Mb.db";

        //Create application
        HttpResponse applicationResponse = restAPIStore.createApplication(APP_NAME,
                "Test Application", APIThrottlingTier.UNLIMITED.getState(),
                ApplicationDTO.TokenTypeEnum.JWT);
        assertEquals(applicationResponse.getResponseCode(), HttpStatus.SC_OK, "Response code is not as expected");

        applicationId = applicationResponse.getData();

        apiCreationRequestBean = new APICreationRequestBean(API_NAME, API_CONTEXT, API_VERSION, providerName,
                new URL(endpointUrl));
        apiCreationRequestBean.setDescription(DESCRIPTION);
        apiCreationRequestBean.setTiersCollection(tierCollection);

        //define resources
        resList = new ArrayList<APIResourceBean>();
        APIResourceBean resource = new APIResourceBean("POST",
                APIMIntegrationConstants.ResourceAuthTypes.APPLICATION_AND_APPLICATION_USER.getAuthType(),
                APIMIntegrationConstants.RESOURCE_TIER.TWENTYK_PER_MIN, "/post");

        resList.add(resource);
        apiCreationRequestBean.setResourceBeanList(resList);

        //
        List<APIOperationsDTO> apiOperationsDTOS = new ArrayList<>();
        APIOperationsDTO apiOperationsDTO = new APIOperationsDTO();
        apiOperationsDTO.setVerb(RESTAPITestConstants.POST_METHOD);
        apiOperationsDTO
                .setAuthType(APIMIntegrationConstants.ResourceAuthTypes.APPLICATION_AND_APPLICATION_USER.getAuthType());
        apiOperationsDTO.setThrottlingPolicy(APIMIntegrationConstants.RESOURCE_TIER.TWENTYK_PER_MIN);
        apiOperationsDTO.setTarget("/post");
        apiOperationsDTOS.add(apiOperationsDTO);

        APIRequest apiRequest = new APIRequest(API_NAME, API_CONTEXT, new URL(endpointUrl));

        apiRequest.setVersion(API_VERSION);
        apiRequest.setProvider(providerName);
        apiRequest.setTiersCollection(tierCollection);
        apiRequest.setOperationsDTOS(apiOperationsDTOS);
        apiRequest.setDescription(DESCRIPTION);

        apiId = createPublishAndSubscribeToAPIUsingRest(apiRequest, restAPIPublisher, restAPIStore, applicationId,
                APIMIntegrationConstants.API_TIER.GOLD);

    }


    @Test(groups = { "wso2.am" }, description = "Subscribe and invoke api")
    public void testApiInvocation() throws Exception {

        //invoke api
        requestHeaders.put(APIMIntegrationConstants.AUTHORIZATION_HEADER, "Bearer invalid_token_key");
        requestHeaders.put("Content-Type", ContentType.APPLICATION_JSON.toString());
        String invokeURL = getAPIInvocationURLHttp(API_CONTEXT, API_VERSION) + "/post";

        HttpResponse response;
        //first test for small payload
        try {
            response = uploadFile(invokeURL, new File(testFile1KBFilePath), requestHeaders);
            Assert.fail("Resource cannot be access with wrong access token");
        } catch (IOException e) {
            Assert.assertTrue(e.getMessage().contains(String.valueOf(HttpStatus.SC_UNAUTHORIZED)));
        }

        //test for medium payload
        try {
            response = uploadFile(invokeURL, new File(testFile100KBFilePath), requestHeaders);
            Assert.fail("Resource cannot be access with wrong access token");
        } catch (IOException e) {
            Assert.assertTrue(e.getMessage().contains(String.valueOf(HttpStatus.SC_UNAUTHORIZED)));
        }

        //test for large payload
        try {
            response = uploadFile(invokeURL, new File(testFile1MBFilePath), requestHeaders);
            Assert.fail("Resource cannot be access with wrong access token");
        } catch (IOException e) {
            Assert.assertTrue(e.getMessage().contains(String.valueOf(HttpStatus.SC_UNAUTHORIZED)));
        }
    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
        restAPIStore.deleteApplication(applicationId);
        undeployAndDeleteAPIRevisionsUsingRest(apiId, restAPIPublisher);
        restAPIPublisher.deleteAPI(apiId);
    }

    @DataProvider
    public static Object[][] userModeDataProvider() {
        return new Object[][] { new Object[] { TestUserMode.SUPER_TENANT_ADMIN },
                new Object[] { TestUserMode.TENANT_ADMIN }, };
    }

    /**
     * Upload a file to the given URL
     *
     * @param endpointUrl URL to be file upload
     * @param fileName    Name of the file to be upload
     * @throws IOException throws if connection issues occurred
     */
    private HttpResponse uploadFile(String endpointUrl, File fileName, Map<String, String> headers) throws IOException {
        //open import API url connection and deploy the exported API
        URL url = new URL(endpointUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setDoOutput(true);
        connection.setRequestMethod("POST");

        FileBody fileBody = new FileBody(fileName);
        MultipartEntity multipartEntity = new MultipartEntity(HttpMultipartMode.STRICT);
        multipartEntity.addPart("file", fileBody);

        connection.setRequestProperty("Content-Type", multipartEntity.getContentType().getValue());
        //setting headers
        if (headers != null && headers.size() > 0) {
            Iterator<String> itr = headers.keySet().iterator();
            while (itr.hasNext()) {
                String key = itr.next();
                if (key != null) {
                    connection.setRequestProperty(key, headers.get(key));
                }
            }
            for (String key : headers.keySet()) {
                connection.setRequestProperty(key, headers.get(key));
            }
        }

        OutputStream out = connection.getOutputStream();
        try {
            multipartEntity.writeTo(out);
        } finally {
            out.close();
        }
        int status = connection.getResponseCode();
        BufferedReader read = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        String temp;
        StringBuilder responseMsg = new StringBuilder();
        while ((temp = read.readLine()) != null) {
            responseMsg.append(temp);
        }
        HttpResponse response = new HttpResponse(responseMsg.toString(), status);
        return response;
    }

}
