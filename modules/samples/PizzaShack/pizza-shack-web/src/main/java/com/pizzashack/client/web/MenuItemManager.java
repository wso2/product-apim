package com.pizzashack.client.web;


import com.pizzashack.client.dto.Pizza;
import org.apache.http.HttpResponse;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

public class MenuItemManager {
	
    private HTTPClient httpClient;
    private String serverURL;
    private final String PIZZA_LIST_URL = "/1.0.0/menu";

    public MenuItemManager() {
        httpClient = new HTTPClient();
        serverURL = PizzaShackWebConfiguration.getInstance().getServerURL();
    }

    public ArrayList<Pizza> getAvailablePizzaList(String token) {
        InputStream is = null;
        HttpResponse httpResponse = null;
        try {
            httpResponse = httpClient.doGet(serverURL + PIZZA_LIST_URL, "Bearer " + token);
            String response = httpClient.getResponsePayload(httpResponse);
            return JSONClient.getAvailablePizzaList(response);

        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
