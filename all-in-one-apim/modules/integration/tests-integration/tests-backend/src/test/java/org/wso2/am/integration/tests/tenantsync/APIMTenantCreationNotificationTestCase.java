/*
 *Copyright (c) 2025, WSO2 LLC. (http://www.wso2.org) All Rights Reserved.
 *
 *WSO2 LLC. licenses this file to you under the Apache License,
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

package org.wso2.am.integration.tests.tenantsync;

import com.google.gson.Gson;
import org.apache.commons.httpclient.HttpStatus;
import org.junit.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;
import org.wso2.am.integration.clients.admin.api.dto.KeyManagerListDTO;
import org.wso2.am.integration.test.impl.RestAPIAdminImpl;
import org.wso2.am.integration.test.utils.base.APIMIntegrationBaseTest;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.http.HTTPSClientUtils;
import org.wso2.am.integration.tests.tenantsync.model.TenantManagementEvent;
import org.wso2.carbon.automation.engine.annotations.ExecutionEnvironment;
import org.wso2.carbon.automation.engine.annotations.SetEnvironment;
import org.wso2.carbon.automation.engine.context.AutomationContext;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;
import org.wso2.carbon.integration.common.utils.FileManager;
import org.wso2.carbon.integration.common.utils.mgt.ServerConfigurationManager;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;

/**
 * Test Tenant related actions when tenant related notification received to APIM.
 */
@SetEnvironment(executionEnvironments = {ExecutionEnvironment.STANDALONE})
public class APIMTenantCreationNotificationTestCase extends APIMIntegrationBaseTest {

    private final String TENANT_DOMAIN = "testtenant.com";
    private final String TENANT_ADMIN = "admin";
    private final String TENANT_ADMIN_PWD = "admin123";
    private final String TENANT_ADMIN_PWD_UPDATE = "admin456";
    private final String TENANT_ADMIN_FIRST_NAME = "John";
    private final String TENANT_ADMIN_LAST_NAME = "Doe";
    private final String TENANT_ADMIN_EMAIL = "mymail@test.com";

    public static final String ACTION_CREATE = "CREATE";
    public static final String ACTION_UPDATE = "UPDATE";
    public static final String ACTION_ACTIVATE = "ACTIVATE";
    public static final String ACTION_DEACTIVATE = "DEACTIVATE";
    public static final String EVENT_CREATE_TENANT_URI =
            "https://schemas.identity.wso2.org/events/tenant/event-type/tenantCreated";
    public static final String EVENT_UPDATE_TENANT_URI =
            "https://schemas.identity.wso2.org/events/tenant/event-type/tenantOwnerUpdated";
    public static final String EVENT_ACTIVATE_TENANT_URI =
            "https://schemas.identity.wso2.org/events/tenant/event-type/tenantActivated";
    public static final String EVENT_INITIATOR = "SYSTEM";

    Map<String, String> requestHeaders = new HashMap<>();
    private String invokeURL;
    private RestAPIAdminImpl adminClient;

    @Factory(dataProvider = "userModeDataProvider")
    public APIMTenantCreationNotificationTestCase(TestUserMode userMode) {

        this.userMode = userMode;
    }

    @DataProvider
    public static Object[][] userModeDataProvider() {

        return new Object[][]{
                {TestUserMode.SUPER_TENANT_ADMIN}
        };
    }

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {

        super.init(userMode);
        requestHeaders.put("Content-Type", "application/json");
        requestHeaders.put("Authorization", "Basic YWRtaW46YWRtaW4=");
        requestHeaders.put("X-WSO2-KEY-MANAGER", "TENANT_MANAGEMENT");

        invokeURL = keyManagerHTTPSURL + "internal/data/v1/notify";
    }

    protected TenantManagementEvent buildPayload(String type, String eventURI, String tenantAdminPassword,
                                                 boolean isActive) {

        // Start building the Tenant object first, as it's nested.
        TenantManagementEvent.Tenant.Builder tenantBuilder = new TenantManagementEvent.Tenant.Builder()
                .id("1234")
                .domain(TENANT_DOMAIN)
                .ref("https://localhost:9444/api/server/v1/tenants/1234");

        // Conditionally build and add owner details for create/update events.
        if (EVENT_CREATE_TENANT_URI.equals(eventURI)
                || EVENT_UPDATE_TENANT_URI.equals(eventURI)) {

            TenantManagementEvent.Owner.Builder ownerBuilder = new TenantManagementEvent.Owner.Builder()
                    .password(tenantAdminPassword)
                    .email(TENANT_ADMIN_EMAIL)
                    .firstname(TENANT_ADMIN_FIRST_NAME)
                    .lastname(TENANT_ADMIN_LAST_NAME);

            // Username is only set during creation.
            if (EVENT_CREATE_TENANT_URI.equals(eventURI)) {
                ownerBuilder.username(TENANT_ADMIN);
            }

            // Build the owner and add it to the tenant builder.
            tenantBuilder.owners(Collections.singletonList(ownerBuilder.build()));
        }

        // Conditionally build and add lifecycle status for activation events.
        if (EVENT_ACTIVATE_TENANT_URI.equals(eventURI)) {
            TenantManagementEvent.LifecycleStatus lifecycleStatus =
                    new TenantManagementEvent.LifecycleStatus.Builder()
                            .activated(isActive)
                            .build();
            tenantBuilder.lifecycleStatus(lifecycleStatus);
        }

        // Build the final, immutable Tenant object.
        TenantManagementEvent.Tenant tenant = tenantBuilder.build();

        // Build the EventDetail object, including the tenant.
        TenantManagementEvent.EventDetail createEventDetail = new TenantManagementEvent.EventDetail.Builder()
                .initiatorType(EVENT_INITIATOR)
                .action(type)
                .tenant(tenant)
                .build();

        // Create the events map.
        Map<String, TenantManagementEvent.EventDetail> events = new HashMap<>();
        events.put(eventURI, createEventDetail);

        // Build the final TenantManagementEvent and return it.
        return new TenantManagementEvent.Builder()
                .iss("https://localhost:9444")
                .jti(UUID.randomUUID().toString())
                .iat(System.currentTimeMillis() / 1000L)
                .events(events)
                .build();
    }

    /**
     * Tests tenant creation event.
     */
    @Test(groups = {"wso2.am"}, description = "Test tenant creation event related tasks.")
    public void testCreateTenantEvent() throws Exception {

        String tenantCreatePayload =
                new Gson().toJson(buildPayload(ACTION_CREATE, EVENT_CREATE_TENANT_URI, TENANT_ADMIN_PWD, true));
        HttpResponse serviceResponse = HTTPSClientUtils.
                doPost(invokeURL, requestHeaders, tenantCreatePayload);
        assertEquals(serviceResponse.getResponseCode(), HttpStatus.SC_OK,
                "Failed to invoke Notify endpoint for tenant creation");

        adminClient = new RestAPIAdminImpl(TENANT_ADMIN,TENANT_ADMIN_PWD, TENANT_DOMAIN, adminURLHttps);
        KeyManagerListDTO keymanagers = adminClient.getKeyManagers();

        assertEquals(1, keymanagers.getCount().intValue(), "Invalid Keymanager count");
        assertEquals("WSO2-IS-7", keymanagers.getList().get(0).getType(), "Invalid Keymanager type");


    }

    /**
     * Tests tenant update event.
     */
    @Test(groups = {
            "wso2.am"}, description = "Test tenant update event related tasks.", dependsOnMethods = "testActivateTenantEvent")
    public void testUpdateTenantEvent() throws Exception {

        String tenantCreatePayload =
                new Gson().toJson(buildPayload(ACTION_UPDATE, EVENT_UPDATE_TENANT_URI, TENANT_ADMIN_PWD_UPDATE, true));
        HttpResponse serviceResponse = HTTPSClientUtils.
                doPost(invokeURL, requestHeaders, tenantCreatePayload);
        assertEquals(serviceResponse.getResponseCode(), HttpStatus.SC_OK,
                "Failed to invoke Notify endpoint for tenant update");

        try {
            adminClient = new RestAPIAdminImpl(TENANT_ADMIN,TENANT_ADMIN_PWD, TENANT_DOMAIN, adminURLHttps);
            Assert.assertTrue("Tenant admin password not updated", false);
        } catch (Exception e) {
            Assert.assertTrue("Tenant admin password is updated", true);
        }

        try {
            adminClient = new RestAPIAdminImpl(TENANT_ADMIN,TENANT_ADMIN_PWD_UPDATE, TENANT_DOMAIN, adminURLHttps);
            Assert.assertTrue("Tenant admin password is updated", true);

            KeyManagerListDTO keymanagers = adminClient.getKeyManagers();

            assertEquals(1, keymanagers.getCount().intValue(), "Invalid Keymanager count");
            assertEquals("WSO2-IS-7", keymanagers.getList().get(0).getType(), "Invalid Keymanager type");
            
        } catch (Exception e) {
            Assert.assertTrue("Tenant admin password not is updated", false);
        }

    }

    /**
     * Tests tenant activate event.
     */
    @Test(groups = {
            "wso2.am"}, description = "Test tenant activate event related tasks.", dependsOnMethods = "testDeActivateTenantEvent")
    public void testActivateTenantEvent() throws Exception {

        String tenantCreatePayload =
                new Gson().toJson(buildPayload(ACTION_ACTIVATE, EVENT_ACTIVATE_TENANT_URI, TENANT_ADMIN_PWD, true));
        HttpResponse serviceResponse = HTTPSClientUtils.
                doPost(invokeURL, requestHeaders, tenantCreatePayload);
        assertEquals(serviceResponse.getResponseCode(), HttpStatus.SC_OK,
                "Failed to invoke Notify endpoint for tenant activation");

        try {
            adminClient = new RestAPIAdminImpl(TENANT_ADMIN,TENANT_ADMIN_PWD, TENANT_DOMAIN, adminURLHttps);
            Assert.assertTrue("Tenant is activated", true);
        } catch (Exception e) {
            Assert.assertTrue("Tenant is still not activated", false);
        }
    }

    /**
     * Tests tenant deactivate event.
     */
    @Test(groups = {
            "wso2.am"}, description = "Test tenant deactivate event related tasks.", dependsOnMethods = "testCreateTenantEvent")
    public void testDeActivateTenantEvent() throws Exception {

        String tenantCreatePayload =
                new Gson().toJson(buildPayload(ACTION_DEACTIVATE, EVENT_ACTIVATE_TENANT_URI, TENANT_ADMIN_PWD, false));
        HttpResponse serviceResponse = HTTPSClientUtils.
                doPost(invokeURL, requestHeaders, tenantCreatePayload);
        assertEquals(serviceResponse.getResponseCode(), HttpStatus.SC_OK,
                "Failed to invoke Notify endpoint for tenant deactivation");

        try {
            adminClient = new RestAPIAdminImpl(TENANT_ADMIN,TENANT_ADMIN_PWD, TENANT_DOMAIN, adminURLHttps);
            Assert.assertTrue("Tenant is not deactivated", false);
        } catch (Exception e) {
            Assert.assertTrue("Tenant is deactivated", true);
        }
    }

}
