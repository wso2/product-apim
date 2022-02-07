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

<%@ page import="org.apache.commons.collections.CollectionUtils" %>
<%@ page import="org.apache.commons.collections.MapUtils" %>
<%@ page import="org.apache.commons.lang.ArrayUtils" %>
<%@ page import="org.apache.commons.lang.StringUtils" %>
<%@ page import="org.owasp.encoder.Encode" %>
<%@ page import="org.wso2.carbon.identity.application.authentication.endpoint.util.Constants" %>
<%@ page import="org.wso2.carbon.identity.mgt.constants.SelfRegistrationStatusCodes" %>
<%@ page import="org.wso2.carbon.identity.mgt.endpoint.util.IdentityManagementEndpointConstants" %>
<%@ page import="org.wso2.carbon.identity.mgt.endpoint.util.IdentityManagementServiceUtil" %>
<%@ page import="org.wso2.carbon.identity.mgt.endpoint.util.IdentityManagementEndpointUtil" %>
<%@ page import="org.wso2.carbon.identity.mgt.endpoint.util.client.ApiException" %>
<%@ page import="org.wso2.carbon.identity.mgt.endpoint.util.client.SelfRegistrationMgtClient" %>
<%@ page import="org.wso2.carbon.identity.mgt.endpoint.util.client.SelfRegistrationMgtClientException" %>
<%@ page import="org.wso2.carbon.identity.mgt.endpoint.util.client.api.UsernameRecoveryApi" %>
<%@ page import="org.wso2.carbon.identity.mgt.endpoint.util.client.model.Claim" %>
<%@ page import="org.wso2.carbon.identity.mgt.endpoint.util.client.model.User" %>
<%@ page import="org.wso2.carbon.identity.core.util.IdentityTenantUtil" %>
<%@ page import="java.io.File" %>
<%@ page import="java.util.Arrays" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.Map" %>

<jsp:directive.include file="includes/localize.jsp"/>
<jsp:directive.include file="tenant-resolve.jsp"/>

<%
    boolean error = IdentityManagementEndpointUtil.getBooleanValue(request.getAttribute("error"));
    String errorMsg = IdentityManagementEndpointUtil.getStringValue(request.getAttribute("errorMsg"));
    SelfRegistrationMgtClient selfRegistrationMgtClient = new SelfRegistrationMgtClient();
    Integer defaultPurposeCatId = null;
    Integer userNameValidityStatusCode = null;
    String username = request.getParameter("username");
    String consentPurposeGroupName = "SELF-SIGNUP";
    String consentPurposeGroupType = "SYSTEM";
    String[] missingClaimList = new String[0];
    String[] missingClaimDisplayName = new String[0];
    Map<String, Claim> uniquePIIs = null;
    boolean piisConfigured = false;
    if (request.getParameter(Constants.MISSING_CLAIMS) != null) {
        missingClaimList = request.getParameter(Constants.MISSING_CLAIMS).split(",");
    }
    if (request.getParameter("missingClaimsDisplayName") != null) {
        missingClaimDisplayName = request.getParameter("missingClaimsDisplayName").split(",");
    }
    boolean allowchangeusername = Boolean.parseBoolean(request.getParameter("allowchangeusername"));
    boolean skipSignUpEnableCheck = Boolean.parseBoolean(request.getParameter("skipsignupenablecheck"));
    boolean isPasswordProvisionEnabled = Boolean.parseBoolean(request.getParameter("passwordProvisionEnabled"));
    boolean isSaaSApp = Boolean.parseBoolean(request.getParameter("isSaaSApp"));
    String callback = Encode.forHtmlAttribute(request.getParameter("callback"));
    User user = IdentityManagementServiceUtil.getInstance().resolveUser(username, tenantDomain, isSaaSApp);

    if (skipSignUpEnableCheck) {
        consentPurposeGroupName = "JIT";
    }
    if (StringUtils.isEmpty(username)) {
        request.setAttribute("error", true);
        request.setAttribute("errorMsg", IdentityManagementEndpointUtil.i18n(recoveryResourceBundle, "Pick.username"));
        request.getRequestDispatcher("register.do").forward(request, response);
        return;
    }


    try {
        userNameValidityStatusCode = selfRegistrationMgtClient
                .checkUsernameValidity(user, skipSignUpEnableCheck);
    } catch (SelfRegistrationMgtClientException e) {
        request.setAttribute("error", true);
        request.setAttribute("errorMsg", IdentityManagementEndpointUtil
                .i18n(recoveryResourceBundle, "Something.went.wrong.while.registering.user") + Encode
                .forHtmlContent(username) + IdentityManagementEndpointUtil
                .i18n(recoveryResourceBundle, "Please.contact.administrator"));

        if (allowchangeusername) {
            request.getRequestDispatcher("register.do").forward(request, response);
        } else {
            IdentityManagementEndpointUtil.addErrorInformation(request, e);
            request.getRequestDispatcher("error.jsp").forward(request, response);
            return;
        }
        return;
    }

    if (StringUtils.isBlank(callback)) {
        callback = IdentityManagementEndpointUtil.getUserPortalUrl(
                application.getInitParameter(IdentityManagementEndpointConstants.ConfigConstants.USER_PORTAL_URL));
    }

    if (userNameValidityStatusCode != null && !SelfRegistrationStatusCodes.CODE_USER_NAME_AVAILABLE.
            equalsIgnoreCase(userNameValidityStatusCode.toString())) {
        if (allowchangeusername || !skipSignUpEnableCheck) {
            request.setAttribute("error", true);
            request.setAttribute("errorCode", userNameValidityStatusCode);
            request.getRequestDispatcher("register.do").forward(request, response);
            return;
        } else {
            String errorCode = String.valueOf(userNameValidityStatusCode);
            if (SelfRegistrationStatusCodes.ERROR_CODE_INVALID_TENANT.equalsIgnoreCase(errorCode)) {
                errorMsg = "Invalid tenant domain - " + user.getTenantDomain() + ".";
            } else if (SelfRegistrationStatusCodes.ERROR_CODE_USER_ALREADY_EXISTS.equalsIgnoreCase(errorCode)) {
                errorMsg = "Username '" + username + "' is already taken.";
            } else if (SelfRegistrationStatusCodes.CODE_USER_NAME_INVALID.equalsIgnoreCase(errorCode)) {
                errorMsg = user.getUsername() + " is an invalid user name. Please pick a valid username.";
            }
            request.setAttribute("errorMsg", errorMsg + " Please contact the administrator to fix this issue.");
            request.setAttribute("errorCode", errorCode);
            request.getRequestDispatcher("error.jsp").forward(request, response);
            return;

        }
    }
    String purposes = selfRegistrationMgtClient.getPurposes(user.getTenantDomain(), consentPurposeGroupName,
            consentPurposeGroupType);
    boolean hasPurposes = StringUtils.isNotEmpty(purposes);
    Claim[] claims = new Claim[0];

    /**
     * Change consentDisplayType to "template" inorder to use a custom html template.
     * other Default values are "row" and "tree".
     */
    String consentDisplayType = "row";

    if (hasPurposes) {
        defaultPurposeCatId = selfRegistrationMgtClient.getDefaultPurposeId(user.getTenantDomain());
        uniquePIIs = IdentityManagementEndpointUtil.getUniquePIIs(purposes);
        if (MapUtils.isNotEmpty(uniquePIIs)) {
            piisConfigured = true;
        }
    }

    List<Claim> claimsList;
    UsernameRecoveryApi usernameRecoveryApi = new UsernameRecoveryApi();
    try {
        claimsList = usernameRecoveryApi.claimsGet(user.getTenantDomain(), false);
        uniquePIIs = IdentityManagementEndpointUtil.fillPiisWithClaimInfo(uniquePIIs, claimsList);
        if (uniquePIIs != null) {
            claims = uniquePIIs.values().toArray(new Claim[0]);
        }
        IdentityManagementEndpointUtil.addReCaptchaHeaders(request, usernameRecoveryApi.getApiClient().getResponseHeaders());

    } catch (ApiException e) {
        IdentityManagementEndpointUtil.addErrorInformation(request, e);
        request.getRequestDispatcher("error.jsp").forward(request, response);
        return;
    }
%>
<%
    boolean reCaptchaEnabled = false;
    if (request.getAttribute("reCaptcha") != null && "TRUE".equalsIgnoreCase((String) request.getAttribute("reCaptcha"))) {
        reCaptchaEnabled = true;
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
        if (reCaptchaEnabled) {
    %>
    <script src='<%=(request.getAttribute("reCaptchaAPI"))%>'></script>
    <%
        }
    %>
</head>
<body class="login-portal layout recovery-layout">
    <main class="center-segment">
        <div class="ui container large center aligned middle aligned">
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

                <h2>
                    <%=IdentityManagementEndpointUtil.i18n(recoveryResourceBundle, "Create.account")%>
                </h2>
                <div class="ui divider hidden"></div>
                <!-- content -->
                <div class="segment-form">
                    <% if (skipSignUpEnableCheck) { %>
                    <form class="ui large form" action="../commonauth" method="post" id="register">
                            <% } else { %>
                        <form class="ui large form" action="processregistration.do" method="post" id="register">
                            <% } %>

                            <div class="">
                                <% if (error) { %>
                                <div class="ui negative message" id="server-error-msg">
                                    <%=IdentityManagementEndpointUtil.i18nBase64(recoveryResourceBundle, errorMsg)%>
                                </div>
                                <% } %>

                                <div class="ui negative message" id="error-msg" hidden="hidden">
                                </div>
                                <input id="isSaaSApp" name="isSaaSApp" type="hidden"
                                       value="<%=isSaaSApp%>">
                                <% if (isPasswordProvisionEnabled || !skipSignUpEnableCheck) { %>
                                <p>
                                    <%=IdentityManagementEndpointUtil.i18n(recoveryResourceBundle, "Enter.fields.to.cmplete.reg")%>
                                </p>
                            </div>
                            <div class="ui divider hidden"></div>
                            <!-- validation -->
                            <div>
                                <div id="regFormError" class="ui negative message" style="display:none"></div>
                                <div id="regFormSuc" class="ui positive message" style="display:none"></div>

                                <% Claim firstNamePII =
                                        uniquePIIs.get(IdentityManagementEndpointConstants.ClaimURIs.FIRST_NAME_CLAIM);
                                   Claim lastNamePII =
                                        uniquePIIs.get(IdentityManagementEndpointConstants.ClaimURIs.LAST_NAME_CLAIM);
                                    if (firstNamePII != null) {
                                        String firstNameValue = request.getParameter(IdentityManagementEndpointConstants.ClaimURIs.FIRST_NAME_CLAIM);
                                %>
                                <% if (lastNamePII !=null) { %>
                                    <div class="two fields">
                                <% } %>
                                    <div class="<% if (firstNamePII.getRequired() || !piisConfigured) {%> required <%}%> field">
                                        <label class="control-label">
                                            <%=IdentityManagementEndpointUtil.i18n(recoveryResourceBundle, "First.name")%>
                                        </label>
                                        <input type="text" name="http://wso2.org/claims/givenname" class="form-control"
                                            <% if (firstNamePII.getRequired() || !piisConfigured) {%> required <%}%>
                                            <% if (skipSignUpEnableCheck && StringUtils.isNotEmpty(firstNameValue)) { %>
                                               value="<%= Encode.forHtmlAttribute(firstNameValue)%>" disabled <% } %>>
                                    </div>
                                    <%}%>

                                    <%
                                        if (lastNamePII != null) {
                                            String lastNameValue =
                                                    request.getParameter(IdentityManagementEndpointConstants.ClaimURIs.LAST_NAME_CLAIM);
                                    %>
                                    <div class="<% if (lastNamePII.getRequired() || !piisConfigured) {%> required <%}%> field">
                                        <label class="control-label">
                                            <%=IdentityManagementEndpointUtil.i18n(recoveryResourceBundle, "Last.name")%>
                                        </label>
                                        <input type="text" name="http://wso2.org/claims/lastname" class="form-control"
                                            <% if (lastNamePII.getRequired() || !piisConfigured) {%> required <%}%>
                                            <% if (skipSignUpEnableCheck && StringUtils.isNotEmpty(lastNameValue)) { %>
                                               value="<%= Encode.forHtmlAttribute(lastNameValue)%>" disabled <% } %>>

                                    </div>
                                <% if(firstNamePII != null ) {%>
                                    </div>
                                <% } %>
                                <%}%>
                                <div class="field">
                                    <input id="username" name="username" type="hidden"
                                           value="<%=Encode.forHtmlAttribute(username)%>"
                                           class="form-control required usrName usrNameLength">
                                </div>
                                <div class="two fields">
                                    <div class="required field">
                                        <label class="control-label">
                                            <%=IdentityManagementEndpointUtil.i18n(recoveryResourceBundle, "Password")%>
                                        </label>
                                        <input id="password" name="password" type="password"
                                               class="form-control" required>
                                    </div>
                                    <div class="required field">
                                        <label class="control-label">
                                            <%=IdentityManagementEndpointUtil.i18n(recoveryResourceBundle, "Confirm.password")%>
                                        </label>
                                        <input id="password2" name="password2" type="password" class="form-control"
                                               data-match="reg-password" required>
                                    </div>
                                </div>

                                <% Claim emailNamePII =
                                        uniquePIIs.get(IdentityManagementEndpointConstants.ClaimURIs.EMAIL_CLAIM);
                                    if (emailNamePII != null) {
                                        String emailValue =
                                                request.getParameter(IdentityManagementEndpointConstants.ClaimURIs.EMAIL_CLAIM);
                                %>
                                <div class="<% if (emailNamePII.getRequired() || !piisConfigured) {%> required <%}%> field">
                                    <label class="control-label">
                                        <%=IdentityManagementEndpointUtil.i18n(recoveryResourceBundle, "Email")%>
                                    </label>
                                    <input type="email" name="http://wso2.org/claims/emailaddress" class="form-control"
                                           data-validate="email"
                                        <% if (emailNamePII.getValidationRegex() != null) {
                                                String pattern = Encode.forHtmlContent(emailNamePII.getValidationRegex());
                                                String[] patterns = pattern.split("\\\\@");
                                                String regex = StringUtils.join(patterns, "@");
                                        %>
                                        pattern="<%= regex %>"
                                        <% } %>
                                        <% if (emailNamePII.getRequired() || !piisConfigured) {%> required <%}%>
                                        <% if
                                            (skipSignUpEnableCheck && StringUtils.isNotEmpty(emailValue)) {%>
                                           value="<%= Encode.forHtmlAttribute(emailValue)%>"
                                           disabled<%}%>>
                                </div>
                                <%
                                    }

                                    if (callback != null) {
                                %>
                                <input type="hidden" name="callback" value="<%=callback %>"/>
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
                                    <label class="control-label">
                                        <%=IdentityManagementEndpointUtil.i18nBase64(recoveryResourceBundle, claimDisplayName)%>
                                    </label>
                                    <input type="text" name="missing-<%=Encode.forHtmlAttribute(claim)%>"
                                           id="<%=Encode.forHtmlAttribute(claim)%>" class="form-control"
                                           required="required">
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
                                                !(claim.getReadOnly() != null ? claim.getReadOnly() : false)) {
                                            String claimURI = claim.getUri();
                                            String claimValue = request.getParameter(claimURI);
                                %>
                                <div class="<% if (claim.getRequired()) {%> required <%}%>field">
                                    <label <% if (claim.getRequired()) {%> class="control-label" <%}%>>
                                        <%=IdentityManagementEndpointUtil.i18nBase64(recoveryResourceBundle, claim.getDisplayName())%>
                                    </label>
                                    <input type="text" name="<%= Encode.forHtmlAttribute(claimURI) %>"
                                           class="form-control"
                                        <% if (claim.getValidationRegex() != null) { %>
                                           pattern="<%= Encode.forHtmlContent(claim.getValidationRegex()) %>"
                                        <% } %>
                                        <% if (claim.getRequired()) { %>
                                           required
                                        <% } %>
                                        <% if(skipSignUpEnableCheck && StringUtils.isNotEmpty(claimValue)) {%>
                                           value="<%= Encode.forHtmlAttribute(claimValue)%>" disabled<%}%>>
                                </div>
                                <%
                                        }
                                    }
                                %>
                            </div>

                            <% } else { %>
                            <div>
                                <div class="field">
                                    <label class="control-label">User Name
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
                                    <!--User Consents from Template-->
                                        <div class="consent-statement"></div>
                                    <!--End User Consents from Template-->
                                    <% } else if (consentDisplayType == "tree") { %>
                                    <!--User Consents Tree-->
                                        <div id="tree-table"></div>
                                    <!--End User Consents Tree-->
                                    <%
                                    } else if (consentDisplayType == "row") {
                                    %>
                                    <!--User Consents Row-->
                                        <div id="row-container"></div>
                                    <!--End User Consents Row-->
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
                                %>
                                <div class="field">
                                    <div class="g-recaptcha"
                                         data-sitekey="<%=Encode.forHtmlContent((String)request.getAttribute("reCaptchaKey"))%>">
                                    </div>
                                </div>
                                <%
                                    }
                                %>
                                <div class="ui divider hidden"></div>
                                <div>
                                    <!--Cookie Policy-->
                                    <div class="ui message info compact" role="alert">
                                        <div>
                                            <%=IdentityManagementEndpointUtil.i18n(recoveryResourceBundle,
                                                    "After.signin.we.use.a.cookie.in.browser")%>
                                            <a href="/authenticationendpoint/cookie_policy.do" target="policy-pane">
                                                <%=IdentityManagementEndpointUtil.i18n(recoveryResourceBundle,
                                                        "Cookie.policy")%>
                                            </a>
                                            <%=IdentityManagementEndpointUtil.i18n(recoveryResourceBundle, "For.more.details")%>
                                        </div>
                                    </div>
                                    <!--End Cookie Policy-->
                                </div>
                                <div class="ui divider hidden"></div>
                                <div>
                                    <!--Terms/Privacy Policy-->
                                    <div class="required field">
                                        <div class="ui checkbox">
                                            <input id="termsCheckbox" type="checkbox"/>
                                            <label><%=IdentityManagementEndpointUtil.i18n(recoveryResourceBundle,
                                                    "I.confirm.that.read.and.understood")%>
                                                <a href="/authenticationendpoint/privacy_policy.do" target="policy-pane">
                                                    <%=IdentityManagementEndpointUtil.i18n(recoveryResourceBundle, "Privacy.policy")%>
                                                </a></label>
                                        </div>
                                    </div>
                                    <!--End Terms/Privacy Policy-->
                                </div>
                                <div class="ui divider hidden"></div>
                                <div class="align-right buttons">
                                    <a href="javascript:goBack()" class="ui button link-button">
                                        <%=IdentityManagementEndpointUtil.i18n(recoveryResourceBundle, "Cancel")%>
                                    </a>
                                    <button id="registrationSubmit"
                                            class="ui primary button"
                                            type="submit">
                                        <%=IdentityManagementEndpointUtil.i18n(recoveryResourceBundle, "Register")%>
                                    </button>
                                </div>
                                <div class="field">
                                    <input id="isSelfRegistrationWithVerification" type="hidden"
                                           name="isSelfRegistrationWithVerification"
                                           value="true"/>
                                    <%
                                        if (!IdentityTenantUtil.isTenantQualifiedUrlsEnabled()) {
                                    %>
                                    <input id="tenantDomain" name="tenantDomain" type="hidden"
                                           value="<%=user.getTenantDomain()%>"/>
                                    <%
                                        }
                                    %>
                                </div>
                                <% if (!skipSignUpEnableCheck) { %>
                                <div>
                                        <span>
                                            <%=IdentityManagementEndpointUtil.i18n(recoveryResourceBundle, "Already.have.account")%></span>
                                    <a href="javascript:history.go(-2)"
                                       id="signInLink">
                                        <%=IdentityManagementEndpointUtil.i18n(recoveryResourceBundle, "Sign.in")%>
                                    </a>
                                </div>
                                <% } %>
                            </div>
                        </form>
                </div>
            </div>
        </div>
    </main>
    <!-- /content/body -->
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


    <div id="attribute_selection_validation" class="ui modal tiny">
        <div class="header">
            <h4>
                <%=IdentityManagementEndpointUtil.i18n(recoveryResourceBundle, "Consent.selection")%>
            </h4>
        </div>
        <div class="content">
            <%=IdentityManagementEndpointUtil.i18n(recoveryResourceBundle, "You.need.consent.all.claims")%>
        </div>
        <div class="actions">
            <button type="button" class="ui primary button" data-dismiss="modal">
                <%=IdentityManagementEndpointUtil.i18n(recoveryResourceBundle, "Ok")%>
            </button>
        </div>
    </div>


    <div id="mandetory_pii_selection_validation" class="ui tiny modal">
        <div class="header">
            <h4>
                <%=IdentityManagementEndpointUtil.i18n(recoveryResourceBundle, "Consent.selection")%>
            </h4>
        </div>
        <div class="content">
            <%=IdentityManagementEndpointUtil.i18n(recoveryResourceBundle, "Need.to.select.all.mandatory.attributes")%>
        </div>
        <div class="actions">
            <button type="button" class="ui primary button cancel" data-dismiss="modal">
                <%=IdentityManagementEndpointUtil.i18n(recoveryResourceBundle, "Ok")%>
            </button>
        </div>
    </div>

    <script type="text/javascript" src="libs/handlebars.min-v4.7.6.js"></script>
    <script type="text/javascript" src="libs/jstree/dist/jstree.min.js"></script>
    <script type="text/javascript" src="libs/jstree/src/jstree-actions.js"></script>
    <script type="text/javascript" src="js/consent_template_1.js"></script>
    <script type="text/javascript" src="js/consent_template_2.js"></script>
    <script type="text/javascript">
        var registrationDataKey = "registrationData";
        var $registerForm = $("#register");

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

        $(document).ready(function () {
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

            var container;
            var allAttributes = [];
            var canSubmit;

            var agreementChk = $(".agreement-checkbox input");
            var registrationBtn = $("#registrationSubmit");

            if (agreementChk.length > 0) {
                registrationBtn.prop("disabled", true).addClass("disabled");
            }
            agreementChk.click(function () {
                if ($(this).is(":checked")) {
                    registrationBtn.prop("disabled", false).removeClass("disabled");
                } else {
                    registrationBtn.prop("disabled", true).addClass("disabled");
                }
            });

            $(".form-info").popup();

            $("#register").submit(function (e) {
                var unsafeCharPattern = /[<>`\"]/;
                var elements = document.getElementsByTagName("input");
                var invalidInput = false;
                var error_msg = $("#error-msg");

                for (i = 0; i < elements.length; i++) {
                    if (elements[i].type === 'text' && elements[i].value != null
                        && elements[i].value.match(unsafeCharPattern) != null) {
                        error_msg.text("<%=IdentityManagementEndpointUtil.i18n(recoveryResourceBundle,
                            "For.security.following.characters.restricted")%>");
                        error_msg.show();
                        $("html, body").animate({scrollTop: error_msg.offset().top}, 'slow');
                        invalidInput = true;
                        return false;
                    } else if (elements[i].type === 'text' && elements[i].required && elements[i].value.trim() === "") {
                        error_msg.text("<%=IdentityManagementEndpointUtil.i18n(recoveryResourceBundle,
                            "For.required.fields.cannot.be.empty")%>");
                        error_msg.show();
                        $("html, body").animate({scrollTop: error_msg.offset().top}, 'slow');
                        invalidInput = true;
                        return false;
                    }
                }

                if (invalidInput) {
                    return false;
                }

                var password = $("#password").val();
                var password2 = $("#password2").val();

                if (password !== password2) {
                    error_msg.text("<%=IdentityManagementEndpointUtil.i18n(recoveryResourceBundle,
                        "Passwords.did.not.match.please.try.again")%>");
                    error_msg.show();
                    $("html, body").animate({scrollTop: error_msg.offset().top}, 'slow');
                    return false;
                }

                if(!$("#termsCheckbox")[0].checked){
                        error_msg.text("<%=IdentityManagementEndpointUtil.i18n(recoveryResourceBundle,
                            "Confirm.Privacy.Policy")%>");
                        error_msg.show();
                        $("html, body").animate({scrollTop: error_msg.offset().top}, 'slow');
                        return false;
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
                    return false;
                }
                <%
                }
                %>

                <%
                if (hasPurposes) {
                %>
                var self = this;
                var receipt;
                e.preventDefault();
                <%
                if (consentDisplayType == "template") {
                %>
                receipt = addReciptInformationFromTemplate();
                <%
                } else if (consentDisplayType == "tree") {
                %>
                receipt = addReciptInformation(container);
                <%
                } else if (consentDisplayType == "row")  {
                %>
                receipt = addReciptInformationFromRows();
                <%
                }
                %>

                $('<input />').attr('type', 'hidden')
                    .attr('name', "consent")
                    .attr('value', JSON.stringify(receipt))
                    .appendTo('#register');
                if (canSubmit) {
                    self.submit();
                }

                <%
                }
                %>

                var data = $("#register").serializeArray();
                var filteredData = data.filter(function(row) {
                    return !(row.name === "password" || row.name === "password2");
                });

                sessionStorage.setItem(registrationDataKey, JSON.stringify(filteredData));

                return true;
            });


            function compareArrays(arr1, arr2) {
                return $(arr1).not(arr2).length == 0 && $(arr2).not(arr1).length == 0
            }

            String.prototype.replaceAll = function (str1, str2, ignore) {
                return this.replace(new RegExp(str1.replace(/([\/\,\!\\\^\$\{\}\[\]\(\)\.\*\+\?\|\<\>\-\&])/g, "\\$&"), (ignore ? "gi" : "g")), (typeof (str2) == "string") ? str2.replace(/\$/g, "$$$$") : str2);
            };

            Handlebars.registerHelper('grouped_each', function (every, context, options) {
                var out = "", subcontext = [], i;
                if (context && context.length > 0) {
                    for (i = 0; i < context.length; i++) {
                        if (i > 0 && i % every === 0) {
                            out += options.fn(subcontext);
                            subcontext = [];
                        }
                        subcontext.push(context[i]);
                    }
                    out += options.fn(subcontext);
                }
                return out;
            });

            <%
            if (hasPurposes) {
                if(consentDisplayType == "template") {
                    %>
            renderReceiptDetailsFromTemplate(<%=purposes%>);
            <%
                } else if (consentDisplayType == "tree") {
            %>
            renderReceiptDetails(<%=purposes%>);
            <%
                } else if (consentDisplayType == "row"){
            %>
            renderReceiptDetailsFromRows(<%=purposes%>);
            <%
                }
            }
            %>

            function renderReceiptDetails(data) {

                var treeTemplate =
                    '<div id="html1">' +
                    '<ul><li class="jstree-open" data-jstree=\'{"icon":"icon-book"}\'>All' +
                    '<ul>' +
                    '{{#purposes}}' +
                    '<li data-jstree=\'{"icon":"icon-book"}\' purposeid="{{purposeId}}" mandetorypurpose={{mandatory}}>' +
                    '{{purpose}}{{#if mandatory}}<span class="required_consent">*</span>{{/if}} {{#if description}}<img src="images/info.png" class="form-info" data-toggle="tooltip" data-content="{{description}}" data-placement="right"/>{{/if}}<ul>' +
                    '{{#piiCategories}}' +
                    '<li data-jstree=\'{"icon":"icon-user"}\' piicategoryid="{{piiCategoryId}}" mandetorypiicatergory={{mandatory}}>{{#if displayName}}{{displayName}}{{else}}{{piiCategory}}{{/if}}{{#if mandatory}}<span class="required_consent">*</span>{{/if}}</li>' +
                    '</li>' +
                    '{{/piiCategories}}' +
                    '</ul>' +
                    '{{/purposes}}' +
                    '</ul></li>' +
                    '</ul>' +
                    '</div>';

                var tree = Handlebars.compile(treeTemplate);
                var treeRendered = tree(data);

                $("#tree-table").html(treeRendered);

                container = $("#html1").jstree({
                    plugins: ["table", "sort", "checkbox", "actions"],
                    checkbox: {"keep_selected_style": false},
                });

                container.bind('hover_node.jstree', function () {
                    var bar = $(this).find('.jstree-wholerow-hovered');
                    bar.css('height',
                        bar.parent().children('a.jstree-anchor').height() + 'px');
                });

                container.on('ready.jstree', function (event, data) {
                    var $tree = $(this);
                    $($tree.jstree().get_json($tree, {
                        flat: true
                    }))
                        .each(function (index, value) {
                            var node = container.jstree().get_node(this.id);
                            allAttributes.push(node.id);
                        });
                    container.jstree('open_all');
                });

            }

            function addReciptInformation(container) {
                // var oldReceipt = receiptData.receipts;
                var newReceipt = {};
                var services = [];
                var service = {};
                var mandatoryPiis = [];
                var selectedMandatoryPiis = [];

                var selectedNodes = container.jstree(true).get_selected('full', true);
                var undeterminedNodes = container.jstree(true).get_undetermined('full', true);
                var allTreeNodes = container.jstree(true).get_json('#', {flat: true});

                $.each(allTreeNodes, function (i, val) {
                    if (typeof (val.li_attr.mandetorypiicatergory) != "undefined" &&
                        val.li_attr.mandetorypiicatergory == "true") {
                        mandatoryPiis.push(val.li_attr.piicategoryid);
                    }
                });

                $.each(selectedNodes, function (i, val) {
                    if (val.hasOwnProperty('li_attr')) {
                        selectedMandatoryPiis.push(selectedNodes[i].li_attr.piicategoryid);
                    }
                });

                var allMandatoryPiisSelected = mandatoryPiis.every(function (val) {
                    return selectedMandatoryPiis.indexOf(val) >= 0;
                });

                if (!allMandatoryPiisSelected) {
                    $("#mandetory_pii_selection_validation").modal({blurring: true}).modal("show");
                    canSubmit = false;
                } else {
                    canSubmit = true;
                }

                if (!selectedNodes || selectedNodes.length < 1) {
                    //revokeReceipt(oldReceipt.consentReceiptID);
                    return;
                }
                selectedNodes = selectedNodes.concat(undeterminedNodes);
                var relationshipTree = unflatten(selectedNodes); //Build relationship tree
                var purposes = relationshipTree[0].children;
                var newPurposes = [];

                for (var i = 0; i < purposes.length; i++) {
                    var purpose = purposes[i];
                    var newPurpose = {};
                    newPurpose["purposeId"] = purpose.li_attr.purposeid;
                    newPurpose['piiCategory'] = [];
                    newPurpose['purposeCategoryId'] = [<%=defaultPurposeCatId%>];

                    var piiCategory = [];
                    var categories = purpose.children;
                    for (var j = 0; j < categories.length; j++) {
                        var category = categories[j];
                        var c = {};
                        c['piiCategoryId'] = category.li_attr.piicategoryid;
                        piiCategory.push(c);
                    }
                    newPurpose['piiCategory'] = piiCategory;
                    newPurposes.push(newPurpose);
                }
                service['purposes'] = newPurposes;
                services.push(service);
                newReceipt['services'] = services;

                return newReceipt;
            }

            function addReciptInformationFromTemplate() {
                var newReceipt = {};
                var services = [];
                var service = {};
                var newPurposes = [];

                $('.consent-statement input[type="checkbox"], .consent-statement strong label')
                    .each(function (i, element) {
                        var checked = $(element).prop('checked');
                        var isLable = $(element).is("lable");
                        var newPurpose = {};
                        var piiCategories = [];
                        var isExistingPurpose = false;

                        if (!isLable && checked) {
                            var purposeId = element.data("purposeid");

                            if (newPurposes.length != 0) {
                                for (var i = 0; i < newPurposes.length; i++) {
                                    var selectedPurpose = newPurposes[i];
                                    if (selectedPurpose.purposeId == purposeId) {
                                        newPurpose = selectedPurpose;
                                        piiCategories = newPurpose.piiCategory;
                                        isExistingPurpose = true;
                                    }
                                }
                            }
                        }

                        var newPiiCategory = {};

                        newPurpose["purposeId"] = element.data("purposeid");
                        newPiiCategory['piiCategoryId'] = element.data("piicategoryid");
                        piiCategories.push(newPiiCategory);
                        newPurpose['piiCategory'] = piiCategories;
                        newPurpose['purposeCategoryId'] = [<%=defaultPurposeCatId%>];
                        if (!isExistingPurpose) {
                            newPurposes.push(newPurpose);
                        }
                    });
                service['purposes'] = newPurposes;
                services.push(service);
                newReceipt['services'] = services;

                return newReceipt;
            }

            function addReciptInformationFromRows() {
                var newReceipt = {};
                var services = [];
                var service = {};
                var newPurposes = [];
                var mandatoryPiis = [];
                var selectedMandatoryPiis = [];

                $('#row-container input[type="checkbox"]').each(function (i, checkbox) {
                    var checkboxLabel = $(checkbox).next();
                    var checked = $(checkbox).prop('checked');
                    var newPurpose = {};
                    var piiCategories = [];
                    var isExistingPurpose = false;

                    if (checkboxLabel.data("mandetorypiicatergory")) {
                        mandatoryPiis.push(checkboxLabel.data("piicategoryid"));
                    }

                    if (checked) {
                        var purposeId = checkboxLabel.data("purposeid");
                        selectedMandatoryPiis.push(checkboxLabel.data("piicategoryid"));
                        if (newPurposes.length != 0) {
                            for (var i = 0; i < newPurposes.length; i++) {
                                var selectedPurpose = newPurposes[i];
                                if (selectedPurpose.purposeId == purposeId) {
                                    newPurpose = selectedPurpose;
                                    piiCategories = newPurpose.piiCategory;
                                    isExistingPurpose = true;
                                }
                            }
                        }
                        var newPiiCategory = {};

                        newPurpose["purposeId"] = checkboxLabel.data("purposeid");
                        newPiiCategory['piiCategoryId'] = checkboxLabel.data("piicategoryid");
                        piiCategories.push(newPiiCategory);
                        newPurpose['piiCategory'] = piiCategories;
                        newPurpose['purposeCategoryId'] = [<%=defaultPurposeCatId%>];
                        if (!isExistingPurpose) {
                            newPurposes.push(newPurpose);
                        }
                    }
                });
                service['purposes'] = newPurposes;
                services.push(service);
                newReceipt['services'] = services;

                var allMandatoryPiisSelected = mandatoryPiis.every(function (val) {
                    return selectedMandatoryPiis.indexOf(val) >= 0;
                });

                if (!allMandatoryPiisSelected) {
                    $("#mandetory_pii_selection_validation").modal({blurring: true}).modal("show");
                    canSubmit = false;
                } else {
                    canSubmit = true;
                }

                return newReceipt;
            }

            function unflatten(arr) {
                var tree = [],
                    mappedArr = {},
                    arrElem,
                    mappedElem;

                // First map the nodes of the array to an object -> create a hash table.
                for (var i = 0, len = arr.length; i < len; i++) {
                    arrElem = arr[i];
                    mappedArr[arrElem.id] = arrElem;
                    mappedArr[arrElem.id]['children'] = [];
                }

                for (var id in mappedArr) {
                    if (mappedArr.hasOwnProperty(id)) {
                        mappedElem = mappedArr[id];
                        // If the element is not at the root level, add it to its parent array of children.
                        if (mappedElem.parent && mappedElem.parent != "#" && mappedArr[mappedElem['parent']]) {
                            mappedArr[mappedElem['parent']]['children'].push(mappedElem);
                        }
                        // If the element is at the root level, add it to first level elements array.
                        else {
                            tree.push(mappedElem);
                        }
                    }
                }
                return tree;
            }

            function renderReceiptDetailsFromTemplate(receipt) {
                /*
                 *   Available when consentDisplayType is set to "template"
                 *   customConsentTempalte1 is from the js file which is loaded as a normal js resource
                 *   also try customConsentTempalte2 located at assets/js/consent_template_2.js
                 */
                var templateString = customConsentTempalte1;
                var purp, purpose, piiCategory, piiCategoryInputTemplate;
                $(receipt.purposes).each(function (i, e) {
                    purp = e.purpose;
                    purpose = "{{purpose:" + purp + "}}";
                    var purposeInputTemplate = '<strong data-id="' + purpose + '">' + purp + '</strong>';
                    templateString = templateString.replaceAll(purpose, purposeInputTemplate);
                    $(e.piiCategories).each(function (i, ee) {
                        piiCategory = "{{pii:" + purp + ":" + ee.displayName + "}}";
                        var piiCategoryMin = piiCategory.replace(/\s/g, '');
                        if (ee.mandatory == true) {
                            piiCategoryInputTemplate = '<strong><label id="' + piiCategoryMin + '" data-id="' +
                                piiCategory + '" data-piiCategoryId="' + ee.piiCategoryId + '" data-purposeId="' +
                                e.purposeId + '" data-mandetoryPiiCategory="' + ee.mandatory + '">' + ee.displayName +
                                '<span class="required_consent">*</span></label></strong>';
                        } else {
                            piiCategoryInputTemplate = '<span><label for="' + piiCategoryMin + '"><input type="checkbox" id="' + piiCategoryMin + '" data-id="' +
                                piiCategory + '" data-piiCategoryId="' + ee.piiCategoryId + '" data-purposeId="' + e.purposeId + '"' +
                                'data-mandetoryPiiCategory="' + ee.mandatory + '" name="" value="">' + ee.displayName + '</label></span>';
                        }
                        templateString = templateString.replaceAll(piiCategory, piiCategoryInputTemplate);
                    });
                });

                $(".consent-statement").html(templateString);
            }

            function renderReceiptDetailsFromRows(data) {
                var rowTemplate =
                    '{{#purposes}}' +
                    '<div class="ui bulleted list">' +
                    '<div class="item"><span>{{purpose}} {{#if description}}<i id="description" class="info circle icon" data-variation="inverted" data-content="{{description}}" data-placement="right"/>{{/if}}</span></div></div>' +
                    '<div class="ui form">' +
                    '{{#grouped_each 2 piiCategories}}' +
                    '{{#each this }}' +
                    '<div class="{{#if mandatory}}required{{/if}} field">'+
                    '<div class="ui checkbox">' +
                    '<input type="checkbox" name="switch" id="consent-checkbox-{{../../purposeId}}-{{piiCategoryId}}" {{#if mandatory}}required{{/if}} />' +
                    '<label for="consent-checkbox-{{../../purposeId}}-{{piiCategoryId}}" data-piicategoryid="{{piiCategoryId}}" data-mandetorypiicatergory="{{mandatory}}" data-purposeid="{{../../purposeId}}">' +
                    '<span>{{#if displayName}}{{displayName}}{{else}}{{piiCategory}}{{/if}}'+
                    '</label></div>' +
                    '</div>'+
                    '{{/each}}' +
                    '{{/grouped_each}}' +
                    '</div></div>' +
                    '{{/purposes}}';

                var rows = Handlebars.compile(rowTemplate);
                var rowsRendered = rows(data);

                $("#row-container").html(rowsRendered);
                $("#description").popup();
            }

        });
    </script>
</body>
</html>
