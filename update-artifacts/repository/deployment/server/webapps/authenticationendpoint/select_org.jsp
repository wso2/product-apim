<%--
  ~ Copyright (c) 2022, WSO2 LLC. (http://www.wso2.com).
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

<%@ page import="org.owasp.encoder.Encode" %>
<%@ page import="org.wso2.carbon.identity.application.authentication.endpoint.util.Constants" %>
<%@ page import="java.io.File" %>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="layout" uri="org.wso2.identity.apps.taglibs.layout.controller" %>
<%@ include file="includes/localize.jsp" %>
<%@ include file="includes/init-url.jsp" %>
<jsp:directive.include file="includes/layout-resolver.jsp"/>

<%

    String idp = request.getParameter("idp");
    String authenticator = request.getParameter("authenticator");
    String sessionDataKey = request.getParameter(Constants.SESSION_DATA_KEY);
    int orgCount = Integer.parseInt(request.getParameter("orgCount"));

    String errorMessage = AuthenticationEndpointUtil.i18n(resourceBundle, "error.retry");
    String authenticationFailed = "false";

    if (Boolean.parseBoolean(request.getParameter(Constants.AUTH_FAILURE))) {
        authenticationFailed = "true";

        if (request.getParameter(Constants.AUTH_FAILURE_MSG) != null) {
            errorMessage = request.getParameter(Constants.AUTH_FAILURE_MSG);

            if (errorMessage.equalsIgnoreCase("authentication.fail.message")) {
                errorMessage = AuthenticationEndpointUtil.i18n(resourceBundle, "error.retry");
            }
        }
    }
%>

<%-- Data for the layout from the page --%>
<%
    layoutData.put("containerSize", "medium");
%>

<html>
    <head>
        <!-- header -->
        <%
            File headerFile = new File(getServletContext().getRealPath("extensions/header.jsp"));
            if (headerFile.exists()) {
        %>
                <jsp:include page="extensions/header.jsp"/>
        <%
            } else {
        %>
                <jsp:include page="includes/header.jsp"/>
        <%
            }
        %>

        <!--[if lt IE 9]>
        <script src="js/html5shiv.min.js"></script>
        <script src="js/respond.min.js"></script>
        <![endif]-->
    </head>

    <body class="login-portal layout authentication-portal-layout">
        <layout:main layoutName="<%= layout %>" layoutFileRelativePath="<%= layoutFileRelativePath %>" data="<%= layoutData %>" >
            <layout:component componentName="ProductHeader" >
                <!-- product-title -->
                <%
                    File productTitleFile = new File(getServletContext().getRealPath("extensions/product-title.jsp"));
                    if (productTitleFile.exists()) {
                %>
                        <jsp:include page="extensions/product-title.jsp"/>
                <%
                    } else {
                %>
                        <jsp:include page="includes/product-title.jsp"/>
                <%
                    }
                %>
            </layout:component>
            <layout:component componentName="MainSection" >
                <div class="ui segment">
                    <!-- page content -->
                    <h2>Select Your Organization</h2>
                    <div class="ui divider hidden"></div>

                    <%
                        if ("true".equals(authenticationFailed)) {
                    %>
                            <div class="ui negative message" id="failed-msg"><%=Encode.forHtmlContent(errorMessage)%></div>
                            <div class="ui divider hidden"></div>
                    <%
                        }
                    %>

                    <div id="alertDiv"></div>

                    <form class="ui large form" id="pin_form" name="pin_form" action="<%=commonauthURL%>" method="GET">
                        <div class="ui segment" align="left">
                            <%
                                for (int i=1; i <= orgCount; i++) { %>
                                    <input id="orgId" type="radio" name="orgId" value="<%=Encode.forHtmlAttribute(request.getParameter("orgId_" + i))%>" required>
                                    <label><%=Encode.forHtml(request.getParameter("org_" + i))%> : </label> <label><%=Encode.forHtml(request.getParameter("orgDesc_" + i))%></label><br>
                                <%
                                }%>
                            <input id="idp" name="idp" type="hidden" value="<%=Encode.forHtmlAttribute(idp)%>"/>
                            <input id="authenticator" name="authenticator" type="hidden" value="<%=Encode.forHtmlAttribute(authenticator)%>"/>
                            <input id="sessionDataKey" name="sessionDataKey" type="hidden" value="<%=Encode.forHtmlAttribute(sessionDataKey)%>"/>
                            <div class="ui divider hidden"></div>
                            <div class="align-right buttons">
                                <button type="submit" class="ui primary large button">
                                    <%=AuthenticationEndpointUtil.i18n(resourceBundle, "Submit")%>
                                </button>
                            </div>

                        </div>
                    </form>

                </div>
            </layout:component>
            <layout:component componentName="ProductFooter" >
                <!-- product-footer -->
                <%
                    File productFooterFile = new File(getServletContext().getRealPath("extensions/product-footer.jsp"));
                    if (productFooterFile.exists()) {
                %>
                        <jsp:include page="extensions/product-footer.jsp"/>
                <%
                    } else {
                %>
                        <jsp:include page="includes/product-footer.jsp"/>
                <%
                    }
                %>
            </layout:component>
        </layout:main>

        <!-- footer -->
        <%
            File footerFile = new File(getServletContext().getRealPath("extensions/footer.jsp"));
            if (footerFile.exists()) {
        %>
                <jsp:include page="extensions/footer.jsp"/>
        <%
            } else {
        %>
                <jsp:include page="includes/footer.jsp"/>
        <%
            }
        %>
    </body>
</html>
