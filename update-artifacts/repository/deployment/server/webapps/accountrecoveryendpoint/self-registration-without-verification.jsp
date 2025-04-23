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
<%@ page import="com.google.gson.Gson" %>
<%@ page import="org.apache.commons.lang.StringUtils" %>
<%@ page import="org.apache.cxf.jaxrs.impl.ResponseImpl" %>
<%@ page import="org.owasp.encoder.Encode" %>
<%@ page import="org.wso2.carbon.identity.captcha.util.CaptchaUtil" %>
<%@ page import="org.wso2.carbon.identity.mgt.endpoint.util.IdentityManagementEndpointConstants" %>
<%@ page import="org.wso2.carbon.identity.mgt.endpoint.util.IdentityManagementEndpointUtil" %>
<%@ page import="org.wso2.carbon.identity.mgt.endpoint.util.serviceclient.UserRegistrationClient" %>
<%@ page import="org.wso2.carbon.identity.mgt.endpoint.util.serviceclient.beans.Claim" %>
<%@ page import="java.io.File" %>
<%@ page import="javax.ws.rs.core.Response" %>
<jsp:directive.include file="includes/localize.jsp"/>

<%
    boolean error = IdentityManagementEndpointUtil.getBooleanValue(request.getAttribute("error"));
    String errorMsg = IdentityManagementEndpointUtil.getStringValue(request.getAttribute("errorMsg"));


    boolean isFirstNameInClaims = true;
    boolean isFirstNameRequired = true;
    boolean isLastNameInClaims = true;
    boolean isLastNameRequired = true;
    boolean isEmailInClaims = true;
    boolean isEmailRequired = true;

    Claim[] claims = new Claim[0];

    UserRegistrationClient userRegistrationClient = new UserRegistrationClient();
    Response responseForAllClaims = userRegistrationClient.getAllClaims(null);
    if(responseForAllClaims != null && Response.Status.OK.getStatusCode() == responseForAllClaims.getStatus()) {
        String claimsContent = responseForAllClaims.readEntity(String.class);
        Gson gson = new Gson();
        claims = gson.fromJson(claimsContent, Claim[].class);
    }
    if(((ResponseImpl)responseForAllClaims).getHeaders().containsKey("reCaptcha") &&
            Boolean.parseBoolean((String) ((ResponseImpl)responseForAllClaims).getHeaders().get("reCaptcha").get(0))) {
        request.setAttribute("reCaptcha", "true");
        request.setAttribute("reCaptchaKey", ((ResponseImpl)responseForAllClaims).getHeaders().get("reCaptchaKey").get(0));
        request.setAttribute("reCaptchaAPI", ((ResponseImpl)responseForAllClaims).getHeaders().get("reCaptchaAPI").get(0));
    }

%>
<%
    boolean reCaptchaEnabled = false;
    if (request.getAttribute("reCaptcha") != null && "TRUE".equalsIgnoreCase((String) request.getAttribute("reCaptcha"))) {
        reCaptchaEnabled = true;
    } else if (request.getParameter("reCaptcha") != null && Boolean.parseBoolean(request.getParameter("reCaptcha"))) {
        reCaptchaEnabled = true;
    }
%>

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
        <link href="libs/bootstrap_5.3.5/css/bootstrap.min.css" rel="stylesheet">
        <link href="css/Roboto.css" rel="stylesheet">
        <link href="css/custom-common.css" rel="stylesheet">

        <!--[if lt IE 9]>
        <script src="js/html5shiv.min.js"></script>
        <script src="js/respond.min.js"></script>
        <![endif]-->

        <%
            if (reCaptchaEnabled) {
                String reCaptchaAPI = CaptchaUtil.reCaptchaAPIURL();
        %>
        <script src='<%=(reCaptchaAPI)%>'></script>
        <%
            }
        %>

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

        <div class="row">
            <!-- content -->
            <div class="col-xs-12 col-sm-10 col-md-8 col-lg-5 col-centered wr-login">
                <form action="processregistration.do" method="post" id="register">
                    <h2 class="wr-title uppercase blue-bg padding-double white boarder-bottom-blue margin-none">
                        <%=IdentityManagementEndpointUtil.i18n(recoveryResourceBundle, "Create.an.account")%>
                    </h2>

                    <div class="clearfix"></div>
                    <div class="boarder-all ">

                        <% if (error) { %>
                        <div class="alert alert-danger" id="server-error-msg">
                            <%=IdentityManagementEndpointUtil.i18nBase64(recoveryResourceBundle, errorMsg)%>
                        </div>
                        <% } %>

                        <div class="alert alert-danger" id="error-msg" hidden="hidden">
                        </div>

                        <div class="padding-double font-large"><%=IdentityManagementEndpointUtil.i18n(recoveryResourceBundle,
                                "Enter.required.fields.to.complete.registration")%>
                        </div>
                        <!-- validation -->
                        <div class="padding-double">
                            <div id="regFormError" class="alert alert-danger" style="display:none"></div>
                            <div id="regFormSuc" class="alert alert-success" style="display:none"></div>

                            <% if (isFirstNameInClaims) { %>
                            <div class="col-xs-12 col-sm-12 col-md-6 col-lg-6 form-group required">
                                <label class="control-label"><%=IdentityManagementEndpointUtil.i18n(recoveryResourceBundle,
                                        "First.name")%>
                                </label>
                                <input type="text" name="http://wso2.org/claims/givenname" class="form-control"
                                    <% if (isFirstNameRequired) {%> required <%}%>>
                            </div>
                            <%}%>

                            <% if (isLastNameInClaims) { %>
                            <div class="col-xs-12 col-sm-12 col-md-6 col-lg-6 form-group required">
                                <label class="control-label"><%=IdentityManagementEndpointUtil.i18n(recoveryResourceBundle,
                                        "Last.name")%>
                                </label>
                                <input type="text" name="http://wso2.org/claims/lastname" class="form-control"
                                    <% if (isLastNameRequired) {%> required <%}%>>
                            </div>
                            <%}%>

                            <div class="col-xs-12 col-sm-12 col-md-12 col-lg-12 form-group required">
                                <label class="control-label"><%=IdentityManagementEndpointUtil.i18n(recoveryResourceBundle,
                                        "Username")%>
                                </label>
                                <input id="username" name="username" type="text"
                                       class="form-control required usrName usrNameLength" required>
                            </div>

                            <div class="col-xs-12 col-sm-12 col-md-6 col-lg-6 form-group required">
                                <label class="control-label"><%=IdentityManagementEndpointUtil.i18n(recoveryResourceBundle,
                                        "Password")%>
                                </label>
                                <input id="password" name="password" type="password"
                                       class="form-control" required>
                            </div>

                            <div class="col-xs-12 col-sm-12 col-md-6 col-lg-6 form-group required">
                                <label class="control-label"><%=IdentityManagementEndpointUtil.i18n(recoveryResourceBundle,
                                        "Confirm.password")%>
                                </label>
                                <input id="password2" name="password2" type="password" class="form-control"
                                       data-match="reg-password" required>
                            </div>

                            <% if (isEmailInClaims) { %>
                            <div class="col-xs-12 col-sm-12 col-md-12 col-lg-12 form-group required">
                                <label class="control-label"><%=IdentityManagementEndpointUtil.i18n(recoveryResourceBundle,
                                        "Email")%>
                                </label>
                                <input type="email" name="http://wso2.org/claims/emailaddress" data-claim-uri="http://wso2.org/claims/emailaddress"
                                       class="form-control" data-validate="email"
                                    <% if (isEmailRequired) {%> required <%}%>>
                            </div>
                            <%}%>

                            <% for (Claim claim : claims) {
                                if (!StringUtils.equals(claim.getClaimUri(), IdentityManagementEndpointConstants.ClaimURIs.FIRST_NAME_CLAIM) &&
                                    !StringUtils.equals(claim.getClaimUri(), IdentityManagementEndpointConstants.ClaimURIs.LAST_NAME_CLAIM) &&
                                    !StringUtils.equals(claim.getClaimUri(), IdentityManagementEndpointConstants.ClaimURIs.EMAIL_CLAIM) &&
                                    !StringUtils.equals(claim.getClaimUri(), IdentityManagementEndpointConstants.ClaimURIs.CHALLENGE_QUESTION_URI_CLAIM) &&
                                    !StringUtils.equals(claim.getClaimUri(), IdentityManagementEndpointConstants.ClaimURIs.CHALLENGE_QUESTION_1_CLAIM) &&
                                    !StringUtils.equals(claim.getClaimUri(), IdentityManagementEndpointConstants.ClaimURIs.CHALLENGE_QUESTION_2_CLAIM) &&
                                    !claim.isReadOnly()) {
                            %>
                            <div class="col-xs-12 col-sm-12 col-md-12 col-lg-12 form-group required">
                                <label <% if (claim.isRequired()) {%> class="control-label" <%}%>>
                                    <%=IdentityManagementEndpointUtil.i18nBase64(recoveryResourceBundle, claim.getDisplayTag())%>
                                </label>
                                <input name="<%= Encode.forHtmlAttribute(claim.getClaimUri()) %>"
                                       data-claim-uri="<%= Encode.forHtmlAttribute(claim.getClaimUri()) %>"
                                       class="form-control"
                                    <% if (claim.isRequired()) {%> required <%}%>>
                            </div>
                            <%
                                    }
                                }
                            %>
                            <%
                                if (reCaptchaEnabled) {
                                    String reCaptchaKey = CaptchaUtil.reCaptchaSiteKey();
                            %>
                            <div class="field">
                                <div class="g-recaptcha"
                                        data-size="invisible"
                                        data-callback="onCompleted"
                                        data-action="register"
                                        data-sitekey=
                                                "<%=Encode.forHtmlContent(reCaptchaKey)%>">
                                </div>
                            </div>
                            <%
                                }
                            %>

                            <div class="col-xs-12 col-sm-12 col-md-12 col-lg-12 form-group">
                                <input id="isSelfRegistrationWithVerification" type="hidden"
                                       name="isSelfRegistrationWithVerification"
                                       value="false"/>
                            </div>

                            <div class="col-xs-12 col-sm-12 col-md-12 col-lg-12 form-group">
                                <br/>
                                <button id="registrationSubmit"
                                        class="wr-btn grey-bg col-xs-12 col-md-12 col-lg-12 uppercase font-extra-large"
                                        type="submit"><%=IdentityManagementEndpointUtil.i18n(recoveryResourceBundle, "Next")%>
                                </button>
                            </div>
                            <div class="col-xs-12 col-sm-12 col-md-12 col-lg-12 form-group">
                                <span class="margin-top padding-top-double font-large">
                                    <%=IdentityManagementEndpointUtil.i18n(recoveryResourceBundle, "Already.have.an.account")%>
                                </span>
                                <a href="<%=Encode.forHtmlAttribute(IdentityManagementEndpointUtil.getUserPortalUrl(
                                    application.getInitParameter(IdentityManagementEndpointConstants.ConfigConstants.USER_PORTAL_URL)))%>"
                                   id="signInLink" class="font-large">
                                    <%=IdentityManagementEndpointUtil.i18n(recoveryResourceBundle, "Sign.in")%>
                                </a>
                            </div>
                            <div class="clearfix"></div>
                        </div>
                    </div>
                </form>
            </div>
        </div>
        <!-- /content/body -->

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

    <script src="libs/jquery_3.6.0/jquery-3.6.0.min.js"></script>
    <script src="libs/bootstrap_5.3.5/js/bootstrap.min.js"></script>
    <script type="text/javascript">

        function onSubmit(token) {
           $("#register").submit();
        }

        function onCompleted() {
            $('#register').submit();
        }

        $(document).ready(function () {

            $("#register").submit(function (e) {

                <%
                    if (reCaptchaEnabled) {
                %>
                if (!grecaptcha.getResponse()) {
                    e.preventDefault();
                    grecaptcha.execute();

                    return true;
                }
                <%
                    }
                %>

                var unsafeCharPattern = /[<>`\"]/;
                var elements = document.getElementsByTagName("input");
                var invalidInput = false;
                var error_msg = $("#error-msg");

                for (i = 0; i < elements.length; i++) {
                    if (elements[i].type === 'text' && elements[i].value != null
                        && elements[i].value.match(unsafeCharPattern) != null) {
                        error_msg.text("<%=IdentityManagementEndpointUtil.i18n(recoveryResourceBundle,"For.security.following.characters.restricted")%>");
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

                if (password != password2) {
                    error_msg.text("<%=IdentityManagementEndpointUtil.i18n(recoveryResourceBundle,
                    "Passwords.did.not.match.please.try.again")%>");
                    error_msg.show();
                    $("html, body").animate({scrollTop: error_msg.offset().top}, 'slow');
                    return false;
                }

                return true;
            });
        });
    </script>
    </body>
    </html>
