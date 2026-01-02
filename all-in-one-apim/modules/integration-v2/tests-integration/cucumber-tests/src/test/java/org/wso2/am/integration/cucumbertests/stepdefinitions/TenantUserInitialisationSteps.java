/*
 *  Copyright (c) 2025, WSO2 LLC. (http://www.wso2.org) All Rights Reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.wso2.am.integration.cucumbertests.stepdefinitions;

import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.And;
import io.cucumber.java.en.When;
import org.jaxen.JaxenException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.wso2.am.integration.cucumbertests.utils.TestContext;
import org.wso2.am.integration.cucumbertests.utils.Utils;
import org.wso2.am.integration.cucumbertests.utils.clients.SimpleHTTPClient;
import org.wso2.am.integration.test.utils.Constants;
import org.wso2.carbon.automation.engine.context.beans.Tenant;
import org.wso2.carbon.automation.engine.context.beans.User;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class TenantUserInitialisationSteps {

    private static final Logger logger = LoggerFactory.getLogger(TenantUserInitialisationSteps.class);
    private final String baseUrl;

    public TenantUserInitialisationSteps() {
        baseUrl = TestContext.get("baseUrl").toString();
    }

    @When("I retrieve existing tenant details")
    public void retrieveTenantDomains() throws IOException {

        String payload =
                "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" " +
                        "xmlns:ser=\"http://services.mgt.tenant.carbon.wso2.org\">" +
                        "<soapenv:Header/>" +
                        "<soapenv:Body>" +
                        "<ser:retrieveTenants/>" +
                        "</soapenv:Body>" +
                        "</soapenv:Envelope>";

        String url = Utils.getTenantMgtAdminServiceURL(baseUrl);
        HttpResponse response = SimpleHTTPClient.getInstance().sendSoapRequest(
                url, payload, "urn:retrieveTenants",
                Constants.SUPER_TENANT_ADMIN_USERNAME, Constants.SUPER_TENANT_ADMIN_PASSWORD);

        Assert.assertEquals(response.getResponseCode(), 200, response.getData());
        TestContext.set("existingTenantsResponse", response);
    }

    @When("I add super tenant to context")
    public void addSuperTenantToContext() {

        Tenant superTenant = new Tenant();
        superTenant.setDomain(Constants.SUPER_TENANT_DOMAIN);
        // Super tenant admin
        User admin = new User();
        admin.setKey(Constants.ADMIN_USER_KEY);
        admin.setUserName(Constants.SUPER_TENANT_ADMIN_USERNAME);
        admin.setPassword(Constants.SUPER_TENANT_ADMIN_PASSWORD);
        superTenant.setTenantAdmin(admin);
        TestContext.set(Constants.SUPER_TENANT_DOMAIN, superTenant);
    }

    @And("I add adpsample tenant to context")
    public void iAddAdpsampleTenantToContext() {
        Tenant adpTenant = new Tenant();
        adpTenant.setDomain(Constants.ADPSAMPLE_TENANT_DOMAIN);
        //  tenant admin
        User admin = new User();
        admin.setKey(Constants.ADPSAMPLE_USER_KEY);
        admin.setUserName(Constants.ADPSAMPLE_TENANT_ADMIN_USERNAME);
        admin.setPassword(Constants.ADPSAMPLE_TENANT_ADMIN_PASSWORD);
        adpTenant.setTenantAdmin(admin);
        TestContext.set(Constants.ADPSAMPLE_TENANT_DOMAIN, adpTenant);

    }

    @When("I add a new tenant with the following details")
    public void addTenant(DataTable dataTable) throws IOException, JaxenException {

        HttpResponse existingTenantsResponse = (HttpResponse) TestContext.get("existingTenantsResponse");
        List<String> existingTenantDomains = Utils.getNodeTextsByXPath(existingTenantsResponse.getData(),
                "//*[local-name()='tenantDomain']");

        Map<String, String> tenantData = dataTable.asMap(String.class, String.class);
        String tenantDomain = tenantData.get("Domain");
        String adminUsername = tenantData.get("Admin Username");
        String adminPassword = tenantData.get("Admin Password");


        if (existingTenantDomains.contains(tenantDomain)) {
            logger.info("Tenant with domain {} already exists", tenantDomain);
        } else {
            String payload = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" " +
                    "xmlns:ser=\"http://services.mgt.tenant.carbon.wso2.org\" " +
                    "xmlns:xsd=\"http://beans.common.stratos.carbon.wso2.org/xsd\">" +
                    "<soapenv:Header/><soapenv:Body>" +
                    "<ser:addTenant>" +
                    "<ser:tenantInfoBean>" +
                    "<xsd:active>true</xsd:active>" +
                    "<xsd:admin>" + adminUsername + "</xsd:admin>" +
                    "<xsd:adminPassword>" + adminPassword + "</xsd:adminPassword>" +
                    "<xsd:email>" + tenantData.get("Email") + "</xsd:email>" +
                    "<xsd:firstname>" + tenantData.get("First Name") + "</xsd:firstname>" +
                    "<xsd:lastname>" + tenantData.get("Last Name")+ "</xsd:lastname>" +
                    "<xsd:tenantDomain>" + tenantDomain + "</xsd:tenantDomain>" +
                    "</ser:tenantInfoBean>" +
                    "</ser:addTenant>" +
                    "</soapenv:Body></soapenv:Envelope>";

            String url = Utils.getTenantMgtAdminServiceURL(baseUrl);
            HttpResponse response = SimpleHTTPClient.getInstance().sendSoapRequest(
                    url, payload, "urn:addTenant",
                    Constants.SUPER_TENANT_ADMIN_USERNAME, Constants.SUPER_TENANT_ADMIN_PASSWORD);
            Assert.assertEquals(response.getResponseCode(), 200, response.getData());
            System.out.println(response.getData());
        }
        User admin = new User();
        admin.setKey(Constants.ADMIN_USER_KEY);
        admin.setUserName(adminUsername + Constants.CHAR_AT + tenantDomain);
        admin.setPassword(adminPassword);
        Tenant tenant = new Tenant();
        tenant.setDomain(tenantDomain);
        tenant.setTenantAdmin(admin);
        TestContext.set(tenantDomain, tenant);
    }

    @When("I retrieve all existing users in the tenant domain {string}")
    public void retrieveAllUsersInTenant(String tenantDomain) throws IOException {

        String payload =
                "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" " +
                        "xmlns:ser=\"http://service.ws.um.carbon.wso2.org\">" +
                        "<soapenv:Header/>" +
                        "<soapenv:Body>" +
                        "<ser:listUsers>" +
                        "<ser:filter>*</ser:filter>" +
                        "<ser:maxItemLimit>-1</ser:maxItemLimit>" +
                        "</ser:listUsers>" +
                        "</soapenv:Body>" +
                        "</soapenv:Envelope>";

        Tenant tenant = Utils.getTenantFromContext(tenantDomain);
        User tenantAdmin = tenant.getTenantAdmin();
        System.out.println(tenantAdmin + tenantAdmin.getUserName() +tenantAdmin.getPassword() );


        String url = Utils.getRemoteUserStoreManagerServiceURL(baseUrl);
        HttpResponse response = SimpleHTTPClient.getInstance().sendSoapRequest(url, payload, "urn:listUsers",
                tenantAdmin.getUserName(), tenantAdmin.getPassword());
        System.out.println(url);
        System.out.println(response.getData());

        Assert.assertEquals(response.getResponseCode(), 200, response.getData());
        TestContext.set("existingTenantUsersResponse", response);
    }

    @When("I add user {string} with username {string}, password {string} and roles {string} to the tenant domain {string}")
    public void addUserWithRoles(String userKey, String username, String password, String roles, String tenantDomain)
            throws IOException, JaxenException {

        HttpResponse existingTenantUsersResponse = (HttpResponse) TestContext.get("existingTenantUsersResponse");
        List<String> existingTenantUsers = Utils.getNodeTextsByXPath(existingTenantUsersResponse.getData(),
                "//*[local-name()='listUsersResponse']/*[local-name()='return']");

        Tenant tenant = Utils.getTenantFromContext(tenantDomain);

        if (existingTenantUsers.contains(username)) {
            logger.info("User with username {} already exists in the tenant", username);
        } else {
            String[] rolesList = roles.split("\\s*,\\s*");
            StringBuilder rolesXml = new StringBuilder();
            for (String role : rolesList) {
                rolesXml.append("<xsd:roles>").append(role).append("</xsd:roles>");
            }

            String payload = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" " +
                    "xmlns:xsd=\"http://org.apache.axis2/xsd\">" +
                    "<soapenv:Header/><soapenv:Body>" +
                    "<xsd:addUser>" +
                    "<xsd:userName>" + username + "</xsd:userName>" +
                    "<xsd:password>" + password + "</xsd:password>" +
                    rolesXml +
                    "</xsd:addUser>" +
                    "</soapenv:Body></soapenv:Envelope>";

            String url = Utils.getMultipleCredentialsUserAdminServiceURL(baseUrl);
            HttpResponse response = SimpleHTTPClient.getInstance().sendSoapRequest(url, payload, "urn:addUser",
                    tenant.getTenantAdmin().getUserName(), tenant.getTenantAdmin().getPassword());
            Assert.assertEquals(response.getResponseCode(), 200, response.getData());
        }
        User tenantUser = new User();
        tenantUser.setUserName(username + Constants.CHAR_AT + tenantDomain);
        tenantUser.setPassword(password);
        tenantUser.setKey(userKey);
        tenant.addTenantUsers(tenantUser);
        TestContext.set(tenantDomain, tenant);
    }


}
