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
import io.cucumber.java.en.When;
import org.jaxen.JaxenException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.wso2.am.integration.cucumbertests.utils.TestContext;
import org.wso2.am.integration.cucumbertests.utils.Utils;
import org.wso2.am.integration.cucumbertests.utils.clients.SimpleHTTPClient;
import org.wso2.am.integration.test.utils.Constants;
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


    @When("I add a new tenant with the following details")
    public void addTenant(DataTable dataTable) throws IOException, JaxenException {

        HttpResponse existingTenantsResponse = (HttpResponse) TestContext.get("existingTenantsResponse");
        List<String> existingTenantDomains = Utils.getNodeTextsByXPath(existingTenantsResponse.getData(),
                "//*[local-name()='tenantDomain']");

        Map<String, String> tenantData = dataTable.asMap(String.class, String.class);
        String tenantDomain = tenantData.get("Domain");

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
                    "<xsd:admin>" + tenantData.get("Admin Username") + "</xsd:admin>" +
                    "<xsd:adminPassword>" + tenantData.get("Admin Password") + "</xsd:adminPassword>" +
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
        }
    }

    @When("I retrieve all existing users in the tenant using tenant admin username {string} and password {string}")
    public void retrieveAllUsersInTenant(String tenantAdminUsername, String tenantAdminPassword) throws IOException {

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

        String url = Utils.getRemoteUserStoreManagerServiceURL(baseUrl);
        HttpResponse response = SimpleHTTPClient.getInstance().sendSoapRequest(url, payload, "urn:listUsers",
                tenantAdminUsername, tenantAdminPassword);

        Assert.assertEquals(response.getResponseCode(), 200, response.getData());
        TestContext.set("existingTenantUsersResponse", response);
    }

    @When("I add user {string} with password {string} and roles {string} using tenant admin username {string} and password {string}")
    public void addUserWithRoles(String username, String password, String roles, String tenantAdminUsername,
                                 String tenantAdminPassword) throws IOException, JaxenException {

        HttpResponse existingTenantUsersResponse = (HttpResponse) TestContext.get("existingTenantUsersResponse");
        List<String> existingTenantUsers = Utils.getNodeTextsByXPath(existingTenantUsersResponse.getData(),
                "//*[local-name()='listUsersResponse']/*[local-name()='return']");

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
                    tenantAdminUsername, tenantAdminPassword);
            Assert.assertEquals(response.getResponseCode(), 200, response.getData());
        }
    }
}
