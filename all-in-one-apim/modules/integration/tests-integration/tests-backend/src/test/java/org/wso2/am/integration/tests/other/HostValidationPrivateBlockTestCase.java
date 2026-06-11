/*
 * Copyright (c) 2026, WSO2 LLC. (http://www.wso2.com).
 *
 * WSO2 LLC. licenses this file to you under the Apache License,
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

package org.wso2.am.integration.tests.other;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.apache.http.HttpStatus;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.am.integration.clients.admin.ApiException;
import org.wso2.am.integration.clients.admin.api.dto.KeyManagerCertificatesDTO;
import org.wso2.am.integration.clients.admin.api.dto.KeyManagerDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.ApiEndpointValidationResponseDTO;
import org.wso2.am.integration.test.impl.DtoFactory;
import org.wso2.am.integration.test.utils.base.APIMIntegrationBaseTest;
import org.wso2.carbon.automation.engine.context.TestUserMode;

import java.util.Collections;
import java.util.Map;

/**
 * Integration tests for outbound host validation with block_private_network_access=true.
 */
public class HostValidationPrivateBlockTestCase extends APIMIntegrationBaseTest {

    private static final String LOOPBACK_URL = "http://127.0.0.1:9999/api";

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {
        super.init(TestUserMode.SUPER_TENANT_ADMIN);
    }

    @Test(groups = {"wso2.am"},
            description = "[bpna=true]: loopback/link-local blocked across endpoint validation, KM create, and WSDL import")
    public void testPrivateNetworkBlock_MultipleAPISurfacesBlocked() throws Exception {
        ApiEndpointValidationResponseDTO endpointDto =
                restAPIPublisher.validateEndpointRaw(LOOPBACK_URL, null);
        Assert.assertNotNull(endpointDto, "Endpoint validation response must not be null");
        Assert.assertNotNull(endpointDto.getError(),
                "Expected host validation error for loopback URL when block_private_network_access=true");
        Assert.assertTrue(endpointDto.getError().contains("not trusted"),
                "Expected private network block for loopback, got: " + endpointDto.getError());

        try {
            restAPIAdmin.addKeyManager(buildKeyManagerDTO("HVLinkLocalKM",
                    "https://169.254.169.254/oauth2/introspect",
                    "https://169.254.169.254/keymanager-operations/dcr/register",
                    "https://169.254.169.254/oauth2/token",
                    "https://169.254.169.254/oauth2/revoke",
                    null));
            Assert.fail("Expected ApiException for KM with link-local URL");
        } catch (ApiException e) {
            Assert.assertEquals(e.getCode(), HttpStatus.SC_BAD_REQUEST,
                    "Expected HTTP 400 for KM with link-local (169.254.x.x) URL");
            Assert.assertTrue(e.getResponseBody() != null && e.getResponseBody().contains("not trusted"),
                    "Expected host validation block error in KM create response body, got: " + e.getResponseBody());
        }

        try {
            restAPIPublisher.importWSDLSchemaDefinition(null,
                    "http://127.0.0.1:9090/service?wsdl",
                    "{\"name\":\"HVLoopbackWSDL\",\"context\":\"/hvloopbackwsdl\","
                            + "\"version\":\"1.0.0\",\"provider\":\"" + user.getUserName() + "\"}",
                    "SOAP");
            Assert.fail("Expected ApiException for WSDL import from loopback address");
        } catch (org.wso2.am.integration.clients.publisher.api.ApiException e) {
            Assert.assertEquals(e.getCode(), HttpStatus.SC_BAD_REQUEST,
                    "Expected HTTP 400 for WSDL import from loopback address");
            Assert.assertTrue(e.getResponseBody() != null && e.getResponseBody().contains("not trusted"),
                    "Expected host validation block error in WSDL import response body, got: " + e.getResponseBody());
        }
    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
        super.cleanUp();
    }

    private KeyManagerDTO buildKeyManagerDTO(String name, String introspectionEndpoint,
            String clientRegistrationEndpoint, String tokenEndpoint, String revokeEndpoint,
            KeyManagerCertificatesDTO certs) {
        JsonObject additionalProperties = new JsonObject();
        additionalProperties.addProperty("Username", "admin");
        additionalProperties.addProperty("Password", "admin");
        additionalProperties.addProperty("self_validate_jwt", true);
        Object additionalPropertiesMap = new Gson().fromJson(additionalProperties, Map.class);
        return DtoFactory.createKeyManagerDTO(name, null, "WSO2-IS", name,
                introspectionEndpoint, null, clientRegistrationEndpoint,
                tokenEndpoint, revokeEndpoint, null, null, null,
                "azp", "scope", Collections.emptyList(), additionalPropertiesMap, certs);
    }
}
