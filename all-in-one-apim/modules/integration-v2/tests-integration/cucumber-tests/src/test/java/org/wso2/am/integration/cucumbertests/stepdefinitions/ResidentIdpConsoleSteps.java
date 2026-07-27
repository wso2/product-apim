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
import org.testng.Assert;
import org.wso2.am.integration.cucumbertests.utils.Requests;
import org.wso2.am.integration.cucumbertests.utils.TestContext;
import org.wso2.am.integration.cucumbertests.utils.Utils;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Steps for the legacy Carbon MANAGEMENT CONSOLE (session-cookie auth, raw JSP pages under {@code /carbon/}) — a
 * different auth/response model from every other step in this suite (bearer token + JSON), needed specifically for
 * console-page regression coverage (e.g. the Resident IdP edit page). Funnels through {@code Requests.*} like every
 * other HTTP-making step, so the generic {@code Then The response status code should be N} / {@code The response
 * should contain "..."} assertions work unmodified against a console page response.
 *
 * <p>{@code SimpleHTTPClient}'s singleton already disables Apache HttpClient's own cookie jar and auto-redirects
 * (see its ctor), so the {@code JSESSIONID} extracted from the login response can be threaded through explicitly
 * as a plain {@code Cookie} header on the next call — no separate cookie-aware client is needed.
 */
public class ResidentIdpConsoleSteps {

    private static final String SESSION_COOKIE_CONTEXT_KEY = "consoleSessionCookie";

    private final BaseSteps baseSteps = new BaseSteps();

    private String getBaseUrl() {
        return baseSteps.getBaseUrl();
    }

    /**
     * Logs into the Carbon management console via the same form POST a browser uses, with a PLAIN
     * username/password (NOT an {@code Identity} actor reference / OAuth token — the console has no bearer-token
     * auth, and a SOAP-provisioned store user such as {@code SECONDARY.COM/idpViewOnlyUser1} is never registered
     * as a resolvable actor in the first place). Extracts the {@code JSESSIONID} from the login response's
     * {@code Set-Cookie} header and stores it under {@value #SESSION_COOKIE_CONTEXT_KEY} for
     * {@link #iFetchTheManagementConsolePage} to reuse.
     */
    @When("I log into the management console as {string} with password {string}")
    public void iLogIntoTheManagementConsole(String username, String password) throws IOException {

        String resolvedUsername = Utils.resolveContextPlaceholders(username);
        String resolvedPassword = Utils.resolveContextPlaceholders(password);
        String payload = "username=" + resolvedUsername + "&password=" + resolvedPassword;
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/x-www-form-urlencoded");
        HttpResponse response = Requests.post(Utils.getConsoleLoginActionURL(getBaseUrl()), headers, payload,
                "application/x-www-form-urlencoded");

        String setCookie = response.getHeaders() != null ? response.getHeaders().get("Set-Cookie") : null;
        Assert.assertTrue(setCookie != null && setCookie.contains("JSESSIONID="),
                "Console login for '" + resolvedUsername + "' did not return a JSESSIONID cookie: "
                        + response.getData());
        String cookieValue = setCookie.substring(setCookie.indexOf("JSESSIONID="));
        int separatorIndex = cookieValue.indexOf(';');
        TestContext.set(SESSION_COOKIE_CONTEXT_KEY, separatorIndex == -1 ? cookieValue
                : cookieValue.substring(0, separatorIndex));
    }

    /**
     * Fetches a Carbon management console page (relative to {@code /carbon/}, e.g.
     * {@code idpmgt/idp-mgt-edit-local.jsp}) reusing the session established by
     * {@link #iLogIntoTheManagementConsole}. Publishes the response via {@code Requests.get} exactly like any
     * REST call, so the generic status-code/body-contains assertions apply unmodified.
     */
    @When("I fetch the management console page {string}")
    public void iFetchTheManagementConsolePage(String relativePath) throws IOException {

        String sessionCookie = (String) TestContext.resolve(SESSION_COOKIE_CONTEXT_KEY);
        Map<String, String> headers = new HashMap<>();
        headers.put("Cookie", sessionCookie);
        Requests.get(Utils.getConsolePageURL(getBaseUrl(), relativePath), headers);
    }
}
