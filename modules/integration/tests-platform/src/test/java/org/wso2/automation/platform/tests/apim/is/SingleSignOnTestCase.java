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

package org.wso2.automation.platform.tests.apim.is;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;
import org.json.simple.parser.JSONParser;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.am.integration.test.utils.base.AMIntegrationBaseTest;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.extensions.ExtensionConstants;

import javax.ws.rs.core.Response;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/**
 * This class basically tests the functionality of SAML2 Single Sign On (SSO) Scenario.
 * Test flow includes log-in to API publisher , create & publish API ,Subscription, Token generation & API
 * invocation.
 * <p/>
 * For more details refer : https://docs.wso2.com/display/AM180/Configuring+Single+Sign-on
 * <p/>
 * Note:
 * IS server should be up and running with port off-set value '1'
 * APIM server should be up and running with port off-set value '0'
 */

public class SingleSignOnTestCase extends AMIntegrationBaseTest {

    private String relayState;
    private String ssoAuthSessionID;
    private String samlRequest;
    private String providerName;
    private String hostIP;
    private String commonAuthUrl;
    private String httpsStoreUrl;
    private String httpsPublisherUrl;
    private String samlSsoEndpointUrl;
    private String apiName = "SingleSignOnAPI";
    private String apiVersion = "1.0.0";
    private String callbackUrl = "www.youtube.com";

    private HttpResponse response;
    private HttpClient httpClient;
    private List<NameValuePair> urlParameters = new ArrayList<NameValuePair>();

    private static JSONParser parser = new JSONParser();

    private final static String SSO_AUTH_SESSION_ID = "SSOAuthSessionID";
    private final static String SAML_REQUEST = "SAMLRequest";
    private final static String RELAY_STATE = "RelayState";
    private final static String USER_AGENT = "Apache-HttpClient/4.2.5 (java 1.5)";
    private final static int IS_SERVER_PORT = Integer.parseInt(ExtensionConstants.SERVER_DEFAULT_HTTPS_PORT) + 1;

    @BeforeClass(alwaysRun = true)
    public void init() throws Exception {

        super.init(TestUserMode.SUPER_TENANT_ADMIN);

        httpClient = new DefaultHttpClient();
        httpsPublisherUrl = contextUrls.getSecureServiceUrl().replace("services", "publisher");
        httpsStoreUrl = contextUrls.getSecureServiceUrl().replace("services", "store");
        hostIP = apimContext.getDefaultInstance().getHosts().get("default");
        providerName = apimContext.getContextTenant().getContextUser().getUserName();
        commonAuthUrl = "https://" + hostIP + ":" + IS_SERVER_PORT + "/commonauth";
        samlSsoEndpointUrl = "https://" + hostIP + ":" + IS_SERVER_PORT + "/samlsso";
    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
        super.cleanup();
    }

    @Test(description = "Login to publisher for first time create api & publish", groups = "wso2.apim.is")
    public void singleSignOnWithSAMLPublisherRelatedOperationsTestCase() throws Exception {

        response = sendGetRequest(String.format(httpsPublisherUrl));

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatusLine().getStatusCode(),
                "Response mismatch not 200");

        HashMap<String, String> requestParameters = extractDataFromResponse(response, "name=\"SAMLRequest\"",
                "name=\"RelayState\"", "name=\"SSOAuthSessionID\"", 1);

        relayState = requestParameters.get(RELAY_STATE).replaceAll("\"", "").
                replaceAll(">", "").replaceAll("/", "");
        ssoAuthSessionID = requestParameters.get(SSO_AUTH_SESSION_ID).replaceAll("\"", "").
                replaceAll(">", "").replaceAll("/", "");
        samlRequest = requestParameters.get(SAML_REQUEST).replaceAll("\"", "").
                replaceAll(">", "").replaceAll("/", "");

        urlParameters.add(new BasicNameValuePair(SAML_REQUEST, samlRequest));
        urlParameters.add(new BasicNameValuePair(RELAY_STATE, relayState));
        urlParameters.add(new BasicNameValuePair(SSO_AUTH_SESSION_ID, ssoAuthSessionID));

        response = sendPOSTMessage(samlSsoEndpointUrl, urlParameters);

        assertEquals(302, response.getStatusLine().getStatusCode(), "Response mismatch not 302");
        EntityUtils.consume(response.getEntity());

        Header[] headers = response.getAllHeaders();
        String url = "";
        for (Header header : headers) {
            if ("Location".equals(header.getName())) {
                url = header.getValue();
                break;
            }
        }

        response = sendRedirectRequest(response);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatusLine().getStatusCode(),
                "Response mismatch not 200");

        String sessionKey = extractDataFromResponse(response, "name=\"sessionDataKey\"", 1);

        urlParameters.clear();
        urlParameters.add(new BasicNameValuePair("username", apimContext.getContextTenant().getContextUser().
                getUserName()));
        urlParameters.add(new BasicNameValuePair("password", apimContext.getContextTenant().getContextUser().
                getPassword()));
        urlParameters.add(new BasicNameValuePair("sessionDataKey", sessionKey));

        response = sendPOSTMessage(commonAuthUrl, urlParameters);
        assertEquals(302, response.getStatusLine().getStatusCode(), "Response mismatch not 302");
        EntityUtils.consume(response.getEntity());

        response = sendGetRequest(String.format(samlSsoEndpointUrl + "?" + url.substring(url.lastIndexOf
                ("sessionDataKey"), url.lastIndexOf("&type"))));

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatusLine().getStatusCode(),
                "Response mismatch not 200");

        String samlResponse = extractSAMLRequestFromResponse(response, "name='SAMLResponse'", 1).replaceAll("'", "").
                replaceAll(">", "");

        urlParameters.add(new BasicNameValuePair("SAMLResponse", samlResponse));
        urlParameters.add(new BasicNameValuePair(RELAY_STATE, relayState));

        response = sendPOSTMessage(httpsPublisherUrl + "/jagg/jaggery_acs.jag",
                urlParameters);
        assertEquals(302, response.getStatusLine().getStatusCode(), "Response mismatch not 302");
        EntityUtils.consume(response.getEntity());

        response = sendGetRequest(String.format(httpsPublisherUrl));
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatusLine().getStatusCode(),
                "Response mismatch not 200");
        EntityUtils.consume(response.getEntity());

        // API Creation and Publish
        assertTrue(createAndPublishAPI(), "API creation and change life cycle to publish was unsuccessful");

    }


    @Test(description = "Verify SSO Login Process , api subscription & invocation of api", groups = "wso2.apim.is",
            dependsOnMethods = "singleSignOnWithSAMLPublisherRelatedOperationsTestCase")
    public void singleSignOnWithSAMLStoreRelatedOperationsTestCase() throws Exception {

        // SSO SAML Login to store
        response = sendGetRequest(httpsStoreUrl + "/site/pages/sso-filter.jag?" +
                "requestedPage=https%3A%2F%2F" + hostIP + "%3A" + ExtensionConstants.SERVER_DEFAULT_HTTPS_PORT +
                "%2Fstore%2F%3Ftenant%3D" + apimContext.getSuperTenant().getDomain());
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatusLine().getStatusCode(),
                "Response mismatch not 200");

        HashMap<String, String> requestParameters = extractDataFromResponse(response, "name=\"SAMLRequest\"",
                "name=\"RelayState\"", "name=\"SSOAuthSessionID\"", 1);

        relayState = requestParameters.get(RELAY_STATE).replaceAll("\"", "").
                replaceAll(">", "").replaceAll("/", "");
        ssoAuthSessionID = requestParameters.get(SSO_AUTH_SESSION_ID).replaceAll("\"", "").
                replaceAll(">", "").replaceAll("/", "");
        samlRequest = requestParameters.get(SAML_REQUEST).replaceAll("\"", "").
                replaceAll(">", "").replaceAll("/", "");

        urlParameters.add(new BasicNameValuePair(SAML_REQUEST, samlRequest));
        urlParameters.add(new BasicNameValuePair(RELAY_STATE, relayState));
        urlParameters.add(new BasicNameValuePair(SSO_AUTH_SESSION_ID, ssoAuthSessionID));

        response = sendPOSTMessage(samlSsoEndpointUrl, urlParameters);
        assertEquals(302, response.getStatusLine().getStatusCode(), "Response mismatch not 302");
        EntityUtils.consume(response.getEntity());

        Header[] headers = response.getAllHeaders();
        String url = "";
        for (Header header : headers) {
            if ("Location".equals(header.getName())) {
                url = header.getValue();
                break;
            }
        }

        response = sendGetRequest(commonAuthUrl + "?null&relyingParty=API_STORE&"
                + url.substring(url.lastIndexOf("sessionDataKey"), url.lastIndexOf("&type"))
                + "&type=samlsso&commonAuthCallerPath=%2Fsamlsso&forceAuth=false&passiveAuth=false");
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatusLine().getStatusCode(),
                "Response mismatch not 200");

        String samlResponse = extractSAMLRequestFromResponse(response, "name='SAMLResponse'", 1).replaceAll("'", "").
                replaceAll(">", "");

        urlParameters.clear();
        urlParameters.add(new BasicNameValuePair("SAMLResponse", samlResponse));
        urlParameters.add(new BasicNameValuePair(RELAY_STATE, relayState));

        response = sendPOSTMessage(httpsStoreUrl + "/jagg/jaggery_acs.jag",
                urlParameters);
        assertEquals(302, response.getStatusLine().getStatusCode(), "Response mismatch not 302");
        EntityUtils.consume(response.getEntity());

        response = sendGetRequest(String.format(httpsStoreUrl + "/?tenant=" + apimContext.getSuperTenant().getDomain()));
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatusLine().getStatusCode(),
                "Response mismatch not 200");

        EntityUtils.consume(response.getEntity());

        createApplication();

        String accessToken = subscriptionAndKeyGenerationToAPI();

        HttpGet request = new HttpGet("http://" + apimContext.getDefaultInstance().getHosts().get("default")
                + ":" + "8280/sso/1.0.0/most_popular");
        request.addHeader("User-Agent", USER_AGENT);
        request.addHeader("Authorization", "Bearer " + accessToken);

        response = httpClient.execute(request);

        assertEquals(response.getStatusLine().getStatusCode(), Response.Status.OK.getStatusCode(),
                "Response code mismatch for api invocation");

        BufferedReader bufferedReader = new BufferedReader(
                new InputStreamReader(response.getEntity().getContent(), "UTF-8"));
        String line;

        boolean feedUrl = false;
        boolean category = false;
        boolean entry = false;

        while ((line = bufferedReader.readLine()) != null) {
            if (line.contains("<feed")) {
                feedUrl = true;
            }
            if (line.contains("<category")) {
                category = true;
            }
            if (line.contains("<entry>")) {
                entry = true;
            }
        }

        bufferedReader.close();

        assertTrue(feedUrl, "Response data mismatched when api invocation. feedUrl not found.");
        assertTrue(category, "Response data mismatched when api invocation. category not found");
        assertTrue(entry, "Response data mismatched when api invocation. entry not found");

    }

    private String subscriptionAndKeyGenerationToAPI() throws Exception {

        urlParameters.clear();
        urlParameters.add(new BasicNameValuePair("action", "addAPISubscription"));
        urlParameters.add(new BasicNameValuePair("applicationName", "SSOApplication"));
        urlParameters.add(new BasicNameValuePair("name", apiName));
        urlParameters.add(new BasicNameValuePair("version", apiVersion));
        urlParameters.add(new BasicNameValuePair("provider", providerName));
        urlParameters.add(new BasicNameValuePair("tier", "Unlimited"));
        response = sendPOSTMessage(httpsStoreUrl + "/site/blocks/subscription/" +
                "subscription-add/ajax/subscription-add.jag",
                urlParameters);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatusLine().getStatusCode(),
                "Response status code mismatch for subscription");
        EntityUtils.consume(response.getEntity());

        // generate production keys
        response = sendGetRequest(String.format(httpsStoreUrl + "/site/pages/subscriptions.jag?" +
                "selectedApp=SSOApplication&tenant=" + apimContext.getSuperTenant().getDomain()));
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatusLine().getStatusCode(),
                "Response mismatch ststus code not 200 for subscription page navigation");
        EntityUtils.consume(response.getEntity());

        urlParameters.clear();
        urlParameters.add(new BasicNameValuePair("action", "generateApplicationKey"));
        urlParameters.add(new BasicNameValuePair("application", "SSOApplication"));
        urlParameters.add(new BasicNameValuePair("keytype", "PRODUCTION"));
        urlParameters.add(new BasicNameValuePair("callbackUrl", callbackUrl));
        urlParameters.add(new BasicNameValuePair("authorizedDomains", "ALL"));
        urlParameters.add(new BasicNameValuePair("validityTime", "500"));  // 2 minutes
        response = sendPOSTMessage(httpsStoreUrl + "/site/blocks/subscription/" +
                "subscription-add/ajax/subscription-add.jag",
                urlParameters);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatusLine().getStatusCode(),
                "Response status code  mismatch for generating production key");
        parser.reset();

        BufferedReader bufferedReader = new BufferedReader(
                new InputStreamReader(response.getEntity().getContent(), "UTF-8"));
        String responseString;

        responseString = bufferedReader.readLine();

        bufferedReader.close();

        JSONObject obj = new JSONObject(responseString);

        String accessToken =
                obj.getJSONObject("data").getJSONObject("key").get("accessToken").toString();

        EntityUtils.consume(response.getEntity());

        return accessToken;
    }

    /**
     * Performing API creation & publish the API
     *
     * @throws Exception
     */
    private Boolean createAndPublishAPI() throws Exception {

        String APIDescription = "This is test API create by API manager for demonstration purposes " +
                "for SSO SAML platform tests";
        String APIContext = "sso";
        HttpResponse response = sendGetRequest(String.format(httpsPublisherUrl + "/design"));
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatusLine().getStatusCode(),
                "Response mismatch not 200");
        EntityUtils.consume(response.getEntity());

        //11.
        urlParameters.clear();
        urlParameters.add(new BasicNameValuePair("name", apiName));
        urlParameters.add(new BasicNameValuePair("context", APIContext));
        urlParameters.add(new BasicNameValuePair("version", apiVersion));
        urlParameters.add(new BasicNameValuePair("visibility", "public"));
        urlParameters.add(new BasicNameValuePair("roles", ""));
        urlParameters.add(new BasicNameValuePair("apiThumb", ""));
        urlParameters.add(new BasicNameValuePair("description", APIDescription));
        urlParameters.add(new BasicNameValuePair("tags", "WSO2,APIM,IS,CrossProduct,PlatformTests"));
        urlParameters.add(new BasicNameValuePair("action", "design"));
        urlParameters.add(new BasicNameValuePair("swagger",                  //
                "{\"apiVersion\":" + apiVersion + ",\"swaggerVersion\":\"1.2\"," +
                        "\"apis\":[{\"path\":\"/default\",\"description\":\"\",\"file\":{\"apiVersion\":" + apiVersion + "," +
                        "\"swaggerVersion\":\"1.2\",\"basePath\":\"http://localhost:8280/CrossProductsAPI3Context/1.0.0\"," +
                        "\"resourcePath\":\"/default\",\"apis\":[{\"path\":\"/*\",\"operations\":[{\"method\":\"GET\"," +
                        "\"parameters\":[{\"name\":\"body\",\"description\":\"Request Body\",\"allowMultiple\":false," +
                        "\"required\":true,\"paramType\":\"body\",\"type\":\"string\"}],\"nickname\":\"get_*\"}," +
                        "{\"method\":\"POST\",\"parameters\":[{\"name\":\"body\",\"description\":\"Request Body\"," +
                        "\"allowMultiple\":false,\"required\":true,\"paramType\":\"body\",\"type\":\"string\"}]," +
                        "\"nickname\":\"post_*\"},{\"method\":\"PUT\",\"parameters\":[{\"name\":\"body\",\"description\":" +
                        "\"Request Body\",\"allowMultiple\":false,\"required\":true,\"paramType\":\"body\",\"type\":" +
                        "\"string\"}],\"nickname\":\"put_*\"},{\"method\":\"DELETE\",\"parameters\":[{\"name\":\"body\"," +
                        "\"description\":\"Request Body\",\"allowMultiple\":false,\"required\":true,\"paramType\":\"body\"," +
                        "\"type\":\"string\"}],\"nickname\":\"delete_*\"},{\"method\":\"OPTIONS\",\"parameters\":[{\"name\":" +
                        "\"body\",\"description\":\"Request Body\",\"allowMultiple\":false,\"required\":true,\"paramType\":" +
                        "\"body\",\"type\":\"string\"}],\"nickname\":\"options_*\"}]}]}}],\"info\":{\"title\":" +
                        "" + apiName + ",\"description\":" +
                        "" + APIDescription + "," + "\"termsOfServiceUrl\":\"\",\"contact\":\"\",\"license\":\"\"," +
                        "\"licenseUrl\":\"\"}," +
                        "\"authorizations\":{\"oauth2\":{\"type\":\"oauth2\",\"scopes\":[]}}}"));


        response = sendPOSTMessage(httpsPublisherUrl + "/site/blocks/item-design/ajax/add.jag?",
                urlParameters);
        EntityUtils.consume(response.getEntity());

        // 12.
        response = sendGetRequest(String.format(httpsPublisherUrl + "/implement?name=" +
                apiName + "&version=" + apiVersion + "&provider=" + providerName));
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatusLine().getStatusCode(),
                "Response mismatch not 200");
        EntityUtils.consume(response.getEntity());

        //14.
        urlParameters.clear();
        urlParameters.add(new BasicNameValuePair("implementation_methods", "endpoint"));
        urlParameters.add(new BasicNameValuePair("endpoint_type", "http"));
        urlParameters.add(new BasicNameValuePair("endpoint_config", "{\"production_endpoints\":{\"url\":" +
                "\"http://gdata.youtube.com/feeds/api/standardfeeds\",\"config\":null},\"endpoint_type\":\"http\"}"));
        urlParameters.add(new BasicNameValuePair("production_endpoints", "http://gdata.youtube.com/feeds/api/" +
                "standardfeeds"));
        urlParameters.add(new BasicNameValuePair("sandbox_endpoints", ""));
        urlParameters.add(new BasicNameValuePair("endpointType", "nonsecured"));
        urlParameters.add(new BasicNameValuePair("epUsername", providerName));
        urlParameters.add(new BasicNameValuePair("epPassword", providerName));
        urlParameters.add(new BasicNameValuePair("wsdl", ""));
        urlParameters.add(new BasicNameValuePair("wadl", ""));
        urlParameters.add(new BasicNameValuePair("name", apiName));
        urlParameters.add(new BasicNameValuePair("version", apiVersion));
        urlParameters.add(new BasicNameValuePair("provider", providerName));
        urlParameters.add(new BasicNameValuePair("action", "implement"));
        urlParameters.add(new BasicNameValuePair("swagger", "{\"apiVersion\":" + apiVersion + ",\"swaggerVersion\":" +
                "\"1.2\",\"authorizations\":{\"oauth2\":{\"scopes\":[],\"type\":\"oauth2\"}},\"apis\":[{\"file\"" +
                ":{\"apiVersion\":" + apiVersion + ",\"basePath\":\"http://10.100.0.42:8280/CrossProductsAPI3Context/1.0.0\"" +
                ",\"swaggerVersion\":\"1.2\",\"resourcePath\":\"/default\",\"apis\":[{\"path\":\"/*\",\"" +
                "operations\":[{\"nickname\":\"get_*\",\"method\":\"GET\",\"parameters\":[{\"description\"" +
                ":\"Request Body\",\"name\":\"body\",\"allowMultiple\":false,\"required\":true,\"type\":" +
                "\"string\",\"paramType\":\"body\"}]},{\"nickname\":\"post_*\",\"method\":\"POST\",\"parameters\"" +
                ":[{\"description\":\"Request Body\",\"name\":\"body\",\"allowMultiple\":false,\"required\":true," +
                "\"type\":\"string\",\"paramType\":\"body\"}]},{\"nickname\":\"put_*\",\"method\":\"PUT\"," +
                "\"parameters\":[{\"description\":\"Request Body\",\"name\":\"body\",\"allowMultiple\":false," +
                "\"required\":true,\"type\":\"string\",\"paramType\":\"body\"}]},{\"nickname\":\"delete_*\"," +
                "\"method\":\"DELETE\",\"parameters\":[{\"description\":\"Request Body\",\"name\":\"body\"," +
                "\"allowMultiple\":false,\"required\":true,\"type\":\"string\",\"paramType\":\"body\"}]}," +
                "{\"nickname\":\"options_*\",\"method\":\"OPTIONS\",\"parameters\":[{\"description\":" +
                "\"Request Body\",\"name\":\"body\",\"allowMultiple\":false,\"required\":true,\"type\":" +
                "\"string\",\"paramType\":\"body\"}]}]}]},\"description\":\"\",\"path\":\"/default\"}]," +
                "\"info\":{\"title\":\"CrossProductsAPI3\",\"termsOfServiceUrl\":\"\",\"description\":" +
                "\"This is test API create by API manager for demonstration purposes for platform tests\"," +
                "\"license\":\"\",\"contact\":\"\",\"licenseUrl\":\"\"}}"));

        response = sendPOSTMessage(httpsPublisherUrl + "/site/blocks/item-design/ajax/add.jag?",
                urlParameters);
        EntityUtils.consume(response.getEntity());

        //15.
        response = sendGetRequest(String.format(httpsPublisherUrl + "/manage?name=" +
                apiName + "&version=" + apiVersion + "&provider=" + providerName));
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatusLine().getStatusCode(),
                "Response mismatch not 200");
        EntityUtils.consume(response.getEntity());

        //20.
        urlParameters.clear();
        urlParameters.add(new BasicNameValuePair("default_version_checked", ""));
        urlParameters.add(new BasicNameValuePair("tier", "Unlimited"));
        urlParameters.add(new BasicNameValuePair("transport_http", "http"));
        urlParameters.add(new BasicNameValuePair("transport_https", "https"));
        urlParameters.add(new BasicNameValuePair("inSequence", "none"));
        urlParameters.add(new BasicNameValuePair("outSequence", "none"));
        urlParameters.add(new BasicNameValuePair("faultSequence", "none"));
        urlParameters.add(new BasicNameValuePair("responseCache", "disabled"));
        urlParameters.add(new BasicNameValuePair("cacheTimeout", "0"));
        urlParameters.add(new BasicNameValuePair("subscriptions", "current_tenant"));
        urlParameters.add(new BasicNameValuePair("tenants", ""));
        urlParameters.add(new BasicNameValuePair("bizOwner", ""));
        urlParameters.add(new BasicNameValuePair("bizOwnerMail", ""));
        urlParameters.add(new BasicNameValuePair("techOwner", ""));
        urlParameters.add(new BasicNameValuePair("techOwnerMail", ""));
        urlParameters.add(new BasicNameValuePair("name", apiName));
        urlParameters.add(new BasicNameValuePair("version", apiVersion));
        urlParameters.add(new BasicNameValuePair("provider", providerName));
        urlParameters.add(new BasicNameValuePair("action", "manage"));
        urlParameters.add(new BasicNameValuePair("swagger", "{\"apiVersion\":" + apiVersion + ",\"swaggerVersion\":\"1.2\"," +
                "\"authorizations\":{\"oauth2\":{\"scopes\":[],\"type\":\"oauth2\"}},\"apis\":[{\"file\":" +
                "{\"apiVersion\":" + apiVersion + ",\"basePath\":\"http://10.100.0.42:8280/CrossProductsAPI3Context/1.0.0\"," +
                "\"swaggerVersion\":\"1.2\",\"resourcePath\":\"/default\",\"apis\":[{\"path\":\"/*\",\"operations\":" +
                "[{\"nickname\":\"get_*\",\"method\":\"GET\",\"parameters\":[{\"description\":\"Request Body\"" +
                ",\"name\":\"body\",\"allowMultiple\":false,\"required\":true,\"type\":\"string\",\"paramType\"" +
                ":\"body\"}],\"auth_type\":\"Application \n" +
                "\t Application User\",\"throttling_tier\":\"Unlimited\"},{\"nickname\":\"post_*\",\"method\":" +
                "\"POST\",\"parameters\":[{\"description\":\"Request Body\",\"name\":\"body\",\"" +
                "allowMultiple\":false,\"required\":true,\"type\":\"string\",\"paramType\":\"body\"}],\"" +
                "auth_type\":\"Application : \n" +
                "\t Application User\",\"throttling_tier\":\"Unlimited\"},{\"nickname\":\"put_*\",\"method\":" +
                "\"PUT\",\"parameters\":[{\"description\":\"Request Body\",\"name\":\"body\"," +
                "\"allowMultiple\":false,\"required\":true,\"type\":\"string\",\"paramType\":" +
                "\"body\"}],\"auth_type\":\"Application : \n" +
                "\t Application User\",\"throttling_tier\":\"Unlimited\"},{\"nickname\":\"delete_*\",\"" +
                "method\":\"DELETE\",\"parameters\":[{\"description\":\"Request Body\",\"name\":\"body\"," +
                "\"allowMultiple\":false,\"required\":true,\"type\":\"string\",\"paramType\":\"body\"}]," +
                "\"auth_type\":\"Application : \n" +
                "\t Application User\",\"throttling_tier\":\"Unlimited\"},{\"nickname\":\"options_*\"," +
                "\"method\":\"OPTIONS\",\"parameters\":[{\"description\":\"Request Body\",\"name\":\"body\"," +
                "\"allowMultiple\":false,\"required\":true,\"type\":\"string\",\"paramType\":\"body\"}],\"" +
                "auth_type\":\"None\",\"throttling_tier\":\"Unlimited\"}]}]},\"description\":\"\",\"path\":" +
                "\"/default\"}],\"info\":{\"title\":\"CrossProductsAPI3\",\"termsOfServiceUrl\":\"\",\"" +
                "description\":\"This is test API create by API manager for demonstration purposes for platform" +
                " tests\",\"license\":\"\",\"contact\":\"\",\"licenseUrl\":\"\"}}"));
        urlParameters.add(new BasicNameValuePair("faultSeq", "json_fault"));
        urlParameters.add(new BasicNameValuePair("outSeq", "log_out_message"));
        urlParameters.add(new BasicNameValuePair("inSeq", "log_in_message"));
        urlParameters.add(new BasicNameValuePair("tiersCollection", "Unlimited"));


        response = sendPOSTMessage(httpsPublisherUrl + "/site/blocks/item-design/ajax/add.jag?",
                urlParameters);
        EntityUtils.consume(response.getEntity());


        //21.
        urlParameters.clear();
        urlParameters.add(new BasicNameValuePair("action", "updateStatus"));
        urlParameters.add(new BasicNameValuePair("name", apiName));
        urlParameters.add(new BasicNameValuePair("version", apiVersion));
        urlParameters.add(new BasicNameValuePair("provider", providerName));
        urlParameters.add(new BasicNameValuePair("status", "PUBLISHED"));
        urlParameters.add(new BasicNameValuePair("publishToGateway", "true"));
        urlParameters.add(new BasicNameValuePair("requireResubscription", "true"));
        response = sendPOSTMessage(httpsPublisherUrl + "/site/blocks/life-cycles/ajax/life-cycles.jag",
                urlParameters);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatusLine().getStatusCode(),
                "Response mismatch not 200");
        EntityUtils.consume(response.getEntity());

        return true;
    }

    private void createApplication() throws Exception {

        //1
        HttpResponse response = sendGetRequest(String.format(httpsStoreUrl + "/site/pages" +
                "/applications.jag?tenant=" + apimContext.getSuperTenant().getDomain()));
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatusLine().getStatusCode(),
                "Response mismatch not 200");
        EntityUtils.consume(response.getEntity());

        //2
        urlParameters.clear();
        urlParameters.add(new BasicNameValuePair("action", "getRecentlyAddedAPIs"));
        urlParameters.add(new BasicNameValuePair("tenant", apimContext.getSuperTenant().getDomain()));
        urlParameters.add(new BasicNameValuePair("limit", "5"));
        response = sendPOSTMessage(httpsStoreUrl + "/site/blocks/api/recently-added/ajax/list.jag",
                urlParameters);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatusLine().getStatusCode(),
                "Response mismatch not 200");
        EntityUtils.consume(response.getEntity());


        //3
        urlParameters.clear();
        urlParameters.add(new BasicNameValuePair("action", "sessionCheck"));
        response = sendPOSTMessage(httpsStoreUrl + "/site/blocks/user/login/ajax/sessionCheck.jag",
                urlParameters);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatusLine().getStatusCode(),
                "Response mismatch not 200");
        EntityUtils.consume(response.getEntity());

        urlParameters.clear();
        urlParameters.add(new BasicNameValuePair("action", "addApplication"));
        urlParameters.add(new BasicNameValuePair("tier", "Unlimited"));
        urlParameters.add(new BasicNameValuePair("callbackUrl", callbackUrl));
        urlParameters.add(new BasicNameValuePair("description", "This is platform based application"));
        urlParameters.add(new BasicNameValuePair("application", "SSOApplication"));
        response = sendPOSTMessage(httpsStoreUrl + "/site/blocks/application/" +
                "application-add/ajax/application-add.jag",
                urlParameters);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatusLine().getStatusCode(),
                "Response mismatch not 200");
        EntityUtils.consume(response.getEntity());


    }

    private HttpResponse sendGetRequest(String url) throws Exception {
        HttpGet request = new HttpGet(url);
        request.addHeader("User-Agent", USER_AGENT);
        return httpClient.execute(request);
    }

    private HttpResponse sendPOSTMessage(String url, List<NameValuePair> urlParameters) throws Exception {
        HttpPost post = new HttpPost(url);
        post.setHeader("User-Agent", USER_AGENT);
        post.addHeader("Referer", url);
        post.setEntity(new UrlEncodedFormEntity(urlParameters));
        return httpClient.execute(post);
    }


    private HttpResponse sendRedirectRequest(HttpResponse response) throws IOException {
        Header[] headers = response.getAllHeaders();
        String url = "";
        for (Header header : headers) {
            if ("Location".equals(header.getName())) {
                url = header.getValue();
            }
        }

        HttpGet request = new HttpGet(url);
        request.addHeader("User-Agent", USER_AGENT);
        request.addHeader("Referer", commonAuthUrl);
        return httpClient.execute(request);
    }

    private String extractSAMLRequestFromResponse(HttpResponse response, String key, int token)
            throws IOException {
        BufferedReader rd = new BufferedReader(
                new InputStreamReader(response.getEntity().getContent(), "UTF-8"));
        String line;
        String value = "";

        while ((line = rd.readLine()) != null) {
            if (line.contains(key)) {
                String[] tokens = line.split("value=");
                value = tokens[token];
                break;
            }
        }
        rd.close();
        return value;
    }

    private String extractDataFromResponse(HttpResponse response, String key, int token)
            throws IOException {
        BufferedReader rd = new BufferedReader(
                new InputStreamReader(response.getEntity().getContent(), "UTF-8"));
        String line;
        String value = "";

        while ((line = rd.readLine()) != null) {
            if (line.contains(key)) {
                String[] tokens = line.split("'");
                value = tokens[token];
                break;
            }
        }
        rd.close();
        return value;
    }

    private HashMap<String, String> extractDataFromResponse(HttpResponse response, String samlRequest,
                                                            String relayState, String ssoAuthSessionID, int token)
            throws IOException {
        BufferedReader rd = new BufferedReader(
                new InputStreamReader(response.getEntity().getContent(), "UTF-8"));
        String line;
        HashMap<String, String> params = new HashMap<String, String>();

        while ((line = rd.readLine()) != null) {
            if (line.contains(samlRequest) || line.contains(relayState) || line.contains(ssoAuthSessionID)) {
                String[] tokens;
                if (line.contains(samlRequest)) {
                    rd.readLine();
                    tokens = rd.readLine().split("value");
                } else {
                    tokens = line.split("value");
                }
                if (line.contains(samlRequest)) {
                    params.put(SAML_REQUEST, tokens[token].substring(2));
                } else if (line.contains(relayState)) {
                    params.put(RELAY_STATE, tokens[token].substring(2));
                } else {
                    params.put(SSO_AUTH_SESSION_ID, tokens[token].substring(2));
                }
            }
        }
        rd.close();

        return params;
    }
}
