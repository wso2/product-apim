<!DOCTYPE html>
<%@page import="com.pizzashack.client.dto.Order"%>
<%@page import="com.pizzashack.client.web.OrderManager"%>
<%
	String token = (String) session.getAttribute("access.token");
	if (token == null) {
		response.sendRedirect("login.jsp");
		return;
	}
	
	String submitted = request.getParameter("submitted");
	if ("true".equals(submitted)) {
		OrderManager manager = new OrderManager();
		String address = request.getParameter("bAddress");
		String pizzaType = request.getParameter("pizzaType");
		String quantity = request.getParameter("quantity");
		String customer = request.getParameter("userName");
		String card = request.getParameter("ccNumber");
		Order order = manager.saveOrder(address, pizzaType, quantity, customer, card, token);
		response.sendRedirect("index.jsp?orderId=" + order.getOrderId());
	}
        session.setAttribute("cancel.order", "true");

%>
<html lang="en">
<head>
    <jsp:include page="include_head.jsp"/>
</head>
<script>

</script>
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
        <div class="span12">
            <h1>Place your order</h1>
        </div>
        <div class="span10">
            <form class="form-horizontal" action="place_order.jsp" method="POST">
                <fieldset>
                	<input type="hidden" name="submitted" value="true"/>
                	<div class="control-group">
                        <label class="control-label" for="quantity">Pizza Type</label>

                        <div class="controls">
                            <input type="text" class="input-xlarge" id="quantity" 
                            	name="pizzaType" value="<%=request.getParameter("type")%>" readonly="readonly"/>                            
                        </div>
                    </div>
                
                    <div class="control-group">
                        <label class="control-label" for="quantity">Quantity</label>

                        <div class="controls">
                            <input type="text" class="input-xlarge" id="quantity" name="quantity"/>

                            <p class="help-block">Enter the number of pizzas.</p>
                        </div>
                    </div>

                    <div class="control-group">
                        <label class="control-label" for="userName">Your Name</label>

                        <div class="controls">
                            <input type="text" class="input-xlarge" id="userName" name="userName"/>

                        </div>
                    </div>

                    <div class="control-group">
                        <label class="control-label" for="ccNumber">Credit Card Number</label>

                        <div class="controls">
                            <input type="text" class="input-xlarge" id="ccNumber" name="ccNumber"/>

                        </div>
                    </div>

                    <div class="control-group">
                        <label class="control-label" for="bAddress">Billing Address</label>

                        <div class="controls">
                            <textarea class="input-xlarge" id="bAddress" name="bAddress"></textarea>
                        </div>
                    </div>


                    <div class="form-actions">
                        <button type="submit" class="btn btn-primary">Checkout</button>
                        <button type="button" class="btn cancelBtn" onclick="javascript:location.href='index.jsp'">Cancel</button>
                    </div>
                </fieldset>
            </form>
        </div>
    </div>

</div>
<!-- /container -->


</body>
</html>
