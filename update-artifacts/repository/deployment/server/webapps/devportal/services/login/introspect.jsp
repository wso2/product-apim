<%--
  ~ Copyright (c) 2017, WSO2 LLC (http://www.wso2.org) All Rights Reserved.
  ~
  ~ WSO2 LLC licenses this file to you under the Apache License,
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

<%@page import="org.apache.commons.logging.LogFactory"%>
<%@page import="org.apache.commons.logging.Log"%>
<%@page import="java.util.regex.Pattern"%>
<%@page import="com.google.gson.GsonBuilder"%>
<%@page import="com.google.gson.Gson"%>
<%@page import="java.util.HashMap"%>
<%@page import="org.wso2.carbon.apimgt.impl.utils.APIUtil"%>
<%@page import="java.net.URI"%>
<%@page import="java.net.http.HttpResponse"%>
<%@page import="java.net.http.HttpRequest"%>
<%@page import="java.net.http.HttpClient"%>
<%@page import="java.util.Map"%>
<%@page import="org.wso2.carbon.apimgt.ui.devportal.Util"%>
<%@include file="../constants.jsp" %>

<%@page trimDirectiveWhitespaces="true" %>

<%    Log log = LogFactory.getLog(this.getClass());
    Gson gson = new GsonBuilder().setPrettyPrinting().create();
    Map settings = Util.readJsonFile("site/public/theme/settings.json", request.getServletContext());
    String userInfoEndpoint = Util.getLoopbackOrigin((String) Util.readJsonObj(settings, "app.origin.host")) + "/oauth2/userinfo";
    String introspectEndpoint = Util.getLoopbackOrigin((String) Util.readJsonObj(settings, "app.origin.host")) + "/oauth2/introspect";
    Cookie[] cookies = request.getCookies();
    String tokenP1 = "";
    String tokenP2 = "";
    for (int i = 0; i < cookies.length; i++) {
        String cookieName = cookies[i].getName();
        if ("WSO2_AM_TOKEN_1_Default".equals(cookieName)) {
            tokenP1 = cookies[i].getValue();
        }
        if ("AM_ACC_TOKEN_DEFAULT_P2".equals(cookieName)) {
            tokenP2 = cookies[i].getValue();
        }
        if (!tokenP1.isEmpty() && !tokenP2.isEmpty()) {
            break;
        }
    }
    String token = tokenP1 + tokenP2;
    HttpClient client = HttpClient.newHttpClient();
    HttpRequest getReq = HttpRequest.newBuilder()
            .uri(URI.create(userInfoEndpoint))
            .header("Authorization", "Bearer " + token)
            .build();
    HttpResponse<String> userResult = client.send(getReq, HttpResponse.BodyHandlers.ofString());

    String body = "token=" + token;
    HttpRequest postReq = HttpRequest.newBuilder()
            .uri(URI.create(introspectEndpoint))
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .header("Authorization", "Basic " + APIUtil.getBase64EncodedAdminCredentials())
            .header("Content-Type", "application/x-www-form-urlencoded")
            .build();
    HttpResponse<String> introspectResult = client.send(postReq, HttpResponse.BodyHandlers.ofString());
    log.debug("Introspection result json: " + introspectResult.body());
    response.setContentType("application/json");

    if (introspectResult.statusCode() == 200) {
        boolean isEnableEmailUserName = Util.isEnableEmailUserName();
        Map introspect = gson.fromJson(introspectResult.body(), Map.class);
        String username = (String) introspect.get("username");
        Pattern regPattern = Pattern.compile("(@)");
        boolean found = regPattern.matcher(username).find();
        int count = !found ? 0 : (int) username.chars().filter(ch -> ch == '@').count();
        if (isEnableEmailUserName || (username.indexOf("@carbon.super") > 0 && count <= 1)) {
            introspect.put("username", username.replace("@carbon.super", ""));
        }
        response.setContentType("application/json");
        out.println(gson.toJson(introspect));
    } else {
        log.warn("Something went wrong while introspecting the token " + tokenP1 + tokenP2);
        log.error(introspectResult.body());
        response.setStatus(500);
        response.setContentType("text/plain");
        out.println("Something went wrong while introspecting the token!!");
    }
%>