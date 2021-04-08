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
<%@ page import="static org.wso2.carbon.identity.application.authentication.endpoint.util.Constants.STATUS" %>
<%@ page import="static org.wso2.carbon.identity.application.authentication.endpoint.util.Constants.STATUS_MSG" %>
<%@ page import="static org.wso2.carbon.identity.application.authentication.endpoint.util.Constants.CONFIGURATION_ERROR" %>
<%@ page import="static org.wso2.carbon.identity.application.authentication.endpoint.util.Constants.AUTHENTICATION_MECHANISM_NOT_CONFIGURED" %>
<%@ page import="static org.wso2.carbon.identity.application.authentication.endpoint.util.Constants.ENABLE_AUTHENTICATION_WITH_REST_API" %>
<%@ page import="static org.wso2.carbon.identity.application.authentication.endpoint.util.Constants.ERROR_WHILE_BUILDING_THE_ACCOUNT_RECOVERY_ENDPOINT_URL" %>
<%@ page import="java.io.File" %>
<%@ page import="java.util.ArrayList" %>
<%@ page import="java.util.Arrays" %>
<%@ page import="java.util.Map" %>

<%@ include file="includes/localize.jsp" %>
<jsp:directive.include file="includes/init-url.jsp"/>

<%!
    private static final String FIDO_AUTHENTICATOR = "FIDOAuthenticator";
    private static final String IWA_AUTHENTICATOR = "IwaNTLMAuthenticator";
    private static final String IS_SAAS_APP = "isSaaSApp";
    private static final String BASIC_AUTHENTICATOR = "BasicAuthenticator";
    private static final String IDENTIFIER_EXECUTOR = "IdentifierExecutor";
    private static final String OPEN_ID_AUTHENTICATOR = "OpenIDAuthenticator";
    private static final String JWT_BASIC_AUTHENTICATOR = "JWTBasicAuthenticator";
    private static final String X509_CERTIFICATE_AUTHENTICATOR = "x509CertificateAuthenticator";
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
        if (error != null && !error.isEmpty()) {
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
%>
<%
    String inputType = request.getParameter("inputType");
    String username = null;

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
        } else {
            String redirectURL = "error.do";
            response.sendRedirect(redirectURL);
        }
    }

    // Login context request url.
    String sessionDataKey = request.getParameter("sessionDataKey");
    String relyingParty = request.getParameter("relyingParty");
    String loginContextRequestUrl = logincontextURL + "?sessionDataKey=" + sessionDataKey + "&relyingParty="
            + relyingParty;
    if (!IdentityTenantUtil.isTenantQualifiedUrlsEnabled()) {
        // We need to send the tenant domain as a query param only in non tenant qualified URL mode.
        loginContextRequestUrl += "&tenantDomain=" + tenantDomain;
    }
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
        <script src='<%=(Encode.forJavaScriptSource(request.getParameter("reCaptchaAPI")))%>'></script>
    <%
        }
    %>
</head>
<body class="login-portal layout authentication-portal-layout" onload="checkSessionKey()">
    <main class="center-segment">
        <div class="ui container medium center aligned middle aligned">

            <!-- product-title -->
            <%
                File productTitleFile = new File(getServletContext().getRealPath("extensions/product-title.jsp"));
                if (productTitleFile.exists()) {
            %>
                <jsp:include page="extensions/product-title.jsp"/>
            <% } else { %>
                <jsp:include page="includes/product-title.jsp"/>
            <% } %>

            <div class="ui segment">
                <h3 class="ui header">
                    <% if (isIdentifierFirstLogin(inputType)) { %>
                        <%=AuthenticationEndpointUtil.i18n(resourceBundle, "welcome") + " " + username%>
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
                        <div class="ui vertical ui center aligned segment form" style="max-width: 300px; margin: 0 auto;">
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
                                <% } else { %>
                                    <div class="field">
                                        <button class="ui icon button fluid"
                                            onclick="handleNoDomain(this,
                                                '<%=Encode.forJavaScriptAttribute(Encode.forUriComponent(idpName))%>',
                                                '<%=Encode.forJavaScriptAttribute(Encode.forUriComponent(idpEntry.getValue()))%>')"
                                            id="icon-<%=iconId%>"
                                            title="<%=AuthenticationEndpointUtil.i18n(resourceBundle, "sign.in.with")%> <%=Encode.forHtmlAttribute(idpName)%>">
                                            <%=AuthenticationEndpointUtil.i18n(resourceBundle, "sign.in.with")%> <strong><%=Encode.forHtmlContent(idpName)%></strong>
                                        </button>
                                    </div>
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
                                <button class="ui grey basic labeled icon button fluid"
                                    onclick="handleNoDomain(this,
                                        '<%=Encode.forJavaScriptAttribute(Encode.forUriComponent(idpEntry.getKey()))%>',
                                        'FIDOAuthenticator')"
                                    id="icon-<%=iconId%>"
                                    title="<%=AuthenticationEndpointUtil.i18n(resourceBundle, "sign.in.with")%> FIDO">
                                    <i class="usb icon"></i>
                                    <img src="libs/themes/default/assets/images/icons/fido-logo.png" height="13px" /> Key
                                </button>
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
        </div>
    </main>

    <!-- product-footer -->
    <%
        File productFooterFile = new File(getServletContext().getRealPath("extensions/product-footer.jsp"));
        if (productFooterFile.exists()) {
    %>
        <jsp:include page="extensions/product-footer.jsp"/>
    <% } else { %>
        <jsp:include page="includes/product-footer.jsp"/>
    <% } %>

    <!-- footer -->
    <%
        File footerFile = new File(getServletContext().getRealPath("extensions/footer.jsp"));
        if (footerFile.exists()) {
    %>
        <jsp:include page="extensions/footer.jsp"/>
    <% } else { %>
        <jsp:include page="includes/footer.jsp"/>
    <% } %>

    <script>
        function checkSessionKey() {
            $.ajax({
                type: "GET",
                url: "<%=loginContextRequestUrl%>",
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

            <%
                if(reCaptchaEnabled) {
            %>
                var error_msg = $("#error-msg");

                $("#loginForm").submit(function (e) {
                    var resp = $("[name='g-recaptcha-response']")[0].value;
                    if (resp.trim() == '') {
                        error_msg.text("<%=AuthenticationEndpointUtil.i18n(resourceBundle,"please.select.recaptcha")%>");
                        error_msg.show();
                        $("html, body").animate({scrollTop: error_msg.offset().top}, 'slow');
                        return false;
                    }
                    return true;
                });
            <%
                }
            %>
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
                <%
                String multiOptionURIParam = "";
                if (localAuthenticatorNames.size() > 1 || idpAuthenticatorMapping != null && idpAuthenticatorMapping.size() > 1) {
                    multiOptionURIParam = "&multiOptionURI=" + Encode.forUriComponent(request.getRequestURI() +
                        (request.getQueryString() != null ? "?" + request.getQueryString() : ""));
                }
                %>
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
