<!DOCTYPE html>
<%@page import="com.pizzashack.client.web.PizzaShackWebConfiguration"%>
<%@page import="com.pizzashack.client.dto.Pizza"%>
<%@page import="java.util.List"%>
<%@page import="com.pizzashack.client.web.MenuItemManager"%>
<%
	String token = (String) session.getAttribute("access.token");
	if (token == null) {
		response.sendRedirect("login.jsp");
		return;
	}
%>
<html lang="en">
<head>
    <jsp:include page="include_head.jsp"/>
</head>

<body>


<div class="container">
    <jsp:include page="include_head_row.jsp" />
    
  

    <div class="row">
        <div class="navbar">
            <div class="navbar-inner">
                <div class="container">

                    <div class="nav-collapse">
                        <ul class="nav">
                            <li class="active"><a href="index.jsp">Pizza</a></li>
                            <li><a href="orders.jsp">My Orders</a></li>
                            <li><a href="logout.jsp">Log Out</a></li>
                        </ul>
                    </div>
                    <!-- /.nav-collapse -->
                </div>
            </div>
            <!-- /navbar-inner -->
        </div>
    </div>
    
    <div class="row well">
    	<%
	    	String orderId = request.getParameter("orderId");
	    	if (orderId != null) {
		    %>
		    <div class="alert alert-success"><h3>Your last order has been submitted. Please note the order ID: <div style="color:#000"><%=orderId %></div></h3></div>	
		    <%
		    	}
		    %>
        <div class="span11">
        	 
            <h1>CHOOSE YOUR PIZZA</h1>
                                  
            <ul class="thumbnails">
            <%
		    	MenuItemManager manager = new MenuItemManager();
		    	List<Pizza> menuItems = manager.getAvailablePizzaList(token);
		    	String serverURL = PizzaShackWebConfiguration.getInstance().getServerURL();
		    %>
            <%
            	for (Pizza item : menuItems) {
            %>

            
                <li class="span3">
                    <div class="thumbnail">
                        <img src="<%="http://localhost:9765/pizzashack-api-1.0.0" + item.getImageUrl()%>" alt="">

                        <div class="caption">
                            <h5><%=item.getName()%></h5>

                            <p><%=item.getDescription()%></p>

                            <p><a href="place_order.jsp?type=<%=item.getName()%>" class="btn btn-primary">Order Now</a></p>
                        </div>
                    </div>
                </li>            
            
            <%
            	}
            %>
            </ul>
        </div>
    </div>

</div>
<!-- /container -->


</body>
</html>
