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

<%@ page import="org.apache.commons.lang.StringUtils" %>
<%@ page import="org.wso2.carbon.identity.mgt.endpoint.util.IdentityManagementEndpointConstants" %>
<%@ page import="org.wso2.carbon.identity.mgt.endpoint.util.IdentityManagementEndpointUtil" %>
<%@ page import="java.net.URISyntaxException" %>
<%@ page import="java.io.File" %>

<jsp:directive.include file="includes/localize.jsp"/>
<%
    boolean isEmailNotificationEnabled = false;
    String callback = (String) request.getAttribute("callback");
    if (StringUtils.isBlank(callback)) {
        callback = IdentityManagementEndpointUtil.getUserPortalUrl(
                application.getInitParameter(IdentityManagementEndpointConstants.ConfigConstants.USER_PORTAL_URL));
    }
    String confirm = (String) request.getAttribute("confirm");
    isEmailNotificationEnabled = Boolean.parseBoolean(application.getInitParameter(
            IdentityManagementEndpointConstants.ConfigConstants.ENABLE_EMAIL_NOTIFICATION));
%>

<!doctype html>
<html>
<head>
    <%
        File headerFile = new File(getServletContext().getRealPath("extensions/header.jsp"));
        if (headerFile.exists()) {
    %>
    <jsp:include page="extensions/header.jsp"/>
    <% } else { %>
    <jsp:directive.include file="includes/header.jsp"/>
    <% } %>
</head>
<body>
    <div class="ui tiny modal notify">
        <div class="header">
            <h4>
                <%=IdentityManagementEndpointUtil.i18n(recoveryResourceBundle, "Information")%>
            </h4>
        </div>
        <div class="content">
            <% if (StringUtils.isNotBlank(confirm) && confirm.equals("true")) {%>
            <p><%=IdentityManagementEndpointUtil.i18n(recoveryResourceBundle, "Successfully.confirmed")%>
            </p>
            <%
            } else {
                if (isEmailNotificationEnabled) {
            %>
            <p><%=IdentityManagementEndpointUtil.i18n(recoveryResourceBundle, "Confirmation.sent.to.mail")%>
            </p>
            <% } else {%>
            <p><%=IdentityManagementEndpointUtil.i18n(recoveryResourceBundle,
                    "User.registration.completed.successfully")%>
            </p>
            <%
                    }
                }
            %>
        </div>
        <div class="actions">
            <button type="button" class="ui primary button cancel" data-dismiss="modal">
                <%=IdentityManagementEndpointUtil.i18n(recoveryResourceBundle, "Close")%>
            </button>
        </div>
    </div>
</div>

<!-- footer -->
<%
    File footerFile = new File(getServletContext().getRealPath("extensions/footer.jsp"));
    if (footerFile.exists()) {
%>
<jsp:include page="extensions/footer.jsp"/>
<% } else { %>
<jsp:directive.include file="includes/footer.jsp"/>
<% } %>

    <script type="application/javascript">
        $(document).ready(function () {
            $('.notify').modal({
                onHide: function () {
                    <%
                        try {
                    %>
                    location.href = "<%= IdentityManagementEndpointUtil.getURLEncodedCallback(callback)%>";
                    <%
                    } catch (URISyntaxException e) {
                        request.setAttribute("error", true);
                        request.setAttribute("errorMsg", "Invalid callback URL found in the request.");
                        request.getRequestDispatcher("error.jsp").forward(request, response);
                        return;
                    }
                    %>
                },
                blurring: true,
                detachable: true,
                closable: false,
                centered: true,
            }).modal("show");
        });
    </script>
    </body>
</html>
