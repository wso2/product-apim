<%--
  ~ Copyright (c) 2016, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
  ~
  ~  WSO2 Inc. licenses this file to you under the Apache License,
  ~  Version 2.0 (the "License"); you may not use this file except
  ~  in compliance with the License.
  ~  You may obtain a copy of the License at
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

<%@ page import="org.apache.commons.collections.map.HashedMap" %>
<%@ page import="org.apache.commons.lang.StringUtils" %>
<%@ page import="org.wso2.carbon.core.util.SignatureUtil" %>
<%@ page import="org.wso2.carbon.identity.mgt.endpoint.util.IdentityManagementEndpointConstants" %>
<%@ page import="org.wso2.carbon.identity.mgt.endpoint.util.IdentityManagementServiceUtil" %>
<%@ page import="org.wso2.carbon.identity.mgt.endpoint.util.client.ApiException" %>
<%@ page import="org.wso2.carbon.identity.mgt.endpoint.util.client.PreferenceRetrievalClient" %>
<%@ page import="org.wso2.carbon.identity.mgt.endpoint.util.client.api.SelfRegisterApi" %>
<%@ page import="org.wso2.carbon.identity.mgt.endpoint.util.client.api.UsernameRecoveryApi" %>
<%@ page import="org.wso2.carbon.identity.mgt.endpoint.util.IdentityManagementEndpointUtil" %>
<%@ page import="org.wso2.carbon.identity.core.util.IdentityTenantUtil" %>
<%@ page import="org.wso2.carbon.identity.mgt.endpoint.util.client.model.*" %>
<%@ page import="org.wso2.carbon.identity.recovery.util.Utils" %>
<%@ page import="org.wso2.carbon.identity.recovery.IdentityRecoveryConstants" %>
<%@ page import="org.wso2.carbon.identity.base.IdentityRuntimeException" %>
<%@ page import="org.json.simple.JSONObject" %>
<%@ page import="java.io.File" %>
<%@ page import="java.net.URLEncoder" %>
<%@ page import="java.util.ArrayList" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.Map" %>
<%@ page import="java.util.Base64" %>
<%@ page import="org.wso2.carbon.identity.core.util.IdentityUtil" %>
<%@ page import="javax.servlet.http.Cookie" %>
<jsp:directive.include file="includes/localize.jsp"/>
<jsp:directive.include file="tenant-resolve.jsp"/>

<html>
<head>
    <meta http-equiv="X-UA-Compatible" content="IE=edge">
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <!-- title -->
    <%
        File titleFile = new File(getServletContext().getRealPath("extensions/title.jsp"));
        if (titleFile.exists()) {
    %>
    <jsp:include page="extensions/title.jsp"/>
    <% } else { %>
    <jsp:include page="includes/title.jsp"/>
    <% } %>

    <link rel="icon" href="images/favicon.png" type="image/x-icon"/>
    <link href="libs/bootstrap_3.4.1/css/bootstrap.min.css" rel="stylesheet">
    <link href="css/Roboto.css" rel="stylesheet">
    <link href="css/custom-common.css" rel="stylesheet">

    <!--[if lt IE 9]>
    <script src="js/html5shiv.min.js"></script>
    <script src="js/respond.min.js"></script>
    <![endif]-->
</head>
<body>
    <!-- header -->
    <%
        File headerFile = new File(getServletContext().getRealPath("extensions/header.jsp"));
        if (headerFile.exists()) {
    %>
    <jsp:include page="extensions/header.jsp"/>
    <% } else { %>
    <jsp:include page="includes/header.jsp"/>
    <% } %>

    <!-- page content -->
    <div class="container-fluid body-wrapper">

        <%
            String ERROR_MESSAGE = "errorMsg";
            String ERROR_CODE = "errorCode";
            String SELF_REGISTRATION_WITH_VERIFICATION_PAGE = "self-registration-with-verification.jsp";
            String SELF_REGISTRATION_WITHOUT_VERIFICATION_PAGE = "* self-registration-without-verification.jsp";
            String passwordPatternErrorCode = "20035";
            String invalidCharErrorCode = "20067";
            String AUTO_LOGIN_COOKIE_NAME = "ALOR";
            String AUTO_LOGIN_COOKIE_DOMAIN = "AutoLoginCookieDomain";
            String AUTO_LOGIN_FLOW_TYPE = "SIGNUP";
            PreferenceRetrievalClient preferenceRetrievalClient = new PreferenceRetrievalClient();
            Boolean isAutoLoginEnable = preferenceRetrievalClient.checkAutoLoginAfterSelfRegistrationEnabled(tenantDomain);
            Boolean isSelfRegistrationWithVerificationEnabled = preferenceRetrievalClient.checkSelfRegistrationLockOnCreation(tenantDomain);
    
            boolean isSelfRegistrationWithVerification =
                    Boolean.parseBoolean(request.getParameter("isSelfRegistrationWithVerification"));

            String userLocale = request.getHeader("Accept-Language");
            String username = request.getParameter("username");
            String password = request.getParameter("password");
            String callback = request.getParameter("callback");
            String consent = request.getParameter("consent");
            boolean isSaaSApp = Boolean.parseBoolean(request.getParameter("isSaaSApp"));
            String policyURL = IdentityManagementServiceUtil.getInstance().getServiceContextURL().replace("/services",
                    "/authenticationendpoint/privacy_policy.do");

            try {
                if (StringUtils.isNotBlank(callback) && !Utils.validateCallbackURL(callback, tenantDomain,
                    IdentityRecoveryConstants.ConnectorConfig.SELF_REGISTRATION_CALLBACK_REGEX)) {
                    request.setAttribute("error", true);
                    request.setAttribute("errorMsg", IdentityManagementEndpointUtil.i18n(recoveryResourceBundle,
                        "Callback.url.format.invalid"));
                    request.getRequestDispatcher("error.jsp").forward(request, response);
                    return;
                }
            } catch (IdentityRuntimeException e) {
                request.setAttribute("error", true);
                request.setAttribute("errorMsg", e.getMessage());
                request.getRequestDispatcher("error.jsp").forward(request, response);
                return;
            }

            if (StringUtils.isNotEmpty(consent)) {
                consent = IdentityManagementEndpointUtil.buildConsentForResidentIDP
                        (username, consent, "USA",
                                IdentityManagementEndpointConstants.Consent.COLLECTION_METHOD_SELF_REGISTRATION,
                                IdentityManagementEndpointConstants.Consent.LANGUAGE_ENGLISH, policyURL,
                                IdentityManagementEndpointConstants.Consent.EXPLICIT_CONSENT_TYPE,
                                true, false, IdentityManagementEndpointConstants.Consent.INFINITE_TERMINATION);
            }
            if (StringUtils.isBlank(callback)) {
                callback = IdentityManagementEndpointUtil.getUserPortalUrl(
                application.getInitParameter(IdentityManagementEndpointConstants.ConfigConstants.USER_PORTAL_URL), tenantDomain);
            }
            if (StringUtils.isBlank(username)) {
                request.setAttribute("error", true);
                request.setAttribute("errorMsg", IdentityManagementEndpointUtil.i18n(recoveryResourceBundle,
                        "Username.cannot.be.empty"));
                if (isSelfRegistrationWithVerification) {
                    request.getRequestDispatcher("self-registration-with-verification.jsp").forward(request, response);
                } else {
                    request.getRequestDispatcher("* self-registration-without-verification.jsp").forward(request, response);
                }
            }

            if (StringUtils.isBlank(password)) {
                request.setAttribute("error", true);
                request.setAttribute("errorMsg", IdentityManagementEndpointUtil.i18n(recoveryResourceBundle,
                        "Password.cannot.be.empty"));
                if (isSelfRegistrationWithVerification) {
                    request.getRequestDispatcher("self-registration-with-verification.jsp").forward(request, response);
                } else {
                    request.getRequestDispatcher("* self-registration-without-verification.jsp").forward(request, response);
                }
            }

            session.setAttribute("username", username);


            User user = IdentityManagementServiceUtil.getInstance().resolveUser(username, tenantDomain, isSaaSApp);


            Claim[] claims = new Claim[0];

            List<Claim> claimsList;
            UsernameRecoveryApi usernameRecoveryApi = new UsernameRecoveryApi();
            try {
                claimsList = usernameRecoveryApi.claimsGet(user.getTenantDomain());
                if (claimsList != null) {
                    claims = claimsList.toArray(new Claim[claimsList.size()]);
                }
            } catch (ApiException e) {
                IdentityManagementEndpointUtil.addErrorInformation(request, e);
                request.getRequestDispatcher("error.jsp").forward(request, response);
                return;
            }


            List<Claim> userClaimList = new ArrayList<Claim>();
            try {

                for (Claim claim : claims) {
                    if (StringUtils.isNotBlank(request.getParameter(claim.getUri()))) {
                        Claim userClaim = new Claim();
                        userClaim.setUri(claim.getUri());
                        userClaim.setValue(request.getParameter(claim.getUri()));
                        userClaimList.add(userClaim);

                    } else if (claim.getUri().trim().equals(IdentityUtil.getClaimUriLocale())
                            && StringUtils.isNotBlank(userLocale)) {

                        Claim localeClaim = new Claim();
                        localeClaim.setUri(claim.getUri());
                        localeClaim.setValue(userLocale.split(",")[0].replace('-', '_'));
                        userClaimList.add(localeClaim);

                    }
                }

                SelfRegistrationUser selfRegistrationUser = new SelfRegistrationUser();
                selfRegistrationUser.setUsername(user.getUsername());
                selfRegistrationUser.setTenantDomain(user.getTenantDomain());
                selfRegistrationUser.setRealm(user.getRealm());
                selfRegistrationUser.setPassword(password);
                selfRegistrationUser.setClaims(userClaimList);

                List<Property> properties = new ArrayList<Property>();
                Property sessionKey = new Property();
                sessionKey.setKey("callback");
                sessionKey.setValue(URLEncoder.encode(callback, "UTF-8"));

                Property consentProperty = new Property();
                consentProperty.setKey("consent");
                consentProperty.setValue(consent);
                properties.add(sessionKey);
                properties.add(consentProperty);


                SelfUserRegistrationRequest selfUserRegistrationRequest = new SelfUserRegistrationRequest();
                selfUserRegistrationRequest.setUser(selfRegistrationUser);
                selfUserRegistrationRequest.setProperties(properties);

                Map<String, String> requestHeaders = new HashedMap();
                if (request.getParameter("g-recaptcha-response") != null) {
                    requestHeaders.put("g-recaptcha-response", request.getParameter("g-recaptcha-response"));
                }

                SelfRegisterApi selfRegisterApi = new SelfRegisterApi();
                selfRegisterApi.mePostCall(selfUserRegistrationRequest, requestHeaders);
                // Add auto login cookie.
                if (isAutoLoginEnable && !isSelfRegistrationWithVerificationEnabled) {
                    if (StringUtils.isNotEmpty(user.getRealm())) {
                        username = user.getRealm() + "/" + user.getUsername() + "@" + user.getTenantDomain();
                    } else {
                        username = user.getUsername() + "@" + user.getTenantDomain();
                    }
                    String cookieDomain = application.getInitParameter(AUTO_LOGIN_COOKIE_DOMAIN);
                    JSONObject contentValueInJson = new JSONObject();
                    contentValueInJson.put("username", username);
                    contentValueInJson.put("createdTime", System.currentTimeMillis());
                    contentValueInJson.put("flowType", AUTO_LOGIN_FLOW_TYPE);
                    SignatureUtil.init();
                    if (StringUtils.isNotBlank(cookieDomain)) {
                        contentValueInJson.put("domain", cookieDomain);
                    }
                    String content = contentValueInJson.toString();
        
                    JSONObject cookieValueInJson = new JSONObject();
                    cookieValueInJson.put("content", content);
                    String signature = Base64.getEncoder().encodeToString(SignatureUtil.doSignature(content));
                    cookieValueInJson.put("signature", signature);
                    Cookie cookie = new Cookie(AUTO_LOGIN_COOKIE_NAME,
                            Base64.getEncoder().encodeToString(cookieValueInJson.toString().getBytes()));
                    cookie.setPath("/");
                    cookie.setSecure(true);
                    cookie.setMaxAge(300);
                    if (StringUtils.isNotBlank(cookieDomain)) {
                        cookie.setDomain(cookieDomain);
                    }
                    response.addCookie(cookie);
                    request.setAttribute("isAutoLoginEnabled", true);
                }
                request.setAttribute("callback", callback);
                request.getRequestDispatcher("self-registration-complete.jsp").forward(request, response);

            } catch (Exception e) {
                IdentityManagementEndpointUtil.addErrorInformation(request, e);
                String errorCode = (String) request.getAttribute("errorCode");
                if (passwordPatternErrorCode.equals(errorCode) || invalidCharErrorCode.equals(errorCode)) {
                    String i18Resource = IdentityManagementEndpointUtil.i18n(recoveryResourceBundle, errorCode);
                    if (!i18Resource.equals(errorCode)) {
                        request.setAttribute(ERROR_MESSAGE, i18Resource);
                    }
                    if (isSelfRegistrationWithVerification) {
                        request.getRequestDispatcher(SELF_REGISTRATION_WITH_VERIFICATION_PAGE).forward(request,
                                response);
                    } else {
                        request.getRequestDispatcher(SELF_REGISTRATION_WITHOUT_VERIFICATION_PAGE).forward(request,
                                response);
                    }

                    return;
                }
            }


        %>
    </div>


    <!-- footer -->
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
