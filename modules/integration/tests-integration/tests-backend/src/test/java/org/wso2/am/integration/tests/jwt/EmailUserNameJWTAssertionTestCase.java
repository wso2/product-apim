/* * Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
 * under the License. */


package org.wso2.am.integration.tests.jwt;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONObject;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.am.admin.clients.client.utils.AuthenticateStub;
import org.wso2.am.integration.test.utils.base.APIMIntegrationBaseTest;
import org.wso2.am.integration.test.utils.bean.*;
import org.wso2.am.integration.test.utils.clients.APIPublisherRestClient;
import org.wso2.am.integration.test.utils.clients.APIStoreRestClient;
import org.wso2.am.integration.test.utils.monitor.utils.WireMonitorServer;
import org.wso2.carbon.automation.test.utils.http.client.HttpRequestUtil;
import org.wso2.carbon.integration.common.admin.client.UserManagementClient;
import org.wso2.carbon.integration.common.utils.mgt.ServerConfigurationManager;
import org.wso2.carbon.tenant.mgt.stub.TenantMgtAdminServiceExceptionException;
import org.wso2.carbon.tenant.mgt.stub.TenantMgtAdminServiceStub;
import org.wso2.carbon.tenant.mgt.stub.beans.xsd.TenantInfoBean;
import org.wso2.carbon.utils.multitenancy.MultitenantConstants;

import java.io.File;
import java.net.URL;
import java.rmi.RemoteException;
import java.util.*;

import static org.testng.Assert.assertEquals;

public class EmailUserNameJWTAssertionTestCase extends APIMIntegrationBaseTest {

    private APIStoreRestClient apiStore;

    private String consumerKey;
    private String consumerSecret;
    private String userName;
    private String password;

    private static final Log log = LogFactory.getLog(EmailUserNameJWTAssertionTestCase.class);

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {
        APIPublisherRestClient apiPublisher;
        String publisherURLHttp;

        ServerConfigurationManager serverConfigurationManager;
        super.init();

        userName = gatewayContext.getContextTenant().getTenantAdmin().getUserName();
        password = gatewayContext.getContextTenant().getTenantAdmin().getPassword();


        publisherURLHttp = publisherUrls.getWebAppURLHttp();
        String storeURLHttp = storeUrls.getWebAppURLHttp();

        String apiManagerXml =
                getAMResourceLocation() +
                File.separator +
                "configFiles/emailusernamejwttest/" +
                "api-manager.xml";

        String userMgtXml =
                getAMResourceLocation() +
                File.separator +
                "configFiles/emailusernamejwttest/" +
                "user-mgt.xml";
        serverConfigurationManager = new ServerConfigurationManager(gatewayContext);
        serverConfigurationManager.applyConfigurationWithoutRestart(new File(apiManagerXml));
        serverConfigurationManager.applyConfiguration(new File(userMgtXml));

        apiPublisher = new APIPublisherRestClient(publisherURLHttp);
        apiStore = new APIStoreRestClient(storeURLHttp);
        apiPublisher.login(userName, password);
        APIRequest apiRequest = new APIRequest("test", "test",
                                               new URL("http://localhost:6789"));
        apiRequest.setVisibility("public");
        apiPublisher.addAPI(apiRequest);
        APILifeCycleStateRequest
                updateRequest = new APILifeCycleStateRequest("test", userName,
                                                             APILifeCycleState.PUBLISHED);
        apiPublisher.changeAPILifeCycleStatus(updateRequest);
        apiStore.login(userName, password);
        SubscriptionRequest subscriptionRequest =
                new SubscriptionRequest("test", userName);
        apiStore.subscribe(subscriptionRequest);
        APPKeyRequestGenerator generateAppKeyRequest =
                new APPKeyRequestGenerator("DefaultApplication");
        String responseString = apiStore.generateApplicationKey(generateAppKeyRequest).getData();
        JSONObject response = new JSONObject(responseString);
        consumerKey =
                response.getJSONObject("data").getJSONObject("key").get("consumerKey").toString();
        consumerSecret = response.getJSONObject("data").getJSONObject("key").get("consumerSecret")
                .toString();
    }

    @Test(groups = {
            "wso2.am"}, description = "username JWT-Token Generation test for super tenant")
    public void userNameInSuperTenantJWTTokenTestCase() throws Exception {
        String requestBody =
                "grant_type=password&username=" + userName + "&password=" +
                password;
        URL tokenEndpointURL = new URL(gatewayUrls.getWebAppURLNhttp() + "token");
        JSONObject accessTokenGenerationResponse =
                new JSONObject(apiStore.generateUserAccessKey(consumerKey,
                                                              consumerSecret, requestBody,
                                                              tokenEndpointURL).getData());
        String accessToken = accessTokenGenerationResponse.getString("access_token");
        Map<String, String> requestHeaders = new HashMap<String, String>();
        requestHeaders.put("Authorization", "Bearer " + accessToken);
        Thread.sleep(2000);
        WireMonitorServer wireServer = new WireMonitorServer(6789);
        wireServer.start();
        HttpRequestUtil.doGet(gatewayUrls.getWebAppURLNhttp() + "test/1.0.0/", requestHeaders);
        String wireLog = wireServer.getCapturedMessage();
        if (wireLog.contains("JWT-Assertion: ")) {
            wireLog = wireLog.split("JWT-Assertion: ")[1];
            int firstDotSeparatorIndex = wireLog.indexOf('.');
            int secondSeparatorIndex = wireLog.indexOf('.', firstDotSeparatorIndex + 1);
            String JWTToken = wireLog.substring(firstDotSeparatorIndex + 1, secondSeparatorIndex);
            byte[] decodedJwt = Base64.decodeBase64(JWTToken.getBytes());
            JSONObject jsonObject = new JSONObject(new String(decodedJwt));
            assertEquals(jsonObject.get("iss"), "wso2.org/products/am");
            assertEquals(jsonObject.get("http://wso2.org/claims/subscriber"), "admin");
            assertEquals(jsonObject.get("http://wso2.org/claims/applicationid"), "1");
            assertEquals(jsonObject.get("http://wso2.org/claims/applicationname"),
                         "DefaultApplication");
            assertEquals(jsonObject.get("http://wso2.org/claims/applicationtier"),
                         "Unlimited");
            assertEquals(jsonObject.get("http://wso2.org/claims/apicontext"), "/test");
            assertEquals(jsonObject.get("http://wso2.org/claims/version"), "1.0.0");
            assertEquals(jsonObject.get("http://wso2.org/claims/tier"), "Gold");
            assertEquals(jsonObject.get("http://wso2.org/claims/keytype"), "PRODUCTION");
            assertEquals(jsonObject.get("http://wso2.org/claims/usertype"),
                         "APPLICATION_USER");
            assertEquals(jsonObject.get("http://wso2.org/claims/enduser"),
                         "admin@carbon.super");
            assertEquals(jsonObject.get("http://wso2.org/claims/enduserTenantId"), "-1234");
        }
    }

    @Test(groups = {
            "wso2.am"}, description = "email username JWT-Token Generation test for super tenant",
            dependsOnMethods = "userNameInSuperTenantJWTTokenTestCase")
    public void emailUserNameInSuperTenantJWTTokenTestCase() throws Exception {
        String userName = "admin@wso2.com";
        String password = "admin123";
        UserManagementClient userManagementClient =
                new UserManagementClient(gatewayContext.getContextUrls().getBackEndUrl(), "admin", "admin");
        userManagementClient
                .addUser(userName, password, new String[]{"Internal/subscriber"}, "admin2");
        String requestBody = "grant_type=password&username=" + userName + "@" +
                             MultitenantConstants.SUPER_TENANT_DOMAIN_NAME + "&password=" +
                             password;
        URL tokenEndpointURL = new URL(gatewayUrls.getWebAppURLNhttp() + "token");
        JSONObject accessTokenGenerationResponse =
                new JSONObject(apiStore.generateUserAccessKey(consumerKey,
                                                              consumerSecret, requestBody,
                                                              tokenEndpointURL).getData());
        String userAccessToken = accessTokenGenerationResponse.getString("access_token");
        Map<String, String> requestHeaders = new HashMap<String, String>();
        requestHeaders.put("Authorization", "Bearer " + userAccessToken);
        Thread.sleep(2000);
        WireMonitorServer wireServer = new WireMonitorServer(6789);
        wireServer.start();
        HttpRequestUtil.doGet(gatewayUrls.getWebAppURLNhttp() + "test/1.0.0/", requestHeaders);
        String wireLog = wireServer.getCapturedMessage();
        if (wireLog.contains("JWT-Assertion: ")) {
            wireLog = wireLog.split("JWT-Assertion: ")[1];
            int firstDotSeparatorIndex = wireLog.indexOf('.');
            int secondSeparatorIndex = wireLog.indexOf('.', firstDotSeparatorIndex + 1);
            String JWTToken = wireLog.substring(firstDotSeparatorIndex + 1, secondSeparatorIndex);
            byte[] decodedJwt = Base64.decodeBase64(JWTToken.getBytes());
            JSONObject jsonObject = new JSONObject(new String(decodedJwt));
            assertEquals(jsonObject.get("iss"), "wso2.org/products/am");
            assertEquals(jsonObject.get("http://wso2.org/claims/subscriber"), userName);
            assertEquals(jsonObject.get("http://wso2.org/claims/applicationid"), "1");
            assertEquals(jsonObject.get("http://wso2.org/claims/applicationname"),
                         "DefaultApplication");
            assertEquals(jsonObject.get("http://wso2.org/claims/applicationtier"),
                         "Unlimited");
            assertEquals(jsonObject.get("http://wso2.org/claims/apicontext"), "/test");
            assertEquals(jsonObject.get("http://wso2.org/claims/version"), "1.0.0");
            assertEquals(jsonObject.get("http://wso2.org/claims/tier"), "Gold");
            assertEquals(jsonObject.get("http://wso2.org/claims/keytype"), "PRODUCTION");
            assertEquals(jsonObject.get("http://wso2.org/claims/usertype"),
                         "APPLICATION_USER");
            assertEquals(jsonObject.get("http://wso2.org/claims/enduser"),
                         "admin@wso2.com@carbon.super");
            assertEquals(jsonObject.get("http://wso2.org/claims/enduserTenantId"), "-1234");
            assertEquals(jsonObject.get("http://wso2.org/claims/role"),
                         "Internal/subscriber,Internal/everyone");

        }
    }

    @Test(groups = {"wso2.am"}, description = "username JWT-Token Generation test for  tenant")
    public void UserNameInTenantJWTTokenTestCase() throws Exception {

        String userName = "tenant";
        String password = "admin123";
        String domainName = "adc.com";
        String fullUserName = userName + "@" + domainName;
        boolean isSuccessful =
                createTenantWithEmailUserName(userName, password,
                                              domainName, gatewayContext.getContextUrls().getBackEndUrl());
        assertEquals(isSuccessful, true);
        UserManagementClient userManagementClient =
                new UserManagementClient(gatewayContext.getContextUrls().getBackEndUrl(), fullUserName, password);
        userManagementClient
                .addRemoveRolesOfUser(fullUserName, new String[]{"Internal/subscriber"}, null);
        String requestBody =
                "grant_type=password&username=" + fullUserName + "&password=" + password;
        URL tokenEndpointURL = new URL(gatewayUrls.getWebAppURLNhttp() + "token");
        JSONObject accessTokenGenerationResponse =
                new JSONObject(apiStore.generateUserAccessKey(consumerKey,
                                                              consumerSecret, requestBody,
                                                              tokenEndpointURL).getData());
        String userAccessToken = accessTokenGenerationResponse.getString("access_token");
        Map<String, String> requestHeaders = new HashMap<String, String>();
        requestHeaders.put("Authorization", "Bearer " + userAccessToken);
        Thread.sleep(2000);
        WireMonitorServer wireServer = new WireMonitorServer(6789);
        wireServer.start();
        HttpRequestUtil.doGet(gatewayUrls.getWebAppURLNhttp() + "test/1.0.0/", requestHeaders);
        String wireLog = wireServer.getCapturedMessage();
        if (wireLog.contains("JWT-Assertion: ")) {
            wireLog = wireLog.split("JWT-Assertion: ")[1];
            int firstDotSeparatorIndex = wireLog.indexOf('.');
            int secondSeparatorIndex = wireLog.indexOf('.', firstDotSeparatorIndex + 1);
            String JWTToken = wireLog.substring(firstDotSeparatorIndex + 1, secondSeparatorIndex);
            byte[] decodedJwt = Base64.decodeBase64(JWTToken.getBytes());

            JSONObject jsonObject = new JSONObject(new String(decodedJwt));
            assertEquals(jsonObject.get("iss"), "wso2.org/products/am");
            assertEquals(jsonObject.get("http://wso2.org/claims/subscriber"), "admin");
            assertEquals(jsonObject.get("http://wso2.org/claims/applicationid"), "1");
            assertEquals(jsonObject.get("http://wso2.org/claims/applicationname"),
                         "DefaultApplication");
            assertEquals(jsonObject.get("http://wso2.org/claims/applicationtier"),
                         "Unlimited");
            assertEquals(jsonObject.get("http://wso2.org/claims/apicontext"), "/test");
            assertEquals(jsonObject.get("http://wso2.org/claims/version"), "1.0.0");
            assertEquals(jsonObject.get("http://wso2.org/claims/tier"), "Gold");
            assertEquals(jsonObject.get("http://wso2.org/claims/keytype"), "PRODUCTION");
            assertEquals(jsonObject.get("http://wso2.org/claims/usertype"),
                         "APPLICATION_USER");
            assertEquals(jsonObject.get("http://wso2.org/claims/enduser"), "tenant@adc.com");
            assertEquals(jsonObject.get("http://wso2.org/claims/givenname"), "admin");
            assertEquals(jsonObject.get("http://wso2.org/claims/lastname"),
                         "adminwso2automation");
            assertEquals(jsonObject.get("http://wso2.org/claims/role"),
                         "admin,Internal/subscriber,Internal/everyone");
        }
    }

    @Test(groups = {
            "wso2.am"}, description = "email username JWT-Token Generation test for  tenant")
    public void emailUserNameInTenantJWTTokenTestCase() throws Exception {

        String userNameWithEmail = "tenant@wso2.com";
        String password = "admin123";
        String domainName = "adc.com";
        String fullUserName = userNameWithEmail + "@" + domainName;
        UserManagementClient userManagementClient =
                new UserManagementClient(gatewayContext.getContextUrls().getBackEndUrl(), "tenant@adc.com", "admin123");
        userManagementClient
                .addUser(userNameWithEmail, password, new String[]{"Internal/subscriber"},
                         "abc");
        String requestBody =
                "grant_type=password&username=" + fullUserName + "&password=" + password;
        URL tokenEndpointURL = new URL(gatewayUrls.getWebAppURLNhttp() + "token");

        JSONObject accessTokenGenerationResponse = new JSONObject(
                apiStore.generateUserAccessKey(consumerKey, consumerSecret, requestBody,
                                               tokenEndpointURL).getData());
        String userAccessToken = accessTokenGenerationResponse.getString("access_token");
        Map<String, String> requestHeaders = new HashMap<String, String>();
        requestHeaders.put("Authorization", "Bearer " + userAccessToken);
        Thread.sleep(2000);
        WireMonitorServer wireServer = new WireMonitorServer(6789);
        wireServer.start();
        HttpRequestUtil.doGet(gatewayUrls.getWebAppURLNhttp() + "test/1.0.0/", requestHeaders);
        String wireLog = wireServer.getCapturedMessage();
        if (wireLog.contains("JWT-Assertion: ")) {
            wireLog = wireLog.split("JWT-Assertion: ")[1];
            int firstDotSeparatorIndex = wireLog.indexOf('.');
            int secondSeparatorIndex = wireLog.indexOf('.', firstDotSeparatorIndex + 1);
            String JWTToken = wireLog.substring(firstDotSeparatorIndex + 1, secondSeparatorIndex);
            byte[] decodedJwt = Base64.decodeBase64(JWTToken.getBytes());
            JSONObject jsonObject = new JSONObject(new String(decodedJwt));
            assertEquals(jsonObject.get("iss"), "wso2.org/products/am");
            assertEquals(jsonObject.get("http://wso2.org/claims/subscriber"), "admin");
            assertEquals(jsonObject.get("http://wso2.org/claims/applicationid"), "1");
            assertEquals(jsonObject.get("http://wso2.org/claims/applicationname"),
                         "DefaultApplication");
            assertEquals(jsonObject.get("http://wso2.org/claims/applicationtier"),
                         "Unlimited");
            assertEquals(jsonObject.get("http://wso2.org/claims/apicontext"), "/test");
            assertEquals(jsonObject.get("http://wso2.org/claims/version"), "1.0.0");
            assertEquals(jsonObject.get("http://wso2.org/claims/tier"), "Gold");
            assertEquals(jsonObject.get("http://wso2.org/claims/keytype"), "PRODUCTION");
            assertEquals(jsonObject.get("http://wso2.org/claims/usertype"),
                         "APPLICATION_USER");
            assertEquals(jsonObject.get("http://wso2.org/claims/enduser"),
                         "tenant@wso2.com@adc.com");
            assertEquals(jsonObject.get("http://wso2.org/claims/role"),
                         "Internal/subscriber,Internal/everyone");

        }
    }

    private boolean createTenantWithEmailUserName(String userNameWithEmail, String pwd,
                                                  String domainName, String backendUrl) {
        boolean isSuccess = false;
        try {
            String endPoint = backendUrl + "TenantMgtAdminService";
            TenantMgtAdminServiceStub tenantMgtAdminServiceStub =
                    new TenantMgtAdminServiceStub(
                            endPoint);
            AuthenticateStub.authenticateStub("admin", "admin", tenantMgtAdminServiceStub);

            Date date = new Date();
            Calendar calendar = new GregorianCalendar();
            calendar.setTime(date);

            TenantInfoBean tenantInfoBean = new TenantInfoBean();
            tenantInfoBean.setActive(true);
            tenantInfoBean.setEmail("abc@fds.com");
            tenantInfoBean.setAdminPassword(pwd);
            tenantInfoBean.setAdmin(userNameWithEmail);
            tenantInfoBean.setTenantDomain(domainName);
            tenantInfoBean.setCreatedDate(calendar);
            tenantInfoBean.setFirstname("admin");
            tenantInfoBean.setLastname("admin" + "wso2automation");
            tenantInfoBean.setSuccessKey("true");
            tenantInfoBean.setUsagePlan("demo");
            TenantInfoBean tenantInfoBeanGet;
            tenantInfoBeanGet = tenantMgtAdminServiceStub.getTenant(domainName);

            if (!tenantInfoBeanGet.getActive() && tenantInfoBeanGet.getTenantId() != 0) {
                tenantMgtAdminServiceStub.activateTenant(domainName);
                System.out.println("Tenant domain " + domainName + " Activated successfully");
                log.info("Tenant domain " + domainName + " Activated successfully");

            } else if (!tenantInfoBeanGet.getActive()) {
                tenantMgtAdminServiceStub.addTenant(tenantInfoBean);
                tenantMgtAdminServiceStub.activateTenant(domainName);
                System.out.println("Tenant domain " + domainName +
                                   " created and activated successfully");
                log.info("Tenant domain " + domainName + " created and activated successfully");
                isSuccess = true;
            } else {
                System.out.println("Tenant domain " + domainName + " already registered");
                log.info("Tenant domain " + domainName + " already registered");
            }
        } catch (RemoteException e) {
            log.error("RemoteException thrown while adding user/tenants : ", e);

        } catch (TenantMgtAdminServiceExceptionException e) {
            log.error("Error connecting to the TenantMgtAdminService : ", e);
        }

        return isSuccess;
    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
        super.cleanup();
    }

}