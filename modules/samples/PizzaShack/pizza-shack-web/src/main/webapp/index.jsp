<!DOCTYPE html>
<%@page import="com.pizzashack.client.web.PizzaShackWebConfiguration"%>
<%@page import="com.pizzashack.client.dto.Pizza"%>
<%@page import="java.util.List"%>
<%@page import="com.pizzashack.client.web.MenuItemManager"%>
<%@page import="com.pizzashack.client.dto.Token"%>
<%@page import="com.pizzashack.client.web.TokenManager"%>
<%
	String token = (String) session.getAttribute("access.token");
    boolean hasPizzaOrder = false;
	if (token == null) {
		response.sendRedirect("login.jsp");
		return;
	}
	else{

	    java.util.List<java.lang.String> scope = (java.util.List<java.lang.String>)session.getAttribute("scope");
	    for(String value : scope){
            if( value.equals("order_pizza")){
                hasPizzaOrder = true;
            }
	    }
	}
%>

<html lang="en">
<head>
    <jsp:include page="include_head.jsp"/>
    <META HTTP-EQUIV="Pragma" CONTENT="no-cache"><META HTTP-EQUIV="Expires" CONTENT="-1">
    <script src="js/jquery-1.8.2.min.js"></script>
    <script src="js/jquery-ui.js"></script>
    <script>

    //This is the function that closes the pop-up
    function endBlackout(){
        $(".blackout").css("display", "none");
        $(".msgbox").css("display", "none");
    }

    //This is the function that starts the pop-up
    function strtBlackout(){
        $(".msgbox").css("display", "block");
        $(".blackout").css("display", "block");
    }

    function hide(){

        if( "<%=hasPizzaOrder%>" =="true"){

            $("#one").css ("display", "block");
            $("#two").css ("display", "block");
            $("#three").css ("display", "block");
        }
        else{
            $("#two").css ("display", "none");
            $("#three").css ("display", "none");

            $(".orderBtn").removeAttr('href');
            $(".orderBtn").removeClass("btn-primary");
            $(".orderBtn").css("color","#bbbbbb");
            $(".myOrders").removeAttr('href');
            $(".myOrders").css("color", "#203737");

            }
    }

    $(document).ready(function(){

<%
            String cancelOrder = (String)session.getAttribute("cancel.order");
            if (cancelOrder == null) {
%>
                strtBlackout();
                hide();
                $(".closeBox").click(endBlackout); // close if a close btn is clicked
<%
              }else{
                session.removeAttribute("cancel.order");
              }
%>
    });
    </script>
</head>

<body>
    <div class="blackout"></div>
    <div class="msgbox col-sm-12">
        <br /><br /><br />
        <center>
    <div class="messagePopup">
        <div id="popupHeading" ><h2>This App will be able to</h2></div>
        <br />
        <ul class="options">
            <div id="one"><li><p> View the menu on your behalf</p></div></li>
            <div id="two"> <li><p> Make orders on your behalf</p></div></li>
            <div id="three"><li><p> View order details on your behalf</p></div></li>
        </ul>
    </div>

        <div class="closeBox">Authenticate</div>
        <a href="logout.jsp"><div class="decline">Decline</div></a>

        </center>
    </div>


    <div class="container">
        <jsp:include page="include_head_row.jsp" />

        <div class="row">
            <div class="navbar">
                <div class="navbar-inner">
                    <div class="container">

                        <div class="nav-collapse">
                            <ul class="nav">
                                <li class="active"><a href="index.jsp">Pizza</a></li>
                                <li><a href="orders.jsp" class="myOrders">My Orders</a></li>
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
                        <img src="<%=request.getContextPath()%><%=item.getImageUrl()%>" alt="">

                        <div class="caption">
                            <h5><%=item.getName()%></h5>

                            <p><%=item.getDescription()%></p>

                            <p><a href="place_order.jsp?type=<%=item.getName()%>" class="btn btn-primary orderBtn">Order Now</a></p>
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
