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

package org.wso2.am.integration.services.jaxrs.coffeesample.bean;

import javax.xml.bind.annotation.XmlRootElement;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

@XmlRootElement(name = "Order")
public class Order {

    private String orderId;

    private String drinkName;

    private String additions;

    private double cost;

    private boolean locked;       //false by default

    private long timestamp;

    public static final NumberFormat currencyFormat = new DecimalFormat("#.##");
    private static final DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss'Z'");

    public Order() {
        this.orderId = UUID.randomUUID().toString();
        this.timestamp = System.currentTimeMillis();
    }

    public String getOrderId() {
        return orderId;
    }
    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public String getDrinkName() {
        return drinkName;
    }

    public void setDrinkName(String drinkName) {
        this.drinkName = drinkName;
//        this.setCost(calculateCost());
        this.timestamp = System.currentTimeMillis();
    }

    public String getAdditions() {
        return additions;
    }

    public void setAdditions(String additions) {
        this.additions = additions;
//        this.setCost(calculateCost());
        this.timestamp = System.currentTimeMillis();
    }

    public String getCost() {
        return currencyFormat.format(cost);
    }

    public void setCost(double cost) {
        this.cost = cost;
    }

    public boolean isLocked() {
        return locked;
    }

    public void setLocked(boolean locked) {
        this.locked = locked;
    }

    public String getTimestamp() {
        return dateFormat.format(new Date(timestamp));
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    public boolean isAmountAcceptable(double amount) {
        return amount >= cost;
    }
}
