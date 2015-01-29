package com.pizzashack.client.web;


import com.pizzashack.client.dto.Token;
import org.apache.http.HttpResponse;
import sun.misc.BASE64Encoder;

import java.io.IOException;

public class TokenManager {
	
    private HTTPClient httpClient;    

    public TokenManager() {
        httpClient = new HTTPClient();
    }

    public Token getToken(String username, String password, String scopes){
        String submitUrl = PizzaShackWebConfiguration.getInstance().getLoginURL();
        String consumerKey = PizzaShackWebConfiguration.getInstance().getConsumerKey();
        String consumerSecret = PizzaShackWebConfiguration.getInstance().getConsumerSecret();
        try {
            String applicationToken = consumerKey + ":" + consumerSecret;
            BASE64Encoder base64Encoder = new BASE64Encoder();
            applicationToken = "Basic " + base64Encoder.encode(applicationToken.getBytes()).trim();

            String payload = "grant_type=password&username="+username+"&password="+password+"&scope="+scopes;
            HttpResponse httpResponse = httpClient.doPost(submitUrl,applicationToken,
            		payload,"application/x-www-form-urlencoded");
            if (httpResponse.getStatusLine().getStatusCode() != 200) {
            	return null;
            }
            String response = httpClient.getResponsePayload(httpResponse);
            return JSONClient.getAccessToken(response);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public Token getTokenWithScopes(String username, String password, String scopes){
        String submitUrl = PizzaShackWebConfiguration.getInstance().getLoginURL();
        String consumerKey = PizzaShackWebConfiguration.getInstance().getConsumerKey();
        String consumerSecret = PizzaShackWebConfiguration.getInstance().getConsumerSecret();
        try {
            String applicationToken = consumerKey + ":" + consumerSecret;
            BASE64Encoder base64Encoder = new BASE64Encoder();
            applicationToken = "Basic " + base64Encoder.encode(applicationToken.getBytes()).trim();

            String payload = "grant_type=password&username="+username+"&password="+password+"&scope="+scopes;
            HttpResponse httpResponse = httpClient.doPost(submitUrl,applicationToken,
                    payload,"application/x-www-form-urlencoded");
            if (httpResponse.getStatusLine().getStatusCode() != 200) {
                return null;
            }
            String response = httpClient.getResponsePayload(httpResponse);
            return JSONClient.getAccessToken(response);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}
