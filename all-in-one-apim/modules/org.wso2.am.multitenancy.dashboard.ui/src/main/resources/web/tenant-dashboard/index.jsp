<!--
 ~ Copyright (c) 2005-2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
 -->
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib uri="http://wso2.org/projects/carbon/taglibs/carbontags.jar" prefix="carbon" %>
<%@ page import="org.wso2.carbon.utils.ServerConstants" %>
<%@ page import="org.wso2.carbon.ui.CarbonUIUtil"%>
<link href="../tenant-dashboard/css/dashboard-common.css" rel="stylesheet" type="text/css" media="all"/>
<%
        Object param = session.getAttribute("authenticated");
        String passwordExpires = (String) session.getAttribute(ServerConstants.PASSWORD_EXPIRATION);
        boolean hasModMgtPermission = CarbonUIUtil.isUserAuthorized(request,
		"/permission/admin/manage/add/module");
        boolean hasServiceMgtPermission = CarbonUIUtil.isUserAuthorized(request, "/permission/admin/manage/add/service");
        boolean hasWebAppMgtPermission = CarbonUIUtil.isUserAuthorized(request,"/permission/admin/manage/manage/webapp");
        boolean loggedIn = false;
        if (param != null) {
            loggedIn = (Boolean) param;             
        } 
%>
  
<div id="passwordExpire">
         <%
         if (loggedIn && passwordExpires != null) {
         %>
              <div class="info-box"><p>Your password expires at <%=passwordExpires%>. Please change by visiting <a href="../user/change-passwd.jsp?isUserChange=true&returnPath=../admin/index.jsp">here</a></p></div>
         <%
             }
         %>
</div>
<div id="middle">
<div id="workArea">
<style type="text/css">
    

    .tip-table td.web-applications {
        background-image: url(../../carbon/tenant-dashboard/images/web-applications.png);
    }
    .tip-table td.service-testing {
        background-image: url(../../carbon/tenant-dashboard/images/service-testing.png);
    }
    .tip-table td.message-tracing {
        background-image: url(../../carbon/tenant-dashboard/images/message-tracing.png);
    }
    
</style>
 <h2 class="dashboard-title">WSO2 API Manager quick start dashboard</h2>
        <table class="tip-table">
            <tr>
                <td class="tip-top web-applications"></td>
                <td class="tip-empty "></td>
                <td class="tip-top service-testing"></td>
                <td class="tip-empty "></td>
                <td class="tip-top message-tracing"></td>
            </tr>
            <tr>
                
                <td class="tip-content">
                    <div class="tip-content-lifter">
                          <%
							if (hasWebAppMgtPermission) {
						%>
                        <a class="tip-title" href="../webapp-list/index.jsp?region=region1&item=webapps_list_menu">Web Applications</a> <br />
						<%
							} else {
						%>
				      <h3>Web Applications</h3> <br />
				        <%
							}
						%>
                        <p>Web Application hosting features in API Manager supports deployment of Jaggery Applications. Deployed Webapps can be easily managed using the Webapp management facilities available in the management console.</p>

                    </div>
                </td>
                <td class="tip-empty"></td>
                <td class="tip-content">
                    <div class="tip-content-lifter">
                         <%
							if (hasServiceMgtPermission) {
						%>
                        <a class="tip-title" href="../tryit/index.jsp?region=region5&item=tryit">Service Testing</a> <br/>
						<%
							} else {
						%>
                       <h3>Service Testing</h3> <br/>
                        <%
							}
						%>
                        <p>Tryit tool can be used as a simple Web Service client which can be used to try your services within Server itself.</p>

                    </div>
                </td>
                <td class="tip-empty"></td>
                <td class="tip-content">
                    <div class="tip-content-lifter">
                          <%
							if (hasServiceMgtPermission) {
						%>
                        <a class="tip-title"  href="../tracer/index.jsp?region=region4&item=tracer_menu">Message Tracing</a> <br/>
						<%
							} else {
						%>
						<h3>Message Tracing</h3> <br/>
						<%
							}
						%>
                        <p>Trace the request and responses to your service. Message Tracing is a vital debugging tool when you have clients from heterogeneous platforms.</p>

                    </div>
                </td>
            </tr>
            <tr>
                <td class="tip-bottom"></td>
                <td class="tip-empty"></td>
                <td class="tip-bottom"></td>
                <td class="tip-empty"></td>
                <td class="tip-bottom"></td>
                <td class="tip-empty"></td>
                <td class="tip-bottom"></td>
            </tr>
        </table>
        
        
        

<p>
    <br/>
</p> </div>
</div>
