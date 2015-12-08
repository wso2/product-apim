/*
*Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
*WSO2 Inc. licenses this file to you under the Apache License,
*Version 2.0 (the "License"); you may not use this file except
*in compliance with the License.
*You may obtain a copy of the License at
*
*http://www.apache.org/licenses/LICENSE-2.0
*
*Unless required by applicable law or agreed to in writing,
*software distributed under the License is distributed on an
*"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
*KIND, either express or implied.  See the License for the
*specific language governing permissions and limitations
*under the License.
*/


package org.wso2.am.integration.tests.restapi.utils;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.cxf.jaxrs.impl.ResponseImpl;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;

import org.wso2.am.integration.test.utils.APIManagerIntegrationTestException;
import org.wso2.am.integration.test.utils.base.APIMIntegrationBaseTest;
import org.wso2.am.integration.tests.restapi.RESTAPITestConstants;
import org.wso2.carbon.automation.engine.annotations.ExecutionEnvironment;
import org.wso2.carbon.automation.engine.annotations.SetEnvironment;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.engine.exceptions.AutomationFrameworkException;
import org.wso2.carbon.automation.test.utils.http.client.HttpRequestUtil;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.ws.rs.core.Response;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

@SetEnvironment(executionEnvironments = {ExecutionEnvironment.STANDALONE})
public class RESTAPITestUtil extends APIMIntegrationBaseTest {

    private static final Log log = LogFactory.getLog(RESTAPITestUtil.class);
    private DataDrivenTestUtils dataDrivenTestUtils = new DataDrivenTestUtils();

    @Factory(dataProvider = "userModeDataProvider")
    public RESTAPITestUtil(TestUserMode userMode) {
        this.userMode = userMode;
    }

    @DataProvider
    public static Object[][] userModeDataProvider() {
        return new Object[][]{
                new Object[]{TestUserMode.SUPER_TENANT_ADMIN}
        };
    }

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {
        super.init(userMode);
    }

    @Test(groups = {"wso2.am"}, description = "REST API Implementation test")
    public void testRestAPI() {

        try {

            String configFilePath = "";
            String configData = getConfigurations(configFilePath);
            Map<String, String> dataMap = registerOAuthApplication();
            String accessToken;
            JSONObject testScenario = new JSONObject(configData);
            JSONArray array = testScenario.getJSONArray(RESTAPITestConstants.JSON_ROOT_ELEMENT);
            int arrayLength = array.length();
            HashMap<String, String> preservedAttributes = new HashMap<String, String>();

            //go through each section of the JSON string and fetch data
            for (int i = 0; i < arrayLength; i++) {

                JSONObject configObject = (JSONObject) array.get(i);
                JSONObject initializationConfigurationObject =
                        configObject.getJSONObject(RESTAPITestConstants.INITIALIZATION_SECTION);
                String scope = initializationConfigurationObject.get(RESTAPITestConstants.SCOPE_ELEMENT).toString();

                if (dataMap == null || dataMap.isEmpty()) {
                    //if the data map is null or empty, that means it has failed to register OAuth application
                    log.error("Failed to register OAuth application to test the REST API.");
                    assertTrue(false);
                }

                accessToken = generateOAuthAccessToken(scope, dataMap);

                if (accessToken == null) {
                    //if the access token is null, that means it has failed to generate OAuth access token
                    log.error("Failed to generate OAuth access token to test the REST API.");
                    assertTrue(false);
                }

                JSONObject dataConfigurationObject = configObject.getJSONObject(RESTAPITestConstants.DATA_SECTION);
                String method = dataConfigurationObject.get(RESTAPITestConstants.METHOD_ELEMENT).toString();
                String resourceUrl = getKeyManagerURLHttp() +
                        dataConfigurationObject.get(RESTAPITestConstants.URL_ELEMENT).toString();

                Pattern parameterPattern = Pattern.compile("\\{(.*?)\\}");
                Matcher matcher = parameterPattern.matcher(resourceUrl);

                while (matcher.find()) {
                    String parameterName = matcher.group(1);
                    String template = "{" + parameterName + "}";
                    resourceUrl = resourceUrl.replace(template, preservedAttributes.get(parameterName));
                }

                Map<String, String> queryParameters = new HashMap<String, String>();
                String queryParameterText =
                        dataConfigurationObject.get(RESTAPITestConstants.QUERY_PARAMETERS).toString();

                if (!queryParameterText.isEmpty()) {
                    Iterator queryParamIterator = dataConfigurationObject.getJSONObject
                            (RESTAPITestConstants.QUERY_PARAMETERS).keys();
                    while (queryParamIterator.hasNext()) {
                        String key = queryParamIterator.next().toString();
                        String value = dataConfigurationObject.getJSONObject
                                (RESTAPITestConstants.QUERY_PARAMETERS).getString(key);
                        queryParameters.put(key, value);
                    }
                }

                Map<String, String> requestHeaders = new HashMap<String, String>();
                String requestHeaderText = dataConfigurationObject.get(RESTAPITestConstants.REQUEST_HEADERS).toString();

                if (!requestHeaderText.isEmpty()) {
                    Iterator requestHeaderIterator = dataConfigurationObject.getJSONObject
                            (RESTAPITestConstants.REQUEST_HEADERS).keys();
                    while (requestHeaderIterator.hasNext()) {
                        String key = requestHeaderIterator.next().toString();
                        String value = dataConfigurationObject.
                                getJSONObject(RESTAPITestConstants.REQUEST_HEADERS).getString(key);

                        //set the Auth header according to the required manner (i.e - separated with a space)
                        if (RESTAPITestConstants.AUTHORIZATION_KEY.equalsIgnoreCase(key)) {
                            value = value.concat(" " + accessToken);
                        }
                        requestHeaders.put(key, value);
                    }
                }

                String requestPayload = dataConfigurationObject.get(RESTAPITestConstants.REQUEST_PAYLOAD).toString();
                String responseHeaderText =
                        dataConfigurationObject.get(RESTAPITestConstants.RESPONSE_HEADERS).toString();
                Map<String, String> responseHeaders = new HashMap<String, String>();

                if (!responseHeaderText.isEmpty()) {
                    Iterator responseHeaderIterator = dataConfigurationObject.getJSONObject
                            (RESTAPITestConstants.RESPONSE_HEADERS).keys();
                    while (responseHeaderIterator.hasNext()) {
                        String key = responseHeaderIterator.next().toString();
                        String value = dataConfigurationObject.getJSONObject
                                (RESTAPITestConstants.RESPONSE_HEADERS).getString(key);
                        responseHeaders.put(key, value);
                    }
                }

                String responsePayload = dataConfigurationObject.get(RESTAPITestConstants.RESPONSE_PAYLOAD).toString();
                String cookie = null;

                Response responseOfHttpCall = dataDrivenTestUtils.sendRequestToRESTAPI
                        (method, resourceUrl, queryParameters, requestHeaders, requestPayload, cookie);
                String outputText = ((ResponseImpl) responseOfHttpCall).readEntity(String.class);

                if (!configObject.isNull(RESTAPITestConstants.PRESERVE_LIST)) {
                    JSONArray preserveListArray = configObject.getJSONArray(RESTAPITestConstants.PRESERVE_LIST);
                    if (preserveListArray != null && preserveListArray.length() > 0) {
                        for (int j = 0; j < preserveListArray.length(); j++) {
                            String parameterName = preserveListArray.getJSONObject(j).
                                    getString(RESTAPITestConstants.PRESERVED_ATTRIBUTE_NAME);
                            String parameterValue = new JSONObject(outputText).get(preserveListArray.getJSONObject(j).
                                    getString(RESTAPITestConstants.RESPONSE_LOCATION)).toString();
                            preservedAttributes.put(parameterName, parameterValue);
                        }
                    }
                }

                int actualStatusCode = responseOfHttpCall.getStatus();
                int expectedStatusCode = dataConfigurationObject.getJSONObject(RESTAPITestConstants.RESPONSE_HEADERS).
                        getInt(RESTAPITestConstants.STATUS_CODE);

                assertEquals(actualStatusCode, expectedStatusCode);
            }

        } catch (APIManagerIntegrationTestException integrationTestException) {
            log.error("Error occurred in sending request to the REST API.", integrationTestException);
            assertTrue(false);

        } catch (JSONException e) {
            log.error("Error occurred in parsing the data in JSON file.", e);
            assertTrue(false);
        }

    }


    private String getConfigurations(String fileLocation) throws APIManagerIntegrationTestException {

        BufferedReader bufferedReader = null;
        try {
            bufferedReader = new BufferedReader(new FileReader(new File(fileLocation)));
            String line;
            StringBuilder stringBuilder = new StringBuilder();
            while ((line = bufferedReader.readLine()) != null) {
                stringBuilder.append(line);
            }
            return stringBuilder.toString();
        } catch (IOException ioE) {
            log.error("IOException when reading configuration data:" + fileLocation, ioE);
            throw new APIManagerIntegrationTestException("Error in reading data from config file.", ioE);
        } finally {
            if (bufferedReader != null) {
                try {
                    bufferedReader.close();
                } catch (IOException e) {
                    //we only log the exception because the required data has been fetched
                    log.warn("Error when closing the buffered reader which used to reed the file:" + fileLocation +
                            ". Error:" + e.getMessage());
                }
            }
        }
    }


    private String generateOAuthAccessToken(String scope, Map<String, String> dataMap)
            throws APIManagerIntegrationTestException {

        try {
            String consumeKey = dataMap.get(RESTAPITestConstants.CONSUMER_KEY);
            String consumerSecret = dataMap.get(RESTAPITestConstants.CONSUMER_SECRET);
            String messageBody = RESTAPITestConstants.OAUTH_MESSAGE_BODY + scope;
            URL tokenEndpointURL = new URL(getGatewayURLNhttp() + RESTAPITestConstants.TOKEN_ENDPOINT_SUFFIX);
            HashMap<String, String> accessKeyMap = new HashMap<String, String>();

            //concat consumeKey and consumerSecret and make the authenticationHeader to get access token
            String authenticationHeader = consumeKey + ":" + consumerSecret;
            byte[] encodedBytes = Base64.encodeBase64(authenticationHeader.getBytes("UTF-8"));
            accessKeyMap.put(RESTAPITestConstants.AUTHORIZATION_KEY, "Basic " + new String(encodedBytes, "UTF-8"));
            HttpResponse tokenGenerateResponse = HttpRequestUtil.doPost(tokenEndpointURL, messageBody, accessKeyMap);
            JSONObject tokenGenJsonObject = new JSONObject(tokenGenerateResponse);
            String accessToken = new JSONObject(tokenGenJsonObject.get(RESTAPITestConstants.DATA_SECTION).toString())
                    .get(RESTAPITestConstants.ACCESS_TOKEN_TEXT).toString();

            if (accessToken != null) {
                return accessToken;
            }

        } catch (MalformedURLException malformedURLException) {
            log.error("Error in getting the URL of token endpoint.", malformedURLException);
            throw new APIManagerIntegrationTestException
                    ("Error in getting the URL of token endpoint.", malformedURLException);
        } catch (UnsupportedEncodingException unsupportedEncodingException) {
            log.error("Message header encoding was unsuccessful using UTF-8.", unsupportedEncodingException);
            throw new APIManagerIntegrationTestException
                    ("Message header encoding was unsuccessful using UTF-8.", unsupportedEncodingException);
        } catch (AutomationFrameworkException automationFrameworkException) {
            log.error("Error in sending the request to token endpoint.", automationFrameworkException);
            throw new APIManagerIntegrationTestException
                    ("Error in sending the request to token endpoint.", automationFrameworkException);
        } catch (JSONException e) {
            log.error("Error in parsing JSON content in response from token endpoint.", e);
            throw new APIManagerIntegrationTestException
                    ("Error in parsing JSON content in response from token endpoint.", e);
        }
        return null;
    }


    public Map<String, String> registerOAuthApplication() throws APIManagerIntegrationTestException {

        String dcrEndpointURL = getKeyManagerURLHttp() + RESTAPITestConstants.CLIENT_REGISTRATION_URL;

        //use a random name for client to avoid conflicts in application(s)
        String randomClientName = RandomStringUtils.randomAlphabetic(5);
        String applicationRequestBody = "{\n" +
                "\"callbackUrl\": \"www.google.lk\",\n" +
                "\"clientName\": \"" + randomClientName + "\",\n" +
                "\"tokenScope\": \"Production\",\n" +
                "\"owner\": \"admin\",\n" +
                "\"grantType\": \"password refresh_token\",\n" +
                "\"saasApp\": true\n" +
                "}";

        Map<String, String> dcrRequestHeaders = new HashMap<String, String>();
        Map<String, String> dataMap = new HashMap<String, String>();

        try {

            //Basic Auth header is used for only to get token
            byte[] encodedBytes = Base64.encodeBase64(RESTAPITestConstants.BASIC_AUTH_HEADER.getBytes("UTF-8"));
            dcrRequestHeaders.put(RESTAPITestConstants.AUTHORIZATION_KEY, "Basic " + new String(encodedBytes, "UTF-8"));

            //Set content type as its mandatory
            dcrRequestHeaders.put(RESTAPITestConstants.CONTENT_TYPE, RESTAPITestConstants.APPLICATION_JSON_CONTENT);
            JSONObject clientRegistrationResponse = new JSONObject
                    (HttpRequestUtil.doPost(new URL(dcrEndpointURL), applicationRequestBody, dcrRequestHeaders));
            String consumerKey = new JSONObject(clientRegistrationResponse.getString(RESTAPITestConstants.DATA_SECTION))
                    .get(RESTAPITestConstants.CLIENT_ID).toString();
            String consumerSecret = new JSONObject
                    (clientRegistrationResponse.getString(RESTAPITestConstants.DATA_SECTION)).
                    get(RESTAPITestConstants.CLIENT_SECRET).toString();

            //give 2 second duration to create consumer key and consumer secret
            Thread.sleep(2000);
            dataMap.put(RESTAPITestConstants.CONSUMER_KEY, consumerKey);
            dataMap.put(RESTAPITestConstants.CONSUMER_SECRET, consumerSecret);

        } catch (UnsupportedEncodingException unsupportedEncodingException) {
            log.error("Header encoding was unsuccessful while registering application.", unsupportedEncodingException);
            throw new APIManagerIntegrationTestException
                    ("Header encoding was unsuccessful while registering application.", unsupportedEncodingException);
        } catch (MalformedURLException malformedURLException) {
            log.error("Error in getting the DCR endpoint URL.", malformedURLException);
            throw new APIManagerIntegrationTestException
                    ("Error in getting the DCR endpoint URL.", malformedURLException);
        } catch (AutomationFrameworkException automationFrameworkException) {
            log.error("Error in sending request to the DCR endpoint.", automationFrameworkException);
            throw new APIManagerIntegrationTestException
                    ("Error in sending request to the DCR endpoint.", automationFrameworkException);
        } catch (JSONException e) {
            log.error("Error in parsing JSON to get consumer key/secret.", e);
            throw new APIManagerIntegrationTestException("Error in parsing JSON to get consumer key/secret.", e);
        } catch (InterruptedException interruptedException) {
            log.error("Thread interrupted while waiting to get consumer key/secret.", interruptedException);
            throw new APIManagerIntegrationTestException
                    ("Thread interrupted while waiting to get consumer key/secret.", interruptedException);
        }
        return dataMap;
    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
        super.cleanUp();
    }
}
