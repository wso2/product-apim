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

package org.wso2.am.integration.cucumbertests.utils;

import org.testng.Assert;
import org.testng.annotations.Test;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import java.io.IOException;
import java.net.ConnectException;

public class ResourceCleanupDiagnosticsTest {

    @Test
    public void retainsOperationTenantHttpFaultAndCauseWithoutSoapPayload() {
        HttpResponse response = new HttpResponse("<Envelope><Body><Fault><faultcode>env:Server</faultcode>"
                + "<faultstring>Fixture access denied</faultstring></Fault></Body></Envelope>", 500);
        String diagnostic = ResourceCleanup.dcrCleanupDiagnostic("client-id", "owner@tenant1.com", "tenant1.com",
                "deleteApplication", "IdentityApplicationManagementService", response,
                new IOException("Service-provider deletion failed"));
        for (String field : new String[]{"client-id", "owner@tenant1.com", "tenant1.com", "deleteApplication",
                "IdentityApplicationManagementService", "HTTP 500", "env:Server: Fixture access denied",
                "IOException: Service-provider deletion failed"}) {
            Assert.assertTrue(diagnostic.contains(field), diagnostic);
        }
        Assert.assertFalse(diagnostic.contains("<Envelope>"), diagnostic);
    }

    @Test
    public void omitsCredentialBearingFaultsAndEmbeddedRequestExceptions() {
        String credential = "canary-credential-value";
        HttpResponse response = new HttpResponse("<Envelope><Fault><faultstring>consumerSecret=" + credential
                + "</faultstring></Fault></Envelope>", 500);
        IOException failure = new IOException("Request failed: <Envelope><password>" + credential
                + "</password></Envelope>", new IOException("Bearer " + credential));
        String diagnostic = ResourceCleanup.dcrCleanupDiagnostic("client-id", "owner", "carbon.super",
                "deleteApplication", "IdentityApplicationManagementService", response, failure);
        Assert.assertFalse(diagnostic.contains(credential), diagnostic);
        Assert.assertFalse(diagnostic.contains("<Envelope>"), diagnostic);
        Assert.assertFalse(diagnostic.contains("<password>"), diagnostic);
        Assert.assertTrue(diagnostic.contains("HTTP 500"), diagnostic);
        Assert.assertTrue(diagnostic.contains("IOException"), diagnostic);
    }

    @Test
    public void preservesTransportCauseAndRedactsKnownCredentialsAndLogBreaks() {
        String credential = "canary-value";
        IOException failure = new IOException("Attempt by " + credential + "\nfailed",
                new ConnectException("Connection refused"));
        String diagnostic = ResourceCleanup.dcrCleanupDiagnostic("client-id", "owner", "carbon.super",
                "getOAuthApplicationData", "OAuthAdminService", null, failure, credential);
        Assert.assertTrue(diagnostic.contains("HTTP unavailable"), diagnostic);
        Assert.assertTrue(diagnostic.contains("ConnectException: Connection refused"), diagnostic);
        Assert.assertFalse(diagnostic.contains(credential), diagnostic);
        Assert.assertFalse(diagnostic.contains("\n"), diagnostic);
    }
}
