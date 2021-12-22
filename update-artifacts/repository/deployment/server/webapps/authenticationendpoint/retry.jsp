<%--
  ~ Copyright (c) 2014, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
  ~
  ~ WSO2 Inc. licenses this file to you under the Apache License,
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

<%@ page import="org.owasp.encoder.Encode" %>
<%@ page import="java.io.File" %>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>

<%@ include file="includes/localize.jsp" %>

<%
    String stat = request.getParameter("status");
    String statusMessage = request.getParameter("statusMsg");
    // Check the error is null or whether there is a corresponding value in the resource bundle.
    if (stat == null || statusMessage == null ||
            statusMessage.equals(AuthenticationEndpointUtil.i18n(resourceBundle, statusMessage))) {
        stat = AuthenticationEndpointUtil.i18n(resourceBundle, "authentication.error");
        statusMessage =  AuthenticationEndpointUtil.i18n(resourceBundle,
                "something.went.wrong.during.authentication");
    } else {
        stat = AuthenticationEndpointUtil.customi18n(resourceBundle, stat);
        statusMessage = AuthenticationEndpointUtil.customi18n(resourceBundle, statusMessage);
    }
    session.invalidate();
%>

<!doctype html>
<html>
<head>
    <!-- header -->
    <%
        File headerFile = new File(getServletContext().getRealPath("extensions/product-title.jsp"));
        if (headerFile.exists()) {
    %>
        <jsp:include page="extensions/header.jsp"/>
    <% } else { %>
        <jsp:directive.include file="includes/header.jsp"/>
    <% } %>
</head>
<body>
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
                <div class="segment-form">
                    <div class="ui visible negative message">
                        <div class="header"><%=Encode.forHtmlContent(stat)%></div>
                        <p><%=Encode.forHtmlContent(statusMessage)%></p>
                    </div>
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
