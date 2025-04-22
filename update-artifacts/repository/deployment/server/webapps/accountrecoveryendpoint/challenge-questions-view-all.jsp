<%--
  ~ Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

<%@ page import="org.owasp.encoder.Encode" %>
<%@ page import="org.wso2.carbon.identity.captcha.util.CaptchaUtil" %>
<%@ page import="org.wso2.carbon.identity.mgt.endpoint.util.IdentityManagementEndpointUtil" %>
<%@ page import="org.wso2.carbon.identity.mgt.endpoint.util.client.model.InitiateAllQuestionResponse" %>
<%@ page import="org.wso2.carbon.identity.mgt.endpoint.util.client.model.Question" %>
<%@ page import="org.wso2.carbon.identity.mgt.endpoint.util.client.model.RetryError" %>
<%@ page import="java.io.File" %>
<%@ page import="java.util.List" %>
<jsp:include page="includes/localize.jsp"/>

<%
    InitiateAllQuestionResponse initiateAllQuestionResponse = (InitiateAllQuestionResponse)
            session.getAttribute("initiateAllQuestionResponse");
    List<Question> challengeQuestions = initiateAllQuestionResponse.getQuestions();

    RetryError errorResponse = (RetryError) request.getAttribute("errorResponse");
    boolean reCaptchaEnabled = false;
    if (request.getAttribute("reCaptcha") != null && "TRUE".equalsIgnoreCase((String) request.getAttribute("reCaptcha"))) {
        reCaptchaEnabled = true;
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
    <%
        if (reCaptchaEnabled) {
            String reCaptchaAPI = CaptchaUtil.reCaptchaAPIURL();
    %>
    <script src='<%=(reCaptchaAPI)%>'></script>
    <%
        }
    %>
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
            <%
                if (errorResponse != null) {
            %>
            <div class="alert alert-danger" id="server-error-msg">
                <%=IdentityManagementEndpointUtil.i18nBase64(recoveryResourceBundle, errorResponse.getDescription())%>
            </div>
            <%
                }
            %>
            <div class="clearfix"></div>
            <div class="boarder-all ">

                <div class="padding-double">
                    <form method="post" action="processsecurityquestions.do" id="securityQuestionForm">
                        <%
                            int count = 0;
                            if (challengeQuestions != null) {
                                for (Question challengeQuestion : challengeQuestions) {
                        %>
                        <div class="col-xs-12 col-sm-12 col-md-12 col-lg-12 form-group">
                            <label class="control-label"><%=Encode.forHtml(challengeQuestion.getQuestion())%>
                            </label>
                        </div>
                        <div class="col-xs-12 col-sm-12 col-md-12 col-lg-12 form-group">
                            <input name="<%=Encode.forHtmlAttribute(challengeQuestion.getQuestionSetId())%>"
                                   type="text"
                                   class="form-control"
                                   tabindex="0" autocomplete="off" required/>
                        </div>
                        <%
                                }
                            }
                        %>
                        <%
                            if (reCaptchaEnabled) {
                                String reCaptchaKey = CaptchaUtil.reCaptchaSiteKey();
                        %>
                        <div class="field">
                            <div class="g-recaptcha"
                                    data-size="invisible"
                                    data-callback="onCompleted"
                                    data-action="securityQuestion"
                                    data-sitekey=
                                            "<%=Encode.forHtmlContent(reCaptchaKey)%>">
                            </div>
                        </div>
                        <%
                            }
                        %>
                        <div class="form-actions">
                            <button id="answerSubmit"
                                    class="wr-btn grey-bg col-xs-12 col-md-12 col-lg-12 uppercase font-extra-large"
                                    type="submit"><%=IdentityManagementEndpointUtil.i18n(recoveryResourceBundle, "Submit")%>
                            </button>
                        </div>
                        <div class="clearfix"></div>
                    </form>
                </div>
            </div>
        </div>
        <!-- /content/body -->

    </div>
</div>
<!-- /content/body -->

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

<script type="text/javascript">
    function onCompleted() {
        $('#securityQuestionForm').submit();
    }
    $(document).ready(function () {
        $("#securityQuestionForm").submit(function (e) {
           <%
               if (reCaptchaEnabled) {
           %>
           if (!grecaptcha.getResponse()) {
               e.preventDefault();
               grecaptcha.execute();

               return true;
           }
           <%
               }
           %>
           return true;
        });
    });

</script>
</body>
</html>
