<%--
  ~ Copyright (c) 2018-2023, WSO2 LLC. (https://www.wso2.com).
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

<%@ page import="org.apache.commons.collections.CollectionUtils" %>
<%@ page import="org.apache.commons.collections.MapUtils" %>
<%@ page import="org.apache.commons.lang.ArrayUtils" %>
<%@ page import="org.apache.commons.lang.StringUtils" %>
<%@ page import="org.apache.commons.text.StringEscapeUtils" %>
<%@ page import="org.owasp.encoder.Encode" %>
<%@ page import="org.wso2.carbon.identity.application.authentication.endpoint.util.AuthenticationEndpointUtil" %>
<%@ page import="static org.wso2.carbon.identity.application.authentication.endpoint.util.Constants.STATUS" %>
<%@ page import="static org.wso2.carbon.identity.application.authentication.endpoint.util.Constants.STATUS_MSG" %>
<%@ page import="org.wso2.carbon.identity.application.authentication.endpoint.util.Constants" %>
<%@ page import="org.wso2.carbon.identity.captcha.util.CaptchaUtil" %>
<%@ page import="org.wso2.carbon.identity.core.URLBuilderException" %>
<%@ page import="org.wso2.carbon.identity.mgt.constants.SelfRegistrationStatusCodes" %>
<%@ page import="org.wso2.carbon.identity.mgt.endpoint.util.IdentityManagementEndpointConstants" %>
<%@ page import="org.wso2.carbon.identity.mgt.endpoint.util.IdentityManagementServiceUtil" %>
<%@ page import="org.wso2.carbon.identity.mgt.endpoint.util.IdentityManagementEndpointUtil" %>
<%@ page import="org.wso2.carbon.identity.core.util.IdentityUtil" %>
<%@ page import="org.wso2.carbon.identity.mgt.endpoint.util.client.ApiException" %>
<%@ page import="org.wso2.carbon.identity.mgt.endpoint.util.client.api.ReCaptchaApi" %>
<%@ page import="org.wso2.carbon.identity.mgt.endpoint.util.client.model.ReCaptchaProperties" %>
<%@ page import="org.wso2.carbon.identity.mgt.endpoint.util.client.SelfRegistrationMgtClient" %>
<%@ page import="org.wso2.carbon.identity.mgt.endpoint.util.client.SelfRegistrationMgtClientException" %>
<%@ page import="org.wso2.carbon.identity.mgt.endpoint.util.client.ApplicationDataRetrievalClient" %>
<%@ page import="org.wso2.carbon.identity.mgt.endpoint.util.client.ApplicationDataRetrievalClientException" %>
<%@ page import="org.wso2.carbon.identity.mgt.endpoint.util.client.ConfiguredAuthenticatorsRetrievalClient" %>
<%@ page import="org.wso2.carbon.identity.mgt.endpoint.util.client.ConfiguredAuthenticatorsRetrievalClientException" %>
<%@ page import="org.wso2.carbon.identity.mgt.endpoint.util.client.IdentityProviderDataRetrievalClient" %>
<%@ page import="org.wso2.carbon.identity.mgt.endpoint.util.client.IdentityProviderDataRetrievalClientException" %>
<%@ page import="org.wso2.carbon.identity.mgt.endpoint.util.client.api.UsernameRecoveryApi" %>
<%@ page import="org.wso2.carbon.identity.mgt.endpoint.util.client.model.Claim" %>
<%@ page import="org.wso2.carbon.identity.mgt.endpoint.util.client.model.User" %>
<%@ page import="org.wso2.carbon.identity.mgt.endpoint.util.client.ValidationConfigurationRetrievalClient" %>
<%@ page import="org.wso2.carbon.identity.mgt.endpoint.util.client.PreferenceRetrievalClient" %>
<%@ page import="org.wso2.carbon.identity.mgt.endpoint.util.client.PreferenceRetrievalClientException" %>
<%@ page import="org.wso2.carbon.identity.core.util.IdentityTenantUtil" %>
<%@ page import="org.wso2.carbon.identity.core.ServiceURLBuilder" %>
<%@ page import="org.wso2.carbon.utils.multitenancy.MultitenantUtils" %>
<%@ page import="java.io.File" %>
<%@ page import="java.util.Arrays" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.Locale" %>
<%@ page import="java.util.Map" %>
<%@ page import="java.util.HashMap" %>
<%@ page import="java.util.TreeMap" %>
<%@ page import="java.util.Enumeration" %>
<%@ page import="org.json.JSONObject" %>
<%@ page import="org.json.JSONArray" %>

<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="layout" uri="org.wso2.identity.apps.taglibs.layout.controller" %>

<%-- Localization --%>
<jsp:directive.include file="includes/localize.jsp"/>

<%-- Include tenant context --%>
<jsp:directive.include file="tenant-resolve.jsp"/>

<%-- Branding Preferences --%>
<jsp:directive.include file="includes/branding-preferences.jsp"/>

<%
    String BASIC_AUTHENTICATOR = "BasicAuthenticator";
    String OPEN_ID_AUTHENTICATOR = "OpenIDAuthenticator";
    String GOOGLE_AUTHENTICATOR = "GoogleOIDCAuthenticator";
    String GITHUB_AUTHENTICATOR = "GithubAuthenticator";
    String FACEBOOK_AUTHENTICATOR = "FacebookAuthenticator";
    String OIDC_AUTHENTICATOR = "OpenIDConnectAuthenticator";
    String SSO_AUTHENTICATOR = "OrganizationAuthenticator";
    String commonauthURL = "../commonauth";

    boolean error = IdentityManagementEndpointUtil.getBooleanValue(request.getAttribute("error"));
    boolean isSaaSApp = Boolean.parseBoolean(request.getParameter("isSaaSApp"));
    boolean skipSignUpEnableCheck = Boolean.parseBoolean(request.getParameter("skipsignupenablecheck"));
    boolean allowchangeusername = Boolean.parseBoolean(request.getParameter("allowchangeusername"));
    boolean isPasswordProvisionEnabled = Boolean.parseBoolean(request.getParameter("passwordProvisionEnabled"));
    boolean piisConfigured = false;
    PreferenceRetrievalClient preferenceRetrievalClient = new PreferenceRetrievalClient();
    boolean isSelfRegistrationLockOnCreationEnabled = preferenceRetrievalClient.checkSelfRegistrationLockOnCreation(tenantDomain);
    String callback = Encode.forHtmlAttribute(request.getParameter("callback"));
    String backToUrl = callback;
    String sp = Encode.forHtmlAttribute(request.getParameter("sp"));
    String previousStep = Encode.forHtmlAttribute(request.getParameter("previous_step"));
    String username = request.getParameter("username");
    String password = request.getParameter("password");
    String emailValue = request.getParameter("http://wso2.org/claims/emailaddress");
    String errorCode = null;
    String spId = "";
    String errorCodeFromRequest = IdentityManagementEndpointUtil.getStringValue(request.getAttribute("errorCode"));
    String errorMsg = IdentityManagementEndpointUtil.getStringValue(request.getAttribute("errorMsg"));
    String consentPurposeGroupName = "SELF-SIGNUP";
    String consentPurposeGroupType = "SYSTEM";

    String[] missingClaimList = new String[0];
    String[] missingClaimDisplayName = new String[0];
    Integer defaultPurposeCatId = null;

    Object errorCodeObj = request.getAttribute("errorCode");
    Object errorMsgObj = request.getAttribute("errorMsg");
    JSONObject usernameValidityResponse;

    Map<String, Claim> uniquePIIs = null;

    SelfRegistrationMgtClient selfRegistrationMgtClient = new SelfRegistrationMgtClient();
    User user = IdentityManagementServiceUtil.getInstance().resolveUser(username, tenantDomain, isSaaSApp);
    boolean isUsernameValidationEnabled = Boolean.parseBoolean(IdentityUtil.getProperty("InputValidation.Username.Enabled"));
    ApplicationDataRetrievalClient applicationDataRetrievalClient = new ApplicationDataRetrievalClient();
    try {
        // Retrieve application Id.
        spId = applicationDataRetrievalClient.getApplicationID(tenantDomain,sp);
    } catch (Exception e) {
        // Nothing happens.
    }

    // Get validation configuration.
    ValidationConfigurationRetrievalClient validationConfigurationRetrievalClient = new ValidationConfigurationRetrievalClient();
    JSONObject passwordConfig = null;
    JSONObject usernameConfig = null;
    try {
        passwordConfig = validationConfigurationRetrievalClient.getPasswordConfiguration(tenantDomain);
        usernameConfig = validationConfigurationRetrievalClient.getUsernameConfiguration(tenantDomain);
    } catch (Exception e) {
        passwordConfig = null;
        usernameConfig = null;
    }

    // Get authenticators configured for an application.
    JSONArray configuredAuthenticators = null;
    if (!StringUtils.equalsIgnoreCase(spId,"")) {
        try {
            ConfiguredAuthenticatorsRetrievalClient configuredAuthenticatorsRetrievalClient = new ConfiguredAuthenticatorsRetrievalClient();
            configuredAuthenticators = configuredAuthenticatorsRetrievalClient.getConfiguredAuthenticators(spId, tenantDomain);
        } catch (Exception e) {
            configuredAuthenticators = null;
        }
    }

    String identityServerEndpointContextParam = IdentityUtil.getServerURL("/", true, true);
    if (!StringUtils.equals(tenantDomain, "carbon.super")) {
        identityServerEndpointContextParam = ServiceURLBuilder.create().setTenant(tenantDomain).build()
                .getAbsolutePublicURL();
    }
    commonauthURL = identityServerEndpointContextParam + "/commonauth";

    String multiOptionURIParam = "";
    String baseURL;
    try {
        baseURL = ServiceURLBuilder.create().addPath(request.getRequestURI()).build().getRelativePublicURL();
    } catch (URLBuilderException e) {
        request.setAttribute(STATUS, AuthenticationEndpointUtil.i18n(recoveryResourceBundle, "internal.error.occurred"));
        request.setAttribute(STATUS_MSG, AuthenticationEndpointUtil.i18n(recoveryResourceBundle, "error.when.processing.authentication.request"));
        request.getRequestDispatcher("error.do").forward(request, response);
        return;
    }

    String queryParamString = request.getQueryString() != null ? ("?" + request.getQueryString()) : "";
    multiOptionURIParam = "&multiOptionURI=" + Encode.forUriComponent(baseURL + queryParamString);

    boolean isLocal = false;
    boolean isFederated = false;
    boolean isBasic = false;
    // Enable basic account creation flow if there are no authenticators configured.
    if (configuredAuthenticators == null) {
        isBasic = true;
    }
    JSONArray localAuthenticators = new JSONArray();
    JSONArray federatedAuthenticators = new JSONArray();
    if (configuredAuthenticators != null) {
        for ( int index = 0; index < configuredAuthenticators.length(); index++) {
            JSONObject step = (JSONObject)configuredAuthenticators.get(index);
            int stepId = (int)step.get("stepId");
            if (stepId == 1 || stepId == 2) {
                JSONArray tempLocalAuthenticators = (JSONArray)step.get("localAuthenticators");
                for (int i = 0; i < tempLocalAuthenticators.length(); i++) {
                    JSONObject localAuth = (JSONObject)tempLocalAuthenticators.get(i);
                    localAuthenticators.put(localAuth);
                    // check basic authenticator
                    if (StringUtils.equalsIgnoreCase(BASIC_AUTHENTICATOR, (String)localAuth.get("type"))) {
                        isLocal = true;
                    }
                }
            }
            if (stepId == 1) {
                federatedAuthenticators = (JSONArray)step.get("federatedAuthenticators");
                if (federatedAuthenticators.length() > 0) {
                    isFederated = true;
                }
            }
        }
    }

    if (request.getParameter(Constants.MISSING_CLAIMS) != null) {
        missingClaimList = request.getParameter(Constants.MISSING_CLAIMS).split(",");
    }
    if (request.getParameter("missingClaimsDisplayName") != null) {
        missingClaimDisplayName = request.getParameter("missingClaimsDisplayName").split(",");
    }

    if (errorCodeObj != null) {
        errorCode = errorCodeObj.toString();
    }
    if (SelfRegistrationStatusCodes.ERROR_CODE_INVALID_TENANT.equalsIgnoreCase(errorCode)) {
        errorMsg = IdentityManagementEndpointUtil.i18n(recoveryResourceBundle, "invalid.tenant.domain")
                + " - " + user.getTenantDomain();
    } else if (SelfRegistrationStatusCodes.ERROR_CODE_USER_ALREADY_EXISTS.equalsIgnoreCase(errorCode)) {
        errorMsg = IdentityManagementEndpointUtil.i18n(recoveryResourceBundle, "Username") + " '"
                + username + "' " + IdentityManagementEndpointUtil.i18n(recoveryResourceBundle, "username.already.taken.pick.different.username");
        isFederated = false;
    } else if (SelfRegistrationStatusCodes.ERROR_CODE_SELF_REGISTRATION_DISABLED.equalsIgnoreCase(errorCode)) {
        errorMsg = IdentityManagementEndpointUtil.i18n(recoveryResourceBundle, "self.registration.disabled.for.tenant")
            + " - " + user.getTenantDomain();
    } else if (SelfRegistrationStatusCodes.CODE_USER_NAME_INVALID.equalsIgnoreCase(errorCode)) {
        if (request.getAttribute("errorMessage") != null) {
            errorMsg = (String) request.getAttribute("errorMessage");
        } else {
            errorMsg = user.getUsername() + " "
                    + IdentityManagementEndpointUtil.i18n(recoveryResourceBundle, "invalid.username.pick.a.valid.username");
        }
    } else if (StringUtils.equalsIgnoreCase(SelfRegistrationStatusCodes.ERROR_CODE_INVALID_EMAIL_USERNAME,
            errorCode)) {
        errorMsg = IdentityManagementEndpointUtil.i18n(recoveryResourceBundle, "username.is.invalid.should.be.in.email.format");
    } else if (SelfRegistrationStatusCodes.ERROR_CODE_INVALID_USERSTORE.equalsIgnoreCase(errorCode)) {
        errorMsg = IdentityManagementEndpointUtil.i18n(recoveryResourceBundle, "invalid.userstore.domain") + " - " + user.getRealm();
    } else if (errorMsgObj != null) {
        errorMsg = errorMsgObj.toString();
    }

        /**
    * For SaaS applications, read the user tenant from parameters.
    */
    String srtenantDomain = request.getParameter("srtenantDomain");
    if (isSaaSApp && StringUtils.isNotBlank(srtenantDomain)) {
        tenantDomain = srtenantDomain;
    }

    if (skipSignUpEnableCheck) {
        consentPurposeGroupName = "JIT";
    }

    if (StringUtils.isBlank(callback) || StringUtils.equalsIgnoreCase(callback, "null")) {
        callback = Encode.forHtmlAttribute(IdentityManagementEndpointUtil.getUserPortalUrl(
                application.getInitParameter(IdentityManagementEndpointConstants.ConfigConstants.USER_PORTAL_URL), tenantDomain));
    }

    /**
    * If backToUrl is null get to access url of the application.
    */
    if (StringUtils.equalsIgnoreCase(backToUrl,"null")) {
        try {
                // Retrieve application access url to redirect user back to the application.
                backToUrl = applicationDataRetrievalClient.getApplicationAccessURL(tenantDomain, sp);
            } catch (Exception e) {
                backToUrl = null;
        }
    }

    String purposes;
    try {
        purposes = selfRegistrationMgtClient.getPurposes(tenantDomain, consentPurposeGroupName,
            consentPurposeGroupType);
    } catch (SelfRegistrationMgtClientException e) {
        purposes = null;
    }
    boolean hasPurposes = StringUtils.isNotEmpty(purposes);
    Claim[] claims = new Claim[0];

    /**
     * Change consentDisplayType to "template" inorder to use a custom html template.
     * other Default values are "row" and "tree".
     */
    String consentDisplayType = "row";

    if (hasPurposes) {
        defaultPurposeCatId = selfRegistrationMgtClient.getDefaultPurposeId(tenantDomain);
        uniquePIIs = IdentityManagementEndpointUtil.getUniquePIIs(purposes);
        if (MapUtils.isNotEmpty(uniquePIIs)) {
            piisConfigured = true;
        }
    }

    List<Claim> claimsList;
    UsernameRecoveryApi usernameRecoveryApi = new UsernameRecoveryApi();
    try {
        claimsList = usernameRecoveryApi.claimsGet(tenantDomain, false);
        uniquePIIs = IdentityManagementEndpointUtil.fillPiisWithClaimInfo(uniquePIIs, claimsList);
        if (uniquePIIs != null) {
            claims = uniquePIIs.values().toArray(new Claim[0]);
        }
        IdentityManagementEndpointUtil.addReCaptchaHeaders(request, usernameRecoveryApi.getApiClient().getResponseHeaders());

    } catch (ApiException e) {
        IdentityManagementEndpointUtil.addErrorInformation(request, e);
        if (!StringUtils.isBlank(username)) {
            request.setAttribute("username", username);
        }
        request.getRequestDispatcher("error.jsp").forward(request, response);
        return;
    }

    Claim emailPII =
        uniquePIIs.get(IdentityManagementEndpointConstants.ClaimURIs.EMAIL_CLAIM);
    Boolean isAlphanumericUsernameEnabled = false;
    if (usernameConfig.has("alphanumericFormatValidator")) {
        isAlphanumericUsernameEnabled = (Boolean) usernameConfig.get("alphanumericFormatValidator");
    }

    /**
    * Temporarily read recapcha status from password recovery endpoint.
    */
    ReCaptchaApi reCaptchaApi = new ReCaptchaApi();
    try {
        ReCaptchaProperties reCaptchaProperties = reCaptchaApi.getReCaptcha(tenantDomain, true, "ReCaptcha",
            "self-registration");

        if (reCaptchaProperties.getReCaptchaEnabled()) {
            Map<String, List<String>> headers = new HashMap<>();
            headers.put("reCaptcha", Arrays.asList(String.valueOf(true)));
            headers.put("reCaptchaAPI", Arrays.asList(reCaptchaProperties.getReCaptchaAPI()));
            headers.put("reCaptchaKey", Arrays.asList(reCaptchaProperties.getReCaptchaKey()));
            IdentityManagementEndpointUtil.addReCaptchaHeaders(request, headers);
        }
    } catch (ApiException e) {
        request.setAttribute("error", true);
        request.setAttribute("errorMsg", e.getMessage());
        if (!StringUtils.isBlank(username)) {
            request.setAttribute("username", username);
        }
        request.getRequestDispatcher("error.jsp").forward(request, response);
        return;
    }

    boolean reCaptchaEnabled = false;
    if (request.getAttribute("reCaptcha") != null && "TRUE".equalsIgnoreCase((String) request.getAttribute("reCaptcha"))) {
        reCaptchaEnabled = true;
    } else if (request.getParameter("reCaptcha") != null && Boolean.parseBoolean(request.getParameter("reCaptcha"))) {
        reCaptchaEnabled = true;
    }
%>
<%!
    /**
     * Retrieve all county codes and country display names and
     * store into a map where key/value pair is defined as the
     * country code/country display name.
     *
     * @return {Map<string, string>}
     */
    private Map<String, String> getCountryList() {
        String[] countryCodes = Locale.getISOCountries();

        Map<String, String> mapCountries = new TreeMap<>();

        for (String countryCode : countryCodes) {
            Locale locale = new Locale("", countryCode);
            String country_code = locale.getCountry();
            String country_display_name = locale.getDisplayCountry();
            mapCountries.put(country_code, country_display_name);
        }

        return mapCountries;
    }
%>

<!doctype html>
<html lang="en-US">
<head>
    <%-- header --%>
    <%
        File headerFile = new File(getServletContext().getRealPath("extensions/header.jsp"));
        if (headerFile.exists()) {
    %>
    <jsp:include page="extensions/header.jsp"/>
    <% } else { %>
    <jsp:include page="includes/header.jsp"/>
    <% } %>
    <%
        if (reCaptchaEnabled) {
            String reCaptchaAPI = CaptchaUtil.reCaptchaAPIURL();
    %>
        <script src='<%=(reCaptchaAPI)%>'></script>
    <%
        }
    %>
    <link rel="stylesheet" href="libs/addons/calendar.min.css"/>
</head>
<body class="login-portal layout recovery-layout">
    <layout:main layoutName="<%= layout %>" layoutFileRelativePath="<%= layoutFileRelativePath %>" data="<%= layoutData %>" >
        <layout:component componentName="ProductHeader">
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
            <div class="ui segment">
                <h3 class="ui header" data-testid="self-registration-username-request-page-header">
                    <%=i18n(recoveryResourceBundle, customText, "sign.up.heading")%>
                </h3>

                <% if (error) { %>
                <div class="ui negative message" id="server-error-msg">
                    <%= IdentityManagementEndpointUtil.i18nBase64(recoveryResourceBundle, errorMsg) %>
                </div>
                <% } %>

                <!-- show continue with email button -->
                <div id="continue-with-email" hidden="hidden">
                    <div class="buttons mt-4">
                        <button
                            id="ContinueWithEmail"
                            class="ui primary button large fluid"
                            type="submit"
                            data-testid="continue-with-email-button"
                            onclick="showEmail()"
                        >
                            <%=IdentityManagementEndpointUtil.i18n(recoveryResourceBundle, "continue.with.email")%>
                        </button>
                    </div>
                    <div class="ui horizontal divider">
                        <%=IdentityManagementEndpointUtil.i18n(recoveryResourceBundle, "or")%>
                    </div>
                </div>

                <!-- federated authenticators -->
                <div class="ui divider hidden"></div>
                <div id="federated-authenticators" hidden="hidden">
                    <%
                    for (int i = 0; i < federatedAuthenticators.length(); i++) {

                        JSONObject federatedAuthenticator = (JSONObject) federatedAuthenticators.get(i);
                        String name = (String)federatedAuthenticator.get("name");
                        String type = (String)federatedAuthenticator.get("type");
                        String displayName = name;

                        String imageURL = "libs/themes/wso2is/assets/images/identity-providers/enterprise-idp-illustration.svg";
                        try {
                            IdentityProviderDataRetrievalClient identityProviderDataRetrievalClient = new IdentityProviderDataRetrievalClient();
                            imageURL = identityProviderDataRetrievalClient.getIdPImage(tenantDomain, name);
                        } catch (IdentityProviderDataRetrievalClientException e) {
                            // Exception is ignored and the default `imageURL` value will be used as a fallback.
                        }
                        // If any IdP's name starts with `Sign in with`, then we need to remove the `Sign in with` part.
                        // If not, the UI will look weird with labels like `Sign in with Sign In With Google`.
                        String EXTERNAL_CONNECTION_PREFIX = "sign in with";
                        if (StringUtils.startsWithIgnoreCase(name, EXTERNAL_CONNECTION_PREFIX)) {
                            displayName = name.substring(EXTERNAL_CONNECTION_PREFIX.length());
                        }

                        if (StringUtils.equals(type,GOOGLE_AUTHENTICATOR)) {
                    %>

                    <div class="social-login blurring social-dimmer">
                        <div class="field">
                            <button type="button"
                                class="ui button"
                                data-testid="sign-up-with-google"
                                onclick="handleNoDomain(this,
                                                    '<%=Encode.forJavaScriptAttribute(Encode.forUriComponent(name))%>',
                                                    '<%=Encode.forJavaScriptAttribute(Encode.forUriComponent(type))%>')"
                            >
                            <img
                                class="ui image"
                                src="libs/themes/wso2is/assets/images/identity-providers/google-idp-illustration.svg"
                                alt="Google sign-up logo"
                                role="presentation">
                            <span><%=IdentityManagementEndpointUtil.i18n(recoveryResourceBundle, "continue.with")%> <%=Encode.forHtmlContent(displayName)%></span>
                            </button>
                        </div>
                    </div>
                    <br>
                    <%
                        } else if (StringUtils.equals(type,GITHUB_AUTHENTICATOR)) {
                    %>
                    <div class="social-login blurring social-dimmer">
                        <div class="field">
                            <button type="button"
                                class="ui button"
                                data-testid="sign-up-with-github"
                                onclick="handleNoDomain(this,
                                                    '<%=Encode.forJavaScriptAttribute(Encode.forUriComponent(name))%>',
                                                    '<%=Encode.forJavaScriptAttribute(Encode.forUriComponent(type))%>')"
                            >
                            <img
                                class="ui image"
                                src="libs/themes/wso2is/assets/images/identity-providers/github-idp-illustration.svg"
                                alt="Github sign-up logo"
                                role="presentation">
                            <span><%=IdentityManagementEndpointUtil.i18n(recoveryResourceBundle, "continue.with")%> <%=Encode.forHtmlContent(displayName)%></span>
                            </button>
                        </div>
                    </div>
                    <br>
                    <%
                        } else if (StringUtils.equals(type,FACEBOOK_AUTHENTICATOR)) {
                    %>
                    <div class="social-login blurring social-dimmer">
                        <div class="field">
                            <button type="button"
                                class="ui button"
                                data-testid="sign-up-with-facebook"
                                onclick="handleNoDomain(this,
                                                    '<%=Encode.forJavaScriptAttribute(Encode.forUriComponent(name))%>',
                                                    '<%=Encode.forJavaScriptAttribute(Encode.forUriComponent(type))%>')"
                            >
                            <img
                                class="ui image"
                                src="libs/themes/wso2is/assets/images/identity-providers/facebook-idp-illustration.svg"
                                alt="Facebook sign-up logo"
                                role="presentation">
                            <span><%=IdentityManagementEndpointUtil.i18n(recoveryResourceBundle, "continue.with")%> <%=Encode.forHtmlContent(displayName)%></span>
                            </button>
                        </div>
                    </div>
                    <br>
                    <% } else if (!StringUtils.equals(type, SSO_AUTHENTICATOR)) {

                        String logoPath = imageURL;
                        if (!imageURL.isEmpty() && imageURL.contains("/")) {
                            String[] imageURLSegements = imageURL.split("/");
                            String logoFileName = imageURLSegements[imageURLSegements.length - 1];

                            logoPath = "libs/themes/default/assets/images/identity-providers/" + logoFileName;
                        }
                    %>
                    <div class="social-login blurring social-dimmer">
                        <div class="field">
                            <button
                                type="button"
                                class="ui button"
                                data-testid='sign-up-with-<%=Encode.forHtmlContent(name)%>'
                                onclick="handleNoDomain(this,
                                    '<%=Encode.forJavaScriptAttribute(Encode.forUriComponent(name))%>',
                                    '<%=Encode.forJavaScriptAttribute(Encode.forUriComponent(type))%>')"
                                >
                                    <img
                                        role="presentation"
                                        alt="sign-up-with-<%=Encode.forHtmlContent(name)%> logo"
                                        class="ui image"
                                        src="<%=Encode.forHtmlAttribute(logoPath)%>">
                                    <span><%=IdentityManagementEndpointUtil.i18n(recoveryResourceBundle, "continue.with")%> <%=Encode.forHtmlContent(displayName)%></span>
                            </button>
                        </div>
                    </div>
                    <br>
                    <%
                        }
                    }
                    %>
                    <div style="text-align: left;">
                        <%--Terms/Privacy Policy--%>
                        <%
                            if (StringUtils.isNotBlank(termsOfUseURL) && StringUtils.isNotBlank(privacyPolicyURL)) {
                        %>
                        <p class="mt-2 mb-0 left privacy">When you continue, you are agreeing to our
                            <a href="<%= StringEscapeUtils.escapeHtml4(termsOfUseURL) %>" target="_blank"
                            data-testid="registration-form-tos-link"
                            rel="noopener noreferrer">
                                <%=IdentityManagementEndpointUtil.i18n(recoveryResourceBundle, "toc")%></a>
                            <%=IdentityManagementEndpointUtil.i18n(recoveryResourceBundle, "and")%>
                            <a href="<%= StringEscapeUtils.escapeHtml4(privacyPolicyURL) %>" target="_blank"
                            data-testid="registration-form-privacy-link"
                            rel="noopener noreferrer">
                                <%=IdentityManagementEndpointUtil.i18n(recoveryResourceBundle, "Privacy.policy")%></a>
                        </p>
                        <%
                            } else if (StringUtils.isNotBlank(termsOfUseURL)) {
                        %>
                        <p class="mt-2 mb-0 left privacy">When you continue, you are agreeing to our
                            <a href="<%= StringEscapeUtils.escapeHtml4(termsOfUseURL) %>" target="_blank"
                                data-testid="registration-form-tos-link" rel="noopener noreferrer"
                            >
                                <%=IdentityManagementEndpointUtil.i18n(recoveryResourceBundle, "toc")%>
                            </a>
                        </p>
                        <%
                            } else if (StringUtils.isNotBlank(privacyPolicyURL)) {
                        %>
                        <p class="mt-2 mb-0 left privacy">When you continue, you are agreeing to our
                            <a href="<%= StringEscapeUtils.escapeHtml4(privacyPolicyURL) %>" target="_blank"
                                data-testid="registration-form-privacy-link" rel="noopener noreferrer"
                            >
                                <%=IdentityManagementEndpointUtil.i18n(recoveryResourceBundle, "Privacy.policy")%>
                            </a>
                        </p>
                        <%
                            }
                        %>
                        <%--End Terms/Privacy Policy--%>

                        <div class="ui divider hidden"></div>
                        <%
                            if (!StringUtils.equalsIgnoreCase(backToUrl,"null") && !StringUtils.isBlank(backToUrl)) {
                        %>
                        <div class="buttons mt-2">
                            <div class="field external-link-container text-small">
                                <%=IdentityManagementEndpointUtil.i18n(recoveryResourceBundle,
                                        "Already.have.an.account")%>
                                <a href="<%=backToUrl%>">
                                    <%=IdentityManagementEndpointUtil.i18n(recoveryResourceBundle, "Sign.in")%>
                                </a>
                            </div>
                        </div>
                        <%
                            }
                        %>
                    </div>
                </div>

                <!-- validation -->
                <div class="ui negative message" id="error-msg" hidden="hidden"></div>
                <div class="segment-form" id="basic-form" hidden="hidden">
                    <form class="ui large form" action="processregistration.do" method="post" id="register" novalidate>
                        <div id="alphanumericUsernameField" class="field required" hidden="hidden">
                            <input id="isSaaSApp" name="isSaaSApp" type="hidden"value="<%=isSaaSApp%>">
                        <% if (isPasswordProvisionEnabled || !skipSignUpEnableCheck) { %>
                            <div class="ui divider hidden"></div>
                            <label><%=IdentityManagementEndpointUtil.i18n(recoveryResourceBundle, "Username")%></label>
                            <div class="ui fluid left icon input">
                                <input
                                    type="text"
                                    id="alphanumericUsernameUserInput"
                                    value=""
                                    name="usernameInput"
                                    tabindex="1"
                                    placeholder="<%=IdentityManagementEndpointUtil.i18n(recoveryResourceBundle, "enter.your.username")%>"
                                    data-testid="self-register-page-username-input"
                                    autocomplete="off"
                                    required
                                />
                                <i aria-hidden="true" class="user outline icon"></i>
                            </div>
                            <div class="mt-1" id="alphanumeric-username-error-msg" hidden="hidden">
                                <div class="ui grid">
                                    <div class="one wide column">
                                        <i class="red exclamation circle icon"></i>
                                    </div>
                                    <div class="fourteen wide column validation-error-message" id="alphanumeric-username-error-msg-text"></div>
                                </div>
                            </div>
                            <div class="mt-1 password-policy-description" id="alphanumeric-username-msg">
                                <div class="ui grid">
                                    <div class="one wide column">
                                        <i class="info circle icon" data-variation="inverted"></i>
                                    </div>
                                    <div class="fourteen wide column" id="alphanumeric-username-msg-text"></div>
                                </div>
                            </div>
                        </div>
                        <input id="username" name="username" type="hidden"
                            <% if(skipSignUpEnableCheck) {%> value="<%=Encode.forHtmlAttribute(username)%>" <%}%>>
                        <% if (emailPII != null) { %>
                        <div id="usernameField"
                            <%if (isSelfRegistrationLockOnCreationEnabled || emailPII.getRequired() || !isAlphanumericUsernameEnabled) { %>
                                class="field required"
                            <%} else { %>
                                class="field"
                            <%}%>>
                            <div class="ui divider hidden"></div>
                            <label><%=IdentityManagementEndpointUtil.i18n(recoveryResourceBundle, "Email")%></label>
                            <div class="ui fluid left icon input">
                                <input
                                    type="email"
                                    id="usernameUserInput"
                                    value=""
                                    name="http://wso2.org/claims/emailaddress"
                                    tabindex="1"
                                    placeholder="<%=IdentityManagementEndpointUtil.i18n(recoveryResourceBundle, "enter.your.email")%>"
                                    data-testid="self-register-page-username-input"
                                    autocomplete="off"
                                    <%if (emailPII.getRequired() || !isAlphanumericUsernameEnabled || isSelfRegistrationLockOnCreationEnabled) {%> required <%}%>
                                />
                                <i aria-hidden="true" class="envelope outline icon"></i>
                            </div>
                            <div class="mt-1" id="username-error-msg" hidden="hidden">
                                <div class="ui grid">
                                    <div class="one wide column">
                                        <i class="red exclamation circle icon"></i>
                                    </div>
                                    <div class="fourteen wide column validation-error-message" id="username-error-msg-text"></div>
                                </div>
                            </div>
                            <div class="ui divider hidden"></div>
                        </div>
                        <% } %>
                        <div id="passwordField" class="field required">
                            <label><%=IdentityManagementEndpointUtil.i18n(recoveryResourceBundle, "Password")%></label>
                            <div class="ui fluid left icon input addon-wrapper">
                                <input
                                    type="password"
                                    id="passwordUserInput"
                                    value=""
                                    name="passwordUserInput"
                                    tabindex="1"
                                    placeholder="<%=IdentityManagementEndpointUtil.i18n(recoveryResourceBundle, "enter.your.password")%>"
                                    data-testid="self-register-page-password-input"
                                    autocomplete="new-password"
                                    required
                                />
                                <i aria-hidden="true" class="lock icon"></i>
                                <i id="password-eye" class="eye icon right-align password-toggle slash" onclick="showPassword()"></i>
                                <input id="password" name="password" type="hidden"
                                    <% if(skipSignUpEnableCheck) {%> value="<%=Encode.forHtmlAttribute(username)%>" <%}%>>
                            </div>
                            <div class="mt-1" id="password-error-msg" hidden="hidden">
                                <div class="ui grid">
                                    <div class="one wide column">
                                        <i class="red exclamation circle icon"></i>
                                    </div>
                                    <div class="fourteen wide column validation-error-message" id="password-error-msg-text"></div>
                                </div>
                            </div>
                        </div>
                        <input name="previous_step" type="hidden" id="previous_step" />

                        <% Map<String, String[]> requestMap = request.getParameterMap();
                            for (Map.Entry<String, String[]> entry : requestMap.entrySet()) {
                                String key = Encode.forHtmlAttribute(entry.getKey());
                                String value = Encode.forHtmlAttribute(entry.getValue()[0]);
                                if (StringUtils.equalsIgnoreCase("reCaptcha", key)) {
                                    continue;
                                } %>
                        <div class="field">
                            <input id="<%= key%>" name="<%= key%>" type="hidden"
                                   value="<%=value%>" class="form-control">
                        </div>
                        <% } %>
                        <div id="password-validation-block">
                            <div id="length-block" class="password-policy-description mb-2" style="display: none;">
                                <i id="password-validation-neutral-length" class="inverted grey circle icon"></i>
                                <i id="password-validation-cross-length" style="display: none;" class="red times circle icon"></i>
                                <i id="password-validation-check-length" style="display: none;" class="green check circle icon"></i>
                                <p id="length" class="pl-4">
                                    <%=IdentityManagementEndpointUtil.i18n(recoveryResourceBundle, "more.than.8.chars")%>
                                </p>
                            </div>
                            <div id="case-block" class="password-policy-description mb-2" style="display: none;">
                                <i id="password-validation-neutral-case" class="inverted grey circle icon"></i>
                                <i id="password-validation-cross-case" style="display: none;" class="red times circle icon"></i>
                                <i id="password-validation-check-case" style="display: none;" class="green check circle icon"></i>
                                <p id="case" class="pl-4">
                                    <%=IdentityManagementEndpointUtil.i18n(recoveryResourceBundle, "lowercase.and.uppercase.letter")%>
                                </p>
                            </div>
                            <div id="number-block" class="password-policy-description mb-2" style="display: none;">
                                <i id="password-validation-neutral-number" class="inverted grey circle icon"></i>
                                <i id="password-validation-cross-number" style="display: none;" class="red times circle icon"></i>
                                <i id="password-validation-check-number" style="display: none;" class="green check circle icon"></i>
                                <p id="number" class="pl-4">
                                    <%=IdentityManagementEndpointUtil.i18n(recoveryResourceBundle, "at.least.one.number")%>
                                </p>
                            </div>
                            <div id="special-chr-block" class="password-policy-description mb-2" style="display: none;">
                                <i id="password-validation-neutral-special-chr" class="inverted grey circle icon"></i>
                                <i id="password-validation-cross-special-chr" style="display: none;" class="red times circle icon"></i>
                                <i id="password-validation-check-special-chr" style="display: none;" class="green check circle icon"></i>
                                <p id="special-chr" class="pl-4"><%=IdentityManagementEndpointUtil.i18n(recoveryResourceBundle, "at.least.one.special.char")%></p>
                            </div>
                            <div id="unique-chr-block" class="password-policy-description mb-2" style="display: none;">
                                <i id="password-validation-neutral-unique-chr" class="inverted grey circle icon"></i>
                                <i id="password-validation-cross-unique-chr" style="display: none;" class="red times circle icon"></i>
                                <i id="password-validation-check-unique-chr" style="display: none;" class="green check circle icon"></i>
                                <p id="unique-chr" class="pl-4">
                                    <%=IdentityManagementEndpointUtil.i18n(recoveryResourceBundle, "at.least.one.unique.char")%>
                                </p>
                            </div>
                            <div id="repeated-chr-block" class="password-policy-description mb-2" style="display: none;">
                                <i id="password-validation-neutral-repeated-chr" class="inverted grey circle icon"></i>
                                <i id="password-validation-cross-repeated-chr" style="display: none;" class="red times circle icon"></i>
                                <i id="password-validation-check-repeated-chr" style="display: none;" class="green check circle icon"></i>
                                <p id="repeated-chr" class="pl-4">
                                    <%=IdentityManagementEndpointUtil.i18n(recoveryResourceBundle, "no.more.than.one.repeated.char")%>
                                </p>
                            </div>
                        </div>

                        <%-- Add claims sections --%>
                            <div>
                                <%
                                    Claim firstNamePII =
                                        uniquePIIs.get(IdentityManagementEndpointConstants.ClaimURIs.FIRST_NAME_CLAIM);
                                    Claim lastNamePII =
                                        uniquePIIs.get(IdentityManagementEndpointConstants.ClaimURIs.LAST_NAME_CLAIM);
                                    if (firstNamePII != null) {
                                        String firstNameValue = request.getParameter(IdentityManagementEndpointConstants.ClaimURIs.FIRST_NAME_CLAIM);
                                %>
                                    <div <% if (lastNamePII != null) { %> class="two fields mb-0" <%} %> >
                                        <div id="firstNameField"
                                        <% if (firstNamePII.getRequired()) { %> class="field form-group required" <%}
                                                else {%> class="field"<%}%>>
                                            <label class="control-label"><%=IdentityManagementEndpointUtil.i18n(recoveryResourceBundle, "First.name")%>
                                            </label>
                                            <input id="firstNameUserInput" type="text" name="http://wso2.org/claims/givenname" class="form-control"
                                                <% if (firstNamePII.getRequired()) {%> required <%}%>
                                                <% if (skipSignUpEnableCheck && StringUtils.isNotEmpty(firstNameValue)) { %>
                                                value="<%= Encode.forHtmlAttribute(firstNameValue)%>" disabled <% } %>
                                                placeholder="<%=IdentityManagementEndpointUtil.i18n(recoveryResourceBundle, "First.name")%>"/>
                                                <div class="mt-1" id="firstname-error-msg" hidden="hidden">
                                                    <div class="ui grid">
                                                        <div class="one wide column">
                                                            <i class="red exclamation circle icon"></i>
                                                        </div>
                                                        <div class="ten wide column validation-error-message" id="firstname-error-msg-text"></div>
                                                    </div>
                                                </div>
                                        </div>
                                        <%}%>

                                        <%
                                            if (lastNamePII != null) {
                                                String lastNameValue =
                                                        request.getParameter(IdentityManagementEndpointConstants.ClaimURIs.LAST_NAME_CLAIM);
                                        %>

                                        <div id="lastNameField"
                                            <% if (lastNamePII.getRequired()) { %> class="field form-group required" <% }
                                                else { %> class="field form-group"<% } %>>
                                            <label class="control-label"><%=IdentityManagementEndpointUtil.i18n(recoveryResourceBundle, "Last.name")%>
                                            </label>
                                            <input id="lastNameUserInput" type="text" name="http://wso2.org/claims/lastname" class="form-control"
                                                <% if (lastNamePII.getRequired()) {%> required <%}%>
                                                <% if (skipSignUpEnableCheck && StringUtils.isNotEmpty(lastNameValue)) { %>
                                                value="<%= Encode.forHtmlAttribute(lastNameValue)%>" disabled <% } %>
                                                placeholder="<%=IdentityManagementEndpointUtil.i18n(recoveryResourceBundle, "Last.name")%>"
                                            />
                                            <div class="mt-1" id="lastname-error-msg" hidden="hidden">
                                                <div class="ui grid">
                                                    <div class="one wide column">
                                                        <i class="red exclamation circle icon"></i>
                                                    </div>
                                                    <div class="ten wide column validation-error-message" id="lastname-error-msg-text"></div>
                                                </div>
                                            </div>
                                        </div>
                                    </div>
                                    <div class="mb-3"></div>

                                <%}%>
                                <%
                                    if (callback != null) {
                                %>
                                <% for (int index = 0; index < missingClaimList.length; index++) {
                                    String claim = missingClaimList[index];
                                    String claimDisplayName = missingClaimDisplayName[index];
                                    if (!StringUtils
                                            .equals(claim, IdentityManagementEndpointConstants.ClaimURIs.FIRST_NAME_CLAIM)
                                            && !StringUtils
                                            .equals(claim, IdentityManagementEndpointConstants.ClaimURIs.LAST_NAME_CLAIM)
                                            && !StringUtils
                                            .equals(claim, IdentityManagementEndpointConstants.ClaimURIs.EMAIL_CLAIM)) {
                                %>
                                <div class="required field">
                                    <input type="text" name="missing-<%=Encode.forHtmlAttribute(claim)%>"
                                        id="<%=Encode.forHtmlAttribute(claim)%>" class="form-control"
                                        onblur="showFieldValidationStatus(this)"
                                        oninput="hideFieldValidationStatus(this)"
                                        required="required" placeholder=<%=IdentityManagementEndpointUtil.i18nBase64(recoveryResourceBundle, claimDisplayName)%>>
                                </div>
                                <% }
                                }%>
                                <%
                                    }
                                    List<String> missingClaims = null;
                                    if (ArrayUtils.isNotEmpty(missingClaimList)) {
                                        missingClaims = Arrays.asList(missingClaimList);
                                    }
                                    for (Claim claim : claims) {
                                        if ((CollectionUtils.isEmpty(missingClaims) || !missingClaims.contains(claim.getUri())) &&
                                                !StringUtils.equals(claim.getUri(), IdentityManagementEndpointConstants.ClaimURIs.FIRST_NAME_CLAIM) &&
                                                !StringUtils.equals(claim.getUri(), IdentityManagementEndpointConstants.ClaimURIs.LAST_NAME_CLAIM) &&
                                                !StringUtils.equals(claim.getUri(), IdentityManagementEndpointConstants.ClaimURIs.EMAIL_CLAIM) &&
                                                !StringUtils.equals(claim.getUri(), IdentityManagementEndpointConstants.ClaimURIs.CHALLENGE_QUESTION_URI_CLAIM) &&
                                                !StringUtils.equals(claim.getUri(), IdentityManagementEndpointConstants.ClaimURIs.CHALLENGE_QUESTION_1_CLAIM) &&
                                                !StringUtils.equals(claim.getUri(), IdentityManagementEndpointConstants.ClaimURIs.CHALLENGE_QUESTION_2_CLAIM) &&
                                                !StringUtils.equals(claim.getUri(), "http://wso2.org/claims/groups") &&
                                                !StringUtils.equals(claim.getUri(), "http://wso2.org/claims/role") &&
                                                !StringUtils.equals(claim.getUri(), "http://wso2.org/claims/url") &&
                                                !(claim.getReadOnly() != null ? claim.getReadOnly() : false)) {
                                            String claimURI = claim.getUri();
                                            String claimValue = request.getParameter(claimURI);
                                            String[] claimFields = claimURI.split("/");
                                            String claimName = claimFields[claimFields.length-1];
                                            String claimFieldID = claimName + "_field";
                                            String claimErrorMsg = claimName + "_error";
                                            String claimErrorMsgText = claimName + "_error_text";
                                %>
                                    <div  id= "<%=IdentityManagementEndpointUtil.i18nBase64(recoveryResourceBundle, claimFieldID)%>"
                                        <% if (claim.getRequired()) { %> class="field form-group required" <%} else {%> class="field"<%}%>  >
                                        <label class="control-label">
                                            <%=IdentityManagementEndpointUtil.i18nBase64(recoveryResourceBundle, claim.getDisplayName())%>
                                        </label>
                                    <% if(StringUtils.equals(claim.getUri(), "http://wso2.org/claims/country")) {%>
                                        <div class="ui fluid search selection dropdown"  id="country-dropdown"
                                            data-testid="country-dropdown">
                                            <input type="hidden"
                                                id="country"
                                                name="<%= Encode.forHtmlAttribute(claimURI) %>"
                                                <% if (claim.getRequired()) { %>
                                                    required
                                                <% }%>
                                                <% if(skipSignUpEnableCheck && StringUtils.isNotEmpty(claimValue)) {%>
                                                    value="<%= Encode.forHtmlAttribute(claimValue)%>" disabled<%}%>
                                            />
                                            <i class="dropdown icon"></i>
                                            <div class="default text">
                                                <%=IdentityManagementEndpointUtil.i18n(recoveryResourceBundle, "enter.country")%>
                                            </div>
                                            <div class="menu">
                                                <c:forEach items="<%=getCountryList()%>" var="country">
                                                    <div class="item" data-value="${country.value}">
                                                        <i class="${country.key.toLowerCase()} flag"></i>
                                                            ${country.value}
                                                    </div>
                                                </c:forEach>
                                            </div>
                                        </div>
                                    <% } else if (StringUtils.equals(claim.getUri(), "http://wso2.org/claims/dob")) { %>
                                        <div class="ui calendar" id="date_picker">
                                            <div class="ui input right icon" style="width: 100%;">
                                                <i class="calendar icon"></i>
                                                <input type="text"
                                                        autocomplete="off"
                                                        name="<%= Encode.forHtmlAttribute(claimURI) %>"
                                                        id="birthOfDate"
                                                        <% if (claim.getRequired()) { %>
                                                            required
                                                        <% } %>
                                                        placeholder="<%=IdentityManagementEndpointUtil.i18n(recoveryResourceBundle, "enter.birth.date")%>"
                                                    <% if(skipSignUpEnableCheck && StringUtils.isNotEmpty(claimValue)) {%>
                                                        value="<%= Encode.forHtmlAttribute(claimValue)%>" disabled<%}%>
                                                />
                                            </div>
                                        </div>
                                    <% } else { %>
                                            <input type="text" name="<%= Encode.forHtmlAttribute(claimURI) %>"
                                                class="form-control"
                                                onblur="showFieldValidationStatus(this)"
                                                oninput="hideFieldValidationStatus(this)"
                                                <% if (claim.getValidationRegex() != null) { %>
                                                pattern="<%= Encode.forHtmlContent(claim.getValidationRegex()) %>"
                                                <% } %>
                                                <% if (claim.getRequired()) { %>
                                                    required
                                                <% } %>
                                                <% if (StringUtils.equals(claim.getUri(), "http://wso2.org/claims/mobile")) { %>
                                                    id="mobileNumber"
                                            <% }%>
                                                placeholder="<%=IdentityManagementEndpointUtil.i18n(recoveryResourceBundle, "enter")%> <%=IdentityManagementEndpointUtil.i18nBase64(recoveryResourceBundle, claim.getDisplayName())%>"
                                                <% if(skipSignUpEnableCheck && StringUtils.isNotEmpty(claimValue)) {%>
                                                value="<%= Encode.forHtmlAttribute(claimValue)%>" disabled<%}%>
                                            />
                                    <% } %>
                                    <div class="mt-1" id="<%=IdentityManagementEndpointUtil.i18nBase64(recoveryResourceBundle, claimErrorMsg)%>" hidden="hidden">
                                        <div class="ui grid">
                                            <div class="one wide column">
                                                <i class="red exclamation circle icon"></i>
                                            </div>
                                            <div class="fourteen wide column validation-error-message"
                                            id="<%=IdentityManagementEndpointUtil.i18nBase64(recoveryResourceBundle, claimErrorMsgText)%>"></div>
                                        </div>
                                    </div>
                                    </div>
                                <%
                                    }
                                }
                                %>

                            </div>
                            <% } else { %>
                            <div>
                                <div class="field">
                                    <label class="control-label">
                                        <%=IdentityManagementEndpointUtil.i18n(recoveryResourceBundle, "username")%>
                                    </label>
                                    <input type="text" class="form-control"
                                        value="<%=Encode.forHtmlAttribute(username)%>" disabled>
                                </div>
                                <%
                                    for (Claim claim : claims) {
                                        String claimUri = claim.getUri();
                                        String claimValue = request.getParameter(claimUri);

                                        if (StringUtils.isNotEmpty(claimValue)) { %>
                                <div class="field">
                                    <label class="control-label">
                                        <%=IdentityManagementEndpointUtil.i18nBase64(recoveryResourceBundle, claim.getDisplayName())%>
                                    </label>
                                    <input type="text" class="form-control"
                                        value="<%= Encode.forHtmlAttribute(claimValue)%>" disabled>
                                </div>
                                <% }
                                }%>
                            </div>
                            <% } %>
                            <% if (skipSignUpEnableCheck) { %>
                            <div class="field">
                                <input type="hidden" name="sessionDataKey" value='<%=Encode.forHtmlAttribute
                                        (request.getParameter("sessionDataKey"))%>'/>
                            </div>
                            <div class="field">
                                <input type="hidden" name="policy" value='<%=Encode.forHtmlAttribute
                                        (IdentityManagementServiceUtil.getInstance().getServiceContextURL().replace("/services",
                                        "/authenticationendpoint/privacy_policy.do"))%>'/>
                            </div>
                            <% }

                                if (hasPurposes) {
                            %>
                            <div class="ui divider hidden"></div>
                            <div class="ui secondary left aligned segment">
                                <p>
                                    <%=IdentityManagementEndpointUtil.i18n(recoveryResourceBundle,
                                        "Need.consent.for.following.purposes")%>
                                    <span>
                                        <%=IdentityManagementEndpointUtil.i18n(recoveryResourceBundle,
                                            "I.consent.to.use.them")%>
                                    </span>
                                    <%
                                        if (consentDisplayType == "template") {
                                    %>
                                    <%--User Consents from Template--%>
                                        <div class="consent-statement"></div>
                                    <%--End User Consents from Template--%>
                                    <% } else if (consentDisplayType == "tree") { %>
                                    <%--User Consents Tree--%>
                                        <div id="tree-table"></div>
                                    <%--End User Consents Tree--%>
                                    <%
                                    } else if (consentDisplayType == "row") {
                                    %>
                                    <%--User Consents Row--%>
                                        <div id="row-container"></div>
                                    <%--End User Consents Row--%>
                                    <%
                                        }
                                    %>
                                </p>
                            </div>
                            <%
                                }
                            %>
                            <div class="field">
                                <%
                                    if (reCaptchaEnabled) {
                                        String reCaptchaKey = CaptchaUtil.reCaptchaSiteKey();
                                %>
                                <div class="ui divider hidden"></div>
                                <div class="field">
                                    <div class="g-recaptcha"
                                        data-sitekey="<%=Encode.forHtmlAttribute(reCaptchaKey)%>"
                                        data-theme="light"
                                        data-bind="registrationSubmit"
                                        data-callback="submitForm"
                                    >
                                    </div>
                                </div>
                                <%
                                    }
                                %>
                                <div class="ui divider hidden"></div>
                                <%
                                if (!isFederated) {
                                %>
                                    <%--Terms/Privacy Policy--%>
                                    <%
                                        if (StringUtils.isNotBlank(termsOfUseURL) && StringUtils.isNotBlank(privacyPolicyURL)) {
                                    %>
                                    <p class="mt-5 mb-0 privacy">
                                        <%=IdentityManagementEndpointUtil.i18n(recoveryResourceBundle, "when.you.click.sign.up.you.are.agreeing")%>
                                        <a href="<%= StringEscapeUtils.escapeHtml4(termsOfUseURL) %>" target="_blank"
                                            data-testid="registration-form-tos-link" rel="noopener noreferrer"
                                        >
                                            <%=IdentityManagementEndpointUtil.i18n(recoveryResourceBundle, "toc")%>
                                        </a>
                                        <%=IdentityManagementEndpointUtil.i18n(recoveryResourceBundle, "and")%>
                                        <a href="<%= StringEscapeUtils.escapeHtml4(privacyPolicyURL) %>" target="_blank"
                                            data-testid="registration-form-privacy-link" rel="noopener noreferrer"
                                        >
                                            <%=IdentityManagementEndpointUtil.i18n(recoveryResourceBundle, "Privacy.policy")%>
                                        </a>
                                    </p>
                                    <%
                                        } else if (StringUtils.isNotBlank(termsOfUseURL)) {
                                    %>
                                    <p class="mt-5 mb-0 privacy">
                                        <%=IdentityManagementEndpointUtil.i18n(recoveryResourceBundle, "when.you.click.sign.up.you.are.agreeing")%>
                                        <a href="<%= StringEscapeUtils.escapeHtml4(termsOfUseURL) %>" target="_blank"
                                            data-testid="registration-form-tos-link" rel="noopener noreferrer"
                                        >
                                            <%=IdentityManagementEndpointUtil.i18n(recoveryResourceBundle, "toc")%>
                                        </a>
                                    </p>
                                    <%
                                        } else if (StringUtils.isNotBlank(privacyPolicyURL)) {
                                    %>
                                    <p class="mt-5 mb-0 privacy">
                                        <%=IdentityManagementEndpointUtil.i18n(recoveryResourceBundle, "when.you.click.sign.up.you.are.agreeing")%>
                                        <a href="<%= StringEscapeUtils.escapeHtml4(privacyPolicyURL) %>" target="_blank"
                                            data-testid="registration-form-privacy-link" rel="noopener noreferrer"
                                        >
                                            <%=IdentityManagementEndpointUtil.i18n(recoveryResourceBundle, "Privacy.policy")%>
                                        </a>
                                    </p>
                                    <%
                                        }
                                    %>
                                    <%--End Terms/Privacy Policy--%>
                                <%
                                }
                                %>
                                <div class="field">
                                    <input id="isSelfRegistrationWithVerification" type="hidden"
                                        name="isSelfRegistrationWithVerification"
                                        value="true"/>
                                    <%
                                        if (!IdentityTenantUtil.isTenantQualifiedUrlsEnabled()) {
                                    %>
                                    <input id="tenantDomain" name="tenantDomain" type="hidden"
                                        value="<%= Encode.forHtmlAttribute(tenantDomain) %>"/>
                                    <%
                                        }
                                    %>
                                    <%
                                        if (isSaaSApp) {
                                    %>
                                    <input id="srtenantDomain" name="srtenantDomain" type="hidden"
                                        value="<%=Encode.forHtmlAttribute(tenantDomain)%>"/>
                                    <%
                                        }
                                    %>
                                    <%
                                        if (StringUtils.isNotBlank(sp)) {
                                    %>
                                    <input id="sp" name="sp" type="hidden" value="<%=sp%>"/>
                                    <%
                                        }
                                    %>
                                </div>

                            </div>
                        <div class="buttons mt-4">
                            <button id="registrationSubmit" class="ui primary button large fluid" type="submit">
                                <%= i18n(recoveryResourceBundle, customText, "sign.up.button") %>
                            </button>
                        </div>
                        <div class="ui divider hidden"></div>
                        <%
                            if (!StringUtils.equalsIgnoreCase(backToUrl,"null") && !StringUtils.isBlank(backToUrl)) {
                        %>
                        <div class="buttons mt-2">
                            <div class="field external-link-container text-small">
                                <%=IdentityManagementEndpointUtil.i18n(recoveryResourceBundle,
                                        "Already.have.an.account")%>
                                <a href="<%=backToUrl%>">
                                    <%=IdentityManagementEndpointUtil.i18n(recoveryResourceBundle, "Sign.in")%>
                                </a>
                            </div>
                        </div>
                        <%
                            }
                        %>
                    </form>
                </div>
            </div>
        </layout:component>
        <layout:component componentName="ProductFooter">
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

    <%-- footer --%>
    <%
        File footerFile = new File(getServletContext().getRealPath("extensions/footer.jsp"));
        if (footerFile.exists()) {
    %>
        <jsp:include page="extensions/footer.jsp"/>
    <% } else { %>
        <jsp:include page="includes/footer.jsp"/>
    <% } %>

    <script>
        const ALPHANUMERIC_USERNAME_REGEX = /^(?=.*[a-zA-Z])[a-zA-Z0-9]+$/;
        const USERNAME_WITH_SPECIAL_CHARS_REGEX = /^(?=.*[a-zA-Z])[a-zA-Z0-9!@#$%&'*+\\=?^_`.{|}~-]+$/;
        var registrationDataKey = "registrationData";
        var passwordField = $("#passwordUserInput");
        var $registerForm = $("#register");
        var validUsername = false;
        var validPassword = false;
        var passwordConfig = <%=passwordConfig%>;
        var usernameConfig = <%=usernameConfig%>;
        var lowerCaseLetters = /[a-z]/g;
        var upperCaseLetters = /[A-Z]/g;
        var numbers = /[0-9]/g;
        var specialChr = /[!#$%&'()*+,\-\.\/:;<=>?@[\]^_{|}~]/g;
        var consecutiveChr = /([^])\1+/g;
        var errorMessage = getErrorMessage();

        if (passwordConfig.minLength> 0 || passwordConfig.maxLength > 0) {
            document.getElementById("length").innerHTML = '<%=IdentityManagementEndpointUtil.i18n(recoveryResourceBundle, "must.be.between")%>' +
                " " + (passwordConfig.minLength ?? 8) +
                " " + '<%=IdentityManagementEndpointUtil.i18n(recoveryResourceBundle, "and")%>' +
                    " " + (passwordConfig.maxLength ?? 30) + " " +
            '<%=IdentityManagementEndpointUtil.i18n(recoveryResourceBundle, "characters")%>';
            $("#length-block").css("display", "block");
        }
        if (passwordConfig.minNumber > 0) {
            document.getElementById("number").innerHTML = '<%=IdentityManagementEndpointUtil.i18n(recoveryResourceBundle, "at.least")%>'
                + " " + passwordConfig.minNumber + " "
                + '<%=IdentityManagementEndpointUtil.i18n(recoveryResourceBundle, "numbers")%>';
            $("#number-block").css("display", "block");
        }
        if ((passwordConfig.minUpperCase > 0) || passwordConfig.minLowerCase > 0) {
            let cases = [];
            if (passwordConfig.minUpperCase > 0) {
                cases.push(passwordConfig.minUpperCase + " "
                    + '<%=IdentityManagementEndpointUtil.i18n(recoveryResourceBundle, "uppercase")%>');
            }
            if (passwordConfig.minLowerCase > 0) {
                cases.push(passwordConfig.minLowerCase + " "
                    + '<%=IdentityManagementEndpointUtil.i18n(recoveryResourceBundle, "lowercase")%>');
            }
            document.getElementById("case").innerHTML = '<%=IdentityManagementEndpointUtil.i18n(recoveryResourceBundle, "at.least")%>'
                + " " + (cases.length > 1
                    ? cases.join(" " + '<%=IdentityManagementEndpointUtil.i18n(recoveryResourceBundle, "and")%>' +  " ")
                    : cases[0]) + " " + '<%=IdentityManagementEndpointUtil.i18n(recoveryResourceBundle, "character.s")%>';
            $("#case-block").css("display", "block");
        }
        if (passwordConfig.minSpecialChr > 0) {
            document.getElementById("special-chr").innerHTML = '<%=IdentityManagementEndpointUtil.i18n(recoveryResourceBundle, "at.least")%>'
                + " " + passwordConfig.minSpecialChr + " "
                + '<%=IdentityManagementEndpointUtil.i18n(recoveryResourceBundle, "special.characters")%>';
            $("#special-chr-block").css("display", "block");
        }
        if (passwordConfig.minUniqueChr > 0) {
            document.getElementById("unique-chr").innerHTML = '<%=IdentityManagementEndpointUtil.i18n(recoveryResourceBundle, "at.least")%>'
                + " " + passwordConfig.minUniqueChr + " "
                + '<%=IdentityManagementEndpointUtil.i18n(recoveryResourceBundle, "unique.characters")%>';
            $("#unique-chr-block").css("display", "block");
        }
        if (passwordConfig.maxConsecutiveChr > 0) {
            document.getElementById("repeated-chr").innerHTML = '<%=IdentityManagementEndpointUtil.i18n(recoveryResourceBundle, "no.more.than")%>'
                + " " + passwordConfig.maxConsecutiveChr + " "
                + '<%=IdentityManagementEndpointUtil.i18n(recoveryResourceBundle, "repeated.characters")%>';
            $("#repeated-chr-block").css("display", "block");
        }

        // Prepare the alphanumeric username message text.
        var alphanumericUsernameText = $("#alphanumeric-username-msg-text");
        if (usernameConfig.enableSpecialCharacters) {
            alphanumericUsernameText.html(
                "<%=IdentityManagementEndpointUtil.i18n(recoveryResourceBundle, "must.be.between")%>"
                + " " + (usernameConfig?.minLength ?? 3) + " "
                + "<%=IdentityManagementEndpointUtil.i18n(recoveryResourceBundle, "to")%>"
                + " " + (usernameConfig.maxLength ?? 255) + " "
                + "<%=IdentityManagementEndpointUtil.i18n(recoveryResourceBundle, "characters.may.contain")%>"
            );
        } else {
            alphanumericUsernameText.text(
                "<%=IdentityManagementEndpointUtil.i18n(recoveryResourceBundle, "must.be.alphanumeric")%>"
                + " " + (usernameConfig?.minLength ?? 3) + " "
                + "<%=IdentityManagementEndpointUtil.i18n(recoveryResourceBundle, "to")%>"
                + " " + (usernameConfig.maxLength ?? 255) + " "
                + "<%=IdentityManagementEndpointUtil.i18n(recoveryResourceBundle, "characters.including.one.letter")%>"
            );
        }
        if (!<%=isUsernameValidationEnabled%>) {
            $("#alphanumeric-username-msg").hide();
        }

        // Check whether the alphanumeric username is enabled or disabled.
        function isAlphanumericUsernameEnabled() {
            return usernameConfig?.alphanumericFormatValidator == true;
        }

        // Show the alphanumeric username field only when alphanumeric usernames are allowed.
        if (isAlphanumericUsernameEnabled()) {
            $("#alphanumericUsernameField").show();
            document.getElementById("alphanumericUsernameUserInput").setAttribute("name", "alphanumericUsernameUserInput");
        }
        if (!<%=isUsernameValidationEnabled%>) {
            $("#alphanumericUsernameField").show();
        }

        // Reloads the page if the page is loaded by going back in history.
        // Fixes issues with Firefox.
        window.addEventListener( "pageshow", function ( event ) {
            var historyTraversal = event.persisted ||
                                ( typeof window.performance != "undefined" &&
                                    window.performance.navigation.type === 2 );

            if ( historyTraversal ) {
                if($registerForm){
                    $registerForm.data("submitted", false);
                }
            }
        });

        function goBack() {
            window.history.back();
        }

        // Fires when username field lose focus.
        $('#usernameUserInput').bind('blur', function () {
            showUsernameValidationStatus();
        });

        // Fires when username field lose focus.
        $('#alphanumericUsernameUserInput').bind('blur', function () {
            if (<%=isUsernameValidationEnabled%>) {
                showAlphanumericUsernameValidationStatus();
            } else {
            	showUsernameRegexValidationStatus();
            }
        });

        // Fires when password field lose focus.
        $('#passwordUserInput').bind('blur', function () {
            showPasswordValidationStatus();
        });

        // Fires when firstname field lose focus.
        $('#firstNameUserInput').bind('blur', function () {
            showFirstNameValidationStatus();
        });

        // Fires when lastname field lose focus.
        $('#lastNameUserInput').bind('blur', function () {
            showLastNameValidationStatus();
        });

        // Fires when mobile field lose focus.
        $('#mobileNumber').bind('blur', function () {
            showMobileNumberValidationStatus();
        });

        // Fires when country field lose focus.
        $('#country').bind('blur', function () {
            showCountryValidationStatus();
        });

        // Fires on username field input.
        $('#usernameUserInput').bind('input', function () {
            hideUsernameValidationStatus();
        });

        // Fires on password field input.
        $('#passwordUserInput').bind('input', function () {
            hidePasswordValidationStatus();
        });

        // Fires on firstname field input.
        $('#firstNameUserInput').bind('input', function () {
            hideFirstNameValidationStatus();
        });

        // Fires on lastname field input.
        $('#lastNameUserInput').bind('input', function () {
            hideLastNameValidationStatus();
        });

        // Fires on mobile field input.
        $('#mobileNumber').bind('input', function () {
            hideMobileNumberValidationStatus();
        });

        // Fires on country field input.
        $('#country').bind('input', function () {
            hideCountryValidationStatus();
        });

        // Handle form submission preventing double submission.
        $(document).ready(function(){

            passwordField.keyup(function() {
                ShowPasswordStatus();
            });

            passwordField.focusout(function() {
                displayPasswordCross();
            });

            <%
                if (error){
            %>
                var registrationData = sessionStorage.getItem(registrationDataKey);
                sessionStorage.removeItem(registrationDataKey);

                if (registrationData){
                    var fields = JSON.parse(registrationData);

                    if (fields.length > 0) {
                        fields.forEach(function(field) {
                            document.getElementsByName(field.name)[0].value = field.value;
                        })
                    }
                }
            <%
                }
            %>

            // Dynamically render the configured authenticators.
            var hasLocal = false;
            var hasFederated = false;
            var isBasicForm = true;
            try {
                var hasLocal=JSON.parse(<%=isLocal%>);
                var hasFederated = JSON.parse(<%=isFederated%>);
                var isBasicForm = JSON.parse(<%=isBasic%>);
            } catch(error) {
                // Do nothing.
            }

            if (hasLocal & hasFederated) {
                $("#continue-with-email").show();
                $("#federated-authenticators").show();
            } else if (hasFederated) {
                $("#federated-authenticators").show();
            } else {
                $("#continue-with-email").hide();
                $("#federated-authenticators").hide();
                if (hasLocal || isBasicForm) {
                    $("#basic-form").show();
                } else {
                    $("#basic-form").hide();
                }
            }

            var container;
            var allAttributes = [];
            var canSubmit;

            var agreementChk = $(".agreement-checkbox input");
            var countryDropdown = $("#country-dropdown");

            countryDropdown.dropdown('hide');
            $("> input.search", countryDropdown).attr("role", "presentation");

            $("#date_picker").calendar({
                type: 'date',
                formatter: {
                    date: function (date, settings) {
                        var EMPTY_STRING = "";
                        var DATE_SEPARATOR = "-";
                        var STRING_ZERO = "0";
                        if (!date) return EMPTY_STRING;
                            var day = date.getDate() + EMPTY_STRING;
                        if (day.length < 2) {
                            day = STRING_ZERO + day;
                        }
                        var month = (date.getMonth() + 1) + EMPTY_STRING;
                        if (month.length < 2) {
                            month = STRING_ZERO + month;
                        }
                        var year = date.getFullYear();
                        return year + DATE_SEPARATOR + month + DATE_SEPARATOR + day;
                    }
                }
            });

            $(".form-info").popup();

            $.fn.preventDoubleSubmission = function() {
                $(this).on("submit", function(e){
                    var $form = $(this);
                    if ($form.data("submitted") === true) {
                        // Previously submitted - don't submit again.
                        e.preventDefault();
                        console.warn("Prevented a possible double submit event");
                    } else {
                        e.preventDefault();

                        var validInput = true;
                        var userName = document.getElementById("username");
                        var alphanumericUsernameUserInput = document.getElementById("alphanumericUsernameUserInput");
                        var usernameUserInput = document.getElementById("usernameUserInput");
                        var password = document.getElementById("password");
                        var passwordUserInput = document.getElementById("passwordUserInput");
                        var unsafeCharPattern = /[<>`\"]/;
                        var elements = document.getElementsByTagName("input");
                        var error_msg = $("#error-msg");
                        var server_error_msg = $("#server-error-msg");

                        if (!<%=isUsernameValidationEnabled%>) {
                            if (showUsernameRegexValidationStatus()) {
                                userName.value = alphanumericUsernameUserInput.value.trim();
                            } else {
                                validInput = false;
                            }
                            if (<%=isSelfRegistrationLockOnCreationEnabled%> && !showUsernameValidationStatus()) {
                                validInput = false
                            }
                        } else if (isAlphanumericUsernameEnabled()) {
                            if (showAlphanumericUsernameValidationStatus()) {
                                userName.value = alphanumericUsernameUserInput.value.trim();
                            } else {
                                validInput = false;
                            }
                        } else {
                            if (showUsernameValidationStatus()) {
                                userName.value = usernameUserInput.value.trim();
                            } else {
                                validInput = false;
                            }
                        }

                        // Password validation.
                        if (showPasswordValidationStatus()) {
                            if (passwordUserInput) {
                                password.value = passwordUserInput.value.trim();
                            }
                        } else {
                            validInput = false;
                        }

                        // Firstname validation.
                        if (!showFirstNameValidationStatus()) {
                            validInput = false;
                        }

                        // Lastname validation.
                        if (!showLastNameValidationStatus()) {
                            validInput = false;
                        }

                        // Date of birth validation.
                        if (!showDateOfBirthValidationStatus()) {
                            validInput = false;
                        }

                        // Mobile number validation.
                        if (!showMobileNumberValidationStatus()) {
                            validInput = false;
                        }

                        // Country validation
                        if (!showCountryValidationStatus()) {
                            validInput = false;
                        }

                        // Validate the custom input fields.
                        // If at least one of the fields return false,
                        // the input will be invalid.
                        for (i = 0; i < elements.length; i++) {
                            if (!showFieldValidationStatus(elements[i])) {
                                validInput = false;
                            }
                        }

                        // Hide the error message from server if exists.
                        if (server_error_msg.text() !== null && server_error_msg.text().trim() !== ""  ) {
                            $("#error-msg").hide();
                            error_msg = $("#server-error-msg");
                        }

                        <%
                        if(reCaptchaEnabled) {
                            %>
                            var resp = $("[name='g-recaptcha-response']")[0].value;
                            if (resp.trim() == '') {
                                error_msg.text("<%=IdentityManagementEndpointUtil.i18n(recoveryResourceBundle,
                                    "Please.select.reCaptcha")%>");
                                error_msg.show();
                                $("html, body").animate({scrollTop: error_msg.offset().top}, 'slow');
                                validInput = false;
                            }
                            <%
                        }
                        %>

                        // If the input is not valid,
                        // This will return false and prevent form submission.
                        if (!validInput) {
                            return false;
                        }

                        // Mark it so that the next submit can be ignored.
                        $form.data("submitted", true);
                        document.getElementById("register").submit();
                    }
                });

                return this;
            };

            $registerForm.preventDoubleSubmission();

            $("#previous_step").val(window.location.href);
        });

        // Submit form method to submit when recaptcha is enabled.
        function submitForm() {
            $form = $("#register");
            if ($form.data("submitted") === true) {
                // Previously submitted - don't submit again.
                console.warn("Prevented a possible double submit event");
            } else {
                var validInput = true;
                var userName = document.getElementById("username");
                var alphanumericUsernameUserInput = document.getElementById("alphanumericUsernameUserInput");
                var usernameUserInput = document.getElementById("usernameUserInput");
                var password = document.getElementById("password");
                var passwordUserInput = document.getElementById("passwordUserInput");
                var unsafeCharPattern = /[<>`\"]/;
                var elements = document.getElementsByTagName("input");
                var error_msg = $("#error-msg");
                var server_error_msg = $("#server-error-msg");

                // Username validation.
                if (!<%=isUsernameValidationEnabled%>) {
                    if (showUsernameRegexValidationStatus()) {
                        userName.value = alphanumericUsernameUserInput.value.trim();
                    } else {
                        validInput = false;
                    }
                    if (<%=isSelfRegistrationLockOnCreationEnabled%> && !showUsernameValidationStatus()) {
                        validInput = false
                    }
		        } else if (isAlphanumericUsernameEnabled()) {
                    if (showAlphanumericUsernameValidationStatus()) {
                        userName.value = alphanumericUsernameUserInput.value.trim();
                    } else {
                        validInput = false;
                    }
                } else {
                    if (showUsernameValidationStatus()) {
                        userName.value = usernameUserInput.value.trim();
                    } else {
                        validInput = false;
                    }
                }
                // Password validation.
                if (showPasswordValidationStatus()) {
                    if (passwordUserInput) {
                        password.value = passwordUserInput.value.trim();
                    }
                } else {
                    validInput = false;
                }
                // Firstname validation.
                if (!showFirstNameValidationStatus()) {
                    validInput = false;
                }
                // Lastname validation.
                if (!showLastNameValidationStatus()) {
                    validInput = false;
                }
                // Date of birth validation.
                if (!showDateOfBirthValidationStatus()) {
                    validInput = false;
                }
                // Mobile number validation.
                if (!showMobileNumberValidationStatus()) {
                    validInput = false;
                }
                // Country validation
                if (!showCountryValidationStatus()) {
                    validInput = false;
                }
                // Validate the custom input fields.
                // If at least one of the fields return false,
                // the input will be invalid.
                for (i = 0; i < elements.length; i++) {
                    if (!showFieldValidationStatus(elements[i])) {
                        validInput = false;
                    }
                }
                // Hide the error message from server if exists.
                if (server_error_msg.text() !== null && server_error_msg.text().trim() !== ""  ) {
                    $("#error-msg").hide();
                    error_msg = $("#server-error-msg");
                }
                <%
                    if(reCaptchaEnabled) {
                        %>
                        var resp = $("[name='g-recaptcha-response']")[0].value;
                        if (resp.trim() == '') {
                            error_msg.text("<%=IdentityManagementEndpointUtil.i18n(recoveryResourceBundle,
                                "Please.select.reCaptcha")%>");
                            error_msg.show();
                            $("html, body").animate({scrollTop: error_msg.offset().top}, 'slow');
                            validInput = false;
                        }
                        <%
                    }
                %>
                // Do the form submission if the inputs are valid.
                if (validInput) {
                    $form.data("submitted", true);
                    document.getElementById("register").submit();
                } else {
                    // Reset the recaptcha to allow another submission.
                    var reCaptchaType = "<%= CaptchaUtil.getReCaptchaType()%>"
                    if ("recaptcha-enterprise" == reCaptchaType) {
                        grecaptcha.enterprise.reset();
                    } else {
                        grecaptcha.reset();
                    }
                }
            }
        }

        // Handle selected authenticators.
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

        // show email
        function showEmail() {
            $("#continue-with-email").hide();
            $("#federated-authenticators").hide();
            $("#basic-form").show();
        }

        // show password function
        function showPassword() {
            var passwordField = $('#passwordUserInput');

            if (passwordField.attr("type") === 'text') {
                passwordField.attr("type", "password")
                document.getElementById("password-eye").classList.add("slash");
            } else {
                passwordField.attr("type", "text")
                document.getElementById("password-eye").classList.remove("slash");
            }
        }

        function showFieldValidationStatus(element) {
            var unsafeCharPattern = /[<>`\"]/;
            var element_claim_name = element.name.split("/").pop();

            if (element_claim_name === "lastname") {
                element_claim_name = "givenname";
            }

            if (element_claim_name === "usernameInput") {
                return true;
            }

            var element_field = element_claim_name + "_field";
            var error_msg_txt = element_claim_name + "_error_text";
            var error_msg_element = element_claim_name + "_error";

            if (element.type === 'text' && element.value != null && element.value.trim() !== ""
                && element.value.length > 250)  {
                $("#" + error_msg_txt).text("<%=IdentityManagementEndpointUtil.i18n(recoveryResourceBundle, "maxium.length.cannot.exceed")%>" + " 250");
                $("#" + error_msg_element).show();
                $("#" + element_field).addClass("error");
                var error_msg_txt_element = document.getElementById(error_msg_txt);
                if (error_msg_txt_element) {
                    $("html, body").animate({scrollTop: $("#" + error_msg_txt).offset().top}, 'slow');
                }

                return false;
            }
            if (element.type === 'text' && element.value != null
                && element.value.match(unsafeCharPattern) != null) {
                $("#" + error_msg_txt).text("<%=IdentityManagementEndpointUtil.i18n(recoveryResourceBundle,
                    "For.security.following.characters.restricted")%>");
                $("#" + error_msg_element).show();
                $("#" + element_field).addClass("error");
                var error_msg_txt_element = document.getElementById(error_msg_txt);
                if (error_msg_txt_element) {
                    $("html, body").animate({scrollTop: $("#" + error_msg_txt).offset().top}, 'slow');
                }

                return false;
            }
            if (element.type === 'text' && element.required && element.value.trim() === "") {
                $("#" + error_msg_txt).text("<%=IdentityManagementEndpointUtil.i18n(recoveryResourceBundle,
                "For.required.fields.cannot.be.empty")%>");
                $("#" + error_msg_element).show();
                $("#" + element_field).addClass("error");
                var error_msg_txt_element = document.getElementById(error_msg_txt);
                if (error_msg_txt_element) {
                    $("html, body").animate({scrollTop: $("#" + error_msg_txt).offset().top}, 'slow');
                }

                return false;
            }
            if (element.type === 'text' && element.value != null && !element.checkValidity()) {
                $("#" + error_msg_txt).text("<%=IdentityManagementEndpointUtil.i18n(recoveryResourceBundle,
                    "Please.enter.valid.input")%>");
                $("#" + error_msg_element).show();
                $("#" + element_field).addClass("error");
                var error_msg_txt_element = document.getElementById(error_msg_txt);
                if (error_msg_txt_element) {
                    $("html, body").animate({scrollTop: $("#" + error_msg_txt).offset().top}, 'slow');
                }

                return false;
            }

            return true;
        }

        function hideFieldValidationStatus(element) {
            var element_claim_name = element.name.split("/").pop();

            if (element_claim_name === "lastname") {
                element_claim_name = "givenname";
            }

            var element_field = element_claim_name + "_field";
            var error_msg_txt = element_claim_name + "_error_text";
            var error_msg_element = element_claim_name + "_error";

            // Remove previous errors.
            $("#" + error_msg_element).hide();
            $("#" + element_field).removeClass("error");
        }

        function showUsernameRegexValidationStatus() {

            var alphanumericUsernameUserInput = document.getElementById("alphanumericUsernameUserInput");
            var alphanumericUsernameField = $("#alphanumericUsernameField");
            var alphanumeric_username_error_msg = $("#alphanumeric-username-error-msg");
            var server_error_msg = $("#server-error-msg");
            var alphanumeric_username_error_msg_text = $("#alphanumeric-username-error-msg-text");
            if (server_error_msg.text() !== null && server_error_msg.text().trim() !== "") {
                alphanumeric_username_error_msg.hide();
                alphanumericUsernameField.removeClass("error");
            }

            if (alphanumericUsernameUserInput.value.trim() === "")  {
                alphanumeric_username_error_msg_text.text("<%=IdentityManagementEndpointUtil.i18n(recoveryResourceBundle, "enter.your.username")%>");
                alphanumeric_username_error_msg.show();
                alphanumericUsernameField.addClass("error");
                $("html, body").animate({scrollTop: alphanumeric_username_error_msg_text.offset().top}, 'slow');

                return false;
            }
            alphanumeric_username_error_msg.hide();
            alphanumericUsernameField.removeClass("error");
            return true
        }

        function showAlphanumericUsernameValidationStatus() {
            var alphanumericUsernameUserInput = document.getElementById("alphanumericUsernameUserInput");
            var alphanumericUsernameField = $("#alphanumericUsernameField");
            var alphanumeric_username_error_msg = $("#alphanumeric-username-error-msg");
            var server_error_msg = $("#server-error-msg");
            var alphanumeric_username_error_msg_text = $("#alphanumeric-username-error-msg-text");

            if (server_error_msg.text() !== null && server_error_msg.text().trim() !== ""  ) {
                alphanumeric_username_error_msg.hide();
                alphanumericUsernameField.removeClass("error");
            }

            if (alphanumericUsernameUserInput.value.trim() === "")  {
                alphanumeric_username_error_msg_text.text("<%=IdentityManagementEndpointUtil.i18n(recoveryResourceBundle, "enter.your.username")%>");
                alphanumeric_username_error_msg.show();
                alphanumericUsernameField.addClass("error");
                $("html, body").animate({scrollTop: alphanumeric_username_error_msg_text.offset().top}, 'slow');

                return false;
            } else {
                if (alphanumericUsernameUserInput.value.trim().length < usernameConfig.minLength
                || alphanumericUsernameUserInput.value.trim().length > usernameConfig.maxLength) {
                    alphanumeric_username_error_msg_text.text(
                        "<%=IdentityManagementEndpointUtil.i18n(recoveryResourceBundle, "username.length.should.be")%>"
                        + " " + usernameConfig.minLength + " "
                        + "<%=IdentityManagementEndpointUtil.i18n(recoveryResourceBundle, "and")%>"
                        + " " + usernameConfig.maxLength + ".");
                    alphanumeric_username_error_msg.show();
                    alphanumericUsernameField.addClass("error");

                } else if (usernameConfig.enableSpecialCharacters
                    && !USERNAME_WITH_SPECIAL_CHARS_REGEX.test(alphanumericUsernameUserInput.value.trim())) {
                    alphanumeric_username_error_msg_text.text(
                        "<%=IdentityManagementEndpointUtil.i18n(recoveryResourceBundle, "username.with.special.character.symbols")%>");
                    alphanumeric_username_error_msg.show();
                    alphanumericUsernameField.addClass("error");
                } else if (!usernameConfig.enableSpecialCharacters
                    && !ALPHANUMERIC_USERNAME_REGEX.test(alphanumericUsernameUserInput.value.trim())) {
                    alphanumeric_username_error_msg_text.text(
                        "<%=IdentityManagementEndpointUtil.i18n(recoveryResourceBundle, "username.with.symbols")%>");
                    alphanumeric_username_error_msg.show();
                    alphanumericUsernameField.addClass("error");
                } else {
                    // When username is accepted.
                    alphanumeric_username_error_msg.hide();
                    alphanumericUsernameField.removeClass("error");

                    return true;
                }
            }
        }

        function showUsernameValidationStatus() {
            var userName = document.getElementById("username");
            var usernameUserInput = document.getElementById("usernameUserInput");
            var usernameField = $("#usernameField");
            var username_error_msg = $("#username-error-msg");
            var server_error_msg = $("#server-error-msg");
            var username_error_msg_text = $("#username-error-msg-text");
            <% if (isSelfRegistrationLockOnCreationEnabled) { %>
            	var emailRequired = true;
            <% } else if (emailPII != null) { %>
                var emailRequired = <%=emailPII.getRequired()%>;
            <% } else { %>
                var emailRequired = false;
            <% } %>

            if (server_error_msg.text() !== null && server_error_msg.text().trim() !== ""  ) {
                username_error_msg.hide();
                usernameField.removeClass("error");
            }

            if (usernameUserInput.value.trim() === "" && (emailRequired || !isAlphanumericUsernameEnabled()))  {
                    username_error_msg_text.text("<%=IdentityManagementEndpointUtil.i18n(recoveryResourceBundle, "enter.your.email")%>");
                    username_error_msg.show();
                    usernameField.addClass("error");
                    $("html, body").animate({scrollTop: username_error_msg_text.offset().top}, 'slow');

                    return false;
            } else {
                var usernamePattern = /^([\u00C0-\u00FFa-zA-Z0-9_\+\.\-])+\@(([a-zA-Z0-9\-])+\.)+([a-zA-Z0-9]{2,10})$/;
                if (!usernamePattern.test(usernameUserInput.value.trim()) && (emailRequired || !isAlphanumericUsernameEnabled())) {
                    username_error_msg_text.text("<%=IdentityManagementEndpointUtil.i18n(recoveryResourceBundle, "Please.enter.valid.email")%>")
                    username_error_msg.show();
                    usernameField.addClass("error");
                    $("html, body").animate({scrollTop: username_error_msg_text.offset().top}, 'slow');

                    return false;
                } else {
                    // When username is accepted.
                    username_error_msg.hide();
                    usernameField.removeClass("error");

                    return true;
                }
            }
        }

        function hideUsernameValidationStatus() {
            var usernameField = $("#usernameField");
            var username_error_msg = $("#username-error-msg");
            var server_error_msg = $("#server-error-msg");

            if (server_error_msg.text() !== null && server_error_msg.text().trim() !== ""  ) {
                username_error_msg.hide();
                usernameField.removeClass("error");
            }

            username_error_msg.hide();
            usernameField.removeClass("error");
        }

        function getErrorMessage() {

            let contain = [];
            if (passwordConfig.minUpperCase > 0) {
                contain.push(passwordConfig.minUpperCase + " " + '<%=IdentityManagementEndpointUtil.i18n(recoveryResourceBundle, "uppercase.letters")%>');
            }
            if (passwordConfig.minLowerCase > 0) {
                contain.push(passwordConfig.minLowerCase + " " + '<%=IdentityManagementEndpointUtil.i18n(recoveryResourceBundle, "lowercase.letters")%>');
            }
            if (passwordConfig.minNumber > 0) {
                contain.push(passwordConfig.minNumber + " " + '<%=IdentityManagementEndpointUtil.i18n(recoveryResourceBundle, "numbers")%>');
            }
            if (passwordConfig.minSpecialChr > 0) {
                contain.push(passwordConfig.minSpecialChr + " " + '<%=IdentityManagementEndpointUtil.i18n(recoveryResourceBundle, "special.characters")%>');
            }
            if (passwordConfig.minUniqueChr > 0) {
                contain.push(passwordConfig.minUniqueChr + " " + '<%=IdentityManagementEndpointUtil.i18n(recoveryResourceBundle, "unique.characters")%>');
            }
            if (passwordConfig.maxConsecutiveChr > 0) {
                contain.push(" " + '<%=IdentityManagementEndpointUtil.i18n(recoveryResourceBundle, "no.more.than")%>'
                    + " " + passwordConfig.maxConsecutiveChr + " " + '<%=IdentityManagementEndpointUtil.i18n(recoveryResourceBundle, "repeated.characters")%>');
            }

            var message = '<%=IdentityManagementEndpointUtil.i18n(recoveryResourceBundle, "your.password.should.be.between")%>'
                + " " +
                (passwordConfig.minLength ? passwordConfig.minLength : 8)
                + " " + '<%=IdentityManagementEndpointUtil.i18n(recoveryResourceBundle, "and")%>' + " " +
                (passwordConfig.maxLength ? passwordConfig.maxLength : 30)
                " " + '<%=IdentityManagementEndpointUtil.i18n(recoveryResourceBundle, "characters")%>';
            if (contain.length > 0) {
                message = message + " " + '<%=IdentityManagementEndpointUtil.i18n(recoveryResourceBundle, "including.atleast")%>' + " ";
                let last = contain.pop();
                if (contain.length > 0) {
                    message = message + contain.join(", ") + " and " + last + ".";
                } else {
                    message = message + " " + last;
                }
            } else {
                message = message + ".";
            }
            return message;
        }

        function showPasswordValidationStatus() {
            var password = document.getElementById("password");
            var passwordUserInput = document.getElementById("passwordUserInput");
            var passwordField = $("#passwordField");
            var password_error_msg = $("#password-error-msg");
            var server_error_msg = $("#server-error-msg");
            var password_error_msg_text = $("#password-error-msg-text");

            if (server_error_msg.text() !== null && server_error_msg.text().trim() !== ""  ) {
                password_error_msg.hide();
                passwordField.removeClass("error");
            }

            if (passwordUserInput.value.trim() === "")  {
                    password_error_msg_text.text("<%=IdentityManagementEndpointUtil.i18n(recoveryResourceBundle, "enter.your.password")%>");
                    password_error_msg.show();
                    passwordField.addClass("error");
                    $("html, body").animate({scrollTop: password_error_msg_text.offset().top}, 'slow');

                    return false;
            } else {
                var passwordPattern = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d).{8,}$/;
                var showError = false;
                if (passwordConfig) {
                    if (!validatePassword(passwordUserInput.value.trim())) {
                        showError = true;
                    }
                } else if (!passwordPattern.test(passwordUserInput.value.trim())) {
                    showError = true;
                }

                if (showError) {
                    passwordField.addClass("error");
                    $("html, body").animate({scrollTop: password_error_msg_text.offset().top}, 'slow');

                    return false;
                } else {
                    // When password is accepted.
                    password_error_msg.hide();
                    passwordField.removeClass("error");

                    return true;
                }
            }
        }

        function displayPasswordCross() {
            var displayError = false;

            $("#reset-password-container").removeClass("error");

            // Prevent validation from happening when the password is empty
            if (passwordField.val().length <= 0) {
                return false;
            }

            if ((!passwordConfig.minLength || passwordField.val().length >= passwordConfig.minLength) &&
                (!passwordConfig.maxLength || passwordField.val().length <= passwordConfig.maxLength)) {
                $("#password-validation-check-length").css("display", "block");
                $("#password-validation-neutral-length").css("display", "none");
                $("#password-validation-cross-length").css("display", "none");
            } else {
                $("#password-validation-cross-length").css("display", "block");
                $("#password-validation-check-length").css("display", "none");
                $("#password-validation-neutral-length").css("display", "none");

                displayError = true;
            }

            if (checkMatch(passwordField.val(), passwordConfig.minUpperCase, upperCaseLetters) &&
                checkMatch(passwordField.val(), passwordConfig.minLowerCase, lowerCaseLetters)) {
                $("#password-validation-check-case").css("display", "block");
                $("#password-validation-neutral-case").css("display", "none");
                $("#password-validation-cross-case").css("display", "none");
            } else {
                $("#password-validation-cross-case").css("display", "block");
                $("#password-validation-check-case").css("display", "none");
                $("#password-validation-neutral-case").css("display", "none");

                displayError = true;
            }

            if (checkMatch(passwordField.val(), passwordConfig.minNumber, numbers)) {
                $("#password-validation-check-number").css("display", "block");
                $("#password-validation-neutral-number").css("display", "none");
                $("#password-validation-cross-number").css("display", "none");
            } else {
                $("#password-validation-cross-number").css("display", "block");
                $("#password-validation-check-number").css("display", "none");
                $("#password-validation-neutral-number").css("display", "none");

                displayError = true;
            }
            if (checkMatch(passwordField.val(), passwordConfig.minSpecialChr, specialChr)) {
                $("#password-validation-check-special-chr").css("display", "block");
                $("#password-validation-neutral-special-chr").css("display", "none");
                $("#password-validation-cross-special-chr").css("display", "none");
            } else {
                $("#password-validation-cross-special-chr").css("display", "block");
                $("#password-validation-check-special-chr").css("display", "none");
                $("#password-validation-neutral-special-chr").css("display", "none");

                displayError = true;
            }
            if (checkUniqueCharacter(passwordField.val(), passwordConfig.minUniqueChr)) {
                $("#password-validation-check-unique-chr").css("display", "block");
                $("#password-validation-neutral-unique-chr").css("display", "none");
                $("#password-validation-cross-unique-chr").css("display", "none");
            } else {
                $("#password-validation-cross-unique-chr").css("display", "block");
                $("#password-validation-check-unique-chr").css("display", "none");
                $("#password-validation-neutral-unique-chr").css("display", "none");

                displayError = true;
            }
            if (checkConsecutiveMatch(passwordField.val(), passwordConfig.maxConsecutiveChr, consecutiveChr)) {
                $("#password-validation-check-repeated-chr").css("display", "block");
                $("#password-validation-neutral-repeated-chr").css("display", "none");
                $("#password-validation-cross-repeated-chr").css("display", "none");
            } else {
                $("#password-validation-cross-repeated-chr").css("display", "block");
                $("#password-validation-check-repeated-chr").css("display", "none");
                $("#password-validation-neutral-repeated-chr").css("display", "none");

                displayError = true;
            }

            if (displayError) {
                $("#reset-password-container").addClass("error");
            }
        }

        /**
         * Util function to validate password
         */
        function ShowPasswordStatus() {

            if ((!passwordConfig.minLength || passwordField.val().length >= passwordConfig.minLength) &&
                (!passwordConfig.maxLength || passwordField.val().length <= passwordConfig.maxLength)) {
                $("#password-validation-check-length").css("display", "block");
                $("#password-validation-neutral-length").css("display", "none");
                $("#password-validation-cross-length").css("display", "none");
            } else {
                $("#password-validation-neutral-length").css("display", "block");
                $("#password-validation-check-length").css("display", "none");
                $("#password-validation-cross-length").css("display", "none");
            }

            if (checkMatch(passwordField.val(), passwordConfig.minUpperCase, upperCaseLetters) &&
                checkMatch(passwordField.val(), passwordConfig.minLowerCase, lowerCaseLetters)) {
                $("#password-validation-check-case").css("display", "block");
                $("#password-validation-neutral-case").css("display", "none");
                $("#password-validation-cross-case").css("display", "none");
            } else {
                $("#password-validation-neutral-case").css("display", "block");
                $("#password-validation-check-case").css("display", "none");
                $("#password-validation-cross-case").css("display", "none");
            }
            if (checkMatch(passwordField.val(), passwordConfig.minNumber, numbers)) {
                $("#password-validation-check-number").css("display", "block");
                $("#password-validation-neutral-number").css("display", "none");
                $("#password-validation-cross-number").css("display", "none");
            } else {
                $("#password-validation-neutral-number").css("display", "block");
                $("#password-validation-check-number").css("display", "none");
                $("#password-validation-cross-number").css("display", "none");
            }
            if (checkMatch(passwordField.val(), passwordConfig.minSpecialChr, specialChr)) {
                $("#password-validation-check-special-chr").css("display", "block");
                $("#password-validation-neutral-special-chr").css("display", "none");
                $("#password-validation-cross-special-chr").css("display", "none");
            } else {
                $("#password-validation-neutral-special-chr").css("display", "block");
                $("#password-validation-check-special-chr").css("display", "none");
                $("#password-validation-cross-special-chr").css("display", "none");
            }
            if (checkUniqueCharacter(passwordField.val(), passwordConfig.minUniqueChr)) {
                $("#password-validation-check-unique-chr").css("display", "block");
                $("#password-validation-neutral-unique-chr").css("display", "none");
                $("#password-validation-cross-unique-chr").css("display", "none");
            } else {
                $("#password-validation-neutral-unique-chr").css("display", "block");
                $("#password-validation-check-unique-chr").css("display", "none");
                $("#password-validation-cross-unique-chr").css("display", "none");
            }
            if (checkConsecutiveMatch(passwordField.val(), passwordConfig.maxConsecutiveChr, consecutiveChr)) {
                $("#password-validation-check-repeated-chr").css("display", "block");
                $("#password-validation-neutral-repeated-chr").css("display", "none");
                $("#password-validation-cross-repeated-chr").css("display", "none");
            } else {
                $("#password-validation-neutral-repeated-chr").css("display", "block");
                $("#password-validation-check-repeated-chr").css("display", "none");
                $("#password-validation-cross-repeated-chr").css("display", "none");
            }
        }

        function validatePassword() {
            var valid = true;

            if ((!passwordConfig.minLength || passwordField.val().length >= passwordConfig.minLength) &&
                (!passwordConfig.maxLength || passwordField.val().length <= passwordConfig.maxLength)) {
                $("#password-validation-check-length").css("display", "block");
                $("#password-validation-neutral-length").css("display", "none");
                $("#password-validation-cross-length").css("display", "none");
            } else {
                $("#password-validation-cross-length").css("display", "block");
                $("#password-validation-check-length").css("display", "none");
                $("#password-validation-neutral-length").css("display", "none");

                valid = false;
            }

            if (checkMatch(passwordField.val(), passwordConfig.minUpperCase, upperCaseLetters) &&
                checkMatch(passwordField.val(), passwordConfig.minLowerCase, lowerCaseLetters)) {
                $("#password-validation-check-case").css("display", "block");
                $("#password-validation-neutral-case").css("display", "none");
                $("#password-validation-cross-case").css("display", "none");
            } else {
                $("#password-validation-cross-case").css("display", "block");
                $("#password-validation-check-case").css("display", "none");
                $("#password-validation-neutral-case").css("display", "none");

                valid = false;
            }

            if (checkMatch(passwordField.val(), passwordConfig.minNumber, numbers)) {
                $("#password-validation-check-number").css("display", "block");
                $("#password-validation-neutral-number").css("display", "none");
                $("#password-validation-cross-number").css("display", "none");
            } else {
                $("#password-validation-cross-number").css("display", "block");
                $("#password-validation-check-number").css("display", "none");
                $("#password-validation-neutral-number").css("display", "none");

                valid = false;
            }
            if (checkMatch(passwordField.val(), passwordConfig.minSpecialChr, specialChr)) {
                $("#password-validation-check-special-chr").css("display", "block");
                $("#password-validation-neutral-special-chr").css("display", "none");
                $("#password-validation-cross-special-chr").css("display", "none");
            } else {
                $("#password-validation-cross-special-chr").css("display", "block");
                $("#password-validation-check-special-chr").css("display", "none");
                $("#password-validation-neutral-special-chr").css("display", "none");

                valid = false;
            }
            if (checkUniqueCharacter(passwordField.val(), passwordConfig.minUniqueChr)) {
                $("#password-validation-check-unique-chr").css("display", "block");
                $("#password-validation-neutral-unique-chr").css("display", "none");
                $("#password-validation-cross-unique-chr").css("display", "none");
            } else {
                $("#password-validation-cross-unique-chr").css("display", "block");
                $("#password-validation-check-unique-chr").css("display", "none");
                $("#password-validation-neutral-unique-chr").css("display", "none");

                valid = false;
            }
            if (checkConsecutiveMatch(passwordField.val(), passwordConfig.maxConsecutiveChr, consecutiveChr)) {
                $("#password-validation-check-repeated-chr").css("display", "block");
                $("#password-validation-neutral-repeated-chr").css("display", "none");
                $("#password-validation-cross-repeated-chr").css("display", "none");
            } else {
                $("#password-validation-cross-repeated-chr").css("display", "block");
                $("#password-validation-check-repeated-chr").css("display", "none");
                $("#password-validation-neutral-repeated-chr").css("display", "none");

                valid = false;
            }

            return valid;
        }

        /**
         * Function to validate against regex pattern.
         */
        function checkMatch(_password, limit, pattern) {
            if (!limit || (_password.match(pattern)?.length >= limit)) {
                return true;
            } else {
                return false;
            }
        }

        /**
         * Function to validate against consecutive character validator.
         */
        function checkConsecutiveMatch(_password, limit, pattern) {

            var _consValid = true;
            if (limit > 0 &&
            _password.match(pattern) && _password.match(pattern).length > 0) {
                var list = _password.match(pattern);
                var longest = list.sort(
                    function(a,b) {
                        return b.length - a.length
                    }
                )[0];
                if (longest.length > limit) {
                    _consValid = false;
                }
            }
            return _consValid;
        }

        /**
         * Function to validate against unique character validator.
         */
        function checkUniqueCharacter(_password, limit) {

            var unique = _password.split("");
            var _unique = new Set(unique);
            if (!limit || (_unique.size >= limit)) {
                return true;
            } else {
                return false;
            }
        }

        function hidePasswordValidationStatus() {
            var passwordField = $("#passwordField");
            var password_error_msg = $("#password-error-msg");
            var server_error_msg = $("#server-error-msg");

            if (server_error_msg.text() !== null && server_error_msg.text().trim() !== ""  ) {
                password_error_msg.hide();
                passwordField.removeClass("error");
            }

            password_error_msg.hide();
            passwordField.removeClass("error");
        }

        function showFirstNameValidationStatus() {
            var firstNameUserInput = document.getElementById("firstNameUserInput");
            var firstname_error_msg = $("#firstname-error-msg");
            var firstname_error_msg_text = $("#firstname-error-msg-text");
            var firstname_field= $("#firstNameField");

            if (firstNameUserInput != null && firstNameUserInput.value.trim() === "" && firstNameUserInput.required)  {
                firstname_error_msg_text.text("<%=IdentityManagementEndpointUtil.i18n(recoveryResourceBundle, "required")%>");
                firstname_error_msg.show();
                firstname_field.addClass("error");
                $("html, body").animate({scrollTop: firstname_error_msg_text.offset().top}, 'slow');

                return false;
            } else {
                // When firstname is accepted.
                firstname_error_msg.hide();
                firstname_field.removeClass("error");

                return true;
            }
        }

        function hideFirstNameValidationStatus() {
            var firstname_error_msg = $("#firstname-error-msg");
            var firstname_field= $("#firstNameField");

            firstname_error_msg.hide();
            firstname_field.removeClass("error");
        }

        function showLastNameValidationStatus() {
            var lastNameUserInput = document.getElementById("lastNameUserInput");
            var lastname_error_msg = $("#lastname-error-msg");
            var lastname_error_msg_text = $("#lastname-error-msg-text");
            var lastname_field= $("#lastNameField");

            if (lastNameUserInput != null && lastNameUserInput.value.trim() === "" && lastNameUserInput.required)  {
                lastname_error_msg_text.text("<%=IdentityManagementEndpointUtil.i18n(recoveryResourceBundle, "required")%>");
                lastname_error_msg.show();
                lastname_field.addClass("error");
                $("html, body").animate({scrollTop: lastname_error_msg_text.offset().top}, 'slow');

                return false;
            } else {
                // When lastname is accepted.
                lastname_error_msg.hide();
                lastname_field.removeClass("error");

                return true;
            }
        }

        function hideLastNameValidationStatus() {
            var lastname_error_msg = $("#lastname-error-msg");
            var lastname_field= $("#lastNameField");

            lastname_error_msg.hide();
            lastname_field.removeClass("error");
        }

        function showDateOfBirthValidationStatus() {
            var birthOfDate = document.getElementById("birthOfDate");
            var dob_error_msg = $("#dob_error");
            var dob_error_text = $("#dob_error_text");
            var dob_field = $("#dob_field");

            dob_field.removeClass("error");
            dob_error_msg.hide();

            if (birthOfDate != null && birthOfDate.value != null && birthOfDate.value.trim() !== ""){
                var dobPattern = /^\d{4}-\d{2}-\d{2}$/;
                if (!dobPattern.test(birthOfDate.value)) {
                    dob_error_text.text("<%=IdentityManagementEndpointUtil.i18n(recoveryResourceBundle, "dob.must.in.correct.format")%>")
                    dob_error_msg.show();
                    dob_field.addClass("error");
                    $("html, body").animate({scrollTop: dob_error_text.offset().top}, 'slow');

                    return false;
                }
            } else if (birthOfDate != null && birthOfDate.required && birthOfDate.value.trim() == "") {
                dob_error_text.text("<%=IdentityManagementEndpointUtil.i18n(recoveryResourceBundle, "For.required.fields.cannot.be.empty")%>")
                dob_error_msg.show();
                dob_field.addClass("error");
                $("html, body").animate({scrollTop: dob_error_text.offset().top}, 'slow');

                return false;
            }

            return true;
        }

        function hideDateOfBirthValidationStatus() {
            var dob_error_msg = $("#dob_error");
            var dob_field = $("#dob_field");

            dob_error_msg.hide();
            dob_field.removeClass("error");
        }

        function showCountryValidationStatus() {
            var country = document.getElementById("country");
            var country_error = $("#country_error");
            var country_error_msg_text = $("#country_error_text");
            var country_field = $("#country_field");

            if (country != null && country.value == "" && country.required) {
                country_error_msg_text.text("<%=IdentityManagementEndpointUtil.i18n(recoveryResourceBundle, "For.required.fields.cannot.be.empty")%>")
                country_error.show();
                country_field.addClass("error");
                return false;
            }

            hideCountryValidationStatus();

            return true;
        }

        function hideCountryValidationStatus() {
            var country_error = $("#country_error");
            var country_field = $("#country_field");

            country_error.hide();
            country_field.removeClass("error");
        }

        function showMobileNumberValidationStatus() {
            var mobileNumber = document.getElementById("mobileNumber");
            var mobile_error_msg = $("#mobile_error");
            var mobile_error_msg_text = $("#mobile_error_text");
            var mobile_field = $("#mobile_field");

            if (mobileNumber != null && mobileNumber.value != null && mobileNumber.value.trim() !== ""){
                var mobilePattern = /^\s*(?:\+?(\d{1,3}))?[-. (]*(\d{3})?[-. )]*(\d{3})?[-. ]*(\d{4,6})(?: *x(\d+))?\s*$/;
                if (!mobilePattern.test(mobileNumber.value)) {
                    mobile_error_msg_text.text("<%=IdentityManagementEndpointUtil.i18n(recoveryResourceBundle, "mobile.number.format.error")%>")
                    mobile_error_msg.show();
                    mobile_field.addClass("error");
                    $("html, body").animate({scrollTop: mobile_error_msg_text.offset().top}, 'slow');

                    return false;
                }
            }

            return true;
        }

        function hideMobileNumberValidationStatus() {
            var mobile_error_msg = $("#mobile_error");
            var mobile_field = $("#mobile_field");

            mobile_error_msg.hide();
            mobile_field.removeClass("error");
        }
    </script>
    <script src="libs/addons/calendar.min.js"></script>
</body>
</html>
