<%--
  ~ Copyright (c) 2026, WSO2 LLC. (https://www.wso2.com).
  ~
  ~ WSO2 LLC. licenses this file to you under the Apache License,
  ~ Version 2.0 (the "License"); you may not use this file except
  ~ in compliance with the License.
  ~ You may obtain a copy of the License at
  ~
  ~ http://www.apache.org/licenses/LICENSE-2.0
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
<%@ page import="org.wso2.carbon.identity.application.authentication.endpoint.util.AuthenticationEndpointUtil" %>
<%@ page import="org.wso2.carbon.identity.mgt.endpoint.util.client.CallBackValidator" %>
<%@ page import="org.wso2.carbon.identity.mgt.endpoint.util.client.IdentityRecoveryException" %>
<%@ page import="org.wso2.carbon.identity.mgt.endpoint.util.IdentityManagementEndpointUtil" %>
<%@ page import="java.io.File" %>
<%@ page import="java.net.URISyntaxException" %>
<%@ taglib prefix="layout" uri="org.wso2.identity.apps.taglibs.layout.controller" %>

<jsp:directive.include file="includes/localize.jsp"/>
<jsp:directive.include file="includes/layout-resolver.jsp"/>

<%
    String callback = (String) request.getAttribute("callback");
    String tenantDomain = (String) request.getAttribute("tenantDomain");
    String username = request.getParameter("username");
    CallBackValidator callBackValidator = new CallBackValidator();
    try {
        if (!callBackValidator.isValidCallbackURL(callback, tenantDomain)) {
            request.setAttribute("error", true);
            request.setAttribute("errorMsg", "Configured callback URL does not match with the provided callback " +
                    "URL in the request.");
            if (!StringUtils.isBlank(username)) {
                request.setAttribute("username", username);
            }
            request.getRequestDispatcher("error.jsp").forward(request, response);
            return;
        }
    } catch (IdentityRecoveryException e) {
        request.setAttribute("error", true);
        request.setAttribute("errorMsg", "Callback URL validation failed. " + e.getMessage());
        if (!StringUtils.isBlank(username)) {
            request.setAttribute("username", username);
        }
        request.getRequestDispatcher("error.jsp").forward(request, response);
        return;
    }
%>

<%-- Data for the layout from the page --%>
<%
    layoutData.put("containerSize", "medium");
%>

<!doctype html>
<html lang="en-US">
    <head>
        <%
            File headerFile = new File(getServletContext().getRealPath("extensions/header.jsp"));
            if (headerFile.exists()) {
        %>
        <jsp:include page="extensions/header.jsp"/>
        <% } else { %>
        <jsp:include page="includes/header.jsp"/>
        <% } %>
    </head>
    <body>
        <layout:main layoutName="<%= layout %>" layoutFileRelativePath="<%= layoutFileRelativePath %>"
                     data="<%= layoutData %>">
            <layout:component componentName="ProductHeader">

            </layout:component>
            <layout:component componentName="MainSection">

            </layout:component>
            <layout:component componentName="ProductFooter">

            </layout:component>
        </layout:main>

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

        <%-- footer --%>
        <%
            File footerFile = new File(getServletContext().getRealPath("extensions/footer.jsp"));
            if (footerFile.exists()) {
        %>
        <jsp:include page="extensions/footer.jsp"/>
        <% } else { %>
        <jsp:include page="includes/footer.jsp"/>
        <% } %>

        <script type="application/javascript">

            $(document).ready(function () {
                $(".notify").modal({
                    blurring: true,
                    closable: false,
                    onHide: function () {
                        <%
                        try {
                            if (callback != null && AuthenticationEndpointUtil.isSchemeSafeURL(callback)) {
                        %>
                        location.href = "<%= IdentityManagementEndpointUtil.getURLEncodedCallback(callback)%>";
                        <%
                        }
                        } catch (URISyntaxException e) {
                            request.setAttribute("error", true);
                            request.setAttribute("errorMsg", "Invalid callback URL found in the request.");
                            if (!StringUtils.isBlank(username)) {
                                request.setAttribute("username", username);
                            }
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
