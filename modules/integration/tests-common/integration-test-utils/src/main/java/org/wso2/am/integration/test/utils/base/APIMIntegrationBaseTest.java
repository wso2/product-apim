/*
*Copyright (c) 2015​, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
package org.wso2.am.integration.test.utils.base;

import org.apache.axiom.om.OMElement;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.Header;
import org.json.JSONException;
import org.json.JSONObject;
import org.testng.Assert;
import org.wso2.am.admin.clients.application.ApplicationManagementClient;
import org.wso2.am.admin.clients.claim.ClaimMetaDataMgtAdminClient;
import org.wso2.am.admin.clients.oauth.OAuthAdminServiceClient;
import org.wso2.am.admin.clients.user.RemoteUserStoreManagerServiceClient;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIInfoDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIListDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIProductInfoDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIProductListDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationInfoDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationListDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.SubscriptionDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.SubscriptionListDTO;
import org.wso2.am.integration.test.impl.RestAPIAdminImpl;
import org.wso2.am.integration.test.impl.RestAPIPublisherImpl;
import org.wso2.am.integration.test.impl.RestAPIStoreImpl;
import org.wso2.am.integration.test.utils.APIManagerIntegrationTestException;
import org.wso2.am.integration.test.utils.bean.APIMURLBean;
import org.wso2.am.integration.test.utils.clients.APIPublisherRestClient;
import org.wso2.am.integration.test.utils.clients.APIStoreRestClient;
import org.wso2.am.integration.test.utils.generic.APIMTestCaseUtils;
import org.wso2.am.integration.test.utils.http.HttpRequestUtil;
import org.wso2.carbon.automation.engine.annotations.ExecutionEnvironment;
import org.wso2.carbon.automation.engine.context.AutomationContext;
import org.wso2.carbon.automation.engine.context.ContextXpathConstants;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.engine.context.beans.User;
import org.wso2.carbon.automation.engine.frameworkutils.FrameworkPathUtil;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;
import org.wso2.carbon.base.MultitenantConstants;
import org.wso2.carbon.integration.common.admin.client.TenantManagementServiceClient;
import org.wso2.carbon.integration.common.admin.client.UserManagementClient;
import org.wso2.carbon.integration.common.utils.LoginLogoutClient;
import org.wso2.carbon.tenant.mgt.stub.beans.xsd.TenantInfoBean;
import org.wso2.carbon.utils.multitenancy.MultitenantUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;

import javax.xml.stream.XMLStreamException;
import javax.xml.xpath.XPathExpressionException;

/**
 * Base class for all API Manager integration tests
 * Users need to extend this class to write integration tests.
 */
public class APIMIntegrationBaseTest {

    private static final Log log = LogFactory.getLog(APIMIntegrationBaseTest.class);
    protected AutomationContext storeContext, publisherContext, keyManagerContext, gatewayContextMgt,
            gatewayContextWrk, backEndServer, superTenantKeyManagerContext;
    protected OMElement synapseConfiguration;
    protected APIMTestCaseUtils apimTestCaseUtils;
    protected TestUserMode userMode;
    protected String executionMode;
    protected APIMURLBean storeUrls, publisherUrls, gatewayUrlsMgt, gatewayUrlsWrk, keyMangerUrl, backEndServerUrl;
    protected User user;
    private static final long WAIT_TIME = 45 * 1000;
    protected APIPublisherRestClient apiPublisher;
    protected APIStoreRestClient apiStore;
    protected RestAPIPublisherImpl restAPIPublisher;
    protected RestAPIStoreImpl restAPIStore;
    protected RestAPIAdminImpl restAPIAdmin;
    protected UserManagementClient userManagementClient;
    protected RemoteUserStoreManagerServiceClient remoteUserStoreManagerServiceClient;
    protected ClaimMetaDataMgtAdminClient remoteClaimMetaDataMgtAdminClient;
    protected OAuthAdminServiceClient oAuthAdminServiceClient;
    protected ApplicationManagementClient applicationManagementClient;
    protected TenantManagementServiceClient tenantManagementServiceClient;
    protected String publisherURLHttp;
    protected String publisherURLHttps;
    protected String keyManagerHTTPSURL;
    protected String gatewayHTTPSURL;
    protected String storeURLHttp;
    protected String storeURLHttps;
    protected String keymanagerSessionCookie;
    protected String keymanagerSuperTenantSessionCookie;
    protected final int inboundWebSocketPort = 9099;
    protected final int portOffset = 500;  //This need to be properly fixed rather than hard coding

    /**
     * This method will initialize test environment
     * based on user mode and configuration given at automation.xml
     *
     * @throws APIManagerIntegrationTestException - if test configuration init fails
     */
    protected void init() throws APIManagerIntegrationTestException {
        userMode = TestUserMode.SUPER_TENANT_ADMIN;
        init(userMode);
    }

    /**
     * init the object with user mode , create context objects and get session cookies
     *
     * @param userMode - user mode to run the tests
     * @throws APIManagerIntegrationTestException - if test configuration init fails
     */
    protected void init(TestUserMode userMode) throws APIManagerIntegrationTestException {

        apimTestCaseUtils = new APIMTestCaseUtils();

        try {
            //create store server instance based on configuration given at automation.xml
            storeContext =
                    new AutomationContext(APIMIntegrationConstants.AM_PRODUCT_GROUP_NAME,
                                          APIMIntegrationConstants.AM_STORE_INSTANCE, userMode);
            storeUrls = new APIMURLBean(storeContext.getContextUrls());

            //create publisher server instance based on configuration given at automation.xml
            publisherContext =
                    new AutomationContext(APIMIntegrationConstants.AM_PRODUCT_GROUP_NAME,
                                          APIMIntegrationConstants.AM_PUBLISHER_INSTANCE, userMode);
            publisherUrls = new APIMURLBean(publisherContext.getContextUrls());

            //create gateway server instance based on configuration given at automation.xml
            gatewayContextMgt =
                    new AutomationContext(APIMIntegrationConstants.AM_PRODUCT_GROUP_NAME,
                                          APIMIntegrationConstants.AM_GATEWAY_MGT_INSTANCE, userMode);
            gatewayUrlsMgt = new APIMURLBean(gatewayContextMgt.getContextUrls());

            gatewayContextWrk =
                    new AutomationContext(APIMIntegrationConstants.AM_PRODUCT_GROUP_NAME,
                                          APIMIntegrationConstants.AM_GATEWAY_WRK_INSTANCE, userMode);
            gatewayUrlsWrk = new APIMURLBean(gatewayContextWrk.getContextUrls());

            keyManagerContext = new AutomationContext(APIMIntegrationConstants.AM_PRODUCT_GROUP_NAME,
                                                      APIMIntegrationConstants.AM_KEY_MANAGER_INSTANCE, userMode);
            keyMangerUrl = new APIMURLBean(keyManagerContext.getContextUrls());

            backEndServer = new AutomationContext(APIMIntegrationConstants.AM_PRODUCT_GROUP_NAME,
                                                  APIMIntegrationConstants.BACKEND_SERVER_INSTANCE, userMode);
            backEndServerUrl = new APIMURLBean(backEndServer.getContextUrls());

            executionMode = gatewayContextMgt.getConfigurationValue(ContextXpathConstants.EXECUTION_ENVIRONMENT);

            user = storeContext.getContextTenant().getContextUser();

            superTenantKeyManagerContext = new AutomationContext(APIMIntegrationConstants.AM_PRODUCT_GROUP_NAME,
                                                      APIMIntegrationConstants.AM_KEY_MANAGER_INSTANCE,
                                                      TestUserMode.SUPER_TENANT_ADMIN);

            keymanagerSessionCookie = createSession(keyManagerContext);
            publisherURLHttp = publisherUrls.getWebAppURLHttp();
            publisherURLHttps = publisherUrls.getWebAppURLHttps();
            keyManagerHTTPSURL = keyMangerUrl.getWebAppURLHttps();
            gatewayHTTPSURL = gatewayUrlsWrk.getWebAppURLNhttps();

            storeURLHttp = storeUrls.getWebAppURLHttp();
            storeURLHttps = storeUrls.getWebAppURLHttps();
            apiPublisher = new APIPublisherRestClient(publisherURLHttp);
            apiStore = new APIStoreRestClient(storeURLHttp);
            restAPIPublisher = new RestAPIPublisherImpl(
                    publisherContext.getContextTenant().getContextUser().getUserNameWithoutDomain(),
                    publisherContext.getContextTenant().getContextUser().getPassword(),
                    publisherContext.getContextTenant().getDomain(), publisherURLHttps);
            restAPIStore =
                    new RestAPIStoreImpl(storeContext.getContextTenant().getContextUser().getUserNameWithoutDomain(),
                            storeContext.getContextTenant().getContextUser().getPassword(),
                            storeContext.getContextTenant().getDomain(), storeURLHttps);
            restAPIAdmin = new RestAPIAdminImpl(publisherContext.getContextTenant().getContextUser().getUserNameWithoutDomain(),
                    publisherContext.getContextTenant().getContextUser().getPassword(),
                    publisherContext.getContextTenant().getDomain(), publisherURLHttps);
            try {
                keymanagerSuperTenantSessionCookie = new LoginLogoutClient(superTenantKeyManagerContext).login();
                userManagementClient = new UserManagementClient(
                        keyManagerContext.getContextUrls().getBackEndUrl(), keymanagerSessionCookie);
                remoteUserStoreManagerServiceClient = new RemoteUserStoreManagerServiceClient(
                        keyManagerContext.getContextUrls().getBackEndUrl(), keymanagerSessionCookie);

                tenantManagementServiceClient = new TenantManagementServiceClient(
                        superTenantKeyManagerContext.getContextUrls().getBackEndUrl(),
                        keymanagerSuperTenantSessionCookie);
                remoteClaimMetaDataMgtAdminClient =
                        new ClaimMetaDataMgtAdminClient(keyManagerContext.getContextUrls().getBackEndUrl(),
                                keymanagerSessionCookie);
                oAuthAdminServiceClient =
                        new OAuthAdminServiceClient(keyManagerContext.getContextUrls().getBackEndUrl(),
                                keymanagerSessionCookie);
                applicationManagementClient =
                        new ApplicationManagementClient(keyManagerContext.getContextUrls().getBackEndUrl(),
                                keymanagerSessionCookie);
            } catch (Exception e) {
                throw new APIManagerIntegrationTestException(e.getMessage(), e);
            }

        } catch (XPathExpressionException e) {
            log.error("APIM test environment initialization failed", e);
            throw new APIManagerIntegrationTestException("APIM test environment initialization failed", e);
        }

    }

    /**
     * init the object with tenant domain, user key and instance of store,publisher and gateway
     * create context objects and construct URL bean
     *
     * @param domainKey - tenant domain key
     * @param userKey   - tenant user key
     * @throws APIManagerIntegrationTestException - if test configuration init fails
     */
    protected void init(String domainKey, String userKey)
            throws APIManagerIntegrationTestException {

        try {
            //create store server instance based configuration given at automation.xml
            storeContext =
                    new AutomationContext(APIMIntegrationConstants.AM_PRODUCT_GROUP_NAME,
                                          APIMIntegrationConstants.AM_STORE_INSTANCE, domainKey, userKey);
            storeUrls = new APIMURLBean(storeContext.getContextUrls());

            //create publisher server instance
            publisherContext =
                    new AutomationContext(APIMIntegrationConstants.AM_PRODUCT_GROUP_NAME,
                                          APIMIntegrationConstants.AM_PUBLISHER_INSTANCE, domainKey, userKey);
            publisherUrls = new APIMURLBean(publisherContext.getContextUrls());

            //create gateway server instance
            gatewayContextMgt =
                    new AutomationContext(APIMIntegrationConstants.AM_PRODUCT_GROUP_NAME,
                                          APIMIntegrationConstants.AM_GATEWAY_MGT_INSTANCE, domainKey, userKey);
            gatewayUrlsMgt = new APIMURLBean(gatewayContextMgt.getContextUrls());

            gatewayContextWrk =
                    new AutomationContext(APIMIntegrationConstants.AM_PRODUCT_GROUP_NAME,
                                          APIMIntegrationConstants.AM_GATEWAY_WRK_INSTANCE, domainKey, userKey);
            gatewayUrlsWrk = new APIMURLBean(gatewayContextWrk.getContextUrls());

            keyManagerContext = new AutomationContext(APIMIntegrationConstants.AM_PRODUCT_GROUP_NAME,
                                                      APIMIntegrationConstants.AM_KEY_MANAGER_INSTANCE, domainKey, userKey);
            keyMangerUrl = new APIMURLBean(keyManagerContext.getContextUrls());

            backEndServer = new AutomationContext(APIMIntegrationConstants.AM_PRODUCT_GROUP_NAME,
                                                  APIMIntegrationConstants.BACKEND_SERVER_INSTANCE, domainKey, userKey);
            backEndServerUrl = new APIMURLBean(backEndServer.getContextUrls());

            user = storeContext.getContextTenant().getContextUser();

        } catch (XPathExpressionException e) {
            log.error("Init failed", e);
            throw new APIManagerIntegrationTestException("APIM test environment initialization failed", e);
        }

    }

    /**
     * @param relativeFilePath - file path to load config
     * @throws APIManagerIntegrationTestException - Throws if load synapse configuration from file path
     *                                            fails
     */
    protected void loadSynapseConfigurationFromClasspath(String relativeFilePath,
                                                         AutomationContext automationContext,
                                                         String sessionCookie)
            throws APIManagerIntegrationTestException {

        relativeFilePath = relativeFilePath.replaceAll("[\\\\/]", Matcher.quoteReplacement(File.separator));
        OMElement synapseConfig;

        try {
            synapseConfig = APIMTestCaseUtils.loadResource(relativeFilePath);
            updateSynapseConfiguration(synapseConfig, automationContext, sessionCookie);

        } catch (FileNotFoundException e) {
            log.error("synapse config loading issue", e);
            throw new APIManagerIntegrationTestException("synapse config loading issue", e);
        } catch (XMLStreamException e) {
            log.error("synapse config loading issue", e);
            throw new APIManagerIntegrationTestException("synapse config loading issue", e);
        }
    }

    /**
     * @param automationContext - automation context instance of given server
     * @return - created session cookie variable
     * @throws APIManagerIntegrationTestException - Throws if creating session cookie fails
     */
    protected String createSession(AutomationContext automationContext)
            throws APIManagerIntegrationTestException {
        LoginLogoutClient loginLogoutClient;
        try {
            loginLogoutClient = new LoginLogoutClient(automationContext);
            return loginLogoutClient.login();
        } catch (Exception e) {
            log.error("session creation error", e);
            throw new APIManagerIntegrationTestException("session creation error", e);
        }
    }

    /**
     * Get test artifact resources location
     *
     * @return - absolute patch of test artifact directory
     */
    protected String getAMResourceLocation() {
        return FrameworkPathUtil.getSystemResourceLocation() + "artifacts" + File.separator + "AM";
    }

    /**
     * update synapse config to server
     *
     * @param synapseConfig     - config to upload
     * @param automationContext - automation context of the server instance
     * @param sessionCookie     -  logged in session cookie
     * @throws APIManagerIntegrationTestException - If synapse config update fails
     */
    protected void updateSynapseConfiguration(OMElement synapseConfig,
                                              AutomationContext automationContext,
                                              String sessionCookie)
            throws APIManagerIntegrationTestException {

        if (synapseConfiguration == null) {
            synapseConfiguration = synapseConfig;
        } else {
            Iterator<OMElement> itr = synapseConfig.cloneOMElement().getChildElements();  //ToDo
            while (itr.hasNext()) {
                synapseConfiguration.addChild(itr.next());
            }
        }

        try {

            APIMTestCaseUtils.updateSynapseConfiguration(synapseConfig,
                                                         automationContext.getContextUrls().getBackEndUrl(),
                                                         sessionCookie);

        } catch (Exception e) {
            log.error("synapse config  upload error", e);
            throw new APIManagerIntegrationTestException("synapse config  upload error", e);
        }
    }

    protected String getStoreURLHttp() {
        return storeUrls.getWebAppURLHttp();
    }

    protected String getStoreURLHttps() {
        return storeUrls.getWebAppURLHttps();
    }

    protected String getPublisherURLHttp() {
        return publisherUrls.getWebAppURLHttp();
    }

    protected String getPublisherURLHttps() {
        return publisherUrls.getWebAppURLHttps();
    }

    protected String getGatewayMgtURLHttp() {
        return gatewayUrlsMgt.getWebAppURLHttp();
    }

    protected String getGatewayMgtBackendURLHttps() {
        return gatewayUrlsMgt.getWebAppURLHttp();
    }

    protected String getGatewayMgtURLHttps() {
        return gatewayUrlsMgt.getWebAppURLHttps();
    }

    protected String getGatewayURLHttp() {
        return gatewayUrlsWrk.getWebAppURLHttp();
    }

    protected String getGatewayURLNhttp() {
        return gatewayUrlsWrk.getWebAppURLNhttp();
    }

    protected String getGatewayURLHttps() {
        return gatewayUrlsWrk.getWebAppURLHttps();
    }

    protected String getGatewayURLNhttps() {
        return gatewayUrlsWrk.getWebAppURLNhttps();
    }

    protected String getKeyManagerURLHttp() {
        return keyMangerUrl.getWebAppURLHttp();
    }

    protected String getKeyManagerURLHttps() throws XPathExpressionException {
        return keyManagerContext.getContextUrls().getBackEndUrl().replace("/services", "");
    }

    protected String getAPIInvocationURLHttp(String apiContext) throws XPathExpressionException {
        return gatewayContextWrk.getContextUrls().getServiceUrl().replace("/services", "") + "/" + apiContext;
    }

    protected String getAPIInvocationURLHttp(String apiContext, String version)
            throws XPathExpressionException {
        return gatewayContextWrk.getContextUrls().getServiceUrl().replace("/services", "") + "/" + apiContext + "/" + version;
    }

    /**
     * To get the API invocation in https with context and version.
     *
     * @param apiContext Relevant context of the API.
     * @param version    Version of the API.
     * @return Https url related with api context and version.
     * @throws XPathExpressionException XPath Express Exception.
     */
    protected String getAPIInvocationURLHttps(String apiContext, String version) throws XPathExpressionException {
        return gatewayContextWrk.getContextUrls().getSecureServiceUrl().replace("/services", "") + "/" + apiContext
                + "/" + version;
    }

    protected String getWebSocketAPIInvocationURL(String apiContext, String version)
            throws XPathExpressionException {
        String url = gatewayContextWrk.getContextUrls().getServiceUrl().replace("/services", "").
                replace("http", "ws");
        url = url.substring(0, url.lastIndexOf(":") + 1) + (inboundWebSocketPort + portOffset) + "/" + apiContext + "/" + version;
        return url;
    }

    protected String getWebSocketTenantAPIInvocationURL(String apiContext, String version, String tenantDomain)
            throws XPathExpressionException  {
        String url = gatewayContextWrk.getContextUrls().getServiceUrl().replace("/services", "").
                replace("http", "ws");

        url = url.substring(0, url.lastIndexOf(":") + 1) + (inboundWebSocketPort + portOffset)
              + "/t/" + tenantDomain + "/" + apiContext + "/" + version;
        return url;
    }

    protected String getAPIInvocationURLHttps(String apiContext) throws XPathExpressionException {
        return gatewayContextWrk.getContextUrls().getSecureServiceUrl().replace("/services", "") + "/" + apiContext;
    }

    protected String getBackendEndServiceEndPointHttp(String serviceName) {
        return backEndServerUrl.getWebAppURLHttp() + serviceName;
    }

    protected String getBackendEndServiceEndPointHttps(String serviceName) {
        return backEndServerUrl.getWebAppURLHttps() + serviceName;
    }

    protected String getSuperTenantAPIInvocationURLHttp(String apiContext, String version)
            throws XPathExpressionException {
        return gatewayContextWrk.getContextUrls().getServiceUrl().replace("/services", "")
                       .replace("/t/" + user.getUserDomain(), "") + "/" + apiContext + "/" + version;
    }

    /**
     * Cleaning up the API manager by removing all APIs and applications other than default application
     *
     * @throws APIManagerIntegrationTestException - occurred when calling the apis
     */
    protected void cleanUp() throws Exception {

        ApplicationListDTO applicationListDTO = restAPIStore.getAllApps();
        for (ApplicationInfoDTO applicationInfoDTO: applicationListDTO.getList()) {
            SubscriptionListDTO subsDTO = restAPIStore
                    .getAllSubscriptionsOfApplication(applicationInfoDTO.getApplicationId());
            if (subsDTO != null) {
                for (SubscriptionDTO subscriptionDTO : subsDTO.getList()) {
                    restAPIStore.removeSubscription(subscriptionDTO.getSubscriptionId());
                }
            }
            if (!APIMIntegrationConstants.OAUTH_DEFAULT_APPLICATION_NAME.equals(applicationInfoDTO.getName())) {
                restAPIStore.deleteApplication(applicationInfoDTO.getApplicationId());
            }
        }

        APIProductListDTO allApiProducts = restAPIPublisher.getAllApiProducts();

        List<APIProductInfoDTO> apiProductListDTO = allApiProducts.getList();

        if (apiProductListDTO != null) {
            for(APIProductInfoDTO apiProductInfoDTO : apiProductListDTO) {
                restAPIPublisher.deleteApiProduct(apiProductInfoDTO.getId());
            }
        }

        APIListDTO apiListDTO = restAPIPublisher.getAllAPIs();
        if (apiListDTO != null) {
            for (APIInfoDTO apiInfoDTO: apiListDTO.getList()) {
                restAPIPublisher.deleteAPI(apiInfoDTO.getId());
            }
        }
    }

    protected void verifyResponse(HttpResponse httpResponse) throws JSONException {
        Assert.assertNotNull(httpResponse, "Response object is null");
        log.info("Response Code : " + httpResponse.getResponseCode());
        log.info("Response Message : " + httpResponse.getData());
        Assert.assertEquals(httpResponse.getResponseCode(), HttpStatus.SC_OK, "Response code is not as expected");
        JSONObject responseData = new JSONObject(httpResponse.getData());
        Assert.assertFalse(responseData.getBoolean(APIMIntegrationConstants.API_RESPONSE_ELEMENT_NAME_ERROR), "Error message received " + httpResponse.getData());

    }

    /**
     * This method can be used to wait for API deployment sync in distributed and clustered environment
     * APIStatusMonitor will be invoked to get API related data and then verify that data matches with
     * expected response provided.
     */
    protected void waitForAPIDeployment() {
        try {
            if (executionMode.equalsIgnoreCase(String.valueOf(ExecutionEnvironment.PLATFORM))) {
                Thread.sleep(WAIT_TIME);
            } else {
                Thread.sleep(15000);
            }
        } catch (InterruptedException ignored) {

        }
    }

    /**
     * This method can be used to wait for API deployment sync in distributed and clustered environment
     * APIStatusMonitor will be invoked to get API related data and then verify that data matches with
     * expected response provided.
     *
     * @param apiProvider      - Provider of the API
     * @param apiName          - API name
     * @param apiVersion       - API version
     * @param expectedResponse - Expected response
     * @throws APIManagerIntegrationTestException - Throws if something goes wrong
     */
    protected void waitForAPIDeploymentSync(String apiProvider, String apiName, String apiVersion,
                                            String expectedResponse)
            throws APIManagerIntegrationTestException, XPathExpressionException {

        long currentTime = System.currentTimeMillis();
        long waitTime = currentTime + WAIT_TIME;
        String colonSeparatedHeader =
                keyManagerContext.getContextTenant().getTenantAdmin().getUserName() + ":" + keyManagerContext
                        .getContextTenant().getTenantAdmin().getPassword();
        String authorizationHeader = "Basic "+new String(Base64.encodeBase64(colonSeparatedHeader.getBytes()));
        Map headerMap = new HashMap();
        headerMap.put("Authorization",authorizationHeader);
        String tenantIdentifier = getTenantIdentifier(apiProvider);

        while (waitTime > System.currentTimeMillis()) {
            HttpResponse response = null;
            try {
                response = HttpRequestUtil.doGet(getGatewayURLHttp() +
                                                          "APIStatusMonitor/apiInformation/api/" +
                                                          tenantIdentifier +
                                                          apiName + "/" + apiVersion, headerMap);
            } catch (IOException ignored) {
                log.warn("WebAPP:" + " APIStatusMonitor not yet deployed or" + " API :" + apiName + " not yet " +
                        "deployed " + " with provider: " + apiProvider);
            }

            log.info("WAIT for availability of API: " + apiName + " with version: " + apiVersion
                     + " with provider: " + apiProvider + " with Tenant Identifier: " + tenantIdentifier
                     + " with expected response : " + expectedResponse);

            if (response != null) {
                log.info("Data: " + response.getData());
                if (response.getData().contains(expectedResponse)) {
                    log.info("API :" + apiName + " with version: " + apiVersion +
                             " with expected response " + expectedResponse + " found");
                    break;
                } else {
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException ignored) {

                    }
                }
            }
        }
    }

    /**
     * This method can be used to wait for API Un-deployment sync in distributed and clustered environment
     * APIStatusMonitor will be invoked to get API related data and then verify that data matches with
     * expected response provided.
     *
     * @param apiProvider      - Provider of the API
     * @param apiName          - API name
     * @param apiVersion       - API version
     * @param expectedResponse - Expected response
     * @throws APIManagerIntegrationTestException - Throws if something goes wrong
     */
    protected void waitForAPIUnDeploymentSync(String apiProvider, String apiName, String apiVersion,
                                              String expectedResponse)
            throws APIManagerIntegrationTestException {

        long currentTime = System.currentTimeMillis();
        long waitTime = currentTime + WAIT_TIME;

        String tenantIdentifier = getTenantIdentifier(apiProvider);
        String colonSeparatedHeader = user.getUserName()+":"+user.getPassword();
        String authorizationHeader = "Basic "+Base64.encodeBase64(colonSeparatedHeader.getBytes()).toString();
        Map headerMap = new HashMap();
        headerMap.put("Authorization",authorizationHeader);
        while (waitTime > System.currentTimeMillis()) {
            HttpResponse response = null;
            try{
                response = HttpRequestUtil.doGet(getGatewayURLHttp() +
                                                          "APIStatusMonitor/apiInformation/api/" +
                                                          tenantIdentifier +
                                                          apiName + "/" + apiVersion, headerMap);
                } catch (IOException ignored) {
                    log.warn("WebAPP:" + " APIStatusMonitor not yet deployed or" + " API :" + apiName + " not yet deployed " + " with provider: " + apiProvider);
                }

            log.info("WAIT for meta data sync of API :" + apiName + " with version: " + apiVersion + " with provider: " + apiProvider +
                     " without entry : " + expectedResponse);

            if (response != null) {
                if (!response.getData().contains(expectedResponse)) {
                    log.info("API :" + apiName + " with version: " + apiVersion +
                             " with expected response " + expectedResponse + " not found");
                    break;
                } else {
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException ignored) {

                    }
                }
            }
        }
    }

    /**
     * This returns "tenatDomain/tenantId/" string
     * @param apiProvider
     */
    private String getTenantIdentifier(String apiProvider) throws APIManagerIntegrationTestException {
        int tenantId = -1234;
        String providerTenantDomain = MultitenantUtils.getTenantDomain(apiProvider);
        try{
            if(!MultitenantConstants.SUPER_TENANT_DOMAIN_NAME.equals(providerTenantDomain)){
                keymanagerSuperTenantSessionCookie = new LoginLogoutClient(superTenantKeyManagerContext).login();
                tenantManagementServiceClient = new TenantManagementServiceClient(
                        superTenantKeyManagerContext.getContextUrls().getBackEndUrl(),
                        keymanagerSuperTenantSessionCookie);
                TenantInfoBean tenant = tenantManagementServiceClient.getTenant(providerTenantDomain);
                if(tenant == null){
                    log.info("tenant is null: " + providerTenantDomain);
                } else {
                    tenantId = tenant.getTenantId();
                }
                //forced tenant loading
                new LoginLogoutClient(gatewayContextWrk).login();
            }
        } catch (Exception e) {
            throw new APIManagerIntegrationTestException(e.getMessage(), e);
        }
        return providerTenantDomain + "/" + tenantId + "/";
    }

    protected Header pickHeader(Header[] headers, String requiredHeader){
        if (requiredHeader == null){
            return null;
        }
        for (Header header : headers) {
            if(requiredHeader.equals(header.getName())){
                return header;
            }
        }
        return null;
    }

    protected RestAPIPublisherImpl getRestAPIPublisherForUser(String user, String pass, String tenantDomain) {
        return new RestAPIPublisherImpl(user, pass, tenantDomain, publisherURLHttps);
    }

    protected RestAPIStoreImpl getRestAPIStoreForUser(String user, String pass, String tenantDomain) {
        return new RestAPIStoreImpl(user, pass, tenantDomain, storeURLHttps);
    }

    protected RestAPIStoreImpl getRestAPIStoreForAnonymousUser(String tenantDomain) {
        return new RestAPIStoreImpl(tenantDomain, storeURLHttps);
    }

    protected void waitForKeyManagerDeployment(String tenantDomain, String keyManagerName)
            throws XPathExpressionException, UnsupportedEncodingException {

        long currentTime = System.currentTimeMillis();
        long waitTime = currentTime + WAIT_TIME;
        String colonSeparatedHeader =
                keyManagerContext.getContextTenant().getTenantAdmin().getUserName() + ":" + keyManagerContext
                        .getContextTenant().getTenantAdmin().getPassword();
        String authorizationHeader = "Basic " + new String(Base64.encodeBase64(colonSeparatedHeader.getBytes()));
        Map headerMap = new HashMap();
        keyManagerName = URLEncoder.encode(keyManagerName, "utf8").replaceAll("\\+", "%20");
        headerMap.put("Authorization", authorizationHeader);

        while (waitTime > System.currentTimeMillis()) {
            HttpResponse response = null;
            try {
                response = HttpRequestUtil.doGet(getGatewayURLHttp() +
                        "APIStatusMonitor/keyManagerInformation/" + tenantDomain + "/" + keyManagerName, headerMap);
            } catch (IOException ignored) {
                log.warn("WebAPP:" + " APIStatusMonitor not yet deployed or" + " KeyManager :" + keyManagerName +
                        " not yet " +
                        "deployed " + " in tenantDomain " + tenantDomain);
            }

            log.info("WAIT for availability of KeyManager: " + keyManagerName + " in tenant Domain : " + tenantDomain);
            if (response != null) {
                log.info("Status Code: " + response.getResponseCode());
                if (response.getResponseCode() == 200) {
                    log.info("Key Manager :" + keyManagerName + " Exist in tenant" + tenantDomain);
                    break;
                } else {
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException ignored) {

                    }
                }
            }
        }
    }
}
