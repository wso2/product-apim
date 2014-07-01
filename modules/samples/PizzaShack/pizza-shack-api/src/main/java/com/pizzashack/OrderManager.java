package com.pizzashack;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.pizzashack.beans.Order;

public class OrderManager {
	
	private Map<String, Order> orders = new ConcurrentHashMap<String, Order>();

	private static final OrderManager instance = new OrderManager();
	
	private OrderManager() {
		
	}
	
	public static OrderManager getInstance() {
		return instance;
	}
	
	public String placeOrder(Order order) {
		String orderId = UUID.randomUUID().toString();
		order.setOrderId(orderId);
		orders.put(orderId, order);
		return orderId;
	}
	
	public Order getOrder(String orderId) {
		return orders.get(orderId);
	}
	
	public boolean updateOrder(String orderId, Order order) {
		if (orders.containsKey(orderId)) {
			order.setOrderId(orderId);
			orders.put(orderId, order);
			return true;
		}
		return false;
	}
	
	public boolean cancelOrder(String orderId) {
		if (orders.containsKey(orderId)) {
			orders.remove(orderId);
			return true;
		}
		return false;
	}
	
	public Order[] listOrders() {
		return orders.values().toArray(new Order[orders.size()]);
	}
	
	public boolean deliverOrder(String orderId) {
		Order order = orders.get(orderId);
		if (order != null) {
			order.setOrderId(orderId);
			order.setDelivered(true);
			return true;
		}
		return false;
	}
}
