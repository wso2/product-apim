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

<%@ page import="org.wso2.carbon.identity.mgt.endpoint.util.client.CallBackValidator" %>
<%@ page import="org.wso2.carbon.identity.mgt.endpoint.util.client.IdentityRecoveryException" %>
<%@ page import="org.wso2.carbon.identity.mgt.endpoint.util.IdentityManagementEndpointUtil" %>
<%@ page import="java.io.File" %>
<%@ page import="java.net.URISyntaxException" %>

<jsp:directive.include file="includes/localize.jsp"/>

<%
    String callback = (String) request.getAttribute("callback");
    String tenantDomain = (String) request.getAttribute("tenantDomain");
    boolean isUserPortalURL = (boolean) request.getAttribute("isUserPortalURL");
    CallBackValidator callBackValidator = new CallBackValidator();
    try {
        if (!callBackValidator.isValidCallbackURL(callback, tenantDomain, isUserPortalURL)) {
            request.setAttribute("error", true);
            request.setAttribute("errorMsg", "Configured callback URL does not match with the provided callback " +
                    "URL in the request.");
            request.getRequestDispatcher("error.jsp").forward(request, response);
            return;
        }
    } catch (IdentityRecoveryException e) {
        request.setAttribute("error", true);
        request.setAttribute("errorMsg", "Callback URL validation failed. " + e);
        request.getRequestDispatcher("error.jsp").forward(request, response);
        return;
    }
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
            <h4 class="modal-title"><%=IdentityManagementEndpointUtil.i18n(recoveryResourceBundle,
                    "Information")%>
            </h4>
        </div>
        <div class="content">
            <p><%=IdentityManagementEndpointUtil.i18n(recoveryResourceBundle,
                    "Username.recovery.information.sent.to.your.email")%>
            </p>
        </div>
        <div class="actions">
            <button type="button" class="ui primary button cancel" data-dismiss="modal">
                <%=IdentityManagementEndpointUtil.i18n(recoveryResourceBundle, "Close")%>
            </button>
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
            $(".notify").modal({
                blurring: true,
                closable: false,
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
                }
            }).modal("show");
        });
    </script>
</body>
</html>
