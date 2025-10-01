/*
 *Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
package org.wso2.am.scenario.test.common;

import org.apache.axiom.om.OMElement;
import org.apache.axis2.AxisFault;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.testng.Assert;
import org.wso2.am.integration.clients.store.api.ApiException;
import org.wso2.am.integration.clients.store.api.v1.dto.APIDTO;
import org.wso2.am.integration.test.ClientAuthenticator;
import org.wso2.am.integration.test.impl.RestAPIAdminImpl;
import org.wso2.am.integration.test.impl.RestAPIPublisherImpl;
import org.wso2.am.integration.test.impl.RestAPIStoreImpl;
import org.wso2.am.integration.test.utils.APIManagerIntegrationTestException;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.bean.APIMURLBean;
import org.wso2.am.integration.test.utils.bean.DCRParamRequest;
import org.wso2.am.integration.test.utils.clients.APIPublisherRestClient;
import org.wso2.am.integration.test.utils.generic.APIMTestCaseUtils;
import org.wso2.am.scenario.test.common.clients.UserMgtClient;
import org.wso2.carbon.apimgt.api.APIManagementException;
import org.wso2.carbon.automation.engine.context.AutomationContext;
import org.wso2.carbon.automation.engine.context.ContextXpathConstants;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.engine.context.beans.User;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;
import org.wso2.carbon.base.MultitenantConstants;
import org.wso2.carbon.integration.common.admin.client.AuthenticatorClient;
import org.wso2.carbon.integration.common.admin.client.TenantManagementServiceClient;
import org.wso2.carbon.integration.common.admin.client.UserManagementClient;
import org.wso2.carbon.integration.common.utils.LoginLogoutClient;
import org.wso2.carbon.tenant.mgt.stub.beans.xsd.TenantInfoBean;
import org.wso2.carbon.user.mgt.stub.UserAdminUserAdminException;
import org.wso2.carbon.utils.multitenancy.MultitenantUtils;

import javax.xml.xpath.XPathExpressionException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

public class ScenarioTestBase {

    private static final String INPUTS_LOCATION = System.getenv("DATA_BUCKET_LOCATION");
    private static final String INFRASTRUCTURE_PROPERTIES = "deployment.properties";
    private static final Log log = LogFactory.getLog(ScenarioTestBase.class);
    private static final long WAIT_TIME = 45 * 1000;
    protected static String publisherURL;
    protected static String storeURL;
    protected static String keyManagerURL;
    protected static String gatewayHttpsURL;
    protected static String serviceEndpoint;
    protected static String adminURL;
    protected static String baseUrl;
    protected static String host;
    private static Properties infraProperties;
    public static final String PUBLISHER_URL = "PublisherUrl";
    public static final String STORE_URL = "StoreUrl";
    public static final String ADMIN_URL = "AdminUrl";
    public static final String KEYAMANAGER_URL = "KeyManagerUrl";
    public static final String GATEWAYHTTPS_URL = "GatewayHttpsUrl";
    public static final String SERVICE_ENDPOINT = "CarbonServerUrl";
    protected static String resourceLocation = System.getProperty("framework.resource.location");
    protected String publisherURLHttp;
    protected String publisherURLHttps;
    protected String keyManagerHTTPSURL;
    protected String gatewayHTTPSURL;
    protected String storeURLHttp;
    protected String storeURLHttps;
    protected TestUserMode userMode;
    protected APIMTestCaseUtils apimTestCaseUtils;
    protected AutomationContext storeContext, publisherContext, keyManagerContext, gatewayContextMgt,
            gatewayContextWrk, backEndServer, superTenantKeyManagerContext;
    protected OMElement synapseConfiguration;
    protected APIMURLBean storeUrls, publisherUrls, gatewayUrlsMgt, gatewayUrlsWrk, keyMangerUrl, backEndServerUrl;
    protected String executionMode;
    protected String keymanagerSessionCookie;
    protected String keymanagerSuperTenantSessionCookie;
    protected APIPublisherRestClient apiPublisher;
    protected org.wso2.am.integration.test.utils.clients.APIStoreRestClient apiStore;
    protected RestAPIPublisherImpl restAPIPublisher;
    protected RestAPIStoreImpl restAPIStore;
    protected RestAPIAdminImpl restAPIAdmin;
    protected UserManagementClient userManagementClient;
    protected TenantManagementServiceClient tenantManagementServiceClient;
    protected User user;


    /**
     * This method will initialize test environment
     * based on user mode and configuration given at automation.xml
     *
     * @throws APIManagerIntegrationTestException - if test configuration init fails
     */
    protected void init() throws Exception {
        userMode = TestUserMode.SUPER_TENANT_ADMIN;
        init(userMode);
    }

    /**
     * init the object with user mode , create context objects and get session cookies
     *
     * @param userMode - user mode to run the tests
     * @throws APIManagerIntegrationTestException - if test configuration init fails
     */
    protected void init(TestUserMode userMode) throws Exception {

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

            if (userMode.equals(TestUserMode.SUPER_TENANT_ADMIN)) {
                keymanagerSessionCookie = createSession(keyManagerContext);
            }
            publisherURLHttp = publisherUrls.getWebAppURLHttp();
            publisherURLHttps = publisherUrls.getWebAppURLHttps();
            keyManagerHTTPSURL = keyMangerUrl.getWebAppURLHttps();
            gatewayHTTPSURL = gatewayUrlsWrk.getWebAppURLNhttps();

            storeURLHttp = storeUrls.getWebAppURLHttp();
            storeURLHttps = storeUrls.getWebAppURLHttps();
            apiPublisher = new APIPublisherRestClient(publisherURLHttp);
            apiStore = new org.wso2.am.integration.test.utils.clients.APIStoreRestClient(storeURLHttp);

            String dcrURL = baseUrl + "client-registration/v0.17/register";

            //DCR call for publisher app
            DCRParamRequest publisherParamRequest = new DCRParamRequest(UUID.randomUUID().toString(), RestAPIPublisherImpl.callBackURL,
                    RestAPIPublisherImpl.tokenScope, RestAPIPublisherImpl.appOwner, RestAPIPublisherImpl.grantType, dcrURL,
                    RestAPIPublisherImpl.username, RestAPIPublisherImpl.password,
                    APIMIntegrationConstants.SUPER_TENANT_DOMAIN);
            ClientAuthenticator.makeDCRRequest(publisherParamRequest);
            //DCR call for dev portal app
            DCRParamRequest devPortalParamRequest = new DCRParamRequest(UUID.randomUUID().toString(), RestAPIStoreImpl.callBackURL,
                    RestAPIStoreImpl.tokenScope, RestAPIStoreImpl.appOwner, RestAPIStoreImpl.grantType, dcrURL,
                    RestAPIStoreImpl.username, RestAPIStoreImpl.password,
                    APIMIntegrationConstants.SUPER_TENANT_DOMAIN);
            ClientAuthenticator.makeDCRRequest(devPortalParamRequest);

            //DCR call for dev portal app
            DCRParamRequest adminPortalParamRequest = new DCRParamRequest(UUID.randomUUID().toString(), RestAPIAdminImpl.callBackURL,
                    RestAPIAdminImpl.tokenScope, RestAPIAdminImpl.appOwner, RestAPIAdminImpl.grantType, dcrURL,
                    RestAPIAdminImpl.username, RestAPIAdminImpl.password,
                    APIMIntegrationConstants.SUPER_TENANT_DOMAIN);

            ClientAuthenticator.makeDCRRequest(adminPortalParamRequest);

            restAPIPublisher = new RestAPIPublisherImpl(
                    publisherContext.getContextTenant().getTenantUserList().get(0).getUserNameWithoutDomain(),
                    publisherContext.getContextTenant().getTenantUserList().get(0).getPassword(),
                    publisherContext.getContextTenant().getDomain(), baseUrl);
            restAPIStore =
                    new RestAPIStoreImpl(
                            storeContext.getContextTenant().getTenantUserList().get(1).getUserNameWithoutDomain(),
                            storeContext.getContextTenant().getTenantUserList().get(1).getPassword(),
                            storeContext.getContextTenant().getDomain(), baseUrl);

            restAPIAdmin = new RestAPIAdminImpl(
                    storeContext.getContextTenant().getTenantAdmin().getUserNameWithoutDomain(),
                    storeContext.getContextTenant().getTenantAdmin().getPassword(),
                    storeContext.getContextTenant().getDomain(), baseUrl);

            storeURLHttps = baseUrl;
            publisherURLHttps = baseUrl;
            log.info("Logging URL's");
            log.info("baseUrl: " + baseUrl);
            log.info("storeURLHttps: " + storeURLHttps);
            log.info("publisherURLHttps: " + publisherURLHttps);
            log.info("DCR end point: " + dcrURL);
            log.info("Logging URL's ENDED");

            try {
                if (userMode.equals(TestUserMode.SUPER_TENANT_ADMIN)) {
                    keymanagerSuperTenantSessionCookie = new LoginLogoutClient(superTenantKeyManagerContext).login();
                    userManagementClient = new UserManagementClient(
                            keyManagerContext.getContextUrls().getBackEndUrl(), keymanagerSessionCookie);
                    tenantManagementServiceClient = new TenantManagementServiceClient(
                            superTenantKeyManagerContext.getContextUrls().getBackEndUrl(),
                            keymanagerSuperTenantSessionCookie);
                }
            } catch (Exception e) {
                throw new APIManagerIntegrationTestException(e.getMessage(), e);
            }

        } catch (XPathExpressionException e) {
            log.error("APIM test environment initialization failed", e);
            throw new APIManagerIntegrationTestException("APIM test environment initialization failed", e);
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

    public ScenarioTestBase() {
        setup();
    }

    protected static void setup() {
        infraProperties = getDeploymentProperties();
        publisherURL = infraProperties.getProperty(PUBLISHER_URL);
        if (publisherURL == null) {
            publisherURL = "https://localhost:9443/publisher";
        }
        keyManagerURL = infraProperties.getProperty(KEYAMANAGER_URL);
        if (StringUtils.isEmpty(keyManagerURL)) {
            keyManagerURL = "https://localhost:9443/services/";
        }
        storeURL = infraProperties.getProperty(STORE_URL);
        if (storeURL == null) {
            storeURL = "https://localhost:9443/store";
        }
        gatewayHttpsURL = infraProperties.getProperty(GATEWAYHTTPS_URL);
        if (gatewayHttpsURL == null) {
            gatewayHttpsURL = "https://localhost:8243";
        }
        serviceEndpoint = infraProperties.getProperty(SERVICE_ENDPOINT);
        if (serviceEndpoint == null) {
            serviceEndpoint = "https://localhost:9443/services/";
        }
        adminURL = infraProperties.getProperty(ADMIN_URL);
        if (adminURL == null) {
            adminURL = "https://localhost:9443/admin";
        }


        if (StringUtils.isNotEmpty(System.getenv("DATA_BUCKET_LOCATION"))) {
            String[] urlProps = keyManagerURL.split("services/");
            baseUrl = urlProps[0];
            String[] urlProps2 = urlProps[0].split("https://");
            if (StringUtils.contains(urlProps2[1], "944")) {
                String[] urlProps3 = urlProps2[1].split(":9443/");
                host = urlProps3[0];
            } else {
                String[] urlProps4 = urlProps2[1].split("/");
                host = urlProps4[0];
            }
        } else {
            baseUrl = "https://localhost:9443/";
            host = "localhost";
        }
        log.info("BASE_URL>>>>>" + baseUrl);
        log.info("SERVICE URL>>>" + serviceEndpoint);
        log.info("HOST>>>>>" + host);
        setKeyStoreProperties();
    }

    /**
     * This is a utility method to load the deployment details.
     * The deployment details are available as key-value pairs in {@link #INFRASTRUCTURE_PROPERTIES},
     * {@link #INPUTS_LOCATION}.
     * <p>
     * This method loads these files into one single properties, and return it.
     *
     * @return properties the deployment properties
     */
    public static Properties getDeploymentProperties() {
        Path infraPropsFile = Paths.get(INPUTS_LOCATION + File.separator + INFRASTRUCTURE_PROPERTIES);
        Properties props = new Properties();
        loadProperties(infraPropsFile, props);

        return props;
    }

    private static void loadProperties(Path propsFile, Properties props) {
        String msg = "Deployment property file not found: ";
        if (!Files.exists(propsFile)) {
            log.warn(msg + propsFile);
            return;
        }

        try (InputStream propsIS = Files.newInputStream(propsFile)) {
            props.load(propsIS);
        } catch (IOException ex) {
            log.warn(ex.getMessage(), ex);
        }
    }

    public static void addTenantAndActivate(String domain, String adminUsername, String adminPassword)
            throws APIManagementException {
        TenantManagementServiceClient tenantManagementServiceClient = null;
        try {
            tenantManagementServiceClient = getTenantManagementServiceClient();
            TenantInfoBean tenantInfoBean = tenantManagementServiceClient.getTenant(domain);
            if (tenantInfoBean.getActive()) {
                tenantManagementServiceClient.activateTenant(domain);
            } else {
                tenantManagementServiceClient.addTenant(domain, adminUsername, adminPassword, "demo");
                tenantManagementServiceClient.activateTenant(domain);
            }
            isActivated(domain);
        } catch (Exception e) {
            throw new APIManagementException("Unable to add new tenant and activate " + domain, e);
        }
    }

    public static void deactivateAndDeleteTenant(String domain)
            throws APIManagementException {
        TenantManagementServiceClient tenantManagementServiceClient;
        try {
            tenantManagementServiceClient = getTenantManagementServiceClient();
            TenantInfoBean tenantInfoBean = tenantManagementServiceClient.getTenant(domain);
            if (tenantInfoBean.getActive()) {
                tenantManagementServiceClient.deactivateTenant(domain);
                tenantManagementServiceClient.deleteTenant(domain);
            }
            Thread.sleep(10000);
        } catch (Exception e) {
            throw new APIManagementException("Unable to add new tenant and activate " + domain, e);
        }
    }

    public static boolean isActivated(String domain) throws APIManagementException {
        TenantManagementServiceClient tenantManagementServiceClient;
        try {
            tenantManagementServiceClient = getTenantManagementServiceClient();
            boolean isActive = false;
            long maxWait = 0;
            TenantInfoBean tenantInfoBean;
            while (!isActive) {
                tenantInfoBean = tenantManagementServiceClient.getTenant(domain);
                if (tenantInfoBean.getActive()) {
                    isActive = true;
                    break;
                }
                log.info("Waiting for the tenant " + domain + " to get activated");
                Thread.sleep(3000);
                maxWait = maxWait + 3000;
                if (maxWait > 60000) {
                    log.error("Tenant domain " + domain + "activation failed");
                    break;
                }
                isActive = false;
            }
            return isActive;
        } catch (Exception e) {
            throw new APIManagementException("Unable to add new tenant and activate " + domain, e);
        }
    }

    public static TenantManagementServiceClient getTenantManagementServiceClient() throws APIManagementException {

        AuthenticatorClient authenticatorClient = null;
        try {
            authenticatorClient = new AuthenticatorClient(keyManagerURL);
            String sessionCookie = authenticatorClient.login("admin", "admin", "localhost");

            TenantManagementServiceClient tenantManagementServiceClient = new TenantManagementServiceClient(keyManagerURL,
                    sessionCookie);
            return tenantManagementServiceClient;
        } catch (Exception e) {
            throw new APIManagementException("Unable to create new tenantManagementClient ", e);
        }
    }

    private static UserManagementClient getRemoteUserManagerClient(String adminUsername, String adminPassword)
            throws AxisFault {
        UserManagementClient userManagementClient = new UserManagementClient(keyManagerURL, adminUsername,
                adminPassword);
        return userManagementClient;
    }

    private static UserMgtClient getRemoteUserMgtClient(String adminUsername, String adminPassword)
            throws AxisFault {
        return new UserMgtClient(keyManagerURL, adminUsername, adminPassword);
    }

    /**
     * Create a Client to communicate with Web Application Admin Service
     * 
     * @deprecated WebApp deployment has been moved to file-based approach during server startup
     * @return {@link org.wso2.am.integration.test.utils.webapp.WebAppDeploymentUtil}
     * @throws APIManagementException If there are any errors during initializing the client
     */
    @Deprecated
    public Object getWebAppAdminClient() throws APIManagementException {
        throw new APIManagementException("WebAppAdminClient has been replaced with file-based webapp deployment. " +
                "WebApps are now deployed during server startup using WebAppDeploymentUtil.copyWebApp()");
    }

    protected String login(String serviceEndpoint, String username, String password) throws APIManagementException {
        AuthenticatorClient authenticatorClient = null;

        try {
            authenticatorClient = new AuthenticatorClient(serviceEndpoint);
            String sessionCookie = authenticatorClient.login(username, password, host);

            return sessionCookie;
        } catch (Exception e) {
            throw new APIManagementException("Unable login to Host: " + host, e);
        }
    }

    public static void setKeyStoreProperties() {
        System.setProperty("javax.net.ssl.trustStore", resourceLocation + "/keystores/client-truststore.jks");
        System.setProperty("javax.net.ssl.trustStorePassword", "wso2carbon");
        System.setProperty("javax.net.ssl.trustStoreType", "JKS");
    }

    public void verifyResponse(HttpResponse httpResponse) throws JSONException {
        Assert.assertNotNull(httpResponse, "Response object is null");
        log.info("Response Code : " + httpResponse.getResponseCode());
        log.info("Response Message : " + httpResponse.getData());
        Assert.assertEquals(httpResponse.getResponseCode(), HttpStatus.SC_OK, "Response code is not as expected");
    }

    public void verifyNegativeResponse(HttpResponse httpResponse) throws JSONException {

        Assert.assertNotNull(httpResponse, "Response object is null");
        log.info("Response Code : " + httpResponse.getResponseCode());
        log.info("Response Message : " + httpResponse.getData());
    }

    public static void createUserWithCreatorRole(String username, String password, String adminUsername,
                                                 String adminPassword) throws APIManagementException {
        UserMgtClient userMgtClient;
        try {
            userMgtClient = getRemoteUserMgtClient(adminUsername, adminPassword);
            if (!userMgtClient.isExistingUser(username)) {
                userMgtClient.addUser(username, password, new String[]{ScenarioTestConstants.CREATOR_ROLE});
            }
        } catch (Exception e) {
            throw new APIManagementException("Unable to create user with creator role " + username, e);
        }
    }

    public static void createUserWithPublisherAndCreatorRole(String username, String password, String adminUsername,
                                                             String adminPassword) throws APIManagementException {
        UserMgtClient userMgtClient;
        try {
            userMgtClient = getRemoteUserMgtClient(adminUsername, adminPassword);

            if (!userMgtClient.isExistingUser(username)) {
                userMgtClient
                        .addUser(username, password, new String[]{ScenarioTestConstants.CREATOR_ROLE,
                                ScenarioTestConstants.PUBLISHER_ROLE});
            }
        } catch (Exception e) {
            throw new APIManagementException("Unable to create user "+ username +" with publisher and creator role ", e);
        }
    }

//    public void createUserWithPublisherRole(String username, String password, String adminUsername,
//                                            String adminPassword) throws APIManagementException {
//        UserManagementClient userManagementClient = null;
//        try {
//            userManagementClient = getRemoteUserManagerClient(adminUsername, adminPassword);
//            userManagementClient
//                    .addUser(username, password, new String[]{ScenarioTestConstants.PUBLISHER_ROLE}, username);
//        } catch (Exception e) {
//            throw new APIManagementException("Unable to create user with publisher role " + username, e);
//        }
//
//    }

    public static void createUserWithSubscriberRole(String username, String password, String adminUsername,
                                                    String adminPassword)
            throws APIManagementException {
        UserMgtClient userMgtClient;
        try {
            userMgtClient = getRemoteUserMgtClient(adminUsername, adminPassword);
            if (!userMgtClient.isExistingUser(username)) {
                userMgtClient
                        .addUser(username, password, new String[]{ScenarioTestConstants.SUBSCRIBER_ROLE});
            }
        } catch (Exception e) {
            throw new APIManagementException("Unable to create user with subscriber role " + username, e);
        }
    }

    public void createUser(String username, String password, String[] roleList, String adminUsername,
                           String adminPassword) throws APIManagementException {
        UserMgtClient userMgtClient;
        try {
            userMgtClient = getRemoteUserMgtClient(adminUsername, adminPassword);
            if (!userMgtClient.isExistingUser(username)) {
                userMgtClient.addUser(username, password, roleList);
            }
        } catch (Exception e) {
            for (String s : roleList) {
                log.error("Unable to create user with the provided role : " + s);
                throw new APIManagementException("Unable to create user with the provided role list : " + roleList, e);
            }
        }
    }

    public void createRole(String adminUsername, String adminPassword, String role,
                           String[] permisionArray) throws APIManagementException {

        UserManagementClient userManagementClient = null;
        try {
            userManagementClient = getRemoteUserManagerClient(adminUsername, adminPassword);
            if (!userManagementClient.roleNameExists(role)){
                userManagementClient.addRole(role,
                        new String[]{},
                        permisionArray
                );
            }
        } catch (Exception e) {
            throw new APIManagementException("Unable to create role :" + role, e);
        }

    }

    public void updateRole(String adminUsername, String adminPassword, String role, String[] userList,
                           String[] permissionArray) throws APIManagementException {

        UserManagementClient userManagementClient = null;
        try {
            userManagementClient = getRemoteUserManagerClient(adminUsername, adminPassword);
            userManagementClient.deleteRole(role);
            userManagementClient.addRole(role,
                    userList,
                    permissionArray
            );
        } catch (Exception e) {
            throw new APIManagementException("Unable to update role :" + role, e);
        }

    }

    public void deleteUser(String username, String adminUsername, String adminPassword) throws APIManagementException {

        UserMgtClient userMgtClient;
        try {
            userMgtClient = getRemoteUserMgtClient(adminUsername, adminPassword);
            userMgtClient.deleteUser(username);
        } catch (Exception e) {
            throw new APIManagementException("Unable to delete user :" + username, e);
        }
    }

    public void updateUser(String username, String[] newRoles, String[] deletedRoles, String adminUsername, String adminPassword)
            throws APIManagementException {

        UserManagementClient userManagementClient = null;
        try {
            userManagementClient = getRemoteUserManagerClient(adminUsername, adminPassword);
            userManagementClient.addRemoveRolesOfUser(username, newRoles, deletedRoles);
        } catch (Exception e) {
            throw new APIManagementException("Unable to update user with the provided role " + newRoles.toString(), e);
        }
    }

    public void deleteRole(String role, String adminUsername, String adminPassword) throws APIManagementException {

        UserManagementClient userManagementClient;
        try {
            userManagementClient = getRemoteUserManagerClient(adminUsername, adminPassword);
            userManagementClient.deleteRole(role);
        } catch (Exception e) {
            throw new APIManagementException("Unable to delete role :" + role, e);
        }
    }

    public void isAPIVisibleInStore(String apiId) throws ApiException {
        long waitTime = System.currentTimeMillis() + ScenarioTestConstants.TIMEOUT_API_APPEAR_IN_STORE_AFTER_PUBLISH;
        log.info("WAIT for availability of API: " + apiId);
        while (waitTime > System.currentTimeMillis()) {
            APIDTO api = restAPIStore.getAPI(apiId);
            if (api != null) {
                if (api.getId().equals(apiId)) {
                    log.info("API found in store : " + apiId);
                    break;
                } else {
                    try {
                        log.info("API : " + apiId + " not found in store yet.");
                        Thread.sleep(500);
                    } catch (InterruptedException ignored) {

                    }
                }
            }
        }
    }

    /*
     * This will check the tags updated in publisher API
     *
     * @param apiUpdateResponsePublisher Provider of the API
     * @param apiName Name of the API
     * @param tags newly added tags of the API
     * @throws Exception
     * */
    public void verifyTagsUpdatedInPublisherAPI(HttpResponse apiUpdateResponsePublisher, String apiName, String tags) {
        JSONObject responseJson = new JSONObject(apiUpdateResponsePublisher.getData());
        List<String> updatedTags = new ArrayList<>();
        JSONArray jsonArray = responseJson.getJSONArray("tags");
        if (jsonArray != null) {
            for (int i = 0; i < jsonArray.length(); i++) {
                updatedTags.add(jsonArray.getString(i));
            }
        }
        List<String> tagsList = Arrays.asList(tags.split(","));
        if (updatedTags != null) {
            if (updatedTags.size() > 1) {
                for (String t : updatedTags) {
                    Assert.assertTrue(tagsList.contains(t.trim()), "tag " + t + " in the " + apiName + " is not updated");
                }
            } else if (updatedTags.size() == 1) {
                Assert.assertTrue(tags.equals(updatedTags.get(0)), "Tags of the " + apiName + " is not updated");
            } else {
                Assert.assertTrue(tags.equals(""), "Tags of the " + apiName + " is not updated");
            }
        }
    }

    /*
     * This will check the tags updated in publisher is visible in the store
     *
     * @param provider Provider of the API
     * @param apiName Name of the API
     * @param version version of the API
     * @param tags String Array of updated tags
     * @param apiStoreRestClient REST Client for the API Store
     * @throws Exception
     * */
    public void isTagsVisibleInStore(String provider, String apiName, String version, String tags, APIStoreRestClient apiStoreRestClient)
            throws Exception {

        long waitTime = System.currentTimeMillis() + ScenarioTestConstants.TIMEOUT_API_APPEAR_IN_STORE_AFTER_PUBLISH;
        HttpResponse apiResponseStore = null;
        log.info("WAIT for availability of API: " + apiName);
        while (waitTime > System.currentTimeMillis()) {
            apiResponseStore = apiStoreRestClient.getAPI(provider, apiName, version);
            if (apiResponseStore != null) {
                if (apiResponseStore.getData().contains(apiName)) {
                    int tagsCount = 0;

                    if (tags != null) {
                        String[] tagsArr = tags.split(",");
                        for (String tag : tagsArr) {
                            if (apiResponseStore.getData().contains(tag)) {
                                tagsCount++;
                                if (tagsCount == tagsArr.length) {
                                    Assert.assertTrue(true, "API tags found in store : " + tagsArr.length);
                                }
                            } else {
                                //a tag is not visible in the store
                                Assert.assertTrue(false, "API tag is not found in store : " + tag);
                            }
                        }
                    }
                    log.info("API found in store : " + apiName);
                    log.info(apiResponseStore.getData());
                    verifyResponse(apiResponseStore);
                    break;
                } else {
                    try {
                        log.info("API : " + apiName + " not found in store yet.");
                        Thread.sleep(500);
                    } catch (InterruptedException ignored) {

                    }
                }
            }
        }
        if (apiResponseStore != null && !apiResponseStore.getData().contains(apiName)) {
            log.info("API :" + apiName + " was not found in store at the end of wait time.");
            Assert.assertTrue(false, "API not found in store : " + apiName);
        }
    }

    public void isAPINotVisibleInStore(String apiName, APIStoreRestClient apiStoreRestClient)
            throws APIManagerIntegrationTestException {
        long waitTime = System.currentTimeMillis() + ScenarioTestConstants.TIMEOUT_API_NOT_APPEAR_IN_STORE_AFTER_PUBLISH;
        HttpResponse apiResponseStore = null;
        log.info("WAIT for API to be unavailable in store: " + apiName);
        while (waitTime > System.currentTimeMillis()) {
            apiResponseStore = apiStoreRestClient.getAPIs();
            if (apiResponseStore != null) {
                verifyResponse(apiResponseStore);
                if (apiResponseStore.getData().contains(apiName)) {
                    try {
                        log.info("API found in store : " + apiName);
                        Thread.sleep(500);
                    } catch (InterruptedException ignored) {
//                        do nothing
                    }
                } else {
                    log.info("API : " + apiName + " not found in store.");
                    break;
                }
            }
        }
        if (apiResponseStore != null && apiResponseStore.getData().contains(apiName)) {
            log.info("API :" + apiName + " was found in store at the end of wait time.");
            Assert.assertTrue(false, "API found in store : " + apiName);
        }
    }

    public void isChangeVisibleInStore(String apiName, APIStoreRestClient apiStoreRestClient, String assertText,
                                       String tenantDomain) throws APIManagerIntegrationTestException {
        long waitTime = System.currentTimeMillis() + ScenarioTestConstants.TIMEOUT_API_APPEAR_IN_STORE_AFTER_PUBLISH;
        HttpResponse apiResponseStore = null;
        log.info("WAIT for availability of change in API: " + apiName);
        boolean apiUpdated = false;
        while ((waitTime > System.currentTimeMillis()) && !apiUpdated) {
            apiResponseStore = apiStoreRestClient.getAllPaginatedPublishedAPIs(tenantDomain, 1, 5);
            if (apiResponseStore != null) {
                JSONObject jsonObjectOfResponse = new JSONObject(apiResponseStore.getData());
                JSONArray jsonArrayOfResponse = jsonObjectOfResponse.getJSONArray("apis");
                for (int i = 0; i < jsonArrayOfResponse.length(); i++) {
                    String response = jsonArrayOfResponse.getJSONObject(i).toString();
                    if (response.contains(apiName) && response.contains(assertText)) {
                        log.info("New changes visible in store for API : " + apiName);
                        verifyResponse(apiResponseStore);
                        apiUpdated = true;
                        break;
                    } else {
                        try {
                            log.info("New changes for  API : " + apiName + " not visible in store yet.");
                            Thread.sleep(500);
                        } catch (InterruptedException ignored) {

                        }
                    }
                }
            }
        }
        if (apiResponseStore != null && !apiResponseStore.getData().contains(apiName) && !apiResponseStore.getData()
                .contains(assertText)) {
            Assert.assertTrue(false,
                    "New changes for :" + apiName + " was not visible in store at the end of wait time.");
        }
    }

    public void isTagVisibleInStore(String tag, APIStoreRestClient apiStoreRestClient, boolean isAnonymousUser)
            throws Exception {
        long waitTime = System.currentTimeMillis() +
                ScenarioTestConstants.TIMEOUT_API_TAG_APPEAR_IN_STORE_AFTER_PUBLISH;
        HttpResponse tagResponse = null;
        log.info("WAIT for tag \'" + tag + "\' to be visible in store");
        while ((waitTime > System.currentTimeMillis())) {
            if (isAnonymousUser) {
                tagResponse = apiStoreRestClient.getTagListFromStoreAsAnonymousUser();
            } else {
                tagResponse = apiStoreRestClient.getAllTags();
            }
            verifyResponse(tagResponse);
            if (tagResponse != null) {
                if (tagResponse.getData().contains(tag)) {
                    log.info("Tag \'" + tag + "\' visible in store");
                    break;
                } else {
                    log.info("Tag \'" + tag + "\' is not visible in store");
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException ignored) {
//                        do nothing
                    }
                }
            }
        }
        if (tagResponse != null && !tagResponse.getData().contains(tag)) {
            Assert.fail("Tag \'" + tag + "\' is not visible in store");
        }
    }

    public String getHttpsAPIInvocationURL(String apiContext, String apiVersion, String apiResource) {
        return gatewayHttpsURL + "/" + apiContext + "/" + apiVersion + apiResource;
    }

    @Deprecated
    public boolean isWebApplicationDeployed(String serviceEndpoint, String username, String password,
                                            String webAppFileName)
            throws RemoteException, APIManagementException {
        log.info("WebApp deployment verification simplified - webapps are now deployed during server startup: " + webAppFileName);
        // Since webapps are now deployed during server startup, we assume they are available
        return true;
    }

    public String getBackendEndServiceEndPointHttps(String serviceName) {
        String webAppURL = serviceEndpoint.replaceFirst("/services.*", "");

        // Avoid adding extra '/' at the end
        webAppURL = StringUtils.isEmpty(serviceName) ? webAppURL : webAppURL + '/' + serviceName;
        return webAppURL;
    }

    /**
     * Checks whether the provided json object (taken from getAllTags response) contains a given tag.
     *
     * @param tagsResponse JSONObject containing the getAllTags response
     * @param tagName      tag name to check for the existence
     * @return true if the tagResponse contains the tagName, false otherwise;
     */
    public boolean isTagsResponseContainsTag(JSONObject tagsResponse, String tagName) {
        JSONArray tags = tagsResponse.getJSONArray("tags");
        for (int i = 0; i < tags.length(); i++) {
            if (tagName.equals(tags.getJSONObject(i).getString("name"))) {
                return true;
            }
        }
        return false;
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
            throws APIManagerIntegrationTestException {

        long currentTime = System.currentTimeMillis();
        long waitTime = currentTime + WAIT_TIME;
        String colonSeparatedHeader = "admin" + ':' + "admin";
        String authorizationHeader = "Basic " + new String(Base64.encodeBase64(colonSeparatedHeader.getBytes()));
        Map headerMap = new HashMap();
        headerMap.put("Authorization", authorizationHeader);
        String tenantIdentifier = getTenantIdentifier(apiProvider);

        while (waitTime > System.currentTimeMillis()) {
            HttpResponse response = null;
            try {
                response = HttpClient.doGet(getBackendEndServiceEndPointHttps("") +
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
                        // Exception can be ignored since we are doing this in a loop.
                        // Worst case: Loop will request for the API status without waiting.
                    }
                }
            }
        }
    }

    /**
     * This returns "tenatDomain/tenantId/" string
     *
     * @param apiProvider
     */
    private String getTenantIdentifier(String apiProvider) throws APIManagerIntegrationTestException {
        int tenantId = -1234;
        String providerTenantDomain = MultitenantUtils.getTenantDomain(apiProvider);
        try {
            if (!MultitenantConstants.SUPER_TENANT_DOMAIN_NAME.equals(providerTenantDomain)) {
                String sessionCookie = login(serviceEndpoint, "admin", "admin");
                TenantManagementServiceClient tenantManagementServiceClient = new TenantManagementServiceClient(
                        serviceEndpoint, sessionCookie);
                TenantInfoBean tenant = tenantManagementServiceClient.getTenant(providerTenantDomain);
                if (tenant == null) {
                    log.info("tenant is null: " + providerTenantDomain);
                } else {
                    tenantId = tenant.getTenantId();
                }
                //forced tenant loading
                login(gatewayHttpsURL, "admin", "admin");
            }
        } catch (Exception e) {
            throw new APIManagerIntegrationTestException(e.getMessage(), e);
        }
        return providerTenantDomain + "/" + tenantId + "/";
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
}
