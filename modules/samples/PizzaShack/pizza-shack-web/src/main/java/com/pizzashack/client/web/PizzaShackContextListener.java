package com.pizzashack.client.web;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

public class PizzaShackContextListener implements ServletContextListener {

	@Override
	public void contextDestroyed(ServletContextEvent event) {
		
	}

	@Override
	public void contextInitialized(ServletContextEvent event) {		
		String serverURL = event.getServletContext().getInitParameter("serverURL");
		System.out.println("Setting server URL to: " + serverURL);
		PizzaShackWebConfiguration.getInstance().setServerURL(serverURL);
		
		String loginURL = event.getServletContext().getInitParameter("loginURL");
        //String loginURL = "https://10.150.3.80:9443/_WSO2AMLoginAPI_";
        PizzaShackWebConfiguration.getInstance().setLoginURL(loginURL);
		
		String consumerKey = event.getServletContext().getInitParameter("consumerKey");
		PizzaShackWebConfiguration.getInstance().setConsumerKey(consumerKey);
		
		String consumerSecret = event.getServletContext().getInitParameter("consumerSecret");
		PizzaShackWebConfiguration.getInstance().setConsumerSecret(consumerSecret);
	}

}
