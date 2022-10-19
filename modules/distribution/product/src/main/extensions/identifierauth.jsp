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
<%@ page import="static org.wso2.carbon.identity.core.util.IdentityUtil.isEmailUsernameEnabled" %>
<%@ page import="static org.wso2.carbon.identity.core.util.IdentityUtil.getServerURL" %>
<%@ page import="org.wso2.carbon.identity.core.URLBuilderException" %>
<%@ page import="org.wso2.carbon.identity.core.ServiceURLBuilder" %>
<%@ page import="org.wso2.carbon.identity.mgt.endpoint.util.client.PreferenceRetrievalClient" %>
<%@ page import="org.wso2.carbon.identity.mgt.endpoint.util.client.PreferenceRetrievalClientException" %>

<jsp:directive.include file="includes/init-loginform-action-url.jsp"/>

<%
    String emailUsernameEnable = application.getInitParameter("EnableEmailUserName");
    Boolean isEmailUsernameEnabled;
    String usernameLabel = "username";
    Boolean isMultiAttributeLoginEnabledInTenant;

    if (StringUtils.isNotBlank(emailUsernameEnable)) {
        isEmailUsernameEnabled = Boolean.valueOf(emailUsernameEnable);
    } else {
        isEmailUsernameEnabled = isEmailUsernameEnabled();
    }

    try {
        PreferenceRetrievalClient preferenceRetrievalClient = new PreferenceRetrievalClient();
        isMultiAttributeLoginEnabledInTenant = preferenceRetrievalClient.checkMultiAttributeLogin(tenantDomain);
    } catch (PreferenceRetrievalClientException e) {
        request.setAttribute("error", true);
        request.setAttribute("errorMsg", AuthenticationEndpointUtil
                .i18n(resourceBundle, "something.went.wrong.contact.admin"));
        IdentityManagementEndpointUtil.addErrorInformation(request, e);
        request.getRequestDispatcher("error.jsp").forward(request, response);
        return;
    }

    if (isEmailUsernameEnabled == true) {
        usernameLabel = "email.username";
    } else if (isMultiAttributeLoginEnabledInTenant) {
        usernameLabel = "user.identifier";
    }
%>


<script>
    function submitIdentifier (e) {
        e.preventDefault();
        var userName = document.getElementById("username");
        userName.value = userName.value.trim();

        if (username.value) {
            $.ajax({
                type: "GET",
                url: "<%=loginContextRequestUrl%>",
                xhrFields: { withCredentials: true },
                success: function (data) {
                    if (data && data.status === "redirect" && data.redirectUrl && data.redirectUrl.length > 0) {
                        window.location.href = data.redirectUrl;
                    } else {
                        document.getElementById("identifierForm").submit();
                    }
                },
                cache: false
            });
        }
    }
</script>

<form class="ui large form" action="<%=loginFormActionURL%>" method="post" id="identifierForm" onsubmit="event.preventDefault()">
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
                id="username"
                value=""
                name="username"
                maxlength="50"
                tabindex="0"
                placeholder="<%=AuthenticationEndpointUtil.i18n(resourceBundle, usernameLabel)%>"
                required />
            <i aria-hidden="true" class="user icon"></i>
        </div>
        <input id="authType" name="authType" type="hidden" value="idf">
    </div>
    <%
        if (reCaptchaEnabled) {
    %>
    <div class="field">
        <div class="g-recaptcha"
             data-sitekey="<%=Encode.forHtmlContent(reCaptchaKey)%>">
        </div>
    </div>
    <%
        }
    %>

    <input type="hidden" name="sessionDataKey" value='<%=Encode.forHtmlAttribute
        (request.getParameter("sessionDataKey"))%>'/>

    <%
        Boolean isUsernameRecoveryEnabledInTenant = false;
        Boolean isSelfSignUpEnabledInTenant = false;

        String identityMgtEndpointContext = "";
        String accountRegistrationEndpointURL = "";
        String urlEncodedURL = "";
        String urlParameters = "";

        try {
            PreferenceRetrievalClient preferenceRetrievalClient = new PreferenceRetrievalClient();
            isSelfSignUpEnabledInTenant = preferenceRetrievalClient.checkSelfRegistration(tenantDomain);
            isUsernameRecoveryEnabledInTenant = preferenceRetrievalClient.checkUsernameRecovery(tenantDomain);
        } catch (PreferenceRetrievalClientException e) {
            request.setAttribute("error", true);
            request.setAttribute("errorMsg", AuthenticationEndpointUtil.i18n(resourceBundle, "something.went.wrong.contact.admin"));
            IdentityManagementEndpointUtil.addErrorInformation(request, e);
            request.getRequestDispatcher("error.jsp").forward(request, response);
            return;
        }

        if (isUsernameRecoveryEnabledInTenant || isSelfSignUpEnabledInTenant) {
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

            accountRegistrationEndpointURL = application.getInitParameter("AccountRegisterEndpointURL");
            if (StringUtils.isBlank(accountRegistrationEndpointURL)) {
                accountRegistrationEndpointURL = identityMgtEndpointContext + ACCOUNT_RECOVERY_ENDPOINT_REGISTER;
            }
        }
    %>

    <% if (isUsernameRecoveryEnabledInTenant) { %>
        <div class="field">
            <a id="usernameRecoverLink" href="<%=getRecoverAccountUrl(identityMgtEndpointContext, urlEncodedURL, true, urlParameters)%>">
                <%=AuthenticationEndpointUtil.i18n(resourceBundle, "forgot.username.password")%>
                <%=AuthenticationEndpointUtil.i18n(resourceBundle, "forgot.username")%> ?
            </a>
        </div>
    <% } %>

    <div class="ui two column stackable grid">
        <div class="column align-left buttons">
            <% if (isSelfSignUpEnabledInTenant) { %>
            <input
                type="button"
                onclick="window.location.href='<%=getRegistrationUrl(accountRegistrationEndpointURL, urlEncodedURL, urlParameters)%>';"
                class="ui large button secondary"
                id="registerLink"
                role="button"
                value="<%=AuthenticationEndpointUtil.i18n(resourceBundle, "create.account")%>" />
            <% } %>
        </div>
        <div class="column align-right buttons">
            <input
                type="submit"
                onclick="submitIdentifier(event)"
                class="ui primary large button"
                role="button"
                data-testid="identifier-auth-continue-button"
                value="<%=AuthenticationEndpointUtil.i18n(resourceBundle, "continue")%>" />
        </div>
    </div>
</form>
