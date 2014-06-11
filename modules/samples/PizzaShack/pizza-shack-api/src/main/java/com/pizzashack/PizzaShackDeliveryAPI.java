package com.pizzashack;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import com.pizzashack.beans.Order;

@Path("/delivery")
public class PizzaShackDeliveryAPI {
	
	@GET
	@Produces("application/json")
	public Response getOrderList() {
		Order[] orders = OrderManager.getInstance().listOrders();
		return Response.ok().entity(orders).build();
	}
	
	@PUT
	@Consumes("application/json")
	@Produces("application/json")
	@Path("/{orderId}")
	public Response deliverOrder(@PathParam("orderId") String orderId) {
		boolean updated = OrderManager.getInstance().deliverOrder(orderId);
		if (updated) {
			return Response.ok().build();
		} else {
			return Response.status(Response.Status.NOT_FOUND).build();
		}
	}

}
