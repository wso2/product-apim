package com.pizzashack;

import static org.junit.Assert.assertEquals;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.core.Response;

import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.jaxrs.client.WebClient;
import org.junit.BeforeClass;
import org.junit.Test;

import com.pizzashack.beans.Order;

public class PizzaShackOrderAPIIT {
	
	private static String endpointUrl;
	
	@BeforeClass
	public static void beforeClass() {
		endpointUrl = System.getProperty("service.url");
	}
	
	@Test
	public void testGetMenu() throws Exception {
		WebClient client = WebClient.create(endpointUrl + "/api/menu");
		Response r = client.accept("application/json").get();
		assertEquals(Response.Status.OK.getStatusCode(), r.getStatus());
		String value = IOUtils.toString((InputStream)r.getEntity());
		System.out.println(value);
	}
	
	@Test
	public void testPlaceOrder() throws Exception {
		List<Object> providers = new ArrayList<Object>();
        providers.add(new org.codehaus.jackson.jaxrs.JacksonJsonProvider());
		WebClient client = WebClient.create(endpointUrl + "/api/order", providers);
		Order order = new Order();
		order.setPizzaType("Pizza1");
		order.setCustomerName("Hiranya");
		order.setCreditCardNumber("123456");
		order.setAddress("59 Flower Road");
		order.setQuantity(1);
		Response r = client.accept("application/json").type("application/json").post(order);
		assertEquals(Response.Status.CREATED.getStatusCode(), r.getStatus());
		String value = IOUtils.toString((InputStream)r.getEntity());
		System.out.println(value);
	}
		
}
