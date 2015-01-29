<%
	session.removeAttribute("access.token");
	session.removeAttribute("cancel.order");
	session.invalidate();
	response.sendRedirect("login.jsp");
%>
