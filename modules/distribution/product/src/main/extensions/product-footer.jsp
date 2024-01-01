
<%--
  ~ Copyright (c) 2019, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
  ~
  ~ WSO2 Inc. licenses this file to you under the Apache License,
  ~ Version 2.0 (the "License"); you may not use this file except
  ~ in compliance with the License.
  ~ You may obtain a copy of the License at
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

<!-- localize.jsp MUST already be included in the calling script -->

<%-- Cookie Consent Banner --%>
<%
    if (config.getServletContext().getResource("extensions/cookie-consent-banner.jsp") != null) {
%>
        <jsp:include page="/extensions/cookie-consent-banner.jsp"/>
<%
    } else {
%>
        <jsp:include page="/includes/cookie-consent-banner.jsp"/>
<%
    }
%>

<%-- footer --%>
<footer class="footer">
    <div class="ui container fluid">
        <div class="ui text stackable menu">
            <div class="left menu">
                <a class="item no-hover" id="copyright">
                    <%
                        String copyright = i18n(recoveryResourceBundle, customText, "copyright", __DEPRECATED__copyrightText);
                        if (StringUtils.isNotBlank(copyright)) {
                    %>
                        <span class="copyright-text"><%= copyright %></span>
                    <% } %>
                    <%
                        if (StringUtils.isNotBlank(copyright) && !shouldRemoveDefaultBranding) {
                    %>
                        <div class="powered-by-logo-divider">|</div>
                    <% } %>
                    <%
                        if (!shouldRemoveDefaultBranding) {
                    %>
                    <div class="powered-by-logo-divider">|</div>Powered by <div class="powered-by-logo" onclick="window.open('<%= StringEscapeUtils.escapeHtml4(productURL) %>', '_self', 'noopener,noreferrer,resizable')">
                        <img width="80" height="20" src="<%= StringEscapeUtils.escapeHtml4(logoURL) %>" alt="<%= StringEscapeUtils.escapeHtml4(logoAlt) %>" />
                    </div>
                    <% } %>
                </a>
            </div>
            <div class="right menu">
            <%
                if (!StringUtils.isBlank(privacyPolicyURL)) {
            %>
                <a
                    id="privacy-policy"
                    class="item"
                    href="<%= i18nLink(userLocale, privacyPolicyURL) %>"
                    target="_blank"
                    rel="noopener noreferrer"
                    data-testid="login-page-privacy-policy-link"
                >
                    <%=IdentityManagementEndpointUtil.i18n(recoveryResourceBundle, "Privacy.policy")%>
                </a>
            <% } %>
            <%
                if (!StringUtils.isBlank(termsOfUseURL)) {
            %>
                <a
                    id="terms-of-service"
                    class="item"
                    href="<%= i18nLink(userLocale, termsOfUseURL) %>"
                    target="_blank"
                    rel="noopener noreferrer"
                    data-testid="login-page-privacy-policy-link"
                >
                    <%=IdentityManagementEndpointUtil.i18n(recoveryResourceBundle, "toc")%>
                </a>
            <% } %>

                <%
                    List<String> langSwitcherEnabledServlets = Arrays.asList("/password-recovery.jsp", "/register.do", "/passwordreset.do", "/error.jsp");
                    if (langSwitcherEnabledServlets.contains(request.getServletPath())) {
                %>
                        <jsp:include page="language-switcher.jsp"/>
                <% } %>
            </div>
        </div>
    </div>
</footer>
