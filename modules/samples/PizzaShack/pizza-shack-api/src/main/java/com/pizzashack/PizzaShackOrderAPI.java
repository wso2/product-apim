package com.pizzashack;

import java.net.URI;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import com.pizzashack.beans.Order;

@Path("/order")
public class PizzaShackOrderAPI {			
	
	@POST
	@Consumes("application/json")
	@Produces("application/json")
	public Response placeOrder(Order order) {
		String orderId = OrderManager.getInstance().placeOrder(order);
		return Response.created(URI.create("order/" + orderId)).entity(order).build();
	}
	
	@GET
	@Produces("application/json")
	@Path("/{orderId}")
	public Response getOrder(@PathParam("orderId") String orderId) {
		Order order = OrderManager.getInstance().getOrder(orderId);
		if (order != null) {
			return Response.ok().entity(order).build();
		} else {
			return Response.status(Response.Status.NOT_FOUND).build();
		}
	}
	
	@DELETE
	@Produces("application/json")
	@Path("/{orderId}")
	public Response cancelOrder(@PathParam("orderId") String orderId) {
		boolean cancelled = OrderManager.getInstance().cancelOrder(orderId);
		if (cancelled) {
			return Response.ok().build();
		} else {
			return Response.status(Response.Status.NOT_FOUND).build();
		}
	}
	
	@PUT
	@Consumes("application/json")
	@Produces("application/json")
	@Path("/{orderId}")
	public Response updateOrder(@PathParam("orderId") String orderId, Order order) {
		boolean updated = OrderManager.getInstance().updateOrder(orderId, order);
		if (updated) {
			return Response.ok().entity(order).build();
		} else {
			return Response.status(Response.Status.NOT_FOUND).build();
		}
	}	

}

