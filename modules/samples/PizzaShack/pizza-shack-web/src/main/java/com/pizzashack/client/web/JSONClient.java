package com.pizzashack.client.web;


import com.pizzashack.client.dto.Order;
import com.pizzashack.client.dto.Pizza;
import com.pizzashack.client.dto.Token;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class JSONClient {
    public static void main(String args[]) {
//        //Get Pizza list Test
//        MenuItemManager menuItemManager = new MenuItemManager();
//        ArrayList<Pizza> pizzaArrayList = menuItemManager.getAvailablePizzaList("3ckecjecnrejkchrhklhckeh");
//        for (Pizza pizza : pizzaArrayList) {
//            System.out.println(pizza.getName()
//                    + " : " + pizza.getDescription() + " : " + pizza.getImageUrl() + " : " + pizza.getPrice());
//        }
//
//        //Save Order Test
//        OrderManager orderManager = new OrderManager();
//        Order order = orderManager.saveOrder("59, Malpara", "Vege Delight", "1"
//                , "Ritigala Jayasena", "3442-3453-5643-2334", "3ckecjecnrejkchrhklhckeh");
//
//        System.out.println(order.getOrderId());
//        order = orderManager.getOrder(order.getOrderId(),"3ckecjecnrejkchrhklhckeh");
//        System.out.println(order);


        //Generate Token Test
//        TokenManager tokenManager = new TokenManager();
//        Token token = tokenManager.getToken("k1NIAas_H49ZKv2jlhZROJkyHQga","fgYlec00Fg3fpP3FgH3ZyAc17EQa","chanaka","test123");
//        System.out.println(token);

//        //Test Pizza List
//        JSONClient jsonClient = new JSONClient();
//        //url = "http://10.150.3.80:9763/order-api-1.0.0/api/order/menu";
//
//        ArrayList<Pizza> pizzaArrayList = jsonClient.getAvailablePizzaList("http://localhost:8080/examples/test-json.txt");
//        for (Pizza pizza : pizzaArrayList) {
//            System.out.println(pizza.getName()
//                    + " : " + pizza.getDescription() + " : " + pizza.getImageUrl() + " : " + pizza.getPrice());
//        }
//
//        //Test Order JSON
//        Order order = new Order();
//        order.setAddress("Malpara");
//        order.setCreditCardNumber("3442-3453-5643-2334");
//        order.setCustomerName("Ritigala Jayasena");
//        order.setDelivered(false);
//        order.setPizzaType("Vegidelight");
//        order.setQuantity(2);
//        String orderJSON = jsonClient.generateSaveOrderMessage(order);
//        System.out.println(orderJSON);
//
//        //Test Order Save
//        HTTPClient HTTPClient = new HTTPClient();
////        HTTPClient.doPost(order, "http://10.150.3.80:9763/pizzashack-1.0.0/api/order");
    }


    /**
     * @param pizzaContents
     * @return
     */
    public static ArrayList<Pizza> getAvailablePizzaList(String pizzaContents) {
        JSONParser parser = new JSONParser();
        ArrayList<Pizza> pizzaList = new ArrayList<Pizza>();
        try {
            Object obj = parser.parse(pizzaContents);

            JSONArray array = (JSONArray) obj;
            Iterator<JSONObject> iterator = array.iterator();
            Pizza pizza;
            while (iterator.hasNext()) {
                pizza = new Pizza();
                JSONObject pizzaItem = iterator.next();
                pizza.setName((String) pizzaItem.get("name"));
                pizza.setDescription((String) pizzaItem.get("description"));
                pizza.setImageUrl((String) pizzaItem.get("icon"));

                double price;

                try {
                    price = Double.valueOf((String) pizzaItem.get("price")).doubleValue();
                }
                catch (NumberFormatException formatEx) {
                    String strPrice = (String) pizzaItem.get("price");
                    strPrice = strPrice.replace(',','.');

                    // If multiple commas existed the above replace would result
                    // in multiple '.' characters being in the string, so take
                    // into account only the first decimal place encountered
                    String[] tokens = strPrice.split("\\.");

                    if (2 < tokens.length) {
                        strPrice = tokens[0] + "." + tokens[1];
                    }

                    price = Double.valueOf(strPrice).doubleValue();
                }

                pizza.setPrice(price);
                pizzaList.add(pizza);
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return pizzaList;
    }


    /**
     * Constructs a JSON String similar to:
     * {"address":"59 Flower Road","pizzaType":"Pizza1","quantity":1,"customerName":"Hiranya",
     * "creditCardNumber":"123456","delivered":false}
     *
     * @param order
     * @return
     */
    public static String generateSaveOrderMessage(Order order) {
        JSONObject orderJSON = new JSONObject();

        orderJSON.put("address", order.getAddress());
        orderJSON.put("pizzaType", order.getPizzaType());
        orderJSON.put("quantity", order.getQuantity());
        orderJSON.put("customerName", order.getCustomerName());
        orderJSON.put("creditCardNumber", order.getCreditCardNumber());
        return orderJSON.toJSONString();
    }

    /**
     * Returns a populated Order object when a JSON message similar to following is passed.
     * {
     * "address": "Malpara",
     * "quantity": 2,
     * "customerName": "Ritigala Jayasena",
     * "creditCardNumber": "3442-3453-5643-2334",
     * "delivered": false,
     * "pizzaType": "Vegidelight",
     * "orderId": "b06fe761-c95e-4b55-91ff-7e1f8ea8f3a0"
     * }
     * @param orderJson
     * @return
     */
    public static Order getOrder(String orderJson){
        JSONParser parser = new JSONParser();

        Order order = new Order();
        try {
            Object obj = parser.parse(orderJson);
            JSONObject jsonObject = (JSONObject)obj;
            order.setAddress((String)jsonObject.get("address"));
            int quantity = ((Long)jsonObject.get("quantity")).intValue();
            order.setQuantity(quantity);
            order.setCustomerName((String) jsonObject.get("customerName"));
            order.setCreditCardNumber((String) jsonObject.get("creditCardNumber"));
            boolean delivered = Boolean.valueOf((Boolean)jsonObject.get("delivered"));
            order.setDelivered(delivered);
            order.setPizzaType((String)jsonObject.get("pizzaType"));
            order.setOrderId((String) jsonObject.get("orderId"));
            return order;
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return null;
    }

//    public static ArrayList<Order> getOrderList(String ordersJson){
//        JSONParser parser = new JSONParser();
//
//        ArrayList<Order> orderList = new ArrayList<Order>();
//        try {
//            Object obj = parser.parse(orderJson);
//            JSONObject jsonObject = (JSONObject)obj;
//            order.setAddress((String)jsonObject.get("address"));
//            int quantity = ((Long)jsonObject.get("quantity")).intValue();
//            order.setQuantity(quantity);
//            order.setCustomerName((String) jsonObject.get("customerName"));
//            order.setCreditCardNumber((String) jsonObject.get("creditCardNumber"));
//            boolean delivered = Boolean.valueOf((String)jsonObject.get("pizzaType"));
//            order.setDelivered(delivered);
//            order.setPizzaType((String)jsonObject.get("pizzaType"));
//            order.setOrderId((String) jsonObject.get("orderId"));
//            return order;
//        } catch (ParseException e) {
//            e.printStackTrace();
//        }
//        return null;
//    }


    /**
     * Populates Token object using folloing JSON String
     * {
     * "token_type": "bearer",
     * "expires_in": 3600000,
     * "refresh_token": "f43de118a489d56c3b3b7ba77a1549e",
     * "access_token": "269becaec9b8b292906b3f9e69b5a9"
     }
     * @param accessTokenJson
     * @return
     */
    public static Token getAccessToken(String accessTokenJson){
        JSONParser parser = new JSONParser();

        Token token = new Token();
        try {
            Object obj = parser.parse(accessTokenJson);
            JSONObject jsonObject = (JSONObject)obj;
            token.setAccessToken((String)jsonObject.get("access_token"));
            long expiresIn = ((Long)jsonObject.get("expires_in")).intValue();
            token.setExpiresIn(expiresIn);
            token.setRefreshToken((String)jsonObject.get("refresh_token"));
            token.setTokenType((String)jsonObject.get("token_type"));
            String scope = (String)jsonObject.get("scope");
            if(scope != null && !"".equals(scope)){
                String[] scopes = scope.split(" ");
                token.setScopes(Arrays.asList(scopes));
            }

        } catch (ParseException e) {
            e.printStackTrace();
        }
        return token;

    }
}
