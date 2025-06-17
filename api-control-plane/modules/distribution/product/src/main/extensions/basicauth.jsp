<%--
  ~ Copyright (c) 2014, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
  ~
  ~ WSO2 Inc. licenses this file to you under the Apache License,
  ~ Version 2.0 (the "License"); you may not use this file except
  ~ in compliance with the License.
  ~ You may obtain a copy of the License at
  ~
  ~ http://www.apache.org/licenses/LICENSE-2.0
  ~
  ~ Unless required by applicable law or agreed to in writing,
  ~ software distributed under the License is distributed on an
  ~ "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
  ~ KIND, either express or implied.  See the License for the
  ~ specific language governing permissions and limitations
  ~ under the License.
  --%>

<%@ page import="org.apache.cxf.jaxrs.client.Client" %>
<%@ page import="org.apache.cxf.configuration.jsse.TLSClientParameters" %>
<%@ page import="org.apache.cxf.transport.http.HTTPConduit" %>
<%@ page import="org.apache.cxf.jaxrs.client.JAXRSClientFactory" %>
<%@ page import="org.apache.cxf.jaxrs.provider.json.JSONProvider" %>
<%@ page import="org.apache.cxf.jaxrs.client.WebClient" %>
<%@ page import="org.apache.http.HttpStatus" %>
<%@ page import="org.json.JSONObject" %>
<%@ page import="org.owasp.encoder.Encode" %>
<%@ page import="org.wso2.carbon.identity.application.authentication.endpoint.util.client.SelfUserRegistrationResource" %>
<%@ page import="org.wso2.carbon.identity.application.authentication.endpoint.util.AuthenticationEndpointUtil" %>
<%@ page import="org.wso2.carbon.identity.application.authentication.endpoint.util.bean.ResendCodeRequestDTO" %>
<%@ page import="org.wso2.carbon.identity.application.authentication.endpoint.util.bean.PropertyDTO" %>
<%@ page import="org.wso2.carbon.identity.application.authentication.endpoint.util.bean.UserDTO" %>
<%@ page import="java.net.URLEncoder" %>
<%@ page import="java.net.URLDecoder" %>
<%@ page import="java.util.regex.Pattern" %>
<%@ page import="javax.ws.rs.core.Response" %>
<%@ page import="javax.servlet.http.HttpServletRequest" %>
<%@ page import="static org.wso2.carbon.identity.core.util.IdentityUtil.isSelfSignUpEPAvailable" %>
<%@ page import="static org.wso2.carbon.identity.core.util.IdentityUtil.isRecoveryEPAvailable" %>
<%@ page import="static org.wso2.carbon.identity.core.util.IdentityUtil.isEmailUsernameEnabled" %>
<%@ page import="static org.wso2.carbon.identity.core.util.IdentityUtil.getServerURL" %>
<%@ page import="org.apache.commons.codec.binary.Base64" %>
<%@ page import="org.apache.commons.text.StringEscapeUtils" %>
<%@ page import="org.apache.commons.logging.Log" %>
<%@ page import="org.apache.commons.logging.LogFactory" %>
<%@ page import="java.nio.charset.Charset" %>
<%@ page import="org.wso2.carbon.base.ServerConfiguration" %>
<%@ page import="org.wso2.carbon.identity.application.authentication.endpoint.util.EndpointConfigManager" %>
<%@ page import="org.wso2.carbon.identity.core.URLBuilderException" %>
<%@ page import="org.wso2.carbon.identity.core.ServiceURLBuilder" %>
<%@ page import="org.wso2.carbon.identity.mgt.endpoint.util.IdentityManagementEndpointUtil" %>
<%@ page import="org.wso2.carbon.identity.mgt.endpoint.util.client.AdminAdvisoryDataRetrievalClient" %>
<%@ page import="org.wso2.carbon.identity.mgt.endpoint.util.client.ApplicationDataRetrievalClient" %>
<%@ page import="org.wso2.carbon.identity.mgt.endpoint.util.client.ApplicationDataRetrievalClientException" %>
<%@ page import="org.wso2.carbon.identity.mgt.endpoint.util.client.PreferenceRetrievalClient" %>
<%@ page import="org.wso2.carbon.identity.mgt.endpoint.util.client.PreferenceRetrievalClientException" %>
<%@ page import="org.wso2.carbon.utils.CustomHostNameVerifier" %>
<%@ page import="javax.net.ssl.HostnameVerifier" %>
<%@ page import="static org.wso2.carbon.CarbonConstants.ALLOW_ALL" %>
<%@ page import="static org.wso2.carbon.CarbonConstants.DEFAULT_AND_LOCALHOST" %>
<%@ page import="static org.wso2.carbon.CarbonConstants.HOST_NAME_VERIFIER" %>
<%@ page import="org.apache.http.conn.ssl.AllowAllHostnameVerifier" %>

<jsp:directive.include file="includes/init-loginform-action-url.jsp"/>
<jsp:directive.include file="plugins/basicauth-extensions.jsp"/>

<%
    String proxyContextPath = ServerConfiguration.getInstance().getFirstProperty(IdentityCoreConstants
            .PROXY_CONTEXT_PATH);
    if (proxyContextPath == null) {
        proxyContextPath = "";
    }
%>
<script>
    function goBack() {
        document.getElementById("restartFlowForm").submit();
    }

    function onCompleted() {
        $('#loginForm').submit();
    }

    // Handle form submission preventing double submission.
    $(document).ready(function(){
        $.fn.preventDoubleSubmission = function() {
            $(this).on('submit',function(e){
                var $form = $(this);
                if ($form.data('submitted') === true) {
                    // Previously submitted - don't submit again.
                    e.preventDefault();
                    console.warn("Prevented a possible double submit event");
                } else {
                    e.preventDefault();
                    <%
                        if (reCaptchaEnabled) {
                    %>
                    if (!grecaptcha.getResponse()) {
                        grecaptcha.execute();
                        return;
                    }
                    <%
                        }
                    %>
                    var userName = document.getElementById("username");
                    userName.value = userName.value.trim();

                    if (userName.value) {
                        let contextPath = "<%=proxyContextPath%>"
                        let loginRequestPath = "<%=loginContextRequestUrl%>"
                        if (contextPath !== "") {
                            contextPath = contextPath.startsWith('/') ? contextPath : "/" + contextPath
                            contextPath = contextPath.endsWith('/') ?
                                contextPath.substring(0, contextPath.length - 1) : contextPath
                            loginRequestPath = loginRequestPath.startsWith('../') ? loginRequestPath.substring(2, loginRequestPath.length) : loginRequestPath
                        }
                        $.ajax({
                            type: "GET",
                            url: contextPath + loginRequestPath,
                            xhrFields: { withCredentials: true },
                            success: function (data) {
                                if (data && data.status == 'redirect' && data.redirectUrl && data.redirectUrl.length > 0) {
                                    window.location.href = data.redirectUrl;
                                } else if ($form.data('submitted') !== true) {
                                    $form.data('submitted', true);
                                    document.getElementById("loginForm").submit();
                                } else {
                                    console.warn("Prevented a possible double submit event.");
                                }
                            },
                            cache: false
                        });
                    }
                }
            });

            return this;
        };
        $('#loginForm').preventDoubleSubmission();
    });
</script>

<%!
    private static final String JAVAX_SERVLET_FORWARD_REQUEST_URI = "javax.servlet.forward.request_uri";
    private static final String JAVAX_SERVLET_FORWARD_QUERY_STRING = "javax.servlet.forward.query_string";
    private static final String UTF_8 = "UTF-8";
    private static final String TENANT_DOMAIN = "tenant-domain";
    private static final String ACCOUNT_RECOVERY_ENDPOINT = "/accountrecoveryendpoint";
    private static final String ACCOUNT_RECOVERY_ENDPOINT_RECOVER = "/recoveraccountrouter.do";
    private static final String ACCOUNT_RECOVERY_ENDPOINT_REGISTER = "/register.do";
    private Log log = LogFactory.getLog(this.getClass());
%>
<%
    String system_app = request.getParameter("sp");
    Boolean isAdminBannerAllowedInSP = system_app != null && system_app.endsWith("apim_admin_portal");
    Boolean isAdminAdvisoryBannerEnabledInTenant = false;
    String adminAdvisoryBannerContentOfTenant = "";

    try {
        if (isAdminBannerAllowedInSP) {
            AdminAdvisoryDataRetrievalClient adminBannerPreferenceRetrievalClient =
                new AdminAdvisoryDataRetrievalClient();
            JSONObject adminAdvisoryBannerConfig = adminBannerPreferenceRetrievalClient
                .getAdminAdvisoryBannerDataFromServiceStub();
            isAdminAdvisoryBannerEnabledInTenant = adminAdvisoryBannerConfig.getBoolean("enableBanner");
            adminAdvisoryBannerContentOfTenant = adminAdvisoryBannerConfig.getString("bannerContent");
        }
    } catch (Exception e) {
        log.error("Error in displaying admin advisory banner", e);
    }

    String emailUsernameEnable = application.getInitParameter("EnableEmailUserName");
    Boolean isEmailUsernameEnabled = false;
    String usernameLabel = "username";
    Boolean isSelfSignUpEnabledInTenant;
    Boolean isMultiAttributeLoginEnabledInTenant;
    if (StringUtils.isNotBlank(emailUsernameEnable)) {
        isEmailUsernameEnabled = Boolean.valueOf(emailUsernameEnable);
    } else {
        isEmailUsernameEnabled = isEmailUsernameEnabled();
    }
    try {
        PreferenceRetrievalClient preferenceRetrievalClient = new PreferenceRetrievalClient();
        isSelfSignUpEnabledInTenant = preferenceRetrievalClient.checkSelfRegistration(tenantDomain);
        isMultiAttributeLoginEnabledInTenant = preferenceRetrievalClient.checkMultiAttributeLogin(tenantDomain);
    } catch (PreferenceRetrievalClientException e) {
        request.setAttribute("error", true);
        request.setAttribute("errorMsg", AuthenticationEndpointUtil
                .i18n(resourceBundle, "something.went.wrong.contact.admin"));
        IdentityManagementEndpointUtil.addErrorInformation(request, e);
        request.getRequestDispatcher("error.jsp").forward(request, response);
        return;
    }
    if (isEmailUsernameEnabled == true) {
        usernameLabel = "email.username";
    } else if (isMultiAttributeLoginEnabledInTenant) {
        usernameLabel = "user.identifier";
    }

    String resendUsername = request.getParameter("resend_username");

    if (StringUtils.isNotBlank(resendUsername)) {
        ResendCodeRequestDTO selfRegistrationRequest = new ResendCodeRequestDTO();
        UserDTO userDTO = AuthenticationEndpointUtil.getUser(resendUsername);
        selfRegistrationRequest.setUser(userDTO);

        PropertyDTO propertyDTO = new PropertyDTO();
        propertyDTO.setKey("RecoveryScenario");
        propertyDTO.setValue("SELF_SIGN_UP");
        selfRegistrationRequest.getProperties().add(propertyDTO);
        // We have to send an empty property for the client to work properly.
        PropertyDTO dummyPropertyDTO = new PropertyDTO();
        dummyPropertyDTO.setKey("");
        dummyPropertyDTO.setValue("");
        selfRegistrationRequest.getProperties().add(dummyPropertyDTO);

        String path = config.getServletContext().getInitParameter(Constants.ACCOUNT_RECOVERY_REST_ENDPOINT_URL);
        String url;
        if (StringUtils.isNotBlank(EndpointConfigManager.getServerOrigin())) {
            url = IdentityManagementEndpointUtil.getBasePath(tenantDomain, path, false);
        } else {
            url = IdentityUtil.getServerURL(path, true, false);
        }
        url = url.replace(TENANT_DOMAIN, userDTO.getTenantDomain());

        List<JSONProvider> providers = new ArrayList<JSONProvider>();
        JSONProvider jsonProvider = new JSONProvider();
        jsonProvider.setDropRootElement(true);
        jsonProvider.setIgnoreNamespaces(true);
        jsonProvider.setValidateOutput(true);
        jsonProvider.setSupportUnwrapped(true);
        providers.add(jsonProvider);

        String toEncode = EndpointConfigManager.getAppName() + ":" + String
                .valueOf(EndpointConfigManager.getAppPassword());
        byte[] encoding = Base64.encodeBase64(toEncode.getBytes());
        String authHeader = new String(encoding, Charset.defaultCharset());
        String header = "Client " + authHeader;

        SelfUserRegistrationResource selfUserRegistrationResource = JAXRSClientFactory
                .create(url, SelfUserRegistrationResource.class, providers);

        Client client = WebClient.client(selfUserRegistrationResource);
        HTTPConduit conduit = WebClient.getConfig(client).getHttpConduit();
        TLSClientParameters tlsParams = conduit.getTlsClientParameters();
        if (tlsParams == null) {
            tlsParams = new TLSClientParameters();
        }
        HostnameVerifier allowAllHostnameVerifier = new AllowAllHostnameVerifier();
        if (EndpointConfigManager.isHostnameVerificationEnabled()) {
            if (DEFAULT_AND_LOCALHOST.equals(System.getProperty(HOST_NAME_VERIFIER))) {
                /*
                 * If hostname verifier is set to DefaultAndLocalhost, allow following domains in addition to the
                 * hostname:
                 *      ["::1", "127.0.0.1", "localhost", "localhost.localdomain"]
                 */
                tlsParams.setHostnameVerifier(new CustomHostNameVerifier());
            } else if (ALLOW_ALL.equals(System.getProperty(HOST_NAME_VERIFIER))) {
                // If hostname verifier is set to AllowAll, disable hostname verification.
                tlsParams.setHostnameVerifier(allowAllHostnameVerifier);
            }
        } else {
            // Disable hostname verification
            tlsParams.setHostnameVerifier(allowAllHostnameVerifier);
        }
        conduit.setTlsClientParameters(tlsParams);

        WebClient.client(selfUserRegistrationResource).header("Authorization", header);
        Response selfRegistrationResponse = selfUserRegistrationResource.regenerateCode(selfRegistrationRequest);
        if (selfRegistrationResponse != null &&  selfRegistrationResponse.getStatus() == HttpStatus.SC_CREATED) {
%>
<div class="ui visible info message">
    <%=AuthenticationEndpointUtil.i18n(resourceBundle,Constants.ACCOUNT_RESEND_SUCCESS_RESOURCE)%>
</div>
<%
} else {
%>
<div class="ui visible negative message">
    <%=AuthenticationEndpointUtil.i18n(resourceBundle,Constants.ACCOUNT_RESEND_FAIL_RESOURCE)%>
</div>
<%
        }
    }
%>

<% if (isAdminBannerAllowedInSP && isAdminAdvisoryBannerEnabledInTenant) { %>
    <div class="ui warning message" data-componentid="login-page-admin-session-advisory-banner">
        <%=Encode.forHtmlContent(adminAdvisoryBannerContentOfTenant)%>
    </div>
<% } %>

<form class="ui large form" action="<%=loginFormActionURL%>" method="post" id="loginForm">
    <%
        if (loginFormActionURL.equals(samlssoURL) || loginFormActionURL.equals(oauth2AuthorizeURL)) {
    %>
    <input id="tocommonauth" name="tocommonauth" type="hidden" value="true">
    <%
        }
    %>

    <% if (StringUtils.equals(request.getParameter("errorCode"), IdentityCoreConstants.USER_ACCOUNT_LOCKED_ERROR_CODE) &&
            StringUtils.equals(request.getParameter("remainingAttempts"), "0") ) { %>
        <div class="ui visible negative message" id="error-msg" data-testid="login-page-error-message">
            <%=AuthenticationEndpointUtil.i18n(resourceBundle, "error.user.account.locked.incorrect.login.attempts")%>
        </div>
    <% } else if (Boolean.parseBoolean(loginFailed) &&
            !errorCode.equals(IdentityCoreConstants.USER_ACCOUNT_NOT_CONFIRMED_ERROR_CODE)) { %>
    <div class="ui visible negative message" id="error-msg" data-testid="login-page-error-message">
        <%= AuthenticationEndpointUtil.i18n(resourceBundle, errorMessage) %>
    </div>
    <% } else if ((Boolean.TRUE.toString()).equals(request.getParameter("authz_failure"))){%>
    <div class="ui visible negative message" id="error-msg" data-testid="login-page-error-message">
        <%=AuthenticationEndpointUtil.i18n(resourceBundle, "unauthorized.to.login")%>
    </div>
    <% } else { %>
        <div class="ui visible negative message" style="display: none;" id="error-msg" data-testid="login-page-error-message"></div>
    <% } %>
    <% if(Boolean.parseBoolean(request.getParameter("passwordReset"))) {
        %>
            <div class="ui visible positive message" data-testid="password-reset-success-message">
                <%=AuthenticationEndpointUtil.i18n(resourceBundle, "Updated.the.password.successfully")%>
            </div>
    <% } %>

    <% if (!isIdentifierFirstLogin(inputType)) { %>
        <div class="field">
            <div class="ui fluid left icon input">
                <input
                    type="text"
                    id="username"
                    value=""
                    name="username"
                    tabindex="1"
                    placeholder="<%=AuthenticationEndpointUtil.i18n(resourceBundle, "username")%>"
                    data-testid="login-page-username-input"
                    required>
                <i aria-hidden="true" class="user icon"></i>
            </div>
        </div>
    <% } else { %>
        <input id="username" name="username" type="hidden" data-testid="login-page-username-input" value="<%=Encode.forHtmlAttribute(username)%>">
    <% } %>
        <div class="field">
            <div class="ui fluid left icon input addon-wrapper">
                <input
                    type="password"
                    id="password"
                    name="password"
                    value=""
                    autocomplete="off"
                    required
                    tabindex="2"
                    placeholder="<%=AuthenticationEndpointUtil.i18n(resourceBundle, "password")%>"
                    data-testid="login-page-password-input"
                    style="padding-right: 2.3em !important;"
                >
                <i aria-hidden="true" class="lock icon"></i>
                <i id="passwordUnmaskIcon"
                   class="eye icon mr-0"
                   style="margin: 0 auto; right: 0; pointer-events: auto; cursor: pointer;"></i>
            </div>
        </div>
    <%
        if (reCaptchaEnabled) {
    %>
        <div class="g-recaptcha"
                data-size="invisible"
                data-callback="onCompleted"
                data-action="login"
                data-sitekey="<%=Encode.forHtmlContent(reCaptchaKey)%>">
        </div>
    <%
        }
    %>

    <%
        String recoveryEPAvailable = application.getInitParameter("EnableRecoveryEndpoint");
        String enableSelfSignUpEndpoint = application.getInitParameter("EnableSelfSignUpEndpoint");
        Boolean isRecoveryEPAvailable = false;
        Boolean isSelfSignUpEPAvailable = false;
        String identityMgtEndpointContext = "";
         String accountRegistrationEndpointURL = "";
        String urlEncodedURL = "";
        String urlParameters = "";

        if (StringUtils.isNotBlank(recoveryEPAvailable)) {
            isRecoveryEPAvailable = Boolean.valueOf(recoveryEPAvailable);
        } else {
            isRecoveryEPAvailable = isRecoveryEPAvailable();
        }

        if (StringUtils.isNotBlank(enableSelfSignUpEndpoint)) {
            isSelfSignUpEPAvailable = Boolean.valueOf(enableSelfSignUpEndpoint);
        } else {
            isSelfSignUpEPAvailable = isSelfSignUpEPAvailable();
        }

        if (isRecoveryEPAvailable || isSelfSignUpEPAvailable) {
            String urlWithoutEncoding = null;
            try {
                ApplicationDataRetrievalClient applicationDataRetrievalClient = new ApplicationDataRetrievalClient();
                urlWithoutEncoding = applicationDataRetrievalClient.getApplicationAccessURL(tenantDomain,
                                        request.getParameter("sp"));
                urlWithoutEncoding =  IdentityManagementEndpointUtil.replaceUserTenantHintPlaceholder(
                                                                        urlWithoutEncoding, userTenantDomain);
            } catch (ApplicationDataRetrievalClientException e) {
                //ignored and fallback to login page url
            }
            if (StringUtils.isBlank(urlWithoutEncoding)) {
                String scheme = request.getScheme();
                String serverName = request.getServerName();
                int serverPort = request.getServerPort();
                String uri = (String) request.getAttribute(JAVAX_SERVLET_FORWARD_REQUEST_URI);
                String prmstr = URLDecoder.decode(((String) request.getAttribute(JAVAX_SERVLET_FORWARD_QUERY_STRING)), UTF_8);
                urlWithoutEncoding = scheme + "://" +serverName + ":" + serverPort + uri + "?" + prmstr;
            }

            urlEncodedURL = URLEncoder.encode(urlWithoutEncoding, UTF_8);
            urlParameters = (String) request.getAttribute(JAVAX_SERVLET_FORWARD_QUERY_STRING);

            identityMgtEndpointContext = application.getInitParameter("IdentityManagementEndpointContextURL");
            if (StringUtils.isBlank(identityMgtEndpointContext)) {
                try {
                    identityMgtEndpointContext = ServiceURLBuilder.create().addPath(ACCOUNT_RECOVERY_ENDPOINT).build()
                            .getAbsolutePublicURL();
                } catch (URLBuilderException e) {
                    request.setAttribute(STATUS, AuthenticationEndpointUtil.i18n(resourceBundle, CONFIGURATION_ERROR));
                    request.setAttribute(STATUS_MSG, AuthenticationEndpointUtil
                            .i18n(resourceBundle, ERROR_WHILE_BUILDING_THE_ACCOUNT_RECOVERY_ENDPOINT_URL));
                    request.getRequestDispatcher("error.do").forward(request, response);
                    return;
                }
            }

            accountRegistrationEndpointURL = application.getInitParameter("AccountRegisterEndpointURL");
            if (StringUtils.isBlank(accountRegistrationEndpointURL)) {
                accountRegistrationEndpointURL = identityMgtEndpointContext + ACCOUNT_RECOVERY_ENDPOINT_REGISTER;
            }
        }
    %>

    <div class="buttons">
        <% if (isRecoveryEPAvailable) { %>
        <div class="field">
            <%=AuthenticationEndpointUtil.i18n(resourceBundle, "forgot.username.password")%>
            <a
                id="passwordRecoverLink"
                tabindex="6"
                href="<%=StringEscapeUtils.escapeHtml4(getRecoverAccountUrlWithUsername(identityMgtEndpointContext, urlEncodedURL, false, urlParameters, usernameIdentifier))%>"
                data-testid="login-page-password-recovery-button"
            >
                <%=AuthenticationEndpointUtil.i18n(resourceBundle, "forgot.password")%>
            </a>
            ?
        </div>
        <% } %>

        <% if (isIdentifierFirstLogin(inputType)) { %>
        <div class="field">
            <a id="backLink" tabindex="7" onclick="goBack()" data-testid="login-page-back-button">
                <%=AuthenticationEndpointUtil.i18n(resourceBundle, "sign.in.different.account")%>
            </a>
        </div>
        <% } %>
    </div>

    <div class="ui divider hidden"></div>

    <div class="field">
        <div class="ui checkbox">
            <input
                tabindex="3"
                type="checkbox"
                id="chkRemember"
                name="chkRemember"
                data-testid="login-page-remember-me-checkbox"
            >
            <label for="chkRemember"><%=AuthenticationEndpointUtil.i18n(resourceBundle, "remember.me")%></label>
        </div>
    </div>
    <input type="hidden" name="sessionDataKey" value='<%=Encode.forHtmlAttribute
            (request.getParameter("sessionDataKey"))%>'/>

    <div class="ui divider hidden"></div>

    <%
    boolean showCookiePolicy = (Boolean)request.getAttribute("showCookiePolicy");
    if (showCookiePolicy) {
    %>
        <div class="ui visible warning message">
            <%
            String cookiePolicyText = (String)request.getAttribute("cookiePolicyText");
            if (!StringUtils.isEmpty(cookiePolicyText)) {
            %>
                <%=cookiePolicyText%>
            <% } else { %>
                <%=AuthenticationEndpointUtil.i18n(resourceBundle, "privacy.policy.cookies.short.description")%>
            <% } %>
            <a href="cookie_policy.do" target="policy-pane">
                <%=AuthenticationEndpointUtil.i18n(resourceBundle, "privacy.policy.cookies")%>
            </a>
            <%=AuthenticationEndpointUtil.i18n(resourceBundle, "privacy.policy.for.more.details")%>
        </div>
    <% } %>

    <%
    boolean showPrivacyPolicy = (Boolean)request.getAttribute("showPrivacyPolicy");
    if (showPrivacyPolicy) {
    %>
        <div class="ui visible warning message">
            <%
            String privacyPolicyText = (String)request.getAttribute("privacyPolicyText");
            if (!StringUtils.isEmpty(privacyPolicyText)) {
            %>
                <%=privacyPolicyText%>
            <% } else { %>
                <%=AuthenticationEndpointUtil.i18n(resourceBundle, "privacy.policy.privacy.short.description")%>
            <% } %>
            <a href="privacy_policy.do" target="policy-pane">
                <%=AuthenticationEndpointUtil.i18n(resourceBundle, "privacy.policy.general")%>
            </a>
        </div>
    <% } %>
    <div class="ui divider hidden"></div>

    
    <div class="mt-0">
        <div class="column buttons">
            <button
                class="ui primary fluid large button"
                tabindex="4"
                type="submit"
            >
                <%=StringEscapeUtils.escapeHtml4(AuthenticationEndpointUtil.i18n(resourceBundle, "continue"))%>
            </button>
        </div>
        <div class="column buttons">
            <%
            String sp = request.getParameter("sp");
            if ( (sp != null && !sp.endsWith("apim_publisher") && !sp.endsWith("apim_admin_portal")) && isSelfSignUpEPAvailable && !isIdentifierFirstLogin(inputType) && isSelfSignUpEnabledInTenant) { %>
            <button
                type="button"
                onclick="window.location.href='<%=StringEscapeUtils.escapeHtml4(getRegistrationUrl(accountRegistrationEndpointURL, urlEncodedURL, urlParameters))%>';"
                class="ui secondary fluid large button"
                id="registerLink"
                tabindex="8"
                role="button"
                data-testid="login-page-create-account-button"
            >
                <%=StringEscapeUtils.escapeHtml4(AuthenticationEndpointUtil.i18n(resourceBundle, "create.account"))%>
            </button>
            <% } %>
        </div>
    </div>

    <% if (Boolean.parseBoolean(loginFailed) && errorCode.equals(IdentityCoreConstants.USER_ACCOUNT_NOT_CONFIRMED_ERROR_CODE) && request.getParameter("resend_username") == null) { %>
    <div class="ui divider hidden"></div>
    <div class="field">
        <div class="form-actions">
            <%=AuthenticationEndpointUtil.i18n(resourceBundle, "no.confirmation.mail")%>
            <a id="registerLink"
                href="login.do?resend_username=<%=Encode.forHtml(URLEncoder.encode(request.getParameter("failedUsername"), UTF_8))%>&<%=AuthenticationEndpointUtil.cleanErrorMessages(Encode.forJava(request.getQueryString()))%>"
                data-testid="login-page-resend-confirmation-email-link"
            >
                <%=StringEscapeUtils.escapeHtml4(AuthenticationEndpointUtil.i18n(resourceBundle, "resend.mail"))%>
            </a>
        </div>
    </div>
    <% } %>
    <%!
        private String getRecoverAccountUrl(String identityMgtEndpointContext, String urlEncodedURL,
                boolean isUsernameRecovery, String urlParameters) {

            return identityMgtEndpointContext + ACCOUNT_RECOVERY_ENDPOINT_RECOVER + "?" + urlParameters
                    + "&isUsernameRecovery=" + isUsernameRecovery + "&callback=" + Encode
                    .forHtmlAttribute(urlEncodedURL);
        }

        private String getRecoverAccountUrlWithUsername(String identityMgtEndpointContext, String urlEncodedURL,
                boolean isUsernameRecovery, String urlParameters, String username) {
            if (StringUtils.isNotBlank(username)) {
               urlParameters = urlParameters + "&username=" + Encode.forHtmlAttribute(username);
            }
            return identityMgtEndpointContext + ACCOUNT_RECOVERY_ENDPOINT_RECOVER + "?" + urlParameters
                    + "&isUsernameRecovery=" + isUsernameRecovery + "&callback=" + Encode
                    .forHtmlAttribute(urlEncodedURL);
        }

        private String getRegistrationUrl(String accountRegistrationEndpointURL, String urlEncodedURL,
                String urlParameters) {
            return accountRegistrationEndpointURL + "?" + urlParameters + "&callback=" + Encode.forHtmlAttribute(urlEncodedURL);
        }
    %>

    <script defer>

        /**
         * Toggles the password visibility using the attribute
         * type of the input.
         *
         * @param event {Event} click target
         * @description stops propagation
         */
        $("#passwordUnmaskIcon").click(function (event) {
            event.preventDefault();
            var $passwordInput = $("#password");

            if ($passwordInput.attr("type") === "password") {
                $(this).addClass("slash outline");
                $passwordInput.attr("type", "text");
            } else {
                $(this).removeClass("slash outline");
                $passwordInput.attr("type", "password");
            }
        });

        function onSubmitResend(token) {
           $("#resendForm").submit();
        }

    </script>

</form>

<form action="<%=loginFormActionURL%>" method="post" id="restartFlowForm">
    <input type="hidden" name="sessionDataKey" value='<%=Encode.forHtmlAttribute(request.getParameter("sessionDataKey"))%>'/>
    <input type="hidden" name="restart_flow" value='true'/>
    <input id="tocommonauth" name="tocommonauth" type="hidden" value="true">
</form>
