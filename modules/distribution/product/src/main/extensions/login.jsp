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

<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="com.google.gson.Gson" %>
<%@ page import="org.wso2.carbon.identity.application.authentication.endpoint.util.AuthContextAPIClient" %>
<%@ page import="org.wso2.carbon.identity.application.authentication.endpoint.util.Constants" %>
<%@ page import="org.wso2.carbon.identity.core.util.IdentityCoreConstants" %>
<%@ page import="org.wso2.carbon.identity.core.util.IdentityUtil" %>
<%@ page import="org.wso2.carbon.base.ServerConfiguration" %>
<%@ page import="org.wso2.carbon.identity.captcha.util.CaptchaUtil" %>
<%@ page import="static org.wso2.carbon.identity.application.authentication.endpoint.util.Constants.STATUS" %>
<%@ page import="static org.wso2.carbon.identity.application.authentication.endpoint.util.Constants.STATUS_MSG" %>
<%@ page import="static org.wso2.carbon.identity.application.authentication.endpoint.util.Constants.CONFIGURATION_ERROR" %>
<%@ page import="static org.wso2.carbon.identity.application.authentication.endpoint.util.Constants.AUTHENTICATION_MECHANISM_NOT_CONFIGURED" %>
<%@ page import="static org.wso2.carbon.identity.application.authentication.endpoint.util.Constants.ENABLE_AUTHENTICATION_WITH_REST_API" %>
<%@ page import="static org.wso2.carbon.identity.application.authentication.endpoint.util.Constants.ERROR_WHILE_BUILDING_THE_ACCOUNT_RECOVERY_ENDPOINT_URL" %>
<%@ page import="org.wso2.carbon.identity.mgt.endpoint.util.client.IdentityProviderDataRetrievalClient" %>
<%@ page import="org.wso2.carbon.identity.mgt.endpoint.util.client.IdentityProviderDataRetrievalClientException" %>
<%@ page import="org.wso2.carbon.identity.mgt.endpoint.util.IdentityManagementEndpointConstants" %>
<%@ page import="java.io.File" %>
<%@ page import="java.util.ArrayList" %>
<%@ page import="java.util.Arrays" %>
<%@ page import="java.util.Map" %>
<%@ taglib prefix="layout" uri="org.wso2.identity.apps.taglibs.layout.controller" %>

<%@ include file="includes/localize.jsp" %>
<jsp:directive.include file="includes/init-url.jsp"/>
<jsp:directive.include file="includes/layout-resolver.jsp"/>

<%!
    private static final String FIDO_AUTHENTICATOR = "FIDOAuthenticator";
    private static final String MAGIC_LINK_AUTHENTICATOR = "MagicLinkAuthenticator";
    private static final String IWA_AUTHENTICATOR = "IwaNTLMAuthenticator";
    private static final String IS_SAAS_APP = "isSaaSApp";
    private static final String BASIC_AUTHENTICATOR = "BasicAuthenticator";
    private static final String IDENTIFIER_EXECUTOR = "IdentifierExecutor";
    private static final String OPEN_ID_AUTHENTICATOR = "OpenIDAuthenticator";
    private static final String JWT_BASIC_AUTHENTICATOR = "JWTBasicAuthenticator";
    private static final String X509_CERTIFICATE_AUTHENTICATOR = "x509CertificateAuthenticator";
    private String reCaptchaAPI = null;
    private String reCaptchaKey = null;
%>

<%
    request.getSession().invalidate();
    String queryString = request.getQueryString();
    Map<String, String> idpAuthenticatorMapping = null;
    if (request.getAttribute(Constants.IDP_AUTHENTICATOR_MAP) != null) {
        idpAuthenticatorMapping = (Map<String, String>) request.getAttribute(Constants.IDP_AUTHENTICATOR_MAP);
    }

    String errorMessage = "authentication.failed.please.retry";
    String errorCode = "";
    if(request.getParameter(Constants.ERROR_CODE)!=null){
        errorCode = request.getParameter(Constants.ERROR_CODE) ;
    }
    String loginFailed = "false";

    if (Boolean.parseBoolean(request.getParameter(Constants.AUTH_FAILURE))) {
        loginFailed = "true";
        String error = request.getParameter(Constants.AUTH_FAILURE_MSG);
        // Check the error is not null and whether there is a corresponding value in the resource bundle.
        if (!(StringUtils.isBlank(error)) &&
            !error.equalsIgnoreCase(AuthenticationEndpointUtil.i18n(resourceBundle, error))) {
                errorMessage = error;
        }
    }
%>
<%
    boolean hasLocalLoginOptions = false;
    boolean isBackChannelBasicAuth = false;
    List<String> localAuthenticatorNames = new ArrayList<String>();

    if (idpAuthenticatorMapping != null && idpAuthenticatorMapping.get(Constants.RESIDENT_IDP_RESERVED_NAME) != null) {
        String authList = idpAuthenticatorMapping.get(Constants.RESIDENT_IDP_RESERVED_NAME);
        if (authList != null) {
            localAuthenticatorNames = Arrays.asList(authList.split(","));
        }
    }

    String multiOptionURIParam = "";
    if (localAuthenticatorNames.size() > 1 || idpAuthenticatorMapping != null && idpAuthenticatorMapping.size() > 1) {
        String baseURL;
        try {
            baseURL = ServiceURLBuilder.create().addPath(request.getRequestURI()).build().getRelativePublicURL();
        } catch (URLBuilderException e) {
            request.setAttribute(STATUS, AuthenticationEndpointUtil.i18n(resourceBundle, "internal.error.occurred"));
            request.setAttribute(STATUS_MSG, AuthenticationEndpointUtil.i18n(resourceBundle, "error.when.processing.authentication.request"));
            request.getRequestDispatcher("error.do").forward(request, response);
            return;
        }

        String queryParamString = request.getQueryString() != null ? ("?" + request.getQueryString()) : "";
        multiOptionURIParam = "&multiOptionURI=" + Encode.forUriComponent(baseURL + queryParamString);
    }
%>
<%
    boolean reCaptchaEnabled = false;
    if (request.getParameter("reCaptcha") != null && Boolean.parseBoolean(request.getParameter("reCaptcha"))) {
        reCaptchaEnabled = true;
    }

    boolean reCaptchaResendEnabled = false;
    if (request.getParameter("reCaptchaResend") != null && Boolean.parseBoolean(request.getParameter("reCaptchaResend"))) {
        reCaptchaResendEnabled = true;
    }

    if (reCaptchaEnabled || reCaptchaResendEnabled) {
        reCaptchaKey = CaptchaUtil.reCaptchaSiteKey();
        reCaptchaAPI = CaptchaUtil.reCaptchaAPIURL();
    }
%>
<%
    String inputType = request.getParameter("inputType");
    String username = null;
    String usernameIdentifier = null;

    if (isIdentifierFirstLogin(inputType)) {
        String authAPIURL = application.getInitParameter(Constants.AUTHENTICATION_REST_ENDPOINT_URL);
        if (StringUtils.isBlank(authAPIURL)) {
            authAPIURL = IdentityUtil.getServerURL("/api/identity/auth/v1.1/", true, true);
        }
        if (!authAPIURL.endsWith("/")) {
            authAPIURL += "/";
        }
        authAPIURL += "context/" + request.getParameter("sessionDataKey");
        String contextProperties = AuthContextAPIClient.getContextProperties(authAPIURL);
        Gson gson = new Gson();
        Map<String, Object> parameters = gson.fromJson(contextProperties, Map.class);
        if (parameters != null) {
            username = (String) parameters.get("username");
            usernameIdentifier = (String) parameters.get("username");
        } else {
            String redirectURL = "error.do";
            response.sendRedirect(redirectURL);
            return;
        }
    }

    // Login context request url.
    String sessionDataKey = request.getParameter("sessionDataKey");
    String appName = request.getParameter("sp");
    String loginContextRequestUrl = logincontextURL + "?sessionDataKey=" + Encode.forUriComponent(sessionDataKey) + "&application="
            + Encode.forUriComponent(appName);
    if (!IdentityTenantUtil.isTenantQualifiedUrlsEnabled()) {
        // We need to send the tenant domain as a query param only in non tenant qualified URL mode.
        loginContextRequestUrl += "&tenantDomain=" + Encode.forUriComponent(tenantDomain);
    }

    String t = request.getParameter("t");
    String ut = request.getParameter("ut");
    if (StringUtils.isNotBlank(t)) {
        loginContextRequestUrl += "&t=" + t;
    }
    if (StringUtils.isNotBlank(ut)) {
        loginContextRequestUrl += "&ut=" + ut;
    }

    if (StringUtils.isNotBlank(usernameIdentifier)) {
        if (usernameIdentifier.split("@").length == 2) {
            usernameIdentifier = usernameIdentifier.split("@")[0];
        }

        if (usernameIdentifier.split("@").length > 2
            && !StringUtils.equals(usernameIdentifier.split("@")[1], IdentityManagementEndpointConstants.SUPER_TENANT)) {

            usernameIdentifier = usernameIdentifier.split("@")[0] + "@" + usernameIdentifier.split("@")[1];
        }
    }
%>

<%-- Data for the layout from the page --%>
<%
    layoutData.put("containerSize", "medium");
%>

<!doctype html>
<html>
<head>
    <!-- header -->
    <%
        File headerFile = new File(getServletContext().getRealPath("extensions/header.jsp"));
        if (headerFile.exists()) {
    %>
        <jsp:include page="extensions/header.jsp"/>
    <% } else { %>
        <jsp:include page="includes/header.jsp"/>
    <% } %>

    <%
        if (reCaptchaEnabled || reCaptchaResendEnabled) {
    %>
    <script src="<%=Encode.forHtmlContent(reCaptchaAPI)%>"></script>
    <%
        }
    %>
</head>
<body class="login-portal layout authentication-portal-layout" onload="checkSessionKey()">

    <% request.setAttribute("pageName", "sign-in"); %>
    <% if (new File(getServletContext().getRealPath("extensions/timeout.jsp")).exists()) { %>
        <jsp:include page="extensions/timeout.jsp"/>
    <% } else { %>
        <jsp:include page="util/timeout.jsp"/>
    <% } %>

    <layout:main layoutName="<%= layout %>" layoutFileRelativePath="<%= layoutFileRelativePath %>" data="<%= layoutData %>" >
        <layout:component componentName="ProductHeader" >
            <!-- product-title -->
            <%
                File productTitleFile = new File(getServletContext().getRealPath("extensions/product-title.jsp"));
                if (productTitleFile.exists()) {
            %>
                <jsp:include page="extensions/product-title.jsp"/>
            <% } else { %>
                <jsp:include page="includes/product-title.jsp"/>
            <% } %>
        </layout:component>
        <layout:component componentName="MainSection" >
            <div class="ui segment">
                <h3 class="ui header ellipsis">
                    <% if (isIdentifierFirstLogin(inputType)) { %>
                        <div class="display-inline"><%=AuthenticationEndpointUtil.i18n(resourceBundle, "welcome") + " "%></div>
                        <div id="user-name-label" class="display-inline" data-position="top left" data-variation="inverted" data-content="<%=usernameIdentifier%>"><%=usernameIdentifier%></div>
                    <% } else { %>
                        <%=AuthenticationEndpointUtil.i18n(resourceBundle, "login")%>
                    <% } %>
                </h3>

                <div class="segment-form">
                    <%
                        if (localAuthenticatorNames.size() > 0) {
                            if (localAuthenticatorNames.contains(OPEN_ID_AUTHENTICATOR)) {
                                hasLocalLoginOptions = true;
                    %>
                        <%@ include file="openid.jsp" %>
                    <%
                        } else if (localAuthenticatorNames.contains(IDENTIFIER_EXECUTOR)) {
                            hasLocalLoginOptions = true;
                    %>
                        <%@ include file="identifierauth.jsp" %>
                    <%
                        } else if (localAuthenticatorNames.contains(JWT_BASIC_AUTHENTICATOR) ||
                            localAuthenticatorNames.contains(BASIC_AUTHENTICATOR)) {
                            hasLocalLoginOptions = true;
                            boolean includeBasicAuth = true;
                            if (localAuthenticatorNames.contains(JWT_BASIC_AUTHENTICATOR)) {
                                if (Boolean.parseBoolean(application.getInitParameter(ENABLE_AUTHENTICATION_WITH_REST_API))) {
                                    isBackChannelBasicAuth = true;
                                } else {
                                    String redirectURL = "error.do?" + STATUS + "=" + CONFIGURATION_ERROR + "&" +
                                            STATUS_MSG + "=" + AUTHENTICATION_MECHANISM_NOT_CONFIGURED;
                                    response.sendRedirect(redirectURL);
                                    return;
                                }
                            } else if (localAuthenticatorNames.contains(BASIC_AUTHENTICATOR)) {
                                isBackChannelBasicAuth = false;
                            if (TenantDataManager.isTenantListEnabled() && Boolean.parseBoolean(request.getParameter(IS_SAAS_APP))) {
                                includeBasicAuth = false;
                    %>
                                <%@ include file="tenantauth.jsp" %>
                    <%
                            }
                        }

                                if (includeBasicAuth) {
                                    %>
                                        <%@ include file="basicauth.jsp" %>
                                    <%
                                }
                            }
                        }
                    %>
                    <%if (idpAuthenticatorMapping != null &&
                            idpAuthenticatorMapping.get(Constants.RESIDENT_IDP_RESERVED_NAME) != null) { %>

                    <%} %>
                    <%
                        if ((hasLocalLoginOptions && localAuthenticatorNames.size() > 1) || (!hasLocalLoginOptions)
                                || (hasLocalLoginOptions && idpAuthenticatorMapping != null && idpAuthenticatorMapping.size() > 1)) {
                    %>
                    <% if (localAuthenticatorNames.contains(BASIC_AUTHENTICATOR) ||
                            localAuthenticatorNames.contains(IDENTIFIER_EXECUTOR)) { %>
                    <div class="ui divider hidden"></div>
                    <div class="ui horizontal divider">
                        Or
                    </div>
                    <% } %>
                    <div class="field">
                        <div class="ui vertical ui center aligned segment form">
                            <%
                                int iconId = 0;
                                if (idpAuthenticatorMapping != null) {
                                for (Map.Entry<String, String> idpEntry : idpAuthenticatorMapping.entrySet()) {
                                    iconId++;
                                    if (!idpEntry.getKey().equals(Constants.RESIDENT_IDP_RESERVED_NAME)) {
                                        String idpName = idpEntry.getKey();
                                        boolean isHubIdp = false;
                                        if (idpName.endsWith(".hub")) {
                                            isHubIdp = true;
                                            idpName = idpName.substring(0, idpName.length() - 4);
                                        }

                                        // Uses the `IdentityProviderDataRetrievalClient` to get the IDP image.
                                        String imageURL = "libs/themes/default/assets/images/identity-providers/enterprise-idp-illustration.svg";

                                        try {
                                            IdentityProviderDataRetrievalClient identityProviderDataRetrievalClient = new IdentityProviderDataRetrievalClient();
                                            imageURL = identityProviderDataRetrievalClient.getIdPImage(tenantDomain, idpName);
                                        } catch (IdentityProviderDataRetrievalClientException e) {
                                            // Exception is ignored and the default `imageURL` value will be used as a fallback.
                                        }
                            %>
                                <% if (isHubIdp) { %>
                                    <div class="field">
                                        <button class="ui labeled icon button fluid isHubIdpPopupButton" id="icon-<%=iconId%>">
                                            <%=AuthenticationEndpointUtil.i18n(resourceBundle, "sign.in.with")%> <strong><%=Encode.forHtmlContent(idpName)%></strong>
                                        </button>
                                        <div class="ui flowing popup transition hidden isHubIdpPopup">
                                            <h5 class="font-large"><%=AuthenticationEndpointUtil.i18n(resourceBundle,"sign.in.with")%>
                                                <%=Encode.forHtmlContent(idpName)%></h5>
                                            <div class="content">
                                                <form class="ui form">
                                                    <div class="field">
                                                        <input id="domainName" class="form-control" type="text"
                                                            placeholder="<%=AuthenticationEndpointUtil.i18n(resourceBundle, "domain.name")%>">
                                                    </div>
                                                    <input type="button" class="ui button primary"
                                                        onClick="javascript: myFunction('<%=idpName%>','<%=idpEntry.getValue()%>','domainName')"
                                                        value="<%=AuthenticationEndpointUtil.i18n(resourceBundle,"go")%>"/>
                                                </form>
                                            </div>
                                        </div>
                                    </div>
                                    <br>
                                <% } else { %>
                                    <div class="external-login blurring external-login-dimmer">
                                        <div class="field">
                                            <button
                                                class="ui button fluid"
                                                onclick="handleNoDomain(this,
                                                    '<%=Encode.forJavaScriptAttribute(Encode.forUriComponent(idpName))%>',
                                                    '<%=Encode.forJavaScriptAttribute(Encode.forUriComponent(idpEntry.getValue()))%>')"
                                                id="icon-<%=iconId%>"
                                                title="<%=AuthenticationEndpointUtil.i18n(resourceBundle, "sign.in.with")%> <%=Encode.forHtmlAttribute(idpName)%>"
                                            >
                                                <img class="ui image" src="<%=Encode.forHtmlAttribute(imageURL)%>">
                                                <span><%=AuthenticationEndpointUtil.i18n(resourceBundle, "sign.in.with")%> <%=Encode.forHtmlContent(idpName)%></span>
                                            </button>
                                        </div>
                                    </div>
                                    <br>
                                <% } %>
                            <% } else if (localAuthenticatorNames.size() > 0) {
                                if (localAuthenticatorNames.contains(IWA_AUTHENTICATOR)) {
                            %>
                            <div class="field">
                                <button class="ui blue labeled icon button fluid"
                                    onclick="handleNoDomain(this,
                                        '<%=Encode.forJavaScriptAttribute(Encode.forUriComponent(idpEntry.getKey()))%>',
                                        'IWAAuthenticator')"
                                    id="icon-<%=iconId%>"
                                    title="<%=AuthenticationEndpointUtil.i18n(resourceBundle, "sign.in.with")%> IWA">
                                    <%=AuthenticationEndpointUtil.i18n(resourceBundle, "sign.in.with")%> <strong>IWA</strong>
                                </button>
                            </div>
                            <%
                                }
                                if (localAuthenticatorNames.contains(X509_CERTIFICATE_AUTHENTICATOR)) {
                            %>
                            <div class="field">
                                <button class="ui grey labeled icon button fluid"
                                    onclick="handleNoDomain(this,
                                        '<%=Encode.forJavaScriptAttribute(Encode.forUriComponent(idpEntry.getKey()))%>',
                                        'x509CertificateAuthenticator')"
                                    id="icon-<%=iconId%>"
                                    title="<%=AuthenticationEndpointUtil.i18n(resourceBundle, "sign.in.with")%> X509 Certificate">
                                    <i class="certificate icon"></i>
                                    <%=AuthenticationEndpointUtil.i18n(resourceBundle, "sign.in.with")%> <strong>x509 Certificate</strong>
                                </button>
                            </div>
                            <%
                                }
                                if (localAuthenticatorNames.contains(FIDO_AUTHENTICATOR)) {
                            %>
                            <div class="field">
                                <button class="ui grey labeled icon button fluid"
                                    onclick="handleNoDomain(this,
                                        '<%=Encode.forJavaScriptAttribute(Encode.forUriComponent(idpEntry.getKey()))%>',
                                        'FIDOAuthenticator')"
                                    id="icon-<%=iconId%>"
                                    title="<%=AuthenticationEndpointUtil.i18n(resourceBundle, "sign.in.with")%>
                                    <%=AuthenticationEndpointUtil.i18n(resourceBundle, "sign.in.with" )%>">
                                    <i class="usb icon"></i>
                                    <img src="libs/themes/default/assets/images/icons/fingerprint.svg" alt="Fido Logo" />
                                    <span>
                                        <%=AuthenticationEndpointUtil.i18n(resourceBundle, "sign.in.with" )%>
                                        <%=AuthenticationEndpointUtil.i18n(resourceBundle, "fido.authenticator" )%>
                                    </span>
                                </button>
                            </div>
                            <%
                                }
                                if (localAuthenticatorNames.contains(MAGIC_LINK_AUTHENTICATOR)) {
                            %>
                            <div class="social-login blurring social-dimmer">
                                <div class="field">
                                    <button class="ui button" onclick="handleNoDomain(this,
                                        '<%=Encode.forJavaScriptAttribute(Encode.forUriComponent(idpEntry.getKey()))%>',
                                        '<%=MAGIC_LINK_AUTHENTICATOR%>')" id="icon-<%=iconId%>"
                                        title="<%=AuthenticationEndpointUtil.i18n(resourceBundle, "sign.in.with")%>
                                            <%=AuthenticationEndpointUtil.i18n(resourceBundle, "magic.link" )%>"
                                        data-componentid="login-page-sign-in-with-magic-link">
                                        <img class="ui image" src="libs/themes/default/assets/images/icons/magic-link-icon.svg" alt="Magic Link Logo" />
                                        <span>
                                            <%=AuthenticationEndpointUtil.i18n(resourceBundle, "sign.in.with" )%>
                                            <%=AuthenticationEndpointUtil.i18n(resourceBundle, "magic.link" )%>
                                        </span>
                                    </button>
                                </div>
                            </div>
                            <%
                                }
                                if (localAuthenticatorNames.contains("totp")) {
                            %>
                            <div class="field">
                                <button class="ui brown labeled icon button fluid"
                                    onclick="handleNoDomain(this,
                                        '<%=Encode.forJavaScriptAttribute(Encode.forUriComponent(idpEntry.getKey()))%>',
                                        'totp')"
                                    id="icon-<%=iconId%>"
                                    title="<%=AuthenticationEndpointUtil.i18n(resourceBundle, "sign.in.with")%> TOTP">
                                    <i class="key icon"></i> <%=AuthenticationEndpointUtil.i18n(resourceBundle, "sign.in.with")%> <strong>TOTP</strong>
                                </button>
                            </div>
                            <%
                            }
                            }
                            }
                            } %>
                            </div>
                        </div>
                    <% } %>
                </div>
            </div>
        </layout:component>
        <layout:component componentName="ProductFooter" >
            <!-- product-footer -->
            <%
                File productFooterFile = new File(getServletContext().getRealPath("extensions/product-footer.jsp"));
                if (productFooterFile.exists()) {
            %>
                <jsp:include page="extensions/product-footer.jsp"/>
            <% } else { %>
                <jsp:include page="includes/product-footer.jsp"/>
            <% } %>
        </layout:component>
    </layout:main>

    <!-- footer -->
    <%
        File footerFile = new File(getServletContext().getRealPath("extensions/footer.jsp"));
        if (footerFile.exists()) {
    %>
        <jsp:include page="extensions/footer.jsp"/>
    <% } else { %>
        <jsp:include page="includes/footer.jsp"/>
    <% } %>

    <%
        String contextPath =
                ServerConfiguration.getInstance().getFirstProperty(IdentityCoreConstants.PROXY_CONTEXT_PATH);
        if (contextPath != null && contextPath != "") {
            if (contextPath.trim().charAt(0) != '/') {
                contextPath = "/" + contextPath;
            }
            if (contextPath.trim().charAt(contextPath.length() - 1) == '/') {
                contextPath = contextPath.substring(0, contextPath.length() - 1);
            }
            contextPath = contextPath.trim();
        } else {
            contextPath = "";
        }
    %>
    <script>
        function checkSessionKey() {
            var proxyPath = "<%=contextPath%>"
            $.ajax({
                type: "GET",
                url: proxyPath + "<%=loginContextRequestUrl%>",
                xhrFields: { withCredentials: true },
                success: function (data) {
                    if (data && data.status == 'redirect' && data.redirectUrl && data.redirectUrl.length > 0) {
                        window.location.href = data.redirectUrl;
                    }
                },
                cache: false
            });
        }

        function getParameterByName(name, url) {
            if (!url) {
                url = window.location.href;
            }
            name = name.replace(/[\[\]]/g, '\\$&');
            var regex = new RegExp('[?&]' + name + '(=([^&#]*)|&|#|$)'),
            results = regex.exec(url);
            if (!results) return null;
            if (!results[2]) return "";
            return decodeURIComponent(results[2].replace(/\+/g, ' '));
        }

        $(document).ready(function () {
            $('#user-name-label').popup({
                lastResort: 'top left'
            });
            $('.main-link').click(function () {
                $('.main-link').next().hide();
                $(this).next().toggle('fast');
                var w = $(document).width();
                var h = $(document).height();
                $('.overlay').css("width", w + "px").css("height", h + "px").show();
            });

            $('.overlay').click(function () {
                $(this).hide();
                $('.main-link').next().hide();
            });
        });

        function myFunction(key, value, name) {
            var object = document.getElementById(name);
            var domain = object.value;


            if (domain != "") {
                document.location = "<%=commonauthURL%>?idp=" + key + "&authenticator=" + value +
                        "&sessionDataKey=<%=Encode.forUriComponent(request.getParameter("sessionDataKey"))%>&domain=" +
                        domain;
            } else {
                document.location = "<%=commonauthURL%>?idp=" + key + "&authenticator=" + value +
                        "&sessionDataKey=<%=Encode.forUriComponent(request.getParameter("sessionDataKey"))%>";
            }
        }

        function handleNoDomain(elem, key, value) {
            var linkClicked = "link-clicked";
            if ($(elem).hasClass(linkClicked)) {
                console.warn("Preventing multi click.")
            } else {
                $(elem).addClass(linkClicked);
                document.location = "<%=commonauthURL%>?idp=" + key + "&authenticator=" + value +
                    "&sessionDataKey=<%=Encode.forUriComponent(request.getParameter("sessionDataKey"))%>" +
                    "<%=multiOptionURIParam%>";
            }
        }

        window.onunload = function(){};

        function changeUsername (e) {
            document.getElementById("changeUserForm").submit();
        }

        $('.isHubIdpPopupButton').popup({
            popup: '.isHubIdpPopup',
            on: 'click',
            position: 'top left',
            delay: {
                show: 300,
                hide: 800
            }
        });
    </script>

    <%!
        private boolean isIdentifierFirstLogin(String inputType) {
            return "idf".equalsIgnoreCase(inputType);
        }
    %>
</body>
</html>
