<%
	session.removeAttribute("access.token");
	response.sendRedirect("login.jsp");
%>
