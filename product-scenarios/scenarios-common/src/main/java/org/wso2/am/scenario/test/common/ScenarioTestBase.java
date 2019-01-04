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
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONException;
import org.json.JSONObject;
import org.testng.Assert;
import org.wso2.am.admin.clients.user.RemoteUserStoreManagerServiceClient;
import org.wso2.am.integration.test.utils.APIManagerIntegrationTestException;
import org.wso2.carbon.apimgt.api.APIManagementException;
import org.wso2.carbon.authenticator.stub.LoginAuthenticationExceptionException;
import org.wso2.carbon.integration.common.admin.client.AuthenticatorClient;
import org.wso2.carbon.integration.common.admin.client.TenantManagementServiceClient;
import org.wso2.carbon.integration.common.admin.client.UserManagementClient;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.rmi.RemoteException;
import java.util.Properties;

import org.wso2.carbon.tenant.mgt.stub.TenantMgtAdminServiceExceptionException;
import org.wso2.carbon.tenant.mgt.stub.beans.xsd.TenantInfoBean;
import org.wso2.carbon.user.mgt.stub.UserAdminUserAdminException;
import org.wso2.carbon.utils.multitenancy.MultitenantUtils;

public class ScenarioTestBase {

    private static final String INPUTS_LOCATION = System.getenv("DATA_BUCKET_LOCATION");
    private static final String INFRASTRUCTURE_PROPERTIES = "deployment.properties";
    private static final Log log = LogFactory.getLog(ScenarioTestBase.class);
    protected static String publisherURL;
    protected static String storeURL;
    protected static String keyManagerURL;
    private static Properties infraProperties;
    public static final String PUBLISHER_URL = "PublisherUrl";
    public static final String STORE_URL = "StoreUrl";
    public static final String KEYAMANAGER_URL = "keyManagerUrl";
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

    public void createUserWithPublisherAndCreatorRole(String username, String password, String adminUsername,
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
    public void createUserWithSubscriberRole(String username, String password,
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
            throw new APIManagementException("Unable to create user with the provided role list " + roleList, e);
        }
    }

    public void createRole(String adminUsername, String adminPassword, String role) throws APIManagementException {

        UserManagementClient userManagementClient = null;
        try {
            userManagementClient = getRemoteUserManagerClient(adminUsername, adminPassword);
            userManagementClient.addRole(role,
                    new String[]{},
                    new String[]{"/permission/admin/login",
                            "/permission/admin/manage/api/subscribe"});
        } catch (Exception e) {
            throw new APIManagementException("Unable to create role :" + role, e);
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

    public void deleteRole(String role, String adminUsername, String adminPassword) throws APIManagementException {

        UserManagementClient userManagementClient;
        try {
            userManagementClient = getRemoteUserManagerClient(adminUsername, adminPassword);
            userManagementClient.deleteRole(role);
        } catch (Exception e) {
            throw new APIManagementException("Unable to delete role :" + role, e);
        }
    }

}
