/*
 * Copyright 2011-2012 WSO2, Inc. (http://wso2.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wso2.am.integration.services.jaxrs.coffeesample;

import org.wso2.am.integration.services.jaxrs.coffeesample.bean.Order;
import org.wso2.am.integration.services.jaxrs.coffeesample.bean.Payment;
import org.wso2.am.integration.services.jaxrs.coffeesample.bean.PaymentStatus;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

/**
 *
 */

public class StarbucksOutletServiceImpl implements StarbucksOutletService {
	private Map<String, Order> ordersList = new ConcurrentHashMap<String, Order>();

	private Map<String, Payment> paymentRegister = new ConcurrentHashMap<String, Payment>();

	private final Map<String, Double> priceList = new ConcurrentHashMap<String, Double>();

	private static final Random rand = new Random();

	//    long currentId = 123;
	//    Map<Long, Customer> customers = new HashMap<Long, Customer>();
	//    Map<Long, Order> orders = new HashMap<Long, Order>();

	public StarbucksOutletServiceImpl() {
		init();
	}

	public Response addOrder(Order orderBean) {
		String drinkName = orderBean.getDrinkName();
		String additions = orderBean.getAdditions();
		orderBean.setCost(calculateCost(drinkName, additions));
		ordersList.put(orderBean.getOrderId(), orderBean);
		return Response.ok().entity(orderBean).type(MediaType.APPLICATION_JSON).build();
	}

	public Order getOrder(String id) {
		return ordersList.get(id);
	}

	public Response updateOrder(Order orderBean) {
		String orderId = orderBean.getOrderId();
		String drinkName = orderBean.getDrinkName();
		String additions = orderBean.getAdditions();

		Order order = ordersList.get(orderId);
		if (order != null) {
			if (order.isLocked()) {
				return Response.notModified().type(MediaType.APPLICATION_JSON_TYPE).build();
			} else {
				if (drinkName != null && !"".equals(drinkName)) {
					order.setDrinkName(drinkName);
				} else {
					drinkName = order.getDrinkName();     //used to calculate the cost
				}
				order.setAdditions(additions);
				order.setCost(calculateCost(drinkName, additions));
				return Response.ok(order).type(MediaType.APPLICATION_JSON_TYPE).build();
			}
		}
		return null;
	}

	public Response getPendingOrders() {  //todo write the client
		List<Order> orders = new ArrayList<Order>();
		for (Order order : ordersList.values()) {
			if (!order.isLocked()) {
				orders.add(order);
			}
		}
		return Response.ok(orders).type("application/atom+xml;type=feed").build();
	}

	public Response lockOrder(String id) {  //@PathParam("orderId")
		Order order = ordersList.get(id);
		if (order != null) {
			order.setLocked(true);
			return Response.ok(order).type(MediaType.APPLICATION_XML).build();
		}
		return Response.notModified().entity(id).type(MediaType.APPLICATION_XML).build();
	}

	public Response removeOrder(String id) {    // @PathParam("orderId")
		Boolean removed = ordersList.remove(id) != null;
		paymentRegister.remove(id);
		String status = removed.toString();
		return removed ? Response.ok(status).build() : Response.notModified().build();
	}

	public Response doPayment(String id, Payment payment) {    // @PathParam("orderId")
		String name = payment.getName();
		Double amount = payment.getAmount();
		String cardNumber = payment.getCardNumber();
		String expiryDate = payment.getExpiryDate();

		PaymentStatus paymentStatus;
		Payment registeredPayment = paymentRegister.get(id);
		if (registeredPayment != null) {
			paymentStatus = new PaymentStatus("Duplicate Payment", registeredPayment);
			return Response.notModified().entity(paymentStatus).type(MediaType.APPLICATION_JSON)
			               .build();
		}

		Order order = ordersList.get(id);
		if (order == null) {
			paymentStatus = new PaymentStatus("Invalid Order ID", null);
			return Response.notModified().entity(paymentStatus).type(MediaType.APPLICATION_JSON)
			               .build();
		}

		if (!order.isAmountAcceptable(amount)) {
			paymentStatus = new PaymentStatus("Insufficient Funds", null);
			return Response.notModified().entity(paymentStatus).type(MediaType.APPLICATION_JSON)
			               .build();
		}

		registeredPayment = new Payment(id);
		registeredPayment.setAmount(amount);
		registeredPayment.setCardNumber(cardNumber);
		registeredPayment.setExpiryDate(expiryDate);
		registeredPayment.setName(name);
		paymentRegister.put(id, registeredPayment);
		paymentStatus = new PaymentStatus("Payment Accepted", registeredPayment);
		return Response.ok().entity(paymentStatus).type(MediaType.APPLICATION_JSON).build();

	}

	public Payment getPayment(String id) {    // @PathParam("orderId")
		return paymentRegister.get(id);
	}

	private double calculateCost(String drinkName, String additions) {
		double cost = getPrice(drinkName, false);
		if (additions != null && !"".equals(additions)) {
			String[] additionalItems = additions.split(" ");
			for (String item : additionalItems) {
				cost += getPrice(item, true);
			}
		}
		return Double.parseDouble(Order.currencyFormat.format(cost));
	}

	private double getPrice(String item, boolean addition) {
		synchronized (priceList) {
			Double price = priceList.get(item);
			if (price == null) {
				if (addition) {
					price = rand.nextDouble() * 5;
				} else {
					price = rand.nextInt(8) + 2 - 0.01;
				}
				priceList.put(item, price);
			}
			return price;
		}
	}

	private void init() {
		String drinkName = "Vanilla Flavored Coffee";
		String additions = "Milk";
		Order order = new Order();
		order.setOrderId("123");
		order.setDrinkName(drinkName);
		order.setAdditions(additions);
		order.setCost(calculateCost(drinkName, additions));

		ordersList.put(order.getOrderId(), order);

		//following order is used by the Client class to show the HTTP DELETE
		drinkName = "Chocolate Flavored Coffee";
		order = new Order();
		order.setOrderId("444");
		order.setDrinkName(drinkName);
		order.setCost(calculateCost(drinkName, null));    //no additions

		ordersList.put(order.getOrderId(), order);
	}
}
