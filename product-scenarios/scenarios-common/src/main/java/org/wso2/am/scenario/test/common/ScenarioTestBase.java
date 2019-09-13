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
import org.wso2.am.admin.clients.webapp.WebAppAdminClient;
import org.wso2.am.integration.test.utils.APIManagerIntegrationTestException;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.carbon.apimgt.api.APIManagementException;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;
import org.wso2.carbon.base.MultitenantConstants;
import org.wso2.carbon.integration.common.admin.client.AuthenticatorClient;
import org.wso2.carbon.integration.common.admin.client.TenantManagementServiceClient;
import org.wso2.carbon.integration.common.admin.client.UserManagementClient;
import org.wso2.carbon.tenant.mgt.stub.beans.xsd.TenantInfoBean;
import org.wso2.carbon.user.mgt.stub.UserAdminUserAdminException;
import org.wso2.carbon.utils.multitenancy.MultitenantUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.rmi.RemoteException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

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
    private static Properties infraProperties;
    public static final String PUBLISHER_URL = "PublisherUrl";
    public static final String STORE_URL = "StoreUrl";
    public static final String ADMIN_URL = "AdminUrl";
    public static final String KEYAMANAGER_URL = "KeyManagerUrl";
    public static final String GATEWAYHTTPS_URL = "GatewayHttpsUrl";
    public static final String SERVICE_ENDPOINT = "CarbonServerUrl";
    protected static String resourceLocation = System.getProperty("framework.resource.location");

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
        } catch (Exception e) {
            throw new APIManagementException("Unable to add new tenant and activate " + domain, e);
        }
    }

    public static TenantManagementServiceClient getTenantManagementServiceClient() throws APIManagementException {

        AuthenticatorClient authenticatorClient = null;
        try {
            authenticatorClient = new AuthenticatorClient(keyManagerURL);
            URL url = new URL(keyManagerURL);
            String sessionCookie = authenticatorClient.login("admin", "admin", url.getHost());

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

    /**
     * Create a Client to communicate with Web Application Admin Service
     *
     * @return {@link WebAppAdminClient}
     * @throws APIManagementException If there are any errors during initializing the client
     */
    public WebAppAdminClient getWebAppAdminClient() throws APIManagementException {
        try {

            // using service endpoint for now. get gw services url from TG once distributed deployment is ready
            String sessionCookie = login(serviceEndpoint, "admin", "admin");

            return new WebAppAdminClient(serviceEndpoint, sessionCookie);
        } catch (RemoteException e) {
            throw new APIManagementException("Unable to create new WebAppAdminClient ", e);
        }
    }

    private String login(String host, String username, String password) throws APIManagementException {
        AuthenticatorClient authenticatorClient = null;

        try {
            authenticatorClient = new AuthenticatorClient(host);
            URL url = new URL(host);
            String sessionCookie = authenticatorClient.login(username, password, url.getHost());

            return sessionCookie;
        } catch (Exception e) {
            throw new APIManagementException("Unable login to Host: " + host, e);
        }
    }

    public static void setKeyStoreProperties() {
        System.setProperty("javax.net.ssl.trustStore", resourceLocation + "/keystore/wso2carbon.jks");
        System.setProperty("javax.net.ssl.trustStorePassword", "wso2carbon");
        System.setProperty("javax.net.ssl.trustStoreType", "JKS");
    }

    public void verifyResponse(HttpResponse httpResponse) throws JSONException {
        Assert.assertNotNull(httpResponse, "Response object is null");
        log.info("Response Code : " + httpResponse.getResponseCode());
        log.info("Response Message : " + httpResponse.getData());
        Assert.assertEquals(httpResponse.getResponseCode(), HttpStatus.SC_OK, "Response code is not as expected");
        JSONObject responseData = new JSONObject(httpResponse.getData());
        Assert.assertFalse(responseData.getBoolean(APIMIntegrationConstants.API_RESPONSE_ELEMENT_NAME_ERROR),
                "Error message received " + httpResponse.getData());
    }

    public void verifyNegativeResponse(HttpResponse httpResponse) throws JSONException {

        Assert.assertNotNull(httpResponse, "Response object is null");
        log.info("Response Code : " + httpResponse.getResponseCode());
        log.info("Response Message : " + httpResponse.getData());
        JSONObject responseData = new JSONObject(httpResponse.getData());
        Assert.assertTrue(responseData.getBoolean(APIMIntegrationConstants.API_RESPONSE_ELEMENT_NAME_ERROR),
                "Error message received " + httpResponse.getData());
    }

    public static void createUserWithCreatorRole(String username, String password,
            String adminUsername, String adminPassword) throws APIManagementException {
        UserManagementClient userManagementClient = null;
        try {
            userManagementClient = getRemoteUserManagerClient(adminUsername, adminPassword);
            userManagementClient.addUser(username, password, new String[] { ScenarioTestConstants.CREATOR_ROLE }, username);
        } catch (Exception e) {
            throw new APIManagementException("Unable to create user with creator role " + username, e);
        }
    }

    public static void createUserWithPublisherAndCreatorRole(String username, String password, String adminUsername,
                                                      String adminPassword) throws APIManagementException {
        UserManagementClient userManagementClient = null;
        try {
            userManagementClient = getRemoteUserManagerClient(adminUsername, adminPassword);
            userManagementClient
                    .addUser(username, password, new String[] { ScenarioTestConstants.CREATOR_ROLE,
                            ScenarioTestConstants.PUBLISHER_ROLE }, username);
        } catch (Exception e) {
            throw new APIManagementException("Unable to create user with publisher and creator role " + username, e);
        }

    }

    public void createUserWithPublisherRole(String username, String password, String adminUsername,
            String adminPassword) throws APIManagementException {
        UserManagementClient userManagementClient = null;
        try {
            userManagementClient = getRemoteUserManagerClient(adminUsername, adminPassword);
            userManagementClient
                    .addUser(username, password, new String[] { ScenarioTestConstants.PUBLISHER_ROLE }, username);
        } catch (Exception e) {
            throw new APIManagementException("Unable to create user with publisher role " + username, e);
        }

    }

    public static void createUserWithSubscriberRole(String username, String password,
            String adminUsername, String adminPassword)
            throws RemoteException, UserAdminUserAdminException, APIManagementException {
        UserManagementClient userManagementClient = null;
        try {
            userManagementClient = getRemoteUserManagerClient(adminUsername, adminPassword);
            userManagementClient
                    .addUser(username, password, new String[] { ScenarioTestConstants.SUBSCRIBER_ROLE }, username);
        } catch (Exception e) {
            throw new APIManagementException("Unable to create user with subscriber role " + username, e);
        }
    }

    public void createUser(String username, String password, String[] roleList,
            String adminUsername, String adminPassword) throws APIManagementException {
        UserManagementClient userManagementClient = null;
        try {
            userManagementClient = getRemoteUserManagerClient(adminUsername, adminPassword);
            userManagementClient.addUser(username, password, roleList, username);
        } catch (Exception e) {
            for(String s : roleList) {
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
            userManagementClient.addRole(role,
                    new String[]{},
                    permisionArray
                   );
        } catch (RemoteException | UserAdminUserAdminException e) {
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

        UserManagementClient userManagementClient;
        try {
            userManagementClient = getRemoteUserManagerClient(adminUsername, adminPassword);
            userManagementClient.deleteUser(username);
        } catch (Exception e) {
            throw new APIManagementException("Unable to delete user :" + username, e);
        }
    }

    public void updateUser(String username,String[] newRoles,String[] deletedRoles, String adminUsername, String adminPassword)
            throws APIManagementException {

        UserManagementClient userManagementClient = null;
        try {
            userManagementClient = getRemoteUserManagerClient(adminUsername, adminPassword);
            userManagementClient.addRemoveRolesOfUser(username,newRoles,deletedRoles);
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

    public void isAPIVisibleInStore(String apiName, APIStoreRestClient apiStoreRestClient)
            throws APIManagerIntegrationTestException {
        long waitTime = System.currentTimeMillis() + ScenarioTestConstants.TIMEOUT_API_APPEAR_IN_STORE_AFTER_PUBLISH;
        HttpResponse apiResponseStore = null;
        log.info("WAIT for availability of API: " + apiName);
        while (waitTime > System.currentTimeMillis()) {
            apiResponseStore = apiStoreRestClient.getAPIs();
            if (apiResponseStore != null) {
                if (apiResponseStore.getData().contains(apiName)) {
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
        if(apiResponseStore != null && !apiResponseStore.getData().contains(apiName)) {
            log.info("API :" + apiName + " was not found in store at the end of wait time.");
            Assert.assertTrue(false, "API not found in store : " + apiName);
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
    public void verifyTagsUpdatedInPublisherAPI(HttpResponse apiUpdateResponsePublisher, String apiName, String tags){
        String updatedTags = (new JSONObject(apiUpdateResponsePublisher.getData()).getJSONObject("api"))
                .get("tags").toString();
        List<String> tagsList = Arrays.asList(tags.split(","));
        if (updatedTags != null) {
            if (updatedTags.contains(",")) {
                String[] updatedTagsArray = updatedTags.split(",");
                for (String t : updatedTagsArray) {
                    Assert.assertTrue(tagsList.contains(t.trim()), "tag " + t + " in the " + apiName + " is not updated");
                }
            } else {
                Assert.assertTrue(updatedTags.equals(tags), "Tags of the " + apiName + " is not updated");
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
                        // do nothing
                    }
                } else {
                    log.info("API : " + apiName + " not found in store.");
                    break;
                }
            }
        }
        if(apiResponseStore != null && apiResponseStore.getData().contains(apiName)) {
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

    public void isTagVisibleInStore(String tag, APIStoreRestClient apiStoreRestClient, boolean  isAnonymousUser)
            throws Exception{
        long waitTime = System.currentTimeMillis() +
                ScenarioTestConstants.TIMEOUT_API_TAG_APPEAR_IN_STORE_AFTER_PUBLISH;
        HttpResponse tagResponse = null;
        log.info("WAIT for tag \'" + tag +"\' to be visible in store");
        while ((waitTime > System.currentTimeMillis())) {
            if(isAnonymousUser) {
                tagResponse = apiStoreRestClient.getTagListFromStoreAsAnonymousUser();
            } else {
                tagResponse = apiStoreRestClient.getAllTags();
            }
            verifyResponse(tagResponse);
            if (tagResponse != null) {
                if(tagResponse.getData().contains(tag)) {
                    log.info("Tag \'" + tag +"\' visible in store");
                    break;
                } else {
                    log.info("Tag \'" + tag +"\' is not visible in store");
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException ignored) {
                        // do nothing
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

    public boolean isWebApplicationDeployed(String serviceEndpoint, String username, String password,
                                            String webAppFileName)
            throws RemoteException, APIManagementException {
        String sessionCookie = login(serviceEndpoint, username, password);
        WebAppAdminClient webAppAdminClient = new WebAppAdminClient(serviceEndpoint, sessionCookie);

        List<String> webAppList;
        long WEB_APP_DEPLOYMENT_DELAY = 90 * 1000;

        String webAppName = webAppFileName + ".war";
        boolean isWebappDeployed = false;
        long waitingTime = System.currentTimeMillis() + WEB_APP_DEPLOYMENT_DELAY;
        while (waitingTime > System.currentTimeMillis()) {
            webAppList = webAppAdminClient.getWebAppList(webAppFileName);
            for (String name : webAppList) {
                if (webAppName.equalsIgnoreCase(name)) {
                    return !isWebappDeployed;
                }
            }
            try {
                Thread.sleep(500);
            } catch (InterruptedException ignored) {

            }
        }
        return isWebappDeployed;
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
        String authorizationHeader = "Basic "+new String(Base64.encodeBase64(colonSeparatedHeader.getBytes()));
        Map headerMap = new HashMap();
        headerMap.put("Authorization",authorizationHeader);
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
        try{
            if(!MultitenantConstants.SUPER_TENANT_DOMAIN_NAME.equals(providerTenantDomain)){
                String sessionCookie = login(serviceEndpoint, "admin", "admin");
                TenantManagementServiceClient tenantManagementServiceClient = new TenantManagementServiceClient(
                        serviceEndpoint, sessionCookie);
                TenantInfoBean tenant = tenantManagementServiceClient.getTenant(providerTenantDomain);
                if(tenant == null){
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
}
