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
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.wso2.carbon.am.tests.sample;

import org.apache.commons.codec.binary.Base64;
import org.json.JSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.carbon.am.tests.APIManagerIntegrationTest;
import org.wso2.carbon.am.tests.util.APIPublisherRestClient;
import org.wso2.carbon.am.tests.util.APIStoreRestClient;
import org.wso2.carbon.am.tests.util.WireMonitorServer;
import org.wso2.carbon.am.tests.util.bean.*;
import org.wso2.carbon.automation.api.clients.user.mgt.UserManagementClient;
import org.wso2.carbon.automation.api.clients.utils.AuthenticateStub;
import org.wso2.carbon.automation.core.ProductConstant;
import org.wso2.carbon.automation.core.utils.HttpRequestUtil;
import org.wso2.carbon.automation.core.utils.serverutils.ServerConfigurationManager;
import org.wso2.carbon.tenant.mgt.stub.TenantMgtAdminServiceExceptionException;
import org.wso2.carbon.tenant.mgt.stub.TenantMgtAdminServiceStub;
import org.wso2.carbon.tenant.mgt.stub.beans.xsd.TenantInfoBean;
import org.wso2.carbon.utils.multitenancy.MultitenantConstants;

import java.io.File;
import java.net.URL;
import java.rmi.RemoteException;
import java.util.*;

public class EmailUserNameJWTAssertionTestCase extends APIManagerIntegrationTest {

	private APIStoreRestClient apiStore;

	String consumerKey;
	String consumerSecret;

	@Override
	@BeforeClass(alwaysRun = true)
	public void init() throws Exception {
		APIPublisherRestClient apiPublisher;
		String publisherURLHttp;

		ServerConfigurationManager serverConfigurationManager;
		super.init(0);

		String storeURLHttp;
		if (isBuilderEnabled()) {
			publisherURLHttp = getServerURLHttp();
			storeURLHttp = getServerURLHttp();

			String apiManagerXml =
					ProductConstant.getResourceLocations(ProductConstant.AM_SERVER_NAME) +
					File.separator +
					"configFiles/emailusernamejwttest/" +
					"api-manager.xml";

			String userMgtXml =
					ProductConstant.getResourceLocations(ProductConstant.AM_SERVER_NAME) +
					File.separator +
					"configFiles/emailusernamejwttest/" +
					"user-mgt.xml";
			serverConfigurationManager = new ServerConfigurationManager(amServer.getBackEndUrl());
			serverConfigurationManager.applyConfiguration(new File(apiManagerXml));
			serverConfigurationManager.applyConfiguration(new File(userMgtXml));
			super.init(0);
		} else {
			publisherURLHttp = getPublisherServerURLHttp();
			storeURLHttp = getStoreServerURLHttp();

		}
		apiPublisher = new APIPublisherRestClient(publisherURLHttp);
		apiStore = new APIStoreRestClient(storeURLHttp);
		apiPublisher.login(userInfo.getUserName(), userInfo.getPassword());
		APIRequest apiRequest = new APIRequest("test", "test",
		                                       new URL("http://localhost:6789"));
		apiRequest.setVisibility("public");
		apiPublisher.addAPI(apiRequest);
		APILifeCycleStateRequest
				updateRequest = new APILifeCycleStateRequest("test", userInfo.getUserName(),
				                                             APILifeCycleState.PUBLISHED);
		apiPublisher.changeAPILifeCycleStatusTo(updateRequest);
		apiStore.login(userInfo.getUserName(), userInfo.getPassword());
		SubscriptionRequest subscriptionRequest =
				new SubscriptionRequest("test", userInfo.getUserName());
		apiStore.subscribe(subscriptionRequest);
		GenerateAppKeyRequest generateAppKeyRequest =
				new GenerateAppKeyRequest("DefaultApplication");
		String responseString = apiStore.generateApplicationKey(generateAppKeyRequest).getData();
		JSONObject response = new JSONObject(responseString);
		consumerKey =
				response.getJSONObject("data").getJSONObject("key").get("consumerKey").toString();
		consumerSecret = response.getJSONObject("data").getJSONObject("key").get("consumerSecret")
		                         .toString();
	}

	@Test(groups = {
			"wso2.am" }, description = "username JWT-Token Generation test for super tenant")
	public void userNameInSuperTenantJWTTokenTestCase() throws Exception {
		String requestBody =
				"grant_type=password&username=" + userInfo.getUserName() + "&password=" +
				userInfo.getPassword();
		URL tokenEndpointURL = new URL("http://localhost:8280/token");
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
		HttpRequestUtil.doGet(getApiInvocationURLHttp("test/1.0.0/"), requestHeaders);
		String wireLog = wireServer.getCapturedMessage();
		if (wireLog.contains("JWT-Assertion: ")) {
			wireLog = wireLog.split("JWT-Assertion: ")[1];
			int firstDotSeparatorIndex = wireLog.indexOf('.');
			int secondSeparatorIndex = wireLog.indexOf('.', firstDotSeparatorIndex + 1);
			String JWTToken = wireLog.substring(firstDotSeparatorIndex + 1, secondSeparatorIndex);
			byte[] decodedJwt = Base64.decodeBase64(JWTToken);
			JSONObject jsonObject = new JSONObject(new String(decodedJwt));
			Assert.assertEquals(jsonObject.get("iss"), "wso2.org/products/am");
			Assert.assertEquals(jsonObject.get("http://wso2.org/claims/subscriber"), "admin");
			Assert.assertEquals(jsonObject.get("http://wso2.org/claims/applicationid"), "1");
			Assert.assertEquals(jsonObject.get("http://wso2.org/claims/applicationname"),
			                    "DefaultApplication");
			Assert.assertEquals(jsonObject.get("http://wso2.org/claims/applicationtier"),
			                    "Unlimited");
			Assert.assertEquals(jsonObject.get("http://wso2.org/claims/apicontext"), "/test");
			Assert.assertEquals(jsonObject.get("http://wso2.org/claims/version"), "1.0.0");
			Assert.assertEquals(jsonObject.get("http://wso2.org/claims/tier"), "Gold");
			Assert.assertEquals(jsonObject.get("http://wso2.org/claims/keytype"), "PRODUCTION");
			Assert.assertEquals(jsonObject.get("http://wso2.org/claims/usertype"),
			                    "APPLICATION_USER");
			Assert.assertEquals(jsonObject.get("http://wso2.org/claims/enduser"),
			                    "admin@carbon.super");
			Assert.assertEquals(jsonObject.get("http://wso2.org/claims/enduserTenantId"), "-1234");
		}
	}

	@Test(groups = {
			"wso2.am" }, description = "email username JWT-Token Generation test for super tenant")
	public void emailUserNameInSuperTenantJWTTokenTestCase() throws Exception {
		String userName = "admin@wso2.com";
		String password = "admin123";
		UserManagementClient userManagementClient =
				new UserManagementClient(amServer.getBackEndUrl(), "admin", "admin");
		userManagementClient
				.addUser(userName, password, new String[] { "Internal/subscriber" }, "admin2");
		String requestBody = "grant_type=password&username=" + userName + "@" +
		                     MultitenantConstants.SUPER_TENANT_DOMAIN_NAME + "&password=" +
		                     password;
		URL tokenEndpointURL = new URL("http://localhost:8280/token");
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
		HttpRequestUtil.doGet(getApiInvocationURLHttp("test/1.0.0/"), requestHeaders);
		String wireLog = wireServer.getCapturedMessage();
		if (wireLog.contains("JWT-Assertion: ")) {
			wireLog = wireLog.split("JWT-Assertion: ")[1];
			int firstDotSeparatorIndex = wireLog.indexOf('.');
			int secondSeparatorIndex = wireLog.indexOf('.', firstDotSeparatorIndex + 1);
			String JWTToken = wireLog.substring(firstDotSeparatorIndex + 1, secondSeparatorIndex);
			byte[] decodedJwt = Base64.decodeBase64(JWTToken);
			JSONObject jsonObject = new JSONObject(new String(decodedJwt));
			Assert.assertEquals(jsonObject.get("iss"), "wso2.org/products/am");
			Assert.assertEquals(jsonObject.get("http://wso2.org/claims/subscriber"), "admin");
			Assert.assertEquals(jsonObject.get("http://wso2.org/claims/applicationid"), "1");
			Assert.assertEquals(jsonObject.get("http://wso2.org/claims/applicationname"),
			                    "DefaultApplication");
			Assert.assertEquals(jsonObject.get("http://wso2.org/claims/applicationtier"),
			                    "Unlimited");
			Assert.assertEquals(jsonObject.get("http://wso2.org/claims/apicontext"), "/test");
			Assert.assertEquals(jsonObject.get("http://wso2.org/claims/version"), "1.0.0");
			Assert.assertEquals(jsonObject.get("http://wso2.org/claims/tier"), "Gold");
			Assert.assertEquals(jsonObject.get("http://wso2.org/claims/keytype"), "PRODUCTION");
			Assert.assertEquals(jsonObject.get("http://wso2.org/claims/usertype"),
			                    "APPLICATION_USER");
			Assert.assertEquals(jsonObject.get("http://wso2.org/claims/enduser"),
			                    "admin@wso2.com@carbon.super");
			Assert.assertEquals(jsonObject.get("http://wso2.org/claims/enduserTenantId"), "-1234");
			Assert.assertEquals(jsonObject.get("http://wso2.org/claims/role"),
			                    "Internal/subscriber,Internal/everyone");

		}
	}

	@Test(groups = { "wso2.am" }, description = "username JWT-Token Generation test for  tenant")
	public void UserNameInTenantJWTTokenTestCase() throws Exception {

		String userName = "tenant";
		String password = "admin123";
		String domainName = "adc.com";
		String fullUserName = userName + "@" + domainName;
		boolean isSuccessful =
				createTenantWithEmailUserName(userName, password,
				                              domainName, amServer.getBackEndUrl());
		Assert.assertEquals(isSuccessful, true);
		UserManagementClient userManagementClient =
				new UserManagementClient(amServer.getBackEndUrl(), fullUserName, password);
		userManagementClient
				.addRemoveRolesOfUser(fullUserName, new String[] { "Internal/subscriber" }, null);
		String requestBody =
				"grant_type=password&username=" + fullUserName + "&password=" + password;
		URL tokenEndpointURL = new URL("http://localhost:8280/token");
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
		HttpRequestUtil.doGet(getApiInvocationURLHttp("test/1.0.0/"), requestHeaders);
		String wireLog = wireServer.getCapturedMessage();
		if (wireLog.contains("JWT-Assertion: ")) {
			wireLog = wireLog.split("JWT-Assertion: ")[1];
			int firstDotSeparatorIndex = wireLog.indexOf('.');
			int secondSeparatorIndex = wireLog.indexOf('.', firstDotSeparatorIndex + 1);
			String JWTToken = wireLog.substring(firstDotSeparatorIndex + 1, secondSeparatorIndex);
			byte[] decodedJwt = Base64.decodeBase64(JWTToken);

			JSONObject jsonObject = new JSONObject(new String(decodedJwt));
			Assert.assertEquals(jsonObject.get("iss"), "wso2.org/products/am");
			Assert.assertEquals(jsonObject.get("http://wso2.org/claims/subscriber"), "admin");
			Assert.assertEquals(jsonObject.get("http://wso2.org/claims/applicationid"), "1");
			Assert.assertEquals(jsonObject.get("http://wso2.org/claims/applicationname"),
			                    "DefaultApplication");
			Assert.assertEquals(jsonObject.get("http://wso2.org/claims/applicationtier"),
			                    "Unlimited");
			Assert.assertEquals(jsonObject.get("http://wso2.org/claims/apicontext"), "/test");
			Assert.assertEquals(jsonObject.get("http://wso2.org/claims/version"), "1.0.0");
			Assert.assertEquals(jsonObject.get("http://wso2.org/claims/tier"), "Gold");
			Assert.assertEquals(jsonObject.get("http://wso2.org/claims/keytype"), "PRODUCTION");
			Assert.assertEquals(jsonObject.get("http://wso2.org/claims/usertype"),
			                    "APPLICATION_USER");
			Assert.assertEquals(jsonObject.get("http://wso2.org/claims/enduser"), "tenant@adc.com");
			Assert.assertEquals(jsonObject.get("http://wso2.org/claims/givenname"), "admin");
			Assert.assertEquals(jsonObject.get("http://wso2.org/claims/lastname"),
			                    "adminwso2automation");
			Assert.assertEquals(jsonObject.get("http://wso2.org/claims/role"),
			                    "admin,Internal/subscriber,Internal/everyone");
		}
	}

	@Test(groups = {
			"wso2.am" }, description = "email username JWT-Token Generation test for  tenant")
	public void emailUserNameInTenantJWTTokenTestCase() throws Exception {

		String userNameWithEmail = "tenant@wso2.com";
		String password = "admin123";
		String domainName = "adc.com";
		String fullUserName = userNameWithEmail + "@" + domainName;
		UserManagementClient userManagementClient =
				new UserManagementClient(amServer.getBackEndUrl(), "tenant@adc.com", "admin123");
		userManagementClient
				.addUser(userNameWithEmail, password, new String[] { "Internal/subscriber" },
				         "abc");
		String requestBody =
				"grant_type=password&username=" + fullUserName + "&password=" + password;
		URL tokenEndpointURL = new URL("http://localhost:8280/token");

		JSONObject accessTokenGenerationResponse = new JSONObject(
				apiStore.generateUserAccessKey(consumerKey, consumerSecret, requestBody,
				                               tokenEndpointURL).getData());
		String userAccessToken = accessTokenGenerationResponse.getString("access_token");
		Map<String, String> requestHeaders = new HashMap<String, String>();
		requestHeaders.put("Authorization", "Bearer " + userAccessToken);
		Thread.sleep(2000);
		WireMonitorServer wireServer = new WireMonitorServer(6789);
		wireServer.start();
		HttpRequestUtil.doGet(getApiInvocationURLHttp("test/1.0.0/"), requestHeaders);
		String wireLog = wireServer.getCapturedMessage();
		if (wireLog.contains("JWT-Assertion: ")) {
			wireLog = wireLog.split("JWT-Assertion: ")[1];
			int firstDotSeparatorIndex = wireLog.indexOf('.');
			int secondSeparatorIndex = wireLog.indexOf('.', firstDotSeparatorIndex + 1);
			String JWTToken = wireLog.substring(firstDotSeparatorIndex + 1, secondSeparatorIndex);
			byte[] decodedJwt = Base64.decodeBase64(JWTToken);
			JSONObject jsonObject = new JSONObject(new String(decodedJwt));
			Assert.assertEquals(jsonObject.get("iss"), "wso2.org/products/am");
			Assert.assertEquals(jsonObject.get("http://wso2.org/claims/subscriber"), "admin");
			Assert.assertEquals(jsonObject.get("http://wso2.org/claims/applicationid"), "1");
			Assert.assertEquals(jsonObject.get("http://wso2.org/claims/applicationname"),
			                    "DefaultApplication");
			Assert.assertEquals(jsonObject.get("http://wso2.org/claims/applicationtier"),
			                    "Unlimited");
			Assert.assertEquals(jsonObject.get("http://wso2.org/claims/apicontext"), "/test");
			Assert.assertEquals(jsonObject.get("http://wso2.org/claims/version"), "1.0.0");
			Assert.assertEquals(jsonObject.get("http://wso2.org/claims/tier"), "Gold");
			Assert.assertEquals(jsonObject.get("http://wso2.org/claims/keytype"), "PRODUCTION");
			Assert.assertEquals(jsonObject.get("http://wso2.org/claims/usertype"),
			                    "APPLICATION_USER");
			Assert.assertEquals(jsonObject.get("http://wso2.org/claims/enduser"),
			                    "tenant@wso2.com@adc.com");
			Assert.assertEquals(jsonObject.get("http://wso2.org/claims/role"),
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
