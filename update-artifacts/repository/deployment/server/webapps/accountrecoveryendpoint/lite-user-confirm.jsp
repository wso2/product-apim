<%--
  ~ Copyright (c) 2020, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
<%@ page import="org.wso2.carbon.base.MultitenantConstants" %>
<%@ page import="org.wso2.carbon.identity.mgt.endpoint.util.IdentityManagementEndpointConstants" %>
<%@ page import="org.wso2.carbon.identity.mgt.endpoint.util.IdentityManagementEndpointUtil" %>
<%@ page import="org.wso2.carbon.identity.recovery.IdentityRecoveryConstants" %>
<%@ page import="org.wso2.carbon.identity.base.IdentityRuntimeException" %>
<%@ page import="org.wso2.carbon.identity.recovery.util.Utils" %>
<%@ page import="org.wso2.carbon.identity.mgt.endpoint.util.client.ApiException" %>
<%@ page import="org.wso2.carbon.identity.mgt.endpoint.util.client.api.LiteRegisterApi" %>
<%@ page import="org.wso2.carbon.identity.mgt.endpoint.util.client.model.CodeValidationRequest" %>
<%@ page import="org.wso2.carbon.identity.mgt.endpoint.util.client.model.CodeIntrospectResponse" %>
<%@ page import="org.wso2.carbon.identity.mgt.endpoint.util.client.model.User" %>
<%@ page import="org.wso2.carbon.identity.mgt.endpoint.util.client.model.Property" %>
<%@ page import="org.wso2.carbon.identity.core.util.IdentityTenantUtil" %>
<%@ page import="java.io.File" %>
<%@ page import="java.util.ArrayList" %>
<%@ page import="java.util.List" %>

<jsp:directive.include file="includes/localize.jsp"/>
<jsp:directive.include file="tenant-resolve.jsp"/>

<%
    boolean error = IdentityManagementEndpointUtil.getBooleanValue(request.getAttribute("error"));
    String errorMsg = IdentityManagementEndpointUtil.getStringValue(request.getAttribute("errorMsg"));

    String confirmationKey = request.getParameter("confirmation");
    String callback = request.getParameter("callback");

    try {
        if (StringUtils.isNotBlank(callback) && !Utils.validateCallbackURL(callback, tenantDomain,
            IdentityRecoveryConstants.ConnectorConfig.LITE_REGISTRATION_CALLBACK_REGEX)) {
            request.setAttribute("error", true);
            request.setAttribute("errorMsg", IdentityManagementEndpointUtil.i18n(recoveryResourceBundle,
                "Callback.url.format.invalid"));
            request.getRequestDispatcher("error.jsp").forward(request, response);
            return;
        }
    } catch (IdentityRuntimeException e) {
        request.setAttribute("error", true);
        request.setAttribute("errorMsg", e.getMessage());
        request.getRequestDispatcher("error.jsp").forward(request, response);
        return;
    }
    
    if (StringUtils.isBlank(callback)) {
        callback = IdentityManagementEndpointUtil.getUserPortalUrl(
                application.getInitParameter(IdentityManagementEndpointConstants.ConfigConstants.USER_PORTAL_URL), tenantDomain);
    }

    if (StringUtils.isBlank(confirmationKey)) {
        confirmationKey = IdentityManagementEndpointUtil.getStringValue(request.getAttribute("confirmationKey"));
    }
    String message = "" ;

    LiteRegisterApi liteRegisterApi = new LiteRegisterApi();
    try {
        List<Property> properties = new ArrayList<>();
        Property tenantDomainProperty = new Property();
        tenantDomainProperty.setKey(MultitenantConstants.TENANT_DOMAIN);
        tenantDomainProperty.setValue(tenantDomain);
        properties.add(tenantDomainProperty);

        CodeValidationRequest validationRequest = new CodeValidationRequest();
        validationRequest.setCode(confirmationKey);
        validationRequest.setProperties(properties);
        CodeIntrospectResponse codeIntrospectResponse = liteRegisterApi.introspectCode(validationRequest);

        if ((codeIntrospectResponse != null) && (codeIntrospectResponse.getUser() != null)
                && (codeIntrospectResponse.getUser().getUsername() != null)) {
            User user = codeIntrospectResponse.getUser();
            String username = user.getUsername();
            callback = callback + "?confirmation=" + confirmationKey + "&username=" + user.getUsername();
        } else {
            request.setAttribute("error", true);
            request.setAttribute("errorMsg",
                    IdentityManagementEndpointUtil.i18n(recoveryResourceBundle, "Invalid.code"));
            request.setAttribute("errorCode", "18001");
            request.getRequestDispatcher("error.jsp").forward(request, response);
            return;
        }

        request.setAttribute("callback", callback);
        request.setAttribute("confirmLiteReg", "true");
        request.getRequestDispatcher("self-registration-complete.jsp").forward(request,response);
    } catch (ApiException e) {
        IdentityManagementEndpointUtil.addErrorInformation(request, e);
        request.getRequestDispatcher("error.jsp").forward(request, response);
        return;
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

                <div class="boarder-all ">

                    <% if (error) { %>
                    <div class="alert alert-danger" id="server-error-msg">
                        <%= IdentityManagementEndpointUtil.i18nBase64(recoveryResourceBundle, errorMsg) %>
                    </div>
                    <% }else{
                        %>
                    <div class="alert alert-info"><%=message%></div>
                    <%
                    } %>
                    <div class="alert alert-danger" id="error-msg" hidden="hidden"></div>
                </div>
                <div class="clearfix"></div>
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
            <jsp:include page="includes/footer.jsp"/>
    <% } %>

    <script src="libs/jquery_3.6.0/jquery-3.6.0.min.js"></script>
    <script src="libs/bootstrap_5.3.5/js/bootstrap.min.js"></script>
    </body>
    </html>
