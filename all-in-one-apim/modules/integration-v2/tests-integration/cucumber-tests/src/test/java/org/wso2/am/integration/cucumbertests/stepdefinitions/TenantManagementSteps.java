/*
 *  Copyright (c) 2026, WSO2 LLC. (http://www.wso2.org) All Rights Reserved.
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

import io.cucumber.java.en.When;
import org.wso2.am.integration.cucumbertests.utils.Identity;
import org.wso2.am.integration.cucumbertests.utils.Requests;
import org.wso2.am.integration.cucumbertests.utils.Utils;
import org.wso2.carbon.automation.engine.context.beans.User;

import java.io.IOException;

/**
 * Steps for tenant CREATION through the {@code TenantMgtAdminService} SOAP admin service — the only interface
 * APIM exposes for it (there is no tenant-creation REST equivalent), which is the CLAUDE.md §14 narrow
 * exception: the call is made from a step, as the ACTING actor's credentials, through the {@code Requests}
 * funnel so the response is published for the feature's assertion.
 *
 * <p>Distinct from {@link TenantSharingSteps}, whose steps drive tenant lifecycle through the IS-7
 * tenant-sharing {@code /internal/data/v1/notify} endpoint, and from
 * {@code utils/TenantUserProvisioner#addTenant}, which creates the block's tenants at BOOT as infrastructure
 * and asserts success internally. Per §12 a positive create step cannot serve a negative, so the step below is
 * the non-asserting {@code "I attempt to …"} variant: it publishes whatever the service answers (including a
 * SOAP fault) and the feature pins the exact status and fault text.
 */
public class TenantManagementSteps {

    /**
     * Attempts to create a tenant with the given domain via {@code TenantMgtAdminService.addTenant}, as the
     * ACTING actor (basic auth — the admin service authenticates carbon credentials; tenant creation is a
     * super-tenant-admin operation, so the scenario must act as a super-tenant admin).
     *
     * <p>Deliberately asserts NOTHING. A domain the product rejects (e.g. one containing an upper-case letter)
     * comes back as an Axis2 SOAP fault, which {@code SimpleHTTPClient} returns as an ordinary response rather
     * than throwing — so the feature asserts the exact status and the fault text. The interpolated values are
     * XML-escaped so a domain/password containing a metacharacter cannot corrupt the envelope.
     *
     * @param tenantDomain  the tenant domain to attempt (valid or deliberately invalid)
     * @param adminPassword password for the tenant's {@code admin} user
     */
    @When("I attempt to create a tenant with domain {string} and admin password {string}")
    public void iAttemptToCreateTenant(String tenantDomain, String adminPassword) throws IOException {

        String payload = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" "
                + "xmlns:ser=\"http://services.mgt.tenant.carbon.wso2.org\" "
                + "xmlns:xsd=\"http://beans.common.stratos.carbon.wso2.org/xsd\">"
                + "<soapenv:Header/><soapenv:Body>"
                + "<ser:addTenant>"
                + "<ser:tenantInfoBean>"
                + "<xsd:active>true</xsd:active>"
                + "<xsd:admin>admin</xsd:admin>"
                + "<xsd:adminPassword>" + Utils.escapeXml(adminPassword) + "</xsd:adminPassword>"
                + "<xsd:email>admin@" + Utils.escapeXml(tenantDomain) + "</xsd:email>"
                + "<xsd:firstname>Tenant</xsd:firstname>"
                + "<xsd:lastname>Validation</xsd:lastname>"
                + "<xsd:tenantDomain>" + Utils.escapeXml(tenantDomain) + "</xsd:tenantDomain>"
                + "</ser:tenantInfoBean>"
                + "</ser:addTenant>"
                + "</soapenv:Body></soapenv:Envelope>";

        User actor = Identity.actingActor();
        Requests.soap(Utils.getTenantMgtAdminServiceURL(Utils.getBaseUrl()), payload, "urn:addTenant",
                actor.getUserName(), actor.getPassword());
    }
}
