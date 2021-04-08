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
<%@ page import="org.apache.commons.lang.StringUtils" %>
<%@ page import="org.wso2.carbon.identity.mgt.endpoint.util.IdentityManagementEndpointUtil" %>
<%@ page import="org.wso2.carbon.identity.recovery.IdentityRecoveryConstants" %>
<%@ page import="org.owasp.encoder.Encode" %>
<%@ page import="java.io.File" %>
<%@ page import="java.net.URISyntaxException" %>
<jsp:directive.include file="includes/localize.jsp"/>

<%
    String errorMsg = IdentityManagementEndpointUtil.getStringValue(request.getAttribute("errorMsg"));
    String errorCode = IdentityManagementEndpointUtil.getStringValue(request.getAttribute("errorCode"));
    String callback = request.getParameter("callback");
    boolean isValidCallback = true;
    try {
        IdentityManagementEndpointUtil.getURLEncodedCallback(callback);
    } catch (URISyntaxException e) {
        isValidCallback = false;
    }
    if (StringUtils.isBlank(errorMsg)) {
        errorMsg = IdentityManagementEndpointUtil.i18n(recoveryResourceBundle, "Server.failed.to.respond");
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
                <div class="segment-form">
                    <div class="ui visible negative message" id="server-error-code">
                        <div class="header"><%=IdentityManagementEndpointUtil.i18n(recoveryResourceBundle, "error")%>!</div>
                        <p><%=IdentityManagementEndpointUtil.i18nBase64(recoveryResourceBundle, errorMsg)%></p>
                    </div>

                    <% if (isValidCallback) { %>
                    <div id="action-buttons" class="buttons">
                        <a href="javascript:goBack()" class="ui button primary button">
                            <%=IdentityManagementEndpointUtil.i18n(recoveryResourceBundle, "Go back")%>
                        </a>
                    </div>
                    <% } %>
                </div>
            </div>
        </div>
    </main>
    <!-- /content/body -->

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
        $(document).ready(function () {
            // Checks if the `callback` URL param is present, and if not, hides the `Go Back` button.
            if ("<%=StringUtils.isEmpty(callback)%>" === "true") {
                $("#action-buttons").hide();
            }
        });

        <% if (isValidCallback) { %>
        function goBack() {

            var errorCodeFromParams = "<%=errorCode%>";
            var invalidConfirmationErrorCode = "<%=IdentityRecoveryConstants.ErrorMessages.ERROR_CODE_INVALID_CODE.getCode()%>";

            // Check if the error is related to the confirmation code being invalid.
            // If so, navigate the users to the URL defined in `callback` URL param.
            if (errorCodeFromParams === invalidConfirmationErrorCode) {
                window.location.href = "<%=callback%>";

                return;
            }

            window.history.back();
        }
        <% } %>
    </script>
</body>
</html>
