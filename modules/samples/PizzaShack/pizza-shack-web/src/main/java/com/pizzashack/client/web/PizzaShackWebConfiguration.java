package com.pizzashack.client.web;

public class PizzaShackWebConfiguration {

	private static final PizzaShackWebConfiguration instance = new PizzaShackWebConfiguration();
	
	private String serverURL;
	private String loginURL;
	private String consumerKey;
	private String consumerSecret;
	
	private PizzaShackWebConfiguration() {
		
	}
	
	public static PizzaShackWebConfiguration getInstance() {
		return instance;
	}
	
	public String getServerURL() {
		return serverURL;
	}
	
	public void setServerURL(String serverURL) {
		this.serverURL = serverURL;
	}

	public String getLoginURL() {
		return loginURL;
	}

	public void setLoginURL(String loginURL) {
		this.loginURL = loginURL;
	}

	public String getConsumerKey() {
		return consumerKey;
	}

	public void setConsumerKey(String consumerKey) {
		this.consumerKey = consumerKey;
	}

	public String getConsumerSecret() {
		return consumerSecret;
	}

	public void setConsumerSecret(String consumerSecret) {
		this.consumerSecret = consumerSecret;
	}	
		
}
