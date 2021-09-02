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
<%@ page import="org.wso2.carbon.identity.mgt.constants.SelfRegistrationStatusCodes" %>
<%@ page import="org.wso2.carbon.identity.mgt.endpoint.util.IdentityManagementEndpointConstants" %>
<%@ page import="org.wso2.carbon.identity.mgt.endpoint.util.IdentityManagementServiceUtil" %>
<%@ page import="org.wso2.carbon.identity.mgt.endpoint.util.client.model.User" %>
<%@ page import="org.wso2.carbon.identity.mgt.endpoint.util.IdentityManagementEndpointUtil" %>
<%@ page import="java.io.File" %>
<%@ page import="java.util.Map" %>

<jsp:directive.include file="includes/localize.jsp"/>

<%
    boolean error = IdentityManagementEndpointUtil.getBooleanValue(request.getAttribute("error"));
    String username = request.getParameter("username");
    User user = IdentityManagementServiceUtil.getInstance().getUser(username);
    Object errorCodeObj = request.getAttribute("errorCode");
    Object errorMsgObj = request.getAttribute("errorMsg");
    String callback = Encode.forHtmlAttribute(request.getParameter("callback"));
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
        errorMsg = user.getUsername() + " is an invalid user name. Please pick a valid username.";
    } else if (StringUtils.equalsIgnoreCase(SelfRegistrationStatusCodes.ERROR_CODE_INVALID_EMAIL_USERNAME, errorCode)) {
        errorMsg = "Username is invalid. Username should be in email format.";
    } else if (errorMsgObj != null) {
        errorMsg = errorMsgObj.toString();
    }
    boolean skipSignUpEnableCheck = Boolean.parseBoolean(request.getParameter("skipsignupenablecheck"));
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
    <jsp:directive.include file="includes/header.jsp"/>
    <% } %>
</head>
<body class="login-portal layout recovery-layout">
    <main class="center-segment">
        <div class="ui container medium center aligned middle aligned">
            <!-- product-title -->
            <%
                File productTitleFile = new File(getServletContext().getRealPath("extensions/product-title.jsp"));
                if (productTitleFile.exists()) {
            %>
            <jsp:include page="extensions/product-title.jsp"/>
            <% } else { %>
            <jsp:directive.include file="includes/product-title.jsp"/>
            <% } %>
            <div class="ui segment">
                <h2>
                    <%=IdentityManagementEndpointUtil.i18n(recoveryResourceBundle, "Start.signing.up")%>
                </h2>

                <div class="ui negative message" id="error-msg" hidden="hidden">
                </div>
                <% if (error) { %>
                <div class="ui negative message" id="server-error-msg">
                    <%=IdentityManagementEndpointUtil.i18nBase64(recoveryResourceBundle, errorMsg)%>
                </div>
                <% } %>
                <!-- validation -->
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
                                String value = Encode.forHtmlAttribute(entry.getValue()[0]); %>
                        <div class="field">
                            <input id="<%= key%>" name="<%= key%>" type="hidden"
                                   value="<%=value%>" class="form-control">
                        </div>
                        <% } %>

                        <div class="ui divider hidden"></div>

                        <div class="align-right buttons">
                            <a id="goBack" onclick="window.history.back()"
                               class="ui button link-button"
                            >
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
        </div>
    </main>
    <!-- product-footer -->
    <%
        File productFooterFile = new File(getServletContext().getRealPath("extensions/product-footer.jsp"));
        if (productFooterFile.exists()) {
    %>
    <jsp:include page="extensions/product-footer.jsp"/>
    <% } else { %>
    <jsp:directive.include file="includes/product-footer.jsp"/>
    <% } %>

    <!-- footer -->
    <%
        File footerFile = new File(getServletContext().getRealPath("extensions/footer.jsp"));
        if (footerFile.exists()) {
    %>
    <jsp:include page="extensions/footer.jsp"/>
    <% } else { %>
    <jsp:directive.include file="includes/footer.jsp"/>
    <% } %>

</body>
</html>
