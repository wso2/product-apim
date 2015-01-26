package com.pizzashack;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import com.pizzashack.beans.MenuItem;

@Path("/menu")
public class PizzaShackMenuAPI {

	@GET
	@Produces("application/json")
	public Response getMenu() {
		MenuItem[] menuItems = PizzaMenu.getInstance().getMenu();
		return Response.ok().entity(menuItems).build();
	}
}
