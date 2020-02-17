
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

<%@ page import="java.io.FileReader" %>
<%@ page import="org.json.simple.parser.JSONParser"%>
<%@ page import="org.json.simple.JSONObject"%>
<%@ page import="java.net.URI"%>
<%@ page import="org.apache.commons.lang.StringUtils"%>

<%
    String tenant = request.getParameter("tenantDomain");
    if (tenant == null) {
        String cb = request.getParameter("callback");
        if (cb != null) {
            URI uri = new URI(cb);
            String decodedValue = uri.getQuery();
            String[] params = decodedValue.split("&");
            for (String param : params) {
                if (param.startsWith("tenantDomain=")) {
                    String[] keyVal = param.split("=");
                    tenant = keyVal[1];
                }
            }
        }
    }

    String headerTitle = "API Manager";
    String pageTitle = "WSO2 API Manager";
    String footerText = "WSO2 API Manager";
    String faviconSrc = "libs/theme/assets/images/favicon.ico";
    String logoSrc = null;
    String logoHeight = "50";
    String logoWidth = "50";
    String logoAltText = "";
    File customCSSFile = null;
    String customCSS = "";
    String tenantThemeDirectoryName = "";
    boolean showCookiePolicy = true;
    boolean showPrivacyPolicy = true;
    String cookiePolicyText = null;
    String privacyPolicyText = null;

    if (tenant != null) {
        String current = new File(".").getCanonicalPath();
        String tenantConfLocation = "/repository/deployment/server/jaggeryapps/devportal/site/public/tenant_themes/";
        tenantThemeDirectoryName = tenant;
        String tenantThemeFile =  current + tenantConfLocation + tenantThemeDirectoryName + "/login/" + "loginTheme.json";
        customCSS = current + tenantConfLocation + tenantThemeDirectoryName + "/login/css/" + "loginTheme.css";
        File directory = new File(current + tenantConfLocation + tenantThemeDirectoryName);
        if (directory != null && directory.exists() && directory.isDirectory()) {
            File themeFile = new File(tenantThemeFile);
            customCSSFile = new File(customCSS);
            if (themeFile != null && themeFile.exists() && themeFile.isFile()) {
                FileReader fr = new FileReader(themeFile);
                JSONParser parser = new JSONParser();
                Object obj = parser.parse(fr);
                JSONObject jsonObject = (JSONObject) obj;

                pageTitle = (String)jsonObject.get("title") != null ? (String)jsonObject.get("title") : "WSO2 API Manager";

                JSONObject headerThemeObj = (JSONObject)jsonObject.get("header");
                if (headerThemeObj != null) {
                    headerTitle = (String)(headerThemeObj.get("title")) != null ? (String)(headerThemeObj.get("title")) : "API Manager";
                }

                JSONObject footerThemeObj = (JSONObject)jsonObject.get("footer");
                if (footerThemeObj != null) {
                    footerText = (String)(footerThemeObj.get("name"));
                }

                JSONObject faviconThemeObj = (JSONObject)jsonObject.get("favicon");
                if (faviconThemeObj != null) {
                    String fileName = (String)(faviconThemeObj.get("src"));
                    if (!StringUtils.isEmpty(fileName)) {
                        faviconSrc = "/devportal/site/public/tenant_themes/" + tenantThemeDirectoryName + "/login/images/"
                                  + fileName;
                    }
                }

                JSONObject logoThemeObj = (JSONObject)jsonObject.get("logo");
                if (logoThemeObj != null) {
                    String fileName = (String)(logoThemeObj.get("src"));
                    if (!StringUtils.isEmpty(fileName)) {
                        logoSrc = "/devportal/site/public/tenant_themes/" + tenantThemeDirectoryName + "/login/images/"
                                  + fileName;
                    }
                    logoHeight = (String)(logoThemeObj.get("height")) != null ? (String)(logoThemeObj.get("height")) : logoHeight;
                    logoWidth = (String)(logoThemeObj.get("width")) != null ? (String)(logoThemeObj.get("width")) : logoWidth;
                    logoAltText = (String)(logoThemeObj.get("alt"));
                }

                JSONObject cookiePolicyThemeObj = (JSONObject)jsonObject.get("cookie-policy");
                if (cookiePolicyThemeObj != null) {
                    showCookiePolicy = (Boolean)(cookiePolicyThemeObj.get("visible"));
                    cookiePolicyText = (String)cookiePolicyThemeObj.get("text");
                }

                JSONObject privacyPolicyThemeObj = (JSONObject)jsonObject.get("privacy-policy");
                if (privacyPolicyThemeObj != null) {
                    showPrivacyPolicy = (Boolean)(privacyPolicyThemeObj.get("visible"));
                    privacyPolicyText = (String)privacyPolicyThemeObj.get("text");
                }
            }
        }
    }
    request.setAttribute("headerTitle", headerTitle);
    request.setAttribute("pageTitle", pageTitle);
    request.setAttribute("footerText", footerText);
    request.setAttribute("faviconSrc", faviconSrc);
    request.setAttribute("showCookiePolicy", showCookiePolicy);
    request.setAttribute("showPrivacyPolicy", showPrivacyPolicy);
    request.setAttribute("cookiePolicyText", cookiePolicyText);
    request.setAttribute("privacyPolicyText", privacyPolicyText);
    request.setAttribute("logoSrc", logoSrc);
    request.setAttribute("logoHeight", logoHeight);
    request.setAttribute("logoWidth", logoWidth);
    request.setAttribute("logoAltText", logoAltText);

    if (customCSSFile != null && customCSSFile.exists() && customCSSFile.isFile()) {
	String cssRelativePath = "/devportal/site/public/tenant_themes/" + tenantThemeDirectoryName + "/login/css/" + "loginTheme.css";
        request.setAttribute("customCSS", cssRelativePath);
    } else {
        request.setAttribute("customCSS", "");
    }

%>

<meta http-equiv="X-UA-Compatible" content="IE=edge">
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">

<link rel="icon" href=<%=request.getAttribute("faviconSrc")%> type="image/x-icon"/>
<link href="libs/theme/wso2-default.min.css" rel="stylesheet">

<title><%=request.getAttribute("pageTitle")%></title>

<style>
    html, body {
        height: 100%;
    }
    body {
        flex-direction: column;
        display: flex;
    }
    main {
        flex-shrink: 0;
    }
    main.center-segment {
        margin: auto;
        display: flex;
        align-items: center;
    }
    main.center-segment > .ui.container.medium {
        max-width: 450px !important;
    }
    main.center-segment > .ui.container.large {
        max-width: 700px !important;
    }
    main.center-segment > .ui.container > .ui.segment {
        padding: 3rem;
    }
    main.center-segment > .ui.container > .ui.segment .segment-form .buttons {
        margin-top: 1em;
    }
    main.center-segment > .ui.container > .ui.segment .segment-form .buttons.align-right button,
    main.center-segment > .ui.container > .ui.segment .segment-form .buttons.align-right input {
        margin: 0 0 0 0.25em;
    }
    main.center-segment > .ui.container > .ui.segment .segment-form .column .buttons.align-left button.link-button,
    main.center-segment > .ui.container > .ui.segment .segment-form .column .buttons.align-left input.link-button {
        padding: .78571429em 1.5em .78571429em 0;
    }
    main.center-segment > .ui.container > .ui.segment .segment-form {
        text-align: left;
    }
    main.center-segment > .ui.container > .ui.segment .segment-form .align-center {
        text-align: center;
    }
    main.center-segment > .ui.container > .ui.segment .segment-form .align-right {
        text-align: right;
    }
    footer {
        padding: 2rem 0;
    }
    body .product-title .product-title-text {
        margin: 0;
    }
    body .center-segment .product-title .product-title-text {
        margin-top: 2em;
        margin-bottom: 1em;
    }
    .ui.menu.fixed.app-header .product-logo {
        padding-left: 0;
    }
    /* Table of content styling */
    main #toc {
        position: sticky;
        top: 93px;
    }
    main .ui.segment.toc {
        padding: 20px;
    }
    main .ui.segment.toc ul.ui.list.nav > li.sub {
        margin-left: 20px;
    }
    main .ui.segment.toc ul.ui.list.nav > li > a {
        color: rgba(0,0,0,.87);
        text-decoration: none;
    }
    main .ui.segment.toc ul.ui.list.nav > li:before {
        content: "\2219";
        font-weight: bold;
        font-size: 1.6em;
        line-height: 0.5em;
        display: inline-block;
        width: 1em;
        margin-left: -0.7em;
    }
    main .ui.segment.toc ul.ui.list.nav > li.sub:before {
        content: "\2192";
        margin-left: -1em;
    }
    main .ui.segment.toc ul.ui.list.nav > li:hover a {
        color: #ff5000;
        text-decoration: none;
    }
    main .ui.segment.toc ul.ui.list.nav > li:hover:before {
        color: #ff5000;
    }
</style>

<%
	String cssPath = request.getAttribute("customCSS") + "";
	if (!StringUtils.isEmpty(cssPath)) {
%>
		<link href=<%=cssPath%> rel="stylesheet" type="text/css">
<%	}
%>

<script src="libs/jquery_3.4.1/jquery-3.4.1.js"></script>
