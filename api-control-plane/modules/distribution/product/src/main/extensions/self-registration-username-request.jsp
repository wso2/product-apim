<%--
  ~ Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

<%@ page import="org.owasp.encoder.Encode" %>
<%@ page import="org.wso2.carbon.identity.mgt.endpoint.util.client.ApiException" %>
<%@ page import="org.wso2.carbon.identity.mgt.constants.SelfRegistrationStatusCodes" %>
<%@ page import="org.wso2.carbon.identity.mgt.endpoint.util.client.api.ReCaptchaApi" %>
<%@ page import="org.wso2.carbon.identity.mgt.endpoint.util.client.model.ReCaptchaProperties" %>
<%@ page import="org.wso2.carbon.identity.mgt.endpoint.util.IdentityManagementEndpointConstants" %>
<%@ page import="org.wso2.carbon.identity.mgt.endpoint.util.IdentityManagementServiceUtil" %>
<%@ page import="org.wso2.carbon.identity.mgt.endpoint.util.client.model.User" %>
<%@ page import="org.wso2.carbon.identity.mgt.endpoint.util.IdentityManagementEndpointUtil" %>
<%@ page import="org.wso2.carbon.identity.captcha.util.CaptchaUtil" %>
<%@ page import="java.util.Arrays" %>
<%@ page import="java.util.HashMap" %>
<%@ page import="java.io.File" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.Map" %>
<%@ page import="java.util.Enumeration" %>
<%@ taglib prefix="layout" uri="org.wso2.identity.apps.taglibs.layout.controller" %>

<jsp:directive.include file="includes/localize.jsp"/>
<jsp:directive.include file="tenant-resolve.jsp"/>
<jsp:directive.include file="includes/layout-resolver.jsp"/>

<%
    boolean error = IdentityManagementEndpointUtil.getBooleanValue(request.getAttribute("error"));
    String username = request.getParameter("username");
    User user = IdentityManagementServiceUtil.getInstance().getUser(username);
    Object errorCodeObj = request.getAttribute("errorCode");
    Object errorMsgObj = request.getAttribute("errorMsg");
    String callback = Encode.forHtmlAttribute(request.getParameter("callback"));
    boolean isCallBackUrlEmpty = false;
    if (callback == null || callback.length() == 0) {
        isCallBackUrlEmpty = true;
    }
    String errorCode = null;
    String errorMsg = null;

    if (errorCodeObj != null) {
        errorCode = errorCodeObj.toString();
    }
    if (SelfRegistrationStatusCodes.ERROR_CODE_INVALID_TENANT.equalsIgnoreCase(errorCode)) {
        errorMsg = "Invalid tenant domain - " + user.getTenantDomain();
    } else if (SelfRegistrationStatusCodes.ERROR_CODE_USER_ALREADY_EXISTS.equalsIgnoreCase(errorCode)) {
        errorMsg = "Username '" + username + "' is already taken. Please pick a different username";
    } else if (SelfRegistrationStatusCodes.ERROR_CODE_SELF_REGISTRATION_DISABLED.equalsIgnoreCase(errorCode)) {
        errorMsg = "Self registration is disabled for tenant - " + user.getTenantDomain();
    } else if (SelfRegistrationStatusCodes.CODE_USER_NAME_INVALID.equalsIgnoreCase(errorCode)) {
         if (request.getAttribute("errorMessage") != null) {
            errorMsg = (String) request.getAttribute("errorMessage");
        } else {
            errorMsg = user.getUsername() + " is an invalid user name. Please pick a valid username.";
        }
    } else if (StringUtils.equalsIgnoreCase(SelfRegistrationStatusCodes.ERROR_CODE_INVALID_EMAIL_USERNAME, errorCode)) {
        errorMsg = "Username is invalid. Username should be in email format.";
    } else if (SelfRegistrationStatusCodes.ERROR_CODE_INVALID_USERSTORE.equalsIgnoreCase(errorCode)) {
        errorMsg = "Invalid user store domain - " + user.getRealm();
    } else if (errorMsgObj != null) {
        errorMsg = errorMsgObj.toString();
    }
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
            request.getRequestDispatcher("error.jsp").forward(request, response);
            return;
        }
    boolean skipSignUpEnableCheck = Boolean.parseBoolean(request.getParameter("skipsignupenablecheck"));
%>

	<%-- Data for the layout from the page --%>
<%
    layoutData.put("containerSize", "medium");
%>

<%
    boolean reCaptchaEnabled = false;
    if (request.getAttribute("reCaptcha") != null && "TRUE".equalsIgnoreCase((String) request.getAttribute("reCaptcha"))) {
        reCaptchaEnabled = true;
    } else if (request.getParameter("reCaptcha") != null && Boolean.parseBoolean(request.getParameter("reCaptcha"))) {
        reCaptchaEnabled = true;
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
    <jsp:directive.include file="includes/header.jsp"/>
    <% } %>
    <%
            if (reCaptchaEnabled) {
                String reCaptchaAPI = CaptchaUtil.reCaptchaAPIURL();
        %>
        <script src='<%=(reCaptchaAPI)%>'></script>
         <%
            }
        %>
</head>
<body class="login-portal layout recovery-layout">
     <layout:main layoutName="<%= layout %>" layoutFileRelativePath="<%= layoutFileRelativePath %>" data="<%= layoutData %>" >
        <layout:component componentName="ProductHeader" >
            <%-- product-title --%>
            <%
                File productTitleFile = new File(getServletContext().getRealPath("extensions/product-title.jsp"));
                if (productTitleFile.exists()) {
            %>
            <jsp:include page="extensions/product-title.jsp"/>
            <% } else { %>
            <jsp:directive.include file="includes/product-title.jsp"/>
            <% } %>
        </layout:component>
        <layout:component componentName="MainSection" >
            <div class="ui segment">
                <h2>
                    <%=IdentityManagementEndpointUtil.i18n(recoveryResourceBundle, "Initiate.sign.up")%>
                </h2>

                <div class="ui negative message" id="error-msg" hidden="hidden">
                </div>
                <% if (error) { %>
                <div class="ui negative message" id="server-error-msg">
                    <%=IdentityManagementEndpointUtil.i18nBase64(recoveryResourceBundle, errorMsg)%>
                </div>
                <% } %>
                <%-- validation --%>
                <p>
                    <%=IdentityManagementEndpointUtil.i18n(recoveryResourceBundle, "Enter.your.username.here")%>
                </p>
                <div class="ui divider hidden"></div>
                <div class="segment-form">
                    <form class="ui large form" action="signup.do" method="post" id="register">

                        <div class="field">
                            <label class="control-label"
                                   for="username"><%=IdentityManagementEndpointUtil.i18n(recoveryResourceBundle, "Username")%>
                            </label>
                            <input id="username" name="username" type="text"
                                   required class="form-control"
                                <% if(skipSignUpEnableCheck) {%> value="<%=Encode.forHtmlAttribute(username)%>" <%}%>>
                        </div>
                        <p class="ui tiny compact info message">
                            <i class="icon info circle"></i>
                            <%=IdentityManagementEndpointUtil.i18n(recoveryResourceBundle,
                                    "If.you.specify.tenant.domain.you.registered.under.super.tenant")%>
                        </p>
                        <input id="callback" name="callback" type="hidden" value="<%=callback%>"
                               class="form-control" required>

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

                        <%
                            if (reCaptchaEnabled) {
                                String reCaptchaKey = CaptchaUtil.reCaptchaSiteKey();
                        %>
                        <div class="field">
                            <div class="g-recaptcha"
                                data-size="invisible"
                                data-callback="onCompleted"
                                data-action="register"
                                data-sitekey="<%=Encode.forHtmlContent(reCaptchaKey)%>"
                            >
                            </div>
                        </div>
                        <%
                            }
                        %>

                        <div class="ui divider hidden"></div>

                        <div class="align-right buttons">
                             <% if (!isCallBackUrlEmpty) { %>
                            <a id="goBack"
                               href='<%=Encode.forHtmlAttribute(request.getParameter("callback"))%>'
                               class="ui button link-button">
                            <% } else { %>
                            <a id="goBack" onclick="window.history.back()" class="ui button link-button">
                            <% } %>
                                <%=IdentityManagementEndpointUtil.i18n(recoveryResourceBundle, "Cancel")%>
                            </a>
                            <button id="registrationSubmit"
                                    class="ui primary button"
                                    type="submit">
                                <%=IdentityManagementEndpointUtil.i18n(recoveryResourceBundle,
                                        "Proceed.to.self.register")%>
                            </button>
                        </div>
                    </form>
                </div>
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
            <jsp:directive.include file="includes/product-footer.jsp"/>
            <% } %>
        </layout:component>
    </layout:main>

    <%-- footer --%>
    <%
        File footerFile = new File(getServletContext().getRealPath("extensions/footer.jsp"));
        if (footerFile.exists()) {
    %>
    <jsp:include page="extensions/footer.jsp"/>
    <% } else { %>
    <jsp:directive.include file="includes/footer.jsp"/>
    <% } %>
    
    <script>
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
        function onCompleted() {
            $('#register').submit();
         }
        function goBack() {
            window.history.back();
        }
        // Handle form submission preventing double submission.
        $(document).ready(function(){
            $.fn.preventDoubleSubmission = function() {
                $(this).on("submit", function(e){
                    var $form = $(this);
                    $("#error-msg").hide(); 
                    $("#server-error-msg").hide();
                    $("#error-msg").text("");
                    if ($form.data("submitted") === true) {
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
                        var normalizedUsername = userName.value.trim();
                        userName.value = normalizedUsername;
                        
                        if (normalizedUsername) {
                            if (!/^[^/].*[^@]$/g.test(normalizedUsername)) {
                                $("#error-msg").text("Username pattern policy violated");
                                $("#error-msg").show();
                                $("#username").val("");
                                return;
                            }
                        }
                        // Mark it so that the next submit can be ignored.
                        $form.data("submitted", true);
                        document.getElementById("register").submit();
                    }
                });
                return this;
            };
            $registerForm.preventDoubleSubmission();
        });
    </script>

</body>
</html>
