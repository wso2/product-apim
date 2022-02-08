<%--
  ~ Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
  ~
  ~  WSO2 Inc. licenses this file to you under the Apache License,
  ~  Version 2.0 (the "License"); you may not use this file except
  ~  in compliance with the License.
  ~  You may obtain a copy of the License at
  ~
  ~      http://www.apache.org/licenses/LICENSE-2.0
  ~
  ~  Unless required by applicable law or agreed to in writing,
  ~  software distributed under the License is distributed on an
  ~  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
  ~  KIND, either express or implied.  See the License for the
  ~  specific language governing permissions and limitations
  ~  under the License.
  --%>

<%@ page import="org.apache.cxf.jaxrs.client.JAXRSClientFactory" %>
<%@ page import="org.apache.cxf.jaxrs.provider.json.JSONProvider" %>
<%@ page import="org.apache.http.HttpStatus" %>
<%@ page import="org.owasp.encoder.Encode" %>
<%@ page import="org.wso2.carbon.identity.application.authentication.endpoint.util.client.SelfUserRegistrationResource" %>
<%@ page import="org.wso2.carbon.identity.application.authentication.endpoint.util.AuthenticationEndpointUtil" %>
<%@ page import="org.wso2.carbon.identity.application.authentication.endpoint.util.bean.ResendCodeRequestDTO" %>
<%@ page import="org.wso2.carbon.identity.application.authentication.endpoint.util.bean.UserDTO" %>
<%@ page import="java.net.URLEncoder" %>
<%@ page import="javax.ws.rs.core.Response" %>
<%@ page import="static org.wso2.carbon.identity.core.util.IdentityUtil.isSelfSignUpEPAvailable" %>
<%@ page import="static org.wso2.carbon.identity.core.util.IdentityUtil.isRecoveryEPAvailable" %>
<%@ page import="static org.wso2.carbon.identity.core.util.IdentityUtil.getServerURL" %>
<%@ page import="org.wso2.carbon.identity.core.URLBuilderException" %>
<%@ page import="org.wso2.carbon.identity.core.ServiceURLBuilder" %>

<jsp:directive.include file="includes/init-loginform-action-url.jsp"/>
<script>
    function submitIdentifier () {
        var userName = document.getElementById("username");
        var usernameUserInput = document.getElementById("usernameUserInput");

        if (usernameUserInput) {
            userName.value = usernameUserInput.value.trim();
        }

        if (username.value) {
            document.getElementById("identifierForm").submit();
        }
    }
</script>

<form class="ui large form" action="<%=loginFormActionURL%>" method="post" id="identifierForm">
    <%
        if (loginFormActionURL.equals(samlssoURL) || loginFormActionURL.equals(oauth2AuthorizeURL)) {
    %>
    <input id="tocommonauth" name="tocommonauth" type="hidden" value="true">
    <%
        }
    %>
    <% if (Boolean.parseBoolean(loginFailed)) { %>
    <div class="ui visible negative message" id="error-msg">
        <%= AuthenticationEndpointUtil.i18n(resourceBundle, errorMessage) %>
    </div>
    <% } else if ((Boolean.TRUE.toString()).equals(request.getParameter("authz_failure"))) { %>
    <div class="ui visible negative message" id="error-msg">
        <%=AuthenticationEndpointUtil.i18n(resourceBundle, "unauthorized.to.login")%>
    </div>
    <% } else { %>
        <div class="ui visible negative message" style="display: none;" id="error-msg"></div>
    <% } %>

    <div class="field">
        <div class="ui fluid left icon input">
            <input
                type="text"
                id="usernameUserInput"
                value=""
                name="usernameUserInput"
                tabindex="0"
                placeholder="<%=AuthenticationEndpointUtil.i18n(resourceBundle, "username")%>"
                required />
            <i aria-hidden="true" class="user icon"></i>
        </div>
        <input id="username" name="username" type="hidden" value="">
        <input id="authType" name="authType" type="hidden" value="idf">
    </div>
    <%
        if (reCaptchaEnabled) {
    %>
    <div class="field">
        <div class="g-recaptcha"
             data-sitekey="<%=Encode.forHtmlContent(request.getParameter("reCaptchaKey"))%>">
        </div>
    </div>
    <%
        }
    %>

    <input type="hidden" name="sessionDataKey" value='<%=Encode.forHtmlAttribute
        (request.getParameter("sessionDataKey"))%>'/>

    <%
        String recoveryEPAvailable = application.getInitParameter("EnableRecoveryEndpoint");
        String enableSelfSignUpEndpoint = application.getInitParameter("EnableSelfSignUpEndpoint");
        Boolean isRecoveryEPAvailable = false;
        Boolean isSelfSignUpEPAvailable = false;
        String identityMgtEndpointContext = "";
        String urlEncodedURL = "";
        String urlParameters = "";

        if (StringUtils.isNotBlank(recoveryEPAvailable)) {
            isRecoveryEPAvailable = Boolean.valueOf(recoveryEPAvailable);
        } else {
            isRecoveryEPAvailable = isRecoveryEPAvailable();
        }

        if (StringUtils.isNotBlank(enableSelfSignUpEndpoint)) {
            isSelfSignUpEPAvailable = Boolean.valueOf(enableSelfSignUpEndpoint);
        } else {
            isSelfSignUpEPAvailable = isSelfSignUpEPAvailable();
        }

        if (isRecoveryEPAvailable || isSelfSignUpEPAvailable) {
            String scheme = request.getScheme();
            String serverName = request.getServerName();
            int serverPort = request.getServerPort();
            String uri = (String) request.getAttribute(JAVAX_SERVLET_FORWARD_REQUEST_URI);
            String prmstr = (String) request.getAttribute(JAVAX_SERVLET_FORWARD_QUERY_STRING);
            String urlWithoutEncoding = scheme + "://" +serverName + ":" + serverPort + uri + "?" + prmstr;

            urlEncodedURL = URLEncoder.encode(urlWithoutEncoding, UTF_8);
            urlParameters = prmstr;

            identityMgtEndpointContext = application.getInitParameter("IdentityManagementEndpointContextURL");
            if (StringUtils.isBlank(identityMgtEndpointContext)) {
                try {
                    identityMgtEndpointContext = ServiceURLBuilder.create().addPath(ACCOUNT_RECOVERY_ENDPOINT).build()
                            .getAbsolutePublicURL();
                } catch (URLBuilderException e) {
                    request.setAttribute(STATUS, AuthenticationEndpointUtil.i18n(resourceBundle, CONFIGURATION_ERROR));
                    request.setAttribute(STATUS_MSG, AuthenticationEndpointUtil
                            .i18n(resourceBundle, ERROR_WHILE_BUILDING_THE_ACCOUNT_RECOVERY_ENDPOINT_URL));
                    request.getRequestDispatcher("error.do").forward(request, response);
                    return;
                }
            }
        }
    %>

    <% if (isSelfSignUpEPAvailable) { %>
        <div class="field">
            <a id="usernameRecoverLink" href="<%=getRecoverAccountUrl(identityMgtEndpointContext, urlEncodedURL, true, urlParameters)%>">
                <%=AuthenticationEndpointUtil.i18n(resourceBundle, "forgot.username.password")%>
                <%=AuthenticationEndpointUtil.i18n(resourceBundle, "forgot.username")%> ?
            </a>
        </div>
    <% } %>

    <div class="ui two column stackable grid">
        <div class="column align-left buttons">
            <% if (isRecoveryEPAvailable) { %>
            <input
                type="button"
                onclick="window.location.href='<%=getRegistrationUrl(identityMgtEndpointContext, urlEncodedURL, urlParameters)%>';"
                class="ui large button link-button"
                id="registerLink"
                role="button"
                value="<%=AuthenticationEndpointUtil.i18n(resourceBundle, "create.account")%>" />
            <% } %>
        </div>
        <div class="column align-right buttons">
            <input
                type="submit"
                onclick="submitIdentifier()"
                class="ui primary large button"
                role="button"
                value="<%=AuthenticationEndpointUtil.i18n(resourceBundle, "continue")%>" />
        </div>
    </div>
</form>