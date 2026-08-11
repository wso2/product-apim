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

package org.wso2.am.integration.cucumbertests.runners.block;

import io.cucumber.testng.CucumberOptions;

/**
 * Runner for the external-IdP console-SSO regression (#17744). Federates the Publisher/Admin/DevPortal consoles
 * to an external WSO2 IS via a multi-option (BasicAuthenticator + OIDC IdP) login step, drives a federated login
 * headlessly with a browser-equivalent HTTP client, then asserts cross-console single sign-on. The multi-option
 * federated {@code /commonauth} request carries the OAuth2 scope list, so on a build whose Tomcat
 * {@code maxHttpHeaderSize} is too small the login is rejected with 400 and never lands - failing this test.
 * Requires the external Identity Server (block param {@code bootExternalIdentityServer=true}).
 */
@CucumberOptions(
        features = {"src/test/resources/features/admin/external_idp_console_sso.feature"},
        glue = {"org.wso2.am.integration.cucumbertests.stepdefinitions"},
        plugin = {"pretty", "html:target/cucumber-report/external-idp-console-sso.html"}
)
public class ExternalIdpConsoleSsoRunner extends BaseBlockRunner {
}
