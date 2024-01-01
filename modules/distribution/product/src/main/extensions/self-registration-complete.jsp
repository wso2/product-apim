<%--
  ~ Copyright (c) 2016-2023, WSO2 LLC. (https://www.wso2.com).
  ~
  ~ WSO2 LLC. licenses this file to you under the Apache License,
  ~ Version 2.0 (the "License"); you may not use this file except
  ~ in compliance with the License.
  ~ You may obtain a copy of the License at
  ~
  ~    http://www.apache.org/licenses/LICENSE-2.0
  ~
  ~ Unless required by applicable law or agreed to in writing,
  ~ software distributed under the License is distributed on an
  ~ "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
  ~ KIND, either express or implied.  See the License for the
  ~ specific language governing permissions and limitations
  ~ under the License.
--%>

<%@ page contentType="text/html;charset=UTF-8" language="java" %>

<%@ page import="org.apache.commons.lang.StringUtils" %>
<%@ page import="org.wso2.carbon.identity.mgt.endpoint.util.IdentityManagementEndpointConstants" %>
<%@ page import="org.wso2.carbon.identity.mgt.endpoint.util.IdentityManagementEndpointUtil" %>
<%@ page import="org.wso2.carbon.utils.multitenancy.MultitenantUtils" %>
<%@ page import="org.wso2.carbon.identity.application.authentication.endpoint.util.EndpointConfigManager" %>
<%@ page import="org.apache.cxf.jaxrs.client.JAXRSClientFactory" %>
<%@ page import="org.apache.cxf.jaxrs.client.WebClient" %>
<%@ page import="org.apache.commons.codec.binary.Base64" %>
<%@ page import="org.wso2.carbon.identity.application.authentication.endpoint.util.client.SelfUserRegistrationResource" %>
<%@ page import="org.wso2.carbon.identity.application.authentication.endpoint.util.AuthenticationEndpointUtil" %>
<%@ page import="org.wso2.carbon.identity.application.authentication.endpoint.util.bean.ResendCodeRequestDTO" %>
<%@ page import="org.wso2.carbon.identity.application.authentication.endpoint.util.bean.UserDTO" %>
<%@ page import="java.net.MalformedURLException" %>
<%@ page import="org.apache.cxf.jaxrs.provider.json.JSONProvider" %>
<%@ page import="javax.ws.rs.core.Response" %>
<%@ page import="java.nio.charset.Charset" %>
<%@ page import="java.io.File" %>
<%@ page import="org.owasp.encoder.Encode" %>
<%@ page import="org.apache.http.HttpStatus" %>
<%@ page import="org.wso2.carbon.base.ServerConfiguration" %>

<%@ page import="java.net.URL" %>
<%@ page import="java.net.URLEncoder" %>
<%@ page import="java.net.URLDecoder" %>
<%@ page import="static org.wso2.carbon.identity.core.util.IdentityUtil.isSelfSignUpEPAvailable" %>
<%@ page import="static org.wso2.carbon.identity.core.util.IdentityUtil.isRecoveryEPAvailable" %>
<%@ page import="static org.wso2.carbon.identity.core.util.IdentityUtil.isEmailUsernameEnabled" %>
<%@ page import="static org.wso2.carbon.identity.core.util.IdentityUtil.getServerURL" %>
<%@ page import="org.apache.commons.text.StringEscapeUtils" %>
<%@ page import="org.wso2.carbon.identity.core.URLBuilderException" %>
<%@ page import="org.wso2.carbon.identity.core.ServiceURLBuilder" %>
<%@ page import="org.wso2.carbon.identity.mgt.endpoint.util.client.PreferenceRetrievalClient" %>
<%@ page import="org.wso2.carbon.identity.mgt.endpoint.util.client.PreferenceRetrievalClientException" %>
<%@ page import="org.wso2.carbon.identity.mgt.endpoint.util.client.ApplicationDataRetrievalClient" %>
<%@ page import="org.wso2.carbon.identity.mgt.endpoint.util.client.ApplicationDataRetrievalClientException" %>
<%@ page import="org.wso2.carbon.identity.core.util.IdentityCoreConstants" %>
<%@ page import="org.wso2.carbon.identity.core.util.IdentityUtil" %>
<%@ page import="java.util.ArrayList" %>
<%@ page import="java.util.Arrays" %>
<%@ page import="org.wso2.carbon.identity.application.authentication.endpoint.util.Constants" %>
<%@ page import="java.util.Collections" %>
<%@ page import="org.wso2.carbon.user.core.util.UserCoreUtil" %>
<%@ taglib prefix="layout" uri="org.wso2.identity.apps.taglibs.layout.controller" %>

<%-- Localization --%>
<jsp:directive.include file="includes/localize.jsp"/>

<%-- Include tenant context --%>
<jsp:directive.include file="tenant-resolve.jsp"/>

<%-- Branding Preferences --%>
<jsp:directive.include file="includes/branding-preferences.jsp"/>

<%
    final String TENANT_DOMAIN = "tenant-domain";

    String username = IdentityManagementEndpointUtil.getStringValue(request.getAttribute("username"));
    String tenantAwareUsername = Encode.forHtml(MultitenantUtils.getTenantAwareUsername(UserCoreUtil.removeDomainFromName(username)));
    boolean isEmailNotificationEnabled = false;
    String callback = (String) request.getAttribute("callback");
    String confirm = (String) request.getAttribute("confirm");
    String confirmLiteReg = (String) request.getAttribute("confirmLiteReg");
    String resendUsername = request.getParameter("username");
    String sp = request.getParameter("sp");
    String spId = request.getParameter("spId");
    String sessionDataKey = (String) request.getAttribute("sessionDataKey");
    String applicationAccessURLWithoutEncoding = null;
    String tenantedMyaccountURL = null;
    boolean showBackButton = false;
    boolean accountLockOnCreationEnabled = true;
    boolean accountVerification = false;
    Boolean autoLoginEnabled = false;
    String emailValue = request.getParameter("http://wso2.org/claims/emailaddress");

    /**
    * For SaaS application read from user tenant from parameters.
    */
    String srtenantDomain =  IdentityManagementEndpointUtil.getStringValue(request.getAttribute("srtenantDomain"));
    if (StringUtils.isNotBlank(srtenantDomain)) {
       tenantDomain = srtenantDomain;
    }

    /**
    * For the tenant should get if it has enabled lock on account creation.
    */
    if (StringUtils.isNotBlank(tenantDomain)) {
        try {
            PreferenceRetrievalClient preferenceRetrievalClient = new PreferenceRetrievalClient();
            accountLockOnCreationEnabled = preferenceRetrievalClient.checkSelfRegistrationLockOnCreation(tenantDomain);
            accountVerification = preferenceRetrievalClient.checkSelfRegistrationSendConfirmationOnCreation(tenantDomain);
            autoLoginEnabled = preferenceRetrievalClient.checkAutoLoginAfterSelfRegistrationEnabled(tenantDomain);

        } catch (PreferenceRetrievalClientException e) {
            accountLockOnCreationEnabled = true;
        }
    }

    /**
    * Make sure username contains tenant && tenantAwareUsername does not remove email domain.
    */
    String usernameWithTenant = IdentityManagementEndpointUtil.getFullQualifiedUsername(username,tenantDomain, null);
    tenantAwareUsername = Encode.forHtml(MultitenantUtils.getTenantAwareUsername(UserCoreUtil.removeDomainFromName(usernameWithTenant)));

    if (StringUtils.isNotBlank(spId)) {
        try {
            ApplicationDataRetrievalClient applicationDataRetrieval = new ApplicationDataRetrievalClient();
            if (spId.equals("My_Account")) {
                sp = "My Account";
            } else {
                sp = applicationDataRetrieval.getApplicationName(tenantDomain,spId);
            }
        } catch (Exception e) {
            // Ignored and fallback to my account page url.
            showBackButton = false;
        }
    }

    if (StringUtils.isNotBlank(sp) && StringUtils.isNotBlank(tenantDomain)) {
        try {
                // Retrieve application access url to redirect user back to the application.
                ApplicationDataRetrievalClient applicationDataRetrievalClient = new ApplicationDataRetrievalClient();
                applicationAccessURLWithoutEncoding = applicationDataRetrievalClient.getApplicationAccessURL(tenantDomain,sp);
                showBackButton = true;
            } catch (Exception e) {
                 // Ignored and fallback to my account page url.
                showBackButton = false;
        }
    } else {
        showBackButton = false;
    }

    if (StringUtils.isBlank(callback)) {
        callback = IdentityManagementEndpointUtil.getUserPortalUrl(
                application.getInitParameter(IdentityManagementEndpointConstants.ConfigConstants.USER_PORTAL_URL), tenantDomain);
    }

    // Set my account URL.
    if (StringUtils.isBlank(tenantedMyaccountURL) && StringUtils.isNotBlank(tenantDomain)) {
        tenantedMyaccountURL = IdentityManagementEndpointUtil.getUserPortalUrl(
                application.getInitParameter(IdentityManagementEndpointConstants.ConfigConstants.USER_PORTAL_URL), tenantDomain);
    }

    isEmailNotificationEnabled = Boolean.parseBoolean(application.getInitParameter(
            IdentityManagementEndpointConstants.ConfigConstants.ENABLE_EMAIL_NOTIFICATION));

    String resendEmail = request.getParameter("resend_email");
    boolean resendSuccess = false;

    if(resendEmail != null) {
        ResendCodeRequestDTO selfRegistrationRequest = new ResendCodeRequestDTO();
        UserDTO userDTO = AuthenticationEndpointUtil.getUser(resendUsername);
        selfRegistrationRequest.setUser(userDTO);

        String path = config.getServletContext().getInitParameter(Constants.ACCOUNT_RECOVERY_REST_ENDPOINT_URL);
        String proxyContextPath = ServerConfiguration.getInstance().getFirstProperty(IdentityCoreConstants
                .PROXY_CONTEXT_PATH);
        if (proxyContextPath == null) {
            proxyContextPath = "";
        }
        String url;
        if (StringUtils.isNotBlank(EndpointConfigManager.getServerOrigin())) {
            url = EndpointConfigManager.getServerOrigin() + proxyContextPath + path;
        } else {
            url = IdentityUtil.getServerURL(path, true, false);
        }
        url = url.replace(TENANT_DOMAIN, userDTO.getTenantDomain());

        ArrayList<JSONProvider> providers = new ArrayList<JSONProvider>();
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
        if (selfRegistrationResponse != null && selfRegistrationResponse.getStatus() == HttpStatus.SC_CREATED) {
            resendSuccess = true;
        } else {
            request.setAttribute("errorMsg", "Unable to resend email verification link.");
            if (!StringUtils.isBlank(username)) {
                request.setAttribute("username", username);
            }
            request.getRequestDispatcher("error.jsp").forward(request, response);
        }
    }
%>

<%-- Data for the layout from the page --%>
<%
    layoutData.put("isResponsePage", true);
    layoutData.put("isSuccessResponse", true);
    layoutData.put("isSelfRegistrationCompletePage", true);
%>

<!doctype html>
<html lang="en-US">
<head>
    <%
        File headerFile = new File(getServletContext().getRealPath("extensions/header.jsp"));
        if (headerFile.exists()) {
    %>
    <jsp:include page="extensions/header.jsp"/>
    <% } else { %>
    <jsp:include page="includes/header.jsp"/>
    <% } %>
</head>
<body class="login-portal layout">
    <script>
        function redirect(redirectURL) {
            var url = redirectURL;
            window.location.href = url;
        }

        // Mask Email
        function maskEmail(email) {
            email = email.split('');
            let finalArr=[];
            let len = email.indexOf('@');
            email.forEach((item,pos)=> {
                (pos>=1 && pos<=len-2) ? finalArr.push('*') : finalArr.push(email[pos]);
            })
            document.getElementById("maskedEmail").innerHTML = finalArr.join('');
        }

        // UI countdown
        function countdown(redirectURL) {
            var timeleft = 3;
            var downloadTimer = setInterval(function() {
                if(timeleft <= 0){
                    clearInterval(downloadTimer);
                    document.getElementById("countdown").innerHTML = "0";
                    redirect(redirectURL);
                } else {
                    document.getElementById("countdown").innerHTML = timeleft;
                }
                timeleft -= 1;
            }, 1000);
        }
    </script>

    <%
        if (StringUtils.isNotBlank(confirmLiteReg) && confirmLiteReg.equals("true")) {
            response.sendRedirect(callback);
        } else {
    %>
        <layout:main layoutName="<%= layout %>" layoutFileRelativePath="<%= layoutFileRelativePath %>" data="<%= layoutData %>" >
            <layout:component componentName="ProductHeader" >
            <%-- product-title --%>
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
                <div class="ui green segment mt-3 attached">
                        <h3 class="ui header text-center slogan-message mt-4 mb-6" data-testid="self-register-complete-page-header">
                            <% if (StringUtils.isNotBlank(confirm)) { %>
                            <%=IdentityManagementEndpointUtil.i18n(recoveryResourceBundle, "account.verified.successfully")%>
                            <% } else if (accountLockOnCreationEnabled) { %>
                            <%=IdentityManagementEndpointUtil.i18n(recoveryResourceBundle, "you.are.almost.there")%>
                            <% } else {%>
                            <%=IdentityManagementEndpointUtil.i18n(recoveryResourceBundle, "account.created.successfully")%>
                            <% } %>
                        </h3>

                        <p class="portal-tagline-description">
                            <%
                            String url = "";
                            if (StringUtils.isNotBlank(confirm)) { %>
                                <%=IdentityManagementEndpointUtil.i18n(recoveryResourceBundle, "your.account.with.username")%>
                                <b><%=resendUsername%></b>
                                <%=IdentityManagementEndpointUtil.i18n(recoveryResourceBundle, "has.been.verified.successfully")%>
                            <%
                                if (!StringUtils.isBlank(sp) && sp.equals("My Account")) {
                                    url = IdentityManagementEndpointUtil.getURLEncodedCallback(tenantedMyaccountURL);
                                } else {
                                    if (StringUtils.isNotBlank(applicationAccessURLWithoutEncoding)) {
                                        url =IdentityManagementEndpointUtil.getURLEncodedCallback(applicationAccessURLWithoutEncoding);
                                    }
                                }
                                if (StringUtils.isNotBlank(url)) {
                            %>
                                <script>countdown('<%= url %>');</script>
                                <br/><br/>
                                <%=IdentityManagementEndpointUtil.i18n(recoveryResourceBundle, "you.will.redirected.back.to.the.application.in")%>
                                <span id="countdown" class="text-typography primary">3</span> <%=IdentityManagementEndpointUtil.i18n(recoveryResourceBundle, "seconds")%>
                                <br/><br/>

                            <% } else { %>
                                <br/><br/>
                                <%=IdentityManagementEndpointUtil.i18n(recoveryResourceBundle, "you.can.now.sign.in.to.account")%>
                                <br/><br/>
                            <%
                                }
                            } else {
                                if(accountLockOnCreationEnabled) {
                            %>
                                <%=IdentityManagementEndpointUtil.i18n(recoveryResourceBundle, "check.your.inbox.at")%>
                                <b><span id="maskedEmail"></span></b> <%=IdentityManagementEndpointUtil.i18n(recoveryResourceBundle, "for.instructions.to.activate.your.account")%>
                                <script>maskEmail('<%= emailValue %>');</script>
                                </br></br>
                        <%
                                if (showBackButton && StringUtils.isNotBlank(applicationAccessURLWithoutEncoding)) {
                        %>
                                    <i class="caret left icon primary"></i>
                                    <a href="<%= IdentityManagementEndpointUtil.getURLEncodedCallback(applicationAccessURLWithoutEncoding)%>">
                                        <%=IdentityManagementEndpointUtil.i18n(recoveryResourceBundle,"Back.to.application")%>
                                    </a>
                        <%
                            } else {
                                if (sp.equals("My Account")) {
                        %>
                                <i class="caret left icon primary"></i>
                                <a href="<%= IdentityManagementEndpointUtil.getURLEncodedCallback(tenantedMyaccountURL)%>">
                                    <%=IdentityManagementEndpointUtil.i18n(recoveryResourceBundle,"Go.to.MyAccount")%>
                                </a>
                        <%
                                }
                            }
                        } else {
                                if (sp.equals("My Account")) {
                                    url = IdentityManagementEndpointUtil.getURLEncodedCallback(tenantedMyaccountURL);
                                } else {
                                    if (StringUtils.isNotBlank(applicationAccessURLWithoutEncoding)) {
                                        url =IdentityManagementEndpointUtil.getURLEncodedCallback(applicationAccessURLWithoutEncoding);
                                    }
                                }
                                if (autoLoginEnabled && !accountLockOnCreationEnabled
                                                && StringUtils.isNotBlank(sessionDataKey)) {

                                    String identityServerEndpointContextParam =
                                        application.getInitParameter("IdentityServerEndpointContextURL");
                                    if (StringUtils.isBlank(identityServerEndpointContextParam)) {
                                        identityServerEndpointContextParam = ServiceURLBuilder.create()
                                                .setTenant(tenantDomain).build().getAbsolutePublicURL();
                                    } else if (!StringUtils.equals(tenantDomain, "carbon.super")){
                                        identityServerEndpointContextParam += "/t/" + tenantDomain;
                                    }

                                    url = identityServerEndpointContextParam + "/commonauth?sessionDataKey="
                                                + sessionDataKey;
                            }
                            int countdown = 3;
                            if (accountVerification) {
                            %>
                                <%=IdentityManagementEndpointUtil.i18n(recoveryResourceBundle, "check.your.inbox.at")%> <b><span id="maskedEmail"></span></b>
                                <%=IdentityManagementEndpointUtil.i18n(recoveryResourceBundle, "several.functionalities.will.not.available.until.confirmation")%>
                                <br/><br/>
                            <%
                                countdown = 5;
                            }
                            if (StringUtils.isNotBlank(url)) {
                        %>
                                <p class="portal-tagline-description">
                                    <script>countdown('<%= url %>');</script>
                                    <%=IdentityManagementEndpointUtil.i18n(recoveryResourceBundle, "you.will.redirected.back.to.the.application.in")%>
                                    <span id="countdown"><%= countdown %></span> <%=IdentityManagementEndpointUtil.i18n(recoveryResourceBundle, "seconds")%>
                                    <br/><br/>
		            <%
		                    } else {
		            %>
                                <p class="portal-tagline-description">
                                    <br/><br/>
                                    <%=IdentityManagementEndpointUtil.i18n(recoveryResourceBundle, "you.can.now.sign.in.to.account")%>
                                    <br/>
                        <%
                            }
                            }
                        }
                        %>
                    </p>
                </div>
            </layout:component>
            <layout:component componentName="ProductFooter" >
                <%-- product-footer --%>
                <%
                    File productFooterFile = new File(getServletContext().getRealPath("extensions/product-footer.jsp"));
                    if (productFooterFile.exists()) {
                %>
                    <jsp:include page="extensions/product-footer.jsp"/>
                <% } else { %>
                    <jsp:include page="includes/product-footer.jsp"/>
                <% } %>
            </layout:component>
            <layout:dynamicComponent filePathStoringVariableName="pathOfDynamicComponent">
                <jsp:include page="${pathOfDynamicComponent}" />
            </layout:dynamicComponent>
        </layout:main>
    <% } %>

    <%-- footer --%>
    <%
        File footerFile = new File(getServletContext().getRealPath("extensions/footer.jsp"));
        if (footerFile.exists()) {
    %>
    <jsp:include page="extensions/footer.jsp"/>
    <% } else { %>
    <jsp:include page="includes/footer.jsp"/>
    <% } %>
</body>
</html>
