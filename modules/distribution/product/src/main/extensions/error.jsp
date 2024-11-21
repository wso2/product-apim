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

<%@ page isErrorPage="true" %>
<%@ page import="org.owasp.encoder.Encode" %>
<%@ page import="org.apache.commons.lang.StringUtils" %>
<%@ page import="org.wso2.carbon.identity.event.IdentityEventException" %>
<%@ page import="org.wso2.carbon.identity.mgt.endpoint.util.IdentityManagementEndpointUtil" %>
<%@ page import="org.wso2.carbon.identity.recovery.IdentityRecoveryConstants" %>
<%@ page import="org.wso2.carbon.identity.recovery.util.Utils" %>
<%@ page import="org.owasp.encoder.Encode" %>
<%@ page import="java.io.File" %>
<%@ page import="java.net.URISyntaxException" %>
<%@ taglib prefix="layout" uri="org.wso2.identity.apps.taglibs.layout.controller" %>
<jsp:directive.include file="includes/localize.jsp"/>
<jsp:directive.include file="includes/layout-resolver.jsp"/>

<%
    String errorMsg = IdentityManagementEndpointUtil.getStringValue(request.getAttribute("errorMsg"));
    String errorCode = IdentityManagementEndpointUtil.getStringValue(request.getAttribute("errorCode"));
    String invalidConfirmationErrorCode = IdentityRecoveryConstants.ErrorMessages.ERROR_CODE_INVALID_CODE.getCode();
    String callback = request.getParameter("callback");
    boolean isValidCallback = true;

    if (invalidConfirmationErrorCode.equals(errorCode)) {
        String tenantDomain = StringUtils.EMPTY;
        if (StringUtils.isNotBlank(request.getParameter("tenantdomain"))){
            tenantDomain = request.getParameter("tenantdomain").trim();
        } else if (StringUtils.isNotBlank(request.getParameter("tenantDomain"))){
            tenantDomain = request.getParameter("tenantDomain").trim();
        }
        try {
            if (StringUtils.isNotBlank(callback) && !Utils.validateCallbackURL
                (callback, tenantDomain, IdentityRecoveryConstants.ConnectorConfig.RECOVERY_CALLBACK_REGEX)) {
                    isValidCallback = false;
                }
        } catch (IdentityEventException e) {
            isValidCallback = false;
        }
    }

    try {
        IdentityManagementEndpointUtil.getURLEncodedCallback(callback);
    } catch (URISyntaxException e) {
        isValidCallback = false;
    }
    if (StringUtils.isBlank(errorMsg)) {
        errorMsg = IdentityManagementEndpointUtil.i18n(recoveryResourceBundle, "Server.failed.to.respond");
    }
%>
<%-- Data for the layout from the page --%>
<%
    layoutData.put("containerSize", "large");
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
            <jsp:include page="includes/product-title.jsp"/>
            <% } %>
        </layout:component>
        <layout:component componentName="MainSection" >
            <div class="ui segment">
                <div class="segment-form">
                    <div class="ui visible negative message" id="server-error-code">
                        <div class="header"><%=IdentityManagementEndpointUtil.i18n(recoveryResourceBundle, "error")%>!</div>
                        <%
                            if (IdentityRecoveryConstants.ErrorMessages.ERROR_CODE_INVALID_CODE.getCode()
                                    .equals(errorCode)) {
                        %>
                        <p><%=IdentityManagementEndpointUtil.i18n(recoveryResourceBundle, "Invalid.reset.link")%></p>
                        <%
                        } else {
                        %>
                        <p><%=IdentityManagementEndpointUtil.i18nBase64(recoveryResourceBundle, errorMsg)%></p>
                        <%
                            }
                        %>
                    </div>

                    <div id="action-buttons" class="buttons">
                        <a id = "go-back-button" href="javascript:goBack()" class="ui button primary button">
                            <%=IdentityManagementEndpointUtil.i18n(recoveryResourceBundle, "Go back")%>
                        </a>
                    </div>
                </div>
            </div>
        </layout:component>
        <layout:component componentName="ProductFooter" >
            <%-- product-footer --%>
            <%
                File productFooterFile = new File(getServletContext().getRealPath("extensions/product-footer.jsp"));
                if (productFooterFile.exists()) {
            %>
                <jsp:include page="extensions/product-footer.jsp" />
            <% } else { %>
                <jsp:include page="includes/product-footer.jsp" />
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
    <jsp:include page="includes/footer.jsp"/>
    <% } %>

    <script>
        $(document).ready(function () {
            // Checks if the `callback` URL param is present, and if not, hides the `Go Back` button.
            if ("<%=StringUtils.isEmpty(callback)%>" === "true") {
                $("#action-buttons").hide();
            }
            if ("<%=isValidCallback%>" === "false") {
                $("#go-back-button").addClass("disabled");
                $("#action-buttons").attr("title", "Request has an invalid callback URL.");
            }
        });

        <% if (isValidCallback) { %>
        function goBack() {

            var errorCodeFromParams = "<%=errorCode%>";
            var invalidConfirmationErrorCode = "<%=invalidConfirmationErrorCode%>";

            // Check if the error is related to the confirmation code being invalid.
            // If so, navigate the users to the URL defined in `callback` URL param.
            if (errorCodeFromParams === invalidConfirmationErrorCode) {
                window.location.href = "<%=Encode.forHtmlAttribute(callback)%>";

                return;
            }

            window.history.back();
        }
        <% } %>
    </script>
</body>
</html>
