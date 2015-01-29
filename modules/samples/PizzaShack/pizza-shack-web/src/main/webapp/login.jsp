<!DOCTYPE html>
<%@page import="com.pizzashack.client.dto.Token"%>
<%@page import="com.pizzashack.client.web.TokenManager"%>
<%
	boolean loginFailed = false;
	String submitted = request.getParameter("submitted");
	if ("true".equals(submitted)) {
		TokenManager manager = new TokenManager();
		String username = request.getParameter("username");
		String password = request.getParameter("password");
		Token token = manager.getToken(username, password, "order_pizza");
		if (token != null) {
			session.setAttribute("access.token", token.getAccessToken());
			session.setAttribute("scope", token.getScopes());
			response.sendRedirect("index.jsp");
			try{
			session.removeAttribute("cancel.order");

			}catch(Exception ex){
			}
		} else {
			loginFailed = true;
		}
	}
%>
<html lang="en">
<head>
    <jsp:include page="include_head.jsp"/>
</head>

<script>
  function preventBack(){window.history.forward();}
  setTimeout("preventBack()", 0);
  window.onunload=function(){null};
</script>

<body>

<div class="container">
    <div class="row">
        <div class="span12 pizza-logo">
            <a href="index.jsp"><img src="images/shack-logo.png"></a>
        </div>
    </div>

    <div class="row">
        <div class="navbar">
            <div class="navbar-inner">
                <div class="container">

                    <div class="nav-collapse">
                        <ul class="nav">
                        </ul>
                    </div>
                    <!-- /.nav-collapse -->
                </div>
            </div>
            <!-- /navbar-inner -->
        </div>
    </div>

    <div class="row well">
        <div class="span12">
            <h1>Login to Pizza Shack</h1>
        </div>
        <div class="span10">
        <%
        	if (loginFailed) {
        %>
        	<div class="alert alert-error">Authentication failed for user</div>
        <%
        	}
        %>

        <form class="form-horizontal" action="login.jsp" method="POST">
        <fieldset>
        <input name="submitted" value="true" type="hidden"/>
        <div class="control-group">
            <label class="control-label" for="quantity">Username</label>

            <div class="controls">
            <input type="text" class="input-xlarge" id="username" name="username" />

        </div>
        </div>

        <div class="control-group">
            <label class="control-label" for="userName">Password</label>

            <div class="controls">
                <input type="password" class="input-xlarge" id="password" name="password"/>

            </div>
        </div>

        <div class="form-actions">
            <button type="submit" class="btn btn-primary">Login</button>
        </div>
        </fieldset>
        </form>
        </div>
    </div>

</div>
<!-- /container -->

</body>
</html>
