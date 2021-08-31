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

<%@ page import="org.apache.cxf.jaxrs.client.JAXRSClientFactory" %>
<%@ page import="org.apache.cxf.jaxrs.provider.json.JSONProvider" %>
<%@ page import="org.apache.cxf.jaxrs.client.WebClient" %>
<%@ page import="org.apache.http.HttpStatus" %>
<%@ page import="org.owasp.encoder.Encode" %>
<%@ page import="org.wso2.carbon.identity.application.authentication.endpoint.util.client.SelfUserRegistrationResource" %>
<%@ page import="org.wso2.carbon.identity.application.authentication.endpoint.util.AuthenticationEndpointUtil" %>
<%@ page import="org.wso2.carbon.identity.application.authentication.endpoint.util.bean.ResendCodeRequestDTO" %>
<%@ page import="org.wso2.carbon.identity.application.authentication.endpoint.util.bean.UserDTO" %>
<%@ page import="java.net.URLEncoder" %>
<%@ page import="java.net.URLDecoder" %>
<%@ page import="javax.ws.rs.core.Response" %>
<%@ page import="static org.wso2.carbon.identity.core.util.IdentityUtil.isSelfSignUpEPAvailable" %>
<%@ page import="static org.wso2.carbon.identity.core.util.IdentityUtil.isRecoveryEPAvailable" %>
<%@ page import="static org.wso2.carbon.identity.core.util.IdentityUtil.isEmailUsernameEnabled" %>
<%@ page import="static org.wso2.carbon.identity.core.util.IdentityUtil.getServerURL" %>
<%@ page import="org.apache.commons.codec.binary.Base64" %>
<%@ page import="org.apache.commons.text.StringEscapeUtils" %>
<%@ page import="java.nio.charset.Charset" %>
<%@ page import="org.wso2.carbon.base.ServerConfiguration" %>
<%@ page import="org.wso2.carbon.identity.application.authentication.endpoint.util.EndpointConfigManager" %>
<%@ page import="org.wso2.carbon.identity.core.URLBuilderException" %>
<%@ page import="org.wso2.carbon.identity.core.ServiceURLBuilder" %>

<jsp:directive.include file="includes/init-loginform-action-url.jsp"/>

<%
    String emailUsernameEnable = application.getInitParameter("EnableEmailUserName");
    Boolean isEmailUsernameEnabled = false;

    if (StringUtils.isNotBlank(emailUsernameEnable)) {
        isEmailUsernameEnabled = Boolean.valueOf(emailUsernameEnable);
    } else {
        isEmailUsernameEnabled = isEmailUsernameEnabled();
    }
%>

<%
    String proxyContextPath = ServerConfiguration.getInstance().getFirstProperty(IdentityCoreConstants
            .PROXY_CONTEXT_PATH);
    if (proxyContextPath == null) {
        proxyContextPath = "";
    }
%>
<script>
    function goBack() {
        window.history.back();
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

                    var isEmailUsernameEnabled = JSON.parse("<%= isEmailUsernameEnabled %>");
                    var tenantName = getParameterByName("tenantDomain");
                    var userName = document.getElementById("username");
                    var usernameUserInput = document.getElementById("usernameUserInput");

                    if (usernameUserInput) {
                        var usernameUserInputValue = usernameUserInput.value.trim();

                        if (tenantName && tenantName !== "null") {

                            if (isEmailUsernameEnabled) {

                                if (usernameUserInputValue.split("@").length <= 1) {
                                    var errorMessage = document.getElementById("error-msg");

                                    errorMessage.innerHTML = "Invalid Username. Username has to be an email address.";
                                    errorMessage.style.display = "block";

                                    return;
                                }

                                if (usernameUserInputValue.split("@").length === 2) {
                                    userName.value = usernameUserInputValue + "@" + tenantName;
                                }
                                else {
                                    userName.value = usernameUserInputValue;
                                }
                            } else {
                                if (usernameUserInputValue.split("@").length > 1) {
                                    userName.value = usernameUserInputValue;
                                } else {
                                    userName.value = usernameUserInputValue + "@" + tenantName;
                                }

                            }
                            
                        } else {
                            userName.value = usernameUserInputValue;
                        }
                    }

                    if (userName.value) {
                         let contextPath = "<%=proxyContextPath%>"
                        if (contextPath !== "") {
                            contextPath = contextPath.startsWith('/') ? contextPath : "/" + contextPath
                            contextPath = contextPath.endsWith('/') ?
                                contextPath.substring(0, contextPath.length - 1) : contextPath
                        }
                        $.ajax({
                            type: "GET",
                            url: contextPath + "/logincontext?sessionDataKey=" + getParameterByName("sessionDataKey") +
                                "&relyingParty=" + getParameterByName("relyingParty") + "&tenantDomain=" + tenantName,
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
%>
<%
    String resendUsername = request.getParameter("resend_username");
    if (StringUtils.isNotBlank(resendUsername)) {
        ResendCodeRequestDTO selfRegistrationRequest = new ResendCodeRequestDTO();
        UserDTO userDTO = AuthenticationEndpointUtil.getUser(resendUsername);
        selfRegistrationRequest.setUser(userDTO);
        String path = config.getServletContext().getInitParameter(Constants.ACCOUNT_RECOVERY_REST_ENDPOINT_URL);
        String url;
        if (StringUtils.isNotBlank(EndpointConfigManager.getServerOrigin())) {
            url = EndpointConfigManager.getServerOrigin() + proxyContextPath + path;
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

<form class="ui large form" action="<%=loginFormActionURL%>" method="post" id="loginForm">
    <%
        if (loginFormActionURL.equals(samlssoURL) || loginFormActionURL.equals(oauth2AuthorizeURL)) {
    %>
    <input id="tocommonauth" name="tocommonauth" type="hidden" value="true">
    <%
        }
    %>

    <% if (Boolean.parseBoolean(loginFailed)) { %>
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
                    id="usernameUserInput"
                    value=""
                    name="usernameUserInput"
                    tabindex="1"
                    placeholder="<%=AuthenticationEndpointUtil.i18n(resourceBundle, "username")%>"
                    data-testid="login-page-username-input"
                    required>
                <i aria-hidden="true" class="user icon"></i>
                <input id="username" name="username" type="hidden" value="<%=username%>">
            </div>
        </div>
    <% } else { %>
        <input id="username" name="username" type="hidden" data-testid="login-page-username-input" value="<%=username%>">
    <% } %>
        <div class="field">
            <div class="ui fluid left icon input">
                <input
                    type="password"
                    id="password"
                    name="password"
                    value=""
                    autocomplete="off"
                    tabindex="2"
                    placeholder="<%=AuthenticationEndpointUtil.i18n(resourceBundle, "password")%>"
                    data-testid="login-page-password-input"
                >
                <i aria-hidden="true" class="lock icon"></i>
            </div>
        </div>
    <%
        if (reCaptchaEnabled) {
    %>
        <div class="field">
            <div class="g-recaptcha"
                data-sitekey="<%=Encode.forHtmlContent(request.getParameter("reCaptchaKey"))%>"
                data-testid="login-page-g-recaptcha"
            >
            </div>
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
            String scheme = request.getScheme();
            String serverName = request.getServerName();
            int serverPort = request.getServerPort();
            String uri = (String) request.getAttribute(JAVAX_SERVLET_FORWARD_REQUEST_URI);
            String prmstr = URLDecoder.decode(((String) request.getAttribute(JAVAX_SERVLET_FORWARD_QUERY_STRING)), UTF_8);
            String urlWithoutEncoding = scheme + "://" +serverName + ":" + serverPort + uri + "?" + prmstr;
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
        }
    %>

    <div class="buttons">
        <% if (isRecoveryEPAvailable) { %>
        <div class="field">
            <%=AuthenticationEndpointUtil.i18n(resourceBundle, "forgot.username.password")%>
            <a
                id="passwordRecoverLink"
                tabindex="6"
                href="<%=StringEscapeUtils.escapeHtml4(getRecoverAccountUrl(identityMgtEndpointContext, urlEncodedURL, false, urlParameters))%>"
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
            <label><%=AuthenticationEndpointUtil.i18n(resourceBundle, "remember.me")%></label>
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

    <div class="ui two column stackable grid">
        <div class="column mobile center aligned tablet left aligned computer left aligned buttons tablet no-padding-left-first-child computer no-padding-left-first-child">
            <%
            String sp = request.getParameter("sp");
            if ( (sp != null && !sp.endsWith("apim_publisher")) && isSelfSignUpEPAvailable && !isIdentifierFirstLogin(inputType)) { %>
            <button
                type="button"
                onclick="window.location.href='<%=StringEscapeUtils.escapeHtml4(getRegistrationUrl(identityMgtEndpointContext, urlEncodedURL, urlParameters))%>';"
                class="ui large button link-button"
                id="registerLink"
                tabindex="8"
                role="button"
                data-testid="login-page-create-account-button"
            >
                <%=StringEscapeUtils.escapeHtml4(AuthenticationEndpointUtil.i18n(resourceBundle, "create.account"))%>
            </button>
            <% } %>
        </div>
        <div class="column mobile center aligned tablet right aligned computer right aligned buttons tablet no-margin-right-last-child computer no-margin-right-last-child">
            <button
                type="submit"
                class="ui primary large button"
                tabindex="4"
                role="button"
                data-testid="login-page-continue-login-button"
            >
                <%=StringEscapeUtils.escapeHtml4(AuthenticationEndpointUtil.i18n(resourceBundle, "continue"))%>
            </button>
        </div>
    </div>

    <% if (Boolean.parseBoolean(loginFailed) && errorCode.equals(IdentityCoreConstants.USER_ACCOUNT_NOT_CONFIRMED_ERROR_CODE) && request.getParameter("resend_username") == null) { %>
    <div class="ui divider hidden"></div>
    <div class="field">
        <div class="form-actions">
            <%=AuthenticationEndpointUtil.i18n(resourceBundle, "no.confirmation.mail")%>
            <a id="registerLink"
                href="login.do?resend_username=<%=Encode.forHtml(request.getParameter("failedUsername"))%>&<%=AuthenticationEndpointUtil.cleanErrorMessages(Encode.forJava(request.getQueryString()))%>"
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

        private String getRegistrationUrl(String identityMgtEndpointContext, String urlEncodedURL,
                String urlParameters) {

            return identityMgtEndpointContext + ACCOUNT_RECOVERY_ENDPOINT_REGISTER + "?"
                    + urlParameters + "&callback=" + Encode.forHtmlAttribute(urlEncodedURL);
        }

    %>
</form>
